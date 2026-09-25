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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreDeleteCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelperTest;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelperTest.CapturingListener;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelperTest.ThrowingListener;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for DeleteAction
 **
 *******************************************************************************/
class DeleteActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test() throws QException
   {
      DeleteInput request = new DeleteInput();
      request.setTableName("person");

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // the mock backend - it'll find a record for id=1, but not for id=2 - so we can test both a found & deleted, and a not-found here //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      request.setPrimaryKeys(List.of(1, 2));
      DeleteOutput result = new DeleteAction().execute(request);
      assertNotNull(result);
      assertEquals(1, result.getDeletedRecordCount());
      assertEquals(1, result.getRecordsWithErrors().size());
      assertEquals(2, result.getRecordsWithErrors().get(0).getValueInteger("id"));
      assertEquals("No record was found to delete for Id = 2", result.getRecordsWithErrors().get(0).getErrors().get(0).getMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testErrorIfBothPrimaryKeysAndFilter()
   {
      DeleteInput request = new DeleteInput();
      request.setTableName("person");
      request.setPrimaryKeys(List.of(1, 2));
      request.setQueryFilter(new QQueryFilter());

      assertThrows(QException.class, () ->
      {
         new DeleteAction().execute(request);
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuditsByPrimaryKey() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 1),
         new QRecord().withValue("id", 2)));
      new InsertAction().execute(insertInput);

      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD));

      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      deleteInput.setPrimaryKeys(List.of(1, 2));
      new DeleteAction().execute(deleteInput);

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertEquals(2, audits.size());
      assertTrue(audits.stream().allMatch(r -> r.getValueString("message").equals("Record was Deleted")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuditsByFilter() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 1),
         new QRecord().withValue("id", 2)));
      new InsertAction().execute(insertInput);

      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD));

      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      deleteInput.setQueryFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, 1, 2)));
      new DeleteAction().execute(deleteInput);

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertEquals(2, audits.size());
      assertTrue(audits.stream().allMatch(r -> r.getValueString("message").equals("Record was Deleted")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAssociatedDeletes() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      {
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
         insertInput.setRecords(List.of(
            new QRecord().withValue("id", 1),
            new QRecord().withValue("id", 2),
            new QRecord().withValue("id", 3)
         ));
         new InsertAction().execute(insertInput);
      }

      {
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(TestUtils.TABLE_NAME_ORDER_EXTRINSIC);
         insertInput.setRecords(List.of(
            new QRecord().withValue("id", 1).withValue("orderId", 1),
            new QRecord().withValue("id", 2).withValue("orderId", 1),
            new QRecord().withValue("id", 3).withValue("orderId", 1),
            new QRecord().withValue("id", 4).withValue("orderId", 1),
            new QRecord().withValue("id", 5).withValue("orderId", 3),
            new QRecord().withValue("id", 6).withValue("orderId", 3),
            new QRecord().withValue("id", 7).withValue("orderId", 3)
         ));
         new InsertAction().execute(insertInput);
      }

      {
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM);
         insertInput.setRecords(List.of(
            new QRecord().withValue("id", 1).withValue("orderId", 1),
            new QRecord().withValue("id", 2).withValue("orderId", 1),
            new QRecord().withValue("id", 3).withValue("orderId", 2),
            new QRecord().withValue("id", 4).withValue("orderId", 3),
            new QRecord().withValue("id", 5).withValue("orderId", 3),
            new QRecord().withValue("id", 6).withValue("orderId", 3)
         ));
         new InsertAction().execute(insertInput);
      }

      {
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
         insertInput.setRecords(List.of(
            new QRecord().withValue("id", 1).withValue("lineItemId", 1), // orderId: 1
            new QRecord().withValue("id", 2).withValue("lineItemId", 1), // orderId: 1
            new QRecord().withValue("id", 3).withValue("lineItemId", 2), // orderId: 1
            new QRecord().withValue("id", 4).withValue("lineItemId", 2), // orderId: 1
            new QRecord().withValue("id", 5).withValue("lineItemId", 3), // orderId: 2
            new QRecord().withValue("id", 6).withValue("lineItemId", 3), // orderId: 2
            new QRecord().withValue("id", 7).withValue("lineItemId", 4), // orderId: 3
            new QRecord().withValue("id", 8).withValue("lineItemId", 5), // orderId: 3
            new QRecord().withValue("id", 9).withValue("lineItemId", 6)  // orderId: 3
         ));
         new InsertAction().execute(insertInput);
      }

      /////////////////////////////////////////////////////////
      // assert about how many things we originally inserted //
      /////////////////////////////////////////////////////////
      assertEquals(3, queryTable(TestUtils.TABLE_NAME_ORDER).size());
      assertEquals(7, queryTable(TestUtils.TABLE_NAME_ORDER_EXTRINSIC).size());
      assertEquals(6, queryTable(TestUtils.TABLE_NAME_LINE_ITEM).size());
      assertEquals(9, queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).size());

      ////////////////////////////////
      // delete (cascading) order 1 //
      ////////////////////////////////
      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      deleteInput.setPrimaryKeys(List.of(1));
      new DeleteAction().execute(deleteInput);

      //////////////////////////////////////////////////
      // assert that the associated data were deleted //
      //////////////////////////////////////////////////
      assertEquals(2, queryTable(TestUtils.TABLE_NAME_ORDER).size());
      assertEquals(3, queryTable(TestUtils.TABLE_NAME_ORDER_EXTRINSIC).size());
      assertEquals(4, queryTable(TestUtils.TABLE_NAME_LINE_ITEM).size());
      assertEquals(5, queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).size());

      ////////////////////
      // delete order 2 //
      ////////////////////
      deleteInput.setPrimaryKeys(List.of(2));
      new DeleteAction().execute(deleteInput);

      //////////////////////////////////////////////////
      // assert that the associated data were deleted //
      //////////////////////////////////////////////////
      assertEquals(1, queryTable(TestUtils.TABLE_NAME_ORDER).size());
      assertEquals(3, queryTable(TestUtils.TABLE_NAME_ORDER_EXTRINSIC).size());
      assertEquals(3, queryTable(TestUtils.TABLE_NAME_LINE_ITEM).size());
      assertEquals(3, queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).size());

      ////////////////////
      // delete order 3 //
      ////////////////////
      deleteInput.setPrimaryKeys(List.of(3));
      new DeleteAction().execute(deleteInput);

      ///////////////////////////////
      // everything is deleted now //
      ///////////////////////////////
      assertEquals(0, queryTable(TestUtils.TABLE_NAME_ORDER).size());
      assertEquals(0, queryTable(TestUtils.TABLE_NAME_ORDER_EXTRINSIC).size());
      assertEquals(0, queryTable(TestUtils.TABLE_NAME_LINE_ITEM).size());
      assertEquals(0, queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).size());

      ////////////////////////////////////////////////
      // make sure no errors if we try more deletes //
      ////////////////////////////////////////////////
      deleteInput.setPrimaryKeys(List.of(3));
      new DeleteAction().execute(deleteInput);

      deleteInput.setPrimaryKeys(List.of(1, 2, 3, 4));
      new DeleteAction().execute(deleteInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> queryTable(String tableName) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      return (queryOutput.getRecords());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSecurityKeys() throws QException
   {
      QContext.getQSession().setSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, ListBuilder.of(1)));
      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      ///////////////////////////////////////////////////////
      // make sure we inserted the records we think we did //
      ///////////////////////////////////////////////////////
      assertIdsExist(TestUtils.TABLE_NAME_ORDER, List.of(1, 2));
      assertIdsExist(TestUtils.TABLE_NAME_LINE_ITEM, List.of(1, 2, 3));
      assertIdsExist(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC, List.of(1, 2, 3));
      assertIdsExist(TestUtils.TABLE_NAME_ORDER_EXTRINSIC, List.of(1, 2, 3, 4));

      QContext.getQSession().setSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, ListBuilder.of(2)));

      //////////////////////////////////////////////////
      // assert can't delete the records at any level //
      //////////////////////////////////////////////////
      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      deleteInput.setPrimaryKeys(List.of(1));
      DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);
      assertEquals(0, deleteOutput.getDeletedRecordCount());
      assertEquals(1, deleteOutput.getRecordsWithErrors().size());
      assertEquals("No record was found to delete for Id = 1", deleteOutput.getRecordsWithErrors().get(0).getErrors().get(0).getMessage());

      deleteInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM);
      deleteInput.setPrimaryKeys(List.of(1));
      deleteOutput = new DeleteAction().execute(deleteInput);
      assertEquals(0, deleteOutput.getDeletedRecordCount());
      assertEquals(1, deleteOutput.getRecordsWithErrors().size());
      assertEquals("No record was found to delete for Id = 1", deleteOutput.getRecordsWithErrors().get(0).getErrors().get(0).getMessage());

      deleteInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      deleteInput.setPrimaryKeys(List.of(1));
      deleteOutput = new DeleteAction().execute(deleteInput);
      assertEquals(0, deleteOutput.getDeletedRecordCount());
      assertEquals(1, deleteOutput.getRecordsWithErrors().size());
      assertEquals("No record was found to delete for Id = 1", deleteOutput.getRecordsWithErrors().get(0).getErrors().get(0).getMessage());

      deleteInput.setTableName(TestUtils.TABLE_NAME_ORDER_EXTRINSIC);
      deleteInput.setPrimaryKeys(List.of(1));
      deleteOutput = new DeleteAction().execute(deleteInput);
      assertEquals(0, deleteOutput.getDeletedRecordCount());
      assertEquals(1, deleteOutput.getRecordsWithErrors().size());
      assertEquals("No record was found to delete for Id = 1", deleteOutput.getRecordsWithErrors().get(0).getErrors().get(0).getMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSecurityLockWriteScope() throws QException
   {
      TestUtils.updatePersonMemoryTableInContextWithWritableByWriteLockAndInsert3TestRecords();

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // try to delete 1, 2, and 3.  2 should be blocked, because it has a writable-By that isn't in our session //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.getQSession().setSecurityKeyValues(MapBuilder.of("writableBy", ListBuilder.of("jdoe")));
      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      deleteInput.setPrimaryKeys(List.of(1, 2, 3));
      DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);

      assertEquals(1, deleteOutput.getRecordsWithErrors().size());
      assertThat(deleteOutput.getRecordsWithErrors().get(0).getErrors().get(0).getMessage())
         .contains("You do not have permission")
         .doesNotContain("kmarsh")
         .doesNotContain("Only Writable By");

      assertEquals(1, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_PERSON_MEMORY)).getCount());
      assertEquals(1, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 2)))).getCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertIdsExist(String tableName, List<Integer> ids) throws QException
   {
      List<QRecord> records = TestUtils.queryTable(tableName);
      for(Integer id : ids)
      {
         assertTrue(records.stream().anyMatch(r -> Objects.equals(id, r.getValueInteger("id"))));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(
         new QRecord().withValue("storeId", 1).withValue("orderNo", "ORD123")

            .withAssociatedRecord("orderLine", new QRecord().withValue("sku", "BASIC1").withValue("quantity", 1)
               .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "LINE-EXT-1.1").withValue("value", "LINE-VAL-1")))

            .withAssociatedRecord("orderLine", new QRecord().withValue("sku", "BASIC2").withValue("quantity", 2)
               .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "LINE-EXT-2.1").withValue("value", "LINE-VAL-2"))
               .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "LINE-EXT-2.2").withValue("value", "LINE-VAL-3")))

            .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "MY-FIELD-1").withValue("value", "MY-VALUE-1"))
            .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "MY-FIELD-2").withValue("value", "MY-VALUE-2"))
            .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "MY-FIELD-3").withValue("value", "MY-VALUE-3")),

         new QRecord().withValue("storeId", 1).withValue("orderNo", "ORD124")
            .withAssociatedRecord("orderLine", new QRecord().withValue("sku", "BASIC3").withValue("quantity", 3))
            .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "YOUR-FIELD-1").withValue("value", "YOUR-VALUE-1"))
      ));
      new InsertAction().execute(insertInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeleteWithErrorsDoesntDeleteAssociations() throws QException
   {
      QContext.getQSession().setSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, ListBuilder.of(1)));

      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER);
      table.withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(OrderPreDeleteCustomizer.class));

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // insert 2 orders - one that will fail to delete, and one that will warn, but should delete. //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER)
         .withRecords(List.of(
            new QRecord().withValue("id", OrderPreDeleteCustomizer.DELETE_ERROR_ID).withValue("storeId", 1).withValue("orderNo", "ORD123")
               .withAssociatedRecord("orderLine", new QRecord().withValue("sku", "BASIC1").withValue("quantity", 1)
                  .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "LINE-EXT-1.1").withValue("value", "LINE-VAL-1"))),

            new QRecord().withValue("id", OrderPreDeleteCustomizer.DELETE_WARN_ID).withValue("storeId", 1).withValue("orderNo", "ORD124")
               .withAssociatedRecord("orderLine", new QRecord().withValue("sku", "BASIC3").withValue("quantity", 3))
               .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "YOUR-FIELD-1").withValue("value", "YOUR-VALUE-1"))
         )));

      ///////////////////////////
      // confirm insert counts //
      ///////////////////////////
      assertEquals(2, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_ORDER)).getCount());
      assertEquals(2, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_LINE_ITEM)).getCount());
      assertEquals(1, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC)).getCount());
      assertEquals(1, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_ORDER_EXTRINSIC)).getCount());

      /////////////////////////////
      // try to delete them both //
      /////////////////////////////
      new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_ORDER).withPrimaryKeys(List.of(OrderPreDeleteCustomizer.DELETE_ERROR_ID, OrderPreDeleteCustomizer.DELETE_WARN_ID)));

      ///////////////////////
      // count what's left //
      ///////////////////////
      assertEquals(1, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_ORDER)).getCount());
      assertEquals(1, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_LINE_ITEM)).getCount());
      assertEquals(1, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC)).getCount());
      assertEquals(0, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_ORDER_EXTRINSIC)).getCount());
   }



   /*******************************************************************************
    ** Typed duplicate targets must yield one failure and cannot recurse forever.
    *******************************************************************************/
   @Test
   void testCyclicDeleteKeepsAllRecordsAndRestoresDuplicateKeys() throws QException
   {
      String tableName = seedCyclicRecords();
      List<QRecord> before = queryTable(tableName);
      DeleteInput input = new DeleteInput(tableName).withPrimaryKeys(List.of(1, "1"));
      DeleteOutput output = new DeleteAction().execute(input);
      assertEquals(0, output.getDeletedRecordCount());
      assertThat(output.getRecordsWithErrors()).hasSize(1);
      assertThat(input.getPrimaryKeys()).containsExactly(1, "1");
      assertEquals(before.stream().map(QRecord::getValues).toList(), queryTable(tableName).stream().map(QRecord::getValues).toList());
   }



   /*******************************************************************************
    ** An omission cascade cannot delete the owner of the pending UPDATE.
    *******************************************************************************/
   @Test
   void testCyclicOmissionKeepsUpdatingParent() throws QException
   {
      String tableName = seedCyclicRecords();
      List<QRecord> before = queryTable(tableName);
      QRecord patch = new QRecord().withValue("id", 1).withValue("payload", "must not change")
         .withAssociatedRecords("children", List.of());
      QRecord output = new UpdateAction().executeForRecord(new UpdateInput(tableName).withRecord(patch));
      assertThat(output.getErrors()).isNotEmpty();
      assertEquals(before.stream().map(QRecord::getValues).toList(), queryTable(tableName).stream().map(QRecord::getValues).toList());
   }



   /*******************************************************************************
    ** Filter/key input state is restored even when the pre-delete customizer throws.
    *******************************************************************************/
   @Test
   void testFilterInputRestoredAfterCustomizerFailure() throws QException
   {
      String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;
      new InsertAction().execute(new InsertInput(tableName).withRecord(new QRecord().withValue("id", 7)));
      QContext.getQInstance().getTable(tableName).withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(ThrowingPreDeleteCustomizer.class));
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 7));
      DeleteInput input = new DeleteInput(tableName).withQueryFilter(filter);
      assertThrows(QException.class, () -> new DeleteAction().execute(input));
      assertSame(filter, input.getQueryFilter());
      assertNull(input.getPrimaryKeys());
      assertThat(queryTable(tableName)).hasSize(1);
   }



   /*******************************************************************************
    ** Validation failures must not be audited as successful deletions.
    *******************************************************************************/
   @Test
   void testRejectedDeleteIsExcludedFromAudit() throws QException
   {
      String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;
      QInstance instance = QContext.getQInstance();
      new InsertAction().execute(new InsertInput(tableName).withRecords(List.of(
         new QRecord().withValue("id", OrderPreDeleteCustomizer.DELETE_ERROR_ID),
         new QRecord().withValue("id", OrderPreDeleteCustomizer.DELETE_WARN_ID))));
      new AuditsMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      instance.getTable(tableName).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD));
      instance.getTable(tableName).withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(OrderPreDeleteCustomizer.class));
      DeleteOutput output = new DeleteAction().execute(new DeleteInput(tableName)
         .withPrimaryKeys(List.of(OrderPreDeleteCustomizer.DELETE_ERROR_ID, OrderPreDeleteCustomizer.DELETE_WARN_ID)));
      assertEquals(1, output.getDeletedRecordCount());
      assertThat(output.getRecordsWithErrors()).hasSize(1);
      assertThat(TestUtils.queryTable("audit")).hasSize(1);
      assertThat(queryTable(tableName)).extracting(record -> record.getValueInteger("id")).containsExactly(OrderPreDeleteCustomizer.DELETE_ERROR_ID);
   }



   /*******************************************************************************
    ** Record change listeners hear about the deleted records (as stored before
    ** the delete, in both records and oldRecords), not about keys that failed,
    ** and they get the caller's transaction.  A throwing listener does not fail
    ** the delete.
    *******************************************************************************/
   @Test
   void testRecordChangeListenerReceivesDeletedRecords() throws QException
   {
      RecordChangeListenerHelperTest.resetListeners();
      String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;
      new InsertAction().execute(new InsertInput(tableName).withRecords(List.of(
         new QRecord().withValue("firstName", "Darin"),
         new QRecord().withValue("firstName", "Tim"),
         new QRecord().withValue("firstName", "James"),
         new QRecord().withValue("firstName", "Kirk"))));
      QContext.getQInstance().withRecordChangeListener(new QCodeReference(CapturingListener.class));

      QBackendTransaction transaction = new QBackendTransaction();
      DeleteOutput deleteOutput = new DeleteAction().execute(new DeleteInput(tableName).withTransaction(transaction).withPrimaryKeys(List.of(1, 47, 3)));
      assertEquals(2, deleteOutput.getDeletedRecordCount());
      assertThat(deleteOutput.getRecordsWithErrors()).hasSize(1);

      assertThat(CapturingListener.events).hasSize(1);
      RecordChangeEvent event = CapturingListener.events.get(0);
      assertEquals(tableName, event.getTableName());
      assertEquals(RecordChangeType.DELETE, event.getType());
      assertThat(event.getRecords()).extracting(r -> r.getValueString("firstName")).containsExactly("Darin", "James");
      assertEquals(event.getRecords(), event.getOldRecords());
      assertSame(transaction, event.getTransaction());

      QContext.getQInstance().setRecordChangeListeners(new ArrayList<>(List.of(new QCodeReference(ThrowingListener.class), new QCodeReference(CapturingListener.class))));
      deleteOutput = new DeleteAction().execute(new DeleteInput(tableName).withQueryFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Tim"))));
      assertEquals(1, deleteOutput.getDeletedRecordCount());
      assertThat(deleteOutput.getRecordsWithWarnings()).isNullOrEmpty();
      assertThat(ThrowingListener.events).hasSize(1);
      assertThat(CapturingListener.events).hasSize(2);
      assertThat(CapturingListener.events.get(1).getRecords()).extracting(r -> r.getValueString("firstName")).containsExactly("Tim");
      assertNull(CapturingListener.events.get(1).getTransaction());
      assertThat(queryTable(tableName)).extracting(r -> r.getValueString("firstName")).containsExactly("Kirk");
   }



   /*******************************************************************************
    ** On a backend that does not pre-fetch old records, the delete queries for
    ** them only when a listener applies.
    *******************************************************************************/
   @Test
   void testRecordChangeListenerOldRecordFetchOnlyWhenApplying() throws QException
   {
      RecordChangeListenerHelperTest.resetListeners();
      String tableName = RecordChangeListenerHelperTest.defineNoPrefetchTable();
      new InsertAction().execute(new InsertInput(tableName).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "one"),
         new QRecord().withValue("id", 2).withValue("name", "two"),
         new QRecord().withValue("id", 3).withValue("name", "three"))));
      MemoryRecordStore.setCollectStatistics(true);

      new DeleteAction().execute(new DeleteInput(tableName).withPrimaryKeys(List.of(1)));
      QContext.getQInstance().withRecordChangeListener(new QCodeReference(CapturingListener.class));
      CapturingListener.appliesToTableName = TestUtils.TABLE_NAME_PERSON_MEMORY;
      new DeleteAction().execute(new DeleteInput(tableName).withPrimaryKeys(List.of(2)));
      assertEquals(0, RecordChangeListenerHelperTest.getMemoryQueryCount());
      assertThat(CapturingListener.events).isEmpty();

      CapturingListener.appliesToTableName = tableName;
      new DeleteAction().execute(new DeleteInput(tableName).withPrimaryKeys(List.of(3)));
      assertEquals(1, RecordChangeListenerHelperTest.getMemoryQueryCount());
      assertThat(CapturingListener.events).hasSize(1);
      assertThat(CapturingListener.events.get(0).getRecords()).extracting(r -> r.getValueString("name")).containsExactly("three");
      assertThat(CapturingListener.events.get(0).getOldRecords()).extracting(r -> r.getValueString("name")).containsExactly("three");
      assertThat(queryTable(tableName)).isEmpty();
   }



   /*******************************************************************************
    ** A two-row cycle and one unrelated row isolate recursive ownership.
    *******************************************************************************/
   private String seedCyclicRecords() throws QException
   {
      String tableName = "cyclicRecords";
      QTableMetaData table = new QTableMetaData().withName(tableName).withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("parentId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("payload", QFieldType.STRING))
         .withAssociation(new Association().withName("children").withAssociatedTableName(tableName).withJoinName("cyclicChildren"));
      QContext.getQInstance().addTable(table);
      QContext.getQInstance().addJoin(new QJoinMetaData().withName("cyclicChildren").withLeftTable(tableName).withRightTable(tableName)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "parentId")));
      new InsertAction().execute(new InsertInput(tableName).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("parentId", 2).withValue("payload", "first"),
         new QRecord().withValue("id", 2).withValue("parentId", 1).withValue("payload", "second"),
         new QRecord().withValue("id", 3).withValue("payload", "unrelated"))));
      return tableName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ThrowingPreDeleteCustomizer extends AbstractPreDeleteCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         throw new QException("Expected pre-delete failure");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class OrderPreDeleteCustomizer extends AbstractPreDeleteCustomizer
   {
      public static final Integer DELETE_ERROR_ID = 9999;
      public static final Integer DELETE_WARN_ID  = 9998;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         for(QRecord record : records)
         {
            if(DELETE_ERROR_ID.equals(record.getValue("id")))
            {
               record.addError(new BadInputStatusMessage("You may not delete this order"));
            }
            else if(DELETE_WARN_ID.equals(record.getValue("id")))
            {
               record.addWarning(new QWarningMessage("It was bad that you deleted this order"));
            }
         }

         return (records);
      }
   }

}
