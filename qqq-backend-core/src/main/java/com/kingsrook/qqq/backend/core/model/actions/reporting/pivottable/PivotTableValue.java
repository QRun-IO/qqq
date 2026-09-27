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

package com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable;


import java.io.Serializable;


/*******************************************************************************
 ** a value (e.g., field name + function) used in a pivot table
 *******************************************************************************/
public class PivotTableValue implements Cloneable, Serializable
{
   private String             fieldName;
   private PivotTableFunction function;



   /*******************************************************************************
    ** Getter for fieldName
    *******************************************************************************/
   public String getFieldName()
   {
      return (this.fieldName);
   }



   /*******************************************************************************
    ** Setter for fieldName
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    *******************************************************************************/
   public PivotTableValue withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for function
    *******************************************************************************/
   public PivotTableFunction getFunction()
   {
      return (this.function);
   }



   /*******************************************************************************
    ** Setter for function
    *******************************************************************************/
   public void setFunction(PivotTableFunction function)
   {
      this.function = function;
   }



   /*******************************************************************************
    ** Fluent setter for function
    *******************************************************************************/
   public PivotTableValue withFunction(PivotTableFunction function)
   {
      this.function = function;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public PivotTableValue clone() throws CloneNotSupportedException
   {
      PivotTableValue clone = (PivotTableValue) super.clone();
      return clone;
   }

}
