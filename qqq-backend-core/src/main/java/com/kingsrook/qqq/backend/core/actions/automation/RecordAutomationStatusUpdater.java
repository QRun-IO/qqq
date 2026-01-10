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

package com.kingsrook.qqq.backend.core.actions.automation;


import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.automation.TableTrigger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTrackingType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TriggerEvent;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;
import org.apache.commons.lang3.NotImplementedException;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility class for updating the automation status data for records
 *******************************************************************************/
public class RecordAutomationStatusUpdater
{
   private static final QLogger LOG = QLogger.getLogger(RecordAutomationStatusUpdater.class);

   ///////////////////////////////////////////////////////////////////////////////////////////////////////
   // feature flag - by default, will be true - before setting records to PENDING_UPDATE_AUTOMATIONS,   //
   // we will fetch them (if we didn't take them in from the caller, which, UpdateAction does if its    //
   // backend supports it), to check their current automationStatus - and if they are currently PENDING //
   // or RUNNING inserts or updates, we won't update them.                                              //
   ///////////////////////////////////////////////////////////////////////////////////////////////////////
   private static boolean allowPreUpdateFetch = new QMetaDataVariableInterpreter().getBooleanFromPropertyOrEnvironment("qqq.recordAutomationStatusUpdater.allowPreUpdateFetch", "QQQ_RECORD_AUTOMATION_STATUS_UPDATER_ALLOW_PRE_UPDATE_FETCH", true);

   ///////////////////////////////////////////////////////////////////////////////////////////////
   // feature flag - by default, we'll memoize the check for triggers - but we can turn it off. //
   ///////////////////////////////////////////////////////////////////////////////////////////////
   private static boolean memoizeCheckForTriggers = new QMetaDataVariableInterpreter().getBooleanFromPropertyOrEnvironment("qqq.recordAutomationStatusUpdater.memoizeCheckForTriggers", "QQQ_RECORD_AUTOMATION_STATUS_UPDATER_MEMOIZE_CHECK_FOR_TRIGGERS", true);

   private static Memoization<Key, Boolean> areThereTableTriggersForTableMemoization = new Memoization<Key, Boolean>().withTimeout(Duration.of(60, ChronoUnit.SECONDS));



