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

package com.kingsrook.qqq.backend.core.processes.implementations.scripts;


import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Extract step for the run-record process.  Exists only to deal with this being
 ** a generic process (e.g., no table name defined in the meta data).
 *******************************************************************************/
public class RunRecordScriptExtractStep extends ExtractViaQueryStep
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void preRun(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // this is a generic (e.g., not table-specific) process - so we must be sure to set the tableName field in the expected slot. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      String tableName = runBackendStepInput.getValueString("tableName");
      if(!StringUtils.hasContent(tableName))
      {
         throw (new QException("Table name was not specified as input value"));
      }

      runBackendStepInput.addValue(FIELD_SOURCE_TABLE, tableName);

      Integer  scriptId = runBackendStepInput.getValueInteger("scriptId");
      GetInput getInput = new GetInput();
      getInput.setTableName(Script.TABLE_NAME);
      getInput.setPrimaryKey(scriptId);
      GetOutput getOutput = new GetAction().execute(getInput);
      if(getOutput.getRecord() != null)
      {
         runBackendStepOutput.addValue("scriptName", getOutput.getRecord().getValueString("name"));
      }

      super.preRun(runBackendStepInput, runBackendStepOutput);
   }



   /*******************************************************************************
    ** Make sure associations are fetched (so api records have children!)
    *******************************************************************************/
   @Override
   protected void customizeInputPreQuery(RunBackendStepInput runBackendStepInput, QueryInput queryInput)
   {
      super.customizeInputPreQuery(runBackendStepInput, queryInput);
      queryInput.setIncludeAssociations(true);
   }

}
