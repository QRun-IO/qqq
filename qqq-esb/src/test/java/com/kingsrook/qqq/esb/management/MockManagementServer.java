/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.management;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;


/*******************************************************************************
 * A mock broker management HTTP server, for the adapter tests.
 *
 * Answers each request with the next canned response (in the order they were
 * added - or a 500, once they run out), and records every request, so tests
 * can assert exactly what an adapter sent.
 *******************************************************************************/
public class MockManagementServer implements AutoCloseable
{
   private final HttpServer                            server;
   private final ConcurrentLinkedQueue<CannedResponse> responses = new ConcurrentLinkedQueue<>();
   private final List<RecordedRequest>                 requests  = new CopyOnWriteArrayList<>();



   /*******************************************************************************
    ** Start the server, on a free localhost port.
    *******************************************************************************/
   public MockManagementServer() throws IOException
   {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/", this::handle);
      server.start();
   }



   /*******************************************************************************
    ** The server's base URL, e.g., http://127.0.0.1:54321 (no trailing slash).
    *******************************************************************************/
   public String getBaseUrl()
   {
      return ("http://127.0.0.1:" + server.getAddress().getPort());
   }



   /*******************************************************************************
    ** Add a canned response (an empty or null body sends no body).
    *******************************************************************************/
   public MockManagementServer withResponse(int status, String body)
   {
      responses.add(new CannedResponse(status, body));
      return (this);
   }



   /*******************************************************************************
    ** The requests received so far, in order.
    *******************************************************************************/
   public List<RecordedRequest> getRequests()
   {
      return (new ArrayList<>(requests));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void close()
   {
      server.stop(0);
   }



   /*******************************************************************************
    ** Record the request, then send the next canned response.
    *******************************************************************************/
   private void handle(HttpExchange exchange) throws IOException
   {
      Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, values.isEmpty() ? null : values.get(0)));

      String body;
      try(InputStream inputStream = exchange.getRequestBody())
      {
         body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      }

      requests.add(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(), headers, body));

      CannedResponse response = responses.poll();
      if(response == null)
      {
         response = new CannedResponse(500, "{\"error\":\"no canned response left\"}");
      }

      byte[] bytes = response.body() == null ? new byte[0] : response.body().getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(response.status(), bytes.length == 0 ? -1 : bytes.length);
      if(bytes.length > 0)
      {
         try(OutputStream outputStream = exchange.getResponseBody())
         {
            outputStream.write(bytes);
         }
      }
      exchange.close();
   }



   /*******************************************************************************
    * One request, as received: method, raw (still percent-encoded) path, headers
    * (case-insensitive names; first value of each), and body.
    *******************************************************************************/
   public record RecordedRequest(String method, String rawPath, Map<String, String> headers, String body)
   {
      /*******************************************************************************
       ** A header's (first) value, or null.
       *******************************************************************************/
      public String header(String name)
      {
         return (headers.get(name));
      }
   }



   /*******************************************************************************
    * A canned response.
    *******************************************************************************/
   private record CannedResponse(int status, String body)
   {
   }

}
