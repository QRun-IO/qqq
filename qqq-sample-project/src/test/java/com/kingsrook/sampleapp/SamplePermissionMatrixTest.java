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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.permissions.CustomPermissionChecker;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Real sample HTTP permission matrices with independent native data readback.
 *******************************************************************************/
class SamplePermissionMatrixTest
{
   private static volatile Set<String> grants = Set.of();
   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final AtomicInteger processCalls = new AtomicInteger();
   private QInstance instance;
   private SampleJavalinServer server;
   private HttpClient client;



   /*******************************************************************************
    ** Requests use independent mock sessions; no cookie retains earlier grants.
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      grants = Set.of();
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(OwnedPermissions.class));
      instance.addProcess(new QProcessMetaData().withName("ownedPermission").withPermissionRules(rules(PermissionLevel.HAS_ACCESS_PERMISSION))
         .withStep(new QBackendStepMetaData().withName("run").withCode(new QCodeReferenceLambda<BackendStep>((in, out) -> out.addValue("calls", processCalls.incrementAndGet())))));
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
      client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void stop()
   {
      if(client != null)
      {
         client.close();
      }
      if(server != null)
      {
         server.stop();
      }
      grants = Set.of();
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnprotectedAndAccessPermissionLevels() throws Exception
   {
      tableRules(rules(PermissionLevel.NOT_PROTECTED));
      assertAccess(true, true, true, true);
      tableRules(rules(PermissionLevel.HAS_ACCESS_PERMISSION).withPermissionBaseName("ownedAlias"));
      assertAccess(false, false, false, false);
      grants = Set.of("ownedAlias.read");
      assertAccess(false, false, false, false);
      grants = Set.of("ownedAlias.hasAccess");
      assertAccess(true, true, true, true);
   }



   /*******************************************************************************
    ** WRITE allows mutations without granting READ; READ cannot perform mutations.
    *******************************************************************************/
   @Test
   void testReadWritePermissionMatrix() throws Exception
   {
      tableRules(rules(PermissionLevel.READ_WRITE_PERMISSIONS));
      assertAccess(false, false, false, false);
      grants = Set.of("fieldLab.read");
      assertAccess(true, false, false, false);
      grants = Set.of("fieldLab.write");
      assertAccess(false, true, true, true);
      grants = Set.of("fieldLab.read", "fieldLab.write");
      assertAccess(true, true, true, true);
   }



