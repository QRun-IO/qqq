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


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.simple.SimpleSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessEvent;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessPublication;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.publish.EsbPublishOutput;
import com.kingsrook.qqq.esb.publish.EsbPublisher;
import com.kingsrook.qqq.middleware.javalin.QApplicationLauncher;
import com.kingsrook.qqq.middleware.javalin.QApplicationLauncherConfig;
import io.javalin.Javalin;
import jakarta.jms.Connection;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import org.apache.activemq.artemis.core.postoffice.RoutingStatus;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerMessagePlugin;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** App-owned scheduled work may still publish while its launcher drains services.
 ** No production behavior is replaced: latches control only the owned job.
 ******************************************************************************/
@Timeout(45)
class EsbApplicationProducerDrainTest extends EsbRuntimeTestBase
{
   private static final String JOB = "ownedDrainJob";
   private static CountDownLatch jobEntered;
   private static CountDownLatch jobRelease;
   private static volatile String processUUID;
   private static CountDownLatch guardEntered;
   private static CountDownLatch guardRelease;
   private static CountDownLatch activePublishEntered;
   private static CountDownLatch activePublishRelease;
   private static CompletableFuture<EsbPublishOutput> activePublishResult;
   private static String firstActiveEventId;
   private static String secondActiveEventId;



   /*******************************************************************************
    ** Complete a real scheduled process while the launcher waits in scheduler stop.
    ** First prove exact native completion delivery, then require no leaked provider.
    ******************************************************************************/
   @Test
   void launcherDrainsScheduledPublicationBeforeFinalProviderCleanup() throws Exception
   {
      assertProducerDrain(false);
   }



   /*******************************************************************************
    ** A native HTTP request remains active across ESB stop and publishes normally.
    ** An owned scheduled guard holds shutdown without changing HTTP drain policy.
    ******************************************************************************/
   @Test
   void launcherDrainsInFlightHttpPublicationBeforeFinalProviderCleanup() throws Exception
   {
      assertProducerDrain(true);
   }



