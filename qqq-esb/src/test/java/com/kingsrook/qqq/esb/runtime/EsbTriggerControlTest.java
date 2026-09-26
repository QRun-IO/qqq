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
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.management.EsbMessageBrowser;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import jakarta.jms.Session;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for EsbTriggerControl and EsbControlChannel: pause, resume and
 ** restart reach every runtime (node) on the broker - here, two runtimes, each
 ** with its own instance and provider (connection), on one embedded broker.
 *******************************************************************************/
class EsbTriggerControlTest extends EsbRuntimeTestBase
{
   ////////////////////////////////////////////////////////////////////////
   // a paused trigger's consumers close within one receive timeout (1 s) //
   ////////////////////////////////////////////////////////////////////////
   private static final Integer CONSUMERS_CLOSED_MS = 2000;



   /*******************************************************************************
    ** Pausing on runtime A pauses the trigger on A and B: messages sent then are
    ** not processed, and wait on the broker.
    *******************************************************************************/
   @Test
   void pauseOnOneRuntimePausesBothAndMessagesAccumulate() throws Exception
   {
      Nodes nodes = startTwoNodes();

      EsbTriggerControl.pause(QUEUE_TRIGGER_NAME);
      waitForState(nodes.a(), QUEUE_TRIGGER_NAME, EsbTriggerState.PAUSED);
      waitForState(nodes.b(), QUEUE_TRIGGER_NAME, EsbTriggerState.PAUSED);
      pause(CONSUMERS_CLOSED_MS);

      for(int i = 0; i < 5; i++)
      {
         sendEvent(QUEUE_NAME, Map.of("n", i));
      }
      pause(1500);

      assertThat(RecordingStep.getRuns()).isEmpty();
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, getBrokerQueueName(), 0, 50)).hasSize(5);
      assertThat(nodes.a().getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.PAUSED);
      assertThat(nodes.b().getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.PAUSED);
   }



   /*******************************************************************************
    ** Resuming (from either runtime) restarts consumption on both, and the
    ** messages that accumulated while paused are all processed.
    *******************************************************************************/
   @Test
   void resumeDrainsTheAccumulatedMessages() throws Exception
   {
      Nodes nodes = startTwoNodes();

      EsbTriggerControl.pause(QUEUE_TRIGGER_NAME);
      waitForState(nodes.a(), QUEUE_TRIGGER_NAME, EsbTriggerState.PAUSED);
      waitForState(nodes.b(), QUEUE_TRIGGER_NAME, EsbTriggerState.PAUSED);
      pause(CONSUMERS_CLOSED_MS);

      List<String> sentIds = new ArrayList<>();
      for(int i = 0; i < 10; i++)
      {
         sentIds.add(sendEvent(QUEUE_NAME, Map.of("n", i)).getId());
      }
      pause(1000);
      assertThat(RecordingStep.getRuns()).isEmpty();

      QContext.init(nodes.instanceB(), new QSession());
      EsbTriggerControl.resume(QUEUE_TRIGGER_NAME);
      waitForState(nodes.a(), QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      waitForState(nodes.b(), QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);

      waitFor("10 runs", () -> RecordingStep.getCompletedRuns().size() == 10);
      assertThat(RecordingStep.getRuns().stream().map(run -> run.getEvents().get(0).getId()).toList()).containsExactlyInAnyOrderElementsOf(sentIds);
      assertThat(EsbMessageBrowser.browse(SECOND_PROVIDER_NAME, getBrokerQueueName(), 0, 50)).isEmpty();
   }



   /*******************************************************************************
    ** Restarting builds new consumers on both runtimes, which then process
    ** messages as before.
    *******************************************************************************/
   @Test
   void restartRecreatesConsumersOnBothRuntimes() throws Exception
   {
      Nodes nodes       = startTwoNodes();
      Long  generationA = nodes.a().getRunner(QUEUE_TRIGGER_NAME).getGeneration();
      Long  generationB = nodes.b().getRunner(QUEUE_TRIGGER_NAME).getGeneration();

      EsbTriggerControl.restart(QUEUE_TRIGGER_NAME);
      waitFor("new consumer generations on both runtimes", () ->
         nodes.a().getRunner(QUEUE_TRIGGER_NAME).getGeneration() > generationA
            && nodes.b().getRunner(QUEUE_TRIGGER_NAME).getGeneration() > generationB);
      waitForState(nodes.a(), QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      waitForState(nodes.b(), QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);

      for(int i = 0; i < 4; i++)
      {
         sendEvent(QUEUE_NAME, Map.of("n", i));
      }
      waitFor("4 runs", () -> RecordingStep.getCompletedRuns().size() == 4);
   }



   /*******************************************************************************
    ** A restart of a paused trigger leaves it paused (on every node).
    *******************************************************************************/
   @Test
   void restartOfAPausedTriggerLeavesItPaused() throws Exception
   {
      Nodes nodes      = startTwoNodes();
      Long  generation = nodes.b().getRunner(QUEUE_TRIGGER_NAME).getGeneration();

      EsbTriggerControl.pause(QUEUE_TRIGGER_NAME);
      waitForState(nodes.b(), QUEUE_TRIGGER_NAME, EsbTriggerState.PAUSED);
      EsbTriggerControl.restart(QUEUE_TRIGGER_NAME);
      waitFor("a new consumer generation", () -> nodes.b().getRunner(QUEUE_TRIGGER_NAME).getGeneration() > generation);

      assertThat(nodes.a().getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.PAUSED);
      assertThat(nodes.b().getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.PAUSED);
   }



   /*******************************************************************************
    ** A trigger that isn't in the QContext's instance is a QException, for each
    ** action - as is a call with no QContext instance.
    *******************************************************************************/
   @Test
   void unknownTriggerIsAQException()
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));

      assertThatThrownBy(() -> EsbTriggerControl.pause("noSuchProcess.orderQueue")).isInstanceOf(QException.class).hasMessageContaining("noSuchProcess.orderQueue");
      assertThatThrownBy(() -> EsbTriggerControl.resume(PROCESS_NAME + ".noSuchDestination")).isInstanceOf(QException.class).hasMessageContaining("noSuchDestination");
      assertThatThrownBy(() -> EsbTriggerControl.restart(null)).isInstanceOf(QException.class);

      QContext.clear();
      assertThatThrownBy(() -> EsbTriggerControl.pause(QUEUE_TRIGGER_NAME)).isInstanceOf(QException.class);
   }



   /*******************************************************************************
    ** With the broker down, the control message can't be sent: a QException, and
    ** nothing changes.
    *******************************************************************************/
   @Test
   void controlWithTheBrokerDownIsAQException() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      stopEmbeddedBroker();
      EsbConnectionManager.getInstance().closeAll();

      assertThatThrownBy(() -> EsbTriggerControl.pause(QUEUE_TRIGGER_NAME)).isInstanceOf(QException.class);
   }



   /*******************************************************************************
    ** Control messages that a runtime can't use - not JSON, an unknown action,
    ** no trigger name, or a trigger it doesn't run - are ignored, and later ones
    ** still apply.
    *******************************************************************************/
   @Test
   void controlMessagesARuntimeCantUseAreIgnored() throws Exception
   {
      Nodes nodes = startTwoNodes();

      sendControlText("not json");
      sendControlText("{\"action\":\"EXPLODE\",\"triggerName\":\"" + QUEUE_TRIGGER_NAME + "\"}");
      sendControlText("{\"action\":\"PAUSE\"}");
      EsbControlChannel.send(PROVIDER_NAME, EsbControlChannel.Action.PAUSE, "otherProcess.otherQueue");
      EsbTriggerControl.pause(QUEUE_TRIGGER_NAME);

      waitForState(nodes.a(), QUEUE_TRIGGER_NAME, EsbTriggerState.PAUSED);
      waitForState(nodes.b(), QUEUE_TRIGGER_NAME, EsbTriggerState.PAUSED);
   }



   /*******************************************************************************
    ** After the broker restarts, each runtime's control channel listens again.
    *******************************************************************************/
   @Test
   void controlChannelListensAgainAfterReconnect() throws Exception
   {
      Nodes nodes = startTwoNodes();

      stopEmbeddedBroker();
      waitFor("the control channel to notice", () -> !EsbConnectionManager.getInstance().isConnected(PROVIDER_NAME));
      startEmbeddedBroker();
      waitForState(nodes.a(), QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      waitForState(nodes.b(), QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      waitForListening(nodes);

      EsbTriggerControl.pause(QUEUE_TRIGGER_NAME);
      waitForState(nodes.a(), QUEUE_TRIGGER_NAME, EsbTriggerState.PAUSED);
      waitForState(nodes.b(), QUEUE_TRIGGER_NAME, EsbTriggerState.PAUSED);
   }



   /*******************************************************************************
    ** Once a runtime stops, its control channel stops listening.
    *******************************************************************************/
   @Test
   void stopClosesTheControlChannel() throws Exception
   {
      Nodes             nodes   = startTwoNodes();
      EsbControlChannel channel = nodes.a().getControlChannel();

      nodes.a().stop();
      assertThat(channel.isListening(PROVIDER_NAME)).isFalse();
      assertThat(nodes.a().getControlChannel()).isNull();
      assertThat(nodes.b().getControlChannel().isListening(SECOND_PROVIDER_NAME)).isTrue();
   }



   /*******************************************************************************
    ** Two runtimes with the same queue trigger: A on the base provider, B on a
    ** second provider (its own connection to the same broker).  Both are
    ** started, running, and listening for control messages; the QContext is
    ** node A's.
    *******************************************************************************/
   private Nodes startTwoNodes()
   {
      QInstance nodeA = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));

      QInstance nodeB = defineInstanceWithDestinations(SECOND_PROVIDER_NAME);
      nodeB.addProcess(defineRecordingProcess(PROCESS_NAME, null, false)
         .withSupplementalMetaData(new EsbProcessMetaData().withTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME))));
      QContext.init(nodeB, new QSession());
      QContext.init(nodeA, new QSession());

      Nodes nodes = new Nodes(startRuntime(nodeA), startRuntime(nodeB), nodeB);
      waitForState(nodes.a(), QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      waitForState(nodes.b(), QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      waitForListening(nodes);
      return (nodes);
   }



   /*******************************************************************************
    ** Wait for both runtimes' control channels to be listening.
    *******************************************************************************/
   private void waitForListening(Nodes nodes)
   {
      waitFor("both control channels to listen", () ->
         nodes.a().getControlChannel().isListening(PROVIDER_NAME)
            && nodes.b().getControlChannel().isListening(SECOND_PROVIDER_NAME));
   }



   /*******************************************************************************
    ** Send a raw text message to the control topic (on the base provider).
    *******************************************************************************/
   private void sendControlText(String text) throws Exception
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();
      try(Session session = manager.openSession(PROVIDER_NAME, false))
      {
         session.createProducer(manager.resolve(session, EsbControlChannel.controlDestination(PROVIDER_NAME))).send(session.createTextMessage(text));
      }
   }



   /*******************************************************************************
    * The two runtimes of a test.
    *******************************************************************************/
   private record Nodes(QEsbRuntime a, QEsbRuntime b, QInstance instanceB)
   {
   }

}
