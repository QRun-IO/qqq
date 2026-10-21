/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Regional preferences are available through the sample application's server.
 *******************************************************************************/
class SamplePreferencesTest
{
   /*******************************************************************************
    ** Defaults, partial writes and persisted reloads use the sample H2 schema.
    *******************************************************************************/
   @Test
   void testRegionalPreferences() throws Exception
   {
      SampleJavalinServer server = new SampleJavalinServer(new SampleMetaDataProvider()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QInstance defineQInstance() throws QException
         {
            return SampleMetaDataProvider.defineTestInstance();
         }
      });
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      try
      {
         server.start();
         try(HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build())
         {
            URI uri = URI.create("http://localhost:" + service.get().port() + "/qqq/v1/preferences/regional");
            JSONObject initial = request(client, uri, "GET", null);
            assertEquals("regional", initial.getString("name"));
            assertEquals("Regional settings", initial.getString("label"));
            JSONObject defaults = initial.getJSONObject("values");
            assertEquals("America/Chicago", defaults.getString("timeZone"));
            assertEquals("ISO", defaults.getString("dateFormat"));
            assertEquals("SUNDAY", defaults.getString("weekStartsOn"));
            JSONObject changed = request(client, uri, "PATCH", "{\"values\":{\"timeZone\":\"Europe/London\",\"dateFormat\":\"EU\",\"weekStartsOn\":\"MONDAY\"}}");
            assertEquals("Europe/London", changed.getJSONObject("values").getString("timeZone"));
            request(client, uri, "PATCH", "{\"values\":{\"dateFormat\":\"US\"}}");
            JSONObject reloaded = request(client, uri, "GET", null).getJSONObject("values");
            assertEquals("Europe/London", reloaded.getString("timeZone"));
            assertEquals("US", reloaded.getString("dateFormat"));
            assertEquals("MONDAY", reloaded.getString("weekStartsOn"));
         }
      }
      finally
      {
         server.stop();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject request(HttpClient client, URI uri, String method, String body) throws Exception
   {
      HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
         .header("Content-Type", "application/json")
         .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode(), response.body());
      return new JSONObject(response.body());
   }
}
