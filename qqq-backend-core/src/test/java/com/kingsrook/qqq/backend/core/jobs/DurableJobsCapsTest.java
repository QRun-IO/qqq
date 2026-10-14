/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


package com.kingsrook.qqq.backend.core.jobs;


import java.util.concurrent.CountDownLatch;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QTooManyRequestsException;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Regression coverage for DurableJobsCaps.
 *******************************************************************************/
class DurableJobsCapsTest extends DurableJobsTestBase
{

   /*******************************************************************************
    ** A second job waits for its owner's active job and then completes.
    *******************************************************************************/
   @Test
   void perUserCapQueuesThenCompletes() throws Exception
   {
      settings.withWorkerThreads(2).withMaxActiveJobsPerUser(1).withDefaultMaxConcurrentJobsPerProcess(5);
      DurableJobsTestSupport.step1Gate = new CountDownLatch(1);
      DurableJobsTestSupport.stepSleepMillis = 400;
      Integer first = launch();
      DurableJobsRuntime.start(instance);
      awaitCondition("first job executing", () -> DurableJobsTestSupport.runs("step1") == 1);
      Integer second = launch();
      awaitCondition("per-user queue reason", () -> store.getJob(second).getValueString("statusReason") != null);
      assertThat(store.getJob(second).getValueString("status")).isEqualTo("QUEUED");
      assertThat(store.getJob(second).getValueString("statusReason")).startsWith("Waiting").contains("you already");
      assertThat(chunk(second).getValueInteger("attempt")).isZero();
      assertThat(store.getJob(first).getValueString("status")).isEqualTo("RUNNING");
      DurableJobsTestSupport.step1Gate.countDown();
      await(first, JobStatus.COMPLETED);
      await(second, JobStatus.COMPLETED);
   }



   /*******************************************************************************
    ** A process cap holds another user's job until the process slot opens.
    *******************************************************************************/
   @Test
   void perProcessCapQueuesThenCompletes() throws Exception
   {
      settings.withWorkerThreads(2).withDefaultMaxConcurrentJobsPerProcess(1);
      DurableJobsTestSupport.step1Gate = new CountDownLatch(1);
      DurableJobsTestSupport.stepSleepMillis = 400;
      Integer first = launch();
      DurableJobsRuntime.start(instance);
      awaitCondition("first process executing", () -> DurableJobsTestSupport.runs("step1") == 1);
      QContext.init(instance, newSession("B"));
      Integer second = launch();
      awaitCondition("per-process queue reason", () -> store.getJob(second).getValueString("statusReason") != null);
      assertThat(store.getJob(second).getValueString("status")).isEqualTo("QUEUED");
      assertThat(store.getJob(second).getValueString("statusReason")).startsWith("Waiting").contains("of this process");
      assertThat(chunk(second).getValueInteger("attempt")).isZero();
      assertThat(store.getJob(first).getValueString("status")).isEqualTo("RUNNING");
      DurableJobsTestSupport.step1Gate.countDown();
      await(first, JobStatus.COMPLETED);
      await(second, JobStatus.COMPLETED);
   }



   /*******************************************************************************
    ** A newer high-priority job is claimed before an older low-priority job.
    *******************************************************************************/
   @Test
   void higherPriorityStartsBeforeOlderJob() throws Exception
   {
      Integer low = DurableJobs.launch(new LaunchRequest(DurableJobsTestSupport.PROCESS_THREE_STEPS).withPriority(1).withValue("seed", "low"));
      Integer high = DurableJobs.launch(new LaunchRequest(DurableJobsTestSupport.PROCESS_THREE_STEPS).withPriority(9).withValue("seed", "high"));
      DurableJobsRuntime.start(instance);
      await(high, JobStatus.COMPLETED);
      await(low, JobStatus.COMPLETED);
      assertThat(DurableJobsTestSupport.START_ORDER).containsExactly("high", "low");
   }



   /*******************************************************************************
    ** Two waiting jobs exhaust a queue limit of two.
    *******************************************************************************/
   @Test
   void queuedCapRejectsThirdLaunch() throws Exception
   {
      settings.withMaxQueuedJobsPerUser(2);
      launch();
      launch();
      assertThatThrownBy(this::launch).isInstanceOf(QTooManyRequestsException.class);
   }



   /*******************************************************************************
    ** Another user's job runs while the capped user's second job waits.
    *******************************************************************************/
   @Test
   void anotherUserCanRunWhileFirstUserIsCapped() throws Exception
   {
      settings.withWorkerThreads(2).withMaxActiveJobsPerUser(1).withDefaultMaxConcurrentJobsPerProcess(5);
      DurableJobsTestSupport.step1Gate = new CountDownLatch(1);
      DurableJobsTestSupport.stepSleepMillis = 400;
      Integer first = launch();
      DurableJobsRuntime.start(instance);
      awaitCondition("first owner executing", () -> DurableJobsTestSupport.runs("step1") == 1);
      Integer waiting = launch();
      awaitCondition("second owner-A job held", () -> store.getJob(waiting).getValueString("statusReason") != null);
      QContext.init(instance, newSession("B"));
      Integer other = launch();
      await(other, JobStatus.RUNNING);
      assertThat(store.getJob(first).getValueString("status")).isEqualTo("RUNNING");
      assertThat(store.getJob(waiting).getValueString("status")).isEqualTo("QUEUED");
      DurableJobsTestSupport.step1Gate.countDown();
      await(first, JobStatus.COMPLETED);
      await(other, JobStatus.COMPLETED);
      await(waiting, JobStatus.COMPLETED);
   }
}

