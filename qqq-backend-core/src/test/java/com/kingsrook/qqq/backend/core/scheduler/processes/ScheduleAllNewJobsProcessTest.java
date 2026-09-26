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

package com.kingsrook.qqq.backend.core.scheduler.processes;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobType;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerTestUtils;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzJobAndTriggerWrapper;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzScheduler;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzTestUtils;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableSQSQueueRunner;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ScheduleAllNewJobsProcess 
 *******************************************************************************/
class ScheduleAllNewJobsProcessTest extends BaseTest
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
   void test() throws QException, SchedulerException
   {
      try
      {
         QCollectingLogger quartzSchedulerLog = QLogger.activateCollectingLoggerForClass(QuartzScheduler.class);

         QInstance qInstance = QContext.getQInstance();
         new ScheduledJobsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
         MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, ScheduleAllNewJobsProcess.class.getPackageName());
         QuartzTestUtils.setupInstanceForQuartzTests();

         ///////////////////////////////////////////////////////////////////////////////////
         // clear out the customizers that would normally schedule jobs as we insert them //
         ///////////////////////////////////////////////////////////////////////////////////
         qInstance.getTable(ScheduledJob.TABLE_NAME).withCustomizers(Collections.emptyMap());

         QScheduleManager qScheduleManager = QScheduleManager.initInstance(qInstance, () -> QContext.getQSession());
         qScheduleManager.start();

         QuartzScheduler                  quartzScheduler = QuartzScheduler.getInstance();
         List<QuartzJobAndTriggerWrapper> wrappers        = quartzScheduler.queryQuartz();

         //////////////////////////////////////////////
         // make sure nothing is scheduled initially //
         //////////////////////////////////////////////
         assertTrue(wrappers.isEmpty());

         ////////////////////////////////////////////////////////////////////////////
         // insert a scheduled job - run schedule-new, make sure it gets scheduled //
         ////////////////////////////////////////////////////////////////////////////
         new InsertAction().execute(new InsertInput(ScheduledJob.TABLE_NAME).withRecordEntity(SchedulerTestUtils
            .newScheduledJob(ScheduledJobType.PROCESS, Map.of("processName", TestUtils.PROCESS_NAME_GREET_PEOPLE))
            .withLabel("Test job 1")
            .withId(null)
            .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)));

         RunProcessInput input = new RunProcessInput();
         input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
         input.setProcessName(ScheduleAllNewJobsProcess.class.getSimpleName());
         new RunProcessAction().execute(input);

         ///////////////////////////////////////////////////////////////
         // make sure our scheduledJob here got scheduled with quartz //
         ///////////////////////////////////////////////////////////////
         wrappers = quartzScheduler.queryQuartz();
         assertEquals(1, wrappers.size());
         assertTrue(wrappers.stream().anyMatch(w -> w.jobDetail().getKey().getName().equals("scheduledJob:1")));

         ///////////////
         // repeat it //
         ///////////////
         new InsertAction().execute(new InsertInput(ScheduledJob.TABLE_NAME).withRecordEntity(SchedulerTestUtils
            .newScheduledJob(ScheduledJobType.PROCESS, Map.of("processName", TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE))
            .withLabel("Test job 2")
            .withId(null)
            .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)));

         input = new RunProcessInput();
         input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
         input.setProcessName(ScheduleAllNewJobsProcess.class.getSimpleName());
         new RunProcessAction().execute(input);

         wrappers = quartzScheduler.queryQuartz();
         assertEquals(2, wrappers.size());
         assertTrue(wrappers.stream().anyMatch(w -> w.jobDetail().getKey().getName().equals("scheduledJob:2")));

         /////////////////////////////////////////////////////////////////////////////////////
         // make sure quartzScheduler never logged about deleting or re-scheduling anything //
         /////////////////////////////////////////////////////////////////////////////////////
         assertThat(quartzSchedulerLog.getCollectedMessages())
            .noneMatch(m -> m.getMessage().toLowerCase().contains("delete"))
            .noneMatch(m -> m.getMessage().toLowerCase().contains("re-schedule"));
      }
      finally
      {
         QLogger.deactivateCollectingLoggerForClass(SchedulableSQSQueueRunner.class);
      }
   }

}