   /*******************************************************************************
    ** A real publisher holds its lease after its first native send, before commit.
    ** Shutdown must preserve that transaction while its scheduled owner drains.
    ******************************************************************************/
   @Test
   void launcherDrainsAlreadyBorrowedPublishingSession() throws Exception
   {
      activePublishEntered = new CountDownLatch(1);
      activePublishRelease = new CountDownLatch(1);
      activePublishResult = new CompletableFuture<>();
      firstActiveEventId = UUID.randomUUID().toString();
      secondActiveEventId = UUID.randomUUID().toString();
      CountDownLatch brokerReceivedFirst = new CountDownLatch(1);
      String queue = "owned.active." + UUID.randomUUID();
      QInstance instance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      EsbInstanceMetaData.of(instance).withDestination(new QEsbDestinationMetaData()
         .withName("activePublication").withProviderName(PROVIDER_NAME).withType(EsbDestinationType.QUEUE).withDestinationName(queue));
      QScheduleManager.defineDefaultSchedulableTypesInInstance(instance);
      instance.addScheduler(new SimpleSchedulerMetaData().withName("ownedSimple"));
      instance.addProcess(new QProcessMetaData().withName("ownedActivePublisher")
         .withSchedule(new QScheduleMetaData().withSchedulerName("ownedSimple").withRepeatSeconds(3600).withInitialDelayMillis(0))
         .withStep(new QBackendStepMetaData().withName("publish").withCode(new QCodeReference(ActivePublishingJob.class))));
      ActiveMQServerMessagePlugin observer = new ActiveMQServerMessagePlugin()
      {
         /*******************************************************************************
          ** Observe a real transacted send without replacing any broker behavior.
          ******************************************************************************/
         @Override
         public void afterSend(ServerSession session, Transaction transaction, org.apache.activemq.artemis.api.core.Message message, boolean direct, boolean noAutoCreateQueue, RoutingStatus result)
         {
            if(queue.equals(message.getAddress()) && firstActiveEventId.equals(message.getStringProperty("ce_id")))
            {
               brokerReceivedFirst.countDown();
            }
         }
      };
      getEmbeddedBrokerServer().registerBrokerPlugin(observer);
      QApplicationLauncher launcher = null;
      Thread stopper = null;
      CompletableFuture<Void> stopped = new CompletableFuture<>();
      try(ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(getBrokerUrl());
          Connection connection = factory.createConnection();
          Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageConsumer consumer = session.createConsumer(session.createQueue(queue)))
      {
         connection.start();
         launcher = QApplicationLauncher.run(new AbstractQQQApplication()
         {
            /*******************************************************************************
             **
             ******************************************************************************/
            @Override
            public QInstance defineQInstance()
            {
               return instance;
            }
         }, new QApplicationLauncherConfig().withRegisterShutdownHook(false)
            .withServerCustomizer(server -> server.withPort(0).withServeFrontendNext(false).withServeFrontendMaterialDashboard(false)
               .withJavalinConfigCustomizer(config -> config.jetty.host = "127.0.0.1")));
         assertTrue(activePublishEntered.await(5, TimeUnit.SECONDS));
         assertTrue(brokerReceivedFirst.await(5, TimeUnit.SECONDS));
         assertNull(consumer.receiveNoWait(), "The native first send must still be uncommitted");
         waitFor("native runtime startup", QEsbRuntime.getInstance()::isRunning);
         QApplicationLauncher ownedLauncher = launcher;
         stopper = Thread.ofPlatform().name("owned-active-publisher-stop").start(() ->
         {
            try
            {
               ownedLauncher.stop();
               stopped.complete(null);
            }
            catch(Throwable failure)
            {
               stopped.completeExceptionally(failure);
            }
         });
         Thread shutdownThread = stopper;
         waitFor("scheduler draining an already borrowed publisher", () ->
         {
            StackTraceElement[] frames = shutdownThread.getStackTrace();
            return (Arrays.stream(frames).anyMatch(frame -> frame.getClassName().endsWith("StandardScheduledExecutor") && frame.getMethodName().equals("stop"))
               && Arrays.stream(frames).anyMatch(frame -> frame.getMethodName().equals("awaitTermination")));
         });
         assertFalse(QEsbRuntime.getInstance().isRunning());
         assertFalse(stopped.isDone());
         System.out.println("OWNED_ACTIVE firstNativeSend=true uncommitted=true schedulerDraining=true managerConnected=" + EsbConnectionManager.getInstance().isConnected(PROVIDER_NAME));
         activePublishRelease.countDown();
         EsbPublishOutput output = activePublishResult.get(5, TimeUnit.SECONDS);
         var first = consumer.receive(1000);
         var second = first == null ? null : consumer.receive(1000);
         stopped.get(10, TimeUnit.SECONDS);
         stopper.join(5000);
         assertFalse(stopper.isAlive());
         System.out.println("OWNED_ACTIVE publishSuccess=" + output.getSuccess() + " sent=" + output.getSent() + " firstReceived=" + (first != null) + " secondReceived=" + (second != null));
         assertAll("Application shutdown must preserve an already borrowed publication",
            () -> assertTrue(output.getSuccess(), "An active publisher failed during shutdown"),
            () -> assertEquals(2, output.getSent()),
            () -> assertNotNull(first, "The first native transacted send was lost"),
            () -> assertNotNull(second, "The rest of the active batch was not delivered"));
         assertEquals(firstActiveEventId, EsbEventCodec.fromMessage(first).getId());
         assertEquals(secondActiveEventId, EsbEventCodec.fromMessage(second).getId());
         assertEquals(1, first.getIntProperty("JMSXDeliveryCount"));
         assertEquals(1, second.getIntProperty("JMSXDeliveryCount"));
         assertNull(consumer.receive(200));
         assertFalse(EsbConnectionManager.getInstance().isConnected(PROVIDER_NAME));
         waitFor("only independent receiver remains", () -> getEmbeddedBrokerServer().getConnectionCount() == 1);
      }
      finally
      {
         activePublishRelease.countDown();
         if(stopper != null)
         {
            stopper.join(10000);
         }
         if(launcher != null)
         {
            launcher.stop();
         }
         getEmbeddedBrokerServer().unRegisterBrokerPlugin(observer);
         EsbConnectionManager.getInstance().closeAll();
         waitFor("active fixture cleanup", () -> getEmbeddedBrokerServer().getConnectionCount() == 0);
         System.out.println("OWNED_ACTIVE fixtureCleanupNativeConnections=0");
      }
   }



