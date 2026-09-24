/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.processes.implementations.mock.MockBackendStep;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ProcessStatusSpecV1
 *******************************************************************************/
class ProcessStatusSpecV1Test extends SpecTestBase
{
   /*******************************************************************************
    ** A process continuation must use the same explicit test session.
    *******************************************************************************/
   @BeforeEach
   void setProcessClientSession()
   {
      Unirest.config().reset().setDefaultHeader("Cookie", "sessionId=" + UUID.randomUUID());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void clearProcessClientSession()
   {
      Unirest.config().reset().getDefaultHeaders().remove("Cookie");
   }



   private static final int MORE_THAN_TIMEOUT = 500;
   private static final int TIMEOUT           = 300;
   private static final int LESS_THAN_TIMEOUT = 50;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ProcessStatusSpecV1();
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
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QLogger.deactivateCollectingLoggerForClass(MockBackendStep.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInitWentAsync()
   {
      /////////////////////////////////////////
      // init process, which should go async //
      /////////////////////////////////////////
      String processBasePath = getBaseUrlAndPath() + "/processes/" + TestUtils.PROCESS_NAME_SIMPLE_SLEEP;
      HttpResponse<String> response = Unirest.post(processBasePath + "/init")
         .multiPartContent()
         .field("stepTimeoutMillis", String.valueOf(TIMEOUT))
         .field("values", new JSONObject()
            .put(TestUtils.SleeperStep.FIELD_SLEEP_MILLIS, MORE_THAN_TIMEOUT)
            .toString())
         .asString();

      ///////////////////////////////////
      // assert we got back job-status //
      ///////////////////////////////////
      assertEquals(200, response.getStatus());
      JSONObject jsonObject  = assertProcessStepWentAsyncResponse(response);
      String     processUUID = jsonObject.getString("processUUID");
      String     jobUUID     = jsonObject.getString("jobUUID");
      assertNotNull(processUUID, "Process UUID should not be null.");
      assertNotNull(jobUUID, "Job UUID should not be null");

      /////////////////////////////////////////////
      // request job status before sleep is done //
      /////////////////////////////////////////////
      response = Unirest.get(processBasePath + "/" + processUUID + "/status/" + jobUUID).asString();
      jsonObject = assertProcessStepRunningResponse(response);

      ///////////////////////////////////
      // sleep, to let that job finish //
      ///////////////////////////////////
      SleepUtils.sleep(MORE_THAN_TIMEOUT, TimeUnit.MILLISECONDS);

      ////////////////////////////////////////////////////////
      // request job status again, get back results instead //
      ////////////////////////////////////////////////////////
      response = Unirest.get(processBasePath + "/" + processUUID + "/status/" + jobUUID).asString();
      jsonObject = assertProcessStepCompleteResponse(response);
   }



   /*******************************************************************************
    ** test running a step a process that goes async
    **
    *******************************************************************************/
   @Test
   public void test_processStepGoingAsync() throws InterruptedException
   {
      ///////////////////////////////////////////////////////////
      // first init the process, to get its UUID               //
      // note this process doesn't sleep until its second step //
      ///////////////////////////////////////////////////////////
      String processBasePath = getBaseUrlAndPath() + "/processes/" + TestUtils.PROCESS_NAME_SLEEP_INTERACTIVE;
      HttpResponse<String> response = Unirest.post(processBasePath + "/init")
         .multiPartContent()
         .field("values", new JSONObject()
            .put(TestUtils.SleeperStep.FIELD_SLEEP_MILLIS, MORE_THAN_TIMEOUT)
            .toString())
         .asString();

      JSONObject jsonObject  = assertProcessStepCompleteResponse(response);
      String     processUUID = jsonObject.getString("processUUID");
      String     nextStep    = jsonObject.getString("nextStep");
      assertNotNull(processUUID, "Process UUID should not be null.");
      assertNotNull(nextStep, "There should be a next step");
      assertFalse(jsonObject.getJSONObject("values").has("didSleep"), "There should not (yet) be a value from the backend step");

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // second, run the 'nextStep' (the backend step, that sleeps). run it with a long enough sleep so that it'll go async //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      response = Unirest.post(processBasePath + "/" + processUUID + "/step/" + nextStep)
         .multiPartContent()
         .field("stepTimeoutMillis", String.valueOf(TIMEOUT))
         .asString();

      jsonObject = assertProcessStepWentAsyncResponse(response);
      String jobUUID = jsonObject.getString("jobUUID");

      ///////////////////////////////////
      // sleep, to let that job finish //
      ///////////////////////////////////
      Thread.sleep(MORE_THAN_TIMEOUT);

      ///////////////////////////////
      // third, request job status //
      ///////////////////////////////
      response = Unirest.get(processBasePath + "/" + processUUID + "/status/" + jobUUID).asString();

      jsonObject = assertProcessStepCompleteResponse(response);
      String nextStep2 = jsonObject.getString("nextStep");
      assertNotNull(nextStep2, "There be one more next step");
      assertNotEquals(nextStep, nextStep2, "The next step should be different this time.");
      assertTrue(jsonObject.getJSONObject("values").has("didSleep"), "There should be a value from the backend step");
   }



   /*******************************************************************************
    ** test init'ing a process that goes async and then throws
    **
    *******************************************************************************/
   @Test
   public void test_processInitGoingAsyncThenThrowing() throws InterruptedException
   {
      String processBasePath = getBaseUrlAndPath() + "/processes/" + TestUtils.PROCESS_NAME_SIMPLE_THROW;
      HttpResponse<String> response = Unirest.post(processBasePath + "/init")
         .multiPartContent()
         .field("stepTimeoutMillis", String.valueOf(TIMEOUT))
         .field("values", new JSONObject()
            .put(TestUtils.SleeperStep.FIELD_SLEEP_MILLIS, MORE_THAN_TIMEOUT)
            .toString())
         .asString();

      JSONObject jsonObject  = assertProcessStepWentAsyncResponse(response);
      String     processUUID = jsonObject.getString("processUUID");
      String     jobUUID     = jsonObject.getString("jobUUID");

      /////////////////////////////////////////////
      // request job status before sleep is done //
      /////////////////////////////////////////////
      response = Unirest.get(processBasePath + "/" + processUUID + "/status/" + jobUUID).asString();
      jsonObject = assertProcessStepRunningResponse(response);

      ///////////////////////////////////
      // sleep, to let that job finish //
      ///////////////////////////////////
      Thread.sleep(MORE_THAN_TIMEOUT);

      /////////////////////////////////////////////////////////////
      // request job status again, get back error status instead //
      /////////////////////////////////////////////////////////////
      response = Unirest.get(processBasePath + "/" + processUUID + "/status/" + jobUUID).asString();
      jsonObject = assertProcessStepErrorResponse(response);
   }



   /*******************************************************************************
    ** every time a process step (or init) has gone async, expect what the
    ** response should look like
    *******************************************************************************/
   private JSONObject assertProcessStepWentAsyncResponse(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());

      assertEquals("JOB_STARTED", jsonObject.getString("type"));

      assertTrue(jsonObject.has("processUUID"), "Async-started response should have a processUUID");
      assertTrue(jsonObject.has("jobUUID"), "Async-started response should have a jobUUID");

      assertFalse(jsonObject.has("values"), "Async-started response should NOT have values");
      assertFalse(jsonObject.has("error"), "Async-started response should NOT have error");

      return (jsonObject);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private JSONObject assertProcessStepRunningResponse(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());

      assertEquals("RUNNING", jsonObject.getString("type"), "Step Running response should have type=RUNNING");

      assertFalse(jsonObject.has("values"), "Step Running response should NOT have values");
      assertFalse(jsonObject.has("error"), "Step Running response should NOT have error");

      return (jsonObject);
   }



   /*******************************************************************************
    ** every time a process step (sync or async) completes, expect certain things
    ** to be (and not to be) in the json response.
    *******************************************************************************/
   private JSONObject assertProcessStepCompleteResponse(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());

      assertEquals("COMPLETE", jsonObject.getString("type"), "Step Running response should have type=COMPLETE");
      assertTrue(jsonObject.has("values"), "Step Complete response should have values");

      assertFalse(jsonObject.has("jobUUID"), "Step Complete response should not have a jobUUID");
      assertFalse(jsonObject.has("error"), "Step Complete response should not have an error");

      return (jsonObject);
   }



   /*******************************************************************************
    ** every time a process step (sync or async) has an error, expect certain things
    ** to be (and not to be) in the json response.
    *******************************************************************************/
   private JSONObject assertProcessStepErrorResponse(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());

      assertEquals("ERROR", jsonObject.getString("type"), "Step Running response should have type=ERROR");
      assertTrue(jsonObject.has("error"), "Step Error response should have an error");

      assertFalse(jsonObject.has("jobUUID"), "Step Error response should not have a jobUUID");
      assertFalse(jsonObject.has("values"), "Step Error response should not have values");

      return (jsonObject);
   }

}
