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


import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;


/*******************************************************************************
 ** Shared fixtures for durable-jobs tests: an instance with the jobs tables on
 ** the memory backend, and a three-step process whose steps count their runs
 ** and pass values along (step 2 sets "fromStep2", step 3 reads it).
 *******************************************************************************/
public class DurableJobsTestSupport
{
   public static final String PROCESS_THREE_STEPS = "threeStepTestProcess";
   public static final String PROCESS_NOT_IDEMPOTENT = "notIdempotentTestProcess";

   public static final Map<String, AtomicInteger> STEP_RUNS = new ConcurrentHashMap<>();
   public static final Map<String, Serializable>  SEEN      = new ConcurrentHashMap<>();

   public static volatile long    stepSleepMillis = 0;
   public static volatile boolean failStep3       = false;



   /*******************************************************************************
    ** A fresh instance with durable jobs on the memory backend.
    *******************************************************************************/
   public static QInstance defineInstance(DurableJobsMetaData settings) throws QException
   {
      STEP_RUNS.clear();
      SEEN.clear();
      stepSleepMillis = 0;
      failStep3 = false;

      QInstance instance = TestUtils.defineInstance();
      new DurableJobsMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      instance.addProcess(threeStepProcess(PROCESS_THREE_STEPS, JobIdempotency.NATURAL));
      instance.addProcess(threeStepProcess(PROCESS_NOT_IDEMPOTENT, JobIdempotency.NONE));
      instance.withSupplementalMetaData(settings);
      return (instance);
   }



   /*******************************************************************************
    ** Fast settings for tests.
    *******************************************************************************/
   public static DurableJobsMetaData fastSettings()
   {
      return (new DurableJobsMetaData()
         .withWorkerThreads(1)
         .withLeaseTtlSeconds(3)
         .withReaperIntervalSeconds(1)
         .withPollIntervalMillis(50)
         .withPodId("test-pod"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static QProcessMetaData threeStepProcess(String name, JobIdempotency idempotency)
   {
      QProcessMetaData process = new QProcessMetaData()
         .withName(name)
         .withLabel("Three Step Test")
         .withStepList(List.of(
            new QBackendStepMetaData().withName("step1").withCode(new QCodeReference(Step1.class)),
            new QBackendStepMetaData().withName("step2").withCode(new QCodeReference(Step2.class)),
            new QBackendStepMetaData().withName("step3").withCode(new QCodeReference(Step3.class))));
      process.withSupplementalMetaData(new DurableProcessMetaData().withEligible(true).withIdempotency(idempotency));
      return (process);
   }



   /*******************************************************************************
    ** Wait until a job reaches one of the statuses, or fail after the timeout.
    *******************************************************************************/
   public static QRecord awaitStatus(Integer jobId, Duration timeout, JobStatus... statuses) throws QException
   {
      long            deadline = System.currentTimeMillis() + timeout.toMillis();
      DurableJobStore store    = new DurableJobStore(DurableJobsMetaData.of(QContext.getQInstance()));
      QRecord         job      = null;
      while(System.currentTimeMillis() < deadline)
      {
         job = store.getJob(jobId);
         for(JobStatus status : statuses)
         {
            if(status.name().equals(job.getValueString("status")))
            {
               return (job);
            }
         }
         SleepUtils.sleep(25, java.util.concurrent.TimeUnit.MILLISECONDS);
      }
      throw (new AssertionError("Job " + jobId + " did not reach " + List.of(statuses) + " in " + timeout + "; it is " + (job == null ? null : job.getValueString("status"))
         + " (" + (job == null ? null : job.getValueString("errorMessage")) + ")"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static int runs(String step)
   {
      return (STEP_RUNS.computeIfAbsent(step, k -> new AtomicInteger()).get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void ran(String step)
   {
      STEP_RUNS.computeIfAbsent(step, k -> new AtomicInteger()).incrementAndGet();
      if(stepSleepMillis > 0)
      {
         SleepUtils.sleep(stepSleepMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
      }
   }



   /*******************************************************************************
    ** Step 1: records that it ran, and the user it ran as.
    *******************************************************************************/
   public static class Step1 implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         ran("step1");
         SEEN.put("userId", QContext.getQSession().getUser().getIdReference());
         if(QContext.getQSession().getClientChannel() != null)
         {
            SEEN.put("clientChannel", QContext.getQSession().getClientChannel());
         }
         output.addValue("fromStep1", "one:" + input.getValueString("seed"));
      }
   }



   /*******************************************************************************
    ** Step 2: passes a value on to step 3.
    *******************************************************************************/
   public static class Step2 implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         ran("step2");
         output.addValue("fromStep2", "two:" + input.getValueString("fromStep1"));
      }
   }



   /*******************************************************************************
    ** Step 3: records what step 2 left; can be told to fail.
    *******************************************************************************/
   public static class Step3 implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         ran("step3");
         if(failStep3)
         {
            throw (new QException("step 3 was told to fail"));
         }
         SEEN.put("step3SawFromStep2", input.getValueString("fromStep2"));
      }
   }

}
