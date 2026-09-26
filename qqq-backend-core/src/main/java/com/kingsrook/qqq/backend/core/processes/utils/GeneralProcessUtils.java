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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Utility methods for working with QQQ records and table actions inside user -
 ** defined QQQ processes steps.
 *******************************************************************************/
public class GeneralProcessUtils
{

   /*******************************************************************************
    ** For a list of sourceRecords,
    ** lookup records in the foreignTableName,
    ** that have their foreignTablePrimaryKeyName in the sourceTableForeignKeyFieldName on the sourceRecords.
    **
    ** e.g., for a list of orders (with a clientId field), build a map of client.id => client record
    ** via getForeignRecordMap(input, orderList, "clientId", "client", "id")
    *******************************************************************************/
   public static Map<Serializable, QRecord> getForeignRecordMap(List<QRecord> sourceRecords, String sourceTableForeignKeyFieldName, String foreignTableName, String foreignTablePrimaryKeyName) throws QException
   {
      return getForeignRecordMap(sourceRecords, sourceTableForeignKeyFieldName, foreignTableName, foreignTablePrimaryKeyName, new QQueryFilter());
   }

   // todo not commit - clean up all method sigs



   /*******************************************************************************
    ** For a list of sourceRecords,
    ** lookup records in the foreignTableName,
    ** that have their foreignTablePrimaryKeyName in the sourceTableForeignKeyFieldName on the sourceRecords.
    **
    ** e.g., for a list of orders (with a clientId field), build a map of client.id => client record
    ** via getForeignRecordMap(input, orderList, "clientId", "client", "id")
    *******************************************************************************/
   public static Map<Serializable, QRecord> getForeignRecordMap(List<QRecord> sourceRecords, String sourceTableForeignKeyFieldName, String foreignTableName, String foreignTablePrimaryKeyName, QQueryFilter additionalFilter) throws QException
   {
      Map<Serializable, QRecord> foreignRecordMap = new HashMap<>();
      QueryInput                 queryInput       = new QueryInput();
      queryInput.setTableName(foreignTableName);
      List<Serializable> foreignIds = new ArrayList<>(sourceRecords.stream().map(r -> r.getValue(sourceTableForeignKeyFieldName)).toList());

      additionalFilter.addCriteria(new QFilterCriteria(foreignTablePrimaryKeyName, QCriteriaOperator.IN, foreignIds));
      queryInput.setFilter(additionalFilter);

      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      for(QRecord foreignRecord : queryOutput.getRecords())
      {
         foreignRecordMap.put(foreignRecord.getValue(foreignTablePrimaryKeyName), foreignRecord);
      }
      return foreignRecordMap;
   }



   /*******************************************************************************
    ** For a list of sourceRecords,
    ** lookup records in the foreignTableName,
    ** that have their foreignTableForeignKeyName in the sourceTableForeignKeyFieldName on the sourceRecords.
    **
    ** e.g., for a list of orders, build a ListingHash of order.id => List(OrderLine records)
    ** via getForeignRecordListingHashMap(input, orderList, "id", "orderLine", "orderId")
    *******************************************************************************/
   public static ListingHash<Serializable, QRecord> getForeignRecordListingHashMap(List<QRecord> sourceRecords, String sourceTableForeignKeyFieldName, String foreignTableName, String foreignTableForeignKeyName) throws QException
   {
      ListingHash<Serializable, QRecord> foreignRecordMap = new ListingHash<>();
      QueryInput                         queryInput       = new QueryInput();
      queryInput.setTableName(foreignTableName);
      List<Serializable> foreignIds = new ArrayList<>(sourceRecords.stream().map(r -> r.getValue(sourceTableForeignKeyFieldName)).toList());

      queryInput.setFilter(new QQueryFilter().withCriteria(new QFilterCriteria(foreignTableForeignKeyName, QCriteriaOperator.IN, foreignIds)));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      for(QRecord foreignRecord : queryOutput.getRecords())
      {
         foreignRecordMap.add(foreignRecord.getValue(foreignTableForeignKeyName), foreignRecord);
      }
      return foreignRecordMap;
   }



