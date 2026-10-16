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


import java.io.Serializable;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.cluster.ClusterCaches;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2BearerTokenVerifier;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Service identities (docs/SERVICE-IDENTITY.md): token issuance and refresh,
 ** revocation, least privilege, delegation, audit attribution, local
 ** principals, and that OAuth2 never accepts a hand-made or local session.
 *******************************************************************************/
class ServiceIdentitiesTest extends BaseTest
{
   private static final String APP_BASE   = "https://auth.example.com/application/o/app/";
   private static final String APP_CLIENT = "app";

   private static RSAKey appKey;

   private final Map<String, String> env = new HashMap<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      if(appKey == null)
      {
         appKey = new RSAKeyGenerator(2048).keyID("app-key").generate();
      }
      OAuth2BearerTokenVerifier.setIssuerResolverForTests(baseUrl -> APP_BASE.equals(baseUrl)
         ? new OAuth2BearerTokenVerifier.TrustedIssuer(APP_BASE, new ImmutableJWKSet<>(new JWKSet(appKey.toPublicJWK()))) : null);
      OAuth2ServiceIdentityProvider.clearLocal();
      OAuth2ServiceIdentityProvider.setEnvironment(env::get);
      OAuth2ServiceIdentityProvider.setClock(Clock.systemUTC());
      StubTokenModule.reset();
      env.put("QQQ_SERVICE_SCHEDULER_USERNAME", "svc-scheduler");
      env.put("QQQ_SERVICE_SCHEDULER_SECRET", "app-password");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      OAuth2BearerTokenVerifier.setIssuerResolverForTests(null);
      OAuth2ServiceIdentityProvider.clearLocal();
      OAuth2ServiceIdentityProvider.setEnvironment(name -> null);
      OAuth2ServiceIdentityProvider.setClock(Clock.systemUTC());
   }



   /*******************************************************************************
    ** OAuth2: a real client_credentials token, verified by the bearer path; the
    ** permissions are exactly what the token's groups grant.
    *******************************************************************************/
   @Test
   void oauth2SessionIsAVerifiedBearerSessionWithThePrincipalsGrants() throws Exception
   {
      useOAuth2(true);
      declare(false);
      StubTokenModule.groups = List.of("readers");

      QSession session = ServiceIdentities.session("scheduler");
      assertEquals("service:scheduler", session.getUser().getIdReference());
      assertEquals("scheduler", ServiceIdentities.principalOf(session));
      assertEquals("service", session.getClientChannel());
      assertEquals(Set.of("person.read"), session.getPermissions());
      assertFalse(session.hasPermission("person.delete"));
      assertFalse(session instanceof QSystemUserSession);
      assertTrue(OAuth2AuthenticationModule.isBearerSessionUnexpired(session));
      assertEquals("service:scheduler", ServiceIdentities.attributionName(session));

      ////////////////////////////////////////////////////////////////////////
      // the request: client_credentials, the app's client, the account's   //
      // username and secret                                                //
      ////////////////////////////////////////////////////////////////////////
      Map<String, List<String>> params = StubTokenModule.requests.get(0);
      assertEquals(List.of("client_credentials"), params.get("grant_type"));
      assertEquals(List.of(APP_CLIENT), params.get("client_id"));
      assertEquals(List.of("svc-scheduler"), params.get("username"));
      assertEquals(List.of("app-password"), params.get("password"));

      QContext.withTemporaryContext(new com.kingsrook.qqq.backend.core.context.CapturedContext(QContext.getQInstance(), session), () -> ActionHelper.validateSession(null));
      assertEquals(StubTokenModule.lastToken.get(), OAuth2ServiceIdentityProvider.accessTokenFor(session).orElseThrow());
   }



   /*******************************************************************************
    ** Security review fixes: the token must name the configured account; each
    ** call returns a fresh session (never shared through the session store);
    ** the token request has bounded timeouts; the service: namespace is
    ** refused for any session that is not a minted principal session.
    *******************************************************************************/
   @Test
   void reviewHardening() throws Exception
   {
      useOAuth2(true);
      declare(false);

      QSession first  = ServiceIdentities.session("scheduler");
      QSession second = ServiceIdentities.session("scheduler");
      assertThat(first).isNotSameAs(second);
      assertThat(StubTokenModule.timeouts).containsExactly(5_000, 10_000);

      OAuth2ServiceIdentityProvider.clearLocal();
      StubTokenModule.claimedUsername = "svc-someone-else";
      assertThrows(QAuthenticationException.class, () -> ServiceIdentities.session("scheduler"));

      QSession impostor = new QSession().withUser(new com.kingsrook.qqq.backend.core.model.session.QUser().withIdReference("service:scheduler"));
      assertFalse(ServiceIdentities.isSessionValid(QContext.getQInstance(), new com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule(), impostor));
      assertTrue(ServiceIdentities.isSessionValid(QContext.getQInstance(), new com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule(),
         new QSession().withUser(new com.kingsrook.qqq.backend.core.model.session.QUser().withIdReference("alice"))));
   }



   /*******************************************************************************
    ** A principal whose groups grant nothing gets nothing (least privilege).
    *******************************************************************************/
   @Test
   void principalWithoutGrantsHasNoPermissions() throws Exception
   {
      useOAuth2(true);
      declare(false);
      StubTokenModule.groups = List.of();
      QSession session = ServiceIdentities.session("scheduler");
      assertThat(session.getPermissions()).isNullOrEmpty();
      assertThat(session.getSecurityKeyValues()).isNullOrEmpty();
   }



   /*******************************************************************************
    ** Tokens are cached (no request per unit of work) for at most 5 minutes.
    *******************************************************************************/
   @Test
   void tokensAreCachedAndRefreshedBeforeExpiry() throws Exception
   {
      useOAuth2(true);
      declare(false);
      StubTokenModule.lifetime = Duration.ofHours(1);

      ServiceIdentities.session("scheduler");
      ServiceIdentities.session("scheduler");
      assertEquals(1, StubTokenModule.requests.size());

      OAuth2ServiceIdentityProvider.setClock(Clock.offset(Clock.systemUTC(), Duration.ofMinutes(6)));
      ServiceIdentities.session("scheduler");
      assertEquals(2, StubTokenModule.requests.size());

      ////////////////////////////////////////////////////////////////
      // a cluster invalidate (key only) drops the cached token     //
      ////////////////////////////////////////////////////////////////
      ClusterCaches.invalidateKey(OAuth2ServiceIdentityProvider.CACHE_NAME, "scheduler");
      ServiceIdentities.session("scheduler");
      assertEquals(3, StubTokenModule.requests.size());
   }



   /*******************************************************************************
    ** A refused (revoked/deactivated) account fails closed, backs off, and
    ** never yields a system session.
    *******************************************************************************/
   @Test
   void refusalFailsClosedWithBackoff() throws Exception
   {
      useOAuth2(true);
      declare(false);
      StubTokenModule.refuseWith = "invalid_grant";

      OAuth2AuthenticationModule.ServiceTokenRefusedException e = assertThrows(OAuth2AuthenticationModule.ServiceTokenRefusedException.class, () -> ServiceIdentities.session("scheduler"));
      assertEquals("invalid_grant", e.getErrorCode());
      assertThrows(QAuthenticationException.class, () -> ServiceIdentities.session("scheduler"));
      assertEquals(1, StubTokenModule.requests.size());

      ///////////////////////////////////////////////////////////
      // after the backoff it tries again, and recovers        //
      ///////////////////////////////////////////////////////////
      StubTokenModule.refuseWith = null;
      OAuth2ServiceIdentityProvider.setClock(Clock.offset(Clock.systemUTC(), Duration.ofSeconds(31)));
      assertNotNull(ServiceIdentities.session("scheduler"));
      assertEquals(2, StubTokenModule.requests.size());
   }



   /*******************************************************************************
    ** No credentials: unavailable, no request, no fallback.
    *******************************************************************************/
   @Test
   void missingCredentialsFail() throws Exception
   {
      useOAuth2(true);
      declare(false);
      env.clear();
      assertThrows(QAuthenticationException.class, () -> ServiceIdentities.session("scheduler"));
      assertThat(StubTokenModule.requests).isEmpty();
      assertThrows(QAuthenticationException.class, () -> ServiceIdentities.session("undeclared"));
   }



   /*******************************************************************************
    ** OAuth2 never accepts a local or hand-made service session, even one this
    ** JVM minted.
    *******************************************************************************/
   @Test
   void oauth2RejectsLocalAndHandMadeSessions() throws Exception
   {
      useOAuth2(true);
      QServicePrincipalMetaData principal = new QServicePrincipalMetaData().withName("scheduler").withLocalPermissions("person.read");
      QSession minted = new LocalServiceIdentityProvider().sessionFor(QContext.getQInstance(), principal);
      OAuth2AuthenticationModule module = new OAuth2AuthenticationModule();
      assertFalse(ServiceIdentities.isSessionValid(QContext.getQInstance(), module, minted));
      assertFalse(ServiceIdentities.isSessionValid(QContext.getQInstance(), module, new QServiceSession("scheduler")));
      assertFalse(ServiceIdentities.isSessionValid(QContext.getQInstance(), module, new QSession().withPermissions("person.read")));
   }



   /*******************************************************************************
    ** Local deployments (the test instance is MOCK): a QServiceSession with
    ** exactly the declared local permissions and keys - not a system session.
    *******************************************************************************/
   @Test
   void localPrincipalHasExactlyItsDeclaredGrants() throws Exception
   {
      ServiceIdentitiesMetaData.declare(QContext.getQInstance(), new QServicePrincipalMetaData().withName("scheduler")
         .withLocalPermissions("person.read").withLocalSecurityKeyValue("storeId", 1));

      QSession session = ServiceIdentities.session("scheduler");
      assertThat(session).isInstanceOf(QServiceSession.class);
      assertEquals(Set.of("person.read"), session.getPermissions());
      assertFalse(session.hasPermission("person.delete"));
      assertEquals(Map.of("storeId", List.<Serializable>of(1)), session.getSecurityKeyValues());
      assertEquals("service:scheduler", ServiceIdentities.attributionName(session));

      List<String> ran = new ArrayList<>();
      ServiceIdentities.run("scheduler", () ->
      {
         ActionHelper.validateSession(null);
         ran.add(QContext.getQSession().getUser().getIdReference());
      });
      assertEquals(List.of("service:scheduler"), ran);

      //////////////////////////////////////////////////////////////
      // a hand-made QServiceSession is never valid, even locally //
      //////////////////////////////////////////////////////////////
      QContext.setQSession(new QServiceSession("scheduler"));
      assertThrows(QAuthenticationException.class, () -> ActionHelper.validateSession(null));
   }



   /*******************************************************************************
    ** On behalf of a user: the user's current access intersected with the
    ** principal's, attributed to both; valid only while the parent is.
    *******************************************************************************/
   @Test
   void onBehalfOfIntersectsAndAttributes() throws Exception
   {
      useOAuth2(true);
      declare(true);
      StubTokenModule.groups = List.of("readers");

      //////////////////////////////////////////////////////////////////////
      // default (owner decision 2026-10-16): exactly the user's access,   //
      // even beyond the principal's own grants                            //
      //////////////////////////////////////////////////////////////////////
      QSession full = ServiceIdentities.sessionOnBehalfOf(QContext.getQInstance(), "scheduler", "alice");
      assertEquals(Set.of("person.read", "person.delete"), full.getPermissions());
      assertEquals(Set.of("person.delete"), ServiceIdentities.sessionOnBehalfOf(QContext.getQInstance(), "scheduler", "bob").getPermissions());

      ServiceIdentitiesMetaData.getPrincipal(QContext.getQInstance(), "scheduler").setDelegatedAccess(QServicePrincipalMetaData.DelegatedAccess.INTERSECT_WITH_PRINCIPAL);
      QSession alice = ServiceIdentities.sessionOnBehalfOf(QContext.getQInstance(), "scheduler", "alice");
      assertThat(alice).isInstanceOf(QDelegatedSession.class);
      assertEquals("alice", alice.getUser().getIdReference());
      assertEquals(Set.of("person.read"), alice.getPermissions());
      assertEquals("alice", alice.getValue(ServiceIdentities.VALUE_ON_BEHALF_OF));
      assertEquals("Alice via service:scheduler", ServiceIdentities.attributionName(alice));
      assertTrue(ServiceIdentities.isSessionValid(QContext.getQInstance(), new StubTokenModule(), alice));

      ///////////////////////////////////////////////////////////////////
      // a user with fewer rights than the principal keeps fewer      //
      ///////////////////////////////////////////////////////////////////
      assertThat(ServiceIdentities.sessionOnBehalfOf(QContext.getQInstance(), "scheduler", "bob").getPermissions()).isEmpty();

      ////////////////////////////////////////////////////////////////
      // an unknown/deactivated user fails closed                   //
      ////////////////////////////////////////////////////////////////
      assertThrows(QPermissionDeniedException.class, () -> ServiceIdentities.sessionOnBehalfOf(QContext.getQInstance(), "scheduler", "gone"));

      ////////////////////////////////////////////////////////////////
      // the parent's expiry ends the delegation                    //
      ////////////////////////////////////////////////////////////////
      QSession expiredParent = new QSession();
      expiredParent.setValue(OAuth2AuthenticationModule.BEARER_SESSION_VALUE_KEY, "true");
      expiredParent.setValue(OAuth2AuthenticationModule.BEARER_SESSION_EXPIRES_AT_VALUE_KEY, String.valueOf(Instant.now().minusSeconds(1).getEpochSecond()));
      assertFalse(ServiceIdentities.isSessionValid(QContext.getQInstance(), new StubTokenModule(), new QDelegatedSession(expiredParent, "scheduler")));
      assertFalse(ServiceIdentities.isSessionValid(QContext.getQInstance(), new StubTokenModule(), new QDelegatedSession(null, "scheduler")));
   }



   /*******************************************************************************
    ** Deliveries (and scheduled reports) with a declared scheduler principal run
    ** on behalf of the owner via the principal, from the system scheduler or a
    ** principal's own session; without one, behavior is unchanged.
    *******************************************************************************/
   @Test
   void deliveriesRunOnBehalfOfTheOwnerViaThePrincipal() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();

      QContext.setQSession(new QSystemUserSession(qInstance));
      QSession before = com.kingsrook.qqq.backend.core.delivery.DeliverySessions.forOwner("alice");
      assertFalse(before instanceof QDelegatedSession);

      ServiceIdentitiesMetaData.declare(qInstance, new QServicePrincipalMetaData().withName(ServiceIdentities.SCHEDULER).withMayActOnBehalfOfUsers(true));
      QSession viaSystemScheduler = com.kingsrook.qqq.backend.core.delivery.DeliverySessions.forOwner("alice");
      assertThat(viaSystemScheduler).isInstanceOf(QDelegatedSession.class);
      assertEquals("alice", viaSystemScheduler.getUser().getIdReference());
      assertEquals(ServiceIdentities.SCHEDULER, ServiceIdentities.principalOf(viaSystemScheduler));

      ServiceIdentities.run(ServiceIdentities.SCHEDULER, () ->
      {
         QSession owner = com.kingsrook.qqq.backend.core.delivery.DeliverySessions.forOwner("alice");
         assertThat(owner).isInstanceOf(QDelegatedSession.class);
         QContext.withTemporaryContext(new com.kingsrook.qqq.backend.core.context.CapturedContext(qInstance, owner), () -> ActionHelper.validateSession(null));
      });
   }



   /*******************************************************************************
    ** Each declared principal gets a health check: UP while it can
    ** authenticate, DOWN (without echoing errors) when it cannot.
    *******************************************************************************/
   @Test
   void healthCheckPerPrincipal() throws Exception
   {
      useOAuth2(true);
      declare(false);
      com.kingsrook.qqq.backend.core.health.HealthCheckMetaData check = com.kingsrook.qqq.backend.core.health.HealthChecksMetaData.ofOrWithNew(QContext.getQInstance())
         .getChecks().stream().filter(c -> c.getName().equals("service_identity.scheduler")).findFirst().orElseThrow();
      com.kingsrook.qqq.backend.core.health.HealthCheckInput input = new com.kingsrook.qqq.backend.core.health.HealthCheckInput()
         .withQInstance(QContext.getQInstance()).withDeadline(Instant.now().plusSeconds(10));
      assertEquals(com.kingsrook.qqq.backend.core.health.HealthStatus.UP, check.getCheckInstance().run(input).getStatus());

      OAuth2ServiceIdentityProvider.clearLocal();
      StubTokenModule.refuseWith = "invalid_grant";
      com.kingsrook.qqq.backend.core.health.HealthCheckOutcome down = check.getCheckInstance().run(input);
      assertEquals(com.kingsrook.qqq.backend.core.health.HealthStatus.DOWN, down.getStatus());
      assertEquals("Cannot authenticate", down.getMessage());
   }



   /*******************************************************************************
    ** Review H1: a signed-in user's scheduled reports are always their own (the
    ** owner is who they run as), and the owner never changes afterwards.
    *******************************************************************************/
   @Test
   void scheduledReportOwnerIsStampedAndImmutable() throws Exception
   {
      com.kingsrook.qqq.backend.core.model.savedreports.ScheduledReportTableCustomizer customizer = new com.kingsrook.qqq.backend.core.model.savedreports.ScheduledReportTableCustomizer();
      QContext.setQSession(new QSession().withUser(new com.kingsrook.qqq.backend.core.model.session.QUser().withIdReference("alice")));

      com.kingsrook.qqq.backend.core.model.data.QRecord inserted = new com.kingsrook.qqq.backend.core.model.data.QRecord()
         .withValue("userId", "victim").withValue("cronExpression", "0 0 9 * * ?");
      customizer.preInsert(new com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput(), List.of(inserted), false);
      assertEquals("alice", inserted.getValueString("userId"));

      com.kingsrook.qqq.backend.core.model.data.QRecord old = new com.kingsrook.qqq.backend.core.model.data.QRecord()
         .withValue("id", 1).withValue("userId", "alice").withValue("cronExpression", "0 0 9 * * ?");
      com.kingsrook.qqq.backend.core.model.data.QRecord takeover = new com.kingsrook.qqq.backend.core.model.data.QRecord().withValue("id", 1).withValue("userId", "victim");
      customizer.preUpdate(new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput(), List.of(takeover), false, java.util.Optional.of(List.of(old)));
      assertThat(takeover.getErrors()).isNotEmpty();

      com.kingsrook.qqq.backend.core.model.data.QRecord edit = new com.kingsrook.qqq.backend.core.model.data.QRecord().withValue("id", 1).withValue("subject", "Weekly");
      customizer.preUpdate(new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput(), List.of(edit), false, java.util.Optional.of(List.of(old)));
      assertThat(edit.getErrors()).isNullOrEmpty();

      QContext.setQSession(new QSession().withUser(new com.kingsrook.qqq.backend.core.model.session.QUser().withIdReference("admin"))
         .withPermission(com.kingsrook.qqq.backend.core.model.savedreports.ScheduledReportTableCustomizer.PERMISSION_ASSIGN_OWNER));
      com.kingsrook.qqq.backend.core.model.data.QRecord assigned = new com.kingsrook.qqq.backend.core.model.data.QRecord()
         .withValue("userId", "carol").withValue("cronExpression", "0 0 9 * * ?");
      customizer.preInsert(new com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput(), List.of(assigned), false);
      assertEquals("carol", assigned.getValueString("userId"), "an explicitly granted assigner may set the owner");

      QContext.setQSession(new QSystemUserSession(QContext.getQInstance()));
      com.kingsrook.qqq.backend.core.model.data.QRecord bySystem = new com.kingsrook.qqq.backend.core.model.data.QRecord()
         .withValue("userId", "bob").withValue("cronExpression", "0 0 9 * * ?");
      customizer.preInsert(new com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput(), List.of(bySystem), false);
      assertEquals("bob", bySystem.getValueString("userId"));
   }



   /*******************************************************************************
    ** Owner decision 2026-10-16: work on a user's behalf (scheduled reports,
    ** deliveries, user-owned jobs) sees and does exactly what the user can -
    ** the same rows (record locks), not fewer and not more, writes included -
    ** even when the principal itself holds nothing.
    *******************************************************************************/
   @Test
   void delegatedWorkHasExactlyTheUsersAccess() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.getAuthentication().setCustomizer(new QCodeReference(StoreUserCustomizer.class));
      ServiceIdentitiesMetaData.declare(qInstance, new QServicePrincipalMetaData().withName(ServiceIdentities.SCHEDULER).withMayActOnBehalfOfUsers(true));

      QContext.setQSession(new QSystemUserSession(qInstance));
      new com.kingsrook.qqq.backend.core.actions.tables.InsertAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput(com.kingsrook.qqq.backend.core.utils.TestUtils.TABLE_NAME_ORDER).withRecords(List.of(
         new com.kingsrook.qqq.backend.core.model.data.QRecord().withValue("orderNo", "a1").withValue("storeId", 1),
         new com.kingsrook.qqq.backend.core.model.data.QRecord().withValue("orderNo", "a2").withValue("storeId", 1),
         new com.kingsrook.qqq.backend.core.model.data.QRecord().withValue("orderNo", "b1").withValue("storeId", 2))));

      QSession userOwn = new com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule().createAutomatedSessionForUser(qInstance, "alice");
      QSession viaScheduler = ServiceIdentities.sessionOnBehalfOf(qInstance, ServiceIdentities.SCHEDULER, "alice");
      assertEquals(userOwn.getPermissions(), viaScheduler.getPermissions());
      assertEquals(userOwn.getSecurityKeyValues(), viaScheduler.getSecurityKeyValues());
      assertEquals("alice via service:scheduler", ServiceIdentities.attributionName(viaScheduler));

      List<String> seen = new ArrayList<>();
      QContext.withTemporaryContext(new com.kingsrook.qqq.backend.core.context.CapturedContext(qInstance, viaScheduler), () ->
      {
         for(com.kingsrook.qqq.backend.core.model.data.QRecord record : new com.kingsrook.qqq.backend.core.actions.tables.QueryAction().execute(
            new com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput(com.kingsrook.qqq.backend.core.utils.TestUtils.TABLE_NAME_ORDER)).getRecords())
         {
            seen.add(record.getValueString("orderNo"));
         }
         //////////////////////////////////////////
         // writes included, as the user could   //
         //////////////////////////////////////////
         new com.kingsrook.qqq.backend.core.actions.tables.InsertAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput(com.kingsrook.qqq.backend.core.utils.TestUtils.TABLE_NAME_ORDER)
            .withRecord(new com.kingsrook.qqq.backend.core.model.data.QRecord().withValue("orderNo", "a3").withValue("storeId", 1)));
      });
      assertThat(seen).containsExactlyInAnyOrder("a1", "a2");

      QContext.setQSession(new QSystemUserSession(qInstance));
      assertEquals(4, new com.kingsrook.qqq.backend.core.actions.tables.CountAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput(com.kingsrook.qqq.backend.core.utils.TestUtils.TABLE_NAME_ORDER)).getCount());
   }



   /*******************************************************************************
    ** Grants per user for automated sessions: alice store 1, bob store 2.
    *******************************************************************************/
   public static class StoreUserCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void customizeAutomatedSessionForUser(QInstance qInstance, QSession session, Serializable userId)
      {
         session.withPermissions("order.read", "order.insert", "order.edit");
         session.withSecurityKeyValue(com.kingsrook.qqq.backend.core.utils.TestUtils.SECURITY_KEY_TYPE_STORE, "alice".equals(userId) ? 1 : 2);
      }
   }



   /*******************************************************************************
    ** Only principals declared for it may act on behalf of users.
    *******************************************************************************/
   @Test
   void delegationNeedsDeclaration() throws Exception
   {
      useOAuth2(true);
      declare(false);
      assertThrows(QPermissionDeniedException.class, () -> ServiceIdentities.sessionOnBehalfOf(QContext.getQInstance(), "scheduler", "alice"));
      ServiceIdentitiesMetaData.declare(QContext.getQInstance(), new QServicePrincipalMetaData().withName("helper").withMayActOnBehalfOfUsers(true));
      assertThrows(QPermissionDeniedException.class, () -> ServiceIdentities.sessionOnBehalfOf(QContext.getQInstance(), "helper", "service:scheduler"));
   }



   /*******************************************************************************
    ** Validation: names, duplicates, and OAuth2 needs bearer tokens enabled.
    *******************************************************************************/
   @Test
   void validation() throws Exception
   {
      useOAuth2(false);
      QInstance qInstance = QContext.getQInstance();
      ServiceIdentitiesMetaData metaData = ServiceIdentitiesMetaData.ofOrWithNew(qInstance)
         .withPrincipal(new QServicePrincipalMetaData().withName("Bad Name"))
         .withPrincipal(new QServicePrincipalMetaData().withName("scheduler"))
         .withPrincipal(new QServicePrincipalMetaData().withName("scheduler"));
      QInstanceValidator validator = new QInstanceValidator();
      metaData.validate(qInstance, validator);
      assertThat(validator.getErrors()).anyMatch(m -> m.contains("Bad Name"))
         .anyMatch(m -> m.contains("declared more than once"))
         .anyMatch(m -> m.contains("bearerTokensEnabled"));
      assertEquals("QQQ_SERVICE_SEARCH_INDEXER_SECRET", new QServicePrincipalMetaData().withName("search-indexer").getSecretEnvironmentVariable());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void declare(boolean mayActOnBehalf)
   {
      ServiceIdentitiesMetaData.declare(QContext.getQInstance(), new QServicePrincipalMetaData().withName("scheduler").withLabel("Scheduler")
         .withMayActOnBehalfOfUsers(mayActOnBehalf));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void useOAuth2(boolean bearerTokensEnabled)
   {
      OAuth2AuthenticationMetaData metaData = new OAuth2AuthenticationMetaData();
      metaData.setName("oauth2");
      metaData.setBaseUrl(APP_BASE);
      metaData.setClientId(APP_CLIENT);
      metaData.setClientSecret("secret");
      metaData.setScopes("openid profile");
      metaData.setBearerTokensEnabled(bearerTokensEnabled);
      metaData.setCustomizer(new QCodeReference(TestCustomizer.class));
      metaData.setModuleCodeReference(new QCodeReference(StubTokenModule.class));
      QContext.getQInstance().registerAuthenticationProvider(AuthScope.instanceDefault(), metaData);
   }



   /*******************************************************************************
    ** OAuth2 module whose token endpoint is a stub that signs real JWTs with the
    ** trusted key (so the full bearer verification runs).
    *******************************************************************************/
   public static class StubTokenModule extends OAuth2AuthenticationModule
   {
      static List<Map<String, List<String>>> requests  = new ArrayList<>();
      static List<String>                    groups    = List.of("readers");
      static Duration                        lifetime  = Duration.ofMinutes(5);
      static String                          refuseWith;
      static AtomicReference<String>         lastToken = new AtomicReference<>();
      static String                          claimedUsername;
      static List<Integer>                   timeouts  = new ArrayList<>();



      /***************************************************************************
       **
       ***************************************************************************/
      static void reset()
      {
         requests = new ArrayList<>();
         groups = List.of("readers");
         lifetime = Duration.ofMinutes(5);
         refuseWith = null;
         lastToken.set(null);
         claimedUsername = null;
         timeouts = new ArrayList<>();
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected URI getServiceTokenEndpoint(OAuth2AuthenticationMetaData oauth2MetaData)
      {
         return (URI.create(APP_BASE + "token/"));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected TokenResponse sendServiceTokenRequest(HTTPRequest httpRequest) throws Exception
      {
         Map<String, List<String>> params = new HashMap<>(httpRequest.getBodyAsFormParameters());
         requests.add(params);
         timeouts.add(httpRequest.getConnectTimeout());
         timeouts.add(httpRequest.getReadTimeout());
         assertFalse(httpRequest.getFollowRedirects());
         HTTPResponse response;
         if(refuseWith != null)
         {
            response = new HTTPResponse(400);
            response.setContentType("application/json");
            response.setContent(new JSONObject().put("error", refuseWith).toString());
         }
         else
         {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
               .issuer(APP_BASE).audience(APP_CLIENT).subject("svc-sub-" + requests.size())
               .claim("preferred_username", claimedUsername != null ? claimedUsername : params.get("username").get(0))
               .claim("groups", groups).claim("scope", "openid profile")
               .issueTime(Date.from(now)).expirationTime(Date.from(now.plus(lifetime)))
               .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(appKey.getKeyID()).type(JOSEObjectType.JWT).build(), claims);
            jwt.sign(new RSASSASigner(appKey));
            lastToken.set(jwt.serialize());
            response = new HTTPResponse(200);
            response.setContentType("application/json");
            response.setContent(new JSONObject().put("access_token", jwt.serialize()).put("token_type", "Bearer").put("expires_in", lifetime.toSeconds()).toString());
         }
         return (TokenResponse.parse(response));
      }
   }



   /*******************************************************************************
    ** Grants from the token's groups claim (as an IdP policy mapper would), and
    ** per-user grants for automated sessions (as the Authentik mapper would).
    *******************************************************************************/
   public static class TestCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
      {
         JSONObject payload = (JSONObject) context.get("jwtPayloadJsonObject");
         if(payload != null && payload.optJSONArray("groups") != null && payload.getJSONArray("groups").toList().contains("readers"))
         {
            qSession.setPermissions(Set.of("person.read"));
         }
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void customizeAutomatedSessionForUser(QInstance qInstance, QSession session, Serializable userId) throws QAuthenticationException
      {
         switch(String.valueOf(userId))
         {
            case "alice" ->
            {
               session.getUser().setFullName("Alice");
               session.setPermissions(Set.of("person.read", "person.delete"));
            }
            case "bob" -> session.setPermissions(Set.of("person.delete"));
            default -> throw (new QAuthenticationException("No such active user"));
         }
      }
   }
}
