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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import com.kingsrook.qqq.middleware.javalin.QApplicationLauncher;
import com.kingsrook.qqq.middleware.javalin.QApplicationLauncherConfig;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Force a control worker to cross application shutdown after its listening check.
 ** Native broker connections are observed before inherited manager cleanup.
 ******************************************************************************/
@Timeout(40)
class EsbControlChannelLifecycleTest extends EsbRuntimeTestBase
{
   /*******************************************************************************
    ** The old worker must finish before cleanup or refuse admission after close.
    ** Restart the same provider name against a second real broker to detect stale
    ** QInstance configuration as well as a lingering native connection.
    ******************************************************************************/
   @Test
   void shutdownCannotRecreateProviderFromInFlightControlSetup() throws Exception
   {
      QInstance first = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      QApplicationLauncher launcher = launch(first);
      QEsbRuntime runtime = QEsbRuntime.getInstance();
      GatedChannel gated = new GatedChannel(runtime, first);
      CompletableFuture<Void> stopped = new CompletableFuture<>();
      Thread stopper = null;
      try
      {
         waitFor("original control listener", runtime::isRunning);
         Field field = QEsbRuntime.class.getDeclaredField("controlChannel");
         field.setAccessible(true);
         EsbControlChannel original = (EsbControlChannel) field.get(runtime);
         original.close();
         field.set(runtime, gated);
         gated.start(Set.of(PROVIDER_NAME));
         assertTrue(gated.entered.await(5, TimeUnit.SECONDS));
         assertTrue(getEmbeddedBrokerServer().getConnectionCount() > 0);
         stopper = Thread.ofPlatform().name("owned-launcher-stop").start(() ->
         {
            try
            {
               launcher.stop();
               stopped.complete(null);
            }
            catch(Throwable failure)
            {
               stopped.completeExceptionally(failure);
            }
         });

         // On the old code shutdown returns before releasing setup. Coordinated
         // shutdown instead interrupts the worker so this test can release it.
         CompletableFuture.anyOf(stopped, gated.interrupted).get(10, TimeUnit.SECONDS);
         gated.release.countDown();
         stopped.get(10, TimeUnit.SECONDS);
         gated.worker.join(TimeUnit.SECONDS.toMillis(5));
         assertFalse(gated.worker.isAlive(), "Old control worker survived launcher shutdown");
         assertFalse(EsbConnectionManager.getInstance().isConnected(PROVIDER_NAME), "Old control setup recreated a provider after final cleanup");
         waitFor("no old broker connections", () -> getEmbeddedBrokerServer().getConnectionCount() == 0);
         assertTrue(Thread.getAllStackTraces().keySet().stream()
            .noneMatch(thread -> thread.isAlive() && thread.getName().equals("qqq-esb-reconnect-" + PROVIDER_NAME)));

         String nextUrl = "vm://" + (UUID.randomUUID().hashCode() & Integer.MAX_VALUE);
         EmbeddedActiveMQ nextBroker = new EmbeddedActiveMQ().setConfiguration(new ConfigurationImpl()
            .setPersistenceEnabled(false).setSecurityEnabled(false).setJMXManagementEnabled(false)
            .addAcceptorConfiguration("owned-next-control", nextUrl));
         nextBroker.start();
         try
         {
            QInstance next = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
            EsbInstanceMetaData.of(next).getProvider(PROVIDER_NAME).setUrl(nextUrl);
            QApplicationLauncher restarted = launch(next);
            try
            {
               waitFor("new control listener", runtime::isRunning);
               waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
               var event = sendEvent(QUEUE_NAME, Map.of("generation", "new"));
               waitFor("new broker event", () -> RecordingStep.getCompletedRuns().size() == 1);
               assertEquals(event.getId(), RecordingStep.getRuns().get(0).getEvents().get(0).getId());
               assertEquals(0, getEmbeddedBrokerServer().getConnectionCount());
               assertTrue(nextBroker.getActiveMQServer().getConnectionCount() > 0);
            }
            finally
            {
               restarted.stop();
            }
            waitFor("new broker cleanup", () -> nextBroker.getActiveMQServer().getConnectionCount() == 0);
         }
         finally
         {
            nextBroker.stop();
         }
      }
      finally
      {
         gated.release.countDown();
         if(stopper != null)
         {
            stopper.join(TimeUnit.SECONDS.toMillis(10));
         }
         launcher.stop();
         gated.close();
         if(gated.worker != null)
         {
            gated.worker.join(TimeUnit.SECONDS.toMillis(5));
         }
      }
   }



