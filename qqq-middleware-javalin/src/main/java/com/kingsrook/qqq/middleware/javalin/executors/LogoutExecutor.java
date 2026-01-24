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
