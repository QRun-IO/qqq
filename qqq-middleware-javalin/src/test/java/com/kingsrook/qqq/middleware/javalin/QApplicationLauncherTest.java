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

package com.kingsrook.qqq.middleware.javalin;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractMetaDataProducerBasedQQQApplication;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.instances.MetaDataProducerBasedQQQApplication;
import com.kingsrook.qqq.backend.core.instances.QRuntimeServiceInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.simple.SimpleSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.middleware.javalin.launcherproducers.TestFailingMetaDataProducer;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QApplicationLauncher
 *******************************************************************************/
class QApplicationLauncherTest
{
   private static final int    PORT           = 6281;
   private static final String METADATA_URL   = "http://localhost:" + PORT + "/metaData";
   private static final String SCHEDULER_NAME = "simpleScheduler";

   private static final List<String> events = new ArrayList<>();

   private static QInstance serviceStartedWithInstance = null;

   private QApplicationLauncher launcher;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      events.clear();
      serviceStartedWithInstance = null;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      if(launcher != null)
      {
         launcher.stop();
         launcher = null;
      }

      try
      {
         QScheduleManager.getInstance().stop();
         QScheduleManager.getInstance().unInit();
      }
      catch(IllegalStateException e)
      {
         ////////////////////////////////////////////////////////
         // ok - means the schedule manager was never init'ed. //
         ////////////////////////////////////////////////////////
      }

