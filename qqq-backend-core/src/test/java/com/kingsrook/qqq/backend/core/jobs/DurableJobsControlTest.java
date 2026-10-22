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
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.processes.JobControlProcesses;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Regression coverage for DurableJobsControl.
 *******************************************************************************/
class DurableJobsControlTest extends DurableJobsTestBase
{

   /*******************************************************************************
    ** A paused queued job stays unclaimed until resumed.
    *******************************************************************************/
   @Test
   void pauseQueuedJobThenResume() throws Exception
   {
      Integer jobId = launch();
      DurableJobs.control(jobId, JobAction.PAUSE);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("PAUSED");
      DurableJobsRuntime.start(instance);
      Integer witness = launch();
      await(witness, JobStatus.COMPLETED);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("PENDING");
      assertThat(chunk(jobId).getValueInteger("attempt")).isZero();
      assertThat(DurableJobsTestSupport.runs("step1")).isEqualTo(1);
      DurableJobs.control(jobId, JobAction.RESUME);
      await(jobId, JobStatus.COMPLETED);
      assertThat(DurableJobsTestSupport.runs("step1")).isEqualTo(2);
   }



   /*******************************************************************************
    ** Pausing checkpoints a finished step and resumes without replay or attempt charge.
    *******************************************************************************/
   @Test
   void pauseRunningJobAtStepBoundary() throws Exception
   {
      DurableJobsTestSupport.stepSleepMillis = 2100;
      Integer jobId = launch();
      DurableJobsRuntime.start(instance);
      awaitCondition("step two executing", () -> DurableJobsTestSupport.runs("step2") == 1);
      DurableJobs.control(jobId, JobAction.PAUSE);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("PAUSING");
      await(jobId, JobStatus.PAUSED);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("PENDING");
      assertThat(chunk(jobId).getValueInteger("attempt")).isZero();
      assertThat(DurableJobsTestSupport.runs("step3")).isZero();
      DurableJobs.control(jobId, JobAction.RESUME);
      await(jobId, JobStatus.COMPLETED);
      assertThat(DurableJobsTestSupport.runs("step1")).isEqualTo(1);
      assertThat(DurableJobsTestSupport.runs("step2")).isEqualTo(1);
      assertThat(DurableJobsTestSupport.runs("step3")).isEqualTo(1);
   }



   /*******************************************************************************
    ** Cancelling queued work cancels its chunk and explains committed work is not undone.
    *******************************************************************************/
   @Test
   void cancelQueuedJob() throws Exception
   {
      Integer jobId = launch();
      DurableJobs.control(jobId, JobAction.CANCEL);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("CANCELLED");
      assertThat(store.getJob(jobId).getValueString("statusReason")).contains("not undone");
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("CANCELLED");
      assertThat(DurableJobsTestSupport.runs("step1")).isZero();
   }



   /*******************************************************************************
    ** Cancelling a running job stops it at the next boundary.
    *******************************************************************************/
   @Test
   void cancelRunningJob() throws Exception
   {
      DurableJobsTestSupport.stepSleepMillis = 2100;
      Integer jobId = launch();
      DurableJobsRuntime.start(instance);
      awaitCondition("first step executing", () -> DurableJobsTestSupport.runs("step1") == 1);
      DurableJobs.control(jobId, JobAction.CANCEL);
      await(jobId, JobStatus.CANCELLED);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("CANCELLED");
      assertThat(DurableJobsTestSupport.runs("step2")).isZero();
      assertThat(store.getJob(jobId).getValueString("statusReason")).contains("not undone");
   }



