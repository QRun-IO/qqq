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

package com.kingsrook.qqq.backend.core.model.actions.tables.count;


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
public class CountInput extends AbstractTableActionInput implements QueryOrCountInputInterface
{
   private QBackendTransaction transaction;
   private QQueryFilter        filter;

   private Integer timeoutSeconds;

   private List<QueryJoin> queryJoins           = null;
   private Boolean         includeDistinctCount = false;

   private EnumSet<QueryHint> queryHints = EnumSet.noneOf(QueryHint.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public CountInput()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public CountInput(String tableName)
   {
      setTableName(tableName);
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
   public CountInput withQueryJoins(List<QueryJoin> queryJoins)
   {
      this.queryJoins = queryJoins;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for queryJoins
    **
    *******************************************************************************/
   public CountInput withQueryJoin(QueryJoin queryJoin)
   {
      if(this.queryJoins == null)
      {
         this.queryJoins = new ArrayList<>();
      }
      this.queryJoins.add(queryJoin);
      return (this);
   }



   /*******************************************************************************
    ** Getter for includeDistinctCount
    *******************************************************************************/
   public Boolean getIncludeDistinctCount()
   {
      return (this.includeDistinctCount);
   }



   /*******************************************************************************
    ** Setter for includeDistinctCount
    *******************************************************************************/
   public void setIncludeDistinctCount(Boolean includeDistinctCount)
   {
      this.includeDistinctCount = includeDistinctCount;
   }



   /*******************************************************************************
    ** Fluent setter for includeDistinctCount
    *******************************************************************************/
   public CountInput withIncludeDistinctCount(Boolean includeDistinctCount)
   {
      this.includeDistinctCount = includeDistinctCount;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for filter
    *******************************************************************************/
   public CountInput withFilter(QQueryFilter filter)
   {
      this.filter = filter;
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
   public CountInput withTimeoutSeconds(Integer timeoutSeconds)
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
   public CountInput withQueryHints(EnumSet<QueryHint> queryHints)
   {
      this.queryHints = queryHints;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for queryHints
    *******************************************************************************/
   public CountInput withQueryHint(QueryHint queryHint)
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
   public CountInput withoutQueryHint(QueryHint queryHint)
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
   public CountInput withTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public CountInput withInputSource(InputSource inputSource)
   {
      super.withInputSource(inputSource);
      return (this);
   }
}
