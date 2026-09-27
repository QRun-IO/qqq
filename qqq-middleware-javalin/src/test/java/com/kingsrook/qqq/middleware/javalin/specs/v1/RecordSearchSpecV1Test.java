/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
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


/*******************************************************************************
 ** Unit test for RecordSearchSpecV1
 *******************************************************************************/
class RecordSearchSpecV1Test extends SpecTestBase
{
   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new RecordSearchSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    ** person is searchable by name and email; pet by name.
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON).withSearchFields("firstName", "lastName", "email");
      qInstance.getTable(TestUtils.TABLE_NAME_PET).withSearchFields("name");
      return (qInstance);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private HttpResponse<String> post(Map<String, ?> body)
   {
      return Unirest.post(getBaseUrlAndPath() + "/search")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(body))
         .asString();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static List<String> results(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus(), response.getBody());
      JSONArray    results = JsonUtils.toJSONObject(response.getBody()).getJSONArray("results");
      List<String> list    = new ArrayList<>();
      for(int i = 0; i < results.length(); i++)
      {
         JSONObject result = results.getJSONObject(i);
         list.add(result.getString("tableName") + "|" + result.getString("tableLabel") + "|" + result.getString("recordId") + "|" + result.getString("recordLabel"));
      }
      return (list);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchAcrossTablesIgnoringCase()
   {
      assertThat(results(post(Map.of("searchTerm", "KELKHOFF"))))
         .containsExactly("person|Person|1|Darin Kelkhoff", "person|Person|6|Linda Kelkhoff");

      assertThat(results(post(Map.of("searchTerm", "ch")))).containsExactly("person|Person|3|Tim Chamberlain", "person|Person|5|Garret Richardson", "pet|Pet|1|Chester");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableNamesAndLimit()
   {
      assertThat(results(post(Map.of("searchTerm", "ch", "tableNames", List.of("pet"))))).containsExactly("pet|Pet|1|Chester");
      assertThat(results(post(Map.of("searchTerm", "kelkhoff", "limitPerTable", 1)))).containsExactly("person|Person|1|Darin Kelkhoff");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoMatchesReturnsAnEmptyList()
   {
      HttpResponse<String> response = post(Map.of("searchTerm", "no such text"));
      assertEquals(200, response.getStatus());
      assertEquals(0, JsonUtils.toJSONObject(response.getBody()).getJSONArray("results").length());
   }



   /*******************************************************************************
    ** A table the session may not read is skipped, even when named.
    *******************************************************************************/
   @Test
   void testTableWithoutReadPermissionIsNotSearched()
   {
      serverQInstance.getTable(TestUtils.TABLE_NAME_PET).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));

      assertThat(results(post(Map.of("searchTerm", "ch")))).containsExactly("person|Person|3|Tim Chamberlain", "person|Person|5|Garret Richardson");
      assertThat(results(post(Map.of("searchTerm", "ch", "tableNames", List.of("pet"))))).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadRequests()
   {
      for(Map<String, ?> body : List.<Map<String, ?>>of(Map.of(), Map.of("searchTerm", "  "), Map.of("searchTerm", "x".repeat(101)), Map.of("searchTerm", "a", "tableNames", "person"), Map.of("searchTerm", "a", "limitPerTable", "x")))
      {
         HttpResponse<String> response = post(body);
         assertEquals(400, response.getStatus(), "for body " + body + ": " + response.getBody());
         assertThat(JsonUtils.toJSONObject(response.getBody()).getString("error")).isNotBlank();
      }
   }

}
