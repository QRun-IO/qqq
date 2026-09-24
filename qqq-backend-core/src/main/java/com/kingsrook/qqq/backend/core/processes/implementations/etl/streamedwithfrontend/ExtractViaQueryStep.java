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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend;


import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.reporting.DistinctFilteringRecordPipe;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Generic implementation of an ExtractStep - that runs a Query action for a
 ** specified table.
 **
 ** If a query is specified from the caller (e.g., using the process Callback
 ** mechanism), that will be used.  Else a filter (object or json) in
 ** StreamedETLWithFrontendProcess.FIELD_DEFAULT_QUERY_FILTER will be checked.
 *******************************************************************************/
public class ExtractViaQueryStep extends AbstractExtractStep
{
   private static final QLogger LOG = QLogger.getLogger(ExtractViaQueryStep.class);

   public static final String FIELD_SOURCE_TABLE = "sourceTable";

   private QQueryFilter queryFilter;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void preRun(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      super.preRun(runBackendStepInput, runBackendStepOutput);
      queryFilter = getQueryFilter(runBackendStepInput);
   }



   /*******************************************************************************
    ** Execute the backend step - using the request as input, and the result as output.
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      //////////////////////////////////////////////////////////////////
      // clone the filter, since we're going to edit it (set a limit) //
      //////////////////////////////////////////////////////////////////
      QQueryFilter filterClone = queryFilter.clone();

      //////////////////////////////////////////////////////////////////////////////////////////////
      // if there's a limit in the extract step (e.g., the 20-record limit on the preview screen) //
      // then set that limit in the filter - UNLESS - there's already a limit in the filter for   //
      // a smaller number of records.                                                             //
      //////////////////////////////////////////////////////////////////////////////////////////////
      if(getLimit() != null)
      {
         if(filterClone.getLimit() != null && filterClone.getLimit() < getLimit())
         {
            LOG.trace("Using filter's limit [" + filterClone.getLimit() + "] rather than step's limit [" + getLimit() + "]");
         }
         else
         {
            filterClone.setLimit(getLimit());
         }
      }

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(runBackendStepInput.getValueString(FIELD_SOURCE_TABLE));
      queryInput.setFilter(filterClone);
      getQueryJoinsForOrderByIfNeeded(queryFilter).forEach(queryJoin -> queryInput.withQueryJoin(queryJoin));
      queryInput.setSelectDistinct(true);
      queryInput.setRecordPipe(getRecordPipe());
      queryInput.setAsyncJobCallback(runBackendStepInput.getAsyncJobCallback());

      if(runBackendStepInput.getValuePrimitiveBoolean(StreamedETLWithFrontendProcess.FIELD_FETCH_HEAVY_FIELDS))
      {
         queryInput.setShouldFetchHeavyFields(true);
      }
      if(runBackendStepInput.getValuePrimitiveBoolean(StreamedETLWithFrontendProcess.FIELD_INCLUDE_ASSOCIATIONS))
      {
         queryInput.setIncludeAssociations(true);
      }

      customizeInputPreQuery(runBackendStepInput, queryInput);

      new QueryAction().execute(queryInput);

      ///////////////////////////////////////////////////////////////////
      // output is done into the pipe - so, nothing for us to do here. //
      ///////////////////////////////////////////////////////////////////
   }



   /*******************************************************************************
    ** chance for sub-classes to change things about the query input, if they want.
    *******************************************************************************/
   protected void customizeInputPreQuery(RunBackendStepInput runBackendStepInput, QueryInput queryInput)
   {
      if(runBackendStepInput.getCallback() != null)
      {
         runBackendStepInput.getCallback().customizeInputPreQuery(runBackendStepInput, queryInput);
      }
   }



