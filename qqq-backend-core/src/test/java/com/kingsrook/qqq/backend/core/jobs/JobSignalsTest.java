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
import com.kingsrook.qqq.backend.core.alerts.NotificationSeverity;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.progress.TrackedJobRequest;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.jobs.runtime.InlineJobTracker;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobSignals;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
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
         assertThat(message.getData().keySet()).isSubsetOf("jobId", "status", "chunksDone", "chunksTotal", "rowsDone", "rowsTotal", "rowsFailed", "rowsPerSecond", "etaSeconds",
            "currentPhase", "percent", "message", "bytesDone", "bytesTotal", "logSeq");
         message.getData().forEach((key, value) ->
         {
            if(!List.of("status", "currentPhase", "message").contains(key) && value != null)
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
      assertThat(store.getJob(jobId).getValueBoolean("notifyOnSuccess")).isTrue();
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
    ** A background job launched with notifyOnSuccess=false completes quietly,
    ** unless it also asked for an email.
    *******************************************************************************/
   @Test
   void backgroundJobCanOptOutOfSuccessNotification() throws Exception
   {
      assertThat(new LaunchRequest(DurableJobsTestSupport.PROCESS_THREE_STEPS).getNotifyOnSuccess()).isTrue();
      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(DurableJobsTestSupport.CapturingSink.class));
      Integer quiet = DurableJobs.launch(new LaunchRequest(DurableJobsTestSupport.PROCESS_THREE_STEPS).withValue("seed", "x").withNotifyOnSuccess(false));
      Integer email = DurableJobs.launch(new LaunchRequest(DurableJobsTestSupport.PROCESS_THREE_STEPS).withValue("seed", "y").withNotifyOnSuccess(false).withNotifyEmail(true));
      DurableJobsRuntime.start(instance);
      await(quiet, JobStatus.COMPLETED);
      await(email, JobStatus.COMPLETED);
      DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));
      assertThat(store.getJob(quiet).getValueBoolean("notifyOnSuccess")).isFalse();
      assertThat(DurableJobsTestSupport.NOTIFICATIONS).singleElement()
         .satisfies(signal -> assertThat(signal.getSourceKey()).isEqualTo("job:" + email + ":COMPLETED"));
   }



   /*******************************************************************************
    ** Tracked jobs complete quietly by default, can opt in, and always notify
    ** when they fail.
    *******************************************************************************/
   @Test
   void trackedJobsNotifyOnlyOnFailureByDefault() throws Exception
   {
      assertThat(new TrackedJobRequest().getNotifyOnSuccess()).isFalse();
      assertThat(new TrackedJobRequest().withNotifyOnSuccess(true).getNotifyOnSuccess()).isTrue();
      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(DurableJobsTestSupport.CapturingSink.class));
      InlineJobTracker tracker = new InlineJobTracker(store, "pod-a");
      String           owner   = DurableJobsSecurity.userId(QContext.getQSession());

      Integer quiet = tracker.startTracked("assistant.turn", "Assistant answer", owner, null, null, null);
      assertThat(store.getJob(quiet).getValueBoolean("notifyOnSuccess")).isFalse();
      tracker.finishTracked(quiet, JobStatus.COMPLETED, "Answered.");
      assertThat(DurableJobsTestSupport.NOTIFICATIONS).isEmpty();

      Integer failed = tracker.startTracked("assistant.turn", "Assistant answer", owner, null, null, null);
      tracker.finishTracked(failed, JobStatus.FAILED, "Broke.");
      assertThat(DurableJobsTestSupport.NOTIFICATIONS).singleElement()
         .satisfies(signal -> assertThat(signal.getSeverity()).isEqualTo(NotificationSeverity.ERROR));

      Integer optedIn = tracker.startTracked("assistant.turn", "Assistant answer", owner, null, null, null, true);
      tracker.finishTracked(optedIn, JobStatus.COMPLETED, "Answered.");
      assertThat(DurableJobsTestSupport.NOTIFICATIONS).hasSize(2).last()
         .satisfies(signal -> assertThat(signal.getSourceKey()).isEqualTo("job:" + optedIn + ":COMPLETED"));
   }



   /*******************************************************************************
    ** Rows with no notifyOnSuccess value use the defaults by source type; a
    ** completion with errors always notifies.
    *******************************************************************************/
   @Test
   void rowsWithoutTheFlagFallBackToDefaults()
   {
      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(DurableJobsTestSupport.CapturingSink.class));
      JobSignals.finished(new QRecord().withValue("id", 9001).withValue("userId", DEFAULT_USER_ID).withValue("sourceType", "INLINE"), JobStatus.COMPLETED);
      assertThat(DurableJobsTestSupport.NOTIFICATIONS).isEmpty();
      JobSignals.finished(new QRecord().withValue("id", 9002).withValue("userId", DEFAULT_USER_ID).withValue("sourceType", "INLINE"), JobStatus.COMPLETED_WITH_ERRORS);
      JobSignals.finished(new QRecord().withValue("id", 9003).withValue("userId", DEFAULT_USER_ID).withValue("sourceType", "STEPS"), JobStatus.COMPLETED);
      assertThat(DurableJobsTestSupport.NOTIFICATIONS).extracting(signal -> signal.getSourceKey())
         .containsExactly("job:9002:COMPLETED_WITH_ERRORS", "job:9003:COMPLETED");
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

