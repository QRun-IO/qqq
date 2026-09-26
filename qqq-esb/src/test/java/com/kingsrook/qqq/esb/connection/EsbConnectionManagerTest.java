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

package com.kingsrook.qqq.esb.connection;


import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.apache.activemq.artemis.jms.client.ActiveMQSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for EsbConnectionManager - against the embedded Artemis broker.
 *******************************************************************************/
class EsbConnectionManagerTest extends EsbTestBase
{
   private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(20);



   /*******************************************************************************
    ** Reset the manager, and make sure the broker is running for the next test
    ** (in case a test that stops it failed part-way).
    *******************************************************************************/
   @AfterEach
   void afterEach() throws Exception
   {
      EsbConnectionManager.getInstance().closeAll();
      startEmbeddedBroker();
   }



   /*******************************************************************************
    ** Sessions share one connection per provider; queue and topic destinations
    ** resolve to their broker-side names, and messages go through both.
    *******************************************************************************/
   @Test
   void testOpenSessionSendAndReceiveOnQueueAndTopic() throws Exception
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();

      QEsbDestinationMetaData queueDestination = new QEsbDestinationMetaData()
         .withName("orderQueue")
         .withType(EsbDestinationType.QUEUE)
         .withProviderName(PROVIDER_NAME)
         .withDestinationName("esb.test.orderQueue");

      QEsbDestinationMetaData topicDestination = new QEsbDestinationMetaData()
         .withName("orderEvents")
         .withType(EsbDestinationType.TOPIC)
         .withProviderName(PROVIDER_NAME);

      Session session = manager.openSession(PROVIDER_NAME, false);
      assertThat(manager.isConnected(PROVIDER_NAME)).isTrue();
      assertThat(session.getTransacted()).isFalse();

      Destination queue = manager.resolve(session, queueDestination);
      assertThat(queue).isInstanceOf(Queue.class);
      assertThat(((Queue) queue).getQueueName()).isEqualTo("esb.test.orderQueue");
      assertThat(sendAndReceive(session, queue, "to the queue")).isEqualTo("to the queue");

      Destination topic = manager.resolve(session, topicDestination);
      assertThat(topic).isInstanceOf(Topic.class);
      assertThat(((Topic) topic).getTopicName()).isEqualTo("orderEvents");
      assertThat(sendAndReceive(session, topic, "to the topic")).isEqualTo("to the topic");

      Queue brokerQueue = manager.resolveQueue(session, PROVIDER_NAME, "esb.test.brokerQueue");
      assertThat(brokerQueue.getQueueName()).isEqualTo("esb.test.brokerQueue");
      assertThat(sendAndReceive(session, brokerQueue, "by broker name")).isEqualTo("by broker name");

