/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetHtmlLine;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.NoCodeWidgetFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Interactive process protocols over the owned sample HTTP server and database.
 *******************************************************************************/
class SampleInteractiveProcessTest
{
   private final String owner = UUID.randomUUID().toString();
   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final CountDownLatch entered = new CountDownLatch(1);
   private final CountDownLatch release = new CountDownLatch(1);
   private HttpClient client;
   private SampleJavalinServer server;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(SampleSavedReportContractTest.ReportUser.class));
      QFrontendStepMetaData setup = instance.getProcess("greetInteractive").getFrontendStep("setup");
      setup.withComponent(new QFrontendComponentMetaData().withType(QComponentType.HELP_TEXT).withValue("text", "Choose a greeting for the selected people."));
      setup.setBackStepName("loadInitialRecords");
      setup.withComponent(new QFrontendComponentMetaData().withType(QComponentType.WIDGET).withValue("widgetName", "PersonsByCreateDateBarChart"));
      setup.withComponent(new NoCodeWidgetFrontendComponentMetaData().withOutput(new WidgetHtmlLine().withVelocityTemplate("<p>Owned greeting preview</p>")));
      instance.addProcess(new QProcessMetaData().withName("ownedTypedForm")
         .withStep(new QFrontendStepMetaData().withName("input").withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM))
            .withFormField(new QFieldMetaData("quantity", QFieldType.INTEGER).withIsRequired(true)))
         .withStep(new QBackendStepMetaData().withName("check").withInputData(new QFunctionInputMetaData()
            .withField(new QFieldMetaData("quantity", QFieldType.INTEGER).withIsRequired(true)))
            .withCode(new QCodeReferenceLambda<BackendStep>((in, out) -> out.addValue("quantity", in.getValueInteger("quantity"))))));
      instance.addProcess(new QProcessMetaData().withName("ownedProgress")
         .withStep(new QBackendStepMetaData().withName("wait").withCode(new QCodeReferenceLambda<BackendStep>((in, out) ->
         {
            in.getAsyncJobCallback().updateStatus("Reading sample people", 1, 2);
            entered.countDown();
            try
            {
               if(!release.await(10, TimeUnit.SECONDS))
               {
                  throw new QException("Owned progress fixture timed out");
               }
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
               throw new QException("Owned progress fixture interrupted", e);
            }
            out.addValue("count", CountAction.execute("person", null));
            in.getAsyncJobCallback().updateStatus("Read complete", 2, 2);
         }))));
      for(String name : List.of("ownedUploadInit", "ownedUploadStep"))
      {
         QProcessMetaData upload = new QProcessMetaData().withName(name);
         if(name.endsWith("Step"))
         {
            upload.withStep(new QFrontendStepMetaData().withName("input"));
         }
         upload.withStep(new QBackendStepMetaData().withName("inspect").withCode(new QCodeReferenceLambda<BackendStep>((in, out) ->
            out.addValue("uploadedFileCount", in.getValue("files") instanceof List<?> files ? files.size() : 0))));
         instance.addProcess(upload);
      }
      server = new SampleJavalinServer(new SampleMetaDataProvider()
      {



         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      });
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      server.start();
      client = HttpClient.newHttpClient();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void stop()
   {
      release.countDown();
      if(client != null)
      {
         client.close();
      }
      if(server != null)
      {
         server.stop();
      }
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSelectedGreetingRoundTripAndComponents() throws Exception
   {
      List<String> before = people();
      JSONObject metadata = json(request("/qqq/v1/metaData/process/greetInteractive"));
      JSONArray steps = metadata.getJSONArray("frontendSteps");
      JSONArray components = steps.getJSONObject(0).getJSONArray("components");
      assertEquals(List.of("EDIT_FORM", "HELP_TEXT", "WIDGET", "HTML"), components.toList().stream()
         .map(value -> ((Map<?, ?>) value).get("type")).toList());
      JSONObject widget = json(request("/widget/PersonsByCreateDateBarChart"));
      assertEquals(List.of(17, 42, 47, 0, 64), widget.getJSONObject("chartData").getJSONArray("datasets").getJSONObject(0).getJSONArray("data").toList());
      assertTrue(steps.getJSONObject(1).toString().contains("VIEW_FORM"));
      assertTrue(steps.getJSONObject(1).toString().contains("RECORD_LIST"));
      JSONObject initial = init("greetInteractive");
      assertEquals("setup", initial.getString("nextStep"));
      assertEquals("<p>Owned greeting preview</p>", initial.getJSONObject("values").getString("setup.html"));
      String run = initial.getString("processUUID");
      JSONObject result = json(post("/qqq/v1/processes/greetInteractive/" + run + "/step/setup", Map.of("values", "{\"greetingPrefix\":\"Hello\",\"greetingSuffix\":\"QQQ\"}")));
      assertEquals("results", result.getString("nextStep"));
      assertEquals(2, result.getJSONObject("values").getInt("noOfPeopleGreeted"));
      JSONObject records = json(request("/processes/greetInteractive/" + run + "/records"));
      assertEquals(2, records.getInt("totalRecords"));
      assertEquals(List.of("Hello Avery QQQ", "Hello Casey QQQ"), records.getJSONArray("records").toList().stream()
         .map(value -> ((Map<?, ?>) ((Map<?, ?>) value).get("values")).get("greetingMessage")).sorted().toList());
      JSONObject complete = json(post("/qqq/v1/processes/greetInteractive/" + run + "/step/results", Map.of()));
      assertFalse(complete.has("nextStep"), complete.toString());
      assertEquals(before, people());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownProcessRunAndExpiredStateAreRejected() throws Exception
   {
      failure(post("/qqq/v1/processes/missingProcess/init", Map.of()));
      failure(post("/qqq/v1/processes/greetInteractive/" + UUID.randomUUID() + "/step/setup", Map.of()));
      String run = init("greetInteractive").getString("processUUID");
      HttpResponse<String> lostSession = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port()
         + "/qqq/v1/processes/greetInteractive/" + run + "/step/setup"))
         .header("Content-Type", "application/x-www-form-urlencoded").timeout(Duration.ofSeconds(15))
         .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
      failure(lostSession);
      RunProcessAction.getStateProvider().remove(new UUIDAndTypeStateKey(UUID.fromString(run), StateType.PROCESS_STATUS));
      failure(post("/qqq/v1/processes/greetInteractive/" + run + "/step/setup", Map.of()));
      assertEquals(5, people().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingAndInvalidFormValuesAndBackendException() throws Exception
   {
      for(String values : List.of("{}", "{\"quantity\":\"not-a-number\"}"))
      {
         String run = init("ownedTypedForm").getString("processUUID");
         failure(post("/qqq/v1/processes/ownedTypedForm/" + run + "/step/input", Map.of("values", values)));
      }
      String valid = init("ownedTypedForm").getString("processUUID");
      JSONObject result = json(post("/qqq/v1/processes/ownedTypedForm/" + valid + "/step/input", Map.of("values", "{\"quantity\":3}")));
      assertEquals(3, result.getJSONObject("values").getInt("quantity"));
      String greeting = init("greetInteractive").getString("processUUID");
      failure(post("/qqq/v1/processes/greetInteractive/" + greeting + "/step/setup", Map.of("values", "{\"greetingSuffix\":\"there\"}")));
      assertEquals(5, people().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProgressStatusAndCompletion() throws Exception
   {
      JSONObject started = json(post("/qqq/v1/processes/ownedProgress/init", Map.of("stepTimeoutMillis", "1")));
      assertEquals("JOB_STARTED", started.getString("type"));
      assertTrue(entered.await(3, TimeUnit.SECONDS));
      String path = "/qqq/v1/processes/ownedProgress/" + started.getString("processUUID") + "/status/" + started.getString("jobUUID");
      JSONObject running = json(request(path));
      assertEquals("RUNNING", running.getString("type"));
      assertEquals("Reading sample people", running.getString("message"));
      assertEquals(1, running.getInt("current"));
      assertEquals(2, running.getInt("total"));
      release.countDown();
      Long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      JSONObject result;
      do
      {
         result = json(request(path));
         if(!"RUNNING".equals(result.optString("type")))
         {
            break;
         }
         Thread.sleep(20);
      }
      while(System.nanoTime() < deadline);
      assertEquals("COMPLETE", result.getString("type"));
      assertEquals(5, result.getJSONObject("values").getInt("count"));
   }



   /*******************************************************************************
    ** The linear API accepts a resume position; an unknown target runs no work.
    *******************************************************************************/
   @Test
   void testLegacyBackAndUnknownResumeDoNotRunWork() throws Exception
   {
      List<String> before = people();
      String run = init("greetInteractive").getString("processUUID");
      JSONObject back = json(post("/processes/greetInteractive/" + run + "/step/loadInitialRecords?isStepBack=true", Map.of()));
      assertEquals("setup", back.getString("nextStep"));
      JSONObject unknown = json(post("/qqq/v1/processes/greetInteractive/" + run + "/step/missingStep", Map.of()));
      assertFalse(unknown.getJSONObject("values").has("noOfPeopleGreeted"), unknown.toString());
      assertEquals(before, people());
   }



   /*******************************************************************************
    ** v1 process routes accept multipart files the way the legacy routes do: they
    ** stream them to the javalin uploaded-file archive table. This instance has no
    ** archive table, so a v1 upload is refused with that reason and nothing is run.
    *******************************************************************************/
   @Test
   void testVersionedUploadsWithoutAnArchiveTableAreRefused() throws Exception
   {
      List<String> before = people();
      for(String name : List.of("ownedUploadInit", "ownedUploadStep"))
      {
         String path = "/qqq/v1/processes/" + name + "/init";
         if(name.endsWith("Step"))
         {
            path = "/qqq/v1/processes/" + name + "/" + init(name).getString("processUUID") + "/step/input";
         }
         String boundary = "OwnedFile" + UUID.randomUUID();
         String body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"owned.txt\"\r\nContent-Type: text/plain\r\n\r\nowned sample bytes\r\n--" + boundary + "--\r\n";
         HttpResponse<String> response = client.send(builder(path).header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
         failure(response);
         assertTrue(response.body().contains("Cannot accept file uploads"), response.body());
      }
      assertEquals(before, people());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject init(String name) throws Exception
   {
      return json(post("/qqq/v1/processes/" + name + "/init", Map.of("recordsParam", "recordIds", "recordIds", "1,3", "stepTimeoutMillis", "10000")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> post(String path, Map<String, String> fields) throws Exception
   {
      String boundary = "OwnedProcess" + UUID.randomUUID();
      StringBuilder body = new StringBuilder();
      for(var field : fields.entrySet())
      {
         body.append("--").append(boundary).append("\r\nContent-Disposition: form-data; name=\"").append(field.getKey()).append("\"\r\n\r\n").append(field.getValue()).append("\r\n");
      }
      body.append("--").append(boundary).append("--\r\n");
      return client.send(builder(path).header("Content-Type", "multipart/form-data; boundary=" + boundary)
         .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> request(String path) throws Exception
   {
      return client.send(builder(path).GET().build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpRequest.Builder builder(String path)
   {
      return HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + path))
         .header("Cookie", "sessionId=" + owner).timeout(Duration.ofSeconds(15));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject json(HttpResponse<String> response)
   {
      assertEquals(200, response.statusCode(), response.body());
      JSONObject result = new JSONObject(response.body());
      assertFalse(result.has("error") || "ERROR".equals(result.optString("type")), result.toString());
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void failure(HttpResponse<String> response)
   {
      JSONObject result = new JSONObject(response.body());
      assertTrue(response.statusCode() >= 400 || result.has("error") || "ERROR".equals(result.optString("type")), result.toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> people() throws Exception
   {
      List<String> people = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id,first_name,last_name FROM person ORDER BY id"))
      {
         while(rows.next())
         {
            people.add(rows.getInt(1) + ":" + rows.getString(2) + ":" + rows.getString(3));
         }
      }
      return people;
   }
}
