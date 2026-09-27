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
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
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
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedView;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedViewsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.RecordFormat;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Stored ownership of saved assets, including sparse edits and presentation.
 ** Native H2 readback avoids trusting action responses for persistence evidence.
 *******************************************************************************/
class SampleSavedAssetOwnershipContractTest
{
   private static final String OWNER = UUID.randomUUID().toString();
   private static final String OTHER = UUID.randomUUID().toString();
   private static final List<String> TABLES = List.of(SavedView.TABLE_NAME, SavedReport.TABLE_NAME);

   @TempDir
   Path directory;

   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final HttpClient client = HttpClient.newHttpClient();
   private SampleJavalinServer server;



   /*******************************************************************************
    ** Configure real providers without optional locks to isolate owner enforcement.
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
      new SavedReportsMetaDataProvider().defineAll(instance, SampleMetaDataProvider.RDBMS_BACKEND_NAME, SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME, table ->
      {
         if(SavedReportsMetaDataProvider.REPORT_STORAGE_TABLE_NAME.equals(table.getName()))
         {
            table.setBackendDetails(new FilesystemTableBackendDetails().withBasePath("reports").withCardinality(Cardinality.MANY).withRecordFormat(RecordFormat.CSV));
         }
         else
         {
            table.setBackendDetails(new RDBMSTableBackendDetails().withTableName(QInstanceEnricher.inferBackendName(table.getName())));
            QInstanceEnricher.setInferredFieldBackendNames(table);
         }
      });
      ((FilesystemBackendMetaData) instance.getBackend(SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME)).setBasePath(directory.toString());
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
         statement.execute("DROP TABLE IF EXISTS shared_saved_report");
         statement.execute("DROP TABLE IF EXISTS saved_report");
         statement.execute("CREATE TABLE saved_view (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, label VARCHAR(250), table_name VARCHAR(250), user_id VARCHAR(250), view_json TEXT)");
         statement.execute("CREATE TABLE shared_saved_view (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, saved_view_id INTEGER, user_id VARCHAR(250), scope VARCHAR(30), UNIQUE(saved_view_id, user_id))");
         statement.execute("CREATE TABLE saved_report (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, label VARCHAR(250), table_name VARCHAR(250), user_id VARCHAR(250), query_filter_json TEXT, columns_json TEXT, input_fields_json TEXT, pivot_table_json TEXT)");
         statement.execute("CREATE TABLE shared_saved_report (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, saved_report_id INTEGER, user_id VARCHAR(250), scope VARCHAR(30), UNIQUE(saved_report_id, user_id))");
      }
      for(String table : TABLES)
      {
         QRecord record = new InsertAction().execute(new InsertInput(table).withRecord(values(table, 1, "Original").withValue("userId", OWNER))).getRecords().get(0);
         assertClean(record);
         assertEquals(1, record.getValueInteger("id"));
      }
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
    ** A submitted owner must not override ownership already stored in the database.
    *******************************************************************************/
   @Test
   void testNativeCannotClaimOwnedAssets() throws Exception
   {
      QContext.setQSession(session(OTHER));
      assertAll(TABLES.stream().map(table -> () -> rejectUpdate(table, values(table, 1, "Taken").withValue("userId", OTHER))));
   }



