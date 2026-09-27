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
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for TableGetSpecV1
 *******************************************************************************/
class TableGetSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableGetSpecV1();
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
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/1")
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONObject record     = jsonObject.getJSONObject("record");
      assertNotNull(record);
      assertThat(record.getString("tableName")).isEqualTo("person");
      assertThat(record.getJSONObject("values").getString("firstName")).isEqualTo("Darin");
      assertThat(record.getJSONObject("values").getString("lastName")).isEqualTo("Kelkhoff");
      assertThat(record.getJSONObject("displayValues").getString("firstName")).isEqualTo("Darin");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordNotFound()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/999999")
         .asString();

      assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      String     error      = jsonObject.getString("error");
      assertThat(error).contains("Could not find");
   }



   /*******************************************************************************
    ** test the table-level meta-data endpoint for a non-real name
    **
    *******************************************************************************/
   @Test
   public void testTableNotFound()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/notAnActualTable/1")
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
    ** Verify that includeAssociations=true parameter does not cause an error,
    ** and that the record is still returned successfully.
    *******************************************************************************/
   @Test
   void testIncludeAssociations()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/1?includeAssociations=true")
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONObject record     = jsonObject.getJSONObject("record");
      assertNotNull(record);
      assertThat(record.getString("tableName")).isEqualTo("person");
      assertThat(record.getJSONObject("values").getString("firstName")).isEqualTo("Darin");
   }



   /*******************************************************************************
    ** includeAssociations returns every association, an empty one as an empty
    ** list (so a client can tell "no children" from "not included").
    *******************************************************************************/
   @Test
   void testIncludeAssociationsListsEveryAssociation() throws QException
   {
      QContext.init(serverQInstance, new QSystemUserSession());
      Integer id = new InsertAction().execute(new InsertInput("person").withRecords(List.of(new QRecord()
         .withValue("firstName", "No").withValue("lastName", "Pets").withValue("email", "no.pets@example.com"))))
         .getRecords().get(0).getValueInteger("id");
      QContext.clear();

      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/" + id + "?includeAssociations=true").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject record = JsonUtils.toJSONObject(response.getBody()).getJSONObject("record");
      assertTrue(record.has("associatedRecords"), response.getBody());
      assertEquals(0, record.getJSONObject("associatedRecords").getJSONArray("pets").length(), response.getBody());

      ///////////////////////////////////////////////////////////////////////
      // like the legacy route, a single record keeps its null values //
      ///////////////////////////////////////////////////////////////////////
      assertTrue(record.getJSONObject("values").has("birthDate"), response.getBody());
      assertTrue(record.getJSONObject("values").isNull("birthDate"), response.getBody());

      HttpResponse<String> without = Unirest.get(getBaseUrlAndPath() + "/table/person/" + id).asString();
      assertFalse(JsonUtils.toJSONObject(without.getBody()).getJSONObject("record").has("associatedRecords"), without.getBody());
   }

}
