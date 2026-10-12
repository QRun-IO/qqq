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

package com.kingsrook.qqq.backend.core.actions.permissions;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Runtime evaluation of FieldSecurityLock (issue #1062).
 **
 ** A field with a FieldSecurityLock is accessible to the current session when
 ** the lock's effective behavior is ALLOW:
 ** - the default behavior applies, unless the session has (any of) the lock's
 **   overrideValues for the lock's securityKeyType - in which case the opposite
 **   behavior applies (e.g., default DENY + an override value present = ALLOW).
 ** - a session holding the key type's all-access key, and the system user
 **   session, may always access the field.
 ** - a table's primary key field is never treated as denied (a lock there would
 **   make records unaddressable).
 **
 ** The core actions apply these helpers only when CorePermissionEnforcer says
 ** permissions are being enforced for a call - the same opt-in (the instance
 ** setting, or PermissionEnforcement.ENFORCE) as table/process permissions.
 *******************************************************************************/
public final class FieldSecurityLockHelper
{

   /*******************************************************************************
    ** private constructor - static utility class.
    *******************************************************************************/
   private FieldSecurityLockHelper()
   {
   }



   /*******************************************************************************
    ** Is the field accessible (readable and writable) for the current session?
    *******************************************************************************/
   public static boolean isFieldAccessible(QFieldMetaData field)
   {
      if(field == null)
      {
         return (true);
      }

      FieldSecurityLock lock = field.getFieldSecurityLock();
      if(lock == null)
      {
         return (true);
      }

      QSession session = QContext.getQSession();
      if(session instanceof QSystemUserSession)
      {
         return (true);
      }

      FieldSecurityLock.Behavior defaultBehavior = Objects.requireNonNullElse(lock.getDefaultBehavior(), FieldSecurityLock.Behavior.DENY);
      if(session == null)
      {
         return (FieldSecurityLock.Behavior.ALLOW.equals(defaultBehavior));
      }

      QInstance        qInstance = QContext.getQInstance();
      QSecurityKeyType keyType   = qInstance == null ? null : qInstance.getSecurityKeyType(lock.getSecurityKeyType());
      if(keyType != null && keyType.getAllAccessKeyName() != null && sessionHasValue(session, keyType.getAllAccessKeyName(), List.of(true)))
      {
         return (true);
      }

      boolean overridden = sessionHasValue(session, lock.getSecurityKeyType(), lock.getOverrideValues());
      FieldSecurityLock.Behavior effectiveBehavior = defaultBehavior;
      if(overridden)
      {
         effectiveBehavior = FieldSecurityLock.Behavior.ALLOW.equals(defaultBehavior) ? FieldSecurityLock.Behavior.DENY : FieldSecurityLock.Behavior.ALLOW;
      }

      return (FieldSecurityLock.Behavior.ALLOW.equals(effectiveBehavior));
   }



   /*******************************************************************************
    ** compare session key values to candidate values as strings, so that, e.g., an
    ** Integer 1 in the session matches a String "1" in a lock's overrideValues.
    *******************************************************************************/
   private static boolean sessionHasValue(QSession session, String keyName, Collection<? extends Serializable> candidateValues)
   {
      if(keyName == null || CollectionUtils.nullSafeIsEmpty(candidateValues))
      {
         return (false);
      }

      for(Serializable sessionValue : CollectionUtils.nonNullList(session.getSecurityKeyValues(keyName)))
      {
         for(Serializable candidateValue : candidateValues)
         {
            if(sessionValue != null && candidateValue != null && String.valueOf(sessionValue).equals(String.valueOf(candidateValue)))
            {
               return (true);
            }
         }
      }

      return (false);
   }



   /*******************************************************************************
    ** Get the names of the fields (and virtual fields) in the table that the
    ** current session may not access.
    *******************************************************************************/
   public static Set<String> getDeniedFieldNames(QTableMetaData table)
   {
      Set<String> rs = new LinkedHashSet<>();
      if(table == null)
      {
         return (rs);
      }

      for(QFieldMetaData field : CollectionUtils.nonNullMap(table.getFields()).values())
      {
         if(!field.getName().equals(table.getPrimaryKeyField()) && !isFieldAccessible(field))
         {
            rs.add(field.getName());
         }
      }

      if(table.getVirtualFields() != null)
      {
         for(QFieldMetaData field : table.getVirtualFields().values())
         {
            if(!isFieldAccessible(field))
            {
               rs.add(field.getName());
            }
         }
      }

      return (rs);
   }



