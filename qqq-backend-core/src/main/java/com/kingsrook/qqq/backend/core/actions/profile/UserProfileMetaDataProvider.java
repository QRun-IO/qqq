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


import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceSectionMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferencesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.profile.UserProfilesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** Installs the core user profile: the user_profile table, the "profile"
 ** preferences section (photo, display name, optional Gravatar) and the
 ** UserProfilesMetaData settings.  Applications supply the table's migration and
 ** a storage (S3/filesystem) table for the avatar images; see
 ** docs/USER-PROFILE-AVATARS.md.
 *******************************************************************************/
public class UserProfileMetaDataProvider
{
   private boolean                  gravatarOptIn  = false;
   private long                     maxAvatarBytes = UserProfilesMetaData.DEFAULT_MAX_BYTES;
   private Consumer<QTableMetaData> tableCustomizer;



   /*******************************************************************************
    ** Define everything, with the default table name (user_profile).
    *******************************************************************************/
   public void defineAll(QInstance qInstance, String backendName, String avatarStorageTableName)
   {
      defineAll(qInstance, backendName, avatarStorageTableName, null);
   }



   /*******************************************************************************
    ** Define everything, with a table name of the application's choosing.
    *******************************************************************************/
   public void defineAll(QInstance qInstance, String backendName, String avatarStorageTableName, String tableNameOrNull)
   {
      String tableName = tableNameOrNull == null ? UserProfilesMetaData.DEFAULT_TABLE_NAME : tableNameOrNull;
      UserProfilesMetaData.ofOrWithNew(qInstance)
         .withTableName(tableName)
         .withAvatarStorageTableName(avatarStorageTableName)
         .withGravatarOptIn(gravatarOptIn)
         .withMaxAvatarBytes(maxAvatarBytes);

      QTableMetaData table = defineTable(tableName, backendName);
      if(tableCustomizer != null)
      {
         tableCustomizer.accept(table);
      }
      qInstance.addTable(table);
      UserPreferencesMetaData.ofOrWithNew(qInstance).withSection(defineSection());
   }



   /*******************************************************************************
    ** The audited profile table (one row per user; no foreign keys).
    *******************************************************************************/
   public QTableMetaData defineTable(String tableName, String backendName)
   {
      return (new QTableMetaData()
         .withName(tableName).withLabel("User Profiles").withBackendName(backendName)
         .withIcon(new QIcon("account_circle"))
         .withPrimaryKeyField("id")
         .withRecordLabelFormatAndFields("%s", "userId")
         .withUniqueKey(new UniqueKey("userId"))
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("userId", QFieldType.STRING).withLabel("User ID").withIsRequired(true).withMaxLength(255))
         .withField(new QFieldMetaData("displayName", QFieldType.STRING).withMaxLength(100))
         .withField(new QFieldMetaData("avatarKey", QFieldType.STRING).withLabel("Avatar Key").withMaxLength(64).withIsEditable(false))
         .withField(new QFieldMetaData("avatarUpdatedAt", QFieldType.DATE_TIME).withLabel("Avatar Updated At").withIsEditable(false))
         .withField(new QFieldMetaData("useGravatar", QFieldType.BOOLEAN))
         .withField(new QFieldMetaData("gravatarHash", QFieldType.STRING).withMaxLength(64).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withIsEditable(false)));
   }



   /*******************************************************************************
    ** The "profile" preferences section.
    *******************************************************************************/
   public UserPreferenceSectionMetaData defineSection()
   {
      UserPreferenceSectionMetaData section = new UserPreferenceSectionMetaData()
         .withName(UserProfilesMetaData.SECTION_NAME)
         .withLabel("Profile")
         .withIcon(new QIcon("account_circle"))
         .withDescription("Your photo and the name other people see.")
         .withOrder(1)
         .withProvider(new QCodeReference(UserProfilePreferenceProvider.class))
         .withField(new QFieldMetaData(UserProfilePreferenceProvider.FIELD_AVATAR, QFieldType.STRING).withLabel("Photo")
            .withFieldAdornment(new FieldAdornment(AdornmentType.FILE_UPLOAD)
               .withValue(AdornmentType.FileUploadAdornment.formatDragAndDrop())
               .withValue(AdornmentType.FileUploadAdornment.VALUE_IS_UPLOAD_REF, true)
               .withValue(AdornmentType.FileUploadAdornment.ACCEPT, "image/png,image/jpeg,image/webp,image/gif")
               .withValue(AdornmentType.FileUploadAdornment.MAX_BYTES, maxAvatarBytes)
               .withValue(AdornmentType.FileUploadAdornment.IMAGE_SHAPE, "circle")))
         .withField(new QFieldMetaData(UserProfilePreferenceProvider.FIELD_DISPLAY_NAME, QFieldType.STRING).withLabel("Display name").withMaxLength(100))
         .withField(new QFieldMetaData(UserProfilePreferenceProvider.FIELD_AVATAR_URL, QFieldType.STRING).withLabel("Avatar URL").withIsHidden(true).withIsEditable(false));
      if(gravatarOptIn)
      {
         section.withField(new QFieldMetaData(UserProfilePreferenceProvider.FIELD_USE_GRAVATAR, QFieldType.BOOLEAN).withLabel("Use my Gravatar when I have no photo").withDefaultValue(false));
      }
      return (section);
   }



   /*******************************************************************************
    ** Fluent setter for gravatarOptIn (default false).
    *******************************************************************************/
   public UserProfileMetaDataProvider withGravatarOptIn(boolean gravatarOptIn)
   {
      this.gravatarOptIn = gravatarOptIn;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for maxAvatarBytes (default 5 MB).
    *******************************************************************************/
   public UserProfileMetaDataProvider withMaxAvatarBytes(long maxAvatarBytes)
   {
      this.maxAvatarBytes = maxAvatarBytes;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for a customizer of the table (e.g. snake_case backend names).
    *******************************************************************************/
   public UserProfileMetaDataProvider withTableCustomizer(Consumer<QTableMetaData> tableCustomizer)
   {
      this.tableCustomizer = tableCustomizer;
      return (this);
   }
}
