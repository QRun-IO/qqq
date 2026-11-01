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
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for TableExportSpecV1
 *******************************************************************************/
class TableExportSpecV1Test extends SpecTestBase
{

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
    ** An unsupported format is a bad request naming the format field (#1345).
    *******************************************************************************/
   @Test
   void testExportInvalidFormat()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("format", "pdf")))
         .asString();

      assertEquals(400, response.getStatus(), response.getBody());
      JSONObject body = new JSONObject(response.getBody());
      assertEquals("BAD_REQUEST", body.getString("code"));
      assertThat(body.getString("message")).contains("Unsupported report format: pdf.");
      assertEquals("format", body.getJSONArray("fieldErrors").getJSONObject(0).getString("field"));
   }



   /*******************************************************************************
    ** An unknown field name is a bad request naming the fieldNames field (#1345).
    *******************************************************************************/
   @Test
   void testExportUnknownFieldName()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("format", "csv", "fieldNames", List.of("id", "bogus"))))
         .asString();

      assertEquals(400, response.getStatus(), response.getBody());
      JSONObject fieldError = new JSONObject(response.getBody()).getJSONArray("fieldErrors").getJSONObject(0);
      assertEquals("fieldNames", fieldError.getString("field"));
      assertThat(fieldError.getString("message")).contains("bogus");
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



   /*******************************************************************************
    ** A joined field name exports through a join given as in a query (here an
    ** aliased self-join that is not an exposed join), and a joined field name
    ** with no join given is joined automatically (#1352).
    *******************************************************************************/
   @Test
   void testExportWithJoins()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of(
            "format", "csv",
            "filter", new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1)),
            "fieldNames", List.of("id", "partner.firstName"),
            "joins", List.of(Map.of("joinTable", "person", "alias", "partner", "joinName", "PersonJoinPartnerPerson", "type", "LEFT"))
         )))
         .asString();
      assertEquals(200, response.getStatus(), response.getBody());
      assertThat(response.getBody()).contains("Person: First Name");
      assertThat(response.getBody()).contains("Linda");

      HttpResponse<String> automatic = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of(
            "format", "csv",
            "filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Kelkhoff")),
            "fieldNames", List.of("id", "pet.name"))))
         .asString();
      assertEquals(200, automatic.getStatus(), automatic.getBody());
      assertThat(automatic.getBody()).containsAnyOf("Chester", "Lucy");

      HttpResponse<String> unknown = Unirest.post(getBaseUrlAndPath() + "/table/person/export")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("format", "csv", "fieldNames", List.of("id", "bogus.name"))))
         .asString();
      assertEquals(400, unknown.getStatus(), unknown.getBody());
      assertEquals("fieldNames", new JSONObject(unknown.getBody()).getJSONArray("fieldErrors").getJSONObject(0).getString("field"));
   }

}
