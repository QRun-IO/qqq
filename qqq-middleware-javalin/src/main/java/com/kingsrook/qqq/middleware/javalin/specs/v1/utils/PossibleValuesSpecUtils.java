/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    ** Extract the id list from the JSON request body.
    ***************************************************************************/
   public static List<String> extractIdList(JSONObject requestBody)
   {
      if(requestBody != null && requestBody.has("ids") && !requestBody.isNull("ids"))
      {
         JSONArray idsArray = requestBody.getJSONArray("ids");
         List<String> idList = new ArrayList<>();
         for(int i = 0; i < idsArray.length(); i++)
         {
            idList.add(idsArray.getString(i));
         }
         return idList;
      }
      return (null);
   }



   /***************************************************************************
    ** Extract the label list from the JSON request body.
    ***************************************************************************/
   public static List<String> extractLabelList(JSONObject requestBody)
   {
      if(requestBody != null && requestBody.has("labels") && !requestBody.isNull("labels"))
      {
         JSONArray labelsArray = requestBody.getJSONArray("labels");
         List<String> labelList = new ArrayList<>();
         for(int i = 0; i < labelsArray.length(); i++)
         {
            labelList.add(labelsArray.getString(i));
         }
         return labelList;
      }
      return (null);
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
