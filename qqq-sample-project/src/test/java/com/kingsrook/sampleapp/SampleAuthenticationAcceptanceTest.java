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

package com.kingsrook.sampleapp;


import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.Auth0AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.TableBasedAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreProviderInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.TableBasedAuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.h2.tools.RunScript;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Authentication lifecycle against the real sample HTTP routes and owned data.
 *******************************************************************************/
class SampleAuthenticationAcceptanceTest
{
   private CapturedContext previousContext;
   private Map<String, Serializable> previousObjects;
   private QInstance instance;
   private Connection anchor;
   private QApplicationJavalinServer server;
   private HttpClient client;
   private URI base;
   private SampleAuthenticationProvider provider;
   private QSessionStoreProviderInterface previousStore;
   private final String username = "owned-" + UUID.randomUUID();



   /*******************************************************************************
    ** Canonical sample records live in an isolated database for every test.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousContext = QContext.capture();
      previousStore = QSessionStoreRegistry.getInstance().getProvider().orElse(null);
      QSessionStoreRegistry.getInstance().clear();
      previousObjects = QContext.getObjects();
      QContext.setObjects(new LinkedHashMap<>());
      ConnectionManager.resetConnectionProviders();
      MemoryRecordStore.getInstance().reset();
      String jdbcUrl = "jdbc:h2:mem:auth_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=0";
      anchor = DriverManager.getConnection(jdbcUrl, "sa", "");
      try(InputStreamReader reader = new InputStreamReader(SampleMetaDataProvider.class.getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(anchor, reader);
      }
      instance = SampleMetaDataProvider.defineTestInstance();
      ((RDBMSBackendMetaData) instance.getBackend(SampleMetaDataProvider.RDBMS_BACKEND_NAME)).setJdbcUrl(jdbcUrl);
      instance.addBackend(new QBackendMetaData().withName("authMemory").withBackendType(MemoryBackendModule.class));
      client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
      OAuth2AuthenticationModule.clearOIDCProviderMetadataCache();
   }



   /*******************************************************************************
    ** Close owned transports before restoring the caller's native context.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(client != null)
         {
            client.close();
         }
         if(server != null)
         {
            server.stop();
         }
         if(provider != null)
         {
            provider.close();
         }
         if(anchor != null)
         {
            anchor.close();
         }
      }
      finally
      {
         ConnectionManager.resetConnectionProviders();
         QSessionStoreRegistry.getInstance().clear();
         if(previousStore != null)
         {
            QSessionStoreRegistry.getInstance().register(previousStore);
         }
         MemoryRecordStore.getInstance().reset();
         QContext.clear();
         QContext.init(previousContext);
         QContext.setObjects(previousObjects);
      }
   }



   /*******************************************************************************
    ** Real credentials issue cookies, reload resumes, and logout rejects reuse.
    *******************************************************************************/
   @Test
   void testTableCredentialsResumeAndLogout() throws Exception
   {
      configureTableAuthentication();
      start();
      assertEquals(401, request("GET", "/metaData/table/person", null, null).statusCode());
      assertEquals(401, request("POST", "/manageSession", "{}", null).statusCode());
      for(String credentials : List.of(username + ":wrong", "unknown:wrong", "not-a-pair"))
      {
         assertEquals(401, request("POST", "/manageSession", "{}", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8))).statusCode());
      }
      HttpResponse<String> login = request("POST", "/manageSession", "{}", basic());
      String uuid = sessionCookie(login);
      assertFalse(login.body().contains("passwordHash"));
      assertTrue(login.headers().allValues("Set-Cookie").stream().anyMatch(cookie -> cookie.startsWith("sessionUUID=")));
      assertEquals("person", success(request("GET", "/metaData/table/person", null, uuid)).getString("name"));
      assertEquals("Avery", success(request("GET", "/table/person/1", null, uuid)).getJSONObject("record").getJSONObject("values").getString("firstName"));
      JSONObject resumed = success(request("POST", "/manageSession", "{}", uuid));
      assertFalse(resumed.has("uuid"), "Cookie resumption must not disclose the HttpOnly identifier in JSON");
      assertEquals(username, resumed.getJSONObject("values").getJSONObject("user").getString("username"));
      HttpResponse<String> logout = request("POST", "/logout", "{}", uuid);
      success(logout);
      assertTrue(logout.headers().allValues("Set-Cookie").stream().flatMap(value -> HttpCookie.parse(value).stream()).anyMatch(cookie -> cookie.getName().equals("sessionUUID") && cookie.getMaxAge() == 0));
      assertEquals(401, request("POST", "/manageSession", "{}", uuid).statusCode());
      assertEquals(401, request("GET", "/metaData/table/person", null, uuid).statusCode());
      String replacement = sessionCookie(request("POST", "/manageSession", "{}", basic()));
      assertNotEquals(uuid, replacement);
      assertEquals(200, request("GET", "/metaData/table/person", null, replacement).statusCode());
   }



   /*******************************************************************************
    ** Unknown and already-expired owned session rows cannot access sample data.
    *******************************************************************************/
   @Test
   void testTableExpiredAndUnknownSessionDenied() throws Exception
   {
      configureTableAuthentication();
      QContext.init(instance, new QSystemUserSession());
      String uuid = UUID.randomUUID().toString();
      new InsertAction().execute(new InsertInput("session").withRecords(List.of(new QRecord().withValue("id", uuid)
         .withValue("userId", 1).withValue("accessTimestamp", Instant.now().minusSeconds(600)))));
      start();
      for(String session : List.of(uuid, UUID.randomUUID().toString()))
      {
         assertEquals(401, request("POST", "/manageSession", "{}", session).statusCode());
         assertEquals(401, request("GET", "/metaData/table/person", null, session).statusCode());
      }
      assertEquals(200, request("POST", "/manageSession", "{}", basic()).statusCode());
   }



   /*******************************************************************************
    ** PKCE grants are exchanged at the real protocol endpoint and cannot replay.
    *******************************************************************************/
   @Test
   void testOAuthCodeResumeAndNormalLogout() throws Exception
   {
      configureOAuthAuthentication();
      start();
      assertEquals(401, request("POST", "/manageSession", "{}", null).statusCode());
      assertEquals(401, request("GET", "/metaData/table/person", null, UUID.randomUUID().toString()).statusCode());
      String code = provider.code(username);
      String body = grant(code, SampleAuthenticationProvider.CALLBACK, SampleAuthenticationProvider.VERIFIER);
      String uuid = sessionCookie(request("POST", "/manageSession", body, null));
      assertEquals(200, request("GET", "/metaData/table/person", null, uuid).statusCode());
      assertFalse(success(request("POST", "/manageSession", "{}", uuid)).has("uuid"));
      assertEquals(401, request("POST", "/manageSession", body, null).statusCode());
      assertEquals(401, request("POST", "/manageSession", grant(provider.code(username), "http://wrong.example.invalid/", SampleAuthenticationProvider.VERIFIER), null).statusCode());
      assertEquals(401, request("POST", "/manageSession", grant(provider.code(username), SampleAuthenticationProvider.CALLBACK, "wrong-verifier"), null).statusCode());
      assertEquals(401, request("POST", "/manageSession", grant(provider.code("disabled-user", SampleAuthenticationProvider.VERIFIER, false), SampleAuthenticationProvider.CALLBACK, SampleAuthenticationProvider.VERIFIER), null).statusCode());
      success(request("POST", "/logout", "{}", uuid));
      assertEquals(401, request("GET", "/metaData/table/person", null, uuid).statusCode());
      assertEquals(401, request("POST", "/manageSession", "{}", uuid).statusCode());
   }



   /*******************************************************************************
    ** Traditional callbacks require stored state and a single-use provider code.
    *******************************************************************************/
   @Test
   void testOAuthCallbackStateAndCodeReplay() throws Exception
   {
      configureOAuthAuthentication();
      QContext.init(instance, new QSystemUserSession());
      String state = UUID.randomUUID().toString();
      new InsertAction().execute(new InsertInput(RedirectStateMetaDataProducer.TABLE_NAME).withRecords(List.of(new QRecord()
         .withValue("state", state).withValue("redirectUri", SampleAuthenticationProvider.CALLBACK).withValue("createDate", Instant.now()))));
      start();
      String code = provider.code(username, null, true);
      assertEquals(401, request("GET", "/metaData/table/person?code=" + code + "&state=unknown", null, null).statusCode());
      HttpResponse<String> callback = request("GET", "/metaData/table/person?code=" + code + "&state=" + state, null, null);
      assertEquals("person", success(callback).getString("name"));
      String uuid = callback.headers().allValues("Set-Cookie").stream().flatMap(value -> HttpCookie.parse(value).stream())
         .filter(cookie -> cookie.getName().equals("sessionId")).findFirst().orElseThrow().getValue();
      assertEquals("Avery", success(request("GET", "/table/person/1", null, uuid)).getJSONObject("record").getJSONObject("values").getString("firstName"));
      assertEquals(401, request("GET", "/metaData/table/person?code=" + code + "&state=" + state, null, null).statusCode());
      success(request("POST", "/logout", "{}", uuid));
      assertEquals(401, request("GET", "/table/person/1", null, uuid).statusCode());
   }



   /*******************************************************************************
    ** Expired tokens and mismatched persisted identities cannot resume a session.
    *******************************************************************************/
   @Test
   void testOAuthExpiredAndMismatchedStoredSession() throws Exception
   {
      configureOAuthAuthentication();
      QContext.init(instance, new QSystemUserSession());
      String expired = UUID.randomUUID().toString();
      String mismatch = UUID.randomUUID().toString();
      new InsertAction().execute(new InsertInput(UserSession.TABLE_NAME).withRecords(List.of(
         new QRecord().withValue("uuid", expired).withValue("userId", username)
            .withValue("accessToken", provider.accessToken(username, Instant.now().minusSeconds(300), false)),
         new QRecord().withValue("uuid", mismatch).withValue("userId", "another-user")
            .withValue("accessToken", provider.accessToken(username, Instant.now().plusSeconds(300), false)))));
      start();
      for(String uuid : List.of(expired, mismatch))
      {
         assertEquals(401, request("POST", "/manageSession", "{}", uuid).statusCode());
         assertEquals(401, request("GET", "/table/person/1", null, uuid).statusCode());
      }
      String fresh = sessionCookie(request("POST", "/manageSession", grant(provider.code(username), SampleAuthenticationProvider.CALLBACK, SampleAuthenticationProvider.VERIFIER), null));
      assertEquals(200, request("GET", "/table/person/1", null, fresh).statusCode());
   }



   /*******************************************************************************
    ** Signed back-channel logout must invalidate a session already used over HTTP.
    *******************************************************************************/
   @Test
   void testOAuthBackChannelInvalidatesUsedSessionAndRejectsReplay() throws Exception
   {
      assertBackChannelLogout(false, false);
   }



   /*******************************************************************************
    ** The optional native session store must also stop accepting a warmed session.
    *******************************************************************************/
   @Test
   void testOAuthBackChannelInvalidatesSessionStore() throws Exception
   {
      assertBackChannelLogout(true, false);
   }



   /*******************************************************************************
    ** A failed cache eviction preserves rows and allows the same event to retry.
    *******************************************************************************/
   @Test
   void testOAuthBackChannelCacheFailureIsRetryable() throws Exception
   {
      assertBackChannelLogout(true, true);
   }



   /*******************************************************************************
    ** Exercise the full native login, cached use and signed logout boundary.
    *******************************************************************************/
   private void assertBackChannelLogout(Boolean useStore, Boolean failRemoval) throws Exception
   {
      configureOAuthAuthentication();
      OwnedSessionStore store = new OwnedSessionStore();
      if(useStore)
      {
         QSessionStoreRegistry.getInstance().register(store);
         ((OAuth2AuthenticationMetaData) instance.getAuthentication()).setSessionStoreEnabled(true);
      }
      else
      {
         store.rejectRemoval = true;
         QSessionStoreRegistry.getInstance().register(store);
      }
      start();
      String uuid = sessionCookie(request("POST", "/manageSession", grant(provider.code(username), SampleAuthenticationProvider.CALLBACK, SampleAuthenticationProvider.VERIFIER), null));
      String other = sessionCookie(request("POST", "/manageSession", grant(provider.code("another-user"), SampleAuthenticationProvider.CALLBACK, SampleAuthenticationProvider.VERIFIER), null));
      assertEquals(200, request("GET", "/metaData/table/person", null, uuid).statusCode());
      assertEquals(400, logoutEvent(provider.logoutToken(username, true)).statusCode());
      assertEquals(400, logoutEvent("malformed-token").statusCode());
      assertEquals(200, request("GET", "/metaData/table/person", null, uuid).statusCode());
      String token = provider.logoutToken(username, false);
      if(useStore)
      {
         assertTrue(store.sessions.containsKey(uuid));
      }
      if(failRemoval)
      {
         store.rejectRemoval = true;
         assertEquals(500, logoutEvent(token).statusCode());
         QContext.init(instance, new QSystemUserSession());
         assertNotNull(new GetAction().executeForRecord(new GetInput(UserSession.TABLE_NAME).withUniqueKey(Map.of("uuid", uuid))));
         assertEquals(200, request("GET", "/table/person/1", null, uuid).statusCode());
         store.rejectRemoval = false;
      }
      success(logoutEvent(token));
      assertFalse(store.sessions.containsKey(uuid));
      assertEquals(401, request("GET", "/metaData/table/person", null, uuid).statusCode());
      assertEquals(200, request("GET", "/metaData/table/person", null, other).statusCode());
      assertEquals(400, logoutEvent(token).statusCode());
   }



   /*******************************************************************************
    ** Auth0 validates owned JWKS signatures and expiration before issuing sessions.
    *******************************************************************************/
   @Test
   void testAuth0SignedTokenAndRejectedTokens() throws Exception
   {
      provider = new SampleAuthenticationProvider();
      String auth0Issuer = provider.issuer() + "/";
      Auth0AuthenticationMetaData auth = new Auth0AuthenticationMetaData();
      auth.setName("owned-auth0");
      auth.setBaseUrl(provider.issuer());
      auth.setClientId(SampleAuthenticationProvider.CLIENT);
      auth.setClientSecret("synthetic-client-secret");
      auth.setAudience(SampleAuthenticationProvider.CLIENT);
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), auth);
      start();
      for(String token : List.of("bad-token",
         provider.accessToken(username, Instant.now().plusSeconds(300), true, auth0Issuer),
         provider.accessToken(username, Instant.now().minusSeconds(300), false, auth0Issuer),
         provider.accessToken(username, Instant.now().plusSeconds(300), false)))
      {
         assertEquals(401, request("POST", "/manageSession", new JSONObject().put("accessToken", token).toString(), null).statusCode());
      }
      String uuid = sessionCookie(request("POST", "/manageSession", new JSONObject().put("accessToken", provider.accessToken(username, Instant.now().plusSeconds(300), false, auth0Issuer)).toString(), null));
      assertEquals(200, request("GET", "/metaData/table/person", null, uuid).statusCode());
      success(request("POST", "/logout", "{}", uuid));
      assertEquals(401, request("GET", "/metaData/table/person", null, uuid).statusCode());
   }



   /*******************************************************************************
    ** Anonymous and development modes retain their explicitly configured behavior.
    *******************************************************************************/
   @Test
   void testAnonymousAndMockModes() throws Exception
   {
      for(QAuthenticationType type : List.of(QAuthenticationType.FULLY_ANONYMOUS, QAuthenticationType.MOCK))
      {
         instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withName("owned-" + type).withType(type));
         start();
         assertEquals(200, request("GET", "/metaData/table/person", null, null).statusCode());
         success(request("POST", "/manageSession", "{}", null));
         if(type == QAuthenticationType.MOCK)
         {
            assertEquals(401, request("GET", "/metaData/table/person", null, "Bearer Deny").statusCode());
         }
         server.stop();
         server = null;
      }
   }



   /*******************************************************************************
    ** Use the framework's standard table definitions and password hasher.
    *******************************************************************************/
   private void configureTableAuthentication() throws Exception
   {
      TableBasedAuthenticationMetaData auth = new TableBasedAuthenticationMetaData().withInactivityTimeoutSeconds(60);
      auth.setName("owned-table-auth");
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), auth);
      instance.addTable(auth.defineStandardUserTable("authMemory"));
      instance.addTable(auth.defineStandardSessionTable("authMemory"));
      QContext.init(instance, new QSystemUserSession());
      new InsertAction().execute(new InsertInput("user").withRecords(List.of(new QRecord().withValue("id", 1)
         .withValue("username", username).withValue("fullName", "Owned sample user")
         .withValue("passwordHash", TableBasedAuthenticationModule.PasswordHasher.createHashedPassword("owned:password")))));
   }



   /*******************************************************************************
    ** Canonical sample tables own the OAuth session and callback state data.
    *******************************************************************************/
   private void configureOAuthAuthentication() throws Exception
   {
      provider = new SampleAuthenticationProvider();
      OAuth2AuthenticationMetaData auth = new OAuth2AuthenticationMetaData().withBaseUrl(provider.issuer())
         .withClientId(SampleAuthenticationProvider.CLIENT).withClientSecret("synthetic-client-secret").withScopes("openid")
         .withUserSessionTableName(UserSession.TABLE_NAME).withRedirectStateTableName(RedirectStateMetaDataProducer.TABLE_NAME);
      auth.setName("owned-oauth-" + UUID.randomUUID());
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), auth);
   }



   /*******************************************************************************
    ** Serve the canonical sample through native V1 middleware.
    *******************************************************************************/
   private void start() throws Exception
   {
      server = new QApplicationJavalinServer(new SampleMetaDataProvider()
      {
         /*******************************************************************************
          ** Use only this test's owned metadata and database.
          *******************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      });
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withServeFrontendMaterialDashboard(false).withServeFrontendNext(false).withServeLegacyUnversionedMiddlewareAPI(false);
      server.withJavalinConfigCustomizer(config -> config.jetty.host = "127.0.0.1");
      server.withJavalinConfigurationCustomizer(service::set);
      server.start();
      base = URI.create("http://127.0.0.1:" + service.get().port() + "/qqq/v1");
      HttpResponse<String> metadata = request("GET", "/metaData/authentication", null, null);
      assertEquals(instance.getAuthentication().getType().name(), success(metadata).getString("type"));
      assertFalse(metadata.body().contains("synthetic-client-secret"));
   }



   /*******************************************************************************
    ** Send credentials or explicit cookies without a browser cookie jar.
    *******************************************************************************/
   private HttpResponse<String> request(String method, String path, String body, String credential) throws Exception
   {
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(15));
      if(credential != null)
      {
         if(credential.startsWith("Basic ") || credential.startsWith("Bearer "))
         {
            request.header("Authorization", credential);
         }
         else
         {
            request.header("Cookie", "sessionUUID=" + credential);
         }
      }
      if(body != null)
      {
         request.header("Content-Type", "application/json");
      }
      return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    ** The back-channel protocol uses a form-encoded signed logout event.
    *******************************************************************************/
   private HttpResponse<String> logoutEvent(String token) throws Exception
   {
      return client.send(HttpRequest.newBuilder(URI.create(base + "/oidc/backchannel-logout")).timeout(Duration.ofSeconds(15))
         .header("Content-Type", "application/x-www-form-urlencoded")
         .POST(HttpRequest.BodyPublishers.ofString("logout_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8))).build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    ** Include the response in an assertion only for owned synthetic test data.
    *******************************************************************************/
   private JSONObject success(HttpResponse<String> response)
   {
      assertEquals(200, response.statusCode(), response.body());
      return new JSONObject(response.body());
   }



   /*******************************************************************************
    ** HttpOnly authentication returns the identifier only through the cookie.
    *******************************************************************************/
   private String sessionCookie(HttpResponse<String> response)
   {
      assertFalse(success(response).has("uuid"));
      String cookie = response.headers().allValues("Set-Cookie").stream().filter(value -> value.startsWith("sessionUUID=")).findFirst().orElseThrow();
      assertTrue(cookie.contains("HttpOnly"));
      return cookie.substring("sessionUUID=".length()).split(";", 2)[0];
   }



   /*******************************************************************************
    ** The colon in the password exercises the actual Basic credential parser.
    *******************************************************************************/
   private String basic()
   {
      return "Basic " + Base64.getEncoder().encodeToString((username + ":owned:password").getBytes(StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    ** Use the native SPA authorization-code request shape.
    *******************************************************************************/
   private String grant(String code, String redirectUri, String verifier)
   {
      return new JSONObject().put("code", code).put("redirectUri", redirectUri).put("codeVerifier", verifier).toString();
   }



   /*******************************************************************************
    ** An owned store exercises the existing provider SPI, including eviction failure.
    *******************************************************************************/
   private static class OwnedSessionStore implements QSessionStoreProviderInterface
   {
      private final Map<String, QSession> sessions = new ConcurrentHashMap<>();
      private Boolean rejectRemoval = false;



      /*******************************************************************************
       ** Keep actual sessions returned by the native authentication module.
       *******************************************************************************/
      @Override
      public void store(String uuid, QSession session, Duration ttl)
      {
         sessions.put(uuid, session);
      }



      /*******************************************************************************
       ** Tests use this cache only within a single short-lived scenario.
       *******************************************************************************/
      @Override
      public Optional<QSession> load(String uuid)
      {
         return Optional.ofNullable(sessions.get(uuid));
      }



      /*******************************************************************************
       ** Fail before eviction so retry behavior can be asserted with the same token.
       *******************************************************************************/
      @Override
      public void remove(String uuid)
      {
         if(rejectRemoval)
         {
            throw new IllegalStateException("Owned session-store eviction failure");
         }
         sessions.remove(uuid);
      }



      /*******************************************************************************
       ** No wall-clock expiration is simulated by this eviction fixture.
       *******************************************************************************/
      @Override
      public void touch(String uuid)
      {
      }



      /*******************************************************************************
       ** Supply the configured provider contract without external storage.
       *******************************************************************************/
      @Override
      public Duration getDefaultTtl()
      {
         return Duration.ofMinutes(5);
      }
   }
}
