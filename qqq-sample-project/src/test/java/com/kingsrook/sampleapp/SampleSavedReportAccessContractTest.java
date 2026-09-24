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
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedreports.SharedSavedReport;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.ShareScope;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.SharingMetaDataProvider;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.RecordFormat;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Configured saved-report sharing with native records and ordinary user sessions.
 *******************************************************************************/
class SampleSavedReportAccessContractTest
{
   private static final String OWNER = UUID.randomUUID().toString();
   private static final String RECIPIENT = UUID.randomUUID().toString();
   private static final String OTHER = UUID.randomUUID().toString();
   private static final String USER_KEY = "savedReportUser";
   private static final String REVERSE_JOIN = "savedReportJoinSharedSavedReport";

   @TempDir
   Path directory;

   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final HttpClient client = HttpClient.newHttpClient();
   private SampleJavalinServer server;
   private Integer reportId;



   /*******************************************************************************
    ** Providers retain application security configuration through the existing hook.
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.addSecurityKeyType(new QSecurityKeyType().withName(USER_KEY));
      instance.addPossibleValueSource(TablesPossibleValueSourceMetaDataProvider.defineTablesPossibleValueSource(instance));
      new SavedReportsMetaDataProvider().defineAll(instance, SampleMetaDataProvider.MEMORY_BACKEND_NAME,
         SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME, table ->
         {
            if(SavedReportsMetaDataProvider.REPORT_STORAGE_TABLE_NAME.equals(table.getName()))
            {
               table.setBackendDetails(new FilesystemTableBackendDetails().withBasePath("reports")
                  .withCardinality(Cardinality.MANY).withRecordFormat(RecordFormat.CSV));
            }
            else if(List.of(SavedReport.TABLE_NAME, SharedSavedReport.TABLE_NAME).contains(table.getName()))
            {
               table.setBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME);
               table.setBackendDetails(new RDBMSTableBackendDetails().withTableName(QInstanceEnricher.inferBackendName(table.getName())));
               QInstanceEnricher.setInferredFieldBackendNames(table);
               if(SavedReport.TABLE_NAME.equals(table.getName()))
               {
                  table.withRecordSecurityLock(new MultiRecordSecurityLock().withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
                     .withLock(userLock())
                     .withLock(userLock().withLockScope(RecordSecurityLock.LockScope.READ).withFieldName("sharedSavedReport.userId")
                        .withJoinNameChain(List.of(SavedReportsMetaDataProvider.SHARED_SAVED_REPORT_JOIN_SAVED_REPORT))));
               }
               else
               {
                  table.withRecordSecurityLock(new MultiRecordSecurityLock().withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
                     .withLock(userLock().withFieldName("savedReport.userId").withJoinNameChain(List.of(REVERSE_JOIN)))
                     .withLock(userLock().withLockScope(RecordSecurityLock.LockScope.READ)));
               }
            }
         });
      instance.addJoin(instance.getJoin(SavedReportsMetaDataProvider.SHARED_SAVED_REPORT_JOIN_SAVED_REPORT).flip().withName(REVERSE_JOIN));
      new SharingMetaDataProvider().defineAll(instance, null);
      ((FilesystemBackendMetaData) instance.getBackend(SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME)).setBasePath(directory.toString());
      instance.getAuthentication().setCustomizer(new QCodeReference(ReportUser.class));
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
         statement.execute("DROP TABLE IF EXISTS shared_saved_report");
         statement.execute("DROP TABLE IF EXISTS saved_report");
         statement.execute("CREATE TABLE saved_report (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, label VARCHAR(250), table_name VARCHAR(250), user_id VARCHAR(250), query_filter_json TEXT, columns_json TEXT, input_fields_json TEXT, pivot_table_json TEXT)");
         statement.execute("CREATE TABLE shared_saved_report (id INTEGER AUTO_INCREMENT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, saved_report_id INTEGER, user_id VARCHAR(250), scope VARCHAR(30), UNIQUE(saved_report_id, user_id))");
      }
      QRecord report = new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withInputSource(QInputSource.USER)
         .withRecordEntity(new SavedReport().withUserId(OWNER).withLabel("Owned species")
            .withTableName(SampleMetaDataProvider.PetSpecies.NAME).withQueryFilterJson("{}")
            .withColumnsJson("{\"columns\":[{\"name\":\"possibleValueId\"},{\"name\":\"possibleValueLabel\"}]}"))).getRecords().get(0);
      assertClean(report);
      reportId = report.getValueInteger("id");
      assertNotNull(reportId);
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
   void testPrivateQueryGetAndRender() throws Exception
   {
      assertEquals(1, query(OWNER, SavedReport.TABLE_NAME).size());
      assertEquals(0, query(RECIPIENT, SavedReport.TABLE_NAME).size());
      assertFailure(get(RECIPIENT, "/data/savedReport/" + reportId));
      assertFailure(render(RECIPIENT));
      assertFalse(Files.exists(directory.resolve("reports")));
   }



   /*******************************************************************************
    ** A recipient renders with its own session and receives only its own grant.
    *******************************************************************************/
   @Test
   void testOwnerSharesRecipientRendersAndRevokes() throws Exception
   {
      Integer shareId = share(RECIPIENT);
      assertEquals(1, query(RECIPIENT, SavedReport.TABLE_NAME).size());
      assertSuccess(get(RECIPIENT, "/data/savedReport/" + reportId));
      assertEquals(0, query(OTHER, SavedReport.TABLE_NAME).size());
      JSONObject result = assertSuccess(render(RECIPIENT)).getJSONObject("values");
      String reference = result.getString("storageReference");
      byte[] archived = Files.readAllBytes(directory.resolve("reports").resolve(reference));
      String csv = new String(archived, StandardCharsets.UTF_8);
      assertTrue(csv.contains("Dog") && csv.contains("Cat"), csv);
      HttpResponse<String> download = get(RECIPIENT, "/download/shared.csv?storageTableName=reportStorage&storageReference=" + URLEncoder.encode(reference, StandardCharsets.UTF_8));
      assertEquals(200, download.statusCode(), download.body());
      assertArrayEquals(archived, download.body().getBytes(StandardCharsets.UTF_8));
      assertEquals(403, get(OTHER, "/download/shared.csv?storageTableName=reportStorage&storageReference=" + URLEncoder.encode(reference, StandardCharsets.UTF_8)).statusCode());
      revoke(shareId);
      assertEquals(0, query(RECIPIENT, SavedReport.TABLE_NAME).size());
      assertFailure(get(RECIPIENT, "/data/savedReport/" + reportId));
      assertFailure(render(RECIPIENT));
      assertEquals(1, query(OWNER, SavedReport.TABLE_NAME).size());
   }



