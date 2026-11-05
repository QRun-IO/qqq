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

package com.kingsrook.qqq.backend.core.utils;


import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Bounded pure-data copies and typed canonical fingerprints for durable intents.
 ** No arbitrary object serialization, reflection, callbacks, or host-code values.
 *******************************************************************************/
public final class BoundedIntentData
{
   private static final int MAX_NODES = 20000;
   private static final int MAX_BYTES = 1048576;
   private static final Set<Class<?>> NUMBERS = Set.of(Byte.class, Short.class, Integer.class, Long.class,
      Float.class, Double.class, BigDecimal.class, BigInteger.class);
   private int nodes;
   private int bytes;
   private final IdentityHashMap<Object, Boolean> ancestors = new IdentityHashMap<>();



   /*******************************************************************************
    ** A separate budget for each complete copied or fingerprinted intent.
    *******************************************************************************/
   private BoundedIntentData() {}



   /*******************************************************************************
    ** Immutable JSON parameters. Native dates and binary data belong in explicit
    ** application encodings, not arbitrary Java serializer behavior.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static Map<String, Serializable> copyParameters(Map<String, ? extends Serializable> values) throws QException
   {
      if(values == null)
      {
         throw (new QException("Durable intent parameters must be a JSON object"));
      }
      return ((Map<String, Serializable>) new BoundedIntentData().copy(values, false, 0));
   }



   /*******************************************************************************
    ** Canonical typed hash preserves dates, decimals, nulls and binary identity.
    ** The immutable result is safe to retain without persisting raw record fields.
    *******************************************************************************/
   public static String fingerprint(Object value) throws QException
   {
      Serializable snapshot = new BoundedIntentData().copy(value, true, 0);
      return (sha256(JsonUtils.toJson(snapshot).getBytes(StandardCharsets.UTF_8)));
   }



   /*******************************************************************************
    ** Digest already bounded data without introducing a storage dependency.
    *******************************************************************************/
   private static String sha256(byte[] value)
   {
      try
      {
         return (HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)));
      }
      catch(NoSuchAlgorithmException error)
      {
         throw (new IllegalStateException("Required SHA-256 digest unavailable", error));
      }
   }



   /*******************************************************************************
    ** Traverse only supported data types; reject cycles, excess depth and budgets
    ** before encoding or hashing. Map key ordering is independent of insertion.
    *******************************************************************************/
   private Serializable copy(Object value, boolean typed, int depth) throws QException
   {
      if(++nodes > MAX_NODES || depth > 16)
      {
         throw (new QException("Durable intent exceeds node or nesting limit"));
      }
      if(value == null)
      {
         return (typed ? "null" : null);
      }
      Class<?> type = value.getClass();
      if(type == String.class)
      {
         String text = (String) value;
         if(text.length() > 65536)
         {
            throw (new QException("Durable intent text is too long"));
         }
         charge(text.length() * 3);
         return (typed ? new ArrayList<>(List.of("string", text)) : text);
      }
      if(type == Boolean.class)
      {
         return (typed ? new ArrayList<>(List.of("boolean", value)) : (Boolean) value);
      }
      if(NUMBERS.contains(type))
      {
         if(value instanceof Double doubleValue && !Double.isFinite(doubleValue) || value instanceof Float floatValue && !Float.isFinite(floatValue)
            || value instanceof BigDecimal decimalValue && (decimalValue.precision() > 128 || Math.abs((long) decimalValue.scale()) > 1024)
            || value instanceof BigInteger integerValue && integerValue.bitLength() > 512)
         {
            throw (new QException("Durable intent number is nonfinite or too large"));
         }
         String number = value instanceof BigDecimal decimal ? decimal.stripTrailingZeros().toPlainString() : value.toString();
         charge(number.length());
         return (typed ? new ArrayList<>(List.of(type.getName(), number)) : (Serializable) value);
      }
      if(typed && type == byte[].class)
      {
         byte[] binary = (byte[]) value;
         charge(binary.length);
         return (new ArrayList<>(List.of("binary", binary.length, sha256(binary))));
      }
      if(typed && Set.of(Instant.class, LocalDate.class, LocalDateTime.class, LocalTime.class, OffsetDateTime.class, ZonedDateTime.class, UUID.class).contains(type))
      {
         String text = value.toString();
         charge(text.length());
         return (new ArrayList<>(List.of(type.getName(), text)));
      }
      if(typed && Set.of(Date.class, java.sql.Date.class, java.sql.Time.class, java.sql.Timestamp.class).contains(type))
      {
         String text = value instanceof java.sql.Timestamp timestamp ? timestamp.toInstant().toString() : Long.toString(((Date) value).getTime());
         charge(text.length());
         return (new ArrayList<>(List.of(type.getName(), text)));
      }
      if(!(value instanceof Map<?, ?>) && !(value instanceof List<?>))
      {
         throw (new QException("Durable intent contains an unsupported host value"));
      }
      if(ancestors.put(value, Boolean.TRUE) != null)
      {
         throw (new QException("Durable intent contains a cycle"));
      }
      try
      {
         if(value instanceof Map<?, ?> map)
         {
            if(map.size() > MAX_NODES)
            {
               throw (new QException("Durable intent map is too large"));
            }
            Map<String, Serializable> result = new TreeMap<>();
            for(Map.Entry<?, ?> entry : map.entrySet())
            {
               if(!(entry.getKey() instanceof String key) || key.length() > 1024)
               {
                  throw (new QException("Durable intent keys must be bounded strings"));
               }
               charge(key.length() * 3);
               result.put(key, copy(entry.getValue(), typed, depth + 1));
            }
            Serializable immutable = (Serializable) Collections.unmodifiableMap(result);
            return (typed ? new ArrayList<>(List.of("map", immutable)) : immutable);
         }
         List<?> list = (List<?>) value;
         if(list.size() > MAX_NODES)
         {
            throw (new QException("Durable intent list is too large"));
         }
         List<Serializable> result = new ArrayList<>();
         for(Object item : list)
         {
            result.add(copy(item, typed, depth + 1));
         }
         Serializable immutable = (Serializable) Collections.unmodifiableList(result);
         return (typed ? new ArrayList<>(List.of("list", immutable)) : immutable);
      }
      finally
      {
         ancestors.remove(value);
      }
   }



   /*******************************************************************************
    ** Aggregate byte/text budget is charged before allocation-heavy encoding.
    *******************************************************************************/
   private void charge(int count) throws QException
   {
      if(count < 0 || count > MAX_BYTES - bytes)
      {
         throw (new QException("Durable intent exceeds its data budget"));
      }
      bytes += count;
   }
}
