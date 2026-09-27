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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.executors.ProcessRecordsExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.ProcessStatusExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessRecordsInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessStatusInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ProcessInitOrStepOrStatusResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ProcessRecordsResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** The v1 process routes honor a table variant (as the v1 table routes do):
 ** init, step and cancel run against the requested variant, and the records
 ** and status routes put it in the session.
 *******************************************************************************/
class ProcessTableVariantSpecV1Test extends SpecTestBase
{
   private static final String PROCESS_NAME = "variantDataProcess";
   private static final String SESSION_ID   = "v1-variant-session";

   ////////////////////////////////////////////////////////////////////////
   // how many variant records the process's cancel step read (-1: none) //
   ////////////////////////////////////////////////////////////////////////
   private static final AtomicInteger CANCEL_STEP_COUNT = new AtomicInteger(-1);

   private static final String VARIANT_PEOPLE  = "{\"type\":\"" + TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS + "\",\"id\":1}";
   private static final String VARIANT_PLANETS = "{\"type\":\"" + TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS + "\",\"id\":\"2\"}";



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ProcessInitSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return List.of(new ProcessStepSpecV1(), new ProcessRecordsSpecV1(), new ProcessStatusSpecV1(), new ProcessCancelSpecV1());
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
    ** Add a process that reads the variant-backed table in a backend step before
    ** and after a frontend step.
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = super.defineQInstance();
      qInstance.addProcess(new QProcessMetaData()
         .withName(PROCESS_NAME)
         .withTableName(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA)
         .withStep(new QBackendStepMetaData()
            .withName("load")
            .withCode(new QCodeReferenceLambda<BackendStep>((runBackendStepInput, runBackendStepOutput) ->
            {
               List<QRecord> records = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA)).getRecords();
               runBackendStepOutput.setRecords(records);
               runBackendStepOutput.addValue("loadCount", records.size());
            })))
         .withStep(new QFrontendStepMetaData()
            .withName("review"))
         .withStep(new QBackendStepMetaData()
            .withName("reload")
            .withCode(new QCodeReferenceLambda<BackendStep>((runBackendStepInput, runBackendStepOutput) ->
               runBackendStepOutput.addValue("reloadCount", new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA)).getRecords().size()))))
         .withStep(new QFrontendStepMetaData()
            .withName("result"))
         .withCancelStep(new QBackendStepMetaData()
            .withName("cancel")
            .withCode(new QCodeReferenceLambda<BackendStep>((runBackendStepInput, runBackendStepOutput) ->
               CANCEL_STEP_COUNT.set(new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA)).getRecords().size())))));
      return (qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
      CANCEL_STEP_COUNT.set(-1);
   }



   /*******************************************************************************
    ** Init runs its backend step against the variant given as a form field or
    ** as a query parameter; without one, the step fails (as a table read would).
    *******************************************************************************/
   @Test
   void testInitUsesVariant() throws QException
   {
      insertVariantData();

      JSONObject withoutVariant = JsonUtils.toJSONObject(Unirest.post(getBaseUrlAndPath() + "/processes/" + PROCESS_NAME + "/init")
         .cookie("sessionId", SESSION_ID)
         .multiPartContent().field("stepTimeoutMillis", "10000").asString().getBody());
      assertEquals("ERROR", withoutVariant.getString("type"));

      JSONObject formVariant = JsonUtils.toJSONObject(Unirest.post(getBaseUrlAndPath() + "/processes/" + PROCESS_NAME + "/init")
         .cookie("sessionId", SESSION_ID)
         .multiPartContent().field("stepTimeoutMillis", "10000").field("tableVariant", VARIANT_PLANETS).asString().getBody());
      assertEquals("COMPLETE", formVariant.getString("type"), formVariant.toString());
      assertEquals(3, formVariant.getJSONObject("values").getInt("loadCount"));

      JSONObject queryVariant = JsonUtils.toJSONObject(Unirest.post(getBaseUrlAndPath() + "/processes/" + PROCESS_NAME + "/init")
         .cookie("sessionId", SESSION_ID)
         .queryString("tableVariant", VARIANT_PEOPLE)
         .multiPartContent().field("stepTimeoutMillis", "10000").asString().getBody());
      assertEquals("COMPLETE", queryVariant.getString("type"), queryVariant.toString());
      assertEquals(2, queryVariant.getJSONObject("values").getInt("loadCount"));

      //////////////////////////////////////////////////////////////////////
      // the records route returns the records init read from the variant //
      //////////////////////////////////////////////////////////////////////
      String processUUID = formVariant.getString("processUUID");
      HttpResponse<String> recordsResponse = Unirest.get(getBaseUrlAndPath() + "/processes/" + PROCESS_NAME + "/" + processUUID + "/records")
         .cookie("sessionId", SESSION_ID)
         .queryString("tableVariant", VARIANT_PLANETS)
         .asString();
      assertEquals(200, recordsResponse.getStatus(), recordsResponse.getBody());
      JSONArray records = JsonUtils.toJSONObject(recordsResponse.getBody()).getJSONArray("records");
      assertEquals(3, records.length());
      assertEquals("Mars", records.getJSONObject(0).getJSONObject("values").getString("name"));
   }



   /*******************************************************************************
    ** A step runs its backend step against the variant it is given - which must
    ** be the variant the process was started with (its state belongs to that
    ** variant).
    *******************************************************************************/
   @Test
   void testStepUsesVariant() throws QException
   {
      insertVariantData();

      JSONObject init = JsonUtils.toJSONObject(Unirest.post(getBaseUrlAndPath() + "/processes/" + PROCESS_NAME + "/init")
         .cookie("sessionId", SESSION_ID)
         .multiPartContent().field("stepTimeoutMillis", "10000").field("tableVariant", VARIANT_PLANETS).asString().getBody());
      assertEquals(3, init.getJSONObject("values").getInt("loadCount"));
      String processUUID = init.getString("processUUID");
      String stepUrl     = getBaseUrlAndPath() + "/processes/" + PROCESS_NAME + "/" + processUUID + "/step/review";

      JSONObject otherVariant = JsonUtils.toJSONObject(Unirest.post(stepUrl)
         .cookie("sessionId", SESSION_ID)
         .multiPartContent().field("stepTimeoutMillis", "10000").field("tableVariant", VARIANT_PEOPLE).asString().getBody());
      assertEquals("ERROR", otherVariant.getString("type"));
      assertThat(otherVariant.getString("error")).contains("Permission denied for process state");

      JSONObject step = JsonUtils.toJSONObject(Unirest.post(stepUrl)
         .cookie("sessionId", SESSION_ID)
         .queryString("tableVariant", VARIANT_PLANETS)
         .multiPartContent().field("stepTimeoutMillis", "10000").asString().getBody());
      assertEquals("COMPLETE", step.getString("type"), step.toString());
      assertEquals(3, step.getJSONObject("values").getInt("reloadCount"));
   }



   /*******************************************************************************
    ** Cancel (and the process's cancel step) runs with the variant the process
    ** started with; another variant, or none, is denied the process's state
    ** before the cancel step can run.
    *******************************************************************************/
   @Test
   void testCancelUsesVariant() throws QException
   {
      insertVariantData();

      JSONObject init = JsonUtils.toJSONObject(Unirest.post(getBaseUrlAndPath() + "/processes/" + PROCESS_NAME + "/init")
         .cookie("sessionId", SESSION_ID)
         .multiPartContent().field("stepTimeoutMillis", "10000").field("tableVariant", VARIANT_PLANETS).asString().getBody());
      assertEquals(3, init.getJSONObject("values").getInt("loadCount"));
      String cancelUrl = getBaseUrlAndPath() + "/processes/" + PROCESS_NAME + "/" + init.getString("processUUID") + "/cancel";

      HttpResponse<String> otherVariant = Unirest.post(cancelUrl)
         .cookie("sessionId", SESSION_ID)
         .queryString("tableVariant", VARIANT_PEOPLE)
         .asString();
      assertEquals(403, otherVariant.getStatus(), otherVariant.getBody());
      assertThat(JsonUtils.toJSONObject(otherVariant.getBody()).getString("error")).contains("Permission denied for process state");

      HttpResponse<String> noVariant = Unirest.post(cancelUrl)
         .cookie("sessionId", SESSION_ID)
         .asString();
      assertEquals(403, noVariant.getStatus(), noVariant.getBody());
      assertEquals(-1, CANCEL_STEP_COUNT.get());

      HttpResponse<String> matchingVariant = Unirest.post(cancelUrl)
         .cookie("sessionId", SESSION_ID)
         .queryString("tableVariant", VARIANT_PLANETS)
         .asString();
      assertEquals(200, matchingVariant.getStatus(), matchingVariant.getBody());
      assertEquals("{}", matchingVariant.getBody());
      assertEquals(3, CANCEL_STEP_COUNT.get());
   }



   /*******************************************************************************
    ** The records and status executors put their input's variant in the session
    ** (before reading the process's state).
    *******************************************************************************/
   @Test
   void testRecordsAndStatusPutVariantInSession() throws QException
   {
      TableVariant tableVariant = new TableVariant().withType(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS).withId("2");

      QContext.init(serverQInstance, new QSession());
      assertThrows(QException.class, () -> new ProcessRecordsExecutor().execute(new ProcessRecordsInput()
         .withProcessName(PROCESS_NAME).withProcessUUID(UUID.randomUUID().toString()).withTableVariant(tableVariant), new ProcessRecordsResponseV1()));
      assertEquals(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, "2"), QContext.getQSession().getBackendVariants());

      QContext.init(serverQInstance, new QSession());
      new ProcessStatusExecutor().execute(new ProcessStatusInput()
         .withProcessName(PROCESS_NAME).withProcessUUID(UUID.randomUUID().toString()).withJobUUID(UUID.randomUUID().toString()).withTableVariant(tableVariant), new ProcessInitOrStepOrStatusResponseV1());
      assertEquals(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, "2"), QContext.getQSession().getBackendVariants());
   }



   /*******************************************************************************
    ** Two variant options, with 2 records (people) and 3 records (planets).
    *******************************************************************************/
   private void insertVariantData() throws QException
   {
      QContext.init(serverQInstance, new QSystemUserSession());
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "People"),
         new QRecord().withValue("id", 2).withValue("name", "Planets"))));

      QContext.getQSession().setBackendVariants(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, 1));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA).withRecords(List.of(
         new QRecord().withValue("name", "Tom"),
         new QRecord().withValue("name", "Sally"))));

      QContext.getQSession().setBackendVariants(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, 2));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA).withRecords(List.of(
         new QRecord().withValue("name", "Mars"),
         new QRecord().withValue("name", "Jupiter"),
         new QRecord().withValue("name", "Saturn"))));
      QContext.clear();
   }

}
