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


import java.util.List;
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
 * Each publish call opens one transacted session on the destination's provider
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
         send(getDestination(destinationName), events);
      }
      catch(Exception | LinkageError e)
      {
         return (failed(destinationName, events.size(), e));
      }

      for(int i = 0; i < events.size(); i++)
      {
         EsbStats.getInstance().published(destinationName);
      }

      return (new EsbPublishOutput().withSuccess(true).withSent(events.size()));
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
    ** Send the events on one transacted session, committed once.  Closing the
    ** session without a commit (after a failed send) rolls back what was sent.
    *******************************************************************************/
   private static void send(QEsbDestinationMetaData destination, List<EsbEvent> events) throws QException, JMSException
   {
      EsbConnectionManager connectionManager = EsbConnectionManager.getInstance();
      Session              session           = connectionManager.openSession(destination.getProviderName(), true);
      try
      {
         MessageProducer producer = session.createProducer(connectionManager.resolve(session, destination));
         producer.setDeliveryMode(DeliveryMode.PERSISTENT);
         for(EsbEvent event : events)
         {
            producer.send(EsbEventCodec.toMessage(session, event));
         }
         session.commit();
      }
      finally
      {
         closeQuietly(session);
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
    ** Close a session, after it has committed (or failed) - logging, rather than
    ** throwing, if closing fails, since by then the outcome is decided.
    *******************************************************************************/
   private static void closeQuietly(Session session)
   {
      try
      {
         session.close();
      }
      catch(Exception e)
      {
         LOG.debug("Error closing an ESB publishing session", e);
      }
   }



   /*******************************************************************************
    ** The failure's message, or its class name if it has none.
    *******************************************************************************/
   static String getErrorText(Throwable throwable)
   {
      return (StringUtils.hasContent(throwable.getMessage()) ? throwable.getMessage() : throwable.getClass().getSimpleName());
   }

}
