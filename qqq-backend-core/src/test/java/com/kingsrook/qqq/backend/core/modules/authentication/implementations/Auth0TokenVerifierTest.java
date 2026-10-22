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


import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.Auth0AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/***************************************************************************
 ** Exercise the public Auth0 session entry point with real RSA/JWKS verification.
 ** Tokens and private keys remain in memory and are never included in assertions.
 ***************************************************************************/
class Auth0TokenVerifierTest extends BaseTest
{
   private static final String KEY_ID = "verifier-fixture-key";
   private static final String API_AUDIENCE = "urn:qqq:verifier-fixture:api";
   private static final String CLIENT_ID = "verifier-fixture-client";
   private static final String SUBJECT = "verifier-fixture-user";
   private static final String JWKS_PATH = "/.well-known/jwks.json";

   private WireMockServer server;
   private QInstance instance;
   private QInstance otherInstance;
   private KeyPair signingKey;
   private String issuer;
   private String token;



   /***************************************************************************
    ** Publish only the generated public key at the configured issuer's JWKS.
    ***************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      signingKey = generator.generateKeyPair();
      RSAPublicKey publicKey = (RSAPublicKey) signingKey.getPublic();
      JSONObject jwk = new JSONObject()
         .put("kty", "RSA").put("use", "sig").put("alg", "RS256").put("kid", KEY_ID)
         .put("n", unsignedBase64(publicKey.getModulus()))
         .put("e", unsignedBase64(publicKey.getPublicExponent()));
      server = new WireMockServer(WireMockConfiguration.options().bindAddress("127.0.0.1").dynamicPort());
      server.start();
      issuer = "http://127.0.0.1:" + server.port() + "/";
      server.stubFor(get(urlEqualTo(JWKS_PATH)).willReturn(aResponse()
         .withHeader("Content-Type", "application/json")
         .withBody(new JSONObject().put("keys", new JSONArray().put(jwk)).toString())));
      instance = new QInstance();
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new Auth0AuthenticationMetaData()
         .withBaseUrl(issuer).withClientId(CLIENT_ID).withAudience(API_AUDIENCE));
   }



   /***************************************************************************
    ** Remove only the owned token's cache entry, including on a failed assertion.
    ***************************************************************************/
   @AfterEach
   void tearDown()
   {
      try
      {
         if(token != null)
         {
            InMemoryStateProvider.getInstance().remove(Auth0AuthenticationModule.tokenValidationKey(instance, token));
            if(otherInstance != null)
            {
               InMemoryStateProvider.getInstance().remove(Auth0AuthenticationModule.tokenValidationKey(otherInstance, token));
            }
         }
      }
      finally
      {
         if(server != null)
         {
            server.stop();
         }
      }
   }



   /***************************************************************************
    ** A correctly signed credential for the configured issuer and API is valid.
    ***************************************************************************/
   @Test
   void acceptsConfiguredIssuerAndAudience() throws Exception
   {
      token = validClaims().sign(algorithm(signingKey));
      assertAccepted();
   }



   /***************************************************************************
    ** A configured URL without its final slash still identifies the same issuer.
    ***************************************************************************/
   @Test
   void acceptsConfiguredBaseUrlWithoutTrailingSlash() throws Exception
   {
      ((Auth0AuthenticationMetaData) instance.getAuthentication()).setBaseUrl(issuer.substring(0, issuer.length() - 1));
      token = validClaims().sign(algorithm(signingKey));
      assertAccepted();
   }



   /***************************************************************************
    ** Normalize only configured values; custom and tenant domains stay distinct.
    ***************************************************************************/
   @Test
   void normalizesConfiguredDomainForms()
   {
      assertEquals("https://tenant.auth0.com/", Auth0AuthenticationModule.normalizeAuth0Issuer("tenant.auth0.com"));
      assertEquals("https://login.example.com/", Auth0AuthenticationModule.normalizeAuth0Issuer("login.example.com"));
      assertEquals("https://login.example.com/", Auth0AuthenticationModule.normalizeAuth0Issuer("https://login.example.com/"));
      assertEquals("https://login.example.com/auth/", Auth0AuthenticationModule.normalizeAuth0Issuer("https://login.example.com/auth"));
   }



   /***************************************************************************
    ** An API audience may coexist with the userinfo audience in an array.
    ***************************************************************************/
   @Test
   void acceptsAudienceArrayContainingConfiguredApi() throws Exception
   {
      token = validClaims().withAudience(API_AUDIENCE, issuer + "userinfo").sign(algorithm(signingKey));
      assertAccepted();
   }



   /***************************************************************************
    ** A key from the configured JWKS does not authorize a different issuer.
    ***************************************************************************/
   @Test
   void rejectsWrongIssuerWithSameSigningKey()
   {
      token = validClaims().withIssuer("urn:qqq:verifier-fixture:other-issuer").sign(algorithm(signingKey));
      assertThrows(QAuthenticationException.class, this::createSession);
   }



   /***************************************************************************
    ** A credential intended for another API cannot create this API's session.
    ***************************************************************************/
   @Test
   void rejectsWrongAudienceWithSameSigningKey()
   {
      token = validClaims().withClaim("aud", "urn:qqq:verifier-fixture:other-api").sign(algorithm(signingKey));
      assertThrows(QAuthenticationException.class, this::createSession);
   }



