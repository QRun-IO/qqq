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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.executors.UserPreferencesExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.UserPreferencesMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.PreferencesResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.UserPreferencesSpecUtils;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;


/*******************************************************************************
 ** List visible sections and the signed-in user's preferences.
 *******************************************************************************/
public class PreferencesSpecV1 extends AbstractEndpointSpec<UserPreferencesMiddlewareInput, PreferencesResponseV1, UserPreferencesExecutor>
{
   /*******************************************************************************
    ** List visible sections and the signed-in user's preferences
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath("/preferences").withHttpMethod(HttpMethod.GET).withTag(TagsV1.GENERAL)
         .withShortSummary("List visible sections and the signed-in user's preferences")
         .withLongDescription("Preferences always belong to the authenticated session user. No user identifier is accepted. Unknown or hidden sections return 404; unauthenticated requests return 401. PATCH changes only supplied fields and fixed rows; invalid changes return 400 with all messages before anything is stored. Writes are audited as the user and client channel."));
   }



   /*******************************************************************************
    ** Path parameters; userId query parameters are never read.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of());
   }



   /*******************************************************************************
    ** Build the typed operation input.
    *******************************************************************************/
   @Override
   public UserPreferencesMiddlewareInput buildInput(Context context) throws Exception
   {
      return (new UserPreferencesMiddlewareInput().withOperation(UserPreferencesMiddlewareInput.Operation.LIST));
   }



   /*******************************************************************************
    ** Publish all preference component schemas.
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return (UserPreferencesSpecUtils.schemas());
   }



   /*******************************************************************************
    ** The typed success response.
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return (new BasicResponse("Current preferences for the session user", "PreferencesResponseV1"));
   }



   /*******************************************************************************
    ** Serialize null preferences explicitly so clients can distinguish cleared values.
    *******************************************************************************/
   @Override
   public void handleOutput(Context context, PreferencesResponseV1 output)
   {
      UserPreferencesSpecUtils.writeResponse(context, output);
   }
}
