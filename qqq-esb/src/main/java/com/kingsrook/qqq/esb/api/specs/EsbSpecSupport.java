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

package com.kingsrook.qqq.esb.api.specs;


import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.api.EsbStatusBuilder;
import com.kingsrook.qqq.middleware.javalin.executors.AbstractMiddlewareExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareOutputInterface;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.TagsInterface;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;


/*******************************************************************************
 ** Shared pieces of the ESB UI API specs (QRun-IO/qqq docs/UI-API-EXTENSIONS.md):
 ** the input (which body to build, from what parameters), the executor (runs
 ** EsbStatusBuilder with the session in context), the response (the built
 ** body, written with nulls kept), and the base spec.
 *******************************************************************************/
public class EsbSpecSupport
{
   /***************************************************************************
    ** OpenAPI tag of the ESB operations.
    ***************************************************************************/
   public enum EsbTag implements TagsInterface
   {
      ESB("ESB");

      private final String text;



      /***************************************************************************
       **
       ***************************************************************************/
      EsbTag(String text)
      {
         this.text = text;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getText()
      {
         return (text);
      }
   }



   /***************************************************************************
    ** Builds one operation's body from an EsbStatusBuilder.
    ***************************************************************************/
   @FunctionalInterface
   public interface BodyBuilder
   {
      /***************************************************************************
       **
       ***************************************************************************/
      Map<String, Object> build(EsbStatusBuilder builder) throws QException;
   }



   /***************************************************************************
    ** input: the body builder bound to the request's parameters
    ***************************************************************************/
   public static class EsbInput extends AbstractMiddlewareInput
   {
      private BodyBuilder bodyBuilder;



      /***************************************************************************
       ** Getter for bodyBuilder
       ***************************************************************************/
      public BodyBuilder getBodyBuilder()
      {
         return (bodyBuilder);
      }



      /***************************************************************************
       ** Fluent setter for bodyBuilder
       ***************************************************************************/
      public EsbInput withBodyBuilder(BodyBuilder bodyBuilder)
      {
         this.bodyBuilder = bodyBuilder;
         return (this);
      }
   }



   /***************************************************************************
    ** output interface
    ***************************************************************************/
   public interface EsbOutputInterface extends AbstractMiddlewareOutputInterface
   {
      /***************************************************************************
       ** set the response body
       ***************************************************************************/
      void setBody(Map<String, Object> body);
   }



   /***************************************************************************
    ** response: the body EsbStatusBuilder built (its schema is per operation)
    ***************************************************************************/
   public static class EsbResponseV1 implements EsbOutputInterface
   {
      private Map<String, Object> body;



      /***************************************************************************
       ** Getter for body
       ***************************************************************************/
      public Map<String, Object> getBody()
      {
         return (body);
      }



      /***************************************************************************
       ** Setter for body
       ***************************************************************************/
      @Override
      public void setBody(Map<String, Object> body)
      {
         this.body = body;
      }
   }



   /***************************************************************************
    ** executor: build the body with the session the spec set up
    ***************************************************************************/
   public static class EsbExecutor extends AbstractMiddlewareExecutor<EsbInput, EsbOutputInterface>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void execute(EsbInput input, EsbOutputInterface output) throws QException
      {
         output.setBody(input.getBodyBuilder().build(new EsbStatusBuilder()));
      }
   }



   /***************************************************************************
    ** base of the ESB specs: secured, tagged, JSON written with nulls kept (as
    ** the route provider before them did), success schema per operation.
    ***************************************************************************/
   public abstract static class AbstractEsbSpecV1 extends AbstractEndpointSpec<EsbInput, EsbResponseV1, EsbExecutor>
   {
      /***************************************************************************
       ** name of the component schema of the success response
       ***************************************************************************/
      protected abstract String getResponseSchemaName();



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void handleOutput(Context context, EsbResponseV1 output) throws Exception
      {
         context.contentType(ContentType.APPLICATION_JSON);
         context.result(JsonUtils.toJsonCustomized(output.getBody(), builder -> builder.serializationInclusion(JsonInclude.Include.ALWAYS)));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Map<String, Schema> defineComponentSchemas()
      {
         return (EsbUiApiSchemas.all());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public BasicResponse defineBasicSuccessResponse()
      {
         return (new BasicResponse("Successful Response", getResponseSchemaName()));
      }
   }



   /*******************************************************************************
    ** a required string path parameter
    *******************************************************************************/
   public static Parameter pathParameter(String name, String description)
   {
      return (new Parameter().withName(name).withDescription(description).withRequired(true).withSchema(new Schema().withType(Type.STRING)).withIn(In.PATH));
   }



   /*******************************************************************************
    ** an optional query parameter
    *******************************************************************************/
   public static Parameter queryParameter(String name, String description, Type type)
   {
      return (new Parameter().withName(name).withDescription(description).withRequired(false).withSchema(new Schema().withType(type)).withIn(In.QUERY));
   }



   /*******************************************************************************
    ** the offset and limit paging parameters
    *******************************************************************************/
   public static List<Parameter> pagingParameters()
   {
      return (List.of(
         queryParameter("offset", "Messages to skip (default 0).", Type.INTEGER),
         queryParameter("limit", "Messages to return (default and maximum set by the server).", Type.INTEGER)));
   }



   /*******************************************************************************
    ** An integer query parameter - null if absent; QBadRequestException if it
    ** isn't an integer.
    *******************************************************************************/
   public static Integer getIntegerParam(Context context, String name) throws QBadRequestException
   {
      String value = context.queryParam(name);
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
}
