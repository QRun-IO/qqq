/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions;


import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthResolutionContext;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthenticationResolver;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.utils.PrefixedDefaultThreadFactory;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility methods to be shared by all of the various Actions (e.g., InsertAction)
 *******************************************************************************/
public class ActionHelper
{
   private static final QLogger LOG = QLogger.getLogger(ActionHelper.class);

   /////////////////////////////////////////////////////////////////////////////
   // we would probably use Executors.newCachedThreadPool() - but - it has no //
   // maxPoolSize...  we think some limit is good, so that at a large number  //
   // of attempted concurrent jobs we'll have new jobs block, rather than     //
   // exhausting all server resources and locking up "everything"             //
   // also, it seems like keeping a handful of core-threads around is very    //
   // little actual waste, and better than ever wasting time starting a new   //
   // one, which we know we'll often be doing.                                //
   /////////////////////////////////////////////////////////////////////////////
   private static Integer         CORE_THREADS    = 8;
   private static Integer         MAX_THREADS     = 500;
   private static ExecutorService executorService = new ThreadPoolExecutor(CORE_THREADS, MAX_THREADS, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new PrefixedDefaultThreadFactory(ActionHelper.class));



   /*******************************************************************************
    ** Validate the session using the appropriate authentication provider.
    **
    ** <p>Resolves the authentication provider based on the session's context
    ** (e.g., API name stored in session). If the session was created with a
    ** scoped authentication provider (e.g., API-specific or route provider-specific),
    ** it will be validated using that same provider. Otherwise, falls back to
    ** instance default authentication.</p>
    **
    ** <p>This ensures that sessions created with fully anonymous authentication
    ** (or any other scoped provider) are validated using the same provider,
    ** maintaining proper server-side session management regardless of auth method.</p>
    **
    ** @param request The action input (may be null)
    ** @throws QException If validation fails
    *******************************************************************************/
   public static void validateSession(AbstractActionInput request) throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QSession  qSession  = QContext.getQSession();

      if(qInstance == null)
      {
         throw (new QException("QInstance was not set in QContext."));
      }

      if(qSession == null)
      {
         throw (new QException("QSession was not set in QContext."));
      }

      ///////////////////////////////////////////////////////////////////////////
      // Resolve the authentication provider based on session context        //
      // This ensures sessions are validated with the same provider that     //
      // created them (e.g., scoped API auth vs instance default)             //
      ///////////////////////////////////////////////////////////////////////////
      QAuthenticationMetaData authMetaData = resolveAuthenticationForSession(qInstance, qSession);

      QAuthenticationModuleDispatcher qAuthenticationModuleDispatcher = new QAuthenticationModuleDispatcher();
      QAuthenticationModuleInterface  authenticationModule            = qAuthenticationModuleDispatcher.getQModule(authMetaData);
      if(!authenticationModule.isSessionValid(qInstance, qSession))
      {
         throw new QAuthenticationException("Invalid session in request");
      }
   }


   /*******************************************************************************
    ** Resolve the authentication provider for the given session.
    **
    ** <p>Checks if the session has an API name stored. If so, resolves the
    ** authentication provider using AuthenticationResolver. Otherwise, falls
    ** back to instance default.</p>
    **
    ** @param qInstance The QInstance
    ** @param qSession The QSession
    ** @return The resolved authentication metadata (never null)
    *******************************************************************************/
   private static QAuthenticationMetaData resolveAuthenticationForSession(QInstance qInstance, QSession qSession)
   {
      ///////////////////////////////////////////////////////////////////////////
      // Check if session has API name - if so, resolve scoped auth          //
      ///////////////////////////////////////////////////////////////////////////
      String apiName = qSession.getValue("apiName");
      if(apiName != null && !apiName.isEmpty())
      {
         try
         {
            // Use reflection to get API metadata (avoiding module dependency)
            QSupplementalInstanceMetaData apiContainer =
               qInstance.getSupplementalMetaData("api");
            if(apiContainer != null)
            {
               Method getApisMethod = apiContainer.getClass().getMethod("getApis");
               Object apisMap = getApisMethod.invoke(apiContainer);
               if(apisMap instanceof Map)
               {
                  Object apiMetaData = ((Map<?, ?>)apisMap).get(apiName);
                  if(apiMetaData != null)
                  {
                     // Resolve authentication provider for this API
                     AuthResolutionContext resolutionContext = new AuthResolutionContext()
                        .withApiMetaData(apiMetaData)
                        .withApiName(apiName);
                     QAuthenticationMetaData resolvedAuth = AuthenticationResolver.resolve(qInstance, resolutionContext);
                     LOG.trace("Resolved scoped authentication for session validation",
                        logPair("apiName", apiName),
                        logPair("authType", resolvedAuth.getType()));
                     return resolvedAuth;
                  }
               }
            }
         }
         catch(Exception e)
         {
            // If resolution fails, fall back to instance default
            LOG.debug("Failed to resolve scoped authentication for session, falling back to instance default",
               logPair("apiName", apiName),
               e);
         }
      }

      ///////////////////////////////////////////////////////////////////////////
      // Fall back to instance default authentication                         //
      ///////////////////////////////////////////////////////////////////////////
      return qInstance.getAuthentication();
   }



   /*******************************************************************************
    ** access an executor service for sharing among the executeAsync methods of all
    ** actions.
    *******************************************************************************/
   static ExecutorService getExecutorService()
   {
      return (executorService);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void editFirstValue(List<Serializable> values, Function<String, String> editFunction)
   {
      if(values.size() > 0)
      {
         values.set(0, editFunction.apply(String.valueOf(values.get(0))));
      }
   }
}
