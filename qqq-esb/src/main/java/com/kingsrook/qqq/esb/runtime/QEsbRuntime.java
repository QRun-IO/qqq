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

package com.kingsrook.qqq.esb.runtime;


import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * The ESB trigger runtime on this node: one EsbTriggerRunner for each trigger
 * (EsbProcessMetaData.triggers) in an instance.  An app starts it at startup
 * and stops it at shutdown, as it does the scheduler:
 *
 *    QEsbRuntime.getInstance().start(qInstance);
 *    ...
 *    QEsbRuntime.getInstance().stop();
 *
 * getInstance is the node's usual runtime.  More can be made with the
 * constructor (e.g., tests that act as two nodes on one broker).
 *
 * start takes a validated instance, as an app has at boot (as with the
 * scheduler): the runtime's threads each put it in their QContext, which
 * would otherwise validate it on each of them at once.
 *
 * start never blocks, even with the broker down: the runners connect on their
 * own threads, wait in CONNECTING while they can't, and rebuild their consumers
 * when EsbConnectionManager reports a reconnect.  Triggers with startPaused
 * start PAUSED.  A trigger whose destination isn't in the instance's ESB
 * meta-data (which validation reports) is skipped, with a warning.
 *
 * stop interrupts runs in progress and rolls their messages back (see
 * EsbTriggerRunner), and waits up to 10 s for the consumers to close.  The
 * runners stay available (getRunner, getRunners), STOPPED, until the next
 * start, which makes new ones.  The provider connections stay open: they
 * belong to EsbConnectionManager, and publishing shares them.
 *******************************************************************************/
public class QEsbRuntime
{
   private static final QLogger LOG = QLogger.getLogger(QEsbRuntime.class);

   static final Long STOP_TIMEOUT_MS = 10_000L;

   private static final QEsbRuntime INSTANCE = new QEsbRuntime();

   private volatile Map<String, EsbTriggerRunner> runners = Map.of();

   ///////////////////////////////////////////////////////////////////////
   // guarded by this.  startCount lets a connection listener from an   //
   // earlier start (they can't be removed) know that it's out of date  //
   ///////////////////////////////////////////////////////////////////////
   private Boolean running    = false;
   private Long    startCount = 0L;



   /*******************************************************************************
    ** Constructor - for a runtime of your own; most apps use getInstance.
    *******************************************************************************/
   public QEsbRuntime()
   {
   }



   /*******************************************************************************
    ** This node's usual runtime.
    *******************************************************************************/
   public static QEsbRuntime getInstance()
   {
      return (INSTANCE);
   }



   /*******************************************************************************
    ** Start a runner for each trigger in the instance.  Doesn't block.  Ignored
    ** (with a warning) if this runtime is already running.
    *******************************************************************************/
   public synchronized void start(QInstance qInstance)
   {
      if(running)
      {
         LOG.warn("The ESB runtime is already running; ignoring start");
         return;
      }

      Map<String, EsbTriggerRunner> newRunners = buildRunners(qInstance);
      runners = Collections.unmodifiableMap(newRunners);
      running = true;
      startCount++;

      Long        thisStart     = startCount;
      Set<String> providerNames = new LinkedHashSet<>();
      newRunners.values().forEach(runner -> providerNames.add(runner.getDestination().getProviderName()));
      for(String providerName : providerNames)
      {
         EsbConnectionManager.getInstance().addConnectionListener(providerName, () -> onReconnect(thisStart, providerName));
      }

      newRunners.values().forEach(EsbTriggerRunner::start);
      LOG.info("Started the ESB runtime", logPair("triggerCount", newRunners.size()));
   }



   /*******************************************************************************
    ** Stop every runner: runs in progress are interrupted and their messages
    ** rolled back.  Waits (up to 10 s) for the consumers to close.  No-op if not
    ** running.
    *******************************************************************************/
   public void stop()
   {
      List<EsbTriggerRunner> runnersToStop;
      synchronized(this)
      {
         if(!running)
         {
            return;
         }

         running = false;
         runnersToStop = List.copyOf(runners.values());
         runnersToStop.forEach(EsbTriggerRunner::requestStop);
      }

      Instant deadline = Instant.now().plusMillis(STOP_TIMEOUT_MS);
      for(EsbTriggerRunner runner : runnersToStop)
      {
         runner.awaitStopped(deadline);
      }

      LOG.info("Stopped the ESB runtime");
   }



   /*******************************************************************************
    ** Whether start has been called (and stop has not, since).
    *******************************************************************************/
   public synchronized Boolean isRunning()
   {
      return (running);
   }



   /*******************************************************************************
    ** The runner for a trigger (by its name, processName.destinationName) - or
    ** null if the last start had no such trigger.
    *******************************************************************************/
   public EsbTriggerRunner getRunner(String triggerName)
   {
      return (triggerName == null ? null : runners.get(triggerName));
   }



   /*******************************************************************************
    ** All the runners from the last start (in process, then trigger, order).
    *******************************************************************************/
   public List<EsbTriggerRunner> getRunners()
   {
      return (List.copyOf(runners.values()));
   }



   /*******************************************************************************
    ** A provider reconnected: pass it on to its runners - if this listener is
    ** from the current start.  Runs on the connection manager's thread.
    *******************************************************************************/
   private void onReconnect(Long fromStart, String providerName)
   {
      Map<String, EsbTriggerRunner> currentRunners;
      synchronized(this)
      {
         if(!running || !fromStart.equals(startCount))
         {
            return;
         }
         currentRunners = runners;
      }

      for(EsbTriggerRunner runner : currentRunners.values())
      {
         if(providerName.equals(runner.getDestination().getProviderName()))
         {
            runner.onReconnect();
         }
      }
   }



   /*******************************************************************************
    ** A (not started) runner for each trigger whose destination is known.
    *******************************************************************************/
   private static Map<String, EsbTriggerRunner> buildRunners(QInstance qInstance)
   {
      Map<String, EsbTriggerRunner> result              = new LinkedHashMap<>();
      EsbInstanceMetaData           esbInstanceMetaData = QSupplementalInstanceMetaData.of(qInstance, EsbInstanceMetaData.NAME);

      for(QProcessMetaData process : CollectionUtils.nonNullMap(qInstance.getProcesses()).values())
      {
         EsbProcessMetaData esbProcessMetaData = EsbProcessMetaData.of(process);
         if(esbProcessMetaData == null)
         {
            continue;
         }

         for(EsbTrigger trigger : CollectionUtils.nonNullList(esbProcessMetaData.getTriggers()))
         {
            QEsbDestinationMetaData destination = esbInstanceMetaData == null ? null : esbInstanceMetaData.getDestination(trigger.getDestinationName());
            if(destination == null)
            {
               LOG.warn("Not starting an ESB trigger whose destination is unknown", logPair("triggerName", trigger.getName(process.getName())));
               continue;
            }

            EsbTriggerRunner runner = new EsbTriggerRunner(qInstance, process.getName(), trigger, destination);
            result.put(runner.getTriggerName(), runner);
         }
      }

      return (result);
   }

}
