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

package com.kingsrook.qqq.backend.core.model.metadata.authentication;


import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility class for resolving the appropriate authentication provider for
 ** a request based on its scope.
 **
 ** <p>The resolver checks scopes in order of specificity:
 ** <ol>
 **   <li>API-specific scope (if context has API metadata)</li>
 **   <li>Route provider-specific scope (if context has route provider metadata)</li>
 **   <li>Instance default scope (fallback)</li>
 ** </ol>
 ** </p>
 **
 ** <p>If no provider is found, a QException is thrown with appropriate error
 ** message.</p>
 *******************************************************************************/
public final class AuthenticationResolver
{
   private static final QLogger LOG = QLogger.getLogger(AuthenticationResolver.class);


   /*******************************************************************************
    ** Private constructor - utility class
    *******************************************************************************/
   private AuthenticationResolver()
   {
   }


   /*******************************************************************************
    ** Resolve the authentication provider for the given context.
    **
    ** <p>Checks scopes in order of specificity:
    ** <ol>
    **   <li>API scope (if apiMetaData is present)</li>
    **   <li>Route provider scope (if routeMetaData is present)</li>
    **   <li>Instance default scope (fallback)</li>
    ** </ol>
    ** </p>
    **
    ** @param instance The QInstance containing scoped authentication providers
    ** @param context The resolution context with request details
    ** @return The resolved authentication metadata (never null)
    ** @throws QException If no authentication provider is found
    *******************************************************************************/
   public static QAuthenticationMetaData resolve(QInstance instance, AuthResolutionContext context) throws QException
   {
      ///////////////////////////////////////////////////////////////////////////
      // Try API-specific scope first (most specific)                        //
      ///////////////////////////////////////////////////////////////////////////
      if(context.getApiMetaData() != null)
      {
         AuthScope.Api apiScope = AuthScope.api(context.getApiMetaData());
         Optional<QAuthenticationMetaData> apiAuth = instance.findAuthenticationProvider(apiScope);
         if(apiAuth.isPresent())
         {
            LOG.debug("Resolved API-specific authentication provider",
               logPair("apiName", context.getApiName()),
               logPair("scope", apiScope.toString()),
               logPair("requestPath", context.getRequestPath()));
            return apiAuth.get();
         }
         LOG.trace("No API-specific authentication provider found, falling back",
            logPair("apiName", context.getApiName()),
            logPair("requestPath", context.getRequestPath()));
      }

      ///////////////////////////////////////////////////////////////////////////
      // Try route provider-specific scope (second most specific)             //
      ///////////////////////////////////////////////////////////////////////////
      if(context.getRouteMetaData() != null)
      {
         AuthScope.RouteProvider routeScope = AuthScope.routeProvider(context.getRouteMetaData());
         Optional<QAuthenticationMetaData> routeAuth = instance.findAuthenticationProvider(routeScope);
         if(routeAuth.isPresent())
         {
            LOG.debug("Resolved route provider-specific authentication provider",
               logPair("routeProviderName", context.getRouteProviderName()),
               logPair("scope", routeScope.toString()),
               logPair("requestPath", context.getRequestPath()));
            return routeAuth.get();
         }
         LOG.trace("No route provider-specific authentication provider found, falling back",
            logPair("routeProviderName", context.getRouteProviderName()),
            logPair("requestPath", context.getRequestPath()));
      }

      ///////////////////////////////////////////////////////////////////////////
      // Fall back to instance default scope                                  //
      ///////////////////////////////////////////////////////////////////////////
      AuthScope.InstanceDefault defaultScope = AuthScope.instanceDefault();
      Optional<QAuthenticationMetaData> defaultAuth = instance.findAuthenticationProvider(defaultScope);
      if(defaultAuth.isPresent())
      {
         LOG.trace("Using instance default authentication provider",
            logPair("requestPath", context.getRequestPath()));
         return defaultAuth.get();
      }

      ///////////////////////////////////////////////////////////////////////////
      // No provider found - throw exception                                 //
      ///////////////////////////////////////////////////////////////////////////
      String errorMessage = "No authentication provider found for request";
      if(context.getRequestPath() != null)
      {
         errorMessage += " at path: " + context.getRequestPath();
      }
      LOG.error(errorMessage,
         logPair("apiName", context.getApiName()),
         logPair("routeProviderName", context.getRouteProviderName()));
      throw new QException(errorMessage);
   }
}

