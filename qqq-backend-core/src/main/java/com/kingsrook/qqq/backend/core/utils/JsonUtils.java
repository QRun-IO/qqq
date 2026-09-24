/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils;


import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/*******************************************************************************
 ** Utility class for working with JSON.
 **
 ** See: https://www.baeldung.com/jackson-vs-gson
 **
 *******************************************************************************/
public class JsonUtils
{
   private static final QLogger LOG = QLogger.getLogger(JsonUtils.class);

   //////////////////////////////////////////////////////////////////////
   // see https://www.baeldung.com/jackson-map-null-values-or-null-key //
   //////////////////////////////////////////////////////////////////////
   public static NullKeyToEmptyStringSerializer nullKeyToEmptyStringSerializer = new NullKeyToEmptyStringSerializer();



   /*******************************************************************************
    ** Serialize any object into a JSON String.
    **
    ** Internally using jackson - so jackson annotations apply!
    **
    *******************************************************************************/
   public static String toJson(Object object)
   {
      return (toJsonCustomized(object, (Consumer<JsonMapper.Builder>) null));
   }



   /*******************************************************************************
    ** Serialize any object into a JSON String - with post-build customizations on
    ** the Jackson JsonMapper (for cases requiring access to the built mapper, such
    ** as configuring the SerializerProvider's null-key serializer).
    **
    ** Prefer toJsonCustomized(Object, Consumer&lt;JsonMapper.Builder&gt;) for builder-level
    ** customizations (e.g., serializationInclusion). Use this variant only when a
    ** post-build mapper instance is required.
    **
    ** Internally using jackson - so jackson annotations apply!
    **
    *******************************************************************************/
   public static String toJsonWithMapper(Object object, Consumer<JsonMapper> jsonMapperCustomizer)
   {
      try
      {
         JsonMapper mapper = newJsonMapperBuilder().build();
         if(jsonMapperCustomizer != null)
         {
            jsonMapperCustomizer.accept(mapper);
         }
         String jsonResult = mapper.writeValueAsString(object);
         return (jsonResult);
      }
      catch(JsonProcessingException e)
      {
         LOG.error("Error serializing object of type [" + object.getClass().getSimpleName() + "] to json", e);
         throw new IllegalArgumentException("Error in JSON Serialization", e);
      }
   }



   /*******************************************************************************
    ** Serialize any object into a JSON String - with builder-level customizations on
    ** the Jackson JsonMapper.
    **
    ** Internally using jackson - so jackson annotations apply!
    **
    *******************************************************************************/
   public static String toJsonCustomized(Object object, Consumer<JsonMapper.Builder> jsonMapperCustomizer)
   {
      try
      {
         JsonMapper.Builder jsonMapperBuilder = newJsonMapperBuilder();
         if(jsonMapperCustomizer != null)
         {
            jsonMapperCustomizer.accept(jsonMapperBuilder);
         }
         String jsonResult = jsonMapperBuilder.build().writeValueAsString(object);
         return (jsonResult);
      }
      catch(JsonProcessingException e)
      {
         LOG.error("Error serializing object of type [" + object.getClass().getSimpleName() + "] to json", e);
         throw new IllegalArgumentException("Error in JSON Serialization", e);
      }
   }



   /*******************************************************************************
    ** Serialize any object into a "pretty" / formatted JSON String.
    **
    ** Internally using jackson - so jackson annotations apply!
    **
    *******************************************************************************/
   public static String toPrettyJson(Object object)
   {
      try
      {
         ObjectMapper mapper       = newObjectMapper();
         ObjectWriter objectWriter = mapper.writerWithDefaultPrettyPrinter();
         String       jsonResult   = objectWriter.writeValueAsString(object);
         return (jsonResult);
      }
      catch(JsonProcessingException e)
      {
         LOG.error("Error serializing object of type [" + object.getClass().getSimpleName() + "] to json", e);
         throw new IllegalArgumentException("Error in JSON Serialization", e);
      }
   }



   /*******************************************************************************
    ** Serialize any object into a "pretty" / formatted JSON String.
    **
    *******************************************************************************/
   public static String prettyPrint(String json)
   {
      try
      {
         ObjectMapper mapper       = newObjectMapper();
         Object       object       = mapper.reader().readValue(json, Map.class);
         ObjectWriter objectWriter = mapper.writerWithDefaultPrettyPrinter();
         String       jsonResult   = objectWriter.writeValueAsString(object);
         return (jsonResult);
      }
      catch(Exception e)
      {
         LOG.error("Error pretty printing JSON string", e);
         throw new IllegalArgumentException("Error in pretty printing JSON", e);
      }
   }



