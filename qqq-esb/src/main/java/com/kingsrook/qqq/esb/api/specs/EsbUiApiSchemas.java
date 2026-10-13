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

package com.kingsrook.qqq.esb.api.specs;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** OpenAPI component schemas of the ESB UI API responses (the shapes
 ** EsbStatusBuilder produces).  Names carry the Esb prefix so they cannot
 ** collide with core schemas.
 *******************************************************************************/
public class EsbUiApiSchemas
{
   public static final String COUNTER          = "EsbCounter";
   public static final String QUEUE_INFO       = "EsbQueueInfo";
   public static final String CAPABILITIES     = "EsbCapabilities";
   public static final String DESTINATION      = "EsbDestination";
   public static final String DEAD_LETTER      = "EsbDeadLetter";
   public static final String TRIGGER          = "EsbTrigger";
   public static final String PERMISSIONS      = "EsbPermissions";
   public static final String PUBLICATION      = "EsbPublication";
   public static final String PROVIDER         = "EsbProvider";
   public static final String PUBLISHER        = "EsbPublisher";
   public static final String OVERVIEW_DEST    = "EsbOverviewDestination";
   public static final String MESSAGE          = "EsbMessage";
   public static final String OVERVIEW         = "EsbOverviewResponseV1";
   public static final String TABLE            = "EsbTableResponseV1";
   public static final String PROCESS          = "EsbProcessResponseV1";
   public static final String MESSAGE_PAGE     = "EsbMessagePageResponseV1";



