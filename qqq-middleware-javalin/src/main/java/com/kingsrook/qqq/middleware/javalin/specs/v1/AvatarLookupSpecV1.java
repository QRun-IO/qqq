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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.profile.UserProfiles;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.metadata.profile.UserProfilesMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.AbstractMiddlewareExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.ExecutorSessionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareOutputInterface;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 ** POST /avatars/lookup: the avatar URLs of up to 200 users, by user id, for
 ** any signed-in user. A POST, so user ids (often emails) are never in a URL or
 ** an access log. Users without an avatar are left out. Since UI API 1.8.0.
 *******************************************************************************/
public class AvatarLookupSpecV1 extends AbstractEndpointSpec<AbstractMiddlewareInput, AbstractMiddlewareOutputInterface, AbstractMiddlewareExecutor<AbstractMiddlewareInput, AbstractMiddlewareOutputInterface>>
{
   private static final String REQUEST  = "AvatarLookupRequestV1";
   private static final String RESPONSE = "AvatarLookupResponseV1";



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation()
         .withPath("/avatars/lookup")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.AUTHENTICATION)
         .withShortSummary("Look up users' avatars")
         .withLongDescription("""
            The avatar URL (GET /avatars/{avatarId}, with its version) of each user id given, for any signed-in user.
            Users without an avatar are not in the result: draw their initials. At most 200 ids per call. Since 1.8.0."""));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return (new RequestBody().withRequired(true).withContent(Map.of(ContentType.APPLICATION_JSON.getMimeType(),
         new Content().withSchema(new Schema().withRefToSchema(REQUEST)))));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      Map<String, Schema> rs = new LinkedHashMap<>();
      rs.put(REQUEST, new Schema().withType(Type.OBJECT).withAdditionalProperties(false)
         .withDescription("The users to look up.")
         .withProperty("userIds", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING))
            .withDescription("User ids (as in comments, presence and mentions); at most 200.")));
      rs.put(RESPONSE, new Schema().withType(Type.OBJECT)
         .withDescription("Avatar URLs by user id.")
         .withProperty("avatars", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema().withType(Type.STRING))
            .withDescription("User id to avatar URL (relative; add size=32|64|128|256). Users without an avatar are absent.")));
      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return (new BasicResponse("The avatar URLs of the users that have one.", RESPONSE));
   }



   /***************************************************************************
    ** No input: serveRequest is overridden.
    ***************************************************************************/
   @Override
   public AbstractMiddlewareInput buildInput(Context context)
   {
      return (null);
   }



   /***************************************************************************
    ** Answer directly (no executor).
    ***************************************************************************/
   @Override
   public AbstractMiddlewareOutputInterface serveRequest(AbstractMiddlewareVersion abstractMiddlewareVersion, Context context) throws Exception
   {
      try
      {
         ExecutorSessionUtils.setupSession(context, qInstance);
         checkCredentialScopeOperation(context);
         abstractMiddlewareVersion.preExecute(context);

         List<String>        userIds = readUserIds(context);
         Map<String, String> avatars = UserProfilesMetaData.of(qInstance) == null ? Map.of() : UserProfiles.lookup(userIds);
         context.header("Cache-Control", "private, no-cache");
         context.contentType(ContentType.APPLICATION_JSON);
         context.result(JsonUtils.toJson(Map.of("avatars", avatars)));
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
      finally
      {
         QContext.clear();
      }
      return (null);
   }



   /***************************************************************************
    ** The body's userIds (strings only, at most MAX_LOOKUP_USER_IDS).
    ***************************************************************************/
   private static List<String> readUserIds(Context context) throws QBadRequestException
   {
      JSONObject body;
      try
      {
         body = new JSONObject(context.body());
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("Send a JSON object with userIds."));
      }
      for(String key : body.keySet())
      {
         if(!"userIds".equals(key))
         {
            throw (new QBadRequestException("Unexpected property: " + key));
         }
      }
      JSONArray array = body.optJSONArray("userIds");
      if(array == null)
      {
         throw (new QBadRequestException("userIds must be an array of user ids."));
      }
      if(array.length() > UserProfilesMetaData.MAX_LOOKUP_USER_IDS)
      {
         throw (new QBadRequestException("At most " + UserProfilesMetaData.MAX_LOOKUP_USER_IDS + " userIds per call."));
      }
      List<String> rs = new ArrayList<>();
      for(int i = 0; i < array.length(); i++)
      {
         if(!(array.get(i) instanceof String userId))
         {
            throw (new QBadRequestException("userIds must be an array of user ids."));
         }
         rs.add(userId);
      }
      return (rs);
   }
}