   /*******************************************************************************
    ** De-serialize a json string into an object of the specified class.
    **
    ** Internally using jackson - so jackson annotations apply!
    **
    *******************************************************************************/
   public static <T> T toObject(String json, Class<T> targetClass) throws IOException
   {
      return (toObject(json, targetClass, null));
   }



   /*******************************************************************************
    ** De-serialize a json string into an object of the specified class - with
    ** customizations on the Jackson ObjectMapper.
    **
    ** Internally using jackson - so jackson annotations apply!
    **
    *******************************************************************************/
   public static <T> T toObject(String json, Class<T> targetClass, Consumer<ObjectMapper> objectMapperCustomizer) throws IOException
   {
      ObjectMapper objectMapper = newObjectMapper();
      if(objectMapperCustomizer != null)
      {
         objectMapperCustomizer.accept(objectMapper);
      }
      return objectMapper.reader().readValue(json, targetClass);
   }



   /*******************************************************************************
    ** De-serialize a json string into an object of the specified class.
    **
    ** Internally using jackson - so jackson annotations apply!
    **
    *******************************************************************************/
   public static <T> T toObject(String json, TypeReference<T> typeReference) throws IOException
   {
      ObjectMapper objectMapper = newObjectMapper();
      return objectMapper.readValue(json, typeReference);
   }



   /*******************************************************************************
    ** De-serialize a json string into an object of the specified class - with
    ** customizations on the Jackson ObjectMapper.
    **
    ** Internally using jackson - so jackson annotations apply!
    **
    *******************************************************************************/
   public static <T> T toObject(String json, TypeReference<T> typeReference, Consumer<ObjectMapper> objectMapperCustomizer) throws IOException
   {
      ObjectMapper objectMapper = newObjectMapper();
      if(objectMapperCustomizer != null)
      {
         objectMapperCustomizer.accept(objectMapper);
      }
      return objectMapper.readValue(json, typeReference);
   }



   /*******************************************************************************
    ** De-serialize a json string into a JSONObject (string must start with "{")
    **
    *******************************************************************************/
   public static JSONObject toJSONObject(String json) throws JSONException
   {
      JSONObject jsonObject = new JSONObject(json);
      return (jsonObject);
   }



   /*******************************************************************************
    ** De-serialize a json string into a JSONArray (string must start with "[")
    **
    *******************************************************************************/
   public static JSONArray toJSONArray(String json) throws JSONException
   {
      JSONArray jsonArray = new JSONArray(json);
      return (jsonArray);
   }



   /*******************************************************************************
    ** Standard private method to build jackson JsonMapperBuilder with standard features.
    **
    *******************************************************************************/
   private static JsonMapper.Builder newJsonMapperBuilder()
   {
      JsonMapper.Builder jsonMapperBuilder = JsonMapper.builder();
      jsonMapperBuilder.addModule(new JavaTimeModule());
      jsonMapperBuilder.serializationInclusion(JsonInclude.Include.NON_NULL);
      jsonMapperBuilder.serializationInclusion(JsonInclude.Include.NON_EMPTY);
      jsonMapperBuilder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
      jsonMapperBuilder.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

      return (jsonMapperBuilder);
   }


   /*******************************************************************************
    ** Standard private method to build jackson ObjectMapper with standard features.
    **
    *******************************************************************************/
   private static ObjectMapper newObjectMapper()
   {
      ObjectMapper mapper = new ObjectMapper()
         .registerModule(new JavaTimeModule())
         .setSerializationInclusion(JsonInclude.Include.NON_NULL)
         .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
         .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
         .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

      /* todo - some future version we may need to do inclusion/exclusion lists like this:
      // this is what we'd put on the class or member we wanted to 'filter':  @JsonFilter("secretsFilter")

      SimpleFilterProvider filterProvider = new SimpleFilterProvider();
      filterProvider.addFilter("secretsFilter", SimpleBeanPropertyFilter.serializeAllExcept("password"));
      mapper.setFilterProvider(filterProvider);
       */

      return (mapper);
   }



