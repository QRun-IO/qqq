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

package com.kingsrook.qqq.middleware.javalin.routeproviders.authentication;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import io.javalin.http.Context;


/*******************************************************************************
 ** Interface for authenticating HTTP requests in Javalin route providers.
 **
 ** Implementations of this interface integrate with QQQ's authentication system
 ** to secure custom Javalin routes. The interface is used by route providers
 ** (ProcessBasedRouter, SimpleFileSystemDirectoryRouter, IsolatedSpaRouteProvider)
 ** to verify that requests are properly authenticated before serving content or
 ** executing processes.
 **
 ** Implementers should:
 ** - Check authentication state via QQQ's session management
 ** - Redirect to login pages for unauthenticated requests
 ** - Return true to allow the request to proceed
 ** - Return false if the request was redirected or should not proceed
 **
 ** @see SimpleRouteAuthenticator for a standard implementation
 *******************************************************************************/
public interface RouteAuthenticatorInterface
{

   /*******************************************************************************
    ** Authenticate an HTTP request before the route is served.
    **
    ** This method is called by route providers before serving content or executing
    ** processes. Implementations should verify the request's authentication state
    ** and handle unauthenticated requests appropriately (typically by redirecting
    ** to a login page).
    **
    ** @param context the Javalin HTTP context containing request information
    ** @return true if the request is authenticated and should proceed; false if
    **         the request was redirected or should not be processed further
    ** @throws QException if authentication processing fails
    *******************************************************************************/
   boolean authenticateRequest(Context context) throws QException;

}
