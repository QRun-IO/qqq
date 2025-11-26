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

package com.kingsrook.qqq.middleware.javalin.routeproviders.authentication;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.javalin.QJavalinImplementation;
import io.javalin.http.Context;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** simple implementation of a route authenticator.  Assumes that unauthenticated
 ** requests should redirect to a login page.  Note though, maybe that should be
 ** more intelligent, like, only redirect requests for a .html file, but not
 ** requests for include files like images or .js/.css?
 *******************************************************************************/
public class SimpleRouteAuthenticator implements RouteAuthenticatorInterface
{
   private static final QLogger LOG = QLogger.getLogger(SimpleRouteAuthenticator.class);



   /*******************************************************************************
    ** Authenticate an HTTP request using QQQ's authentication system.
    **
    ** Sets up the QQQ session from the request context. If authentication fails,
    ** redirects unauthenticated requests to the login page provided by the
    ** authentication module.
    **
    ** Special handling for OAuth callbacks: If the request contains 'code' and
    ** 'state' query parameters (OAuth callback), redirects to the same URL with
    ** those parameters removed to prevent them from appearing in the browser URL.
    **
    ** @param context the Javalin HTTP context
    ** @return true if authenticated and should proceed; false if redirected
    ** @throws QException if authentication processing fails
    *******************************************************************************/
   public boolean authenticateRequest(Context context) throws QException
   {
      try
      {
         QSession qSession = QJavalinImplementation.setupSession(context, null);
         LOG.debug("Session has been activated", logPair("uuid", qSession.getUuid()));

         if(context.queryParamMap().containsKey("code") && context.queryParamMap().containsKey("state"))
         {
            //////////////////////////////////////////////////////////////////////////
            // if this request was a callback from oauth, with code & state params, //
            // then redirect one last time removing those from the query string     //
            //////////////////////////////////////////////////////////////////////////
            String redirectURL = context.fullUrl().replace("code=" + context.queryParam("code"), "")
               .replace("state=" + context.queryParam("state"), "")
               .replaceFirst("&+$", "")
               .replaceFirst("\\?&", "?")
               .replaceFirst("\\?$", "");
            context.redirect(redirectURL);
            LOG.debug("Redirecting request to remove code and state parameters");
            return (false);
         }

         return (true);
      }
      catch(QAuthenticationException e)
      {
         QAuthenticationModuleDispatcher qAuthenticationModuleDispatcher = new QAuthenticationModuleDispatcher();
         QAuthenticationModuleInterface  authenticationModule            = qAuthenticationModuleDispatcher.getQModule(QContext.getQInstance().getAuthentication());

         String redirectURL = authenticationModule.getLoginRedirectUrl(context.fullUrl());

         context.redirect(redirectURL);
         LOG.debug("Redirecting request, due to required session missing");
         return (false);
      }
      catch(QModuleDispatchException e)
      {
         throw (new QException("Error authenticating request", e));
      }
   }
}
