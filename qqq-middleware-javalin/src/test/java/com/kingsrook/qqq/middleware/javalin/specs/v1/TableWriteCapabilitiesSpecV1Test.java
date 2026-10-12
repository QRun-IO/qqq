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
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** The v1 insert, update and delete routes honor the table's declared write
 ** capabilities.
 *******************************************************************************/
class TableWriteCapabilitiesSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableInsertSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return List.of(new TableUpdateSpecV1(), new TableDeleteSpecV1());
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
    **
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance()
   {
      QInstance instance = TestUtils.defineInstance();
      TestUtils.addStateEnumTable(instance);
      return (instance);
   }



   /*******************************************************************************
    ** A table without TABLE_INSERT / TABLE_UPDATE / TABLE_DELETE (an enum table,
    ** under NOT_PROTECTED rules) refuses every write route before its backend
    ** module is reached.
    *******************************************************************************/
   @Test
   void testWritesWithoutCapabilitiesAreRefused()
   {
      String tableUrl = getBaseUrlAndPath() + "/table/" + TestUtils.TABLE_NAME_STATE_ENUM;
      String body     = JsonUtils.toJson(Map.of("name", "Kansas"));

      assertAll(
         () -> assertRefused(Unirest.post(tableUrl).contentType(ContentType.APPLICATION_JSON.getMimeType()).body(body).asString()),
         () -> assertRefused(Unirest.patch(tableUrl + "/1").contentType(ContentType.APPLICATION_JSON.getMimeType()).body(body).asString()),
         () -> assertRefused(Unirest.delete(tableUrl + "/1").asString()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void assertRefused(HttpResponse<String> response)
   {
      assertEquals(HttpStatus.FORBIDDEN_403, response.getStatus(), response.getBody());
      assertEquals("Permission denied.", JsonUtils.toJSONObject(response.getBody()).getString("error"));
   }
}
