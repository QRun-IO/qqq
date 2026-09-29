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

package com.kingsrook.sampleapp;


import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.api.actions.ApiImplementation;
import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
import com.kingsrook.qqq.api.javalin.QJavalinApiHandler;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.HttpApiResponse;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessCustomizers;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInput;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInputFieldsContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaData;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessObjectOutput;
import com.kingsrook.qqq.api.model.metadata.processes.PreRunApiProcessCustomizer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiAssociationMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobState;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.TableBasedAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.TableBasedAuthenticationModule;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.SimpleStateKey;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.eclipse.jetty.http.HttpStatus;
import org.h2.tools.RunScript;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Application API transport acceptance through real loopback Javalin routes,
 ** native authentication and the sample Person table in a private H2 database.
 *******************************************************************************/
class SampleApplicationApiHttpAcceptanceTest
{
   private static final String VERSION = "2026.01";
   private static final Map<String, ProcessControl> CONTROLS = new ConcurrentHashMap<>();
   private final ProcessControl control = new ProcessControl();
   private final String sessionId = UUID.randomUUID().toString();
   private CapturedContext previousContext;
   private Map<String, Serializable> previousObjects;
   private QInstance previousServerInstance;
   private Connection anchor;
   private Javalin server;
   private HttpClient client;
   private String apiName;
   private String baseUrl;



