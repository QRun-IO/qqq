/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 ** Shared utilities for possible values spec definitions.
 *******************************************************************************/
public class PossibleValuesSpecUtils
{

   /***************************************************************************
    ** Build the shared request body schema used by all possible values endpoints.
    ***************************************************************************/
   public static RequestBody defineRequestBody()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();

      properties.put("searchTerm", new Schema()
         .withDescription("Text to search for within possible value labels")
         .withType(Type.STRING));

      properties.put("ids", new Schema()
         .withDescription("List of specific ids to look up")
         .withType(Type.ARRAY)
         .withItems(new Schema().withType(Type.STRING)));

      properties.put("values", new Schema()
         .withDescription("Map of other field values, used for filter interpolation on possible value source filters")
         .withType(Type.OBJECT));

      properties.put("useCase", new Schema()
         .withDescription("Use case for possible value search filtering. Controls behavior when filter values are missing.")
         .withType(Type.STRING)
         .withEnumValues(List.of("FORM", "FILTER")));

      properties.put("labels", new Schema()
         .withDescription("List of specific labels to look up")
         .withType(Type.ARRAY)
         .withItems(new Schema().withType(Type.STRING)));

      properties.put("processValues", new Schema()
         .withDescription("Map of field values from the current process step, used for filter interpolation")
         .withType(Type.OBJECT));

      properties.put("filter", new Schema()
         .withDescription("Optional default filter to apply to the possible value search")
         .withRef("#/components/schemas/QueryFilter"));

      return new RequestBody()
         .withContent(Map.of(
            ContentType.APPLICATION_JSON.getMimeType(), new Content()
               .withSchema(new Schema()
                  .withType(Type.OBJECT)
                  .withProperties(properties))
         ));
   }



   /***************************************************************************
    ** Extract a string field from the JSON request body.
    ***************************************************************************/
   public static String extractStringField(JSONObject requestBody, String fieldName)
   {
      if(requestBody != null && requestBody.has(fieldName) && !requestBody.isNull(fieldName))
      {
         return requestBody.getString(fieldName);
      }
      return (null);
   }



   /***************************************************************************
    ** Extract the id list from the JSON request body.  Accepts an array of
    ** strings or numbers, or a comma-separated string (like the legacy query
    ** parameter).
    ***************************************************************************/
   public static List<String> extractIdList(JSONObject requestBody)
   {
      return (extractStringList(requestBody, "ids"));
   }



   /***************************************************************************
    ** Extract the label list from the JSON request body (an array, or a
    ** comma-separated string).
    ***************************************************************************/
   public static List<String> extractLabelList(JSONObject requestBody)
   {
      return (extractStringList(requestBody, "labels"));
   }



   /***************************************************************************
    ** Read a list of strings from an array (of strings or numbers) or from a
    ** comma-separated string.
    ***************************************************************************/
   private static List<String> extractStringList(JSONObject requestBody, String fieldName)
   {
      if(requestBody == null || !requestBody.has(fieldName) || requestBody.isNull(fieldName))
      {
         return (null);
      }

      List<String> list  = new ArrayList<>();
      Object       value = requestBody.get(fieldName);
      if(value instanceof JSONArray array)
      {
         for(int i = 0; i < array.length(); i++)
         {
            if(!array.isNull(i))
            {
               list.add(String.valueOf(array.get(i)));
            }
         }
      }
      else if(StringUtils.hasContent(String.valueOf(value)))
      {
         list.addAll(Arrays.asList(String.valueOf(value).split(",")));
      }
      return (list);
   }



   /***************************************************************************
    ** Extract other values map from the JSON request body.
    ***************************************************************************/
   public static Map<String, Serializable> extractOtherValues(JSONObject requestBody)
   {
      if(requestBody != null && requestBody.has("values") && !requestBody.isNull("values"))
      {
         JSONObject valuesObject = requestBody.getJSONObject("values");
         Map<String, Serializable> otherValues = new LinkedHashMap<>();
         for(String key : valuesObject.keySet())
         {
            Object value = valuesObject.get(key);
            if(value instanceof Serializable s)
            {
               otherValues.put(key, s);
            }
         }
         return otherValues;
      }
      return (null);
   }



   /***************************************************************************
    ** Extract process values map from the JSON request body.
    ***************************************************************************/
   public static Map<String, Serializable> extractProcessValues(JSONObject requestBody)
   {
      if(requestBody != null && requestBody.has("processValues") && !requestBody.isNull("processValues"))
      {
         JSONObject processValuesObject = requestBody.getJSONObject("processValues");
         Map<String, Serializable> processValues = new LinkedHashMap<>();
         for(String key : processValuesObject.keySet())
         {
            Object value = processValuesObject.get(key);
            if(value instanceof Serializable s)
            {
               processValues.put(key, s);
            }
         }
         return processValues;
      }
      return (null);
   }

}
