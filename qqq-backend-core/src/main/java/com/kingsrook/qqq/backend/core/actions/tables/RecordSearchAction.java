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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.CriteriaOption;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchDetail;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchHighlight;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchHighlightMatch;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchProviderState;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchProviderStatus;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchSort;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchTableSummary;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
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
 ** fields match when they equal a numeric term.  Results per table are limited,
 ** and may be paged (offset) and sorted (relevance, or most recently modified).
 **
 ** If the instance has a {@link RecordSearchProviderInterface record search
 ** provider}, the tables it claims are searched by it instead (its hits are
 ** re-read through QueryAction, so the same security applies), falling back to
 ** the default search for those tables if the provider fails.  The output says
 ** what searched each table, and whether (and why) the provider failed.
 **
 ** Each result is described from the freshly read record, using only the
 ** fields the session can see: plain-text highlights of the search fields that
 ** match the term, a few other search field values (details), and when it was
 ** last modified.  Fields in the record label are neither highlighted nor
 ** detailed (the label already shows them), nor is the primary key detailed.
 *******************************************************************************/
public class RecordSearchAction
{
   private static final QLogger LOG = QLogger.getLogger(RecordSearchAction.class);

   public static final int DEFAULT_LIMIT_PER_TABLE    = 5;
   public static final int MAX_LIMIT_PER_TABLE        = 25;
   public static final int MAX_SEARCH_TERM_LENGTH     = 100;
   public static final int MAX_PROVIDER_IDS_PER_TABLE = 100;
   public static final int MAX_OFFSET                 = 1000;

   public static final int MAX_HIGHLIGHTS_PER_RESULT = 3;
   public static final int MAX_DETAILS_PER_RESULT    = 3;
   public static final int HIGHLIGHT_SNIPPET_LENGTH  = 160;

   private static final int    HIGHLIGHT_SNIPPET_LEAD        = 40;
   private static final int    HIGHLIGHT_MAX_SCANNED_CHARS   = 10_000;
   private static final int    HIGHLIGHT_MAX_MATCHES         = 50;
   private static final int    HIGHLIGHT_MAX_TOKENS          = 10;
   private static final int    PROVIDER_HIGHLIGHT_MAX_LENGTH = 1000;
   private static final int    FAILURE_MESSAGE_MAX_LENGTH    = 200;
   static final String         ELLIPSIS                      = "…";

   private static final int     QUERY_TIMEOUT_SECONDS = 30;
   private static final Pattern INTEGER_TERM          = Pattern.compile("-?\\d{1,18}");
   private static final Pattern WHITESPACE            = Pattern.compile("\\s+");

   ////////////////////////////////////////////////////////////////////////////
   // how long the provider took in this search (for the log line that each //
   // search writes); an action instance runs one search.                   //
   ////////////////////////////////////////////////////////////////////////////
   private long providerMillis = 0;



   /*******************************************************************************
    ** The results of searching one table.
    *******************************************************************************/
   private record TableSearch(List<RecordSearchResult> results, boolean hasMore, Integer totalCount)
   {
   }



   /*******************************************************************************
    ** What the provider did: its results by table (only for tables it searched),
    ** the tables it claimed but failed to search, and its status.
    *******************************************************************************/
   private record ProviderOutcome(Map<String, TableSearch> resultsByTable, Set<String> fallbackTableNames, RecordSearchProviderStatus status)
   {
   }



   /*******************************************************************************
    ** The search's parameters, once validated.
    *******************************************************************************/
   private record SearchParameters(String searchTerm, List<String> tokens, int limitPerTable, int offset, RecordSearchSort sort)
   {
   }



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