   /*******************************************************************************
    ** Each named mutation grant is independent, including denied native readback.
    *******************************************************************************/
   @Test
   void testGranularPermissionMatrix() throws Exception
   {
      tableRules(rules(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      assertAccess(false, false, false, false);
      grants = Set.of("fieldLab.read");
      assertAccess(true, false, false, false);
      grants = Set.of("fieldLab.insert");
      assertAccess(false, true, false, false);
      grants = Set.of("fieldLab.edit");
      assertAccess(false, false, true, false);
      grants = Set.of("fieldLab.delete");
      assertAccess(false, false, false, true);
      grants = Set.of("fieldLab.read", "fieldLab.insert", "fieldLab.edit", "fieldLab.delete");
      assertAccess(true, true, true, true);
   }



   /*******************************************************************************
    ** HIDDEN omits the object; DISABLED exposes metadata with all permissions false.
    *******************************************************************************/
   @Test
   void testHiddenDisabledAndGrantedMetadata() throws Exception
   {
      tableRules(rules(PermissionLevel.READ_WRITE_PERMISSIONS));
      assertFalse(metadata().getJSONObject("tables").has("fieldLab"));
      tableRules(rules(PermissionLevel.READ_WRITE_PERMISSIONS).withDenyBehavior(DenyBehavior.DISABLED));
      JSONObject disabled = metadata().getJSONObject("tables").getJSONObject("fieldLab");
      for(String permission : List.of("readPermission", "insertPermission", "editPermission", "deletePermission"))
      {
         assertFalse(disabled.getBoolean(permission));
      }
      assertAccess(false, false, false, false);
      grants = Set.of("fieldLab.read");
      JSONObject readOnly = metadata().getJSONObject("tables").getJSONObject("fieldLab");
      assertTrue(readOnly.getBoolean("readPermission"));
      assertFalse(readOnly.getBoolean("insertPermission"));
      assertFalse(readOnly.getBoolean("editPermission"));
      assertFalse(readOnly.getBoolean("deletePermission"));
   }



   /*******************************************************************************
    ** Legacy and versioned process requests enforce access before executing code.
    *******************************************************************************/
   @Test
   void testProcessPermissionAndMetadata() throws Exception
   {
      assertFalse(metadata().getJSONObject("processes").has("ownedPermission"));
      assertProcessAccess(false);
      instance.getProcess("ownedPermission").getPermissionRules().setDenyBehavior(DenyBehavior.DISABLED);
      assertFalse(metadata().getJSONObject("processes").getJSONObject("ownedPermission").getBoolean("hasPermission"));
      assertProcessAccess(false);
      grants = Set.of("ownedPermission.hasAccess");
      assertTrue(metadata().getJSONObject("processes").getJSONObject("ownedPermission").getBoolean("hasPermission"));
      assertProcessAccess(true);
   }



   /*******************************************************************************
    ** Custom policy controls metadata, table routes and process routes consistently.
    *******************************************************************************/
   @Test
   void testCustomPermissionCheckerOverridesStandardGrants() throws Exception
   {
      tableRules(rules(PermissionLevel.HAS_ACCESS_PERMISSION).withCustomPermissionChecker(new QCodeReference(OwnedChecker.class)));
      instance.getProcess("ownedPermission").getPermissionRules().setCustomPermissionChecker(new QCodeReference(OwnedChecker.class));
      grants = Set.of("fieldLab.hasAccess", "ownedPermission.hasAccess");
      assertFalse(metadata().getJSONObject("tables").has("fieldLab"));
      assertAccess(false, false, false, false);
      assertProcessAccess(false);
      grants = Set.of("owned.override");
      assertTrue(metadata().getJSONObject("tables").has("fieldLab"));
      assertAccess(true, true, true, true);
      assertProcessAccess(true);
   }



   /*******************************************************************************
    ** Without capabilities, count and export are refused over every route even on
    ** an unprotected table; get, query and mutations still follow permissions.
    *******************************************************************************/
   @Test
   void testDisabledCapabilityRuntimeBoundary() throws Exception
   {
      tableRules(rules(PermissionLevel.NOT_PROTECTED));
      instance.getTable("fieldLab").withoutCapabilities(EnumSet.allOf(Capability.class));
      assertEquals(0, metadata().getJSONObject("tables").getJSONObject("fieldLab").getJSONArray("capabilities").length());
      assertAccess(true, false, true, true, true);
      tableRules(rules(PermissionLevel.READ_WRITE_PERMISSIONS));
      assertAccess(false, false, false, false, false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QPermissionRules rules(PermissionLevel level)
   {
      return QPermissionRules.defaultInstance().withLevel(level);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void tableRules(QPermissionRules rules)
   {
      instance.getTable("fieldLab").setPermissionRules(rules);
   }



   /*******************************************************************************
    ** Check response authorization and every mutation against native SQL; count
    ** and export follow READ while the table declares those capabilities.
    *******************************************************************************/
   private void assertAccess(Boolean read, Boolean insert, Boolean edit, Boolean delete) throws Exception
   {
      assertAccess(read, read, insert, edit, delete);
   }



   /*******************************************************************************
    ** Check response authorization and every mutation against native SQL, with
    ** count and export (TABLE_COUNT / TABLE_EXPORT) asserted separately from READ.
    *******************************************************************************/
   private void assertAccess(Boolean read, Boolean countAndExport, Boolean insert, Boolean edit, Boolean delete) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("DELETE FROM field_lab");
         statement.executeUpdate("ALTER TABLE field_lab ALTER COLUMN id RESTART WITH 100");
         statement.executeUpdate("INSERT INTO field_lab(id,name) VALUES (1,'Target'),(2,'Bystander')");
      }
      for(String path : List.of("/data/fieldLab/1", "/data/fieldLab"))
      {
         assertStatus(request("GET", path, null, null), read);
      }
      assertStatus(request("POST", "/qqq/v1/table/fieldLab/query", "{}", "application/json"), read);
      assertStatus(request("GET", "/data/fieldLab/count", null, null), countAndExport);
      assertStatus(request("POST", "/qqq/v1/table/fieldLab/count", "{}", "application/json"), countAndExport);
      assertStatus(request("POST", "/data/fieldLab/export/fieldLab.csv", "fields=id,name", "application/x-www-form-urlencoded"), countAndExport);
      assertStatus(request("POST", "/data/fieldLab", "{\"name\":\"Inserted\"}", "application/json"), insert);
      assertStatus(request("PATCH", "/data/fieldLab/1", "{\"name\":\"Changed\"}", "application/json"), edit);
      assertStatus(request("DELETE", "/data/fieldLab/1", null, null), delete);
      List<String> expected = new ArrayList<>();
      if(!delete)
      {
         expected.add(edit ? "Changed" : "Target");
      }
      expected.add("Bystander");
      if(insert)
      {
         expected.add("Inserted");
      }
      assertEquals(expected, names());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertProcessAccess(Boolean allowed) throws Exception
   {
      Integer before = processCalls.get();
      assertStatus(request("POST", "/processes/ownedPermission/init", "", "application/x-www-form-urlencoded"), allowed);
      assertStatus(request("POST", "/qqq/v1/processes/ownedPermission/init", "", "application/x-www-form-urlencoded"), allowed);
      assertEquals(before + (allowed ? 2 : 0), processCalls.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertStatus(HttpResponse<String> response, Boolean allowed)
   {
      assertEquals(allowed ? 200 : 403, response.statusCode(), response.uri() + ": " + response.body());
      if(!allowed)
      {
         JSONObject error = new JSONObject(response.body());
         for(String payload : List.of("record", "records", "values", "count"))
         {
            assertFalse(error.has(payload), response.body());
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject metadata() throws Exception
   {
      HttpResponse<String> response = request("GET", "/metaData", null, null);
      assertEquals(200, response.statusCode(), response.body());
      return new JSONObject(response.body());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> request(String method, String path, String body, String contentType) throws Exception
   {
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + path)).timeout(Duration.ofSeconds(10));
      if(contentType != null)
      {
         request.header("Content-Type", contentType);
      }
      return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> names() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT name FROM field_lab ORDER BY id"))
      {
         List<String> names = new ArrayList<>();
         while(rows.next())
         {
            names.add(rows.getString(1));
         }
         return names;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class OwnedPermissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions(grants.toArray(String[]::new));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class OwnedChecker implements CustomPermissionChecker
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void checkPermissionsThrowing(AbstractActionInput input, MetaDataWithPermissionRules metadata) throws QPermissionDeniedException
      {
         if(!QContext.getQSession().hasPermission("owned.override"))
         {
            throw new QPermissionDeniedException("Owned custom policy denied access");
         }
      }
   }
}
