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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Verifies an `Authorization: Bearer <JWT>` access token issued by the app's
 ** OAuth2 / OIDC provider, without a server-side session.
 **
 ** A token is accepted only when ALL of these hold:
 ** - it is a signed JWT (JWS); `alg: none`, unsigned (plain) and encrypted
 **   tokens, and symmetric (HS*) algorithms are refused;
 ** - its `iss` is one of the trusted issuers: the app's provider (baseUrl), or
 **   the native client's provider (nativeClientBaseUrl);
 ** - its signature verifies against that issuer's published JWKS, fetched from
 **   the jwks_uri of the issuer's OIDC discovery document.  Keys are cached and
 **   refetched (rate limited) when a token names an unknown key id, so key
 **   rotation needs no restart;
 ** - its `aud` contains an accepted client id for that issuer: clientId and
 **   additionalBearerAudiences for the app's issuer, nativeClientId for the
 **   native issuer;
 ** - it has `exp` in the future and `nbf` (if any) in the past, with
 **   MAX_CLOCK_SKEW_SECONDS of tolerance, and lives no longer than
 **   bearerTokenMaxLifetimeSeconds (exp - iat, or exp - now without iat);
 ** - it is an access token, not an ID token: no `nonce` or `at_hash`, and at
 **   least one of `scope`, `azp`, `client_id` (typ `at+jwt` is accepted too);
 ** - it has a non-blank `sub` (machine tokens without one only when
 **   bearerMachineTokenUsernameSubject is on, then `preferred_username`).
 **
 ** Before any network call, the token's (unverified) `iss` must equal one of
 ** the configured issuers, so a forged token cannot trigger discovery.  The
 ** discovery document's issuer must equal the configured one (OIDC Discovery
 ** section 4.3).  A failed discovery is cached with backoff (30 s doubling to 5
 ** minutes), so a provider outage is not amplified by requests.
 **
 ** Errors never reveal which check failed (they are logged at debug level).
 *******************************************************************************/
public class OAuth2BearerTokenVerifier
{
   private static final QLogger LOG = QLogger.getLogger(OAuth2BearerTokenVerifier.class);

   public static final int MAX_TOKEN_LENGTH       = 16_384;
   public static final int MAX_CLOCK_SKEW_SECONDS = 30;

   public static final String REJECTED_MESSAGE = "Bearer token rejected";

   private static final Set<JWSAlgorithm> ACCEPTED_ALGORITHMS = Set.of(
      JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512,
      JWSAlgorithm.PS256, JWSAlgorithm.PS384, JWSAlgorithm.PS512,
      JWSAlgorithm.ES256, JWSAlgorithm.ES384, JWSAlgorithm.ES512);

   //////////////////////////////////////////////////////////////////////////////
   // processors per (issuer, audiences) - each holds its own caching JWK set  //
   //////////////////////////////////////////////////////////////////////////////
   private static final Map<String, TrustedIssuer> ISSUER_CACHE = new ConcurrentHashMap<>();

   ///////////////////////////////////////////////////////////////////////////////
   // failed discovery, per base url: when it may be tried again, and how many //
   // times in a row it failed (for backoff)                                   //
   ///////////////////////////////////////////////////////////////////////////////
   private static final Map<String, long[]> DISCOVERY_FAILURES = new ConcurrentHashMap<>();
   private static final long                 MIN_RETRY_MILLIS   = 30_000L;
   private static final long                 MAX_RETRY_MILLIS   = 300_000L;

   private static Function<String, TrustedIssuer> issuerResolver = OAuth2BearerTokenVerifier::discoverIssuer;



   /***************************************************************************
    ** An issuer this app trusts: its exact `iss` value, its keys, and the client
    ** ids (audiences) accepted from it.
    ***************************************************************************/
   public record TrustedIssuer(String issuer, JWKSource<SecurityContext> keySource)
   {
   }



   /***************************************************************************
    ** An issuer the app is configured to trust: where to discover it, the
    ** issuer value its tokens (and its discovery document) must carry, and
    ** the client ids accepted from it.
    ***************************************************************************/
   public record ConfiguredIssuer(String discoveryBaseUrl, String expectedIssuer, Set<String> audiences)
   {
   }



