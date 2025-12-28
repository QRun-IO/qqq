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

package com.kingsrook.qqq.middleware.javalin.routeproviders.contexthandlers;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import io.javalin.http.Context;


/*******************************************************************************
 ** Interface for bidirectional mapping between Javalin HTTP context and QQQ process execution.
 **
 ** This interface defines how HTTP requests are translated into process inputs
 ** and how process outputs are translated back into HTTP responses for
 ** ProcessBasedRouter routes.
 **
 ** Implementations handle:
 ** - Request mapping: Extract data from HTTP request (params, headers, body)
 **   and populate RunProcessInput for the process to consume
 ** - Response mapping: Extract data from RunProcessOutput and construct
 **   HTTP response (status, headers, body)
 **
 ** The default implementation (DefaultRouteProviderContextHandler) handles
 ** common cases like form parameters, JSON bodies, and string/byte responses.
 ** Custom implementations can support specialized content types, file uploads,
 ** streaming responses, or custom authentication flows.
 **
 ** @see DefaultRouteProviderContextHandler for the standard implementation
 *******************************************************************************/
public interface RouteProviderContextHandlerInterface
{

   /*******************************************************************************
    ** Map HTTP request data into process input.
    **
    ** Extract relevant data from the Javalin context (path, method, parameters,
    ** headers, body) and populate the RunProcessInput so the process can access it.
    **
    ** @param context the Javalin HTTP context
    ** @param runProcessInput the process input to populate with request data
    *******************************************************************************/
   void handleRequest(Context context, RunProcessInput runProcessInput);

   /*******************************************************************************
    ** Map process output into HTTP response.
    **
    ** Extract response data from the RunProcessOutput (status code, headers, body)
    ** and write it to the Javalin context to construct the HTTP response.
    **
    ** @param context the Javalin HTTP context
    ** @param runProcessOutput the process output containing response data
    ** @return true if the response was handled; false otherwise
    ** @throws QException if response processing fails
    *******************************************************************************/
   boolean handleResponse(Context context, RunProcessOutput runProcessOutput) throws QException;

}
