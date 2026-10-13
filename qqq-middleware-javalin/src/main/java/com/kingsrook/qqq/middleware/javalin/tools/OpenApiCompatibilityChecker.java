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

package com.kingsrook.qqq.middleware.javalin.tools;


import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.yaml.snakeyaml.LoaderOptions;


/*******************************************************************************
 ** Structural comparison of two OpenAPI documents (as parsed YAML/JSON maps),
 ** reporting the changes that would break an existing client of the older one.
 **
 ** Breaking (for a client built against OLD):
 ** - a path, or an operation (method) on a path, is removed;
 ** - a response status code of an operation is removed;
 ** - a parameter is removed, changes `in`, or becomes required (or a new
 **   required parameter is added);
 ** - a request body becomes required;
 ** - a component schema is removed;
 ** - in any schema: a property is removed, `type` or `format` changes, `$ref`
 **   changes, an enum value is removed, or a property becomes required.
 **
 ** Not breaking: added paths, operations, optional parameters, properties,
 ** schemas, enum values and response codes; descriptions, summaries, tags and
 ** examples.  Adding an enum value to a response can still surprise a strict
 ** client, so clients must treat unknown enum values as "other"
 ** (docs/UI-API-CLIENTS.md).
 *******************************************************************************/
public class OpenApiCompatibilityChecker
{
   private static final Set<String> HTTP_METHODS       = Set.of("get", "put", "post", "delete", "patch", "head", "options");
   private static final Set<String> DOCUMENTATION_KEYS = Set.of("description", "summary", "example", "examples", "tags");



   /*******************************************************************************
    ** Parse an OpenAPI YAML (or JSON) document into a map.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static Map<String, Object> parse(String yamlOrJson) throws JsonProcessingException
   {
      LoaderOptions loaderOptions = new LoaderOptions();
      loaderOptions.setCodePointLimit(100 * 1024 * 1024);
      ObjectMapper mapper = new ObjectMapper(YAMLFactory.builder().loaderOptions(loaderOptions).build());
      return (mapper.readValue(yamlOrJson, LinkedHashMap.class));
   }



   /*******************************************************************************
    ** The info.version of a parsed document (or null).
    *******************************************************************************/
   public static String getInfoVersion(Map<String, Object> document)
   {
      Object info = document.get("info");
      return (info instanceof Map<?, ?> infoMap && infoMap.get("version") != null ? String.valueOf(infoMap.get("version")) : null);
   }



   /*******************************************************************************
    ** Whether two documents are the same apart from documentation text
    ** (descriptions, summaries, examples) and info.
    *******************************************************************************/
   public static boolean isStructurallyEqual(Map<String, Object> a, Map<String, Object> b)
   {
      return (Objects.equals(stripDocumentation(a, true), stripDocumentation(b, true)));
   }



