/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.automation;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.automation.TableTrigger;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TriggerEvent;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for RecordAutomationStatusUpdater 
 *******************************************************************************/
class RecordAutomationStatusUpdaterTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCanWeSkipPendingAndGoToOkay() throws QException
   {
      QContext.getQInstance()
         .addTable(new ScriptsMetaDataProvider().defineTableTriggerTable(TestUtils.MEMORY_BACKEND_NAME));

      ////////////////////////////////////////////////////////////
      // define tables with various automations and/or triggers //
      ////////////////////////////////////////////////////////////
      QTableMetaData tableWithNoAutomations = new QTableMetaData()
         .withName("tableWithNoAutomations");

      QTableMetaData tableWithInsertAutomation = new QTableMetaData()
         .withName("tableWithInsertAutomation")
         .withAutomationDetails(new QTableAutomationDetails()
            .withAction(new TableAutomationAction().withTriggerEvent(TriggerEvent.POST_INSERT)));

      QTableMetaData tableWithUpdateAutomation = new QTableMetaData()
         .withName("tableWithUpdateAutomation")
         .withAutomationDetails(new QTableAutomationDetails()
            .withAction(new TableAutomationAction()
               .withTriggerEvent(TriggerEvent.POST_UPDATE)));

      QTableMetaData tableWithInsertAndUpdateAutomations = new QTableMetaData()
         .withName("tableWithInsertAndUpdateAutomations ")
         .withAutomationDetails(new QTableAutomationDetails()
            .withAction(new TableAutomationAction().withTriggerEvent(TriggerEvent.POST_INSERT))
            .withAction(new TableAutomationAction().withTriggerEvent(TriggerEvent.POST_UPDATE)));

      QTableMetaData tableWithInsertTrigger = new QTableMetaData()
         .withName("tableWithInsertTrigger");
      new InsertAction().execute(new InsertInput(TableTrigger.TABLE_NAME)
         .withRecordEntity(new TableTrigger().withTableName(tableWithInsertTrigger.getName()).withScriptId(-1).withPostInsert(true).withPostUpdate(false)));

      QTableMetaData tableWithUpdateTrigger = new QTableMetaData()
         .withName("tableWithUpdateTrigger");
      new InsertAction().execute(new InsertInput(TableTrigger.TABLE_NAME)
         .withRecordEntity(new TableTrigger().withTableName(tableWithUpdateTrigger.getName()).withScriptId(-1).withPostInsert(false).withPostUpdate(true)));

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // tests for going to PENDING_INSERT.                                                                                     //
      // we should be allowed to skip and go to OK (return true) if the table does not have insert automations or triggers      //
      // we should NOT be allowed to skip and go to OK (return false) if the table does NOT have insert automations or triggers //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertTrue(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithNoAutomations, AutomationStatus.PENDING_INSERT_AUTOMATIONS));
      assertFalse(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithInsertAutomation, AutomationStatus.PENDING_INSERT_AUTOMATIONS));
      assertTrue(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithUpdateAutomation, AutomationStatus.PENDING_INSERT_AUTOMATIONS));
      assertFalse(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithInsertAndUpdateAutomations, AutomationStatus.PENDING_INSERT_AUTOMATIONS));
      assertFalse(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithInsertTrigger, AutomationStatus.PENDING_INSERT_AUTOMATIONS));
      assertTrue(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithUpdateTrigger, AutomationStatus.PENDING_INSERT_AUTOMATIONS));

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // tests for going to PENDING_UPDATE.                                                                                     //
      // we should be allowed to skip and go to OK (return true) if the table does not have update automations or triggers      //
      // we should NOT be allowed to skip and go to OK (return false) if the table does NOT have insert automations or triggers //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertTrue(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithNoAutomations, AutomationStatus.PENDING_UPDATE_AUTOMATIONS));
      assertTrue(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithInsertAutomation, AutomationStatus.PENDING_UPDATE_AUTOMATIONS));
      assertFalse(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithUpdateAutomation, AutomationStatus.PENDING_UPDATE_AUTOMATIONS));
      assertFalse(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithInsertAndUpdateAutomations, AutomationStatus.PENDING_UPDATE_AUTOMATIONS));
      assertTrue(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithInsertTrigger, AutomationStatus.PENDING_UPDATE_AUTOMATIONS));
      assertFalse(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(tableWithUpdateTrigger, AutomationStatus.PENDING_UPDATE_AUTOMATIONS));

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // tests for going to non-PENDING states                                                                            //
      // this function should NEVER return true for skipping pending if the target state (2nd arg) isn't a pending state. //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      for(AutomationStatus automationStatus : List.of(AutomationStatus.RUNNING_INSERT_AUTOMATIONS, AutomationStatus.RUNNING_UPDATE_AUTOMATIONS, AutomationStatus.FAILED_INSERT_AUTOMATIONS, AutomationStatus.FAILED_UPDATE_AUTOMATIONS, AutomationStatus.OK))
      {
         for(QTableMetaData table : List.of(tableWithNoAutomations, tableWithInsertAutomation, tableWithUpdateAutomation, tableWithInsertAndUpdateAutomations, tableWithInsertTrigger, tableWithUpdateTrigger))
         {
            assertFalse(RecordAutomationStatusUpdater.canWeSkipPendingAndGoToOkay(table, automationStatus), "Should never be okay to skip pending and go to OK (because we weren't going to pending).  table=[" + table.getName() + "], status=[" + automationStatus + "]");
         }
      }
   }

}