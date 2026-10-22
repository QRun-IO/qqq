/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaInsertStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.general.ProcessSummaryWarningsAndErrorsRollup;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class BulkInsertLoadStep extends LoadViaInsertStep implements ProcessSummaryProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(BulkInsertLoadStep.class);

   private Serializable firstInsertedPrimaryKey = null;
   private Serializable lastInsertedPrimaryKey  = null;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected InputSource getInputSource()
   {
      return (QInputSource.USER);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      super.runOnePage(runBackendStepInput, runBackendStepOutput);

      QTableMetaData table = QContext.getQInstance().getTable(runBackendStepInput.getValueString("tableName"));

      /////////////////////////////////////////////////////////////////////////////////////////////
      // the transform step builds summary lines that it predicts will insert successfully.      //
      // but those lines don't have ids, which we'd like to have (e.g., for a process trace that //
      // might link to the built record).  also, it's possible that there was a fail that only   //
      // happened in the actual insert, so, basically, re-do the summary here                    //
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

      List<QRecord> insertedRecords = runBackendStepOutput.getRecords();
      for(QRecord insertedRecord : insertedRecords)
      {
         Serializable primaryKey = insertedRecord.getValue(table.getPrimaryKeyField());
         if(CollectionUtils.nullSafeIsEmpty(insertedRecord.getErrors()) && primaryKey != null)
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

            if(!CollectionUtils.nullSafeIsEmpty(insertedRecord.getWarnings()))
            {
               /////////////////////////////////////////////////////////////////////////////
               // if there were warnings on the inserted record, put it in a warning line //
               /////////////////////////////////////////////////////////////////////////////
               String message = insertedRecord.getWarnings().get(0).getMessage();
               processSummaryWarningsAndErrorsRollup.addWarning(message, primaryKey);
            }
            else
            {
               ////////////////////////////////////////////////////////////////////////
               // if no warnings for the inserted record, then put it in the OK line //
               ////////////////////////////////////////////////////////////////////////
               okSummary.incrementCountAndAddPrimaryKey(primaryKey);
            }
         }
         else
         {
            //////////////////////////////////////////////////////////////////////
            // else if there were errors or no primary key, build an error line //
            //////////////////////////////////////////////////////////////////////
            String message = "Failed to insert";
            if(!CollectionUtils.nullSafeIsEmpty(insertedRecord.getErrors()))
            {
               //////////////////////////////////////////////////////////
               // use the error message from the record if we have one //
               //////////////////////////////////////////////////////////
               message = insertedRecord.getErrors().get(0).getMessage();
            }
            processSummaryWarningsAndErrorsRollup.addError(message, primaryKey);
         }
      }

      okSummary.pickMessage(true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      ArrayList<ProcessSummaryLineInterface> processSummary = getTransformStep().getProcessSummary(runBackendStepOutput, isForResultScreen);

      try
      {
         if(firstInsertedPrimaryKey != null)
         {
            QTableMetaData table = QContext.getQInstance().getTable(runBackendStepOutput.getValueString("tableName"));
            QFieldMetaData field = table.getField(table.getPrimaryKeyField());
            if(field.getType().isNumeric())
            {
               ProcessSummaryLine idsLine = new ProcessSummaryLine(Status.INFO, "Inserted " + field.getLabel() + " values between " + firstInsertedPrimaryKey + " and " + lastInsertedPrimaryKey);
               if(Objects.equals(firstInsertedPrimaryKey, lastInsertedPrimaryKey))
               {
                  idsLine.setMessage("Inserted " + field.getLabel() + " " + firstInsertedPrimaryKey);
               }
               idsLine.setCount(null);
               processSummary.add(idsLine);
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error adding inserted-keys process summary line", e);
      }

      return (processSummary);
   }
}
