/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qqq.middleware.javalin.specs;


import java.net.ServerSocket;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.middleware.javalin.specs.v1.VersionSpecV1;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** The HTTP fixture must reserve its own port instead of guessing a free one.
 *******************************************************************************/
class SpecTestBaseTest
{
   /*******************************************************************************
    ** Keep the previously guessed next port occupied while starting a real spec.
    *******************************************************************************/
   @Test
   void testOccupiedNextPortDoesNotPreventFixtureStartup() throws Exception
   {
      SpecTestBase fixture = new SpecTestBase()
      {
         /*******************************************************************************
          ** Use a public endpoint to verify requests reach the actual bound port.
          *******************************************************************************/
         @Override
         protected AbstractEndpointSpec<?, ?, ?> getSpec()
         {
            return new VersionSpecV1();
         }



         /*******************************************************************************
          ** Serve the versioned UI API.
          *******************************************************************************/
         @Override
         protected String getVersion()
         {
            return "v1";
         }
      };
      int previousPort = SpecTestBase.PORT;
      try(ServerSocket occupiedPort = new ServerSocket(0))
      {
         SpecTestBase.PORT = occupiedPort.getLocalPort() - 1;
         fixture.beforeAndAfterEach();
         fixture.beforeEach();
         assertTrue(SpecTestBase.PORT > 0);
         assertNotEquals(occupiedPort.getLocalPort(), SpecTestBase.PORT);
         assertEquals(SpecTestBase.service.port(), SpecTestBase.PORT);
         assertEquals(200, Unirest.get(fixture.getBaseUrlAndPath() + "/version").asString().getStatus());
      }
      finally
      {
         SpecTestBase.afterAll();
         fixture.beforeAndAfterEach();
         SpecTestBase.PORT = previousPort;
         QContext.clear();
      }
   }
}
