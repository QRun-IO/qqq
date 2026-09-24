/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.modules.backend.implementations.memory;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.DateTimeGroupBy;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordUpdate;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyLookup;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAndJoinTable;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeRegistry;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsUtil;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.utils.BackendQueryFilterUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Storage provider for the MemoryBackendModule
 *******************************************************************************/
public class MemoryRecordStore
{
   private static final QLogger LOG = QLogger.getLogger(MemoryRecordStore.class);

   private static MemoryRecordStore instance;

   //////////////////////////////////////////////////////////
   // these maps are: BackendIdentifier > tableName > data //
   //////////////////////////////////////////////////////////
   private Map<BackendIdentifier, Map<String, Map<Serializable, QRecord>>> data;
   private Map<BackendIdentifier, Map<String, Integer>>                    nextSerials;

   private static boolean collectStatistics = false;

   public static final String STAT_QUERIES_RAN = "queriesRan";
   public static final String STAT_INSERTS_RAN = "insertsRan";

   private static final Map<String, Integer> statistics = Collections.synchronizedMap(new HashMap<>());

   public static final ListingHash<Class<? extends AbstractActionInput>, AbstractActionInput> actionInputs = new ListingHash<>();

   ////////////////////////////////////////////////////////////////////////
   // this flag controls whether MemoryRecordStore builds its join cross //
   // product from the JoinsContext's query joins (which includes joins  //
   // needed for security) or from the QueryInput's query joins (which   //
   // only has explicitly-requested joins).  Originally defaulted to     //
   // false while we gained confidence; now defaults to true.  If a test //
   // needs the old behavior, it can set this to false on the singleton. //
   ////////////////////////////////////////////////////////////////////////
   public static boolean BUILD_JOIN_CROSS_PRODUCT_FROM_JOIN_CONTEXT_DEFAULT = true;
   private       boolean buildJoinCrossProductFromJoinContext               = BUILD_JOIN_CROSS_PRODUCT_FROM_JOIN_CONTEXT_DEFAULT;


   /*******************************************************************************
    ** private singleton constructor
    *******************************************************************************/
   private MemoryRecordStore()
   {
      data = Collections.synchronizedMap(new HashMap<>());
      nextSerials = Collections.synchronizedMap(new HashMap<>());
   }



   /*******************************************************************************
    ** forget all data AND statistics
    *******************************************************************************/
   public static void fullReset()
   {
      getInstance().reset();
      resetStatistics();
      setCollectStatistics(false);
   }



   /*******************************************************************************
    ** Forget all data in the memory store...
    *******************************************************************************/
   public void reset()
   {
      data.clear();
      nextSerials.clear();
   }



