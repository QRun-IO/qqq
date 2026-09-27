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

package com.kingsrook.qqq.esb.api;


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.management.MockManagementServer;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessEvent;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessPublication;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import com.kingsrook.qqq.esb.runtime.QEsbRuntime;
import com.kingsrook.qqq.esb.stats.EsbStats;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 * Unit test for EsbRouteProvider (and EsbStatusBuilder): each endpoint's JSON
 * is exactly the endpoint contract, 404 without ESB meta-data, and broker data
 * comes from the management API only when the provider has a managementUrl.
 *******************************************************************************/
class EsbRouteProviderTest extends EsbApiTestBase
{
   private static final List<String> COUNTER_KEYS       = List.of("published", "publishFailures", "consumed", "succeeded", "failed", "retried", "deadLettered", "inFlight", "lastActivity", "avgMs", "maxMs", "lastError");
   private static final List<String> DESTINATION_KEYS   = List.of("name", "type", "provider", "brokerName", "counters", "queueInfo", "capabilities");
   private static final List<String> CAPABILITY_KEYS    = List.of("browse", "pauseQueue", "purge", "deleteSelected", "deleteOlderThan", "move");
   private static final List<String> TRIGGER_KEYS       = List.of("name", "processName", "processLabel", "destination", "mode", "concurrency", "maxAttempts", "state", "counters", "deadLetter", "subscription");
   private static final List<String> MESSAGE_KEYS       = List.of("messageId", "timestamp", "deliveryCount", "event", "rawBody", "properties");
   private static final List<String> PERMISSION_KEYS    = List.of("canOperate", "canDelete");
   private static final String       RABBIT_PROVIDER    = "rabbit";
   private static final String       RABBIT_QUEUE       = "rabbitQueue";
   private static final String       RABBIT_PROCESS     = "rabbitProcess";
   private static final String       RABBIT_URL         = "amqp://esbUser:esbSecret@localhost:5672/%2f";



   /*******************************************************************************
    ** GET /table/{table}: its publications (with each destination), the
    ** triggers subscribed to those destinations, and permissions - with
    ** counters from EsbStats, and no broker data without a managementUrl.
    *******************************************************************************/
   @Test
   void testTableEndpointMatchesContract() throws Exception
   {
      EsbStats.getInstance().published(DESTINATION_ORDER_EVENTS);
      EsbStats.getInstance().publishFailed(DESTINATION_ORDER_EVENTS, new RuntimeException("broker down"));
      EsbStats.getInstance().consumed(TRIGGER_SYNC_ORDER);
      EsbStats.getInstance().succeeded(TRIGGER_SYNC_ORDER, 40);

      JSONObject body = getJson("/qqq/v1/esb/table/" + TABLE_NAME_ORDER);
      assertThat(body.keySet()).containsExactlyInAnyOrder("table", "publications", "subscribers", "permissions");
      assertEquals(TABLE_NAME_ORDER, body.getString("table"));
      assertPermissions(body.getJSONObject("permissions"), false, false);

      JSONArray publications = body.getJSONArray("publications");
      assertEquals(2, publications.length());
      assertThat(publications.getJSONObject(0).keySet()).containsExactlyInAnyOrder("destination", "events");
      assertEquals(List.of("INSERT", "UPDATE"), publications.getJSONObject(0).getJSONArray("events").toList());
      assertEquals(List.of("INSERT"), publications.getJSONObject(1).getJSONArray("events").toList());

      JSONObject orderEvents = publications.getJSONObject(0).getJSONObject("destination");
      assertDestination(orderEvents, DESTINATION_ORDER_EVENTS, "TOPIC", DESTINATION_ORDER_EVENTS);
      assertTrue(orderEvents.isNull("queueInfo"));
      JSONObject destinationCounters = orderEvents.getJSONObject("counters");
      assertEquals(1, destinationCounters.getInt("published"));
      assertEquals(1, destinationCounters.getInt("publishFailures"));
      assertEquals("An ESB operation failed.", destinationCounters.getString("lastError"));
      assertThat(Instant.parse(destinationCounters.getString("lastActivity"))).isNotNull();

      assertDestination(publications.getJSONObject(1).getJSONObject("destination"), DESTINATION_FULFILLMENT, "QUEUE", BROKER_NAME_FULFILLMENT);

      JSONObject capabilities = orderEvents.getJSONObject("capabilities");
      assertTrue(capabilities.getBoolean("browse"));
      for(String capability : List.of("pauseQueue", "purge", "deleteSelected", "deleteOlderThan", "move"))
      {
         assertFalse(capabilities.getBoolean(capability), capability);
      }

      JSONArray subscribers = body.getJSONArray("subscribers");
      assertEquals(2, subscribers.length());

      JSONObject syncOrder = subscribers.getJSONObject(0);
      assertThat(syncOrder.keySet()).containsExactlyInAnyOrderElementsOf(TRIGGER_KEYS);
      assertEquals(TRIGGER_SYNC_ORDER, syncOrder.getString("name"));
      assertEquals(PROCESS_NAME_SYNC_ORDER, syncOrder.getString("processName"));
      assertEquals("Sync Order", syncOrder.getString("processLabel"));
      assertEquals("SINGLE", syncOrder.getString("mode"));
      assertEquals(1, syncOrder.getInt("concurrency"));
      assertEquals(3, syncOrder.getInt("maxAttempts"));
      assertEquals("STOPPED", syncOrder.getString("state"));
      assertDestination(syncOrder.getJSONObject("destination"), DESTINATION_ORDER_EVENTS, "TOPIC", DESTINATION_ORDER_EVENTS);
      assertEquals(1, syncOrder.getJSONObject("counters").getInt("consumed"));
      assertEquals(1, syncOrder.getJSONObject("counters").getInt("succeeded"));
      assertEquals(40, syncOrder.getJSONObject("counters").getInt("avgMs"));

      JSONObject deadLetter = syncOrder.getJSONObject("deadLetter");
      assertThat(deadLetter.keySet()).containsExactlyInAnyOrder("brokerName", "messageCount");
      assertEquals(DEAD_LETTERS_SYNC_ORDER, deadLetter.getString("brokerName"));
      assertTrue(deadLetter.isNull("messageCount"));

      JSONObject subscription = syncOrder.getJSONObject("subscription");
      assertThat(subscription.keySet()).containsExactlyInAnyOrder("brokerName", "messageCount");
      assertEquals("orderEvents::syncOrder\\.orderEvents", subscription.getString("brokerName"));
      assertTrue(subscription.isNull("messageCount"));

      JSONObject fulfillOrder = subscribers.getJSONObject(1);
      assertEquals(TRIGGER_FULFILL_ORDER, fulfillOrder.getString("name"));
      assertTrue(fulfillOrder.isNull("subscription"));
   }



