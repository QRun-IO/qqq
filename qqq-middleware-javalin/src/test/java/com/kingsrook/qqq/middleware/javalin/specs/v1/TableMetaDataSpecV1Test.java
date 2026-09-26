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


import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.WhiteSpaceBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpFormat;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpRole;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for TableMetaDataSpecV1
 *******************************************************************************/
class TableMetaDataSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableMetaDataSpecV1();
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
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData/table/person").asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("person", jsonObject.getString("name"));
      assertEquals("Person", jsonObject.getString("label"));

      JSONObject fields         = jsonObject.getJSONObject("fields");
      JSONObject firstNameField = fields.getJSONObject("firstName");
      assertEquals("firstName", firstNameField.getString("name"));
      assertEquals("First Name", firstNameField.getString("label"));
   }



   /*******************************************************************************
    ** field help content is part of v1 field meta-data, with its format and roles
    ** (the frontend picks the entry for the current screen), as in legacy meta-data.
    *******************************************************************************/
   @Test
   void testFieldHelpContents()
   {
      QFieldMetaData firstName = serverQInstance.getTable("person").getField("firstName");
      List<QHelpContent> original = firstName.getHelpContents();
      try
      {
         firstName.setHelpContents(List.of(
            new QHelpContent().withContentAsText("Given name on file.").withRole(QHelpRole.READ_SCREENS),
            new QHelpContent().withContentAsMarkdown("Enter the **given** name.").withRole(QHelpRole.WRITE_SCREENS)));

         HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData/table/person").asString();
         assertEquals(200, response.getStatus());
         JSONArray helpContents = JsonUtils.toJSONObject(response.getBody()).getJSONObject("fields").getJSONObject("firstName").getJSONArray("helpContents");
         assertEquals(2, helpContents.length());

         JSONObject read = helpContents.getJSONObject(0);
         assertEquals("Given name on file.", read.getString("content"));
         assertEquals(HelpFormat.TEXT.name(), read.getString("format"));
         assertEquals(List.of("READ_SCREENS"), read.getJSONArray("roles").toList());

         JSONObject write = helpContents.getJSONObject(1);
         assertEquals(HelpFormat.MARKDOWN.name(), write.getString("format"));
         assertEquals(List.of("WRITE_SCREENS"), write.getJSONArray("roles").toList());
         assertThat(write.getString("contentAsHtml")).contains("<strong>given</strong>");

         /////////////////////////////////////////////////////////
         // a field without help content omits the key entirely //
         /////////////////////////////////////////////////////////
         assertFalse(JsonUtils.toJSONObject(response.getBody()).getJSONObject("fields").getJSONObject("lastName").has("helpContents"));
      }
      finally
      {
         firstName.setHelpContents(original);
      }
   }



   /*******************************************************************************
    ** a field's grid width and its frontend behaviors (live case change, white
    ** space) are part of v1 field meta-data, as in the legacy table meta-data
    ** (QRun-IO/qqq#723); fields without them omit the keys.
    *******************************************************************************/
   @Test
   void testFieldGridColumnsAndBehaviors()
   {
      QFieldMetaData firstName = serverQInstance.getTable("person").getField("firstName");
      Integer originalGridColumns = firstName.getGridColumns();
      Set<FieldBehavior<?>> originalBehaviors = firstName.getBehaviors();
      try
      {
         firstName.setGridColumns(12);
         firstName.setBehaviors(new LinkedHashSet<>(List.of(CaseChangeBehavior.TO_UPPER_CASE, WhiteSpaceBehavior.TRIM, ValueTooLongBehavior.TRUNCATE)));

         HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData/table/person").asString();
         assertEquals(200, response.getStatus());
         JSONObject fields = JsonUtils.toJSONObject(response.getBody()).getJSONObject("fields");
         JSONObject field = fields.getJSONObject("firstName");
         assertEquals(12, field.getInt("gridColumns"));

         /////////////////////////////////////////////////////////////////////////////
         // only behaviors a frontend applies are listed (not the backend-only ones) //
         /////////////////////////////////////////////////////////////////////////////
         assertThat(field.getJSONArray("behaviors").toList()).containsExactlyInAnyOrder("TO_UPPER_CASE", "TRIM");

         assertFalse(fields.getJSONObject("lastName").has("gridColumns"));
         assertFalse(fields.getJSONObject("lastName").has("behaviors"));
      }
      finally
      {
         firstName.setGridColumns(originalGridColumns);
         firstName.setBehaviors(originalBehaviors);
      }
   }



   /*******************************************************************************
    ** test the table-level meta-data endpoint for a non-real name
    **
    *******************************************************************************/
   @Test
   public void testNotFound()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData/table/notAnActualTable").asString();

      assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(1, jsonObject.keySet().size(), "Number of top-level keys");
      String error = jsonObject.getString("error");
      assertThat(error).contains("Table").contains("notAnActualTable").contains("not found");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPersonalizedTable()
   {
      Supplier<JSONObject> request = () ->
      {
         HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData/table/person").asString();
         assertEquals(200, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         JSONObject fields     = jsonObject.getJSONObject("fields");
         return (fields);
      };

      ///////////////////////////////////////////////////////////
      // first make sure non-personalized table has createDate //
      ///////////////////////////////////////////////////////////
      {
         JSONObject fields = request.get();
         assertTrue(fields.has("createDate"));
      }

      /////////////////////////////////////////////////////////////////////
      // now repeat with personalizer active, and assert we don't get it //
      /////////////////////////////////////////////////////////////////////
      try
      {
         TestUtils.TablePersonalizer.register(serverQInstance);

         JSONObject fields = request.get();
         assertFalse(fields.has("createDate"));
      }
      finally
      {
         TestUtils.TablePersonalizer.unregister(serverQInstance);
      }
   }
}