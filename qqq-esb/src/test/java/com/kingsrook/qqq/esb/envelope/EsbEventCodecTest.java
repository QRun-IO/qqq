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

package com.kingsrook.qqq.esb.envelope;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.esb.EsbTestBase;
import jakarta.jms.Connection;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for EsbEventCodec: CloudEvents 1.0 structured JSON, and JMS
 ** TextMessages (against the embedded Artemis broker).
 *******************************************************************************/
class EsbEventCodecTest extends EsbTestBase
{

   /*******************************************************************************
    ** An event survives toJson / fromJson unchanged, including null values and
    ** exact decimals inside its data.
    *******************************************************************************/
   @Test
   void testJsonRoundTrip() throws Exception
   {
      EsbEvent event = newFullEvent();

      EsbEvent parsed = EsbEventCodec.fromJson(EsbEventCodec.toJson(event));
      assertThat(parsed).usingRecursiveComparison().isEqualTo(event);

      @SuppressWarnings("unchecked")
      Map<String, Serializable> record = (Map<String, Serializable>) parsed.getData().get("record");
      assertThat(record).containsEntry("status", null);
      assertThat(record.get("total")).isEqualTo(new BigDecimal("19.90"));
   }



   /*******************************************************************************
    ** The JSON has the CloudEvents 1.0 attributes from spec section 4, with the
    ** causation id as the qqqcausationid extension attribute.
    *******************************************************************************/
   @Test
   void testJsonAttributes()
   {
      EsbEvent   event = newFullEvent();
      JSONObject json  = JsonUtils.toJSONObject(EsbEventCodec.toJson(event));

      assertThat(json.getString("specversion")).isEqualTo("1.0");
      assertThat(json.getString("datacontenttype")).isEqualTo("application/json");
      assertThat(json.getString("id")).isEqualTo(event.getId());
      assertThat(json.getString("source")).isEqualTo("qqq://test/table/order");
      assertThat(json.getString("type")).isEqualTo("qqq.table.order.updated");
      assertThat(json.getString("subject")).isEqualTo("47");
      assertThat(json.getString("time")).isEqualTo("2026-09-25T12:00:00.123456Z");
      assertThat(json.getString("qqqcausationid")).isEqualTo("cause-1");
      assertThat(json.getJSONObject("data").getJSONObject("record").getString("orderNo")).isEqualTo("A-47");
      assertThat(json.getJSONObject("data").getJSONObject("record").isNull("status")).isTrue();
   }



   /*******************************************************************************
    ** Unset optional attributes are left out of the JSON, and read back as null.
    *******************************************************************************/
   @Test
   void testOptionalAttributesOmittedWhenNull() throws Exception
   {
      EsbEvent event = new EsbEvent()
         .withId("id-1")
         .withSource("qqq://test/process/syncOrder")
         .withType("qqq.process.syncOrder.started");

      JSONObject json = JsonUtils.toJSONObject(EsbEventCodec.toJson(event));
      assertThat(json.keySet()).containsExactlyInAnyOrder("specversion", "id", "source", "type", "datacontenttype");

      EsbEvent parsed = EsbEventCodec.fromJson(EsbEventCodec.toJson(event));
      assertThat(parsed.getSubject()).isNull();
      assertThat(parsed.getCausationId()).isNull();
      assertThat(parsed.getTime()).isNull();
      assertThat(parsed.getData()).isNull();
   }



   /*******************************************************************************
    ** An event made by the factory round-trips; its record's Instant values come
    ** back as ISO-8601 strings.
    *******************************************************************************/
   @Test
   void testFactoryEventRoundTrip() throws Exception
   {
      QRecord record = new QRecord()
         .withValue("id", 1)
         .withValue("orderNo", "A-1")
         .withValue("createDate", Instant.parse("2026-09-25T12:00:00Z"));

      EsbEvent event  = EsbEventFactory.forRecordChange("test", TABLE_NAME_ORDER, RecordChangeType.INSERT, record, null);
      EsbEvent parsed = EsbEventCodec.fromJson(EsbEventCodec.toJson(event));

      assertThat(parsed.getId()).isEqualTo(event.getId());
      assertThat(parsed.getTime()).isEqualTo(event.getTime());
      assertThat(parsed.getSubject()).isEqualTo("1");
      assertThat(parsed.getData().get("record")).isEqualTo(Map.of("id", 1, "orderNo", "A-1", "createDate", "2026-09-25T12:00:00Z"));
   }



