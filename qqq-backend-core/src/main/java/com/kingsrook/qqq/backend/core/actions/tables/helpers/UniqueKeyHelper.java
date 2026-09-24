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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.DuplicateKeyBadInputStatusMessage;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Methods to help with unique key checks.
 *******************************************************************************/
public class UniqueKeyHelper
{
   private static Integer pageSize = 1000;

   /*******************************************************************************
    ** Validate final sparse candidates without hydrating caller patches or replacing
    ** the ordinary access-checked old records. Existing other owners remain occupied.
    *******************************************************************************/
   public static void validateUpdateUniqueKeys(UpdateInput input) throws QException
   {
      QTableMetaData table = input.getTable();
      List<UniqueKey> keys = CollectionUtils.nonNullList(table.getUniqueKeys());
      if(keys.isEmpty() || CollectionUtils.nullSafeIsEmpty(input.getRecords()))
      {
         return;
      }
      String primaryKey = table.getPrimaryKeyField();
      Map<Object, List<QRecord>> byPrimaryKey = new LinkedHashMap<>();
      for(QRecord record : input.getRecords())
      {
         try
         {
            Object typedKey = UniqueKeyLookup.equalityValue(UniqueKeyLookup.typedValue(table, primaryKey, record.getValue(primaryKey)));
            byPrimaryKey.computeIfAbsent(typedKey, ignored -> new ArrayList<>()).add(record);
         }
         catch(QException e)
         {
            record.addError(new BadInputStatusMessage("Invalid primary key for unique-key validation"));
         }
      }
      for(List<QRecord> occurrences : byPrimaryKey.values())
      {
         if(occurrences.size() > 1 && occurrences.stream().anyMatch(record -> keys.stream().anyMatch(key -> touchesKey(record, key))))
         {
            occurrences.forEach(record -> record.addError(new BadInputStatusMessage("Repeated primary key in a key-changing update batch")));
         }
      }
      List<QRecord> candidates = input.getRecords().stream().filter(record -> CollectionUtils.nullSafeIsEmpty(record.getErrors()))
         .filter(record -> keys.stream().anyMatch(key -> touchesKey(record, key))).toList();
      Map<Object, QRecord> stored = new HashMap<>();
      List<QRecord> sparseCandidates = candidates.stream().filter(record -> needsStoredComponents(record, keys)).toList();
      for(List<QRecord> page : CollectionUtils.getPages(sparseCandidates, pageSize))
      {
         List<Serializable> primaryKeys = page.stream().map(record -> record.getValue(primaryKey)).toList();
         for(QRecord record : UniqueKeyLookup.readStoredComponents(table, primaryKeys, input.getTransaction()))
         {
            Serializable value = UniqueKeyLookup.typedValue(table, primaryKey, record.getValue(primaryKey));
            if(value == null)
            {
               throw new QException("Native unique-key lookup returned a null owner primary key");
            }
            stored.put(UniqueKeyLookup.equalityValue(value), record);
         }
      }
      Map<UniqueKey, Set<List<Object>>> reserved = new HashMap<>();
      for(QRecord patch : candidates)
      {
         Object self = UniqueKeyLookup.equalityValue(UniqueKeyLookup.typedValue(table, primaryKey, patch.getValue(primaryKey)));
         QRecord old = needsStoredComponents(patch, keys) ? stored.get(self) : new QRecord();
         if(old == null)
         {
            patch.addError(new BadInputStatusMessage("Unable to validate stored unique-key components"));
            continue;
         }
         QRecord candidate = new QRecord();
         candidate.setValues(new HashMap<>(old.getValues()));
         candidate.getValues().putAll(patch.getValues());
         Map<UniqueKey, List<Object>> proposed = new LinkedHashMap<>();
         for(UniqueKey key : keys)
         {
            if(!touchesKey(patch, key))
            {
               continue;
            }
            List<Object> tuple;
            try
            {
               tuple = strictTuple(table, key, candidate);
            }
            catch(QException e)
            {
               patch.addError(new BadInputStatusMessage("Invalid value for unique-key validation"));
               break;
            }
            if(tuple == null)
            {
               continue;
            }
            boolean duplicate = reserved.getOrDefault(key, Set.of()).contains(tuple);
            for(QRecord owner : UniqueKeyLookup.findConflicts(table, key, candidate, input.getTransaction()))
            {
               Serializable ownerKey = UniqueKeyLookup.typedValue(table, primaryKey, owner.getValue(primaryKey));
               if(ownerKey == null)
               {
                  throw new QException("Native unique-key lookup returned a null owner primary key");
               }
               duplicate |= !self.equals(UniqueKeyLookup.equalityValue(ownerKey));
            }
            if(duplicate)
            {
               addDuplicateError(patch, table, key);
               break;
            }
            proposed.put(key, tuple);
         }
         if(CollectionUtils.nullSafeIsEmpty(patch.getErrors()))
         {
            proposed.forEach((key, tuple) -> reserved.computeIfAbsent(key, ignored -> new HashSet<>()).add(tuple));
         }
      }
   }



