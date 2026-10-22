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


import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobStatus;
import com.kingsrook.qqq.backend.core.actions.async.NonPersistedAsyncJobCallback;
import com.kingsrook.qqq.backend.core.jobs.plans.ChunkContext;
import com.kingsrook.qqq.backend.core.jobs.plans.JobChunkScope;
import com.kingsrook.qqq.backend.core.jobs.progress.JobPhase;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgress;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgressAsyncJobCallback;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Existing callbacks and new reporters share the same progress and safe log.
 *******************************************************************************/
class JobProgressAdapterTest extends DurableJobsTestBase
{
   /*******************************************************************************
    ** Durable callback updates use the chunk reporter and lease.
    *******************************************************************************/
   @Test
   void durableAdapter() throws Exception
   {
      Integer id = launch();
      var lease = store.claimChunk(chunk(id), "adapter", 60).orElseThrow();
      ChunkContext context = new ChunkContext(store.getJob(id), chunk(id), lease, store, () -> false);
      JobProgress progress = context.getProgress();
      JobProgressAsyncJobCallback callback = new JobProgressAsyncJobCallback(progress);
      RunBackendStepInput input = new RunBackendStepInput();
      input.setAsyncJobCallback(callback);
      assertThat(JobProgress.of(input)).isSameAs(progress);
      callback.updateStatus("Loading", 2, 10);
      callback.incrementCurrent();
      progress.flush();
      assertThat(store.getJob(id).getValueInteger("percentDone")).isEqualTo(30);
      progress.phases(JobPhase.of("load", "Load", 1)).phase("load").total(10, "rows").done(5).bytesTotal(100).bytesDone(60).message("password=hidden").info("safe").warn("warning").error("error").debug("debug");
      progress.flush();
      assertThat(store.getJob(id).getValueString("progressMessage")).doesNotContain("hidden");
      try(JobChunkScope scope = JobChunkScope.open(null, context::check, context.identity()).withProgress(progress))
      {
         assertThat(JobProgress.current()).isSameAs(progress);
         assertThat(JobProgress.of(null)).isSameAs(progress);
      }
   }



   /*******************************************************************************
    ** Inline status retains a redacted two-hundred-line ring.
    *******************************************************************************/
   @Test
   void inlineAdapter()
   {
      AsyncJobStatus status = new AsyncJobStatus();
      RunBackendStepInput input = new RunBackendStepInput();
      input.setAsyncJobCallback(new NonPersistedAsyncJobCallback(UUID.randomUUID(), status));
      JobProgress progress = JobProgress.of(input);
      assertThat(JobProgress.of(input)).isSameAs(progress);
      progress.total(10, "files").done(4).message("Working");
      for(int i = 0; i < 205; i++)
      {
         progress.info("password=hidden count " + i);
      }
      assertThat(status.getProgress().getPercent()).isEqualTo(40.0);
      assertThat(status.getCurrent()).isEqualTo(4);
      assertThat(status.getLogTail()).hasSize(200);
      assertThat(status.getLogTail().getFirst().getId()).isEqualTo(6);
      assertThat(status.getLogTail()).allSatisfy(line -> assertThat(line.getMessage()).doesNotContain("hidden"));
      assertThat(JobProgress.of(new RunBackendStepInput())).isNotNull();
   }
}
