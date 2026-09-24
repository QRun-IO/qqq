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


import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Canonical FieldLab value/identity contracts use native SQL as their oracle.
 ** Alternative keys below are logical metadata keys on existing columns; the
 ** fixture's physical primary key remains INTEGER id, with unchanged schema.
 *******************************************************************************/
class SampleUpdateValueContractTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;



   /*******************************************************************************
    ** Fixed native values separate Update behavior from Insert normalization.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement())
      {
         assertEquals(2, statement.executeUpdate("INSERT INTO field_lab (id,name,long_value,decimal_value,boolean_value,text_value,blob_value) VALUES "
            + "(1,'Target',7,2.0000,TRUE,'Original target',X'010203'),"
            + "(2,'Decoy',8,3.0000,TRUE,'Original decoy',X'040506')"));
      }
   }



   /*******************************************************************************
    ** Each test owns its metadata instance and canonical in-memory database.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** A present empty binary value is not a missing required value.
    *******************************************************************************/
   @Test
   void testRequiredEmptyBinaryUpdatePersists() throws Exception
   {
      assertRequiredBinaryUpdate(new byte[0]);
   }



   /*******************************************************************************
    ** A zero byte is data even though String.trim removes its decoded character.
    *******************************************************************************/
   @Test
   void testRequiredZeroBinaryUpdatePersists() throws Exception
   {
      assertRequiredBinaryUpdate(new byte[] { 0 });
   }



   /*******************************************************************************
    ** Binary contents are not subject to text whitespace validation.
    *******************************************************************************/
   @Test
   void testRequiredWhitespaceBinaryUpdatePersists() throws Exception
   {
      assertRequiredBinaryUpdate(new byte[] { 9, 10, 32 });
   }



   /*******************************************************************************
    ** Sparse omission preserves required bytes; explicit null rejects only when
    ** required, and can clear the same native nullable column when optional.
    *******************************************************************************/
   @Test
   void testSparseRequiredBinaryAndExplicitNullRemainDistinct() throws Exception
   {
      instance.getTable(TABLE).getField("blobValue").setIsRequired(true);
      List<Map<String, String>> expected = snapshot();
      expected.get(0).put("TEXT_VALUE", "Sparse update");
      QRecord patch = new QRecord().withValue("id", 1).withValue("textValue", "Sparse update");
      QRecord result = update(patch);
      assertAll(
         () -> assertTrue(result.getErrors().isEmpty(), result.getErrorsAsString()),
         () -> assertFalse(patch.getValues().containsKey("blobValue")),
         () -> assertFalse(result.getValues().containsKey("blobValue")),
         () -> assertArrayEquals(new byte[] { 1, 2, 3 }, readBlob(1)),
         () -> assertEquals(expected, snapshot()));

      List<Map<String, String>> beforeNull = snapshot();
      QRecord rejected = update(new QRecord().withValue("id", 1).withValue("blobValue", null).withValue("textValue", "Must not persist"));
      assertAll(
         () -> assertTrue(rejected.getErrors().stream().anyMatch(error -> error instanceof BadInputStatusMessage)),
         () -> assertEquals(beforeNull, snapshot()));

      instance.getTable(TABLE).getField("blobValue").setIsRequired(false);
      QRecord cleared = update(new QRecord().withValue("id", 1).withValue("blobValue", null));
      expected.get(0).put("BLOB_VALUE", null);
      assertAll(
         () -> assertTrue(cleared.getErrors().isEmpty(), cleared.getErrorsAsString()),
         () -> assertTrue(cleared.getValues().containsKey("blobValue")),
         () -> assertNull(cleared.getValue("blobValue")),
         () -> assertNull(readBlob(1)),
         () -> assertEquals(expected, snapshot()));
   }



   /*******************************************************************************
    ** Required typed values preserve false/zero; actual blank text and null
    ** remain invalid, while omitted required fields retain their stored values.
    *******************************************************************************/
   @Test
   void testRequiredFalseZeroSparseAndBlankControls() throws Exception
   {
      instance.getTable(TABLE).getField("longValue").setIsRequired(true);
      instance.getTable(TABLE).getField("booleanValue").setIsRequired(true);
      instance.getTable(TABLE).getField("booleanValue").setDefaultValue(null);
      List<Map<String, String>> expected = snapshot();
      expected.get(0).put("LONG_VALUE", "0");
      expected.get(0).put("BOOLEAN_VALUE", "FALSE");
      QRecord result = update(new QRecord().withValue("id", "1").withValue("longValue", 0L).withValue("booleanValue", false));
      assertAll(
         () -> assertTrue(result.getErrors().isEmpty(), result.getErrorsAsString()),
         () -> assertEquals(0L, result.getValueLong("longValue")),
         () -> assertEquals(false, result.getValueBoolean("booleanValue")),
         () -> assertEquals(expected, snapshot()));

      QRecord sparse = update(new QRecord().withValue("id", 1).withValue("textValue", "Required fields omitted"));
      expected.get(0).put("TEXT_VALUE", "Required fields omitted");
      assertAll(
         () -> assertTrue(sparse.getErrors().isEmpty(), sparse.getErrorsAsString()),
         () -> assertFalse(sparse.getValues().containsKey("longValue")),
         () -> assertFalse(sparse.getValues().containsKey("booleanValue")),
         () -> assertEquals(expected, snapshot()));

      for(QRecord invalid : List.of(
         new QRecord().withValue("id", 1).withValue("longValue", null),
         new QRecord().withValue("id", 1).withValue("longValue", ""),
         new QRecord().withValue("id", 1).withValue("longValue", " \t "),
         new QRecord().withValue("id", 1).withValue("booleanValue", null),
         new QRecord().withValue("id", 1).withValue("booleanValue", "")))
      {
         QRecord rejected = update(invalid.withValue("textValue", "Must not persist"));
         assertAll(
            () -> assertTrue(rejected.getErrors().stream().anyMatch(error -> error instanceof BadInputStatusMessage)),
            () -> assertEquals(expected, snapshot()));
      }
   }



   /*******************************************************************************
    ** Existing BLOB values serve as logical keys; this does not change or claim
    ** a physical BLOB primary-key constraint on the canonical database table.
    *******************************************************************************/
   @Test
   void testLogicalBinaryKeyMatchesTargetAndPreservesDecoy() throws Exception
   {
      QTableMetaData original = useLogicalPrimaryKey("blobValue");
      List<Map<String, String>> expected = snapshot();
      expected.get(0).put("TEXT_VALUE", "Binary key update");
      byte[] key = new byte[] { 1, 2, 3 };
      QRecord patch = new QRecord().withValue("blobValue", key).withValue("textValue", "Binary key update");
      QRecord result = update(patch);
      List<Map<String, String>> afterUpdate = snapshot();
      QRecord missing = update(new QRecord().withValue("blobValue", new byte[] { 7, 8, 9 }).withValue("textValue", "Missing target"));
      assertAll(
         () -> assertTrue(result.getErrors().isEmpty(), result.getErrorsAsString()),
         () -> assertArrayEquals(new byte[] { 1, 2, 3 }, patch.getValueByteArray("blobValue")),
         () -> assertArrayEquals(new byte[] { 1, 2, 3 }, result.getValueByteArray("blobValue")),
         () -> assertSame(key, patch.getValue("blobValue")),
         () -> assertFalse(patch.getValues().containsKey("id")),
         () -> assertFalse(result.getValues().containsKey("id")),
         () -> assertEquals(expected, afterUpdate),
         () -> assertTrue(missing.getErrors().stream().anyMatch(error -> error instanceof NotFoundStatusMessage)),
         () -> assertEquals(afterUpdate, snapshot()),
         () -> assertEquals("id", original.getPrimaryKeyField()));
   }



   /*******************************************************************************
    ** DECIMAL(20,4) storage differs in scale from the logical-key request. Native
    ** equality must find the target while preserving the original caller value.
    *******************************************************************************/
   @Test
   void testLogicalDecimalKeyMatchesTargetAndPreservesDecoy() throws Exception
   {
      QTableMetaData original = useLogicalPrimaryKey("decimalValue");
      List<Map<String, String>> expected = snapshot();
      assertEquals("2.0000", expected.get(0).get("DECIMAL_VALUE"));
      expected.get(0).put("TEXT_VALUE", "Decimal key update");
      BigDecimal key = new BigDecimal("2.0");
      QRecord patch = new QRecord().withValue("decimalValue", key).withValue("textValue", "Decimal key update");
      QRecord result = update(patch);
      List<Map<String, String>> afterUpdate = snapshot();
      QRecord missing = update(new QRecord().withValue("decimalValue", new BigDecimal("9.0")).withValue("textValue", "Missing target"));
      assertAll(
         () -> assertTrue(result.getErrors().isEmpty(), result.getErrorsAsString()),
         () -> assertSame(key, patch.getValue("decimalValue")),
         () -> assertEquals(new BigDecimal("2.0"), result.getValue("decimalValue")),
         () -> assertFalse(patch.getValues().containsKey("id")),
         () -> assertFalse(result.getValues().containsKey("id")),
         () -> assertEquals(expected, afterUpdate),
         () -> assertTrue(missing.getErrors().stream().anyMatch(error -> error instanceof NotFoundStatusMessage)),
         () -> assertEquals(afterUpdate, snapshot()),
         () -> assertEquals("id", original.getPrimaryKeyField()));
   }



   /*******************************************************************************
    ** The existing native-unique name becomes the logical key. The ordinary id
    ** field must then be writable both alone and beside another supplied field.
    *******************************************************************************/
   @Test
   void testRenamedLogicalKeyAllowsOrdinaryIdData() throws Exception
   {
      QTableMetaData original = useLogicalPrimaryKey("name");
      instance.getTable(TABLE).getField("id").setIsEditable(true);
      List<Map<String, String>> firstExpected = snapshot();
      firstExpected.get(0).put("ID", "101");
      QRecord first = update(new QRecord().withValue("name", "Target").withValue("id", 101));
      List<Map<String, String>> afterFirst = snapshotByName();
      QRecord second = update(new QRecord().withValue("name", "Target").withValue("id", 102).withValue("textValue", "Ordinary id and payload"));
      List<Map<String, String>> secondExpected = new ArrayList<>();
      for(Map<String, String> row : firstExpected)
      {
         secondExpected.add(new LinkedHashMap<>(row));
      }
      secondExpected.get(0).put("ID", "102");
      secondExpected.get(0).put("TEXT_VALUE", "Ordinary id and payload");
      assertAll(
         () -> assertTrue(first.getErrors().isEmpty(), first.getErrorsAsString()),
         () -> assertTrue(second.getErrors().isEmpty(), second.getErrorsAsString()),
         () -> assertEquals(101, first.getValueInteger("id")),
         () -> assertEquals(102, second.getValueInteger("id")),
         () -> assertEquals("Target", second.getValueString("name")),
         () -> assertEquals(firstExpected, afterFirst),
         () -> assertEquals(secondExpected, snapshotByName()),
         () -> assertEquals("id", original.getPrimaryKeyField()),
         () -> assertFalse(original.getField("id").getIsEditable()));
   }



   /*******************************************************************************
    ** Under the ordinary metadata id stays the selector; a key-only patch is a
    ** no-op and a typed string key changes only the requested non-key value.
    *******************************************************************************/
   @Test
   void testCanonicalIdSelectorAndKeyOnlyControl() throws Exception
   {
      List<Map<String, String>> expected = snapshot();
      QRecord keyOnly = update(new QRecord().withValue("id", 1));
      assertAll(
         () -> assertTrue(keyOnly.getErrors().isEmpty(), keyOnly.getErrorsAsString()),
         () -> assertEquals(expected, snapshot()));
      QRecord changed = update(new QRecord().withValue("id", "1").withValue("textValue", "Canonical key update"));
      expected.get(0).put("TEXT_VALUE", "Canonical key update");
      assertAll(
         () -> assertTrue(changed.getErrors().isEmpty(), changed.getErrorsAsString()),
         () -> assertEquals(1, changed.getValueInteger("id")),
         () -> assertEquals(expected, snapshot()));
   }



   /*******************************************************************************
    ** Full native rows prove both unchanged omitted fields and untouched decoys;
    ** getBytes additionally checks the exact required binary value independently.
    *******************************************************************************/
   private void assertRequiredBinaryUpdate(byte[] bytes) throws Exception
   {
      instance.getTable(TABLE).getField("blobValue").setIsRequired(true);
      List<Map<String, String>> expected = snapshot();
      expected.get(0).put("BLOB_VALUE", Base64.getEncoder().encodeToString(bytes));
      QRecord patch = new QRecord().withValue("id", 1).withValue("blobValue", bytes.clone());
      QRecord result = update(patch);
      assertAll(
         () -> assertTrue(result.getErrors().isEmpty(), result.getErrorsAsString()),
         () -> assertArrayEquals(bytes, result.getValueByteArray("blobValue")),
         () -> assertArrayEquals(bytes, patch.getValueByteArray("blobValue")),
         () -> assertArrayEquals(bytes, readBlob(1)),
         () -> assertEquals(expected, snapshot()));
   }



   /*******************************************************************************
    ** Display omission must not confound the logical key equality regression.
    ** Only a cloned table changes; the registered source object remains intact.
    *******************************************************************************/
   private QTableMetaData useLogicalPrimaryKey(String fieldName) throws Exception
   {
      QTableMetaData original = instance.getTable(TABLE);
      QTableMetaData table = original.clone();
      table.setPrimaryKeyField(fieldName);
      table.getField(fieldName).setIsHeavy(false);
      table.getField(fieldName).setIsHidden(false);
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(instance.getTables());
      tables.put(TABLE, table);
      instance.setTables(tables);
      new QInstanceValidator().revalidate(instance);
      return original;
   }



   /*******************************************************************************
    ** Timestamp defaults are separately covered; omit them for exact snapshots.
    *******************************************************************************/
   private QRecord update(QRecord record) throws QException
   {
      List<QRecord> records = new UpdateAction().execute(new UpdateInput(TABLE).withInputSource(QInputSource.USER)
         .withOmitModifyDateUpdate(true).withRecord(record)).getRecords();
      assertEquals(1, records.size());
      return records.get(0);
   }



   /*******************************************************************************
    ** JDBC binary readback cannot be satisfied by an echoed action input.
    *******************************************************************************/
   private byte[] readBlob(int id) throws SQLException
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          PreparedStatement statement = connection.prepareStatement("SELECT blob_value FROM field_lab WHERE id=?"))
      {
         statement.setInt(1, id);
         try(ResultSet result = statement.executeQuery())
         {
            assertTrue(result.next());
            byte[] bytes = result.getBytes(1);
            assertFalse(result.next());
            return bytes;
         }
      }
   }



   /*******************************************************************************
    ** Default key order is stable for all tests that preserve physical id.
    *******************************************************************************/
   private List<Map<String, String>> snapshot() throws SQLException
   {
      return rows("SELECT * FROM field_lab ORDER BY id");
   }



   /*******************************************************************************
    ** A stable original-name order remains valid when physical id is edited.
    *******************************************************************************/
   private List<Map<String, String>> snapshotByName() throws SQLException
   {
      return rows("SELECT * FROM field_lab ORDER BY CASE name WHEN 'Target' THEN 0 ELSE 1 END");
   }



   /*******************************************************************************
    ** Full native row snapshots preserve nulls and binary contents explicitly.
    *******************************************************************************/
   private List<Map<String, String>> rows(String sql) throws SQLException
   {
      List<Map<String, String>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
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
}
