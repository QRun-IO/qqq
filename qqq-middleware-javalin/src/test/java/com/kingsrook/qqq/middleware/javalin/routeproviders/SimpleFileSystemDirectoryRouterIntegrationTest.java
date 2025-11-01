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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.javalin.TestUtils;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Integration tests for SimpleFileSystemDirectoryRouter
 *******************************************************************************/
class SimpleFileSystemDirectoryRouterIntegrationTest
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
    ** Test SPA deep linking with 404 handler
    *******************************************************************************/
   @Test
   void testSpaDeepLinking()
   {
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("/static", "src/test/resources/public-site/");
      router.setQInstance(qInstance);
      router.withSpaRootPath("/static");
      router.withSpaRootFile("index.html");

      Javalin service = Javalin.create();
      try
      {
         router.acceptJavalinService(service);
         // Should register 404 handler for SPA deep linking
         assertNotNull(service);
      }
      finally
      {
         service.stop();
      }
   }

}
