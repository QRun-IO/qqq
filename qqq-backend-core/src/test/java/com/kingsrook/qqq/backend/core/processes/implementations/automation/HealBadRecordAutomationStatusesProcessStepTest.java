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

package com.kingsrook.qqq.backend.core.processes.implementations.automation;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationStatusUpdater;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for HealBadRecordAutomationStatusesProcessStep 
 *******************************************************************************/
class HealBadRecordAutomationStatusesProcessStepTest extends BaseTest
{
   private static String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTwoFailedUpdates() throws QException
   {
      new InsertAction().execute(new InsertInput(tableName).withRecords(List.of(new QRecord(), new QRecord())));
      List<QRecord> records = queryAllRecords();
      RecordAutomationStatusUpdater.setAutomationStatusInRecordsAndUpdate(QContext.getQInstance().getTable(tableName), records, AutomationStatus.FAILED_UPDATE_AUTOMATIONS, null);

      assertThat(queryAllRecords()).allMatch(r -> AutomationStatus.FAILED_UPDATE_AUTOMATIONS.getId().equals(getAutomationStatus(r)));

      RunBackendStepOutput output = runProcessStep();

      assertEquals(2, output.getValueInteger("totalRecordsUpdated"));
      assertThat(queryAllRecords()).allMatch(r -> AutomationStatus.PENDING_UPDATE_AUTOMATIONS.getId().equals(getAutomationStatus(r)));
   }



