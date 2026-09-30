/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp.authentication;


import com.kingsrook.qqq.middleware.javalin.routeproviders.authentication.RouteAuthenticatorInterface;
import io.javalin.http.Context;
import io.javalin.security.BasicAuthCredentials;


/*******************************************************************************
 ** Local demonstration only: sample / sample-only are public synthetic credentials.
 ** This exercises route rejection without an external identity service. Replace
 ** it with the application's authentication provider before serving real data.
 *******************************************************************************/
public class LocalDemoRouteAuthenticator implements RouteAuthenticatorInterface
{
   /*******************************************************************************
    ** Missing, incorrect and malformed credentials all reject the request.
    *******************************************************************************/
   @Override
   public boolean authenticateRequest(Context context)
   {
      BasicAuthCredentials credentials;
      try
      {
         credentials = context.basicAuthCredentials();
      }
      catch(IllegalArgumentException | IndexOutOfBoundsException e)
      {
         credentials = null;
      }

      if(credentials != null && "sample".equals(credentials.getUsername()) && "sample-only".equals(credentials.getPassword()))
      {
         return true;
      }

      context.header("WWW-Authenticate", "Basic realm=\"QQQ local demonstration\"");
      context.status(401).result("Local demonstration authentication required. See the public sample page.");
      return false;
   }
}
