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


import java.util.function.Supplier;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.middleware.specs.ApiAwareSpecTestBase;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 **
 *******************************************************************************/
class ApiAwareTableMetaDataSpecV1Test extends ApiAwareSpecTestBase
{


   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ApiAwareTableMetaDataSpecV1();
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
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/metaData/table/person").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.getJSONObject("fields").getJSONObject("noOfShoes").getString("label")).isEqualTo("No Of Shoes");
      assertFalse(jsonObject.getJSONObject("fields").has("shoeCount"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPersonalizedTable()
   {
      Supplier<JSONObject> request = () ->
      {
         HttpResponse<String> response = Unirest.get(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/metaData/table/person").asString();
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



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryOldVersion()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2022_Q4) + "/metaData/table/person").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertFalse(jsonObject.getJSONObject("fields").has("noOfShoes"));
      assertThat(jsonObject.getJSONObject("fields").getJSONObject("shoeCount").getString("label")).isEqualTo("Shoe Count");
   }

}