   /*******************************************************************************
    ** at one point, when the review step go added, we were double-adding records
    ** to the output/result screen.  This test verifies, if we run the full process
    ** that that doesn't happen.
    *******************************************************************************/
   @Test
   void testTwoFailedUpdatesFullProcess() throws QException
   {
      QContext.getQInstance().addProcess(new HealBadRecordAutomationStatusesProcessStep().produce(QContext.getQInstance()));

      new InsertAction().execute(new InsertInput(tableName).withRecords(List.of(new QRecord(), new QRecord())));
      List<QRecord> records = queryAllRecords();
      RecordAutomationStatusUpdater.setAutomationStatusInRecordsAndUpdate(QContext.getQInstance().getTable(tableName), records, AutomationStatus.FAILED_UPDATE_AUTOMATIONS, null);

      assertThat(queryAllRecords()).allMatch(r -> AutomationStatus.FAILED_UPDATE_AUTOMATIONS.getId().equals(getAutomationStatus(r)));

      RunProcessInput input = new RunProcessInput();
      input.setProcessName(HealBadRecordAutomationStatusesProcessStep.NAME);
      input.setCallback(QProcessCallbackFactory.forFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, records.stream().map(r -> r.getValue("id")).toList()))));
      RunProcessAction runProcessAction = new RunProcessAction();
      RunProcessOutput runProcessOutput = runProcessAction.execute(input);

      input.setStartAfterStep(runProcessOutput.getProcessState().getNextStepName().get());
      runProcessOutput = runProcessAction.execute(input);

      input.setStartAfterStep(runProcessOutput.getProcessState().getNextStepName().get());
      runProcessOutput = runProcessAction.execute(input);

      List<QRecord> outputRecords = runProcessOutput.getProcessState().getRecords();
      assertEquals(1, outputRecords.size());
      assertEquals(2, outputRecords.get(0).getValueInteger("count"));

      assertThat(queryAllRecords()).allMatch(r -> AutomationStatus.PENDING_UPDATE_AUTOMATIONS.getId().equals(getAutomationStatus(r)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneFailedUpdateOneFailedInsert() throws QException
   {
      new InsertAction().execute(new InsertInput(tableName).withRecords(List.of(new QRecord(), new QRecord())));
      List<QRecord> records = queryAllRecords();
      RecordAutomationStatusUpdater.setAutomationStatusInRecordsAndUpdate(QContext.getQInstance().getTable(tableName), records.subList(0, 1), AutomationStatus.FAILED_UPDATE_AUTOMATIONS, null);
      RecordAutomationStatusUpdater.setAutomationStatusInRecordsAndUpdate(QContext.getQInstance().getTable(tableName), records.subList(1, 2), AutomationStatus.FAILED_INSERT_AUTOMATIONS, null);

      assertThat(queryAllRecords())
         .anyMatch(r -> AutomationStatus.FAILED_UPDATE_AUTOMATIONS.getId().equals(getAutomationStatus(r)))
         .anyMatch(r -> AutomationStatus.FAILED_INSERT_AUTOMATIONS.getId().equals(getAutomationStatus(r)));

      RunBackendStepOutput output = runProcessStep();

      assertEquals(2, output.getValueInteger("totalRecordsUpdated"));
      assertThat(queryAllRecords())
         .anyMatch(r -> AutomationStatus.PENDING_UPDATE_AUTOMATIONS.getId().equals(getAutomationStatus(r)))
         .anyMatch(r -> AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId().equals(getAutomationStatus(r)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOldRunningUpdates() throws QException
   {
      /////////////////////////////////////////////////
      // temporarily remove the modify-date behavior //
      /////////////////////////////////////////////////
      QContext.getQInstance().getTable(tableName).getField("modifyDate").withBehavior(DynamicDefaultValueBehavior.NONE);

      //////////////////////////////////////////////////////////////////////////
      // insert 2 records, one with an old modifyDate, one with 6 minutes ago //
      //////////////////////////////////////////////////////////////////////////
      new InsertAction().execute(new InsertInput(tableName).withRecords(List.of(
         new QRecord().withValue("firstName", "Darin").withValue("modifyDate", Instant.parse("2023-01-01T12:00:00Z")),
         new QRecord().withValue("firstName", "Tim").withValue("modifyDate", Instant.now().minus(6, ChronoUnit.MINUTES))
      )));
      List<QRecord> records = queryAllRecords();

      ///////////////////////////////////////////////////////
      // put those records both in status: running-updates //
      ///////////////////////////////////////////////////////
      RecordAutomationStatusUpdater.setAutomationStatusInRecordsAndUpdate(QContext.getQInstance().getTable(tableName), records, AutomationStatus.RUNNING_UPDATE_AUTOMATIONS, null);

      assertThat(queryAllRecords())
         .allMatch(r -> AutomationStatus.RUNNING_UPDATE_AUTOMATIONS.getId().equals(getAutomationStatus(r)));

      /////////////////////////////////////
      // restore the modifyDate behavior //
      /////////////////////////////////////
      QContext.getQInstance().getTable(tableName).getField("modifyDate").withBehavior(DynamicDefaultValueBehavior.MODIFY_DATE);

      /////////////////////////
      // run code under test //
      /////////////////////////
      RunBackendStepOutput output = runProcessStep();

      /////////////////////////////////////////////////////////////////////////////////////////////
      // assert we updated 1 (the old one) to pending-updates, the other left as running-updates //
      /////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(1, output.getValueInteger("totalRecordsUpdated"));
      assertThat(queryAllRecords())
         .anyMatch(r -> AutomationStatus.PENDING_UPDATE_AUTOMATIONS.getId().equals(getAutomationStatus(r)))
         .anyMatch(r -> AutomationStatus.RUNNING_UPDATE_AUTOMATIONS.getId().equals(getAutomationStatus(r)));

      /////////////////////////////////
      // re-run, with 3-minute limit //
      /////////////////////////////////
      output = runProcessStep(new RunBackendStepInput().withValues(Map.of("minutesOldLimit", 3)));

      /////////////////////////////////////////////////////////////////
      // assert that one updated too, and all are now pending-update //
      /////////////////////////////////////////////////////////////////
      assertEquals(1, output.getValueInteger("totalRecordsUpdated"));
      assertThat(queryAllRecords())
         .allMatch(r -> AutomationStatus.PENDING_UPDATE_AUTOMATIONS.getId().equals(getAutomationStatus(r)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOldRunningInserts() throws QException
   {
      ///////////////////////////////////////////////////////////////
      // temporarily remove the create-date & modify-date behavior //
      ///////////////////////////////////////////////////////////////
      QContext.getQInstance().getTable(tableName).getField("modifyDate").withBehavior(DynamicDefaultValueBehavior.NONE);
      QContext.getQInstance().getTable(tableName).getField("createDate").withBehavior(DynamicDefaultValueBehavior.NONE);

      //////////////////////////////////////////////////////////////////////////
      // insert 2 records, one with an old createDate, one with 6 minutes ago //
      // but set both with modifyDate very recent                             //
      //////////////////////////////////////////////////////////////////////////
      Instant old    = Instant.parse("2023-01-01T12:00:00Z");
      Instant recent = Instant.now().minus(6, ChronoUnit.MINUTES);
      new InsertAction().execute(new InsertInput(tableName).withRecords(List.of(
         new QRecord().withValue("firstName", "Darin").withValue("createDate", old).withValue("modifyDate", recent),
         new QRecord().withValue("firstName", "Tim").withValue("createDate", recent).withValue("modifyDate", recent)
      )));
      List<QRecord> records = queryAllRecords();

      ///////////////////////////////////////////////////////
      // put those records both in status: running-inserts //
      ///////////////////////////////////////////////////////
      RecordAutomationStatusUpdater.setAutomationStatusInRecordsAndUpdate(QContext.getQInstance().getTable(tableName), records, AutomationStatus.RUNNING_INSERT_AUTOMATIONS, null);

      assertThat(queryAllRecords())
         .allMatch(r -> AutomationStatus.RUNNING_INSERT_AUTOMATIONS.getId().equals(getAutomationStatus(r)));

      //////////////////////////////////////////////////
      // restore the createDate & modifyDate behavior //
      //////////////////////////////////////////////////
      QContext.getQInstance().getTable(tableName).getField("modifyDate").withBehavior(DynamicDefaultValueBehavior.MODIFY_DATE);
      QContext.getQInstance().getTable(tableName).getField("createDate").withBehavior(DynamicDefaultValueBehavior.CREATE_DATE);

      /////////////////////////
      // run code under test //
      /////////////////////////
      RunBackendStepOutput output = runProcessStep();

      /////////////////////////////////////////////////////////////////////////////////////////////
      // assert we updated 1 (the old one) to pending-inserts, the other left as running-inserts //
      /////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(1, output.getValueInteger("totalRecordsUpdated"));
      assertThat(queryAllRecords())
         .anyMatch(r -> AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId().equals(getAutomationStatus(r)))
         .anyMatch(r -> AutomationStatus.RUNNING_INSERT_AUTOMATIONS.getId().equals(getAutomationStatus(r)));

      /////////////////////////////////
      // re-run, with 3-minute limit //
      /////////////////////////////////
      output = runProcessStep(new RunBackendStepInput().withValues(Map.of("minutesOldLimit", 3)));

      /////////////////////////////////////////////////////////////////
      // assert that one updated too, and all are now pending-insert //
      /////////////////////////////////////////////////////////////////
      assertEquals(1, output.getValueInteger("totalRecordsUpdated"));
      assertThat(queryAllRecords())
         .allMatch(r -> AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId().equals(getAutomationStatus(r)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Integer getAutomationStatus(QRecord r)
   {
      return r.getValueInteger(TestUtils.standardQqqAutomationStatusField().getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> queryAllRecords() throws QException
   {
      return new QueryAction().execute(new QueryInput(tableName)).getRecords();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RunBackendStepOutput runProcessStep() throws QException
   {
      RunBackendStepInput input = new RunBackendStepInput();
      return runProcessStep(input);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RunBackendStepOutput runProcessStep(RunBackendStepInput input) throws QException
   {
      RunBackendStepOutput output = new RunBackendStepOutput();
      new HealBadRecordAutomationStatusesProcessStep().run(input, output);
      return output;
   }

}