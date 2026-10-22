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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 ** A stable SHA-256 fingerprint of the persistent shape of a table, together
 ** with the descriptor (canonical JSON) it was computed from.
 **
 ** The descriptor covers only what affects stored data:
 ** - the table name, primary key field and backend type;
 ** - each field's name, type, isRequired, maxLength, possible-value-source
 **   name, isHeavy and isSensitive (sorted by field name);
 ** - the unique keys (field names sorted within a key, keys sorted) and
 **   associations (sorted by name).
 **
 ** Labels, adornments, help text and other cosmetic metadata are left out on
 ** purpose, so changing them does not change the fingerprint.  Field order
 ** does not matter either.
 **
 ** Use SchemaDiff to see what changed between two fingerprints.
 *******************************************************************************/
public final class TableSchemaFingerprint
{
   private static final ObjectMapper MAPPER = new ObjectMapper();

   private final String              descriptorJson;
   private final Map<String, Object> descriptor;
   private final String              hash;



   /*******************************************************************************
    ** Private constructor - use of(...) or fromDescriptorJson(...)
    *******************************************************************************/
   private TableSchemaFingerprint(String descriptorJson, Map<String, Object> descriptor)
   {
      this.descriptorJson = descriptorJson;
      this.descriptor = descriptor;
      this.hash = sha256Hex(descriptorJson);
   }



   /*******************************************************************************
    ** Fingerprint a table.  The backend type comes from the table's backend in
    ** the current QContext instance, when there is one - else from the table's
    ** backend details - else is empty.  Use the 2-arg overload to be explicit.
    *******************************************************************************/
   public static TableSchemaFingerprint of(QTableMetaData table)
   {
      return (of(table, resolveBackendType(table)));
   }



   /*******************************************************************************
    ** Fingerprint a table, with an explicit backend type.
    *******************************************************************************/
   public static TableSchemaFingerprint of(QTableMetaData table, String backendType)
   {
      Map<String, Object> descriptor = new TreeMap<>();
      descriptor.put("table", table.getName());
      descriptor.put("primaryKeyField", table.getPrimaryKeyField());
      descriptor.put("backendType", backendType == null ? "" : backendType);

      List<Map<String, Object>> fields = new ArrayList<>();
      if(table.getFields() != null)
      {
         for(QFieldMetaData field : table.getFields().values())
         {
            Map<String, Object> fieldMap = new TreeMap<>();
            fieldMap.put("name", field.getName());
            fieldMap.put("type", field.getType() == null ? null : field.getType().name());
            fieldMap.put("isRequired", field.getIsRequired());
            fieldMap.put("maxLength", field.getMaxLength());
            fieldMap.put("possibleValueSource", field.getPossibleValueSourceName());
            fieldMap.put("isHeavy", field.getIsHeavy());
            fieldMap.put("isSensitive", field.getIsSensitive());
            fields.add(fieldMap);
         }
      }
      fields.sort(Comparator.comparing(f -> String.valueOf(f.get("name"))));
      descriptor.put("fields", fields);

      List<List<String>> uniqueKeys = new ArrayList<>();
      if(table.getUniqueKeys() != null)
      {
         for(UniqueKey uniqueKey : table.getUniqueKeys())
         {
            List<String> names = new ArrayList<>(uniqueKey.getFieldNames());
            Collections.sort(names);
            uniqueKeys.add(names);
         }
      }
      uniqueKeys.sort(Comparator.comparing(Object::toString));
      descriptor.put("uniqueKeys", uniqueKeys);

      List<Map<String, Object>> associations = new ArrayList<>();
      if(table.getAssociations() != null)
      {
         for(Association association : table.getAssociations())
         {
            Map<String, Object> associationMap = new TreeMap<>();
            associationMap.put("name", association.getName());
            associationMap.put("associatedTable", association.getAssociatedTableName());
            associationMap.put("joinName", association.getJoinName());
            associations.add(associationMap);
         }
      }
      associations.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
      descriptor.put("associations", associations);

      return (fromDescriptorJson(toCanonicalJson(descriptor)));
   }



