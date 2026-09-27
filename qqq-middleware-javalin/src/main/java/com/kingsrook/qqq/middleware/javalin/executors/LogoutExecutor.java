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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.middleware.javalin.executors.io.LogoutInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.LogoutOutputInterface;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Executor for the logout endpoint.
 *******************************************************************************/
public class LogoutExecutor extends AbstractMiddlewareExecutor<LogoutInput, LogoutOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(LogoutExecutor.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(LogoutInput input, LogoutOutputInterface output) throws QException
   {
      QInstance                       qInstance                       = QContext.getQInstance();
      QAuthenticationModuleDispatcher qAuthenticationModuleDispatcher = new QAuthenticationModuleDispatcher();

      /////////////////////////////////////////////////////////////////////////
      // Call logout on all registered authentication modules.               //
      // This handles multi-auth scenarios (e.g., different OAuth2 providers //
      // for different APIs/route providers). Each module's logout is either //
      // a no-op or performs actual cleanup (delete session, clear cache).   //
      // Using a Set to avoid calling the same module multiple times.        //
      /////////////////////////////////////////////////////////////////////////
      Set<QAuthenticationMetaData> processedAuthMetaData = new HashSet<>();
      Map<AuthScope, QAuthenticationMetaData> scopedProviders = qInstance.getScopedAuthenticationProviders();

      for(QAuthenticationMetaData authMetaData : scopedProviders.values())
      {
         if(processedAuthMetaData.add(authMetaData))
         {
            try
            {
               QAuthenticationModuleInterface authModule = qAuthenticationModuleDispatcher.getQModule(authMetaData);
               authModule.logout(qInstance, input.getSessionUUID());
            }
            catch(Exception e)
            {
               LOG.warn("Error calling logout on auth module", e, logPair("authName", authMetaData.getName()));
            }
         }
      }
   }

}
