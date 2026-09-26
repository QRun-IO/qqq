/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
