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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.streamed;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.async.AsyncRecordPipeLoop;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.basic.BasicETLExtractFunction;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.basic.BasicETLLoadFunction;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.basic.BasicETLProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.basic.BasicETLTransformFunction;


/*******************************************************************************
 ** Backend step to do a streamed ETL
 *******************************************************************************/
public class StreamedETLBackendStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(StreamedETLBackendStep.class);

   private static final int TIMEOUT_AFTER_NO_RECORDS_MS = 10 * 60 * 1000;

   private static final int MAX_SLEEP_MS  = 1000;
   private static final int INIT_SLEEP_MS = 10;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   @SuppressWarnings("checkstyle:indentation")
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      QBackendTransaction transaction = openTransaction(runBackendStepInput);

      try
      {
         RecordPipe              recordPipe              = new RecordPipe();
         BasicETLExtractFunction basicETLExtractFunction = new BasicETLExtractFunction();
         basicETLExtractFunction.setRecordPipe(recordPipe);

         ////////////////////////////////////
         // run the async-record-pipe loop //
         ////////////////////////////////////
         int recordCount = new AsyncRecordPipeLoop().run("StreamedETL>Extract", null, recordPipe, (status) ->
            {
               basicETLExtractFunction.run(runBackendStepInput, runBackendStepOutput);
               return (runBackendStepOutput);
            },
            () -> (consumeRecordsFromPipe(recordPipe, runBackendStepInput, runBackendStepOutput, transaction))
         );

         runBackendStepOutput.addValue(StreamedETLProcess.FIELD_RECORD_COUNT, recordCount);

         /////////////////////
         // commit the work //
         /////////////////////
         transaction.commit();
      }
      catch(Exception e)
      {
         ////////////////////////////////////////////////////////////////////////////////
         // rollback the work, then re-throw the error for up-stream to catch & report //
         ////////////////////////////////////////////////////////////////////////////////
         LOG.warn("Caught top-level process exception - rolling back transaction", e);
         transaction.rollback();
         throw (e);
      }
      finally
      {
         ////////////////////////////////////////////////////////////
         // always close our transactions (e.g., jdbc connections) //
         ////////////////////////////////////////////////////////////
         transaction.close();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QBackendTransaction openTransaction(RunBackendStepInput runBackendStepInput) throws QException
   {
      InsertInput insertInput = new InsertInput();

      insertInput.setTableName(runBackendStepInput.getValueString(BasicETLProcess.FIELD_DESTINATION_TABLE));

      return QBackendTransaction.openFor(insertInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int consumeRecordsFromPipe(RecordPipe recordPipe, RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput, QBackendTransaction transaction) throws QException
   {
      List<QRecord> qRecords = recordPipe.consumeAvailableRecords();

      preTransform(qRecords, runBackendStepInput, runBackendStepOutput);

      runBackendStepInput.setRecords(qRecords);
      new BasicETLTransformFunction().run(runBackendStepInput, runBackendStepOutput);

      postTransform(qRecords, runBackendStepInput, runBackendStepOutput);

      runBackendStepInput.setRecords(runBackendStepOutput.getRecords());
      BasicETLLoadFunction basicETLLoadFunction = new BasicETLLoadFunction();
      basicETLLoadFunction.setReturnStoredRecords(false);
      basicETLLoadFunction.setTransaction(transaction);
      basicETLLoadFunction.run(runBackendStepInput, runBackendStepOutput);

      return (qRecords.size());
   }



   /*******************************************************************************
    ** Customization point for subclasses of this step.
    *******************************************************************************/
   protected void preTransform(List<QRecord> qRecords, RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }



   /*******************************************************************************
    ** Customization point for subclasses of this step.
    *******************************************************************************/
   protected void postTransform(List<QRecord> qRecords, RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }

}
