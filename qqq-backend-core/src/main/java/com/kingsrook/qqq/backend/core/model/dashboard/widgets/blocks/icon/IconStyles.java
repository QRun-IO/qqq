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
