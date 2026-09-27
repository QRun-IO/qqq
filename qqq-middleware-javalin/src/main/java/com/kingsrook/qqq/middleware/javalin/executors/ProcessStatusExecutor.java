/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobState;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobStatus;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.middleware.javalin.QJavalinAccessLogger;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepOrStatusOutputInterface;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessStatusInput;
import com.kingsrook.qqq.middleware.javalin.executors.utils.ProcessExecutorUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessStatusExecutor extends AbstractMiddlewareExecutor<ProcessStatusInput, ProcessInitOrStepOrStatusOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(ProcessStatusExecutor.class);



   /***************************************************************************
    ** Note:  implementation of the output interface here, it wants to know what
    ** type it's going to be first, so, be polite and always call .setType before
    ** any other setters.
    ***************************************************************************/
   @Override
   public void execute(ProcessStatusInput input, ProcessInitOrStepOrStatusOutputInterface output) throws QException
   {
      try
      {
         ExecutorSessionUtils.setTableVariantInSession(input.getTableVariant());

         String processName = input.getProcessName();
         String processUUID = input.getProcessUUID();
         String jobUUID     = input.getJobUUID();

         LOG.debug("Request for status of process " + processUUID + ", job " + jobUUID);
         Optional<AsyncJobStatus> optionalJobStatus = new AsyncJobManager().getJobStatusForUser(jobUUID, processName, processUUID);
         if(optionalJobStatus.isEmpty())
         {
            ProcessExecutorUtils.serializeRunProcessExceptionForCaller(output, new RuntimeException("Could not find status of process step job"));
         }
         else
         {
            AsyncJobStatus jobStatus = optionalJobStatus.get();

            // resultForCaller.put("jobStatus", jobStatus);
            LOG.debug("Job status is " + jobStatus.getState() + " for " + jobUUID);

            if(jobStatus.getState().equals(AsyncJobState.COMPLETE))
            {
               ///////////////////////////////////////////////////////////////////////////////////////
               // if the job is complete, get the process result from state provider, and return it //
               // this output should look like it did if the job finished synchronously!!           //
               ///////////////////////////////////////////////////////////////////////////////////////
               Optional<ProcessState> processState = RunProcessAction.getStateForUser(processUUID, processName);
               if(processState.isPresent())
               {
                  RunProcessOutput runProcessOutput = new RunProcessOutput(processState.get());
                  ProcessExecutorUtils.serializeRunProcessResultForCaller(output, processName, runProcessOutput);
                  QJavalinAccessLogger.logProcessSummary(processName, processUUID, runProcessOutput);
               }
               else
               {
                  ProcessExecutorUtils.serializeRunProcessExceptionForCaller(output, new RuntimeException("Could not find results for process " + processUUID));
               }
            }
            else if(jobStatus.getState().equals(AsyncJobState.ERROR))
            {
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////
               // if the job had an error (e.g., a process step threw), "nicely" serialize its exception for the caller //
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////
               if(jobStatus.getCaughtException() != null)
               {
                  ProcessExecutorUtils.serializeRunProcessExceptionForCaller(output, jobStatus.getCaughtException());
               }
            }
            else
            {
               output.setType(ProcessInitOrStepOrStatusOutputInterface.Type.RUNNING);
               output.setMessage(jobStatus.getMessage());
               output.setCurrent(jobStatus.getCurrent());
               output.setTotal(jobStatus.getTotal());
            }
         }

         output.setProcessUUID(processUUID);
      }
      catch(Exception e)
      {
         ProcessExecutorUtils.serializeRunProcessExceptionForCaller(output, e);
      }
   }

}
