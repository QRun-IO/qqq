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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreInsertCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreUpdateCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Uniqueness uses final write candidates and native key material only.
 *******************************************************************************/
class UniqueKeyValidationTest extends BaseTest
{
   private QTableMetaData table;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed() throws QException
   {
      table = new QTableMetaData().withName("uniqueValidation").withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("a", QFieldType.STRING)).withField(new QFieldMetaData("b", QFieldType.STRING))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("tenant", QFieldType.INTEGER))
         .withUniqueKey(new UniqueKey("a", "b"));
      QContext.getQInstance().addTable(table);
      new InsertAction().execute(new InsertInput(table.getName()).withSkipUniqueKeyCheck(true).withRecords(List.of(
         row(1, "A", "1"), row(2, "A", "2"), row(3, "B", "3"))));
   }



   /*******************************************************************************
    ** Sparse validation must neither miss old components nor hydrate the patch.
    *******************************************************************************/
   @Test
   void testSparseFullSelfAndNull() throws QException
   {
      QRecord sparse = update(new QRecord().withValue("id", "1").withValue("b", "2"));
      assertThat(sparse.getErrorsAsString()).contains("Another record");
      assertThat(sparse.getValues()).doesNotContainKey("a");
      assertThat(update(row(1, "A", "2")).getErrorsAsString()).contains("Another record");
      assertThat(update(new QRecord().withValue("id", "1").withValue("b", "1")).getErrors()).isNullOrEmpty();
      assertThat(update(new QRecord().withValue("id", 1).withValue("b", null)).getErrors()).isNullOrEmpty();
      assertThat(update(new QRecord().withValue("id", 2).withValue("b", null)).getErrors()).isNullOrEmpty();
      assertEquals(null, stored(1).getValue("b"));
      assertEquals(null, stored(2).getValue("b"));
   }



   /*******************************************************************************
    ** Presentation settings cannot erase a stored component or disclose it in patches.
    *******************************************************************************/
   @Test
   void testHiddenHeavyMaskedComponents() throws QException
   {
      table.getField("a").withIsHidden(true).withIsHeavy(true).withType(QFieldType.PASSWORD);
      QRecord patch = update(new QRecord().withValue("id", 1).withValue("b", "2"));
      assertThat(patch.getErrorsAsString()).contains("Another record");
      assertThat(patch.getValues()).doesNotContainKey("a");
      assertEquals("1", stored(1).getValue("b"));
   }



   /*******************************************************************************
    ** All native owners matter, including one hidden from ordinary queries.
    *******************************************************************************/
   @Test
   void testHiddenOwnerAndHistoricalDuplicate() throws QException
   {
      new InsertAction().execute(new InsertInput(table.getName()).withSkipUniqueKeyCheck(true).withRecord(row(4, "A", "1")));
      assertThat(update(new QRecord().withValue("id", 1).withValue("b", "1")).getErrorsAsString()).contains("Another record");
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("uniqueTenant"));
      table.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("uniqueTenant").withFieldName("tenant"));
      assertThat(new QueryAction().execute(new QueryInput(table.getName())).getRecords()).isEmpty();
      QRecord duplicate = new InsertAction().execute(new InsertInput(table.getName()).withRecord(row(5, "A", "2"))).getRecords().get(0);
      assertThat(duplicate.getErrorsAsString()).contains("Another record");
      QRecord denied = update(new QRecord().withValue("id", 1).withValue("b", "fresh"));
      assertThat(denied.getErrors()).isNotEmpty();
      assertThat(denied.getValues()).doesNotContainKey("a");
   }



   /*******************************************************************************
    ** Invalid candidates reserve no fresh key, while accepted candidates do.
    *******************************************************************************/
   @Test
   void testBatchReservationAfterValidation() throws QException
   {
      List<QRecord> result = new UpdateAction().execute(new UpdateInput(table.getName()).withInputSource(QInputSource.USER).withRecords(List.of(
         row(1, "fresh", "key").withValue("name", null), row(2, "fresh", "key"), row(3, "fresh", "key")))).getRecords();
      assertThat(result.get(0).getErrorsAsString()).contains("required");
      assertThat(result.get(1).getErrors()).isNullOrEmpty();
      assertThat(result.get(2).getErrorsAsString()).contains("Another record");
      assertEquals("A", stored(1).getValue("a"));
      assertEquals("fresh", stored(2).getValue("a"));
      assertEquals("B", stored(3).getValue("a"));

      List<QRecord> inserted = new InsertAction().execute(new InsertInput(table.getName()).withInputSource(QInputSource.USER).withRecords(List.of(
         row(10, "insert", "key").withValue("name", null), row(11, "insert", "key"), row(12, "insert", "key")))).getRecords();
      assertThat(inserted.get(0).getErrorsAsString()).contains("required");
      assertThat(inserted.get(1).getErrors()).isNullOrEmpty();
      assertThat(inserted.get(2).getErrorsAsString()).contains("Another record");
   }



   /*******************************************************************************
    ** Portable sparse execution cannot order repeated key-changing target fragments.
    *******************************************************************************/
   @Test
   void testRepeatedTypedPrimaryKeyFragments() throws QException
   {
      List<QRecord> result = new UpdateAction().execute(new UpdateInput(table.getName()).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("a", "B"), new QRecord().withValue("id", "1").withValue("b", "3"),
         new QRecord().withValue("id", 1).withValue("name", "non-key fragment")))).getRecords();
      assertThat(result).allSatisfy(record -> assertThat(record.getErrorsAsString()).contains("Repeated primary key"));
      assertEquals("A", stored(1).getValue("a"));
      assertEquals("1", stored(1).getValue("b"));
      List<QRecord> nonKey = new UpdateAction().execute(new UpdateInput(table.getName()).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "one"), new QRecord().withValue("id", "1").withValue("name", "two")))).getRecords();
      assertThat(nonKey).allSatisfy(record -> assertThat(record.getErrors()).isNullOrEmpty());
   }



   /*******************************************************************************
    ** A final pre-update customizer is checked in preview and actual execution.
    *******************************************************************************/
   @Test
   void testCustomizerAndPreview() throws QException
   {
      table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(CollidingCustomizer.class));
      QRecord patch = new QRecord().withValue("id", 1).withValue("name", "preview");
      new UpdateAction().performValidations(new UpdateInput(table.getName()).withRecord(patch), Optional.of(List.of(stored(1))), true);
      assertThat(patch.getErrorsAsString()).contains("Another record");
      assertEquals("1", stored(1).getValue("b"));
      assertThat(update(new QRecord().withValue("id", 1).withValue("name", "actual")).getErrorsAsString()).contains("Another record");
      assertEquals("1", stored(1).getValue("b"));
   }



   /*******************************************************************************
    ** Decimal scale and binary object identity are not distinct unique keys.
    *******************************************************************************/
   @Test
   void testDecimalAndBinaryTuples() throws QException
   {
      MemoryRecordStore.getInstance().reset();
      table.getField("a").withType(QFieldType.DECIMAL);
      table.getField("b").withType(QFieldType.BLOB);
      new InsertAction().execute(new InsertInput(table.getName()).withSkipUniqueKeyCheck(true).withRecord(
         new QRecord().withValue("id", 20).withValue("name", "valid").withValue("a", new BigDecimal("2.00")).withValue("b", new byte[] { 1, 2 })));
      QRecord collision = new InsertAction().execute(new InsertInput(table.getName()).withRecord(
         new QRecord().withValue("id", 21).withValue("name", "valid").withValue("a", new BigDecimal("2.0")).withValue("b", new byte[] { 1, 2 }))).getRecords().get(0);
      assertThat(collision.getErrorsAsString()).contains("Another record");
   }



   /*******************************************************************************
    ** Null/empty writes remain no-ops even with declared keys.
    *******************************************************************************/
   @Test
   void testEmptyUpdate() throws QException
   {
      new UpdateAction().execute(new UpdateInput(table.getName()));
      new UpdateAction().execute(new UpdateInput(table.getName()).withRecords(List.of()));
      assertEquals("1", stored(1).getValue("b"));
   }



   /*******************************************************************************
    ** Reservations span raw lookup pages; failure of another key reserves nothing.
    *******************************************************************************/
   @Test
   void testPageBoundaryAndMultipleKeyRejection() throws QException
   {
      int oldPageSize = UniqueKeyHelper.getPageSize();
      UniqueKeyHelper.setPageSize(1);
      try
      {
         table.withUniqueKey(new UniqueKey("name"));
         List<QRecord> results = new UpdateAction().execute(new UpdateInput(table.getName()).withRecords(List.of(
            row(1, "fresh", "tuple"), row(2, "fresh", "tuple").withValue("name", "different"),
            row(3, "fresh", "tuple").withValue("name", "another")))).getRecords();
         assertThat(results.get(0).getErrorsAsString()).contains("Another record");
         assertThat(results.get(1).getErrors()).isNullOrEmpty();
         assertThat(results.get(2).getErrorsAsString()).contains("Another record");
         assertEquals("A", stored(1).getValue("a"));
         assertEquals("fresh", stored(2).getValue("a"));
      }
      finally
      {
         UniqueKeyHelper.setPageSize(oldPageSize);
      }
   }



   /*******************************************************************************
    ** Null in either component is distinct, but invalid typed input still fails.
    *******************************************************************************/
   @Test
   void testNullPositionsAndInvalidTypedKeys() throws QException
   {
      MemoryRecordStore.getInstance().reset();
      table.getField("a").setType(QFieldType.INTEGER);
      List<QRecord> inserted = new InsertAction().execute(new InsertInput(table.getName()).withRecords(List.of(
         row(1, "1", null), row(2, "1", null), row(3, null, "same"), row(4, null, "same")))).getRecords();
      assertThat(inserted).allSatisfy(record -> assertThat(record.getErrors()).isNullOrEmpty());
      QRecord invalid = row(5, "not numeric", null);
      new InsertAction().performValidations(new InsertInput(table.getName()).withRecord(invalid), false, false);
      assertThat(invalid.getErrorsAsString()).contains("Invalid value");
      assertThat(update(row(1, "not numeric", "new")).getErrorsAsString()).contains("Invalid value");
      assertEquals(1, stored(1).getValueInteger("a"));
   }



   /*******************************************************************************
    ** Customizers keep their original phase and run once; batch reservation follows
    ** required/security validation, before the trusted final phase.
    *******************************************************************************/
   @Test
   void testInsertCustomizerPhaseObservations() throws QException
   {
      table.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(PhaseObserver.class));
      for(AbstractPreInsertCustomizer.WhenToRun phase : AbstractPreInsertCustomizer.WhenToRun.values())
      {
         PhaseObserver.phase = phase;
         PhaseObserver.calls = 0;
         List<QRecord> records = List.of(row(10, "A", "2"), row(11, "new", "bad").withValue("name", null),
            row(12, "new", "batch"), row(13, "new", "batch"));
         new InsertAction().performValidations(new InsertInput(table.getName()).withInputSource(QInputSource.USER).withRecords(records), false, false);
         assertEquals(1, PhaseObserver.calls);
         assertEquals(phase.ordinal() >= AbstractPreInsertCustomizer.WhenToRun.BEFORE_REQUIRED_FIELD_CHECKS.ordinal(), PhaseObserver.observed.get(0).contains("Another record"));
         assertEquals(phase.ordinal() >= AbstractPreInsertCustomizer.WhenToRun.BEFORE_SECURITY_CHECKS.ordinal(), PhaseObserver.observed.get(1).contains("required"));
         assertEquals(phase == AbstractPreInsertCustomizer.WhenToRun.AFTER_ALL_VALIDATIONS, PhaseObserver.observed.get(3).contains("Another record"));
         assertThat(records.get(3).getErrorsAsString()).contains("Another record");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord row(int id, String a, String b)
   {
      return new QRecord().withValue("id", id).withValue("a", a).withValue("b", b).withValue("name", "valid").withValue("tenant", 99);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord update(QRecord patch) throws QException
   {
      return new UpdateAction().execute(new UpdateInput(table.getName()).withRecord(patch)).getRecords().get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord stored(int id) throws QException
   {
      return new QueryAction().execute(new QueryInput(table.getName()).withShouldFetchHeavyFields(true)
         .withShouldOmitHiddenFields(false).withShouldMaskPasswords(false)).getRecords().stream()
         .filter(record -> record.getValueInteger("id").equals(id)).findFirst().orElseThrow();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PhaseObserver extends AbstractPreInsertCustomizer
   {
      private static WhenToRun phase;
      private static int calls;
      private static List<String> observed;



      @Override
      public WhenToRun getWhenToRun()
      {
         return phase;
      }



      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         calls++;
         observed = records.stream().map(QRecord::getErrorsAsString).toList();
         return records;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CollidingCustomizer extends AbstractPreUpdateCustomizer
   {
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         records.forEach(record -> record.setValue("b", "2"));
         return records;
      }
   }
}
