/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables.aggregate;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


/*******************************************************************************
 **
 *******************************************************************************/
public class AggregateResult
{
   private Map<Aggregate, Serializable> aggregateValues = new LinkedHashMap<>();
   private Map<GroupBy, Serializable>   groupByValues   = new LinkedHashMap<>();



   /*******************************************************************************
    ** Getter for aggregateValues
    **
    *******************************************************************************/
   public Map<Aggregate, Serializable> getAggregateValues()
   {
      return aggregateValues;
   }



   /*******************************************************************************
    ** Setter for aggregateValues
    **
    *******************************************************************************/
   public void setAggregateValues(Map<Aggregate, Serializable> aggregateValues)
   {
      this.aggregateValues = aggregateValues;
   }



   /*******************************************************************************
    ** Fluent setter for aggregateValues
    **
    *******************************************************************************/
   public AggregateResult withAggregateValues(Map<Aggregate, Serializable> aggregateValues)
   {
      this.aggregateValues = aggregateValues;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for groupByValues
    **
    *******************************************************************************/
   public AggregateResult withAggregateValue(Aggregate aggregate, Serializable value)
   {
      if(this.aggregateValues == null)
      {
         this.aggregateValues = new LinkedHashMap<>();
      }
      this.aggregateValues.put(aggregate, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Serializable getAggregateValue(Aggregate aggregate)
   {
      return (this.aggregateValues.get(aggregate));
   }



   /*******************************************************************************
    ** Getter for groupByValues
    **
    *******************************************************************************/
   public Map<GroupBy, Serializable> getGroupByValues()
   {
      return groupByValues;
   }



   /*******************************************************************************
    ** Setter for groupByValues
    **
    *******************************************************************************/
   public void setGroupByValues(Map<GroupBy, Serializable> groupByValues)
   {
      this.groupByValues = groupByValues;
   }



   /*******************************************************************************
    ** Fluent setter for groupByValues
    **
    *******************************************************************************/
   public AggregateResult withGroupByValues(Map<GroupBy, Serializable> groupByValues)
   {
      this.groupByValues = groupByValues;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for groupByValues
    **
    *******************************************************************************/
   public AggregateResult withGroupByValue(GroupBy groupBy, Serializable value)
   {
      if(this.groupByValues == null)
      {
         this.groupByValues = new LinkedHashMap<>();
      }
      this.groupByValues.put(groupBy, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Serializable getGroupByValue(GroupBy groupBy)
   {
      return (this.groupByValues.get(groupBy));
   }

}