   /*******************************************************************************
    ** Rebuild a fingerprint from a stored descriptor (as returned by
    ** getDescriptorJson), e.g., one saved in a backup manifest.  The JSON is
    ** re-canonicalized, so the hash is the same as the original's.
    *******************************************************************************/
   public static TableSchemaFingerprint fromDescriptorJson(String json)
   {
      try
      {
         Map<String, Object> parsed    = MAPPER.readValue(json, new TypeReference<>() {});
         String              canonical = toCanonicalJson(parsed);
         Map<String, Object> reparsed  = MAPPER.readValue(canonical, new TypeReference<>() {});
         return (new TableSchemaFingerprint(canonical, reparsed));
      }
      catch(Exception e)
      {
         throw new QRuntimeException("Error reading schema descriptor: " + e.getMessage(), e);
      }
   }



   /*******************************************************************************
    ** The SHA-256 of the canonical descriptor JSON, as lower-case hex.
    *******************************************************************************/
   public String getHash()
   {
      return (hash);
   }



   /*******************************************************************************
    ** The canonical JSON the hash was computed from: sorted keys, no whitespace.
    *******************************************************************************/
   public String getDescriptorJson()
   {
      return (descriptorJson);
   }



   /*******************************************************************************
    ** The descriptor as maps and lists (a fresh copy on each call).
    *******************************************************************************/
   public Map<String, Object> getDescriptor()
   {
      try
      {
         return (MAPPER.readValue(descriptorJson, new TypeReference<>() {}));
      }
      catch(Exception e)
      {
         throw new QRuntimeException("Error reading schema descriptor: " + e.getMessage(), e);
      }
   }



   /*******************************************************************************
    ** Fingerprints are equal when their hashes are.
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      return (o instanceof TableSchemaFingerprint other && other.hash.equals(hash));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return (hash.hashCode());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return (hash);
   }



   /*******************************************************************************
    ** The parsed descriptor, without copying, for SchemaDiff (which only reads it).
    *******************************************************************************/
   Map<String, Object> descriptorNoCopy()
   {
      return (descriptor);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String resolveBackendType(QTableMetaData table)
   {
      try
      {
         QInstance qInstance = QContext.getQInstance();
         if(qInstance != null && table.getBackendName() != null)
         {
            QBackendMetaData backend = qInstance.getBackend(table.getBackendName());
            if(backend != null && backend.getBackendType() != null)
            {
               return (backend.getBackendType());
            }
         }
      }
      catch(Exception e)
      {
         ////////////////////////////////////////////////////////////
         // no usable context - fall through to the table's details //
         ////////////////////////////////////////////////////////////
      }

      if(table.getBackendDetails() != null && table.getBackendDetails().getBackendType() != null)
      {
         return (table.getBackendDetails().getBackendType());
      }
      return ("");
   }



   /*******************************************************************************
    ** Serialize maps (keys sorted), lists, strings, numbers, booleans and nulls,
    ** with no whitespace, so the text is identical however the input was built.
    *******************************************************************************/
   private static String toCanonicalJson(Object value)
   {
      StringBuilder sb = new StringBuilder();
      appendCanonical(sb, value);
      return (sb.toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void appendCanonical(StringBuilder sb, Object value)
   {
      try
      {
         if(value instanceof Map<?, ?> map)
         {
            sb.append('{');
            boolean first = true;
            for(Map.Entry<String, Object> entry : new TreeMap<>(castMap(map)).entrySet())
            {
               sb.append(first ? "" : ",");
               first = false;
               sb.append(MAPPER.writeValueAsString(entry.getKey())).append(':');
               appendCanonical(sb, entry.getValue());
            }
            sb.append('}');
         }
         else if(value instanceof List<?> list)
         {
            sb.append('[');
            boolean first = true;
            for(Object item : list)
            {
               sb.append(first ? "" : ",");
               first = false;
               appendCanonical(sb, item);
            }
            sb.append(']');
         }
         else
         {
            sb.append(MAPPER.writeValueAsString(value));
         }
      }
      catch(Exception e)
      {
         throw new QRuntimeException("Error building schema descriptor: " + e.getMessage(), e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static Map<String, Object> castMap(Map<?, ?> map)
   {
      return ((Map<String, Object>) map);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String sha256Hex(String s)
   {
      try
      {
         return (HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))));
      }
      catch(NoSuchAlgorithmException e)
      {
         throw new QRuntimeException("SHA-256 is not available", e);
      }
   }

}
