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
import com.kingsrook.qqq.middleware.javalin.executors.HelpSearchExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.HelpSearchMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.HelpSearchResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
    ** HelpSearchSpecV1 contract.
    *******************************************************************************/
public class HelpSearchSpecV1 extends AbstractEndpointSpec<HelpSearchMiddlewareInput, HelpSearchResponseV1, HelpSearchExecutor>
{
   /*******************************************************************************
    ** Describe the secured help endpoint.
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath("/help/search").withHttpMethod(HttpMethod.GET).withTag(TagsV1.GENERAL)
         .withShortSummary("Search visible application help")
         .withLongDescription("Search help visible to the session. Label hits weigh 3, summary hits 2 and content hits 1. Stable ties sort by kind and key. Limit defaults to 20 and is capped at 50."));
   }



   /*******************************************************************************
    ** Query parameters.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(
         new Parameter().withName("q").withIn(In.QUERY).withRequired(true).withSchema(new Schema().withType(Type.STRING)),
         new Parameter().withName("limit").withIn(In.QUERY).withRequired(false).withSchema(new Schema().withType(Type.NUMBER))));
   }



   /*******************************************************************************
    ** Parse input; malformed parameters receive HTTP 400.
    *******************************************************************************/
   @Override
   public HelpSearchMiddlewareInput buildInput(Context context) throws Exception
   {
      String limit = getRequestParam(context, "limit");
      try
      {
         return (new HelpSearchMiddlewareInput().withQuery(getRequestParam(context, "q")).withLimit(limit == null ? null : Integer.valueOf(limit)));
      }
      catch(NumberFormatException e)
      {
         throw (new QBadRequestException("limit must be an integer."));
      }
   }



   /*******************************************************************************
    ** Response schema.
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return (Map.of(HelpSearchResponseV1.class.getSimpleName(), new HelpSearchResponseV1().toSchema()));
   }



   /*******************************************************************************
    ** Success response.
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return (new BasicResponse("Help search results", HelpSearchResponseV1.class.getSimpleName(), Map.of()));
   }



   /*******************************************************************************
    ** Serialize the public response.
    *******************************************************************************/
   @Override
   public void handleOutput(Context context, HelpSearchResponseV1 output) throws Exception
   {
      context.result(JsonUtils.toJson(output));
   }
}
