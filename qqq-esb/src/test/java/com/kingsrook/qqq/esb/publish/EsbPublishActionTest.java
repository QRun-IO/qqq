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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.stats.EsbCounterSnapshot;
import com.kingsrook.qqq.esb.stats.EsbStats;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for EsbPublishAction (explicit publishes) and EsbPublisher -
 ** against the embedded Artemis broker.
 *******************************************************************************/
class EsbPublishActionTest extends EsbTestBase
{
   private static final String INSTANCE_NAME     = "orderService";
   private static final String QUEUE_DESTINATION = "orderQueue";
   private static final String TOPIC_DESTINATION = "orderEvents";
   private static final String CUSTOM_TYPE       = "com.example.order.shipped";

   private static final Long RECEIVE_TIMEOUT_MS          = 5000L;
   private static final Long NOTHING_RECEIVED_TIMEOUT_MS = 500L;

   private String  queueBrokerName;
   private String  topicBrokerName;
   private Session receivingSession;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      EsbStats.getInstance().reset();

      String suffix = UUID.randomUUID().toString();
      queueBrokerName = "esb.test.publishAction.queue." + suffix;
      topicBrokerName = "esb.test.publishAction.topic." + suffix;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach() throws Exception
   {
      receivingSession = null;
      EsbConnectionManager.getInstance().closeAll();
      startEmbeddedBroker();
   }



   /*******************************************************************************
    ** An explicit publish sends one event with the caller's type, data, and
    ** source path, as a PERSISTENT message - and reports it as sent.
    *******************************************************************************/
   @Test
   void testPublishActionSendsCustomType() throws Exception
   {
      setUpInstance(INSTANCE_NAME);
      MessageConsumer consumer = openQueueConsumer();

      LinkedHashMap<String, Serializable> data = new LinkedHashMap<>();
      data.put("orderNo", "ORD-7");
      data.put("lineCount", 3);

      EsbPublishOutput output = new EsbPublishAction().execute(new EsbPublishInput()
         .withDestinationName(QUEUE_DESTINATION)
         .withType(CUSTOM_TYPE)
         .withSourcePath("process/" + PROCESS_NAME_SYNC_ORDER)
         .withData(data));

      assertThat(output.getSuccess()).isTrue();
      assertThat(output.getSent()).isEqualTo(1);
      assertThat(output.getError()).isNull();

      Message message = consumer.receive(RECEIVE_TIMEOUT_MS);
      assertThat(message).isNotNull();
      assertThat(message.getJMSDeliveryMode()).isEqualTo(DeliveryMode.PERSISTENT);
      assertThat(message.getStringProperty(EsbEventCodec.PROPERTY_TYPE)).isEqualTo(CUSTOM_TYPE);
      assertThat(consumer.receive(NOTHING_RECEIVED_TIMEOUT_MS)).isNull();

      EsbEvent event = EsbEventCodec.fromMessage(message);
      assertThat(event.getType()).isEqualTo(CUSTOM_TYPE);
      assertThat(event.getSource()).isEqualTo("qqq://" + INSTANCE_NAME + "/process/" + PROCESS_NAME_SYNC_ORDER);
      assertThat(event.getSubject()).isNull();
      assertThat(event.getData()).containsEntry("orderNo", "ORD-7").containsEntry("lineCount", 3);

      assertThat(EsbStats.getInstance().destination(QUEUE_DESTINATION).published()).isEqualTo(1L);
   }



   /*******************************************************************************
    ** Without an instance name or a source path, the source is the application
    ** root with an empty authority; without data, data is empty.
    *******************************************************************************/
   @Test
   void testPublishActionDefaultSource() throws Exception
   {
      setUpInstance(null);
      MessageConsumer consumer = openQueueConsumer();

      EsbPublishOutput output = new EsbPublishAction().execute(new EsbPublishInput()
         .withDestinationName(QUEUE_DESTINATION)
         .withType(CUSTOM_TYPE));
      assertThat(output.getSuccess()).isTrue();

      EsbEvent event = EsbEventCodec.fromMessage(consumer.receive(RECEIVE_TIMEOUT_MS));
      assertThat(event.getSource()).isEqualTo(EsbEventFactory.SOURCE_PREFIX + "/");
      assertThat(event.getData()).isEmpty();
   }



   /*******************************************************************************
    ** Without a type, nothing is sent: the output (not an exception) says why,
    ** and the failure is counted.
    *******************************************************************************/
   @Test
   void testPublishActionWithoutTypeReturnsError() throws Exception
   {
      setUpInstance(INSTANCE_NAME);
      MessageConsumer consumer = openQueueConsumer();

      EsbPublishOutput output = new EsbPublishAction().execute(new EsbPublishInput().withDestinationName(QUEUE_DESTINATION));

      assertThat(output.getSuccess()).isFalse();
      assertThat(output.getSent()).isZero();
      assertThat(output.getError()).contains("type is required");
      assertThat(consumer.receive(NOTHING_RECEIVED_TIMEOUT_MS)).isNull();
      assertThat(EsbStats.getInstance().destination(QUEUE_DESTINATION).publishFailures()).isEqualTo(1L);
   }



