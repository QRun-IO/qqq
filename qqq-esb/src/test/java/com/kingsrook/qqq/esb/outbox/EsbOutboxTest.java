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

package com.kingsrook.qqq.esb.outbox;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.health.HealthCheckOutcome;
import com.kingsrook.qqq.backend.core.health.HealthChecksMetaData;
import com.kingsrook.qqq.backend.core.health.HealthStatus;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.health.EsbOutboxHealthChecks;
import com.kingsrook.qqq.esb.model.EsbDedupeConfig;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbOutboxConfig;
import com.kingsrook.qqq.esb.model.EsbTableEvent;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.qqq.esb.model.EsbTablePublication;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.stats.EsbStats;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerMessagePlugin;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for the transactional outbox: EsbOutbox, EsbOutboxRelay, and the
 ** outbox health check - on an H2 backend and the embedded broker.
 *******************************************************************************/
class EsbOutboxTest extends EsbTestBase
{
   private static final String DESTINATION       = "orderQueue";
   private static final String REJECTED_PREFIX  = "esb.test.rejected.";

   private final RejectingPlugin rejectingPlugin = new RejectingPlugin();
   private static final Long   RECEIVE_TIMEOUT   = 5000L;
   private static final Long   NOTHING_TIMEOUT   = 300L;

   private String  brokerQueueName;
   private Session receivingSession;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      EsbH2TestSupport.resetDatabase();
      EsbStats.getInstance().reset();
      brokerQueueName = "esb.test.outbox." + UUID.randomUUID();
      RejectingPlugin.rejecting = true;
      getEmbeddedBrokerServer().registerBrokerPlugin(rejectingPlugin);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach() throws Exception
   {
      EsbOutboxRelay.getInstance().stop();
      if(getEmbeddedBrokerServer() != null)
      {
         getEmbeddedBrokerServer().unRegisterBrokerPlugin(rejectingPlugin);
      }
      receivingSession = null;
      EsbConnectionManager.getInstance().closeAll();
      startEmbeddedBroker();
   }



   /*******************************************************************************
    ** A validated instance whose H2 order table publishes inserts, updates and
    ** deletes to a queue, through the outbox.
    *******************************************************************************/
   private QInstance setUpInstance(EsbOutboxConfig outboxConfig, String queueName) throws Exception
   {
      QInstance qInstance = defineInstance();
      EsbH2TestSupport.addTo(qInstance);
      EsbInstanceMetaData.of(qInstance)
         .withInstanceName("outboxTest")
         .withOutbox(outboxConfig)
         .withDestination(new QEsbDestinationMetaData()
            .withName(DESTINATION)
            .withType(EsbDestinationType.QUEUE)
            .withProviderName(PROVIDER_NAME)
            .withDestinationName(queueName));
      EsbTableMetaData.ofOrWithNew(qInstance.getTable(EsbH2TestSupport.TABLE_NAME_ORDER))
         .withPublication(new EsbTablePublication().withDestinationName(DESTINATION).withEvents(List.of(EsbTableEvent.INSERT, EsbTableEvent.UPDATE, EsbTableEvent.DELETE)));
      new QInstanceValidator().validate(qInstance);
      QContext.init(qInstance, new QSession());
      return (qInstance);
   }



   /*******************************************************************************
    ** The default (enabled) outbox config, with no backoff, for tests.
    *******************************************************************************/
   private static EsbOutboxConfig enabledConfig()
   {
      return (new EsbOutboxConfig().withEnabled(true).withTableName(EsbH2TestSupport.OUTBOX_TABLE).withInitialBackoffMs(0).withMaxBackoffMs(0));
   }



