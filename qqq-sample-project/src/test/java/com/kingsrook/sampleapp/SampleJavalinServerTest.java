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


import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** First-party acceptance of the sample over HTTP with synthetic H2 data.
 *******************************************************************************/
public class SampleJavalinServerTest
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

         try(HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build())
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
            assertEquals(400, invalidResponse.statusCode());
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
            assertEquals("Charlie", requestJson(client, baseUri, "GET", "/data/pet/1", null).getString("recordLabel"));
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
   @Test
   void testCountHttpContracts() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(CountPermissions.class));
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(PrivateReadFields.class));
      instance.getTable("person").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getTable("pet").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getTable("fieldLab").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      SampleJavalinServer server = new SampleJavalinServer(new SampleMetaDataProvider()
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
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      try
      {
         server.start();
         try(HttpClient client = HttpClient.newHttpClient())
         {
            URI baseUri = URI.create("http://localhost:" + service.get().port());
            JSONObject total = requestJson(client, baseUri, "GET", "/data/person/count", null);
            assertEquals(5, total.getInt("count"));
            assertTrue(total.isNull("distinctCount"));
            String filter = URLEncoder.encode("{\"criteria\":[{\"fieldName\":\"isEmployed\",\"operator\":\"EQUALS\",\"values\":[true]}],\"skip\":50,\"limit\":1}", StandardCharsets.UTF_8);
            HttpResponse<String> filtered = request(client, baseUri, "POST", "/data/person/count", "filter=" + filter, "application/x-www-form-urlencoded");
            assertEquals(200, filtered.statusCode(), filtered.body());
            assertEquals(4, JsonUtils.toJSONObject(filtered.body()).getInt("count"));
            String joins = URLEncoder.encode("[{\"joinTable\":\"pet\",\"type\":\"LEFT\",\"alias\":\"animal\"}]", StandardCharsets.UTF_8);
            JSONObject joined = requestJson(client, baseUri, "GET", "/data/person/count?includeDistinct=true&queryJoins=" + joins, null);
            assertEquals(8, joined.getInt("count"));
            assertEquals(5, joined.getInt("distinctCount"));
            String species = URLEncoder.encode("{\"criteria\":[{\"fieldName\":\"animal.speciesId\",\"operator\":\"EQUALS\",\"values\":[1]}]}", StandardCharsets.UTF_8);
            JSONObject selected = requestJson(client, baseUri, "GET", "/data/person/count?includeDistinct=true&queryJoins=" + joins + "&filter=" + species, null);
            assertEquals(5, selected.getInt("count"));
            assertEquals(2, selected.getInt("distinctCount"));
            String selectedJoin = URLEncoder.encode("[{\"joinTable\":\"pet\",\"alias\":\"animal\",\"select\":true}]", StandardCharsets.UTF_8);
            JSONObject joinedRecord = requestJson(client, baseUri, "GET", "/data/person/1?queryJoins=" + selectedJoin, null);
            assertTrue(joinedRecord.getJSONObject("values").has("animal.id"));
            JSONObject associatedRecord = requestJson(client, baseUri, "GET", "/data/person/1?includeAssociations=true", null);
            assertEquals(4, associatedRecord.getJSONObject("associatedRecords").getJSONArray("pets").length());

            HttpResponse<String> denied = request(client, baseUri, "GET", "/data/fieldLab/count", null, null);
            assertEquals(403, denied.statusCode(), denied.body());
            JSONObject deniedBody = JsonUtils.toJSONObject(denied.body());
            assertEquals("Permission denied.", deniedBody.getString("error"));
            assertFalse(deniedBody.has("count"));
            assertFalse(deniedBody.has("distinctCount"));
            String invalid = URLEncoder.encode("{\"criteria\":[{\"fieldName\":\"missingField\",\"operator\":\"EQUALS\",\"values\":[1]}]}", StandardCharsets.UTF_8);
            HttpResponse<String> rejected = request(client, baseUri, "GET", "/data/person/count?filter=" + invalid, null, null);
            assertEquals(500, rejected.statusCode(), rejected.body());
            assertFalse(JsonUtils.toJSONObject(rejected.body()).has("count"));
            assertEquals(5, requestJson(client, baseUri, "GET", "/data/person/count", null).getInt("count"));
            String privateFilter = URLEncoder.encode("{\"criteria\":[{\"fieldName\":\"annualSalary\",\"operator\":\"GREATER_THAN\",\"values\":[100000]}]}", StandardCharsets.UTF_8);
            JSONObject legacyQuery = requestJson(client, baseUri, "GET", "/data/person", null);
            JSONObject versionedQuery = requestJson(client, baseUri, "POST", "/qqq/v1/table/person/query", "{}");
            assertAll(
               () -> assertFalse(legacyQuery.getJSONArray("records").getJSONObject(0).getJSONObject("values").has("annualSalary")),
               () -> assertFalse(versionedQuery.getJSONArray("records").getJSONObject(0).getJSONObject("values").has("annualSalary")),
               () -> assertEquals(500, request(client, baseUri, "GET", "/data/person/count?filter=" + privateFilter, null, null).statusCode()),
               () -> assertFalse(joinedRecord.getJSONObject("values").has("animal.name")),
               () -> assertFalse(associatedRecord.getJSONObject("associatedRecords").getJSONArray("pets").getJSONObject(0).getJSONObject("values").has("name")));
         }
      }
      finally
      {
         server.stop();
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Happy-path joins require both tables' reads; Field Lab writes cannot authorize counts.
    *******************************************************************************/
   public static class CountPermissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read", "pet.read", "fieldLab.write");
      }
   }



   /*******************************************************************************
    ** User reads must preserve field personalization through joins and associations.
    *******************************************************************************/
   public static class PrivateReadFields implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         String privateField = Map.of("person", "annualSalary", "pet", "name").get(input.getTableName());
         if(!QInputSource.USER.equals(input.getInputSource()) || privateField == null)
         {
            return input.getTable();
         }
         QTableMetaData personalized = input.getTable().clone();
         personalized.getFields().remove(privateField);
         return personalized;
      }
   }



   /*******************************************************************************
    ** Both HTTP versions must enforce READ for explicit and implicitly joined tables.
    *******************************************************************************/
   @Test
   void testJoinedTableReadPermissions() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(PersonOnlyReads.class));
      instance.getTable("person").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getTable("pet").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("species"));
      instance.getTable("person").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("species")
         .withFieldName("pet.speciesId").withJoinNameChain(List.of("personJoinPet")));
      SampleJavalinServer server = new SampleJavalinServer(new SampleMetaDataProvider()
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
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      try
      {
         server.start();
         try(HttpClient client = HttpClient.newHttpClient())
         {
            URI baseUri = URI.create("http://localhost:" + service.get().port());
            assertEquals(2, requestJson(client, baseUri, "GET", "/data/person/count", null).getInt("count"));
            assertEquals(2, requestJson(client, baseUri, "POST", "/qqq/v1/table/person/count", "{}").getInt("count"));
            JSONObject allowedQuery = requestJson(client, baseUri, "GET", "/data/person", null);
            assertEquals(2, allowedQuery.getJSONArray("records").length());
            assertEquals(403, request(client, baseUri, "GET", "/data/pet/count", null, null).statusCode());
            List<Executable> checks = new ArrayList<>();
            String getJoin = URLEncoder.encode("[{\"joinTable\":\"pet\",\"select\":true}]", StandardCharsets.UTF_8);
            checks.add(() -> assertReadDenied(request(client, baseUri, "GET", "/data/person/1?queryJoins=" + getJoin, null, null)));
            checks.add(() -> assertReadDenied(request(client, baseUri, "GET", "/data/person/1?includeAssociations=true", null, null)));
            for(String json : List.of(
               "{\"joins\":[{\"joinTable\":\"pet\",\"alias\":\"animal\",\"type\":\"LEFT\",\"select\":true}]}",
               "{\"filter\":{\"subFilters\":[{\"criteria\":[{\"fieldName\":\"pet.speciesId\",\"operator\":\"EQUALS\",\"values\":[1]}]}]}}",
               "{\"filter\":{\"orderBys\":[{\"fieldName\":\"pet.name\",\"isAscending\":true}]}}"))
            {
               JSONObject input = new JSONObject(json);
               String parameters = input.has("joins") ? "queryJoins=" + URLEncoder.encode(input.getJSONArray("joins").toString(), StandardCharsets.UTF_8)
                  : "filter=" + URLEncoder.encode(input.getJSONObject("filter").toString(), StandardCharsets.UTF_8);
               checks.add(() -> assertReadDenied(request(client, baseUri, "GET", "/data/person/count?" + parameters, null, null)));
               checks.add(() -> assertReadDenied(request(client, baseUri, "GET", "/data/person?" + parameters, null, null)));
               checks.add(() -> assertReadDenied(request(client, baseUri, "POST", "/qqq/v1/table/person/count", json, "application/json")));
               checks.add(() -> assertReadDenied(request(client, baseUri, "POST", "/qqq/v1/table/person/query", json, "application/json")));
            }
            assertAll(checks);
         }
      }
      finally
      {
         server.stop();
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** READ permission still requires privacy for selected joined values in both APIs.
    *******************************************************************************/
   @Test
   void testJoinedFieldPrivacyAcrossHttpReads() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(PersonAndPetReads.class));
      instance.getTable("person").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getTable("pet").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getTable("pet").getField("name").setType(QFieldType.PASSWORD);
      instance.getTable("pet").getField("speciesId").setIsHidden(true);
      instance.getTable("pet").getSections().forEach(section -> section.setFieldNames(section.getFieldNames().stream()
         .filter(field -> !field.equals("speciesId")).toList()));
      SampleJavalinServer server = new SampleJavalinServer(new SampleMetaDataProvider()
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
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      try
      {
         server.start();
         List<List<String>> before = nativePetRows();
         assertEquals(6, before.size());
         Set<Integer> childIds = Set.of(1, 2, 3, 4);
         assertEquals(List.of("Charlie", "Coco", "Louie", "Barkley"), before.stream().filter(row -> row.get(1).equals("1")).map(row -> row.get(2)).toList());
         try(HttpClient client = HttpClient.newHttpClient())
         {
            URI baseUri = URI.create("http://localhost:" + service.get().port());
            String joins = "[{\"joinTable\":\"pet\",\"alias\":\"animal\",\"select\":true}]";
            String filter = "{\"criteria\":[{\"fieldName\":\"id\",\"operator\":\"EQUALS\",\"values\":[1]}],\"orderBys\":[{\"fieldName\":\"animal.id\",\"isAscending\":true}]}";
            String query = "?queryJoins=" + URLEncoder.encode(joins, StandardCharsets.UTF_8) + "&filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8);
            JSONObject legacy = requestJson(client, baseUri, "GET", "/data/person" + query, null);
            JSONObject versioned = requestJson(client, baseUri, "POST", "/qqq/v1/table/person/query", "{\"joins\":" + joins + ",\"filter\":" + filter + "}");
            JSONObject get = requestJson(client, baseUri, "GET", "/data/person/1?queryJoins=" + URLEncoder.encode(joins, StandardCharsets.UTF_8), null);
            List<Executable> checks = new ArrayList<>();
            for(JSONObject result : List.of(legacy, versioned))
            {
               JSONArray records = result.getJSONArray("records");
               assertEquals(4, records.length());
               for(int i = 0; i < records.length(); i++)
               {
                  JSONObject record = records.getJSONObject(i);
                  assertEquals(i + 1, record.getJSONObject("values").getInt("animal.id"));
                  checks.add(() -> assertPrivateJoinedPet(record, childIds));
               }
            }
            checks.add(() -> assertPrivateJoinedPet(get, childIds));
            checks.add(() -> assertEquals(before, nativePetRows()));
            assertAll(checks);
         }
      }
      finally
      {
         server.stop();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Native typed values survive the HTTP representation without exposing passwords.
    *******************************************************************************/
   @Test
   void testFieldLabJsonRoundTrip() throws Exception
   {
      SampleJavalinServer server = new SampleJavalinServer(new SampleMetaDataProvider()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QInstance defineQInstance() throws QException
         {
            return SampleMetaDataProvider.defineTestInstance();
         }
      });
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      try
      {
         server.start();
         Integer id;
         try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
            Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("INSERT INTO field_lab(name,long_value,decimal_value,boolean_value,date_value,time_value,date_time_value,text_value,html_value,password_value,blob_value) VALUES "
               + "('HTTP field types',9223372036854775807,1234567890123456.7890,FALSE,DATE '2024-02-29',TIME '23:59:58',TIMESTAMP '2024-02-29 23:59:58','Plain text é','<p>Sample HTML</p>','synthetic-http-only',X'0001FF')"));
            try(ResultSet rows = statement.executeQuery("SELECT id FROM field_lab WHERE name='HTTP field types'"))
            {
               assertTrue(rows.next());
               id = rows.getInt(1);
               assertFalse(rows.next());
            }
         }
         try(HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build())
         {
            URI base = URI.create("http://localhost:" + service.get().port());
            JSONObject record = requestJson(client, base, "GET", "/data/fieldLab/" + id, null);
            JSONObject values = record.getJSONObject("values");
            assertEquals(id, values.getInt("id"));
            assertEquals("HTTP field types", values.getString("name"));
            assertEquals(Long.MAX_VALUE, values.getLong("longValue"));
            assertEquals(0, new BigDecimal("1234567890123456.7890").compareTo(values.getBigDecimal("decimalValue")));
            assertFalse(values.getBoolean("booleanValue"));
            assertEquals("2024-02-29", values.getString("dateValue"));
            assertEquals("23:59:58", values.getString("timeValue"));
            assertEquals("2024-02-29T23:59:58Z", values.getString("dateTimeValue"));
            assertEquals("Plain text é", values.getString("textValue"));
            assertEquals("<p>Sample HTML</p>", values.getString("htmlValue"));
            assertEquals("************", values.getString("passwordValue"));
            assertFalse(record.toString().contains("synthetic-http-only"));
            assertArrayEquals(new byte[] { 0, 1, -1 }, Base64.getDecoder().decode(values.getString("blobValue")));
            assertEquals("HTTP field types", record.getString("recordLabel"));
            assertFalse(record.getJSONObject("displayValues").isEmpty());
         }
      }
      finally
      {
         server.stop();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertPrivateJoinedPet(JSONObject record, Set<Integer> childIds)
   {
      JSONObject values = record.getJSONObject("values");
      JSONObject display = record.optJSONObject("displayValues");
      assertAll(
         () -> assertEquals(1, values.getInt("id")),
         () -> assertTrue(childIds.contains(values.getInt("animal.id"))),
         () -> assertEquals("************", values.getString("animal.name")),
         () -> assertFalse(values.has("animal.speciesId")),
         () -> assertTrue(display == null || !display.has("animal.speciesId")),
         () -> assertTrue(display == null || !display.has("animal.name") || display.getString("animal.name").equals("************")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> nativePetRows() throws Exception
   {
      List<List<String>> result = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement();
          ResultSet rows = statement.executeQuery("SELECT id,person_id,name,species_id FROM pet ORDER BY id"))
      {
         while(rows.next())
         {
            result.add(List.of(rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4)));
         }
      }
      return result;
   }



   /*******************************************************************************
    ** Both selected tables permit reading; field privacy remains independently required.
    *******************************************************************************/
   public static class PersonAndPetReads implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read", "pet.read");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertReadDenied(HttpResponse<String> response)
   {
      assertEquals(403, response.statusCode(), response.uri() + ": " + response.body());
      JSONObject body = JsonUtils.toJSONObject(response.body());
      assertFalse(body.has("records"));
      assertFalse(body.has("record"));
      assertFalse(body.has("values"));
      assertFalse(body.has("associatedRecords"));
      assertFalse(body.has("count"));
      assertFalse(body.has("distinctCount"));
   }



   /*******************************************************************************
    ** A Pet WRITE permission cannot authorize revealing joined Pet values or counts.
    *******************************************************************************/
   public static class PersonOnlyReads implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read", "pet.write");
         session.withSecurityKeyValue("species", 1);
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
