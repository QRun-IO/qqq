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

package com.kingsrook.qqq.esb.runtime;


import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * A runtime's listener on the ESB control topic (spec section 6): the internal,
 * non-durable topic qqq.esb.control, on each provider that the runtime's
 * triggers use.  EsbTriggerControl sends { "action", "triggerName" } messages
 * to it, and each runtime listening applies them to its own runner for that
 * trigger (pauseLocal, resumeLocal, restartLocal) - so an action reaches every
 * node on the broker, including the sender's.  A runtime without the trigger
 * ignores the message.
 *
 * The topic is non-durable, and nothing is persisted: a node that isn't
 * listening when a message is sent (stopped, or disconnected) never gets it,
 * and a node that starts later uses startPaused.
 *
 * Opening a listener never blocks the caller: it runs on a virtual thread, and
 * if the provider can't be reached or listener setup fails, that thread keeps
 * trying.  QEsbRuntime's connection listener calls onReconnect after a lost
 * connection, which discards the old session so the thread rebuilds it.
 * Messages arrive on the broker client's threads.
 *******************************************************************************/
public class EsbControlChannel
{
   private static final QLogger LOG = QLogger.getLogger(EsbControlChannel.class);

   public static final String CONTROL_TOPIC_NAME = "qqq.esb.control";

   static final String FIELD_ACTION       = "action";
   static final String FIELD_TRIGGER_NAME = "triggerName";



   /***************************************************************************
    * What a control message asks each node to do to its runner for a trigger.
    ***************************************************************************/
   public enum Action
   {
      PAUSE,
      RESUME,
      RESTART
   }



   private final QEsbRuntime runtime;
   private final QInstance   qInstance;

   ////////////////////////////////////////////////////////////////////
   // guarded by this: the listening session for each provider, and  //
   // whether the channel is open (sessions opened after close are   //
   // closed at once)                                                //
   ////////////////////////////////////////////////////////////////////
   private final Map<String, Session> sessions = new HashMap<>();
   private       Boolean              open     = true;
   private final List<Thread>          workers  = new CopyOnWriteArrayList<>();



   /*******************************************************************************
    ** Constructor - for a runtime, whose runners the messages act on, and the
    ** (validated) instance it was started with.
    *******************************************************************************/
   EsbControlChannel(QEsbRuntime runtime, QInstance qInstance)
   {
      this.runtime = runtime;
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    ** Start listening on each of the providers.  Doesn't block.
    *******************************************************************************/
   void start(Set<String> providerNames)
   {
      providerNames.forEach(this::listenInBackground);
   }



   /*******************************************************************************
    ** The provider (re)connected: listen on it again.  Doesn't block.
    *******************************************************************************/
   void onReconnect(String providerName)
   {
      Session oldSession;
      synchronized(this)
      {
         oldSession = sessions.remove(providerName);
      }
      closeQuietly(oldSession);
   }



   /*******************************************************************************
    ** Stop setup before the application owner closes provider connections. Interrupt
    ** before acquiring the setup lock so a native session open can be cancelled.
    ** Never close JMS sessions or join workers while holding the channel lock.
    *******************************************************************************/
   void close()
   {
      workers.forEach(Thread::interrupt);
      Map<String, Session> sessionsToClose;
      List<Thread> workersToStop;
      synchronized(this)
      {
         open = false;
         sessionsToClose = Map.copyOf(sessions);
         sessions.clear();
         workersToStop = List.copyOf(workers);
      }
      workersToStop.forEach(Thread::interrupt);
      sessionsToClose.values().forEach(EsbControlChannel::closeQuietly);
      long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
      boolean interrupted = false;
      for(Thread worker : workersToStop)
      {
         while(worker != Thread.currentThread() && worker.isAlive())
         {
            long remaining = deadline - System.nanoTime();
            if(remaining <= 0)
            {
               LOG.warn("ESB control worker did not finish before shutdown deadline", logPair("worker", worker.getName()));
               break;
            }
            try
            {
               worker.join(Duration.ofNanos(remaining));
            }
            catch(InterruptedException e)
            {
               interrupted = true;
            }
         }
      }
      if(interrupted)
      {
         Thread.currentThread().interrupt();
      }
   }



   /*******************************************************************************
    ** Whether the channel has a listener on the provider (as far as it knows: a
    ** lost connection shows once the provider reconnects).
    *******************************************************************************/
   synchronized Boolean isListening(String providerName)
   {
      return (open && sessions.containsKey(providerName) && EsbConnectionManager.getInstance().isConnected(providerName));
   }



   /*******************************************************************************
    ** Send a control message to the control topic on a provider, for every node
    ** listening there.  Needs a QContext with the provider's instance.
    *******************************************************************************/
   static void send(String providerName, Action action, String triggerName) throws QException
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();
      try(Session session = manager.openSession(providerName, false))
      {
         MessageProducer producer = session.createProducer(manager.resolve(session, controlDestination(providerName)));
         producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
         producer.send(session.createTextMessage(JsonUtils.toJson(Map.of(FIELD_ACTION, action.name(), FIELD_TRIGGER_NAME, triggerName))));
      }
      catch(JMSException e)
      {
         throw (new QException("Error sending an ESB control message", e));
      }
   }



