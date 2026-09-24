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

package com.kingsrook.qqq.backend.core.model.data;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Operational identities survive in-process copies without changing the
 ** presentation record or becoming serializable input.
 *******************************************************************************/
class QRecordIdentityTest
{
   /*******************************************************************************
    ** Hidden values stay absent in a copied record and in public serialization.
    *******************************************************************************/
   @Test
   void testHiddenIdentitySurvivesCopyWithoutPresentationChanges() throws Exception
   {
      QTableMetaData table = table(QFieldType.INTEGER);
      table.getField("key").setIsHidden(true);
      QRecord record = new QRecord().withValue("name", "Visible");
      String before = JsonUtils.toJson(record);
      record.capturePrimaryKey(table, 731);
      QRecord copy = new QRecord(record);

      assertAll(
         () -> assertEquals(731, record.resolvePrimaryKey(table)),
         () -> assertEquals(731, copy.resolvePrimaryKey(table)),
         () -> assertFalse(copy.getValues().containsKey("key")),
         () -> assertNull(copy.getValue("key")),
         () -> assertNull(copy.getDisplayValue("key")),
         () -> assertEquals(before, JsonUtils.toJson(record)),
         () -> assertEquals(before, JsonUtils.toJson(copy)),
         () -> assertTrue(copy.getBackendDetails().isEmpty()));
   }



   /*******************************************************************************
    ** Masked passwords remain masked; only the dedicated accessor has identity.
    *******************************************************************************/
   @Test
   void testPasswordMaskSurvivesCopy() throws Exception
   {
      QTableMetaData table = table(QFieldType.PASSWORD);
      QRecord record = new QRecord().withValue("key", "************").withDisplayValue("key", "************");
      String before = JsonUtils.toJson(record);
      record.capturePrimaryKey(table, "private-native-key");
      QRecord copy = new QRecord(record);

      assertAll(
         () -> assertEquals("private-native-key", copy.resolvePrimaryKey(table)),
         () -> assertEquals("************", copy.getValue("key")),
         () -> assertEquals("************", copy.getDisplayValue("key")),
         () -> assertEquals(before, JsonUtils.toJson(copy)),
         () -> assertFalse(JsonUtils.toJson(copy).contains("private-native-key")));
   }



