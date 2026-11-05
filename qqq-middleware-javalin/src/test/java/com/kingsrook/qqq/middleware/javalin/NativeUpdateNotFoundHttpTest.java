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

package com.kingsrook.qqq.middleware.javalin;


import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** The legacy data route updating a record that is not there (or not visible, which
 ** the update action reports identically) is a 404, as a matching GET is, and not
 ** a server error. Bad input stays a 4xx and a good update stays 200.
 *******************************************************************************/
class NativeUpdateNotFoundHttpTest extends QJavalinTestBase
{
   /*******************************************************************************
    ** PATCH on an absent id is 404 like GET.
    *******************************************************************************/
   @Test
   void absentRecordPatchIsNotFound()
   {
      HttpResponse<String> get = Unirest.get(BASE_URL + "/data/person/999999").asString();
      HttpResponse<String> patch = Unirest.patch(BASE_URL + "/data/person/999999").header("Content-Type", "application/json")
         .body(JsonUtils.toJson(Map.of("firstName", "Ghost"))).asString();
      assertEquals(404, get.getStatus());
      assertEquals(404, patch.getStatus(), "PATCH must match GET, body: " + patch.getBody());
      assertTrue(patch.getBody().contains("No record was found"), patch.getBody());
   }



   /*******************************************************************************
    ** A normal update is unaffected.
    *******************************************************************************/
   @Test
   void existingRecordPatchStillWorks()
   {
      HttpResponse<String> patch = Unirest.patch(BASE_URL + "/data/person/4").header("Content-Type", "application/json")
         .body(JsonUtils.toJson(Map.of("firstName", "StillWorks"))).asString();
      assertEquals(200, patch.getStatus());
   }
}
