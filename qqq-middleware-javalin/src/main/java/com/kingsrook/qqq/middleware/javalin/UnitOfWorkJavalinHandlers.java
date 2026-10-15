/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import com.kingsrook.qqq.backend.core.instrumentation.StatementExecutionListeners;
import com.kingsrook.qqq.backend.core.instrumentation.UnitOfWork;
import com.kingsrook.qqq.backend.core.instrumentation.UnitOfWorkKind;
import com.kingsrook.qqq.backend.core.instrumentation.UnitsOfWork;
import io.javalin.http.Context;
import io.javalin.router.Endpoint;


/*******************************************************************************
 ** Javalin before/after handlers that make each HTTP request a core
 ** {@link UnitOfWork} (kind HTTP_REQUEST), so statement listeners can
 ** attribute database work to routes. The unit is named "METHOD /route/{template}"
 ** once Javalin has matched the route (concrete paths are never used, so ids
 ** and other path values do not enter the name). Both handlers do nothing
 ** while no statement listener is registered.
 *******************************************************************************/
public final class UnitOfWorkJavalinHandlers
{
   static final String SCOPE_ATTRIBUTE = "qqq.unitOfWorkScope";



   /*******************************************************************************
    ** Static handlers only.
    *******************************************************************************/
   private UnitOfWorkJavalinHandlers()
   {
   }



   /*******************************************************************************
    ** Before every request: drop anything a previous request left on this
    ** thread, and begin the request's unit.
    *******************************************************************************/
   public static void before(Context context)
   {
      if(!StatementExecutionListeners.isActive())
      {
         return;
      }
      UnitsOfWork.reset();
      context.attribute(SCOPE_ATTRIBUTE, UnitsOfWork.begin(UnitOfWorkKind.HTTP_REQUEST, context.method().name() + " (unmatched)"));
   }



   /*******************************************************************************
    ** After every request: name the unit by its matched route template and end it.
    *******************************************************************************/
   public static void after(Context context)
   {
      Object attribute = context.attribute(SCOPE_ATTRIBUTE);
      if(!(attribute instanceof UnitsOfWork.Scope scope))
      {
         return;
      }
      UnitOfWork unit = scope.getUnitOfWork();
      if(unit != null)
      {
         unit.setName(context.method().name() + " " + routeTemplate(context));
      }
      scope.close();
   }



   /*******************************************************************************
    ** The matched endpoint's path template, or "(unmatched)".
    *******************************************************************************/
   static String routeTemplate(Context context)
   {
      try
      {
         Endpoint endpoint = context.endpoints().lastHttpEndpoint();
         String   template = endpoint == null ? null : endpoint.path;
         if(template != null && !template.isBlank() && !"*".equals(template))
         {
            return (template);
         }
      }
      catch(Exception e)
      {
         /////////////////////////////////////////////////////////
         // no endpoint matched (404, or a static/SPA handler)  //
         /////////////////////////////////////////////////////////
      }
      return ("(unmatched)");
   }
}
