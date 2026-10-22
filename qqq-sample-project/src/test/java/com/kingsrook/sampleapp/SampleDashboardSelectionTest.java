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