   /*******************************************************************************
    ** A minimal CloudEvent from another producer parses; attributes QQQ doesn't
    ** use are ignored, and a time with an offset is converted to UTC.
    *******************************************************************************/
   @Test
   void testFromJsonMinimalForeignEvent() throws Exception
   {
      EsbEvent parsed = EsbEventCodec.fromJson("""
         {"specversion": "1.0", "id": "abc", "source": "/other/app", "type": "com.example.thing",
          "time": "2026-09-25T14:00:00+02:00", "someextension": "x", "data": {"a": [1, 2.5, {"b": true}]}}
         """);

      assertThat(parsed.getId()).isEqualTo("abc");
      assertThat(parsed.getSource()).isEqualTo("/other/app");
      assertThat(parsed.getType()).isEqualTo("com.example.thing");
      assertThat(parsed.getTime()).isEqualTo(Instant.parse("2026-09-25T12:00:00Z"));
      assertThat(parsed.getData().get("a")).isEqualTo(List.of(1, new BigDecimal("2.5"), Map.of("b", true)));
   }



   /*******************************************************************************
    ** Non-JSON input, and JSON that isn't a CloudEvents 1.0 event with a JSON
    ** object (or no) data, throw EsbUnparseableMessageException.
    *******************************************************************************/
   @Test
   void testFromJsonUnparseable()
   {
      String valid = "\"specversion\": \"1.0\", \"id\": \"abc\", \"source\": \"/s\", \"type\": \"t\"";

      List<String> unparseable = new ArrayList<>(List.of(
         "not json",
         "",
         "   ",
         "null",
         "[]",
         "123",
         "\"a string\"",
         "{}",
         "{" + valid + "} trailing",
         "{" + valid + ", \"data\": \"a string\"}",
         "{" + valid + ", \"data\": [1, 2]}",
         "{" + valid + ", \"data_base64\": \"AAEC\"}",
         "{" + valid + ", \"time\": \"yesterday\"}",
         "{" + valid + ", \"time\": 12345}",
         "{" + valid + ", \"subject\": 47}",
         "{" + valid + ", \"qqqcausationid\": {}}",
         "{\"specversion\": \"0.3\", \"id\": \"abc\", \"source\": \"/s\", \"type\": \"t\"}",
         "{\"id\": \"abc\", \"source\": \"/s\", \"type\": \"t\"}",
         "{\"specversion\": \"1.0\", \"source\": \"/s\", \"type\": \"t\"}",
         "{\"specversion\": \"1.0\", \"id\": \"\", \"source\": \"/s\", \"type\": \"t\"}",
         "{\"specversion\": \"1.0\", \"id\": 7, \"source\": \"/s\", \"type\": \"t\"}",
         "{\"specversion\": \"1.0\", \"id\": \"abc\", \"type\": \"t\"}",
         "{\"specversion\": \"1.0\", \"id\": \"abc\", \"source\": \"/s\"}"
      ));
      unparseable.add(null);

      for(String json : unparseable)
      {
         assertThatThrownBy(() -> EsbEventCodec.fromJson(json))
            .as("unparseable: " + json)
            .isInstanceOf(EsbUnparseableMessageException.class);
      }

      assertThatThrownBy(() -> EsbEventCodec.fromJson("not json"))
         .isInstanceOf(EsbUnparseableMessageException.class)
         .hasMessageContaining("not a JSON object");
   }