   /*******************************************************************************
    ** Persisted conflicts remain checked at the existing INSERT unique-key phase.
    *******************************************************************************/
   public static void validateInsertStoredKeys(InsertInput input) throws QException
   {
      if(input.getSkipUniqueKeyCheck())
      {
         return;
      }
      for(QRecord record : input.getRecords())
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            continue;
         }
         for(UniqueKey key : CollectionUtils.nonNullList(input.getTable().getUniqueKeys()))
         {
            try
            {
               if(strictTuple(input.getTable(), key, record) == null)
               {
                  continue;
               }
            }
            catch(QException e)
            {
               record.addError(new BadInputStatusMessage("Invalid value for unique-key validation"));
               break;
            }
            if(!UniqueKeyLookup.findConflicts(input.getTable(), key, record, input.getTransaction()).isEmpty())
            {
               addDuplicateError(record, input.getTable(), key);
               break;
            }
         }
      }
   }



   /*******************************************************************************
    ** Reserve in-request keys only after required-field and security validation.
    ** Customizer phase order is retained; AFTER_ALL_VALIDATIONS remains trusted.
    *******************************************************************************/
   public static void validateInsertBatchKeys(InsertInput input) throws QException
   {
      if(input.getSkipUniqueKeyCheck())
      {
         return;
      }
      Map<UniqueKey, Set<List<Object>>> reserved = new HashMap<>();
      for(QRecord record : input.getRecords())
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            continue;
         }
         Map<UniqueKey, List<Object>> proposed = new LinkedHashMap<>();
         for(UniqueKey key : CollectionUtils.nonNullList(input.getTable().getUniqueKeys()))
         {
            List<Object> tuple;
            try
            {
               tuple = strictTuple(input.getTable(), key, record);
            }
            catch(QException e)
            {
               record.addError(new BadInputStatusMessage("Invalid value for unique-key validation"));
               break;
            }
            if(tuple != null)
            {
               if(reserved.getOrDefault(key, Set.of()).contains(tuple))
               {
                  addDuplicateError(record, input.getTable(), key);
                  break;
               }
               proposed.put(key, tuple);
            }
         }
         if(CollectionUtils.nullSafeIsEmpty(record.getErrors()))
         {
            proposed.forEach((key, tuple) -> reserved.computeIfAbsent(key, ignored -> new HashSet<>()).add(tuple));
         }
      }
   }



   /*******************************************************************************
    ** Only sparse touched keys need privileged stored components. Complete keys can
    ** be checked by providers that deliberately do not support ordinary prefetch.
    *******************************************************************************/
   private static boolean needsStoredComponents(QRecord record, List<UniqueKey> keys)
   {
      return keys.stream().anyMatch(key -> touchesKey(record, key)
         && key.getFieldNames().stream().anyMatch(field -> !record.getValues().containsKey(field)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean touchesKey(QRecord record, UniqueKey key)
   {
      return key.getFieldNames().stream().anyMatch(record.getValues()::containsKey);
   }



   /*******************************************************************************
    ** Null components are distinct; type conversion failures are never proof of uniqueness.
    *******************************************************************************/
   private static List<Object> strictTuple(QTableMetaData table, UniqueKey key, QRecord record) throws QException
   {
      List<Object> tuple = new ArrayList<>();
      boolean hasNull = false;
      for(String field : key.getFieldNames())
      {
         Serializable value = UniqueKeyLookup.typedValue(table, field, record.getValue(field));
         hasNull |= value == null;
         tuple.add(UniqueKeyLookup.equalityValue(value));
      }
      return hasNull ? null : tuple;
   }



   /*******************************************************************************
    ** Reveal no conflicting owner or stored value.
    *******************************************************************************/
   private static void addDuplicateError(QRecord record, QTableMetaData table, UniqueKey key)
   {
      record.addError(new DuplicateKeyBadInputStatusMessage("Another record already exists with this " + key.getDescription(table)));
   }



   /*******************************************************************************
    ** Match a complete Replace request against native READ-visible values. The
    ** returned identity map never hydrates private matching fields into callers.
    ** Legacy bulk-insert helpers retain their existing tuple and Query contracts.
    *******************************************************************************/
   public static Map<QRecord, Serializable> findMatches(QTableMetaData table, List<QRecord> records, UniqueKey key,
      boolean allowNullKeyValuesToEqual, InputSource inputSource, QBackendTransaction transaction) throws QException
   {
      if(table == null || table.getFields() == null || key == null || CollectionUtils.nullSafeIsEmpty(key.getFieldNames()) || records == null || inputSource == null)
      {
         throw new QException("Replace requires a table, matching key, records and input source");
      }
      QTableMetaData canonicalTable = QContext.getQInstance().getTable(table.getName());
      Set<String> keyFields = new LinkedHashSet<>();
      for(String fieldName : key.getFieldNames())
      {
         if(fieldName == null || fieldName.isBlank() || !keyFields.add(fieldName))
         {
            throw new QException("Replace requires distinct available physical matching fields");
         }
         QFieldMetaData field = table.getFields().get(fieldName);
         if(field == null || field.getType() == null || canonicalTable == null || canonicalTable.getFields().get(fieldName) == null)
         {
            throw new QException("Replace requires distinct available physical matching fields");
         }
      }
      QTableMetaData physicalTable = AssociatedRecordDiscovery.physicalParentTable(table);
      physicalTable.setVirtualFields(Map.of());
      String primaryKeyField = physicalTable.getPrimaryKeyField();
      Set<String> projectedFields = new LinkedHashSet<>(keyFields);
      projectedFields.add(primaryKeyField);

      Map<QRecord, List<Serializable>> valuesByRecord = new IdentityHashMap<>();
      Map<List<Object>, QRecord> requestedRecords = new LinkedHashMap<>();
      for(QRecord record : records)
      {
         if(record == null || CollectionUtils.nullSafeHasContents(record.getErrors()) || valuesByRecord.containsKey(record))
         {
            throw new QException("Replace requires distinct records without errors");
         }
         List<Serializable> values = getReplaceKeyValues(table, keyFields, record, true);
         valuesByRecord.put(record, values);
         if(!allowNullKeyValuesToEqual && values.contains(null))
         {
            continue;
         }
         if(requestedRecords.putIfAbsent(canonicalReplaceKey(values), record) != null)
         {
            throw new QException("Replace contains duplicate matching keys");
         }
      }

      Map<QRecord, Serializable> matches = new IdentityHashMap<>();
      Map<Object, QRecord> recordsByPrimaryKey = new HashMap<>();
      for(List<QRecord> page : CollectionUtils.getPages(new ArrayList<>(requestedRecords.values()), pageSize))
      {
         Set<List<Object>> pageKeys = new HashSet<>();
         QQueryFilter filter = new QQueryFilter();
         filter.setBooleanOperator(QQueryFilter.BooleanOperator.OR);
         if(keyFields.size() == 1)
         {
            List<Serializable> nonNullValues = new ArrayList<>();
            boolean includesNull = false;
            for(QRecord record : page)
            {
               List<Serializable> values = valuesByRecord.get(record);
               pageKeys.add(canonicalReplaceKey(values));
               if(values.get(0) == null)
               {
                  includesNull = true;
               }
               else
               {
                  nonNullValues.add(values.get(0));
               }
            }
            String fieldName = keyFields.iterator().next();
            filter.addCriteria(new QFilterCriteria(fieldName, includesNull ? QCriteriaOperator.IS_NULL_OR_IN : QCriteriaOperator.IN, nonNullValues));
         }
         else
         {
            for(QRecord record : page)
            {
               List<Serializable> values = valuesByRecord.get(record);
               pageKeys.add(canonicalReplaceKey(values));
               QQueryFilter tupleFilter = new QQueryFilter();
               int index = 0;
               for(String fieldName : keyFields)
               {
                  Serializable value = values.get(index++);
                  tupleFilter.addCriteria(value == null
                     ? new QFilterCriteria(fieldName, QCriteriaOperator.IS_NULL_OR_IN, List.of())
                     : new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, Collections.singletonList(value)));
               }
               filter.addSubFilter(tupleFilter);
            }
         }

         QueryInput queryInput = new QueryInput(table.getName()).withTransaction(transaction).withFilter(filter)
            .withFieldNamesToInclude(projectedFields).withShouldFetchHeavyFields(true)
            .withShouldOmitHiddenFields(false).withShouldMaskPasswords(false)
            .withFlag(QueryActionCacheHelper.CacheActionFlags.DO_NOT_QUERY_SOURCE_TABLE);
         queryInput.setTableMetaData(physicalTable);
         queryInput.setInputSource(inputSource);
         ActionHelper.validateSession(queryInput);
         QueryOutput queryOutput = new QBackendModuleDispatcher().getQBackendModule(queryInput.getBackend()).getQueryInterface().executeForDml(queryInput);
         if(queryOutput == null || queryOutput.getRecords() == null)
         {
            throw new QException("Native Replace matching returned no record list");
         }
         for(QRecord record : queryOutput.getRecords())
         {
            if(record == null || CollectionUtils.nullSafeHasContents(record.getErrors()))
            {
               throw new QException("Native Replace matching returned a failed record");
            }
            List<Object> tuple = canonicalReplaceKey(getReplaceKeyValues(physicalTable, keyFields, record, false));
            if(!pageKeys.contains(tuple))
            {
               ///////////////////////////////////////////////////////////////////////
               // Some native blank operators include empty strings as well as null. //
               // Such rows are not null owners; no other native mismatch is ignored. //
               ///////////////////////////////////////////////////////////////////////
               if(pageKeys.stream().anyMatch(expected -> isNullBlankSuperset(expected, tuple)))
               {
                  continue;
               }
               throw new QException("Native Replace matching returned an unrecognized tuple");
            }
            Serializable primaryKey = typedReplaceValue(physicalTable.getField(primaryKeyField), record.getValue(primaryKeyField));
            if(primaryKey == null)
            {
               throw new QException("Native Replace matching returned no primary key");
            }
            QRecord candidate = requestedRecords.get(tuple);
            Object typedPrimaryKey = UniqueKeyLookup.equalityValue(primaryKey);
            QRecord previousCandidate = recordsByPrimaryKey.putIfAbsent(typedPrimaryKey, candidate);
            Serializable previousKey = matches.putIfAbsent(candidate, primaryKey);
            if((previousCandidate != null && previousCandidate != candidate)
               || (previousKey != null && !Objects.equals(UniqueKeyLookup.equalityValue(previousKey), typedPrimaryKey)))
            {
               throw new QException("Replace matching is ambiguous");
            }
         }
      }
      return matches;
   }



   /*******************************************************************************
    ** Only filter normalization is appropriate before INSERT/UPDATE classification;
    ** dynamic defaults, READ transformations and customizers must not run here.
    *******************************************************************************/
   private static List<Serializable> getReplaceKeyValues(QTableMetaData table, Set<String> fields, QRecord record, boolean normalize) throws QException
   {
      List<Serializable> result = new ArrayList<>();
      for(String fieldName : fields)
      {
         QFieldMetaData field = table.getField(fieldName);
         Serializable value = UniqueKeyLookup.snapshot(record.getValue(fieldName));
         if(normalize)
         {
            QQueryFilter filter = new QQueryFilter(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, Collections.singletonList(value)));
            try
            {
               filter = ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getQInstance(), table, filter, Collections.emptySet());
               value = filter.getCriteria().get(0).getValues().get(0);
            }
            catch(RuntimeException e)
            {
               throw new QException("Invalid Replace matching value");
            }
         }
         result.add(typedReplaceValue(field, value));
      }
      return result;
   }



   /*******************************************************************************
    ** Keep typed values private, including independent mutable binary operands.
    *******************************************************************************/
   private static Serializable typedReplaceValue(QFieldMetaData field, Serializable value) throws QException
   {
      try
      {
         return UniqueKeyLookup.snapshot(ValueUtils.getValueAsFieldType(field.getType(), value));
      }
      catch(RuntimeException e)
      {
         throw new QException("Invalid Replace matching value");
      }
   }



   /*******************************************************************************
    ** This is Java tuple bookkeeping, not a database collation/coercion emulator.
    *******************************************************************************/
   private static List<Object> canonicalReplaceKey(List<Serializable> values)
   {
      return values.stream().map(UniqueKeyLookup::equalityValue).collect(Collectors.toList());
   }



   /*******************************************************************************
    ** Exclude only the known empty-string superset of a requested null tuple.
    *******************************************************************************/
   private static boolean isNullBlankSuperset(List<Object> expected, List<Object> actual)
   {
      for(int i = 0; i < expected.size(); i++)
      {
         if(!Objects.equals(expected.get(i), actual.get(i)) && !(expected.get(i) == null && "".equals(actual.get(i))))
         {
            return false;
         }
      }
      return true;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Map<List<Serializable>, Serializable> getExistingKeys(QBackendTransaction transaction, QTableMetaData table, List<QRecord> recordList, UniqueKey uniqueKey, boolean allowNullKeyValuesToEqual) throws QException
   {
      List<String>                          ukFieldNames    = uniqueKey.getFieldNames();
      Map<List<Serializable>, Serializable> existingRecords = new HashMap<>();
      if(ukFieldNames != null)
      {
         for(List<QRecord> page : CollectionUtils.getPages(recordList, pageSize))
         {
            QueryInput queryInput = new QueryInput();
            queryInput.setTableName(table.getName());
            queryInput.setTransaction(transaction);

            QQueryFilter filter = new QQueryFilter();
            if(ukFieldNames.size() == 1)
            {
               List<Serializable> values = page.stream()
                  .filter(r -> CollectionUtils.nullSafeIsEmpty(r.getErrors()))
                  .map(r -> r.getValue(ukFieldNames.get(0)))
                  .collect(Collectors.toList());

               if(values.isEmpty())
               {
                  continue;
               }

               filter.addCriteria(new QFilterCriteria(ukFieldNames.get(0), QCriteriaOperator.IN, values));
            }
            else
            {
               filter.setBooleanOperator(QQueryFilter.BooleanOperator.OR);
               for(QRecord record : page)
               {
                  if(CollectionUtils.nullSafeHasContents(record.getErrors()))
                  {
                     continue;
                  }

                  QQueryFilter subFilter = new QQueryFilter();
                  filter.addSubFilter(subFilter);
                  for(String fieldName : ukFieldNames)
                  {
                     Serializable value = record.getValue(fieldName);
                     if(value == null)
                     {
                        subFilter.addCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.IS_BLANK));
                     }
                     else
                     {
                        subFilter.addCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, value));
                     }
                  }
               }

               if(CollectionUtils.nullSafeIsEmpty(filter.getSubFilters()))
               {
                  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // if we didn't build any sub-filters (because all records have errors in them), don't run a query w/ no clauses - continue to next page //
                  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  continue;
               }
            }

            queryInput.setFilter(filter);

            /////////////////////////////////////////////////////////////////////////////////////////
            // in case the table we're working with is a CacheOf - tell the QueryActionCacheHelper //
            // not to do queries on the table behind the cache table (e.g., for cache misses or    //
            // expired records).  we just want to look at the cache table itself.                  //
            /////////////////////////////////////////////////////////////////////////////////////////
            queryInput.withFlag(QueryActionCacheHelper.CacheActionFlags.DO_NOT_QUERY_SOURCE_TABLE);

            QueryOutput queryOutput = new QueryAction().execute(queryInput);
            for(QRecord record : queryOutput.getRecords())
            {
               Optional<List<Serializable>> keyValues = getKeyValues(table, uniqueKey, record, allowNullKeyValuesToEqual);
               if(keyValues.isPresent())
               {
                  existingRecords.put(keyValues.get(), record.getValue(table.getPrimaryKeyField()));
               }
            }
         }
      }

      return (existingRecords);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Map<List<Serializable>, Serializable> getExistingKeys(QBackendTransaction transaction, QTableMetaData table, List<QRecord> recordList, UniqueKey uniqueKey) throws QException
   {
      return (getExistingKeys(transaction, table, recordList, uniqueKey, false));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Optional<List<Serializable>> getKeyValues(QTableMetaData table, UniqueKey uniqueKey, QRecord record, boolean allowNullKeyValuesToEqual)
   {
      try
      {
         List<Serializable> keyValues = new ArrayList<>();
         for(String fieldName : uniqueKey.getFieldNames())
         {
            QFieldMetaData field      = table.getField(fieldName);
            Serializable   value      = record.getValue(fieldName);
            Serializable   typedValue = ValueUtils.getValueAsFieldType(field.getType(), value);

            ///////////////////////////////////////////////////////////////////////////////////
            // if null value, look at flag to determine if a null should be used (which will //
            // allow keys to match), or a NullUniqueKeyValue, (which will never match)       //
            ///////////////////////////////////////////////////////////////////////////////////
            if(typedValue == null)
            {
               keyValues.add(allowNullKeyValuesToEqual ? null : new NullUniqueKeyValue());
            }
            else
            {
               keyValues.add(typedValue);
            }
         }
         return (Optional.of(keyValues));
      }
      catch(Exception e)
      {
         return (Optional.empty());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Optional<List<Serializable>> getKeyValues(QTableMetaData table, UniqueKey uniqueKey, QRecord record)
   {
      return (getKeyValues(table, uniqueKey, record, false));
   }



   /*******************************************************************************
    ** To make a list of unique key values here behave like they do in an RDBMS
    ** (which is what we're trying to mimic - which is - 2 null values in a field
    ** aren't considered the same, so they don't violate a unique key) (at least, that's
    ** how some RDBMS's work, right??) - use this value instead of nulls in the
    ** output of getKeyValues - where interestingly, this class always returns
    ** false in it equals method... Unclear how bad this is, e.g., if it's violating
    ** the contract for equals and hashCode...
    *******************************************************************************/
   public static class NullUniqueKeyValue implements Serializable
   {
      @Override
      public boolean equals(Object obj)
      {
         return (false);
      }
   }



   /*******************************************************************************
    ** Getter for pageSize
    **
    *******************************************************************************/
   public static Integer getPageSize()
   {
      return pageSize;
   }



   /*******************************************************************************
    ** Setter for pageSize
    **
    *******************************************************************************/
   public static void setPageSize(Integer pageSize)
   {
      UniqueKeyHelper.pageSize = pageSize;
   }

}
