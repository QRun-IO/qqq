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
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.DeliveryExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.DeliveryMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.DeliveryTargetStepResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.DeliveryTargetsRequestV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.UserPreferencesSpecUtils;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 ** Authenticated output delivery operation.
 *******************************************************************************/
public class DeliveryTargetsSpecV1 extends AbstractEndpointSpec<DeliveryMiddlewareInput, DeliveryTargetStepResponseV1, DeliveryExecutor>
{
   /*******************************************************************************
    ** Describe this versioned operation.
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath("/delivery/destinations/{name}/targets").withHttpMethod(HttpMethod.POST).withTag(TagsV1.GENERAL)
         .withShortSummary("Describe the next delivery target"));
   }



   /*******************************************************************************
    ** Parameters from the URL path only.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(new Parameter().withName("name").withIn(In.PATH).withRequired(true).withSchema(new Schema().withType(Type.STRING))));
   }



   /*******************************************************************************
    ** Build the typed input; identity always comes from the authenticated session.
    *******************************************************************************/
   @Override
   public DeliveryMiddlewareInput buildInput(Context context) throws Exception
   {
      try
      {
         DeliveryTargetsRequestV1 request = JsonUtils.toObject(context.body(), DeliveryTargetsRequestV1.class);
         if(request == null || request.getChosen() == null)
         {
            throw new IllegalArgumentException("chosen is required");
         }
         return (new DeliveryMiddlewareInput().withOperation(DeliveryMiddlewareInput.Operation.TARGETS).withName(context.pathParam("name")).withChosen(request.getChosen()));
      }
      catch(Exception e)
      {
         throw new QBadRequestException("Expected an object with a chosen map", e);
      }
   }



   /*******************************************************************************
    ** Publish response and request schemas.
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return (Map.of("DeliveryTargetStepResponseV1", new DeliveryTargetStepResponseV1().toSchema(), "DeliveryTargetsRequestV1", new DeliveryTargetsRequestV1().toSchema()));
   }



   /*******************************************************************************
    ** Success response contract.
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return (new BasicResponse("Delivery response", "DeliveryTargetStepResponseV1"));
   }



   /*******************************************************************************
    ** Write the operation response.
    *******************************************************************************/
   @Override
   public void handleOutput(Context context, DeliveryTargetStepResponseV1 output) throws Exception
   {
      UserPreferencesSpecUtils.writeResponse(context, output);
   }



   /*******************************************************************************
    ** Required chosen-values object.
    *******************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return (new RequestBody().withRequired(true).withContent(Map.of("application/json", new Content().withSchema(new Schema().withRefToSchema("DeliveryTargetsRequestV1")))));
   }
}
