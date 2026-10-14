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
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Regression coverage for DurableJobsDrain.
 *******************************************************************************/
class DurableJobsDrainTest extends DurableJobsTestBase
{

   /*******************************************************************************
    ** Graceful drain releases the lease without charging an attempt or replaying completed steps.
    *******************************************************************************/
   @Test
   void drainReleasesCheckpointAndResumesWithoutRepeatingSteps() throws Exception
   {
      DurableJobsTestSupport.stepSleepMillis = 400;
      Integer jobId = launch();
      DurableJobsRuntime.start(instance);
      awaitCondition("step two running", () -> DurableJobsTestSupport.runs("step2") == 1);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("RUNNING");
      DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("PENDING");
      assertThat(chunk(jobId).getValueInteger("attempt")).isZero();
      assertThat(chunk(jobId).getValueString("checkpointJson")).contains("step2");
      assertThat(events(jobId)).contains("RELEASED");
      assertThat(store.query(DurableJobsTables.TABLE_WORKER, new QQueryFilter())).singleElement()
         .satisfies(worker -> assertThat(worker.getValueBoolean("draining")).isTrue());
      assertThat(DurableJobsTestSupport.runs("step3")).isZero();

      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.COMPLETED);
      assertThat(DurableJobsTestSupport.runs("step1")).isEqualTo(1);
      assertThat(DurableJobsTestSupport.runs("step2")).isEqualTo(1);
      assertThat(DurableJobsTestSupport.runs("step3")).isEqualTo(1);
      assertThat(DurableJobsTestSupport.SEEN.get("step3SawFromStep2")).isEqualTo("two:one:test");
   }
}

