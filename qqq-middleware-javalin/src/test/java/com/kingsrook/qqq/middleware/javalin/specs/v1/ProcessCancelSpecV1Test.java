/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for ProcessCancelSpecV1
 *******************************************************************************/
class ProcessCancelSpecV1Test extends SpecTestBase
{
   private static final String THROWING_PROCESS_NAME = "throwingNativeProcess";
   private static final String THROWN_MESSAGE        = "native step failed on purpose";


   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ProcessCancelSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return List.of(new ProcessInitSpecV1(), new ProcessStepSpecV1());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /*******************************************************************************
    ** Test cancelling a process that was previously initialized.
    *******************************************************************************/
   @Test
   void testCancelProcess()
   {
      /////////////////////////////////////////////////////////////
      // first, init a process to get a valid processUUID.       //
      // use the greet process since it completes synchronously. //
      /////////////////////////////////////////////////////////////
      HttpResponse<String> initResponse = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .cookie("sessionId", "v1-process-session")
         .multiPartContent()
         .field("recordsParam", "recordIds")
         .field("recordIds", "1,2")
         .asString();

      assertEquals(200, initResponse.getStatus());
      JSONObject initJson    = JsonUtils.toJSONObject(initResponse.getBody());
      String     processUUID = initJson.getString("processUUID");
      assertNotNull(processUUID);

      //////////////////////////////////////////////
      // now cancel it via the cancel endpoint.   //
      //////////////////////////////////////////////
      HttpResponse<String> cancelResponse = Unirest.post(getBaseUrlAndPath() + "/processes/greet/" + processUUID + "/cancel")
         .cookie("sessionId", "v1-process-session")
         .asString();

      assertEquals(200, cancelResponse.getStatus());
      assertEquals("{}", cancelResponse.getBody());
   }



   /*******************************************************************************
    ** Another session cannot cancel (run the cancel step of) a process it did
    ** not run.
    *******************************************************************************/
   @Test
   void testCancelAnotherSessionsProcessIsRefused()
   {
      HttpResponse<String> initResponse = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .cookie("sessionId", "v1-process-owner")
         .multiPartContent()
         .field("recordsParam", "recordIds")
         .field("recordIds", "1,2")
         .asString();
      String processUUID = JsonUtils.toJSONObject(initResponse.getBody()).getString("processUUID");

      HttpResponse<String> cancelResponse = Unirest.post(getBaseUrlAndPath() + "/processes/greet/" + processUUID + "/cancel")
         .cookie("sessionId", "v1-process-intruder")
         .asString();
      assertEquals(403, cancelResponse.getStatus(), cancelResponse.getBody());
   }



