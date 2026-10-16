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

package com.kingsrook.qqq.esb.cluster;


import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.cluster.ClusterBusInterface;
import com.kingsrook.qqq.backend.core.cluster.ClusterEvent;
import com.kingsrook.qqq.backend.core.cluster.InMemoryClusterBus;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.connection.EsbListenerDispatcher;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;


/*******************************************************************************
 ** Cluster control events (core ClusterBus) over an ESB topic,
 ** qqq.cluster.control: every node publishes and subscribes (non-durable,
 ** non-shared, so each node gets each event; non-persistent with a 2 minute
 ** time-to-live - a lost event is covered by cache TTLs). Configure with
 ** ClusterBusMetaData.withBus(new QCodeReference(EsbClusterBus.class)). The
 ** topic is declared automatically when the instance has a single ESB
 ** provider; with several, declare it (type TOPIC) with its provider.
 *******************************************************************************/
public class EsbClusterBus implements ClusterBusInterface, AutoCloseable
{
   private static final QLogger LOG = QLogger.getLogger(EsbClusterBus.class);

   /** the topic every node publishes and subscribes to */
   public static final String DESTINATION_NAME = "qqq.cluster.control";
   private final QInstance instance;
   private final QEsbDestinationMetaData destination;
   private final InMemoryClusterBus local = new InMemoryClusterBus();
   private final Thread worker;
   private final EsbListenerDispatcher dispatcher;
   private volatile boolean closed;
   private Session session;



   /*******************************************************************************
    ** Code-reference constructor, initialized with the owning instance in context.
    *******************************************************************************/
   public EsbClusterBus()
   {
      this(QContext.getQInstance());
   }



   /*******************************************************************************
    ** Resolve a declared topic, or the default topic on a single configured provider.
    *******************************************************************************/
   public EsbClusterBus(QInstance instance)
   {
      this.instance = instance;
      EsbInstanceMetaData metadata = EsbInstanceMetaData.of(instance);
      QEsbDestinationMetaData configured = metadata.getDestination(DESTINATION_NAME);
      if(configured == null)
      {
         if(metadata.getProviders().size() != 1)
         {
            throw (new IllegalArgumentException("Declare the cluster control topic (" + DESTINATION_NAME + ") with its provider when using multiple ESB providers"));
         }
         configured = new QEsbDestinationMetaData().withName(DESTINATION_NAME).withType(EsbDestinationType.TOPIC)
            .withProviderName(metadata.getProviders().keySet().iterator().next());
      }
      if(configured.getType() != EsbDestinationType.TOPIC)
      {
         throw (new IllegalArgumentException("The cluster control destination must be a topic"));
      }
      destination = configured;
      EsbConnectionManager.getInstance().addConnectionListener(destination.getProviderName(), this::invalidate);
      dispatcher = new EsbListenerDispatcher("qqq-cluster-esb-dispatch", instance);
      worker = Thread.ofVirtual().name("qqq-cluster-esb").unstarted(this::listen);
      worker.start();
   }



   /*******************************************************************************
    ** Use a non-transacted, auto-ack session like the control topic. Never fail a caller.
    *******************************************************************************/
   @Override
   public void publish(ClusterEvent message)
   {
      if(closed)
      {
         return;
      }
      try
      {
         QContext.withTemporaryContext(new CapturedContext(instance, null), () ->
         {
            EsbConnectionManager manager = EsbConnectionManager.getInstance();
            try(Session publishing = manager.openSession(destination.getProviderName(), false);
               MessageProducer producer = publishing.createProducer(manager.resolve(publishing, destination)))
            {
               producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
               producer.setTimeToLive(120000);
               producer.send(publishing.createTextMessage(JsonUtils.toJson(message)));
            }
         });
      }
      catch(Exception e)
      {
         LOG.warn("Could not publish a cluster event", e);
      }
   }



   /*******************************************************************************
    ** Subscribe locally; the broker sends each message to every pod, including its producer.
    *******************************************************************************/
   @Override
   public AutoCloseable subscribe(Consumer<ClusterEvent> listener)
   {
      return (local.subscribe(listener));
   }



   /*******************************************************************************
    ** Retry failed setup and rebuild consumers after the manager reconnects.
    *******************************************************************************/
   private void listen()
   {
      try
      {
         QContext.init(instance, null);
         while(!closed)
         {
            if(!isListening())
            {
               openConsumer();
            }
            Thread.sleep(1000);
         }
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
      finally
      {
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Readiness is also useful for hosts starting an HTTP server after the bus.
    *******************************************************************************/
   public synchronized boolean isListening()
   {
      return (!closed && session != null && EsbConnectionManager.getInstance().isConnected(destination.getProviderName()));
   }



   /*******************************************************************************
    ** No subscription name, durable consumer, or shared consumer: every pod gets every hint.
    *******************************************************************************/
   private void openConsumer()
   {
      Session opened = null;
      try
      {
         EsbConnectionManager manager = EsbConnectionManager.getInstance();
         synchronized(this)
         {
            if(closed)
            {
               return;
            }
            opened = manager.openSession(destination.getProviderName(), false);
         }
         opened.createConsumer(manager.resolve(opened, destination)).setMessageListener(this::onMessage);
         Session previous;
         synchronized(this)
         {
            previous = closed ? opened : session;
            if(!closed)
            {
               session = opened;
            }
         }
         closeSession(previous);
      }
      catch(Exception e)
      {
         closeSession(opened);
         LOG.debug("Cluster control topic not available yet; retrying", e);
      }
   }



   /*******************************************************************************
    ** Malformed messages are acknowledged and dropped; subscribers run on the
    ** dispatcher, off the JMS thread (they may publish, which opens and closes a
    ** session - refused on a listener thread), in order, isolated from each other.
    *******************************************************************************/
   private void onMessage(Message message)
   {
      try
      {
         if(message instanceof TextMessage text)
         {
            ClusterEvent event = JsonUtils.toObject(text.getText(), ClusterEvent.class);
            dispatcher.dispatch(() -> local.publish(event));
         }
      }
      catch(Exception e)
      {
         LOG.warn("Ignoring an invalid cluster event", e);
      }
   }



   /*******************************************************************************
    ** Close sessions outside the setup monitor, as in the control topic.
    *******************************************************************************/
   private void invalidate()
   {
      Session previous;
      synchronized(this)
      {
         previous = session;
         session = null;
      }
      closeSession(previous);
   }



   /*******************************************************************************
    ** Closing a failed JMS session is best-effort.
    *******************************************************************************/
   private void closeSession(Session previous)
   {
      if(previous != null)
      {
         try
         {
            previous.close();
         }
         catch(Exception e)
         {
            LOG.debug("Could not close the cluster JMS session", e);
         }
      }
   }



   /*******************************************************************************
    ** Interrupt setup before taking its lock; the owner retains provider connections.
    *******************************************************************************/
   @Override
   public void close()
   {
      closed = true;
      worker.interrupt();
      invalidate();
      dispatcher.close();
   }
}

