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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.image;


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.base.BaseStyles;


/*******************************************************************************
 **
 *******************************************************************************/
public class ImageStyles extends BaseStyles
{
   private String width;
   private String height;


   /*******************************************************************************
    ** Fluent setter for padding
    *******************************************************************************/
   @Override
   public ImageStyles withPadding(Directional<String> padding)
   {
      super.setPadding(padding);
      return (this);
   }


   /*******************************************************************************
    ** Getter for width
    *******************************************************************************/
   public String getWidth()
   {
      return (this.width);
   }



   /*******************************************************************************
    ** Setter for width
    *******************************************************************************/
   public void setWidth(String width)
   {
      this.width = width;
   }



   /*******************************************************************************
    ** Fluent setter for width
    *******************************************************************************/
   public ImageStyles withWidth(String width)
   {
      this.width = width;
      return (this);
   }



   /*******************************************************************************
    ** Getter for height
    *******************************************************************************/
   public String getHeight()
   {
      return (this.height);
   }



   /*******************************************************************************
    ** Setter for height
    *******************************************************************************/
   public void setHeight(String height)
   {
      this.height = height;
   }



   /*******************************************************************************
    ** Fluent setter for height
    *******************************************************************************/
   public ImageStyles withHeight(String height)
   {
      this.height = height;
      return (this);
   }

}
