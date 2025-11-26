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