   /*******************************************************************************
    ** Multiple grants do not duplicate reports or disclose other recipients.
    *******************************************************************************/
   @Test
   void testMultipleRecipientsSeeOnlyOwnGrants() throws Exception
   {
      share(RECIPIENT);
      share(OTHER);
      assertEquals(1, query(OWNER, SavedReport.TABLE_NAME).size());
      assertEquals(2, query(OWNER, SharedSavedReport.TABLE_NAME).size());
      for(String user : List.of(RECIPIENT, OTHER))
      {
         assertEquals(1, query(user, SavedReport.TABLE_NAME).size());
         List<QRecord> grants = query(user, SharedSavedReport.TABLE_NAME);
         assertEquals(1, grants.size());
         assertEquals(user, grants.get(0).getValueString("userId"));
      }
   }



   /*******************************************************************************
    ** The generic label does not transfer ownership of a built-in saved report.
    *******************************************************************************/
   @Test
   void testReadWriteShareStillDeniesRecipientMutation() throws Exception
   {
      Integer shareId = share(RECIPIENT);
      assertSuccess(post(OWNER, "editSharedRecord", Map.of("tableName", SavedReport.TABLE_NAME, "recordId", reportId.toString(), "shareId", shareId.toString(), "scopeId", ShareScope.READ_WRITE.name())));
      assertSuccess(render(RECIPIENT));
      QContext.setQSession(session(RECIPIENT));
      QRecord updated = new UpdateAction().execute(new UpdateInput(SavedReport.TABLE_NAME).withInputSource(QInputSource.USER)
         .withRecord(new QRecord().withValue("id", reportId).withValue("label", "Taken"))).getRecords().get(0);
      assertTrue(updated.getErrors() != null && !updated.getErrors().isEmpty(), updated.toString());
      assertEquals(0, new DeleteAction().execute(new DeleteInput(SavedReport.TABLE_NAME).withInputSource(QInputSource.USER).withPrimaryKey(reportId)).getDeletedRecordCount());
      assertEquals("Owned species", query(OWNER, SavedReport.TABLE_NAME).get(0).getValueString("label"));
   }



