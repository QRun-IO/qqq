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
import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.WidgetExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.WidgetInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.WidgetResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.json.JSONObject;


/*******************************************************************************
 ** Endpoint spec for rendering a widget via POST.
 *******************************************************************************/
public class WidgetSpecV1 extends AbstractEndpointSpec<WidgetInput, WidgetResponseV1, WidgetExecutor>
{

   /***************************************************************************
    ** Note: Legacy widget endpoint supported both GET and POST. V1 API uses POST-only
    ** since widget parameters are passed in the JSON request body.
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/widget/{widgetName}")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.WIDGETS)
         .withShortSummary("Render a widget")
         .withLongDescription("""
            Render a widget by name, passing in query parameters as a JSON object in the request body.
            The response shape varies by widget type.""");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return List.of(
         new Parameter()
            .withName("widgetName")
            .withDescription("Name of the widget to render.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("myWidget")
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return new RequestBody()
         .withContent(
            ContentType.APPLICATION_JSON.getMimeType(), new Content()
               .withSchema(new Schema()
                  .withType(Type.OBJECT)
                  .withDescription("Dynamic query parameters to pass to the widget renderer.")
               )
         );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public WidgetInput buildInput(Context context) throws Exception
   {
      WidgetInput input = new WidgetInput();
      input.setWidgetName(getRequestParam(context, "widgetName"));

      /////////////////////////////////////////////////////////////////////////////////
      // widget inputs may come as query parameters (as on the legacy GET route) or //
      // in the JSON body; a body value wins over a query parameter of the same name //
      /////////////////////////////////////////////////////////////////////////////////
      context.queryParamMap().forEach((name, values) ->
      {
         if(values != null && !values.isEmpty())
         {
            input.getQueryParams().put(name, values.get(0));
         }
      });

      JSONObject requestBody = getRequestBodyAsJsonObject(context);
      if(requestBody != null)
      {
         for(String key : requestBody.keySet())
         {
            input.getQueryParams().put(key, requestBody.optString(key));
         }
      }

      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(WidgetResponseV1.class.getSimpleName(), new WidgetResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("""
         The rendered widget data, whose shape varies by widget type.""",
         WidgetResponseV1.class.getSimpleName()
      );
   }



   /***************************************************************************
    ** Override to serialize just the widgetData portion directly, rather than
    ** the wrapper response object.
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, WidgetResponseV1 output) throws Exception
   {
      context.result(JsonUtils.toJson(output.getWidgetData()));
   }

}
