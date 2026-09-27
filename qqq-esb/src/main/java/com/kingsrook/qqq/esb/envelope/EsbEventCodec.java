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

package com.kingsrook.qqq.esb.envelope;


import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;


/*******************************************************************************
 * Reads and writes EsbEvents as CloudEvents 1.0 structured JSON (spec section
 * 4), and as JMS TextMessages.
 *
 * Written JSON always has specversion "1.0" and datacontenttype
 * "application/json"; subject, time, qqqcausationid (the causation id, as a
 * CloudEvent extension attribute) and data are left out when null.  Values
 * inside data are written as they are, nulls included; Instants and other
 * java.time values are written as ISO-8601 strings.
 *
 * Reading accepts any CloudEvents 1.0 JSON event whose data (if any) is a JSON
 * object; other attributes are ignored.  Decimal numbers in data are read as
 * BigDecimal, so they keep their exact value.  Anything else throws
 * EsbUnparseableMessageException.
 *
 * Uses one shared, thread-safe Jackson mapper, because building one per message
 * is slow for bulk publishes.
 *******************************************************************************/
public class EsbEventCodec
{
   public static final String SPEC_VERSION      = "1.0";
   public static final String DATA_CONTENT_TYPE = "application/json";

   public static final String ATTRIBUTE_SPEC_VERSION      = "specversion";
   public static final String ATTRIBUTE_ID                = "id";
   public static final String ATTRIBUTE_SOURCE            = "source";
   public static final String ATTRIBUTE_TYPE              = "type";
   public static final String ATTRIBUTE_SUBJECT           = "subject";
   public static final String ATTRIBUTE_TIME              = "time";
   public static final String ATTRIBUTE_DATA_CONTENT_TYPE = "datacontenttype";
   public static final String ATTRIBUTE_CAUSATION_ID      = "qqqcausationid";
   public static final String ATTRIBUTE_DATA              = "data";
   public static final String ATTRIBUTE_DATA_BASE64       = "data_base64";

   /////////////////////////////////////////////////////////////
   // JMS properties that mirror CloudEvent attributes, so    //
   // tools that browse a queue can see them without parsing  //
   /////////////////////////////////////////////////////////////
   public static final String PROPERTY_ID     = "ce_id";
   public static final String PROPERTY_TYPE   = "ce_type";
   public static final String PROPERTY_SOURCE = "ce_source";

   private static final JsonMapper MAPPER = JsonMapper.builder()
      .addModule(new JavaTimeModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
      .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true)
      .build();

   private static final TypeReference<LinkedHashMap<String, Object>> JSON_OBJECT_TYPE = new TypeReference<>()
   {
   };



   /*******************************************************************************
    ** Write an event as CloudEvents 1.0 structured JSON.
    **
    ** Throws IllegalArgumentException if the event is null, is missing its id,
    ** source, or type, or has data that can't be written as JSON.
    *******************************************************************************/
   public static String toJson(EsbEvent event)
   {
      if(event == null)
      {
         throw (new IllegalArgumentException("Cannot write a null ESB event"));
      }

      requireAttribute(event.getId(), ATTRIBUTE_ID);
      requireAttribute(event.getSource(), ATTRIBUTE_SOURCE);
      requireAttribute(event.getType(), ATTRIBUTE_TYPE);

      Map<String, Object> json = new LinkedHashMap<>();
      json.put(ATTRIBUTE_SPEC_VERSION, SPEC_VERSION);
      json.put(ATTRIBUTE_ID, event.getId());
      json.put(ATTRIBUTE_SOURCE, event.getSource());
      json.put(ATTRIBUTE_TYPE, event.getType());
      putIfNotNull(json, ATTRIBUTE_SUBJECT, event.getSubject());
      putIfNotNull(json, ATTRIBUTE_TIME, event.getTime() == null ? null : event.getTime().toString());
      json.put(ATTRIBUTE_DATA_CONTENT_TYPE, DATA_CONTENT_TYPE);
      putIfNotNull(json, ATTRIBUTE_CAUSATION_ID, event.getCausationId());
      putIfNotNull(json, ATTRIBUTE_DATA, event.getData());

      try
      {
         return (MAPPER.writeValueAsString(json));
      }
      catch(JsonProcessingException e)
      {
         throw (new IllegalArgumentException("Error writing ESB event as JSON", e));
      }
   }



