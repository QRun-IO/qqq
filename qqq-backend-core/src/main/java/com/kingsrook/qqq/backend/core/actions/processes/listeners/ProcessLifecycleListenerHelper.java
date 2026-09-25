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


import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Calls the instance's ProcessLifecycleListenerInterface implementations.
 **
 ** Each listener is loaded via QCodeLoader and skipped unless it appliesTo the
 ** process.  Anything a listener throws (including a code reference that does
 ** not load as a listener) is caught and logged, so listeners can never change
 ** the outcome of a process run.  With no listeners registered, this is a no-op.
 *******************************************************************************/
public class ProcessLifecycleListenerHelper
{
   private static final QLogger LOG = QLogger.getLogger(ProcessLifecycleListenerHelper.class);



   /*******************************************************************************
    ** Tell applicable listeners that a new process run has started.
    *******************************************************************************/
   public static void fireStarted(QInstance qInstance, RunProcessInput input)
   {
      fire(qInstance, input, "started", listener -> listener.onProcessStarted(input));
   }



   /*******************************************************************************
    ** Tell applicable listeners that a process run has completed its last step.
    *******************************************************************************/
   public static void fireCompleted(QInstance qInstance, RunProcessInput input, RunProcessOutput output)
   {
      fire(qInstance, input, "completed", listener -> listener.onProcessCompleted(input, output));
   }



   /*******************************************************************************
    ** Tell applicable listeners that a process run has failed.
    *******************************************************************************/
   public static void fireFailed(QInstance qInstance, RunProcessInput input, Exception exception)
   {
      fire(qInstance, input, "failed", listener -> listener.onProcessFailed(input, exception));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void fire(QInstance qInstance, RunProcessInput input, String eventName, Consumer<ProcessLifecycleListenerInterface> callback)
   {
      if(qInstance == null)
      {
         return;
      }

      List<QCodeReference> listenerCodeReferences = qInstance.getProcessLifecycleListeners();
      if(CollectionUtils.nullSafeIsEmpty(listenerCodeReferences))
      {
         return;
      }

      String processName = input.getProcessName();
      for(QCodeReference listenerCodeReference : listenerCodeReferences)
      {
         String listenerName = listenerCodeReference == null ? null : listenerCodeReference.getName();
         try
         {
            ProcessLifecycleListenerInterface listener = QCodeLoader.getAdHoc(ProcessLifecycleListenerInterface.class, listenerCodeReference);
            if(listener == null)
            {
               LOG.warn("Could not load process lifecycle listener", logPair("event", eventName), logPair("processName", processName), logPair("listener", listenerName));
               continue;
            }

            if(listener.appliesTo(processName))
            {
               callback.accept(listener);
            }
         }
         catch(Exception e)
         {
            LOG.warn("Error calling process lifecycle listener", e, logPair("event", eventName), logPair("processName", processName), logPair("processUUID", input.getProcessUUID()), logPair("listener", listenerName));
         }
      }
   }

}
