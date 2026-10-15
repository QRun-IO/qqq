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

package com.kingsrook.qqq.backend.core.actions.profile;


import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.profile.UserProfilesMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Read side of the core user profile: a user's row, their avatar URL and
 ** objects, and the values a session sends to its frontend.  Lookups of other
 ** users run as the system (avatars are visible to every signed-in user); the
 ** write side is UserProfilePreferenceProvider, for the signed-in user only.
 *******************************************************************************/
public final class UserProfiles
{
   private static final QLogger LOG = QLogger.getLogger(UserProfiles.class);

   public static final String AVATAR_PATH = "/qqq/v1/avatars/";



   /*******************************************************************************
    ** What GET /avatars serves for a user: their photo (key), a Gravatar (hash),
    ** or nothing.
    *******************************************************************************/
   public record Avatar(String avatarKey, String gravatarHash)
   {
      /***************************************************************************
       ** The cache-busting version token used in avatar URLs.
       ***************************************************************************/
      public String version()
      {
         return (avatarKey != null ? avatarKey : "g" + gravatarHash);
      }
   }



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private UserProfiles()
   {
   }



   /*******************************************************************************
    ** The installed profile settings, or null.
    *******************************************************************************/
   public static UserProfilesMetaData settings()
   {
      return (UserProfilesMetaData.of(QContext.getQInstance()));
   }



   /*******************************************************************************
    ** A user's profile row, or null (also null when profiles aren't installed).
    *******************************************************************************/
   public static QRecord find(String userId) throws QException
   {
      UserProfilesMetaData settings = settings();
      if(settings == null || !StringUtils.hasContent(userId))
      {
         return (null);
      }
      List<QRecord> rows = SystemScope.call(() -> QueryAction.execute(settings.getTableName(), new QQueryFilter(new QFilterCriteria("userId", QCriteriaOperator.EQUALS, userId)).withLimit(1)));
      return (rows.isEmpty() ? null : rows.get(0));
   }



   /*******************************************************************************
    ** The avatar a profile row shows, or null for none (initials).
    *******************************************************************************/
   public static Avatar avatarOf(QRecord row)
   {
      if(row == null)
      {
         return (null);
      }
      String key = row.getValueString("avatarKey");
      if(StringUtils.hasContent(key))
      {
         return (new Avatar(key, null));
      }
      UserProfilesMetaData settings = settings();
      String               hash     = row.getValueString("gravatarHash");
      if(settings != null && settings.getGravatarOptIn() && Boolean.TRUE.equals(ValueUtils.getValueAsBoolean(row.getValue("useGravatar"))) && StringUtils.hasContent(hash))
      {
         return (new Avatar(null, hash));
      }
      return (null);
   }



   /*******************************************************************************
    ** The relative URL of a user's avatar (add size=32|64|128|256).
    *******************************************************************************/
   public static String avatarUrl(String userId, Avatar avatar)
   {
      return (AVATAR_PATH + URLEncoder.encode(userId, StandardCharsets.UTF_8).replace("+", "%20") + "?v=" + avatar.version());
   }



   /*******************************************************************************
    ** The storage object of one size of an avatar.
    *******************************************************************************/
   public static StorageInput avatarObject(String avatarKey, int size)
   {
      return (new StorageInput(settings().getAvatarStorageTableName()).withReference("avatars/" + avatarKey + "/" + size + ".png").withContentType("image/png"));
   }



   /*******************************************************************************
    ** Write every size of an avatar.
    *******************************************************************************/
   static void writeAvatar(String avatarKey, Map<Integer, byte[]> pngBySize) throws QException
   {
      for(Map.Entry<Integer, byte[]> entry : pngBySize.entrySet())
      {
         try(OutputStream out = new StorageAction().createOutputStream(avatarObject(avatarKey, entry.getKey())))
         {
            out.write(entry.getValue());
         }
         catch(QException e)
         {
            throw (e);
         }
         catch(Exception e)
         {
            throw (new QException("Error storing avatar", e));
         }
      }
   }



   /*******************************************************************************
    ** Delete every size of an avatar (best effort: a leftover object is
    ** unreachable once its row no longer names it).
    *******************************************************************************/
   static void deleteAvatar(String avatarKey)
   {
      if(!StringUtils.hasContent(avatarKey))
      {
         return;
      }
      for(Integer size : UserProfilesMetaData.AVATAR_SIZES)
      {
         try
         {
            new StorageAction().deleteObject(avatarObject(avatarKey, size));
         }
         catch(Exception e)
         {
            LOG.info("Could not delete a replaced avatar object", e, logPair("avatarKey", avatarKey), logPair("size", size));
         }
      }
   }



   /*******************************************************************************
    ** Content key of an avatar: per user and content, so it changes whenever the
    ** photo does (immutable URLs) and never collides across users.
    *******************************************************************************/
   static String avatarKey(String userId, byte[] largestPng)
   {
      MessageDigest digest = sha256();
      digest.update(userId.getBytes(StandardCharsets.UTF_8));
      digest.update((byte) 0);
      digest.update(largestPng);
      return (HexFormat.of().formatHex(digest.digest()).substring(0, 32));
   }



   /*******************************************************************************
    ** Gravatar's hash of an email address (MD5 of the trimmed, lower-cased email,
    ** as Gravatar defines it).
    *******************************************************************************/
   static String gravatarHash(String email)
   {
      try
      {
         MessageDigest md5 = MessageDigest.getInstance("MD5");
         return (HexFormat.of().formatHex(md5.digest(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))));
      }
      catch(Exception e)
      {
         throw (new IllegalStateException("MD5 unavailable", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static MessageDigest sha256()
   {
      try
      {
         return (MessageDigest.getInstance("SHA-256"));
      }
      catch(Exception e)
      {
         throw (new IllegalStateException("SHA-256 unavailable", e));
      }
   }



   /*******************************************************************************
    ** The profile values a session sends its frontend under values.user:
    ** avatarUrl (when the user has a photo or Gravatar) and displayName (when
    ** set).  Empty when profiles aren't installed.  Never throws: a profile
    ** lookup must not break signing in.
    *******************************************************************************/
   public static Map<String, Serializable> frontendUserValues(QInstance qInstance, QSession session)
   {
      Map<String, Serializable> rs = new LinkedHashMap<>();
      if(UserProfilesMetaData.of(qInstance) == null || session == null || session.getUser() == null || session.getUser().getIdReference() == null)
      {
         return (rs);
      }
      try
      {
         String  userId = session.getUser().getIdReference();
         QRecord row    = find(userId);
         Avatar  avatar = avatarOf(row);
         if(avatar != null)
         {
            rs.put("avatarUrl", avatarUrl(userId, avatar));
         }
         if(row != null && StringUtils.hasContent(row.getValueString("displayName")))
         {
            rs.put("displayName", row.getValueString("displayName"));
         }
      }
      catch(Exception e)
      {
         LOG.warn("Could not read the user's profile for the session", e);
      }
      return (rs);
   }
}