   /*******************************************************************************
    ** For a list of sourceRecords,
    ** lookup records in the foreignTableName,
    ** that have their foreignTablePrimaryKeyName in the sourceTableForeignKeyFieldName on the sourceRecords.
    ** and set those foreign records as a value in the sourceRecords.
    **
    ** e.g., for a list of orders (with a clientId field), setValue("client", QRecord(client));
    ** via addForeignRecordsToRecordList(input, orderList, "clientId", "client", "id")
    *******************************************************************************/
   public static void addForeignRecordsToRecordList(List<QRecord> sourceRecords, String sourceTableForeignKeyFieldName, String foreignTableName, String foreignTablePrimaryKeyName) throws QException
   {
      Map<Serializable, QRecord> foreignRecordMap = getForeignRecordMap(sourceRecords, sourceTableForeignKeyFieldName, foreignTableName, foreignTablePrimaryKeyName);
      for(QRecord sourceRecord : sourceRecords)
      {
         QRecord foreignRecord = foreignRecordMap.get(sourceRecord.getValue(sourceTableForeignKeyFieldName));
         sourceRecord.setValue(foreignTableName, foreignRecord);
      }
   }



   /*******************************************************************************
    ** For a list of sourceRecords,
    ** lookup records in the foreignTableName,
    ** that have their foreignTableForeignKeyName in the sourceTableForeignKeyFieldName on the sourceRecords.
    **
    ** e.g., for a list of orders, setValue("orderLine", List(QRecord(orderLine)))
    ** via addForeignRecordsListToRecordList(input, orderList, "id", "orderLine", "orderId")
    *******************************************************************************/
   public static void addForeignRecordsListToRecordList(List<QRecord> sourceRecords, String sourceTableForeignKeyFieldName, String foreignTableName, String foreignTableForeignKeyName) throws QException
   {
      ListingHash<Serializable, QRecord> foreignRecordMap = getForeignRecordListingHashMap(sourceRecords, sourceTableForeignKeyFieldName, foreignTableName, foreignTableForeignKeyName);
      for(QRecord sourceRecord : sourceRecords)
      {
         List<QRecord> foreignRecordList = foreignRecordMap.get(sourceRecord.getValue(sourceTableForeignKeyFieldName));
         if(foreignRecordList != null)
         {
            if(foreignRecordList instanceof Serializable s)
            {
               sourceRecord.setValue(foreignTableName, s);
            }
            else
            {
               sourceRecord.setValue(foreignTableName, new ArrayList<>(foreignRecordList));
            }
         }
      }
   }



