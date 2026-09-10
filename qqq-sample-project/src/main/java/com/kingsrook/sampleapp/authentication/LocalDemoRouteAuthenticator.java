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
