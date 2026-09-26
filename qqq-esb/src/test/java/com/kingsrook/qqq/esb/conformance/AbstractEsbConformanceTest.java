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

package com.kingsrook.qqq.esb.conformance;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.management.EsbBrokerAdapter;
import com.kingsrook.qqq.esb.management.EsbBrokerAdapters;
import com.kingsrook.qqq.esb.management.EsbBrokerCapabilities;
import com.kingsrook.qqq.esb.management.EsbBrokerNames;
import com.kingsrook.qqq.esb.management.EsbMessageBrowser;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.EsbTableEvent;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.qqq.esb.model.EsbTablePublication;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.EsbTriggerMode;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import com.kingsrook.qqq.esb.publish.EsbPublishAction;
import com.kingsrook.qqq.esb.publish.EsbPublishInput;
import com.kingsrook.qqq.esb.runtime.EsbRuntimeTestBase;
import com.kingsrook.qqq.esb.runtime.EsbTriggerState;
import com.kingsrook.qqq.esb.runtime.QEsbRuntime;
import jakarta.jms.Message;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 * The same behavior and management contract against both supported brokers.
 * The inherited memory QQQ instance and recording step are test fixtures; all
 * JMS traffic and management requests below go to the container broker.
 *
 * Coverage still awaiting other tasks is represented by named disabled tests,
 * so a passing Failsafe run is not mistaken for full spec section 11 coverage.
 ******************************************************************************/
public abstract class AbstractEsbConformanceTest extends EsbRuntimeTestBase
{
   /** The running Testcontainers broker. */
   protected abstract GenericContainer<?> broker();

   /** The provider type for this broker. */
   protected abstract EsbProviderType providerType();

   /** The mapped JMS or AMQP connection URL. */
   protected abstract String brokerUrl();

   /** The mapped broker management URL. */
   protected abstract String managementUrl();

   /** The broker test username. */
   protected abstract String brokerUsername();

   /** The broker test password. */
   protected abstract String brokerPassword();



   /*******************************************************************************
    ** Point both logical nodes at the real broker, including management.
    ******************************************************************************/
   @Override
   protected QInstance defineInstanceWithDestinations(String providerName)
   {
      QInstance instance = super.defineInstanceWithDestinations(providerName);
      EsbInstanceMetaData metadata = EsbInstanceMetaData.of(instance);
      configure(metadata.getProvider(PROVIDER_NAME));
      if(!PROVIDER_NAME.equals(providerName))
      {
         configure(metadata.getProvider(providerName));
      }
      return (instance);
   }



   /** Apply this container connection to one metadata provider. */
   private void configure(QEsbProviderMetaData provider)
   {
      provider.withType(providerType())
         .withUrl(brokerUrl())
         .withUsername(brokerUsername())
         .withPassword(brokerPassword())
         .withManagementUrl(managementUrl())
         .withManagementUsername(brokerUsername())
         .withManagementPassword(brokerPassword());
   }



