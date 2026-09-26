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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.icon;


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.base.BaseStyles;


/*******************************************************************************
 **
 *******************************************************************************/
public class IconStyles extends BaseStyles
{
   private String fontSize;
   private String color;



   /*******************************************************************************
    ** Fluent setter for padding
    *******************************************************************************/
   @Override
   public IconStyles withPadding(Directional<String> padding)
   {
      super.setPadding(padding);
      return (this);
   }



   /*******************************************************************************
    * Getter for fontSize
    * @see #withFontSize(String)
    *******************************************************************************/
   public String getFontSize()
   {
      return (this.fontSize);
   }



   /*******************************************************************************
    * Setter for fontSize
    * @see #withFontSize(String)
    *******************************************************************************/
   public void setFontSize(String fontSize)
   {
      this.fontSize = fontSize;
   }



   /*******************************************************************************
    * Fluent setter for fontSize
    *
    * @param fontSize
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public IconStyles withFontSize(String fontSize)
   {
      this.fontSize = fontSize;
      return (this);
   }



   /*******************************************************************************
    * Getter for color
    * @see #withColor(String)
    *******************************************************************************/
   public String getColor()
   {
      return (this.color);
   }



   /*******************************************************************************
    * Setter for color
    * @see #withColor(String)
    *******************************************************************************/
   public void setColor(String color)
   {
      this.color = color;
   }



   /*******************************************************************************
    * Fluent setter for color
    *
    * @param color
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public IconStyles withColor(String color)
   {
      this.color = color;
      return (this);
   }

}
