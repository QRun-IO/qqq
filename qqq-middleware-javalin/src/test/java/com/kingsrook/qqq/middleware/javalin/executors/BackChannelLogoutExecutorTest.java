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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreDeleteCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.middleware.javalin.LogoutTestProvider;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.BackChannelLogoutInput;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BackChannelLogoutResponseV1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for BackChannelLogoutExecutor.
 *******************************************************************************/
class BackChannelLogoutExecutorTest
{
   private QInstance qInstance;
   private LogoutTestProvider provider;



   /***************************************************************************
    **
    ***************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      MemoryRecordStore.fullReset();
      provider = new LogoutTestProvider();
      qInstance = TestUtils.defineInstance();
      qInstance.withInstanceDefaultAuthentication(provider.authentication());
      qInstance.addTable(new UserSessionMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(qInstance));
      qInstance.addTable(new RedirectStateMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(qInstance));
      QContext.init(qInstance, new QSystemUserSession());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
      provider.close();
      QContext.clear();
      MemoryRecordStore.fullReset();
   }



   /*******************************************************************************
    ** Test that null logout token is handled gracefully.
    *******************************************************************************/
   @Test
   void testNullLogoutToken() throws Exception
   {
      BackChannelLogoutExecutor executor = new BackChannelLogoutExecutor();
      BackChannelLogoutInput input = new BackChannelLogoutInput();
      input.setLogoutToken(null);

      BackChannelLogoutResponseV1 output = new BackChannelLogoutResponseV1();

      /////////////////////////////////////////////////
      // Reject before accessing session storage.              //
      /////////////////////////////////////////////////
      assertThrows(QBadRequestException.class, () -> executor.execute(input, output));
   }



   /*******************************************************************************
    ** Test that empty logout token is handled gracefully.
    *******************************************************************************/
   @Test
   void testEmptyLogoutToken() throws Exception
   {
      BackChannelLogoutExecutor executor = new BackChannelLogoutExecutor();
      BackChannelLogoutInput input = new BackChannelLogoutInput();
      input.setLogoutToken("");

      BackChannelLogoutResponseV1 output = new BackChannelLogoutResponseV1();

      /////////////////////////////////////////////////
      // Reject before accessing session storage.              //
      /////////////////////////////////////////////////
      assertThrows(QBadRequestException.class, () -> executor.execute(input, output));
   }



   /*******************************************************************************
    ** Test that a malformed JWT is handled gracefully.
    *******************************************************************************/
   @Test
   void testMalformedJwt() throws Exception
   {
      BackChannelLogoutExecutor executor = new BackChannelLogoutExecutor();
      BackChannelLogoutInput input = new BackChannelLogoutInput();
      input.setLogoutToken("not-a-valid-jwt");

      BackChannelLogoutResponseV1 output = new BackChannelLogoutResponseV1();

      /////////////////////////////////////////////////
      // Reject before accessing session storage.              //
      /////////////////////////////////////////////////
      assertThrows(QBadRequestException.class, () -> executor.execute(input, output));
   }



   /*******************************************************************************
    ** Test that sessions are deleted by 'sub' claim.
    *******************************************************************************/
   @Test
   void testDeleteBySubClaim() throws Exception
   {
      String userId = "user-sub-123";
      String sessionUuid = UUID.randomUUID().toString();

      ///////////////////////////////////
      // Insert a session with userId  //
      ///////////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(UserSession.TABLE_NAME);
      insertInput.setRecords(List.of(new QRecord()
         .withValue("uuid", sessionUuid)
         .withValue("userId", userId)
         .withValue("accessToken", provider.accessToken("user-sub-123", null))));
      new InsertAction().execute(insertInput);

      ////////////////////////////////////
      // Execute the logout with 'sub'  //
      ////////////////////////////////////
      BackChannelLogoutExecutor executor = new BackChannelLogoutExecutor();
      BackChannelLogoutInput input = new BackChannelLogoutInput();
      input.setLogoutToken(createLogoutToken(userId, null));

      BackChannelLogoutResponseV1 output = new BackChannelLogoutResponseV1();
      executor.execute(input, output);

      //////////////////////////////////////
      // Verify the session was deleted   //
      //////////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(UserSession.TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(0, queryOutput.getRecords().size());
   }



