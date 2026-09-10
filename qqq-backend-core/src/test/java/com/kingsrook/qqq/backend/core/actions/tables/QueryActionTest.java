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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.async.AsyncRecordPipeLoop;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStatMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.tables.QQQTablesMetaDataProvider;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.mock.MockQueryAction;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QueryAction
 **
 *******************************************************************************/
class QueryActionTest extends BaseTest
{

   /*******************************************************************************
    ** At the core level, there isn't much that can be asserted, as it uses the
    ** mock implementation - just confirming that all of the "wiring" works.
    **
    *******************************************************************************/
   @Test
   public void test() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName("person");
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertNotNull(queryOutput);

      assertThat(queryOutput.getRecords()).isNotEmpty();
      for(QRecord record : queryOutput.getRecords())
      {
         assertThat(record.getValues()).isNotEmpty();
         assertThat(record.getErrors()).isEmpty();

         ///////////////////////////////////////////////////////////////
         // this SHOULD be empty, based on the default for the should //
         ///////////////////////////////////////////////////////////////
         assertThat(record.getDisplayValues()).isEmpty();

         //////////////////////////////////////////
         // hidden field should not be in record //
         //////////////////////////////////////////
         assertThat(record.getValue("superSecret")).isNull();

         /////////////////////////////////////
         // password field should be masked //
         /////////////////////////////////////
         assertThat(record.getValueString("ssn")).contains("****");
      }

