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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import jakarta.jms.BytesMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for EsbMessageBrowser - against the embedded Artemis broker.
 *******************************************************************************/
class EsbMessageBrowserTest extends EsbTestBase
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      EsbConnectionManager.getInstance().closeAll();
   }



   /*******************************************************************************
    ** Browsing pages through the queue from its head, by offset and limit - and
    ** leaves the messages on the queue.
    *******************************************************************************/
   @Test
   void testBrowsePagesWithOffsetAndLimit() throws Exception
   {
      String         queueName = "esb.test.browse.paging";
      List<EsbEvent> events    = new ArrayList<>();
      for(int i = 0; i < 5; i++)
      {
         events.add(EsbEventFactory.custom("test", "table/order", "qqq.test.browsed", Map.of("n", i)));
      }
      send(queueName, events);

      assertThat(eventIds(EsbMessageBrowser.browse(PROVIDER_NAME, queueName, 0, 2))).containsExactly(events.get(0).getId(), events.get(1).getId());
      assertThat(eventIds(EsbMessageBrowser.browse(PROVIDER_NAME, queueName, 2, 2))).containsExactly(events.get(2).getId(), events.get(3).getId());
      assertThat(eventIds(EsbMessageBrowser.browse(PROVIDER_NAME, queueName, 4, 2))).containsExactly(events.get(4).getId());
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, queueName, 5, 2)).isEmpty();
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, queueName, 0, 0)).isEmpty();

      ////////////////////////////////////////////////
      // browsing doesn't take messages off a queue //
      ////////////////////////////////////////////////
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, queueName, 0, 50)).hasSize(5);
   }



   /*******************************************************************************
    ** A browsed event message has its JMS id, timestamp, delivery count, parsed
    ** event, raw body, and properties (e.g., a dead letter's qqqError).
    *******************************************************************************/
   @Test
   void testBrowsedMessageFields() throws Exception
   {
      String   queueName = "esb.test.browse.fields";
      EsbEvent event     = EsbEventFactory.custom("test", "table/order", "qqq.table.order.inserted", Map.of("record", new HashMap<>(Map.of("id", 1))));

      Instant before = Instant.now().minusSeconds(1);
      Message sent;
      try(Session session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
      {
         Queue   queue   = EsbConnectionManager.getInstance().resolveQueue(session, PROVIDER_NAME, queueName);
         Message message = EsbEventCodec.toMessage(session, event);
         message.setStringProperty("qqqError", "boom");
         message.setIntProperty("qqqAttempts", 3);
         session.createProducer(queue).send(message);
         sent = message;
      }
      Instant after = Instant.now().plusSeconds(1);

      List<EsbBrowsedMessage> browsed = EsbMessageBrowser.browse(PROVIDER_NAME, queueName, 0, 10);
      assertThat(browsed).hasSize(1);

      EsbBrowsedMessage browsedMessage = browsed.get(0);
      assertThat(browsedMessage.getMessageId()).startsWith("ID:").isEqualTo(sent.getJMSMessageID());
      assertThat(browsedMessage.getTimestamp()).isBetween(before, after);
      assertThat(browsedMessage.getDeliveryCount()).as("Artemis counts no deliveries for a message not yet delivered").isZero();
      assertThat(browsedMessage.getRawBody()).isEqualTo(EsbEventCodec.toJson(event));

      assertThat(browsedMessage.getEvent()).isNotNull();
      assertThat(browsedMessage.getEvent().getId()).isEqualTo(event.getId());
      assertThat(browsedMessage.getEvent().getType()).isEqualTo("qqq.table.order.inserted");
      assertThat(browsedMessage.getEvent().getSource()).isEqualTo(event.getSource());

      Map<String, Serializable> properties = browsedMessage.getProperties();
      assertThat(properties).containsEntry("qqqError", "boom");
      assertThat(properties).containsEntry("qqqAttempts", 3);
      assertThat(properties).containsEntry(EsbEventCodec.PROPERTY_ID, event.getId());
      assertThat(properties).containsEntry(EsbEventCodec.PROPERTY_TYPE, event.getType());
      assertThat(properties).containsEntry(EsbEventCodec.PROPERTY_SOURCE, event.getSource());
   }



   /*******************************************************************************
    ** A message that isn't an event (e.g., a poison message) has no event, but
    ** its body is still shown - as text for a bytes message; a message with no
    ** body shows an empty one.
    *******************************************************************************/
   @Test
   void testNonEventMessages() throws Exception
   {
      String queueName = "esb.test.browse.nonEvents";
      try(Session session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
      {
         MessageProducer producer = session.createProducer(EsbConnectionManager.getInstance().resolveQueue(session, PROVIDER_NAME, queueName));
         producer.send(session.createTextMessage("not json"));

         BytesMessage bytesMessage = session.createBytesMessage();
         bytesMessage.writeBytes("{\"specversion\":\"1.0\"} as bytes".getBytes(StandardCharsets.UTF_8));
         producer.send(bytesMessage);

         producer.send(session.createTextMessage(null));
         producer.send(session.createMapMessage());
      }

      List<EsbBrowsedMessage> browsed = EsbMessageBrowser.browse(PROVIDER_NAME, queueName, 0, 10);
      assertThat(browsed).hasSize(4);
      assertThat(browsed).extracting(EsbBrowsedMessage::getEvent).containsOnlyNulls();
      assertThat(browsed).extracting(EsbBrowsedMessage::getRawBody).containsExactly("not json", "{\"specversion\":\"1.0\"} as bytes", "", "");
   }



   /*******************************************************************************
    ** The raw body is cut to 10,000 characters - but the event is read from the
    ** whole body.
    *******************************************************************************/
   @Test
   void testRawBodyIsTruncated() throws Exception
   {
      String   queueName = "esb.test.browse.truncated";
      String   bigText   = "x".repeat(25_000);
      EsbEvent bigEvent  = EsbEventFactory.custom("test", "table/order", "qqq.test.big", Map.of("big", "y".repeat(20_000)));

      try(Session session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
      {
         MessageProducer producer = session.createProducer(EsbConnectionManager.getInstance().resolveQueue(session, PROVIDER_NAME, queueName));
         producer.send(session.createTextMessage(bigText));
         producer.send(EsbEventCodec.toMessage(session, bigEvent));

         BytesMessage bytesMessage = session.createBytesMessage();
         bytesMessage.writeBytes(bigText.getBytes(StandardCharsets.UTF_8));
         producer.send(bytesMessage);
      }

      List<EsbBrowsedMessage> browsed = EsbMessageBrowser.browse(PROVIDER_NAME, queueName, 0, 10);
      assertThat(browsed).hasSize(3);
      assertThat(browsed).extracting(EsbBrowsedMessage::getRawBody).allSatisfy(rawBody -> assertThat(rawBody).hasSize(EsbMessageBrowser.MAX_RAW_BODY_LENGTH));
      assertThat(browsed.get(0).getRawBody()).isEqualTo(bigText.substring(0, EsbMessageBrowser.MAX_RAW_BODY_LENGTH));

      assertThat(browsed.get(1).getEvent()).isNotNull();
      assertThat(browsed.get(1).getEvent().getId()).isEqualTo(bigEvent.getId());
      assertThat((String) browsed.get(1).getEvent().getData().get("big")).hasSize(20_000);
   }



   /*******************************************************************************
    ** A topic subscription's queue is browsed by its broker-side name, from
    ** EsbBrokerNames - showing the messages kept for the subscription.
    *******************************************************************************/
   @Test
   void testBrowseArtemisSubscriptionQueue() throws Exception
   {
      String topicName        = "esb.test.browse.topic";
      String subscriptionName = "syncOrder.browseEvents";

      try(Session session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
      {
         Topic topic = session.createTopic(topicName);

         /////////////////////////////////////////////////////////////////////
         // make the subscription (and so its queue), then close its        //
         // consumer - the queue keeps the messages sent while it is closed //
         /////////////////////////////////////////////////////////////////////
         MessageConsumer consumer = session.createSharedDurableConsumer(topic, subscriptionName);
         consumer.close();

         MessageProducer producer = session.createProducer(topic);
         producer.send(session.createTextMessage("one"));
         producer.send(session.createTextMessage("two"));
      }

      String subscriptionQueue = EsbBrokerNames.subscriptionQueue(EsbProviderType.ACTIVEMQ_ARTEMIS, topicName, subscriptionName);
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, subscriptionQueue, 0, 10))
         .extracting(EsbBrowsedMessage::getRawBody)
         .containsExactly("one", "two");
   }



   /*******************************************************************************
    ** Bad paging arguments are the user's to fix; a missing queue name or an
    ** unknown provider fails too.
    *******************************************************************************/
   @Test
   void testBadArgumentsThrow()
   {
      assertThatThrownBy(() -> EsbMessageBrowser.browse(PROVIDER_NAME, "esb.test.browse.bad", -1, 10)).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> EsbMessageBrowser.browse(PROVIDER_NAME, "esb.test.browse.bad", 0, -1)).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> EsbMessageBrowser.browse(PROVIDER_NAME, "esb.test.browse.bad", 0, EsbMessageBrowser.MAX_LIMIT + 1)).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> EsbMessageBrowser.browse(PROVIDER_NAME, " ", 0, 10)).isInstanceOf(QException.class);
      assertThatThrownBy(() -> EsbMessageBrowser.browse("noSuchProvider", "esb.test.browse.bad", 0, 10))
         .isInstanceOf(QException.class)
         .hasMessageContaining("noSuchProvider");
   }



   /*******************************************************************************
    ** Send events to a queue.
    *******************************************************************************/
   private static void send(String queueName, List<EsbEvent> events) throws Exception
   {
      try(Session session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
      {
         MessageProducer producer = session.createProducer(EsbConnectionManager.getInstance().resolveQueue(session, PROVIDER_NAME, queueName));
         for(EsbEvent event : events)
         {
            producer.send(EsbEventCodec.toMessage(session, event));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> eventIds(List<EsbBrowsedMessage> browsedMessages)
   {
      return (browsedMessages.stream().map(browsedMessage -> browsedMessage.getEvent().getId()).toList());
   }

}