   /*******************************************************************************
    ** Compare semantic versions: negative, zero or positive as a < b, a == b,
    ** a > b.  Non-numeric parts sort as zero.
    *******************************************************************************/
   public static int compareSemver(String a, String b)
   {
      String[] partsA = (a == null ? "0" : a).split("[.-]");
      String[] partsB = (b == null ? "0" : b).split("[.-]");
      for(int i = 0; i < 3; i++)
      {
         int diff = Integer.compare(semverPart(partsA, i), semverPart(partsB, i));
         if(diff != 0)
         {
            return (diff);
         }
      }
      return (0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int semverPart(String[] parts, int index)
   {
      try
      {
         return (index < parts.length ? Integer.parseInt(parts[index].replaceAll("\\D", "")) : 0);
      }
      catch(NumberFormatException e)
      {
         return (0);
      }
   }



   /*******************************************************************************
    ** List the changes from OLD to NEW that would break a client of OLD.
    *******************************************************************************/
   public static List<String> findBreakingChanges(Map<String, Object> oldDocument, Map<String, Object> newDocument)
   {
      List<String> problems = new ArrayList<>();

      Map<String, Object> oldPaths = asMap(oldDocument.get("paths"));
      Map<String, Object> newPaths = asMap(newDocument.get("paths"));
      for(Map.Entry<String, Object> pathEntry : oldPaths.entrySet())
      {
         String path = pathEntry.getKey();
         if(!newPaths.containsKey(path))
         {
            problems.add("path removed: " + path);
            continue;
         }

         Map<String, Object> oldOperations = asMap(pathEntry.getValue());
         Map<String, Object> newOperations = asMap(newPaths.get(path));
         for(Map.Entry<String, Object> operationEntry : oldOperations.entrySet())
         {
            String method = operationEntry.getKey();
            if(!HTTP_METHODS.contains(method))
            {
               continue;
            }

            String where = method.toUpperCase() + " " + path;
            if(!newOperations.containsKey(method))
            {
               problems.add("operation removed: " + where);
               continue;
            }

            compareOperation(where, asMap(operationEntry.getValue()), asMap(newOperations.get(method)), problems);
         }
      }

      Map<String, Object> oldSchemas = asMap(asMap(oldDocument.get("components")).get("schemas"));
      Map<String, Object> newSchemas = asMap(asMap(newDocument.get("components")).get("schemas"));
      for(Map.Entry<String, Object> schemaEntry : oldSchemas.entrySet())
      {
         String where = "components.schemas." + schemaEntry.getKey();
         if(!newSchemas.containsKey(schemaEntry.getKey()))
         {
            problems.add("schema removed: " + where);
            continue;
         }
         compareSchema(where, asMap(schemaEntry.getValue()), asMap(newSchemas.get(schemaEntry.getKey())), problems);
      }

      return (problems);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void compareOperation(String where, Map<String, Object> oldOperation, Map<String, Object> newOperation, List<String> problems)
   {
      ////////////////
      // parameters //
      ////////////////
      Map<String, Map<String, Object>> oldParameters = parametersByKey(oldOperation.get("parameters"));
      Map<String, Map<String, Object>> newParameters = parametersByKey(newOperation.get("parameters"));
      for(Map.Entry<String, Map<String, Object>> entry : oldParameters.entrySet())
      {
         Map<String, Object> newParameter = newParameters.get(entry.getKey());
         if(newParameter == null)
         {
            problems.add(where + ": parameter removed (or moved): " + entry.getKey());
            continue;
         }
         if(!isTrue(entry.getValue().get("required")) && isTrue(newParameter.get("required")))
         {
            problems.add(where + ": parameter became required: " + entry.getKey());
         }
         compareSchema(where + " parameter " + entry.getKey(), asMap(entry.getValue().get("schema")), asMap(newParameter.get("schema")), problems);
      }
      for(Map.Entry<String, Map<String, Object>> entry : newParameters.entrySet())
      {
         if(!oldParameters.containsKey(entry.getKey()) && isTrue(entry.getValue().get("required")))
         {
            problems.add(where + ": new required parameter: " + entry.getKey());
         }
      }

      //////////////////
      // request body //
      //////////////////
      Map<String, Object> oldBody = asMap(oldOperation.get("requestBody"));
      Map<String, Object> newBody = asMap(newOperation.get("requestBody"));
      if(!isTrue(oldBody.get("required")) && isTrue(newBody.get("required")))
      {
         problems.add(where + ": request body became required");
      }
      compareContent(where + " requestBody", asMap(oldBody.get("content")), asMap(newBody.get("content")), problems);

      ///////////////
      // responses //
      ///////////////
      Map<String, Object> oldResponses = asMap(oldOperation.get("responses"));
      Map<String, Object> newResponses = asMap(newOperation.get("responses"));
      for(Map.Entry<String, Object> entry : oldResponses.entrySet())
      {
         if(!newResponses.containsKey(entry.getKey()))
         {
            problems.add(where + ": response removed: " + entry.getKey());
            continue;
         }
         compareContent(where + " response " + entry.getKey(), asMap(asMap(entry.getValue()).get("content")), asMap(asMap(newResponses.get(entry.getKey())).get("content")), problems);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void compareContent(String where, Map<String, Object> oldContent, Map<String, Object> newContent, List<String> problems)
   {
      for(Map.Entry<String, Object> entry : oldContent.entrySet())
      {
         if(!newContent.containsKey(entry.getKey()))
         {
            problems.add(where + ": content type removed: " + entry.getKey());
            continue;
         }
         compareSchema(where + " " + entry.getKey(), asMap(asMap(entry.getValue()).get("schema")), asMap(asMap(newContent.get(entry.getKey())).get("schema")), problems);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void compareSchema(String where, Map<String, Object> oldSchema, Map<String, Object> newSchema, List<String> problems)
   {
      if(oldSchema.isEmpty())
      {
         return;
      }

      for(String key : List.of("type", "format", "$ref"))
      {
         if(oldSchema.containsKey(key) && !Objects.equals(oldSchema.get(key), newSchema.get(key)))
         {
            problems.add(where + ": " + key + " changed from " + oldSchema.get(key) + " to " + newSchema.get(key));
         }
      }

      if(oldSchema.get("enum") instanceof Collection<?> oldEnum)
      {
         Collection<?> newEnum = newSchema.get("enum") instanceof Collection<?> c ? c : null;
         if(newEnum != null)
         {
            for(Object value : oldEnum)
            {
               if(!newEnum.contains(value))
               {
                  problems.add(where + ": enum value removed: " + value);
               }
            }
         }
      }

      Collection<?> oldRequired = oldSchema.get("required") instanceof Collection<?> c ? c : List.of();
      Collection<?> newRequired = newSchema.get("required") instanceof Collection<?> c ? c : List.of();
      for(Object field : newRequired)
      {
         if(!oldRequired.contains(field))
         {
            problems.add(where + ": property became required: " + field);
         }
      }

      Map<String, Object> oldProperties = asMap(oldSchema.get("properties"));
      Map<String, Object> newProperties = asMap(newSchema.get("properties"));
      for(Map.Entry<String, Object> entry : oldProperties.entrySet())
      {
         if(!newProperties.containsKey(entry.getKey()))
         {
            problems.add(where + ": property removed: " + entry.getKey());
            continue;
         }
         compareSchema(where + "." + entry.getKey(), asMap(entry.getValue()), asMap(newProperties.get(entry.getKey())), problems);
      }

      compareSchema(where + "[]", asMap(oldSchema.get("items")), asMap(newSchema.get("items")), problems);
      if(oldSchema.get("additionalProperties") instanceof Map<?, ?>)
      {
         compareSchema(where + "{}", asMap(oldSchema.get("additionalProperties")), asMap(newSchema.get("additionalProperties")), problems);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Map<String, Object>> parametersByKey(Object parameters)
   {
      Map<String, Map<String, Object>> rs = new LinkedHashMap<>();
      if(parameters instanceof Collection<?> list)
      {
         for(Object parameter : list)
         {
            Map<String, Object> map = asMap(parameter);
            rs.put(map.get("in") + ":" + map.get("name"), map);
         }
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Object stripDocumentation(Object node, boolean root)
   {
      return (stripDocumentation(node, root ? "" : null));
   }



   /*******************************************************************************
    ** parentKey is "" at the root, and the key holding this node otherwise - so
    ** that a property *named* description (under "properties") is kept.
    *******************************************************************************/
   private static Object stripDocumentation(Object node, String parentKey)
   {
      if(node instanceof Map<?, ?> map)
      {
         boolean             isNameMap = "properties".equals(parentKey) || "schemas".equals(parentKey) || "paths".equals(parentKey);
         Map<Object, Object> rs        = new LinkedHashMap<>();
         for(Map.Entry<?, ?> entry : map.entrySet())
         {
            String key = String.valueOf(entry.getKey());
            if(!isNameMap && (DOCUMENTATION_KEYS.contains(key) || ("".equals(parentKey) && key.equals("info"))))
            {
               continue;
            }
            rs.put(entry.getKey(), stripDocumentation(entry.getValue(), key));
         }
         return (rs);
      }
      else if(node instanceof Collection<?> collection)
      {
         return (collection.stream().map(o -> stripDocumentation(o, "[]")).toList());
      }
      return (node);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static Map<String, Object> asMap(Object o)
   {
      return (o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean isTrue(Object o)
   {
      return (Boolean.TRUE.equals(o) || "true".equals(o));
   }
}
