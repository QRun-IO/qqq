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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** Small builders for the strict JSON Schemas of AI tool inputs.  All objects
 ** are additionalProperties=false with an explicit required list, and every
 ** map is insertion-ordered, so the serialized schemas are byte-stable.
 *******************************************************************************/
public class AiJsonSchema
{

   /*******************************************************************************
    ** An object schema, with properties (in order) and the required names.
    *******************************************************************************/
   public static Map<String, Object> object(Map<String, Object> properties, List<String> required)
   {
      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("type", "object");
      schema.put("additionalProperties", false);
      schema.put("properties", properties == null ? new LinkedHashMap<>() : properties);
      schema.put("required", required == null ? new ArrayList<>() : required);
      return (schema);
   }



   /*******************************************************************************
    ** An ordered properties map - from alternating name, schema arguments.
    *******************************************************************************/
   public static Map<String, Object> properties(Object... nameSchemaPairs)
   {
      Map<String, Object> properties = new LinkedHashMap<>();
      for(int i = 0; i + 1 < nameSchemaPairs.length; i += 2)
      {
         properties.put(String.valueOf(nameSchemaPairs[i]), nameSchemaPairs[i + 1]);
      }
      return (properties);
   }



   /*******************************************************************************
    ** A string schema with a description.
    *******************************************************************************/
   public static Map<String, Object> string(String description)
   {
      return (typed("string", description));
   }



   /*******************************************************************************
    ** An integer schema with a description.
    *******************************************************************************/
   public static Map<String, Object> integer(String description)
   {
      return (typed("integer", description));
   }



   /*******************************************************************************
    ** A boolean schema with a description.
    *******************************************************************************/
   public static Map<String, Object> bool(String description)
   {
      return (typed("boolean", description));
   }



   /*******************************************************************************
    ** An array schema with an item schema and a description.
    *******************************************************************************/
   public static Map<String, Object> array(Map<String, Object> items, String description)
   {
      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("type", "array");
      schema.put("items", items);
      if(description != null)
      {
         schema.put("description", description);
      }
      return (schema);
   }



   /*******************************************************************************
    ** An enum schema (of string values).
    *******************************************************************************/
   public static Map<String, Object> enumOf(List<String> values, String description)
   {
      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("type", "string");
      schema.put("enum", Collections.unmodifiableList(new ArrayList<>(values)));
      if(description != null)
      {
         schema.put("description", description);
      }
      return (schema);
   }



   /*******************************************************************************
    ** A reference to a definition under the root's $defs.
    *******************************************************************************/
   public static Map<String, Object> ref(String definitionName)
   {
      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("$ref", "#/$defs/" + definitionName);
      return (schema);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Object> typed(String type, String description)
   {
      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("type", type);
      if(description != null)
      {
         schema.put("description", description);
      }
      return (schema);
   }
}