   /*******************************************************************************
    ** Read an event from CloudEvents 1.0 structured JSON.
    *******************************************************************************/
   public static EsbEvent fromJson(String json) throws EsbUnparseableMessageException
   {
      if(!StringUtils.hasContent(json))
      {
         throw (new EsbUnparseableMessageException("Message body is empty"));
      }

      Map<String, Object> jsonObject;
      try
      {
         jsonObject = MAPPER.readValue(json, JSON_OBJECT_TYPE);
      }
      catch(JsonProcessingException e)
      {
         throw (new EsbUnparseableMessageException("Message body is not a JSON object", e));
      }

      if(jsonObject == null)
      {
         throw (new EsbUnparseableMessageException("Message body is not a JSON object"));
      }

      if(!SPEC_VERSION.equals(jsonObject.get(ATTRIBUTE_SPEC_VERSION)))
      {
         throw (new EsbUnparseableMessageException("Message is not a CloudEvents 1.0 event (specversion is not \"1.0\")"));
      }

      if(jsonObject.containsKey(ATTRIBUTE_DATA_BASE64))
      {
         throw (new EsbUnparseableMessageException("CloudEvent has binary data (data_base64), which is not supported"));
      }

      return (new EsbEvent()
         .withId(getRequiredString(jsonObject, ATTRIBUTE_ID))
         .withSource(getRequiredString(jsonObject, ATTRIBUTE_SOURCE))
         .withType(getRequiredString(jsonObject, ATTRIBUTE_TYPE))
         .withSubject(getOptionalString(jsonObject, ATTRIBUTE_SUBJECT))
         .withTime(getOptionalTime(jsonObject))
         .withCausationId(getOptionalString(jsonObject, ATTRIBUTE_CAUSATION_ID))
         .withData(getOptionalData(jsonObject)));
   }



   /*******************************************************************************
    ** Make a JMS TextMessage holding the event's JSON, with the ce_id, ce_type,
    ** and ce_source properties set.
    *******************************************************************************/
   public static TextMessage toMessage(Session session, EsbEvent event) throws JMSException
   {
      TextMessage message = session.createTextMessage(toJson(event));
      message.setStringProperty(PROPERTY_ID, event.getId());
      message.setStringProperty(PROPERTY_TYPE, event.getType());
      message.setStringProperty(PROPERTY_SOURCE, event.getSource());
      return (message);
   }



   /*******************************************************************************
    ** Read an event from a JMS message.  Only TextMessages hold events.
    **
    ** Throws JMSException if the broker client can't read the message, and
    ** EsbUnparseableMessageException if the message isn't an event.
    *******************************************************************************/
   public static EsbEvent fromMessage(Message message) throws JMSException, EsbUnparseableMessageException
   {
      if(message instanceof TextMessage textMessage)
      {
         return (fromJson(textMessage.getText()));
      }

      String kind = (message == null) ? "null" : message.getClass().getSimpleName();
      throw (new EsbUnparseableMessageException("Message is not a JMS TextMessage (" + kind + ")"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void requireAttribute(String value, String attributeName)
   {
      if(!StringUtils.hasContent(value))
      {
         throw (new IllegalArgumentException("ESB event is missing its " + attributeName));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void putIfNotNull(Map<String, Object> json, String attributeName, Object value)
   {
      if(value != null)
      {
         json.put(attributeName, value);
      }
   }



   /*******************************************************************************
    ** A required attribute: a non-empty string.
    *******************************************************************************/
   private static String getRequiredString(Map<String, Object> jsonObject, String attributeName) throws EsbUnparseableMessageException
   {
      if(jsonObject.get(attributeName) instanceof String value && !value.isEmpty())
      {
         return (value);
      }

      throw (new EsbUnparseableMessageException("CloudEvent is missing its " + attributeName + " (or it is not a non-empty string)"));
   }



   /*******************************************************************************
    ** An optional attribute: absent, null, or a string.
    *******************************************************************************/
   private static String getOptionalString(Map<String, Object> jsonObject, String attributeName) throws EsbUnparseableMessageException
   {
      Object value = jsonObject.get(attributeName);
      if(value == null || value instanceof String)
      {
         return ((String) value);
      }

      throw (new EsbUnparseableMessageException("CloudEvent " + attributeName + " is not a string"));
   }



   /*******************************************************************************
    ** The optional time attribute: an RFC 3339 timestamp (any offset).
    *******************************************************************************/
   private static Instant getOptionalTime(Map<String, Object> jsonObject) throws EsbUnparseableMessageException
   {
      String time = getOptionalString(jsonObject, ATTRIBUTE_TIME);
      if(time == null)
      {
         return (null);
      }

      try
      {
         return (Instant.parse(time));
      }
      catch(DateTimeParseException e)
      {
         throw (new EsbUnparseableMessageException("CloudEvent time is not an RFC 3339 timestamp", e));
      }
   }



   /*******************************************************************************
    ** The optional data attribute: absent, null, or a JSON object.
    *******************************************************************************/
   private static Map<String, Serializable> getOptionalData(Map<String, Object> jsonObject) throws EsbUnparseableMessageException
   {
      Object data = jsonObject.get(ATTRIBUTE_DATA);
      if(data == null)
      {
         return (null);
      }

      if(!(data instanceof Map<?, ?> dataObject))
      {
         throw (new EsbUnparseableMessageException("CloudEvent data is not a JSON object"));
      }

      /////////////////////////////////////////////////////////////////////
      // Jackson reads untyped JSON as LinkedHashMap, ArrayList, String, //
      // Integer, Long, BigInteger, BigDecimal, and Boolean - all of     //
      // which are Serializable, so these casts always succeed           //
      /////////////////////////////////////////////////////////////////////
      LinkedHashMap<String, Serializable> result = new LinkedHashMap<>();
      for(Map.Entry<?, ?> entry : dataObject.entrySet())
      {
         result.put((String) entry.getKey(), (Serializable) entry.getValue());
      }

      return (result);
   }

}
