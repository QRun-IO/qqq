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

package com.kingsrook.qqq.backend.core.model.scheduledjobs.customizers;


import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobParameter;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class ScheduledJobParameterTableCustomizer implements TableCustomizerInterface
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
   {
      ///////////////////////////////////////////////////////////////////////////////////////
      // if we're in this insert as a result of an insert (or update) on a different table //
      // (e.g., under a manageAssociations call), then return with noop - assume that the  //
      // parent table's customizer will do what needed to be done.                         //
      ///////////////////////////////////////////////////////////////////////////////////////
      if(!isThisAnActionDirectlyOnThisTable())
      {
         return (records);
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // else - this was an action directly on this table - so bump all of the parent records, to get them rescheduled //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      bumpParentRecords(records, Optional.empty());

      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void bumpParentRecords(List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      try
      {
         ///////////////////////////////////////////////////////////////////////////////////////////
         // (listing) hash up the records by scheduledJobId - we'll use this to have a set of the //
         // job ids, and in case we need to add warnings to them later                            //
         ///////////////////////////////////////////////////////////////////////////////////////////
         ListingHash<Integer, QRecord> recordsByJobId = new ListingHash<>();
         for(QRecord record : records)
         {
            recordsByJobId.add(record.getValueInteger("scheduledJobId"), record);
         }

         Set<Integer> scheduledJobIds = new HashSet<>(recordsByJobId.keySet());

         ////////////////////////////////////////////////////////////////////////////////
         // if we have an old record list (e.g., is an edit), add any job ids that are //
         // in those too, e.g., in case moving a param from one job to another...      //
         // note, we won't line these up for doing a proper warning on these...        //
         ////////////////////////////////////////////////////////////////////////////////
         if(oldRecordList.isPresent())
         {
            for(QRecord oldRecord : oldRecordList.get())
            {
               scheduledJobIds.add(oldRecord.getValueInteger("scheduledJobId"));
            }
         }

         ////////////////////////////////////////////////////////////////////////////////////////////////////
         // update the modify date on the scheduled jobs - to get their post-actions to run, to reschedule //
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(ScheduledJob.TABLE_NAME);
         updateInput.setRecords(scheduledJobIds.stream()
            .map(id -> new QRecord().withValue("id", id).withValue("modifyDate", Instant.now()))
            .toList());
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);

         ////////////////////////////////////////////////////////////////////////////////////////
         // look for warnings on those jobs - and propagate them to the params we just stored. //
         ////////////////////////////////////////////////////////////////////////////////////////
         for(QRecord updatedScheduledJob : updateOutput.getRecords())
         {
            if(CollectionUtils.nullSafeHasContents(updatedScheduledJob.getWarnings()))
            {
               for(QRecord paramToWarn : CollectionUtils.nonNullList(recordsByJobId.get(updatedScheduledJob.getValueInteger("id"))))
               {
                  paramToWarn.setWarnings(updatedScheduledJob.getWarnings());
               }
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error in scheduledJobParameter post-crud", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      /////////////////////////////////////////////////////////////////////////////
      // if we're in this update as a result of an update on a different table   //
      // (e.g., under a manageAssociations call), then return with noop - assume //
      // that the parent table's customizer will do what needed to be done.      //
      /////////////////////////////////////////////////////////////////////////////
      if(!isThisAnActionDirectlyOnThisTable())
      {
         return (records);
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // else - this was an action directly on this table - so bump all of the parent records, to get them rescheduled //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      bumpParentRecords(records, oldRecordList);

      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postDelete(DeleteInput deleteInput, List<QRecord> records) throws QException
   {
      /////////////////////////////////////////////////////////////////////////////
      // if we're in this update as a result of an update on a different table   //
      // (e.g., under a manageAssociations call), then return with noop - assume //
      // that the parent table's customizer will do what needed to be done.      //
      /////////////////////////////////////////////////////////////////////////////
      if(!isThisAnActionDirectlyOnThisTable())
      {
         return (records);
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // else - this was an action directly on this table - so bump all of the parent records, to get them rescheduled //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      bumpParentRecords(records, Optional.empty());

      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean isThisAnActionDirectlyOnThisTable()
   {
      Optional<AbstractActionInput> firstActionInStack = QContext.getFirstActionInStack();
      if(firstActionInStack.isPresent())
      {
         if(firstActionInStack.get() instanceof AbstractTableActionInput tableActionInput)
         {
            if(!ScheduledJobParameter.TABLE_NAME.equals(tableActionInput.getTableName()))
            {
               return (false);
            }
         }
         else if(firstActionInStack.get() instanceof RunProcessInput runProcessInput)
         {
            String tableName = runProcessInput.getValueString("tableName");
            if(StringUtils.hasContent(tableName))
            {
               if(!ScheduledJobParameter.TABLE_NAME.equals(tableName))
               {
                  return (false);
               }
            }
         }
      }
      return (true);
   }

}
