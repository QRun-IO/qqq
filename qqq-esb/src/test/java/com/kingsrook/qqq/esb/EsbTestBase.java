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

package com.kingsrook.qqq.esb;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
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
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;


/*******************************************************************************
 * Base class for qqq-esb unit tests.
 *
 * Starts an embedded ActiveMQ Artemis broker (in-memory, no security) before
 * each test class, and stops it after.  Puts a fresh memory-backend QInstance
 * (table `order`, process `syncOrder`, and an ESB provider named `artemis`
 * pointing at the embedded broker) into the QContext before each test.
 *
 * The broker's localhost port is picked (a free, random one) once per JVM, and
 * then reused by every test class and every restart - so tests can stop and
 * start the broker and expect clients to reconnect to the same URL.
 *******************************************************************************/
public class EsbTestBase
{
   public static final String BACKEND_NAME            = "memory";
   public static final String TABLE_NAME_ORDER        = "order";
   public static final String PROCESS_NAME_SYNC_ORDER = "syncOrder";
   public static final String PROVIDER_NAME           = "artemis";

   private static EmbeddedActiveMQ embeddedBroker;
   private static Integer          brokerPort;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeAll
   static void esbTestBaseBeforeAll() throws Exception
   {
      startEmbeddedBroker();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterAll
   static void esbTestBaseAfterAll() throws Exception
   {
      stopEmbeddedBroker();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void esbTestBaseBeforeEach()
   {
      QContext.init(defineInstance(), new QSession());
      MemoryRecordStore.getInstance().reset();
      SyncOrderStep.reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void esbTestBaseAfterEach()
   {
      QContext.clear();
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    ** Start the embedded broker, if it isn't already running.
    *******************************************************************************/
   public static synchronized void startEmbeddedBroker() throws Exception
   {
      if(embeddedBroker != null)
      {
         return;
      }

      if(brokerPort == null)
      {
         brokerPort = findFreePort();
      }

      String dataDirectory = "target/embedded-artemis/" + brokerPort;

      Configuration configuration = new ConfigurationImpl()
         .setPersistenceEnabled(false)
         .setSecurityEnabled(false)
         .setJMXManagementEnabled(false)
         .setBindingsDirectory(dataDirectory + "/bindings")
         .setJournalDirectory(dataDirectory + "/journal")
         .setPagingDirectory(dataDirectory + "/paging")
         .setLargeMessagesDirectory(dataDirectory + "/largemessages")
         .addAcceptorConfiguration("tcp", getBrokerUrl());

      EmbeddedActiveMQ broker = new EmbeddedActiveMQ().setConfiguration(configuration);
      broker.start();
      embeddedBroker = broker;
   }



   /*******************************************************************************
    ** Stop the embedded broker, if it is running.
    *******************************************************************************/
   public static synchronized void stopEmbeddedBroker() throws Exception
   {
      if(embeddedBroker == null)
      {
         return;
      }

      try
      {
         embeddedBroker.stop();
      }
      finally
      {
         embeddedBroker = null;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static synchronized boolean isEmbeddedBrokerRunning()
   {
      return (embeddedBroker != null && embeddedBroker.getActiveMQServer().isStarted());
   }



   /*******************************************************************************
    ** The embedded broker's server (e.g., to register a broker plugin that
    ** watches what clients send) - or null if the broker isn't running.
    *******************************************************************************/
   public static synchronized ActiveMQServer getEmbeddedBrokerServer()
   {
      return (embeddedBroker == null ? null : embeddedBroker.getActiveMQServer());
   }



   /*******************************************************************************
    ** URL of the embedded broker, e.g., tcp://localhost:61616 (on the port that
    ** was picked, once per JVM, when the broker first started).
    *******************************************************************************/
   public static String getBrokerUrl()
   {
      return ("tcp://localhost:" + brokerPort);
   }



   /*******************************************************************************
    ** Build a new (not yet validated) QInstance for ESB tests.
    *******************************************************************************/
   public static QInstance defineInstance()
   {
      QInstance qInstance = new QInstance();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData()
         .withName("mock")
         .withType(QAuthenticationType.MOCK));

      qInstance.addBackend(new QBackendMetaData()
         .withName(BACKEND_NAME)
         .withBackendType(MemoryBackendModule.class));

      qInstance.addTable(defineTableOrder());
      qInstance.addProcess(defineProcessSyncOrder());

      EsbInstanceMetaData.of(qInstance).withProvider(new QEsbProviderMetaData()
         .withName(PROVIDER_NAME)
         .withType(EsbProviderType.ACTIVEMQ_ARTEMIS)
         .withUrl(getBrokerUrl()));

      return (qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QTableMetaData defineTableOrder()
   {
      return (new QTableMetaData()
         .withName(TABLE_NAME_ORDER)
         .withLabel("Order")
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("orderNo", QFieldType.STRING))
         .withField(new QFieldMetaData("status", QFieldType.STRING)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QProcessMetaData defineProcessSyncOrder()
   {
      return (new QProcessMetaData()
         .withName(PROCESS_NAME_SYNC_ORDER)
         .withLabel("Sync Order")
         .withTableName(TABLE_NAME_ORDER)
         .withStep(new QBackendStepMetaData()
            .withName("sync")
            .withCode(new QCodeReference(SyncOrderStep.class))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int findFreePort() throws IOException
   {
      try(ServerSocket serverSocket = new ServerSocket(0))
      {
         return (serverSocket.getLocalPort());
      }
   }



   /*******************************************************************************
    * Backend step for the syncOrder process - counts its runs, so tests can
    * assert how many times the process ran.
    *******************************************************************************/
   public static class SyncOrderStep implements BackendStep
   {
      private static final AtomicInteger runCount = new AtomicInteger(0);



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         runCount.incrementAndGet();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static int getRunCount()
      {
         return (runCount.get());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static void reset()
      {
         runCount.set(0);
      }
   }

}