   /*******************************************************************************
    ** Three failed attempts raise an alert that a successful explicit retry resolves.
    *******************************************************************************/
   @Test
   void failedStepExhaustsRetriesThenExplicitRetryResolvesAlert() throws Exception
   {
      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(DurableJobsTestSupport.CapturingSink.class));
      DurableJobsTestSupport.failStep3 = true;
      Integer jobId = launch();
      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.FAILED);
      DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));
      assertThat(chunk(jobId).getValueInteger("attempt")).isEqualTo(3);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("FAILED");
      assertThat(DurableJobsTestSupport.runs("step3")).isEqualTo(3);
      assertThat(DurableJobsTestSupport.runs("step1")).isEqualTo(1);
      assertThat(events(jobId).stream().filter("RETRIED"::equals)).hasSize(2);
      assertThat(DurableJobsTestSupport.ALERTS).anySatisfy(signal ->
      {
         assertThat(signal.getAlertTypeName()).isEqualTo("jobs.job-failed");
         assertThat(signal.getEffectiveDedupKey()).isEqualTo(jobId.toString());
      });

      DurableJobsTestSupport.failStep3 = false;
      DurableJobs.control(jobId, JobAction.RETRY_FAILED);
      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.COMPLETED);
      DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));
      assertThat(chunk(jobId).getValueInteger("attempt")).isEqualTo(1);
      assertThat(DurableJobsTestSupport.runs("step1")).isEqualTo(1);
      assertThat(DurableJobsTestSupport.runs("step3")).isEqualTo(4);
      assertThat(DurableJobsTestSupport.RESOLUTIONS).anySatisfy(resolution ->
      {
         assertThat(resolution.getAlertTypeName()).isEqualTo("jobs.job-failed");
         assertThat(resolution.getEffectiveDedupKey()).isEqualTo(jobId.toString());
      });
   }



   /*******************************************************************************
    ** Restart reruns only unfinished chunks (spec 2.7): a COMPLETED job has
    ** none, so Restart refuses and points to Run Again.
    *******************************************************************************/
   @Test
   void restartCompletedJobIsRefused() throws Exception
   {
      Integer jobId = launch();
      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.COMPLETED);
      assertThatThrownBy(() -> DurableJobs.control(jobId, JobAction.RESTART)).isInstanceOf(QUserFacingException.class).hasMessageContaining("Run Again");
      assertThat(DurableJobsTestSupport.runs("step1")).isEqualTo(1);
   }



   /*******************************************************************************
    ** Run again creates a new completed job linked to its original parent.
    *******************************************************************************/
   @Test
   void runAgainCreatesCompletedChild() throws Exception
   {
      Integer jobId = launch();
      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.COMPLETED);
      DurableJobs.control(jobId, JobAction.RUN_AGAIN);
      var children = store.query(DurableJobsTables.TABLE_JOB, new QQueryFilter(new QFilterCriteria("parentJobId", QCriteriaOperator.EQUALS, jobId)));
      assertThat(children).hasSize(1);
      Integer child = children.get(0).getValueInteger("id");
      assertThat(child).isNotEqualTo(jobId);
      await(child, JobStatus.COMPLETED);
      assertThat(store.getJob(child).getValueInteger("parentJobId")).isEqualTo(jobId);
      assertThat(DurableJobsTestSupport.runs("step1")).isEqualTo(2);
      assertThat(DurableJobsTestSupport.runs("step3")).isEqualTo(2);
   }



   /*******************************************************************************
    ** Pause record actions report one paused job and a skipped inapplicable status.
    *******************************************************************************/
   @Test
   void pauseRecordActionReportsChangedAndSkippedJobs() throws Exception
   {
      Integer queued = launch();
      Integer cancelled = launch();
      DurableJobs.control(cancelled, JobAction.CANCEL);
      List<ProcessSummaryLine> lines = runControl(JobAction.PAUSE, queued + "," + cancelled, null);
      assertSummary(lines, "job was paused");
      assertThat(lines).anySatisfy(line ->
      {
         assertThat(line.getCount()).isEqualTo(1);
         assertThat(line.getSingularPastMessage()).isEqualTo("job was skipped");
         assertThat(line.getMessage()).contains("cancelled");
      });
      assertThat(store.getJob(queued).getValueString("status")).isEqualTo("PAUSED");
      assertThat(store.getJob(cancelled).getValueString("status")).isEqualTo("CANCELLED");
   }



   /*******************************************************************************
    ** Pause reports one changed job through the standard record-action process.
    *******************************************************************************/
   @Test
   void pauseRecordAction() throws Exception
   {
      Integer jobId = launch();
      assertSummary(runControl(JobAction.PAUSE, jobId.toString(), null), "job was paused");
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("PAUSED");
   }



   /*******************************************************************************
    ** Resume runs through the standard record-action process using a jobs filter.
    *******************************************************************************/
   @Test
   void resumeRecordAction() throws Exception
   {
      Integer jobId = launch();
      DurableJobs.control(jobId, JobAction.PAUSE);
      assertSummary(runControl(JobAction.RESUME, null, new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, jobId))), "job was resumed");
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("QUEUED");
   }



   /*******************************************************************************
    ** Cancel runs through the standard record-action process.
    *******************************************************************************/
   @Test
   void cancelRecordAction() throws Exception
   {
      Integer jobId = launch();
      assertSummary(runControl(JobAction.CANCEL, jobId.toString(), null), "job was cancelled");
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("CANCELLED");
   }



   /*******************************************************************************
    ** Start confirms an awaiting job through the standard record-action process.
    *******************************************************************************/
   @Test
   void startRecordAction() throws Exception
   {
      Integer jobId = launch();
      store.setJobStatus(jobId, List.of(JobStatus.QUEUED), JobStatus.AWAITING_CONFIRMATION, null);
      assertSummary(runControl(JobAction.START, jobId.toString(), null), "job was started");
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("QUEUED");
   }



   /*******************************************************************************
    ** Retry failed resets a failed chunk through the record-action process.
    *******************************************************************************/
   @Test
   void retryFailedRecordAction() throws Exception
   {
      Integer jobId = launch();
      store.setChunkStatuses(jobId, List.of(ChunkStatus.PENDING), ChunkStatus.FAILED, Map.of("attempt", 3));
      DurableJobs.evaluate(jobId);
      assertSummary(runControl(JobAction.RETRY_FAILED, jobId.toString(), null), "job was set to retry");
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("PENDING");
      assertThat(chunk(jobId).getValueInteger("attempt")).isZero();
   }



   /*******************************************************************************
    ** Restart requeues a cancelled chunk through the record-action process.
    *******************************************************************************/
   @Test
   void restartRecordAction() throws Exception
   {
      Integer jobId = launch();
      DurableJobs.control(jobId, JobAction.CANCEL);
      assertSummary(runControl(JobAction.RESTART, jobId.toString(), null), "job was restarted");
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("PENDING");
   }



   /*******************************************************************************
    ** Run again creates a child through the record-action process.
    *******************************************************************************/
   @Test
   void runAgainRecordAction() throws Exception
   {
      Integer jobId = launch();
      DurableJobs.control(jobId, JobAction.CANCEL);
      assertSummary(runControl(JobAction.RUN_AGAIN, jobId.toString(), null), "job was run again");
      assertThat(store.query(DurableJobsTables.TABLE_JOB, new QQueryFilter(new QFilterCriteria("parentJobId", QCriteriaOperator.EQUALS, jobId)))).hasSize(1);
   }



   /*******************************************************************************
    ** Run the actual frontend-skipping process and read its Result summary.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private List<ProcessSummaryLine> runControl(JobAction action, String ids, QQueryFilter filter) throws Exception
   {
      DurableJobsSecurity.applyToSession(com.kingsrook.qqq.backend.core.context.QContext.getQSession());
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(JobControlProcesses.processName(action));
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      if(ids != null)
      {
         input.addValue("recordIds", ids);
      }
      if(filter != null)
      {
         input.addValue("filterJSON", JsonUtils.toJson(filter));
      }
      RunProcessOutput output = new RunProcessAction().execute(input);
      assertThat(output.getException()).isEmpty();
      assertThat(output.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY)).isInstanceOf(List.class);
      return ((List<ProcessSummaryLine>) output.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY));
   }



   /*******************************************************************************
    ** Assert the count and past-tense result emitted for one changed job.
    *******************************************************************************/
   private void assertSummary(List<ProcessSummaryLine> lines, String message)
   {
      assertThat(lines).anySatisfy(line ->
      {
         assertThat(line.getCount()).isEqualTo(1);
         assertThat(line.getMessage()).isEqualTo(message);
      });
   }
}

