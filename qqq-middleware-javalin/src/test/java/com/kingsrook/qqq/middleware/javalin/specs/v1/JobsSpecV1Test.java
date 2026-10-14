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


package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsSecurity;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.progress.JobLogLevel;
import com.kingsrook.qqq.backend.core.jobs.progress.JobLogWriter;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobClaimer;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** HTTP acceptance of every jobs operation, including owner isolation.
 *******************************************************************************/
class JobsSpecV1Test extends SpecTestBase
{
   private CountDownLatch gate;



   /*******************************************************************************
    ** Main route.
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (JobsSpecV1.all().getFirst());
   }



   /*******************************************************************************
    ** The remaining jobs routes.
    *******************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (new ArrayList<>(JobsSpecV1.all().subList(1, 6)));
   }



   /*******************************************************************************
    ** API version.
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    ** Jobs on the memory backend, no chunk workers.
    *******************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = TestUtils.defineInstance();
      new DurableJobsMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      instance.withSupplementalMetaData(new DurableJobsMetaData().withWorkerThreads(0).withEffectDispatcherThreads(0).withEligibleProcessName("greet"));
      instance.getAuthentication().withCustomizer(new QCodeReference(Permissions.class));
      return (instance);
   }



   /*******************************************************************************
    ** Grant only explicit fixture permissions.
    *******************************************************************************/
   public static class Permissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       ** Customize each stable mock session.
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         if(!"denied".equals(session.getUuid()))
         {
            session.withPermission("jobs_job.read");
         }
         if("admin".equals(session.getUuid()))
         {
            session.withPermission("jobs_admin");
         }
      }
   }



   /*******************************************************************************
    ** Stop the tracker and release any foreground worker.
    *******************************************************************************/
   @AfterEach
   void stop()
   {
      if(gate != null)
      {
         gate.countDown();
      }
      DurableJobsRuntime.stop(serverQInstance, Duration.ofSeconds(2));
   }



   /*******************************************************************************
    ** Own jobs are visible; other users see 404; admin can list all.
    *******************************************************************************/
   @Test
   void readsAndPermissions() throws Exception
   {
      Integer id = launch("owner");
      assertThat(get("/jobs/" + id, "owner").getStatus()).isEqualTo(200);
      assertThat(get("/jobs/" + id, "other").getStatus()).isEqualTo(404);
      assertThat(get("/jobs/" + id + "/progress", "other").getStatus()).isEqualTo(404);
      assertThat(post("/jobs/" + id + "/cancel", "other", "{}").getStatus()).isEqualTo(404);
      assertThat(post("/jobs/" + id + "/retry", "other", "{}").getStatus()).isEqualTo(404);
      assertThat(get("/jobs?scope=all", "owner").getStatus()).isEqualTo(403);
      assertThat(get("/jobs", "denied").getStatus()).isEqualTo(403);
      assertThat(json(get("/jobs?scope=all", "admin")).getJSONArray("jobs")).hasSize(1);
      assertThat(json(get("/jobs", "other")).getJSONArray("jobs")).isEmpty();
      assertThat(get("/jobs?limit=-1", "owner").getStatus()).isEqualTo(400);
      assertThat(get("/jobs?state=unknown", "owner").getStatus()).isEqualTo(400);
      assertThat(get("/jobs/" + id, "admin").getStatus()).isEqualTo(200);
   }



   /*******************************************************************************
    ** Polling exposes redacted lines, a cursor, and terminal state.
    *******************************************************************************/
   @Test
   void pollingAndActions() throws Exception
   {
      Integer id = launch("owner");
      DurableJobStore store = store();
      JobLogWriter writer = new JobLogWriter(store, id, null, null);
      writer.append(JobLogLevel.INFO, "load", "token=hidden");
      writer.flush(null, true);
      JSONObject poll = json(get("/jobs/" + id + "/progress", "owner"));
      long cursor = poll.getLong("nextAfterLogId");
      assertThat(poll.getJSONArray("log").getJSONObject(0).getString("message")).doesNotContain("hidden");
      assertThat(poll.getBoolean("terminal")).isFalse();
      assertThat(json(get("/jobs/" + id + "/progress?afterLogId=" + (cursor + 20), "owner")).getJSONArray("log")).isEmpty();
      assertThat(post("/jobs/" + id + "/retry", "owner", "{}").getStatus()).isEqualTo(409);
      assertThat(post("/jobs/" + id + "/cancel", "owner", "{}").getStatus()).isEqualTo(200);
      assertThat(post("/jobs/" + id + "/cancel", "owner", "{}").getStatus()).isEqualTo(409);
      assertThat(json(get("/jobs/" + id + "/progress", "owner")).getBoolean("terminal")).isTrue();
      JSONObject retry = json(post("/jobs/" + id + "/retry", "owner", "{}"));
      assertThat(retry.getInt("jobId")).isNotEqualTo(id);
      assertThat(json(get("/jobs?state=recent", "owner")).getJSONArray("jobs")).hasSize(1);
   }



   /*******************************************************************************
    ** Failed chunks retry in place; active jobs reject retry.
    *******************************************************************************/
   @Test
   void retryFailedChunks() throws Exception
   {
      Integer id = launch("owner");
      store().setChunkStatuses(id, List.of(ChunkStatus.PENDING), ChunkStatus.FAILED, null);
      store().setJobStatus(id, List.of(JobStatus.QUEUED), JobStatus.FAILED, null);
      assertThat(json(post("/jobs/" + id + "/retry", "owner", "{}")).getInt("jobId")).isEqualTo(id);
   }



   /*******************************************************************************
    ** Track checks process ownership and mirrors completion without chunk claims.
    *******************************************************************************/
   @Test
   void inlineTracking() throws Exception
   {
      session("owner");
      String processUUID = UUID.randomUUID().toString();
      gate = new CountDownLatch(1);
      String uuid = new AsyncJobManager().withProcessUUID(processUUID).startJob("greet", callback ->
      {
         callback.getJobProgress().total(2).done(1).info("foreground started");
         gate.await(10, TimeUnit.SECONDS);
         return ("done");
      });
      String body = JsonUtils.toJson(Map.of("processName", "greet", "processUUID", processUUID, "jobUUID", uuid));
      assertThat(post("/jobs/track", "other", body).getStatus()).isIn(403, 404);
      JSONObject result = json(post("/jobs/track", "owner", body));
      Integer id = result.getInt("jobId");
      assertThat(store().getJob(id).getValueString("sourceType")).isEqualTo("INLINE");
      assertThat(store().getChunks(id)).isEmpty();
      assertThat(new JobClaimer(store(), "test").claimNext()).isEmpty();
      gate.countDown();
      long deadline = System.currentTimeMillis() + 5000;
      while(System.currentTimeMillis() < deadline && !"COMPLETED".equals(store().getJob(id).getValueString("status")))
      {
         Thread.sleep(20);
      }
      assertThat(store().getJob(id).getValueString("status")).isEqualTo("COMPLETED");
      assertThat(store().getJob(id).getValueInteger("percentDone")).isEqualTo(100);
      Integer rerun = json(post("/jobs/" + id + "/retry", "owner", "{}")).getInt("jobId");
      assertThat(rerun).isNotEqualTo(id);
      assertThat(store().getJob(rerun).getValueString("sourceType")).isEqualTo("INLINE");
      assertThat(store().getJob(rerun).getValueInteger("parentJobId")).isEqualTo(id);
   }



   /*******************************************************************************
    ** Establish the same stable identity used by HTTP authentication.
    *******************************************************************************/
   private void session(String name) throws Exception
   {
      QSession session = new MockAuthenticationModule().createSession(serverQInstance, Map.of("sessionId", name));
      new Permissions().customizeSession(serverQInstance, session, Map.of());
      QContext.init(serverQInstance, session);
      DurableJobsSecurity.ensureSessionKeys(session);
   }



   /*******************************************************************************
    ** Launch a queued fixture job.
    *******************************************************************************/
   private Integer launch(String owner) throws Exception
   {
      session(owner);
      return (DurableJobs.launch(new LaunchRequest("greet")));
   }



   /*******************************************************************************
    ** The memory store for the current test instance.
    *******************************************************************************/
   private DurableJobStore store()
   {
      QContext.setQInstance(serverQInstance);
      return (new DurableJobStore(DurableJobsMetaData.of(serverQInstance)));
   }



   /*******************************************************************************
    ** Authenticated GET.
    *******************************************************************************/
   private HttpResponse<String> get(String path, String user)
   {
      return (Unirest.get(getBaseUrlAndPath() + path).cookie("sessionId", user).asString());
   }



   /*******************************************************************************
    ** Authenticated POST.
    *******************************************************************************/
   private HttpResponse<String> post(String path, String user, String body)
   {
      return (Unirest.post(getBaseUrlAndPath() + path).cookie("sessionId", user).header("Content-Type", "application/json").body(body).asString());
   }



   /*******************************************************************************
    ** Assert successful JSON responses with useful diagnostics.
    *******************************************************************************/
   private JSONObject json(HttpResponse<String> response)
   {
      assertThat(response.getStatus()).as(response.getBody()).isEqualTo(200);
      return (JsonUtils.toJSONObject(response.getBody()));
   }
}
