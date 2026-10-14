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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessDecision;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.ClientChannelsMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.API_KEY_VALUE;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.APP_NAME;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.PROCESS_NAME;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.TABLE_ASSIST;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.TABLE_CUSTOMER;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.TABLE_DISABLED;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.TABLE_SECRET;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.WIDGET_STATS;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.call;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.criterion;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.filter;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.store1User;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for AiToolKernel and its tools.
 *******************************************************************************/
class AiToolKernelTest extends BaseTest
{
   private AiToolKernel kernel;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      QInstance qInstance = AiKernelTestSupport.defineInstance();
      QContext.init(qInstance, new QSession());
      AiKernelTestSupport.insertData();
      QContext.clear();
      kernel = new AiToolKernel(qInstance, AiKernelTestSupport.settings());
   }



   /*******************************************************************************
    ** The data object inside a result's envelope.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   static Map<String, Object> data(AiToolResult result) throws Exception
   {
      String content = result.getModelContent();
      assertThat(content).startsWith("<qqq_data source=\"").endsWith("</qqq_data>");
      String json = content.substring(content.indexOf('>') + 1, content.length() - "</qqq_data>".length());
      return (JsonUtils.toObject(json, Map.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AiToolResult run(QSession session, AiToolCall call)
   {
      return (kernel.execute(session, call));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testToolDefinitions()
   {
      List<AiToolDefinition> definitions = kernel.getToolDefinitions();
      assertEquals(List.of("count_records", "describe_table", "get_record", "get_record_history", "list_tables", "navigate", "query_records", "render_widget", "search_help", "search_records"),
         definitions.stream().map(AiToolDefinition::getName).toList());

      for(AiToolDefinition definition : definitions)
      {
         assertTrue(definition.getStrict());
         assertNotNull(definition.getDescription());
         assertNotNull(definition.getKind());
         Map<String, Object> schema = definition.getInputSchema();
         assertEquals("object", schema.get("type"));
         assertEquals(false, schema.get("additionalProperties"));
         assertThat(schema.get("required")).isInstanceOf(List.class);
      }

      assertEquals(AiToolKind.NAVIGATION, definitions.stream().filter(d -> d.getName().equals("navigate")).findFirst().orElseThrow().getKind());

      //////////////////////////////////////////////////////
      // byte-stable: another kernel gives identical JSON //
      //////////////////////////////////////////////////////
      String json1 = JsonUtils.toJson(definitions);
      String json2 = JsonUtils.toJson(new AiToolKernel(QContext.getQInstance() == null ? kernelInstance() : QContext.getQInstance(), null).getToolDefinitions());
      assertEquals(json1, json2);
      assertThat(json1).contains("\"$defs\"").contains("QueryFilter").contains("\"additionalProperties\":false");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QInstance kernelInstance()
   {
      try
      {
         return (AiKernelTestSupport.defineInstance());
      }
      catch(QException e)
      {
         throw (new RuntimeException(e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testListTables() throws Exception
   {
      AiToolResult result = run(store1User(), call("list_tables"));
      assertEquals(AiToolOutcome.OK, result.getOutcome());

      List<Map<String, Object>> tables = (List<Map<String, Object>>) data(result).get("tables");
      List<String>              names  = tables.stream().map(t -> (String) t.get("name")).toList();
      assertThat(names).contains(TABLE_CUSTOMER, TABLE_SECRET, TABLE_DISABLED, AuditsMetaDataProvider.TABLE_NAME_AUDIT).doesNotContain(TABLE_ASSIST);
      Map<String, Object> customer = tables.stream().filter(t -> t.get("name").equals(TABLE_CUSTOMER)).findFirst().orElseThrow();
      assertEquals("One row per customer account.", customer.get("description"));
      assertEquals(true, customer.get("canEdit"));
      assertEquals(names.stream().sorted().toList(), names);
      assertThat(result.getSummary()).startsWith("Listed ");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testDescribeTable() throws Exception
   {
      AiToolResult result = run(store1User(), call("describe_table", "table", TABLE_CUSTOMER));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      assertEquals("table:customer:READ:OK", result.getPermissionCheck());

      Map<String, Object>       data   = data(result);
      List<Map<String, Object>> fields = (List<Map<String, Object>>) data.get("fields");
      List<String>              names  = fields.stream().map(f -> (String) f.get("name")).toList();
      assertThat(names).contains("id", "name", "email", "internalScore", "notes").doesNotContain("secretNote", "password", "salary");
      assertEquals("MASKED", fields.stream().filter(f -> f.get("name").equals("email")).findFirst().orElseThrow().get("exposure"));
      assertEquals("METADATA_ONLY", fields.stream().filter(f -> f.get("name").equals("internalScore")).findFirst().orElseThrow().get("exposure"));
      assertEquals("The customer's full legal name.", fields.stream().filter(f -> f.get("name").equals("name")).findFirst().orElseThrow().get("help"));
      assertEquals("Customers are the people who buy from us.", data.get("help"));
      assertEquals("/app/customer", data.get("openHref"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testQueryRecords() throws Exception
   {
      AiToolResult result = run(store1User(), call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND")));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      assertEquals(3, result.getRowCount());
      assertEquals(TABLE_CUSTOMER, result.getTableName());
      assertThat(result.getModelContent()).startsWith("<qqq_data source=\"query_records\" table=\"customer\" trust=\"untrusted\">");

      Map<String, Object>       data    = data(result);
      List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
      assertEquals(3, records.size());
      Map<String, Object> jane   = records.get(0);
      Map<String, Object> values = (Map<String, Object>) jane.get("values");
      assertEquals("Jane Doe", values.get("name"));
      assertEquals("j***@example.com", values.get("email"));
      assertFalse(values.containsKey("internalScore"));
      assertEquals("/app/customer/1", jane.get("href"));
      assertEquals("Jane Doe", jane.get("label"));
      assertThat(result.getRedactions()).contains("field:customer.email:MASKED");

      ///////////////////
      // render & refs //
      ///////////////////
      Map<String, Object> render = result.getRender();
      assertEquals("records", render.get("type"));
      assertEquals(false, render.get("truncated"));
      assertEquals(3, render.get("totalCount"));
      List<Map<String, Object>> columns = (List<Map<String, Object>>) render.get("columns");
      assertThat(columns.stream().map(c -> c.get("name")).toList()).contains("name", "email").doesNotContain("internalScore", "secretNote", "password", "salary");
      assertEquals(3, result.getRecordRefs().size());
      assertEquals("/app/customer/2", result.getRecordRefs().get(1).getHref());

      String openHref = (String) render.get("openHref");
      assertThat(openHref).startsWith("/app/customer?filter=");
      String decoded = new String(Base64.getDecoder().decode(URLDecoder.decode(openHref.substring(openHref.indexOf('=') + 1), StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
      assertThat(decoded).contains("\"booleanOperator\":\"AND\"");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testQueryRecordsFiltersSortsAndLimits() throws Exception
   {
      AiToolResult result = run(store1User(), call("query_records", "table", TABLE_CUSTOMER, "filter", filter("OR", criterion("name", "EQUALS", "John Roe"), criterion("id", "IN", 3, "1"))));
      assertEquals(3, result.getRowCount());

      result = run(store1User(), call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("id", "BETWEEN", 2, 3)),
         "orderBy", List.of(Map.of("field", "id", "ascending", false)), "fields", List.of("name")));
      List<Map<String, Object>> records = (List<Map<String, Object>>) data(result).get("records");
      assertEquals(List.of(3, 2), records.stream().map(r -> r.get("id")).toList());
      assertEquals(Map.of("name", "Max Moe"), records.get(0).get("values"));

      ///////////////////////////////////////////////////////////////
      // relative dates: signupDate is now minus id days - so only //
      // customer 1 signed up in the last 36 hours                 //
      ///////////////////////////////////////////////////////////////
      result = run(store1User(), call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("signupDate", "GREATER_THAN_OR_EQUALS", "NOW-36 HOURS"))));
      assertEquals(1, result.getRowCount());
      assertThat((String) result.getRender().get("openHref")).startsWith("/app/customer?filter=");

      result = run(store1User(), call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("signupDate", "LESS_THAN", "THIS_YEAR"))));
      assertEquals(AiToolOutcome.OK, result.getOutcome());

      ///////////////////////////////
      // limit and hasMore / count //
      ///////////////////////////////
      result = run(store1User(), call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND"), "limit", 2));
      Map<String, Object> data = data(result);
      assertEquals(2, data.get("rowCount"));
      assertEquals(3, data.get("totalCount"));
      assertEquals(true, data.get("hasMore"));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      assertEquals(true, result.getRender().get("truncated"));

      //////////////////////////////////////////////////
      // a server cap below the asked limit truncates //
      //////////////////////////////////////////////////
      AiToolKernel cappedKernel = new AiToolKernel(kernelInstanceWithData(), new AiToolKernelSettings().withMaxRowsToModel(2));
      result = cappedKernel.execute(store1User(), call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND"), "limit", 100));
      assertEquals(AiToolOutcome.TRUNCATED, result.getOutcome());
      assertEquals(2, result.getRowCount());
      assertThat((String) data(result).get("note")).contains("capped");
   }



   /*******************************************************************************
    ** The kernel's instance (data is shared via the memory store).
    *******************************************************************************/
   private QInstance kernelInstanceWithData() throws QException
   {
      return (AiKernelTestSupport.defineInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryRecordsInvalidInputs() throws Exception
   {
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("nope", "EQUALS", 1))), "Unknown field 'nope'");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("name", "LIKE", "x"))), "Unknown operator");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("id", "EQUALS", "abc"))), "not valid for field 'id'");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("id", "BETWEEN", 1))), "exactly 2 values");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("id", "IN"))), "between 1 and");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("XOR")), "AND or OR");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", Map.of("criteria", "x")), "must be a list");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", Map.of("subFilters", List.of(Map.of("subFilters", List.of())))), "may not be nested");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND"), "fields", List.of("internalScore")), "only be used in filters");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND"), "fields", List.of("secretNote")), "Unknown field 'secretNote'");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND"), "orderBy", List.of(Map.of("field", "password", "ascending", true))), "Unknown field 'password'");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND"), "orderBy", List.of(Map.of("field", "id", "ascending", "yes"))), "must be a boolean");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND"), "limit", "ten"), "must be an integer");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("signupDate", "EQUALS", "NOW-2 FORTNIGHTS"))), "not valid for field 'signupDate'");
      assertInvalid(call("query_records", "table", TABLE_CUSTOMER, "filter", filter("AND"), "bogus", 1), "Unknown input properties: bogus");
      assertInvalid(call("query_records", "filter", filter("AND")), "'table' is required");

      ///////////////////////////////////////////
      // sub-filters (one level) are supported //
      ///////////////////////////////////////////
      Map<String, Object> withSub = filter("AND");
      withSub.put("subFilters", List.of(filter("OR", criterion("id", "EQUALS", 1), criterion("id", "EQUALS", 2))));
      assertEquals(2, run(store1User(), call("query_records", "table", TABLE_CUSTOMER, "filter", withSub)).getRowCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertInvalid(AiToolCall call, String expectedMessagePart) throws Exception
   {
      AiToolResult result = run(store1User(), call);
      assertEquals(AiToolOutcome.INVALID_INPUT, result.getOutcome(), "Outcome for " + call.getInput());
      assertThat((String) data(result).get("message")).contains(expectedMessagePart);
      assertThat(result.getRecordRefs()).isEmpty();
      assertNull(result.getRender());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCountRecords() throws Exception
   {
      AiToolResult result = run(store1User(), call("count_records", "table", TABLE_CUSTOMER, "filter", filter("AND")));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      assertEquals(3, data(result).get("count"));
      assertEquals("count", result.getRender().get("type"));
      assertEquals(3, result.getRender().get("count"));
      assertThat(result.getSummary()).isEqualTo("Counted Customer · 3");

      //////////////////////////////////////////////////////////
      // METADATA_ONLY fields can be filtered on (count only) //
      //////////////////////////////////////////////////////////
      result = run(store1User(), call("count_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("internalScore", "GREATER_THAN", AiKernelTestSupport.SENTINEL_SCORE + 1))));
      assertEquals(2, data(result).get("count"));

      result = run(store1User(), call("count_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("notes", "IS_BLANK"))));
      assertEquals(0, data(result).get("count"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testGetRecord() throws Exception
   {
      AiToolResult result = run(store1User(), call("get_record", "table", TABLE_CUSTOMER, "id", "2"));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      Map<String, Object> data = data(result);
      assertEquals(2, data.get("id"));
      assertEquals("John Roe", ((Map<String, Object>) data.get("values")).get("name"));
      assertEquals("record", result.getRender().get("type"));
      assertEquals("/app/customer/2", result.getRender().get("href"));
      assertEquals(1, result.getRecordRefs().size());

      assertEquals(AiToolOutcome.NOT_FOUND, run(store1User(), call("get_record", "table", TABLE_CUSTOMER, "id", "999")).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("get_record", "table", TABLE_CUSTOMER, "id", "abc")).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("get_record", "table", TABLE_CUSTOMER, "id", Map.of("x", 1))).getOutcome());

      //////////////////////////////////////////
      // long values are truncated for models //
      //////////////////////////////////////////
      result = run(store1User(), call("get_record", "table", TABLE_CUSTOMER, "id", 3));
      String notes = (String) ((Map<String, Object>) data(result).get("values")).get("notes");
      assertEquals(500 + "...[truncated]".length(), notes.length());
      assertThat(notes).endsWith("...[truncated]");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testSearchRecords() throws Exception
   {
      AiToolResult result = run(store1User(), call("search_records", "term", "Jane"));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      assertEquals(1, result.getRecordRefs().size());
      assertEquals("Jane Doe", result.getRecordRefs().get(0).getLabel());
      assertEquals("/app/customer/1", result.getRecordRefs().get(0).getHref());
      List<Map<String, Object>> results = (List<Map<String, Object>>) data(result).get("results");
      assertEquals(TABLE_CUSTOMER, results.get(0).get("table"));

      result = run(store1User(), call("search_records", "term", "Jane", "tables", List.of(TABLE_SECRET)));
      assertEquals(0, result.getRowCount());

      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("search_records", "term", "x".repeat(101))).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("search_records")).getOutcome());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testGetRecordHistory() throws Exception
   {
      AiToolResult result = run(store1User(), call("get_record_history", "table", TABLE_CUSTOMER, "id", "1"));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      assertEquals("table:customer:READ:OK;table:audit:READ:OK", result.getPermissionCheck());

      Map<String, Object>       data    = data(result);
      List<Map<String, Object>> entries = (List<Map<String, Object>>) data.get("entries");
      assertEquals(2, entries.size());
      assertEquals("Admin User", entries.get(0).get("user"));

      /////////////////////////////////////////////////////////////
      // newest first: entry 0 is audit 2 (email masked; score,  //
      // METADATA_ONLY, dropped); entry 1 is audit 1 (name kept; //
      // secretNote, hidden, dropped)                            //
      /////////////////////////////////////////////////////////////
      List<Map<String, Object>> newestDetails = (List<Map<String, Object>>) entries.get(0).get("details");
      assertEquals(1, newestDetails.size());
      assertEquals("email", newestDetails.get(0).get("field"));
      assertEquals("j***@example.com", newestDetails.get(0).get("newValue"));
      assertEquals("o***@example.com", newestDetails.get(0).get("oldValue"));

      List<Map<String, Object>> oldestDetails = (List<Map<String, Object>>) entries.get(1).get("details");
      assertEquals(1, oldestDetails.size());
      assertEquals("name", oldestDetails.get(0).get("field"));
      assertEquals("Jane Doe", oldestDetails.get(0).get("newValue"));
      assertEquals(false, oldestDetails.get(0).get("truncated"));

      assertEquals("history", result.getRender().get("type"));

      ///////////////////
      // since / until //
      ///////////////////
      result = run(store1User(), call("get_record_history", "table", TABLE_CUSTOMER, "id", "1", "since", "NOW-1 DAYS"));
      assertEquals(1, result.getRowCount());
      result = run(store1User(), call("get_record_history", "table", TABLE_CUSTOMER, "id", "1", "until", "2000-01-01T00:00:00Z"));
      assertEquals(0, result.getRowCount());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("get_record_history", "table", TABLE_CUSTOMER, "id", "1", "since", "yesterday-ish")).getOutcome());

      ////////////////////////////////////
      // a record with no audit history //
      ////////////////////////////////////
      result = run(store1User(), call("get_record_history", "table", TABLE_CUSTOMER, "id", "3"));
      assertEquals(0, result.getRowCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testRenderWidget() throws Exception
   {
      AiToolResult result = run(store1User(), call("render_widget", "name", WIDGET_STATS, "params", List.of(Map.of("name", "region", "value", "west"))));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      assertEquals(WIDGET_STATS, result.getWidgetName());
      assertEquals("widget:customerStats:OK", result.getPermissionCheck());

      Map<String, Object> render = result.getRender();
      assertEquals("widget", render.get("type"));
      assertEquals("chart", render.get("widgetType"));
      Map<String, Object> widgetData = (Map<String, Object>) render.get("data");
      assertThat(JsonUtils.toJson(widgetData)).contains("Customers per month west").doesNotContain(API_KEY_VALUE).contains("[REDACTED:api_key]");
      assertThat(result.getRedactions()).contains("api_key");

      /////////////////////////////////////////////
      // lists are capped at maxRowsToModel (50) //
      /////////////////////////////////////////////
      Map<String, Object> chartData = (Map<String, Object>) widgetData.get("chartData");
      assertEquals(50, ((List<Object>) chartData.get("labels")).size());

      assertEquals(AiToolOutcome.DENIED, run(store1User(), call("render_widget", "name", "noSuchWidget")).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("render_widget", "name", WIDGET_STATS, "params", List.of(Map.of("name", "x", "value", "y", "z", "w")))).getOutcome());

      AiToolKernel hidingKernel = new AiToolKernel(kernelInstanceWithData(), AiKernelTestSupport.settings().withHiddenWidgetName(WIDGET_STATS));
      assertEquals(AiToolOutcome.DENIED, hidingKernel.execute(store1User(), call("render_widget", "name", WIDGET_STATS)).getOutcome());

      ////////////////////////////////////////////////////////////////
      // an unexpected error is ERROR, with no detail for the model //
      ////////////////////////////////////////////////////////////////
      result = run(store1User(), call("render_widget", "name", WIDGET_STATS, "params", List.of(Map.of("name", "explode", "value", "true"))));
      assertEquals(AiToolOutcome.ERROR, result.getOutcome());
      assertThat(result.getModelContent()).doesNotContain("internal detail").doesNotContain("Exception");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testSearchHelp() throws Exception
   {
      AiToolResult result = run(store1User(), call("search_help", "query", "customer legal name"));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      List<Map<String, Object>> results = (List<Map<String, Object>>) data(result).get("results");
      assertThat(results).isNotEmpty();
      assertThat(results.stream().map(r -> r.get("kind") + ":" + r.get("name")).toList()).contains("field:name", "table:customer");
      assertThat(JsonUtils.toJson(results)).doesNotContain("Jane").doesNotContain("assistantConversation");

      result = run(store1User(), call("search_help", "query", "recalculate"));
      assertThat(JsonUtils.toJson(data(result))).contains(PROCESS_NAME).contains("/app/" + PROCESS_NAME);

      result = run(store1User(), call("search_help", "query", "crm"));
      assertThat(JsonUtils.toJson(data(result))).contains("\"app\"");

      result = run(store1User(), call("search_help", "query", "stats"));
      assertThat(JsonUtils.toJson(data(result))).contains(WIDGET_STATS);

      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("search_help", "query", "?")).getOutcome());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNavigate() throws Exception
   {
      assertEquals("/app/customer", navigate(call("navigate", "target", TABLE_CUSTOMER)));
      assertEquals("/app/customer/2", navigate(call("navigate", "target", TABLE_CUSTOMER, "recordId", "2")));
      assertThat(navigate(call("navigate", "target", TABLE_CUSTOMER, "filter", filter("AND", criterion("name", "CONTAINS", "Jane"))))).startsWith("/app/customer?filter=");
      assertEquals("/app/" + PROCESS_NAME, navigate(call("navigate", "target", PROCESS_NAME)));
      assertEquals("/app/" + APP_NAME, navigate(call("navigate", "target", APP_NAME)));
      assertEquals("/app/customer/1#audit", navigate(call("navigate", "target", "/app/customer/1#audit")));

      AiToolResult result = run(store1User(), call("navigate", "target", TABLE_CUSTOMER));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      assertEquals("{\"href\":\"/app/customer\"}", JsonUtils.toJson(data(result)));

      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("navigate", "target", PROCESS_NAME, "recordId", "1")).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("navigate", "target", "/app/customer", "recordId", "1")).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("navigate", "target", TABLE_CUSTOMER, "recordId", "1", "filter", filter("AND"))).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("navigate", "target", "/app/noSuchThing")).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("navigate", "target", TABLE_ASSIST)).getOutcome());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String navigate(AiToolCall call)
   {
      AiToolResult result = run(store1User(), call);
      assertEquals(AiToolOutcome.OK, result.getOutcome(), () -> result.getModelContent());
      assertEquals(AiToolKind.NAVIGATION.name(), "NAVIGATION");
      return (result.getNavigateHref());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalidCallsNeverThrow() throws Exception
   {
      AiToolResult result = run(store1User(), call("drop_tables"));
      assertEquals(AiToolOutcome.INVALID_INPUT, result.getOutcome());
      assertThat((String) data(result).get("message")).contains("Unknown tool 'drop_tables'").contains("query_records");

      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), null).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), new AiToolCall()).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), new AiToolCall().withName("list_tables").withInput(Map.of("x", 1))).getOutcome());
      assertEquals(AiToolOutcome.OK, run(store1User(), new AiToolCall().withName("list_tables")).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("describe_table", "table", List.of("a"))).getOutcome());
      assertEquals(AiToolOutcome.INVALID_INPUT, run(store1User(), call("query_records", "table", TABLE_CUSTOMER, "filter", "AND")).getOutcome());

      result = run(null, call("list_tables"));
      assertEquals(AiToolOutcome.DENIED, result.getOutcome());
      assertNotNull(result.getDurationMs());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testContextIsRestored() throws Exception
   {
      QInstance otherInstance = AiKernelTestSupport.defineInstance();
      QSession  outerSession  = new QSession();
      QContext.init(otherInstance, outerSession);

      AiToolResult result = run(store1User(), call("count_records", "table", TABLE_CUSTOMER, "filter", filter("AND")));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      assertSame(otherInstance, QContext.getQInstance());
      assertSame(outerSession, QContext.getQSession());

      QContext.clear();
      run(store1User(), call("list_tables"));
      assertNull(QContext.getQInstance());
      assertNull(QContext.getQSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDigests()
   {
      String appDigest = kernel.buildAppDigest();
      assertEquals(appDigest, kernel.buildAppDigest());
      assertThat(appDigest).contains("Tables:").contains("- publicNote: Public Note - Notes everyone may read.").contains("Processes:").contains("- publicProcess: Public Process")
         .doesNotContain(TABLE_ASSIST).doesNotContain("- customer");

      String userDigest = kernel.buildUserDigest(store1User());
      assertThat(userDigest).contains("User: store1User").contains("Timezone:").contains("- customer (Customer) [insert, edit, delete] - One row per customer account.")
         .contains("Your apps:").contains("- " + APP_NAME + ": CRM").contains("Your processes:").contains("- " + PROCESS_NAME).contains("Your widgets:").contains("- " + WIDGET_STATS)
         .contains("- publicNote (Public Note) [insert, edit, delete]\n").doesNotContain(TABLE_ASSIST);

      String limitedDigest = kernel.buildUserDigest(AiKernelTestSupport.userWithout("customer.read", "customer.edit", "customer.insert", "customer.delete"));
      assertThat(limitedDigest).doesNotContain("- customer ");

      assertThat(kernel.buildUserDigest(null)).contains("unavailable");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCustomRedactionAndDefaultExposure() throws Exception
   {
      AiToolKernel kernel2 = new AiToolKernel(kernelInstanceWithData(), new AiToolKernelSettings().withCustomRedactionPattern(Pattern.compile("John")));
      AiToolResult result  = kernel2.execute(store1User(), call("get_record", "table", TABLE_CUSTOMER, "id", "2"));
      assertThat(result.getModelContent()).doesNotContain("John").contains("[REDACTED:custom]");
      assertThat(result.getRedactions()).contains("custom");

      AiToolKernel metadataOnlyKernel = new AiToolKernel(kernelInstanceWithData(), new AiToolKernelSettings().withDefaultExposure(AiExposure.METADATA_ONLY));
      result = metadataOnlyKernel.execute(store1User(), call("get_record", "table", TABLE_CUSTOMER, "id", "2"));
      assertEquals(AiToolOutcome.OK, result.getOutcome());
      assertThat(result.getModelContent()).doesNotContain("John Roe");
      assertEquals(AiToolOutcome.OK, metadataOnlyKernel.execute(store1User(), call("count_records", "table", TABLE_CUSTOMER, "filter", filter("AND", criterion("name", "EQUALS", "John Roe")))).getOutcome());
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDescribeAccess()
   {
      AiAccessReport report = kernel.describeAccess(store1User(), TABLE_CUSTOMER, 1);
      assertEquals(true, report.getRecordReadable());
      assertEquals("Jane Doe", report.getRecordLabel());
      assertEquals("Customer", report.getTableLabel());
      assertEquals(AiExposure.VALUES, report.getTableExposure());
      assertNull(report.getReason());
      assertEquals(Map.of("read", true, "insert", true, "edit", true, "delete", true), report.getUserTablePermissions());
      assertEquals(kernel.getToolDefinitions().stream().map(AiToolDefinition::getName).toList(), report.getTools().stream().map(AiToolApplicability::getToolName).toList());
      assertTrue(applicability(report, "query_records").getApplies());
      assertFalse(applicability(report, "render_widget").getApplies());

      AiFieldAccess email = report.getFields().stream().filter(f -> f.getFieldName().equals("email")).findFirst().orElseThrow();
      assertEquals(AiExposure.MASKED, email.getExposure());
      assertEquals("metadata", email.getReason());
      assertTrue(email.getCanPropose());
      AiFieldAccess salary = report.getFields().stream().filter(f -> f.getFieldName().equals("salary")).findFirst().orElseThrow();
      assertEquals("field security lock", salary.getReason());
      assertFalse(salary.getCanPropose());

      ///////////////////////////////////////////////////////////
      // table-level report; no audit permission; no edit perm //
      ///////////////////////////////////////////////////////////
      AiAccessReport noAudit = kernel.describeAccess(AiKernelTestSupport.userWithout("audit.read", "customer.edit"), TABLE_CUSTOMER, null);
      assertNull(noAudit.getRecordReadable());
      assertEquals("no READ on audit table", applicability(noAudit, "get_record_history").getReason());
      assertEquals(false, noAudit.getUserTablePermissions().get("edit"));
      assertTrue(noAudit.getFields().stream().noneMatch(AiFieldAccess::getCanPropose));

      /////////////////////////////////////////////////////////
      // a NONE table: readable by the user, but no tool use //
      /////////////////////////////////////////////////////////
      AiAccessReport assistant = kernel.describeAccess(store1User(), TABLE_ASSIST, 1);
      assertEquals(AiExposure.NONE, assistant.getTableExposure());
      assertNull(assistant.getRecordLabel());
      assertEquals(true, assistant.getRecordReadable());
      assertFalse(applicability(assistant, "query_records").getApplies());
      assertEquals("table not exposed to the assistant", applicability(assistant, "get_record").getReason());
      assertEquals("table values not exposed to the assistant", applicability(assistant, "search_records").getReason());
      assertEquals("table", assistant.getFields().stream().filter(field -> !field.getFieldName().equals("id")).findFirst().orElseThrow().getReason());
      assertEquals(AiExposure.VALUES, assistant.getFields().stream().filter(field -> field.getFieldName().equals("id")).findFirst().orElseThrow().getExposure());

      AiAccessReport disabled = kernel.describeAccess(store1User(), TABLE_DISABLED, "abc");
      assertEquals(false, disabled.getRecordReadable());
      assertEquals("table has no search fields", applicability(disabled, "search_records").getReason());
      assertEquals("record not available", applicability(disabled, "get_record_history").getReason());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static AiToolApplicability applicability(AiAccessReport report, String toolName)
   {
      return (report.getTools().stream().filter(t -> t.getToolName().equals(toolName)).findFirst().orElseThrow());
   }



   /*******************************************************************************
    ** Kernel entry uses the assistant wall and restores the caller's session.
    *******************************************************************************/
   @Test
   void assistantChannelAndSharedResolver() throws Exception
   {
      QInstance instance = kernelInstanceWithData();
      instance.getSupplementalMetaData().remove(ClientChannelsMetaData.NAME);
      QSession caller = store1User().withClientChannel("next-ui");
      QContext.init(instance, caller);
      AiToolKernel assistant = new AiToolKernel(instance, AiKernelTestSupport.settings());
      AiToolResult denied = assistant.execute(caller, call("get_record_history", "table", TABLE_CUSTOMER, "id", 1));
      assertEquals(AiToolOutcome.DENIED, denied.getOutcome());
      assertSame(caller, QContext.getQSession());
      assertEquals("next-ui", caller.getClientChannelName());
      FieldAccessResolverMetaData.ofOrWithNew(instance).withResolverInstance((table, field) ->
      {
         assertEquals("assistant", QContext.getQSession().getClientChannelName());
         return ("name".equals(field.getName()) ? new FieldAccessDecision(FieldAccessTier.MASKED, null, "resolver") : FieldAccessDecision.FULL);
      });
      AiAccessReport report = assistant.describeAccess(caller, TABLE_CUSTOMER, null);
      assertEquals(AiExposure.MASKED, report.getFields().stream().filter(field -> "name".equals(field.getFieldName())).findFirst().orElseThrow().getExposure());
      assertSame(caller, QContext.getQSession());
      assertEquals("next-ui", caller.getClientChannelName());
   }
}
