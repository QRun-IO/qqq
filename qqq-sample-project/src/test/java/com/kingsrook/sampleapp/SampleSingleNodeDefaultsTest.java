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

package com.kingsrook.sampleapp;


import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.cluster.ClusterBusMetaData;
import com.kingsrook.qqq.backend.core.cluster.QNodeLifecycle;
import com.kingsrook.qqq.backend.core.cluster.QNodeRoles;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.jobs.DurableProcessMetaData;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.simple.SimpleSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.QStateProviders;
import com.kingsrook.qqq.backend.core.state.StateProviderMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.middleware.javalin.cluster.JavalinClusterSupport;
import com.kingsrook.sampleapp.cluster.SampleClusterHealthProducer;
import com.kingsrook.sampleapp.cluster.SampleClusterMode;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import com.kingsrook.sampleapp.processes.cluster.SampleClusterProcesses;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/*******************************************************************************
 ** Single-node (non-HA) mode, end to end, with nothing set: no node roles, no
 ** state table, no encryption key, no cluster bus, H2. One sample server must
 ** run a process wizard across steps, poll and cancel an async step, hand off
 ** a one-time secret, run the scheduler and durable jobs, answer /ready, and
 ** stop on SIGTERM without a drain delay - exactly as before cluster support.
 ** The multi-node counterpart is SampleMultiNodeIT; see docs/DEPLOYMENT-MODES.md.
 *******************************************************************************/
@ResourceLock("qqq.sample.esb.port")
class SampleSingleNodeDefaultsTest
{
   private static final List<String> CLUSTER_ENVIRONMENT = List.of("QQQ_NODE_ROLES", "QQQ_HTTP_ENABLED", "QQQ_SCHEDULE_MANAGER_ENABLED",
      "QQQ_ESB_TRIGGERS_ENABLED", "QQQ_JOBS_WORKER_THREADS", "QQQ_STATE_ENCRYPTION_KEY", "QQQ_DRAIN_DELAY_SECONDS", "SAMPLE_CLUSTER_MODE");

   private static final String TICK_PROCESS    = "singleNodeTick";
   private static final String DURABLE_PROCESS = "singleNodeDurable";

   private static final AtomicInteger TICKS        = new AtomicInteger();
   private static final AtomicInteger DURABLE_RUNS = new AtomicInteger();

   private final AtomicReference<Javalin> service = new AtomicReference<>();

   private QInstance           instance;
   private SampleJavalinServer server;
   private QScheduleManager    scheduleManager;
   private SampleProcessHttp   http;



   /*******************************************************************************
    ** The default sample instance, plus the cluster demo processes (the same ones
    ** the multi-node test runs), probes, a scheduled process and durable jobs on
    ** the memory backend - and no cluster configuration at all.
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      for(String name : CLUSTER_ENVIRONMENT)
      {
         assumeTrue(System.getenv(name) == null, "single-node defaults need an environment without " + name);
      }
      assumeTrue(System.getProperty("qqq.node.roles") == null && !SampleClusterMode.isEnabled());
      QNodeRoles.set(null);
      QNodeLifecycle.resetForTests();
      QStateProviders.clearForTests();
      TICKS.set(0);
      DURABLE_RUNS.set(0);

      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      SampleClusterProcesses.defineAll(instance);
      new SampleClusterHealthProducer().produce(instance);

      QScheduleManager.defineDefaultSchedulableTypesInInstance(instance);
      instance.addScheduler(new SimpleSchedulerMetaData().withName("singleNodeSimple"));
      instance.addProcess(new QProcessMetaData().withName(TICK_PROCESS)
         .withStep(new QBackendStepMetaData().withName("tick").withCode(new QCodeReferenceLambda<BackendStep>((in, out) -> TICKS.incrementAndGet())))
         .withSchedule(new QScheduleMetaData().withSchedulerName("singleNodeSimple").withRepeatMillis(100).withInitialDelayMillis(0)));

      new DurableJobsMetaDataProvider().defineAll(instance, SampleMetaDataProvider.MEMORY_BACKEND_NAME, null);
      instance.withSupplementalMetaData(new DurableJobsMetaData().withWorkerThreads(1).withPollIntervalMillis(50).withReaperIntervalSeconds(1).withLeaseTtlSeconds(5));
      instance.addProcess(new QProcessMetaData().withName(DURABLE_PROCESS)
         .withStep(new QBackendStepMetaData().withName("run").withCode(new QCodeReferenceLambda<BackendStep>((in, out) -> DURABLE_RUNS.incrementAndGet())))
         .withSupplementalMetaData(new DurableProcessMetaData().withEligible(true).withIdempotency(JobIdempotency.NATURAL)));

      QInstance owned = instance;
      server = new SampleJavalinServer(new SampleMetaDataProvider()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return owned;
         }
      });
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      server.start();
      http = new SampleProcessHttp(URI.create("http://localhost:" + service.get().port()), UUID.randomUUID().toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void stop()
   {
      try
      {
         if(scheduleManager != null)
         {
            try
            {
               scheduleManager.stop();
            }
            finally
            {
               scheduleManager.unInit();
            }
         }
         if(instance != null)
         {
            DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));
         }
         if(server != null)
         {
            server.stop();
         }
      }
      finally
      {
         QNodeRoles.set(null);
         QNodeLifecycle.resetForTests();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** The resolved defaults equal the old single-node behavior: every role, the
    ** instance's worker threads, in-memory state and bus, no state table or key.
    *******************************************************************************/
   @Test
   void defaultsAreOneNodeWithEveryRole()
   {
      QNodeRoles.Settings settings = QNodeRoles.get();
      assertEquals(EnumSet.allOf(QNodeRoles.Role.class), settings.roles());
      assertTrue(settings.http() && settings.scheduler() && settings.esbTriggers());
      assertNull(settings.workerThreads(), "no roles set: durable-job workers use the instance's worker threads");
      assertTrue(QScheduleManager.isEnabledOnThisNode());

      assertNull(StateProviderMetaData.of(instance));
      assertNull(ClusterBusMetaData.of(instance));
      assertNull(instance.getTable(SampleClusterMode.STATE_TABLE_NAME), "no qqq_state table is needed");
      assertFalse(QStateProviders.isShared(instance));
      assertSame(InMemoryStateProvider.getInstance(), QStateProviders.forInstance(instance));
   }



