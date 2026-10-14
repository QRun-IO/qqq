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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTables;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRef;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRefs;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Kill-the-pod, the full spec section 6.1 test: Postgres plus TWO pod JVMs
 ** (workerThreads=1, 10s lease TTL, 3s reaper). A 10,000-row CSV with 20 bad
 ** rows, uploaded, launched as {table}.bulkInsert with chunk size 500 into a
 ** table with a unique key. Pod A is killed (kill -9) mid-chunk.
 **
 ** Asserts: B reclaims the orphaned chunk within TTL + reaper and writes
 ** RECLAIMED; the job ends COMPLETED_WITH_ERRORS; EXACTLY 9,980 rows (no
 ** duplicates, none missing - SAME_TRANSACTION); the killed chunk ends at
 ** attempt 2; exactly 20 row errors at the right row numbers; retry failed
 ** rows with a corrected profile loads those 20; 9,980 insert audits carry the
 ** launching user, the client channel and the job; a job.progress event was
 ** published on record:jobs_job:{id}; and the owner was notified exactly once.
 *******************************************************************************/
class DurableJobsBulkLoadKillPodTest extends BaseTest
{
   private static final int ROWS       = 10_000;
   private static final int CHUNK_SIZE = 500;

   private final List<Process> pods = new ArrayList<>();
   private Path storageDir;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void prime() throws Exception
   {
      TestUtils.primeTestDatabase("prime-durable-jobs.sql");
      storageDir = Files.createTempDirectory("qqq-jobs-killtest-");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanup() throws Exception
   {
      pods.forEach(Process::destroyForcibly);
      pods.clear();
      if(storageDir != null)
      {
         try(var walk = Files.walk(storageDir))
         {
            walk.sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
         }
      }
   }



   /*******************************************************************************
    ** The spec section 6.1 acceptance test.
    *******************************************************************************/
   @Test
   void testKillPodMidChunkBulkLoad() throws Exception
   {
      QInstance instance = KillTestInstance.define(postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName(), postgres.getUsername(), postgres.getPassword(), "launcher", 0, storageDir.toString());
      QSession session = new QSession().withUser(new QUser().withIdReference("kill-test-user").withFullName("Kill Test User"));
      session.setClientChannel("next-ui/killtest");
      QContext.init(instance, session);
      DurableJobStore store = new DurableJobStore(DurableJobsMetaData.of(instance));

      ///////////////////////////////////////////////////////////////////////
      // 10,000 data rows (file rows 2..10001), 20 bad amounts: every 500th //
      ///////////////////////////////////////////////////////////////////////
      StringBuilder csv     = new StringBuilder("code,name,amount\n");
      Set<Integer>  badRows = new HashSet<>();
      for(int i = 1; i <= ROWS; i++)
      {
         boolean bad = i % 500 == 250;
         if(bad)
         {
            badRows.add(i + 1);
         }
         csv.append("k").append(i).append(",Name ").append(i).append(",").append(bad ? "notANumber" : String.valueOf(i)).append("\n");
      }
      UploadRef ref = UploadRefs.store("load.csv", "text/csv", new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8)));

      String profile = """
         {"version":"v1","layout":"FLAT","hasHeaderRow":true,"fieldList":[
           {"fieldName":"code","columnIndex":0},{"fieldName":"name","columnIndex":1},{"fieldName":"amount","columnIndex":2}]}""";
      Integer jobId = DurableJobs.launch(new LaunchRequest(BulkLoadKillTestParts.TABLE_TARGET + ".bulkInsert")
         .withUploadRefs(List.of(ref.ref())).withProfileJson(profile).withValue("tableName", BulkLoadKillTestParts.TABLE_TARGET).withLabel("Kill test load"));

      QRecord launched = store.getJob(jobId);
      assertThat(launched.getValueInteger("rowsTotal")).isEqualTo(ROWS);
      assertThat(launched.getValueString("idempotency")).isEqualTo(JobIdempotency.SAME_TRANSACTION.name());
      assertThat(store.getChunks(jobId)).hasSize(ROWS / CHUNK_SIZE);

