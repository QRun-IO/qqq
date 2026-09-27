/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.variants;


import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Configs for how a backend that uses variants works.  Specifically:
 **
 ** - the variant "type key" - e.g., key for variants map in session.
 ** - what table supplies the variant options (optionsTableName
 ** - an optional filter to apply to that options table
 ** - a map of the settings that a backend gets from its variant table to the
 ** field names in that table that they come from.  e.g., a backend may have a
 ** username attribute, whose value comes from a field named "theUser" in the
 ** variant options table.
 ** - an optional code reference to a variantRecordLookupFunction - to customize
 ** how the variant record is looked up (such as, adding joined or other custom
 ** fields).
 *******************************************************************************/
public class BackendVariantsConfig
{
   private String variantTypeKey;

   private String         optionsTableName;
   private QQueryFilter   optionsFilter;
   private QCodeReference variantRecordLookupFunction;

   private Map<BackendVariantSetting, String> backendSettingSourceFieldNameMap;



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getOptionsTableName()
   {
      return (this.optionsTableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setOptionsTableName(String optionsTableName)
   {
      this.optionsTableName = optionsTableName;
   }



   /*******************************************************************************
    ** Getter for filter
    *******************************************************************************/
   public QQueryFilter getOptionsFilter()
   {
      return (this.optionsFilter);
   }



   /*******************************************************************************
    ** Setter for filter
    *******************************************************************************/
   public void setOptionsFilter(QQueryFilter optionsFilter)
   {
      this.optionsFilter = optionsFilter;
   }



   /*******************************************************************************
    ** Getter for backendSettingSourceFieldNameMap
    *******************************************************************************/
   public Map<BackendVariantSetting, String> getBackendSettingSourceFieldNameMap()
   {
      return (this.backendSettingSourceFieldNameMap);
   }



   /*******************************************************************************
    ** Setter for backendSettingSourceFieldNameMap
    *******************************************************************************/
   public void setBackendSettingSourceFieldNameMap(Map<BackendVariantSetting, String> backendSettingSourceFieldNameMap)
   {
      this.backendSettingSourceFieldNameMap = backendSettingSourceFieldNameMap;
   }



   /*******************************************************************************
    ** Fluent setter for backendSettingSourceFieldNameMap
    *******************************************************************************/
   public BackendVariantsConfig withBackendSettingSourceFieldName(BackendVariantSetting backendVariantSetting, String sourceFieldName)
   {
      if(this.backendSettingSourceFieldNameMap == null)
      {
         this.backendSettingSourceFieldNameMap = new HashMap<>();
      }
      this.backendSettingSourceFieldNameMap.put(backendVariantSetting, sourceFieldName);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for backendSettingSourceFieldNameMap
    *******************************************************************************/
   public BackendVariantsConfig withBackendSettingSourceFieldNameMap(Map<BackendVariantSetting, String> backendSettingSourceFieldNameMap)
   {
      this.backendSettingSourceFieldNameMap = backendSettingSourceFieldNameMap;
      return (this);
   }



   /*******************************************************************************
    ** Getter for variantTypeKey
    *******************************************************************************/
   public String getVariantTypeKey()
   {
      return (this.variantTypeKey);
   }



   /*******************************************************************************
    ** Setter for variantTypeKey
    *******************************************************************************/
   public void setVariantTypeKey(String variantTypeKey)
   {
      this.variantTypeKey = variantTypeKey;
   }



   /*******************************************************************************
    ** Fluent setter for variantTypeKey
    *******************************************************************************/
   public BackendVariantsConfig withVariantTypeKey(String variantTypeKey)
   {
      this.variantTypeKey = variantTypeKey;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for optionsTableName
    *******************************************************************************/
   public BackendVariantsConfig withOptionsTableName(String optionsTableName)
   {
      this.optionsTableName = optionsTableName;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for optionsFilter
    *******************************************************************************/
   public BackendVariantsConfig withOptionsFilter(QQueryFilter optionsFilter)
   {
      this.optionsFilter = optionsFilter;
      return (this);
   }


   /*******************************************************************************
    ** Getter for variantRecordLookupFunction
    *******************************************************************************/
   public QCodeReference getVariantRecordLookupFunction()
   {
      return (this.variantRecordLookupFunction);
   }



   /*******************************************************************************
    ** Setter for variantRecordLookupFunction
    *******************************************************************************/
   public void setVariantRecordLookupFunction(QCodeReference variantRecordLookupFunction)
   {
      this.variantRecordLookupFunction = variantRecordLookupFunction;
   }



   /*******************************************************************************
    ** Fluent setter for variantRecordLookupFunction
    *******************************************************************************/
   public BackendVariantsConfig withVariantRecordLookupFunction(QCodeReference variantRecordLookupFunction)
   {
      this.variantRecordLookupFunction = variantRecordLookupFunction;
      return (this);
   }


}
