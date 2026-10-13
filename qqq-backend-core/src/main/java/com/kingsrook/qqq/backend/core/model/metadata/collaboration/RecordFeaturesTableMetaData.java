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
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public class RecordFeaturesTableMetaData extends QSupplementalTableMetaData
{
   private RecordFeaturesMetaData recordFeatures = new RecordFeaturesMetaData();



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getType()
   {
      return (RecordFeaturesMetaData.NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordFeaturesTableMetaData of(QTableMetaData object)
   {
      return (object.getSupplementalMetaData() == null ? null : (RecordFeaturesTableMetaData) object.getSupplementalMetaData(RecordFeaturesMetaData.NAME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordFeaturesTableMetaData ofOrWithNew(QTableMetaData object)
   {
      RecordFeaturesTableMetaData metadata = of(object);
      if(metadata == null)
      {
         metadata = new RecordFeaturesTableMetaData();
         object.withSupplementalMetaData(metadata);
      }
      return (metadata);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordFeaturesTableMetaData withEnabled(RecordFeature feature, boolean enabled)
   {
      recordFeatures.withEnabled(feature, enabled);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordFeaturesTableMetaData withSetting(RecordFeature feature, String name, Serializable value)
   {
      recordFeatures.withSetting(feature, name, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordFeaturesTableMetaData withPermissionMode(RecordFeature feature, FeaturePermissionMode mode)
   {
      recordFeatures.withPermissionMode(feature, mode);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordFeaturesTableMetaData withFeatureLocked(RecordFeature feature, boolean value)
   {
      recordFeatures.withFeatureLocked(feature, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordFeaturesTableMetaData withOwnership(RecordOwnership ownership)
   {
      recordFeatures.withOwnership(ownership);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validate(QInstance instance, QTableMetaData table, QInstanceValidator validator)
   {
      recordFeatures.validate(instance, validator);
      if(recordFeatures.getEnabled().containsValue(true))
      {
         validator.assertCondition(table.getPrimaryKeyField() != null && table.getFields().containsKey(table.getPrimaryKeyField()),
            "Record features explicitly enabled on table [" + table.getName() + "] require a single primary key");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected QSupplementalTableMetaData finishClone(QSupplementalTableMetaData abstractClone)
   {
      RecordFeaturesTableMetaData clone = (RecordFeaturesTableMetaData) abstractClone;
      clone.recordFeatures = recordFeatures.copy();
      return (clone);
   }



   /*******************************************************************************
    ** Getter for recordFeatures
    *******************************************************************************/
   public RecordFeaturesMetaData getRecordFeatures()
   {
      return (this.recordFeatures);
   }



   /*******************************************************************************
    ** Setter for recordFeatures
    *******************************************************************************/
   public void setRecordFeatures(RecordFeaturesMetaData recordFeatures)
   {
      this.recordFeatures = recordFeatures;
   }



   /*******************************************************************************
    ** Fluent setter for recordFeatures
    *******************************************************************************/
   public RecordFeaturesTableMetaData withRecordFeatures(RecordFeaturesMetaData recordFeatures)
   {
      setRecordFeatures(recordFeatures);
      return (this);
   }

}
