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
 ** GET /avatars/{userId}: a user's avatar image, for any signed-in user (avatars
 ** appear beside comments, presence and mentions).  404 when the user has none:
 ** clients then draw initials.  A user who chose Gravatar (and whose app allows
 ** it) is a redirect there.  Since UI API 1.7.0.
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
         .withPath("/avatars/{userId}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.AUTHENTICATION)
         .withShortSummary("Get a user's avatar")
         .withLongDescription("""
            A user's avatar as a square PNG, for any signed-in user. 404 when the user has no avatar (draw their
            initials instead). A user who turned on Gravatar (when the application allows it) is a 302 to Gravatar.
            The signed-in user's own URL, with its `v` version, is `values.user.avatarUrl` of POST /manageSession, and
            the photo is set in the `profile` preferences section. With `v` equal to the current version the response
            is cacheable for a year; otherwise for five minutes, with an ETag. Since 1.7.0."""));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(
         new Parameter()
            .withName("userId")
            .withDescription("The user's id (as in comments, presence and mentions).")
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
      rs.put(302, new Response().withDescription("The user chose Gravatar: its image URL is in the Location header."));
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

      String              userId = context.pathParam("userId");
      UserProfiles.Avatar avatar = UserProfiles.avatarOf(UserProfiles.find(userId));
      if(avatar == null)
      {
         throw (new QNotFoundException("No avatar."));
      }

      String version = avatar.version();
      context.header("X-Content-Type-Options", "nosniff");
      context.header("Cache-Control", version.equals(context.queryParam("v")) ? IMMUTABLE : SHORT);

      if(avatar.avatarKey() == null)
      {
         context.redirect("https://www.gravatar.com/avatar/" + avatar.gravatarHash() + "?s=" + size + "&d=404", HttpStatus.FOUND);
         return;
      }

      String etag = "\"" + version + "-" + size + "\"";
      if(etag.equals(context.header("If-None-Match")))
      {
         context.header("ETag", etag);
         context.status(HttpStatus.NOT_MODIFIED);
         return;
      }

      byte[] bytes;
      try(InputStream input = new StorageAction().getInputStream(UserProfiles.avatarObject(avatar.avatarKey(), size)))
      {
         bytes = input.readAllBytes();
      }
      catch(Exception e)
      {
         ////////////////////////////////////////////////////////////////////
         // a row naming an object that is gone (storage restored or       //
         // expired) shows initials, not an error page in every comment    //
         ////////////////////////////////////////////////////////////////////
         LOG.warn("Avatar object missing; serving none", e, logPair("avatarKey", avatar.avatarKey()), logPair("size", size));
         context.header("Cache-Control", SHORT);
         throw (new QNotFoundException("No avatar."));
      }
      context.header("ETag", etag);
      context.contentType("image/png");
      context.result(bytes);
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