   /***************************************************************************
    ** The same configured trust settings can reuse a successful validation.
    ***************************************************************************/
   @Test
   void reusesValidationForSameConfiguration() throws Exception
   {
      token = validClaims().sign(algorithm(signingKey));
      assertAccepted();
      QSession second = new Auth0AuthenticationModule().createSession(instance, Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token));
      assertEquals(SUBJECT, second.getUser().getIdReference());
      assertEquals(1, server.getAllServeEvents().size(), "Same configuration should reuse validated token");
   }



   /***************************************************************************
    ** A token validated for one API must not bypass another API's audience check.
    ***************************************************************************/
   @Test
   void rejectsCachedTokenForAnotherConfiguredAudience() throws Exception
   {
      token = validClaims().sign(algorithm(signingKey));
      assertAccepted();
      assertTrue(InMemoryStateProvider.getInstance().get(Instant.class, Auth0AuthenticationModule.tokenValidationKey(instance, token)).isPresent(),
         "The first public call must populate the validation cache");

      otherInstance = new QInstance();
      otherInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new Auth0AuthenticationMetaData()
         .withBaseUrl(issuer).withClientId(CLIENT_ID).withAudience("urn:qqq:verifier-fixture:other-api"));
      assertThrows(QAuthenticationException.class,
         () -> new Auth0AuthenticationModule().createSession(otherInstance, Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token)),
         () -> "JWKS request count across both configurations: " + server.getAllServeEvents().size());
   }



   /***************************************************************************
    ** A client-addressed ID token is not an API access credential.
    ***************************************************************************/
   @Test
   void rejectsIdTokenShapedClientAudience()
   {
      token = validClaims().withClaim("aud", CLIENT_ID).withClaim("nonce", "fixture-nonce")
         .withClaim("auth_time", Instant.now().minusSeconds(60).getEpochSecond()).sign(algorithm(signingKey));
      assertThrows(QAuthenticationException.class, this::createSession);
   }



   /***************************************************************************
    ** The correct claims must not bypass signature verification.
    ***************************************************************************/
   @Test
   void rejectsWrongSignature() throws Exception
   {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      token = validClaims().sign(algorithm(generator.generateKeyPair()));
      assertThrows(QAuthenticationException.class, this::createSession);
   }



   /***************************************************************************
    ** A correctly signed but expired credential must be rejected.
    ***************************************************************************/
   @Test
   void rejectsExpiredToken()
   {
      token = validClaims().withExpiresAt(Instant.now().minusSeconds(300)).sign(algorithm(signingKey));
      QAuthenticationException error = assertThrows(QAuthenticationException.class, this::createSession);
      assertEquals(Auth0AuthenticationModule.EXPIRED_TOKEN_ERROR, error.getMessage());
   }



   /***************************************************************************
    ** Reusing a validated token must still enforce its natural expiry.
    ***************************************************************************/
   @Test
   void rejectsPreviouslyValidatedTokenAfterExpiry() throws Exception
   {
      Instant expiration = Instant.now().plusSeconds(5).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
      token = validClaims().withExpiresAt(expiration).sign(algorithm(signingKey));
      assertAccepted();
      long waitMillis = java.time.Duration.between(Instant.now(), expiration).toMillis() + 20;
      if(waitMillis > 0)
      {
         Thread.sleep(waitMillis);
      }
      assertFalse(Instant.now().isBefore(expiration), "Fixture must have reached its declared expiry");
      assertThrows(QAuthenticationException.class,
         () -> new Auth0AuthenticationModule().createSession(instance, Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token)));
   }



   /***************************************************************************
    ** Each invocation starts uncached and reaches exactly one owned JWKS request.
    ***************************************************************************/
   private QSession createSession() throws QAuthenticationException
   {
      assertFalse(InMemoryStateProvider.getInstance().get(Instant.class, Auth0AuthenticationModule.tokenValidationKey(instance, token)).isPresent(),
         "The verifier must not be bypassed by the validation cache");
      try
      {
         return new Auth0AuthenticationModule().createSession(instance, Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token));
      }
      finally
      {
         assertEquals(1, server.findAll(getRequestedFor(urlEqualTo(JWKS_PATH))).size(), "Expected one configured JWKS lookup");
         assertEquals(1, server.getAllServeEvents().size(), "Unexpected provider request");
      }
   }



   /***************************************************************************
    ** Check only the synthetic user identity, never the session bearer value.
    ***************************************************************************/
   private void assertAccepted() throws QAuthenticationException
   {
      QSession session = createSession();
      assertNotNull(session);
      assertNotNull(session.getUser());
      assertEquals(SUBJECT, session.getUser().getIdReference());
   }



   /***************************************************************************
    ** Common valid claims; each negative control changes only its tested boundary.
    ***************************************************************************/
   private JWTCreator.Builder validClaims()
   {
      return JWT.create().withKeyId(KEY_ID).withIssuer(issuer).withSubject(SUBJECT)
         .withClaim("name", "System User Verifier Fixture").withClaim("aud", API_AUDIENCE)
         .withIssuedAt(Instant.now().minusSeconds(600)).withExpiresAt(Instant.now().plusSeconds(600));
   }



   /***************************************************************************
    ** Construct the real signer using an in-memory RSA key pair.
    ***************************************************************************/
   private Algorithm algorithm(KeyPair key)
   {
      return Algorithm.RSA256((RSAPublicKey) key.getPublic(), (RSAPrivateKey) key.getPrivate());
   }



   /***************************************************************************
    ** JWKS encodes unsigned RSA integers without BigInteger's sign-padding byte.
    ***************************************************************************/
   private String unsignedBase64(BigInteger value)
   {
      byte[] bytes = value.toByteArray();
      if(bytes[0] == 0)
      {
         bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
      }
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
   }
}
