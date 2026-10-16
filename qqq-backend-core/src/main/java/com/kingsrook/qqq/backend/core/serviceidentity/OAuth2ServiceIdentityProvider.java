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

package com.kingsrook.qqq.backend.core.serviceidentity;


import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.cluster.ClusterCaches;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule.ServiceToken;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule.ServiceTokenRefusedException;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Service principals under OAuth2 (docs/SERVICE-IDENTITY.md section 4.3).
 **
 ** The principal's service account credentials (environment only) buy a real
 ** access token with client_credentials; the session is then built by the
 ** module's ordinary bearer path (createSession with Authorization: Bearer), so
 ** OAuth2 gains no new way to accept a session: issuer, signature, audience,
 ** lifetime and the customizer's policy mapping all apply.
 **
 ** Tokens are cached per node, in memory only, for at most MAX_CACHE (and never
 ** handed out with less than MIN_REMAINING left). The cache is registered with
 ** ClusterCaches: a key-only invalidate (the principal's name, no token
 ** material) drops a principal's token on every node, e.g. after a refusal.
 ** Refusals back off (30 s doubling to 5 minutes) and raise an alert; there is
 ** never a fallback to a system session.
 *******************************************************************************/
public class OAuth2ServiceIdentityProvider implements QServiceIdentityProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(OAuth2ServiceIdentityProvider.class);

   public static final String CACHE_NAME = "serviceIdentity.tokens";

   static final Duration MAX_CACHE     = Duration.ofMinutes(5);
   static final Duration MIN_REMAINING = Duration.ofSeconds(60);
   static final Duration FIRST_BACKOFF = Duration.ofSeconds(30);
   static final Duration MAX_BACKOFF   = Duration.ofMinutes(5);

   private static final Set<String> REFUSAL_CODES = Set.of("invalid_grant", "invalid_client", "unauthorized_client", "access_denied", "invalid_scope");

   private static final Map<String, CachedToken> TOKENS   = new ConcurrentHashMap<>();
   private static final Map<String, Failure>     FAILURES = new ConcurrentHashMap<>();
   private static final Map<String, Object>      LOCKS    = new ConcurrentHashMap<>();

   //////////////////////////////////////////////////////////////////////////
   // the token each minted service session was built from (weak, by       //
   // identity), so in-process code acting as the principal toward the IdP //
   // (e.g. Authentik token exchange) can present it - see accessTokenFor  //
   //////////////////////////////////////////////////////////////////////////
   private static final Map<QSession, String> SESSION_TOKENS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

   static
   {
      ClusterCaches.shared(CACHE_NAME, TOKENS, TOKENS::clear, TOKENS::remove);
   }

   private static Function<String, String> environment = name -> new QMetaDataVariableInterpreter().interpret("${env." + name + "}");
   private static java.time.Clock          clock       = java.time.Clock.systemUTC();

   private final OAuth2AuthenticationModule module;



   /*******************************************************************************
    ** A cached token and until when it may be used.
    *******************************************************************************/
   private record CachedToken(String accessToken, Instant useUntil)
   {
      /***************************************************************************
       ** Never print the token.
       ***************************************************************************/
      @Override
      public String toString()
      {
         return ("CachedToken[useUntil=" + useUntil + "]");
      }
   }



   /*******************************************************************************
    ** A refusal and when to try again.
    *******************************************************************************/
   private record Failure(String reason, Instant retryAt, Duration backoff)
   {
   }



   /*******************************************************************************
    ** Constructor.
    *******************************************************************************/
   public OAuth2ServiceIdentityProvider(OAuth2AuthenticationModule module)
   {
      this.module = module;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QSession sessionFor(QInstance qInstance, QServicePrincipalMetaData principal) throws QAuthenticationException
   {
      if(principal == null)
      {
         throw (new QAuthenticationException("No such service principal."));
      }
      String token = currentToken(qInstance, principal);
      Map<String, String> context = new HashMap<>();
      context.put(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, token);
      QSession session = module.createSession(qInstance, context);
      SESSION_TOKENS.put(session, token);
      return (session);
   }



   /*******************************************************************************
    ** A token with at least MIN_REMAINING left, from the cache or the provider.
    *******************************************************************************/
   private String currentToken(QInstance qInstance, QServicePrincipalMetaData principal) throws QAuthenticationException
   {
      String  name = principal.getName();
      Instant now  = clock.instant();

      CachedToken cached = TOKENS.get(name);
      if(cached != null && now.isBefore(cached.useUntil()))
      {
         return (cached.accessToken());
      }

      synchronized(LOCKS.computeIfAbsent(name, k -> new Object()))
      {
         now = clock.instant();
         cached = TOKENS.get(name);
         if(cached != null && now.isBefore(cached.useUntil()))
         {
            return (cached.accessToken());
         }

         Failure failure = FAILURES.get(name);
         if(failure != null && now.isBefore(failure.retryAt()))
         {
            throw (new QAuthenticationException("Service principal " + name + " is unavailable (" + failure.reason() + "); retrying after " + failure.retryAt() + "."));
         }

         String username = credential(principal.getUsernameEnvironmentVariable());
         String secret   = credential(principal.getSecretEnvironmentVariable());
         if(username == null || secret == null)
         {
            fail(principal, "not configured", false);
            throw (new QAuthenticationException("Service principal " + name + " has no credentials (" + principal.getUsernameEnvironmentVariable() + ", " + principal.getSecretEnvironmentVariable() + ")."));
         }

         ServiceToken token;
         try
         {
            token = module.requestServiceToken(qInstance, username, secret);
         }
         catch(ServiceTokenRefusedException e)
         {
            boolean refused = REFUSAL_CODES.contains(e.getErrorCode());
            fail(principal, e.getErrorCode(), refused);
            throw (e);
         }
         catch(QAuthenticationException e)
         {
            fail(principal, "token request failed", false);
            throw (e);
         }

         FAILURES.remove(name);
         Instant  issued   = clock.instant();
         Duration lifetime = Duration.between(issued, token.expiresAt());
         Duration margin   = lifetime.dividedBy(5).compareTo(MIN_REMAINING) > 0 ? lifetime.dividedBy(5) : MIN_REMAINING;
         Instant  useUntil = token.expiresAt().minus(margin);
         if(useUntil.isAfter(issued.plus(MAX_CACHE)))
         {
            useUntil = issued.plus(MAX_CACHE);
         }
         if(!useUntil.isAfter(issued))
         {
            /////////////////////////////////////////////////////////////////
            // too short-lived to cache: use it now, ask again next time   //
            /////////////////////////////////////////////////////////////////
            TOKENS.remove(name);
            LOG.warn("Service token lifetime is too short to cache", logPair("principal", name), logPair("lifetimeSeconds", lifetime.toSeconds()));
            return (token.accessToken());
         }
         TOKENS.put(name, new CachedToken(token.accessToken(), useUntil));
         LOG.info("Service principal authenticated", logPair("principal", name), logPair("expiresAt", token.expiresAt()));
         return (token.accessToken());
      }
   }



   /*******************************************************************************
    ** Record a failure: back off, alert, and (for a refusal: revoked, rotated
    ** or deactivated) drop the principal's token on every node.
    *******************************************************************************/
   private void fail(QServicePrincipalMetaData principal, String reason, boolean refused)
   {
      String   name     = principal.getName();
      Failure  previous = FAILURES.get(name);
      Duration backoff  = previous == null ? FIRST_BACKOFF : previous.backoff().multipliedBy(2);
      if(backoff.compareTo(MAX_BACKOFF) > 0)
      {
         backoff = MAX_BACKOFF;
      }
      FAILURES.put(name, new Failure(reason, clock.instant().plus(backoff), backoff));
      TOKENS.remove(name);
      LOG.warn("Service principal unavailable", logPair("principal", name), logPair("reason", reason), logPair("backoffSeconds", backoff.toSeconds()));

      if(refused)
      {
         try
         {
            ClusterCaches.invalidateKey(CACHE_NAME, name);
         }
         catch(Exception e)
         {
            LOG.warn("Could not invalidate the service token on other nodes", e, logPair("principal", name));
         }
      }

      try
      {
         QAlerts.raise(new AlertSignal(ServiceIdentitiesMetaData.ALERT_TYPE_UNAVAILABLE)
            .withDedupKey("service_identity:" + name)
            .withSeverity(AlertSeverity.ERROR)
            .withTitle("Service identity " + principal.getLabel() + " unavailable")
            .withSummary("Background work that runs as " + principal.getPrincipalId() + " cannot authenticate (" + reason + "). It does not fall back to the system user; check the service account in the identity provider and its credentials."));
      }
      catch(Exception e)
      {
         LOG.warn("Could not raise the service identity alert", e, logPair("principal", name));
      }
   }



   /*******************************************************************************
    ** The access token a service session minted here was built from, while it
    ** is unexpired - for in-process code that must present the principal's own
    ** token to the identity provider (never to clients, never logged).
    *******************************************************************************/
   public static java.util.Optional<String> accessTokenFor(QSession session)
   {
      if(session == null || !OAuth2AuthenticationModule.isBearerSessionUnexpired(session))
      {
         return (java.util.Optional.empty());
      }
      return (java.util.Optional.ofNullable(SESSION_TOKENS.get(session)));
   }



   /*******************************************************************************
    ** A credential from the environment (never logged).
    *******************************************************************************/
   private static String credential(String variable)
   {
      String value = environment.apply(variable);
      return (StringUtils.hasContent(value) && !value.startsWith("${") ? value : null);
   }



   /*******************************************************************************
    ** Drop all cached tokens and failures on this node (tests; also used after
    ** credential rotation by an operator).
    *******************************************************************************/
   public static void clearLocal()
   {
      TOKENS.clear();
      FAILURES.clear();
   }



   /*******************************************************************************
    ** Test seam: where credentials come from.
    *******************************************************************************/
   static void setEnvironment(Function<String, String> environment)
   {
      OAuth2ServiceIdentityProvider.environment = environment;
   }



   /*******************************************************************************
    ** Test seam: the clock.
    *******************************************************************************/
   static void setClock(java.time.Clock clock)
   {
      OAuth2ServiceIdentityProvider.clock = clock;
   }
}