   /*******************************************************************************
    ** Counters must not send a broker URI's embedded credentials to clients.
    *******************************************************************************/
   @Test
   void testCounterErrorDoesNotExposeBrokerCredentials() throws Exception
   {
      String error = "Could not connect to amqp://esbUser:esbSecret@broker.example/";
      EsbStats.getInstance().publishFailed(DESTINATION_ORDER_EVENTS, new RuntimeException(error));

      String body = get("/qqq/v1/esb/table/" + TABLE_NAME_ORDER).body();
      assertThat(body).doesNotContain("esbUser", "esbSecret", "broker.example");
      assertEquals("An ESB operation failed.", new JSONObject(body).getJSONArray("publications").getJSONObject(0)
         .getJSONObject("destination").getJSONObject("counters").getString("lastError"));
      assertEquals(error, EsbStats.getInstance().destination(DESTINATION_ORDER_EVENTS).lastError());
   }



   /*******************************************************************************
    ** GET /process/{process}: its publications and triggers.
    *******************************************************************************/
   @Test
   void testProcessEndpointMatchesContract() throws Exception
   {
      JSONObject body = getJson("/qqq/v1/esb/process/" + PROCESS_NAME_FULFILL_ORDER);
      assertThat(body.keySet()).containsExactlyInAnyOrder("process", "publications", "triggers", "permissions");
      assertEquals(PROCESS_NAME_FULFILL_ORDER, body.getString("process"));
      assertPermissions(body.getJSONObject("permissions"), false, false);

      JSONArray publications = body.getJSONArray("publications");
      assertEquals(1, publications.length());
      assertEquals(List.of("COMPLETED"), publications.getJSONObject(0).getJSONArray("events").toList());
      assertDestination(publications.getJSONObject(0).getJSONObject("destination"), DESTINATION_ORDER_EVENTS, "TOPIC", DESTINATION_ORDER_EVENTS);

      JSONArray triggers = body.getJSONArray("triggers");
      assertEquals(1, triggers.length());
      JSONObject trigger = triggers.getJSONObject(0);
      assertThat(trigger.keySet()).containsExactlyInAnyOrderElementsOf(TRIGGER_KEYS);
      assertEquals(TRIGGER_FULFILL_ORDER, trigger.getString("name"));
      assertEquals("Fulfill Order", trigger.getString("processLabel"));
      assertEquals(2, trigger.getInt("concurrency"));
      assertEquals(5, trigger.getInt("maxAttempts"));
      assertDestination(trigger.getJSONObject("destination"), DESTINATION_FULFILLMENT, "QUEUE", BROKER_NAME_FULFILLMENT);
      assertEquals(DEAD_LETTERS_FULFILL_ORDER, trigger.getJSONObject("deadLetter").getString("brokerName"));
      assertTrue(trigger.isNull("subscription"));
   }



