/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.savedviews;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.savedviews.QuickSavedView;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedView;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Load the saved view records available to the current user for the specified
 * table.
 *
 * <p>Runs in an alternative mode, to just fetch one savedView, triggered by input
 * param: `savedViewId`</p>
 *
 * <p>Returns a list of QRecords, based on {@link SavedView} table, as `savedViewList`.
 * In single-mode, also returns the single savedView as `savedView`</p>
 *******************************************************************************/
public class QuerySavedViewProcess implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(QuerySavedViewProcess.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessMetaData getProcessMetaData()
   {
      return (new QProcessMetaData()
         .withName("querySavedView")
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withCode(new QCodeReference(QuerySavedViewProcess.class))
               .withName("query")
         )));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ActionHelper.validateSession(runBackendStepInput);
      Integer savedViewId = runBackendStepInput.getValueInteger("id");

      try
      {
         if(savedViewId != null)
         {
            GetInput input = new GetInput();
            input.setTableName(SavedView.TABLE_NAME);
            input.setPrimaryKey(savedViewId);

            GetOutput output = new GetAction().execute(input);
            if(output.getRecord() == null)
            {
               throw (new QNotFoundException("The requested view was not found."));
            }

            List<QRecord> recordList = List.of(output.getRecord());

            lookupQuickViews(runBackendStepInput, recordList);

            runBackendStepOutput.addRecord(recordList.get(0));
            runBackendStepOutput.addValue("savedView", recordList.get(0));
            runBackendStepOutput.addValue("savedViewList", (Serializable) recordList);
         }
         else
         {
            String tableName = runBackendStepInput.getValueString("tableName");

            QueryInput input = new QueryInput();
            input.setTableName(SavedView.TABLE_NAME);
            input.setFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("tableName", QCriteriaOperator.EQUALS, tableName))
               .withOrderBy(new QFilterOrderBy("label")));

            QueryOutput output = new QueryAction().execute(input);
            List<QRecord> savedViewRecords = output.getRecords();

            /////////////////////////////////////////////////////////////////////////////////////
            // possibly add data to the saved views for "quick views"                          //
            // note, we'll call this even if we didn't find any savedViewRecords, just in case //
            // an application wants to sub-class this process step, to, for example, hard-code //
            // some quick-views that get returned even if a user doesn't have any saved views  //
            /////////////////////////////////////////////////////////////////////////////////////
            lookupQuickViews(runBackendStepInput, savedViewRecords);

            runBackendStepOutput.setRecords(savedViewRecords);
            runBackendStepOutput.addValue("savedViewList", (Serializable) savedViewRecords);
         }
      }
      catch(QNotFoundException qnfe)
      {
         LOG.info("View not found", logPair("savedViewId", savedViewId));
         throw (qnfe);
      }
      catch(Exception e)
      {
         LOG.warn("Error querying for saved views", e);
         throw (e);
      }
   }



   /***************************************************************************
    * If the qInstance has a {@link QuickSavedView} table, then query for
    * quick-saved view records associated with the list of saved view records
    * that were found.
    *
    * <p>If corresponding {@link QuickSavedView}'s are found, then update the
    * saved-view records with the following values from the QuickSavedView:</p>
    * <ul>
    *    <li>type = "quickView"</li>
    *    <li>doCount = boolean from the QuickSavedView</li>
    *    <li>sortOrder = integer sortOrder from the QuickSavedView</li>
    * </ul>
    ***************************************************************************/
   protected void lookupQuickViews(RunBackendStepInput runBackendStepInput, List<QRecord> savedViewRecords) throws QException
   {
      ////////////////////////////////////////////////////////////
      // if there's no quick saved view table, return with noop //
      ////////////////////////////////////////////////////////////
      if(QContext.getQInstance().getTable(QuickSavedView.TABLE_NAME) == null)
      {
         return;
      }

      if(CollectionUtils.nullSafeIsEmpty(savedViewRecords))
      {
         return;
      }

      List<Integer> savedViewIds = savedViewRecords.stream().map(r -> r.getValueInteger("id")).toList();

      ///////////////////////////////////////////////////////////////////////////////////////
      // given that, based on sharing rules, multiple quick view records might be found    //
      // for a given saved view. sort this query in a predictable manner (noting that rows //
      // will be iterated over, and the last one found for a particular saved view is the  //
      // one that will apply).  So sort by sortOrder descending, (so the lowest value will //
      // be used), then by id ascending (so more recently added ones would be preferred)   //
      ///////////////////////////////////////////////////////////////////////////////////////
      List<QRecord> quickSavedViews = QueryAction.execute(QuickSavedView.TABLE_NAME, new QQueryFilter()
         .withCriteria(new QFilterCriteria("savedViewId", QCriteriaOperator.IN, savedViewIds))
         .withOrderBy(new QFilterOrderBy("sortOrder", false))
         .withOrderBy(new QFilterOrderBy("id", true)));

      Map<Integer, QRecord> quickSavedViewsBySavedViewIdMap = CollectionUtils.listToMap(quickSavedViews, r -> r.getValueInteger("savedViewId"), r -> r);
      for(QRecord savedViewRecord : savedViewRecords)
      {
         QRecord quickSavedViewRecord = quickSavedViewsBySavedViewIdMap.get(savedViewRecord.getValueInteger("id"));
         if(quickSavedViewRecord != null)
         {
            savedViewRecord.setValue("type", "quickView");
            savedViewRecord.setValue("doCount", BooleanUtils.isTrue(quickSavedViewRecord.getValueBoolean("doCount")));
            savedViewRecord.setValue("sortOrder", quickSavedViewRecord.getValueInteger("sortOrder"));

            if(StringUtils.hasContent(quickSavedViewRecord.getValueString("label")))
            {
               savedViewRecord.setValue("label", quickSavedViewRecord.getValueString("label"));
            }
         }
      }

   }

}
