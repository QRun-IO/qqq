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


import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule;


/*******************************************************************************
 ** Meta-data to provide details of an Auth0 Authentication module
 *******************************************************************************/
public class Auth0AuthenticationMetaData extends QAuthenticationMetaData
{
   private String baseUrl;
   private String clientId;
   private String audience;

   ////////////////////////////////////////////////////////////////////////////////////////
   // keep this secret, on the server - don't let it be serialized and sent to a client! //
   ////////////////////////////////////////////////////////////////////////////////////////
   @JsonIgnore
   private String clientSecret;

   ///////////////////////////////////////////////////////////////////////////////////////////
   // these tables and fields are used to store auth0 application data and access data, the //
   // access token can potentially be too large to send to qqq because of size limiations,  //
   // so we need to hash it and send the qqq user a version mapped to a smaller token       //
   ///////////////////////////////////////////////////////////////////////////////////////////
   private String clientAuth0ApplicationTableName;
   private String accessTokenTableName;

   /////////////////////////////////////////
   // fields on the auth0ApplicationTable //
   /////////////////////////////////////////
   private String       applicationNameField;
   private String       auth0ClientIdField;
   private String       auth0ClientSecretField;
   private Serializable qqqRecordIdField;

   /////////////////////////////////////
   // fields on the accessToken table //
   /////////////////////////////////////
   private String clientAuth0ApplicationIdField;
   private String auth0AccessTokenField;
   private String qqqAccessTokenField;
   private String qqqApiKeyField;
   private String expiresInSecondsField;



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public Auth0AuthenticationMetaData()
   {
      super();
      setType(QAuthenticationType.AUTH_0);

      //////////////////////////////////////////////////////////
      // ensure this module is registered with the dispatcher //
      //////////////////////////////////////////////////////////
      QAuthenticationModuleDispatcher.registerModule(QAuthenticationType.AUTH_0.getName(), Auth0AuthenticationModule.class.getName());
   }



   /*******************************************************************************
    ** Fluent setter, override to help fluent flows
    *******************************************************************************/
   public Auth0AuthenticationMetaData withBaseUrl(String baseUrl)
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
    ** Fluent setter, override to help fluent flows
    *******************************************************************************/
   public Auth0AuthenticationMetaData withClientId(String clientId)
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
   public Auth0AuthenticationMetaData withClientSecret(String clientSecret)
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
    ** Getter for audience
    *******************************************************************************/
   public String getAudience()
   {
      return (this.audience);
   }



   /*******************************************************************************
    ** Setter for audience
    *******************************************************************************/
   public void setAudience(String audience)
   {
      this.audience = audience;
   }



   /*******************************************************************************
    ** Fluent setter for audience
    *******************************************************************************/
   public Auth0AuthenticationMetaData withAudience(String audience)
   {
      this.audience = audience;
      return (this);
   }



   /*******************************************************************************
    ** Getter for clientAuth0ApplicationTableName
    **
    *******************************************************************************/
   public String getClientAuth0ApplicationTableName()
   {
      return clientAuth0ApplicationTableName;
   }



   /*******************************************************************************
    ** Setter for clientAuth0ApplicationTableName
    **
    *******************************************************************************/
   public void setClientAuth0ApplicationTableName(String clientAuth0ApplicationTableName)
   {
      this.clientAuth0ApplicationTableName = clientAuth0ApplicationTableName;
   }



