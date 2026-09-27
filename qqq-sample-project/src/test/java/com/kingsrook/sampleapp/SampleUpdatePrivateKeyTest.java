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


import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Update uses explicit caller keys while its old-record snapshots stay private.
 ** BLOB/PASSWORD are logical metadata keys on existing FieldLab columns; no
 ** physical schema changes or private identity access are used by this fixture.
 *******************************************************************************/
class SampleUpdatePrivateKeyTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;
   private QTableMetaData canonical;
   private String canonicalJson;
   private QTableMetaData active;
   private String activeJson;
   private String keyField;



   /*******************************************************************************
    ** Equal-length binary keys and identical password masks must remain distinct.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      SnapshotCustomizer.before = List.of();
      SnapshotCustomizer.after = List.of();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(4, statement.executeUpdate("INSERT INTO field_lab(id,name,text_value,password_value,blob_value) VALUES "
            + "(879660101,'First','Original first','update-private-key-1',X'010203'),"
            + "(879660102,'Second','Original second','update-private-key-2',X'040506'),"
            + "(879660103,'Rejected','Original rejected','update-private-key-3',X'070809'),"
            + "(879660104,'Unrelated','Original unrelated','update-private-key-4',X'0A0B0C')"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      SnapshotCustomizer.before = List.of();
      SnapshotCustomizer.after = List.of();
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** Hidden stored ids still match explicit inputs, without hydrating snapshots.
    *******************************************************************************/
   @Test
   void testHiddenKeyMixedUpdateKeepsOldSnapshotsPrivate() throws Exception
   {
      configure("id");
      assertMixedUpdate();
   }



   /*******************************************************************************
    ** Heavy binary identities match explicit byte-array inputs, while old values
    ** remain omitted. Ordinary Query/Get read options are checked before/after.
    *******************************************************************************/
   @Test
   void testHeavyKeyMixedUpdatePreservesSnapshotsAndPublicReadOptions() throws Exception
   {
      configure("blobValue");
      List<Map<String, String>> before = rows();
      assertHeavyPublicReadOptions();
      assertEquals(before, rows());
      assertMixedUpdate();
      List<Map<String, String>> updated = rows();
      assertHeavyPublicReadOptions();
      assertEquals(updated, rows());
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Distinct native password keys do not collapse to the same public mask.
    *******************************************************************************/
   @Test
   void testPasswordKeyMixedUpdateKeepsOldSnapshotsMasked() throws Exception
   {
      configure("passwordValue");
      assertMixedUpdate();
   }



   /*******************************************************************************
    ** USER metadata removal applies to both old-record callback snapshots.
    *******************************************************************************/
   @Test
   void testUserRemovedFieldStaysAbsentFromPreAndPostOldRecords() throws Exception
   {
      assertSourceSpecificOldRecords(QInputSource.USER);
   }



   /*******************************************************************************
    ** SYSTEM still receives the same field under its unrestricted metadata.
    *******************************************************************************/
   @Test
   void testSystemRetainsFieldRemovedOnlyForUserOldRecords() throws Exception
   {
      assertSourceSpecificOldRecords(QInputSource.SYSTEM);
   }



   /*******************************************************************************
    ** The removed field has a distinctive native value and is never submitted in
    ** the patch. Successful text mutation proves this is not an empty-read test.
    *******************************************************************************/
   private void assertSourceSpecificOldRecords(QInputSource source) throws Exception
   {
      configure("id");
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(PrivateOldField.class));
      String sentinel = "private-update-old-record-sentinel";
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(1, statement.executeUpdate("UPDATE field_lab SET html_value='" + sentinel + "' WHERE id=879660101"));
      }
      List<Map<String, String>> expected = rows();
      expected.get(0).put("TEXT_VALUE", "Allowed proposal");
      QRecord patch = new QRecord().withValue("id", key(1)).withValue("textValue", "Allowed proposal");
      List<QRecord> output = new UpdateAction().execute(new UpdateInput(TABLE).withInputSource(source)
         .withOmitModifyDateUpdate(true).withRecord(patch)).getRecords();
      assertAll(
         () -> assertEquals(expected, rows()),
         () -> assertEquals(1, output.size()),
         () -> assertTrue(output.get(0).getErrors().isEmpty(), output.get(0).getErrorsAsString()),
         () -> assertFalse(output.get(0).getValues().containsKey("htmlValue")),
         () ->
         {
            for(List<QRecord> snapshot : List.of(SnapshotCustomizer.before, SnapshotCustomizer.after))
            {
               assertEquals(1, snapshot.size());
               QRecord old = snapshot.get(0);
               assertEquals("First", old.getValueString("name"));
               assertEquals("Original first", old.getValueString("textValue"));
               assertFalse(old.getValues().containsKey("id"));
               if(source == QInputSource.USER)
               {
                  assertFalse(old.getValues().containsKey("htmlValue"));
                  assertFalse(CollectionUtils.nonNullMap(old.getDisplayValues()).containsKey("htmlValue"));
                  assertFalse(JsonUtils.toJson(snapshot).contains(sentinel));
               }
               else
               {
                  assertEquals(sentinel, old.getValueString("htmlValue"));
                  assertTrue(JsonUtils.toJson(snapshot).contains(sentinel));
               }
            }
         },
         () -> assertMetadataUnchanged());
   }



   /*******************************************************************************
    ** Caller-supplied keys stay in sparse write results; privacy here applies to
    ** fetched old snapshots, not a promise to erase submitted patch values.
    *******************************************************************************/
   private void assertMixedUpdate() throws Exception
   {
      List<Map<String, String>> expected = rows();
      expected.get(0).put("TEXT_VALUE", "Allowed proposal");
      expected.get(1).put("TEXT_VALUE", "Warn proposal");
      List<Serializable> keys = List.of(key(2), key(1), key(3), key(9));
      List<QRecord> patches = List.of(
         new QRecord().withValue(keyField, keys.get(0)).withValue("textValue", "Warn proposal"),
         new QRecord().withValue(keyField, keys.get(1)).withValue("textValue", "Allowed proposal"),
         new QRecord().withValue(keyField, keys.get(2)).withValue("textValue", "Reject proposal"),
         new QRecord().withValue(keyField, keys.get(3)).withValue("textValue", "Missing proposal"));
      UpdateInput input = new UpdateInput(TABLE).withInputSource(QInputSource.USER).withOmitModifyDateUpdate(true).withRecords(patches);
      List<QRecord> result = new UpdateAction().execute(input).getRecords();
      assertAll(
         () -> assertEquals(expected, rows()),
         () -> assertEquals(4, result.size()),
         () -> assertTrue(result.get(0).getErrors().isEmpty(), result.get(0).getErrorsAsString()),
         () -> assertTrue(result.get(1).getErrors().isEmpty(), result.get(1).getErrorsAsString()),
         () -> assertEquals(List.of("Reviewed private-key update"), result.get(0).getWarnings().stream().map(QWarningMessage::getMessage).toList()),
         () -> assertTrue(result.get(2).getErrors().stream().anyMatch(error -> error instanceof BadInputStatusMessage)),
         () -> assertFalse(result.get(2).getErrors().stream().anyMatch(NotFoundStatusMessage.class::isInstance)),
         () -> assertTrue(result.get(3).getErrors().stream().anyMatch(NotFoundStatusMessage.class::isInstance)),
         () ->
         {
            for(int i = 0; i < patches.size(); i++)
            {
               assertKeyEquals(keys.get(i), patches.get(i).getValue(keyField));
               assertKeyEquals(keys.get(i), result.get(i).getValue(keyField));
               assertFalse(result.get(i).getValues().containsKey("name"));
            }
         },
         () -> assertPrivateOldRecords(SnapshotCustomizer.before),
         () -> assertPrivateOldRecords(SnapshotCustomizer.after),
         () -> assertMetadataUnchanged());
   }



   /*******************************************************************************
    ** Native identity read flags remain scoped to internal DML. Get's documented
    ** default fetches heavy values, while Query and an explicit light Get do not.
    *******************************************************************************/
   private void assertHeavyPublicReadOptions() throws Exception
   {
      List<QRecord> query = new QueryAction().execute(new QueryInput(TABLE)
         .withFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Unrelated")))).getRecords();
      QRecord light = new GetAction().execute(new GetInput(TABLE).withPrimaryKey(key(4)).withShouldFetchHeavyFields(false)).getRecord();
      QRecord full = new GetAction().execute(new GetInput(TABLE).withPrimaryKey(key(4))).getRecord();
      assertEquals(1, query.size());
      assertNotNull(light);
      assertNotNull(full);
      assertAll(
         () -> assertEquals("Unrelated", query.get(0).getValueString("name")),
         () -> assertEquals("Unrelated", light.getValueString("name")),
         () -> assertEquals("Unrelated", full.getValueString("name")),
         () -> assertFalse(query.get(0).getValues().containsKey("blobValue")),
         () -> assertFalse(light.getValues().containsKey("blobValue")),
         () -> assertArrayEquals(new byte[] { 10, 11, 12 }, full.getValueByteArray("blobValue")));
   }



   /*******************************************************************************
    ** Both callbacks receive original old values, never raw private key hydration.
    *******************************************************************************/
   private void assertPrivateOldRecords(List<QRecord> records)
   {
      assertEquals(List.of("First", "Rejected", "Second"), records.stream().map(record -> record.getValueString("name")).sorted().toList());
      Map<String, String> originalTexts = Map.of("First", "Original first", "Second", "Original second", "Rejected", "Original rejected");
      for(QRecord record : records)
      {
         assertEquals(originalTexts.get(record.getValueString("name")), record.getValueString("textValue"));
         if("passwordValue".equals(keyField))
         {
            assertEquals("************", record.getValueString(keyField));
            String display = CollectionUtils.nonNullMap(record.getDisplayValues()).get(keyField);
            assertTrue(display == null || "************".equals(display));
         }
         else
         {
            assertFalse(record.getValues().containsKey(keyField));
            assertFalse(CollectionUtils.nonNullMap(record.getDisplayValues()).containsKey(keyField));
         }
      }
      String json = JsonUtils.toJson(records);
      assertFalse(json.contains("update-private-key-"), json);
      if("id".equals(keyField))
      {
         for(int i = 1; i <= 3; i++)
         {
            assertFalse(json.contains(Integer.toString(879660100 + i)), json);
         }
      }
      else if("blobValue".equals(keyField))
      {
         for(int i = 1; i <= 3; i++)
         {
            assertFalse(json.contains(Base64.getEncoder().encodeToString((byte[]) key(i))), json);
         }
      }
   }



   /*******************************************************************************
    ** Explicit caller values have the same native types as the declared key.
    *******************************************************************************/
   private Serializable key(int index)
   {
      if("blobValue".equals(keyField))
      {
         return new byte[] { (byte) (index * 3 - 2), (byte) (index * 3 - 1), (byte) (index * 3) };
      }
      if("passwordValue".equals(keyField))
      {
         return "update-private-key-" + index;
      }
      return 879660100 + index;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertKeyEquals(Serializable expected, Serializable actual)
   {
      if(expected instanceof byte[] bytes)
      {
         assertArrayEquals(bytes, (byte[]) actual);
      }
      else
      {
         assertEquals(expected, actual);
      }
   }



   /*******************************************************************************
    ** Only cloned metadata changes; physical id stays INTEGER and primary key.
    *******************************************************************************/
   private void configure(String fieldName) throws Exception
   {
      keyField = fieldName;
      canonical = instance.getTable(TABLE);
      canonicalJson = JsonUtils.toJson(canonical);
      active = canonical.clone();
      active.setPrimaryKeyField(fieldName);
      if("id".equals(fieldName))
      {
         active.getField(fieldName).setIsHidden(true);
         for(QFieldSection section : active.getSections())
         {
            section.setFieldNames(section.getFieldNames().stream().filter(name -> !fieldName.equals(name)).toList());
         }
      }
      else if("blobValue".equals(fieldName))
      {
         active.getField(fieldName).setIsHeavy(true);
      }
      active.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(SnapshotCustomizer.class))
         .withCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(SnapshotCustomizer.class));
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(instance.getTables());
      tables.put(TABLE, active);
      instance.setTables(tables);
      new QInstanceValidator().revalidate(instance);
      activeJson = JsonUtils.toJson(active);
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertMetadataUnchanged() throws Exception
   {
      assertSame(active, instance.getTable(TABLE));
      assertEquals(activeJson, JsonUtils.toJson(active));
      assertEquals("id", canonical.getPrimaryKeyField());
      assertEquals(canonicalJson, JsonUtils.toJson(canonical));
      try(Connection connection = connection())
      {
         try(ResultSet keys = connection.getMetaData().getPrimaryKeys(null, null, "FIELD_LAB"))
         {
            assertTrue(keys.next());
            assertEquals("ID", keys.getString("COLUMN_NAME"));
            assertFalse(keys.next());
         }
         try(ResultSet columns = connection.getMetaData().getColumns(null, null, "FIELD_LAB", "ID"))
         {
            assertTrue(columns.next());
            assertEquals(Types.INTEGER, columns.getInt("DATA_TYPE"));
            assertFalse(columns.next());
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    ** All native fields and binary contents participate in the row oracle.
    *******************************************************************************/
   private List<Map<String, String>> rows() throws Exception
   {
      List<Map<String, String>> rows = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery("SELECT * FROM field_lab ORDER BY id"))
      {
         while(result.next())
         {
            Map<String, String> row = new LinkedHashMap<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               String value;
               if(result.getMetaData().getColumnType(column) == Types.BLOB)
               {
                  byte[] bytes = result.getBytes(column);
                  value = bytes == null ? null : Base64.getEncoder().encodeToString(bytes);
               }
               else
               {
                  value = result.getString(column);
               }
               row.put(result.getMetaData().getColumnLabel(column), value);
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Capture only the public old snapshots; patch status uses submitted text.
    *******************************************************************************/
   public static class SnapshotCustomizer implements TableCustomizerInterface
   {
      private static List<QRecord> before = List.of();
      private static List<QRecord> after = List.of();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecords)
      {
         before = oldRecords.orElseThrow().stream().map(QRecord::new).toList();
         return records.stream().map(record ->
         {
            QRecord copy = new QRecord(record);
            if("Reject proposal".equals(copy.getValueString("textValue")))
            {
               copy.addError(new BadInputStatusMessage("Rejected private-key update"));
            }
            else if("Warn proposal".equals(copy.getValueString("textValue")))
            {
               copy.addWarning(new QWarningMessage("Reviewed private-key update"));
            }
            return copy;
         }).toList();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postUpdate(UpdateInput input, List<QRecord> records, Optional<List<QRecord>> oldRecords)
      {
         after = oldRecords.orElseThrow().stream().map(QRecord::new).toList();
         return records;
      }
   }



   /*******************************************************************************
    ** Active USER metadata loses a field without altering the registered schema.
    *******************************************************************************/
   public static class PrivateOldField implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(!TABLE.equals(input.getTableName()) || input.getInputSource() != QInputSource.USER)
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         table.getFields().remove("htmlValue");
         return table;
      }
   }
}
