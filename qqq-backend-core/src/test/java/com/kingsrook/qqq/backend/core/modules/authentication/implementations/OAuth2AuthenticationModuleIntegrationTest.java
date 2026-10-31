/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Integration tests for OAuth2AuthenticationModule with WireMock
 *******************************************************************************/
class OAuth2AuthenticationModuleIntegrationTest extends BaseTest
{
   private static final String BACKEND_NAME = "memory";
   private static final String REDIRECT_STATE_TABLE = "oauthRedirectState";

   private WireMockServer wireMockServer;
   private QInstance      qInstance;



   /*******************************************************************************
    ** Exercise the final expiry check after a token expires during lookup. The
    ** clock boundary is simulated by successive expiry reads, not by sleeping.
    *******************************************************************************/
   @Test
   void expiredSessionDebugLogOmitsCredentials() throws Exception
   {
      String uuid = UUID.randomUUID().toString();
      String token = createTestJwt("log-user", "Log User", Map.of());
      insertUserSession(uuid, token, "log-user");
      DecodedJWT decoded = spy(JWT.decode(token));
      Instant future = Instant.now().plusSeconds(3600);
      Instant past = Instant.now().minusSeconds(60);
      QSession session = new QSession();
      session.setUuid(uuid);
      when(decoded.getExpiresAtAsInstant()).thenReturn(future, future, future, past);
      try(OAuthLogCapture capture = new OAuthLogCapture(); MockedStatic<JWT> jwt = mockStatic(JWT.class))
      {
         jwt.when(() -> JWT.decode(token)).thenReturn(decoded);
         assertFalse(new OAuth2AuthenticationModule().isSessionValid(qInstance, session));
         capture.assertEventWithoutSecrets("accessToken is expired", uuid, token);
         assertTrue(capture.appender.getEventList().stream().anyMatch(event -> event.getLevel() == Level.DEBUG
            && event.getMessage().getFormattedMessage().contains("accessToken is expired")));
      }
   }



   /*******************************************************************************
    ** An invalid callback retains a useful event without copying state or code.
    *******************************************************************************/
   @Test
   void missingCallbackStateLogOmitsCredentials()
   {
      String state = "state-canary-" + UUID.randomUUID();
      String code = "code-canary-" + UUID.randomUUID();
      try(OAuthLogCapture capture = new OAuthLogCapture())
      {
         QAuthenticationException exception = assertThrows(QAuthenticationException.class, () -> new OAuth2AuthenticationModule()
            .createSession(qInstance, Map.of("state", state, "code", code)));
         assertEquals("State not found", exception.getMessage(), () -> String.valueOf(exception.getCause()));
         capture.assertEventWithoutSecrets("OAuth callback state not found", state, code);
      }
   }



   /*******************************************************************************
    ** Successful logout keeps its DEBUG event and removes the stored session.
    *******************************************************************************/
   @Test
   void logoutDebugLogOmitsCredentials() throws Exception
   {
      String uuid = UUID.randomUUID().toString();
      String token = createTestJwt("log-user", "Log User", Map.of());
      insertUserSession(uuid, token, "log-user");
      try(OAuthLogCapture capture = new OAuthLogCapture())
      {
         OAuth2AuthenticationModule module = new OAuth2AuthenticationModule();
         module.logout(qInstance, uuid);
         capture.assertEventWithoutSecrets("Logged out session", uuid, token);
         assertThrows(QAuthenticationException.class, () -> module.createSession(qInstance, Map.of("sessionUUID", uuid)));
      }
   }



