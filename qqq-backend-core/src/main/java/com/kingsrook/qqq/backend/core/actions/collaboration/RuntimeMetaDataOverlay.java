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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeatureOverride;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RuntimeMetaDataContribution;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RuntimeTableMetaDataContribution;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.realtime.RealtimeBus;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** An instance-identity scoped, atomic overlay. QInstance uses identity equality. Tables are copied before enrichment and validation.
 *******************************************************************************/
public class RuntimeMetaDataOverlay
{
   private static final Map<QInstance, RuntimeMetaDataContribution> SNAPSHOTS = new WeakHashMap<>();
   private static final Map<QInstance, Map<QCodeReference, RuntimeMetaDataContribution>> CONTRIBUTIONS = new WeakHashMap<>();
   private static final Map<QInstance, Map<String, AppliedTable>> APPLIED = new WeakHashMap<>();
   private static final QLogger LOG = QLogger.getLogger(RuntimeMetaDataOverlay.class);



   /*******************************************************************************
    ** Atomically publish a new generation. Invalid provider contributions retain
    ** their previous accepted values. Refresh also invalidates code declarations.
    *******************************************************************************/
   public static void refresh(QInstance instance) throws QException
   {
      long previousGeneration;
      long generation;
      synchronized(RuntimeMetaDataOverlay.class)
      {
         previousGeneration = getGeneration(instance);
         refreshSnapshot(instance);
         generation = getGeneration(instance);
      }
      if(generation != previousGeneration)
      {
         try
         {
            RealtimeBus.get(instance).publish(new RealtimeMessage()
               .withChannel("instance").withEvent("metadata.changed").withData(Map.of("generation", generation)));
         }
         catch(Exception | LinkageError e)
         {
            LOG.warn("Could not publish metadata change after runtime metadata refresh", e);
         }
      }
   }



