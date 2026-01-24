/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
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
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Integration tests for OAuth2AuthenticationModule with WireMock
 *******************************************************************************/
class OAuth2AuthenticationModuleIntegrationTest extends BaseTest
{
   private static final String BACKEND_NAME = "memory";
   private static final String REDIRECT_STATE_TABLE = "oauthRedirectState";

   private WireMockServer wireMockServer;
   private QInstance      qInstance;



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
      instance.setAuthentication(authMetaData);

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
