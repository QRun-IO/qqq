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
import com.kingsrook.qqq.backend.core.jobs.progress.JobPhase;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgressMath;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgressSnapshot;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Progress arithmetic, fallbacks, attempt resets and sliding ETA.
 *******************************************************************************/
class JobProgressMathTest
{
   /*******************************************************************************
    ** Weighted phases prefer bytes and count skipped phases as finished.
    *******************************************************************************/
   @Test
   void weightedPhases()
   {
      JobPhase first = JobPhase.of("a", "A", 70).withStatus(JobPhase.Status.DONE);
      JobPhase second = JobPhase.of("b", "B", 30).withStatus(JobPhase.Status.RUNNING);
      JobProgressSnapshot snapshot = new JobProgressSnapshot().withPhases(List.of(first, second)).withDone(1L).withTotal(10L).withBytesDone(50L).withBytesTotal(100L);
      new JobProgressMath().update(snapshot, null, 1, Instant.EPOCH);
      assertThat(snapshot.getPercent()).isEqualTo(85.0);
      first.setStatus(JobPhase.Status.SKIPPED);
      snapshot.setBytesTotal(null);
      new JobProgressMath().update(snapshot, null, 1, Instant.EPOCH);
      assertThat(snapshot.getPercent()).isEqualTo(73.0);
   }



   /*******************************************************************************
    ** Counts, committed rows, committed chunks and unknown totals.
    *******************************************************************************/
   @Test
   void fallbacks()
   {
      JobProgressSnapshot snapshot = new JobProgressSnapshot().withDone(3L).withTotal(4L);
      new JobProgressMath().update(snapshot, null, 1, Instant.EPOCH);
      assertThat(snapshot.getPercent()).isEqualTo(75.0);
      snapshot.setTotal(null);
      QRecord job = new QRecord().withValue("rowsDone", 2).withValue("rowsTotal", 10).withValue("chunksDone", 1).withValue("chunksTotal", 2);
      new JobProgressMath().update(snapshot, job, 1, Instant.EPOCH);
      assertThat(snapshot.getPercent()).isEqualTo(20.0);
      job.setValue("rowsTotal", null);
      new JobProgressMath().update(snapshot, job, 1, Instant.EPOCH);
      assertThat(snapshot.getPercent()).isEqualTo(50.0);
      new JobProgressMath().update(snapshot, null, 1, Instant.EPOCH);
      assertThat(snapshot.getPercent()).isNull();
      assertThat(snapshot.getEtaSeconds()).isNull();
   }



   /*******************************************************************************
    ** An attempt is monotonic; a retry resets the rate window and high water.
    *******************************************************************************/
   @Test
   void attemptsAndEta()
   {
      JobProgressMath math = new JobProgressMath();
      JobProgressSnapshot snapshot = new JobProgressSnapshot().withDone(10L).withTotal(100L);
      math.update(snapshot, null, 1, Instant.EPOCH);
      assertThat(snapshot.getEtaSeconds()).isNull();
      snapshot.setDone(20L);
      math.update(snapshot, null, 1, Instant.EPOCH.plusSeconds(10));
      assertThat(snapshot.getEtaSeconds()).isEqualTo(80);
      snapshot.setDone(5L);
      math.update(snapshot, null, 1, Instant.EPOCH.plusSeconds(11));
      assertThat(snapshot.getPercent()).isEqualTo(20.0);
      math.update(snapshot, null, 2, Instant.EPOCH.plusSeconds(12));
      assertThat(snapshot.getPercent()).isEqualTo(5.0);
      assertThat(snapshot.getEtaSeconds()).isNull();
      math.update(snapshot, null, 2, Instant.EPOCH.plusSeconds(100));
      assertThat(snapshot.getEtaSeconds()).isNull();
   }
}