   /*******************************************************************************
    ** If the queryFilter has order-by fields from a joinTable, then create QueryJoins
    ** for each such table - marked as LEFT, and select=true.
    **
    ** This is under the rationale that, the filter would have come from the frontend,
    ** which would be doing outer-join semantics for a column being shown (but not filtered by).
    ** If the table IS filtered by, it's still OK to do a LEFT, as we'll only get rows
    ** that match.
    **
    ** Also, they are being select=true'ed so that the DISTINCT clause works (since
    ** process queries always try to be DISTINCT).
    *******************************************************************************/
   private List<QueryJoin> getQueryJoinsForOrderByIfNeeded(QQueryFilter queryFilter)
   {
      if(queryFilter == null)
      {
         return (Collections.emptyList());
      }

      List<QueryJoin> rs          = new ArrayList<>();
      Set<String>     addedTables = new HashSet<>();
      for(QFilterOrderBy filterOrderBy : CollectionUtils.nonNullList(queryFilter.getOrderBys()))
      {
         if(filterOrderBy.getFieldName().contains("."))
         {
            String tableName = filterOrderBy.getFieldName().split("\\.")[0];
            if(!addedTables.contains(tableName))
            {
               rs.add(new QueryJoin(tableName).withType(QueryJoin.Type.LEFT).withSelect(true));
            }
            addedTables.add(tableName);
         }
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer doCount(RunBackendStepInput runBackendStepInput) throws QException
   {
      CountInput countInput = new CountInput();
      countInput.setTableName(runBackendStepInput.getValueString(FIELD_SOURCE_TABLE));
      countInput.setFilter(queryFilter);
      getQueryJoinsForOrderByIfNeeded(queryFilter).forEach(queryJoin -> countInput.withQueryJoin(queryJoin));
      countInput.setIncludeDistinctCount(true);
      CountOutput countOutput = new CountAction().execute(countInput);
      Integer     count       = countOutput.getDistinctCount();

      /////////////////////////////////////////////////////////////////////////////////////////////
      // in case the filter we're running has a limit, but the count found more than that limit, //
      // well then, just return that limit - as the process won't run on more rows than that.    //
      /////////////////////////////////////////////////////////////////////////////////////////////
      if(count != null & queryFilter.getLimit() != null && count > queryFilter.getLimit())
      {
         count = queryFilter.getLimit();
      }

      return count;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected QQueryFilter getQueryFilter(RunBackendStepInput runBackendStepInput) throws QException
   {
      String       queryFilterJson    = runBackendStepInput.getValueString("queryFilterJson");
      Serializable defaultQueryFilter = runBackendStepInput.getValue(StreamedETLWithFrontendProcess.FIELD_DEFAULT_QUERY_FILTER);

      //////////////////////////////////////////////////////////////////////////////////////
      // if the queryFilterJson field is populated, read the filter from it and return it //
      //////////////////////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(queryFilterJson))
      {
         return getQueryFilterFromJson(queryFilterJson, "Error loading query filter from json field");
      }
      else if(runBackendStepInput.getCallback() != null && runBackendStepInput.getCallback().getQueryFilter() != null)
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // else, try to get filter from process callback.  if we got one, store it as a process value for later steps //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         QQueryFilter queryFilter = runBackendStepInput.getCallback().getQueryFilter();
         runBackendStepInput.addValue("queryFilterJson", JsonUtils.toJson(queryFilter));
         return (queryFilter);
      }
      else if(defaultQueryFilter instanceof QQueryFilter filter)
      {
         /////////////////////////////////////////////////////////////////////////////
         // else, see if a defaultQueryFilter was specified as a QueryFilter object //
         /////////////////////////////////////////////////////////////////////////////
         return (filter);
      }
      else if(defaultQueryFilter instanceof String string)
      {
         /////////////////////////////////////////////////////////////////////////////
         // else, see if a defaultQueryFilter was specified as a JSON string
         /////////////////////////////////////////////////////////////////////////////
         return getQueryFilterFromJson(string, "Error loading default query filter from json");
      }
      else if(StringUtils.hasContent(runBackendStepInput.getValueString("filterJSON")))
      {
         ///////////////////////////////////////////////////////////////////////
         // else, check for filterJSON from a frontend launching of a process //
         ///////////////////////////////////////////////////////////////////////
         return getQueryFilterFromJson(runBackendStepInput.getValueString("filterJSON"), "Error loading default query filter from json");
      }
      else if(StringUtils.hasContent(runBackendStepInput.getValueString("recordIds")))
      {
         //////////////////////////////////////////////////////////////////////
         // else, check for recordIds from a frontend launching of a process //
         //////////////////////////////////////////////////////////////////////
         QTableMetaData table = QContext.getQInstance().getTable(runBackendStepInput.getValueString(FIELD_SOURCE_TABLE));
         if(table == null)
         {
            throw (new QException("source table name was not set - could not load records by id"));
         }
         String             recordIds = runBackendStepInput.getValueString("recordIds");
         Serializable[]     split     = recordIds.split(",");
         List<Serializable> idStrings = Arrays.stream(split).toList();
         return (new QQueryFilter().withCriteria(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, idStrings)));
      }

      throw (new CouldNotFindQueryFilterForExtractStepException("No records were selected for running this process."));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QQueryFilter getQueryFilterFromJson(String queryFilterJson, String message) throws QException
   {
      try
      {
         return (JsonUtils.toObject(queryFilterJson, QQueryFilter.class));
      }
      catch(IOException e)
      {
         throw new QException(message, e);
      }
   }



   /*******************************************************************************
    ** Create the record pipe to be used for this process step.
    **
    *******************************************************************************/
   @Override
   public RecordPipe createRecordPipe(RunBackendStepInput runBackendStepInput, Integer overrideCapacity)
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if the filter has order-bys from a join-table, then we have to include that join-table in the SELECT clause,                 //
      // which means we need to do distinct "manually", e.g., via a DistinctFilteringRecordPipe                                       //
      // todo - really, wouldn't this only be if it's a many-join?  but that's not completely trivial to detect, given join-chains... //
      //  as it is, we may end up using DistinctPipe in some cases that we need it - which isn't an error, just slightly sub-optimal. //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<QueryJoin> queryJoinsForOrderByIfNeeded = getQueryJoinsForOrderByIfNeeded(queryFilter);
      boolean         needDistinctPipe             = CollectionUtils.nullSafeHasContents(queryJoinsForOrderByIfNeeded);

      if(needDistinctPipe)
      {
         String         sourceTableName = runBackendStepInput.getValueString(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE);
         QTableMetaData sourceTable     = QContext.getQInstance().getTable(sourceTableName);
         return (new DistinctFilteringRecordPipe(new UniqueKey(sourceTable.getPrimaryKeyField()), overrideCapacity));
      }
      else
      {
         return (super.createRecordPipe(runBackendStepInput, overrideCapacity));
      }
   }
}
