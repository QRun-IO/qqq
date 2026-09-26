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

package com.kingsrook.qqq.backend.core.processes.utils;


import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Utility to help processes lookup records.  Caches lookups - and potentially
 ** can pre-load entire tables or subsets of tables.
 **
 *******************************************************************************/
public class RecordLookupHelper implements Serializable
{
   private Map<String, Map<Serializable, QRecord>> recordMaps;

   private Map<String, Map<Map<String, Serializable>, QRecord>> uniqueKeyMaps;

   private Set<String> preloadedKeys;

   private Set<Pair<String, String>> disallowedOneOffLookups;
   private boolean                   useSynchronizedCollections;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public RecordLookupHelper()
   {
      this(false);
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public RecordLookupHelper(boolean useSynchronizedCollections)
   {
      this.useSynchronizedCollections = useSynchronizedCollections;

      if(useSynchronizedCollections)
      {
         recordMaps = Collections.synchronizedMap(new HashMap<>());
         uniqueKeyMaps = Collections.synchronizedMap(new HashMap<>());
         preloadedKeys = Collections.synchronizedSet(new HashSet<>());
         disallowedOneOffLookups = Collections.synchronizedSet(new HashSet<>());
      }
      else
      {
         recordMaps = new HashMap<>();
         uniqueKeyMaps = new HashMap<>();
         preloadedKeys = new HashSet<>();
         disallowedOneOffLookups = new HashSet<>();
      }
   }



   /*******************************************************************************
    ** Fetch a record from a table by a uniqueKey from the table
    *******************************************************************************/
   public QRecord getRecordByUniqueKey(String tableName, Map<String, Serializable> uniqueKey) throws QException
   {
      String                                  mapKey    = tableName + "." + uniqueKey.keySet().stream().sorted().collect(Collectors.joining(","));
      Map<Map<String, Serializable>, QRecord> recordMap = uniqueKeyMaps.computeIfAbsent(mapKey, (k) -> useSynchronizedCollections ? Collections.synchronizedMap(new HashMap<>()) : new HashMap<>());

      if(!recordMap.containsKey(uniqueKey))
      {
         QRecord record = new GetAction().executeForRecord(new GetInput(tableName).withUniqueKey(uniqueKey));
         recordMap.put(uniqueKey, record);
      }

      return (recordMap.get(uniqueKey));
   }



   /*******************************************************************************
    ** Fetch a record from a table by a key field (doesn't have to be its primary key).
    *******************************************************************************/
   public QRecord getRecordByKey(String tableName, String keyFieldName, Serializable key) throws QException
   {
      String                     mapKey    = tableName + "." + keyFieldName;
      Map<Serializable, QRecord> recordMap = recordMaps.computeIfAbsent(mapKey, (k) -> useSynchronizedCollections ? Collections.synchronizedMap(new HashMap<>()) : new HashMap<>());

      ////////////////////////////////////////////////////////////
      // make sure we have they key object in the expected type //
      ////////////////////////////////////////////////////////////
      QFieldType type = QContext.getQInstance().getTable(tableName).getField(keyFieldName).getType();
      key = ValueUtils.getValueAsFieldType(type, key);

      if(!recordMap.containsKey(key))
      {
         if(disallowedOneOffLookups.isEmpty() || !disallowedOneOffLookups.contains(Pair.of(tableName, keyFieldName)))
         {
            Optional<QRecord> optRecord = GeneralProcessUtils.getRecordByField(tableName, keyFieldName, key);
            recordMap.put(key, optRecord.orElse(null));
         }
      }

      return (recordMap.get(key));
   }



   /*******************************************************************************
    ** Optimization - to pre-load the records in a single query, which would otherwise
    ** have to be looked up one-by-one.
    **
    ** Note that when this method is called for a given pair of params (table/field),
    ** a flag is set to avoid ever re-loading this pair (e.g., subsequent calls to this
    ** method w/ a given input pair does a noop).
    *******************************************************************************/
   public void preloadRecords(String tableName, String keyFieldName) throws QException
   {
      String mapKey = tableName + "." + keyFieldName;
      if(!preloadedKeys.contains(mapKey))
      {
         Map<Serializable, QRecord> recordMap = GeneralProcessUtils.loadTableToMap(tableName, keyFieldName);
         recordMaps.put(mapKey, recordMap);
         preloadedKeys.add(mapKey);
      }
   }



   /*******************************************************************************
    ** Optimization - to pre-load some records in a single query, which would otherwise
    ** have to be looked up one-by-one.
    **
    ** Note that this method is different from the overload that doesn't take a filter,
    ** in that it doesn't set any flags to avoid re-running (the idea being, you'd pass
    ** a unique filter in each time, so you'd always want it to re-run).
    *******************************************************************************/
   public void preloadRecords(String tableName, String keyFieldName, QQueryFilter filter) throws QException
   {
      String                     mapKey   = tableName + "." + keyFieldName;
      Map<Serializable, QRecord> tableMap = recordMaps.computeIfAbsent(mapKey, s -> useSynchronizedCollections ? Collections.synchronizedMap(new HashMap<>()) : new HashMap<>());
      tableMap.putAll(GeneralProcessUtils.loadTableToMap(tableName, keyFieldName, filter));
   }



   /*******************************************************************************
    ** Optimization - to pre-load some records in a single IN-LIST query,
    ** which would otherwise have to be looked up one-by-one - where - if the records
    ** aren't found, then a null will be cached (for each element in the inList).
    **
    *******************************************************************************/
   public Map<Serializable, QRecord> preloadRecords(String tableName, String keyFieldName, List<Serializable> inList) throws QException
   {
      if(CollectionUtils.nullSafeIsEmpty(inList))
      {
         return null;
      }

      String                     mapKey   = tableName + "." + keyFieldName;
      Map<Serializable, QRecord> tableMap = recordMaps.computeIfAbsent(mapKey, s -> useSynchronizedCollections ? Collections.synchronizedMap(new HashMap<>()) : new HashMap<>());

      QQueryFilter filter = new QQueryFilter(new QFilterCriteria(keyFieldName, QCriteriaOperator.IN, inList));
      tableMap.putAll(GeneralProcessUtils.loadTableToMap(tableName, keyFieldName, filter));

      QFieldType type = QContext.getQInstance().getTable(tableName).getField(keyFieldName).getType();
      for(Serializable keyValue : inList)
      {
         if(!tableMap.containsKey(keyValue))
         {
            keyValue = ValueUtils.getValueAsFieldType(type, keyValue);
            tableMap.put(keyValue, null);
         }
      }

      return (tableMap);
   }



   /*******************************************************************************
    ** Get a value from a record, by doing a lookup on the specified keyFieldName,
    ** for the specified key value.
    **
    *******************************************************************************/
   public Serializable getRecordValue(String tableName, String requestedField, String keyFieldName, Serializable key) throws QException
   {
      QRecord record = getRecordByKey(tableName, keyFieldName, key);
      if(record == null)
      {
         return (null);
      }

      return (record.getValue(requestedField));
   }



   /*******************************************************************************
    ** Get a value from a record, in the requested type, by doing a lookup on the
    ** specified keyFieldName, for the specified key value.
    **
    *******************************************************************************/
   public <T extends Serializable> T getRecordValue(String tableName, String requestedField, String keyFieldName, Serializable key, Class<T> type) throws QException
   {
      Serializable value = getRecordValue(tableName, requestedField, keyFieldName, key);
      return (ValueUtils.getValueAsType(type, value));
   }



   /*******************************************************************************
    ** Get the id (primary key) value from a record, by doing a lookup on the
    ** specified keyFieldName, for the specified key value.
    **
    *******************************************************************************/
   public Serializable getRecordId(String tableName, String keyFieldName, Serializable key) throws QException
   {
      String primaryKeyField = QContext.getQInstance().getTable(tableName).getPrimaryKeyField();
      return (getRecordValue(tableName, primaryKeyField, keyFieldName, key));
   }



   /*******************************************************************************
    ** Get the id (primary key) value from a record, in the requested type, by doing
    ** a lookup on the specified keyFieldName, for the specified key value.
    **
    *******************************************************************************/
   public <T extends Serializable> T getRecordId(String tableName, String keyFieldName, Serializable key, Class<T> type) throws QException
   {
      Serializable value = getRecordId(tableName, keyFieldName, key);
      return (ValueUtils.getValueAsType(type, value));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setMayNotDoOneOffLookups(String tableName, String fieldName)
   {
      this.disallowedOneOffLookups.add(Pair.of(tableName, fieldName));
   }

}
