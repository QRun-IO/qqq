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
// SPDX-License-Identifier: Apache-2.0

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.executors.ExecutorSessionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.LogoutExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.LogoutInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiInProcessSessions;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.LogoutResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.json.JSONObject;


/*******************************************************************************
 ** Endpoint spec for logging out a session.
 *******************************************************************************/
public class LogoutSpecV1 extends AbstractEndpointSpec<LogoutInput, LogoutResponseV1, LogoutExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/logout")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.AUTHENTICATION)
         .withShortSummary("Logout and invalidate session")
         .withLongDescription("""
            Invalidates the current session server-side, deleting it from the session store
            and clearing any cached data. The session cookie is also removed.

            Frontends should call this endpoint before performing client-side logout
            (clearing localStorage, redirecting to IdP logout, etc.) to ensure the
            session cannot be resumed.""");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean isSecured()
   {
      return (false);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public LogoutResponseV1 serveRequest(AbstractMiddlewareVersion abstractMiddlewareVersion, Context context) throws Exception
   {
      context.header("Cache-Control", "no-store");
      try
      {
         return (super.serveRequest(abstractMiddlewareVersion, context));
      }
      finally
      {

         ///////////////////////////////////////////////////////////////////////////
         // Expire both cookie aliases even when local invalidation fails.        //
         ///////////////////////////////////////////////////////////////////////////
         context.removeCookie(QJavalinImplementation.SESSION_UUID_COOKIE_NAME);
         context.removeCookie(QJavalinImplementation.SESSION_ID_COOKIE_NAME);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public LogoutInput buildInput(Context context) throws Exception
   {
      LogoutInput logoutInput = new LogoutInput();
      logoutInput.setSessionUUID(context.cookie(QJavalinImplementation.SESSION_UUID_COOKIE_NAME));
      if(logoutInput.getSessionUUID() == null && (context.cookie(QJavalinImplementation.SESSION_ID_COOKIE_NAME) != null || context.header("Authorization") != null))
      {
         try
         {
            logoutInput.setSessionUUID(ExecutorSessionUtils.setupSession(context, qInstance).getUuid());
         }
         catch(QAuthenticationException ignored)
         {
            // An already invalid session can still have its cookies cleared.
         }
      }
      // Invalid optional input never blocks local logout and never releases a hint.
      try
      {
         if(context.header("Authorization") == null && context.header(UiApiInProcessSessions.HEADER) == null
            && context.header("x-api-key") == null && context.queryParam("code") == null && context.queryParam("state") == null
            && context.cookie(QJavalinImplementation.SESSION_UUID_COOKIE_NAME) != null
            && (context.cookie(QJavalinImplementation.SESSION_ID_COOKIE_NAME) == null
               || logoutInput.getSessionUUID().equals(context.cookie(QJavalinImplementation.SESSION_ID_COOKIE_NAME))))
         {
            JSONObject body = context.body().isBlank() ? new JSONObject() : new JSONObject(context.body());
            JSONObject nativeLogout = body.optJSONObject("nativeLogout");
            if(body.keySet().equals(Set.of("nativeLogout")) && nativeLogout != null
               && nativeLogout.keySet().equals(Set.of("postLogoutRedirectUri"))
               && nativeLogout.opt("postLogoutRedirectUri") instanceof String callback && callback.length() <= 2048
               && logoutInput.getSessionUUID().equals(ExecutorSessionUtils.setupSession(context, qInstance).getUuid()))
            {
               logoutInput.setNativePostLogoutRedirectUri(callback);
            }
         }
      }
      catch(Exception ignored)
      {
         // Never include raw parsing/provider exceptions or optional body data in logs.
      }
      return (logoutInput);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();

      examples.put("Successful logout", new Example().withValue(new LogoutResponseV1()));

      return new BasicResponse("Session has been invalidated",
         LogoutResponseV1.class.getSimpleName(),
         examples);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(
         LogoutResponseV1.class.getSimpleName(), new LogoutResponseV1().toSchema()
      );
   }



   /*******************************************************************************
    ** Additive opt-in; empty legacy requests retain local-only logout.
    *******************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return (new RequestBody().withRequired(false).withContent(Map.of(ContentType.JSON, new Content().withSchema(
         new Schema().withType(Type.OBJECT).withProperty("nativeLogout", new Schema().withType(Type.OBJECT)
            .withDescription("Optional 1.10 native provider logout. Only the current authenticated cookie can receive sensitive logout data. Absence retains local-only behavior.")
            .withProperty("postLogoutRedirectUri", new Schema().withType(Type.STRING)
               .withDescription("Exact registered callback from nativeSessionLogout.postLogoutRedirectUris.")))))));
   }

}
