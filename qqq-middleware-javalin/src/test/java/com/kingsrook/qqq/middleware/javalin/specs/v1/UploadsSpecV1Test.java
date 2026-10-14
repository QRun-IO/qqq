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


package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.nio.charset.StandardCharsets;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** POST /qqq/v1/uploads: multipart and raw-body uploads return an UploadRef.
 *******************************************************************************/
class UploadsSpecV1Test extends SpecTestBase
{
   private static final String STORAGE_TABLE = "jobsUploadStorage";



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new UploadsSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    ** The standard test instance plus durable jobs with a memory storage table.
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = TestUtils.defineInstance();
      new DurableJobsMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      instance.addTable(new QTableMetaData()
         .withName(STORAGE_TABLE)
         .withBackendName(TestUtils.BACKEND_NAME_MEMORY)
         .withPrimaryKeyField("reference")
         .withField(new QFieldMetaData("reference", QFieldType.STRING))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB)));
      instance.withSupplementalMetaData(new DurableJobsMetaData().withStorageTableName(STORAGE_TABLE).withMaxUploadBytes(1000L));
      return (instance);
   }



   /*******************************************************************************
    ** A multipart upload returns a ref with the file's name and size.
    *******************************************************************************/
   @Test
   void testMultipartUpload()
   {
      byte[] bytes = "code,name\nk1,One\n".getBytes(StandardCharsets.UTF_8);
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/uploads")
         .cookie("sessionId", "uploads-session")
         .field("file", new java.io.ByteArrayInputStream(bytes), "items.csv")
         .asString();

      assertThat(response.getStatus()).as(response.getBody()).isEqualTo(200);
      JSONObject json = JsonUtils.toJSONObject(response.getBody());
      assertThat(json.getString("ref")).startsWith("up_");
      assertThat(json.getString("fileName")).isEqualTo("items.csv");
      assertThat(json.getLong("sizeBytes")).isEqualTo(bytes.length);
      assertThat(json.getString("expiresAt")).isNotBlank();
   }



   /*******************************************************************************
    ** A raw body upload with fileName works; without fileName it is 400; over
    ** the size limit it is 400.
    *******************************************************************************/
   @Test
   void testRawUploadAndRefusals()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/uploads?fileName=raw.csv")
         .cookie("sessionId", "uploads-session")
         .header("Content-Type", "application/octet-stream")
         .body("a,b\n1,2\n")
         .asString();
      assertThat(response.getStatus()).as(response.getBody()).isEqualTo(200);
      assertThat(JsonUtils.toJSONObject(response.getBody()).getString("fileName")).isEqualTo("raw.csv");

      assertThat(Unirest.post(getBaseUrlAndPath() + "/uploads").cookie("sessionId", "uploads-session").header("Content-Type", "application/octet-stream").body("x").asString().getStatus()).isEqualTo(400);
      assertThat(Unirest.post(getBaseUrlAndPath() + "/uploads?fileName=big.csv").cookie("sessionId", "uploads-session").header("Content-Type", "application/octet-stream").body("x".repeat(2000)).asString().getStatus()).isEqualTo(400);
   }

}
