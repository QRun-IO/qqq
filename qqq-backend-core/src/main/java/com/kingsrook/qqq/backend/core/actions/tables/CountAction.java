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
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.interfaces.CountInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.FilterValidationHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;


/*******************************************************************************
 ** Action to run a count against a table.
 **
 *******************************************************************************/
public class CountAction
{
   private static final QLogger LOG = QLogger.getLogger(CountAction.class);

   private CountInterface countInterface;



   /*******************************************************************************
    **
    *******************************************************************************/
   public CountOutput execute(CountInput countInput) throws QException
   {
      ActionHelper.validateSession(countInput);

      if(countInput.getTableName() == null)
      {
         throw (new QException("Table name was not specified in count input"));
      }

      QTableMetaData table = countInput.getTable();
      if(table == null)
      {
         throw (new QException("A table named [" + countInput.getTableName() + "] was not found in the active QInstance"));
      }

      table = TableMetaDataPersonalizerAction.execute(countInput);
      countInput.setTableMetaData(table);

      FilterValidationHelper.validateFieldNamesInFilter(countInput);

      QBackendMetaData backend = countInput.getBackend();

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // apply any available field behaviors to the filter (noting that, if anything changes, a new filter is returned) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      countInput.setFilter(ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getQInstance(), table, countInput.getFilter(), Collections.emptySet()));

      QueryStat queryStat = QueryStatManager.newQueryStat(backend, table, countInput.getFilter(), CountAction.class.getSimpleName());

      QBackendModuleDispatcher qBackendModuleDispatcher = new QBackendModuleDispatcher();
      QBackendModuleInterface  qModule                  = qBackendModuleDispatcher.getQBackendModule(countInput.getBackend());

      countInterface = qModule.getCountInterface();
      countInterface.setQueryStat(queryStat);
      CountOutput countOutput = countInterface.execute(countInput);

      if(queryStat != null)
      {
         queryStat.setRecordCount(1);
         QueryStatManager.getInstance().add(queryStat);
      }

      return countOutput;
   }



   /*******************************************************************************
    ** shorthand way to call for the most common use-case, when you just want the
    ** count to be returned, and you just want to pass in a table name and filter.
    *******************************************************************************/
   public static Integer execute(String tableName, QQueryFilter filter) throws QException
   {
      CountAction countAction = new CountAction();
      CountInput  countInput  = new CountInput();
      countInput.setTableName(tableName);
      countInput.setFilter(filter);
      CountOutput countOutput = countAction.execute(countInput);
      return (countOutput.getCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void cancel()
   {
      if(countInterface == null)
      {
         LOG.warn("countInterface object was null when requested to cancel");
         return;
      }

      countInterface.cancelAction();
   }
}