   /*******************************************************************************
    ** Off by default.
    *******************************************************************************/
   @Test
   void testDefaults()
   {
      EsbInstanceMetaData esbInstanceMetaData = new EsbInstanceMetaData();
      assertThat(esbInstanceMetaData.isOutboxEnabled()).isFalse();
      assertThat(esbInstanceMetaData.isDedupeEnabled()).isFalse();
      assertThat(esbInstanceMetaData.getOutbox().getTableName()).isEqualTo("esb_outbox");
      assertThat(esbInstanceMetaData.getDedupe().getTableName()).isEqualTo("esb_processed_message");
   }



   /*******************************************************************************
    ** An enabled outbox needs its table in the instance.
    *******************************************************************************/
   @Test
   void testValidationRequiresTable()
   {
      QInstance qInstance = defineInstance();
      EsbInstanceMetaData.of(qInstance)
         .withOutbox(new EsbOutboxConfig().withEnabled(true).withTableName("noSuchTable"))
         .withDedupe(new EsbDedupeConfig().withEnabled(true).withTableName("noSuchTableEither"));
      assertThatThrownBy(() -> new QInstanceValidator().validate(qInstance))
         .hasMessageContaining("outbox table noSuchTable is not in the instance")
         .hasMessageContaining("processed-message table noSuchTableEither is not in the instance");
   }



   /*******************************************************************************
    ** Rows are written in the record write's transaction (invisible until it
    ** commits), then the relay publishes them, in order, with the row uuid as
    ** the message id, and marks them SENT.
    *******************************************************************************/
   @Test
   void testWriteInTransactionThenRelay() throws Exception
   {
      setUpInstance(enabledConfig(), brokerQueueName);
      MessageConsumer consumer = openConsumer(brokerQueueName);

      QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(EsbH2TestSupport.TABLE_NAME_ORDER));
      try
      {
         insertOrders(transaction, "ORD-1", "ORD-2");
         assertThat(countOutbox()).as("rows are not visible outside the transaction").isZero();
         transaction.commit();
      }
      finally
      {
         transaction.close();
      }

      List<QRecord> rows = queryOutbox();
      assertThat(rows).hasSize(2).allSatisfy(row ->
      {
         assertThat(row.getValueString(EsbOutboxTable.FIELD_STATUS)).isEqualTo(EsbOutboxTable.STATUS_PENDING);
         assertThat(row.getValueString(EsbOutboxTable.FIELD_DESTINATION_NAME)).isEqualTo(DESTINATION);
         assertThat(row.getValueString(EsbOutboxTable.FIELD_EVENT_TYPE)).isEqualTo("qqq.table.h2Order.inserted");
      });
      assertThat(consumer.receive(NOTHING_TIMEOUT)).as("nothing is sent before the relay runs").isNull();

      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isEqualTo(2);

      List<Message> messages = receive(consumer, 2);
      assertThat(messages.get(0).getStringProperty(EsbEventCodec.PROPERTY_MESSAGE_ID)).isEqualTo(rows.get(0).getValueString(EsbOutboxTable.FIELD_UUID));
      assertThat(messages.get(1).getStringProperty(EsbEventCodec.PROPERTY_MESSAGE_ID)).isEqualTo(rows.get(1).getValueString(EsbOutboxTable.FIELD_UUID));
      assertThat(((TextMessage) messages.get(0)).getText()).contains("ORD-1");
      assertThat(((TextMessage) messages.get(1)).getText()).contains("ORD-2");

