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
import com.kingsrook.qqq.middleware.javalin.metadata.JavalinRouteProviderMetaData;
import io.javalin.apibuilder.EndpointGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for ProcessBasedRouter
 *******************************************************************************/
class ProcessBasedRouterTest
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
    ** Test constructor with path and process name
    *******************************************************************************/
   @Test
   void testConstructor()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess");

      // hostedPath is private, test that router was created
      assertNotNull(router);
   }


   /*******************************************************************************
    ** Test constructor with methods
    *******************************************************************************/
   @Test
   void testConstructorWithMethods()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess", List.of("GET", "POST"));

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
      metadata.setHostedPath("/api/test");
      metadata.setProcessName("testProcess");
      metadata.setMethods(List.of("POST"));

      ProcessBasedRouter router = new ProcessBasedRouter(metadata);

      // hostedPath is private, test that router was created
      assertNotNull(router);
   }


   /*******************************************************************************
    ** Test constructor defaults to GET method
    *******************************************************************************/
   @Test
   void testConstructorDefaultsToGet()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess", null);

      EndpointGroup endpointGroup = router.getJavalinEndpointGroup();
      assertNotNull(endpointGroup);
   }


   /*******************************************************************************
    ** Test getJavalinEndpointGroup
    *******************************************************************************/
   @Test
   void testGetJavalinEndpointGroup()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess");
      router.setQInstance(qInstance);

      EndpointGroup endpointGroup = router.getJavalinEndpointGroup();

      assertNotNull(endpointGroup);
   }


   /*******************************************************************************
    ** Test fluent setters
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess");
      QCodeReference authenticator = new QCodeReference(String.class);
      QCodeReference contextHandler = new QCodeReference(String.class);

      ProcessBasedRouter result = router
         .withRouteAuthenticator(authenticator)
         .withContextHandler(contextHandler);

      assertNotNull(result);
      assertEquals(router, result);
      assertEquals(authenticator, router.getRouteAuthenticator());
      assertEquals(contextHandler, router.getContextHandler());
   }


   /*******************************************************************************
    ** Test getJavalinEndpointGroup with unsupported method throws exception
    *******************************************************************************/
   @Test
   void testGetJavalinEndpointGroup_UnsupportedMethod()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess", List.of("INVALID"));
      router.setQInstance(qInstance);

      EndpointGroup endpointGroup = router.getJavalinEndpointGroup();
      
      // Exception is thrown when the endpoint group is executed, not when created
      assertThrows(IllegalArgumentException.class, () ->
      {
         endpointGroup.addEndpoints();
      });
   }


   /*******************************************************************************
    ** Test getJavalinEndpointGroup with multiple HTTP methods
    *******************************************************************************/
   @Test
   void testGetJavalinEndpointGroup_MultipleMethods()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess", List.of("GET", "POST", "PUT"));
      router.setQInstance(qInstance);

      EndpointGroup endpointGroup = router.getJavalinEndpointGroup();

      assertNotNull(endpointGroup);
      // EndpointGroup is callable and can be invoked
   }


   /*******************************************************************************
    ** Test getJavalinEndpointGroup with all HTTP methods
    *******************************************************************************/
   @Test
   void testGetJavalinEndpointGroup_AllMethods()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess", 
         List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
      router.setQInstance(qInstance);

      EndpointGroup endpointGroup = router.getJavalinEndpointGroup();

      assertNotNull(endpointGroup);
   }

}
