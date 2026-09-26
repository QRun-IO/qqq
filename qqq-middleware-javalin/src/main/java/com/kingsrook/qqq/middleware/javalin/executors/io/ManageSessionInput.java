/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors.io;


/*******************************************************************************
 **
 *******************************************************************************/
public class ManageSessionInput extends AbstractMiddlewareInput
{
   private String accessToken;
   private String code;
   private String codeVerifier;
   private String redirectUri;
   private String sessionUUID;
   private String basicAuthString;



   /*******************************************************************************
    ** Getter for accessToken
    *******************************************************************************/
   public String getAccessToken()
   {
      return (this.accessToken);
   }



   /*******************************************************************************
    ** Setter for accessToken
    *******************************************************************************/
   public void setAccessToken(String accessToken)
   {
      this.accessToken = accessToken;
   }



   /*******************************************************************************
    ** Fluent setter for accessToken
    *******************************************************************************/
   public ManageSessionInput withAccessToken(String accessToken)
   {
      this.accessToken = accessToken;
      return (this);
   }



   /*******************************************************************************
    ** Getter for code
    *******************************************************************************/
   public String getCode()
   {
      return (code);
   }



   /*******************************************************************************
    ** Setter for code
    *******************************************************************************/
   public void setCode(String code)
   {
      this.code = code;
   }



   /*******************************************************************************
    ** Fluent setter for code
    *******************************************************************************/
   public ManageSessionInput withCode(String code)
   {
      this.code = code;
      return (this);
   }



   /*******************************************************************************
    ** Getter for codeVerifier
    *******************************************************************************/
   public String getCodeVerifier()
   {
      return (codeVerifier);
   }



   /*******************************************************************************
    ** Setter for codeVerifier
    *******************************************************************************/
   public void setCodeVerifier(String codeVerifier)
   {
      this.codeVerifier = codeVerifier;
   }



   /*******************************************************************************
    ** Fluent setter for codeVerifier
    *******************************************************************************/
   public ManageSessionInput withCodeVerifier(String codeVerifier)
   {
      this.codeVerifier = codeVerifier;
      return (this);
   }



   /*******************************************************************************
    ** Getter for redirectUri
    *******************************************************************************/
   public String getRedirectUri()
   {
      return (redirectUri);
   }



   /*******************************************************************************
    ** Setter for redirectUri
    *******************************************************************************/
   public void setRedirectUri(String redirectUri)
   {
      this.redirectUri = redirectUri;
   }



   /*******************************************************************************
    ** Fluent setter for redirectUri
    *******************************************************************************/
   public ManageSessionInput withRedirectUri(String redirectUri)
   {
      this.redirectUri = redirectUri;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sessionUUID
    *******************************************************************************/
   public String getSessionUUID()
   {
      return (sessionUUID);
   }



   /*******************************************************************************
    ** Setter for sessionUUID
    *******************************************************************************/
   public void setSessionUUID(String sessionUUID)
   {
      this.sessionUUID = sessionUUID;
   }



   /*******************************************************************************
    ** Fluent setter for sessionUUID
    *******************************************************************************/
   public ManageSessionInput withSessionUUID(String sessionUUID)
   {
      this.sessionUUID = sessionUUID;
      return (this);
   }



   /*******************************************************************************
    ** Getter for basicAuthString - the base64 user-id:password of an
    ** `Authorization: Basic` header (TABLE_BASED authentication)
    *******************************************************************************/
   public String getBasicAuthString()
   {
      return (this.basicAuthString);
   }



   /*******************************************************************************
    ** Setter for basicAuthString
    *******************************************************************************/
   public void setBasicAuthString(String basicAuthString)
   {
      this.basicAuthString = basicAuthString;
   }



   /*******************************************************************************
    ** Fluent setter for basicAuthString
    *******************************************************************************/
   public ManageSessionInput withBasicAuthString(String basicAuthString)
   {
      this.basicAuthString = basicAuthString;
      return (this);
   }

}
