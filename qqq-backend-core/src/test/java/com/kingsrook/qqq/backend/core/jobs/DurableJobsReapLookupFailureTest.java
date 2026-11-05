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


import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.JobLease;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Reclaiming an expired chunk must not assume the job is idempotent just because
 ** the job could not be read: a transient lookup failure must send a non-idempotent
 ** chunk to review, never back to a retry that could run it twice.
 *******************************************************************************/
class DurableJobsReapLookupFailureTest extends DurableJobsTestBase
{
   /*******************************************************************************
    ** The job lookup fails while reaping a non-idempotent chunk.
    *******************************************************************************/
   @Test
   void unreadableJobIsNotAssumedIdempotent() throws Exception
   {
      Integer  jobId = DurableJobs.launch(new LaunchRequest(DurableJobsTestSupport.PROCESS_NOT_IDEMPOTENT).withValue("seed", "lookup"));
      JobLease lease = store.claimChunk(chunk(jobId), "A", 1).orElseThrow();
      awaitCondition("lease expiry", () -> store.now().isAfter(lease.expiresAt()));

      AtomicBoolean   failedOnce    = new AtomicBoolean(false);
      DurableJobStore failingLookup = new DurableJobStore(settings)
      {
         @Override
         public QRecord getJob(Integer id) throws QException
         {
            if(failedOnce.compareAndSet(false, true))
            {
               throw (new QException("simulated transient lookup failure"));
            }
            return (super.getJob(id));
         }
      };
      assertThat(failingLookup.reapExpired("B")).hasSize(1);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("NEEDS_REVIEW");
   }
}
