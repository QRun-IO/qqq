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


import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbTableEvent;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.qqq.esb.model.EsbTablePublication;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.stats.EsbCounterSnapshot;
import com.kingsrook.qqq.esb.stats.EsbStats;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import org.apache.activemq.artemis.core.postoffice.RoutingStatus;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerMessagePlugin;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for publishing table record change events (EsbRecordChangeListener
 ** and EsbPublisher) - against the embedded Artemis broker.
 **
 ** Each test uses its own broker-side queue and topic names, so no messages
 ** carry over between tests.
 *******************************************************************************/
class EsbTablePublishingTest extends EsbTestBase
{
   private static final String INSTANCE_NAME     = "orderService";
   private static final String QUEUE_DESTINATION = "orderQueue";
   private static final String TOPIC_DESTINATION = "orderEvents";

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
      queueBrokerName = "esb.test.publish.queue." + suffix;
      topicBrokerName = "esb.test.publish.topic." + suffix;
   }



   /*******************************************************************************
    ** Reset the connection manager (which closes the receiving session), and make
    ** sure the broker is running for the next test (in case a test that stops it
    ** failed part-way).
    *******************************************************************************/
   @AfterEach
   void afterEach() throws Exception
   {
      receivingSession = null;
      EsbConnectionManager.getInstance().closeAll();
      startEmbeddedBroker();
   }



   /*******************************************************************************
    ** Each inserted record is published as one inserted event: type, source,
    ** subject (the primary key), and data { record } - as a PERSISTENT JMS
    ** TextMessage with the ce_* properties.
    *******************************************************************************/
   @Test
   void testInsertPublishesOneEventPerRecord() throws Exception
   {
      setUpInstance(publication(QUEUE_DESTINATION, EsbTableEvent.INSERT));
      MessageConsumer consumer = openQueueConsumer();

      List<QRecord> inserted = insertOrders(null, 2).getRecords();

      List<Message> messages = receiveMessages(consumer, 2);
      assertNothingReceived(consumer);
      assertThat(messages).allSatisfy(message ->
      {
         assertThat(message.getJMSDeliveryMode()).isEqualTo(DeliveryMode.PERSISTENT);
         assertThat(message.getStringProperty(EsbEventCodec.PROPERTY_TYPE)).isEqualTo("qqq.table.order.inserted");
      });

      List<EsbEvent> events = toEvents(messages);
      assertThat(events).extracting(EsbEvent::getType).containsOnly("qqq.table.order.inserted");
      assertThat(events).extracting(EsbEvent::getSource).containsOnly("qqq://" + INSTANCE_NAME + "/table/" + TABLE_NAME_ORDER);
      assertThat(events).extracting(EsbEvent::getSubject).containsExactly(primaryKeyString(inserted.get(0)), primaryKeyString(inserted.get(1)));
      assertThat(events.get(0).getData()).containsOnlyKeys(EsbEventFactory.DATA_RECORD);
      assertThat(dataRecord(events.get(0), EsbEventFactory.DATA_RECORD)).containsEntry("orderNo", "ORD-1").containsEntry("status", "NEW");
      assertThat(dataRecord(events.get(1), EsbEventFactory.DATA_RECORD)).containsEntry("orderNo", "ORD-2");

      EsbCounterSnapshot counters = EsbStats.getInstance().destination(QUEUE_DESTINATION);
      assertThat(counters.published()).isEqualTo(2L);
      assertThat(counters.publishFailures()).isZero();
   }



   /*******************************************************************************
    ** Each updated record is published as one updated event, whose data.record is
    ** the full post-update record and data.oldRecord the record from before.
    *******************************************************************************/
   @Test
   void testUpdatePublishesOneEventPerRecord() throws Exception
   {
      setUpInstance(publication(QUEUE_DESTINATION, EsbTableEvent.UPDATE));
      MessageConsumer consumer = openQueueConsumer();

      List<QRecord> inserted = insertOrders(null, 2).getRecords();
      assertNothingReceived(consumer);

      new UpdateAction().execute(new UpdateInput(TABLE_NAME_ORDER).withRecords(List.of(
         new QRecord().withValue("id", inserted.get(0).getValue("id")).withValue("status", "SHIPPED"),
         new QRecord().withValue("id", inserted.get(1).getValue("id")).withValue("status", "SHIPPED"))));

      List<EsbEvent> events = toEvents(receiveMessages(consumer, 2));
      assertNothingReceived(consumer);
      assertThat(events).extracting(EsbEvent::getType).containsOnly("qqq.table.order.updated");
      assertThat(events).extracting(EsbEvent::getSubject).containsExactly(primaryKeyString(inserted.get(0)), primaryKeyString(inserted.get(1)));
      assertThat(dataRecord(events.get(0), EsbEventFactory.DATA_RECORD)).containsEntry("orderNo", "ORD-1").containsEntry("status", "SHIPPED");
      assertThat(dataRecord(events.get(0), EsbEventFactory.DATA_OLD_RECORD)).containsEntry("orderNo", "ORD-1").containsEntry("status", "NEW");
      assertThat(dataRecord(events.get(1), EsbEventFactory.DATA_RECORD)).containsEntry("orderNo", "ORD-2").containsEntry("status", "SHIPPED");
      assertThat(EsbStats.getInstance().destination(QUEUE_DESTINATION).published()).isEqualTo(2L);
   }



   /*******************************************************************************
    ** Each deleted record is published as one deleted event, with data
    ** { oldRecord }.
    *******************************************************************************/
   @Test
   void testDeletePublishesOneEventPerRecord() throws Exception
   {
      setUpInstance(publication(QUEUE_DESTINATION, EsbTableEvent.DELETE));
      MessageConsumer consumer = openQueueConsumer();

      List<QRecord> inserted = insertOrders(null, 2).getRecords();
      assertNothingReceived(consumer);

      new DeleteAction().execute(new DeleteInput(TABLE_NAME_ORDER).withPrimaryKeys(List.of(inserted.get(0).getValue("id"), inserted.get(1).getValue("id"))));

      List<EsbEvent> events = toEvents(receiveMessages(consumer, 2));
      assertNothingReceived(consumer);
      assertThat(events).extracting(EsbEvent::getType).containsOnly("qqq.table.order.deleted");
      assertThat(events).extracting(EsbEvent::getSubject).containsExactlyInAnyOrder(primaryKeyString(inserted.get(0)), primaryKeyString(inserted.get(1)));
      assertThat(events).allSatisfy(event -> assertThat(event.getData()).containsOnlyKeys(EsbEventFactory.DATA_OLD_RECORD));
      assertThat(events).extracting(event -> dataRecord(event, EsbEventFactory.DATA_OLD_RECORD).get("orderNo")).containsExactlyInAnyOrder("ORD-1", "ORD-2");
   }



   /*******************************************************************************
    ** Only the events a publication lists are published (and the listener says
    ** it doesn't apply to the rest, so actions skip fetching old records).
    *******************************************************************************/
   @Test
   void testOnlyConfiguredEventsArePublished() throws Exception
   {
      setUpInstance(publication(QUEUE_DESTINATION, EsbTableEvent.INSERT));
      MessageConsumer consumer = openQueueConsumer();

      EsbRecordChangeListener listener = new EsbRecordChangeListener();
      assertThat(listener.appliesTo(TABLE_NAME_ORDER, RecordChangeType.INSERT)).isTrue();
      assertThat(listener.appliesTo(TABLE_NAME_ORDER, RecordChangeType.UPDATE)).isFalse();
      assertThat(listener.appliesTo(TABLE_NAME_ORDER, RecordChangeType.DELETE)).isFalse();
      assertThat(listener.appliesTo(TABLE_NAME_ORDER, null)).isFalse();
      assertThat(listener.appliesTo("noSuchTable", RecordChangeType.INSERT)).isFalse();

      List<QRecord> inserted = insertOrders(null, 1).getRecords();
      assertThat(receiveMessages(consumer, 1)).hasSize(1);

      Serializable id = inserted.get(0).getValue("id");
      new UpdateAction().execute(new UpdateInput(TABLE_NAME_ORDER).withRecords(List.of(new QRecord().withValue("id", id).withValue("status", "SHIPPED"))));
      new DeleteAction().execute(new DeleteInput(TABLE_NAME_ORDER).withPrimaryKeys(List.of(id)));
      assertNothingReceived(consumer);
      assertThat(EsbStats.getInstance().destination(QUEUE_DESTINATION).published()).isEqualTo(1L);
   }



   /*******************************************************************************
    ** A table without ESB publications publishes nothing, and the listener
    ** doesn't apply to it - nor to anything, without a QInstance in context.
    *******************************************************************************/
   @Test
   void testTableWithoutPublicationsPublishesNothing() throws Exception
   {
      setUpInstance();
      MessageConsumer consumer = openQueueConsumer();

      assertThat(new EsbRecordChangeListener().appliesTo(TABLE_NAME_ORDER, RecordChangeType.INSERT)).isFalse();
      insertOrders(null, 1);
      assertNothingReceived(consumer);

      QContext.clear();
      assertThat(new EsbRecordChangeListener().appliesTo(TABLE_NAME_ORDER, RecordChangeType.INSERT)).isFalse();
   }



   /*******************************************************************************
    ** A table with two publications (here, a queue and a topic) sends each event
    ** to both - the same event (same id) to each.
    *******************************************************************************/
   @Test
   void testTwoPublicationsSendToBoth() throws Exception
   {
      setUpInstance(publication(QUEUE_DESTINATION, EsbTableEvent.INSERT), publication(TOPIC_DESTINATION, EsbTableEvent.INSERT, EsbTableEvent.UPDATE));
      MessageConsumer queueConsumer = openQueueConsumer();
      MessageConsumer topicConsumer = openTopicConsumer();

      insertOrders(null, 1);

      EsbEvent queueEvent = toEvents(receiveMessages(queueConsumer, 1)).get(0);
      EsbEvent topicEvent = toEvents(receiveMessages(topicConsumer, 1)).get(0);
      assertThat(queueEvent.getType()).isEqualTo("qqq.table.order.inserted");
      assertThat(topicEvent.getId()).isEqualTo(queueEvent.getId());
      assertNothingReceived(queueConsumer);
      assertNothingReceived(topicConsumer);

      assertThat(EsbStats.getInstance().destination(QUEUE_DESTINATION).published()).isEqualTo(1L);
      assertThat(EsbStats.getInstance().destination(TOPIC_DESTINATION).published()).isEqualTo(1L);
   }



   /*******************************************************************************
    ** Inside a caller's transaction, nothing is sent until the transaction
    ** commits - then every record's event is.
    *******************************************************************************/
   @Test
   void testNothingSentUntilCallersTransactionCommits() throws Exception
   {
      setUpInstance(publication(QUEUE_DESTINATION, EsbTableEvent.INSERT));
      MessageConsumer consumer = openQueueConsumer();

      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE_NAME_ORDER)))
      {
         insertOrders(transaction, 2);
         assertNothingReceived(consumer);
         assertThat(EsbStats.getInstance().destination(QUEUE_DESTINATION).published()).isZero();

         transaction.commit();
      }

      assertThat(toEvents(receiveMessages(consumer, 2))).extracting(EsbEvent::getType).containsOnly("qqq.table.order.inserted");
      assertThat(EsbStats.getInstance().destination(QUEUE_DESTINATION).published()).isEqualTo(2L);
   }



   /*******************************************************************************
    ** A rolled-back transaction publishes nothing - not even when the same
    ** transaction is committed later.
    *******************************************************************************/
   @Test
   void testRollbackPublishesNothing() throws Exception
   {
      setUpInstance(publication(QUEUE_DESTINATION, EsbTableEvent.INSERT));
      MessageConsumer consumer = openQueueConsumer();

      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE_NAME_ORDER)))
      {
         insertOrders(transaction, 2);
         transaction.rollback();
         transaction.commit();
      }

      assertNothingReceived(consumer);
      EsbCounterSnapshot counters = EsbStats.getInstance().destination(QUEUE_DESTINATION);
      assertThat(counters.published()).isZero();
      assertThat(counters.publishFailures()).isZero();
   }



   /*******************************************************************************
    ** Review focus 1: inserting 5,000 records inside a transaction succeeds, sends
    ** nothing before the commit, and then publishes all 5,000 messages - on one
    ** session (as the broker sees it).
    *******************************************************************************/
   @Test
   void bulkInsertPublishesAllAfterCommit() throws Exception
   {
      Integer recordCount = 5000;
      setUpInstance(publication(QUEUE_DESTINATION, EsbTableEvent.INSERT));
      MessageConsumer consumer = openQueueConsumer();

      ActiveMQServer         brokerServer = getEmbeddedBrokerServer();
      SendingSessionRecorder recorder     = new SendingSessionRecorder(queueBrokerName);
      brokerServer.registerBrokerPlugin(recorder);
      try
      {
         try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE_NAME_ORDER)))
         {
            InsertOutput insertOutput = insertOrders(transaction, recordCount);
            assertThat(insertOutput.getRecords()).hasSize(recordCount).allSatisfy(record ->
            {
               assertThat(record.getErrors()).isNullOrEmpty();
               assertThat(record.getValue("id")).isNotNull();
            });

            assertThat(recorder.getMessageCount()).isZero();
            assertThat(EsbStats.getInstance().destination(QUEUE_DESTINATION).published()).isZero();

            transaction.commit();
         }

         List<EsbEvent> events = toEvents(receiveMessages(consumer, recordCount));
         assertNothingReceived(consumer);
         assertThat(events).extracting(EsbEvent::getSubject).doesNotHaveDuplicates().hasSize(recordCount);

         EsbCounterSnapshot counters = EsbStats.getInstance().destination(QUEUE_DESTINATION);
         assertThat(counters.published()).isEqualTo(recordCount.longValue());
         assertThat(counters.publishFailures()).isZero();

         assertThat(recorder.getMessageCount()).isEqualTo(recordCount);
         assertThat(recorder.getSessionNames()).hasSize(1);
      }
      finally
      {
         brokerServer.unRegisterBrokerPlugin(recorder);
      }
   }



   /*******************************************************************************
    ** Review focus 2: with the broker down, an insert (with no transaction, so it
    ** publishes right away) still succeeds, within the 5 s connect timeout - and
    ** each record's event is counted as a publish failure, not thrown.
    *******************************************************************************/
   @Test
   void saveSucceedsWhenBrokerDown() throws Exception
   {
      setUpInstance(publication(QUEUE_DESTINATION, EsbTableEvent.INSERT));
      stopEmbeddedBroker();

      long         startNanos   = System.nanoTime();
      InsertOutput insertOutput = insertOrders(null, 5);
      Duration     elapsed      = Duration.ofNanos(System.nanoTime() - startNanos);

      assertThat(elapsed).isLessThan(Duration.ofSeconds(6));
      assertThat(insertOutput.getRecords()).hasSize(5).allSatisfy(record -> assertThat(record.getErrors()).isNullOrEmpty());
      assertThat(new CountAction().execute(new CountInput(TABLE_NAME_ORDER)).getCount()).isEqualTo(5);

      EsbCounterSnapshot counters = EsbStats.getInstance().destination(QUEUE_DESTINATION);
      assertThat(counters.publishFailures()).isEqualTo(5L);
      assertThat(counters.published()).isZero();
      assertThat(counters.lastError()).isNotBlank();
   }



   /*******************************************************************************
    ** With the broker down, committing the caller's transaction still succeeds,
    ** and the events are counted as publish failures.
    *******************************************************************************/
   @Test
   void testCommitSucceedsWhenBrokerDown() throws Exception
   {
      setUpInstance(publication(QUEUE_DESTINATION, EsbTableEvent.INSERT));
      stopEmbeddedBroker();

      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE_NAME_ORDER)))
      {
         insertOrders(transaction, 3);
         transaction.commit();
      }

      assertThat(new CountAction().execute(new CountInput(TABLE_NAME_ORDER)).getCount()).isEqualTo(3);
      assertThat(EsbStats.getInstance().destination(QUEUE_DESTINATION).publishFailures()).isEqualTo(3L);
   }



   /*******************************************************************************
    ** Enriching the ESB meta-data registers the record change listener once,
    ** however many times it runs.
    *******************************************************************************/
   @Test
   void testEnrichRegistersListenerOnce() throws Exception
   {
      QInstance qInstance = setUpInstance(publication(QUEUE_DESTINATION, EsbTableEvent.INSERT));
      EsbInstanceMetaData.of(qInstance).enrich(qInstance);

      assertThat(qInstance.getRecordChangeListeners())
         .extracting(QCodeReference::getName)
         .containsOnlyOnce(EsbRecordChangeListener.class.getName());
   }



   /*******************************************************************************
    ** Set up (validate, which enriches, and put in context) an instance with the
    ** queue and topic destinations, an instance name, and these publications on
    ** the order table (none: no ESB table meta-data).
    *******************************************************************************/
   private QInstance setUpInstance(EsbTablePublication... publications) throws QException
   {
      QInstance qInstance = defineInstance();
      EsbInstanceMetaData.of(qInstance)
         .withInstanceName(INSTANCE_NAME)
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

      if(publications.length > 0)
      {
         EsbTableMetaData esbTableMetaData = EsbTableMetaData.ofOrWithNew(qInstance.getTable(TABLE_NAME_ORDER));
         for(EsbTablePublication publication : publications)
         {
            esbTableMetaData.withPublication(publication);
         }
      }

      new QInstanceValidator().validate(qInstance);
      QContext.init(qInstance, new QSession());
      return (qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static EsbTablePublication publication(String destinationName, EsbTableEvent... events)
   {
      return (new EsbTablePublication()
         .withDestinationName(destinationName)
         .withEvents(List.of(events)));
   }



   /*******************************************************************************
    ** Insert orders ORD-1 .. ORD-count (status NEW), in the transaction if given.
    *******************************************************************************/
   private static InsertOutput insertOrders(QBackendTransaction transaction, Integer count) throws QException
   {
      List<QRecord> records = new ArrayList<>();
      for(int i = 1; i <= count; i++)
      {
         records.add(new QRecord().withValue("orderNo", "ORD-" + i).withValue("status", "NEW"));
      }

      return (new InsertAction().execute(new InsertInput(TABLE_NAME_ORDER).withRecords(records).withTransaction(transaction)));
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
    ** A (non-durable) topic subscriber - which must exist before a message is
    ** published, to receive it.
    *******************************************************************************/
   private MessageConsumer openTopicConsumer() throws Exception
   {
      Session session = getReceivingSession();
      return (session.createConsumer(session.createTopic(topicBrokerName)));
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



   /*******************************************************************************
    ** Receive exactly count messages (failing if any doesn't arrive in time).
    *******************************************************************************/
   private static List<Message> receiveMessages(MessageConsumer consumer, Integer count) throws Exception
   {
      List<Message> messages = new ArrayList<>();
      for(int i = 0; i < count; i++)
      {
         Message message = consumer.receive(RECEIVE_TIMEOUT_MS);
         assertThat(message).as("message " + (i + 1) + " of " + count).isNotNull();
         messages.add(message);
      }
      return (messages);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void assertNothingReceived(MessageConsumer consumer) throws Exception
   {
      assertThat(consumer.receive(NOTHING_RECEIVED_TIMEOUT_MS)).isNull();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<EsbEvent> toEvents(List<Message> messages) throws Exception
   {
      List<EsbEvent> events = new ArrayList<>();
      for(Message message : messages)
      {
         events.add(EsbEventCodec.fromMessage(message));
      }
      return (events);
   }



   /*******************************************************************************
    ** A record's values (data.record or data.oldRecord) from a parsed event.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static Map<String, Object> dataRecord(EsbEvent event, String key)
   {
      return ((Map<String, Object>) event.getData().get(key));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String primaryKeyString(QRecord record)
   {
      return (String.valueOf(record.getValue("id")));
   }



   /*******************************************************************************
    * Broker plugin that records which server sessions send messages to one
    * address, and how many.
    *******************************************************************************/
   private static class SendingSessionRecorder implements ActiveMQServerMessagePlugin
   {
      private final String        address;
      private final Set<String>   sessionNames = ConcurrentHashMap.newKeySet();
      private final AtomicInteger messageCount = new AtomicInteger(0);



      /*******************************************************************************
       ** Constructor
       *******************************************************************************/
      SendingSessionRecorder(String address)
      {
         this.address = address;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void afterSend(ServerSession session, Transaction tx, org.apache.activemq.artemis.api.core.Message message, boolean direct, boolean noAutoCreateQueue, RoutingStatus result)
      {
         if(address.equals(message.getAddress()))
         {
            sessionNames.add(session.getName());
            messageCount.incrementAndGet();
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      Set<String> getSessionNames()
      {
         return (sessionNames);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      Integer getMessageCount()
      {
         return (messageCount.get());
      }
   }

}
