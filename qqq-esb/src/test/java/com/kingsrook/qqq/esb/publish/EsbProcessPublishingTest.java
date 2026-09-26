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


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessEvent;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessPublication;
import com.kingsrook.qqq.esb.model.EsbTableEvent;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.qqq.esb.model.EsbTablePublication;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.runtime.EsbRuntimeTestBase;
import com.kingsrook.qqq.esb.runtime.EsbTriggerState;
import com.kingsrook.qqq.esb.runtime.QEsbRuntime;
import com.kingsrook.qqq.esb.stats.EsbCounterSnapshot;
import com.kingsrook.qqq.esb.stats.EsbStats;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for publishing process lifecycle events
 ** (EsbProcessLifecycleListener) - against the embedded Artemis broker.
 **
 ** The processes under test are syncOrder (from EsbTestBase, which succeeds)
 ** and failOrder (whose step throws).  Their events go to this test's queue
 ** (and, where a test says so, topic) - each test has its own broker-side
 ** names, from EsbRuntimeTestBase.
 *******************************************************************************/
class EsbProcessPublishingTest extends EsbRuntimeTestBase
{
   private static final String INSTANCE_NAME           = "orderService";
   private static final String PROCESS_NAME_FAIL_ORDER = "failOrder";
   private static final String PROCESS_NAME_FLAG_ORDER = "flagOrder";
   private static final String FAILURE_MESSAGE         = "Could not sync order 42";
   private static final String FLAGGED_STATUS          = "FLAGGED";

