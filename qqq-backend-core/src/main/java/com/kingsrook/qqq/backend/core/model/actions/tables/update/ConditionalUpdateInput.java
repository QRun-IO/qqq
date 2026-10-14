/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


package com.kingsrook.qqq.backend.core.model.actions.tables.update;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;


/*******************************************************************************
 ** Input for a ConditionalUpdateAction: set {@code values} on the rows of the
 ** table that match {@code filter}, atomically. See ConditionalUpdateInterface.
 *******************************************************************************/
public class ConditionalUpdateInput extends AbstractTableActionInput
{
   private Map<String, Serializable> values = new LinkedHashMap<>();
   private QQueryFilter              filter;
   private QBackendTransaction       transaction;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public ConditionalUpdateInput()
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public ConditionalUpdateInput(String tableName)
   {
      setTableName(tableName);
   }



   /*******************************************************************************
    ** Getter for values
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return (this.values);
   }



   /*******************************************************************************
    ** Setter for values
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Fluent setter for one value
    *******************************************************************************/
   public ConditionalUpdateInput withValue(String fieldName, Serializable value)
   {
      this.values.put(fieldName, value);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for values
    *******************************************************************************/
   public ConditionalUpdateInput withValues(Map<String, Serializable> values)
   {
      this.values = values;
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
   public ConditionalUpdateInput withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
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
   public ConditionalUpdateInput withTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
      return (this);
   }

}