   /*******************************************************************************
    ** Omitting owner fields from an otherwise valid update must not bypass ownership.
    *******************************************************************************/
   @Test
   void testNativeCannotOmitOwnerToEditOthersAssets() throws Exception
   {
      QContext.setQSession(session(OTHER));
      assertAll(TABLES.stream().map(table -> () -> rejectUpdate(table, values(table, 1, "Taken"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNativeCannotClearOwnerToEditOthersAssets() throws Exception
   {
      QContext.setQSession(session(OTHER));
      assertAll(TABLES.stream().map(table -> () -> rejectUpdate(table, values(table, 1, "Taken").withValue("userId", null))));
   }



   /*******************************************************************************
    ** The actual owner can update one field without resubmitting report content.
    *******************************************************************************/
   @Test
   void testNativeOwnerSparseUpdates() throws Exception
   {
      assertAll(TABLES.stream().map(table -> () ->
      {
         Map<String, String> contentBefore = reportContent();
         QRecord result = update(table, new QRecord().withValue("id", "1").withValue("label", "Renamed"));
         assertClean(result);
         assertStored(table, 1, OWNER, "Renamed");
         if(SavedReport.TABLE_NAME.equals(table))
         {
            assertFalse(result.getValues().containsKey("columnsJson"));
            assertEquals(contentBefore, reportContent());
         }
      }));
   }



   /*******************************************************************************
    ** A nullable submitted field must never clear an existing owned asset.
    *******************************************************************************/
   @Test
   void testNativeOwnerCannotRemoveOwnership() throws Exception
   {
      assertAll(TABLES.stream().map(table -> () ->
      {
         update(table, values(table, 1, "Original").withValue("userId", null));
         assertStored(table, 1, OWNER, "Original");
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNativeOwnerCannotTransferOwnership() throws Exception
   {
      assertAll(TABLES.stream().map(table -> () -> rejectUpdate(table, values(table, 1, "Taken").withValue("userId", OTHER))));
   }



   /*******************************************************************************
    ** Batch decisions use record identities, including HTTP-style string IDs.
    *******************************************************************************/
   @Test
   void testNativeMixedOwnerBatch() throws Exception
   {
      assertAll(TABLES.stream().map(table -> () ->
      {
         QRecord own = new InsertAction().execute(new InsertInput(table).withRecord(values(table, 2, "Other owned").withValue("userId", OTHER))).getRecords().get(0);
         assertClean(own);
         QContext.setQSession(session(OTHER));
         var output = new UpdateAction().execute(new UpdateInput(table).withInputSource(QInputSource.USER).withRecords(List.of(
            values(table, 2, "Changed mine").withValue("id", "2"), values(table, 1, "Taken").withValue("id", "1"))));
         assertClean(output.getRecords().get(0));
         assertDenied(output.getRecords().get(1));
         assertStored(table, 1, OWNER, "Original");
         assertStored(table, 2, OTHER, "Changed mine");
      }));
   }



   /*******************************************************************************
    ** Presentation must not supply the stored owner used for authorization.
    *******************************************************************************/
   @Test
   void testNativeOwnerPresentationCannotAuthorizeUpdate() throws Exception
   {
      QContext.setQSession(session(OTHER));
      assertAll(TABLES.stream().map(table -> () ->
      {
         QContext.getQInstance().getTable(table).withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(PresentCurrentUserAsOwner.class));
         rejectUpdate(table, values(table, 1, "Taken").withValue("userId", OTHER));
      }));
   }



   /*******************************************************************************
    ** Hidden fields and altered display content must not invalidate sparse reports.
    *******************************************************************************/
   @Test
   void testNativeOwnerCanEditWithPrivatePresentedContent() throws Exception
   {
      for(String table : TABLES)
      {
         QContext.getQInstance().getTable(table).getField("userId").setIsHidden(true);
      }
      QContext.getQInstance().getTable(SavedReport.TABLE_NAME).withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(PresentInvalidReportContent.class));
      testNativeOwnerSparseUpdates();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNativePresentedOwnerCannotAuthorizeDeletion() throws Exception
   {
      QContext.setQSession(session(OTHER));
      assertAll(TABLES.stream().map(table -> () ->
      {
         QContext.getQInstance().getTable(table).withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(PresentCurrentUserAsOwner.class));
         var output = new DeleteAction().execute(new DeleteInput(table).withInputSource(QInputSource.USER).withPrimaryKeys(List.of(1)));
         assertEquals(0, output.getDeletedRecordCount());
         assertEquals(1, output.getRecordsWithErrors().size());
         assertDenied(output.getRecordsWithErrors().get(0));
         assertStored(table, 1, OWNER, "Original");
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNativeHiddenOwnerCannotAuthorizeDeletion() throws Exception
   {
      QContext.setQSession(session(OTHER));
      assertAll(TABLES.stream().map(table -> () ->
      {
         QContext.getQInstance().getTable(table).getField("userId").setIsHidden(true);
         var output = new DeleteAction().execute(new DeleteInput(table).withInputSource(QInputSource.USER).withPrimaryKeys(List.of(1)));
         assertEquals(0, output.getDeletedRecordCount());
         assertEquals(1, output.getRecordsWithErrors().size());
         assertStored(table, 1, OWNER, "Original");
      }));
   }



   /*******************************************************************************
    ** Framework-owned callers cannot bypass the existing source-agnostic policy.
    *******************************************************************************/
   @Test
   void testNativeSystemInputCannotTakeOwnedAssets() throws Exception
   {
      QContext.setQSession(session(OTHER));
      assertAll(TABLES.stream().map(table -> () ->
      {
         QRecord result = new UpdateAction().execute(new UpdateInput(table).withRecord(values(table, 1, "Taken").withValue("userId", OTHER))).getRecords().get(0);
         assertDenied(result);
         assertStored(table, 1, OWNER, "Original");
      }));
   }



   /*******************************************************************************
    ** Deletion depends on ownership, not whether report presentation is valid JSON.
    *******************************************************************************/
   @Test
   void testNativeOwnerCanDeletePrivatePresentedContent() throws Exception
   {
      QContext.getQInstance().getTable(SavedReport.TABLE_NAME).withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(PresentInvalidReportContent.class));
      assertAll(TABLES.stream().map(table -> () ->
      {
         QContext.getQInstance().getTable(table).getField("userId").setIsHidden(true);
         var result = new DeleteAction().execute(new DeleteInput(table).withInputSource(QInputSource.USER).withPrimaryKeys(List.of(1)));
         assertEquals(1, result.getDeletedRecordCount());
         assertTrue(result.getRecordsWithErrors() == null || result.getRecordsWithErrors().isEmpty());
      }));
   }



   /*******************************************************************************
    ** Keep the existing nullable-owner behavior, including report USER_ID defaults.
    *******************************************************************************/
   @Test
   void testNativeUnownedAssetsRetainLegacyMaintenance() throws Exception
   {
      QContext.setQSession(session(OTHER));
      for(String table : TABLES)
      {
         try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
            Statement statement = connection.createStatement())
         {
            statement.executeUpdate("UPDATE " + (SavedView.TABLE_NAME.equals(table) ? "saved_view" : "saved_report") + " SET user_id = NULL WHERE id = 1");
         }
         assertClean(update(table, values(table, 1, "Updated unowned")));
         assertStored(table, 1, SavedView.TABLE_NAME.equals(table) ? null : OTHER, "Updated unowned");
         assertEquals(1, new DeleteAction().execute(new DeleteInput(table).withInputSource(QInputSource.USER).withPrimaryKeys(List.of(1))).getDeletedRecordCount());
      }
   }



   /*******************************************************************************
    ** A private snapshot must use, and must not commit, the caller's transaction.
    *******************************************************************************/
   @Test
   void testNativeCallerTransactionIsPreserved() throws Exception
   {
      QContext.setQSession(session(OTHER));
      for(String table : TABLES)
      {
         try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend())))
         {
            String sqlTable = SavedView.TABLE_NAME.equals(table) ? "saved_view" : "saved_report";
            try(var statement = transaction.getConnection().prepareStatement("UPDATE " + sqlTable + " SET user_id = ? WHERE id = 1"))
            {
               statement.setString(1, OTHER);
               assertEquals(1, statement.executeUpdate());
            }
            QRecord result = new UpdateAction().execute(new UpdateInput(table).withInputSource(QInputSource.USER).withTransaction(transaction)
               .withRecord(new QRecord().withValue("id", 1).withValue("label", "Pending"))).getRecords().get(0);
            assertClean(result);
            try(var statement = transaction.getConnection().prepareStatement("SELECT user_id, label FROM " + sqlTable + " WHERE id = 1");
               var row = statement.executeQuery())
            {
               assertTrue(row.next());
               assertEquals(OTHER, row.getString(1));
               assertEquals("Pending", row.getString(2));
            }
            assertStored(table, 1, OWNER, "Original");
            transaction.rollback();
         }
         assertStored(table, 1, OWNER, "Original");
      }
   }



   /*******************************************************************************
    ** Invalid sparse edits still run content validation and leave native data intact.
    *******************************************************************************/
   @Test
   void testNativeInvalidSparseReportContentIsRejected() throws Exception
   {
      Map<String, String> before = reportContent();
      for(String field : List.of("columnsJson", "queryFilterJson"))
      {
         QRecord result = update(SavedReport.TABLE_NAME, new QRecord().withValue("id", 1).withValue("label", "Bad edit").withValue(field, "invalid JSON"));
         assertTrue(result.getErrors() != null && result.getErrors().stream().anyMatch(error -> error.getMessage().contains(field)), result.getErrorsAsString());
         assertStored(SavedReport.TABLE_NAME, 1, OWNER, "Original");
         assertEquals(before, reportContent());
      }
   }



   /*******************************************************************************
    ** Normalize an explicitly supplied filter without writing back other old fields.
    *******************************************************************************/
   @Test
   void testNativeSubmittedFilterIsNormalized() throws Exception
   {
      Map<String, String> before = reportContent();
      QRecord result = update(SavedReport.TABLE_NAME, new QRecord().withValue("id", 1)
         .withValue("queryFilterJson", " { \"criteria\" : [{\"fieldName\":\"id\",\"operator\":\"EQUALS\",\"values\":[1]}] } "));
      assertClean(result);
      Map<String, String> after = reportContent();
      String storedFilter = after.remove("query_filter_json");
      before.remove("query_filter_json");
      assertEquals(before, after);
      assertEquals(storedFilter.strip(), storedFilter);
      JSONObject criterion = new JSONObject(storedFilter).getJSONArray("criteria").getJSONObject(0);
      assertEquals("id", criterion.getString("fieldName"));
      assertEquals(1, criterion.getJSONArray("values").getInt(0));
      assertFalse(result.getValues().containsKey("columnsJson"));
   }



   /*******************************************************************************
    ** The real CRUD endpoint must use stored ownership for submitted owner changes.
    *******************************************************************************/
   @Test
   void testHttpPatchCannotTakeOwnedAssets() throws Exception
   {
      assertAll(TABLES.stream().map(table -> () ->
      {
         assertError(patch(OTHER, table, new JSONObject(values(table, 1, "Taken").withValue("userId", OTHER).getValues())), "owner");
         assertStored(table, 1, OWNER, "Original");
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHttpOwnerSparsePatch() throws Exception
   {
      for(String table : TABLES)
      {
         Map<String, String> before = reportContent();
         assertSuccess(patch(OWNER, table, new JSONObject().put("label", "HTTP rename")));
         assertStored(table, 1, OWNER, "HTTP rename");
         assertEquals(before, reportContent());
      }
   }



   /*******************************************************************************
    ** Saved-view processes must use the caller's table-write permissions.
    *******************************************************************************/
   @Test
   void testHttpViewProcessesRespectWritePermissions() throws Exception
   {
      QContext.getQInstance().getTable(SavedView.TABLE_NAME).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      assertError(post(OWNER, "storeSavedView", Map.of("id", "1", "label", "Denied", "tableName", "person", "viewJson", "{}")), "permission");
      assertError(post(OWNER, "storeSavedView", Map.of("label", "Denied insert", "tableName", "person", "viewJson", "{}")), "permission");
      assertError(post(OWNER, "deleteSavedView", Map.of("id", "1")), "permission");
      assertStored(SavedView.TABLE_NAME, 1, OWNER, "Original");
   }



   /*******************************************************************************
    ** The public store process must not transfer an existing view to its caller.
    *******************************************************************************/
   @Test
   void testHttpStoreCannotTakeAnotherOwnersView() throws Exception
   {
      assertError(post(OTHER, "storeSavedView", Map.of("id", "1", "label", "Taken", "tableName", "person", "viewJson", "{}")), "owner");
      assertStored(SavedView.TABLE_NAME, 1, OWNER, "Original");
   }



   /*******************************************************************************
    ** Rejected deletion must be visible to the process caller.
    *******************************************************************************/
   @Test
   void testHttpDeleteReportsOwnershipFailure() throws Exception
   {
      assertError(post(OTHER, "deleteSavedView", Map.of("id", "1")), "owner");
      assertStored(SavedView.TABLE_NAME, 1, OWNER, "Original");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHttpMissingViewMutationsFail() throws Exception
   {
      assertError(post(OWNER, "storeSavedView", Map.of("id", "999", "label", "Missing", "tableName", "person", "viewJson", "{}")), "found");
      assertError(post(OWNER, "deleteSavedView", Map.of("id", "999")), "found");
      assertStored(SavedView.TABLE_NAME, 1, OWNER, "Original");
   }



   /*******************************************************************************
    ** USER validation failures must not be returned as a successful store process.
    *******************************************************************************/
   @Test
   void testHttpInvalidNewViewFails() throws Exception
   {
      assertError(post(OWNER, "storeSavedView", Map.of("label", "", "tableName", "person", "viewJson", "{}")), "required");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHttpOwnerViewLifecycle() throws Exception
   {
      JSONObject created = assertSuccess(post(OWNER, "storeSavedView", Map.of("label", "Created", "tableName", "person", "viewJson", "{}")));
      int id = created.getJSONObject("values").getJSONArray("savedViewList").getJSONObject(0).getJSONObject("values").getInt("id");
      assertStored(SavedView.TABLE_NAME, id, OWNER, "Created");
      assertSuccess(post(OWNER, "storeSavedView", Map.of("id", String.valueOf(id), "label", "Renamed", "tableName", "person", "viewJson", "{}")));
      assertStored(SavedView.TABLE_NAME, id, OWNER, "Renamed");
      assertSuccess(post(OWNER, "deleteSavedView", Map.of("id", String.valueOf(id))));
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         var statement = connection.prepareStatement("SELECT count(*) FROM saved_view WHERE id = ?"))
      {
         statement.setInt(1, id);
         try(var result = statement.executeQuery())
         {
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
         }
      }
   }



   /*******************************************************************************
    ** Read every report-content column without framework presentation.
    *******************************************************************************/
   private Map<String, String> reportContent() throws Exception
   {
      Map<String, String> values = new HashMap<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         var statement = connection.prepareStatement("SELECT query_filter_json, columns_json, input_fields_json, pivot_table_json FROM saved_report WHERE id = 1");
         var result = statement.executeQuery())
      {
         assertTrue(result.next());
         for(String field : List.of("query_filter_json", "columns_json", "input_fields_json", "pivot_table_json"))
         {
            values.put(field, result.getString(field));
         }
      }
      return values;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> patch(String user, String table, JSONObject values) throws Exception
   {
      return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + "/data/" + table + "/1"))
         .header("Cookie", "sessionId=" + user).header("Content-Type", "application/json")
         .timeout(Duration.ofSeconds(15)).method("PATCH", HttpRequest.BodyPublishers.ofString(values.toString())).build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord values(String table, int id, String label)
   {
      QRecord record = new QRecord().withValue("id", id).withValue("label", label).withValue("tableName", "person");
      if(SavedView.TABLE_NAME.equals(table))
      {
         record.setValue("viewJson", "{}");
      }
      else
      {
         record.setValue("columnsJson", "{\"columns\":[{\"name\":\"id\"}]}");
         record.setValue("queryFilterJson", "{}");
      }
      return record;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord update(String table, QRecord record) throws Exception
   {
      return new UpdateAction().execute(new UpdateInput(table).withInputSource(QInputSource.USER).withRecord(record)).getRecords().get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void rejectUpdate(String table, QRecord input) throws Exception
   {
      QRecord record = update(table, input);
      assertAll(() -> assertDenied(record), () -> assertStored(table, 1, OWNER, "Original"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertStored(String table, int id, String owner, String label) throws Exception
   {
      String sqlTable = SavedView.TABLE_NAME.equals(table) ? "saved_view" : "saved_report";
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         var statement = connection.prepareStatement("SELECT user_id, label FROM " + sqlTable + " WHERE id = ?"))
      {
         statement.setInt(1, id);
         try(var result = statement.executeQuery())
         {
            assertTrue(result.next());
            assertEquals(owner, result.getString(1));
            assertEquals(label, result.getString(2));
            assertFalse(result.next());
         }
      }
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
   private void assertDenied(QRecord record)
   {
      assertNotNull(record.getErrors());
      assertTrue(record.getErrors().stream().anyMatch(error -> error.getMessage().contains("owner")), record.getErrorsAsString());
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
   private void assertError(HttpResponse<String> response, String expected)
   {
      JSONObject result = new JSONObject(response.body());
      assertTrue(result.has("error"), response.body());
      assertTrue(response.body().toLowerCase().contains(expected), response.body());
      assertFalse(result.has("jobUUID"), response.body());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QSession session(String user)
   {
      return new QSession().withUser(new QUser().withIdReference(user)).withPermissions(Set.of());
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
   public static class PresentInvalidReportContent implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         records.forEach(record -> record.setValue("columnsJson", "presentation-only"));
         return records;
      }
   }
}
