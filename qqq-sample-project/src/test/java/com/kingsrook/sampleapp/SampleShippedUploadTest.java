/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import com.kingsrook.sampleapp.metadata.UploadedFileArchiveTableMetaDataProducer;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** The sample as shipped (no archive-table override) accepts process file
 ** uploads over the v1 UI API: the person bulk insert upload step stores the
 ** file in the sample's uploaded-file archive table and moves on to the file
 ** mapping screen (QRun-IO/qqq#784).
 *******************************************************************************/
class SampleShippedUploadTest
{
   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final HttpClient               client  = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
   private final String                   session = UUID.randomUUID().toString();
   private SampleUploadTestFixture        fixture;



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
         MemoryRecordStore.getInstance().reset();
      }
   }



   /*******************************************************************************
    ** The shipped javalin metadata names a memory storage table that exists.
    *******************************************************************************/
   @Test
   void testShippedMetaDataNamesTheArchiveTable() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      assertEquals(UploadedFileArchiveTableMetaDataProducer.NAME, QJavalinMetaData.of(instance).getUploadedFileArchiveTableName());

      QTableMetaData table = instance.getTable(UploadedFileArchiveTableMetaDataProducer.NAME);
      assertNotNull(table);
      assertEquals(SampleMetaDataProvider.MEMORY_BACKEND_NAME, table.getBackendName());
      assertTrue(table.getIsHidden());
   }



   /*******************************************************************************
    ** A CSV posted to the v1 bulk insert upload step is archived and read.
    *******************************************************************************/
   @Test
   void testV1BulkInsertUploadSucceeds() throws Exception
   {
      fixture = new SampleUploadTestFixture(SampleMetaDataProvider.defineTestInstance());
      fixture.server().setPort(0);
      fixture.server().withJavalinConfigurationCustomizer(service::set);
      fixture.server().start();

      JSONObject init = processResponse(post("/qqq/v1/processes/person.bulkInsert/init", null, null));
      assertEquals("upload", init.getString("nextStep"), init.toString());
      String processUUID = init.getString("processUUID");

      byte[]     csv    = "First Name,Last Name,Email\nUma,Upload,uma.upload@example.invalid\n".getBytes(StandardCharsets.UTF_8);
      JSONObject upload = processResponse(post("/qqq/v1/processes/person.bulkInsert/" + processUUID + "/step/upload", "people.csv", csv));
      assertEquals("fileMapping", upload.getString("nextStep"), upload.toString());

      QContext.init(fixture.server().getQInstance(), new QSession());
      List<QRecord> archived = new QueryAction().execute(new QueryInput(UploadedFileArchiveTableMetaDataProducer.NAME)).getRecords();
      assertEquals(1, archived.size());
      assertTrue(archived.get(0).getValueString("fileName").endsWith("/people.csv"), archived.get(0).getValueString("fileName"));
      assertArrayEquals(csv, archived.get(0).getValueByteArray("contents"));
   }



   /*******************************************************************************
    ** A process response that is a 200 and not an error.
    *******************************************************************************/
   private static JSONObject processResponse(HttpResponse<String> response)
   {
      assertEquals(200, response.statusCode(), response.body());
      JSONObject result = new JSONObject(response.body());
      assertFalse(result.has("error") || "ERROR".equals(result.optString("type")), result.toString());
      return (result);
   }



   /*******************************************************************************
    ** A multipart post, with the bulk insert file field when a file is given.
    *******************************************************************************/
   private HttpResponse<String> post(String path, String fileName, byte[] content) throws Exception
   {
      String                boundary = "ShippedUpload" + UUID.randomUUID();
      ByteArrayOutputStream body     = new ByteArrayOutputStream();
      body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"stepTimeoutMillis\"\r\n\r\n10000\r\n").getBytes(StandardCharsets.UTF_8));
      body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"values\"\r\n\r\n{\"tableName\":\"person\"}\r\n").getBytes(StandardCharsets.UTF_8));
      if(fileName != null)
      {
         body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"theFile\"; filename=\"" + fileName + "\"\r\nContent-Type: text/csv\r\n\r\n").getBytes(StandardCharsets.UTF_8));
         body.write(content);
         body.write("\r\n".getBytes(StandardCharsets.UTF_8));
      }
      body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
      return (client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + service.get().port() + path))
         .header("Cookie", "sessionId=" + session)
         .header("Content-Type", "multipart/form-data; boundary=" + boundary)
         .timeout(Duration.ofSeconds(20))
         .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString()));
   }
}
