/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.processes.listeners;


import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;


/*******************************************************************************
 ** Instance-level listener for the lifecycle of process runs, registered via
 ** QInstance.withProcessLifecycleListener.  Unlike a process's single
 ** processTracerCodeReference, any number of these can be registered, and
 ** they apply across processes (filtered by appliesTo).
 **
 ** Started fires once per run (not when a run resumes after a frontend step,
 ** or goes back to re-run a step).  Completed fires when the run's last step
 ** has finished.  Failed fires when a step (or the run itself) throws, on the
 ** run's first request or on a resumed one.
 **
 ** A started event is not always followed by exactly one terminal (completed
 ** or failed) event, so don't assume it is - correlate events by processUUID:
 ** - a run that stops at a frontend step may never be resumed (no terminal).
 ** - a resumed request that fails before its steps run (e.g., its process
 **   state has expired) fires nothing.
 ** - a failed step can be retried, and a final step re-run (after going
 **   back), so one run can fire failed, or completed, more than once.
 **
 ** A new instance is loaded (via QCodeLoader) for each event, so a listener
 ** can't carry state in its own fields from started to completed; keep any
 ** such state outside the instance.
 **
 ** Exceptions (and LinkageErrors, such as a NoClassDefFoundError from an
 ** optional library that isn't on the classpath) thrown by a listener are
 ** caught and logged, and never affect the process.
 *******************************************************************************/
public interface ProcessLifecycleListenerInterface
{
   /***************************************************************************
    ** Whether this listener wants events for the named process.
    ***************************************************************************/
   boolean appliesTo(String processName);

   /***************************************************************************
    ** Called once, when a new run of a process starts.
    ***************************************************************************/
   void onProcessStarted(RunProcessInput input);

   /***************************************************************************
    ** Called when the last step of a process run has finished.
    ***************************************************************************/
   void onProcessCompleted(RunProcessInput input, RunProcessOutput output);

   /***************************************************************************
    ** Called when a process run fails, with the exception that ended it.
    ***************************************************************************/
   void onProcessFailed(RunProcessInput input, Exception exception);

}