      /////////////////////////////////////////////////////////////
      // a second session is on the same (long-lived) connection //
      /////////////////////////////////////////////////////////////
      Session secondSession = manager.openSession(PROVIDER_NAME, false);
      assertThat(((ActiveMQSession) secondSession).getConnection()).isSameAs(((ActiveMQSession) session).getConnection());
   }



   /*******************************************************************************
    ** A transacted session sends only when it commits.
    *******************************************************************************/
   @Test
   void testTransactedSessionSendsOnCommit() throws Exception
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();

      Session transactedSession = manager.openSession(PROVIDER_NAME, true);
      assertThat(transactedSession.getTransacted()).isTrue();

      Session         receivingSession = manager.openSession(PROVIDER_NAME, false);
      MessageConsumer consumer         = receivingSession.createConsumer(manager.resolveQueue(receivingSession, PROVIDER_NAME, "esb.test.transacted"));

      Queue queue = manager.resolveQueue(transactedSession, PROVIDER_NAME, "esb.test.transacted");
      transactedSession.createProducer(queue).send(transactedSession.createTextMessage("committed"));
      assertThat(consumer.receive(500)).isNull();

      transactedSession.commit();
      TextMessage message = (TextMessage) consumer.receive(5000);
      assertThat(message).isNotNull();
      assertThat(message.getText()).isEqualTo("committed");
   }



   /*******************************************************************************
    ** When the broker restarts, the manager reconnects in the background, then
    ** fires that provider's listeners (a failing listener doesn't stop others).
    *******************************************************************************/
   @Test
   void testReconnectAfterBrokerRestartFiresListener() throws Exception
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();

      AtomicInteger reconnectCount     = new AtomicInteger(0);
      AtomicInteger otherProviderCount = new AtomicInteger(0);
      manager.addConnectionListener(PROVIDER_NAME, () ->
      {
         throw (new RuntimeException("a failing listener must not stop the others"));
      });
      manager.addConnectionListener(PROVIDER_NAME, reconnectCount::incrementAndGet);
      manager.addConnectionListener("someOtherProvider", otherProviderCount::incrementAndGet);

      manager.openSession(PROVIDER_NAME, false);
      assertThat(manager.isConnected(PROVIDER_NAME)).isTrue();

      stopEmbeddedBroker();
      waitFor("the connection loss to be noticed", () -> !manager.isConnected(PROVIDER_NAME));
      assertThat(reconnectCount.get()).isZero();

      startEmbeddedBroker();
      waitFor("the reconnect listener to fire", () -> reconnectCount.get() == 1);
      assertThat(manager.isConnected(PROVIDER_NAME)).isTrue();
      assertThat(otherProviderCount.get()).isZero();

      Session session = manager.openSession(PROVIDER_NAME, false);
      assertThat(sendAndReceive(session, manager.resolveQueue(session, PROVIDER_NAME, "esb.test.afterReconnect"), "reconnected")).isEqualTo("reconnected");
   }



   /*******************************************************************************
    ** With the broker down, the first open fails within the connect timeout;
    ** later opens fail at once while the manager retries in the background;
    ** when the broker comes up, the listeners fire and sessions open again.
    *******************************************************************************/
   @Test
   void testBrokerDownFailsFastThenConnectsInBackground() throws Exception
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();

      AtomicInteger reconnectCount = new AtomicInteger(0);
      manager.addConnectionListener(PROVIDER_NAME, reconnectCount::incrementAndGet);

      stopEmbeddedBroker();

      Instant firstAttemptStart = Instant.now();
      assertThatThrownBy(() -> manager.openSession(PROVIDER_NAME, false))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Could not connect to ESB provider " + PROVIDER_NAME);
      assertThat(Duration.between(firstAttemptStart, Instant.now())).isLessThan(Duration.ofMillis(EsbConnectionFactoryBuilder.CONNECT_TIMEOUT_MS));
      assertThat(manager.isConnected(PROVIDER_NAME)).isFalse();

      Instant secondAttemptStart = Instant.now();
      assertThatThrownBy(() -> manager.openSession(PROVIDER_NAME, true))
         .isInstanceOf(QException.class)
         .hasMessageContaining("reconnecting");
      assertThat(Duration.between(secondAttemptStart, Instant.now())).isLessThan(Duration.ofSeconds(1));

      startEmbeddedBroker();
      waitFor("the background connect listener to fire", () -> reconnectCount.get() == 1);
      assertThat(manager.isConnected(PROVIDER_NAME)).isTrue();

      Session session = manager.openSession(PROVIDER_NAME, false);
      assertThat(sendAndReceive(session, manager.resolveQueue(session, PROVIDER_NAME, "esb.test.afterBrokerDown"), "connected")).isEqualTo("connected");
   }



   /*******************************************************************************
    ** An unknown provider (or one with no type, or no ESB meta-data at all)
    ** throws QException; resolving for a provider with no connection throws
    ** JMSException.
    *******************************************************************************/
   @Test
   void testUnknownProviderThrowsQException() throws Exception
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();

      assertThatThrownBy(() -> manager.openSession("noSuchProvider", false))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Unknown ESB provider: noSuchProvider");
      assertThat(manager.isConnected("noSuchProvider")).isFalse();

      assertThatThrownBy(() -> manager.resolveQueue(null, "noSuchProvider", "someQueue"))
         .isInstanceOf(JMSException.class)
         .hasMessageContaining("noSuchProvider");
      assertThatThrownBy(() -> manager.resolve(null, new QEsbDestinationMetaData().withName("someQueue").withType(EsbDestinationType.QUEUE)))
         .isInstanceOf(JMSException.class);

      EsbInstanceMetaData.of(QContext.getQInstance()).withProvider(new QEsbProviderMetaData()
         .withName("untyped")
         .withUrl(getBrokerUrl()));
      assertThatThrownBy(() -> manager.openSession("untyped", false))
         .isInstanceOf(QException.class)
         .hasMessageContaining("has no type");

      QInstance instanceWithoutEsb = defineInstance();
      instanceWithoutEsb.getSupplementalMetaData().remove(EsbInstanceMetaData.NAME);
      QContext.init(instanceWithoutEsb, new QSession());
      assertThatThrownBy(() -> manager.openSession(PROVIDER_NAME, false))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Unknown ESB provider: " + PROVIDER_NAME);

      QContext.clear();
      assertThatThrownBy(() -> manager.openSession(PROVIDER_NAME, false))
         .isInstanceOf(QException.class);
   }



   /*******************************************************************************
    ** A missing broker client jar fails only for providers of that type, with a
    ** message naming the missing class.
    *******************************************************************************/
   @Test
   void testMissingClientClassGivesClearMessage() throws Exception
   {
      ClassLoader withoutRabbit = new ClassLoaderWithoutPackage(getClass().getClassLoader(), "com.rabbitmq.");

      assertThatThrownBy(() -> EsbConnectionFactoryBuilder.forProviderType(EsbProviderType.RABBITMQ, withoutRabbit))
         .isInstanceOf(QException.class)
         .hasMessage("Broker client for RABBITMQ not on classpath: com.rabbitmq.jms.admin.RMQConnectionFactory");

      assertThat(EsbConnectionFactoryBuilder.forProviderType(EsbProviderType.ACTIVEMQ_ARTEMIS, withoutRabbit)).isInstanceOf(ArtemisConnectionFactoryBuilder.class);
      assertThat(EsbConnectionFactoryBuilder.forProviderType(EsbProviderType.RABBITMQ, getClass().getClassLoader())).isInstanceOf(RabbitConnectionFactoryBuilder.class);
   }



   /*******************************************************************************
    ** A RabbitMQ provider whose broker isn't there fails with a QException (and
    ** doesn't affect the Artemis provider).
    *******************************************************************************/
   @Test
   void testUnreachableRabbitBrokerThrowsQException() throws Exception
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();

      EsbInstanceMetaData.of(QContext.getQInstance()).withProvider(new QEsbProviderMetaData()
         .withName("rabbit")
         .withType(EsbProviderType.RABBITMQ)
         .withUrl("amqp://localhost:" + findClosedPort() + "/%2F")
         .withUsername("guest")
         .withPassword("guest"));

      assertThatThrownBy(() -> manager.openSession("rabbit", false))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Could not connect to ESB provider rabbit");
      assertThat(manager.isConnected("rabbit")).isFalse();

      manager.openSession(PROVIDER_NAME, false);
      assertThat(manager.isConnected(PROVIDER_NAME)).isTrue();
   }



   /*******************************************************************************
    ** closeAll closes connections (and their sessions) and forgets listeners;
    ** the next open makes a new connection.
    *******************************************************************************/
   @Test
   void testCloseAll() throws Exception
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();

      AtomicInteger forgottenListenerCount = new AtomicInteger(0);
      manager.addConnectionListener(PROVIDER_NAME, forgottenListenerCount::incrementAndGet);

      Session session = manager.openSession(PROVIDER_NAME, false);
      manager.closeAll();
      assertThat(manager.isConnected(PROVIDER_NAME)).isFalse();
      assertThatThrownBy(() -> session.createTextMessage("closed")).isInstanceOf(JMSException.class);

      ////////////////////////////
      // closeAll is idempotent //
      ////////////////////////////
      manager.closeAll();

      Session newSession = manager.openSession(PROVIDER_NAME, false);
      assertThat(manager.isConnected(PROVIDER_NAME)).isTrue();
      assertThat(((ActiveMQSession) newSession).getConnection()).isNotSameAs(((ActiveMQSession) session).getConnection());

      /////////////////////////////////////////////////////////////////
      // after a reconnect, only listeners added after closeAll fire //
      /////////////////////////////////////////////////////////////////
      AtomicInteger newListenerCount = new AtomicInteger(0);
      manager.addConnectionListener(PROVIDER_NAME, newListenerCount::incrementAndGet);

      stopEmbeddedBroker();
      waitFor("the connection loss to be noticed", () -> !manager.isConnected(PROVIDER_NAME));
      startEmbeddedBroker();
      waitFor("the new listener to fire", () -> newListenerCount.get() == 1);
      assertThat(forgottenListenerCount.get()).isZero();
   }



   /*******************************************************************************
    ** Send a text message to a destination and receive it back (the consumer is
    ** created first, so this works for topics too).
    *******************************************************************************/
   private String sendAndReceive(Session session, Destination destination, String text) throws JMSException
   {
      MessageConsumer consumer = session.createConsumer(destination);
      try
      {
         session.createProducer(destination).send(session.createTextMessage(text));
         TextMessage message = (TextMessage) consumer.receive(5000);
         return (message == null ? null : message.getText());
      }
      finally
      {
         consumer.close();
      }
   }



   /*******************************************************************************
    ** Wait (polling) for a condition to become true, failing after WAIT_TIMEOUT.
    *******************************************************************************/
   private void waitFor(String description, BooleanSupplier condition)
   {
      Instant deadline = Instant.now().plus(WAIT_TIMEOUT);
      while(!condition.getAsBoolean())
      {
         if(Instant.now().isAfter(deadline))
         {
            fail("Timed out waiting for " + description);
         }
         SleepUtils.sleep(50, TimeUnit.MILLISECONDS);
      }
   }



   /*******************************************************************************
    ** A localhost port that nothing is listening on.
    *******************************************************************************/
   private static int findClosedPort() throws IOException
   {
      try(ServerSocket serverSocket = new ServerSocket(0))
      {
         return (serverSocket.getLocalPort());
      }
   }



   /*******************************************************************************
    ** A class loader that hides the classes in one package - to simulate a broker
    ** client jar that isn't on the classpath.
    *******************************************************************************/
   private static class ClassLoaderWithoutPackage extends ClassLoader
   {
      private final String hiddenPackagePrefix;



      /*******************************************************************************
       **
       *******************************************************************************/
      ClassLoaderWithoutPackage(ClassLoader parent, String hiddenPackagePrefix)
      {
         super(parent);
         this.hiddenPackagePrefix = hiddenPackagePrefix;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
      {
         if(name.startsWith(hiddenPackagePrefix))
         {
            throw (new ClassNotFoundException(name));
         }
         return (super.loadClass(name, resolve));
      }
   }

}
