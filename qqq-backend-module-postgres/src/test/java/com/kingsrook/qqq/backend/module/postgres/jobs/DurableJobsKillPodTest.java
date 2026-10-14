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


package com.kingsrook.qqq.backend.module.postgres.jobs;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTables;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;


/*******************************************************************************
 ** Kill-the-pod (spec section 6.1, STEPS plan): Postgres plus TWO separate
 ** QQQ JVMs (pods A and B), workerThreads=1, a 10s lease TTL and a 3s reaper.
 ** A runs a six-step job; once three steps are checkpointed, A is killed
 ** (kill -9, no shutdown hooks). B must reclaim the orphaned chunk within TTL
 ** plus the reaper interval, write a RECLAIMED event, and finish the job from
 ** the last checkpoint - steps A finished are never rerun, and the chunk ends at
 ** attempt 2.
 *******************************************************************************/
class DurableJobsKillPodTest extends BaseTest
{
   private final List<Process> pods = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void primeJobsTables() throws Exception
   {
      TestUtils.primeTestDatabase("prime-durable-jobs.sql");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void killPods()
   {
      pods.forEach(Process::destroyForcibly);
      pods.clear();
   }



   /*******************************************************************************
    ** kill -9 mid-job: reclaim, resume from checkpoint, exactly the steps owed.
    *******************************************************************************/
   @Test
   void testKillPodMidJob() throws Exception
   {
      DurableJobStore store = launcherContext();
      Integer         jobId = DurableJobs.launch(new LaunchRequest(KillTestInstance.PROCESS_SIX_STEPS).withValue("stepMillis", 1500).withLabel("Kill test"));

      Process podA = startPod("pod-A");
      await("pod-A holds the chunk with 3 steps checkpointed", Duration.ofSeconds(60), () -> checkpointedStep(store, jobId, "pod-A") >= 3);
      Process podB = startPod("pod-B");
      SleepUtils.sleep(2, TimeUnit.SECONDS); // B is up and idle (one chunk, and A holds it)

      int stepsDoneByA = checkpointedStep(store, jobId, "pod-A");
      podA.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
      Instant killedAt = Instant.now();

      QRecord job = awaitJob(store, jobId, Duration.ofSeconds(90), JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.NEEDS_REVIEW);
      assertThat(job.getValueString("status")).isEqualTo(JobStatus.COMPLETED.name());

      //////////////////////////////////////////////////////////////////////
      // B reclaimed it within TTL + reaper interval (+1s polling slack)  //
      //////////////////////////////////////////////////////////////////////
      QRecord reclaimed = events(store, jobId).stream().filter(e -> JobEventType.RECLAIMED.name().equals(e.getValueString("type"))).findFirst().orElseThrow();
      assertThat(reclaimed.getValueString("pod")).isEqualTo("pod-B");
      assertThat(reclaimed.getValueString("detail")).contains("pod-A");
      long reclaimSeconds = Duration.between(killedAt, reclaimed.getValueInstant("createDate")).toSeconds();
      assertThat(reclaimSeconds).isLessThanOrEqualTo(KillTestInstance.LEASE_TTL + KillTestInstance.REAPER_SECS + 1);

      QRecord chunk = store.getChunks(jobId).get(0);
      assertThat(chunk.getValueString("status")).isEqualTo(ChunkStatus.DONE.name());
      assertThat(chunk.getValueInteger("attempt")).isEqualTo(2);

      /////////////////////////////////////////////////////////////////////////
      // every step ran; the ones A checkpointed ran once, on A, never on B //
      /////////////////////////////////////////////////////////////////////////
      Map<String, List<String>> podsByStep = stepLog();
      for(int i = 1; i <= KillTestInstance.STEP_COUNT; i++)
      {
         List<String> ranOn = podsByStep.get("step" + i);
         assertThat(ranOn).as("step" + i).isNotEmpty();
         if(i <= stepsDoneByA)
         {
            assertThat(ranOn).as("checkpointed step" + i).containsExactly("pod-A");
         }
         else if(i > stepsDoneByA + 1)
         {
            assertThat(ranOn).as("step" + i + " after the kill").containsExactly("pod-B");
         }
      }
      assertThat(events(store, jobId).stream().map(e -> e.getValueString("type")).toList()).contains(JobEventType.LAUNCHED.name(), JobEventType.STARTED.name(), JobEventType.COMPLETED.name());
      podB.destroyForcibly();
   }



   /*******************************************************************************
    ** SIGTERM (graceful redeploy): A releases its lease at the next step
    ** boundary without counting the attempt; B continues at once; no
    ** checkpointed step reruns. (POSIX only: on Windows, destroy() is a kill.)
    *******************************************************************************/
   @Test
   void testGracefulDrainOnSigterm() throws Exception
   {
      assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"), "SIGTERM is not available on Windows");

      DurableJobStore store = launcherContext();
      Integer         jobId = DurableJobs.launch(new LaunchRequest(KillTestInstance.PROCESS_SIX_STEPS).withValue("stepMillis", 1500));

      Process podA = startPod("pod-A");
      await("pod-A holds the chunk with 2 steps checkpointed", Duration.ofSeconds(60), () -> checkpointedStep(store, jobId, "pod-A") >= 2);
      startPod("pod-B");
      SleepUtils.sleep(2, TimeUnit.SECONDS);

      Instant stoppedAt = Instant.now();
      podA.destroy();
      podA.waitFor(30, TimeUnit.SECONDS);

      QRecord job = awaitJob(store, jobId, Duration.ofSeconds(90), JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.NEEDS_REVIEW);
      assertThat(job.getValueString("status")).isEqualTo(JobStatus.COMPLETED.name());

      List<QRecord> events = events(store, jobId);
      assertThat(events.stream().map(e -> e.getValueString("type")).toList()).contains(JobEventType.RELEASED.name()).doesNotContain(JobEventType.RECLAIMED.name());
      QRecord chunk = store.getChunks(jobId).get(0);
      assertThat(chunk.getValueInteger("attempt")).as("a released lease does not count an attempt").isEqualTo(1);
      assertThat(Duration.between(stoppedAt, chunk.getValueInstant("finishedAt")).toSeconds()).isLessThan(KillTestInstance.LEASE_TTL + 15L);

      Map<String, List<String>> podsByStep = stepLog();
      for(int i = 1; i <= KillTestInstance.STEP_COUNT; i++)
      {
         assertThat(podsByStep.get("step" + i)).as("step" + i + " ran exactly once").hasSize(1);
      }
   }



   /*******************************************************************************
    ** This JVM only launches (workerThreads 0, no runtime started).
    *******************************************************************************/
   private DurableJobStore launcherContext() throws Exception
   {
      QInstance instance = KillTestInstance.define(postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName(), postgres.getUsername(), postgres.getPassword(), "launcher", 0);
      QContext.init(instance, new QSession().withUser(new QUser().withIdReference("kill-test-user").withFullName("Kill Test")));
      return (new DurableJobStore(DurableJobsMetaData.of(instance)));
   }



   /*******************************************************************************
    ** Start a pod JVM and wait for it to say READY.
    *******************************************************************************/
   private Process startPod(String podId) throws Exception
   {
      String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
      ProcessBuilder builder = new ProcessBuilder(java, "-Duser.timezone=UTC", "-cp", System.getProperty("java.class.path"), DurableJobsTestNode.class.getName(),
         postgres.getHost(), String.valueOf(postgres.getFirstMappedPort()), postgres.getDatabaseName(), postgres.getUsername(), postgres.getPassword(), podId);
      builder.redirectErrorStream(true);
      Process process = builder.start();
      pods.add(process);

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      long           deadline = System.currentTimeMillis() + 60_000;
      String         line;
      while((line = reader.readLine()) != null)
      {
         System.out.println("[" + podId + "] " + line);
         if(line.contains("READY " + podId))
         {
            break;
         }
         if(System.currentTimeMillis() > deadline)
         {
            throw (new AssertionError(podId + " did not start"));
         }
      }

      //////////////////////////////////////////////////////////////////////
      // keep draining the pod's output so it never blocks on a full pipe //
      //////////////////////////////////////////////////////////////////////
      Thread drainer = new Thread(() -> reader.lines().forEach(l -> System.out.println("[" + podId + "] " + l)), "drain-" + podId);
      drainer.setDaemon(true);
      drainer.start();
      return (process);
   }



   /*******************************************************************************
    ** The last checkpointed step number on the job's chunk while the given pod
    ** holds it (0 if it does not).
    *******************************************************************************/
   private static int checkpointedStep(DurableJobStore store, Integer jobId, String podId) throws Exception
   {
      QRecord chunk = store.getChunks(jobId).get(0);
      if(!ChunkStatus.RUNNING.name().equals(chunk.getValueString("status")) || !podId.equals(chunk.getValueString("leaseOwner")) || chunk.getValueString("checkpointJson") == null)
      {
         return (0);
      }
      Map<String, Object> checkpoint = JsonUtils.toObject(chunk.getValueString("checkpointJson"), new TypeReference<>() {});
      return (Integer.parseInt(String.valueOf(checkpoint.get("lastStep")).replace("step", "")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> events(DurableJobStore store, Integer jobId) throws Exception
   {
      return (store.query(DurableJobsTables.TABLE_EVENT, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId))));
   }



   /*******************************************************************************
    ** Step name to the pods that logged it, in order.
    *******************************************************************************/
   private static Map<String, List<String>> stepLog() throws Exception
   {
      QSession session = QContext.getQSession();
      try
      {
         QContext.init(QContext.getQInstance(), new QSystemUserSession());
         return (QueryAction.execute(KillTestInstance.TABLE_STEP_LOG, new QQueryFilter()).stream()
            .sorted((a, b) -> a.getValueInteger("id").compareTo(b.getValueInteger("id")))
            .collect(Collectors.groupingBy(r -> r.getValueString("stepName"), Collectors.mapping(r -> r.getValueString("podId"), Collectors.toList()))));
      }
      finally
      {
         QContext.init(QContext.getQInstance(), session);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord awaitJob(DurableJobStore store, Integer jobId, Duration timeout, JobStatus... statuses) throws Exception
   {
      QRecord[] job = new QRecord[1];
      await("job " + jobId + " finishes", timeout, () ->
      {
         job[0] = store.getJob(jobId);
         String status = job[0].getValueString("status");
         for(JobStatus s : statuses)
         {
            if(s.name().equals(status))
            {
               return (true);
            }
         }
         return (false);
      });
      return (job[0]);
   }



   /*******************************************************************************
    ** A condition that may throw.
    *******************************************************************************/
   @FunctionalInterface
   private interface Condition
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      boolean test() throws Exception;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void await(String what, Duration timeout, Condition condition) throws Exception
   {
      long deadline = System.currentTimeMillis() + timeout.toMillis();
      while(System.currentTimeMillis() < deadline)
      {
         if(condition.test())
         {
            return;
         }
         SleepUtils.sleep(100, TimeUnit.MILLISECONDS);
      }
      throw (new AssertionError("Timed out waiting for: " + what));
   }

}
