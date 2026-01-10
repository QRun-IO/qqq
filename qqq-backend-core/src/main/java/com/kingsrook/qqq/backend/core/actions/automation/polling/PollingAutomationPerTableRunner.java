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

package com.kingsrook.qqq.backend.core.actions.automation.polling;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.async.AsyncRecordPipeLoop;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationHandlerInterface;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationStatusUpdater;
import com.kingsrook.qqq.backend.core.actions.automation.RunCustomTableTriggerRecordAutomationHandler;
import com.kingsrook.qqq.backend.core.actions.automation.RunRecordScriptAutomationHandler;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallback;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.adapters.QQueryFilterJsonAdapter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.LogPair;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.automation.RecordAutomationInput;
import com.kingsrook.qqq.backend.core.model.automation.TableTrigger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTrackingType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TriggerEvent;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedView;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.apache.commons.lang3.NotImplementedException;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Runnable for the Polling Automation Provider, that looks for records that
 ** need automations, and executes them.
 **
 ** An instance of this class should be created for each table/automation-status
 ** - see the TableActions inner record for that definition, and the static
 ** getTableActions method that helps someone who wants to start these threads
 ** figure out which ones are needed.
 *******************************************************************************/
public class PollingAutomationPerTableRunner implements Runnable
{
   private static final QLogger LOG = QLogger.getLogger(PollingAutomationPerTableRunner.class);

   private final TableActionsInterface tableActions;

   private String name;

   private QInstance          instance;
   private Supplier<QSession> sessionSupplier;

   private static Map<TriggerEvent, AutomationStatus> triggerEventAutomationStatusMap = Map.of(
      TriggerEvent.POST_INSERT, AutomationStatus.PENDING_INSERT_AUTOMATIONS,
      TriggerEvent.POST_UPDATE, AutomationStatus.PENDING_UPDATE_AUTOMATIONS
   );

   private static Map<AutomationStatus, TriggerEvent> automationStatusTriggerEventMap = Map.of(
      AutomationStatus.PENDING_INSERT_AUTOMATIONS, TriggerEvent.POST_INSERT,
      AutomationStatus.PENDING_UPDATE_AUTOMATIONS, TriggerEvent.POST_UPDATE
   );

   private static Map<AutomationStatus, AutomationStatus> pendingToRunningStatusMap = Map.of(
      AutomationStatus.PENDING_INSERT_AUTOMATIONS, AutomationStatus.RUNNING_INSERT_AUTOMATIONS,
      AutomationStatus.PENDING_UPDATE_AUTOMATIONS, AutomationStatus.RUNNING_UPDATE_AUTOMATIONS
   );

   private static Map<AutomationStatus, AutomationStatus> pendingToFailedStatusMap = Map.of(
      AutomationStatus.PENDING_INSERT_AUTOMATIONS, AutomationStatus.FAILED_INSERT_AUTOMATIONS,
      AutomationStatus.PENDING_UPDATE_AUTOMATIONS, AutomationStatus.FAILED_UPDATE_AUTOMATIONS
   );



   /*******************************************************************************
    ** Interface to be used by 2 records in this class - normal TableActions, and
    ** ShardedTableActions.
    *******************************************************************************/
   public interface TableActionsInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      String tableName();

      /*******************************************************************************
       **
       *******************************************************************************/
      QTableAutomationDetails tableAutomationDetails();

