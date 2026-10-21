/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.middleware.specs.v1;


import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.middleware.specs.ApiAwareSpecTestBase;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for ApiAwareTableQuerySpecV1 
 *******************************************************************************/
class ApiAwareTableCountSpecV1Test extends ApiAwareSpecTestBase
{


   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ApiAwareTableCountSpecV1();
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
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/person/count")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Simpson")))))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.getInt("count")).isEqualTo(5);
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
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2022_Q4) + "/table/person/count")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("shoeCount", QCriteriaOperator.EQUALS, 2)))))
         .asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.getInt("count")).isGreaterThanOrEqualTo(1);

      /////////////////////////////////////////////////////
      // old field name - make sure fails in current api //
      /////////////////////////////////////////////////////
      response = Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/person/count")
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
      Supplier<HttpResponse<String>> makeResponse = () ->
         Unirest.post(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2022_Q4) + "/table/person/count")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(JsonUtils.toJson(Map.of("filter", new QQueryFilter(new QFilterCriteria("createDate", QCriteriaOperator.IS_NOT_BLANK)))))
            .asString();

      /////////////////////////////////////////////////////////////////////////////////////////////
      // first make sure that a query (without the personalizer active) DOES include create date //
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
         HttpResponse<String> response = makeResponse.get();
         assertEquals(200, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertThat(jsonObject.getInt("count")).isGreaterThan(0);
      }

      ////////////////////////////////////////////////////////////////////////////////////////////
      // now repeat the query with the personalizer active - create date should NOT be allowed //
      ////////////////////////////////////////////////////////////////////////////////////////////
      try
      {
         TestUtils.TablePersonalizer.register(serverQInstance);

         HttpResponse<String> response = makeResponse.get();
         assertEquals(400, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertEquals("Unrecognized criteria field name: createDate.", jsonObject.getString("error"));
      }
      finally
      {
         TestUtils.TablePersonalizer.unregister(serverQInstance);
      }
   }

}