   /*******************************************************************************
    ** Route registration and session resolution are the production module's code.
    ** Only the database, application metadata and ephemeral listener are owned here.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousContext = QContext.capture();
      previousObjects = QContext.getObjects();
      previousServerInstance = QJavalinImplementation.getQInstance();
      QContext.setObjects(new LinkedHashMap<>());
      ConnectionManager.resetConnectionProviders();
      String jdbcUrl = "jdbc:h2:mem:api_http_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=0";
      anchor = DriverManager.getConnection(jdbcUrl, "sa", "");
      try(InputStreamReader reader = new InputStreamReader(SampleMetaDataProvider.class.getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(anchor, reader);
      }
      try(Statement statement = anchor.createStatement())
      {
         statement.execute("CREATE TABLE http_user (id INT PRIMARY KEY, create_date TIMESTAMP DEFAULT now(), modify_date TIMESTAMP DEFAULT now(), username VARCHAR(100), password_hash VARCHAR(200), full_name VARCHAR(100))");
         statement.execute("CREATE TABLE http_session (id VARCHAR(40) PRIMARY KEY, create_date TIMESTAMP DEFAULT now(), modify_date TIMESTAMP DEFAULT now(), user_id INT, access_timestamp TIMESTAMP DEFAULT now())");
         statement.execute("INSERT INTO http_user (id,username,full_name) VALUES (1,'sample-http','Sample HTTP User')");
      }
      try(PreparedStatement statement = anchor.prepareStatement("INSERT INTO http_session (id,user_id,access_timestamp) VALUES (?,1,?)"))
      {
         statement.setString(1, sessionId);
         statement.setObject(2, LocalDateTime.now(ZoneOffset.UTC));
         statement.executeUpdate();
      }
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.getSupplementalMetaData().remove(EsbInstanceMetaData.NAME);
      instance.getTables().values().forEach(table ->
      {
         if(table.getSupplementalMetaData() != null)
         {
            table.getSupplementalMetaData().remove(EsbTableMetaData.TYPE);
         }
      });
      instance.getProcesses().values().forEach(process ->
      {
         if(process.getSupplementalMetaData() != null)
         {
            process.getSupplementalMetaData().remove(EsbProcessMetaData.TYPE);
         }
      });
      RDBMSBackendMetaData backend = SampleMetaDataProvider.defineRdbmsBackend().withName("apiHttpDatabase").withJdbcUrl(jdbcUrl);
      instance.addBackend(backend);
      for(String tableName : List.of("person", "pet", "petNote"))
      {
         instance.getTable(tableName).setBackendName(backend.getName());
      }
      TableBasedAuthenticationMetaData authentication = new TableBasedAuthenticationMetaData().withUserTableName("httpUser").withSessionTableName("httpSession");
      authentication.setName("sampleHttpAuthentication");
      instance.addTable(authentication.defineStandardUserTable(backend.getName()));
      instance.addTable(authentication.defineStandardSessionTable(backend.getName()));
      for(String tableName : List.of("httpUser", "httpSession"))
      {
         QTableMetaData table = instance.getTable(tableName);
         table.withBackendDetails(new RDBMSTableBackendDetails().withTableName(tableName.equals("httpUser") ? "http_user" : "http_session"));
         for(QFieldMetaData field : table.getFields().values())
         {
            field.setBackendName(field.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT));
         }
      }
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), authentication);
      apiName = "sampleHttp" + UUID.randomUUID();
      CONTROLS.put(apiName, control);
      ApiInstanceMetaData api = new ApiInstanceMetaData().withName(apiName).withPath("/sample-http/").withLabel("Sample HTTP")
         .withDescription("Owned loopback acceptance").withContactEmail("sample@example.invalid")
         .withCurrentVersion(new APIVersion(VERSION)).withSupportedVersions(List.of(new APIVersion(VERSION)));
      instance.withSupplementalMetaData(new ApiInstanceMetaDataContainer().withApiInstanceMetaData(api));
      instance.getTable("person").withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(apiName,
         new ApiTableMetaData().withInitialVersion(VERSION).withApiAssociationMetaData("pets", new ApiAssociationMetaData().withIsExcluded(true))));
      for(HttpMethod method : List.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE))
      {
         addProcess(instance, "read" + method, method, ApiProcessMetaData.AsyncMode.NEVER, false);
      }
      addProcess(instance, "readAsync", HttpMethod.POST, ApiProcessMetaData.AsyncMode.ALWAYS, false);
      addProcess(instance, "readText", HttpMethod.GET, ApiProcessMetaData.AsyncMode.NEVER, true);
      addProcess(instance, "failProcess", HttpMethod.POST, ApiProcessMetaData.AsyncMode.NEVER, false);
      addProcess(instance, "deniedProcess", HttpMethod.POST, ApiProcessMetaData.AsyncMode.NEVER, false);
      instance.getProcess("deniedProcess").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      QContext.init(instance, new QSession());
      ApiImplementation.clearCaches();
      GetTableApiFieldsAction.clearCaches();
      new QInstanceValidator().revalidate(instance);
      QJavalinImplementation.setQInstance(instance);
      QJavalinApiHandler handler = new QJavalinApiHandler(instance);
      server = Javalin.create(config ->
      {
         config.jetty.host = "127.0.0.1";
         config.routes.before(context -> context.contentType("application/json"));
         config.routes.apiBuilder(handler.getRoutes());
         config.routes.after(QJavalinImplementation::clearQContext);
      }).start(0);
      baseUrl = "http://127.0.0.1:" + server.port() + "/sample-http/" + VERSION;
      client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
   }



   /*******************************************************************************
    ** Release any blocked job before stopping HTTP and removing only owned state.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      control.release.countDown();
      try
      {
         if(client != null)
         {
            client.close();
         }
         if(server != null)
         {
            server.stop();
         }
         for(String id : control.processIds)
         {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while(new AsyncJobManager().getJobStatus(id).map(status -> status.getState() != AsyncJobState.COMPLETE && status.getState() != AsyncJobState.ERROR).orElse(false))
            {
               assertTrue(System.nanoTime() < deadline, "Owned async job did not terminate");
               Thread.sleep(10);
            }
            RunProcessAction.getStateProvider().remove(new UUIDAndTypeStateKey(UUID.fromString(id), StateType.PROCESS_STATUS));
            InMemoryStateProvider.getInstance().remove(new UUIDAndTypeStateKey(UUID.fromString(id), StateType.ASYNC_JOB_STATUS));
         }
         TableBasedAuthenticationModule.getStateProvider().remove(new SimpleStateKey<>(sessionId));
         TableBasedAuthenticationModule.getStateProvider().remove(new SimpleStateKey<>("tableBasedAuthActivity:" + sessionId));
      }
      finally
      {
         try
         {
            if(anchor != null)
            {
               try(Statement statement = anchor.createStatement())
               {
                  statement.execute("SHUTDOWN");
               }
               finally
               {
                  anchor.close();
               }
            }
         }
         finally
         {
            if(apiName != null)
            {
               CONTROLS.remove(apiName);
            }
            QJavalinImplementation.setQInstance(previousServerInstance);
            new QJavalinApiHandler(previousServerInstance);
            ApiImplementation.clearCaches();
            GetTableApiFieldsAction.clearCaches();
            ConnectionManager.resetConnectionProviders();
            QContext.clear();
            QContext.init(previousContext);
            QContext.setObjects(previousObjects);
         }
      }
   }



   /*******************************************************************************
    ** Counts belong to query; pagination and filtering must reach native data.
    *******************************************************************************/
   @Test
   void testGetAndQuery() throws Exception
   {
      JSONObject person = json(send("GET", "/person/1", null), 200);
      assertEquals("Avery", person.getString("firstName"));
      assertEquals("Sample", person.getString("lastName"));
      JSONObject page = json(send("GET", "/person/query?pageSize=2&orderBy=id%20ASC&includeCount=true", null), 200);
      assertEquals(5, page.getInt("count"));
      assertEquals(2, page.getJSONArray("records").length());
      assertEquals(1, page.getJSONArray("records").getJSONObject(0).getInt("id"));
      assertEquals(2, page.getJSONArray("records").getJSONObject(1).getInt("id"));
      JSONObject filtered = json(send("GET", "/person/query?firstName=%3DAvery&includeCount=false", null), 200);
      assertFalse(filtered.has("count"));
      assertEquals(1, filtered.getJSONArray("records").length());
      assertEquals(1, filtered.getJSONArray("records").getJSONObject(0).getInt("id"));
      assertEquals(0, json(send("GET", "/person/query?firstName=%3DNobody", null), 200).getInt("count"));
   }



