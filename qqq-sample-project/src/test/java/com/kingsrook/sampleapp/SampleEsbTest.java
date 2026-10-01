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

package com.kingsrook.sampleapp;


import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.BooleanSupplier;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.esb.api.EsbRouteProvider;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.metadata.EsbOverviewWidgetMetaDataProducer;
import com.kingsrook.qqq.esb.runtime.EsbTriggerState;
import com.kingsrook.qqq.esb.runtime.QEsbRuntime;
import com.kingsrook.qqq.esb.stats.EsbStats;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** A person write crosses the sample's embedded broker into syncPerson.
 *******************************************************************************/
class SampleEsbTest
{
   /*******************************************************************************
    ** A normal sample definition exposes ESB observability and all management
    ** processes, as well as the person publication and subscriber.
    *******************************************************************************/
   @Test
   void registersEsbAppProcessesAndRoutes() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineInstance();
      assertNotNull(instance.getApp("esb"));
      assertNotNull(instance.getWidget(EsbOverviewWidgetMetaDataProducer.NAME));
      for(String process : List.of("esbPauseTrigger", "esbResumeTrigger", "esbRestartTrigger", "esbReplayDeadLetters",
         "esbPauseQueue", "esbResumeQueue", "esbPurgeQueue", "esbDeleteMessages", "esbMoveMessages"))
      {
         assertNotNull(instance.getProcess(process), process);
      }
      QJavalinMetaData javalin = QJavalinMetaData.of(instance);
      assertNotNull(javalin);
      assertTrue(javalin.getAdditionalRouteProviderReferences().stream()
         .anyMatch(reference -> EsbRouteProvider.class.getName().equals(reference.getName())));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void repeatedCleanStartupRunsSyncPersonOncePerCycle() throws Exception
   {
      String previousPort = System.getProperty("qqq.sample.esb.port");
      String previousMockAuthentication = System.getProperty("qqq.sample.mockAuthentication");
      try(ServerSocket socket = new ServerSocket(0))
      {
         System.setProperty("qqq.sample.esb.port", String.valueOf(socket.getLocalPort()));
      }

      SampleJavalinServer server = null;
      try
      {
         System.setProperty("qqq.sample.mockAuthentication", "true");
         QEsbRuntime.getInstance().stop();
         EsbConnectionManager.getInstance().closeAll();
         for(int cycle = 0; cycle < 2; cycle++)
         {
            server = new SampleJavalinServer();
            server.setPort(0);
            EsbStats.getInstance().reset();
            server.start();
            await(() -> QEsbRuntime.getInstance().getRunner("syncPerson.personEvents").getState() == EsbTriggerState.RUNNING);
            QContext.init(server.getQInstance(), new QSystemUserSession());

            new InsertAction().executeForRecord(new InsertInput("person").withRecord(new QRecord()
               .withValue("firstName", "Event")
               .withValue("lastName", "Sample")
               .withValue("email", "event.sample" + cycle + "@example.invalid")));

            await(() -> EsbStats.getInstance().trigger("syncPerson.personEvents").succeeded() == 1);
            assertEquals(1, EsbStats.getInstance().destination("personEvents").published());
            assertEquals(1, EsbStats.getInstance().trigger("syncPerson.personEvents").consumed());
            assertEquals(0, EsbStats.getInstance().trigger("syncPerson.personEvents").failed());
            QContext.clear();
            server.stop();
            server = null;
            ConnectionManager.resetConnectionProviders();
         }
      }
      finally
      {
         QContext.clear();
         if(server != null)
         {
            server.stop();
         }
         ConnectionManager.resetConnectionProviders();
         if(previousPort == null)
         {
            System.clearProperty("qqq.sample.esb.port");
         }
         else
         {
            System.setProperty("qqq.sample.esb.port", previousPort);
         }
         if(previousMockAuthentication == null)
         {
            System.clearProperty("qqq.sample.mockAuthentication");
         }
         else
         {
            System.setProperty("qqq.sample.mockAuthentication", previousMockAuthentication);
         }
      }
   }



   /*******************************************************************************
    ** Wait for asynchronous consumer activity without assuming scheduling order.
    *******************************************************************************/
   private void await(BooleanSupplier condition) throws InterruptedException
   {
      Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
      while(!condition.getAsBoolean() && Instant.now().isBefore(deadline))
      {
         Thread.sleep(25);
      }
      assertTrue(condition.getAsBoolean(), "Timed out waiting for sample ESB delivery");
   }
}
