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
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.middleware.javalin.routeproviders.SimpleFileSystemDirectoryRouter;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
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
      Javalin server = Javalin.create(router::acceptJavalinConfig);
      try
      {
         router.acceptJavalinService(server);
         server.start(0);
         try(HttpClient client = HttpClient.newHttpClient())
         {
            String baseUrl = "http://localhost:" + server.port();
            HttpResponse<String> file = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/sample/hello.txt")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, file.statusCode());
            assertEquals("World!", file.body().strip());
            HttpResponse<String> deepLink = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/sample/deep/link")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, deepLink.statusCode());
            assertTrue(deepLink.body().strip().endsWith("hello world"));
            HttpResponse<String> missingAsset = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/sample/missing.js")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(404, missingAsset.statusCode());
         }
      }
      finally
      {
         server.stop();
         QContext.clear();
      }
   }
}
