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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 **
 *******************************************************************************/
public class QuerySpecUtils
{

   /***************************************************************************
    **
    ***************************************************************************/
   public static RequestBody defineQueryOrCountRequestBody()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("filter", new Schema()
         .withDescription("QueryFilter to specify matching records to be returned by the query")
         .withRef("#/components/schemas/QueryFilter"));
      properties.put("joins", new Schema()
         .withDescription("QueryJoin objects to specify tables to be joined into the query")
         .withType(Type.ARRAY)
         .withItems(new Schema()
            .withRef("#/components/schemas/QueryJoin")));
      properties.put("tableVariant", new Schema()
         .withDescription("For tables that use variant backends, specification of which variant to use.")
         .withRef("#/components/schemas/TableVariant"));

      return new RequestBody()
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
   public static QQueryFilter getFilterFromRequestBody(JSONObject requestBody) throws IOException
   {
      if(requestBody.has("filter"))
      {
         Object filterFromJson = requestBody.get("filter");
         if(filterFromJson instanceof JSONObject filterJsonObject)
         {
            return (JsonUtils.toObject(filterJsonObject.toString(), QQueryFilter.class));
         }
      }

      return (null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static List<QueryJoin> getJoinsFromRequestBody(JSONObject requestBody) throws IOException
   {
      if(requestBody.has("joins") && !requestBody.isNull("joins"))
      {
         Object joinsFromJson = requestBody.get("joins");
         if(joinsFromJson instanceof JSONArray joinsJsonArray)
         {
            List<QueryJoin> joins = new ArrayList<>();
            for(int i = 0; i < joinsJsonArray.length(); i++)
            {
               JSONObject joinJsonObject = joinsJsonArray.getJSONObject(i);
               QueryJoin queryJoin = new QueryJoin();
               joins.add(queryJoin);

               queryJoin.setJoinTable(joinJsonObject.optString("joinTable"));

               if(joinJsonObject.has("baseTableOrAlias") && !joinJsonObject.isNull("baseTableOrAlias"))
               {
                  queryJoin.setBaseTableOrAlias(joinJsonObject.optString("baseTableOrAlias"));
               }

               if(joinJsonObject.has("alias") && !joinJsonObject.isNull("alias"))
               {
                  queryJoin.setAlias(joinJsonObject.optString("alias"));
               }

               queryJoin.setSelect(joinJsonObject.optBoolean("select"));

               if(joinJsonObject.has("type") && !joinJsonObject.isNull("type"))
               {
                  queryJoin.setType(QueryJoin.Type.valueOf(joinJsonObject.getString("type")));
               }

               if(joinJsonObject.has("joinName") && !joinJsonObject.isNull("joinName"))
               {
                  queryJoin.setJoinMetaData(QContext.getQInstance().getJoin(joinJsonObject.getString("joinName")));
               }
            }
            return (joins);
         }
      }

      return null;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static TableVariant getTableVariantFromRequestBody(JSONObject requestBody)
   {
      if(requestBody.has("tableVariant"))
      {
         Object variantFromJson = requestBody.get("tableVariant");
         if(variantFromJson instanceof JSONObject variantJsonObject)
         {
            TableVariant tableVariant = new TableVariant();
            tableVariant.setType(variantJsonObject.optString("type"));
            tableVariant.setId(variantJsonObject.optString("id"));
            tableVariant.setName(variantJsonObject.optString("name"));
            return (tableVariant);
         }
      }

      return null;
   }
}
