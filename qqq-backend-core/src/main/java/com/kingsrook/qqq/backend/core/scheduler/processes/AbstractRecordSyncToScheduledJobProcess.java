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
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.ActionFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobParameter;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobType;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.tablesync.AbstractTableSyncTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.tablesync.TableSyncProcess;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;


/*******************************************************************************
 * Base class to manage creating scheduled jobs based on records in another table
 *
 * Expected to be used via BaseSyncToScheduledJobTableCustomizer - see its javadoc.
 * @see BaseSyncToScheduledJobTableCustomizer
 *******************************************************************************/
public abstract class AbstractRecordSyncToScheduledJobProcess extends AbstractTableSyncTransformStep implements MetaDataProducerInterface<QProcessMetaData>
{
   private static final QLogger LOG = QLogger.getLogger(AbstractRecordSyncToScheduledJobProcess.class);

   public static final String SCHEDULER_NAME_FIELD_NAME = "schedulerName";



   /***************************************************************************
    * action flags that can be put in an insert/update/delete input to control
    * behavior of this process.
    ***************************************************************************/
   public enum ActionFlags implements ActionFlag
   {
      /***************************************************************************
       * tell this process not to run upon such an action taken on the source table.
       ***************************************************************************/
      DO_NOT_SYNC
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      QProcessMetaData processMetaData = TableSyncProcess.processMetaDataBuilder(false)
         .withName(getClass().getSimpleName())
         .withSyncTransformStepClass(getClass())
         .withReviewStepRecordFields(List.of(
            new QFieldMetaData(getRecordForeignKeyFieldName(), QFieldType.INTEGER).withPossibleValueSourceName(getRecordForeignKeyPossibleValueSourceName()),
            new QFieldMetaData("cronExpression", QFieldType.STRING),
            new QFieldMetaData("isActive", QFieldType.BOOLEAN)
         ))
         .getProcessMetaData();

      processMetaData.getBackendStep(StreamedETLWithFrontendProcess.STEP_NAME_PREVIEW).getInputMetaData()
         .withField(new QFieldMetaData(SCHEDULER_NAME_FIELD_NAME, QFieldType.STRING));

