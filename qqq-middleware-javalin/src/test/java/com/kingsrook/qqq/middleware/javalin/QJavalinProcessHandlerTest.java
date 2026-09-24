/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobState;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for the javalin process handler methods.
 *******************************************************************************/
class QJavalinProcessHandlerTest extends QJavalinTestBase
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
   private static final int LESS_THAN_TIMEOUT = 50;



   /*******************************************************************************
    ** test running a process
    **
    ** Note: ported to v1
    *******************************************************************************/
   @Test
   public void test_processGreetInit()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/processes/greet/init?recordsParam=recordIds&recordIds=2,3").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals("null X null", jsonObject.getJSONObject("values").getString("outputMessage"));
   }



   /*******************************************************************************
    ** test running a process that requires rows, but we didn't tell it how to get them.
    **
    ** Note: ported to v1
    *******************************************************************************/
   @Test
   public void test_processRequiresRowsButNotSpecified()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/processes/greet/init").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertTrue(jsonObject.has("error"));
      assertTrue(jsonObject.getString("error").contains("Missing input records"));
   }



   /*******************************************************************************
    ** test running a process and telling it rows to load via recordIds param
    **
    ** Note: ported to v1
    *******************************************************************************/
   @Test
   public void test_processRequiresRowsWithRecordIdParam()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/processes/greet/init?recordsParam=recordIds&recordIds=2,3").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      String processUUID = jsonObject.getString("processUUID");

      getProcessRecords(processUUID, 2);
   }



   /*******************************************************************************
    ** test running a process and telling it rows to load via filter JSON
    **
    ** Note: ported to v1
    *******************************************************************************/
   @Test
   public void test_processRequiresRowsWithFilterJSON()
   {
      QQueryFilter queryFilter = new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("id")
            .withOperator(QCriteriaOperator.IN)
            .withValues(List.of(3, 4, 5)));
      String filterJSON = JsonUtils.toJson(queryFilter);

      HttpResponse<String> response = Unirest.get(BASE_URL + "/processes/greet/init?recordsParam=filterJSON&filterJSON=" + URLEncoder.encode(filterJSON, StandardCharsets.UTF_8)).asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      String processUUID = jsonObject.getString("processUUID");

      getProcessRecords(processUUID, 3);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject getProcessRecords(String processUUID, int expectedNoOfRecords)
   {
      return (getProcessRecords(processUUID, expectedNoOfRecords, 0, 20));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject getProcessRecords(String processUUID, int expectedNoOfRecords, int skip, int limit)
   {
      HttpResponse<String> response;
      JSONObject           jsonObject;
      response = Unirest.get(BASE_URL + "/processes/greet/" + processUUID + "/records?skip=" + skip + "&limit=" + limit).asString();
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);

      if(expectedNoOfRecords == 0)
      {
         assertFalse(jsonObject.has("records"));
      }
      else
      {
         assertTrue(jsonObject.has("records"), jsonObject.toString());
         JSONArray records = jsonObject.getJSONArray("records");
         assertEquals(expectedNoOfRecords, records.length());
      }
      return (jsonObject);
   }



   /*******************************************************************************
    ** test running a process with field values on the query string
    **
    ** Note: ported to v1
    *******************************************************************************/
   @Test
   public void test_processGreetInitWithQueryValues()
   {
      HttpResponse<String> response = Unirest.post(BASE_URL + "/processes/greet/init?recordsParam=recordIds&recordIds=2,3&greetingPrefix=Hey&greetingSuffix=Jude").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals("Hey X Jude", jsonObject.getJSONObject("values").getString("outputMessage"));
   }



   /*******************************************************************************
    ** test init'ing a process that goes async
    **
    ** Note: ported to v1
    *******************************************************************************/
   @Test
   public void test_processInitGoingAsync() throws InterruptedException
   {
      String               processBasePath = BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_SIMPLE_SLEEP;
      HttpResponse<String> response        = Unirest.get(processBasePath + "/init?" + TestUtils.SleeperStep.FIELD_SLEEP_MILLIS + "=" + MORE_THAN_TIMEOUT).asString();

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
      Thread.sleep(MORE_THAN_TIMEOUT);

      ////////////////////////////////////////////////////////
      // request job status again, get back results instead //
      ////////////////////////////////////////////////////////
      response = Unirest.get(processBasePath + "/" + processUUID + "/status/" + jobUUID).asString();
      jsonObject = assertProcessStepCompleteResponse(response);
   }



   /*******************************************************************************
    ** test init'ing a process that does NOT goes async
    **
    ** Note: not ported to v1, but feels redundant, so, not going to.
    *******************************************************************************/
   @Test
   public void test_processInitNotGoingAsync()
   {
      HttpResponse<String> response = Unirest.post(BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_SIMPLE_SLEEP + "/init?" + TestUtils.SleeperStep.FIELD_SLEEP_MILLIS + "=" + LESS_THAN_TIMEOUT)
         .header("Content-Type", "application/json").asString();
      assertProcessStepCompleteResponse(response);
   }



   /*******************************************************************************
    ** test running a step a process that goes async
    **
    ** Note:  ported to v1
    *******************************************************************************/
   @Test
   public void test_processStepGoingAsync() throws InterruptedException
   {
      /////////////////////////////////////////////
      // first init the process, to get its UUID //
      /////////////////////////////////////////////
      String processBasePath = BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_SLEEP_INTERACTIVE;
      HttpResponse<String> response = Unirest.post(processBasePath + "/init?" + TestUtils.SleeperStep.FIELD_SLEEP_MILLIS + "=" + MORE_THAN_TIMEOUT)
         .header("Content-Type", "application/json").asString();

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
         .header("Content-Type", "application/json").asString();

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
    ** test fully running a process that has frontend steps - and observing that we
    ** don't stop on them.
    **
    *******************************************************************************/
   @Test
   public void test_processRunWithoutBreakingForFrontendSteps() throws InterruptedException
   {
      /////////////////////////////////////////////
      // first init the process, to get its UUID //
      /////////////////////////////////////////////
      String processBasePath = BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_SLEEP_INTERACTIVE;
      HttpResponse<String> response = Unirest.post(processBasePath + "/run?" + TestUtils.SleeperStep.FIELD_SLEEP_MILLIS + "=" + LESS_THAN_TIMEOUT)
         .header("Content-Type", "application/json").asString();

      JSONObject jsonObject  = assertProcessStepCompleteResponse(response);
      String     processUUID = jsonObject.getString("processUUID");
      assertNotNull(processUUID, "Process UUID should not be null.");
      assertFalse(jsonObject.has("nextStep"), "There should not be a next step");
      assertTrue(jsonObject.getJSONObject("values").has("didSleep"), "There should be a value from the backend step");
   }



   /*******************************************************************************
    ** test running a step a process that does NOT goes async
    **
    *******************************************************************************/
   @Test
   public void test_processStepNotGoingAsync()
   {
      /////////////////////////////////////////////
      // first init the process, to get its UUID //
      /////////////////////////////////////////////
      String processBasePath = BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_SLEEP_INTERACTIVE;
      HttpResponse<String> response = Unirest.post(processBasePath + "/init?" + TestUtils.SleeperStep.FIELD_SLEEP_MILLIS + "=" + LESS_THAN_TIMEOUT)
         .header("Content-Type", "application/json").asString();

      JSONObject jsonObject  = assertProcessStepCompleteResponse(response);
      String     processUUID = jsonObject.getString("processUUID");
      String     nextStep    = jsonObject.getString("nextStep");
      assertNotNull(nextStep, "There should be a next step");

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // second, run the 'nextStep' (the backend step, that sleeps). run it with a short enough sleep so that it won't go async //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      response = Unirest.post(processBasePath + "/" + processUUID + "/step/" + nextStep)
         .header("Content-Type", "application/json").asString();

      jsonObject = assertProcessStepCompleteResponse(response);
      String nextStep2 = jsonObject.getString("nextStep");
      assertNotNull(nextStep2, "There be one more next step");
      assertNotEquals(nextStep, nextStep2, "The next step should be different this time.");
   }



   /*******************************************************************************
    ** test init'ing a process that goes async and then throws
    **
    ** Note:  ported to v1
    *******************************************************************************/
   @Test
   public void test_processInitGoingAsyncThenThrowing() throws InterruptedException
   {
      String               processBasePath = BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_SIMPLE_THROW;
      HttpResponse<String> response        = Unirest.get(processBasePath + "/init?" + TestUtils.ThrowerStep.FIELD_SLEEP_MILLIS + "=" + MORE_THAN_TIMEOUT).asString();

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
    ** test init'ing a process that does NOT goes async, but throws.
    **
    *******************************************************************************/
   @Test
   public void test_processInitNotGoingAsyncButThrowing()
   {
      HttpResponse<String> response = Unirest.post(BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_SIMPLE_THROW + "/init?" + TestUtils.ThrowerStep.FIELD_SLEEP_MILLIS + "=" + LESS_THAN_TIMEOUT)
         .header("Content-Type", "application/json").asString();
      assertProcessStepErrorResponse(response);
   }



   /*******************************************************************************
    ** every time a process step (or init) has gone async, expect what the
    ** response should look like
    *******************************************************************************/
   private JSONObject assertProcessStepWentAsyncResponse(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());

      assertTrue(jsonObject.has("processUUID"), "Async-started response should have a processUUID");
      assertTrue(jsonObject.has("jobUUID"), "Async-started response should have a jobUUID");

      assertFalse(jsonObject.has("values"), "Async-started response should NOT have values");
      assertFalse(jsonObject.has("error"), "Async-started response should NOT have error");

      return (jsonObject);
   }



   /*******************************************************************************
    ** every time a process step (sync or async) is still running, expect certain things
    ** to be (and not to be) in the json response.
    *******************************************************************************/
   private JSONObject assertProcessStepRunningResponse(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());

      assertTrue(jsonObject.has("jobStatus"), "Step Running response should have a jobStatus");

      assertFalse(jsonObject.has("values"), "Step Running response should NOT have values");
      assertFalse(jsonObject.has("error"), "Step Running response should NOT have error");

      assertEquals(AsyncJobState.RUNNING.name(), jsonObject.getJSONObject("jobStatus").getString("state"));

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

      assertTrue(jsonObject.has("error"), "Step Error response should have an error");

      assertFalse(jsonObject.has("jobUUID"), "Step Error response should not have a jobUUID");
      assertFalse(jsonObject.has("values"), "Step Error response should not have values");

      return (jsonObject);
   }



   /*******************************************************************************
    ** test getting records back from a process
    **
    *******************************************************************************/
   @Test
   public void test_processRecords()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/processes/greet/init?recordsParam=recordIds&recordIds=2,3&greetingPrefix=Hey&greetingSuffix=Jude").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      String processUUID = jsonObject.getString("processUUID");

      jsonObject = getProcessRecords(processUUID, 2);
      JSONArray  records = jsonObject.getJSONArray("records");
      JSONObject record0 = records.getJSONObject(0);
      JSONObject values  = record0.getJSONObject("values");
      assertTrue(values.has("id"));
      assertTrue(values.has("firstName"));
   }



   /*******************************************************************************
    ** test getting records back from a process with skip & Limit
    **
    *******************************************************************************/
   @Test
   public void test_processRecordsSkipAndLimit()
   {
      HttpResponse<String> response    = Unirest.get(BASE_URL + "/processes/greet/init?recordsParam=recordIds&recordIds=1,2,3,4,5").asString();
      JSONObject           jsonObject  = JsonUtils.toJSONObject(response.getBody());
      String               processUUID = jsonObject.getString("processUUID");

      getProcessRecords(processUUID, 5);
      getProcessRecords(processUUID, 1, 4, 5);
      getProcessRecords(processUUID, 0, 5, 5);
   }



   /*******************************************************************************
    ** test running a report
    **
    *******************************************************************************/
   @Test
   public void test_report()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/reports/personsReport?format=csv&firstNamePrefix=D").asString();
      assertEquals(200, response.getStatus());
      assertThat(response.getHeaders().get("Content-Type").get(0)).contains("text/csv");
      assertThat(response.getHeaders().get("Content-Disposition").get(0)).contains("filename=personsReport.csv");
      String csv = response.getBody();
      System.out.println(csv);
      assertThat(csv).contains("""
         "Id","First Name","Last Name\"""");
      assertThat(csv).contains("""
         "1","Darin","Kelkhoff\"""");
   }



   /*******************************************************************************
    ** test running a report
    **
    *******************************************************************************/
   @Test
   public void test_reportMissingFormat()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/reports/personsReport?firstNamePrefix=D").asString();
      assertEquals(400, response.getStatus());
      assertThat(response.getHeaders().get("Content-Type").get(0)).contains("application/json");
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.getString("error")).contains("Report format was not specified");
   }



   /*******************************************************************************
    ** test running a report by filename
    **
    *******************************************************************************/
   @Test
   public void test_reportWithFileName()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/reports/personsReport/myFile.json?firstNamePrefix=D").asString();
      assertEquals(200, response.getStatus());
      assertThat(response.getHeaders().get("Content-Type").get(0)).contains("application/json");
      assertThat(response.getHeaders().get("Content-Disposition").get(0)).contains("filename=myFile.json");
      // JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      // System.out.println(jsonObject);
      JSONArray jsonArray = JsonUtils.toJSONArray(response.getBody());
   }



   /*******************************************************************************
    ** test calling download file endpoint
    **
    *******************************************************************************/
   @Test
   public void test_downloadFile()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/download/myTestFile.txt?filePath=/dev/null").asString();
      assertEquals(403, response.getStatus());
      assertTrue(response.getBody().contains("not an authorized process download"));
   }



   /*******************************************************************************
    ** test calling download file without needed query-string params
    **
    *******************************************************************************/
   @Test
   public void test_downloadFileMissingQueryStringParams()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/download/myTestFile.txt").asString();
      assertEquals(400, response.getStatus());
      assertTrue(response.getBody().contains("Missing query parameters to identify file"));
   }



   /*******************************************************************************
    ** test calling for possibleValue
    **
    *******************************************************************************/
   @Test
   public void test_possibleValues()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/processes/greet/possibleValues/partnerPersonId").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertNotNull(jsonObject.getJSONArray("options"));
      assertEquals(6, jsonObject.getJSONArray("options").length());
      assertEquals(1, jsonObject.getJSONArray("options").getJSONObject(0).getInt("id"));
      assertEquals("Darin Kelkhoff (1)", jsonObject.getJSONArray("options").getJSONObject(0).getString("label"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_processCancel()
   {
      /////////////////////////
      // 400s for bad inputs //
      /////////////////////////
      {
         HttpResponse<String> response = Unirest.get(BASE_URL + "/processes/noSuchProcess/" + UUID.randomUUID() + "/cancel").asString();
         assertEquals(400, response.getStatus());
         assertThat(response.getBody()).contains("Process [noSuchProcess] is not defined in this instance");
      }
      {
         HttpResponse<String> response = Unirest.get(BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_SIMPLE_SLEEP + "/" + UUID.randomUUID() + "/cancel").asString();
         assertEquals(400, response.getStatus());
         assertThat(response.getBody()).matches(".*State for process UUID.*not found.*");
      }

      ///////////////////////////////////
      // start a process, get its uuid //
      ///////////////////////////////////
      String processBasePath = BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_SLEEP_INTERACTIVE;
      HttpResponse<String> response = Unirest.post(processBasePath + "/init?" + TestUtils.SleeperStep.FIELD_SLEEP_MILLIS + "=" + MORE_THAN_TIMEOUT)
         .header("Content-Type", "application/json").asString();

      JSONObject jsonObject  = assertProcessStepCompleteResponse(response);
      String     processUUID = jsonObject.getString("processUUID");
      assertNotNull(processUUID, "Process UUID should not be null.");

      /////////////////////////////////////////
      // now cancel that, and expect success //
      /////////////////////////////////////////
      response = Unirest.get(BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_SLEEP_INTERACTIVE + "/" + processUUID + "/cancel").asString();
      assertEquals(200, response.getStatus());
   }



   /*******************************************************************************
    ** test running a process who has a value with a null key.
    *
    ** This was a regression - that threw an exception from jackson at one point in time.
    **
    ** Note:  ported to v1
    *******************************************************************************/
   @Test
   public void test_processPutsNullKeyInMap()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_PUTS_NULL_KEY_IN_MAP + "/init").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      JSONObject values = jsonObject.getJSONObject("values");
      JSONObject mapWithNullKey = values.getJSONObject("mapWithNullKey");
      assertTrue(mapWithNullKey.has("")); // null key currently set to become empty-string key...
      assertEquals("hadNullKey", mapWithNullKey.getString(""));
      assertTrue(mapWithNullKey.has("one"));
      assertEquals("1", mapWithNullKey.getString("one"));
   }

}
