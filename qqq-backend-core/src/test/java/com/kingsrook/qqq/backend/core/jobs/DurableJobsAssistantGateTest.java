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


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QTooManyRequestsException;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Assistant confirmation, channel preservation, and admission limits.
 *******************************************************************************/
class DurableJobsAssistantGateTest extends DurableJobsTestBase
{

   /*******************************************************************************
    ** Assistant launches remain unclaimed until a human confirms.
    *******************************************************************************/
   @Test
   void assistantLaunchWaitsForConfirmation() throws Exception
   {
      QContext.getQSession().setClientChannel("assistant/turn-1");
      Integer jobId = launch();
      DurableJobsRuntime.start(instance);
      QContext.getQSession().setClientChannel("next-ui");
      Integer humanJob = launch();
      await(humanJob, JobStatus.COMPLETED);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("AWAITING_CONFIRMATION");
      assertThat(chunk(jobId).getValueInteger("attempt")).isZero();
      assertThat(DurableJobsTestSupport.runs("step1")).isEqualTo(1);
   }



   /*******************************************************************************
    ** Human confirmation starts the job while preserving its launching channel.
    *******************************************************************************/
   @Test
   void assistantCannotStartButHumanCan() throws Exception
   {
      QContext.getQSession().setClientChannel("assistant/turn-1");
      Integer jobId = launch();
      assertThatThrownBy(() -> DurableJobs.control(jobId, JobAction.START)).isInstanceOf(QPermissionDeniedException.class);
      QContext.getQSession().setClientChannel("next-ui");
      DurableJobs.control(jobId, JobAction.START);
      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.COMPLETED);
      assertThat(DurableJobsTestSupport.SEEN.get("clientChannel")).isEqualTo("assistant/turn-1");
   }



   /*******************************************************************************
    ** The assistant can pause its own unconfirmed job.
    *******************************************************************************/
   @Test
   void assistantMayPause() throws Exception
   {
      QContext.getQSession().setClientChannel("assistant/turn-1");
      Integer jobId = launch();
      DurableJobs.control(jobId, JobAction.PAUSE);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("PAUSED");
   }



   /*******************************************************************************
    ** The assistant can cancel its own unconfirmed job.
    *******************************************************************************/
   @Test
   void assistantMayCancel() throws Exception
   {
      QContext.getQSession().setClientChannel("assistant/turn-1");
      Integer jobId = launch();
      DurableJobs.control(jobId, JobAction.CANCEL);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("CANCELLED");
   }



   /*******************************************************************************
    ** The assistant cannot request priority above five.
    *******************************************************************************/
   @Test
   void assistantPriorityIsCapped() throws Exception
   {
      QContext.getQSession().setClientChannel("assistant/turn-1");
      Integer jobId = DurableJobs.launch(new LaunchRequest(DurableJobsTestSupport.PROCESS_THREE_STEPS).withPriority(9));
      assertThat(store.getJob(jobId).getValueInteger("priority")).isEqualTo(5);
   }



   /*******************************************************************************
    ** Unconfirmed assistant jobs count against the assistant active cap.
    *******************************************************************************/
   @Test
   void assistantActiveCapRejectsSecondLaunch() throws Exception
   {
      settings.withAssistantMaxActiveJobs(1);
      QContext.getQSession().setClientChannel("assistant/turn-1");
      launch();
      assertThatThrownBy(this::launch).isInstanceOf(QTooManyRequestsException.class);
   }



   /*******************************************************************************
    ** An explicitly enabled small-job exception bypasses confirmation.
    *******************************************************************************/
   @Test
   void smallAssistantJobCanAutoStart() throws Exception
   {
      settings.withAssistantAutoStartMaxRows(10);
      QContext.getQSession().setClientChannel("assistant/turn-1");
      Integer jobId = DurableJobs.launch(new LaunchRequest(DurableJobsTestSupport.PROCESS_THREE_STEPS).withExpectedRows(5L).withValue("seed", "small"));
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("QUEUED");
      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.COMPLETED);
   }
}

