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


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.text.TextValues;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
 **
 *******************************************************************************/
@OpenAPIDescription("Values used for a TEXT type widget block")
public final class WidgetBlockTextValues implements WidgetBlockValues
{
   @OpenAPIExclude()
   private TextValues wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetBlockTextValues(TextValues textValues)
   {
      this.wrapped = textValues;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetBlockTextValues()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The text to display in the block")
   public String getText()
   {
      return (this.wrapped.getText());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("An optional icon to display before the text")
   public Icon getStartIcon()
   {
      return (this.wrapped.getStartIcon() == null ? null : new Icon(this.wrapped.getStartIcon()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("An optional icon to display after the text")
   public Icon getEndIcon()
   {
      return (this.wrapped.getEndIcon() == null ? null : new Icon(this.wrapped.getEndIcon()));
   }

}
