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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTables;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectDispatcher;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectKeys;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectStatus;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobLease;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryLockMode;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Side effects outside the job's database under kill -9 (spec section 2.13,
 ** item 6): Postgres, separate pod JVMs, and stub targets that record every
 ** call - an email server that does not dedupe, an S3 PUT log with its object
 ** map, an HTTP API that honors Idempotency-Key (served by this JVM), and an
 ** upsert into a second database. A pod is halted (no shutdown hooks) at:
 **
 ** (a) after the external call, before the outbox row is marked SENT;
 ** (b) between the chunk commits and any dispatch;
 ** (c) in the middle of a dispatch batch.
 **
 ** Then another pod finishes. Every run must end with: each customer visited
 ** exactly once (LOCAL_TX), at most one email per Message-ID (and exactly one
 ** per customer, via reconcile - never a blind resend), one S3 object and one
 ** other-database row per customer, one partner resource per Idempotency-Key,
 ** and no effect left unknown (reconciled or, if it can't be, queued for review).
 **
 ** Also: claims use FOR UPDATE SKIP LOCKED on Postgres - a chunk locked by
 ** another claimer is skipped, not waited on or double-claimed.
 *******************************************************************************/
class DurableJobsEffectsFaultTest extends BaseTest
{
   private static final int CUSTOMERS = 10;

   private final List<Process> pods = new ArrayList<>();
   private HttpServer           partner;
   private final Map<String, String> partnerResources = new ConcurrentHashMap<>();
   private final AtomicInteger       partnerRequests  = new AtomicInteger();



   /*******************************************************************************
    ** Fresh tables, ten customers, and the partner API stub.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      TestUtils.primeTestDatabase("prime-durable-jobs.sql");
      partner = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      partner.createContext("/effects", this::partnerApi);
      partner.start();

      launcherContext();
      List<QRecord> customers = new ArrayList<>();
      for(int i = 1; i <= CUSTOMERS; i++)
      {
         customers.add(new QRecord().withValue("code", "cust-" + i).withValue("email", "c" + i + "@example.com").withValue("visits", 0));
      }
      SystemScope.call(() -> new InsertAction().execute(new InsertInput(EffectsKillTestParts.TABLE_CUSTOMER).withRecords(customers)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      pods.forEach(Process::destroyForcibly);
      pods.clear();
      if(partner != null)
      {
         partner.stop(0);
      }
   }



   /*******************************************************************************
    ** (a) Halt right after an email was sent, before its outbox row says SENT.
    ** The next pod finds the lease expired: the outcome is unknown, so it
    ** reconciles (finds the Message-ID at the stub) instead of resending.
    *******************************************************************************/
   @Test
   void killAfterExternalCallBeforeMarkedSent() throws Exception
   {
      DurableJobStore store = launcherContext();
      Integer         jobId = DurableJobs.launch(new LaunchRequest(EffectsKillTestParts.PROCESS).withFilter(new QQueryFilter()));

      Process podA = startPod("pod-A", 1, EffectDispatcher.POINT_AFTER_DELIVER + ":welcomeEmail:3");
      assertThat(podA.waitFor(120, TimeUnit.SECONDS)).as("pod-A halted at the fault point").isTrue();
      assertThat(emailsSent()).as("pod-A sent 3 emails before it died").hasSize(3);
      List<QRecord> inFlight = effects(jobId).stream().filter(e -> EffectStatus.SENDING.name().equals(e.getValueString("status"))).toList();
      assertThat(inFlight).as("pod-A died holding effect leases").isNotEmpty();

      startPod("pod-B", 1, null);
      QRecord job = awaitJob(store, jobId, Duration.ofSeconds(120));
      assertThat(job.getValueString("status")).isEqualTo(JobStatus.COMPLETED.name());
      assertExactlyOnce(jobId);

      ///////////////////////////////////////////////////////////////////
      // the interrupted email was reconciled, not resent              //
      ///////////////////////////////////////////////////////////////////
      String interrupted = emailsSent().get(2).getValueString("messageId");
      QRecord effect = effects(jobId).stream().filter(e -> "welcomeEmail".equals(e.getValueString("effectName"))
         && EffectKeys.messageId(e.getValueString("idempotencyKey"), EffectsKillTestParts.EMAIL_DOMAIN).equals(interrupted)).findFirst().orElseThrow();
      assertThat(effect.getValueInteger("reconcileAttempts")).isGreaterThanOrEqualTo(1);
      assertThat(effect.getValueInteger("attempt")).as("delivered once, never resent").isEqualTo(1);
   }



   /*******************************************************************************
    ** (b) Halt after every chunk committed (with its outbox rows) and before any
    ** dispatch (pod-A runs no dispatcher). The effects survive in the outbox,
    ** and pod-B delivers each exactly once.
    *******************************************************************************/
   @Test
   void killBetweenChunkCommitAndDispatch() throws Exception
   {
      DurableJobStore store = launcherContext();
      Integer         jobId = DurableJobs.launch(new LaunchRequest(EffectsKillTestParts.PROCESS).withFilter(new QQueryFilter()));

      Process podA = startPod("pod-A", 0, null);
      await("every chunk committed", Duration.ofSeconds(120), () -> store.getChunks(jobId).stream().allMatch(c -> ChunkStatus.DONE.name().equals(c.getValueString("status"))));
      podA.destroyForcibly().waitFor(10, TimeUnit.SECONDS);

      assertThat(effects(jobId)).hasSize(CUSTOMERS * 4).allSatisfy(e -> assertThat(e.getValueString("status")).isEqualTo(EffectStatus.PENDING.name()));
      assertThat(emailsSent()).isEmpty();
      assertThat(store.getJob(jobId).getValueString("status")).as("not finished while effects are pending").isEqualTo(JobStatus.RUNNING.name());

      startPod("pod-B", 1, null);
      QRecord job = awaitJob(store, jobId, Duration.ofSeconds(120));
      assertThat(job.getValueString("status")).isEqualTo(JobStatus.COMPLETED.name());
      assertExactlyOnce(jobId);
   }



   /*******************************************************************************
    ** (c) Halt in the middle of a dispatch batch, before the 7th delivery: some
    ** effects of the claimed batch were delivered and marked, one or more were
    ** claimed but never called. The next pod resends the idempotent ones with
    ** the same key and reconciles the emails (NOT_FOUND -> send once).
    *******************************************************************************/
   @Test
   void killMidDispatch() throws Exception
   {
      DurableJobStore store = launcherContext();
      Integer         jobId = DurableJobs.launch(new LaunchRequest(EffectsKillTestParts.PROCESS).withFilter(new QQueryFilter()));

      Process podA = startPod("pod-A", 1, EffectDispatcher.POINT_BEFORE_DELIVER + ":*:7");
      assertThat(podA.waitFor(120, TimeUnit.SECONDS)).as("pod-A halted mid-dispatch").isTrue();
      assertThat(effects(jobId).stream().filter(e -> EffectStatus.SENDING.name().equals(e.getValueString("status")))).as("claimed, unfinished").isNotEmpty();

      startPod("pod-B", 1, null);
      QRecord job = awaitJob(store, jobId, Duration.ofSeconds(120));
      assertThat(job.getValueString("status")).isEqualTo(JobStatus.COMPLETED.name());
      assertExactlyOnce(jobId);
   }



   /*******************************************************************************
    ** FOR UPDATE SKIP LOCKED: while another transaction holds chunk 0 locked,
    ** a claim takes chunk 1 instead of waiting on (or double-claiming) chunk 0.
    *******************************************************************************/
   @Test
   void skipLockedClaimSkipsALockedChunk() throws Exception
   {
      DurableJobStore store = launcherContext();
      Integer         jobId = DurableJobs.launch(new LaunchRequest(EffectsKillTestParts.PROCESS).withFilter(new QQueryFilter()));
      assertThat(store.usesSkipLockedClaims()).isTrue();

      QBackendTransaction holder = QBackendTransaction.openFor(new InsertInput(DurableJobsTables.TABLE_CHUNK));
      try
      {
         List<QRecord> locked = SystemScope.call(() -> new QueryAction().execute(new QueryInput(DurableJobsTables.TABLE_CHUNK).withTransaction(holder)
            .withLockMode(QueryLockMode.FOR_UPDATE_SKIP_LOCKED)
            .withFilter(new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)).withOrderBy(new QFilterOrderBy("seq")).withLimit(1))).getRecords());
         assertThat(locked).singleElement().satisfies(c -> assertThat(c.getValueInteger("seq")).isZero());

         Optional<JobLease> lease = store.claimNextChunkSkipLocked(jobId, "claimer", 30);
         assertThat(lease).isPresent();
         assertThat(lease.get().seq()).as("skipped the locked chunk 0").isEqualTo(1);
      }
      finally
      {
         holder.rollback();
         holder.close();
      }
      Optional<JobLease> next = store.claimNextChunkSkipLocked(jobId, "claimer", 30);
      assertThat(next).isPresent();
      assertThat(next.get().seq()).as("chunk 0 is claimable once unlocked").isZero();
   }



   /*******************************************************************************
    ** The exactly-once checks, from the targets' point of view.
    *******************************************************************************/
   private void assertExactlyOnce(Integer jobId) throws Exception
   {
      ////////////////////////////////////////////
      // LOCAL_TX: every customer visited once //
      ////////////////////////////////////////////
      assertThat(query(EffectsKillTestParts.TABLE_CUSTOMER)).hasSize(CUSTOMERS).allSatisfy(c -> assertThat(c.getValueInteger("visits")).isEqualTo(1));

      ////////////////////////////////////////////////////////////////////
      // email: at most one per Message-ID; exactly one per customer    //
      ////////////////////////////////////////////////////////////////////
      Map<String, Long> perMessage = emailsSent().stream().collect(Collectors.groupingBy(e -> e.getValueString("messageId"), Collectors.counting()));
      assertThat(perMessage.values()).as("at most one email per Message-ID").allMatch(count -> count == 1);
      assertThat(perMessage).hasSize(CUSTOMERS);

      //////////////////////////////////////////////////////////////
      // S3: one object per key (re-PUTs overwrite, never add)    //
      //////////////////////////////////////////////////////////////
      List<QRecord> objects = query(EffectsKillTestParts.TABLE_S3_OBJ);
      assertThat(objects).hasSize(CUSTOMERS);
      assertThat(query(EffectsKillTestParts.TABLE_S3_PUT).stream().map(p -> p.getValueString("objectKey")).distinct()).hasSize(CUSTOMERS);

      ////////////////////////////////////////////////////////////////
      // HTTP: one partner resource per Idempotency-Key             //
      ////////////////////////////////////////////////////////////////
      assertThat(partnerResources).hasSize(CUSTOMERS);
      assertThat(partnerRequests.get()).isGreaterThanOrEqualTo(CUSTOMERS);

      //////////////////////////////////////////////////////////////////
      // other database: one upserted row per natural key, no dupes   //
      //////////////////////////////////////////////////////////////////
      List<QRecord> flags = query(EffectsKillTestParts.TABLE_OTHER_DB);
      assertThat(flags).hasSize(CUSTOMERS);
      assertThat(flags.stream().map(f -> f.getValueString("code")).distinct()).hasSize(CUSTOMERS);

      //////////////////////////////////////////////////////////////////
      // the outbox: every effect resolved, nothing left unknown      //
      //////////////////////////////////////////////////////////////////
      assertThat(effects(jobId)).hasSize(CUSTOMERS * 4).allSatisfy(e ->
      {
         assertThat(e.getValueString("status")).isEqualTo(EffectStatus.SENT.name());
         assertThat(e.getValueBoolean("needsReview")).isFalse();
      });
   }



   /*******************************************************************************
    ** The partner API: an idempotent receiver. The first POST with a key creates
    ** a resource; a repeat returns the same one.
    *******************************************************************************/
   private void partnerApi(HttpExchange exchange) throws java.io.IOException
   {
      partnerRequests.incrementAndGet();
      String   key      = exchange.getRequestHeaders().getFirst("Idempotency-Key");
      String   resource = key == null ? null : partnerResources.computeIfAbsent(key, k -> "res-" + (partnerResources.size() + 1));
      int      status   = key == null ? 400 : 200;
      byte[]   body     = (resource == null ? "missing Idempotency-Key" : resource).getBytes(StandardCharsets.UTF_8);
      exchange.getRequestBody().readAllBytes();
      exchange.sendResponseHeaders(status, body.length);
      try(OutputStream out = exchange.getResponseBody())
      {
         out.write(body);
      }
   }



   /*******************************************************************************
    ** This JVM launches and inspects (no runtime).
    *******************************************************************************/
   private DurableJobStore launcherContext() throws Exception
   {
      QInstance instance = KillTestInstance.define(postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName(), postgres.getUsername(), postgres.getPassword(), "launcher", 0);
      EffectsKillTestParts.add(instance, KillTestInstance.BACKEND, postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName(), postgres.getUsername(), postgres.getPassword());
      QContext.init(instance, new QSession().withUser(new QUser().withIdReference("effects-test-user").withFullName("Effects Test")));
      return (new DurableJobStore(DurableJobsMetaData.of(instance)));
   }



   /*******************************************************************************
    ** Start a pod JVM (dispatcher threads, optional fault) and wait for READY.
    *******************************************************************************/
   private Process startPod(String podId, int dispatchers, String fault) throws Exception
   {
      String       java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
      List<String> cmd  = new ArrayList<>(List.of(java, "-Duser.timezone=UTC",
         "-D" + EffectsKillTestParts.PROPERTY_ENABLED + "=true",
         "-D" + EffectsKillTestParts.PROPERTY_DISPATCHERS + "=" + dispatchers,
         "-D" + EffectsKillTestParts.PROPERTY_HTTP_STUB + "=http://127.0.0.1:" + partner.getAddress().getPort()));
      if(fault != null)
      {
         cmd.add("-D" + EffectsKillTestParts.PROPERTY_FAULT + "=" + fault);
      }
      cmd.addAll(List.of("-cp", System.getProperty("java.class.path"), DurableJobsTestNode.class.getName(),
         postgres.getHost(), String.valueOf(postgres.getFirstMappedPort()), postgres.getDatabaseName(), postgres.getUsername(), postgres.getPassword(), podId));

      ProcessBuilder builder = new ProcessBuilder(cmd);
      builder.redirectErrorStream(true);
      Process process = builder.start();
      pods.add(process);

      BufferedReader reader   = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
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
      Thread drainer = new Thread(() ->
      {
         try
         {
            reader.lines().forEach(l -> System.out.println("[" + podId + "] " + l));
         }
         catch(Exception e)
         {
            ///////////////////////////////////////////
            // the pod was stopped; its output ended //
            ///////////////////////////////////////////
         }
      }, "drain-" + podId);
      drainer.setDaemon(true);
      drainer.start();
      return (process);
   }



   /*******************************************************************************
    ** Wait for the job to finish (any final status, or NEEDS_REVIEW).
    *******************************************************************************/
   private QRecord awaitJob(DurableJobStore store, Integer jobId, Duration timeout) throws Exception
   {
      QRecord[] job = new QRecord[1];
      await("job " + jobId + " finished", timeout, () ->
      {
         job[0] = store.getJob(jobId);
         JobStatus status = JobStatus.valueOf(job[0].getValueString("status"));
         return (status.isTerminal() || status == JobStatus.NEEDS_REVIEW);
      });
      return (job[0]);
   }



   /*******************************************************************************
    ** Poll until true or fail.
    *******************************************************************************/
   private static void await(String what, Duration timeout, java.util.concurrent.Callable<Boolean> condition) throws Exception
   {
      long deadline = System.nanoTime() + timeout.toNanos();
      while(System.nanoTime() < deadline)
      {
         if(condition.call())
         {
            return;
         }
         SleepUtils.sleep(250, TimeUnit.MILLISECONDS);
      }
      throw (new AssertionError("Timed out waiting for " + what));
   }



   /*******************************************************************************
    ** The job's outbox rows.
    *******************************************************************************/
   private List<QRecord> effects(Integer jobId) throws Exception
   {
      return (SystemScope.call(() -> new QueryAction().execute(new QueryInput(DurableJobsTables.TABLE_EFFECT)
         .withFilter(new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)).withOrderBy(new QFilterOrderBy("id")))).getRecords()));
   }



   /*******************************************************************************
    ** The email stub's log, in send order.
    *******************************************************************************/
   private List<QRecord> emailsSent() throws Exception
   {
      return (SystemScope.call(() -> new QueryAction().execute(new QueryInput(EffectsKillTestParts.TABLE_EMAIL)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")))).getRecords()));
   }



   /*******************************************************************************
    ** All rows of a table.
    *******************************************************************************/
   private List<QRecord> query(String table) throws Exception
   {
      return (SystemScope.call(() -> new QueryAction().execute(new QueryInput(table)).getRecords()));
   }
}
