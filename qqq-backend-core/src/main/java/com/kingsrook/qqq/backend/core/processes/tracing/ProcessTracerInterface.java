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
 ** Interface that can be plugged into the execution of a QProcess, that gets
 ** callbacks from QQQ for events in the lifecycle of a process, which one may
 ** wish to log or otherwise be aware of.
 *******************************************************************************/
public interface ProcessTracerInterface
{
   /***************************************************************************
    ** Called when a new process is started.
    ***************************************************************************/
   void handleProcessStart(RunProcessInput runProcessInput);

   /***************************************************************************
    ** Called when a process is resumed, e.g., after a "break" occurs between
    ** backend steps and frontend steps.
    ***************************************************************************/
   void handleProcessResume(RunProcessInput runProcessInput);

   /***************************************************************************
    ** Called when a (backend) step is started.
    ***************************************************************************/
   void handleStepStart(RunBackendStepInput runBackendStepInput);

   /***************************************************************************
    ** Called when the (application, custom) process step code itself decides to
    ** trace something.  We imagine various subclasses of ProcessTracerMessage
    ** to be created, to communicate more specific data for the tracer implementation.
    ***************************************************************************/
   void handleMessage(RunBackendStepInput runBackendStepInput, ProcessTracerMessage message);

   /***************************************************************************
    ** Called when a (backend) step finishes.
    ***************************************************************************/
   void handleStepFinish(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput);

   /***************************************************************************
    ** Called when a process break occurs, e.g., between backend and frontend
    ** steps (but only if there are no more backend steps in the queue).
    ***************************************************************************/
   void handleProcessBreak(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput, Exception processException);

   /***************************************************************************
    ** Called after the last (backend) step of a process.
    ***************************************************************************/
   void handleProcessFinish(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput, Exception processException);

}
