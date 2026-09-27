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

package com.kingsrook.qqq.backend.core.model.actions.tables.insert;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.ActionFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Input data for the Insert action
 **
 *******************************************************************************/
public class InsertInput extends AbstractTableActionInput
{
   private QBackendTransaction transaction;
   private List<QRecord>       records;

   private boolean skipUniqueKeyCheck = false;

   private boolean omitDmlAudit = false;
   private String  auditContext = null;

   private Set<ActionFlag> flags;



   /*******************************************************************************
    **
    *******************************************************************************/
   public InsertInput()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public InsertInput(String tableName)
   {
      setTableName(tableName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public InsertInput withTableName(String tableName)
   {
      super.withTableName(tableName);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public InsertInput withRecord(QRecord record)
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
   public InsertInput withRecordEntity(QRecordEntity recordEntity)
   {
      return (withRecord(recordEntity.toQRecord()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public InsertInput withRecordEntities(List<? extends QRecordEntity> recordEntityList)
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
   public InsertInput withTransaction(QBackendTransaction transaction)
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
    ** Getter for skipUniqueKeyCheck
    **
    *******************************************************************************/
   public boolean getSkipUniqueKeyCheck()
   {
      return skipUniqueKeyCheck;
   }



   /*******************************************************************************
    ** Setter for skipUniqueKeyCheck
    **
    *******************************************************************************/
   public void setSkipUniqueKeyCheck(boolean skipUniqueKeyCheck)
   {
      this.skipUniqueKeyCheck = skipUniqueKeyCheck;
   }



   /*******************************************************************************
    ** Fluent setter for skipUniqueKeyCheck
    **
    *******************************************************************************/
   public InsertInput withSkipUniqueKeyCheck(boolean skipUniqueKeyCheck)
   {
      this.skipUniqueKeyCheck = skipUniqueKeyCheck;
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
   public InsertInput withOmitDmlAudit(boolean omitDmlAudit)
   {
      this.omitDmlAudit = omitDmlAudit;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for records
    *******************************************************************************/
   public InsertInput withRecords(List<QRecord> records)
   {
      this.records = records;
      return (this);
   }




   /*******************************************************************************
    ** Fluent setter for inputSource
    *******************************************************************************/
   public InsertInput withInputSource(InputSource inputSource)
   {
      setInputSource(inputSource);
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
   public InsertInput withAuditContext(String auditContext)
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
   public InsertInput withFlags(Set<ActionFlag> flags)
   {
      this.flags = flags;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public InsertInput withFlag(ActionFlag flag)
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