      return (processMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QRecord populateRecordToStore(RunBackendStepInput runBackendStepInput, QRecord destinationRecord, QRecord sourceRecord) throws QException
   {
      ScheduledJob scheduledJob;
      if(destinationRecord == null || destinationRecord.getValue("id") == null)
      {
         QInstance qInstance = QContext.getQInstance();

         ////////////////////////////////////////////////////////////////
         // this is the table at which the scheduled job will point to //
         ////////////////////////////////////////////////////////////////
         QTableMetaData sourceTableMetaData = qInstance.getTable(getSourceTableName());
         String         sourceTableId       = String.valueOf(sourceRecord.getValueString(sourceTableMetaData.getPrimaryKeyField()));
         String         sourceTableJobKey   = getSourceTableName() + "Id";

         ///////////////////////////////////////////////////////////
         // this is the table that the scheduled record points to //
         ///////////////////////////////////////////////////////////
         QTableMetaData recordForeignTableMetaData = qInstance.getTable(getRecordForeignKeyPossibleValueSourceName());
         String         sourceRecordForeignKeyId   = sourceRecord.getValueString(getRecordForeignKeyFieldName());

         ////////////////////////////////////////////////////////////////////////
         // need to do an insert - set lots of key values in the scheduled job //
         ////////////////////////////////////////////////////////////////////////
         scheduledJob = new ScheduledJob();
         scheduledJob.setSchedulerName(runBackendStepInput.getValueString(SCHEDULER_NAME_FIELD_NAME));
         scheduledJob.setType(ScheduledJobType.PROCESS.name());
         scheduledJob.setForeignKeyType(getSourceTableName());
         scheduledJob.setForeignKeyValue(sourceTableId);
         scheduledJob.setJobParameters(ListBuilder.of(
            new ScheduledJobParameter().withKey("isScheduledJob").withValue("true"),
            new ScheduledJobParameter().withKey("processName").withValue(getProcessNameScheduledJobParameter()),
            new ScheduledJobParameter().withKey(sourceTableJobKey).withValue(sourceTableId),
            new ScheduledJobParameter().withKey("recordId").withValue(ValueUtils.getValueAsString(sourceRecordForeignKeyId))
         ));

         //////////////////////////////////////////////////////////////////////////
         // make a call to allow subclasses to customize parts of the job record //
         //////////////////////////////////////////////////////////////////////////
         scheduledJob.setLabel(recordForeignTableMetaData.getLabel() + " " + sourceRecordForeignKeyId);
         scheduledJob.setDescription("Job to run " + sourceTableMetaData.getLabel() + " Id " + sourceTableId
            + " (which runs for " + recordForeignTableMetaData.getLabel() + " Id " + sourceRecordForeignKeyId + ")");
      }
      else
      {
         //////////////////////////////////////////////////////////////////////////////////
         // else doing an update - populate scheduled job entity from destination record //
         //////////////////////////////////////////////////////////////////////////////////
         scheduledJob = new ScheduledJob(destinationRecord);
      }

      //////////////////////////////////////////////////////////////////////////////////
      // these fields sync on insert and update                                       //
      // todo - if no diffs, should we return null (to avoid changing quartz at all?) //
      //////////////////////////////////////////////////////////////////////////////////
      scheduledJob.setCronExpression(sourceRecord.getValueString("cronExpression"));
      scheduledJob.setCronTimeZoneId(sourceRecord.getValueString("cronTimeZoneId"));
      scheduledJob.setIsActive(true);

      scheduledJob = customizeScheduledJob(scheduledJob, sourceRecord);

      ////////////////////////////////////////////////////////////////////
      // try to make sure scheduler name is set (and fail if it isn't!) //
      ////////////////////////////////////////////////////////////////////
      makeSureSchedulerNameIsSet(scheduledJob);

      return scheduledJob.toQRecord();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected void makeSureSchedulerNameIsSet(ScheduledJob scheduledJob) throws QException
   {
      if(!StringUtils.hasContent(scheduledJob.getSchedulerName()))
      {
         Map<String, QSchedulerMetaData> schedulers = QContext.getQInstance().getSchedulers();
         if(schedulers.size() == 1)
         {
            scheduledJob.setSchedulerName(schedulers.keySet().iterator().next());
         }
      }

      if(!StringUtils.hasContent(scheduledJob.getSchedulerName()))
      {
         String message = "Could not determine scheduler name for webhook scheduled job.";
         LOG.warn(message);
         throw (new QException(message));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected ScheduledJob customizeScheduledJob(ScheduledJob scheduledJob, QRecord sourceRecord) throws QException
   {
      return (scheduledJob);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected QQueryFilter getExistingRecordQueryFilter(RunBackendStepInput runBackendStepInput, List<Serializable> sourceKeyList)
   {
      return super.getExistingRecordQueryFilter(runBackendStepInput, sourceKeyList)
         .withCriteria(new QFilterCriteria("foreignKeyType", QCriteriaOperator.EQUALS, getScheduledJobForeignKeyType()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected SyncProcessConfig getSyncProcessConfig()
   {
      return new SyncProcessConfig(getSourceTableName(), getSourceTableKeyField(), ScheduledJob.TABLE_NAME, "foreignKeyValue", true, true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected abstract String getScheduledJobForeignKeyType();


   /*******************************************************************************
    **
    *******************************************************************************/
   protected abstract String getRecordForeignKeyFieldName();


   /*******************************************************************************
    **
    *******************************************************************************/
   protected abstract String getRecordForeignKeyPossibleValueSourceName();

   /*******************************************************************************
    **
    *******************************************************************************/
   protected abstract String getSourceTableName();


   /*******************************************************************************
    **
    *******************************************************************************/
   protected abstract String getProcessNameScheduledJobParameter();



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String getSourceTableKeyField()
   {
      return ("id");
   }

}