      /*******************************************************************************
       **
       *******************************************************************************/
      AutomationStatus status();
   }



   /*******************************************************************************
    ** Wrapper for a pair of (tableName, automationStatus)
    *******************************************************************************/
   public record TableActions(String tableName, QTableAutomationDetails tableAutomationDetails, AutomationStatus status) implements TableActionsInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      public void noopToFakeTestCoverage()
      {
      }
   }



   /*******************************************************************************
    ** extended version of TableAction, for sharding use-case - adds the shard
    ** details.
    *******************************************************************************/
   public record ShardedTableActions(String tableName, QTableAutomationDetails tableAutomationDetails, AutomationStatus status, String shardByFieldName, Serializable shardValue, String shardLabel) implements TableActionsInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      public void noopToFakeTestCoverage()
      {
      }
   }



   /*******************************************************************************
    ** basically just get a list of tables which at least *could* have automations
    ** run - either meta-data automations, or table-triggers (data/user defined).
    *******************************************************************************/
   public static List<TableActionsInterface> getTableActions(QInstance instance, String providerName)
   {
      List<TableActionsInterface> tableActionList = new ArrayList<>();

      for(QTableMetaData table : instance.getTables().values())
      {
         QTableAutomationDetails automationDetails = table.getAutomationDetails();
         if(automationDetails != null && providerName.equals(automationDetails.getProviderName()))
         {
            if(StringUtils.hasContent(automationDetails.getShardByFieldName()))
            {
               //////////////////////////////////////////////////////////////////////////////////////////////
               // for sharded automations, add a tableAction (of the sharded subtype) for each shard-value //
               //////////////////////////////////////////////////////////////////////////////////////////////
               try
               {
                  QueryInput queryInput = new QueryInput();
                  queryInput.setTableName(automationDetails.getShardSourceTableName());
                  QueryOutput queryOutput = new QueryAction().execute(queryInput);
                  for(QRecord record : queryOutput.getRecords())
                  {
                     Serializable shardId = record.getValue(automationDetails.getShardIdFieldName());
                     String       label   = record.getValueString(automationDetails.getShardLabelFieldName());
                     tableActionList.add(new ShardedTableActions(table.getName(), automationDetails, AutomationStatus.PENDING_INSERT_AUTOMATIONS, automationDetails.getShardByFieldName(), shardId, label));
                     tableActionList.add(new ShardedTableActions(table.getName(), automationDetails, AutomationStatus.PENDING_UPDATE_AUTOMATIONS, automationDetails.getShardByFieldName(), shardId, label));
                  }
               }
               catch(Exception e)
               {
                  LOG.error("Error getting sharded table automation actions for a table", e, new LogPair("tableName", table.getName()));
               }
            }
            else
            {
               //////////////////////////////////////////////////////////////////
               // for non-sharded, we just need table name & automation status //
               //////////////////////////////////////////////////////////////////
               tableActionList.add(new TableActions(table.getName(), automationDetails, AutomationStatus.PENDING_INSERT_AUTOMATIONS));
               tableActionList.add(new TableActions(table.getName(), automationDetails, AutomationStatus.PENDING_UPDATE_AUTOMATIONS));
            }
         }
      }

      return (tableActionList);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public PollingAutomationPerTableRunner(QInstance instance, String providerName, Supplier<QSession> sessionSupplier, TableActionsInterface tableActions)
   {
      this.instance = instance;
      this.sessionSupplier = sessionSupplier;
      this.tableActions = tableActions;
      this.name = providerName + ">" + tableActions.tableName() + ">" + tableActions.status().getInsertOrUpdate();

      if(tableActions instanceof ShardedTableActions shardedTableActions)
      {
         this.name += ":" + shardedTableActions.shardLabel();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run()
   {
      QContext.init(instance, sessionSupplier.get());

      String originalThreadName = Thread.currentThread().getName();
      Thread.currentThread().setName(name);
      LOG.debug("Running " + this.getClass().getSimpleName() + "[" + name + "]");

      try
      {
         processTableInsertOrUpdate(instance.getTable(tableActions.tableName()), tableActions.status());
      }
      catch(Exception e)
      {
         LOG.warn("Error running automations", e, logPair("tableName", tableActions.tableName()), logPair("status", tableActions.status()));
      }
      finally
      {
         Thread.currentThread().setName(originalThreadName);
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Query for and process records that have a PENDING_INSERT or PENDING_UPDATE status on a given table.
    *******************************************************************************/
   public void processTableInsertOrUpdate(QTableMetaData table, AutomationStatus automationStatus) throws QException
   {
      /////////////////////////////////////////////////////////////////////////
      // get the actions to run against this table in this automation status //
      /////////////////////////////////////////////////////////////////////////
      List<TableAutomationAction> actions = getTableActions(table, automationStatus);
      if(CollectionUtils.nullSafeIsEmpty(actions))
      {
         return;
      }

      LOG.debug("  Query for records " + automationStatus + " in " + table);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // run an async-pipe loop - that will query for records in PENDING - put them in a pipe - then apply actions to them //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QTableAutomationDetails automationDetails   = table.getAutomationDetails();
      AsyncRecordPipeLoop     asyncRecordPipeLoop = new AsyncRecordPipeLoop();

      RecordPipe recordPipe = automationDetails.getOverrideBatchSize() == null
         ? new RecordPipe() : new RecordPipe(automationDetails.getOverrideBatchSize());

      asyncRecordPipeLoop.run("PollingAutomationRunner>Query>" + automationStatus + ">" + table.getName(), null, recordPipe, (status) ->
         {
            QueryInput queryInput = new QueryInput();
            queryInput.setTableName(table.getName());

            AutomationStatusTrackingType statusTrackingType = automationDetails.getStatusTracking().getType();
            if(AutomationStatusTrackingType.FIELD_IN_TABLE.equals(statusTrackingType))
            {
               QQueryFilter filter = new QQueryFilter().withCriteria(new QFilterCriteria(automationDetails.getStatusTracking().getFieldName(), QCriteriaOperator.EQUALS, List.of(automationStatus.getId())));
               addOrderByToQueryFilter(table, automationStatus, filter);
               queryInput.setFilter(filter);
            }
            else
            {
               throw (new NotImplementedException("Automation Status Tracking type [" + statusTrackingType + "] is not yet implemented in here."));
            }

            if(tableActions instanceof ShardedTableActions shardedTableActions)
            {
               //////////////////////////////////////////////////////////////
               // for sharded actions, add the shardBy field as a criteria //
               //////////////////////////////////////////////////////////////
               QQueryFilter filter = queryInput.getFilter();
               filter.addCriteria(new QFilterCriteria(shardedTableActions.shardByFieldName(), QCriteriaOperator.EQUALS, shardedTableActions.shardValue()));
            }

            queryInput.setRecordPipe(recordPipe);
            return (new QueryAction().execute(queryInput));
         }, () ->
         {
            List<QRecord> records = recordPipe.consumeAvailableRecords();
            applyActionsToRecords(table, records, actions, automationStatus);
            return (records.size());
         }
      );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void addOrderByToQueryFilter(QTableMetaData table, AutomationStatus automationStatus, QQueryFilter filter)
   {
      ////////////////////////////////////////////////////////////////////////////////////
      // look for a field in the table with either create-date or modify-date behavior, //
      // based on if doing insert or update automations                                 //
      ////////////////////////////////////////////////////////////////////////////////////
      DynamicDefaultValueBehavior dynamicDefaultValueBehavior = automationStatus.equals(AutomationStatus.PENDING_INSERT_AUTOMATIONS) ? DynamicDefaultValueBehavior.CREATE_DATE : DynamicDefaultValueBehavior.MODIFY_DATE;
      Optional<QFieldMetaData> field = table.getFields().values().stream()
         .filter(f -> dynamicDefaultValueBehavior.equals(f.getBehaviorOrDefault(QContext.getQInstance(), DynamicDefaultValueBehavior.class)))
         .findFirst();

      if(field.isPresent())
      {
         //////////////////////////////////////////////////////////////////////
         // if a create/modify date field was found, order by it (ascending) //
         //////////////////////////////////////////////////////////////////////
         filter.addOrderBy(new QFilterOrderBy(field.get().getName()));
      }
      else
      {
         ////////////////////////////////////
         // else, order by the primary key //
         ////////////////////////////////////
         filter.addOrderBy(new QFilterOrderBy(table.getPrimaryKeyField()));
      }
   }



   /*******************************************************************************
    ** get the actions to run against a table in an automation status.  both from
    ** metaData and tableTriggers/data.
    *******************************************************************************/
   private List<TableAutomationAction> getTableActions(QTableMetaData table, AutomationStatus automationStatus) throws QException
   {
      List<TableAutomationAction> rs           = new ArrayList<>();
      TriggerEvent                triggerEvent = automationStatusTriggerEventMap.get(automationStatus);

      ///////////////////////////////////////////////////////////
      // start with any actions defined in the table meta data //
      ///////////////////////////////////////////////////////////
      for(TableAutomationAction action : table.getAutomationDetails().getActions())
      {
         if(action.getTriggerEvent().equals(triggerEvent))
         {
            ///////////////////////////////////////////////////////////
            // for sharded configs, only run if the shard id matches //
            ///////////////////////////////////////////////////////////
            if(tableActions instanceof ShardedTableActions shardedTableActions)
            {
               if(shardedTableActions.shardValue().equals(action.getShardId()))
               {
                  rs.add(action);
               }
            }
            else
            {
               ////////////////////////////////////////////
               // for non-sharded, always add the action //
               ////////////////////////////////////////////
               rs.add(action);
            }
         }
      }

      /////////////////////////////////////////////////
      // next add any tableTriggers, defined in data //
      /////////////////////////////////////////////////
      if(QContext.getQInstance().getTable(TableTrigger.TABLE_NAME) != null)
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(TableTrigger.TABLE_NAME);
         queryInput.setFilter(new QQueryFilter(
            new QFilterCriteria("tableName", QCriteriaOperator.EQUALS, table.getName()),
            new QFilterCriteria(triggerEvent.equals(TriggerEvent.POST_INSERT) ? "postInsert" : "postUpdate", QCriteriaOperator.EQUALS, true)
         ));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         for(QRecord record : queryOutput.getRecords())
         {
            TableTrigger tableTrigger = new TableTrigger(record);

            try
            {
               QQueryFilter filter   = null;
               Integer      filterId = tableTrigger.getFilterId();
               if(filterId != null)
               {
                  GetInput getInput = new GetInput();
                  getInput.setTableName(SavedView.TABLE_NAME);
                  getInput.setPrimaryKey(filterId);
                  GetOutput getOutput = new GetAction().execute(getInput);
                  if(getOutput.getRecord() != null)
                  {
                     SavedView  savedView   = new SavedView(getOutput.getRecord());
                     JSONObject viewJson    = new JSONObject(savedView.getViewJson());
                     JSONObject queryFilter = viewJson.getJSONObject("queryFilter");
                     filter = new QQueryFilterJsonAdapter().jsonToQQueryFilter(queryFilter.toString());
                  }
               }

               TableAutomationAction tableAutomationAction = new TableAutomationAction()
                  .withFilter(filter)
                  .withTriggerEvent(triggerEvent)
                  .withPriority(tableTrigger.getPriority())
                  .withIncludeRecordAssociations(true);

               ///////////////////////////////////////////////////////////////////////////////////////////////////////
               // if the table trigger has a script id on it, then we know how to run that here in qqq-backend-core //
               ///////////////////////////////////////////////////////////////////////////////////////////////////////
               if(tableTrigger.getScriptId() != null)
               {
                  rs.add(tableAutomationAction
                     .withName("Script:" + tableTrigger.getScriptId())
                     .withValues(MapBuilder.of("scriptId", tableTrigger.getScriptId()))
                     .withCodeReference(new QCodeReference(RunRecordScriptAutomationHandler.class)));
               }
               else
               {
                  ////////////////////////////////////////////////////////////////////////////////////////////////
                  // but - the app may have added an extension to the TableTrigger table (e.g., workflows qbit) //
                  // so, defer to RunCustomRecordAutomationHandler for unrecognized triggers                    //
                  ////////////////////////////////////////////////////////////////////////////////////////////////
                  rs.add(tableAutomationAction
                     .withName("Custom Trigger:" + tableTrigger.getScriptId())
                     .withValues(MapBuilder.of("tableTriggerId", tableTrigger.getId()))
                     .withCodeReference(new QCodeReference(RunCustomTableTriggerRecordAutomationHandler.class)));
               }
            }
            catch(Exception e)
            {
               LOG.error("Error setting up table trigger", e, logPair("tableTriggerId", tableTrigger.getId()));
            }
         }
      }

      rs.sort(Comparator.comparing(taa -> Objects.requireNonNullElse(taa.getPriority(), Integer.MAX_VALUE)));

      return (rs);
   }



   /*******************************************************************************
    ** For a set of records that were found to be in a PENDING state - run all the
    ** table's actions against them - IF they are found to match the action's filter
    ** (assuming it has one - if it doesn't, then all records match).
    *******************************************************************************/
   private void applyActionsToRecords(QTableMetaData table, List<QRecord> records, List<TableAutomationAction> actions, AutomationStatus automationStatus) throws QException
   {
      if(CollectionUtils.nullSafeIsEmpty(records))
      {
         return;
      }

      ///////////////////////////////////////////////////
      // mark the records as RUNNING their automations //
      ///////////////////////////////////////////////////
      RecordAutomationStatusUpdater.setAutomationStatusInRecordsAndUpdate(table, records, pendingToRunningStatusMap.get(automationStatus), null);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // foreach action - run it against the records (but only if they match the action's filter, if there is one) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      boolean anyActionsFailed = false;
      for(TableAutomationAction action : actions)
      {
         boolean hadError = applyActionToRecords(table, records, action);
         if(hadError)
         {
            anyActionsFailed = true;
         }
      }

      ////////////////////////////////////////
      // update status on all these records //
      ////////////////////////////////////////
      AutomationStatus statusToUpdateTo = anyActionsFailed ? pendingToFailedStatusMap.get(automationStatus) : AutomationStatus.OK;
      try
      {
         RecordAutomationStatusUpdater.setAutomationStatusInRecordsAndUpdate(table, records, statusToUpdateTo, null);
      }
      catch(Exception e)
      {
         LOG.warn("Error updating automationStatus after running automations", logPair("tableName", table), logPair("count", records.size()), logPair("status", statusToUpdateTo));
         throw (e);
      }
   }



   /*******************************************************************************
    ** Run one action over a list of records (if they match the action's filter).
    **
    ** @return hadError - true if an exception was caught; false if all OK.
    *******************************************************************************/
   protected boolean applyActionToRecords(QTableMetaData table, List<QRecord> records, TableAutomationAction action)
   {
      try
      {
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // note - this method - will re-query the objects, so we should have confidence that their data is fresh... //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////
         List<QRecord> matchingQRecords = getRecordsMatchingActionFilter(table, records, action);
         LOG.debug("Of the [" + records.size() + "] records that were pending automations, [" + matchingQRecords.size() + "] of them match the filter on the action:" + action);
         if(CollectionUtils.nullSafeHasContents(matchingQRecords))
         {
            LOG.debug("  Processing " + matchingQRecords.size() + " records in " + table + " for action " + action);
            applyActionToMatchingRecords(table, matchingQRecords, action);
         }

         return (false);
      }
      catch(Exception e)
      {
         LOG.warn("Caught exception processing automations", e, logPair("tableName", table), logPair("action", action.getName()));
         return (true);
      }
   }



   /*******************************************************************************
    ** For a given action, and a list of records - return a new list, of the ones
    ** which match the action's filter (if there is one - if not, then all match).
    **
    ** Note that this WILL re-query the objects  (ALWAYS - even if the action has no filter).
    ** This has the nice side effect of always giving fresh/updated records, despite having
    ** some cost.
    **
    ** At one point, we considered just applying the filter using java-comparisons,
    ** but that will almost certainly give potentially different results than a true
    ** backend - e.g., just consider if the DB is case-sensitive for strings...
    *******************************************************************************/
   private List<QRecord> getRecordsMatchingActionFilter(QTableMetaData table, List<QRecord> records, TableAutomationAction action) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(table.getName());

      ///////////////////////////////////////////////////////////////////////////////////////
      // set up a filter that is for the primary keys IN the list that we identified above //
      ///////////////////////////////////////////////////////////////////////////////////////
      QQueryFilter filter = new QQueryFilter();
      filter.addCriteria(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, records.stream().map(r -> r.getValue(table.getPrimaryKeyField())).toList()));

      if(action.getFilter() != null)
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // if the action defines a filter of its own, add that to the filter we'll run now as a sub-filter //
         // not entirely clear if this needs to be a clone, but, it feels safe and cheap enough             //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         filter.addSubFilter(action.getFilter().clone());

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // we also want to set order-bys from the action into our filter (since they only apply at the top-level) //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(action.getFilter().getOrderBys() != null)
         {
            action.getFilter().getOrderBys().forEach(filter::addOrderBy);
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////////////
      // always add order-by the primary key, to give more predictable/consistent results        //
      // todo - in future - if this becomes a source of slowness, make this a config to opt-out? //
      /////////////////////////////////////////////////////////////////////////////////////////////
      filter.addOrderBy(new QFilterOrderBy().withFieldName(table.getPrimaryKeyField()));

      queryInput.setFilter(filter);

      queryInput.setIncludeAssociations(action.getIncludeRecordAssociations());

      return (new QueryAction().execute(queryInput).getRecords());
   }



   /*******************************************************************************
    ** Finally, actually run action code against a list of known matching records.
    **
    *******************************************************************************/
   public static void applyActionToMatchingRecords(QTableMetaData table, List<QRecord> records, TableAutomationAction action) throws Exception
   {
      if(StringUtils.hasContent(action.getProcessName()))
      {
         /////////////////////////////////////////////////////////////////////////////////////////
         // if the action has a process associated with it - run that process.                  //
         // tell it to SKIP frontend steps.                                                     //
         // give the process a callback w/ a query filter that has the p-keys of these records. //
         /////////////////////////////////////////////////////////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(action.getProcessName());
         runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
         runProcessInput.setCallback(new QProcessCallback()
         {
            @Override
            public QQueryFilter getQueryFilter()
            {
               List<Serializable> recordIds = records.stream().map(r -> r.getValue(table.getPrimaryKeyField())).collect(Collectors.toList());
               return (new QQueryFilter().withCriteria(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, recordIds)));
            }
         });

         try
         {
            QContext.pushAction(runProcessInput);

            RunProcessAction runProcessAction = new RunProcessAction();
            RunProcessOutput runProcessOutput = runProcessAction.execute(runProcessInput);
            if(runProcessOutput.getException().isPresent())
            {
               throw (runProcessOutput.getException().get());
            }
         }
         finally
         {
            QContext.popAction();
         }
      }
      else if(action.getCodeReference() != null)
      {
         LOG.debug("    Executing action: [" + action.getName() + "] as code reference: " + action.getCodeReference());
         RecordAutomationInput input = new RecordAutomationInput();
         input.setTableName(table.getName());
         input.setRecordList(records);
         input.setAction(action);

         RecordAutomationHandlerInterface recordAutomationHandler = QCodeLoader.getAdHoc(RecordAutomationHandlerInterface.class, action.getCodeReference());
         recordAutomationHandler.execute(input);
      }
   }



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }
}