   /*******************************************************************************
    ** A wizard runs across steps on the one node, keeping its state between them.
    *******************************************************************************/
   @Test
   void wizardRunsAcrossSteps() throws Exception
   {
      JSONObject started = http.init(SampleClusterProcesses.PROCESS_WIZARD, null, 10_000);
      assertEquals("input", started.getString("nextStep"));
      JSONObject values = started.getJSONObject("values");
      String     token  = values.getString("token");
      assertEquals(QNodeRoles.nodeId(), values.getString("startedOn"));

      JSONObject remembered = http.step(SampleClusterProcesses.PROCESS_WIZARD, started.getString("processUUID"), "input", "{\"note\":\"one node\"}");
      assertEquals(QNodeRoles.nodeId() + "|" + token + "|one node", remembered.getJSONObject("values").getString("summary"));
      assertEquals(QNodeRoles.nodeId(), remembered.getJSONObject("values").getString("rememberedOn"));
   }



   /*******************************************************************************
    ** An async step reports progress to status polls, finishes, and can be
    ** cancelled from another request.
    *******************************************************************************/
   @Test
   void asyncStepPollsAndCancels() throws Exception
   {
      JSONObject started = http.init(SampleClusterProcesses.PROCESS_WORK, "{\"seconds\":1}", 1);
      assertEquals("JOB_STARTED", started.getString("type"), started.toString());
      JSONObject done = http.awaitDone(SampleClusterProcesses.PROCESS_WORK, started.getString("processUUID"), started.getString("jobUUID"), Duration.ofSeconds(15));
      assertEquals("COMPLETE", done.getString("type"), done.toString());
      assertFalse(done.getJSONObject("values").getBoolean("cancelled"));

      JSONObject longRun = http.init(SampleClusterProcesses.PROCESS_WORK, "{\"seconds\":30}", 1);
      assertEquals("JOB_STARTED", longRun.getString("type"));
      JSONObject running = awaitRunningWithMessage(longRun.getString("processUUID"), longRun.getString("jobUUID"));
      assertTrue(running.getString("message").startsWith("Working on"), running.toString());

      http.run(SampleClusterProcesses.PROCESS_CANCEL_JOB, "{\"jobUUID\":\"" + longRun.getString("jobUUID") + "\"}");
      JSONObject cancelled = http.awaitDone(SampleClusterProcesses.PROCESS_WORK, longRun.getString("processUUID"), longRun.getString("jobUUID"), Duration.ofSeconds(15));
      assertEquals("COMPLETE", cancelled.getString("type"), cancelled.toString());
      assertTrue(cancelled.getJSONObject("values").getBoolean("cancelled"));
   }



