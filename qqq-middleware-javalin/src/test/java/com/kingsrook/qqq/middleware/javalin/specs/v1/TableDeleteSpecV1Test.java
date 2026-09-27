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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for TableDeleteSpecV1
 *******************************************************************************/
class TableDeleteSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableDeleteSpecV1();
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
    **
    *******************************************************************************/
   @Test
   void test()
   {
      HttpResponse<String> response = Unirest.delete(getBaseUrlAndPath() + "/table/person/1")
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(1, jsonObject.getInt("deletedRecordCount"));
   }



   /*******************************************************************************
    ** test the table-level delete endpoint for a non-real name
    **
    *******************************************************************************/
   @Test
   public void testTableNotFound()
   {
      HttpResponse<String> response = Unirest.delete(getBaseUrlAndPath() + "/table/notAnActualTable/1")
         .asString();

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // expect a non-existing table to 403, the same as one that does exist but that you don't have permission to //
      // to kinda hide from someone what is or isn't a real table (as a security thing i guess)                    //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(HttpStatus.FORBIDDEN_403, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(1, jsonObject.keySet().size(), "Number of top-level keys");
      String error = jsonObject.getString("error");
      assertThat(error).contains("Permission denied");
   }



   /*******************************************************************************
    ** Test deleting a record that does not exist.
    *******************************************************************************/
   @Test
   void testDeleteNonExistentRecord()
   {
      HttpResponse<String> response = Unirest.delete(getBaseUrlAndPath() + "/table/person/999999")
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(0, jsonObject.getInt("deletedRecordCount"));
      assertEquals(1, jsonObject.getJSONArray("errors").length());
      assertThat(jsonObject.getJSONArray("errors").getString(0)).contains("No record was found to delete");
   }

}
