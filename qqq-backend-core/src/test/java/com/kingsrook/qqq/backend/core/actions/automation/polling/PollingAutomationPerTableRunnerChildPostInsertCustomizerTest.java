/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.automation.polling;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationHandlerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostInsertCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.automation.RecordAutomationInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TriggerEvent;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.actions.automation.polling.PollingAutomationPerTableRunnerTest.runAllTableActions;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Test for the case where:
 ** - inserting into a main table and a child table, and the child table has a
 ** post-insert customizer, which mo
 *******************************************************************************/
public class PollingAutomationPerTableRunnerChildPostInsertCustomizerTest extends BaseTest
{

   static
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // we can set this property to revert to the behavior that existed before this test was written. //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // System.setProperty("qqq.recordAutomationStatusUpdater.skipPreUpdateFetch", "true");
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      ////////////////////////////////////
      // add automations to order table //
      ////////////////////////////////////
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER)
         .withField(TestUtils.standardQqqAutomationStatusField())
         .withAutomationDetails(TestUtils.defineStandardAutomationDetails()
            .withAction(new TableAutomationAction()
               .withName("orderPostInsertAction")
               .withTriggerEvent(TriggerEvent.POST_INSERT)
               .withCodeReference(new QCodeReference(OrderPostInsertAction.class))
            ));

      ///////////////////////////////////////////////////////////////////////////
      // add a post-insert customizer to line-ite table (child of order table) //
      ///////////////////////////////////////////////////////////////////////////
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_LINE_ITEM)
         .withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(LineItemPostInsertCustomizer.class));

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(new QRecord()
         .withValue("orderNo", "10101")
         .withAssociatedRecord("orderLine", new QRecord()
            .withValue("sku", "ABC")
            .withValue("quantity", 1))));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure the order is in pending-inserts status (at one time, a bug meant that it wouldn't have been...) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         QRecord order = new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_ORDER).withPrimaryKey(1));
         assertEquals(AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId(), order.getValue(TestUtils.standardQqqAutomationStatusField().getName()));
         assertEquals(new BigDecimal(1), order.getValueBigDecimal("total"));
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // run automations - that should... insert a second line item, but should leave the order in //
      // automation-status = OK, to avoid perpetual re-running                                     //
      // the line-item post-inserter should run a second time, making the order's total = 2        //
      ///////////////////////////////////////////////////////////////////////////////////////////////
      runAllTableActions(QContext.getQInstance());

      {
         QRecord order = new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_ORDER).withPrimaryKey(1));
         assertEquals(AutomationStatus.OK.getId(), order.getValue(TestUtils.standardQqqAutomationStatusField().getName()));
         assertEquals(new BigDecimal(2), order.getValueBigDecimal("total"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class OrderPostInsertAction implements RecordAutomationHandlerInterface
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void execute(RecordAutomationInput recordAutomationInput) throws QException
      {
         ///////////////////////////////////////
         // add a new line item to the orders //
         ///////////////////////////////////////
         List<QRecord> lineItemsToInsert = new ArrayList<>();
         for(QRecord record : recordAutomationInput.getRecordList())
         {
            lineItemsToInsert.add(new QRecord()
               .withValue("orderId", record.getValue("id"))
               .withValue("sku", UUID.randomUUID())
               .withValue("quantity", 1)
            );
         }
         new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_LINE_ITEM).withRecords(lineItemsToInsert));

      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class LineItemPostInsertCustomizer extends AbstractPostInsertCustomizer
   {
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         //////////////////////////////////
         // count line items by order id //
         //////////////////////////////////
         Set<Serializable> orderIds = records.stream().map(r -> r.getValue("orderId")).collect(Collectors.toSet());

         GroupBy   groupByOrderId = new GroupBy(QFieldType.STRING, "orderId");
         Aggregate countId        = new Aggregate("id", AggregateOperator.COUNT);

         AggregateInput aggregateInput = new AggregateInput();
         aggregateInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM);
         aggregateInput.setFilter(new QQueryFilter(new QFilterCriteria("orderId", QCriteriaOperator.IN, orderIds)));
         aggregateInput.withGroupBy(groupByOrderId);
         aggregateInput.withAggregate(countId);
         AggregateOutput       aggregateOutput = new AggregateAction().execute(aggregateInput);
         Map<Integer, Integer> countByOrderId  = new HashMap<>();
         for(AggregateResult result : aggregateOutput.getResults())
         {
            countByOrderId.put(ValueUtils.getValueAsInteger(result.getGroupByValue(groupByOrderId)), ValueUtils.getValueAsInteger(result.getAggregateValue(countId)));
         }

         ///////////////////////////////////
         // update the order total fields //
         // s/b in bulk, but, meh         //
         ///////////////////////////////////
         for(Integer orderId : countByOrderId.keySet())
         {
            UpdateInput updateInput = new UpdateInput();
            updateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
            updateInput.setRecords(List.of(new QRecord()
               .withValue("id", orderId)
               .withValue("total", new BigDecimal(countByOrderId.get(orderId)))));
            new UpdateAction().execute(updateInput);
         }

         return (records);
      }
   }

}
