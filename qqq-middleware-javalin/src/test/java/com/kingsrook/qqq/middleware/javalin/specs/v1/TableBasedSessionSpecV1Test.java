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


import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.TableBasedAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.TableBasedAuthenticationModule;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** TABLE_BASED sessions over v1 (QRun-IO/qqq#700): manageSession takes the
 ** username and password as an `Authorization: Basic` header, its sessionUUID
 ** cookie authenticates secured routes, and logout deletes the session row.
 *******************************************************************************/
class TableBasedSessionSpecV1Test extends SpecTestBase
{
   private static final String USERNAME  = "tess.table";
   private static final String PASSWORD  = "table:pass-2026";
   private static final String FULL_NAME = "Tess Table";



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
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new TableCountSpecV1(), new LogoutSpecV1()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    ** the standard user and session tables (authentication is switched to
    ** TABLE_BASED once they are primed)
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance                        qInstance      = TestUtils.defineInstance();
      TableBasedAuthenticationMetaData authentication = new TableBasedAuthenticationMetaData();
      qInstance.addTable(authentication.defineStandardUserTable(TestUtils.BACKEND_NAME_MEMORY));
      qInstance.addTable(authentication.defineStandardSessionTable(TestUtils.BACKEND_NAME_MEMORY));
      return (qInstance);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected void primeTestData(QInstance qInstance) throws Exception
   {
      super.primeTestData(qInstance);
      QContext.init(qInstance, new QSession());
      TestUtils.insertRecords(qInstance, qInstance.getTable("user"), List.of(new QRecord()
         .withValue("username", USERNAME)
         .withValue("fullName", FULL_NAME)
         .withValue("passwordHash", TableBasedAuthenticationModule.PasswordHasher.createHashedPassword(PASSWORD))));

      ///////////////////////////////////////////////////////////////////////
      // table-based authentication from here on (the data above is primed //
      // under the test instance's default authentication)                //
      ///////////////////////////////////////////////////////////////////////
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new TableBasedAuthenticationMetaData());
   }



   /***************************************************************************
    ** cookies are sent explicitly, so no test sees another test's session
    ***************************************************************************/
   @BeforeEach
   void disableCookieManagement()
   {
      Unirest.config().reset().enableCookieManagement(false);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterAll
   static void resetUnirest()
   {
      Unirest.config().reset();
   }



   /*******************************************************************************
    ** Sign in, use the session on a secured route, log out, and replay the ended
    ** session.
    *******************************************************************************/
   @Test
   void testSignInUseAndLogout() throws QException
   {
      HttpResponse<String> signIn = Unirest.post(getBaseUrlAndPath() + "/manageSession")
         .header("Authorization", "Basic " + basic(USERNAME, PASSWORD))
         .header("Content-Type", "application/json")
         .body("{}")
         .asString();
      assertEquals(200, signIn.getStatus());
      JSONObject body = JsonUtils.toJSONObject(signIn.getBody());
      String     uuid = body.getString("uuid");
      assertEquals(FULL_NAME, body.getJSONObject("values").getJSONObject("user").getString("name"));
      assertEquals(USERNAME, body.getJSONObject("values").getJSONObject("user").getString("username"));
      assertFalse(signIn.getBody().contains("passwordHash"));
      assertEquals(uuid, signIn.getCookies().getNamed("sessionUUID").getValue());
      assertEquals(1, sessionCount());

      HttpResponse<String> count = Unirest.post(getBaseUrlAndPath() + "/table/person/count")
         .header("Cookie", "sessionUUID=" + uuid)
         .asString();
      assertEquals(200, count.getStatus());

      HttpResponse<String> logout = Unirest.post(getBaseUrlAndPath() + "/logout")
         .header("Cookie", "sessionUUID=" + uuid)
         .asString();
      assertEquals(200, logout.getStatus());
      assertEquals(0, sessionCount());

      for(String cookie : List.of("sessionUUID=" + uuid, "sessionId=" + uuid))
      {
         HttpResponse<String> replay = Unirest.post(getBaseUrlAndPath() + "/table/person/count")
            .header("Cookie", cookie)
            .asString();
         assertEquals(401, replay.getStatus());
      }
   }



   /*******************************************************************************
    ** Wrong, malformed and missing credentials are refused with a 401 and create
    ** no session.
    *******************************************************************************/
   @Test
   void testRefusedCredentials() throws QException
   {
      for(String authorization : List.of("Basic " + basic(USERNAME, "table"), "Basic " + basic("nobody", PASSWORD), "Basic " + Base64.getEncoder().encodeToString("no-colon".getBytes(StandardCharsets.UTF_8))))
      {
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/manageSession")
            .header("Authorization", authorization)
            .header("Content-Type", "application/json")
            .body("{}")
            .asString();
         assertEquals(401, response.getStatus());
         assertEquals("Incorrect username or password.", JsonUtils.toJSONObject(response.getBody()).getString("error"));
         assertThat(response.getHeaders().get("Set-Cookie")).noneMatch(cookie -> cookie.startsWith("sessionUUID=") && !cookie.startsWith("sessionUUID=;"));
      }

      HttpResponse<String> noCredentials = Unirest.post(getBaseUrlAndPath() + "/manageSession")
         .header("Content-Type", "application/json")
         .body("{}")
         .asString();
      assertEquals(401, noCredentials.getStatus());
      assertEquals(0, sessionCount());
   }



   /***************************************************************************
    ** rows in the session table
    ***************************************************************************/
   private int sessionCount() throws QException
   {
      QContext.init(serverQInstance, new QSystemUserSession());
      return (new CountAction().execute(new CountInput("session")).getCount());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static String basic(String username, String password)
   {
      return (Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
   }
}