   /*******************************************************************************
    ** for a list of records from a table, set their automation status - based on
    ** how the table is configured.
    *******************************************************************************/
   public static boolean setAutomationStatusInRecords(QTableMetaData table, List<QRecord> records, AutomationStatus automationStatus, QBackendTransaction transaction, List<QRecord> oldRecordList)
   {
      if(table == null || table.getAutomationDetails() == null || CollectionUtils.nullSafeIsEmpty(records))
      {
         return (false);
      }

      QTableAutomationDetails automationDetails   = table.getAutomationDetails();
      Set<Serializable>       pkeysWeMayNotUpdate = new HashSet<>();

      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // In case an automation is running, and it updates records - don't let those records be marked  //
      // as PENDING_UPDATE_AUTOMATIONS... this is meant to avoid having a record's automation update   //
      // itself, and then continue to do so in a loop (infinitely).                                    //
      // BUT - shouldn't this be allowed to update OTHER records to be pending updates?  It seems like //
      // yes - so -that'll probably be a bug to fix at some point in the future todo                   //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      if(automationStatus.equals(AutomationStatus.PENDING_UPDATE_AUTOMATIONS))
      {
         Exception e = new Exception();
         for(StackTraceElement stackTraceElement : e.getStackTrace())
         {
            String className = stackTraceElement.getClassName();
            if(className.contains(RecordAutomationStatusUpdater.class.getPackageName()) && !className.equals(RecordAutomationStatusUpdater.class.getName()) && !className.endsWith("Test") && !className.contains("Test$"))
            {
               LOG.debug("Avoiding re-setting automation status to PENDING_UPDATE while running an automation");
               return (false);
            }
         }

         ///////////////////////////////////////////////////////////////////////////////
         // if table uses field-in-table status tracking, then check the old records, //
         // before we set them to pending-updates, to avoid losing other pending or   //
         // running status information.  We will allow moving  from OK or the 2       //
         // failed statuses into pending-updates - which seems right.                 //
         // This is added to fix cases where an update that comes in before insert    //
         // -automations have run, will cause the pending-insert status to be missed. //
         ///////////////////////////////////////////////////////////////////////////////
         if(automationDetails.getStatusTracking() != null && AutomationStatusTrackingType.FIELD_IN_TABLE.equals(automationDetails.getStatusTracking().getType()))
         {
            try
            {
               if(CollectionUtils.nullSafeIsEmpty(oldRecordList))
               {
                  ///////////////////////////////////////////////////////////////////////////////////////////////
                  // if we didn't get the oldRecordList as input (though UpdateAction should usually pass it?) //
                  // then check feature-flag if we're allowed to do a lookup here & now.  If so, then do.      //
                  ///////////////////////////////////////////////////////////////////////////////////////////////
                  if(allowPreUpdateFetch)
                  {
                     List<Serializable> pkeysToLookup = records.stream().map(r -> r.getValue(table.getPrimaryKeyField())).toList();
                     oldRecordList = new QueryAction().execute(new QueryInput(table.getName())
                        .withFilter(new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, pkeysToLookup)))
                        .withTransaction(transaction)
                     ).getRecords();
                  }
               }

               for(QRecord freshRecord : CollectionUtils.nonNullList(oldRecordList))
               {
                  Serializable recordStatus = freshRecord.getValue(automationDetails.getStatusTracking().getFieldName());
                  if(AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId().equals(recordStatus)
                     || AutomationStatus.PENDING_UPDATE_AUTOMATIONS.getId().equals(recordStatus)
                     || AutomationStatus.RUNNING_INSERT_AUTOMATIONS.getId().equals(recordStatus)
                     || AutomationStatus.RUNNING_UPDATE_AUTOMATIONS.getId().equals(recordStatus))
                  {
                     Serializable primaryKey = freshRecord.getValue(table.getPrimaryKeyField());
                     LOG.debug("May not update automation status", logPair("table", table.getName()), logPair("id", primaryKey), logPair("currentStatus", recordStatus), logPair("requestedStatus", automationStatus.getId()));
                     pkeysWeMayNotUpdate.add(primaryKey);
                  }
               }
            }
            catch(QException qe)
            {
               LOG.error("Error checking existing automation status before setting new automation status - more records will be updated than maybe should be...", qe);
            }
         }
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // Avoid setting records to PENDING_INSERT or PENDING_UPDATE even if they don't have any insert or update automations or triggers //
      // such records should go straight to OK status.                                                                                  //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(canWeSkipPendingAndGoToOkay(table, automationStatus))
      {
         automationStatus = AutomationStatus.OK;
      }

      if(automationDetails.getStatusTracking() != null && AutomationStatusTrackingType.FIELD_IN_TABLE.equals(automationDetails.getStatusTracking().getType()))
      {
         for(QRecord record : records)
         {
            if(!pkeysWeMayNotUpdate.contains(record.getValue(table.getPrimaryKeyField())))
            {
               record.setValue(automationDetails.getStatusTracking().getFieldName(), automationStatus.getId());
               // todo - another field - for the automation timestamp??
            }
         }
      }

