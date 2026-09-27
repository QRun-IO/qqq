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

package com.kingsrook.qqq.backend.core.scheduler.quartz;


import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerTestUtils;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerTestUtils.BasicStep;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.SchedulableType;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.BasicSchedulableIdentity;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableSQSQueueRunner;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableTableAutomationsRunner;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QuartzScheduler 
 *******************************************************************************/
class QuartzSchedulerTest extends BaseTest
{

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
   void test() throws Exception
   {
      try
      {
         QInstance qInstance = QContext.getQInstance();
         QuartzTestUtils.setupInstanceForQuartzTests();

         //////////////////////////////////////////////////////////////////////////////////////////////////////
         // set these runners to use collecting logger, so we can assert that they did run, and didn't throw //
         //////////////////////////////////////////////////////////////////////////////////////////////////////
         QCollectingLogger quartzSqsPollerJobLog        = QLogger.activateCollectingLoggerForClass(SchedulableSQSQueueRunner.class);
         QCollectingLogger quartzTableAutomationsJobLog = QLogger.activateCollectingLoggerForClass(SchedulableTableAutomationsRunner.class);

         //////////////////////////////////////////
         // add a process we can run and observe //
         //////////////////////////////////////////
         qInstance.addProcess(SchedulerTestUtils.buildTestProcess("testScheduledProcess", QuartzTestUtils.QUARTZ_SCHEDULER_NAME));

         /////////////////////////////////////////////////////////////////////
         // start the schedule manager, then ask it to set up all schedules //
         /////////////////////////////////////////////////////////////////////
         QSession         qSession         = QContext.getQSession();
         QScheduleManager qScheduleManager = QScheduleManager.initInstance(qInstance, () -> qSession);
         qScheduleManager.start();
         qScheduleManager.setupAllSchedules();

         //////////////////////////////////////////////////
         // give a moment for the job to run a few times //
         //////////////////////////////////////////////////
         SleepUtils.sleep(150, TimeUnit.MILLISECONDS);
         qScheduleManager.stopAsync();

         System.out.println("Ran: " + BasicStep.counter + " times");
         assertTrue(BasicStep.counter > 1, "Scheduled process should have ran at least twice (but only ran [" + BasicStep.counter + "] time(s)).");

         //////////////////////////////////////////////////////
         // make sure poller ran, and didn't issue any warns //
         //////////////////////////////////////////////////////
         assertThat(quartzSqsPollerJobLog.getCollectedMessages())
            .anyMatch(m -> m.getLevel().equals(Level.DEBUG) && m.getMessage().contains("Running SQS Queue poller"))
            .noneMatch(m -> m.getLevel().equals(Level.WARN));

         //////////////////////////////////////////////////////
         // make sure poller ran, and didn't issue any warns //
         //////////////////////////////////////////////////////
         assertThat(quartzTableAutomationsJobLog.getCollectedMessages())
            .anyMatch(m -> m.getLevel().equals(Level.DEBUG) && m.getMessage().contains("Running Table Automations"))
            .noneMatch(m -> m.getLevel().equals(Level.WARN));
      }
      finally
      {
         QLogger.deactivateCollectingLoggerForClass(SchedulableSQSQueueRunner.class);
         QLogger.deactivateCollectingLoggerForClass(SchedulableTableAutomationsRunner.class);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRemovingNoLongerNeededJobsDuringSetupSchedules() throws SchedulerException
   {
      QInstance qInstance = QContext.getQInstance();
      QuartzTestUtils.setupInstanceForQuartzTests();

      ////////////////////////////
      // put two jobs in quartz //
      ////////////////////////////
      QProcessMetaData test1 = SchedulerTestUtils.buildTestProcess("test1", QuartzTestUtils.QUARTZ_SCHEDULER_NAME);
      QProcessMetaData test2 = SchedulerTestUtils.buildTestProcess("test2", QuartzTestUtils.QUARTZ_SCHEDULER_NAME);
      qInstance.addProcess(test1);
      qInstance.addProcess(test2);

      SchedulableType schedulableType = qInstance.getSchedulableType(ScheduledJobType.PROCESS.name());

      QuartzScheduler quartzScheduler = QuartzScheduler.initInstance(qInstance, QuartzTestUtils.QUARTZ_SCHEDULER_NAME, QuartzTestUtils.getQuartzProperties(), () -> QContext.getQSession());
      quartzScheduler.start();

      quartzScheduler.setupSchedulable(new BasicSchedulableIdentity("process:test1", null), schedulableType, Collections.emptyMap(), test1.getSchedule(), false);
      quartzScheduler.setupSchedulable(new BasicSchedulableIdentity("process:test2", null), schedulableType, Collections.emptyMap(), test1.getSchedule(), false);

      quartzScheduler.startOfSetupSchedules();
      quartzScheduler.setupSchedulable(new BasicSchedulableIdentity("process:test1", null), schedulableType, Collections.emptyMap(), test1.getSchedule(), false);
      quartzScheduler.endOfSetupSchedules();

      List<QuartzJobAndTriggerWrapper> quartzJobAndTriggerWrappers = quartzScheduler.queryQuartz();
      assertEquals(1, quartzJobAndTriggerWrappers.size());
      assertEquals("process:test1", quartzJobAndTriggerWrappers.get(0).jobDetail().getKey().getName());
   }


}