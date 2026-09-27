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

package com.kingsrook.qqq.backend.core.actions.scripts;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeOutput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAssociatedScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAssociatedScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.metadata.code.AssociatedScriptCodeReference;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevision;


/*******************************************************************************
 **
 *******************************************************************************/
public class RunAssociatedScriptAction
{
   private Map<AssociatedScriptCodeReference, ScriptRevision> scriptRevisionCache = new HashMap<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public void run(RunAssociatedScriptInput input, RunAssociatedScriptOutput output) throws QException
   {
      ActionHelper.validateSession(input);

      ScriptRevision   scriptRevision   = getScriptRevision(input);
      ExecuteCodeInput executeCodeInput = ExecuteCodeAction.setupExecuteCodeInput(input, scriptRevision);

      if(input.getAssociatedScriptContextPrimerInterface() != null)
      {
         input.getAssociatedScriptContextPrimerInterface().primeContext(executeCodeInput, scriptRevision);
      }

      ExecuteCodeOutput executeCodeOutput = new ExecuteCodeOutput();
      new ExecuteCodeAction().run(executeCodeInput, executeCodeOutput);

      output.setOutput(executeCodeOutput.getOutput());
      output.setScriptRevisionId(scriptRevision.getId());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ScriptRevision getScriptRevision(RunAssociatedScriptInput input) throws QException
   {
      if(!scriptRevisionCache.containsKey(input.getCodeReference()))
      {
         Serializable scriptId = getScriptId(input);
         if(scriptId == null)
         {
            throw (new QNotFoundException("The input record [" + input.getCodeReference().getRecordTable() + "][" + input.getCodeReference().getRecordPrimaryKey()
               + "] does not have a script specified for [" + input.getCodeReference().getFieldName() + "]"));
         }

         Script script = getScript(input, scriptId);
         if(script.getCurrentScriptRevisionId() == null)
         {
            throw (new QNotFoundException("The script for record [" + input.getCodeReference().getRecordTable() + "][" + input.getCodeReference().getRecordPrimaryKey()
               + "] (scriptId=" + scriptId + ") does not have a current version."));
         }

         ScriptRevision scriptRevision = getCurrentScriptRevision(input, script.getCurrentScriptRevisionId());
         scriptRevisionCache.put(input.getCodeReference(), scriptRevision);
      }

      return scriptRevisionCache.get(input.getCodeReference());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ScriptRevision getCurrentScriptRevision(RunAssociatedScriptInput input, Serializable scriptRevisionId) throws QException
   {
      GetInput getInput = new GetInput();
      getInput.setTableName("scriptRevision");
      getInput.setPrimaryKey(scriptRevisionId);
      getInput.setIncludeAssociations(true);
      GetOutput getOutput = new GetAction().execute(getInput);
      if(getOutput.getRecord() == null)
      {
         throw (new QNotFoundException("The current revision of the script for record [" + input.getCodeReference().getRecordTable() + "][" + input.getCodeReference().getRecordPrimaryKey() + "]["
            + input.getCodeReference().getFieldName() + "] (scriptRevisionId=" + scriptRevisionId + ") was not found."));
      }

      return (new ScriptRevision(getOutput.getRecord()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Script getScript(RunAssociatedScriptInput input, Serializable scriptId) throws QException
   {
      GetInput getInput = new GetInput();
      getInput.setTableName("script");
      getInput.setPrimaryKey(scriptId);
      GetOutput getOutput = new GetAction().execute(getInput);

      if(getOutput.getRecord() == null)
      {
         throw (new QNotFoundException("The script for record [" + input.getCodeReference().getRecordTable() + "][" + input.getCodeReference().getRecordPrimaryKey() + "]["
            + input.getCodeReference().getFieldName() + "] (script id=" + scriptId + ") was not found."));
      }

      return (new Script(getOutput.getRecord()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable getScriptId(RunAssociatedScriptInput input) throws QException
   {
      GetInput getInput = new GetInput();
      getInput.setTableName(input.getCodeReference().getRecordTable());
      getInput.setPrimaryKey(input.getCodeReference().getRecordPrimaryKey());
      GetOutput getOutput = new GetAction().execute(getInput);
      if(getOutput.getRecord() == null)
      {
         throw (new QNotFoundException("The requested record [" + input.getCodeReference().getRecordTable() + "][" + input.getCodeReference().getRecordPrimaryKey() + "] was not found."));
      }

      return (getOutput.getRecord().getValue(input.getCodeReference().getFieldName()));
   }

}
