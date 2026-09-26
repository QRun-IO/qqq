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

package com.kingsrook.qqq.backend.core.scheduler.simple;


import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerTestUtils;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerTestUtils.BasicStep;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ScheduleManager
 *******************************************************************************/
class SimpleSchedulerTest extends BaseTest
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
   void testStartAndStop() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      QScheduleManager qScheduleManager = QScheduleManager.initInstance(qInstance, () -> QContext.getQSession());
      qScheduleManager.start();

      SimpleScheduler simpleScheduler = SimpleScheduler.getInstance(qInstance);
      assertThat(simpleScheduler.getExecutors()).isNotEmpty();

      qScheduleManager.stop();
      simpleScheduler.getExecutors().forEach(e -> assertEquals(StandardScheduledExecutor.RunningState.STOPPED, e.getRunningState()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testScheduledProcess() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new QInstanceValidator().validate(qInstance);
      qInstance.getAutomationProviders().clear();
      qInstance.getQueueProviders().clear();

      qInstance.addProcess(SchedulerTestUtils.buildTestProcess("testScheduledProcess", TestUtils.SIMPLE_SCHEDULER_NAME));

      BasicStep.counter = 0;

      QSession         qSession         = QContext.getQSession();
      QScheduleManager qScheduleManager = QScheduleManager.initInstance(qInstance, () -> qSession);
      qScheduleManager.start();

      //////////////////////////////////////////////////
      // give a moment for the job to run a few times //
      //////////////////////////////////////////////////
      SleepUtils.sleep(50, TimeUnit.MILLISECONDS);
      qScheduleManager.stopAsync();

      System.out.println("Ran: " + BasicStep.counter + " times");
      assertTrue(BasicStep.counter > 1, "Scheduled process should have ran at least twice (but only ran [" + BasicStep.counter + "] time(s).");
   }


}