   private static final Duration RECEIVE_TIMEOUT          = Duration.ofSeconds(5);
   private static final Duration NOTHING_RECEIVED_TIMEOUT = Duration.ofMillis(500);



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      FlagOrderStep.reset();
   }



   /*******************************************************************************
    ** A successful run publishes started, then completed: the process's type,
    ** source, and data { processName, processUUID } (no error, and no causation
    ** id, as no triggered run caused it).
    *******************************************************************************/
   @Test
   void testSuccessfulRunPublishesStartedThenCompleted() throws Exception
   {
      setUpInstance(publication(QUEUE_NAME, EsbProcessEvent.STARTED, EsbProcessEvent.COMPLETED, EsbProcessEvent.FAILED));

      String           processUUID      = UUID.randomUUID().toString();
      RunProcessOutput runProcessOutput = runProcess(PROCESS_NAME_SYNC_ORDER, processUUID);
      assertThat(runProcessOutput.getException()).isEmpty();
      assertThat(SyncOrderStep.getRunCount()).isEqualTo(1);

      List<EsbEvent> events = receiveEvents(getBrokerQueueName(), RECEIVE_TIMEOUT);
      assertThat(events).extracting(EsbEvent::getType).containsExactly("qqq.process.syncOrder.started", "qqq.process.syncOrder.completed");
      assertThat(events).allSatisfy(event ->
      {
         assertThat(event.getSource()).isEqualTo("qqq://" + INSTANCE_NAME + "/process/" + PROCESS_NAME_SYNC_ORDER);
         assertThat(event.getSubject()).isNull();
         assertThat(event.getCausationId()).isNull();
         assertThat(event.getData())
            .containsEntry(EsbEventFactory.DATA_PROCESS_NAME, PROCESS_NAME_SYNC_ORDER)
            .containsEntry(EsbEventFactory.DATA_PROCESS_UUID, processUUID)
            .doesNotContainKey(EsbEventFactory.DATA_ERROR);
      });

      assertThat(EsbStats.getInstance().destination(QUEUE_NAME).published()).isEqualTo(2L);
   }



   /*******************************************************************************
    ** With only STARTED configured, each run - successful or failed - publishes
    ** just its started event.
    *******************************************************************************/
   @Test
   void testOnlyStartedPublishedWhenOnlyStartedConfigured() throws Exception
   {
      setUpInstance(publication(QUEUE_NAME, EsbProcessEvent.STARTED));

      runProcess(PROCESS_NAME_SYNC_ORDER, UUID.randomUUID().toString());
      assertThatThrownBy(() -> runProcess(PROCESS_NAME_FAIL_ORDER, UUID.randomUUID().toString())).isInstanceOf(QException.class);

      assertThat(receiveEvents(getBrokerQueueName(), RECEIVE_TIMEOUT))
         .extracting(EsbEvent::getType)
         .containsExactly("qqq.process.syncOrder.started", "qqq.process.failOrder.started");
   }



   /*******************************************************************************
    ** With only COMPLETED configured, only a successful run publishes, and only
    ** its completed event.
    *******************************************************************************/
   @Test
   void testOnlyCompletedPublishedWhenOnlyCompletedConfigured() throws Exception
   {
      setUpInstance(publication(QUEUE_NAME, EsbProcessEvent.COMPLETED));

      runProcess(PROCESS_NAME_SYNC_ORDER, UUID.randomUUID().toString());
      assertThatThrownBy(() -> runProcess(PROCESS_NAME_FAIL_ORDER, UUID.randomUUID().toString())).isInstanceOf(QException.class);

      assertThat(receiveEvents(getBrokerQueueName(), RECEIVE_TIMEOUT))
         .extracting(EsbEvent::getType)
         .containsExactly("qqq.process.syncOrder.completed");
   }



   /*******************************************************************************
    ** With only FAILED configured, only a failed run publishes, and only its
    ** failed event.
    *******************************************************************************/
   @Test
   void testOnlyFailedPublishedWhenOnlyFailedConfigured() throws Exception
   {
      setUpInstance(publication(QUEUE_NAME, EsbProcessEvent.FAILED));

      runProcess(PROCESS_NAME_SYNC_ORDER, UUID.randomUUID().toString());
      assertThatThrownBy(() -> runProcess(PROCESS_NAME_FAIL_ORDER, UUID.randomUUID().toString())).isInstanceOf(QException.class);

      assertThat(receiveEvents(getBrokerQueueName(), RECEIVE_TIMEOUT))
         .extracting(EsbEvent::getType)
         .containsExactly("qqq.process.failOrder.failed");
   }



   /*******************************************************************************
    ** Each publication gets only its own events: here, started to the queue and
    ** completed to the topic.
    *******************************************************************************/
   @Test
   void testEachPublicationGetsItsOwnEvents() throws Exception
   {
      setUpInstance(publication(QUEUE_NAME, EsbProcessEvent.STARTED), publication(TOPIC_NAME, EsbProcessEvent.COMPLETED));
      MessageConsumer topicConsumer = openTopicConsumer();

      runProcess(PROCESS_NAME_SYNC_ORDER, UUID.randomUUID().toString());

      assertThat(receiveEvents(getBrokerQueueName(), RECEIVE_TIMEOUT)).extracting(EsbEvent::getType).containsExactly("qqq.process.syncOrder.started");

      Message topicMessage = topicConsumer.receive(RECEIVE_TIMEOUT.toMillis());
      assertThat(topicMessage).isNotNull();
      assertThat(EsbEventCodec.fromMessage(topicMessage).getType()).isEqualTo("qqq.process.syncOrder.completed");
      assertThat(topicConsumer.receive(NOTHING_RECEIVED_TIMEOUT.toMillis())).isNull();
   }



   /*******************************************************************************
    ** A process with no publications (no ESB meta-data, or triggers only)
    ** publishes nothing - and the listener doesn't apply to it.
    *******************************************************************************/
   @Test
   void testProcessWithoutPublicationsPublishesNothing() throws Exception
   {
      QInstance qInstance = setUpInstance();
      EsbProcessMetaData.ofOrWithNew(qInstance.getProcess(PROCESS_NAME_FAIL_ORDER))
         .withPublication(new EsbProcessPublication().withDestinationName(QUEUE_NAME).withEvents(List.of()));

      runProcess(PROCESS_NAME_SYNC_ORDER, UUID.randomUUID().toString());
      assertThatThrownBy(() -> runProcess(PROCESS_NAME_FAIL_ORDER, UUID.randomUUID().toString())).isInstanceOf(QException.class);

      assertThat(receiveEvents(getBrokerQueueName(), NOTHING_RECEIVED_TIMEOUT)).isEmpty();
      assertThat(EsbStats.getInstance().destination(QUEUE_NAME).published()).isZero();

      EsbProcessLifecycleListener listener = new EsbProcessLifecycleListener();
      assertThat(listener.appliesTo(PROCESS_NAME_SYNC_ORDER)).isFalse();
      assertThat(listener.appliesTo(PROCESS_NAME_FAIL_ORDER)).isFalse();
      assertThat(listener.appliesTo("noSuchProcess")).isFalse();
      assertThat(listener.appliesTo(null)).isFalse();

      EsbProcessMetaData.ofOrWithNew(qInstance.getProcess(PROCESS_NAME_SYNC_ORDER))
         .withTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      assertThat(listener.appliesTo(PROCESS_NAME_SYNC_ORDER)).isFalse();

      EsbProcessMetaData.of(qInstance.getProcess(PROCESS_NAME_SYNC_ORDER))
         .withPublication(publication(TOPIC_NAME, EsbProcessEvent.FAILED));
      assertThat(listener.appliesTo(PROCESS_NAME_SYNC_ORDER)).isTrue();

      QContext.clear();
      assertThat(listener.appliesTo(PROCESS_NAME_SYNC_ORDER)).isFalse();
   }



   /*******************************************************************************
    ** A failing step publishes failed, with the step's error text - and the
    ** process still fails with the step's own exception.
    *******************************************************************************/
   @Test
   void testFailingStepPublishesFailedWithErrorText() throws Exception
   {
      setUpInstance(publication(QUEUE_NAME, EsbProcessEvent.STARTED, EsbProcessEvent.COMPLETED, EsbProcessEvent.FAILED));

      String processUUID = UUID.randomUUID().toString();
      assertThatThrownBy(() -> runProcess(PROCESS_NAME_FAIL_ORDER, processUUID))
         .isInstanceOf(QException.class)
         .hasMessage(FAILURE_MESSAGE);

      List<EsbEvent> events = receiveEvents(getBrokerQueueName(), RECEIVE_TIMEOUT);
      assertThat(events).extracting(EsbEvent::getType).containsExactly("qqq.process.failOrder.started", "qqq.process.failOrder.failed");

      EsbEvent failedEvent = events.get(1);
      assertThat(failedEvent.getData())
         .containsEntry(EsbEventFactory.DATA_PROCESS_NAME, PROCESS_NAME_FAIL_ORDER)
         .containsEntry(EsbEventFactory.DATA_PROCESS_UUID, processUUID)
         .containsEntry(EsbEventFactory.DATA_ERROR, FAILURE_MESSAGE);
      assertThat(events.get(0).getData()).doesNotContainKey(EsbEventFactory.DATA_ERROR);
   }



   /*******************************************************************************
    ** With the broker down, a process still runs and completes (within the 5 s
    ** connect timeout: after the first failed connect, publishes fail fast), and
    ** each event is counted as a publish failure, not thrown.
    *******************************************************************************/
   @Test
   void processCompletesWhenBrokerDown() throws Exception
   {
      setUpInstance(publication(QUEUE_NAME, EsbProcessEvent.STARTED, EsbProcessEvent.COMPLETED, EsbProcessEvent.FAILED));
      stopEmbeddedBroker();

      String           processUUID      = UUID.randomUUID().toString();
      long             startNanos       = System.nanoTime();
      RunProcessOutput runProcessOutput = runProcess(PROCESS_NAME_SYNC_ORDER, processUUID);
      Duration         elapsed          = Duration.ofNanos(System.nanoTime() - startNanos);

      assertThat(elapsed).isLessThan(Duration.ofSeconds(6));
      assertThat(runProcessOutput.getException()).isEmpty();
      assertThat(runProcessOutput.getProcessUUID()).isEqualTo(processUUID);
      assertThat(SyncOrderStep.getRunCount()).isEqualTo(1);

      EsbCounterSnapshot counters = EsbStats.getInstance().destination(QUEUE_NAME);
      assertThat(counters.publishFailures()).isEqualTo(2L);
      assertThat(counters.published()).isZero();
      assertThat(counters.lastError()).isNotBlank();
   }



   /*******************************************************************************
    ** With the broker down, a failing process fails with its step's exception -
    ** not a publishing error - and its events are counted as publish failures.
    *******************************************************************************/
   @Test
   void testFailedProcessThrowsItsOwnErrorWhenBrokerDown() throws Exception
   {
      setUpInstance(publication(QUEUE_NAME, EsbProcessEvent.STARTED, EsbProcessEvent.FAILED));
      stopEmbeddedBroker();

      assertThatThrownBy(() -> runProcess(PROCESS_NAME_FAIL_ORDER, UUID.randomUUID().toString()))
         .isInstanceOf(QException.class)
         .hasMessage(FAILURE_MESSAGE);

      assertThat(EsbStats.getInstance().destination(QUEUE_NAME).publishFailures()).isEqualTo(2L);
   }



   /*******************************************************************************
    ** Review focus 5 (self-trigger loop): flagOrder is triggered by the order
    ** table's events, and flags the order (an update of the same table) only if
    ** it isn't flagged yet - so the chain stops after one hop.
    **
    ** Inserting an order publishes the original event; flagOrder's run for it
    ** writes the flag, which publishes exactly one derived event, whose
    ** qqqcausationid is the original event's id.  flagOrder runs again for the
    ** derived event, and changes nothing.  Each run's completed event carries
    ** the id of the event that triggered it.
    *******************************************************************************/
   @Test
   void triggeredWritesCarryCausationId() throws Exception
   {
      QInstance qInstance = defineInstanceWithDestinations(PROVIDER_NAME);
      EsbInstanceMetaData.of(qInstance).withInstanceName(INSTANCE_NAME);
      EsbTableMetaData.ofOrWithNew(qInstance.getTable(TABLE_NAME_ORDER))
         .withPublication(new EsbTablePublication()
            .withDestinationName(TOPIC_NAME)
            .withEvents(List.of(EsbTableEvent.INSERT, EsbTableEvent.UPDATE)));
      qInstance.addProcess(defineProcess(PROCESS_NAME_FLAG_ORDER, FlagOrderStep.class)
         .withSupplementalMetaData(new EsbProcessMetaData()
            .withTrigger(new EsbTrigger().withDestinationName(TOPIC_NAME))
            .withPublication(publication(QUEUE_NAME, EsbProcessEvent.COMPLETED))));
      new QInstanceValidator().validate(qInstance);
      QContext.init(qInstance, new QSession());

      ////////////////////////////////////////////////////////////////////
      // watch every table event on the topic - subscribed (like the    //
      // trigger's durable subscription) before the insert is published //
      ////////////////////////////////////////////////////////////////////
      MessageConsumer topicConsumer = openTopicConsumer();
      QEsbRuntime     runtime       = startRuntime(qInstance);
      waitForState(runtime, PROCESS_NAME_FLAG_ORDER + "." + TOPIC_NAME, EsbTriggerState.RUNNING);

      new InsertAction().execute(new InsertInput(TABLE_NAME_ORDER).withRecord(new QRecord().withValue("orderNo", "ORD-1").withValue("status", "NEW")));

      waitFor("flagOrder to run twice", () -> FlagOrderStep.getRunCount() == 2);
      waitFor("both completed events to be published", () -> EsbStats.getInstance().destination(QUEUE_NAME).published().equals(2L));
      List<EsbEvent> completedEvents = receiveEvents(getBrokerQueueName(), RECEIVE_TIMEOUT);
      pause(500);

      List<EsbEvent> tableEvents = new ArrayList<>();
      Message        message     = topicConsumer.receive(RECEIVE_TIMEOUT.toMillis());
      while(message != null)
      {
         tableEvents.add(EsbEventCodec.fromMessage(message));
         message = topicConsumer.receive(NOTHING_RECEIVED_TIMEOUT.toMillis());
      }

      assertThat(tableEvents).extracting(EsbEvent::getType).containsExactly("qqq.table.order.inserted", "qqq.table.order.updated");
      EsbEvent originalEvent = tableEvents.get(0);
      EsbEvent derivedEvent  = tableEvents.get(1);
      assertThat(originalEvent.getCausationId()).isNull();
      assertThat(tableEvents).filteredOn(event -> event.getCausationId() != null).containsExactly(derivedEvent);
      assertThat(derivedEvent.getCausationId()).isEqualTo(originalEvent.getId());

      assertThat(FlagOrderStep.getRunCount()).isEqualTo(2);
      assertThat(FlagOrderStep.getWriteCount()).isEqualTo(1);
      assertThat(new GetAction().executeForRecord(new GetInput(TABLE_NAME_ORDER).withPrimaryKey(1)).getValueString("status")).isEqualTo(FLAGGED_STATUS);

      assertThat(completedEvents).extracting(EsbEvent::getType).containsOnly("qqq.process.flagOrder.completed");
      assertThat(completedEvents).extracting(EsbEvent::getCausationId).containsExactly(originalEvent.getId(), derivedEvent.getId());
   }



   /*******************************************************************************
    ** Enriching the ESB meta-data registers the process lifecycle listener once,
    ** however many times it runs.
    *******************************************************************************/
   @Test
   void testEnrichRegistersListenerOnce() throws Exception
   {
      QInstance qInstance = setUpInstance(publication(QUEUE_NAME, EsbProcessEvent.COMPLETED));
      EsbInstanceMetaData.of(qInstance).enrich(qInstance);

      assertThat(qInstance.getProcessLifecycleListeners())
         .extracting(QCodeReference::getName)
         .containsOnlyOnce(EsbProcessLifecycleListener.class.getName());
   }



   /*******************************************************************************
    ** Set up (validate, which enriches, and put in context) an instance with this
    ** test's queue and topic destinations, an instance name, the failOrder
    ** process, and these publications on both syncOrder and failOrder (none: no
    ** ESB process meta-data).
    *******************************************************************************/
   private QInstance setUpInstance(EsbProcessPublication... publications) throws QException
   {
      QInstance qInstance = defineInstanceWithDestinations(PROVIDER_NAME);
      EsbInstanceMetaData.of(qInstance).withInstanceName(INSTANCE_NAME);
      qInstance.addProcess(defineProcess(PROCESS_NAME_FAIL_ORDER, FailOrderStep.class));

      if(publications.length > 0)
      {
         for(String processName : List.of(PROCESS_NAME_SYNC_ORDER, PROCESS_NAME_FAIL_ORDER))
         {
            EsbProcessMetaData esbProcessMetaData = EsbProcessMetaData.ofOrWithNew(qInstance.getProcess(processName));
            for(EsbProcessPublication publication : publications)
            {
               esbProcessMetaData.withPublication(publication);
            }
         }
      }

      new QInstanceValidator().validate(qInstance);
      QContext.init(qInstance, new QSession());
      return (qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static EsbProcessPublication publication(String destinationName, EsbProcessEvent... events)
   {
      return (new EsbProcessPublication()
         .withDestinationName(destinationName)
         .withEvents(List.of(events)));
   }



   /*******************************************************************************
    ** A backend-only process with one step.
    *******************************************************************************/
   private static QProcessMetaData defineProcess(String processName, Class<? extends BackendStep> stepClass)
   {
      return (new QProcessMetaData()
         .withName(processName)
         .withStep(new QBackendStepMetaData()
            .withName("run")
            .withCode(new QCodeReference(stepClass))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RunProcessOutput runProcess(String processName, String processUUID) throws QException
   {
      return (new RunProcessAction().execute(new RunProcessInput()
         .withProcessName(processName)
         .withProcessUUID(processUUID)
         .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP)));
   }



   /*******************************************************************************
    ** Receive (and parse) every message now on a broker queue.
    *******************************************************************************/
   private List<EsbEvent> receiveEvents(String brokerQueueName, Duration firstWait) throws Exception
   {
      List<EsbEvent> events = new ArrayList<>();
      for(Message message : receiveAll(brokerQueueName, firstWait))
      {
         events.add(EsbEventCodec.fromMessage(message));
      }
      return (events);
   }



   /*******************************************************************************
    ** A (non-durable) subscriber to this test's topic - which must exist before
    ** a message is published, to receive it.  Closed by the base class's reset
    ** of the connection manager.
    *******************************************************************************/
   private MessageConsumer openTopicConsumer() throws Exception
   {
      Session session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false);
      return (session.createConsumer(session.createTopic(getBrokerTopicName())));
   }



   /*******************************************************************************
    * Backend step for the failOrder process - always throws.
    *******************************************************************************/
   public static class FailOrderStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         throw (new QException(FAILURE_MESSAGE));
      }
   }



   /*******************************************************************************
    * Backend step for the flagOrder process: for each triggering event, sets the
    * order's status to FLAGGED - unless it already is (so its own update event
    * doesn't cause another write).  Counts its runs and its writes.
    *******************************************************************************/
   public static class FlagOrderStep implements BackendStep
   {
      private static final AtomicInteger runCount   = new AtomicInteger(0);
      private static final AtomicInteger writeCount = new AtomicInteger(0);



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      @SuppressWarnings("unchecked")
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         for(EsbEvent event : (List<EsbEvent>) runBackendStepInput.getValue("esbMessages"))
         {
            Integer orderId = Integer.valueOf(event.getSubject());
            QRecord order   = new GetAction().executeForRecord(new GetInput(TABLE_NAME_ORDER).withPrimaryKey(orderId));
            if(order != null && !FLAGGED_STATUS.equals(order.getValueString("status")))
            {
               new UpdateAction().execute(new UpdateInput(TABLE_NAME_ORDER).withRecord(new QRecord()
                  .withValue("id", orderId)
                  .withValue("status", FLAGGED_STATUS)));
               writeCount.incrementAndGet();
            }
         }
         runCount.incrementAndGet();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static int getRunCount()
      {
         return (runCount.get());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static int getWriteCount()
      {
         return (writeCount.get());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static void reset()
      {
         runCount.set(0);
         writeCount.set(0);
      }
   }

}
