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


import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessCustomizers;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInput;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInputFieldsContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaData;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessObjectOutput;
import com.kingsrook.qqq.api.model.metadata.processes.PreRunApiProcessCustomizer;
import com.kingsrook.qqq.backend.core.actions.permissions.ReportProcessPermissionChecker;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.ProcessFileDownload;
import com.kingsrook.qqq.backend.core.actions.reporting.ListOfMapsExportStreamer;
import com.kingsrook.qqq.backend.core.actions.reporting.customizers.DataSourceQueryInputCustomizer;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.reports.BasicRunReportProcess;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Public download boundaries using owned synthetic files only.
 *******************************************************************************/
class SampleDownloadContractTest
{
   @TempDir
   Path directory;

   private SampleJavalinServer server;
   private ApiProcessMetaData reportApi;
   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
      .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanUp()
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
    ** A client cannot turn an arbitrary server path into a download grant.
    *******************************************************************************/
   @Test
   void testClientSuppliedServerPathIsDenied() throws Exception
   {
      Path privateFile = Files.writeString(directory.resolve("owned-private-marker.txt"), "owned-private-download-marker");
      start(false);
      for(String method : new String[] {"GET", "POST"})
      {
         HttpResponse<String> response = request(method, "/download/marker.txt?filePath=" + URLEncoder.encode(privateFile.toString(), StandardCharsets.UTF_8));
         assertTrue(response.statusCode() >= 400 && response.statusCode() < 500, method + ": " + response.statusCode() + " " + response.body());
         assertFalse(response.body().contains("owned-private-download-marker"));
      }
      assertEquals("owned-private-download-marker", Files.readString(privateFile));
   }



   /*******************************************************************************
    ** A denied table cannot be read through its storage download path.
    *******************************************************************************/
   @Test
   void testDeniedStorageTableCannotBeDownloaded() throws Exception
   {
      Path storedFile = Files.writeString(Files.createDirectories(directory.resolve("cities")).resolve("marker.txt"), "owned-archive-download-marker");
      start(true);
      assertEquals(403, request("GET", "/data/city").statusCode(), "Ordinary table access is the permission control");
      HttpResponse<String> response = request("GET", "/download/marker.txt?storageTableName=city&storageReference=marker.txt");
      assertEquals(403, response.statusCode(), response.body());
      assertFalse(response.body().contains("owned-archive-download-marker"));
      assertEquals("owned-archive-download-marker", Files.readString(storedFile));
   }



   /*******************************************************************************
    ** Relative storage references cannot escape the selected table directory.
    *******************************************************************************/
   @Test
   void testStorageTraversalCannotReadSiblingFile() throws Exception
   {
      Files.createDirectories(directory.resolve("cities"));
      Path privateFile = Files.writeString(directory.resolve("private-marker.txt"), "owned-sibling-download-marker");
      start(false);
      HttpResponse<String> response = request("GET", "/download/marker.txt?storageTableName=city&storageReference=..%2Fprivate-marker.txt");
      assertTrue(response.statusCode() >= 400 && response.statusCode() < 500, response.statusCode() + " " + response.body());
      assertFalse(response.body().contains("owned-sibling-download-marker"));
      assertEquals("owned-sibling-download-marker", Files.readString(privateFile));
   }



