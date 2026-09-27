/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.Auth0AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.TableBasedAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.SimpleStateKey;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for the TableBasedAuthenticationModule
 *******************************************************************************/
public class TableBasedAuthenticationModuleTest extends BaseTest
{
   public static final String USERNAME  = "jdoe";
   public static final String PASSWORD  = "abc123";
   public static final String FULL_NAME = "John Doe";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
      MemoryRecordStore.resetStatistics();
      MemoryRecordStore.setCollectStatistics(false);
      TableBasedAuthenticationModule.clearSignInThrottle();
   }



   /*******************************************************************************
    ** A remembered validation no longer hides an inactivity timeout shorter than
    ** the validation interval (QRun-IO/qqq#696).
    *******************************************************************************/
   @Test
   void testShortInactivityTimeoutIsEnforcedPromptly() throws Exception
   {
      QInstance qInstance = getQInstance();
      ((TableBasedAuthenticationMetaData) qInstance.getAuthentication()).setInactivityTimeoutSeconds(60);
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      String uuid = insertTestSession(qInstance, USERNAME, Instant.now().minus(90, ChronoUnit.SECONDS));

      QSession session = new QSession();
      session.setIdReference(uuid);

      ///////////////////////////////////////////////////////////////////////
      // validated 10 seconds ago (within the interval), idle for 90 of 60 //
      ///////////////////////////////////////////////////////////////////////
      InMemoryStateProvider.getInstance().put(new SimpleStateKey<>(uuid), Instant.now().minus(10, ChronoUnit.SECONDS));
      InMemoryStateProvider.getInstance().put(new SimpleStateKey<>("tableBasedAuthActivity:" + uuid), Instant.now().minus(90, ChronoUnit.SECONDS));
      assertFalse(new TableBasedAuthenticationModule().isSessionValid(qInstance, session));
      assertTrue(InMemoryStateProvider.getInstance().get(Instant.class, new SimpleStateKey<>(uuid)).isEmpty(), "the remembered validation is dropped");
   }



   /*******************************************************************************
    ** Requests answered from the remembered validation count as activity, so an
    ** active session is not expired because the table's access time is stale.
    *******************************************************************************/
   @Test
   void testActiveSessionIsNotExpiredEarly() throws Exception
   {
      QInstance qInstance = getQInstance();
      ((TableBasedAuthenticationMetaData) qInstance.getAuthentication()).setInactivityTimeoutSeconds(60);
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      String uuid = insertTestSession(qInstance, USERNAME, Instant.now().minus(70, ChronoUnit.SECONDS));

      QSession session = new QSession();
      session.setIdReference(uuid);
      InMemoryStateProvider.getInstance().put(new SimpleStateKey<>(uuid), Instant.now().minus(40, ChronoUnit.SECONDS));
      InMemoryStateProvider.getInstance().put(new SimpleStateKey<>("tableBasedAuthActivity:" + uuid), Instant.now().minus(5, ChronoUnit.SECONDS));
      assertTrue(new TableBasedAuthenticationModule().isSessionValid(qInstance, session));

      //////////////////////////////////////////////////////////
      // the revalidation refreshed the table's access time   //
      //////////////////////////////////////////////////////////
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new Auth0AuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK));
      GetInput getInput = new GetInput();
      getInput.setTableName("session");
      getInput.setPrimaryKey(uuid);
      Instant accessTimestamp = new GetAction().execute(getInput).getRecord().getValueInstant("accessTimestamp");
      assertTrue(accessTimestamp.isAfter(Instant.now().minus(5, ChronoUnit.SECONDS)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidationIntervalIsAtMostHalfTheTimeout()
   {
      assertEquals(1800, TableBasedAuthenticationModule.validationInterval(new TableBasedAuthenticationMetaData()).toSeconds());
      assertEquals(30, TableBasedAuthenticationModule.validationInterval(new TableBasedAuthenticationMetaData().withInactivityTimeoutSeconds(60)).toSeconds());
      assertEquals(1, TableBasedAuthenticationModule.validationInterval(new TableBasedAuthenticationMetaData().withInactivityTimeoutSeconds(1)).toSeconds());
      assertEquals(1800, TableBasedAuthenticationModule.validationInterval(new TableBasedAuthenticationMetaData().withInactivityTimeoutSeconds(null)).toSeconds());
   }



   /*******************************************************************************
    ** Repeated failures lock the username out, whatever password follows; known
    ** and unknown usernames behave the same (QRun-IO/qqq#696).
    *******************************************************************************/
   @Test
   void testSignInLockout() throws Exception
   {
      QInstance                      qInstance  = getQInstance();
      TableBasedAuthenticationModule authModule = new TableBasedAuthenticationModule();
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);

      for(int i = 0; i < 5; i++)
      {
         assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, "wrong"))))
            .isInstanceOf(QAuthenticationException.class).hasMessage("Incorrect username or password.");
      }
      assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, PASSWORD))))
         .isInstanceOf(QAuthenticationException.class).hasMessage("Too many failed sign-in attempts. Try again later.");
      assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(" " + USERNAME.toUpperCase(), PASSWORD))))
         .isInstanceOf(QAuthenticationException.class).hasMessage("Too many failed sign-in attempts. Try again later.");

      for(int i = 0; i < 5; i++)
      {
         assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth("nobody", "wrong"))))
            .hasMessage("Incorrect username or password.");
      }
      assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth("nobody", "wrong"))))
         .hasMessage("Too many failed sign-in attempts. Try again later.");

      /////////////////////////////////////////
      // another username is unaffected      //
      /////////////////////////////////////////
      insertTestUser(qInstance, "other", PASSWORD, "Other");
      assertNotNull(authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth("other", PASSWORD))));
   }



   /*******************************************************************************
    ** A success resets the count; the lockout can be switched off.
    *******************************************************************************/
   @Test
   void testSignInLockoutResetAndDisable() throws Exception
   {
      QInstance                      qInstance  = getQInstance();
      TableBasedAuthenticationModule authModule = new TableBasedAuthenticationModule();
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);

      for(int round = 0; round < 2; round++)
      {
         for(int i = 0; i < 4; i++)
         {
            assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, "wrong"))))
               .hasMessage("Incorrect username or password.");
         }
         assertNotNull(authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, PASSWORD))));
      }

      ((TableBasedAuthenticationMetaData) qInstance.getAuthentication()).setMaxFailedSignInAttempts(0);
      for(int i = 0; i < 10; i++)
      {
         assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, "wrong"))))
            .hasMessage("Incorrect username or password.");
      }
      assertNotNull(authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, PASSWORD))));
   }



   /*******************************************************************************
    ** The user and session tables are protected by default: no permissions, no
    ** access, password hashes hidden; the module still signs users in; rules the
    ** application set are kept (QRun-IO/qqq#696).
    *******************************************************************************/
   @Test
   void testAuthenticationTablesProtectedByDefault() throws Exception
   {
      QInstance qInstance = getQInstance();
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      new QInstanceEnricher(qInstance).enrich();

      for(String tableName : List.of("user", "session"))
      {
         assertEquals(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS, qInstance.getTable(tableName).getPermissionRules().getLevel(), tableName);
         assertEquals(DenyBehavior.HIDDEN, qInstance.getTable(tableName).getPermissionRules().getDenyBehavior(), tableName);
      }
      assertTrue(qInstance.getTable("user").getField("passwordHash").getIsHidden());

      QSession signedIn = new TableBasedAuthenticationModule().createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, PASSWORD)));
      QContext.init(qInstance, signedIn);
      for(String tableName : List.of("user", "session"))
      {
         QueryInput queryInput = new QueryInput(tableName);
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.READ)).isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.INSERT)).isInstanceOf(QPermissionDeniedException.class);
      }

      signedIn.withPermission("user.read");
      PermissionsHelper.checkTablePermissionThrowing(new QueryInput("user"), TablePermissionSubType.READ);
      QueryInput userQuery = new QueryInput("user");
      userQuery.setShouldOmitHiddenFields(true);
      QRecord user = new QueryAction().execute(userQuery).getRecords().get(0);
      assertEquals(USERNAME, user.getValueString("username"));
      assertFalse(user.getValues().containsKey("passwordHash"));

      /////////////////////////////////////////
      // an application's own rules are kept //
      /////////////////////////////////////////
      QInstance custom = getQInstance();
      custom.getTable("user").setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED));
      new QInstanceEnricher(custom).enrich();
      assertEquals(PermissionLevel.NOT_PROTECTED, custom.getTable("user").getPermissionRules().getLevel());
      assertEquals(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS, custom.getTable("session").getPermissionRules().getLevel());
      assertTrue(custom.getTable("user").getField("passwordHash").getIsHidden());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuccessfulLogin() throws Exception
   {
      QInstance qInstance = getQInstance();
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);

      QSession session = new TableBasedAuthenticationModule().createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, PASSWORD)));

      assertNotNull(session);
      assertNotNull(session.getIdReference());
      assertNotNull(session.getUser());
      assertEquals(USERNAME, session.getUser().getIdReference());
      assertEquals(FULL_NAME, session.getUser().getFullName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadUsernameAndPassword() throws Exception
   {
      QInstance qInstance = getQInstance();
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);

      TableBasedAuthenticationModule authModule = new TableBasedAuthenticationModule();

      assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth("not-" + USERNAME, PASSWORD))))
         .isInstanceOf(QAuthenticationException.class)
         .hasMessageContaining("Incorrect username or password");

      assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, "not-" + PASSWORD))))
         .isInstanceOf(QAuthenticationException.class)
         .hasMessageContaining("Incorrect username or password");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoContextProvided() throws Exception
   {
      QInstance qInstance = getQInstance();
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);

      assertThatThrownBy(() -> new TableBasedAuthenticationModule().createSession(qInstance, Collections.emptyMap()))
         .isInstanceOf(QAuthenticationException.class)
         .hasMessageContaining("Session ID was not provided");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUseExistingSession() throws Exception
   {
      QInstance qInstance = getQInstance();

      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      String uuid = insertTestSession(qInstance, USERNAME, Instant.now());

      QSession session = new TableBasedAuthenticationModule().createSession(qInstance, Map.of(TableBasedAuthenticationModule.SESSION_ID_KEY, uuid));
      assertNotNull(session);
      assertEquals(uuid, session.getIdReference());
      assertNotNull(session.getUser());
      assertEquals(USERNAME, session.getUser().getIdReference());
      assertEquals(FULL_NAME, session.getUser().getFullName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCreatingAlmostExpiredSession() throws Exception
   {
      QInstance qInstance = getQInstance();

      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      String uuid = insertTestSession(qInstance, USERNAME, Instant.now().minus(4, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES));

      QSession session = new TableBasedAuthenticationModule().createSession(qInstance, Map.of(TableBasedAuthenticationModule.SESSION_ID_KEY, uuid));
      assertNotNull(session);
      assertEquals(uuid, session.getIdReference());
      assertNotNull(session.getUser());
      assertEquals(USERNAME, session.getUser().getIdReference());
      assertEquals(FULL_NAME, session.getUser().getFullName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidatingAlmostExpiredSession() throws Exception
   {
      QInstance qInstance = getQInstance();

      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      String uuid = insertTestSession(qInstance, USERNAME, Instant.now().minus(4, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES));

      QSession session = new QSession();
      session.setIdReference(uuid);
      InMemoryStateProvider.getInstance().put(new SimpleStateKey<>(session.getIdReference()), Instant.now());
      assertTrue(new TableBasedAuthenticationModule().isSessionValid(qInstance, session));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCreatingJustExpiredSession() throws Exception
   {
      QInstance qInstance = getQInstance();

      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      String uuid = insertTestSession(qInstance, USERNAME, Instant.now().minus(4, ChronoUnit.HOURS).minus(1, ChronoUnit.MINUTES));

      assertThatThrownBy(() -> new TableBasedAuthenticationModule().createSession(qInstance, Map.of(TableBasedAuthenticationModule.SESSION_ID_KEY, uuid)))
         .isInstanceOf(QAuthenticationException.class)
         .hasMessageContaining("Session is expired");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidatingJustExpiredSession() throws Exception
   {
      QInstance qInstance = getQInstance();

      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      String uuid = insertTestSession(qInstance, USERNAME, Instant.now().minus(4, ChronoUnit.HOURS).minus(1, ChronoUnit.MINUTES));

      QSession session = new QSession();
      session.setIdReference(uuid);
      assertFalse(new TableBasedAuthenticationModule().isSessionValid(qInstance, session));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidatingNullInputs()
   {
      assertFalse(new TableBasedAuthenticationModule().isSessionValid(getQInstance(), null));
      assertFalse(new TableBasedAuthenticationModule().isSessionValid(getQInstance(), new QSession()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNonExistingSessionUUID() throws Exception
   {
      QInstance qInstance = getQInstance();

      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      String uuid = insertTestSession(qInstance, USERNAME, Instant.now());

      assertThatThrownBy(() -> new TableBasedAuthenticationModule().createSession(qInstance, Map.of(TableBasedAuthenticationModule.SESSION_ID_KEY, "not-" + uuid)))
         .isInstanceOf(QAuthenticationException.class)
         .hasMessageContaining("Session not found");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExistingSessionWithBadUserId() throws Exception
   {
      QInstance qInstance = getQInstance();

      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      String uuid = insertTestSession(qInstance, "not-" + USERNAME, Instant.now());

      assertThatThrownBy(() -> new TableBasedAuthenticationModule().createSession(qInstance, Map.of(TableBasedAuthenticationModule.SESSION_ID_KEY, uuid)))
         .isInstanceOf(QAuthenticationException.class)
         .hasMessageContaining("User for session not found");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWeDontAlwaysRevalidate() throws Exception
   {
      QInstance qInstance = getQInstance();

      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      String uuid = insertTestSession(qInstance, USERNAME, Instant.now().minus(4, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES));

      QSession session = new TableBasedAuthenticationModule().createSession(qInstance, Map.of(TableBasedAuthenticationModule.SESSION_ID_KEY, uuid));

      MemoryRecordStore.setCollectStatistics(true);

      assertTrue(new TableBasedAuthenticationModule().isSessionValid(qInstance, session));
      Map<String, Integer> statistics = MemoryRecordStore.getStatistics();
      assertEquals(0, statistics.size()); // should be no stats of any type!
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWeDoAlwaysRevalidateIfNeeded() throws Exception
   {
      QInstance qInstance = getQInstance();

      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      String uuid = insertTestSession(qInstance, USERNAME, Instant.now().minus(4, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES));

      QSession session = new TableBasedAuthenticationModule().createSession(qInstance, Map.of(TableBasedAuthenticationModule.SESSION_ID_KEY, uuid));

      assertTrue(new TableBasedAuthenticationModule().isSessionValid(qInstance, session));

      InMemoryStateProvider.getInstance().put(new SimpleStateKey<>(session.getIdReference()), Instant.now().minus(TableBasedAuthenticationModule.ID_TOKEN_VALIDATION_INTERVAL_SECONDS + 10, ChronoUnit.SECONDS));

      MemoryRecordStore.setCollectStatistics(true);
      assertTrue(new TableBasedAuthenticationModule().isSessionValid(qInstance, session));
      Map<String, Integer> statistics = MemoryRecordStore.getStatistics();
      assertEquals(4, statistics.get(MemoryRecordStore.STAT_QUERIES_RAN));
   }



   /*******************************************************************************
    ** A password may contain colons: the user-id ends at the first one (RFC 7617).
    *******************************************************************************/
   @Test
   void testPasswordContainingColons() throws Exception
   {
      QInstance qInstance = getQInstance();
      insertTestUser(qInstance, USERNAME, "pass:word:2026", FULL_NAME);

      TableBasedAuthenticationModule authModule = new TableBasedAuthenticationModule();
      QSession                       session    = authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, "pass:word:2026")));
      assertEquals(USERNAME, session.getUser().getIdReference());

      assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, "pass"))))
         .isInstanceOf(QAuthenticationException.class)
         .hasMessage("Incorrect username or password.");
   }



   /*******************************************************************************
    ** Credentials without a user-id, or a user without a password hash, are
    ** refused as incorrect - never as an internal error that describes them.
    *******************************************************************************/
   @Test
   void testMalformedCredentialsAndMissingHash() throws Exception
   {
      QInstance qInstance = getQInstance();
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);

      TableBasedAuthenticationModule authModule = new TableBasedAuthenticationModule();
      for(String credentials : List.of(USERNAME + PASSWORD, ":" + PASSWORD, ""))
      {
         String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
         assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encoded)))
            .isInstanceOf(QAuthenticationException.class)
            .hasMessage("Incorrect username or password.");
      }

      QAuthenticationMetaData tableBasedAuthentication = qInstance.getAuthentication();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new Auth0AuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK));
      TestUtils.insertRecords(qInstance.getTable("user"), List.of(new QRecord().withValue("username", "nohash").withValue("fullName", "No Hash")));
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), tableBasedAuthentication);

      assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth("nohash", ""))))
         .isInstanceOf(QAuthenticationException.class)
         .hasMessage("Incorrect username or password.");
   }



   /*******************************************************************************
    ** An unknown username is refused only after hashing the given password against
    ** a stand-in hash, like a wrong password, so timing does not reveal users.
    *******************************************************************************/
   @Test
   void testUnknownUserIsHashedLikeAWrongPassword() throws Exception
   {
      String unknownUserHash = TableBasedAuthenticationModule.PasswordHasher.getUnknownUserHash();
      assertTrue(unknownUserHash.startsWith("sha256:100000:"));
      assertEquals(unknownUserHash, TableBasedAuthenticationModule.PasswordHasher.getUnknownUserHash());

      QInstance qInstance = getQInstance();
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);
      assertThatThrownBy(() -> new TableBasedAuthenticationModule().createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth("nobody", PASSWORD))))
         .isInstanceOf(QAuthenticationException.class)
         .hasMessage("Incorrect username or password.");
   }



   /*******************************************************************************
    ** A session is identified to frontends by name and username (never the hash),
    ** and resumes from the sessionUUID key that manageSession's cookie supplies.
    *******************************************************************************/
   @Test
   void testFrontendValuesAndSessionUuidKey() throws Exception
   {
      QInstance qInstance = getQInstance();
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);

      TableBasedAuthenticationModule authModule = new TableBasedAuthenticationModule();
      QSession                       session    = authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, PASSWORD)));
      assertEquals(Map.of("user", Map.of("name", FULL_NAME, "username", USERNAME)), session.getValuesForFrontend());

      QSession resumed = authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.SESSION_UUID_KEY, session.getUuid()));
      assertEquals(session.getUuid(), resumed.getUuid());
      assertEquals(USERNAME, resumed.getUser().getIdReference());
      assertEquals(session.getValuesForFrontend(), resumed.getValuesForFrontend());
   }



   /*******************************************************************************
    ** Logout deletes the session row, so the session can no longer be resumed.
    *******************************************************************************/
   @Test
   void testLogoutDeletesSession() throws Exception
   {
      QInstance qInstance = getQInstance();
      insertTestUser(qInstance, USERNAME, PASSWORD, FULL_NAME);

      TableBasedAuthenticationModule authModule = new TableBasedAuthenticationModule();
      QSession                       session    = authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.BASIC_AUTH_KEY, encodeBasicAuth(USERNAME, PASSWORD)));
      String                         uuid       = session.getUuid();
      assertTrue(InMemoryStateProvider.getInstance().get(Instant.class, new SimpleStateKey<>(uuid)).isPresent());

      authModule.logout(qInstance, null);
      authModule.logout(qInstance, "not-a-session");
      assertNotNull(authModule.createSession(qInstance, Map.of(TableBasedAuthenticationModule.SESSION_ID_KEY, uuid)));

      authModule.logout(qInstance, uuid);
      assertFalse(InMemoryStateProvider.getInstance().get(Instant.class, new SimpleStateKey<>(uuid)).isPresent());
      for(String key : List.of(TableBasedAuthenticationModule.SESSION_ID_KEY, TableBasedAuthenticationModule.SESSION_UUID_KEY))
      {
         assertThatThrownBy(() -> authModule.createSession(qInstance, Map.of(key, uuid)))
            .isInstanceOf(QAuthenticationException.class)
            .hasMessage("Session not found.");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void insertTestUser(QInstance qInstance, String username, String password, String fullName) throws Exception
   {
      QAuthenticationMetaData tableBasedAuthentication = qInstance.getAuthentication();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new Auth0AuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK));
      TestUtils.insertRecords(qInstance.getTable("user"), List.of(new QRecord()
         .withValue("username", username)
         .withValue("fullName", fullName)
         .withValue("passwordHash", TableBasedAuthenticationModule.PasswordHasher.createHashedPassword(password))));
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), tableBasedAuthentication);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String insertTestSession(QInstance qInstance, String username, Instant accessTimestamp) throws Exception
   {
      QAuthenticationMetaData tableBasedAuthentication = qInstance.getAuthentication();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new Auth0AuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK));

      String uuid = UUID.randomUUID().toString();

      GetInput getUserInput = new GetInput();
      getUserInput.setTableName("user");
      getUserInput.setUniqueKey(Map.of("username", username));
      GetOutput getUserOutput = new GetAction().execute(getUserInput);

      TestUtils.insertRecords(qInstance.getTable("session"), List.of(new QRecord()
         .withValue("id", uuid)
         .withValue("userId", getUserOutput.getRecord() == null ? -1 : getUserOutput.getRecord().getValueInteger("id"))
         .withValue("accessTimestamp", accessTimestamp)
         .withValue("passwordHash", TableBasedAuthenticationModule.PasswordHasher.createHashedPassword(PASSWORD))));

      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), tableBasedAuthentication);

      return (uuid);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String encodeBasicAuth(String username, String password)
   {
      Base64.Encoder encoder        = Base64.getEncoder();
      String         originalString = username + ":" + password;
      return (encoder.encodeToString(originalString.getBytes()));
   }



   /*******************************************************************************
    ** utility method to prime a qInstance for these tests
    **
    *******************************************************************************/
   private QInstance getQInstance()
   {
      TableBasedAuthenticationMetaData authenticationMetaData = new TableBasedAuthenticationMetaData();

      QInstance qInstance = TestUtils.defineInstance();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), authenticationMetaData);
      qInstance.addTable(authenticationMetaData.defineStandardUserTable(TestUtils.MEMORY_BACKEND_NAME));
      qInstance.addTable(authenticationMetaData.defineStandardSessionTable(TestUtils.MEMORY_BACKEND_NAME));

      reInitInstanceInContext(qInstance);

      return (qInstance);
   }

}
