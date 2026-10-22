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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.actions.tables.picker.RelatedRecordPickerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.RelatedRecordPickerMiddlewareInput;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 ** Shared definitions for the relatedRecordPicker routes
 ** (docs/RELATED-RECORD-PICKER.md).
 *******************************************************************************/
public class RelatedRecordPickerSpecUtils
{
   public static final String PATH_PREFIX = "/table/{tableName}/relatedRecordPicker/{pickerName}";



   /*******************************************************************************
    ** Constructor (static utilities only).
    *******************************************************************************/
   private RelatedRecordPickerSpecUtils()
   {
   }



   /*******************************************************************************
    ** The tableName and pickerName path parameters.
    *******************************************************************************/
   public static List<Parameter> defineRequestParameters()
   {
      return List.of(
         new Parameter()
            .withName("tableName")
            .withDescription("Name of the table that declares the picker (e.g. the order).")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("sales_order")
            .withIn(In.PATH),
         new Parameter()
            .withName("pickerName")
            .withDescription("Name of the picker, from the table metadata's relatedRecordPickers.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("addSkus")
            .withIn(In.PATH)
      );
   }



   /*******************************************************************************
    ** The JSON request body of an operation.
    *******************************************************************************/
   public static RequestBody defineRequestBody(RelatedRecordPickerMiddlewareInput.Operation operation)
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("primaryKey", new Schema()
         .withDescription(operation == RelatedRecordPickerMiddlewareInput.Operation.QUERY
            ? "Primary key of the parent record (required for MULTI_CHILD and LINK; for SINGLE, the record being edited, if any). Read with the user's own security."
            : "Primary key of the parent record. Read with the user's own security.")
         .withType(Type.STRING));

      if(operation == RelatedRecordPickerMiddlewareInput.Operation.QUERY)
      {
         properties.put("values", new Schema()
            .withDescription("Current form values (SINGLE on a create form, when there is no primaryKey), for ${input.fieldName} variables in the picker's filters")
            .withType(Type.OBJECT));
         properties.put("searchTerm", new Schema()
            .withDescription("Case-insensitive text matched (contains) against the target table's search fields, or its record-label fields")
            .withType(Type.STRING));
         properties.put("filter", new Schema()
            .withDescription("The user's filter on the target table (criteria and orderBys on target fields only); ANDed with the picker's filter")
            .withRef("#/components/schemas/QueryFilter"));
         properties.put("skip", new Schema()
            .withDescription("Records to skip (offset paging)")
            .withType(Type.INTEGER));
         properties.put("limit", new Schema()
            .withDescription("Records per page, 1 to 200 (default 50)")
            .withType(Type.INTEGER));
      }
      else
      {
         properties.put("targetIds", new Schema()
            .withDescription("Primary keys of the picked target records")
            .withType(Type.ARRAY)
            .withItems(new Schema().withType(Type.STRING)));
      }

      if(operation == RelatedRecordPickerMiddlewareInput.Operation.ADD)
      {
         Map<String, Schema> recordProperties = new LinkedHashMap<>();
         recordProperties.put("values", new Schema()
            .withDescription("The row's values: the childTargetFieldName (which target) and the picker's reviewFieldNames; other values are ignored, and the parent and target keys always come from the server")
            .withType(Type.OBJECT));
         properties.put("records", new Schema()
            .withDescription("Reviewed child rows (instead of targetIds), e.g. the prepare route's rows after the user filled the review fields")
            .withType(Type.ARRAY)
            .withItems(new Schema().withType(Type.OBJECT).withProperties(recordProperties)));
      }

      return new RequestBody()
         .withContent(Map.of(ContentType.APPLICATION_JSON.getMimeType(), new Content()
            .withSchema(new Schema().withType(Type.OBJECT).withProperties(properties))));
   }



   /*******************************************************************************
    ** Build the core input from the path and the JSON body.
    *******************************************************************************/
   public static RelatedRecordPickerMiddlewareInput buildInput(RelatedRecordPickerMiddlewareInput.Operation operation, String tableName, String pickerName, JSONObject body) throws QBadRequestException
   {
      RelatedRecordPickerInput input = new RelatedRecordPickerInput(tableName, pickerName);
      if(body != null)
      {
         try
         {
            if(body.has("primaryKey") && !body.isNull("primaryKey"))
            {
               input.setPrimaryKey(toSerializable(body.get("primaryKey")));
            }
            if(body.has("values") && !body.isNull("values"))
            {
               input.setValues(toMap(body.getJSONObject("values")));
            }
            if(body.has("searchTerm") && !body.isNull("searchTerm"))
            {
               input.setSearchTerm(body.getString("searchTerm"));
            }
            if(body.has("filter") && !body.isNull("filter"))
            {
               input.setFilter(JsonUtils.toObject(body.getJSONObject("filter").toString(), QQueryFilter.class));
            }
            if(body.has("skip") && !body.isNull("skip"))
            {
               input.setSkip(body.getInt("skip"));
            }
            if(body.has("limit") && !body.isNull("limit"))
            {
               input.setLimit(body.getInt("limit"));
            }
            if(body.has("targetIds") && !body.isNull("targetIds"))
            {
               List<Serializable> targetIds = new ArrayList<>();
               JSONArray          array     = body.getJSONArray("targetIds");
               for(int i = 0; i < array.length(); i++)
               {
                  targetIds.add(toSerializable(array.get(i)));
               }
               input.setTargetIds(targetIds);
            }
            if(body.has("records") && !body.isNull("records"))
            {
               List<Map<String, Serializable>> records = new ArrayList<>();
               JSONArray                       array   = body.getJSONArray("records");
               for(int i = 0; i < array.length(); i++)
               {
                  JSONObject record = array.getJSONObject(i);
                  records.add(toMap(record.has("values") ? record.getJSONObject("values") : record));
               }
               input.setRecords(records);
            }
         }
         catch(Exception e)
         {
            throw (new QBadRequestException("Invalid request body: " + e.getMessage(), e));
         }
      }
      return (new RelatedRecordPickerMiddlewareInput().withOperation(operation).withPickerInput(input));
   }



   /*******************************************************************************
    ** A JSON object as a map (JSON null as null).
    *******************************************************************************/
   private static Map<String, Serializable> toMap(JSONObject object)
   {
      Map<String, Serializable> map = new LinkedHashMap<>();
      for(String key : object.keySet())
      {
         map.put(key, toSerializable(object.get(key)));
      }
      return (map);
   }



   /*******************************************************************************
    ** A JSON value as a serializable (JSON null as null, other values as text).
    *******************************************************************************/
   private static Serializable toSerializable(Object value)
   {
      if(value == null || JSONObject.NULL.equals(value))
      {
         return (null);
      }
      if(value instanceof Serializable serializable && !(value instanceof JSONObject) && !(value instanceof JSONArray))
      {
         return (serializable);
      }
      return (String.valueOf(value));
   }
}
