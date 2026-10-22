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

package com.kingsrook.qqq.backend.core.cluster;


import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Cluster control events (spec 2026-10-15-cluster-ha-design): when a node
 ** changes something other nodes hold in memory - a session ends, the meta data
 ** overlay changes, a cached record is edited - it publishes an event. Every
 ** node (this one at once, the others through the transport) runs the handlers
 ** registered for that event type. A node ignores its own events coming back
 ** from the transport. Caches keep short TTLs as a safety net in case an event
 ** is lost.
 **
 ** Handlers are global (ClusterBus.on) and run as the system user of the
 ** instance the event arrived on.
 *******************************************************************************/
public final class ClusterBus
{
   private static final QLogger LOG = QLogger.getLogger(ClusterBus.class);

   private static final Map<QInstance, Connection> CONNECTIONS = new WeakHashMap<>();

   private static final Map<String, List<BiConsumer<QInstance, ClusterEvent>>> HANDLERS = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** An instance's transport and its subscription.
    **
    ** @param transport the transport
    ** @param subscription this node's subscription to it
    *******************************************************************************/
   private record Connection(ClusterBusInterface transport, AutoCloseable subscription)
   {
   }



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private ClusterBus()
   {
   }



   /*******************************************************************************
    ** Register a handler for an event type (it runs on every node).
    *******************************************************************************/
   public static void on(String type, BiConsumer<QInstance, ClusterEvent> handler)
   {
      HANDLERS.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(handler);
   }



   /*******************************************************************************
    ** Connect an instance to its transport (idempotent). Call at startup, so this
    ** node receives events before it publishes any.
    *******************************************************************************/
   public static void connect(QInstance instance)
   {
      if(instance != null)
      {
         connection(instance);
      }
   }



   /*******************************************************************************
    ** Publish an event: handled on this node now, and sent to the others.
    *******************************************************************************/
   public static void publish(QInstance instance, ClusterEvent event)
   {
      if(event == null)
      {
         return;
      }
      event.setOriginNodeId(QNodeRoles.nodeId());
      dispatch(instance, event);
      if(instance == null)
      {
         return;
      }

      try
      {
         connection(instance).transport().publish(event);
      }
      catch(Exception e)
      {
         LOG.warn("Could not send a cluster event; other nodes rely on cache TTLs", e, logPair("event", event.toString()));
      }
   }



   /*******************************************************************************
    ** Disconnect an instance (shutdown, tests).
    *******************************************************************************/
   public static void close(QInstance instance)
   {
      Connection connection;
      synchronized(CONNECTIONS)
      {
         connection = CONNECTIONS.remove(instance);
      }
      if(connection != null)
      {
         try
         {
            connection.subscription().close();
            if(connection.transport() instanceof AutoCloseable closeable)
            {
               closeable.close();
            }
         }
         catch(Exception e)
         {
            LOG.debug("Error closing the cluster bus", e);
         }
      }
   }



   /*******************************************************************************
    ** The instance's connection (created on first use).
    *******************************************************************************/
   private static Connection connection(QInstance instance)
   {
      synchronized(CONNECTIONS)
      {
         return (CONNECTIONS.computeIfAbsent(instance, key ->
         {
            ClusterBusInterface transport = buildTransport(key);

            ///////////////////////////////////////////////////////////////////
            // the map is weak on the instance: the subscription must not   //
            // hold it strongly, or no instance would ever be collected     //
            ///////////////////////////////////////////////////////////////////
            WeakReference<QInstance> weakInstance = new WeakReference<>(key);
            AutoCloseable subscription = transport.subscribe(event ->
            {
               QInstance target = weakInstance.get();
               if(target == null || event == null || QNodeRoles.nodeId().equals(event.getOriginNodeId()))
               {
                  return;
               }
               dispatch(target, event);
            });
            return (new Connection(transport, subscription));
         }));
      }
   }



   /*******************************************************************************
    ** The configured transport, else in-memory.
    *******************************************************************************/
   private static ClusterBusInterface buildTransport(QInstance instance)
   {
      ClusterBusMetaData metaData = ClusterBusMetaData.of(instance);
      if(metaData == null || metaData.getBus() == null)
      {
         return (new InMemoryClusterBus());
      }
      try
      {
         ClusterBusInterface[] result = new ClusterBusInterface[1];
         QContext.withTemporaryContext(new CapturedContext(instance, null), () -> result[0] = QCodeLoader.getAdHoc(ClusterBusInterface.class, metaData.getBus()));
         return (result[0]);
      }
      catch(Exception e)
      {
         LOG.error("Could not load the cluster bus; events stay on this node", e);
         return (new InMemoryClusterBus());
      }
   }



   /*******************************************************************************
    ** Run the handlers for an event, each isolated from the others.
    *******************************************************************************/
   static void dispatch(QInstance instance, ClusterEvent event)
   {
      List<BiConsumer<QInstance, ClusterEvent>> handlers = HANDLERS.get(event.getType());
      if(handlers == null)
      {
         return;
      }
      for(BiConsumer<QInstance, ClusterEvent> handler : handlers)
      {
         try
         {
            if(instance == null)
            {
               handler.accept(null, event);
            }
            else
            {
               QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession(instance)), () -> handler.accept(instance, event));
            }
         }
         catch(Exception e)
         {
            LOG.warn("Cluster event handler failed", e, logPair("event", event.toString()));
         }
      }
   }
}
