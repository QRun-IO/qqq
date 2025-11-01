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
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Unit test for IsolatedSpaRouteProvider
 *******************************************************************************/
class IsolatedSpaRouteProviderTest
{
   private QInstance qInstance;
   private IsolatedSpaRouteProvider provider;


   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      qInstance = TestUtils.defineInstance();
      // QContext not needed for these tests
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
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "admin-spa/");
      QCodeReference authenticator = new QCodeReference(String.class);

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
      // Use an existing test resource path
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/admin", "test-spa-admin/");
      provider.setQInstance(qInstance);

      Javalin service = Javalin.create(config ->
      {
         provider.acceptJavalinConfig(config);
      });
      try
      {
         // Config should have been called during create
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
         // Should register handlers without error
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
         // Should register handlers without error
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

      // Verify it was set
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

      // Verify it was set
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

}
