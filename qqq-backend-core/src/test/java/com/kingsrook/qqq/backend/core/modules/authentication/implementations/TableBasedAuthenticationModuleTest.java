/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.Auth0AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.TableBasedAuthenticationMetaData;
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
