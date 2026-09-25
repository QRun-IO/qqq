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

package com.kingsrook.sampleapp;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** The packaged dashboard and supported middleware routes remain available.
 *******************************************************************************/
class SampleApiNamespaceIT
{
   @TempDir
   Path directory;



   /*******************************************************************************
    ** Real data and neighboring SPA paths work in the packaged application with
    ** the Material Dashboard (selected by the acceptance profile).
    *******************************************************************************/
   @Test
   void testPackagedApiAndDashboardRoutes() throws Exception
   {
      try(PackagedSampleServer server = PackagedSampleServer.start(SampleJavalinServer.class, directory, List.of());
          HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
      {
         URI base = server.awaitReady();
         HttpResponse<String> metadata = get(client, base, "/qqq/v1/metaData");
         assertEquals(200, metadata.statusCode());
         assertTrue(new JSONObject(metadata.body()).getJSONObject("tables").has("person"));
         HttpResponse<String> record = get(client, base, "/data/person/1");
         assertEquals(200, record.statusCode());
         assertEquals("Avery", new JSONObject(record.body()).getJSONObject("values").getString("firstName"));

         for(String path : List.of("/person/1", "/qqq-tools/help"))
         {
            HttpResponse<String> dashboard = get(client, base, path);
            assertEquals(200, dashboard.statusCode(), path);
            assertTrue(dashboard.body().contains("<html"), path);
         }
      }
   }



   /*******************************************************************************
    ** Without an explicit selection the packaged sample serves the Next dashboard:
    ** its own deep links render, other paths get its not-found page, and API
    ** routes keep their JSON responses.
    *******************************************************************************/
   @Test
   void testPackagedNextDashboardRoutes() throws Exception
   {
      try(PackagedSampleServer server = PackagedSampleServer.start(SampleJavalinServer.class, directory, List.of(), List.of("-Dqqq.javalin.frontend=next"));
          HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
      {
         URI base = server.awaitReady();
         for(String path : List.of("/", "/app", "/app/person", "/app/person/1", "/app/person/1/edit"))
         {
            HttpResponse<String> dashboard = get(client, base, path);
            assertEquals(200, dashboard.statusCode(), path);
            assertTrue(dashboard.body().contains("/_next/static/"), path);
         }

         HttpResponse<String> unknown = get(client, base, "/qqq-tools/help");
         assertEquals(404, unknown.statusCode());

         HttpResponse<String> missingTable = get(client, base, "/metaData/table/noSuchTable");
         assertEquals(404, missingTable.statusCode());
         assertTrue(missingTable.body().contains("\"error\""));
      }
   }



   /*******************************************************************************
    ** All acceptance requests stay on this test's owned ephemeral loopback server.
    *******************************************************************************/
   private HttpResponse<String> get(HttpClient client, URI base, String path) throws Exception
   {
      URI target = base.resolve(path);
      assertEquals(base.getAuthority(), target.getAuthority());
      return client.send(HttpRequest.newBuilder(target).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
   }
}
