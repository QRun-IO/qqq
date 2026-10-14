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


import java.time.Duration;
import java.util.List;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.alerts.NotificationAudience;
import com.kingsrook.qqq.backend.core.alerts.NotificationCategory;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.realtime.RealtimeBus;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Regression coverage for JobSignals.
 *******************************************************************************/
class JobSignalsTest extends DurableJobsTestBase
{

   /*******************************************************************************
    ** Completion publishes status and count-only progress on both authorized channels.
    *******************************************************************************/
   @Test
   void completedJobPublishesCountsOnRecordAndOwnerChannels() throws Exception
   {
      instance.withSupplementalMetaData(new RealtimeMetaData().withBus(new QCodeReference(DurableJobsTestSupport.CapturingBus.class)));
      Integer jobId = launch();
      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.COMPLETED);
      DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));

      for(String channel : List.of("record:jobs_job:" + jobId, "user:" + DEFAULT_USER_ID))
      {
         assertThat(DurableJobsTestSupport.MESSAGES).anySatisfy(message ->
         {
            assertThat(message.getChannel()).isEqualTo(channel);
            assertThat(message.getEvent()).isEqualTo("job.status");
            assertThat(message.getData()).containsEntry("jobId", jobId).containsEntry("status", "COMPLETED");
         });
         assertThat(DurableJobsTestSupport.MESSAGES).anySatisfy(message ->
         {
            assertThat(message.getChannel()).isEqualTo(channel);
            assertThat(message.getEvent()).isEqualTo("job.progress");
            assertThat(message.getData()).containsEntry("chunksDone", 1).containsEntry("chunksTotal", 1).containsEntry("status", "COMPLETED");
         });
      }
      assertThat(DurableJobsTestSupport.MESSAGES).allSatisfy(message ->
      {
         assertThat(message.getChannel()).isIn("record:jobs_job:" + jobId, "user:" + DEFAULT_USER_ID);
         assertThat(message.getData().keySet()).isSubsetOf("jobId", "status", "chunksDone", "chunksTotal", "rowsDone", "rowsTotal", "rowsFailed", "rowsPerSecond", "etaSeconds");
         message.getData().forEach((key, value) ->
         {
            if(!"status".equals(key) && value != null)
            {
               assertThat(value).isInstanceOf(Number.class);
            }
         });
      });
   }



   /*******************************************************************************
    ** A completed job notifies its owner once without an implicit email request.
    *******************************************************************************/
   @Test
   void completionNotifiesOwnerExactlyOnceWithoutEmailByDefault() throws Exception
   {
      assertNotification(false);
   }



   /*******************************************************************************
    ** An opted-in completed job requests email on its single owner notification.
    *******************************************************************************/
   @Test
   void completionRequestsEmailOnlyWhenOptedIn() throws Exception
   {
      assertNotification(true);
   }



   /*******************************************************************************
    ** Repeated finalizer passes cannot duplicate a completed job's notification.
    *******************************************************************************/
   private void assertNotification(boolean email) throws Exception
   {
      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(DurableJobsTestSupport.CapturingSink.class));
      Integer jobId = DurableJobs.launch(new LaunchRequest(DurableJobsTestSupport.PROCESS_THREE_STEPS).withValue("seed", "secret-input").withNotifyEmail(email));
      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.COMPLETED);
      DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));
      DurableJobs.evaluate(jobId);
      DurableJobs.evaluate(jobId);
      assertThat(DurableJobsTestSupport.NOTIFICATIONS).singleElement().satisfies(signal ->
      {
         assertThat(signal.getCategory()).isEqualTo(NotificationCategory.JOB);
         assertThat(signal.getAudience()).isEqualTo(NotificationAudience.user(DEFAULT_USER_ID));
         assertThat(signal.getSourceKey()).isEqualTo("job:" + jobId + ":COMPLETED");
         if(email)
         {
            assertThat(signal.getAttributes()).containsEntry("emailRequested", "true");
         }
         else
         {
            assertThat(signal.getAttributes()).doesNotContainKey("emailRequested");
         }
      });
   }



   /*******************************************************************************
    ** Close the test's bus after its last worker has stopped publishing.
    *******************************************************************************/
   @AfterEach
   void closeBus()
   {
      DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));
      RealtimeBus.close(instance);
   }
}

