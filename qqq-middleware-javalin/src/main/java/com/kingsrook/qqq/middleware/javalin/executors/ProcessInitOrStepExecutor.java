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


import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.async.JobGoingAsyncException;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallback;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.QJavalinAccessLogger;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepOrStatusOutputInterface;
import com.kingsrook.qqq.middleware.javalin.executors.utils.ProcessExecutorUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessInitOrStepExecutor extends AbstractMiddlewareExecutor<ProcessInitOrStepInput, ProcessInitOrStepOrStatusOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(ProcessInitOrStepExecutor.class);



   /***************************************************************************
    ** Note:  implementation of the output interface here, it wants to know what
    ** type it's going to be first, so, be polite and always call .setType before
    ** any other setters.
    ***************************************************************************/
   @Override
   public void execute(ProcessInitOrStepInput input, ProcessInitOrStepOrStatusOutputInterface output) throws QException
   {
      Exception returningException = null;

      String processName    = input.getProcessName();
      String startAfterStep = input.getStartAfterStep();
      String startAtStep    = input.getStartAtStep();
      String processUUID    = input.getProcessUUID();

      if(processUUID == null)
      {
         processUUID = UUID.randomUUID().toString();
      }

      if(startAtStep != null)
      {
         LOG.info("Resuming process [" + processName + "] [" + processUUID + "] at step [" + startAtStep + "]");
      }
      else
      {
         LOG.info(startAfterStep == null ? "Initiating process [" + processName + "] [" + processUUID + "]"
            : "Resuming process [" + processName + "] [" + processUUID + "] after step [" + startAfterStep + "]");
      }

      try
      {
         /////////////////////////////////////////////////////////////////////////
         // a variant-backed table's process runs against the requested variant //
         /////////////////////////////////////////////////////////////////////////
         ExecutorSessionUtils.setTableVariantInSession(input.getTableVariant());

         RunProcessInput runProcessInput = new RunProcessInput();
         QContext.pushAction(runProcessInput);

         runProcessInput.setInputSource(QInputSource.USER);
         runProcessInput.setProcessName(processName);
         runProcessInput.setFrontendStepBehavior(input.getFrontendStepBehavior());
         runProcessInput.setProcessUUID(processUUID);
         runProcessInput.setStartAfterStep(startAfterStep);
         runProcessInput.setStartAtStep(startAtStep);
         runProcessInput.setValues(Objects.requireNonNullElseGet(input.getValues(), HashMap::new));

         if(input.getRecordsFilter() != null)
         {
            runProcessInput.setCallback(new QProcessCallback()
            {
               /***************************************************************************
                **
                ***************************************************************************/
               @Override
               public QQueryFilter getQueryFilter()
               {
                  return (input.getRecordsFilter());
               }



               /***************************************************************************
                **
                ***************************************************************************/
               @Override
               public Map<String, Serializable> getFieldValues(List<QFieldMetaData> fields)
               {
                  return (Collections.emptyMap());
               }
            });
         }

         String reportName = ValueUtils.getValueAsString(runProcessInput.getValue("reportName"));
         QJavalinAccessLogger.logStart(startAfterStep == null && startAtStep == null ? "processInit" : "processStep", logPair("processName", processName), logPair("processUUID", processUUID),
            StringUtils.hasContent(startAfterStep) ? logPair("startAfterStep", startAfterStep) : null,
            StringUtils.hasContent(reportName) ? logPair("reportName", reportName) : null);

         //////////////////////////////////////////////////////////////////////////////////////////////////
         // important to do this check AFTER the runProcessInput is populated with values from context - //
         // e.g., in case things like a reportName are set in here                                       //
         //////////////////////////////////////////////////////////////////////////////////////////////////
         PermissionsHelper.checkProcessPermissionThrowing(runProcessInput, processName);

         ////////////////////////////////////////
         // run the process as an async action //
         ////////////////////////////////////////
         RunProcessOutput runProcessOutput = new AsyncJobManager().withProcessUUID(processUUID).startJob(processName, input.getStepTimeoutMillis(), TimeUnit.MILLISECONDS, (callback) ->
         {
            runProcessInput.setAsyncJobCallback(callback);
            return (new RunProcessAction().execute(runProcessInput));
         });

         LOG.debug("Process result error? " + runProcessOutput.getException());
         for(QFieldMetaData outputField : QContext.getQInstance().getProcess(runProcessInput.getProcessName()).getOutputFields())
         {
            LOG.debug("Process result output value: " + outputField.getName() + ": " + runProcessOutput.getValues().get(outputField.getName()));
         }

         ProcessExecutorUtils.serializeRunProcessResultForCaller(output, processName, runProcessOutput);
         QJavalinAccessLogger.logProcessSummary(processName, processUUID, runProcessOutput);
      }
      catch(JobGoingAsyncException jgae)
      {
         output.setType(ProcessInitOrStepOrStatusOutputInterface.Type.JOB_STARTED);
         output.setJobUUID(jgae.getJobUUID());
      }
      catch(QPermissionDeniedException | QAuthenticationException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         //////////////////////////////////////////////////////////////////////////////
         // our other actions in here would do: handleException(context, e);         //
         // which would return a 500 to the client.                                  //
         // but - other process-step actions, they always return a 200, just with an //
         // optional error message - so - keep all of the processes consistent.      //
         //////////////////////////////////////////////////////////////////////////////
         returningException = e;
         ProcessExecutorUtils.serializeRunProcessExceptionForCaller(output, e);
      }

      output.setProcessUUID(processUUID);

      if(returningException != null)
      {
         QJavalinAccessLogger.logEndFail(returningException);
      }
      else
      {
         QJavalinAccessLogger.logEndSuccess();
      }
   }

}
