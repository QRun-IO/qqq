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

package com.kingsrook.qqq.middleware.javalin;


import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.YamlUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QMiddlewareApiSpecHandler 
 *******************************************************************************/
class QMiddlewareApiSpecHandlerTest
{
   private static int PORT = 6264;

   protected static Javalin service;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeAll
   static void beforeAll()
   {
      service = Javalin.create(config ->
         {
            List<AbstractMiddlewareVersion> middlewareVersionList = List.of(new MiddlewareVersionV1());
            config.routes.apiBuilder(new QMiddlewareApiSpecHandler(middlewareVersionList).defineJavalinEndpointGroup());
         }
      ).start(PORT);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private String getBaseUrlAndPath()
   {
      return "http://localhost:" + PORT + "/qqq";
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIndex()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath()).asString();
      assertEquals(200, response.getStatus());
      assertThat(response.getBody()).contains("<html").contains("QQQ Middleware API - v1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testVersionsJson()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/versions.json").asString();
      assertEquals(200, response.getStatus());
      JSONObject object = new JSONObject(response.getBody());
      object.getJSONArray("supportedVersions");
      assertEquals("v1", object.getString("currentVersion"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSpecYaml() throws JsonProcessingException
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/v1/openapi.yaml").asString();
      assertEquals(200, response.getStatus());
      Map<String, Object> map = YamlUtils.toMap(response.getBody());
      assertTrue(map.containsKey("openapi"));
      assertTrue(map.containsKey("info"));
      assertTrue(map.containsKey("paths"));
      assertTrue(map.containsKey("components"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSpecJson() throws JsonProcessingException
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/v1/openapi.json").asString();
      assertEquals(200, response.getStatus());
      JSONObject map = JsonUtils.toJSONObject(response.getBody());
      assertTrue(map.has("openapi"));
      assertTrue(map.has("info"));
      assertTrue(map.has("paths"));
      assertTrue(map.has("components"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testServeResources()
   {
      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/api/docs/js/rapidoc.min.js").asString();
      assertEquals(200, response.getStatus());
   }

}
