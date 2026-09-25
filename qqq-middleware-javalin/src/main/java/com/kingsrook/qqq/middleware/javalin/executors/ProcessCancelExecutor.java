/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