   /*******************************************************************************
    ** Test that sessions are deleted by 'sid' claim in accessToken.
    *******************************************************************************/
   @Test
   void testDeleteBySidClaim() throws Exception
   {
      String sid = "oidc-sid-789";
      String sessionUuid = UUID.randomUUID().toString();

      ///////////////////////////////////////////
      // Insert a session with accessToken     //
      // containing the 'sid' claim            //
      ///////////////////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(UserSession.TABLE_NAME);
      insertInput.setRecords(List.of(new QRecord()
         .withValue("uuid", sessionUuid)
         .withValue("userId", "some-user")
         .withValue("accessToken", createAccessTokenWithSid(sid))));
      new InsertAction().execute(insertInput);

      /////////////////////////////////////////
      // Execute the logout with only 'sid'  //
      /////////////////////////////////////////
      BackChannelLogoutExecutor executor = new BackChannelLogoutExecutor();
      BackChannelLogoutInput input = new BackChannelLogoutInput();
      input.setLogoutToken(createLogoutToken(null, sid));

      BackChannelLogoutResponseV1 output = new BackChannelLogoutResponseV1();
      executor.execute(input, output);

      //////////////////////////////////////
      // Verify the session was deleted   //
      //////////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(UserSession.TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(0, queryOutput.getRecords().size());
   }



   /*******************************************************************************
    ** Test that multiple sessions for the same user are all deleted.
    *******************************************************************************/
   @Test
   void testDeleteMultipleSessions() throws Exception
   {
      String userId = "user-multi-session";

      ////////////////////////////////////
      // Insert multiple sessions       //
      ////////////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(UserSession.TABLE_NAME);
      insertInput.setRecords(List.of(
         new QRecord()
            .withValue("uuid", UUID.randomUUID().toString())
            .withValue("userId", userId)
            .withValue("accessToken", provider.accessToken(userId, "first")),
         new QRecord()
            .withValue("uuid", UUID.randomUUID().toString())
            .withValue("userId", userId)
            .withValue("accessToken", provider.accessToken(userId, "second")),
         new QRecord()
            .withValue("uuid", UUID.randomUUID().toString())
            .withValue("userId", userId)
            .withValue("accessToken", provider.accessToken(userId, "third"))
      ));
      new InsertAction().execute(insertInput);

      ////////////////////////////////////
      // Execute the logout             //
      ////////////////////////////////////
      BackChannelLogoutExecutor executor = new BackChannelLogoutExecutor();
      BackChannelLogoutInput input = new BackChannelLogoutInput();
      input.setLogoutToken(createLogoutToken(userId, null));

      BackChannelLogoutResponseV1 output = new BackChannelLogoutResponseV1();
      executor.execute(input, output);

      /////////////////////////////////////////
      // Verify all sessions were deleted    //
      /////////////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(UserSession.TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(0, queryOutput.getRecords().size());
   }



   /*******************************************************************************
    ** Test that unrelated sessions are not deleted.
    *******************************************************************************/
   @Test
   void testDoesNotDeleteUnrelatedSessions() throws Exception
   {
      /////////////////////////////////////
      // Insert sessions for two users   //
      /////////////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(UserSession.TABLE_NAME);
      insertInput.setRecords(List.of(
         new QRecord()
            .withValue("uuid", UUID.randomUUID().toString())
            .withValue("userId", "user-to-logout")
            .withValue("accessToken", provider.accessToken("user-to-logout", null)),
         new QRecord()
            .withValue("uuid", UUID.randomUUID().toString())
            .withValue("userId", "user-to-keep")
            .withValue("accessToken", provider.accessToken("user-to-keep", null))
      ));
      new InsertAction().execute(insertInput);

      ////////////////////////////////////
      // Execute logout for user1 only  //
      ////////////////////////////////////
      BackChannelLogoutExecutor executor = new BackChannelLogoutExecutor();
      BackChannelLogoutInput input = new BackChannelLogoutInput();
      input.setLogoutToken(createLogoutToken("user-to-logout", null));

      BackChannelLogoutResponseV1 output = new BackChannelLogoutResponseV1();
      executor.execute(input, output);

      /////////////////////////////////////////
      // Verify only one session remains     //
      /////////////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(UserSession.TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size());
      assertEquals("user-to-keep", queryOutput.getRecords().get(0).getValueString("userId"));
   }



