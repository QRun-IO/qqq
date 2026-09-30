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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableAudienceType;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedviews.QuickSavedView;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedView;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedViewsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedviews.SharedSavedView;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.ShareScope;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.SharingMetaDataProvider;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Ordinary users exercise configured saved-view security, sharing and quick views.
 *******************************************************************************/
class SampleSavedViewAccessContractTest
{
   private static final String OWNER = UUID.randomUUID().toString();
   private static final String RECIPIENT = UUID.randomUUID().toString();
   private static final String OTHER = UUID.randomUUID().toString();
   private static final String USER_KEY = "savedViewUser";
   private static final String USERS = "sharingUser";

   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final HttpClient client = HttpClient.newHttpClient();
   private SampleJavalinServer server;
   private Integer viewId;



   /*******************************************************************************
    ** Bind actual user IDs to keys; there is no all-access key or privileged seed.
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.addSecurityKeyType(new QSecurityKeyType().withName(USER_KEY));
      instance.addPossibleValueSource(TablesPossibleValueSourceMetaDataProvider.defineTablesPossibleValueSource(instance));
      new SavedViewsMetaDataProvider().withIsQuickSavedViewEnabled(true)
         .withUserLevelRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(USER_KEY).withFieldName("userId"))
         .defineAll(instance, SampleMetaDataProvider.RDBMS_BACKEND_NAME, table ->
         {
            table.setBackendDetails(new RDBMSTableBackendDetails().withTableName(QInstanceEnricher.inferBackendName(table.getName())));
            QInstanceEnricher.setInferredFieldBackendNames(table);
         });
      instance.getTable(SavedView.TABLE_NAME).setShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName(SharedSavedView.TABLE_NAME)
         .withAssetIdFieldName("savedViewId").withScopeFieldName("scope").withThisTableOwnerIdFieldName("userId")
         .withAudienceType(new ShareableAudienceType().withName("user").withFieldName("userId")));
      instance.addTable(new QTableMetaData().withName(USERS).withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("sharing_user")).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.STRING)).withField(new QFieldMetaData("name", QFieldType.STRING))
         .withRecordLabelFields("name").withRecordLabelFormat("%s"));
      instance.getTable(SavedView.TABLE_NAME).getShareableTableMetaData().getAudienceTypes().get("user").setSourceTableName(USERS);
      new SharingMetaDataProvider().defineAll(instance, null);
      instance.getAuthentication().setCustomizer(new QCodeReference(ViewUser.class));
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
         statement.execute("DROP TABLE IF EXISTS quick_saved_view");
         statement.execute("DROP TABLE IF EXISTS saved_view");
         statement.execute("DROP TABLE IF EXISTS sharing_user");
         statement.execute("CREATE TABLE saved_view (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, label VARCHAR(250), table_name VARCHAR(250), user_id VARCHAR(250), view_json TEXT)");
         statement.execute("CREATE TABLE shared_saved_view (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, saved_view_id INTEGER, user_id VARCHAR(250), scope VARCHAR(30), UNIQUE(saved_view_id, user_id))");
         statement.execute("CREATE TABLE quick_saved_view (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, saved_view_id INTEGER, user_id VARCHAR(250), label VARCHAR(250), sort_order INTEGER, do_count BOOLEAN, UNIQUE(saved_view_id, user_id))");
         statement.execute("CREATE TABLE sharing_user (id VARCHAR(250) PRIMARY KEY, name VARCHAR(100))");
         try(var insert = connection.prepareStatement("INSERT INTO sharing_user (id, name) VALUES (?, ?)"))
         {
            for(String user : List.of(OWNER, RECIPIENT, OTHER))
            {
               insert.setString(1, user);
               insert.setString(2, "Synthetic user");
               insert.executeUpdate();
            }
         }
      }
      QRecord view = new InsertAction().execute(new InsertInput(SavedView.TABLE_NAME).withInputSource(QInputSource.USER)
         .withRecordEntity(new SavedView().withUserId(OWNER).withTableName("person").withLabel("Owner view").withViewJson("{}"))).getRecords().get(0);
      assertClean(view);
      viewId = view.getValueInteger("id");
      assertNotNull(viewId);
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
    **
    *******************************************************************************/
   @Test
   void testPrivateListAndDirectAccess() throws Exception
   {
      assertEquals(1, views(OWNER).length());
      assertEquals(0, views(RECIPIENT).length());
      assertError(post(RECIPIENT, "querySavedView", Map.of("id", viewId.toString())), "not found");
      assertError(get(RECIPIENT, "/data/savedView/" + viewId), "could not find");
   }



