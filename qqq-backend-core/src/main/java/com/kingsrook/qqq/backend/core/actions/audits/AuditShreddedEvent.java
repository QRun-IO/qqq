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

package com.kingsrook.qqq.backend.core.actions.audits;


import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.ClassifiedValueTokens;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Records key destruction without modifying previously sealed audit rows.
 *******************************************************************************/
public final class AuditShreddedEvent
{
   /*******************************************************************************
    ** Static utility.
    *******************************************************************************/
   private AuditShreddedEvent()
   {
   }



   /*******************************************************************************
    ** Write through AuditAction; callers destroying keys pass the same transaction.
    *******************************************************************************/
   public static void shreddedDetail(QTableMetaData table, Serializable pk, QFieldMetaData field, String ruleId, QBackendTransaction transaction) throws QException
   {
      String message = "Audit values for " + field.getLabel() + " shredded per policy";
      QRecord detail = new QRecord().withValue("fieldName", field.getName()).withValue("newValue", ClassifiedValueTokens.shreddedToken(ruleId)).withValue("message", message);
      AuditInput input = new AuditInput();
      input.setTransaction(transaction);
      QRecord record = new QRecord().withValue(table.getPrimaryKeyField(), pk);
      if(table.getRecordSecurityLocks() != null && !table.getRecordSecurityLocks().isEmpty())
      {
         QRecord stored = new GetAction().executeForRecord(new GetInput(table.getName()).withPrimaryKey(pk).withTransaction(transaction));
         if(stored != null)
         {
            record = stored;
         }
      }
      AuditAction.appendToInput(input, table.getName(), pk, AuditAction.getRecordSecurityKeyValues(table, record, Optional.empty()), message, List.of(detail));
      new AuditAction().execute(input);
   }



   /*******************************************************************************
    ** Use the transaction in the current context, if any.
    *******************************************************************************/
   public static void shreddedDetail(QTableMetaData table, Serializable pk, QFieldMetaData field, String ruleId) throws QException
   {
      shreddedDetail(table, pk, field, ruleId, QContext.getQBackendTransaction());
   }
}
