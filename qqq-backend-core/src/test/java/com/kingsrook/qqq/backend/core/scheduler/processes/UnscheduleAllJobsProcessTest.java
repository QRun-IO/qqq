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


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobType;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerTestUtils;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzJobAndTriggerWrapper;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzScheduler;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzTestUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for UnscheduleAllJobsProcess 
 *******************************************************************************/
class UnscheduleAllJobsProcessTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      SchedulerTestUtils.afterEach();
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
   void test() throws QException, SchedulerException
   {
      QInstance qInstance = QContext.getQInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, UnscheduleAllJobsProcess.class.getPackageName());
      QuartzTestUtils.setupInstanceForQuartzTests();

      QScheduleManager qScheduleManager = QScheduleManager.initInstance(qInstance, () -> QContext.getQSession());
      qScheduleManager.start();

      qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.PROCESS,
         Map.of("processName", TestUtils.PROCESS_NAME_GREET_PEOPLE))
         .withId(2)
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME));

      QuartzScheduler quartzScheduler = QuartzScheduler.getInstance();
      List<QuartzJobAndTriggerWrapper> wrappers = quartzScheduler.queryQuartz();
      assertEquals(1, wrappers.size());

      RunProcessInput input = new RunProcessInput();
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      input.setProcessName(UnscheduleAllJobsProcess.class.getSimpleName());
      new RunProcessAction().execute(input);

      wrappers = quartzScheduler.queryQuartz();
      assertTrue(wrappers.isEmpty());
   }

}