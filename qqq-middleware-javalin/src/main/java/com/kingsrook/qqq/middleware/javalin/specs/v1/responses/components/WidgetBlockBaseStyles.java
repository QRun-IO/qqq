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


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.base.BaseStyles;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
 **
 *******************************************************************************/
public final class WidgetBlockBaseStyles implements WidgetBlockStyles
{
   @OpenAPIExclude()
   private BaseStyles wrapped;



   /***************************************************************************
    **
    ***************************************************************************/
   public WidgetBlockBaseStyles(BaseStyles baseStyles)
   {
      this.wrapped = baseStyles;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetBlockBaseStyles()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Optional padding to apply to the block")
   public BaseStyles.Directional<String> getPadding()
   {
      return (this.wrapped.getPadding());
   }


   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("A background color to use for the block")
   public String getBackgroundColor()
   {
      return (this.wrapped.getBackgroundColor());
   }

}
