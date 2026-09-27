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


import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;


/*******************************************************************************
 ** Input data for the Count action
 **
 *******************************************************************************/
public class AggregateInput extends AbstractTableActionInput implements QueryOrCountInputInterface
{
   private QBackendTransaction transaction;

   private QQueryFilter    filter;
   private List<Aggregate> aggregates;
   private List<GroupBy>   groupBys = new ArrayList<>();
   private Integer         limit;

   private Integer timeoutSeconds;

   private List<QueryJoin> queryJoins = null;

   private EnumSet<QueryHint> queryHints = EnumSet.noneOf(QueryHint.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public AggregateInput()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public AggregateInput(String tableName)
   {
      this();
      setTableName(tableName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public AggregateInput withTableName(String tableName)
   {
      super.withTableName(tableName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for filter
    **
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return filter;
   }



   /*******************************************************************************
    ** Setter for filter
    **
    *******************************************************************************/
   public void setFilter(QQueryFilter filter)
   {
      this.filter = filter;
   }



   /*******************************************************************************
    ** Fluent setter for filter
    **
    *******************************************************************************/
   public AggregateInput withFilter(QQueryFilter filter)
   {
      setFilter(filter);
      return (this);
   }



   /*******************************************************************************
    ** Getter for aggregates
    **
    *******************************************************************************/
   public List<Aggregate> getAggregates()
   {
      return aggregates;
   }



   /*******************************************************************************
    ** Setter for aggregates
    **
    *******************************************************************************/
   public void setAggregates(List<Aggregate> aggregates)
   {
      this.aggregates = aggregates;
   }



   /*******************************************************************************
    ** Fluent setter for aggregates
    **
    *******************************************************************************/
   public AggregateInput withAggregates(List<Aggregate> aggregates)
   {
      this.aggregates = aggregates;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for aggregates
    **
    *******************************************************************************/
   public AggregateInput withAggregate(Aggregate aggregate)
   {
      if(this.aggregates == null)
      {
         this.aggregates = new ArrayList<>();
      }
      this.aggregates.add(aggregate);
      return (this);
   }



   /*******************************************************************************
    ** Getter for groupBys
    **
    *******************************************************************************/
   public List<GroupBy> getGroupBys()
   {
      return groupBys;
   }



   /*******************************************************************************
    ** Setter for groupBys
    **
    *******************************************************************************/
   public void setGroupBys(List<GroupBy> groupBys)
   {
      this.groupBys = groupBys;
   }



   /*******************************************************************************
    ** Fluent setter for groupBys
    **
    *******************************************************************************/
   public AggregateInput withGroupBys(List<GroupBy> groupBys)
   {
      this.groupBys = groupBys;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for groupBys
    **
    *******************************************************************************/
   public AggregateInput withGroupBy(GroupBy groupBy)
   {
      if(this.groupBys == null)
      {
         this.groupBys = new ArrayList<>();
      }
      this.groupBys.add(groupBy);
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryJoins
    **
    *******************************************************************************/
   public List<QueryJoin> getQueryJoins()
   {
      return queryJoins;
   }



   /*******************************************************************************
    ** Setter for queryJoins
    **
    *******************************************************************************/
   public void setQueryJoins(List<QueryJoin> queryJoins)
   {
      this.queryJoins = queryJoins;
   }



   /*******************************************************************************
    ** Fluent setter for queryJoins
    **
    *******************************************************************************/
   public AggregateInput withQueryJoins(List<QueryJoin> queryJoins)
   {
      this.queryJoins = queryJoins;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for queryJoins
    **
    *******************************************************************************/
   public AggregateInput withQueryJoin(QueryJoin queryJoin)
   {
      if(this.queryJoins == null)
      {
         this.queryJoins = new ArrayList<>();
      }
      this.queryJoins.add(queryJoin);
      return (this);
   }



   /*******************************************************************************
    ** Getter for limit
    **
    *******************************************************************************/
   public Integer getLimit()
   {
      return limit;
   }



   /*******************************************************************************
    ** Setter for limit
    **
    *******************************************************************************/
   public void setLimit(Integer limit)
   {
      this.limit = limit;
   }



   /*******************************************************************************
    ** Fluent setter for limit
    **
    *******************************************************************************/
   public AggregateInput withLimit(Integer limit)
   {
      this.limit = limit;
      return (this);
   }



   /*******************************************************************************
    ** Getter for timeoutSeconds
    *******************************************************************************/
   public Integer getTimeoutSeconds()
   {
      return (this.timeoutSeconds);
   }



   /*******************************************************************************
    ** Setter for timeoutSeconds
    *******************************************************************************/
   public void setTimeoutSeconds(Integer timeoutSeconds)
   {
      this.timeoutSeconds = timeoutSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for timeoutSeconds
    *******************************************************************************/
   public AggregateInput withTimeoutSeconds(Integer timeoutSeconds)
   {
      this.timeoutSeconds = timeoutSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryHints
    *******************************************************************************/
   public EnumSet<QueryHint> getQueryHints()
   {
      return (this.queryHints);
   }



   /*******************************************************************************
    ** Setter for queryHints
    *******************************************************************************/
   public void setQueryHints(EnumSet<QueryHint> queryHints)
   {
      this.queryHints = queryHints;
   }



   /*******************************************************************************
    ** Fluent setter for queryHints
    *******************************************************************************/
   public AggregateInput withQueryHints(EnumSet<QueryHint> queryHints)
   {
      this.queryHints = queryHints;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for queryHints
    *******************************************************************************/
   public AggregateInput withQueryHint(QueryHint queryHint)
   {
      if(this.queryHints == null)
      {
         this.queryHints = EnumSet.noneOf(QueryHint.class);
      }
      this.queryHints.add(queryHint);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for queryHints
    *******************************************************************************/
   public AggregateInput withoutQueryHint(QueryHint queryHint)
   {
      if(this.queryHints != null)
      {
         this.queryHints.remove(queryHint);
      }
      return (this);
   }



   /*******************************************************************************
    ** null-safely check if query hints map contains the specified hint
    *******************************************************************************/
   public boolean hasQueryHint(QueryHint queryHint)
   {
      if(this.queryHints == null)
      {
         return (false);
      }

      return (queryHints.contains(queryHint));
   }



   /*******************************************************************************
    ** Getter for transaction
    *******************************************************************************/
   public QBackendTransaction getTransaction()
   {
      return (this.transaction);
   }



   /*******************************************************************************
    ** Setter for transaction
    *******************************************************************************/
   public void setTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
   }



   /*******************************************************************************
    ** Fluent setter for transaction
    *******************************************************************************/
   public AggregateInput withTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public AggregateInput withInputSource(InputSource inputSource)
   {
      super.withInputSource(inputSource);
      return (this);
   }
}
