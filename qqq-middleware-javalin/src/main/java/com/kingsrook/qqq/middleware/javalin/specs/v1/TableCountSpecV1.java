/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.executors.TableCountExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableCountInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.TableCountResponseV1;
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
public class TableCountSpecV1 extends AbstractEndpointSpec<TableCountInput, TableCountResponseV1, TableCountExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/count")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Count records in a table")
         .withLongDescription("""
            Execute a query against a table, returning the number of records that match a filter."""
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
            .withDescription("Name of the table to count.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person")
            .withIn(In.PATH),
         new Parameter()
            .withName("includeDistinct")
            .withDescription("Whether or not to also return the count distinct records from the main table (e.g., in case of a to-many join; by default, do not).")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.BOOLEAN))
            .withExample("true")
            .withIn(In.QUERY)
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
   public TableCountInput buildInput(Context context) throws Exception
   {
      TableCountInput input = new TableCountInput();
      input.setTableName(getRequestParam(context, "tableName"));

      JSONObject requestBody = getRequestBodyAsJsonObject(context);
      if(requestBody != null)
      {
         input.setFilter(QuerySpecUtils.getFilterFromRequestBody(requestBody));
         input.setJoins(QuerySpecUtils.getJoinsFromRequestBody(requestBody));
         input.setTableVariant(QuerySpecUtils.getTableVariantFromRequestBody(requestBody));
      }

      String includeDistinctParam = getRequestParam(context, "includeDistinct");
      if("true".equals(includeDistinctParam))
      {
         input.setIncludeDistinct(true);
      }

      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(TableCountResponseV1.class.getSimpleName(), new TableCountResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("TODO", new Example()
         .withValue(new TableCountResponseV1().withCount(42L)));

      return new BasicResponse("""
         The number (count) of records matching the query""",
         TableCountResponseV1.class.getSimpleName(),
         examples
      );
   }

}
