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
