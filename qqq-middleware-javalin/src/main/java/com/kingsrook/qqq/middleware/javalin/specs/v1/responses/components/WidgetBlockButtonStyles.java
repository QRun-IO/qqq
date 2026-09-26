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


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.button.ButtonStyles;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
 **
 *******************************************************************************/
public final class WidgetBlockButtonStyles implements WidgetBlockStyles
{

   @OpenAPIExclude()
   private ButtonStyles wrapped;



   /***************************************************************************
    **
    ***************************************************************************/
   public WidgetBlockButtonStyles(ButtonStyles buttonStyles)
   {
      this.wrapped = buttonStyles;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetBlockButtonStyles()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("A Color to use for the button.  May be specified as a StandardColor (one of: "
                       + "SUCCESS, WARNING, ERROR, INFO, MUTED) or an RGB code.")
   public String getColor()
   {
      return (this.wrapped.getColor());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("An optional indicator of the screen format preferred by the application to be used for this block, "
                       + "such as OUTLINED, FILLED, or TEXT.  Different frontends may support different formats, and implement them differently.")
   public String getFormat()
   {
      return (this.wrapped.getFormat());
   }


}