      return (true);
   }



   /*******************************************************************************
    ** If a table has no automation actions defined for Insert (or Update), and we're
    ** being asked to set status to PENDING_INSERT (or PENDING_UPDATE), then just
    ** move the status straight to OK.
    *******************************************************************************/
   static boolean canWeSkipPendingAndGoToOkay(QTableMetaData table, AutomationStatus automationStatus)
   {
      List<TableAutomationAction> tableActions = Collections.emptyList();
      if(table.getAutomationDetails() != null && table.getAutomationDetails().getActions() != null)
      {
         tableActions = table.getAutomationDetails().getActions();
      }

      if(automationStatus.equals(AutomationStatus.PENDING_INSERT_AUTOMATIONS))
      {
         if(tableActions.stream().anyMatch(a -> TriggerEvent.POST_INSERT.equals(a.getTriggerEvent())))
         {
            return (false);
         }
         else if(areThereTableTriggersForTable(table, TriggerEvent.POST_INSERT))
         {
            return (false);
         }

         ////////////////////////////////////////////////////////////////////////////////////////
         // if we're going to pending-insert, and there are no insert automations or triggers, //
         // then we may skip pending and go to okay.                                           //
         ////////////////////////////////////////////////////////////////////////////////////////
         return (true);
      }
      else if(automationStatus.equals(AutomationStatus.PENDING_UPDATE_AUTOMATIONS))
      {
         if(tableActions.stream().anyMatch(a -> TriggerEvent.POST_UPDATE.equals(a.getTriggerEvent())))
         {
            return (false);
         }
         else if(areThereTableTriggersForTable(table, TriggerEvent.POST_UPDATE))
         {
            return (false);
         }

         ////////////////////////////////////////////////////////////////////////////////////////
         // if we're going to pending-update, and there are no insert automations or triggers, //
         // then we may skip pending and go to okay.                                           //
         ////////////////////////////////////////////////////////////////////////////////////////
         return (true);
      }
      else
      {
         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         // if we're going to any other automation status - then we may never "skip pending" and go to okay - //
         // because we weren't asked to go to pending!                                                        //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean areThereTableTriggersForTable(QTableMetaData table, TriggerEvent triggerEvent)
   {
      if(QContext.getQInstance().getTable(TableTrigger.TABLE_NAME) == null)
      {
         return (false);
      }

      if(memoizeCheckForTriggers)
      {
         ///////////////////////////////////////////////////////////////////////////////////////
         // as within the lookup method, error on the side of "yes, maybe there are triggers" //
         ///////////////////////////////////////////////////////////////////////////////////////
         Optional<Boolean> result = areThereTableTriggersForTableMemoization.getResult(new Key(table, triggerEvent), key -> lookupIfThereAreTriggersForTable(table, triggerEvent));
         return result.orElse(true);
      }
      else
      {
         return lookupIfThereAreTriggersForTable(table, triggerEvent);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Boolean lookupIfThereAreTriggersForTable(QTableMetaData table, TriggerEvent triggerEvent)
   {
      try
      {
         CountInput countInput = new CountInput();
         countInput.setTableName(TableTrigger.TABLE_NAME);
         countInput.setFilter(new QQueryFilter(
            new QFilterCriteria("tableName", QCriteriaOperator.EQUALS, table.getName()),
            new QFilterCriteria(triggerEvent.equals(TriggerEvent.POST_INSERT) ? "postInsert" : "postUpdate", QCriteriaOperator.EQUALS, true)
         ));
         CountOutput countOutput = new CountAction().execute(countInput);
         return (countOutput.getCount() != null && countOutput.getCount() > 0);
      }
      catch(Exception e)
      {
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if the count query failed, we're a bit safer to err on the side of "yeah, there might be automations" //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////
         LOG.warn("Error looking if there are triggers for table", e, logPair("tableName", table.getName()));
         return (true);
      }
   }



   /*******************************************************************************
    ** for a list of records, update their automation status and actually Update the
    ** backend as well.
    *******************************************************************************/
   public static void setAutomationStatusInRecordsAndUpdate(QTableMetaData table, List<QRecord> records, AutomationStatus automationStatus, QBackendTransaction transaction) throws QException
   {
      QTableAutomationDetails automationDetails = table.getAutomationDetails();
      if(automationDetails != null && AutomationStatusTrackingType.FIELD_IN_TABLE.equals(automationDetails.getStatusTracking().getType()))
      {
         boolean didSetStatusField = setAutomationStatusInRecords(table, records, automationStatus, transaction, null);
         if(didSetStatusField)
         {
            UpdateInput updateInput = new UpdateInput();
            updateInput.setTableName(table.getName());

            /////////////////////////////////////////////////////////////////////////////////////
            // build records with just their pkey & status field for this update, to avoid     //
            // changing other values (relies on assumption of Patch semantics in UpdateAction) //
            /////////////////////////////////////////////////////////////////////////////////////
            updateInput.setRecords(records.stream().map(r -> new QRecord()
               .withTableName(r.getTableName())
               .withValue(table.getPrimaryKeyField(), r.getValue(table.getPrimaryKeyField()))
               .withValue(automationDetails.getStatusTracking().getFieldName(), r.getValue(automationDetails.getStatusTracking().getFieldName()))).toList());
            updateInput.setAreAllValuesBeingUpdatedTheSame(true);
            updateInput.setTransaction(transaction);
            updateInput.setOmitDmlAudit(true);

            new UpdateAction().execute(updateInput);
         }
      }
      else
      {
         // todo - verify if this is valid as other types are built
         throw (new NotImplementedException("Updating record automation status is not implemented for table [" + table + "], tracking type: "
            + (automationDetails == null ? "null" : automationDetails.getStatusTracking().getType())));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private record Key(QTableMetaData table, TriggerEvent triggerEvent) {}

}
