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


import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.api.implementations.savedreports.RenderSavedReportProcessApiCustomizer;
import com.kingsrook.qqq.api.implementations.savedreports.RenderSavedReportProcessApiMetaDataEnricher;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessCustomizers;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaData;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportAction;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.actions.reporting.customizers.DataSourceQueryInputCustomizer;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.QMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.email.EmailMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedreports.RenderedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.RenderedReportStatus;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedreports.ScheduledReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SharedSavedReport;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.savedreports.RenderSavedReportMetaDataProducer;
import com.kingsrook.qqq.backend.core.processes.implementations.savedreports.SavedReportToReportMetaDataAdapter;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.RecordFormat;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Full saved-report process over canonical Pet Species and owned local storage.
 *******************************************************************************/
class SampleSavedReportContractTest
{
   private static final String OWNER = UUID.randomUUID().toString();

   @TempDir
   Path directory;

   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final HttpClient client = HttpClient.newHttpClient();
   private SampleJavalinServer server;
   private SampleScheduledReportContractTest.SmtpSink smtpSink;
   private QInstance instance;
   private Integer reportId;
   private ApiProcessMetaData apiProcess;
   private final String apiName = "reportApi" + UUID.randomUUID().toString().replace("-", "");



   /*******************************************************************************
    ** Use the framework's full provider and process with canonical source data.
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addPossibleValueSource(TablesPossibleValueSourceMetaDataProvider.defineTablesPossibleValueSource(instance));
      new SavedReportsMetaDataProvider().defineAll(instance, SampleMetaDataProvider.MEMORY_BACKEND_NAME,
         SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME, table ->
         {
            if(SavedReportsMetaDataProvider.REPORT_STORAGE_TABLE_NAME.equals(table.getName()))
            {
               table.setBackendDetails(new FilesystemTableBackendDetails().withBasePath("reports")
                  .withCardinality(Cardinality.MANY).withRecordFormat(RecordFormat.CSV));
            }
         });
      ((FilesystemBackendMetaData) instance.getBackend(SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME)).setBasePath(directory.toString());
      instance.getAuthentication().setCustomizer(new QCodeReference(ReportUser.class));
      instance.getTable("city").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getTable(SavedReportsMetaDataProvider.REPORT_STORAGE_TABLE_NAME).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.withSupplementalMetaData(new ApiInstanceMetaDataContainer().withApiInstanceMetaData(new ApiInstanceMetaData()
         .withName(apiName).withPath("/report-api/").withLabel("Sample Reports")
         .withDescription("Owned sample report acceptance API").withContactEmail("reports@example.test")
         .withCurrentVersion(new APIVersion("2026.Q3")).withSupportedVersions(List.of(new APIVersion("2026.Q3")))));
      apiProcess = RenderSavedReportProcessApiMetaDataEnricher.setupProcessForApi(instance.getProcess("renderSavedReport"), apiName, "2026.Q3");
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
      QContext.init(instance, new QSession().withUser(new QUser().withIdReference(OWNER)));
      QRecord saved = new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withRecordEntity(new SavedReport()
         .withLabel("Owned Species Report")
         .withUserId(OWNER)
         .withTableName(SampleMetaDataProvider.PetSpecies.NAME)
         .withColumnsJson("{\"columns\":[{\"name\":\"possibleValueId\"},{\"name\":\"possibleValueLabel\"}]}")
         .withQueryFilterJson("{}"))).getRecords().get(0);
      assertTrue(saved.getErrors() == null || saved.getErrors().isEmpty(), String.valueOf(saved.getErrors()));
      reportId = saved.getValueInteger("id");
      assertNotNull(reportId);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanUp() throws Exception
   {
      try
      {
         if(server != null)
         {
            server.stop();
         }
         if(smtpSink != null)
         {
            smtpSink.close();
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
    ** Generate, persist completion, grant and download the exact native CSV bytes.
    *******************************************************************************/
   @Test
   void testSavedReportProcessArchivesAndDownloadsBytes() throws Exception
   {
      HttpResponse<byte[]> response = render(OWNER, "");
      assertEquals(200, response.statusCode(), body(response));
      JSONObject result = new JSONObject(body(response));
      assertFalse(result.has("error"), result.toString());
      verifyGeneratedReport(result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void verifyGeneratedReport(JSONObject result) throws Exception
   {
      JSONObject values = result.getJSONObject("values");
      assertEquals("reportStorage", values.getString("storageTableName"));
      String reference = values.getString("storageReference");
      byte[] archived = Files.readAllBytes(directory.resolve("reports").resolve(reference));
      String csv = new String(archived, StandardCharsets.UTF_8);
      assertTrue(csv.contains("\"Dog\"") && csv.contains("\"Cat\""), csv);
      assertEquals(3, csv.lines().count());
      String downloadPath = "/download/owned.csv?storageTableName=reportStorage&storageReference=" + URLEncoder.encode(reference, StandardCharsets.UTF_8);
      HttpResponse<byte[]> download = request(OWNER, downloadPath);
      assertEquals(200, download.statusCode(), body(download));
      assertArrayEquals(archived, download.body());
      assertEquals(403, request(UUID.randomUUID().toString(), downloadPath).statusCode());
      QRecord rendered = new GetAction().execute(new GetInput(RenderedReport.TABLE_NAME).withPrimaryKey(values.getInt("renderedReportId"))).getRecord();
      assertEquals(RenderedReportStatus.COMPLETE.getId(), rendered.getValueInteger("renderedReportStatusId"));
      assertEquals(2, rendered.getValueInteger("rowCount"));
      assertEquals(reference, rendered.getValueString("resultPath"));
   }



   /*******************************************************************************
    ** Explicit read sharing remains a supported way to render another user's report.
    *******************************************************************************/
   @Test
   void testExplicitSharedReportRemainsRenderable() throws Exception
   {
      String recipient = UUID.randomUUID().toString();
      QRecord share = new InsertAction().execute(new InsertInput(SharedSavedReport.TABLE_NAME).withRecordEntity(new SharedSavedReport()
         .withSavedReportId(reportId).withUserId(recipient).withScope("READ_ONLY"))).getRecords().get(0);
      assertNotNull(share.getValueInteger("id"), String.valueOf(share.getErrors()));
      HttpResponse<byte[]> response = render(recipient, "");
      assertEquals(200, response.statusCode(), body(response));
      JSONObject result = new JSONObject(body(response));
      assertFalse(isError(result), result.toString());
      String reference = result.getJSONObject("values").getString("storageReference");
      byte[] archived = Files.readAllBytes(directory.resolve("reports").resolve(reference));
      HttpResponse<byte[]> download = request(recipient, "/download/shared.csv?storageTableName=reportStorage&storageReference="
         + URLEncoder.encode(reference, StandardCharsets.UTF_8));
      assertEquals(200, download.statusCode(), body(download));
      assertArrayEquals(archived, download.body());
   }



   /*******************************************************************************
    ** Automatic record loading must enforce the user's table READ permission.
    *******************************************************************************/
   @Test
   void testReportTableReadPermissionIsRequired() throws Exception
   {
      instance.getTable(SavedReport.TABLE_NAME).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      Map<String, String> before = snapshot();
      HttpResponse<byte[]> response = render(OWNER, "");
      assertAll(() -> assertTrue(response.statusCode() >= 400 || isError(new JSONObject(body(response))), body(response)),
         () -> assertEquals(before, snapshot()));
   }



   /*******************************************************************************
    ** Trusted Java callers retain explicit output references for scheduled jobs.
    *******************************************************************************/
   @Test
   void testTrustedProcessCanChooseOutputReference() throws Exception
   {
      QRecord saved = new GetAction().execute(new GetInput(SavedReport.TABLE_NAME).withPrimaryKey(reportId)).getRecord();
      RunProcessInput input = new RunProcessInput();
      input.setProcessName("renderSavedReport");
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      input.setCallback(QProcessCallbackFactory.forRecord(saved));
      input.addValue("reportFormat", "CSV");
      input.addValue("storageReference", "trusted-owned.csv");
      RunProcessOutput output = new RunProcessAction().execute(input);
      assertEquals("trusted-owned.csv", output.getValueString("storageReference"));
      assertEquals(3, Files.readString(directory.resolve("reports/trusted-owned.csv")).lines().count());
      assertThrows(QPermissionDeniedException.class, () -> RunProcessAction.getStateForUser(input.getProcessUUID(), "renderSavedReport"));
   }



   /*******************************************************************************
    ** The v1 frontend can resume with normal values and retained server defaults.
    *******************************************************************************/
   @Test
   void testVersionedReportInitAndStepPreserveTrustedDefaults() throws Exception
   {
      JSONObject initial = new JSONObject(body(versionedInit(OWNER)));
      assertFalse(isError(initial), initial.toString());
      HttpResponse<byte[]> response = versionedStep(OWNER, initial.getString("processUUID"), new JSONObject().put("reportFormat", "CSV"));
      assertEquals(200, response.statusCode(), body(response));
      JSONObject result = new JSONObject(body(response));
      assertFalse(isError(result), result.toString());
      verifyGeneratedReport(result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<byte[]> versionedInit(String session) throws Exception
   {
      return postForm(session, "/qqq/v1/processes/renderSavedReport/init", "recordsParam=recordIds&recordIds=" + reportId + "&stepTimeoutMillis=10000");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<byte[]> versionedStep(String session, String processUUID, JSONObject values) throws Exception
   {
      return postForm(session, "/qqq/v1/processes/renderSavedReport/" + processUUID + "/step/input", "stepTimeoutMillis=10000&values="
         + URLEncoder.encode(values.toString(), StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<byte[]> postForm(String session, String path, String form) throws Exception
   {
      String boundary = "OwnedReportForm" + UUID.randomUUID();
      StringBuilder body = new StringBuilder();
      for(String field : form.split("&"))
      {
         String[] pair = field.split("=", 2);
         body.append("--").append(boundary).append("\r\nContent-Disposition: form-data; name=\"").append(pair[0]).append("\"\r\n\r\n")
            .append(URLDecoder.decode(pair[1], StandardCharsets.UTF_8)).append("\r\n");
      }
      body.append("--").append(boundary).append("--\r\n");
      return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + path))
         .header("Cookie", "sessionId=" + session).header("Content-Type", "multipart/form-data; boundary=" + boundary)
         .timeout(Duration.ofSeconds(15)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofByteArray());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Boolean isError(JSONObject result)
   {
      return result.has("error") || "ERROR".equals(result.optString("type"));
   }



   /*******************************************************************************
    ** The application API must return the actual archived bytes for the owner.
    *******************************************************************************/
   @Test
   void testApplicationApiOwnerReceivesArchivedReport() throws Exception
   {
      HttpResponse<byte[]> response = apiRender(OWNER, "");
      assertEquals(200, response.statusCode(), body(response));
      assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("csv"));
      assertTrue(body(response).contains("\"Dog\"") && body(response).contains("\"Cat\""), body(response));
      Map<String, String> files = snapshot();
      assertEquals(1, files.size());
      assertArrayEquals(response.body(), Base64.getDecoder().decode(files.values().iterator().next()));
   }



   /*******************************************************************************
    ** Trusted API customizers may still supply server-owned output configuration.
    *******************************************************************************/
   @Test
   void testApplicationApiPreservesTrustedCustomizerConfiguration() throws Exception
   {
      apiProcess.getCustomizers().put(ApiProcessCustomizers.PRE_RUN.getRole(), new QCodeReference(TrustedReportApiCustomizer.class));
      HttpResponse<byte[]> response = apiRender(OWNER, "");
      assertEquals(200, response.statusCode(), body(response));
      assertArrayEquals(response.body(), Files.readAllBytes(directory.resolve("reports/trusted-api.csv")));
   }



   /*******************************************************************************
    ** Omitted fields may use trusted API defaults.
    *******************************************************************************/
   @Test
   void testApplicationApiPreservesTrustedMetadataDefault() throws Exception
   {
      apiProcess.getInput().getQueryStringParams().withField(new QFieldMetaData("storageReference", QFieldType.STRING)
         .withDefaultValue("default-api.csv"));
      HttpResponse<byte[]> response = apiRender(OWNER, "");
      assertEquals(200, response.statusCode(), body(response));
      assertArrayEquals(response.body(), Files.readAllBytes(directory.resolve("reports/default-api.csv")));
   }



   /*******************************************************************************
    ** A completed asynchronous report must only be returned to its initiating user.
    *******************************************************************************/
   @Test
   void testAsyncReportResultIsPrivate() throws Exception
   {
      String path = startAndCompleteAsyncReport();
      HttpResponse<byte[]> response = request(UUID.randomUUID().toString(), path);
      assertEquals(403, response.statusCode(), body(response));
      assertFalse(body(response).contains("Dog"), body(response));
   }



   /*******************************************************************************
    ** Revoking process permission applies to retrieval of an existing result.
    *******************************************************************************/
   @Test
   void testAsyncReportResultRechecksProcessPermission() throws Exception
   {
      String path = startAndCompleteAsyncReport();
      instance.getProcess("renderSavedReport").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      HttpResponse<byte[]> response = request(OWNER, path);
      assertEquals(403, response.statusCode(), body(response));
   }



   /*******************************************************************************
    ** Stored process records cannot be read by another authenticated user.
    *******************************************************************************/
   @Test
   void testSavedReportStateRecordsArePrivate() throws Exception
   {
      JSONObject initial = new JSONObject(body(versionedInit(OWNER)));
      assertFalse(isError(initial), initial.toString());
      String path = "/processes/renderSavedReport/" + initial.getString("processUUID") + "/records";
      HttpResponse<byte[]> owner = request(OWNER, path);
      assertEquals(200, owner.statusCode(), body(owner));
      assertTrue(body(owner).contains("Owned Species Report"), body(owner));
      HttpResponse<byte[]> other = request(UUID.randomUUID().toString(), path);
      assertEquals(403, other.statusCode(), body(other));
      assertFalse(body(other).contains("Owned Species Report"), body(other));
   }



   /*******************************************************************************
    ** A state identifier belongs to one process, even within the same session.
    *******************************************************************************/
   @Test
   void testSavedReportStateCannotChangeProcess() throws Exception
   {
      JSONObject initial = new JSONObject(body(versionedInit(OWNER)));
      assertFalse(isError(initial), initial.toString());
      HttpResponse<byte[]> response = request(OWNER, "/processes/greet/" + initial.getString("processUUID") + "/records");
      assertEquals(403, response.statusCode(), body(response));
   }



   /*******************************************************************************
    ** Failed unauthorized resumes must not modify the owner's saved inputs.
    *******************************************************************************/
   @Test
   void testAnotherUserCannotMutateSavedReportState() throws Exception
   {
      JSONObject initial = new JSONObject(body(versionedInit(OWNER)));
      assertFalse(isError(initial), initial.toString());
      String processUUID = initial.getString("processUUID");
      Map<String, ?> before = new LinkedHashMap<>(RunProcessAction.getState(processUUID).orElseThrow().getValues());
      HttpResponse<byte[]> response = versionedStep(UUID.randomUUID().toString(), processUUID,
         new JSONObject().put("reportFormat", "CSV").put("emailSubject", "unauthorized-modification"));
      assertAll(() -> assertTrue(response.statusCode() >= 400 || isError(new JSONObject(body(response))), body(response)),
         () -> assertEquals(before, RunProcessAction.getState(processUUID).orElseThrow().getValues()),
         () -> assertTrue(snapshot().isEmpty(), snapshot().toString()));
   }



   /*******************************************************************************
    ** Both browser status routes enforce ownership and the job's process identity.
    *******************************************************************************/
   @Test
   void testBrowserStatusRoutesProtectAsyncReport() throws Exception
   {
      String apiPath = startAndCompleteAsyncReport();
      String jobId = apiPath.substring(apiPath.lastIndexOf('/') + 1);
      for(String prefix : List.of("/processes/", "/qqq/v1/processes/"))
      {
         String path = prefix + "renderSavedReport/" + jobId + "/status/" + jobId;
         HttpResponse<byte[]> owner = request(OWNER, path);
         assertEquals(200, owner.statusCode(), body(owner));
         JSONObject result = new JSONObject(body(owner));
         assertFalse(isError(result), result.toString());
         verifyGeneratedReport(result);

         HttpResponse<byte[]> other = request(UUID.randomUUID().toString(), path);
         assertTrue(other.statusCode() == 403 || isError(new JSONObject(body(other))), body(other));
         assertFalse(body(other).contains("storageReference"), body(other));
         HttpResponse<byte[]> wrongProcess = request(OWNER, prefix + "greet/" + jobId + "/status/" + jobId);
         assertTrue(wrongProcess.statusCode() == 403 || isError(new JSONObject(body(wrongProcess))), body(wrongProcess));
         HttpResponse<byte[]> wrongState = request(OWNER, prefix + "renderSavedReport/" + UUID.randomUUID() + "/status/" + jobId);
         assertTrue(wrongState.statusCode() == 403 || isError(new JSONObject(body(wrongState))), body(wrongState));
      }
   }



   /*******************************************************************************
    ** Cancellation is subject to the same ownership boundary as process resume.
    *******************************************************************************/
   @Test
   void testAnotherUserCannotCancelSavedReport() throws Exception
   {
      JSONObject initial = new JSONObject(body(versionedInit(OWNER)));
      assertFalse(isError(initial), initial.toString());
      String processUUID = initial.getString("processUUID");
      Map<String, ?> before = new LinkedHashMap<>(RunProcessAction.getState(processUUID).orElseThrow().getValues());
      String path = "/processes/renderSavedReport/" + processUUID + "/cancel";
      HttpResponse<byte[]> other = request(UUID.randomUUID().toString(), path);
      assertEquals(403, other.statusCode(), body(other));
      assertEquals(before, RunProcessAction.getState(processUUID).orElseThrow().getValues());
      HttpResponse<byte[]> owner = request(OWNER, path);
      assertEquals(200, owner.statusCode(), body(owner));
      assertFalse(isError(new JSONObject(body(owner))), body(owner));
      assertEquals(before, RunProcessAction.getState(processUUID).orElseThrow().getValues());
      assertTrue(snapshot().isEmpty(), snapshot().toString());
   }



   /*******************************************************************************
    ** Unknown cancellation identifiers are rejected without producing output.
    *******************************************************************************/
   @Test
   void testHttpCancellationRejectsUnknownProcessId() throws Exception
   {
      for(String processUUID : List.of(UUID.randomUUID().toString()))
      {
         HttpResponse<byte[]> response = request(OWNER, "/processes/renderSavedReport/" + processUUID + "/cancel");
         assertEquals(400, response.statusCode(), body(response));
      }
      assertTrue(snapshot().isEmpty(), snapshot().toString());
   }



   /*******************************************************************************
    ** A browser can continue using cookies issued by the server on its first visit.
    *******************************************************************************/
   @Test
   void testBrowserCookieJarPreservesProcessOwnership() throws Exception
   {
      CookieManager cookies = new CookieManager();
      try(HttpClient browser = HttpClient.newBuilder().cookieHandler(cookies).build())
      {
         String base = "http://localhost:" + service.get().port();
         HttpResponse<byte[]> initial = browser.send(HttpRequest.newBuilder(URI.create(base
            + "/processes/greetInteractive/init?recordsParam=recordIds&recordIds=1&_qStepTimeoutMillis=10000"))
            .timeout(Duration.ofSeconds(15)).build(), HttpResponse.BodyHandlers.ofByteArray());
         assertEquals(200, initial.statusCode(), body(initial));
         JSONObject result = new JSONObject(body(initial));
         assertFalse(isError(result), result.toString());
         assertFalse(cookies.getCookieStore().getCookies().isEmpty());
         String path = "/processes/greetInteractive/" + result.getString("processUUID") + "/records";
         HttpResponse<byte[]> records = browser.send(HttpRequest.newBuilder(URI.create(base + path))
            .timeout(Duration.ofSeconds(15)).build(), HttpResponse.BodyHandlers.ofByteArray());
         assertEquals(200, records.statusCode(), body(records));
         assertEquals(1, new JSONObject(body(records)).getInt("totalRecords"));
         assertEquals(403, request(UUID.randomUUID().toString(), path).statusCode());
      }
   }



   /*******************************************************************************
    ** Restored authentication stays valid, but application and tenant switches fail.
    *******************************************************************************/
   @Test
   void testProcessStateRejectsApplicationAndBackendVariantChanges() throws Exception
   {
      QSession original = QContext.getQSession();
      original.setIdReference("owned-authentication-" + UUID.randomUUID());
      original.setBackendVariants(new LinkedHashMap<>(Map.of("tenant", "owned-a")));
      RunProcessInput input = new RunProcessInput();
      input.setProcessName("greetInteractive");
      input.setInputSource(QInputSource.USER);
      input.setCallback(QProcessCallbackFactory.forRecord(new GetAction().execute(new GetInput("person").withPrimaryKey(1)).getRecord()));
      new RunProcessAction().execute(input);
      String processUUID = input.getProcessUUID();
      ProcessState state = RunProcessAction.getStateForUser(processUUID, "greetInteractive").orElseThrow();
      QSession restored = original.clone();
      restored.setUuid(UUID.randomUUID().toString());
      try
      {
         QContext.init(instance, restored);
         assertSame(state, RunProcessAction.getStateForUser(processUUID, "greetInteractive").orElseThrow());
         restored.getBackendVariants().put("tenant", "owned-b");
         assertThrows(QPermissionDeniedException.class, () -> RunProcessAction.getStateForUser(processUUID, "greetInteractive"));
         restored.getBackendVariants().put("tenant", "owned-a");
         QContext.init(SampleMetaDataProvider.defineTestInstance(), restored);
         assertThrows(QPermissionDeniedException.class, () -> RunProcessAction.getStateForUser(processUUID, "greetInteractive"));
      }
      finally
      {
         QContext.init(instance, original);
      }
   }



   /*******************************************************************************
    ** A legitimate owner may run the persisted schedule through the HTTP process.
    *******************************************************************************/
   @Test
   void testScheduledReportOwnerCanRun() throws Exception
   {
      Integer scheduleId = insertSchedule(OWNER, "{}");
      HttpResponse<byte[]> response = runSchedule(OWNER, scheduleId);
      assertEquals(200, response.statusCode(), body(response));
      assertFalse(isError(new JSONObject(body(response))), body(response));
      assertEquals(1, snapshot().size());
      String csv = new String(Base64.getDecoder().decode(snapshot().values().iterator().next()), StandardCharsets.UTF_8);
      assertTrue(csv.contains("\"Dog\"") && csv.contains("\"Cat\""), csv);
   }



   /*******************************************************************************
    ** Normal owner edits and deletion remain supported.
    *******************************************************************************/
   @Test
   void testScheduleOwnerCanEditAndDelete() throws Exception
   {
      Integer scheduleId = insertSchedule(OWNER, "{}");
      QRecord result = new UpdateAction().execute(new UpdateInput(ScheduledReport.TABLE_NAME).withInputSource(QInputSource.USER)
         .withRecord(new QRecord().withValue("id", scheduleId).withValue("subject", "updated subject"))).getRecords().get(0);
      assertTrue(result.getErrors() == null || result.getErrors().isEmpty(), String.valueOf(result.getErrors()));
      assertEquals("updated subject", new GetAction().executeForRecord(new GetInput(ScheduledReport.TABLE_NAME).withPrimaryKey(scheduleId)).getValueString("subject"));
      assertEquals(1, new DeleteAction().execute(new DeleteInput(ScheduledReport.TABLE_NAME).withInputSource(QInputSource.USER)
         .withPrimaryKey(scheduleId)).getDeletedRecordCount());
   }



   /*******************************************************************************
    ** A user can create a schedule using the normal current-owner default.
    *******************************************************************************/
   @Test
   void testScheduleCreateDefaultsToCurrentOwner() throws Exception
   {
      QRecord result = new InsertAction().execute(new InsertInput(ScheduledReport.TABLE_NAME).withInputSource(QInputSource.USER)
         .withRecordEntity(schedule(null, "{}"))).getRecords().get(0);
      assertTrue(result.getErrors() == null || result.getErrors().isEmpty(), String.valueOf(result.getErrors()));
      assertEquals(OWNER, new GetAction().executeForRecord(new GetInput(ScheduledReport.TABLE_NAME)
         .withPrimaryKey(result.getValue("id"))).getValueString("userId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ScheduledReport schedule(String owner, String values)
   {
      return new ScheduledReport().withSavedReportId(reportId).withUserId(owner).withFormat("CSV").withInputValues(values)
         .withCronExpression("0 0 0 1 1 ?").withCronTimeZoneId("UTC").withIsActive(false)
         .withToAddresses("reports@example.com").withSubject("Owned sample report");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer insertSchedule(String owner, String values) throws Exception
   {
      if(smtpSink == null)
      {
         smtpSink = new SampleScheduledReportContractTest.SmtpSink(false);
         instance.addMessagingProvider(new EmailMessagingProviderMetaData()
            .withSmtpServer("127.0.0.1").withSmtpPort(String.valueOf(smtpSink.getPort())).withName("owned-schedule-smtp"));
         var inputs = instance.getProcess("renderSavedReport").getBackendStep("pre").getInputMetaData();
         inputs.getFieldThrowing(RenderSavedReportMetaDataProducer.SES_PROVIDER_NAME).setDefaultValue("owned-schedule-smtp");
         inputs.getFieldThrowing(RenderSavedReportMetaDataProducer.FROM_EMAIL_ADDRESS).setDefaultValue("sender@example.com");
      }
      QRecord record = new InsertAction().execute(new InsertInput(ScheduledReport.TABLE_NAME).withRecordEntity(schedule(owner, values))).getRecords().get(0);
      assertTrue(record.getErrors() == null || record.getErrors().isEmpty(), String.valueOf(record.getErrors()));
      assertNotNull(record.getValueInteger("id"));
      return record.getValueInteger("id");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<byte[]> runSchedule(String session, Integer scheduleId) throws Exception
   {
      return request(session, "/processes/runScheduledReport/run?recordsParam=recordIds&recordIds=" + scheduleId + "&_qStepTimeoutMillis=10000");
   }



   /*******************************************************************************
    ** A failed source produces a failed history record without an empty archive.
    *******************************************************************************/
   @Test
   void testFailedSavedReportLeavesNoArchive() throws Exception
   {
      instance.getTable(SampleMetaDataProvider.PetSpecies.NAME).withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(RejectReportSourceQuery.class));
      HttpResponse<byte[]> response = apiRender(OWNER, "");
      assertTrue(response.statusCode() >= 400, body(response));
      assertTrue(body(response).contains("owned-report-source-failure"), body(response));
      List<QRecord> history = QueryAction.execute(RenderedReport.TABLE_NAME, null).stream()
         .filter(record -> reportId.equals(record.getValueInteger("savedReportId"))).toList();
      assertEquals(1, history.size());
      assertEquals(RenderedReportStatus.FAILED.getId(), history.get(0).getValueInteger("renderedReportStatusId"));
      assertNotNull(history.get(0).getValue("endTime"));
      assertTrue(snapshot().isEmpty(), snapshot().toString());
   }



   /*******************************************************************************
    ** Trusted output references must not destroy an earlier successful archive.
    *******************************************************************************/
   @Test
   void testFailedSavedReportPreservesExistingArchive() throws Exception
   {
      apiProcess.getInput().getQueryStringParams().withField(new QFieldMetaData("storageReference", QFieldType.STRING).withDefaultValue("existing.csv"));
      Path file = Files.writeString(Files.createDirectories(directory.resolve("reports")).resolve("existing.csv"), "owned-previous-report");
      instance.getTable(SampleMetaDataProvider.PetSpecies.NAME).withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(RejectReportSourceQuery.class));
      HttpResponse<byte[]> response = apiRender(OWNER, "");
      assertTrue(response.statusCode() >= 400, body(response));
      assertTrue(body(response).contains("owned-report-source-failure"), body(response));
      assertEquals("owned-previous-report", Files.readString(file));
      assertEquals(1, snapshot().size());
   }



   /*******************************************************************************
    ** The existing render-and-deliver lifecycle records delivery failures as FAILED.
    ** Missing sender validation occurs before any messaging provider is invoked.
    *******************************************************************************/
   @Test
   void testEmailFailureMarksRenderedReportFailed() throws Exception
   {
      instance.addMessagingProvider(new QMessagingProviderMetaData().withName("owned-no-network-provider"));
      apiProcess.getInput().getQueryStringParams().withField(new QFieldMetaData("reportDestinationEmailAddress", QFieldType.STRING).withDefaultValue("reports@example.com"));
      HttpResponse<byte[]> response = apiRender(OWNER, "");
      assertTrue(response.statusCode() >= 400, body(response));
      assertTrue(body(response).contains("no from email address"), body(response));
      List<QRecord> history = QueryAction.execute(RenderedReport.TABLE_NAME, null).stream()
         .filter(record -> reportId.equals(record.getValueInteger("savedReportId"))).toList();
      assertEquals(1, history.size());
      assertEquals(RenderedReportStatus.FAILED.getId(), history.get(0).getValueInteger("renderedReportStatusId"));
      assertTrue(Files.readString(directory.resolve("reports").resolve(history.get(0).getValueString("resultPath"))).contains("Dog"));
      assertEquals(1, snapshot().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RejectReportSourceQuery implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records) throws QException
      {
         throw new QException("owned-report-source-failure");
      }
   }



   /*******************************************************************************
    ** A fully authorized joined report returns real native data and archive bytes.
    *******************************************************************************/
   @Test
   void testReportSourceJoinedRowsRemainRenderable() throws Exception
   {
      configurePeopleReport("firstName", "pet.name");
      HttpResponse<byte[]> response = apiRender(OWNER, "");
      verifyApiArchive(response);
      assertTrue(body(response).contains("Avery") && body(response).contains("Charlie"), body(response));
   }



   /*******************************************************************************
    ** Explicit trusted Java rendering keeps the SYSTEM policy for backend work.
    *******************************************************************************/
   @Test
   void testReportSourceTrustedSystemBehaviorRemains() throws Exception
   {
      configurePeopleReport("firstName", "lastName");
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(ReportSourcePrivacy.class));
      QRecord saved = new GetAction().executeForRecord(new GetInput(SavedReport.TABLE_NAME).withPrimaryKey(reportId));
      RunProcessInput input = new RunProcessInput();
      input.setProcessName("renderSavedReport");
      input.setCallback(QProcessCallbackFactory.forRecord(saved));
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      input.addValue("reportFormat", "CSV");
      RunProcessOutput output = new RunProcessAction().execute(input);
      String csv = Files.readString(directory.resolve("reports").resolve(output.getValueString("storageReference")));
      assertTrue(csv.contains("Avery") && csv.contains("Blair"), csv);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ReportSourceQueryCustomizer implements DataSourceQueryInputCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QueryInput run(ReportInput reportInput, QueryInput queryInput)
      {
         queryInput.setInputSource(QInputSource.SYSTEM);
         queryInput.setShouldOmitHiddenFields(false);
         queryInput.setShouldMaskPasswords(false);
         return queryInput;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ReportInput exporterReportInput(QReportMetaData metadata, ReportFormat format, ByteArrayOutputStream destination)
   {
      ReportInput input = new ReportInput();
      input.setInputSource(QInputSource.USER);
      input.setReportMetaData(metadata);
      input.setReportDestination(new ReportDestination().withReportFormat(format).withReportOutputStream(destination));
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static class ReportDestinationProbe extends ByteArrayOutputStream
   {
      private int closes;

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void close()
      {
         closes++;
      }
   }



   /*******************************************************************************
    ** A final page smaller than the pipe buffer must reach the destination.
    *******************************************************************************/
   @Test
   void testTableExportReturnsAllRows() throws Exception
   {
      ReportDestinationProbe destination = new ReportDestinationProbe();
      ExportOutput output = new ExportAction().execute(tableExportInput(destination));
      assertEquals(5, output.getRecordCount());
      assertEquals(6, destination.toString(StandardCharsets.UTF_8).lines().count());
      assertEquals(1, destination.closes);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ExportInput tableExportInput(ReportDestinationProbe destination)
   {
      ExportInput input = new ExportInput();
      input.setTableName("person");
      input.setFieldNames(List.of("firstName", "lastName"));
      input.setInputSource(QInputSource.USER);
      input.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.CSV).withReportOutputStream(destination));
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSummaryJsonUnlabeledFormulaUsesName() throws Exception
   {
      QReportView summary = reportSummaryView("summary", "people").withIncludeTotalRow(true)
         .withColumns(List.of(new QReportField().withName("share").withFormula("=DIVIDE(${pivot.count.id},${total.count.id})")));
      QReportMetaData metadata = new QReportMetaData().withName("ownedUnlabeledFormula")
         .withDataSources(List.of(new QReportDataSource().withName("people").withSourceTable("person")))
         .withViews(List.of(summary));
      JSONArray rows = runSummaryReport(metadata);
      assertEquals(6, rows.length());
      for(int row = 0; row < rows.length() - 1; row++)
      {
         assertEquals(0.2, rows.getJSONObject(row).getDouble("share"), 0.000001);
      }
      assertEquals(1.0, rows.getJSONObject(5).getDouble("share"), 0.000001);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QReportView reportSummaryView(String name, String source)
   {
      return new QReportView().withName(name).withLabel(name).withDataSourceName(source).withType(ReportType.SUMMARY)
         .withSummaryFields(List.of("id")).withIncludeTotalRow(true)
         .withColumns(List.of(new QReportField().withName("count").withLabel("Count").withFormula("${pivot.count.id}")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONArray runSummaryReport(QReportMetaData metadata) throws Exception
   {
      ReportDestinationProbe destination = new ReportDestinationProbe();
      new GenerateReportAction().execute(exporterReportInput(metadata, ReportFormat.JSON, destination));
      assertEquals(1, destination.closes);
      return new JSONArray(destination.toString(StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QReportMetaData countingReport() throws Exception
   {
      configurePeopleReport("firstName", "lastName");
      SavedReport report = new SavedReport(new GetAction().executeForRecord(new GetInput(SavedReport.TABLE_NAME).withPrimaryKey(reportId)));
      return new SavedReportToReportMetaDataAdapter().adapt(report, ReportFormat.CSV);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void configurePeopleReport(String... fields) throws Exception
   {
      instance.getTable("person").setExposedJoins(List.of(new ExposedJoin().withJoinTable("pet").withJoinPath(List.of("personJoinPet"))));
      List<Map<String, String>> columns = java.util.Arrays.stream(fields).map(field -> Map.of("name", field)).toList();
      QRecord updated = new UpdateAction().execute(new UpdateInput(SavedReport.TABLE_NAME).withRecord(new QRecord()
         .withValue("id", reportId).withValue("tableName", "person")
         .withValue("columnsJson", new JSONObject().put("columns", columns).toString()))).getRecords().get(0);
      assertTrue(updated.getErrors() == null || updated.getErrors().isEmpty(), String.valueOf(updated.getErrors()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void verifyApiArchive(HttpResponse<byte[]> response) throws Exception
   {
      assertEquals(200, response.statusCode(), body(response));
      Map<String, String> files = snapshot();
      assertEquals(1, files.size());
      assertArrayEquals(response.body(), Base64.getDecoder().decode(files.values().iterator().next()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ReportSourcePrivacy implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() == QInputSource.USER && Set.of("person", "pet").contains(input.getTableName()))
         {
            QTableMetaData table = input.getTable().clone();
            if("person".equals(input.getTableName()))
            {
               table.getField("firstName").setIsHidden(true);
            }
            else
            {
               table.getField("name").setType(QFieldType.PASSWORD);
            }
            return table;
         }
         return input.getTable();
      }
   }



   /*******************************************************************************
    ** Poll the concrete job created by this test and verify the owner's bytes.
    *******************************************************************************/
   private String startAndCompleteAsyncReport() throws Exception
   {
      HttpResponse<byte[]> initial = apiRender(OWNER, "&async=true");
      assertEquals(202, initial.statusCode(), body(initial));
      String jobId = new JSONObject(body(initial)).getString("jobId");
      String path = "/report-api/2026.Q3/savedReport/renderSavedReport/" + reportId + "/status/" + jobId;
      long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
      HttpResponse<byte[]> result;
      do
      {
         result = request(OWNER, path);
         if(result.statusCode() != 202)
         {
            break;
         }
         Thread.sleep(20);
      }
      while(System.nanoTime() < deadline);
      assertEquals(200, result.statusCode(), body(result));
      assertTrue(body(result).contains("\"Dog\"") && body(result).contains("\"Cat\""), body(result));
      Map<String, String> files = snapshot();
      assertEquals(1, files.size());
      assertArrayEquals(result.body(), Base64.getDecoder().decode(files.values().iterator().next()));
      return path;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<byte[]> apiRender(String session, String query) throws Exception
   {
      return request(session, "/report-api/2026.Q3/savedReport/renderSavedReport/" + reportId + "?reportFormat=CSV" + query);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class TrustedReportApiCustomizer extends RenderSavedReportProcessApiCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void preApiRun(RunProcessInput input) throws QException
      {
         super.preApiRun(input);
         input.addValue("storageReference", "trusted-api.csv");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<byte[]> render(String session, String extraQuery) throws Exception
   {
      return request(session, "/processes/renderSavedReport/run?recordsParam=recordIds&recordIds=" + reportId
         + "&reportFormat=CSV&_qStepTimeoutMillis=10000" + extraQuery);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<byte[]> request(String session, String path) throws Exception
   {
      return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + path))
         .header("Cookie", "sessionId=" + session).timeout(Duration.ofSeconds(15)).build(), HttpResponse.BodyHandlers.ofByteArray());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String body(HttpResponse<byte[]> response)
   {
      return new String(response.body(), StandardCharsets.UTF_8);
   }



   /*******************************************************************************
    ** Independent readback of owned filesystem state catches overwritten objects.
    *******************************************************************************/
   private Map<String, String> snapshot() throws Exception
   {
      Map<String, String> files = new LinkedHashMap<>();
      try(var paths = Files.walk(directory))
      {
         for(Path path : paths.filter(Files::isRegularFile).sorted().toList())
         {
            files.put(directory.relativize(path).toString(), Base64.getEncoder().encodeToString(Files.readAllBytes(path)));
         }
      }
      return files;
   }



   /*******************************************************************************
    ** Stable synthetic users, without table permission grants.
    *******************************************************************************/
   public static class ReportUser implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.getUser().setIdReference(session.getUuid());
         session.setPermissions(Set.of());
      }
   }
}
