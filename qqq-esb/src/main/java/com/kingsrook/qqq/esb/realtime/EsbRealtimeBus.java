/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.realtime;


import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.realtime.InMemoryRealtimeBus;
import com.kingsrook.qqq.backend.core.realtime.RealtimeBusInterface;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMessage;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;


/*******************************************************************************
 ** Non-durable, non-shared topic fan-out using the ESB provider connection manager.
 *******************************************************************************/
public class EsbRealtimeBus implements RealtimeBusInterface, AutoCloseable
{
   private static final QLogger LOG = QLogger.getLogger(EsbRealtimeBus.class);
   private final QInstance instance;
   private final QEsbDestinationMetaData destination;
   private final InMemoryRealtimeBus local = new InMemoryRealtimeBus();
   private final Thread worker;
   private volatile boolean closed;
   private Session session;



   /*******************************************************************************
    ** Code-reference constructor, initialized with the owning instance in context.
    *******************************************************************************/
   public EsbRealtimeBus()
   {
      this(QContext.getQInstance());
   }



   /*******************************************************************************
    ** Resolve a declared topic, or the default topic on a single configured provider.
    *******************************************************************************/
   public EsbRealtimeBus(QInstance instance)
   {
      this.instance = instance;
      EsbInstanceMetaData metadata = EsbInstanceMetaData.of(instance);
      QEsbDestinationMetaData configured = metadata.getDestination(metadata.getRealtimeDestinationName());
      if(configured == null)
      {
         if(metadata.getProviders().size() != 1)
         {
            throw (new IllegalArgumentException("Declare the realtime topic with its provider when using multiple ESB providers"));
         }
         configured = new QEsbDestinationMetaData().withName(metadata.getRealtimeDestinationName()).withType(EsbDestinationType.TOPIC)
            .withProviderName(metadata.getProviders().keySet().iterator().next());
      }
      if(configured.getType() != EsbDestinationType.TOPIC)
      {
         throw (new IllegalArgumentException("Realtime destination must be a topic"));
      }
      destination = configured;
      EsbConnectionManager.getInstance().addConnectionListener(destination.getProviderName(), this::invalidate);
      worker = Thread.ofVirtual().name("qqq-realtime-esb").unstarted(this::listen);
      worker.start();
   }



   /*******************************************************************************
    ** Use a non-transacted, auto-ack session like the control topic. Never fail a caller.
    *******************************************************************************/
   @Override
   public void publish(RealtimeMessage message)
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
         LOG.warn("Could not publish realtime hint", e);
      }
   }



   /*******************************************************************************
    ** Subscribe locally; the broker sends each message to every pod, including its producer.
    *******************************************************************************/
   @Override
   public AutoCloseable subscribe(Consumer<RealtimeMessage> listener)
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
         LOG.debug("Realtime topic not available yet; retrying", e);
      }
   }



   /*******************************************************************************
    ** Malformed messages and listener failures are acknowledged and isolated.
    *******************************************************************************/
   private void onMessage(Message message)
   {
      try
      {
         if(message instanceof TextMessage text)
         {
            local.publish(JsonUtils.toObject(text.getText(), RealtimeMessage.class));
         }
      }
      catch(Exception e)
      {
         LOG.warn("Ignoring invalid realtime message", e);
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
            LOG.debug("Could not close realtime JMS session", e);
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
      local.close();
   }
}

