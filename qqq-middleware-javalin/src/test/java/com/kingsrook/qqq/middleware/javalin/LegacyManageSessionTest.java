/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.TableBasedAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.TableBasedAuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.v1.LogoutSpecV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.ManageSessionSpecV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import io.javalin.Javalin;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static io.javalin.apibuilder.ApiBuilder.path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Exercise the real legacy and v1 routes with owned TABLE_BASED sessions (#733).
 *******************************************************************************/
class LegacyManageSessionTest
{
   private Javalin service;
   private QInstance instance;
   private QInstance previousInstance;
   private boolean previousHttpOnly;
   private String baseUrl;



   /***************************************************************************
    ** Register both endpoint families; SpecTestBase only registers v1 routes.
    ***************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousInstance = QJavalinImplementation.getQInstance();
      previousHttpOnly = QJavalinImplementation.getSessionCookieHttpOnly();
      Unirest.config().reset().enableCookieManagement(false);
      MemoryRecordStore.fullReset();
      TestUtils.primeTestDatabase();
      instance = TestUtils.defineInstance();
      TableBasedAuthenticationMetaData authentication = new TableBasedAuthenticationMetaData();
      instance.addTable(authentication.defineStandardUserTable(TestUtils.BACKEND_NAME_MEMORY));
      instance.addTable(authentication.defineStandardSessionTable(TestUtils.BACKEND_NAME_MEMORY));
      QContext.init(instance, new QSystemUserSession());
      String passwordHash = TableBasedAuthenticationModule.PasswordHasher.createHashedPassword("fixture-password");
      TestUtils.insertRecords(instance, instance.getTable("user"), List.of(
         new QRecord().withValue("id", 1).withValue("username", "first").withValue("fullName", "First User").withValue("passwordHash", passwordHash),
         new QRecord().withValue("id", 2).withValue("username", "second").withValue("fullName", "Second User").withValue("passwordHash", passwordHash)));
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), authentication);
      QJavalinImplementation implementation = new QJavalinImplementation(instance);
      MiddlewareVersionV1 version = new MiddlewareVersionV1();
      service = Javalin.create(config ->
      {
         config.routes.apiBuilder(() -> path("/qqq", implementation.getRoutes()));
         for(AbstractEndpointSpec<?, ?, ?> spec : List.of(new ManageSessionSpecV1(), new LogoutSpecV1()))
         {
            spec.setQInstance(instance);
            config.routes.apiBuilder(() -> spec.defineRoute(version, version.getVersionBasePath()));
         }
         config.routes.after(QJavalinImplementation::clearQContext);
      }).start(0);
      baseUrl = "http://localhost:" + service.port() + "/qqq";
   }



   /***************************************************************************
    ** Restore static middleware settings even after an assertion fails.
    ***************************************************************************/
   @AfterEach
   void tearDown()
   {
      try
      {
         if(service != null)
         {
            service.stop();
         }
      }
      finally
      {
         QJavalinImplementation.setSessionCookieHttpOnly(previousHttpOnly);
         QJavalinImplementation.setQInstance(previousInstance);
         Unirest.config().reset();
         MemoryRecordStore.fullReset();
         QContext.clear();
      }
   }



   /***************************************************************************
    ** Cookie resume preserves identity and never returns the bearer in JSON.
    ***************************************************************************/
   @Test
   void emptyBodyResumesCookieReadable() throws Exception
   {
      emptyBodyResumesCookie(false);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   @Test
   void emptyBodyResumesCookieHttpOnly() throws Exception
   {
      emptyBodyResumesCookie(true);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   private void emptyBodyResumesCookie(boolean httpOnly) throws Exception
   {
      QJavalinImplementation.setSessionCookieHttpOnly(httpOnly);
      String uuid = signIn("first");
      HttpResponse<String> response = legacy("{}", uuid, null);
      assertResumed(response, uuid, httpOnly);
      assertEquals(1, sessionCount());
   }



   /***************************************************************************
    ** The existing storage control is not an explicit credential.
    ***************************************************************************/
   @Test
   void storageControlStillResumesCookieReadable() throws Exception
   {
      storageControlStillResumesCookie(false);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   @Test
   void storageControlStillResumesCookieHttpOnly() throws Exception
   {
      storageControlStillResumesCookie(true);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   private void storageControlStillResumesCookie(boolean httpOnly) throws Exception
   {
      QJavalinImplementation.setSessionCookieHttpOnly(httpOnly);
      String uuid = signIn("first");
      for(boolean store : List.of(false, true))
      {
         assertResumed(legacy(new JSONObject().put("doStoreUserSession", store).toString(), uuid, null), uuid, httpOnly);
      }
      assertEquals(1, sessionCount());
   }



   /***************************************************************************
    ** Explicit legacy tokens win over a stale cookie, with readable-mode parity.
    ***************************************************************************/
   @Test
   void explicitSessionWinsReadable()
   {
      explicitSessionWins(false);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   @Test
   void explicitSessionWinsHttpOnly()
   {
      explicitSessionWins(true);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   private void explicitSessionWins(boolean httpOnly)
   {
      QJavalinImplementation.setSessionCookieHttpOnly(httpOnly);
      String uuid = signIn("first");
      HttpResponse<String> response = legacy(new JSONObject().put("sessionUUID", uuid).toString(), "invalid-session", null);
      assertEquals(200, response.getStatus());
      assertEquals(!httpOnly, JsonUtils.toJSONObject(response.getBody()).has("uuid"));
      assertTrue(uuid.equals(response.getCookies().getNamed("sessionUUID").getValue()));
      assertEquals("First User", JsonUtils.toJSONObject(response.getBody()).getJSONObject("values").getJSONObject("user").getString("name"));
   }



   /***************************************************************************
    ** A fresh legacy sign-in keeps its response contract in readable-cookie mode.
    ***************************************************************************/
   @Test
   void explicitCredentialsWinReadable() throws Exception
   {
      explicitCredentialsWin(false);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   @Test
   void explicitCredentialsWinHttpOnly() throws Exception
   {
      explicitCredentialsWin(true);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   private void explicitCredentialsWin(boolean httpOnly) throws Exception
   {
      QJavalinImplementation.setSessionCookieHttpOnly(httpOnly);
      String earlier = signIn("first");
      HttpResponse<String> response = legacy(new JSONObject().put("basicAuthString", basic("second")).toString(), earlier, null);
      assertEquals(200, response.getStatus());
      JSONObject body = JsonUtils.toJSONObject(response.getBody());
      assertEquals("Second User", body.getJSONObject("values").getJSONObject("user").getString("name"));
      assertEquals(!httpOnly, body.has("uuid"));
      assertFalse(earlier.equals(response.getCookies().getNamed("sessionUUID").getValue()));
      assertEquals(2, sessionCount());
   }



   /***************************************************************************
    ** Never silently authenticate with a cookie when explicit input was supplied.
    ***************************************************************************/
   @Test
   void explicitInputPreventsCookieFallback() throws Exception
   {
      String uuid = signIn("first");
      for(String key : List.of("accessToken", "apiKey", "code", "state", "codeVerifier", "redirectUri", "sessionUUID", "sessionId", "uuid", "customCredential"))
      {
         assertRefused(legacy(new JSONObject().put(key, "invalid").toString(), uuid, null));
      }
      assertEquals(1, sessionCount());
   }



   /***************************************************************************
    ** Legacy does not parse Authorization; its presence must not enable resume.
    ***************************************************************************/
   @Test
   void authorizationPreventsCookieFallback()
   {
      String uuid = signIn("first");
      for(String authorization : List.of("Basic invalid", "Bearer invalid", ""))
      {
         assertRefused(legacy("{}", uuid, authorization));
      }
   }



   /***************************************************************************
    ** Missing and forged cookies cannot create an authenticated session.
    ***************************************************************************/
   @Test
   void missingAndForgedCookiesAreRefusedReadable() throws Exception
   {
      missingAndForgedCookiesAreRefused(false);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   @Test
   void missingAndForgedCookiesAreRefusedHttpOnly() throws Exception
   {
      missingAndForgedCookiesAreRefused(true);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   private void missingAndForgedCookiesAreRefused(boolean httpOnly) throws Exception
   {
      QJavalinImplementation.setSessionCookieHttpOnly(httpOnly);
      assertRefused(legacy("{}", null, null));
      assertRefused(legacy("{}", "invalid-session", null));
      assertEquals(0, sessionCount());
   }



   /***************************************************************************
    ** A logged-out bearer remains invalid on the legacy endpoint.
    ***************************************************************************/
   @Test
   void loggedOutCookieIsRefusedReadable() throws Exception
   {
      loggedOutCookieIsRefused(false);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   @Test
   void loggedOutCookieIsRefusedHttpOnly() throws Exception
   {
      loggedOutCookieIsRefused(true);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   private void loggedOutCookieIsRefused(boolean httpOnly) throws Exception
   {
      QJavalinImplementation.setSessionCookieHttpOnly(httpOnly);
      String uuid = signIn("first");
      assertEquals(200, Unirest.post(baseUrl + "/v1/logout").header("Cookie", "sessionUUID=" + uuid).asString().getStatus());
      assertRefused(legacy("{}", uuid, null));
      assertEquals(0, sessionCount());
   }



   /***************************************************************************
    ** Exercise actual expiry without sleeps or a remembered validation entry.
    ***************************************************************************/
   @Test
   void expiredCookieIsRefusedReadable() throws Exception
   {
      expiredCookieIsRefused(false);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   @Test
   void expiredCookieIsRefusedHttpOnly() throws Exception
   {
      expiredCookieIsRefused(true);
   }



   /***************************************************************************
    ** Exercise the matching cookie mode through the same endpoint contract.
    ***************************************************************************/
   private void expiredCookieIsRefused(boolean httpOnly) throws Exception
   {
      QJavalinImplementation.setSessionCookieHttpOnly(httpOnly);
      String uuid = UUID.randomUUID().toString();
      QContext.init(instance, new QSystemUserSession());
      TestUtils.insertRecords(instance, instance.getTable("session"), List.of(new QRecord()
         .withValue("uuid", uuid).withValue("userId", 1).withValue("accessTimestamp", Instant.now().minusSeconds(86400))));
      assertRefused(legacy("{}", uuid, null));
   }



   /***************************************************************************
    ** Bootstrap through the existing v1 credential path, never a mock auth context.
    ***************************************************************************/
   private String signIn(String username)
   {
      HttpResponse<String> response = Unirest.post(baseUrl + "/v1/manageSession")
         .header("Content-Type", "application/json").header("Authorization", "Basic " + basic(username)).body("{}").asString();
      assertEquals(200, response.getStatus());
      return response.getCookies().getNamed("sessionUUID").getValue();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private String basic(String username)
   {
      return Base64.getEncoder().encodeToString((username + ":fixture-password").getBytes(StandardCharsets.UTF_8));
   }



   /***************************************************************************
    ** Send cookies explicitly so the HTTP client cannot leak one between tests.
    ***************************************************************************/
   private HttpResponse<String> legacy(String body, String uuid, String authorization)
   {
      HttpRequestWithBody request = Unirest.post(baseUrl + "/manageSession").header("Content-Type", "application/json");
      if(uuid != null)
      {
         request.header("Cookie", "sessionUUID=" + uuid);
      }
      if(authorization != null)
      {
         request.header("Authorization", authorization);
      }
      return request.body(body).asString();
   }



   /***************************************************************************
    ** Compare identity only in memory; do not include bearer values in failures.
    ***************************************************************************/
   private void assertResumed(HttpResponse<String> response, String uuid, boolean httpOnly)
   {
      assertEquals(200, response.getStatus());
      JSONObject body = JsonUtils.toJSONObject(response.getBody());
      assertEquals("First User", body.getJSONObject("values").getJSONObject("user").getString("name"));
      assertFalse(body.has("uuid"));
      assertFalse(response.getBody().contains(uuid));
      assertTrue(uuid.equals(response.getCookies().getNamed("sessionUUID").getValue()));
      assertEquals(httpOnly, response.getHeaders().get("Set-Cookie").stream().anyMatch(value -> value.startsWith("sessionUUID=") && value.contains("; HttpOnly")));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void assertRefused(HttpResponse<String> response)
   {
      assertEquals(401, response.getStatus());
      assertFalse(JsonUtils.toJSONObject(response.getBody()).has("uuid"));
      assertFalse(response.getHeaders().get("Set-Cookie").stream().anyMatch(value -> value.startsWith("sessionUUID=") && !value.startsWith("sessionUUID=;")));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private int sessionCount() throws Exception
   {
      QContext.init(instance, new QSystemUserSession());
      return new CountAction().execute(new CountInput("session")).getCount();
   }
}
