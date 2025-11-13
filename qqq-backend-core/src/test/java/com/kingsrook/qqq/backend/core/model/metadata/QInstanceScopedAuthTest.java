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

package com.kingsrook.qqq.backend.core.model.metadata;


import java.util.Optional;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for QInstance scoped authentication provider registry
 *******************************************************************************/
class QInstanceScopedAuthTest extends BaseTest
{

   /*******************************************************************************
    ** Test that setAuthentication() automatically registers instance default
    *******************************************************************************/
   @Test
   void testSetAuthenticationAutoRegisters()
   {
      QInstance qInstance = new QInstance();
      QAuthenticationMetaData auth = new QAuthenticationMetaData()
         .withName("test-auth")
         .withType(QAuthenticationType.MOCK);

      qInstance.setAuthentication(auth);

      // Should be registered under instance default
      Optional<QAuthenticationMetaData> found =
         qInstance.findAuthenticationProvider(AuthScope.instanceDefault());
      assertTrue(found.isPresent());
      assertEquals(auth, found.get());

      // getAuthentication() should still work
      assertEquals(auth, qInstance.getAuthentication());
   }


   /*******************************************************************************
    ** Test registerAuthenticationProvider for instance default
    *******************************************************************************/
   @Test
   void testRegisterInstanceDefault()
   {
      QInstance qInstance = new QInstance();
      QAuthenticationMetaData auth = new QAuthenticationMetaData()
         .withName("test-auth")
         .withType(QAuthenticationType.MOCK);

      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), auth);

      Optional<QAuthenticationMetaData> found =
         qInstance.findAuthenticationProvider(AuthScope.instanceDefault());
      assertTrue(found.isPresent());
      assertEquals(auth, found.get());
   }


   /*******************************************************************************
    ** Test registerAuthenticationProvider for API scope
    *******************************************************************************/
   @Test
   void testRegisterApiScope()
   {
      QInstance qInstance = new QInstance();
      QAuthenticationMetaData apiAuth = new QAuthenticationMetaData()
         .withName("api-auth")
         .withType(QAuthenticationType.MOCK);

      Object apiMetaData = createMockApiMetaData("test-api");
      AuthScope.Api apiScope = AuthScope.api(apiMetaData);

      qInstance.registerAuthenticationProvider(apiScope, apiAuth);

      Optional<QAuthenticationMetaData> found = qInstance.findAuthenticationProvider(apiScope);
      assertTrue(found.isPresent());
      assertEquals(apiAuth, found.get());
   }


   /*******************************************************************************
    ** Test registerAuthenticationProvider for route provider scope
    *******************************************************************************/
   @Test
   void testRegisterRouteProviderScope()
   {
      QInstance qInstance = new QInstance();
      QAuthenticationMetaData routeAuth = new QAuthenticationMetaData()
         .withName("route-auth")
         .withType(QAuthenticationType.MOCK);

      Object routeMetaData = createMockRouteMetaData("test-route");
      AuthScope.RouteProvider routeScope = AuthScope.routeProvider(routeMetaData);

      qInstance.registerAuthenticationProvider(routeScope, routeAuth);

      Optional<QAuthenticationMetaData> found = qInstance.findAuthenticationProvider(routeScope);
      assertTrue(found.isPresent());
      assertEquals(routeAuth, found.get());
   }


   /*******************************************************************************
    ** Test findAuthenticationProvider returns empty when not found
    *******************************************************************************/
   @Test
   void testFindAuthenticationProviderReturnsEmpty()
   {
      QInstance qInstance = new QInstance();
      Object apiMetaData = createMockApiMetaData("test-api");
      AuthScope.Api apiScope = AuthScope.api(apiMetaData);

      Optional<QAuthenticationMetaData> found = qInstance.findAuthenticationProvider(apiScope);
      assertFalse(found.isPresent());
   }


   /*******************************************************************************
    ** Test withInstanceDefaultAuthentication fluent method
    *******************************************************************************/
   @Test
   void testWithInstanceDefaultAuthentication()
   {
      QInstance qInstance = new QInstance();
      QAuthenticationMetaData auth = new QAuthenticationMetaData()
         .withName("test-auth")
         .withType(QAuthenticationType.MOCK);

      QInstance result = qInstance.withInstanceDefaultAuthentication(auth);

      // Should return same instance (for chaining)
      assertEquals(qInstance, result);

      // Should be registered
      Optional<QAuthenticationMetaData> found =
         qInstance.findAuthenticationProvider(AuthScope.instanceDefault());
      assertTrue(found.isPresent());
      assertEquals(auth, found.get());
   }


   /*******************************************************************************
    ** Test multiple scoped providers can coexist
    *******************************************************************************/
   @Test
   void testMultipleScopedProviders()
   {
      QInstance qInstance = new QInstance();

      QAuthenticationMetaData defaultAuth = new QAuthenticationMetaData()
         .withName("default-auth")
         .withType(QAuthenticationType.MOCK);
      QAuthenticationMetaData apiAuth = new QAuthenticationMetaData()
         .withName("api-auth")
         .withType(QAuthenticationType.MOCK);
      QAuthenticationMetaData routeAuth = new QAuthenticationMetaData()
         .withName("route-auth")
         .withType(QAuthenticationType.MOCK);

      Object apiMetaData = createMockApiMetaData("test-api");
      Object routeMetaData = createMockRouteMetaData("test-route");

      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), defaultAuth);
      qInstance.registerAuthenticationProvider(AuthScope.api(apiMetaData), apiAuth);
      qInstance.registerAuthenticationProvider(AuthScope.routeProvider(routeMetaData), routeAuth);

      // All should be findable
      assertTrue(qInstance.findAuthenticationProvider(AuthScope.instanceDefault()).isPresent());
      assertTrue(qInstance.findAuthenticationProvider(AuthScope.api(apiMetaData)).isPresent());
      assertTrue(qInstance.findAuthenticationProvider(AuthScope.routeProvider(routeMetaData)).isPresent());

      // Should return correct providers
      assertEquals(defaultAuth, qInstance.findAuthenticationProvider(AuthScope.instanceDefault()).get());
      assertEquals(apiAuth, qInstance.findAuthenticationProvider(AuthScope.api(apiMetaData)).get());
      assertEquals(routeAuth, qInstance.findAuthenticationProvider(AuthScope.routeProvider(routeMetaData)).get());
   }


   /*******************************************************************************
    ** Create a mock API metadata object for testing
    *******************************************************************************/
   private Object createMockApiMetaData(String name)
   {
      return new Object()
      {
         @Override
         public boolean equals(Object obj)
         {
            return this == obj;
         }

         @Override
         public int hashCode()
         {
            return System.identityHashCode(this);
         }
      };
   }


   /*******************************************************************************
    ** Create a mock route provider metadata object for testing
    *******************************************************************************/
   private Object createMockRouteMetaData(String name)
   {
      return new Object()
      {
         @Override
         public boolean equals(Object obj)
         {
            return this == obj;
         }

         @Override
         public int hashCode()
         {
            return System.identityHashCode(this);
         }
      };
   }
}

