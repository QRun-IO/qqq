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

package com.kingsrook.qqq.middleware.javalin.routeproviders.handlers;


import io.javalin.http.Context;
import io.javalin.http.Handler;


/*******************************************************************************
 ** Interface for before handlers used by IsolatedSpaRouteProvider.
 ** Extends Javalin's Handler interface for compatibility.
 *******************************************************************************/
public interface RouteProviderBeforeHandlerInterface extends Handler
{
   /***************************************************************************
    ** Handle request before route processing
    **
    ** @param ctx Javalin context
    ***************************************************************************/
   @Override
   void handle(Context ctx) throws Exception;

}

