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


import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for TableApisSpecV1 (TestTableApisProvider is registered in
 ** src/test/resources/META-INF/services).
 *******************************************************************************/
class TableApisSpecV1Test extends SpecTestBase
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new TableApisSpecV1());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testListsProvidedApis()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/apis").asString();
      assertEquals(200, response.getStatus(), response.getBody());

      JSONArray apis = new JSONObject(response.getBody()).getJSONArray("apis");
      assertEquals(2, apis.length());
      assertEquals("a-api", apis.getJSONObject(0).getString("name"));

      JSONObject api = apis.getJSONObject(1);
      assertEquals("test", api.getString("name"));
      assertEquals("Test API", api.getString("label"));
      assertEquals("/api/", api.getString("path"));
      assertEquals("2", api.getString("currentVersion"));
      JSONObject version = api.getJSONArray("versions").getJSONObject(1);
      assertEquals("2", version.getString("version"));
      assertEquals("2.1.0", version.getString("semanticVersion"));
      assertEquals("/api/2/person/openapi.json", version.getString("tableSpecPath"));
      assertTrue(api.getJSONArray("versions").getJSONObject(0).isNull("semanticVersion"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableWithoutApis()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/pet/apis").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      assertEquals(0, new JSONObject(response.getBody()).getJSONArray("apis").length());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownTableIsForbidden()
   {
      assertEquals(403, Unirest.get(getBaseUrlAndPath() + "/table/notATable/apis").asString().getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoReadPermissionIsForbidden()
   {
      serverQInstance.getTable("person").setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      assertEquals(403, Unirest.get(getBaseUrlAndPath() + "/table/person/apis").asString().getStatus());
   }



   /*******************************************************************************
    ** two APIs for person (out of order, to prove the sort), none otherwise.
    *******************************************************************************/
   public static class TestTableApisProvider implements TableApisProviderInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<TableApi> getTableApis(QInstance qInstance, String tableName)
      {
         if(!"person".equals(tableName))
         {
            return (List.of());
         }

         return (List.of(
            new TableApi("test", "Test API", "/api/", "2", List.of(
               new TableApiVersion("1", null, "/api/1/person/openapi.json"),
               new TableApiVersion("2", "2.1.0", "/api/2/person/openapi.json"))),
            new TableApi("a-api", "A API", "/a-api/", null, List.of())));
      }



      /***************************************************************************
       ** a tiny document for person in test 2
       ***************************************************************************/
      @Override
      public Optional<String> getTableSpecJson(QInstance qInstance, String apiName, String version, String tableName)
      {
         if("person".equals(tableName) && "test".equals(apiName) && "2".equals(version))
         {
            return (Optional.of("{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"person\",\"version\":\"2\"}}"));
         }
         return (Optional.empty());
      }
   }

}
