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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.actions.tables.RecordSearchAction;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchDetail;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchHighlight;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchHighlightMatch;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchProviderState;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchProviderStatus;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchSort;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchTableSummary;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.RecordSearchExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordSearchMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RecordSearchResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordSearchDetailV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordSearchHighlightMatchV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordSearchHighlightV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordSearchResultV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordSearchTableSummaryV1;
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
            + RecordSearchAction.DEFAULT_LIMIT_PER_TABLE + ", maximum " + RecordSearchAction.MAX_LIMIT_PER_TABLE + """
            ), after skipping `offset` matches in each table (for paging one table).  An instance may have a record search
            provider (e.g., a search index) that searches some tables instead; if it fails, those tables are searched with
            the default search, and the response's `provider` and `tables` say so.

            Each result can carry plain-text `highlights` of the fields that match (with the matched spans as character
            offsets - never markup), `details` (other search field values), `updatedAt` and, from a provider, a `score`.
            Only fields the user may see are used.  All of these, and `tables` and `provider`, are optional: clients must
            tolerate their absence.""");
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
      properties.put("offset", new Schema()
         .withDescription("Number of matches to skip in each table, for paging (default 0, maximum " + RecordSearchAction.MAX_OFFSET + ").  Mainly useful with one table in tableNames.")
         .withType(Type.NUMBER));
      properties.put("sort", new Schema()
         .withDescription("Order of the results within each table: RELEVANCE (default) or RECENT (most recently modified first, for tables with a last-modified field).")
         .withType(Type.STRING)
         .withEnumValues(Arrays.stream(RecordSearchSort.values()).map(String::valueOf).toList()));

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

      if(requestBody.has("offset") && !requestBody.isNull("offset"))
      {
         if(!(requestBody.get("offset") instanceof Number offset))
         {
            throw (new QBadRequestException("offset must be a number."));
         }
         input.setOffset(offset.intValue());
      }

      if(requestBody.has("sort") && !requestBody.isNull("sort"))
      {
         if(!(requestBody.get("sort") instanceof String sort))
         {
            throw (new QBadRequestException("sort must be one of " + Arrays.toString(RecordSearchSort.values()) + "."));
         }

         try
         {
            input.setSort(RecordSearchSort.valueOf(sort.trim().toUpperCase(Locale.ROOT)));
         }
         catch(IllegalArgumentException e)
         {
            throw (new QBadRequestException("sort must be one of " + Arrays.toString(RecordSearchSort.values()) + "."));
         }
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
         RecordSearchResultV1.class.getSimpleName(), new RecordSearchResultV1().toSchema(),
         RecordSearchHighlightV1.class.getSimpleName(), new RecordSearchHighlightV1().toSchema(),
         RecordSearchHighlightMatchV1.class.getSimpleName(), new RecordSearchHighlightMatchV1().toSchema(),
         RecordSearchDetailV1.class.getSimpleName(), new RecordSearchDetailV1().toSchema(),
         RecordSearchTableSummaryV1.class.getSimpleName(), new RecordSearchTableSummaryV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("Matches in one table", new Example()
         .withValue(new RecordSearchResponseV1()
            .withResults(List.of(
               new RecordSearchResult().withTableName("person").withTableLabel("Person").withRecordId(1).withRecordLabel("Darin Kelkhoff")
                  .withScore(7.25)
                  .withUpdatedAt(Instant.parse("2026-10-01T14:30:00Z"))
                  .withHighlights(List.of(new RecordSearchHighlight().withFieldName("email").withFieldLabel("Email").withText("darin.kelkhoff@example.com")
                     .withMatches(List.of(new RecordSearchHighlightMatch().withStart(6).withEnd(14)))))
                  .withDetails(List.of(new RecordSearchDetail().withFieldName("city").withFieldLabel("City").withDisplayValue("Saint Louis"))),
               new RecordSearchResult().withTableName("person").withTableLabel("Person").withRecordId(2).withRecordLabel("Tim Kelkhoff").withScore(6.5)))
            .withTables(List.of(new RecordSearchTableSummary().withTableName("person").withTableLabel("Person").withResultCount(2).withHasMore(true).withTotalCount(14).withSource(RecordSearchSource.PROVIDER)))
            .withProviderStatus(new RecordSearchProviderStatus().withStatus(RecordSearchProviderState.OK))));

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
