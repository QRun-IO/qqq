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


import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreProviderInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.middleware.javalin.LogoutTestProvider;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Integration test for BackChannelLogoutSpecV1.
 *******************************************************************************/
class BackChannelLogoutSpecV1Test extends SpecTestBase
{
   private LogoutTestProvider provider;

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
      provider = new LogoutTestProvider();
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.withInstanceDefaultAuthentication(provider.authentication());
      qInstance.addTable(new UserSessionMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(qInstance));
      qInstance.addTable(new RedirectStateMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(qInstance));
      return qInstance;
   }



   /*******************************************************************************
    ** Test that endpoint returns 400 when no logout_token is provided.
    ** Invalid logout tokens must not be accepted as successful notifications.
    *******************************************************************************/
   @Test
   void testMissingLogoutToken()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/oidc/backchannel-logout")
         .header("Content-Type", "application/x-www-form-urlencoded")
         .asString();

      assertEquals(400, response.getStatus());
   }



   /*******************************************************************************
    ** Test that endpoint returns 400 when an empty logout_token is provided.
    *******************************************************************************/
   @Test
   void testEmptyLogoutToken()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/oidc/backchannel-logout")
         .header("Content-Type", "application/x-www-form-urlencoded")
         .field("logout_token", "")
         .asString();

      assertEquals(400, response.getStatus());
   }



   /*******************************************************************************
    ** Test that endpoint deletes sessions matching the 'sub' claim.
    *******************************************************************************/
   @Test
   void testLogoutBySubClaim() throws Exception
   {
      String userId = "user-123";
      String sessionUuid = UUID.randomUUID().toString();
      String accessToken = provider.accessToken(userId, null);

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
            .withValue("accessToken", accessToken)));
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
      String accessToken1 = provider.accessToken("user-to-logout", null);
      String accessToken2 = provider.accessToken("user-to-keep", null);

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
               .withValue("accessToken", accessToken1),
            new QRecord()
               .withValue("uuid", sessionUuid2)
               .withValue("userId", "user-to-keep")
               .withValue("accessToken", accessToken2)
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
    ** The actual unsecured HTTP endpoint rejects invalid token identities and claims.
    *******************************************************************************/
   @Test
   void testInvalidTokensCannotDeleteSessions() throws Exception
   {
      String accessToken = provider.accessToken("protected-user", "protected-sid");
      QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
         new InsertAction().execute(new InsertInput().withTableName(UserSession.TABLE_NAME)
            .withRecords(List.of(new QRecord().withValue("uuid", "protected-session").withValue("userId", "protected-user")
               .withValue("accessToken", accessToken)))));
      for(var entry : provider.invalidTokens("protected-user", "protected-sid").entrySet())
      {
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/oidc/backchannel-logout")
            .field("logout_token", entry.getValue()).asString();
         assertEquals(400, response.getStatus(), entry.getKey());
         QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
            assertEquals(1, new QueryAction().execute(new QueryInput(UserSession.TABLE_NAME)).getRecords().size(), entry.getKey()));
      }
   }



   /*******************************************************************************
    ** A valid HTTP event succeeds once; replay cannot log out a later session.
    *******************************************************************************/
   @Test
   void testReplayCannotDeleteNewSession() throws Exception
   {
      String token = provider.logoutToken("replay-user", null);
      assertEquals(200, Unirest.post(getBaseUrlAndPath() + "/oidc/backchannel-logout").field("logout_token", token).asString().getStatus());
      String accessToken = provider.accessToken("replay-user", "new-sid");
      QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
         new InsertAction().execute(new InsertInput().withTableName(UserSession.TABLE_NAME)
            .withRecords(List.of(new QRecord().withValue("uuid", "new-session").withValue("userId", "replay-user").withValue("accessToken", accessToken)))));
      assertEquals(400, Unirest.post(getBaseUrlAndPath() + "/oidc/backchannel-logout").field("logout_token", token).asString().getStatus());
      QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
         assertEquals(1, new QueryAction().execute(new QueryInput(UserSession.TABLE_NAME)).getRecords().size()));
   }



   /*******************************************************************************
    ** Authenticate real HTTP requests through the ordinary session resume path.
    *******************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new TableMetaDataSpecV1()));
   }



   /*******************************************************************************
    ** Removing the row must also revoke a previously warmed access-token cache.
    *******************************************************************************/
   @Test
   void testValidLogoutRevokesWarmedHttpSession() throws Exception
   {
      assertWarmedHttpLogout(false, false);
   }



   /*******************************************************************************
    ** Optional session-store entries must not authorize requests after logout.
    *******************************************************************************/
   @Test
   void testValidLogoutRevokesWarmedSessionStore() throws Exception
   {
      assertWarmedHttpLogout(true, false);
   }



   /*******************************************************************************
    ** Failed store eviction retains authoritative data and permits event retry.
    *******************************************************************************/
   @Test
   void testSessionStoreEvictionFailureCanRetry() throws Exception
   {
      assertWarmedHttpLogout(true, true);
   }



   /*******************************************************************************
    ** Seed owned sessions, warm actual HTTP authentication, then revoke one user.
    *******************************************************************************/
   private void assertWarmedHttpLogout(Boolean useStore, Boolean failRemoval) throws Exception
   {
      var registry = QSessionStoreRegistry.getInstance();
      var previous = registry.getProvider();
      OwnedSessionStore store = new OwnedSessionStore();
      String sessionUuid = UUID.randomUUID().toString();
      String unrelatedUuid = UUID.randomUUID().toString();
      String accessToken = provider.accessToken("warm-user", "warm-sid");
      String otherToken = provider.accessToken("other-user", "other-sid");
      ((OAuth2AuthenticationMetaData) serverQInstance.getAuthentication()).setSessionStoreEnabled(useStore);
      registry.register(store);
      try
      {
         QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
            new InsertAction().execute(new InsertInput(UserSession.TABLE_NAME).withRecords(List.of(
               new QRecord().withValue("uuid", sessionUuid).withValue("userId", "warm-user").withValue("accessToken", accessToken),
               new QRecord().withValue("uuid", unrelatedUuid).withValue("userId", "other-user").withValue("accessToken", otherToken)))));
         assertEquals(200, sessionStatus(sessionUuid));
         assertEquals(200, sessionStatus(unrelatedUuid));
         if(useStore)
         {
            assertTrue(store.sessions.containsKey(sessionUuid));
         }
         String token = provider.logoutToken("warm-user", "warm-sid");
         if(failRemoval)
         {
            store.rejectRemoval = true;
            assertEquals(500, logoutStatus(token));
            QContext.withTemporaryContext(new CapturedContext(serverQInstance, new QSystemUserSession()), () ->
               assertEquals(2, new QueryAction().execute(new QueryInput(UserSession.TABLE_NAME)).getRecords().size()));
            assertEquals(200, sessionStatus(sessionUuid));
            store.rejectRemoval = false;
         }
         assertEquals(200, logoutStatus(token));
         assertEquals(401, sessionStatus(sessionUuid));
         assertEquals(200, sessionStatus(unrelatedUuid));
         assertEquals(400, logoutStatus(token));
      }
      finally
      {
         registry.clear();
         previous.ifPresent(registry::register);
      }
   }



   /*******************************************************************************
    ** Return only HTTP status, keeping credentials out of assertion messages.
    *******************************************************************************/
   private Integer sessionStatus(String sessionUuid)
   {
      return (Unirest.get(getBaseUrlAndPath() + "/metaData/table/person")
         .header("Cookie", "sessionUUID=" + sessionUuid).asString().getStatus());
   }



   /*******************************************************************************
    ** Send the original form-encoded endpoint contract without logging its token.
    *******************************************************************************/
   private Integer logoutStatus(String token)
   {
      return (Unirest.post(getBaseUrlAndPath() + "/oidc/backchannel-logout")
         .field("logout_token", token).asString().getStatus());
   }



   /*******************************************************************************
    ** Helper to create a minimal logout_token JWT.
    ** Signed by the owned HTTP provider fixture.
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



   /*******************************************************************************
    ** Release only this test's provider; SpecTestBase owns the application server.
    *******************************************************************************/
   @AfterEach
   void closeProvider()
   {
      provider.close();
   }

   /*******************************************************************************
    ** Real in-memory provider for the existing session-store SPI and failure path.
    *******************************************************************************/
   private static class OwnedSessionStore implements QSessionStoreProviderInterface
   {
      private final Map<String, QSession> sessions = new ConcurrentHashMap<>();
      private Boolean rejectRemoval = false;



      /*******************************************************************************
       ** Keep actual sessions returned by the native authentication module.
       *******************************************************************************/
      @Override
      public void store(String uuid, QSession session, Duration ttl)
      {
         sessions.put(uuid, session);
      }



      /*******************************************************************************
       ** Tests use this cache only within a single short-lived scenario.
       *******************************************************************************/
      @Override
      public Optional<QSession> load(String uuid)
      {
         return Optional.ofNullable(sessions.get(uuid));
      }



      /*******************************************************************************
       ** Fail before eviction so retry behavior can be asserted with the same token.
       *******************************************************************************/
      @Override
      public void remove(String uuid)
      {
         if(rejectRemoval)
         {
            throw new IllegalStateException("Owned session-store eviction failure");
         }
         sessions.remove(uuid);
      }



      /*******************************************************************************
       ** No wall-clock expiration is simulated by this eviction fixture.
       *******************************************************************************/
      @Override
      public void touch(String uuid)
      {
      }



      /*******************************************************************************
       ** Supply the configured provider contract without external storage.
       *******************************************************************************/
      @Override
      public Duration getDefaultTtl()
      {
         return Duration.ofMinutes(5);
      }
   }

}
