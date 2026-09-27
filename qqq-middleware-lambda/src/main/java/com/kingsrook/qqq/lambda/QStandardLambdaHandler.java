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

package com.kingsrook.qqq.lambda;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.async.JobGoingAsyncException;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.lambda.model.QLambdaRequest;
import com.kingsrook.qqq.lambda.model.QLambdaResponse;
import org.json.JSONObject;


/*******************************************************************************
 ** Class to provide QQQ Standard table & process actions via AWS Lambda.
 **
 ** Right now, an application is responsible for:
 ** - in a constructor, calling setQInstance.
 ** - overriding setupSession to do a default system-session...
 *******************************************************************************/
public class QStandardLambdaHandler extends QAbstractLambdaHandler
{
   protected QInstance qInstance;



   /*******************************************************************************
    ** Setter for qInstance
    **
    *******************************************************************************/
   public void setQInstance(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void setupSession(QLambdaRequest request, AbstractActionInput actionInput)
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected QLambdaResponse handleRequest(QLambdaRequest request) throws QException
   {
      String   path      = request.getPath();
      String[] pathParts = path.split("/");

      String httpMethod = "unknown";
      if(request.getRequestContext().has("http"))
      {
         JSONObject httpObject = request.getRequestContext().getJSONObject("http");
         httpMethod = httpObject.optString("method");
      }

      if(path.matches("/processes/\\w+/init/?"))
      {
         return (processInit(request, pathParts[2]));
      }
      if(path.matches("/metaData/?"))
      {
         // todo return (metaData(request));
      }
      if(path.matches("/metaData/table/\\w+/?"))
      {
         // todo return (tableMetaData(request));
      }
      if(path.matches("/metaData/process/\\w+/?"))
      {
         // todo return (processMetaData(request));
      }
      if(path.matches("/data/table/\\w+/?") || path.matches("/data/table/\\w+/query/?"))
      {
         // todo return (dataQuery(request));
      }
      if(path.matches("/data/table/\\w+/count/?"))
      {
         // todo return (dataCount(request));
      }
      if(path.matches("/data/table/\\w+/export/?"))
      {
         // todo return (dataExportWithoutFilename(request));
      }
      if(path.matches("/data/table/\\w+/export/\\w+/?"))
      {
         // todo return (dataExportWithFilename(request));
      }
      if(path.matches("/data/table/\\w+/prossibleValues/\\w+/?"))
      {
         // todo return (possibleValues(request));
      }
      if(path.matches("/data/table/\\w+/\\w+/?"))
      {
         if("GET".equals(httpMethod))
         {
            // todo return (dataGet(request));
         }
         else if("PATCH".equals(httpMethod))
         {
            // todo return (dataUpdate(request));
         }
         else if("PUT".equals(httpMethod))
         {
            // todo return (dataUpdate(request));
         }
         else if("DELETE".equals(httpMethod))
         {
            // todo return (dataDelete(request));
         }
         else
         {
            // todo return (new QLambdaResponse(405, "Unrecognized method: " + httpMethod));
         }
      }
      if(path.matches("/widget/\\w+/?"))
      {
         // todo return (widget(request));
      }
      else
      {
         return (new QLambdaResponse(404, "Unrecognized path: " + path));
      }

      return (GENERIC_SERVER_ERROR);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QLambdaResponse processInit(QLambdaRequest request, String processName)
   {
      String processUUID    = null;
      String startAfterStep = null;

      Map<String, Object> resultForCaller = new HashMap<>();
      QLambdaResponse     response;

      try
      {
         if(processUUID == null)
         {
            processUUID = UUID.randomUUID().toString();
         }
         resultForCaller.put("processUUID", processUUID);

         log(startAfterStep == null ? "Initiating process [" + processName + "] [" + processUUID + "]"
            : "Resuming process [" + processName + "] [" + processUUID + "] after step [" + startAfterStep + "]");

         RunProcessInput runProcessInput = new RunProcessInput();
         setupSession(request, runProcessInput);
         runProcessInput.setProcessName(processName);
         runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
         runProcessInput.setProcessUUID(processUUID);
         runProcessInput.setStartAfterStep(startAfterStep);
         populateRunProcessRequestWithValuesFromContext(request, runProcessInput);

         ////////////////////////////////////////
         // run the process as an async action //
         ////////////////////////////////////////
         Integer timeout = 60_000; // getTimeoutMillis(context);
         RunProcessOutput runProcessOutput = new AsyncJobManager().startJob(timeout, TimeUnit.MILLISECONDS, (callback) ->
         {
            runProcessInput.setAsyncJobCallback(callback);
            return (new RunProcessAction().execute(runProcessInput));
         });

         log("Process result error? " + runProcessOutput.getException());
         for(QFieldMetaData outputField : qInstance.getProcess(runProcessInput.getProcessName()).getOutputFields())
         {
            log("Process result output value: " + outputField.getName() + ": " + runProcessOutput.getValues().get(outputField.getName()));
         }

         serializeRunProcessResultForCaller(resultForCaller, runProcessOutput);
      }
      catch(JobGoingAsyncException jgae)
      {
         resultForCaller.put("jobUUID", jgae.getJobUUID());
      }
      catch(Exception e)
      {
         //////////////////////////////////////////////////////////////////////////////
         // our other actions in here would do: handleException(context, e);         //
         // which would return a 500 to the client.                                  //
         // but - other process-step actions, they always return a 200, just with an //
         // optional error message - so - keep all of the processes consistent.      //
         //////////////////////////////////////////////////////////////////////////////
         serializeRunProcessExceptionForCaller(resultForCaller, e);
      }

      response = new QLambdaResponse(200);
      response.getBody().setBody(resultForCaller);

      return (response);
   }



   /*******************************************************************************
    ** Whether a step finished synchronously or asynchronously, return its data
    ** to the caller the same way.
    *******************************************************************************/
   private void serializeRunProcessResultForCaller(Map<String, Object> resultForCaller, RunProcessOutput runProcessOutput)
   {
      if(runProcessOutput.getException().isPresent())
      {
         ////////////////////////////////////////////////////////////////
         // per code coverage, this path may never actually get hit... //
         ////////////////////////////////////////////////////////////////
         serializeRunProcessExceptionForCaller(resultForCaller, runProcessOutput.getException().get());
      }
      resultForCaller.put("values", runProcessOutput.getValues());
      runProcessOutput.getProcessState().getNextStepName().ifPresent(nextStep -> resultForCaller.put("nextStep", nextStep));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void serializeRunProcessExceptionForCaller(Map<String, Object> resultForCaller, Exception exception)
   {
      QUserFacingException userFacingException = ExceptionUtils.findClassInRootChain(exception, QUserFacingException.class);

      if(userFacingException != null)
      {
         log("User-facing exception in process", userFacingException);
         resultForCaller.put("error", userFacingException.getMessage());
         resultForCaller.put("userFacingError", userFacingException.getMessage());
      }
      else
      {
         Throwable rootException = ExceptionUtils.getRootException(exception);
         log("Uncaught Exception in process", exception);
         resultForCaller.put("error", "Error message: " + rootException.getMessage());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void populateRunProcessRequestWithValuesFromContext(QLambdaRequest request, RunProcessInput runProcessInput)
   {
      runProcessInput.addValue("body", request.getBody());
      // todo - a lot more...
   }

}
