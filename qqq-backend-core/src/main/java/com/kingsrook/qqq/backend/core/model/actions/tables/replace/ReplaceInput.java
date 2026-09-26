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

package com.kingsrook.qqq.backend.core.model.actions.tables.replace;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.ActionFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 **
 *******************************************************************************/
public class ReplaceInput extends AbstractTableActionInput
{
   private QBackendTransaction transaction;
   private UniqueKey           key;
   private List<QRecord>       records;
   private QQueryFilter        filter;
   private boolean             performDeletes                 = true;
   private boolean             allowNullKeyValuesToEqual      = false;
   private boolean             setPrimaryKeyInInsertedRecords = false;

   private boolean omitDmlAudit = false;

   private Set<ActionFlag> flags;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ReplaceInput()
   {
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
   public ReplaceInput withTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
      return (this);
   }



   /*******************************************************************************
    ** Getter for records
    *******************************************************************************/
   public List<QRecord> getRecords()
   {
      return (this.records);
   }



   /*******************************************************************************
    ** Setter for records
    *******************************************************************************/
   public void setRecords(List<QRecord> records)
   {
      this.records = records;
   }



   /*******************************************************************************
    ** Fluent setter for records
    *******************************************************************************/
   public ReplaceInput withRecords(List<QRecord> records)
   {
      this.records = records;
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
   public ReplaceInput withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for key
    *******************************************************************************/
   public UniqueKey getKey()
   {
      return (this.key);
   }



   /*******************************************************************************
    ** Setter for key
    *******************************************************************************/
   public void setKey(UniqueKey key)
   {
      this.key = key;
   }



   /*******************************************************************************
    ** Fluent setter for key
    *******************************************************************************/
   public ReplaceInput withKey(UniqueKey key)
   {
      this.key = key;
      return (this);
   }



   /*******************************************************************************
    ** Getter for omitDmlAudit
    *******************************************************************************/
   public boolean getOmitDmlAudit()
   {
      return (this.omitDmlAudit);
   }



   /*******************************************************************************
    ** Setter for omitDmlAudit
    *******************************************************************************/
   public void setOmitDmlAudit(boolean omitDmlAudit)
   {
      this.omitDmlAudit = omitDmlAudit;
   }



   /*******************************************************************************
    ** Fluent setter for omitDmlAudit
    *******************************************************************************/
   public ReplaceInput withOmitDmlAudit(boolean omitDmlAudit)
   {
      this.omitDmlAudit = omitDmlAudit;
      return (this);
   }



   /*******************************************************************************
    ** Getter for performDeletes
    *******************************************************************************/
   public boolean getPerformDeletes()
   {
      return (this.performDeletes);
   }



   /*******************************************************************************
    ** Setter for performDeletes
    *******************************************************************************/
   public void setPerformDeletes(boolean performDeletes)
   {
      this.performDeletes = performDeletes;
   }



   /*******************************************************************************
    ** Fluent setter for performDeletes
    *******************************************************************************/
   public ReplaceInput withPerformDeletes(boolean performDeletes)
   {
      this.performDeletes = performDeletes;
      return (this);
   }



   /*******************************************************************************
    ** Getter for allowNullKeyValuesToEqual
    *******************************************************************************/
   public boolean getAllowNullKeyValuesToEqual()
   {
      return (this.allowNullKeyValuesToEqual);
   }



   /*******************************************************************************
    ** Setter for allowNullKeyValuesToEqual
    *******************************************************************************/
   public void setAllowNullKeyValuesToEqual(boolean allowNullKeyValuesToEqual)
   {
      this.allowNullKeyValuesToEqual = allowNullKeyValuesToEqual;
   }



   /*******************************************************************************
    ** Fluent setter for allowNullKeyValuesToEqual
    *******************************************************************************/
   public ReplaceInput withAllowNullKeyValuesToEqual(boolean allowNullKeyValuesToEqual)
   {
      this.allowNullKeyValuesToEqual = allowNullKeyValuesToEqual;
      return (this);
   }



   /*******************************************************************************
    ** Getter for setPrimaryKeyInInsertedRecords
    *******************************************************************************/
   public boolean getSetPrimaryKeyInInsertedRecords()
   {
      return (this.setPrimaryKeyInInsertedRecords);
   }



   /*******************************************************************************
    ** Setter for setPrimaryKeyInInsertedRecords
    *******************************************************************************/
   public void setSetPrimaryKeyInInsertedRecords(boolean setPrimaryKeyInInsertedRecords)
   {
      this.setPrimaryKeyInInsertedRecords = setPrimaryKeyInInsertedRecords;
   }



   /*******************************************************************************
    ** Fluent setter for setPrimaryKeyInInsertedRecords
    *******************************************************************************/
   public ReplaceInput withSetPrimaryKeyInInsertedRecords(boolean setPrimaryKeyInInsertedRecords)
   {
      this.setPrimaryKeyInInsertedRecords = setPrimaryKeyInInsertedRecords;
      return (this);
   }



   /*******************************************************************************
    ** Getter for flags
    *******************************************************************************/
   public Set<ActionFlag> getFlags()
   {
      return (this.flags);
   }



   /*******************************************************************************
    ** Setter for flags
    *******************************************************************************/
   public void setFlags(Set<ActionFlag> flags)
   {
      this.flags = flags;
   }



   /*******************************************************************************
    ** Fluent setter for flags
    *******************************************************************************/
   public ReplaceInput withFlags(Set<ActionFlag> flags)
   {
      this.flags = flags;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public ReplaceInput withFlag(ActionFlag flag)
   {
      if(this.flags == null)
      {
         this.flags = new HashSet<>();
      }
      this.flags.add(flag);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public boolean hasFlag(ActionFlag flag)
   {
      if(this.flags == null)
      {
         return (false);
      }

      return (this.flags.contains(flag));
   }

}
