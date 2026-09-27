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

package com.kingsrook.qqq.esb.runtime;


import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.instances.QRuntimeServiceInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.publish.EsbPublisher;
import com.kingsrook.qqq.middleware.javalin.QApplicationLauncher;
import com.kingsrook.qqq.middleware.javalin.QApplicationLauncherConfig;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Real launcher ownership of the provider resources retained by trigger stop.
 ** Assertions run before inherited teardown can reset the connection manager.
 ******************************************************************************/
@Timeout(40)
class EsbLauncherLifecycleTest extends EsbRuntimeTestBase
{
   private static final String PUBLISH_DESTINATION = "ownedPublisherQueue";
   private static Session partiallyStartedSession;
   private QApplicationLauncher launcher;



   /*******************************************************************************
    ** Cleanup still runs after a failed assertion, without supplying its evidence.
    ******************************************************************************/
   @AfterEach
   void stopOwnedLauncher()
   {
      if(launcher != null)
      {
         launcher.stop();
      }
      QEsbRuntime.getInstance().stop();
      if(partiallyStartedSession != null)
      {
         try
         {
            partiallyStartedSession.close();
         }
         catch(JMSException ignored)
         {
            // The application owner already closed this session in passing cases.
         }
         partiallyStartedSession = null;
      }
   }



   /*******************************************************************************
    ** Launcher stop must close actual publisher sessions and broker connections.
    ******************************************************************************/
   @Test
   void launcherStopClosesProviderConnectionAndPublisherSession() throws Exception
   {
      QInstance instance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      launcher = launch(instance);
      awaitRuntime();
      EsbConnectionManager manager = EsbConnectionManager.getInstance();
      try(Session retained = manager.openSession(PROVIDER_NAME, false))
      {
         assertTrue(manager.isConnected(PROVIDER_NAME));
         assertTrue(getEmbeddedBrokerServer().getConnectionCount() > 0);
         launcher.stop();
         assertFalse(manager.isConnected(PROVIDER_NAME), "Launcher retained the ESB provider connection");
         assertThrows(JMSException.class, retained::createMessage);
         waitFor("broker connections to close", () -> getEmbeddedBrokerServer().getConnectionCount() == 0);
         assertFalse(QEsbRuntime.getInstance().isRunning());
      }
   }



   /*******************************************************************************
    ** The same provider name must use a different broker URL after app restart.
    ** Broker-side connection counts and an independent JMS receiver are oracles.
    ******************************************************************************/
   @Test
   void restartUsesFreshProviderConfiguration() throws Exception
   {
      QInstance first = applicationInstance();
      launcher = launch(first);
      awaitRuntime();
      assertPublishRoundTrip(first, getBrokerUrl());
      launcher.stop();

      String nextUrl = "vm://" + (UUID.randomUUID().hashCode() & Integer.MAX_VALUE);
      EmbeddedActiveMQ nextBroker = new EmbeddedActiveMQ().setConfiguration(new ConfigurationImpl()
         .setPersistenceEnabled(false).setSecurityEnabled(false).setJMXManagementEnabled(false)
         .addAcceptorConfiguration("owned-next", nextUrl));
      nextBroker.start();
      try
      {
         QInstance next = applicationInstance();
         EsbInstanceMetaData.of(next).getProvider(PROVIDER_NAME).setUrl(nextUrl);
         launcher = launch(next);
         awaitRuntime();
         assertPublishRoundTrip(next, nextUrl);
         assertEquals(0, getEmbeddedBrokerServer().getConnectionCount(), "Previous provider configuration was reused");
         assertTrue(nextBroker.getActiveMQServer().getConnectionCount() > 0);
         launcher.stop();
         waitFor("replacement broker connection cleanup", () -> nextBroker.getActiveMQServer().getConnectionCount() == 0);
      }
      finally
      {
         launcher.stop();
         nextBroker.stop();
      }
   }



