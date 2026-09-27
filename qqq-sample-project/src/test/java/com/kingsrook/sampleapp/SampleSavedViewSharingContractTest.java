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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableAudienceType;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedView;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedViewsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedviews.SharedSavedView;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.DeleteSharedRecordProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.EditSharedRecordProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.ShareScope;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.SharingMetaDataProvider;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Generic sharing must bind the selected asset and share before mutation.
 ** These native H2 cases use provider defaults, without optional record locks;
 ** they verify process ownership and asset binding, not read confidentiality.
 *******************************************************************************/
class SampleSavedViewSharingContractTest
{
   private static Integer displayedViewId;

   private static final String OWNER = UUID.randomUUID().toString();
   private static final String OTHER_OWNER = UUID.randomUUID().toString();
   private static final String RECIPIENT = UUID.randomUUID().toString();

   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final HttpClient client = HttpClient.newHttpClient();
   private SampleJavalinServer server;
   private Integer ownedViewId;
   private Integer otherViewId;
   private Integer otherShareId;



   /*******************************************************************************
    ** Own the database and use real framework processes and ordinary sessions.
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.addPossibleValueSource(TablesPossibleValueSourceMetaDataProvider.defineTablesPossibleValueSource(instance));
      new SavedViewsMetaDataProvider().defineAll(instance, SampleMetaDataProvider.RDBMS_BACKEND_NAME, table ->
      {
         table.setBackendDetails(new RDBMSTableBackendDetails().withTableName(QInstanceEnricher.inferBackendName(table.getName())));
         QInstanceEnricher.setInferredFieldBackendNames(table);
      });
      instance.getTable(SavedView.TABLE_NAME).setShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName(SharedSavedView.TABLE_NAME)
         .withAssetIdFieldName("savedViewId").withScopeFieldName("scope").withThisTableOwnerIdFieldName("userId")
         .withAudienceType(new ShareableAudienceType().withName("user").withFieldName("userId")));
      new SharingMetaDataProvider().defineAll(instance, null);
      instance.getAuthentication().setCustomizer(new QCodeReference(SampleSavedReportContractTest.ReportUser.class));
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
      QContext.init(instance, session(OWNER));
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         Statement statement = connection.createStatement())
      {
         assertEquals("jdbc:h2:mem:test_database", connection.getMetaData().getURL());
         statement.execute("DROP TABLE IF EXISTS shared_saved_view");
         statement.execute("DROP TABLE IF EXISTS saved_view");
         statement.execute("CREATE TABLE saved_view (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, label VARCHAR(200), table_name VARCHAR(100), user_id VARCHAR(100), view_json TEXT)");
         statement.execute("CREATE TABLE shared_saved_view (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, saved_view_id INTEGER, user_id VARCHAR(100), scope VARCHAR(30), UNIQUE(saved_view_id, user_id))");
      }
      ownedViewId = insertView(OWNER, "Owner view");
      otherViewId = insertView(OTHER_OWNER, "Other view");
      QContext.setQSession(session(OTHER_OWNER));
      otherShareId = insertShare(otherViewId);
      QContext.setQSession(session(OWNER));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void stop() throws Exception
   {
      try
      {
         if(server != null)
         {
            server.stop();
         }
      }
      finally
      {
         client.close();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Presentation customizers must not supply the asset used for authorization.
    *******************************************************************************/
   @Test
   void testNativeReadCustomizerCannotChangeShareAuthorization() throws Exception
   {
      displayedViewId = ownedViewId;
      var table = QContext.getQInstance().getTable(SharedSavedView.TABLE_NAME);
      table.withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(DisplayShareReference.class));
      try
      {
         assertThrows(QException.class, () -> new EditSharedRecordProcess().run(input(ownedViewId, otherShareId), new RunBackendStepOutput()));
         assertThrows(QException.class, () -> new DeleteSharedRecordProcess().run(input(ownedViewId, otherShareId), new RunBackendStepOutput()));
      }
      finally
      {
         table.getCustomizers().remove(TableCustomizers.POST_QUERY_RECORD.getRole());
      }
      assertShareUnchanged();
   }



   /*******************************************************************************
    ** Hiding a foreign key from returned records must not reject its actual owner.
    *******************************************************************************/
   @Test
   void testNativeHiddenReferenceAllowsOwnerShareChanges() throws Exception
   {
      Integer shareId = insertShare(ownedViewId);
      QContext.getQInstance().getTable(SharedSavedView.TABLE_NAME).getField("savedViewId").setIsHidden(true);
      new EditSharedRecordProcess().run(input(ownedViewId, shareId), new RunBackendStepOutput());
      assertEquals(ShareScope.READ_WRITE.name(), getShare(shareId).getValueString("scope"));
      new DeleteSharedRecordProcess().run(input(ownedViewId, shareId), new RunBackendStepOutput());
      assertNull(getShare(shareId));
   }



   /*******************************************************************************
    ** The unrelated owner's share must remain unchanged after a rejected edit.
    *******************************************************************************/
   @Test
   void testNativeEditRejectsAnotherAssetsShare() throws Exception
   {
      assertThrows(QException.class, () -> new EditSharedRecordProcess().run(input(ownedViewId, otherShareId), new RunBackendStepOutput()));
      assertShareUnchanged();
   }



   /*******************************************************************************
    ** The unrelated owner's share must remain present after a rejected delete.
    *******************************************************************************/
   @Test
   void testNativeDeleteRejectsAnotherAssetsShare() throws Exception
   {
      assertThrows(QException.class, () -> new DeleteSharedRecordProcess().run(input(ownedViewId, otherShareId), new RunBackendStepOutput()));
      assertShareUnchanged();
   }



   /*******************************************************************************
    ** A wrong asset/share pair is invalid even when the caller owns both assets.
    *******************************************************************************/
   @Test
   void testNativeRejectsMismatchedShareWithinOneOwner() throws Exception
   {
      Integer secondViewId = insertView(OWNER, "Second owner view");
      Integer shareId = insertShare(secondViewId);
      assertThrows(QException.class, () -> new EditSharedRecordProcess().run(input(ownedViewId, shareId), new RunBackendStepOutput()));
      assertThrows(QException.class, () -> new DeleteSharedRecordProcess().run(input(ownedViewId, shareId), new RunBackendStepOutput()));
      assertEquals(ShareScope.READ_ONLY.name(), getShare(shareId).getValueString("scope"));
   }



   /*******************************************************************************
    ** Exercise the actual form POST route, then read the database independently.
    *******************************************************************************/
   @Test
   void testHttpEditRejectsAnotherAssetsShare() throws Exception
   {
      assertRejected(post(OWNER, "editSharedRecord", values(ownedViewId, otherShareId)), "No record was found to update for the specified asset and share");
      assertShareUnchanged();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHttpDeleteRejectsAnotherAssetsShare() throws Exception
   {
      assertRejected(post(OWNER, "deleteSharedRecord", values(ownedViewId, otherShareId)), "No record was found to delete for the specified asset and share");
      assertShareUnchanged();
   }



   /*******************************************************************************
    ** Existing owner authorization still rejects a matching share for a nonowner.
    *******************************************************************************/
   @Test
   void testHttpRecipientCannotEditOrDeleteShares() throws Exception
   {
      assertRejected(post(RECIPIENT, "editSharedRecord", values(otherViewId, otherShareId)), "not the owner of this record");
      assertRejected(post(RECIPIENT, "deleteSharedRecord", values(otherViewId, otherShareId)), "not the owner of this record");
      assertShareUnchanged();
   }



   /*******************************************************************************
    ** An ordinary owner can create, edit, and revoke a matching share over HTTP.
    *******************************************************************************/
   @Test
   void testHttpOwnerShareLifecycle() throws Exception
   {
      assertSuccessful(post(OWNER, "insertSharedRecord", Map.of("tableName", SavedView.TABLE_NAME,
         "recordId", ownedViewId.toString(), "audienceType", "user", "audienceId", RECIPIENT, "scopeId", ShareScope.READ_ONLY.name())));
      Integer shareId;
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         var statement = connection.prepareStatement("SELECT id FROM shared_saved_view WHERE saved_view_id = ?"))
      {
         statement.setInt(1, ownedViewId);
         try(var result = statement.executeQuery())
         {
            assertTrue(result.next());
            shareId = result.getInt(1);
            assertFalse(result.next());
         }
      }
      assertSuccessful(post(OWNER, "editSharedRecord", values(ownedViewId, shareId)));
      assertEquals(ShareScope.READ_WRITE.name(), getShare(shareId).getValueString("scope"));
      assertSuccessful(post(OWNER, "deleteSharedRecord", values(ownedViewId, shareId)));
      assertNull(getShare(shareId));
      assertShareUnchanged();
   }



   /*******************************************************************************
    ** Missing share IDs fail visibly without changing any existing share.
    *******************************************************************************/
   @Test
   void testHttpMissingSharesFail() throws Exception
   {
      assertRejected(post(OWNER, "editSharedRecord", values(ownedViewId, Integer.MAX_VALUE)), "No record was found to update");
      assertRejected(post(OWNER, "deleteSharedRecord", values(ownedViewId, Integer.MAX_VALUE)), "No record was found to delete");
      assertShareUnchanged();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer insertView(String owner, String label) throws Exception
   {
      QRecord record = new InsertAction().execute(new InsertInput(SavedView.TABLE_NAME).withRecordEntity(new SavedView()
         .withUserId(owner).withLabel(label).withTableName(SampleMetaDataProvider.TABLE_NAME_PERSON).withViewJson("{}"))).getRecords().get(0);
      assertTrue(record.getErrors() == null || record.getErrors().isEmpty(), String.valueOf(record.getErrors()));
      assertNotNull(record.getValueInteger("id"));
      return record.getValueInteger("id");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer insertShare(Integer viewId) throws Exception
   {
      QRecord record = new InsertAction().execute(new InsertInput(SharedSavedView.TABLE_NAME).withRecordEntity(new SharedSavedView()
         .withSavedViewId(viewId).withUserId(RECIPIENT).withScope(ShareScope.READ_ONLY.name()))).getRecords().get(0);
      assertTrue(record.getErrors() == null || record.getErrors().isEmpty(), String.valueOf(record.getErrors()));
      assertNotNull(record.getValueInteger("id"));
      return record.getValueInteger("id");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord getShare(Integer shareId) throws Exception
   {
      return new GetAction().executeForRecord(new GetInput(SharedSavedView.TABLE_NAME).withPrimaryKey(shareId));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertShareUnchanged() throws Exception
   {
      QRecord share = getShare(otherShareId);
      assertNotNull(share);
      assertEquals(otherViewId, share.getValueInteger("savedViewId"));
      assertEquals(RECIPIENT, share.getValueString("userId"));
      assertEquals(ShareScope.READ_ONLY.name(), share.getValueString("scope"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunBackendStepInput input(Integer viewId, Integer shareId)
   {
      RunBackendStepInput input = new RunBackendStepInput();
      values(viewId, shareId).forEach(input::addValue);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> values(Integer viewId, Integer shareId)
   {
      return Map.of("tableName", SavedView.TABLE_NAME, "recordId", viewId.toString(), "shareId", shareId.toString(), "scopeId", ShareScope.READ_WRITE.name());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> post(String user, String process, Map<String, String> values) throws Exception
   {
      String body = values.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
         + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
      return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + "/processes/" + process + "/run"))
         .header("Cookie", "sessionId=" + user).header("Content-Type", "application/x-www-form-urlencoded")
         .timeout(Duration.ofSeconds(15)).POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertSuccessful(HttpResponse<String> response)
   {
      assertEquals(200, response.statusCode(), response.body());
      JSONObject result = new JSONObject(response.body());
      assertFalse(result.has("error"), response.body());
      assertFalse(result.has("jobUUID"), "This synchronous sharing process must finish before readback: " + response.body());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertRejected(HttpResponse<String> response, String expectedMessage)
   {
      JSONObject result = new JSONObject(response.body());
      assertTrue(result.has("error"), response.body());
      assertFalse(result.has("jobUUID"), response.body());
      assertTrue(response.body().contains(expectedMessage), response.body());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QSession session(String user)
   {
      return new QSession().withUser(new QUser().withIdReference(user)).withPermissions(Set.of());
   }



   /*******************************************************************************
    ** Model a display-only reference remapping without changing stored records.
    *******************************************************************************/
   public static class DisplayShareReference implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         records.forEach(record -> record.setValue("savedViewId", displayedViewId));
         return records;
      }
   }
}
