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
import java.util.UUID;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for ProcessCancelSpecV1
 *******************************************************************************/
class ProcessCancelSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ProcessCancelSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return List.of(new ProcessInitSpecV1());
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
    ** Test cancelling a process that was previously initialized.
    *******************************************************************************/
   @Test
   void testCancelProcess()
   {
      /////////////////////////////////////////////////////////////
      // first, init a process to get a valid processUUID.       //
      // use the greet process since it completes synchronously. //
      /////////////////////////////////////////////////////////////
      HttpResponse<String> initResponse = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .multiPartContent()
         .field("recordsParam", "recordIds")
         .field("recordIds", "1,2")
         .asString();

      assertEquals(200, initResponse.getStatus());
      JSONObject initJson    = JsonUtils.toJSONObject(initResponse.getBody());
      String     processUUID = initJson.getString("processUUID");
      assertNotNull(processUUID);

      //////////////////////////////////////////////
      // now cancel it via the cancel endpoint.   //
      //////////////////////////////////////////////
      HttpResponse<String> cancelResponse = Unirest.post(getBaseUrlAndPath() + "/processes/greet/" + processUUID + "/cancel")
         .asString();

      assertEquals(200, cancelResponse.getStatus());
      assertEquals("{}", cancelResponse.getBody());
   }



   /*******************************************************************************
    ** Test cancelling a non-existent process (random UUID) returns an error.
    *******************************************************************************/
   @Test
   void testCancelNonExistentProcess()
   {
      String fakeUUID = UUID.randomUUID().toString();

      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/" + fakeUUID + "/cancel")
         .asString();

      assertThat(response.getStatus()).isIn(400, 500);
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.has("error")).isTrue();
      assertThat(jsonObject.getString("error")).isNotEmpty();
   }

}
