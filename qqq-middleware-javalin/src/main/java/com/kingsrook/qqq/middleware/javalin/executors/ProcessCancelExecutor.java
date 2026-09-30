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

package com.kingsrook.qqq.middleware.javalin.executors;


import com.kingsrook.qqq.backend.core.actions.processes.CancelProcessAction;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.middleware.javalin.executors.io.EmptyMiddlewareOutputInterface;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessCancelInput;


/*******************************************************************************
 ** Executor for cancelling a running process.
 *******************************************************************************/
public class ProcessCancelExecutor extends AbstractMiddlewareExecutor<ProcessCancelInput, EmptyMiddlewareOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(ProcessCancelInput input, EmptyMiddlewareOutputInterface output) throws QException
   {
      /////////////////////////////////////////////////////////////////////////
      // the process's state belongs to the variant the process started with //
      /////////////////////////////////////////////////////////////////////////
      ExecutorSessionUtils.setTableVariantInSession(input.getTableVariant());

      //////////////////////////////////////////////////////////////////////
      // only the session that ran the process may cancel it: another    //
      // user's process state is refused before its cancel step can run //
      //////////////////////////////////////////////////////////////////////
      RunProcessAction.getStateForUser(input.getProcessUUID(), input.getProcessName());

      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setInputSource(QInputSource.USER);
      runProcessInput.setProcessName(input.getProcessName());
      runProcessInput.setProcessUUID(input.getProcessUUID());

      new CancelProcessAction().execute(runProcessInput);
   }

}
