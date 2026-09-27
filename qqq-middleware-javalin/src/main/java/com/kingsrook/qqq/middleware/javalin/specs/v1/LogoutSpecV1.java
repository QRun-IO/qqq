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


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.executors.LogoutExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.LogoutInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.LogoutResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;


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
      LogoutResponseV1 result = super.serveRequest(abstractMiddlewareVersion, context);

      ///////////////////////////////////////////////////////////////////////////
      // remove the session cookies by setting them to expired.  sessionId is   //
      // also issued (by modules that use it) on every request; a stale one    //
      // would otherwise shadow the next sign-in's sessionUUID.                //
      ///////////////////////////////////////////////////////////////////////////
      context.removeCookie(QJavalinImplementation.SESSION_UUID_COOKIE_NAME);
      context.removeCookie(QJavalinImplementation.SESSION_ID_COOKIE_NAME);

      return (result);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public LogoutInput buildInput(Context context) throws Exception
   {
      LogoutInput logoutInput = new LogoutInput();
      logoutInput.setSessionUUID(context.cookie(QJavalinImplementation.SESSION_UUID_COOKIE_NAME));
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

}
