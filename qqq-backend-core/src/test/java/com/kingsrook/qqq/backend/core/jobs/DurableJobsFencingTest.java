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


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkOutcome;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import com.kingsrook.qqq.backend.core.jobs.model.JobLease;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.jobs.store.LeaseLostException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Regression coverage for DurableJobsFencing.
 *******************************************************************************/
class DurableJobsFencingTest extends DurableJobsTestBase
{

   /*******************************************************************************
    ** A reclaimed token cannot commit a zombie worker's completion.
    *******************************************************************************/
   @Test
   void staleCompletionCannotFinishReclaimedChunk() throws Exception
   {
      Integer jobId = launch();
      JobLease lease = expireAndReap(jobId);
      assertThatThrownBy(() -> store.completeChunk(lease, new ChunkOutcome(1, 1, 0, null, null), null)).isInstanceOf(LeaseLostException.class);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("RETRY");
      assertThat(events(jobId)).contains("RECLAIMED");
   }



   /*******************************************************************************
    ** A reclaimed token cannot renew its lease.
    *******************************************************************************/
   @Test
   void staleHeartbeatCannotRenewLease() throws Exception
   {
      Integer jobId = launch();
      JobLease lease = expireAndReap(jobId);
      assertThat(store.heartbeat(lease, 3)).isFalse();
      assertThat(chunk(jobId).getValue("leaseExpiresAt")).isNull();
   }



   /*******************************************************************************
    ** A reclaimed token cannot overwrite the next worker's checkpoint.
    *******************************************************************************/
   @Test
   void staleCheckpointCannotOverwriteProgress() throws Exception
   {
      Integer jobId = launch();
      JobLease lease = expireAndReap(jobId);
      assertThatThrownBy(() -> store.checkpoint(lease, "{}", null)).isInstanceOf(LeaseLostException.class);
      assertThat(chunk(jobId).getValue("checkpointJson")).isNull();
   }



   /*******************************************************************************
    ** An interrupted non-idempotent job needs review before explicit retry.
    *******************************************************************************/
   @Test
   void nonIdempotentExpiryNeedsReviewThenExplicitRetry() throws Exception
   {
      Integer jobId = DurableJobs.launch(new LaunchRequest(DurableJobsTestSupport.PROCESS_NOT_IDEMPOTENT).withValue("seed", "review"));
      expireAndReap(jobId);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("NEEDS_REVIEW");
      DurableJobs.evaluate(jobId);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("NEEDS_REVIEW");
      DurableJobs.control(jobId, JobAction.RETRY_FAILED);
      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.COMPLETED);
      assertThat(chunk(jobId).getValueInteger("attempt")).isEqualTo(1);
   }



   /*******************************************************************************
    ** An expired lease at the retry limit fails instead of retrying.
    *******************************************************************************/
   @Test
   void expiredFinalAttemptFails() throws Exception
   {
      Integer jobId = launch();
      store.setChunkStatuses(jobId, List.of(ChunkStatus.PENDING), ChunkStatus.PENDING, Map.of("attempt", 2));
      JobLease lease = expireAndReap(jobId);
      assertThat(lease.attempt()).isEqualTo(3);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("FAILED");
      DurableJobs.evaluate(jobId);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("FAILED");
   }



   /*******************************************************************************
    ** Wait against the store clock until a one-second lease is actually expired.
    *******************************************************************************/
   private JobLease expireAndReap(Integer jobId) throws Exception
   {
      JobLease lease = store.claimChunk(chunk(jobId), "A", 1).orElseThrow();
      awaitCondition("lease expiry", () -> store.now().isAfter(lease.expiresAt()));
      assertThat(store.reapExpired("B")).hasSize(1);
      return (lease);
   }
}

