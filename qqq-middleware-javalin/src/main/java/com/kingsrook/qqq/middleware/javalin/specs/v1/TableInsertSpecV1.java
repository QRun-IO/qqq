/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.executors.TableInsertExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableInsertInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.TableInsertResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.json.JSONObject;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableInsertSpecV1 extends AbstractEndpointSpec<TableInsertInput, TableInsertResponseV1, TableInsertExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Insert a record into a table")
         .withLongDescription("""
            Insert a new record into a table. The request body should contain the field values for the new record."""
         );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return List.of(
         new Parameter()
            .withName("tableName")
            .withDescription("Name of the table to insert into.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person")
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("fieldName", new Schema()
         .withDescription("Value for a field in the record. Repeat for each field to set.")
         .withType(Type.STRING));

      Schema bodySchema = new Schema()
         .withType(Type.OBJECT)
         .withDescription("JSON object with field names as keys and field values as values.")
         .withProperties(properties);

      return new RequestBody()
         .withContent(Map.of(ContentType.APPLICATION_JSON.getMimeType(), new Content()
            .withSchema(bodySchema)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public TableInsertInput buildInput(Context context) throws Exception
   {
      TableInsertInput input = new TableInsertInput();
      input.setTableName(getRequestParam(context, "tableName"));

      JSONObject requestBody = getRequestBodyAsJsonObject(context);
      if(requestBody != null)
      {
         Map<String, Serializable> recordValues = new LinkedHashMap<>();
         for(String key : requestBody.keySet())
         {
            Object value = requestBody.get(key);
            if(JSONObject.NULL.equals(value) || "".equals(value))
            {
               recordValues.put(key, null);
            }
            else if(value instanceof Serializable s)
            {
               recordValues.put(key, s);
            }
         }
         input.setRecordValues(recordValues);
      }

      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(TableInsertResponseV1.class.getSimpleName(), new TableInsertResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("TODO", new Example()
         .withValue(new TableInsertResponseV1().withRecord(new QRecord()
            .withRecordLabel("New Person")
            .withTableName("person")
            .withValue("id", 47).withValue("firstName", "New").withValue("lastName", "Person")
            .withDisplayValue("id", "47").withDisplayValue("firstName", "New").withDisplayValue("lastName", "Person")
         )));

      return new BasicResponse("""
         The record that was inserted""",
         TableInsertResponseV1.class.getSimpleName(),
         examples
      );
   }

}
