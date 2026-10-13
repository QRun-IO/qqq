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

package com.kingsrook.qqq.backend.core.model.metadata.collaboration;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.collaboration.RecordFeatureOverrideProviderInterface;
import com.kingsrook.qqq.backend.core.actions.collaboration.RecordFeatureSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.actions.collaboration.RuntimeMetaDataProviderInterface;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Shared feature declarations; also the instance-level supplemental metadata.
 *******************************************************************************/
public class RecordFeaturesMetaData implements QSupplementalInstanceMetaData
{
   private Map<RecordFeature, Boolean> enabled = new EnumMap<>(RecordFeature.class);
   private Map<RecordFeature, Map<String, Serializable>> settings = new EnumMap<>(RecordFeature.class);
   private Map<RecordFeature, FeaturePermissionMode> permissionModes = new EnumMap<>(RecordFeature.class);
   private Map<RecordFeature, Boolean> featureLocks = new EnumMap<>(RecordFeature.class);
   private RecordOwnership ownership;
   private QCodeReference overrideProvider;
   private List<QCodeReference> runtimeProviders = new ArrayList<>();
   private List<QCodeReference> summaryProviders = new ArrayList<>();
   public static final String NAME = "recordFeatures";



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordFeaturesMetaData of(QInstance instance)
   {
      return ((RecordFeaturesMetaData) instance.getSupplementalMetaData(NAME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordFeaturesMetaData ofOrWithNew(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(instance, NAME, RecordFeaturesMetaData::new));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordFeaturesMetaData of(QTableMetaData table)
   {
      RecordFeaturesTableMetaData supplemental = RecordFeaturesTableMetaData.of(table);
      return (supplemental == null ? null : supplemental.getRecordFeatures());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordFeaturesMetaData ofOrWithNew(QTableMetaData table)
   {
      return (RecordFeaturesTableMetaData.ofOrWithNew(table).getRecordFeatures());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordFeaturesMetaData of(QAppMetaData app)
   {
      RecordFeaturesAppMetaData supplemental = RecordFeaturesAppMetaData.of(app);
      return (supplemental == null ? null : supplemental.getRecordFeatures());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordFeaturesMetaData ofOrWithNew(QAppMetaData app)
   {
      return (RecordFeaturesAppMetaData.ofOrWithNew(app).getRecordFeatures());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordFeaturesMetaData withEnabled(RecordFeature feature, boolean value)
   {
      enabled.put(feature, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordFeaturesMetaData withSetting(RecordFeature feature, String name, Serializable value)
   {
      settings.computeIfAbsent(feature, key -> new LinkedHashMap<>()).put(name, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordFeaturesMetaData withPermissionMode(RecordFeature feature, FeaturePermissionMode mode)
   {
      permissionModes.put(feature, mode);
      return (this);
   }



   /*******************************************************************************
    ** A false lock is an absolute prohibition; a true value does not force enablement.
    *******************************************************************************/
   public RecordFeaturesMetaData withFeatureLocked(RecordFeature feature, boolean value)
   {
      featureLocks.put(feature, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validate(QInstance instance, QInstanceValidator validator)
   {
      if(overrideProvider != null)
      {
         validator.validateSimpleCodeReference(NAME + " override provider", overrideProvider, RecordFeatureOverrideProviderInterface.class);
      }
      for(QCodeReference provider : runtimeProviders)
      {
         validator.validateSimpleCodeReference(NAME + " runtime provider", provider, RuntimeMetaDataProviderInterface.class);
      }
      for(QCodeReference provider : summaryProviders)
      {
         validator.validateSimpleCodeReference(NAME + " summary provider", provider, RecordFeatureSummaryProviderInterface.class);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordFeaturesMetaData copy()
   {
      RecordFeaturesMetaData copy = new RecordFeaturesMetaData();
      copy.enabled.putAll(enabled);
      settings.forEach((feature, values) -> copy.settings.put(feature, new LinkedHashMap<>(values)));
      copy.permissionModes.putAll(permissionModes);
      copy.featureLocks.putAll(featureLocks);
      copy.ownership = ownership == null ? null : new RecordOwnership().withOwnerFieldName(ownership.getOwnerFieldName()).withAssigneeFieldName(ownership.getAssigneeFieldName());
      copy.overrideProvider = overrideProvider;
      copy.runtimeProviders = new ArrayList<>(runtimeProviders);
      copy.summaryProviders = new ArrayList<>(summaryProviders);
      return (copy);
   }



   /*******************************************************************************
    ** Getter for enabled
    *******************************************************************************/
   public Map<RecordFeature, Boolean> getEnabled()
   {
      return (this.enabled);
   }



   /*******************************************************************************
    ** Setter for enabled
    *******************************************************************************/
   public void setEnabled(Map<RecordFeature, Boolean> enabled)
   {
      this.enabled = enabled;
   }



   /*******************************************************************************
    ** Fluent setter for enabled
    *******************************************************************************/
   public RecordFeaturesMetaData withEnabled(Map<RecordFeature, Boolean> enabled)
   {
      setEnabled(enabled);
      return (this);
   }



   /*******************************************************************************
    ** Getter for settings
    *******************************************************************************/
   public Map<RecordFeature, Map<String, Serializable>> getSettings()
   {
      return (this.settings);
   }



   /*******************************************************************************
    ** Setter for settings
    *******************************************************************************/
   public void setSettings(Map<RecordFeature, Map<String, Serializable>> settings)
   {
      this.settings = settings;
   }



   /*******************************************************************************
    ** Fluent setter for settings
    *******************************************************************************/
   public RecordFeaturesMetaData withSettings(Map<RecordFeature, Map<String, Serializable>> settings)
   {
      setSettings(settings);
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissionModes
    *******************************************************************************/
   public Map<RecordFeature, FeaturePermissionMode> getPermissionModes()
   {
      return (this.permissionModes);
   }



   /*******************************************************************************
    ** Setter for permissionModes
    *******************************************************************************/
   public void setPermissionModes(Map<RecordFeature, FeaturePermissionMode> permissionModes)
   {
      this.permissionModes = permissionModes;
   }



   /*******************************************************************************
    ** Fluent setter for permissionModes
    *******************************************************************************/
   public RecordFeaturesMetaData withPermissionModes(Map<RecordFeature, FeaturePermissionMode> permissionModes)
   {
      setPermissionModes(permissionModes);
      return (this);
   }



   /*******************************************************************************
    ** Getter for featureLocks
    *******************************************************************************/
   public Map<RecordFeature, Boolean> getFeatureLocks()
   {
      return (this.featureLocks);
   }



   /*******************************************************************************
    ** Setter for featureLocks
    *******************************************************************************/
   public void setFeatureLocks(Map<RecordFeature, Boolean> featureLocks)
   {
      this.featureLocks = featureLocks;
   }



   /*******************************************************************************
    ** Fluent setter for featureLocks
    *******************************************************************************/
   public RecordFeaturesMetaData withFeatureLocks(Map<RecordFeature, Boolean> featureLocks)
   {
      setFeatureLocks(featureLocks);
      return (this);
   }



   /*******************************************************************************
    ** Getter for ownership
    *******************************************************************************/
   public RecordOwnership getOwnership()
   {
      return (this.ownership);
   }



   /*******************************************************************************
    ** Setter for ownership
    *******************************************************************************/
   public void setOwnership(RecordOwnership ownership)
   {
      this.ownership = ownership;
   }



   /*******************************************************************************
    ** Fluent setter for ownership
    *******************************************************************************/
   public RecordFeaturesMetaData withOwnership(RecordOwnership ownership)
   {
      setOwnership(ownership);
      return (this);
   }



   /*******************************************************************************
    ** Getter for overrideProvider
    *******************************************************************************/
   public QCodeReference getOverrideProvider()
   {
      return (this.overrideProvider);
   }



   /*******************************************************************************
    ** Setter for overrideProvider
    *******************************************************************************/
   public void setOverrideProvider(QCodeReference overrideProvider)
   {
      this.overrideProvider = overrideProvider;
   }



   /*******************************************************************************
    ** Fluent setter for overrideProvider
    *******************************************************************************/
   public RecordFeaturesMetaData withOverrideProvider(QCodeReference overrideProvider)
   {
      setOverrideProvider(overrideProvider);
      return (this);
   }



   /*******************************************************************************
    ** Getter for runtimeProviders
    *******************************************************************************/
   public List<QCodeReference> getRuntimeProviders()
   {
      return (this.runtimeProviders);
   }



   /*******************************************************************************
    ** Setter for runtimeProviders
    *******************************************************************************/
   public void setRuntimeProviders(List<QCodeReference> runtimeProviders)
   {
      this.runtimeProviders = runtimeProviders;
   }



   /*******************************************************************************
    ** Fluent setter for runtimeProviders
    *******************************************************************************/
   public RecordFeaturesMetaData withRuntimeProviders(List<QCodeReference> runtimeProviders)
   {
      setRuntimeProviders(runtimeProviders);
      return (this);
   }



   /*******************************************************************************
    ** Getter for summaryProviders
    *******************************************************************************/
   public List<QCodeReference> getSummaryProviders()
   {
      return (this.summaryProviders);
   }



   /*******************************************************************************
    ** Setter for summaryProviders
    *******************************************************************************/
   public void setSummaryProviders(List<QCodeReference> summaryProviders)
   {
      this.summaryProviders = summaryProviders;
   }



   /*******************************************************************************
    ** Fluent setter for summaryProviders
    *******************************************************************************/
   public RecordFeaturesMetaData withSummaryProviders(List<QCodeReference> summaryProviders)
   {
      setSummaryProviders(summaryProviders);
      return (this);
   }

}
