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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.progressbar;


import java.io.Serializable;
import java.math.BigDecimal;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockValuesInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProgressBarValues implements BlockValuesInterface
{
   private String       heading;
   private BigDecimal   percent;
   private Serializable value;



   /*******************************************************************************
    ** Getter for heading
    *******************************************************************************/
   public String getHeading()
   {
      return (this.heading);
   }



   /*******************************************************************************
    ** Setter for heading
    *******************************************************************************/
   public void setHeading(String heading)
   {
      this.heading = heading;
   }



   /*******************************************************************************
    ** Fluent setter for heading
    *******************************************************************************/
   public ProgressBarValues withHeading(String heading)
   {
      this.heading = heading;
      return (this);
   }



   /*******************************************************************************
    ** Getter for percent
    *******************************************************************************/
   public BigDecimal getPercent()
   {
      return (this.percent);
   }



   /*******************************************************************************
    ** Setter for percent
    *******************************************************************************/
   public void setPercent(BigDecimal percent)
   {
      this.percent = percent;
   }



   /*******************************************************************************
    ** Fluent setter for percent
    *******************************************************************************/
   public ProgressBarValues withPercent(BigDecimal percent)
   {
      this.percent = percent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for value
    *******************************************************************************/
   public Serializable getValue()
   {
      return (this.value);
   }



   /*******************************************************************************
    ** Setter for value
    *******************************************************************************/
   public void setValue(Serializable value)
   {
      this.value = value;
   }



   /*******************************************************************************
    ** Fluent setter for value
    *******************************************************************************/
   public ProgressBarValues withValue(Serializable value)
   {
      this.value = value;
      return (this);
   }
}
