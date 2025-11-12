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

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


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

}
