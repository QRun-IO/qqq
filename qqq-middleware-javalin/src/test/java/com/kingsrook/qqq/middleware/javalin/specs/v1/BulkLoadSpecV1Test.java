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


import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadAiRoute;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadUploadSources;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** The assisted bulk-load operations over HTTP: profile, suggestMapping and
 ** validate (a dry run; nothing stored).
 *******************************************************************************/
class BulkLoadSpecV1Test extends SpecTestBase
{
   private static final String STORAGE_TABLE = "bulkLoadTestStorage";

   private static final String CSV = """
      First Name,Last Name,Birth Date,Notes
      Darin,Kelkhoff,1980-05-31,one
      Tim,Chamberlain,not a date,two
      """;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new BulkLoadUploadProfileSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new BulkLoadSuggestMappingSpecV1(), new BulkLoadValidateSpecV1()));
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
    ** Also a memory storage table holding one upload, and a source for it.
    ***************************************************************************/
   @Override
   protected void primeTestData(QInstance qInstance) throws Exception
   {
      super.primeTestData(qInstance);
      qInstance.addTable(new QTableMetaData()
         .withName(STORAGE_TABLE)
         .withBackendName(TestUtils.BACKEND_NAME_MEMORY)
         .withPrimaryKeyField("reference")
         .withField(new QFieldMetaData("reference", QFieldType.STRING))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB)));

      QContext.withTemporaryContext(new CapturedContext(qInstance, new QSystemUserSession()), () ->
      {
         try(OutputStream outputStream = new StorageAction().createOutputStream(new StorageInput(STORAGE_TABLE).withReference("uploads/ref-1/people.csv")))
         {
            outputStream.write(CSV.getBytes(StandardCharsets.UTF_8));
         }
      });

      BulkLoadUploadSources.register(ref ->
      {
         if(!"ref-1".equals(ref))
         {
            throw (new QNotFoundException("Upload not found."));
         }
         return (new StorageInput(STORAGE_TABLE).withReference("uploads/ref-1/people.csv"));
      });
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
      BulkLoadUploadSources.clear();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testProfile()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/bulkLoad/uploads/ref-1/profile?tableName=person&sampleCount=2").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject body = new JSONObject(response.getBody());
      assertEquals("people.csv", body.getString("fileName"));
      assertThat(body.getBoolean("hasHeaderRow")).isTrue();
      JSONArray columns = body.getJSONArray("columns");
      assertEquals(4, columns.length());
      assertEquals("firstName", columns.getJSONObject(0).getString("mappedFieldName"));
      assertEquals(2, columns.getJSONObject(0).getJSONArray("sampleValues").length());

      ////////////////////////////////////////////////////////////////
      // the unmapped Notes column gets samples only on a hipaa route //
      ////////////////////////////////////////////////////////////////
      assertThat(columns.getJSONObject(3).optJSONArray("sampleValues")).isNullOrEmpty();
      response = Unirest.get(getBaseUrlAndPath() + "/bulkLoad/uploads/ref-1/profile?tableName=person&sampleCount=2")
         .header(BulkLoadAiRoute.HEADER_NAME, "hipaa").asString();
      assertEquals(2, new JSONObject(response.getBody()).getJSONArray("columns").getJSONObject(3).getJSONArray("sampleValues").length());

      assertEquals(404, Unirest.get(getBaseUrlAndPath() + "/bulkLoad/uploads/nope/profile?tableName=person").asString().getStatus());
      assertEquals(404, Unirest.get(getBaseUrlAndPath() + "/bulkLoad/uploads/ref-1/profile?tableName=notATable").asString().getStatus());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testSuggestAndValidate()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/bulkLoad/person/suggestMapping")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("uploadRef", "ref-1")))
         .asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject suggestion = new JSONObject(response.getBody());
      JSONObject profile    = suggestion.getJSONObject("profile");
      assertEquals("v1", profile.getString("version"));
      assertThat(suggestion.getJSONArray("unmappedColumns").toString()).contains("Notes");

      for(String mode : List.of("SAMPLE", "FULL"))
      {
         response = Unirest.post(getBaseUrlAndPath() + "/bulkLoad/person/validate")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(new JSONObject().put("uploadRef", "ref-1").put("profile", profile).put("mode", mode).toString())
            .asString();
         assertEquals(200, response.getStatus(), response.getBody());
         JSONObject validation = new JSONObject(response.getBody());
         assertEquals(mode, validation.getString("mode"));
         assertEquals(1, validation.getInt("okCount"));
         assertEquals(1, validation.getInt("errorCount"));
         assertEquals("Row 3", validation.getJSONArray("errors").getJSONObject(0).getString("rowNumbers"));
         assertEquals("birthDate", validation.getJSONArray("errors").getJSONObject(0).getString("fieldName"));
      }

      response = Unirest.post(getBaseUrlAndPath() + "/bulkLoad/person/validate")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(new JSONObject().put("uploadRef", "ref-1").put("profile", profile).put("mode", "SOMETIMES").toString())
         .asString();
      assertEquals(400, response.getStatus());
   }

}