   /** Three consumers on two runtimes process six queue messages once each. */
   @Test
   void queueConsumersCompeteWithConfiguredConcurrency() throws Exception
   {
      QInstance nodeA = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withConcurrency(2));
      QInstance nodeB = defineInstanceWithDestinations(SECOND_PROVIDER_NAME);
      nodeB.addProcess(defineRecordingProcess(PROCESS_NAME, null, false)
         .withSupplementalMetaData(new EsbProcessMetaData().withTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME))));
      QContext.init(nodeA, new QSession());
      RecordingStep.slowNextRuns(3, 1000L);
      QEsbRuntime runtimeA = startRuntime(nodeA);
      QEsbRuntime runtimeB = startRuntime(nodeB);
      waitForState(runtimeA, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      waitForState(runtimeB, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);

      List<String> ids = new ArrayList<>();
      for(int i = 0; i < 6; i++)
      {
         ids.add(sendEvent(QUEUE_NAME, Map.of("n", i)).getId());
      }

      waitFor("six queue runs", () -> RecordingStep.getCompletedRuns().size() == 6);
      assertThat(RecordingStep.getMaxConcurrentRuns()).isEqualTo(3);
      assertThat(RecordingStep.getRuns()).hasSize(6);
      assertThat(RecordingStep.getRuns().stream().map(run -> run.getEvents().get(0).getId()).toList())
         .containsExactlyInAnyOrderElementsOf(ids);
      assertThat(RecordingStep.getRuns()).anySatisfy(run -> assertThat(run.getQInstance()).isSameAs(nodeA));
      assertThat(RecordingStep.getRuns()).anySatisfy(run -> assertThat(run.getQInstance()).isSameAs(nodeB));
   }



   /** Two nodes share one durable topic subscription and retain messages while stopped. */
   @Test
   void sharedDurableTopicIsOnceAcrossNodesAndRetainsMessagesWhileDown() throws Exception
   {
      QInstance nodeA = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(TOPIC_NAME));
      QInstance nodeB = defineInstanceWithDestinations(SECOND_PROVIDER_NAME);
      nodeB.addProcess(defineRecordingProcess(PROCESS_NAME, null, false)
         .withSupplementalMetaData(new EsbProcessMetaData().withTrigger(new EsbTrigger().withDestinationName(TOPIC_NAME))));
      QContext.init(nodeA, new QSession());

      QEsbRuntime runtimeA = startRuntime(nodeA);
      QEsbRuntime runtimeB = startRuntime(nodeB);
      waitForState(runtimeA, TOPIC_TRIGGER_NAME, EsbTriggerState.RUNNING);
      waitForState(runtimeB, TOPIC_TRIGGER_NAME, EsbTriggerState.RUNNING);

      List<String> ids = new ArrayList<>();
      for(int i = 0; i < 10; i++)
      {
         ids.add(sendEvent(TOPIC_NAME, Map.of("n", i)).getId());
      }
      waitFor("ten topic runs", () -> RecordingStep.getCompletedRuns().size() == 10);
      assertThat(RecordingStep.getRuns().stream().map(run -> run.getEvents().get(0).getId()).toList())
         .containsExactlyInAnyOrderElementsOf(ids);

      runtimeA.stop();
      runtimeB.stop();
      EsbEvent retained = sendEvent(TOPIC_NAME, Map.of("whileDown", true));
      String subscriptionName = PROCESS_NAME + "." + getBrokerTopicName();
      String subscriptionQueue = EsbBrokerNames.subscriptionQueue(providerType(), getBrokerTopicName(), subscriptionName);
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, subscriptionQueue, 0, 10))
         .extracting(message -> message.getEvent().getId()).contains(retained.getId());
      waitForQueueDepth(EsbBrokerAdapters.forProvider(PROVIDER_NAME).orElseThrow(), subscriptionQueue, 1L);

      runtimeA.start(nodeA);
      waitFor("retained topic run", () -> RecordingStep.getCompletedRuns().size() == 11);
      assertThat(RecordingStep.getRuns().get(10).getEvents().get(0).getId()).isEqualTo(retained.getId());
   }



   /** Broker redelivery drives attempts and the final dead letter. */
   @Test
   void retriesUseBrokerDeliveryCountAndDeadLetterAfterBackoff() throws Exception
   {
      EsbTrigger trigger = new EsbTrigger().withDestinationName(QUEUE_NAME).withMaxAttempts(3).withRetryDelayMs(200);
      QInstance instance = defineInstanceWithTrigger(trigger);
      RecordingStep.failAlways();
      EsbEvent event = sendEvent(QUEUE_NAME, Map.of("failure", true));
      startRuntime(instance);

      String deadLetterQueue = trigger.getEffectiveDeadLetterDestinationName(PROCESS_NAME,
         EsbInstanceMetaData.of(instance).getDestination(QUEUE_NAME));
      List<Message> deadLetters = receiveAll(deadLetterQueue, WAIT_TIMEOUT);
      assertThat(deadLetters).hasSize(1);
      assertThat(RecordingStep.getRuns()).hasSize(3);
      assertThat(Duration.between(RecordingStep.getRuns().get(0).getStartedAt(),
         RecordingStep.getRuns().get(2).getStartedAt())).isGreaterThanOrEqualTo(Duration.ofMillis(350));
      assertThat(deadLetters.get(0).getIntProperty("qqqAttempts")).isEqualTo(3);
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, deadLetterQueue, 0, 10)).isEmpty();
      assertThat(event.getId()).isEqualTo(RecordingStep.getRuns().get(2).getEvents().get(0).getId());
   }



   /** A full batch produces one process invocation. */
   @Test
   void batchConsumesOneRunWithAllMessages() throws Exception
   {
      QInstance instance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME)
         .withMode(EsbTriggerMode.BATCH).withBatchSize(4).withBatchWaitMs(5000));
      QEsbRuntime runtime = startRuntime(instance);
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      List<String> ids = new ArrayList<>();
      for(int i = 0; i < 4; i++)
      {
         ids.add(sendEvent(QUEUE_NAME, Map.of("n", i)).getId());
      }
      waitFor("one batch run", () -> RecordingStep.getCompletedRuns().size() == 1);
      assertThat(RecordingStep.getRuns()).hasSize(1);
      assertThat(RecordingStep.getRuns().get(0).getEvents().stream().map(EsbEvent::getId).toList())
         .containsExactlyInAnyOrderElementsOf(ids);
   }



   /** Existing local trigger controls preserve pause state across a restart. */
   @Test
   void localPauseResumeAndRestartKeepQueuedMessages() throws Exception
   {
      QInstance instance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withStartPaused(true));
      QEsbRuntime runtime = startRuntime(instance);
      assertThat(runtime.getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.PAUSED);

      sendEvent(QUEUE_NAME, Map.of("n", 1));
      pause(500);
      assertThat(RecordingStep.getRuns()).isEmpty();

      runtime.getRunner(QUEUE_TRIGGER_NAME).resumeLocal();
      waitFor("first resumed run", () -> RecordingStep.getCompletedRuns().size() == 1);
      runtime.getRunner(QUEUE_TRIGGER_NAME).pauseLocal();
      // The worker closes its consumer after a one-second receive timeout.
      pause(2000);
      sendEvent(QUEUE_NAME, Map.of("n", 2));
      pause(500);
      assertThat(RecordingStep.getRuns()).hasSize(1);

      runtime.getRunner(QUEUE_TRIGGER_NAME).restartLocal();
      assertThat(runtime.getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.PAUSED);
      runtime.getRunner(QUEUE_TRIGGER_NAME).resumeLocal();
      waitFor("second resumed run", () -> RecordingStep.getCompletedRuns().size() == 2);
      runtime.getRunner(QUEUE_TRIGGER_NAME).restartLocal();
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      sendEvent(QUEUE_NAME, Map.of("n", 3));
      waitFor("run after restart", () -> RecordingStep.getCompletedRuns().size() == 3);
   }



   /** Validate table insert, update, delete, and explicit publication over JMS. */
   @Test
   void tableHooksAndExplicitPublishReachTheRealBroker() throws Exception
   {
      QInstance instance = defineInstanceWithDestinations(PROVIDER_NAME);
      EsbInstanceMetaData.of(instance).withInstanceName("conformance");
      EsbTableMetaData.ofOrWithNew(instance.getTable(TABLE_NAME_ORDER))
         .withPublication(new EsbTablePublication().withDestinationName(QUEUE_NAME)
            .withEvents(List.of(EsbTableEvent.INSERT, EsbTableEvent.UPDATE, EsbTableEvent.DELETE)));
      new QInstanceValidator().validate(instance);
      QContext.init(instance, new QSession());

      QRecord inserted = new InsertAction().execute(new InsertInput(TABLE_NAME_ORDER)
         .withRecords(List.of(new QRecord().withValue("orderNo", "CONF-1").withValue("status", "NEW"))))
         .getRecords().get(0);
      new UpdateAction().execute(new UpdateInput(TABLE_NAME_ORDER).withRecords(List.of(
         new QRecord().withValue("id", inserted.getValue("id")).withValue("status", "SHIPPED"))));
      new DeleteAction().execute(new DeleteInput(TABLE_NAME_ORDER).withPrimaryKeys(List.of(inserted.getValue("id"))));
      assertThat(new EsbPublishAction().execute(new EsbPublishInput()
         .withDestinationName(QUEUE_NAME).withType("qqq.test.explicit").withSourcePath("process/conformance")
         .withData(Map.of("orderNo", "CONF-1"))).getSuccess()).isTrue();

      List<EsbEvent> events = EsbMessageBrowser.browse(PROVIDER_NAME, getBrokerQueueName(), 0, 10).stream()
         .map(message -> message.getEvent()).toList();
      assertThat(events).extracting(EsbEvent::getType).containsExactly(
         "qqq.table.order.inserted", "qqq.table.order.updated", "qqq.table.order.deleted", "qqq.test.explicit");
      assertThat(events.subList(0, 3)).extracting(EsbEvent::getSubject)
         .containsOnly(String.valueOf(inserted.getValue("id")));
      assertThat(events.get(0).getData()).containsKey("record");
      assertThat(events.get(1).getData()).containsKeys("record", "oldRecord");
      assertThat(events.get(2).getData()).containsKey("oldRecord");
   }



   /** Caller commit publishes table events, while rollback emits none. */
   @Test
   void tableEventsFollowCallerCommitAndRollback() throws Exception
   {
      QInstance instance = defineInstanceWithDestinations(PROVIDER_NAME);
      EsbTableMetaData.ofOrWithNew(instance.getTable(TABLE_NAME_ORDER))
         .withPublication(new EsbTablePublication().withDestinationName(QUEUE_NAME)
            .withEvents(List.of(EsbTableEvent.INSERT)));
      new QInstanceValidator().validate(instance);
      QContext.init(instance, new QSession());

      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE_NAME_ORDER)))
      {
         new InsertAction().execute(new InsertInput(TABLE_NAME_ORDER)
            .withRecords(List.of(new QRecord().withValue("orderNo", "COMMIT-1")))
            .withTransaction(transaction));
         assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, getBrokerQueueName(), 0, 10)).isEmpty();
         transaction.commit();
      }
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, getBrokerQueueName(), 0, 10))
         .extracting(message -> message.getEvent().getType()).containsExactly("qqq.table.order.inserted");

      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE_NAME_ORDER)))
      {
         new InsertAction().execute(new InsertInput(TABLE_NAME_ORDER)
            .withRecords(List.of(new QRecord().withValue("orderNo", "ROLLBACK-1")))
            .withTransaction(transaction));
         transaction.rollback();
         transaction.commit();
      }
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, getBrokerQueueName(), 0, 10))
         .extracting(message -> message.getEvent().getType()).containsExactly("qqq.table.order.inserted");
   }



   /** A consumer recovers after a real broker container restart. */
   @Test
   @EnabledOnOs(OS.LINUX)
   void reconnectsAfterBrokerRestart() throws Exception
   {
      QInstance instance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      QEsbRuntime runtime = startRuntime(instance);
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);

      DockerClientFactory.instance().client().stopContainerCmd(broker().getContainerId()).exec();
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.CONNECTING);
      DockerClientFactory.instance().client().startContainerCmd(broker().getContainerId()).exec();
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      sendEvent(QUEUE_NAME, Map.of());
      waitFor("run after broker restart", () -> RecordingStep.getCompletedRuns().size() == 1);
   }



   /** The advertised management capability matrix matches this broker. */
   @Test
   void managementCapabilitiesMatchRealBroker() throws Exception
   {
      QContext.init(defineInstanceWithDestinations(PROVIDER_NAME), new QSession());
      EsbBrokerAdapter adapter = EsbBrokerAdapters.forProvider(PROVIDER_NAME).orElseThrow();
      EsbBrokerCapabilities capabilities = adapter.capabilities();
      assertThat(capabilities.browse()).isTrue();
      assertThat(capabilities.queueInfo()).isTrue();
      assertThat(capabilities.purge()).isTrue();
      assertThat(capabilities.pauseQueue()).isEqualTo(providerType() == EsbProviderType.ACTIVEMQ_ARTEMIS);
      assertThat(capabilities.deleteSelected()).isEqualTo(providerType() == EsbProviderType.ACTIVEMQ_ARTEMIS);
      assertThat(capabilities.deleteOlderThan()).isEqualTo(providerType() == EsbProviderType.ACTIVEMQ_ARTEMIS);
      assertThat(capabilities.move()).isEqualTo(providerType() == EsbProviderType.ACTIVEMQ_ARTEMIS);
   }



   /** Management reads and purges a queue on the running broker. */
   @Test
   void managementInfoBrowseAndPurgeOperateOnBrokerQueue() throws Exception
   {
      QInstance instance = defineInstanceWithDestinations(PROVIDER_NAME);
      QContext.init(instance, new QSession());
      EsbBrokerAdapter adapter = EsbBrokerAdapters.forProvider(PROVIDER_NAME).orElseThrow();
      String queue = getBrokerQueueName();
      assertThat(adapter.getQueueInfo(queue)).isEmpty();
      EsbEvent first = sendEvent(QUEUE_NAME, Map.of("n", 1));
      EsbEvent second = sendEvent(QUEUE_NAME, Map.of("n", 2));
      waitForQueueDepth(adapter, queue, 2L);
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, queue, 0, 1))
         .extracting(message -> message.getEvent().getId()).containsExactly(first.getId());
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, queue, 1, 1))
         .extracting(message -> message.getEvent().getId()).containsExactly(second.getId());
      assertThat(adapter.purgeQueue(queue)).isGreaterThanOrEqualTo(1);
      waitForQueueDepth(adapter, queue, 0L);
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, queue, 0, 10)).isEmpty();
   }



   /** Wait until the broker management API reports the expected depth. */
   private void waitForQueueDepth(EsbBrokerAdapter adapter, String queue, Long expected) throws Exception
   {
      Instant deadline = Instant.now().plus(WAIT_TIMEOUT);
      while(Instant.now().isBefore(deadline))
      {
         if(adapter.getQueueInfo(queue).map(info -> info.messageCount().equals(expected)).orElse(false))
         {
            return;
         }
         Thread.sleep(100);
      }
      assertThat(adapter.getQueueInfo(queue).orElseThrow().messageCount()).isEqualTo(expected);
   }



   /** Unsupported actions fail before making a broker request. */
   @Test
   void unsupportedManagementActionsFailClearly() throws Exception
   {
      QInstance instance = defineInstanceWithDestinations(PROVIDER_NAME);
      QContext.init(instance, new QSession());
      EsbBrokerAdapter adapter = EsbBrokerAdapters.forProvider(PROVIDER_NAME).orElseThrow();
      if(adapter.capabilities().pauseQueue())
      {
         return;
      }
      assertThatThrownBy(() -> adapter.pauseQueue(getBrokerQueueName())).isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> adapter.resumeQueue(getBrokerQueueName())).isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> adapter.deleteMessages(getBrokerQueueName(), List.of("id"))).isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> adapter.deleteMessagesOlderThan(getBrokerQueueName(), Instant.now())).isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> adapter.moveMessages(getBrokerQueueName(), List.of("id"), "other")).isInstanceOf(UnsupportedOperationException.class);
   }



   /** Staged cross-node control topic contract. */
   @Disabled("Task 13 control topic is not merged; integrate cross-node pause/resume/restart assertions before certifying either broker")
   @Test
   void controlTopicPausesResumesAndRestartsBothNodes()
   {
   }



   /** Staged dead-letter replay contract. */
   @Disabled("Task 14 replay process is not merged; assert replay success removes and failure retains dead letters")
   @Test
   void replayDeadLettersThroughOperateProcess()
   {
   }



   /** Staged process lifecycle publication contract. */
   @Disabled("Task 9 process publication is not merged; assert started/completed/failed over both brokers")
   @Test
   void processLifecyclePublications()
   {
   }



   /** Staged server-side permission checks for the Task 14 endpoints and processes. */
   @Disabled("Task 14 endpoints and operate processes are not merged; assert denied requests and actions for each required permission")
   @Test
   void permissionsDenyEndpointsAndOperateProcesses()
   {
   }

}
