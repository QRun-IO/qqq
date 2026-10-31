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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.util.List;
import com.fasterxml.jackson.annotation.JsonValue;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2NativeSessionLoginMetaData;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 ** Public native cookie-session policy. Never contains server client credentials.
 *******************************************************************************/
public class NativeSessionLogin implements ToSchema
{
   /*******************************************************************************
    ** The supported native cookie-session protocol.
    *******************************************************************************/
   public enum Flow
   {
      AUTHORIZATION_CODE_PKCE_SESSION
   }



   /*******************************************************************************
    ** Plain PKCE challenges are never supported by this native protocol.
    *******************************************************************************/
   public enum CodeChallengeMethod
   {
      S256
   }



   /*******************************************************************************
    ** A shared handheld requests fresh provider authentication for each sign-in.
    *******************************************************************************/
   public enum Prompt
   {
      LOGIN;

      /***************************************************************************
       ** The OAuth wire value is lowercase, in JSON and the generated schema.
       ***************************************************************************/
      @Override
      @JsonValue
      public String toString()
      {
         return ("login");
      }
   }

   @OpenAPIDescription("Send this exact authenticationFlow to POST /manageSession with code, codeVerifier and redirectUri.")
   private Flow flow = Flow.AUTHORIZATION_CODE_PKCE_SESSION;

   private CodeChallengeMethod codeChallengeMethod = CodeChallengeMethod.S256;

   @OpenAPIDescription("Request fresh provider authentication in the external browser. This is not provider-global logout or proof of authentication time.")
   private Prompt prompt = Prompt.LOGIN;

   @OpenAPIDescription("Explicit public HTTPS issuer. Match the discovery document issuer exactly; never infer it from baseUrl.")
   private String issuer;

   @OpenAPIDescription("Explicit public HTTPS OIDC discovery document URL, without credentials, query or fragment.")
   private String discoveryUrl;

   @OpenAPIDescription("Exact allowed native callback URIs. Use only a callback owned by this installed client; do not alter or infer one.")
   @OpenAPIListItems(String.class)
   private List<String> redirectUris;



   /*******************************************************************************
    ** Copy the explicitly configured public policy.
    *******************************************************************************/
   public NativeSessionLogin withConfiguration(OAuth2NativeSessionLoginMetaData configuration)
   {
      if(!configuration.isValid())
      {
         throw (new IllegalArgumentException("Invalid native session login configuration."));
      }
      issuer = configuration.getIssuer();
      discoveryUrl = configuration.getDiscoveryUrl();
      redirectUris = configuration.getRedirectUris();
      return (this);
   }



   /*******************************************************************************
    ** Getter for flow.
    *******************************************************************************/
   public Flow getFlow()
   {
      return (flow);
   }



   /*******************************************************************************
    ** Getter for codeChallengeMethod.
    *******************************************************************************/
   public CodeChallengeMethod getCodeChallengeMethod()
   {
      return (codeChallengeMethod);
   }



   /*******************************************************************************
    ** Getter for the fixed fresh-authentication prompt.
    *******************************************************************************/
   public Prompt getPrompt()
   {
      return (prompt);
   }



   /*******************************************************************************
    ** Getter for issuer.
    *******************************************************************************/
   public String getIssuer()
   {
      return (issuer);
   }



   /*******************************************************************************
    ** Getter for discoveryUrl.
    *******************************************************************************/
   public String getDiscoveryUrl()
   {
      return (discoveryUrl);
   }



   /*******************************************************************************
    ** Getter for redirectUris.
    *******************************************************************************/
   public List<String> getRedirectUris()
   {
      return (redirectUris);
   }
}
