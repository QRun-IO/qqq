/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;


/*******************************************************************************
 ** Local protocol fixture with signed tokens and single-use authorization codes.
 *******************************************************************************/
class SampleAuthenticationProvider implements AutoCloseable
{
   static final String CLIENT = "owned-sample-client";
   static final String CALLBACK = "http://sample.example.invalid/callback";
   static final String VERIFIER = "owned-sample-code-verifier-with-at-least-forty-three-characters";
   private final RSAKey key;
   private final RSAKey wrongKey;
   private final HttpServer server;
   private final Map<String, Grant> codes = new ConcurrentHashMap<>();
   private final String issuer;

   /*******************************************************************************
    ** Bind each owned code to its original verifier and provider account state.
    *******************************************************************************/
   private record Grant(String token, String verifier, Boolean enabled)
   {
   }



   /*******************************************************************************
    ** No request leaves the owned loopback provider.
    *******************************************************************************/
   SampleAuthenticationProvider() throws Exception
   {
      key = new RSAKeyGenerator(2048).keyID("sample-key").generate();
      wrongKey = new RSAKeyGenerator(2048).keyID("sample-key").generate();
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      issuer = "http://127.0.0.1:" + server.getAddress().getPort();
      server.createContext("/", exchange ->
      {
         try
         {
            String path = exchange.getRequestURI().getPath();
            if(path.equals("/.well-known/jwks.json") || path.equals("/jwks"))
            {
               reply(exchange, 200, new JWKSet(key.toPublicJWK()).toString());
            }
            else if(path.equals("/.well-known/openid-configuration"))
            {
               reply(exchange, 200, new JSONObject().put("issuer", issuer).put("jwks_uri", issuer + "/jwks")
                  .put("authorization_endpoint", issuer + "/authorize").put("token_endpoint", issuer + "/token")
                  .put("response_types_supported", List.of("code")).put("subject_types_supported", List.of("public"))
                  .put("id_token_signing_alg_values_supported", List.of("RS256")).toString());
            }
            else if(path.equals("/token") && exchange.getRequestMethod().equals("POST"))
            {
               Map<String, String> form = new HashMap<>();
               String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
               for(String pair : body.split("&"))
               {
                  String[] parts = pair.split("=", 2);
                  form.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
               }
               Grant grant = codes.remove(form.getOrDefault("code", ""));
               String authorization = "Basic " + Base64.getEncoder().encodeToString((CLIENT + ":synthetic-client-secret").getBytes(StandardCharsets.UTF_8));
               if(grant == null || !grant.enabled() || !CALLBACK.equals(form.get("redirect_uri"))
                  || !Objects.equals(grant.verifier(), form.get("code_verifier"))
                  || !authorization.equals(exchange.getRequestHeaders().getFirst("Authorization")))
               {
                  reply(exchange, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"Owned authorization grant rejected\"}");
               }
               else
               {
                  reply(exchange, 200, new JSONObject().put("access_token", grant.token()).put("token_type", "Bearer").put("expires_in", 300).toString());
               }
            }
            else
            {
               reply(exchange, 404, "{}");
            }
         }
         catch(Exception e)
         {
            reply(exchange, 500, "{\"error\":\"owned_fixture_failure\"}");
         }
      });
      server.start();
   }



   /*******************************************************************************
    ** Trust only this provider's configured discovery location.
    *******************************************************************************/
   String issuer()
   {
      return issuer;
   }



   /*******************************************************************************
    ** Register a new code so reuse is rejected by the protocol provider.
    *******************************************************************************/
   String code(String subject) throws Exception
   {
      return code(subject, VERIFIER, true);
   }



   /*******************************************************************************
    ** Traditional callbacks have no PKCE verifier; disabled accounts reject grants.
    *******************************************************************************/
   String code(String subject, String verifier, Boolean enabled) throws Exception
   {
      String code = UUID.randomUUID().toString();
      codes.put(code, new Grant(accessToken(subject, Instant.now().plusSeconds(300), false), verifier, enabled));
      return code;
   }



   /*******************************************************************************
    ** Issue signed access tokens; deliberately wrong signatures remain real JWTs.
    *******************************************************************************/
   String accessToken(String subject, Instant expiration, boolean wrongSignature) throws Exception
   {
      JWTClaimsSet claims = claims(subject).expirationTime(Date.from(expiration)).claim("name", "Owned " + subject).build();
      return sign(claims, wrongSignature ? wrongKey : key);
   }



   /*******************************************************************************
    ** Logout events share the issuer/client/subject with the actual stored token.
    *******************************************************************************/
   String logoutToken(String subject, boolean unsigned) throws Exception
   {
      JWTClaimsSet claims = claims(subject).claim("events", Map.of("http://schemas.openid.net/event/backchannel-logout", Map.of())).build();
      return unsigned ? new PlainJWT(claims).serialize() : sign(claims, key);
   }



   /*******************************************************************************
    ** Native validators inspect the actual signed fields.
    *******************************************************************************/
   private JWTClaimsSet.Builder claims(String subject)
   {
      return new JWTClaimsSet.Builder().issuer(issuer).audience(CLIENT).subject(subject).claim("sid", subject + "-provider-session")
         .issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(300))).jwtID(UUID.randomUUID().toString());
   }



   /*******************************************************************************
    ** Use the existing Nimbus dependency for real RS256 signatures.
    *******************************************************************************/
   private String sign(JWTClaimsSet claims, RSAKey signingKey) throws Exception
   {
      SignedJWT token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
      token.sign(new RSASSASigner(signingKey));
      return token.serialize();
   }



   /*******************************************************************************
    ** Close every exchange, including rejected credentials and grants.
    *******************************************************************************/
   private static void reply(HttpExchange exchange, int status, String body) throws java.io.IOException
   {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length);
      try(var output = exchange.getResponseBody())
      {
         output.write(bytes);
      }
      finally
      {
         exchange.close();
      }
   }



   /*******************************************************************************
    ** Stop only this fixture's owned ephemeral listener.
    *******************************************************************************/
   @Override
   public void close()
   {
      server.stop(0);
   }
}
