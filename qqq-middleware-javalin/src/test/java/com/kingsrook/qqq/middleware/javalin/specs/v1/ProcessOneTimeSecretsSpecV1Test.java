/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.secrets.OneTimeSecret;
import com.kingsrook.qqq.middleware.javalin.QJavalinAccessLogger;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** HTTP contract for synchronous secrets and the once-only asynchronous hand-off.
 *******************************************************************************/
class ProcessOneTimeSecretsSpecV1Test extends SpecTestBase
{
   private static final String SECRET = "http-one-time-token-98765";



   /*******************************************************************************
    ** Serve the step endpoint under test.
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new ProcessStepSpecV1());
   }



   /*******************************************************************************
    ** Init and status use the same server and authenticated session.
    *******************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new ProcessInitSpecV1(), new ProcessStatusSpecV1()));
   }



   /*******************************************************************************
    ** API version under test.
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    ** A tiny create-token process followed by a confirmation and no-op step.
    *******************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = super.defineQInstance();
      instance.addProcess(new QProcessMetaData().withName("secretProcess")
         .withStep(new QFrontendStepMetaData().withName("create"))
         .withStep(new QBackendStepMetaData().withName("createToken").withCode(new QCodeReferenceLambda<BackendStep>((input, output) ->
            output.addOneTimeSecret(new OneTimeSecret().withLabel("Access token").withValue(SECRET)
               .withMasked("****8765").withUsageText("Copy this token now.")))))
         .withStep(new QFrontendStepMetaData().withName("shown"))
         .withStep(new QBackendStepMetaData().withName("finish").withCode(new QCodeReferenceLambda<BackendStep>((input, output) -> output.addValue("finished", true)))));
      instance.addProcess(new QProcessMetaData().withName("secretInit")
         .withStep(new QBackendStepMetaData().withName("createToken").withCode(new QCodeReferenceLambda<BackendStep>((input, output) ->
            output.addOneTimeSecret(new OneTimeSecret().withLabel("Access token").withValue(SECRET).withMasked("****8765"))))));
      return (instance);
   }



   /*******************************************************************************
    ** A synchronous step delivers the secret once; subsequent status cannot replay it.
    *******************************************************************************/
   @Test
   void testSynchronousStepAndStatus() throws Exception
   {
      String cookie = "sessionId=" + UUID.randomUUID();
      String base = getBaseUrlAndPath() + "/processes/secretProcess";
      JSONObject init = new JSONObject(Unirest.post(base + "/init").header("Cookie", cookie).asString().getBody());
      String processPath = base + "/" + init.getString("processUUID");
      QCollectingLogger logger = QLogger.activateCollectingLoggerForClass(QJavalinAccessLogger.class);
      try
      {
         HttpResponse<String> response = Unirest.post(processPath + "/step/create").header("Cookie", cookie)
            .multiPartContent().field("stepTimeoutMillis", "10000").asString();
         assertSecretResponse(response);
         assertThat(logger.getCollectedMessages()).allSatisfy(message -> assertThat(message.toString()).doesNotContain(SECRET));

         JSONObject next = new JSONObject(Unirest.post(processPath + "/step/shown").header("Cookie", cookie)
            .multiPartContent().field("stepTimeoutMillis", "0").asString().getBody());
         assertThat(next.getString("type")).isEqualTo("JOB_STARTED");
         assertStatusHasOnlyMasks(processPath, next.getString("jobUUID"), cookie);
      }
      finally
      {
         QLogger.deactivateCollectingLoggerForClass(QJavalinAccessLogger.class);
      }
   }



   /*******************************************************************************
    ** The init route also uses the synchronous response channel.
    *******************************************************************************/
   @Test
   void testSynchronousInit()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/secretInit/init")
         .multiPartContent().field("stepTimeoutMillis", "10000").asString();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("no-store");
      assertThat(new JSONObject(response.getBody()).getJSONArray("oneTimeSecrets").getJSONObject(0).getString("value")).isEqualTo(SECRET);
   }



   /*******************************************************************************
    ** Force the producing step async: the first status poll that sees the job
    ** complete gets the secret once (OneTimeSecretHandoff); later polls do not.
    *******************************************************************************/
   @Test
   void testAsyncSecretHandedOffOnce() throws Exception
   {
      String cookie = "sessionId=" + UUID.randomUUID();
      String base = getBaseUrlAndPath() + "/processes/secretProcess";
      JSONObject init = new JSONObject(Unirest.post(base + "/init").header("Cookie", cookie).asString().getBody());
      String processPath = base + "/" + init.getString("processUUID");
      HttpResponse<String> response = Unirest.post(processPath + "/step/create").header("Cookie", cookie)
         .multiPartContent().field("stepTimeoutMillis", "0").asString();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getBody()).doesNotContain(SECRET, "oneTimeSecrets");
      JSONObject job = new JSONObject(response.getBody());
      assertThat(job.getString("type")).isEqualTo("JOB_STARTED");

      HttpResponse<String> status = null;
      for(int attempt = 0; attempt < 100; attempt++)
      {
         status = Unirest.get(processPath + "/status/" + job.getString("jobUUID")).header("Cookie", cookie).asString();
         if(!"RUNNING".equals(new JSONObject(status.getBody()).getString("type")))
         {
            break;
         }
         Thread.sleep(20);
      }
      assertSecretResponse(status);
      assertThat(Unirest.get(processPath + "/status/" + job.getString("jobUUID")).header("Cookie", cookie).asString().getBody())
         .doesNotContain(SECRET, "\"oneTimeSecrets\"");
   }



   /*******************************************************************************
    ** Verify all four shared schema fields and cache prevention.
    *******************************************************************************/
   private void assertSecretResponse(HttpResponse<String> response)
   {
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("no-store");
      JSONObject json = new JSONObject(response.getBody());
      assertThat(json.getString("type")).isEqualTo("COMPLETE");
      assertThat(json.getJSONArray("oneTimeSecrets").length()).isEqualTo(1);
      JSONObject secret = json.getJSONArray("oneTimeSecrets").getJSONObject(0);
      assertThat(secret.getString("value")).isEqualTo(SECRET);
      assertThat(secret.getString("label")).isEqualTo("Access token");
      assertThat(secret.getString("masked")).isEqualTo("****8765");
      assertThat(secret.getString("usageText")).isEqualTo("Copy this token now.");
      assertThat(json.getJSONObject("values").toString()).doesNotContain(SECRET);
   }



   /*******************************************************************************
    ** Poll a real completed job and repeat the request to prove no secret replay.
    *******************************************************************************/
   private void assertStatusHasOnlyMasks(String processPath, String jobUUID, String cookie) throws Exception
   {
      JSONObject status = null;
      for(int attempt = 0; attempt < 100; attempt++)
      {
         HttpResponse<String> response = Unirest.get(processPath + "/status/" + jobUUID).header("Cookie", cookie).asString();
         assertThat(response.getStatus()).isEqualTo(200);
         assertThat(response.getBody()).doesNotContain(SECRET, "\"oneTimeSecrets\"");
         status = new JSONObject(response.getBody());
         if(!"RUNNING".equals(status.getString("type")))
         {
            break;
         }
         Thread.sleep(20);
      }
      assertThat(status.getString("type")).isEqualTo("COMPLETE");
      assertThat(status.getJSONObject("values").getJSONArray("oneTimeSecretsMasked").toList()).containsExactly("****8765");
      assertThat(Unirest.get(processPath + "/status/" + jobUUID).header("Cookie", cookie).asString().getBody())
         .doesNotContain(SECRET, "\"oneTimeSecrets\"");
   }
}
