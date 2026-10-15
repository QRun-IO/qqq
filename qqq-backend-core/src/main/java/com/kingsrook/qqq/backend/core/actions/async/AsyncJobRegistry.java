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

package com.kingsrook.qqq.backend.core.actions.async;


import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.cluster.QNodeRoles;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.state.QStateProviders;
import com.kingsrook.qqq.backend.core.state.StateProviderInterface;
import com.kingsrook.qqq.backend.core.state.StateProviderMetaData;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.core.utils.PrefixedDefaultThreadFactory;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** The async jobs running on this node, and how their statuses reach a shared
 ** (multi-node) state provider (spec 2026-10-15-cluster-ha-design, section B):
 ** - progress writes are throttled per job (state changes are not);
 ** - before each write, a cancel requested from any node is read back;
 ** - a heartbeat re-writes each running job's status (and flushes throttled
 **   progress), so a reader can tell a job whose node died - its status says
 **   RUNNING but its heartbeat is stale - and report it as failed.
 ** With in-memory state none of this applies: writes go straight through.
 *******************************************************************************/
public final class AsyncJobRegistry
{
   private static final QLogger LOG = QLogger.getLogger(AsyncJobRegistry.class);

   public static final String LOST_NODE_MESSAGE = "The server running this step stopped before it finished. Please run it again.";

   private static final Map<UUID, RunningJob> RUNNING = new ConcurrentHashMap<>();

   private static ScheduledExecutorService heartbeat;



   /*******************************************************************************
    ** A job running on this node.
    *******************************************************************************/
   private static final class RunningJob
   {
      private final QInstance      instance;
      private final AsyncJobStatus status;
      private volatile long        lastWriteMillis;
      private volatile long        lastCancelCheckMillis;



      /*******************************************************************************
       ** Constructor.
       *******************************************************************************/
      private RunningJob(QInstance instance, AsyncJobStatus status)
      {
         this.instance = instance;
         this.status = status;
      }
   }



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private AsyncJobRegistry()
   {
   }



   /*******************************************************************************
    ** Register a job starting on this node (stamps its node and heartbeat).
    *******************************************************************************/
   static void register(UUID jobUUID, AsyncJobStatus status)
   {
      status.setNodeId(QNodeRoles.nodeId());
      status.setHeartbeatAt(Instant.now());
      QInstance instance = QContext.getQInstance();
      RUNNING.put(jobUUID, new RunningJob(instance, status));
      if(QStateProviders.isShared(instance))
      {
         startHeartbeat(instance);
      }
   }



   /*******************************************************************************
    ** A job finished on this node.
    *******************************************************************************/
   static void unregister(UUID jobUUID)
   {
      RUNNING.remove(jobUUID);
   }



   /*******************************************************************************
    ** Number of async jobs running on this node.
    *******************************************************************************/
   public static int runningCount()
   {
      return (RUNNING.size());
   }



   /*******************************************************************************
    ** Wait (up to the timeout) for this node's running jobs to finish. Returns
    ** whether all did.
    *******************************************************************************/
   public static boolean awaitIdle(Duration timeout)
   {
      long deadline = System.currentTimeMillis() + timeout.toMillis();
      while(!RUNNING.isEmpty() && System.currentTimeMillis() < deadline)
      {
         try
         {
            Thread.sleep(250);
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            break;
         }
      }
      if(!RUNNING.isEmpty())
      {
         LOG.warn("Async jobs still running at the end of the drain", logPair("count", RUNNING.size()));
      }
      return (RUNNING.isEmpty());
   }



   /*******************************************************************************
    ** Store a job's status. Progress (force false) is throttled when state is
    ** shared; state changes (force true) are written at once.
    *******************************************************************************/
   static void store(UUID jobUUID, AsyncJobStatus status, boolean force)
   {
      StateProviderInterface provider = AsyncJobManager.getStateProvider();
      UUIDAndTypeStateKey    key      = new UUIDAndTypeStateKey(jobUUID, StateType.ASYNC_JOB_STATUS);
      RunningJob             running  = RUNNING.get(jobUUID);
      StateProviderMetaData  metaData = StateProviderMetaData.of(QContext.getQInstance());

      if(metaData == null || !metaData.isShared())
      {
         provider.put(key, status);
         return;
      }

      long now = System.currentTimeMillis();
      if(!force && running != null && now - running.lastWriteMillis < metaData.getProgressWriteIntervalMillis())
      {
         //////////////////////////////////////////////////////////////
         // throttled: the next write (or the heartbeat) carries it //
         //////////////////////////////////////////////////////////////
         return;
      }

      synchronized(status)
      {
         mergeStoredCancel(provider, key, status);
         status.setHeartbeatAt(Instant.now());
         provider.put(key, status);
      }

      if(running != null)
      {
         running.lastWriteMillis = now;
         running.lastCancelCheckMillis = now;
      }
   }



