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


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** POST /qqq/v1/processes/{processName}/jobs: launch a durable job.
 *******************************************************************************/
class ProcessJobLaunchSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ProcessJobLaunchSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    ** The standard test instance plus durable jobs (no runtime is started, so
    ** launched jobs stay queued).
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = TestUtils.defineInstance();
      new DurableJobsMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      instance.withSupplementalMetaData(new DurableJobsMetaData().withEligibleProcessName("greet").withMaxQueuedJobsPerUser(3));
      return (instance);
   }



   /*******************************************************************************
    ** A launch returns the new job's id and QUEUED.
    *******************************************************************************/
   @Test
   void testLaunch()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/jobs")
         .cookie("sessionId", "jobs-launch-session")
         .header("Content-Type", "application/json")
         .body("""
            {"values": {"greetingPrefix": "Hi"}, "recordIds": [1, 2], "label": "Greet in the background", "priority": 7, "notifyEmail": true}""")
         .asString();

      assertThat(response.getStatus()).as(response.getBody()).isEqualTo(200);
      JSONObject json = JsonUtils.toJSONObject(response.getBody());
      assertThat(json.getInt("jobId")).isPositive();
      assertThat(json.getString("status")).isEqualTo("QUEUED");
   }



   /*******************************************************************************
    ** The assistant channel's launches wait for confirmation.
    *******************************************************************************/
   @Test
   void testAssistantLaunchAwaitsConfirmation()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/jobs")
         .cookie("sessionId", "jobs-assistant-session")
         .header("X-QQQ-Client", "assistant/turn-1")
         .header("Content-Type", "application/json")
         .body("{}")
         .asString();

      assertThat(response.getStatus()).as(response.getBody()).isEqualTo(200);
      assertThat(JsonUtils.toJSONObject(response.getBody()).getString("status")).isEqualTo("AWAITING_CONFIRMATION");
   }



   /*******************************************************************************
    ** Unknown process is 404; a process that cannot run as a job is 400; a bad
    ** body is 400.
    *******************************************************************************/
   @Test
   void testRefusals()
   {
      assertThat(Unirest.post(getBaseUrlAndPath() + "/processes/noSuchProcess/jobs").cookie("sessionId", "jobs-refusal-session").body("{}").asString().getStatus()).isEqualTo(404);
      assertThat(Unirest.post(getBaseUrlAndPath() + "/processes/" + TestUtils.PROCESS_NAME_SIMPLE_SLEEP + "/jobs").cookie("sessionId", "jobs-refusal-session").body("{}").asString().getStatus()).isEqualTo(400);
      assertThat(Unirest.post(getBaseUrlAndPath() + "/processes/greet/jobs").cookie("sessionId", "jobs-refusal-session").body("[not json").asString().getStatus()).isEqualTo(400);
      assertThat(Unirest.post(getBaseUrlAndPath() + "/processes/greet/jobs").cookie("sessionId", "jobs-refusal-session").body("{\"mode\":\"SIDEWAYS\"}").asString().getStatus()).isEqualTo(400);
   }



   /*******************************************************************************
    ** Over maxQueuedJobsPerUser the launch is refused with 429.
    *******************************************************************************/
   @Test
   void testTooManyQueuedIs429()
   {
      List<Integer> statuses = new java.util.ArrayList<>();
      for(int i = 0; i < 4; i++)
      {
         statuses.add(Unirest.post(getBaseUrlAndPath() + "/processes/greet/jobs").cookie("sessionId", "jobs-cap-session").body("{}").asString().getStatus());
      }
      assertThat(statuses).containsExactly(200, 200, 200, 429);
   }

}
