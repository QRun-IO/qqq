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
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
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