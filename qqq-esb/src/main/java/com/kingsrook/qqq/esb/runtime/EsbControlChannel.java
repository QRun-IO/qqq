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


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
 * if the provider can't be reached, QEsbRuntime's connection listener calls
 * onReconnect when it can, which opens the listener again (also after a lost
 * connection).  Messages arrive on the broker client's threads.
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
      listenInBackground(providerName);
   }



   /*******************************************************************************
    ** Stop listening, on every provider.
    *******************************************************************************/
   void close()
   {
      Map<String, Session> sessionsToClose;
      synchronized(this)
      {
         open = false;
         sessionsToClose = Map.copyOf(sessions);
         sessions.clear();
      }
      sessionsToClose.values().forEach(EsbControlChannel::closeQuietly);
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
    ** Open the provider's listener on a new virtual thread (whose QContext holds
    ** the instance, for the connection manager).
    *******************************************************************************/
   private void listenInBackground(String providerName)
   {
      Thread.ofVirtual().name("qqq-esb-control-" + providerName).start(() ->
      {
         try
         {
            QContext.init(qInstance, null);
            listen(providerName);
         }
         finally
         {
            QContext.clear();
         }
      });
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
         newSession = manager.openSession(providerName, false);
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
