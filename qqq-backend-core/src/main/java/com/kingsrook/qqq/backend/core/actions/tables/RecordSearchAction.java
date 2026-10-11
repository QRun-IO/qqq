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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.CriteriaOption;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Record search across tables: matches a free-text term against the
 ** {@link QTableMetaData#getSearchFields() search fields} of each table that
 ** declares them, and returns each match's table, primary key and label.
 **
 ** Only tables the session may read (and that support query) are searched; a
 ** table the session may not read, or that has no search fields, is skipped
 ** without error, so a caller cannot probe which tables exist.  Each table is
 ** queried through {@link QueryAction}, so record security locks, table
 ** personalization and query customizers apply exactly as for any query.
 ** String fields match when they contain the term, case-insensitively; integer
 ** fields match when they equal a numeric term.  Results per table are limited.
 **
 ** If the instance has a {@link RecordSearchProviderInterface record search
 ** provider}, the tables it claims are searched by it instead (its hits are
 ** re-read through QueryAction, so the same security applies), falling back to
 ** the default search for those tables if the provider fails.
 *******************************************************************************/
public class RecordSearchAction
{
   private static final QLogger LOG = QLogger.getLogger(RecordSearchAction.class);

   public static final int DEFAULT_LIMIT_PER_TABLE    = 5;
   public static final int MAX_LIMIT_PER_TABLE        = 25;
   public static final int MAX_SEARCH_TERM_LENGTH     = 100;
   public static final int MAX_PROVIDER_IDS_PER_TABLE = 100;

   private static final int     QUERY_TIMEOUT_SECONDS = 30;
   private static final Pattern INTEGER_TERM          = Pattern.compile("-?\\d{1,18}");



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordSearchOutput execute(RecordSearchInput input) throws QException
   {
      String searchTerm = input.getSearchTerm() == null ? "" : input.getSearchTerm().trim();
      if(!StringUtils.hasContent(searchTerm))
      {
         throw (new QBadRequestException("A search term is required."));
      }
      if(searchTerm.length() > MAX_SEARCH_TERM_LENGTH)
      {
         throw (new QBadRequestException("The search term may not be longer than " + MAX_SEARCH_TERM_LENGTH + " characters."));
      }

      int limitPerTable = input.getLimitPerTable() == null ? DEFAULT_LIMIT_PER_TABLE : Math.max(1, Math.min(MAX_LIMIT_PER_TABLE, input.getLimitPerTable()));

      Set<String> onlyTableNames = CollectionUtils.nullSafeHasContents(input.getTableNames()) ? new HashSet<>(input.getTableNames()) : null;

      QInstance            qInstance = QContext.getQInstance();
      List<QTableMetaData> tables    = new ArrayList<>();
      for(QTableMetaData table : CollectionUtils.nonNullMap(qInstance.getTables()).values())
      {
         if(CollectionUtils.nullSafeIsEmpty(table.getSearchFields()) || (onlyTableNames != null && !onlyTableNames.contains(table.getName())))
         {
            continue;
         }

         if(!PermissionsHelper.hasTablePermission(input, table.getName(), TablePermissionSubType.READ) || !table.isCapabilityEnabled(qInstance.getBackendForTable(table.getName()), Capability.TABLE_QUERY))
         {
            continue;
         }

         tables.add(table);
      }

      ///////////////////////////////////////////////////////////////////////////
      // tables a provider searched use its results; all others (including the //
      // provider's tables, if it failed) use the default search.  either way, //
      // results are output in instance table order.                           //
      ///////////////////////////////////////////////////////////////////////////
      Map<String, List<RecordSearchResult>> providerResults = searchWithProvider(qInstance, input, tables, searchTerm, limitPerTable);

      RecordSearchOutput output = new RecordSearchOutput();
      for(QTableMetaData table : tables)
      {
         List<RecordSearchResult> tableResults = providerResults.get(table.getName());
         output.getResults().addAll(tableResults != null ? tableResults : searchTable(input, table, searchTerm, limitPerTable));
      }

      return (output);
   }



   /*******************************************************************************
    ** Search the tables that the instance's record search provider (if any)
    ** claims.  Returns results by table name, for only those tables the provider
    ** searched - so empty if there is no provider, it claims no tables, or it
    ** fails (which is logged, and leaves its tables to the default search).
    *******************************************************************************/
   private Map<String, List<RecordSearchResult>> searchWithProvider(QInstance qInstance, RecordSearchInput input, List<QTableMetaData> tables, String searchTerm, int limitPerTable) throws QException
   {
      QCodeReference providerReference = qInstance.getRecordSearchProvider();
      if(providerReference == null || tables.isEmpty())
      {
         return (new HashMap<>());
      }

      Map<String, QTableMetaData> claimedTables = new LinkedHashMap<>();
      RecordSearchOutput          providerOutput;
      try
      {
         RecordSearchProviderInterface provider = QCodeLoader.getAdHoc(RecordSearchProviderInterface.class, providerReference);
         if(provider == null)
         {
            throw (new QException("The record search provider could not be loaded."));
         }

         for(QTableMetaData table : tables)
         {
            if(provider.claimsTable(table))
            {
               claimedTables.put(table.getName(), table);
            }
         }

         if(claimedTables.isEmpty())
         {
            return (new HashMap<>());
         }

         providerOutput = provider.search(new RecordSearchInput()
            .withSearchTerm(searchTerm)
            .withTableNames(new ArrayList<>(claimedTables.keySet()))
            .withLimitPerTable(limitPerTable)
            .withInputSource(input.getInputSource()));
      }
      catch(Exception | LinkageError e)
      {
         LOG.warn("Error in record search provider - searching its tables with the default search instead", e, logPair("provider", providerReference.getName()), logPair("tableNames", claimedTables.keySet()));
         return (new HashMap<>());
      }

      /////////////////////////////////////////////////////////////////////
      // collect the provider's ids per claimed table, in its order      //
      // (ignoring results for other tables, and results without an id) //
      /////////////////////////////////////////////////////////////////////
      Map<String, List<Serializable>> idsByTable = new HashMap<>();
      for(String tableName : claimedTables.keySet())
      {
         idsByTable.put(tableName, new ArrayList<>());
      }

      List<RecordSearchResult> providerResults = providerOutput == null ? null : providerOutput.getResults();
      for(RecordSearchResult result : CollectionUtils.nonNullList(providerResults))
      {
         List<Serializable> ids = result == null ? null : idsByTable.get(result.getTableName());
         if(ids != null && result.getRecordId() != null)
         {
            ids.add(result.getRecordId());
         }
      }

      Map<String, List<RecordSearchResult>> resultsByTable = new HashMap<>();
      for(QTableMetaData table : claimedTables.values())
      {
         resultsByTable.put(table.getName(), readProviderResults(input, table, idsByTable.get(table.getName()), limitPerTable));
      }
      return (resultsByTable);
   }



   /*******************************************************************************
    ** Re-read the records a provider returned for one table, by primary key,
    ** through QueryAction - so only records that still exist and that the
    ** session may read are output - keeping the provider's order.
    *******************************************************************************/
   private List<RecordSearchResult> readProviderResults(RecordSearchInput input, QTableMetaData table, List<Serializable> providerIds, int limitPerTable) throws QException
   {
      QFieldMetaData    primaryKeyField = table.getField(table.getPrimaryKeyField());
      Set<Serializable> ids             = new LinkedHashSet<>();
      for(Serializable providerId : providerIds)
      {
         if(ids.size() >= MAX_PROVIDER_IDS_PER_TABLE)
         {
            break;
         }

         Serializable id = valueAsFieldType(primaryKeyField, providerId);
         if(id != null)
         {
            ids.add(id);
         }
      }

      List<RecordSearchResult> results = new ArrayList<>();
      if(ids.isEmpty())
      {
         return (results);
      }

      QQueryFilter filter = new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, new ArrayList<>(ids)));

      Map<Serializable, QRecord> recordsById = new HashMap<>();
      for(QRecord record : new QueryAction().execute(newQueryInput(input, table, filter)).getRecords())
      {
         recordsById.put(record.getValue(table.getPrimaryKeyField()), record);
      }

      for(Serializable id : ids)
      {
         QRecord record = recordsById.get(id);
         if(record != null)
         {
            results.add(toResult(table, record));
            if(results.size() >= limitPerTable)
            {
               break;
            }
         }
      }
      return (results);
   }



   /*******************************************************************************
    ** Query one table for records whose search fields match the term.
    *******************************************************************************/
   private List<RecordSearchResult> searchTable(RecordSearchInput input, QTableMetaData table, String searchTerm, int limitPerTable) throws QException
   {
      ////////////////////////////////////////////////////////////////////////////
      // search only the fields this session can see (personalization may      //
      // remove fields for some users; hidden and password fields never match) //
      ////////////////////////////////////////////////////////////////////////////
      QTableMetaData personalized = TableMetaDataPersonalizerAction.execute(new TableMetaDataPersonalizerInput()
         .withTableName(table.getName())
         .withTableMetaData(table)
         .withInputSource(input.getInputSource()));

      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
      for(String fieldName : table.getSearchFields())
      {
         QFieldMetaData field = personalized.getFields().get(fieldName);
         if(field == null || field.getIsHidden() || field.getType() == null || field.getType().needsMasked())
         {
            continue;
         }

         if(field.getType().isStringLike())
         {
            filter.withCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.CONTAINS, searchTerm).withOption(CriteriaOption.CASE_INSENSITIVE));
         }
         else if(field.getType().isIntegral() && INTEGER_TERM.matcher(searchTerm).matches())
         {
            Serializable value = valueAsFieldType(field, searchTerm);
            if(value != null)
            {
               filter.withCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, value));
            }
         }
      }

      if(CollectionUtils.nullSafeIsEmpty(filter.getCriteria()))
      {
         return (new ArrayList<>());
      }

      filter.withOrderBy(new QFilterOrderBy(table.getPrimaryKeyField()));
      filter.withLimit(limitPerTable);

      List<RecordSearchResult> results = new ArrayList<>();
      for(QRecord record : new QueryAction().execute(newQueryInput(input, table, filter)).getRecords())
      {
         results.add(toResult(table, record));
      }
      return (results);
   }



   /*******************************************************************************
    ** A query of the table as the search's caller, with display values (for
    ** record labels).
    *******************************************************************************/
   private static QueryInput newQueryInput(RecordSearchInput input, QTableMetaData table, QQueryFilter filter)
   {
      QueryInput queryInput = new QueryInput(table.getName());
      queryInput.setInputSource(input.getInputSource());
      queryInput.setFilter(filter);
      queryInput.setShouldGenerateDisplayValues(true);
      queryInput.setShouldTranslatePossibleValues(true);
      queryInput.setTimeoutSeconds(QUERY_TIMEOUT_SECONDS);
      queryInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
      return (queryInput);
   }



   /*******************************************************************************
    ** The search result for a record.
    *******************************************************************************/
   private static RecordSearchResult toResult(QTableMetaData table, QRecord record)
   {
      String recordLabel = record.getRecordLabel() != null ? record.getRecordLabel() : QValueFormatter.formatRecordLabel(table, record);
      return (new RecordSearchResult()
         .withTableName(table.getName())
         .withTableLabel(table.getLabel())
         .withRecordId(record.getValue(table.getPrimaryKeyField()))
         .withRecordLabel(recordLabel));
   }



   /*******************************************************************************
    ** The value as the field's type, or null if it does not convert.
    *******************************************************************************/
   private static Serializable valueAsFieldType(QFieldMetaData field, Serializable value)
   {
      try
      {
         return (ValueUtils.getValueAsFieldType(field.getType(), value));
      }
      catch(Exception e)
      {
         ///////////////////////////////////////////////////////////////////
         // e.g., a term too large for an INTEGER field - it cannot match //
         ///////////////////////////////////////////////////////////////////
         return (null);
      }
   }

}
