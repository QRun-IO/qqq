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


import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for TableApiDocumentSpecV1 (the provider is
 ** TableApisSpecV1Test.TestTableApisProvider, registered in the test
 ** META-INF/services).
 *******************************************************************************/
class TableApiDocumentSpecV1Test extends SpecTestBase
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new TableApiDocumentSpecV1());
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
   void testReturnsTheProvidersDocument()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/apis/test/2/openapi").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject document = new JSONObject(response.getBody());
      assertEquals("3.0.3", document.getString("openapi"));
      assertEquals("person", document.getJSONObject("info").getString("title"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownApiOrVersionIsNotFound()
   {
      assertEquals(404, Unirest.get(getBaseUrlAndPath() + "/table/person/apis/test/1/openapi").asString().getStatus());
      assertEquals(404, Unirest.get(getBaseUrlAndPath() + "/table/person/apis/nope/2/openapi").asString().getStatus());
      assertEquals(404, Unirest.get(getBaseUrlAndPath() + "/table/pet/apis/test/2/openapi").asString().getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownTableAndNoReadPermissionAreForbidden()
   {
      assertEquals(403, Unirest.get(getBaseUrlAndPath() + "/table/notATable/apis/test/2/openapi").asString().getStatus());
      serverQInstance.getTable("person").setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      assertEquals(403, Unirest.get(getBaseUrlAndPath() + "/table/person/apis/test/2/openapi").asString().getStatus());
   }

}
