/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.Base64;
import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.BackChannelLogoutInput;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BackChannelLogoutResponseV1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for BackChannelLogoutExecutor.
 *******************************************************************************/
class BackChannelLogoutExecutorTest
{
   private QInstance qInstance;



   /***************************************************************************
    **
    ***************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      MemoryRecordStore.fullReset();
      qInstance = TestUtils.defineInstance();
      qInstance.addTable(new UserSessionMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(qInstance));
      QContext.init(qInstance, new QSystemUserSession());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
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
      // Should not throw - just return successfully //
      /////////////////////////////////////////////////
      executor.execute(input, output);
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
      // Should not throw - just return successfully //
      /////////////////////////////////////////////////
      executor.execute(input, output);
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
      // Should not throw - just return successfully //
      /////////////////////////////////////////////////
      executor.execute(input, output);
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
         .withValue("accessToken", "token")));
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
            .withValue("accessToken", "token1"),
         new QRecord()
            .withValue("uuid", UUID.randomUUID().toString())
            .withValue("userId", userId)
            .withValue("accessToken", "token2"),
         new QRecord()
            .withValue("uuid", UUID.randomUUID().toString())
            .withValue("userId", userId)
            .withValue("accessToken", "token3")
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
            .withValue("accessToken", "token1"),
         new QRecord()
            .withValue("uuid", UUID.randomUUID().toString())
            .withValue("userId", "user-to-keep")
            .withValue("accessToken", "token2")
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
      String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
      String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes());
      String logoutToken = header + "." + payload + ".";

      BackChannelLogoutExecutor executor = new BackChannelLogoutExecutor();
      BackChannelLogoutInput input = new BackChannelLogoutInput();
      input.setLogoutToken(logoutToken);

      BackChannelLogoutResponseV1 output = new BackChannelLogoutResponseV1();
      executor.execute(input, output);

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
    ** Helper to create a minimal logout_token JWT.
    *******************************************************************************/
   private String createLogoutToken(String sub, String sid)
   {
      String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());

      StringBuilder payloadBuilder = new StringBuilder("{");
      boolean hasField = false;
      if(sub != null)
      {
         payloadBuilder.append("\"sub\":\"").append(sub).append("\"");
         hasField = true;
      }
      if(sid != null)
      {
         if(hasField)
         {
            payloadBuilder.append(",");
         }
         payloadBuilder.append("\"sid\":\"").append(sid).append("\"");
      }
      payloadBuilder.append("}");

      String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBuilder.toString().getBytes());

      return header + "." + payload + ".";
   }



   /*******************************************************************************
    ** Helper to create an access token JWT with an embedded 'sid' claim.
    *******************************************************************************/
   private String createAccessTokenWithSid(String sid)
   {
      String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
      String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sid\":\"" + sid + "\"}").getBytes());
      return header + "." + payload + ".";
   }

}
