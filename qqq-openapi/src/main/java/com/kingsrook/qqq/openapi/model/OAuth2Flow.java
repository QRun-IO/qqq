/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.openapi.model;


import java.util.Map;


/*******************************************************************************
 **
 *******************************************************************************/
public class OAuth2Flow
{
   private String              authorizationUrl;
   private String              tokenUrl;
   private Map<String, String> scopes;



   /*******************************************************************************
    ** Getter for authorizationUrl
    *******************************************************************************/
   public String getAuthorizationUrl()
   {
      return (this.authorizationUrl);
   }



   /*******************************************************************************
    ** Setter for authorizationUrl
    *******************************************************************************/
   public void setAuthorizationUrl(String authorizationUrl)
   {
      this.authorizationUrl = authorizationUrl;
   }



   /*******************************************************************************
    ** Fluent setter for authorizationUrl
    *******************************************************************************/
   public OAuth2Flow withAuthorizationUrl(String authorizationUrl)
   {
      this.authorizationUrl = authorizationUrl;
      return (this);
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
   public OAuth2Flow withTokenUrl(String tokenUrl)
   {
      this.tokenUrl = tokenUrl;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scopes
    *******************************************************************************/
   public Map<String, String> getScopes()
   {
      return (this.scopes);
   }



   /*******************************************************************************
    ** Setter for scopes
    *******************************************************************************/
   public void setScopes(Map<String, String> scopes)
   {
      this.scopes = scopes;
   }



   /*******************************************************************************
    ** Fluent setter for scopes
    *******************************************************************************/
   public OAuth2Flow withScopes(Map<String, String> scopes)
   {
      this.scopes = scopes;
      return (this);
   }

}
