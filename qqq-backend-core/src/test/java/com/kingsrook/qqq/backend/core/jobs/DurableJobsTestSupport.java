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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.alerts.AlertResolution;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.AlertSinkInterface;
import com.kingsrook.qqq.backend.core.alerts.NotificationSignal;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
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
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule;
import com.kingsrook.qqq.backend.core.realtime.RealtimeBusInterface;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMessage;
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
   public static final List<String> START_ORDER = new CopyOnWriteArrayList<>();
   public static final List<AlertSignal> ALERTS = new CopyOnWriteArrayList<>();
   public static final List<AlertResolution> RESOLUTIONS = new CopyOnWriteArrayList<>();
   public static final List<NotificationSignal> NOTIFICATIONS = new CopyOnWriteArrayList<>();
   public static final List<RealtimeMessage> MESSAGES = new CopyOnWriteArrayList<>();

   public static volatile long    stepSleepMillis = 0;
   public static volatile boolean failStep3       = false;
   public static volatile CountDownLatch step1Gate;



   /*******************************************************************************
    ** A fresh instance with durable jobs on the memory backend.
    *******************************************************************************/
   public static QInstance defineInstance(DurableJobsMetaData settings) throws QException
   {
      STEP_RUNS.clear();
      SEEN.clear();
      START_ORDER.clear();
      ALERTS.clear();
      RESOLUTIONS.clear();
      NOTIFICATIONS.clear();
      MESSAGES.clear();
      stepSleepMillis = 0;
      failStep3 = false;
      step1Gate = null;

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
         if(input.getValueString("seed") != null)
         {
            START_ORDER.add(input.getValueString("seed"));
         }
         ran("step1");
         CountDownLatch gate = step1Gate;
         if(gate != null)
         {
            try
            {
               if(!gate.await(15, TimeUnit.SECONDS))
               {
                  throw (new QException("Timed out waiting for the test to release step 1"));
               }
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
               throw (new QException("Interrupted waiting for the test's step 1 gate", e));
            }
         }
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



   /*******************************************************************************
    ** Capture only messages published by the configured realtime bus.
    *******************************************************************************/
   public static class CapturingBus implements RealtimeBusInterface
   {
      /*******************************************************************************
       ** Record a published event without retaining application subscribers.
       *******************************************************************************/
      @Override
      public void publish(RealtimeMessage message)
      {
         MESSAGES.add(message);
      }



      /*******************************************************************************
       ** These tests capture publishes directly and need no live subscription.
       *******************************************************************************/
      @Override
      public AutoCloseable subscribe(Consumer<RealtimeMessage> listener)
      {
         return (() ->
         {
         });
      }
   }



   /*******************************************************************************
    ** Refuse an owner whose authentication has been revoked.
    *******************************************************************************/
   public static class RejectingCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       ** Reject only the revoked owner when building a worker session.
       *******************************************************************************/
      @Override
      public void customizeAutomatedSessionForUser(QInstance instance, QSession session, Serializable userId) throws QAuthenticationException
      {
         if("revoked".equals(userId))
         {
            throw (new QAuthenticationException("Owner access revoked"));
         }
      }
   }



   /*******************************************************************************
    ** Model an authentication provider that cannot find a deleted user.
    *******************************************************************************/
   public static class MissingOwnerAuthentication extends MockAuthenticationModule
   {
      /*******************************************************************************
       ** Return no automated session for the deleted owner.
       *******************************************************************************/
      @Override
      public QSession createAutomatedSessionForUser(QInstance instance, Serializable userId) throws QAuthenticationException
      {
         return ("missing".equals(userId) ? null : super.createAutomatedSessionForUser(instance, userId));
      }
   }



   /*******************************************************************************
    ** Capture alerts and notifications safely across worker threads.
    *******************************************************************************/
   public static class CapturingSink implements AlertSinkInterface
   {
      /*******************************************************************************
       ** Record each failure alert.
       *******************************************************************************/
      @Override
      public void raise(AlertSignal signal)
      {
         ALERTS.add(signal);
      }



      /*******************************************************************************
       ** Record each alert resolution.
       *******************************************************************************/
      @Override
      public void resolve(AlertResolution resolution)
      {
         RESOLUTIONS.add(resolution);
      }



      /*******************************************************************************
       ** Reaper heartbeats need no test side effects.
       *******************************************************************************/
      @Override
      public void heartbeat(String key, Instant at)
      {
      }



      /*******************************************************************************
       ** Record each completion notification.
       *******************************************************************************/
      @Override
      public void notify(NotificationSignal signal)
      {
         NOTIFICATIONS.add(signal);
      }
   }

}