   /*******************************************************************************
    ** Test that JWT without sub or sid is handled gracefully.
    *******************************************************************************/
   @Test
   void testJwtWithoutSubOrSid() throws Exception
   {
      ////////////////////////////////////
      // Insert a session               //
      ////////////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(UserSession.TABLE_NAME);
      insertInput.setRecords(List.of(new QRecord()
         .withValue("uuid", UUID.randomUUID().toString())
         .withValue("userId", "some-user")
         .withValue("accessToken", "token")));
      new InsertAction().execute(insertInput);

      //////////////////////////////////////
      // Execute logout with empty JWT    //
      //////////////////////////////////////
      String logoutToken = provider.logoutToken(null, null);

      BackChannelLogoutExecutor executor = new BackChannelLogoutExecutor();
      BackChannelLogoutInput input = new BackChannelLogoutInput();
      input.setLogoutToken(logoutToken);

      BackChannelLogoutResponseV1 output = new BackChannelLogoutResponseV1();
      assertThrows(QBadRequestException.class, () -> executor.execute(input, output));

      /////////////////////////////////////////
      // Verify session was NOT deleted      //
      // (no sub or sid to match on)         //
      /////////////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(UserSession.TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size());
   }



   /*******************************************************************************
    ** Each invalid signed/unsigned claim set leaves sessions and caller context intact.
    *******************************************************************************/
   @Test
   void testInvalidTokensCannotDeleteSessions() throws Exception
   {
      String userId = "protected-user";
      new InsertAction().execute(new InsertInput().withTableName(UserSession.TABLE_NAME)
         .withRecords(List.of(new QRecord().withValue("uuid", "protected-session").withValue("userId", userId)
            .withValue("accessToken", provider.accessToken(userId, "protected-sid")))));
      var previousSession = QContext.getQSession();
      for(var entry : provider.invalidTokens(userId, "protected-sid").entrySet())
      {
         assertThrows(QBadRequestException.class, () -> new BackChannelLogoutExecutor().execute(
            new BackChannelLogoutInput().withLogoutToken(entry.getValue()), new BackChannelLogoutResponseV1()), entry.getKey());
         assertSame(previousSession, QContext.getQSession(), entry.getKey());
         assertEquals(1, new QueryAction().execute(new QueryInput(UserSession.TABLE_NAME)).getRecords().size(), entry.getKey());
      }
   }



   /*******************************************************************************
    ** Replaying a processed event must not invalidate a newly created user session.
    *******************************************************************************/
   @Test
   void testReplayRejectedAndContextRestored() throws Exception
   {
      String token = provider.logoutToken("replay-user", null);
      var previousSession = QContext.getQSession();
      BackChannelLogoutInput input = new BackChannelLogoutInput().withLogoutToken(token);
      new BackChannelLogoutExecutor().execute(input, new BackChannelLogoutResponseV1());
      assertSame(previousSession, QContext.getQSession());
      new InsertAction().execute(new InsertInput().withTableName(UserSession.TABLE_NAME)
         .withRecords(List.of(new QRecord().withValue("uuid", "new-session").withValue("userId", "replay-user")
            .withValue("accessToken", provider.accessToken("replay-user", null)))));
      assertThrows(QBadRequestException.class, () -> new BackChannelLogoutExecutor().execute(input, new BackChannelLogoutResponseV1()));
      assertSame(previousSession, QContext.getQSession());
      assertEquals(1, new QueryAction().execute(new QueryInput(UserSession.TABLE_NAME)).getRecords().size());
   }



