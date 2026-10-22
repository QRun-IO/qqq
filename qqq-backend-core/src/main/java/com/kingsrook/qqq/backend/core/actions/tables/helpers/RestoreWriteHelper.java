/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.audits.DMLAuditAction;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.RestoreWriteFlag;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTrackingType;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Helpers for the pieces of Insert/Update that behave differently under a
 ** RestoreWriteFlag.  Each method is only called when the flag is present; the
 ** permission to use the flag is checked up front by the actions (via
 ** CorePermissionEnforcer.checkRestoreWriteFlag).
 *******************************************************************************/
public class RestoreWriteHelper
{

   /*******************************************************************************
    ** private constructor - static utility class.
    *******************************************************************************/
   private RestoreWriteHelper()
   {
   }



   /*******************************************************************************
    ** Apply field behaviors (as ValueBehaviorApplier.applyFieldBehaviors), but keep
    ** any create-date / modify-date value the caller supplied - those behaviors
    ** only fill nulls in restore mode.
    *******************************************************************************/
   public static void applyFieldBehaviorsKeepingDates(ValueBehaviorApplier.Action action, QTableMetaData table, List<QRecord> records, Set<FieldBehavior<?>> behaviorsToOmit)
   {
      ///////////////////////////////////////////////////////////////////////////////
      // remember each supplied date value, apply behaviors, then put them back    //
      // (a null stays null, and so gets the behavior's value, as usual)           //
      ///////////////////////////////////////////////////////////////////////////////
      Map<QRecord, Map<String, Serializable>> supplied = new HashMap<>();
      for(QFieldMetaData field : table.getFields().values())
      {
         DynamicDefaultValueBehavior behavior = field.getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class);
         if(DynamicDefaultValueBehavior.CREATE_DATE.equals(behavior) || DynamicDefaultValueBehavior.MODIFY_DATE.equals(behavior))
         {
            for(QRecord record : CollectionUtils.nonNullList(records))
            {
               Serializable value = record.getValue(field.getName());
               if(value != null)
               {
                  supplied.computeIfAbsent(record, r -> new HashMap<>()).put(field.getName(), value);
               }
            }
         }
      }

      ValueBehaviorApplier.applyFieldBehaviors(action, QContext.getQInstance(), table, records, behaviorsToOmit);

      supplied.forEach((record, values) -> values.forEach(record::setValue));
   }



   /*******************************************************************************
    ** For an insert in restore mode with suppressAutomations:  put records straight
    ** to automation status OK (unless the record supplied a status), rather than
    ** PENDING_INSERT_AUTOMATIONS.
    *******************************************************************************/
   public static void setAutomationStatusOk(QTableMetaData table, List<QRecord> records)
   {
      if(table == null || table.getAutomationDetails() == null || table.getAutomationDetails().getStatusTracking() == null)
      {
         return;
      }

      if(!AutomationStatusTrackingType.FIELD_IN_TABLE.equals(table.getAutomationDetails().getStatusTracking().getType()))
      {
         return;
      }

      String fieldName = table.getAutomationDetails().getStatusTracking().getFieldName();
      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         if(record.getValue(fieldName) == null)
         {
            record.setValue(fieldName, AutomationStatus.OK.getId());
         }
      }
   }



   /*******************************************************************************
    ** Write the one summary audit for a restore-mode action call - honoring the
    ** table's audit level (none means no audit at all).  Used in place of the
    ** per-record DML audit.
    *******************************************************************************/
   public static void writeSummaryAudit(AbstractTableActionInput input, String operation, List<QRecord> records)
   {
      writeSummaryAudit(input, operation, (int) CollectionUtils.nonNullList(records).stream().filter(r -> CollectionUtils.nullSafeIsEmpty(r.getErrors())).count());
   }



   /*******************************************************************************
    ** Overload taking the count of successfully-written records directly.
    *******************************************************************************/
   public static void writeSummaryAudit(AbstractTableActionInput input, String operation, int successCount)
   {
      AuditLevel auditLevel = DMLAuditAction.getAuditLevel(input);
      if(auditLevel == null || AuditLevel.NONE.equals(auditLevel) || successCount == 0)
      {
         return;
      }

      AuditAction.execute(input.getTableName(), null, null, "Restore-mode " + operation + " of " + successCount + " record(s)");
   }

}
