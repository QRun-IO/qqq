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

package com.kingsrook.qqq.esb.management;


import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import org.apache.activemq.artemis.core.filter.Filter;
import org.apache.activemq.artemis.core.filter.impl.FilterImpl;
import org.apache.activemq.artemis.jms.client.ActiveMQMessage;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for ArtemisJolokiaAdapter - against a mock HTTP server that answers
 ** with canned responses in Jolokia's response format (the real broker is
 ** covered by the conformance suite).  Extends EsbTestBase for the test that
 ** checks the generated message filters against messages from the embedded
 ** broker.
 *******************************************************************************/
class ArtemisJolokiaAdapterTest extends EsbTestBase
{
   private static final String QUEUE_MBEAN = "org.apache.activemq.artemis:address=\"orders\",broker=\"0.0.0.0\",component=addresses,queue=\"orders\",routing-type=\"anycast\",subcomponent=queues";

   private static final String SEARCH_PATTERN = "org.apache.activemq.artemis:component=addresses,subcomponent=queues,queue=\"orders\",*";

   private static final String SEARCH_RESPONSE = """
      {"request":{"mbean":"org.apache.activemq.artemis:component=addresses,queue=\\"orders\\",subcomponent=queues,*","type":"search"},
       "value":["org.apache.activemq.artemis:address=\\"orders\\",broker=\\"0.0.0.0\\",component=addresses,queue=\\"orders\\",routing-type=\\"anycast\\",subcomponent=queues"],
       "timestamp":1758801600,"status":200}
      """;

   private static final String SEARCH_RESPONSE_NO_MATCH = """
      {"request":{"mbean":"org.apache.activemq.artemis:component=addresses,queue=\\"orders\\",subcomponent=queues,*","type":"search"},
       "value":[],"timestamp":1758801600,"status":200}
      """;

   private static final String READ_RESPONSE = """
      {"request":{"mbean":"org.apache.activemq.artemis:address=\\"orders\\",broker=\\"0.0.0.0\\",component=addresses,queue=\\"orders\\",routing-type=\\"anycast\\",subcomponent=queues",
                  "attribute":["MessageCount","ConsumerCount","Paused"],"type":"read"},
       "value":{"MessageCount":3,"ConsumerCount":1,"Paused":true},"timestamp":1758801600,"status":200}
      """;

   private static final String EXEC_ERROR_RESPONSE = """
      {"request":{"mbean":"org.apache.activemq.artemis:address=\\"orders\\",broker=\\"0.0.0.0\\",component=addresses,queue=\\"orders\\",routing-type=\\"anycast\\",subcomponent=queues",
                  "arguments":[],"type":"exec","operation":"removeAllMessages()"},
       "error_type":"javax.management.InstanceNotFoundException",
       "error":"javax.management.InstanceNotFoundException : org.apache.activemq.artemis:address=\\"orders\\",broker=\\"0.0.0.0\\",component=addresses,queue=\\"orders\\",routing-type=\\"anycast\\",subcomponent=queues",
       "status":404}
      """;

   private MockManagementServer server;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws IOException
   {
      server = new MockManagementServer();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      server.close();
      EsbConnectionManager.getInstance().closeAll();
   }



   /*******************************************************************************
    ** Artemis supports every management action in spec section 8.
    *******************************************************************************/
   @Test
   void testCapabilities()
   {
      EsbBrokerCapabilities capabilities = newAdapter().capabilities();
      assertThat(capabilities.browse()).isTrue();
      assertThat(capabilities.queueInfo()).isTrue();
      assertThat(capabilities.pauseQueue()).isTrue();
      assertThat(capabilities.purge()).isTrue();
      assertThat(capabilities.deleteSelected()).isTrue();
      assertThat(capabilities.deleteOlderThan()).isTrue();
      assertThat(capabilities.move()).isTrue();
   }