   /*******************************************************************************
    ** A stopped launcher owns no reconnect worker or listener in the next start.
    ** Force a second native reconnect so removal of the old listener is observed.
    ******************************************************************************/
   @Test
   void shutdownEndsReconnectWorkAndRemovesOldListeners() throws Exception
   {
      launcher = launch(applicationInstance());
      awaitRuntime();
      EsbConnectionManager manager = EsbConnectionManager.getInstance();
      AtomicInteger oldNotifications = new AtomicInteger();
      manager.addConnectionListener(PROVIDER_NAME, oldNotifications::incrementAndGet);
      stopEmbeddedBroker();
      waitFor("old provider reconnect worker", () -> reconnectWorker() != null);
      Thread oldWorker = reconnectWorker();
      assertNotNull(oldWorker);
      launcher.stop();
      oldWorker.join(TimeUnit.SECONDS.toMillis(8));
      assertFalse(oldWorker.isAlive(), "Stopped launcher retained reconnect work");
      assertFalse(manager.isConnected(PROVIDER_NAME));

      startEmbeddedBroker();
      launcher = launch(applicationInstance());
      awaitRuntime();
      AtomicInteger newNotifications = new AtomicInteger();
      manager.addConnectionListener(PROVIDER_NAME, newNotifications::incrementAndGet);
      stopEmbeddedBroker();
      waitFor("new provider reconnect worker", () -> reconnectWorker() != null);
      startEmbeddedBroker();
      waitFor("new provider reconnect notification", () -> newNotifications.get() > 0);
      awaitRuntime();
      assertEquals(0, oldNotifications.get(), "Prior application's listener survived closeAll");
      assertEquals(1, newNotifications.get());
   }



   /*******************************************************************************
    ** Failure of a later application service must release already-started ESB.
    ******************************************************************************/
   @Test
   void partialApplicationStartupClosesProviderResources() throws Exception
   {
      QInstance instance = applicationInstance();
      instance.withRuntimeService(new QCodeReference(FailingStartService.class));
      QException failure = assertThrows(QException.class, () -> launch(instance));
      assertTrue(failure.getMessage().contains("owned later startup failure"));
      assertNotNull(partiallyStartedSession);
      assertThrows(JMSException.class, partiallyStartedSession::createMessage);
      assertFalse(EsbConnectionManager.getInstance().isConnected(PROVIDER_NAME));
      assertFalse(QEsbRuntime.getInstance().isRunning());
      waitFor("failed startup broker cleanup", () -> getEmbeddedBrokerServer().getConnectionCount() == 0);
   }



   /*******************************************************************************
    ** Inject only a stop failure, after the actual native control channel closes.
    ** The service finally must still close provider sessions and connections.
    ******************************************************************************/
   @Test
   void runtimeStopFailureStillClosesProviderResources() throws Exception
   {
      QInstance instance = applicationInstance();
      launcher = launch(instance);
      awaitRuntime();
      QEsbRuntime runtime = QEsbRuntime.getInstance();
      Field field = QEsbRuntime.class.getDeclaredField("controlChannel");
      field.setAccessible(true);
      AtomicInteger closeCalls = new AtomicInteger();
      synchronized(runtime)
      {
         EsbControlChannel original = (EsbControlChannel) field.get(runtime);
         field.set(runtime, new EsbControlChannel(runtime, instance)
         {
            /*******************************************************************************
             ** Delegate native cleanup, then reproduce a failing runtime stopper.
             ******************************************************************************/
            @Override
            public void close()
            {
               original.close();
               closeCalls.incrementAndGet();
               throw new IllegalStateException("owned runtime shutdown failure");
            }
         });
      }
      try(Session retained = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
      {
         launcher.stop();
         assertEquals(1, closeCalls.get());
         assertThrows(JMSException.class, retained::createMessage);
         assertFalse(EsbConnectionManager.getInstance().isConnected(PROVIDER_NAME));
         assertTrue(launcher.getStartedServiceNames().isEmpty());
         waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.STOPPED);
         waitFor("failed stop broker cleanup", () -> getEmbeddedBrokerServer().getConnectionCount() == 0);
      }
   }



