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
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.h2.tools.RunScript;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Versioned metadata and read protocols over owned HTTP and H2 resources.
 *******************************************************************************/
class SampleV1TableHttpAcceptanceTest
{
   private CapturedContext previousContext;
   private Map<String, Serializable> previousObjects;
   private Connection anchor;
   private QInstance instance;
   private QApplicationJavalinServer server;
   private HttpClient client;
   private URI base;
   private final String sentinelName = "OwnedV1-" + UUID.randomUUID();



   /*******************************************************************************
    ** The unique database contains only the canonical synthetic sample rows.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousContext = QContext.capture();
      previousObjects = QContext.getObjects();
      QContext.setObjects(new LinkedHashMap<>());
      ConnectionManager.resetConnectionProviders();
      String jdbcUrl = "jdbc:h2:mem:v1_table_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=0";
      anchor = DriverManager.getConnection(jdbcUrl, "sa", "");
      try(InputStreamReader reader = new InputStreamReader(SampleMetaDataProvider.class.getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(anchor, reader);
      }
      try(var statement = anchor.prepareStatement("UPDATE person SET first_name=? WHERE id=1"))
      {
         statement.setString(1, sentinelName);
         assertEquals(1, statement.executeUpdate());
      }
      instance = SampleMetaDataProvider.defineTestInstance();
      ((RDBMSBackendMetaData) instance.getBackend(SampleMetaDataProvider.RDBMS_BACKEND_NAME)).setJdbcUrl(jdbcUrl);
      QJavalinMetaData.ofOrWithNew(instance).withQueryWithoutLimitAllowed(false).withQueryWithoutLimitDefault(2);
   }



   /*******************************************************************************
    ** Release the owned server before its database and restore the caller context.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
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
      }
      finally
      {
         try
         {
            if(anchor != null)
            {
               anchor.close();
            }
         }
         finally
         {
            ConnectionManager.resetConnectionProviders();
            QContext.clear();
            QContext.init(previousContext);
            QContext.setObjects(previousObjects);
         }
      }
   }



   /*******************************************************************************
    ** V1 returns table/process metadata directly, with native field/step details.
    *******************************************************************************/
   @Test
   void testVersionedMetadataShapes() throws Exception
   {
      start();
      JSONObject all = success(request("GET", "/metaData", null, false));
      assertTrue(all.getJSONObject("tables").has("person"));
      assertTrue(all.getJSONObject("processes").has("greetInteractive"));
      JSONObject table = success(request("GET", "/metaData/table/person", null, false));
      assertEquals("person", table.getString("name"));
      assertTrue(table.getJSONObject("fields").has("firstName"));
      assertFalse(table.has("table"));
      JSONObject process = success(request("GET", "/metaData/process/greetInteractive", null, false));
      assertEquals("greetInteractive", process.getString("name"));
      assertEquals("setup", process.getJSONArray("frontendSteps").getJSONObject(0).getString("name"));
   }



   /*******************************************************************************
    ** Omitted limits are bounded; explicit limits and total count stay independent.
    *******************************************************************************/
   @Test
   void testConfiguredDefaultLimitAndExplicitPaging() throws Exception
   {
      start();
      assertEquals(List.of(1, 2, 3, 4, 5), nativeIds("SELECT id FROM person ORDER BY id"));
      assertEquals(2, success(request("POST", "/table/person/query", "{}", false)).getJSONArray("records").length());
      String order = "{\"filter\":{\"orderBys\":[{\"fieldName\":\"id\",\"isAscending\":true}]}}";
      assertEquals(List.of(1, 2), ids(success(request("POST", "/table/person/query", order, false)), "id"));
      String page = "{\"filter\":{\"limit\":3,\"skip\":1,\"orderBys\":[{\"fieldName\":\"id\",\"isAscending\":true}]}}";
      assertEquals(List.of(2, 3, 4), ids(success(request("POST", "/table/person/query", page, false)), "id"));
      assertEquals(5, success(request("POST", "/table/person/count", "{}", false)).getInt("count"));
      assertEquals(2, success(request("POST", "/table/person/query", "{\"filter\":null}", false)).getJSONArray("records").length());
      assertEquals(5, success(request("POST", "/table/person/count", "{\"filter\":null}", false)).getInt("count"));
   }