      assertThat(queryOutbox()).allSatisfy(row ->
      {
         assertThat(row.getValueString(EsbOutboxTable.FIELD_STATUS)).isEqualTo(EsbOutboxTable.STATUS_SENT);
         assertThat(row.getValueInstant(EsbOutboxTable.FIELD_SENT_AT)).isNotNull();
         assertThat(row.getValueString(EsbOutboxTable.FIELD_LEASE_OWNER)).isNull();
      });
      assertThat(EsbStats.getInstance().destination(DESTINATION).published()).isEqualTo(2L);
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isZero();
   }



   /*******************************************************************************
    ** A rolled-back write leaves no outbox row (and sends nothing).
    *******************************************************************************/
   @Test
   void testRollbackLeavesNoOutboxRow() throws Exception
   {
      setUpInstance(enabledConfig(), brokerQueueName);

      QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(EsbH2TestSupport.TABLE_NAME_ORDER));
      try
      {
         insertOrders(transaction, "ORD-1");
         transaction.rollback();
      }
      finally
      {
         transaction.close();
      }

      assertThat(new CountAction().execute(new CountInput(EsbH2TestSupport.TABLE_NAME_ORDER)).getCount()).isZero();
      assertThat(countOutbox()).isZero();
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isZero();
   }



   /*******************************************************************************
    ** Without a transaction, the rows are written before the action returns;
    ** updates and deletes are written too.
    *******************************************************************************/
   @Test
   void testNoTransactionWritesRightAway() throws Exception
   {
      setUpInstance(enabledConfig(), brokerQueueName);
      QRecord order = insertOrders(null, "ORD-1").get(0);
      assertThat(countOutbox()).isEqualTo(1);

      new UpdateAction().execute(new UpdateInput(EsbH2TestSupport.TABLE_NAME_ORDER).withRecord(new QRecord().withValue("id", order.getValue("id")).withValue("status", "SHIPPED")));
      assertThat(queryOutbox()).extracting(row -> row.getValueString(EsbOutboxTable.FIELD_EVENT_TYPE))
         .containsExactly("qqq.table.h2Order.inserted", "qqq.table.h2Order.updated");
   }



   /*******************************************************************************
    ** Writes that omit record change events write no outbox rows (though the
    ** listener is required, so it runs).
    *******************************************************************************/
   @Test
   void testSuppressedEventsAreNotWritten() throws Exception
   {
      setUpInstance(enabledConfig(), brokerQueueName);
      new InsertAction().execute(new InsertInput(EsbH2TestSupport.TABLE_NAME_ORDER)
         .withOmitRecordChangeEvents(true)
         .withRecord(new QRecord().withValue("orderNo", "ORD-1")));
      assertThat(countOutbox()).isZero();
   }



   /*******************************************************************************
    ** If the outbox write fails in the transaction, the record write fails too.
    *******************************************************************************/
   @Test
   void testOutboxFailureFailsTheWrite() throws Exception
   {
      QInstance qInstance = setUpInstance(enabledConfig(), brokerQueueName);
      qInstance.getTable(EsbH2TestSupport.OUTBOX_TABLE).withBackendDetails(new RDBMSTableBackendDetails().withTableName("no_such_outbox"));

      QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(EsbH2TestSupport.TABLE_NAME_ORDER));
      try
      {
         assertThatThrownBy(() -> insertOrders(transaction, "ORD-1")).isNotNull();
         transaction.rollback();
      }
      finally
      {
         transaction.close();
      }
      assertThat(new CountAction().execute(new CountInput(EsbH2TestSupport.TABLE_NAME_ORDER)).getCount()).isZero();
   }



   /*******************************************************************************
    ** Each destination's rows go in id order, a batch at a time.
    *******************************************************************************/
   @Test
   void testOrderingAcrossBatches() throws Exception
   {
      setUpInstance(enabledConfig().withBatchSize(2), brokerQueueName);
      MessageConsumer consumer = openConsumer(brokerQueueName);
      for(int i = 1; i <= 5; i++)
      {
         insertOrders(null, "ORD-" + i);
      }

      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isEqualTo(2);
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isEqualTo(2);
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isEqualTo(1);
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isZero();

      List<Message> messages = receive(consumer, 5);
      for(int i = 0; i < 5; i++)
      {
         assertThat(((TextMessage) messages.get(i)).getText()).contains("ORD-" + (i + 1));
      }
   }



   /*******************************************************************************
    ** A failed send (broker connected) counts an attempt on the head row and
    ** blocks the destination during its backoff; then it retries, alone, and
    ** succeeds once the destination works.
    *******************************************************************************/
   @Test
   void testRetryWithBackoffThenSuccess() throws Exception
   {
      String queueName = REJECTED_PREFIX + UUID.randomUUID();
      setUpInstance(enabledConfig().withInitialBackoffMs(60_000).withMaxBackoffMs(120_000), queueName);
      EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false).close();
      insertOrders(null, "ORD-1", "ORD-2");

      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isZero();
      QRecord head = queryOutbox().get(0);
      assertThat(head.getValueInteger(EsbOutboxTable.FIELD_ATTEMPTS)).isEqualTo(1);
      assertThat(head.getValueString(EsbOutboxTable.FIELD_STATUS)).isEqualTo(EsbOutboxTable.STATUS_PENDING);
      assertThat(head.getValueString(EsbOutboxTable.FIELD_LAST_ERROR)).isNotBlank();
      assertThat(head.getValueString(EsbOutboxTable.FIELD_LEASE_OWNER)).isNull();
      assertThat(head.getValueInstant(EsbOutboxTable.FIELD_NEXT_ATTEMPT_AT)).isAfter(Instant.now().plusSeconds(50));
      assertThat(queryOutbox().get(1).getValueInteger(EsbOutboxTable.FIELD_ATTEMPTS)).isZero();

      //////////////////////////////////////////////////////////
      // backing off: the whole destination waits, in order   //
      //////////////////////////////////////////////////////////
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isZero();
      assertThat(queryOutbox().get(0).getValueInteger(EsbOutboxTable.FIELD_ATTEMPTS)).isEqualTo(1);

      ////////////////////////////////////////////////////////////////////////
      // make the queue, and the backoff due: the head goes alone, then rest //
      ////////////////////////////////////////////////////////////////////////
      RejectingPlugin.rejecting = false;
      new UpdateAction().execute(new UpdateInput(EsbH2TestSupport.OUTBOX_TABLE).withRecord(new QRecord()
         .withValue(EsbOutboxTable.FIELD_ID, head.getValue(EsbOutboxTable.FIELD_ID))
         .withValue(EsbOutboxTable.FIELD_NEXT_ATTEMPT_AT, Instant.now().minusSeconds(1))));
      MessageConsumer consumer = openConsumer(queueName);

      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isEqualTo(1);
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isEqualTo(1);
      List<Message> messages = receive(consumer, 2);
      assertThat(((TextMessage) messages.get(0)).getText()).contains("ORD-1");
      assertThat(((TextMessage) messages.get(1)).getText()).contains("ORD-2");
      assertThat(queryOutbox().get(0).getValueInteger(EsbOutboxTable.FIELD_ATTEMPTS)).isEqualTo(2);
   }



   /*******************************************************************************
    ** After maxAttempts failures a row is DEAD (and no longer blocks its
    ** destination), and the health check is DOWN.
    *******************************************************************************/
   @Test
   void testDeadAfterMaxAttempts() throws Exception
   {
      String queueName = REJECTED_PREFIX + UUID.randomUUID();
      setUpInstance(enabledConfig().withMaxAttempts(3), queueName);
      EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false).close();
      insertOrders(null, "ORD-1");

      assertThat(EsbOutboxHealthChecks.check(QContext.getQInstance()).getStatus()).isEqualTo(HealthStatus.UP);
      for(int i = 0; i < 3; i++)
      {
         assertThat(EsbOutboxRelay.getInstance().relayOnce()).isZero();
      }

      QRecord row = queryOutbox().get(0);
      assertThat(row.getValueString(EsbOutboxTable.FIELD_STATUS)).isEqualTo(EsbOutboxTable.STATUS_DEAD);
      assertThat(row.getValueInteger(EsbOutboxTable.FIELD_ATTEMPTS)).isEqualTo(3);
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isZero();
      assertThat(EsbStats.getInstance().destination(DESTINATION).publishFailures()).isEqualTo(3L);

      HealthCheckOutcome outcome = EsbOutboxHealthChecks.check(QContext.getQInstance());
      assertThat(outcome.getStatus()).isEqualTo(HealthStatus.DOWN);
      assertThat(outcome.getMessage()).contains("1 ESB outbox event(s) are DEAD");
      assertThat(HealthChecksMetaData.ofOrWithNew(QContext.getQInstance()).getChecks()).anyMatch(check -> EsbOutboxHealthChecks.CHECK_NAME.equals(check.getName()));
   }



   /*******************************************************************************
    ** While the broker is unreachable, failed sends don't count as attempts.
    *******************************************************************************/
   @Test
   void testBrokerDownDoesNotCountAttempts() throws Exception
   {
      setUpInstance(enabledConfig().withMaxAttempts(1), brokerQueueName);
      insertOrders(null, "ORD-1");
      EsbConnectionManager.getInstance().closeAll();
      stopEmbeddedBroker();

      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isZero();
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isZero();
      QRecord row = queryOutbox().get(0);
      assertThat(row.getValueString(EsbOutboxTable.FIELD_STATUS)).isEqualTo(EsbOutboxTable.STATUS_PENDING);
      assertThat(row.getValueInteger(EsbOutboxTable.FIELD_ATTEMPTS)).isZero();
      assertThat(row.getValueString(EsbOutboxTable.FIELD_LAST_ERROR)).isNotBlank();

      startEmbeddedBroker();
      waitFor(() -> EsbConnectionManager.getInstance().isConnected(PROVIDER_NAME));
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isEqualTo(1);
      assertThat(queryOutbox().get(0).getValueString(EsbOutboxTable.FIELD_STATUS)).isEqualTo(EsbOutboxTable.STATUS_SENT);
   }



   /*******************************************************************************
    ** A head row leased (unexpired) by another node blocks the destination on
    ** this node; an expired lease is taken over.
    *******************************************************************************/
   @Test
   void testLeaseByAnotherNode() throws Exception
   {
      setUpInstance(enabledConfig(), brokerQueueName);
      insertOrders(null, "ORD-1");
      QRecord row = queryOutbox().get(0);

      new UpdateAction().execute(new UpdateInput(EsbH2TestSupport.OUTBOX_TABLE).withRecord(new QRecord()
         .withValue(EsbOutboxTable.FIELD_ID, row.getValue(EsbOutboxTable.FIELD_ID))
         .withValue(EsbOutboxTable.FIELD_LEASE_OWNER, "otherNode:1234")
         .withValue(EsbOutboxTable.FIELD_LEASE_EXPIRES_AT, Instant.now().plusSeconds(60))));
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isZero();

      new UpdateAction().execute(new UpdateInput(EsbH2TestSupport.OUTBOX_TABLE).withRecord(new QRecord()
         .withValue(EsbOutboxTable.FIELD_ID, row.getValue(EsbOutboxTable.FIELD_ID))
         .withValue(EsbOutboxTable.FIELD_LEASE_EXPIRES_AT, Instant.now().minusSeconds(1))));
      assertThat(EsbOutboxRelay.getInstance().relayOnce()).isEqualTo(1);
   }



   /*******************************************************************************
    ** Purge deletes SENT rows (and processed-message rows) older than their
    ** retention, and keeps the rest.
    *******************************************************************************/
   @Test
   void testPurge() throws Exception
   {
      QInstance qInstance = setUpInstance(enabledConfig().withRetentionDays(7), brokerQueueName);
      EsbInstanceMetaData.of(qInstance).withDedupe(new EsbDedupeConfig().withEnabled(true).withTableName(EsbH2TestSupport.PROCESSED_TABLE).withRetentionDays(7));
      insertOrders(null, "ORD-1", "ORD-2", "ORD-3");
      List<QRecord> rows = queryOutbox();
      setSent(rows.get(0), Instant.now().minus(8, ChronoUnit.DAYS));
      setSent(rows.get(1), Instant.now().minus(1, ChronoUnit.DAYS));

      new InsertAction().execute(new InsertInput(EsbH2TestSupport.PROCESSED_TABLE).withRecords(List.of(
         new QRecord().withValue(EsbProcessedMessageTable.FIELD_TRIGGER_NAME, "t").withValue(EsbProcessedMessageTable.FIELD_MESSAGE_ID, "old").withValue(EsbProcessedMessageTable.FIELD_PROCESSED_AT, Instant.now().minus(8, ChronoUnit.DAYS)),
         new QRecord().withValue(EsbProcessedMessageTable.FIELD_TRIGGER_NAME, "t").withValue(EsbProcessedMessageTable.FIELD_MESSAGE_ID, "new").withValue(EsbProcessedMessageTable.FIELD_PROCESSED_AT, Instant.now()))));

      assertThat(EsbOutboxRelay.getInstance().purgeOnce()).isEqualTo(2);
      assertThat(queryOutbox()).extracting(row -> row.getValue(EsbOutboxTable.FIELD_ID))
         .containsExactly(rows.get(1).getValue(EsbOutboxTable.FIELD_ID), rows.get(2).getValue(EsbOutboxTable.FIELD_ID));
      assertThat(new QueryAction().execute(new QueryInput(EsbH2TestSupport.PROCESSED_TABLE)).getRecords())
         .extracting(r -> r.getValueString(EsbProcessedMessageTable.FIELD_MESSAGE_ID)).containsExactly("new");
   }



   /*******************************************************************************
    ** The health check degrades when the oldest PENDING row is too old.
    *******************************************************************************/
   @Test
   void testHealthDegradedWhenPendingTooLong() throws Exception
   {
      setUpInstance(enabledConfig().withPendingAlertMs(60_000), brokerQueueName);
      insertOrders(null, "ORD-1");
      assertThat(EsbOutboxHealthChecks.check(QContext.getQInstance()).getStatus()).isEqualTo(HealthStatus.UP);

      new UpdateAction().execute(new UpdateInput(EsbH2TestSupport.OUTBOX_TABLE).withRecord(new QRecord()
         .withValue(EsbOutboxTable.FIELD_ID, queryOutbox().get(0).getValue(EsbOutboxTable.FIELD_ID))
         .withValue(EsbOutboxTable.FIELD_CREATE_DATE, Instant.now().minusSeconds(120))));
      assertThat(EsbOutboxHealthChecks.check(QContext.getQInstance()).getStatus()).isEqualTo(HealthStatus.DEGRADED);
   }



   /*******************************************************************************
    ** The relay thread delivers committed rows promptly (a commit nudges it),
    ** is a daemon, and stops cleanly.
    *******************************************************************************/
   @Test
   void testRelayThreadLifecycle() throws Exception
   {
      QInstance qInstance = setUpInstance(enabledConfig().withPollIntervalMs(60_000), brokerQueueName);
      MessageConsumer consumer = openConsumer(brokerQueueName);
      EsbOutboxRelay relay = EsbOutboxRelay.getInstance();
      relay.start(qInstance);
      assertThat(relay.isRunning()).isTrue();
      Thread relayThread = Thread.getAllStackTraces().keySet().stream().filter(t -> "qqq-esb-outbox-relay".equals(t.getName())).findFirst().orElseThrow();
      assertThat(relayThread.isDaemon()).isTrue();

      insertOrders(null, "ORD-1");
      assertThat(consumer.receive(RECEIVE_TIMEOUT)).as("the nudge beats the 60 s poll").isNotNull();

      relay.stop();
      assertThat(relay.isRunning()).isFalse();
      relayThread.join(1000);
      assertThat(relayThread.isAlive()).isFalse();
   }



   /*******************************************************************************
    ** Backoff doubles, capped.
    *******************************************************************************/
   @Test
   void testBackoff()
   {
      EsbOutboxConfig config = new EsbOutboxConfig().withInitialBackoffMs(1000).withMaxBackoffMs(5000);
      assertThat(EsbOutboxRelay.getBackoffMs(config, 1)).isEqualTo(1000);
      assertThat(EsbOutboxRelay.getBackoffMs(config, 2)).isEqualTo(2000);
      assertThat(EsbOutboxRelay.getBackoffMs(config, 3)).isEqualTo(4000);
      assertThat(EsbOutboxRelay.getBackoffMs(config, 4)).isEqualTo(5000);
      assertThat(EsbOutboxRelay.getBackoffMs(config, 40)).isEqualTo(5000);
   }



   /*******************************************************************************
    ** Insert orders (in the transaction, if given).
    *******************************************************************************/
   private static List<QRecord> insertOrders(QBackendTransaction transaction, String... orderNos) throws Exception
   {
      List<QRecord> records = new ArrayList<>();
      for(String orderNo : orderNos)
      {
         records.add(new QRecord().withValue("orderNo", orderNo).withValue("status", "NEW"));
      }
      return (new InsertAction().execute(new InsertInput(EsbH2TestSupport.TABLE_NAME_ORDER).withRecords(records).withTransaction(transaction)).getRecords());
   }



   /*******************************************************************************
    ** The outbox rows, oldest first.
    *******************************************************************************/
   private static List<QRecord> queryOutbox() throws Exception
   {
      return (new QueryAction().execute(new QueryInput(EsbH2TestSupport.OUTBOX_TABLE).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy(EsbOutboxTable.FIELD_ID)))).getRecords());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Integer countOutbox() throws Exception
   {
      return (new CountAction().execute(new CountInput(EsbH2TestSupport.OUTBOX_TABLE)).getCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setSent(QRecord row, Instant sentAt) throws Exception
   {
      new UpdateAction().execute(new UpdateInput(EsbH2TestSupport.OUTBOX_TABLE).withRecord(new QRecord()
         .withValue(EsbOutboxTable.FIELD_ID, row.getValue(EsbOutboxTable.FIELD_ID))
         .withValue(EsbOutboxTable.FIELD_STATUS, EsbOutboxTable.STATUS_SENT)
         .withValue(EsbOutboxTable.FIELD_SENT_AT, sentAt)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private MessageConsumer openConsumer(String queueName) throws Exception
   {
      if(receivingSession == null)
      {
         receivingSession = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false);
      }
      return (receivingSession.createConsumer(receivingSession.createQueue(queueName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<Message> receive(MessageConsumer consumer, int count) throws Exception
   {
      List<Message> messages = new ArrayList<>();
      for(int i = 0; i < count; i++)
      {
         Message message = consumer.receive(RECEIVE_TIMEOUT);
         assertThat(message).as("message " + (i + 1) + " of " + count).isNotNull();
         messages.add(message);
      }
      return (messages);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void waitFor(java.util.function.BooleanSupplier condition)
   {
      Instant deadline = Instant.now().plusSeconds(20);
      while(!condition.getAsBoolean() && Instant.now().isBefore(deadline))
      {
         SleepUtils.sleep(50, TimeUnit.MILLISECONDS);
      }
      assertThat(condition.getAsBoolean()).isTrue();
   }




   /*******************************************************************************
    ** Broker plugin that rolls back transactions that send to the "rejected" test addresses while
    ** rejecting is set - a send failure with the broker connected.
    *******************************************************************************/
   private static class RejectingPlugin implements ActiveMQServerMessagePlugin
   {
      private static volatile boolean rejecting = true;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void beforeSend(ServerSession session, Transaction tx, org.apache.activemq.artemis.api.core.Message message, boolean direct, boolean noAutoCreateQueue) throws ActiveMQException
      {
         if(rejecting && tx != null && message.getAddress() != null && message.getAddress().contains(REJECTED_PREFIX))
         {
            tx.markAsRollbackOnly(new ActiveMQException("rejected by test plugin"));
         }
      }
   }

}
