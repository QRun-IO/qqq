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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.edit;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaUpdateStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.general.ProcessSummaryWarningsAndErrorsRollup;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.processes.implementations.bulk.edit.BulkEditTransformStep.buildInfoSummaryLines;


/*******************************************************************************
 ** Load step for generic table bulk-edit ETL process
 *******************************************************************************/
public class BulkEditLoadStep extends LoadViaUpdateStep implements ProcessSummaryProviderInterface
{
   public static final String FIELD_ENABLED_FIELDS = "bulkEditEnabledFields";

   private ProcessSummaryLine       okSummary     = new ProcessSummaryLine(Status.OK);
   private List<ProcessSummaryLine> infoSummaries = new ArrayList<>();

   private Serializable firstInsertedPrimaryKey = null;
   private Serializable lastInsertedPrimaryKey  = null;

   private ProcessSummaryWarningsAndErrorsRollup processSummaryWarningsAndErrorsRollup = ProcessSummaryWarningsAndErrorsRollup.build("edited");

   private String tableLabel;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected InputSource getInputSource()
   {
      return (QInputSource.USER);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      ArrayList<ProcessSummaryLineInterface> rs = new ArrayList<>();

      String noWarningsSuffix = processSummaryWarningsAndErrorsRollup.countWarnings() == 0 ? "" : " with no warnings";

      okSummary.setSingularPastMessage(tableLabel + " record was edited" + noWarningsSuffix + ".");
      okSummary.setPluralPastMessage(tableLabel + " records were edited" + noWarningsSuffix + ".");
      okSummary.pickMessage(isForResultScreen);
      okSummary.addSelfToListIfAnyCount(rs);

      processSummaryWarningsAndErrorsRollup.addToList(rs);
      rs.addAll(infoSummaries);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void preRun(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      super.preRun(runBackendStepInput, runBackendStepOutput);

      QTableMetaData table = QContext.getQInstance().getTable(runBackendStepInput.getTableName());
      if(table != null)
      {
         tableLabel = table.getLabel();
      }

      boolean isBulkEdit = BooleanUtils.isTrue(runBackendStepInput.getValueBoolean("isBulkEdit"));
      if(isBulkEdit)
      {
         buildBulkUpdateWithFileInfoSummaryLines(runBackendStepOutput, table);
      }
      else
      {
         buildInfoSummaryLines(runBackendStepInput, table, infoSummaries, true);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ////////////////////////////
      // have base class update //
      ////////////////////////////
      super.runOnePage(runBackendStepInput, runBackendStepOutput);

      ////////////////////////////////////////////////////////
      // roll up results based on output from update action //
      ////////////////////////////////////////////////////////
      QTableMetaData table = QContext.getQInstance().getTable(runBackendStepInput.getTableName());
      for(QRecord record : runBackendStepOutput.getRecords())
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
         }
         else
         {
            okSummary.incrementCountAndAddPrimaryKey(recordPrimaryKey);
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void buildBulkUpdateWithFileInfoSummaryLines(RunBackendStepOutput runBackendStepOutput, QTableMetaData table)
   {
      /////////////////////////////////////////////////////////////////////////////////////////////
      // the transform step builds summary lines that it predicts will update successfully.      //
      // but those lines don't have ids, which we'd like to have (e.g., for a process trace that //
      // might link to the built record).  also, it's possible that there was a fail that only   //
      // happened in the actual update, so, basically, re-do the summary here                    //
      /////////////////////////////////////////////////////////////////////////////////////////////
      BulkInsertTransformStep transformStep = (BulkInsertTransformStep) getTransformStep();
      ProcessSummaryLine      okSummary     = transformStep.okSummary;
      okSummary.setCount(0);
      okSummary.setPrimaryKeys(new ArrayList<>());

      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // but - since errors from the transform step don't even make it through to us here in the load step,   //
      // do re-use the ProcessSummaryWarningsAndErrorsRollup from transform step as follows:                  //
      // clear out its warnings - we'll completely rebuild them here (with primary keys)                      //
      // and add new error lines, e.g., in case of errors that only happened past the validation if possible. //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      ProcessSummaryWarningsAndErrorsRollup processSummaryWarningsAndErrorsRollup = transformStep.processSummaryWarningsAndErrorsRollup;
      processSummaryWarningsAndErrorsRollup.resetWarnings();

      List<QRecord> updatedRecords = runBackendStepOutput.getRecords();
      for(QRecord updatedRecord : updatedRecords)
      {
         Serializable primaryKey = updatedRecord.getValue(table.getPrimaryKeyField());
         if(CollectionUtils.nullSafeIsEmpty(updatedRecord.getErrors()) && primaryKey != null)
         {
            /////////////////////////////////////////////////////////////////////////
            // if the record had no errors, and we have a primary key for it, then //
            // keep track of the range of primary keys (first and last)            //
            /////////////////////////////////////////////////////////////////////////
            if(firstInsertedPrimaryKey == null)
            {
               firstInsertedPrimaryKey = primaryKey;
            }

            lastInsertedPrimaryKey = primaryKey;

            if(!CollectionUtils.nullSafeIsEmpty(updatedRecord.getWarnings()))
            {
               ////////////////////////////////////////////////////////////////////////////
               // if there were warnings on the updated record, put it in a warning line //
               ////////////////////////////////////////////////////////////////////////////
               String message = updatedRecord.getWarnings().get(0).getMessage();
               processSummaryWarningsAndErrorsRollup.addWarning(message, primaryKey);
            }
            else
            {
               ///////////////////////////////////////////////////////////////////////
               // if no warnings for the updated record, then put it in the OK line //
               ///////////////////////////////////////////////////////////////////////
               okSummary.incrementCountAndAddPrimaryKey(primaryKey);
            }
         }
         else
         {
            //////////////////////////////////////////////////////////////////////
            // else if there were errors or no primary key, build an error line //
            //////////////////////////////////////////////////////////////////////
            String message = "Failed to update";
            if(!CollectionUtils.nullSafeIsEmpty(updatedRecord.getErrors()))
            {
               //////////////////////////////////////////////////////////
               // use the error message from the record if we have one //
               //////////////////////////////////////////////////////////
               message = updatedRecord.getErrors().get(0).getMessage();
            }
            processSummaryWarningsAndErrorsRollup.addError(message, primaryKey);
         }
      }

      okSummary.pickMessage(true);
   }
}
