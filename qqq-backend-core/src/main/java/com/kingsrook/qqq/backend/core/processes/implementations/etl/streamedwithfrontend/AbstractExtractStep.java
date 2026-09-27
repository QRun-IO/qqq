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


import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;


/*******************************************************************************
 ** Base class for the Extract logic of Streamed ETL processes.
 **
 ** These steps are invoked by both the "preview" and the "execute" steps of a
 ** StreamedETLWithFrontend process.
 **
 ** Key here, is that subclasses here should put records that they're "Extracting"
 ** into the recordPipe member.  That is to say, DO NOT use the recordList in
 ** the Step input/output objects.
 **
 ** Ideally, they'll also stop once they've hit the "limit" number of records
 ** (though if you keep going, the pipe will get terminated and the job will be
 ** cancelled, etc...).
 *******************************************************************************/
public abstract class AbstractExtractStep implements BackendStep
{
   private RecordPipe recordPipe;
   private Integer    limit;



   /*******************************************************************************
    ** Allow subclasses to do an action before the run begins.
    *******************************************************************************/
   public void preRun(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Integer doCount(RunBackendStepInput runBackendStepInput) throws QException
   {
      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setRecordPipe(RecordPipe recordPipe)
   {
      this.recordPipe = recordPipe;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordPipe getRecordPipe()
   {
      return recordPipe;
   }



   /*******************************************************************************
    ** Getter for limit
    **
    *******************************************************************************/
   public Integer getLimit()
   {
      return limit;
   }



   /*******************************************************************************
    ** Setter for limit
    **
    *******************************************************************************/
   public void setLimit(Integer limit)
   {
      this.limit = limit;
   }



   /*******************************************************************************
    ** Create the record pipe to be used for this process step.
    **
    ** Here in case a subclass needs a different type of pipe - for example, a
    ** DistinctFilteringRecordPipe.
    *******************************************************************************/
   public RecordPipe createRecordPipe(RunBackendStepInput runBackendStepInput, Integer overrideCapacity)
   {
      return (new RecordPipe(overrideCapacity));
   }

}
