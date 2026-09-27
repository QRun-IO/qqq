/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.publish;


import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.processes.listeners.ProcessLifecycleListenerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbProcessEvent;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessPublication;


/*******************************************************************************
 * Publishes process lifecycle events (spec sections 4 and 5) - started,
 * completed, failed - to the destinations of the process's ESB publications
 * (EsbProcessMetaData) that list the event.  EsbInstanceMetaData.enrich
 * registers it on the instance.
 *
 * Each event's data is { processName, processUUID }, plus error (the
 * exception's message) for failed.  Events are sent right away, on the
 * process's thread - so an event from a SINGLE-mode triggered run carries the
 * triggering event's id as its causation id (EsbEventFactory reads
 * EsbCausation).
 *
 * Sending never fails (or changes the outcome of) the process: EsbPublisher
 * logs and counts failures, and the core helper catches anything else a
 * listener throws.
 *
 * Holds no state (a new instance is loaded for each event).
 *******************************************************************************/
public class EsbProcessLifecycleListener implements ProcessLifecycleListenerInterface
{

   /*******************************************************************************
    ** Whether the process (in the QContext's instance) publishes any lifecycle
    ** event.
    *******************************************************************************/
   @Override
   public boolean appliesTo(String processName)
   {
      for(EsbProcessEvent processEvent : EsbProcessEvent.values())
      {
         if(!getDestinationNames(processName, processEvent).isEmpty())
         {
            return (true);
         }
      }

      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void onProcessStarted(RunProcessInput input)
   {
      publish(input, EsbProcessEvent.STARTED, null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void onProcessCompleted(RunProcessInput input, RunProcessOutput output)
   {
      publish(input, EsbProcessEvent.COMPLETED, null);
   }



   /*******************************************************************************
    ** Publish failed, with the exception's message (or, if it has none, its
    ** class name) as the event's error.
    *******************************************************************************/
   @Override
   public void onProcessFailed(RunProcessInput input, Exception exception)
   {
      publish(input, EsbProcessEvent.FAILED, exception == null ? null : EsbPublisher.getErrorText(exception));
   }



   /*******************************************************************************
    ** Make one event, and send it to each destination that publishes it.
    *******************************************************************************/
   private static void publish(RunProcessInput input, EsbProcessEvent processEvent, String error)
   {
      Set<String> destinationNames = getDestinationNames(input.getProcessName(), processEvent);
      if(destinationNames.isEmpty())
      {
         return;
      }

      EsbEvent event = EsbEventFactory.forProcess(EsbPublisher.getInstanceNameFromContext(), input.getProcessName(), processEvent, input.getProcessUUID(), error);
      for(String destinationName : destinationNames)
      {
         EsbPublisher.getInstance().publish(destinationName, List.of(event));
      }
   }



   /*******************************************************************************
    ** The destinations (in publication order, each once) of the process's
    ** publications that list this event.  Empty if there is no QInstance in
    ** context, no such process, or no ESB meta-data on it.
    *******************************************************************************/
   private static Set<String> getDestinationNames(String processName, EsbProcessEvent processEvent)
   {
      QInstance          qInstance          = QContext.getQInstance();
      QProcessMetaData   process            = (qInstance == null || processName == null) ? null : qInstance.getProcess(processName);
      EsbProcessMetaData esbProcessMetaData = (process == null) ? null : EsbProcessMetaData.of(process);
      if(esbProcessMetaData == null)
      {
         return (Set.of());
      }

      Set<String> destinationNames = new LinkedHashSet<>();
      for(EsbProcessPublication publication : CollectionUtils.nonNullList(esbProcessMetaData.getPublications()))
      {
         if(CollectionUtils.nonNullList(publication.getEvents()).contains(processEvent))
         {
            destinationNames.add(publication.getDestinationName());
         }
      }

      return (destinationNames);
   }

}
