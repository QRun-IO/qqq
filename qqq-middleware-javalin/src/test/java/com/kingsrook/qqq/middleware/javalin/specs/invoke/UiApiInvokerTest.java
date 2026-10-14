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


import java.net.http.HttpRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;


/*******************************************************************************
 ** Custom headers must not bypass the invoker's server-side identity metadata.
 *******************************************************************************/
class UiApiInvokerTest
{
   private static final List<String> RESERVED_HEADERS = List.of("Cookie", "Authorization", "X-Api-Key", "Content-Type", "Accept",
      "X-QQQ-Client", "X-QQQ-Invoked-By", "X-QQQ-UserTimezone", UiApiInProcessSessions.HEADER);



   /*******************************************************************************
    ** Custom headers reach the HTTP server; removed and null values do not.
    *******************************************************************************/
   @Test
   void testCustomHeadersReachServer() throws Exception
   {
      Javalin server = Javalin.create(config -> config.routes.get("/qqq/v1/version", context -> context.json(context.headerMap()))).start(0);
      try
      {
         UiApiRequest request = new UiApiRequest().withMethod(HttpMethod.GET).withPath("/version")
            .withHeader("X-Custom", "custom-value").withHeader("X-Removed", "old-value").withHeader("X-Removed", null)
            .withHeader("X-Absent", null);
         assertSame(request, request.withHeader("X-Second", "second-value"));
         assertEquals(List.of("X-Custom", "X-Second"), List.copyOf(request.getHeaders().keySet()));
         request.getHeaders().put("X-Null", null);

         UiApiResponse response = UiApiInvoker.forBaseUrl("http://127.0.0.1:" + server.port(), new MiddlewareVersionV1()).invoke(request);
         assertEquals(200, response.getStatus());
         Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
         response.getJson().fields().forEachRemaining(entry -> headers.put(entry.getKey(), entry.getValue().asText()));
         assertEquals("custom-value", headers.get("X-Custom"));
         assertEquals("second-value", headers.get("X-Second"));
         assertFalse(headers.containsKey("X-Removed"));
         assertFalse(headers.containsKey("X-Absent"));
         assertFalse(headers.containsKey("X-Null"));
      }
      finally
      {
         server.stop();
      }
   }



   /*******************************************************************************
    ** Every reserved header is blocked in original, lower and upper case, both
    ** with and without an invoker-owned value. Check all values, not just first.
    *******************************************************************************/
   @Test
   void testReservedHeadersAreDropped() throws Exception
   {
      UiApiInvoker invoker = UiApiInvoker.forBaseUrl("http://127.0.0.1:8000", new MiddlewareVersionV1()).withClientChannel("assistant");
      UiApiOperation operation = invoker.findOperation(HttpMethod.GET, "/version").orElseThrow();
      for(String reserved : RESERVED_HEADERS)
      {
         for(String name : List.of(reserved, reserved.toLowerCase(Locale.ROOT), reserved.toUpperCase(Locale.ROOT)))
         {
            UiApiRequest request = new UiApiRequest().withHeader(name, "forged");
            HttpRequest minimal = invoker.buildHttpRequest(operation, request);
            assertEquals(List.of("assistant"), minimal.headers().allValues("X-QQQ-Client"));
            assertEquals(List.of("in-process"), minimal.headers().allValues("X-QQQ-Invoked-By"));
            for(String absent : List.of("Cookie", "Authorization", "X-Api-Key", "Content-Type", "Accept", "X-QQQ-UserTimezone"))
            {
               assertEquals(List.of(), minimal.headers().allValues(absent), name + " must not supply " + absent);
            }

            request.withCredentials(new UiApiCredentials("trusted-uuid", "trusted-id", "Bearer trusted", "trusted-key"))
               .withJsonBody(Map.of()).withUserTimezone("America/Chicago").withInvokedBy("trusted-caller").withClientDetail("turn-1");
            HttpRequest populated = invoker.buildHttpRequest(operation, request);
            assertEquals(List.of("sessionId=trusted-id; sessionUUID=trusted-uuid"), populated.headers().allValues("Cookie"));
            assertEquals(List.of("Bearer trusted"), populated.headers().allValues("Authorization"));
            assertEquals(List.of("trusted-key"), populated.headers().allValues("X-Api-Key"));
            assertEquals(List.of("application/json"), populated.headers().allValues("Content-Type"));
            assertEquals(List.of(), populated.headers().allValues("Accept"));
            assertEquals(List.of("America/Chicago"), populated.headers().allValues("X-QQQ-UserTimezone"));
            assertEquals(List.of("trusted-caller"), populated.headers().allValues("X-QQQ-Invoked-By"));
            assertEquals(List.of("assistant/turn-1"), populated.headers().allValues("X-QQQ-Client"));
         }
      }
   }
}
