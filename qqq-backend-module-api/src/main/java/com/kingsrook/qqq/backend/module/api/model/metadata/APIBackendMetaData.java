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

package com.kingsrook.qqq.backend.module.api.model.metadata;


import java.io.Serializable;
import java.util.HashMap;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.api.APIBackendModule;
import com.kingsrook.qqq.backend.module.api.model.AuthorizationType;


/*******************************************************************************
 ** Meta-data to provide details of an API backend (e.g., connection params)
 *******************************************************************************/
public class APIBackendMetaData extends QBackendMetaData
{
   private String baseUrl;
   private String apiKey;
   private String clientId;
   private String clientSecret;
   private String username;
   private String password;
   private String apiKeyQueryParamName;

   private AuthorizationType authorizationType;
   private String            contentType; // todo enum?

   private QCodeReference actionUtil;

   private HashMap<String, Serializable> customValues = new HashMap<>();



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public APIBackendMetaData()
   {
      super();
      setBackendType(APIBackendModule.class);
   }



   /*******************************************************************************
    ** Fluent setter, override to help fluent flows
    *******************************************************************************/
   @Override
   public APIBackendMetaData withName(String name)
   {
      setName(name);
      return this;
   }

   // todo?
   // /*******************************************************************************
   //  ** Called by the QInstanceEnricher - to do backend-type-specific enrichments.
   //  ** Original use case is:  reading secrets into fields (e.g., passwords).
   //  *******************************************************************************/
   // @Override
   // public void enrich()
   // {
   //    super.enrich();
   //    QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
   //    username = interpreter.interpret(username);
   //    password = interpreter.interpret(password);
   // }



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
    ** Fluent setter for baseUrl
    **
    *******************************************************************************/
   public APIBackendMetaData withBaseUrl(String baseUrl)
   {
      this.baseUrl = baseUrl;
      return (this);
   }



   /*******************************************************************************
    ** Getter for apiKey
    **
    *******************************************************************************/
   public String getApiKey()
   {
      return apiKey;
   }



   /*******************************************************************************
    ** Setter for apiKey
    **
    *******************************************************************************/
   public void setApiKey(String apiKey)
   {
      this.apiKey = apiKey;
   }



