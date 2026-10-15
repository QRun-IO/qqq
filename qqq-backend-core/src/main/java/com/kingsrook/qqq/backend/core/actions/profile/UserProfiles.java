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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Read side and security of the core user profile: a user's row, their avatar
 ** (addressed by an opaque avatarId, never by user id or email) and its storage
 ** objects, the record-lock keys for sessions, and the values a session sends to
 ** its frontend. Lookups of other users run as the system (avatars are visible
 ** to every signed-in user); the write side is UserProfilePreferenceProvider,
 ** for the signed-in user only.
 *******************************************************************************/
public final class UserProfiles
{
   private static final QLogger LOG = QLogger.getLogger(UserProfiles.class);

   public static final String AVATAR_PATH = "/qqq/v1/avatars/";

   private static final SecureRandom RANDOM = new SecureRandom();



   /*******************************************************************************
    ** What GET /avatars serves for an avatarId: the user's photo (avatarKey), or
    ** their chosen Gravatar (fetched and re-encoded by the server, so neither the
    ** email hash nor the viewer reaches Gravatar's URL from a browser).
    *******************************************************************************/
   public record Avatar(String avatarId, String avatarKey, String gravatarHash, Instant updatedAt)
   {
      /***************************************************************************
       ** The cache-busting version token used in avatar URLs (never derived from
       ** the email).
       ***************************************************************************/
      public String version()
      {
         return (avatarKey != null ? avatarKey : "g" + (updatedAt == null ? 0 : updatedAt.getEpochSecond()));
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
    ** Give a session its profile record-lock keys: user_profile_user = its user
    ** id, and the all-access key when it holds user_profile_admin. Hosts call this
    ** when they build sessions (as for durable jobs); the profile provider also
    ** ensures it before writing. No-op for null and system sessions.
    *******************************************************************************/
   public static void applyToSession(QSession session)
   {
      if(session == null || session instanceof QSystemUserSession)
      {
         return;
      }
      Map<String, List<Serializable>> keys = session.getSecurityKeyValues();
      if(keys != null)
      {
         keys.remove(UserProfilesMetaData.SECURITY_KEY_USER);
         keys.remove(UserProfilesMetaData.SECURITY_KEY_USER_ALL_ACCESS);
      }
      if(session.getUser() != null && StringUtils.hasContent(session.getUser().getIdReference()))
      {
         session.withSecurityKeyValue(UserProfilesMetaData.SECURITY_KEY_USER, session.getUser().getIdReference());
      }
      if(session.hasPermission(UserProfilesMetaData.PERMISSION_ADMIN))
      {
         session.withSecurityKeyValue(UserProfilesMetaData.SECURITY_KEY_USER_ALL_ACCESS, true);
      }
   }



   /*******************************************************************************
    ** Apply the keys only when the session has none yet.
    *******************************************************************************/
   public static void ensureSessionKeys(QSession session)
   {
      if(session != null && !(session instanceof QSystemUserSession) && session.getSecurityKeyValues(UserProfilesMetaData.SECURITY_KEY_USER).isEmpty())
      {
         applyToSession(session);
      }
   }



   /*******************************************************************************
    ** A user's profile row, or null (also null when profiles aren't installed).
    *******************************************************************************/
   public static QRecord find(String userId) throws QException
   {
      return (findOne("userId", userId));
   }



   /*******************************************************************************
    ** The profile row with an avatarId, or null.
    *******************************************************************************/
   public static QRecord findByAvatarId(String avatarId) throws QException
   {
      return (findOne("avatarId", avatarId));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord findOne(String fieldName, String value) throws QException
   {
      UserProfilesMetaData settings = settings();
      if(settings == null || !StringUtils.hasContent(value))
      {
         return (null);
      }
      List<QRecord> rows = SystemScope.call(() -> QueryAction.execute(settings.getTableName(), new QQueryFilter(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, value)).withLimit(1)));
      return (rows.isEmpty() ? null : rows.get(0));
   }



   /*******************************************************************************
    ** Avatar URLs of up to MAX_LOOKUP_USER_IDS users; users without one are left
    ** out (clients draw their initials).
    *******************************************************************************/
   public static Map<String, String> lookup(Collection<String> userIds) throws QException
   {
      Map<String, String> rs       = new LinkedHashMap<>();
      UserProfilesMetaData settings = settings();
      List<String>         ids      = userIds == null ? List.of() : userIds.stream().filter(StringUtils::hasContent).distinct().toList();
      if(settings == null || ids.isEmpty())
      {
         return (rs);
      }
      List<QRecord> rows = SystemScope.call(() -> QueryAction.execute(settings.getTableName(), new QQueryFilter(new QFilterCriteria("userId", QCriteriaOperator.IN, new ArrayList<>(ids)))));
      for(QRecord row : rows)
      {
         Avatar avatar = avatarOf(row);
         if(avatar != null)
         {
            rs.put(row.getValueString("userId"), avatarUrl(avatar));
         }
      }
      return (rs);
   }



   /*******************************************************************************
    ** The avatar a profile row shows, or null for none (initials).
    *******************************************************************************/
   public static Avatar avatarOf(QRecord row)
   {
      if(row == null || !StringUtils.hasContent(row.getValueString("avatarId")))
      {
         return (null);
      }
      String  avatarId  = row.getValueString("avatarId");
      Instant updatedAt = row.getValueInstant("avatarUpdatedAt");
      String  key       = row.getValueString("avatarKey");
      if(StringUtils.hasContent(key))
      {
         return (new Avatar(avatarId, key, null, updatedAt));
      }
      UserProfilesMetaData settings = settings();
      String               hash     = row.getValueString("gravatarHash");
      if(settings != null && settings.getGravatarOptIn() && Boolean.TRUE.equals(ValueUtils.getValueAsBoolean(row.getValue("useGravatar"))) && StringUtils.hasContent(hash))
      {
         return (new Avatar(avatarId, null, hash, updatedAt));
      }
      return (null);
   }



   /*******************************************************************************
    ** The relative URL of an avatar (add size=32|64|128|256).
    *******************************************************************************/
   public static String avatarUrl(Avatar avatar)
   {
      return (AVATAR_PATH + avatar.avatarId() + "?v=" + avatar.version());
   }



   /*******************************************************************************
    ** A new opaque avatar id: 128 random bits, hex.
    *******************************************************************************/
   static String newAvatarId()
   {
      byte[] bytes = new byte[16];
      RANDOM.nextBytes(bytes);
      return (HexFormat.of().formatHex(bytes));
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
    ** as Gravatar defines it). Kept server-side only: never in a URL or a response.
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
         QRecord row    = find(session.getUser().getIdReference());
         Avatar  avatar = avatarOf(row);
         if(avatar != null)
         {
            rs.put("avatarUrl", avatarUrl(avatar));
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
