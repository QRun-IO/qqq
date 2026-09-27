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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Launch the actual bundled SPA example from an empty working directory and
 ** verify HTTP isolation, authentication and client-side navigation in a browser.
 *******************************************************************************/
class SampleIsolatedSpaIT
{
   private static final String DEMO_AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString("sample:sample-only".getBytes(StandardCharsets.UTF_8));

   @TempDir
   Path directory;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPackagedIsolatedSpas() throws Exception
   {
      try(PackagedSampleServer server = PackagedSampleServer.start(IsolatedSpaServer.class, directory, List.of());
          HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
      {
         URI base = server.awaitReady();
         for(String path : List.of("/", "/index.html", "/welcome/details", "/privateer", "/private-area"))
         {
            HttpResponse<String> response = get(client, base.resolve(path), null);
            assertEquals(200, response.statusCode(), path + ": " + response.body());
            assertTrue(response.body().contains("<h1>Welcome to the public site</h1>"));
            assertFalse(response.body().contains("<h1>Welcome to the private site</h1>"));
         }

         for(String path : List.of("/private", "/private/", "/private/index.html", "/private/deep/link", "/private/app.js"))
         {
            HttpResponse<String> denied = get(client, base.resolve(path), null);
            assertEquals(401, denied.statusCode(), path + ": " + denied.body());
            assertTrue(denied.headers().firstValue("WWW-Authenticate").orElse("").startsWith("Basic "));
            assertFalse(denied.body().contains("<h1>Welcome to the private site</h1>"));
            assertFalse(denied.body().contains("const sampleSite"));
         }

         for(String invalid : List.of("Basic " + Base64.getEncoder().encodeToString("sample:wrong".getBytes(StandardCharsets.UTF_8)),
            "Basic invalid-base64", "Basic " + Base64.getEncoder().encodeToString("missing-colon".getBytes(StandardCharsets.UTF_8))))
         {
            assertEquals(401, get(client, base.resolve("/private/deep/link"), invalid).statusCode());
         }

         for(String path : List.of("/private/", "/private/index.html", "/private/deep/link"))
         {
            HttpResponse<String> allowed = get(client, base.resolve(path), DEMO_AUTHORIZATION);
            assertEquals(200, allowed.statusCode(), path + ": " + allowed.body());
            assertTrue(allowed.body().contains("<h1>Welcome to the private site</h1>"));
            assertFalse(allowed.body().contains("<h1>Welcome to the public site</h1>"));
         }
         assertTrue(get(client, base.resolve("/private/deep/link"), DEMO_AUTHORIZATION).body().contains("<base href=\"/private/\">"));

         HttpResponse<String> publicAsset = get(client, base.resolve("/app.js"), null);
         assertEquals(200, publicAsset.statusCode());
         assertTrue(publicAsset.body().contains("const sampleSite = \"public\""));
         HttpResponse<String> privateAsset = get(client, base.resolve("/private/app.js"), DEMO_AUTHORIZATION);
         assertEquals(200, privateAsset.statusCode());
         assertTrue(privateAsset.body().contains("const sampleSite = \"private\""));

         for(String path : List.of("/missing.js", "/private/missing.js", "/metaData/table/noSuchTable", "/api/noSuchEndpoint"))
         {
            HttpResponse<String> missing = get(client, base.resolve(path), DEMO_AUTHORIZATION);
            assertEquals(404, missing.statusCode(), path + ": " + missing.body());
            assertFalse(missing.body().contains("<h1>Welcome to the"));
         }
         assertBrowserNavigation(base);
      }
   }



   /*******************************************************************************
    ** Relative assets must load on deep links; client routing must keep the page.
    *******************************************************************************/
   private void assertBrowserNavigation(URI base)
   {
      ChromeOptions options = new ChromeOptions();
      options.addArguments("--headless=new", "--window-size=1440,1000");
      ChromeDriver driver = new ChromeDriver(options);
      try
      {
         driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
         driver.executeCdpCommand("Network.enable", Map.of());
         driver.executeCdpCommand("Network.setExtraHTTPHeaders", Map.of("headers", Map.of("Authorization", DEMO_AUTHORIZATION)));
         WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
         for(Map.Entry<String, String> site : Map.of("/", "public", "/private/", "private").entrySet())
         {
            driver.get(base.resolve(site.getKey() + "deep/link").toString());
            wait.until(browser -> browser.findElement(By.id("current-route")).getText().equals(site.getValue() + ": " + site.getKey() + "deep/link"));
            assertEquals("Welcome to the " + site.getValue() + " site", driver.findElement(By.tagName("h1")).getText());
            driver.executeScript("window.sampleNavigationMarker = true;");
            driver.findElement(By.cssSelector("[data-route]")).click();
            wait.until(browser -> browser.findElement(By.id("current-route")).getText().equals(site.getValue() + ": " + site.getKey() + "details"));
            assertEquals(Boolean.TRUE, driver.executeScript("return window.sampleNavigationMarker;"), "Client navigation must not reload the page");
            driver.navigate().refresh();
            wait.until(browser -> browser.findElement(By.id("current-route")).getText().equals(site.getValue() + ": " + site.getKey() + "details"));
         }
      }
      finally
      {
         driver.quit();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> get(HttpClient client, URI uri, String authorization) throws Exception
   {
      HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10));
      if(authorization != null)
      {
         request.header("Authorization", authorization);
      }
      return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
   }
}
