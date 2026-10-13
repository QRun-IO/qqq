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
import java.util.WeakHashMap;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeatureOverride;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RuntimeMetaDataContribution;


/*******************************************************************************
 ** An instance-identity scoped, atomic overlay. QInstance uses identity equality. No runtime fields in phase (a).
 *******************************************************************************/
public class RuntimeMetaDataOverlay
{
   private static final Map<QInstance, RuntimeMetaDataContribution> SNAPSHOTS = new WeakHashMap<>();
   private static final Map<QInstance, Map<QCodeReference, RuntimeMetaDataContribution>> CONTRIBUTIONS = new WeakHashMap<>();
   private static final QLogger LOG = QLogger.getLogger(RuntimeMetaDataOverlay.class);



   /*******************************************************************************
    ** Atomically publish a new generation. Invalid provider contributions retain
    ** their previous accepted values. Refresh also invalidates code declarations.
    *******************************************************************************/
   public static synchronized void refresh(QInstance instance) throws QException
   {
      List<RecordFeatureOverride> overrides = new ArrayList<>();
      long generation = getGeneration(instance) + 1;
      Map<QCodeReference, RuntimeMetaDataContribution> previous = CONTRIBUTIONS.getOrDefault(instance, Map.of());
      Map<QCodeReference, RuntimeMetaDataContribution> accepted = new LinkedHashMap<>();
      RecordFeaturesMetaData metadata = RecordFeaturesMetaData.of(instance);
      if(metadata != null)
      {
         List<QCodeReference> providers = new ArrayList<>();
         if(metadata.getOverrideProvider() != null)
         {
            providers.add(metadata.getOverrideProvider());
         }
         providers.addAll(metadata.getRuntimeProviders());
         for(QCodeReference reference : providers)
         {
            RuntimeMetaDataContribution contribution = previous.get(reference);
            try
            {
               RuntimeMetaDataContribution candidate = reference == metadata.getOverrideProvider()
                  ? new RuntimeMetaDataContribution().withFeatureOverrides(QCodeLoader.getAdHoc(RecordFeatureOverrideProviderInterface.class, reference).getOverrides(instance))
                  : QCodeLoader.getAdHoc(RuntimeMetaDataProviderInterface.class, reference).getContribution(instance);
               contribution = new RuntimeMetaDataContribution().withGeneration(candidate.getGeneration())
                  .withFeatureOverrides(snapshot(instance, candidate.getFeatureOverrides()));
            }
            catch(Exception | LinkageError e)
            {
               LOG.warn("Skipping invalid runtime metadata contribution; retaining previous contribution", e);
            }
            if(contribution != null)
            {
               accepted.put(reference, contribution);
               overrides.addAll(contribution.getFeatureOverrides());
               generation = Math.max(generation, contribution.getGeneration());
            }
         }
      }
      CONTRIBUTIONS.put(instance, accepted);
      SNAPSHOTS.put(instance, new RuntimeMetaDataContribution().withGeneration(generation).withFeatureOverrides(List.copyOf(overrides)));
      RecordFeaturesResolver.invalidate(instance);
   }



   /*******************************************************************************
    ** Copy and validate a provider's override list before making it visible.
    *******************************************************************************/
   private static List<RecordFeatureOverride> snapshot(QInstance instance, List<RecordFeatureOverride> overrides) throws QException
   {
      List<RecordFeatureOverride> snapshot = new ArrayList<>();
      for(RecordFeatureOverride entry : overrides)
      {
         if(entry == null || entry.getFeature() == null || entry.getScopeType() == null || entry.getSettings() == null
            || (entry.getScopeType() == RecordFeatureOverride.ScopeType.APP && instance.getApp(entry.getScopeName()) == null)
            || (entry.getScopeType() == RecordFeatureOverride.ScopeType.TABLE && instance.getTable(entry.getScopeName()) == null))
         {
            throw (new QException("Invalid record feature override"));
         }
         snapshot.add(new RecordFeatureOverride().withScopeType(entry.getScopeType()).withScopeName(entry.getScopeName())
            .withFeature(entry.getFeature()).withEnabled(entry.getEnabled()).withPermissionMode(entry.getPermissionMode())
            .withSettings(new LinkedHashMap<>(entry.getSettings())));
      }
      return (List.copyOf(snapshot));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static synchronized long getGeneration(QInstance instance)
   {
      return (SNAPSHOTS.containsKey(instance) ? SNAPSHOTS.get(instance).getGeneration() : 0L);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static synchronized List<RecordFeatureOverride> getOverrides(QInstance instance)
   {
      return (SNAPSHOTS.containsKey(instance) ? SNAPSHOTS.get(instance).getFeatureOverrides() : List.of());
   }

}
