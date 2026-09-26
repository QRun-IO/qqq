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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.button;


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockStylesInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class ButtonStyles implements BlockStylesInterface
{
   private String color;
   private String format;



   /***************************************************************************
    **
    ***************************************************************************/
   public enum StandardColor
   {
      SUCCESS,
      WARNING,
      ERROR,
      INFO,
      MUTED
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public enum StandardFormat
   {
      OUTLINED,
      FILLED,
      TEXT
   }



   /*******************************************************************************
    ** Getter for color
    *******************************************************************************/
   public String getColor()
   {
      return (this.color);
   }



   /*******************************************************************************
    ** Setter for color
    *******************************************************************************/
   public void setColor(String color)
   {
      this.color = color;
   }



   /*******************************************************************************
    ** Fluent setter for color
    *******************************************************************************/
   public ButtonStyles withColor(String color)
   {
      this.color = color;
      return (this);
   }



   /*******************************************************************************
    ** Getter for format
    *******************************************************************************/
   public String getFormat()
   {
      return (this.format);
   }



   /*******************************************************************************
    ** Setter for format
    *******************************************************************************/
   public void setFormat(String format)
   {
      this.format = format;
   }



   /*******************************************************************************
    ** Fluent setter for format
    *******************************************************************************/
   public ButtonStyles withFormat(String format)
   {
      this.format = format;
      return (this);
   }

   /*******************************************************************************
    ** Setter for format
    *******************************************************************************/
   public void setFormat(StandardFormat format)
   {
      this.format = (format == null ? null : format.name().toLowerCase());
   }



   /*******************************************************************************
    ** Fluent setter for format
    *******************************************************************************/
   public ButtonStyles withFormat(StandardFormat format)
   {
      setFormat(format);
      return (this);
   }

}
