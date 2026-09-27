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
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Subclass of RunBackendStepOutput, meant for use in the pseudo-steps used by
 ** the Streamed-ETL-with-frontend processes - where the Record list is not the
 ** full process's record list - rather - is just a page at a time -- so this class
 ** overrides the getRecords and setRecords method, to just work with that page.
 *******************************************************************************/
public class StreamedBackendStepOutput extends RunBackendStepOutput
{
   private List<QRecord> outputRecords = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public StreamedBackendStepOutput(RunBackendStepOutput runBackendStepOutput)
   {
      super();
      setValues(runBackendStepOutput.getValues());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void setRecords(List<QRecord> records)
   {
      this.outputRecords = records;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> getRecords()
   {
      return (outputRecords);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addRecord(QRecord record)
   {
      if(this.outputRecords == null)
      {
         this.outputRecords = new ArrayList<>();
      }
      this.outputRecords.add(record);
   }
}
