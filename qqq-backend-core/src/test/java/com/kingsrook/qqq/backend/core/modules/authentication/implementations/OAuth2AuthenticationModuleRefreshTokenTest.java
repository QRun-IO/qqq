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


import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.CollectedLogMessage;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionRefreshTokenMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSessionRefreshToken;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for OAuth2AuthenticationModule refresh-token support (#1041), against
 ** a WireMock OIDC provider (discovery, token and revocation endpoints).
 *******************************************************************************/
class OAuth2AuthenticationModuleRefreshTokenTest extends BaseTest
{
   private static final String BACKEND_NAME         = "memory";
   private static final String REDIRECT_STATE_TABLE = "oauthRedirectState";
   private static final String TOKEN_PATH           = "/oauth/token";
   private static final String REVOKE_PATH          = "/oauth/revoke";
   private static final String USER_ID              = "refresh-user@example.com";

   //////////////////////////////////////////////////////////////////
   // fixed test keys (base64 of 32 bytes) - for tests only        //
   //////////////////////////////////////////////////////////////////
   private static final String TEST_KEY  = Base64.getEncoder().encodeToString("test-key-0123456789-0123456789-0".getBytes(StandardCharsets.UTF_8));
   private static final String OTHER_KEY = Base64.getEncoder().encodeToString("other-key-012345678-0123456789-0".getBytes(StandardCharsets.UTF_8));

   private WireMockServer wireMockServer;
   private QInstance      qInstance;



   /*******************************************************************************
    ** Start WireMock and build an OAuth2 instance with refresh-token storage.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
      wireMockServer.start();
      OAuth2AuthenticationModule.clearOIDCProviderMetadataCache();

      qInstance = buildQInstance(true);
      QContext.init(qInstance, new QSystemUserSession());
      stubOidcDiscovery(true);
   }



   /*******************************************************************************
    ** Stop WireMock and restore the module's logger.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QLogger.deactivateCollectingLoggerForClass(OAuth2AuthenticationModule.class);
      if(wireMockServer != null)
      {
         wireMockServer.stop();
      }
   }



   /*******************************************************************************
    ** A refresh token from the code exchange is stored server-side - and never
    ** appears in the session (values, frontend values, JSON) or in the logs.
    *******************************************************************************/
   @Test
   void testCodeExchange_storesRefreshTokenServerSideOnly() throws Exception
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(OAuth2AuthenticationModule.class);

      String refreshToken = "initial-refresh-" + UUID.randomUUID();
      stubTokenEndpoint("authorization_code", 200, tokenResponse(createJwt(3600), refreshToken).put("refresh_expires_in", 7200));

      QSession session = new OAuth2AuthenticationModule().createSession(qInstance, pkceContext());

      QRecord stored = getRefreshTokenRecord(session.getUuid());
      assertNotNull(stored);

      ////////////////////////////////////////////////////////////////////
      // stored encrypted (#1048) - not the token, but decrypts to it   //
      ////////////////////////////////////////////////////////////////////
      String storedValue = stored.getValueString("refreshToken");
      assertThat(storedValue).startsWith("v1:" + RefreshTokenCipher.DEFAULT_KEY_ID + ":").doesNotContain(refreshToken);
      assertEquals(refreshToken, getDecryptedRefreshToken(session.getUuid()));
      assertNotNull(stored.getValueInstant("sessionStartedAt"));
      assertNotNull(stored.getValueInstant("refreshTokenExpiresAt"));

      assertThat(JsonUtils.toJson(session)).doesNotContain(refreshToken);
      assertThat(String.valueOf(session.getValuesForFrontend())).doesNotContain(refreshToken);
      assertThat(String.valueOf(session.getValues())).doesNotContain(refreshToken);
      assertNoLogContains(collectingLogger, refreshToken);

      ///////////////////////////////////////////////////////////////////
      // the refresh token field is hidden: standard queries omit it   //
      ///////////////////////////////////////////////////////////////////
      List<QRecord> queried = new QueryAction().execute(new QueryInput(UserSessionRefreshToken.TABLE_NAME)).getRecords();
      assertEquals(1, queried.size());
      assertNull(queried.get(0).getValue("refreshToken"));
   }



   /*******************************************************************************
    ** Without the refresh-token table, a returned refresh token is simply not
    ** stored, and login still works (existing apps need no schema change).
    *******************************************************************************/
   @Test
   void testCodeExchange_noRefreshTokenTable_loginStillWorks() throws Exception
   {
      qInstance = buildQInstance(false);
      QContext.init(qInstance, new QSystemUserSession());

      stubTokenEndpoint("authorization_code", 200, tokenResponse(createJwt(3600), "unused-refresh"));
      QSession session = new OAuth2AuthenticationModule().createSession(qInstance, pkceContext());
      assertEquals(USER_ID, session.getUser().getIdReference());
   }



   /*******************************************************************************
    ** Refresh disabled in meta-data: the refresh token is not stored.
    *******************************************************************************/
   @Test
   void testCodeExchange_refreshDisabled_notStored() throws Exception
   {
      getOAuth2MetaData().setRefreshTokensEnabled(false);
      stubTokenEndpoint("authorization_code", 200, tokenResponse(createJwt(3600), "unused-refresh"));
      QSession session = new OAuth2AuthenticationModule().createSession(qInstance, pkceContext());
      assertNull(getRefreshTokenRecord(session.getUuid()));
   }



   /*******************************************************************************
    ** Resuming a session whose access token has expired refreshes it, and the
    ** rotated tokens are persisted.
    *******************************************************************************/
   @Test
   void testResume_expiredAccessToken_refreshesAndPersistsRotation() throws Exception
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(OAuth2AuthenticationModule.class);

      String sessionUuid = UUID.randomUUID().toString();
      String oldRefresh  = "old-refresh-" + UUID.randomUUID();
      String newRefresh  = "new-refresh-" + UUID.randomUUID();
      String newAccess   = createJwt(3600);
      insertSession(sessionUuid, createJwt(-60), oldRefresh, Instant.now());
      stubTokenEndpoint("refresh_token", 200, tokenResponse(newAccess, newRefresh));

      QSession session = new OAuth2AuthenticationModule().createSession(qInstance, Map.of("sessionUUID", sessionUuid));
      assertEquals(USER_ID, session.getUser().getIdReference());
      assertEquals(sessionUuid, session.getUuid());

      assertEquals(newAccess, getUserSessionRecord(sessionUuid).getValueString("accessToken"));
      assertEquals(newRefresh, getDecryptedRefreshToken(sessionUuid));
      assertTrue(new OAuth2AuthenticationModule().isSessionValid(qInstance, session));

      wireMockServer.verify(1, postRequestedFor(urlEqualTo(TOKEN_PATH))
         .withRequestBody(containing("grant_type=refresh_token"))
         .withRequestBody(containing("refresh_token=" + oldRefresh)));

      assertNoLogContains(collectingLogger, oldRefresh);
      assertNoLogContains(collectingLogger, newRefresh);
      assertThat(JsonUtils.toJson(session)).doesNotContain(newRefresh);
   }



   /*******************************************************************************
    ** isSessionValid refreshes an access token that is within the skew window,
    ** and a provider that doesn't rotate leaves the stored refresh token as is.
    *******************************************************************************/
   @Test
   void testIsSessionValid_nearExpiry_refreshesWithoutRotation() throws Exception
   {
      String sessionUuid = UUID.randomUUID().toString();
      String refresh     = "stable-refresh-" + UUID.randomUUID();
      String newAccess   = createJwt(3600);
      insertSession(sessionUuid, createJwt(30), refresh, Instant.now());
      stubTokenEndpoint("refresh_token", 200, tokenResponse(newAccess, null));

      assertTrue(new OAuth2AuthenticationModule().isSessionValid(qInstance, sessionWithUuid(sessionUuid)));
      assertEquals(newAccess, getUserSessionRecord(sessionUuid).getValueString("accessToken"));
      assertEquals(refresh, getDecryptedRefreshToken(sessionUuid));
   }



   /*******************************************************************************
    ** invalid_grant (expired/revoked refresh token) ends the session cleanly.
    *******************************************************************************/
   @Test
   void testInvalidGrant_endsSession() throws Exception
   {
      String sessionUuid = UUID.randomUUID().toString();
      insertSession(sessionUuid, createJwt(-60), "revoked-refresh", Instant.now());
      stubTokenEndpoint("refresh_token", 400, new JSONObject().put("error", "invalid_grant").put("error_description", "Token is not active"));

      OAuth2AuthenticationModule module = new OAuth2AuthenticationModule();
      assertFalse(module.isSessionValid(qInstance, sessionWithUuid(sessionUuid)));
      assertNull(getUserSessionRecord(sessionUuid));
      assertNull(getRefreshTokenRecord(sessionUuid));

      assertThrows(QAuthenticationException.class, () -> module.createSession(qInstance, Map.of("sessionUUID", sessionUuid)));
   }



   /*******************************************************************************
    ** A transient provider failure doesn't destroy the session's refresh token,
    ** so a later request can still refresh.
    *******************************************************************************/
   @Test
   void testTransientFailure_keepsRefreshToken() throws Exception
   {
      String sessionUuid = UUID.randomUUID().toString();
      insertSession(sessionUuid, createJwt(-60), "keep-me", Instant.now());
      stubTokenEndpoint("refresh_token", 503, new JSONObject().put("error", "temporarily_unavailable"));

      assertFalse(new OAuth2AuthenticationModule().isSessionValid(qInstance, sessionWithUuid(sessionUuid)));
      assertNotNull(getUserSessionRecord(sessionUuid));
      assertEquals("keep-me", getDecryptedRefreshToken(sessionUuid));
   }



   /*******************************************************************************
    ** Concurrent requests at expiry trigger exactly one refresh.
    *******************************************************************************/
   @Test
   void testConcurrentRequests_refreshExactlyOnce() throws Exception
   {
      String sessionUuid = UUID.randomUUID().toString();
      String newAccess   = createJwt(3600);
      insertSession(sessionUuid, createJwt(-60), "concurrent-refresh", Instant.now());
      wireMockServer.stubFor(post(urlEqualTo(TOKEN_PATH))
         .withRequestBody(containing("grant_type=refresh_token"))
         .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withFixedDelay(300)
            .withBody(tokenResponse(newAccess, "rotated-concurrent").toString())));

      int             threadCount = 8;
      ExecutorService executor    = Executors.newFixedThreadPool(threadCount);
      try
      {
         CountDownLatch        startLatch = new CountDownLatch(1);
         List<Future<Boolean>> futures    = new ArrayList<>();
         for(int i = 0; i < threadCount; i++)
         {
            Callable<Boolean> callable = () ->
            {
               QContext.init(qInstance, new QSystemUserSession());
               try
               {
                  startLatch.await();
                  return (new OAuth2AuthenticationModule().isSessionValid(qInstance, sessionWithUuid(sessionUuid)));
               }
               finally
               {
                  QContext.clear();
               }
            };
            futures.add(executor.submit(callable));
         }

         startLatch.countDown();
         for(Future<Boolean> future : futures)
         {
            assertTrue(future.get(30, TimeUnit.SECONDS));
         }
      }
      finally
      {
         executor.shutdownNow();
      }

      wireMockServer.verify(1, postRequestedFor(urlEqualTo(TOKEN_PATH)));
      assertEquals(newAccess, getUserSessionRecord(sessionUuid).getValueString("accessToken"));
      assertEquals("rotated-concurrent", getDecryptedRefreshToken(sessionUuid));
   }



   /*******************************************************************************
    ** A subclass that overrides createSession / isSessionValid (delegating to
    ** super, as apps do) gets refreshing without any changes.
    *******************************************************************************/
   @Test
   void testSubclassDelegatingToSuper_refreshes() throws Exception
   {
      String sessionUuid = UUID.randomUUID().toString();
      String newAccess   = createJwt(3600);
      insertSession(sessionUuid, createJwt(-60), "subclass-refresh", Instant.now());
      stubTokenEndpoint("refresh_token", 200, tokenResponse(newAccess, "subclass-rotated"));

      SubclassedOAuth2AuthenticationModule module = new SubclassedOAuth2AuthenticationModule();
      assertTrue(module.isSessionValid(qInstance, sessionWithUuid(sessionUuid)));
      QSession session = module.createSession(qInstance, Map.of("sessionUUID", sessionUuid));
      assertEquals("true", session.getValue("fromSubclass"));
      assertEquals("subclass-rotated", getDecryptedRefreshToken(sessionUuid));
   }



   /*******************************************************************************
    ** With refresh disabled, an expired access token is not refreshed.
    *******************************************************************************/
   @Test
   void testRefreshDisabled_expiredSessionNotRefreshed() throws Exception
   {
      getOAuth2MetaData().setRefreshTokensEnabled(false);
      String sessionUuid = UUID.randomUUID().toString();
      insertSession(sessionUuid, createJwt(-60), "never-used", Instant.now());
      stubTokenEndpoint("refresh_token", 200, tokenResponse(createJwt(3600), null));

      assertFalse(new OAuth2AuthenticationModule().isSessionValid(qInstance, sessionWithUuid(sessionUuid)));
      wireMockServer.verify(0, postRequestedFor(urlEqualTo(TOKEN_PATH)));
   }



   /*******************************************************************************
    ** Past maxSessionLifetimeSeconds, the session isn't refreshed, and its
    ** refresh token is revoked and removed.
    *******************************************************************************/
   @Test
   void testMaxSessionLifetime_stopsRefreshing() throws Exception
   {
      getOAuth2MetaData().setMaxSessionLifetimeSeconds(3600);
      stubRevocationEndpoint();
      String sessionUuid = UUID.randomUUID().toString();
      insertSession(sessionUuid, createJwt(-60), "too-old", Instant.now().minusSeconds(7200));
      stubTokenEndpoint("refresh_token", 200, tokenResponse(createJwt(3600), null));

      assertFalse(new OAuth2AuthenticationModule().isSessionValid(qInstance, sessionWithUuid(sessionUuid)));
      wireMockServer.verify(0, postRequestedFor(urlEqualTo(TOKEN_PATH)));
      wireMockServer.verify(1, postRequestedFor(urlEqualTo(REVOKE_PATH)).withRequestBody(containing("token=too-old")));
      assertNull(getRefreshTokenRecord(sessionUuid));
   }



   /*******************************************************************************
    ** A refresh token known (from refresh_expires_in) to be expired isn't used.
    *******************************************************************************/
   @Test
   void testExpiredRefreshToken_notUsed() throws Exception
   {
      String sessionUuid = UUID.randomUUID().toString();
      insertSession(sessionUuid, createJwt(-60), "expired-refresh", Instant.now());
      new UpdateAction().execute(new UpdateInput(UserSessionRefreshToken.TABLE_NAME)
         .withRecord(new QRecord().withValue("id", getRefreshTokenRecord(sessionUuid).getValue("id")).withValue("refreshTokenExpiresAt", Instant.now().minusSeconds(10))));

      assertFalse(new OAuth2AuthenticationModule().isSessionValid(qInstance, sessionWithUuid(sessionUuid)));
      wireMockServer.verify(0, postRequestedFor(urlEqualTo(TOKEN_PATH)));
      assertNull(getRefreshTokenRecord(sessionUuid));
   }



   /*******************************************************************************
    ** Logout revokes (via the advertised revocation endpoint) and deletes the
    ** stored refresh token.
    *******************************************************************************/
   @Test
   void testLogout_revokesAndDeletesRefreshToken() throws Exception
   {
      stubRevocationEndpoint();
      String sessionUuid = UUID.randomUUID().toString();
      insertSession(sessionUuid, createJwt(3600), "logout-refresh", Instant.now());

      new OAuth2AuthenticationModule().logout(qInstance, sessionUuid);

      wireMockServer.verify(1, postRequestedFor(urlEqualTo(REVOKE_PATH))
         .withRequestBody(containing("token=logout-refresh"))
         .withRequestBody(containing("token_type_hint=refresh_token")));
      assertNull(getRefreshTokenRecord(sessionUuid));
      assertNull(getUserSessionRecord(sessionUuid));
   }



   /*******************************************************************************
    ** Logout without an advertised revocation endpoint still deletes the token;
    ** and a failing revocation doesn't prevent deletion.
    *******************************************************************************/
   @Test
   void testLogout_noRevocationEndpoint_deletesRefreshToken() throws Exception
   {
      wireMockServer.resetAll();
      OAuth2AuthenticationModule.clearOIDCProviderMetadataCache();
      stubOidcDiscovery(false);

      String sessionUuid = UUID.randomUUID().toString();
      insertSession(sessionUuid, createJwt(3600), "no-revoke", Instant.now());

      new OAuth2AuthenticationModule().logout(qInstance, sessionUuid);
      assertNull(getRefreshTokenRecord(sessionUuid));
      wireMockServer.verify(0, postRequestedFor(urlEqualTo(REVOKE_PATH)));
   }



   /*******************************************************************************
    ** The static helper used by back-channel logout deletes stored tokens, and
    ** is a no-op for instances without the refresh-token table.
    *******************************************************************************/
   @Test
   void testDeleteStoredRefreshTokens() throws Exception
   {
      String sessionUuid = UUID.randomUUID().toString();
      insertSession(sessionUuid, createJwt(3600), "backchannel-refresh", Instant.now());

      OAuth2AuthenticationModule.deleteStoredRefreshTokens(qInstance, List.of(sessionUuid));
      assertNull(getRefreshTokenRecord(sessionUuid));
      assertNotNull(getUserSessionRecord(sessionUuid));

      OAuth2AuthenticationModule.deleteStoredRefreshTokens(buildQInstance(false), List.of(sessionUuid));
      OAuth2AuthenticationModule.deleteStoredRefreshTokens(qInstance, List.of());
   }



   /*******************************************************************************
    ** Meta-data validation of the new refresh settings.
    *******************************************************************************/
   @Test
   void testMetaDataValidation()
   {
      OAuth2AuthenticationMetaData metaData = getOAuth2MetaData()
         .withRefreshTokenSkewSeconds(-1)
         .withMaxSessionLifetimeSeconds(0)
         .withRefreshTokenTableName("notATable");

      QInstanceValidator validator = new QInstanceValidator();
      metaData.validate(qInstance, validator);
      assertThat(validator.getErrors()).anyMatch(e -> e.contains("refreshTokenSkewSeconds"));
      assertThat(validator.getErrors()).anyMatch(e -> e.contains("maxSessionLifetimeSeconds"));
      assertThat(validator.getErrors()).anyMatch(e -> e.contains("notATable"));

      assertEquals(-1, metaData.getRefreshTokenSkewSeconds());
      assertEquals(0, metaData.getMaxSessionLifetimeSeconds());
      assertEquals("notATable", metaData.getRefreshTokenTableName());
      assertTrue(metaData.withRefreshTokensEnabled(true).getRefreshTokensEnabled());
   }



   /*******************************************************************************
    ** A stored value encrypted under a different key (with the same key id)
    ** does not decrypt: the session ends cleanly, without calling the provider,
    ** and the log says why - without any token material.
    *******************************************************************************/
   @Test
   void testDecryptFailure_wrongKey_endsSession() throws Exception
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(OAuth2AuthenticationModule.class);

      String sessionUuid = UUID.randomUUID().toString();
      String refresh     = "wrong-key-refresh-" + UUID.randomUUID();
      String storedValue = encryptForSession(new OAuth2AuthenticationMetaData().withRefreshTokenEncryptionKey(OTHER_KEY), refresh, sessionUuid);
      insertUserSessionRow(sessionUuid, createJwt(-60));
      insertRefreshTokenRow(sessionUuid, storedValue, Instant.now());
      stubTokenEndpoint("refresh_token", 200, tokenResponse(createJwt(3600), null));

      assertSessionEndedWithoutRefresh(sessionUuid);
      assertThat(collectingLogger.getCollectedMessages()).anyMatch(m -> m.getMessage().contains("could not be decrypted") && m.getMessage().contains("authenticationFailed") && m.getMessage().contains(sessionUuid));
      assertNoLogContains(collectingLogger, refresh);
      assertNoLogContains(collectingLogger, storedValue);
   }



   /*******************************************************************************
    ** A ciphertext copied from another session's row fails to decrypt (the AAD
    ** binds it to its session), ending the session it was copied to - while the
    ** original session still refreshes normally.
    *******************************************************************************/
   @Test
   void testDecryptFailure_ciphertextFromAnotherSession_endsSession() throws Exception
   {
      String victimUuid   = UUID.randomUUID().toString();
      String attackerUuid = UUID.randomUUID().toString();
      insertSession(victimUuid, createJwt(-60), "victim-refresh", Instant.now());
      insertUserSessionRow(attackerUuid, createJwt(-60));
      insertRefreshTokenRow(attackerUuid, getRefreshTokenRecord(victimUuid).getValueString("refreshToken"), Instant.now());
      stubTokenEndpoint("refresh_token", 200, tokenResponse(createJwt(3600), null));

      assertSessionEndedWithoutRefresh(attackerUuid);

      assertTrue(new OAuth2AuthenticationModule().isSessionValid(qInstance, sessionWithUuid(victimUuid)));
      wireMockServer.verify(1, postRequestedFor(urlEqualTo(TOKEN_PATH)).withRequestBody(containing("refresh_token=victim-refresh")));
   }



   /*******************************************************************************
    ** A stored value whose userId binding doesn't match the userSession row
    ** (e.g., the row's userId was changed) does not decrypt.
    *******************************************************************************/
   @Test
   void testDecryptFailure_otherUserId_endsSession() throws Exception
   {
      String sessionUuid = UUID.randomUUID().toString();
      String storedValue = RefreshTokenCipher.fromMetaData(getOAuth2MetaData()).encrypt("other-user-refresh", RefreshTokenCipher.buildAssociatedData(sessionUuid, "someone-else@example.com"));
      insertUserSessionRow(sessionUuid, createJwt(-60));
      insertRefreshTokenRow(sessionUuid, storedValue, Instant.now());

      assertSessionEndedWithoutRefresh(sessionUuid);
   }



   /*******************************************************************************
    ** A tampered ciphertext, an unknown key id, and a legacy plaintext value all
    ** fail to decrypt, and end the session cleanly.
    *******************************************************************************/
   @Test
   void testDecryptFailure_tamperedOrUnrecognized_endsSession() throws Exception
   {
      String   validUuid = UUID.randomUUID().toString();
      insertSession(validUuid, createJwt(3600), "tamper-me", Instant.now());
      String[] parts     = getRefreshTokenRecord(validUuid).getValueString("refreshToken").split(":", 3);
      byte[]   payload   = Base64.getDecoder().decode(parts[2]);
      payload[payload.length - 1] ^= 0x01;
      String tampered = parts[0] + ":" + parts[1] + ":" + Base64.getEncoder().encodeToString(payload);

      for(String storedValue : List.of(tampered, "v1:unknownKeyId:" + parts[2], "legacy-plaintext-refresh-token", "v1:" + RefreshTokenCipher.DEFAULT_KEY_ID + ":not*base64", "v1:" + RefreshTokenCipher.DEFAULT_KEY_ID + ":AAAA"))
      {
         String sessionUuid = UUID.randomUUID().toString();
         insertUserSessionRow(sessionUuid, createJwt(-60));
         insertRefreshTokenRow(sessionUuid, storedValue, Instant.now());
         assertSessionEndedWithoutRefresh(sessionUuid);
      }
   }



   /*******************************************************************************
    ** Key rotation: with a new current key and the old one listed as previous,
    ** values stored under the old key still work, and are re-encrypted under the
    ** new key on their next write (even when the provider doesn't rotate).
    *******************************************************************************/
   @Test
   void testKeyRotation_oldKeyReadableAndReEncrypted() throws Exception
   {
      String sessionUuid = UUID.randomUUID().toString();
      String refresh     = "pre-rotation-refresh-" + UUID.randomUUID();
      String oldValue    = encryptForSession(new OAuth2AuthenticationMetaData().withRefreshTokenEncryptionKey(OTHER_KEY).withRefreshTokenEncryptionKeyId("2025"), refresh, sessionUuid);
      insertUserSessionRow(sessionUuid, createJwt(-60));
      insertRefreshTokenRow(sessionUuid, oldValue, Instant.now());

      getOAuth2MetaData()
         .withRefreshTokenEncryptionKey(TEST_KEY)
         .withRefreshTokenEncryptionKeyId("2026")
         .withPreviousRefreshTokenEncryptionKeys(Map.of("2025", OTHER_KEY));
      stubTokenEndpoint("refresh_token", 200, tokenResponse(createJwt(3600), null));

      assertTrue(new OAuth2AuthenticationModule().isSessionValid(qInstance, sessionWithUuid(sessionUuid)));
      wireMockServer.verify(1, postRequestedFor(urlEqualTo(TOKEN_PATH)).withRequestBody(containing("refresh_token=" + refresh)));

      String newValue = getRefreshTokenRecord(sessionUuid).getValueString("refreshToken");
      assertThat(newValue).startsWith("v1:2026:");
      assertEquals(refresh, getDecryptedRefreshToken(sessionUuid));

      ///////////////////////////////////////////////////////////////////
      // once the old key is dropped, the re-encrypted value still works //
      ///////////////////////////////////////////////////////////////////
      getOAuth2MetaData().setPreviousRefreshTokenEncryptionKeys(null);
      assertEquals(refresh, getDecryptedRefreshToken(sessionUuid));
   }



   /*******************************************************************************
    ** Instance validation requires a valid key whenever refresh tokens will be
    ** stored, unless plaintext was explicitly allowed; key variables resolve.
    *******************************************************************************/
   @Test
   void testValidation_encryptionKey() throws Exception
   {
      ////////////////////////////////////////////
      // the standard test setup (key set) passes //
      ////////////////////////////////////////////
      assertNoKeyErrors(getOAuth2MetaData());

      //////////////////////////////////////////////////////////////////
      // no key, with the table defined & refresh enabled: fails fast //
      //////////////////////////////////////////////////////////////////
      assertKeyError(getOAuth2MetaData().withRefreshTokenEncryptionKey(null), "refreshTokenEncryptionKey must be set");

      //////////////////////////////////////////////////////////////////
      // no key is fine when nothing would be stored (refresh off, or //
      // no table), or plaintext is explicitly allowed (dev only).    //
      //////////////////////////////////////////////////////////////////
      assertNoKeyErrors(getOAuth2MetaData().withRefreshTokensEnabled(false));
      getOAuth2MetaData().setRefreshTokensEnabled(true);
      assertNoKeyErrors(getOAuth2MetaData().withAllowPlaintextRefreshTokens(true));
      getOAuth2MetaData().setAllowPlaintextRefreshTokens(false);

      QInstance noTableInstance = buildQInstance(false);
      OAuth2AuthenticationMetaData noTableMetaData = ((OAuth2AuthenticationMetaData) noTableInstance.getAuthentication()).withRefreshTokenEncryptionKey(null);
      QInstanceValidator noTableValidator = new QInstanceValidator();
      noTableMetaData.validate(noTableInstance, noTableValidator);
      assertThat(noTableValidator.getErrors()).noneMatch(e -> e.contains("refreshTokenEncryptionKey") || e.contains("encryption"));

      //////////////////////////////////////////////////
      // invalid keys and key ids fail (even if unused) //
      //////////////////////////////////////////////////
      assertKeyError(getOAuth2MetaData().withRefreshTokenEncryptionKey("not base64 !"), "not valid base64");
      assertKeyError(getOAuth2MetaData().withRefreshTokenEncryptionKey(Base64.getEncoder().encodeToString(new byte[16])), "must decode to 32 bytes");
      assertKeyError(getOAuth2MetaData().withRefreshTokenEncryptionKey("${env.QQQ_UNSET_VARIABLE_FOR_TEST_1048}"), "did not resolve");
      assertKeyError(getOAuth2MetaData().withRefreshTokenEncryptionKey(TEST_KEY).withRefreshTokenEncryptionKeyId("has:colon"), "refreshTokenEncryptionKeyId must be");
      getOAuth2MetaData().setRefreshTokenEncryptionKeyId(null);
      assertKeyError(getOAuth2MetaData().withPreviousRefreshTokenEncryptionKeys(Map.of(RefreshTokenCipher.DEFAULT_KEY_ID, OTHER_KEY)), "may not reuse the current key id");
      assertKeyError(getOAuth2MetaData().withPreviousRefreshTokenEncryptionKeys(Map.of("old", "short")), "previousRefreshTokenEncryptionKeys[old]");
      assertKeyError(getOAuth2MetaData().withRefreshTokenEncryptionKey(null).withRefreshTokensEnabled(false), "previousRefreshTokenEncryptionKeys is set, but refreshTokenEncryptionKey is not");
      getOAuth2MetaData().withRefreshTokensEnabled(true).withPreviousRefreshTokenEncryptionKeys(null);

      ///////////////////////////////////////////////////
      // ${prop.X} / ${env.X} references are resolved  //
      ///////////////////////////////////////////////////
      String propertyName = "qqq.test.oauth2.refreshTokenKey.1048";
      System.setProperty(propertyName, TEST_KEY);
      try
      {
         assertNoKeyErrors(getOAuth2MetaData().withRefreshTokenEncryptionKey("${prop." + propertyName + "}"));

         String sessionUuid = UUID.randomUUID().toString();
         insertSession(sessionUuid, createJwt(3600), "via-property-key", Instant.now());
         assertEquals("via-property-key", getDecryptedRefreshToken(sessionUuid));
      }
      finally
      {
         System.clearProperty(propertyName);
      }
   }



   /*******************************************************************************
    ** With allowPlaintextRefreshTokens (dev only) and no key, tokens are stored
    ** as-is, and refresh still works.
    *******************************************************************************/
   @Test
   void testAllowPlaintext_storesAsIsAndRefreshes() throws Exception
   {
      getOAuth2MetaData().withRefreshTokenEncryptionKey(null).withAllowPlaintextRefreshTokens(true);
      stubTokenEndpoint("authorization_code", 200, tokenResponse(createJwt(30), "plaintext-refresh"));
      QSession session = new OAuth2AuthenticationModule().createSession(qInstance, pkceContext());
      assertEquals("plaintext-refresh", getRefreshTokenRecord(session.getUuid()).getValueString("refreshToken"));

      stubTokenEndpoint("refresh_token", 200, tokenResponse(createJwt(3600), "plaintext-rotated"));
      assertTrue(new OAuth2AuthenticationModule().isSessionValid(qInstance, sessionWithUuid(session.getUuid())));
      assertEquals("plaintext-rotated", getRefreshTokenRecord(session.getUuid()).getValueString("refreshToken"));
   }



   /*******************************************************************************
    ** If (validation having been skipped) there is no key and plaintext isn't
    ** allowed, the refresh token is not stored - but login still works - and a
    ** stored value can't be used (the session ends cleanly).
    *******************************************************************************/
   @Test
   void testNoKey_notStoredAndNotUsed() throws Exception
   {
      String sessionUuid = UUID.randomUUID().toString();
      insertSession(sessionUuid, createJwt(-60), "stored-before-key-removed", Instant.now());
      getOAuth2MetaData().setRefreshTokenEncryptionKey(null);

      stubTokenEndpoint("authorization_code", 200, tokenResponse(createJwt(3600), "unencryptable-refresh"));
      QSession session = new OAuth2AuthenticationModule().createSession(qInstance, pkceContext());
      assertEquals(USER_ID, session.getUser().getIdReference());
      assertNull(getRefreshTokenRecord(session.getUuid()));

      assertSessionEndedWithoutRefresh(sessionUuid);
   }



   /*******************************************************************************
    ** Logout with a stored value that won't decrypt still deletes it (and can't
    ** revoke it, as it can't be read).
    *******************************************************************************/
   @Test
   void testLogout_undecryptableToken_deletedWithoutRevocation() throws Exception
   {
      stubRevocationEndpoint();
      String sessionUuid = UUID.randomUUID().toString();
      insertUserSessionRow(sessionUuid, createJwt(3600));
      insertRefreshTokenRow(sessionUuid, encryptForSession(new OAuth2AuthenticationMetaData().withRefreshTokenEncryptionKey(OTHER_KEY), "unreadable", sessionUuid), Instant.now());

      new OAuth2AuthenticationModule().logout(qInstance, sessionUuid);
      wireMockServer.verify(0, postRequestedFor(urlEqualTo(REVOKE_PATH)));
      assertNull(getRefreshTokenRecord(sessionUuid));
      assertNull(getUserSessionRecord(sessionUuid));
   }



   /*******************************************************************************
    ** The refresh-token table is locked down: a session holding every available
    ** permission, plus every conventional permission name for the table, still
    ** can't read (query/get/count/export) or write it; it isn't offered as an
    ** available permission; its token field is hidden.  The system session (as
    ** used by the module itself) is allowed.
    *******************************************************************************/
   @Test
   void testRefreshTokenTable_lockedDown() throws Exception
   {
      String         tableName = UserSessionRefreshToken.TABLE_NAME;
      QTableMetaData table     = qInstance.getTable(tableName);
      assertTrue(table.getIsHidden());
      assertTrue(table.getField("refreshToken").getIsHidden());
      assertEquals(PermissionLevel.HAS_ACCESS_PERMISSION, table.getPermissionRules().getLevel());
      assertEquals(UserSessionRefreshTokenMetaDataProducer.NO_ACCESS_PERMISSION_BASE_NAME, table.getPermissionRules().getPermissionBaseName());

      Collection<String> availablePermissions = PermissionsHelper.getAllAvailablePermissionNames(qInstance);
      assertThat(availablePermissions).noneMatch(p -> p.startsWith(UserSessionRefreshTokenMetaDataProducer.NO_ACCESS_PERMISSION_BASE_NAME) || p.startsWith(tableName));

      Set<String> adminPermissions = new HashSet<>(availablePermissions);
      for(String suffix : List.of("read", "insert", "edit", "delete", "write", "hasAccess"))
      {
         adminPermissions.add(tableName + "." + suffix);
      }

      ExportInput exportInput = new ExportInput();
      exportInput.setTableName(tableName);

      QSession adminSession = new QSession();
      adminSession.setPermissions(adminPermissions);
      QContext.init(qInstance, adminSession);
      assertThrows(QPermissionDeniedException.class, () -> PermissionsHelper.checkTablePermissionThrowing(new QueryInput(tableName), TablePermissionSubType.READ));
      assertThrows(QPermissionDeniedException.class, () -> PermissionsHelper.checkTablePermissionThrowing(new GetInput(tableName), TablePermissionSubType.READ));
      assertThrows(QPermissionDeniedException.class, () -> PermissionsHelper.checkTablePermissionThrowing(new CountInput(tableName), TablePermissionSubType.READ));
      assertThrows(QPermissionDeniedException.class, () -> PermissionsHelper.checkTablePermissionThrowing(exportInput, TablePermissionSubType.READ));
      assertThrows(QPermissionDeniedException.class, () -> PermissionsHelper.checkTablePermissionThrowing(new InsertInput(tableName), TablePermissionSubType.INSERT));
      assertThrows(QPermissionDeniedException.class, () -> PermissionsHelper.checkTablePermissionThrowing(new UpdateInput(tableName), TablePermissionSubType.EDIT));
      assertThrows(QPermissionDeniedException.class, () -> PermissionsHelper.checkTablePermissionThrowing(new DeleteInput(tableName), TablePermissionSubType.DELETE));
      assertFalse(PermissionsHelper.hasTablePermission(new QueryInput(tableName), tableName, TablePermissionSubType.READ));

      QContext.init(qInstance, new QSystemUserSession());
      PermissionsHelper.checkTablePermissionThrowing(new QueryInput(tableName), TablePermissionSubType.READ);
   }



   /*******************************************************************************
    ** Assert that resuming / validating the session fails without a provider
    ** call, and that its rows are gone.
    *******************************************************************************/
   private void assertSessionEndedWithoutRefresh(String sessionUuid) throws Exception
   {
      OAuth2AuthenticationModule module = new OAuth2AuthenticationModule();
      assertFalse(module.isSessionValid(qInstance, sessionWithUuid(sessionUuid)));
      assertNull(getRefreshTokenRecord(sessionUuid));
      assertNull(getUserSessionRecord(sessionUuid));
      assertThrows(QAuthenticationException.class, () -> module.createSession(qInstance, Map.of("sessionUUID", sessionUuid)));
      wireMockServer.verify(0, postRequestedFor(urlEqualTo(TOKEN_PATH)).withRequestBody(containing("grant_type=refresh_token")));
   }



   /*******************************************************************************
    ** Validate the OAuth2 meta-data, expecting an error containing the text.
    *******************************************************************************/
   private void assertKeyError(OAuth2AuthenticationMetaData metaData, String expectedText)
   {
      QInstanceValidator validator = new QInstanceValidator();
      metaData.validate(qInstance, validator);
      assertThat(validator.getErrors()).anyMatch(e -> e.contains(expectedText));
      assertThat(validator.getErrors()).noneMatch(e -> e.contains(TEST_KEY) || e.contains(OTHER_KEY));
   }



   /*******************************************************************************
    ** Validate the OAuth2 meta-data, expecting no key-related errors.
    *******************************************************************************/
   private void assertNoKeyErrors(OAuth2AuthenticationMetaData metaData)
   {
      QInstanceValidator validator = new QInstanceValidator();
      metaData.validate(qInstance, validator);
      assertThat(validator.getErrors()).noneMatch(e -> e.contains("refreshTokenEncryptionKey") || e.contains("encryption"));
   }



   /*******************************************************************************
    ** Fail if any collected log message (or exception) contains the secret.
    *******************************************************************************/
   private void assertNoLogContains(QCollectingLogger collectingLogger, String secret)
   {
      for(CollectedLogMessage message : collectingLogger.getCollectedMessages())
      {
         assertThat(message.getMessage()).doesNotContain(secret);
         if(message.getException() != null)
         {
            assertThat(String.valueOf(message.getException())).doesNotContain(secret);
         }
      }
   }



   /*******************************************************************************
    ** Get the instance's OAuth2 meta-data
    *******************************************************************************/
   private OAuth2AuthenticationMetaData getOAuth2MetaData()
   {
      return ((OAuth2AuthenticationMetaData) qInstance.getAuthentication());
   }



   /*******************************************************************************
    ** Build a QSession pointing at a stored session uuid
    *******************************************************************************/
   private QSession sessionWithUuid(String sessionUuid)
   {
      QSession session = new QSession();
      session.setUuid(sessionUuid);
      return (session);
   }



   /*******************************************************************************
    ** Context for a PKCE code exchange
    *******************************************************************************/
   private Map<String, String> pkceContext()
   {
      return (Map.of("code", "test-code-" + UUID.randomUUID(),
         "redirectUri", "http://localhost:3000/callback",
         "codeVerifier", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
   }



   /*******************************************************************************
    ** Build a QInstance with OAuth2 auth, optionally with the refresh-token table
    *******************************************************************************/
   private QInstance buildQInstance(boolean withRefreshTokenTable) throws Exception
   {
      QInstance instance = new QInstance();
      instance.addBackend(new QBackendMetaData().withName(BACKEND_NAME).withBackendType(MemoryBackendModule.class));
      instance.addTable(new UserSessionMetaDataProducer(BACKEND_NAME).produce(instance));
      if(withRefreshTokenTable)
      {
         instance.addTable(new UserSessionRefreshTokenMetaDataProducer(BACKEND_NAME).produce(instance));
      }

      instance.addTable(new QTableMetaData()
         .withName(REDIRECT_STATE_TABLE)
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("state", QFieldType.STRING).withMaxLength(100))
         .withField(new QFieldMetaData("redirectUri", QFieldType.STRING)));

      OAuth2AuthenticationMetaData authMetaData = new OAuth2AuthenticationMetaData()
         .withBaseUrl(wireMockServer.baseUrl())
         .withClientId("test-client-id")
         .withClientSecret("test-client-secret")
         .withScopes("openid profile email offline_access")
         .withUserSessionTableName(UserSession.TABLE_NAME)
         .withRedirectStateTableName(REDIRECT_STATE_TABLE)
         .withRefreshTokenEncryptionKey(TEST_KEY);
      authMetaData.setName("oauth2");
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), authMetaData);
      return (instance);
   }



   /*******************************************************************************
    ** Insert a userSession, and a refresh token row for it
    *******************************************************************************/
   private void insertSession(String sessionUuid, String accessToken, String refreshToken, Instant sessionStartedAt) throws Exception
   {
      insertUserSessionRow(sessionUuid, accessToken);
      insertRefreshTokenRow(sessionUuid, encryptForSession(getOAuth2MetaData(), refreshToken, sessionUuid), sessionStartedAt);
   }



   /*******************************************************************************
    ** Insert a userSession row for USER_ID
    *******************************************************************************/
   private void insertUserSessionRow(String sessionUuid, String accessToken) throws Exception
   {
      new InsertAction().execute(new InsertInput(UserSession.TABLE_NAME).withRecordEntity(new UserSession()
         .withUuid(sessionUuid)
         .withUserId(USER_ID)
         .withAccessToken(accessToken)));
   }



   /*******************************************************************************
    ** Insert a refresh token row with exactly the given stored value
    *******************************************************************************/
   private void insertRefreshTokenRow(String sessionUuid, String storedValue, Instant sessionStartedAt) throws Exception
   {
      new InsertAction().execute(new InsertInput(UserSessionRefreshToken.TABLE_NAME).withRecordEntity(new UserSessionRefreshToken()
         .withSessionUuid(sessionUuid)
         .withRefreshToken(storedValue)
         .withSessionStartedAt(sessionStartedAt)));
   }



   /*******************************************************************************
    ** Encrypt a refresh token as the module would for the given session (whose
    ** userSession row has USER_ID), under the given meta-data's current key.
    *******************************************************************************/
   private String encryptForSession(OAuth2AuthenticationMetaData metaData, String refreshToken, String sessionUuid) throws Exception
   {
      return (RefreshTokenCipher.fromMetaData(metaData).encrypt(refreshToken, RefreshTokenCipher.buildAssociatedData(sessionUuid, USER_ID)));
   }



   /*******************************************************************************
    ** Read and decrypt (with the instance's keys) a session's stored refresh token
    *******************************************************************************/
   private String getDecryptedRefreshToken(String sessionUuid) throws Exception
   {
      String storedValue = getRefreshTokenRecord(sessionUuid).getValueString("refreshToken");
      return (RefreshTokenCipher.fromMetaData(getOAuth2MetaData()).decrypt(storedValue, RefreshTokenCipher.buildAssociatedData(sessionUuid, USER_ID)));
   }



   /*******************************************************************************
    ** Read a userSession record (unmasked, including hidden fields)
    *******************************************************************************/
   private QRecord getUserSessionRecord(String sessionUuid) throws Exception
   {
      return (new GetAction().executeForRecord(new GetInput(UserSession.TABLE_NAME).withUniqueKey(Map.of("uuid", sessionUuid)).withShouldOmitHiddenFields(false)));
   }



   /*******************************************************************************
    ** Read a refresh token record (unmasked, including hidden fields)
    *******************************************************************************/
   private QRecord getRefreshTokenRecord(String sessionUuid) throws Exception
   {
      return (new GetAction().executeForRecord(new GetInput(UserSessionRefreshToken.TABLE_NAME).withUniqueKey(Map.of("sessionUuid", sessionUuid)).withShouldOmitHiddenFields(false)));
   }



   /*******************************************************************************
    ** Create an (unsigned) JWT access token for USER_ID, expiring in the given
    ** number of seconds (negative = already expired).
    *******************************************************************************/
   private String createJwt(long expiresInSeconds)
   {
      JSONObject header  = new JSONObject().put("alg", "HS256").put("typ", "JWT");
      JSONObject payload = new JSONObject()
         .put("sub", USER_ID)
         .put("email", USER_ID)
         .put("name", "Refresh User")
         .put("jti", UUID.randomUUID().toString())
         .put("iat", Instant.now().getEpochSecond())
         .put("exp", Instant.now().plusSeconds(expiresInSeconds).getEpochSecond());

      Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
      return (encoder.encodeToString(header.toString().getBytes()) + "." + encoder.encodeToString(payload.toString().getBytes()) + ".fake-signature");
   }



   /*******************************************************************************
    ** Build a successful token-endpoint response body
    *******************************************************************************/
   private JSONObject tokenResponse(String accessToken, String refreshToken)
   {
      JSONObject response = new JSONObject()
         .put("access_token", accessToken)
         .put("token_type", "Bearer")
         .put("expires_in", 3600);
      if(refreshToken != null)
      {
         response.put("refresh_token", refreshToken);
      }
      return (response);
   }



   /*******************************************************************************
    ** Stub the token endpoint for a given grant type
    *******************************************************************************/
   private void stubTokenEndpoint(String grantType, int status, JSONObject body)
   {
      wireMockServer.stubFor(post(urlEqualTo(TOKEN_PATH))
         .withRequestBody(containing("grant_type=" + grantType))
         .willReturn(aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body.toString())));
   }



   /*******************************************************************************
    ** Stub the revocation endpoint
    *******************************************************************************/
   private void stubRevocationEndpoint()
   {
      wireMockServer.stubFor(post(urlEqualTo(REVOKE_PATH)).willReturn(aResponse().withStatus(200)));
   }



   /*******************************************************************************
    ** Stub OIDC discovery, optionally advertising a revocation endpoint
    *******************************************************************************/
   private void stubOidcDiscovery(boolean withRevocationEndpoint)
   {
      String     baseUrl   = wireMockServer.baseUrl();
      JSONObject discovery = new JSONObject()
         .put("issuer", baseUrl)
         .put("authorization_endpoint", baseUrl + "/authorize")
         .put("token_endpoint", baseUrl + TOKEN_PATH)
         .put("jwks_uri", baseUrl + "/.well-known/jwks.json")
         .put("response_types_supported", new JSONArray().put("code"))
         .put("subject_types_supported", new JSONArray().put("public"))
         .put("id_token_signing_alg_values_supported", new JSONArray().put("RS256"));
      if(withRevocationEndpoint)
      {
         discovery.put("revocation_endpoint", baseUrl + REVOKE_PATH);
      }

      wireMockServer.stubFor(get(urlEqualTo("/.well-known/openid-configuration"))
         .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(discovery.toString())));
   }



   /*******************************************************************************
    ** Stand-in for an application's subclass of the module.
    *******************************************************************************/
   public static class SubclassedOAuth2AuthenticationModule extends OAuth2AuthenticationModule
   {
      /*******************************************************************************
       ** Delegate to super, then decorate the session.
       *******************************************************************************/
      @Override
      public QSession createSession(QInstance instance, Map<String, String> context) throws QAuthenticationException
      {
         return (super.createSession(instance, context).withValue("fromSubclass", "true"));
      }



      /*******************************************************************************
       ** Delegate to super.
       *******************************************************************************/
      @Override
      public boolean isSessionValid(QInstance instance, QSession session)
      {
         return (super.isSessionValid(instance, session));
      }
   }

}
