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

package com.kingsrook.qqq.esb.conformance;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
}