   /*******************************************************************************
    ** All the component schemas, by name.
    *******************************************************************************/
   public static Map<String, Schema> all()
   {
      Map<String, Schema> rs = new LinkedHashMap<>();

      rs.put(COUNTER, object("Activity counters since the server started.",
         "published", integer("Messages published."),
         "publishFailures", integer("Publish attempts that failed."),
         "consumed", integer("Messages consumed."),
         "succeeded", integer("Consumed messages processed successfully."),
         "failed", integer("Consumed messages that failed."),
         "retried", integer("Redeliveries."),
         "deadLettered", integer("Messages moved to the dead-letter destination."),
         "inFlight", integer("Messages being processed now."),
         "lastActivity", nullable(string("Time of the last activity (ISO-8601), or null.").withFormat("date-time")),
         "avgMs", integer("Average processing time in milliseconds."),
         "maxMs", integer("Longest processing time in milliseconds."),
         "lastError", nullable(string("The last error message, or null."))));

      rs.put(QUEUE_INFO, object("Broker-reported state of a queue.",
         "messageCount", integer("Messages waiting."),
         "consumerCount", integer("Connected consumers."),
         "paused", bool("Whether the queue is paused.")));

      rs.put(CAPABILITIES, object("What the provider supports for this destination - show only the actions that are true.",
         "browse", bool("Messages can be browsed."),
         "pauseQueue", bool("The queue can be paused and resumed."),
         "purge", bool("All messages can be deleted."),
         "deleteSelected", bool("Selected messages can be deleted."),
         "deleteOlderThan", bool("Messages older than a time can be deleted."),
         "move", bool("Messages can be moved to another destination.")));

      rs.put(DESTINATION, object("A queue or topic.",
         "name", string("Destination name."),
         "type", enumOf("Destination type.", "QUEUE", "TOPIC"),
         "provider", string("Name of the provider (broker connection)."),
         "brokerName", string("Name of the destination on the broker."),
         "counters", ref(COUNTER),
         "queueInfo", nullable(ref(QUEUE_INFO)),
         "capabilities", ref(CAPABILITIES)));

      rs.put(DEAD_LETTER, object("A dead-letter (or subscription) destination of a trigger.",
         "brokerName", string("Name on the broker."),
         "messageCount", nullable(integer("Messages waiting, or null when unknown.")),
         "paused", nullable(bool("Whether it is paused, or null when unknown."))));

      rs.put(TRIGGER, object("A process subscribed to a destination.",
         "name", string("Trigger name."),
         "processName", string("Process the trigger runs."),
         "processLabel", string("User-facing label of the process."),
         "destination", ref(DESTINATION),
         "mode", enumOf("SINGLE runs the process per message; BATCH per batch of messages.", "SINGLE", "BATCH"),
         "concurrency", integer("Consumers run in parallel."),
         "maxAttempts", integer("Attempts before a message is dead-lettered."),
         "state", enumOf("Current state of the trigger.", "RUNNING", "PAUSED", "CONNECTING", "STOPPED"),
         "counters", ref(COUNTER),
         "deadLetter", ref(DEAD_LETTER),
         "subscription", nullable(ref(DEAD_LETTER))));

      rs.put(PERMISSIONS, object("What the user may do.",
         "canOperate", bool("May pause, resume and retry."),
         "canDelete", bool("May delete messages.")));

      rs.put(PUBLICATION, object("A destination that a table or process publishes events to.",
         "destination", ref(DESTINATION),
         "events", array("Events published: INSERT, UPDATE or DELETE for tables; STARTED, COMPLETED or FAILED for processes.", string("Event name."))));

      rs.put(PROVIDER, object("A broker connection.",
         "name", string("Provider name."),
         "type", enumOf("Broker type.", "ACTIVEMQ_ARTEMIS", "RABBITMQ"),
         "connected", bool("Whether the connection is up."),
         "managementEnabled", bool("Whether broker management (browse, pause, purge) is available.")));

      rs.put(PUBLISHER, object("A table or process that publishes to a destination.",
         "kind", enumOf("What publishes.", "TABLE", "PROCESS"),
         "name", string("Table or process name."),
         "events", array("Events published.", string("Event name."))));

      rs.put(OVERVIEW_DEST, new Schema()
         .withDescription("A destination with its publishers and triggers.")
         .withAllOf(List.of(ref(DESTINATION), object(null,
            "publishers", array("Tables and processes publishing to it.", ref(PUBLISHER)),
            "triggers", array("Triggers consuming it.", ref(TRIGGER))))));

      rs.put(MESSAGE, object("A message on a destination.",
         "messageId", string("Broker message id."),
         "timestamp", string("When it was sent (ISO-8601).").withFormat("date-time"),
         "deliveryCount", integer("Delivery attempts so far."),
         "event", nullable(new Schema().withType(Type.OBJECT).withDescription("The CloudEvents envelope (specversion, id, source, type, time, subject, datacontenttype, data, qqqcausationid), or null when the body is not one.").withAdditionalProperties(true)),
         "rawBody", string("The message body as text."),
         "properties", new Schema().withType(Type.OBJECT).withDescription("Message properties.").withAdditionalProperties(true)));

      rs.put(OVERVIEW, object("GET /esb/overview: providers and every destination.",
         "providers", array("Broker connections.", ref(PROVIDER)),
         "destinations", array("Destinations with their publishers and triggers.", ref(OVERVIEW_DEST)),
         "permissions", ref(PERMISSIONS)));

      rs.put(TABLE, object("GET /esb/table/{table}: what a table publishes, and who subscribes.",
         "table", string("Table name."),
         "publications", array("Destinations the table publishes to.", ref(PUBLICATION)),
         "subscribers", array("Triggers subscribed to those destinations (on processes the user can access).", ref(TRIGGER)),
         "permissions", ref(PERMISSIONS)));

      rs.put(PROCESS, object("GET /esb/process/{process}: what a process publishes, and the triggers that run it.",
         "process", string("Process name."),
         "publications", array("Destinations the process publishes to.", ref(PUBLICATION)),
         "triggers", array("Triggers that run the process.", ref(TRIGGER)),
         "permissions", ref(PERMISSIONS)));

      rs.put(MESSAGE_PAGE, object("A page of messages.",
         "messages", array("The messages, oldest first.", ref(MESSAGE)),
         "hasMore", bool("Whether more messages follow this page.")));

      return (rs);
   }



   /*******************************************************************************
    ** an object schema from a description and name/schema pairs
    *******************************************************************************/
   static Schema object(String description, Object... namesAndSchemas)
   {
      if(namesAndSchemas.length % 2 != 0)
      {
         throw (new IllegalArgumentException("Expected name/schema pairs, got an odd number of arguments"));
      }

      Map<String, Schema> properties = new LinkedHashMap<>();
      for(int i = 0; i + 1 < namesAndSchemas.length; i += 2)
      {
         properties.put((String) namesAndSchemas[i], (Schema) namesAndSchemas[i + 1]);
      }
      return (new Schema().withType(Type.OBJECT).withDescription(description).withProperties(properties));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Schema string(String description)
   {
      return (new Schema().withType(Type.STRING).withDescription(description));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Schema integer(String description)
   {
      return (new Schema().withType(Type.INTEGER).withDescription(description));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Schema bool(String description)
   {
      return (new Schema().withType(Type.BOOLEAN).withDescription(description));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Schema enumOf(String description, String... values)
   {
      return (string(description).withEnumValues(List.of(values)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Schema array(String description, Schema items)
   {
      return (new Schema().withType(Type.ARRAY).withDescription(description).withItems(items));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Schema ref(String componentName)
   {
      return (new Schema().withRefToSchema(componentName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Schema nullable(Schema schema)
   {
      return (schema.withNullable(true));
   }
}
