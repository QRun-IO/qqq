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
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.routeproviders.IsolatedSpaRouteProvider;
import com.kingsrook.qqq.middleware.javalin.routeproviders.SimpleFileSystemDirectoryRouter;
import com.kingsrook.qqq.middleware.javalin.routeproviders.SpaNotFoundHandlerRegistry;
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
      if(javalinServer != null)
      {
         javalinServer.stop();
      }
      TestApplication.callCount = 0;
      System.clearProperty("qqq.javalin.enableStaticFilesFromJar");
      Unirest.config().reset();

      //////////////////////////////////////////////////////////////////////////
      // Clear the SPA 404 handler registry to prevent test pollution.        //
      // The registry is a singleton, so handlers from one test can leak into //
      // subsequent tests if not cleared.                                     //
      //////////////////////////////////////////////////////////////////////////
      SpaNotFoundHandlerRegistry.getInstance().clear();
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

      //////////////////////////////////////////
      // Verify the configuration is accepted //
      //////////////////////////////////////////
      assertEquals("/app", javalinServer.getFrontendMaterialDashboardHostedPath());

      ////////////////////////////////////
      // Root should not serve anything //
      ////////////////////////////////////
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

      ////////////////////////////////////////////////
      // Test 1: Root path of first SPA should work //
      ////////////////////////////////////////////////
      HttpResponse<String> appRootResponse = Unirest.get("http://localhost:" + PORT + "/app/").asString();
      assertEquals(200, appRootResponse.getStatus());
      assertThat(appRootResponse.getBody()).contains("Test SPA - App");
      assertThat(appRootResponse.getBody()).contains("<html");

      //////////////////////////////////////////////////////////////////////
      // Test 2: Deep link in first SPA should serve index.html (NOT 404) //
      //////////////////////////////////////////////////////////////////////
      HttpResponse<String> appDeepLink1 = Unirest.get("http://localhost:" + PORT + "/app/users/123").asString();
      assertEquals(200, appDeepLink1.getStatus(), "Deep link /app/users/123 should return 200");
      assertThat(appDeepLink1.getBody()).contains("<html");
      assertThat(appDeepLink1.getBody()).contains("Test SPA - App");
      assertThat(appDeepLink1.getBody()).doesNotContain("404");

      //////////////////////////////////////////////////////
      // Test 3: Another deep link with multiple segments //
      //////////////////////////////////////////////////////
      HttpResponse<String> appDeepLink2 = Unirest.get("http://localhost:" + PORT + "/app/users/123/edit/profile").asString();
      assertEquals(200, appDeepLink2.getStatus(), "Deep link /app/users/123/edit/profile should return 200");
      assertThat(appDeepLink2.getBody()).contains("<html");
      assertThat(appDeepLink2.getBody()).contains("Test SPA - App");

      /////////////////////////////////////////////
      // Test 4: Deep link with query parameters //
      /////////////////////////////////////////////
      HttpResponse<String> appDeepLink3 = Unirest.get("http://localhost:" + PORT + "/app/search?q=test&filter=active").asString();
      assertEquals(200, appDeepLink3.getStatus(), "Deep link with query params should return 200");
      assertThat(appDeepLink3.getBody()).contains("<html");

      /////////////////////////////////////////////////
      // Test 5: Root path of second SPA should work //
      /////////////////////////////////////////////////
      HttpResponse<String> adminRootResponse = Unirest.get("http://localhost:" + PORT + "/admin/").asString();
      assertEquals(200, adminRootResponse.getStatus());
      assertThat(adminRootResponse.getBody()).contains("Test SPA - Admin");
      assertThat(adminRootResponse.getBody()).contains("<html");

      //////////////////////////////////////////////////////
      // Test 6: Deep link in second SPA should also work //
      //////////////////////////////////////////////////////
      HttpResponse<String> adminDeepLink = Unirest.get("http://localhost:" + PORT + "/admin/settings/profile").asString();
      assertEquals(200, adminDeepLink.getStatus(), "Deep link /admin/settings/profile should return 200");
      assertThat(adminDeepLink.getBody()).contains("<html");
      assertThat(adminDeepLink.getBody()).contains("Test SPA - Admin");
      assertThat(adminDeepLink.getBody()).doesNotContain("404");

      //////////////////////////////////////////////////////////////////////////
      // Test 7: Static asset in first SPA should load (NOT serve index.html) //
      //////////////////////////////////////////////////////////////////////////
      HttpResponse<String> appJs = Unirest.get("http://localhost:" + PORT + "/app/assets/app.js").asString();
      assertEquals(200, appJs.getStatus(), "Static asset app.js should return 200");
      assertThat(appJs.getBody()).contains("console.log");
      assertThat(appJs.getBody()).doesNotContain("<html");
      assertThat(appJs.getBody()).doesNotContain("<!DOCTYPE");

      ////////////////////////////////////////////////////
      // Test 8: Static asset in second SPA should load //
      ////////////////////////////////////////////////////
      HttpResponse<String> adminCss = Unirest.get("http://localhost:" + PORT + "/admin/assets/admin.css").asString();
      assertEquals(200, adminCss.getStatus(), "Static asset admin.css should return 200");
      assertThat(adminCss.getBody()).contains("font-family");
      assertThat(adminCss.getBody()).doesNotContain("<html");

      //////////////////////////////////////////////////////////////////////
      // Test 9: Paths with and without trailing slashes should both work //
      //////////////////////////////////////////////////////////////////////
      HttpResponse<String> withSlash    = Unirest.get("http://localhost:" + PORT + "/app/users/").asString();
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

      //////////////////////////////////////////////////////
      // These paths should serve index.html (SPA routes) //
      //////////////////////////////////////////////////////
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

      ////////////////////////////////////////////////////////////////
      // These paths should be treated as assets (even if they 404) //
      ////////////////////////////////////////////////////////////////
      String[] assetPaths = {
         "/app/assets/missing.js",
         "/app/assets/missing.css",
         "/app/static/image.png"
      };

      for(String assetPath : assetPaths)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + assetPath).asString();
         //////////////////////////////////////////////////////////////////////////////
         // Assets may 404 if they don't exist, but they should NOT serve index.html //
         //////////////////////////////////////////////////////////////////////////////
         if(response.getStatus() == 200)
         {
            ////////////////////////////////////////////////
            // If the asset exists, it should not be HTML //
            ////////////////////////////////////////////////
            assertThat(response.getBody()).doesNotContain("<html");
         }
         ///////////////////////////////////////////////////
         // Either way, it should not serve the SPA index //
         ///////////////////////////////////////////////////
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

      ///////////////////////////////
      // Existing file should work //
      ///////////////////////////////
      HttpResponse<String> existingFile = Unirest.get("http://localhost:" + PORT + "/static/foo.html").asString();
      assertEquals(200, existingFile.getStatus());
      assertThat(existingFile.getBody()).contains("Foo? Bar!");

      ///////////////////////////////////////////////////////////////////////////////
      // Non-existing file should 404 (NOT serve index.html because no SPA config) //
      ///////////////////////////////////////////////////////////////////////////////
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

      /////////////////////////////////////////////////////////////
      // Test 1: API routes should return JSON and work normally //
      /////////////////////////////////////////////////////////////
      HttpResponse<String> metaDataResponse = Unirest.get("http://localhost:" + PORT + "/metaData").asString();
      assertEquals(200, metaDataResponse.getStatus(), "API route /metaData should return 200");
      assertThat(metaDataResponse.getBody()).contains("tables");  // Should be JSON with tables
      assertThat(metaDataResponse.getBody()).contains("processes");  // Should be JSON with processes
      assertThat(metaDataResponse.getBody()).doesNotContain("<html");  // Should NOT be HTML

      ///////////////////////////////////////////////////////
      // Test 2: SPA root path should work and return HTML //
      ///////////////////////////////////////////////////////
      HttpResponse<String> spaRootResponse = Unirest.get("http://localhost:" + PORT + "/app/").asString();
      assertEquals(200, spaRootResponse.getStatus(), "SPA root /app/ should return 200");
      assertThat(spaRootResponse.getBody()).contains("<html");  // Should be HTML
      assertThat(spaRootResponse.getBody()).contains("Test SPA - App");  // Should be our test SPA
      assertThat(spaRootResponse.getBody()).doesNotContain("\"tables\"");  // Should NOT be JSON

      /////////////////////////////////////////////////////////////////////////
      // Test 3: SPA deep link should serve index.html (client-side routing) //
      /////////////////////////////////////////////////////////////////////////
      HttpResponse<String> spaDeepLinkResponse = Unirest.get("http://localhost:" + PORT + "/app/users/123").asString();
      assertEquals(200, spaDeepLinkResponse.getStatus(), "SPA deep link /app/users/123 should return 200");
      assertThat(spaDeepLinkResponse.getBody()).contains("<html");  // Should be HTML (index.html)
      assertThat(spaDeepLinkResponse.getBody()).contains("Test SPA - App");  // Should be our test SPA
      assertThat(spaDeepLinkResponse.getBody()).doesNotContain("404");  // Should NOT be 404 page

      /////////////////////////////////////////////////////////////////
      // Test 4: Another SPA deep link with different path structure //
      /////////////////////////////////////////////////////////////////
      HttpResponse<String> anotherDeepLink = Unirest.get("http://localhost:" + PORT + "/app/dashboard/analytics").asString();
      assertEquals(200, anotherDeepLink.getStatus(), "SPA deep link /app/dashboard/analytics should return 200");
      assertThat(anotherDeepLink.getBody()).contains("<html");
      assertThat(anotherDeepLink.getBody()).contains("Test SPA - App");

      ///////////////////////////////////////////////////////////
      // Test 5: Verify SPA static assets still load correctly //
      ///////////////////////////////////////////////////////////
      HttpResponse<String> spaAsset = Unirest.get("http://localhost:" + PORT + "/app/assets/app.js").asString();
      assertEquals(200, spaAsset.getStatus(), "SPA static asset should return 200");
      assertThat(spaAsset.getBody()).contains("console.log");  // Should be JavaScript
      assertThat(spaAsset.getBody()).doesNotContain("<html");  // Should NOT be HTML

      /////////////////////////////////////////////////////////////////
      // Test 6: Verify that paths outside SPA root don't get caught //
      /////////////////////////////////////////////////////////////////
      HttpResponse<String> outsideSpaResponse = Unirest.get("http://localhost:" + PORT + "/some/random/path").asString();
      assertEquals(404, outsideSpaResponse.getStatus(), "Paths outside SPA root should 404");
      assertThat(outsideSpaResponse.getBody()).doesNotContain("Test SPA - App");  // Should NOT serve SPA index
   }



   /*******************************************************************************
    ** Test getters and setters for basic configuration
    *******************************************************************************/
   @Test
   void testGettersAndSetters() throws QException
   {
      AbstractQQQApplication app = getQqqApplication();
      javalinServer = new QApplicationJavalinServer(app);

      /////////////////////////////
      // Test port getter/setter //
      /////////////////////////////
      javalinServer.setPort(9000);
      assertEquals(9000, javalinServer.getPort());

      /////////////////////////////////
      // Test fluent setter for port //
      /////////////////////////////////
      QApplicationJavalinServer returned = javalinServer.withPort(9001);
      assertEquals(9001, javalinServer.getPort());
      assertEquals(javalinServer, returned); // Verify fluent API returns this

      ///////////////////////////////////////////////////////
      // Test serveFrontendMaterialDashboard getter/setter //
      ///////////////////////////////////////////////////////
      javalinServer.setServeFrontendMaterialDashboard(false);
      assertEquals(false, javalinServer.getServeFrontendMaterialDashboard());

      ////////////////////////////////////////////////////////////
      // Test serveLegacyUnversionedMiddlewareAPI getter/setter //
      ////////////////////////////////////////////////////////////
      javalinServer.setServeLegacyUnversionedMiddlewareAPI(false);
      assertEquals(false, javalinServer.getServeLegacyUnversionedMiddlewareAPI());

      //////////////////////////////////////////////
      // Test middlewareVersionList getter/setter //
      //////////////////////////////////////////////
      List<com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion> middlewareVersions = List.of(new MiddlewareVersionV1());
      javalinServer.setMiddlewareVersionList(middlewareVersions);
      assertEquals(middlewareVersions, javalinServer.getMiddlewareVersionList());

      /////////////////////////////////////////////////
      // Test additionalRouteProviders getter/setter //
      /////////////////////////////////////////////////
      List<QJavalinRouteProviderInterface> routeProviders = new java.util.ArrayList<>();
      javalinServer.setAdditionalRouteProviders(routeProviders);
      assertEquals(routeProviders, javalinServer.getAdditionalRouteProviders());

      //////////////////////////////////////////////
      // Test millisBetweenHotSwaps getter/setter //
      //////////////////////////////////////////////
      javalinServer.setMillisBetweenHotSwaps(5000);
      assertEquals(5000, javalinServer.getMillisBetweenHotSwaps());

      //////////////////////////////////////////
      // Test hotSwapCustomizer getter/setter //
      //////////////////////////////////////////
      java.util.function.Consumer<QInstance> customizer = (qi) ->
      {
      };
      javalinServer.setHotSwapCustomizer(customizer);
      assertEquals(customizer, javalinServer.getHotSwapCustomizer());

      ///////////////////////////////////////////////////////
      // Test javalinConfigurationCustomizer getter/setter //
      ///////////////////////////////////////////////////////
      java.util.function.Consumer<io.javalin.Javalin> javalinCustomizer = (j) ->
      {
      };
      javalinServer.setJavalinConfigurationCustomizer(javalinCustomizer);
      assertEquals(javalinCustomizer, javalinServer.getJavalinConfigurationCustomizer());

      ////////////////////////////////////////
      // Test javalinMetaData getter/setter //
      ////////////////////////////////////////
      com.kingsrook.qqq.backend.javalin.QJavalinMetaData metaData = new com.kingsrook.qqq.backend.javalin.QJavalinMetaData();
      javalinServer.setJavalinMetaData(metaData);
      assertEquals(metaData, javalinServer.getJavalinMetaData());
   }



   /*******************************************************************************
    ** Test all fluent setters work correctly and return this
    *******************************************************************************/
   @Test
   void testFluentSetters() throws QException
   {
      AbstractQQQApplication app = getQqqApplication();
      javalinServer = new QApplicationJavalinServer(app);

      /////////////////////////////////////////////////////
      // Chain multiple fluent setters and verify result //
      /////////////////////////////////////////////////////
      QApplicationJavalinServer result = javalinServer
         .withPort(8080)
         .withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(false)
         .withMillisBetweenHotSwaps(3000);

      ///////////////////////////////////////////
      // Verify all returned the same instance //
      ///////////////////////////////////////////
      assertEquals(javalinServer, result);

      ////////////////////////////
      // Verify values were set //
      ////////////////////////////
      assertEquals(8080, javalinServer.getPort());
      assertEquals(false, javalinServer.getServeFrontendMaterialDashboard());
      assertEquals(false, javalinServer.getServeLegacyUnversionedMiddlewareAPI());
      assertEquals(3000, javalinServer.getMillisBetweenHotSwaps());
   }



   /*******************************************************************************
    ** Test withIsolatedSpaRouteProvider fluent setters
    *******************************************************************************/
   @Test
   void testWithIsolatedSpaRouteProvider() throws QException
   {
      AbstractQQQApplication app = getQqqApplication();
      javalinServer = new QApplicationJavalinServer(app);

      //////////////////////////////////////////////////////////////
      // Test simple withIsolatedSpaRouteProvider (2-arg version) //
      //////////////////////////////////////////////////////////////
      javalinServer.withIsolatedSpaRouteProvider("/app", "app-files/");
      assertThat(javalinServer.getAdditionalRouteProviders()).hasSize(1);

      ///////////////////////////////////////////////////////////////
      // Test withIsolatedSpaRouteProvider with index file (3-arg) //
      ///////////////////////////////////////////////////////////////
      javalinServer.withIsolatedSpaRouteProvider("/admin", "admin-files/", "admin-files/index.html");
      assertThat(javalinServer.getAdditionalRouteProviders()).hasSize(2);
   }



   /*******************************************************************************
    ** Test stop() when service is null
    *******************************************************************************/
   @Test
   void testStopWhenServiceIsNull() throws QException
   {
      AbstractQQQApplication app = getQqqApplication();
      javalinServer = new QApplicationJavalinServer(app);

      //////////////////////////////////////////////////////////////
      // stop() should not throw when service hasn't been started //
      //////////////////////////////////////////////////////////////
      javalinServer.stop();  // Should log and noop, not throw
   }



   /*******************************************************************************
    ** Test hot swap customizer is called during hot swap
    *******************************************************************************/
   @Test
   void testHotSwapCustomizerIsCalled() throws QException
   {
      System.setProperty("qqq.javalin.hotSwapInstance", "true");
      final boolean[] customizerCalled = {false};

      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withPort(PORT)
         .withMiddlewareVersionList(List.of(new MiddlewareVersionV1()))
         .withMillisBetweenHotSwaps(0)
         .withServeFrontendMaterialDashboard(false)
         .withHotSwapCustomizer(qInstance ->
         {
            customizerCalled[0] = true;
         });

      javalinServer.start();
      System.clearProperty("qqq.javalin.hotSwapInstance");

      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/qqq/v1/metaData").asString();
      assertEquals(200, response.getStatus());

      ////////////////////////////////////////////////////////
      // Customizer should have been called during hot swap //
      ////////////////////////////////////////////////////////
      assertThat(customizerCalled[0]).isTrue();
   }



   /*******************************************************************************
    ** Test javalin configuration customizer is called during start
    *******************************************************************************/
   @Test
   void testJavalinConfigurationCustomizerIsCalled() throws QException
   {
      final boolean[] customizerCalled = {false};

      javalinServer = new QApplicationJavalinServer(getQqqApplication())
         .withPort(PORT)
         .withServeFrontendMaterialDashboard(false)
         .withJavalinConfigurationCustomizer(javalin ->
         {
            customizerCalled[0] = true;
         });

      javalinServer.start();

      /////////////////////////////////////////////////////
      // Customizer should have been called during start //
      /////////////////////////////////////////////////////
      assertThat(customizerCalled[0]).isTrue();
   }



   /*******************************************************************************
    * Serve material-dashboard as an isolated SPA, at a non-root path.
    *
    * This shows the <base href> tag being inserted in the index.html file when
    * it is necessary (e.g., for deep link requests).
    *******************************************************************************/
   @Test
   void testBaseHrefForMaterialDashboardAsIsolatedSpaAtNonRootPath() throws QException
   {
      /////////////////////////////////////////////////////////////////////////
      // Explicitly enable redirect following. Javalin's static file serving //
      // may redirect /mdb to /mdb/ (trailing slash). This is normal Jetty   //
      // behavior for directory paths, and browsers handle it automatically. //
      /////////////////////////////////////////////////////////////////////////
      Unirest.config().followRedirects(true);

      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(false);

      javalinServer.withAdditionalRouteProvider(
         new IsolatedSpaRouteProvider("/mdb", "material-dashboard")
            .withSpaIndexFile("material-dashboard/index.html")
            .withDeepLinking(true)
            .withLoadFromJar(true));

      javalinServer.start();

      List<String> pathsThatShouldNotIncludeBase = List.of("/mdb", "/mdb/");
      for(String requestPath : pathsThatShouldNotIncludeBase)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + requestPath).asString();
         assertEquals(200, response.getStatus());
         assertThat(response.getBody()).doesNotContainIgnoringCase("""
            <base""");
      }

      List<String> pathsThatShouldIncludeBase = List.of("/mdb/someApp", "/mdb/someApp/someTable");
      for(String requestPath : pathsThatShouldIncludeBase)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + requestPath).asString();
         assertEquals(200, response.getStatus());
         assertThat(response.getBody()).containsIgnoringCase("""
            <base href="/mdb/\"""");
      }
   }



   /*******************************************************************************
    * Serve material-dashboard as an isolated SPA, at the root path.
    *
    * This shows the <base href> tag being inserted in the index.html file when
    * it is necessary (e.g., for deep link requests).
    *
    * BUT - that's broken in the initial commit here!
    *******************************************************************************/
   @Test
   void testBaseHrefForMaterialDashboardAsIsolatedSpaAtRootPath() throws QException
   {
      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(false);

      javalinServer.withAdditionalRouteProvider(
         new IsolatedSpaRouteProvider("/", "material-dashboard")
            .withSpaIndexFile("material-dashboard/index.html")
            .withDeepLinking(true)
            .withLoadFromJar(true));

      javalinServer.start();

      List<String> pathsThatShouldNotIncludeBase = List.of("/");
      for(String requestPath : pathsThatShouldNotIncludeBase)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + requestPath).asString();
         assertEquals(200, response.getStatus());
         assertThat(response.getBody()).doesNotContainIgnoringCase("""
            <base""");
      }

      List<String> pathsThatShouldIncludeBase = List.of("/someApp", "/someApp/someTable");
      for(String requestPath : pathsThatShouldIncludeBase)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + requestPath).asString();
         assertEquals(200, response.getStatus());
         assertThat(response.getBody()).containsIgnoringCase("""
            <base href="/\"""");
      }
   }



   /*******************************************************************************
    * Serve material-dashboard as the built-in mechanism to do so from
    * {@link QApplicationJavalinServer} - not as an isolated SPA.
    *
    * This shows the <base href> tag being inserted in the index.html file when
    * it is necessary (e.g., for deep link requests).
    *
    * BUT - that's broken in the initial commit here!
    *******************************************************************************/
   @Test
   void testBaseHrefForMaterialDashboardServedBuiltIn() throws QException
   {
      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(true);

      javalinServer.start();

      List<String> pathsThatShouldNotIncludeBase = List.of("/");
      for(String requestPath : pathsThatShouldNotIncludeBase)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + requestPath).asString();
         assertEquals(200, response.getStatus());
         assertThat(response.getBody()).doesNotContainIgnoringCase("""
            <base""");
      }

      List<String> pathsThatShouldIncludeBase = List.of("/someApp", "/someApp/someTable");
      for(String requestPath : pathsThatShouldIncludeBase)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + requestPath).asString();
         assertEquals(200, response.getStatus());
         assertThat(response.getBody()).containsIgnoringCase("""
            <base href="/\"""");
      }
   }



   /*******************************************************************************
    * Verify that static assets (js, css, etc.) 404 naturally and don't get
    * index.html served for them. This is important because serving index.html
    * for missing assets would cause confusing JavaScript errors.
    *******************************************************************************/
   @Test
   void testStaticAssetRequestsReturn404NotIndex() throws QException
   {
      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(false);

      javalinServer.withAdditionalRouteProvider(
         new IsolatedSpaRouteProvider("/mdb", "material-dashboard")
            .withSpaIndexFile("material-dashboard/index.html")
            .withDeepLinking(true)
            .withLoadFromJar(true));

      javalinServer.start();

      //////////////////////////////////////////////////////////////////////////
      // Requests for non-existent static assets should 404, NOT serve index //
      //////////////////////////////////////////////////////////////////////////
      List<String> assetPathsThatShould404 = List.of(
         "/mdb/static/js/nonexistent.js",
         "/mdb/static/css/missing.css",
         "/mdb/assets/image.png"
      );

      for(String assetPath : assetPathsThatShould404)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + assetPath).asString();
         assertEquals(404, response.getStatus(), "Asset path should 404: " + assetPath);
      }
   }



   /*******************************************************************************
    * Demonstrate the core problem: QFMD uses "homepage": "." in package.json,
    * which means all asset paths are relative (./static/js/main.js).
    *
    * When a user deep-links to /someApp/someTable, the browser resolves
    * ./static/js/main.js relative to the current path, resulting in:
    *   /someApp/static/js/main.js  (WRONG - 404)
    * Instead of:
    *   /static/js/main.js          (CORRECT)
    *
    * The fix is to inject <base href="/"> so the browser resolves ./ from root.
    *******************************************************************************/
   @Test
   void testRelativePathProblemDemonstration() throws QException
   {
      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(true);

      javalinServer.start();

      ////////////////////////////////////////////////////////////////////
      // When accessing root, the HTML contains ./static/js/main.xxx.js //
      // The browser correctly resolves this to /static/js/main.xxx.js  //
      ////////////////////////////////////////////////////////////////////
      HttpResponse<String> rootResponse = Unirest.get("http://localhost:" + PORT + "/").asString();
      assertEquals(200, rootResponse.getStatus());
      assertThat(rootResponse.getBody()).contains("./static/js/main.");

      /////////////////////////////////////////////////////////////////////////
      // When deep-linking to /someApp/someTable, WITHOUT <base href="/">,   //
      // the browser would resolve ./static/js/main.xxx.js to:               //
      // /someApp/static/js/main.xxx.js  (WRONG PATH - would 404)            //
      //                                                                     //
      // WITH <base href="/">, the browser correctly resolves to:            //
      // /static/js/main.xxx.js  (CORRECT PATH)                              //
      //                                                                     //
      // This test verifies that deep-link responses include <base href="/"> //
      /////////////////////////////////////////////////////////////////////////
      HttpResponse<String> deepLinkResponse = Unirest.get("http://localhost:" + PORT + "/someApp/someTable").asString();
      assertEquals(200, deepLinkResponse.getStatus());

      //////////////////////////////////////////////////////////////////////////////
      // The response should have <base href="/"> to fix relative path resolution //
      //////////////////////////////////////////////////////////////////////////////
      assertThat(deepLinkResponse.getBody())
         .as("Deep-link response should include <base href=\"/\"> to fix relative path resolution")
         .containsIgnoringCase("<base href=\"/\"");
   }



   /*******************************************************************************
    * Test built-in Material Dashboard serving at a non-root path.
    * This verifies base-href injection works with the frontendMaterialDashboardHostedPath setting.
    *******************************************************************************/
   @Test
   void testBaseHrefForMaterialDashboardServedBuiltInAtNonRootPath() throws QException
   {
      /////////////////////////////////////////////////////////////////////////
      // Explicitly enable redirect following. Javalin's static file serving //
      // may redirect /app to /app/ (trailing slash). This is normal Jetty   //
      // behavior for directory paths, and browsers handle it automatically. //
      /////////////////////////////////////////////////////////////////////////
      Unirest.config().followRedirects(true);

      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(true);
      javalinServer.setFrontendMaterialDashboardHostedPath("/app");

      javalinServer.start();

      ////////////////////////////////////////
      // Direct access should NOT have base //
      ////////////////////////////////////////
      List<String> pathsThatShouldNotIncludeBase = List.of("/app", "/app/");
      for(String requestPath : pathsThatShouldNotIncludeBase)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + requestPath).asString();
         assertEquals(200, response.getStatus(), "Should get 200 for: " + requestPath);
         assertThat(response.getBody()).doesNotContainIgnoringCase("<base");
      }

      ////////////////////////////////////////////////////
      // Deep-link access SHOULD have base href="/app/" //
      ////////////////////////////////////////////////////
      List<String> pathsThatShouldIncludeBase = List.of("/app/someTable", "/app/someApp/someTable");
      for(String requestPath : pathsThatShouldIncludeBase)
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + requestPath).asString();
         assertEquals(200, response.getStatus(), "Should get 200 for: " + requestPath);
         assertThat(response.getBody())
            .as("Deep-link to " + requestPath + " should include <base href=\"/app/\">")
            .containsIgnoringCase("<base href=\"/app/\"");
      }
   }



   ///////////////////////////////////////////////////////////////////////
   //                                                                   //
   // COMPREHENSIVE SPA CONFIGURATION TESTS                             //
   //                                                                   //
   // These tests verify all aspects of SPA serving:                    //
   // - Deep linking (returning index.html for unknown routes)          //
   // - Base href injection (fixing relative paths for deep links)      //
   // - Static asset serving (real files return 200)                    //
   // - 404 behavior (missing static assets return 404, not index.html) //
   // - Multi-SPA isolation (SPAs at different paths don't interfere)   //
   //                                                                   //
   ///////////////////////////////////////////////////////////////////////



   /*******************************************************************************
    * SCENARIO 1: Built-in Material Dashboard at root path "/"
    *
    * Verifies:
    * - Root path returns index.html (200)
    * - Deep links return index.html with <base href="/">
    * - Existing static assets return 200
    * - Non-existent static assets return 404 (not index.html)
    *******************************************************************************/
   @Test
   void testComprehensive_BuiltInMaterialDashboardAtRoot() throws QException
   {
      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(true);
      javalinServer.setFrontendMaterialDashboardHostedPath("/");

      javalinServer.start();

      ////////////////////////////////////////////////////////
      // 1. Root path should return index.html without base //
      ////////////////////////////////////////////////////////
      HttpResponse<String> rootResponse = Unirest.get("http://localhost:" + PORT + "/").asString();
      assertEquals(200, rootResponse.getStatus(), "Root path should return 200");
      assertThat(rootResponse.getBody())
         .as("Root should serve index.html")
         .containsIgnoringCase("<!doctype html>")
         .contains("./static/js/main.");
      assertThat(rootResponse.getBody())
         .as("Root path should NOT have <base> tag")
         .doesNotContainIgnoringCase("<base");

      /////////////////////////////////////////////////////////////////
      // 2. Deep links should return index.html WITH <base href="/"> //
      /////////////////////////////////////////////////////////////////
      for(String deepLink : List.of("/someApp", "/someApp/someTable", "/person/123/edit"))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + deepLink).asString();
         assertEquals(200, response.getStatus(), "Deep link " + deepLink + " should return 200");
         assertThat(response.getBody())
            .as("Deep link " + deepLink + " should serve index.html")
            .containsIgnoringCase("<!doctype html>");
         assertThat(response.getBody())
            .as("Deep link " + deepLink + " should have <base href=\"/\">")
            .containsIgnoringCase("<base href=\"/\"");
      }

      //////////////////////////////////////////////////////////////
      // 3. Existing static assets should return 200 with content //
      //////////////////////////////////////////////////////////////
      HttpResponse<String> manifestResponse = Unirest.get("http://localhost:" + PORT + "/manifest.json").asString();
      assertEquals(200, manifestResponse.getStatus(), "manifest.json should return 200");
      assertThat(manifestResponse.getBody())
         .as("manifest.json should contain JSON, not HTML")
         .contains("name")
         .doesNotContainIgnoringCase("<!doctype html>");

      ///////////////////////////////////////////////////////////////
      // 4. Non-existent static assets should 404, NOT serve index //
      ///////////////////////////////////////////////////////////////
      for(String missingAsset : List.of("/static/js/nonexistent.js", "/static/css/missing.css", "/favicon-missing.ico"))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + missingAsset).asString();
         assertEquals(404, response.getStatus(), "Missing asset " + missingAsset + " should return 404");
      }
   }



   /*******************************************************************************
    * SCENARIO 2: Built-in Material Dashboard at non-root path "/dashboard/"
    *
    * Verifies:
    * - /dashboard returns index.html (follows redirect to /dashboard/)
    * - Deep links return index.html with <base href="/dashboard/">
    * - Existing static assets return 200
    * - Non-existent static assets return 404
    * - Root path "/" is NOT handled by this SPA (returns 404)
    *******************************************************************************/
   @Test
   void testComprehensive_BuiltInMaterialDashboardAtDashboardPath() throws QException
   {
      Unirest.config().followRedirects(true);

      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(true);
      javalinServer.setFrontendMaterialDashboardHostedPath("/dashboard");

      javalinServer.start();

      ////////////////////////////////////////////////////////////
      // 1. /dashboard and /dashboard/ should return index.html //
      // without base (direct access, not deep link)            //
      ////////////////////////////////////////////////////////////
      for(String directPath : List.of("/dashboard", "/dashboard/"))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + directPath).asString();
         assertEquals(200, response.getStatus(), directPath + " should return 200");
         assertThat(response.getBody())
            .as(directPath + " should serve index.html")
            .containsIgnoringCase("<!doctype html>");
         assertThat(response.getBody())
            .as(directPath + " (direct access) should NOT have <base> tag")
            .doesNotContainIgnoringCase("<base");
      }

      ///////////////////////////////////////////////////////////////////////////
      // 2. Deep links should return index.html WITH <base href="/dashboard/"> //
      ///////////////////////////////////////////////////////////////////////////
      for(String deepLink : List.of("/dashboard/someApp", "/dashboard/person/123", "/dashboard/table/view"))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + deepLink).asString();
         assertEquals(200, response.getStatus(), "Deep link " + deepLink + " should return 200");
         assertThat(response.getBody())
            .as("Deep link " + deepLink + " should have <base href=\"/dashboard/\">")
            .containsIgnoringCase("<base href=\"/dashboard/\"");
      }

      //////////////////////////////////////////////////////////////
      // 3. Existing static assets should return 200 with content //
      //////////////////////////////////////////////////////////////
      HttpResponse<String> manifestResponse = Unirest.get("http://localhost:" + PORT + "/dashboard/manifest.json").asString();
      assertEquals(200, manifestResponse.getStatus(), "/dashboard/manifest.json should return 200");
      assertThat(manifestResponse.getBody())
         .as("manifest.json should contain JSON, not HTML")
         .contains("name")
         .doesNotContainIgnoringCase("<!doctype html>");

      ///////////////////////////////////////////////////////////////
      // 4. Non-existent static assets should 404, NOT serve index //
      ///////////////////////////////////////////////////////////////
      for(String missingAsset : List.of("/dashboard/static/js/nonexistent.js", "/dashboard/missing.css"))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + missingAsset).asString();
         assertEquals(404, response.getStatus(), "Missing asset " + missingAsset + " should return 404");
      }

      /////////////////////////////////////////////////////////
      // 5. Root path "/" should NOT be handled - expect 404 //
      /////////////////////////////////////////////////////////
      HttpResponse<String> rootResponse = Unirest.get("http://localhost:" + PORT + "/").asString();
      assertEquals(404, rootResponse.getStatus(), "Root path should return 404 when MD is at /dashboard");
   }



   /*******************************************************************************
    * SCENARIO 3: Built-in Material Dashboard at "/dashboard/" AND custom SPA at "/"
    *
    * Verifies:
    * - Both SPAs serve their respective paths correctly
    * - Deep links work for both SPAs with correct base href
    * - They don't interfere with each other
    *******************************************************************************/
   @Test
   void testComprehensive_MaterialDashboardAtDashboardAndCustomSpaAtRoot() throws QException
   {
      Unirest.config().followRedirects(true);

      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);

      ///////////////////////////////////////////////
      // Built-in Material Dashboard at /dashboard //
      ///////////////////////////////////////////////
      javalinServer.setServeFrontendMaterialDashboard(true);
      javalinServer.setFrontendMaterialDashboardHostedPath("/dashboard");

      ////////////////////////////////////////////////////////////////
      // Custom SPA (using material-dashboard files for testing) at //
      ////////////////////////////////////////////////////////////////
      javalinServer.withAdditionalRouteProvider(
         new IsolatedSpaRouteProvider("/", "material-dashboard")
            .withSpaIndexFile("material-dashboard/index.html")
            .withDeepLinking(true)
            .withLoadFromJar(true)
            .withExcludedPaths(List.of("/dashboard"))  // Exclude the dashboard path
      );

      javalinServer.start();

      ////////////////////////////////////////////////////////////
      // 1. Root SPA: "/" should return index.html without base //
      ////////////////////////////////////////////////////////////
      HttpResponse<String> rootResponse = Unirest.get("http://localhost:" + PORT + "/").asString();
      assertEquals(200, rootResponse.getStatus(), "Root path should return 200");
      assertThat(rootResponse.getBody())
         .as("Root should serve index.html")
         .containsIgnoringCase("<!doctype html>");
      assertThat(rootResponse.getBody())
         .as("Root path should NOT have <base> tag")
         .doesNotContainIgnoringCase("<base");

      /////////////////////////////////////////////////////////
      // 2. Root SPA: Deep links should have <base href="/"> //
      /////////////////////////////////////////////////////////
      for(String deepLink : List.of("/customers", "/orders/123"))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + deepLink).asString();
         assertEquals(200, response.getStatus(), "Root SPA deep link " + deepLink + " should return 200");
         assertThat(response.getBody())
            .as("Root SPA deep link should have <base href=\"/\">")
            .containsIgnoringCase("<base href=\"/\"");
      }

      ///////////////////////////////////////////////////////////////////
      // 3. Dashboard SPA: /dashboard should return index without base //
      ///////////////////////////////////////////////////////////////////
      HttpResponse<String> dashboardResponse = Unirest.get("http://localhost:" + PORT + "/dashboard").asString();
      assertEquals(200, dashboardResponse.getStatus(), "/dashboard should return 200");
      assertThat(dashboardResponse.getBody())
         .as("/dashboard should serve index.html")
         .containsIgnoringCase("<!doctype html>");
      assertThat(dashboardResponse.getBody())
         .as("/dashboard (direct access) should NOT have <base> tag")
         .doesNotContainIgnoringCase("<base");

      ////////////////////////////////////////////////////////////////////////
      // 4. Dashboard SPA: Deep links should have <base href="/dashboard/"> //
      ////////////////////////////////////////////////////////////////////////
      for(String deepLink : List.of("/dashboard/admin", "/dashboard/settings/users"))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + deepLink).asString();
         assertEquals(200, response.getStatus(), "Dashboard deep link " + deepLink + " should return 200");
         assertThat(response.getBody())
            .as("Dashboard deep link should have <base href=\"/dashboard/\">")
            .containsIgnoringCase("<base href=\"/dashboard/\"");
      }

      /////////////////////////////////////////
      // 5. Static assets work for both SPAs //
      /////////////////////////////////////////
      HttpResponse<String> rootManifest = Unirest.get("http://localhost:" + PORT + "/manifest.json").asString();
      assertEquals(200, rootManifest.getStatus(), "Root SPA manifest.json should return 200");

      HttpResponse<String> dashManifest = Unirest.get("http://localhost:" + PORT + "/dashboard/manifest.json").asString();
      assertEquals(200, dashManifest.getStatus(), "Dashboard manifest.json should return 200");
   }



   /*******************************************************************************
    * SCENARIO 4: Material Dashboard at "/dashboard", custom SPA at "/custom",
    *             and NOTHING at "/"
    *
    * Verifies:
    * - Both SPAs work at their respective non-root paths
    * - Root path "/" returns 404
    * - Deep links work for both SPAs
    * - SPAs are properly isolated
    *******************************************************************************/
   @Test
   void testComprehensive_TwoNonRootSpasWithNothingAtRoot() throws QException
   {
      Unirest.config().followRedirects(true);

      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);

      ///////////////////////////////////////////////
      // Built-in Material Dashboard at /dashboard //
      ///////////////////////////////////////////////
      javalinServer.setServeFrontendMaterialDashboard(true);
      javalinServer.setFrontendMaterialDashboardHostedPath("/dashboard");

      ////////////////////////////////////////////////////////////////////////
      // Custom SPA at /custom (using material-dashboard files for testing) //
      ////////////////////////////////////////////////////////////////////////
      javalinServer.withAdditionalRouteProvider(
         new IsolatedSpaRouteProvider("/custom", "material-dashboard")
            .withSpaIndexFile("material-dashboard/index.html")
            .withDeepLinking(true)
            .withLoadFromJar(true)
      );

      javalinServer.start();

      //////////////////////////////////////////////////////////
      // 1. Root "/" should return 404 - nothing serves there //
      //////////////////////////////////////////////////////////
      HttpResponse<String> rootResponse = Unirest.get("http://localhost:" + PORT + "/").asString();
      assertEquals(404, rootResponse.getStatus(), "Root path should return 404 when no SPA serves it");

      ////////////////////////////////////////////////////
      // 2. Dashboard SPA works correctly at /dashboard //
      ////////////////////////////////////////////////////
      HttpResponse<String> dashboardResponse = Unirest.get("http://localhost:" + PORT + "/dashboard").asString();
      assertEquals(200, dashboardResponse.getStatus(), "/dashboard should return 200");
      assertThat(dashboardResponse.getBody())
         .as("/dashboard should serve index.html")
         .containsIgnoringCase("<!doctype html>");

      /////////////////////////////////////////////////
      // Dashboard deep links have correct base href //
      /////////////////////////////////////////////////
      HttpResponse<String> dashDeepLink = Unirest.get("http://localhost:" + PORT + "/dashboard/admin/users").asString();
      assertEquals(200, dashDeepLink.getStatus());
      assertThat(dashDeepLink.getBody())
         .as("Dashboard deep link should have <base href=\"/dashboard/\">")
         .containsIgnoringCase("<base href=\"/dashboard/\"");

      //////////////////////////////////////////////
      // 3. Custom SPA works correctly at /custom //
      //////////////////////////////////////////////
      HttpResponse<String> customResponse = Unirest.get("http://localhost:" + PORT + "/custom").asString();
      assertEquals(200, customResponse.getStatus(), "/custom should return 200");
      assertThat(customResponse.getBody())
         .as("/custom should serve index.html")
         .containsIgnoringCase("<!doctype html>");

      //////////////////////////////////////////////////
      // Custom SPA deep links have correct base href //
      //////////////////////////////////////////////////
      HttpResponse<String> customDeepLink = Unirest.get("http://localhost:" + PORT + "/custom/reports/sales").asString();
      assertEquals(200, customDeepLink.getStatus());
      assertThat(customDeepLink.getBody())
         .as("Custom SPA deep link should have <base href=\"/custom/\">")
         .containsIgnoringCase("<base href=\"/custom/\"");

      /////////////////////////////////////////
      // 4. Static assets work for both SPAs //
      /////////////////////////////////////////
      HttpResponse<String> dashManifest = Unirest.get("http://localhost:" + PORT + "/dashboard/manifest.json").asString();
      assertEquals(200, dashManifest.getStatus(), "/dashboard/manifest.json should return 200");

      HttpResponse<String> customManifest = Unirest.get("http://localhost:" + PORT + "/custom/manifest.json").asString();
      assertEquals(200, customManifest.getStatus(), "/custom/manifest.json should return 200");

      /////////////////////////////////////////
      // 5. Missing assets 404 for both SPAs //
      /////////////////////////////////////////
      HttpResponse<String> dashMissing = Unirest.get("http://localhost:" + PORT + "/dashboard/static/missing.js").asString();
      assertEquals(404, dashMissing.getStatus(), "Missing dashboard asset should 404");

      HttpResponse<String> customMissing = Unirest.get("http://localhost:" + PORT + "/custom/static/missing.js").asString();
      assertEquals(404, customMissing.getStatus(), "Missing custom asset should 404");

      /////////////////////////////////////////////////////////
      // 6. Random paths that don't match any SPA should 404 //
      /////////////////////////////////////////////////////////
      HttpResponse<String> randomPath = Unirest.get("http://localhost:" + PORT + "/random/path").asString();
      assertEquals(404, randomPath.getStatus(), "Random path not under any SPA should 404");
   }



   /*******************************************************************************
    * SCENARIO 5: Cross-SPA Boundary Isolation
    *
    * Verifies that SPA path matching uses proper boundary checking:
    * - /dashboard should NOT match /dashboardx or /dashboard-admin
    * - /app should NOT match /application or /app-store
    * - This prevents false matches on similar path prefixes
    *******************************************************************************/
   @Test
   void testComprehensive_CrossSpaBoundaryIsolation() throws QException
   {
      Unirest.config().followRedirects(true);

      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(false);

      /////////////////
      // SPA at /app //
      /////////////////
      javalinServer.withAdditionalRouteProvider(
         new IsolatedSpaRouteProvider("/app", "material-dashboard")
            .withSpaIndexFile("material-dashboard/index.html")
            .withDeepLinking(true)
            .withLoadFromJar(true)
      );

      ///////////////////
      // SPA at /admin //
      ///////////////////
      javalinServer.withAdditionalRouteProvider(
         new IsolatedSpaRouteProvider("/admin", "material-dashboard")
            .withSpaIndexFile("material-dashboard/index.html")
            .withDeepLinking(true)
            .withLoadFromJar(true)
      );

      javalinServer.start();

      /////////////////////////
      // 1. /app should work //
      /////////////////////////
      HttpResponse<String> appResponse = Unirest.get("http://localhost:" + PORT + "/app").asString();
      assertEquals(200, appResponse.getStatus(), "/app should return 200");

      //////////////////////////////////////////////////////////
      // 2. /app/deep/link should work with correct base href //
      //////////////////////////////////////////////////////////
      HttpResponse<String> appDeepLink = Unirest.get("http://localhost:" + PORT + "/app/users/123").asString();
      assertEquals(200, appDeepLink.getStatus(), "/app/users/123 should return 200");
      assertThat(appDeepLink.getBody()).containsIgnoringCase("<base href=\"/app/\"");

      ////////////////////////////////////////////////////////
      // 3. /application should NOT match /app - expect 404 //
      ////////////////////////////////////////////////////////
      HttpResponse<String> applicationResponse = Unirest.get("http://localhost:" + PORT + "/application").asString();
      assertEquals(404, applicationResponse.getStatus(), "/application should NOT match /app SPA");

      //////////////////////////////////////////////////////
      // 4. /app-store should NOT match /app - expect 404 //
      //////////////////////////////////////////////////////
      HttpResponse<String> appStoreResponse = Unirest.get("http://localhost:" + PORT + "/app-store").asString();
      assertEquals(404, appStoreResponse.getStatus(), "/app-store should NOT match /app SPA");

      /////////////////////////////////////////////////
      // 5. /apps should NOT match /app - expect 404 //
      /////////////////////////////////////////////////
      HttpResponse<String> appsResponse = Unirest.get("http://localhost:" + PORT + "/apps").asString();
      assertEquals(404, appsResponse.getStatus(), "/apps should NOT match /app SPA");

      ///////////////////////////////
      // 6. /admin works correctly //
      ///////////////////////////////
      HttpResponse<String> adminResponse = Unirest.get("http://localhost:" + PORT + "/admin").asString();
      assertEquals(200, adminResponse.getStatus(), "/admin should return 200");

      ////////////////////////////////////////////////////////////
      // 7. /administrator should NOT match /admin - expect 404 //
      ////////////////////////////////////////////////////////////
      HttpResponse<String> administratorResponse = Unirest.get("http://localhost:" + PORT + "/administrator").asString();
      assertEquals(404, administratorResponse.getStatus(), "/administrator should NOT match /admin SPA");

      //////////////////////////////////////////////////////////
      // 8. /admin-panel should NOT match /admin - expect 404 //
      //////////////////////////////////////////////////////////
      HttpResponse<String> adminPanelResponse = Unirest.get("http://localhost:" + PORT + "/admin-panel").asString();
      assertEquals(404, adminPanelResponse.getStatus(), "/admin-panel should NOT match /admin SPA");
   }



   /*******************************************************************************
    * SCENARIO 6: Comprehensive Static Asset 404 Behavior
    *
    * Verifies that ALL common static asset file types return 404 when missing,
    * rather than incorrectly serving index.html (which would cause JS errors).
    *
    * This is critical because serving index.html for missing .js files would
    * cause "Unexpected token '<'" JavaScript errors.
    *******************************************************************************/
   @Test
   void testComprehensive_StaticAsset404ForAllFileTypes() throws QException
   {
      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(true);

      javalinServer.start();

      ////////////////////////////////////////////
      // JavaScript files should 404 if missing //
      ////////////////////////////////////////////
      for(String jsFile : List.of(
         "/static/js/missing.js",
         "/static/js/chunk.abc123.js",
         "/assets/vendor.mjs",
         "/scripts/app.cjs"
      ))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + jsFile).asString();
         assertEquals(404, response.getStatus(), "Missing JS file should 404: " + jsFile);
      }

      /////////////////////////////////////
      // CSS files should 404 if missing //
      /////////////////////////////////////
      for(String cssFile : List.of(
         "/static/css/missing.css",
         "/styles/theme.css",
         "/assets/main.abc123.css"
      ))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + cssFile).asString();
         assertEquals(404, response.getStatus(), "Missing CSS file should 404: " + cssFile);
      }

      ///////////////////////////////////////
      // Image files should 404 if missing //
      ///////////////////////////////////////
      for(String imageFile : List.of(
         "/images/logo.png",
         "/images/hero.jpg",
         "/images/icon.jpeg",
         "/images/banner.gif",
         "/images/graphic.svg",
         "/images/photo.webp",
         "/favicon.ico"
      ))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + imageFile).asString();
         assertEquals(404, response.getStatus(), "Missing image file should 404: " + imageFile);
      }

      //////////////////////////////////////
      // Font files should 404 if missing //
      //////////////////////////////////////
      for(String fontFile : List.of(
         "/fonts/roboto.woff",
         "/fonts/roboto.woff2",
         "/fonts/arial.ttf",
         "/fonts/custom.eot",
         "/fonts/icons.otf"
      ))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + fontFile).asString();
         assertEquals(404, response.getStatus(), "Missing font file should 404: " + fontFile);
      }

      ////////////////////////////////////////////
      // Source map files should 404 if missing //
      ////////////////////////////////////////////
      for(String mapFile : List.of(
         "/static/js/main.js.map",
         "/static/css/main.css.map"
      ))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + mapFile).asString();
         assertEquals(404, response.getStatus(), "Missing source map should 404: " + mapFile);
      }

      ///////////////////////////////////////////////////////////////
      // Data/config files should 404 if missing                   //
      // Note: robots.txt exists in material-dashboard, so skip it //
      ///////////////////////////////////////////////////////////////
      for(String dataFile : List.of(
         "/sitemap.xml",
         "/config.xml",
         "/missing-file.txt"
      ))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + dataFile).asString();
         assertEquals(404, response.getStatus(), "Missing data file should 404: " + dataFile);
      }
   }



   /*******************************************************************************
    * SCENARIO 7: Deep Paths and Query Strings
    *
    * Verifies that deep linking works correctly with:
    * - Very deeply nested paths
    * - Paths with query strings
    * - Paths with special characters (URL encoded)
    *******************************************************************************/
   @Test
   void testComprehensive_DeepPathsAndQueryStrings() throws QException
   {
      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(true);

      javalinServer.start();

      //////////////////////////////////////////////////////////////
      // 1. Very deep paths should still get index.html with base //
      //////////////////////////////////////////////////////////////
      for(String deepPath : List.of(
         "/a/b/c/d/e/f",
         "/module/submodule/feature/action/id/edit",
         "/org/team/project/sprint/task/subtask/comment"
      ))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + deepPath).asString();
         assertEquals(200, response.getStatus(), "Deep path should return 200: " + deepPath);
         assertThat(response.getBody())
            .as("Deep path should have <base href=\"/\">: " + deepPath)
            .containsIgnoringCase("<base href=\"/\"");
      }

      /////////////////////////////////////////////
      // 2. Paths with query strings should work //
      /////////////////////////////////////////////
      for(String pathWithQuery : List.of(
         "/app?tab=settings",
         "/users?page=1&limit=10",
         "/search?q=hello+world&filter=active"
      ))
      {
         HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + pathWithQuery).asString();
         assertEquals(200, response.getStatus(), "Path with query should return 200: " + pathWithQuery);
         assertThat(response.getBody())
            .as("Path with query should have <base href=\"/\">")
            .containsIgnoringCase("<base href=\"/\"");
      }

      /////////////////////////////////////////////////////////
      // 3. Root path with query string should NOT have base //
      /////////////////////////////////////////////////////////
      HttpResponse<String> rootWithQuery = Unirest.get("http://localhost:" + PORT + "/?welcome=true").asString();
      assertEquals(200, rootWithQuery.getStatus());
      assertThat(rootWithQuery.getBody())
         .as("Root with query should NOT have <base> tag")
         .doesNotContainIgnoringCase("<base");
   }



   /*******************************************************************************
    * SCENARIO 8: Darin's Specific Concern - Relative Path Resolution
    *
    * This test specifically verifies the fix for the issue Darin raised:
    * QFMD uses "homepage": "." in package.json, causing all asset paths to be
    * relative (./static/js/main.js).
    *
    * When deep-linking, the browser resolves ./ relative to the current path:
    *   At /someApp/someTable: ./static/js/main.js -> /someApp/static/js/main.js (WRONG!)
    *
    * The fix injects <base href="/"> so the browser resolves from root:
    *   With <base href="/">: ./static/js/main.js -> /static/js/main.js (CORRECT!)
    *
    * This test simulates what the browser would request after parsing the HTML.
    *******************************************************************************/
   @Test
   void testComprehensive_DarinsRelativePathScenario() throws QException
   {
      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(true);

      javalinServer.start();

      ////////////////////////////////////////
      // 1. Get index.html from a deep link //
      ////////////////////////////////////////
      HttpResponse<String> deepLinkResponse = Unirest.get("http://localhost:" + PORT + "/someApp/someTable").asString();
      assertEquals(200, deepLinkResponse.getStatus());

      ///////////////////////////////////////////////////////////
      // Verify it has the base href tag - THIS IS THE KEY FIX //
      ///////////////////////////////////////////////////////////
      assertThat(deepLinkResponse.getBody())
         .as("Deep link response must include <base href=\"/\">")
         .containsIgnoringCase("<base href=\"/\"");

      ////////////////////////////////////////////////////////////////////////////////
      // Verify it still has the relative path references (./static/js/main.xxx.js) //
      ////////////////////////////////////////////////////////////////////////////////
      assertThat(deepLinkResponse.getBody())
         .as("HTML should contain relative ./static/js path")
         .contains("./static/js/main.");

      /////////////////////////////////////////////////////////////
      // 2. The CORRECT path: /static/js/main.xxx.js should work //
      // With <base href="/">, the browser resolves ./ from root //
      /////////////////////////////////////////////////////////////
      HttpResponse<String> correctJsPath = Unirest.get("http://localhost:" + PORT + "/static/js/main.723fb195.js").asString();
      assertEquals(200, correctJsPath.getStatus(), "CORRECT: /static/js/main.xxx.js should return 200");

      //////////////////////////////////////////////////////////////
      // 3. The WRONG path: /someApp/static/js/... should 404     //
      // This is what the browser would request WITHOUT base href //
      //////////////////////////////////////////////////////////////
      HttpResponse<String> wrongJsPath = Unirest.get("http://localhost:" + PORT + "/someApp/static/js/main.723fb195.js").asString();
      assertEquals(404, wrongJsPath.getStatus(), "WRONG: /someApp/static/js/main.xxx.js should 404");

      ////////////////////////////////
      // 4. Same test for CSS files //
      ////////////////////////////////
      HttpResponse<String> correctCssPath = Unirest.get("http://localhost:" + PORT + "/static/css/main.bb7af874.css").asString();
      assertEquals(200, correctCssPath.getStatus(), "CORRECT: /static/css/main.xxx.css should return 200");

      HttpResponse<String> wrongCssPath = Unirest.get("http://localhost:" + PORT + "/someApp/static/css/main.bb7af874.css").asString();
      assertEquals(404, wrongCssPath.getStatus(), "WRONG: /someApp/static/css/main.xxx.css should 404");
   }



   /*******************************************************************************
    * SCENARIO 9: Static Files vs SPA Deep Links
    *
    * Verifies that static files (JSON, etc.) are served correctly alongside
    * the SPA, and that deep links to non-existent paths return the SPA HTML
    * with the correct base href.
    *******************************************************************************/
   @Test
   void testComprehensive_StaticFilesVsDeepLinks() throws QException
   {
      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(true);

      javalinServer.start();

      ///////////////////////////////////////////////////////////
      // 1. Static JSON files should return JSON, not SPA HTML //
      ///////////////////////////////////////////////////////////
      HttpResponse<String> manifestFile = Unirest.get("http://localhost:" + PORT + "/manifest.json").asString();
      assertEquals(200, manifestFile.getStatus());
      assertThat(manifestFile.getBody())
         .as("manifest.json should be JSON, not HTML")
         .contains("name")
         .doesNotContain("<!doctype");

      /////////////////////////////////////////////////
      // 2. Static JS files should return JavaScript //
      /////////////////////////////////////////////////
      HttpResponse<String> jsFile = Unirest.get("http://localhost:" + PORT + "/static/js/main.723fb195.js").asString();
      assertEquals(200, jsFile.getStatus());
      // Check Content-Type header if present
      String contentType = jsFile.getHeaders().getFirst("Content-Type");
      if(contentType != null)
      {
         assertThat(contentType)
            .as("JS file should have JavaScript content type")
            .containsIgnoringCase("javascript");
      }

      ///////////////////////////////////////////////////
      // 3. Deep SPA routes return HTML with base href //
      ///////////////////////////////////////////////////
      HttpResponse<String> spaDeepLink = Unirest.get("http://localhost:" + PORT + "/myApp/myTable").asString();
      assertEquals(200, spaDeepLink.getStatus());
      assertThat(spaDeepLink.getBody())
         .as("Deep links should return SPA HTML with base href")
         .containsIgnoringCase("<base href=\"/\"")
         .containsIgnoringCase("<!doctype html>");
   }



   /*******************************************************************************
    * SCENARIO 10: Content-Type Verification
    *
    * Verifies that static files are served with correct MIME types.
    * This is important for browser security and correct file handling.
    *******************************************************************************/
   @Test
   void testComprehensive_StaticFileContentTypes() throws QException
   {
      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(true);

      javalinServer.start();

      ////////////////////////////////////////////////
      // 1. HTML should have text/html content type //
      ////////////////////////////////////////////////
      HttpResponse<String> htmlResponse = Unirest.get("http://localhost:" + PORT + "/").asString();
      assertThat(htmlResponse.getHeaders().getFirst("Content-Type"))
         .as("HTML should have text/html content type")
         .containsIgnoringCase("text/html");

      ////////////////////////////////////////////////////////
      // 2. JavaScript should have appropriate content type //
      ////////////////////////////////////////////////////////
      HttpResponse<String> jsResponse = Unirest.get("http://localhost:" + PORT + "/static/js/main.723fb195.js").asString();
      assertThat(jsResponse.getHeaders().getFirst("Content-Type"))
         .as("JS should have javascript content type")
         .containsIgnoringCase("javascript");

      //////////////////////////////////////////////
      // 3. CSS should have text/css content type //
      //////////////////////////////////////////////
      HttpResponse<String> cssResponse = Unirest.get("http://localhost:" + PORT + "/static/css/main.bb7af874.css").asString();
      assertThat(cssResponse.getHeaders().getFirst("Content-Type"))
         .as("CSS should have text/css content type")
         .containsIgnoringCase("text/css");

      ///////////////////////////////////////////////////////
      // 4. JSON should have application/json content type //
      ///////////////////////////////////////////////////////
      HttpResponse<String> jsonResponse = Unirest.get("http://localhost:" + PORT + "/manifest.json").asString();
      assertThat(jsonResponse.getHeaders().getFirst("Content-Type"))
         .as("JSON should have application/json content type")
         .containsIgnoringCase("application/json");
   }



   /*******************************************************************************
    * SCENARIO 11: SPA Path Exclusions (Root and Non-Root)
    *
    * Verifies that path exclusions work for BOTH root and non-root SPAs.
    * Excluded paths should 404 instead of returning the SPA index.
    *******************************************************************************/
   @Test
   void testComprehensive_SpaPathExclusions() throws QException
   {
      Unirest.config().followRedirects(true);

      javalinServer = new QApplicationJavalinServer(createMinimalApplication());
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(false);

      //////////////////////////////
      // Root SPA with exclusions //
      //////////////////////////////
      javalinServer.withAdditionalRouteProvider(
         new IsolatedSpaRouteProvider("/", "material-dashboard")
            .withSpaIndexFile("material-dashboard/index.html")
            .withDeepLinking(true)
            .withLoadFromJar(true)
            .withExcludedPaths(List.of("/api", "/portal"))  // Exclude /portal for the other SPA
      );

      /////////////////////////////////////////////////////
      // Non-root SPA at /portal with its own exclusions //
      /////////////////////////////////////////////////////
      javalinServer.withAdditionalRouteProvider(
         new IsolatedSpaRouteProvider("/portal", "material-dashboard")
            .withSpaIndexFile("material-dashboard/index.html")
            .withDeepLinking(true)
            .withLoadFromJar(true)
            .withExcludedPaths(List.of("/portal/api", "/portal/webhook"))
      );

      javalinServer.start();

      ///////////////////////////////////////////////
      // 1. Root SPA works and respects exclusions //
      ///////////////////////////////////////////////
      HttpResponse<String> rootResponse = Unirest.get("http://localhost:" + PORT + "/").asString();
      assertEquals(200, rootResponse.getStatus());
      assertThat(rootResponse.getBody()).containsIgnoringCase("<!doctype html>");

      HttpResponse<String> rootDeepLink = Unirest.get("http://localhost:" + PORT + "/users/list").asString();
      assertEquals(200, rootDeepLink.getStatus());
      assertThat(rootDeepLink.getBody()).containsIgnoringCase("<base href=\"/\"");

      /////////////////////////////////////
      // Root SPA's /api exclusion works //
      /////////////////////////////////////
      HttpResponse<String> excludedApi = Unirest.get("http://localhost:" + PORT + "/api/users").asString();
      assertEquals(404, excludedApi.getStatus(), "Root SPA excluded path /api/users should 404");

      //////////////////////////////////////
      // 2. Non-root SPA at /portal works //
      //////////////////////////////////////
      HttpResponse<String> portalResponse = Unirest.get("http://localhost:" + PORT + "/portal").asString();
      assertEquals(200, portalResponse.getStatus());
      assertThat(portalResponse.getBody()).containsIgnoringCase("<!doctype html>");

      HttpResponse<String> portalDeepLink = Unirest.get("http://localhost:" + PORT + "/portal/users/list").asString();
      assertEquals(200, portalDeepLink.getStatus());
      assertThat(portalDeepLink.getBody()).containsIgnoringCase("<base href=\"/portal/\"");

      ///////////////////////////////////////
      // 3. Non-root SPA's exclusions work //
      ///////////////////////////////////////
      HttpResponse<String> portalApiExcluded = Unirest.get("http://localhost:" + PORT + "/portal/api/users").asString();
      assertEquals(404, portalApiExcluded.getStatus(), "Portal SPA excluded path /portal/api/users should 404");

      HttpResponse<String> portalWebhookExcluded = Unirest.get("http://localhost:" + PORT + "/portal/webhook/github").asString();
      assertEquals(404, portalWebhookExcluded.getStatus(), "Portal SPA excluded path /portal/webhook/github should 404");

      ////////////////////////////////////////////////////
      // 4. Non-excluded paths under /portal still work //
      ////////////////////////////////////////////////////
      HttpResponse<String> portalNormalDeepLink = Unirest.get("http://localhost:" + PORT + "/portal/dashboard/settings").asString();
      assertEquals(200, portalNormalDeepLink.getStatus());
      assertThat(portalNormalDeepLink.getBody()).containsIgnoringCase("<base href=\"/portal/\"");
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static AbstractQQQApplication createMinimalApplication()
   {
      return new AbstractQQQApplication()
      {
         /***************************************************************************
          *
          ***************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            QInstance qInstance1 = new QInstance();
            qInstance1.addBackend(new QBackendMetaData().withBackendType(MemoryBackendModule.class).withName("memory"));
            qInstance1.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withName("anon").withType(QAuthenticationType.FULLY_ANONYMOUS));
            qInstance1.addTable(new QTableMetaData()
               .withName("table")
               .withBackendName("memory")
               .withField(new QFieldMetaData("id", QFieldType.INTEGER))
               .withPrimaryKeyField("id"));
            return qInstance1;
         }
      };
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
