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


import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** A control action that is refused (the job is no longer in a state it applies
 ** to) must not have changed chunks first: reviving failed chunks of a job that
 ** moved on could replay work that already failed, including non-idempotent work.
 *******************************************************************************/
class DurableJobsControlAtomicityTest extends DurableJobsTestBase
{
   /*******************************************************************************
    ** Retrying the failed chunks of a cancelled job is refused and leaves them failed.
    *******************************************************************************/
   @Test
   void refusedRetryLeavesFailedChunksFailed() throws Exception
   {
      Integer jobId = launch();
      store.setChunkStatuses(jobId, List.of(ChunkStatus.PENDING), ChunkStatus.FAILED, Map.of("attempt", 3));
      assertThat(store.setJobStatus(jobId, EnumSet.of(JobStatus.QUEUED), JobStatus.CANCELLED, Map.of())).isTrue();

      assertThatThrownBy(() -> DurableJobs.control(jobId, JobAction.RETRY_FAILED)).isInstanceOf(QUserFacingException.class);

      assertThat(chunk(jobId).getValueString("status")).isEqualTo("FAILED");
      assertThat(chunk(jobId).getValueInteger("attempt")).isEqualTo(3);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("CANCELLED");
   }



   /*******************************************************************************
    ** A restart decided on a stale status (another controller resumed the job in
    ** between) is refused and must not revive the failed chunk.
    *******************************************************************************/
   @Test
   void staleRestartDoesNotReviveFailedChunk() throws Exception
   {
      Integer jobId = launch();
      store.setChunkStatuses(jobId, List.of(ChunkStatus.PENDING), ChunkStatus.FAILED, Map.of("attempt", 3));
      assertThat(store.setJobStatus(jobId, EnumSet.of(JobStatus.QUEUED), JobStatus.PAUSED, Map.of())).isTrue();
      QRecord stale = store.getJob(jobId);
      assertThat(store.setJobStatus(jobId, EnumSet.of(JobStatus.PAUSED), JobStatus.QUEUED, Map.of())).isTrue();

      assertThatThrownBy(() -> new JobControl(store, "actor", "test").apply(stale, JobAction.RESTART)).isInstanceOf(QUserFacingException.class);

      assertThat(chunk(jobId).getValueString("status")).isEqualTo("FAILED");
      assertThat(chunk(jobId).getValueInteger("attempt")).isEqualTo(3);
   }



   /*******************************************************************************
    ** The same stale decision cannot be applied twice: the first controller wins,
    ** the second is refused without touching the chunks the first one reset.
    *******************************************************************************/
   @Test
   void concurrentControllersCannotBothApplyARestart() throws Exception
   {
      Integer jobId = launch();
      store.setChunkStatuses(jobId, List.of(ChunkStatus.PENDING), ChunkStatus.FAILED, Map.of("attempt", 3));
      assertThat(store.setJobStatus(jobId, EnumSet.of(JobStatus.QUEUED), JobStatus.FAILED, Map.of())).isTrue();
      QRecord seenByBoth = store.getJob(jobId);

      new JobControl(store, "actor", "test").apply(seenByBoth, JobAction.RESTART);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("PENDING");
      store.setChunkStatuses(jobId, List.of(ChunkStatus.PENDING), ChunkStatus.FAILED, Map.of("attempt", 3));
      assertThat(store.setJobStatus(jobId, EnumSet.of(JobStatus.QUEUED), JobStatus.FAILED, Map.of())).isTrue();

      assertThatThrownBy(() -> new JobControl(store, "actor", "test").apply(seenByBoth, JobAction.RESTART)).isInstanceOf(QUserFacingException.class);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("FAILED");
   }



   /*******************************************************************************
    ** Negative control: an allowed restart and an allowed retry still do their work.
    *******************************************************************************/
   @Test
   void allowedRestartAndRetryStillWork() throws Exception
   {
      Integer restarted = launch();
      store.setChunkStatuses(restarted, List.of(ChunkStatus.PENDING), ChunkStatus.FAILED, Map.of("attempt", 3));
      assertThat(store.setJobStatus(restarted, EnumSet.of(JobStatus.QUEUED), JobStatus.FAILED, Map.of())).isTrue();
      DurableJobs.control(restarted, JobAction.RESTART);
      assertThat(chunk(restarted).getValueString("status")).isEqualTo("PENDING");
      assertThat(chunk(restarted).getValueInteger("attempt")).isZero();
      assertThat(store.getJob(restarted).getValueString("status")).isEqualTo("QUEUED");

      Integer retried = launch();
      store.setChunkStatuses(retried, List.of(ChunkStatus.PENDING), ChunkStatus.FAILED, Map.of("attempt", 3));
      assertThat(store.setJobStatus(retried, EnumSet.of(JobStatus.QUEUED), JobStatus.FAILED, Map.of())).isTrue();
      DurableJobs.control(retried, JobAction.RETRY_FAILED);
      assertThat(chunk(retried).getValueString("status")).isEqualTo("PENDING");
      assertThat(store.getJob(retried).getValueString("status")).isEqualTo("QUEUED");
   }
}