   /*******************************************************************************
    ** Queue info searches for the queue's MBean, then reads its attributes - both
    ** as authenticated JSON POSTs to Jolokia, with the console's Origin.
    *******************************************************************************/
   @Test
   void testGetQueueInfo() throws QException
   {
      server.withResponse(200, SEARCH_RESPONSE).withResponse(200, READ_RESPONSE);

      Optional<EsbQueueInfo> queueInfo = newAdapter().getQueueInfo("orders");
      assertThat(queueInfo).contains(new EsbQueueInfo(3L, 1, true));

      List<MockManagementServer.RecordedRequest> requests = server.getRequests();
      assertThat(requests).hasSize(2);

      MockManagementServer.RecordedRequest search = requests.get(0);
      assertThat(search.method()).isEqualTo("POST");
      assertThat(search.rawPath()).isEqualTo("/console/jolokia/search");
      assertThat(search.header("Authorization")).isEqualTo(basicAuth("admin", "secret"));
      assertThat(search.header("Content-Type")).startsWith("application/json");
      assertThat(search.header("Origin")).isEqualTo(server.getBaseUrl());
      JSONObject searchBody = new JSONObject(search.body());
      assertThat(searchBody.getString("type")).isEqualTo("search");
      assertThat(searchBody.getString("mbean")).isEqualTo(SEARCH_PATTERN);

      MockManagementServer.RecordedRequest read = requests.get(1);
      assertThat(read.method()).isEqualTo("POST");
      assertThat(read.rawPath()).isEqualTo("/console/jolokia/read");
      assertThat(read.header("Authorization")).isEqualTo(basicAuth("admin", "secret"));
      JSONObject readBody = new JSONObject(read.body());
      assertThat(readBody.getString("type")).isEqualTo("read");
      assertThat(readBody.getString("mbean")).isEqualTo(QUEUE_MBEAN);
      assertThat(readBody.getJSONArray("attribute").toList()).containsExactly("MessageCount", "ConsumerCount", "Paused");
   }



   /*******************************************************************************
    ** A queue with no MBean has no queue info (and nothing else is asked).
    *******************************************************************************/
   @Test
   void testGetQueueInfoForUnknownQueue() throws QException
   {
      server.withResponse(200, SEARCH_RESPONSE_NO_MATCH);
      assertThat(newAdapter().getQueueInfo("orders")).isEmpty();
      assertThat(server.getRequests()).hasSize(1);
   }



   /*******************************************************************************
    ** Pause, resume, and purge exec the QueueControl operations on the queue's
    ** MBean.
    *******************************************************************************/
   @Test
   void testPauseResumeAndPurge() throws QException
   {
      ArtemisJolokiaAdapter adapter = newAdapter();

      server.withResponse(200, SEARCH_RESPONSE).withResponse(200, execResponse("pause()", "null"));
      adapter.pauseQueue("orders");
      assertExec(server.getRequests().get(1), "pause()");

      server.withResponse(200, SEARCH_RESPONSE).withResponse(200, execResponse("resume()", "null"));
      adapter.resumeQueue("orders");
      assertExec(server.getRequests().get(3), "resume()");

      server.withResponse(200, SEARCH_RESPONSE).withResponse(200, execResponse("removeAllMessages()", "3"));
      assertThat(adapter.purgeQueue("orders")).isEqualTo(3L);
      assertExec(server.getRequests().get(5), "removeAllMessages()");

      /////////////////////////////////////////////////////////
      // each action first finds the queue's MBean by search //
      /////////////////////////////////////////////////////////
      for(Integer searchIndex : List.of(0, 2, 4))
      {
         JSONObject searchBody = new JSONObject(server.getRequests().get(searchIndex).body());
         assertThat(searchBody.getString("type")).isEqualTo("search");
         assertThat(searchBody.getString("mbean")).isEqualTo(SEARCH_PATTERN);
      }
   }



   /*******************************************************************************
    ** Deleting selected messages removes them by a JMS message id filter.
    *******************************************************************************/
   @Test
   void testDeleteMessages() throws QException
   {
      server.withResponse(200, SEARCH_RESPONSE).withResponse(200, execResponse("removeMessages(java.lang.String)", "2"));

      assertThat(newAdapter().deleteMessages("orders", List.of("ID:aaa", "ID:bbb"))).isEqualTo(2);
      assertExec(server.getRequests().get(1), "removeMessages(java.lang.String)", "AMQUserID IN ('ID:aaa', 'ID:bbb')");
   }



