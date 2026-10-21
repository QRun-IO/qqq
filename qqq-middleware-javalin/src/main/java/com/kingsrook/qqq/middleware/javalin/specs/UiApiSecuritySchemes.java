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

package com.kingsrook.qqq.middleware.javalin.specs;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.openapi.model.OAuth2;
import com.kingsrook.qqq.openapi.model.OAuth2Flow;
import com.kingsrook.qqq.openapi.model.SecurityScheme;
import com.kingsrook.qqq.openapi.model.SecuritySchemeType;


/*******************************************************************************
 ** The security schemes of the UI API.  A secured operation accepts any ONE of
 ** them (OpenAPI alternatives): the session cookie (web), or an Authorization
 ** bearer token (CLI, TUI, native apps, scripts), which is either an OAuth2
 ** access token (JWT) from the app's identity provider, or a session UUID.
 ** See docs/UI-API-CLIENTS.md, "Authentication".
 *******************************************************************************/
public class UiApiSecuritySchemes
{
   public static final String SESSION_COOKIE = "sessionUuidCookie";
   public static final String BEARER_AUTH    = "bearerAuth";
   public static final String OAUTH2         = "oauth2";

   ///////////////////////////////////////////////////////////////////////////////
   // the identity provider's endpoints differ per deployment: a client reads   //
   // values.baseUrl (the issuer) from GET /qqq/v1/metaData/authentication and  //
   // its OIDC discovery document.  the URLs here are placeholders, relative to //
   // the issuer, as OpenAPI 3.0 requires a URL.                                //
   ///////////////////////////////////////////////////////////////////////////////
   public static final Map<String, SecurityScheme> SCHEMES;

   ////////////////////////////////////////////////////////////////////////////////
   // note: the committed documents are written without empty values, and an   //
   // empty requirement ({}) would mean "no authentication" - so the non-oauth2 //
   // schemes carry the "N/A" placeholder scope, as the 1.0 document always did //
   ////////////////////////////////////////////////////////////////////////////////
   public static final List<Map<String, List<String>>> DEFAULT_SECURITY = List.of(
      Map.of(SESSION_COOKIE, List.of("N/A")),
      Map.of(BEARER_AUTH, List.of("N/A")),
      Map.of(OAUTH2, List.of("openid", "profile", "email")));

   static
   {
      Map<String, SecurityScheme> schemes = new LinkedHashMap<>();

      schemes.put(SESSION_COOKIE, new SecurityScheme()
         .withType(SecuritySchemeType.API_KEY)
         .withName("sessionUUID")
         .withIn("cookie")
         .withDescription("The session cookie set by POST /qqq/v1/manageSession (web clients)."));

      schemes.put(BEARER_AUTH, new SecurityScheme()
         .withType(SecuritySchemeType.HTTP)
         .withScheme("bearer")
         .withBearerFormat("JWT or session UUID")
         .withDescription("""
            `Authorization: Bearer <token>`, where the token is either
            - an OAuth2 access token (a signed JWT) issued by the app's identity provider to the app's client,
              or to its native client (`values.nativeClientId` in GET /qqq/v1/metaData/authentication), for
              example from the device-authorization grant or auth-code + PKCE. It is verified against the
              provider's keys (issuer, audience, expiry, signature) and gives the same user and permissions
              as signing in on the web. When it expires (401), refresh it at the provider and retry; or
            - a session UUID returned by POST /qqq/v1/manageSession, which the server keeps refreshed."""));

      ////////////////////////////////////////////////////////////////////////////
      // OAS 3.0 has no device-authorization flow; it is documented on the     //
      // scheme and in UI-API-CLIENTS.md                                       //
      ////////////////////////////////////////////////////////////////////////////
      OAuth2 oauth2 = new OAuth2().withFlows(Map.of("authorizationCode", new OAuth2Flow()
         .withAuthorizationUrl("/authorize")
         .withTokenUrl("/token")
         .withScopes(new java.util.TreeMap<>(Map.of(
            "openid", "OpenID Connect sign-in",
            "profile", "Name, username and groups (permissions come from the groups)",
            "email", "Email address",
            "offline_access", "A refresh token, to renew the access token without signing in again")))));
      oauth2.withDescription("""
         The app's OAuth2 / OpenID Connect provider. The real endpoints come from the provider's discovery
         document: `{values.baseUrl}.well-known/openid-configuration`, with values from
         GET /qqq/v1/metaData/authentication. Native clients use `values.nativeClientId` (a public client)
         with auth-code + PKCE (loopback or custom-scheme redirect URI registered at the provider) or the
         device-authorization grant (`device_authorization_endpoint`, RFC 8628), then send the access token
         as `Authorization: Bearer`.""");
      schemes.put(OAUTH2, oauth2);

      SCHEMES = java.util.Collections.unmodifiableMap(schemes);
   }
}