   /*******************************************************************************
    ** Real report generation remains downloadable across authenticated requests.
    *******************************************************************************/
   @Test
   void testGeneratedReportDownloadIsScopedToItsSession() throws Exception
   {
      start(false);
      HttpResponse<String> result = request("GET", "/processes/reports.basic/run?reportName=downloadSpecies&reportFormat=CSV&_qStepTimeoutMillis=10000");
      assertEquals(200, result.statusCode(), result.body());
      JSONObject process = new JSONObject(result.body());
      assertFalse(process.has("error"), process.toString());
      assertFalse(process.has("jobUUID"), process.toString());
      String filePath = process.getJSONObject("values").getString("serverFilePath");
      Path report = Path.of(filePath);
      try
      {
         String expected = Files.readString(report);
         assertTrue(expected.contains("Dog") && expected.contains("Cat"), expected);
         String path = "/download/species%20%C3%A9.csv?filePath=" + URLEncoder.encode(filePath, StandardCharsets.UTF_8);
         for(String method : List.of("GET", "POST"))
         {
            HttpResponse<String> response = request(method, path);
            assertEquals(200, response.statusCode(), response.body());
            assertEquals(expected, response.body());
            assertEquals("application/octet-stream", response.headers().firstValue("Content-Type").orElseThrow().split(";")[0]);
            assertEquals("attachment; filename*=UTF-8''species%20%C3%A9.csv", response.headers().firstValue("Content-Disposition").orElseThrow());
         }
         try(HttpClient anotherSession = HttpClient.newHttpClient())
         {
            HttpResponse<String> denied = anotherSession.send(HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + path))
               .timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(403, denied.statusCode(), denied.body());
            assertFalse(denied.body().contains("Dog"));
            assertTrue(denied.headers().firstValue("Content-Disposition").isEmpty());
         }
         assertEquals(403, request("GET", path + ".unregistered").statusCode());
      }
      finally
      {
         Files.deleteIfExists(report);
      }
   }



   /*******************************************************************************
    ** Echoed client values never authorize an unrelated local file.
    *******************************************************************************/
   @Test
   void testForgedProcessOutputCannotGrantDownload() throws Exception
   {
      Path privateFile = Files.writeString(directory.resolve("owned-forged.txt"), "owned-forged-marker");
      start(false);
      String encoded = URLEncoder.encode(privateFile.toString(), StandardCharsets.UTF_8);
      HttpResponse<String> response = request("GET", "/processes/downloadEcho/run?serverFilePath=" + encoded);
      assertEquals(200, response.statusCode(), response.body());
      JSONObject process = new JSONObject(response.body());
      assertFalse(process.has("error"), process.toString());
      assertEquals(privateFile.toString(), process.getJSONObject("values").getString("serverFilePath"));
      response = request("GET", "/download/forged.txt?filePath=" + encoded);
      assertEquals(403, response.statusCode(), response.body());
      assertFalse(response.body().contains("owned-forged-marker"));
   }



   /*******************************************************************************
    ** Storage grants authorize one generated object without table-wide access.
    *******************************************************************************/
   @Test
   void testGeneratedStorageDownloadDoesNotGrantOtherFiles() throws Exception
   {
      Path base = Files.createDirectories(directory.resolve("cities"));
      Files.writeString(base.resolve("other.txt"), "owned-other-storage-marker");
      start(true);
      assertEquals(403, request("GET", "/data/city").statusCode());
      HttpResponse<String> produced = request("GET", "/processes/produceDownload/run");
      assertEquals(200, produced.statusCode(), produced.body());
      assertFalse(new JSONObject(produced.body()).has("error"), produced.body());
      HttpResponse<String> download = request("GET", "/download/report.txt?storageTableName=city&storageReference=produced%2Freport.txt");
      assertEquals(200, download.statusCode(), download.body());
      assertEquals("owned-generated-storage-report", download.body());
      assertEquals(403, request("GET", "/download/other.txt?storageTableName=city&storageReference=other.txt").statusCode());
      try(HttpClient anotherSession = HttpClient.newHttpClient())
      {
         HttpResponse<String> denied = anotherSession.send(HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port()
            + "/download/report.txt?storageTableName=city&storageReference=produced%2Freport.txt")).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
         assertEquals(403, denied.statusCode(), denied.body());
      }
      assertEquals(403, request("GET", "/data/city").statusCode());
      assertEquals("owned-other-storage-marker", Files.readString(base.resolve("other.txt")));
   }



   /*******************************************************************************
    ** Table READ alone cannot establish ownership of an arbitrary storage object.
    *******************************************************************************/
   @Test
   void testTableReadDoesNotGrantArbitraryStorageObjects() throws Exception
   {
      Files.writeString(Files.createDirectories(directory.resolve("cities")).resolve("permitted.txt"), "owned-permitted-storage-marker");
      start(false);
      HttpResponse<String> response = request("GET", "/download/permitted.txt?storageTableName=city&storageReference=permitted.txt");
      assertEquals(403, response.statusCode(), response.body());
      assertFalse(response.body().contains("owned-permitted-storage-marker"));
   }



   /*******************************************************************************
    ** Report permission can authorize a server-defined view without raw table READ.
    *******************************************************************************/
   @Test
   void testNamedReportUsesItsOwnPermission() throws Exception
   {
      QInstance instance = start(true);
      instance.getTable(SampleMetaDataProvider.PetSpecies.NAME).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      assertEquals(403, request("GET", "/data/" + SampleMetaDataProvider.PetSpecies.NAME).statusCode());
      HttpResponse<String> response = request("GET", "/processes/reports.basic/run?reportName=downloadSpecies&reportFormat=CSV&_qStepTimeoutMillis=10000");
      JSONObject result = new JSONObject(response.body());
      JSONObject values = result.optJSONObject("values");
      String path = values == null ? "" : values.optString("serverFilePath");
      try
      {
         assertEquals(200, response.statusCode(), result.toString());
         assertFalse(result.has("error"), result.toString());
         assertFalse(path.isEmpty(), result.toString());
         String csv = Files.readString(Path.of(path));
         assertTrue(csv.contains("Dog") && csv.contains("Cat"), csv);
      }
      finally
      {
         if(!path.isEmpty())
         {
            Files.deleteIfExists(Path.of(path));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFailedReportRemovesTemporaryOutput() throws Exception
   {
      QInstance instance = start(true);
      instance.getTable(SampleMetaDataProvider.PetSpecies.NAME).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getReport("downloadSpecies").getDataSources().getFirst().setQueryInputCustomizer(new QCodeReference(FailingReportQuery.class));
      String reportName = "ownedReportCleanup" + UUID.randomUUID().toString().replace("-", "");
      instance.addReport(instance.getReport("downloadSpecies").withName(reportName));
      Path temporaryDirectory = Path.of(System.getProperty("java.io.tmpdir"));
      try
      {
         HttpResponse<String> response = request("GET", "/processes/reports.basic/run?reportName=" + reportName + "&reportFormat=CSV&_qStepTimeoutMillis=10000");
         JSONObject result = new JSONObject(response.body());
         assertTrue(response.statusCode() >= 400 || result.has("error"), result.toString());
         JSONObject values = result.optJSONObject("values");
         assertTrue(values == null || !values.has("serverFilePath"), result.toString());
         try(var files = Files.newDirectoryStream(temporaryDirectory, reportName + "*.csv"))
         {
            assertFalse(files.iterator().hasNext(), "Failed report left a temporary output file");
         }
      }
      finally
      {
         try(var files = Files.newDirectoryStream(temporaryDirectory, reportName + "*.csv"))
         {
            for(Path file : files)
            {
               Files.deleteIfExists(file);
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHttpExportRejectsUnsupportedFormats() throws Exception
   {
      ListOfMapsExportStreamer.reset();
      try
      {
         start(false);
         for(String format : List.of("PDF", "ownedUnknownFormat"))
         {
            HttpResponse<String> response = request("GET", "/data/" + SampleMetaDataProvider.PetSpecies.NAME + "/export/?format=" + format);
            assertEquals(400, response.statusCode(), response.body());
            assertTrue(ListOfMapsExportStreamer.getList("Sheet 1") == null);
         }
      }
      finally
      {
         ListOfMapsExportStreamer.reset();
      }
   }



   /*******************************************************************************
    ** Direct named reports use report permission rather than raw-table permission.
    *******************************************************************************/
   @Test
   void testHttpNamedReportUsesItsOwnPermission() throws Exception
   {
      Set<Path> temporaryFiles = temporaryReportFiles();
      QInstance instance = start(true);
      instance.getTable(SampleMetaDataProvider.PetSpecies.NAME).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      assertEquals(403, request("GET", "/data/" + SampleMetaDataProvider.PetSpecies.NAME).statusCode());
      HttpResponse<String> response = request("GET", "/reports/downloadSpecies?format=json");
      assertEquals(200, response.statusCode(), response.body());
      assertTrue(response.body().contains("Dog") && response.body().contains("Cat"), response.body());
      assertEquals(temporaryFiles, temporaryReportFiles());
   }



   /*******************************************************************************
    ** Both entry points still reject users lacking the report's own permission.
    *******************************************************************************/
   @Test
   void testNamedReportPermissionIsRequired() throws Exception
   {
      QInstance instance = start(true);
      instance.getReport("downloadSpecies").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      HttpResponse<String> direct = request("GET", "/reports/downloadSpecies?format=json");
      assertEquals(403, direct.statusCode(), direct.body());
      assertFalse(direct.body().contains("Dog"), direct.body());
      HttpResponse<String> process = request("GET", "/processes/reports.basic/run?reportName=downloadSpecies&reportFormat=CSV&_qStepTimeoutMillis=10000");
      JSONObject result = new JSONObject(process.body());
      assertTrue(process.statusCode() >= 400 || result.has("error"), result.toString());
      JSONObject values = result.optJSONObject("values");
      assertTrue(values == null || !values.has("serverFilePath"), result.toString());
   }



   /*******************************************************************************
    ** API report permission checks must see a submitted report name.
    *******************************************************************************/
   @Test
   void testApiNamedReportPermissionIsRequired() throws Exception
   {
      QInstance instance = start(true);
      instance.getReport("downloadSpecies").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      assertNamedReportDenied("?reportName=downloadSpecies");
   }



   /*******************************************************************************
    ** Metadata defaults select the report subject to authorization.
    *******************************************************************************/
   @Test
   void testApiDefaultedNamedReportPermissionIsRequired() throws Exception
   {
      QInstance instance = start(true);
      instance.getReport("downloadSpecies").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      reportApi.getInput().getQueryStringParams().getFields().getFirst().setDefaultValue("downloadSpecies");
      assertNamedReportDenied("");
   }



   /*******************************************************************************
    ** A pre-run customizer's effective report must be authorized before execution.
    *******************************************************************************/
   @Test
   void testApiCustomizedNamedReportPermissionIsRequired() throws Exception
   {
      QInstance instance = start(true);
      instance.getReport("downloadSpecies").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      reportApi.withCustomizer(ApiProcessCustomizers.PRE_RUN.getRole(), new QCodeReference(SelectNamedReport.class));
      assertNamedReportDenied("?reportName=unusedBeforeCustomization");
   }



   /*******************************************************************************
    ** Report-level authorization may grant its configured sources without table READ.
    *******************************************************************************/
   @Test
   void testApiNamedReportUsesItsOwnPermission() throws Exception
   {
      QInstance instance = start(true);
      instance.getTable(SampleMetaDataProvider.PetSpecies.NAME).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getProcess(BasicRunReportProcess.PROCESS_NAME).setPermissionRules(QPermissionRules.defaultInstance()
         .withCustomPermissionChecker(new QCodeReference(CountingReportPermissionChecker.class)));
      CountingReportPermissionChecker.calls = 0;
      HttpResponse<String> response = request("GET", "/named-report-api/2026.Q3/render?reportName=downloadSpecies");
      assertEquals(1, CountingReportPermissionChecker.calls);
      assertEquals(200, response.statusCode(), response.body());
      Path report = Path.of(new JSONObject(response.body()).getString("serverFilePath"));
      try
      {
         HttpResponse<String> download = request("GET", "/download/species.csv?filePath=" + URLEncoder.encode(report.toString(), StandardCharsets.UTF_8));
         assertEquals(200, download.statusCode(), download.body());
         assertTrue(download.body().contains("Dog") && download.body().contains("Cat"), download.body());
         assertEquals(Files.readString(report), download.body());
      }
      finally
      {
         Files.deleteIfExists(report);
      }
   }



   /*******************************************************************************
    ** Standard process denial must precede customizer side effects.
    *******************************************************************************/
   @Test
   void testApiStandardProcessDenialPrecedesCustomizer() throws Exception
   {
      QInstance instance = start(true);
      instance.getProcess(BasicRunReportProcess.PROCESS_NAME).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      reportApi.withCustomizer(ApiProcessCustomizers.PRE_RUN.getRole(), new QCodeReference(SelectNamedReport.class));
      SelectNamedReport.calls = 0;
      HttpResponse<String> response = request("GET", "/named-report-api/2026.Q3/render");
      assertEquals(403, response.statusCode(), response.body());
      assertEquals(0, SelectNamedReport.calls);
   }



   /*******************************************************************************
    ** A regression reports actual downloadable synthetic bytes, not only a status.
    *******************************************************************************/
   private void assertNamedReportDenied(String query) throws Exception
   {
      HttpResponse<String> direct = request("GET", "/reports/downloadSpecies?format=json");
      assertEquals(403, direct.statusCode(), direct.body());
      HttpResponse<String> response = request("GET", "/named-report-api/2026.Q3/render" + query);
      String unexpectedDownload = "";
      if(response.statusCode() == 200)
      {
         Path report = Path.of(new JSONObject(response.body()).getString("serverFilePath"));
         try
         {
            HttpResponse<String> download = request("GET", "/download/species.csv?filePath=" + URLEncoder.encode(report.toString(), StandardCharsets.UTF_8));
            unexpectedDownload = "HTTP " + download.statusCode() + " " + download.body();
         }
         finally
         {
            Files.deleteIfExists(report);
         }
      }
      assertEquals(403, response.statusCode(), response.body() + " Unexpected download: " + unexpectedDownload);
      assertFalse(response.body().contains("serverFilePath"), response.body());
   }



   /*******************************************************************************
    ** A named report preserves the input source selected by its application customizer.
    *******************************************************************************/
   @Test
   void testNamedReportRetainsCustomizerInputSource() throws Exception
   {
      QInstance instance = start(true);
      instance.getReport("downloadSpecies").getDataSources().getFirst().setQueryInputCustomizer(new QCodeReference(UserReportQuery.class));
      UserReportQuery.query.set(null);
      HttpResponse<String> response = request("GET", "/reports/downloadSpecies?format=json");
      assertEquals(200, response.statusCode(), response.body());
      assertTrue(response.body().contains("Dog") && response.body().contains("Cat"), response.body());
      assertEquals(QInputSource.USER, UserReportQuery.query.get().getInputSource());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHttpNamedReportReturnsCompleteJson() throws Exception
   {
      Set<Path> temporaryFiles = temporaryReportFiles();
      start(false);
      HttpResponse<String> response = request("GET", "/reports/downloadSpecies?format=json");
      assertEquals(200, response.statusCode(), response.body());
      assertTrue(new JSONArray(response.body()).length() >= 2);
      assertTrue(response.body().contains("Dog") && response.body().contains("Cat"), response.body());
      assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
      assertEquals("filename=downloadSpecies.json", response.headers().firstValue("Content-Disposition").orElse(""));
      assertEquals(temporaryFiles, temporaryReportFiles());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHttpNamedReportReturnsCompleteWorkbook() throws Exception
   {
      Set<Path> temporaryFiles = temporaryReportFiles();
      start(false);
      HttpResponse<byte[]> response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + "/reports/downloadSpecies/species.xlsx"))
         .timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
      assertEquals(200, response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
      try(XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.body())))
      {
         assertTrue(workbook.getSheetAt(0).getPhysicalNumberOfRows() >= 3);
      }
      assertEquals(temporaryFiles, temporaryReportFiles());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Set<Path> temporaryReportFiles() throws Exception
   {
      try(var files = Files.list(Path.of(System.getProperty("java.io.tmpdir"))))
      {
         return files.filter(path -> path.getFileName().toString().startsWith("qqq-http-report-")).collect(Collectors.toSet());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QInstance start(Boolean denyCity) throws QException
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      String apiName = "namedReportApi" + UUID.randomUUID().toString().replace("-", "");
      instance.withSupplementalMetaData(new ApiInstanceMetaDataContainer().withApiInstanceMetaData(new ApiInstanceMetaData()
         .withName(apiName).withPath("/named-report-api/").withLabel("Named Reports")
         .withDescription("Owned named-report permission fixture").withContactEmail("reports@example.test")
         .withCurrentVersion(new APIVersion("2026.Q3")).withSupportedVersions(List.of(new APIVersion("2026.Q3")))));
      reportApi = new ApiProcessMetaData().withInitialVersion("2026.Q3").withApiProcessName("render")
         .withOverrideProcessIsHidden(true).withMethod(HttpMethod.GET)
         .withInput(new ApiProcessInput().withQueryStringParams(new ApiProcessInputFieldsContainer()
            .withField(new QFieldMetaData("reportName", QFieldType.STRING))
            .withField(new QFieldMetaData("reportFormat", QFieldType.STRING).withDefaultValue("CSV"))))
         .withOutput(new ApiProcessObjectOutput()
            .withOutputField(new QFieldMetaData("serverFilePath", QFieldType.STRING))
            .withOutputField(new QFieldMetaData("downloadFileName", QFieldType.STRING)));
      instance.addProcess(BasicRunReportProcess.defineProcessMetaData()
         .withPermissionRules(QPermissionRules.defaultInstance().withCustomPermissionChecker(new QCodeReference(ReportProcessPermissionChecker.class)))
         .withSupplementalMetaData(new ApiProcessMetaDataContainer().withApiProcessMetaData(apiName, reportApi)));
      instance.addReport(new QReportMetaData().withName("downloadSpecies").withLabel("Species")
         .withProcessName(BasicRunReportProcess.PROCESS_NAME)
         .withDataSources(List.of(new QReportDataSource().withName("species").withSourceTable(SampleMetaDataProvider.PetSpecies.NAME)))
         .withViews(List.of(new QReportView().withName("species").withLabel("Species").withDataSourceName("species").withType(ReportType.TABLE)
            .withColumns(List.of(new QReportField().withName("possibleValueId"), new QReportField().withName("possibleValueLabel"))))));
      instance.addProcess(new QProcessMetaData().withName("downloadEcho")
         .withStep(new QBackendStepMetaData().withName("echo").withCode(new QCodeReference(EchoDownloadStep.class))));
      instance.addProcess(new QProcessMetaData().withName("produceDownload")
         .withStep(new QBackendStepMetaData().withName("produce").withCode(new QCodeReference(ProduceStorageDownloadStep.class))));
      ((FilesystemBackendMetaData) instance.getBackend(SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME)).setBasePath(directory.toString());
      if(denyCity)
      {
         instance.getAuthentication().setCustomizer(new QCodeReference(NoTablePermissions.class));
         instance.getTable(SampleMetaDataProvider.TABLE_NAME_CITY).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      }
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
      return instance;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> request(String method, String path) throws Exception
   {
      return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + path))
         .timeout(Duration.ofSeconds(15)).method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CountingReportPermissionChecker extends ReportProcessPermissionChecker
   {
      private static Integer calls = 0;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void checkPermissionsThrowing(AbstractActionInput input, MetaDataWithPermissionRules metadata) throws QPermissionDeniedException
      {
         calls++;
         super.checkPermissionsThrowing(input, metadata);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class SelectNamedReport implements PreRunApiProcessCustomizer
   {
      private static Integer calls = 0;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void preApiRun(RunProcessInput input)
      {
         calls++;
         input.addValue("reportName", "downloadSpecies");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class UserReportQuery implements DataSourceQueryInputCustomizer
   {
      private static final AtomicReference<QueryInput> query = new AtomicReference<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QueryInput run(ReportInput reportInput, QueryInput queryInput)
      {
         queryInput.setInputSource(QInputSource.USER);
         query.set(queryInput);
         return queryInput;
      }
   }



   /*******************************************************************************
    ** A supported query customizer may reject an invalid input combination.
    *******************************************************************************/
   public static class FailingReportQuery implements DataSourceQueryInputCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QueryInput run(ReportInput reportInput, QueryInput queryInput) throws QException
      {
         throw new QException("Owned report source failure");
      }
   }



   /*******************************************************************************
    ** Ensure mock authentication grants no table permissions in the denied fixture.
    *******************************************************************************/
   public static class NoTablePermissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.setPermissions(Set.of());
      }
   }



   /*******************************************************************************
    ** This fixture intentionally echoes input without granting any file access.
    *******************************************************************************/
   public static class EchoDownloadStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output)
      {
         output.addValue("serverFilePath", input.getValueString("serverFilePath"));
      }
   }



   /*******************************************************************************
    ** Trusted code generates and authorizes an exact owned storage object.
    *******************************************************************************/
   public static class ProduceStorageDownloadStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         StorageInput storage = new StorageInput("city").withReference("produced/report.txt");
         try(OutputStream stream = new StorageAction().createOutputStream(storage))
         {
            stream.write("owned-generated-storage-report".getBytes(StandardCharsets.UTF_8));
         }
         catch(java.io.IOException e)
         {
            throw new QException("Could not create fixture report", e);
         }
         ProcessFileDownload.registerStorage(storage);
         output.addValue("storageTableName", storage.getTableName());
         output.addValue("storageReference", storage.getReference());
      }
   }
}
