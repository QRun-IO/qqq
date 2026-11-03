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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2NativeSessionLoginMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2NativeSessionLogoutMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.NativeSessionLogoutSupport;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiInProcessSessions;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Actual HTTP and durable session deletion; only remote discovery is substituted.
 *******************************************************************************/
class NativeSessionLogoutSpecV1Test extends SpecTestBase
{
   private static final String CALLBACK = "com.example.handheld:/oauth2/callback";
   private static final String TOKEN = "synthetic-native-id-token-private-canary";



   /*******************************************************************************
    ** Explicit migrated system table and opt-in policy, with no refresh grant.
    *******************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      Provider.discoveryFailure = false;
      QInstance instance = TestUtils.defineInstance();
      instance.addTable(new UserSessionMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).withNativeLogoutContext().produce(instance));
      instance.addTable(new RedirectStateMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(instance));
      OAuth2AuthenticationMetaData auth = new OAuth2AuthenticationMetaData().withBaseUrl("https://internal.example.test")
         .withClientId("native-session-client").withClientSecret("synthetic-private-client-secret").withScopes("openid")
         .withUserSessionTableName(UserSession.TABLE_NAME).withRedirectStateTableName(RedirectStateMetaDataProducer.TABLE_NAME)
         .withRefreshTokenEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]))
         .withNativeSessionLogin(new OAuth2NativeSessionLoginMetaData().withIssuer("https://login.example.test")
            .withDiscoveryUrl("https://login.example.test/discovery").withRedirectUris(List.of(CALLBACK)).withRequireNonceValidation(true))
         .withNativeSessionLogout(new OAuth2NativeSessionLogoutMetaData().withPostLogoutRedirectUris(List.of(CALLBACK)));
      auth.setModuleCodeReference(new QCodeReference(Provider.class));
      auth.setName("native-session-provider");
      instance.withInstanceDefaultAuthentication(auth);
      return (instance);
   }



   /*******************************************************************************
    ** Logout under the existing API major version.
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new LogoutSpecV1());
   }



   /*******************************************************************************
    ** Exercise capability, metadata and stale-cookie resume on the same server.
    *******************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new VersionSpecV1(), new AuthenticationMetaDataSpecV1(), new ManageSessionSpecV1()));
   }



   /*******************************************************************************
    ** Additive minor version retains v1 routing.
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    ** The only hint-bearing response is for an authenticated opt-in cookie request.
    *******************************************************************************/
   @Test
   void nativeLogoutReturnsTypedHintAfterInvalidation() throws Exception
   {
      String uuid = seed();
      HttpResponse<String> response = logout(uuid, request(CALLBACK), null);
      assertEquals(200, response.getStatus());
      assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));
      JSONObject body = new JSONObject(response.getBody());
      assertEquals(Set.of("nativeLogoutData"), body.keySet());
      JSONObject data = body.getJSONObject("nativeLogoutData");
      assertEquals(Set.of("issuer", "endSessionEndpoint", "postLogoutRedirectUri", "idTokenHint"), data.keySet());
      assertEquals(TOKEN, data.getString("idTokenHint"));
      assertEquals(CALLBACK, data.getString("postLogoutRedirectUri"));
      assertEquals("https://login.example.test", data.getString("issuer"));
      assertEquals("https://login.example.test/logout", data.getString("endSessionEndpoint"));
      assertEquals(401, Unirest.post(getBaseUrlAndPath() + "/manageSession").header("Cookie", "sessionUUID=" + uuid)
         .header("Content-Type", "application/json").body("{}").asString().getStatus());
      assertEquals("{}", logout(uuid, request(CALLBACK), null).getBody());
   }



   /*******************************************************************************
    ** Wrong, absent, malformed and mixed identities cannot retrieve private hints.
    *******************************************************************************/
   @Test
   void invalidOptionalRequestsStillLogoutWithoutHint() throws Exception
   {
      for(String body : List.of("{}", "{", request("com.attacker:/callback"),
         new JSONObject().put("nativeLogout", new JSONObject().put("postLogoutRedirectUri", CALLBACK).put("sessionUUID", "forged")).toString()))
      {
         String uuid = seed();
         HttpResponse<String> response = logout(uuid, body, null);
         assertEquals(200, response.getStatus());
         assertEquals("{}", response.getBody());
      }
      assertEquals("{}", logout(null, request(CALLBACK), null).getBody());
      assertEquals("{}", logout("not-a-session", request(CALLBACK), null).getBody());
      assertEquals("{}", logout(seed(), request(CALLBACK), "Bearer synthetic-invalid").getBody());
      HttpResponse<String> bearerOnly = logout(null, request(CALLBACK), "Bearer synthetic-invalid");
      assertFalse(bearerOnly.getBody().contains(TOKEN));
      assertEquals("{}", logout(seed(-60), request(CALLBACK), null).getBody());
   }



   /*******************************************************************************
    ** A valid internal session cannot substitute for authenticating a cookie.
    *******************************************************************************/
   @Test
   void validInProcessCredentialNeverReleasesCookieLogoutHint() throws Exception
   {
      for(long lifetime : new long[] { 3600, -60 })
      {
         String uuid = seed(lifetime);
         QSession internal = new QSession().withUser(new QUser().withIdReference("native-user"));
         internal.setUuid(uuid);
         String token = UiApiInProcessSessions.register(internal, Duration.ofMinutes(1));
         try
         {
            assertTrue(UiApiInProcessSessions.resolve(token).isPresent());
            HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/logout")
               .header("Cookie", "sessionUUID=" + uuid).header(UiApiInProcessSessions.HEADER, token)
               .header("Content-Type", "application/json").body(request(CALLBACK)).asString();
            assertEquals(200, response.getStatus());
            assertEquals("{}", response.getBody());
            assertEquals(401, Unirest.post(getBaseUrlAndPath() + "/manageSession").header("Cookie", "sessionUUID=" + uuid)
               .header("Content-Type", "application/json").body("{}").asString().getStatus());
         }
         finally
         {
            UiApiInProcessSessions.revoke(token);
         }
      }
   }



   /*******************************************************************************
    ** Alternate credentials remain ineligible regardless of current precedence.
    *******************************************************************************/
   @Test
   void apiKeyAndAuthorizationQueryCannotEnableNativeLogout() throws Exception
   {
      String uuid = seed();
      HttpResponse<String> apiKey = Unirest.post(getBaseUrlAndPath() + "/logout")
         .header("Cookie", "sessionUUID=" + uuid).header("x-api-key", "synthetic-api-key")
         .header("Content-Type", "application/json").body(request(CALLBACK)).asString();
      assertEquals(200, apiKey.getStatus());
      assertEquals("{}", apiKey.getBody());
      for(String query : List.of("?code=synthetic", "?state=synthetic", "?code=synthetic&state=synthetic"))
      {
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/logout" + query)
            .header("Cookie", "sessionUUID=" + seed()).header("Content-Type", "application/json")
            .body(request(CALLBACK)).asString();
         assertEquals(200, response.getStatus());
         assertEquals("{}", response.getBody());
      }
   }



   /*******************************************************************************
    ** Separate configured feature and public descriptor never contain credentials.
    *******************************************************************************/
   @Test
   void capabilityIsConditionalAndVersioned()
   {
      JSONObject version = new JSONObject(Unirest.get(getBaseUrlAndPath() + "/version").asString().getBody());
      assertEquals("1.10.0", version.getString("apiVersion"));
      assertTrue(version.getJSONArray("features").toList().contains("nativeSessionLogout"));
      String body = Unirest.get(getBaseUrlAndPath() + "/metaData/authentication").asString().getBody();
      JSONObject data = new JSONObject(body).getJSONObject("values").getJSONObject("nativeSessionLogout");
      assertEquals(Set.of("flow", "postLogoutRedirectUris"), data.keySet());
      assertEquals("OIDC_RP_INITIATED_LOGOUT", data.getString("flow"));
      assertEquals(Set.of("issuer", "endSessionEndpoint", "postLogoutRedirectUri", "idTokenHint"),
         new com.kingsrook.qqq.middleware.javalin.specs.v1.responses.LogoutResponseV1().toSchema()
            .getProperties().get("nativeLogoutData").getProperties().keySet());
      assertEquals(List.of(CALLBACK), data.getJSONArray("postLogoutRedirectUris").toList());
      assertFalse(body.contains(TOKEN));
      assertFalse(body.contains("synthetic-private-client-secret"));
      ((OAuth2AuthenticationMetaData) serverQInstance.getAuthentication()).getNativeSessionLogin().setRequireNonceValidation(false);
      JSONObject legacy = new JSONObject(Unirest.get(getBaseUrlAndPath() + "/version").asString().getBody());
      assertTrue(legacy.getJSONArray("features").toList().contains("nativeSessionLogin"));
      assertFalse(legacy.getJSONArray("features").toList().contains("nativeSessionLogout"));
      assertFalse(new JSONObject(Unirest.get(getBaseUrlAndPath() + "/metaData/authentication").asString().getBody())
         .getJSONObject("values").has("nativeSessionLogout"));
      ((OAuth2AuthenticationMetaData) serverQInstance.getAuthentication()).getNativeSessionLogin().setRequireNonceValidation(true);
      ((OAuth2AuthenticationMetaData) serverQInstance.getAuthentication()).setNativeSessionLogout(null);
      JSONObject unavailable = new JSONObject(Unirest.get(getBaseUrlAndPath() + "/version").asString().getBody());
      assertFalse(unavailable.getJSONArray("features").toList().contains("nativeSessionLogout"));
      assertFalse(new JSONObject(Unirest.get(getBaseUrlAndPath() + "/metaData/authentication").asString().getBody())
         .getJSONObject("values").has("nativeSessionLogout"));
   }



   /*******************************************************************************
    ** Provider failure cannot leave the QQQ session usable.
    *******************************************************************************/
   @Test
   void discoveryFailureStillInvalidatesLocalSession() throws Exception
   {
      String uuid = seed();
      Provider.discoveryFailure = true;
      HttpResponse<String> response = logout(uuid, request(CALLBACK), null);
      assertEquals(200, response.getStatus());
      assertEquals("{}", response.getBody());
      assertEquals(401, Unirest.post(getBaseUrlAndPath() + "/manageSession").header("Cookie", "sessionUUID=" + uuid)
         .header("Content-Type", "application/json").body("{}").asString().getStatus());
   }



   /*******************************************************************************
    ** Failed deletion cannot release a hint or claim successful server invalidation.
    *******************************************************************************/
   @Test
   void databaseFailureExpiresCookieWithoutReturningHint() throws Exception
   {
      String uuid = seed();
      serverQInstance.getTable(UserSession.TABLE_NAME).setBackendName("missing-backend");
      HttpResponse<String> response = logout(uuid, request(CALLBACK), null);
      assertEquals(500, response.getStatus());
      assertFalse(response.getBody().contains(TOKEN));
      assertFalse(response.getBody().contains("nativeLogoutData"));
      assertTrue(response.getHeaders().get("Set-Cookie").stream().anyMatch(cookie -> cookie.startsWith("sessionUUID=;")));
   }



   /*******************************************************************************
    ** Seed the same private storage as native exchange without a remote IdP.
    *******************************************************************************/
   private String seed() throws Exception
   {
      return (seed(3600));
   }



   /*******************************************************************************
    ** Model an expired native cookie independently of a valid internal session.
    *******************************************************************************/
   private String seed(long lifetimeSeconds) throws Exception
   {
      var before = QContext.capture();
      try
      {
         QContext.init(serverQInstance, new QSystemUserSession());
         String uuid = UUID.randomUUID().toString();
         Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
         String access = encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
            + encoder.encodeToString(new JSONObject().put("sub", "native-user").put("exp", Instant.now().plusSeconds(lifetimeSeconds).getEpochSecond())
               .toString().getBytes(StandardCharsets.UTF_8)) + ".synthetic";
         new InsertAction().execute(new InsertInput(UserSession.TABLE_NAME).withRecordEntity(
            new UserSession().withUuid(uuid).withUserId("native-user").withAccessToken(access)));
         QSession session = new QSession().withUser(new QUser().withIdReference("native-user"));
         session.setUuid(uuid);
         new NativeSessionLogoutSupport().retain(session, TOKEN, CALLBACK);
         var retained = new com.kingsrook.qqq.backend.core.actions.tables.GetAction().executeForRecord(
            new com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput(UserSession.TABLE_NAME).withUniqueKey(Map.of("uuid", uuid)).withShouldOmitHiddenFields(false));
         assertTrue(retained.getValueString(NativeSessionLogoutSupport.FIELD) != null, "The native exchange must retain an encrypted context before logout");
         return (uuid);
      }
      finally
      {
         QContext.init(before);
      }
   }



   /*******************************************************************************
    ** Explicit opt-in body; no identity or endpoint is supplied.
    *******************************************************************************/
   private static String request(String callback)
   {
      return (new JSONObject().put("nativeLogout", new JSONObject().put("postLogoutRedirectUri", callback)).toString());
   }



   /*******************************************************************************
    ** The test HTTP client keeps identity sources explicit for each assertion.
    *******************************************************************************/
   private HttpResponse<String> logout(String uuid, String body, String authorization)
   {
      var request = Unirest.post(getBaseUrlAndPath() + "/logout").header("Content-Type", "application/json");
      if(uuid != null)
      {
         request.header("Cookie", "sessionUUID=" + uuid);
      }
      if(authorization != null)
      {
         request.header("Authorization", authorization);
      }
      return (request.body(body).asString());
   }



   /*******************************************************************************
    ** Substitute remote discovery only; session validation/deletion are production.
    *******************************************************************************/
   public static class Provider extends OAuth2AuthenticationModule
   {
      private static boolean discoveryFailure;



      /*******************************************************************************
       ** Fixed public endpoint for this transport fixture.
       *******************************************************************************/
      @Override
      protected NativeSessionLogoutSupport nativeLogoutSupport()
      {
         return (new NativeSessionLogoutSupport()
         {
            /*******************************************************************************
             ** Inject discovery success or failure without bypassing production session revocation.
             *******************************************************************************/
            @Override
            protected String resolveEndSessionEndpoint(OAuth2AuthenticationMetaData auth)
            {
               if(discoveryFailure)
               {
                  throw (new IllegalStateException("synthetic provider failure"));
               }
               return ("https://login.example.test/logout");
            }
         });
      }
   }
}
