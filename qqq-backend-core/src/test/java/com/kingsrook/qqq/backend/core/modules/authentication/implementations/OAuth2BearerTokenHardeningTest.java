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


import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreProviderInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Security review follow-ups for bearer tokens (QRun-IO/qqq#1229): ID tokens,
 ** typ, JWE, other key types, jku/x5u, clock skew, lifetime, discovery outage
 ** and issuer mismatch, kid refetch rate limit, the session store, stored
 ** (cookie) sessions, and subclass refusal.
 *******************************************************************************/
class OAuth2BearerTokenHardeningTest extends BaseTest
{
   private static final String APP_BASE   = "https://auth.example.com/application/o/app/";
   private static final String APP_CLIENT = "app";

   private static RSAKey rsaKey;
   private static ECKey  ecKey;
   private static RSAKey psKey;

   private OAuth2AuthenticationMetaData metaData;
   private final AtomicInteger          discoveries = new AtomicInteger();



   /***************************************************************************
    **
    ***************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      if(rsaKey == null)
      {
         rsaKey = new RSAKeyGenerator(2048).keyID("rsa").generate();
         ecKey = new ECKeyGenerator(Curve.P_256).keyID("ec").generate();
         psKey = new RSAKeyGenerator(2048).keyID("ps").generate();
      }

      useResolver(baseUrl ->
      {
         discoveries.incrementAndGet();
         return (APP_BASE.equals(baseUrl) ? new OAuth2BearerTokenVerifier.TrustedIssuer(APP_BASE, new ImmutableJWKSet<>(new JWKSet(List.of(rsaKey.toPublicJWK(), ecKey.toPublicJWK(), psKey.toPublicJWK())))) : null);
      });

      QInstance qInstance = QContext.getQInstance();
      metaData = new OAuth2AuthenticationMetaData();
      metaData.setName("oauth2");
      metaData.setBaseUrl(APP_BASE);
      metaData.setClientId(APP_CLIENT);
      metaData.setClientSecret("secret");
      metaData.setBearerTokensEnabled(true);
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), metaData);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
      OAuth2BearerTokenVerifier.setIssuerResolverForTests(null);
      QSessionStoreRegistry.getInstance().clear();
   }



   /***************************************************************************
    ** ID tokens are not access tokens.
    ***************************************************************************/
   @Test
   void testIdTokensRejected() throws Exception
   {
      assertRejected(sign(rsaKey, JWSAlgorithm.RS256, null, claims().claim("nonce", "n-1").build()));
      assertRejected(sign(rsaKey, JWSAlgorithm.RS256, null, claims().claim("at_hash", "abc").build()));
      assertRejected(sign(rsaKey, JWSAlgorithm.RS256, null, claims().claim("scope", null).build()));
      assertAccepted(sign(rsaKey, JWSAlgorithm.RS256, null, claims().claim("scope", null).claim("azp", APP_CLIENT).build()));
      assertAccepted(sign(rsaKey, JWSAlgorithm.RS256, null, claims().claim("scope", null).claim("client_id", APP_CLIENT).build()));
   }



   /***************************************************************************
    ** typ at+jwt (RFC 9068) is an access token on its own; other typ values are refused.
    ***************************************************************************/
   @Test
   void testTypHeader() throws Exception
   {
      assertAccepted(sign(rsaKey, JWSAlgorithm.RS256, new JOSEObjectType("at+jwt"), claims().claim("scope", null).build()));
      assertRejected(sign(rsaKey, JWSAlgorithm.RS256, new JOSEObjectType("id+jwt"), claims().build()));
      assertRejected(sign(rsaKey, JWSAlgorithm.RS256, new JOSEObjectType("logout+jwt"), claims().build()));
   }



   /***************************************************************************
    ** an encrypted JWT (JWE) is never accepted.
    ***************************************************************************/
   @Test
   void testJweRejected() throws Exception
   {
      EncryptedJWT jwe = new EncryptedJWT(new JWEHeader(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM), claims().build());
      jwe.encrypt(new RSAEncrypter(rsaKey.toRSAPublicKey()));
      assertRejected(jwe.serialize());
   }



   /***************************************************************************
    ** EC (ES256) and RSA-PSS (PS256) keys are accepted, from the issuer's JWKS.
    ***************************************************************************/
   @Test
   void testEcAndPssKeys() throws Exception
   {
      assertAccepted(sign(ecKey, JWSAlgorithm.ES256, null, claims().build()));
      assertAccepted(sign(psKey, JWSAlgorithm.PS256, null, claims().build()));
   }



   /***************************************************************************
    ** jku / x5u headers never choose the key: an attacker's key named there is
    ** not trusted (the issuer's JWKS is the only source).
    ***************************************************************************/
   @Test
   void testJkuAndX5uIgnored() throws Exception
   {
      RSAKey    attacker = new RSAKeyGenerator(2048).keyID("attacker").generate();
      SignedJWT jwt      = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("attacker")
         .jwkURL(URI.create("https://evil.example.com/jwks")).x509CertURL(URI.create("https://evil.example.com/cert")).build(), claims().build());
      jwt.sign(new RSASSASigner(attacker));
      assertRejected(jwt.serialize());

      SignedJWT embedded = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("attacker").jwk(attacker.toPublicJWK()).build(), claims().build());
      embedded.sign(new RSASSASigner(attacker));
      assertRejected(embedded.serialize());
   }



   /***************************************************************************
    ** 30 seconds of clock skew, no more.
    ***************************************************************************/
   @Test
   void testClockSkewBoundary() throws Exception
   {
      Instant now = Instant.now();
      assertAccepted(sign(rsaKey, JWSAlgorithm.RS256, null, claims().expirationTime(Date.from(now.minusSeconds(20))).build()));
      assertRejected(sign(rsaKey, JWSAlgorithm.RS256, null, claims().expirationTime(Date.from(now.minusSeconds(60))).build()));
      assertAccepted(sign(rsaKey, JWSAlgorithm.RS256, null, claims().notBeforeTime(Date.from(now.plusSeconds(20))).build()));
      assertRejected(sign(rsaKey, JWSAlgorithm.RS256, null, claims().notBeforeTime(Date.from(now.plusSeconds(60))).build()));
   }



   /***************************************************************************
    ** a bounded lifetime (exp - iat, or exp - now without iat).
    ***************************************************************************/
   @Test
   void testMaxLifetime() throws Exception
   {
      Instant now = Instant.now();
      assertRejected(sign(rsaKey, JWSAlgorithm.RS256, null, claims().issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(3 * 24 * 3600))).build()));
      assertRejected(sign(rsaKey, JWSAlgorithm.RS256, null, claims().issueTime(null).expirationTime(Date.from(now.plusSeconds(3 * 24 * 3600))).build()));
      metaData.setBearerTokenMaxLifetimeSeconds(4 * 24 * 3600);
      assertAccepted(sign(rsaKey, JWSAlgorithm.RS256, null, claims().issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(3 * 24 * 3600))).build()));
      metaData.setBearerTokenMaxLifetimeSeconds(600);
      assertRejected(sign(rsaKey, JWSAlgorithm.RS256, null, claims().build()));
   }



   /***************************************************************************
    ** an untrusted issuer is refused before any discovery; a failed discovery
    ** is cached (no request per token); a discovered issuer other than the
    ** configured one is refused (and cached as a failure).
    ***************************************************************************/
   @Test
   void testDiscoveryOutageAndIssuerMismatch() throws Exception
   {
      assertRejected(sign(rsaKey, JWSAlgorithm.RS256, null, claims().issuer("https://evil.example.com/").build()));
      assertEquals(0, discoveries.get(), "no discovery for an unconfigured issuer");

      useResolver(baseUrl ->
      {
         discoveries.incrementAndGet();
         return (null);
      });
      String token = sign(rsaKey, JWSAlgorithm.RS256, null, claims().build());
      for(int i = 0; i < 5; i++)
      {
         assertRejected(token);
      }
      assertEquals(1, discoveries.get(), "a failed discovery is cached");

      useResolver(baseUrl ->
      {
         discoveries.incrementAndGet();
         return (new OAuth2BearerTokenVerifier.TrustedIssuer("https://other.example.com/", new ImmutableJWKSet<>(new JWKSet(rsaKey.toPublicJWK()))));
      });
      discoveries.set(0);
      assertRejected(token);
      assertRejected(token);
      assertEquals(1, discoveries.get(), "an issuer mismatch is cached as a failure");
   }



   /***************************************************************************
    ** an unknown kid refetches the JWKS at most once per rate-limit interval.
    ***************************************************************************/
   @Test
   void testUnknownKidRefetchIsRateLimited() throws Exception
   {
      AtomicInteger fetches = new AtomicInteger();
      var keySource = OAuth2BearerTokenVerifier.buildKeySource(new URL("https://auth.example.com/jwks"), url ->
      {
         fetches.incrementAndGet();
         return (new Resource(new JWKSet(rsaKey.toPublicJWK()).toString(), "application/json"));
      }, 60_000L);
      useResolver(baseUrl -> APP_BASE.equals(baseUrl) ? new OAuth2BearerTokenVerifier.TrustedIssuer(APP_BASE, keySource) : null);

      assertAccepted(sign(rsaKey, JWSAlgorithm.RS256, null, claims().build()));
      RSAKey unknown = new RSAKeyGenerator(2048).keyID("unknown").generate();
      for(int i = 0; i < 5; i++)
      {
         assertRejected(sign(unknown, JWSAlgorithm.RS256, null, claims().build()));
      }
      assertThat(fetches.get()).isLessThanOrEqualTo(2);
   }



   /***************************************************************************
    ** with a session store: the cached session's TTL is capped at the token's
    ** expiry, an expired cached session is not used, and a bearer session's
    ** derived uuid is never a credential (Bearer <uuid> / sessionUUID).
    ***************************************************************************/
   @Test
   void testSessionStore() throws Exception
   {
      MemoryStore store = new MemoryStore();
      QSessionStoreRegistry.getInstance().register(store);
      metaData.setSessionStoreEnabled(true);

      String   token   = sign(rsaKey, JWSAlgorithm.RS256, null, claims().expirationTime(Date.from(Instant.now().plusSeconds(120))).build());
      QSession session = createSession(Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token));
      assertThat(store.ttls.get(session.getUuid())).isLessThanOrEqualTo(Duration.ofSeconds(121));
      assertEquals(session.getUuid(), createSession(Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token)).getUuid());

      ///////////////////////////////////////////////////////
      // the derived uuid is not a credential on its own   //
      ///////////////////////////////////////////////////////
      assertThrows(QAuthenticationException.class, () -> createSession(Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, session.getUuid())));
      assertThrows(QAuthenticationException.class, () -> createSession(Map.of("sessionUUID", session.getUuid())));

      ///////////////////////////////////////////////////////////////////
      // an expired session in the store is not returned for a token   //
      ///////////////////////////////////////////////////////////////////
      store.sessions.get(session.getUuid()).setValue(OAuth2AuthenticationModule.BEARER_SESSION_EXPIRES_AT_VALUE_KEY, String.valueOf(Instant.now().minusSeconds(5).getEpochSecond()));
      QSession fresh = createSession(Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token));
      assertTrue(OAuth2AuthenticationModule.isBearerSessionUnexpired(fresh));
   }



   /***************************************************************************
    ** a bearer JWT never becomes a stored (cookie) session - manageSession
    ** passes DO_STORE_USER_SESSION_KEY.
    ***************************************************************************/
   @Test
   void testStoredSessionFromJwtRefused() throws Exception
   {
      String token = sign(rsaKey, JWSAlgorithm.RS256, null, claims().build());
      QAuthenticationException e = assertThrows(QAuthenticationException.class, () -> createSession(Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token, Auth0AuthenticationModule.DO_STORE_USER_SESSION_KEY, "true")));
      assertThat(e.getMessage()).contains("cannot create a stored session");
   }



   /***************************************************************************
    ** a subclass can refuse a verified token (customizeBearerSession).
    ***************************************************************************/
   @Test
   void testCustomizeBearerSessionRefusal() throws Exception
   {
      String token = sign(rsaKey, JWSAlgorithm.RS256, null, claims().build());
      OAuth2AuthenticationModule refusing = new OAuth2AuthenticationModule()
      {
         @Override
         protected void customizeBearerSession(QInstance qInstance, QSession session, JWTClaimsSet claims, String bearerToken) throws QAuthenticationException
         {
            throw (new QAuthenticationException("refused by the app"));
         }
      };
      assertThrows(QAuthenticationException.class, () -> refusing.createSession(QContext.getQInstance(), Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token)));
      assertNotEquals(null, createSession(Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token)));
   }



   /***************************************************************************
    ** a cookie (sessionUUID) in the same context wins over a bearer value: the
    ** bearer branch is not taken, so the token is not verified as a JWT.
    ***************************************************************************/
   @Test
   void testCookieTakesPrecedence() throws Exception
   {
      String token = sign(rsaKey, JWSAlgorithm.RS256, null, claims().build());
      discoveries.set(0);
      assertThrows(QAuthenticationException.class, () -> createSession(Map.of("sessionUUID", "1b4e28ba-2fa1-11d2-883f-0016d3cca427", Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token)));
      assertEquals(0, discoveries.get());
      assertFalse(metaData.getBearerMachineTokenUsernameSubject());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void useResolver(java.util.function.Function<String, OAuth2BearerTokenVerifier.TrustedIssuer> resolver)
   {
      OAuth2BearerTokenVerifier.setIssuerResolverForTests(resolver);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QSession createSession(Map<String, String> context) throws QAuthenticationException
   {
      return (new OAuth2AuthenticationModule().createSession(QContext.getQInstance(), context));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void assertRejected(String token)
   {
      QAuthenticationException e = assertThrows(QAuthenticationException.class, () -> createSession(Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token)));
      assertEquals(OAuth2BearerTokenVerifier.REJECTED_MESSAGE, e.getMessage());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void assertAccepted(String token) throws QAuthenticationException
   {
      assertEquals("user@example.com", createSession(Map.of(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token)).getUser().getIdReference());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static JWTClaimsSet.Builder claims()
   {
      return (new JWTClaimsSet.Builder()
         .issuer(APP_BASE)
         .audience(APP_CLIENT)
         .subject("user@example.com")
         .claim("email", "user@example.com")
         .claim("scope", "openid profile email")
         .issueTime(new Date())
         .expirationTime(Date.from(Instant.now().plusSeconds(1800))));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static String sign(JWK key, JWSAlgorithm algorithm, JOSEObjectType type, JWTClaimsSet claims) throws Exception
   {
      JWSSigner signer = key instanceof ECKey ec ? new ECDSASigner(ec) : new RSASSASigner((RSAKey) key);
      SignedJWT jwt    = new SignedJWT(new JWSHeader.Builder(algorithm).keyID(key.getKeyID()).type(type).build(), claims);
      jwt.sign(signer);
      return (jwt.serialize());
   }



   /*******************************************************************************
    ** An in-memory session store that records TTLs.
    *******************************************************************************/
   private static class MemoryStore implements QSessionStoreProviderInterface
   {
      final Map<String, QSession> sessions = new ConcurrentHashMap<>();
      final Map<String, Duration> ttls     = new ConcurrentHashMap<>();



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void store(String sessionUuid, QSession session, Duration ttl)
      {
         sessions.put(sessionUuid, session);
         ttls.put(sessionUuid, ttl);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Optional<QSession> load(String sessionUuid)
      {
         return (Optional.ofNullable(sessions.get(sessionUuid)));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void remove(String sessionUuid)
      {
         sessions.remove(sessionUuid);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void touch(String sessionUuid)
      {
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Duration getDefaultTtl()
      {
         return (Duration.ofHours(1));
      }
   }
}