   /*******************************************************************************
    ** Generic processes and direct actions both retain parent-owner authorization.
    *******************************************************************************/
   @Test
   void testRecipientCannotManageShares() throws Exception
   {
      Integer shareId = share(RECIPIENT);
      assertFailure(post(RECIPIENT, "insertSharedRecord", shareValues(OTHER)));
      assertFailure(post(RECIPIENT, "deleteSharedRecord", shareMutation(shareId)));
      assertFailure(post(RECIPIENT, "editSharedRecord", shareMutation(shareId)));
      QContext.setQSession(session(RECIPIENT));
      QRecord inserted = new InsertAction().execute(new InsertInput(SharedSavedReport.TABLE_NAME).withInputSource(QInputSource.USER)
         .withRecordEntity(new SharedSavedReport().withSavedReportId(reportId).withUserId(OTHER).withScope(ShareScope.READ_ONLY.name()))).getRecords().get(0);
      assertTrue(inserted.getErrors() != null && !inserted.getErrors().isEmpty(), inserted.toString());
      QRecord updated = new UpdateAction().execute(new UpdateInput(SharedSavedReport.TABLE_NAME).withInputSource(QInputSource.USER)
         .withRecord(new QRecord().withValue("id", shareId).withValue("savedReportId", reportId).withValue("scope", ShareScope.READ_WRITE.name()))).getRecords().get(0);
      assertTrue(updated.getErrors() != null && !updated.getErrors().isEmpty(), updated.toString());
      assertEquals(0, new DeleteAction().execute(new DeleteInput(SharedSavedReport.TABLE_NAME).withInputSource(QInputSource.USER).withPrimaryKey(shareId)).getDeletedRecordCount());
      assertEquals(1, shareCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> query(String user, String table) throws Exception
   {
      QContext.setQSession(session(user));
      return new QueryAction().execute(new QueryInput(table).withInputSource(QInputSource.USER)).getRecords();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> render(String user) throws Exception
   {
      return get(user, "/processes/renderSavedReport/run?recordsParam=recordIds&recordIds=" + reportId + "&reportFormat=CSV&_qStepTimeoutMillis=10000");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RecordSecurityLock userLock()
   {
      return new RecordSecurityLock().withSecurityKeyType(USER_KEY).withFieldName("userId");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer share(String recipient) throws Exception
   {
      assertSuccess(post(OWNER, "insertSharedRecord", shareValues(recipient)));
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         var statement = connection.prepareStatement("SELECT id FROM shared_saved_report WHERE saved_report_id = ? AND user_id = ?"))
      {
         statement.setInt(1, reportId);
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
         var statement = connection.prepareStatement("SELECT count(*) FROM shared_saved_report");
         var row = statement.executeQuery())
      {
         assertTrue(row.next());
         return row.getInt(1);
      }
   }




   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> shareValues(String recipient)
   {
      return Map.of("tableName", SavedReport.TABLE_NAME, "recordId", reportId.toString(), "audienceType", "user", "audienceId", recipient, "scopeId", ShareScope.READ_ONLY.name());
   }




   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> shareMutation(Integer shareId)
   {
      return Map.of("tableName", SavedReport.TABLE_NAME, "recordId", reportId.toString(), "shareId", shareId.toString(), "scopeId", ShareScope.READ_ONLY.name());
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
   public static class ReportUser extends SampleSavedReportContractTest.ReportUser
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



}
