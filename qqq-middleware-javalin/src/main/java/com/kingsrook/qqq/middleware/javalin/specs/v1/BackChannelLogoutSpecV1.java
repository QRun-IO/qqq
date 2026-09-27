/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.executors.BackChannelLogoutExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.BackChannelLogoutInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BackChannelLogoutResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;


/*******************************************************************************
 ** Endpoint spec for OIDC Back-Channel Logout.
 **
 ** This endpoint receives logout notifications from the Identity Provider (IdP)
 ** when a user logs out from the IdP or another application in the SSO ecosystem.
 **
 ** Per OIDC Back-Channel Logout spec (https://openid.net/specs/openid-connect-backchannel-1_0.html):
 ** - POST request with Content-Type: application/x-www-form-urlencoded
 ** - Contains 'logout_token' parameter (a JWT)
 ** - No authentication required (the JWT itself authenticates the request)
 ** - Returns HTTP 200 on success
 *******************************************************************************/
public class BackChannelLogoutSpecV1 extends AbstractEndpointSpec<BackChannelLogoutInput, BackChannelLogoutResponseV1, BackChannelLogoutExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/oidc/backchannel-logout")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.AUTHENTICATION)
         .withShortSummary("OIDC Back-Channel Logout")
         .withLongDescription("""
            Receives logout notifications from the Identity Provider (IdP) when a user
            logs out from the IdP or another application in the SSO ecosystem.

            The IdP sends a POST request with a 'logout_token' JWT parameter containing
            either a 'sub' (subject) or 'sid' (session ID) claim. QQQ finds and deletes
            all matching sessions.

            This endpoint should be registered with your IdP as the back-channel logout URI.""");
   }



   /***************************************************************************
    ** Back-channel logout is not secured by session - the JWT itself is the auth.
    ***************************************************************************/
   @Override
   public boolean isSecured()
   {
      return (false);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BackChannelLogoutInput buildInput(Context context) throws Exception
   {
      BackChannelLogoutInput input = new BackChannelLogoutInput();

      //////////////////////////////////////////////////////////////////////////
      // Per OIDC spec, logout_token comes as form parameter                   //
      // Content-Type: application/x-www-form-urlencoded                       //
      //////////////////////////////////////////////////////////////////////////
      input.setLogoutToken(context.formParam("logout_token"));

      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();

      examples.put("Successful logout", new Example().withValue(new BackChannelLogoutResponseV1()));

      return new BasicResponse("Back-channel logout processed successfully",
         BackChannelLogoutResponseV1.class.getSimpleName(),
         examples);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(
         BackChannelLogoutResponseV1.class.getSimpleName(), new BackChannelLogoutResponseV1().toSchema()
      );
   }

}
