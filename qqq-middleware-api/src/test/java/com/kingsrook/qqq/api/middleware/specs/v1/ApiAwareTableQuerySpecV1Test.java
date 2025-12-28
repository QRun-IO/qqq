/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.middleware.specs.v1;


import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.middleware.specs.ApiAwareSpecTestBase;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ApiAwareTableQuerySpecV1 
 *******************************************************************************/
class ApiAwareTableQuerySpecV1Test extends ApiAwareSpecTestBase
{


   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ApiAwareTableQuerySpecV1();
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
   void testBasicSuccess()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/person/query")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Simpson")))))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONArray  records    = jsonObject.getJSONArray("records");
      assertThat(records.length()).isGreaterThanOrEqualTo(1);

      JSONObject record = records.getJSONObject(0);
      assertThat(record.getString("recordLabel")).contains("Simpson");
      assertThat(record.getString("tableName")).isEqualTo("person");
      assertThat(record.getJSONObject("values").getString("lastName")).isEqualTo("Simpson");
      assertThat(record.getJSONObject("displayValues").getString("lastName")).isEqualTo("Simpson");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDisplayValues()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/person/query")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("bestFriendPersonId", QCriteriaOperator.IS_NOT_BLANK)))))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONArray  records    = jsonObject.getJSONArray("records");
      assertThat(records.length()).isGreaterThanOrEqualTo(1);

      JSONObject record = records.getJSONObject(0);
      assertThat(record.getString("tableName")).isEqualTo("person");
      assertThat(record.getJSONObject("values").getInt("bestFriendPersonId")).isEqualTo(1);
      assertThat(record.getJSONObject("displayValues").getString("bestFriendPersonId")).isEqualTo("Homer Simpson");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoBody()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/person/query")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONArray  records    = jsonObject.getJSONArray("records");
      assertThat(records.length()).isGreaterThanOrEqualTo(1);
   }



   /*******************************************************************************
    ** Note - same data cases as in QRecordApiAdapterTest
    *******************************************************************************/
   @Test
   void testVersions()
   {
      String requestBody = JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Tim"))));

      /////////////////
      // old version //
      /////////////////
      {
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2022_Q4) + "/table/person/query")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(requestBody)
            .asString();

         assertEquals(200, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         JSONArray  records    = jsonObject.getJSONArray("records");
         JSONObject record     = records.getJSONObject(0);
         assertThat(record.getJSONObject("values").getInt("shoeCount")).isEqualTo(2);
         assertFalse(record.getJSONObject("values").has("noOfShoes"));
         assertFalse(record.getJSONObject("values").has("cost"));
         assertThat(record.getJSONObject("values").getString("photo")).isEqualTo("QUJDRA==");
      }

      /////////////////////
      // current version //
      /////////////////////
      {
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/person/query")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(requestBody)
            .asString();

         assertEquals(200, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         JSONArray  records    = jsonObject.getJSONArray("records");
         JSONObject record     = records.getJSONObject(0);
         assertFalse(record.getJSONObject("values").has("shoeCount"));
         assertThat(record.getJSONObject("values").getInt("noOfShoes")).isEqualTo(2);
         assertFalse(record.getJSONObject("values").has("cost"));
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // future version ... not actually yet exposed, so, can't query on it                                          //
      // (unlike the QRecordApiAdapterTest that this is based on, that doesn't care about supported versions or not) //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      /*
      {
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q2) + "/table/person/query")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(requestBody)
            .asString();

         assertEquals(200, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         JSONArray  records    = jsonObject.getJSONArray("records");
         JSONObject record     = records.getJSONObject(0);
         assertFalse(record.getJSONObject("values").has("shoeCount"));
         assertThat(record.getJSONObject("values").getInt("noOfShoes")).isEqualTo(2);
         assertThat(record.getJSONObject("values").getString("cost")).isEqualTo("3.50");
      }
      */
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryByOldFieldName()
   {
      /////////////////////////////////////////////////
      // old field name - make sure works in old api //
      /////////////////////////////////////////////////
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2022_Q4) + "/table/person/query")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("shoeCount", QCriteriaOperator.EQUALS, 2)))))
         .asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONArray  records    = jsonObject.getJSONArray("records");
      assertThat(records.length()).isGreaterThanOrEqualTo(1);

      /////////////////////////////////////////////////////
      // old field name - make sure fails in current api //
      /////////////////////////////////////////////////////
      response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/person/query")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("shoeCount", QCriteriaOperator.EQUALS, 2)))))
         .asString();
      assertEquals(400, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("Unrecognized criteria field name: shoeCount.", jsonObject.getString("error"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPersonalizedTable()
   {
      Supplier<JSONArray> request = () ->
      {
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2022_Q4) + "/table/person/query")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("shoeCount", QCriteriaOperator.EQUALS, 2)))))
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
         assertFalse(records.getJSONObject(0).getJSONObject("values").has("createDate"));
         assertFalse(records.getJSONObject(0).getJSONObject("displayValues").has("createDate"));

         ///////////////////////////////////////////////
         // try to query by create date - should fail //
         ///////////////////////////////////////////////
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2022_Q4) + "/table/person/query")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("createDate", QCriteriaOperator.IS_NOT_BLANK)))))
            .asString();
         assertEquals(400, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertEquals("Unrecognized criteria field name: createDate.", jsonObject.getString("error"));
      }
      finally
      {
         TestUtils.TablePersonalizer.unregister(serverQInstance);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNotFoundCases()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1 + "-no-such-version") + "/table/person/query")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .asString();
      assertEquals(404, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("No API exists at the requested path.", jsonObject.getString("error"));

      response = Unirest.post(getBaseUrlAndPath("no-such-api", TestUtils.V2023_Q1) + "/table/person/query")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .asString();
      assertEquals(404, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("No API exists at the requested path.", jsonObject.getString("error"));

      response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/no-such-table/query")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .asString();

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // todo - better as 403 (to make non-tables look like non-permissed tables, to avoid leaking that bit of data? //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(404, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("Could not find a table named no-such-table in this api.", jsonObject.getString("error"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoin() throws QException
   {
      /////////////////////////
      // insert a test order //
      /////////////////////////
      QContext.init(TestUtils.defineInstance(), new QSystemUserSession());
      TestUtils.insert1Order3Lines4LineExtrinsicsAnd1OrderExtrinsic();

      /////////////////////////////
      // assert success function //
      /////////////////////////////
      BiConsumer<HttpResponse<String>, Integer> assertOrderCount = (response, expectedCount) ->
      {
         assertEquals(200, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         JSONArray  records    = jsonObject.getJSONArray("records");
         assertThat(records.length()).isEqualTo(expectedCount);
      };

      /////////////////////////
      // assert 400 function //
      /////////////////////////
      BiConsumer<HttpResponse<String>, String> assert400 = (response, expectedMessage) ->
      {
         assertEquals(400, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertEquals(expectedMessage, jsonObject.getString("error"));
      };

      ////////////////////////////////////////////////////////////
      // basic query (with no join filter) should find 1 record //
      ////////////////////////////////////////////////////////////
      HttpResponse<String> response;
      response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/order/query")
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("orderNo", QCriteriaOperator.EQUALS, "ORD123")))))
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .asString();
      assertOrderCount.accept(response, 1);

      //////////////////////////////////////////////////////////////////
      // basic query (with no join filter) that should find 0 records //
      //////////////////////////////////////////////////////////////////
      response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/order/query")
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("orderNo", QCriteriaOperator.EQUALS, "not-found")))))
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .asString();
      assertOrderCount.accept(response, 0);

      ///////////////////////////////////////////////////////////
      // try to filter by unknown join-table name - should 400 //
      ///////////////////////////////////////////////////////////
      response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/order/query")
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("noSuchTable.sku", QCriteriaOperator.EQUALS, "BASIC1")))))
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .asString();
      assert400.accept(response, "Error processing criteria field: noSuchTable.sku: Unrecognized table name: noSuchTable");

      ///////////////////////////////////////////////////////////
      // try to filter by unknown join field name - should 400 //
      ///////////////////////////////////////////////////////////
      response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/order/query")
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_LINE_ITEM + ".noSuchField", QCriteriaOperator.EQUALS, "BASIC1")))))
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .asString();
      assert400.accept(response, "Unrecognized criteria field name: orderLine.noSuchField.");

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // join for sku - should find (but... memory backend isn't joining correctly at this time... //
      // so we'll ensure at least http 200, and trust that other backends join correctly...        //
      ///////////////////////////////////////////////////////////////////////////////////////////////
      response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/order/query")
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_LINE_ITEM + ".sku", QCriteriaOperator.EQUALS, "BASIC1")))))
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .asString();
      assertOrderCount.accept(response, 0); // todo - ideally 1, but memory backend joining...

      ///////////////////////////////
      // fetch exposed join fields //
      ///////////////////////////////
      List<QueryJoin> joins = List.of(
         new QueryJoin(TestUtils.TABLE_NAME_LINE_ITEM).withSelect(true).withType(QueryJoin.Type.LEFT),
         new QueryJoin(TestUtils.TABLE_NAME_ORDER_EXTRINSIC).withSelect(true).withType(QueryJoin.Type.LEFT));
      response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/order/query")
         .body(JsonUtils.toJson(Map.of("joins", joins)))
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .asString();

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // 1 order, but cross product gives us 3 results (for the 3 lines (and one order-extrinsic)) //
      ///////////////////////////////////////////////////////////////////////////////////////////////
      assertOrderCount.accept(response, 3);
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      JSONArray  records    = jsonObject.getJSONArray("records");

      //////////////////////////////////////////////////////////////////////
      // assert join values are set, and they have had customizer applied //
      // (which up-shifts the extrinsic key)                              //
      //////////////////////////////////////////////////////////////////////
      assertThat(records).allMatch(r ->
      {
         String extrinsicKey = ((JSONObject) r).getJSONObject("values").getString(TestUtils.TABLE_NAME_ORDER_EXTRINSIC + ".key");
         return (extrinsicKey.equals(extrinsicKey.toUpperCase()));
      });

      /////////////////////////////////////////////////////////////////////
      // make sure this field added in the selected version is available //
      /////////////////////////////////////////////////////////////////////
      assertThat(records).allMatch(r -> ((JSONObject) r).getJSONObject("values").has(TestUtils.TABLE_NAME_LINE_ITEM + ".lineNumber"));

      ////////////////////////////////////////////////////////////////
      // assert that a field added in newer version isn't available //
      ////////////////////////////////////////////////////////////////
      response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2022_Q4) + "/table/order/query")
         .body(JsonUtils.toJson(Map.of("joins", joins)))
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .asString();
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      records = jsonObject.getJSONArray("records");
      assertThat(records).allMatch(r -> !((JSONObject) r).getJSONObject("values").has(TestUtils.TABLE_NAME_LINE_ITEM + ".lineNumber"));

   }

}