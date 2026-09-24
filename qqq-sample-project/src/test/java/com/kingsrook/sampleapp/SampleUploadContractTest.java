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
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
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
   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
   private SampleJavalinServer server;



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
      byte[][] contents = {"owned first é".getBytes(StandardCharsets.UTF_8), new byte[] {0, 1, 2, 127, -1}};
      HttpResponse<byte[]> response = upload("uploadProbe", new Part("one é.txt", contents[0]), new Part("two.bin", contents[1]));
      assertEquals(200, response.statusCode(), body(response));
      JSONObject result = new JSONObject(body(response));
      assertFalse(result.has("error"), result.toString());
      JSONObject values = result.getJSONObject("values");
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
    **
    *******************************************************************************/
   private void start(Boolean denied, String archiveTable, Boolean smallRequestLimit) throws QException
   {
      EXECUTIONS.set(0);
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
      if(smallRequestLimit)
      {
         server.withJavalinConfigCustomizer(config -> config.jetty.multipartConfig.maxTotalRequestSize(1024, SizeUnit.BYTES));
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
      return client.send(HttpRequest.newBuilder(uri("/processes/" + process + "/run?_qStepTimeoutMillis=10000"))
         .header("Content-Type", "multipart/form-data; boundary=" + boundary).timeout(Duration.ofSeconds(15))
         .POST(chunked ? HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(requestBody.toByteArray()))
            : HttpRequest.BodyPublishers.ofByteArray(requestBody.toByteArray())).build(), HttpResponse.BodyHandlers.ofByteArray());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private URI uri(String path)
   {
      return URI.create("http://localhost:" + service.get().port() + path);
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
         output.addValue("receivedBytes", bytes);
         output.addValue("storageReferences", references);
      }
   }
}