   /*******************************************************************************
    ** A provider can echo credentials in either error field. Neither its free
    ** text description nor an unknown error code may reach logs or the caller.
    *******************************************************************************/
   @Test
   void providerRejectionOmitsUntrustedErrorDetails() throws Exception
   {
      stubOidcDiscovery();
      String description = "provider-description-canary-" + UUID.randomUUID();
      String errorCode = "provider-code-canary-" + UUID.randomUUID();
      wireMockServer.stubFor(post(urlEqualTo("/oauth/token"))
         .willReturn(aResponse().withStatus(400).withHeader("Content-Type", "application/json")
            .withBody(new JSONObject().put("error", errorCode).put("error_description", description).toString())));
      try(OAuthLogCapture capture = new OAuthLogCapture())
      {
         QAuthenticationException exception = assertThrows(QAuthenticationException.class, () -> new OAuth2AuthenticationModule()
            .createSession(qInstance, Map.of("code", "authorization-code-canary", "redirectUri", "https://app.example.test/callback",
               "codeVerifier", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")));
         assertEquals("OAuth2 token request failed", exception.getMessage());
         capture.assertEventWithoutSecrets("Token request failed", description, errorCode, "authorization-code-canary", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");
         assertTrue(capture.appender.getEventList().stream().anyMatch(event -> event.getMessage().getFormattedMessage().contains("unknown")));
      }
   }



   /*******************************************************************************
    ** Capture actual formatted Log4j events, including DEBUG, with scoped cleanup.
    *******************************************************************************/
   private static class OAuthLogCapture implements AutoCloseable
   {
      private final CapturingAppender appender = new CapturingAppender();
      private final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(OAuth2AuthenticationModule.class);
      private final Level previousLevel = logger.getLevel();

      /***************************************************************************
       ** Enable DEBUG only for this logger and attach the actual output sink.
       ***************************************************************************/
      private OAuthLogCapture()
      {
         logger.setLevel(Level.DEBUG);
         appender.start();
         logger.addAppender(appender);
      }

      /***************************************************************************
       ** A required event prevents a vacuous assertion against an empty capture.
       ***************************************************************************/
      private void assertEventWithoutSecrets(String eventText, String... secrets)
      {
         String output = appender.getEventList().stream()
            .map(event -> event.getMessage().getFormattedMessage() + String.valueOf(event.getThrown()))
            .reduce("", String::concat);
         assertTrue(output.contains(eventText));
         for(String secret : secrets)
         {
            assertFalse(output.contains(secret), "Authentication credential reached the OAuth2 logger");
         }
      }

      /***************************************************************************
       ** Restore the logger even when the regression assertion fails.
       ***************************************************************************/
      @Override
      public void close()
      {
         logger.removeAppender(appender);
         logger.setLevel(previousLevel);
         appender.stop();
      }
   }



   /*******************************************************************************
    ** In-memory appender used only by these credential-redaction regressions.
    *******************************************************************************/
   private static class CapturingAppender extends AbstractAppender
   {
      private final List<LogEvent> events = new ArrayList<>();

      /***************************************************************************
       ** Preserve real formatted events without applying a test-only redactor.
       ***************************************************************************/
      private CapturingAppender()
      {
         super("OAuthCredentialCapture", null, null, true, Property.EMPTY_ARRAY);
      }

      /***************************************************************************
       ** Store an immutable copy before Log4j can reuse the original event.
       ***************************************************************************/
      @Override
      public void append(LogEvent event)
      {
         events.add(event.toImmutable());
      }

      /***************************************************************************
       ** Captured events for assertions.
       ***************************************************************************/
      private List<LogEvent> getEventList()
      {
         return (events);
      }
   }



   /***************************************************************************
    ** Set up WireMock server and QInstance before each test
    ***************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ///////////////////////////////
      // Start WireMock on a random port
      ///////////////////////////////
      wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
      wireMockServer.start();
      WireMock.configureFor("localhost", wireMockServer.port());

      ////////////////////////////////////////////////////////////////////////////
      // Clear OIDC provider metadata cache - important because WireMock starts //
      // on a different port each test, and the cache would have stale URLs     //
      ////////////////////////////////////////////////////////////////////////////
      OAuth2AuthenticationModule.clearOIDCProviderMetadataCache();

      ///////////////////////////////
      // Build the QInstance
      ///////////////////////////////
      qInstance = buildQInstance(wireMockServer.baseUrl());
      QContext.init(qInstance, new QSystemUserSession());
   }



   /***************************************************************************
    ** Clean up WireMock after each test (BaseTest handles QContext cleanup)
    ***************************************************************************/
   @AfterEach
   void tearDown()
   {
      if(wireMockServer != null)
      {
         wireMockServer.stop();
      }
   }



   /***************************************************************************
    ** Test that customizer is called when resuming a session via sessionUUID
    ***************************************************************************/
   @Test
   void testSessionResume_customizerIsCalled() throws Exception
   {
      ///////////////////////////////////////////////////////////////////////
      // Create a valid JWT access token that expires in the future        //
      ///////////////////////////////////////////////////////////////////////
      String accessToken = createTestJwt("test-user@example.com", "Test User", Map.of("customClaim", "claimValue"));

      ///////////////////////////////////////////////////////////////////////
      // Insert a UserSession record that we'll look up by UUID            //
      ///////////////////////////////////////////////////////////////////////
      String sessionUuid = "test-session-uuid-12345";
      insertUserSession(sessionUuid, accessToken, "test-user@example.com");

      ///////////////////////////////////////////////////////////////////////
      // Create session via sessionUUID (resume flow)                      //
      ///////////////////////////////////////////////////////////////////////
      OAuth2AuthenticationModule module = new OAuth2AuthenticationModule();
      Map<String, String> context = new HashMap<>();
      context.put("sessionUUID", sessionUuid);

      QSession session = module.createSession(qInstance, context);

      ///////////////////////////////////////////////////////////////////////
      // Verify session was created correctly                              //
      ///////////////////////////////////////////////////////////////////////
      assertNotNull(session);
      assertNotNull(session.getUser());
      assertEquals("test-user@example.com", session.getUser().getIdReference());
      assertEquals("Test User", session.getUser().getFullName());

      ///////////////////////////////////////////////////////////////////////
      // Verify the customizer was called and set the security key         //
      ///////////////////////////////////////////////////////////////////////
      assertTrue(session.hasSecurityKeyValue("testSecurityKey", "fromCustomizer"),
         "Customizer should have set security key on session resume");
   }



   /***************************************************************************
    ** Test that customizer is called during token exchange (PKCE flow)
    ***************************************************************************/
   @Test
   void testTokenExchange_customizerIsCalled() throws Exception
   {
      ///////////////////////////////////////////////////////////////////////
      // Create a valid JWT that will be returned by the mock token endpoint
      ///////////////////////////////////////////////////////////////////////
      String accessToken = createTestJwt("pkce-user@example.com", "PKCE User", Map.of("role", "admin"));

      ///////////////////////////////////////////////////////////////////////
      // Set up WireMock stubs for OIDC discovery and token endpoint       //
      ///////////////////////////////////////////////////////////////////////
      stubOidcDiscovery();
      stubTokenEndpoint(accessToken);

      ///////////////////////////////////////////////////////////////////////
      // Create session via code+redirectUri+codeVerifier (PKCE flow)      //
      ///////////////////////////////////////////////////////////////////////
      OAuth2AuthenticationModule module = new OAuth2AuthenticationModule();
      Map<String, String> context = new HashMap<>();
      context.put("code", "test-authorization-code");
      context.put("redirectUri", "http://localhost:3000/callback");
      context.put("codeVerifier", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");

      QSession session = module.createSession(qInstance, context);

      ///////////////////////////////////////////////////////////////////////
      // Verify session was created correctly                              //
      ///////////////////////////////////////////////////////////////////////
      assertNotNull(session);
      assertNotNull(session.getUser());
      assertEquals("pkce-user@example.com", session.getUser().getIdReference());
      assertEquals("PKCE User", session.getUser().getFullName());

      ///////////////////////////////////////////////////////////////////////
      // Verify the customizer was called and set the security key         //
      ///////////////////////////////////////////////////////////////////////
      assertTrue(session.hasSecurityKeyValue("testSecurityKey", "fromCustomizer"),
         "Customizer should have set security key during token exchange");
   }



   /***************************************************************************
    ** Test that finalCustomizeSession is called after session resume
    ***************************************************************************/
   @Test
   void testSessionResume_finalCustomizeSessionIsCalled() throws Exception
   {
      String accessToken = createTestJwt("final-test@example.com", "Final Test User", Map.of());
      String sessionUuid = "final-test-uuid-67890";
      insertUserSession(sessionUuid, accessToken, "final-test@example.com");

      OAuth2AuthenticationModule module = new OAuth2AuthenticationModule();
      Map<String, String> context = new HashMap<>();
      context.put("sessionUUID", sessionUuid);

      QSession session = module.createSession(qInstance, context);

      ///////////////////////////////////////////////////////////////////////
      // Verify finalCustomizeSession was called (sets a different key)    //
      ///////////////////////////////////////////////////////////////////////
      assertTrue(session.hasSecurityKeyValue("finalSecurityKey", "fromFinalCustomizer"),
         "finalCustomizeSession should have been called on session resume");
   }



   /***************************************************************************
    ** Test that session store integration works gracefully when QBit not present
    ** When sessionStoreEnabled=true but qbit-session-store is not on classpath,
    ** the module should fall back to the standard flow without errors.
    ***************************************************************************/
   @Test
   void testSessionResume_sessionStoreEnabled_gracefulFallbackWhenQBitNotPresent() throws Exception
   {
      ///////////////////////////////////////////////////////////////////////
      // Enable session store on the auth metadata                         //
      ///////////////////////////////////////////////////////////////////////
      OAuth2AuthenticationMetaData authMetaData = (OAuth2AuthenticationMetaData) qInstance.getAuthentication();
      authMetaData.setSessionStoreEnabled(true);

      ///////////////////////////////////////////////////////////////////////
      // Create session the normal way                                     //
      ///////////////////////////////////////////////////////////////////////
      String accessToken = createTestJwt("store-test@example.com", "Store Test User", Map.of());
      String sessionUuid = "store-test-uuid-11111";
      insertUserSession(sessionUuid, accessToken, "store-test@example.com");

      OAuth2AuthenticationModule module = new OAuth2AuthenticationModule();
      Map<String, String> context = new HashMap<>();
      context.put("sessionUUID", sessionUuid);

      ///////////////////////////////////////////////////////////////////////
      // Should complete successfully even though QBit is not on classpath //
      ///////////////////////////////////////////////////////////////////////
      QSession session = module.createSession(qInstance, context);

      assertNotNull(session);
      assertNotNull(session.getUser());
      assertEquals("store-test@example.com", session.getUser().getIdReference());
      assertEquals("Store Test User", session.getUser().getFullName());

      ///////////////////////////////////////////////////////////////////////
      // Customizers should still be called                                //
      ///////////////////////////////////////////////////////////////////////
      assertTrue(session.hasSecurityKeyValue("testSecurityKey", "fromCustomizer"));
      assertTrue(session.hasSecurityKeyValue("finalSecurityKey", "fromFinalCustomizer"));
   }



   /***************************************************************************
    ** Test that token exchange works with sessionStoreEnabled=true
    ***************************************************************************/
   @Test
   void testTokenExchange_sessionStoreEnabled_gracefulFallbackWhenQBitNotPresent() throws Exception
   {
      ///////////////////////////////////////////////////////////////////////
      // Enable session store on the auth metadata                         //
      ///////////////////////////////////////////////////////////////////////
      OAuth2AuthenticationMetaData authMetaData = (OAuth2AuthenticationMetaData) qInstance.getAuthentication();
      authMetaData.setSessionStoreEnabled(true);

      ///////////////////////////////////////////////////////////////////////
      // Set up mocks for token exchange                                   //
      ///////////////////////////////////////////////////////////////////////
      String accessToken = createTestJwt("pkce-store@example.com", "PKCE Store User", Map.of());
      stubOidcDiscovery();
      stubTokenEndpoint(accessToken);

      ///////////////////////////////////////////////////////////////////////
      // Create session via PKCE flow with session store enabled           //
      ///////////////////////////////////////////////////////////////////////
      OAuth2AuthenticationModule module = new OAuth2AuthenticationModule();
      Map<String, String> context = new HashMap<>();
      context.put("code", "test-authorization-code-2");
      context.put("redirectUri", "http://localhost:3000/callback");
      context.put("codeVerifier", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");

      QSession session = module.createSession(qInstance, context);

      ///////////////////////////////////////////////////////////////////////
      // Should complete successfully                                       //
      ///////////////////////////////////////////////////////////////////////
      assertNotNull(session);
      assertEquals("pkce-store@example.com", session.getUser().getIdReference());
   }



   /***************************************************************************
    ** Build a QInstance configured for OAuth2 with memory backend
    ***************************************************************************/
   private QInstance buildQInstance(String baseUrl) throws Exception
   {
      QInstance instance = new QInstance();

      //////////////////////////////////
      // Add memory backend           //
      //////////////////////////////////
      instance.addBackend(new QBackendMetaData()
         .withName(BACKEND_NAME)
         .withBackendType(MemoryBackendModule.class));

      //////////////////////////////////
      // Add UserSession table        //
      //////////////////////////////////
      instance.addTable(new QTableMetaData()
         .withName(UserSession.TABLE_NAME)
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("uuid"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("uuid", QFieldType.STRING))
         .withField(new QFieldMetaData("accessToken", QFieldType.STRING))
         .withField(new QFieldMetaData("userId", QFieldType.STRING))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME)));

      //////////////////////////////////
      // Add redirect state table     //
      //////////////////////////////////
      instance.addTable(new QTableMetaData()
         .withName(REDIRECT_STATE_TABLE)
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("state"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("state", QFieldType.STRING).withMaxLength(100))
         .withField(new QFieldMetaData("redirectUri", QFieldType.STRING)));

      //////////////////////////////////
      // Configure OAuth2 authentication
      //////////////////////////////////
      OAuth2AuthenticationMetaData authMetaData = new OAuth2AuthenticationMetaData();
      authMetaData.setName("oauth2");
      authMetaData.setBaseUrl(baseUrl);
      authMetaData.setClientId("test-client-id");
      authMetaData.setClientSecret("test-client-secret");
      authMetaData.setScopes("openid profile email");
      authMetaData.setUserSessionTableName(UserSession.TABLE_NAME);
      authMetaData.setRedirectStateTableName(REDIRECT_STATE_TABLE);
      authMetaData.setCustomizer(new QCodeReference(TestOAuth2Customizer.class));
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), authMetaData);

      return instance;
   }



   /***************************************************************************
    ** Create a test JWT token with the given claims
    ***************************************************************************/
   private String createTestJwt(String email, String name, Map<String, Object> additionalClaims)
   {
      JSONObject header = new JSONObject();
      header.put("alg", "HS256");
      header.put("typ", "JWT");

      JSONObject payload = new JSONObject();
      payload.put("sub", email);
      payload.put("email", email);
      payload.put("name", name);
      payload.put("exp", Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond());
      payload.put("iat", Instant.now().getEpochSecond());

      for(Map.Entry<String, Object> entry : additionalClaims.entrySet())
      {
         payload.put(entry.getKey(), entry.getValue());
      }

      String headerBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(header.toString().getBytes());
      String payloadBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toString().getBytes());

      // For testing purposes, we use a fake signature (the JWT library only decodes, doesn't verify in our usage)
      return headerBase64 + "." + payloadBase64 + ".fake-signature";
   }



   /***************************************************************************
    ** Insert a UserSession record into the memory backend
    ***************************************************************************/
   private void insertUserSession(String uuid, String accessToken, String userId) throws Exception
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(UserSession.TABLE_NAME);
      insertInput.setRecords(java.util.List.of(new com.kingsrook.qqq.backend.core.model.data.QRecord()
         .withValue("uuid", uuid)
         .withValue("accessToken", accessToken)
         .withValue("userId", userId)));

      new InsertAction().execute(insertInput);
   }



