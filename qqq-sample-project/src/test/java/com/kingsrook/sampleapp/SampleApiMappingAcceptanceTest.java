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

package com.kingsrook.sampleapp;


import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.api.actions.BaseAPIActionUtil;
import com.kingsrook.qqq.backend.module.api.actions.QHttpResponse;
import com.kingsrook.qqq.backend.module.api.model.AuthorizationType;
import com.kingsrook.qqq.backend.module.api.model.metadata.APIBackendMetaData;
import com.kingsrook.qqq.backend.module.api.model.metadata.APITableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** A sample provider contract through real QQQ actions and loopback HTTP.
 *******************************************************************************/
@Timeout(20)
class SampleApiMappingAcceptanceTest
{
   private static final String TABLE = "apiMappingRecord";
   private static final String REMOTE_RECORD = "{\"remote_id\":41,\"details\":{\"label\":\"Blue & green\"},\"units\":7}";



   /*******************************************************************************
    ** Wire assertions are literal provider expectations, independent of the mapper.
    *******************************************************************************/
   @Test
   void testGetInsertUpdateDeleteTranslation() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.reply(200, "{\"items\":" + REMOTE_RECORD + "}");
         QRecord fetched = GetAction.execute(TABLE, 41);
         assertEquals(41, fetched.getValueInteger("id"));
         assertEquals("Blue & green", fetched.getValueString("name"));
         assertEquals(7, fetched.getValueInteger("quantity"));
         fixture.assertRequest(0, "GET", "/v1/widgets/41", "");

         fixture.reply(201, "{\"items\":{\"remote_id\":42}}");
         QRecord inserted = InsertAction.executeForRecords(new InsertInput().withTableName(TABLE)
            .withRecord(new QRecord().withValue("name", "Snow ☃ & ice").withValue("quantity", 3))).get(0);
         assertTrue(inserted.getErrors().isEmpty(), inserted.getErrors().toString());
         assertEquals(42, inserted.getValueInteger("id"));
         fixture.assertRequest(1, "POST", "/v1/widgets", "{\"items\":{\"details\":{\"label\":\"Snow ☃ & ice\"},\"units\":3}}");

         fixture.reply(204, "");
         QRecord updated = UpdateAction.executeForRecords(new UpdateInput().withTableName(TABLE)
            .withRecord(new QRecord().withValue("id", 42).withValue("name", "Changed").withValue("quantity", 8))).get(0);
         assertTrue(updated.getErrors().isEmpty(), updated.getErrors().toString());
         fixture.assertRequest(2, "PUT", "/v1/widgets", "{\"items\":[{\"remote_id\":42,\"details\":{\"label\":\"Changed\"},\"units\":8}]}");