   /*******************************************************************************
    ** HTTP success is checked against a separate JDBC persistence oracle.
    *******************************************************************************/
   @Test
   void testInsertPatchAndDelete() throws Exception
   {
      List<String> before = people();
      JSONObject inserted = json(send("POST", "/person/", "{\"firstName\":\"Riley\",\"lastName\":\"Sample\",\"email\":\"riley@example.invalid\"}"), 201);
      Integer id = inserted.getInt("id");
      assertEquals(before.size() + 1, people().size());
      assertTrue(people().contains(id + ":Riley:Sample:riley@example.invalid:null"));
      HttpResponse<String> patched = send("PATCH", "/person/" + id, "{\"firstName\":\"River\",\"daysWorked\":42}");
      assertEquals(204, patched.statusCode());
      assertEquals("", patched.body());
      assertTrue(people().contains(id + ":River:Sample:riley@example.invalid:42"));
      assertEquals("River", json(send("GET", "/person/" + id, null), 200).getString("firstName"));
      HttpResponse<String> deleted = send("DELETE", "/person/" + id, null);
      assertEquals(204, deleted.statusCode());
      assertEquals("", deleted.body());
      assertEquals(before, people());
      assertError(send("GET", "/person/" + id, null), 404);
   }



   /*******************************************************************************
    ** Bulk responses preserve per-record outcomes and partial success in H2.
    *******************************************************************************/
   @Test
   void testBulkInsertUpdateDelete() throws Exception
   {
      List<String> before = people();
      HttpResponse<String> response = send("POST", "/person/bulk", """
         [{"firstName":"Robin","lastName":"Sample","email":"robin@example.invalid"},
          {"firstName":"Reese","lastName":"Sample","email":"reese@example.invalid"},
          {"firstName":"MissingEmail","lastName":"Sample"}]
         """);
      assertEquals(207, response.statusCode());
      JSONArray inserted = new JSONArray(response.body());
      assertEquals(3, inserted.length());
      assertEquals(201, inserted.getJSONObject(0).getInt("statusCode"));
      assertEquals(201, inserted.getJSONObject(1).getInt("statusCode"));
      assertEquals(400, inserted.getJSONObject(2).getInt("statusCode"));
      assertTrue(inserted.getJSONObject(2).getString("error").contains("Email"));
      Integer first = inserted.getJSONObject(0).getInt("id");
      Integer second = inserted.getJSONObject(1).getInt("id");
      assertEquals(before.size() + 2, people().size());
      assertTrue(people().contains(first + ":Robin:Sample:robin@example.invalid:null"));
      assertTrue(people().contains(second + ":Reese:Sample:reese@example.invalid:null"));
      response = send("PATCH", "/person/bulk", """
         [{"id":%d,"daysWorked":17},{"id":%d,"daysWorked":18},
          {"daysWorked":19},{"id":999999,"daysWorked":20}]
         """.formatted(first, second));
      assertEquals(207, response.statusCode());
      JSONArray updated = new JSONArray(response.body());
      assertEquals(4, updated.length());
      assertEquals(204, updated.getJSONObject(0).getInt("statusCode"));
      assertEquals(first, updated.getJSONObject(0).getInt("id"));
      assertEquals(204, updated.getJSONObject(1).getInt("statusCode"));
      assertEquals(400, updated.getJSONObject(2).getInt("statusCode"));
      assertEquals(404, updated.getJSONObject(3).getInt("statusCode"));
      assertTrue(people().contains(first + ":Robin:Sample:robin@example.invalid:17"));
      assertTrue(people().contains(second + ":Reese:Sample:reese@example.invalid:18"));
      response = send("DELETE", "/person/bulk", "[%d,%d,999999]".formatted(first, second));
      assertEquals(207, response.statusCode());
      JSONArray deleted = new JSONArray(response.body());
      assertEquals(3, deleted.length());
      assertEquals(204, deleted.getJSONObject(0).getInt("statusCode"));
      assertEquals(first, deleted.getJSONObject(0).getInt("id"));
      assertEquals(204, deleted.getJSONObject(1).getInt("statusCode"));
      assertEquals(404, deleted.getJSONObject(2).getInt("statusCode"));
      assertEquals(before, people());
   }