   /*******************************************************************************
    ** Message ids go into the filter as quoted literals, so an id can't widen it
    ** (e.g., to every message in the queue).
    *******************************************************************************/
   @Test
   void testDeleteMessagesQuotesIds() throws QException
   {
      server.withResponse(200, SEARCH_RESPONSE).withResponse(200, execResponse("removeMessages(java.lang.String)", "0"));

      newAdapter().deleteMessages("orders", List.of("x' OR '1'='1"));
      assertExec(server.getRequests().get(1), "removeMessages(java.lang.String)", "AMQUserID IN ('x'' OR ''1''=''1')");
   }



   /*******************************************************************************
    ** With no (non-blank) ids, nothing is deleted, and Jolokia isn't called - an
    ** empty filter would remove every message.
    *******************************************************************************/
   @Test
   void testDeleteMessagesWithNoIds() throws QException
   {
      ArtemisJolokiaAdapter adapter = newAdapter();
      assertThat(adapter.deleteMessages("orders", List.of())).isZero();
      assertThat(adapter.deleteMessages("orders", null)).isZero();
      assertThat(adapter.deleteMessages("orders", Arrays.asList(" ", null))).isZero();
      assertThat(adapter.moveMessages("orders", List.of(), "orders.dlq")).isZero();
      assertThat(server.getRequests()).isEmpty();
   }



   /*******************************************************************************
    ** Deleting messages older than a time removes them by a timestamp filter.
    *******************************************************************************/
   @Test
   void testDeleteMessagesOlderThan() throws QException
   {
      server.withResponse(200, SEARCH_RESPONSE).withResponse(200, execResponse("removeMessages(java.lang.String)", "5"));

      assertThat(newAdapter().deleteMessagesOlderThan("orders", Instant.parse("2026-09-25T12:00:00Z"))).isEqualTo(5);
      assertExec(server.getRequests().get(1), "removeMessages(java.lang.String)", "AMQTimestamp < 1790337600000");

      assertThatThrownBy(() -> newAdapter().deleteMessagesOlderThan("orders", null)).isInstanceOf(QException.class);
   }



   /*******************************************************************************
    ** Moving messages moves them by id filter to the other queue.
    *******************************************************************************/
   @Test
   void testMoveMessages() throws QException
   {
      server.withResponse(200, SEARCH_RESPONSE).withResponse(200, execResponse("moveMessages(java.lang.String,java.lang.String)", "1"));

      assertThat(newAdapter().moveMessages("orders", List.of("ID:aaa"), "orders.dlq")).isEqualTo(1);
      assertExec(server.getRequests().get(1), "moveMessages(java.lang.String,java.lang.String)", "AMQUserID IN ('ID:aaa')", "orders.dlq");

      assertThatThrownBy(() -> newAdapter().moveMessages("orders", List.of("ID:aaa"), " "))
         .isInstanceOf(QException.class)
         .hasMessageContaining("queue to move");
   }



