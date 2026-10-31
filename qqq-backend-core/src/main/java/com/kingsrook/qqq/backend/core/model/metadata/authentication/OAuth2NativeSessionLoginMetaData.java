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

package com.kingsrook.qqq.backend.core.model.metadata.authentication;


import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;


/*******************************************************************************
 ** Explicit public discovery and callback policy for native OAuth2 cookie login.
 ** Uses the parent OAuth2 clientId; the separate native bearer client is unrelated.
 *******************************************************************************/
public class OAuth2NativeSessionLoginMetaData implements Serializable
{
   public static final String FLOW = "AUTHORIZATION_CODE_PKCE_SESSION";
   public static final String CODE_CHALLENGE_METHOD = "S256";

   private String issuer;
   private String discoveryUrl;
   private List<String> redirectUris;



   /*******************************************************************************
    ** Configuration is explicit: no inferred issuer, callback, or insecure URL.
    *******************************************************************************/
   @JsonIgnore
   public boolean isValid()
   {
      return (publicHttpsUrl(issuer) && publicHttpsUrl(discoveryUrl) && redirectUris != null
         && !redirectUris.isEmpty() && redirectUris.size() <= 32
         && redirectUris.stream().allMatch(OAuth2NativeSessionLoginMetaData::redirectUri));
   }



   /*******************************************************************************
    ** Public metadata must never contain credentials, query secrets or fragments.
    *******************************************************************************/
   private static URI safeUri(String value)
   {
      try
      {
         if(value == null || value.length() > 2048 || value.contains("*") || value.contains("\\"))
         {
            return (null);
         }
         URI uri = URI.create(value);
         return (uri.isAbsolute() && !uri.isOpaque() && uri.getRawUserInfo() == null
            && (uri.getRawAuthority() == null || (uri.getHost() != null && !uri.getRawAuthority().contains("@")))
            && uri.getPort() >= -1 && uri.getPort() != 0 && uri.getPort() <= 65535
            && uri.getRawQuery() == null && uri.getRawFragment() == null ? uri : null);
      }
      catch(IllegalArgumentException exception)
      {
         return (null);
      }
   }



   /*******************************************************************************
    ** The host explicitly supplies the browser-reachable HTTPS URLs.
    *******************************************************************************/
   private static boolean publicHttpsUrl(String value)
   {
      URI uri = safeUri(value);
      return (uri != null && "https".equals(uri.getScheme()) && uri.getHost() != null && !uri.getHost().isBlank());
   }



   /*******************************************************************************
    ** Permit HTTPS app links or explicit private-use scheme callbacks, never HTTP.
    *******************************************************************************/
   private static boolean redirectUri(String value)
   {
      URI uri = safeUri(value);
      if(uri == null || uri.getRawPath() == null || uri.getRawPath().isEmpty())
      {
         return (false);
      }
      return ("https".equals(uri.getScheme()) ? publicHttpsUrl(value)
         : !"http".equalsIgnoreCase(uri.getScheme()) && uri.getScheme().contains("."));
   }



   /*******************************************************************************
    ** Getter for issuer.
    *******************************************************************************/
   public String getIssuer()
   {
      return (issuer);
   }



   /*******************************************************************************
    ** Setter for issuer.
    *******************************************************************************/
   public void setIssuer(String issuer)
   {
      this.issuer = issuer;
   }



   /*******************************************************************************
    ** Fluent setter for issuer.
    *******************************************************************************/
   public OAuth2NativeSessionLoginMetaData withIssuer(String issuer)
   {
      this.issuer = issuer;
      return (this);
   }



   /*******************************************************************************
    ** Getter for discoveryUrl.
    *******************************************************************************/
   public String getDiscoveryUrl()
   {
      return (discoveryUrl);
   }



   /*******************************************************************************
    ** Setter for discoveryUrl.
    *******************************************************************************/
   public void setDiscoveryUrl(String discoveryUrl)
   {
      this.discoveryUrl = discoveryUrl;
   }



   /*******************************************************************************
    ** Fluent setter for discoveryUrl.
    *******************************************************************************/
   public OAuth2NativeSessionLoginMetaData withDiscoveryUrl(String discoveryUrl)
   {
      this.discoveryUrl = discoveryUrl;
      return (this);
   }



   /*******************************************************************************
    ** Getter for redirectUris; callers cannot mutate the configured allowlist.
    *******************************************************************************/
   public List<String> getRedirectUris()
   {
      return (redirectUris == null ? null : List.copyOf(redirectUris));
   }



   /*******************************************************************************
    ** Setter for exact redirectUris.
    *******************************************************************************/
   public void setRedirectUris(List<String> redirectUris)
   {
      this.redirectUris = redirectUris == null ? null : new ArrayList<>(redirectUris);
   }



   /*******************************************************************************
    ** Fluent setter for exact redirectUris.
    *******************************************************************************/
   public OAuth2NativeSessionLoginMetaData withRedirectUris(List<String> redirectUris)
   {
      setRedirectUris(redirectUris);
      return (this);
   }
}
