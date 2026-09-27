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

package com.kingsrook.qqq.middleware.javalin.executors.utils;


import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepOrStatusOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessExecutorUtils
{
   private static final QLogger LOG = QLogger.getLogger(ProcessExecutorUtils.class);



   /*******************************************************************************
    ** Whether a step finished synchronously or asynchronously, return its data
    ** to the caller the same way.
    *******************************************************************************/
   public static void serializeRunProcessResultForCaller(ProcessInitOrStepOrStatusOutputInterface processInitOrStepOutput, String processName, RunProcessOutput runProcessOutput)
   {
      processInitOrStepOutput.setType(ProcessInitOrStepOrStatusOutputInterface.Type.COMPLETE);

      if(runProcessOutput.getException().isPresent())
      {
         ////////////////////////////////////////////////////////////////
         // per code coverage, this path may never actually get hit... //
         ////////////////////////////////////////////////////////////////
         serializeRunProcessExceptionForCaller(processInitOrStepOutput, runProcessOutput.getException().get());
      }

      processInitOrStepOutput.setValues(runProcessOutput.getValues());
      // processInitOrStepOutput.setValues(getValuesForCaller(processName, runProcessOutput));

      runProcessOutput.getProcessState().getNextStepName().ifPresent(nextStep -> processInitOrStepOutput.setNextStep(nextStep));
      runProcessOutput.getProcessState().getBackStepName().ifPresent(backStep -> processInitOrStepOutput.setBackStep(backStep));

      if(runProcessOutput.getProcessMetaDataAdjustment() != null)
      {
         processInitOrStepOutput.setProcessMetaDataAdjustment(runProcessOutput.getProcessMetaDataAdjustment());
      }
   }

   // /***************************************************************************
   //  ** maybe good idea here, but... to only return fields that the frontend steps
   //  ** say they care about.  yeah.
   //  ***************************************************************************/
   // private static Map<String, Serializable> getValuesForCaller(String processName, RunProcessOutput runProcessOutput)
   // {
   //    QProcessMetaData process = QContext.getQInstance().getProcess(processName);
   //    Map<String, QFieldMetaData> frontendFields = new LinkedHashMap<>();
   //    for(QStepMetaData step : process.getAllSteps().values())
   //    {
   //       if(step instanceof QFrontendStepMetaData frontendStepMetaData)
   //       {
   //          frontendFields.addAll(frontendStepMetaData.getAllFields());
   //       }
   //       else if(step instanceof QStateMachineStep stateMachineStep)
   //       {
   //          for(QStepMetaData subStep : stateMachineStep.getSubSteps())
   //          {
   //             // recur, etc
   //          }
   //       }
   //    }
   //
   //    // then, only return ones in the map, eh
   // }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void serializeRunProcessExceptionForCaller(ProcessInitOrStepOrStatusOutputInterface processInitOrStepOutput, Exception exception)
   {
      processInitOrStepOutput.setType(ProcessInitOrStepOrStatusOutputInterface.Type.ERROR);

      QUserFacingException userFacingException = ExceptionUtils.findClassInRootChain(exception, QUserFacingException.class);

      if(userFacingException != null)
      {
         LOG.info("User-facing exception in process", userFacingException);
         processInitOrStepOutput.setError(userFacingException.getMessage());
         processInitOrStepOutput.setUserFacingError(userFacingException.getMessage());
      }
      else
      {
         Throwable rootException = ExceptionUtils.getRootException(exception);
         LOG.warn("Uncaught Exception in process", exception);
         processInitOrStepOutput.setError("Error message: " + rootException.getMessage());
      }
   }
}
