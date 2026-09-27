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
