/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.tables.query.VirtualFieldValueProviderInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for TableExportSpecV1
 *******************************************************************************/
class TableExportSpecV1Test extends SpecTestBase
{

   /*******************************************************************************
    ** HTTP downloads execute selected providers and omit unrequested provider work.
    *******************************************************************************/
   @Test
   void exportsSelectedProvider() throws Exception
   {
      HttpExportValueProvider.calls.set(0);
      serverQInstance.getTable("person").withVirtualField(new QVirtualFieldMetaData("provided", QFieldType.STRING).withLabel("Provided")
         .withIsQuerySelectable(true).withValueProvider(new QCodeReference(HttpExportValueProvider.class)));
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("format", "csv", "fieldNames", List.of("provided"))))
         .asString();
      assertEquals(200, response.getStatus());
      try(CSVParser csv = CSVParser.parse(response.getBody(), CSVFormat.DEFAULT.withFirstRecordAsHeader()))
      {
         assertEquals(List.of("Provided"), csv.getHeaderNames());
         List<CSVRecord> records = csv.getRecords();
         assertThat(records).isNotEmpty();
         assertThat(records).allMatch(record -> record.get("Provided").matches("record-[0-9]+"));
      }
      int calls = HttpExportValueProvider.calls.get();
      assertThat(calls).isPositive();

      HttpResponse<String> omitted = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("format", "csv", "fieldNames", List.of("firstName"))))
         .asString();
      assertEquals(200, omitted.getStatus());
      assertThat(omitted.getBody()).doesNotContain("Provided", "record-");
      assertEquals(calls, HttpExportValueProvider.calls.get());
   }



   /*******************************************************************************
    ** Execute in the HTTP request's query context.
    *******************************************************************************/
   public static class HttpExportValueProvider implements VirtualFieldValueProviderInterface
   {
      private static final AtomicInteger calls = new AtomicInteger();

      @Override
      public void fill(QTableMetaData table, String fieldName, List<QRecord> records, QueryInput input)
      {
         calls.incrementAndGet();
         for(QRecord record : records)
         {
            record.setValue(fieldName, "record-" + record.getValue(table.getPrimaryKeyField()));
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableExportSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCsvExport()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of(
            "format", "csv",
            "filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Kelkhoff"))
         )))
         .asString();

      assertEquals(200, response.getStatus());
      assertThat(response.getHeaders().getFirst("Content-Type")).contains("text/csv");
      assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("person.csv");

      String body = response.getBody();
      assertThat(body).contains("Kelkhoff");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCsvExportWithCustomFilename()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of(
            "format", "csv",
            "filename", "my-people.csv"
         )))
         .asString();

      assertEquals(200, response.getStatus());
      assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("my-people.csv");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExportTableNotFound()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/notAnActualTable/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("format", "csv")))
         .asString();

      assertEquals(403, response.getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExportInvalidFormat()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("format", "invalidFormat")))
         .asString();

      assertThat(response.getStatus()).isIn(400, 500);
   }



   /*******************************************************************************
    ** Test JSON export format to verify multi-format support.
    *******************************************************************************/
   @Test
   void testJsonExport()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of(
            "format", "json",
            "filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Kelkhoff"))
         )))
         .asString();

      assertEquals(200, response.getStatus());
      assertThat(response.getHeaders().getFirst("Content-Type")).contains("json");
      assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("person.json");
      assertThat(response.getBody()).contains("Kelkhoff");
   }



   /*******************************************************************************
    ** Test export with specific field names subset.
    *******************************************************************************/
   @Test
   void testExportWithFieldNames()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of(
            "format", "csv",
            "fieldNames", List.of("firstName", "lastName")
         )))
         .asString();

      assertEquals(200, response.getStatus());
      String body = response.getBody();
      assertThat(body).contains("First Name");
      assertThat(body).contains("Last Name");
   }

}
