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

package com.kingsrook.qqq.esb.processes;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbCausation;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.management.EsbMessageBrowser;
import com.kingsrook.qqq.esb.management.MockManagementServer;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.EsbTriggerMode;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 * The public operate processes are discoverable and enforce their permission
 * even when invoked directly through RunProcessAction.
 ******************************************************************************/
class EsbOperateProcessesTest extends EsbTestBase
{
   private static final List<String> OPERATE = List.of("esbPauseTrigger", "esbResumeTrigger", "esbRestartTrigger", "esbReplayDeadLetters", "esbPauseQueue", "esbResumeQueue", "esbMoveMessages");
   private static final List<String> DELETE  = List.of("esbPurgeQueue", "esbDeleteMessages");



   /*******************************************************************************
    ** Every contract process exists, with the shared permission base name.
    *******************************************************************************/
   @Test
   void registersNinePermissionScopedProcesses() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(instance, "com.kingsrook.qqq.esb.processes");

      for(String name : OPERATE)
      {
         assertThat(instance.getProcess(name)).as(name).isNotNull();
         assertThat(instance.getProcess(name).getPermissionRules().getPermissionBaseName()).isEqualTo("esbOperate");
      }
      for(String name : DELETE)
      {
         assertThat(instance.getProcess(name)).as(name).isNotNull();
         assertThat(instance.getProcess(name).getPermissionRules().getPermissionBaseName()).isEqualTo("esbDelete");
      }
   }



   /*******************************************************************************
    ** Direct process calls cannot bypass the service-level permissions.
    *******************************************************************************/
   @Test
   void deniesDirectRunsWithoutOperateOrDeletePermission() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(instance, "com.kingsrook.qqq.esb.processes");
      QContext.init(instance, new QSession());

      for(String name : OPERATE)
      {
         assertThatThrownBy(() -> new RunProcessAction().execute(new RunProcessInput().withProcessName(name))).as(name)
            .hasMessageContaining("Permission denied");
      }
      for(String name : DELETE)
      {
         assertThatThrownBy(() -> new RunProcessAction().execute(new RunProcessInput().withProcessName(name))).as(name)
            .hasMessageContaining("Permission denied");
      }
   }



   /*******************************************************************************
    ** Management actions use the broker adapter, with the contract output.
    *******************************************************************************/
   @Test
   void operatesBrokerQueuesThroughManagementApi() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(instance, "com.kingsrook.qqq.esb.processes");
      try(MockManagementServer server = new MockManagementServer())
      {
         com.kingsrook.qqq.esb.model.EsbInstanceMetaData.of(instance).getProvider(PROVIDER_NAME).withManagementUrl(server.getBaseUrl());
         QContext.init(instance, new QSession().withPermission("esbOperate.hasAccess").withPermission("esbDelete.hasAccess"));

         queueResponse(server, "pause()", "null");
         assertThat(run("esbPauseQueue", Map.of("providerName", PROVIDER_NAME, "brokerQueueName", "orders")).getValue("count")).isEqualTo(0);
         queueResponse(server, "resume()", "null");
         run("esbResumeQueue", Map.of("providerName", PROVIDER_NAME, "brokerQueueName", "orders"));
         queueResponse(server, "removeAllMessages()", "3");
         assertThat(run("esbPurgeQueue", Map.of("providerName", PROVIDER_NAME, "brokerQueueName", "orders")).getValue("count")).isEqualTo(3L);
         queueResponse(server, "removeMessages(java.lang.String)", "2");
         assertThat(run("esbDeleteMessages", Map.of("providerName", PROVIDER_NAME, "brokerQueueName", "orders", "messageIds", "ID:a,ID:b")).getValue("count")).isEqualTo(2);
         queueResponse(server, "moveMessages(java.lang.String,java.lang.String)", "1");
         assertThat(run("esbMoveMessages", Map.of("providerName", PROVIDER_NAME, "brokerQueueName", "orders", "messageIds", "ID:a", "toBrokerQueueName", "archive")).getValue("count")).isEqualTo(1);

         assertThat(server.getRequests().stream().filter(request -> request.rawPath().endsWith("/exec")).map(request -> new org.json.JSONObject(request.body()).getString("operation")))
            .containsExactly("pause()", "resume()", "removeAllMessages()", "removeMessages(java.lang.String)", "moveMessages(java.lang.String,java.lang.String)");
      }
   }



   /*******************************************************************************
    ** Group permissions are independent and enforced before I/O.
    *******************************************************************************/
   @Test
   void keepsOperateAndDeletePermissionsSeparate() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(instance, "com.kingsrook.qqq.esb.processes");

      QContext.init(instance, new QSession().withPermission("esbOperate.hasAccess"));
      assertThatThrownBy(() -> run("esbPurgeQueue", Map.of("providerName", PROVIDER_NAME, "brokerQueueName", "orders"))).hasMessageContaining("Permission denied");
      QContext.init(instance, new QSession().withPermission("esbDelete.hasAccess"));
      assertThatThrownBy(() -> run("esbPauseQueue", Map.of("providerName", PROVIDER_NAME, "brokerQueueName", "orders"))).hasMessageContaining("Permission denied");
   }



   /*******************************************************************************
    ** A missing management API and unsupported RabbitMQ action fail clearly.
    *******************************************************************************/
   @Test
   void rejectsUnavailableAndUnsupportedQueueActions() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(instance, "com.kingsrook.qqq.esb.processes");
      QContext.init(instance, new QSession().withPermission("esbOperate.hasAccess"));
      assertThatThrownBy(() -> run("esbPauseQueue", Map.of("providerName", PROVIDER_NAME, "brokerQueueName", "orders"))).hasMessageContaining("management");

      com.kingsrook.qqq.esb.model.EsbInstanceMetaData.of(instance).getProvider(PROVIDER_NAME)
         .withType(EsbProviderType.RABBITMQ).withManagementUrl("http://127.0.0.1:1");
      assertThatThrownBy(() -> run("esbPauseQueue", Map.of("providerName", PROVIDER_NAME, "brokerQueueName", "orders"))).hasMessageContaining("unsupported");
   }



   /*******************************************************************************
    ** Delete by time uses the older-than operation, never a broad purge.
    *******************************************************************************/
   @Test
   void deletesOnlyMessagesOlderThanTheGivenInstant() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(instance, "com.kingsrook.qqq.esb.processes");
      try(MockManagementServer server = new MockManagementServer())
      {
         EsbInstanceMetaData.of(instance).getProvider(PROVIDER_NAME).withManagementUrl(server.getBaseUrl());
         QContext.init(instance, new QSession().withPermission("esbDelete.hasAccess"));
         queueResponse(server, "removeMessages(java.lang.String)", "2");

         RunProcessOutput output = run("esbDeleteMessages", Map.of("providerName", PROVIDER_NAME,
            "brokerQueueName", "orders", "olderThan", "2026-09-25T12:00:00Z"));

         assertThat(output.getValue("count")).isEqualTo(2);
         JSONObject request = new JSONObject(server.getRequests().get(1).body());
         assertThat(request.getString("operation")).isEqualTo("removeMessages(java.lang.String)");
         assertThat(request.getJSONArray("arguments").getString(0)).contains("AMQTimestamp <");
      }
   }



   /*******************************************************************************
    ** Replay invokes the target process directly and removes a successful dead
    ** letter in the same transaction.
    *******************************************************************************/
   @Test
   void replaySuccessRemovesSelectedDeadLetter() throws Exception
   {
      QInstance instance = replayInstance(false);
      String messageId = sendDeadLetter("orders.dlq");
      String otherId = sendDeadLetter("orders.dlq");

      RunProcessOutput output = run("esbReplayDeadLetters", Map.of("triggerName", "syncOrder.orders", "messageIds", messageId));
      assertThat(output.getValue("count")).isEqualTo(1);
      assertThat(SyncOrderStep.getRunCount()).isEqualTo(1);
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, "orders.dlq", 0, 10))
         .extracting(message -> message.getMessageId()).containsExactly(otherId);
      assertThat(QContext.getQInstance()).isSameAs(instance);
   }



   /*******************************************************************************
    ** Failed replay rolls back the receive, leaving the dead letter on broker.
    *******************************************************************************/
   @Test
   void replayFailureLeavesDeadLetter() throws Exception
   {
      replayInstance(true);
      String messageId = sendDeadLetter("orders.dlq");

      assertThatThrownBy(() -> run("esbReplayDeadLetters", Map.of("triggerName", "failingReplay.orders", "messageIds", messageId)))
         .hasMessageContaining("replay failed");
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, "orders.dlq", 0, 10)).hasSize(1);
   }



   /*******************************************************************************
    ** all=true processes every pending dead letter and reports its count.
    *******************************************************************************/
   @Test
   void replayAllDrainsDeadLetterQueue() throws Exception
   {
      replayInstance(false);
      sendDeadLetter("orders.dlq");
      sendDeadLetter("orders.dlq");

      assertThat(run("esbReplayDeadLetters", Map.of("triggerName", "syncOrder.orders", "all", true)).getValue("count")).isEqualTo(2);
      assertThat(SyncOrderStep.getRunCount()).isEqualTo(2);
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, "orders.dlq", 0, 10)).isEmpty();
   }



   /*******************************************************************************
    ** Replay is always one event at a time, even for BATCH triggers.
    *******************************************************************************/
   @Test
   void replaySetsCausationForBatchTrigger() throws Exception
   {
      QInstance instance = replayInstance(false);
      QProcessMetaData target = instance.getProcess(PROCESS_NAME_SYNC_ORDER);
      EsbProcessMetaData.of(target).getTriggers().get(0).withMode(EsbTriggerMode.BATCH);
      target.getBackendStep("sync").setCode(new QCodeReference(CapturingCausationStep.class));
      EsbEvent event = EsbEventFactory.custom("test", "test", "qqq.test.replay", Map.of("id", 1));
      String messageId = sendDeadLetter("orders.dlq", event);

      run("esbReplayDeadLetters", Map.of("triggerName", "syncOrder.orders", "messageIds", messageId));

      assertThat(CapturingCausationStep.CAUSATION.get()).isEqualTo(event.getId());
   }



   /*******************************************************************************
    ** A process that only publishes cannot make unknown-trigger lookup crash.
    *******************************************************************************/
   @Test
   void unknownReplayTriggerFailsClearly() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      instance.getProcess(PROCESS_NAME_SYNC_ORDER)
         .withSupplementalMetaData(new EsbProcessMetaData().withPublication(new com.kingsrook.qqq.esb.model.EsbProcessPublication()));
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(instance, "com.kingsrook.qqq.esb.processes");
      QContext.init(instance, new QSession().withPermission("esbOperate.hasAccess"));

      assertThatThrownBy(() -> run("esbReplayDeadLetters", Map.of("triggerName", "missing.orders", "all", true)))
         .isInstanceOf(QException.class).hasMessageContaining("Unknown ESB trigger");
   }



   /*******************************************************************************
    ** Trigger operations publish the required action to the control topic.
    *******************************************************************************/
   @Test
   void triggerProcessesSendPauseResumeAndRestart() throws Exception
   {
      replayInstance(false);
      try(Session session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false);
          MessageConsumer consumer = session.createConsumer(session.createTopic("qqq.esb.control")))
      {
         for(String action : List.of("Pause", "Resume", "Restart"))
         {
            run("esb" + action + "Trigger", Map.of("triggerName", "syncOrder.orders"));
            Message message = consumer.receive(2000);
            assertThat(message).as(action).isInstanceOf(TextMessage.class);
            JSONObject body = new JSONObject(((TextMessage) message).getText());
            assertThat(body.getString("action")).isEqualTo(action.toUpperCase());
            assertThat(body.getString("triggerName")).isEqualTo("syncOrder.orders");
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QInstance replayInstance(boolean failing) throws Exception
   {
      QInstance instance = QContext.getQInstance();
      try(Session session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false);
          MessageConsumer consumer = session.createConsumer(session.createQueue("orders.dlq")))
      {
         while(consumer.receiveNoWait() != null)
         {
            // isolate each replay test from the embedded broker's prior messages
         }
      }
      EsbInstanceMetaData.of(instance).withDestination(new QEsbDestinationMetaData()
         .withName("orders").withType(EsbDestinationType.QUEUE).withProviderName(PROVIDER_NAME));
      QProcessMetaData target;
      if(failing)
      {
         target = new QProcessMetaData().withName("failingReplay")
            .withStep(new QBackendStepMetaData().withName("fail").withCode(new QCodeReference(FailingReplayStep.class)));
         instance.addProcess(target);
      }
      else
      {
         target = instance.getProcess(PROCESS_NAME_SYNC_ORDER);
      }
      target.withSupplementalMetaData(new EsbProcessMetaData().withTrigger(new EsbTrigger().withDestinationName("orders")));
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(instance, "com.kingsrook.qqq.esb.processes");
      QContext.init(instance, new QSession().withPermission("esbOperate.hasAccess"));
      return (instance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String sendDeadLetter(String queueName) throws Exception
   {
      EsbEvent event = EsbEventFactory.custom("test", "test", "qqq.test.replay", Map.of("id", 1));
      return (sendDeadLetter(queueName, event));
   }



   /*******************************************************************************
    ** Send a specific event so tests can assert its causation ID.
    *******************************************************************************/
   private static String sendDeadLetter(String queueName, EsbEvent event) throws Exception
   {
      try(Session session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
      {
         Message message = EsbEventCodec.toMessage(session, event);
         session.createProducer(session.createQueue(queueName)).send(message);
         return (message.getJMSMessageID());
      }
   }



   /*******************************************************************************
    ** Captures causation seen by the replayed process.
    *******************************************************************************/
   public static class CapturingCausationStep implements BackendStep
   {
      private static final AtomicReference<String> CAUSATION = new AtomicReference<>();

      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output)
      {
         CAUSATION.set(EsbCausation.current());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class FailingReplayStep implements BackendStep
   {
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         throw (new QException("replay failed"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RunProcessOutput run(String name, Map<String, ? extends Serializable> values) throws Exception
   {
      RunProcessInput input = new RunProcessInput().withProcessName(name);
      values.forEach(input::withValue);
      return (new RunProcessAction().execute(input));
   }



   /*******************************************************************************
    ** A search and exec response from the mock Jolokia endpoint.
    *******************************************************************************/
   private static void queueResponse(MockManagementServer server, String operation, String value)
   {
      server.withResponse(200, "{\"status\":200,\"value\":[\"org.apache.activemq.artemis:queue=\\\"orders\\\",component=addresses,subcomponent=queues\"]}");
      server.withResponse(200, "{\"status\":200,\"value\":" + value + ",\"request\":{\"operation\":\"" + operation + "\"}}");
   }
}
