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

package com.kingsrook.qqq.backend.core.model.scheduledjobs.customizers;


import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.scheduler.CronDescriber;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.quartz.CronScheduleBuilder;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class ScheduledJobTableCustomizer implements TableCustomizerInterface
{
   private QBackendTransaction transaction = null;

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
   {
      transaction = insertInput.getTransaction();
      validateConditionalFields(records, Collections.emptyMap());
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
   {
      transaction = insertInput.getTransaction();
      scheduleJobsForRecordList(records);
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput updateInput, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
   {
      transaction = updateInput.getTransaction();
      Map<Integer, QRecord> freshOldRecordsWithAssociationsMap = CollectionUtils.recordsToMap(freshlyQueryForRecordsWithAssociations(oldRecordList.get()), "id", Integer.class);

      validateConditionalFields(records, freshOldRecordsWithAssociationsMap);

      if(isPreview || oldRecordList.isEmpty())
      {
         return (records);
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // refresh the old-records w/ versions that have associations - so we can use those in the post-update to property unschedule things //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      ListIterator<QRecord> iterator = oldRecordList.get().listIterator();
      while(iterator.hasNext())
      {
         QRecord record      = iterator.next();
         QRecord freshRecord = freshOldRecordsWithAssociationsMap.get(record.getValue("id"));
         if(freshRecord != null)
         {
            iterator.set(freshRecord);
         }
      }

      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void validateConditionalFields(List<QRecord> records, Map<Integer, QRecord> freshOldRecordsWithAssociationsMap)
   {
      QRecord blankRecord = new QRecord();
      for(QRecord record : records)
      {
         QRecord oldRecord = Objects.requireNonNullElse(freshOldRecordsWithAssociationsMap.get(record.getValueInteger("id")), blankRecord);
         String cronExpression = record.getValues().containsKey("cronExpression") ? record.getValueString("cronExpression") : oldRecord.getValueString("cronExpression");
         String cronTimeZoneId = record.getValues().containsKey("cronTimeZoneId") ? record.getValueString("cronTimeZoneId") : oldRecord.getValueString("cronTimeZoneId");
         String repeatSeconds = record.getValues().containsKey("repeatSeconds") ? record.getValueString("repeatSeconds") : oldRecord.getValueString("repeatSeconds");

         if(StringUtils.hasContent(cronExpression))
         {
            if(StringUtils.hasContent(repeatSeconds))
            {
               record.addError(new BadInputStatusMessage("Cron Expression and Repeat Seconds may not both be given."));
            }
            
            try
            {
               CronScheduleBuilder.cronScheduleNonvalidatedExpression(cronExpression);
            }
            catch(ParseException e)
            {
               record.addError(new BadInputStatusMessage("Cron Expression [" + cronExpression + "] is not valid: " + e.getMessage()));
            }

            CronDescriber.setDescriptionInRecord(record, "cronDescription", cronExpression);

            if(!StringUtils.hasContent(cronTimeZoneId))
            {
               record.addError(new BadInputStatusMessage("If a Cron Expression is given, then a Cron Time Zone is required."));
            }
         }
         else
         {
            record.setValue("cronDescription", null);
            if(!StringUtils.hasContent(repeatSeconds))
            {
               record.addError(new BadInputStatusMessage("Either Cron Expression or Repeat Seconds must be given."));
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      transaction = updateInput.getTransaction();
      if(oldRecordList.isPresent())
      {
         Set<Integer> idsWithErrors = getRecordIdsWithErrors(records);
         unscheduleJobsForRecordList(oldRecordList.get(), idsWithErrors);
      }

      scheduleJobsForRecordList(records);

      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Set<Integer> getRecordIdsWithErrors(List<QRecord> records)
   {
      return records.stream()
         .filter(r -> !recordHasErrors().test(r))
         .map(r -> r.getValueInteger("id"))
         .collect(Collectors.toSet());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postDelete(DeleteInput deleteInput, List<QRecord> records) throws QException
   {
      transaction = deleteInput.getTransaction();
      Set<Integer> idsWithErrors = getRecordIdsWithErrors(records);
      unscheduleJobsForRecordList(records, idsWithErrors);
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void scheduleJobsForRecordList(List<QRecord> records)
   {
      List<QRecord> recordsWithoutErrors = records.stream().filter(recordHasErrors()).toList();
      if(CollectionUtils.nullSafeIsEmpty(recordsWithoutErrors))
      {
         return;
      }

      try
      {
         Map<Integer, QRecord> originalRecordMap               = recordsWithoutErrors.stream().collect(Collectors.toMap(r -> r.getValueInteger("id"), r -> r));
         List<QRecord>         freshRecordListWithAssociations = freshlyQueryForRecordsWithAssociations(recordsWithoutErrors);

         QScheduleManager scheduleManager = QScheduleManager.getInstance();
         for(QRecord record : freshRecordListWithAssociations)
         {
            try
            {
               scheduleManager.setupScheduledJob(new ScheduledJob(record));
            }
            catch(Exception e)
            {
               LOG.warn("Caught exception while scheduling a job in post-action", e, logPair("id", record.getValue("id")));
               if(originalRecordMap.containsKey(record.getValueInteger("id")))
               {
                  originalRecordMap.get(record.getValueInteger("id")).addWarning(new QWarningMessage("Error scheduling job: " + e.getMessage()));
               }
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error scheduling jobs in post-action", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Predicate<QRecord> recordHasErrors()
   {
      return r -> CollectionUtils.nullSafeIsEmpty(r.getErrors());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> freshlyQueryForRecordsWithAssociations(List<QRecord> records) throws QException
   {
      List<Integer> idList = records.stream().map(r -> r.getValueInteger("id")).toList();

      return new QueryAction().execute(new QueryInput(ScheduledJob.TABLE_NAME)
            .withIncludeAssociations(true)
            .withTransaction(transaction)
            .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, idList))))
         .getRecords();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void unscheduleJobsForRecordList(List<QRecord> oldRecords, Set<Integer> exceptIdsWithErrors)
   {
      try
      {
         QScheduleManager scheduleManager = QScheduleManager.getInstance();

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////
         // for un-schedule - use the old records as they are - don't re-query them (they may not exist anymore!) //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////
         for(QRecord record : oldRecords)
         {
            try
            {
               ScheduledJob scheduledJob = new ScheduledJob(record);

               if(exceptIdsWithErrors.contains(scheduledJob.getId()))
               {
                  LOG.info("Will not unschedule the job for a record that had an error", logPair("id", scheduledJob.getId()));
                  continue;
               }

               scheduleManager.unscheduleScheduledJob(scheduledJob);
            }
            catch(Exception e)
            {
               LOG.warn("Caught exception while un-scheduling a job in post-action", e, logPair("id", record.getValue("id")));
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error scheduling jobs in post-action", e);
      }
   }

}