   /***************************************************************************
    ** The issuers an app trusts: its own (baseUrl, whose public issuer is
    ** externalBaseUrl when set) and, when set, its native client's.
    ***************************************************************************/
   public static List<ConfiguredIssuer> getConfiguredIssuers(OAuth2AuthenticationMetaData metaData)
   {
      List<ConfiguredIssuer> rs = new ArrayList<>();
      for(Map.Entry<String, Set<String>> entry : getTrustedIssuerAudiences(metaData).entrySet())
      {
         String expected = entry.getKey();
         if(entry.getKey().equals(metaData.getBaseUrl()) && StringUtils.hasContent(metaData.getExternalBaseUrl()))
         {
            expected = metaData.getExternalBaseUrl();
         }
         rs.add(new ConfiguredIssuer(entry.getKey(), expected, entry.getValue()));
      }
      return (rs);
   }



   /***************************************************************************
    ** The (issuer discovery base -> accepted audiences) pairs for an app.
    ** Empty unless bearerTokensEnabled is true (opt-in).
    ***************************************************************************/
   public static Map<String, Set<String>> getTrustedIssuerAudiences(OAuth2AuthenticationMetaData metaData)
   {
      Map<String, Set<String>> rs = new LinkedHashMap<>();
      if(metaData == null || !Boolean.TRUE.equals(metaData.getBearerTokensEnabled()) || !StringUtils.hasContent(metaData.getBaseUrl()))
      {
         return (rs);
      }

      Set<String> appAudiences = new LinkedHashSet<>();
      if(StringUtils.hasContent(metaData.getClientId()))
      {
         appAudiences.add(metaData.getClientId());
      }
      for(String audience : CollectionUtils.nonNullList(metaData.getAdditionalBearerAudiences()))
      {
         if(StringUtils.hasContent(audience))
         {
            appAudiences.add(audience);
         }
      }
      rs.put(metaData.getBaseUrl(), appAudiences);

      if(StringUtils.hasContent(metaData.getNativeClientId()))
      {
         String nativeBase = StringUtils.hasContent(metaData.getNativeClientBaseUrl()) ? metaData.getNativeClientBaseUrl() : metaData.getBaseUrl();
         rs.computeIfAbsent(nativeBase, k -> new LinkedHashSet<>()).add(metaData.getNativeClientId());
      }
      return (rs);
   }



   /***************************************************************************
    ** Verify a bearer token; return its claims.
    **
    ** @throws QAuthenticationException (always with REJECTED_MESSAGE) when the
    ** token fails any check, or the provider's keys cannot be loaded.
    ***************************************************************************/
   public JWTClaimsSet verify(String token, OAuth2AuthenticationMetaData metaData) throws QAuthenticationException
   {
      try
      {
         if(token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH)
         {
            throw (new IllegalArgumentException("missing or oversized token"));
         }

         //////////////////////////////////////////////////////////////////////
         // only signed JWTs (JWS) - JWTParser returns PlainJWT for alg=none //
         // and EncryptedJWT for JWE; both are refused here.                 //
         //////////////////////////////////////////////////////////////////////
         JWT parsed = JWTParser.parse(token);
         if(!(parsed instanceof SignedJWT signedJWT))
         {
            throw (new IllegalArgumentException("not a signed JWT"));
         }
         if(!ACCEPTED_ALGORITHMS.contains(signedJWT.getHeader().getAlgorithm()))
         {
            throw (new IllegalArgumentException("algorithm not accepted: " + signedJWT.getHeader().getAlgorithm()));
         }

         ////////////////////////////////////////////////////////////////////
         // find the trusted issuer the (as yet unverified) iss claims; it //
         // is then re-checked as an exact-match claim after verification  //
         ////////////////////////////////////////////////////////////////////
         String                 claimedIssuer = signedJWT.getJWTClaimsSet().getIssuer();
         List<ConfiguredIssuer> configured    = getConfiguredIssuers(metaData);
         if(configured.isEmpty() || claimedIssuer == null)
         {
            throw (new IllegalArgumentException("bearer tokens not enabled, or no iss"));
         }

         ////////////////////////////////////////////////////////////////////////////
         // match the claimed issuer against configuration BEFORE any network     //
         // call, so an arbitrary token cannot make the server fetch anything     //
         ////////////////////////////////////////////////////////////////////////////
         for(ConfiguredIssuer configuredIssuer : configured)
         {
            if(!sameIssuer(configuredIssuer.expectedIssuer(), claimedIssuer))
            {
               continue;
            }
            if(configuredIssuer.audiences().isEmpty())
            {
               throw (new IllegalArgumentException("no accepted audiences"));
            }

            TrustedIssuer trustedIssuer = getTrustedIssuer(configuredIssuer);
            if(trustedIssuer == null)
            {
               throw (new IllegalArgumentException("issuer unavailable"));
            }
            return (process(signedJWT, trustedIssuer, configuredIssuer.audiences(), metaData));
         }

         throw (new IllegalArgumentException("untrusted issuer"));
      }
      catch(Exception e)
      {
         LOG.debug("Bearer token rejected", logPair("reason", e.getMessage()));
         throw (new QAuthenticationException(REJECTED_MESSAGE));
      }
   }



