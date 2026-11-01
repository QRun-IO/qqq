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


import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.json.JSONObject;


/*******************************************************************************
 ** Strict native OIDC checks before any principal, customizer or session exists.
 ** ID tokens are accepted only from the direct, normally trusted TLS token
 ** response, as permitted by OIDC Core 3.1.3.7 item 6, never from client input.
 *******************************************************************************/
final class NativeSessionNonceSupport
{
   private static final long CLOCK_SKEW_SECONDS = 60;



   /*******************************************************************************
    ** Only explicitly configured public HTTPS discovery selects this endpoint.
    *******************************************************************************/
   static URI tokenEndpoint(OAuth2AuthenticationMetaData auth) throws QAuthenticationException
   {
      try
      {
         HTTPRequest request = new HTTPRequest(HTTPRequest.Method.GET, URI.create(auth.getNativeSessionLogin().getDiscoveryUrl()));
         HTTPResponse response = sendBounded(request, 65536);
         JSONObject discovery = new JSONObject(response.getContent());
         if(!auth.getNativeSessionLogin().getIssuer().equals(discovery.getString("issuer")))
         {
            throw new IllegalArgumentException();
         }
         URI endpoint = URI.create(discovery.getString("token_endpoint"));
         requireHttps(endpoint);
         return (endpoint);
      }
      catch(Exception ignored)
      {
         throw (new QAuthenticationException("Native identity provider discovery failed"));
      }
   }



   /*******************************************************************************
    ** Keep normal certificate/hostname verification; never follow credentials.
    *******************************************************************************/
   static void secureTransport(HTTPRequest request) throws QAuthenticationException
   {
      requireHttps(request.getURI());
      request.setConnectTimeout(5000);
      request.setReadTimeout(10000);
      request.setFollowRedirects(false);
   }



   /*******************************************************************************
    ** Nimbus prepares the authenticated request; bound bytes before JSON parsing.
    ** Keep its normal TLS defaults and never consume a redirect/error response.
    *******************************************************************************/
   static HTTPResponse sendBounded(HTTPRequest request, int maximumBytes) throws QAuthenticationException
   {
      HttpURLConnection connection = null;
      try
      {
         secureTransport(request);
         connection = request.toHttpURLConnection();
         if(connection.getResponseCode() != 200)
         {
            throw new IllegalArgumentException();
         }
         try(InputStream body = connection.getInputStream())
         {
            byte[] bytes = body.readNBytes(maximumBytes + 1);
            if(bytes.length > maximumBytes)
            {
               throw new IllegalArgumentException();
            }
            HTTPResponse response = new HTTPResponse(200);
            response.setContentType(connection.getContentType());
            response.setContent(new String(bytes, StandardCharsets.UTF_8));
            return (response);
         }
      }
      catch(Exception ignored)
      {
         throw (new QAuthenticationException("Native identity provider response failed"));
      }
      finally
      {
         if(connection != null)
         {
            connection.disconnect();
         }
      }
   }



   /*******************************************************************************
    ** Neither redirects nor embedded credentials are an identity trust boundary.
    *******************************************************************************/
   private static void requireHttps(URI uri) throws QAuthenticationException
   {
      if(!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawFragment() != null || uri.getRawQuery() != null
         || uri.getPort() == 0 || uri.getPort() < -1 || uri.getPort() > 65535)
      {
         throw (new QAuthenticationException("Native identity provider requires a direct HTTPS endpoint"));
      }
   }



   /*******************************************************************************
    ** Validate the direct token response and bind the existing access principal.
    ** Exceptions deliberately omit claims, token material, nonce and causes.
    *******************************************************************************/
   static JSONObject validate(OAuth2AuthenticationMetaData auth, Object rawIdToken, String accessToken, String expectedNonce) throws QAuthenticationException
   {
      try
      {
         if(expectedNonce == null || !expectedNonce.matches("[A-Za-z0-9_-]{16,512}") || !(rawIdToken instanceof String token)
            || token.length() > 65536 || accessToken == null || accessToken.length() > 65536)
         {
            throw new IllegalArgumentException();
         }
         SignedJWT jwt = SignedJWT.parse(token);
         if(!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm()) || jwt.getSignature().decode().length == 0
            || (jwt.getHeader().getCriticalParams() != null && !jwt.getHeader().getCriticalParams().isEmpty()))
         {
            throw new IllegalArgumentException();
         }
         JWTClaimsSet claims = jwt.getJWTClaimsSet();
         JWTClaimsSet accessClaims = JWTParser.parse(accessToken).getJWTClaimsSet();
         Instant now = Instant.now();
         String subject = claims.getSubject();
         String authorizedParty = claims.getStringClaim("azp");
         if(!auth.getNativeSessionLogin().getIssuer().equals(claims.getIssuer())
            || !List.of(auth.getClientId()).equals(claims.getAudience())
            || (authorizedParty != null && !auth.getClientId().equals(authorizedParty))
            || claims.getExpirationTime() == null || claims.getIssueTime() == null
            || claims.getExpirationTime().toInstant().isBefore(now.minusSeconds(CLOCK_SKEW_SECONDS))
            || claims.getIssueTime().toInstant().isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS))
            || claims.getIssueTime().after(claims.getExpirationTime())
            || (claims.getNotBeforeTime() != null && claims.getNotBeforeTime().toInstant().isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS)))
            || !expectedNonce.equals(claims.getStringClaim("nonce")) || subject == null || subject.isBlank()
            || !subject.equals(accessClaims.getSubject()) || accessClaims.getExpirationTime() == null
            || accessClaims.getExpirationTime().toInstant().isBefore(now.minusSeconds(CLOCK_SKEW_SECONDS))
            || (accessClaims.getNotBeforeTime() != null && accessClaims.getNotBeforeTime().toInstant().isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS)))
            || (accessClaims.getIssueTime() != null && accessClaims.getIssueTime().toInstant().isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS))))
         {
            throw new IllegalArgumentException();
         }
         return (new JSONObject(claims.toJSONObject()));
      }
      catch(Exception ignored)
      {
         throw (new QAuthenticationException("Native identity token validation failed"));
      }
   }
}