   /*******************************************************************************
    ** Both producers complete after ESB stop and before the final application sweep.
    ******************************************************************************/
   private void assertProducerDrain(boolean throughHttp) throws Exception
   {
      System.out.println("OWNED_DRAIN throughHttp=" + throughHttp);
      guardEntered = new CountDownLatch(1);
      guardRelease = new CountDownLatch(1);
      jobEntered = new CountDownLatch(1);
      jobRelease = new CountDownLatch(1);
      processUUID = null;
      QInstance instance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      String completionQueue = "owned.completion." + UUID.randomUUID();
      EsbInstanceMetaData.of(instance).withDestination(new QEsbDestinationMetaData()
         .withName("completion").withProviderName(PROVIDER_NAME).withType(EsbDestinationType.QUEUE)
         .withDestinationName(completionQueue));
      QScheduleManager.defineDefaultSchedulableTypesInInstance(instance);
      instance.addScheduler(new SimpleSchedulerMetaData().withName("ownedSimple"));
      QScheduleMetaData schedule = new QScheduleMetaData().withSchedulerName("ownedSimple").withRepeatSeconds(3600).withInitialDelayMillis(0);
      QProcessMetaData process = new QProcessMetaData().withName(JOB)
         .withStep(new QBackendStepMetaData().withName("retained").withCode(new QCodeReference(RetainedJob.class)))
         .withSupplementalMetaData(new EsbProcessMetaData().withPublication(new EsbProcessPublication()
            .withDestinationName("completion").withEvents(List.of(EsbProcessEvent.COMPLETED))));
      instance.addProcess(process);
      if(throughHttp)
      {
         instance.addProcess(new QProcessMetaData().withName("ownedShutdownGuard").withSchedule(schedule)
            .withStep(new QBackendStepMetaData().withName("guard").withCode(new QCodeReference(SchedulerGuard.class))));
      }
      else
      {
         process.withSchedule(schedule);
      }
      AtomicReference<Javalin> httpService = new AtomicReference<>();
      HttpClient httpClient = null;
      CompletableFuture<HttpResponse<String>> httpResponse = null;
      QApplicationLauncher launcher = null;
      CompletableFuture<Void> stopped = new CompletableFuture<>();
      Thread stopper = null;
      try
      {
         launcher = QApplicationLauncher.run(new AbstractQQQApplication()
         {
            /*******************************************************************************
             ** Use owned broker metadata and the actual scheduled application process.
             ******************************************************************************/
            @Override
            public QInstance defineQInstance()
            {
               return instance;
            }
         }, new QApplicationLauncherConfig().withRegisterShutdownHook(false)
            .withServerCustomizer(server -> server.withPort(0)
               .withServeFrontendNext(false).withServeFrontendMaterialDashboard(false)
               .withJavalinConfigurationCustomizer(httpService::set)
               .withJavalinConfigCustomizer(config ->
               {
                  config.jetty.host = "127.0.0.1";
                  if(throughHttp)
                  {
                     config.routes.get("/owned-drain", context ->
                     {
                        QContext.init(instance, new QSession());
                        try
                        {
                           processUUID = UUID.randomUUID().toString();
                           new RunProcessAction().execute(new RunProcessInput().withProcessName(JOB).withProcessUUID(processUUID));
                           context.result("completed");
                        }
                        finally
                        {
                           QContext.clear();
                        }
                     });
                  }
               })));
         if(throughHttp)
         {
            assertTrue(guardEntered.await(5, TimeUnit.SECONDS));
            httpClient = HttpClient.newHttpClient();
            httpResponse = httpClient.sendAsync(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + httpService.get().port() + "/owned-drain")).GET().build(), HttpResponse.BodyHandlers.ofString());
         }
         assertEquals(List.of("javalinServer", "scheduleManager", "esbRuntime"), launcher.getStartedServiceNames());
         assertTrue(jobEntered.await(5, TimeUnit.SECONDS));
         assertNotNull(processUUID);
         waitFor("native ESB startup", QEsbRuntime.getInstance()::isRunning);
         QApplicationLauncher startedLauncher = launcher;
         stopper = Thread.ofPlatform().name("owned-producer-drain-stop").start(() ->
         {
            try
            {
               startedLauncher.stop();
               stopped.complete(null);
            }
            catch(Throwable failure)
            {
               stopped.completeExceptionally(failure);
            }
         });
         Thread shutdownThread = stopper;
         waitFor("launcher waiting for the retained scheduled job", () ->
         {
            StackTraceElement[] frames = shutdownThread.getStackTrace();
            return (Arrays.stream(frames).anyMatch(frame ->
               frame.getClassName().equals("com.kingsrook.qqq.backend.core.scheduler.simple.StandardScheduledExecutor")
                  && frame.getMethodName().equals("stop"))
               && Arrays.stream(frames).anyMatch(frame -> frame.getMethodName().equals("awaitTermination")));
         });
         System.out.println("OWNED_DRAIN schedulerStopEntered=true jobStillHeld=" + (jobRelease.getCount() == 1));
         assertFalse(QEsbRuntime.getInstance().isRunning());
         assertTrue(EsbConnectionManager.getInstance().isConnected(PROVIDER_NAME));
         waitFor("only the publishing provider remains", () -> getEmbeddedBrokerServer().getConnectionCount() == 1);
         assertFalse(stopped.isDone());
         System.out.println("OWNED_DRAIN afterEsbStop runtimeRunning=false managerConnected=true nativeConnections=1");