   /***************************************************************************
    ** Run the nimbus processor: signature (key by kid from the issuer's JWKS),
    ** algorithm allow-list, exact issuer, audience, exp/nbf, required sub.
    ***************************************************************************/
   private JWTClaimsSet process(SignedJWT signedJWT, TrustedIssuer trustedIssuer, Set<String> audiences, OAuth2AuthenticationMetaData metaData) throws Exception
   {
      DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();

      ////////////////////////////////////////////////////////////////////////////
      // access tokens are typ JWT or at+jwt (RFC 9068), or have no typ at all //
      ////////////////////////////////////////////////////////////////////////////
      processor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(JOSEObjectType.JWT, new JOSEObjectType("at+jwt"), new JOSEObjectType("at+JWT"), null));
      processor.setJWSKeySelector(new JWSVerificationKeySelector<>(ACCEPTED_ALGORITHMS, trustedIssuer.keySource()));

      ///////////////////////////////////////////////////////////////////////////
      // audiences: the token's aud must contain at least one accepted client //
      ///////////////////////////////////////////////////////////////////////////
      DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier = new DefaultJWTClaimsVerifier<>(
         new LinkedHashSet<>(audiences),
         new JWTClaimsSet.Builder().issuer(signedJWT.getJWTClaimsSet().getIssuer()).build(),
         Set.of("exp", "iss", "aud"),
         null);
      claimsVerifier.setMaxClockSkew(MAX_CLOCK_SKEW_SECONDS);
      processor.setJWTClaimsSetVerifier(claimsVerifier);

      JWTClaimsSet claims = processor.process(signedJWT, null);

      //////////////////////////////////////////////////////////////////////////
      // an access token, not an ID token: ID tokens carry nonce / at_hash, and //
      // access tokens name their client or scope                              //
      //////////////////////////////////////////////////////////////////////////
      if(claims.getClaim("nonce") != null || claims.getClaim("at_hash") != null)
      {
         throw (new IllegalArgumentException("an ID token, not an access token"));
      }
      boolean isAtJwt = signedJWT.getHeader().getType() != null && "at+jwt".equalsIgnoreCase(signedJWT.getHeader().getType().getType());
      if(!isAtJwt && claims.getClaim("scope") == null && claims.getClaim("azp") == null && claims.getClaim("client_id") == null)
      {
         throw (new IllegalArgumentException("no scope, azp or client_id"));
      }

      ///////////////////////////////
      // a bounded token lifetime  //
      ///////////////////////////////
      long    now         = Instant.now().getEpochSecond();
      long    expires     = claims.getExpirationTime().toInstant().getEpochSecond();
      long    issued      = claims.getIssueTime() == null ? now : claims.getIssueTime().toInstant().getEpochSecond();
      Integer maxLifetime = metaData.getBearerTokenMaxLifetimeSeconds();
      if(maxLifetime != null && expires - issued > maxLifetime + MAX_CLOCK_SKEW_SECONDS)
      {
         throw (new IllegalArgumentException("lifetime too long"));
      }

