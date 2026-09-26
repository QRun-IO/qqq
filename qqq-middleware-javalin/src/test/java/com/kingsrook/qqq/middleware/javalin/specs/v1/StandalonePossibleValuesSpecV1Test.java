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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for StandalonePossibleValuesSpecV1
 *******************************************************************************/
class StandalonePossibleValuesSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new StandalonePossibleValuesSpecV1();
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
    ** Test happy path - search with searchTerm returning results
    *******************************************************************************/
   @Test
   void testSearchWithSearchTerm()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/possibleValues/person")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("searchTerm", "Kelkhoff")))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("options"));
      JSONArray options = jsonObject.getJSONArray("options");
      assertThat(options.length()).isGreaterThan(0);

      for(int i = 0; i < options.length(); i++)
      {
         JSONObject option = options.getJSONObject(i);
         assertTrue(option.has("id"));
         assertTrue(option.has("label"));
      }
   }



   /*******************************************************************************
    ** Test search returning empty results
    *******************************************************************************/
   @Test
   void testSearchReturningEmptyResults()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/possibleValues/person")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("searchTerm", "NoSuchPersonExists9999")))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());

      ///////////////////////////////////////////////////////////////////////////
      // Jackson NON_EMPTY serialization omits empty arrays, so options may    //
      // either be absent or present as an empty array.                        //
      ///////////////////////////////////////////////////////////////////////////
      if(jsonObject.has("options"))
      {
         JSONArray options = jsonObject.getJSONArray("options");
         assertEquals(0, options.length());
      }
   }



   /*******************************************************************************
    ** Test with invalid possible value source name returns error
    *******************************************************************************/
   @Test
   void testInvalidPossibleValueSourceName()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/possibleValues/notAPossibleValueSource")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body("{}")
         .asString();

      assertThat(response.getStatus()).isGreaterThanOrEqualTo(400);
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.getString("error")).contains("Could not find possible value source");
   }



   /*******************************************************************************
    ** Test search with no body (empty request)
    *******************************************************************************/
   @Test
   void testSearchWithNoBody()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/possibleValues/person")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body("{}")
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("options"));
      JSONArray options = jsonObject.getJSONArray("options");
      assertThat(options.length()).isGreaterThan(0);
   }



   /*******************************************************************************
    ** Test searching by labels returns matching results.
    *******************************************************************************/
   @Test
   void testSearchByLabels()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/possibleValues/person")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("labels", List.of("Kelkhoff"))))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("options"));
   }

}
