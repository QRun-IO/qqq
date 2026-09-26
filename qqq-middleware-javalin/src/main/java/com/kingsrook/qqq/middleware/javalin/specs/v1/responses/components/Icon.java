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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/***************************************************************************
 **
 ***************************************************************************/
public class Icon implements ToSchema
{
   @OpenAPIExclude()
   private QIcon wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Icon(QIcon wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Icon()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("A material UI icon name.")
   public String getName()
   {
      return (this.wrapped.getName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("A color code to use for displaying the icon")
   public String getColor()
   {
      return (this.wrapped.getColor());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("A path to an image file that can be requested from the server, to serve as the icon image instead of a material UI icon.")
   public String getPath()
   {
      return (this.wrapped.getPath());
   }
}
