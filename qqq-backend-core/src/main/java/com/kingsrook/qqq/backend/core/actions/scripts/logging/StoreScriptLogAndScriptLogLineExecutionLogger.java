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

package com.kingsrook.qqq.backend.core.actions.scripts.logging;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Implementation of a code execution logger that logs into scriptLog and scriptLogLine
 ** tables - e.g., as defined in ScriptMetaDataProvider.
 *******************************************************************************/
public class StoreScriptLogAndScriptLogLineExecutionLogger extends BuildScriptLogAndScriptLogLineExecutionLogger
{
   private static final QLogger LOG = QLogger.getLogger(StoreScriptLogAndScriptLogLineExecutionLogger.class);



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public StoreScriptLogAndScriptLogLineExecutionLogger(Serializable scriptId, Serializable scriptRevisionId)
   {
      super(scriptId, scriptRevisionId);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptExecutionStart(ExecuteCodeInput executeCodeInput)
   {
      try
      {
         super.acceptExecutionStart(executeCodeInput);

         InsertInput insertInput = new InsertInput();
         insertInput.setTableName("scriptLog");
         insertInput.setRecords(List.of(getScriptLog()));
         InsertOutput insertOutput = new InsertAction().execute(insertInput);

         setScriptLog(insertOutput.getRecords().get(0));
      }
      catch(Exception e)
      {
         LOG.warn("Error starting storage of script log", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptException(Exception exception)
   {
      store(null, exception);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptExecutionEnd(Serializable output)
   {
      store(output, null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void store(Serializable output, Exception exception)
   {
      try
      {
         updateHeaderAtEnd(output, exception);
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName("scriptLog");
         updateInput.setRecords(List.of(getScriptLog()));
         new UpdateAction().execute(updateInput);

         if(CollectionUtils.nullSafeHasContents(getScriptLogLines()))
         {
            InsertInput insertInput = new InsertInput();
            insertInput.setTableName("scriptLogLine");
            insertInput.setRecords(getScriptLogLines());
            new InsertAction().execute(insertInput);
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error storing script log", e);
      }
   }

}
