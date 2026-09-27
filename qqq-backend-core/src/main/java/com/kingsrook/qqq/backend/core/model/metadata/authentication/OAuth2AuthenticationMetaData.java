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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


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


}
