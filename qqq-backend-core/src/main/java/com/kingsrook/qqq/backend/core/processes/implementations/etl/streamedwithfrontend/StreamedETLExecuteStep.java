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
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.async.AsyncRecordPipeLoop;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Backend step to do the execute portion of a streamed ETL job.
 **
 ** Works within a transaction (per the backend module of the destination table).
 *******************************************************************************/
public class StreamedETLExecuteStep extends BaseStreamedETLStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(StreamedETLExecuteStep.class);

   private int currentRowCount = 1;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      Optional<QBackendTransaction> transaction = Optional.empty();

      try
      {
         runBackendStepInput.getAsyncJobCallback().updateStatus("Executing Process");

         ///////////////////////////////////////////////////////
         // set up the extract, transform, and load functions //
         ///////////////////////////////////////////////////////
         AbstractExtractStep   extractStep   = getExtractStep(runBackendStepInput);
         AbstractTransformStep transformStep = getTransformStep(runBackendStepInput);
         AbstractLoadStep      loadStep      = getLoadStep(runBackendStepInput);

         loadStep.setTransformStep(transformStep);

         /////////////////////////////////////////////////////////////////////////////
         // let the load step override the capacity for the record pipe.            //
         // this is useful for slower load steps - so that the extract step doesn't //
         // fill the pipe, then timeout waiting for all the records to be consumed, //
         // before it can put more records in.                                      //
         /////////////////////////////////////////////////////////////////////////////
         RecordPipe recordPipe;
         Integer    overrideRecordPipeCapacity = runBackendStepInput.getValueInteger("recordPipeCapacity");
         if(overrideRecordPipeCapacity != null)
         {
            recordPipe = new RecordPipe(overrideRecordPipeCapacity);
            LOG.debug("per input value [recordPipeCapacity], we are overriding record pipe capacity to: " + overrideRecordPipeCapacity);
         }
         else
         {
            overrideRecordPipeCapacity = loadStep.getOverrideRecordPipeCapacity(runBackendStepInput);
            if(overrideRecordPipeCapacity != null)
            {
               recordPipe = new RecordPipe(overrideRecordPipeCapacity);
               LOG.debug("per " + loadStep.getClass().getName() + ", we are overriding record pipe capacity to: " + overrideRecordPipeCapacity);
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
         }

         extractStep.setRecordPipe(recordPipe);
         extractStep.preRun(runBackendStepInput, runBackendStepOutput);

         transformStep.preRun(runBackendStepInput, runBackendStepOutput);
         loadStep.preRun(runBackendStepInput, runBackendStepOutput);

         /////////////////////////////////////////////////////////////////////////////
         // open a transaction for the whole process, if that's the requested level //
         /////////////////////////////////////////////////////////////////////////////
         boolean doProcessLevelTransaction = StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_PROCESS.equals(runBackendStepInput.getValueString(StreamedETLWithFrontendProcess.FIELD_TRANSACTION_LEVEL));
         if(doProcessLevelTransaction)
         {
            transaction = loadStep.openTransaction(runBackendStepInput);
            loadStep.setTransaction(transaction);
            transformStep.setTransaction(transaction);
         }

         List<QRecord>       loadedRecordList    = new ArrayList<>();
         AsyncRecordPipeLoop asyncRecordPipeLoop = new AsyncRecordPipeLoop();
         if(overrideRecordPipeCapacity != null && overrideRecordPipeCapacity < asyncRecordPipeLoop.getMinRecordsToConsume())
         {
            asyncRecordPipeLoop.setMinRecordsToConsume(overrideRecordPipeCapacity);
         }

         int recordCount = asyncRecordPipeLoop.run("StreamedETLExecute>Extract>" + runBackendStepInput.getProcessName(), null, recordPipe, (status) ->
            {
               extractStep.run(runBackendStepInput, runBackendStepOutput);
               return (runBackendStepOutput);
            },
            () -> (consumeRecordsFromPipe(recordPipe, transformStep, loadStep, runBackendStepInput, runBackendStepOutput, loadedRecordList))
         );

         runBackendStepOutput.addValue(StreamedETLWithFrontendProcess.FIELD_RECORD_COUNT, recordCount);

         updateRecordsWithDisplayValuesAndPossibleValues(runBackendStepInput, loadedRecordList);
         runBackendStepOutput.setRecords(loadedRecordList);

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // get the process summary from the load step, if it's a summary-provider -- else, use the transform step (which is always a provider) //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         ArrayList<ProcessSummaryLineInterface> processSummaryLines = null;
         if(loadStep instanceof ProcessSummaryProviderInterface provider)
         {
            processSummaryLines = provider.doGetProcessSummary(runBackendStepOutput, true);
            runBackendStepOutput.addValue(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY, processSummaryLines);
         }

         if(CollectionUtils.nullSafeIsEmpty(processSummaryLines))
         {
            processSummaryLines = transformStep.doGetProcessSummary(runBackendStepOutput, true);
            runBackendStepOutput.addValue(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY, processSummaryLines);
         }

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // use a subclass of runBackendStepOutput that makes it clear you can't use the recordList, as it's a "preview/subset" record list //
         // this prevents bugs where you might think you have the full record list, but really don't                                        //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         BackendStepPostRunOutput postRunOutput = new BackendStepPostRunOutput(runBackendStepOutput);
         BackendStepPostRunInput  postRunInput  = new BackendStepPostRunInput(runBackendStepInput);
         transformStep.postRun(postRunInput, postRunOutput);
         loadStep.postRun(postRunInput, postRunOutput);

         //////////////////////////////////////////////////////////////////////
         // propagate data from inner-step state to process-level step state //
         //////////////////////////////////////////////////////////////////////
         if(postRunOutput.getProcessMetaDataAdjustment() != null)
         {
            runBackendStepOutput.setProcessMetaDataAdjustment(postRunOutput.getProcessMetaDataAdjustment());
         }

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // explicitly copy values back into the runStepOutput from the post-run output                                     //
         // this might not be needed, since they (presumably) share a processState object, but just in case that changes... //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         runBackendStepOutput.setValues(postRunOutput.getValues());

         if(recordCount > 0)
         {
            LOG.debug("Processed [" + recordCount + "] records.");
         }

         //////////////////////////////////////////////////////////////////////////////
         // set the flag to state that the basepull timestamp should be updated now. //
         // (upstream will check if the process was running as a basepull)           //
         //////////////////////////////////////////////////////////////////////////////
         runBackendStepOutput.addValue(RunProcessAction.BASEPULL_READY_TO_UPDATE_TIMESTAMP_FIELD, true);

         ////////////////////////////////////////////////////////
         // commit the work at the process level if applicable //
         ////////////////////////////////////////////////////////
         if(doProcessLevelTransaction && transaction.isPresent())
         {
            transaction.get().commit();
         }
      }
      catch(Exception e)
      {
         ////////////////////////////////////////////////////////////////////////////////
         // rollback the work, then re-throw the error for up-stream to catch & report //
         ////////////////////////////////////////////////////////////////////////////////
         if(transaction.isPresent())
         {
            LOG.warn("Caught top-level process exception - rolling back transaction", e);
            transaction.get().rollback();
         }
         else
         {
            LOG.warn("Caught top-level process exception - would roll back transaction, but none is present", e);
         }
         throw (e);
      }
      finally
      {
         ////////////////////////////////////////////////////////////
         // always close our transactions (e.g., jdbc connections) //
         ////////////////////////////////////////////////////////////
         if(transaction.isPresent())
         {
            transaction.get().close();
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int consumeRecordsFromPipe(RecordPipe recordPipe, AbstractTransformStep transformStep, AbstractLoadStep loadStep, RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput, List<QRecord> loadedRecordList) throws QException
   {
      /////////////////////////////////////////////////////////////////////////////
      // open a transaction for the whole process, if that's the requested level //
      /////////////////////////////////////////////////////////////////////////////
      Optional<QBackendTransaction> transaction            = Optional.empty();
      boolean                       doPageLevelTransaction = StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_PAGE.equals(runBackendStepInput.getValueString(StreamedETLWithFrontendProcess.FIELD_TRANSACTION_LEVEL));
      if(doPageLevelTransaction)
      {
         transaction = loadStep.openTransaction(runBackendStepInput);
         loadStep.setTransaction(transaction);
         transformStep.setTransaction(transaction);
      }

      try
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
         List<AuditInput> auditInputListFromTransform = streamedBackendStepOutput.getAuditInputList();

         //////////////////////////////////////////////////////////////////////
         // propagate data from inner-step state to process-level step state //
         //////////////////////////////////////////////////////////////////////
         if(streamedBackendStepOutput.getProcessMetaDataAdjustment() != null)
         {
            runBackendStepOutput.getProcessState().setStepList(streamedBackendStepOutput.getProcessState().getStepList());
            runBackendStepOutput.getProcessState().setProcessMetaDataAdjustment(streamedBackendStepOutput.getProcessMetaDataAdjustment());
         }

         ////////////////////////////////////////////////
         // pass the records through the load function //
         ////////////////////////////////////////////////
         streamedBackendStepInput = new StreamedBackendStepInput(runBackendStepInput, streamedBackendStepOutput.getRecords());
         streamedBackendStepOutput = new StreamedBackendStepOutput(runBackendStepOutput);

         loadStep.runOnePage(streamedBackendStepInput, streamedBackendStepOutput);
         List<AuditInput> auditInputListFromLoad = streamedBackendStepOutput.getAuditInputList();

         //////////////////////////////////////////////////////////////////////
         // propagate data from inner-step state to process-level step state //
         //////////////////////////////////////////////////////////////////////
         if(streamedBackendStepOutput.getProcessMetaDataAdjustment() != null)
         {
            runBackendStepOutput.getProcessState().setStepList(streamedBackendStepOutput.getProcessState().getStepList());
            runBackendStepOutput.getProcessState().setProcessMetaDataAdjustment(streamedBackendStepOutput.getProcessMetaDataAdjustment());
         }

         ///////////////////////////////////////////////////////
         // copy a small number of records to the output list //
         ///////////////////////////////////////////////////////
         int i = 0;
         while(loadedRecordList.size() < PROCESS_OUTPUT_RECORD_LIST_LIMIT && i < streamedBackendStepOutput.getRecords().size())
         {
            loadedRecordList.add(streamedBackendStepOutput.getRecords().get(i++));
         }

         //////////////////////////////////////////////////////
         // if we have a batch of audit inputs, execute them //
         //////////////////////////////////////////////////////
         List<AuditInput> mergedAuditInputList = CollectionUtils.mergeLists(auditInputListFromTransform, auditInputListFromLoad);
         if(CollectionUtils.nullSafeHasContents(mergedAuditInputList))
         {
            AuditAction auditAction = new AuditAction();
            for(AuditInput auditInput : mergedAuditInputList)
            {
               auditAction.execute(auditInput);
            }
         }
         runBackendStepOutput.setAuditInputList(null);

         if(doPageLevelTransaction && transaction.isPresent())
         {
            transaction.get().commit();
         }

         currentRowCount += qRecords.size();
         return (qRecords.size());
      }
      catch(Exception e)
      {
         if(doPageLevelTransaction && transaction.isPresent())
         {
            LOG.warn("Caught page-level process exception - rolling back transaction", e);
            transaction.get().rollback();
         }
         throw (e);
      }
      finally
      {
         if(doPageLevelTransaction && transaction.isPresent())
         {
            transaction.get().close();
         }
      }
   }

}
