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

package com.kingsrook.qqq.middleware.javalin;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractMetaDataProducerBasedQQQApplication;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.instances.QRuntimeServiceInterface;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** One entry point that starts everything an application has configured, in
 ** this order:
 ** - its QApplicationJavalinServer (which builds the application's QInstance);
 ** - the QScheduleManager, when that instance has anything scheduled (or the
 **   scheduledJob table);
 ** - each of the instance's runtime services (QInstance.withRuntimeService).
 ** All of them use the one QInstance the server built.
 **
 ** stop (and, unless turned off in the config, a JVM shutdown hook) stops them
 ** in reverse order.  If anything fails to start (including a LinkageError,
 ** e.g., from a library missing from the classpath), the ones already started
 ** are stopped, and run throws.
 **
 ** e.g., in a main method:
 ** QApplicationLauncher.run(new MyApplication(), new QApplicationLauncherConfig()
 **    .withServerCustomizer(server -> server.withPort(8080))
 **    .withFailOnMetaDataProducerError(true));
 *******************************************************************************/
public class QApplicationLauncher
{
   private static final QLogger LOG = QLogger.getLogger(QApplicationLauncher.class);

   public static final String JAVALIN_SERVER_SERVICE_NAME   = "javalinServer";
   public static final String SCHEDULE_MANAGER_SERVICE_NAME = "scheduleManager";

   private final AbstractQQQApplication     application;
   private final QApplicationLauncherConfig config;

   private QApplicationJavalinServer server;
   private QInstance                 qInstance;
   private Thread                    shutdownHook;

   private final List<StartedService> startedServices = new ArrayList<>();



   /***************************************************************************
    ** something the launcher started, and how to stop it.
    ***************************************************************************/
   private record StartedService(String name, Runnable stopper)
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   private QApplicationLauncher(AbstractQQQApplication application, QApplicationLauncherConfig config)
   {
      this.application = application;
      this.config = config;
   }



   /*******************************************************************************
    ** Start the application's server, schedule manager, and runtime services,
    ** returning the launcher that can stop them.  A null config means the
    ** defaults.
    *******************************************************************************/
   public static QApplicationLauncher run(AbstractQQQApplication application, QApplicationLauncherConfig config) throws QException
   {
      QApplicationLauncher launcher = new QApplicationLauncher(application, Objects.requireNonNullElseGet(config, QApplicationLauncherConfig::new));
      launcher.start();
      return (launcher);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void start() throws QException
   {
      applyFailOnMetaDataProducerError();

      server = new QApplicationJavalinServer(application);
      if(config.getServerCustomizer() != null)
      {
         config.getServerCustomizer().accept(server);
      }

      String serviceName = JAVALIN_SERVER_SERVICE_NAME;
      try
      {
         ////////////////////////////////////////////////////////////////////////////
         // track the server (and the schedule manager, below) before starting it, //
         // so a failed start still stops whatever it got running.                 //
         ////////////////////////////////////////////////////////////////////////////
         startedServices.add(new StartedService(JAVALIN_SERVER_SERVICE_NAME, server::stop));
         server.start();
         qInstance = server.getQInstance();

         if(hasSchedules(qInstance))
         {
            serviceName = SCHEDULE_MANAGER_SERVICE_NAME;
            QScheduleManager scheduleManager = QScheduleManager.initInstance(qInstance, config.getSystemUserSessionSupplier());
            startedServices.add(new StartedService(SCHEDULE_MANAGER_SERVICE_NAME, () ->
            {
               scheduleManager.stop();
               scheduleManager.unInit();
            }));
            scheduleManager.start();
         }
         else
         {
            LOG.info("Not starting the schedule manager, as nothing is scheduled.");
         }

         for(QCodeReference codeReference : CollectionUtils.nonNullList(qInstance.getRuntimeServices()))
         {
            serviceName = codeReference.getName();
            QRuntimeServiceInterface runtimeService = QCodeLoader.getAdHoc(QRuntimeServiceInterface.class, codeReference);
            if(runtimeService == null)
            {
               throw (new QException("Could not load runtime service [" + codeReference.getName() + "]"));
            }

            serviceName = runtimeService.getName();
            LOG.info("Starting runtime service", logPair("service", serviceName));
            runtimeService.start(qInstance);
            startedServices.add(new StartedService(serviceName, runtimeService::stop));
         }
      }
      catch(Exception | LinkageError e)
      {
         LOG.warn("Error starting application service - stopping the ones already started", logPair("service", serviceName), logPair("error", e.getMessage()));
         stop();
         throw (new QException("Error starting application service [" + serviceName + "]: " + e.getMessage(), e));
      }

      if(BooleanUtils.isTrue(config.getRegisterShutdownHook()))
      {
         shutdownHook = new Thread(this::stop, "qqq-application-launcher-shutdown");
         Runtime.getRuntime().addShutdownHook(shutdownHook);
      }

      LOG.info("Application started", logPair("services", getStartedServiceNames()));
   }



   /***************************************************************************
    ** the instance is built inside the application, so the flag has to be set
    ** there - which only a producer-based application supports.
    ***************************************************************************/
   private void applyFailOnMetaDataProducerError() throws QException
   {
      if(!BooleanUtils.isTrue(config.getFailOnMetaDataProducerError()))
      {
         return;
      }

      if(application instanceof AbstractMetaDataProducerBasedQQQApplication producerBasedApplication)
      {
         producerBasedApplication.setFailOnMetaDataProducerError(true);
      }
      else
      {
         throw (new QException("failOnMetaDataProducerError needs an application that extends " + AbstractMetaDataProducerBasedQQQApplication.class.getSimpleName()
            + ", but [" + application.getClass().getName() + "] does not; set failOnMetaDataProducerError on the QInstance it defines instead."));
      }
   }



   /*******************************************************************************
    ** Whether the instance needs the QScheduleManager: if any process, queue or
    ** table automation has a schedule, or it has the scheduledJob table.
    *******************************************************************************/
   static boolean hasSchedules(QInstance qInstance)
   {
      return (CollectionUtils.nonNullMap(qInstance.getTables()).containsKey(ScheduledJob.TABLE_NAME)
         || CollectionUtils.nonNullMap(qInstance.getProcesses()).values().stream().anyMatch(process -> process.getSchedule() != null)
         || CollectionUtils.nonNullMap(qInstance.getQueues()).values().stream().anyMatch(queue -> queue.getSchedule() != null)
         || CollectionUtils.nonNullMap(qInstance.getTables()).values().stream().anyMatch(table -> table.getAutomationDetails() != null && table.getAutomationDetails().getSchedule() != null));
   }



   /*******************************************************************************
    ** Stop everything the launcher started, in reverse order.  An error stopping
    ** one is logged, and the rest are still stopped.  Calling it again does
    ** nothing.
    *******************************************************************************/
   public synchronized void stop()
   {
      for(int i = startedServices.size() - 1; i >= 0; i--)
      {
         StartedService startedService = startedServices.get(i);
         try
         {
            LOG.info("Stopping application service", logPair("service", startedService.name()));
            startedService.stopper().run();
         }
         catch(Exception | LinkageError e)
         {
            LOG.warn("Error stopping application service", e, logPair("service", startedService.name()));
         }
      }
      startedServices.clear();

      if(shutdownHook != null)
      {
         try
         {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
         }
         catch(IllegalStateException e)
         {
            ///////////////////////////////////////////////////////////////////////////
            // the jvm is already shutting down (e.g., this is running in the hook), //
            // so the hook can't be - and needn't be - removed.                      //
            ///////////////////////////////////////////////////////////////////////////
         }
         shutdownHook = null;
      }
   }



   /*******************************************************************************
    ** Names of what's running, in the order it was started.
    *******************************************************************************/
   public synchronized List<String> getStartedServiceNames()
   {
      return (startedServices.stream().map(StartedService::name).toList());
   }



   /*******************************************************************************
    ** Getter for the QInstance the server built, which the schedule manager and
    ** runtime services were started with.
    *******************************************************************************/
   public QInstance getQInstance()
   {
      return (this.qInstance);
   }



   /*******************************************************************************
    ** Getter for the javalin server
    *******************************************************************************/
   public QApplicationJavalinServer getServer()
   {
      return (this.server);
   }



   /*******************************************************************************
    ** Getter for the JVM shutdown hook, while it's registered.
    *******************************************************************************/
   Thread getShutdownHook()
   {
      return (this.shutdownHook);
   }

}
