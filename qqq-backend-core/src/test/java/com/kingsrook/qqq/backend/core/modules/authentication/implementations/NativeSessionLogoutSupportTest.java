/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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
// SPDX-License-Identifier: Apache-2.0

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2NativeSessionLoginMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2NativeSessionLogoutMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Real private storage, encryption binding and current-authority checks.
 *******************************************************************************/
class NativeSessionLogoutSupportTest extends BaseTest
{
   private static final String CALLBACK = "com.example.handheld:/oauth2/callback";
   private static final String TOKEN = "synthetic-id-token-private-canary";
   private QInstance instance;
   private OAuth2AuthenticationMetaData auth;
   private QSession session;
   private final NativeSessionLogoutSupport support = new NativeSessionLogoutSupport()
   {
      /*******************************************************************************
       ** Keep discovery deterministic while exercising real session storage and encryption binding.
       *******************************************************************************/
      @Override
      protected String resolveEndSessionEndpoint(OAuth2AuthenticationMetaData metadata)
      {
         return ("https://login.example.test/logout");
      }
   };



   /*******************************************************************************
    ** No refresh-token table or refresh grant is present in this fixture.
    *******************************************************************************/
   @BeforeEach
   void prepare() throws Exception
   {
      instance = new QInstance();
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      instance.addTable(new UserSessionMetaDataProducer("memory").withNativeLogoutContext().produce(instance));
      instance.addTable(new RedirectStateMetaDataProducer("memory").produce(instance));
      auth = new OAuth2AuthenticationMetaData().withBaseUrl("https://internal.example.test").withClientId("client")
         .withClientSecret("synthetic-secret").withScopes("openid").withUserSessionTableName(UserSession.TABLE_NAME)
         .withRedirectStateTableName(RedirectStateMetaDataProducer.TABLE_NAME)
         .withRefreshTokensEnabled(false).withRefreshTokenEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]))
         .withNativeSessionLogin(new OAuth2NativeSessionLoginMetaData().withIssuer("https://login.example.test")
            .withDiscoveryUrl("https://login.example.test/discovery").withRedirectUris(List.of(CALLBACK)).withRequireNonceValidation(true))
         .withNativeSessionLogout(new OAuth2NativeSessionLogoutMetaData().withPostLogoutRedirectUris(List.of(CALLBACK)));
      auth.setName("provider");
      instance.withInstanceDefaultAuthentication(auth);
      session = new QSession().withUser(new QUser().withIdReference("user"));
      session.setUuid("native-session");
      QContext.init(instance, new QSystemUserSession());
      new InsertAction().execute(new InsertInput(UserSession.TABLE_NAME).withRecordEntity(
         new UserSession().withUuid(session.getUuid()).withUserId("user").withAccessToken("synthetic-access")));
      QContext.setQSession(session);
      support.retain(session, TOKEN, CALLBACK);
   }



   /*******************************************************************************
    ** Retained data decrypts only in the deliberate logout response boundary.
    *******************************************************************************/
   @Test
   void privateStorageAndNoRefreshDependency() throws Exception
   {
      assertTrue(NativeSessionLogoutSupport.isConfigured(instance));
      assertEquals(TOKEN, support.prepare(session.getUuid(), CALLBACK).getIdTokenHint());
      assertFalse(JsonUtils.toJson(session).contains(TOKEN));
      QContext.setQSession(new QSystemUserSession());
      String stored = row().getValueString(NativeSessionLogoutSupport.FIELD);
      assertNotNull(stored);
      assertFalse(stored.contains(TOKEN));
      assertThrows(Exception.class, () -> RefreshTokenCipher.fromMetaData(auth).decrypt(stored,
         RefreshTokenCipher.buildAssociatedData(session.getUuid(), "user")));
      assertThrows(Exception.class, () -> RefreshTokenCipher.fromMetaData(auth).decrypt(stored,
         NativeSessionLogoutSupport.associatedData("another-provider", session.getUuid(), "user")));
      assertThrows(Exception.class, () -> RefreshTokenCipher.fromMetaData(auth).decrypt(stored,
         NativeSessionLogoutSupport.associatedData(auth.getName(), "another-session", "user")));
      assertThrows(Exception.class, () -> RefreshTokenCipher.fromMetaData(auth).decrypt(stored,
         NativeSessionLogoutSupport.associatedData(auth.getName(), session.getUuid(), "another-user")));
   }



   /*******************************************************************************
    ** Current session, identity, configuration and ciphertext must all still match.
    *******************************************************************************/
   @Test
   void rejectsChangedAuthorityAndSwappedCiphertext() throws Exception
   {
      assertNull(support.prepare("different-session", CALLBACK));
      session.setValue(OAuth2AuthenticationModule.BEARER_SESSION_VALUE_KEY, "true");
      assertNull(support.prepare(session.getUuid(), CALLBACK));
      session.setValue(OAuth2AuthenticationModule.BEARER_SESSION_VALUE_KEY, "false");
      assertNull(support.prepare(session.getUuid(), "com.attacker:/callback"));
      session.getUser().setIdReference("different-user");
      assertNull(support.prepare(session.getUuid(), CALLBACK));
      session.getUser().setIdReference("user");
      auth.setClientId("different-client");
      assertNull(support.prepare(session.getUuid(), CALLBACK));
      auth.setClientId("client");
      auth.getNativeSessionLogin().setIssuer("https://different.example.test");
      assertNull(support.prepare(session.getUuid(), CALLBACK));
      auth.getNativeSessionLogin().setIssuer("https://login.example.test");
      QContext.setQSession(new QSystemUserSession());
      QRecord original = row();
      new InsertAction().execute(new InsertInput(UserSession.TABLE_NAME).withRecord(new QRecord()
         .withValue("uuid", "second-session").withValue("userId", "user").withValue("accessToken", "synthetic")
         .withValue(NativeSessionLogoutSupport.FIELD, original.getValueString(NativeSessionLogoutSupport.FIELD))));
      session.setUuid("second-session");
      QContext.setQSession(session);
      assertNull(support.prepare(session.getUuid(), CALLBACK));
   }



   /*******************************************************************************
    ** Absent policy/key/storage and ended sessions are never advertised or usable.
    *******************************************************************************/
   @Test
   void missingConfigurationAndLogoutFailClosed()
   {
      auth.setRefreshTokenEncryptionKey(null);
      assertFalse(NativeSessionLogoutSupport.isConfigured(instance));
      assertNull(support.prepare(session.getUuid(), CALLBACK));
      auth.setRefreshTokenEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]));
      new OAuth2AuthenticationModule().logout(instance, session.getUuid());
      assertNull(support.prepare(session.getUuid(), CALLBACK));
      instance.getTable(UserSession.TABLE_NAME).getFields().remove(NativeSessionLogoutSupport.FIELD);
      assertFalse(NativeSessionLogoutSupport.isConfigured(instance));
   }



   /*******************************************************************************
    ** Database errors do not report successful invalidation or expose token data.
    *******************************************************************************/
   @Test
   void deletionFailureIsObservableAndContextRestored()
   {
      instance.getTable(UserSession.TABLE_NAME).setBackendName("missing-backend");
      IllegalStateException failure = assertThrows(IllegalStateException.class,
         () -> new OAuth2AuthenticationModule().logout(instance, session.getUuid()));
      assertEquals("Session invalidation failed", failure.getMessage());
      assertNull(failure.getCause());
      assertEquals(session, QContext.getQSession());
   }



   /*******************************************************************************
    ** System-only lookup used by storage assertions.
    *******************************************************************************/
   private QRecord row() throws Exception
   {
      return (new GetAction().executeForRecord(new GetInput(UserSession.TABLE_NAME).withUniqueKey(Map.of("uuid", session.getUuid()))
         .withShouldOmitHiddenFields(false).withShouldMaskPasswords(false)));
   }
}
