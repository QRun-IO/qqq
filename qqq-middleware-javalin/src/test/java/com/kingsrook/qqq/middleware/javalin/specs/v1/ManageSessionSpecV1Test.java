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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ManageSessionV1 
 *******************************************************************************/
class ManageSessionSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ManageSessionSpecV1();
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
    **
    *******************************************************************************/
   @Test
   void test()
   {
      String body = """
         {
            "accessToken": "abcdefg"
         }
         """;

      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/manageSession")
         .header("Content-Type", "application/json")
         .body(body)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertTrue(jsonObject.has("uuid"));
      assertThat(response.getHeaders().get("Set-Cookie")).anyMatch(s -> s.contains("sessionUUID"));
   }



   /*******************************************************************************
    ** The session cookie is SameSite=Lax for the whole site, and Secure only when
    ** the request came over HTTPS (here: through a TLS-terminating proxy).
    *******************************************************************************/
   @Test
   void testSessionCookieAttributes()
   {
      String body = """
         {"accessToken": "abcdefg"}
         """;

      HttpResponse<String> plain = Unirest.post(getBaseUrlAndPath() + "/manageSession")
         .header("Content-Type", "application/json")
         .body(body)
         .asString();
      String plainCookie = sessionUuidCookie(plain);
      assertThat(plainCookie).contains("Path=/").containsIgnoringCase("SameSite=Lax").containsIgnoringCase("Max-Age=86400").doesNotContainIgnoringCase("Secure");

      HttpResponse<String> proxied = Unirest.post(getBaseUrlAndPath() + "/manageSession")
         .header("Content-Type", "application/json")
         .header("X-Forwarded-Proto", "https, http")
         .body(body)
         .asString();
      assertThat(sessionUuidCookie(proxied)).containsIgnoringCase("SameSite=Lax").containsIgnoringCase("; Secure");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String sessionUuidCookie(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus());
      return (response.getHeaders().get("Set-Cookie").stream().filter(s -> s.startsWith("sessionUUID=")).findFirst().orElseThrow());
   }

}