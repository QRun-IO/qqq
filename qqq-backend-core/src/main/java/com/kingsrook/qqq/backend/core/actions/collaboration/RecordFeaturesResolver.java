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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeatureOverride;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordOwnership;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.ResolvedRecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.ResolvedRecordFeatures;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Merge code levels, then runtime levels, then enforce hard limits. Apps use instance iteration order.
 *******************************************************************************/
public class RecordFeaturesResolver
{
   private static final Map<QInstance, Map<QTableMetaData, ResolvedRecordFeatures>> CACHE = new WeakHashMap<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   static void invalidate(QInstance instance)
   {
      CACHE.remove(instance);
   }



   /*******************************************************************************
    ** Return a defensive copy of a resolution cached until overlay refresh.
    *******************************************************************************/
   public static ResolvedRecordFeatures resolve(QInstance instance, QTableMetaData table)
   {
      synchronized(RuntimeMetaDataOverlay.class)
      {
         Map<QTableMetaData, ResolvedRecordFeatures> tables = CACHE.computeIfAbsent(instance, key -> new WeakHashMap<>());
         ResolvedRecordFeatures resolved = tables.computeIfAbsent(table, key -> build(instance, table));
         RecordOwnership ownership = resolved.getOwnership();
         ResolvedRecordFeatures copy = new ResolvedRecordFeatures().withOwnership(ownership == null ? null
            : new RecordOwnership().withOwnerFieldName(ownership.getOwnerFieldName()).withAssigneeFieldName(ownership.getAssigneeFieldName()));
         resolved.getFeatures().forEach((feature, value) -> copy.getFeatures().put(feature, new ResolvedRecordFeature()
            .withEnabled(value.getEnabled()).withPermissionMode(value.getPermissionMode()).withSettings(new LinkedHashMap<>(value.getSettings()))));
         return (copy);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean hasAnyMetaData(QInstance instance)
   {
      return (RecordFeaturesMetaData.of(instance) != null
         || CollectionUtils.nonNullMap(instance.getApps()).values().stream().anyMatch(app -> RecordFeaturesMetaData.of(app) != null)
         || CollectionUtils.nonNullMap(instance.getTables()).values().stream().anyMatch(table -> RecordFeaturesMetaData.of(table) != null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ResolvedRecordFeatures build(QInstance instance, QTableMetaData table)
   {
      ResolvedRecordFeatures result = new ResolvedRecordFeatures();
      for(RecordFeature feature : RecordFeature.values())
      {
         result.getFeatures().put(feature, new ResolvedRecordFeature());
      }
      List<RecordFeaturesMetaData> levels = new ArrayList<>();
      levels.add(RecordFeaturesMetaData.of(instance));
      List<String> appNames = new ArrayList<>();
      for(QAppMetaData app : CollectionUtils.nonNullMap(instance.getApps()).values())
      {
         boolean containsTable = false;
         for(QAppSection section : CollectionUtils.nonNullList(app.getSections()))
         {
            containsTable |= CollectionUtils.nonNullList(section.getTables()).contains(table.getName());
         }
         if(containsTable)
         {
            levels.add(RecordFeaturesMetaData.of(app));
            appNames.add(app.getName());
         }
      }
      levels.add(RecordFeaturesMetaData.of(table));
      for(RecordFeaturesMetaData level : levels)
      {
         if(level == null)
         {
            continue;
         }
         level.getEnabled().forEach((feature, value) -> result.getFeatures().get(feature).setEnabled(Boolean.TRUE.equals(value)));
         level.getSettings().forEach((feature, values) -> result.getFeatures().get(feature).getSettings().putAll(values));
         level.getPermissionModes().forEach((feature, mode) -> result.getFeatures().get(feature).setPermissionMode(mode));
         if(level.getOwnership() != null)
         {
            result.setOwnership(level.getOwnership());
         }
      }
      List<RecordFeatureOverride> overrides = RuntimeMetaDataOverlay.getOverrides(instance);
      applyOverrides(result, overrides, RecordFeatureOverride.ScopeType.INSTANCE, null);
      for(String appName : appNames)
      {
         applyOverrides(result, overrides, RecordFeatureOverride.ScopeType.APP, appName);
      }
      applyOverrides(result, overrides, RecordFeatureOverride.ScopeType.TABLE, table.getName());
      boolean eligible = table.getPrimaryKeyField() != null && table.getFields().containsKey(table.getPrimaryKeyField())
         && !table.getName().startsWith("collaboration_")
         && (table.isCapabilityEnabled(instance.getBackend(table.getBackendName()), Capability.TABLE_GET)
         || table.isCapabilityEnabled(instance.getBackend(table.getBackendName()), Capability.TABLE_QUERY));
      for(RecordFeature feature : RecordFeature.values())
      {
         if(!eligible || levels.stream().filter(Objects::nonNull).anyMatch(level -> Boolean.FALSE.equals(level.getFeatureLocks().get(feature))))
         {
            result.getFeatures().get(feature).setEnabled(false);
         }
      }
      return (result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void applyOverrides(ResolvedRecordFeatures result, List<RecordFeatureOverride> overrides, RecordFeatureOverride.ScopeType scope, String name)
   {
      for(RecordFeatureOverride entry : overrides)
      {
         if(entry.getScopeType() != scope || (scope != RecordFeatureOverride.ScopeType.INSTANCE && !Objects.equals(name, entry.getScopeName())))
         {
            continue;
         }
         ResolvedRecordFeature feature = result.getFeatures().get(entry.getFeature());
         if(entry.getEnabled() != null)
         {
            feature.setEnabled(entry.getEnabled());
         }
         feature.getSettings().putAll(entry.getSettings());
         if(entry.getPermissionMode() != null)
         {
            feature.setPermissionMode(entry.getPermissionMode());
         }
      }
   }

}
