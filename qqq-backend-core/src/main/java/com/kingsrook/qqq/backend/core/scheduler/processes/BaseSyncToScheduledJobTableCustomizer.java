/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.scheduler.processes;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackBuilder;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.InitializableViaCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceWithProperties;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** an implementation of a TableCustomizer that runs a subclass of
 ** AbstractRecordSyncToScheduledJobProcess - to manage scheduledJob records that
 ** correspond to records in another table (e.g., a job for each Client)
 **
 ** Easiest way to use is:
 ** - BaseSyncToScheduledJobTableCustomizer.setTableCustomizers(tableMetaData, new YourSyncScheduledJobProcessSubclass());
 ** which adds post-insert, -update, and -delete customizers to your table.
 **
 ** If you need additional table customizer code in those slots, I suppose you could
 ** simply make your customizer create an instance of this class, set its
 ** properties, and run its appropriate postInsertOrUpdate/postDelete methods.
 *******************************************************************************/
public class BaseSyncToScheduledJobTableCustomizer implements TableCustomizerInterface, InitializableViaCodeReference
{
   private static final QLogger LOG = QLogger.getLogger(BaseSyncToScheduledJobTableCustomizer.class);

   public static final String KEY_TABLE_NAME                     = "tableName";
   public static final String KEY_SYNC_PROCESS_NAME              = "syncProcessName";
   public static final String KEY_SCHEDULED_JOB_FOREIGN_KEY_TYPE = "scheduledJobForeignKeyType";

   private String tableName;
   private String syncProcessName;
   private String scheduledJobForeignKeyType;



   /***************************************************************************
    * Create a {@link QCodeReferenceWithProperties} that can be used to add this
    * class to a table.
    *
    * If this is the only customizer for the post insert/update/delete events
    * on your table, you can instead call setTableCustomizers.  But if you want,
    * for example, a sync-scheduled-job (what this customizer does) plus some other
    * customizers, then you can call this method to get a code reference that you
    * can add, for example, to {@link com.kingsrook.qqq.backend.core.actions.customizers.MultiCustomizer}
    *
    * @param tableMetaData the table that the customizer will be used on.
    * @param syncProcess instance of the subclass of AbstractRecordSyncToScheduledJobProcess
    *                    that should run in the table's post insert/update/delete
    *                    events.
    * @see #setTableCustomizers(QTableMetaData, AbstractRecordSyncToScheduledJobProcess)
    ***************************************************************************/
   public static QCodeReferenceWithProperties makeCodeReference(QTableMetaData tableMetaData, AbstractRecordSyncToScheduledJobProcess syncProcess)
   {
      return new QCodeReferenceWithProperties(BaseSyncToScheduledJobTableCustomizer.class, Map.of(
         KEY_TABLE_NAME, tableMetaData.getName(),
         KEY_SYNC_PROCESS_NAME, syncProcess.getClass().getSimpleName(),
         KEY_SCHEDULED_JOB_FOREIGN_KEY_TYPE, syncProcess.getScheduledJobForeignKeyType()
      ));
   }



