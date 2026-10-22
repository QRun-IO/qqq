/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.tracing;


import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;


/*******************************************************************************
 ** Implementation of ProcessTracerInterface that does nothing (no-op).
 *******************************************************************************/
public class NoopProcessTracer implements ProcessTracerInterface
{


   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleProcessStart(RunProcessInput runProcessInput)
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleProcessResume(RunProcessInput runProcessInput)
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleStepStart(RunBackendStepInput runBackendStepInput)
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleMessage(RunBackendStepInput runBackendStepInput, ProcessTracerMessage message)
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleStepFinish(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleProcessBreak(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput, Exception processException)
   {
   }




   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleProcessFinish(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput, Exception processException)
   {
   }

}