      if(!StringUtils.hasContent(claims.getSubject()))
      {
         if(!Boolean.TRUE.equals(metaData.getBearerMachineTokenUsernameSubject()) || !StringUtils.hasContent(claims.getStringClaim("preferred_username")))
         {
            throw (new IllegalArgumentException("no sub"));
         }
      }
      return (claims);
   }



   /***************************************************************************
    ** Issuers compare exactly, except for one trailing slash (providers differ
    ** on whether their configured base URL carries it).
    ***************************************************************************/
   static boolean sameIssuer(String expected, String claimed)
   {
      return (Objects.equals(stripSlash(expected), stripSlash(claimed)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static String stripSlash(String s)
   {
      return (s != null && s.endsWith("/") ? s.substring(0, s.length() - 1) : s);
   }



   /***************************************************************************
    ** Discovery (and the JWK source it builds) is cached per base URL.  A
    ** failed discovery - unreachable, malformed, or an issuer other than the
    ** configured one - is cached too, with backoff (30 s, doubling to 5 min).
    ***************************************************************************/
   private static TrustedIssuer getTrustedIssuer(ConfiguredIssuer configured)
   {
      String        baseUrl = configured.discoveryBaseUrl();
      TrustedIssuer cached  = ISSUER_CACHE.get(baseUrl);
      if(cached != null)
      {
         return (cached);
      }

      long[] failure = DISCOVERY_FAILURES.get(baseUrl);
      if(failure != null && System.currentTimeMillis() < failure[0])
      {
         return (null);
      }

      TrustedIssuer discovered = issuerResolver.apply(baseUrl);
      if(discovered != null && !sameIssuer(configured.expectedIssuer(), discovered.issuer()))
      {
         LOG.warn("OAuth2 discovery issuer differs from the configured issuer; bearer tokens from it are refused", logPair("configuredIssuer", configured.expectedIssuer()), logPair("discoveredIssuer", discovered.issuer()));
         discovered = null;
      }

      if(discovered != null)
      {
         ISSUER_CACHE.put(baseUrl, discovered);
         DISCOVERY_FAILURES.remove(baseUrl);
      }
      else
      {
         long failures = failure == null ? 1 : failure[1] + 1;
         long delay    = Math.min(MAX_RETRY_MILLIS, MIN_RETRY_MILLIS << Math.min(10, failures - 1));
         DISCOVERY_FAILURES.put(baseUrl, new long[] { System.currentTimeMillis() + delay, failures });
      }
      return (discovered);
   }



   /***************************************************************************
    ** OIDC discovery of the issuer and its jwks_uri; keys are cached for 15
    ** minutes, refreshed in the background, and refetched (at most every 30
    ** seconds) when a token names a key id the cached set does not have.
    ***************************************************************************/
   private static TrustedIssuer discoverIssuer(String baseUrl)
   {
      try
      {
         ////////////////////////////////////////////////////////////////////////////
         // only issuer and jwks_uri are needed, so read the discovery document   //
         // leniently (OIDC, or an RFC 8414 authorization server) - the URL comes //
         // from server configuration, never from a token                         //
         ////////////////////////////////////////////////////////////////////////////
         String     discoveryUrl = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + ".well-known/openid-configuration";
         Resource   resource     = new DefaultResourceRetriever(5_000, 5_000, 512 * 1024).retrieveResource(new URL(discoveryUrl));
         JSONObject discovery    = new JSONObject(resource.getContent());
         String     issuer       = discovery.optString("issuer", null);
         String     jwksUri      = discovery.optString("jwks_uri", null);
         if(!StringUtils.hasContent(issuer) || !StringUtils.hasContent(jwksUri))
         {
            throw (new IllegalStateException("discovery document has no issuer or jwks_uri"));
         }
         return (new TrustedIssuer(issuer, buildKeySource(new URL(jwksUri), new DefaultResourceRetriever(5_000, 5_000, 512 * 1024))));
      }
      catch(Exception e)
      {
         LOG.warn("Could not discover OAuth2 issuer for bearer-token verification", e, logPair("baseUrl", baseUrl));
         return (null);
      }
   }



   /***************************************************************************
    ** The caching, rotation-aware JWK source for a jwks_uri.
    ***************************************************************************/
   public static JWKSource<SecurityContext> buildKeySource(URL jwksUrl, ResourceRetriever retriever)
   {
      return (buildKeySource(jwksUrl, retriever, 30 * 1000L));
   }



   /***************************************************************************
    ** The caching, rotation-aware JWK source for a jwks_uri, with a given
    ** minimum interval between fetches.
    ***************************************************************************/
   public static JWKSource<SecurityContext> buildKeySource(URL jwksUrl, ResourceRetriever retriever, long minMillisBetweenFetches)
   {
      return (JWKSourceBuilder.create(jwksUrl, retriever)
         .cache(15 * 60 * 1000L, 15 * 1000L)
         .rateLimited(minMillisBetweenFetches)
         .refreshAheadCache(true)
         .retrying(false)
         .build());
   }



   /***************************************************************************
    ** For tests: replace discovery (base url -> trusted issuer), and clear the
    ** cache.  Pass null to restore OIDC discovery.
    ***************************************************************************/
   public static void setIssuerResolverForTests(Function<String, TrustedIssuer> resolver)
   {
      issuerResolver = resolver == null ? OAuth2BearerTokenVerifier::discoverIssuer : resolver;
      clearCache();
   }



   /***************************************************************************
    ** Forget discovered issuers and their cached keys.
    ***************************************************************************/
   public static void clearCache()
   {
      ISSUER_CACHE.clear();
      DISCOVERY_FAILURES.clear();
   }



   /***************************************************************************
    ** For diagnostics: the accepted audiences as a flat list.
    ***************************************************************************/
   public static List<String> getAcceptedAudiences(OAuth2AuthenticationMetaData metaData)
   {
      List<String> rs = new ArrayList<>();
      getTrustedIssuerAudiences(metaData).values().forEach(rs::addAll);
      return (rs);
   }
}
