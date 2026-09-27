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
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ValidateRecordSecurityLockHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevision;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevisionFile;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Action to store a new version (revision) of a script.
 **
 ** The script referenced by the record is always updated to point at the new
 ** scriptRevision record that is inserted.
 **
 *******************************************************************************/
public class StoreScriptRevisionProcessStep implements BackendStep
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      InsertAction insertAction = new InsertAction();
      InsertInput  insertInput  = new InsertInput();
      insertInput.setTableName(ScriptRevision.TABLE_NAME);
      QBackendTransaction transaction = QBackendTransaction.openFor(insertInput);
      insertInput.setTransaction(transaction);

      try
      {
         ActionHelper.validateSession(input);

         //////////////////////////////////////////////////////////////////
         // check if there's currently a script referenced by the record //
         //////////////////////////////////////////////////////////////////
         Integer scriptId       = input.getValueInteger("scriptId");
         Integer nextSequenceNo = 1;

         ////////////////////////////////////////
         // get the existing script, to update //
         ////////////////////////////////////////
         GetInput getInput = new GetInput();
         getInput.setTableName(Script.TABLE_NAME);
         getInput.setPrimaryKey(scriptId);
         getInput.setTransaction(transaction);
         GetOutput getOutput = new GetAction().execute(getInput);
         QRecord   script    = getOutput.getRecord();

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // in case the app added a security field to the scripts table, make sure the user is allowed to edit the script //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         ValidateRecordSecurityLockHelper.validateSecurityFields(QContext.getQInstance().getTable(Script.TABLE_NAME), List.of(script), ValidateRecordSecurityLockHelper.Action.UPDATE, transaction);
         if(CollectionUtils.nullSafeHasContents(script.getErrors()))
         {
            throw (new QPermissionDeniedException(script.getErrors().get(0).getMessage()));
         }

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(ScriptRevision.TABLE_NAME);
         queryInput.setFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("scriptId", QCriteriaOperator.EQUALS, List.of(script.getValue("id"))))
            .withOrderBy(new QFilterOrderBy("sequenceNo", false))
            .withLimit(1));
         queryInput.setTransaction(transaction);
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         if(!queryOutput.getRecords().isEmpty())
         {
            nextSequenceNo = queryOutput.getRecords().get(0).getValueInteger("sequenceNo") + 1;
         }

         //////////////////////////////////
         // insert a new script revision //
         //////////////////////////////////
         String commitMessage = input.getValueString("commitMessage");
         if(!StringUtils.hasContent(commitMessage))
         {
            if(nextSequenceNo == 1)
            {
               commitMessage = "Initial version";
            }
            else
            {
               commitMessage = "No commit message given";
            }
         }

         QRecord scriptRevision = new QRecord()
            .withValue("scriptId", script.getValue("id"))
            .withValue("apiName", input.getValueString("apiName"))
            .withValue("apiVersion", input.getValueString("apiVersion"))
            .withValue("commitMessage", commitMessage)
            .withValue("sequenceNo", nextSequenceNo);

         try
         {
            scriptRevision.setValue("author", QContext.getQSession().getUser().getFullName());
         }
         catch(Exception e)
         {
            scriptRevision.setValue("author", "Unknown");
         }

         insertInput.setRecords(List.of(scriptRevision));
         InsertOutput insertOutput = insertAction.execute(insertInput);
         scriptRevision = insertOutput.getRecords().get(0);
         Integer scriptRevisionId = scriptRevision.getValueInteger("id");

         //////////////////////////////////////////
         // Store the file(s) under the revision //
         //////////////////////////////////////////
         List<QRecord> scriptRevisionFileRecords = null;
         if(StringUtils.hasContent(input.getValueString("fileNames")))
         {
            scriptRevisionFileRecords = new ArrayList<>();
            for(String fileName : input.getValueString("fileNames").split(","))
            {
               scriptRevisionFileRecords.add(new ScriptRevisionFile()
                  .withScriptRevisionId(scriptRevisionId)
                  .withFileName(fileName)
                  .withContents(input.getValueString("fileContents:" + fileName))
                  .toQRecord());
            }
         }
         else if(StringUtils.hasContent(input.getValueString("contents")))
         {
            scriptRevisionFileRecords = new ArrayList<>();
            scriptRevisionFileRecords.add(new ScriptRevisionFile()
               .withScriptRevisionId(scriptRevisionId)
               .withFileName("Script.js")
               .withContents(input.getValueString("contents"))
               .toQRecord());
         }

         if(CollectionUtils.nullSafeHasContents(scriptRevisionFileRecords))
         {
            InsertInput scriptRevisionFileInsertInput = new InsertInput();
            scriptRevisionFileInsertInput.setTableName(ScriptRevisionFile.TABLE_NAME);
            scriptRevisionFileInsertInput.setRecords(scriptRevisionFileRecords);
            scriptRevisionFileInsertInput.setTransaction(transaction);
            new InsertAction().execute(scriptRevisionFileInsertInput);
         }

         ////////////////////////////////////////////////////
         // update the script to point at the new revision //
         ////////////////////////////////////////////////////
         script.setValue("currentScriptRevisionId", scriptRevision.getValue("id"));
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(Script.TABLE_NAME);
         updateInput.setRecords(List.of(script));
         updateInput.setTransaction(transaction);
         new UpdateAction().execute(updateInput);

         transaction.commit();

         output.addValue("scriptId", script.getValueInteger("id"));
         output.addValue("scriptName", script.getValueString("name"));
         output.addValue("scriptRevisionId", scriptRevisionId);
         output.addValue("scriptRevisionSequenceNo", scriptRevision.getValueInteger("sequenceNo"));
      }
      catch(Exception e)
      {
         transaction.rollback();
         throw (e);
      }
      finally
      {
         transaction.close();
      }
   }

}
