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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;


/*******************************************************************************
 **
 *******************************************************************************/
public class QueryMiddlewareInput extends AbstractMiddlewareInput
{
   private String          table;
   private QQueryFilter    filter;
   private List<QueryJoin> queryJoins;



   /*******************************************************************************
    ** Getter for table
    *******************************************************************************/
   public String getTable()
   {
      return (this.table);
   }



   /*******************************************************************************
    ** Setter for table
    *******************************************************************************/
   public void setTable(String table)
   {
      this.table = table;
   }



   /*******************************************************************************
    ** Fluent setter for table
    *******************************************************************************/
   public QueryMiddlewareInput withTable(String table)
   {
      this.table = table;
      return (this);
   }



   /*******************************************************************************
    ** Getter for filter
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return (this.filter);
   }



   /*******************************************************************************
    ** Setter for filter
    *******************************************************************************/
   public void setFilter(QQueryFilter filter)
   {
      this.filter = filter;
   }



   /*******************************************************************************
    ** Fluent setter for filter
    *******************************************************************************/
   public QueryMiddlewareInput withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryJoins
    *******************************************************************************/
   public List<QueryJoin> getQueryJoins()
   {
      return (this.queryJoins);
   }



   /*******************************************************************************
    ** Setter for queryJoins
    *******************************************************************************/
   public void setQueryJoins(List<QueryJoin> queryJoins)
   {
      this.queryJoins = queryJoins;
   }



   /*******************************************************************************
    ** Fluent setter for queryJoins
    *******************************************************************************/
   public QueryMiddlewareInput withQueryJoins(List<QueryJoin> queryJoins)
   {
      this.queryJoins = queryJoins;
      return (this);
   }

}
