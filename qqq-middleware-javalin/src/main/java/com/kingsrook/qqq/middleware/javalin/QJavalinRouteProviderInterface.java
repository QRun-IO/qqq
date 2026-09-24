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

package com.kingsrook.qqq.middleware.javalin;


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.JavalinConfig;


/*******************************************************************************
 ** Interface for classes that can provide a list of endpoints to a javalin
 ** server.
 *******************************************************************************/
public interface QJavalinRouteProviderInterface
{

   /***************************************************************************
    ** For initial setup when server boots, set the qInstance - but also,
    ** e.g., for development, to do a hot-swap.
    ***************************************************************************/
   void setQInstance(QInstance qInstance);

   /***************************************************************************
    **
    ***************************************************************************/
   default EndpointGroup getJavalinEndpointGroup()
   {
      /////////////////////////////
      // no endpoints at default //
      /////////////////////////////
      return (null);
   }


   /***************************************************************************
    ** when the javalin service is being configured as part of its boot up,
    ** accept the javalinConfig object, to perform whatever setup you need,
    ** such as setting up routes.
    ***************************************************************************/
   default void acceptJavalinConfig(JavalinConfig config)
   {
      /////////////////////
      // noop at default //
      /////////////////////
   }

   /***************************************************************************
    ** Observe the created service before it starts. Routes, before/after handlers
    ** and error handlers must be registered in acceptJavalinConfig instead.
    ***************************************************************************/
   default void acceptJavalinService(Javalin service)
   {
      /////////////////////
      // noop at default //
      /////////////////////
   }

}
