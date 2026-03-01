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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.executors.TableGetExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableGetInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.TableGetResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableGetSpecV1 extends AbstractEndpointSpec<TableGetInput, TableGetResponseV1, TableGetExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/{primaryKey}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Get a record from a table by primary key")
         .withLongDescription("""
            Fetch a single record from a table, identified by its primary key value."""
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
            .withDescription("Name of the table to get the record from.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person")
            .withIn(In.PATH),
         new Parameter()
            .withName("primaryKey")
            .withDescription("Primary key value of the record to get.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("42")
            .withIn(In.PATH),
         new Parameter()
            .withName("includeAssociations")
            .withDescription("Whether or not to include associated records.")
            .withRequired(false)
            .withSchema(new Schema().withType(Type.BOOLEAN))
            .withExample("true")
            .withIn(In.QUERY),
         new Parameter()
            .withName("queryJoins")
            .withDescription("JSON array of QueryJoin objects specifying tables to join into the get.")
            .withRequired(false)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("""
               [{"joinTable":"orderLine","select":true}]""")
            .withIn(In.QUERY)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public TableGetInput buildInput(Context context) throws Exception
   {
      TableGetInput input = new TableGetInput();
      input.setTableName(getRequestParam(context, "tableName"));
      input.setPrimaryKey(getRequestParam(context, "primaryKey"));

      String includeAssociations = getRequestParam(context, "includeAssociations");
      if("true".equals(includeAssociations))
      {
         input.setIncludeAssociations(true);
      }

      String queryJoinsParam = getRequestParam(context, "queryJoins");
      if(StringUtils.hasContent(queryJoinsParam))
      {
         List<QueryJoin> queryJoins = new ArrayList<>();
         JSONArray       queryJoinsJSON = new JSONArray(queryJoinsParam);
         for(int i = 0; i < queryJoinsJSON.length(); i++)
         {
            QueryJoin  queryJoin  = new QueryJoin();
            JSONObject jsonObject = queryJoinsJSON.getJSONObject(i);

            queryJoin.setJoinTable(jsonObject.optString("joinTable"));
            queryJoin.setSelect(jsonObject.optBoolean("select"));

            if(jsonObject.has("baseTableOrAlias") && !jsonObject.isNull("baseTableOrAlias"))
            {
               queryJoin.setBaseTableOrAlias(jsonObject.optString("baseTableOrAlias"));
            }

            if(jsonObject.has("alias") && !jsonObject.isNull("alias"))
            {
               queryJoin.setAlias(jsonObject.optString("alias"));
            }

            if(jsonObject.has("type") && !jsonObject.isNull("type"))
            {
               queryJoin.setType(QueryJoin.Type.valueOf(jsonObject.getString("type")));
            }

            if(jsonObject.has("joinName") && !jsonObject.isNull("joinName"))
            {
               queryJoin.setJoinMetaData(QContext.getQInstance().getJoin(jsonObject.getString("joinName")));
            }

            queryJoins.add(queryJoin);
         }
         input.setQueryJoins(queryJoins);
      }

      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(TableGetResponseV1.class.getSimpleName(), new TableGetResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("TODO", new Example()
         .withValue(new TableGetResponseV1().withRecord(new QRecord()
            .withRecordLabel("Darin Kelkhoff")
            .withTableName("person")
            .withValue("id", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff")
            .withDisplayValue("id", "1").withDisplayValue("firstName", "Darin").withDisplayValue("lastName", "Kelkhoff")
         )));

      return new BasicResponse("""
         The record matching the given primary key""",
         TableGetResponseV1.class.getSimpleName(),
         examples
      );
   }

}