   /*******************************************************************************
    ** Structured filters/order/joins match independently selected SQL identities.
    *******************************************************************************/
   @Test
   void testFilterJoinAndEmptyResponse() throws Exception
   {
      start();
      String body = """
         {"joins":[{"joinTable":"pet","alias":"animal","select":true}],
          "filter":{"criteria":[{"fieldName":"id","operator":"EQUALS","values":[1]}],
          "limit":10,"orderBys":[{"fieldName":"animal.id","isAscending":false}]}}
         """;
      JSONObject result = success(request("POST", "/table/person/query", body, false));
      assertEquals(nativeIds("SELECT id FROM pet WHERE person_id=1 ORDER BY id DESC"), ids(result, "animal.id"));
      assertEquals(sentinelName, result.getJSONArray("records").getJSONObject(0).getJSONObject("values").getString("firstName"));
      assertEquals(4, success(request("POST", "/table/person/count", body, false)).getInt("count"));
      String empty = "{\"filter\":{\"criteria\":[{\"fieldName\":\"id\",\"operator\":\"EQUALS\",\"values\":[999]}]}}";
      assertEquals(List.of(), success(request("POST", "/table/person/query", empty, false)).getJSONArray("records").toList());
      assertEquals(0, success(request("POST", "/table/person/count", empty, false)).getInt("count"));
   }



   /*******************************************************************************
    ** Absent metadata and malformed requests cannot return an unfiltered success.
    *******************************************************************************/
   @Test
   void testUnknownTablesAndMalformedRequests() throws Exception
   {
      start();
      assertEquals(404, request("GET", "/metaData/table/missingTable", null, false).statusCode());
      for(String action : List.of("query", "count"))
      {
         reject(request("POST", "/table/missingTable/" + action, "{}", false));
         for(String body : List.of("{", "[]", "{\"filter\":{\"criteria\":[{\"fieldName\":\"id\",\"operator\":\"NOT_AN_OPERATOR\"}]}}"))
         {
            reject(request("POST", "/table/person/" + action, body, false));
         }
      }
      assertEquals(List.of(1, 2, 3, 4, 5), nativeIds("SELECT id FROM person ORDER BY id"));
   }



