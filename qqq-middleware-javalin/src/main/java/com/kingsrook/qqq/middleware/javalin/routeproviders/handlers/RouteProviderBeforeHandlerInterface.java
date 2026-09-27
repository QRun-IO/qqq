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

