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


import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationHandlerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.automation.RecordAutomationInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TriggerEvent;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.actions.automation.polling.PollingAutomationPerTableRunnerTest.runAllTableActions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


/*******************************************************************************
 ** Test for the case where:
 ** - inserting into a main table and a child table, and the child table has a
 ** post-insert customizer, which mo
 *******************************************************************************/
public class PollingAutomationPerTableRunnerAutomtationUpdatingSelfAvoidInfiniteLoopTest extends BaseTest
{
   private static boolean didFailInThread = false;

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
   @BeforeEach
   void beforeEach()
   {
      didFailInThread = false;
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
               .withCodeReference(new QCodeReference(OrderPostInsertAndUpdateAction.class)))
            .withAction(new TableAutomationAction()
               .withName("orderPostUpdateAction")
               .withTriggerEvent(TriggerEvent.POST_UPDATE)
               .withCodeReference(new QCodeReference(OrderPostInsertAndUpdateAction.class))));

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(new QRecord().withValue("orderNo", "10101").withValue("total", new BigDecimal(1))));
      new InsertAction().execute(insertInput);

      //////////////////////////////////////////////////////
      // make sure the order is in pending-inserts status //
      //////////////////////////////////////////////////////
      {
         QRecord order = new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_ORDER).withPrimaryKey(1));
         assertEquals(AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId(), order.getValue(TestUtils.standardQqqAutomationStatusField().getName()));
         assertEquals(new BigDecimal(1), order.getValueBigDecimal("total"));
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // run automations - that should update the order via the automation - but leave status as OK //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      runAllTableActions(QContext.getQInstance());
      assertFalse(didFailInThread, "A failure condition happened in the automation sub-thread.  Check System.out for message.");

      {
         QRecord order = new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_ORDER).withPrimaryKey(1));
         assertEquals(AutomationStatus.OK.getId(), order.getValue(TestUtils.standardQqqAutomationStatusField().getName()));
         assertEquals(new BigDecimal(2), order.getValueBigDecimal("total"));
      }

      //////////////////////////////////////////////////////////////////
      // now update the order, verify status moves to pending-updates //
      //////////////////////////////////////////////////////////////////
      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_ORDER).withRecord(new QRecord()
         .withValue("id", 1)
         .withValue("storeId", "x")));

      {
         QRecord order = new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_ORDER).withPrimaryKey(1));
         assertEquals(AutomationStatus.PENDING_UPDATE_AUTOMATIONS.getId(), order.getValue(TestUtils.standardQqqAutomationStatusField().getName()));
         assertEquals(new BigDecimal(2), order.getValueBigDecimal("total"));
         assertEquals("x", order.getValueString("storeId"));
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // run automations - that should update the order via the automation - but leave status as OK //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      runAllTableActions(QContext.getQInstance());
      assertFalse(didFailInThread, "A failure condition happened in the automation sub-thread.  Check System.out for message.");

      {
         QRecord order = new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_ORDER).withPrimaryKey(1));
         assertEquals(AutomationStatus.OK.getId(), order.getValue(TestUtils.standardQqqAutomationStatusField().getName()));
         assertEquals(new BigDecimal(3), order.getValueBigDecimal("total"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class OrderPostInsertAndUpdateAction implements RecordAutomationHandlerInterface
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void execute(RecordAutomationInput recordAutomationInput) throws QException
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // launch a new thread, to make sure we avoid the "stack contains automations" check in RecordAutomationStatusUpdater //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         CapturedContext capturedContext = QContext.capture();
         for(QRecord record : recordAutomationInput.getRecordList())
         {
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            Future<?> submit = service.submit(() ->
            {
               QContext.init(capturedContext);
               try
               {
                  new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_ORDER).withRecord(new QRecord()
                     .withValue("id", record.getValue("id"))
                     .withValue("total", record.getValueBigDecimal("total").add(new BigDecimal(1)))
                  ));

                  ///////////////////////////////////////////////////////////////////
                  // make sure that update action didn't change the order's status //
                  ///////////////////////////////////////////////////////////////////
                  QRecord order = new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_ORDER).withPrimaryKey(1));
                  if(Objects.equals(AutomationStatus.PENDING_UPDATE_AUTOMATIONS.getId(), order.getValue(TestUtils.standardQqqAutomationStatusField().getName())))
                  {
                     System.out.println("Failing test - expected status to not be [PENDING_UPDATE_AUTOMATIONS], but it was.");
                     didFailInThread = true;
                  }
                  assertNotEquals(AutomationStatus.PENDING_UPDATE_AUTOMATIONS.getId(), order.getValue(TestUtils.standardQqqAutomationStatusField().getName()));
               }
               catch(QException e)
               {
                  e.printStackTrace();
               }
               finally
               {
                  QContext.clear();
               }
            });

            while(!submit.isDone())
            {
            }
         }
      }
   }

}
