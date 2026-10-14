/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.invoke;


import com.kingsrook.qqq.backend.core.utils.StringUtils;
import io.javalin.http.Context;


/*******************************************************************************
 ** The credentials a caller's request carried, to forward on in-process UI API
 ** calls so they authenticate exactly as the caller did: whatever the
 ** authentication module (session cookies for OAuth2/OIDC, Auth0, table-based,
 ** mock; Authorization or x-api-key headers for API keys and bearer tokens).
 ** Capture them with fromContext() while handling the user's request (e.g. at
 ** the start of an assistant turn); they hold secrets - never log them.
 **
 ** @param sessionUUID   the sessionUUID cookie
 ** @param sessionId     the sessionId cookie
 ** @param authorization the Authorization header
 ** @param apiKey        the x-api-key header
 ** @param inProcessSessionToken JVM-local session token, never captured from a client
 *******************************************************************************/
public record UiApiCredentials(String sessionUUID, String sessionId, String authorization, String apiKey, String inProcessSessionToken)
{

   /*******************************************************************************
    ** Backwards-compatible credentials without an in-process session.
    *******************************************************************************/
   public UiApiCredentials(String sessionUUID, String sessionId, String authorization, String apiKey)
   {
      this(sessionUUID, sessionId, authorization, apiKey, null);
   }



   /*******************************************************************************
    ** Credentials minted by a component in this JVM after authenticating a user.
    *******************************************************************************/
   public static UiApiCredentials ofInProcessSession(String token)
   {
      return (new UiApiCredentials(null, null, null, null, token));
   }



   /*******************************************************************************
    ** The credentials of a request being handled.
    *******************************************************************************/
   public static UiApiCredentials fromContext(Context context)
   {
      return (new UiApiCredentials(context.cookie("sessionUUID"), context.cookie("sessionId"), context.header("Authorization"), context.header("x-api-key")));
   }



   /*******************************************************************************
    ** Credentials of just a session UUID (the sessionUUID cookie).
    *******************************************************************************/
   public static UiApiCredentials ofSessionUUID(String sessionUUID)
   {
      return (new UiApiCredentials(sessionUUID, null, null, null));
   }



   /*******************************************************************************
    ** Whether any credential is present.
    *******************************************************************************/
   public boolean isPresent()
   {
      return (StringUtils.hasContent(sessionUUID) || StringUtils.hasContent(sessionId) || StringUtils.hasContent(authorization) || StringUtils.hasContent(apiKey) || StringUtils.hasContent(inProcessSessionToken));
   }



   /*******************************************************************************
    ** Never print the secrets.
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("UiApiCredentials[present=" + isPresent() + "]");
   }
}
