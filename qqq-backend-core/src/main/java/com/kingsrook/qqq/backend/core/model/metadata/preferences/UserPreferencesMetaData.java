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

package com.kingsrook.qqq.backend.core.model.metadata.preferences;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Registry and startup validation of user preference sections.
 *******************************************************************************/
public class UserPreferencesMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "userPreferences";
   public static final String DEFAULT_TABLE_NAME = "user_preference";

   private List<UserPreferenceSectionMetaData> sections = new ArrayList<>();
   private String keyValueTableName = DEFAULT_TABLE_NAME;



   /*******************************************************************************
    ** Get registered preferences, or null when none are installed.
    *******************************************************************************/
   public static UserPreferencesMetaData of(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.of(qInstance, NAME));
   }



   /*******************************************************************************
    ** Get or register preferences metadata.
    *******************************************************************************/
   public static UserPreferencesMetaData ofOrWithNew(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, UserPreferencesMetaData::new));
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
    ** Add a section.
    *******************************************************************************/
   public UserPreferencesMetaData withSection(UserPreferenceSectionMetaData section)
   {
      sections.add(section);
      return (this);
   }



   /*******************************************************************************
    ** Get sections.
    *******************************************************************************/
   public List<UserPreferenceSectionMetaData> getSections()
   {
      return (sections);
   }



   /*******************************************************************************
    ** Set sections.
    *******************************************************************************/
   public void setSections(List<UserPreferenceSectionMetaData> sections)
   {
      this.sections = sections;
   }



   /*******************************************************************************
    ** With sections.
    *******************************************************************************/
   public UserPreferencesMetaData withSections(List<UserPreferenceSectionMetaData> sections)
   {
      this.sections = sections;
      return (this);
   }



   /*******************************************************************************
    ** Get keyValueTableName.
    *******************************************************************************/
   public String getKeyValueTableName()
   {
      return (keyValueTableName);
   }



   /*******************************************************************************
    ** Set keyValueTableName.
    *******************************************************************************/
   public void setKeyValueTableName(String keyValueTableName)
   {
      this.keyValueTableName = keyValueTableName;
   }



   /*******************************************************************************
    ** With keyValueTableName.
    *******************************************************************************/
   public UserPreferencesMetaData withKeyValueTableName(String keyValueTableName)
   {
      this.keyValueTableName = keyValueTableName;
      return (this);
   }



   /*******************************************************************************
    ** Validate through QInstanceValidator's supplemental metadata hook.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      Set<String> names = new HashSet<>();
      for(UserPreferenceSectionMetaData section : sections)
      {
         String path = "User preference section [" + section.getName() + "]: ";
         validateNameAndLabel(section.getName(), section.getLabel(), names, path, validator);
         validateFields(section.getFields(), path, qInstance, validator);
         validator.assertCondition(section.getAppName() == null || qInstance.getApp(section.getAppName()) != null, path + "appName must exist");
         Set<String> listNames = new HashSet<>();
         for(UserPreferenceListMetaData list : section.getLists())
         {
            String listPath = path + "list [" + list.getName() + "]: ";
            validateNameAndLabel(list.getName(), list.getLabel(), listNames, listPath, validator);
            validateFields(list.getFields(), listPath, qInstance, validator);
         }
         if(section.getProvider() == null)
         {
            validator.assertCondition(section.getLists().isEmpty(), path + "the key-value provider does not support lists");
            validator.assertCondition(qInstance.getTable(keyValueTableName) != null, path + "key-value table [" + keyValueTableName + "] must exist");
         }
         else
         {
            try
            {
               Object provider = QCodeLoader.getAdHoc(Object.class, section.getProvider());
               validator.assertCondition(provider instanceof UserPreferenceProviderInterface, path + "provider must load and implement UserPreferenceProviderInterface");
            }
            catch(Exception | LinkageError e)
            {
               validator.assertCondition(false, path + "provider must load and implement UserPreferenceProviderInterface");
            }
         }
      }
   }



   /*******************************************************************************
    ** Names are retained in lists so duplicate registration fails validation.
    *******************************************************************************/
   private void validateNameAndLabel(String name, String label, Set<String> names, String path, QInstanceValidator validator)
   {
      validator.assertCondition(name != null && name.matches("[a-z][a-zA-Z0-9_-]*"), path + "name must match [a-z][a-zA-Z0-9_-]*");
      validator.assertCondition(names.add(name), path + "name must be unique");
      validator.assertCondition(StringUtils.hasContent(label), path + "label is required");
   }



   /*******************************************************************************
    ** Validate scalar and row fields independently.
    *******************************************************************************/
   private void validateFields(List<QFieldMetaData> fields, String path, QInstance qInstance, QInstanceValidator validator)
   {
      Set<String> names = new HashSet<>();
      Set<QFieldType> types = Set.of(QFieldType.BOOLEAN, QFieldType.INTEGER, QFieldType.DECIMAL, QFieldType.STRING, QFieldType.TEXT, QFieldType.DATE, QFieldType.TIME, QFieldType.DATE_TIME);
      for(QFieldMetaData field : fields)
      {
         String fieldPath = path + "field [" + field.getName() + "]: ";
         validateNameAndLabel(field.getName(), field.getLabel(), names, fieldPath, validator);
         validator.assertCondition(field.getType() != null && types.contains(field.getType()), fieldPath + "unsupported field type");
         validator.assertCondition(!"userId".equals(field.getName()), fieldPath + "userId is reserved for session identity");
         validator.assertCondition(field.getPossibleValueSourceName() == null || qInstance.getPossibleValueSource(field.getPossibleValueSourceName()) != null, fieldPath + "possible value source must exist");
      }
   }
}
