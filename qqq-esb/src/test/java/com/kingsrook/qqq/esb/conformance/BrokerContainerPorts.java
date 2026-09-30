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


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.testcontainers.containers.GenericContainer;


/*******************************************************************************
 * Docker can assign a new published port when the same container restarts.
 * ESB connection factories retain their original address, so restart tests
 * need stable host ports.  The socket check confirms the JVM can reach them.
 ******************************************************************************/
final class BrokerContainerPorts
{
   /** Utility class. */
   private BrokerContainerPorts()
   {
   }



   /** Pick two distinct unused ports before Docker binds them on this machine. */
   static int[] availablePair()
   {
      try(ServerSocket first = new ServerSocket(0); ServerSocket second = new ServerSocket(0))
      {
         return (new int[]{first.getLocalPort(), second.getLocalPort()});
      }
      catch(IOException e)
      {
         throw new IllegalStateException("Cannot allocate broker host ports", e);
      }
   }



   /** Confirm that the test JVM can reach every published broker port. */
   static void assertReachable(GenericContainer<?> broker, int... containerPorts)
   {
      for(int containerPort : containerPorts)
      {
         String host = broker.getHost();
         int hostPort = broker.getMappedPort(containerPort);
         try(Socket socket = new Socket())
         {
            socket.connect(new InetSocketAddress(host, hostPort), 2000);
         }
         catch(IOException e)
         {
            throw new IllegalStateException("Broker port " + host + ":" + hostPort
               + " is unreachable from the test JVM", e);
         }
      }
   }



   /** Wait until a real JMS connection succeeds, beyond TCP or HTTP readiness. */
   static void awaitJmsReady(ConnectionFactory factory, String brokerName) throws Exception
   {
      Instant deadline = Instant.now().plusSeconds(30);
      JMSException lastFailure = null;
      while(Instant.now().isBefore(deadline))
      {
         try(Connection connection = factory.createConnection())
         {
            connection.start();
            return;
         }
         catch(JMSException e)
         {
            lastFailure = e;
            Thread.sleep(200);
         }
      }
      throw new IllegalStateException(brokerName + " JMS is not ready on its mapped port", lastFailure);
   }
}