   /*******************************************************************************
    ** A worker already resolving its native connection must be interrupted before
    ** close waits on setup; holding the channel lock first would deadlock here.
    ******************************************************************************/
   @Test
   void shutdownInterruptsInFlightProviderSetupBeforeWaiting() throws Exception
   {
      QInstance instance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      QApplicationLauncher launcher = launch(instance);
      QEsbRuntime runtime = QEsbRuntime.getInstance();
      GatedProvider provider = new GatedProvider();
      provider.withName("ownedSetup").withType(EsbProviderType.ACTIVEMQ_ARTEMIS).withUrl(getBrokerUrl());
      EsbInstanceMetaData.of(instance).withProvider(provider);
      EsbControlChannel channel = new EsbControlChannel(runtime, instance);
      CompletableFuture<Void> stopped = new CompletableFuture<>();
      Thread stopper = null;
      try
      {
         waitFor("initial control channel", runtime::isRunning);
         Field field = QEsbRuntime.class.getDeclaredField("controlChannel");
         field.setAccessible(true);
         ((EsbControlChannel) field.get(runtime)).close();
         field.set(runtime, channel);
         channel.start(Set.of("ownedSetup"));
         assertTrue(provider.entered.await(5, TimeUnit.SECONDS));
         stopper = Thread.ofPlatform().name("owned-in-flight-stop").start(() ->
         {
            try
            {
               launcher.stop();
               stopped.complete(null);
            }
            catch(Throwable failure)
            {
               stopped.completeExceptionally(failure);
            }
         });
         assertTrue(provider.interrupted.await(5, TimeUnit.SECONDS), "Shutdown did not interrupt in-flight provider setup");
         assertFalse(stopped.isDone(), "Shutdown returned before in-flight setup could finish");
         provider.release.countDown();
         stopped.get(10, TimeUnit.SECONDS);
         provider.worker.join(TimeUnit.SECONDS.toMillis(5));
         assertFalse(provider.worker.isAlive());
         assertFalse(EsbConnectionManager.getInstance().isConnected("ownedSetup"));
         assertFalse(EsbConnectionManager.getInstance().isConnected(PROVIDER_NAME));
         waitFor("all native connections closed", () -> getEmbeddedBrokerServer().getConnectionCount() == 0);
         waitFor("no setup reconnect worker", () -> Thread.getAllStackTraces().keySet().stream()
            .noneMatch(thread -> thread.isAlive() && thread.getName().equals("qqq-esb-reconnect-ownedSetup")));
      }
      finally
      {
         provider.release.countDown();
         if(stopper != null)
         {
            stopper.join(TimeUnit.SECONDS.toMillis(10));
         }
         launcher.stop();
         channel.close();
      }
   }



   /*******************************************************************************
    ** Hold the existing metadata lookup inside real manager/factory setup.
    ** No manager, factory, connection, session, or native broker is substituted.
    ******************************************************************************/
   private static class GatedProvider extends QEsbProviderMetaData
   {
      private final CountDownLatch entered = new CountDownLatch(1);
      private final CountDownLatch interrupted = new CountDownLatch(1);
      private final CountDownLatch release = new CountDownLatch(1);
      private volatile Thread worker;



      /*******************************************************************************
       ** Record cancellation but keep setup in flight until the test releases it.
       ******************************************************************************/
      @Override
      public String getUrl()
      {
         worker = Thread.currentThread();
         entered.countDown();
         boolean restoreInterrupt = false;
         while(true)
         {
            try
            {
               if(!release.await(15, TimeUnit.SECONDS))
               {
                  throw new AssertionError("Provider setup gate was not released");
               }
               break;
            }
            catch(InterruptedException e)
            {
               restoreInterrupt = true;
               interrupted.countDown();
            }
         }
         if(restoreInterrupt)
         {
            Thread.currentThread().interrupt();
         }
         return super.getUrl();
      }
   }



   /*******************************************************************************
    ** Replace only scheduling at the existing listening check; setup remains real.
    ******************************************************************************/
   private static class GatedChannel extends EsbControlChannel
   {
      private final CountDownLatch entered = new CountDownLatch(1);
      private final CountDownLatch release = new CountDownLatch(1);
      private final CompletableFuture<Void> interrupted = new CompletableFuture<>();
      private volatile Thread worker;



      /*******************************************************************************
       ** Keep the original runtime and metadata for the delayed worker.
       ******************************************************************************/
      GatedChannel(QEsbRuntime runtime, QInstance instance)
      {
         super(runtime, instance);
      }



      /*******************************************************************************
       ** Let shutdown overtake the check without mocking any JMS operation.
       ** An interrupt is recorded but cannot itself release this scheduling gate.
       ******************************************************************************/
      @Override
      Boolean isListening(String providerName)
      {
         Boolean listening = super.isListening(providerName);
         if(!listening && Thread.currentThread().getName().startsWith("qqq-esb-control-"))
         {
            worker = Thread.currentThread();
            entered.countDown();
            boolean restoreInterrupt = false;
            while(true)
            {
               try
               {
                  if(!release.await(15, TimeUnit.SECONDS))
                  {
                     throw new AssertionError("Control setup gate was not released");
                  }
                  break;
               }
               catch(InterruptedException e)
               {
                  restoreInterrupt = true;
                  interrupted.complete(null);
               }
            }
            if(restoreInterrupt)
            {
               Thread.currentThread().interrupt();
            }
         }
         return listening;
      }
   }



   /*******************************************************************************
    ** Use the actual launcher and an ephemeral loopback HTTP connector.
    ******************************************************************************/
   private QApplicationLauncher launch(QInstance instance) throws Exception
   {
      return QApplicationLauncher.run(new AbstractQQQApplication()
      {
         /*******************************************************************************
          ** Return this test's native broker metadata.
          ******************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      }, new QApplicationLauncherConfig().withRegisterShutdownHook(false)
         .withServerCustomizer(server -> server.withPort(0)
            .withServeFrontendNext(false).withServeFrontendMaterialDashboard(false)
            .withJavalinConfigCustomizer(config -> config.jetty.host = "127.0.0.1")));
   }
}
