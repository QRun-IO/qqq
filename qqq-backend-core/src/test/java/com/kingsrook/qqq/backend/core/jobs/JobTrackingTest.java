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


import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.progress.JobPhase;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgress;
import com.kingsrook.qqq.backend.core.jobs.progress.JobTracking;
import com.kingsrook.qqq.backend.core.jobs.progress.TrackedJobRequest;
import com.kingsrook.qqq.backend.core.jobs.runtime.InlineJobTracker;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobClaimer;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Tracking jobs with no process behind them (JobTracking).
 *******************************************************************************/
class JobTrackingTest extends DurableJobsTestBase
{

   /*******************************************************************************
    ** Start, report progress and a log line, cancel through the listener, and
    ** finish: the first final status wins and workers never claim the row.
    *******************************************************************************/
   @Test
   void startProgressCancelFinish() throws Exception
   {
      InlineJobTracker tracker = new InlineJobTracker(store, "pod-a");
      String           owner   = DurableJobsSecurity.userId(QContext.getQSession());
      Integer          jobId   = tracker.startTracked("assistant.turn", "Assistant answer", owner, "web", null, null);

      QRecord job = store.getJob(jobId);
      assertThat(job.getValueString("status")).isEqualTo("RUNNING");
      assertThat(job.getValueString("sourceType")).isEqualTo("INLINE");
      assertThat(job.getValueString("leaseOwner")).isEqualTo("pod-a");
      assertThat(new JobClaimer(store, "pod-a").claimNext()).isEmpty();

      JobProgress progress = new JobProgress(store, jobId);
      progress.phases(JobPhase.of("thinking", "Thinking", 1), JobPhase.of("writing", "Writing", 1));
      progress.phase("thinking").message("Looking things up");
      progress.warn("token=abc123 should not survive");
      progress.flush();
      job = store.getJob(jobId);
      assertThat(job.getValueString("currentPhase")).isEqualTo("thinking");
      assertThat(job.getValueString("progressMessage")).isEqualTo("Looking things up");
      var lines = store.query(DurableJobsTables.TABLE_LOG, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)));
      assertThat(lines).hasSize(1);
      assertThat(lines.get(0).getValueString("message")).doesNotContain("abc123");

      AtomicInteger cancels = new AtomicInteger();
      tracker.onCancel(jobId, cancels::incrementAndGet);
      tracker.tick();
      assertThat(cancels.get()).isZero();
      store.updateJob(jobId, Map.of("status", "CANCELLING"));
      tracker.tick();
      tracker.tick();
      assertThat(cancels.get()).isEqualTo(1);

      tracker.finishTracked(jobId, JobStatus.CANCELLED, "Stopped by the user.");
      tracker.finishTracked(jobId, JobStatus.COMPLETED, "Too late.");
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("CANCELLED");
      assertThatThrownBy(() -> tracker.finishTracked(jobId, JobStatus.RUNNING, "no")).isInstanceOf(IllegalArgumentException.class);
   }



   /*******************************************************************************
    ** A tracking job needs an owner, and the facade needs a running runtime.
    *******************************************************************************/
   @Test
   void guards()
   {
      InlineJobTracker tracker = new InlineJobTracker(store, "pod-a");
      assertThatThrownBy(() -> tracker.startTracked("assistant.turn", "x", null, null, null, null)).hasMessageContaining("owner");
      assertThatThrownBy(() -> JobTracking.start(new TrackedJobRequest().withUserId("u").withProcessName("assistant.turn"))).isInstanceOf(QException.class);
   }



   /*******************************************************************************
    ** A tracking job whose pod went away is failed by the reaper.
    *******************************************************************************/
   @Test
   void reapedWhenPodGone() throws Exception
   {
      InlineJobTracker tracker = new InlineJobTracker(store, "pod-gone");
      Integer          jobId   = tracker.startTracked("assistant.turn", "Assistant answer", DurableJobsSecurity.userId(QContext.getQSession()), null, null, 7);
      new InlineJobTracker(store, "pod-live").reap();
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("FAILED");
      assertThat(store.getJob(jobId).getValueString("sourceJson")).contains("\"recordId\":7");
   }

}
