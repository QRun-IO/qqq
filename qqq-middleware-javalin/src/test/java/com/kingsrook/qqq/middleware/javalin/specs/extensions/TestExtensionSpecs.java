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


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import com.kingsrook.qqq.middleware.javalin.executors.AbstractMiddlewareExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;


/*******************************************************************************
 ** Extension specs and a raw route provider, for tests of the UI API extension
 ** point.
 *******************************************************************************/
public class TestExtensionSpecs
{

   /***************************************************************************
    ** a valid extension with one spec
    ***************************************************************************/
   public static MiddlewareSpecExtension echoExtension()
   {
      return (new MiddlewareSpecExtension()
         .withName("test-ext")
         .withSemanticVersion("1.2.3")
         .withDescription("Test extension")
         .withSpec(EchoSpec.class)
         .withSpec(JsonSumSpec.class));
   }



   /***************************************************************************
    ** input for the echo spec
    ***************************************************************************/
   public static class EchoInput extends AbstractMiddlewareInput
   {
      private String word;



      /***************************************************************************
       ** Getter for word
       ***************************************************************************/
      public String getWord()
      {
         return (word);
      }



      /***************************************************************************
       ** Fluent setter for word
       ***************************************************************************/
      public EchoInput withWord(String word)
      {
         this.word = word;
         return (this);
      }
   }



   /***************************************************************************
    ** output interface for the echo spec
    ***************************************************************************/
   public interface EchoOutputInterface extends AbstractMiddlewareOutputInterface
   {
      /***************************************************************************
       ** set the echoed word
       ***************************************************************************/
      void setWord(String word);

      /***************************************************************************
       ** set the user id of the session
       ***************************************************************************/
      void setUserId(String userId);

      /***************************************************************************
       ** set the client channel of the session
       ***************************************************************************/
      void setClientChannel(String clientChannel);
   }



   /***************************************************************************
    ** response for the echo spec
    ***************************************************************************/
   public static class TestExtEchoResponseV1 implements EchoOutputInterface, ToSchema
   {
      @OpenAPIDescription("The word from the path.")
      private String word;

      @OpenAPIDescription("The user id of the session.")
      private String userId;

      @OpenAPIDescription("The client channel of the session.")
      private String clientChannel;



      /***************************************************************************
       ** Getter for clientChannel
       ***************************************************************************/
      public String getClientChannel()
      {
         return (clientChannel);
      }



      /***************************************************************************
       ** Setter for clientChannel
       ***************************************************************************/
      @Override
      public void setClientChannel(String clientChannel)
      {
         this.clientChannel = clientChannel;
      }



      /***************************************************************************
       ** Getter for word
       ***************************************************************************/
      public String getWord()
      {
         return (word);
      }



      /***************************************************************************
       ** Setter for word
       ***************************************************************************/
      @Override
      public void setWord(String word)
      {
         this.word = word;
      }



      /***************************************************************************
       ** Getter for userId
       ***************************************************************************/
      public String getUserId()
      {
         return (userId);
      }



      /***************************************************************************
       ** Setter for userId
       ***************************************************************************/
      @Override
      public void setUserId(String userId)
      {
         this.userId = userId;
      }
   }



   /***************************************************************************
    ** executor for the echo spec
    ***************************************************************************/
   public static class EchoExecutor extends AbstractMiddlewareExecutor<EchoInput, EchoOutputInterface>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void execute(EchoInput input, EchoOutputInterface output) throws QException
      {
         output.setWord(input.getWord());
         output.setClientChannel(QContext.getQSession() == null ? null : QContext.getQSession().getClientChannel());
         output.setUserId(QContext.getQSession() == null || QContext.getQSession().getUser() == null ? null : QContext.getQSession().getUser().getIdReference());
      }
   }



   /***************************************************************************
    ** GET /qbit/test-ext/echo/{word}
    ***************************************************************************/
   public static class EchoSpec extends AbstractEndpointSpec<EchoInput, TestExtEchoResponseV1, EchoExecutor>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public BasicOperation defineBasicOperation()
      {
         return (new BasicOperation()
            .withPath("/qbit/test-ext/echo/{word}")
            .withHttpMethod(HttpMethod.GET)
            .withShortSummary("Echo a word")
            .withLongDescription("Echoes the word in the path, with the session user."));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<Parameter> defineRequestParameters()
      {
         return (List.of(new Parameter()
            .withName("word")
            .withDescription("The word to echo.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withIn(In.PATH)));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public EchoInput buildInput(Context context) throws Exception
      {
         return (new EchoInput().withWord(context.pathParam("word")));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Map<String, Schema> defineComponentSchemas()
      {
         return (Map.of(TestExtEchoResponseV1.class.getSimpleName(), new TestExtEchoResponseV1().toSchema()));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public BasicResponse defineBasicSuccessResponse()
      {
         return (new BasicResponse("Successful Response", TestExtEchoResponseV1.class.getSimpleName()));
      }
   }



   /***************************************************************************
    ** POST /qbit/test-ext/sum/{label}?scale= with body {"numbers": [..]}
    ** - an AbstractJsonEndpointSpec
    ***************************************************************************/
   public static class JsonSumSpec extends AbstractJsonEndpointSpec
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public BasicOperation defineBasicOperation()
      {
         return (new BasicOperation()
            .withPath("/qbit/test-ext/sum/{label}")
            .withHttpMethod(HttpMethod.POST)
            .withShortSummary("Sum numbers")
            .withLongDescription("Sums the numbers in the body, times the scale."));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected Schema defineRequestBodySchema()
      {
         return (new Schema().withType(Type.OBJECT).withProperties(Map.of("numbers", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.NUMBER)))));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected String getResponseSchemaName()
      {
         return ("TestExtSumResponseV1");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Map<String, Schema> defineComponentSchemas()
      {
         return (Map.of("TestExtSumResponseV1", new Schema().withType(Type.OBJECT).withProperties(Map.of(
            "label", new Schema().withType(Type.STRING),
            "sum", new Schema().withType(Type.NUMBER),
            "note", new Schema().withType(Type.STRING).withNullable(true)))));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         Integer scale = input.getQueryParamInteger("scale");
         double  sum   = 0;
         for(Object number : (List<?>) input.getBody().getOrDefault("numbers", List.of()))
         {
            sum += ((Number) number).doubleValue();
         }

         Map<String, Object> rs = new java.util.LinkedHashMap<>();
         rs.put("label", input.getPathParam("label"));
         rs.put("sum", sum * (scale == null ? 1 : scale));
         rs.put("note", input.getQueryParam("note"));
         return (rs);
      }
   }



   /***************************************************************************
    ** a spec whose path is outside its extension's prefix
    ***************************************************************************/
   public static class WrongPrefixSpec extends EchoSpec
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public BasicOperation defineBasicOperation()
      {
         return (super.defineBasicOperation().withPath("/qbit/other/echo/{word}"));
      }
   }



   /***************************************************************************
    ** a raw (unspec'd) route under /qqq/v1
    ***************************************************************************/
   public static class RawRouteProvider implements QJavalinRouteProviderInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void setQInstance(QInstance qInstance)
      {
         // noop
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void acceptJavalinConfig(JavalinConfig config)
      {
         config.routes.get("/qqq/v1/raw-thing", context -> context.result("raw"));
      }
   }
}
