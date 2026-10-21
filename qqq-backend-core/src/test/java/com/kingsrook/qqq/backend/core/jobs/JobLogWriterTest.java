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


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.actions.tables.ConditionalUpdateAction;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkOutcome;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobLease;
import com.kingsrook.qqq.backend.core.jobs.progress.JobLogLevel;
import com.kingsrook.qqq.backend.core.jobs.progress.JobLogWriter;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgressLifecycle;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgressSnapshot;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobFinalizer;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Memory backend integration of cap, throttle, fencing and lifecycle writes.
 *******************************************************************************/
class JobLogWriterTest extends DurableJobsTestBase
{
   /*******************************************************************************
    ** Flush triggers and pruning preserve severe lines and count discarded lines.
    *******************************************************************************/
   @Test
   void throttleAndCap() throws Exception
   {
      Integer id = launch();
      settings.withLogMaxLines(3).withLogRedactor(text -> text == null ? null : text.replace("sensitive-app", "[redacted]"));
      JobLease lease = store.claimChunk(chunk(id), "test", 60).orElseThrow();
      JobLogWriter writer = new JobLogWriter(store, id, lease, null);
      writer.append(JobLogLevel.WARN, "load", "Keep warning");
      assertThat(writer.flush(null, true)).isTrue();
      writer.append(JobLogLevel.DEBUG, "load", "oldest");
      assertThat(writer.flush(null, false)).isFalse();
      writer.append(JobLogLevel.INFO, "load", "old");
      writer.append(JobLogLevel.ERROR, "load", "Keep error");
      writer.append(JobLogLevel.INFO, "load", "sensitive-app");
      assertThat(writer.flush(null, true)).isTrue();
      assertThat(lines(id)).extracting(r -> r.getValueString("message")).containsExactly("Keep warning", "Keep error", "[redacted]");
      assertThat(store.getJob(id).getValueInteger("logDropped")).isEqualTo(2);
      Thread.sleep(1050);
      writer.append(JobLogLevel.INFO, null, "next");
      assertThat(writer.flush(null, false)).isTrue();
      assertThat(writer.flush(new JobProgressSnapshot().withPercent(12.5), true)).isTrue();
      assertThat(store.getJob(id).getValueInteger("percentDone")).isEqualTo(12);
      assertThat(JobProgressLifecycle.snapshot(store.getJob(id)).getPercent()).isEqualTo(12.5);
   }



   /*******************************************************************************
    ** A stale token drops the buffer and marks the next page for interruption.
    *******************************************************************************/
   @Test
   void lostLeaseDropsFlush() throws Exception
   {
      Integer id = launch();
      JobLease lease = store.claimChunk(chunk(id), "old", 60).orElseThrow();
      AtomicBoolean lost = new AtomicBoolean();
      JobLogWriter writer = new JobLogWriter(store, id, lease, () -> lost.set(true));
      store.releaseChunk(lease, ChunkStatus.PENDING);
      store.claimChunk(chunk(id), "new", 60).orElseThrow();
      writer.append(JobLogLevel.ERROR, null, "zombie");
      assertThat(writer.flush(new JobProgressSnapshot().withPercent(99.0), true)).isFalse();
      assertThat(lost).isTrue();
      assertThat(lines(id)).isEmpty();
      assertThat(store.getJob(id).getValueInteger("percentDone")).isNull();
   }



   /*******************************************************************************
    ** Reclaim resets live work and preserves attempt lines; finish produces 100.
    *******************************************************************************/
   @Test
   void reclaimAndFinish() throws Exception
   {
      Integer id = launch();
      JobLease lease = store.claimChunk(chunk(id), "dead", 60).orElseThrow();
      JobLogWriter writer = new JobLogWriter(store, id, lease, null);
      writer.append(JobLogLevel.INFO, "load", "attempt one");
      writer.flush(new JobProgressSnapshot().withPercent(90.0).withCurrentPhase("load").withMessage("working"), true);
      new ConditionalUpdateAction().execute(new ConditionalUpdateInput(DurableJobsTables.TABLE_CHUNK)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, lease.chunkId())))
         .withValues(Map.of("leaseExpiresAt", Instant.now().minusSeconds(1))));
      assertThat(store.reapExpired("new")).hasSize(1);
      assertThat(store.reapExpired("new")).isEmpty();
      assertThat(store.getJob(id).getValueInteger("percentDone")).isZero();
      assertThat(store.getJob(id).getValueString("currentPhase")).isNull();
      assertThat(lines(id)).extracting(r -> r.getValueString("message")).containsExactly("attempt one", "Attempt 1 was interrupted (the server stopped); retrying");
      JobLease next = store.claimChunk(chunk(id), "new", 60).orElseThrow();
      store.completeChunk(next, new ChunkOutcome(5, 5, 0, null, null), null);
      new JobFinalizer(store).evaluate(id);
      assertThat(store.getJob(id).getValueInteger("percentDone")).isEqualTo(100);
      assertThat(lines(id)).hasSize(3);
      assertThat(lines(id).getLast().getValueString("message")).contains("Completed");
   }



   /*******************************************************************************
    ** Ordered log records.
    *******************************************************************************/
   private List<QRecord> lines(Integer id) throws Exception
   {
      return (store.query(DurableJobsTables.TABLE_LOG, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, id)).withOrderBy(new QFilterOrderBy("id"))));
   }
}
