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

package com.kingsrook.qqq.backend.core.processes.implementations.general;


import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QRecordListMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Function body to take care of loading the initial records to be used by a
 ** process.
 **
 *******************************************************************************/
public class LoadInitialRecordsStep implements BackendStep
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // basically this is a no-op... sometimes we just need a backendStep to be the first step in a process. //
      // While we're here, go ahead and put the query filter in the payload as a value - this is needed for   //
      // processes that have a screen before their first backend step (why is this needed?  not sure, but is) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      runBackendStepInput.getAsyncJobCallback().updateStatus("Loading records");
      if(runBackendStepInput.getCallback() != null)
      {
         QQueryFilter queryFilter = runBackendStepInput.getCallback().getQueryFilter();
         runBackendStepOutput.addValue("queryFilterJson", JsonUtils.toJson(queryFilter));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QBackendStepMetaData defineMetaData(String tableName)
   {
      return (new QBackendStepMetaData()
         .withName("loadInitialRecords")
         .withCode(new QCodeReference()
            .withName(LoadInitialRecordsStep.class.getName())
            .withCodeType(QCodeType.JAVA))
         .withInputData(new QFunctionInputMetaData()
            .withRecordListMetaData(new QRecordListMetaData()
               .withTableName(tableName))));

   }

}
