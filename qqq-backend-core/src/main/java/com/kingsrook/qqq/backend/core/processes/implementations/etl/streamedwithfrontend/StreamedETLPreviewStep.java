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
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamed.StreamedETLProcess;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Backend step to do a preview of a full streamed ETL job
 *******************************************************************************/
public class StreamedETLPreviewStep extends BaseStreamedETLStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(StreamedETLPreviewStep.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   @SuppressWarnings("checkstyle:indentation")
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      Integer limit = PROCESS_OUTPUT_RECORD_LIST_LIMIT; // todo - use a field instead of hard-coded here?
      runBackendStepInput.getAsyncJobCallback().updateStatus("Generating Preview");

      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if the do-full-validation flag has already been set, then do the validation step instead of this one //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      boolean supportsFullValidation = runBackendStepInput.getValuePrimitiveBoolean(StreamedETLWithFrontendProcess.FIELD_SUPPORTS_FULL_VALIDATION);
      boolean doFullValidation       = runBackendStepInput.getValuePrimitiveBoolean(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION);
      if(supportsFullValidation && doFullValidation)
      {
         runBackendStepOutput.addValue(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
         moveReviewStepAfterValidateStep(runBackendStepOutput);
         return;
      }

      /////////////////////////////////////////////////////////////////
      // if we're running inside an automation, then skip this step. //
      /////////////////////////////////////////////////////////////////
      if(runningWithinAutomation())
      {
         LOG.debug("Skipping preview step when [" + runBackendStepInput.getProcessName() + "] is running as part of an automation.");
         return;
      }

      /////////////////////////////
      // set up the extract step //
      /////////////////////////////
      AbstractExtractStep extractStep = getExtractStep(runBackendStepInput);
      extractStep.setLimit(limit);
      extractStep.preRun(runBackendStepInput, runBackendStepOutput);

      //////////////////////////////////////////
      // set up a record pipe for the process //
      //////////////////////////////////////////
      RecordPipe recordPipe = extractStep.createRecordPipe(runBackendStepInput, null);
      extractStep.setRecordPipe(recordPipe);

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if skipping frontend steps, skip this action -                                                                 //
      // but, if inside an (ideally, only async) API call, at least do the count, so status calls can get x of y status //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(RunProcessInput.FrontendStepBehavior.SKIP.equals(runBackendStepInput.getFrontendStepBehavior()))
      {
         if(QContext.getQSession().getValue("apiVersion") != null)
         {
            countRecords(runBackendStepInput, runBackendStepOutput, extractStep);
         }

         LOG.debug("Skipping preview because frontend behavior is [" + RunProcessInput.FrontendStepBehavior.SKIP + "].");
         return;
      }

      countRecords(runBackendStepInput, runBackendStepOutput, extractStep);

      //////////////////////////////
      // setup the transform step //
      //////////////////////////////
      AbstractTransformStep transformStep = getTransformStep(runBackendStepInput);
      transformStep.preRun(runBackendStepInput, runBackendStepOutput);

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if the count is less than the normal limit here, and this process supports validation, then go straight to the validation step //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // todo - maybe some future version we do this - maybe based on a user-preference
      // if(supportsFullValidation && recordCount <= limit)
      // {
      //    runBackendStepOutput.addValue(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      //    moveReviewStepAfterValidateStep(runBackendStepOutput);
      //    return;
      // }

      List<QRecord> previewRecordList = new ArrayList<>();
      new AsyncRecordPipeLoop().run("StreamedETLPreview>Extract>" + runBackendStepInput.getProcessName(), PROCESS_OUTPUT_RECORD_LIST_LIMIT, recordPipe, (status) ->
         {
            runBackendStepInput.setAsyncJobCallback(status);
            extractStep.run(runBackendStepInput, runBackendStepOutput);
            return (runBackendStepOutput);
         },
         () -> (consumeRecordsFromPipe(recordPipe, transformStep, runBackendStepInput, runBackendStepOutput, previewRecordList))
      );

      updateRecordsWithDisplayValuesAndPossibleValues(runBackendStepInput, previewRecordList);
      runBackendStepOutput.setRecords(previewRecordList);

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
   private void countRecords(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput, AbstractExtractStep extractStep) throws QException
   {
      String         sourceTableName = runBackendStepInput.getValueString(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE);
      QTableMetaData sourceTable     = QContext.getQInstance().getTable(sourceTableName);
      if(StringUtils.hasContent(sourceTableName))
      {
         QBackendMetaData sourceTableBackend = QContext.getQInstance().getBackendForTable(sourceTableName);
         if(sourceTable.isCapabilityEnabled(sourceTableBackend, Capability.TABLE_COUNT))
         {
            Integer recordCount = extractStep.doCount(runBackendStepInput);
            runBackendStepOutput.addValue(StreamedETLProcess.FIELD_RECORD_COUNT, recordCount);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean runningWithinAutomation()
   {
      Exception e = new Exception();
      for(StackTraceElement stackTraceElement : e.getStackTrace())
      {
         String className = stackTraceElement.getClassName();
         if(className.contains("com.kingsrook.qqq.backend.core.actions.automation"))
         {
            return (true);
         }
      }
      return false;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int consumeRecordsFromPipe(RecordPipe recordPipe, AbstractTransformStep transformStep, RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput, List<QRecord> transformedRecordList) throws QException
   {
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
         runBackendStepOutput.getProcessState().setProcessMetaDataAdjustment(streamedBackendStepOutput.getProcessMetaDataAdjustment());
      }

      ////////////////////////////////////////////////////
      // add the transformed records to the output list //
      ////////////////////////////////////////////////////
      transformedRecordList.addAll(streamedBackendStepOutput.getRecords());

      return (qRecords.size());
   }

}
