/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.delete;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.general.ProcessSummaryWarningsAndErrorsRollup;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Transform step for generic table bulk-insert ETL process
 *******************************************************************************/
public class BulkDeleteTransformStep extends AbstractTransformStep
{
   private ProcessSummaryLine okSummary = new ProcessSummaryLine(Status.OK);

   private ProcessSummaryWarningsAndErrorsRollup processSummaryWarningsAndErrorsRollup = ProcessSummaryWarningsAndErrorsRollup.build("deleted");

   private String tableLabel;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void preRun(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ///////////////////////////////////////////////////////
      // capture the table label - for the process summary //
      ///////////////////////////////////////////////////////
      QTableMetaData table = QContext.getQInstance().getTable(runBackendStepInput.getTableName());
      if(table != null)
      {
         tableLabel = table.getLabel();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      QTableMetaData table           = runBackendStepInput.getTable();
      String         primaryKeyField = table.getPrimaryKeyField();

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // on the validate step, we haven't read the full file, so we don't know how many rows there are - thus        //
      // record count is null, and the ValidateStep won't be setting status counters - so - do it here in that case. //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(runBackendStepInput.getStepName().equals(StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE)
         || runBackendStepInput.getStepName().equals(StreamedETLWithFrontendProcess.STEP_NAME_PREVIEW))
      {
         if(runBackendStepInput.getValue(StreamedETLWithFrontendProcess.FIELD_RECORD_COUNT) == null)
         {
            runBackendStepInput.getAsyncJobCallback().updateStatus("Processing " + tableLabel + " record " + "%,d".formatted(okSummary.getCount()));
         }
         else
         {
            runBackendStepInput.getAsyncJobCallback().updateStatus("Processing " + tableLabel + " record");
         }

         ///////////////////////////////////////////////////////////////////////
         // run the validation - critically - in preview mode (boolean param) //
         ///////////////////////////////////////////////////////////////////////
         DeleteAction deleteAction = new DeleteAction();
         DeleteInput  deleteInput  = new DeleteInput();
         deleteInput.setInputSource(QInputSource.USER);
         deleteInput.setTableName(runBackendStepInput.getTableName());
         deleteInput.setPrimaryKeys(runBackendStepInput.getRecords().stream().map(r -> r.getValue(primaryKeyField)).toList());
         List<QRecord> validationResultRecords = deleteAction.performValidations(deleteInput, Optional.of(runBackendStepInput.getRecords()), true);

         /////////////////////////////////////////////////////////////
         // look at the update input to build process summary lines //
         /////////////////////////////////////////////////////////////
         List<QRecord> outputRecords = new ArrayList<>();
         for(QRecord record : validationResultRecords)
         {
            Serializable recordPrimaryKey = record.getValue(table.getPrimaryKeyField());
            if(CollectionUtils.nullSafeHasContents(record.getErrors()))
            {
               String message = record.getErrors().get(0).getMessage();
               processSummaryWarningsAndErrorsRollup.addError(message, recordPrimaryKey);
            }
            else if(CollectionUtils.nullSafeHasContents(record.getWarnings()))
            {
               String message = record.getWarnings().get(0).getMessage();
               processSummaryWarningsAndErrorsRollup.addWarning(message, recordPrimaryKey);
               outputRecords.add(record);
            }
            else
            {
               okSummary.incrementCountAndAddPrimaryKey(recordPrimaryKey);
               outputRecords.add(record);
            }
         }

         runBackendStepOutput.setRecords(outputRecords);
      }
      else if(runBackendStepInput.getStepName().equals(StreamedETLWithFrontendProcess.STEP_NAME_EXECUTE))
      {
         if(runBackendStepInput.getValue(StreamedETLWithFrontendProcess.FIELD_RECORD_COUNT) == null)
         {
            runBackendStepInput.getAsyncJobCallback().updateStatus("Deleting " + tableLabel + " record " + "%,d".formatted(okSummary.getCount()));
         }
         else
         {
            runBackendStepInput.getAsyncJobCallback().updateStatus("Deleting " + tableLabel + " records");
         }

         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // no transformation needs done - just pass records through from input to output, and assume errors & warnings will come from the delete action //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         runBackendStepOutput.setRecords(runBackendStepInput.getRecords());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      ArrayList<ProcessSummaryLineInterface> rs = new ArrayList<>();

      String noWarningsSuffix = processSummaryWarningsAndErrorsRollup.countWarnings() == 0 ? "" : " with no warnings";
      okSummary.setSingularFutureMessage(tableLabel + " record will be deleted" + noWarningsSuffix + ".");
      okSummary.setPluralFutureMessage(tableLabel + " records will be deleted" + noWarningsSuffix + ".");
      okSummary.pickMessage(isForResultScreen);
      okSummary.addSelfToListIfAnyCount(rs);

      processSummaryWarningsAndErrorsRollup.addToList(rs);

      return (rs);
   }
}
