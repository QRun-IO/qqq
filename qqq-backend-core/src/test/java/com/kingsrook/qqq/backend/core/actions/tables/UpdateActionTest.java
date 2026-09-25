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


import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelperTest;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelperTest.CapturingListener;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelperTest.NoPrefetchMemoryModule;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelperTest.ThrowingListener;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.statusmessages.QErrorMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for UpdateAction
 **
 *******************************************************************************/
class UpdateActionTest extends BaseTest
{

   /*******************************************************************************
    ** At the core level, there isn't much that can be asserted, as it uses the
    ** mock implementation - just confirming that all of the "wiring" works.
    **
    *******************************************************************************/
   @Test
   public void test() throws QException
   {
      UpdateInput request = new UpdateInput();
      request.setTableName("person");
      List<QRecord> records = new ArrayList<>();
      QRecord       record  = new QRecord();
      record.setValue("id", "47");
      record.setValue("firstName", "James");
      records.add(record);
      request.setRecords(records);
      UpdateOutput result = new UpdateAction().execute(request);
      assertNotNull(result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateAssociationsUpdateOneChild() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      //////////////////////////////////////////////////////////////////////
      // update the order's orderNo, and the quantity on one of the lines //
      //////////////////////////////////////////////////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("orderNo", "ORD123-b")
            .withAssociatedRecord("orderLine", new QRecord().withValue("id", 1).withValue("quantity", 17))
            .withAssociatedRecord("orderLine", new QRecord().withValue("id", 2))
      ));
      new UpdateAction().execute(updateInput);

