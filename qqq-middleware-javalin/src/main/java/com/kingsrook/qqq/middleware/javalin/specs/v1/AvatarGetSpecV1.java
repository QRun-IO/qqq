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


import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.profile.GravatarImages;
import com.kingsrook.qqq.backend.core.actions.profile.UserProfiles;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.profile.UserProfilesMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.executors.AbstractMiddlewareExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.ExecutorSessionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareOutputInterface;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** GET /avatars/{avatarId}: an avatar image by its opaque id (never a user id or
 ** email), for any signed-in user (avatars appear beside comments, presence and
 ** mentions; POST /avatars/lookup maps user ids to avatar URLs).  404 when there
 ** is none: clients then draw initials.  A chosen Gravatar is fetched and
 ** re-encoded by the server.  Since UI API 1.7.0.
 *******************************************************************************/
public class AvatarGetSpecV1 extends AbstractEndpointSpec<AbstractMiddlewareInput, AbstractMiddlewareOutputInterface, AbstractMiddlewareExecutor<AbstractMiddlewareInput, AbstractMiddlewareOutputInterface>>
{
   private static final QLogger LOG = QLogger.getLogger(AvatarGetSpecV1.class);

   private static final String IMMUTABLE = "private, max-age=31536000, immutable";
   private static final String SHORT     = "private, max-age=300";



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation()
         .withPath("/avatars/{avatarId}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.AUTHENTICATION)
         .withShortSummary("Get an avatar")
         .withLongDescription("""
            An avatar as a square PNG, by its opaque id, for any signed-in user. 404 when there is none (draw initials).
            Avatar URLs come from POST /avatars/lookup (other users), `values.user.avatarUrl` of POST /manageSession and
            the `profile` preferences section (the signed-in user). A user's chosen Gravatar is fetched and re-encoded by
            the server, so browsers never contact Gravatar. With `v` equal to the current version the response is
            cacheable for a year; otherwise for five minutes, with an ETag. Since 1.7.0."""));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(
         new Parameter()
            .withName("avatarId")
            .withDescription("The avatar's opaque id (from an avatar URL).")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withIn(In.PATH),
         new Parameter()
            .withName("size")
            .withDescription("Edge in pixels: 32, 64, 128 or 256 (default 128).")
            .withRequired(false)
            .withSchema(new Schema().withType(Type.INTEGER))
            .withIn(In.QUERY),
         new Parameter()
            .withName("v")
            .withDescription("The avatar's version, from an avatarUrl; makes the response cacheable for a year.")
            .withRequired(false)
            .withSchema(new Schema().withType(Type.STRING))
            .withIn(In.QUERY)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<Integer, Response> defineResponses()
   {
      Map<Integer, Response> rs = new LinkedHashMap<>();
      rs.put(200, new Response()
         .withDescription("The avatar (PNG).")
         .withContent(Map.of("image/png", new Content().withSchema(new Schema().withType(Type.STRING).withFormat("binary")))));
      rs.put(304, new Response().withDescription("Not modified (If-None-Match matched the ETag)."));
      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return (Map.of());
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
    ** Serve the image (no executor).
    ***************************************************************************/
   @Override
   public AbstractMiddlewareOutputInterface serveRequest(AbstractMiddlewareVersion abstractMiddlewareVersion, Context context) throws Exception
   {
      try
      {
         ExecutorSessionUtils.setupSession(context, qInstance);
         checkCredentialScopeOperation();
         abstractMiddlewareVersion.preExecute(context);
         serve(context);
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
    **
    ***************************************************************************/
   private void serve(Context context) throws Exception
   {
      int size = parseSize(context.queryParam("size"));
      if(UserProfilesMetaData.of(qInstance) == null)
      {
         throw (new QNotFoundException("No avatar."));
      }

      String              avatarId = context.pathParam("avatarId");
      UserProfiles.Avatar avatar   = UserProfiles.avatarOf(UserProfiles.findByAvatarId(avatarId));
      if(avatar == null)
      {
         throw (new QNotFoundException("No avatar."));
      }

      String version = avatar.version();
      context.header("X-Content-Type-Options", "nosniff");
      context.header("Content-Security-Policy", "default-src 'none'; sandbox");
      context.header("Content-Disposition", "inline");
      context.header("Cache-Control", version.equals(context.queryParam("v")) ? IMMUTABLE : SHORT);

      String etag = "\"" + version + "-" + size + "\"";
      if(etag.equals(context.header("If-None-Match")))
      {
         context.header("ETag", etag);
         context.status(HttpStatus.NOT_MODIFIED);
         return;
      }

      byte[] bytes;
      try
      {
         bytes = avatar.avatarKey() == null ? GravatarImages.png(avatar.gravatarHash(), size) : read(avatar.avatarKey(), size);
         if(bytes == null)
         {
            throw (new QNotFoundException("No avatar."));
         }
      }
      catch(Exception e)
      {
         ////////////////////////////////////////////////////////////////////
         // a row naming an object that is gone (storage restored or       //
         // expired) shows initials, not an error page in every comment    //
         ////////////////////////////////////////////////////////////////////
         if(avatar.avatarKey() != null)
         {
            LOG.warn("Avatar object missing; serving none", e, logPair("avatarKey", avatar.avatarKey()), logPair("size", size));
         }
         context.header("Cache-Control", SHORT);
         throw (new QNotFoundException("No avatar."));
      }
      context.header("ETag", etag);
      context.contentType("image/png");
      context.result(bytes);
   }



   /***************************************************************************
    ** One stored size of a photo.
    ***************************************************************************/
   private static byte[] read(String avatarKey, int size) throws Exception
   {
      try(InputStream input = new StorageAction().getInputStream(UserProfiles.avatarObject(avatarKey, size)))
      {
         return (input.readAllBytes());
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static int parseSize(String size) throws QBadRequestException
   {
      if(!StringUtils.hasContent(size))
      {
         return (UserProfilesMetaData.DEFAULT_AVATAR_SIZE);
      }
      try
      {
         int value = Integer.parseInt(size);
         if(UserProfilesMetaData.AVATAR_SIZES.contains(value))
         {
            return (value);
         }
      }
      catch(NumberFormatException e)
      {
         //////////////////////////////
         // falls through to the 400 //
         //////////////////////////////
      }
      throw (new QBadRequestException("size must be one of " + UserProfilesMetaData.AVATAR_SIZES + "."));
   }
}