      Unirest.config().reset();
   }



   /*******************************************************************************
    ** The server starts first, then the runtime services in registration order,
    ** all with the one instance the server built; stop runs in reverse.
    *******************************************************************************/
   @Test
   void testStartsServerThenServicesAndStopsInReverse() throws Exception
   {
      TestApplication application = new TestApplication(qInstance -> qInstance
         .withRuntimeService(new QCodeReference(ServiceA.class))
         .withRuntimeService(new QCodeReference(ServiceB.class)));

      launcher = QApplicationLauncher.run(application, newConfig());

      assertEquals(List.of(QApplicationLauncher.JAVALIN_SERVER_SERVICE_NAME, ServiceA.NAME, ServiceB.NAME), launcher.getStartedServiceNames());
      assertEquals(List.of("start:" + ServiceA.NAME, "start:" + ServiceB.NAME), events);
      assertEquals(200, Unirest.get(METADATA_URL).asString().getStatus());
      assertNull(launcher.getShutdownHook());

      assertEquals(1, application.defineCount);
      assertNotNull(launcher.getQInstance());
      assertSame(launcher.getServer().getQInstance(), launcher.getQInstance());
      assertSame(launcher.getQInstance(), serviceStartedWithInstance);

      launcher.stop();
      assertEquals(List.of("start:" + ServiceA.NAME, "start:" + ServiceB.NAME, "stop:" + ServiceB.NAME, "stop:" + ServiceA.NAME), events);
      assertThat(launcher.getStartedServiceNames()).isEmpty();
      assertServerIsNotRunning();

      //////////////////////////////////
      // stopping again does nothing. //
      //////////////////////////////////
      launcher.stop();
      assertEquals(4, events.size());
   }



   /*******************************************************************************
    ** A service that fails to start stops the ones already started (including
    ** the server), skips the rest, and fails the launch.
    *******************************************************************************/
   @Test
   void testServiceFailingToStartStopsStartedServicesAndFailsLaunch()
   {
      TestApplication application = new TestApplication(qInstance -> qInstance
         .withRuntimeService(new QCodeReference(ServiceA.class))
         .withRuntimeService(new QCodeReference(FailingService.class))
         .withRuntimeService(new QCodeReference(ServiceB.class)));

      assertThatThrownBy(() -> QApplicationLauncher.run(application, newConfig()))
         .isInstanceOf(QException.class)
         .hasMessageContaining(FailingService.NAME)
         .hasRootCauseMessage(FailingService.MESSAGE);

      assertEquals(List.of("start:" + ServiceA.NAME, "start:" + FailingService.NAME, "stop:" + ServiceA.NAME), events);
      assertServerIsNotRunning();
   }



   /*******************************************************************************
    ** With nothing scheduled, the schedule manager is not started (or init'ed).
    *******************************************************************************/
   @Test
   void testScheduleManagerSkippedWhenNothingIsScheduled() throws QException
   {
      TestApplication application = new TestApplication(qInstance -> qInstance
         .withRuntimeService(new QCodeReference(ServiceA.class)));

      launcher = QApplicationLauncher.run(application, newConfig());

      assertEquals(List.of(QApplicationLauncher.JAVALIN_SERVER_SERVICE_NAME, ServiceA.NAME), launcher.getStartedServiceNames());
      assertThatThrownBy(QScheduleManager::getInstance).isInstanceOf(IllegalStateException.class);
   }



   /*******************************************************************************
    ** With a scheduled process, the schedule manager starts after the server and
    ** before the runtime services, and stop releases it.
    *******************************************************************************/
   @Test
   void testScheduleManagerStartedWhenSomethingIsScheduled() throws QException
   {
      TestApplication application = new TestApplication(qInstance ->
      {
         addScheduledProcess(qInstance);
         qInstance.withRuntimeService(new QCodeReference(ServiceA.class));
      });

      launcher = QApplicationLauncher.run(application, newConfig());

      assertEquals(List.of(QApplicationLauncher.JAVALIN_SERVER_SERVICE_NAME, QApplicationLauncher.SCHEDULE_MANAGER_SERVICE_NAME, ServiceA.NAME), launcher.getStartedServiceNames());
      assertNotNull(QScheduleManager.getInstance());

      launcher.stop();
      assertEquals(List.of("start:" + ServiceA.NAME, "stop:" + ServiceA.NAME), events);
      assertThatThrownBy(QScheduleManager::getInstance).isInstanceOf(IllegalStateException.class);
   }



   /*******************************************************************************
    ** Anything with a schedule, or the scheduledJob table, means the schedule
    ** manager is needed.
    *******************************************************************************/
   @Test
   void testHasSchedules()
   {
      QScheduleMetaData schedule = new QScheduleMetaData().withSchedulerName(SCHEDULER_NAME).withRepeatSeconds(60);

      assertFalse(QApplicationLauncher.hasSchedules(new QInstance()));
      assertFalse(QApplicationLauncher.hasSchedules(withTable(new QTableMetaData().withName("notScheduled"))));

      QInstance qInstance = new QInstance();
      qInstance.addProcess(new QProcessMetaData().withName("notScheduled"));
      qInstance.addQueue(new QQueueMetaData().withName("notScheduled"));
      assertFalse(QApplicationLauncher.hasSchedules(qInstance));

      qInstance = new QInstance();
      qInstance.addProcess(new QProcessMetaData().withName("scheduled").withSchedule(schedule));
      assertTrue(QApplicationLauncher.hasSchedules(qInstance));

      qInstance = new QInstance();
      qInstance.addQueue(new QQueueMetaData().withName("scheduled").withSchedule(schedule));
      assertTrue(QApplicationLauncher.hasSchedules(qInstance));

      assertTrue(QApplicationLauncher.hasSchedules(withTable(new QTableMetaData().withName("scheduled").withAutomationDetails(new QTableAutomationDetails().withSchedule(schedule)))));
      assertFalse(QApplicationLauncher.hasSchedules(withTable(new QTableMetaData().withName("notScheduled").withAutomationDetails(new QTableAutomationDetails()))));
      assertTrue(QApplicationLauncher.hasSchedules(withTable(new QTableMetaData().withName(ScheduledJob.TABLE_NAME))));
   }



   /*******************************************************************************
    ** The shutdown hook stops everything in reverse order, and is removed once
    ** it has run.
    *******************************************************************************/
   @Test
   void testShutdownHookStopsServicesInReverse() throws QException
   {
      TestApplication application = new TestApplication(qInstance -> qInstance
         .withRuntimeService(new QCodeReference(ServiceA.class))
         .withRuntimeService(new QCodeReference(ServiceB.class)));

      launcher = QApplicationLauncher.run(application, newConfig().withRegisterShutdownHook(true));

      Thread shutdownHook = launcher.getShutdownHook();
      assertNotNull(shutdownHook);

      ////////////////////////////////////////////////
      // run it the way the jvm would, at shutdown. //
      ////////////////////////////////////////////////
      shutdownHook.run();

      assertEquals(List.of("start:" + ServiceA.NAME, "start:" + ServiceB.NAME, "stop:" + ServiceB.NAME, "stop:" + ServiceA.NAME), events);
      assertServerIsNotRunning();
      assertNull(launcher.getShutdownHook());
      assertFalse(Runtime.getRuntime().removeShutdownHook(shutdownHook), "stop should have removed the shutdown hook");
   }



   /*******************************************************************************
    ** failOnMetaDataProducerError makes a producer-based application's instance
    ** fail-fast, so a failing producer fails the launch.
    *******************************************************************************/
   @Test
   void testFailOnMetaDataProducerError()
   {
      AbstractMetaDataProducerBasedQQQApplication application = new MetaDataProducerBasedQQQApplication(TestFailingMetaDataProducer.class);

      assertThatThrownBy(() -> QApplicationLauncher.run(application, newConfig().withFailOnMetaDataProducerError(true)))
         .isInstanceOf(QException.class)
         .hasMessageContaining(QApplicationLauncher.JAVALIN_SERVER_SERVICE_NAME)
         .hasRootCauseMessage(TestFailingMetaDataProducer.MESSAGE);

      assertTrue(application.getFailOnMetaDataProducerError());
      assertServerIsNotRunning();
   }



   /*******************************************************************************
    ** failOnMetaDataProducerError can't be applied to an application that builds
    ** its own instance, so the launch fails before anything starts.
    *******************************************************************************/
   @Test
   void testFailOnMetaDataProducerErrorRequiresProducerBasedApplication()
   {
      TestApplication application = new TestApplication(qInstance -> qInstance
         .withRuntimeService(new QCodeReference(ServiceA.class)));

      assertThatThrownBy(() -> QApplicationLauncher.run(application, newConfig().withFailOnMetaDataProducerError(true)))
         .isInstanceOf(QException.class)
         .hasMessageContaining(AbstractMetaDataProducerBasedQQQApplication.class.getSimpleName());

      assertEquals(0, application.defineCount);
      assertThat(events).isEmpty();
   }



   /***************************************************************************
    ** config for tests: the test port, no material dashboard, no shutdown hook.
    ***************************************************************************/
   private static QApplicationLauncherConfig newConfig()
   {
      return new QApplicationLauncherConfig()
         .withRegisterShutdownHook(false)
         .withServerCustomizer(server -> server
            .withPort(PORT)
            .withServeFrontendMaterialDashboard(false));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void assertServerIsNotRunning()
   {
      assertThatThrownBy(() -> Unirest.get(METADATA_URL).asString()).isInstanceOf(UnirestException.class);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QInstance withTable(QTableMetaData table)
   {
      QInstance qInstance = new QInstance();
      qInstance.addTable(table);
      return (qInstance);
   }



   /***************************************************************************
    ** add a simple scheduler, and a process on it that won't run during a test.
    ***************************************************************************/
   private static void addScheduledProcess(QInstance qInstance)
   {
      qInstance.addScheduler(new SimpleSchedulerMetaData().withName(SCHEDULER_NAME));
      qInstance.addProcess(new QProcessMetaData()
         .withName("scheduledProcess")
         .withSchedule(new QScheduleMetaData()
            .withSchedulerName(SCHEDULER_NAME)
            .withRepeatSeconds(3600)
            .withInitialDelaySeconds(3600))
         .withStepList(List.of(new QBackendStepMetaData()
            .withName("noop")
            .withCode(new QCodeReference(NoopStep.class)))));
   }



   /***************************************************************************
    ** application with a minimal instance, plus whatever the test adds to it.
    ***************************************************************************/
   public static class TestApplication extends AbstractQQQApplication
   {
      private final Consumer<QInstance> customizer;

      private int defineCount = 0;



      /***************************************************************************
       **
       ***************************************************************************/
      public TestApplication(Consumer<QInstance> customizer)
      {
         this.customizer = customizer;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QInstance defineQInstance()
      {
         defineCount++;

         QInstance qInstance = new QInstance();
         qInstance.addBackend(new QBackendMetaData().withBackendType(MemoryBackendModule.class).withName("memory"));
         qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withName("anon").withType(QAuthenticationType.FULLY_ANONYMOUS));
         qInstance.addTable(new QTableMetaData()
            .withName("table")
            .withBackendName("memory")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER))
            .withPrimaryKeyField("id"));

         customizer.accept(qInstance);
         return (qInstance);
      }
   }



   /***************************************************************************
    ** runtime service that records when it starts and stops.
    ***************************************************************************/
   public abstract static class RecordingService implements QRuntimeServiceInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void start(QInstance qInstance) throws QException
      {
         events.add("start:" + getName());
         serviceStartedWithInstance = qInstance;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void stop()
      {
         events.add("stop:" + getName());
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class ServiceA extends RecordingService
   {
      public static final String NAME = "serviceA";



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getName()
      {
         return (NAME);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class ServiceB extends RecordingService
   {
      public static final String NAME = "serviceB";



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getName()
      {
         return (NAME);
      }
   }



   /***************************************************************************
    ** runtime service whose start fails.
    ***************************************************************************/
   public static class FailingService extends RecordingService
   {
      public static final String NAME    = "failingService";
      public static final String MESSAGE = "failingService could not start";



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getName()
      {
         return (NAME);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void start(QInstance qInstance) throws QException
      {
         super.start(qInstance);
         throw (new QException(MESSAGE));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class NoopStep implements BackendStep
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
      {
      }
   }
}
