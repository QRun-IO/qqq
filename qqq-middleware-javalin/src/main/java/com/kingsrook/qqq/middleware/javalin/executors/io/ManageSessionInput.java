/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.util.Map;


/*******************************************************************************
 **
 *******************************************************************************/
public class ManageSessionInput extends AbstractMiddlewareInput
{
   private String accessToken;
   private String code;
   private String codeVerifier;
   private String redirectUri;
   private Map<String, String> allFields;



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
      return (this.code);
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
      return (this.codeVerifier);
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
      return (this.redirectUri);
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
    ** Getter for allFields
    *******************************************************************************/
   public Map<String, String> getAllFields()
   {
      return (this.allFields);
   }



   /*******************************************************************************
    ** Setter for allFields
    *******************************************************************************/
   public void setAllFields(Map<String, String> allFields)
   {
      this.allFields = allFields;
   }



   /*******************************************************************************
    ** Fluent setter for allFields
    *******************************************************************************/
   public ManageSessionInput withAllFields(Map<String, String> allFields)
   {
      this.allFields = allFields;
      return (this);
   }

}
