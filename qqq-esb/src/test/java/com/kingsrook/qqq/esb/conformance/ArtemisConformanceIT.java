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
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.management.EsbBrokerAdapter;
import com.kingsrook.qqq.esb.management.EsbBrokerAdapters;
import com.kingsrook.qqq.esb.management.EsbMessageBrowser;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Artemis JMS and Jolokia conformance against the official broker image.
 ******************************************************************************/
class ArtemisConformanceIT extends AbstractEsbConformanceTest
{
   private static final String USERNAME = "esbtest";
   private static final String PASSWORD = "esbtest";

   private static final GenericContainer<?> BROKER = new GenericContainer<>(DockerImageName.parse("apache/artemis:2.57.0"))
      .withEnv("ARTEMIS_USER", USERNAME)
      .withEnv("ARTEMIS_PASSWORD", PASSWORD)
      .withExposedPorts(61616, 8161)
      .waitingFor(Wait.forListeningPort());



   /** Start the broker container once for the conformance class. */
   @BeforeAll
   static void startContainer()
   {
      BROKER.start();
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
      return (EsbProviderType.ACTIVEMQ_ARTEMIS);
   }



   /** The mapped JMS or AMQP connection URL. */
   @Override
   protected String brokerUrl()
   {
      return ("tcp://" + BROKER.getHost() + ":" + BROKER.getMappedPort(61616));
   }



   /** The mapped broker management URL. */
   @Override
   protected String managementUrl()
   {
      return ("http://" + BROKER.getHost() + ":" + BROKER.getMappedPort(8161));
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



   /** All Artemis-only management actions change real queue state. */
   @Test
   void pauseResumeSelectedDeleteOldDeleteAndMoveWorkThroughJolokia() throws Exception
   {
      QContext.init(defineInstanceWithDestinations(PROVIDER_NAME), new QSession());
      EsbBrokerAdapter adapter = EsbBrokerAdapters.forProvider(PROVIDER_NAME).orElseThrow();
      String source = getBrokerQueueName();
      String target = source + ".moved";
      sendEvent(QUEUE_NAME, Map.of("n", 1));
      sendEvent(QUEUE_NAME, Map.of("n", 2));
      List<String> ids = EsbMessageBrowser.browse(PROVIDER_NAME, source, 0, 10).stream()
         .map(message -> message.getMessageId()).toList();
      assertThat(ids).hasSize(2);

      adapter.pauseQueue(source);
      assertThat(adapter.getQueueInfo(source).orElseThrow().paused()).isTrue();
      adapter.resumeQueue(source);
      assertThat(adapter.getQueueInfo(source).orElseThrow().paused()).isFalse();

      assertThat(adapter.deleteMessages(source, List.of(ids.get(0)))).isEqualTo(1);
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, source, 0, 10)).hasSize(1);
      assertThat(adapter.deleteMessagesOlderThan(source, Instant.now().plusSeconds(60))).isEqualTo(1);
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, source, 0, 10)).isEmpty();

      sendEvent(QUEUE_NAME, Map.of("n", 3));
      String moveId = EsbMessageBrowser.browse(PROVIDER_NAME, source, 0, 1).get(0).getMessageId();
      try(var session = com.kingsrook.qqq.esb.connection.EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
      {
         session.createProducer(com.kingsrook.qqq.esb.connection.EsbConnectionManager.getInstance()
            .resolveQueue(session, PROVIDER_NAME, target)).send(session.createTextMessage("target exists"));
      }
      assertThat(adapter.moveMessages(source, List.of(moveId), target)).isEqualTo(1);
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, source, 0, 10)).isEmpty();
      assertThat(EsbMessageBrowser.browse(PROVIDER_NAME, target, 0, 10)).hasSize(2);
   }

}
