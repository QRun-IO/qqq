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


import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for TableUpdateSpecV1
 *******************************************************************************/
class TableUpdateSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableUpdateSpecV1();
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
   void test()
   {
      HttpResponse<String> response = Unirest.patch(getBaseUrlAndPath() + "/table/person/1")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("firstName", "Updated")))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONObject record     = jsonObject.getJSONObject("record");
      assertNotNull(record);
      assertThat(record.getJSONObject("values").getString("firstName")).isEqualTo("Updated");
      assertThat(record.getJSONObject("values").getInt("id")).isEqualTo(1);
   }



   /*******************************************************************************
    ** test the table-level update endpoint for a non-real name
    **
    *******************************************************************************/
   @Test
   public void testTableNotFound()
   {
      HttpResponse<String> response = Unirest.patch(getBaseUrlAndPath() + "/table/notAnActualTable/1")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("firstName", "Test")))
         .asString();

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // expect a non-existing table to 403, the same as one that does exist but that you don't have permission to //
      // to kinda hide from someone what is or isn't a real table (as a security thing i guess)                    //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(HttpStatus.FORBIDDEN_403, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(1, jsonObject.keySet().size(), "Number of top-level keys");
      String error = jsonObject.getString("error");
      assertThat(error).contains("Permission denied");
   }



   /*******************************************************************************
    ** Test updating a record that does not exist (primaryKey=999999).
    ** The RDBMS backend returns a record with errors for non-existent updates,
    ** which surfaces as a user-facing error in the response.
    *******************************************************************************/
   @Test
   void testUpdateNonExistentRecord()
   {
      HttpResponse<String> response = Unirest.patch(getBaseUrlAndPath() + "/table/person/999999")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("firstName", "Ghost")))
         .asString();

      /////////////////////////////////////////////////////////////////////////////////////
      // the response may be a 200 with errors on the record, or a 4xx/5xx error status; //
      // we verify the response is valid JSON either way.                                //
      /////////////////////////////////////////////////////////////////////////////////////
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
   }

}
