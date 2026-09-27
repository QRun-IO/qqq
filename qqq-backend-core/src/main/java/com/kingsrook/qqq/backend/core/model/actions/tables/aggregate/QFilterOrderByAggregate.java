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


import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;


/*******************************************************************************
 ** Bean representing an element of a query order-by clause - ordering by an
 ** aggregate field.
 **
 *******************************************************************************/
public class QFilterOrderByAggregate extends QFilterOrderBy implements Cloneable
{
   private Aggregate aggregate;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QFilterOrderByAggregate clone()
   {
      return (QFilterOrderByAggregate) super.clone();
   }



   /*******************************************************************************
    ** Default no-arg constructor
    *******************************************************************************/
   public QFilterOrderByAggregate()
   {

   }



   /*******************************************************************************
    ** Constructor that sets field name, but leaves default for isAscending (true)
    *******************************************************************************/
   public QFilterOrderByAggregate(Aggregate aggregate)
   {
      this.aggregate = aggregate;
   }



   /*******************************************************************************
    ** Constructor that takes field name and isAscending.
    *******************************************************************************/
   public QFilterOrderByAggregate(Aggregate aggregate, boolean isAscending)
   {
      this.aggregate = aggregate;
      setIsAscending(isAscending);
   }



   /*******************************************************************************
    ** Getter for aggregate
    **
    *******************************************************************************/
   public Aggregate getAggregate()
   {
      return aggregate;
   }



   /*******************************************************************************
    ** Setter for aggregate
    **
    *******************************************************************************/
   public void setAggregate(Aggregate aggregate)
   {
      this.aggregate = aggregate;
   }



   /*******************************************************************************
    ** Fluent setter for aggregate
    **
    *******************************************************************************/
   public QFilterOrderByAggregate withAggregate(Aggregate aggregate)
   {
      this.aggregate = aggregate;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return (aggregate + " " + (getIsAscending() ? "ASC" : "DESC"));
   }
}
