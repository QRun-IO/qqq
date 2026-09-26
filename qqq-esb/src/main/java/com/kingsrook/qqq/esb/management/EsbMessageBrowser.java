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

package com.kingsrook.qqq.esb.management;


import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbUnparseableMessageException;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;


/*******************************************************************************
 * Browses the messages on a broker queue (including dead-letter and topic
 * subscription queues) with a JMS QueueBrowser - so on every broker, with or
 * without a management API.  Browsing leaves the messages on the queue.
 *
 * Needs a QContext with the provider's instance (it opens a session through
 * EsbConnectionManager).
 *******************************************************************************/
public final class EsbMessageBrowser
{
   public static final Integer MAX_RAW_BODY_LENGTH = 10_000;
   public static final Integer MAX_LIMIT           = 1000;

   private static final QLogger LOG = QLogger.getLogger(EsbMessageBrowser.class);

   ///////////////////////////////////////////////////////////////////////
   // the most bytes read from a bytes message's body: a UTF-8 character //
   // is at most 4 bytes, so this always holds MAX_RAW_BODY_LENGTH chars  //
   ///////////////////////////////////////////////////////////////////////
   private static final Integer MAX_RAW_BODY_BYTES = MAX_RAW_BODY_LENGTH * 4;

   private static final String PROPERTY_DELIVERY_COUNT = "JMSXDeliveryCount";



   /*******************************************************************************
    ** Utility class - static methods only.
    *******************************************************************************/
   private EsbMessageBrowser()
   {
   }



   /*******************************************************************************
    ** Up to limit messages from a queue, skipping the first offset (counting
    ** from the head of the queue, as the broker orders it).  Messages being
    ** delivered to a consumer at that moment aren't shown.  To tell whether
    ** there are more after a page, ask for one more than you show.
    **
    ** Throws QUserFacingException for a negative offset, or a limit outside 0 to
    ** MAX_LIMIT; QException if the provider is unknown, or its broker can't be
    ** reached or browsed.
    *******************************************************************************/
   public static List<EsbBrowsedMessage> browse(String providerName, String brokerQueueName, int offset, int limit) throws QException
   {
      if(offset < 0)
      {
         throw (new QUserFacingException("The offset for browsing messages must be 0 or more"));
      }

      if(limit < 0 || limit > MAX_LIMIT)
      {
         throw (new QUserFacingException("The limit for browsing messages must be from 0 to " + MAX_LIMIT));
      }

      if(!StringUtils.hasContent(brokerQueueName))
      {
         throw (new QException("A queue name is required, to browse messages"));
      }

      EsbConnectionManager connectionManager = EsbConnectionManager.getInstance();
      Session              session           = connectionManager.openSession(providerName, false);
      try
      {
         Queue queue = connectionManager.resolveQueue(session, providerName, brokerQueueName);
         try(QueueBrowser browser = session.createBrowser(queue))
         {
            List<EsbBrowsedMessage> browsedMessages = new ArrayList<>();
            Enumeration<?>          enumeration     = browser.getEnumeration();
            Integer                 position        = 0;
            while(browsedMessages.size() < limit && enumeration.hasMoreElements())
            {
               Message message = (Message) enumeration.nextElement();
               if(position >= offset)
               {
                  browsedMessages.add(toBrowsedMessage(message));
               }
               position++;
            }
            return (browsedMessages);
         }
      }
      catch(JMSException | RuntimeException e)
      {
         throw (new QException("Could not browse queue " + brokerQueueName + " on ESB provider " + providerName, e));
      }
      finally
      {
         closeQuietly(session);
      }
   }



   /*******************************************************************************
    ** One browsed message, in the shape EsbBrowsedMessage documents.
    *******************************************************************************/
   private static EsbBrowsedMessage toBrowsedMessage(Message message) throws JMSException
   {
      Long timestamp = message.getJMSTimestamp();

      return (new EsbBrowsedMessage()
         .withMessageId(message.getJMSMessageID())
         .withTimestamp(timestamp > 0 ? Instant.ofEpochMilli(timestamp) : null)
         .withDeliveryCount(message.getObjectProperty(PROPERTY_DELIVERY_COUNT) instanceof Number deliveryCount ? deliveryCount.intValue() : null)
         .withEvent(readEvent(message))
         .withRawBody(truncate(readBody(message)))
         .withProperties(readProperties(message)));
   }



   /*******************************************************************************
    ** The CloudEvent in a message - or null if it isn't one (read as a trigger
    ** would: only a text message holds an event).
    *******************************************************************************/
   private static EsbEvent readEvent(Message message) throws JMSException
   {
      try
      {
         return (EsbEventCodec.fromMessage(message));
      }
      catch(EsbUnparseableMessageException e)
      {
         return (null);
      }
   }



   /*******************************************************************************
    ** A message's body as text: a text message's text, or a bytes message's
    ** bytes (the first MAX_RAW_BODY_BYTES, anyway) as UTF-8; empty for any other
    ** kind of message, or no body.
    *******************************************************************************/
   private static String readBody(Message message) throws JMSException
   {
      if(message instanceof TextMessage textMessage)
      {
         return (Objects.requireNonNullElse(textMessage.getText(), ""));
      }

      if(message instanceof BytesMessage bytesMessage)
      {
         byte[] bytes = new byte[(int) Math.min(bytesMessage.getBodyLength(), MAX_RAW_BODY_BYTES)];
         bytesMessage.readBytes(bytes);
         return (new String(bytes, StandardCharsets.UTF_8));
      }

      return ("");
   }



   /*******************************************************************************
    ** Cut a body to MAX_RAW_BODY_LENGTH characters - one fewer, rather than split
    ** a surrogate pair (which would not be valid text, e.g., in JSON).
    *******************************************************************************/
   private static String truncate(String body)
   {
      if(body.length() <= MAX_RAW_BODY_LENGTH)
      {
         return (body);
      }

      Integer end = Character.isHighSurrogate(body.charAt(MAX_RAW_BODY_LENGTH - 1)) ? MAX_RAW_BODY_LENGTH - 1 : MAX_RAW_BODY_LENGTH;
      return (body.substring(0, end));
   }



   /*******************************************************************************
    ** A message's JMS properties, by name, with their JMS types (String,
    ** Integer, Long, Boolean, etc.).
    *******************************************************************************/
   private static Map<String, Serializable> readProperties(Message message) throws JMSException
   {
      Map<String, Serializable> properties = new TreeMap<>();

      Enumeration<?> propertyNames = message.getPropertyNames();
      while(propertyNames.hasMoreElements())
      {
         String propertyName = (String) propertyNames.nextElement();
         if(message.getObjectProperty(propertyName) instanceof Serializable value)
         {
            properties.put(propertyName, value);
         }
      }

      return (properties);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void closeQuietly(Session session)
   {
      try
      {
         session.close();
      }
      catch(Exception e)
      {
         LOG.debug("Error closing an ESB browsing session", e);
      }
   }

}
