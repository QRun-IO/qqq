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

package com.kingsrook.qqq.middleware.javalin;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.javalin.TestUtils;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.middleware.javalin.routeproviders.IsolatedSpaRouteProvider;
import com.kingsrook.qqq.middleware.javalin.routeproviders.SimpleFileSystemDirectoryRouter;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import io.javalin.http.HttpStatus;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for QApplicationJavalinServer 
 *******************************************************************************/
class QApplicationJavalinServerTest
{
   private static final int PORT = 6265;

   private QApplicationJavalinServer javalinServer;



   /***************************************************************************
    **
    ***************************************************************************/
   private static AbstractQQQApplication getQqqApplication()
   {
      return new TestApplication();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach() throws IOException
   {
      javalinServer.stop();
      TestApplication.callCount = 0;
      System.clearProperty("qqq.javalin.enableStaticFilesFromJar");
      Unirest.config().reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithLegacyImplementation() throws QException
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withPort(PORT)
         .withServeFrontendMaterialDashboard(false);
      javalinServer.start();

      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/metaData").asString();
      assertEquals(200, response.getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithoutLegacyImplementation() throws QException
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withPort(PORT)
         .withServeLegacyUnversionedMiddlewareAPI(false)
         .withServeFrontendMaterialDashboard(false);
      javalinServer.start();

      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/metaData").asString();
      assertEquals(404, response.getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithVersionedImplementation() throws QException
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withPort(PORT)
         .withMiddlewareVersionList(List.of(new MiddlewareVersionV1()))
         .withServeFrontendMaterialDashboard(false);
      javalinServer.start();

      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/qqq/v1/metaData").asString();
      assertEquals(200, response.getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithoutHotSwap() throws QException
   {
      testWithOrWithoutHotSwap(false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithHotSwap() throws QException
   {
      testWithOrWithoutHotSwap(true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void testWithOrWithoutHotSwap(boolean withHotSwap) throws QException
   {
      System.setProperty("qqq.javalin.hotSwapInstance", String.valueOf(withHotSwap));
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withPort(PORT)
         .withMiddlewareVersionList(List.of(new MiddlewareVersionV1()))
         .withMillisBetweenHotSwaps(0)
         .withServeFrontendMaterialDashboard(false);
      javalinServer.start();
      System.clearProperty("qqq.javalin.hotSwapInstance");
      assertThat(TestApplication.callCount).isEqualTo(1);

      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/qqq/v1/metaData").asString();
      assertEquals(200, response.getStatus());

      response = Unirest.get("http://localhost:" + PORT + "/qqq/v1/metaData").asString();
      assertEquals(200, response.getStatus());
      JSONObject metaData   = new JSONObject(response.getBody());
      JSONObject tables     = metaData.getJSONObject("tables");
      String     aTableName = tables.keySet().iterator().next();
      JSONObject aTable     = tables.getJSONObject(aTableName);

      if(withHotSwap)
      {
         assertThat(aTable.getString("label")).doesNotEndWith("1");
         assertThat(TestApplication.callCount).isGreaterThanOrEqualTo(1);
      }
      else
      {
         assertThat(aTable.getString("label")).endsWith("1");
         assertThat(TestApplication.callCount).isEqualTo(1);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStaticRouter() throws Exception
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withServeFrontendMaterialDashboard(false)
         .withPort(PORT);
      javalinServer.start();

      Unirest.config().setDefaultResponseEncoding("UTF-8");
      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/statically-served/foo.html").asString();
      assertEquals("Foo? Bar!", response.getBody());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStaticRouterFilesFromExternal() throws Exception
   {
      System.setProperty("qqq.javalin.enableStaticFilesFromJar", "false");

      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withServeFrontendMaterialDashboard(false)
         .withPort(PORT);
      javalinServer.start();

      Unirest.config().setDefaultResponseEncoding("UTF-8");
      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/statically-served/foo.html").asString();
      assertEquals("Foo? Bar!", response.getBody());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStaticRouterFilesFromClasspath() throws Exception
   {
      System.setProperty("qqq.javalin.enableStaticFilesFromJar", "true");

      javalinServer = new QApplicationJavalinServer(new QApplicationJavalinServerTest.TestApplication())
         .withServeFrontendMaterialDashboard(false)
         .withPort(PORT)
         .withAdditionalRouteProvider(new SimpleFileSystemDirectoryRouter("/statically-served-from-jar", "static-site-from-jar/"));

      javalinServer.start();

      Unirest.config().setDefaultResponseEncoding("UTF-8");
      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/statically-served-from-jar/foo-in-jar.html").asString();
      assertEquals("Foo in a Jar!\n", response.getBody());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthenticatedStaticRouter() throws Exception
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withServeFrontendMaterialDashboard(false)
         .withPort(PORT);
      javalinServer.start();

      Unirest.config().setDefaultResponseEncoding("UTF-8")
         .reset()
         .followRedirects(false);

      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/protected-statically-served/foo.html")
         .header("Authorization", "Bearer Deny")
         .asString();

      assertEquals(HttpStatus.FOUND.getCode(), response.getStatus());
      assertThat(response.getHeaders().getFirst("Location")).contains("createMockSession");

      response = Unirest.get("http://localhost:" + PORT + "/protected-statically-served/foo.html")
         .asString();
      assertEquals(HttpStatus.OK.getCode(), response.getStatus());
      assertEquals("Foo? Bar!", response.getBody());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessRouter() throws Exception
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withServeFrontendMaterialDashboard(false)
         .withPort(PORT);
      javalinServer.start();

      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/served-by-process/foo.html").asString();
      assertEquals(200, response.getStatus());
      assertEquals("So you've done a GET for: /served-by-process/foo.html", response.getBody());

      response = Unirest.post("http://localhost:" + PORT + "/served-by-process/foo.html").asString();
      assertEquals(200, response.getStatus());
      assertEquals("So you've done a POST for: /served-by-process/foo.html", response.getBody());
      assertEquals("Yes, Test", response.getHeaders().getFirst("X-Test"));

      response = Unirest.put("http://localhost:" + PORT + "/served-by-process/foo.html?requestedRedirect=google.com").asString();
      assertEquals(302, response.getStatus());
      assertEquals("google.com", response.getHeaders().getFirst("Location"));

      HttpResponse<byte[]> responseBytes = Unirest.delete("http://localhost:" + PORT + "/served-by-process/foo.html?respondInBytes=true").asBytes();
      assertEquals(200, responseBytes.getStatus());
      assertArrayEquals("So you've done a DELETE for: /served-by-process/foo.html".getBytes(StandardCharsets.UTF_8), responseBytes.getBody());

      response = Unirest.get("http://localhost:" + PORT + "/served-by-process/foo.html?noResponse=true").asString();
      assertEquals(200, response.getStatus());
      assertEquals("", response.getBody());

      response = Unirest.get("http://localhost:" + PORT + "/served-by-process/foo.html?doThrow=true").asString();
      assertEquals(500, response.getStatus());
      assertThat(response.getBody()).contains("Test Exception");

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthenticatedProcessRouter() throws Exception
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withServeFrontendMaterialDashboard(false)
         .withPort(PORT);
      javalinServer.start();

      Unirest.config().setDefaultResponseEncoding("UTF-8")
         .reset()
         .followRedirects(false);

      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/protected-served-by-process/foo.html")
         .header("Authorization", "Bearer Deny")
         .asString();

      assertEquals(HttpStatus.FOUND.getCode(), response.getStatus());
      assertThat(response.getHeaders().getFirst("Location")).contains("createMockSession");

      response = Unirest.get("http://localhost:" + PORT + "/protected-served-by-process/foo.html")
         .asString();
      assertEquals(200, response.getStatus());
      assertEquals("So you've done a GET for: /protected-served-by-process/foo.html", response.getBody());
   }



   /*******************************************************************************
    ** Test Material Dashboard at custom hosted path
    ** 
    ** Note: This test verifies the API but doesn't fully test because 
    ** material-dashboard resources don't exist in test classpath
    *******************************************************************************/
   @Test
   void testMaterialDashboardAtCustomPath() throws QException
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withPort(PORT)
         .withServeFrontendMaterialDashboard(false)  // Disable to avoid classpath errors
         .withServeLegacyUnversionedMiddlewareAPI(false)
         .withFrontendMaterialDashboardHostedPath("/app");
      javalinServer.start();

      // Verify the configuration is accepted
      assertEquals("/app", javalinServer.getFrontendMaterialDashboardHostedPath());
      
      // Root should not serve anything
      HttpResponse<String> rootResponse = Unirest.get("http://localhost:" + PORT + "/").asString();
      assertEquals(404, rootResponse.getStatus());
   }



   /*******************************************************************************
    ** CRITICAL TEST: Test deep linking with multiple SPAs
    ** 
    ** This is the blocker feature - users must be able to navigate directly to 
    ** deep routes within SPAs (e.g., /app/users/123) without getting 404s.
    *******************************************************************************/
   @Test
   void testMultipleSPAsWithDeepLinking() throws Exception
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withPort(PORT)
         .withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(false)
         .withAdditionalRouteProvider(
            new SimpleFileSystemDirectoryRouter("/app", "test-spa-app/")
               .withSpaRootPath("/app")
               .withSpaRootFile("test-spa-app/index.html"))
         .withAdditionalRouteProvider(
            new SimpleFileSystemDirectoryRouter("/admin", "test-spa-admin/")
               .withSpaRootPath("/admin")
               .withSpaRootFile("test-spa-admin/index.html"));
      javalinServer.start();

      Unirest.config().setDefaultResponseEncoding("UTF-8");

      /////////////////////////////////////////////////////////////////////////////
      // Test 1: Root path of first SPA should work                             //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> appRootResponse = Unirest.get("http://localhost:" + PORT + "/app/").asString();
      assertEquals(200, appRootResponse.getStatus());
      assertThat(appRootResponse.getBody()).contains("Test SPA - App");
      assertThat(appRootResponse.getBody()).contains("<html");

      /////////////////////////////////////////////////////////////////////////////
      // Test 2: Deep link in first SPA should serve index.html (NOT 404)       //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> appDeepLink1 = Unirest.get("http://localhost:" + PORT + "/app/users/123").asString();
      assertEquals(200, appDeepLink1.getStatus(), "Deep link /app/users/123 should return 200");
      assertThat(appDeepLink1.getBody()).contains("<html");
      assertThat(appDeepLink1.getBody()).contains("Test SPA - App");
      assertThat(appDeepLink1.getBody()).doesNotContain("404");

      /////////////////////////////////////////////////////////////////////////////
      // Test 3: Another deep link with multiple segments                       //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> appDeepLink2 = Unirest.get("http://localhost:" + PORT + "/app/users/123/edit/profile").asString();
      assertEquals(200, appDeepLink2.getStatus(), "Deep link /app/users/123/edit/profile should return 200");
      assertThat(appDeepLink2.getBody()).contains("<html");
      assertThat(appDeepLink2.getBody()).contains("Test SPA - App");

      /////////////////////////////////////////////////////////////////////////////
      // Test 4: Deep link with query parameters                                //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> appDeepLink3 = Unirest.get("http://localhost:" + PORT + "/app/search?q=test&filter=active").asString();
      assertEquals(200, appDeepLink3.getStatus(), "Deep link with query params should return 200");
      assertThat(appDeepLink3.getBody()).contains("<html");

      /////////////////////////////////////////////////////////////////////////////
      // Test 5: Root path of second SPA should work                            //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> adminRootResponse = Unirest.get("http://localhost:" + PORT + "/admin/").asString();
      assertEquals(200, adminRootResponse.getStatus());
      assertThat(adminRootResponse.getBody()).contains("Test SPA - Admin");
      assertThat(adminRootResponse.getBody()).contains("<html");

      /////////////////////////////////////////////////////////////////////////////
      // Test 6: Deep link in second SPA should also work                       //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> adminDeepLink = Unirest.get("http://localhost:" + PORT + "/admin/settings/profile").asString();
      assertEquals(200, adminDeepLink.getStatus(), "Deep link /admin/settings/profile should return 200");
      assertThat(adminDeepLink.getBody()).contains("<html");
      assertThat(adminDeepLink.getBody()).contains("Test SPA - Admin");
      assertThat(adminDeepLink.getBody()).doesNotContain("404");

      /////////////////////////////////////////////////////////////////////////////
      // Test 7: Static asset in first SPA should load (NOT serve index.html)   //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> appJs = Unirest.get("http://localhost:" + PORT + "/app/assets/app.js").asString();
      assertEquals(200, appJs.getStatus(), "Static asset app.js should return 200");
      assertThat(appJs.getBody()).contains("console.log");
      assertThat(appJs.getBody()).doesNotContain("<html");
      assertThat(appJs.getBody()).doesNotContain("<!DOCTYPE");

      /////////////////////////////////////////////////////////////////////////////
      // Test 8: Static asset in second SPA should load                         //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> adminCss = Unirest.get("http://localhost:" + PORT + "/admin/assets/admin.css").asString();
      assertEquals(200, adminCss.getStatus(), "Static asset admin.css should return 200");
      assertThat(adminCss.getBody()).contains("font-family");
      assertThat(adminCss.getBody()).doesNotContain("<html");

      /////////////////////////////////////////////////////////////////////////////
      // Test 9: Paths with and without trailing slashes should both work       //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> withSlash = Unirest.get("http://localhost:" + PORT + "/app/users/").asString();
      HttpResponse<String> withoutSlash = Unirest.get("http://localhost:" + PORT + "/app/users").asString();
      assertEquals(200, withSlash.getStatus());
      assertEquals(200, withoutSlash.getStatus());
      assertThat(withSlash.getBody()).contains("<html");
      assertThat(withoutSlash.getBody()).contains("<html");
   }



   /*******************************************************************************
    ** Test that static asset detection correctly identifies assets vs SPA routes
    *******************************************************************************/
   @Test
   void testStaticAssetDetectionInSPA() throws Exception
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withPort(PORT)
         .withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(false)
         .withAdditionalRouteProvider(
            new SimpleFileSystemDirectoryRouter("/app", "test-spa-app/")
               .withSpaRootPath("/app")
               .withSpaRootFile("test-spa-app/index.html"));
      javalinServer.start();

      Unirest.config().setDefaultResponseEncoding("UTF-8");

      /////////////////////////////////////////////////////////////////////
      // These paths should serve index.html (SPA routes)               //
      /////////////////////////////////////////////////////////////////////
      String[] spaRoutes = {
         "/app/users",
         "/app/users/123",
         "/app/dashboard",
         "/app/settings/profile"
      };

      for(String route : spaRoutes)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + route).asString();
         assertEquals(200, response.getStatus(), "SPA route " + route + " should return 200");
         assertThat(response.getBody()).contains("<html");
         assertThat(response.getBody()).contains("Test SPA - App");
      }

      /////////////////////////////////////////////////////////////////////
      // These paths should be treated as assets (even if they 404)     //
      /////////////////////////////////////////////////////////////////////
      String[] assetPaths = {
         "/app/assets/missing.js",
         "/app/assets/missing.css",
         "/app/static/image.png"
      };

      for(String assetPath : assetPaths)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + assetPath).asString();
         // Assets may 404 if they don't exist, but they should NOT serve index.html
         if(response.getStatus() == 200)
         {
            // If the asset exists, it should not be HTML
            assertThat(response.getBody()).doesNotContain("<html");
         }
         // Either way, it should not serve the SPA index
         assertThat(response.getBody()).doesNotContain("Test SPA - App");
      }
   }



   /*******************************************************************************
    ** Test SPA configuration without spaRootPath (should work as regular static site)
    *******************************************************************************/
   @Test
   void testStaticSiteWithoutSPAConfiguration() throws Exception
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withPort(PORT)
         .withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(false)
         .withAdditionalRouteProvider(
            new SimpleFileSystemDirectoryRouter("/static", "static-site/"));
      javalinServer.start();

      Unirest.config().setDefaultResponseEncoding("UTF-8");

      // Existing file should work
      HttpResponse<String> existingFile = Unirest.get("http://localhost:" + PORT + "/static/foo.html").asString();
      assertEquals(200, existingFile.getStatus());
      assertThat(existingFile.getBody()).contains("Foo? Bar!");

      // Non-existing file should 404 (NOT serve index.html because no SPA config)
      HttpResponse<String> missingFile = Unirest.get("http://localhost:" + PORT + "/static/nonexistent").asString();
      assertEquals(404, missingFile.getStatus());
   }



   /*******************************************************************************
    ** CRITICAL TEST: Verify that API routes and SPA deep linking coexist properly
    ** 
    ** This test ensures that:
    ** 1. API routes like /metaData, /data/* always work (never caught by SPA handler)
    ** 2. SPA deep linking works for unmatched routes
    ** 3. The two systems don't interfere with each other
    *******************************************************************************/
   @Test
   void testAPIsAndSPAsCoexist() throws Exception
   {
      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withPort(PORT)
         .withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(true)  // Enable legacy API routes
         .withAdditionalRouteProvider(
            new SimpleFileSystemDirectoryRouter("/app", "test-spa-app/")
               .withSpaRootPath("/app")
               .withSpaRootFile("test-spa-app/index.html"));
      javalinServer.start();

      Unirest.config().setDefaultResponseEncoding("UTF-8");

      /////////////////////////////////////////////////////////////////////////////
      // Test 1: API routes should return JSON and work normally                //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> metaDataResponse = Unirest.get("http://localhost:" + PORT + "/metaData").asString();
      assertEquals(200, metaDataResponse.getStatus(), "API route /metaData should return 200");
      assertThat(metaDataResponse.getBody()).contains("tables");  // Should be JSON with tables
      assertThat(metaDataResponse.getBody()).contains("processes");  // Should be JSON with processes
      assertThat(metaDataResponse.getBody()).doesNotContain("<html");  // Should NOT be HTML

      /////////////////////////////////////////////////////////////////////////////
      // Test 2: SPA root path should work and return HTML                      //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> spaRootResponse = Unirest.get("http://localhost:" + PORT + "/app/").asString();
      assertEquals(200, spaRootResponse.getStatus(), "SPA root /app/ should return 200");
      assertThat(spaRootResponse.getBody()).contains("<html");  // Should be HTML
      assertThat(spaRootResponse.getBody()).contains("Test SPA - App");  // Should be our test SPA
      assertThat(spaRootResponse.getBody()).doesNotContain("\"tables\"");  // Should NOT be JSON

      /////////////////////////////////////////////////////////////////////////////
      // Test 3: SPA deep link should serve index.html (client-side routing)    //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> spaDeepLinkResponse = Unirest.get("http://localhost:" + PORT + "/app/users/123").asString();
      assertEquals(200, spaDeepLinkResponse.getStatus(), "SPA deep link /app/users/123 should return 200");
      assertThat(spaDeepLinkResponse.getBody()).contains("<html");  // Should be HTML (index.html)
      assertThat(spaDeepLinkResponse.getBody()).contains("Test SPA - App");  // Should be our test SPA
      assertThat(spaDeepLinkResponse.getBody()).doesNotContain("404");  // Should NOT be 404 page

      /////////////////////////////////////////////////////////////////////////////
      // Test 4: Another SPA deep link with different path structure            //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> anotherDeepLink = Unirest.get("http://localhost:" + PORT + "/app/dashboard/analytics").asString();
      assertEquals(200, anotherDeepLink.getStatus(), "SPA deep link /app/dashboard/analytics should return 200");
      assertThat(anotherDeepLink.getBody()).contains("<html");
      assertThat(anotherDeepLink.getBody()).contains("Test SPA - App");

      /////////////////////////////////////////////////////////////////////////////
      // Test 5: Verify SPA static assets still load correctly                  //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> spaAsset = Unirest.get("http://localhost:" + PORT + "/app/assets/app.js").asString();
      assertEquals(200, spaAsset.getStatus(), "SPA static asset should return 200");
      assertThat(spaAsset.getBody()).contains("console.log");  // Should be JavaScript
      assertThat(spaAsset.getBody()).doesNotContain("<html");  // Should NOT be HTML

      /////////////////////////////////////////////////////////////////////////////
      // Test 6: Verify that paths outside SPA root don't get caught            //
      /////////////////////////////////////////////////////////////////////////////
      HttpResponse<String> outsideSpaResponse = Unirest.get("http://localhost:" + PORT + "/some/random/path").asString();
      assertEquals(404, outsideSpaResponse.getStatus(), "Paths outside SPA root should 404");
      assertThat(outsideSpaResponse.getBody()).doesNotContain("Test SPA - App");  // Should NOT serve SPA index
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class TestApplication extends AbstractQQQApplication
   {
      static int callCount = 0;



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QInstance defineQInstance() throws QException
      {
         callCount++;
         QInstance qInstance = TestUtils.defineInstance();

         qInstance.getTables().values().forEach(t -> t.setLabel(t.getLabel() + callCount));

         return (qInstance);
      }
   }

}