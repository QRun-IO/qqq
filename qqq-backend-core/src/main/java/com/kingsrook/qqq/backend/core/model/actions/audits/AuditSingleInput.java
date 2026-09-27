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

package com.kingsrook.qqq.backend.core.model.actions.audits;


import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLockFilters;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Input data to insert a single audit record (with optional child record)..
 *******************************************************************************/
public class AuditSingleInput implements Serializable
{
   private String       auditTableName;
   private String       auditUserName;
   private Instant      timestamp;
   private String       message;
   private Serializable recordId;

   private Map<String, Serializable> securityKeyValues;

   private List<QRecord> details;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public AuditSingleInput()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public AuditSingleInput(QTableMetaData table, QRecord record, String auditMessage)
   {
      setAuditTableName(table.getName());
      setRecordId(record.getValue(table.getPrimaryKeyField()));
      setSecurityKeyValues(AuditAction.getRecordSecurityKeyValues(table, record, Optional.empty()));
      setMessage(auditMessage);
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public AuditSingleInput(String tableName, QRecord record, String auditMessage)
   {
      this(QContext.getQInstance().getTable(tableName), record, auditMessage);
   }



   /*******************************************************************************
    ** Getter for auditTableName
    *******************************************************************************/
   public String getAuditTableName()
   {
      return (this.auditTableName);
   }



   /*******************************************************************************
    ** Setter for auditTableName
    *******************************************************************************/
   public void setAuditTableName(String auditTableName)
   {
      this.auditTableName = auditTableName;
   }



   /*******************************************************************************
    ** Fluent setter for auditTableName
    *******************************************************************************/
   public AuditSingleInput withAuditTableName(String auditTableName)
   {
      this.auditTableName = auditTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for auditUserName
    *******************************************************************************/
   public String getAuditUserName()
   {
      return (this.auditUserName);
   }



   /*******************************************************************************
    ** Setter for auditUserName
    *******************************************************************************/
   public void setAuditUserName(String auditUserName)
   {
      this.auditUserName = auditUserName;
   }



   /*******************************************************************************
    ** Fluent setter for auditUserName
    *******************************************************************************/
   public AuditSingleInput withAuditUserName(String auditUserName)
   {
      this.auditUserName = auditUserName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for timestamp
    *******************************************************************************/
   public Instant getTimestamp()
   {
      return (this.timestamp);
   }



   /*******************************************************************************
    ** Setter for timestamp
    *******************************************************************************/
   public void setTimestamp(Instant timestamp)
   {
      this.timestamp = timestamp;
   }



   /*******************************************************************************
    ** Fluent setter for timestamp
    *******************************************************************************/
   public AuditSingleInput withTimestamp(Instant timestamp)
   {
      this.timestamp = timestamp;
      return (this);
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Setter for message
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public AuditSingleInput withMessage(String message)
   {
      this.message = message;
      return (this);
   }



   /*******************************************************************************
    ** Getter for securityKeyValues
    *******************************************************************************/
   public Map<String, Serializable> getSecurityKeyValues()
   {
      return (this.securityKeyValues);
   }



   /*******************************************************************************
    ** Setter for securityKeyValues
    *******************************************************************************/
   public void setSecurityKeyValues(Map<String, Serializable> securityKeyValues)
   {
      this.securityKeyValues = securityKeyValues;
   }



   /*******************************************************************************
    ** Fluent setter for securityKeyValues
    *******************************************************************************/
   public AuditSingleInput withSecurityKeyValues(Map<String, Serializable> securityKeyValues)
   {
      this.securityKeyValues = securityKeyValues;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordId
    *******************************************************************************/
   public Serializable getRecordId()
   {
      return (this.recordId);
   }



   /*******************************************************************************
    ** Setter for recordId
    *******************************************************************************/
   public void setRecordId(Serializable recordId)
   {
      this.recordId = recordId;
   }



   /*******************************************************************************
    ** Fluent setter for recordId
    *******************************************************************************/
   public AuditSingleInput withRecordId(Serializable recordId)
   {
      this.recordId = recordId;
      return (this);
   }



   /*******************************************************************************
    ** Populate this input from a table and record, extracting the primary key
    ** and security key values.
    *******************************************************************************/
   public AuditSingleInput forRecord(QTableMetaData table, QRecord record)
   {
      setRecordId(record.getValue(table.getPrimaryKeyField()));
      setAuditTableName(table.getName());

      this.securityKeyValues = new HashMap<>();
      for(RecordSecurityLock recordSecurityLock : RecordSecurityLockFilters.filterForReadLocks(CollectionUtils.nonNullList(table.getRecordSecurityLocks())))
      {
         this.securityKeyValues.put(recordSecurityLock.getFieldName(), record.getValue(recordSecurityLock.getFieldName()));
      }

      return (this);
   }



   /*******************************************************************************
    ** Getter for details
    *******************************************************************************/
   public List<QRecord> getDetails()
   {
      return (this.details);
   }



   /*******************************************************************************
    ** Setter for details
    *******************************************************************************/
   public void setDetails(List<QRecord> details)
   {
      this.details = details;
   }



   /*******************************************************************************
    ** Fluent setter for details
    *******************************************************************************/
   public AuditSingleInput withDetails(List<QRecord> details)
   {
      this.details = details;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for details
    *******************************************************************************/
   public AuditSingleInput withDetailMessages(List<String> details)
   {
      for(String detail : details)
      {
         addDetail(message);
      }
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addDetail(String message)
   {
      if(this.details == null)
      {
         this.details = new ArrayList<>();
      }
      QRecord detail = new QRecord().withValue("message", message);
      this.details.add(detail);
   }

}
