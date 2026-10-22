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
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.PreferenceActionResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.UserPreferencesSpecUtils;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;


/*******************************************************************************
 ** Run an action on the signed-in user's preferences.
 *******************************************************************************/
public class PreferenceSectionActionSpecV1 extends AbstractEndpointSpec<UserPreferencesMiddlewareInput, PreferenceActionResponseV1, UserPreferencesExecutor>
{
   /*******************************************************************************
    ** Never cache this operation, including authentication and validation failures.
    *******************************************************************************/
   @Override
   public PreferenceActionResponseV1 serveRequest(AbstractMiddlewareVersion version, Context context) throws Exception
   {
      context.header("Cache-Control", "no-store");
      return (super.serveRequest(version, context));
   }



   /*******************************************************************************
    ** Run an action on the signed-in user's preferences
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath("/preferences/{sectionName}/actions/{actionName}").withHttpMethod(HttpMethod.POST).withTag(TagsV1.GENERAL)
         .withShortSummary("Run an action on the signed-in user's preferences")
         .withLongDescription("Runs a registered action for the authenticated session user. ROW actions target only rows loaded for that user and allowed by actionNames. Unknown actions or hidden sections return 404; invalid inputs return 400; unauthenticated requests return 401. Clients must confirm confirmationText before running. Secrets are shown once and must not be stored, logged, cached, or sent to analytics by clients. Responses use Cache-Control: no-store."));
   }



   /*******************************************************************************
    ** Path parameters; userId query parameters are never read.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (UserPreferencesSpecUtils.actionParameters());
   }



   /*******************************************************************************
    ** Build the typed operation input.
    *******************************************************************************/
   @Override
   public UserPreferencesMiddlewareInput buildInput(Context context) throws Exception
   {
      return (new UserPreferencesMiddlewareInput().withOperation(UserPreferencesMiddlewareInput.Operation.ACTION).withSectionName(context.pathParam("sectionName"))
         .withActionName(context.pathParam("actionName")).withActionRequest(UserPreferencesSpecUtils.readActionRequest(context)));
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
      return (new BasicResponse("Current preferences for the session user", "PreferenceActionResponseV1"));
   }



   /*******************************************************************************
    ** Serialize null preferences explicitly so clients can distinguish cleared values.
    *******************************************************************************/
   @Override
   public void handleOutput(Context context, PreferenceActionResponseV1 output)
   {
      UserPreferencesSpecUtils.writeResponse(context, output);
   }



   /*******************************************************************************
    ** Action request contract.
    *******************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return (UserPreferencesSpecUtils.actionRequestBody());
   }
}
