/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.api;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.api.model.metadata.APIBackendMetaData;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Local protocol tests for the EasyPost-shaped adapter. These exercise actual
 ** HTTP requests and responses, without certifying the live provider's behavior.
 *******************************************************************************/
public class EasyPostApiTest extends BaseTest
{
   private static final String FIXTURE_API_KEY = "local-protocol-fixture-key";
   private static final String EXPECTED_AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString((FIXTURE_API_KEY + ":").getBytes(StandardCharsets.UTF_8));

   private HttpServer server;
   private final List<CapturedRequest> requests = new CopyOnWriteArrayList<>();



   /*******************************************************************************
    ** Override both endpoint and credentials before the adapter makes any request.
    *******************************************************************************/
   @BeforeEach
   void startFixture() throws IOException
   {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/", this::respond);
      server.start();
      APIBackendMetaData backend = (APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.EASYPOST_BACKEND_NAME);
      backend.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v2/");
      backend.setUsername(FIXTURE_API_KEY);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void stopFixture()
   {
      if(server != null)
      {
         server.stop(0);
      }
   }



   /*******************************************************************************
    ** Logical field names map to the wrapped provider body and returned primary key.
    *******************************************************************************/
   @Test
   void testPostTrackerSuccess() throws QException
   {
      QRecord record = new QRecord().withValue("__ignoreMe", "123")
         .withValue("carrier", "USPS").withValue("trackingNo", "EZ4000000004");
      QRecord output = new InsertAction().execute(new InsertInput("easypostTracker").withRecord(record)).getRecords().get(0);

      assertEquals("trk_fixture_delivered", output.getValueString("id"));
      assertThat(output.getErrors()).isNullOrEmpty();
      assertThat(requests).hasSize(1);
      assertRequest(requests.get(0), "EZ4000000004", EXPECTED_AUTHORIZATION);
   }



   /*******************************************************************************
    ** Each bulk record makes its own request and receives its own returned ID.
    *******************************************************************************/
   @Test
   void testPostMultiple() throws QException
   {
      InsertOutput output = new InsertAction().execute(new InsertInput("easypostTracker").withRecords(List.of(
         new QRecord().withValue("carrier", "USPS").withValue("trackingNo", "EZ1000000001"),
         new QRecord().withValue("carrier", "USPS").withValue("trackingNo", "EZ2000000002"))));

      assertThat(output.getRecords()).extracting(record -> record.getValueString("id"))
         .containsExactly("trk_fixture_pre_transit", "trk_fixture_in_transit");
      assertThat(output.getRecords()).allSatisfy(record -> assertThat(record.getErrors()).isNullOrEmpty());
      assertThat(requests).hasSize(2);
      assertRequest(requests.get(0), "EZ1000000001", EXPECTED_AUTHORIZATION);
      assertRequest(requests.get(1), "EZ2000000002", EXPECTED_AUTHORIZATION);
   }



   /*******************************************************************************
    ** Empty input performs no HTTP work.
    *******************************************************************************/
   @Test
   void testPostTrackerEmptyInput() throws QException
   {
      InsertOutput output = new InsertAction().execute(new InsertInput("easypostTracker").withRecords(List.of()));
      assertThat(output.getRecords()).isEmpty();
      assertThat(requests).isEmpty();
   }



   /*******************************************************************************
    ** The actual wrong authentication header produces a retained row error.
    *******************************************************************************/
   @Test
   void testPostTrackerBadApiKey() throws QException
   {
      ((APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.EASYPOST_BACKEND_NAME)).setUsername("not-valid");
      QRecord output = new InsertAction().execute(new InsertInput("easypostTracker").withRecord(
         new QRecord().withValue("carrier", "USPS").withValue("trackingNo", "EZ1000000001"))).getRecords().get(0);

      assertNull(output.getValue("id"));
      assertThat(output.getErrorsAsString()).contains("401", "AUTHENTICATION_ERROR");
      assertThat(requests).hasSize(1);
      assertRequest(requests.get(0), "EZ1000000001", "Basic " + Base64.getEncoder().encodeToString("not-valid:".getBytes(StandardCharsets.UTF_8)));
   }



   /*******************************************************************************
    ** An authenticated provider validation error cannot look like a successful insert.
    *******************************************************************************/
   @Test
   void testPostTrackerError() throws QException
   {
      QRecord output = new InsertAction().execute(new InsertInput("easypostTracker").withRecord(
         new QRecord().withValue("carrier", "USPS").withValue("trackingNo", "Not-Valid-Tracking-No"))).getRecords().get(0);

      assertNull(output.getValue("id"));
      assertThat(output.getErrorsAsString()).contains("422", "TRACKER.INVALID");
      assertThat(requests).hasSize(1);
      assertRequest(requests.get(0), "Not-Valid-Tracking-No", EXPECTED_AUTHORIZATION);
   }



   /*******************************************************************************
    ** Validate captured wire data on the test thread, independent of adapter helpers.
    *******************************************************************************/
   private void assertRequest(CapturedRequest request, String trackingNumber, String authorization)
   {
      assertEquals("POST", request.method());
      assertEquals("/v2/trackers", request.path());
      assertEquals(authorization, request.authorization());
      assertThat(request.contentType()).startsWith("application/json");
      assertEquals(Map.of("tracker", Map.of("carrier", "USPS", "tracking_code", trackingNumber)), new JSONObject(request.body()).toMap());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void respond(HttpExchange exchange) throws IOException
   {
      CapturedRequest request = new CapturedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
         exchange.getRequestHeaders().getFirst("Authorization"), exchange.getRequestHeaders().getFirst("Content-Type"),
         new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      requests.add(request);
      int status = 201;
      JSONObject response;
      if(!EXPECTED_AUTHORIZATION.equals(request.authorization()))
      {
         status = 401;
         response = new JSONObject().put("error", new JSONObject().put("message", "AUTHENTICATION_ERROR"));
      }
      else
      {
         JSONObject body = new JSONObject(request.body());
         String trackingNumber = body.getJSONObject("tracker").getString("tracking_code");
         String id = switch(trackingNumber)
         {
            case "EZ1000000001" -> "trk_fixture_pre_transit";
            case "EZ2000000002" -> "trk_fixture_in_transit";
            case "EZ4000000004" -> "trk_fixture_delivered";
            default -> null;
         };
         if(id == null)
         {
            status = 422;
            response = new JSONObject().put("error", new JSONObject().put("message", "TRACKER.INVALID"));
         }
         else
         {
            response = new JSONObject().put("id", id);
         }
      }
      byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length);
      try(exchange)
      {
         exchange.getResponseBody().write(bytes);
      }
   }



   /*******************************************************************************
    ** Immutable wire data safely published by the fixture to the test thread.
    *******************************************************************************/
   private record CapturedRequest(String method, String path, String authorization, String contentType, String body)
   {
   }
}
