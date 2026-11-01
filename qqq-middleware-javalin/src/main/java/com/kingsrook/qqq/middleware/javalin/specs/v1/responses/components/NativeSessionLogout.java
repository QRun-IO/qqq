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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2NativeSessionLogoutMetaData;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 ** Public opt-in logout policy, without tokens or provider credentials.
 *******************************************************************************/
public class NativeSessionLogout implements ToSchema
{
   /*******************************************************************************
    ** The supported provider logout protocol.
    *******************************************************************************/
   public enum Flow
   {
      OIDC_RP_INITIATED_LOGOUT
   }

   private Flow flow = Flow.OIDC_RP_INITIATED_LOGOUT;

   @OpenAPIListItems(String.class)
   private List<String> postLogoutRedirectUris;



   /*******************************************************************************
    ** Copy explicit provider-registered callback policy.
    *******************************************************************************/
   public NativeSessionLogout withConfiguration(OAuth2NativeSessionLogoutMetaData configuration)
   {
      postLogoutRedirectUris = configuration.getPostLogoutRedirectUris();
      return (this);
   }



   /*******************************************************************************
    ** Getter for flow.
    *******************************************************************************/
   public Flow getFlow()
   {
      return (flow);
   }



   /*******************************************************************************
    ** Getter for postLogoutRedirectUris.
    *******************************************************************************/
   public List<String> getPostLogoutRedirectUris()
   {
      return (postLogoutRedirectUris);
   }
}