   /*******************************************************************************
    ** Bad JSON, types and filters fail at HTTP without changing persisted records.
    *******************************************************************************/
   @Test
   void testBadRequestsAndUnknownRecords() throws Exception
   {
      List<String> before = people();
      assertError(send("POST", "/person/", "{"), 400);
      assertError(send("POST", "/person/", "{\"firstName\":\"Missing\",\"lastName\":\"Sample\"}"), 400);
      assertError(send("GET", "/person/query?unknownField=EQ%20x", null), 400);
      assertError(send("GET", "/person/query?firstName=EMPTY%20unexpected", null), 400);
      assertError(send("GET", "/person/query?pageSize=not-a-number", null), 400);
      for(String method : List.of("POST", "PATCH", "DELETE"))
      {
         assertError(send(method, "/person/bulk", "{"), 400);
      }
      assertError(send("GET", "/person/999999", null), 404);
      assertError(send("PATCH", "/person/999999", "{\"firstName\":\"Missing\"}"), 404);
      assertError(send("DELETE", "/person/999999", null), 404);
      assertError(send("DELETE", "/person/", null), 404);
      assertEquals(before, people());
   }



   /*******************************************************************************
    ** Invalid native field types are client errors before any persistence occurs.
    *******************************************************************************/
   @Test
   void testInvalidFieldTypes() throws Exception
   {
      List<String> before = people();
      assertTrue(assertError(send("PATCH", "/person/1", "{\"daysWorked\":\"not-a-number\"}"), 400).contains("daysWorked"));
      assertError(send("POST", "/person/", "{\"firstName\":\"Invalid\",\"lastName\":\"Sample\",\"email\":\"invalid@example.invalid\",\"daysWorked\":\"bad\"}"), 400);
      assertError(send("PATCH", "/person/bulk", "[{\"id\":1,\"daysWorked\":\"bad\"}]"), 400);
      assertError(send("POST", "/person/bulk", "[{\"firstName\":\"Invalid\",\"lastName\":\"Sample\",\"email\":\"invalid@example.invalid\",\"daysWorked\":\"bad\"}]"), 400);
      assertEquals(before, people());
   }



   /*******************************************************************************
    ** Native authentication, permissions and exception mapping cross HTTP.
    *******************************************************************************/
   @Test
   void testAuthenticationPermissionsAndServerError() throws Exception
   {
      List<String> before = people();
      assertEquals("The required authentication credentials were missing or invalid.",
         assertError(request("GET", "/person/1", null, null, "application/json"), 401));
      assertError(request("GET", "/person/1", null, "unknown-synthetic-session", "application/json"), 401);
      assertEquals("Avery", json(send("GET", "/person/1", null), 200).getString("firstName"));
      assertEquals("You do not have permission to access the requested resource.", assertError(send("POST", "/operations/deniedProcess", "{\"personId\":1}"), 403));
      assertEquals("Synthetic HTTP process failure", assertError(send("POST", "/operations/failProcess", "{\"personId\":1}"), 500));
      assertEquals(before, people());
   }



