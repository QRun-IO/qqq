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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Action handler for running the cancel step of a qqq process
 *
 *******************************************************************************/
public class CancelProcessAction extends RunProcessAction
{
   private static final QLogger LOG = QLogger.getLogger(CancelProcessAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunProcessOutput execute(RunProcessInput runProcessInput) throws QException
   {
      ActionHelper.validateSession(runProcessInput);

      QProcessMetaData process = QContext.getQInstance().getProcess(runProcessInput.getProcessName());
      if(process == null)
      {
         throw new QBadRequestException("Process [" + runProcessInput.getProcessName() + "] is not defined in this instance.");
      }

      if(runProcessInput.getProcessUUID() == null)
      {
         throw (new QBadRequestException("Cannot cancel process - processUUID was not given."));
      }

      UUIDAndTypeStateKey    stateKey     = new UUIDAndTypeStateKey(UUID.fromString(runProcessInput.getProcessUUID()), StateType.PROCESS_STATUS);
      Optional<ProcessState> processState = runProcessInput.getInputSource() == QInputSource.USER
         ? getStateForUser(runProcessInput.getProcessUUID(), runProcessInput.getProcessName()) : getState(runProcessInput.getProcessUUID());
      if(processState.isEmpty())
      {
         throw (new QBadRequestException("Cannot cancel process - State for process UUID [" + runProcessInput.getProcessUUID() + "] was not found."));
      }

      RunProcessOutput runProcessOutput = new RunProcessOutput();
      try
      {
         if(process.getCancelStep() != null)
         {
            LOG.info("Running cancel step for process", logPair("processName", process.getName()));
            runBackendStep(runProcessInput, process, runProcessOutput, stateKey, process.getCancelStep(), process, processState.get());
         }
         else
         {
            LOG.debug("Process does not have a custom cancel step to run.", logPair("processName", process.getName()));
         }
      }
      catch(QException qe)
      {
         ////////////////////////////////////////////////////////////
         // upon exception (e.g., one thrown by a step), throw it. //
         ////////////////////////////////////////////////////////////
         throw (qe);
      }
      catch(Exception e)
      {
         throw (new QException("Error cancelling process", e));
      }
      finally
      {
         //////////////////////////////////////////////////////
         // always put the final state in the process result //
         //////////////////////////////////////////////////////
         runProcessOutput.setProcessState(processState.get());
      }

      return (runProcessOutput);
   }

}
