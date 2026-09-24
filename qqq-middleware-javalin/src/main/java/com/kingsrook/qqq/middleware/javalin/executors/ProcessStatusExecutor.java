/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
