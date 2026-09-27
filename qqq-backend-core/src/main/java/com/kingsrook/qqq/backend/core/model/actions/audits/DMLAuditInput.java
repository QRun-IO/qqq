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
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Input object for the DML audit action.
 *******************************************************************************/
public class DMLAuditInput extends AbstractActionInput implements Serializable
{
   private List<QRecord>            recordList;
   private List<QRecord>            oldRecordList;
   private AbstractTableActionInput tableActionInput;

   private QBackendTransaction transaction;

   private String auditContext = null;



   /*******************************************************************************
    ** Getter for recordList
    *******************************************************************************/
   public List<QRecord> getRecordList()
   {
      return (this.recordList);
   }



   /*******************************************************************************
    ** Setter for recordList
    *******************************************************************************/
   public void setRecordList(List<QRecord> recordList)
   {
      this.recordList = recordList;
   }



   /*******************************************************************************
    ** Fluent setter for recordList
    *******************************************************************************/
   public DMLAuditInput withRecordList(List<QRecord> recordList)
   {
      this.recordList = recordList;
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
   public DMLAuditInput withTableActionInput(AbstractTableActionInput tableActionInput)
   {
      this.tableActionInput = tableActionInput;
      return (this);
   }



   /*******************************************************************************
    ** Getter for oldRecordList
    *******************************************************************************/
   public List<QRecord> getOldRecordList()
   {
      return (this.oldRecordList);
   }



   /*******************************************************************************
    ** Setter for oldRecordList
    *******************************************************************************/
   public void setOldRecordList(List<QRecord> oldRecordList)
   {
      this.oldRecordList = oldRecordList;
   }



   /*******************************************************************************
    ** Fluent setter for oldRecordList
    *******************************************************************************/
   public DMLAuditInput withOldRecordList(List<QRecord> oldRecordList)
   {
      this.oldRecordList = oldRecordList;
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
   public DMLAuditInput withAuditContext(String auditContext)
   {
      this.auditContext = auditContext;
      return (this);
   }


   /*******************************************************************************
    * Getter for transaction
    * @see #withTransaction(QBackendTransaction)
    *******************************************************************************/
   public QBackendTransaction getTransaction()
   {
      return (this.transaction);
   }



   /*******************************************************************************
    * Setter for transaction
    * @see #withTransaction(QBackendTransaction)
    *******************************************************************************/
   public void setTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
   }



   /*******************************************************************************
    * Fluent setter for transaction
    *
    * @param transaction
    * transaction that will be used for inserting the audits, where (presumably)
    * the DML against the record occurred as well
    *
    * @return this
    *******************************************************************************/
   public DMLAuditInput withTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
      return (this);
   }


}
