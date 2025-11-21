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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.interfaces.AggregateInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.FilterValidationHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.SelectionValidationHelper;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Action to run an aggregate against a table.
 **
 *******************************************************************************/
public class AggregateAction
{
   private static final QLogger LOG = QLogger.getLogger(AggregateAction.class);

   private AggregateInterface aggregateInterface;



   /*******************************************************************************
    **
    *******************************************************************************/
   public AggregateOutput execute(AggregateInput aggregateInput) throws QException
   {
      ActionHelper.validateSession(aggregateInput);

      if(aggregateInput.getTableName() == null)
      {
         throw (new QException("Table name was not specified in aggregate input"));
      }

      QTableMetaData table = aggregateInput.getTable();
      if(table == null)
      {
         throw (new QException("A table named [" + aggregateInput.getTableName() + "] was not found in the active QInstance"));
      }

      table = TableMetaDataPersonalizerAction.execute(aggregateInput);
      aggregateInput.setTableMetaData(table);

      FilterValidationHelper.validateFieldNamesInFilter(aggregateInput);
      validateFieldNames(aggregateInput);

      QBackendMetaData backend = aggregateInput.getBackend();

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // apply any available field behaviors to the filter (noting that, if anything changes, a new filter is returned) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      aggregateInput.setFilter(ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getQInstance(), table, aggregateInput.getFilter(), Collections.emptySet()));

      QueryStat queryStat = QueryStatManager.newQueryStat(backend, table, aggregateInput.getFilter(), AggregateAction.class.getSimpleName());

      QBackendModuleDispatcher qBackendModuleDispatcher = new QBackendModuleDispatcher();
      QBackendModuleInterface  qModule                  = qBackendModuleDispatcher.getQBackendModule(aggregateInput.getBackend());

      aggregateInterface = qModule.getAggregateInterface();
      aggregateInterface.setQueryStat(queryStat);
      AggregateOutput aggregateOutput = aggregateInterface.execute(aggregateInput);

      // todo, maybe, not real important? ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.READ, QContext.getQInstance(), table, aggregateOutput.getResults(), null);
      //  issue being, the signature there... it takes a list of QRecords, which aren't what we have...
      //  do we want to ... idk, refactor all these behavior deals?  hmm... maybe a new interface/ for ones that do reads?  not sure.

      if(queryStat != null)
      {
         queryStat.setRecordCount(aggregateOutput.getResults().size());
         QueryStatManager.getInstance().add(queryStat);
      }

      return aggregateOutput;
   }



   /***************************************************************************
    * make sure field names in the aggregate and group by sections are valid.
    ***************************************************************************/
   private static void validateFieldNames(AggregateInput aggregateInput) throws QException
   {
      Set<String> inputFieldNames = new LinkedHashSet<>();
      for(Aggregate aggregate : CollectionUtils.nonNullList(aggregateInput.getAggregates()))
      {
         CollectionUtils.addIfNotNull(inputFieldNames, aggregate.getFieldName());
      }

      for(GroupBy groupBy : CollectionUtils.nonNullList(aggregateInput.getGroupBys()))
      {
         CollectionUtils.addIfNotNull(inputFieldNames, groupBy.getFieldName());
      }

      if(CollectionUtils.nullSafeHasContents(inputFieldNames))
      {
         List<String> unrecognizedFieldNames = SelectionValidationHelper.getUnrecognizedFieldNames(aggregateInput, inputFieldNames);
         if(CollectionUtils.nullSafeHasContents(unrecognizedFieldNames))
         {
            throw (new QException("AggregateInput contained " + unrecognizedFieldNames.size() + " unrecognized field name" + StringUtils.plural(unrecognizedFieldNames) + ": " + StringUtils.join(",", unrecognizedFieldNames)));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void cancel()
   {
      if(aggregateInterface == null)
      {
         LOG.warn("aggregateInterface object was null when requested to cancel");
         return;
      }

      aggregateInterface.cancelAction();
   }
}
