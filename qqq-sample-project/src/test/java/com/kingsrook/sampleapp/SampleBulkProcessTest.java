/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.sampleapp;


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreUpdateCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryRecordLink;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles.SavedBulkLoadProfileMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.delete.BulkDeleteLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.edit.BulkEditTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfileField;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.savedbulkloadprofiles.DeleteSavedBulkLoadProfileProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.savedbulkloadprofiles.QuerySavedBulkLoadProfileProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.savedbulkloadprofiles.StoreSavedBulkLoadProfileProcess;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.qqq.middleware.javalin.executors.ProcessInitOrStepExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepInput;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ProcessInitOrStepOrStatusResponseV1;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Built-in bulk workflows against owned Person/Pet data and native SQL readback.
 *******************************************************************************/
class SampleBulkProcessTest
{
   private QInstance instance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      MemoryRecordStore.fullReset();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      sql("DROP TABLE IF EXISTS shared_saved_bulk_load_profile");
      sql("DROP TABLE IF EXISTS saved_bulk_load_profile");
      sql("CREATE TABLE saved_bulk_load_profile (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, label VARCHAR(250), table_name VARCHAR(250), user_id VARCHAR(250), mapping_json TEXT, is_bulk_edit BOOLEAN)");
      sql("CREATE TABLE shared_saved_bulk_load_profile (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, saved_bulk_load_profile_id INTEGER, user_id VARCHAR(250), scope VARCHAR(30), UNIQUE(saved_bulk_load_profile_id,user_id))");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addPossibleValueSource(TablesPossibleValueSourceMetaDataProvider.defineTablesPossibleValueSource(instance));
      instance.addTable(new QTableMetaData().withName("ownedBulkFiles").withBackendName("memory").withPrimaryKeyField("reference")
         .withField(new QFieldMetaData("reference", QFieldType.STRING).withIsEditable(false))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB)));
      new SavedBulkLoadProfileMetaDataProvider().defineAll(instance, SampleMetaDataProvider.RDBMS_BACKEND_NAME, table ->
      {
         table.setBackendDetails(new RDBMSTableBackendDetails().withTableName(table.getName().equals("savedBulkLoadProfile") ? "saved_bulk_load_profile" : "shared_saved_bulk_load_profile"));
         QInstanceEnricher.setInferredFieldBackendNames(table);
      });
      QContext.init(instance, new QSession().withUser(new QUser().withIdReference("owned-bulk-user")).withPermissions());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      MemoryRecordStore.fullReset();
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCsvColumnValueMappingPreviewValidationAndSavedProfile() throws Exception
   {
      BulkLoadProfile profile = profile(true);
      RunProcessInput input = upload("mapped.csv", "Name,Owner,Species\nMapDog,1,Canine\nMapCat,2,Feline\n".getBytes(StandardCharsets.UTF_8));
      RunProcessOutput preview = map(input, profile);
      assertEquals(2, preview.getRecords().size());
      assertEquals(List.of("6"), rows("SELECT COUNT(*) FROM pet"));
      input.addValue(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      RunProcessOutput validation = resume(input, "review");
      assertStep(validation, "review");
      assertEquals(List.of("6"), rows("SELECT COUNT(*) FROM pet"));
      RunProcessOutput loaded = resume(input, "review");
      assertStep(loaded, "result");
      assertEquals(2, summaryCount(loaded, Status.OK));
      assertEquals(List.of("MapCat:2:2", "MapDog:1:1"), rows("SELECT name || ':' || species_id || ':' || person_id FROM pet WHERE name LIKE 'Map%' ORDER BY name"));

      RunProcessInput store = input(StoreSavedBulkLoadProfileProcess.getProcessMetaData().getName());
      store.addValue("label", "Owned pet import");
      store.addValue("tableName", "pet");
      store.addValue("mappingJson", JsonUtils.toJson(profile));
      new RunProcessAction().execute(store);
      assertEquals(List.of("Owned pet import:owned-bulk-user:pet"), rows("SELECT label || ':' || user_id || ':' || table_name FROM saved_bulk_load_profile"));
      assertThrows(QException.class, () -> new RunProcessAction().execute(store));
      RunProcessInput query = input(QuerySavedBulkLoadProfileProcess.getProcessMetaData().getName());
      query.addValue("tableName", "pet");
      List<?> saved = (List<?>) new RunProcessAction().execute(query).getValue("savedBulkLoadProfileList");
      assertEquals(1, saved.size());
      QRecord record = (QRecord) saved.get(0);
      BulkLoadProfile restored = JsonUtils.toObject(record.getValueString("mappingJson"), BulkLoadProfile.class);
      assertEquals(profile.getFieldList().get(2).getValueMappings(), restored.getFieldList().get(2).getValueMappings());
      RunProcessInput reuse = upload("reuse.csv", "Name,Owner,Species\nReusedCat,3,Feline\n".getBytes(StandardCharsets.UTF_8));
      map(reuse, restored);
      resume(reuse, "review");
      assertEquals(List.of("ReusedCat:2:3"), rows("SELECT name || ':' || species_id || ':' || person_id FROM pet WHERE name='ReusedCat'"));
      RunProcessInput delete = input(DeleteSavedBulkLoadProfileProcess.getProcessMetaData().getName());
      delete.addValue("id", record.getValueInteger("id"));
      new RunProcessAction().execute(delete);
      assertEquals(List.of("0"), rows("SELECT COUNT(*) FROM saved_bulk_load_profile"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSpreadsheetUploadPersistsTypedValues() throws Exception
   {
      byte[] content;
      try(XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bytes = new ByteArrayOutputStream())
      {
         Sheet sheet = workbook.createSheet("Pets");
         Row header = sheet.createRow(0);
         header.createCell(0).setCellValue("Name");
         header.createCell(1).setCellValue("Owner");
         header.createCell(2).setCellValue("Species");
         Row row = sheet.createRow(1);
         row.createCell(0).setCellValue("SheetCat");
         row.createCell(1).setCellValue(3);
         row.createCell(2).setCellValue(2);
         workbook.write(bytes);
         content = bytes.toByteArray();
      }
      RunProcessInput input = upload("pets.xlsx", content);
      map(input, profile(false));
      assertEquals(List.of("6"), rows("SELECT COUNT(*) FROM pet"));
      assertEquals(1, summaryCount(resume(input, "review"), Status.OK));
      assertEquals(List.of("SheetCat:2:3"), rows("SELECT name || ':' || species_id || ':' || person_id FROM pet WHERE name='SheetCat'"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMalformedFileAndMissingRequiredMappingDoNotWrite() throws Exception
   {
      assertThrows(QException.class, () -> upload("malformed.xlsx", "not a workbook".getBytes(StandardCharsets.UTF_8)));
      RunProcessInput input = upload("missing.csv", "Name,Species\nNoOwner,1\n".getBytes(StandardCharsets.UTF_8));
      BulkLoadProfile profile = profile(false).withFieldList(new ArrayList<>(List.of(
         new BulkLoadProfileField().withFieldName("name").withColumnIndex(0),
         new BulkLoadProfileField().withFieldName("speciesId").withColumnIndex(1))));
      map(input, profile);
      RunProcessOutput output = resume(input, "review");
      assertTrue(summaryCount(output, Status.ERROR) > 0);
      assertEquals(List.of("6"), rows("SELECT COUNT(*) FROM pet"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalidRowsAndDuplicateKeysHavePartialResults() throws Exception
   {
      instance.getTable("pet").withUniqueKey(new UniqueKey("name"));
      sql("ALTER TABLE pet ADD CONSTRAINT owned_pet_unique_name UNIQUE(name)");
      RunProcessInput input = upload("mixed.csv", "Name,Owner,Species\nValidNew,1,1\nBadSpecies,1,no-number\nCharlie,1,1\n".getBytes(StandardCharsets.UTF_8));
      map(input, profile(false));
      assertEquals(List.of("6"), rows("SELECT COUNT(*) FROM pet"));
      RunProcessOutput output = resume(input, "review");
      assertEquals(1, summaryCount(output, Status.OK));
      assertTrue(summaryCount(output, Status.ERROR) >= 2);
      assertEquals(List.of("7"), rows("SELECT COUNT(*) FROM pet"));
      assertEquals(List.of("Charlie", "ValidNew"), rows("SELECT name FROM pet WHERE name IN ('Charlie','ValidNew','BadSpecies') ORDER BY name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBulkEditSelectedAndQueryMatchedRecords() throws Exception
   {
      RunProcessInput input = input("person.bulkEdit");
      input.addValue("recordIds", "1,3");
      start(input, "edit");
      input.addValue(BulkEditTransformStep.FIELD_ENABLED_FIELDS, "firstName");
      input.addValue("firstName", "Edited");
      assertStep(resume(input, "edit"), "review");
      assertEquals(List.of("Avery", "Casey"), rows("SELECT first_name FROM person WHERE id IN (1,3) ORDER BY id"));
      input.addValue(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      assertStep(resume(input, "review"), "review");
      assertEquals(List.of("Avery", "Casey"), rows("SELECT first_name FROM person WHERE id IN (1,3) ORDER BY id"));
      assertEquals(2, summaryCount(resume(input, "review"), Status.OK));
      assertEquals(List.of("Edited", "Blair", "Edited", "Drew", "Morgan"), rows("SELECT first_name FROM person ORDER BY id"));
      RunProcessInput filtered = input("person.bulkEdit");
      filtered.addValue(StreamedETLWithFrontendProcess.FIELD_DEFAULT_QUERY_FILTER, new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 2)));
      start(filtered, "edit");
      filtered.addValue(BulkEditTransformStep.FIELD_ENABLED_FIELDS, "lastName");
      filtered.addValue("lastName", "Filtered");
      resume(filtered, "edit");
      assertEquals(1, summaryCount(resume(filtered, "review"), Status.OK));
      assertEquals(List.of("2"), rows("SELECT id FROM person WHERE last_name='Filtered'"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBulkEditInvalidValuesAndPartialErrors() throws Exception
   {
      RunProcessInput invalid = input("person.bulkEdit");
      invalid.addValue("recordIds", "1");
      start(invalid, "edit");
      invalid.addValue(BulkEditTransformStep.FIELD_ENABLED_FIELDS, "firstName");
      invalid.addValue("firstName", null);
      resume(invalid, "edit");
      assertTrue(summaryCount(resume(invalid, "review"), Status.ERROR) > 0);
      assertEquals(List.of("Avery"), rows("SELECT first_name FROM person WHERE id=1"));
      instance.getTable("person").withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(RejectSecondUpdate.class));
      RunProcessInput mixed = input("person.bulkEdit");
      mixed.addValue("recordIds", "1,2");
      start(mixed, "edit");
      mixed.addValue(BulkEditTransformStep.FIELD_ENABLED_FIELDS, "firstName");
      mixed.addValue("firstName", "Accepted");
      resume(mixed, "edit");
      RunProcessOutput output = resume(mixed, "review");
      assertEquals(1, summaryCount(output, Status.OK));
      assertEquals(1, summaryCount(output, Status.ERROR));
      assertEquals(List.of("Accepted", "Blair"), rows("SELECT first_name FROM person WHERE id IN (1,2) ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBulkPermissionsMissingAndUnknownSelectionsDoNotWrite() throws Exception
   {
      for(String action : List.of("bulkEdit", "bulkDelete"))
      {
         assertThrows(QException.class, () -> new RunProcessAction().execute(input("person." + action)));
      }
      RunProcessInput unknown = input("person.bulkDelete");
      unknown.addValue("recordIds", "999999");
      start(unknown, "review");
      assertEquals(0, summaryCount(resume(unknown, "review"), Status.OK));
      instance.getTable("person").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      for(String action : List.of("bulkInsert", "bulkEdit", "bulkDelete"))
      {
         ProcessInitOrStepInput denied = new ProcessInitOrStepInput().withProcessName("person." + action)
            .withValues(Map.of("tableName", "person", "recordIds", "1"));
         assertThrows(QPermissionDeniedException.class, () -> new ProcessInitOrStepExecutor().execute(denied, new ProcessInitOrStepOrStatusResponseV1()));
      }
      assertEquals(List.of("5"), rows("SELECT COUNT(*) FROM person"));
      assertEquals(List.of("6"), rows("SELECT COUNT(*) FROM pet"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordLocksExcludeDeniedRowsFromBulkEditAndDelete() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("ownedPerson"));
      instance.getTable("person").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("ownedPerson")
         .withFieldName("id").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE));
      QContext.getQSession().withSecurityKeyValue("ownedPerson", 1);
      RunProcessInput edit = input("person.bulkEdit");
      edit.addValue("recordIds", "1,3");
      start(edit, "edit");
      edit.addValue(BulkEditTransformStep.FIELD_ENABLED_FIELDS, "firstName");
      edit.addValue("firstName", "Allowed");
      resume(edit, "edit");
      assertEquals(1, summaryCount(resume(edit, "review"), Status.OK));
      assertEquals(List.of("Allowed", "Casey"), rows("SELECT first_name FROM person WHERE id IN (1,3) ORDER BY id"));
      RunProcessInput delete = input("person.bulkDelete");
      delete.addValue("recordIds", "1,3");
      start(delete, "review");
      assertEquals(1, summaryCount(resume(delete, "review"), Status.OK));
      assertEquals(List.of("3"), rows("SELECT id FROM person WHERE id IN (1,3) ORDER BY id"));
      assertEquals(List.of("6"), rows("SELECT id FROM pet WHERE person_id=3"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBulkDeletePreviewAssociationsAndExceptionRollback() throws Exception
   {
      List<String> original = graph();
      RunProcessInput failed = input("person.bulkDelete");
      failed.addValue("recordIds", "1");
      start(failed, "review");
      assertEquals(original, graph());
      failed.addValue(StreamedETLWithFrontendProcess.FIELD_TRANSACTION_LEVEL, StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_PROCESS);
      failed.addValue(StreamedETLWithFrontendProcess.FIELD_LOAD_CODE, new QCodeReference(FailingDeleteLoader.class));
      assertThrows(QException.class, () -> resume(failed, "review"));
      assertEquals(original, graph());
      RunProcessInput input = input("person.bulkDelete");
      input.addValue("recordIds", "1");
      start(input, "review");
      input.addValue(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      assertStep(resume(input, "review"), "review");
      assertEquals(original, graph());
      assertEquals(1, summaryCount(resume(input, "review"), Status.OK));
      assertEquals(List.of("2", "3", "4", "5"), rows("SELECT id FROM person ORDER BY id"));
      assertEquals(List.of("5", "6"), rows("SELECT id FROM pet ORDER BY id"));
      assertEquals(List.of("0"), rows("SELECT COUNT(*) FROM pet_note WHERE pet_id IN (1,2,3,4)"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessInput input(String process)
   {
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(process);
      input.setInputSource(QInputSource.USER);
      if(process.contains("."))
      {
         input.addValue("tableName", process.substring(0, process.indexOf('.')));
      }
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void start(RunProcessInput input, String step) throws QException
   {
      RunProcessOutput output = new RunProcessAction().execute(input);
      input.setProcessUUID(output.getProcessUUID());
      assertStep(output, step);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessOutput resume(RunProcessInput input, String after) throws QException
   {
      input.setStartAfterStep(after);
      return new RunProcessAction().execute(input);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertStep(RunProcessOutput output, String step)
   {
      assertEquals(step, output.getProcessState().getNextStepName().orElse(null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessInput upload(String name, byte[] bytes) throws Exception
   {
      RunProcessInput input = input("pet.bulkInsert");
      start(input, "upload");
      StorageInput storage = new StorageInput("ownedBulkFiles").withReference(UUID.randomUUID() + "-" + name);
      try(OutputStream stream = new StorageAction().createOutputStream(storage))
      {
         stream.write(bytes);
      }
      input.addValue("theFile", new ArrayList<>(List.of(storage)));
      assertStep(resume(input, "upload"), "fileMapping");
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private BulkLoadProfile profile(Boolean valueMapping)
   {
      return new BulkLoadProfile().withVersion("v1").withLayout("FLAT").withHasHeaderRow(true)
         .withFieldList(new ArrayList<>(List.of(new BulkLoadProfileField().withFieldName("name").withColumnIndex(0),
            new BulkLoadProfileField().withFieldName("personId").withColumnIndex(1),
            new BulkLoadProfileField().withFieldName("speciesId").withColumnIndex(2).withDoValueMapping(valueMapping)
               .withValueMappings(valueMapping ? Map.of("Canine", 1, "Feline", 2) : Map.of()))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessOutput map(RunProcessInput input, BulkLoadProfile profile) throws QException
   {
      input.addValue("version", profile.getVersion());
      input.addValue("layout", profile.getLayout());
      input.addValue("hasHeaderRow", profile.getHasHeaderRow());
      input.addValue("fieldListJSON", JsonUtils.toJson(profile.getFieldList()));
      RunProcessOutput output = resume(input, "fileMapping");
      if(output.getProcessState().getNextStepName().orElse("").equals("valueMapping"))
      {
         assertEquals("speciesId", output.getValue("valueMappingFullFieldName"));
         input.addValue("mappedValuesJSON", JsonUtils.toJson(profile.getFieldList().get(2).getValueMappings()));
         output = resume(input, "valueMapping");
      }
      assertStep(output, "review");
      return output;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer summaryCount(RunProcessOutput output, Status status)
   {
      List<?> lines = (List<?>) output.getValue(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY);
      return lines.stream().map(line -> (ProcessSummaryLineInterface) line).filter(line -> status.equals(line.getStatus()))
         .mapToInt(line -> line instanceof ProcessSummaryRecordLink ? 1 : ((ProcessSummaryLine) line).getCount()).sum();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void sql(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement())
      {
         statement.execute(sql);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> rows(String sql) throws Exception
   {
      List<String> result = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql))
      {
         while(rows.next())
         {
            result.add(rows.getString(1));
         }
      }
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> graph() throws Exception
   {
      List<String> result = new ArrayList<>();
      result.addAll(rows("SELECT 'person:' || id FROM person ORDER BY id"));
      result.addAll(rows("SELECT 'pet:' || id || ':' || person_id FROM pet ORDER BY id"));
      result.addAll(rows("SELECT 'note:' || id || ':' || pet_id FROM pet_note ORDER BY id"));
      return result;
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RejectSecondUpdate extends AbstractPreUpdateCustomizer
   {


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         for(QRecord record : records)
         {
            if(Integer.valueOf(2).equals(record.getValueInteger("id")))
            {
               record.addError(new BadInputStatusMessage("Owned second row rejected"));
            }
         }
         return records;
      }
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   public static class FailingDeleteLoader extends BulkDeleteLoadStep
   {


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         super.runOnePage(input, output);
         throw new QException("Owned exception after bulk delete");
      }
   }
}