      //////////////////////////////////////////////////////////////////////////////
      // A starts; once 3 chunks are done and A holds a running chunk, start B,   //
      // then kill -9 A while it holds a chunk mid-way.                            //
      //////////////////////////////////////////////////////////////////////////////
      Process podA = startPod("pod-A");
      await("3 chunks done and pod-A mid-chunk", Duration.ofSeconds(120), () -> doneChunks(store, jobId) >= 3 && runningChunkOf(store, jobId, "pod-A") != null);
      startPod("pod-B");
      QRecord killedChunk = null;
      long    deadline    = System.currentTimeMillis() + 60_000;
      while(killedChunk == null && System.currentTimeMillis() < deadline)
      {
         QRecord running = runningChunkOf(store, jobId, "pod-A");
         if(running != null && running.getValue("startedAt") != null)
         {
            SleepUtils.sleep(400, TimeUnit.MILLISECONDS); // let it get into the chunk's inserts
            QRecord still = store.getChunk(running.getValueInteger("id"));
            if(ChunkStatus.RUNNING.name().equals(still.getValueString("status")) && "pod-A".equals(still.getValueString("leaseOwner")))
            {
               podA.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
               killedChunk = still;
            }
         }
         else
         {
            SleepUtils.sleep(50, TimeUnit.MILLISECONDS);
         }
      }
      assertThat(killedChunk).as("pod-A was killed while holding a chunk").isNotNull();
      long killedAt = System.currentTimeMillis();

      QRecord job = awaitJob(store, jobId, Duration.ofSeconds(240));
      assertThat(job.getValueString("status")).as(job.getValueString("errorMessage")).isEqualTo(JobStatus.COMPLETED_WITH_ERRORS.name());