   /*******************************************************************************
    ** Return the table with any fields denied to the current session removed (from
    ** fields, virtual fields, and sections).  Returns the input table if nothing
    ** is denied; otherwise a clone (never modifies the input table).
    *******************************************************************************/
   public static QTableMetaData removeDeniedFields(QTableMetaData table)
   {
      Set<String> deniedFieldNames = getDeniedFieldNames(table);
      if(deniedFieldNames.isEmpty())
      {
         return (table);
      }

      QTableMetaData clone = table.clone();

      Map<String, QFieldMetaData> fields = new LinkedHashMap<>(CollectionUtils.nonNullMap(clone.getFields()));
      fields.keySet().removeAll(deniedFieldNames);
      clone.setFields(fields);

      if(clone.getVirtualFields() != null)
      {
         clone.getVirtualFields().keySet().removeAll(deniedFieldNames);
      }

      if(clone.getSections() != null)
      {
         List<QFieldSection> sections = new ArrayList<>();
         for(QFieldSection section : clone.getSections())
         {
            QFieldSection sectionClone = section.clone();
            if(sectionClone.getFieldNames() != null)
            {
               sectionClone.getFieldNames().removeAll(deniedFieldNames);
            }
            sections.add(sectionClone);
         }
         clone.setSections(sections);
      }

      return (clone);
   }



   /*******************************************************************************
    ** Throw a QPermissionDeniedException if the filter (criteria - including
    ** criteria comparing to another field - and order-bys, recursively through
    ** sub-filters), or any of the additional field names (e.g., aggregates, group
    ** bys, fields to include) reference a field the current session may not
    ** access - on the base table or on any joined table.  Filtering, sorting or
    ** grouping on a denied field would leak its values indirectly.
    *******************************************************************************/
   public static void assertCriteriaAccessible(String tableName, List<QueryJoin> queryJoins, QQueryFilter filter, Collection<String> additionalFieldNames) throws QPermissionDeniedException
   {
      QInstance qInstance = QContext.getQInstance();
      if(qInstance == null || qInstance.getTable(tableName) == null)
      {
         return;
      }

      Set<String> fieldNames = new LinkedHashSet<>();
      collectFilterFieldNames(filter, fieldNames);
      if(additionalFieldNames != null)
      {
         for(String fieldName : additionalFieldNames)
         {
            CollectionUtils.addIfNotNull(fieldNames, fieldName);
         }
      }

      Map<String, String> aliasToTableName = getAliasToTableNameMap(tableName, queryJoins);
      for(String fieldName : fieldNames)
      {
         TableAndField tableAndField = resolve(tableName, aliasToTableName, fieldName);
         QTableMetaData table = qInstance.getTable(tableAndField.tableName());
         if(table == null || tableAndField.fieldName().equals(table.getPrimaryKeyField()))
         {
            continue;
         }

         QFieldMetaData field = CollectionUtils.nonNullMap(table.getFields()).get(tableAndField.fieldName());
         if(field == null && table.getVirtualFields() != null)
         {
            field = table.getVirtualFields().get(tableAndField.fieldName());
         }

         if(field != null && !isFieldAccessible(field))
         {
            throw (new QPermissionDeniedException("Permission denied."));
         }
      }
   }



