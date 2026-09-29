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

package com.kingsrook.qqq.middleware.javalin;


import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;


/*******************************************************************************
 ** Owned HTTP discovery/JWKS provider; keys and sessions are synthetic test data.
 *******************************************************************************/
public class LogoutTestProvider implements AutoCloseable
{
   public static final String CLIENT_ID = "qqq-logout-test";
   private final HttpServer server;
   private final RSAKey key;
   private final RSAKey wrongKey;
   private final String issuer;



   /*******************************************************************************
    ** Serve only the public key and trusted discovery metadata on an owned port.
    *******************************************************************************/
   public LogoutTestProvider() throws QException
   {
      try
      {
         key = new RSAKeyGenerator(2048).keyID("test-key").generate();
         wrongKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
         server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
         issuer = "http://127.0.0.1:" + server.getAddress().getPort();
         server.createContext("/", exchange ->
         {
            String response = exchange.getRequestURI().getPath().equals("/jwks")
               ? new JWKSet(key.toPublicJWK()).toString()
               : new JSONObject().put("issuer", issuer).put("jwks_uri", issuer + "/jwks")
                  .put("authorization_endpoint", issuer + "/authorize").put("token_endpoint", issuer + "/token")
                  .put("response_types_supported", List.of("code")).put("subject_types_supported", List.of("public"))
                  .put("id_token_signing_alg_values_supported", List.of("RS256")).toString();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try(var output = exchange.getResponseBody())
            {
               output.write(bytes);
            }
         });
         server.start();
      }
      catch(Exception e)
      {
         throw (new QException("Could not start owned logout provider", e));
      }
   }



   /*******************************************************************************
    ** Configure the same provider/client used in issued logout tokens.
    *******************************************************************************/
   public OAuth2AuthenticationMetaData authentication()
   {
      return (new OAuth2AuthenticationMetaData().withBaseUrl(issuer).withClientId(CLIENT_ID)
         .withClientSecret("synthetic-test-client-secret").withScopes("openid")
         .withRedirectStateTableName(RedirectStateMetaDataProducer.TABLE_NAME)
         .withUserSessionTableName(UserSession.TABLE_NAME));
   }



   /*******************************************************************************
    ** Create a fresh logout event; callers can alter claims for rejection tests.
    *******************************************************************************/
   public JWTClaimsSet.Builder claims(String subject, String sessionId)
   {
      return (new JWTClaimsSet.Builder().issuer(issuer).audience(CLIENT_ID).subject(subject)
         .claim("sid", sessionId).issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(120)))
         .jwtID(UUID.randomUUID().toString()).claim("events", Map.of("http://schemas.openid.net/event/backchannel-logout", Map.of())));
   }



   /*******************************************************************************
    ** Sign legitimate test logout events using the provider's private key.
    *******************************************************************************/
   public String logoutToken(String subject, String sessionId) throws Exception
   {
      return (sign(claims(subject, sessionId).build(), key));
   }



   /*******************************************************************************
    ** Access-token fields used to identify already stored application sessions.
    *******************************************************************************/
   public String accessToken(String subject, String sessionId) throws Exception
   {
      return (sign(claims(subject, sessionId).claim("events", null).build(), key));
   }



   /*******************************************************************************
    ** Real signatures and changed claims exercise validation, not a mocked verifier.
    *******************************************************************************/
   public Map<String, String> invalidTokens(String subject, String sessionId) throws Exception
   {
      Map<String, String> tokens = new LinkedHashMap<>();
      tokens.put("unsigned", new PlainJWT(claims(subject, sessionId).build()).serialize());
      tokens.put("tampered-signature", sign(claims(subject, sessionId).build(), wrongKey));
      tokens.put("wrong-issuer", sign(claims(subject, sessionId).issuer(issuer + "/other").build(), key));
      tokens.put("wrong-client", sign(claims(subject, sessionId).audience("another-client").build(), key));
      tokens.put("expired", sign(claims(subject, sessionId).expirationTime(Date.from(Instant.now().minusSeconds(120))).build(), key));
      tokens.put("old-issued-at", sign(claims(subject, sessionId).issueTime(Date.from(Instant.now().minusSeconds(400))).build(), key));
      tokens.put("future-issued-at", sign(claims(subject, sessionId).issueTime(Date.from(Instant.now().plusSeconds(120))).build(), key));
      tokens.put("missing-expiration", sign(claims(subject, sessionId).expirationTime(null).build(), key));
      tokens.put("missing-id", sign(claims(subject, sessionId).jwtID(null).build(), key));
      tokens.put("empty-id", sign(claims(subject, sessionId).jwtID("").build(), key));
      tokens.put("ordinary-access-token", accessToken(subject, sessionId));
      tokens.put("wrong-event", sign(claims(subject, sessionId).claim("events", Map.of("another-event", Map.of())).build(), key));
      tokens.put("nonce", sign(claims(subject, sessionId).claim("nonce", "not-a-logout-token").build(), key));
      tokens.put("no-sub-or-sid", logoutToken(null, null));
      tokens.put("malformed", "not-a-jwt");
      return (tokens);
   }



   /*******************************************************************************
    ** Use Nimbus to create signed JWTs matching the public JWKS fixture.
    *******************************************************************************/
   private String sign(JWTClaimsSet claims, RSAKey signingKey) throws Exception
   {
      SignedJWT token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
      token.sign(new RSASSASigner(signingKey));
      return (token.serialize());
   }



   /*******************************************************************************
    ** Release the owned provider port after each test.
    *******************************************************************************/
   @Override
   public void close()
   {
      server.stop(0);
   }
}
