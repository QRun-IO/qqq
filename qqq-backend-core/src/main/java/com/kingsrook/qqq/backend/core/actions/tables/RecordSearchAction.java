/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
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
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


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
 *******************************************************************************/
public class RecordSearchAction
{
   public static final int DEFAULT_LIMIT_PER_TABLE = 5;
   public static final int MAX_LIMIT_PER_TABLE     = 25;
   public static final int MAX_SEARCH_TERM_LENGTH  = 100;

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

      QInstance          qInstance = QContext.getQInstance();
      RecordSearchOutput output    = new RecordSearchOutput();
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

         output.getResults().addAll(searchTable(input, table, searchTerm, limitPerTable));
      }

      return (output);
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
            Serializable value = integerValue(field, searchTerm);
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

      QueryInput queryInput = new QueryInput(table.getName());
      queryInput.setInputSource(input.getInputSource());
      queryInput.setFilter(filter);
      queryInput.setShouldGenerateDisplayValues(true);
      queryInput.setShouldTranslatePossibleValues(true);
      queryInput.setTimeoutSeconds(QUERY_TIMEOUT_SECONDS);
      queryInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

      List<RecordSearchResult> results = new ArrayList<>();
      for(QRecord record : new QueryAction().execute(queryInput).getRecords())
      {
         String recordLabel = record.getRecordLabel() != null ? record.getRecordLabel() : QValueFormatter.formatRecordLabel(table, record);
         results.add(new RecordSearchResult()
            .withTableName(table.getName())
            .withTableLabel(table.getLabel())
            .withRecordId(record.getValue(table.getPrimaryKeyField()))
            .withRecordLabel(recordLabel));
      }
      return (results);
   }



   /*******************************************************************************
    ** The term as the field's integer type, or null if it does not fit.
    *******************************************************************************/
   private static Serializable integerValue(QFieldMetaData field, String searchTerm)
   {
      try
      {
         return (ValueUtils.getValueAsFieldType(field.getType(), searchTerm));
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
