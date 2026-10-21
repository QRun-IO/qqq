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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.RecordVersionConflictError;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Best-effort optimistic preflight using the already permission-filtered old rows.
 *******************************************************************************/
public class RecordVersionCheck
{
   /*******************************************************************************
    ** A configured version must change on every ordinary write, even without a guard.
    *******************************************************************************/
   public static void advance(UpdateInput input, List<QRecord> oldRecords) throws com.kingsrook.qqq.backend.core.exceptions.QException
   {
      var table = input.getTable();
      String name = table.getRecordVersionField();
      if(name == null || name.isEmpty() || input.getRecords() == null || input.getRecords().isEmpty())
      {
         return;
      }
      var field = table.getField(name);
      if(input.getOmitModifyDateUpdate() && DynamicDefaultValueBehavior.MODIFY_DATE.equals(field.getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class)))
      {
         return;
      }
      Map<String, QRecord> previous = new LinkedHashMap<>();
      oldRecords.forEach(row -> previous.put(ValueUtils.getValueAsString(row.getValue(table.getPrimaryKeyField())), row));
      for(QRecord record : input.getRecords())
      {
         if(record.getErrors() != null && !record.getErrors().isEmpty())
         {
            continue;
         }
         var old = previous.get(ValueUtils.getValueAsString(record.getValue(table.getPrimaryKeyField())));
         if(old == null)
         {
            continue;
         }
         Serializable value = old.getValue(name);
         Serializable next = switch(field.getType())
         {
            case INTEGER -> value == null ? 1 : Math.addExact(((Number) value).intValue(), 1);
            case LONG -> value == null ? 1L : Math.addExact(((Number) value).longValue(), 1L);
            case STRING -> java.util.UUID.randomUUID().toString();
            case DATE_TIME ->
            {
               var now = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
               var prior = value == null ? java.time.Instant.MIN : (java.time.Instant) ValueUtils.getValueAsFieldType(table, field, value);
               yield now.isAfter(prior) ? now : prior.truncatedTo(java.time.temporal.ChronoUnit.MILLIS).plusMillis(1);
            }
            default -> throw new com.kingsrook.qqq.backend.core.exceptions.QBadRequestException("Record version fields must be INTEGER, LONG, STRING or DATE_TIME.");
         };
         record.setValue(name, next);
      }
   }

   /*******************************************************************************
    ** Compare typed versions before defaults change the caller supplied values.
    *******************************************************************************/
   public static void check(UpdateInput input, List<QRecord> oldRecords)
   {
      QTableMetaData table = input.getTable();
      String versionField = table.getRecordVersionField();
      if(input.getExpectedVersions() == null || versionField == null || versionField.isEmpty())
      {
         return;
      }
      Map<Object, Serializable> expected = normalizedExpectedVersions(input);
      Map<Object, QRecord> oldByKey = new LinkedHashMap<>();
      oldRecords.forEach(record -> oldByKey.put(primaryKeyIdentity(table, record.getValue(table.getPrimaryKeyField())), record));
      for(QRecord record : input.getRecords())
      {
         Object key = primaryKeyIdentity(table, record.getValue(table.getPrimaryKeyField()));
         QRecord old = oldByKey.get(key);
         if(old == null || !expected.containsKey(key))
         {
            continue;
         }
         QFieldMetaData field = table.getField(versionField);
         Serializable current = com.kingsrook.qqq.backend.core.utils.JsonFieldUtils.read(old, field, "");
         Serializable requested = field == null ? expected.get(key) : ValueUtils.getValueAsFieldType(table, field, expected.get(key));
         if(!Objects.equals(current, requested))
         {
            List<String> changed = record.getValues().keySet().stream()
               .filter(name -> !name.equals(table.getPrimaryKeyField()) && !Objects.deepEquals(old.getValue(name), record.getValue(name)))
               .toList();
            Serializable modifiedBy = table.getFields().containsKey("modifiedByUserId") ? old.getValue("modifiedByUserId") : old.getValue("modifiedBy");
            String modifyDate = table.getFields().values().stream()
               .filter(candidate -> DynamicDefaultValueBehavior.MODIFY_DATE.equals(candidate.getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class)))
               .map(QFieldMetaData::getName).findFirst().orElse(null);
            record.addError(new RecordVersionConflictError(current, modifiedBy, modifyDate == null ? null : old.getValue(modifyDate), changed));
         }
      }
   }

   /*******************************************************************************
    ** Check one stored row while its backend write lock is held.
    *******************************************************************************/
   public static void checkAtWrite(UpdateInput input, QRecord record, QRecord stored)
   {
      if(input.getExpectedVersions() == null || input.getTable().getRecordVersionField() == null || input.getTable().getRecordVersionField().isEmpty())
      {
         return;
      }
      Object key = primaryKeyIdentity(input.getTable(), record.getValue(input.getTable().getPrimaryKeyField()));
      if(!normalizedExpectedVersions(input).containsKey(key))
      {
         return;
      }
      if(stored == null)
      {
         record.addError(new RecordVersionConflictError(null, null, null, List.of()));
         return;
      }
      UpdateInput single = new UpdateInput(input.getTableName()).withRecord(record).withExpectedVersions(input.getExpectedVersions());
      single.setTableMetaData(input.getTable());
      check(single, List.of(stored));
   }

   /*******************************************************************************
    ** Use backend field types for both request keys and persisted primary keys.
    *******************************************************************************/
   public static Object primaryKeyIdentity(QTableMetaData table, Serializable value)
   {
      return (UniqueKeyLookup.equalityValue(ValueUtils.getValueAsFieldType(table, table.getField(table.getPrimaryKeyField()), value)));
   }

   /*******************************************************************************
    ** Reject conflicting aliases instead of selecting a guard by map iteration order.
    *******************************************************************************/
   public static Map<Object, Serializable> normalizedExpectedVersions(UpdateInput input)
   {
      Map<Object, Serializable> result = new LinkedHashMap<>();
      QTableMetaData table = input.getTable();
      QFieldMetaData version = table.getField(table.getRecordVersionField());
      input.getExpectedVersions().forEach((key, value) ->
      {
         Object identity = primaryKeyIdentity(table, key);
         Serializable typedVersion = ValueUtils.getValueAsFieldType(table, version, value);
         if(result.containsKey(identity) && !Objects.equals(result.get(identity), typedVersion))
         {
            throw (new com.kingsrook.qqq.backend.core.exceptions.QValueException("Conflicting expected versions for the same primary key"));
         }
         result.put(identity, typedVersion);
      });
      return (result);
   }
}

