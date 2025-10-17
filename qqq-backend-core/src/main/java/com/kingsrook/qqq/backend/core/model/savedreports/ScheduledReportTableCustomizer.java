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

package com.kingsrook.qqq.backend.core.model.savedreports;


import java.io.Serializable;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.RecordCustomizerUtilityInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.scheduler.CronDescriber;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValidationUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.quartz.CronScheduleBuilder;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class ScheduledReportTableCustomizer implements TableCustomizerInterface
{
   private static final QLogger LOG = QLogger.getLogger(ScheduledReportTableCustomizer.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
   {
      preInsertOrUpdate(records, Optional.empty());
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput updateInput, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
   {
      preInsertOrUpdate(records, oldRecordList);
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void preInsertOrUpdate(List<QRecord> records, Optional<List<QRecord>> oldRecordList)
   {
      Optional<Map<Serializable, QRecord>> oldRecordMap = oldRecordListToMap("id", oldRecordList);

      for(QRecord record : records)
      {
         String cronExpression = ValueUtils.getValueAsString(RecordCustomizerUtilityInterface.getValueFromRecordElseFromOldRecord("cronExpression", record, record.getValue("id"), oldRecordMap));
         try
         {
            CronScheduleBuilder.cronScheduleNonvalidatedExpression(cronExpression);
         }
         catch(ParseException e)
         {
            record.addError(new BadInputStatusMessage("Cron Expression [" + cronExpression + "] is not valid: " + e.getMessage()));
         }

         CronDescriber.setDescriptionInRecord(record, "cronDescription", cronExpression);

         try
         {
            String toAddresses = record.getValueString("toAddresses");
            if(StringUtils.hasContent(toAddresses))
            {
               ValidationUtils.parseAndValidateEmailAddresses(toAddresses);
            }
         }
         catch(QUserFacingException ufe)
         {
            record.addError(new BadInputStatusMessage(ufe.getMessage()));
         }
         catch(Exception e)
         {
            record.addError(new BadInputStatusMessage("To Addresses is not valid: " + e.getMessage()));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
   {
      runSyncProcess(records);
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      runSyncProcess(records);
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void runSyncProcess(List<QRecord> records)
   {
      List<Serializable> scheduledReportIds = records.stream()
         .filter(r -> CollectionUtils.nullSafeIsEmpty(r.getErrors()))
         .map(r -> r.getValue("id")).toList();

      if(CollectionUtils.nullSafeIsEmpty(scheduledReportIds))
      {
         return;
      }

      try
      {
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(ScheduledReportSyncToScheduledJobProcess.NAME);
         runProcessInput.setCallback(QProcessCallbackFactory.forPrimaryKeys("id", scheduledReportIds));
         runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);

         Serializable processSummary = runProcessOutput.getValue(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY);
         System.out.println(processSummary);
      }
      catch(Exception e)
      {
         LOG.warn("Error syncing scheduled reports to scheduled jobs", e, logPair("scheduledReportIds", scheduledReportIds));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postDelete(DeleteInput deleteInput, List<QRecord> records) throws QException
   {
      List<String> scheduledReportIds = records.stream()
         .filter(r -> CollectionUtils.nullSafeIsEmpty(r.getErrors()))
         .map(r -> r.getValueString("id")).toList();

      if(scheduledReportIds.isEmpty())
      {
         return (records);
      }

      ///////////////////////////////////////////////////
      // delete any corresponding scheduledJob records //
      ///////////////////////////////////////////////////
      try
      {
         DeleteOutput deleteOutput = new DeleteAction().execute(new DeleteInput(ScheduledJob.TABLE_NAME).withQueryFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("foreignKeyType", QCriteriaOperator.EQUALS, ScheduledReportSyncToScheduledJobProcess.getScheduledJobForeignKeyType()))
            .withCriteria(new QFilterCriteria("foreignKeyValue", QCriteriaOperator.IN, scheduledReportIds))
         ));

      }
      catch(Exception e)
      {
         LOG.warn("Error deleting scheduled jobs for scheduled reports", e);
      }

      return (records);
   }
}
