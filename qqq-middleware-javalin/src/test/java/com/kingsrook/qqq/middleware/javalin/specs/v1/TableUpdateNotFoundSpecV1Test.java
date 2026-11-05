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
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** The v1 table update spec reports a record that is not there (or not visible:
 ** the update action reports both the same) as 404 NOT_FOUND, as GET does.
 *******************************************************************************/
class TableUpdateNotFoundSpecV1Test extends SpecTestBase
{
   /***************************************************************************
    ** The spec under test.
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableUpdateSpecV1();
   }



   /***************************************************************************
    ** The version path segment.
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /*******************************************************************************
    ** Absent id: 404 with the standard error body and NOT_FOUND code.
    *******************************************************************************/
   @Test
   void absentRecordIsNotFound()
   {
      HttpResponse<String> response = Unirest.patch(getBaseUrlAndPath() + "/table/person/999999")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("firstName", "Ghost")))
         .asString();
      assertEquals(404, response.getStatus(), response.getBody());
      JSONObject body = JsonUtils.toJSONObject(response.getBody());
      assertEquals("NOT_FOUND", body.getString("code"));
   }



   /*******************************************************************************
    ** A normal update is unaffected.
    *******************************************************************************/
   @Test
   void existingRecordStillUpdates()
   {
      HttpResponse<String> response = Unirest.patch(getBaseUrlAndPath() + "/table/person/1")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("firstName", "StillWorks")))
         .asString();
      assertEquals(200, response.getStatus());
   }
}
