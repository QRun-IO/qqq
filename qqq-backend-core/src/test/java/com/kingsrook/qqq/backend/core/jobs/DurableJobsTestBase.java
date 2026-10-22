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
import java.util.concurrent.Callable;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Shared lifecycle and bounded waits for the durable job acceptance tests.
 *******************************************************************************/
abstract class DurableJobsTestBase extends BaseTest
{
   protected QInstance instance;
   protected DurableJobsMetaData settings;
   protected DurableJobStore store;



   /*******************************************************************************
    ** Give every test a fresh instance and memory store.
    *******************************************************************************/
   @BeforeEach
   void initializeJobs() throws QException
   {
      settings = DurableJobsTestSupport.fastSettings();
      instance = DurableJobsTestSupport.defineInstance(settings);
      QContext.init(instance, newSession());
      store = new DurableJobStore(settings);
   }



   /*******************************************************************************
    ** Drain all workers before BaseTest clears their shared memory store.
    *******************************************************************************/
   @AfterEach
   void stopJobs()
   {
      if(DurableJobsTestSupport.step1Gate != null)
      {
         DurableJobsTestSupport.step1Gate.countDown();
      }
      DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));
   }



   /*******************************************************************************
    ** Launch the three-step fixture with checkpointable input.
    *******************************************************************************/
   protected Integer launch() throws QException
   {
      return (DurableJobs.launch(new LaunchRequest(DurableJobsTestSupport.PROCESS_THREE_STEPS).withValue("seed", "test")));
   }



   /*******************************************************************************
    ** Read the fixture's single chunk.
    *******************************************************************************/
   protected QRecord chunk(Integer jobId) throws QException
   {
      return (store.getChunks(jobId).get(0));
   }



   /*******************************************************************************
    ** Await the requested status and fail promptly on an unexpected terminal state.
    *******************************************************************************/
   protected QRecord await(Integer jobId, JobStatus status) throws QException
   {
      QRecord job = DurableJobsTestSupport.awaitStatus(jobId, Duration.ofSeconds(15), status,
         JobStatus.FAILED, JobStatus.CANCELLED, JobStatus.COMPLETED, JobStatus.COMPLETED_WITH_ERRORS, JobStatus.NEEDS_REVIEW);
      assertThat(job.getValueString("status")).isEqualTo(status.name());
      return (job);
   }



   /*******************************************************************************
    ** Poll an observable condition without guessing when a worker will run.
    *******************************************************************************/
   protected static void awaitCondition(String description, Callable<Boolean> condition) throws Exception
   {
      long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
      while(System.nanoTime() < deadline)
      {
         if(condition.call())
         {
            return;
         }
         Thread.sleep(10);
      }
      throw (new AssertionError("Timed out waiting for " + description));
   }



   /*******************************************************************************
    ** Read timeline event types for assertions after workers have settled.
    *******************************************************************************/
   protected List<String> events(Integer jobId) throws QException
   {
      return (store.query(DurableJobsTables.TABLE_EVENT, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)))
         .stream().map(record -> record.getValueString("type")).toList());
   }
}

