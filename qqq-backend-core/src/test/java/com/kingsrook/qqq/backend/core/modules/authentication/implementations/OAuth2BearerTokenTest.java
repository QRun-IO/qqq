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

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Bearer-token (JWT) authentication in OAuth2AuthenticationModule: the
 ** security checks (signature, algorithm, issuer, audience, expiry), native
 ** clients, key rotation, and that a bearer session carries exactly the
 ** user's permissions, as a cookie session would.
 *******************************************************************************/
class OAuth2BearerTokenTest extends BaseTest
{
   private static final String APP_BASE    = "https://auth.example.com/application/o/app/";
   private static final String NATIVE_BASE = "https://auth.example.com/application/o/app-cli/";
   private static final String APP_CLIENT  = "app";
   private static final String CLI_CLIENT  = "app-cli";

   private static RSAKey appKey;
   private static RSAKey nativeKey;
   private static RSAKey otherKey;

   private OAuth2AuthenticationMetaData metaData;



   /***************************************************************************
    **
    ***************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      if(appKey == null)
      {
         appKey = new RSAKeyGenerator(2048).keyID("app-key").generate();
         nativeKey = new RSAKeyGenerator(2048).keyID("cli-key").generate();
         otherKey = new RSAKeyGenerator(2048).keyID("app-key").generate(); // same kid, different key
      }

      OAuth2BearerTokenVerifier.setIssuerResolverForTests(baseUrl -> switch(baseUrl)
      {
         case APP_BASE -> new OAuth2BearerTokenVerifier.TrustedIssuer(APP_BASE, new ImmutableJWKSet<>(new JWKSet(appKey.toPublicJWK())));
         case NATIVE_BASE -> new OAuth2BearerTokenVerifier.TrustedIssuer(NATIVE_BASE, new ImmutableJWKSet<>(new JWKSet(nativeKey.toPublicJWK())));
         default -> null;
      });

      QInstance qInstance = QContext.getQInstance();
      metaData = new OAuth2AuthenticationMetaData();
      metaData.setName("oauth2");
      metaData.setBaseUrl(APP_BASE);
      metaData.setClientId(APP_CLIENT);
      metaData.setClientSecret("secret");
      metaData.setNativeClientId(CLI_CLIENT);
      metaData.setNativeClientBaseUrl(NATIVE_BASE);
      metaData.setBearerTokensEnabled(true);
      metaData.setCustomizer(new QCodeReference(GroupPermissionsCustomizer.class));
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), metaData);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
      OAuth2BearerTokenVerifier.setIssuerResolverForTests(null);
   }



   /***************************************************************************
    ** a valid app token makes the same user, with exactly their permissions.
    ***************************************************************************/
   @Test
   void testValidTokenMakesUserSessionWithTheirPermissions() throws Exception
   {
      String   token   = sign(appKey, claims(APP_BASE, APP_CLIENT).build());
      QSession session = createSession(token);

      assertEquals("user@example.com", session.getUser().getIdReference());
      assertEquals("Test User", session.getUser().getFullName());
      assertEquals(Set.of("person.read"), session.getPermissions());
      assertTrue(OAuth2AuthenticationModule.isBearerSessionUnexpired(session));
      assertTrue(new OAuth2AuthenticationModule().isSessionValid(QContext.getQInstance(), session));

      ///////////////////////////////////////////////////////////////
      // the token is passed to the customizer as for code exchange //
      ///////////////////////////////////////////////////////////////
      assertEquals(token, GroupPermissionsCustomizer.lastAccessToken.get());

      ///////////////////////////////////////////////////////////////////
      // one token is one session (stable uuid); another token is not //
      ///////////////////////////////////////////////////////////////////
      assertEquals(session.getUuid(), createSession(token).getUuid());
      assertNotEquals(session.getUuid(), createSession(sign(appKey, claims(APP_BASE, APP_CLIENT).jwtID("other").build())).getUuid());
   }



