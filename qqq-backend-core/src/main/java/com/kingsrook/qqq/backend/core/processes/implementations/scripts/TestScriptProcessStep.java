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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.scripts.TestScriptActionInterface;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.TestScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.TestScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.AdHocScriptCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevision;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevisionFile;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptType;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Action to test a script!
 **
 *******************************************************************************/
public class TestScriptProcessStep implements BackendStep
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

         ///////////////////////////////////////////////////////////////////////
         // build a script revision based on the input params & file contents //
         ///////////////////////////////////////////////////////////////////////
         Integer scriptId = input.getValueInteger("scriptId");

         ScriptRevision scriptRevision = new ScriptRevision();
         scriptRevision.setScriptId(scriptId);

         ArrayList<ScriptRevisionFile> files = new ArrayList<>();
         if(StringUtils.hasContent(input.getValueString("fileNames")))
         {
            for(String fileName : input.getValueString("fileNames").split(","))
            {
               files.add(new ScriptRevisionFile()
                  .withFileName(fileName)
                  .withContents(input.getValueString("fileContents:" + fileName)));
            }
         }

         scriptRevision.setFiles(files);
         scriptRevision.setApiName(input.getValueString("apiName"));
         scriptRevision.setApiVersion(input.getValueString("apiVersion"));

         ///////////////////////////////////////////////////////
         // set up a code reference using the script revision //
         ///////////////////////////////////////////////////////
         AdHocScriptCodeReference adHocScriptCodeReference = new AdHocScriptCodeReference().withScriptRevisionRecord(scriptRevision.toQRecord());
         adHocScriptCodeReference.setCodeType(QCodeType.JAVA_SCRIPT); // todo - load dynamically?
         adHocScriptCodeReference.setInlineCode(scriptRevision.getFiles().get(0).getContents()); // todo - ugh.

         /////////////////////////////////////////////////////////////////////////////////////////////////////////
         // load the script and its type, to find the TestScriptActionInterface where the script will be tested //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////
         QRecord  script       = getScript(scriptId);
         Integer  scriptTypeId = script.getValueInteger("scriptTypeId");
         GetInput getInput     = new GetInput();
         getInput.setTableName(ScriptType.TABLE_NAME);
         getInput.setPrimaryKey(scriptTypeId);
         GetOutput  getOutput  = new GetAction().execute(getInput);
         ScriptType scriptType = new ScriptType(getOutput.getRecord());

         TestScriptActionInterface testScriptActionInterface = QCodeLoader.getAdHoc(TestScriptActionInterface.class, new QCodeReference(scriptType.getTestScriptInterfaceName(), QCodeType.JAVA));

         /////////////////////////////////////////////////////////////////////////////////////////////////
         // finish setting up input for the testScript action - including coyping over all input values //
         /////////////////////////////////////////////////////////////////////////////////////////////////
         TestScriptInput testScriptInput = new TestScriptInput();
         testScriptInput.setApiName(input.getValueString("apiName"));
         testScriptInput.setApiVersion(input.getValueString("apiVersion"));
         testScriptInput.setCodeReference(adHocScriptCodeReference);

         Map<String, Serializable> inputValues = new HashMap<>();
         testScriptInput.setInputValues(inputValues);

         for(Map.Entry<String, Serializable> entry : input.getValues().entrySet())
         {
            String key   = entry.getKey();
            String value = ValueUtils.getValueAsString(entry.getValue());
            inputValues.put(key, value);
         }

         ////////////////////////////////
         // run the test script action //
         ////////////////////////////////
         TestScriptOutput testScriptOutput = new TestScriptOutput();
         testScriptActionInterface.execute(testScriptInput, testScriptOutput);

         //////////////////////////////////
         // send script outputs back out //
         //////////////////////////////////
         output.addValue("scriptLogLines", CollectionUtils.useOrWrap(testScriptOutput.getScriptLogLines(), new TypeToken<ArrayList<QRecord>>() {}));
         output.addValue("outputObject", testScriptOutput.getOutputObject());

         if(testScriptOutput.getException() != null)
         {
            output.addValue("exception", testScriptOutput.getException());
            output.setException(testScriptOutput.getException());
         }
      }
      catch(Exception e)
      {
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // is this the kind of exception meant here?  or is it more for one thrown by the script execution?  or are those the same?? //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         output.addValue("exception", e);
         output.setException(e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord getScript(Integer scriptId) throws QException
   {
      GetInput getScriptInput = new GetInput();
      getScriptInput.setTableName(Script.TABLE_NAME);
      getScriptInput.setPrimaryKey(scriptId);
      GetOutput getScriptOutput = new GetAction().execute(getScriptInput);
      if(getScriptOutput.getRecord() == null)
      {
         throw (new QException("Script was not found by id " + scriptId));
      }

      return (getScriptOutput.getRecord());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getScriptTypeName(QRecord script) throws QException
   {
      GetInput getScriptTypeInput = new GetInput();
      getScriptTypeInput.setTableName(ScriptType.TABLE_NAME);
      getScriptTypeInput.setPrimaryKey(script.getValueInteger("scriptTypeId"));
      GetOutput getScriptTypeOutput = new GetAction().execute(getScriptTypeInput);
      if(getScriptTypeOutput.getRecord() == null)
      {
         throw (new QException("Script Type was not found for script " + script.getValue("id")));
      }

      return (getScriptTypeOutput.getRecord().getValueString("name"));
   }

}
