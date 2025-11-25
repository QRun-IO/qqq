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

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.javalin.TestUtils;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;


/*******************************************************************************
 ** Unit test for IsolatedSpaRouteProvider
 *******************************************************************************/
class IsolatedSpaRouteProviderTest
{
   private QInstance                qInstance;
   private IsolatedSpaRouteProvider provider;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      qInstance = TestUtils.defineInstance();
      /////////////////////////////////////////
      // QContext not needed for these tests //
      /////////////////////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      SpaNotFoundHandlerRegistry.getInstance().clear();
   }



   /*******************************************************************************
    ** Test constructor and basic setup
    *******************************************************************************/
   @Test
   void testConstructor()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");

      assertEquals("/admin", provider.getSpaPath());
      assertEquals("admin-spa/", provider.getStaticFilesPath());
   }



   /*******************************************************************************
    ** Test path normalization
    *******************************************************************************/
   @Test
   void testPathNormalization()
   {
      IsolatedSpaRouteProvider provider1 = new IsolatedSpaRouteProvider("admin", "admin-spa/");
      assertEquals("/admin", provider1.getSpaPath());

      IsolatedSpaRouteProvider provider2 = new IsolatedSpaRouteProvider("/admin/", "admin-spa/");
      assertEquals("/admin", provider2.getSpaPath());

      IsolatedSpaRouteProvider provider3 = new IsolatedSpaRouteProvider(null, "root-spa/");
      assertEquals("/", provider3.getSpaPath());
   }



   /*******************************************************************************
    ** Test fluent setters
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      IsolatedSpaRouteProvider provider      = new IsolatedSpaRouteProvider("/admin", "admin-spa/");
      QCodeReference           authenticator = new QCodeReference(String.class);

      IsolatedSpaRouteProvider result = provider
         .withSpaIndexFile("admin-spa/index.html")
         .withAuthenticator(authenticator)
         .withExcludedPath("/api")
         .withExcludedPaths(List.of("/other"))
         .withBeforeHandler(mock(Handler.class))
         .withAfterHandler(mock(Handler.class))
         .withDeepLinking(false)
         .withLoadFromJar(true);

      assertNotNull(result);
      assertEquals("admin-spa/index.html", provider.getSpaIndexFile());
   }



   /*******************************************************************************
    ** Test acceptJavalinConfig
    *******************************************************************************/
   @Test
   void testAcceptJavalinConfig()
   {
      ////////////////////////////////////////
      // Use an existing test resource path //
      ////////////////////////////////////////
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "test-spa-admin/");
      provider.setQInstance(qInstance);

      Javalin service = Javalin.create(config ->
      {
         provider.acceptJavalinConfig(config);
      });
      try
      {
         //////////////////////////////////////////////////
         // Config should have been called during create //
         //////////////////////////////////////////////////
         assertNotNull(service);
      }
      finally
      {
         service.stop();
      }
   }



   /*******************************************************************************
    ** Test acceptJavalinService for path-scoped SPA
    *******************************************************************************/
   @Test
   void testAcceptJavalinService_PathScoped()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");
      provider.setQInstance(qInstance);
      provider.withSpaIndexFile("admin-spa/index.html");

      Javalin service = Javalin.create();
      try
      {
         provider.acceptJavalinService(service);
         ////////////////////////////////////////////
         // Should register handlers without error //
         ////////////////////////////////////////////
      }
      finally
      {
         service.stop();
      }
   }



   /*******************************************************************************
    ** Test acceptJavalinService for root SPA
    *******************************************************************************/
   @Test
   void testAcceptJavalinService_RootSpa()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/", "root-spa/");
      provider.setQInstance(qInstance);
      provider.withSpaIndexFile("root-spa/index.html");
      provider.withExcludedPaths(List.of("/api", "/admin"));

      Javalin service = Javalin.create();
      try
      {
         provider.acceptJavalinService(service);
         ////////////////////////////////////////////
         // Should register handlers without error //
         ////////////////////////////////////////////
      }
      finally
      {
         service.stop();
      }
   }



   /*******************************************************************************
    ** Test getters
    *******************************************************************************/
   @Test
   void testGetters()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");
      provider.withSpaIndexFile("admin-spa/index.html");

      assertEquals("/admin", provider.getSpaPath());
      assertEquals("admin-spa/", provider.getStaticFilesPath());
      assertEquals("admin-spa/index.html", provider.getSpaIndexFile());
   }



   /*******************************************************************************
    ** Test withLoadFromJar
    *******************************************************************************/
   @Test
   void testWithLoadFromJar()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");
      provider.withLoadFromJar(true);

      ///////////////////////
      // Verify it was set //
      ///////////////////////
      assertNotNull(provider);
   }



   /*******************************************************************************
    ** Test withDeepLinking
    *******************************************************************************/
   @Test
   void testWithDeepLinking()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");
      provider.withDeepLinking(false);

      ///////////////////////
      // Verify it was set //
      ///////////////////////
      assertNotNull(provider);
   }



   /*******************************************************************************
    ** Test acceptJavalinConfig with loadFromJar enabled
    *******************************************************************************/
   @Test
   void testAcceptJavalinConfig_WithJarLoading()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "test-spa-admin/");
      provider.setQInstance(qInstance);
      provider.withLoadFromJar(true);

      Javalin service = Javalin.create(config ->
      {
         provider.acceptJavalinConfig(config);
      });
      try
      {
         assertNotNull(service);
      }
      finally
      {
         service.stop();
      }
   }



   /*******************************************************************************
    ** Test acceptJavalinService without deep linking
    *******************************************************************************/
   @Test
   void testAcceptJavalinService_WithoutDeepLinking()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "test-spa-admin/");
      provider.setQInstance(qInstance);
      provider.withSpaIndexFile("index.html");
      provider.withDeepLinking(false);

      Javalin service = Javalin.create();
      try
      {
         provider.acceptJavalinService(service);
         assertNotNull(service);
      }
      finally
      {
         service.stop();
      }
   }



   /*******************************************************************************
    ** Test acceptJavalinService with authenticator
    *******************************************************************************/
   @Test
   void testAcceptJavalinService_WithAuthenticator()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "test-spa-admin/");
      provider.setQInstance(qInstance);
      provider.withAuthenticator(new QCodeReference(String.class));

      Javalin service = Javalin.create();
      try
      {
         provider.acceptJavalinService(service);
         assertNotNull(service);
      }
      finally
      {
         service.stop();
      }
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - inject base tag when none exists
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_InjectBaseTag()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");

      String originalHtml = """
         <!DOCTYPE html>
         <html lang="en">
         <head>
           <meta charset="UTF-8">
           <title>Test SPA</title>
           <link rel="stylesheet" href="./assets/main.css">
         </head>
         <body>
           <div id="root"></div>
           <script src="./assets/main.js"></script>
         </body>
         </html>
         """;

      String result = provider.rewriteIndexHtmlPaths(originalHtml, "/admin");

      ///////////////////////////////////////////////
      // Should inject base tag right after <head> //
      ///////////////////////////////////////////////
      assertTrue(result.contains("<base href=\"/admin/\">"), "Should inject <base> tag");
      assertTrue(result.indexOf("<base href=\"/admin/\">") > result.indexOf("<head>"), "Base tag should be after <head>");
      assertTrue(result.indexOf("<base href=\"/admin/\">") < result.indexOf("</head>"), "Base tag should be before </head>");
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - inject base tag with trailing slash handling
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_TrailingSlashHandling()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");

      String originalHtml = "<html><head><title>Test</title></head><body></body></html>";

      ///////////////////////////////////////////
      // Test with path without trailing slash //
      ///////////////////////////////////////////
      String result1 = provider.rewriteIndexHtmlPaths(originalHtml, "/admin");
      assertTrue(result1.contains("<base href=\"/admin/\">"), "Should add trailing slash to base href");

      ////////////////////////////////////////
      // Test with path with trailing slash //
      ////////////////////////////////////////
      String result2 = provider.rewriteIndexHtmlPaths(originalHtml, "/admin/");
      assertTrue(result2.contains("<base href=\"/admin/\">"), "Should preserve single trailing slash");
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - update existing base tag
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_UpdateExistingBaseTag()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");

      String originalHtml = """
         <!DOCTYPE html>
         <html>
         <head>
           <base href="/">
           <title>Test SPA</title>
         </head>
         <body></body>
         </html>
         """;

      String result = provider.rewriteIndexHtmlPaths(originalHtml, "/admin");

      /////////////////////////////////////////
      // Should update the existing base tag //
      /////////////////////////////////////////
      assertTrue(result.contains("<base href=\"/admin/\">"), "Should update existing <base> tag to new path");
      assertFalse(result.contains("<base href=\"/\">"), "Should not contain old base href");
      assertEquals(1, countOccurrences(result, "<base"), "Should have exactly one base tag");
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - update existing base tag with single quotes
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_UpdateExistingBaseTagSingleQuotes()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");

      String originalHtml = "<html><head><base href='/old-path/'><title>Test</title></head><body></body></html>";

      String result = provider.rewriteIndexHtmlPaths(originalHtml, "/admin");

      assertTrue(result.contains("<base href=\"/admin/\">"), "Should update base tag and normalize to double quotes");
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - case insensitive matching
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_CaseInsensitive()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");

      //////////////////////////////
      // Test with uppercase HEAD //
      //////////////////////////////
      String html1   = "<HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY></BODY></HTML>";
      String result1 = provider.rewriteIndexHtmlPaths(html1, "/admin");
      assertTrue(result1.contains("<base href=\"/admin/\">"), "Should inject base tag even with uppercase HEAD");

      //////////////////////////////////
      // Test with uppercase BASE tag //
      //////////////////////////////////
      String html2   = "<html><head><BASE HREF='/'><title>Test</title></head><body></body></html>";
      String result2 = provider.rewriteIndexHtmlPaths(html2, "/admin");
      assertTrue(result2.contains("href=\"/admin/\""), "Should update uppercase BASE tag");
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - head tag with attributes
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_HeadTagWithAttributes()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");

      String originalHtml = "<html><head lang=\"en\" data-theme=\"dark\"><title>Test</title></head><body></body></html>";

      String result = provider.rewriteIndexHtmlPaths(originalHtml, "/admin");

      assertTrue(result.contains("<base href=\"/admin/\">"), "Should inject base tag even when head has attributes");
      assertTrue(result.contains("head lang=\"en\" data-theme=\"dark\""), "Should preserve head tag attributes");
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - base tag with additional attributes
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_BaseTagWithAttributes()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");

      String originalHtml = "<html><head><base href=\"/\" target=\"_blank\"><title>Test</title></head><body></body></html>";

      String result = provider.rewriteIndexHtmlPaths(originalHtml, "/admin");

      assertTrue(result.contains("href=\"/admin/\""), "Should update href attribute");
      assertTrue(result.contains("target=\"_blank\""), "Should preserve other base tag attributes");
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - real Vite SPA output
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_ViteOutput()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");

      // Typical Vite build output with relative paths
      String viteHtml = """
         <!DOCTYPE html>
         <html lang="en">
           <head>
             <meta charset="UTF-8" />
             <link rel="icon" type="image/svg+xml" href="./vite.svg" />
             <meta name="viewport" content="width=device-width, initial-scale=1.0" />
             <title>Vite App</title>
             <script type="module" crossorigin src="./assets/index-abc123.js"></script>
             <link rel="modulepreload" crossorigin href="./assets/vendor-def456.js">
             <link rel="stylesheet" href="./assets/index-ghi789.css">
           </head>
           <body>
             <div id="app"></div>
           </body>
         </html>
         """;

      String result = provider.rewriteIndexHtmlPaths(viteHtml, "/admin");

      assertTrue(result.contains("<base href=\"/admin/\">"), "Should inject base tag for Vite output");
      //////////////////////////////////////////////////////////////////////////////
      // The base tag will make the browser resolve all ./assets/ paths correctly //
      //////////////////////////////////////////////////////////////////////////////
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - real Create React App output
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_CreateReactAppOutput()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/customer", "customer-spa/");

      //////////////////////////////
      // Typical CRA build output //
      //////////////////////////////
      String craHtml = """
         <!doctype html>
         <html lang="en">
           <head>
             <meta charset="utf-8"/>
             <link rel="icon" href="./favicon.ico"/>
             <meta name="viewport" content="width=device-width,initial-scale=1"/>
             <title>React App</title>
             <script defer="defer" src="./static/js/main.abc123.js"></script>
             <link href="./static/css/main.def456.css" rel="stylesheet">
           </head>
           <body>
             <noscript>You need to enable JavaScript to run this app.</noscript>
             <div id="root"></div>
           </body>
         </html>
         """;

      String result = provider.rewriteIndexHtmlPaths(craHtml, "/customer");

      assertTrue(result.contains("<base href=\"/customer/\">"), "Should inject base tag for CRA output");
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - Angular output (no ./ prefix)
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_AngularOutput()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/portal", "portal-spa/");

      ////////////////////////////////////////////////
      // Angular often uses paths without ./ prefix //
      ////////////////////////////////////////////////
      String angularHtml = """
         <!DOCTYPE html>
         <html lang="en">
           <head>
             <meta charset="utf-8">
             <title>Angular App</title>
             <base href="/">
             <meta name="viewport" content="width=device-width, initial-scale=1">
             <link rel="icon" type="image/x-icon" href="favicon.ico">
             <link rel="stylesheet" href="styles.abc123.css">
           </head>
           <body>
             <app-root></app-root>
             <script src="runtime.def456.js" type="module"></script>
             <script src="polyfills.ghi789.js" type="module"></script>
             <script src="main.jkl012.js" type="module"></script>
           </body>
         </html>
         """;

      String result = provider.rewriteIndexHtmlPaths(angularHtml, "/portal");

      ///////////////////////////////////////////////////////
      // Angular already has a base tag - should update it //
      ///////////////////////////////////////////////////////
      assertTrue(result.contains("<base href=\"/portal/\">"), "Should update Angular's existing base tag");
      assertFalse(result.contains("<base href=\"/\">"), "Should not contain original root base tag");
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - multiple SPAs don't interfere
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_MultipleSPAs()
   {
      IsolatedSpaRouteProvider adminProvider    = new IsolatedSpaRouteProvider("/admin", "admin-spa/");
      IsolatedSpaRouteProvider customerProvider = new IsolatedSpaRouteProvider("/customer", "customer-spa/");

      String html = "<html><head><title>Test</title></head><body></body></html>";

      String adminResult    = adminProvider.rewriteIndexHtmlPaths(html, "/admin");
      String customerResult = customerProvider.rewriteIndexHtmlPaths(html, "/customer");

      assertTrue(adminResult.contains("<base href=\"/admin/\">"), "Admin SPA should have /admin/ base");
      assertTrue(customerResult.contains("<base href=\"/customer/\">"), "Customer SPA should have /customer/ base");
      assertFalse(adminResult.contains("/customer/"), "Admin result should not contain customer path");
      assertFalse(customerResult.contains("/admin/"), "Customer result should not contain admin path");
   }



   /*******************************************************************************
    ** Test rewriteIndexHtmlPaths - handles whitespace variations
    *******************************************************************************/
   @Test
   void testRewriteIndexHtmlPaths_WhitespaceVariations()
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");

      ////////////////////////////////////////
      // Various whitespace around base tag //
      ////////////////////////////////////////
      String html1   = "<html><head><base    href  =  \"/\"   ><title>Test</title></head><body></body></html>";
      String result1 = provider.rewriteIndexHtmlPaths(html1, "/admin");
      assertTrue(result1.contains("href=\"/admin/\""), "Should handle extra whitespace in base tag");

      ///////////////////
      // No whitespace //
      ///////////////////
      String html2   = "<html><head><base href=\"/\"><title>Test</title></head><body></body></html>";
      String result2 = provider.rewriteIndexHtmlPaths(html2, "/admin");
      assertTrue(result2.contains("href=\"/admin/\""), "Should handle base tag with no extra whitespace");
   }



   /*******************************************************************************
    ** Helper method to count occurrences of a substring
    *******************************************************************************/
   private int countOccurrences(String str, String substring)
   {
      int count = 0;
      int index = 0;
      while((index = str.indexOf(substring, index)) != -1)
      {
         count++;
         index += substring.length();
      }
      return count;
   }

}
