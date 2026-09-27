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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for WidgetSpecV1
 *******************************************************************************/
class WidgetSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new WidgetSpecV1();
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
    ** Test rendering the EchoWidgetRenderer, which echoes back its "input" param.
    *******************************************************************************/
   @Test
   void testRenderWidget()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/widget/EchoWidgetRenderer")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("input", "Hello World")))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.getString("html")).isEqualTo("Hello World");
   }



   /*******************************************************************************
    ** Test rendering a widget with an empty JSON body -- should not NPE and
    ** should return 200 with widget data (html will be null since no input).
    *******************************************************************************/
   @Test
   void testRenderWidgetWithEmptyBody()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/widget/EchoWidgetRenderer")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body("{}")
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.getString("type")).isEqualTo("html");
   }



   /*******************************************************************************
    ** Test that requesting a non-existent widget returns an appropriate error.
    *******************************************************************************/
   @Test
   void testWidgetNotFound()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/widget/noSuchWidget")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body("{}")
         .asString();

      assertEquals(404, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.getString("error")).contains("Widget not found");
   }

}