         fixture.reply(204, "");
         assertEquals(1, new DeleteAction().execute(new DeleteInput().withTableName(TABLE).withPrimaryKey(42)).getDeletedRecordCount());
         fixture.assertRequest(3, "DELETE", "/v1/widgets/42", "");
         fixture.reply(404, "{\"error\":\"missing\"}");
         assertNull(GetAction.execute(TABLE, 42));
         fixture.assertRequest(4, "GET", "/v1/widgets/42", "");
         assertEquals(5, fixture.requests.size());
      }
   }



   /*******************************************************************************
    ** Pagination must advance offsets, preserve order and stop on the short page.
    *******************************************************************************/
   @Test
   void testQueryPaginationAndNestedResponseTranslation() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.reply(200, "{\"items\":[" + REMOTE_RECORD + ", {\"remote_id\":42,\"details\":{\"label\":\"Two\"},\"units\":2}]}");
         fixture.reply(200, "{\"items\":[{\"remote_id\":43},{\"remote_id\":44}]}");
         fixture.reply(200, "{\"items\":[{\"remote_id\":45}]}");
         List<QRecord> records = new QueryAction().execute(new QueryInput().withTableName(TABLE)).getRecords();
         assertEquals(List.of(41, 42, 43, 44, 45), records.stream().map(record -> record.getValueInteger("id")).toList());
         assertEquals("Blue & green", records.get(0).getValueString("name"));
         assertEquals(7, records.get(0).getValueInteger("quantity"));
         assertEquals("Two", records.get(1).getValueString("name"));
         fixture.assertRequest(0, "GET", "/v1/widgets?limit=2&offset=0", "");
         fixture.assertRequest(1, "GET", "/v1/widgets?limit=2&offset=2", "");
         fixture.assertRequest(2, "GET", "/v1/widgets?limit=2&offset=4", "");
         assertEquals(3, fixture.requests.size());
      }
   }



   /*******************************************************************************
    ** Full last pages require an empty-page probe; explicit limits must stop early.
    *******************************************************************************/
   @Test
   void testQueryEmptyPageAndExplicitLimitSkipFilter() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.reply(200, "{\"items\":[{\"remote_id\":1},{\"remote_id\":2}]}");
         fixture.reply(200, "{\"items\":[]}");
         assertEquals(2, new QueryAction().execute(new QueryInput().withTableName(TABLE)).getRecords().size());
         fixture.assertRequest(1, "GET", "/v1/widgets?limit=2&offset=2", "");
         fixture.reply(200, "{\"items\":[" + REMOTE_RECORD + "]}");
         List<QRecord> records = new QueryAction().execute(new QueryInput().withTableName(TABLE)
            .withFilter(new QQueryFilter().withLimit(1).withSkip(3)
               .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Blue & green")))).getRecords();
         assertEquals(List.of(41), records.stream().map(record -> record.getValueInteger("id")).toList());
         fixture.assertRequest(2, "GET", "/v1/widgets?limit=1&offset=3&label=Blue+%26+green", "");
         assertEquals(3, fixture.requests.size());
      }
   }



   /*******************************************************************************
    ** Count reads the provider total, not the size of its default result page.
    *******************************************************************************/
   @Test
   void testCountUsesProviderTotalAndFilter() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.reply(200, "{\"total\":17,\"items\":[" + REMOTE_RECORD + "]}");
         assertEquals(17, CountAction.execute(TABLE, new QQueryFilter()
            .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Blue & green"))));
         fixture.assertRequest(0, "GET", "/v1/widgets?count=true&label=Blue+%26+green", "");
         assertEquals(1, fixture.requests.size());
      }
   }



   /*******************************************************************************
    ** Non-success responses must not masquerade as records or successful writes.
    ** Read retries use the native default budget; mutations must be sent only once.
    *******************************************************************************/
   @Test
   void testNon2xxFailuresAndNoDuplicateWrites() throws Exception
   {
      for(int status : List.of(400, 503))
      {
         for(String action : List.of("get", "query", "count", "insert", "update", "delete"))
         {
            try(Fixture fixture = new Fixture())
            {
               int attempts = status == 503 && List.of("get", "query").contains(action) ? 4 : 1;
               for(int i = 0; i < attempts; i++)
               {
                  fixture.reply(status, "{\"error\":\"fixture rejection\"}");
               }
               assertActionFails(action);
               assertEquals(attempts, fixture.requests.size(), action + " HTTP " + status);
               for(Request request : fixture.requests)
               {
                  assertEquals(methodFor(action), request.method());
               }
            }
         }
      }
   }



   /*******************************************************************************
    ** The server has consumed the request before withholding its response.
    ** A timeout cannot be treated as success or cause an ambiguous write to repeat.
    *******************************************************************************/
   @Test
   void testTimeoutsAndNoDuplicateWrites() throws Exception
   {
      for(String action : List.of("get", "query", "count", "insert", "update", "delete"))
      {
         try(Fixture fixture = new Fixture())
         {
            fixture.reply(-1, "");
            long start = System.nanoTime();
            assertActionFails(action);
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 3000, action + " must honor the socket timeout");
            assertEquals(1, fixture.releaseResponse.getCount(), "Response is still withheld when the action fails");
            assertEquals(1, fixture.requests.size(), action);
            assertEquals(methodFor(action), fixture.requests.get(0).method());
         }
      }
   }



   /*******************************************************************************
    ** Missing/malformed resource envelopes and wrong JSON response types fail GET.
    *******************************************************************************/
   @Test
   void testGetRejectsMissingMalformedAndWrongResponseTypes() throws Exception
   {
      for(String body : List.of("", "not-json", "[]", "{}", "{\"items\":null}", "{\"items\":23}"))
      {
         try(Fixture fixture = new Fixture())
         {
            fixture.reply(200, body);
            assertThrows(QException.class, () -> GetAction.execute(TABLE, 41), body);
            fixture.assertRequest(0, "GET", "/v1/widgets/41", "");
            assertEquals(1, fixture.requests.size());
         }
      }
   }



   /*******************************************************************************
    ** Invalid query bodies fail; the existing empty-body contract is an empty result.
    *******************************************************************************/
   @Test
   void testQueryMalformedWrongTypeAndEmptyResponses() throws Exception
   {
      for(String body : List.of("not-json", "{\"items\":23}", "{\"items\":[23]}"))
      {
         try(Fixture fixture = new Fixture())
         {
            fixture.reply(200, body);
            assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput().withTableName(TABLE)), body);
            assertEquals(1, fixture.requests.size());
         }
      }
      for(String body : List.of("", "null", "[]", "{\"items\":[]}"))
      {
         try(Fixture fixture = new Fixture())
         {
            fixture.reply(200, body);
            assertTrue(new QueryAction().execute(new QueryInput().withTableName(TABLE)).getRecords().isEmpty());
            fixture.assertRequest(0, "GET", "/v1/widgets?limit=2&offset=0", "");
            assertEquals(1, fixture.requests.size());
         }
      }
   }



   /*******************************************************************************
    ** The total is mandatory; invalid count bodies cannot silently become zero.
    *******************************************************************************/
   @Test
   void testCountRejectsMissingMalformedAndWrongResponseTypes() throws Exception
   {
      for(String body : List.of("", "not-json", "{}", "{\"total\":\"invalid\"}"))
      {
         try(Fixture fixture = new Fixture())
         {
            fixture.reply(200, body);
            assertThrows(QException.class, () -> CountAction.execute(TABLE, null), body);
            fixture.assertRequest(0, "GET", "/v1/widgets?count=true", "");
            assertEquals(1, fixture.requests.size());
         }
      }
   }



   /*******************************************************************************
    ** A provider might commit then return an unusable response. Never send POST twice.
    *******************************************************************************/
   @Test
   void testInsertRejectsMissingMalformedAndWrongResponseWithoutRetry() throws Exception
   {
      for(String body : List.of("", "not-json", "[]", "{\"items\":23}", "{\"items\":{}}"))
      {
         try(Fixture fixture = new Fixture())
         {
            fixture.reply(201, body);
            QRecord inserted = InsertAction.executeForRecords(new InsertInput().withTableName(TABLE)
               .withRecord(new QRecord().withValue("name", "Once").withValue("quantity", 1))).get(0);
            assertFalse(inserted.getErrors().isEmpty(), body);
            assertNull(inserted.getValue("id"));
            fixture.assertRequest(0, "POST", "/v1/widgets", "{\"items\":{\"details\":{\"label\":\"Once\"},\"units\":1}}");
            assertEquals(1, fixture.requests.size());
         }
      }
   }



   /*******************************************************************************
    ** Unsupported filter/delete shapes fail before sending a request.
    *******************************************************************************/
   @Test
   void testUnsupportedProviderMappingsCannotBroadenAnOperation() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput().withTableName(TABLE)
            .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria("quantity", QCriteriaOperator.GREATER_THAN, 1)))));
         assertThrows(QException.class, () -> new DeleteAction().execute(new DeleteInput().withTableName(TABLE).withPrimaryKeys(List.of(41, 42))));
         assertTrue(fixture.requests.isEmpty());
      }
   }



   /*******************************************************************************
    ** Insert returns per-record errors; the other actions propagate QException.
    *******************************************************************************/
   private void assertActionFails(String action) throws QException
   {
      if(action.equals("insert"))
      {
         QRecord result = InsertAction.executeForRecords(new InsertInput().withTableName(TABLE)
            .withRecord(new QRecord().withValue("name", "Once").withValue("quantity", 1))).get(0);
         assertFalse(result.getErrors().isEmpty());
         assertNull(result.getValue("id"));
         return;
      }
      assertThrows(QException.class, () ->
      {
         switch(action)
         {
            case "get" -> GetAction.execute(TABLE, 41);
            case "query" -> new QueryAction().execute(new QueryInput().withTableName(TABLE));
            case "count" -> CountAction.execute(TABLE, null);
            case "update" -> new UpdateAction().execute(new UpdateInput().withTableName(TABLE)
               .withRecord(new QRecord().withValue("id", 41).withValue("name", "Changed")));
            case "delete" -> new DeleteAction().execute(new DeleteInput().withTableName(TABLE).withPrimaryKey(41));
            default -> throw new IllegalArgumentException(action);
         }
      }, action);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String methodFor(String action)
   {
      return switch(action)
      {
         case "insert" -> "POST";
         case "update" -> "PUT";
         case "delete" -> "DELETE";
         default -> "GET";
      };
   }



   /*******************************************************************************
    ** Only provider-specific shape/URL hooks are overridden; transport stays native.
    *******************************************************************************/
   public static class MappingActionUtil extends BaseAPIActionUtil
   {
      /***************************************************************************
       ** Bound stalled fixture calls while preserving native transport/retry behavior.
       ***************************************************************************/
      @Override
      protected int getSocketTimeoutMillis()
      {
         return 300;
      }



      /***************************************************************************
       ** This fixture provider uses offset pagination and one optional label filter.
       ***************************************************************************/
      @Override
      protected String buildQueryStringForGet(QQueryFilter filter, Integer limit, Integer skip, Map<String, QFieldMetaData> fields) throws QException
      {
         String query = limit == null ? "?count=true" : "?limit=" + limit + "&offset=" + (skip == null ? 0 : skip);
         if(filter != null)
         {
            if((filter.getSubFilters() != null && !filter.getSubFilters().isEmpty()) || (filter.getOrderBys() != null && !filter.getOrderBys().isEmpty()))
            {
               throw new QException("Provider does not support nested filters or ordering");
            }
            if(filter.getCriteria() != null)
            {
               for(QFilterCriteria criterion : filter.getCriteria())
               {
                  if(!"name".equals(criterion.getFieldName()) || criterion.getOperator() != QCriteriaOperator.EQUALS || criterion.getValues().size() != 1)
                  {
                     throw new QException("Provider supports only name equality");
                  }
                  query += "&label=" + URLEncoder.encode(criterion.getValues().get(0).toString(), StandardCharsets.UTF_8);
               }
            }
         }
         return query;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected Integer getApiStandardLimit()
      {
         return 2;
      }



      /***************************************************************************
       ** The provider count endpoint returns a total independently of page size.
       ***************************************************************************/
      @Override
      public Integer processGetResponseForCount(QTableMetaData table, QHttpResponse response) throws QException
      {
         try
         {
            return new JSONObject(response.getContent()).getInt("total");
         }
         catch(Exception e)
         {
            throw new QException("Invalid provider count response", e);
         }
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String buildUrlSuffixForSingleRecordGet(Serializable primaryKey)
      {
         return "/" + urlEncode(primaryKey);
      }



      /***************************************************************************
       ** This provider supports a single-resource delete.
       ***************************************************************************/
      @Override
      protected String buildQueryStringForDelete(QQueryFilter filter, List<Serializable> primaryKeys) throws QException
      {
         if(primaryKeys == null || primaryKeys.size() != 1)
         {
            throw new QException("Provider requires one delete key");
         }
         return "/" + urlEncode(primaryKeys.get(0));
      }



      /***************************************************************************
       ** Dotted response fields become nested request JSON for this provider.
       ***************************************************************************/
      @Override
      protected JSONObject recordToJsonObject(QTableMetaData table, QRecord record)
      {
         JSONObject body = super.recordToJsonObject(table, record);
         if(body.has("details.label"))
         {
            body.put("details", new JSONObject().put("label", body.remove("details.label")));
         }
         return body;
      }



      /***************************************************************************
       ** GET/POST resource responses are enveloped; query uses the native wrapper.
       ***************************************************************************/
      @Override
      protected JSONObject getJsonObject(QHttpResponse response) throws QException
      {
         try
         {
            return super.getJsonObject(response).getJSONObject("items");
         }
         catch(Exception e)
         {
            throw new QException("Invalid provider resource response", e);
         }
      }
   }



   /*******************************************************************************
    ** Scripted provider responses and captured requests never call mapping helpers.
    *******************************************************************************/
   private static class Fixture implements AutoCloseable
   {
      private final HttpServer server;
      private final ExecutorService executor = Executors.newCachedThreadPool();
      private final ConcurrentLinkedQueue<Reply> replies = new ConcurrentLinkedQueue<>();
      private final List<Request> requests = new CopyOnWriteArrayList<>();
      private final CountDownLatch releaseResponse = new CountDownLatch(1);



      /***************************************************************************
       **
       ***************************************************************************/
      private Fixture() throws Exception
      {
         server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
         server.createContext("/", this::handle);
         server.setExecutor(executor);
         server.start();
         QInstance instance = SampleMetaDataProvider.defineTestInstance();
         instance.addBackend(new APIBackendMetaData().withName("mappingApi")
            .withBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/")
            .withContentType("application/json").withAuthorizationType(AuthorizationType.CUSTOM)
            .withActionUtil(new QCodeReference(MappingActionUtil.class)));
         instance.addTable(new QTableMetaData().withName(TABLE).withBackendName("mappingApi").withPrimaryKeyField("id")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER).withBackendName("remote_id"))
            .withField(new QFieldMetaData("name", QFieldType.STRING).withBackendName("details.label"))
            .withField(new QFieldMetaData("quantity", QFieldType.INTEGER).withBackendName("units"))
            .withBackendDetails(new APITableBackendDetails().withTablePath("widgets").withTableWrapperObjectName("items")));
         QContext.init(instance, new QSession());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      private void reply(int status, String body)
      {
         replies.add(new Reply(status, body));
      }



      /***************************************************************************
       ** Unexpected extra requests receive an error and remain visible to assertions.
       ***************************************************************************/
      private void handle(HttpExchange exchange) throws IOException
      {
         try(exchange)
         {
            requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().toASCIIString(),
               new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), exchange.getRequestHeaders().getFirst("Content-Type")));
            Reply reply = replies.poll();
            if(reply == null)
            {
               reply = new Reply(400, "{\"error\":\"unexpected request\"}");
            }
            if(reply.status() == -1)
            {
               try
               {
                  releaseResponse.await(10, TimeUnit.SECONDS);
               }
               catch(InterruptedException e)
               {
                  Thread.currentThread().interrupt();
               }
               return;
            }
            byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), reply.status() == 204 ? -1 : body.length);
            if(reply.status() != 204)
            {
               exchange.getResponseBody().write(body);
            }
         }
      }



      /***************************************************************************
       ** JSON key order is immaterial, but field names, wrappers and values are exact.
       ***************************************************************************/
      private void assertRequest(int index, String method, String target, String body)
      {
         Request request = requests.get(index);
         assertEquals(method, request.method());
         assertEquals(target, request.target());
         if(body.isEmpty())
         {
            assertEquals("", request.body());
         }
         else
         {
            assertTrue(new JSONObject(body).similar(new JSONObject(request.body())), request.body());
            assertTrue(request.contentType().startsWith("application/json"));
         }
      }



      /***************************************************************************
       ** Every fixture owns its server and threads; pending replies expose false greens.
       ***************************************************************************/
      @Override
      public void close() throws Exception
      {
         QContext.clear();
         releaseResponse.countDown();
         server.stop(0);
         executor.shutdownNow();
         assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
         assertTrue(replies.isEmpty(), "Expected HTTP calls were not made");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record Reply(int status, String body) {}



   /*******************************************************************************
    **
    *******************************************************************************/
   private record Request(String method, String target, String body, String contentType) {}
}
