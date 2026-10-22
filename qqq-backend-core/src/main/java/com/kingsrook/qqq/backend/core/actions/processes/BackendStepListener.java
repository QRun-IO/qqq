/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;


/*******************************************************************************
 ** Optional hook on RunProcessInput, called by RunProcessAction after each
 ** backend step finishes successfully (after its state is stored). Durable jobs
 ** use it to checkpoint the process values after every step, and to stop a run
 ** cooperatively (pause, cancel, drain) by throwing.
 *******************************************************************************/
@FunctionalInterface
public interface BackendStepListener
{

   /*******************************************************************************
    ** Called after a backend step. Throwing stops the process run.
    *******************************************************************************/
   void afterBackendStep(String stepName, ProcessState processState) throws QException;

}
