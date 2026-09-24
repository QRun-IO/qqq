/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.sampleapp;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.VariantRunStrategy;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryModuleBackendVariantSetting;
import com.kingsrook.qqq.backend.core.processes.implementations.basepull.BasepullConfiguration;
import com.kingsrook.qqq.backend.core.processes.implementations.basepull.ExtractViaBasepullQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native basepull watermark and timestamp-window behavior on owned Person data.
 *******************************************************************************/
class SampleBasepullTest
{
   private static final String STATE = "ownedBasepullState";
   private QSession session;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement())
      {
         statement.execute("DROP TABLE IF EXISTS owned_basepull_state");
         statement.execute("CREATE TABLE owned_basepull_state (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, processkey VARCHAR(100) UNIQUE NOT NULL, lastrun TIMESTAMP(9) NOT NULL)");
      }
      for(Integer id = 1; id <= 5; id++)
      {
         setTimestamp(id, Instant.now().minusSeconds(3600));
      }
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.addTable(new QTableMetaData().withName(STATE).withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("owned_basepull_state")).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("processkey", QFieldType.STRING))
         .withField(new QFieldMetaData("lastrun", QFieldType.DATE_TIME)));
      instance.addBackend(new QBackendMetaData().withName("basepullVariants").withBackendType(MemoryBackendModule.class).withUsesVariants(true)
         .withBackendVariantsConfig(new BackendVariantsConfig().withVariantTypeKey("ownedVariant")
            .withOptionsTableName(SampleMetaDataProvider.PetSpecies.NAME)
            .withBackendSettingSourceFieldNameMap(Map.of(MemoryModuleBackendVariantSetting.PRIMARY_KEY, "possibleValueId"))));
      instance.addProcess(process("ownedBasepull"));
      instance.addProcess(process("ownedVariantPull").withVariantBackend("basepullVariants").withVariantRunStrategy(VariantRunStrategy.SERIAL));
      session = new QSession().withUser(new QUser().withIdReference("owned-basepull-user")).withPermissions();
      QContext.init(instance, session);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInitialLookbackPersistsWatermarkAndEmptyNextBatch() throws Exception
   {
      RunProcessOutput first = new RunProcessAction().execute(input("ownedBasepull"));
      assertEquals(5, first.getRecords().size());
      Instant start = first.getValueInstant(RunProcessAction.BASEPULL_LAST_RUNTIME_KEY);
      Instant end = first.getValueInstant(RunProcessAction.BASEPULL_THIS_RUNTIME_KEY);
      assertEquals(Duration.ofHours(24), Duration.between(start, end));
      Instant stored = watermarks().get("ownedBasepull");
      assertEquals(end.toEpochMilli(), stored.toEpochMilli());
      RunProcessOutput next = new RunProcessAction().execute(input("ownedBasepull"));
      assertEquals(stored, next.getValueInstant(RunProcessAction.BASEPULL_LAST_RUNTIME_KEY));
      assertTrue(next.getRecords().isEmpty());
      assertTrue(!watermarks().get("ownedBasepull").isBefore(stored));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFailureAndExplicitSelectionDoNotAdvanceWatermark() throws Exception
   {
      new RunProcessAction().execute(input("ownedBasepull"));
      Map<String, Instant> before = watermarks();
      RunProcessInput failed = input("ownedBasepull");
      failed.addValue("failAfterExtract", true);
      assertThrows(QException.class, () -> new RunProcessAction().execute(failed));
      assertEquals(before, watermarks());
      RunProcessInput selection = input("ownedBasepull");
      selection.addValue("recordIds", "1");
      RunProcessOutput selected = new RunProcessAction().execute(selection);
      assertEquals(List.of(1), selected.getRecords().stream().map(record -> record.getValueInteger("id")).toList());
      assertEquals(before, watermarks());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTimestampEqualityOverlapEmptyAndReversedWindows() throws Exception
   {
      Instant start = Instant.parse("2026-09-20T10:00:00Z");
      Instant end = start.plusSeconds(60);
      setTimestamp(1, start);
      setTimestamp(2, start.plusSeconds(1));
      setTimestamp(3, end);
      setTimestamp(4, end.plusSeconds(1));
      setTimestamp(5, start.minusSeconds(1));
      assertEquals(List.of(2, 3), extract(start, end, new BasepullConfiguration()));
      assertEquals(List.of(1, 2, 3), extract(start, end, new BasepullConfiguration().withSecondsToSubtractFromLastRunTimeForTimestampQuery(1)));
      assertEquals(List.of(2), extract(start, end, new BasepullConfiguration().withSecondsToSubtractFromThisRunTimeForTimestampQuery(1)));
      assertEquals(List.of(), extract(end, end, new BasepullConfiguration()));
      assertEquals(List.of(), extract(end, start, new BasepullConfiguration()));
      assertTrue(watermarks().isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testVariantWatermarksAreIndependent() throws Exception
   {
      session.setBackendVariants(Map.of("ownedVariant", 1));
      assertEquals(5, new RunProcessAction().execute(input("ownedVariantPull")).getRecords().size());
      Instant firstVariant = watermarks().get("ownedVariantPull-1");
      session.setBackendVariants(Map.of("ownedVariant", 2));
      assertEquals(5, new RunProcessAction().execute(input("ownedVariantPull")).getRecords().size());
      assertEquals(firstVariant, watermarks().get("ownedVariantPull-1"));
      assertEquals(2, watermarks().size());
      Instant secondVariant = watermarks().get("ownedVariantPull-2");
      session.setBackendVariants(Map.of("ownedVariant", 1));
      assertTrue(new RunProcessAction().execute(input("ownedVariantPull")).getRecords().isEmpty());
      assertEquals(secondVariant, watermarks().get("ownedVariantPull-2"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QProcessMetaData process(String name)
   {
      return new QProcessMetaData().withName(name).withTableName("person")
         .withBasepullConfiguration(new BasepullConfiguration().withTableName(STATE).withKeyField("processkey")
            .withLastRunTimeFieldName("lastrun").withHoursBackForInitialTimestamp(24).withTimestampField("createDate"))
         .withStep(new QBackendStepMetaData().withName("extract").withInputData(new QFunctionInputMetaData()
            .withField(new QFieldMetaData(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, QFieldType.STRING).withDefaultValue("person"))
            .withField(new QFieldMetaData(StreamedETLWithFrontendProcess.FIELD_EXTRACT_CODE, QFieldType.STRING).withDefaultValue(new QCodeReference(ExtractViaBasepullQueryStep.class))))
            .withCode(new QCodeReferenceLambda<BackendStep>((in, out) ->
            {
               ExtractViaBasepullQueryStep extract = QCodeLoader.getAdHoc(ExtractViaBasepullQueryStep.class,
                  (QCodeReference) in.getValue(StreamedETLWithFrontendProcess.FIELD_EXTRACT_CODE));
               RecordPipe pipe = new RecordPipe(32);
               extract.setRecordPipe(pipe);
               extract.preRun(in, out);
               extract.run(in, out);
               out.setRecords(pipe.consumeAvailableRecords());
               out.addValue(RunProcessAction.BASEPULL_READY_TO_UPDATE_TIMESTAMP_FIELD, true);
               if(Boolean.TRUE.equals(in.getValue("failAfterExtract")))
               {
                  throw new QException("Owned failure before successful process completion");
               }
            })));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessInput input(String name)
   {
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(name);
      input.setInputSource(QInputSource.USER);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Integer> extract(Instant start, Instant end, BasepullConfiguration config) throws Exception
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.setTableName("person");
      input.setInputSource(QInputSource.USER);
      input.setBasepullLastRunTime(start);
      input.addValue(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, "person");
      input.addValue(RunProcessAction.BASEPULL_TIMESTAMP_FIELD, "createDate");
      input.addValue(RunProcessAction.BASEPULL_THIS_RUNTIME_KEY, end);
      input.addValue(RunProcessAction.BASEPULL_CONFIGURATION, config);
      ExtractViaBasepullQueryStep extract = new ExtractViaBasepullQueryStep();
      RecordPipe pipe = new RecordPipe(32);
      extract.setRecordPipe(pipe);
      RunBackendStepOutput output = new RunBackendStepOutput();
      extract.preRun(input, output);
      extract.run(input, output);
      return pipe.consumeAvailableRecords().stream().map(record -> record.getValueInteger("id")).toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setTimestamp(Integer id, Instant timestamp) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         PreparedStatement statement = connection.prepareStatement("UPDATE person SET create_date=? WHERE id=?"))
      {
         statement.setTimestamp(1, Timestamp.from(timestamp));
         statement.setInt(2, id);
         assertEquals(1, statement.executeUpdate());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, Instant> watermarks() throws Exception
   {
      Map<String, Instant> result = new LinkedHashMap<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement();
         ResultSet rows = statement.executeQuery("SELECT processkey,lastrun FROM owned_basepull_state ORDER BY processkey"))
      {
         while(rows.next())
         {
            result.put(rows.getString(1), rows.getTimestamp(2).toInstant());
         }
      }
      return result;
   }
}
