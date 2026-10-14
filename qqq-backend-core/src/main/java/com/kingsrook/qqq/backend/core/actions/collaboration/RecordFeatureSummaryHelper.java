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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.util.LinkedHashMap;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.FrontendRecordFeatures;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeatureSummary;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.ResolvedRecordFeatures;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Build session-specific frontend publication and record summaries.
 *******************************************************************************/
public class RecordFeatureSummaryHelper
{



   /*******************************************************************************
    **
    *******************************************************************************/
   public static FrontendRecordFeatures frontend(QInstance instance, QTableMetaData table)
   {
      if(!RecordFeaturesResolver.hasAnyMetaData(instance))
      {
         return (null);
      }
      ResolvedRecordFeatures resolved = RecordFeaturesResolver.resolve(instance, table);
      FrontendRecordFeatures result = new FrontendRecordFeatures().withFeatures(resolved.enabledFeatures()).withOwnership(resolved.getOwnership());
      for(RecordFeature feature : resolved.enabledFeatures())
      {
         result.getSettings().put(feature, new LinkedHashMap<>(resolved.getFeatures().get(feature).getSettings()));
      }
      for(RecordFeature feature : RecordFeature.values())
      {
         if(feature.getPermissionSuffix() != null)
         {
            result.getCapabilities().put(feature.getPermissionSuffix(), RecordFeaturePermissions.hasTableCapability(table, feature));
         }
      }
      return (result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordFeatureSummary summarize(QTableMetaData table, QRecord parent) throws QException
   {
      ResolvedRecordFeatures resolved = RecordFeaturesResolver.resolve(QContext.getQInstance(), table);
      RecordReference reference = new RecordReference(table.getName(), parent.getValue(table.getPrimaryKeyField()));
      RecordFeatureSummary result = new RecordFeatureSummary().withTableName(table.getName()).withPrimaryKey(reference.primaryKeyAsString())
         .withFeatures(resolved.enabledFeatures());
      if(resolved.getOwnership() != null)
      {
         result.setOwner(parent.getValueString(resolved.getOwnership().getOwnerFieldName()));
         result.setAssignee(parent.getValueString(resolved.getOwnership().getAssigneeFieldName()));
      }
      RecordFeaturesMetaData metadata = RecordFeaturesMetaData.of(QContext.getQInstance());
      if(metadata != null)
      {
         for(QCodeReference provider : metadata.getSummaryProviders())
         {
            QCodeLoader.getAdHoc(RecordFeatureSummaryProviderInterface.class, provider).contribute(reference, new QRecord(parent), result);
         }
      }
      result.setTableName(table.getName());
      result.setPrimaryKey(reference.primaryKeyAsString());
      result.setFeatures(resolved.enabledFeatures());
      result.getCapabilities().clear();
      for(RecordFeature feature : RecordFeature.values())
      {
         if(feature.getPermissionSuffix() != null)
         {
            result.getCapabilities().put(feature.getPermissionSuffix(), RecordFeaturePermissions.checkRecordCapability(table, feature, parent));
         }
      }
      return (result);
   }

}
