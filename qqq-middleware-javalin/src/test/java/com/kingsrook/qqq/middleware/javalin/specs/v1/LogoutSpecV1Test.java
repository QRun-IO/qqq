/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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