   /*******************************************************************************
    ** An unknown destination is an error in the output, counted against that
    ** name - never an exception.
    *******************************************************************************/
   @Test
   void testUnknownDestinationReturnsError() throws Exception
   {
      setUpInstance(INSTANCE_NAME);

      EsbPublishOutput output = new EsbPublishAction().execute(new EsbPublishInput()
         .withDestinationName("noSuchDestination")
         .withType(CUSTOM_TYPE));

      assertThat(output.getSuccess()).isFalse();
      assertThat(output.getSent()).isZero();
      assertThat(output.getError()).isEqualTo("Unknown ESB destination: noSuchDestination");

      EsbCounterSnapshot counters = EsbStats.getInstance().destination("noSuchDestination");
      assertThat(counters.publishFailures()).isEqualTo(1L);
      assertThat(counters.lastError()).isEqualTo("Unknown ESB destination: noSuchDestination");
   }



   /*******************************************************************************
    ** Without a QInstance in context, publishing fails (in the output).
    *******************************************************************************/
   @Test
   void testPublishWithoutQInstanceReturnsError() throws Exception
   {
      setUpInstance(INSTANCE_NAME);
      EsbEvent event = EsbEventFactory.custom(INSTANCE_NAME, "", CUSTOM_TYPE, null);
      QContext.clear();

      EsbPublishOutput output = EsbPublisher.getInstance().publish(QUEUE_DESTINATION, List.of(event));
      assertThat(output.getSuccess()).isFalse();
      assertThat(output.getSent()).isZero();
      assertThat(output.getError()).contains("no QInstance");
   }



   /*******************************************************************************
    ** Publishing no events (an empty or null list) succeeds, sending nothing.
    *******************************************************************************/
   @Test
   void testPublishNoEvents() throws Exception
   {
      setUpInstance(INSTANCE_NAME);

      for(List<EsbEvent> events : Arrays.asList(List.<EsbEvent>of(), null))
      {
         EsbPublishOutput output = EsbPublisher.getInstance().publish(QUEUE_DESTINATION, events);
         assertThat(output.getSuccess()).isTrue();
         assertThat(output.getSent()).isZero();
         assertThat(output.getError()).isNull();
      }

      assertThat(EsbStats.getInstance().destination(QUEUE_DESTINATION)).isEqualTo(EsbCounterSnapshot.EMPTY);
   }



   /*******************************************************************************
    ** Several events in one publish all reach a topic subscriber, in order.
    *******************************************************************************/
   @Test
   void testPublishSeveralEventsToTopic() throws Exception
   {
      setUpInstance(INSTANCE_NAME);
      Session         session  = getReceivingSession();
      MessageConsumer consumer = session.createConsumer(session.createTopic(topicBrokerName));

      List<EsbEvent> events = new ArrayList<>();
      for(int i = 0; i < 3; i++)
      {
         events.add(EsbEventFactory.custom(INSTANCE_NAME, "", CUSTOM_TYPE, Map.of("index", i)));
      }

      EsbPublishOutput output = EsbPublisher.getInstance().publish(TOPIC_DESTINATION, events);
      assertThat(output.getSuccess()).isTrue();
      assertThat(output.getSent()).isEqualTo(3);

      for(EsbEvent event : events)
      {
         assertThat(EsbEventCodec.fromMessage(consumer.receive(RECEIVE_TIMEOUT_MS)).getId()).isEqualTo(event.getId());
      }
      assertThat(EsbStats.getInstance().destination(TOPIC_DESTINATION).published()).isEqualTo(3L);
   }



   /*******************************************************************************
    ** With the broker down, a publish returns (not throws) the failure, and counts
    ** each event.
    *******************************************************************************/
   @Test
   void testPublishWhenBrokerDownReturnsError() throws Exception
   {
      setUpInstance(INSTANCE_NAME);
      stopEmbeddedBroker();

      List<EsbEvent> events = List.of(
         EsbEventFactory.custom(INSTANCE_NAME, "", CUSTOM_TYPE, null),
         EsbEventFactory.custom(INSTANCE_NAME, "", CUSTOM_TYPE, null));

      EsbPublishOutput output = EsbPublisher.getInstance().publish(QUEUE_DESTINATION, events);
      assertThat(output.getSuccess()).isFalse();
      assertThat(output.getSent()).isZero();
      assertThat(output.getError()).isNotBlank();
      assertThat(EsbStats.getInstance().destination(QUEUE_DESTINATION).publishFailures()).isEqualTo(2L);
   }



   /*******************************************************************************
    ** Validate (and put in context) an instance with the queue and topic
    ** destinations, and this instance name (none if null).
    *******************************************************************************/
   private QInstance setUpInstance(String instanceName) throws QException
   {
      QInstance qInstance = defineInstance();
      EsbInstanceMetaData.of(qInstance)
         .withInstanceName(instanceName)
         .withDestination(new QEsbDestinationMetaData()
            .withName(QUEUE_DESTINATION)
            .withType(EsbDestinationType.QUEUE)
            .withProviderName(PROVIDER_NAME)
            .withDestinationName(queueBrokerName))
         .withDestination(new QEsbDestinationMetaData()
            .withName(TOPIC_DESTINATION)
            .withType(EsbDestinationType.TOPIC)
            .withProviderName(PROVIDER_NAME)
            .withDestinationName(topicBrokerName));

      new QInstanceValidator().validate(qInstance);
      QContext.init(qInstance, new QSession());
      return (qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private MessageConsumer openQueueConsumer() throws Exception
   {
      Session session = getReceivingSession();
      return (session.createConsumer(session.createQueue(queueBrokerName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Session getReceivingSession() throws QException
   {
      if(receivingSession == null)
      {
         receivingSession = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false);
      }
      return (receivingSession);
   }

}