   /***************************************************************************
    ** A process whose backend step sets some output, then throws.
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = super.defineQInstance();
      instance.addProcess(new QProcessMetaData().withName(THROWING_PROCESS_NAME)
         .withStep(new QFrontendStepMetaData().withName("input"))
         .withStep(new QBackendStepMetaData().withName("boom")
            .withCode(new QCodeReferenceLambda<BackendStep>((input, output) ->
            {
               output.addValue("partial", "should-not-be-kept");
               throw new QException(THROWN_MESSAGE);
            })))
         .withStep(new QFrontendStepMetaData().withName("result")));
      return instance;
   }



   /*******************************************************************************
    ** A backend step that throws, after a frontend step, still leaves the run
    ** owned by the session that started it: the thrown error comes back as it
    ** always has (a 200 with type ERROR), the owner can submit the step again or
    ** cancel the run, and another session can do neither.
    *******************************************************************************/
   @Test
   void testCancelProcessAfterBackendStepThrew()
   {
      String processPath = getBaseUrlAndPath() + "/processes/" + THROWING_PROCESS_NAME;

      HttpResponse<String> initResponse = Unirest.post(processPath + "/init")
         .cookie("sessionId", "v1-process-owner")
         .multiPartContent()
         .field("stepTimeoutMillis", "10000")
         .asString();
      assertEquals(200, initResponse.getStatus(), initResponse.getBody());
      JSONObject initJson = JsonUtils.toJSONObject(initResponse.getBody());
      assertEquals("input", initJson.getString("nextStep"));
      String processUUID = initJson.getString("processUUID");

      /////////////////////////////////////////////////////////////////////////
      // submit the frontend step - so the backend step runs, and throws.    //
      // the error is reported as before - 200, type ERROR, the thrown text. //
      /////////////////////////////////////////////////////////////////////////
      HttpResponse<String> failedResponse = submitInputStep(processPath, processUUID, "v1-process-owner");
      assertEquals(200, failedResponse.getStatus(), failedResponse.getBody());
      JSONObject failedJson = JsonUtils.toJSONObject(failedResponse.getBody());
      assertEquals("ERROR", failedJson.getString("type"));
      assertThat(failedJson.getString("error")).contains(THROWN_MESSAGE);
      assertEquals(processUUID, failedJson.getString("processUUID"));
      assertThat(failedResponse.getBody()).doesNotContain("should-not-be-kept");

      ///////////////////////////////////////////////////////////////////////////
      // another session cannot cancel the failed run, or consume its state by //
      // submitting a step                                                     //
      ///////////////////////////////////////////////////////////////////////////
      HttpResponse<String> intruderCancelResponse = Unirest.post(processPath + "/" + processUUID + "/cancel")
         .cookie("sessionId", "v1-process-intruder")
         .asString();
      assertEquals(403, intruderCancelResponse.getStatus(), intruderCancelResponse.getBody());

      HttpResponse<String> intruderStepResponse = submitInputStep(processPath, processUUID, "v1-process-intruder");
      ////////////////////////////////////////////////////////////////////////////////////
      // a step runs as an async job, which wraps the permission failure - so the step  //
      // route (unlike cancel) reports it as the usual 200 / type ERROR, naming the     //
      // process-state permission denial, and nothing of the step's own failure         //
      ////////////////////////////////////////////////////////////////////////////////////
      assertEquals(200, intruderStepResponse.getStatus(), intruderStepResponse.getBody());
      JSONObject intruderStepJson = JsonUtils.toJSONObject(intruderStepResponse.getBody());
      assertEquals("ERROR", intruderStepJson.getString("type"));
      assertEquals("Error message: Permission denied for process state.", intruderStepJson.getString("error"));
      assertThat(intruderStepResponse.getBody()).doesNotContain(THROWN_MESSAGE).doesNotContain("should-not-be-kept");

      /////////////////////////////////////////////////////////////////////////////
      // the owner is not locked out: resubmitting reaches the same step failure //
      // (not a permission error), and cancelling works - and is then terminal   //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> retryResponse = submitInputStep(processPath, processUUID, "v1-process-owner");
      assertEquals(200, retryResponse.getStatus(), retryResponse.getBody());
      assertThat(JsonUtils.toJSONObject(retryResponse.getBody()).getString("error")).contains(THROWN_MESSAGE);

      HttpResponse<String> cancelResponse = Unirest.post(processPath + "/" + processUUID + "/cancel")
         .cookie("sessionId", "v1-process-owner")
         .asString();
      assertEquals(200, cancelResponse.getStatus(), cancelResponse.getBody());
      assertEquals("{}", cancelResponse.getBody());

      HttpResponse<String> afterCancelResponse = submitInputStep(processPath, processUUID, "v1-process-owner");
      assertEquals(400, afterCancelResponse.getStatus(), afterCancelResponse.getBody());
      assertThat(JsonUtils.toJSONObject(afterCancelResponse.getBody()).getString("error")).contains("process was cancelled");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private HttpResponse<String> submitInputStep(String processPath, String processUUID, String session)
   {
      return Unirest.post(processPath + "/" + processUUID + "/step/input")
         .cookie("sessionId", session)
         .multiPartContent()
         .field("values", "{}")
         .field("stepTimeoutMillis", "10000")
         .asString();
   }



   /*******************************************************************************
    ** Test cancelling a non-existent process (random UUID) returns an error.
    *******************************************************************************/
   @Test
   void testCancelNonExistentProcess()
   {
      String fakeUUID = UUID.randomUUID().toString();

      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/" + fakeUUID + "/cancel")
         .cookie("sessionId", "v1-process-session")
         .asString();

      assertThat(response.getStatus()).isIn(400, 500);
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.has("error")).isTrue();
      assertThat(jsonObject.getString("error")).isNotEmpty();
   }



   /*******************************************************************************
    ** A processUUID that is not a UUID is a bad request (#1346).
    *******************************************************************************/
   @Test
   void testCancelMalformedUUID()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/nonexistent-uuid/cancel")
         .cookie("sessionId", "v1-process-session")
         .asString();

      assertEquals(400, response.getStatus(), response.getBody());
      assertEquals("processUUID", JsonUtils.toJSONObject(response.getBody()).getJSONArray("fieldErrors").getJSONObject(0).getString("field"));
   }



   /*******************************************************************************
    ** Cancelling is terminal: a cancelled run refuses further steps with a 400,
    ** even when the process has no cancel step; cancelling again is a no-op
    ** (#1346).
    *******************************************************************************/
   @Test
   void testCancelledProcessRefusesSteps()
   {
      String processPath = getBaseUrlAndPath() + "/processes/" + TestUtils.PROCESS_NAME_SLEEP_INTERACTIVE;
      HttpResponse<String> initResponse = Unirest.post(processPath + "/init")
         .cookie("sessionId", "v1-process-session")
         .multiPartContent()
         .field("values", new JSONObject().put(TestUtils.SleeperStep.FIELD_SLEEP_MILLIS, 1).toString())
         .asString();
      assertEquals(200, initResponse.getStatus(), initResponse.getBody());
      String processUUID = JsonUtils.toJSONObject(initResponse.getBody()).getString("processUUID");

      HttpResponse<String> cancelResponse = Unirest.post(processPath + "/" + processUUID + "/cancel")
         .cookie("sessionId", "v1-process-session")
         .asString();
      assertEquals(200, cancelResponse.getStatus(), cancelResponse.getBody());

      HttpResponse<String> stepResponse = Unirest.post(processPath + "/" + processUUID + "/step/" + TestUtils.SCREEN_0)
         .cookie("sessionId", "v1-process-session")
         .multiPartContent()
         .field("values", "{}")
         .asString();
      assertEquals(400, stepResponse.getStatus(), stepResponse.getBody());
      assertThat(JsonUtils.toJSONObject(stepResponse.getBody()).getString("error")).contains("process was cancelled");

      HttpResponse<String> cancelAgainResponse = Unirest.post(processPath + "/" + processUUID + "/cancel")
         .cookie("sessionId", "v1-process-session")
         .asString();
      assertEquals(200, cancelAgainResponse.getStatus(), cancelAgainResponse.getBody());
   }

}
