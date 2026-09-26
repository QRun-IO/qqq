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

package com.kingsrook.sampleapp;


import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.runtime.EsbTriggerState;
import com.kingsrook.qqq.esb.runtime.QEsbRuntime;
import com.kingsrook.qqq.esb.stats.EsbStats;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** A person write crosses the sample's embedded broker into syncPerson.
 *******************************************************************************/
class SampleEsbTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void insertedPersonRunsSyncPersonOnce() throws Exception
   {
      String previousPort = System.getProperty("qqq.sample.esb.port");
      String previousMockAuthentication = System.getProperty("qqq.sample.mockAuthentication");
      try(ServerSocket socket = new ServerSocket(0))
      {
         System.setProperty("qqq.sample.esb.port", String.valueOf(socket.getLocalPort()));
      }

      SampleJavalinServer server = new SampleJavalinServer();
      server.setPort(0);
      try
      {
         System.setProperty("qqq.sample.mockAuthentication", "true");
         QEsbRuntime.getInstance().stop();
         EsbConnectionManager.getInstance().closeAll();
         EsbStats.getInstance().reset();
         server.start();
         await(() -> QEsbRuntime.getInstance().getRunner("syncPerson.personEvents").getState() == EsbTriggerState.RUNNING);
         QContext.init(server.getQInstance(), new QSystemUserSession());

         new InsertAction().executeForRecord(new InsertInput("person").withRecord(new QRecord()
            .withValue("firstName", "Event")
            .withValue("lastName", "Sample")
            .withValue("email", "event.sample@example.invalid")));

         await(() -> EsbStats.getInstance().trigger("syncPerson.personEvents").succeeded() == 1);
         assertEquals(1, EsbStats.getInstance().destination("personEvents").published());
         assertEquals(1, EsbStats.getInstance().trigger("syncPerson.personEvents").consumed());
         assertEquals(0, EsbStats.getInstance().trigger("syncPerson.personEvents").failed());
      }
      finally
      {
         QContext.clear();
         server.stop();
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
