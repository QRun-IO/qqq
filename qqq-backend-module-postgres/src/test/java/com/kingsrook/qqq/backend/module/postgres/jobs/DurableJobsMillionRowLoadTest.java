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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRef;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRefs;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
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
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/*******************************************************************************
 ** The spec section 6.2 load test: 1,000,000 rows at chunk size 1,000 across two
 ** pod JVMs (each -Xmx512m). Reports throughput; checks that every row loads,
 ** that pod memory stays flat (bounded buffers: the peak used heap stays well
 ** under the cap), and that no files are created (each pod's java.io.tmpdir is
 ** a fresh directory that must still be empty at the end).
 **
 ** Opt-in (minutes long): run with -Dqqq.jobs.loadTest=true (CI nightly).
 *******************************************************************************/
class DurableJobsMillionRowLoadTest extends BaseTest
{
   private static final int ROWS       = 1_000_000;
   private static final int CHUNK_SIZE = 1_000;
   private static final int HEAP_MB    = 512;

   private static final Pattern HEAP_LINE = Pattern.compile("HEAP (\\S+) usedMb=(\\d+)");

   private final List<Process>        pods        = new ArrayList<>();
   private final Map<String, Integer> peakHeapMb  = new ConcurrentHashMap<>();
   private final List<Path>           directories = new ArrayList<>();
   private Path                       storageDir;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void prime() throws Exception
   {
      assumeTrue(Boolean.getBoolean("qqq.jobs.loadTest"), "the 1M-row load test runs only with -Dqqq.jobs.loadTest=true");
      TestUtils.primeTestDatabase("prime-durable-jobs.sql");
      storageDir = Files.createTempDirectory("qqq-jobs-load-storage-");
      directories.add(storageDir);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanup() throws Exception
   {
      pods.forEach(Process::destroyForcibly);
      pods.clear();
      for(Path directory : directories)
      {
         try(var walk = Files.walk(directory))
         {
            walk.sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
         }
      }
   }



   /*******************************************************************************
    ** 1M rows, two pods, chunk size 1,000.
    *******************************************************************************/
   @Test
   void testMillionRows() throws Exception
   {
      QInstance instance = KillTestInstance.define(postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName(), postgres.getUsername(), postgres.getPassword(), "launcher", 0, storageDir.toString());
      DurableJobsMetaData.of(instance).withDefaultChunkSize(CHUNK_SIZE);
      QContext.init(instance, new QSession().withUser(new QUser().withIdReference("load-test-user").withFullName("Load Test User")));
      DurableJobStore store = new DurableJobStore(DurableJobsMetaData.of(instance));

      ////////////////////////////////////////////////////////////////////
      // the 1M-row CSV is generated as a stream - never held in memory //
      ////////////////////////////////////////////////////////////////////
      Instant   uploadStart = Instant.now();
      UploadRef ref         = UploadRefs.store("million.csv", "text/csv", new GeneratedCsv(ROWS));
      Duration  uploadTime  = Duration.between(uploadStart, Instant.now());

      String profile = """
         {"version":"v1","layout":"FLAT","hasHeaderRow":true,"fieldList":[
           {"fieldName":"code","columnIndex":0},{"fieldName":"name","columnIndex":1},{"fieldName":"amount","columnIndex":2}]}""";
      Instant planStart = Instant.now();
      Integer jobId = DurableJobs.launch(new LaunchRequest(BulkLoadKillTestParts.TABLE_LOAD + ".bulkInsert")
         .withUploadRefs(List.of(ref.ref())).withProfileJson(profile).withValue("tableName", BulkLoadKillTestParts.TABLE_LOAD).withLabel("Million rows"));
      Duration planTime = Duration.between(planStart, Instant.now());
      assertThat(store.getChunks(jobId)).hasSize(ROWS / CHUNK_SIZE);

      Instant runStart = Instant.now();
      startPod("pod-A");
      startPod("pod-B");

      QRecord job = null;
      long    deadline = System.currentTimeMillis() + Duration.ofMinutes(30).toMillis();
      while(System.currentTimeMillis() < deadline)
      {
         job = store.getJob(jobId);
         JobStatus status = JobStatus.valueOf(job.getValueString("status"));
         if(status.isTerminal() || status == JobStatus.NEEDS_REVIEW)
         {
            break;
         }
         SleepUtils.sleep(2, TimeUnit.SECONDS);
      }
      Duration runTime = Duration.between(runStart, Instant.now());

      assertThat(job.getValueString("status")).as(job.getValueString("errorMessage")).isEqualTo(JobStatus.COMPLETED.name());
      int loaded = SystemScope.call(() -> new CountAction().execute(new CountInput(BulkLoadKillTestParts.TABLE_LOAD).withFilter(new QQueryFilter())).getCount());
      assertThat(loaded).isEqualTo(ROWS);

      long rowsPerSecond = ROWS * 1000L / Math.max(1, runTime.toMillis());
      System.out.println("LOAD-TEST RESULT: rows=" + ROWS + " chunkSize=" + CHUNK_SIZE + " pods=2"
         + " upload=" + uploadTime.toMillis() + "ms plan=" + planTime.toMillis() + "ms run=" + runTime.toMillis() + "ms"
         + " throughput=" + rowsPerSecond + " rows/s peakHeapMb=" + peakHeapMb + " (cap " + HEAP_MB + ")");

      //////////////////////////////////////////////////////////////////////////
      // memory stays flat: peak used heap well under the cap (bounded work) //
      //////////////////////////////////////////////////////////////////////////
      assertThat(peakHeapMb).containsKeys("pod-A", "pod-B");
      assertThat(peakHeapMb.values()).allMatch(mb -> mb < HEAP_MB * 0.8);

      ////////////////////////////////////////////////////////
      // no files: the pods' temp directories are still empty //
      ////////////////////////////////////////////////////////
      for(Path directory : directories.subList(1, directories.size()))
      {
         try(var list = Files.list(directory))
         {
            assertThat(list.toList()).as("files created in " + directory).isEmpty();
         }
      }
   }



   /*******************************************************************************
    ** Start a pod JVM with a capped heap and its own empty temp directory.
    *******************************************************************************/
   private void startPod(String podId) throws Exception
   {
      Path tmp = Files.createTempDirectory("qqq-jobs-load-tmp-" + podId + "-");
      directories.add(tmp);
      String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
      ProcessBuilder builder = new ProcessBuilder(java, "-Xmx" + HEAP_MB + "m", "-Duser.timezone=UTC", "-Djava.io.tmpdir=" + tmp, "-Dlog4j2.level=WARN",
         "-cp", System.getProperty("java.class.path"), DurableJobsTestNode.class.getName(),
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

      Thread drainer = new Thread(() ->
      {
         try
         {
            reader.lines().forEach(l ->
            {
               Matcher matcher = HEAP_LINE.matcher(l);
               if(matcher.find())
               {
                  peakHeapMb.merge(matcher.group(1), Integer.parseInt(matcher.group(2)), Math::max);
               }
               else if(l.contains("ERROR"))
               {
                  System.out.println("[" + podId + "] " + l);
               }
            });
         }
         catch(Exception e)
         {
            //////////////////////////////
            // the pod's output ended //
            //////////////////////////////
         }
      }, "drain-" + podId);
      drainer.setDaemon(true);
      drainer.start();
   }



   /*******************************************************************************
    ** A CSV of N rows produced on the fly (header + "c<i>,Name <i>,<i>").
    *******************************************************************************/
   static class GeneratedCsv extends InputStream
   {
      private final int rows;
      private int       next = 0;
      private byte[]    line = "code,name,amount\n".getBytes(StandardCharsets.UTF_8);
      private int       pos  = 0;



      /*******************************************************************************
       **
       *******************************************************************************/
      GeneratedCsv(int rows)
      {
         this.rows = rows;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public int read()
      {
         if(pos >= line.length)
         {
            if(next >= rows)
            {
               return (-1);
            }
            next++;
            line = ("c" + next + ",Name " + next + "," + next + "\n").getBytes(StandardCharsets.UTF_8);
            pos = 0;
         }
         return (line[pos++] & 0xff);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public int read(byte[] buffer, int offset, int length)
      {
         int count = 0;
         while(count < length)
         {
            int b = read();
            if(b == -1)
            {
               return (count == 0 ? -1 : count);
            }
            buffer[offset + count++] = (byte) b;
         }
         return (count);
      }
   }

}
