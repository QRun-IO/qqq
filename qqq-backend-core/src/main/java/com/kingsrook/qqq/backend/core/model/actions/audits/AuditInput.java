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
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;


/*******************************************************************************
 ** Input object for the audit action - an object which contains a list of "single"
 ** audit inputs - e.g., the data needed to insert 1 audit.
 *******************************************************************************/
public class AuditInput extends AbstractActionInput implements Serializable
{
   private List<AuditSingleInput> auditSingleInputList = new ArrayList<>();

   private QBackendTransaction transaction;



   /*******************************************************************************
    ** Getter for auditSingleInputList
    *******************************************************************************/
   public List<AuditSingleInput> getAuditSingleInputList()
   {
      return (this.auditSingleInputList);
   }



   /*******************************************************************************
    ** Setter for auditSingleInputList
    *******************************************************************************/
   public void setAuditSingleInputList(List<AuditSingleInput> auditSingleInputList)
   {
      this.auditSingleInputList = auditSingleInputList;
   }



   /*******************************************************************************
    ** Fluent setter for auditSingleInputList
    *******************************************************************************/
   public AuditInput withAuditSingleInputList(List<AuditSingleInput> auditSingleInputList)
   {
      this.auditSingleInputList = auditSingleInputList;
      return (this);
   }



   /*******************************************************************************
    ** Add a single auditSingleInput
    *******************************************************************************/
   public void addAuditSingleInput(AuditSingleInput auditSingleInput)
   {
      if(this.auditSingleInputList == null)
      {
         this.auditSingleInputList = new ArrayList<>();
      }
      this.auditSingleInputList.add(auditSingleInput);
   }



   /*******************************************************************************
    ** Fluent setter to add a single auditSingleInput
    *******************************************************************************/
   public AuditInput withAuditSingleInput(AuditSingleInput auditSingleInput)
   {
      addAuditSingleInput(auditSingleInput);
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
    * transaction upon which the audits will be inserted.
    *
    * @return this
    *******************************************************************************/
   public AuditInput withTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
      return (this);
   }


}
