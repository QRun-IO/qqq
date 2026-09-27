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

package com.kingsrook.qqq.backend.core.model.actions.tables.delete;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.ActionFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.collections.MutableList;


/*******************************************************************************
 ** Input for a Delete action.
 **
 *******************************************************************************/
public class DeleteInput extends AbstractTableActionInput
{
   private QBackendTransaction transaction;
   private List<Serializable>  primaryKeys;
   private QQueryFilter        queryFilter;
   private InputSource         inputSource = QInputSource.SYSTEM;

   private boolean omitDmlAudit = false;
   private String  auditContext = null;

   private Set<ActionFlag> flags;



   /*******************************************************************************
    **
    *******************************************************************************/
   public DeleteInput()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public DeleteInput(String tableName)
   {
      setTableName(tableName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public DeleteInput withTableName(String tableName)
   {
      super.withTableName(tableName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for transaction
    **
    *******************************************************************************/
   public QBackendTransaction getTransaction()
   {
      return transaction;
   }



   /*******************************************************************************
    ** Setter for transaction
    **
    *******************************************************************************/
   public void setTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
   }



   /*******************************************************************************
    ** Fluent setter for transaction
    **
    *******************************************************************************/
   public DeleteInput withTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
      return (this);
   }



   /*******************************************************************************
    ** Getter for ids
    **
    *******************************************************************************/
   public List<Serializable> getPrimaryKeys()
   {
      return primaryKeys;
   }



   /*******************************************************************************
    ** Setter for ids
    **
    *******************************************************************************/
   public void setPrimaryKeys(List<Serializable> primaryKeys)
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////
      // the action may edit this list (e.g., to remove keys w/ errors), so wrap it in MutableList //
      ///////////////////////////////////////////////////////////////////////////////////////////////
      this.primaryKeys = primaryKeys == null ? null : new MutableList<>(primaryKeys);
   }



   /*******************************************************************************
    ** Fluently add 1 primary key to the delete input
    **
    *******************************************************************************/
   public DeleteInput withPrimaryKey(Serializable primaryKey)
   {
      if(primaryKeys == null)
      {
         primaryKeys = new ArrayList<>();
      }

      primaryKeys.add(primaryKey);

      return (this);
   }



   /*******************************************************************************
    ** Setter for ids
    **
    *******************************************************************************/
   public DeleteInput withPrimaryKeys(List<Serializable> primaryKeys)
   {
      setPrimaryKeys(primaryKeys);
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryFilter
    **
    *******************************************************************************/
   public QQueryFilter getQueryFilter()
   {
      return queryFilter;
   }



   /*******************************************************************************
    ** Setter for queryFilter
    **
    *******************************************************************************/
   public void setQueryFilter(QQueryFilter queryFilter)
   {
      this.queryFilter = queryFilter;
   }



   /*******************************************************************************
    ** Fluent setter for queryFilter
    **
    *******************************************************************************/
   public DeleteInput withQueryFilter(QQueryFilter queryFilter)
   {
      this.queryFilter = queryFilter;
      return this;
   }



   /*******************************************************************************
    ** Getter for inputSource
    *******************************************************************************/
   public InputSource getInputSource()
   {
      return (this.inputSource);
   }



   /*******************************************************************************
    ** Setter for inputSource
    *******************************************************************************/
   public void setInputSource(InputSource inputSource)
   {
      this.inputSource = inputSource;
   }



   /*******************************************************************************
    ** Fluent setter for inputSource
    *******************************************************************************/
   public DeleteInput withInputSource(InputSource inputSource)
   {
      this.inputSource = inputSource;
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
   public DeleteInput withOmitDmlAudit(boolean omitDmlAudit)
   {
      this.omitDmlAudit = omitDmlAudit;
      return (this);
   }



   /*******************************************************************************
    ** Getter for auditContext
    *******************************************************************************/
   public String getAuditContext()
   {
      return (this.auditContext);
   }



   /*******************************************************************************
    ** Setter for auditContext
    *******************************************************************************/
   public void setAuditContext(String auditContext)
   {
      this.auditContext = auditContext;
   }



   /*******************************************************************************
    ** Fluent setter for auditContext
    *******************************************************************************/
   public DeleteInput withAuditContext(String auditContext)
   {
      this.auditContext = auditContext;
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
   public DeleteInput withFlags(Set<ActionFlag> flags)
   {
      this.flags = flags;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public DeleteInput withFlag(ActionFlag flag)
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
