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

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for NextDashboardRouteProvider
 *******************************************************************************/
class NextDashboardRouteProviderTest
{
   private Javalin service;



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      if(service != null)
      {
         service.stop();
      }
   }



   /*******************************************************************************
    ** Exact entries win; otherwise the "_" placeholder directory matches.
    *******************************************************************************/
   @Test
   void testResolve()
   {
      NextDashboardRouteProvider provider = new NextDashboardRouteProvider("test-next-dashboard");
      assertEquals("index.html", provider.resolve("/"));
      assertEquals("app/index.html", provider.resolve("/app"));
      assertEquals("app/index.html", provider.resolve("/app/"));
      assertEquals("app/_/index.html", provider.resolve("/app/person"));
      assertEquals("app/_/__next._tree.txt", provider.resolve("/app/person/__next._tree.txt"));
      assertEquals("app/_/_/index.html", provider.resolve("/app/person/42/"));
      assertEquals("app/_/_/edit/index.html", provider.resolve("/app/person/42/edit"));
      assertEquals("app/developer/index.html", provider.resolve("/app/developer"));
      assertEquals("app/_/index.html", provider.resolve("/app/a%20b"));
      assertEquals("_next/static/chunks/main.js", provider.resolve("/_next/static/chunks/main.js"));
      assertNull(provider.resolve("/app/person/42/unknown/deeper"));
      assertNull(provider.resolve("/metaData"));
      assertNull(provider.resolve("/app/../index.html"));
      assertNull(provider.resolve("/app/%2e%2e/index.html"));
      assertNull(provider.resolve("/app/person/42/..%2Fedit"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingExportFailsFast()
   {
      NextDashboardRouteProvider provider = new NextDashboardRouteProvider("no-such-next-dashboard");
      assertThrows(IllegalStateException.class, () -> Javalin.create(provider::acceptJavalinConfig));
   }



   /*******************************************************************************
    ** Served only when no endpoint matched: API routes, including their own 404s,
    ** keep their responses.
    *******************************************************************************/
   @Test
   void testServing() throws Exception
   {
      NextDashboardRouteProvider provider = new NextDashboardRouteProvider("test-next-dashboard");
      service = Javalin.create(config ->
      {
         config.routes.get("/data/{table}/{id}", context -> context.status(404).json("{\"error\":\"not found\"}"));
         config.routes.get("/metaData", context -> context.result("metadata"));
         provider.acceptJavalinConfig(config);
      }).start(0);

      HttpClient client = HttpClient.newHttpClient();
      String     base   = "http://localhost:" + service.port();

      HttpResponse<String> record = client.send(HttpRequest.newBuilder(URI.create(base + "/app/person/1")).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(200, record.statusCode());
      assertEquals("<html>record</html>\n", record.body());
      assertThat(record.headers().firstValue("Content-Type").orElse("")).startsWith("text/html");
      assertEquals("no-cache", record.headers().firstValue("Cache-Control").orElse(""));

      HttpResponse<String> asset = client.send(HttpRequest.newBuilder(URI.create(base + "/_next/static/chunks/main.js")).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(200, asset.statusCode());
      assertThat(asset.headers().firstValue("Cache-Control").orElse("")).contains("immutable");
      assertThat(asset.headers().firstValue("Content-Type").orElse("")).startsWith("text/javascript");

      HttpResponse<String> api = client.send(HttpRequest.newBuilder(URI.create(base + "/data/person/99")).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(404, api.statusCode());
      assertThat(api.body()).contains("not found").doesNotContain("<html>");

      assertEquals("metadata", client.send(HttpRequest.newBuilder(URI.create(base + "/metaData")).build(), HttpResponse.BodyHandlers.ofString()).body());

      HttpResponse<String> unknownPage = client.send(HttpRequest.newBuilder(URI.create(base + "/app/person/1/unknown/deeper")).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(404, unknownPage.statusCode());
      assertEquals("<html>not found</html>\n", unknownPage.body());

      HttpResponse<String> unknownFile = client.send(HttpRequest.newBuilder(URI.create(base + "/missing.js")).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(404, unknownFile.statusCode());
      assertThat(unknownFile.body()).doesNotContain("<html>");

      HttpResponse<String> post = client.send(HttpRequest.newBuilder(URI.create(base + "/app/person")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(404, post.statusCode());
      assertThat(post.body()).doesNotContain("<html>");
   }
}
