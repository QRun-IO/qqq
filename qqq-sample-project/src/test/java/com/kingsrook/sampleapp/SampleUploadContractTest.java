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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.ProcessFileDownload;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import io.javalin.config.SizeUnit;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native multipart upload boundaries with owned City storage and a process fixture.
 *******************************************************************************/
class SampleUploadContractTest
{
   @TempDir
   Path directory;

   private static final AtomicInteger EXECUTIONS = new AtomicInteger();
   private static final AtomicReference<byte[]> FAILED_PROCESS_BYTES = new AtomicReference<>();
   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
   private QApplicationJavalinServer server;
   private SampleUploadTestFixture fixture;



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanUp() throws Exception
   {
      try
      {
         if(fixture != null)
         {
            fixture.close();
         }
      }
      finally
      {
         client.close();
      }
   }



   /*******************************************************************************
    ** A denied process must not leave uploaded content in its internal archive.
    *******************************************************************************/
   @Test
   void testDeniedProcessDoesNotArchiveFiles() throws Exception
   {
      start(true, "city", false);
      Map<String, String> before = snapshot();
      HttpResponse<byte[]> response = upload("uploadProbe", new Part("owned.txt", "owned-denied-upload".getBytes(StandardCharsets.UTF_8)));
      assertAll(() -> assertEquals(403, response.statusCode(), body(response)),
         () -> assertEquals(before, snapshot(), "Denied requests must not write archive files"),
         () -> assertEquals(0, EXECUTIONS.get()));
   }



   /*******************************************************************************
    ** An unknown process is rejected before creating archive files.
    *******************************************************************************/
   @Test
   void testUnknownProcessDoesNotArchiveFiles() throws Exception
   {
      start(false, "city", false);
      Map<String, String> before = snapshot();
      HttpResponse<byte[]> response = upload("unknownUploadProcess", new Part("owned.txt", "owned-unknown-upload".getBytes(StandardCharsets.UTF_8)));
      assertAll(() -> assertEquals(403, response.statusCode(), body(response)),
         () -> assertEquals(before, snapshot(), "Unknown processes must not write archive files"),
         () -> assertEquals(0, EXECUTIONS.get()));
   }



   /*******************************************************************************
    ** A submitted filename must not escape its generated per-file directory.
    *******************************************************************************/
   @Test
   void testFilenameTraversalCannotOverwriteArchiveSibling() throws Exception
   {
      start(false, "city", false);
      Path target = Files.createDirectories(directory.resolve("cities").resolve(QValueFormatter.formatDate(LocalDate.now()))).resolve("owned-existing.txt");
      Files.writeString(target, "owned-existing-marker");
      Map<String, String> before = snapshot();
      HttpResponse<byte[]> response = upload("uploadProbe", new Part("../../owned-existing.txt", "owned-overwrite-marker".getBytes(StandardCharsets.UTF_8)));
      assertAll(() -> assertTrue(response.statusCode() >= 400 && response.statusCode() < 500, response.statusCode() + " " + body(response)),
         () -> assertEquals("owned-existing-marker", Files.readString(target)),
         () -> assertEquals(before, snapshot()),
         () -> assertEquals(0, EXECUTIONS.get()));
   }



   /*******************************************************************************
    ** All filenames must be checked before the first archive write.
    *******************************************************************************/
   @Test
   void testInvalidSecondFilenameDoesNotLeaveFirstUpload() throws Exception
   {
      start(false, "city", false);
      Map<String, String> before = snapshot();
      HttpResponse<byte[]> response = upload("uploadProbe", new Part("good.txt", new byte[] {1, 2, 3}), new Part("../bad.txt", new byte[] {4, 5, 6}));
      assertAll(() -> assertTrue(response.statusCode() >= 400 && response.statusCode() < 500, response.statusCode() + " " + body(response)),
         () -> assertEquals(before, snapshot()),
         () -> assertEquals(0, EXECUTIONS.get()));
   }



