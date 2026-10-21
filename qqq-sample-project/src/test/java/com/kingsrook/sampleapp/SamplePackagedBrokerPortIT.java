/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import jakarta.jms.Connection;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** A packaged child must bind and publish through its caller-selected broker.
 *******************************************************************************/
@ResourceLock("qqq.sample.esb.port")
class SamplePackagedBrokerPortIT
{
   private static final String BROKER_PORT_PROPERTY = "qqq.sample.esb.port";

   @TempDir
   Path directory;

   private String previousBrokerPort;



   /*******************************************************************************
    ** Save any per-run override supplied by the Maven caller.
    *******************************************************************************/
   @BeforeEach
   void setUp()
   {
      previousBrokerPort = System.getProperty(BROKER_PORT_PROPERTY);
   }



   /*******************************************************************************
    ** Restore the caller's property after successful and failed child assertions.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      if(previousBrokerPort == null)
      {
         System.clearProperty(BROKER_PORT_PROPERTY);
      }
      else
      {
         System.setProperty(BROKER_PORT_PROPERTY, previousBrokerPort);
      }
   }



   /*******************************************************************************
    ** The convenience overload forwards the port alongside its frontend options.
    *******************************************************************************/
   @Test
   void testDefaultOverloadForwardsBrokerPort() throws Exception
   {
      int port = freeBrokerPort();
      System.setProperty(BROKER_PORT_PROPERTY, Integer.toString(port));
      try(PackagedSampleServer server = PackagedSampleServer.start(SampleJavalinServer.class, directory, List.of()))
      {
         assertPublication(server, port, false);
         assertEquals(Integer.toString(port), System.getProperty(BROKER_PORT_PROPERTY));
      }
      assertPortClosed(port);
   }



   /*******************************************************************************
    ** An unrelated explicit JVM option remains effective with the inherited port.
    *******************************************************************************/
   @Test
   void testExplicitOptionsOverloadForwardsBrokerPort() throws Exception
   {
      int port = freeBrokerPort();
      System.setProperty(BROKER_PORT_PROPERTY, Integer.toString(port));
      List<String> options = List.of("-Dqqq.sample.sharing=true");
      try(PackagedSampleServer server = PackagedSampleServer.start(ConfigFileBasedSampleJavalinServer.class, directory, List.of(), options))
      {
         assertPublication(server, port, true);
         assertEquals(List.of("-Dqqq.sample.sharing=true"), options);
         assertEquals(Integer.toString(port), System.getProperty(BROKER_PORT_PROPERTY));
      }
      assertPortClosed(port);
   }



   /*******************************************************************************
    ** The child's explicit setting wins without changing the parent's property.
    *******************************************************************************/
   @Test
   void testExplicitChildBrokerPortOverridesInheritedPort() throws Exception
   {
      try(ServerSocket parentPort = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")))
      {
         int childPort = freeBrokerPort();
         System.setProperty(BROKER_PORT_PROPERTY, Integer.toString(parentPort.getLocalPort()));
         List<String> options = List.of("-D" + BROKER_PORT_PROPERTY + "=" + childPort, "-Dqqq.sample.sharing=true");
         try(PackagedSampleServer server = PackagedSampleServer.start(SampleJavalinServer.class, directory, List.of(), options))
         {
            assertPublication(server, childPort, true);
            assertEquals(Integer.toString(parentPort.getLocalPort()), System.getProperty(BROKER_PORT_PROPERTY));
            assertEquals(List.of("-D" + BROKER_PORT_PROPERTY + "=" + childPort, "-Dqqq.sample.sharing=true"), options);
         }
         assertPortClosed(childPort);
      }
   }



   /*******************************************************************************
    ** Subscribe before a real HTTP insert; the received event proves metadata uses
    ** the selected broker, rather than merely checking a JVM property or socket.
    *******************************************************************************/
   private void assertPublication(PackagedSampleServer server, int brokerPort, boolean sharing) throws Exception
   {
      URI base = server.awaitReady();
      try(ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(
         "tcp://127.0.0.1:" + brokerPort + "?connect-timeout-millis=2000&initialConnectAttempts=1&reconnectAttempts=0");
         Connection connection = factory.createConnection();
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
         MessageConsumer consumer = session.createConsumer(session.createTopic("personEvents"));
         HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
      {
         connection.start();
         HttpResponse<String> metadata = client.send(HttpRequest.newBuilder(base.resolve("/metaData"))
            .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
         assertEquals(200, metadata.statusCode(), metadata.body());
         assertEquals(sharing, new JSONObject(metadata.body()).getJSONObject("apps").has("sharing"));

         String email = "broker-port-" + UUID.randomUUID() + "@example.invalid";
         JSONObject person = new JSONObject().put("firstName", "Broker").put("lastName", "Port").put("email", email);
         HttpResponse<String> inserted = client.send(HttpRequest.newBuilder(base.resolve("/data/person"))
            .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(person.toString())).build(), HttpResponse.BodyHandlers.ofString());
         assertEquals(200, inserted.statusCode(), inserted.body());
         TextMessage message = assertInstanceOf(TextMessage.class, consumer.receive(5000), "The selected broker must receive the sample's table publication");
         JSONObject event = new JSONObject(message.getText());
         assertEquals("qqq.table.person.inserted", event.getString("type"));
         assertEquals("qqq://qqq-sample/table/person", event.getString("source"));
         assertEquals(email, event.getJSONObject("data").getJSONObject("record").getString("email"));
      }
   }



   /*******************************************************************************
    ** Two distinct reservations guarantee a nondefault port without selection retries.
    *******************************************************************************/
   private int freeBrokerPort() throws IOException
   {
      try(ServerSocket first = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
         ServerSocket second = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")))
      {
         return first.getLocalPort() == 61616 ? second.getLocalPort() : first.getLocalPort();
      }
   }



   /*******************************************************************************
    ** The helper must terminate its child and release the actual broker socket.
    *******************************************************************************/
   private void assertPortClosed(int port) throws IOException
   {
      try(Socket socket = new Socket())
      {
         assertThrows(IOException.class, () -> socket.connect(new InetSocketAddress("127.0.0.1", port), 500));
      }
   }
}
