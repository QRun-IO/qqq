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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;
import io.javalin.http.Context;


/*******************************************************************************
 **
 *******************************************************************************/
public class ExecutorSessionUtils
{
   private static final QLogger LOG = QLogger.getLogger(ExecutorSessionUtils.class);

   public static final int    SESSION_COOKIE_AGE       = 60 * 60 * 24;
   public static final String SESSION_ID_COOKIE_NAME   = "sessionId";
   public static final String API_KEY_NAME             = "apiKey";



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QSession setupSession(Context context, QInstance qInstance) throws QModuleDispatchException, QAuthenticationException
   {
      QAuthenticationModuleDispatcher qAuthenticationModuleDispatcher = new QAuthenticationModuleDispatcher();
      QAuthenticationModuleInterface  authenticationModule            = qAuthenticationModuleDispatcher.getQModule(qInstance.getAuthentication());

      try
      {
         /////////////////////////////////////////////////
         // note:  duplicated in QJavalinImplementation //
         /////////////////////////////////////////////////
         Map<String, String> authenticationContext = new HashMap<>();

         String sessionIdCookieValue     = context.cookie(SESSION_ID_COOKIE_NAME);
         String sessionUuidCookieValue   = context.cookie(Auth0AuthenticationModule.SESSION_UUID_KEY);
         String authorizationHeaderValue = context.header("Authorization");
         String apiKeyHeaderValue        = context.header("x-api-key");
         String codeQueryParamValue      = context.queryParam("code");
         String stateQueryParamValue     = context.queryParam("state");

         if(StringUtils.hasContent(codeQueryParamValue) && StringUtils.hasContent(stateQueryParamValue))
         {
            authenticationContext.put("code", codeQueryParamValue);
            authenticationContext.put("state", stateQueryParamValue);
         }
         else if(StringUtils.hasContent(sessionIdCookieValue))
         {
            ///////////////////////////////////////////////////////
            // sessionId - maybe used by table-based auth module //
            ///////////////////////////////////////////////////////
            authenticationContext.put(SESSION_ID_COOKIE_NAME, sessionIdCookieValue);
         }
         else if(StringUtils.hasContent(sessionUuidCookieValue))
         {
            ///////////////////////////////////////////////////////////////////////////
            // session UUID - known to be used by auth0 module (in aug. 2023 update) //
            ///////////////////////////////////////////////////////////////////////////
            authenticationContext.put(Auth0AuthenticationModule.SESSION_UUID_KEY, sessionUuidCookieValue);
         }
         else if(apiKeyHeaderValue != null)
         {
            /////////////////////////////////////////////////////////////////
            // next, look for an api key header:                           //
            // this will be used to look up auth0 values via an auth table //
            /////////////////////////////////////////////////////////////////
            authenticationContext.put(API_KEY_NAME, apiKeyHeaderValue);
         }
         else if(authorizationHeaderValue != null)
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////
            // second, look for the authorization header:                                                  //
            // either with a "Basic " prefix (for a username:password pair)                                //
            // or with a "Bearer " prefix (for a token that can be handled the same as a sessionId cookie) //
            /////////////////////////////////////////////////////////////////////////////////////////////////
            processAuthorizationValue(authenticationContext, authorizationHeaderValue);
         }
         else
         {
            try
            {
               String authorizationFormValue = context.formParam("Authorization");
               if(StringUtils.hasContent(authorizationFormValue))
               {
                  processAuthorizationValue(authenticationContext, authorizationFormValue);
               }
            }
            catch(Exception e)
            {
               LOG.info("Exception looking for Authorization formParam", e);
            }
         }

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // put the qInstance into context - but no session yet (since, the whole point of this call is to setup the session!) //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         QContext.init(qInstance, null);
         QSession session = authenticationModule.createSession(qInstance, authenticationContext);
         QContext.init(qInstance, session, null, null);

         //////////////////////////////////////////////////////////////////////////////////////////////
         // note - QJavalinImplementation did table variants here - but we have a method             //
         // that each executor must call instead, with wherever the tableVariant was in its input... //
         //////////////////////////////////////////////////////////////////////////////////////////////

         /////////////////////////////////////////////////////////////////////////////////
         // if we got a session id cookie in, then send it back with updated cookie age //
         /////////////////////////////////////////////////////////////////////////////////
         if(authenticationModule.usesSessionIdCookie())
         {
            // Use getIdReference() if available, otherwise fallback to getUuid() for OAuth2 sessions
            String sessionId = session.getIdReference();
            if(sessionId == null)
            {
               sessionId = session.getUuid();
            }
            context.cookie(SESSION_ID_COOKIE_NAME, sessionId, SESSION_COOKIE_AGE);
         }

         setUserTimezoneOffsetMinutesInSession(context, session);
         setUserTimezoneInSession(context, session);

         return (session);
      }
      catch(QAuthenticationException qae)
      {
         ////////////////////////////////////////////////////////////////////////////////
         // if exception caught, clear out the cookie so the frontend will reauthorize //
         ////////////////////////////////////////////////////////////////////////////////
         if(authenticationModule.usesSessionIdCookie())
         {
            context.removeCookie(SESSION_ID_COOKIE_NAME);
         }

         throw (qae);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processAuthorizationValue(Map<String, String> authenticationContext, String authorizationHeaderValue)
   {
      String basicPrefix  = "Basic ";
      String bearerPrefix = "Bearer ";
      if(authorizationHeaderValue.startsWith(basicPrefix))
      {
         authorizationHeaderValue = authorizationHeaderValue.replaceFirst(basicPrefix, "");
         authenticationContext.put(Auth0AuthenticationModule.BASIC_AUTH_KEY, authorizationHeaderValue);
      }
      else if(authorizationHeaderValue.startsWith(bearerPrefix))
      {
         authorizationHeaderValue = authorizationHeaderValue.replaceFirst(bearerPrefix, "");
         authenticationContext.put(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, authorizationHeaderValue);
      }
      else
      {
         LOG.debug("Authorization value did not have Basic or Bearer prefix. [" + authorizationHeaderValue + "]");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setUserTimezoneOffsetMinutesInSession(Context context, QSession session)
   {
      String userTimezoneOffsetMinutes = context.header("X-QQQ-UserTimezoneOffsetMinutes");
      if(StringUtils.hasContent(userTimezoneOffsetMinutes))
      {
         try
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // even though we're putting it in the session as a string, go through parse int, to make sure it's a valid int. //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            session.setValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES, String.valueOf(Integer.parseInt(userTimezoneOffsetMinutes)));
         }
         catch(Exception e)
         {
            LOG.debug("Received non-integer value for X-QQQ-UserTimezoneOffsetMinutes header: " + userTimezoneOffsetMinutes);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setUserTimezoneInSession(Context context, QSession session)
   {
      String userTimezone = context.header("X-QQQ-UserTimezone");
      if(StringUtils.hasContent(userTimezone))
      {
         session.setValue(QSession.VALUE_KEY_USER_TIMEZONE, userTimezone);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void setTableVariantInSession(TableVariant tableVariant)
   {
      if(tableVariant != null && StringUtils.hasContent(tableVariant.getType()) && StringUtils.hasContent(tableVariant.getId()))
      {
         QContext.getQSession().setBackendVariants(MapBuilder.of(tableVariant.getType(), tableVariant.getId()));
      }
   }
}
