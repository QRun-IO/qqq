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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.memory;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** INSERT creates a new Memory owner; it must never act as an implicit UPDATE.
 *******************************************************************************/
class InsertPrimaryKeyContractTest extends BaseTest
{
   private static final String TABLE = "insertKeyContract";



   /*******************************************************************************
    ** Typed identity includes numeric coercion, decimal scale and binary contents.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = QFieldType.class, names = { "INTEGER", "LONG", "DECIMAL", "BLOB", "STRING" })
   void testExistingPrimaryKeyNeverOverwritesOwner(QFieldType keyType) throws Exception
   {
      defineTable(keyType);
      InsertOutput original = insert(new QRecord().withValue("key", ownerKey(keyType)).withValue("name", "Original owner"));
      assertThat(original.getRecords().get(0).getErrors()).isEmpty();
      String before = rawSnapshot();
      QRecord attempted = insert(new QRecord().withValue("key", equivalentKey(keyType)).withValue("name", "Overwrite attempt")).getRecords().get(0);
      assertThat(attempted.getErrors()).anyMatch(error -> error instanceof BadInputStatusMessage);
      assertEquals(before, rawSnapshot());
   }



   /*******************************************************************************
    ** A manual key at the next serial must not be reused later in the same batch.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = QFieldType.class, names = { "INTEGER", "LONG" })
   void testGeneratedKeySkipsSuppliedKeyEqualToNextSerial(QFieldType keyType) throws Exception
   {
      defineTable(keyType);
      InsertOutput output = insert(new QRecord().withValue("key", ownerKey(keyType)).withValue("name", "Manual owner"),
         new QRecord().withValue("name", "Generated owner"));
      assertThat(output.getRecords()).hasSize(2).allSatisfy(record -> assertThat(record.getErrors()).isEmpty());
      assertEquals(keyType == QFieldType.LONG ? Long.valueOf(1) : (Serializable) Integer.valueOf(1), output.getRecords().get(0).getValue("key"));
      assertEquals(keyType == QFieldType.LONG ? Long.valueOf(2) : (Serializable) Integer.valueOf(2), output.getRecords().get(1).getValue("key"));
      List<QRecord> stored = rawRecords();
      assertEquals(2, stored.size());
      assertEquals("Generated owner", stored.get(0).getValueString("name"));
      assertEquals("Manual owner", stored.get(1).getValueString("name"));
      assertEquals(2, stored.get(0).getValueInteger("key"));
      assertEquals(1, stored.get(1).getValueInteger("key"));
   }



   /*******************************************************************************
    ** Missing keys without a native generator must fail before a null-key row exists.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = QFieldType.class, names = { "STRING", "DECIMAL", "BLOB" })
   void testMissingKeyWithoutSerialGeneratorIsRejected(QFieldType keyType) throws Exception
   {
      defineTable(keyType);
      QRecord output = insert(new QRecord().withValue("name", "Missing key")).getRecords().get(0);
      assertThat(output.getErrors()).anyMatch(error -> error instanceof BadInputStatusMessage);
      assertThat(rawRecords()).isEmpty();
   }



   /*******************************************************************************
    ** Rejection is per record; the accepted first owner must remain unchanged.
    *******************************************************************************/
   @Test
   void testDuplicatePrimaryKeyWithinBatchPreservesFirstOwner() throws Exception
   {
      defineTable(QFieldType.INTEGER);
      InsertOutput output = insert(new QRecord().withValue("key", 1).withValue("name", "First owner"),
         new QRecord().withValue("key", "1").withValue("name", "Duplicate owner"));
      assertThat(output.getRecords()).hasSize(2);
      assertThat(output.getRecords().get(0).getErrors()).isEmpty();
      assertThat(output.getRecords().get(1).getErrors()).anyMatch(error -> error instanceof BadInputStatusMessage);
      assertThat(rawRecords()).singleElement().satisfies(record -> assertEquals("First owner", record.getValueString("name")));
   }



   /*******************************************************************************
    ** Use a non-id primary-key name with no extra unique-key declaration.
    *******************************************************************************/
   private void defineTable(QFieldType keyType) throws Exception
   {
      QContext.getQInstance().addTable(new QTableMetaData().withName(TABLE).withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("key").withField(new QFieldMetaData("key", keyType)).withField(new QFieldMetaData("name", QFieldType.STRING)));
      new QInstanceValidator().revalidate(QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable ownerKey(QFieldType keyType)
   {
      return switch(keyType)
      {
         case INTEGER -> 1;
         case LONG -> 1L;
         case DECIMAL -> new BigDecimal("1.0");
         case BLOB -> new byte[] { 1, 2, 3 };
         case STRING -> "manual-key";
         default -> throw new IllegalArgumentException("Unexpected test key type " + keyType);
      };
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable equivalentKey(QFieldType keyType)
   {
      return switch(keyType)
      {
         case INTEGER, LONG -> "1";
         case DECIMAL -> new BigDecimal("1.0000");
         case BLOB -> new byte[] { 1, 2, 3 };
         case STRING -> "manual-key";
         default -> throw new IllegalArgumentException("Unexpected test key type " + keyType);
      };
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private InsertOutput insert(QRecord... records) throws Exception
   {
      return new InsertAction().execute(new InsertInput(TABLE).withRecords(List.of(records)));
   }



   /*******************************************************************************
    ** Read the native Memory adapter directly, without core display processing.
    *******************************************************************************/
   private List<QRecord> rawRecords() throws Exception
   {
      return new MemoryQueryAction().execute(new QueryInput(TABLE)).getRecords().stream()
         .sorted(Comparator.comparing(record -> record.getValueString("name"))).toList();
   }



   /*******************************************************************************
    ** Serialization compares binary content rather than array object identity.
    *******************************************************************************/
   private String rawSnapshot() throws Exception
   {
      return JsonUtils.toJson(rawRecords().stream().map(QRecord::getValues).toList());
   }
}
