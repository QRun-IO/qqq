/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.garbagecollector;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.metadata.JoinGraph;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.BackendStepPostRunInput;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.BackendStepPostRunOutput;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.IN;


/*******************************************************************************
 **
 *******************************************************************************/
public class GarbageCollectorTransformStep extends AbstractTransformStep
{
   private static final QLogger LOG = QLogger.getLogger(GarbageCollectorTransformStep.class);

   private int count = 0;
   private int total = 0;

   private final ProcessSummaryLine okSummary = new ProcessSummaryLine(Status.OK)
      .withMessageSuffix(" deleted")
      .withSingularFutureMessage("will be")
      .withPluralFutureMessage("will be")
      .withSingularPastMessage("has been")
      .withPluralPastMessage("have been");

   private Map<String, Integer> descendantRecordCountToDelete = new LinkedHashMap<>();



   /*******************************************************************************
    ** getProcessSummary
    *
    *******************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      ArrayList<ProcessSummaryLineInterface> rs = new ArrayList<>();
      okSummary.addSelfToListIfAnyCount(rs);

      for(Map.Entry<String, Integer> entry : descendantRecordCountToDelete.entrySet())
      {
         ProcessSummaryLine childSummary = new ProcessSummaryLine(Status.OK)
            .withMessageSuffix(" deleted")
            .withSingularFutureMessage("associated " + entry.getKey() + " record will be")
            .withPluralFutureMessage("associated " + entry.getKey() + " records will be")
            .withSingularPastMessage("associated " + entry.getKey() + " record has been")
            .withPluralPastMessage("associated " + entry.getKey() + " records have been");
         childSummary.setCount(entry.getValue());
         rs.add(childSummary);
      }

      if(total == 0)
      {
         rs.add(new ProcessSummaryLine(Status.INFO, null, "No records were found to be garbage collected."));
      }

      return (rs);
   }



   /*******************************************************************************
    ** run
    *
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ////////////////////////////////
      // return if no input records //
      ////////////////////////////////
      if(CollectionUtils.nullSafeIsEmpty(runBackendStepInput.getRecords()))
      {
         return;
      }

      ///////////////////////////////////////////////////////////////////
      // keep a count (in case table doesn't support count capacility) //
      ///////////////////////////////////////////////////////////////////
      count += runBackendStepInput.getRecords().size();
      total = Objects.requireNonNullElse(runBackendStepInput.getValueInteger(StreamedETLWithFrontendProcess.FIELD_RECORD_COUNT), count);
      runBackendStepInput.getAsyncJobCallback().updateStatus("Validating records", count, total);

      ////////////////////////////////////////////////////////////////////////////////////////
      // process the joinedTablesToAlsoDelete value.                                        //
      // if it's "*", interpret that as all tables in the instance.                         //
      // else split it on commas.                                                           //
      // note that absent value or empty string means we won't delete from any other tables //
      ////////////////////////////////////////////////////////////////////////////////////////
      String      joinedTablesToAlsoDelete      = runBackendStepInput.getValueString("joinedTablesToAlsoDelete");
      Set<String> setOfJoinedTablesToAlsoDelete = new HashSet<>();
      if("*".equals(joinedTablesToAlsoDelete))
      {
         setOfJoinedTablesToAlsoDelete.addAll(QContext.getQInstance().getTables().keySet());
      }
      else if(joinedTablesToAlsoDelete != null)
      {
         setOfJoinedTablesToAlsoDelete.addAll(Arrays.asList(joinedTablesToAlsoDelete.split(",")));
      }

      ///////////////////
      // process joins //
      ///////////////////
      String tableName = runBackendStepInput.getValueString(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE);
      lookForJoins(runBackendStepInput, tableName, runBackendStepInput.getRecords(), new HashSet<>(Set.of(tableName)), setOfJoinedTablesToAlsoDelete);

      LOG.info("GarbageCollector called with a page of records", logPair("count", runBackendStepInput.getRecords().size()), logPair("table", tableName));

      ////////////////////////////////////////////////////
      // move records (from primary table) to next step //
      ////////////////////////////////////////////////////
      for(QRecord qRecord : runBackendStepInput.getRecords())
      {
         okSummary.incrementCountAndAddPrimaryKey(qRecord.getValue(runBackendStepInput.getTable().getPrimaryKeyField()));
         runBackendStepOutput.getRecords().add(qRecord);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void postRun(BackendStepPostRunInput runBackendStepInput, BackendStepPostRunOutput runBackendStepOutput) throws QException
   {
      super.postRun(runBackendStepInput, runBackendStepOutput);

      ///////////////////////////////////////////////////////////////////////////////////////
      // if we've just finished the validate step -                                        //
      // and if there wasn't a COUNT performed (e.g., because the table didn't support it) //
      // then set our total that we accumulated into the count field.                      //
      ///////////////////////////////////////////////////////////////////////////////////////
      if(runBackendStepInput.getStepName().equals(StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE))
      {
         if(runBackendStepInput.getValueInteger(StreamedETLWithFrontendProcess.FIELD_RECORD_COUNT) == null)
         {
            runBackendStepInput.addValue(StreamedETLWithFrontendProcess.FIELD_RECORD_COUNT, total);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void lookForJoins(RunBackendStepInput runBackendStepInput, String tableName, List<QRecord> records, Set<String> visitedTables, Set<String> allowedToAlsoDelete) throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////
      // if we've already visited all the tables we're allowed to delete, then return early //
      ////////////////////////////////////////////////////////////////////////////////////////
      HashSet<String> anyAllowedLeft = new HashSet<>(allowedToAlsoDelete);
      anyAllowedLeft.removeAll(visitedTables);
      if(CollectionUtils.nullSafeIsEmpty(anyAllowedLeft))
      {
         return;
      }

      QInstance qInstance = QContext.getQInstance();
      JoinGraph joinGraph = qInstance.getJoinGraph();

      ////////////////////////////////////////////////////////////////////
      // get join connections from this table from the joinGraph object //
      ////////////////////////////////////////////////////////////////////
      Set<JoinGraph.JoinConnectionList> joinConnections = joinGraph.getJoinConnections(tableName);
      for(JoinGraph.JoinConnectionList joinConnectionList : CollectionUtils.nonNullCollection(joinConnections))
      {
         List<JoinGraph.JoinConnection> list           = joinConnectionList.list();
         JoinGraph.JoinConnection       joinConnection = list.get(0);
         QJoinMetaData                  join           = qInstance.getJoin(joinConnection.viaJoinName());

         String recurOnTable             = null;
         String thisTableFKeyField       = null;
         String joinTablePrimaryKeyField = null;

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // find the input table in the join - but only if it's on a '1' side of the join (not a many side)        //
         // this means we may get out of this if/else with recurOnTable = null, if we shouldn't process this join. //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(join.getLeftTable().equals(tableName) && (join.getType().equals(JoinType.ONE_TO_MANY) || join.getType().equals(JoinType.ONE_TO_ONE)))
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if this table is on the left side of this join, and it's a 1-n or 1-1, then delete from the right table //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            recurOnTable = join.getRightTable();
            thisTableFKeyField = join.getJoinOns().get(0).getLeftField();
            joinTablePrimaryKeyField = join.getJoinOns().get(0).getRightField();
         }
         else if(join.getRightTable().equals(tableName) && (join.getType().equals(JoinType.MANY_TO_ONE) || join.getType().equals(JoinType.ONE_TO_ONE)))
         {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // else if this table is on the right side of this join, and it's n-1 or 1-1, then delete from the left table //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            recurOnTable = join.getLeftTable();
            thisTableFKeyField = join.getJoinOns().get(0).getRightField();
            joinTablePrimaryKeyField = join.getJoinOns().get(0).getLeftField();
         }

         //////////////////////////////////////////////////////////////////////////////////////////////
         // if we found a table to 'recur' on, and we haven't visited it before, then process it now //
         //////////////////////////////////////////////////////////////////////////////////////////////
         if(recurOnTable != null && !visitedTables.contains(recurOnTable))
         {
            if(join.getJoinOns().size() > 1)
            {
               LOG.warn("We would delete child records from the join [" + join.getName() + "], but it has multiple joinOns, and we don't support that yet...");
               continue;
            }

            visitedTables.add(recurOnTable);

            ////////////////////////////////////////////////////////////
            // query for records in the child table based on the join //
            ////////////////////////////////////////////////////////////
            QTableMetaData     foreignTable            = qInstance.getTable(recurOnTable);
            String             finalThisTableFKeyField = thisTableFKeyField;
            List<Serializable> foreignKeys             = records.stream().map(r -> r.getValue(finalThisTableFKeyField)).distinct().toList();
            List<QRecord>      foreignRecords          = new QueryAction().execute(new QueryInput(recurOnTable).withFilter(new QQueryFilter(new QFilterCriteria(joinTablePrimaryKeyField, IN, foreignKeys)))).getRecords();

            ////////////////////////////////////////////////////////////////////////////////////
            // make a recursive call looking for children of this table                       //
            // we do this before we delete from this table, so that the children can be found //
            ////////////////////////////////////////////////////////////////////////////////////
            lookForJoins(runBackendStepInput, recurOnTable, foreignRecords, visitedTables, allowedToAlsoDelete);

            if(allowedToAlsoDelete.contains(recurOnTable))
            {
               LOG.info("Deleting descendant records from: " + recurOnTable);
               descendantRecordCountToDelete.putIfAbsent(foreignTable.getLabel(), 0);
               descendantRecordCountToDelete.put(foreignTable.getLabel(), descendantRecordCountToDelete.get(foreignTable.getLabel()) + foreignRecords.size());

               /////////////////////////////////////////////////////////////////////
               // if this is the execute step - then do it - delete the children. //
               /////////////////////////////////////////////////////////////////////
               if(runBackendStepInput.getStepName().equals(StreamedETLWithFrontendProcess.STEP_NAME_EXECUTE))
               {
                  List<Serializable> foreignPKeys = foreignRecords.stream().map(r -> r.getValue(foreignTable.getPrimaryKeyField())).toList();
                  new DeleteAction().execute(new DeleteInput(recurOnTable).withPrimaryKeys(foreignPKeys).withTransaction(getTransaction().orElse(null)));
               }
            }
         }
      }
   }

}
