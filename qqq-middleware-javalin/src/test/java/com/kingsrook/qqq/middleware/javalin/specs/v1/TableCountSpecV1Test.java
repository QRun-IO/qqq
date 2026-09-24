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
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Unit test for TableCountSpecV1
 *******************************************************************************/
class TableCountSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableCountSpecV1();
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
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/count")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Kelkhoff")))))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(2, jsonObject.getInt("count"));
      assertFalse(jsonObject.has("distinctCount"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIncludingDistinct()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/count?includeDistinct=true")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Kelkhoff")))))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(2, jsonObject.getInt("count"));
      assertEquals(2, jsonObject.getInt("distinctCount"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoRecordsFound()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/count")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Unkelkhoff")))))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(0, jsonObject.getInt("count"));
   }



   /*******************************************************************************
    ** test the for a non-real name
    **
    *******************************************************************************/
   @Test
   public void testTableNotFound()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/notAnActualTable/count").asString();

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
    **
    *******************************************************************************/
   @Test
   void testPostingTableVariant() throws QException
   {
      ////////////////////////////////////
      // insert our two variant options //
      ////////////////////////////////////
      QContext.init(TestUtils.defineInstance(), new QSystemUserSession());
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "People"),
         new QRecord().withValue("id", 2).withValue("name", "Planets")
      )));

      ////////////////////////////////////////
      // insert some data into each variant //
      ////////////////////////////////////////
      QContext.getQSession().setBackendVariants(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, 1));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA).withRecords(List.of(
         new QRecord().withValue("name", "Tom"),
         new QRecord().withValue("name", "Sally")
      )));

      QContext.getQSession().setBackendVariants(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, 2));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA).withRecords(List.of(
         new QRecord().withValue("name", "Mars"),
         new QRecord().withValue("name", "Jupiter"),
         new QRecord().withValue("name", "Saturn")
      )));

      //////////////////////////////////////////
      // count with no variant - expect error //
      //////////////////////////////////////////
      String url = getBaseUrlAndPath() + "/table/" + TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA + "/count";
      HttpResponse<String> response = Unirest.post(url)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of()))
         .asString();
      assertEquals(500, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("Could not find Backend Variant information in session under key 'memoryVariantOptions' for Backend 'memoryWithVariants'", jsonObject.getString("error"));

      //////////////////////////////
      // count for people variant //
      //////////////////////////////
      response = Unirest.post(url)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("tableVariant", new TableVariant().withType("memoryVariantOptions").withId("1"))))
         .asString();

      assertEquals(200, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(2, jsonObject.getInt("count"));

      ///////////////////////////////
      // count for planets variant //
      ///////////////////////////////
      response = Unirest.post(url)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("tableVariant", new TableVariant().withType("memoryVariantOptions").withId("2"))))
         .asString();

      assertEquals(200, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(3, jsonObject.getInt("count"));

      ///////////////////////////////////////////////
      // count with unknown variant - expect error //
      ///////////////////////////////////////////////
      response = Unirest.post(url)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("tableVariant", new TableVariant().withType("memoryVariantOptions").withId("3"))))
         .asString();
      assertEquals(500, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("Could not find Backend Variant in table memoryVariantOptions with id '3'", jsonObject.getString("error"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPersonalizedTable()
   {
      Supplier<HttpResponse<String>> request = () ->
      {
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/count")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("createDate", QCriteriaOperator.IS_NOT_BLANK)))))
            .asString();
         return (response);
      };

      ////////////////////////////////////////////////////////////////////////////////////////
      // first make sure that a query (without the personalizer active) can use create date //
      ////////////////////////////////////////////////////////////////////////////////////////
      {
         HttpResponse<String> response = request.get();
         assertEquals(200, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertThat(jsonObject.getInt("count")).isGreaterThan(0);
      }

      /////////////////////////////////////////////////////////////////////////////////
      // now repeat the query with the personalizer active - create date should work //
      /////////////////////////////////////////////////////////////////////////////////
      try
      {
         TestUtils.TablePersonalizer.register(serverQInstance);

         ///////////////////////////////////////////////
         // try to query by create date - should fail //
         ///////////////////////////////////////////////
         HttpResponse<String> response = request.get();
         assertEquals(500, response.getStatus()); // might be better as 400...
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertEquals("Query Filter contained 1 unrecognized field name: createDate", jsonObject.getString("error"));
      }
      finally
      {
         TestUtils.TablePersonalizer.unregister(serverQInstance);
      }
   }

}