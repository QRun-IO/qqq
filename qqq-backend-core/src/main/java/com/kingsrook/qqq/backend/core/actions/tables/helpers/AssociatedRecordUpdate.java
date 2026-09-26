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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Private old and prospective relationship tuples for replacement updates.
 ** Stored values never hydrate the parent patch or its public response.
 *******************************************************************************/
public class AssociatedRecordUpdate
{
   /*******************************************************************************
    ** Existing membership uses stored tuples; assignments use normalized patch overlays.
    ** Primary-key-only relationships need no additional native read.
    *******************************************************************************/
   public static Map<QRecord, Values> prepare(QTableMetaData activeTable, List<QRecord> records, QBackendTransaction transaction) throws QException
   {
      Map<QRecord, Values> result = new IdentityHashMap<>();
      if(CollectionUtils.nullSafeIsEmpty(records) || CollectionUtils.nullSafeIsEmpty(activeTable.getAssociations())
         || records.stream().noneMatch(record -> record.getAssociatedRecords() != null
            && activeTable.getAssociations().stream().anyMatch(association -> record.getAssociatedRecords().containsKey(association.getName()))))
      {
         return result;
      }
      QTableMetaData table = AssociatedRecordDiscovery.physicalParentTable(activeTable);
      String primaryKey = table.getPrimaryKeyField();
      Map<QRecord, Set<String>> fieldsByRecord = new IdentityHashMap<>();
      Map<Object, List<QRecord>> recordsByKey = new HashMap<>();
      Set<Association> requestedAssociations = new LinkedHashSet<>();
      for(QRecord record : records)
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            continue;
         }
         Object key = primaryKey(table, record);
         if(key != null)
         {
            recordsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
         }
         for(Association association : activeTable.getAssociations())
         {
            if(record.getAssociatedRecords() == null || !record.getAssociatedRecords().containsKey(association.getName()))
            {
               continue;
            }
            Set<String> fields = fieldsByRecord.computeIfAbsent(record, ignored -> new LinkedHashSet<>(Set.of(primaryKey)));
            for(JoinOn joinOn : AssociationJoin.resolve(table, association).getJoinOns())
            {
               fields.add(joinOn.getLeftField());
            }
            requestedAssociations.add(association);
         }
      }
      for(List<QRecord> duplicates : recordsByKey.values())
      {
         if(duplicates.size() > 1 && duplicates.stream().anyMatch(fieldsByRecord::containsKey))
         {
            duplicates.forEach(record -> record.addError(new BadInputStatusMessage("A parent may appear only once in an association replacement update")));
         }
      }

      List<Serializable> keysToRead = new ArrayList<>();
      for(Map.Entry<QRecord, Set<String>> entry : fieldsByRecord.entrySet())
      {
         if(CollectionUtils.nullSafeIsEmpty(entry.getKey().getErrors()) && entry.getKey().getValue(primaryKey) != null && entry.getValue().stream().anyMatch(field -> !field.equals(primaryKey)))
         {
            keysToRead.add(entry.getKey().getValue(primaryKey));
         }
      }
      Map<Object, QRecord> storedByKey = new HashMap<>();
      if(!keysToRead.isEmpty())
      {
         for(QRecord stored : AssociatedRecordDiscovery.readParentValues(table, new ArrayList<>(requestedAssociations), keysToRead, transaction))
         {
            storedByKey.put(primaryKey(table, stored), stored);
         }
      }
      for(Map.Entry<QRecord, Set<String>> entry : fieldsByRecord.entrySet())
      {
         QRecord record = entry.getKey();
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            continue;
         }
         Object key = primaryKey(table, record);
         if(key == null)
         {
            record.addError(new BadInputStatusMessage("Missing primary key for association replacement"));
            continue;
         }
         QRecord stored = entry.getValue().size() == 1 ? new QRecord().withValue(primaryKey, record.getValue(primaryKey)) : storedByKey.get(key);
         if(stored == null)
         {
            record.addError(new NotFoundStatusMessage("No parent record was found for the association update"));
            continue;
         }
         QRecord before = new QRecord();
         QRecord after = new QRecord();
         Set<String> storedFields = new LinkedHashSet<>();
         for(String field : entry.getValue())
         {
            before.setValue(field, stored.getValue(field));
            boolean supplied = activeTable.getFields().containsKey(field) && record.getValues().containsKey(field);
            after.setValue(field, supplied ? record.getValue(field) : stored.getValue(field));
            if(!supplied)
            {
               storedFields.add(field);
            }
         }
         for(Association association : activeTable.getAssociations())
         {
            if(record.getAssociatedRecords().containsKey(association.getName())
               && CollectionUtils.nullSafeHasContents(record.getAssociatedRecords().get(association.getName()))
               && AssociationJoin.resolve(table, association).parentValues(after).contains(null))
            {
               record.addError(new BadInputStatusMessage("Cannot write children for association [" + association.getName() + "] with a null parent relationship value"));
            }
         }
         if(CollectionUtils.nullSafeIsEmpty(record.getErrors()))
         {
            result.put(record, new Values(before, after, storedFields));
         }
      }
      return result;
   }



   /*******************************************************************************
    ** Typed Java bookkeeping for exact target identifiers, including numeric scale.
    *******************************************************************************/
   public static Object primaryKey(QTableMetaData table, QRecord record) throws QException
   {
      try
      {
         return UniqueKeyLookup.equalityValue(record.resolvePrimaryKey(table));
      }
      catch(RuntimeException e)
      {
         throw new QException("Invalid primary key for association replacement", e);
      }
   }



   /*******************************************************************************
    ** Private snapshots, with the fields that were not supplied by the caller.
    *******************************************************************************/
   public record Values(QRecord before, QRecord after, Set<String> storedFields)
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      public Values
      {
         before = new QRecord(before);
         before.getValues().replaceAll((field, value) -> UniqueKeyLookup.snapshot(value));
         after = new QRecord(after);
         after.getValues().replaceAll((field, value) -> UniqueKeyLookup.snapshot(value));
         storedFields = Set.copyOf(storedFields);
      }
   }
}