   /*******************************************************************************
    ** Archive storage is internal; a permitted process may consume and grant its files.
    *******************************************************************************/
   @Test
   void testMultipleUploadsPreserveArchiveProcessAndDownloadBytes() throws Exception
   {
      start(false, "city", true);
      String marker = markOwnedDatabase();
      byte[][] contents = {"owned first é".getBytes(StandardCharsets.UTF_8), new byte[] {0, 1, 2, 127, -1}};
      HttpResponse<byte[]> response = upload("uploadProbe", new Part("one é.txt", contents[0]), new Part("two.bin", contents[1]));
      assertEquals(200, response.statusCode(), body(response));
      JSONObject result = new JSONObject(body(response));
      assertFalse(result.has("error"), result.toString());
      JSONObject values = result.getJSONObject("values");
      assertEquals(marker, values.getString("personName"));
      assertEquals(2, values.getJSONArray("receivedBytes").length());
      for(int i = 0; i < contents.length; i++)
      {
         assertArrayEquals(contents[i], Base64.getDecoder().decode(values.getJSONArray("receivedBytes").getString(i)));
         String reference = values.getJSONArray("storageReferences").getString(i);
         assertArrayEquals(contents[i], Files.readAllBytes(directory.resolve("cities").resolve(reference)));
         HttpResponse<byte[]> download = client.send(HttpRequest.newBuilder(uri("/download/owned.bin?storageTableName=city&storageReference="
            + URLEncoder.encode(reference, StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofByteArray());
         assertEquals(200, download.statusCode(), body(download));
         assertArrayEquals(contents[i], download.body());
      }
      assertEquals(2, snapshot().size());
      assertEquals(1, EXECUTIONS.get());
   }



   /*******************************************************************************
    ** A missing archive configuration must report failure and leave no files.
    *******************************************************************************/
   @Test
   void testMissingArchiveConfigurationDoesNotExecute() throws Exception
   {
      start(false, null, false);
      HttpResponse<byte[]> response = upload("uploadProbe", new Part("owned.txt", new byte[] {1}));
      assertTrue(new JSONObject(body(response)).has("error"), body(response));
      assertTrue(snapshot().isEmpty());
      assertEquals(0, EXECUTIONS.get());
   }



   /*******************************************************************************
    ** Request-size refusal is an HTTP client error with no archive side effects.
    *******************************************************************************/
   @Test
   void testOversizedUploadDoesNotArchiveOrExecute() throws Exception
   {
      start(false, "city", true);
      HttpResponse<byte[]> response = upload("uploadProbe", new Part("owned-large.bin", new byte[4096]));
      assertAll(() -> assertTrue(response.statusCode() >= 400 && response.statusCode() < 500, response.statusCode() + " " + body(response)),
         () -> assertTrue(snapshot().isEmpty()),
         () -> assertEquals(0, EXECUTIONS.get()));
   }



   /*******************************************************************************
    ** Unknown content length must not bypass the native multipart request limit.
    *******************************************************************************/
   @Test
   void testOversizedChunkedUploadDoesNotArchiveOrExecute() throws Exception
   {
      start(false, "city", true);
      HttpResponse<byte[]> response = upload("uploadProbe", true, new Part("owned-large.bin", new byte[4096]));
      assertAll(() -> assertTrue(response.statusCode() >= 400 && response.statusCode() < 500, response.statusCode() + " " + body(response)),
         () -> assertTrue(snapshot().isEmpty()),
         () -> assertEquals(0, EXECUTIONS.get()));
   }



   /*******************************************************************************
    ** Reject names that are unsafe across filesystem providers and platforms.
    *******************************************************************************/
   @Test
   void testUnsafeFilenamesDoNotWriteArchive() throws Exception
   {
      start(false, "city", false);
      for(String filename : List.of("", "   ", ".", "..", "/owned-absolute.txt", "..\\owned-windows.txt", "C:owned-drive.txt"))
      {
         Map<String, String> before = snapshot();
         HttpResponse<byte[]> response = upload("uploadProbe", new Part(filename, new byte[] {1}));
         assertAll(filename, () -> assertEquals(400, response.statusCode(), body(response)),
            () -> assertEquals(before, snapshot()),
            () -> assertEquals(0, EXECUTIONS.get()));
      }
   }



   /*******************************************************************************
    ** V1 init and step uploads traverse native storage and scoped download routes.
    *******************************************************************************/
   @Test
   void testVersionedUploadsPreserveBytesAndDownloadGrants() throws Exception
   {
      start(false, "city", false);
      String marker = markOwnedDatabase();
      byte[] contents = new byte[] {0, 9, 13, 10, 127, -1};
      for(String process : List.of("uploadProbe", "interactiveUpload"))
      {
         String path = "/qqq/v1/processes/" + process + "/init";
         if(process.equals("interactiveUpload"))
         {
            HttpResponse<byte[]> initial = client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
               .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, initial.statusCode(), body(initial));
            path = "/qqq/v1/processes/" + process + "/" + new JSONObject(body(initial)).getString("processUUID") + "/step/input";
         }
         HttpResponse<byte[]> response = uploadTo(path, false, new Part("owned.bin", contents));
         assertEquals(200, response.statusCode(), body(response));
         JSONObject result = new JSONObject(body(response));
         assertFalse(result.has("error"), result.toString());
         JSONObject values = result.getJSONObject("values");
         assertEquals(marker, values.getString("personName"));
         assertArrayEquals(contents, Base64.getDecoder().decode(values.getJSONArray("receivedBytes").getString(0)));
         String reference = values.getJSONArray("storageReferences").getString(0);
         assertArrayEquals(contents, Files.readAllBytes(directory.resolve("cities").resolve(reference)));
         String downloadPath = "/qqq/v1/download/owned.bin?storageTableName=city&storageReference=" + URLEncoder.encode(reference, StandardCharsets.UTF_8);
         HttpResponse<byte[]> downloaded = client.send(HttpRequest.newBuilder(uri(downloadPath)).build(), HttpResponse.BodyHandlers.ofByteArray());
         assertEquals(200, downloaded.statusCode(), body(downloaded));
         assertArrayEquals(contents, downloaded.body());
         try(HttpClient anotherSession = HttpClient.newHttpClient())
         {
            assertEquals(403, anotherSession.send(HttpRequest.newBuilder(uri(downloadPath)).build(), HttpResponse.BodyHandlers.ofByteArray()).statusCode());
         }
         assertEquals(403, client.send(HttpRequest.newBuilder(uri(downloadPath + ".unregistered")).build(), HttpResponse.BodyHandlers.ofByteArray()).statusCode());
      }
      assertEquals(2, EXECUTIONS.get());
      assertEquals(2, snapshot().size());
   }



   /*******************************************************************************
    ** A process that requires files reports its ordinary error for missing input.
    *******************************************************************************/
   @Test
   void testMissingUploadLeavesNoArchive() throws Exception
   {
      start(false, "city", false);
      HttpResponse<byte[]> response = upload("uploadProbe");
      assertEquals(400, response.statusCode(), body(response));
      assertTrue(new JSONObject(body(response)).has("error"), body(response));
      assertTrue(body(response).contains("requires uploaded files"), body(response));
      assertEquals(1, EXECUTIONS.get());
      assertTrue(snapshot().isEmpty());
   }



   /*******************************************************************************
    ** Malformed multipart keeps its client-error status before writes or execution,
    ** including a resumed V1 process whose pending step must remain usable.
    *******************************************************************************/
   @Test
   void testMalformedMultipartDoesNotArchiveOrExecute() throws Exception
   {
      start(false, "city", false);
      HttpResponse<byte[]> initialized = client.send(HttpRequest.newBuilder(uri("/qqq/v1/processes/interactiveUpload/init"))
         .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofByteArray());
      assertEquals(200, initialized.statusCode(), body(initialized));
      String stepPath = "/qqq/v1/processes/interactiveUpload/" + new JSONObject(body(initialized)).getString("processUUID") + "/step/input";
      List<Integer> statuses = new ArrayList<>();
      for(String path : List.of("/processes/uploadProbe/run", "/qqq/v1/processes/uploadProbe/init", stepPath))
      {
         HttpResponse<byte[]> response = client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
            .header("Content-Type", "multipart/form-data; boundary=OwnedMissingBoundary")
            .POST(HttpRequest.BodyPublishers.ofString("not a multipart body")).build(), HttpResponse.BodyHandlers.ofByteArray());
         statuses.add(response.statusCode());
         assertTrue(new JSONObject(body(response)).has("error"), body(response));
         assertEquals(0, EXECUTIONS.get());
         assertTrue(snapshot().isEmpty());
      }
      assertEquals(List.of(400, 400, 400), statuses, "Malformed multipart status: legacy run, V1 init, V1 step");
      byte[] bytes = "owned valid retry".getBytes(StandardCharsets.UTF_8);
      HttpResponse<byte[]> retried = uploadTo(stepPath, false, new Part("retry.txt", bytes));
      assertEquals(200, retried.statusCode(), body(retried));
      JSONObject values = new JSONObject(body(retried)).getJSONObject("values");
      assertArrayEquals(bytes, Base64.getDecoder().decode(values.getJSONArray("receivedBytes").getString(0)));
      assertArrayEquals(bytes, Files.readAllBytes(directory.resolve("cities").resolve(values.getJSONArray("storageReferences").getString(0))));
      assertEquals(1, EXECUTIONS.get());
      assertEquals(1, snapshot().size());
   }



   /*******************************************************************************
    ** Documented archive retention is not rollback: the application owns cleanup.
    *******************************************************************************/
   @Test
   void testProcessFailureRetainsArchiveWithoutDownloadGrant() throws Exception
   {
      start(false, "city", false);
      Files.writeString(directory.resolve("existing.txt"), "owned-preserved-marker");
      Map<String, String> before = snapshot();
      byte[] bytes = "owned failure payload".getBytes(StandardCharsets.UTF_8);
      HttpResponse<byte[]> response = upload("failingUpload", new Part("failure.txt", bytes));
      assertEquals(200, response.statusCode(), body(response));
      assertTrue(new JSONObject(body(response)).has("error"), body(response));
      assertTrue(body(response).contains("owned failure after storage read"), body(response));
      assertArrayEquals(bytes, FAILED_PROCESS_BYTES.get());
      assertEquals(1, EXECUTIONS.get());
      Map<String, String> after = snapshot();
      after.keySet().removeAll(before.keySet());
      assertEquals(1, after.size());
      Path archived = directory.resolve(after.keySet().iterator().next());
      assertArrayEquals(bytes, Files.readAllBytes(archived));
      String reference = directory.resolve("cities").relativize(archived).toString();
      HttpResponse<byte[]> denied = client.send(HttpRequest.newBuilder(uri("/download/failure.txt?storageTableName=city&storageReference="
         + URLEncoder.encode(reference, StandardCharsets.UTF_8))).build(), HttpResponse.BodyHandlers.ofByteArray());
      assertEquals(403, denied.statusCode(), body(denied));
      Files.delete(archived);
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** A later native filesystem write failure retains the earlier accepted file.
    ** Only the owned retained file is removed; an existing sibling survives.
    *******************************************************************************/
   @Test
   void testLaterStorageFailureRetainsEarlierArchiveForCleanup() throws Exception
   {
      start(false, "city", false);
      Files.writeString(directory.resolve("existing.txt"), "owned-preserved-marker");
      Map<String, String> before = snapshot();
      byte[] bytes = "owned first archived file".getBytes(StandardCharsets.UTF_8);
      HttpResponse<byte[]> response = upload("uploadProbe", new Part("first.txt", bytes), new Part("x".repeat(300) + ".txt", new byte[] {2}));
      assertEquals(200, response.statusCode(), body(response));
      assertTrue(new JSONObject(body(response)).has("error"), body(response));
      assertTrue(body(response).contains("File name too long"), body(response));
      assertEquals(0, EXECUTIONS.get());
      Map<String, String> after = snapshot();
      after.keySet().removeAll(before.keySet());
      assertEquals(1, after.size());
      Path archived = directory.resolve(after.keySet().iterator().next());
      assertArrayEquals(bytes, Files.readAllBytes(archived));
      Files.delete(archived);
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Change the actual JDBC row so canonical seed equality cannot mask misrouting.
    *******************************************************************************/
   private String markOwnedDatabase() throws Exception
   {
      String marker = "upload-" + UUID.randomUUID();
      try(PreparedStatement statement = fixture.database().prepareStatement("UPDATE person SET first_name=? WHERE id=1"))
      {
         statement.setString(1, marker);
         assertEquals(1, statement.executeUpdate());
      }
      return marker;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void start(Boolean denied, String archiveTable, Boolean smallRequestLimit) throws QException
   {
      EXECUTIONS.set(0);
      FAILED_PROCESS_BYTES.set(null);
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      ((FilesystemBackendMetaData) instance.getBackend(SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME)).setBasePath(directory.toString());
      instance.getAuthentication().setCustomizer(new QCodeReference(NoPermissions.class));
      instance.getTable("city").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      QJavalinMetaData.ofOrWithNew(instance).setUploadedFileArchiveTableName(archiveTable);
      QProcessMetaData process = new QProcessMetaData().withName("uploadProbe")
         .withStep(new QBackendStepMetaData().withName("inspect").withCode(new QCodeReference(InspectUploadsStep.class)));
      if(denied)
      {
         process.setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      }
      instance.addProcess(process);
      instance.addProcess(new QProcessMetaData().withName("interactiveUpload")
         .withStep(new QFrontendStepMetaData().withName("input"))
         .withStep(new QBackendStepMetaData().withName("inspect").withCode(new QCodeReference(InspectUploadsStep.class))));
      instance.addProcess(new QProcessMetaData().withName("failingUpload")
         .withStep(new QBackendStepMetaData().withName("fail").withCode(new QCodeReference(ReadThenFailUploadStep.class))));
      fixture = new SampleUploadTestFixture(instance);
      server = fixture.server();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      if(smallRequestLimit)
      {
         server.withJavalinConfigCustomizer(config ->
         {
            config.jetty.host = "127.0.0.1";
            config.jetty.multipartConfig.maxTotalRequestSize(1024, SizeUnit.BYTES);
         });
      }
      server.start();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<byte[]> upload(String process, Part... parts) throws Exception
   {
      return upload(process, false, parts);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<byte[]> upload(String process, Boolean chunked, Part... parts) throws Exception
   {
      return uploadTo("/processes/" + process + "/run?_qStepTimeoutMillis=10000", chunked, parts);
   }



   /*******************************************************************************
    ** Reuse the same multipart encoding against native legacy and V1 routes.
    *******************************************************************************/
   private HttpResponse<byte[]> uploadTo(String path, Boolean chunked, Part... parts) throws Exception
   {
      String boundary = "OwnedUpload" + UUID.randomUUID();
      ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
      for(Part part : parts)
      {
         requestBody.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"" + part.filename().replace("\\", "\\\\").replace("\"", "\\\"")
            + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
         requestBody.write(part.content());
         requestBody.write("\r\n".getBytes(StandardCharsets.UTF_8));
      }
      requestBody.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
      return client.send(HttpRequest.newBuilder(uri(path))
         .header("Content-Type", "multipart/form-data; boundary=" + boundary).timeout(Duration.ofSeconds(15))
         .POST(chunked ? HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(requestBody.toByteArray()))
            : HttpRequest.BodyPublishers.ofByteArray(requestBody.toByteArray())).build(), HttpResponse.BodyHandlers.ofByteArray());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private URI uri(String path)
   {
      return URI.create("http://127.0.0.1:" + service.get().port() + path);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String body(HttpResponse<byte[]> response)
   {
      return new String(response.body(), StandardCharsets.UTF_8);
   }



   /*******************************************************************************
    ** Independent archive readback detects partial writes and overwritten files.
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
    **
    *******************************************************************************/
   private record Part(String filename, byte[] content)
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class NoPermissions implements QAuthenticationModuleCustomizerInterface
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
    ** Consume the actual server-created StorageInputs, then grant only those uploads.
    *******************************************************************************/
   public static class InspectUploadsStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         EXECUTIONS.incrementAndGet();
         if(!(input.getValue("files") instanceof List<?> uploads) || uploads.isEmpty())
         {
            throw new QBadRequestException("This fixture requires uploaded files");
         }
         ArrayList<String> bytes = new ArrayList<>();
         ArrayList<String> references = new ArrayList<>();
         for(Object value : uploads)
         {
            StorageInput storage = (StorageInput) value;
            try(InputStream stream = new StorageAction().getInputStream(storage))
            {
               bytes.add(Base64.getEncoder().encodeToString(stream.readAllBytes()));
            }
            catch(java.io.IOException e)
            {
               throw new QException("Could not read owned upload", e);
            }
            references.add(storage.getReference());
            ProcessFileDownload.registerStorage(storage);
         }
         output.addValue("personName", new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(1)).getValueString("firstName"));
         output.addValue("receivedBytes", bytes);
         output.addValue("storageReferences", references);
      }
   }



   /*******************************************************************************
    ** Consume the native archived bytes, then fail before granting any download.
    *******************************************************************************/
   public static class ReadThenFailUploadStep implements BackendStep
   {
      /***************************************************************************
       ** The deliberate failure exercises the documented process error lifecycle.
       ***************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         EXECUTIONS.incrementAndGet();
         StorageInput storage = (StorageInput) ((List<?>) input.getValue("files")).get(0);
         try(InputStream stream = new StorageAction().getInputStream(storage))
         {
            FAILED_PROCESS_BYTES.set(stream.readAllBytes());
         }
         catch(java.io.IOException e)
         {
            throw new QException("Could not read owned failure upload", e);
         }
         throw new QException("owned failure after storage read");
      }
   }
}
