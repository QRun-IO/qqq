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
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;
import io.javalin.http.ContentType;
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
 ** Unit test for TableQuerySpecV1
 *******************************************************************************/
class TableQuerySpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableQuerySpecV1();
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
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/query")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Kelkhoff")))))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONArray  records    = jsonObject.getJSONArray("records");
      assertThat(records.length()).isGreaterThanOrEqualTo(1);

      JSONObject record = records.getJSONObject(0);
      assertThat(record.getString("recordLabel")).contains("Kelkhoff");
      assertThat(record.getString("tableName")).isEqualTo("person");
      assertThat(record.getJSONObject("values").getString("lastName")).isEqualTo("Kelkhoff");
      assertThat(record.getJSONObject("displayValues").getString("lastName")).isEqualTo("Kelkhoff");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoin()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/query")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of(
            "filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Kelkhoff")),
            "joins", List.of(MapBuilder.of(
               "joinTable", TestUtils.TABLE_NAME_PET,
               "select", true,
               "type", "LEFT",
               "alias", null,
               "joinName", null,
               "baseTableOrAlias", null
            ))
         )))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONArray  records    = jsonObject.getJSONArray("records");
      assertThat(records.length()).isGreaterThanOrEqualTo(3);

      JSONObject record = records.getJSONObject(0);
      assertThat(record.getString("recordLabel")).contains("Kelkhoff");
      assertThat(record.getString("tableName")).isEqualTo("person");
      assertThat(record.getJSONObject("values").getString("lastName")).isEqualTo("Kelkhoff");
      assertThat(record.getJSONObject("displayValues").getString("lastName")).isEqualTo("Kelkhoff");
      assertThat(record.getJSONObject("displayValues").getString("pet.name")).isIn(List.of("Chester", "Lucy"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoRecordsFound()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/query")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Unkelkhoff")))))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONArray  records    = jsonObject.getJSONArray("records");
      assertThat(records.length()).isZero();
   }



   /*******************************************************************************
    ** test the table-level meta-data endpoint for a non-real name
    **
    *******************************************************************************/
   @Test
   public void testTableNotFound()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/notAnActualTable/query").asString();

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
      // query with no variant - expect error //
      //////////////////////////////////////////
      String url = getBaseUrlAndPath() + "/table/" + TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA + "/query";
      HttpResponse<String> response = Unirest.post(url)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of()))
         .asString();
      assertEquals(500, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("Could not find Backend Variant information in session under key 'memoryVariantOptions' for Backend 'memoryWithVariants'", jsonObject.getString("error"));

      //////////////////////////////
      // query for people variant //
      //////////////////////////////
      response = Unirest.post(url)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("tableVariant", new TableVariant().withType("memoryVariantOptions").withId("1"))))
         .asString();

      assertEquals(200, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONArray records = jsonObject.getJSONArray("records");
      assertThat(records.length()).isEqualTo(2);
      JSONObject record = records.getJSONObject(0);
      assertThat(record.getJSONObject("values").getString("name")).isEqualTo("Tom");

      ///////////////////////////////
      // query for planets variant //
      ///////////////////////////////
      response = Unirest.post(url)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("tableVariant", new TableVariant().withType("memoryVariantOptions").withId("2"))))
         .asString();

      assertEquals(200, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      records = jsonObject.getJSONArray("records");
      assertThat(records.length()).isEqualTo(3);
      record = records.getJSONObject(0);
      assertThat(record.getJSONObject("values").getString("name")).isEqualTo("Mars");

      ///////////////////////////////////////////////
      // query with unknown variant - expect error //
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
      Supplier<JSONArray> request = () ->
      {
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/query")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Darin")))))
            .asString();
         assertEquals(200, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         JSONArray  records    = jsonObject.getJSONArray("records");
         return (records);
      };

      /////////////////////////////////////////////////////////////////////////////////////////////
      // first make sure that a query (without the personalizer active) DOES include create date //
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
         JSONArray records = request.get();
         assertThat(records.length()).isGreaterThan(0);
         assertTrue(records.getJSONObject(0).getJSONObject("values").has("createDate"));
         assertTrue(records.getJSONObject(0).getJSONObject("displayValues").has("createDate"));
      }

      //////////////////////////////////////////////////////////////////////////////////////////
      // now repeat the query with the personalizer active - create date should NOT come back //
      //////////////////////////////////////////////////////////////////////////////////////////
      try
      {
         TestUtils.TablePersonalizer.register(serverQInstance);

         JSONArray records = request.get();
         assertThat(records.length()).isGreaterThan(0);
         assertFalse(records.getJSONObject(0).getJSONObject("values").has("createDate"));
         assertFalse(records.getJSONObject(0).getJSONObject("displayValues").has("createDate"));

         ///////////////////////////////////////////////
         // try to query by create date - should fail //
         ///////////////////////////////////////////////
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/query")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("createDate", QCriteriaOperator.IS_NOT_BLANK)))))
            .asString();
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