   /***************************************************************************
    * Add post insert/update/delete customizers to a table, that will run a
    * sync-scheduled-job process.
    *
    * @param tableMetaData the table that the customizer will be used on.
    * @param syncProcess instance of the subclass of AbstractRecordSyncToScheduledJobProcess
    *                    that should run in the table's post insert/update/delete
    *                    events.
    ***************************************************************************/
   public static void setTableCustomizers(QTableMetaData tableMetaData, AbstractRecordSyncToScheduledJobProcess syncProcess)
   {
      QCodeReference codeReference = makeCodeReference(tableMetaData, syncProcess);
      tableMetaData.withCustomizer(TableCustomizers.POST_INSERT_RECORD, codeReference);
      tableMetaData.withCustomizer(TableCustomizers.POST_UPDATE_RECORD, codeReference);
      tableMetaData.withCustomizer(TableCustomizers.POST_DELETE_RECORD, codeReference);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void initialize(QCodeReference codeReference)
   {
      if(codeReference instanceof QCodeReferenceWithProperties codeReferenceWithProperties)
      {
         tableName = ValueUtils.getValueAsString(codeReferenceWithProperties.getProperties().get(KEY_TABLE_NAME));
         syncProcessName = ValueUtils.getValueAsString(codeReferenceWithProperties.getProperties().get(KEY_SYNC_PROCESS_NAME));
         scheduledJobForeignKeyType = ValueUtils.getValueAsString(codeReferenceWithProperties.getProperties().get(KEY_SCHEDULED_JOB_FOREIGN_KEY_TYPE));

         if(!StringUtils.hasContent(tableName))
         {
            LOG.warn("Missing property under KEY_TABLE_NAME [" + KEY_TABLE_NAME + "] in codeReference for BaseSyncToScheduledJobTableCustomizer");
         }

         if(!StringUtils.hasContent(syncProcessName))
         {
            LOG.warn("Missing property under KEY_SYNC_PROCESS_NAME [" + KEY_SYNC_PROCESS_NAME + "] in codeReference for BaseSyncToScheduledJobTableCustomizer");
         }

         if(!StringUtils.hasContent(scheduledJobForeignKeyType))
         {
            LOG.warn("Missing property under KEY_SCHEDULED_JOB_FOREIGN_KEY_TYPE [" + KEY_SCHEDULED_JOB_FOREIGN_KEY_TYPE + "] in codeReference for BaseSyncToScheduledJobTableCustomizer");
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<QRecord> postInsertOrUpdate(AbstractActionInput input, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      QBackendTransaction transaction = null;
      if(input instanceof UpdateInput updateInput)
      {
         if(updateInput.hasFlag(AbstractRecordSyncToScheduledJobProcess.ActionFlags.DO_NOT_SYNC))
         {
            return records;
         }
         transaction = updateInput.getTransaction();
      }

      if(input instanceof InsertInput insertInput)
      {
         if(insertInput.hasFlag(AbstractRecordSyncToScheduledJobProcess.ActionFlags.DO_NOT_SYNC))
         {
            return records;
         }
         transaction = insertInput.getTransaction();
      }

      runSyncProcessForRecordList(records, transaction, syncProcessName);
      return records;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<QRecord> postDelete(DeleteInput deleteInput, List<QRecord> records) throws QException
   {
      deleteScheduledJobsForRecordList(records);
      return records;
   }



   /***************************************************************************
    * Run the named process over a set of records (e.g., that were inserted or
    * updated).
    *
    * This method is normally called from within this class, in postInsertOrUpdate.
    *
    * Note that if the {@link ScheduledJob} table isn't defined in the QInstance,
    * that the process will not be called.
    * @param records list of records to use as source records in the table-sync
    *                to the scheduledJob table.
    * @param transaction that the insert or update is happening on - which the
    *                    process we run might need to peek in to for seeing the
    *                    records.
    * @param processName name of the sync-process to run.
    ***************************************************************************/
   public void runSyncProcessForRecordList(List<QRecord> records, QBackendTransaction transaction, String processName)
   {
      if(QContext.getQInstance().getTable(ScheduledJob.TABLE_NAME) == null)
      {
         LOG.info("ScheduledJob table not found, skipping scheduled job sync.");
         return;
      }

      String primaryKeyField = QContext.getQInstance().getTable(tableName).getPrimaryKeyField();

      List<Serializable> sourceRecordIds = records.stream()
         .filter(r -> CollectionUtils.nullSafeIsEmpty(r.getErrors()))
         .map(r -> r.getValue(primaryKeyField))
         .filter(Objects::nonNull).toList();

      if(CollectionUtils.nullSafeIsEmpty(sourceRecordIds))
      {
         return;
      }

      try
      {
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(processName);
         runProcessInput.setCallback(new QProcessCallbackBuilder()
            .withPrimaryKeys("id", sourceRecordIds)
            .withQueryInputCustomizer((runBackendStepInput, queryInput) -> queryInput.setTransaction(transaction))
            .build());
         runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);

         Serializable processSummary = runProcessOutput.getValue(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY);
         ProcessSummaryLineInterface.log("Sync to ScheduledJob Process Summary", processSummary, List.of(logPair("sourceTable", tableName)));
      }
      catch(Exception e)
      {
         LOG.warn("Error syncing records to scheduled jobs", e, logPair("sourceTable", tableName), logPair("sourceRecordIds", sourceRecordIds));
      }
   }



   /***************************************************************************
    * Delete scheduled job records for source-table records that have been deleted.
    *
    * This method is normally called from within this class, in postDelete.
    *
    * Note that if the {@link ScheduledJob} table isn't defined in the QInstance,
    * that the process will not be called.
    *
    * @param records list of records to use as foreign-key sources to identify
    *                scheduledJob records to delete
    ***************************************************************************/
   public void deleteScheduledJobsForRecordList(List<QRecord> records)
   {
      if(QContext.getQInstance().getTable(ScheduledJob.TABLE_NAME) == null)
      {
         LOG.info("ScheduledJob table not found, skipping scheduled job delete.");
         return;
      }

      List<String> sourceRecordIds = records.stream()
         .filter(r -> CollectionUtils.nullSafeIsEmpty(r.getErrors()))
         .map(r -> r.getValueString("id")).toList();

      if(sourceRecordIds.isEmpty())
      {
         return;
      }

      ///////////////////////////////////////////////////
      // delete any corresponding scheduledJob records //
      ///////////////////////////////////////////////////
      try
      {
         new DeleteAction().execute(new DeleteInput(ScheduledJob.TABLE_NAME).withQueryFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("foreignKeyType", QCriteriaOperator.EQUALS, getScheduledJobForeignKeyType()))
            .withCriteria(new QFilterCriteria("foreignKeyValue", QCriteriaOperator.IN, sourceRecordIds))));
      }
      catch(Exception e)
      {
         LOG.warn("Error deleting scheduled jobs for scheduled records", e, logPair("sourceTable", tableName), logPair("sourceRecordIds", sourceRecordIds));
      }
   }



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public BaseSyncToScheduledJobTableCustomizer withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for syncProcessName
    *******************************************************************************/
   public String getSyncProcessName()
   {
      return (this.syncProcessName);
   }



   /*******************************************************************************
    ** Setter for syncProcessName
    *******************************************************************************/
   public void setSyncProcessName(String syncProcessName)
   {
      this.syncProcessName = syncProcessName;
   }



   /*******************************************************************************
    ** Fluent setter for syncProcessName
    *******************************************************************************/
   public BaseSyncToScheduledJobTableCustomizer withSyncProcessName(String syncProcessName)
   {
      this.syncProcessName = syncProcessName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scheduledJobForeignKeyType
    *******************************************************************************/
   public String getScheduledJobForeignKeyType()
   {
      return (this.scheduledJobForeignKeyType);
   }



   /*******************************************************************************
    ** Setter for scheduledJobForeignKeyType
    *******************************************************************************/
   public void setScheduledJobForeignKeyType(String scheduledJobForeignKeyType)
   {
      this.scheduledJobForeignKeyType = scheduledJobForeignKeyType;
   }



   /*******************************************************************************
    ** Fluent setter for scheduledJobForeignKeyType
    *******************************************************************************/
   public BaseSyncToScheduledJobTableCustomizer withScheduledJobForeignKeyType(String scheduledJobForeignKeyType)
   {
      this.scheduledJobForeignKeyType = scheduledJobForeignKeyType;
      return (this);
   }

}
