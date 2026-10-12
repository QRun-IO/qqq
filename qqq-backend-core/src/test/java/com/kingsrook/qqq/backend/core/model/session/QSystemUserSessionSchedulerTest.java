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

package com.kingsrook.qqq.backend.core.model.session;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobType;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerTestUtils;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerUtils;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzJobRunner;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzScheduler;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzTestUtils;
import com.kingsrook.qqq.backend.core.scheduler.simple.SimpleJobRunner;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;


/*******************************************************************************
 ** Schedulers must install their instance before invoking a system-session supplier.
 *******************************************************************************/
class QSystemUserSessionSchedulerTest extends BaseTest
{
   private static int executions;



   /*******************************************************************************
    ** Release scheduler singletons even when an assertion fails.
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      SchedulerTestUtils.afterEach();
   }



   /*******************************************************************************
    ** Startup can happen with a different instance already in the caller's context.
    *******************************************************************************/
   @Test
   void testScheduleManagerStartup() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      QInstance callerInstance = TestUtils.defineInstance();
      QSession callerSession = QContext.getQSession();
      QScheduleManager manager = QScheduleManager.initInstance(qInstance, () ->
      {
         assertSame(qInstance, QContext.getQInstance());
         QSystemUserSession session = new QSystemUserSession();
         assertEquals(List.of(true), session.getSecurityKeyValues(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS));
         return (session);
      });
      QContext.init(callerInstance, callerSession);
      manager.start();
      manager.stop();
      assertSame(callerInstance, QContext.getQInstance());
      assertSame(callerSession, QContext.getQSession());
   }



   /*******************************************************************************
    ** A fresh worker has no context and cannot rely on a previous session's keys.
    *******************************************************************************/
   @Test
   void testSimpleJobRunner() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      QProcessMetaData process = addProcess(qInstance);
      QScheduleManager.initInstance(qInstance, QSystemUserSession::new);
      QContext.clear();
      new SimpleJobRunner(qInstance, qInstance.getSchedulableType(ScheduledJobType.PROCESS.name()), Map.of("processName", process.getName())).run();
      assertEquals(1, executions);
      assertNull(QContext.getQInstance());
   }



   /*******************************************************************************
    ** Exercise native Quartz dispatch without waiting for a timer.
    *******************************************************************************/
   @Test
   void testQuartzJobRunner() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      QuartzTestUtils.setupInstanceForQuartzTests();
      QProcessMetaData process = addProcess(qInstance);
      QuartzScheduler.initInstance(qInstance, QuartzTestUtils.QUARTZ_SCHEDULER_NAME, QuartzTestUtils.getQuartzProperties(), QSystemUserSession::new);
      JobExecutionContext jobContext = Mockito.mock(JobExecutionContext.class);
      Mockito.when(jobContext.getJobDetail()).thenReturn(JobBuilder.newJob(QuartzJobRunner.class)
         .withIdentity("systemSession")
         .usingJobData(new JobDataMap(Map.of("type", ScheduledJobType.PROCESS.name(), "params", Map.of("processName", process.getName()))))
         .build());
      QContext.clear();
      new QuartzJobRunner().execute(jobContext);
      assertEquals(1, executions);
      assertNull(QContext.getQInstance());
   }



   /*******************************************************************************
    ** The process entry point also accepts a system-session supplier directly.
    *******************************************************************************/
   @Test
   void testScheduledProcess()
   {
      QInstance qInstance = QContext.getQInstance();
      QProcessMetaData process = addProcess(qInstance);
      QContext.clear();
      SchedulerUtils.runProcess(qInstance, QSystemUserSession::new, process, null, null);
      assertEquals(1, executions);
      assertNull(QContext.getQInstance());
   }



   /*******************************************************************************
    ** Create a real process whose step verifies the session seen by scheduled work.
    *******************************************************************************/
   private QProcessMetaData addProcess(QInstance qInstance)
   {
      executions = 0;
      QProcessMetaData process = new QProcessMetaData().withName("verifySystemSession")
         .withStep(new QBackendStepMetaData().withName("verify").withCode(new QCodeReference(VerifySystemSessionStep.class)));
      qInstance.addProcess(process);
      return (process);
   }



   /*******************************************************************************
    ** Assertions happen in the process, after all session/context setup is complete.
    *******************************************************************************/
   public static class VerifySystemSessionStep implements BackendStep
   {
      /*******************************************************************************
       ** Verify the all-access key used by record security locks.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output)
      {
         assertEquals(List.of(true), QContext.getQSession().getSecurityKeyValues(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS));
         executions++;
      }
   }
}
