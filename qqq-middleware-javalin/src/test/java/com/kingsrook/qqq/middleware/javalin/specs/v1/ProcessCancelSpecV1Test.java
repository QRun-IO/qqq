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


import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
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
         .cookie("sessionId", "v1-process-session")
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
         .cookie("sessionId", "v1-process-session")
         .asString();

      assertEquals(200, cancelResponse.getStatus());
      assertEquals("{}", cancelResponse.getBody());
   }



   /*******************************************************************************
    ** Another session cannot cancel (run the cancel step of) a process it did
    ** not run.
    *******************************************************************************/
   @Test
   void testCancelAnotherSessionsProcessIsRefused()
   {
      HttpResponse<String> initResponse = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .cookie("sessionId", "v1-process-owner")
         .multiPartContent()
         .field("recordsParam", "recordIds")
         .field("recordIds", "1,2")
         .asString();
      String processUUID = JsonUtils.toJSONObject(initResponse.getBody()).getString("processUUID");

      HttpResponse<String> cancelResponse = Unirest.post(getBaseUrlAndPath() + "/processes/greet/" + processUUID + "/cancel")
         .cookie("sessionId", "v1-process-intruder")
         .asString();
      assertEquals(403, cancelResponse.getStatus(), cancelResponse.getBody());
   }



   /*******************************************************************************
    ** Test cancelling a non-existent process (random UUID) returns an error.
    *******************************************************************************/
   @Test
   void testCancelNonExistentProcess()
   {
      String fakeUUID = UUID.randomUUID().toString();

      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/" + fakeUUID + "/cancel")
         .cookie("sessionId", "v1-process-session")
         .asString();

      assertThat(response.getStatus()).isIn(400, 500);
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.has("error")).isTrue();
      assertThat(jsonObject.getString("error")).isNotEmpty();
   }

}
