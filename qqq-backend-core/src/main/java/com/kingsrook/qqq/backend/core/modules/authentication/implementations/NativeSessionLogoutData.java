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
// SPDX-License-Identifier: Apache-2.0

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


/*******************************************************************************
 ** Sensitive, short-lived provider logout response. Never log or cache this value.
 *******************************************************************************/
public class NativeSessionLogoutData
{
   private final String issuer;
   private final String endSessionEndpoint;
   private final String postLogoutRedirectUri;
   private final String idTokenHint;



   /*******************************************************************************
    ** Construct a response only from verified provider/session bindings.
    *******************************************************************************/
   public NativeSessionLogoutData(String issuer, String endSessionEndpoint, String postLogoutRedirectUri, String idTokenHint)
   {
      this.issuer = issuer;
      this.endSessionEndpoint = endSessionEndpoint;
      this.postLogoutRedirectUri = postLogoutRedirectUri;
      this.idTokenHint = idTokenHint;
   }



   /*******************************************************************************
    ** Getter for issuer.
    *******************************************************************************/
   public String getIssuer()
   {
      return (issuer);
   }



   /*******************************************************************************
    ** Getter for endSessionEndpoint.
    *******************************************************************************/
   public String getEndSessionEndpoint()
   {
      return (endSessionEndpoint);
   }



   /*******************************************************************************
    ** Getter for postLogoutRedirectUri.
    *******************************************************************************/
   public String getPostLogoutRedirectUri()
   {
      return (postLogoutRedirectUri);
   }



   /*******************************************************************************
    ** Getter for idTokenHint.
    *******************************************************************************/
   public String getIdTokenHint()
   {
      return (idTokenHint);
   }
}