   /*******************************************************************************
    ** toJson refuses an event without the required CloudEvent attributes.
    *******************************************************************************/
   @Test
   void testToJsonRequiresIdSourceAndType()
   {
      assertThatThrownBy(() -> EsbEventCodec.toJson(new EsbEvent().withSource("s").withType("t")))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("id");

      assertThatThrownBy(() -> EsbEventCodec.toJson(new EsbEvent().withId("i").withType("t")))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("source");

      assertThatThrownBy(() -> EsbEventCodec.toJson(new EsbEvent().withId("i").withSource("s")))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("type");

      assertThatThrownBy(() -> EsbEventCodec.toJson(null))
         .isInstanceOf(IllegalArgumentException.class);
   }



   /*******************************************************************************
    ** toMessage makes a TextMessage with the JSON body and the ce_ properties,
    ** and fromMessage reads it back after a trip through the broker.
    *******************************************************************************/
   @Test
   void testToMessageAndFromMessage() throws Exception
   {
      EsbEvent event = newFullEvent();

      try(Connection connection = new ActiveMQConnectionFactory(getBrokerUrl()).createConnection())
      {
         connection.start();
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         Queue   queue   = session.createQueue("esbEventCodecTest." + UUID.randomUUID());

         TextMessage message = EsbEventCodec.toMessage(session, event);
         assertThat(message.getText()).isEqualTo(EsbEventCodec.toJson(event));
         assertThat(message.getStringProperty("ce_id")).isEqualTo(event.getId());
         assertThat(message.getStringProperty("ce_type")).isEqualTo("qqq.table.order.updated");
         assertThat(message.getStringProperty("ce_source")).isEqualTo("qqq://test/table/order");

         try(MessageProducer producer = session.createProducer(queue); MessageConsumer consumer = session.createConsumer(queue))
         {
            producer.send(message);

            Message received = consumer.receive(5000);
            assertThat(received).isNotNull();
            assertThat(received.getStringProperty("ce_id")).isEqualTo(event.getId());
            assertThat(EsbEventCodec.fromMessage(received)).usingRecursiveComparison().isEqualTo(event);
         }
      }
   }



   /*******************************************************************************
    ** Only TextMessages hold events; anything else is unparseable.
    *******************************************************************************/
   @Test
   void testFromMessageNotText() throws Exception
   {
      try(Connection connection = new ActiveMQConnectionFactory(getBrokerUrl()).createConnection())
      {
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

         assertThatThrownBy(() -> EsbEventCodec.fromMessage(session.createBytesMessage()))
            .isInstanceOf(EsbUnparseableMessageException.class)
            .hasMessageContaining("not a JMS TextMessage");

         assertThatThrownBy(() -> EsbEventCodec.fromMessage(session.createTextMessage("not json")))
            .isInstanceOf(EsbUnparseableMessageException.class);

         assertThatThrownBy(() -> EsbEventCodec.fromMessage(null))
            .isInstanceOf(EsbUnparseableMessageException.class);
      }
   }



   /*******************************************************************************
    ** An update event with every attribute set, and a record holding a null value
    ** and a decimal with a trailing zero.
    *******************************************************************************/
   private static EsbEvent newFullEvent()
   {
      LinkedHashMap<String, Serializable> record = new LinkedHashMap<>();
      record.put("id", 47);
      record.put("orderNo", "A-47");
      record.put("status", null);
      record.put("total", new BigDecimal("19.90"));
      record.put("big", 12_345_678_901L);

      LinkedHashMap<String, Serializable> oldRecord = new LinkedHashMap<>(record);
      oldRecord.put("status", "NEW");

      LinkedHashMap<String, Serializable> data = new LinkedHashMap<>();
      data.put("record", record);
      data.put("oldRecord", oldRecord);

      return (new EsbEvent()
         .withId(UUID.randomUUID().toString())
         .withSource("qqq://test/table/order")
         .withType("qqq.table.order.updated")
         .withSubject("47")
         .withCausationId("cause-1")
         .withTime(Instant.parse("2026-09-25T12:00:00.123456Z"))
         .withData(data));
   }

}