   /*******************************************************************************
    ** A trigger's state comes from the runtime's runner for it.
    *******************************************************************************/
   @Test
   void testTriggerStateFromRuntime() throws Exception
   {
      QEsbRuntime runtime = new QEsbRuntime();
      try
      {
         EsbProcessMetaData.of(qInstance.getProcess(PROCESS_NAME_FULFILL_ORDER)).getTriggers().get(0).setStartPaused(true);
         runtime.start(qInstance);

         EsbStatusBuilder builder = new EsbStatusBuilder().withRuntime(runtime);
         Map<String, Object> body = builder.buildProcess(PROCESS_NAME_FULFILL_ORDER);
         @SuppressWarnings("unchecked")
         Map<String, Object> trigger = ((List<Map<String, Object>>) body.get("triggers")).get(0);
         assertEquals("PAUSED", trigger.get("state"));
      }
      finally
      {
         runtime.stop();
      }
   }



   /*******************************************************************************
    ** GET /overview: providers (without urls or credentials), and every
    ** destination with its publishers and triggers.
    *******************************************************************************/
   @Test
   void testOverviewEndpointMatchesContract() throws Exception
   {
      addRabbitProvider(qInstance, "http://localhost:1");
      setPermissions("esbView.hasAccess");

      String body = get("/qqq/v1/esb/overview").body();
      assertThat(body).doesNotContain("esbSecret", "esbUser", "mgmtSecret", "localhost");

      JSONObject overview = new JSONObject(body);
      assertThat(overview.keySet()).containsExactlyInAnyOrder("providers", "destinations", "permissions");
      assertPermissions(overview.getJSONObject("permissions"), false, false);

      JSONArray providers = overview.getJSONArray("providers");
      assertEquals(2, providers.length());
      JSONObject artemis = providers.getJSONObject(0);
      assertThat(artemis.keySet()).containsExactlyInAnyOrder("name", "type", "connected", "managementEnabled");
      assertEquals(PROVIDER_NAME, artemis.getString("name"));
      assertEquals("ACTIVEMQ_ARTEMIS", artemis.getString("type"));
      assertFalse(artemis.getBoolean("managementEnabled"));
      JSONObject rabbit = providers.getJSONObject(1);
      assertEquals("RABBITMQ", rabbit.getString("type"));
      assertFalse(rabbit.getBoolean("connected"));
      assertTrue(rabbit.getBoolean("managementEnabled"));

      JSONArray destinations = overview.getJSONArray("destinations");
      assertEquals(3, destinations.length());

      JSONObject orderEvents = destinations.getJSONObject(0);
      assertThat(orderEvents.keySet()).containsExactlyInAnyOrderElementsOf(concat(DESTINATION_KEYS, List.of("publishers", "triggers")));
      assertEquals(DESTINATION_ORDER_EVENTS, orderEvents.getString("name"));

      JSONArray publishers = orderEvents.getJSONArray("publishers");
      assertEquals(2, publishers.length());
      assertThat(publishers.getJSONObject(0).keySet()).containsExactlyInAnyOrder("kind", "name", "events");
      assertEquals("TABLE", publishers.getJSONObject(0).getString("kind"));
      assertEquals(TABLE_NAME_ORDER, publishers.getJSONObject(0).getString("name"));
      assertEquals(List.of("INSERT", "UPDATE"), publishers.getJSONObject(0).getJSONArray("events").toList());
      assertEquals("PROCESS", publishers.getJSONObject(1).getString("kind"));
      assertEquals(PROCESS_NAME_FULFILL_ORDER, publishers.getJSONObject(1).getString("name"));
      assertEquals(List.of("COMPLETED"), publishers.getJSONObject(1).getJSONArray("events").toList());

      assertEquals(1, orderEvents.getJSONArray("triggers").length());
      assertEquals(TRIGGER_SYNC_ORDER, orderEvents.getJSONArray("triggers").getJSONObject(0).getString("name"));

      JSONObject fulfillment = destinations.getJSONObject(1);
      assertEquals(DESTINATION_FULFILLMENT, fulfillment.getString("name"));
      assertEquals(TRIGGER_FULFILL_ORDER, fulfillment.getJSONArray("triggers").getJSONObject(0).getString("name"));

      //////////////////////////////////////////////////////////////////////
      // the management API is unreachable - so no broker data, but still //
      // the management capabilities, and a 200                           //
      //////////////////////////////////////////////////////////////////////
      JSONObject rabbitQueue = destinations.getJSONObject(2);
      assertEquals(RABBIT_QUEUE, rabbitQueue.getString("name"));
      assertEquals(RABBIT_PROVIDER, rabbitQueue.getString("provider"));
      assertTrue(rabbitQueue.isNull("queueInfo"));
      assertTrue(rabbitQueue.getJSONObject("capabilities").getBoolean("purge"));
      assertEquals(1, rabbitQueue.getJSONArray("publishers").length());
      assertEquals(RABBIT_PROCESS, rabbitQueue.getJSONArray("publishers").getJSONObject(0).getString("name"));
      assertEquals(0, rabbitQueue.getJSONArray("triggers").length());
   }