      List<QRecord> orders = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER);
      assertEquals(2, orders.size());
      assertEquals("ORD123-b", orders.get(0).getValueString("orderNo"));

      List<QRecord> orderLines = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM);
      assertEquals(3, orderLines.size());
      assertEquals(17, orderLines.get(0).getValueInteger("quantity"));

      List<QRecord> lineItemExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      assertEquals(3, lineItemExtrinsics.size());

      List<QRecord> orderExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_EXTRINSIC);
      assertEquals(4, orderExtrinsics.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateAssociationsUpdateOneGrandChild() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      //////////////////////////////////////////////////////////////////////
      // update the order's orderNo, and the quantity on one of the lines //
      //////////////////////////////////////////////////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("orderNo", "ORD123-b")
            .withAssociatedRecord("orderLine", new QRecord().withValue("id", 1)
               .withAssociatedRecord("extrinsics", new QRecord().withValue("id", 1).withValue("value", "LINE-VAL-1-updated")))
            .withAssociatedRecord("orderLine", new QRecord().withValue("id", 2))
      ));
      new UpdateAction().execute(updateInput);

      List<QRecord> orders = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER);
      assertEquals(2, orders.size());
      assertEquals("ORD123-b", orders.get(0).getValueString("orderNo"));

      List<QRecord> orderLines = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM);
      assertEquals(3, orderLines.size());

      List<QRecord> lineItemExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      assertEquals(3, lineItemExtrinsics.size());
      assertEquals("LINE-VAL-1-updated", lineItemExtrinsics.get(0).getValueString("value"));

      List<QRecord> orderExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_EXTRINSIC);
      assertEquals(4, orderExtrinsics.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateAssociationsDeleteOneChild() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      //////////////////////////////////////////////////////////////////////
      // update the order's orderNo, and the quantity on one of the lines //
      //////////////////////////////////////////////////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("orderNo", "ORD123-b")
            .withAssociatedRecord("orderLine", new QRecord().withValue("id", 2))
      ));
      new UpdateAction().execute(updateInput);

      List<QRecord> orders = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER);
      assertEquals(2, orders.size());
      assertEquals("ORD123-b", orders.get(0).getValueString("orderNo"));

      List<QRecord> orderLines = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM);
      assertEquals(2, orderLines.size());
      assertTrue(orderLines.stream().noneMatch(r -> r.getValueInteger("id").equals(1))); // id=1 should be deleted

      List<QRecord> lineItemExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      assertEquals(2, lineItemExtrinsics.size()); // one was deleted (when its parent was deleted)

      List<QRecord> orderExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_EXTRINSIC);
      assertEquals(4, orderExtrinsics.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateAssociationsDeleteGrandchildren() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      //////////////////////////////////////////////////////////////////////
      // update the order's orderNo, and the quantity on one of the lines //
      //////////////////////////////////////////////////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("orderNo", "ORD123-b")
            .withAssociatedRecord("orderLine", new QRecord().withValue("id", 1))
            .withAssociatedRecord("orderLine", new QRecord().withValue("id", 2).withAssociatedRecords("extrinsics", new ArrayList<>()))
      ));
      new UpdateAction().execute(updateInput);

      List<QRecord> orders = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER);
      assertEquals(2, orders.size());
      assertEquals("ORD123-b", orders.get(0).getValueString("orderNo"));

      List<QRecord> orderLines = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM);
      assertEquals(3, orderLines.size());

      List<QRecord> lineItemExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      assertEquals(1, lineItemExtrinsics.size()); // deleted the two beneath line item id=2

      List<QRecord> orderExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_EXTRINSIC);
      assertEquals(4, orderExtrinsics.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateAssociationsInsertOneChild() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      //////////////////////////////////////////////////////////////////////
      // update the order's orderNo, and the quantity on one of the lines //
      //////////////////////////////////////////////////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("orderNo", "ORD123-b")
            .withAssociatedRecord("orderLine", new QRecord().withValue("id", 1))
            .withAssociatedRecord("orderLine", new QRecord().withValue("id", 2))
            .withAssociatedRecord("orderLine", new QRecord().withValue("sku", "BASIC4").withValue("quantity", 47))
      ));
      new UpdateAction().execute(updateInput);

      List<QRecord> orders = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER);
      assertEquals(2, orders.size());
      assertEquals("ORD123-b", orders.get(0).getValueString("orderNo"));

      List<QRecord> orderLines = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM);
      assertEquals(4, orderLines.size());
      assertEquals("BASIC4", orderLines.get(3).getValueString("sku"));
      assertEquals(47, orderLines.get(3).getValueInteger("quantity"));

      List<QRecord> lineItemExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      assertEquals(3, lineItemExtrinsics.size());

      List<QRecord> orderExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_EXTRINSIC);
      assertEquals(4, orderExtrinsics.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateAssociationsDeleteAllChildren() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      //////////////////////////////////////////////////////////////////////
      // update the order's orderNo, and the quantity on one of the lines //
      //////////////////////////////////////////////////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withAssociatedRecords("orderLine", new ArrayList<>()),
         new QRecord().withValue("id", 2).withAssociatedRecords("orderLine", new ArrayList<>())
      ));
      new UpdateAction().execute(updateInput);

      List<QRecord> orders = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER);
      assertEquals(2, orders.size());

      List<QRecord> lineItemExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      assertEquals(0, lineItemExtrinsics.size());

      List<QRecord> orderLines = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM);
      assertEquals(0, orderLines.size()); // all of these got deleted too.

      List<QRecord> orderExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_EXTRINSIC);
      assertEquals(4, orderExtrinsics.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoNotUpdateAssociationsIfErrorInMainRecord() throws QException
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER)
         .getField("storeId").setIsRequired(true);

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      //////////////////////////////////////////////////////////////////////
      // update the order's orderNo, and the quantity on one of the lines //
      //////////////////////////////////////////////////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("storeId", null)
            .withAssociatedRecord("orderLine", new QRecord().withValue("id", 2))
      ));
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
      assertFalse(updateOutput.getRecords().get(0).getErrors().isEmpty());

      List<QRecord> orders = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER);
      assertEquals(2, orders.size());
      assertNotNull(orders.get(0).getValue("storeId"));

      List<QRecord> orderLines = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM);
      assertEquals(3, orderLines.size());
      assertTrue(orderLines.stream().anyMatch(r -> r.getValueInteger("id").equals(1))); // id=1 should NOT be deleted

      List<QRecord> lineItemExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      assertEquals(3, lineItemExtrinsics.size()); // none were was deleted (when its parent was not deleted)
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
   void testRequiredFields() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getField("orderNo").setIsRequired(true);
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      ///////////////////////////////////////////////////
      // insert records that we'll later try to update //
      ///////////////////////////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("storeId", 999).withValue("orderNo", "ORD1"),
         new QRecord().withValue("id", 2).withValue("storeId", 999).withValue("orderNo", "ORD2"),
         new QRecord().withValue("id", 3).withValue("storeId", 999).withValue("orderNo", "ORD3"),
         new QRecord().withValue("id", 4).withValue("storeId", 999).withValue("orderNo", "ORD4")
      ));
      new InsertAction().execute(insertInput);

      //////////////////////////////////////////////////
      // do our update that we'll test the results of //
      //////////////////////////////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("orderNo", null),
         new QRecord().withValue("id", 2).withValue("total", new BigDecimal("3.50")),
         new QRecord().withValue("id", 3).withValue("orderNo", "ORD3B"),
         new QRecord().withValue("id", 4).withValue("orderNo", "   ")
      ));
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);

      ////////////////////////////////////////////////////////////////
      // 1st record tried to set a null orderNo - assert it errored //
      ////////////////////////////////////////////////////////////////
      assertEquals(1, updateOutput.getRecords().get(0).getErrors().size());
      assertEquals("Missing value in required field: Order No", updateOutput.getRecords().get(0).getErrors().get(0).getMessage());

      ////////////////////////////////////////////////////////////////
      // 2nd record didn't try to change orderNo, so should be fine //
      ////////////////////////////////////////////////////////////////
      assertEquals(0, updateOutput.getRecords().get(1).getErrors().size());

      ///////////////////////////////////////////////////////////////////
      // 3rd record should have actually set a new order no - no error //
      ///////////////////////////////////////////////////////////////////
      assertEquals(0, updateOutput.getRecords().get(2).getErrors().size());

      ///////////////////////////////////////////////////////////////////////
      // 4th record tried to set orderNo to all spaces - assert it errored //
      ///////////////////////////////////////////////////////////////////////
      assertEquals(1, updateOutput.getRecords().get(3).getErrors().size());
      assertEquals("Missing value in required field: Order No", updateOutput.getRecords().get(3).getErrors().get(0).getMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateSecurityJoins() throws QException
   {
      QContext.getQSession().setSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, ListBuilder.of(1, 2)));

      ////////////////////////////////////////////////////
      // insert an order in each of store 1 and store 2 //
      // with some lines and line-extrinsics            //
      ////////////////////////////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("orderNo", "O1").withValue("storeId", 1),
         new QRecord().withValue("id", 2).withValue("orderNo", "O2").withValue("storeId", 2)
      ));
      new InsertAction().execute(insertInput);

      insertInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 10).withValue("orderId", 1).withValue("sku", "BASIC1"),
         new QRecord().withValue("id", 20).withValue("orderId", 2).withValue("sku", "BASIC2")
      ));
      new InsertAction().execute(insertInput);

      insertInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 100).withValue("lineItemId", 10).withValue("key", "Key1").withValue("value", "Value1"),
         new QRecord().withValue("id", 200).withValue("lineItemId", 20).withValue("key", "Key2").withValue("value", "Value2")
      ));
      new InsertAction().execute(insertInput);

      ///////////////////////////////////////////////////////////
      // try to remove the value that provides the foreign key //
      ///////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 10).withValue("orderId", null).withValue("sku", "BASIC2")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertEquals("You do not have permission to update this record - the referenced Order was not found.", updateOutput.getRecords().get(0).getErrors().get(0).getMessage());
      }

      ////////////////////////////////////////////////////////////////////////////////////
      // with a session that can only access store 1, try to update the line in store 2 //
      // should fail as a not-found - you can't see that record.                        //
      ////////////////////////////////////////////////////////////////////////////////////
      {
         QContext.getQSession().setSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, ListBuilder.of(1)));
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 20).withValue("sku", "BASIC3")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertTrue(updateOutput.getRecords().get(0).getErrors().stream().anyMatch(em -> em.getMessage().equals("No record was found to update for Id = 20")));
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // with a session that can only access store 1, try to update the line from the order in store 1 to be in store 2 //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         QContext.getQSession().setSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, ListBuilder.of(1)));
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 10).withValue("orderId", 2).withValue("sku", "BASIC3")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertTrue(updateOutput.getRecords().get(0).getErrors().stream().anyMatch(em -> em.getMessage().equals("You do not have permission to update this record - the referenced Order was not found.")));
      }

      ///////////////////////////////////////////////////////////
      // try to remove the value that provides the foreign key //
      ///////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 100).withValue("lineItemId", null).withValue("key", "updatedKey")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertEquals("You do not have permission to update this record - the referenced Order was not found.", updateOutput.getRecords().get(0).getErrors().get(0).getMessage());
      }

      //////////////////////////////////////////////////////////////////////////////////////////////
      // with a session that can only access store 1, try to update the line-extrinsic in store 2 //
      // should fail as a not-found - you can't see that record.                                  //
      //////////////////////////////////////////////////////////////////////////////////////////////
      {
         QContext.getQSession().setSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, ListBuilder.of(1)));
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 200).withValue("key", "updatedKey")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertTrue(updateOutput.getRecords().get(0).getErrors().stream().anyMatch(em -> em.getMessage().equals("No record was found to update for Id = 200")));
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // with a session that can only access store 1, try to update the line-extrinsic from the order in store 1 to be in store 2 //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         QContext.getQSession().setSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, ListBuilder.of(1)));
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 100).withValue("lineItemId", 20).withValue("key", "updatedKey")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertEquals("You do not have permission to update this record - the referenced Order was not found.", updateOutput.getRecords().get(0).getErrors().get(0).getMessage());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateNotFoundFails() throws QException
   {
      QContext.getQSession().withSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, ListBuilder.of(true)));

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(new QRecord().withValue("id", 999).withValue("orderNo", "updated")));
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
      assertEquals("No record was found to update for Id = 999", updateOutput.getRecords().get(0).getErrors().get(0).getMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSecurityKeyValueDenied() throws QException
   {
      ////////////////////////////////
      // insert an order in store 1 //
      ////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("orderNo", "original").withValue("storeId", 1)));
      new InsertAction().execute(insertInput);

      //////////////////////////////////////////////////////////////////////
      // now, as a session with store 2, try to update that store 1 order //
      // it should error as "not found"                                   //
      //////////////////////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, ListBuilder.of(2)));

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("orderNo", "updated")));
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
      assertEquals("No record was found to update for Id = 1", updateOutput.getRecords().get(0).getErrors().get(0).getMessage());

      QContext.getQSession().withSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, ListBuilder.of(true)));
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER)).noneMatch(r -> r.getValueString("orderNo").equals("updated"));
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER)).anyMatch(r -> r.getValueString("orderNo").equals("original"));

      /////////////////////////////////////////////////////////////////////////////////
      // now, go back to store 1 in session, and try to change the order to store 2. //
      // that should fail, as you don't have permission to write to store 2.         //
      /////////////////////////////////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, ListBuilder.of(1)));

      updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("orderNo", "updated").withValue("storeId", 2)));
      updateOutput = new UpdateAction().execute(updateInput);
      assertEquals("You do not have permission to update a record with a value of 2 in the field: Store Id", updateOutput.getRecords().get(0).getErrors().get(0).getMessage());

      QContext.getQSession().withSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, ListBuilder.of(true)));
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER)).noneMatch(r -> r.getValueString("orderNo").equals("updated"));
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER)).anyMatch(r -> r.getValueString("orderNo").equals("original"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSecurityKeyNullDenied() throws QException
   {
      ////////////////////////////////
      // insert an order in store 1 //
      ////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("orderNo", "original").withValue("storeId", 1)));
      new InsertAction().execute(insertInput);

      ///////////////////////////////////////////
      // try to update its storeId to null now //
      ///////////////////////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("storeId", null)));
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
      assertEquals("You do not have permission to update a record without a value in the field: Store Id", updateOutput.getRecords().get(0).getErrors().get(0).getMessage());
      assertEquals(0, TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER).stream().filter(r -> r.getValue("storeId") == null).count());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSecurityKeyNullAllowed() throws QException
   {
      /////////////////////////////////////
      // change storeId to be allow-null //
      /////////////////////////////////////
      QInstance qInstance = QContext.getQInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getRecordSecurityLocks().get(0).setNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW);

      ////////////////////////////////
      // insert an order in store 1 //
      ////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("orderNo", "original").withValue("storeId", 1)));
      new InsertAction().execute(insertInput);

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("storeId", null)));
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
      assertEquals(0, updateOutput.getRecords().get(0).getErrors().size());
      assertEquals(1, TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER).stream().filter(r -> r.getValue("storeId") == null).count());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSecurityKeyAllAccess() throws QException
   {
      ////////////////////////////////
      // insert 2 orders in store 1 //
      ////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("orderNo", "O1").withValue("storeId", 1),
         new QRecord().withValue("id", 2).withValue("orderNo", "O2").withValue("storeId", 1)
      ));
      new InsertAction().execute(insertInput);

      /////////////////////////////////////////////////////////
      // make sure with all-access key we can update however //
      /////////////////////////////////////////////////////////
      QContext.getQSession().setSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, ListBuilder.of(true)));
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      updateInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("storeId", 999),
         new QRecord().withValue("id", 2).withValue("storeId", null)
      ));
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
      assertEquals(0, updateOutput.getRecords().get(0).getErrors().size());
      assertEquals(0, updateOutput.getRecords().get(1).getErrors().size());
      assertEquals(1, TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER).stream().filter(r -> Objects.equals(r.getValue("storeId"), 999)).count());
      assertEquals(1, TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER).stream().filter(r -> r.getValue("storeId") == null).count());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSecurityLockWriteScope() throws QException
   {
      TestUtils.updatePersonMemoryTableInContextWithWritableByWriteLockAndInsert3TestRecords();

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // try to update them all 1, 2, and 3.  2 should be blocked, because it has a writable-By that isn't in our session //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         QContext.getQSession().setSecurityKeyValues(MapBuilder.of("writableBy", ListBuilder.of("jdoe")));
         UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(List.of(
            new QRecord().withValue("id", 1).withValue("lastName", "Kelkhoff"),
            new QRecord().withValue("id", 2).withValue("lastName", "Chamberlain"),
            new QRecord().withValue("id", 3).withValue("lastName", "Maes")
         )));

         List<QRecord> errorRecords = updateOutput.getRecords().stream().filter(r -> CollectionUtils.nullSafeHasContents(r.getErrors())).toList();
         assertEquals(1, errorRecords.size());
         assertEquals(2, errorRecords.get(0).getValueInteger("id"));
         assertThat(errorRecords.get(0).getErrors().get(0).getMessage())
            .isEqualTo("You do not have permission to update this record.")
            .doesNotContain("kmarsh");

         assertEquals(2, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
            .withFilter(new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.IS_NOT_BLANK)))).getCount());
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // now try to change one of the records to have a different value in the lock-field.  Should fail (as it's not in our session) //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(List.of(
            new QRecord().withValue("id", 1).withValue("onlyWritableBy", "ecartman"))));

         List<QRecord> errorRecords = updateOutput.getRecords().stream().filter(r -> CollectionUtils.nullSafeHasContents(r.getErrors())).toList();
         assertEquals(1, errorRecords.size());
         assertThat(errorRecords.get(0).getErrors().get(0).getMessage())
            .contains("You do not have permission")
            .contains("ecartman")
            .contains("Only Writable By");
      }

      ///////////////////////////////////////////////////////////////
      // add that to our session and confirm we can do that update //
      ///////////////////////////////////////////////////////////////
      {
         QContext.getQSession().setSecurityKeyValues(MapBuilder.of("writableBy", ListBuilder.of("jdoe", "ecartman")));
         UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(List.of(
            new QRecord().withValue("id", 1).withValue("onlyWritableBy", "ecartman"))));
         List<QRecord> errorRecords = updateOutput.getRecords().stream().filter(r -> CollectionUtils.nullSafeHasContents(r.getErrors())).toList();
         assertEquals(0, errorRecords.size());
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////
      // change the null behavior to deny, then try to udpate a record and remove its onlyWritableBy //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getRecordSecurityLocks().get(0).setNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY);
      {
         UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(List.of(
            new QRecord().withValue("id", 1).withValue("onlyWritableBy", null))));
         List<QErrorMessage> errors = updateOutput.getRecords().get(0).getErrors();
         assertEquals(1, errors.size());
         assertThat(errors.get(0).getMessage())
            .contains("You do not have permission")
            .contains("without a value")
            .contains("Only Writable By");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateDoNotUpdateModifyDate() throws QException
   {
      QContext.getQSession().withSecurityKeyValues(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, ListBuilder.of(true)));

      ////////////////////////////////////////////////////
      // create a test order and capture its modifyDate //
      ////////////////////////////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(new QRecord()));
      QRecord qRecord = new InsertAction().execute(insertInput).getRecords().get(0);

      Instant initialModifyDate = qRecord.getValueInstant("modifyDate");
      assertNotNull(initialModifyDate);

      ///////////////////////////////////////////////////////////////////
      // update the order, the modify date should be in the future now //
      ///////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
         updateInput.setRecords(List.of(qRecord));
         QRecord updatedRecord = new UpdateAction().execute(updateInput).getRecords().get(0);

         Instant newModifyDate = updatedRecord.getValueInstant("modifyDate");
         assertNotNull(initialModifyDate);
         assertThat(initialModifyDate).isBefore(newModifyDate);

         /////////////////////////////////////////////////////////////////////////////
         // set the initial modify date to this modify date for the next test below //
         /////////////////////////////////////////////////////////////////////////////
         initialModifyDate = newModifyDate;
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // now do an update setting flag to not update the modify date, then compare, should be equal //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
         updateInput.setRecords(List.of(qRecord));
         updateInput.setOmitModifyDateUpdate(true);
         QRecord updatedRecord = new UpdateAction().execute(updateInput).getRecords().get(0);

         Instant newModifyDate = updatedRecord.getValueInstant("modifyDate");
         assertNotNull(initialModifyDate);
         assertThat(initialModifyDate).isEqualTo(newModifyDate);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPersonalization() throws QException
   {
      String userWithPesonalizedTable    = "jdoe";
      String userWithoutPesonalizedTable = "jkirk";

      /////////////////////////////////////////////////////////////////////////////////
      // for one user, add short max-length to lastName, and isRequired to noOfShoes //
      /////////////////////////////////////////////////////////////////////////////////
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      ExamplePersonalizer.addFieldToAddForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY,
         QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getField("lastName").clone().withMaxLength(6).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS),
         userWithPesonalizedTable);
      ExamplePersonalizer.addFieldToAddForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY,
         QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getField("noOfShoes").clone().withIsRequired(true),
         userWithPesonalizedTable);

      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("lastName", "Smith")));
      Integer      id           = insertOutput.getRecords().get(0).getValueInteger("id");

      ///////////////////////////////////////////////////////////////
      // ensure default behaviors for user without personalization //
      ///////////////////////////////////////////////////////////////
      QContext.getQSession().getUser().setIdReference(userWithoutPesonalizedTable);
      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("id", id).withValue("lastName", "Simpson")).withInputSource(QInputSource.USER));
      assertEquals("Simpson", insertOutput.getRecords().get(0).getValueString("lastName"));

      //////////////////////////////////////////////////////////////////////////////
      // now as personalized user - first get an error for missing required value //
      //////////////////////////////////////////////////////////////////////////////
      QContext.getQSession().getUser().setIdReference(userWithPesonalizedTable);
      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("id", id).withValue("lastName", "Jefferson").withValue("noOfShoes", null)).withInputSource(QInputSource.USER));
      assertThat(updateOutput.getRecords().get(0).getErrorsAsString()).contains("Missing value in required field: No Of Shoes");

      ///////////////////////////////////////////////////////
      // try again, and let it work and see name truncated //
      ///////////////////////////////////////////////////////
      updateOutput = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("id", id).withValue("lastName", "Jefferson").withValue("noOfShoes", 3)).withInputSource(QInputSource.USER));
      assertThat(updateOutput.getRecords().get(0).getErrors()).isNullOrEmpty();
      assertEquals("Jef...", insertOutput.getRecords().get(0).getValueString("lastName"));

      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // now remove a field from the table for that user - then make sure values in that field don't get set. //
      // note, this detail is handled in the backend module.                                                  //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY, "cost", userWithPesonalizedTable);
      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("id", id).withValue("cost", new BigDecimal("3.50"))).withInputSource(QInputSource.USER));
      QRecord updatedRecord = new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withPrimaryKey(id));
      assertNull(updatedRecord.getValue("cost"));
   }



   /*******************************************************************************
    ** Record change listeners hear about updated records, each paired with its
    ** old record, but not about records that failed, and they get the caller's
    ** transaction.
    *******************************************************************************/
   @Test
   void testRecordChangeListenerReceivesUpdatedAndOldRecords() throws QException
   {
      RecordChangeListenerHelperTest.resetListeners();
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(List.of(
         new QRecord().withValue("firstName", "Darin").withValue("lastName", "Kelkhoff"),
         new QRecord().withValue("firstName", "Tim").withValue("lastName", "Chamberlain"))));
      QContext.getQInstance().withRecordChangeListener(new QCodeReference(CapturingListener.class));

      QBackendTransaction transaction = new QBackendTransaction();
      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withTransaction(transaction).withRecords(List.of(
         new QRecord().withValue("id", 2).withValue("firstName", "Timothy"),
         new QRecord().withValue("id", 47).withValue("firstName", "Nobody"),
         new QRecord().withValue("id", 1).withValue("firstName", "Dude"))));
      assertThat(updateOutput.getRecords().get(1).getErrors()).isNotEmpty();

      assertThat(CapturingListener.events).hasSize(1);
      RecordChangeEvent event = CapturingListener.events.get(0);
      assertEquals(TestUtils.TABLE_NAME_PERSON_MEMORY, event.getTableName());
      assertEquals(RecordChangeType.UPDATE, event.getType());
      assertThat(event.getRecords()).extracting(r -> r.getValueString("firstName")).containsExactly("Timothy", "Dude");
      assertThat(event.getOldRecords()).extracting(r -> r.getValueString("firstName")).containsExactly("Tim", "Darin");
      assertThat(event.getOldRecords()).extracting(r -> r.getValueInteger("id")).containsExactly(2, 1);
      assertSame(transaction, event.getTransaction());
   }



   /*******************************************************************************
    ** A listener that throws does not fail the update.
    *******************************************************************************/
   @Test
   void testRecordChangeListenerExceptionDoesNotFailUpdate() throws QException
   {
      RecordChangeListenerHelperTest.resetListeners();
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("firstName", "Darin")));
      QContext.getQInstance().withRecordChangeListener(new QCodeReference(ThrowingListener.class));

      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("id", 1).withValue("firstName", "Dude")));

      assertThat(updateOutput.getRecords().get(0).getErrors()).isNullOrEmpty();
      assertThat(ThrowingListener.events).hasSize(1);
      assertEquals("Dude", TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY).get(0).getValueString("firstName"));
   }



   /*******************************************************************************
    ** On a backend that does not pre-fetch old records, the update queries for
    ** them only when a listener applies, and a failed fetch leaves the old record
    ** null instead of failing the update.
    *******************************************************************************/
   @Test
   void testRecordChangeListenerOldRecordFetchOnlyWhenApplying() throws QException
   {
      RecordChangeListenerHelperTest.resetListeners();
      String tableName = RecordChangeListenerHelperTest.defineNoPrefetchTable();
      new InsertAction().execute(new InsertInput(tableName).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "one"),
         new QRecord().withValue("id", 2).withValue("name", "two"))));
      MemoryRecordStore.setCollectStatistics(true);

      ///////////////////////////////////////////////////////////////////
      // no listeners, then only a listener for another table: nothing //
      // extra is queried, and nothing is heard                        //
      ///////////////////////////////////////////////////////////////////
      new UpdateAction().execute(new UpdateInput(tableName).withRecord(new QRecord().withValue("id", 1).withValue("name", "uno")));
      QContext.getQInstance().withRecordChangeListener(new QCodeReference(CapturingListener.class));
      CapturingListener.appliesToTableName = TestUtils.TABLE_NAME_PERSON_MEMORY;
      new UpdateAction().execute(new UpdateInput(tableName).withRecord(new QRecord().withValue("id", 1).withValue("name", "eins")));
      assertEquals(0, RecordChangeListenerHelperTest.getMemoryQueryCount());
      assertThat(CapturingListener.events).isEmpty();

      /////////////////////////////////////////////////////////////////
      // once a listener applies, one query fetches the old records //
      /////////////////////////////////////////////////////////////////
      CapturingListener.appliesToTableName = tableName;
      new UpdateAction().execute(new UpdateInput(tableName).withRecord(new QRecord().withValue("id", 2).withValue("name", "dos")));
      assertEquals(1, RecordChangeListenerHelperTest.getMemoryQueryCount());
      assertThat(CapturingListener.events).hasSize(1);
      assertEquals("dos", CapturingListener.events.get(0).getRecords().get(0).getValueString("name"));
      assertEquals("two", CapturingListener.events.get(0).getOldRecords().get(0).getValueString("name"));

      ////////////////////////////////////////////////////////////////
      // if that fetch fails, the update still works, and the event //
      // carries a null old record for it                           //
      ////////////////////////////////////////////////////////////////
      NoPrefetchMemoryModule.failQueries = true;
      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(tableName).withRecord(new QRecord().withValue("id", 2).withValue("name", "deux")));
      NoPrefetchMemoryModule.failQueries = false;
      assertThat(updateOutput.getRecords().get(0).getErrors()).isNullOrEmpty();
      assertThat(CapturingListener.events).hasSize(2);
      assertThat(CapturingListener.events.get(1).getOldRecords()).hasSize(1);
      assertNull(CapturingListener.events.get(1).getOldRecords().get(0));
      assertEquals("deux", TestUtils.queryTable(tableName).get(1).getValueString("name"));
   }

}
