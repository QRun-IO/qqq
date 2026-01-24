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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.Base64;
import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Integration test for BackChannelLogoutSpecV1.
 *******************************************************************************/
class BackChannelLogoutSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new BackChannelLogoutSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    ** Override to add UserSession table to the QInstance.
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.addTable(new UserSessionMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(qInstance));
      return qInstance;
   }



   /*******************************************************************************
    ** Test that endpoint returns 200 when no logout_token is provided.
    ** Per OIDC spec, back-channel logout should return 200 even on "errors".
    *******************************************************************************/
   @Test
   void testMissingLogoutToken()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/oidc/backchannel-logout")
         .header("Content-Type", "application/x-www-form-urlencoded")
         .asString();

      assertEquals(200, response.getStatus());
   }



   /*******************************************************************************
    ** Test that endpoint returns 200 when an empty logout_token is provided.
    *******************************************************************************/
   @Test
   void testEmptyLogoutToken()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/oidc/backchannel-logout")
         .header("Content-Type", "application/x-www-form-urlencoded")
         .field("logout_token", "")
         .asString();

      assertEquals(200, response.getStatus());
   }



   /*******************************************************************************
    ** Test that endpoint deletes sessions matching the 'sub' claim.
    *******************************************************************************/
   @Test
   void testLogoutBySubClaim() throws Exception
   {
      String userId = "user-123";
      String sessionUuid = UUID.randomUUID().toString();

      /////////////////////////////
      // Insert a session record //
      /////////////////////////////
      QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
      {
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(UserSession.TABLE_NAME);
         insertInput.setRecords(List.of(new QRecord()
            .withValue("uuid", sessionUuid)
            .withValue("userId", userId)
            .withValue("accessToken", "some-token")));
         new InsertAction().execute(insertInput);
      });

      //////////////////////////////////
      // Create a logout token JWT    //
      // (header.payload.signature)   //
      //////////////////////////////////
      String logoutToken = createLogoutToken(userId, null);

      /////////////////////////////
      // Call the logout endpoint //
      /////////////////////////////
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/oidc/backchannel-logout")
         .header("Content-Type", "application/x-www-form-urlencoded")
         .field("logout_token", logoutToken)
         .asString();

      assertEquals(200, response.getStatus());

      //////////////////////////////////
      // Verify session was deleted   //
      //////////////////////////////////
      QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(UserSession.TABLE_NAME);
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size(), "Session should have been deleted");
      });
   }



   /*******************************************************************************
    ** Test that endpoint deletes sessions matching the 'sid' claim in accessToken.
    *******************************************************************************/
   @Test
   void testLogoutBySidClaim() throws Exception
   {
      String sessionId = "sid-456";
      String sessionUuid = UUID.randomUUID().toString();

      ///////////////////////////////////////////////////////
      // Create an access token JWT with the 'sid' claim   //
      ///////////////////////////////////////////////////////
      String accessToken = createAccessTokenWithSid(sessionId);

      /////////////////////////////
      // Insert a session record //
      /////////////////////////////
      QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
      {
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(UserSession.TABLE_NAME);
         insertInput.setRecords(List.of(new QRecord()
            .withValue("uuid", sessionUuid)
            .withValue("userId", "some-user")
            .withValue("accessToken", accessToken)));
         new InsertAction().execute(insertInput);
      });

      //////////////////////////////////////////////////////
      // Create a logout token with only 'sid' claim      //
      //////////////////////////////////////////////////////
      String logoutToken = createLogoutToken(null, sessionId);

      /////////////////////////////
      // Call the logout endpoint //
      /////////////////////////////
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/oidc/backchannel-logout")
         .header("Content-Type", "application/x-www-form-urlencoded")
         .field("logout_token", logoutToken)
         .asString();

      assertEquals(200, response.getStatus());

      //////////////////////////////////
      // Verify session was deleted   //
      //////////////////////////////////
      QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(UserSession.TABLE_NAME);
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size(), "Session should have been deleted");
      });
   }



   /*******************************************************************************
    ** Test that sessions NOT matching the claim are not deleted.
    *******************************************************************************/
   @Test
   void testLogoutDoesNotDeleteUnrelatedSessions() throws Exception
   {
      String sessionUuid1 = UUID.randomUUID().toString();
      String sessionUuid2 = UUID.randomUUID().toString();

      /////////////////////////////
      // Insert two sessions      //
      /////////////////////////////
      QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
      {
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(UserSession.TABLE_NAME);
         insertInput.setRecords(List.of(
            new QRecord()
               .withValue("uuid", sessionUuid1)
               .withValue("userId", "user-to-logout")
               .withValue("accessToken", "token1"),
            new QRecord()
               .withValue("uuid", sessionUuid2)
               .withValue("userId", "user-to-keep")
               .withValue("accessToken", "token2")
         ));
         new InsertAction().execute(insertInput);
      });

      //////////////////////////////////
      // Create logout token for user1 //
      //////////////////////////////////
      String logoutToken = createLogoutToken("user-to-logout", null);

      /////////////////////////////
      // Call the logout endpoint //
      /////////////////////////////
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/oidc/backchannel-logout")
         .header("Content-Type", "application/x-www-form-urlencoded")
         .field("logout_token", logoutToken)
         .asString();

      assertEquals(200, response.getStatus());

      ///////////////////////////////////////////
      // Verify only the matching session was   //
      // deleted, the other remains            //
      ///////////////////////////////////////////
      QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(UserSession.TABLE_NAME);
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size(), "Only one session should remain");
         assertEquals("user-to-keep", queryOutput.getRecords().get(0).getValueString("userId"));
      });
   }



   /*******************************************************************************
    ** Helper to create a minimal logout_token JWT.
    ** Format: header.payload.signature (signature is empty for testing)
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
