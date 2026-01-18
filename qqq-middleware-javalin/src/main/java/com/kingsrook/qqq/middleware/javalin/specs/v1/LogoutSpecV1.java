/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.javalin.QJavalinImplementation;
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

      ////////////////////////////////////////////////////////
      // remove the session cookie by setting it to expired //
      ////////////////////////////////////////////////////////
      context.removeCookie(QJavalinImplementation.SESSION_UUID_COOKIE_NAME);

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
