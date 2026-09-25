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
import java.util.concurrent.atomic.AtomicReference;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** The sample serves the Next dashboard by default and the Material Dashboard
 ** when it is selected explicitly (QRun-IO/qqq#649).
 *******************************************************************************/
class SampleDashboardSelectionTest
{
   private SampleJavalinServer server;



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
      System.clearProperty("qqq.javalin.frontend");
      System.clearProperty("qqq.sample.mockAuthentication");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNextIsTheDefault() throws Exception
   {
      String base = start();
      HttpResponse<String> root = get(base + "/");
      assertEquals(200, root.statusCode());
      assertThat(root.body()).contains("/_next/static/");

      HttpResponse<String> deepLink = get(base + "/app/person/1");
      assertEquals(200, deepLink.statusCode());
      assertThat(deepLink.body()).contains("/_next/static/");

      HttpResponse<String> api = get(base + "/metaData/table/noSuchTable");
      assertEquals(404, api.statusCode());
      assertThat(api.body()).contains("\"error\"").doesNotContain("<html");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMaterialWhenSelected() throws Exception
   {
      System.setProperty("qqq.javalin.frontend", "material");
      String base = start();
      HttpResponse<String> root = get(base + "/");
      assertEquals(200, root.statusCode());
      assertThat(root.body()).contains("static/js/main").doesNotContain("/_next/static/");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String start() throws Exception
   {
      System.setProperty("qqq.sample.mockAuthentication", "true");
      AtomicReference<Javalin> service = new AtomicReference<>();
      server = new SampleJavalinServer();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      server.start();
      return ("http://localhost:" + service.get().port());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> get(String url) throws Exception
   {
      try(HttpClient client = HttpClient.newHttpClient())
      {
         return (client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString()));
      }
   }
}
