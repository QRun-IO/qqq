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


import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.processes.implementations.mock.MockBackendStep;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.openapi.model.Parameter;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ProcessInitSpecV1 
 *******************************************************************************/
class ProcessInitSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ProcessInitSpecV1();
   }



   /***************************************************************************
    ** the step endpoint, for processes that continue past init.
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return List.of(new ProcessStepSpecV1());
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
   void testGetInitialRecordsFromRecordIdsParam()
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(MockBackendStep.class);

      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .multiPartContent()
         .field("recordsParam", "recordIds")
         .field("recordIds", "2,3")
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals("COMPLETE", jsonObject.getString("type"));
      assertEquals("null X null", jsonObject.getJSONObject("values").getString("outputMessage")); // these nulls are because we didn't pass values for some fields.

      assertThat(collectingLogger.getCollectedMessages())
         .filteredOn(clm -> clm.getMessage().contains("We are mocking"))
         .hasSize(2);
      // todo - also request records
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetInitialRecordsFromFilterParam()
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(MockBackendStep.class);

      QQueryFilter queryFilter = new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("id")
            .withOperator(QCriteriaOperator.IN)
            .withValues(List.of(3, 4, 5)));
      String filterJSON = JsonUtils.toJson(queryFilter);

      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .multiPartContent()
         .field("recordsParam", "filterJSON")
         .field("filterJSON", filterJSON)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals("COMPLETE", jsonObject.getString("type"));
      assertEquals("null X null", jsonObject.getJSONObject("values").getString("outputMessage"));

      assertThat(collectingLogger.getCollectedMessages())
         .filteredOn(clm -> clm.getMessage().contains("We are mocking"))
         .hasSize(3);
      // todo - also request records
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRequiresRowsButNotSpecified()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .multiPartContent()
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals("ERROR", jsonObject.getString("type"));
      assertTrue(jsonObject.has("error"));
      assertTrue(jsonObject.getString("error").contains("Missing input records"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldValues()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .multiPartContent()
         .field("recordsParam", "recordIds")
         .field("recordIds", "2,3")
         .field("values", new JSONObject()
            .put("greetingPrefix", "Hey")
            .put("greetingSuffix", "Jude")
            .toString()
         )
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals("COMPLETE", jsonObject.getString("type"));
      assertEquals("Hey X Jude", jsonObject.getJSONObject("values").getString("outputMessage"));
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInitGoingAsync()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/" + TestUtils.PROCESS_NAME_SIMPLE_SLEEP + "/init")
         .multiPartContent()
         .field("stepTimeoutMillis", "50")
         .field("values", new JSONObject()
            .put(TestUtils.SleeperStep.FIELD_SLEEP_MILLIS, 500)
            .toString()
         )
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      String     processUUID = jsonObject.getString("processUUID");
      String     jobUUID     = jsonObject.getString("jobUUID");
      assertNotNull(processUUID, "Process UUID should not be null.");
      assertNotNull(jobUUID, "Job UUID should not be null");

      // todo - in a higher-level test, resume test_processInitGoingAsync at the // request job status before sleep is done // line
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessPutsNullKeyInMap()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/" + TestUtils.PROCESS_NAME_PUTS_NULL_KEY_IN_MAP + "/init")
         .multiPartContent()
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());

      JSONObject values = jsonObject.getJSONObject("values");
      JSONObject mapWithNullKey = values.getJSONObject("mapWithNullKey");
      assertTrue(mapWithNullKey.has("")); // null key currently set to become empty-string key...
      assertEquals("hadNullKey", mapWithNullKey.getString(""));
      assertTrue(mapWithNullKey.has("one"));
      assertEquals("1", mapWithNullKey.getString("one"));
   }



   /*******************************************************************************
    ** An uploaded file with an unsafe name is refused (400) before anything is
    ** stored, as on the legacy route (QRun-IO/qqq#543).
    *******************************************************************************/
   @Test
   void testUnsafeUploadFilenameIsRefused()
   {
      for(String filename : List.of("..", "a/b.txt", "c:\\d.txt"))
      {
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
            .field("values", "{}")
            .field("theFile", new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), filename)
            .asString();
         assertEquals(400, response.getStatus(), filename + ": " + response.getBody());
         assertThat(response.getBody()).contains("Uploaded filename must be a nonempty filename");
      }
   }



   /*******************************************************************************
    ** Without an uploaded-file archive table, a file upload is an error rather
    ** than silently dropped.
    *******************************************************************************/
   @Test
   void testUploadWithoutArchiveTableIsAnError()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .field("values", "{}")
         .field("theFile", new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), "ok.txt")
         .asString();
      assertThat(response.getStatus()).isGreaterThanOrEqualTo(400);
      assertThat(response.getBody()).contains("UploadFileArchiveTableName was not specified");
   }



   /*******************************************************************************
    ** An application/x-www-form-urlencoded body is read like a multipart one -
    ** its values are not silently ignored (#1336).
    *******************************************************************************/
   @Test
   void testUrlEncodedFieldValues()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .field("recordsParam", "recordIds")
         .field("recordIds", "2,3")
         .field("values", new JSONObject()
            .put("greetingPrefix", "Hey")
            .put("greetingSuffix", "Jude")
            .toString())
         .asString();

      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("COMPLETE", jsonObject.getString("type"), jsonObject.toString());
      assertEquals("Hey X Jude", jsonObject.getJSONObject("values").getString("outputMessage"));
   }



   /*******************************************************************************
    ** A JSON null in values is a cleared value, not a 500 (#1356).
    *******************************************************************************/
   @Test
   void testNullFieldValue()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .multiPartContent()
         .field("recordsParam", "recordIds")
         .field("recordIds", "2,3")
         .field("values", new JSONObject()
            .put("greetingPrefix", JSONObject.NULL)
            .put("greetingSuffix", "Jude")
            .toString())
         .asString();

      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("COMPLETE", jsonObject.getString("type"), jsonObject.toString());
   }



   /*******************************************************************************
    ** The step operation declares the isStepBack query parameter it honors
    ** (#1347).
    *******************************************************************************/
   @Test
   void testStepDeclaresIsStepBack()
   {
      Parameter isStepBack = new ProcessStepSpecV1().defineRequestParameters().stream()
         .filter(parameter -> "isStepBack".equals(parameter.getName()))
         .findFirst().orElseThrow();
      assertEquals("query", isStepBack.getIn());
      assertEquals("boolean", isStepBack.getSchema().getType());
      assertThat(isStepBack.getRequired()).isNotEqualTo(Boolean.TRUE);
   }



   /*******************************************************************************
    ** A process whose first step is a form keeps the record selection made at
    ** init: its backend step, run on the later step call, gets those records
    ** (#1366).
    *******************************************************************************/
   @Test
   void testInitSelectionReachesStepAfterForm()
   {
      String processPath = getBaseUrlAndPath() + "/processes/" + TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE;
      HttpResponse<String> initResponse = Unirest.post(processPath + "/init")
         .cookie("sessionId", "v1-process-session")
         .multiPartContent()
         .field("recordsParam", "recordIds")
         .field("recordIds", "1,2")
         .asString();
      assertEquals(200, initResponse.getStatus(), initResponse.getBody());
      JSONObject initJson = JsonUtils.toJSONObject(initResponse.getBody());
      assertEquals("setup", initJson.getString("nextStep"), initResponse.getBody());
      String processUUID = initJson.getString("processUUID");

      HttpResponse<String> stepResponse = Unirest.post(processPath + "/" + processUUID + "/step/setup")
         .cookie("sessionId", "v1-process-session")
         .multiPartContent()
         .field("values", new JSONObject().put("greetingPrefix", "Hi").put("greetingSuffix", "!").toString())
         .asString();
      assertEquals(200, stepResponse.getStatus(), stepResponse.getBody());
      JSONObject stepJson = JsonUtils.toJSONObject(stepResponse.getBody());
      assertThat(stepJson.has("error")).as(stepResponse.getBody()).isFalse();
      assertEquals("results", stepJson.getString("nextStep"), stepResponse.getBody());
      assertEquals(2, stepJson.getJSONObject("values").getInt("noOfPeopleGreeted"));
   }

}