   /*******************************************************************************
    ** With a managementUrl, a queue's queueInfo and a trigger's dead-letter
    ** count come from the broker's management API (each queue asked once).
    *******************************************************************************/
   @Test
   void testQueueInfoFromManagementApi() throws Exception
   {
      try(MockManagementServer server = new MockManagementServer())
      {
         server.withResponse(200, "{\"name\":\"rabbitQueue\",\"messages\":7,\"consumers\":2}")
            .withResponse(200, "{\"name\":\"rabbitQueue.dlq\",\"messages\":3,\"consumers\":0}");
         addRabbitProvider(qInstance, server.getBaseUrl());
         EsbProcessMetaData.of(qInstance.getProcess(RABBIT_PROCESS)).withTrigger(new EsbTrigger().withDestinationName(RABBIT_QUEUE));

         JSONObject body = getJson("/qqq/v1/esb/process/" + RABBIT_PROCESS);

         JSONObject destination = body.getJSONArray("publications").getJSONObject(0).getJSONObject("destination");
         JSONObject queueInfo   = destination.getJSONObject("queueInfo");
         assertThat(queueInfo.keySet()).containsExactlyInAnyOrder("messageCount", "consumerCount", "paused");
         assertEquals(7, queueInfo.getInt("messageCount"));
         assertEquals(2, queueInfo.getInt("consumerCount"));
         assertFalse(queueInfo.getBoolean("paused"));
         assertTrue(destination.getJSONObject("capabilities").getBoolean("purge"));
         assertFalse(destination.getJSONObject("capabilities").getBoolean("move"));

         JSONObject trigger = body.getJSONArray("triggers").getJSONObject(0);
         assertEquals(7, trigger.getJSONObject("destination").getJSONObject("queueInfo").getInt("messageCount"));
         assertEquals("rabbitQueue.dlq", trigger.getJSONObject("deadLetter").getString("brokerName"));
         assertEquals(3, trigger.getJSONObject("deadLetter").getInt("messageCount"));

         assertEquals(2, server.getRequests().size());
      }
   }



   /*******************************************************************************
    ** 404 when the table, process, trigger or destination is unknown, or the
    ** table or process has no ESB meta-data.
    *******************************************************************************/
   @Test
   void testNotFoundWithoutEsbMetaData() throws Exception
   {
      setPermissions("esbView.hasAccess");
      for(String path : List.of(
         "/qqq/v1/esb/table/" + TABLE_NAME_CUSTOMER,
         "/qqq/v1/esb/table/noSuchTable",
         "/qqq/v1/esb/process/" + PROCESS_NAME_PLAIN,
         "/qqq/v1/esb/process/noSuchProcess",
         "/qqq/v1/esb/deadLetters/noSuchProcess.orderEvents",
         "/qqq/v1/esb/deadLetters/" + PROCESS_NAME_SYNC_ORDER + ".noSuchDestination",
         "/qqq/v1/esb/messages/noSuchDestination",
         "/qqq/v1/esb/messages/" + DESTINATION_ORDER_EVENTS + "?trigger=" + TRIGGER_FULFILL_ORDER))
      {
         assertEquals(404, get(path).statusCode(), path);
      }
   }



