/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
import com.kingsrook.qqq.backend.core.utils.StringUtils;
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
      authContext.put(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, input.getAccessToken());
      authContext.put(Auth0AuthenticationModule.DO_STORE_USER_SESSION_KEY, "true");

      ////////////////////////////////////////////////////////////////////////////
      // username + password (Authorization: Basic), e.g. for TABLE_BASED auth //
      ////////////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(input.getBasicAuthString()))
      {
         authContext.put(Auth0AuthenticationModule.BASIC_AUTH_KEY, input.getBasicAuthString());
      }

      //////////////////////////////////////////////////////////////////////////////////
      // the values the authentication modules read besides the access token: an OAuth2 //
      // code exchange (code, codeVerifier, redirectUri) or resuming a session.        //
      // Only these named values - not arbitrary body keys - reach the module.        //
      //////////////////////////////////////////////////////////////////////////////////
      putIfPresent(authContext, "code", input.getCode());
      putIfPresent(authContext, "codeVerifier", input.getCodeVerifier());
      putIfPresent(authContext, "redirectUri", input.getRedirectUri());
      putIfPresent(authContext, "sessionUUID", input.getSessionUUID());
      putIfPresent(authContext, "uuid", input.getSessionUUID());

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



   /***************************************************************************
    **
    ***************************************************************************/
   private static void putIfPresent(Map<String, String> authContext, String key, String value)
   {
      if(value != null)
      {
         authContext.put(key, value);
      }
   }

}
