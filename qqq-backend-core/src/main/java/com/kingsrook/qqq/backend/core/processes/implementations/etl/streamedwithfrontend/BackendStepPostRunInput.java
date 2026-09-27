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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Subclass of RunBackendStepInput, meant for use in the postRun of the transform/load
 ** steps of a Streamed-ETL-with-frontend processes - where the Record list is not the
 ** full process's record list - rather - is just a preview (e.g., first n).
 ** overrides the getRecords and setRecords method.
 **
 *******************************************************************************/
public class BackendStepPostRunInput extends RunBackendStepInput
{
   private List<QRecord> previewRecordList;



   /*******************************************************************************
    **
    *******************************************************************************/
   public BackendStepPostRunInput(RunBackendStepInput runBackendStepInput)
   {
      super();
      runBackendStepInput.cloneFieldsInto(this);
      previewRecordList = runBackendStepInput.getRecords();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> getRecords()
   {
      throw (new IllegalStateException("Method getRecords should not be called in a post-run - as it is NOT a full record list.  Call getPreviewRecordList to get a subset of the process's output records."));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<QRecord> getPreviewRecordList()
   {
      return (this.previewRecordList);
   }
}
