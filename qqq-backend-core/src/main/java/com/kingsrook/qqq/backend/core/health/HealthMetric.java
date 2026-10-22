/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.health;




/*******************************************************************************
 ** HealthMetric
 *******************************************************************************/
public class HealthMetric
{
   private String name;
   private Number value;
   private String unit;



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public HealthMetric withName(String name)
   {
      setName(name);
      return (this);
   }



   /*******************************************************************************
    ** Getter for value
    *******************************************************************************/
   public Number getValue()
   {
      return (value);
   }



   /*******************************************************************************
    ** Setter for value
    *******************************************************************************/
   public void setValue(Number value)
   {
      this.value = value;
   }



   /*******************************************************************************
    ** Fluent setter for value
    *******************************************************************************/
   public HealthMetric withValue(Number value)
   {
      setValue(value);
      return (this);
   }



   /*******************************************************************************
    ** Getter for unit
    *******************************************************************************/
   public String getUnit()
   {
      return (unit);
   }



   /*******************************************************************************
    ** Setter for unit
    *******************************************************************************/
   public void setUnit(String unit)
   {
      this.unit = unit;
   }



   /*******************************************************************************
    ** Fluent setter for unit
    *******************************************************************************/
   public HealthMetric withUnit(String unit)
   {
      setUnit(unit);
      return (this);
   }
}
