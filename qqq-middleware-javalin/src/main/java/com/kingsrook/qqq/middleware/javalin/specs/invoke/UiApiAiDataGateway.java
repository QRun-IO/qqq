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

package com.kingsrook.qqq.middleware.javalin.specs.invoke;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.kingsrook.qqq.backend.core.actions.ai.AiDataGateway;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.openapi.model.HttpMethod;


/*******************************************************************************
 ** The AI tool kernel's data access through the UI API: every query, count,
 ** get, search and widget render is a /qqq/v1 operation, invoked in-process
 ** (UiApiInvoker) with the human user's own credentials and the invoker's
 ** client channel (assistant/<detail>).  So the assistant reads exactly what the
 ** user could read in the UI - permissions, record security, field access, the
 ** assistant wall and credential scopes all apply, enforced by the server.
 **
 ** Usage (per assistant turn):
 **   new AiToolKernelSettings().withDataGateway(new UiApiAiDataGateway(
 **      UiApiInvoker.forServer(server, "assistant"), UiApiCredentials.fromContext(ctx), turnId))
 *******************************************************************************/
public class UiApiAiDataGateway implements AiDataGateway
{
   private final UiApiInvoker     invoker;
   private final UiApiCredentials credentials;
   private final String           clientDetail;



   /*******************************************************************************
    ** Constructor
    **
    ** @param invoker      the invoker (its channel labels every call, e.g. assistant)
    ** @param credentials  the human user's credentials, as their request carried them
    ** @param clientDetail detail after the channel (e.g. the assistant turn id), or null
    *******************************************************************************/
   public UiApiAiDataGateway(UiApiInvoker invoker, UiApiCredentials credentials, String clientDetail)
   {
      this.invoker = invoker;
      this.credentials = credentials;
      this.clientDetail = clientDetail;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> query(QueryInput input) throws QException
   {
      JsonNode           json    = call(HttpMethod.POST, "/table/{tableName}/query", Map.of("tableName", input.getTableName()), filterBody(input.getFilter()), false);
      List<QRecord>      records = new ArrayList<>();
      Iterator<JsonNode> nodes   = json == null || !json.has("records") ? List.<JsonNode>of().iterator() : json.get("records").elements();
      while(nodes.hasNext())
      {
         records.add(toRecord(input.getTableName(), nodes.next()));
      }
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer count(CountInput input) throws QException
   {
      JsonNode json = call(HttpMethod.POST, "/table/{tableName}/count", Map.of("tableName", input.getTableName()), filterBody(input.getFilter()), false);
      return (json == null || !json.hasNonNull("count") ? null : json.get("count").asInt());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QRecord get(GetInput input) throws QException
   {
      JsonNode json = call(HttpMethod.GET, "/table/{tableName}/{primaryKey}", Map.of("tableName", input.getTableName(), "primaryKey", String.valueOf(input.getPrimaryKey())), null, true);
      return (json == null || !json.hasNonNull("record") ? null : toRecord(input.getTableName(), json.get("record")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<RecordSearchResult> search(RecordSearchInput input) throws QException
   {
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("searchTerm", input.getSearchTerm());
      if(input.getTableNames() != null)
      {
         body.put("tableNames", input.getTableNames());
      }
      if(input.getLimitPerTable() != null)
      {
         body.put("limitPerTable", input.getLimitPerTable());
      }

      JsonNode                 json    = call(HttpMethod.POST, "/search", Map.of(), body, false);
      List<RecordSearchResult> results = new ArrayList<>();
      if(json != null && json.has("results"))
      {
         for(JsonNode node : json.get("results"))
         {
            results.add(new RecordSearchResult()
               .withTableName(text(node, "tableName"))
               .withTableLabel(text(node, "tableLabel"))
               .withRecordId(text(node, "recordId"))
               .withRecordLabel(text(node, "recordLabel")));
         }
      }
      return (results);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Object> renderWidget(RenderWidgetInput input) throws QException
   {
      String   widgetName = input.getWidgetMetaData() == null ? null : input.getWidgetMetaData().getName();
      JsonNode json       = call(HttpMethod.POST, "/widget/{widgetName}", Map.of("widgetName", widgetName), new LinkedHashMap<>(input.getQueryParams() == null ? Map.of() : input.getQueryParams()), false);
      if(json == null || !json.isObject())
      {
         return (null);
      }

      try
      {
         return (JsonUtils.toObject(json.toString(), new TypeReference<>() {}));
      }
      catch(Exception e)
      {
         throw (new QException("Error reading widget data", e));
      }
   }



   /*******************************************************************************
    ** Invoke an operation; 401/403 become a permission-denied exception, 404
    ** null (when allowed) or not-found, other failures a QException with the
    ** server's message.
    *******************************************************************************/
   private JsonNode call(HttpMethod method, String path, Map<String, String> pathParams, Object jsonBody, boolean nullOnNotFound) throws QException
   {
      UiApiRequest request = new UiApiRequest()
         .withMethod(method)
         .withPath(path)
         .withCredentials(credentials)
         .withClientDetail(clientDetail)
         .withInvokedBy("ai-tool-kernel");
      pathParams.forEach(request::withPathParam);
      if(jsonBody != null)
      {
         request.withJsonBody(jsonBody);
      }

      UiApiResponse response = invoker.invoke(request);
      try
      {
         if(response.isSuccess())
         {
            return (response.getJson());
         }

         if(response.getStatus() == 404 && nullOnNotFound)
         {
            return (null);
         }

         String message = errorMessage(response);
         if(response.getStatus() == 401 || response.getStatus() == 403 || response.getStatus() == 404)
         {
            throw (new QPermissionDeniedException(message == null ? "Permission denied." : message));
         }
         throw (new QException(message == null ? ("UI API call failed: " + method + " " + path + " (" + response.getStatus() + ")") : message));
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Error reading the UI API response for " + method + " " + path, e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String errorMessage(UiApiResponse response)
   {
      try
      {
         JsonNode json = response.getJson();
         return (json != null && json.hasNonNull("error") ? json.get("error").asText() : null);
      }
      catch(Exception e)
      {
         return (null);
      }
   }



   /*******************************************************************************
    ** The {filter} body of query and count.
    *******************************************************************************/
   private static Map<String, Object> filterBody(QQueryFilter filter) throws QException
   {
      Map<String, Object> body = new LinkedHashMap<>();
      if(filter != null)
      {
         try
         {
            body.put("filter", JsonUtils.toObject(JsonUtils.toJson(filter), new TypeReference<Map<String, Object>>() {}));
         }
         catch(Exception e)
         {
            throw (new QException("Error serializing the query filter", e));
         }
      }
      return (body);
   }



   /*******************************************************************************
    ** An OutputRecord JSON node as a QRecord, values typed per the table's fields.
    *******************************************************************************/
   static QRecord toRecord(String tableName, JsonNode node) throws QException
   {
      QRecord        record = new QRecord().withTableName(tableName);
      QTableMetaData table  = QContext.getQInstance() == null ? null : QContext.getQInstance().getTable(tableName);
      try
      {
         Map<String, Object> values = node.has("values") ? JsonUtils.toObject(node.get("values").toString(), new TypeReference<>() {}) : Map.of();
         for(Map.Entry<String, Object> entry : values.entrySet())
         {
            QFieldMetaData field = table == null ? null : table.getFields().get(entry.getKey());
            Object         value = entry.getValue();
            record.setValue(entry.getKey(), field == null || value == null ? (value instanceof Serializable s ? s : ValueUtils.getValueAsString(value)) : ValueUtils.getValueAsFieldType(field.getType(), value));
         }

         Map<String, String> displayValues = node.has("displayValues") ? JsonUtils.toObject(node.get("displayValues").toString(), new TypeReference<>() {}) : Map.of();
         displayValues.forEach(record::setDisplayValue);
         if(node.hasNonNull("recordLabel"))
         {
            record.setRecordLabel(node.get("recordLabel").asText());
         }
      }
      catch(Exception e)
      {
         throw (new QException("Error reading a record from the UI API response", e));
      }
      return (record);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String text(JsonNode node, String key)
   {
      return (node.hasNonNull(key) ? node.get(key).asText() : null);
   }
}
