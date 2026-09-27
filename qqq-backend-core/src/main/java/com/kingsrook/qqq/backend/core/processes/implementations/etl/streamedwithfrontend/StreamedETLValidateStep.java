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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.async.AsyncRecordPipeLoop;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Backend step to do a full validation of a streamed ETL job
 *******************************************************************************/
public class StreamedETLValidateStep extends BaseStreamedETLStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(StreamedETLValidateStep.class);

   private int currentRowCount = 1;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   @SuppressWarnings("checkstyle:indentation")
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      /////////////////////////////////////////////////////////////////////
      // check if we are supported in this process - if not, return noop //
      /////////////////////////////////////////////////////////////////////
      boolean supportsFullValidation = runBackendStepInput.getValuePrimitiveBoolean(StreamedETLWithFrontendProcess.FIELD_SUPPORTS_FULL_VALIDATION);
      if(!supportsFullValidation)
      {
         LOG.debug("Process does not support validation, so skipping validation step");
         return;
      }

      ////////////////////////////////////////////////////////////////////////////////
      // check if we've been requested to run in this process - if not, return noop //
      ////////////////////////////////////////////////////////////////////////////////
      boolean doFullValidation = runBackendStepInput.getValuePrimitiveBoolean(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION);
      if(!doFullValidation)
      {
         LOG.trace("Not requested to do full validation, so skipping validation step");
         return;
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if we're proceeding with full validation, make sure the review step is after validation in the step list //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      moveReviewStepAfterValidateStep(runBackendStepOutput);

      AbstractExtractStep   extractStep   = getExtractStep(runBackendStepInput);
      AbstractTransformStep transformStep = getTransformStep(runBackendStepInput);

      runBackendStepInput.getAsyncJobCallback().updateStatus("Validating Records");

      //////////////////////////////////////////////////////////
      // basically repeat the preview step, but with no limit //
      //////////////////////////////////////////////////////////
      runBackendStepInput.getAsyncJobCallback().updateStatus("Validating Records");

      //////////////////////////////////////////////////////////////////////
      // let the transform step override the capacity for the record pipe //
      //////////////////////////////////////////////////////////////////////
      RecordPipe recordPipe;
      Integer    overrideRecordPipeCapacity = runBackendStepInput.getValueInteger("recordPipeCapacity");
      if(overrideRecordPipeCapacity != null)
      {
         recordPipe = new RecordPipe(overrideRecordPipeCapacity);
         LOG.debug("per input value [recordPipeCapacity], we are overriding record pipe capacity to: " + overrideRecordPipeCapacity);
      }
      else
      {
         overrideRecordPipeCapacity = transformStep.getOverrideRecordPipeCapacity(runBackendStepInput);
         if(overrideRecordPipeCapacity != null)
         {
            recordPipe = new RecordPipe(overrideRecordPipeCapacity);
            LOG.debug("per " + transformStep.getClass().getName() + ", we are overriding record pipe capacity to: " + overrideRecordPipeCapacity);
         }
         else
         {
            recordPipe = new RecordPipe();
         }
      }

      /////////////////////////////
      // set up the extract step //
      /////////////////////////////
      extractStep.setLimit(null);
      extractStep.setRecordPipe(recordPipe);
      extractStep.preRun(runBackendStepInput, runBackendStepOutput);

      transformStep.preRun(runBackendStepInput, runBackendStepOutput);

      List<QRecord> previewRecordList = new ArrayList<>();
      int recordCount = new AsyncRecordPipeLoop().run("StreamedETLValidate>Extract>" + runBackendStepInput.getProcessName(), null, recordPipe, (status) ->
         {
            extractStep.run(runBackendStepInput, runBackendStepOutput);
            return (runBackendStepOutput);
         },
         () -> (consumeRecordsFromPipe(recordPipe, transformStep, runBackendStepInput, runBackendStepOutput, previewRecordList))
      );

      updateRecordsWithDisplayValuesAndPossibleValues(runBackendStepInput, previewRecordList);
      runBackendStepOutput.setRecords(previewRecordList);
      runBackendStepOutput.addValue(StreamedETLWithFrontendProcess.FIELD_RECORD_COUNT, recordCount);

      //////////////////////////////////////////////////////
      // get the process summary from the validation step //
      //////////////////////////////////////////////////////
      runBackendStepOutput.addValue(StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY, transformStep.doGetProcessSummary(runBackendStepOutput, false));

      BackendStepPostRunOutput postRunOutput = new BackendStepPostRunOutput(runBackendStepOutput);
      BackendStepPostRunInput  postRunInput  = new BackendStepPostRunInput(runBackendStepInput);
      transformStep.postRun(postRunInput, postRunOutput);

      //////////////////////////////////////////////////////////////////////
      // propagate data from inner-step state to process-level step state //
      //////////////////////////////////////////////////////////////////////
      if(postRunOutput.getProcessMetaDataAdjustment() != null)
      {
         runBackendStepOutput.setProcessMetaDataAdjustment(postRunOutput.getProcessMetaDataAdjustment());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int consumeRecordsFromPipe(RecordPipe recordPipe, AbstractTransformStep transformStep, RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput, List<QRecord> previewRecordList) throws QException
   {
      Integer totalRows = runBackendStepInput.getValueInteger(StreamedETLWithFrontendProcess.FIELD_RECORD_COUNT);
      if(totalRows != null)
      {
         runBackendStepInput.getAsyncJobCallback().updateStatus(currentRowCount, totalRows);
      }

      ///////////////////////////////////
      // get the records from the pipe //
      ///////////////////////////////////
      List<QRecord> qRecords = recordPipe.consumeAvailableRecords();

      ///////////////////////////////////////////////////////////////////////
      // make streamed input & output objects from the run input & outputs //
      ///////////////////////////////////////////////////////////////////////
      StreamedBackendStepInput  streamedBackendStepInput  = new StreamedBackendStepInput(runBackendStepInput, qRecords);
      StreamedBackendStepOutput streamedBackendStepOutput = new StreamedBackendStepOutput(runBackendStepOutput);

      /////////////////////////////////////////////////////
      // pass the records through the transform function //
      /////////////////////////////////////////////////////
      transformStep.runOnePage(streamedBackendStepInput, streamedBackendStepOutput);

      //////////////////////////////////////////////////////////////////////
      // propagate data from inner-step state to process-level step state //
      //////////////////////////////////////////////////////////////////////
      if(streamedBackendStepOutput.getProcessMetaDataAdjustment() != null)
      {
         runBackendStepOutput.setProcessMetaDataAdjustment(streamedBackendStepOutput.getProcessMetaDataAdjustment());
      }

      ///////////////////////////////////////////////////////
      // copy a small number of records to the output list //
      ///////////////////////////////////////////////////////
      int i = 0;
      while(previewRecordList.size() < PROCESS_OUTPUT_RECORD_LIST_LIMIT && i < streamedBackendStepOutput.getRecords().size())
      {
         previewRecordList.add(streamedBackendStepOutput.getRecords().get(i++));
      }

      currentRowCount += qRecords.size();
      return (qRecords.size());
   }

}