   /*******************************************************************************
    ** The overview requires ESB instance meta-data, even when the user has
    ** permission to view the ESB app.
    *******************************************************************************/
   @Test
   void testOverviewNotFoundWithoutEsbInstanceMetaData() throws Exception
   {
      setPermissions("esbView.hasAccess");
      qInstance.getSupplementalMetaData().remove(EsbInstanceMetaData.NAME);

      assertEquals(404, get("/qqq/v1/esb/overview").statusCode());
   }



   /*******************************************************************************
    ** GET /messages/{destination}: a queue's messages, paged, with each one's
    ** CloudEvent when it is one.
    *******************************************************************************/
   @Test
   void testMessagesEndpointPagesAQueue() throws Exception
   {
      EsbEvent event = new EsbEvent()
         .withId("event-1")
         .withSource("qqq:///table/order")
         .withType("com.kingsrook.qqq.table.order.insert")
         .withTime(Instant.parse("2026-09-25T12:00:00Z"))
         .withData(Map.of("id", 1));
      sendToQueue(BROKER_NAME_FULFILLMENT, Map.of(), EsbEventCodec.toJson(event), "not json", "third");

      JSONObject firstPage = getJson("/qqq/v1/esb/messages/" + DESTINATION_FULFILLMENT + "?offset=0&limit=2");
      assertThat(firstPage.keySet()).containsExactlyInAnyOrder("messages", "hasMore");
      assertTrue(firstPage.getBoolean("hasMore"));
      JSONArray messages = firstPage.getJSONArray("messages");
      assertEquals(2, messages.length());

      JSONObject first = messages.getJSONObject(0);
      assertThat(first.keySet()).containsExactlyInAnyOrderElementsOf(MESSAGE_KEYS);
      assertThat(first.getString("messageId")).isNotBlank();
      assertThat(Instant.parse(first.getString("timestamp"))).isNotNull();
      assertThat(first.getInt("deliveryCount")).isNotNegative();
      assertEquals("event-1", first.getJSONObject("event").getString("id"));
      assertEquals("1.0", first.getJSONObject("event").getString("specversion"));
      assertEquals("2026-09-25T12:00:00Z", first.getJSONObject("event").getString("time"));
      assertEquals(1, first.getJSONObject("event").getJSONObject("data").getInt("id"));

      JSONObject second = messages.getJSONObject(1);
      assertTrue(second.isNull("event"));
      assertEquals("not json", second.getString("rawBody"));

      JSONObject secondPage = getJson("/qqq/v1/esb/messages/" + DESTINATION_FULFILLMENT + "?offset=2&limit=2");
      assertFalse(secondPage.getBoolean("hasMore"));
      assertEquals(1, secondPage.getJSONArray("messages").length());
      assertEquals("third", secondPage.getJSONArray("messages").getJSONObject(0).getString("rawBody"));

      JSONObject defaultPage = getJson("/qqq/v1/esb/messages/" + DESTINATION_FULFILLMENT);
      assertEquals(3, defaultPage.getJSONArray("messages").length());
   }



