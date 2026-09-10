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

package com.kingsrook.sampleapp;


import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** First-party acceptance of the sample over HTTP with synthetic H2 data.
 *******************************************************************************/
class SampleJavalinServerTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDashboardAndDatabaseWorkflow() throws Exception
   {
      String originalMockAuthentication = System.getProperty("qqq.sample.mockAuthentication");
      SampleJavalinServer sampleJavalinServer = new SampleJavalinServer();
      AtomicReference<Javalin> service = new AtomicReference<>();
      sampleJavalinServer.setPort(0);
      sampleJavalinServer.withJavalinConfigurationCustomizer(service::set);

      try
      {
         System.setProperty("qqq.sample.mockAuthentication", "true");
         assertEquals(QAuthenticationType.MOCK, new SampleMetaDataProvider().defineQInstance().getAuthentication().getType());
         sampleJavalinServer.start();

         try(HttpClient client = HttpClient.newHttpClient())
         {
            URI baseUri = URI.create("http://localhost:" + service.get().port());
            HttpResponse<String> dashboard = request(client, baseUri, "GET", "/", null, null);
            assertEquals(200, dashboard.statusCode());
            assertTrue(dashboard.body().contains("<html"));
            Matcher script = Pattern.compile("src=\"([^\"]+\\.js)\"").matcher(dashboard.body());
            assertTrue(script.find(), "The dashboard must include its JavaScript bundle");
            HttpResponse<String> bundle = request(client, baseUri, "GET", script.group(1), null, null);
            assertEquals(200, bundle.statusCode());
            assertTrue(bundle.headers().firstValue("Content-Type").orElse("").contains("javascript"));
            assertFalse(bundle.body().stripLeading().startsWith("<!DOCTYPE html"), "A fallback HTML page is not a JavaScript bundle");

            JSONObject metadata = requestJson(client, baseUri, "GET", "/metaData", null);
            assertTrue(metadata.getJSONObject("tables").has("person"));
            assertTrue(metadata.getJSONObject("processes").has("greet"));

            JSONObject inserted = requestJson(client, baseUri, "POST", "/data/person",
               "{\"firstName\":\"Release\",\"lastName\":\"Acceptance\",\"email\":\"release@example.invalid\"}");
            Integer personId = inserted.getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id");
            assertDatabasePerson(personId, "Release");

            JSONObject fetched = requestJson(client, baseUri, "GET", "/data/person/" + personId, null);
            assertEquals("Release", fetched.getJSONObject("values").getString("firstName"));
            requestJson(client, baseUri, "PATCH", "/data/person/" + personId, "{\"firstName\":\"Updated\"}");
            assertDatabasePerson(personId, "Updated");
            fetched = requestJson(client, baseUri, "GET", "/data/person/" + personId, null);
            assertEquals("Updated", fetched.getJSONObject("values").getString("firstName"));

            String filter = "{\"criteria\":[{\"fieldName\":\"id\",\"operator\":\"EQUALS\",\"values\":[" + personId + "]}]}";
            JSONObject filtered = requestJson(client, baseUri, "GET", "/data/person?filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8), null);
            assertEquals(1, filtered.getJSONArray("records").length());
            assertEquals(personId, filtered.getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id"));

            HttpResponse<String> processResponse = request(client, baseUri, "POST", "/processes/greet/init",
               "recordsParam=recordIds&recordIds=" + personId + "&greetingPrefix=Hello&greetingSuffix=QQQ",
               "application/x-www-form-urlencoded");
            assertEquals(200, processResponse.statusCode(), processResponse.body());
            JSONObject process = JsonUtils.toJSONObject(processResponse.body());
            assertFalse(process.has("error"), process.toString());
            assertFalse(process.has("jobUUID"), "The greeting should finish synchronously");
            assertFalse(process.has("nextStep"), "The greeting should have no remaining steps");
            assertEquals("Hello X QQQ", process.getJSONObject("values").getString("outputMessage"));
            JSONObject results = requestJson(client, baseUri, "GET", "/processes/greet/" + process.getString("processUUID") + "/records", null);
            assertEquals(1, results.getInt("totalRecords"));
            assertEquals("Hello Updated QQQ", results.getJSONArray("records").getJSONObject(0).getJSONObject("values").getString("greetingMessage"));

            HttpResponse<String> invalidResponse = request(client, baseUri, "POST", "/data/person", "{\"firstName\":\"Invalid\"}", "application/json");
            assertEquals(500, invalidResponse.statusCode());
            String validationError = JsonUtils.toJSONObject(invalidResponse.body()).getString("error");
            assertTrue(validationError.contains("Missing value in required field: Last Name"));
            assertTrue(validationError.contains("Missing value in required field: Email"));
            try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
                PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM person WHERE first_name = ?"))
            {
               statement.setString(1, "Invalid");
               try(ResultSet rows = statement.executeQuery())
               {
                  assertTrue(rows.next());
                  assertEquals(0, rows.getInt(1), "Invalid records must not be persisted");
               }
            }

            JSONObject deleted = requestJson(client, baseUri, "DELETE", "/data/person/" + personId, null);
            assertEquals(1, deleted.getInt("deletedRecordCount"));
            assertDatabasePerson(personId, null);
            assertEquals(404, request(client, baseUri, "GET", "/metaData/table/noSuchTable", null, null).statusCode());
         }

         System.clearProperty("qqq.sample.mockAuthentication");
         assertEquals(QAuthenticationType.OAUTH2, new SampleMetaDataProvider().defineQInstance().getAuthentication().getType());
      }
      finally
      {
         sampleJavalinServer.stop();
         QContext.clear();
         if(originalMockAuthentication == null)
         {
            System.clearProperty("qqq.sample.mockAuthentication");
         }
         else
         {
            System.setProperty("qqq.sample.mockAuthentication", originalMockAuthentication);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> request(HttpClient client, URI baseUri, String method, String path, String body, String contentType) throws Exception
   {
      URI target = baseUri.resolve(path);
      assertEquals(baseUri.getAuthority(), target.getAuthority(), "Acceptance requests must stay on the local server");
      HttpRequest.Builder request = HttpRequest.newBuilder(target)
         .timeout(Duration.ofSeconds(30))
         .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
      if(contentType != null)
      {
         request.header("Content-Type", contentType);
      }
      return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject requestJson(HttpClient client, URI baseUri, String method, String path, String body) throws Exception
   {
      HttpResponse<String> response = request(client, baseUri, method, path, body, "application/json");
      assertEquals(200, response.statusCode(), response.body());
      return JsonUtils.toJSONObject(response.body());
   }



   /*******************************************************************************
    ** Check the storage layer independently of the HTTP response.
    *******************************************************************************/
   private void assertDatabasePerson(Integer personId, String firstName) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          PreparedStatement statement = connection.prepareStatement("SELECT first_name FROM person WHERE id = ?"))
      {
         statement.setInt(1, personId);
         try(ResultSet rows = statement.executeQuery())
         {
            if(firstName == null)
            {
               assertFalse(rows.next(), "Deleted records must be absent from storage");
            }
            else
            {
               assertTrue(rows.next());
               assertEquals(firstName, rows.getString(1));
               assertFalse(rows.next());
            }
         }
      }
   }
}
