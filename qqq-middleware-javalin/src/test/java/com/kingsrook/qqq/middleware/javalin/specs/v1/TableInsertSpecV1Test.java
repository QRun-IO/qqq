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


import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for TableInsertSpecV1
 *******************************************************************************/
class TableInsertSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableInsertSpecV1();
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
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of(
            "firstName", "New",
            "lastName", "Person",
            "email", "new.person@example.com"
         )))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONObject record     = jsonObject.getJSONObject("record");
      assertNotNull(record);
      assertThat(record.getString("tableName")).isEqualTo("person");
      assertThat(record.getJSONObject("values").getString("firstName")).isEqualTo("New");
      assertThat(record.getJSONObject("values").getString("lastName")).isEqualTo("Person");
      assertThat(record.getJSONObject("values").getInt("id")).isGreaterThan(0);
   }



   /*******************************************************************************
    ** test the table-level insert endpoint for a non-real name
    **
    *******************************************************************************/
   @Test
   public void testTableNotFound()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/notAnActualTable")
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
    ** Verify that a JSON body with a null value does not cause an error,
    ** and that an empty string is treated as null.
    *******************************************************************************/
   @Test
   void testNullAndEmptyStringValues()
   {
      ///////////////////////////////////////////////////////////////////////////////////////
      // send a body with explicit null for birthDate and empty string for partnerPersonId; //
      // both should be stored as null values. Required fields (firstName, lastName, email) //
      // are populated to satisfy NOT NULL constraints.                                     //
      ///////////////////////////////////////////////////////////////////////////////////////
      String body = """
         {"firstName": "NullTest", "lastName": "Person", "email": "null@test.com", "birthDate": null, "partnerPersonId": ""}""";

      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(body)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONObject record     = jsonObject.getJSONObject("record");
      assertNotNull(record);
      assertThat(record.getJSONObject("values").getString("firstName")).isEqualTo("NullTest");
      assertThat(record.getJSONObject("values").isNull("birthDate")).isTrue();
      assertThat(record.getJSONObject("values").isNull("partnerPersonId")).isTrue();
   }



   /*******************************************************************************
    ** A multipart insert reads form fields and a blob file, like the legacy route.
    *******************************************************************************/
   @Test
   void testMultipartInsertWithFile()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person")
         .field("firstName", "Multi")
         .field("lastName", "Part")
         .field("email", "multi.part@example.com")
         .field("photo", new ByteArrayInputStream("photo-bytes".getBytes(StandardCharsets.UTF_8)), "photo.png")
         .asString();

      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject values = JsonUtils.toJSONObject(response.getBody()).getJSONObject("record").getJSONObject("values");
      assertEquals("Multi", values.getString("firstName"));
      assertEquals("Part", values.getString("lastName"));
      assertThat(values.getInt("id")).isGreaterThan(0);
   }



   /*******************************************************************************
    ** Associated records in the record-v1 format are inserted with the parent and
    ** returned under associatedRecords.
    *******************************************************************************/
   @Test
   void testInsertWithRecordV1Associations()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person")
         .header("X-QQQ-Association-Format", "record-v1")
         .field("firstName", "Pet")
         .field("lastName", "Owner")
         .field("email", "pet.owner@example.com")
         .field("associations", """
            {"pets": [{"values": {"name": "Rex", "species": "dog"}}]}""")
         .asString();

      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject record = JsonUtils.toJSONObject(response.getBody()).getJSONObject("record");
      JSONArray  pets   = record.getJSONObject("associatedRecords").getJSONArray("pets");
      assertEquals(1, pets.length());
      assertEquals("Rex", pets.getJSONObject(0).getJSONObject("values").getString("name"));
      assertEquals(record.getJSONObject("values").getInt("id"), pets.getJSONObject(0).getJSONObject("values").getInt("ownerPersonId"));
   }



   /*******************************************************************************
    ** The record-v1 association format requires exactly one associations field.
    *******************************************************************************/
   @Test
   void testRecordV1AssociationsRequireTheField()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person")
         .header("X-QQQ-Association-Format", "record-v1")
         .field("firstName", "No")
         .field("lastName", "Associations")
         .asString();

      assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatus(), response.getBody());
      assertThat(JsonUtils.toJSONObject(response.getBody()).getString("error")).contains("record-v1 requires exactly one associations form field");
   }

}
