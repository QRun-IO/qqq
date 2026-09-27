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
 ** Bean representing an element of a query order-by clause - ordering by a
 ** group by
 **
 *******************************************************************************/
public class QFilterOrderByGroupBy extends QFilterOrderBy implements Cloneable
{
   private GroupBy groupBy;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QFilterOrderByGroupBy clone()
   {
      return (QFilterOrderByGroupBy) super.clone();
   }



   /*******************************************************************************
    ** Default no-arg constructor
    *******************************************************************************/
   public QFilterOrderByGroupBy()
   {

   }



   /*******************************************************************************
    ** Constructor that sets groupBy, but leaves default for isAscending (true)
    *******************************************************************************/
   public QFilterOrderByGroupBy(GroupBy groupBy)
   {
      this.groupBy = groupBy;
   }



   /*******************************************************************************
    ** Constructor that takes groupBy and isAscending.
    *******************************************************************************/
   public QFilterOrderByGroupBy(GroupBy groupBy, boolean isAscending)
   {
      this.groupBy = groupBy;
      setIsAscending(isAscending);
   }



   /*******************************************************************************
    ** Getter for groupBy
    **
    *******************************************************************************/
   public GroupBy getGroupBy()
   {
      return groupBy;
   }



   /*******************************************************************************
    ** Setter for groupBy
    **
    *******************************************************************************/
   public void setGroupBy(GroupBy groupBy)
   {
      this.groupBy = groupBy;
   }



   /*******************************************************************************
    ** Fluent setter for groupBy
    **
    *******************************************************************************/
   public QFilterOrderByGroupBy withGroupBy(GroupBy groupBy)
   {
      this.groupBy = groupBy;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return (groupBy + " " + (getIsAscending() ? "ASC" : "DESC"));
   }
}