   /*******************************************************************************
    ** Serialization does not carry hidden identity, including hostile JSON input.
    *******************************************************************************/
   @Test
   void testIdentityIsNeitherSerializedNorDeserialized() throws Exception
   {
      QTableMetaData table = table(QFieldType.INTEGER);
      QRecord record = new QRecord().withValue("name", "Visible");
      record.capturePrimaryKey(table, 731);
      QRecord javaCopy = SerializationUtils.clone(record);
      QRecord jsonCopy = JsonUtils.toObject(JsonUtils.toJson(record), QRecord.class);
      String injectedJson = """
         {"values":{"key":19},"primaryKeyIdentity":{"tableName":"identityTable","fieldName":"key","type":"INTEGER","primaryKey":731,"present":true,"presentationValue":19}}
         """;
      assertThrows(UnrecognizedPropertyException.class, () -> JsonUtils.toObject(injectedJson, QRecord.class));
      QRecord injected = JsonUtils.toObject(injectedJson, QRecord.class, mapper -> mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

      assertAll(
         () -> assertNull(javaCopy.resolvePrimaryKey(table)),
         () -> assertNull(jsonCopy.resolvePrimaryKey(table)),
         () -> assertEquals(19, injected.resolvePrimaryKey(table)),
         () -> assertFalse(JsonUtils.toJson(injected).contains("primaryKeyIdentity")),
         () -> assertEquals(731, record.resolvePrimaryKey(table)));
   }



   /*******************************************************************************
    ** Neither caller arrays nor accessor results can mutate the captured bytes.
    *******************************************************************************/
   @Test
   void testBinaryIdentityIsDefensive() throws Exception
   {
      QTableMetaData table = table(QFieldType.BLOB);
      table.getField("key").setIsHeavy(true);
      byte[] raw = new byte[] { 0, 1, (byte) 255 };
      QRecord record = new QRecord();
      record.capturePrimaryKey(table, raw);
      QRecord copy = new QRecord(record);
      raw[0] = 99;
      byte[] resolved = (byte[]) copy.resolvePrimaryKey(table);
      resolved[1] = 99;

      assertAll(
         () -> assertArrayEquals(new byte[] { 0, 1, (byte) 255 }, (byte[]) record.resolvePrimaryKey(table)),
         () -> assertArrayEquals(new byte[] { 0, 1, (byte) 255 }, (byte[]) copy.resolvePrimaryKey(table)),
         () -> assertFalse(copy.getValues().containsKey("key")),
         () -> assertEquals(JsonUtils.toJson(new QRecord()), JsonUtils.toJson(copy)));
   }



   /*******************************************************************************
    ** Array mutation through public values cannot silently retarget the record.
    *******************************************************************************/
   @Test
   void testChangedPublicBinaryValueRejects() throws Exception
   {
      QTableMetaData table = table(QFieldType.BLOB);
      byte[] key = new byte[] { 1, 2, 3 };
      QRecord record = new QRecord().withValue("key", key);
      record.capturePrimaryKey(table, key);
      QRecord copy = new QRecord(record);
      key[0] = 9;
      assertAll(
         () -> assertThrows(QException.class, () -> record.resolvePrimaryKey(table)),
         () -> assertThrows(QException.class, () -> copy.resolvePrimaryKey(table)));
   }



   /*******************************************************************************
    ** Equivalent numeric scales and independently equal arrays retain identity.
    *******************************************************************************/
   @Test
   void testEquivalentPublicKeysRemainValid() throws Exception
   {
      QTableMetaData decimal = table(QFieldType.DECIMAL);
      QRecord number = new QRecord().withValue("key", new BigDecimal("2.0000"));
      number.capturePrimaryKey(decimal, new BigDecimal("2.0"));
      number.withValue("key", new BigDecimal("2.00"));
      assertEquals(0, new BigDecimal("2").compareTo((BigDecimal) number.resolvePrimaryKey(decimal)));

      QTableMetaData binary = table(QFieldType.BLOB);
      QRecord bytes = new QRecord().withValue("key", new byte[] { 1, 2, 3 });
      bytes.capturePrimaryKey(binary, new byte[] { 1, 2, 3 });
      bytes.getValues().put("key", new byte[] { 1, 2, 3 });
      assertArrayEquals(new byte[] { 1, 2, 3 }, (byte[]) bytes.resolvePrimaryKey(binary));
   }



   /*******************************************************************************
    ** Every public mutation path is checked, including presence versus null.
    *******************************************************************************/
   @Test
   void testChangedOrRemovedPresentationRejects() throws Exception
   {
      QTableMetaData table = table(QFieldType.INTEGER);
      QRecord original = new QRecord().withValue("key", 7);
      original.capturePrimaryKey(table, 7);
      QRecord changedBySetter = new QRecord(original).withValue("key", 8);
      QRecord changedByMap = new QRecord(original);
      changedByMap.getValues().put("key", 8);
      QRecord removed = new QRecord(original);
      removed.removeValue("key");
      QRecord replacedMap = new QRecord(original);
      replacedMap.setValues(new HashMap<>());
      QRecord hidden = new QRecord();
      hidden.capturePrimaryKey(table, 7);
      hidden.withValue("key", null);

      for(QRecord changed : List.of(changedBySetter, changedByMap, removed, replacedMap, hidden))
      {
         QException error = assertThrows(QException.class, () -> changed.resolvePrimaryKey(table));
         assertFalse(error.getMessage().contains("7"));
         assertFalse(error.getMessage().contains("8"));
      }
      assertEquals(7, original.resolvePrimaryKey(table));
   }



   /*******************************************************************************
    ** Captured keys cannot be reused under another table, key field or type.
    *******************************************************************************/
   @Test
   void testMismatchedMetadataRejects() throws Exception
   {
      QTableMetaData original = table(QFieldType.INTEGER);
      QRecord record = new QRecord();
      record.capturePrimaryKey(original, 7);
      QTableMetaData otherTable = table(QFieldType.INTEGER).withName("otherTable");
      QTableMetaData otherField = table(QFieldType.INTEGER).withField(new QFieldMetaData("otherKey", QFieldType.INTEGER)).withPrimaryKeyField("otherKey");
      QTableMetaData otherType = table(QFieldType.LONG);
      for(QTableMetaData mismatch : List.of(otherTable, otherField, otherType))
      {
         assertThrows(QException.class, () -> record.resolvePrimaryKey(mismatch));
         assertThrows(QException.class, () -> record.capturePrimaryKey(mismatch, 7));
      }
      assertEquals(7, record.resolvePrimaryKey(original));
   }



   /*******************************************************************************
    ** Internal privacy cleanup may refresh presentation without changing identity.
    *******************************************************************************/
   @Test
   void testRecaptureAllowsOnlySameIdentity() throws Exception
   {
      QTableMetaData table = table(QFieldType.INTEGER);
      QRecord record = new QRecord().withValue("key", 7);
      record.capturePrimaryKey(table, 7);
      record.removeValue("key");
      assertThrows(QException.class, () -> record.resolvePrimaryKey(table));
      record.capturePrimaryKey(table, 7);
      assertEquals(7, record.resolvePrimaryKey(table));
      assertThrows(QException.class, () -> record.capturePrimaryKey(table, 8));
      assertEquals(7, record.resolvePrimaryKey(table));
   }



   /*******************************************************************************
    ** Fresh input records retain ordinary typed values, including absent values.
    *******************************************************************************/
   @Test
   void testFreshRecordsResolveOrdinaryValues() throws Exception
   {
      QTableMetaData table = table(QFieldType.INTEGER);
      assertAll(
         () -> assertEquals(7, new QRecord().withValue("key", "7").resolvePrimaryKey(table)),
         () -> assertEquals(0, new QRecord().withValue("key", 0).resolvePrimaryKey(table)),
         () -> assertNull(new QRecord().resolvePrimaryKey(table)),
         () -> assertEquals("", new QRecord().withValue("key", "").resolvePrimaryKey(table(QFieldType.STRING))),
         () -> assertEquals(false, new QRecord().withValue("key", false).resolvePrimaryKey(table(QFieldType.BOOLEAN))));
   }



   /*******************************************************************************
    ** Invalid capture cannot create an identity or expose its input in errors.
    *******************************************************************************/
   @Test
   void testInvalidCaptureRejectsWithoutLeakingValue() throws Exception
   {
      QTableMetaData table = table(QFieldType.INTEGER);
      QRecord record = new QRecord();
      assertThrows(QException.class, () -> record.capturePrimaryKey(table, null));
      QException error = assertThrows(QException.class, () -> record.capturePrimaryKey(table, "private-invalid-key"));
      assertFalse(error.toString().contains("private-invalid-key"));
      assertNull(error.getCause());
      assertThrows(QException.class, () -> record.capturePrimaryKey(null, 7));
      assertNull(record.resolvePrimaryKey(table));
   }



   /*******************************************************************************
    ** Invalid declarations use the checked identity error for both entry points.
    *******************************************************************************/
   @Test
   void testMissingFieldAndTypeRejectAsCheckedErrors() throws Exception
   {
      QTableMetaData valid = table(QFieldType.INTEGER);
      QRecord record = new QRecord();
      record.capturePrimaryKey(valid, 7);
      QTableMetaData missingField = table(QFieldType.INTEGER).withPrimaryKeyField("missing");
      QTableMetaData missingType = table(QFieldType.INTEGER);
      missingType.getField("key").setType(null);
      for(QTableMetaData invalid : List.of(missingField, missingType))
      {
         assertThrows(QException.class, () -> record.capturePrimaryKey(invalid, 7));
         assertThrows(QException.class, () -> record.resolvePrimaryKey(invalid));
         assertThrows(QException.class, () -> new QRecord().resolvePrimaryKey(invalid));
      }
      assertEquals(7, record.resolvePrimaryKey(valid));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData table(QFieldType type)
   {
      return new QTableMetaData().withName("identityTable").withPrimaryKeyField("key")
         .withField(new QFieldMetaData("key", type)).withField(new QFieldMetaData("name", QFieldType.STRING));
   }
}