      ////////////////////////////////////////////////////////////
      // reclaim: by B, within TTL + reaper interval (+1s slack) //
      ////////////////////////////////////////////////////////////
      Integer killedChunkId = killedChunk.getValueInteger("id");
      QRecord reclaimed = store.query(DurableJobsTables.TABLE_EVENT, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)))
         .stream().filter(e -> JobEventType.RECLAIMED.name().equals(e.getValueString("type")) && killedChunkId.equals(e.getValueInteger("chunkId"))).findFirst().orElseThrow();
      assertThat(reclaimed.getValueString("pod")).isEqualTo("pod-B");
      assertThat(reclaimed.getValueInstant("createDate").toEpochMilli() - killedAt).isLessThanOrEqualTo((KillTestInstance.LEASE_TTL + KillTestInstance.REAPER_SECS + 1) * 1000L);
      assertThat(store.getChunk(killedChunkId).getValueInteger("attempt")).as("the killed chunk ran twice").isEqualTo(2);

      //////////////////////////////////////////////////////////
      // exactly once: 9,980 rows, no duplicates, none missing //
      //////////////////////////////////////////////////////////
      assertThat(count(BulkLoadKillTestParts.TABLE_TARGET, new QQueryFilter())).isEqualTo(ROWS - badRows.size());
      assertThat(job.getValueInteger("rowsOk")).isEqualTo(ROWS - badRows.size());
      assertThat(job.getValueInteger("rowsFailed")).isEqualTo(badRows.size());

      ///////////////////////////////////////////////////////////////
      // exactly 20 row errors, at exactly the bad file row numbers //
      ///////////////////////////////////////////////////////////////
      List<QRecord> rowErrors = store.query(DurableJobsTables.TABLE_ROW_ERROR, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)).withOrderBy(new QFilterOrderBy("rowNumber")));
      assertThat(rowErrors).hasSize(20);
      assertThat(rowErrors.stream().map(e -> e.getValueInteger("rowNumber")).toList()).containsExactlyInAnyOrderElementsOf(badRows);

      ////////////////////////////////////////////////////////////////////////////
      // audits: one insert audit per row, as the launching user and channel, //
      // with the job in the audit message                                     //
      ////////////////////////////////////////////////////////////////////////////
      Integer userAuditId = SystemScope.call(() -> QueryAction.execute("auditUser", new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Kill Test User"))).get(0).getValueInteger("id"));
      Integer targetAuditTableId = SystemScope.call(() -> QueryAction.execute("auditTable", new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, BulkLoadKillTestParts.TABLE_TARGET))).get(0).getValueInteger("id"));
      assertThat(count("audit", new QQueryFilter(new QFilterCriteria("auditTableId", QCriteriaOperator.EQUALS, targetAuditTableId)))).isEqualTo(ROWS - badRows.size());
      assertThat(count("audit", new QQueryFilter()
         .withCriteria(new QFilterCriteria("auditTableId", QCriteriaOperator.EQUALS, targetAuditTableId))
         .withCriteria(new QFilterCriteria("auditUserId", QCriteriaOperator.EQUALS, userAuditId))
         .withCriteria(new QFilterCriteria("clientChannel", QCriteriaOperator.EQUALS, "next-ui/killtest"))
         .withCriteria(new QFilterCriteria("message", QCriteriaOperator.CONTAINS, "job:" + jobId + "/chunk:")))).isEqualTo(ROWS - badRows.size());

      ////////////////////////////////////////////////////////////////////
      // signals: job.progress on the job's record channel; one notify //
      ////////////////////////////////////////////////////////////////////
      assertThat(count(BulkLoadKillTestParts.TABLE_SIGNAL, new QQueryFilter()
         .withCriteria(new QFilterCriteria("kind", QCriteriaOperator.EQUALS, "job.progress"))
         .withCriteria(new QFilterCriteria("channel", QCriteriaOperator.EQUALS, "record:jobs_job:" + jobId)))).isGreaterThan(0);
      assertThat(count(BulkLoadKillTestParts.TABLE_SIGNAL, new QQueryFilter()
         .withCriteria(new QFilterCriteria("kind", QCriteriaOperator.EQUALS, "notify"))
         .withCriteria(new QFilterCriteria("channel", QCriteriaOperator.EQUALS, String.valueOf(jobId))))).isEqualTo(1);

      ///////////////////////////////////////////////////////////////////////////
      // retry failed rows with a corrected profile (amount not mapped): the   //
      // 20 load, the table has all 10,000, and the parent's errors resolve.   //
      ///////////////////////////////////////////////////////////////////////////
      String corrected = """
         {"version":"v1","layout":"FLAT","hasHeaderRow":true,"fieldList":[
           {"fieldName":"code","columnIndex":0},{"fieldName":"name","columnIndex":1}]}""";
      Integer retryId = DurableJobs.retryFailedRows(jobId, corrected, null);
      assertThat(store.getJob(retryId).getValueInteger("rowsTotal")).isEqualTo(20);
      QRecord retried = awaitJob(store, retryId, Duration.ofSeconds(120));
      assertThat(retried.getValueString("status")).as(retried.getValueString("errorMessage")).isEqualTo(JobStatus.COMPLETED.name());
      assertThat(count(BulkLoadKillTestParts.TABLE_TARGET, new QQueryFilter())).isEqualTo(ROWS);
      assertThat(store.query(DurableJobsTables.TABLE_ROW_ERROR, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId))))
         .allMatch(e -> Boolean.TRUE.equals(e.getValueBoolean("resolved")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int count(String tableName, QQueryFilter filter) throws Exception
   {
      return (SystemScope.call(() -> new CountAction().execute(new CountInput(tableName).withFilter(filter)).getCount()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static long doneChunks(DurableJobStore store, Integer jobId) throws Exception
   {
      return (store.getChunks(jobId).stream().filter(c -> ChunkStatus.DONE.name().equals(c.getValueString("status"))).count());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord runningChunkOf(DurableJobStore store, Integer jobId, String podId) throws Exception
   {
      return (store.getChunks(jobId).stream().filter(c -> ChunkStatus.RUNNING.name().equals(c.getValueString("status")) && podId.equals(c.getValueString("leaseOwner"))).findFirst().orElse(null));
   }



   /*******************************************************************************
    ** Start a pod JVM (in UTC, as servers run) and wait for READY.
    *******************************************************************************/
   private Process startPod(String podId) throws Exception
   {
      String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
      ProcessBuilder builder = new ProcessBuilder(java, "-Duser.timezone=UTC", "-cp", System.getProperty("java.class.path"), DurableJobsTestNode.class.getName(),
         postgres.getHost(), String.valueOf(postgres.getFirstMappedPort()), postgres.getDatabaseName(), postgres.getUsername(), postgres.getPassword(), podId, storageDir.toString());
      builder.redirectErrorStream(true);
      Process process = builder.start();
      pods.add(process);

      BufferedReader reader   = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      long           deadline = System.currentTimeMillis() + 60_000;
      String         line;
      while((line = reader.readLine()) != null)
      {
         if(line.contains("READY " + podId))
         {
            break;
         }
         if(System.currentTimeMillis() > deadline)
         {
            throw (new AssertionError(podId + " did not start"));
         }
      }
      Thread drainer = new Thread(() -> reader.lines().forEach(l ->
      {
         if(l.contains("WARN") || l.contains("ERROR") || l.contains("Reclaimed") || l.contains("Durable job status"))
         {
            System.out.println("[" + podId + "] " + l);
         }
      }), "drain-" + podId);
      drainer.setDaemon(true);
      drainer.start();
      return (process);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord awaitJob(DurableJobStore store, Integer jobId, Duration timeout) throws Exception
   {
      QRecord[] job = new QRecord[1];
      await("job " + jobId + " finishes", timeout, () ->
      {
         job[0] = store.getJob(jobId);
         JobStatus status = JobStatus.valueOf(job[0].getValueString("status"));
         return (status.isTerminal() || status == JobStatus.NEEDS_REVIEW);
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
