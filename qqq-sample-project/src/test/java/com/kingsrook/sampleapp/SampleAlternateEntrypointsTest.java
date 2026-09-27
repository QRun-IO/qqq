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

package com.kingsrook.sampleapp;


import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Exercise alternate sample launchers in the unit-test coverage lifecycle.
 *******************************************************************************/
class SampleAlternateEntrypointsTest
{
   @TempDir
   Path directory;

   private SampleJavalinServer server;
   private String previousMockAuthentication;
   private String previousEsbPort;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousMockAuthentication = System.getProperty("qqq.sample.mockAuthentication");
      previousEsbPort = System.getProperty("qqq.sample.esb.port");
      System.setProperty("qqq.sample.mockAuthentication", "true");
      try(ServerSocket socket = new ServerSocket(0))
      {
         System.setProperty("qqq.sample.esb.port", String.valueOf(socket.getLocalPort()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      if(server != null)
      {
         server.stop();
      }
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
      restoreProperty("qqq.sample.mockAuthentication", previousMockAuthentication);
      restoreProperty("qqq.sample.esb.port", previousEsbPort);
   }



   /*******************************************************************************
    ** A directory passed to the config launcher augments the running app.
    *******************************************************************************/
   @Test
   void configLauncherServesAdditionalMetadata() throws Exception
   {
      Files.writeString(directory.resolve("configuredApp.json"), """
         {"class":"QAppMetaData","version":1,"name":"configuredApp","label":"Configured App",
          "sections":[{"name":"records","tables":["person"]}]}
         """);
      server = new ConfigFileBasedSampleJavalinServer(directory.toString());
      URI base = startServer();

      HttpResponse<String> response = get(base.resolve("/metaData"));
      assertEquals(200, response.statusCode());
      JSONObject apps = new JSONObject(response.body()).getJSONObject("apps");
      assertTrue(apps.has("configuredApp"));
      assertTrue(apps.has("peopleApp"));
   }



   /*******************************************************************************
    ** The isolated launcher keeps the private SPA behind its route auth.
    *******************************************************************************/
   @Test
   void isolatedSpaLauncherSeparatesPublicAndPrivateRoutes() throws Exception
   {
      server = new IsolatedSpaServer();
      URI base = startServer();

      HttpResponse<String> publicPage = get(base.resolve("/welcome/details"));
      assertEquals(200, publicPage.statusCode());
      assertTrue(publicPage.body().contains("Welcome to the public site"));
      assertFalse(publicPage.body().contains("Welcome to the private site"));

      HttpResponse<String> privatePage = get(base.resolve("/private/deep/link"));
      assertEquals(401, privatePage.statusCode());
      assertFalse(privatePage.body().contains("Welcome to the private site"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private URI startServer() throws Exception
   {
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      server.start();
      return URI.create("http://localhost:" + service.get().port());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> get(URI uri) throws Exception
   {
      try(HttpClient client = HttpClient.newHttpClient())
      {
         return client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void restoreProperty(String name, String value)
   {
      if(value == null)
      {
         System.clearProperty(name);
      }
      else
      {
         System.setProperty(name, value);
      }
   }
}
