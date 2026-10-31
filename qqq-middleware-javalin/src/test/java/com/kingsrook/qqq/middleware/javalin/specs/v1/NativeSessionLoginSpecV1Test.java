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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2NativeSessionLoginMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.NativeSessionLogin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Actual HTTP contract, with a scoped provider double counting exchange attempts.
 *******************************************************************************/
class NativeSessionLoginSpecV1Test extends SpecTestBase
{
   private static final String CALLBACK = "com.example.handheld:/oauth2/callback";
   private static final AtomicInteger EXCHANGES = new AtomicInteger();
   private static Map<String, String> lastContext;



   /*******************************************************************************
    ** Configure explicit native policy without changing the global auth dispatcher.
    *******************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      EXCHANGES.set(0);
      lastContext = null;
      QInstance instance = TestUtils.defineInstance();
      instance.addTable(new UserSessionMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(instance));
      instance.addTable(new RedirectStateMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(instance));
      OAuth2AuthenticationMetaData auth = new OAuth2AuthenticationMetaData()
         .withBaseUrl("https://internal.example.test/application/o/server/")
         .withExternalBaseUrl("https://login.example.test/application/o/server/")
         .withClientId("session-client").withClientSecret("fixture-server-only-value")
         .withScopes("openid profile").withNativeClientId("separate-bearer-client")
         .withUserSessionTableName(UserSession.TABLE_NAME).withRedirectStateTableName(RedirectStateMetaDataProducer.TABLE_NAME)
         .withNativeSessionLogin(configuration());
      auth.setModuleCodeReference(new QCodeReference(CountingProvider.class));
      instance.withInstanceDefaultAuthentication(auth);
      return (instance);
   }



   /*******************************************************************************
    ** Deliberately distinct public issuer, discovery URL and backend base URL.
    *******************************************************************************/
   private static OAuth2NativeSessionLoginMetaData configuration()
   {
      return (new OAuth2NativeSessionLoginMetaData().withIssuer("https://login.example.test/application/o/mobile/")
         .withDiscoveryUrl("https://login.example.test/application/o/mobile/.well-known/openid-configuration")
         .withRedirectUris(List.of(CALLBACK)));
   }



   /*******************************************************************************
    ** Exercise version, metadata and exchange on the same actual HTTP server.
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new ManageSessionSpecV1());
   }



   /*******************************************************************************
    ** Public discovery accompanies the existing session operation.
    *******************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new AuthenticationMetaDataSpecV1(), new VersionSpecV1()));
   }



   /*******************************************************************************
    ** The additive contract stays under the existing major-version URL.
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    ** Only explicit public values are advertised, with a typed fixed flow.
    *******************************************************************************/
   @Test
   void configuredPublicDescriptorAndFeature()
   {
      JSONObject version = new JSONObject(Unirest.get(getBaseUrlAndPath() + "/version").asString().getBody());
      assertEquals("1.9.0", version.getString("apiVersion"));
      assertTrue(version.getJSONArray("features").toList().contains("nativeSessionLogin"));
      String body = Unirest.get(getBaseUrlAndPath() + "/metaData/authentication").asString().getBody();
      JSONObject values = new JSONObject(body).getJSONObject("values");
      JSONObject descriptor = values.getJSONObject("nativeSessionLogin");
      assertEquals(Set.of("flow", "codeChallengeMethod", "prompt", "issuer", "discoveryUrl", "redirectUris"), descriptor.keySet());
      assertEquals(OAuth2NativeSessionLoginMetaData.FLOW, descriptor.getString("flow"));
      assertEquals("S256", descriptor.getString("codeChallengeMethod"));
      assertEquals("login", descriptor.getString("prompt"));
      assertEquals(configuration().getIssuer(), descriptor.getString("issuer"));
      assertEquals(configuration().getDiscoveryUrl(), descriptor.getString("discoveryUrl"));
      assertEquals(List.of(CALLBACK), descriptor.getJSONArray("redirectUris").toList());
      assertEquals("session-client", values.getString("clientId"));
      assertEquals("separate-bearer-client", values.getString("nativeClientId"));
      assertFalse(body.contains("fixture-server-only-value"));
      assertEquals("https://login.example.test/application/o/server/", values.getString("baseUrl"));
      assertFalse(descriptor.toString().contains("internal.example.test"));
      assertEquals(List.of(OAuth2NativeSessionLoginMetaData.FLOW), new NativeSessionLogin().toSchema().getProperties().get("flow").getEnumValues());
      assertEquals(List.of("S256"), new NativeSessionLogin().toSchema().getProperties().get("codeChallengeMethod").getEnumValues());
      assertEquals(List.of("login"), new NativeSessionLogin().toSchema().getProperties().get("prompt").getEnumValues());
      assertEquals(0, EXCHANGES.get());
   }



   /*******************************************************************************
    ** Absent or invalid configuration is not inferred from an OAuth provider URL.
    *******************************************************************************/
   @Test
   void unconfiguredOrInvalidNativeFlowIsUnavailable()
   {
      OAuth2AuthenticationMetaData auth = (OAuth2AuthenticationMetaData) serverQInstance.getAuthentication();
      for(OAuth2NativeSessionLoginMetaData policy : new OAuth2NativeSessionLoginMetaData[] { null, configuration().withIssuer("http://login.example.test/") })
      {
         auth.setNativeSessionLogin(policy);
         JSONObject version = new JSONObject(Unirest.get(getBaseUrlAndPath() + "/version").asString().getBody());
         assertFalse(version.getJSONArray("features").toList().contains("nativeSessionLogin"));
         JSONObject metadata = new JSONObject(Unirest.get(getBaseUrlAndPath() + "/metaData/authentication").asString().getBody());
         assertFalse(metadata.getJSONObject("values").has("nativeSessionLogin"));
         assertRejected(nativeRequest());
      }
   }



