/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables;


import java.util.EnumSet;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Common getters & setters, shared by both QueryInput and CountInput.
 **
 ** Original impetus for this class is the setCommonParamsFrom() method - for cases
 ** where we need to change a Query to a Get, or vice-versa, and we want to copy over
 ** all of those input params.
 *******************************************************************************/
public interface QueryOrCountInputInterface
{

   /*******************************************************************************
    ** Set in THIS, the "common params" (e.g., common to both Query & Count inputs)
    ** from the parameter SOURCE object.
    *******************************************************************************/
   default void setCommonParamsFrom(QueryOrCountInputInterface source)
   {
      this.setTransaction(source.getTransaction());
      this.setFilter(source.getFilter());
      this.setTableName(source.getTableName());
      this.setQueryJoins(source.getQueryJoins());
      this.setTimeoutSeconds(source.getTimeoutSeconds());
      this.setQueryHints(source.getQueryHints());
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   String getTableName();

   /***************************************************************************
    **
    ***************************************************************************/
   void setTableName(String tableName);

   /*******************************************************************************
    **
    *******************************************************************************/
   QQueryFilter getFilter();

   /***************************************************************************
    **
    ***************************************************************************/
   void setFilter(QQueryFilter filter);

   /*******************************************************************************
    ** Getter for transaction
    *******************************************************************************/
   QBackendTransaction getTransaction();

   /*******************************************************************************
    ** Setter for transaction
    *******************************************************************************/
   void setTransaction(QBackendTransaction transaction);

   /*******************************************************************************
    ** Getter for queryJoins
    *******************************************************************************/
   List<QueryJoin> getQueryJoins();

   /*******************************************************************************
    ** Setter for queryJoins
    **
    *******************************************************************************/
   void setQueryJoins(List<QueryJoin> queryJoins);

   /*******************************************************************************
    **
    *******************************************************************************/
   Integer getTimeoutSeconds();

   /***************************************************************************
    **
    ***************************************************************************/
   void setTimeoutSeconds(Integer timeoutSeconds);


   /*******************************************************************************
    ** Getter for queryHints
    *******************************************************************************/
   EnumSet<QueryHint> getQueryHints();


   /*******************************************************************************
    ** Setter for queryHints
    *******************************************************************************/
   void setQueryHints(EnumSet<QueryHint> queryHints);

   /***************************************************************************
    *
    ***************************************************************************/
   QTableMetaData getTable();

   /***************************************************************************
    *
    ***************************************************************************/
   InputSource getInputSource();
}
