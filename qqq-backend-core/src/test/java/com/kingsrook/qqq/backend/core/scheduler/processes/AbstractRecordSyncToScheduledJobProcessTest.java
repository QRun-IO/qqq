/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.scheduler.processes;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.common.TimeZonePossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerTestUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for AbstractRecordSyncToScheduledJobProcess 
 *******************************************************************************/
class AbstractRecordSyncToScheduledJobProcessTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new ScheduledJobsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      qInstance.addProcess(new SyncPersonToScheduledJobProcess().produce(qInstance));
      qInstance.addPossibleValueSource(new TimeZonePossibleValueSourceMetaDataProvider().produce());
      QScheduleManager.initInstance(qInstance, QSystemUserSession::new);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      SchedulerTestUtils.afterEach();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QRecord person = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
            .withRecord(new QRecord().withValue("id", 1701).withValue("firstName", "Darin")))
         .getRecords().get(0);

      RunProcessInput input = new RunProcessInput();
      input.setProcessName(SyncPersonToScheduledJobProcess.class.getSimpleName());
      input.setCallback(QProcessCallbackFactory.forRecord(person));
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      new RunProcessAction().execute(input);

      List<ScheduledJob> scheduledJobs = new QueryAction().execute(new QueryInput(ScheduledJob.TABLE_NAME).withIncludeAssociations(true)).getRecordEntities(ScheduledJob.class);
      assertEquals(1, scheduledJobs.size());
      ScheduledJob scheduledJob = scheduledJobs.get(0);
      assertEquals(TestUtils.TABLE_NAME_PERSON_MEMORY, scheduledJob.getForeignKeyType());
      assertEquals(person.getValueString("id"), scheduledJob.getForeignKeyValue());
      assertEquals(60, scheduledJob.getRepeatSeconds());
      assertTrue(scheduledJob.getIsActive());
      assertEquals(4, scheduledJob.getJobParameters().size());
      assertEquals(TestUtils.PROCESS_NAME_GREET_PEOPLE, scheduledJob.getJobParameters().stream().filter(jp -> jp.getKey().equals("processName")).findFirst().get().getValue());
      assertEquals("true", scheduledJob.getJobParameters().stream().filter(jp -> jp.getKey().equals("isScheduledJob")).findFirst().get().getValue());
      assertEquals(person.getValueString("id"), scheduledJob.getJobParameters().stream().filter(jp -> jp.getKey().equals(TestUtils.TABLE_NAME_PERSON_MEMORY + "Id")).findFirst().get().getValue());
      assertEquals(person.getValueString("id"), scheduledJob.getJobParameters().stream().filter(jp -> jp.getKey().equals("recordId")).findFirst().get().getValue());

      /////////////////////////////////////////////////////////////////////////////////////////
      // re-run - it should update the repeat seconds (per custom logic in test class below) //
      /////////////////////////////////////////////////////////////////////////////////////////
      new RunProcessAction().execute(input);
      scheduledJobs = new QueryAction().execute(new QueryInput(ScheduledJob.TABLE_NAME).withIncludeAssociations(true)).getRecordEntities(ScheduledJob.class);
      assertEquals(1, scheduledJobs.size());
      scheduledJob = scheduledJobs.get(0);
      assertEquals(61, scheduledJob.getRepeatSeconds());
   }




   /***************************************************************************
    **
    ***************************************************************************/
   public static class SyncPersonToScheduledJobProcess extends AbstractRecordSyncToScheduledJobProcess
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected ScheduledJob customizeScheduledJob(ScheduledJob scheduledJob, QRecord sourceRecord) throws QException
      {
         if(scheduledJob.getRepeatSeconds() != null)
         {
            ///////////////////////////////////
            // increment by one on an update //
            ///////////////////////////////////
            return scheduledJob.withRepeatSeconds(scheduledJob.getRepeatSeconds() + 1);
         }
         else
         {
            return scheduledJob.withRepeatSeconds(60);
         }
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected String getScheduledJobForeignKeyType()
      {
         return TestUtils.TABLE_NAME_PERSON_MEMORY;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected String getRecordForeignKeyFieldName()
      {
         return "id";
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected String getRecordForeignKeyPossibleValueSourceName()
      {
         return TestUtils.TABLE_NAME_PERSON_MEMORY;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected String getSourceTableName()
      {
         return TestUtils.TABLE_NAME_PERSON_MEMORY;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected String getProcessNameScheduledJobParameter()
      {
         return TestUtils.PROCESS_NAME_GREET_PEOPLE;
      }
   }

}