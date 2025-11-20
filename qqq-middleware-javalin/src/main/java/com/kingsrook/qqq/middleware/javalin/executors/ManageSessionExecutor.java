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


import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.ManageSessionInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.ManageSessionOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class ManageSessionExecutor extends AbstractMiddlewareExecutor<ManageSessionInput, ManageSessionOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(ManageSessionInput input, ManageSessionOutputInterface output) throws QException
   {
      QAuthenticationModuleDispatcher qAuthenticationModuleDispatcher = new QAuthenticationModuleDispatcher();
      QAuthenticationModuleInterface  authenticationModule            = qAuthenticationModuleDispatcher.getQModule(QContext.getQInstance().getAuthentication());

      Map<String, String> authContext = new HashMap<>();
      authContext.put(Auth0AuthenticationModule.DO_STORE_USER_SESSION_KEY, "true");

      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // Loop over all fields from request body, similar to legacy endpoint - this allows any auth module    //
      // to receive whatever fields it needs (accessToken, code, codeVerifier, redirectUri, etc.)          //
      // without needing explicit extraction here                                                             //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(input.getAllFields() != null)
      {
         for(Map.Entry<String, String> entry : input.getAllFields().entrySet())
         {
            authContext.put(ValueUtils.getValueAsString(entry.getKey()), ValueUtils.getValueAsString(entry.getValue()));
         }
      }

      /////////////////////////////////
      // (try to) create the session //
      /////////////////////////////////
      QSession session = authenticationModule.createSession(QContext.getQInstance(), authContext);

      //////////////////
      // build output //
      //////////////////
      output.setUuid(session.getUuid());

      if(session.getValuesForFrontend() != null)
      {
         LinkedHashMap<String, Serializable> valuesForFrontend = new LinkedHashMap<>(session.getValuesForFrontend());
         output.setValues(valuesForFrontend);
      }
   }

}
