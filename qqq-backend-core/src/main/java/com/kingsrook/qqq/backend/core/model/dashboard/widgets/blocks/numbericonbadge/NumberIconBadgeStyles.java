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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.numbericonbadge;


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockStylesInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class NumberIconBadgeStyles implements BlockStylesInterface
{
   private String color;



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
   public NumberIconBadgeStyles withColor(String color)
   {
      this.color = color;
      return (this);
   }

}