   /***************************************************************************
    ** Stub the OIDC discovery endpoint
    ***************************************************************************/
   private void stubOidcDiscovery()
   {
      String discoveryResponse = new JSONObject()
         .put("issuer", wireMockServer.baseUrl())
         .put("authorization_endpoint", wireMockServer.baseUrl() + "/authorize")
         .put("token_endpoint", wireMockServer.baseUrl() + "/oauth/token")
         .put("userinfo_endpoint", wireMockServer.baseUrl() + "/userinfo")
         .put("jwks_uri", wireMockServer.baseUrl() + "/.well-known/jwks.json")
         .put("response_types_supported", new org.json.JSONArray().put("code"))
         .put("subject_types_supported", new org.json.JSONArray().put("public"))
         .put("id_token_signing_alg_values_supported", new org.json.JSONArray().put("RS256"))
         .toString();

      wireMockServer.stubFor(get(urlEqualTo("/.well-known/openid-configuration"))
         .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(discoveryResponse)));
   }



   /***************************************************************************
    ** Stub the token endpoint to return the given access token
    ***************************************************************************/
   private void stubTokenEndpoint(String accessToken)
   {
      String tokenResponse = new JSONObject()
         .put("access_token", accessToken)
         .put("token_type", "Bearer")
         .put("expires_in", 3600)
         .toString();

      wireMockServer.stubFor(post(urlEqualTo("/oauth/token"))
         .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(tokenResponse)));
   }



   /*******************************************************************************
    ** Test customizer that sets security keys when called
    *******************************************************************************/
   public static class TestOAuth2Customizer implements QAuthenticationModuleCustomizerInterface
   {
      @Override
      public void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
      {
         /////////////////////////////////////////////////////////////////
         // Set a security key to prove customizeSession was called     //
         /////////////////////////////////////////////////////////////////
         qSession.withSecurityKeyValue("testSecurityKey", "fromCustomizer");
      }



      @Override
      public void finalCustomizeSession(QInstance qInstance, QSession qSession)
      {
         /////////////////////////////////////////////////////////////////
         // Set a different security key to prove finalCustomizeSession //
         // was called                                                   //
         /////////////////////////////////////////////////////////////////
         qSession.withSecurityKeyValue("finalSecurityKey", "fromFinalCustomizer");
      }
   }

}
