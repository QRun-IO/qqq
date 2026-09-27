/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.savedreports;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormatPossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobParameter;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobType;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.savedreports.RunScheduledReportMetaDataProducer;
import com.kingsrook.qqq.backend.core.processes.implementations.tablesync.AbstractTableSyncTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.tablesync.TableSyncProcess;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class ScheduledReportSyncToScheduledJobProcess extends AbstractTableSyncTransformStep implements MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "scheduledReportSyncToScheduledJob";

   public static final String SCHEDULER_NAME_FIELD_NAME = "schedulerName";

   private static final QLogger LOG = QLogger.getLogger(ScheduledReportSyncToScheduledJobProcess.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      QProcessMetaData processMetaData = TableSyncProcess.processMetaDataBuilder(false)
         .withName(NAME)
         .withTableName(ScheduledReport.TABLE_NAME)

         /////////////////////////////////////////
         // todo - maybe - to keep 'em in sync? //
         /////////////////////////////////////////
         //.withBasepullConfiguration(CoreMetaDataProvider.getDefaultBasepullConfiguration("modifyDate", ONE_DAY_IN_HOURS)
         //   .withSecondsToSubtractFromLastRunTimeForTimestampQuery(10 * 60))
         // .withSchedule(new QScheduleMetaData()
         //    .withRepeatSeconds(SYNC_BASEPULLS_INTERVAL_SECONDS))

         .withSyncTransformStepClass(getClass())
         .withReviewStepRecordFields(List.of(
            new QFieldMetaData("savedReportId", QFieldType.INTEGER).withPossibleValueSourceName(SavedReport.TABLE_NAME),
            new QFieldMetaData("cronExpression", QFieldType.STRING),
            new QFieldMetaData("isActive", QFieldType.BOOLEAN),
            new QFieldMetaData("toAddresses", QFieldType.STRING),
            new QFieldMetaData("subject", QFieldType.STRING),
            new QFieldMetaData("format", QFieldType.STRING).withPossibleValueSourceName(ReportFormatPossibleValueEnum.NAME)
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
      ScheduledReport scheduledReport = new ScheduledReport(sourceRecord);
      ScheduledJob    scheduledJob;

      if(destinationRecord == null || destinationRecord.getValue("id") == null)
      {
         ////////////////////////////////////////////////////////////////////////
         // need to do an insert - set lots of key values in the scheduled job //
         ////////////////////////////////////////////////////////////////////////
         scheduledJob = new ScheduledJob();
         scheduledJob.setLabel("Scheduled Report " + scheduledReport.getId());
         scheduledJob.setDescription("Job to run Scheduled Report Id " + scheduledReport.getId()
                                     + " (which runs Report Id " + scheduledReport.getSavedReportId() + ")");
         scheduledJob.setSchedulerName(runBackendStepInput.getValueString(SCHEDULER_NAME_FIELD_NAME));
         scheduledJob.setType(ScheduledJobType.PROCESS.name());
         scheduledJob.setForeignKeyType(getScheduledJobForeignKeyType());
         scheduledJob.setForeignKeyValue(String.valueOf(scheduledReport.getId()));
         scheduledJob.setJobParameters(List.of(
            new ScheduledJobParameter().withKey("processName").withValue(getProcessNameScheduledJobParameter()),
            new ScheduledJobParameter().withKey("recordId").withValue(ValueUtils.getValueAsString(scheduledReport.getId()))
         ));
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
      scheduledJob.setCronExpression(scheduledReport.getCronExpression());
      scheduledJob.setCronTimeZoneId(scheduledReport.getCronTimeZoneId());
      scheduledJob.setIsActive(scheduledReport.getIsActive());

      return scheduledJob.toQRecord();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static String getScheduledJobForeignKeyType()
   {
      return "scheduledReport";
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String getProcessNameScheduledJobParameter()
   {
      return RunScheduledReportMetaDataProducer.NAME;
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
      return new SyncProcessConfig(ScheduledReport.TABLE_NAME, "id", ScheduledJob.TABLE_NAME, "foreignKeyValue", true, true);
   }

}
