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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for AuthScope
 *******************************************************************************/
class AuthScopeTest extends BaseTest
{

   /*******************************************************************************
    ** Test instanceDefault() factory method
    *******************************************************************************/
   @Test
   void testInstanceDefault()
   {
      AuthScope.InstanceDefault scope1 = AuthScope.instanceDefault();
      AuthScope.InstanceDefault scope2 = AuthScope.instanceDefault();

      assertNotNull(scope1);
      assertNotNull(scope2);
      assertEquals(scope1, scope2);
      assertEquals(scope1.hashCode(), scope2.hashCode());
   }


   /*******************************************************************************
    ** Test Api scope factory method and equality
    *******************************************************************************/
   @Test
   void testApiScope()
   {
      // Create mock API metadata objects (using same instance for equality)
      MockApiMetaData apiMetaData1 = new MockApiMetaData("test-api");
      MockApiMetaData apiMetaData2 = apiMetaData1; // Same instance
      MockApiMetaData apiMetaData3 = new MockApiMetaData("other-api");

      AuthScope.Api scope1 = AuthScope.api(apiMetaData1);
      AuthScope.Api scope2 = AuthScope.api(apiMetaData2);
      AuthScope.Api scope3 = AuthScope.api(apiMetaData3);

      assertNotNull(scope1);
      assertNotNull(scope2);
      assertNotNull(scope3);

      // Same metadata instance should produce equal scopes
      assertEquals(scope1, scope2);
      assertEquals(scope1.hashCode(), scope2.hashCode());

      // Different metadata should produce different scopes
      assertNotEquals(scope1, scope3);
   }


   /*******************************************************************************
    ** Test RouteProvider scope factory method and equality
    *******************************************************************************/
   @Test
   void testRouteProviderScope()
   {
      // Create mock route provider metadata objects (using same instance for equality)
      MockRouteMetaData routeMetaData1 = new MockRouteMetaData("test-route");
      MockRouteMetaData routeMetaData2 = routeMetaData1; // Same instance
      MockRouteMetaData routeMetaData3 = new MockRouteMetaData("other-route");

      AuthScope.RouteProvider scope1 = AuthScope.routeProvider(routeMetaData1);
      AuthScope.RouteProvider scope2 = AuthScope.routeProvider(routeMetaData2);
      AuthScope.RouteProvider scope3 = AuthScope.routeProvider(routeMetaData3);

      assertNotNull(scope1);
      assertNotNull(scope2);
      assertNotNull(scope3);

      // Same metadata instance should produce equal scopes
      assertEquals(scope1, scope2);
      assertEquals(scope1.hashCode(), scope2.hashCode());

      // Different metadata should produce different scopes
      assertNotEquals(scope1, scope3);
   }


   /*******************************************************************************
    ** Test that different scope types are not equal
    *******************************************************************************/
   @Test
   void testDifferentScopeTypesNotEqual()
   {
      AuthScope.InstanceDefault defaultScope = AuthScope.instanceDefault();
      MockApiMetaData apiMetaData = new MockApiMetaData("test-api");
      AuthScope.Api apiScope = AuthScope.api(apiMetaData);
      MockRouteMetaData routeMetaData = new MockRouteMetaData("test-route");
      AuthScope.RouteProvider routeScope = AuthScope.routeProvider(routeMetaData);

      assertNotEquals(defaultScope, apiScope);
      assertNotEquals(defaultScope, routeScope);
      assertNotEquals(apiScope, routeScope);
   }


   /*******************************************************************************
    ** Helper classes for mock metadata
    *******************************************************************************/

   /*******************************************************************************
    ** Mock API metadata for testing
    *******************************************************************************/
   private static class MockApiMetaData
   {
      private final String name;


      /*******************************************************************************
       ** Constructor
       **
       ** @param name The API name
       *******************************************************************************/
      MockApiMetaData(String name)
      {
         this.name = name;
      }


      /*******************************************************************************
       ** Getter for name
       **
       ** @return The API name
       *******************************************************************************/
      String getName()
      {
         return name;
      }
   }


   /*******************************************************************************
    ** Mock route provider metadata for testing
    *******************************************************************************/
   private static class MockRouteMetaData
   {
      private final String name;


      /*******************************************************************************
       ** Constructor
       **
       ** @param name The route provider name
       *******************************************************************************/
      MockRouteMetaData(String name)
      {
         this.name = name;
      }


      /*******************************************************************************
       ** Getter for name
       **
       ** @return The route provider name
       *******************************************************************************/
      String getName()
      {
         return name;
      }
   }
}

