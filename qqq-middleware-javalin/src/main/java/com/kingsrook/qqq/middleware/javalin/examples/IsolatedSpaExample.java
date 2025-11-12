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

package com.kingsrook.qqq.middleware.javalin.examples;


import java.util.List;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.routeproviders.IsolatedSpaRouteProvider;


/*******************************************************************************
 ** Example showing how to configure multiple SPAs using IsolatedSpaRouteProvider
 **
 ** This example demonstrates:
 ** 1. Root SPA at "/" with exclusions for other SPAs and APIs
 ** 2. Admin SPA at "/admin" with authentication
 ** 3. Customer SPA at "/customer" 
 ** 4. All APIs remain functional at their original paths
 **
 ** Key Features:
 ** - Each SPA handles its own 404s and deep linking
 ** - SPAs are completely isolated from each other
 ** - API endpoints continue to work normally
 ** - Support for authentication per SPA
 ** - Support for custom before/after handlers per SPA
 *******************************************************************************/
public class IsolatedSpaExample
{
   /***************************************************************************
    **
    ***************************************************************************/
   public static void main(String[] args) throws Exception
   {
      // Create the QQQ application
      AbstractQQQApplication application = new ExampleApplication();

      // Configure the Javalin server with multiple SPAs
      QApplicationJavalinServer server = new QApplicationJavalinServer(application)
         .withPort(8080)
         .withServeFrontendMaterialDashboard(false)  // Disable default dashboard
         .withServeLegacyUnversionedMiddlewareAPI(true)  // Keep APIs enabled

         // Root SPA - serves the main public website
         .withAdditionalRouteProvider(
            new IsolatedSpaRouteProvider("/", "public-site/")
               .withSpaIndexFile("public-site/index.html")
               .withExcludedPaths(List.of("/admin", "/customer", "/api", "/qqq-api",
                  "/metaData", "/data", "/processes", "/reports", "/download"))
               .withDeepLinking(true)
               .withLoadFromJar(false))  // Load from filesystem for development

         // Admin SPA - requires authentication
         .withAdditionalRouteProvider(
            new IsolatedSpaRouteProvider("/admin", "admin-spa/dist/")
               .withSpaIndexFile("admin-spa/dist/index.html")
               .withAuthenticator(new QCodeReference(AdminAuthenticator.class))
               .withDeepLinking(true)
               .withLoadFromJar(true))  // Load from JAR for production

         // Customer SPA - no authentication required
         .withAdditionalRouteProvider(
            new IsolatedSpaRouteProvider("/customer", "customer-spa/build/")
               .withSpaIndexFile("customer-spa/build/index.html")
               .withDeepLinking(true)
               .withLoadFromJar(false));

      // Start the server
      server.start();

      System.out.println("Server started with multiple SPAs:");
      System.out.println("- Root SPA: http://localhost:8080/");
      System.out.println("- Admin SPA: http://localhost:8080/admin/");
      System.out.println("- Customer SPA: http://localhost:8080/customer/");
      System.out.println("- APIs: http://localhost:8080/metaData, /data, /processes, etc.");
   }



   /*******************************************************************************
    ** Example QQQ Application
    *******************************************************************************/
   public static class ExampleApplication extends AbstractQQQApplication
   {
      @Override
      public QInstance defineQInstance()
      {
         // Define your QInstance here
         // This is where you would configure your tables, processes, etc.
         return null;  // Replace with actual implementation
      }
   }



   /*******************************************************************************
    ** Example Admin Authenticator
    *******************************************************************************/
   public static class AdminAuthenticator
   {
      // This would implement RouteAuthenticatorInterface
      // For this example, we're just showing the structure
   }
}