   /*******************************************************************************
    ** An action on a queue the broker doesn't have fails, without an exec.
    *******************************************************************************/
   @Test
   void testActionOnUnknownQueueThrows()
   {
      server.withResponse(200, SEARCH_RESPONSE_NO_MATCH);

      assertThatThrownBy(() -> newAdapter().purgeQueue("orders"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("orders")
         .hasMessageContaining("not found");
      assertThat(server.getRequests()).hasSize(1);
   }



   /*******************************************************************************
    ** A queue name that matches more than one MBean (e.g., two brokers in one
    ** JVM) fails, rather than act on either.
    *******************************************************************************/
   @Test
   void testAmbiguousQueueThrows()
   {
      server.withResponse(200, """
         {"request":{"type":"search"},
          "value":["org.apache.activemq.artemis:address=\\"orders\\",broker=\\"a\\",component=addresses,queue=\\"orders\\",routing-type=\\"anycast\\",subcomponent=queues",
                   "org.apache.activemq.artemis:address=\\"orders\\",broker=\\"b\\",component=addresses,queue=\\"orders\\",routing-type=\\"anycast\\",subcomponent=queues"],
          "timestamp":1758801600,"status":200}
         """);

      assertThatThrownBy(() -> newAdapter().pauseQueue("orders"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("more than one");
      assertThat(server.getRequests()).hasSize(1);
   }



   /*******************************************************************************
    ** A Jolokia error (reported in the response body) fails with its error text.
    *******************************************************************************/
   @Test
   void testJolokiaErrorThrows()
   {
      server.withResponse(200, SEARCH_RESPONSE).withResponse(200, EXEC_ERROR_RESPONSE);

      assertThatThrownBy(() -> newAdapter().purgeQueue("orders"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("InstanceNotFoundException")
         .hasMessageContaining("404");
   }



   /*******************************************************************************
    ** An HTTP error (e.g., bad credentials, or an Origin the console doesn't
    ** allow) fails with its status.
    *******************************************************************************/
   @Test
   void testHttpErrorThrows()
   {
      server.withResponse(401, "");

      assertThatThrownBy(() -> newAdapter().getQueueInfo("orders"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("401");
   }



   /*******************************************************************************
    ** A response that isn't JSON fails clearly.
    *******************************************************************************/
   @Test
   void testNonJsonResponseThrows()
   {
      server.withResponse(200, "<html>login</html>");

      assertThatThrownBy(() -> newAdapter().getQueueInfo("orders"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("not JSON");
   }



   /*******************************************************************************
    ** An unreachable management API fails (within the connect timeout).
    *******************************************************************************/
   @Test
   void testUnreachableThrows() throws IOException
   {
      ArtemisJolokiaAdapter adapter = new ArtemisJolokiaAdapter(new QEsbProviderMetaData()
         .withName("artemisManaged")
         .withType(EsbProviderType.ACTIVEMQ_ARTEMIS)
         .withUrl(getBrokerUrl())
         .withManagementUrl("http://127.0.0.1:" + findClosedPort()));

      assertThatThrownBy(() -> adapter.getQueueInfo("orders"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("artemisManaged");
   }



   /*******************************************************************************
    ** Without management credentials, no Authorization header is sent; a
    ** trailing slash on the management URL is ignored.
    *******************************************************************************/
   @Test
   void testNoCredentialsAndTrailingSlash() throws QException
   {
      server.withResponse(200, SEARCH_RESPONSE_NO_MATCH);

      ArtemisJolokiaAdapter adapter = new ArtemisJolokiaAdapter(new QEsbProviderMetaData()
         .withName("artemisManaged")
         .withType(EsbProviderType.ACTIVEMQ_ARTEMIS)
         .withUrl(getBrokerUrl())
         .withManagementUrl(server.getBaseUrl() + "/"));

      assertThat(adapter.getQueueInfo("orders")).isEmpty();
      assertThat(server.getRequests().get(0).rawPath()).isEqualTo("/console/jolokia/search");
      assertThat(server.getRequests().get(0).header("Authorization")).isNull();
   }



   /*******************************************************************************
    ** Special characters in a queue name are quoted in the MBean search pattern
    ** (as Artemis quotes them in its MBean names) - e.g., the backslash-escaped
    ** dots in an Artemis subscription queue's name.
    *******************************************************************************/
   @Test
   void testQueueNameIsQuotedInSearch() throws QException
   {
      server.withResponse(200, SEARCH_RESPONSE_NO_MATCH);

      newAdapter().getQueueInfo("syncOrder\\.orderEvents*");
      assertThat(new JSONObject(server.getRequests().get(0).body()).getString("mbean"))
         .isEqualTo("org.apache.activemq.artemis:component=addresses,subcomponent=queues,queue=\"syncOrder\\\\.orderEvents\\*\",*");
   }



   /*******************************************************************************
    ** A fully qualified queue name (ADDRESS::QUEUE, as EsbBrokerNames gives for
    ** an Artemis subscription queue) searches by address and queue; a move
    ** target's address is dropped (QueueControl moves to a queue by its name).
    *******************************************************************************/
   @Test
   void testFullyQualifiedQueueNames() throws QException
   {
      server.withResponse(200, SEARCH_RESPONSE).withResponse(200, execResponse("moveMessages(java.lang.String,java.lang.String)", "1"));

      newAdapter().moveMessages("orderEvents::syncOrder\\.orderEvents", List.of("ID:aaa"), "otherEvents::other\\.queue");

      assertThat(new JSONObject(server.getRequests().get(0).body()).getString("mbean"))
         .isEqualTo("org.apache.activemq.artemis:component=addresses,address=\"orderEvents\",subcomponent=queues,queue=\"syncOrder\\\\.orderEvents\",*");
      assertExec(server.getRequests().get(1), "moveMessages(java.lang.String,java.lang.String)", "AMQUserID IN ('ID:aaa')", "other\\.queue");
   }



   /*******************************************************************************
    ** The message-id and older-than filters select exactly the intended messages,
    ** by Artemis's own filter implementation, on messages sent through the
    ** embedded broker (whose JMS ids and timestamps are what browsing shows).
    *******************************************************************************/
   @Test
   void testFiltersSelectTheIntendedArtemisMessages() throws Exception
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();
      Session              session = manager.openSession(PROVIDER_NAME, false);
      Queue                queue   = manager.resolveQueue(session, PROVIDER_NAME, "esb.test.jolokiaFilters");

      MessageProducer producer = session.createProducer(queue);
      for(int i = 0; i < 3; i++)
      {
         producer.send(session.createTextMessage("message " + i));
         SleepUtils.sleep(5, TimeUnit.MILLISECONDS);
      }

      List<ActiveMQMessage> messages = new ArrayList<>();
      try(QueueBrowser browser = session.createBrowser(queue))
      {
         Enumeration<?> enumeration = browser.getEnumeration();
         for(Object message : Collections.list(enumeration))
         {
            messages.add((ActiveMQMessage) message);
         }
      }
      assertThat(messages).hasSize(3);

      Filter idFilter = FilterImpl.createFilter(ArtemisJolokiaAdapter.messageIdFilter(List.of(messages.get(0).getJMSMessageID(), messages.get(2).getJMSMessageID())));
      assertThat(idFilter.match(messages.get(0).getCoreMessage())).isTrue();
      assertThat(idFilter.match(messages.get(1).getCoreMessage())).isFalse();
      assertThat(idFilter.match(messages.get(2).getCoreMessage())).isTrue();

      Filter olderThanFilter = FilterImpl.createFilter(ArtemisJolokiaAdapter.olderThanFilter(Instant.ofEpochMilli(messages.get(1).getJMSTimestamp())));
      assertThat(olderThanFilter.match(messages.get(0).getCoreMessage())).isTrue();
      assertThat(olderThanFilter.match(messages.get(1).getCoreMessage())).isFalse();
      assertThat(olderThanFilter.match(messages.get(2).getCoreMessage())).isFalse();
   }



   /*******************************************************************************
    ** An adapter for the mock server, with management credentials.
    *******************************************************************************/
   private ArtemisJolokiaAdapter newAdapter()
   {
      return (new ArtemisJolokiaAdapter(new QEsbProviderMetaData()
         .withName("artemisManaged")
         .withType(EsbProviderType.ACTIVEMQ_ARTEMIS)
         .withUrl(getBrokerUrl())
         .withManagementUrl(server.getBaseUrl())
         .withManagementUsername("admin")
         .withManagementPassword("secret")));
   }



   /*******************************************************************************
    ** Assert a request is a Jolokia exec of an operation (with arguments) on the
    ** queue's MBean.
    *******************************************************************************/
   private static void assertExec(MockManagementServer.RecordedRequest request, String operation, Object... arguments)
   {
      assertThat(request.method()).isEqualTo("POST");
      assertThat(request.rawPath()).isEqualTo("/console/jolokia/exec");
      assertThat(request.header("Authorization")).isEqualTo(basicAuth("admin", "secret"));

      JSONObject body = new JSONObject(request.body());
      assertThat(body.getString("type")).isEqualTo("exec");
      assertThat(body.getString("mbean")).isEqualTo(QUEUE_MBEAN);
      assertThat(body.getString("operation")).isEqualTo(operation);
      assertThat(body.getJSONArray("arguments").toList()).containsExactly(arguments);
   }



   /*******************************************************************************
    ** A Jolokia exec response, with a (JSON) return value.
    *******************************************************************************/
   private static String execResponse(String operation, String jsonValue)
   {
      return ("{\"request\":{\"mbean\":" + JSONObject.quote(QUEUE_MBEAN) + ",\"arguments\":[],\"type\":\"exec\",\"operation\":" + JSONObject.quote(operation) + "},"
         + "\"value\":" + jsonValue + ",\"timestamp\":1758801600,\"status\":200}");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String basicAuth(String username, String password)
   {
      return ("Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
   }



   /*******************************************************************************
    ** A localhost port with nothing listening on it.
    *******************************************************************************/
   static int findClosedPort() throws IOException
   {
      try(ServerSocket serverSocket = new ServerSocket(0))
      {
         return (serverSocket.getLocalPort());
      }
   }

}