   /*******************************************************************************
    ** Wrong filter shapes must not silently turn a requested filter into no filter.
    *******************************************************************************/
   @Test
   void testNonObjectFilterIsRejected() throws Exception
   {
      start();
      List<Executable> checks = new ArrayList<>();
      for(String action : List.of("query", "count"))
      {
         for(String body : List.of("{\"filter\":\"id=1\"}", "{\"filter\":[]}", "{\"filter\":42}", "{\"filter\":true}"))
         {
            checks.add(() -> assertEquals(400, request("POST", "/table/person/" + action, body, false).statusCode(), action + ": " + body));
         }
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** The mock provider's documented denial token exercises the actual 401 path.
    *******************************************************************************/
   @Test
   void testAuthenticationDenialHasAllowedControls() throws Exception
   {
      start();
      for(String path : List.of("/metaData", "/metaData/table/person", "/metaData/process/greetInteractive", "/table/person/query", "/table/person/count"))
      {
         boolean post = path.endsWith("query") || path.endsWith("count");
         assertEquals(200, request(post ? "POST" : "GET", path, post ? "{}" : null, false).statusCode());
         HttpResponse<String> denied = request(post ? "POST" : "GET", path, post ? "{}" : null, true);
         assertEquals(401, denied.statusCode());
         assertFalse(denied.body().contains(sentinelName));
      }
   }



   /*******************************************************************************
    ** Table READ denial applies to both query/count without disclosing rows.
    *******************************************************************************/
   @Test
   void testReadPermissionDenial() throws Exception
   {
      instance.getTable("person").setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      start();
      for(String action : List.of("query", "count"))
      {
         HttpResponse<String> response = request("POST", "/table/person/" + action, "{}", false);
         assertEquals(403, response.statusCode());
         JSONObject error = new JSONObject(response.body());
         assertFalse(error.has("records"));
         assertFalse(error.has("count"));
      }
      assertEquals(2, success(request("POST", "/table/pet/query", "{}", false)).getJSONArray("records").length());
   }



   /*******************************************************************************
    ** Serve the sample metadata through native HTTP without demo database/broker startup.
    *******************************************************************************/
   private void start() throws Exception
   {
      server = new QApplicationJavalinServer(new SampleMetaDataProvider()
      {
         /***************************************************************************
          ** Use the owned instance instead of defining another database connection.
          ***************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      });
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withServeFrontendMaterialDashboard(false).withServeFrontendNext(false).withServeLegacyUnversionedMiddlewareAPI(false);
      server.withJavalinConfigCustomizer(config -> config.jetty.host = "127.0.0.1");
      server.withJavalinConfigurationCustomizer(service::set);
      server.start();
      try(Connection actual = ConnectionManager.getConnection((RDBMSBackendMetaData) instance.getBackend(SampleMetaDataProvider.RDBMS_BACKEND_NAME)))
      {
         assertEquals(anchor.getMetaData().getURL(), actual.getMetaData().getURL());
         try(Statement statement = actual.createStatement(); var rows = statement.executeQuery("SELECT first_name FROM person WHERE id=1"))
         {
            assertTrue(rows.next());
            assertEquals(sentinelName, rows.getString(1));
         }
      }
      base = URI.create("http://127.0.0.1:" + service.get().port() + "/qqq/v1");
      client = HttpClient.newHttpClient();
   }



   /*******************************************************************************
    ** No cookie jar lets each denial token reach the provider independently.
    *******************************************************************************/
   private HttpResponse<String> request(String method, String path, String body, boolean deny) throws Exception
   {
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(15));
      if(deny)
      {
         request.header("Authorization", "Bearer Deny");
      }
      if(body != null)
      {
         request.header("Content-Type", "application/json");
      }
      return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    ** Success assertions include wire format and status, not just parseable JSON.
    *******************************************************************************/
   private JSONObject success(HttpResponse<String> response)
   {
      assertEquals(200, response.statusCode(), response.body());
      assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
      return new JSONObject(response.body());
   }



   /*******************************************************************************
    ** Preserve existing error status mapping while rejecting accidental successes.
    *******************************************************************************/
   private void reject(HttpResponse<String> response)
   {
      assertTrue(response.statusCode() >= 400 && response.statusCode() < 600, "Unexpected status " + response.statusCode());
      assertFalse(response.body().contains(sentinelName));
   }



   /*******************************************************************************
    ** Extract the selected identity without duplicating backend filtering logic.
    *******************************************************************************/
   private List<Integer> ids(JSONObject response, String field)
   {
      List<Integer> result = new ArrayList<>();
      for(Object record : response.getJSONArray("records"))
      {
         result.add(((JSONObject) record).getJSONObject("values").getInt(field));
      }
      return result;
   }



   /*******************************************************************************
    ** JDBC readback provides an independent oracle over owned fixture rows.
    *******************************************************************************/
   private List<Integer> nativeIds(String sql) throws Exception
   {
      List<Integer> result = new ArrayList<>();
      try(Statement statement = anchor.createStatement(); var rows = statement.executeQuery(sql))
      {
         while(rows.next())
         {
            result.add(rows.getInt(1));
         }
      }
      return result;
   }
}
