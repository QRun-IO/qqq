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


import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2NativeSessionLoginMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2NativeSessionLogoutMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Real HTTPS token exchange with an explicitly trusted fixture certificate.
 ** No production trust override, hostname bypass or provider response mock seam.
 *******************************************************************************/
class OAuth2NativeSessionNonceTest
{
   private static final String NONCE = "original_client_nonce_123456";
   private static final String CALLBACK = "com.example.handheld:/oauth2/callback";
   private static final AtomicInteger CUSTOMIZATIONS = new AtomicInteger();
   @TempDir
   private static Path certificateDirectory;
   private static Path keyStorePath;
   private WireMockServer provider;
   private SSLSocketFactory previousSocketFactory;
   private OAuth2AuthenticationMetaData auth;
   private QInstance instance;
   private String issuer;



   /*******************************************************************************
    ** Generate a disposable localhost SAN certificate with the running JDK.
    ** No host trust store is modified; the test trusts only this certificate.
    *******************************************************************************/
   @BeforeAll
   static void createCertificate() throws Exception
   {
      keyStorePath = certificateDirectory.resolve("provider.p12");
      String executable = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
      Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
         "-genkeypair", "-alias", "provider", "-keyalg", "RSA", "-keysize", "2048", "-storetype", "PKCS12",
         "-keystore", keyStorePath.toString(), "-storepass", "fixture-password", "-keypass", "fixture-password",
         "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-validity", "2", "-noprompt")
         .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
      if(!process.waitFor(30, TimeUnit.SECONDS))
      {
         process.destroyForcibly();
         throw new IllegalStateException("Fixture certificate generation timed out");
      }
      assertEquals(0, process.exitValue(), "Fixture certificate generation must succeed");
   }



   /*******************************************************************************
    ** Trust only the test server's existing certificate, restoring it afterward.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousSocketFactory = HTTPRequest.getDefaultSSLSocketFactory();
      MemoryRecordStore.fullReset();
      KeyStore keys = KeyStore.getInstance("PKCS12");
      try(InputStream stream = Files.newInputStream(keyStorePath))
      {
         keys.load(stream, "fixture-password".toCharArray());
      }
      TrustManagerFactory managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      managers.init(keys);
      SSLContext tls = SSLContext.getInstance("TLS");
      tls.init(null, managers.getTrustManagers(), null);
      HTTPRequest.setDefaultSSLSocketFactory(tls.getSocketFactory());
      provider = new WireMockServer(WireMockConfiguration.options().dynamicPort().dynamicHttpsPort()
         .keystorePath(keyStorePath.toString()).keystoreType("PKCS12").keystorePassword("fixture-password").keyManagerPassword("fixture-password"));
      provider.start();
      issuer = "https://localhost:" + provider.httpsPort();
      CUSTOMIZATIONS.set(0);
      OAuth2AuthenticationModule.clearOIDCProviderMetadataCache();
      instance = new QInstance();
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      instance.addTable(new UserSessionMetaDataProducer("memory").withNativeLogoutContext().produce(instance));
      instance.addTable(new RedirectStateMetaDataProducer("memory").produce(instance));
      auth = new OAuth2AuthenticationMetaData().withBaseUrl(provider.baseUrl()).withClientId("exchange-client").withClientSecret("synthetic-test-secret")
         .withScopes("openid").withRefreshTokensEnabled(false).withUserSessionTableName(UserSession.TABLE_NAME)
         .withRedirectStateTableName(RedirectStateMetaDataProducer.TABLE_NAME)
         .withNativeSessionLogin(new OAuth2NativeSessionLoginMetaData().withIssuer(issuer).withDiscoveryUrl(issuer + "/discovery")
            .withRedirectUris(List.of(CALLBACK)).withRequireNonceValidation(true))
         .withNativeSessionLogout(new OAuth2NativeSessionLogoutMetaData().withPostLogoutRedirectUris(List.of(CALLBACK)))
         .withRefreshTokenEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]));
      auth.setName("nonce-test-provider");
      auth.setCustomizer(new QCodeReference(CountingCustomizer.class));
      instance.withInstanceDefaultAuthentication(auth);
      QContext.init(instance, new QSystemUserSession(instance));
      discovery(issuer, issuer + "/token");
      HTTPRequest positiveControl = new HTTPRequest(HTTPRequest.Method.GET, java.net.URI.create(issuer + "/discovery"));
      positiveControl.setConnectTimeout(5000);
      positiveControl.setReadTimeout(5000);
      assertEquals(200, positiveControl.send().getStatusCode(), "Every negative test first proves trusted HTTPS discovery");
   }



   /*******************************************************************************
    ** Do not leak test TLS state to other OAuth tests or processes.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      HTTPRequest.setDefaultSSLSocketFactory(previousSocketFactory);
      if(provider != null)
      {
         provider.stop();
      }
      QContext.clear();
      MemoryRecordStore.fullReset();
      OAuth2AuthenticationModule.clearOIDCProviderMetadataCache();
   }



   /*******************************************************************************
    ** Valid matching claims create the existing access-token principal once.
    *******************************************************************************/
   @Test
   void validHttpsExchangeCreatesBoundPrincipal() throws Exception
   {
      String original = compact("RS256", claims());
      tokenResponse(original, access("same-user"));
      QSession session = new OAuth2AuthenticationModule().createSession(instance, context());
      assertEquals("same-user", session.getUser().getIdReference());
      assertEquals(1, CUSTOMIZATIONS.get());
      assertEquals(1, sessionCount());
      assertFalse(String.valueOf(session.getValues()).contains(NONCE));
      var row = new GetAction().executeForRecord(new GetInput(UserSession.TABLE_NAME).withUniqueKey(Map.of("uuid", session.getUuid())).withShouldOmitHiddenFields(false));
      String stored = row.getValueString(NativeSessionLogoutSupport.FIELD);
      assertNotNull(stored);
      assertFalse(stored.contains(original));
      String decrypted = RefreshTokenCipher.fromMetaData(auth).decrypt(stored,
         NativeSessionLogoutSupport.associatedData(auth.getName(), session.getUuid(), "same-user"));
      assertEquals(original, new JSONObject(decrypted).getString("idToken"));
      assertFalse(JsonUtils.toJson(session).contains(original));
      assertTrue(instance.getTable("userSessionRefreshToken") == null, "No refresh-token grant or storage is needed");
      provider.verify(1, postRequestedFor(urlEqualTo("/token")));
   }



   /*******************************************************************************
    ** Every bad claim fails before customizer or session persistence.
    *******************************************************************************/
   @Test
   void invalidClaimsNeverCreateSession() throws Exception
   {
      List<JSONObject> invalid = List.of(
         claims().put("nonce", "different_original_nonce"), without("nonce"),
         claims().put("iss", "https://other.example.test"),
         claims().put("aud", "other-client"), claims().put("aud", new JSONArray(List.of("exchange-client", "other-client"))),
         claims().put("azp", "other-client"),
         claims().put("exp", Instant.now().minusSeconds(120).getEpochSecond()), without("exp"),
         claims().put("iat", Instant.now().plusSeconds(120).getEpochSecond()), without("iat"),
         claims().put("nbf", Instant.now().plusSeconds(120).getEpochSecond()),
         claims().put("sub", "other-user"), without("sub"), claims().put("sub", ""));
      for(JSONObject invalidClaims : invalid)
      {
         tokenResponse(compact("RS256", invalidClaims), access("same-user"));
         assertRejected(context());
      }
   }



   /*******************************************************************************
    ** Algorithm restrictions and subject binding are not optional parse fallbacks.
    *******************************************************************************/
   @Test
   void missingUnsupportedOrUnboundTokensFailClosed() throws Exception
   {
      for(String token : List.of("", "not-a-jwt", compact("none", claims()), compact("HS256", claims()), compact("ES256", claims())))
      {
         tokenResponse(token, access("same-user"));
         assertRejected(context());
      }
      tokenResponse(null, access("same-user"));
      assertRejected(context());
      tokenResponse(compact("RS256", claims()), compact("RS256", new JSONObject().put("exp", Instant.now().plusSeconds(300).getEpochSecond())));
      assertRejected(context());
   }



   /*******************************************************************************
    ** Matching subjects cannot make an expired or not-yet-valid access token live.
    *******************************************************************************/
   @Test
   void invalidAccessTokenTimesNeverCreateSession() throws Exception
   {
      for(JSONObject accessClaims : List.of(new JSONObject().put("sub", "same-user"),
         new JSONObject().put("sub", "same-user").put("exp", Instant.now().minusSeconds(120).getEpochSecond()),
         new JSONObject().put("sub", "same-user").put("exp", "not-a-numeric-date"),
         new JSONObject().put("sub", "same-user").put("exp", Instant.now().plusSeconds(300).getEpochSecond()).put("nbf", Instant.now().plusSeconds(120).getEpochSecond()),
         new JSONObject().put("sub", "same-user").put("exp", Instant.now().plusSeconds(300).getEpochSecond()).put("nbf", "not-a-numeric-date")))
      {
         tokenResponse(compact("RS256", claims()), compact("RS256", accessClaims));
         assertRejected(context());
      }
   }



   /*******************************************************************************
    ** Invalid caller nonce is rejected before any token request.
    *******************************************************************************/
   @Test
   void expectedNonceIsRequiredBeforeExchange() throws Exception
   {
      for(String nonce : List.of("", "short", "a".repeat(513), "not a url safe nonce"))
      {
         Map<String, String> context = context();
         context.put("expectedNonce", nonce);
         assertRejected(context);
      }
      Map<String, String> missing = context();
      missing.remove("expectedNonce");
      assertRejected(missing);
      provider.verify(0, postRequestedFor(urlEqualTo("/token")));
   }



   /*******************************************************************************
    ** Discovery cannot silently select insecure or redirected token transport.
    *******************************************************************************/
   @Test
   void discoveryAndTokenRedirectsFailClosed() throws Exception
   {
      discovery("https://wrong.example.test", issuer + "/token");
      assertRejected(context());
      discovery(issuer, provider.baseUrl() + "/token");
      assertRejected(context());
      discovery(issuer, issuer + "/token");
      provider.stubFor(post(urlEqualTo("/token")).willReturn(aResponse().withStatus(302).withHeader("Location", issuer + "/unexpected")));
      assertRejected(context());
      provider.verify(0, postRequestedFor(urlEqualTo("/unexpected")));
      provider.stubFor(get(urlEqualTo("/discovery")).willReturn(aResponse().withStatus(302).withHeader("Location", issuer + "/unexpected")));
      assertRejected(context());
   }



   /*******************************************************************************
    ** Limit response bytes before buffering/parsing and reject invalid ports.
    *******************************************************************************/
   @Test
   void oversizedResponsesAndInvalidEndpointsFailClosed() throws Exception
   {
      provider.stubFor(get(urlEqualTo("/discovery")).willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(" ".repeat(65537))));
      assertRejected(context());
      for(String endpoint : List.of("https://localhost:0/token", "https://localhost:65536/token", "https://user@localhost/token", issuer + "/token#fragment"))
      {
         discovery(issuer, endpoint);
         assertRejected(context());
      }
      discovery(issuer, issuer + "/token");
      provider.stubFor(post(urlEqualTo("/token")).willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(" ".repeat(262145))));
      assertRejected(context());
      tokenResponse("a".repeat(65537), access("same-user"));
      assertRejected(context());
   }



   /*******************************************************************************
    ** Normal TLS rejects the fixture when its explicit trust is removed.
    *******************************************************************************/
   @Test
   void untrustedProviderCannotCreateSession() throws Exception
   {
      HTTPRequest.setDefaultSSLSocketFactory(previousSocketFactory);
      tokenResponse(compact("RS256", claims()), access("same-user"));
      assertRejected(context());
      provider.verify(0, postRequestedFor(urlEqualTo("/token")));
   }



   /*******************************************************************************
    ** Malformed and rejected tokens never appear in exceptions or core logs.
    *******************************************************************************/
   @Test
   void rejectedTokenErrorsAreSanitized() throws Exception
   {
      var logger = QLogger.activateCollectingLoggerForClass(OAuth2AuthenticationModule.class);
      try
      {
         String raw = compact("RS256", claims().put("nonce", "private_nonce_canary"));
         tokenResponse(raw, access("same-user"));
         QAuthenticationException error = assertThrows(QAuthenticationException.class, () -> new OAuth2AuthenticationModule().createSession(instance, context()));
         assertEquals(null, error.getCause());
         assertFalse(error.toString().contains("private_nonce_canary"));
         assertFalse(logger.getCollectedMessages().toString().contains("private_nonce_canary"));
         assertFalse(logger.getCollectedMessages().toString().contains(raw));
         assertEquals(0, CUSTOMIZATIONS.get());
         assertEquals(0, sessionCount());
      }
      finally
      {
         QLogger.deactivateCollectingLoggerForClass(OAuth2AuthenticationModule.class);
      }
   }



   /*******************************************************************************
    ** The existing opt-out native flow remains compatible without an ID token.
    *******************************************************************************/
   @Test
   void optOutKeepsLegacyNativeBehavior() throws Exception
   {
      auth.getNativeSessionLogin().setRequireNonceValidation(false);
      assertFalse(NativeSessionLogoutSupport.isConfigured(instance));
      JSONObject document = new JSONObject().put("issuer", provider.baseUrl()).put("authorization_endpoint", provider.baseUrl() + "/authorize")
         .put("token_endpoint", provider.baseUrl() + "/token").put("jwks_uri", provider.baseUrl() + "/jwks")
         .put("response_types_supported", new JSONArray(List.of("code"))).put("subject_types_supported", new JSONArray(List.of("public")))
         .put("id_token_signing_alg_values_supported", new JSONArray(List.of("RS256")));
      provider.stubFor(get(urlEqualTo("/.well-known/openid-configuration")).willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(document.toString())));
      tokenResponse(null, access("same-user"));
      Map<String, String> context = context();
      context.remove("expectedNonce");
      assertNotNull(new OAuth2AuthenticationModule().createSession(instance, context()));
      assertEquals(1, sessionCount());
   }



   /*******************************************************************************
    ** Responses use the exact public issuer independently from the backend URL.
    *******************************************************************************/
   private void discovery(String advertisedIssuer, String tokenEndpoint)
   {
      provider.stubFor(get(urlEqualTo("/discovery")).willReturn(aResponse().withHeader("Content-Type", "application/json")
         .withBody(new JSONObject().put("issuer", advertisedIssuer).put("token_endpoint", tokenEndpoint).toString())));
   }



   /*******************************************************************************
    ** Raw token bytes come from the HTTPS token response, never the client body.
    *******************************************************************************/
   private void tokenResponse(String idToken, String accessToken)
   {
      JSONObject body = new JSONObject().put("token_type", "Bearer").put("access_token", accessToken).put("expires_in", 300);
      if(idToken != null)
      {
         body.put("id_token", idToken);
      }
      provider.stubFor(post(urlEqualTo("/token")).willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body.toString())));
   }



   /*******************************************************************************
    ** Synthetic signed-form JWT; identity trust is the directly trusted TLS hop.
    *******************************************************************************/
   private static String compact(String algorithm, JSONObject claims)
   {
      Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
      return (encoder.encodeToString(new JSONObject().put("alg", algorithm).toString().getBytes(StandardCharsets.UTF_8)) + "."
         + encoder.encodeToString(claims.toString().getBytes(StandardCharsets.UTF_8)) + "." + encoder.encodeToString(new byte[256]));
   }



   /*******************************************************************************
    ** Existing access JWT principal, separate from ID-token validation.
    *******************************************************************************/
   private static String access(String subject)
   {
      return (compact("RS256", new JSONObject().put("sub", subject).put("exp", Instant.now().plusSeconds(300).getEpochSecond())));
   }



   /*******************************************************************************
    ** Valid ID claims before applying one negative change.
    *******************************************************************************/
   private JSONObject claims()
   {
      return (new JSONObject().put("iss", issuer).put("aud", "exchange-client").put("azp", "exchange-client").put("sub", "same-user")
         .put("exp", Instant.now().plusSeconds(300).getEpochSecond()).put("iat", Instant.now().getEpochSecond()).put("nonce", NONCE));
   }



   /*******************************************************************************
    ** Missing required claim variant.
    *******************************************************************************/
   private JSONObject without(String field)
   {
      JSONObject result = claims();
      result.remove(field);
      return (result);
   }



   /*******************************************************************************
    ** Explicit server-validated native flow marker and retained request nonce.
    *******************************************************************************/
   private Map<String, String> context()
   {
      return (new HashMap<>(Map.of("code", "synthetic-code", "redirectUri", CALLBACK, "codeVerifier", "a".repeat(43),
         NativeSessionLogoutSupport.NATIVE_FLOW_KEY, OAuth2NativeSessionLoginMetaData.FLOW, "expectedNonce", NONCE)));
   }



   /*******************************************************************************
    ** All negative paths preserve storage and do not invoke host customization.
    *******************************************************************************/
   private void assertRejected(Map<String, String> context) throws Exception
   {
      QAuthenticationException error = assertThrows(QAuthenticationException.class, () -> new OAuth2AuthenticationModule().createSession(instance, context));
      assertFalse(error.getMessage().contains(NONCE));
      assertEquals(0, CUSTOMIZATIONS.get());
      assertEquals(0, sessionCount());
   }



   /*******************************************************************************
    ** Query the real memory backend rather than infer persistence from exceptions.
    *******************************************************************************/
   private int sessionCount() throws Exception
   {
      return (new QueryAction().execute(new QueryInput(UserSession.TABLE_NAME)).getRecords().size());
   }



   /*******************************************************************************
    ** Count the earliest host customization boundary.
    *******************************************************************************/
   public static class CountingCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       ** Count host customization calls so rejected tokens must fail before this boundary.
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
      {
         CUSTOMIZATIONS.incrementAndGet();
      }
   }
}
