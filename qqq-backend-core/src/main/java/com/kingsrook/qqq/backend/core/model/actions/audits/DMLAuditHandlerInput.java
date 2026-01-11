/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.model.actions.audits;


import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** Input for DML-level audit handlers containing full record snapshots.
 ** This provides complete before/after record states for HIPAA/WORM compliance.
 *******************************************************************************/
public class DMLAuditHandlerInput implements Serializable
{
   private String                   tableName;
   private DMLType                  dmlType;
   private List<QRecord>            newRecords;
   private List<QRecord>            oldRecords;
   private AbstractTableActionInput tableActionInput;
   private Instant                  timestamp;
   private String                   auditContext;
   private QSession                 session;



   /*******************************************************************************
    ** DML operation type
    *******************************************************************************/
   public enum DMLType
   {
      INSERT,
      UPDATE,
      DELETE
   }



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public DMLAuditHandlerInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dmlType
    *******************************************************************************/
   public DMLType getDmlType()
   {
      return (this.dmlType);
   }



   /*******************************************************************************
    ** Setter for dmlType
    *******************************************************************************/
   public void setDmlType(DMLType dmlType)
   {
      this.dmlType = dmlType;
   }



   /*******************************************************************************
    ** Fluent setter for dmlType
    *******************************************************************************/
   public DMLAuditHandlerInput withDmlType(DMLType dmlType)
   {
      this.dmlType = dmlType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for newRecords
    *******************************************************************************/
   public List<QRecord> getNewRecords()
   {
      return (this.newRecords);
   }



   /*******************************************************************************
    ** Setter for newRecords
    *******************************************************************************/
   public void setNewRecords(List<QRecord> newRecords)
   {
      this.newRecords = newRecords;
   }



   /*******************************************************************************
    ** Fluent setter for newRecords
    *******************************************************************************/
   public DMLAuditHandlerInput withNewRecords(List<QRecord> newRecords)
   {
      this.newRecords = newRecords;
      return (this);
   }



   /*******************************************************************************
    ** Getter for oldRecords
    *******************************************************************************/
   public List<QRecord> getOldRecords()
   {
      return (this.oldRecords);
   }



   /*******************************************************************************
    ** Setter for oldRecords
    *******************************************************************************/
   public void setOldRecords(List<QRecord> oldRecords)
   {
      this.oldRecords = oldRecords;
   }



   /*******************************************************************************
    ** Fluent setter for oldRecords
    *******************************************************************************/
   public DMLAuditHandlerInput withOldRecords(List<QRecord> oldRecords)
   {
      this.oldRecords = oldRecords;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableActionInput
    *******************************************************************************/
   public AbstractTableActionInput getTableActionInput()
   {
      return (this.tableActionInput);
   }



   /*******************************************************************************
    ** Setter for tableActionInput
    *******************************************************************************/
   public void setTableActionInput(AbstractTableActionInput tableActionInput)
   {
      this.tableActionInput = tableActionInput;
   }



   /*******************************************************************************
    ** Fluent setter for tableActionInput
    *******************************************************************************/
   public DMLAuditHandlerInput withTableActionInput(AbstractTableActionInput tableActionInput)
   {
      this.tableActionInput = tableActionInput;
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
   public DMLAuditHandlerInput withTimestamp(Instant timestamp)
   {
      this.timestamp = timestamp;
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
   public DMLAuditHandlerInput withAuditContext(String auditContext)
   {
      this.auditContext = auditContext;
      return (this);
   }



   /*******************************************************************************
    ** Getter for session
    *******************************************************************************/
   public QSession getSession()
   {
      return (this.session);
   }



   /*******************************************************************************
    ** Setter for session
    *******************************************************************************/
   public void setSession(QSession session)
   {
      this.session = session;
   }



   /*******************************************************************************
    ** Fluent setter for session
    *******************************************************************************/
   public DMLAuditHandlerInput withSession(QSession session)
   {
      this.session = session;
      return (this);
   }

}
