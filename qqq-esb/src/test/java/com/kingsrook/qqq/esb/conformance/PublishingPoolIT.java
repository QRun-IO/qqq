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

package com.kingsrook.qqq.esb.conformance;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.connection.EsbConnectionFactoryBuilder;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import com.kingsrook.qqq.esb.publish.EsbPublisher;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Dedicated publishing-pool evidence on both supported native broker images.
 ** These are additional to, and do not modify, the shared conformance suite.
 *******************************************************************************/
class PublishingPoolIT
{
   /*******************************************************************************
    ** Run against the same Artemis image used by the existing conformance gate.
    *******************************************************************************/
   @Test
   void artemisPublishingPool() throws Exception
   {
      try(GenericContainer<?> broker = new GenericContainer<>("apache/artemis:2.57.0")
         .withEnv("ARTEMIS_USER", "pooltest").withEnv("ARTEMIS_PASSWORD", "pooltest")
         .withExposedPorts(61616)
         .waitingFor(Wait.forLogMessage(".*AMQ221007: Server is now active.*\\n", 1).withStartupTimeout(Duration.ofMinutes(2))))
      {
         broker.start();
         verifyBroker(new QEsbProviderMetaData().withType(EsbProviderType.ACTIVEMQ_ARTEMIS)
            .withUrl("tcp://" + broker.getHost() + ":" + broker.getMappedPort(61616)));
      }
   }



   /*******************************************************************************
    ** Native RabbitMQ transactions must not leak aborted batch contents.
    *******************************************************************************/
   @Test
   void rabbitPublishingPool() throws Exception
   {
      try(GenericContainer<?> broker = new GenericContainer<>("rabbitmq:4-management")
         .withEnv("RABBITMQ_DEFAULT_USER", "pooltest").withEnv("RABBITMQ_DEFAULT_PASS", "pooltest")
         .withExposedPorts(5672, 15672)
         .waitingFor(Wait.forHttp("/api/overview").forPort(15672).withBasicCredentials("pooltest", "pooltest")))
      {
         broker.start();
         verifyBroker(new QEsbProviderMetaData().withType(EsbProviderType.RABBITMQ)
            .withUrl("amqp://" + broker.getHost() + ":" + broker.getMappedPort(5672) + "/%2F"));
      }
   }



   /*******************************************************************************
    ** Public publication, rollback isolation, simultaneous native transactions,
    ** and independently received payloads.
    *******************************************************************************/
   private void verifyBroker(QEsbProviderMetaData provider) throws Exception
   {
      provider.withName("pool").withUsername("pooltest").withPassword("pooltest");
      ConnectionFactory readinessFactory = EsbConnectionFactoryBuilder.forProviderType(provider.getType(), getClass().getClassLoader()).buildConnectionFactory(provider);
      try
      {
         BrokerContainerPorts.awaitJmsReady(readinessFactory, provider.getType().name());
      }
      finally
      {
         if(readinessFactory instanceof AutoCloseable closeable)
         {
            closeable.close();
         }
      }
      QInstance instance = EsbTestBase.defineInstance();
      EsbInstanceMetaData.of(instance).getProviders().clear();
      String queueName = "pool-" + UUID.randomUUID();
      EsbInstanceMetaData.of(instance).withProvider(provider).withDestination(new QEsbDestinationMetaData()
         .withName(queueName).withProviderName("pool").withType(EsbDestinationType.QUEUE));
      EsbConnectionManager manager = EsbConnectionManager.getInstance();
      try
      {
         QContext.init(instance, new QSession());
         try(Session receiver = manager.openSession("pool", false);
            MessageConsumer consumer = receiver.createConsumer(manager.resolveQueue(receiver, "pool", queueName)))
         {
            Session pooled;
            try(var warm = manager.borrowPublishingSession("pool"))
            {
               pooled = warm.getSession();
               warm.commit();
            }
            for(int i = 0; i < 12; i++)
            {
               assertThat(EsbPublisher.getInstance().publish(queueName, List.of(event("public-" + i))).getSuccess()).isTrue();
               Message message = consumer.receive(5000);
               assertThat(message).isNotNull();
               assertThat(message.getJMSDeliveryMode()).isEqualTo(DeliveryMode.PERSISTENT);
               assertThat(message.getStringProperty("ce_id")).isEqualTo("public-" + i);
               try(var reused = manager.borrowPublishingSession("pool"))
               {
                  assertThat(reused.getSession()).isSameAs(pooled);
                  reused.commit();
               }
            }
            assertThat(EsbPublisher.getInstance().publish(queueName, List.of(event("must-rollback"), new EsbEvent())).getSuccess()).isFalse();
            try(var replacement = manager.borrowPublishingSession("pool"))
            {
               assertThat(replacement.getSession()).isNotSameAs(pooled);
               replacement.commit();
            }
            assertThat(consumer.receive(200)).isNull();
            concurrentTransactions(manager, queueName, consumer);
            List<String> received = new ArrayList<>();
            for(int i = 0; i < 8; i++)
            {
               Message message = consumer.receive(5000);
               assertThat(message).isInstanceOf(TextMessage.class);
               received.add(((TextMessage) message).getText());
            }
            assertThat(received).containsExactlyInAnyOrder("worker-0", "worker-1", "worker-2", "worker-3", "worker-4", "worker-5", "worker-6", "worker-7");
            assertThat(consumer.receive(200)).isNull();
         }
      }
      finally
      {
         manager.closeAll();
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Hold all eight sessions with pending sends before allowing any commit.
    ** Independent receipt detects transaction mixing or message loss.
    *******************************************************************************/
   private void concurrentTransactions(EsbConnectionManager manager, String queueName, MessageConsumer consumer) throws Exception
   {
      CountDownLatch sent = new CountDownLatch(8);
      CountDownLatch commit = new CountDownLatch(1);
      List<Future<?>> futures = new ArrayList<>();
      try(var executor = Executors.newFixedThreadPool(8))
      {
         try
         {
            for(int i = 0; i < 8; i++)
            {
               String payload = "worker-" + i;
               futures.add(executor.submit(() ->
               {
                  try(var lease = manager.borrowPublishingSession("pool"))
                  {
                     Session session = lease.getSession();
                     try(MessageProducer producer = session.createProducer(manager.resolveQueue(session, "pool", queueName)))
                     {
                        producer.send(session.createTextMessage(payload));
                     }
                     sent.countDown();
                     assertThat(commit.await(15, TimeUnit.SECONDS)).isTrue();
                     lease.commit();
                  }
                  return (null);
               }));
            }
            assertThat(sent.await(15, TimeUnit.SECONDS)).isTrue();
            assertThat(consumer.receive(200)).isNull();
         }
         finally
         {
            commit.countDown();
         }
         for(Future<?> future : futures)
         {
            future.get(15, TimeUnit.SECONDS);
         }
      }
   }



   /*******************************************************************************
    ** Individually identifiable public event, without external application data.
    *******************************************************************************/
   private EsbEvent event(String id)
   {
      return (new EsbEvent().withId(id).withType("pool.test").withSource("qqq://pool/test"));
   }
}
