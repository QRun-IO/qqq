/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.actions.tables.RecordSearchAction;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.RecordSearchExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordSearchMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RecordSearchResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordSearchResultV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 ** POST /search - record search across the tables that declare search fields.
 *******************************************************************************/
public class RecordSearchSpecV1 extends AbstractEndpointSpec<RecordSearchMiddlewareInput, RecordSearchResponseV1, RecordSearchExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/search")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Search records across tables")
         .withLongDescription("""
            Search for records whose search fields (declared per table in its meta-data, and listed as `searchFields` in the
            table meta-data) match a search term. String fields match when they contain the term, ignoring case; integer
            fields match when they equal a numeric term. Only tables that the user has permission to read are searched,
            record security locks apply, and each table returns at most `limitPerTable` records (default\s"""
            + RecordSearchAction.DEFAULT_LIMIT_PER_TABLE + ", maximum " + RecordSearchAction.MAX_LIMIT_PER_TABLE + ").");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("searchTerm", new Schema()
         .withDescription("Text to search for (required; at most " + RecordSearchAction.MAX_SEARCH_TERM_LENGTH + " characters after trimming).")
         .withType(Type.STRING)
         .withExample("Kelkhoff"));
      properties.put("tableNames", new Schema()
         .withDescription("Optional list of table names to search. When omitted or empty, every table with search fields is searched.")
         .withType(Type.ARRAY)
         .withItems(new Schema().withType(Type.STRING)));
      properties.put("limitPerTable", new Schema()
         .withDescription("Maximum number of records to return from each table.")
         .withType(Type.NUMBER));

      return new RequestBody()
         .withRequired(true)
         .withContent(Map.of(
            ContentType.APPLICATION_JSON.getMimeType(), new Content()
               .withSchema(new Schema()
                  .withType(Type.OBJECT)
                  .withProperties(properties))
         ));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RecordSearchMiddlewareInput buildInput(Context context) throws Exception
   {
      RecordSearchMiddlewareInput input       = new RecordSearchMiddlewareInput();
      JSONObject                  requestBody = getRequestBodyAsJsonObject(context);
      if(requestBody == null)
      {
         throw (new QBadRequestException("A JSON request body with a searchTerm is required."));
      }

      input.setSearchTerm(requestBody.optString("searchTerm", null));

      if(requestBody.has("tableNames"))
      {
         if(!(requestBody.get("tableNames") instanceof JSONArray tableNamesArray))
         {
            throw (new QBadRequestException("tableNames must be an array of table names."));
         }

         List<String> tableNames = new ArrayList<>();
         for(int i = 0; i < tableNamesArray.length(); i++)
         {
            tableNames.add(tableNamesArray.getString(i));
         }
         input.setTableNames(tableNames);
      }

      if(requestBody.has("limitPerTable"))
      {
         if(!(requestBody.get("limitPerTable") instanceof Number limitPerTable))
         {
            throw (new QBadRequestException("limitPerTable must be a number."));
         }
         input.setLimitPerTable(limitPerTable.intValue());
      }

      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(
         RecordSearchResponseV1.class.getSimpleName(), new RecordSearchResponseV1().toSchema(),
         RecordSearchResultV1.class.getSimpleName(), new RecordSearchResultV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("TODO", new Example()
         .withValue(new RecordSearchResponseV1().withResults(List.of(
            new RecordSearchResult().withTableName("person").withTableLabel("Person").withRecordId(1).withRecordLabel("Darin Kelkhoff"),
            new RecordSearchResult().withTableName("person").withTableLabel("Person").withRecordId(2).withRecordLabel("Tim Kelkhoff")
         ))));

      return new BasicResponse("""
         The records matching the search term""",
         RecordSearchResponseV1.class.getSimpleName(),
         examples
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, RecordSearchResponseV1 output) throws Exception
   {
      if(CollectionUtils.nullSafeIsEmpty(output.getResults()))
      {
         ///////////////////////////////////////////////////////////////////
         // return an empty list (not an empty object) when nothing matched //
         ///////////////////////////////////////////////////////////////////
         context.result(JsonUtils.toJsonCustomized(output, builder -> builder
            .serializationInclusion(JsonInclude.Include.ALWAYS)));
      }
      else
      {
         super.handleOutput(context, output);
      }
   }

}