   /*******************************************************************************
    ** Whether a cancel was requested for a job, checking the shared store (at
    ** most once a second) when state is shared.
    *******************************************************************************/
   static boolean wasCancelRequested(UUID jobUUID, AsyncJobStatus status)
   {
      if(status.getCancelRequested())
      {
         return (true);
      }

      RunningJob running = RUNNING.get(jobUUID);
      if(running == null || !QStateProviders.isShared(running.instance))
      {
         return (false);
      }

      long now = System.currentTimeMillis();
      if(now - running.lastCancelCheckMillis >= 1000)
      {
         running.lastCancelCheckMillis = now;
         try
         {
            Optional<AsyncJobStatus> stored = QStateProviders.forInstance(running.instance).get(AsyncJobStatus.class, new UUIDAndTypeStateKey(jobUUID, StateType.ASYNC_JOB_STATUS));
            if(stored.isPresent() && stored.get().getCancelRequested())
            {
               status.setCancelRequested(true);
            }
         }
         catch(Exception e)
         {
            LOG.debug("Could not check for a cancel request", e);
         }
      }
      return (status.getCancelRequested());
   }



   /*******************************************************************************
    ** Request a cancel: on the live status when the job runs here, and in the
    ** store (so the node running it sees it).
    *******************************************************************************/
   static void requestCancel(UUID jobUUID, AsyncJobStatus storedStatus)
   {
      RunningJob running = RUNNING.get(jobUUID);
      if(running != null)
      {
         running.status.setCancelRequested(true);
      }

      storedStatus.setCancelRequested(true);
      AsyncJobManager.getStateProvider().put(new UUIDAndTypeStateKey(jobUUID, StateType.ASYNC_JOB_STATUS), storedStatus);
   }



   /*******************************************************************************
    ** Keep a cancel that another node stored.
    *******************************************************************************/
   private static void mergeStoredCancel(StateProviderInterface provider, UUIDAndTypeStateKey key, AsyncJobStatus status)
   {
      if(status.getCancelRequested())
      {
         return;
      }
      try
      {
         Optional<AsyncJobStatus> stored = provider.get(AsyncJobStatus.class, key);
         if(stored.isPresent() && stored.get().getCancelRequested())
         {
            status.setCancelRequested(true);
         }
      }
      catch(Exception e)
      {
         LOG.debug("Could not read a stored cancel request", e);
      }
   }



   /*******************************************************************************
    ** A status as a reader should see it: a RUNNING job whose heartbeat is older
    ** than lostAfterSeconds ran on a node that is gone - report it failed.
    *******************************************************************************/
   static AsyncJobStatus checkLost(AsyncJobStatus status)
   {
      if(status == null || status.getState() != AsyncJobState.RUNNING || status.getHeartbeatAt() == null)
      {
         return (status);
      }

      StateProviderMetaData metaData = StateProviderMetaData.of(QContext.getQInstance());
      if(metaData == null || !metaData.isShared())
      {
         return (status);
      }

      if(status.getHeartbeatAt().isBefore(Instant.now().minusSeconds(metaData.getLostAfterSeconds())))
      {
         LOG.info("Async job's node stopped heart-beating; reporting it failed", logPair("nodeId", status.getNodeId()),
            logPair("heartbeatAt", status.getHeartbeatAt()));
         status.setState(AsyncJobState.ERROR);
         status.setCaughtException(new QUserFacingException(LOST_NODE_MESSAGE));
      }
      return (status);
   }



   /*******************************************************************************
    ** Start the heartbeat (once).
    *******************************************************************************/
   private static synchronized void startHeartbeat(QInstance instance)
   {
      if(heartbeat != null)
      {
         return;
      }

      StateProviderMetaData metaData = StateProviderMetaData.of(instance);
      int                   seconds  = metaData == null ? 10 : metaData.getHeartbeatSeconds();
      heartbeat = Executors.newSingleThreadScheduledExecutor(new PrefixedDefaultThreadFactory(AsyncJobRegistry.class));
      heartbeat.scheduleAtFixedRate(AsyncJobRegistry::beat, seconds, seconds, TimeUnit.SECONDS);
   }



   /*******************************************************************************
    ** Re-write every running job's status (heartbeat + any throttled progress).
    *******************************************************************************/
   static void beat()
   {
      for(Map.Entry<UUID, RunningJob> entry : RUNNING.entrySet())
      {
         RunningJob running = entry.getValue();
         if(running.instance == null || !QStateProviders.isShared(running.instance))
         {
            continue;
         }
         try
         {
            QContext.withTemporaryContext(new CapturedContext(running.instance, null), () -> store(entry.getKey(), running.status, true));
         }
         catch(Exception e)
         {
            LOG.warn("Could not write an async job heartbeat", e);
         }
      }
   }
}