   /*******************************************************************************
    ** Grant and revoke through the same endpoints used by an ordinary owner.
    *******************************************************************************/
   @Test
   void testOwnerSharesRecipientReadsAndRevokes() throws Exception
   {
      Integer shareId = share(RECIPIENT);
      assertEquals(1, views(RECIPIENT).length());
      assertSuccess(post(RECIPIENT, "querySavedView", Map.of("id", viewId.toString())));
      assertSuccess(get(RECIPIENT, "/data/savedView/" + viewId));
      assertEquals(0, views(OTHER).length());
      revoke(shareId);
      assertEquals(0, views(RECIPIENT).length());
      assertError(post(RECIPIENT, "querySavedView", Map.of("id", viewId.toString())), "not found");
      assertError(get(RECIPIENT, "/data/savedView/" + viewId), "could not find");
      assertEquals(1, views(OWNER).length());
   }



   /*******************************************************************************
    ** A read recipient cannot edit/delete the asset or manage its shares.
    *******************************************************************************/
   @Test
   void testRecipientCannotMutateAssetOrShares() throws Exception
   {
      Integer shareId = share(RECIPIENT);
      assertFailure(post(RECIPIENT, "storeSavedView", Map.of("id", viewId.toString(), "label", "Taken", "tableName", "person", "viewJson", "{}")));
      assertFailure(post(RECIPIENT, "deleteSavedView", Map.of("id", viewId.toString())));
      assertError(post(RECIPIENT, "insertSharedRecord", shareValues(OTHER)), "not the owner");
      assertError(post(RECIPIENT, "editSharedRecord", shareMutation(shareId)), "not the owner");
      assertError(post(RECIPIENT, "deleteSharedRecord", shareMutation(shareId)), "not the owner");
      assertEquals(1, shareCount());
      assertEquals("Owner view", views(OWNER).getJSONObject(0).getJSONObject("values").getString("label"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQuickPreferencesAreUserScoped() throws Exception
   {
      share(RECIPIENT);
      quick(OWNER, "Owner shortcut", 2, false);
      quick(RECIPIENT, "Recipient shortcut", 7, true);
      JSONObject owner = views(OWNER).getJSONObject(0).getJSONObject("values");
      JSONObject recipient = views(RECIPIENT).getJSONObject(0).getJSONObject("values");
      assertEquals("Owner shortcut", owner.getString("label"));
      assertEquals(2, owner.getInt("sortOrder"));
      assertFalse(owner.getBoolean("doCount"));
      assertEquals("Recipient shortcut", recipient.getString("label"));
      assertEquals(7, recipient.getInt("sortOrder"));
      assertTrue(recipient.getBoolean("doCount"));
      assertEquals(0, views(OTHER).length());
   }



   /*******************************************************************************
    ** Quick-view preferences cannot independently grant access after revocation.
    *******************************************************************************/
   @Test
   void testRevokedQuickPreferenceDoesNotRestoreAccess() throws Exception
   {
      Integer shareId = share(RECIPIENT);
      quick(RECIPIENT, "Shortcut", 1, true);
      revoke(shareId);
      assertEquals(0, views(RECIPIENT).length());
      assertError(post(RECIPIENT, "querySavedView", Map.of("id", viewId.toString())), "not found");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMultipleRecipientsDoNotDuplicateViews() throws Exception
   {
      share(RECIPIENT);
      share(OTHER);
      assertEquals(2, shareCount());
      assertEquals(1, views(OWNER).length());
      assertEquals(1, views(RECIPIENT).length());
      assertEquals(1, views(OTHER).length());
   }



   /*******************************************************************************
    ** Validate directory membership and the existing per-recipient uniqueness rule.
    *******************************************************************************/
   @Test
   void testInvalidRecipientAndDuplicateShareRejected() throws Exception
   {
      assertFailure(post(OWNER, "insertSharedRecord", shareValues("unknown-user")));
      assertEquals(0, shareCount());
      share(RECIPIENT);
      assertFailure(post(OWNER, "insertSharedRecord", shareValues(RECIPIENT)));
      assertEquals(1, shareCount());
   }



   /*******************************************************************************
    ** Hiding the owner's field must not prevent legitimate management.
    *******************************************************************************/
   @Test
   void testHiddenOwnerCanShareAndRevoke() throws Exception
   {
      QContext.getQInstance().getTable(SavedView.TABLE_NAME).getField("userId").setIsHidden(true);
      revoke(share(RECIPIENT));
      assertEquals(0, shareCount());
   }



   /*******************************************************************************
    ** A presentation customizer must not promote a recipient into a share owner.
    *******************************************************************************/
   @Test
   void testPresentedOwnerCannotAuthorizeRecipientSharing() throws Exception
   {
      share(RECIPIENT);
      QContext.getQInstance().getTable(SavedView.TABLE_NAME).withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(PresentCurrentUserAsOwner.class));
      assertError(post(RECIPIENT, "insertSharedRecord", shareValues(OTHER)), "not the owner");
      assertEquals(1, shareCount());
   }



   /*******************************************************************************
    ** The same constraint must protect direct actions, not only the share process.
    *******************************************************************************/
   @Test
   void testPresentedOwnerCannotAuthorizeDirectShareWrites() throws Exception
   {
      share(RECIPIENT);
      QContext.getQInstance().getTable(SavedView.TABLE_NAME).withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(PresentCurrentUserAsOwner.class));
      QContext.setQSession(session(RECIPIENT));
      QRecord record = new InsertAction().execute(new InsertInput(SharedSavedView.TABLE_NAME).withInputSource(QInputSource.USER)
         .withRecordEntity(new SharedSavedView().withSavedViewId(viewId).withUserId(OTHER).withScope(ShareScope.READ_ONLY.name()))).getRecords().get(0);
      assertTrue(record.getErrors() != null && !record.getErrors().isEmpty(), "Recipient must not create a share through presentation");
      assertEquals(1, shareCount());
   }



   /*******************************************************************************
    ** Generic share edits are sparse; they must retain their verified parent link.
    *******************************************************************************/
   @Test
   void testOwnerCanEditShareAndRevoke() throws Exception
   {
      Integer shareId = share(RECIPIENT);
      assertSuccess(post(OWNER, "editSharedRecord", shareMutation(shareId)));
      revoke(shareId);
      assertEquals(0, shareCount());
   }



   /*******************************************************************************
    ** Directory-free metadata supports the generic API with application-supplied IDs.
    *******************************************************************************/
   @Test
   void testDefaultAudienceCanShareWithoutDirectory() throws Exception
   {
      QContext.getQInstance().getTable(SavedView.TABLE_NAME).getShareableTableMetaData().getAudienceTypes().get("user").setSourceTableName(null);
      revoke(share(RECIPIENT));
      assertEquals(0, shareCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDisabledSharingDoesNotRegisterSharingMetadata() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      new SavedViewsMetaDataProvider().withIsShareSavedViewEnabled(false).defineAll(instance, SampleMetaDataProvider.MEMORY_BACKEND_NAME, null);
      assertNull(instance.getTable(SavedView.TABLE_NAME).getShareableTableMetaData());
      assertNull(instance.getTable(SharedSavedView.TABLE_NAME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHeavyOwnerCanShareAndRevoke() throws Exception
   {
      QContext.getQInstance().getTable(SavedView.TABLE_NAME).getField("userId").setIsHeavy(true);
      revoke(share(RECIPIENT));
      assertEquals(0, shareCount());
   }



   /*******************************************************************************
    ** Direct share mutations remain owner-only even when recipients can read the view.
    *******************************************************************************/
   @Test
   void testRecipientCannotDirectlyEditOrDeleteShares() throws Exception
   {
      Integer shareId = share(RECIPIENT);
      QContext.setQSession(session(RECIPIENT));
      QRecord update = new UpdateAction().execute(new UpdateInput(SharedSavedView.TABLE_NAME).withInputSource(QInputSource.USER)
         .withRecord(new QRecord().withValue("id", shareId).withValue("savedViewId", viewId).withValue("scope", ShareScope.READ_WRITE.name()))).getRecords().get(0);
      assertTrue(update.getErrors() != null && !update.getErrors().isEmpty(), update.getErrorsAsString());
      var deletion = new DeleteAction().execute(new DeleteInput(SharedSavedView.TABLE_NAME).withInputSource(QInputSource.USER).withPrimaryKey(shareId));
      assertEquals(0, deletion.getDeletedRecordCount());
      assertEquals(1, shareCount());
   }



   /*******************************************************************************
    ** The generic scope enum does not override saved-view ownership policy.
    *******************************************************************************/
   @Test
   void testReadWriteScopeDoesNotTransferViewOwnership() throws Exception
   {
      Integer shareId = share(RECIPIENT);
      Map<String, String> values = new HashMap<>(shareMutation(shareId));
      values.put("scopeId", ShareScope.READ_WRITE.name());
      assertSuccess(post(OWNER, "editSharedRecord", values));
      assertEquals(1, views(RECIPIENT).length());
      assertFailure(post(RECIPIENT, "storeSavedView", Map.of("id", viewId.toString(), "label", "Taken", "tableName", "person", "viewJson", "{}")));
      assertFailure(post(RECIPIENT, "deleteSavedView", Map.of("id", viewId.toString())));
      assertEquals("Owner view", views(OWNER).getJSONObject(0).getJSONObject("values").getString("label"));
      revoke(shareId);
   }



   /*******************************************************************************
    ** Joined authorization sees the caller's uncommitted state and leaves it open.
    *******************************************************************************/
   @Test
   void testJoinedOwnerLookupUsesCallerTransaction() throws Exception
   {
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend())))
      {
         try(var statement = transaction.getConnection().prepareStatement("UPDATE saved_view SET user_id = ? WHERE id = ?"))
         {
            statement.setString(1, OTHER);
            statement.setInt(2, viewId);
            assertEquals(1, statement.executeUpdate());
         }
         QRecord record = new InsertAction().execute(new InsertInput(SharedSavedView.TABLE_NAME).withInputSource(QInputSource.USER).withTransaction(transaction)
            .withRecordEntity(new SharedSavedView().withSavedViewId(viewId).withUserId(RECIPIENT).withScope(ShareScope.READ_ONLY.name()))).getRecords().get(0);
         assertTrue(record.getErrors() != null && !record.getErrors().isEmpty(), record.getErrorsAsString());
         try(var statement = transaction.getConnection().prepareStatement("SELECT user_id FROM saved_view WHERE id = ?"))
         {
            statement.setInt(1, viewId);
            try(var row = statement.executeQuery())
            {
               assertTrue(row.next());
               assertEquals(OTHER, row.getString(1));
            }
         }
         assertEquals(1, views(OWNER).length());
         assertEquals(0, shareCount());
         transaction.rollback();
      }
      revoke(share(RECIPIENT));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void seedCleanup() throws Exception
   {
      share(RECIPIENT);
      share(OTHER);
      quick(OWNER, "Owner", 1, false);
      quick(RECIPIENT, "Recipient", 2, true);
      quick(OTHER, "Other", 3, false);
      QContext.setQSession(session(OWNER));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int rowCount(String table) throws Exception
   {
      assertTrue(List.of("saved_view", "shared_saved_view", "quick_saved_view").contains(table));
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         var statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table); var row = statement.executeQuery())
      {
         assertTrue(row.next());
         return row.getInt(1);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer share(String recipient) throws Exception
   {
      assertSuccess(post(OWNER, "insertSharedRecord", shareValues(recipient)));
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         var statement = connection.prepareStatement("SELECT id FROM shared_saved_view WHERE saved_view_id = ? AND user_id = ?"))
      {
         statement.setInt(1, viewId);
         statement.setString(2, recipient);
         try(var row = statement.executeQuery())
         {
            assertTrue(row.next());
            return row.getInt(1);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void revoke(Integer shareId) throws Exception
   {
      assertSuccess(post(OWNER, "deleteSharedRecord", shareMutation(shareId)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int shareCount() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         var statement = connection.prepareStatement("SELECT count(*) FROM shared_saved_view");
         var row = statement.executeQuery())
      {
         assertTrue(row.next());
         return row.getInt(1);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void quick(String user, String label, int order, boolean count) throws Exception
   {
      QContext.setQSession(session(user));
      QRecord record = new InsertAction().execute(new InsertInput(QuickSavedView.TABLE_NAME).withInputSource(QInputSource.USER)
         .withRecordEntity(new QuickSavedView().withSavedViewId(viewId).withUserId(user).withLabel(label).withSortOrder(order).withDoCount(count))).getRecords().get(0);
      assertClean(record);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONArray views(String user) throws Exception
   {
      return assertSuccess(post(user, "querySavedView", Map.of("tableName", "person"))).getJSONObject("values").getJSONArray("savedViewList");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> shareValues(String recipient)
   {
      return Map.of("tableName", SavedView.TABLE_NAME, "recordId", viewId.toString(), "audienceType", "user", "audienceId", recipient, "scopeId", ShareScope.READ_ONLY.name());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> shareMutation(Integer shareId)
   {
      return Map.of("tableName", SavedView.TABLE_NAME, "recordId", viewId.toString(), "shareId", shareId.toString(), "scopeId", ShareScope.READ_ONLY.name());
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
   private HttpResponse<String> get(String user, String path) throws Exception
   {
      return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + path)).header("Cookie", "sessionId=" + user)
         .timeout(Duration.ofSeconds(15)).build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject assertSuccess(HttpResponse<String> response)
   {
      assertEquals(200, response.statusCode(), response.body());
      JSONObject result = new JSONObject(response.body());
      assertFalse(result.has("error"), response.body());
      assertFalse(result.has("jobUUID"), response.body());
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertFailure(HttpResponse<String> response)
   {
      JSONObject result = new JSONObject(response.body());
      assertTrue(result.has("error"), response.body());
      assertFalse(result.has("jobUUID"), response.body());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertError(HttpResponse<String> response, String message)
   {
      assertFailure(response);
      assertTrue(response.body().toLowerCase().contains(message), response.body());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertClean(QRecord record)
   {
      assertTrue(record.getErrors() == null || record.getErrors().isEmpty(), record.getErrorsAsString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QSession session(String user)
   {
      return new QSession().withUser(new QUser().withIdReference(user)).withPermissions(Set.of()).withSecurityKeyValues(Map.of(USER_KEY, List.of(user)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ViewUser extends SampleSavedReportContractTest.ReportUser
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         super.customizeSession(instance, session, context);
         session.setSecurityKeyValues(Map.of(USER_KEY, List.of(session.getUser().getIdReference())));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PresentCurrentUserAsOwner implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         records.forEach(record -> record.setValue("userId", QContext.getQSession().getUser().getIdReference()));
         return records;
      }
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RejectCleanup implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         records.forEach(record -> record.addError(new BadInputStatusMessage("Synthetic cleanup rejection")));
         return records;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ObserveCleanup implements TableCustomizerInterface
   {
      private static final AtomicInteger deleted = new AtomicInteger();

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postDelete(DeleteInput input, List<QRecord> records)
      {
         deleted.addAndGet(records.size());
         return records;
      }
   }

}
