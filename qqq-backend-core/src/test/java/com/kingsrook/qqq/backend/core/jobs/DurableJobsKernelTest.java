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
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.model.JobLease;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTestSupport.PROCESS_THREE_STEPS;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTestSupport.awaitStatus;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTestSupport.runs;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** End-to-end kernel tests on the memory backend: launch, run, reclaim, resume.
 *******************************************************************************/
class DurableJobsKernelTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      DurableJobsRuntime.stop(QContext.getQInstance(), Duration.ofSeconds(5));
   }



   /*******************************************************************************
    ** A STEPS job runs all three steps as the launching user and completes.
    *******************************************************************************/
   @Test
   void testStepsJobCompletes() throws QException
   {
      QInstance instance = DurableJobsTestSupport.defineInstance(DurableJobsTestSupport.fastSettings());
      QContext.init(instance, newSession());

      Integer jobId = DurableJobs.launch(new LaunchRequest(PROCESS_THREE_STEPS).withValue("seed", "s"));
      DurableJobsRuntime.start(instance);

      QRecord job = awaitStatus(jobId, Duration.ofSeconds(10), JobStatus.COMPLETED, JobStatus.FAILED);
      assertThat(job.getValueString("status")).isEqualTo(JobStatus.COMPLETED.name());
      assertThat(runs("step1")).isEqualTo(1);
      assertThat(runs("step3")).isEqualTo(1);
      assertThat(DurableJobsTestSupport.SEEN.get("userId")).isEqualTo(DEFAULT_USER_ID);
      assertThat(DurableJobsTestSupport.SEEN.get("step3SawFromStep2")).isEqualTo("two:one:s");

      DurableJobStore store = new DurableJobStore(DurableJobsMetaData.of(instance));
      assertThat(store.getChunks(jobId)).hasSize(1).allMatch(c -> ChunkStatus.DONE.name().equals(c.getValueString("status")));
      assertThat(eventTypes(store, jobId)).contains(JobEventType.LAUNCHED.name(), JobEventType.STARTED.name(), JobEventType.COMPLETED.name());
   }



   /*******************************************************************************
    ** A pod that held the chunk dies after step 2's checkpoint: the reaper
    ** reclaims it and the job resumes at step 3 with step 2's values, without
    ** re-running steps 1 and 2.
    *******************************************************************************/
   @Test
   void testReclaimResumesAfterLastCheckpoint() throws QException
   {
      QInstance instance = DurableJobsTestSupport.defineInstance(DurableJobsTestSupport.fastSettings());
      QContext.init(instance, newSession());
      DurableJobStore store = new DurableJobStore(DurableJobsMetaData.of(instance));

      Integer jobId = DurableJobs.launch(new LaunchRequest(PROCESS_THREE_STEPS).withValue("seed", "s"));

      /////////////////////////////////////////////////////////////////////
      // a "dead" pod claims the chunk, checkpoints after step 2, and dies //
      /////////////////////////////////////////////////////////////////////
      QRecord  chunk = store.getChunks(jobId).get(0);
      JobLease lease = store.claimChunk(chunk, "dead-pod", 1).orElseThrow();
      store.checkpoint(lease, JsonUtils.toJson(Map.of("lastStep", "step2", "values", Map.of("seed", "s", "fromStep1", "one:s", "fromStep2", "two:one:s"),
         "stepList", List.of("step1", "step2", "step3"))), null);
      store.setJobStatus(jobId, List.of(JobStatus.QUEUED), JobStatus.RUNNING, null);

      DurableJobsRuntime.start(instance);
      QRecord job = awaitStatus(jobId, Duration.ofSeconds(15), JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.NEEDS_REVIEW);

      assertThat(job.getValueString("status")).isEqualTo(JobStatus.COMPLETED.name());
      assertThat(runs("step1")).isZero();
      assertThat(runs("step2")).isZero();
      assertThat(runs("step3")).isEqualTo(1);
      assertThat(DurableJobsTestSupport.SEEN.get("step3SawFromStep2")).isEqualTo("two:one:s");
      assertThat(store.getChunks(jobId).get(0).getValueInteger("attempt")).isEqualTo(2);
      assertThat(eventTypes(store, jobId)).contains(JobEventType.RECLAIMED.name());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> eventTypes(DurableJobStore store, Integer jobId) throws QException
   {
      return (store.query(DurableJobsTables.TABLE_EVENT, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)))
         .stream().map(r -> r.getValueString("type")).toList());
   }

}
