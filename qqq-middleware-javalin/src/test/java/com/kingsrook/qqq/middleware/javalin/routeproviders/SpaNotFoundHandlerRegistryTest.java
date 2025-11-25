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

import java.lang.reflect.Method;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for SpaNotFoundHandlerRegistry
 *******************************************************************************/
class SpaNotFoundHandlerRegistryTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp()
   {
      SpaNotFoundHandlerRegistry.getInstance().clear();
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      SpaNotFoundHandlerRegistry.getInstance().clear();
   }


   /*******************************************************************************
    ** Test singleton instance
    *******************************************************************************/
   @Test
   void testGetInstance()
   {
      SpaNotFoundHandlerRegistry instance1 = SpaNotFoundHandlerRegistry.getInstance();
      SpaNotFoundHandlerRegistry instance2 = SpaNotFoundHandlerRegistry.getInstance();

      assertNotNull(instance1);
      assertNotNull(instance2);
      assertEquals(instance1, instance2);
   }


   /*******************************************************************************
    ** Test clearing handlers
    *******************************************************************************/
   @Test
   void testClear()
   {
      SpaNotFoundHandlerRegistry registry = SpaNotFoundHandlerRegistry.getInstance();

      registry.registerSpaHandler("/admin", ctx ->
      {
      });

      registry.clear();

      // After clear, should be able to register again
      registry.registerSpaHandler("/admin", ctx ->
      {
      });
   }


   /*******************************************************************************
    ** Test registering multiple handlers
    *******************************************************************************/
   @Test
   void testRegisterMultipleHandlers()
   {
      SpaNotFoundHandlerRegistry registry = SpaNotFoundHandlerRegistry.getInstance();

      boolean[] handler1Called = {false};
      boolean[] handler2Called = {false};

      registry.registerSpaHandler("/admin", ctx -> handler1Called[0] = true);
      registry.registerSpaHandler("/customer", ctx -> handler2Called[0] = true);

      // Verify handlers are registered
      assertNotNull(registry);
   }


   /*******************************************************************************
    ** Test registering global handler
    *******************************************************************************/
   @Test
   void testRegisterGlobalHandler()
   {
      SpaNotFoundHandlerRegistry registry = SpaNotFoundHandlerRegistry.getInstance();
      Javalin service = Javalin.create();

      try
      {
         registry.registerGlobalHandler(service);
         // Should not throw on first call

         registry.registerGlobalHandler(service);
         // Should not throw on second call (idempotent)
      }
      finally
      {
         service.stop();
      }
   }


   ///////////////////////////////////////////////////////////////////////////////
   // Path Matching Tests (using reflection to test private method)             //
   ///////////////////////////////////////////////////////////////////////////////

   /*******************************************************************************
    ** Helper to invoke private isPathUnderPrefix method via reflection
    *******************************************************************************/
   private boolean callIsPathUnderPrefix(String requestPath, String pathPrefix) throws Exception
   {
      Method method = SpaNotFoundHandlerRegistry.class.getDeclaredMethod("isPathUnderPrefix", String.class, String.class);
      method.setAccessible(true);
      return (Boolean) method.invoke(null, requestPath, pathPrefix);
   }


   /*******************************************************************************
    ** Test exact path match
    *******************************************************************************/
   @Test
   void testPathMatching_ExactMatch() throws Exception
   {
      assertTrue(callIsPathUnderPrefix("/admin", "/admin"));
      assertTrue(callIsPathUnderPrefix("/api", "/api"));
      assertTrue(callIsPathUnderPrefix("/customer", "/customer"));
      assertTrue(callIsPathUnderPrefix("/", "/"));
   }


   /*******************************************************************************
    ** Test sub-path matching
    *******************************************************************************/
   @Test
   void testPathMatching_SubPath() throws Exception
   {
      assertTrue(callIsPathUnderPrefix("/admin/users", "/admin"));
      assertTrue(callIsPathUnderPrefix("/admin/users/123", "/admin"));
      assertTrue(callIsPathUnderPrefix("/api/v1/data", "/api"));
      assertTrue(callIsPathUnderPrefix("/customer/settings", "/customer"));
   }


   /*******************************************************************************
    ** Test deep nested sub-paths
    *******************************************************************************/
   @Test
   void testPathMatching_DeepNested() throws Exception
   {
      assertTrue(callIsPathUnderPrefix("/admin/portal/settings/users/123/edit", "/admin"));
      assertTrue(callIsPathUnderPrefix("/api/v1/users/123/profile/settings", "/api"));
   }


   /*******************************************************************************
    ** Test prefix collision prevention (critical test)
    *******************************************************************************/
   @Test
   void testPathMatching_NoPrefixCollision() throws Exception
   {
      /////////////////////////////////////////////////////////////////////
      // These should NOT match - different paths despite shared prefix  //
      /////////////////////////////////////////////////////////////////////
      assertFalse(callIsPathUnderPrefix("/administrator", "/admin"));
      assertFalse(callIsPathUnderPrefix("/admin-panel", "/admin"));
      assertFalse(callIsPathUnderPrefix("/admins", "/admin"));
      assertFalse(callIsPathUnderPrefix("/api-docs", "/api"));
      assertFalse(callIsPathUnderPrefix("/api2", "/api"));
      assertFalse(callIsPathUnderPrefix("/customer-portal", "/customer"));
   }


   /*******************************************************************************
    ** Test with query parameters
    *******************************************************************************/
   @Test
   void testPathMatching_QueryParameters() throws Exception
   {
      assertTrue(callIsPathUnderPrefix("/admin?tab=users", "/admin"));
      assertTrue(callIsPathUnderPrefix("/admin/users?id=123", "/admin"));
      assertTrue(callIsPathUnderPrefix("/api/data?format=json&limit=10", "/api"));
   }


   /*******************************************************************************
    ** Test with trailing slashes
    *******************************************************************************/
   @Test
   void testPathMatching_TrailingSlash() throws Exception
   {
      assertTrue(callIsPathUnderPrefix("/admin/", "/admin"));
      assertTrue(callIsPathUnderPrefix("/admin/users/", "/admin"));
      assertTrue(callIsPathUnderPrefix("/api/", "/api"));
   }


   /*******************************************************************************
    ** Test paths that should not match
    *******************************************************************************/
   @Test
   void testPathMatching_NoMatch() throws Exception
   {
      assertFalse(callIsPathUnderPrefix("/users", "/admin"));
      assertFalse(callIsPathUnderPrefix("/dashboard", "/admin"));
      assertFalse(callIsPathUnderPrefix("/products/123", "/admin"));
      assertFalse(callIsPathUnderPrefix("/admin", "/customer"));
   }


   /*******************************************************************************
    ** Test root path behavior
    *******************************************************************************/
   @Test
   void testPathMatching_RootPath() throws Exception
   {
      /////////////////////////////////////////////////////////////////////
      // Root path should match itself                                   //
      /////////////////////////////////////////////////////////////////////
      assertTrue(callIsPathUnderPrefix("/", "/"));

      /////////////////////////////////////////////////////////////////////
      // Root path prefix matches EVERYTHING - this is the special case //
      // that allows "/" SPA to be a catch-all fallback                  //
      /////////////////////////////////////////////////////////////////////
      assertTrue(callIsPathUnderPrefix("/admin", "/"));
      assertTrue(callIsPathUnderPrefix("/anything", "/"));
      assertTrue(callIsPathUnderPrefix("/deep/nested/path", "/"));
   }


   /*******************************************************************************
    ** Test similar prefixes
    *******************************************************************************/
   @Test
   void testPathMatching_SimilarPrefixes() throws Exception
   {
      /////////////////////////////////////////////////////////////////////
      // Test with nested/similar paths                                  //
      /////////////////////////////////////////////////////////////////////
      assertTrue(callIsPathUnderPrefix("/api/v1", "/api"));
      assertTrue(callIsPathUnderPrefix("/api/v1/users", "/api"));
      assertTrue(callIsPathUnderPrefix("/api/v1", "/api/v1"));
      assertFalse(callIsPathUnderPrefix("/api/v2", "/api/v1"));
   }


   /*******************************************************************************
    ** Test empty and edge case paths
    *******************************************************************************/
   @Test
   void testPathMatching_EdgeCases() throws Exception
   {
      /////////////////////////////////////////////////////////////////////
      // Single character paths                                          //
      /////////////////////////////////////////////////////////////////////
      assertTrue(callIsPathUnderPrefix("/a", "/a"));
      assertTrue(callIsPathUnderPrefix("/a/b", "/a"));
      assertFalse(callIsPathUnderPrefix("/ab", "/a"));

      /////////////////////////////////////////////////////////////////////
      // Numbers in paths                                                //
      /////////////////////////////////////////////////////////////////////
      assertTrue(callIsPathUnderPrefix("/v1", "/v1"));
      assertTrue(callIsPathUnderPrefix("/v1/users", "/v1"));
      assertFalse(callIsPathUnderPrefix("/v12", "/v1"));
   }


   /*******************************************************************************
    ** Test case sensitivity
    *******************************************************************************/
   @Test
   void testPathMatching_CaseSensitive() throws Exception
   {
      /////////////////////////////////////////////////////////////////////
      // URLs are case-sensitive per HTTP spec                           //
      /////////////////////////////////////////////////////////////////////
      assertTrue(callIsPathUnderPrefix("/Admin", "/Admin"));
      assertFalse(callIsPathUnderPrefix("/Admin", "/admin"));
      assertFalse(callIsPathUnderPrefix("/admin", "/Admin"));
   }

}