   /*******************************************************************************
    ** Refresh under the overlay lock; publish after releasing it to avoid listener deadlocks.
    *******************************************************************************/
   private static void refreshSnapshot(QInstance instance) throws QException
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
                  .withFeatureOverrides(snapshot(instance, candidate.getFeatureOverrides())).withTables(snapshotTables(candidate.getTables()));
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
      applyTables(instance, accepted.values());
      CONTRIBUTIONS.put(instance, accepted);
      SNAPSHOTS.put(instance, new RuntimeMetaDataContribution().withGeneration(generation).withFeatureOverrides(List.copyOf(overrides)));
      RecordFeaturesResolver.invalidate(instance);
   }



   /*******************************************************************************
    ** Detach accepted contributions from provider-owned mutable metadata.
    *******************************************************************************/
   private static Map<String, RuntimeTableMetaDataContribution> snapshotTables(Map<String, RuntimeTableMetaDataContribution> tables)
   {
      Map<String, RuntimeTableMetaDataContribution> result = new LinkedHashMap<>();
      tables.forEach((name, table) -> result.put(name, new RuntimeTableMetaDataContribution()
         .withFields(table.getFields().stream().map(QFieldMetaData::clone).toList())
         .withVirtualFields(table.getVirtualFields().stream().map(QVirtualFieldMetaData::clone).toList())
         .withSearchFields(new ArrayList<>(table.getSearchFields()))
         .withSectionPlacements(new LinkedHashMap<>(table.getSectionPlacements()))));
      return (result);
   }



   /*******************************************************************************
    ** Track only overlay-owned objects, so later code declarations are not removed.
    *******************************************************************************/
   private record AppliedTable(Map<String, QFieldMetaData> fields, Map<String, QVirtualFieldMetaData> virtualFields,
                               Set<String> searchFields, Map<String, String> placements, Set<String> sections)
   {
   }



   /*******************************************************************************
    ** Build and validate independently, then publish all successful tables together.
    *******************************************************************************/
   private static void applyTables(QInstance instance, Collection<RuntimeMetaDataContribution> contributions) throws QException
   {
      synchronized(instance)
      {
         Map<String, AppliedTable> previous = APPLIED.getOrDefault(instance, Map.of());
         Map<String, AppliedTable> next = new LinkedHashMap<>(previous);
         Map<String, List<RuntimeTableMetaDataContribution>> byTable = new LinkedHashMap<>();
         previous.keySet().forEach(name -> byTable.put(name, new ArrayList<>()));
         for(RuntimeMetaDataContribution contribution : contributions)
         {
            contribution.getTables().forEach((name, table) -> byTable.computeIfAbsent(name, ignored -> new ArrayList<>()).add(table));
         }
         Map<String, QTableMetaData> replacements = new LinkedHashMap<>();
         for(var entry : byTable.entrySet())
         {
            String name = entry.getKey();
            try
            {
               QTableMetaData current = instance.getTable(name);
               if(current == null)
               {
                  throw (new QException("Unknown runtime table: " + name));
               }
               QTableMetaData table = current.clone();
               AppliedTable old = previous.get(name);
               if(old != null)
               {
                  old.fields().forEach((fieldName, field) ->
                  {
                     if(current.getFields().get(fieldName) == field)
                     {
                        table.getFields().remove(fieldName);
                     }
                  });
                  old.virtualFields().forEach((fieldName, field) ->
                  {
                     if(CollectionUtils.nonNullMap(current.getVirtualFields()).get(fieldName) == field)
                     {
                        table.getVirtualFields().remove(fieldName);
                     }
                  });
                  if(table.getSearchFields() != null)
                  {
                     table.getSearchFields().removeAll(old.searchFields());
                     if(table.getSearchFields().isEmpty())
                     {
                        table.setSearchFields(null);
                     }
                  }
                  old.placements().forEach((field, sectionName) ->
                  {
                     QFieldSection section = table.getSection(sectionName);
                     if(section != null && !table.getFields().containsKey(field) && !CollectionUtils.nonNullMap(table.getVirtualFields()).containsKey(field))
                     {
                        section.getFieldNames().remove(field);
                     }
                  });
                  if(table.getSections() != null)
                  {
                     table.getSections().removeIf(section -> old.sections().contains(section.getName()) && CollectionUtils.nullSafeIsEmpty(section.getFieldNames()));
                  }
               }
               Map<String, QFieldMetaData> fields = new LinkedHashMap<>();
               Map<String, QVirtualFieldMetaData> virtualFields = new LinkedHashMap<>();
               Map<String, String> placements = new LinkedHashMap<>();
               Set<String> searchFields = new LinkedHashSet<>();
               Set<String> sections = new LinkedHashSet<>();
               if(CollectionUtils.nullSafeIsEmpty(table.getSections()))
               {
                  new QInstanceEnricher(instance).enrichTable(table);
               }
               for(RuntimeTableMetaDataContribution contribution : entry.getValue())
               {
                  List<QFieldMetaData> additions = new ArrayList<>(contribution.getFields());
                  additions.addAll(contribution.getVirtualFields());
                  for(QFieldMetaData field : additions)
                  {
                     if(table.getFields().containsKey(field.getName()) || CollectionUtils.nonNullMap(table.getVirtualFields()).containsKey(field.getName()))
                     {
                        LOG.warn("Skipping runtime field name clash: " + name + "." + field.getName());
                        continue;
                     }
                     QFieldMetaData copy = field.clone();
                     if(copy instanceof QVirtualFieldMetaData virtualField)
                     {
                        table.addVirtualField(virtualField);
                        virtualFields.put(copy.getName(), virtualField);
                     }
                     else
                     {
                        table.addField(copy);
                        fields.put(copy.getName(), copy);
                     }
                     if(!(copy instanceof QVirtualFieldMetaData) && !copy.getIsHidden())
                     {
                        String sectionName = contribution.getSectionPlacements().getOrDefault(copy.getName(), "customFields");
                        QFieldSection section = table.getSection(sectionName);
                        if(section == null)
                        {
                           section = new QFieldSection().withName(sectionName).withLabel("Custom fields").withTier(Tier.T2).withFieldNames(new ArrayList<>());
                           table.addSection(section);
                           sections.add(sectionName);
                        }
                        section.getFieldNames().add(copy.getName());
                        placements.put(copy.getName(), sectionName);
                     }
                  }
                  if(table.getSearchFields() == null && !contribution.getSearchFields().isEmpty())
                  {
                     table.setSearchFields(new ArrayList<>());
                  }
                  for(String fieldName : contribution.getSearchFields())
                  {
                     if(!table.getSearchFields().contains(fieldName))
                     {
                        table.getSearchFields().add(fieldName);
                        searchFields.add(fieldName);
                     }
                  }
               }
               new QInstanceEnricher(instance).enrichTable(table);
               new QInstanceValidator().validateRuntimeTable(instance, table);
               replacements.put(name, table);
               next.put(name, new AppliedTable(fields, virtualFields, searchFields, placements, sections));
            }
            catch(Exception | LinkageError e)
            {
               LOG.warn("Skipping invalid runtime table contribution; retaining previous metadata: " + name, e);
            }
         }
         new QInstanceValidator().replaceRuntimeTables(instance, replacements);
         APPLIED.put(instance, next);
      }
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
