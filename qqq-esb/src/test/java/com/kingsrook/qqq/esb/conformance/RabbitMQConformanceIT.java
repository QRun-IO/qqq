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


import java.time.Instant;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.management.EsbBrokerAdapter;
import com.kingsrook.qqq.esb.management.EsbBrokerAdapters;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.runtime.EsbTriggerState;
import com.kingsrook.qqq.esb.runtime.QEsbRuntime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * RabbitMQ 4 JMS and HTTP management conformance.
 ******************************************************************************/
class RabbitMQConformanceIT extends AbstractEsbConformanceTest
{
   private static final String USERNAME = "esbtest";
   private static final String PASSWORD = "esbtest";

   private static final int[] HOST_PORTS = BrokerContainerPorts.availablePair();
   private static final GenericContainer<?> BROKER = new FixedHostPortGenericContainer<>("rabbitmq:4-management")
      .withEnv("RABBITMQ_DEFAULT_USER", USERNAME)
      .withEnv("RABBITMQ_DEFAULT_PASS", PASSWORD)
      .withFixedExposedPort(HOST_PORTS[0], 5672)
      .withFixedExposedPort(HOST_PORTS[1], 15672)
      .waitingFor(Wait.forHttp("/api/overview").forPort(15672).withBasicCredentials(USERNAME, PASSWORD));



   /** Start the broker container once for the conformance class. */
   @BeforeAll
   static void startContainer()
   {
      BROKER.start();
      BrokerContainerPorts.assertReachable(BROKER, 5672, 15672);
   }



   /** Stop the broker container after all conformance cases. */
   @AfterAll
   static void stopContainer()
   {
      BROKER.stop();
   }



   /** The running Testcontainers broker. */
   @Override
   protected GenericContainer<?> broker()
   {
      return (BROKER);
   }



   /** The provider type for this broker. */
   @Override
   protected EsbProviderType providerType()
   {
      return (EsbProviderType.RABBITMQ);
   }



   /** The mapped JMS or AMQP connection URL. */
   @Override
   protected String brokerUrl()
   {
      return ("amqp://" + BROKER.getHost() + ":" + BROKER.getMappedPort(5672) + "/%2F");
   }



   /** The mapped broker management URL. */
   @Override
   protected String managementUrl()
   {
      return ("http://" + BROKER.getHost() + ":" + BROKER.getMappedPort(15672));
   }



   /** The broker test username. */
   @Override
   protected String brokerUsername()
   {
      return (USERNAME);
   }



   /** The broker test password. */
   @Override
   protected String brokerPassword()
   {
      return (PASSWORD);
   }



   /*******************************************************************************
    ** RabbitMQ JMS 3.9 synchronous receive polls with basicGet, rather than
    ** registering a basicConsume subscription.  Broker management therefore
    ** reports zero consumers even while a QQQ trigger is processing messages.
    ** Bound the observation so a delayed management sample cannot masquerade
    ** as this client limitation.
    *******************************************************************************/
   @Test
   void synchronousJmsPollingDoesNotIncreaseBrokerConsumerCount() throws Exception
   {
      QInstance instance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      QContext.init(instance, new QSession());
      QEsbRuntime runtime = startRuntime(instance);
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      sendEvent(QUEUE_NAME, Map.of("observed", true));
      waitFor("RabbitMQ JMS run", () -> RecordingStep.getCompletedRuns().size() == 1);

      EsbBrokerAdapter adapter = EsbBrokerAdapters.forProvider(PROVIDER_NAME).orElseThrow();
      Instant deadline = Instant.now().plusSeconds(10);
      int maximum = 0;
      do
      {
         maximum = Math.max(maximum, adapter.getQueueInfo(getBrokerQueueName()).orElseThrow().consumerCount());
         Thread.sleep(250);
      }
      while(Instant.now().isBefore(deadline));
      assertThat(maximum).isZero();
      assertThat(runtime.getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.RUNNING);
   }

}