      ////////////////////////////////////////////////////////
      // now flip some fields, re-run, and validate results //
      ////////////////////////////////////////////////////////
      queryInput.setShouldGenerateDisplayValues(true);
      queryInput.setShouldMaskPasswords(false);
      queryInput.setShouldOmitHiddenFields(false);
      assertThat(queryOutput.getRecords()).isNotEmpty();
      queryOutput = new QueryAction().execute(queryInput);
      for(QRecord record : queryOutput.getRecords())
      {
         assertThat(record.getDisplayValues()).isNotEmpty();

         //////////////////////////////////////////
         // hidden field should now be in record //
         //////////////////////////////////////////
         assertThat(record.getValue("superSecret")).isNotNull();

         /////////////////////////////////////
         // password field should be masked //
         /////////////////////////////////////
         Serializable mockValue = MockQueryAction.getMockValue(QContext.getQInstance().getTable("person"), "ssn");
         assertThat(record.getValueString("ssn")).isEqualTo(mockValue);
      }
   }



   /*******************************************************************************
    ** Test running with a recordPipe - using the shape table, which uses the memory
    ** backend, which is known to do an addAll to the query output.
    **
    *******************************************************************************/
   @Test
   public void testRecordPipeShapeTable() throws QException
   {
      TestUtils.insertDefaultShapes(QContext.getQInstance());

      RecordPipe pipe       = new RecordPipe();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_SHAPE);
      queryInput.setRecordPipe(pipe);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertNotNull(queryOutput);

      List<QRecord> records = pipe.consumeAvailableRecords();
      assertThat(records).isNotEmpty();
   }



   /*******************************************************************************
    ** Test running with a recordPipe - using the person table, which uses the mock
    ** backend, which is known to do a single-add (not addAll) to the query output.
    **
    *******************************************************************************/
   @Test
   public void testRecordPipePersonTable() throws QException
   {
      RecordPipe pipe       = new RecordPipe();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput.setRecordPipe(pipe);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertNotNull(queryOutput);

      List<QRecord> records = pipe.consumeAvailableRecords();
      assertThat(records).isNotEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryAssociations() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setFilter(new QQueryFilter());
      queryInput.setIncludeAssociations(true);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      QRecord order0 = queryOutput.getRecords().get(0);
      assertEquals(2, order0.getAssociatedRecords().get("orderLine").size());
      assertEquals(3, order0.getAssociatedRecords().get("extrinsics").size());

      QRecord orderLine00 = order0.getAssociatedRecords().get("orderLine").get(0);
      assertEquals(1, orderLine00.getAssociatedRecords().get("extrinsics").size());
      QRecord orderLine01 = order0.getAssociatedRecords().get("orderLine").get(1);
      assertEquals(2, orderLine01.getAssociatedRecords().get("extrinsics").size());

      QRecord order1 = queryOutput.getRecords().get(1);
      assertEquals(1, order1.getAssociatedRecords().get("orderLine").size());
      assertEquals(1, order1.getAssociatedRecords().get("extrinsics").size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAssociationQueriesPreserveInputSource() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      QContext.getQSession().getUser().setIdReference("association-reader");
      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_LINE_ITEM);
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_LINE_ITEM, "sku", "association-reader");
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC, "value", "association-reader");

      QueryInput userInput = new QueryInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.USER)
         .withFilter(new QQueryFilter(new QFilterCriteria("orderNo", QCriteriaOperator.EQUALS, "ORD123")))
         .withIncludeAssociations(true);
      QRecord userOrder = new QueryAction().execute(userInput).getRecords().get(0);
      assertEquals(2, userOrder.getAssociatedRecords().get("orderLine").size());
      QRecord userLine = userOrder.getAssociatedRecords().get("orderLine").get(0);
      assertEquals(1, userLine.getValueInteger("quantity"));
      assertThat(userLine.getValues()).doesNotContainKey("sku");
      assertEquals(1, userLine.getAssociatedRecords().get("extrinsics").size());
      QRecord userExtrinsic = userLine.getAssociatedRecords().get("extrinsics").get(0);
      assertEquals("LINE-EXT-1.1", userExtrinsic.getValueString("key"));
      assertThat(userExtrinsic.getValues()).doesNotContainKey("value");
      assertEquals(QInputSource.USER, userInput.getInputSource());

      QueryInput systemInput = new QueryInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.SYSTEM)
         .withFilter(new QQueryFilter(new QFilterCriteria("orderNo", QCriteriaOperator.EQUALS, "ORD123")))
         .withIncludeAssociations(true);
      QRecord systemOrder = new QueryAction().execute(systemInput).getRecords().get(0);
      QRecord systemLine = systemOrder.getAssociatedRecords().get("orderLine").get(0);
      assertEquals("BASIC1", systemLine.getValueString("sku"));
      assertEquals("LINE-VAL-1", systemLine.getAssociatedRecords().get("extrinsics").get(0).getValueString("value"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryAssociationsWithPipe() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      RecordPipe pipe       = new RecordPipe();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setRecordPipe(pipe);
      queryInput.setIncludeAssociations(true);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertNotNull(queryOutput);

      List<QRecord> records = pipe.consumeAvailableRecords();
      assertThat(records).isNotEmpty();

      QRecord order0 = records.get(0);
      assertEquals(2, order0.getAssociatedRecords().get("orderLine").size());
      assertEquals(3, order0.getAssociatedRecords().get("extrinsics").size());

      QRecord orderLine00 = order0.getAssociatedRecords().get("orderLine").get(0);
      assertEquals(1, orderLine00.getAssociatedRecords().get("extrinsics").size());
      QRecord orderLine01 = order0.getAssociatedRecords().get("orderLine").get(1);
      assertEquals(2, orderLine01.getAssociatedRecords().get("extrinsics").size());

      QRecord order1 = records.get(1);
      assertEquals(1, order1.getAssociatedRecords().get("orderLine").size());
      assertEquals(1, order1.getAssociatedRecords().get("extrinsics").size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryManyRecordsAssociationsWithPipe() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      insertNOrdersWithAssociations(2500);

      RecordPipe pipe       = new RecordPipe(1000);
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setRecordPipe(pipe);
      queryInput.setIncludeAssociations(true);

      int recordsConsumed = new AsyncRecordPipeLoop().run("Test", null, pipe, (callback) ->
      {
         new QueryAction().execute(queryInput);
         return (true);
      }, () ->
      {
         List<QRecord> records = pipe.consumeAvailableRecords();
         for(QRecord record : records)
         {
            assertEquals(1, record.getAssociatedRecords().get("orderLine").size());
            assertEquals(1, record.getAssociatedRecords().get("extrinsics").size());
         }
         return (records.size());
      });

      assertEquals(2500, recordsConsumed);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryAssociationsNoAssociationNamesToInclude() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setFilter(new QQueryFilter());
      queryInput.setIncludeAssociations(true);
      queryInput.setAssociationNamesToInclude(new ArrayList<>());
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      QRecord order0 = queryOutput.getRecords().get(0);
      assertTrue(CollectionUtils.nullSafeIsEmpty(order0.getAssociatedRecords()));
      QRecord order1 = queryOutput.getRecords().get(1);
      assertTrue(CollectionUtils.nullSafeIsEmpty(order1.getAssociatedRecords()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryAssociationsLimitedAssociationNamesToInclude() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setFilter(new QQueryFilter());
      queryInput.setIncludeAssociations(true);
      queryInput.setAssociationNamesToInclude(List.of("orderLine"));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      QRecord order0 = queryOutput.getRecords().get(0);
      assertEquals(2, order0.getAssociatedRecords().get("orderLine").size());
      assertTrue(CollectionUtils.nullSafeIsEmpty(CollectionUtils.nonNullCollection(order0.getAssociatedRecords().get("extrinsics"))));

      QRecord orderLine00 = order0.getAssociatedRecords().get("orderLine").get(0);
      assertTrue(CollectionUtils.nullSafeIsEmpty(CollectionUtils.nonNullCollection(orderLine00.getAssociatedRecords().get("extrinsics"))));
      QRecord orderLine01 = order0.getAssociatedRecords().get("orderLine").get(1);
      assertTrue(CollectionUtils.nullSafeIsEmpty(CollectionUtils.nonNullCollection(orderLine01.getAssociatedRecords().get("extrinsics"))));

      QRecord order1 = queryOutput.getRecords().get(1);
      assertEquals(1, order1.getAssociatedRecords().get("orderLine").size());
      assertTrue(CollectionUtils.nullSafeIsEmpty(CollectionUtils.nonNullCollection(order1.getAssociatedRecords().get("extrinsics"))));

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryAssociationsLimitedAssociationNamesToIncludeChildTableDuplicatedAssociationNameExcluded() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setFilter(new QQueryFilter());
      queryInput.setIncludeAssociations(true);

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // say that we want extrinsics - but that should only get them from the top-level -- to get them from the child, we need orderLine.extrinsics //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      queryInput.setAssociationNamesToInclude(List.of("orderLine", "extrinsics"));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      QRecord order0 = queryOutput.getRecords().get(0);
      assertEquals(2, order0.getAssociatedRecords().get("orderLine").size());
      assertEquals(3, order0.getAssociatedRecords().get("extrinsics").size());

      QRecord orderLine00 = order0.getAssociatedRecords().get("orderLine").get(0);
      assertTrue(CollectionUtils.nullSafeIsEmpty(CollectionUtils.nonNullCollection(orderLine00.getAssociatedRecords().get("extrinsics"))));
      QRecord orderLine01 = order0.getAssociatedRecords().get("orderLine").get(1);
      assertTrue(CollectionUtils.nullSafeIsEmpty(CollectionUtils.nonNullCollection(orderLine01.getAssociatedRecords().get("extrinsics"))));

      QRecord order1 = queryOutput.getRecords().get(1);
      assertEquals(1, order1.getAssociatedRecords().get("orderLine").size());
      assertEquals(1, order1.getAssociatedRecords().get("extrinsics").size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryAssociationsLimitedAssociationNamesToIncludeChildTableDuplicatedAssociationNameIncluded() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setFilter(new QQueryFilter());
      queryInput.setIncludeAssociations(true);

      /////////////////////////////////////////////////////////////////////////////
      // this time say we want the orderLine.extrinsics - not the top-level ones //
      /////////////////////////////////////////////////////////////////////////////
      queryInput.setAssociationNamesToInclude(List.of("orderLine", "orderLine.extrinsics"));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      QRecord order0 = queryOutput.getRecords().get(0);
      assertEquals(2, order0.getAssociatedRecords().get("orderLine").size());
      assertTrue(CollectionUtils.nullSafeIsEmpty(CollectionUtils.nonNullCollection(order0.getAssociatedRecords().get("extrinsics"))));

      QRecord orderLine00 = order0.getAssociatedRecords().get("orderLine").get(0);
      assertFalse(CollectionUtils.nullSafeIsEmpty(CollectionUtils.nonNullCollection(orderLine00.getAssociatedRecords().get("extrinsics"))));
      QRecord orderLine01 = order0.getAssociatedRecords().get("orderLine").get(1);
      assertFalse(CollectionUtils.nullSafeIsEmpty(CollectionUtils.nonNullCollection(orderLine01.getAssociatedRecords().get("extrinsics"))));

      QRecord order1 = queryOutput.getRecords().get(1);
      assertEquals(1, order1.getAssociatedRecords().get("orderLine").size());
      assertTrue(CollectionUtils.nullSafeIsEmpty(CollectionUtils.nonNullCollection(order1.getAssociatedRecords().get("extrinsics"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryManager() throws QException
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // add tables for QueryStats, and turn them on in the memory backend, then start the query-stat manager //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      QInstance qInstance = QContext.getQInstance();
      qInstance.getBackend(TestUtils.MEMORY_BACKEND_NAME).withCapability(Capability.QUERY_STATS);
      new QQQTablesMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);
      new QueryStatMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      QueryStatManager.getInstance().start(QContext.getQInstance(), QSession::new);

      /////////////////////////////////////////////////////////////////////////////////
      // insert some order "trees", then query them, so some stats will get recorded //
      /////////////////////////////////////////////////////////////////////////////////
      insert2OrdersWith3Lines3LineExtrinsicsAnd4OrderExtrinsicAssociations();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setIncludeAssociations(true);
      queryInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
      QContext.pushAction(queryInput);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      ////////////////////////////////////////////////////////////
      // run the stat manager (so we don't have to wait for it) //
      ////////////////////////////////////////////////////////////
      QueryStatManager.getInstance().storeStatsNow();

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // stat manager expects to be ran in a thread, where it needs to clear context, so reset context after it //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.init(qInstance, new QSession());

      ////////////////////////////////////////////////
      // query to see that some stats were inserted //
      ////////////////////////////////////////////////
      queryInput = new QueryInput();
      queryInput.setTableName(QueryStat.TABLE_NAME);
      QContext.pushAction(queryInput);
      queryOutput = new QueryAction().execute(queryInput);

      ///////////////////////////////////////////////////////////////////////////////////
      // selecting all of those associations should have caused (at least?) 4 queries. //
      // this is the most basic test here, but we'll take it.                          //
      ///////////////////////////////////////////////////////////////////////////////////
      assertThat(queryOutput.getRecords().size()).isGreaterThanOrEqualTo(4);
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
   private static void insertNOrdersWithAssociations(int n) throws QException
   {
      List<QRecord> recordList = new ArrayList<>();
      for(int i = 0; i < n; i++)
      {
         recordList.add(new QRecord().withValue("storeId", 1).withValue("orderNo", "ORD" + i)
            .withAssociatedRecord("orderLine", new QRecord().withValue("sku", "BASIC1").withValue("quantity", 3))
            .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "YOUR-FIELD").withValue("value", "YOUR-VALUE")));
      }

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(recordList);
      new InsertAction().execute(insertInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilterFieldBehaviors() throws QException
   {
      /////////////////////////////////////////////////////////////////////////
      // insert one shape with a mixed-case name, one with an all-lower name //
      /////////////////////////////////////////////////////////////////////////
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_SHAPE).withRecords(List.of(
         new QRecord().withValue("name", "Triangle"),
         new QRecord().withValue("name", "square")
      )));

      ///////////////////////////////////////////////////////////////////////////
      // now set the shape table's name field to have a to-lower-case behavior //
      ///////////////////////////////////////////////////////////////////////////
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_SHAPE);
      QFieldMetaData field     = table.getField("name");
      field.setBehaviors(Set.of(CaseChangeBehavior.TO_LOWER_CASE));

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // confirm that if we query for "Triangle", we can't find it (because query will to-lower-case the criteria) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(0, QueryAction.execute(TestUtils.TABLE_NAME_SHAPE, new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle"))).size());

      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // confirm that if we query for "SQUARE", we do find it (because query will to-lower-case the criteria) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(1, QueryAction.execute(TestUtils.TABLE_NAME_SHAPE, new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "SqUaRe"))).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidateFieldNamesToInclude() throws QException
   {
      /////////////////////////////
      // cases that do not throw //
      /////////////////////////////
      QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withFieldNamesToInclude(null));

      QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withFieldNamesToInclude(Set.of("id")));

      QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withFieldNamesToInclude(Set.of("id", "firstName", "lastName", "birthDate", "modifyDate", "createDate")));

      QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_SHAPE).withSelect(true))
         .withFieldNamesToInclude(Set.of("id", "firstName", "lastName", "birthDate", "modifyDate", "createDate")));

      QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_SHAPE).withSelect(true))
         .withFieldNamesToInclude(Set.of("id", "firstName", TestUtils.TABLE_NAME_SHAPE + ".id", TestUtils.TABLE_NAME_SHAPE + ".noOfSides")));

      QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_SHAPE).withSelect(true).withAlias("s"))
         .withFieldNamesToInclude(Set.of("id", "s.id", "s.noOfSides")));

      //////////////////////////
      // cases that do throw! //
      //////////////////////////
      assertThatThrownBy(() -> QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withFieldNamesToInclude(new HashSet<>())))
         .hasMessageContaining("empty set of fieldNamesToInclude");

      assertThatThrownBy(() -> QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withFieldNamesToInclude(Set.of("notAField"))))
         .hasMessageContaining("1 unrecognized field name: notAField");

      assertThatThrownBy(() -> QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withFieldNamesToInclude(new LinkedHashSet<>(List.of("notAField", "alsoNot")))))
         .hasMessageContaining("2 unrecognized field names: notAField,alsoNot");

      assertThatThrownBy(() -> QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withFieldNamesToInclude(new LinkedHashSet<>(List.of("notAField", "alsoNot", "join.not")))))
         .hasMessageContaining("3 unrecognized field names: notAField,alsoNot,join.not");

      assertThatThrownBy(() -> QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_SHAPE)) // oops, didn't select it!
         .withFieldNamesToInclude(new LinkedHashSet<>(List.of("id", "firstName", TestUtils.TABLE_NAME_SHAPE + ".id", TestUtils.TABLE_NAME_SHAPE + ".noOfSides")))))
         .hasMessageContaining("2 unrecognized field names: shape.id,shape.noOfSides");

      assertThatThrownBy(() -> QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_SHAPE).withSelect(true))
         .withFieldNamesToInclude(Set.of(TestUtils.TABLE_NAME_SHAPE + ".noField"))))
         .hasMessageContaining("1 unrecognized field name: shape.noField");

      assertThatThrownBy(() -> QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_SHAPE).withSelect(true).withAlias("s")) // oops, not using alias
         .withFieldNamesToInclude(new LinkedHashSet<>(List.of("id", "firstName", TestUtils.TABLE_NAME_SHAPE + ".id", "s.noOfSides")))))
         .hasMessageContaining("1 unrecognized field name: shape.id");

      assertThatThrownBy(() -> QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_SHAPE).withSelect(true))
         .withFieldNamesToInclude(new LinkedHashSet<>(List.of("id", "firstName", TestUtils.TABLE_NAME_SHAPE + ".id", "noOfSides")))))
         .hasMessageContaining("1 unrecognized field name: noOfSides");

      assertThatThrownBy(() -> QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withQueryJoin(new QueryJoin("noJoinTable").withSelect(true))
         .withFieldNamesToInclude(Set.of("noJoinTable.id"))))
         .hasMessageContaining("Unrecognized name for join table: noJoinTable");

      assertThatThrownBy(() -> QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withFieldNamesToInclude(Set.of("noJoin.id"))))
         .hasMessageContaining("1 unrecognized field name: noJoin.id");

      assertThatThrownBy(() -> QueryAction.validateFieldNamesToInclude(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withFieldNamesToInclude(Set.of("noDb.noJoin.id"))))
         .hasMessageContaining("1 unrecognized field name: noDb.noJoin.id");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalidFieldNamesInFilter() throws QException
   {
      assertThatThrownBy(() -> QueryAction.execute(TestUtils.TABLE_NAME_SHAPE, new QQueryFilter(new QFilterCriteria("notAField", QCriteriaOperator.IS_NOT_BLANK))))
         .hasMessageContaining("1 unrecognized field name: notAField");

      assertThatThrownBy(() -> QueryAction.execute(TestUtils.TABLE_NAME_SHAPE, new QQueryFilter()
         .withSubFilter(new QQueryFilter(new QFilterCriteria("notAField", QCriteriaOperator.IS_NOT_BLANK)))))
         .hasMessageContaining("1 unrecognized field name: notAField");

      assertThatThrownBy(() -> QueryAction.execute(TestUtils.TABLE_NAME_SHAPE, new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("stillNotAField"))))
         .hasMessageContaining("1 unrecognized field name: stillNotAField");

      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      // and we'll assume that the positive (non-throwing cases) are covered in all the other tests in here. //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTablePersonalization() throws QException
   {
      QContext.getQSession().getUser().setIdReference("jdoe");
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_PERSON, "noOfShoes", QContext.getQSession().getUser().getIdReference());

      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_SHAPE);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_SHAPE, "noOfSides", QContext.getQSession().getUser().getIdReference());

      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("firstName", "Darin")));

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // assert that validateFieldNamesToInclude works as expected (e.g., considers personalized-removed fields are not valid fields //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON).withInputSource(QInputSource.USER)
         .withFieldNamesToInclude(Set.of("id", "noOfShoes"))))
         .hasMessageContaining("1 unrecognized field name: noOfShoes");

      assertThatThrownBy(() -> new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON).withInputSource(QInputSource.USER)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_SHAPE).withSelect(true))
         .withFieldNamesToInclude(Set.of(TestUtils.TABLE_NAME_SHAPE + ".noOfSides", TestUtils.TABLE_NAME_SHAPE + ".id"))))
         .hasMessageContaining("1 unrecognized field name: shape.noOfSides");

      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // customize firstName field to do a to-upper-case                                                   //
      // this is verifying that QueryAction.postRecordActions has access to the personalized tableMetaData //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      QueryInput queryInput = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withInputSource(QInputSource.USER);
      assertEquals("Darin", new QueryAction().execute(queryInput).getRecords().get(0).getValueString("firstName"));
      ExamplePersonalizer.addFieldToAddForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY,
         QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getField("firstName").clone().withBehavior(CaseChangeBehavior.TO_UPPER_CASE),
         QContext.getQSession().getUser().getIdReference());
      assertEquals("DARIN", new QueryAction().execute(queryInput).getRecords().get(0).getValueString("firstName"));
   }

}
