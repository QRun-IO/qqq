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

package com.kingsrook.qqq.backend.core.actions.ai.tools;


import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;


/*******************************************************************************
 ** Typed, validating access to the (untrusted) input object of an AI tool call.
 ** Every accessor throws an INVALID_INPUT {@link AiToolException} for a value of
 ** the wrong type, so tools never see unchecked input.
 *******************************************************************************/
public class AiToolInput
{
   public static final int MAX_STRING_LENGTH = 1000;

   private final Map<String, Object> values;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AiToolInput(Map<String, Object> values)
   {
      this.values = values == null ? new LinkedHashMap<>() : values;
   }



   /*******************************************************************************
    ** Reject any keys outside the allowed set (the schemas are additionalProperties
    ** = false, and the kernel enforces that, rather than trusting the model).
    *******************************************************************************/
   public void requireOnlyKeys(Collection<String> allowedKeys) throws AiToolException
   {
      TreeSet<String> unknownKeys = new TreeSet<>();
      for(String key : values.keySet())
      {
         if(!allowedKeys.contains(key))
         {
            unknownKeys.add(String.valueOf(key));
         }
      }

      if(!unknownKeys.isEmpty())
      {
         throw (AiToolException.invalidInput("Unknown input properties: " + String.join(", ", unknownKeys) + ".  Allowed properties are: " + String.join(", ", new TreeSet<>(allowedKeys)) + "."));
      }
   }



   /*******************************************************************************
    ** A required, non-blank string.
    *******************************************************************************/
   public String getRequiredString(String key) throws AiToolException
   {
      String value = getOptionalString(key);
      if(value == null || value.isBlank())
      {
         throw (AiToolException.invalidInput("The input property '" + key + "' is required."));
      }
      return (value);
   }



   /*******************************************************************************
    ** An optional string (numbers are accepted, as their string form).
    *******************************************************************************/
   public String getOptionalString(String key) throws AiToolException
   {
      return (asString(values.get(key), key));
   }



   /*******************************************************************************
    ** An optional integer.
    *******************************************************************************/
   public Integer getOptionalInteger(String key) throws AiToolException
   {
      Object value = values.get(key);
      if(value == null)
      {
         return (null);
      }

      if(value instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue()) && Math.abs(number.doubleValue()) <= Integer.MAX_VALUE)
      {
         return (number.intValue());
      }

      throw (AiToolException.invalidInput("The input property '" + key + "' must be an integer."));
   }



   /*******************************************************************************
    ** An optional list of strings.
    *******************************************************************************/
   public List<String> getOptionalStringList(String key) throws AiToolException
   {
      List<Object> list = getOptionalList(key);
      if(list == null)
      {
         return (null);
      }

      List<String> rs = new ArrayList<>();
      for(Object element : list)
      {
         String string = asString(element, key);
         if(string == null)
         {
            throw (AiToolException.invalidInput("The input property '" + key + "' must be a list of strings."));
         }
         rs.add(string);
      }
      return (rs);
   }



   /*******************************************************************************
    ** An optional list.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public List<Object> getOptionalList(String key) throws AiToolException
   {
      Object value = values.get(key);
      if(value == null)
      {
         return (null);
      }

      if(value instanceof List<?> list)
      {
         return ((List<Object>) list);
      }

      throw (AiToolException.invalidInput("The input property '" + key + "' must be a list."));
   }



   /*******************************************************************************
    ** An optional object (map).
    *******************************************************************************/
   public Map<String, Object> getOptionalMap(String key) throws AiToolException
   {
      return (asMap(values.get(key), key));
   }



   /*******************************************************************************
    ** Whether the input has a (non-null) value for a key.
    *******************************************************************************/
   public boolean has(String key)
   {
      return (values.get(key) != null);
   }



   /*******************************************************************************
    ** A value as a string - accepting strings, numbers and booleans.
    *******************************************************************************/
   public static String asString(Object value, String key) throws AiToolException
   {
      if(value == null)
      {
         return (null);
      }

      if(value instanceof String || value instanceof Number || value instanceof Boolean)
      {
         String string = String.valueOf(value);
         if(string.length() > MAX_STRING_LENGTH)
         {
            throw (AiToolException.invalidInput("The input property '" + key + "' is too long."));
         }
         return (string);
      }

      throw (AiToolException.invalidInput("The input property '" + key + "' must be a string."));
   }



   /*******************************************************************************
    ** A value as an object (a map with string keys).
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static Map<String, Object> asMap(Object value, String key) throws AiToolException
   {
      if(value == null)
      {
         return (null);
      }

      if(value instanceof Map<?, ?> map)
      {
         for(Object mapKey : map.keySet())
         {
            if(!(mapKey instanceof String))
            {
               throw (AiToolException.invalidInput("The input property '" + key + "' must be an object."));
            }
         }
         return ((Map<String, Object>) map);
      }

      throw (AiToolException.invalidInput("The input property '" + key + "' must be an object."));
   }
}
