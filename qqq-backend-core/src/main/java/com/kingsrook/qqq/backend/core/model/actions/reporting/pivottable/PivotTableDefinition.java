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
import java.util.ArrayList;
import java.util.List;


/*******************************************************************************
 ** Full definition of a pivot table - its rows, columns, and values.
 *******************************************************************************/
public class PivotTableDefinition implements Cloneable, Serializable
{
   private List<PivotTableGroupBy> rows;
   private List<PivotTableGroupBy> columns;
   private List<PivotTableValue>   values;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected PivotTableDefinition clone() throws CloneNotSupportedException
   {
      PivotTableDefinition clone = (PivotTableDefinition) super.clone();

      if(rows != null)
      {
         clone.rows = new ArrayList<>();
         for(PivotTableGroupBy row : rows)
         {
            clone.rows.add(row.clone());
         }
      }

      if(columns != null)
      {
         clone.columns = new ArrayList<>();
         for(PivotTableGroupBy column : columns)
         {
            clone.columns.add(column.clone());
         }
      }

      if(values != null)
      {
         clone.values = new ArrayList<>();
         for(PivotTableValue value : values)
         {
            clone.values.add(value.clone());
         }
      }

      return (clone);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public PivotTableDefinition withRow(PivotTableGroupBy row)
   {
      if(this.rows == null)
      {
         this.rows = new ArrayList<>();
      }
      this.rows.add(row);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public PivotTableDefinition withColumn(PivotTableGroupBy column)
   {
      if(this.columns == null)
      {
         this.columns = new ArrayList<>();
      }
      this.columns.add(column);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public PivotTableDefinition withValue(PivotTableValue value)
   {
      if(this.values == null)
      {
         this.values = new ArrayList<>();
      }
      this.values.add(value);
      return (this);
   }



   /*******************************************************************************
    ** Getter for rows
    *******************************************************************************/
   public List<PivotTableGroupBy> getRows()
   {
      return (this.rows);
   }



   /*******************************************************************************
    ** Setter for rows
    *******************************************************************************/
   public void setRows(List<PivotTableGroupBy> rows)
   {
      this.rows = rows;
   }



   /*******************************************************************************
    ** Fluent setter for rows
    *******************************************************************************/
   public PivotTableDefinition withRows(List<PivotTableGroupBy> rows)
   {
      this.rows = rows;
      return (this);
   }



   /*******************************************************************************
    ** Getter for columns
    *******************************************************************************/
   public List<PivotTableGroupBy> getColumns()
   {
      return (this.columns);
   }



   /*******************************************************************************
    ** Setter for columns
    *******************************************************************************/
   public void setColumns(List<PivotTableGroupBy> columns)
   {
      this.columns = columns;
   }



   /*******************************************************************************
    ** Fluent setter for columns
    *******************************************************************************/
   public PivotTableDefinition withColumns(List<PivotTableGroupBy> columns)
   {
      this.columns = columns;
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    *******************************************************************************/
   public List<PivotTableValue> getValues()
   {
      return (this.values);
   }



   /*******************************************************************************
    ** Setter for values
    *******************************************************************************/
   public void setValues(List<PivotTableValue> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Fluent setter for values
    *******************************************************************************/
   public PivotTableDefinition withValues(List<PivotTableValue> values)
   {
      this.values = values;
      return (this);
   }

}
