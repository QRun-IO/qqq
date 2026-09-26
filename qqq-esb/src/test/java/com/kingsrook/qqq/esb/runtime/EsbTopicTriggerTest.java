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

package com.kingsrook.qqq.esb.runtime;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for topic triggers in QEsbRuntime: a shared durable subscription,
 ** so each message is processed once across runtimes (nodes), and is kept
 ** while the runtime is stopped.
 *******************************************************************************/
class EsbTopicTriggerTest extends EsbRuntimeTestBase
{

   /*******************************************************************************
    ** Two runtimes (each with its own instance and its own connection to the one
    ** broker) with the same topic trigger: every message is processed exactly
    ** once, and both runtimes take part.
    *******************************************************************************/
   @Test
   void topicTriggerInTwoRuntimesProcessesEachMessageOnce() throws Exception
   {
      QInstance nodeA = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(TOPIC_NAME));

      QInstance nodeB = defineInstanceWithDestinations(SECOND_PROVIDER_NAME);
      nodeB.addProcess(defineRecordingProcess(PROCESS_NAME, null, false)
         .withSupplementalMetaData(new EsbProcessMetaData().withTrigger(new EsbTrigger().withDestinationName(TOPIC_NAME))));
      QContext.init(nodeB, new QSession());
      QContext.init(nodeA, new QSession());

      QEsbRuntime runtimeA = startRuntime(nodeA);
      QEsbRuntime runtimeB = startRuntime(nodeB);
      waitForState(runtimeA, TOPIC_TRIGGER_NAME, EsbTriggerState.RUNNING);
      waitForState(runtimeB, TOPIC_TRIGGER_NAME, EsbTriggerState.RUNNING);
      assertThat(EsbConnectionManager.getInstance().isConnected(SECOND_PROVIDER_NAME)).isTrue();

      List<String> sentIds = new ArrayList<>();
      for(int i = 0; i < 20; i++)
      {
         sentIds.add(sendEvent(TOPIC_NAME, Map.of("n", i)).getId());
      }

      waitFor("20 runs", () -> RecordingStep.getCompletedRuns().size() == 20);
      pause(1000);

      List<RecordedRun> runs = RecordingStep.getRuns();
      assertThat(runs.stream().map(run -> run.getEvents().get(0).getId()).toList()).containsExactlyInAnyOrderElementsOf(sentIds);
      assertThat(runs.stream().filter(run -> run.getQInstance() == nodeA).count()).isPositive();
      assertThat(runs.stream().filter(run -> run.getQInstance() == nodeB).count()).isPositive();
   }



   /*******************************************************************************
    ** A message published while the runtime is stopped is kept by the durable
    ** subscription, and processed once the runtime starts again.
    *******************************************************************************/
   @Test
   void messageSentWhileStoppedIsProcessedAfterStart() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(TOPIC_NAME));

      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      waitForState(runtime, TOPIC_TRIGGER_NAME, EsbTriggerState.RUNNING);
      runtime.stop();

      List<String> sentIds = new ArrayList<>();
      for(int i = 0; i < 3; i++)
      {
         sentIds.add(sendEvent(TOPIC_NAME, Map.of("n", i)).getId());
      }
      pause(500);
      assertThat(RecordingStep.getRuns()).isEmpty();

      QEsbRuntime restarted = startRuntime(QContext.getQInstance());
      waitFor("3 runs", () -> RecordingStep.getCompletedRuns().size() == 3);
      assertThat(RecordingStep.getRuns().stream().map(run -> run.getEvents().get(0).getId()).toList()).containsExactlyInAnyOrderElementsOf(sentIds);
      assertThat(restarted.getRunner(TOPIC_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.RUNNING);
   }



   /*******************************************************************************
    ** The trigger's shared durable subscription has the default name,
    ** processName.brokerTopicName: a consumer on that subscription gets what was
    ** published while the runtime was stopped.
    *******************************************************************************/
   @Test
   void subscriptionHasTheDefaultName() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(TOPIC_NAME));

      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      waitForState(runtime, TOPIC_TRIGGER_NAME, EsbTriggerState.RUNNING);
      runtime.stop();

      EsbEvent sent = sendEvent(TOPIC_NAME, Map.of());

      QEsbDestinationMetaData topicDestination = EsbInstanceMetaData.of(QContext.getQInstance()).getDestination(TOPIC_NAME);
      EsbConnectionManager    manager          = EsbConnectionManager.getInstance();
      try(Session session = manager.openSession(PROVIDER_NAME, false))
      {
         Topic           topic    = (Topic) manager.resolve(session, topicDestination);
         MessageConsumer consumer = session.createSharedDurableConsumer(topic, PROCESS_NAME + "." + getBrokerTopicName());
         Message         message  = consumer.receive(WAIT_TIMEOUT.toMillis());
         assertThat(message).isNotNull();
         assertThat(EsbEventCodec.fromMessage(message).getId()).isEqualTo(sent.getId());
      }
   }

}
