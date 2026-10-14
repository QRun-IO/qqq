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

package com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Protect audit table egress using the policy of the table whose data changed.
 *******************************************************************************/
public final class AuditFieldAccess
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private AuditFieldAccess()
   {
   }



   /*******************************************************************************
    ** Called only at enforced read boundaries, after ordinary query post-processing.
    *******************************************************************************/
   public static void apply(String tableName, List<QueryJoin> joins, List<QRecord> records) throws QException
   {
      Set<String> prefixes = new LinkedHashSet<>();
      boolean auditTable = AuditsMetaDataProvider.TABLE_NAME_AUDIT.equals(tableName);
      Set<String> auditPrefixes = new LinkedHashSet<>();
      if(auditTable)
      {
         auditPrefixes.add("");
      }
      if(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL.equals(tableName))
      {
         prefixes.add("");
      }
      for(QueryJoin join : CollectionUtils.nonNullList(joins))
      {
         if(join == null)
         {
            continue;
         }
         if(AuditsMetaDataProvider.TABLE_NAME_AUDIT.equals(join.getJoinTable()))
         {
            auditPrefixes.add(join.getJoinTableOrItsAlias() + ".");
         }
         if(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL.equals(join.getJoinTable()))
         {
            prefixes.add(join.getJoinTableOrItsAlias() + ".");
         }
      }
      // Trusted process/custom-source records may retain table-prefixed joins without a QueryInput.
      for(QRecord record : records)
      {
         if(record == null)
         {
            continue;
         }
         Set<String> keys = new LinkedHashSet<>(CollectionUtils.nonNullMap(record.getValues()).keySet());
         keys.addAll(CollectionUtils.nonNullMap(record.getDisplayValues()).keySet());
         if(keys.stream().anyMatch(key -> key.startsWith(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL + ".")))
         {
            prefixes.add(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL + ".");
         }
         if(keys.stream().anyMatch(key -> key.startsWith(AuditsMetaDataProvider.TABLE_NAME_AUDIT + ".")))
         {
            auditPrefixes.add(AuditsMetaDataProvider.TABLE_NAME_AUDIT + ".");
         }
      }
      if(prefixes.isEmpty() && auditPrefixes.isEmpty())
      {
         return;
      }
      Map<Serializable, String> tableNames = new HashMap<>();
      Iterator<QRecord> iterator = records.iterator();
      while(iterator.hasNext())
      {
         QRecord record = iterator.next();
         if(record == null)
         {
            continue;
         }
         boolean keep = true;
         for(String prefix : prefixes)
         {
            Serializable auditId = record.getValue(prefix + "auditId");
            String fieldName = record.getValueString(prefix + "fieldName");
            boolean projectedFieldName = fieldName == null;
            if((auditId == null || fieldName == null) && record.getValue(prefix + "id") != null)
            {
               QRecord fullDetail = get(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL, record.getValue(prefix + "id"));
               if(fullDetail != null)
               {
                  auditId = fullDetail.getValue("auditId");
                  fieldName = fullDetail.getValueString("fieldName");
               }
            }
            if(auditId == null && auditTable)
            {
               auditId = record.getValue("id");
            }
            String auditedTableName = tableNameForAudit(auditId, tableNames);
            if(auditedTableName == null)
            {
               keep = false;
               break;
            }
            record.setValue(prefix + "fieldName", fieldName);
            List<QRecord> singleton = new ArrayList<>(List.of(record));
            FieldAccessHelper.applyToAuditDetails(auditedTableName, singleton, prefix);
            if(projectedFieldName)
            {
               record.getValues().remove(prefix + "fieldName");
            }
            if(singleton.isEmpty())
            {
               keep = false;
               break;
            }
            if((fieldName == null || !QContext.getQInstance().getTable(auditedTableName).getFields().containsKey(fieldName)) && restricted(auditedTableName))
            {
               record.setValue(prefix + "oldValue", FieldValueMasker.MASK);
               record.setValue(prefix + "newValue", FieldValueMasker.MASK);
               record.setDisplayValue(prefix + "oldValue", FieldValueMasker.MASK);
               record.setDisplayValue(prefix + "newValue", FieldValueMasker.MASK);
               record.setValue(prefix + "message", null);
               record.setDisplayValue(prefix + "message", null);
            }
         }
         for(String prefix : auditPrefixes)
         {
            String auditedTableName = tableNameForAudit(record.getValue(prefix + "id"), tableNames);
            if(auditedTableName == null)
            {
               keep = false;
            }
            else if(restricted(auditedTableName))
            {
               record.setValue(prefix + "message", null);
               record.setDisplayValue(prefix + "message", null);
               record.setRecordLabel(null);
            }
         }
         if(!keep)
         {
            iterator.remove();
         }
      }
   }



   /*******************************************************************************
    ** Resolve ownership once per audit in each result batch; unknown ownership is withheld.
    *******************************************************************************/
   private static String tableNameForAudit(Serializable auditId, Map<Serializable, String> tableNames) throws QException
   {
      if(!tableNames.containsKey(auditId))
      {
         QRecord audit = get(AuditsMetaDataProvider.TABLE_NAME_AUDIT, auditId);
         QRecord auditTable = audit == null ? null : get(AuditsMetaDataProvider.TABLE_NAME_AUDIT_TABLE, audit.getValue("auditTableId"));
         String name = auditTable == null ? null : auditTable.getValueString("name");
         tableNames.put(auditId, name == null || QContext.getQInstance().getTable(name) == null ? null : name);
      }
      return (tableNames.get(auditId));
   }



   /*******************************************************************************
    ** Free-text audit messages cannot safely preserve values for restricted tables.
    *******************************************************************************/
   private static boolean restricted(String tableName)
   {
      return (FieldAccessHelper.getTiers(QContext.getQInstance().getTable(tableName)).values().stream().anyMatch(tier -> tier != FieldAccessTier.FULL));
   }


   /*******************************************************************************
    ** Resolve audit ownership inside the caller's authorized scope.
    *******************************************************************************/
   private static QRecord get(String tableName, Serializable id) throws QException
   {
      try(com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer.AuthorizedScope ignored = com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer.openAuthorizedScope())
      {
         return (id == null ? null : new GetAction().executeForRecord(new GetInput(tableName).withPrimaryKey(id)));
      }
   }
}