   /*******************************************************************************
    ** Independently paused/stopped triggers do not own the publisher connection.
    ******************************************************************************/
   @Test
   void ordinaryRuntimePauseAndStopLeavePublisherUsable() throws Exception
   {
      QInstance instance = applicationInstance();
      QEsbRuntime runtime = startRuntime(instance);
      waitFor("independent runtime", runtime::isRunning);
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      runtime.getRunner(QUEUE_TRIGGER_NAME).pauseLocal();
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.PAUSED);
      assertPublishRoundTrip(instance, getBrokerUrl());
      runtime.stop();
      assertTrue(EsbConnectionManager.getInstance().isConnected(PROVIDER_NAME));
      assertPublishRoundTrip(instance, getBrokerUrl());
   }



   /*******************************************************************************
    ** Keep publication independent of trigger consumers and identify every event.
    ******************************************************************************/
   private QInstance applicationInstance()
   {
      QInstance instance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      EsbInstanceMetaData.of(instance).withDestination(new QEsbDestinationMetaData()
         .withName(PUBLISH_DESTINATION).withProviderName(PROVIDER_NAME).withType(EsbDestinationType.QUEUE)
         .withDestinationName("launcher.publish." + UUID.randomUUID()));
      return instance;
   }



   /*******************************************************************************
    ** The receiver uses an independent native client, never the connection manager.
    ******************************************************************************/
   private void assertPublishRoundTrip(QInstance instance, String url) throws Exception
   {
      QContext.init(instance, new QSession());
      EsbEvent event = EsbEventFactory.custom("launcher", "test/lifecycle", "qqq.test.lifecycle", Map.of("marker", UUID.randomUUID().toString()));
      try(ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);
          Connection connection = factory.createConnection();
          Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE))
      {
         connection.start();
         String queue = EsbInstanceMetaData.of(instance).getDestination(PUBLISH_DESTINATION).getDestinationName();
         try(MessageConsumer consumer = session.createConsumer(session.createQueue(queue)))
         {
            assertTrue(EsbPublisher.getInstance().publish(PUBLISH_DESTINATION, List.of(event)).getSuccess());
            var message = consumer.receive(5000);
            assertNotNull(message);
            assertEquals(event.getId(), EsbEventCodec.fromMessage(message).getId());
         }
      }
   }



   /*******************************************************************************
    ** Observe the existing named platform worker without private manager access.
    ******************************************************************************/
   private Thread reconnectWorker()
   {
      return Thread.getAllStackTraces().keySet().stream()
         .filter(thread -> thread.isAlive() && thread.getName().equals("qqq-esb-reconnect-" + PROVIDER_NAME))
         .findFirst().orElse(null);
   }



   /*******************************************************************************
    ** A real registered service that fails after acquiring an owned ESB session.
    ******************************************************************************/
   public static class FailingStartService implements QRuntimeServiceInterface
   {
      /*******************************************************************************
       ** Identify the failing service in the launcher's propagated error.
       ******************************************************************************/
      @Override
      public String getName()
      {
         return "ownedFailingStart";
      }



      /*******************************************************************************
       ** ESB starts first; prove native startup before raising the controlled fault.
       ******************************************************************************/
      @Override
      public void start(QInstance instance) throws QException
      {
         waitFor("ESB before later startup failure", QEsbRuntime.getInstance()::isRunning);
         partiallyStartedSession = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false);
         throw new QException("owned later startup failure");
      }



      /*******************************************************************************
       ** The launcher does not register a service whose start threw.
       ******************************************************************************/
      @Override
      public void stop()
      {
         throw new AssertionError("Failing start service should not be registered as started");
      }
   }



   /*******************************************************************************
    ** Bind the real HTTP server only to an ephemeral loopback port, with no UI.
    ******************************************************************************/
   private QApplicationLauncher launch(QInstance instance) throws Exception
   {
      return QApplicationLauncher.run(new OwnedApplication(instance), new QApplicationLauncherConfig()
         .withRegisterShutdownHook(false)
         .withServerCustomizer(server -> server.withPort(0)
            .withServeFrontendNext(false).withServeFrontendMaterialDashboard(false)
            .withJavalinConfigCustomizer(config -> config.jetty.host = "127.0.0.1")));
   }



   /*******************************************************************************
    ** Await actual trigger and control subscriptions, not just a startup flag.
    ******************************************************************************/
   private void awaitRuntime()
   {
      waitFor("launcher control listener", QEsbRuntime.getInstance()::isRunning);
      waitForState(QEsbRuntime.getInstance(), QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
   }



   /*******************************************************************************
    ** Supply the first-party broker fixture's instance to the actual launcher.
    ******************************************************************************/
   private static class OwnedApplication extends AbstractQQQApplication
   {
      private final QInstance instance;



      /*******************************************************************************
       ** Keep metadata ownership explicit for restarts and failure injection.
       ******************************************************************************/
      OwnedApplication(QInstance instance)
      {
         this.instance = instance;
      }



      /*******************************************************************************
       ** Use ordinary server validation/enrichment without substituting services.
       ******************************************************************************/
      @Override
      public QInstance defineQInstance()
      {
         return instance;
      }
   }
}