   /*******************************************************************************
    ** The control topic, as an ESB destination on a provider.
    *******************************************************************************/
   static QEsbDestinationMetaData controlDestination(String providerName)
   {
      return (new QEsbDestinationMetaData()
         .withName(CONTROL_TOPIC_NAME)
         .withType(EsbDestinationType.TOPIC)
         .withProviderName(providerName));
   }



   /*******************************************************************************
    ** Keep the provider's listener open on one virtual thread (whose QContext
    ** holds the instance, for the connection manager).  A failed setup or lost
    ** connection is retried without waiting for another reconnect callback.
    *******************************************************************************/
   private synchronized void listenInBackground(String providerName)
   {
      if(!open)
      {
         return;
      }
      Thread worker = Thread.ofVirtual().name("qqq-esb-control-" + providerName).unstarted(() ->
      {
         try
         {
            QContext.init(qInstance, null);
            while(isOpen())
            {
               if(!isListening(providerName))
               {
                  listen(providerName);
               }
               try
               {
                  Thread.sleep(1000);
               }
               catch(InterruptedException e)
               {
                  Thread.currentThread().interrupt();
                  return;
               }
            }
         }
         finally
         {
            QContext.clear();
            workers.remove(Thread.currentThread());
         }
      });
      workers.add(worker);
      worker.start();
   }



   /*******************************************************************************
    ** Whether the channel has not been closed.
    *******************************************************************************/
   private synchronized Boolean isOpen()
   {
      return (open);
   }



   /*******************************************************************************
    ** Open a session and a non-durable consumer on the control topic, replacing
    ** the provider's previous one (if any).
    *******************************************************************************/
   private void listen(String providerName)
   {
      Session newSession = null;
      try
      {
         EsbConnectionManager manager = EsbConnectionManager.getInstance();
         // Admission and provider creation must be atomic with close. The later
         // session installation check alone cannot prevent recreating a provider.
         synchronized(this)
         {
            if(!open)
            {
               return;
            }
            newSession = manager.openSession(providerName, false);
         }
         newSession.createConsumer(manager.resolve(newSession, controlDestination(providerName))).setMessageListener(this::onMessage);
      }
      catch(QException | JMSException | RuntimeException e)
      {
         closeQuietly(newSession);
         if(EsbConnectionManager.getInstance().isConnected(providerName))
         {
            LOG.warn("Could not listen on the ESB control topic though its provider is connected", e, logPair("providerName", providerName));
         }
         else
         {
            LOG.info("Could not listen on the ESB control topic yet; will listen when the provider connects", logPair("providerName", providerName), logPair("error", e.getMessage()));
         }
         return;
      }

      Session sessionToClose;
      synchronized(this)
      {
         sessionToClose = open ? sessions.put(providerName, newSession) : newSession;
      }
      closeQuietly(sessionToClose);
   }



   /*******************************************************************************
    ** Apply a control message to this runtime's runner for its trigger - if the
    ** runtime has one.  Messages it can't use are logged and dropped.
    *******************************************************************************/
   private void onMessage(Message message)
   {
      try
      {
         if(!(message instanceof TextMessage textMessage))
         {
            LOG.warn("Ignoring an ESB control message that isn't text", logPair("messageId", message.getJMSMessageID()));
            return;
         }

         Map<?, ?> json        = JsonUtils.toObject(textMessage.getText(), Map.class);
         Action    action      = Action.valueOf(String.valueOf(json.get(FIELD_ACTION)));
         String    triggerName = (json.get(FIELD_TRIGGER_NAME) instanceof String name) ? name : null;

         EsbTriggerRunner runner = runtime.getRunner(triggerName);
         if(runner == null)
         {
            LOG.debug("Ignoring an ESB control message for a trigger this node doesn't run", logPair("action", action), logPair("triggerName", triggerName));
            return;
         }

         LOG.info("Applying an ESB control message", logPair("action", action), logPair("triggerName", triggerName));
         Runnable apply = switch(action)
         {
            case PAUSE -> runner::pauseLocal;
            case RESUME -> runner::resumeLocal;
            case RESTART -> runner::restartLocal;
         };
         apply.run();
      }
      catch(Exception e)
      {
         LOG.warn("Ignoring an ESB control message that couldn't be applied", logPair("error", e.getMessage()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void closeQuietly(Session session)
   {
      if(session == null)
      {
         return;
      }

      try
      {
         session.close();
      }
      catch(Exception e)
      {
         LOG.debug("Error closing an ESB control session", e);
      }
   }

}
