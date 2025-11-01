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

import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.metadata.JavalinRouteProviderMetaData;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for SimpleFileSystemDirectoryRouter
 *******************************************************************************/
class SimpleFileSystemDirectoryRouterTest
{
   private QInstance qInstance;


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
   }


   /*******************************************************************************
    ** Test constructor with path parameters
    *******************************************************************************/
   @Test
   void testConstructor()
   {
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("/static", "static-files/");

      // hostedPath is private, test that router was created
      assertNotNull(router);
   }


   /*******************************************************************************
    ** Test constructor with metadata
    *******************************************************************************/
   @Test
   void testConstructorWithMetadata()
   {
      JavalinRouteProviderMetaData metadata = new JavalinRouteProviderMetaData();
      metadata.setHostedPath("/static");
      metadata.setFileSystemPath("static-files/");

      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter(metadata);

      // hostedPath is private, test that router was created
      assertNotNull(router);
   }


   /*******************************************************************************
    ** Test fluent setters
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("/static", "static-files/");
      QCodeReference authenticator = new QCodeReference(String.class);

      SimpleFileSystemDirectoryRouter result = router
         .withRouteAuthenticator(authenticator)
         .withSpaRootPath("/spa")
         .withSpaRootFile("index.html");

      assertNotNull(result);
      assertEquals(router, result);
      assertEquals("/spa", router.getSpaRootPath());
      assertEquals("index.html", router.getSpaRootFile());
      assertEquals(authenticator, router.getRouteAuthenticator());
   }


   /*******************************************************************************
    ** Test acceptJavalinConfig
    *******************************************************************************/
   @Test
   void testAcceptJavalinConfig()
   {
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("/static", "src/test/resources");
      router.setQInstance(qInstance);

      Javalin service = Javalin.create(config ->
      {
         router.acceptJavalinConfig(config);
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
    ** Test acceptJavalinService
    *******************************************************************************/
   @Test
   void testAcceptJavalinService()
   {
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("/static", "src/test/resources");
      router.setQInstance(qInstance);

      Javalin service = Javalin.create();
      try
      {
         router.acceptJavalinService(service);
         // Should not throw
      }
      finally
      {
         service.stop();
      }
   }


   /*******************************************************************************
    ** Test acceptJavalinService with SPA configuration
    *******************************************************************************/
   @Test
   void testAcceptJavalinServiceWithSpa()
   {
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("/static", "src/test/resources/public-site/");
      router.setQInstance(qInstance);
      router.withSpaRootPath("/static");
      router.withSpaRootFile("index.html");

      Javalin service = Javalin.create();
      try
      {
         router.acceptJavalinService(service);
         // Should register 404 handler for SPA
         assertNotNull(service);
      }
      finally
      {
         service.stop();
      }
   }


   /*******************************************************************************
    ** Test withRouteAuthenticator
    *******************************************************************************/
   @Test
   void testWithRouteAuthenticator()
   {
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("/static", "static-files/");
      QCodeReference authenticator = new QCodeReference(String.class);

      SimpleFileSystemDirectoryRouter result = router.withRouteAuthenticator(authenticator);

      assertNotNull(result);
      assertEquals(authenticator, router.getRouteAuthenticator());
   }


   /*******************************************************************************
    ** Test acceptJavalinConfig with JAR loading enabled
    *******************************************************************************/
   @Test
   void testAcceptJavalinConfig_WithJarLoading()
   {
      System.setProperty("qqq.javalin.enableStaticFilesFromJar", "true");
      
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("/static", "src/test/resources/public-site/");
      router.setQInstance(qInstance);

      Javalin service = Javalin.create(config ->
      {
         router.acceptJavalinConfig(config);
      });
      try
      {
         assertNotNull(service);
      }
      finally
      {
         service.stop();
         System.clearProperty("qqq.javalin.enableStaticFilesFromJar");
      }
   }


   /*******************************************************************************
    ** Test acceptJavalinConfig with path not starting with slash
    *******************************************************************************/
   @Test
   void testAcceptJavalinConfig_PathWithoutLeadingSlash()
   {
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("static", "src/test/resources");
      router.setQInstance(qInstance);

      Javalin service = Javalin.create(config ->
      {
         router.acceptJavalinConfig(config);
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

}