   /*******************************************************************************
    ** singleton accessor
    *******************************************************************************/
   public static MemoryRecordStore getInstance()
   {
      if(instance == null)
      {
         instance = new MemoryRecordStore();
      }
      return (instance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<Serializable, QRecord> getTableData(QTableMetaData table) throws QException
   {
      BackendIdentifier                       backendIdentifier = getBackendIdentifier(table);
      Map<String, Map<Serializable, QRecord>> dataForBackend = data.computeIfAbsent(backendIdentifier, k -> Collections.synchronizedMap(new HashMap<>()));
      return (dataForBackend.computeIfAbsent(table.getName(), k -> Collections.synchronizedMap(new HashMap<>())));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private BackendIdentifier getBackendIdentifier(QTableMetaData table) throws QException
   {
      BackendIdentifier     backendIdentifier     = NonVariant.getInstance();
      QBackendMetaData      backendMetaData       = QContext.getQInstance().getBackend(table.getBackendName());
      BackendVariantsConfig backendVariantsConfig = backendMetaData.getBackendVariantsConfig();
      if(backendVariantsConfig != null)
      {
         String       variantType   = backendMetaData.getBackendVariantsConfig().getVariantTypeKey();
         QRecord      variantRecord = BackendVariantsUtil.getVariantRecord(backendMetaData);
         Serializable variantId     = variantRecord.getValue(QContext.getQInstance().getTable(variantRecord.getTableName()).getPrimaryKeyField());
         backendIdentifier = new Variant(variantType, variantId);
      }
      return backendIdentifier;
   }



   /*******************************************************************************
    ** Inspect only structural relationship values; never expose whole stored rows.
    *******************************************************************************/
   public List<Serializable> findAssociatedPrimaryKeys(AssociatedRecordDiscovery.Input input) throws QException
   {
      QueryInput queryInput = input.newQueryInput();
      List<Serializable> keys = new ArrayList<>();
      for(QRecord record : getTableData(queryInput.getTable()).values())
      {
         if(BackendQueryFilterUtils.doesRecordMatch(queryInput.getFilter(), record))
         {
            keys.add(record.getValue(queryInput.getTable().getPrimaryKeyField()));
         }
      }
      return keys;
   }



   /*******************************************************************************
    ** Return only declared parent relationship values without read transformations.
    *******************************************************************************/
   public List<QRecord> readAssociationValues(AssociatedRecordDiscovery.StoredValuesInput input) throws QException
   {
      QueryInput query = input.newQueryInput();
      List<QRecord> result = new ArrayList<>();
      for(QRecord record : getTableData(query.getTable()).values())
      {
         if(input.matches(record))
         {
            QRecord projected = new QRecord();
            query.getFieldNamesToInclude().forEach(field -> projected.setValue(field, record.getValue(field)));
            result.add(projected);
         }
      }
      return result;
   }



   /*******************************************************************************
    ** Return only schema-constrained key material, without read transformations.
    *******************************************************************************/
   public List<QRecord> lookupUniqueKey(UniqueKeyLookup.Input input) throws QException
   {
      QueryInput query = input.newQueryInput();
      List<QRecord> result = new ArrayList<>();
      for(QRecord record : getTableData(query.getTable()).values())
      {
         if(input.matches(record))
         {
            QRecord projected = new QRecord();
            query.getFieldNamesToInclude().forEach(field -> projected.setValue(field, record.getValue(field)));
            result.add(projected);
         }
      }
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<QRecord> query(QueryInput input) throws QException
   {
      if(!buildJoinCrossProductFromJoinContext)
      {
         throw (new QException("Memory query requires security-aware join resolution"));
      }

      incrementStatistic(input);

      Collection<QRecord> tableData = getTableData(input.getTable()).values();
      List<QRecord>       records   = new ArrayList<>();

      QQueryFilter filter       = clonedOrNewFilter(input.getFilter());
      JoinsContext joinsContext = new JoinsContext(QContext.getQInstance(), input, filter);

      List<QueryJoin> queryJoins = joinsContext.getQueryJoins();

      ///////////////////////////////////////////////////////////////////////////////////////////
      // if there are query joins, then use the cross product of those joins as the table data //
      ///////////////////////////////////////////////////////////////////////////////////////////
      if(CollectionUtils.nullSafeHasContents(queryJoins))
      {
         tableData = buildJoinCrossProduct(input.getTable(), queryJoins, joinsContext);
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // in cae table personalization is being used, build a map of all join tables in the query and their //
      // active/personalized meta data - to be later used in stripping fields that aren't in the tables.   //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      Map<String, QTableMetaData> personalizedTables = new HashMap<>();
      personalizedTables.put(input.getTableName(), input.getTableMetaData());
      for(QueryJoin queryJoin : CollectionUtils.nonNullList(queryJoins))
      {
         QTableMetaData joinTable = QContext.getQInstance().getTable(queryJoin.getJoinTable());
         joinTable = TableMetaDataPersonalizerAction.execute(new TableMetaDataPersonalizerInput().withTableMetaData(joinTable).withInputSource(input.getInputSource()));
         if(joinTable == null)
         {
            throw (new QException("Query join table is not available"));
         }
         personalizedTables.put(joinTable.getName(), joinTable);
      }

      for(QRecord storedRecord : tableData)
      {
         QRecord recordToReturn = new QRecord(storedRecord);
         recordToReturn.setTableName(input.getTableName());
         if(BackendQueryFilterUtils.doesRecordMatch(filter, joinsContext, recordToReturn))
         {
            addVirtualFieldsToRecord(recordToReturn, input.getTable(), null, input.getFieldNamesToInclude(), joinsContext);
            for(QueryJoin queryJoin : queryJoins)
            {
               if(queryJoin.getSelect())
               {
                  addVirtualFieldsToRecord(recordToReturn, personalizedTables.get(queryJoin.getJoinTable()), queryJoin.getJoinTableOrItsAlias(), input.getFieldNamesToInclude(), joinsContext);
               }
            }
            recordToReturn.setAssociatedRecords(new HashMap<>());
            recordToReturn.setDisplayValues(new HashMap<>());
            recordToReturn.setErrors(new ArrayList<>());
            records.add(recordToReturn);
         }
      }

      BackendQueryFilterUtils.sortRecordList(joinsContext, filter, records);
      records = BackendQueryFilterUtils.applySkipAndLimit(filter, records);
      for(QRecord record : records)
      {
         projectQueryRecord(record, input, personalizedTables, joinsContext);
      }

      return (records);
   }



   /*******************************************************************************
    ** Project after filtering and ordering so unselected operands stay internal.
    ** Heavy values use the same length-detail representation as native queries.
    *******************************************************************************/
   private void projectQueryRecord(QRecord record, QueryInput input, Map<String, QTableMetaData> tableMap, JoinsContext joinsContext) throws QException
   {
      Collection<String> fieldNames = input.getFieldNamesToInclude() == null ? record.getValues().keySet() : input.getFieldNamesToInclude();
      Map<String, Serializable> values = new HashMap<>();
      HashMap<String, Serializable> heavyFieldLengths = new HashMap<>();
      for(String fieldName : fieldNames)
      {
         String tableNameOrAlias = input.getTableName();
         String localFieldName = fieldName;
         QueryJoin selectedJoin = null;
         int dotIndex = fieldName.indexOf('.');
         if(dotIndex >= 0)
         {
            tableNameOrAlias = fieldName.substring(0, dotIndex);
            localFieldName = fieldName.substring(dotIndex + 1);
            for(QueryJoin queryJoin : joinsContext.getQueryJoins())
            {
               if(queryJoin.getJoinTableOrItsAlias().equals(tableNameOrAlias))
               {
                  selectedJoin = queryJoin;
                  break;
               }
            }
            if((selectedJoin != null && !selectedJoin.getSelect()) || (selectedJoin == null && !tableNameOrAlias.equals(input.getTableName())))
            {
               continue;
            }
         }
         QTableMetaData table = tableMap.get(joinsContext.resolveTableNameOrAliasToTableName(tableNameOrAlias));
         QFieldMetaData field = table == null ? null : table.getFields().get(localFieldName);
         if(field == null && table != null)
         {
            field = table.getVirtualField(localFieldName);
         }
         if(field == null)
         {
            continue;
         }
         Serializable value = record.getValue(selectedJoin == null ? localFieldName : fieldName);
         if(field.getIsHeavy() && !input.getShouldFetchHeavyFields())
         {
            Integer length = value == null ? null : field.getType() == QFieldType.BLOB
               ? ValueUtils.getValueAsByteArray(value).length : ValueUtils.getValueAsString(value).length();
            heavyFieldLengths.put(fieldName, length);
         }
         else
         {
            values.put(fieldName, value);
         }
      }
      record.setValues(values);
      record.setBackendDetails(new HashMap<>(CollectionUtils.nonNullMap(record.getBackendDetails())));
      record.getBackendDetails().remove(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS);
      if(!heavyFieldLengths.isEmpty())
      {
         record.addBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS, heavyFieldLengths);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void addVirtualFieldsToRecord(QRecord record, QTableMetaData table, String tableNameOrAlias, Set<String> fieldNamesToInclude, JoinsContext joinsContext) throws QException
   {
      for(QVirtualFieldMetaData virtualField : CollectionUtils.nonNullMap(table.getVirtualFields()).values())
      {
         String fieldName = tableNameOrAlias == null ? virtualField.getName() : tableNameOrAlias + "." + virtualField.getName();
         if(virtualField.getIsQuerySelectable() && virtualField.getFieldFunction() != null
            && (fieldNamesToInclude == null || fieldNamesToInclude.contains(fieldName)))
         {
            FieldFunctionType fieldFunctionType = FieldFunctionTypeRegistry.ofOrWithNew(QContext.getQInstance()).getFieldFunctionType(virtualField.getFieldFunction().getFunctionTypeIdentifier());

            Serializable value = fieldFunctionType.apply(virtualField.getFieldFunction(), BackendQueryFilterUtils.recordForFieldFunction(record, fieldName, joinsContext));
            record.withValue(fieldName, ValueUtils.getValueAsFieldType(virtualField.getType(), value));
         }
      }
   }



   /*******************************************************************************
    * Given a table and a list of query joins, build a collection of records that
    * make up a cross-product necessary to perform the join query.
    *
    * <p>Note that this can potentially be explosively huge... which is why the memory
    * backend is not meant for use with large production data sets...</p>
    *
    * <p>Of course, I suppose, we could probably stream through the cross-product,
    * or take an altogether different approach, but, this serves us for the time being.</p>
    *
    * <p>Note that INNER & LEFT joins should work but, RIGHT joins will not work
    * at this time.</p>
    *
    * @param table the main-table being queried for
    * @param queryJoins the list of joins to cross against the main table.
    * @param joinsContext more details about the join.
    *******************************************************************************/
   private Collection<QRecord> buildJoinCrossProduct(QTableMetaData table, List<QueryJoin> queryJoins, JoinsContext joinsContext) throws QException
   {
      List<QRecord>  crossProduct = new ArrayList<>();
      QTableMetaData leftTable    = table;
      for(QRecord record : getTableData(leftTable).values())
      {
         QRecord productRecord = new QRecord().withTableName(table.getName());
         addRecordToProduct(productRecord, record, null);
         crossProduct.add(productRecord);
      }

      for(QueryJoin queryJoin : queryJoins)
      {
         QTableMetaData      nextTable        = joinsContext.getTable(queryJoin.getJoinTable());
         Collection<QRecord> nextTableRecords = getTableData(nextTable).values();
         QQueryFilter joinFilter = new QQueryFilter();
         joinFilter.setCriteria(queryJoin.getSecurityCriteria());

         QJoinMetaData joinMetaData = queryJoin.getJoinMetaData();
         if(joinMetaData == null)
         {
            joinMetaData = joinsContext.findJoinMetaData(table.getName(), queryJoin.getJoinTable(), false);
            Objects.requireNonNull(joinMetaData, () -> "Did not have, and could not find a join metaData between tables in QueryJoin object base=[" + leftTable + "], join=[" + queryJoin.getJoinTable() + "]");
         }

         List<QRecord> nextLevelProduct = new ArrayList<>();
         for(QRecord productRecord : crossProduct)
         {
            boolean matchFound = false;
            for(QRecord nextTableRecord : nextTableRecords)
            {
               if(joinMatches(productRecord, nextTableRecord, queryJoin, joinMetaData))
               {
                  QRecord joinRecord = new QRecord(productRecord);
                  addRecordToProduct(joinRecord, nextTableRecord, queryJoin.getJoinTableOrItsAlias());
                  if(BackendQueryFilterUtils.doesRecordMatch(joinFilter, joinsContext, joinRecord))
                  {
                     nextLevelProduct.add(joinRecord);
                     matchFound = true;
                  }
               }
            }

            if(!matchFound)
            {
               if(QueryJoin.Type.LEFT.equals(queryJoin.getType()))
               {
                  QRecord joinRecord = new QRecord(productRecord);
                  for(String fieldName : nextTable.getFields().keySet())
                  {
                     joinRecord.setValue(queryJoin.getJoinTableOrItsAlias() + "." + fieldName, null);
                  }
                  nextLevelProduct.add(joinRecord);
               }
            }
         }

         crossProduct = nextLevelProduct;
      }

      return (crossProduct);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean joinMatches(QRecord productRecord, QRecord nextTableRecord, QueryJoin queryJoin, QJoinMetaData joinMetaData)
   {
      for(JoinOn joinOn : joinMetaData.getJoinOns())
      {
         Serializable leftValue = productRecord.getValues().containsKey(queryJoin.getBaseTableOrAlias() + "." + joinOn.getLeftField())
            ? productRecord.getValue(queryJoin.getBaseTableOrAlias() + "." + joinOn.getLeftField())
            : productRecord.getValue(joinOn.getLeftField());
         Serializable rightValue = nextTableRecord.getValue(joinOn.getRightField());
         if(leftValue == null || rightValue == null || !Objects.equals(leftValue, rightValue))
         {
            return (false);
         }
      }

      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addRecordToProduct(QRecord productRecord, QRecord record, String tableNameOrAlias)
   {
      for(Map.Entry<String, Serializable> entry : record.getValues().entrySet())
      {
         productRecord.withValue(tableNameOrAlias == null ? entry.getKey() : tableNameOrAlias + "." + entry.getKey(), entry.getValue());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public CountOutput count(CountInput input) throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////////
      // set up a query input - we'll implement count by counting the records in a query output //
      ////////////////////////////////////////////////////////////////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setCommonParamsFrom(input);
      QTableMetaData queryTable = input.getTable();
      if(BooleanUtils.isTrue(input.getIncludeDistinctCount()) && !queryTable.getFields().containsKey(queryTable.getPrimaryKeyField()))
      {
         queryTable = queryTable.clone();
         queryTable.addField(QContext.getQInstance().getTable(input.getTableName()).getField(queryTable.getPrimaryKeyField()).clone());
      }
      queryInput.setTableMetaData(queryTable);
      queryInput.setInputSource(input.getInputSource());
      queryInput.setFieldNamesToInclude(BooleanUtils.isTrue(input.getIncludeDistinctCount()) ? Set.of(queryTable.getPrimaryKeyField()) : Set.of());
      queryInput.setShouldFetchHeavyFields(true);

      if(input.getFilter() != null)
      {
         queryInput.setFilter(input.getFilter().clone().withSkip(null).withLimit(null));
      }

      if(input.getQueryJoins() != null)
      {
         queryInput.setQueryJoins(new ArrayList<>());
         for(QueryJoin queryJoin : input.getQueryJoins())
         {
            queryInput.getQueryJoins().add(queryJoin.clone());
         }
      }

      ///////////////////
      // run the query //
      ///////////////////
      List<QRecord> queryResult = query(queryInput);

      ////////////////////////
      // build count output //
      ////////////////////////
      CountOutput countOutput = new CountOutput();
      countOutput.setCount(queryResult.size());

      //////////////////////////////////////
      // figure out distinct if requested //
      //////////////////////////////////////
      if(BooleanUtils.isTrue(input.getIncludeDistinctCount()))
      {
         String            primaryKeyField = queryInput.getTable().getPrimaryKeyField();
         Set<Serializable> distinctValues  = new HashSet<>();
         for(QRecord record : queryResult)
         {
            distinctValues.add(record.getValue(primaryKeyField));
         }
         countOutput.setDistinctCount(distinctValues.size());
      }

      return (countOutput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<QRecord> insert(InsertInput input, boolean returnInsertedRecords) throws QException
   {
      incrementStatistic(input);

      if(input.getRecords() == null)
      {
         return (new ArrayList<>());
      }

      QTableMetaData             table     = input.getTable();
      Map<Serializable, QRecord> tableData = getTableData(table);

      ///////////////////////////////////////////////////////////////////////
      // Keep key selection, owner checks and writes in the same table lock. //
      ///////////////////////////////////////////////////////////////////////
      synchronized(tableData)
      {
         Integer nextSerial = Objects.requireNonNullElse(getNextSerial(table), 1);
         Set<Object> existingKeys = new HashSet<>();
         for(QRecord stored : tableData.values())
         {
            existingKeys.add(AssociatedRecordUpdate.primaryKey(table, stored));
         }

         List<QRecord> outputRecords = new ArrayList<>();
         QFieldMetaData primaryKeyField = table.getField(table.getPrimaryKeyField());
         boolean generatedSerial = primaryKeyField.getType().equals(QFieldType.INTEGER) || primaryKeyField.getType().equals(QFieldType.LONG);
         for(QRecord record : input.getRecords())
         {
            //////////////////////////////////////////////////////////////////////
            // Copy values so native storage never shares the caller's record. //
            //////////////////////////////////////////////////////////////////////
            QRecord recordToInsert = new QRecord(record);
            stripUnrecognizedFieldsFromRecords(List.of(recordToInsert), table);
            makeValueTypesMatchFieldTypes(table, recordToInsert);

            if(CollectionUtils.nullSafeHasContents(recordToInsert.getErrors()))
            {
               outputRecords.add(recordToInsert);
               continue;
            }

            if(recordToInsert.getValue(primaryKeyField.getName()) == null && generatedSerial)
            {
               do
               {
                  Serializable serial = primaryKeyField.getType().equals(QFieldType.LONG) ? nextSerial.longValue() : (Serializable) nextSerial;
                  recordToInsert.setValue(primaryKeyField.getName(), serial);
                  nextSerial++;
               }
               while(existingKeys.contains(AssociatedRecordUpdate.primaryKey(table, recordToInsert)));
            }

            Object key = AssociatedRecordUpdate.primaryKey(table, recordToInsert);
            if(key == null || existingKeys.contains(key))
            {
               recordToInsert.addError(new BadInputStatusMessage(key == null ? "A primary key is required" : "A record with this primary key already exists"));
               outputRecords.add(recordToInsert);
               continue;
            }

            //////////////////////////////////////////////////////////////////////////
            // Preserve the existing serial sequence after a caller-supplied key. //
            //////////////////////////////////////////////////////////////////////////
            if(generatedSerial && recordToInsert.getValueInteger(primaryKeyField.getName()) >= nextSerial)
            {
               nextSerial = recordToInsert.getValueInteger(primaryKeyField.getName()) + 1;
            }

            tableData.put(recordToInsert.getValue(primaryKeyField.getName()), recordToInsert);
            existingKeys.add(key);
            if(returnInsertedRecords)
            {
               outputRecords.add(recordToInsert);
            }
         }

         setNextSerial(table, nextSerial);
         return (outputRecords);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static void stripUnrecognizedFieldsFromRecords(List<QRecord> records, QTableMetaData table)
   {
      stripUnrecognizedFieldsFromRecords(records, Map.of(table.getName(), table), table);
   }



   /***************************************************************************
    * take map of "personalized" tables - e.g., possibly with fields removed
    * and then only allow a field if it's in that personalized table.
    ***************************************************************************/
   private static void stripUnrecognizedFieldsFromRecords(List<QRecord> records, Map<String, QTableMetaData> tableMap, QTableMetaData mainTable)
   {
      if(CollectionUtils.nullSafeHasContents(records))
      {
         for(QRecord record : records)
         {
            Iterator<Map.Entry<String, Serializable>> iterator = record.getValues().entrySet().iterator();
            while(iterator.hasNext())
            {
               Map.Entry<String, Serializable> entry     = iterator.next();
               String                          fieldName = entry.getKey();

               try
               {
                  FieldAndJoinTable fieldAndJoinTable = FieldAndJoinTable.get(mainTable, fieldName);
                  QTableMetaData    tableMetaData     = tableMap.get(fieldAndJoinTable.joinTable().getName());
                  if(!tableMetaData.getFields().containsKey(fieldAndJoinTable.field().getName()))
                  {
                     iterator.remove();
                  }
               }
               catch(Exception e) // from the FieldAndJoinTable call
               {
                  iterator.remove();
               }
            }
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void setNextSerial(QTableMetaData table, Integer nextSerial) throws QException
   {
      BackendIdentifier    backendIdentifier     = getBackendIdentifier(table);
      Map<String, Integer> nextSerialsForBackend = nextSerials.computeIfAbsent(backendIdentifier, (k) -> Collections.synchronizedMap(new HashMap<>()));
      nextSerialsForBackend.put(table.getName(), nextSerial);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private Integer getNextSerial(QTableMetaData table) throws QException
   {
      BackendIdentifier    backendIdentifier     = getBackendIdentifier(table);
      Map<String, Integer> nextSerialsForBackend = nextSerials.computeIfAbsent(backendIdentifier, (k) -> Collections.synchronizedMap(new HashMap<>()));
      return (nextSerialsForBackend.get(table.getName()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void makeValueTypesMatchFieldTypes(QTableMetaData table, QRecord recordToInsert)
   {
      for(QFieldMetaData field : table.getFields().values())
      {
         Serializable value = recordToInsert.getValue(field.getName());
         if(value != null)
         {
            try
            {
               recordToInsert.setValue(field.getName(), ValueUtils.getValueAsFieldType(field.getType(), value));
            }
            catch(Exception e)
            {
               LOG.info("Error converting value to field's type", e, logPair("fieldName", field.getName()), logPair("value", value));
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<QRecord> update(UpdateInput input, boolean returnUpdatedRecords) throws QException
   {
      if(input.getRecords() == null)
      {
         return (new ArrayList<>());
      }

      QTableMetaData             table     = input.getTable();
      Map<Serializable, QRecord> tableData = getTableData(table);

      List<QRecord>  outputRecords   = new ArrayList<>();
      QFieldMetaData primaryKeyField = table.getField(table.getPrimaryKeyField());
      for(QRecord record : input.getRecords())
      {
         Serializable primaryKeyValue = ValueUtils.getValueAsFieldType(primaryKeyField.getType(), record.getValue(primaryKeyField.getName()));

         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            outputRecords.add(record);
            continue;
         }

         if(tableData.containsKey(primaryKeyValue))
         {
            QRecord recordToUpdate = tableData.get(primaryKeyValue);
            for(Map.Entry<String, Serializable> valueEntry : record.getValues().entrySet())
            {
               if(!table.getFields().containsKey(valueEntry.getKey()))
               {
                  /////////////////////////////////////////////////////////////
                  // don't update values in fields that aren't in the table  //
                  // (or that the user doesn't have, due to personalization) //
                  /////////////////////////////////////////////////////////////
                  continue;
               }

               String fieldName = valueEntry.getKey();
               try
               {
                  ///////////////////////////////////////////////
                  // try to make field values match field type //
                  ///////////////////////////////////////////////
                  recordToUpdate.setValue(fieldName, ValueUtils.getValueAsFieldType(table.getField(fieldName).getType(), valueEntry.getValue()));
               }
               catch(Exception e)
               {
                  LOG.info("Error converting value to field's type", e, logPair("fieldName", fieldName), logPair("value", valueEntry.getValue()));
                  recordToUpdate.setValue(fieldName, valueEntry.getValue());
               }
            }

            if(returnUpdatedRecords)
            {
               outputRecords.add(record);
            }
         }
      }

      return (outputRecords);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public int delete(DeleteInput input) throws QException
   {
      if(input.getPrimaryKeys() == null)
      {
         return (0);
      }

      QTableMetaData             table           = input.getTable();
      QFieldMetaData             primaryKeyField = table.getField(table.getPrimaryKeyField());
      Map<Serializable, QRecord> tableData       = getTableData(table);
      int                        rowsDeleted     = 0;
      for(Serializable primaryKeyValue : input.getPrimaryKeys())
      {
         primaryKeyValue = ValueUtils.getValueAsFieldType(primaryKeyField.getType(), primaryKeyValue);
         if(tableData.containsKey(primaryKeyValue))
         {
            tableData.remove(primaryKeyValue);
            rowsDeleted++;
         }
      }

      return (rowsDeleted);
   }



   /*******************************************************************************
    ** Setter for collectStatistics
    **
    *******************************************************************************/
   public static void setCollectStatistics(boolean collectStatistics)
   {
      MemoryRecordStore.collectStatistics = collectStatistics;
   }



   /*******************************************************************************
    ** Increment a statistic
    **
    *******************************************************************************/
   public static void incrementStatistic(AbstractActionInput input)
   {
      if(collectStatistics)
      {
         actionInputs.add(input.getClass(), input);
         if(input instanceof QueryInput)
         {
            incrementStatistic(STAT_QUERIES_RAN);
         }
         else if(input instanceof InsertInput)
         {
            incrementStatistic(STAT_INSERTS_RAN);
         }
      }
   }



   /*******************************************************************************
    ** Increment a statistic
    **
    *******************************************************************************/
   public static void incrementStatistic(String statName)
   {
      if(collectStatistics)
      {
         statistics.putIfAbsent(statName, 0);
         statistics.put(statName, statistics.get(statName) + 1);
      }
   }



   /*******************************************************************************
    ** clear the map of statistics
    **
    *******************************************************************************/
   public static void resetStatistics()
   {
      statistics.clear();
      actionInputs.clear();
   }



   /*******************************************************************************
    ** Getter for statistics
    **
    *******************************************************************************/
   public static Map<String, Integer> getStatistics()
   {
      return statistics;
   }



   /*******************************************************************************
    ** Getter for the actionInputs that were recorded - only while collectStatistics
    ** was true.
    *******************************************************************************/
   public static ListingHash<Class<? extends AbstractActionInput>, AbstractActionInput> getActionInputs()
   {
      return (actionInputs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public AggregateOutput aggregate(AggregateInput aggregateInput) throws QException
   {
      //////////////////////
      // first do a query //
      //////////////////////
      JoinsContext joinsContext = new JoinsContext(QContext.getQInstance(), aggregateInput, clonedOrNewFilter(aggregateInput.getFilter()));
      QueryInput queryInput = new QueryInput();
      queryInput.setCommonParamsFrom(aggregateInput);
      queryInput.setQueryJoins(joinsContext.getQueryJoins().stream().map(queryJoin -> queryJoin.clone().withSelect(true)).toList());
      queryInput.setTableMetaData(aggregateInput.getTable());
      queryInput.setInputSource(aggregateInput.getInputSource());
      Set<String> operandFields = new HashSet<>();
      CollectionUtils.nonNullList(aggregateInput.getAggregates()).forEach(aggregate -> operandFields.add(aggregate.getFieldName()));
      CollectionUtils.nonNullList(aggregateInput.getGroupBys()).forEach(groupBy -> operandFields.add(groupBy.getFieldName()));
      queryInput.setFieldNamesToInclude(operandFields);
      queryInput.setShouldFetchHeavyFields(true);
      queryInput.setFilter(clonedOrNewFilter(aggregateInput.getFilter()));
      queryInput.getFilter().setOrderBys(List.of());
      List<QRecord> queryResult = query(queryInput);

      List<AggregateResult> results    = new ArrayList<>();
      List<GroupBy>         groupBys   = CollectionUtils.nonNullList(aggregateInput.getGroupBys());
      List<Aggregate>       aggregates = CollectionUtils.nonNullList(aggregateInput.getAggregates());

      /////////////////////
      // do the group-by //
      /////////////////////
      ListingHash<List<Serializable>, QRecord> bins = new ListingHash<>();
      for(QRecord record : queryResult)
      {
         List<Serializable> groupByValues = new ArrayList<>(groupBys.size());
         for(GroupBy groupBy : groupBys)
         {
            Serializable groupByValue = record.getValue(groupBy.getFieldName());
            if(StringUtils.hasContent(groupBy.getFormatString()))
            {
               groupByValue = applyFormatString(groupByValue, groupBy);
            }
            else if(groupBy.getType() != null)
            {
               groupByValue = ValueUtils.getValueAsFieldType(groupBy.getType(), groupByValue);
            }
            groupByValues.add(groupByValue);
         }

         bins.add(groupByValues, record);
      }

      ////////////////////////
      // do the aggregating //
      ////////////////////////
      for(Map.Entry<List<Serializable>, List<QRecord>> entry : bins.entrySet())
      {
         List<Serializable> groupByValueList = entry.getKey();
         List<QRecord>      records          = entry.getValue();

         AggregateResult aggregateResult = new AggregateResult();
         results.add(aggregateResult);

         ////////////////////////////////////////////
         // set the group-by values in this result //
         ////////////////////////////////////////////
         Map<GroupBy, Serializable> groupByValues = new HashMap<>();
         aggregateResult.setGroupByValues(groupByValues);
         for(int i = 0; i < groupBys.size(); i++)
         {
            GroupBy      groupBy = groupBys.get(i);
            Serializable value   = groupByValueList.get(i);
            groupByValues.put(groupBy, value);
         }

         ////////////////////////////
         // compute the aggregates //
         ////////////////////////////
         Map<Aggregate, Serializable> aggregateValues = new HashMap<>();
         aggregateResult.setAggregateValues(aggregateValues);

         for(Aggregate aggregate : aggregates)
         {
            Serializable aggregateValue = computeAggregate(records, aggregate, joinsContext);

            aggregateValues.put(aggregate, aggregateValue);
         }
      }

      /////////////////////
      // sort the result //
      /////////////////////
      if(aggregateInput.getFilter() != null && CollectionUtils.nullSafeHasContents(aggregateInput.getFilter().getOrderBys()))
      {
         /////////////////////////////////////////////////////////////////////////////////////
         // lambda to compare 2 serializables, as we'll assume (& cast) them to Comparables //
         /////////////////////////////////////////////////////////////////////////////////////
         Comparator<Serializable> serializableComparator = (Serializable a, Serializable b) ->
         {
            if(a == null && b == null)
            {
               return (0);
            }
            else if(a == null)
            {
               return (1);
            }
            else if(b == null)
            {
               return (-1);
            }

            @SuppressWarnings("unchecked")
            Comparable<Serializable> comparableSerializableA = (Comparable<Serializable>) a;

            return comparableSerializableA.compareTo(b);
         };

         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // reverse of the lambda above (we had some errors calling .reversed() on the comparator we were building, so this seemed simpler & worked) //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         Comparator<Serializable> reverseSerializableComparator = (Serializable a, Serializable b) -> -serializableComparator.compare(a, b);

         ////////////////////////////////////////////////
         // build a comparator out of all the orderBys //
         ////////////////////////////////////////////////
         Comparator<AggregateResult> comparator = null;
         for(QFilterOrderBy orderBy : aggregateInput.getFilter().getOrderBys())
         {
            Function<AggregateResult, Serializable> keyExtractor = aggregateResult ->
            {
               if(orderBy instanceof QFilterOrderByGroupBy orderByGroupBy)
               {
                  return aggregateResult.getGroupByValue(orderByGroupBy.getGroupBy());
               }
               else if(orderBy instanceof QFilterOrderByAggregate orderByAggregate)
               {
                  return aggregateResult.getAggregateValue(orderByAggregate.getAggregate());
               }
               else
               {
                  throw (new IllegalStateException("Unexpected orderBy [" + orderBy + "] in aggregate"));
               }
            };

            if(comparator == null)
            {
               comparator = Comparator.comparing(keyExtractor, orderBy.getIsAscending() ? serializableComparator : reverseSerializableComparator);
            }
            else
            {
               comparator = comparator.thenComparing(keyExtractor, orderBy.getIsAscending() ? serializableComparator : reverseSerializableComparator);
            }
         }

         ///////////////////////////////////////
         // sort the list with the comparator //
         ///////////////////////////////////////
         results.sort(comparator);
      }

      AggregateOutput aggregateOutput = new AggregateOutput();
      aggregateOutput.setResults(results);
      return (aggregateOutput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable applyFormatString(Serializable value, GroupBy groupBy) throws QException
   {
      if(value == null)
      {
         return (null);
      }

      String formatString = groupBy.getFormatString();

      try
      {
         if(formatString.startsWith("DATE_FORMAT"))
         {
            /////////////////////////////////////////////////////////////////////////////
            // one known-use case we have here looks like this:                        //
            // DATE_FORMAT(CONVERT_TZ(%s, 'UTC', 'UTC'), '%%Y-%%m-%%dT%%H')            //
            // ... for now, let's just try to support the formatting bit at the end... //
            // todo - support the CONVERT_TZ bit too!                                  //
            /////////////////////////////////////////////////////////////////////////////
            String            sqlDateTimeFormat = formatString.replaceFirst(".*'%%", "%%").replaceFirst("'.*", "");
            DateTimeFormatter dateTimeFormatter = DateTimeGroupBy.sqlDateFormatToSelectedDateTimeFormatter(sqlDateTimeFormat);
            if(dateTimeFormatter == null)
            {
               throw (new QException("Unsupported sql dateTime format string [" + sqlDateTimeFormat + "] for MemoryRecordStore"));
            }

            String        valueAsString  = ValueUtils.getValueAsString(value);
            Instant       valueAsInstant = ValueUtils.getValueAsInstant(valueAsString);
            ZonedDateTime zonedDateTime  = valueAsInstant.atZone(ZoneId.systemDefault());
            return (dateTimeFormatter.format(zonedDateTime));
         }
         else
         {
            throw (new QException("Unsupported group-by format string [" + formatString + "] for MemoryRecordStore"));
         }
      }
      catch(QException qe)
      {
         throw (qe);
      }
      catch(Exception e)
      {
         throw (new QException("Error applying format string [" + formatString + "] to group by value [" + value + "]", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings({ "rawtypes", "unchecked" })
   private static Serializable computeAggregate(List<QRecord> records, Aggregate aggregate, JoinsContext joinsContext)
   {
      String            fieldName = aggregate.getFieldName();
      AggregateOperator operator  = aggregate.getOperator();
      QFieldType        fieldType;
      if(aggregate.getFieldType() == null)
      {
         QFieldMetaData field = joinsContext.getFieldAndTableNameOrAlias(fieldName, true).field();
         if((field.getType().equals(QFieldType.INTEGER) || field.getType().equals(QFieldType.LONG)) && (operator.equals(AggregateOperator.AVG)))
         {
            fieldType = QFieldType.DECIMAL;
         }
         else if(operator.equals(AggregateOperator.COUNT) || operator.equals(AggregateOperator.COUNT_DISTINCT))
         {
            fieldType = QFieldType.INTEGER;
         }
         else
         {
            fieldType = field.getType();
         }
      }
      else
      {
         fieldType = aggregate.getFieldType();
      }

      Serializable aggregateValue = switch(operator)
      {
         case COUNT -> records.stream()
            .filter(r -> r.getValue(fieldName) != null)
            .count();

         case COUNT_DISTINCT -> records.stream()
            .filter(r -> r.getValue(fieldName) != null)
            .map(r -> r.getValue(fieldName))
            .collect(Collectors.toSet())
            .size();

         case SUM -> switch(fieldType)
         {
            case INTEGER -> records.stream()
               .filter(r -> r.getValue(fieldName) != null)
               .mapToInt(r -> r.getValueInteger(fieldName))
               .sum();
            case LONG -> records.stream()
               .filter(r -> r.getValue(fieldName) != null)
               .mapToLong(r -> r.getValueLong(fieldName))
               .sum();
            case DECIMAL -> records.stream()
               .filter(r -> r.getValue(fieldName) != null)
               .map(r -> r.getValueBigDecimal(fieldName))
               .reduce(BigDecimal.ZERO, BigDecimal::add);
            default -> throw (new IllegalArgumentException("Cannot perform " + operator + " aggregate on " + fieldType + " field."));
         };

         case MIN -> switch(fieldType)
         {
            case INTEGER -> records.stream()
               .filter(r -> r.getValue(fieldName) != null)
               .mapToInt(r -> r.getValueInteger(fieldName))
               .min()
               .stream().boxed().findFirst().orElse(null);
            case LONG -> records.stream()
               .filter(r -> r.getValue(fieldName) != null)
               .mapToLong(r -> r.getValueLong(fieldName))
               .min()
               .stream().boxed().findFirst().orElse(null);
            case DECIMAL, STRING, DATE, DATE_TIME ->
            {
               Optional<Serializable> serializable = records.stream()
                  .filter(r -> r.getValue(fieldName) != null)
                  .map(r -> ((Comparable) ValueUtils.getValueAsFieldType(fieldType, r.getValue(fieldName))))
                  .min(Comparator.naturalOrder())
                  .map(c -> (Serializable) c);
               yield serializable.orElse(null);
            }
            default -> throw (new IllegalArgumentException("Cannot perform " + operator + " aggregate on " + fieldType + " field."));
         };

         case MAX -> switch(fieldType)
         {
            case INTEGER -> records.stream()
               .filter(r -> r.getValue(fieldName) != null)
               .mapToLong(r -> r.getValueInteger(fieldName))
               .max()
               .stream().boxed().findFirst().orElse(null);
            case LONG -> records.stream()
               .filter(r -> r.getValue(fieldName) != null)
               .mapToLong(r -> r.getValueLong(fieldName))
               .max()
               .stream().boxed().findFirst().orElse(null);
            case DECIMAL, STRING, DATE, DATE_TIME ->
            {
               Optional<Serializable> serializable = records.stream()
                  .filter(r -> r.getValue(fieldName) != null)
                  .map(r -> ((Comparable) ValueUtils.getValueAsFieldType(fieldType, r.getValue(fieldName))))
                  .max(Comparator.naturalOrder())
                  .map(c -> (Serializable) c);
               yield serializable.orElse(null);
            }
            default -> throw (new IllegalArgumentException("Cannot perform " + operator + " aggregate on " + fieldType + " field."));
         };

         case AVG -> switch(fieldType)
         {
            case INTEGER -> records.stream()
               .filter(r -> r.getValue(fieldName) != null)
               .mapToInt(r -> r.getValueInteger(fieldName))
               .average()
               .stream().boxed().findFirst().orElse(null);
            case LONG -> records.stream()
               .filter(r -> r.getValue(fieldName) != null)
               .mapToLong(r -> r.getValueLong(fieldName))
               .average()
               .stream().boxed().findFirst().orElse(null);
            case DECIMAL -> records.stream()
               .filter(r -> r.getValue(fieldName) != null)
               .mapToDouble(r -> r.getValueBigDecimal(fieldName).doubleValue())
               .average()
               .stream().boxed().map(d -> new BigDecimal(d)).findFirst().orElse(null);
            default -> throw (new IllegalArgumentException("Cannot perform " + operator + " aggregate on " + fieldType + " field."));
         };
      };

      return ValueUtils.getValueAsFieldType(fieldType, aggregateValue);
   }



   /*******************************************************************************
    ** Either clone the input filter (so we can change it safely), or return a new blank filter.
    *******************************************************************************/
   protected QQueryFilter clonedOrNewFilter(QQueryFilter filter)
   {
      if(filter == null)
      {
         return (new QQueryFilter());
      }
      else
      {
         return (filter.clone());
      }
   }



   /***************************************************************************
    ** key for the internal maps of this class - either for a non-variant version
    ** of the memory backend, or for one based on variants.
    ***************************************************************************/
   private sealed interface BackendIdentifier permits NonVariant, Variant
   {
   }



   /***************************************************************************
    ** singleton, representing non-variant instance of memory backend.
    ***************************************************************************/
   private static final class NonVariant implements BackendIdentifier
   {
      private static NonVariant nonVariant = null;



      /*******************************************************************************
       ** Singleton constructor
       *******************************************************************************/
      private NonVariant()
      {

      }



      /*******************************************************************************
       ** Singleton accessor
       *******************************************************************************/
      public static NonVariant getInstance()
      {
         if(nonVariant == null)
         {
            nonVariant = new NonVariant();
         }
         return (nonVariant);
      }
   }



   /***************************************************************************
    ** record representing a variant type & id
    ***************************************************************************/
   private record Variant(String type, Serializable id) implements BackendIdentifier
   {
   }



   /*******************************************************************************
    * Getter for buildJoinCrossProductFromJoinContext
    * @see #withBuildJoinCrossProductFromJoinContext(boolean)
    *******************************************************************************/
   public boolean getBuildJoinCrossProductFromJoinContext()
   {
      return (this.buildJoinCrossProductFromJoinContext);
   }



   /*******************************************************************************
    * Setter for buildJoinCrossProductFromJoinContext
    * @see #withBuildJoinCrossProductFromJoinContext(boolean)
    *******************************************************************************/
   public void setBuildJoinCrossProductFromJoinContext(boolean buildJoinCrossProductFromJoinContext)
   {
      this.buildJoinCrossProductFromJoinContext = buildJoinCrossProductFromJoinContext;
   }



   /*******************************************************************************
    * Fluent setter for buildJoinCrossProductFromJoinContext
    *
    * <p>Keep this setting true so queries include the joins required by READ
    * rules. The legacy false mode is retained as a configuration value, but
    * queries reject it with a checked QException.</p>
    *
    * @return this
    *******************************************************************************/
   public MemoryRecordStore withBuildJoinCrossProductFromJoinContext(boolean buildJoinCrossProductFromJoinContext)
   {
      this.buildJoinCrossProductFromJoinContext = buildJoinCrossProductFromJoinContext;
      return (this);
   }


}