      int              limitPerTable = input.getLimitPerTable() == null ? DEFAULT_LIMIT_PER_TABLE : Math.max(1, Math.min(MAX_LIMIT_PER_TABLE, input.getLimitPerTable()));
      int              offset        = input.getOffset() == null ? 0 : Math.max(0, Math.min(MAX_OFFSET, input.getOffset()));
      RecordSearchSort sort          = input.getSort() == null ? RecordSearchSort.RELEVANCE : input.getSort();
      SearchParameters parameters    = new SearchParameters(searchTerm, termTokens(searchTerm), limitPerTable, offset, sort);

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

      /////////////////////////////////////////////////////////////////////////////
      // the fields this session can see, per table (personalization may remove //
      // fields for some users) - only these are searched, highlighted or shown //
      /////////////////////////////////////////////////////////////////////////////
      Map<String, QTableMetaData> personalizedTables = new HashMap<>();
      for(QTableMetaData table : tables)
      {
         personalizedTables.put(table.getName(), TableMetaDataPersonalizerAction.execute(new TableMetaDataPersonalizerInput()
            .withTableName(table.getName())
            .withTableMetaData(table)
            .withInputSource(input.getInputSource())));
      }

      ///////////////////////////////////////////////////////////////////////////
      // tables a provider searched use its results; all others (including the //
      // provider's tables, if it failed) use the default search.  either way, //
      // results are output in instance table order.                           //
      ///////////////////////////////////////////////////////////////////////////
      long            start           = System.currentTimeMillis();
      ProviderOutcome providerOutcome = null;
      try
      {
         providerOutcome = searchWithProvider(qInstance, input, tables, personalizedTables, parameters);

         RecordSearchOutput output = new RecordSearchOutput().withProviderStatus(providerOutcome.status());
         for(QTableMetaData table : tables)
         {
            TableSearch        tableSearch = providerOutcome.resultsByTable().get(table.getName());
            RecordSearchSource source      = RecordSearchSource.PROVIDER;
            if(tableSearch == null)
            {
               tableSearch = searchTable(input, table, personalizedTables.get(table.getName()), parameters);
               source = providerOutcome.fallbackTableNames().contains(table.getName()) ? RecordSearchSource.FALLBACK : RecordSearchSource.DEFAULT;
            }

            output.getResults().addAll(tableSearch.results());
            output.getTables().add(new RecordSearchTableSummary()
               .withTableName(table.getName())
               .withTableLabel(table.getLabel())
               .withResultCount(tableSearch.results().size())
               .withHasMore(tableSearch.hasMore())
               .withTotalCount(tableSearch.totalCount())
               .withSource(source));
         }

         LOG.info("Record search", logPair("termLength", searchTerm.length()), logPair("tableCount", tables.size()), logPair("resultCount", output.getResults().size()), logPair("offset", offset), logPair("sort", sort),
            logPair("provider", providerOutcome.status().getStatus()), logPair("providerFailureClass", providerOutcome.status().getFailureClass()), logPair("providerMillis", providerMillis), logPair("millis", System.currentTimeMillis() - start));
         return (output);
      }
      catch(QException | RuntimeException e)
      {
         LOG.warn("Record search failed", e, logPair("termLength", searchTerm.length()), logPair("tableCount", tables.size()), logPair("provider", providerOutcome == null ? null : providerOutcome.status().getStatus()),
            logPair("errorClass", e.getClass().getName()), logPair("millis", System.currentTimeMillis() - start));
         throw (e);
      }
   }



   /*******************************************************************************
    ** Search the tables that the instance's record search provider (if any)
    ** claims.  Returns results by table name, for only those tables the provider
    ** searched - so none if there is no provider, it claims no tables, or it
    ** fails (which is logged, reported in the status, and leaves its tables to
    ** the default search).
    *******************************************************************************/
   private ProviderOutcome searchWithProvider(QInstance qInstance, RecordSearchInput input, List<QTableMetaData> tables, Map<String, QTableMetaData> personalizedTables, SearchParameters parameters) throws QException
   {
      QCodeReference providerReference = qInstance.getRecordSearchProvider();
      if(providerReference == null || tables.isEmpty())
      {
         return (new ProviderOutcome(Map.of(), Set.of(), new RecordSearchProviderStatus().withStatus(RecordSearchProviderState.NONE)));
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
            return (new ProviderOutcome(Map.of(), Set.of(), new RecordSearchProviderStatus().withStatus(RecordSearchProviderState.NONE)));
         }

         long providerStart = System.currentTimeMillis();
         providerOutput = provider.search(new RecordSearchInput()
            .withSearchTerm(parameters.searchTerm())
            .withTableNames(new ArrayList<>(claimedTables.keySet()))
            .withLimitPerTable(parameters.limitPerTable())
            .withOffset(parameters.offset())
            .withSort(parameters.sort())
            .withInputSource(input.getInputSource()));
         providerMillis = System.currentTimeMillis() - providerStart;
      }
      catch(Exception | LinkageError e)
      {
         LOG.warn("Error in record search provider - searching its tables with the default search instead", e, logPair("provider", providerReference.getName()), logPair("tableNames", claimedTables.keySet()), logPair("errorClass", e.getClass().getName()));
         return (new ProviderOutcome(Map.of(), new HashSet<>(claimedTables.keySet()), failedStatus(e)));
      }

      /////////////////////////////////////////////////////////////////////
      // collect the provider's results per claimed table, in its order //
      // (ignoring results for other tables, and results without an id) //
      /////////////////////////////////////////////////////////////////////
      Map<String, List<RecordSearchResult>> providerResultsByTable = new HashMap<>();
      for(String tableName : claimedTables.keySet())
      {
         providerResultsByTable.put(tableName, new ArrayList<>());
      }

      List<RecordSearchResult> providerResults = providerOutput == null ? null : providerOutput.getResults();
      for(RecordSearchResult result : CollectionUtils.nonNullList(providerResults))
      {
         List<RecordSearchResult> tableResults = result == null ? null : providerResultsByTable.get(result.getTableName());
         if(tableResults != null && result.getRecordId() != null)
         {
            tableResults.add(result);
         }
      }

      Map<String, Integer>     totals         = providerOutput == null ? null : providerOutput.getTotalCountByTable();
      Map<String, TableSearch> resultsByTable = new HashMap<>();
      for(QTableMetaData table : claimedTables.values())
      {
         Integer totalCount = totals == null ? null : totals.get(table.getName());
         resultsByTable.put(table.getName(), readProviderResults(input, table, personalizedTables.get(table.getName()), providerResultsByTable.get(table.getName()), totalCount, parameters));
      }
      return (new ProviderOutcome(resultsByTable, Set.of(), new RecordSearchProviderStatus().withStatus(RecordSearchProviderState.OK)));
   }



   /*******************************************************************************
    ** The status of a provider that failed: the simple class name of the root
    ** cause, and - only when the failure is meant for users - its message.
    ** Other messages (which may name internal hosts) stay in the log.
    *******************************************************************************/
   static RecordSearchProviderStatus failedStatus(Throwable e)
   {
      Throwable      root = e;
      Set<Throwable> seen = new HashSet<>();
      while(root.getCause() != null && seen.add(root))
      {
         root = root.getCause();
      }

      QUserFacingException userFacingException = ExceptionUtils.findClassInRootChain(e, QUserFacingException.class);
      String               message             = userFacingException == null ? null : firstLine(userFacingException.getMessage(), FAILURE_MESSAGE_MAX_LENGTH);

      return (new RecordSearchProviderStatus()
         .withStatus(RecordSearchProviderState.FAILED)
         .withFailureClass(root.getClass().getSimpleName())
         .withMessage(message));
   }



   /*******************************************************************************
    ** Re-read the records a provider returned for one table, by primary key,
    ** through QueryAction - so only records that still exist and that the
    ** session may read are output - keeping the provider's order (and its
    ** scores and highlights).
    *******************************************************************************/
   private TableSearch readProviderResults(RecordSearchInput input, QTableMetaData table, QTableMetaData personalized, List<RecordSearchResult> providerResults, Integer totalCount, SearchParameters parameters) throws QException
   {
      QFieldMetaData                        primaryKeyField    = table.getField(table.getPrimaryKeyField());
      Map<Serializable, RecordSearchResult> providerResultById = new LinkedHashMap<>();
      for(RecordSearchResult providerResult : providerResults)
      {
         if(providerResultById.size() >= MAX_PROVIDER_IDS_PER_TABLE)
         {
            break;
         }

         Serializable id = valueAsFieldType(primaryKeyField, providerResult.getRecordId());
         if(id != null)
         {
            providerResultById.putIfAbsent(id, providerResult);
         }
      }

      boolean                  totalHasMore = totalCount != null && totalCount > parameters.offset() + parameters.limitPerTable();
      List<RecordSearchResult> results      = new ArrayList<>();
      if(providerResultById.isEmpty())
      {
         return (new TableSearch(results, totalHasMore, totalCount));
      }

      QQueryFilter filter = new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, new ArrayList<>(providerResultById.keySet())));

      Map<Serializable, QRecord> recordsById = new HashMap<>();
      for(QRecord record : new QueryAction().execute(newQueryInput(input, table, filter)).getRecords())
      {
         recordsById.put(record.getValue(table.getPrimaryKeyField()), record);
      }

      boolean hasMore = totalHasMore;
      for(Map.Entry<Serializable, RecordSearchResult> entry : providerResultById.entrySet())
      {
         QRecord record = recordsById.get(entry.getKey());
         if(record != null)
         {
            if(results.size() >= parameters.limitPerTable())
            {
               hasMore = true;
               break;
            }
            results.add(toResult(table, personalized, record, parameters.tokens(), entry.getValue()));
         }
      }

      return (new TableSearch(results, hasMore, totalCount));
   }



   /*******************************************************************************
    ** Query one table for records whose search fields match the term.
    *******************************************************************************/
   private TableSearch searchTable(RecordSearchInput input, QTableMetaData table, QTableMetaData personalized, SearchParameters parameters) throws QException
   {
      String searchTerm = parameters.searchTerm();

      ////////////////////////////////////////////////////////////////////////////
      // search only the fields this session can see (personalization may      //
      // remove fields for some users; hidden and password fields never match) //
      ////////////////////////////////////////////////////////////////////////////
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
      for(String fieldName : table.getSearchFields())
      {
         QFieldMetaData field = visibleField(personalized, fieldName);
         if(field == null)
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
         return (new TableSearch(new ArrayList<>(), false, null));
      }

      QFieldMetaData modifyDateField = parameters.sort() == RecordSearchSort.RECENT ? modifyDateField(table) : null;
      if(modifyDateField != null)
      {
         filter.withOrderBy(new QFilterOrderBy(modifyDateField.getName(), false));
      }
      filter.withOrderBy(new QFilterOrderBy(table.getPrimaryKeyField()));
      filter.withSkip(parameters.offset());

      //////////////////////////////////////////////////////////////
      // one more than the limit, to know if there are more pages //
      //////////////////////////////////////////////////////////////
      filter.withLimit(parameters.limitPerTable() + 1);

      List<RecordSearchResult> results = new ArrayList<>();
      boolean                  hasMore = false;
      for(QRecord record : new QueryAction().execute(newQueryInput(input, table, filter)).getRecords())
      {
         if(results.size() >= parameters.limitPerTable())
         {
            hasMore = true;
            break;
         }
         results.add(toResult(table, personalized, record, parameters.tokens(), null));
      }
      return (new TableSearch(results, hasMore, null));
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
    ** The search result for a record: its label, plus (from only the fields the
    ** session can see) highlights, details and last-modified time.  Fields in the
    ** record label are not highlighted or detailed - the label shows them.  The
    ** label is never blank: a record whose label fields are empty is labeled by
    ** its primary key instead (a blank label is left out of the JSON response,
    ** and clients require one).
    *******************************************************************************/
   private static RecordSearchResult toResult(QTableMetaData table, QTableMetaData personalized, QRecord record, List<String> tokens, RecordSearchResult providerResult)
   {
      String recordLabel = record.getRecordLabel() != null ? record.getRecordLabel() : QValueFormatter.formatRecordLabel(table, record);
      if(!StringUtils.hasContent(recordLabel))
      {
         String primaryKeyDisplayValue = record.getDisplayValue(table.getPrimaryKeyField());
         recordLabel = StringUtils.hasContent(primaryKeyDisplayValue) ? primaryKeyDisplayValue : ValueUtils.getValueAsString(record.getValue(table.getPrimaryKeyField()));
      }
      RecordSearchResult result = new RecordSearchResult()
         .withTableName(table.getName())
         .withTableLabel(table.getLabel())
         .withRecordId(record.getValue(table.getPrimaryKeyField()))
         .withRecordLabel(recordLabel)
         .withScore(providerResult == null ? null : providerResult.getScore());

      QFieldMetaData modifyDateField = modifyDateField(table);
      if(modifyDateField != null && visibleField(personalized, modifyDateField.getName()) != null)
      {
         result.setUpdatedAt(valueAsInstant(record.getValue(modifyDateField.getName())));
      }

      Set<String>                 labelFields  = new HashSet<>(CollectionUtils.nonNullList(table.getRecordLabelFields()));
      List<RecordSearchHighlight> highlights   = new ArrayList<>();
      Set<String>                 highlighted  = new HashSet<>();
      List<QFieldMetaData>        detailFields = new ArrayList<>();
      for(String fieldName : CollectionUtils.nonNullList(table.getSearchFields()))
      {
         QFieldMetaData field = visibleField(personalized, fieldName);
         if(field == null || labelFields.contains(fieldName))
         {
            continue;
         }

         RecordSearchHighlight highlight = highlights.size() < MAX_HIGHLIGHTS_PER_RESULT ? highlight(field, fieldText(record, field), tokens) : null;
         if(highlight != null)
         {
            highlights.add(highlight);
            highlighted.add(fieldName);
         }
         else
         {
            detailFields.add(field);
         }
      }

      ////////////////////////////////////////////////////////////////////////////
      // a provider's highlights can show matches core can't find (e.g., stems) //
      // - but only for fields the session can see, and only if well-formed     //
      ////////////////////////////////////////////////////////////////////////////
      List<RecordSearchHighlight> providerHighlights = providerResult == null ? null : providerResult.getHighlights();
      for(RecordSearchHighlight providerHighlight : CollectionUtils.nonNullList(providerHighlights))
      {
         if(highlights.size() >= MAX_HIGHLIGHTS_PER_RESULT)
         {
            break;
         }

         QFieldMetaData field = providerHighlight == null ? null : visibleField(personalized, providerHighlight.getFieldName());
         if(field == null || labelFields.contains(field.getName()) || highlighted.contains(field.getName()))
         {
            continue;
         }

         RecordSearchHighlight validHighlight = validProviderHighlight(field, providerHighlight);
         if(validHighlight != null)
         {
            highlights.add(validHighlight);
            highlighted.add(field.getName());
         }
      }

      List<RecordSearchDetail> details = new ArrayList<>();
      for(QFieldMetaData field : detailFields)
      {
         String displayValue = fieldText(record, field);
         if(details.size() < MAX_DETAILS_PER_RESULT && !highlighted.contains(field.getName()) && !field.getName().equals(table.getPrimaryKeyField()) && StringUtils.hasContent(displayValue))
         {
            details.add(new RecordSearchDetail().withFieldName(field.getName()).withFieldLabel(field.getLabel()).withDisplayValue(displayValue));
         }
      }

      return (result
         .withHighlights(highlights.isEmpty() ? null : highlights)
         .withDetails(details.isEmpty() ? null : details));
   }



   /*******************************************************************************
    ** The field, if the session can see it in the (personalized) table: it
    ** exists, is not hidden, and is not a masked (e.g., password) field.
    *******************************************************************************/
   private static QFieldMetaData visibleField(QTableMetaData personalized, String fieldName)
   {
      QFieldMetaData field = fieldName == null || personalized == null ? null : personalized.getFields().get(fieldName);
      if(field == null || field.getIsHidden() || field.getType() == null || field.getType().needsMasked())
      {
         return (null);
      }
      return (field);
   }



   /*******************************************************************************
    ** The table's last-modified field (the one whose default value behavior is
    ** MODIFY_DATE), or null if it has none.
    *******************************************************************************/
   static QFieldMetaData modifyDateField(QTableMetaData table)
   {
      for(QFieldMetaData field : CollectionUtils.nonNullMap(table.getFields()).values())
      {
         if(field.getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class) == DynamicDefaultValueBehavior.MODIFY_DATE)
         {
            return (field);
         }
      }
      return (null);
   }



   /*******************************************************************************
    ** A field's value as plain text (its display value, else its raw value),
    ** with whitespace runs collapsed to single spaces; null if blank.
    *******************************************************************************/
   private static String fieldText(QRecord record, QFieldMetaData field)
   {
      String text = record.getDisplayValue(field.getName());
      if(!StringUtils.hasContent(text))
      {
         text = ValueUtils.getValueAsString(record.getValue(field.getName()));
      }
      return (StringUtils.hasContent(text) ? WHITESPACE.matcher(text.trim()).replaceAll(" ") : null);
   }



   /*******************************************************************************
    ** The distinct (ignoring case) whitespace-separated words of the search term.
    *******************************************************************************/
   static List<String> termTokens(String searchTerm)
   {
      List<String> tokens = new ArrayList<>();
      Set<String>  seen   = new HashSet<>();
      for(String token : WHITESPACE.split(searchTerm.trim()))
      {
         if(!token.isEmpty() && tokens.size() < HIGHLIGHT_MAX_TOKENS && seen.add(token.toLowerCase(Locale.ROOT)))
         {
            tokens.add(token);
         }
      }
      return (tokens);
   }



   /*******************************************************************************
    ** A highlight of a field's text: a snippet around its first match of any
    ** token (case-insensitive), with every match in the snippet.  Null if the
    ** text is blank or does not match.
    *******************************************************************************/
   static RecordSearchHighlight highlight(QFieldMetaData field, String text, List<String> tokens)
   {
      if(!StringUtils.hasContent(text))
      {
         return (null);
      }

      String      scanned = text.length() > HIGHLIGHT_MAX_SCANNED_CHARS ? text.substring(0, HIGHLIGHT_MAX_SCANNED_CHARS) : text;
      List<int[]> ranges  = new ArrayList<>();
      for(String token : tokens)
      {
         int from = 0;
         while(ranges.size() < HIGHLIGHT_MAX_MATCHES)
         {
            int at = indexOfIgnoreCase(scanned, token, from);
            if(at < 0)
            {
               break;
            }
            ranges.add(new int[] { at, at + token.length() });
            from = at + token.length();
         }
      }

      if(ranges.isEmpty())
      {
         return (null);
      }

      ///////////////////////////////////////////
      // sort, and merge overlapping or touching //
      ///////////////////////////////////////////
      ranges.sort(Comparator.comparingInt(r -> r[0]));
      List<int[]> merged = new ArrayList<>();
      for(int[] range : ranges)
      {
         int[] last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
         if(last != null && range[0] <= last[1])
         {
            last[1] = Math.max(last[1], range[1]);
         }
         else
         {
            merged.add(new int[] { range[0], range[1] });
         }
      }

      /////////////////////////////////////////////////////////////
      // the snippet: the whole text if short, else a window that //
      // starts a little before the first match                   //
      /////////////////////////////////////////////////////////////
      int start = 0;
      int end   = text.length();
      if(text.length() > HIGHLIGHT_SNIPPET_LENGTH)
      {
         start = Math.max(0, merged.get(0)[0] - HIGHLIGHT_SNIPPET_LEAD);
         end = Math.min(text.length(), start + HIGHLIGHT_SNIPPET_LENGTH);
         start = Math.max(0, end - HIGHLIGHT_SNIPPET_LENGTH);
         if(start > 0 && Character.isLowSurrogate(text.charAt(start)))
         {
            start++;
         }
         if(end < text.length() && Character.isHighSurrogate(text.charAt(end - 1)))
         {
            end--;
         }
      }

      String prefix = start > 0 ? ELLIPSIS : "";
      String suffix = end < text.length() ? ELLIPSIS : "";
      int    shift  = prefix.length() - start;

      List<RecordSearchHighlightMatch> matches = new ArrayList<>();
      for(int[] range : merged)
      {
         int matchStart = Math.max(range[0], start);
         int matchEnd   = Math.min(range[1], end);
         if(matchStart < matchEnd)
         {
            matches.add(new RecordSearchHighlightMatch().withStart(matchStart + shift).withEnd(matchEnd + shift));
         }
      }

      return (new RecordSearchHighlight()
         .withFieldName(field.getName())
         .withFieldLabel(field.getLabel())
         .withText(prefix + text.substring(start, end) + suffix)
         .withMatches(matches));
   }



   /*******************************************************************************
    ** Index of the needle in the text at or after from, ignoring case; -1 if none.
    *******************************************************************************/
   private static int indexOfIgnoreCase(String text, String needle, int from)
   {
      for(int i = from; i <= text.length() - needle.length(); i++)
      {
         if(text.regionMatches(true, i, needle, 0, needle.length()))
         {
            return (i);
         }
      }
      return (-1);
   }



   /*******************************************************************************
    ** A copy of a provider's highlight, labeled by core, if it is well-formed:
    ** non-blank text of bounded length, and at least one match, each within the
    ** text, sorted and non-overlapping.  Null otherwise.
    *******************************************************************************/
   static RecordSearchHighlight validProviderHighlight(QFieldMetaData field, RecordSearchHighlight providerHighlight)
   {
      String text = providerHighlight.getText();
      if(!StringUtils.hasContent(text) || text.length() > PROVIDER_HIGHLIGHT_MAX_LENGTH || CollectionUtils.nullSafeIsEmpty(providerHighlight.getMatches()))
      {
         return (null);
      }

      List<RecordSearchHighlightMatch> matches  = new ArrayList<>();
      int                              previous = 0;
      for(RecordSearchHighlightMatch match : providerHighlight.getMatches())
      {
         if(match == null || match.getStart() == null || match.getEnd() == null || match.getStart() < previous || match.getEnd() <= match.getStart() || match.getEnd() > text.length())
         {
            return (null);
         }
         matches.add(new RecordSearchHighlightMatch().withStart(match.getStart()).withEnd(match.getEnd()));
         previous = match.getEnd();
      }

      return (new RecordSearchHighlight()
         .withFieldName(field.getName())
         .withFieldLabel(field.getLabel())
         .withText(text)
         .withMatches(matches));
   }



   /*******************************************************************************
    ** The first line of a message, at most maxLength characters; null if blank.
    *******************************************************************************/
   private static String firstLine(String message, int maxLength)
   {
      if(!StringUtils.hasContent(message))
      {
         return (null);
      }
      String line = message.strip().lines().findFirst().orElse("").strip();
      return (line.length() > maxLength ? line.substring(0, maxLength - 1) + ELLIPSIS : line);
   }



   /*******************************************************************************
    ** The value as an Instant, or null if it is not one.
    *******************************************************************************/
   private static Instant valueAsInstant(Serializable value)
   {
      try
      {
         return (ValueUtils.getValueAsInstant(value));
      }
      catch(Exception e)
      {
         return (null);
      }
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