   /*******************************************************************************
    ** Both claims must match; other issuers and unreadable sessions remain untouched.
    *******************************************************************************/
   @Test
   void testIssuerAndSessionScopeWithCustomStorage() throws Exception
   {
      String tableName = "customSessions";
      qInstance.addTable(new UserSessionMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(qInstance).withName(tableName));
      qInstance.withInstanceDefaultAuthentication(provider.authentication().withUserSessionTableName(tableName));
      try(LogoutTestProvider otherProvider = new LogoutTestProvider())
      {
         new InsertAction().execute(new InsertInput().withTableName(tableName).withRecords(List.of(
            new QRecord().withValue("uuid", "match").withValue("userId", "mapped-application-user").withValue("accessToken", provider.accessToken("user", "sid")),
            new QRecord().withValue("uuid", "other-sid").withValue("userId", "user").withValue("accessToken", provider.accessToken("user", "other")),
            new QRecord().withValue("uuid", "other-user").withValue("userId", "other").withValue("accessToken", provider.accessToken("other", "sid")),
            new QRecord().withValue("uuid", "other-issuer").withValue("userId", "user").withValue("accessToken", otherProvider.accessToken("user", "sid")),
            new QRecord().withValue("uuid", "malformed").withValue("userId", "user").withValue("accessToken", "malformed"),
            new QRecord().withValue("uuid", "missing-token").withValue("userId", "user"))));
         new BackChannelLogoutExecutor().execute(new BackChannelLogoutInput().withLogoutToken(provider.logoutToken("user", "sid")), new BackChannelLogoutResponseV1());
         assertEquals(List.of("malformed", "missing-token", "other-issuer", "other-sid", "other-user"),
            new QueryAction().execute(new QueryInput(tableName).withShouldOmitHiddenFields(false)).getRecords().stream().map(record -> record.getValueString("uuid")).sorted().toList());
      }
   }



   /*******************************************************************************
    ** A storage error preserves caller context and permits retrying the same event.
    *******************************************************************************/
   @Test
   void testFailedDeletionCanBeRetried() throws Exception
   {
      new InsertAction().execute(new InsertInput().withTableName(UserSession.TABLE_NAME)
         .withRecords(List.of(new QRecord().withValue("uuid", "retry-session").withValue("userId", "user")
            .withValue("accessToken", provider.accessToken("user", null)))));
      var table = qInstance.getTable(UserSession.TABLE_NAME);
      table.withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(RejectDelete.class));
      var previousSession = QContext.getQSession();
      var input = new BackChannelLogoutInput().withLogoutToken(provider.logoutToken("user", null));
      assertThrows(QException.class, () -> new BackChannelLogoutExecutor().execute(input, new BackChannelLogoutResponseV1()));
      assertSame(previousSession, QContext.getQSession());
      assertEquals(1, new QueryAction().execute(new QueryInput(UserSession.TABLE_NAME)).getRecords().size());
      table.getCustomizers().remove(TableCustomizers.PRE_DELETE_RECORD.getRole());
      new BackChannelLogoutExecutor().execute(input, new BackChannelLogoutResponseV1());
      assertSame(previousSession, QContext.getQSession());
      assertEquals(0, new QueryAction().execute(new QueryInput(UserSession.TABLE_NAME)).getRecords().size());
   }



   /*******************************************************************************
    ** Synthetic per-record failure exercises DeleteOutput rather than thrown errors.
    *******************************************************************************/
   public static class RejectDelete extends AbstractPreDeleteCustomizer
   {
      /*******************************************************************************
       ** Keep the session and report an ordinary per-record deletion failure.
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         records.forEach(record -> record.addError(new BadInputStatusMessage("Synthetic deletion failure")));
         return (records);
      }
   }



   /*******************************************************************************
    ** Missing provider configuration cannot authorize a privileged storage operation.
    *******************************************************************************/
   @Test
   void testMissingConfigurationRejected() throws Exception
   {
      String token = provider.logoutToken("user", null);
      qInstance.withInstanceDefaultAuthentication(provider.authentication().withBaseUrl(null));
      assertThrows(QBadRequestException.class, () -> new BackChannelLogoutExecutor().execute(
         new BackChannelLogoutInput().withLogoutToken(token), new BackChannelLogoutResponseV1()));
   }



   /*******************************************************************************
    ** Helper to create a minimal logout_token JWT.
    *******************************************************************************/
   private String createLogoutToken(String sub, String sid) throws Exception
   {
      return (provider.logoutToken(sub, sid));
   }



   /*******************************************************************************
    ** Session access token with the configured issuer and a session identifier.
    *******************************************************************************/
   private String createAccessTokenWithSid(String sid) throws Exception
   {
      return (provider.accessToken("some-user", sid));
   }

}
