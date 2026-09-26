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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for RabbitManagementAdapter - against a mock HTTP server that
 ** answers with canned responses in the RabbitMQ management API's format (the
 ** real broker is covered by the conformance suite).
 *******************************************************************************/
class RabbitManagementAdapterTest
{
   private static final String QUEUE_RESPONSE = """
      {"arguments":{"x-queue-type":"quorum"},"auto_delete":false,"consumers":2,"durable":true,"exclusive":false,
       "leader":"rabbit@broker","members":["rabbit@broker"],"messages":7,"messages_ready":6,"messages_unacknowledged":1,
       "name":"orders","node":"rabbit@broker","online":["rabbit@broker"],"state":"running","type":"quorum","vhost":"/"}
      """;

   private static final String NOT_FOUND_RESPONSE = """
      {"error":"Object Not Found","reason":"Not Found"}
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
   }



   /*******************************************************************************
    ** RabbitMQ supports browse, queue info, and purge only (spec section 8).
    *******************************************************************************/
   @Test
   void testCapabilities()
   {
      EsbBrokerCapabilities capabilities = newAdapter("amqp://localhost:5672").capabilities();
      assertThat(capabilities.browse()).isTrue();
      assertThat(capabilities.queueInfo()).isTrue();
      assertThat(capabilities.purge()).isTrue();
      assertThat(capabilities.pauseQueue()).isFalse();
      assertThat(capabilities.deleteSelected()).isFalse();
      assertThat(capabilities.deleteOlderThan()).isFalse();
      assertThat(capabilities.move()).isFalse();
   }



   /*******************************************************************************
    ** Queue info is an authenticated GET of the queue, in the default vhost for
    ** a url with no path.
    *******************************************************************************/
   @Test
   void testGetQueueInfo() throws QException
   {
      server.withResponse(200, QUEUE_RESPONSE);

      assertThat(newAdapter("amqp://localhost:5672").getQueueInfo("orders")).contains(new EsbQueueInfo(7L, 2, false));

      MockManagementServer.RecordedRequest request = server.getRequests().get(0);
      assertThat(request.method()).isEqualTo("GET");
      assertThat(request.rawPath()).isEqualTo("/api/queues/%2F/orders");
      assertThat(request.header("Authorization")).isEqualTo(basicAuth("guest", "guestPassword"));
   }



   /*******************************************************************************
    ** A queue the broker doesn't have (404) has no queue info.
    *******************************************************************************/
   @Test
   void testGetQueueInfoForUnknownQueue() throws QException
   {
      server.withResponse(404, NOT_FOUND_RESPONSE);
      assertThat(newAdapter("amqp://localhost:5672").getQueueInfo("orders")).isEmpty();
   }



   /*******************************************************************************
    ** The vhost comes from the provider's AMQP URI (decoded, then re-encoded as a
    ** path segment, as is the queue name); "/" and no path both mean the default
    ** vhost.
    *******************************************************************************/
   @Test
   void testVhostAndQueueNameAreEncoded() throws QException
   {
      for(int i = 0; i < 4; i++)
      {
         server.withResponse(404, NOT_FOUND_RESPONSE);
      }

      newAdapter("amqp://guest:guest@localhost:5672/prod%2Feu").getQueueInfo("a b/c");
      newAdapter("amqps://localhost:5671/orders").getQueueInfo("syncOrder.orderEvents");
      newAdapter("amqp://localhost:5672/").getQueueInfo("orders");
      newAdapter("amqp://localhost").getQueueInfo("orders");

      assertThat(server.getRequests()).extracting(MockManagementServer.RecordedRequest::rawPath).containsExactly(
         "/api/queues/prod%2Feu/a%20b%2Fc",
         "/api/queues/orders/syncOrder.orderEvents",
         "/api/queues/%2F/orders",
         "/api/queues/%2F/orders");
   }



   /*******************************************************************************
    ** A provider url that isn't a URI fails clearly.
    *******************************************************************************/
   @Test
   void testBadProviderUrlThrows()
   {
      assertThatThrownBy(() -> newAdapter("amqp://local host:5672").getQueueInfo("orders"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("vhost");
      assertThat(server.getRequests()).isEmpty();
   }



   /*******************************************************************************
    ** Purge deletes the queue's contents, and returns how many messages the
    ** queue held just before (the API doesn't say how many it purged).
    *******************************************************************************/
   @Test
   void testPurgeQueue() throws QException
   {
      server.withResponse(200, QUEUE_RESPONSE).withResponse(204, null);

      assertThat(newAdapter("amqp://localhost:5672").purgeQueue("orders")).isEqualTo(7L);

      List<MockManagementServer.RecordedRequest> requests = server.getRequests();
      assertThat(requests).hasSize(2);
      assertThat(requests.get(1).method()).isEqualTo("DELETE");
      assertThat(requests.get(1).rawPath()).isEqualTo("/api/queues/%2F/orders/contents");
      assertThat(requests.get(1).header("Authorization")).isEqualTo(basicAuth("guest", "guestPassword"));
   }



   /*******************************************************************************
    ** Purging a queue the broker doesn't have fails, without a DELETE.
    *******************************************************************************/
   @Test
   void testPurgeUnknownQueueThrows()
   {
      server.withResponse(404, NOT_FOUND_RESPONSE);

      assertThatThrownBy(() -> newAdapter("amqp://localhost:5672").purgeQueue("orders"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("orders")
         .hasMessageContaining("not found");
      assertThat(server.getRequests()).hasSize(1);
   }



   /*******************************************************************************
    ** A failed DELETE fails the purge.
    *******************************************************************************/
   @Test
   void testPurgeHttpErrorThrows()
   {
      server.withResponse(200, QUEUE_RESPONSE).withResponse(403, "{\"error\":\"access_refused\",\"reason\":\"Access refused.\"}");

      assertThatThrownBy(() -> newAdapter("amqp://localhost:5672").purgeQueue("orders"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("403")
         .hasMessageContaining("Access refused");
   }



   /*******************************************************************************
    ** Actions RabbitMQ doesn't support throw UnsupportedOperationException,
    ** without calling the API.
    *******************************************************************************/
   @Test
   void testUnsupportedOperationsThrow()
   {
      RabbitManagementAdapter adapter = newAdapter("amqp://localhost:5672");

      assertThatThrownBy(() -> adapter.pauseQueue("orders")).isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> adapter.resumeQueue("orders")).isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> adapter.deleteMessages("orders", List.of("ID:aaa"))).isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> adapter.deleteMessagesOlderThan("orders", Instant.now())).isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> adapter.moveMessages("orders", List.of("ID:aaa"), "orders.dlq"))
         .isInstanceOf(UnsupportedOperationException.class)
         .hasMessageContaining("RabbitMQ");
      assertThat(server.getRequests()).isEmpty();
   }



   /*******************************************************************************
    ** An HTTP error fails with its status and the API's reason.
    *******************************************************************************/
   @Test
   void testHttpErrorThrows()
   {
      server.withResponse(401, "{\"error\":\"not_authorized\",\"reason\":\"Login failed\"}");

      assertThatThrownBy(() -> newAdapter("amqp://localhost:5672").getQueueInfo("orders"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("401")
         .hasMessageContaining("Login failed");
   }



   /*******************************************************************************
    ** A trailing slash on the management URL is ignored.
    *******************************************************************************/
   @Test
   void testManagementUrlTrailingSlash() throws QException
   {
      server.withResponse(200, QUEUE_RESPONSE);

      RabbitManagementAdapter adapter = new RabbitManagementAdapter(new QEsbProviderMetaData()
         .withName("rabbit")
         .withType(EsbProviderType.RABBITMQ)
         .withUrl("amqp://localhost:5672")
         .withManagementUrl(server.getBaseUrl() + "/"));

      assertThat(adapter.getQueueInfo("orders")).isPresent();
      assertThat(server.getRequests().get(0).rawPath()).isEqualTo("/api/queues/%2F/orders");
      assertThat(server.getRequests().get(0).header("Authorization")).isNull();
   }



   /*******************************************************************************
    ** An unreachable management API fails.
    *******************************************************************************/
   @Test
   void testUnreachableThrows() throws IOException
   {
      RabbitManagementAdapter adapter = new RabbitManagementAdapter(new QEsbProviderMetaData()
         .withName("rabbit")
         .withType(EsbProviderType.RABBITMQ)
         .withUrl("amqp://localhost:5672")
         .withManagementUrl("http://127.0.0.1:" + ArtemisJolokiaAdapterTest.findClosedPort()));

      assertThatThrownBy(() -> adapter.getQueueInfo("orders"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("rabbit");
   }



   /*******************************************************************************
    ** An adapter for the mock server, with management credentials.
    *******************************************************************************/
   private RabbitManagementAdapter newAdapter(String amqpUrl)
   {
      return (new RabbitManagementAdapter(new QEsbProviderMetaData()
         .withName("rabbit")
         .withType(EsbProviderType.RABBITMQ)
         .withUrl(amqpUrl)
         .withManagementUrl(server.getBaseUrl())
         .withManagementUsername("guest")
         .withManagementPassword("guestPassword")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String basicAuth(String username, String password)
   {
      return ("Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
   }

}
