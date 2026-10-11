/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.RefreshTokenCipher;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSessionRefreshToken;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Meta-data to provide details of an OAuth2 Authentication module.
 **
 ** baseUrl is used by the backend server for OIDC discovery and token exchange
 ** (server-to-server calls). In Kubernetes or environments behind a reverse proxy,
 ** this may be an internal/cluster-local URL (e.g., http://auth-service.auth.svc/).
 **
 ** externalBaseUrl, if set, is the URL sent to the frontend/SPA for browser-based
 ** OAuth2 redirects. This is needed when the backend cannot reach the OAuth2
 ** provider via the same URL that browsers use (e.g., Calico NetworkPolicy
 ** blocking pod-to-LoadBalancer traffic, or internal HTTP vs external HTTPS).
 ** If not set, baseUrl is used for both purposes (backwards compatible).
 *******************************************************************************/
public class OAuth2AuthenticationMetaData extends QAuthenticationMetaData
{
   private static final QLogger LOG = QLogger.getLogger(OAuth2AuthenticationMetaData.class);

   public static final int DEFAULT_REFRESH_TOKEN_SKEW_SECONDS = 60;

   ///////////////////////////////////////////////////////////////////////////////
   // URL used by the backend for OIDC discovery and token exchange.            //
   // In split-URL deployments, this should be the internal/cluster-reachable   //
   // URL for the OAuth2 provider.                                              //
   ///////////////////////////////////////////////////////////////////////////////
   private String baseUrl;

   ///////////////////////////////////////////////////////////////////////////////
   // Optional URL sent to the frontend for browser-based OAuth2 redirects.     //
   // When set, the /metaData/authentication API response returns this value    //
   // instead of baseUrl, allowing the SPA to redirect to a different (external //
   // HTTPS) URL than the one the backend uses internally.                      //
   // If not set, baseUrl is used for both backend and frontend (default).      //
   ///////////////////////////////////////////////////////////////////////////////
   private String externalBaseUrl;
   private String tokenUrl;
   private String clientId;
   private String scopes;

   private String userSessionTableName;
   private String redirectStateTableName;

   ///////////////////////////////////////////////////////////////////////////////
   // refresh-token support (#1041).  when the provider returns a refresh token //
   // (e.g., the offline_access scope was granted) and the refresh-token table  //
   // is defined in the instance, the token is stored server-side and used to  //
   // renew the access token as it expires.  refreshTokenTableName defaults to //
   // UserSessionRefreshToken.TABLE_NAME when not set.                          //
   ///////////////////////////////////////////////////////////////////////////////
   private Boolean refreshTokensEnabled      = true;
   private Integer refreshTokenSkewSeconds   = DEFAULT_REFRESH_TOKEN_SKEW_SECONDS;
   private Integer maxSessionLifetimeSeconds;
   private String  refreshTokenTableName;

   ///////////////////////////////////////////////////////////////////////////////
   // encryption of stored refresh tokens (#1048).  the key is a base64 32-byte //
   // AES key (e.g., "${env.QQQ_OAUTH2_REFRESH_TOKEN_KEY}"), required whenever  //
   // refresh tokens are stored.  previous keys (by key id) stay readable, for  //
   // rotation.  allowPlaintextRefreshTokens is a local-development escape      //
   // hatch only.  keys stay on the server - they are never serialized.         //
   ///////////////////////////////////////////////////////////////////////////////
   @JsonIgnore
   private String              refreshTokenEncryptionKey;
   private String              refreshTokenEncryptionKeyId;
   @JsonIgnore
   private Map<String, String> previousRefreshTokenEncryptionKeys;
   private Boolean             allowPlaintextRefreshTokens = false;

   ////////////////////////////////////////////////////////////////////////////////////////
   // keep this secret, on the server - don't let it be serialized and sent to a client! //
   ////////////////////////////////////////////////////////////////////////////////////////
   @JsonIgnore
   private String clientSecret;



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public OAuth2AuthenticationMetaData()
   {
      super();
      setType(QAuthenticationType.OAUTH2);

      //////////////////////////////////////////////////////////
      // ensure this module is registered with the dispatcher //
      //////////////////////////////////////////////////////////
      QAuthenticationModuleDispatcher.registerModule(QAuthenticationType.OAUTH2.getName(), OAuth2AuthenticationModule.class.getName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator qInstanceValidator)
   {
      super.validate(qInstance, qInstanceValidator);

      String prefix = "OAuth2AuthenticationMetaData (named '" + getName() + "'): ";

      qInstanceValidator.assertCondition(StringUtils.hasContent(baseUrl), prefix + "baseUrl must be set");
      qInstanceValidator.assertCondition(StringUtils.hasContent(clientId), prefix + "clientId must be set");
      qInstanceValidator.assertCondition(StringUtils.hasContent(clientSecret), prefix + "clientSecret must be set");
      qInstanceValidator.assertCondition(StringUtils.hasContent(scopes), prefix + "scopes must be set");

      if(qInstanceValidator.assertCondition(StringUtils.hasContent(userSessionTableName), prefix + "userSessionTableName must be set"))
      {
         qInstanceValidator.assertCondition(qInstance.getTable(userSessionTableName) != null, prefix + "userSessionTableName ('" + userSessionTableName + "') was not found in the instance");
      }

      if(qInstanceValidator.assertCondition(StringUtils.hasContent(redirectStateTableName), prefix + "redirectStateTableName must be set"))
      {
         qInstanceValidator.assertCondition(qInstance.getTable(redirectStateTableName) != null, prefix + "redirectStateTableName ('" + redirectStateTableName + "') was not found in the instance");
      }

      if(StringUtils.hasContent(refreshTokenTableName))
      {
         qInstanceValidator.assertCondition(qInstance.getTable(refreshTokenTableName) != null, prefix + "refreshTokenTableName ('" + refreshTokenTableName + "') was not found in the instance");
      }

      qInstanceValidator.assertCondition(refreshTokenSkewSeconds == null || refreshTokenSkewSeconds >= 0, prefix + "refreshTokenSkewSeconds may not be negative");
      qInstanceValidator.assertCondition(maxSessionLifetimeSeconds == null || maxSessionLifetimeSeconds > 0, prefix + "maxSessionLifetimeSeconds must be positive when set");

      validateRefreshTokenEncryption(qInstance, qInstanceValidator, prefix);
   }



   /*******************************************************************************
    ** When refresh tokens will be stored (enabled, and the refresh-token table is
    ** in the instance), require a valid encryption key - unless plaintext storage
    ** was explicitly allowed (local development only), which is loudly logged.
    ** Any key settings that are given must be valid either way.
    *******************************************************************************/
   private void validateRefreshTokenEncryption(QInstance qInstance, QInstanceValidator qInstanceValidator, String prefix)
   {
      RefreshTokenCipher cipher;
      try
      {
         cipher = RefreshTokenCipher.fromMetaData(this);
      }
      catch(QException e)
      {
         qInstanceValidator.assertCondition(false, prefix + "invalid refresh-token encryption settings: " + e.getMessage());
         return;
      }

      String  effectiveTableName  = Objects.requireNonNullElse(refreshTokenTableName, UserSessionRefreshToken.TABLE_NAME);
      boolean storesRefreshTokens = !Boolean.FALSE.equals(refreshTokensEnabled) && qInstance.getTable(effectiveTableName) != null;
      if(!storesRefreshTokens || cipher != null)
      {
         return;
      }

      if(Boolean.TRUE.equals(allowPlaintextRefreshTokens))
      {
         LOG.warn("SECURITY WARNING: OAuth2 refresh tokens will be stored UNENCRYPTED, because allowPlaintextRefreshTokens is true.  "
               + "Anyone who can read the refresh-token table, or a backup of it, can impersonate users.  "
               + "Use this for local development only, and set refreshTokenEncryptionKey everywhere else.",
            logPair("authenticationName", getName()), logPair("refreshTokenTableName", effectiveTableName));
         return;
      }

      qInstanceValidator.assertCondition(false, prefix + "refreshTokenEncryptionKey must be set (a base64 32-byte key, e.g., from: openssl rand -base64 32), "
         + "because refresh tokens are enabled and the refresh-token table ('" + effectiveTableName + "') is defined");
   }



   /*******************************************************************************
    ** Fluent setter, override to help fluent flows
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withBaseUrl(String baseUrl)
   {
      setBaseUrl(baseUrl);
      return this;
   }



   /*******************************************************************************
    ** Getter for baseUrl
    **
    *******************************************************************************/
   public String getBaseUrl()
   {
      return baseUrl;
   }



   /*******************************************************************************
    ** Setter for baseUrl
    **
    *******************************************************************************/
   public void setBaseUrl(String baseUrl)
   {
      this.baseUrl = baseUrl;
   }



   /*******************************************************************************
    ** Getter for externalBaseUrl
    *******************************************************************************/
   public String getExternalBaseUrl()
   {
      return (this.externalBaseUrl);
   }



   /*******************************************************************************
    ** Setter for externalBaseUrl
    *******************************************************************************/
   public void setExternalBaseUrl(String externalBaseUrl)
   {
      this.externalBaseUrl = externalBaseUrl;
   }



   /*******************************************************************************
    ** Fluent setter for externalBaseUrl
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withExternalBaseUrl(String externalBaseUrl)
   {
      this.externalBaseUrl = externalBaseUrl;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter, override to help fluent flows
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withClientId(String clientId)
   {
      setClientId(clientId);
      return this;
   }



   /*******************************************************************************
    ** Getter for clientId
    **
    *******************************************************************************/
   public String getClientId()
   {
      return clientId;
   }



   /*******************************************************************************
    ** Setter for clientId
    **
    *******************************************************************************/
   public void setClientId(String clientId)
   {
      this.clientId = clientId;
   }



   /*******************************************************************************
    ** Fluent setter, override to help fluent flows
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withClientSecret(String clientSecret)
   {
      setClientSecret(clientSecret);
      return this;
   }



   /*******************************************************************************
    ** Getter for clientSecret
    **
    *******************************************************************************/
   public String getClientSecret()
   {
      return clientSecret;
   }



   /*******************************************************************************
    ** Setter for clientSecret
    **
    *******************************************************************************/
   public void setClientSecret(String clientSecret)
   {
      this.clientSecret = clientSecret;
   }



   /*******************************************************************************
    ** Getter for tokenUrl
    *******************************************************************************/
   public String getTokenUrl()
   {
      return (this.tokenUrl);
   }



   /*******************************************************************************
    ** Setter for tokenUrl
    *******************************************************************************/
   public void setTokenUrl(String tokenUrl)
   {
      this.tokenUrl = tokenUrl;
   }



   /*******************************************************************************
    ** Fluent setter for tokenUrl
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withTokenUrl(String tokenUrl)
   {
      this.tokenUrl = tokenUrl;
      return (this);
   }



   /*******************************************************************************
    ** Getter for userSessionTableName
    *******************************************************************************/
   public String getUserSessionTableName()
   {
      return (this.userSessionTableName);
   }



   /*******************************************************************************
    ** Setter for userSessionTableName
    *******************************************************************************/
   public void setUserSessionTableName(String userSessionTableName)
   {
      this.userSessionTableName = userSessionTableName;
   }



   /*******************************************************************************
    ** Fluent setter for userSessionTableName
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withUserSessionTableName(String userSessionTableName)
   {
      this.userSessionTableName = userSessionTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for redirectStateTableName
    *******************************************************************************/
   public String getRedirectStateTableName()
   {
      return (this.redirectStateTableName);
   }



   /*******************************************************************************
    ** Setter for redirectStateTableName
    *******************************************************************************/
   public void setRedirectStateTableName(String redirectStateTableName)
   {
      this.redirectStateTableName = redirectStateTableName;
   }



   /*******************************************************************************
    ** Fluent setter for redirectStateTableName
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withRedirectStateTableName(String redirectStateTableName)
   {
      this.redirectStateTableName = redirectStateTableName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for scopes
    *******************************************************************************/
   public String getScopes()
   {
      return (this.scopes);
   }



   /*******************************************************************************
    ** Setter for scopes
    *******************************************************************************/
   public void setScopes(String scopes)
   {
      this.scopes = scopes;
   }



   /*******************************************************************************
    ** Fluent setter for scopes
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withScopes(String scopes)
   {
      this.scopes = scopes;
      return (this);
   }



   /*******************************************************************************
    ** Getter for refreshTokensEnabled - whether refresh tokens returned by the
    ** provider are stored server-side and used to renew expiring access tokens.
    *******************************************************************************/
   public Boolean getRefreshTokensEnabled()
   {
      return (this.refreshTokensEnabled);
   }



   /*******************************************************************************
    ** Setter for refreshTokensEnabled
    *******************************************************************************/
   public void setRefreshTokensEnabled(Boolean refreshTokensEnabled)
   {
      this.refreshTokensEnabled = refreshTokensEnabled;
   }



   /*******************************************************************************
    ** Fluent setter for refreshTokensEnabled
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withRefreshTokensEnabled(Boolean refreshTokensEnabled)
   {
      this.refreshTokensEnabled = refreshTokensEnabled;
      return (this);
   }



   /*******************************************************************************
    ** Getter for refreshTokenSkewSeconds - how many seconds before an access
    ** token's expiry it is proactively refreshed.
    *******************************************************************************/
   public Integer getRefreshTokenSkewSeconds()
   {
      return (this.refreshTokenSkewSeconds);
   }



   /*******************************************************************************
    ** Setter for refreshTokenSkewSeconds
    *******************************************************************************/
   public void setRefreshTokenSkewSeconds(Integer refreshTokenSkewSeconds)
   {
      this.refreshTokenSkewSeconds = refreshTokenSkewSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for refreshTokenSkewSeconds
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withRefreshTokenSkewSeconds(Integer refreshTokenSkewSeconds)
   {
      this.refreshTokenSkewSeconds = refreshTokenSkewSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxSessionLifetimeSeconds - optional cap on how long (from the
    ** initial login) a session may be extended by refreshing.  null = no cap.
    *******************************************************************************/
   public Integer getMaxSessionLifetimeSeconds()
   {
      return (this.maxSessionLifetimeSeconds);
   }



   /*******************************************************************************
    ** Setter for maxSessionLifetimeSeconds
    *******************************************************************************/
   public void setMaxSessionLifetimeSeconds(Integer maxSessionLifetimeSeconds)
   {
      this.maxSessionLifetimeSeconds = maxSessionLifetimeSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for maxSessionLifetimeSeconds
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withMaxSessionLifetimeSeconds(Integer maxSessionLifetimeSeconds)
   {
      this.maxSessionLifetimeSeconds = maxSessionLifetimeSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for refreshTokenTableName - the table where refresh tokens are kept
    ** (server-side only).  When null, UserSessionRefreshToken.TABLE_NAME is used.
    *******************************************************************************/
   public String getRefreshTokenTableName()
   {
      return (this.refreshTokenTableName);
   }



   /*******************************************************************************
    ** Setter for refreshTokenTableName
    *******************************************************************************/
   public void setRefreshTokenTableName(String refreshTokenTableName)
   {
      this.refreshTokenTableName = refreshTokenTableName;
   }



   /*******************************************************************************
    ** Fluent setter for refreshTokenTableName
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withRefreshTokenTableName(String refreshTokenTableName)
   {
      this.refreshTokenTableName = refreshTokenTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for refreshTokenEncryptionKey - the base64 32-byte AES key (or a
    ** ${env.X} / ${prop.X} reference to one) that encrypts stored refresh tokens.
    *******************************************************************************/
   public String getRefreshTokenEncryptionKey()
   {
      return (this.refreshTokenEncryptionKey);
   }



   /*******************************************************************************
    ** Setter for refreshTokenEncryptionKey
    *******************************************************************************/
   public void setRefreshTokenEncryptionKey(String refreshTokenEncryptionKey)
   {
      this.refreshTokenEncryptionKey = refreshTokenEncryptionKey;
   }



   /*******************************************************************************
    ** Fluent setter for refreshTokenEncryptionKey
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withRefreshTokenEncryptionKey(String refreshTokenEncryptionKey)
   {
      this.refreshTokenEncryptionKey = refreshTokenEncryptionKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for refreshTokenEncryptionKeyId - the id of the current key, stored
    ** with each encrypted value (default: RefreshTokenCipher.DEFAULT_KEY_ID).
    *******************************************************************************/
   public String getRefreshTokenEncryptionKeyId()
   {
      return (this.refreshTokenEncryptionKeyId);
   }



   /*******************************************************************************
    ** Setter for refreshTokenEncryptionKeyId
    *******************************************************************************/
   public void setRefreshTokenEncryptionKeyId(String refreshTokenEncryptionKeyId)
   {
      this.refreshTokenEncryptionKeyId = refreshTokenEncryptionKeyId;
   }



   /*******************************************************************************
    ** Fluent setter for refreshTokenEncryptionKeyId
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withRefreshTokenEncryptionKeyId(String refreshTokenEncryptionKeyId)
   {
      this.refreshTokenEncryptionKeyId = refreshTokenEncryptionKeyId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for previousRefreshTokenEncryptionKeys - map of key id to key (or a
    ** variable reference to one) for retired keys, which are only used to decrypt.
    *******************************************************************************/
   public Map<String, String> getPreviousRefreshTokenEncryptionKeys()
   {
      return (this.previousRefreshTokenEncryptionKeys);
   }



   /*******************************************************************************
    ** Setter for previousRefreshTokenEncryptionKeys
    *******************************************************************************/
   public void setPreviousRefreshTokenEncryptionKeys(Map<String, String> previousRefreshTokenEncryptionKeys)
   {
      this.previousRefreshTokenEncryptionKeys = previousRefreshTokenEncryptionKeys;
   }



   /*******************************************************************************
    ** Fluent setter for previousRefreshTokenEncryptionKeys
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withPreviousRefreshTokenEncryptionKeys(Map<String, String> previousRefreshTokenEncryptionKeys)
   {
      this.previousRefreshTokenEncryptionKeys = previousRefreshTokenEncryptionKeys;
      return (this);
   }



   /*******************************************************************************
    ** Getter for allowPlaintextRefreshTokens - a local-development escape hatch:
    ** when true and no key is set, refresh tokens are stored unencrypted.
    *******************************************************************************/
   public Boolean getAllowPlaintextRefreshTokens()
   {
      return (this.allowPlaintextRefreshTokens);
   }



   /*******************************************************************************
    ** Setter for allowPlaintextRefreshTokens
    *******************************************************************************/
   public void setAllowPlaintextRefreshTokens(Boolean allowPlaintextRefreshTokens)
   {
      this.allowPlaintextRefreshTokens = allowPlaintextRefreshTokens;
   }



   /*******************************************************************************
    ** Fluent setter for allowPlaintextRefreshTokens
    *******************************************************************************/
   public OAuth2AuthenticationMetaData withAllowPlaintextRefreshTokens(Boolean allowPlaintextRefreshTokens)
   {
      this.allowPlaintextRefreshTokens = allowPlaintextRefreshTokens;
      return (this);
   }

}