   /*******************************************************************************
    ** Fluent setter for clientAuth0ApplicationTableName
    **
    *******************************************************************************/
   public Auth0AuthenticationMetaData withClientAuth0ApplicationTableName(String clientAuth0ApplicationTableName)
   {
      this.clientAuth0ApplicationTableName = clientAuth0ApplicationTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for accessTokenTableName
    **
    *******************************************************************************/
   public String getAccessTokenTableName()
   {
      return accessTokenTableName;
   }



   /*******************************************************************************
    ** Setter for accessTokenTableName
    **
    *******************************************************************************/
   public void setAccessTokenTableName(String accessTokenTableName)
   {
      this.accessTokenTableName = accessTokenTableName;
   }



   /*******************************************************************************
    ** Fluent setter for accessTokenTableName
    **
    *******************************************************************************/
   public Auth0AuthenticationMetaData withAccessTokenTableName(String accessTokenTableName)
   {
      this.accessTokenTableName = accessTokenTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for applicationNameField
    **
    *******************************************************************************/
   public String getApplicationNameField()
   {
      return applicationNameField;
   }



   /*******************************************************************************
    ** Setter for applicationNameField
    **
    *******************************************************************************/
   public void setApplicationNameField(String applicationNameField)
   {
      this.applicationNameField = applicationNameField;
   }



   /*******************************************************************************
    ** Fluent setter for applicationNameField
    **
    *******************************************************************************/
   public Auth0AuthenticationMetaData withApplicationNameField(String applicationNameField)
   {
      this.applicationNameField = applicationNameField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for auth0ClientIdField
    **
    *******************************************************************************/
   public String getAuth0ClientIdField()
   {
      return auth0ClientIdField;
   }



   /*******************************************************************************
    ** Setter for auth0ClientIdField
    **
    *******************************************************************************/
   public void setAuth0ClientIdField(String auth0ClientIdField)
   {
      this.auth0ClientIdField = auth0ClientIdField;
   }



   /*******************************************************************************
    ** Fluent setter for auth0ClientIdField
    **
    *******************************************************************************/
   public Auth0AuthenticationMetaData withAuth0ClientIdField(String auth0ClientIdField)
   {
      this.auth0ClientIdField = auth0ClientIdField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for qqqRecordIdField
    **
    *******************************************************************************/
   public Serializable getQqqRecordIdField()
   {
      return qqqRecordIdField;
   }



   /*******************************************************************************
    ** Setter for qqqRecordIdField
    **
    *******************************************************************************/
   public void setQqqRecordIdField(Serializable qqqRecordIdField)
   {
      this.qqqRecordIdField = qqqRecordIdField;
   }



   /*******************************************************************************
    ** Fluent setter for qqqRecordIdField
    **
    *******************************************************************************/
   public Auth0AuthenticationMetaData withQqqRecordIdField(Serializable qqqRecordIdField)
   {
      this.qqqRecordIdField = qqqRecordIdField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for clientAuth0ApplicationIdField
    **
    *******************************************************************************/
   public String getClientAuth0ApplicationIdField()
   {
      return clientAuth0ApplicationIdField;
   }



   /*******************************************************************************
    ** Setter for clientAuth0ApplicationIdField
    **
    *******************************************************************************/
   public void setClientAuth0ApplicationIdField(String clientAuth0ApplicationIdField)
   {
      this.clientAuth0ApplicationIdField = clientAuth0ApplicationIdField;
   }



   /*******************************************************************************
    ** Fluent setter for clientAuth0ApplicationIdField
    **
    *******************************************************************************/
   public Auth0AuthenticationMetaData withClientAuth0ApplicationIdField(String clientAuth0ApplicationIdField)
   {
      this.clientAuth0ApplicationIdField = clientAuth0ApplicationIdField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for auth0AccessTokenField
    **
    *******************************************************************************/
   public String getAuth0AccessTokenField()
   {
      return auth0AccessTokenField;
   }



   /*******************************************************************************
    ** Setter for auth0AccessTokenField
    **
    *******************************************************************************/
   public void setAuth0AccessTokenField(String auth0AccessTokenField)
   {
      this.auth0AccessTokenField = auth0AccessTokenField;
   }



   /*******************************************************************************
    ** Fluent setter for auth0AccessTokenField
    **
    *******************************************************************************/
   public Auth0AuthenticationMetaData withAuth0AccessTokenField(String auth0AccessTokenField)
   {
      this.auth0AccessTokenField = auth0AccessTokenField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for qqqAccessTokenField
    **
    *******************************************************************************/
   public String getQqqAccessTokenField()
   {
      return qqqAccessTokenField;
   }



   /*******************************************************************************
    ** Setter for qqqAccessTokenField
    **
    *******************************************************************************/
   public void setQqqAccessTokenField(String qqqAccessTokenField)
   {
      this.qqqAccessTokenField = qqqAccessTokenField;
   }



   /*******************************************************************************
    ** Fluent setter for qqqAccessTokenField
    **
    *******************************************************************************/
   public Auth0AuthenticationMetaData withQqqAccessTokenField(String qqqAccessTokenField)
   {
      this.qqqAccessTokenField = qqqAccessTokenField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for expiresInSecondsField
    **
    *******************************************************************************/
   public String getExpiresInSecondsField()
   {
      return expiresInSecondsField;
   }



   /*******************************************************************************
    ** Setter for expiresInSecondsField
    **
    *******************************************************************************/
   public void setExpiresInSecondsField(String expiresInSecondsField)
   {
      this.expiresInSecondsField = expiresInSecondsField;
   }



   /*******************************************************************************
    ** Fluent setter for expiresInSecondsField
    **
    *******************************************************************************/
   public Auth0AuthenticationMetaData withExpiresInSecondsField(String expiresInSecondsField)
   {
      this.expiresInSecondsField = expiresInSecondsField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for qqqApiKeyField
    *******************************************************************************/
   public String getQqqApiKeyField()
   {
      return (this.qqqApiKeyField);
   }



   /*******************************************************************************
    ** Setter for qqqApiKeyField
    *******************************************************************************/
   public void setQqqApiKeyField(String qqqApiKeyField)
   {
      this.qqqApiKeyField = qqqApiKeyField;
   }



   /*******************************************************************************
    ** Fluent setter for qqqApiKeyField
    *******************************************************************************/
   public Auth0AuthenticationMetaData withQqqApiKeyField(String qqqApiKeyField)
   {
      this.qqqApiKeyField = qqqApiKeyField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for auth0ClientSecretField
    *******************************************************************************/
   public String getAuth0ClientSecretField()
   {
      return (this.auth0ClientSecretField);
   }



   /*******************************************************************************
    ** Setter for auth0ClientSecretField
    *******************************************************************************/
   public void setAuth0ClientSecretField(String auth0ClientSecretField)
   {
      this.auth0ClientSecretField = auth0ClientSecretField;
   }



   /*******************************************************************************
    ** Fluent setter for auth0ClientSecretField
    *******************************************************************************/
   public Auth0AuthenticationMetaData withAuth0ClientSecretField(String auth0ClientSecretField)
   {
      this.auth0ClientSecretField = auth0ClientSecretField;
      return (this);
   }

}