   /*******************************************************************************
    ** Every configured method executes the native step; the other four get 405.
    *******************************************************************************/
   @Test
   void testConfiguredProcessMethodsAnd405() throws Exception
   {
      for(String method : List.of("GET", "POST", "PUT", "PATCH", "DELETE"))
      {
         String path = "/operations/read" + method;
         Boolean queryInput = method.equals("GET") || method.equals("DELETE");
         JSONObject output = json(send(method, path + (queryInput ? "?personId=2" : ""), queryInput ? null : "{\"personId\":2}"), 200);
         assertEquals("Blair", output.getString("firstName"));
         assertEquals("read" + method, output.getString("processName"));
         Integer executions = control.processIds.size();
         for(String wrongMethod : List.of("GET", "POST", "PUT", "PATCH", "DELETE"))
         {
            if(!method.equals(wrongMethod))
            {
               assertError(send(wrongMethod, path + "?personId=1", null), 405);
            }
         }
         assertEquals(executions, control.processIds.size());
      }
   }



   /*******************************************************************************
    ** Process output customization controls the actual wire status, body and MIME
    ** header; native CORS and HTML error negotiation also survive the handler.
    *******************************************************************************/
   @Test
   void testResponseStatusAndHeaderTransformation() throws Exception
   {
      HttpResponse<String> response = send("GET", "/operations/readText?personId=1", null);
      assertEquals(201, response.statusCode());
      assertEquals("Person: Avery", response.body());
      assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("text/plain"));
      assertEquals("http://127.0.0.1", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
      assertEquals("Origin", response.headers().firstValue("Vary").orElseThrow());
      response = request("GET", "/person/999999", null, sessionId, "text/html");
      assertEquals(404, response.statusCode());
      assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("text/html"));
      assertTrue(response.body().startsWith("Error: "));
      assertTrue(response.body().contains("999999"));
   }



   /*******************************************************************************
    ** The job stays pending until explicitly released, then yields the real result.
    *******************************************************************************/
   @Test
   void testAsyncStatus() throws Exception
   {
      JSONObject accepted = json(send("POST", "/operations/readAsync", "{\"personId\":3}"), 202);
      String jobId = accepted.getString("jobId");
      assertTrue(control.started.await(5, TimeUnit.SECONDS));
      assertTrue(control.processIds.contains(jobId));
      String path = "/operations/readAsync/status/" + jobId;
      assertEquals(jobId, json(send("GET", path, null), 202).getString("jobId"));
      control.release.countDown();
      HttpResponse<String> response;
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      do
      {
         response = send("GET", path, null);
         if(response.statusCode() != 202)
         {
            break;
         }
         Thread.sleep(10);
      }
      while(System.nanoTime() < deadline);
      JSONObject completed = json(response, 200);
      assertEquals("Casey", completed.getString("firstName"));
      assertEquals("readAsync", completed.getString("processName"));
   }



   /*******************************************************************************
    ** Ordinary API process metadata declares input locations and output adapters.
    *******************************************************************************/
   private void addProcess(QInstance instance, String name, HttpMethod method, ApiProcessMetaData.AsyncMode asyncMode, Boolean textOutput)
   {
      ApiProcessInputFieldsContainer fields = new ApiProcessInputFieldsContainer().withField(new QFieldMetaData("personId", QFieldType.INTEGER).withIsRequired(true));
      ApiProcessInput input = new ApiProcessInput();
      if(method == HttpMethod.GET || method == HttpMethod.DELETE)
      {
         input.withQueryStringParams(fields);
      }
      else
      {
         input.withRecordBodyParams(fields);
      }
      ApiProcessObjectOutput output = textOutput ? new PersonTextOutput() : new ApiProcessObjectOutput();
      output.withOutputField(new QFieldMetaData("firstName", QFieldType.STRING)).withOutputField(new QFieldMetaData("processName", QFieldType.STRING));
      instance.addProcess(new QProcessMetaData().withName(name)
         .withStep(new QBackendStepMetaData().withName("readPerson").withCode(new QCodeReference(ReadPersonStep.class)))
         .withSupplementalMetaData(new ApiProcessMetaDataContainer().withApiProcessMetaData(apiName, new ApiProcessMetaData()
            .withApiProcessName(name).withPath("operations").withInitialVersion(VERSION).withMethod(method).withAsyncMode(asyncMode)
            .withInput(input).withOutput(output).withCustomizer(ApiProcessCustomizers.PRE_RUN.getRole(), new QCodeReference(ReadPersonStep.class)))));
   }



   /*******************************************************************************
    ** The client uses a database-backed synthetic session, never a mock provider.
    *******************************************************************************/
   private HttpResponse<String> send(String method, String path, String body) throws Exception
   {
      return request(method, path, body, sessionId, "application/json");
   }



   /*******************************************************************************
    ** No redirects or external destinations are configured for these requests.
    *******************************************************************************/
   private HttpResponse<String> request(String method, String path, String body, String authentication, String accept) throws Exception
   {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10))
         .header("Accept", accept).header("Content-Type", "application/json").header("Origin", "http://127.0.0.1")
         .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
      if(authentication != null)
      {
         builder.header("Cookie", "sessionId=" + authentication);
      }
      return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    ** Assert the wire envelope before interpreting success JSON.
    *******************************************************************************/
   private JSONObject json(HttpResponse<String> response, Integer status)
   {
      assertEquals(status, response.statusCode());
      assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
      return new JSONObject(response.body());
   }



   /*******************************************************************************
    ** Error responses must be structured and carry a non-empty module message.
    *******************************************************************************/
   private String assertError(HttpResponse<String> response, Integer status)
   {
      String error = json(response, status).getString("error");
      assertFalse(error.isBlank());
      return error;
   }



   /*******************************************************************************
    ** Direct SQL is an independent oracle for application-API mutations.
    *******************************************************************************/
   private List<String> people() throws Exception
   {
      List<String> rows = new ArrayList<>();
      try(Statement statement = anchor.createStatement(); ResultSet result = statement.executeQuery("SELECT id,first_name,last_name,email,days_worked FROM person ORDER BY id"))
      {
         while(result.next())
         {
            rows.add(result.getInt(1) + ":" + result.getString(2) + ":" + result.getString(3) + ":" + result.getString(4) + ":" + result.getString(5));
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Per-fixture controls cross request/job threads without changing API behavior.
    *******************************************************************************/
   private static class ProcessControl
   {
      private final List<String> processIds = new CopyOnWriteArrayList<>();
      private final CountDownLatch started = new CountDownLatch(1);
      private final CountDownLatch release = new CountDownLatch(1);
   }



   /*******************************************************************************
    ** A normal application backend step reads native sample data through GetAction.
    *******************************************************************************/
   public static class ReadPersonStep implements BackendStep, PreRunApiProcessCustomizer
   {
      /***************************************************************************
       ** Capture only the API-assigned process IDs for owned-state cleanup.
       ***************************************************************************/
      @Override
      public void preApiRun(RunProcessInput input)
      {
         CONTROLS.get(QContext.getQSession().getValue("apiName")).processIds.add(input.getProcessUUID());
      }



      /***************************************************************************
       ** The latch provides deterministic pending status; errors are synthetic.
       ***************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         if(input.getProcessName().equals("failProcess"))
         {
            throw new QException("Synthetic HTTP process failure");
         }
         if(input.getProcessName().equals("readAsync"))
         {
            ProcessControl control = CONTROLS.get(QContext.getQSession().getValue("apiName"));
            control.started.countDown();
            try
            {
               if(!control.release.await(10, TimeUnit.SECONDS))
               {
                  throw new QException("Timed out waiting for the owned HTTP test");
               }
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
               throw new QException("Owned HTTP test interrupted", e);
            }
         }
         QRecord person = new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(input.getValueInteger("personId")));
         output.addValue("firstName", person.getValueString("firstName"));
         output.addValue("processName", input.getProcessName());
      }
   }



   /*******************************************************************************
    ** Application output adapter exercised by QJavalinApiHandler on the wire.
    *******************************************************************************/
   public static class PersonTextOutput extends ApiProcessObjectOutput
   {
      /***************************************************************************
       ** A configured custom process may choose its successful response status.
       ***************************************************************************/
      @Override
      public HttpStatus.Code getSuccessStatusCode(RunProcessInput input, RunProcessOutput output)
      {
         return HttpStatus.Code.CREATED;
      }



      /***************************************************************************
       ** The native handler serializes this adapter's body and MIME header.
       ***************************************************************************/
      @Override
      public void customizeHttpApiResponse(HttpApiResponse response, RunProcessInput input, RunProcessOutput output)
      {
         response.withNeedsFormattedAsJson(false).withContentType("text/plain")
            .withResponseBodyObject("Person: " + output.getValueString("firstName"));
      }
   }
}