   /*******************************************************************************
    ** Fluent setter for apiKey
    **
    *******************************************************************************/
   public APIBackendMetaData withApiKey(String apiKey)
   {
      this.apiKey = apiKey;
      return (this);
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
    ** Fluent setter for clientId
    **
    *******************************************************************************/
   public APIBackendMetaData withClientId(String clientId)
   {
      this.clientId = clientId;
      return (this);
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
    ** Fluent setter for clientSecret
    **
    *******************************************************************************/
   public APIBackendMetaData withClientSecret(String clientSecret)
   {
      this.clientSecret = clientSecret;
      return (this);
   }



   /*******************************************************************************
    ** Getter for username
    **
    *******************************************************************************/
   public String getUsername()
   {
      return username;
   }



   /*******************************************************************************
    ** Setter for username
    **
    *******************************************************************************/
   public void setUsername(String username)
   {
      this.username = username;
   }



   /*******************************************************************************
    ** Fluent setter for username
    **
    *******************************************************************************/
   public APIBackendMetaData withUsername(String username)
   {
      this.username = username;
      return (this);
   }



   /*******************************************************************************
    ** Getter for password
    **
    *******************************************************************************/
   public String getPassword()
   {
      return password;
   }



   /*******************************************************************************
    ** Setter for password
    **
    *******************************************************************************/
   public void setPassword(String password)
   {
      this.password = password;
   }



   /*******************************************************************************
    ** Fluent setter for password
    **
    *******************************************************************************/
   public APIBackendMetaData withPassword(String password)
   {
      this.password = password;
      return (this);
   }



   /*******************************************************************************
    ** Getter for authorizationType
    **
    *******************************************************************************/
   public AuthorizationType getAuthorizationType()
   {
      return authorizationType;
   }



   /*******************************************************************************
    ** Setter for authorizationType
    **
    *******************************************************************************/
   public void setAuthorizationType(AuthorizationType authorizationType)
   {
      this.authorizationType = authorizationType;
   }



   /*******************************************************************************
    ** Fluent setter for authorizationType
    **
    *******************************************************************************/
   public APIBackendMetaData withAuthorizationType(AuthorizationType authorizationType)
   {
      this.authorizationType = authorizationType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contentType
    **
    *******************************************************************************/
   public String getContentType()
   {
      return contentType;
   }



   /*******************************************************************************
    ** Setter for contentType
    **
    *******************************************************************************/
   public void setContentType(String contentType)
   {
      this.contentType = contentType;
   }



   /*******************************************************************************
    ** Fluent setter for contentType
    **
    *******************************************************************************/
   public APIBackendMetaData withContentType(String contentType)
   {
      this.contentType = contentType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for actionUtil
    **
    *******************************************************************************/
   public QCodeReference getActionUtil()
   {
      return actionUtil;
   }



   /*******************************************************************************
    ** Setter for actionUtil
    **
    *******************************************************************************/
   public void setActionUtil(QCodeReference actionUtil)
   {
      this.actionUtil = actionUtil;
   }



   /*******************************************************************************
    ** Fluent setter for actionUtil
    **
    *******************************************************************************/
   public APIBackendMetaData withActionUtil(QCodeReference actionUtil)
   {
      this.actionUtil = actionUtil;
      return (this);
   }



   /*******************************************************************************
    ** Getter for customValues
    **
    *******************************************************************************/
   public HashMap<String, Serializable> getCustomValues()
   {
      return customValues;
   }



   /*******************************************************************************
    ** Getter for a single customValue
    **
    *******************************************************************************/
   public Serializable getCustomValue(String key)
   {
      return customValues.get(key);
   }



   /*******************************************************************************
    ** Setter for customValues
    **
    *******************************************************************************/
   public void setCustomValues(HashMap<String, Serializable> customValues)
   {
      this.customValues = customValues;
   }



   /*******************************************************************************
    ** Fluent setter for customValues
    **
    *******************************************************************************/
   public APIBackendMetaData withCustomValues(HashMap<String, Serializable> customValues)
   {
      this.customValues = customValues;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for a single customValue
    **
    *******************************************************************************/
   public APIBackendMetaData withCustomValue(String key, Serializable value)
   {
      this.customValues.put(key, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void performValidation(QInstanceValidator qInstanceValidator)
   {
      qInstanceValidator.assertCondition(StringUtils.hasContent(baseUrl), "Missing baseUrl for API backend: " + getName());

      if(AuthorizationType.API_KEY_QUERY_PARAM.equals(authorizationType))
      {
         qInstanceValidator.assertCondition(StringUtils.hasContent(apiKey), "Missing apiKey for API backend: " + getName() + " (required when using AuthorizationType=API_KEY_QUERY_PARAM))");
         qInstanceValidator.assertCondition(StringUtils.hasContent(apiKeyQueryParamName), "Missing apiKeyQueryParamName for API backend: " + getName() + " (required when using AuthorizationType=API_KEY_QUERY_PARAM))");
      }
      else
      {
         qInstanceValidator.assertCondition(!StringUtils.hasContent(apiKeyQueryParamName), "Unexpected apiKeyQueryParamName for API backend: " + getName() + " (only allowed when using AuthorizationType=API_KEY_QUERY_PARAM))");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean requiresPrimaryKeyOnTables()
   {
      return (false);
   }



   /*******************************************************************************
    ** Getter for apiKeyQueryParamName
    *******************************************************************************/
   public String getApiKeyQueryParamName()
   {
      return (this.apiKeyQueryParamName);
   }



   /*******************************************************************************
    ** Setter for apiKeyQueryParamName
    *******************************************************************************/
   public void setApiKeyQueryParamName(String apiKeyQueryParamName)
   {
      this.apiKeyQueryParamName = apiKeyQueryParamName;
   }



   /*******************************************************************************
    ** Fluent setter for apiKeyQueryParamName
    *******************************************************************************/
   public APIBackendMetaData withApiKeyQueryParamName(String apiKeyQueryParamName)
   {
      this.apiKeyQueryParamName = apiKeyQueryParamName;
      return (this);
   }

}