   /*******************************************************************************
    ** returns a JSONArray with a single value if given value looks like an object
    ** otherwise returns the JSONArray
    **
    *******************************************************************************/
   public static JSONArray getJSONArrayFromJSONObjectOrJSONArray(Object o)
   {
      JSONArray a = new JSONArray();
      if(o instanceof JSONObject)
      {
         a.put(o);
      }
      else
      {
         a = (JSONArray) o;
      }
      return (a);
   }



   /*******************************************************************************
    ** Check if a string looks like it could be a JSON object (e.g., starts with "{"
    ** (plus optional whitespace))
    **
    *******************************************************************************/
   public static boolean looksLikeObject(String json)
   {
      return (json != null && json.matches("(?s)\\s*\\{.*"));
   }



   /*******************************************************************************
    ** Check if a string looks like it could be a JSON array (e.g., starts with "["
    ** (plus optional whitespace))
    **
    *******************************************************************************/
   public static boolean looksLikeArray(String json)
   {
      return (json != null && json.matches("(?s)\\s*\\[.*"));
   }



   /*******************************************************************************
    ** Convert a json object into a QRecord
    **
    *******************************************************************************/
   public static QRecord parseQRecord(JSONObject jsonObject, Map<String, QFieldMetaData> fields, boolean useBackendFieldNames)
   {
      QRecord record = new QRecord();

      FIELDS_LOOP:
      for(String fieldName : fields.keySet())
      {
         String originalBackendName = null;
         try
         {
            QFieldMetaData metaData    = fields.get(fieldName);
            String         backendName = fieldName;
            if(useBackendFieldNames)
            {
               backendName = metaData.getBackendName() != null ? metaData.getBackendName() : fieldName;
            }

            originalBackendName = backendName;

            /////////////////////////////////////////////////////////////////////////////////////////////////
            // if the field backend name has dots in it, interpret that to mean traversal down sub-objects //
            /////////////////////////////////////////////////////////////////////////////////////////////////
            JSONObject jsonObjectToUse = jsonObject;
            if(backendName.contains("."))
            {
               ArrayList<String> levels = new ArrayList<>(List.of(backendName.split("\\.")));
               backendName = levels.remove(levels.size() - 1);

               for(String level : levels)
               {
                  try
                  {
                     jsonObjectToUse = jsonObjectToUse.optJSONObject(level);
                     if(jsonObjectToUse == null)
                     {
                        continue FIELDS_LOOP;
                     }
                  }
                  catch(Exception e)
                  {
                     continue FIELDS_LOOP;
                  }
               }
            }

            if(jsonObjectToUse.isNull(backendName))
            {
               record.setValue(fieldName, null);
               continue;
            }

            switch(metaData.getType())
            {
               case INTEGER -> record.setValue(fieldName, jsonObjectToUse.optInt(backendName));
               case LONG -> record.setValue(fieldName, jsonObjectToUse.optLong(backendName));
               case DECIMAL -> record.setValue(fieldName, jsonObjectToUse.optBigDecimal(backendName, null));
               case BOOLEAN -> record.setValue(fieldName, jsonObjectToUse.optBoolean(backendName));
               case DATE_TIME ->
               {
                  String dateTimeString = jsonObjectToUse.optString(backendName);
                  if(StringUtils.hasContent(dateTimeString))
                  {
                     Instant instant = ValueUtils.getValueAsInstant(dateTimeString);
                     record.setValue(fieldName, instant);
                  }
               }
               case DATE ->
               {
                  String dateString = jsonObjectToUse.optString(backendName);
                  if(StringUtils.hasContent(dateString))
                  {
                     LocalDate localDate = ValueUtils.getValueAsLocalDate(dateString);
                     record.setValue(fieldName, localDate);
                  }
               }
               default -> record.setValue(fieldName, jsonObjectToUse.optString(backendName));
            }
         }
         catch(Exception e)
         {
            LOG.debug("Caught exception parsing field [" + fieldName + "] as [" + originalBackendName + "]", e);
         }
      }

      return (record);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class NullKeyToEmptyStringSerializer extends StdSerializer<Object>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      public NullKeyToEmptyStringSerializer()
      {
         this(null);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      public NullKeyToEmptyStringSerializer(Class<Object> t)
      {
         super(t);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void serialize(Object nullKey, JsonGenerator jsonGenerator, SerializerProvider unused) throws IOException
      {
         jsonGenerator.writeFieldName("");
      }
   }

}
