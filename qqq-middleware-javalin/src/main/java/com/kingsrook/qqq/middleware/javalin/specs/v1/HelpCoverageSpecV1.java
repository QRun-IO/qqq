/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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
import com.kingsrook.qqq.middleware.javalin.executors.HelpCoverageExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.HelpCoverageMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BasicErrorResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.HelpCoverageResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;


/*******************************************************************************
    ** HelpCoverageSpecV1 contract.
    *******************************************************************************/
public class HelpCoverageSpecV1 extends AbstractEndpointSpec<HelpCoverageMiddlewareInput, HelpCoverageResponseV1, HelpCoverageExecutor>
{
   /*******************************************************************************
    ** Describe the secured help endpoint.
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath("/help/coverage").withHttpMethod(HttpMethod.GET).withTag(TagsV1.GENERAL)
         .withShortSummary("Inspect application help coverage")
         .withLongDescription("Requires INSERT or EDIT permission on helpContent. Includes hidden targets. Totals reflect kind and parent scope before missingOnly is applied."));
   }



   /*******************************************************************************
    ** Query parameters.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(
         new Parameter().withName("kind").withIn(In.QUERY).withRequired(false).withSchema(new Schema().withType(Type.STRING)),
         new Parameter().withName("missingOnly").withIn(In.QUERY).withRequired(false).withSchema(new Schema().withType(Type.BOOLEAN)),
         new Parameter().withName("parentKey").withIn(In.QUERY).withRequired(false).withSchema(new Schema().withType(Type.STRING))));
   }



   /*******************************************************************************
    ** Parse input; malformed parameters receive HTTP 400.
    *******************************************************************************/
   @Override
   public HelpCoverageMiddlewareInput buildInput(Context context) throws Exception
   {
      String missingOnly = getRequestParam(context, "missingOnly");
      if(missingOnly != null && !List.of("true", "false").contains(missingOnly))
      {
         throw (new QBadRequestException("missingOnly must be true or false."));
      }
      return (new HelpCoverageMiddlewareInput().withKind(getRequestParam(context, "kind")).withMissingOnly(Boolean.parseBoolean(missingOnly)).withParentKey(getRequestParam(context, "parentKey")));
   }



   /*******************************************************************************
    ** Response schema.
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return (Map.of(HelpCoverageResponseV1.class.getSimpleName(), new HelpCoverageResponseV1().toSchema(),
         BasicErrorResponseV1.class.getSimpleName(), new BasicErrorResponseV1().toSchema()));
   }



   /*******************************************************************************
    ** Success response.
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return (new BasicResponse("Help coverage results", HelpCoverageResponseV1.class.getSimpleName(), Map.of()));
   }



   /*******************************************************************************
    ** Serialize the public response.
    *******************************************************************************/
   @Override
   public void handleOutput(Context context, HelpCoverageResponseV1 output) throws Exception
   {
      context.result(JsonUtils.toJson(output));
   }


   /*******************************************************************************
    ** Coverage requires an author permission on helpContent.
    *******************************************************************************/
   @Override
   public List<BasicResponse> defineAdditionalBasicResponses()
   {
      return (List.of(new BasicResponse(HttpStatus.FORBIDDEN, "No INSERT or EDIT permission on helpContent", BasicErrorResponseV1.class.getSimpleName(), Map.of())));
   }
}
