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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for LogoutSpecV1
 *******************************************************************************/
class LogoutSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new LogoutSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /*******************************************************************************
    ** Logout expires every session cookie the server issues, so a stale sessionId
    ** cannot shadow the next sign-in's sessionUUID.
    *******************************************************************************/
   @Test
   void testLogoutExpiresSessionCookies()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/logout")
         .header("Cookie", QJavalinImplementation.SESSION_UUID_COOKIE_NAME + "=abc; " + QJavalinImplementation.SESSION_ID_COOKIE_NAME + "=abc")
         .asString();

      assertEquals(200, response.getStatus());
      List<String> setCookies = response.getHeaders().get("Set-Cookie");
      for(String name : List.of(QJavalinImplementation.SESSION_UUID_COOKIE_NAME, QJavalinImplementation.SESSION_ID_COOKIE_NAME))
      {
         assertTrue(setCookies.stream().anyMatch(cookie -> cookie.startsWith(name + "=;") && (cookie.contains("Max-Age=0") || cookie.contains("Expires=Thu, 01 Jan 1970"))),
            () -> "expected " + name + " to be expired, got " + setCookies);
      }
   }
}