   /*******************************************************************************
    ** Native validation runs before the provider, including mixed legacy aliases.
    *******************************************************************************/
   @Test
   void invalidNativeRequestsCannotReachProvider()
   {
      for(String verifier : List.of("x".repeat(42), "x".repeat(129), "x".repeat(42) + "+", "x".repeat(42) + " "))
      {
         assertRejected(nativeRequest().put("codeVerifier", verifier));
      }
      for(String callback : List.of(CALLBACK + "/", CALLBACK + "?extra=1", "com.other.handheld:/oauth2/callback", "https://attacker.example.test/callback"))
      {
         assertRejected(nativeRequest().put("redirectUri", callback));
      }
      for(String flow : List.of("", "UNKNOWN", "authorization_code"))
      {
         assertRejected(nativeRequest().put("authenticationFlow", flow));
      }
      for(String mixed : List.of("accessToken", "sessionUUID", "uuid"))
      {
         assertRejected(nativeRequest().put(mixed, "fixture-mixed-credential"));
      }
      for(String missing : List.of("code", "codeVerifier", "redirectUri"))
      {
         JSONObject request = nativeRequest();
         request.remove(missing);
         assertRejected(request);
      }
      for(String field : List.of("authenticationFlow", "code", "codeVerifier", "redirectUri"))
      {
         assertRejected(nativeRequest().put(field, JSONObject.NULL));
         assertRejected(nativeRequest().put(field, 123));
      }
      for(String authorization : List.of("Basic Zml4dHVyZTppbnZhbGlk", "Bearer fixture-token"))
      {
         HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/manageSession")
            .header("Content-Type", "application/json").header("Authorization", authorization).body(nativeRequest().toString()).asString();
         assertEquals(400, response.getStatus());
         assertEquals(0, EXCHANGES.get());
      }
   }



   /*******************************************************************************
    ** Exact allowed native inputs reach the existing provider and set its cookie.
    *******************************************************************************/
   @Test
   void configuredNativeExchangeAndMarkerFreeWebRemainDistinct()
   {
      HttpResponse<String> nativeResponse = Unirest.post(getBaseUrlAndPath() + "/manageSession")
         .header("Content-Type", "application/json").header("Cookie", "sessionUUID=stale-fixture; sessionId=stale-fixture")
         .body(nativeRequest().toString()).asString();
      assertEquals(200, nativeResponse.getStatus());
      assertEquals(1, EXCHANGES.get());
      assertEquals(CALLBACK, lastContext.get("redirectUri"));
      assertEquals("x".repeat(43), lastContext.get("codeVerifier"));
      assertFalse(lastContext.containsKey("sessionUUID"), "A native code exchange must not resume a stale cookie");
      assertTrue(nativeResponse.getHeaders().get("Set-Cookie").stream().anyMatch(value -> value.startsWith("sessionUUID=")));
      ((OAuth2AuthenticationMetaData) serverQInstance.getAuthentication()).setNativeSessionLogin(null);
      JSONObject web = nativeRequest();
      web.remove("authenticationFlow");
      web.put("redirectUri", "https://existing-web.example.test/callback").put("codeVerifier", "legacy-provider-value");
      assertEquals(200, post(web).getStatus());
      assertEquals(2, EXCHANGES.get());
      assertEquals("legacy-provider-value", lastContext.get("codeVerifier"));
   }



   /*******************************************************************************
    ** A non-OAuth2 application cannot opt into an OAuth2 native exchange.
    *******************************************************************************/
   @Test
   void otherAuthenticationTypesRejectNativeMarker() throws QException
   {
      serverQInstance.withInstanceDefaultAuthentication(TestUtils.defineInstance().getAuthentication());
      assertRejected(nativeRequest());
   }



   /*******************************************************************************
    ** Valid public request fixture, never a real credential.
    *******************************************************************************/
   private JSONObject nativeRequest()
   {
      return (new JSONObject().put("authenticationFlow", OAuth2NativeSessionLoginMetaData.FLOW)
         .put("code", "fixture-authorization-code").put("codeVerifier", "x".repeat(43)).put("redirectUri", CALLBACK));
   }



   /*******************************************************************************
    ** The public endpoint rejects invalid inputs without issuing a session cookie.
    *******************************************************************************/
   private void assertRejected(JSONObject request)
   {
      HttpResponse<String> response = post(request);
      assertEquals(400, response.getStatus());
      assertEquals(0, EXCHANGES.get());
      assertTrue(response.getHeaders().get("Set-Cookie").stream().noneMatch(value -> value.startsWith("sessionUUID=")));
   }



   /*******************************************************************************
    ** Send JSON through the real semantic HTTP route.
    *******************************************************************************/
   private HttpResponse<String> post(JSONObject body)
   {
      return (Unirest.post(getBaseUrlAndPath() + "/manageSession").header("Content-Type", "application/json").body(body.toString()).asString());
   }



   /*******************************************************************************
    ** Scoped provider boundary double; no network or global registration changes.
    *******************************************************************************/
   public static class CountingProvider extends MockAuthenticationModule
   {
      /*******************************************************************************
       ** Count and retain only fixture inputs reaching the provider exchange.
       *******************************************************************************/
      @Override
      public QSession createSession(QInstance instance, Map<String, String> context) throws QAuthenticationException
      {
         EXCHANGES.incrementAndGet();
         lastContext = new HashMap<>(context);
         return (super.createSession(instance, context));
      }
   }
}
