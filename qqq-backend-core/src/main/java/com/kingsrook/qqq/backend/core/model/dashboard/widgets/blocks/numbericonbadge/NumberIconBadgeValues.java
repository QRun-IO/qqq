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


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockValuesInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class NumberIconBadgeValues implements BlockValuesInterface
{
   private Serializable number;
   private String       iconName;



   /*******************************************************************************
    ** Getter for number
    *******************************************************************************/
   public Serializable getNumber()
   {
      return (this.number);
   }



   /*******************************************************************************
    ** Setter for number
    *******************************************************************************/
   public void setNumber(Serializable number)
   {
      this.number = number;
   }



   /*******************************************************************************
    ** Fluent setter for number
    *******************************************************************************/
   public NumberIconBadgeValues withNumber(Serializable number)
   {
      this.number = number;
      return (this);
   }



   /*******************************************************************************
    ** Getter for iconName
    *******************************************************************************/
   public String getIconName()
   {
      return (this.iconName);
   }



   /*******************************************************************************
    ** Setter for iconName
    *******************************************************************************/
   public void setIconName(String iconName)
   {
      this.iconName = iconName;
   }



   /*******************************************************************************
    ** Fluent setter for iconName
    *******************************************************************************/
   public NumberIconBadgeValues withIconName(String iconName)
   {
      this.iconName = iconName;
      return (this);
   }

}