   /*******************************************************************************
    ** One-time secrets hand off once: from a synchronous step, and from an async
    ** step's status poll (kept in memory - no key, no shared store).
    *******************************************************************************/
   @Test
   void oneTimeSecretsHandOffOnce() throws Exception
   {
      JSONObject direct = http.run(SampleClusterProcesses.PROCESS_SECRET, null);
      assertTrue(secretValue(direct).startsWith("sample-secret-"), direct.toString());

      JSONObject started = http.init(SampleClusterProcesses.PROCESS_SECRET, "{\"sleepMillis\":750}", 1);
      assertEquals("JOB_STARTED", started.getString("type"));
      JSONObject done = http.awaitDone(SampleClusterProcesses.PROCESS_SECRET, started.getString("processUUID"), started.getString("jobUUID"), Duration.ofSeconds(15));
      assertTrue(secretValue(done).startsWith("sample-secret-"), done.toString());
      JSONObject again = http.status(SampleClusterProcesses.PROCESS_SECRET, started.getString("processUUID"), started.getString("jobUUID"));
      assertFalse(again.has("oneTimeSecrets") && !again.getJSONArray("oneTimeSecrets").isEmpty(), "a one-time secret is handed out once: " + again);
   }



   /*******************************************************************************
    ** The scheduler starts by default and runs the scheduled process; a durable
    ** job runs on this node's own workers.
    *******************************************************************************/
   @Test
   void schedulerAndDurableJobsRun() throws Exception
   {
      scheduleManager = QScheduleManager.initInstance(instance, () -> new QSession().withUser(new QUser().withIdReference("scheduled-user")));
      scheduleManager.start();
      assertTrue(scheduleManager.isStarted(), "the scheduler starts with no roles set");
      awaitCondition("two scheduled runs", () -> TICKS.get() >= 2);

      QContext.init(instance, new QSession().withUser(new QUser().withIdReference("single-node-user").withFullName("Single Node")));
      Integer jobId = DurableJobs.launch(new LaunchRequest(DURABLE_PROCESS));
      DurableJobStore store = new DurableJobStore(DurableJobsMetaData.of(instance));
      awaitCondition("the durable job to complete", () -> JobStatus.COMPLETED.name().equals(store.getJob(jobId).getValueString("status")));
      QRecord job = store.getJob(jobId);
      assertEquals(JobStatus.COMPLETED.name(), job.getValueString("status"));
      assertEquals(1, DURABLE_RUNS.get());
   }



   /*******************************************************************************
    ** /ready answers 200 with one replica, 503 once the node drains; SIGTERM's
    ** drain hook adds no delay when nothing is running (drain delay default 0).
    *******************************************************************************/
   @Test
   void readyAndDrainOnOneNode() throws Exception
   {
      HttpResponse<String> ready = http.get("/ready");
      assertEquals(200, ready.statusCode(), ready.body());
      assertEquals(200, http.get("/health").statusCode());

      AtomicLong stoppedAfter = new AtomicLong(-1);
      long       begin        = System.nanoTime();
      Thread     hook         = JavalinClusterSupport.installShutdownHook(() -> stoppedAfter.set(System.nanoTime() - begin));
      assertTrue(Runtime.getRuntime().removeShutdownHook(hook));
      hook.run();
      assertTrue(stoppedAfter.get() >= 0, "the drain hook stops the server");
      assertTrue(Duration.ofNanos(stoppedAfter.get()).compareTo(Duration.ofSeconds(2)) < 0, "no drain delay on a single node: " + Duration.ofNanos(stoppedAfter.get()));

      HttpResponse<String> draining = http.get("/ready");
      assertEquals(503, draining.statusCode(), draining.body());
      assertEquals("draining", new JSONObject(draining.body()).getString("reason"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject awaitRunningWithMessage(String processUUID, String jobUUID) throws Exception
   {
      long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
      while(System.nanoTime() < deadline)
      {
         JSONObject status = http.status(SampleClusterProcesses.PROCESS_WORK, processUUID, jobUUID);
         if("RUNNING".equals(status.optString("type")) && status.has("message"))
         {
            return (status);
         }
         Thread.sleep(50);
      }
      throw (new AssertionError("The step never reported progress"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static String secretValue(JSONObject response)
   {
      JSONArray secrets = response.optJSONArray("oneTimeSecrets");
      assertTrue(secrets != null && secrets.length() == 1, "one secret expected: " + response);
      return (secrets.getJSONObject(0).getString("value"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void awaitCondition(String description, java.util.concurrent.Callable<Boolean> condition) throws Exception
   {
      long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
      while(System.nanoTime() < deadline)
      {
         if(condition.call())
         {
            return;
         }
         Thread.sleep(25);
      }
      throw (new AssertionError("Timed out waiting for " + description));
   }
}
