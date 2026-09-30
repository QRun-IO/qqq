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
import java.time.Duration;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.middleware.javalin.routeproviders.SimpleFileSystemDirectoryRouter;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Exercise static resources and SPA deep links from the packaged sample JAR.
 *******************************************************************************/
class SamplePackagedRoutesIT
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPackagedStaticFilesAndSpaDeepLinks() throws Exception
   {
      assertEquals("jar", SampleJavalinServer.class.getResource("/static-site/index.html").getProtocol());
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("/sample", "static-site")
         .withSpaRootPath("/sample").withSpaRootFile("static-site/index.html");
      assertFalse(SimpleFileSystemDirectoryRouter.loadStaticFilesFromJar);
      router.setQInstance(SampleMetaDataProvider.defineTestInstance());
      Javalin server = Javalin.create(config ->
      {
         router.acceptJavalinConfig(config);
         config.routes.get("/sample/missing-record", context -> context.status(404).result("record missing"));
      });
      try
      {
         server.start(0);
         try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
         {
            String baseUrl = "http://localhost:" + server.port();
            HttpResponse<String> file = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/sample/hello.txt")).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, file.statusCode());
            assertEquals("World!", file.body().strip());
            HttpResponse<String> deepLink = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/sample/deep/link")).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, deepLink.statusCode());
            assertTrue(deepLink.body().strip().endsWith("hello world"));
            HttpResponse<String> missingAsset = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/sample/missing.js")).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(404, missingAsset.statusCode());
            assertAll(
               () ->
               {
                  HttpResponse<String> neighbor = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/sample-other/deep/link")).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
                  assertEquals(404, neighbor.statusCode());
               },
               () ->
               {
                  HttpResponse<String> missingRecord = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/sample/missing-record")).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
                  assertEquals(404, missingRecord.statusCode());
                  assertEquals("record missing", missingRecord.body());
               });
         }
      }
      finally
      {
         server.stop();
         QContext.clear();
      }
   }
}
