/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.listeners;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** What a RecordChangeListenerInterface receives after a successful write.
 **
 ** records:
 ** - INSERT: the inserted records, including generated keys.
 ** - UPDATE: the updated records, as the backend returned them.
 ** - DELETE: the deleted records, as they were stored before the delete.
 **
 ** oldRecords:
 ** - INSERT: null.
 ** - UPDATE: the stored records from before the update, index-aligned with
 **   records.  An entry is null when its old record could not be fetched.
 ** - DELETE: the same list as records.
 **
 ** transaction: the caller's transaction, or null if the action ran without one.
 *******************************************************************************/
public class RecordChangeEvent
{
   private String              tableName;
   private RecordChangeType    type;
   private List<QRecord>       records;
   private List<QRecord>       oldRecords;
   private QBackendTransaction transaction;



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
   public RecordChangeEvent withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public RecordChangeType getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(RecordChangeType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public RecordChangeEvent withType(RecordChangeType type)
   {
      this.type = type;
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
   public RecordChangeEvent withRecords(List<QRecord> records)
   {
      this.records = records;
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
   public RecordChangeEvent withOldRecords(List<QRecord> oldRecords)
   {
      this.oldRecords = oldRecords;
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
   public RecordChangeEvent withTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
      return (this);
   }

}
