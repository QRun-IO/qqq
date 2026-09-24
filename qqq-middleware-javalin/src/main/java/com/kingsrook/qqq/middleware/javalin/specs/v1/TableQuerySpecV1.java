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


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.TableQueryExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableQueryInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.TableQueryResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.QuerySpecUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;
import org.json.JSONObject;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableQuerySpecV1 extends AbstractEndpointSpec<TableQueryInput, TableQueryResponseV1, TableQueryExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/query")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Query for records from a table")
         .withLongDescription("""
            Execute a query against a table, returning records that match a filter."""
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
            .withDescription("Name of the table to query.")
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
      return (QuerySpecUtils.defineQueryOrCountRequestBody());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public TableQueryInput buildInput(Context context) throws Exception
   {
      TableQueryInput input = new TableQueryInput();
      input.setTableName(getRequestParam(context, "tableName"));

      JSONObject requestBody = getRequestBodyAsJsonObject(context);
      if(requestBody != null)
      {
         input.setFilter(QuerySpecUtils.getFilterFromRequestBody(requestBody));
         input.setJoins(QuerySpecUtils.getJoinsFromRequestBody(requestBody));
         input.setTableVariant(QuerySpecUtils.getTableVariantFromRequestBody(requestBody));
      }

      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(TableQueryResponseV1.class.getSimpleName(), new TableQueryResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("TODO", new Example()
         .withValue(new TableQueryResponseV1().withRecords(List.of(new QRecord()
            .withRecordLabel("Item 17")
            .withTableName("item")
            .withValue("id", 17).withValue("quantity", 1000).withValue("storeId", 42)
            .withDisplayValue("id", "17").withDisplayValue("quantity", "1,000").withDisplayValue("storeId", "QQQ-Mart")
         ))));

      return new BasicResponse("""
         The records matching query""",
         TableQueryResponseV1.class.getSimpleName(),
         examples
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, TableQueryResponseV1 tableQueryResponseV1) throws Exception
   {
      if(CollectionUtils.nullSafeIsEmpty(tableQueryResponseV1.getRecords()))
      {
         ////////////////////////////////////////////////////////////////////////////////////
         // special case here, where we want an empty list to be returned for the case     //
         // with no records found by default our serialization doesn't include empty lists //
         ////////////////////////////////////////////////////////////////////////////////////
         context.result(JsonUtils.toJsonCustomized(tableQueryResponseV1, builder -> builder
            .serializationInclusion(JsonInclude.Include.ALWAYS)));
      }
      else
      {
         super.handleOutput(context, tableQueryResponseV1);
      }
   }
}
