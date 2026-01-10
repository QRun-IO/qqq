/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs;


import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.javalin.QJavalinUtils;
import com.kingsrook.qqq.middleware.javalin.executors.AbstractMiddlewareExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.ExecutorSessionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareOutputInterface;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Method;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.commons.lang3.NotImplementedException;
import org.json.JSONObject;


/*******************************************************************************
 ** Base class for individual endpoint specs.
 ** e.g., one path, that has one "spec" (a "Method" in openapi structure),
 ** with one implementation (executor + input & output)
 *******************************************************************************/
public abstract class AbstractEndpointSpec<
   INPUT extends AbstractMiddlewareInput,
   OUTPUT extends AbstractMiddlewareOutputInterface,
   EXECUTOR extends AbstractMiddlewareExecutor<INPUT, ? super OUTPUT>>
{
   private static final QLogger LOG = QLogger.getLogger(AbstractEndpointSpec.class);

   protected QInstance qInstance;

   private List<Parameter> memoizedRequestParameters = null;
   private RequestBody     memoizedRequestBody       = null;



   /***************************************************************************
    ** build the endpoint's input object from a javalin context
    ***************************************************************************/
   public abstract INPUT buildInput(Context context) throws Exception;



   /***************************************************************************
    ** build the endpoint's http response (written to the javalin context) from
    ** an execution output object
    ***************************************************************************/
   public void handleOutput(Context context, OUTPUT output) throws Exception
   {
      context.result(JsonUtils.toJson(output));
   }



   /***************************************************************************
    ** Construct a new instance of the executor class, based on type-argument
    ***************************************************************************/
   @SuppressWarnings("unchecked")
   public EXECUTOR newExecutor()
   {
      Object object = newObjectFromTypeArgument(2);
      return (EXECUTOR) object;
   }



   /***************************************************************************
    ** Construct a new instance of the output class, based on type-argument
    ***************************************************************************/
   @SuppressWarnings("unchecked")
   public OUTPUT newOutput()
   {
      Object object = newObjectFromTypeArgument(1);
      return (OUTPUT) object;
   }



   /***************************************************************************
    ** credit: https://www.baeldung.com/java-generic-type-find-class-runtime
    ***************************************************************************/
   private Object newObjectFromTypeArgument(int argumentIndex)
   {
      try
      {
         /////////////////////////////////////////////////
         // moving up type hierarchy to find this class //
         /////////////////////////////////////////////////
         Class<?> s = getClass();
         while(!s.getSuperclass().equals(AbstractEndpointSpec.class))
         {
            s = s.getSuperclass();
         }

         Type     superClass         = s.getGenericSuperclass();
         Type     actualTypeArgument = ((ParameterizedType) superClass).getActualTypeArguments()[argumentIndex];
         String   className          = actualTypeArgument.getTypeName().replaceAll("<.*", "");
         Class<?> aClass             = Class.forName(className);
         return (aClass.getConstructor().newInstance());
      }
      catch(Exception e)
      {
         throw (new QRuntimeException("Failed to reflectively create new object from type argument", e));
      }
   }



   /***************************************************************************
    ** define a javalin route for the spec
    ***************************************************************************/
   public void defineRoute(AbstractMiddlewareVersion abstractMiddlewareVersion, String versionBasePath)
   {
      CompleteOperation completeOperation = defineCompleteOperation();

      String fullPath = "/qqq/" + versionBasePath + completeOperation.getPath();
      fullPath = fullPath.replaceAll("/+", "/");

      final Handler handler = context -> serveRequest(abstractMiddlewareVersion, context);

      LOG.trace("Binding: " + completeOperation.getHttpMethod() + " " + fullPath);
      switch(completeOperation.getHttpMethod())
      {
         case GET -> ApiBuilder.get(fullPath, handler);
         case POST -> ApiBuilder.post(fullPath, handler);
         case PUT -> ApiBuilder.put(fullPath, handler);
         case PATCH -> ApiBuilder.patch(fullPath, handler);
         case DELETE -> ApiBuilder.delete(fullPath, handler);
         default -> throw new IllegalStateException("Unexpected value: " + completeOperation.getHttpMethod());
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public OUTPUT serveRequest(AbstractMiddlewareVersion abstractMiddlewareVersion, Context context) throws Exception
   {
      try
      {
         if(isSecured())
         {
            ExecutorSessionUtils.setupSession(context, qInstance);
         }
         else
         {
            QContext.setQInstance(qInstance);
         }

         abstractMiddlewareVersion.preExecute(context);

         INPUT    input    = buildInput(context);
         EXECUTOR executor = newExecutor();
         OUTPUT   output   = newOutput();
         executor.execute(input, output);
         handleOutput(context, output);
         return (output);
      }
      catch(Exception e)
      {
         handleException(context, e);
         return (null);
      }
      finally
      {
         QContext.clear();
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected void handleException(Context context, Exception e)
   {
      QJavalinUtils.handleException(null, context, e);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public Method defineMethod()
   {
      BasicOperation basicOperation = defineBasicOperation();

      Method method = new Method()
         .withTag(basicOperation.getTag().getText())
         .withSummary(basicOperation.getShortSummary())
         .withDescription(basicOperation.getLongDescription())
         .withParameters(defineRequestParameters())
         .withRequestBody(defineRequestBody())
         .withResponses(defineResponses());

      customizeMethod(method);

      return (method);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected void customizeMethod(Method method)
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      throw new NotImplementedException(getClass().getSimpleName() + " did not implement defineBasicOperation or defineMethod");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public CompleteOperation defineCompleteOperation()
   {
      CompleteOperation completeOperation = new CompleteOperation(defineBasicOperation());
      completeOperation.setMethod(defineMethod());
      return completeOperation;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public boolean isSecured()
   {
      return (true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public BasicResponse defineBasicSuccessResponse()
   {
      return (null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public List<BasicResponse> defineAdditionalBasicResponses()
   {
      return (Collections.emptyList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public Map<Integer, Response> defineResponses()
   {
      BasicResponse       standardSuccessResponse = defineBasicSuccessResponse();
      List<BasicResponse> basicResponseList       = defineAdditionalBasicResponses();

      List<BasicResponse> allBasicResponses = new ArrayList<>();
      if(standardSuccessResponse != null)
      {
         allBasicResponses.add(standardSuccessResponse);
      }

      if(basicResponseList != null)
      {
         allBasicResponses.addAll(basicResponseList);
      }

      Map<Integer, Response> rs = new HashMap<>();
      for(BasicResponse basicResponse : allBasicResponses)
      {
         Response responseObject = rs.computeIfAbsent(basicResponse.status().getCode(), (k) -> new Response());
         responseObject.withDescription(basicResponse.description());
         Map<String, Content> content = responseObject.getContent();
         if(content == null)
         {
            content = new HashMap<>();
            responseObject.setContent(content);
         }

         content.put(basicResponse.contentType(), new Content()
            .withSchema(new Schema().withRefToSchema(basicResponse.schemaRefName()))
            .withExamples(basicResponse.examples())
         );
      }

      return rs;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public List<Parameter> defineRequestParameters()
   {
      return Collections.emptyList();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public RequestBody defineRequestBody()
   {
      return null;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public Map<String, Schema> defineComponentSchemas()
   {
      return Collections.emptyMap();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected JSONObject getRequestBodyAsJsonObject(Context context)
   {
      RequestBody requestBody = getMemoizedRequestBody();
      if(requestBody != null)
      {
         String requestContentType = context.contentType();
         if(requestContentType != null)
         {
            requestContentType = requestContentType.toLowerCase().replaceAll(" *;.*", "");
         }

         Content contentSpec = requestContentType == null ? null : requestBody.getContent().get(requestContentType);
         if(contentSpec != null && "object".equals(contentSpec.getSchema().getType()))
         {
            if(ContentType.APPLICATION_JSON.getMimeType().equals(requestContentType))
            {
               /////////////////////////////////////////////////////////////////////////////
               // avoid re-parsing the JSON object if getting multiple attributes from it //
               // by stashing it in a (request) attribute.                                //
               /////////////////////////////////////////////////////////////////////////////
               Object     jsonBodyAttribute = context.attribute("jsonBody");
               JSONObject jsonObject        = null;

               if(jsonBodyAttribute instanceof JSONObject jo)
               {
                  jsonObject = jo;
               }

               if(jsonObject == null)
               {
                  String body = context.body();
                  if(StringUtils.hasContent(body))
                  {
                     jsonObject = new JSONObject(body);
                     context.attribute("jsonBody", jsonObject);
                  }
               }

               return (jsonObject);
            }
         }
      }

      return (null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected String getRequestParam(Context context, String name)
   {
      for(Parameter parameter : CollectionUtils.nonNullList(getMemoizedRequestParameters()))
      {
         if(parameter.getName().equals(name))
         {
            String value = switch(parameter.getIn())
            {
               case "path" -> context.pathParam(parameter.getName());
               case "query" -> context.queryParam(parameter.getName());
               default -> throw new IllegalStateException("Unexpected 'in' value for parameter [" + parameter.getName() + "]: " + parameter.getIn());
            };

            // todo - validate value vs. required?
            // todo - validate value vs. schema?

            return (value);
         }
      }

      RequestBody requestBody = getMemoizedRequestBody();
      if(requestBody != null)
      {
         String requestContentType = context.contentType();
         if(requestContentType != null)
         {
            requestContentType = requestContentType.toLowerCase().replaceAll(" *;.*", "");
         }

         Content contentSpec = requestBody.getContent().get(requestContentType);
         if(contentSpec != null && "object".equals(contentSpec.getSchema().getType()))
         {
            if(contentSpec.getSchema().getProperties() != null && contentSpec.getSchema().getProperties().containsKey(name))
            {
               String value = null;
               if(ContentType.MULTIPART_FORM_DATA.getMimeType().equals(requestContentType))
               {
                  value = context.formParam(name);
               }
               else if(ContentType.APPLICATION_JSON.getMimeType().equals(requestContentType))
               {
                  JSONObject jsonObject = getRequestBodyAsJsonObject(context);
                  if(jsonObject.has(name))
                  {
                     value = jsonObject.getString(name);
                  }
               }
               else
               {
                  LOG.warn("Unhandled content type: " + requestContentType);
               }

               return (value);
            }
         }
      }

      return (null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected Map<String, Serializable> getRequestParamMap(Context context, String name)
   {
      String requestParam = getRequestParam(context, name);
      if(requestParam == null)
      {
         return (null);
      }

      JSONObject                jsonObject = new JSONObject(requestParam);
      Map<String, Serializable> map        = new LinkedHashMap<>();
      for(String key : jsonObject.keySet())
      {
         Object value = jsonObject.get(key);
         if(value instanceof Serializable s)
         {
            map.put(key, s);
         }
         else
         {
            throw (new QRuntimeException("Non-serializable value in param map under key [" + name + "][" + key + "]: " + value.getClass().getSimpleName()));
         }
      }
      return (map);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected Integer getRequestParamInteger(Context context, String name)
   {
      String requestParam = getRequestParam(context, name);
      return ValueUtils.getValueAsInteger(requestParam);
   }



   /***************************************************************************
    ** For initial setup when server boots, set the qInstance - but also,
    ** e.g., for development, to do a hot-swap.
    ***************************************************************************/
   public void setQInstance(QInstance qInstance)
   {
      this.qInstance = qInstance;

      //////////////////////////////////////////////////////////////////
      // if we did a hot swap, we should clear these memoizations too //
      //////////////////////////////////////////////////////////////////
      memoizedRequestParameters = null;
      memoizedRequestBody = null;
   }



   /***************************************************************************
    ** An original implementation here was prone to race-condition-based errors:
    *
    ** if(memoizedRequestParameters == null)
    ** {
    **    memoizedRequestParameters = CollectionUtils.nonNullList(defineRequestParameters());
    ** }
    ** return (memoizedRequestParameters);
    **
    ** where between the defineX call and the return, if another thread cleared the
    ** memoizedX field, then a null would be returned, which isn't supposed to happen.
    ** Thus, this implementation which looks a bit more convoluted, but should
    ** be safe(r).
    ***************************************************************************/
   private List<Parameter> getMemoizedRequestParameters()
   {
      List<Parameter> rs = memoizedRequestParameters;
      if(rs == null)
      {
         rs = CollectionUtils.nonNullList(defineRequestParameters());
         memoizedRequestParameters = rs;
      }

      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private RequestBody getMemoizedRequestBody()
   {
      RequestBody rs = memoizedRequestBody;
      if(rs == null)
      {
         rs = defineRequestBody();
         memoizedRequestBody = rs;
      }

      return (rs);
   }

}
