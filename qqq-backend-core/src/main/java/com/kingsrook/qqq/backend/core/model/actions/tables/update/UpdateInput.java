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

package com.kingsrook.qqq.backend.core.model.actions.tables.update;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.ActionFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Input data handler for the update action
 **
 *******************************************************************************/
public class UpdateInput extends AbstractTableActionInput
{
   private QBackendTransaction transaction;
   private List<QRecord>       records;
   private InputSource         inputSource = QInputSource.SYSTEM;

   ////////////////////////////////////////////////////////////////////////////////////////////
   // allow a caller to specify that they KNOW this optimization (e.g., in SQL) can be made. //
   // If you set this to true, but it isn't, then you may not get an accurate update.        //
   // If you set this to false, but it isn't, then you may not get the best performance.     //
   // Just leave it null if you don't know what you're dong.                                 //
   ////////////////////////////////////////////////////////////////////////////////////////////
   private Boolean areAllValuesBeingUpdatedTheSame = null;

   private boolean omitTriggeringAutomations = false;
   private boolean omitDmlAudit              = false;
   private boolean omitModifyDateUpdate      = false;
   private String  auditContext              = null;

   private Set<ActionFlag> flags;



   /*******************************************************************************
    **
    *******************************************************************************/
   public UpdateInput()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public UpdateInput(String tableName)
   {
      setTableName(tableName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public UpdateInput withTableName(String tableName)
   {
      super.withTableName(tableName);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public UpdateInput withRecord(QRecord record)
   {
      if(records == null)
      {
         records = new ArrayList<>();
      }

      records.add(record);

      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public UpdateInput withRecordEntity(QRecordEntity recordEntity)
   {
      return (withRecord(recordEntity.toQRecord()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public UpdateInput withRecordEntities(List<? extends QRecordEntity> recordEntityList)
   {
      for(QRecordEntity recordEntity : CollectionUtils.nonNullList(recordEntityList))
      {
         withRecordEntity(recordEntity);
      }

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
   public UpdateInput withTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
      return (this);
   }



   /*******************************************************************************
    ** Getter for records
    **
    *******************************************************************************/
   public List<QRecord> getRecords()
   {
      return records;
   }



   /*******************************************************************************
    ** Setter for records
    **
    *******************************************************************************/
   public void setRecords(List<QRecord> records)
   {
      this.records = records;
   }



   /*******************************************************************************
    ** Getter for areAllValuesBeingUpdatedTheSame
    **
    *******************************************************************************/
   public Boolean getAreAllValuesBeingUpdatedTheSame()
   {
      return areAllValuesBeingUpdatedTheSame;
   }



   /*******************************************************************************
    ** Setter for areAllValuesBeingUpdatedTheSame
    **
    *******************************************************************************/
   public void setAreAllValuesBeingUpdatedTheSame(Boolean areAllValuesBeingUpdatedTheSame)
   {
      this.areAllValuesBeingUpdatedTheSame = areAllValuesBeingUpdatedTheSame;
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
   public UpdateInput withOmitDmlAudit(boolean omitDmlAudit)
   {
      this.omitDmlAudit = omitDmlAudit;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for records
    *******************************************************************************/
   public UpdateInput withRecords(List<QRecord> records)
   {
      this.records = records;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for areAllValuesBeingUpdatedTheSame
    *******************************************************************************/
   public UpdateInput withAreAllValuesBeingUpdatedTheSame(Boolean areAllValuesBeingUpdatedTheSame)
   {
      this.areAllValuesBeingUpdatedTheSame = areAllValuesBeingUpdatedTheSame;
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
   public UpdateInput withAuditContext(String auditContext)
   {
      this.auditContext = auditContext;
      return (this);
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
   public UpdateInput withInputSource(InputSource inputSource)
   {
      this.inputSource = inputSource;
      return (this);
   }



   /*******************************************************************************
    ** Getter for omitTriggeringAutomations
    *******************************************************************************/
   public boolean getOmitTriggeringAutomations()
   {
      return (this.omitTriggeringAutomations);
   }



   /*******************************************************************************
    ** Setter for omitTriggeringAutomations
    *******************************************************************************/
   public void setOmitTriggeringAutomations(boolean omitTriggeringAutomations)
   {
      this.omitTriggeringAutomations = omitTriggeringAutomations;
   }



   /*******************************************************************************
    ** Fluent setter for omitTriggeringAutomations
    *******************************************************************************/
   public UpdateInput withOmitTriggeringAutomations(boolean omitTriggeringAutomations)
   {
      this.omitTriggeringAutomations = omitTriggeringAutomations;
      return (this);
   }



   /*******************************************************************************
    ** Getter for omitModifyDateUpdate
    *******************************************************************************/
   public boolean getOmitModifyDateUpdate()
   {
      return (this.omitModifyDateUpdate);
   }



   /*******************************************************************************
    ** Setter for omitModifyDateUpdate
    *******************************************************************************/
   public void setOmitModifyDateUpdate(boolean omitModifyDateUpdate)
   {
      this.omitModifyDateUpdate = omitModifyDateUpdate;
   }



   /*******************************************************************************
    ** Fluent setter for omitModifyDateUpdate
    *******************************************************************************/
   public UpdateInput withOmitModifyDateUpdate(boolean omitModifyDateUpdate)
   {
      this.omitModifyDateUpdate = omitModifyDateUpdate;
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
   public UpdateInput withFlags(Set<ActionFlag> flags)
   {
      this.flags = flags;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public UpdateInput withFlag(ActionFlag flag)
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
