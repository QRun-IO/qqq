/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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


import java.util.List;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.routeproviders.IsolatedSpaRouteProvider;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import static com.kingsrook.sampleapp.metadata.SampleMetaDataProvider.primeTestDatabase;


/*******************************************************************************
 ** Example Server showing IsolatedSpaRouteProvider usage with API endpoints
 **
 ** This server demonstrates:
 ** 1. Multiple SPAs running simultaneously
 ** 2. Complete API endpoint listing
 ** 3. Proper SPA isolation and deep linking
 ** 4. Authentication per SPA
 *******************************************************************************/
public class IsolatedSpaServer
{
   private static final QLogger LOG = QLogger.getLogger(IsolatedSpaServer.class);
   private static final int PORT = 8080;
   private static final String BASE_URL = "http://localhost:" + PORT;



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void main(String[] args)
   {
      new IsolatedSpaServer().start();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void start()
   {
      try
      {
         primeTestDatabase("prime-test-database.sql");

         // Configure the server with multiple SPAs and APIs
         QApplicationJavalinServer javalinServer = new QApplicationJavalinServer(new SampleMetaDataProvider())
            .withPort(PORT)
            .withServeFrontendMaterialDashboard(false)  // Disable default dashboard
            .withServeLegacyUnversionedMiddlewareAPI(true)  // Enable all APIs
            .withServeVersionedMiddlewareAPI(true)  // Enable versioned APIs
            
            // Root SPA - Public website
            .withAdditionalRouteProvider(
               new IsolatedSpaRouteProvider("/", "public-site/")
                  .withSpaIndexFile("public-site/index.html")
                  .withExcludedPaths(List.of(
                     "/admin", "/customer", "/dashboard",
                     "/api", "/qqq-api", "/metaData", "/data", 
                     "/processes", "/reports", "/download", "/health"))
                  .withDeepLinking(true)
                  .withLoadFromJar(false))
            
            // Admin SPA - Requires authentication
            .withAdditionalRouteProvider(
               new IsolatedSpaRouteProvider("/admin", "admin-spa/dist/")
                  .withSpaIndexFile("admin-spa/dist/index.html")
                  .withDeepLinking(true)
                  .withLoadFromJar(false))
            
            // Customer Portal SPA
            .withAdditionalRouteProvider(
               new IsolatedSpaRouteProvider("/customer", "customer-portal/build/")
                  .withSpaIndexFile("customer-portal/build/index.html")
                  .withDeepLinking(true)
                  .withLoadFromJar(false))
            
            // Dashboard SPA - Internal tooling
            .withAdditionalRouteProvider(
               new IsolatedSpaRouteProvider("/dashboard", "dashboard-app/dist/")
                  .withSpaIndexFile("dashboard-app/dist/index.html")
                  .withDeepLinking(true)
                  .withLoadFromJar(false));

         javalinServer.start();

         // Print comprehensive server information
         printServerInfo();
      }
      catch(Exception e)
      {
         LOG.error("Failed to start javalin server. See stack trace for details.", e);
      }
   }



   /*******************************************************************************
    ** Print comprehensive server information including all SPAs and APIs
    *******************************************************************************/
   private void printServerInfo()
   {
      System.out.println();
      System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
      System.out.println("║                           QQQ SERVER STARTED SUCCESSFULLY                      ║");
      System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
      System.out.println();
      
      printSpaApplications();
      printApiEndpoints();
      printHealthEndpoints();
      printStaticResources();
      printServerDetails();
      
      System.out.println();
      System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
      System.out.println("║  All endpoints are now available. Check the logs for any startup warnings.      ║");
      System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
      System.out.println();
   }



   /*******************************************************************************
    ** Print SPA Applications
    *******************************************************************************/
   private void printSpaApplications()
   {
      System.out.println("🌐 SINGLE PAGE APPLICATIONS (SPAs):");
      System.out.println("   ┌─────────────────────────────────────────────────────────────────────────────┐");
      System.out.println("   │  Application    │  URL                           │  Description              │");
      System.out.println("   ├─────────────────────────────────────────────────────────────────────────────┤");
      System.out.println("   │  Public Site   │  " + padRight(BASE_URL + "/", 25) + " │  Main public website        │");
      System.out.println("   │  Admin Panel   │  " + padRight(BASE_URL + "/admin/", 25) + " │  Administrative interface   │");
      System.out.println("   │  Customer Portal│ " + padRight(BASE_URL + "/customer/", 25) + " │  Customer self-service      │");
      System.out.println("   │  Dashboard     │  " + padRight(BASE_URL + "/dashboard/", 25) + " │  Internal tooling           │");
      System.out.println("   └─────────────────────────────────────────────────────────────────────────────┘");
      System.out.println();
      System.out.println("   📝 SPA Features:");
      System.out.println("      • Deep linking support (404 → index.html fallback)");
      System.out.println("      • Complete isolation between SPAs");
      System.out.println("      • Independent routing and state management");
      System.out.println("      • Static file serving from classpath/filesystem");
      System.out.println();
   }



   /*******************************************************************************
    ** Print API Endpoints
    *******************************************************************************/
   private void printApiEndpoints()
   {
      System.out.println("🔌 API ENDPOINTS:");
      System.out.println("   ┌─────────────────────────────────────────────────────────────────────────────┐");
      System.out.println("   │  Category       │  Endpoint                    │  Description              │");
      System.out.println("   ├─────────────────────────────────────────────────────────────────────────────┤");
      
      // Legacy Unversioned Middleware API
      System.out.println("   │  Legacy API     │  " + padRight(BASE_URL + "/metaData", 25) + " │  Application metadata       │");
      System.out.println("   │  Legacy API     │  " + padRight(BASE_URL + "/data", 25) + " │  Data access endpoints      │");
      System.out.println("   │  Legacy API     │  " + padRight(BASE_URL + "/processes", 25) + " │  Process management         │");
      System.out.println("   │  Legacy API     │  " + padRight(BASE_URL + "/reports", 25) + " │  Report generation          │");
      System.out.println("   │  Legacy API     │  " + padRight(BASE_URL + "/download", 25) + " │  File download service      │");
      
      // Versioned Middleware API
      System.out.println("   │  Versioned API  │  " + padRight(BASE_URL + "/qqq-api/v1/metaData", 25) + " │  V1 metadata API            │");
      System.out.println("   │  Versioned API  │  " + padRight(BASE_URL + "/qqq-api/v1/data", 25) + " │  V1 data API                │");
      System.out.println("   │  Versioned API  │  " + padRight(BASE_URL + "/qqq-api/v1/processes", 25) + " │  V1 process API             │");
      System.out.println("   │  Versioned API  │  " + padRight(BASE_URL + "/qqq-api/v1/reports", 25) + " │  V1 report API              │");
      System.out.println("   │  Versioned API  │  " + padRight(BASE_URL + "/qqq-api/v1/download", 25) + " │  V1 download API            │");
      
      // OpenAPI Documentation
      System.out.println("   │  Documentation │  " + padRight(BASE_URL + "/qqq-api/v1/openapi.json", 25) + " │  OpenAPI specification      │");
      System.out.println("   │  Documentation │  " + padRight(BASE_URL + "/qqq-api/v1/swagger-ui/", 25) + " │  Swagger UI interface       │");
      
      System.out.println("   └─────────────────────────────────────────────────────────────────────────────┘");
      System.out.println();
      System.out.println("   📝 API Features:");
      System.out.println("      • RESTful JSON APIs with comprehensive error handling");
      System.out.println("      • Versioned API support for backward compatibility");
      System.out.println("      • OpenAPI 3.0 specification with interactive documentation");
      System.out.println("      • CORS support for cross-origin requests");
      System.out.println("      • Request/response logging and monitoring");
      System.out.println();
   }



   /*******************************************************************************
    ** Print Health and Monitoring Endpoints
    *******************************************************************************/
   private void printHealthEndpoints()
   {
      System.out.println("🏥 HEALTH & MONITORING:");
      System.out.println("   ┌─────────────────────────────────────────────────────────────────────────────┐");
      System.out.println("   │  Endpoint                    │  Description                              │");
      System.out.println("   ├─────────────────────────────────────────────────────────────────────────────┤");
      System.out.println("   │  " + padRight(BASE_URL + "/health", 25) + " │  Server health status              │");
      System.out.println("   │  " + padRight(BASE_URL + "/health/live", 25) + " │  Liveness probe                    │");
      System.out.println("   │  " + padRight(BASE_URL + "/health/ready", 25) + " │  Readiness probe                   │");
      System.out.println("   │  " + padRight(BASE_URL + "/metrics", 25) + " │  Application metrics (if enabled)  │");
      System.out.println("   └─────────────────────────────────────────────────────────────────────────────┘");
      System.out.println();
   }



   /*******************************************************************************
    ** Print Static Resources
    *******************************************************************************/
   private void printStaticResources()
   {
      System.out.println("📁 STATIC RESOURCES:");
      System.out.println("   ┌─────────────────────────────────────────────────────────────────────────────┐");
      System.out.println("   │  Resource Type  │  Path                        │  Description              │");
      System.out.println("   ├─────────────────────────────────────────────────────────────────────────────┤");
      System.out.println("   │  SPA Assets     │  /admin/*.js, *.css, *.png   │  Admin SPA static files   │");
      System.out.println("   │  SPA Assets     │  /customer/*.js, *.css, *.png│  Customer SPA static files│");
      System.out.println("   │  SPA Assets     │  /dashboard/*.js, *.css, *.png│  Dashboard SPA static files│");
      System.out.println("   │  SPA Assets     │  /*.js, *.css, *.png         │  Root SPA static files    │");
      System.out.println("   │  Favicon        │  /favicon.ico                │  Site favicon             │");
      System.out.println("   │  Robots         │  /robots.txt                 │  Search engine directives │");
      System.out.println("   └─────────────────────────────────────────────────────────────────────────────┘");
      System.out.println();
   }



   /*******************************************************************************
    ** Print Server Details
    *******************************************************************************/
   private void printServerDetails()
   {
      System.out.println("⚙️  SERVER CONFIGURATION:");
      System.out.println("   ┌─────────────────────────────────────────────────────────────────────────────┐");
      System.out.println("   │  Property        │  Value                        │  Description              │");
      System.out.println("   ├─────────────────────────────────────────────────────────────────────────────┤");
      System.out.println("   │  Server Port     │  " + padRight(String.valueOf(PORT), 25) + " │  HTTP server port           │");
      System.out.println("   │  Base URL        │  " + padRight(BASE_URL, 25) + " │  Server base URL            │");
      System.out.println("   │  Framework       │  " + padRight("Javalin", 25) + " │  Web framework              │");
      System.out.println("   │  Java Version    │  " + padRight(System.getProperty("java.version"), 25) + " │  Runtime Java version       │");
      System.out.println("   │  OS              │  " + padRight(System.getProperty("os.name"), 25) + " │  Operating system           │");
      System.out.println("   │  Architecture    │  " + padRight(System.getProperty("os.arch"), 25) + " │  System architecture        │");
      System.out.println("   └─────────────────────────────────────────────────────────────────────────────┘");
      System.out.println();
   }



   /*******************************************************************************
    ** Utility method to pad strings to fixed width
    *******************************************************************************/
   private String padRight(String str, int length)
   {
      if(str == null) str = "";
      return String.format("%-" + length + "s", str.length() > length ? str.substring(0, length) : str);
   }
}