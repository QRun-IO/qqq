/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.ActionFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheUseCase;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import org.apache.commons.lang3.NotImplementedException;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** After running a query, if it's for a table that's a CacheOf another table,
 ** see if there are any cache use-cases to apply to the query result.
 **
 ** Such as:
 ** - if it's a query for one or more values in a UniqueKey:
 **   - if any particular UniqueKeys weren't found, look in the source table
 ** - if any cached records are expired, refresh them from the source
 **   - possibly updating the cached record; possibly deleting it.
 *******************************************************************************/
public class QueryActionCacheHelper
{
   private static final QLogger LOG = QLogger.getLogger(QueryActionCacheHelper.class);

   private boolean                              isQueryInputCacheable = false;
   private Map<CacheUseCase.Type, CacheUseCase> cacheUseCaseMap       = new HashMap<>();
   private CacheUseCase                         activeCacheUseCase    = null;

   private UniqueKey                         cacheUniqueKey  = null;
   private ListingHash<String, Serializable> uniqueKeyValues = new ListingHash<>();



   /***************************************************************************
    * action flags that can be put in a query request to control behavior in
    * this class.
    ***************************************************************************/
   public enum CacheActionFlags implements ActionFlag
   {
      /***************************************************************************
       * Cause the handleCaching method to basically noop - but the intent is, that
       * it doesn't every query the source table (the one behind the cache).
       ***************************************************************************/
      DO_NOT_QUERY_SOURCE_TABLE
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void handleCaching(QueryInput queryInput, QueryOutput queryOutput) throws QException
   {
      if(queryInput.hasFlag(CacheActionFlags.DO_NOT_QUERY_SOURCE_TABLE))
      {
         //////////////////////////////////////////////////////////////////////////////////////////
         // if the caller is requesting that we not query the source table under any conditions, //
         // (e.g., unique key checks) then there's nothing to do in this method, so return early //
         //////////////////////////////////////////////////////////////////////////////////////////
         return;
      }

      analyzeInput(queryInput);
      if(!isQueryInputCacheable)
      {
         return;
      }

      //////////////////////////////////////////////////////////////////////////
      // figure out which keys in the query were found, and which were missed //
      //////////////////////////////////////////////////////////////////////////
      List<QRecord>           recordsFoundInCache           = new ArrayList<>(queryOutput.getRecords());
      Set<List<Serializable>> uniqueKeyValuesInFoundRecords = getUniqueKeyValuesFromFoundRecords(queryOutput.getRecords());
      Set<List<Serializable>> missedUniqueKeyValues         = getUniqueKeyValuesFromQuery();
      missedUniqueKeyValues.removeAll(uniqueKeyValuesInFoundRecords);

      ///////////////////////////////////////////////////////////////////////////////////
      // if any requested records weren't found, see if we should look in cache-source //
      ///////////////////////////////////////////////////////////////////////////////////
      if(CollectionUtils.nullSafeHasContents(missedUniqueKeyValues))
      {
         List<QRecord> recordsFromSource = tryToGetFromCacheSource(queryInput, missedUniqueKeyValues);
         if(CollectionUtils.nullSafeHasContents(recordsFromSource))
         {
            //////////////////////////////////////////////////////////////////////////////////////////////////
            // good, we found records from the source, make sure we should cache them, and if so, do it now //
            // note, we always return the record from the source, even if we don't cache it.                //
            //////////////////////////////////////////////////////////////////////////////////////////////////
            QTableMetaData table = queryInput.getTable();

            List<QRecord> recordsToReturn = recordsFromSource.stream()
               .map(r -> CacheUtils.mapSourceRecordToCacheRecord(table, r, activeCacheUseCase))
               .toList();
            queryOutput.addRecords(recordsToReturn);

            List<QRecord> recordsToCache = recordsToReturn.stream()
               .filter(r -> CacheUtils.shouldCacheRecord(table, r))
               .toList();

            if(CollectionUtils.nullSafeHasContents(recordsToCache))
            {
               try
               {
                  InsertInput insertInput = new InsertInput();
                  insertInput.setTableName(queryInput.getTableName());
                  insertInput.setRecords(recordsToCache);
                  insertInput.setSkipUniqueKeyCheck(true);
                  InsertOutput insertOutput = new InsertAction().execute(insertInput);

                  //////////////////////////////////////////////////////////
                  // set the (generated) ids in the records being returne //
                  //////////////////////////////////////////////////////////
                  Map<List<Serializable>, QRecord> insertedRecordsByUniqueKey = new HashMap<>();
                  for(QRecord record : insertOutput.getRecords())
                  {
                     insertedRecordsByUniqueKey.put(getUniqueKeyValues(record), record);
                  }
                  for(QRecord record : recordsToReturn)
                  {
                     QRecord insertedRecord = insertedRecordsByUniqueKey.get(getUniqueKeyValues(record));
                     if(insertedRecord != null)
                     {
                        record.setValue(table.getPrimaryKeyField(), insertedRecord.getValue(table.getPrimaryKeyField()));
                     }
                  }
               }
               catch(Exception e)
               {
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // don't let an exception break this query - it (probably) just indicates some data that didn't get cached - so - that's generally "ok" //
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  LOG.warn("Error inserting cached records", e, logPair("cacheTable", queryInput.getTableName()));
               }
            }
         }
      }

      //////////////////////////////////////////////////////////////////////////
      // for records that were found, if they're too old, maybe re-fetch them //
      //////////////////////////////////////////////////////////////////////////
      if(CollectionUtils.nullSafeHasContents(recordsFoundInCache))
      {
         refreshCacheIfExpired(recordsFoundInCache, queryInput, queryOutput);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void refreshCacheIfExpired(List<QRecord> recordsFoundInCache, QueryInput queryInput, QueryOutput queryOutput) throws QException
   {
      QTableMetaData table             = queryInput.getTable();
      Integer        expirationSeconds = table.getCacheOf().getExpirationSeconds();

      if(expirationSeconds != null)
      {
         List<QRecord> expiredRecords = new ArrayList<>();
         for(QRecord cachedRecord : recordsFoundInCache)
         {
            Instant cachedDate = cachedRecord.getValueInstant(table.getCacheOf().getCachedDateFieldName());
            if(cachedDate == null || cachedDate.isBefore(Instant.now().minus(expirationSeconds, ChronoUnit.SECONDS)))
            {
               expiredRecords.add(cachedRecord);
            }
         }

         if(CollectionUtils.nullSafeHasContents(expiredRecords))
         {
            Map<List<Serializable>, Serializable> uniqueKeyToPrimaryKeyMap = getUniqueKeyToPrimaryKeyMap(table.getPrimaryKeyField(), expiredRecords);
            Set<List<Serializable>>               uniqueKeyValuesToRefresh = uniqueKeyToPrimaryKeyMap.keySet();

            ////////////////////////////////////////////
            // fetch records from original source now //
            ////////////////////////////////////////////
            List<QRecord> recordsFromSource = tryToGetFromCacheSource(queryInput, uniqueKeyValuesToRefresh);

            Set<List<Serializable>> uniqueKeyValuesInFoundRecords = getUniqueKeyValuesFromFoundRecords(recordsFromSource);
            Set<List<Serializable>> missedUniqueKeyValues         = getUniqueKeyValuesFromQuery();
            missedUniqueKeyValues.retainAll(getUniqueKeyValuesFromFoundRecords(expiredRecords));
            missedUniqueKeyValues.removeAll(uniqueKeyValuesInFoundRecords);

            //////////////////////////////////////////////////////////////////////////////////////////////////////
            // build records to cache - setting their original (from cache) ids back in them, so they'll update //
            //////////////////////////////////////////////////////////////////////////////////////////////////////
            List<QRecord> refreshedRecordsToReturn = recordsFromSource.stream()
               .map(r ->
               {
                  QRecord recordToCache = CacheUtils.mapSourceRecordToCacheRecord(table, r, activeCacheUseCase);
                  recordToCache.setValue(table.getPrimaryKeyField(), uniqueKeyToPrimaryKeyMap.get(getUniqueKeyValues(recordToCache)));
                  return (recordToCache);
               })
               .toList();

            ///////////////////////////////////////////////////////////////////////////////////////////////////////
            // if the records were found in the source, put it into the output object so returned back to caller //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////
            Map<List<Serializable>, QRecord> refreshedRecordsByUniqueKeyValues = refreshedRecordsToReturn.stream().collect(Collectors.toMap(this::getUniqueKeyValues, r -> r, (a, b) -> a));

            ListIterator<QRecord> queryOutputListIterator = queryOutput.getRecords().listIterator();
            while(queryOutputListIterator.hasNext())
            {
               QRecord            originalRecord        = queryOutputListIterator.next();
               List<Serializable> recordUniqueKeyValues = getUniqueKeyValues(originalRecord);
               QRecord            refreshedRecord       = refreshedRecordsByUniqueKeyValues.get(recordUniqueKeyValues);

               if(refreshedRecord != null)
               {
                  queryOutputListIterator.set(refreshedRecord);
               }
               else if(missedUniqueKeyValues.contains(recordUniqueKeyValues))
               {
                  queryOutputListIterator.remove();
               }
            }

            ////////////////////////////////////////////////////////////////////////////
            // for refreshed records which should be cached, update them in the cache //
            ////////////////////////////////////////////////////////////////////////////
            List<QRecord> recordsToUpdate = refreshedRecordsToReturn.stream().filter(r -> CacheUtils.shouldCacheRecord(table, r)).toList();
            if(CollectionUtils.nullSafeHasContents(recordsToUpdate))
            {
               try
               {
                  UpdateInput updateInput = new UpdateInput();
                  updateInput.setTableName(queryInput.getTableName());
                  updateInput.setRecords(recordsToUpdate);
                  UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
               }
               catch(Exception e)
               {
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // don't let an exception break this query - it (probably) just indicates some data that didn't get cached - so - that's generally "ok" //
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  LOG.warn("Error updating cached records", e, logPair("cacheTable", queryInput.getTableName()));
               }
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if the records were missed in the source - OR if they shouldn't be cached now, then mark them for deleting //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            Set<Serializable> cachedRecordIdsToDelete = missedUniqueKeyValues.stream()
               .map(uniqueKeyToPrimaryKeyMap::get)
               .collect(Collectors.toSet());

            cachedRecordIdsToDelete.addAll(refreshedRecordsToReturn.stream()
               .filter(r -> !CacheUtils.shouldCacheRecord(table, r))
               .map(r -> r.getValue(table.getPrimaryKeyField()))
               .collect(Collectors.toSet()));

            if(CollectionUtils.nullSafeHasContents(cachedRecordIdsToDelete))
            {
               /////////////////////////////////////////////////////////////////////////////////
               // if the records are no longer in the source, then remove them from the cache //
               /////////////////////////////////////////////////////////////////////////////////
               try
               {
                  DeleteInput deleteInput = new DeleteInput();
                  deleteInput.setTableName(queryInput.getTableName());
                  deleteInput.setPrimaryKeys(new ArrayList<>(cachedRecordIdsToDelete));
                  new DeleteAction().execute(deleteInput);

               }
               catch(Exception e)
               {
                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // don't let an exception break this query - it (probably) just indicates some data that didn't get uncached - so - that's generally "ok" //
                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  LOG.warn("Error deleting cached records", e, logPair("cacheTable", queryInput.getTableName()));
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Set<List<Serializable>> getUniqueKeyValuesFromQuery()
   {
      Set<List<Serializable>> rs = new HashSet<>();

      int noOfUniqueKeys = uniqueKeyValues.get(cacheUniqueKey.getFieldNames().get(0)).size();
      for(int i = 0; i < noOfUniqueKeys; i++)
      {
         List<Serializable> values = new ArrayList<>();

         for(String fieldName : cacheUniqueKey.getFieldNames())
         {
            values.add(uniqueKeyValues.get(fieldName).get(i));
         }

         ////////////////////////////////////////////////////////////////////////////////
         // critical - leave this here so hashCode from the list is correctly computed //
         ////////////////////////////////////////////////////////////////////////////////
         rs.add(values);
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Set<List<Serializable>> getUniqueKeyValuesFromFoundRecords(List<QRecord> records)
   {
      return (getUniqueKeyToPrimaryKeyMap("ignore", records).keySet());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<List<Serializable>, Serializable> getUniqueKeyToPrimaryKeyMap(String primaryKeyField, List<QRecord> records)
   {
      Map<List<Serializable>, Serializable> rs = new HashMap<>();

      for(QRecord record : records)
      {
         List<Serializable> uniqueKeyValues = getUniqueKeyValues(record);
         rs.put(uniqueKeyValues, record.getValue(primaryKeyField));
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Serializable> getUniqueKeyValues(QRecord record)
   {
      List<Serializable> uniqueKeyValues = new ArrayList<>();
      for(String fieldName : cacheUniqueKey.getFieldNames())
      {
         uniqueKeyValues.add(record.getValue(fieldName));
      }
      return uniqueKeyValues;
   }



   /*******************************************************************************
    ** figure out if this was a request that we can cache records for -
    ** e.g., if it's a request for unique-key EQUALS or IN
    ** build up fields for the unique keys, the values, etc.
    *******************************************************************************/
   private void analyzeInput(QueryInput queryInput)
   {
      QTableMetaData table = queryInput.getTable();

      for(CacheUseCase cacheUseCase : CollectionUtils.nonNullList(table.getCacheOf().getUseCases()))
      {
         cacheUseCaseMap.put(cacheUseCase.getType(), cacheUseCase);
      }

      if(cacheUseCaseMap.containsKey(CacheUseCase.Type.UNIQUE_KEY_TO_UNIQUE_KEY))
      {
         if(queryInput.getFilter() == null)
         {
            LOG.trace("Unable to cache: there is no filter");
            return;
         }

         QQueryFilter filter      = queryInput.getFilter();
         Set<String>  queryFields = new HashSet<>();
         if(CollectionUtils.nullSafeHasContents(filter.getSubFilters()))
         {
            if(CollectionUtils.nullSafeHasContents(filter.getCriteria()))
            {
               LOG.trace("Unable to cache: we have sub-filters and criteria");
               return;
            }

            if(!QQueryFilter.BooleanOperator.OR.equals(filter.getBooleanOperator()))
            {
               LOG.trace("Unable to cache: we have sub-filters but not an OR query");
               return;
            }

            /////////////////////////
            // look at sub-filters //
            /////////////////////////
            for(QQueryFilter subFilter : filter.getSubFilters())
            {
               Set<String> thisSubFilterFields = getQueryFieldsIfCacheableFilter(subFilter, false);
               if(thisSubFilterFields == null)
               {
                  return;
               }

               if(queryFields.isEmpty())
               {
                  queryFields.addAll(thisSubFilterFields);
               }
               else
               {
                  if(!queryFields.equals(thisSubFilterFields))
                  {
                     LOG.trace("Unable to cache: sub-filters have different sets of fields");
                     return;
                  }
               }
            }

            if(doQueryFieldsMatchAUniqueKey(table, queryFields))
            {
               return;
            }

            LOG.trace("Unable to cache: we have sub-filters that do match a unique key");
            return;
         }
         else
         {
            //////////////////////////////////////////
            // look at the criteria in the query:   //
            // - build a set of field names         //
            // - fail upon unsupported operators    //
            // - collect the values in the criteria //
            //////////////////////////////////////////
            queryFields = getQueryFieldsIfCacheableFilter(filter, true);
            if(queryFields == null)
            {
               return;
            }
         }

         if(doQueryFieldsMatchAUniqueKey(table, queryFields))
         {
            return;
         }

         LOG.trace("Unable to cache: we have query fields that don't match a unique key: " + queryFields);
         return;
      }

      LOG.trace("Unable to cache: No supported use case: " + cacheUseCaseMap.keySet());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean doQueryFieldsMatchAUniqueKey(QTableMetaData table, Set<String> queryFields)
   {
      for(UniqueKey uniqueKey : CollectionUtils.nonNullList(table.getUniqueKeys()))
      {
         if(queryFields.equals(new HashSet<>(uniqueKey.getFieldNames())))
         {
            this.cacheUniqueKey = uniqueKey;
            isQueryInputCacheable = true;
            activeCacheUseCase = cacheUseCaseMap.get(CacheUseCase.Type.UNIQUE_KEY_TO_UNIQUE_KEY);
            return true;
         }
      }
      return false;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Set<String> getQueryFieldsIfCacheableFilter(QQueryFilter filter, boolean allowOperatorIn)
   {
      Set<String> rs = new HashSet<>();
      for(QFilterCriteria criterion : filter.getCriteria())
      {
         boolean isEquals = criterion.getOperator().equals(QCriteriaOperator.EQUALS);
         boolean isIn     = criterion.getOperator().equals(QCriteriaOperator.IN);

         if(isEquals || (isIn && allowOperatorIn))
         {
            rs.add(criterion.getFieldName());
            this.uniqueKeyValues.addAll(criterion.getFieldName(), criterion.getValues());
         }
         else
         {
            LOG.trace("Unable to cache: we have an unsupported criteria operator: " + criterion.getOperator());
            isQueryInputCacheable = false;
            return (null);
         }
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> tryToGetFromCacheSource(QueryInput queryInput, Set<List<Serializable>> uniqueKeyValues) throws QException
   {
      List<QRecord>  recordsFromSource = null;
      QTableMetaData table             = queryInput.getTable();

      if(CacheUseCase.Type.UNIQUE_KEY_TO_UNIQUE_KEY.equals(activeCacheUseCase.getType()))
      {
         recordsFromSource = getFromCachedSourceForUniqueKeyToUniqueKey(queryInput, uniqueKeyValues, table.getCacheOf().getSourceTable());
      }
      else
      {
         // todo!!
         throw (new NotImplementedException("Not-yet-implemented cache use case type: " + activeCacheUseCase.getType()));
      }

      return (recordsFromSource);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> getFromCachedSourceForUniqueKeyToUniqueKey(QueryInput cacheQueryInput, Set<List<Serializable>> uniqueKeyValues, String sourceTableName) throws QException
   {
      QTableMetaData   sourceTable   = QContext.getQInstance().getTable(sourceTableName);
      QBackendMetaData sourceBackend = QContext.getQInstance().getBackendForTable(sourceTableName);

      if(sourceTable.isCapabilityEnabled(sourceBackend, Capability.TABLE_QUERY))
      {
         ///////////////////////////////////////////////////////
         // do a Query on the source table, by the unique key //
         ///////////////////////////////////////////////////////
         QueryInput sourceQueryInput = new QueryInput();
         sourceQueryInput.setTableName(sourceTableName);

         QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
         sourceQueryInput.setFilter(filter);
         ((QueryOrGetInputInterface) sourceQueryInput).setCommonParamsFrom(cacheQueryInput);

         for(List<Serializable> uniqueKeyValue : uniqueKeyValues)
         {
            QQueryFilter subFilter = new QQueryFilter();
            filter.addSubFilter(subFilter);

            for(int i = 0; i < cacheUniqueKey.getFieldNames().size(); i++)
            {
               subFilter.addCriteria(new QFilterCriteria(cacheUniqueKey.getFieldNames().get(i), QCriteriaOperator.EQUALS, uniqueKeyValue.get(i)));
            }
         }

         QueryOutput sourceQueryOutput = new QueryAction().execute(sourceQueryInput);
         return (sourceQueryOutput.getRecords());
      }
      else if(sourceTable.isCapabilityEnabled(sourceBackend, Capability.TABLE_GET))
      {
         ///////////////////////////////////////////////////////////////////////
         // if the table only supports GET, then do a GET for each unique key //
         ///////////////////////////////////////////////////////////////////////
         List<QRecord> outputRecords = new ArrayList<>();
         for(List<Serializable> uniqueKeyValue : uniqueKeyValues)
         {
            Map<String, Serializable> uniqueKey = new HashMap<>();
            for(int i = 0; i < cacheUniqueKey.getFieldNames().size(); i++)
            {
               uniqueKey.put(cacheUniqueKey.getFieldNames().get(i), uniqueKeyValue.get(i));
            }

            GetInput getInput = new GetInput();
            getInput.setTableName(sourceTableName);
            getInput.setUniqueKey(uniqueKey);
            getInput.setCommonParamsFrom(cacheQueryInput);
            GetOutput getOutput = new GetAction().execute(getInput);

            if(getOutput.getRecord() != null)
            {
               outputRecords.add(getOutput.getRecord());
            }
         }

         return (outputRecords);
      }
      else
      {
         throw (new QException("Cache source table " + sourceTableName + " does not support Query or Get capability."));
      }
   }

}
