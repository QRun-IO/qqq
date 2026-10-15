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

package com.kingsrook.qqq.esb.publish;


import java.util.List;
import java.util.Locale;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.stats.EsbStats;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Sends ESB events to a destination (spec section 5): one PERSISTENT JMS
 * TextMessage per event, all on one session.
 *
 * Each publish call leases one transacted session on the destination's provider
 * (whose connection EsbConnectionManager keeps open), sends every event, and
 * commits once.  So a call sends all of its events or none of them, and a bulk
 * write's thousands of messages wait on the broker once, at the commit, rather
 * than once per message.
 *
 * publish never throws.  A failure (unknown destination, no QInstance in
 * context, broker down or its client missing, an event that can't be written)
 * is logged, counted once per event with EsbStats.publishFailed, and returned
 * in the output - so a failed publish can't fail the record write or process
 * that asked for it.  Sent messages are counted with EsbStats.published.
 *
 * A send that failed because of a failover or a stale session (a commit
 * rolled back or left in doubt by a failover, or a session or connection that
 * is no longer usable) is retried once, on a fresh session, if the provider
 * is connected.  Other failures (e.g., the broker rejected the batch) are not.  That can deliver a message twice if the first commit reached the
 * broker; every message carries a stable qqq_message_id, so consumers dedupe.
 *
 * sendOutbox is the outbox relay's path: it throws instead, so the relay can
 * retry the rows.
 *
 * Destinations are looked up (and a provider's first connection is made) from
 * the QContext's instance, so call it with a QContext.
 *******************************************************************************/
public final class EsbPublisher
{
   private static final QLogger LOG = QLogger.getLogger(EsbPublisher.class);

   private static final EsbPublisher INSTANCE = new EsbPublisher();



   /*******************************************************************************
    ** Singleton - use getInstance.
    *******************************************************************************/
   private EsbPublisher()
   {
   }



   /*******************************************************************************
    ** Get the singleton instance.
    *******************************************************************************/
   public static EsbPublisher getInstance()
   {
      return (INSTANCE);
   }



   /*******************************************************************************
    ** Send the events, in order, to the destination (by its QQQ name).  No events
    ** (null or empty) is a success that sends nothing.  Never throws - see the
    ** class comment.
    *******************************************************************************/
   public EsbPublishOutput publish(String destinationName, List<EsbEvent> events)
   {
      if(CollectionUtils.nullSafeIsEmpty(events))
      {
         return (new EsbPublishOutput().withSuccess(true).withSent(0));
      }

      try
      {
         send(getDestination(destinationName), events, null);
      }
      catch(Exception | LinkageError e)
      {
         if(!isRetryable(destinationName, e))
         {
            return (failed(destinationName, events.size(), e));
         }
         try
         {
            LOG.info("Retrying an ESB publish on a fresh session", logPair("destinationName", destinationName), logPair("error", getErrorText(e)));
            send(getDestination(destinationName), events, null);
         }
         catch(Exception | LinkageError retryException)
         {
            return (failed(destinationName, events.size(), retryException));
         }
      }

      for(int i = 0; i < events.size(); i++)
      {
         EsbStats.getInstance().published(destinationName);
      }

      return (new EsbPublishOutput().withSuccess(true).withSent(events.size()));
   }



   /*******************************************************************************
    ** Send the events, in order, in one broker transaction, each with its
    ** message id (index-aligned) as its qqq_message_id - for the outbox relay.
    ** Throws on any failure (nothing is then sent); does not count.
    *******************************************************************************/
   public void sendOutbox(String destinationName, List<EsbEvent> events, List<String> messageIds) throws QException, JMSException
   {
      send(getDestination(destinationName), events, messageIds);
   }



   /*******************************************************************************
    ** Whether a failed publish is worth one retry: it failed because of a
    ** failover or a stale session (see the class comment), and the provider is
    ** connected now.
    *******************************************************************************/
   static boolean isRetryable(String destinationName, Throwable throwable)
   {
      if(!isFailoverOrStaleSession(throwable))
      {
         return (false);
      }
      try
      {
         return (EsbConnectionManager.getInstance().isConnected(getDestination(destinationName).getProviderName()));
      }
      catch(Exception e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    ** Whether a failure (or one of its causes) is a session or connection that is
    ** no longer usable, or a transaction rolled back - or left in doubt - by a
    ** failover.
    *******************************************************************************/
   static boolean isFailoverOrStaleSession(Throwable throwable)
   {
      for(Throwable cause = throwable; cause != null; cause = (cause.getCause() == cause ? null : cause.getCause()))
      {
         if(cause instanceof jakarta.jms.IllegalStateException)
         {
            return (true);
         }
         String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(Locale.ROOT);
         if(message.contains("failover") || message.contains("outcome unknown") || message.contains("no longer current"))
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** Log, and count, a publish of eventCount events to the destination that
    ** failed (before, or while, sending) - and make its output.
    *******************************************************************************/
   EsbPublishOutput failed(String destinationName, Integer eventCount, Throwable throwable)
   {
      String errorText = getErrorText(throwable);
      LOG.warn("Could not publish ESB events", throwable, logPair("destinationName", destinationName), logPair("eventCount", eventCount));

      ///////////////////////////////////////////////////////////////////////
      // counters take an Exception; a LinkageError (e.g., a broker client //
      // that's only partly on the classpath) is counted with its text     //
      ///////////////////////////////////////////////////////////////////////
      Exception exception = (throwable instanceof Exception e) ? e : new QException(errorText, throwable);
      for(int i = 0; i < eventCount; i++)
      {
         EsbStats.getInstance().publishFailed(destinationName, exception);
      }

      return (new EsbPublishOutput().withSuccess(false).withSent(0).withError(errorText));
   }



   /*******************************************************************************
    ** The instance name (for event sources) from the QContext's instance's ESB
    ** meta-data - or null if there is none.
    *******************************************************************************/
   static String getInstanceNameFromContext()
   {
      QInstance qInstance = QContext.getQInstance();
      if(qInstance == null)
      {
         return (null);
      }

      EsbInstanceMetaData esbInstanceMetaData = QSupplementalInstanceMetaData.of(qInstance, EsbInstanceMetaData.NAME);
      return (esbInstanceMetaData == null ? null : esbInstanceMetaData.getInstanceName());
   }



   /*******************************************************************************
    ** Send on an exclusive pooled session. Close the producer before committing;
    ** any send, producer cleanup, or commit failure discards the whole lease.
    *******************************************************************************/
   private static void send(QEsbDestinationMetaData destination, List<EsbEvent> events, List<String> messageIds) throws QException, JMSException
   {
      EsbConnectionManager connectionManager = EsbConnectionManager.getInstance();
      try(EsbConnectionManager.PublishingSessionLease lease = connectionManager.borrowPublishingSession(destination.getProviderName()))
      {
         Session session = lease.getSession();
         try(MessageProducer producer = session.createProducer(connectionManager.resolve(session, destination)))
         {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            for(int i = 0; i < events.size(); i++)
            {
               String messageId = (messageIds == null) ? null : messageIds.get(i);
               producer.send(EsbEventCodec.toMessage(session, events.get(i), messageId));
            }
         }
         lease.commit();
      }
   }



   /*******************************************************************************
    ** Look up a destination in the QContext's instance.
    *******************************************************************************/
   private static QEsbDestinationMetaData getDestination(String destinationName) throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      if(qInstance == null)
      {
         throw (new QException("There is no QInstance in context, to publish to ESB destination " + destinationName));
      }

      EsbInstanceMetaData     esbInstanceMetaData = QSupplementalInstanceMetaData.of(qInstance, EsbInstanceMetaData.NAME);
      QEsbDestinationMetaData destination         = esbInstanceMetaData == null ? null : esbInstanceMetaData.getDestination(destinationName);
      if(destination == null)
      {
         throw (new QException("Unknown ESB destination: " + destinationName));
      }

      return (destination);
   }



   /*******************************************************************************
    ** The failure's message, or its class name if it has none.
    *******************************************************************************/
   static String getErrorText(Throwable throwable)
   {
      return (StringUtils.hasContent(throwable.getMessage()) ? throwable.getMessage() : throwable.getClass().getSimpleName());
   }

}
