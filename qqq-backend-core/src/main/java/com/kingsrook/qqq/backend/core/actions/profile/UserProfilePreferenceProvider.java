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


import java.io.InputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRefs;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceChange;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceProviderInterface;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRequestContext;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceValues;
import com.kingsrook.qqq.backend.core.model.metadata.profile.UserProfilesMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Preferences provider of the core "profile" section (photo, display name and,
 ** when the app allows it, Gravatar).  Always the signed-in user's own row.
 **
 ** The avatar field reads as the photo's URL and is written with a ref from
 ** POST /uploads (or null to remove the photo).  The hidden, read-only avatarUrl
 ** is what others see: the photo, else the chosen Gravatar, else null.  Writing a value equal to the
 ** current URL changes nothing, so a client may send the whole form back.
 *******************************************************************************/
public class UserProfilePreferenceProvider implements UserPreferenceProviderInterface
{
   public static final String FIELD_AVATAR       = "avatar";
   public static final String FIELD_DISPLAY_NAME = "displayName";
   public static final String FIELD_USE_GRAVATAR = "useGravatar";
   public static final String FIELD_AVATAR_URL   = "avatarUrl";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public UserPreferenceValues load(UserPreferenceRequestContext context) throws QException
   {
      QRecord                   row    = UserProfiles.find(context.getUserId());
      Map<String, Serializable> values = new HashMap<>();
      String                    key    = row == null ? null : row.getValueString("avatarKey");
      values.put(FIELD_AVATAR, StringUtils.hasContent(key) ? UserProfiles.avatarUrl(context.getUserId(), new UserProfiles.Avatar(key, null)) : null);
      values.put(FIELD_DISPLAY_NAME, row == null ? null : row.getValueString("displayName"));
      UserProfiles.Avatar effective = UserProfiles.avatarOf(row);
      values.put(FIELD_AVATAR_URL, effective == null ? null : UserProfiles.avatarUrl(context.getUserId(), effective));
      if(UserProfiles.settings().getGravatarOptIn())
      {
         values.put(FIELD_USE_GRAVATAR, row != null && Boolean.TRUE.equals(row.getValueBoolean("useGravatar")));
      }
      return (new UserPreferenceValues().withValues(values));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<String> validate(UserPreferenceRequestContext context, UserPreferenceChange change) throws QException
   {
      List<String> errors = new ArrayList<>();
      if(Boolean.TRUE.equals(change.getValues().get(FIELD_USE_GRAVATAR)) && context.getUserEmailAddress() == null)
      {
         errors.add("Use Gravatar: your account has no email address for Gravatar to look up.");
      }
      return (errors);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void store(UserPreferenceRequestContext context, UserPreferenceChange change) throws QException
   {
      String                    userId   = context.getUserId();
      QRecord                   existing = UserProfiles.find(userId);
      String                    oldKey   = existing == null ? null : existing.getValueString("avatarKey");
      Map<String, Serializable> values   = change.getValues();
      QRecord                   record   = new QRecord();

      if(values.containsKey(FIELD_DISPLAY_NAME))
      {
         String displayName = values.get(FIELD_DISPLAY_NAME) == null ? null : values.get(FIELD_DISPLAY_NAME).toString().strip();
         record.setValue("displayName", StringUtils.hasContent(displayName) ? displayName : null);
      }

      if(values.containsKey(FIELD_USE_GRAVATAR) && UserProfiles.settings().getGravatarOptIn())
      {
         boolean useGravatar = Boolean.TRUE.equals(values.get(FIELD_USE_GRAVATAR));
         record.setValue("useGravatar", useGravatar);
         record.setValue("gravatarHash", useGravatar ? UserProfiles.gravatarHash(context.getUserEmailAddress()) : null);
      }

      String newKey = oldKey;
      if(values.containsKey(FIELD_AVATAR))
      {
         Serializable avatar = values.get(FIELD_AVATAR);
         if(avatar == null)
         {
            newKey = null;
         }
         else if(!(oldKey != null && avatar.toString().equals(UserProfiles.avatarUrl(userId, new UserProfiles.Avatar(oldKey, null)))))
         {
            Map<Integer, byte[]> pngBySize = readAndConvert(userId, avatar.toString());
            newKey = UserProfiles.avatarKey(userId, pngBySize.get(UserProfilesMetaData.AVATAR_SIZES.get(UserProfilesMetaData.AVATAR_SIZES.size() - 1)));
            UserProfiles.writeAvatar(newKey, pngBySize);
         }
         if(!Objects.equals(newKey, oldKey))
         {
            record.setValue("avatarKey", newKey);
            record.setValue("avatarUpdatedAt", newKey == null ? null : Instant.now());
         }
      }

      if(record.getValues().isEmpty())
      {
         return;
      }

      String tableName = UserProfiles.settings().getTableName();
      if(existing == null)
      {
         record.setValue("userId", userId);
         new InsertAction().execute(new InsertInput(tableName).withRecord(record));
      }
      else
      {
         record.setValue("id", existing.getValue("id"));
         new UpdateAction().execute(new UpdateInput(tableName).withRecord(record));
      }

      ////////////////////////////////////////////////////////////////////////
      // only once the row names the new photo do the old objects go away  //
      ////////////////////////////////////////////////////////////////////////
      if(oldKey != null && !oldKey.equals(newKey))
      {
         UserProfiles.deleteAvatar(oldKey);
      }
   }



   /*******************************************************************************
    ** Resolve the caller's own upload ref (an administrator's access to other
    ** users' refs does not extend to their photos), and convert the image.
    *******************************************************************************/
   private Map<Integer, byte[]> readAndConvert(String userId, String ref) throws QException
   {
      QRecord upload;
      try
      {
         upload = UploadRefs.get(ref);
      }
      catch(QNotFoundException e)
      {
         throw (new QBadRequestException("Photo: " + e.getMessage() + " Upload the image again."));
      }
      if(!userId.equals(upload.getValueString("userId")))
      {
         throw (new QBadRequestException("Photo: Upload not found. Upload the image again."));
      }

      try(InputStream input = new StorageAction().getInputStream(UploadRefs.open(ref)))
      {
         byte[] bytes = AvatarImageProcessor.readBounded(input, UserProfiles.settings().getMaxAvatarBytes());
         return (AvatarImageProcessor.process(bytes));
      }
      catch(QBadRequestException e)
      {
         throw (new QBadRequestException("Photo: " + e.getMessage()));
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Error reading the uploaded photo", e));
      }
   }
}
