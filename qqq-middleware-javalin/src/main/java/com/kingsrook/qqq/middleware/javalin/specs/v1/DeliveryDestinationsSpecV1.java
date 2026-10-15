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
import com.kingsrook.qqq.middleware.javalin.executors.DeliveryExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.DeliveryMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.DeliveryDestinationsResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.UserPreferencesSpecUtils;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;


/*******************************************************************************
 ** Authenticated output delivery operation.
 *******************************************************************************/
public class DeliveryDestinationsSpecV1 extends AbstractEndpointSpec<DeliveryMiddlewareInput, DeliveryDestinationsResponseV1, DeliveryExecutor>
{
   /*******************************************************************************
    ** Describe this versioned operation.
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath("/delivery/destinations").withHttpMethod(HttpMethod.GET).withTag(TagsV1.GENERAL)
         .withShortSummary("List authorized output destinations"));
   }



   /*******************************************************************************
    ** Parameters from the URL path only.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of());
   }



   /*******************************************************************************
    ** Build the typed input; identity always comes from the authenticated session.
    *******************************************************************************/
   @Override
   public DeliveryMiddlewareInput buildInput(Context context) throws Exception
   {
      return (new DeliveryMiddlewareInput().withOperation(DeliveryMiddlewareInput.Operation.LIST));
   }



   /*******************************************************************************
    ** Publish response and request schemas.
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return (Map.of("DeliveryDestinationsResponseV1", new DeliveryDestinationsResponseV1().toSchema()));
   }



   /*******************************************************************************
    ** Success response contract.
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return (new BasicResponse("Delivery response", "DeliveryDestinationsResponseV1"));
   }



   /*******************************************************************************
    ** Write the operation response.
    *******************************************************************************/
   @Override
   public void handleOutput(Context context, DeliveryDestinationsResponseV1 output) throws Exception
   {
      UserPreferencesSpecUtils.writeResponse(context, output);
   }
}
