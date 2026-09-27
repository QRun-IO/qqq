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
 ** Either a row or column grouping in a pivot table.  e.g., a field plus
 ** sorting details, plus showTotals boolean.
 *******************************************************************************/
public class PivotTableGroupBy implements Cloneable, Serializable
{
   private String            fieldName;
   private PivotTableOrderBy orderBy;
   private boolean           showTotals;



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
   public PivotTableGroupBy withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for orderBy
    *******************************************************************************/
   public PivotTableOrderBy getOrderBy()
   {
      return (this.orderBy);
   }



   /*******************************************************************************
    ** Setter for orderBy
    *******************************************************************************/
   public void setOrderBy(PivotTableOrderBy orderBy)
   {
      this.orderBy = orderBy;
   }



   /*******************************************************************************
    ** Fluent setter for orderBy
    *******************************************************************************/
   public PivotTableGroupBy withOrderBy(PivotTableOrderBy orderBy)
   {
      this.orderBy = orderBy;
      return (this);
   }



   /*******************************************************************************
    ** Getter for showTotals
    *******************************************************************************/
   public boolean getShowTotals()
   {
      return (this.showTotals);
   }



   /*******************************************************************************
    ** Setter for showTotals
    *******************************************************************************/
   public void setShowTotals(boolean showTotals)
   {
      this.showTotals = showTotals;
   }



   /*******************************************************************************
    ** Fluent setter for showTotals
    *******************************************************************************/
   public PivotTableGroupBy withShowTotals(boolean showTotals)
   {
      this.showTotals = showTotals;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public PivotTableGroupBy clone() throws CloneNotSupportedException
   {
      PivotTableGroupBy clone = (PivotTableGroupBy) super.clone();
      return clone;
   }

}
