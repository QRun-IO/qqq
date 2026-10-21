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

package com.kingsrook.qqq.backend.core.alerts;


import com.kingsrook.qqq.backend.core.actions.processes.listeners.ProcessLifecycleListenerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** Turns process lifecycle events into alerts, for processes that carry
 ** AlertProcessMetaData (registered automatically by its enrichment):
 ** - a failed run raises process.(name).failed (when alertOnFailure is set);
 ** - a completed run resolves it, and beats process.(name) (when
 **   expectedEvery is set), which a heartbeat monitor watches.
 **
 ** The exception message goes in the alert's internal details only, never in
 ** its external-safe title or summary.
 *******************************************************************************/
public class AlertProcessLifecycleListener implements ProcessLifecycleListenerInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean appliesTo(String processName)
   {
      return (getAlertProcessMetaData(processName) != null);
   }



   /*******************************************************************************
    ** Nothing to do when a run starts.
    *******************************************************************************/
   @Override
   public void onProcessStarted(RunProcessInput input)
   {
      ////////////////////////////////////////////
      // noop - only outcomes matter for alerts //
      ////////////////////////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void onProcessCompleted(RunProcessInput input, RunProcessOutput output)
   {
      String               processName          = input.getProcessName();
      AlertProcessMetaData alertProcessMetaData = getAlertProcessMetaData(processName);
      if(alertProcessMetaData == null)
      {
         return;
      }

      if(alertProcessMetaData.getAlertOnFailure() != null)
      {
         QAlerts.resolve(AlertProcessMetaData.failedTypeName(processName), processName, "A later run completed");
      }

      if(alertProcessMetaData.getExpectedEvery() != null)
      {
         QAlerts.heartbeat(AlertProcessMetaData.heartbeatKey(processName));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void onProcessFailed(RunProcessInput input, Exception exception)
   {
      String               processName          = input.getProcessName();
      AlertProcessMetaData alertProcessMetaData = getAlertProcessMetaData(processName);
      if(alertProcessMetaData == null || alertProcessMetaData.getAlertOnFailure() == null)
      {
         return;
      }

      QProcessMetaData process = QContext.getQInstance().getProcess(processName);
      String           label   = process != null && process.getLabel() != null ? process.getLabel() : processName;

      QAlerts.raise(new AlertSignal(AlertProcessMetaData.failedTypeName(processName))
         .withDedupKey(processName)
         .withSeverity(alertProcessMetaData.getAlertOnFailure())
         .withTitle(label + " failed")
         .withSummary("The latest run of " + label + " failed.")
         .withDetails(exception == null ? null : exception.getClass().getSimpleName() + ": " + exception.getMessage())
         .withAttribute("processName", processName)
         .withAttribute("processUUID", input.getProcessUUID()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AlertProcessMetaData getAlertProcessMetaData(String processName)
   {
      QInstance qInstance = QContext.getQInstance();
      if(qInstance == null || processName == null)
      {
         return (null);
      }
      return (AlertProcessMetaData.of(qInstance.getProcess(processName)));
   }
}
