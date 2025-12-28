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

package com.kingsrook.qqq.backend.core.model.metadata.authentication;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit tests for AuthenticationResolver
 *******************************************************************************/
class AuthenticationResolverTest extends BaseTest
{

   /*******************************************************************************
    ** Test resolution falls back to instance default when no scoped provider
    *******************************************************************************/
   @Test
   void testResolveFallsBackToInstanceDefault() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QAuthenticationMetaData defaultAuth = qInstance.getAuthentication();

      AuthResolutionContext context = new AuthResolutionContext()
         .withRequestPath("/test/path");

      QAuthenticationMetaData resolved = AuthenticationResolver.resolve(qInstance, context);

      assertNotNull(resolved);
      assertEquals(defaultAuth, resolved);
   }


   /*******************************************************************************
    ** Test resolution with API scope
    *******************************************************************************/
   @Test
   void testResolveWithApiScope() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      // Create API-specific auth
      QAuthenticationMetaData apiAuth = new QAuthenticationMetaData()
         .withName("api-auth")
         .withType(QAuthenticationType.MOCK);

      // Create mock API metadata
      Object apiMetaData = createMockApiMetaData("test-api");

      // Register API-specific auth
      AuthScope.Api apiScope = AuthScope.api(apiMetaData);
      qInstance.registerAuthenticationProvider(apiScope, apiAuth);

      // Resolve with API context
      AuthResolutionContext context = new AuthResolutionContext()
         .withApiMetaData(apiMetaData)
         .withRequestPath("/api/test");

      QAuthenticationMetaData resolved = AuthenticationResolver.resolve(qInstance, context);

      assertNotNull(resolved);
      assertEquals(apiAuth, resolved);
   }


   /*******************************************************************************
    ** Test resolution falls back when API scope not found
    *******************************************************************************/
   @Test
   void testResolveFallsBackWhenApiScopeNotFound() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QAuthenticationMetaData defaultAuth = qInstance.getAuthentication();

      // Create API metadata but don't register scoped auth
      Object apiMetaData = createMockApiMetaData("test-api");

      // Resolve with API context
      AuthResolutionContext context = new AuthResolutionContext()
         .withApiMetaData(apiMetaData)
         .withRequestPath("/api/test");

      QAuthenticationMetaData resolved = AuthenticationResolver.resolve(qInstance, context);

      // Should fall back to instance default
      assertNotNull(resolved);
      assertEquals(defaultAuth, resolved);
   }


   /*******************************************************************************
    ** Test resolution throws exception when no provider found
    *******************************************************************************/
   @Test
   void testResolveThrowsWhenNoProviderFound()
   {
      QInstance qInstance = new QInstance();
      // Don't set instance default auth

      AuthResolutionContext context = new AuthResolutionContext()
         .withRequestPath("/test/path");

      assertThrows(QException.class, () ->
      {
         AuthenticationResolver.resolve(qInstance, context);
      });
   }


   /*******************************************************************************
    ** Test resolution precedence: API > RouteProvider > InstanceDefault
    *******************************************************************************/
   @Test
   void testResolutionPrecedence() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      // Create different auth providers
      QAuthenticationMetaData defaultAuth = qInstance.getAuthentication();
      QAuthenticationMetaData apiAuth = new QAuthenticationMetaData()
         .withName("api-auth")
         .withType(QAuthenticationType.MOCK);
      QAuthenticationMetaData routeAuth = new QAuthenticationMetaData()
         .withName("route-auth")
         .withType(QAuthenticationType.MOCK);

      // Create mock metadata
      Object apiMetaData = createMockApiMetaData("test-api");
      Object routeMetaData = createMockRouteMetaData("test-route");

      // Register scoped providers
      qInstance.registerAuthenticationProvider(AuthScope.api(apiMetaData), apiAuth);
      qInstance.registerAuthenticationProvider(AuthScope.routeProvider(routeMetaData), routeAuth);

      // Test API scope takes precedence
      AuthResolutionContext apiContext = new AuthResolutionContext()
         .withApiMetaData(apiMetaData)
         .withRouteMetaData(routeMetaData);
      QAuthenticationMetaData resolved = AuthenticationResolver.resolve(qInstance, apiContext);
      assertEquals(apiAuth, resolved);

      // Test route provider scope when no API
      AuthResolutionContext routeContext = new AuthResolutionContext()
         .withRouteMetaData(routeMetaData);
      resolved = AuthenticationResolver.resolve(qInstance, routeContext);
      assertEquals(routeAuth, resolved);

      // Test instance default when no scoped providers
      AuthResolutionContext defaultContext = new AuthResolutionContext();
      resolved = AuthenticationResolver.resolve(qInstance, defaultContext);
      assertEquals(defaultAuth, resolved);
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