   /*******************************************************************************
    ** A topic's messages are browsed on one trigger's subscription (named in
    ** the trigger query parameter); without one, it's a bad request.
    *******************************************************************************/
   @Test
   void testMessagesEndpointBrowsesATopicSubscription() throws Exception
   {
      try(Session session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
      {
         Topic           topic    = session.createTopic(DESTINATION_ORDER_EVENTS);
         MessageConsumer consumer = session.createSharedDurableConsumer(topic, SUBSCRIPTION_SYNC_ORDER);
         consumer.close();

         MessageProducer producer = session.createProducer(topic);
         producer.send(session.createTextMessage("for the subscription"));
      }

      JSONObject page = getJson("/qqq/v1/esb/messages/" + DESTINATION_ORDER_EVENTS + "?trigger=" + TRIGGER_SYNC_ORDER);
      assertEquals(1, page.getJSONArray("messages").length());
      assertEquals("for the subscription", page.getJSONArray("messages").getJSONObject(0).getString("rawBody"));

      assertEquals(400, get("/qqq/v1/esb/messages/" + DESTINATION_ORDER_EVENTS).statusCode());
   }



   /*******************************************************************************
    ** GET /deadLetters/{trigger}: the trigger's dead-letter queue, with each
    ** message's properties (e.g., qqqError).
    *******************************************************************************/
   @Test
   void testDeadLettersEndpoint() throws Exception
   {
      sendToQueue(DEAD_LETTERS_FULFILL_ORDER, Map.of("qqqError", "boom"), "dead letter");

      JSONObject page = getJson("/qqq/v1/esb/deadLetters/" + TRIGGER_FULFILL_ORDER + "?offset=0&limit=50");
      assertFalse(page.getBoolean("hasMore"));
      JSONObject message = page.getJSONArray("messages").getJSONObject(0);
      assertEquals("dead letter", message.getString("rawBody"));
      assertEquals("boom", message.getJSONObject("properties").getString("qqqError"));
   }



   /*******************************************************************************
    ** Bad paging parameters are a 400.
    *******************************************************************************/
   @Test
   void testBadPagingIsBadRequest() throws Exception
   {
      for(String query : List.of("?offset=-1", "?limit=0", "?limit=abc", "?offset=x", "?limit=100000"))
      {
         assertEquals(400, get("/qqq/v1/esb/messages/" + DESTINATION_FULFILLMENT + query).statusCode(), query);
         assertEquals(400, get("/qqq/v1/esb/deadLetters/" + TRIGGER_FULFILL_ORDER + query).statusCode(), query);
      }
   }



   /*******************************************************************************
    ** The meta-data producer registers the route provider with Javalin once.
    *******************************************************************************/
   @Test
   void testJavalinMetaDataProducerRegistersRouteProvider() throws Exception
   {
      QInstance instance = defineInstance();
      new EsbJavalinMetaDataProducer().produce(instance);
      QJavalinMetaData javalinMetaData = new EsbJavalinMetaDataProducer().produce(instance);

      assertThat(javalinMetaData.getAdditionalRouteProviderReferences())
         .extracting(QCodeReference::getName)
         .containsExactly(EsbRouteProvider.class.getName());
      assertThat(QJavalinMetaData.of(instance)).isSameAs(javalinMetaData);
   }



   /*******************************************************************************
    ** Add a RabbitMQ provider (with credentials in its url, and a management
    ** url), a queue on it, and a process publishing to that queue.
    *******************************************************************************/
   private static void addRabbitProvider(QInstance instance, String managementUrl)
   {
      EsbInstanceMetaData.of(instance)
         .withProvider(new QEsbProviderMetaData()
            .withName(RABBIT_PROVIDER)
            .withType(EsbProviderType.RABBITMQ)
            .withUrl(RABBIT_URL)
            .withUsername("esbUser")
            .withPassword("esbSecret")
            .withManagementUrl(managementUrl)
            .withManagementUsername("esbUser")
            .withManagementPassword("mgmtSecret"))
         .withDestination(new QEsbDestinationMetaData()
            .withName(RABBIT_QUEUE)
            .withType(EsbDestinationType.QUEUE)
            .withProviderName(RABBIT_PROVIDER));

      QProcessMetaData process = new QProcessMetaData()
         .withName(RABBIT_PROCESS)
         .withStep(new QBackendStepMetaData().withName("step").withCode(new QCodeReference(SyncOrderStep.class)));
      EsbProcessMetaData.ofOrWithNew(process)
         .withPublication(new EsbProcessPublication().withDestinationName(RABBIT_QUEUE).withEvents(List.of(EsbProcessEvent.STARTED)));
      instance.addProcess(process);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void assertDestination(JSONObject destination, String name, String type, String brokerName)
   {
      assertThat(destination.keySet()).containsExactlyInAnyOrderElementsOf(DESTINATION_KEYS);
      assertEquals(name, destination.getString("name"));
      assertEquals(type, destination.getString("type"));
      assertEquals(PROVIDER_NAME, destination.getString("provider"));
      assertEquals(brokerName, destination.getString("brokerName"));
      assertThat(destination.getJSONObject("counters").keySet()).containsExactlyInAnyOrderElementsOf(COUNTER_KEYS);
      assertThat(destination.getJSONObject("capabilities").keySet()).containsExactlyInAnyOrderElementsOf(CAPABILITY_KEYS);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void assertPermissions(JSONObject permissions, boolean canOperate, boolean canDelete)
   {
      assertThat(permissions.keySet()).containsExactlyInAnyOrderElementsOf(PERMISSION_KEYS);
      assertEquals(canOperate, permissions.getBoolean("canOperate"));
      assertEquals(canDelete, permissions.getBoolean("canDelete"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> concat(List<String> a, List<String> b)
   {
      return (Stream.concat(a.stream(), b.stream()).toList());
   }

}
