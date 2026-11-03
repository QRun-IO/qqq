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

package com.kingsrook.qqq.backend.core.model.metadata.authentication;


import java.io.Serializable;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;


/*******************************************************************************
 ** Explicit opt-in to provider logout; registration is separate from login.
 *******************************************************************************/
public class OAuth2NativeSessionLogoutMetaData implements Serializable
{
   private List<String> postLogoutRedirectUris;



   /*******************************************************************************
    ** Validate exact callbacks using the existing native URI policy.
    *******************************************************************************/
   @JsonIgnore
   public boolean isValid()
   {
      return (postLogoutRedirectUris != null && !postLogoutRedirectUris.isEmpty() && postLogoutRedirectUris.size() <= 32
         && postLogoutRedirectUris.stream().allMatch(OAuth2NativeSessionLoginMetaData::redirectUri));
   }



   /*******************************************************************************
    ** Getter for postLogoutRedirectUris.
    *******************************************************************************/
   public List<String> getPostLogoutRedirectUris()
   {
      return (postLogoutRedirectUris == null ? null : List.copyOf(postLogoutRedirectUris));
   }



   /*******************************************************************************
    ** Setter for postLogoutRedirectUris.
    *******************************************************************************/
   public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris)
   {
      this.postLogoutRedirectUris = postLogoutRedirectUris == null ? null : new java.util.ArrayList<>(postLogoutRedirectUris);
   }



   /*******************************************************************************
    ** Fluent setter for postLogoutRedirectUris.
    *******************************************************************************/
   public OAuth2NativeSessionLogoutMetaData withPostLogoutRedirectUris(List<String> postLogoutRedirectUris)
   {
      setPostLogoutRedirectUris(postLogoutRedirectUris);
      return (this);
   }
}
