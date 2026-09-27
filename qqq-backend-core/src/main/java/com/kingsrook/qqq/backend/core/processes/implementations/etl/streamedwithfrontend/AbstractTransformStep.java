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


import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;


/*******************************************************************************
 ** Base class for the Transform logic of Streamed ETL processes.
 **
 ** Records are to be read out of the input object's Records field, and after storing,
 ** should be written to the output object's Records, noting that when running
 ** as a streamed-ETL process, those input & output objects will be instances of
 ** the StreamedBackendStep{Input,Output} classes, that will be associated with
 ** a page of records flowing through a pipe.
 **
 *******************************************************************************/
public abstract class AbstractTransformStep implements ProcessSummaryProviderInterface
{
   private Optional<QBackendTransaction> transaction = Optional.empty();



   /*******************************************************************************
    ** Do the transform logic for one page of records
    *******************************************************************************/
   public abstract void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException;



   /*******************************************************************************
    ** Allow subclasses to do an action before any pages of records are processed.
    *******************************************************************************/
   public void preRun(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }



   /*******************************************************************************
    ** Allow subclasses to do an action after the run is complete - after the last
    ** page of records is passed in.
    *******************************************************************************/
   public void postRun(BackendStepPostRunInput runBackendStepInput, BackendStepPostRunOutput runBackendStepOutput) throws QException
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }



   /*******************************************************************************
    ** Setter for transaction
    **
    *******************************************************************************/
   public void setTransaction(Optional<QBackendTransaction> transaction)
   {
      this.transaction = transaction;
   }



   /*******************************************************************************
    ** Getter for transaction
    **
    *******************************************************************************/
   public Optional<QBackendTransaction> getTransaction()
   {
      return (transaction);
   }



   /*******************************************************************************
    ** Allow this transform step to specify the capacity of the pipe being used by the process.
    **
    ** The specific use-case for which this is being added is, in the case of a process
    ** with many records being extracted, if the transform job is too slow, then the pipe
    ** can get filled, and the extractor (who puts records into the pipe) can time out
    ** waiting for capacity in the pipe to open up, while a slow loader is consuming
    ** the records.
    **
    ** In other words, for a slow transformer, setting a lower pipe capacity can help prevent
    ** time-out errors ("Giving up adding record to pipe...")
    *******************************************************************************/
   public Integer getOverrideRecordPipeCapacity(RunBackendStepInput runBackendStepInput)
   {
      return (null);
   }

}