         // Independent native receiver: it never creates a QQQ manager connection.
         try(ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(getBrokerUrl());
             Connection connection = factory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             MessageConsumer consumer = session.createConsumer(session.createQueue(completionQueue)))
         {
            connection.start();
            jobRelease.countDown();
            var message = consumer.receive(5000);
            assertNotNull(message, "In-flight application completion must still be published");
            var event = EsbEventCodec.fromMessage(message);
            assertEquals(JOB, event.getData().get("processName"));
            assertEquals(processUUID, event.getData().get("processUUID"));
            assertEquals("qqq.process." + JOB + ".completed", event.getType());
            assertEquals(1, message.getIntProperty("JMSXDeliveryCount"));
            assertNull(consumer.receive(200));
            System.out.println("OWNED_DRAIN completionReceived=true processNameMatched=true processUUIDMatched=true deliveryCount=1 duplicate=false");
         }
         if(throughHttp)
         {
            HttpResponse<String> response = httpResponse.get(5, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode());
            assertEquals("completed", response.body());
            assertFalse(stopped.isDone());
            System.out.println("OWNED_DRAIN httpStatus=200 httpCompletedDuringShutdown=true");
            guardRelease.countDown();
         }
         waitFor("independent receiver closed", () -> getEmbeddedBrokerServer().getConnectionCount() <= 1);
         stopped.get(10, TimeUnit.SECONDS);
         stopper.join(5000);
         assertFalse(stopper.isAlive());
         assertTrue(launcher.getStartedServiceNames().isEmpty());
         System.out.println("OWNED_DRAIN immediatelyAfterReturnNativeConnections=" + getEmbeddedBrokerServer().getConnectionCount());
         waitFor("broker observes final provider close before fixture cleanup", () -> getEmbeddedBrokerServer().getConnectionCount() == 0);
         int nativeConnections = getEmbeddedBrokerServer().getConnectionCount();
         boolean managerConnected = EsbConnectionManager.getInstance().isConnected(PROVIDER_NAME);
         System.out.println("OWNED_DRAIN launcherReturned=true servicesEmpty=true nativeConnections=" + nativeConnections + " managerConnected=" + managerConnected);
         assertAll("Launcher must release resources after scheduled producers finish",
            () -> assertEquals(0, nativeConnections, "Native broker connection remains after launcher return"),
            () -> assertFalse(managerConnected, "Scheduled completion recreated a provider after ESB cleanup"));
      }
      finally
      {
         jobRelease.countDown();
         guardRelease.countDown();
         if(stopper != null)
         {
            stopper.join(10000);
         }
         if(launcher != null)
         {
            launcher.stop();
         }
         if(httpClient != null)
         {
            httpClient.close();
         }
         EsbConnectionManager.getInstance().closeAll();
         waitFor("fixture cleanup closes native resources", () -> getEmbeddedBrokerServer().getConnectionCount() == 0);
         System.out.println("OWNED_DRAIN fixtureCleanupNativeConnections=0");
      }
   }



   /*******************************************************************************
    ** Use the actual publisher, pooled lease, JMS producer, broker and commit path.
    ******************************************************************************/
   public static class ActivePublishingJob implements BackendStep
   {
      /*******************************************************************************
       **
       ******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output)
      {
         EsbEvent first = EsbEventFactory.custom("owned", "active", "owned.active", Map.of()).withId(firstActiveEventId);
         EsbEvent second = new RetainedPublicationEvent().withId(secondActiveEventId).withSource(first.getSource()).withType(first.getType());
         activePublishResult.complete(EsbPublisher.getInstance().publish("activePublication", List.of(first, second)));
      }
   }



   /*******************************************************************************
    ** Encoding the second event retains the real lease after the first native send.
    ******************************************************************************/
   public static class RetainedPublicationEvent extends EsbEvent
   {
      /*******************************************************************************
       ** Pause only fixture data access; the publisher and broker are unchanged.
       ******************************************************************************/
      @Override
      public String getType()
      {
         activePublishEntered.countDown();
         try
         {
            if(!activePublishRelease.await(20, TimeUnit.SECONDS))
            {
               throw new IllegalStateException("Owned active publication was not released");
            }
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Owned active publication interrupted", e);
         }
         return super.getType();
      }
   }



   /*******************************************************************************
    ** Hold the real scheduler stop while the native HTTP request finishes.
    ******************************************************************************/
   public static class SchedulerGuard implements BackendStep
   {
      /*******************************************************************************
       **
       ******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         guardEntered.countDown();
         try
         {
            if(!guardRelease.await(20, TimeUnit.SECONDS))
            {
               throw new QException("Owned shutdown guard was not released");
            }
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new QException("Owned shutdown guard interrupted", e);
         }
      }
   }



   /*******************************************************************************
    ** Work entered through the real scheduler or the native HTTP process request.
    ******************************************************************************/
   public static class RetainedJob implements BackendStep
   {
      /*******************************************************************************
       ** Release normal process completion only after the scheduler starts draining.
       ******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         for(var action : QContext.getActionStack())
         {
            if(action instanceof RunProcessInput process)
            {
               processUUID = process.getProcessUUID();
            }
         }
         jobEntered.countDown();
         try
         {
            if(!jobRelease.await(20, TimeUnit.SECONDS))
            {
               throw new QException("Owned scheduled work was not released");
            }
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new QException("Owned scheduled work interrupted", e);
         }
      }
   }
}
