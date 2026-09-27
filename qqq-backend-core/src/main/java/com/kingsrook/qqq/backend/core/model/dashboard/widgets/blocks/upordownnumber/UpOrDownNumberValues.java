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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.upordownnumber;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockValuesInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class UpOrDownNumberValues implements BlockValuesInterface
{
   private boolean      isUp   = false;
   private boolean      isGood = false;
   private Serializable number;
   private String       context;



   /*******************************************************************************
    ** Getter for isUp
    *******************************************************************************/
   public boolean getIsUp()
   {
      return (this.isUp);
   }



   /*******************************************************************************
    ** Setter for isUp
    *******************************************************************************/
   public void setIsUp(boolean isUp)
   {
      this.isUp = isUp;
   }



   /*******************************************************************************
    ** Fluent setter for isUp
    *******************************************************************************/
   public UpOrDownNumberValues withIsUp(boolean isUp)
   {
      this.isUp = isUp;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isGood
    *******************************************************************************/
   public boolean getIsGood()
   {
      return (this.isGood);
   }



   /*******************************************************************************
    ** Setter for isGood
    *******************************************************************************/
   public void setIsGood(boolean isGood)
   {
      this.isGood = isGood;
   }



   /*******************************************************************************
    ** Fluent setter for isGood
    *******************************************************************************/
   public UpOrDownNumberValues withIsGood(boolean isGood)
   {
      this.isGood = isGood;
      return (this);
   }



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
   public UpOrDownNumberValues withNumber(Serializable number)
   {
      this.number = number;
      return (this);
   }



   /*******************************************************************************
    ** Getter for context
    *******************************************************************************/
   public String getContext()
   {
      return (this.context);
   }



   /*******************************************************************************
    ** Setter for context
    *******************************************************************************/
   public void setContext(String context)
   {
      this.context = context;
   }



   /*******************************************************************************
    ** Fluent setter for context
    *******************************************************************************/
   public UpOrDownNumberValues withContext(String context)
   {
      this.context = context;
      return (this);
   }

}