   /*******************************************************************************
    ** Run a query on tableName, for where fieldName equals fieldValue, and return
    ** the list of QRecords.
    *******************************************************************************/
   public static List<QRecord> getRecordListByField(String tableName, String fieldName, Serializable fieldValue) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setFilter(new QQueryFilter().withCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, List.of(fieldValue))));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      return (queryOutput.getRecords());
   }



   /*******************************************************************************
    ** Query to get one record by a unique key value.  That field can be the primary
    ** key, or any other field on the table.  Note, if multiple rows do match the value,
    ** only 1 (determined in an unspecified way) is returned.
    *******************************************************************************/
   public static Optional<QRecord> getRecordByField(String tableName, String fieldName, Serializable fieldValue) throws QException
   {
      if(fieldValue == null)
      {
         return (Optional.empty());
      }

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setFilter(new QQueryFilter().withCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, fieldValue)).withLimit(1));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      return (queryOutput.getRecords().stream().findFirst());
   }



   /*******************************************************************************
    ** Query to get one entity by a unique key value.  That field can be the primary
    ** key, or any other field on the table.  Note, if multiple rows do match the value,
    ** only 1 (determined in an unspecified way) is returned.
    *******************************************************************************/
   public static <T extends QRecordEntity> Optional<T> getEntityByField(String tableName, String fieldName, Serializable fieldValue, Class<T> entityClass) throws QException
   {
      Optional<QRecord> optionalQRecord = getRecordByField(tableName, fieldName, fieldValue);
      if(optionalQRecord.isPresent())
      {
         return (Optional.of(QRecordEntity.fromQRecord(entityClass, optionalQRecord.get())));
      }
      return (Optional.empty());
   }



   /*******************************************************************************
    ** Query to get one record by a unique key value.
    *******************************************************************************/
   public static QRecord getRecordByFieldOrElseThrow(String tableName, String fieldName, Serializable fieldValue) throws QException
   {
      return getRecordByField(tableName, fieldName, fieldValue)
         .orElseThrow(() -> new QException(tableName + " with " + fieldName + " of " + fieldValue + " was not found."));
   }



   /*******************************************************************************
    ** Query to get one record by its primary key value.
    *******************************************************************************/
   public static Optional<QRecord> getRecordByPrimaryKey(String tableName, Serializable value) throws QException
   {
      GetInput getInput = new GetInput();
      getInput.setTableName(tableName);
      getInput.setPrimaryKey(value);
      GetOutput getOutput = new GetAction().execute(getInput);
      return (Optional.ofNullable(getOutput.getRecord()));
   }



   /*******************************************************************************
    ** Query to get one record by its primary key value.
    *******************************************************************************/
   public static QRecord getRecordByPrimaryKeyOrElseThrow(String tableName, Serializable value) throws QException
   {
      return getRecordByPrimaryKey(tableName, value)
         .orElseThrow(() -> new QException(tableName + " with primary key of " + value + " was not found."));
   }



   /*******************************************************************************
    ** Load all rows from a table.
    **
    ** Note, this is inherently unsafe, if you were to call it on a table with
    ** too many rows...  Caveat emptor.
    *******************************************************************************/
   public static List<QRecord> loadTable(String tableName) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      return (queryOutput.getRecords());
   }



   /*******************************************************************************
    ** Load all rows from a table as a RecordEntity.
    **
    ** Note, this is inherently unsafe, if you were to call it on a table with
    ** too many rows...  Caveat emptor.
    *******************************************************************************/
   public static <T extends QRecordEntity> List<T> loadTable(String tableName, Class<T> entityClass) throws QException
   {
      return (loadTable(tableName, entityClass, null));
   }



   /*******************************************************************************
    ** Load all rows from a table as a RecordEntity, takes in a filter as well
    **
    ** Note, this is inherently unsafe, if you were to call it on a table with
    ** too many rows...  Caveat emptor.
    *******************************************************************************/
   public static <T extends QRecordEntity> List<T> loadTable(String tableName, Class<T> entityClass, QQueryFilter filter) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      if(filter != null)
      {
         queryInput.setFilter(filter);
      }
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      List<T> rs = new ArrayList<>();
      for(QRecord record : queryOutput.getRecords())
      {
         rs.add(QRecordEntity.fromQRecord(entityClass, record));
      }

      return (rs);
   }



   /*******************************************************************************
    ** Load all rows from a table, into a map, keyed by the keyFieldName.
    **
    ** Note - null values from the key field are NOT put in the map.
    **
    ** If multiple values are found for the key, they'll squash each other, and only
    ** one random value will appear.
    **
    ** Also, note, this is inherently unsafe, if you were to call it on a table with
    ** too many rows...  Caveat emptor.
    *******************************************************************************/
   public static Map<Serializable, QRecord> loadTableToMap(String tableName, String keyFieldName) throws QException
   {
      return (loadTableToMap(tableName, keyFieldName, (QQueryFilter) null));
   }



   /*******************************************************************************
    ** Load rows from a table matching the specified filter, into a map, keyed by the keyFieldName.
    **
    ** Note - null values from the key field are NOT put in the map.
    **
    ** If multiple values are found for the key, they'll squash each other, and only
    ** one (random) value will appear.
    *******************************************************************************/
   public static <T extends QRecordEntity> Map<Serializable, T> loadTableToMap(String tableName, String keyFieldName, Class<T> entityClass, QQueryFilter filter) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setFilter(filter);
      QueryOutput   queryOutput = new QueryAction().execute(queryInput);
      List<QRecord> records     = queryOutput.getRecords();

      Map<Serializable, T> map = new HashMap<>();
      for(QRecord record : records)
      {
         Serializable value = record.getValue(keyFieldName);
         if(value != null)
         {
            map.put(value, QRecordEntity.fromQRecord(entityClass, record));
         }
      }
      return (map);
   }



   /*******************************************************************************
    ** Load rows from a table matching the specified filter, into a map, keyed by the keyFieldName.
    **
    ** Note - null values from the key field are NOT put in the map.
    **
    ** If multiple values are found for the key, they'll squash each other, and only
    ** one (random) value will appear.
    *******************************************************************************/
   public static Map<Serializable, QRecord> loadTableToMap(String tableName, String keyFieldName, QQueryFilter filter) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setFilter(filter);
      QueryOutput   queryOutput = new QueryAction().execute(queryInput);
      List<QRecord> records     = queryOutput.getRecords();

      Map<Serializable, QRecord> map = new HashMap<>();
      for(QRecord record : records)
      {
         Serializable value = record.getValue(keyFieldName);
         if(value != null)
         {
            map.put(value, record);
         }
      }
      return (map);
   }



   /*******************************************************************************
    ** Load all rows from a table, into a map, keyed by the keyFieldName - typed as
    ** the specified keyType.
    **
    ** Note - null values from the key field are NOT put in the map.
    **
    ** If multiple values are found for the key, they'll squash each other, and only
    ** one random value will appear.
    **
    ** Also, note, this is inherently unsafe, if you were to call it on a table with
    ** too many rows...  Caveat emptor.
    *******************************************************************************/
   public static <T extends Serializable> Map<T, QRecord> loadTableToMap(String tableName, Class<T> keyType, String keyFieldName) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      QueryOutput   queryOutput = new QueryAction().execute(queryInput);
      List<QRecord> records     = queryOutput.getRecords();

      Map<T, QRecord> map = new HashMap<>();
      for(QRecord record : records)
      {
         Serializable value = record.getValue(keyFieldName);
         if(value != null)
         {
            T valueAsT = ValueUtils.getValueAsType(keyType, value);
            map.put(valueAsT, record);
         }
      }
      return (map);
   }



   /*******************************************************************************
    ** Note - null values from the key field are NOT put in the map.
    *******************************************************************************/
   public static <T extends QRecordEntity> Map<Serializable, T> loadTableToMap(String tableName, String keyFieldName, Class<T> entityClass) throws QException
   {
      return (loadTableToMap(tableName, keyFieldName, entityClass, (Consumer<QueryInput>) null));
   }



   /*******************************************************************************
    ** Note - null values from the key field are NOT put in the map.
    *******************************************************************************/
   public static <T extends QRecordEntity> Map<Serializable, T> loadTableToMap(String tableName, String keyFieldName, Class<T> entityClass, Consumer<QueryInput> queryInputCustomizer) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);

      if(queryInputCustomizer != null)
      {
         queryInputCustomizer.accept(queryInput);
      }

      QueryOutput   queryOutput = new QueryAction().execute(queryInput);
      List<QRecord> records     = queryOutput.getRecords();

      Map<Serializable, T> map = new HashMap<>();
      for(QRecord record : records)
      {
         Serializable value = record.getValue(keyFieldName);
         if(value != null)
         {
            map.put(value, QRecordEntity.fromQRecord(entityClass, record));
         }
      }
      return (map);
   }



   /*******************************************************************************
    ** Load all rows from a table, into a ListingHash, keyed by the keyFieldName.
    **
    ** Note - null values from the key field are NOT put in the map.
    **
    ** The ordering of the records is not specified.
    **
    ** Also, note, this is inherently unsafe, if you were to call it on a table with
    ** too many rows...  Caveat emptor.
    *******************************************************************************/
   public static ListingHash<Serializable, QRecord> loadTableToListingHash(String tableName, String keyFieldName) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      QueryOutput   queryOutput = new QueryAction().execute(queryInput);
      List<QRecord> records     = queryOutput.getRecords();

      ListingHash<Serializable, QRecord> map = new ListingHash<>();
      for(QRecord record : records)
      {
         Serializable value = record.getValue(keyFieldName);
         if(value != null)
         {
            map.add(value, record);
         }
      }
      return (map);
   }



   /*******************************************************************************
    ** Ensure that a process has been initiated with a single record as input - and
    ** get that record id.
    **
    *******************************************************************************/
   public static Integer validateSingleSelectedId(RunBackendStepInput runBackendStepInput, String tableName) throws QException
   {
      String tableLabel = QContext.getQInstance().getTable(tableName).getLabel();

      ////////////////////////////////////////////////////
      // Get the selected recordId and verify we only 1 //
      ////////////////////////////////////////////////////
      String recordIds = (String) runBackendStepInput.getValue("recordIds");
      if(!StringUtils.hasContent(recordIds))
      {
         throw new QUserFacingException("Select a " + tableLabel + " to process.");
      }

      String[] idStrings = recordIds.split(",");
      if(idStrings.length > 1)
      {
         throw new QUserFacingException("Select a single " + tableLabel + " to process.");
      }

      return (Integer.parseInt(idStrings[0]));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <T extends QRecordEntity> List<T> recordsToEntities(Class<T> recordEntityClass, List<QRecord> records) throws QException
   {
      if(records == null)
      {
         return (null);
      }

      List<T> rs = new ArrayList<>();
      for(QRecord record : records)
      {
         rs.add(QRecordEntity.fromQRecord(recordEntityClass, record));
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Integer count(String tableName, QQueryFilter filter) throws QException
   {
      CountInput countInput = new CountInput();
      countInput.setTableName(tableName);
      countInput.setFilter(filter);
      CountOutput countOutput = new CountAction().execute(countInput);
      return (countOutput.getCount());

   }
}
