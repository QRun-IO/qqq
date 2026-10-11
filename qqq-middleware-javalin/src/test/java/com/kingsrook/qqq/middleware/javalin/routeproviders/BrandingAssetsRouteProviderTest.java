/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.branding.QBrandingMetaData;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Fixed-role resources do not turn the overlay into a public directory.
 *******************************************************************************/
class BrandingAssetsRouteProviderTest
{
   /***************************************************************************
    ** These canonical path vectors are shared with the Node alias tests.
    ***************************************************************************/
   @Test
   void testCanonicalImagePaths()
   {
      assertEquals("/brand/logo.png", BrandingAssetsRouteProvider.imagePath("/brand/logo.png?v=1#icon"));
      assertEquals("/brand/a b.png", BrandingAssetsRouteProvider.imagePath("/brand/a%20b.png"));
      assertEquals("/brand/é.png", BrandingAssetsRouteProvider.imagePath("/brand/%C3%A9.png"));
      assertEquals("/brand/a+b.PNG", BrandingAssetsRouteProvider.imagePath("/brand/a+b.PNG"));
      assertNull(BrandingAssetsRouteProvider.imagePath(null));
      for(String value : List.of("logo.png", "https://images.example/a.png", "//images.example/a.png", "data:image/png;base64,a", "/", "/brand/../a.png", "/brand/%2e%2e/a.png", "/brand/%252e%252e/a.png", "/brand/a%2fb.png", "/brand/a%5cb.png", "/brand//a.png", "/brand/./a.png", "/brand/a%00.png", "/brand/a%ZZ.png", "/brand/a%FF.png", "/brand/index.html", "/brand/private.js", "/app/a.png", "/login/a.png", "/callback/a.png", "/_next/a.png", "/qqq/a.png", "/data/a.png", "/metaData/a.png"))
      {
         assertNull(BrandingAssetsRouteProvider.imagePath(value), value);
      }
   }



   /***************************************************************************
    ** The role, not a caller-provided path, resolves against the current instance.
    ***************************************************************************/
   @Test
   void testCurrentInstanceMethodsAndUnlistedResources() throws Exception
   {
      BrandingAssetsRouteProvider provider = new BrandingAssetsRouteProvider();
      provider.setQInstance(instance("/branding1009/logo.png", "/branding1009/icon.svg"));
      Javalin service = Javalin.create(provider::acceptJavalinConfig).start(0);
      try
      {
         HttpClient client = HttpClient.newHttpClient();
         String base = "http://127.0.0.1:" + service.port();
         HttpResponse<byte[]> image = client.send(HttpRequest.newBuilder(URI.create(base + "/qqq/branding/logo?path=/branding1009/private.js")).build(), HttpResponse.BodyHandlers.ofByteArray());
         assertEquals(200, image.statusCode());
         assertTrue(image.body().length > 20);
         HttpResponse<byte[]> head = client.send(HttpRequest.newBuilder(URI.create(base + "/qqq/branding/icon")).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofByteArray());
         assertEquals(200, head.statusCode());
         assertEquals(0, head.body().length);
         assertEquals("image/svg+xml", head.headers().firstValue("content-type").orElseThrow().split(";")[0]);
         assertEquals("default-src 'none'; sandbox", head.headers().firstValue("content-security-policy").orElseThrow());
         assertEquals("no-cache", head.headers().firstValue("cache-control").orElseThrow());
         for(String suffix : List.of("/qqq/branding/unknown", "/qqq/branding/logo/extra", "/branding1009/logo.png", "/branding1009/index.html", "/branding1009/private.js", "/login"))
         {
            assertEquals(404, client.send(HttpRequest.newBuilder(URI.create(base + suffix)).build(), HttpResponse.BodyHandlers.discarding()).statusCode(), suffix);
         }
         assertTrue(client.send(HttpRequest.newBuilder(URI.create(base + "/qqq/branding/logo")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding()).statusCode() >= 400);
         provider.setQInstance(instance("/branding1009/icon.svg", "/branding1009/missing.png"));
         assertTrue(client.send(HttpRequest.newBuilder(URI.create(base + "/qqq/branding/logo")).build(), HttpResponse.BodyHandlers.ofString()).body().startsWith("<svg"));
         assertEquals(404, client.send(HttpRequest.newBuilder(URI.create(base + "/qqq/branding/icon")).build(), HttpResponse.BodyHandlers.discarding()).statusCode());
         for(String value : List.of("/branding1009/index.html", "/branding1009/../private.png", "https://example.invalid/image.png"))
         {
            provider.setQInstance(instance(value, null));
            assertEquals(404, client.send(HttpRequest.newBuilder(URI.create(base + "/qqq/branding/logo")).build(), HttpResponse.BodyHandlers.discarding()).statusCode(), value);
         }
         provider.setQInstance(new QInstance());
         assertEquals(404, client.send(HttpRequest.newBuilder(URI.create(base + "/qqq/branding/logo")).build(), HttpResponse.BodyHandlers.discarding()).statusCode());
      }
      finally
      {
         service.stop();
      }
   }



   /***************************************************************************
    ** Declared branding is owned by the current instance.
    ***************************************************************************/
   private static QInstance instance(String logo, String icon)
   {
      QInstance instance = new QInstance();
      instance.setBranding(new QBrandingMetaData().withLogo(logo).withIcon(icon));
      return instance;
   }
}

