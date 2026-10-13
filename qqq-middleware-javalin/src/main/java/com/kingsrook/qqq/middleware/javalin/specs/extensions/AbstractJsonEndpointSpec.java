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

package com.kingsrook.qqq.middleware.javalin.specs.extensions;


import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.executors.AbstractMiddlewareExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareOutputInterface;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.ContentType;
import io.javalin.http.Context;


/*******************************************************************************
 ** Base for extension endpoint specs whose request is path/query parameters and
 ** an optional JSON object body, and whose response is a JSON value - the
 ** common shape of module and qbit endpoints.  A subclass defines the operation
 ** (path, method, parameters, request and response schemas) and handle(); the
 ** session is set up (for secured specs) before handle runs, and the result is
 ** written as JSON with nulls kept.
 **
 ** Each subclass needs a public no-arg constructor (it is registered by class
 ** in a MiddlewareSpecExtension).  See docs/UI-API-EXTENSIONS.md.
 *******************************************************************************/
public abstract class AbstractJsonEndpointSpec extends AbstractEndpointSpec<AbstractJsonEndpointSpec.JsonInput, AbstractJsonEndpointSpec.JsonOutput, AbstractJsonEndpointSpec.JsonExecutor>
{

   /***************************************************************************
    ** Do the operation's work (the QContext has the session for a secured
    ** spec) and return the response body (serialized to JSON).
    ***************************************************************************/
   protected abstract Object handle(JsonInput input) throws Exception;



   /***************************************************************************
    ** The component schema name of the 200 response body.
    ***************************************************************************/
   protected abstract String getResponseSchemaName();



   /***************************************************************************
    ** The schema of the JSON request body, or null (the default) for none.
    ***************************************************************************/
   protected Schema defineRequestBodySchema()
   {
      return (null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      Schema schema = defineRequestBodySchema();
      if(schema == null)
      {
         return (null);
      }

      return (new RequestBody()
         .withRequired(true)
         .withContent(Map.of(ContentType.APPLICATION_JSON.getMimeType(), new Content().withSchema(schema))));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return (new BasicResponse("Successful Response", getResponseSchemaName()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public JsonInput buildInput(Context context) throws Exception
   {
      JsonInput input = new JsonInput();
      input.spec = this;
      input.pathParams.putAll(context.pathParamMap());
      context.queryParamMap().forEach((name, values) -> input.queryParams.put(name, values == null || values.isEmpty() ? null : values.get(0)));

      if(defineRequestBodySchema() != null)
      {
         input.body = readJsonBody(context);
      }

      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, JsonOutput output) throws Exception
   {
      context.contentType(ContentType.APPLICATION_JSON);
      context.result(JsonUtils.toJsonCustomized(output.getResult(), builder -> builder.serializationInclusion(JsonInclude.Include.ALWAYS)));
   }



   /***************************************************************************
    ** The JSON object body (empty when there is no body), or 400.
    ***************************************************************************/
   @SuppressWarnings("unchecked")
   static Map<String, Object> readJsonBody(Context context) throws QBadRequestException
   {
      String body = context.body();
      if(!StringUtils.hasContent(body))
      {
         return (new LinkedHashMap<>());
      }

      String contentType = context.contentType();
      if(contentType == null || !contentType.toLowerCase().startsWith("application/json"))
      {
         throw (new QBadRequestException("The request body must be JSON (Content-Type: application/json)."));
      }

      try
      {
         return (JsonUtils.toObject(body, LinkedHashMap.class));
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("The request body is not a valid JSON object."));
      }
   }



   /***************************************************************************
    ** The request: path parameters, the first value of each query parameter,
    ** and the JSON body (empty unless the spec defines one).
    ***************************************************************************/
   public static class JsonInput extends AbstractMiddlewareInput
   {
      private final Map<String, String> pathParams  = new LinkedHashMap<>();
      private final Map<String, String> queryParams = new LinkedHashMap<>();
      private Map<String, Object>       body        = new LinkedHashMap<>();
      private AbstractJsonEndpointSpec  spec;



      /***************************************************************************
       ** A path parameter.
       ***************************************************************************/
      public String getPathParam(String name)
      {
         return (pathParams.get(name));
      }



      /***************************************************************************
       ** A query parameter (null when absent).
       ***************************************************************************/
      public String getQueryParam(String name)
      {
         return (queryParams.get(name));
      }



      /***************************************************************************
       ** An integer query parameter: null when absent, 400 when not an integer.
       ***************************************************************************/
      public Integer getQueryParamInteger(String name) throws QBadRequestException
      {
         String value = queryParams.get(name);
         if(!StringUtils.hasContent(value))
         {
            return (null);
         }

         try
         {
            return (Integer.parseInt(value.trim()));
         }
         catch(NumberFormatException e)
         {
            throw (new QBadRequestException("The " + name + " parameter must be an integer."));
         }
      }



      /***************************************************************************
       ** Getter for queryParams (first value of each)
       ***************************************************************************/
      public Map<String, String> getQueryParams()
      {
         return (queryParams);
      }



      /***************************************************************************
       ** Getter for pathParams
       ***************************************************************************/
      public Map<String, String> getPathParams()
      {
         return (pathParams);
      }



      /***************************************************************************
       ** Getter for body
       ***************************************************************************/
      public Map<String, Object> getBody()
      {
         return (body);
      }



      /***************************************************************************
       ** Getter for spec
       ***************************************************************************/
      public AbstractJsonEndpointSpec getSpec()
      {
         return (spec);
      }
   }



   /***************************************************************************
    ** output interface
    ***************************************************************************/
   public interface JsonOutputInterface extends AbstractMiddlewareOutputInterface
   {
      /***************************************************************************
       ** set the response body
       ***************************************************************************/
      void setResult(Object result);
   }



   /***************************************************************************
    ** the response body
    ***************************************************************************/
   public static class JsonOutput implements JsonOutputInterface
   {
      private Object result;



      /***************************************************************************
       ** Getter for result
       ***************************************************************************/
      public Object getResult()
      {
         return (result);
      }



      /***************************************************************************
       ** Setter for result
       ***************************************************************************/
      @Override
      public void setResult(Object result)
      {
         this.result = result;
      }
   }



   /***************************************************************************
    ** runs the spec's handle()
    ***************************************************************************/
   public static class JsonExecutor extends AbstractMiddlewareExecutor<JsonInput, JsonOutputInterface>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void execute(JsonInput input, JsonOutputInterface output) throws QException
      {
         try
         {
            output.setResult(input.getSpec().handle(input));
         }
         catch(QException e)
         {
            throw (e);
         }
         catch(Exception e)
         {
            throw (new QException(e.getMessage(), e));
         }
      }
   }
}
