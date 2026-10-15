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

package com.kingsrook.qqq.backend.core.model.metadata.profile;


import java.util.List;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferencesMetaData;


/*******************************************************************************
 ** Settings of the core user profile (photo/avatar and display name), installed
 ** by UserProfileMetaDataProvider.defineAll.  Absent when an application does not
 ** install profiles; then GET /avatars answers 404 and sessions carry no avatar.
 *******************************************************************************/
public class UserProfilesMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME               = "userProfiles";
   public static final String DEFAULT_TABLE_NAME = "user_profile";
   public static final String SECTION_NAME       = "profile";

   public static final List<Integer> AVATAR_SIZES        = List.of(32, 64, 128, 256);
   public static final int           DEFAULT_AVATAR_SIZE = 128;
   public static final long          DEFAULT_MAX_BYTES   = 5L * 1024 * 1024;
   public static final long          MAX_PIXELS          = 40_000_000L;

   private String  tableName              = DEFAULT_TABLE_NAME;
   private String  avatarStorageTableName;
   private boolean gravatarOptIn          = false;
   private long    maxAvatarBytes         = DEFAULT_MAX_BYTES;



   /*******************************************************************************
    ** Get the installed profile settings, or null when profiles are not installed.
    *******************************************************************************/
   public static UserProfilesMetaData of(QInstance qInstance)
   {
      return (qInstance == null ? null : QSupplementalInstanceMetaData.of(qInstance, NAME));
   }



   /*******************************************************************************
    ** Get or register profile settings.
    *******************************************************************************/
   public static UserProfilesMetaData ofOrWithNew(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, UserProfilesMetaData::new));
   }



   /*******************************************************************************
    ** Supplemental metadata registry name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** The profile table and the avatar storage table must exist, and the profile
    ** section must be registered.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      String path = "User profiles: ";
      validator.assertCondition(qInstance.getTable(tableName) != null, path + "table [" + tableName + "] must exist");
      validator.assertCondition(avatarStorageTableName != null && qInstance.getTable(avatarStorageTableName) != null, path + "avatarStorageTableName [" + avatarStorageTableName + "] must name a storage table");
      validator.assertCondition(maxAvatarBytes > 0, path + "maxAvatarBytes must be positive");
      UserPreferencesMetaData preferences = UserPreferencesMetaData.of(qInstance);
      validator.assertCondition(preferences != null && preferences.getSections().stream().anyMatch(s -> SECTION_NAME.equals(s.getName())), path + "the [" + SECTION_NAME + "] preference section must be registered");
   }



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public UserProfilesMetaData withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for avatarStorageTableName
    *******************************************************************************/
   public String getAvatarStorageTableName()
   {
      return (this.avatarStorageTableName);
   }



   /*******************************************************************************
    ** Setter for avatarStorageTableName
    *******************************************************************************/
   public void setAvatarStorageTableName(String avatarStorageTableName)
   {
      this.avatarStorageTableName = avatarStorageTableName;
   }



   /*******************************************************************************
    ** Fluent setter for avatarStorageTableName: a storage (filesystem or S3) table
    ** the avatar images are written to, at avatars/{avatarKey}/{size}.png.
    *******************************************************************************/
   public UserProfilesMetaData withAvatarStorageTableName(String avatarStorageTableName)
   {
      this.avatarStorageTableName = avatarStorageTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for gravatarOptIn
    *******************************************************************************/
   public boolean getGravatarOptIn()
   {
      return (this.gravatarOptIn);
   }



   /*******************************************************************************
    ** Setter for gravatarOptIn
    *******************************************************************************/
   public void setGravatarOptIn(boolean gravatarOptIn)
   {
      this.gravatarOptIn = gravatarOptIn;
   }



   /*******************************************************************************
    ** Fluent setter for gravatarOptIn: when true, each user may choose to show
    ** their Gravatar while they have no uploaded photo (default false: no
    ** third-party avatar is ever requested).
    *******************************************************************************/
   public UserProfilesMetaData withGravatarOptIn(boolean gravatarOptIn)
   {
      this.gravatarOptIn = gravatarOptIn;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxAvatarBytes
    *******************************************************************************/
   public long getMaxAvatarBytes()
   {
      return (this.maxAvatarBytes);
   }



   /*******************************************************************************
    ** Setter for maxAvatarBytes
    *******************************************************************************/
   public void setMaxAvatarBytes(long maxAvatarBytes)
   {
      this.maxAvatarBytes = maxAvatarBytes;
   }



   /*******************************************************************************
    ** Fluent setter for maxAvatarBytes (largest accepted upload; default 5 MB).
    *******************************************************************************/
   public UserProfilesMetaData withMaxAvatarBytes(long maxAvatarBytes)
   {
      this.maxAvatarBytes = maxAvatarBytes;
      return (this);
   }
}