   /***************************************************************************
    ** no groups claim: no permissions - a bearer token grants nothing on its own.
    ***************************************************************************/
   @Test
   void testNoPrivilegeBeyondTheUsers() throws Exception
   {
      QSession session = createSession(sign(appKey, claims(APP_BASE, APP_CLIENT).claim("groups", List.of()).build()));
      assertThat(session.getPermissions()).isNullOrEmpty();

      ////////////////////////////////////////////////////////////////////////////
      // and a forged groups claim (any key but the provider's) is never read  //
      ////////////////////////////////////////////////////////////////////////////
      assertRejected(sign(otherKey, claims(APP_BASE, APP_CLIENT).claim("groups", List.of("admins")).build()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testExpiredTokenRejected() throws Exception
   {
      assertRejected(sign(appKey, claims(APP_BASE, APP_CLIENT).expirationTime(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES))).build()));
      assertRejected(sign(appKey, claims(APP_BASE, APP_CLIENT).expirationTime(null).build()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testNotYetValidTokenRejected() throws Exception
   {
      assertRejected(sign(appKey, claims(APP_BASE, APP_CLIENT).notBeforeTime(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES))).build()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWrongAudienceRejected() throws Exception
   {
      assertRejected(sign(appKey, claims(APP_BASE, "some-other-app").build()));
      assertRejected(sign(appKey, claims(APP_BASE, null).build()));

      ///////////////////////////////////////////////////////////////////////
      // the native client's id is only accepted from the native issuer,  //
      // and the app's id is not accepted from the native issuer           //
      ///////////////////////////////////////////////////////////////////////
      assertRejected(sign(appKey, claims(APP_BASE, CLI_CLIENT).build()));
      assertRejected(sign(nativeKey, claims(NATIVE_BASE, APP_CLIENT).build()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWrongIssuerRejected() throws Exception
   {
      assertRejected(sign(appKey, claims("https://evil.example.com/", APP_CLIENT).build()));
      assertRejected(sign(appKey, claims(null, APP_CLIENT).build()));

      /////////////////////////////////////////////////////////////////////////
      // a trusted issuer's name with another trusted issuer's key: refused //
      /////////////////////////////////////////////////////////////////////////
      assertRejected(sign(nativeKey, claims(APP_BASE, APP_CLIENT).build()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testBadSignatureRejected() throws Exception
   {
      assertRejected(sign(otherKey, claims(APP_BASE, APP_CLIENT).build()));

      String   token = sign(appKey, claims(APP_BASE, APP_CLIENT).build());
      String[] parts = token.split("\\.");
      String   tamperedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
         new JSONObject(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)).put("sub", "admin@example.com").toString().getBytes(StandardCharsets.UTF_8));
      assertRejected(parts[0] + "." + tamperedPayload + "." + parts[2]);
   }



   /***************************************************************************
    ** alg none, and alg confusion (HS256 keyed with the public key) are refused.
    ***************************************************************************/
   @Test
   void testNoneAndSymmetricAlgorithmsRejected() throws Exception
   {
      JWTClaimsSet claims = claims(APP_BASE, APP_CLIENT).build();
      String       none   = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
         + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(claims.toString().getBytes(StandardCharsets.UTF_8)) + ".";
      assertRejected(none);

      SignedJWT hs256 = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).keyID("app-key").build(), claims);
      hs256.sign(new MACSigner(padTo32(appKey.toPublicJWK().toRSAPublicKey().getEncoded())));
      assertRejected(hs256.serialize());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testMalformedAndOversizedRejected()
   {
      assertRejected("not-a-jwt");
      assertRejected("a.b.c");
      assertRejected("x".repeat(OAuth2BearerTokenVerifier.MAX_TOKEN_LENGTH + 1));
   }



   /***************************************************************************
    ** the native (public) client's tokens are accepted from its own issuer.
    ***************************************************************************/
   @Test
   void testNativeClientTokenAccepted() throws Exception
   {
      QSession session = createSession(sign(nativeKey, claims(NATIVE_BASE, CLI_CLIENT).build()));
      assertEquals("user@example.com", session.getUser().getIdReference());
      assertEquals(Set.of("person.read"), session.getPermissions());
   }



   /***************************************************************************
    ** additional audiences (e.g., another client of the same provider).
    ***************************************************************************/
   @Test
   void testAdditionalAudience() throws Exception
   {
      String token = sign(appKey, claims(APP_BASE, "reporting-service").build());
      assertRejected(token);
      metaData.setAdditionalBearerAudiences(List.of("reporting-service"));
      assertEquals("user@example.com", createSession(token).getUser().getIdReference());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testBearerTokensDisabled() throws Exception
   {
      assertFalse(new OAuth2AuthenticationMetaData().getBearerTokensEnabled(), "bearer tokens are opt-in");
      metaData.setBearerTokensEnabled(false);
      assertRejected(sign(appKey, claims(APP_BASE, APP_CLIENT).build()));
   }



   /***************************************************************************
    ** a machine token without sub is identified by its signed username.
    ***************************************************************************/
   @Test
   void testMachineTokenWithoutSub() throws Exception
   {
      assertRejected(sign(appKey, claims(APP_BASE, APP_CLIENT).subject(null).claim("email", null).claim("preferred_username", "svc-export").build()));
      metaData.setBearerMachineTokenUsernameSubject(true);
      QSession session = createSession(sign(appKey, claims(APP_BASE, APP_CLIENT).subject(null).claim("email", null).claim("preferred_username", "svc-export").build()));
      assertEquals("username:svc-export", session.getUser().getIdReference());

      assertRejected(sign(appKey, claims(APP_BASE, APP_CLIENT).subject(null).build()));
   }



   /***************************************************************************
    ** keys rotate without a restart: a token naming an unknown kid refetches.
    ***************************************************************************/
   @Test
   void testKeyRotation() throws Exception
   {
      RSAKey                  rotatedKey = new RSAKeyGenerator(2048).keyID("rotated").generate();
      AtomicReference<JWKSet> published  = new AtomicReference<>(new JWKSet(appKey.toPublicJWK()));
      var keySource = OAuth2BearerTokenVerifier.buildKeySource(new URL("https://auth.example.com/jwks"), url -> new Resource(published.get().toString(), "application/json"), 0);
      OAuth2BearerTokenVerifier.setIssuerResolverForTests(baseUrl -> APP_BASE.equals(baseUrl) ? new OAuth2BearerTokenVerifier.TrustedIssuer(APP_BASE, keySource) : null);

      createSession(sign(appKey, claims(APP_BASE, APP_CLIENT).build()));
      assertRejected(sign(rotatedKey, claims(APP_BASE, APP_CLIENT).build()));

      published.set(new JWKSet(List.of(appKey.toPublicJWK(), rotatedKey.toPublicJWK())));
      assertEquals("user@example.com", createSession(sign(rotatedKey, claims(APP_BASE, APP_CLIENT).build())).getUser().getIdReference());
   }



   /***************************************************************************
    ** real discovery: issuer and jwks_uri read from the provider's
    ** .well-known/openid-configuration (a loopback fixture).
    ***************************************************************************/
   @Test
   void testDiscovery() throws Exception
   {
      com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
      String                            base   = "http://127.0.0.1:" + server.getAddress().getPort() + "/application/o/disc/";
      server.createContext("/application/o/disc/.well-known/openid-configuration", exchange ->
      {
         byte[] body = new JSONObject().put("issuer", base).put("jwks_uri", base + "jwks/").toString().getBytes(StandardCharsets.UTF_8);
         exchange.sendResponseHeaders(200, body.length);
         exchange.getResponseBody().write(body);
         exchange.close();
      });
      server.createContext("/application/o/disc/jwks/", exchange ->
      {
         byte[] body = new JWKSet(appKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
         exchange.sendResponseHeaders(200, body.length);
         exchange.getResponseBody().write(body);
         exchange.close();
      });
      server.start();
      try
      {
         OAuth2BearerTokenVerifier.setIssuerResolverForTests(null);
         metaData.setBaseUrl(base);
         metaData.setNativeClientId(null);
         metaData.setNativeClientBaseUrl(null);
         assertEquals("user@example.com", createSession(sign(appKey, claims(base, APP_CLIENT).build())).getUser().getIdReference());
         assertRejected(sign(otherKey, claims(base, APP_CLIENT).build()));
      }
      finally
      {
         server.stop(0);
      }
   }



   /***************************************************************************
    ** an expired bearer session is invalid (e.g. for a queued async job).
    ***************************************************************************/
   @Test
   void testBearerSessionValidity()
   {
      QSession session = new QSession();
      session.setValue(OAuth2AuthenticationModule.BEARER_SESSION_VALUE_KEY, "true");
      session.setValue(OAuth2AuthenticationModule.BEARER_SESSION_EXPIRES_AT_VALUE_KEY, String.valueOf(Instant.now().minusSeconds(1).getEpochSecond()));
      assertFalse(new OAuth2AuthenticationModule().isSessionValid(QContext.getQInstance(), session));

      session.setValue(OAuth2AuthenticationModule.BEARER_SESSION_EXPIRES_AT_VALUE_KEY, "garbage");
      assertFalse(OAuth2AuthenticationModule.isBearerSessionUnexpired(session));
   }



   /***************************************************************************
    ** `Bearer <session uuid>` resumes a stored session (here: none, so 401)
    ** and is never treated as a JWT.
    ***************************************************************************/
   @Test
   void testBearerSessionUuidIsResumedNotVerified()
   {
      QAuthenticationException e = assertThrows(QAuthenticationException.class, () -> createSession("1b4e28ba-2fa1-11d2-883f-0016d3cca427"));
      assertNotEquals(OAuth2BearerTokenVerifier.REJECTED_MESSAGE, e.getMessage());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QSession createSession(String token) throws QAuthenticationException
   {
      return (new OAuth2AuthenticationModule().createSession(QContext.getQInstance(), Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void assertRejected(String token)
   {
      QAuthenticationException e = assertThrows(QAuthenticationException.class, () -> createSession(token));
      assertEquals(OAuth2BearerTokenVerifier.REJECTED_MESSAGE, e.getMessage());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static JWTClaimsSet.Builder claims(String issuer, String audience)
   {
      return (new JWTClaimsSet.Builder()
         .issuer(issuer)
         .audience(audience)
         .subject("user@example.com")
         .claim("email", "user@example.com")
         .claim("name", "Test User")
         .claim("groups", List.of("readers"))
         .claim("scope", "openid profile email")
         .issueTime(new Date())
         .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS))));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static String sign(RSAKey key, JWTClaimsSet claims) throws Exception
   {
      SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).type(JOSEObjectType.JWT).build(), claims);
      jwt.sign(new RSASSASigner(key));
      return (jwt.serialize());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static byte[] padTo32(byte[] bytes)
   {
      return (bytes.length >= 32 ? bytes : java.util.Arrays.copyOf(bytes, 32));
   }



   /*******************************************************************************
    ** Grants permissions from the token's groups claim, as an IdP mapper would.
    *******************************************************************************/
   public static class GroupPermissionsCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      static final AtomicReference<String> lastAccessToken = new AtomicReference<>();



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
      {
         lastAccessToken.set((String) context.get("accessToken"));
         JSONObject payload = (JSONObject) context.get("jwtPayloadJsonObject");
         if(payload != null && payload.optJSONArray("groups") != null && payload.getJSONArray("groups").toList().contains("readers"))
         {
            qSession.setPermissions(Set.of("person.read"));
         }
         assertNull(context.get("idToken"));
      }
   }
}
