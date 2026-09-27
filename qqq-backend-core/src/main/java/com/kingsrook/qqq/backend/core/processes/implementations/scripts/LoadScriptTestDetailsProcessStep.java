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


import java.util.ArrayList;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.scripts.TestScriptActionInterface;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptType;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Action to load the details necessary to test a script.
 **
 *******************************************************************************/
public class LoadScriptTestDetailsProcessStep implements BackendStep
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      try
      {
         ActionHelper.validateSession(input);

         Integer  scriptTypeId = input.getValueInteger("scriptTypeId");
         GetInput getInput     = new GetInput();
         getInput.setTableName(ScriptType.TABLE_NAME);
         getInput.setPrimaryKey(scriptTypeId);
         GetOutput  getOutput  = new GetAction().execute(getInput);
         ScriptType scriptType = new ScriptType(getOutput.getRecord());

         TestScriptActionInterface testScriptActionInterface = QCodeLoader.getAdHoc(TestScriptActionInterface.class, new QCodeReference(scriptType.getTestScriptInterfaceName(), QCodeType.JAVA));

         QInstanceEnricher qInstanceEnricher = new QInstanceEnricher(new QInstance());

         ArrayList<QFieldMetaData> inputFields = new ArrayList<>();
         for(QFieldMetaData testInputField : CollectionUtils.nonNullList(testScriptActionInterface.getTestInputFields()))
         {
            qInstanceEnricher.enrichField(testInputField);
            inputFields.add(testInputField);
         }

         ArrayList<QFieldMetaData> outputFields = new ArrayList<>();
         for(QFieldMetaData testOutputField : CollectionUtils.nonNullList(testScriptActionInterface.getTestOutputFields()))
         {
            qInstanceEnricher.enrichField(testOutputField);
            outputFields.add(testOutputField);
         }

         output.addValue("testInputFields", inputFields);
         output.addValue("testOutputFields", outputFields);
      }
      catch(Exception e)
      {
         output.addValue("exception", e);
      }
   }

}