   /*******************************************************************************
    ** Throw a QPermissionDeniedException if any of the records has a value
    ** (including an explicit null) for a field the current session may not access
    ** - e.g., an insert or update that would write a locked field.
    *******************************************************************************/
   public static void assertWritesAccessible(QTableMetaData table, List<QRecord> records) throws QPermissionDeniedException
   {
      Set<String> deniedFieldNames = getDeniedFieldNames(table);
      if(deniedFieldNames.isEmpty())
      {
         return;
      }

      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         if(record == null || record.getValues() == null)
         {
            continue;
         }

         for(String deniedFieldName : deniedFieldNames)
         {
            if(record.getValues().containsKey(deniedFieldName))
            {
               QFieldMetaData field = table.getFields().containsKey(deniedFieldName) ? table.getField(deniedFieldName) : null;
               String         label = field == null || field.getLabel() == null ? deniedFieldName : field.getLabel();
               throw (new QPermissionDeniedException("Permission denied: you may not write to the field [" + label + "]."));
            }
         }
      }
   }



   /*******************************************************************************
    ** Remove values (and display values) of fields denied to the current session
    ** from records of the table - including values from joined tables (keyed as
    ** joinTableOrAlias.fieldName) and associated child records, recursively.
    *******************************************************************************/
   public static void removeDeniedValues(String tableName, List<QueryJoin> queryJoins, List<QRecord> records)
   {
      QInstance qInstance = QContext.getQInstance();
      if(qInstance == null || CollectionUtils.nullSafeIsEmpty(records))
      {
         return;
      }

      QTableMetaData table = qInstance.getTable(tableName);
      if(table == null)
      {
         return;
      }

      Set<String> deniedKeys = new LinkedHashSet<>(getDeniedFieldNames(table));

      ///////////////////////////////////////////////////////////////////////////
      // a record label built from a denied field would carry its value - so  //
      // records of such a table lose their label (callers fall back to ids). //
      ///////////////////////////////////////////////////////////////////////////
      boolean clearLabels = CollectionUtils.nonNullList(table.getRecordLabelFields()).stream().anyMatch(deniedKeys::contains);
      for(Map.Entry<String, String> entry : getAliasToTableNameMap(tableName, queryJoins).entrySet())
      {
         if(entry.getKey().equals(tableName) && entry.getValue().equals(tableName))
         {
            continue;
         }

         for(String deniedFieldName : getDeniedFieldNames(qInstance.getTable(entry.getValue())))
         {
            deniedKeys.add(entry.getKey() + "." + deniedFieldName);
         }
      }

      Map<String, String> associationNameToTableName = new HashMap<>();
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         associationNameToTableName.put(association.getName(), association.getAssociatedTableName());
      }

      for(QRecord record : records)
      {
         if(record == null)
         {
            continue;
         }

         if(clearLabels)
         {
            record.setRecordLabel(null);
         }

         if(!deniedKeys.isEmpty())
         {
            if(record.getValues() != null)
            {
               record.getValues().keySet().removeAll(deniedKeys);
            }
            if(record.getDisplayValues() != null)
            {
               record.getDisplayValues().keySet().removeAll(deniedKeys);
            }
         }

         if(record.getAssociatedRecords() != null)
         {
            for(Map.Entry<String, List<QRecord>> entry : record.getAssociatedRecords().entrySet())
            {
               String associatedTableName = associationNameToTableName.get(entry.getKey());
               if(associatedTableName != null)
               {
                  removeDeniedValues(associatedTableName, null, entry.getValue());
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void collectFilterFieldNames(QQueryFilter filter, Set<String> fieldNames)
   {
      if(filter == null)
      {
         return;
      }

      for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         if(criteria != null)
         {
            CollectionUtils.addIfNotNull(fieldNames, criteria.getFieldName());
            CollectionUtils.addIfNotNull(fieldNames, criteria.getOtherFieldName());
         }
      }

      for(QFilterOrderBy orderBy : CollectionUtils.nonNullList(filter.getOrderBys()))
      {
         if(orderBy != null)
         {
            CollectionUtils.addIfNotNull(fieldNames, orderBy.getFieldName());
         }
      }

      for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         collectFilterFieldNames(subFilter, fieldNames);
      }
   }



   /*******************************************************************************
    ** map of table-name-or-alias to table name, for the base table and the joins.
    *******************************************************************************/
   private static Map<String, String> getAliasToTableNameMap(String tableName, List<QueryJoin> queryJoins)
   {
      Map<String, String> rs = new LinkedHashMap<>();
      rs.put(tableName, tableName);
      for(QueryJoin queryJoin : CollectionUtils.nonNullList(queryJoins))
      {
         if(queryJoin != null && queryJoin.getJoinTable() != null)
         {
            rs.put(queryJoin.getJoinTableOrItsAlias(), queryJoin.getJoinTable());
         }
      }
      return (rs);
   }



   /*******************************************************************************
    ** resolve a (possibly "tableOrAlias.fieldName") field name to its table name
    ** and plain field name.  Unknown prefixes are treated as table names (e.g.,
    ** for joins that are implied by the filter, not listed in the input's joins).
    *******************************************************************************/
   private static TableAndField resolve(String tableName, Map<String, String> aliasToTableName, String fieldName)
   {
      int dotIndex = fieldName.indexOf('.');
      if(dotIndex > 0)
      {
         String tableOrAlias = fieldName.substring(0, dotIndex);
         return (new TableAndField(aliasToTableName.getOrDefault(tableOrAlias, tableOrAlias), fieldName.substring(dotIndex + 1)));
      }
      return (new TableAndField(tableName, fieldName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record TableAndField(String tableName, String fieldName)
   {
   }

}
