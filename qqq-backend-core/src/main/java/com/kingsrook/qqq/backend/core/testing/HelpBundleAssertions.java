/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.testing;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import com.kingsrook.qqq.backend.core.actions.help.HelpTarget;
import com.kingsrook.qqq.backend.core.actions.help.HelpTargetCatalog;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpTextRenderer;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundle;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;


/*******************************************************************************
 ** Framework-independent help coverage gate for core, qbits and host applications.
 *******************************************************************************/
public class HelpBundleAssertions
{
   private final QInstance instance;
   private final QInstance bundleInstance = new QInstance();
   private final List<String> loadProblems = new ArrayList<>();
   private final Set<String> exclusions = new HashSet<>();
   private Predicate<HelpTarget> owning = target -> true;
   private Predicate<QFieldMetaData> excludedFields = field -> false;


   /*******************************************************************************
    ** Create a gate for this instance.
    *******************************************************************************/
   private HelpBundleAssertions(QInstance instance)
   {
      this.instance = instance;
   }



   /*******************************************************************************
    ** Entry point, with no dependency on JUnit.
    *******************************************************************************/
   public static HelpBundleAssertions forInstance(QInstance instance)
   {
      return (new HelpBundleAssertions(instance));
   }



   /*******************************************************************************
    ** Load resources independently of the instance's registered bundles.
    *******************************************************************************/
   public HelpBundleAssertions bundles(String... resources)
   {
      for(String resource : resources)
      {
         try
         {
            HelpBundles.register(bundleInstance, resource);
         }
         catch(IllegalArgumentException e)
         {
            loadProblems.add(e.getMessage());
         }
      }
      return (this);
   }



   /*******************************************************************************
    ** Select targets using their kind, key, name, table or owner.
    *******************************************************************************/
   public HelpBundleAssertions owning(Predicate<HelpTarget> owning)
   {
      this.owning = owning;
      return (this);
   }



   /*******************************************************************************
    ** Exempt explicit keys from required coverage.
    *******************************************************************************/
   public HelpBundleAssertions excluding(String... keys)
   {
      exclusions.addAll(List.of(keys));
      return (this);
   }



   /*******************************************************************************
    ** Exempt application-specific trivial fields.
    *******************************************************************************/
   public HelpBundleAssertions excludingFields(Predicate<QFieldMetaData> predicate)
   {
      excludedFields = excludedFields.or(predicate);
      return (this);
   }



   /*******************************************************************************
    ** Return all missing coverage and invalid entries, suitable for tooling.
    *******************************************************************************/
   public List<String> report()
   {
      List<String> problems = new ArrayList<>(loadProblems);
      List<HelpTarget> targets = HelpTargetCatalog.collect(instance);
      Set<String> covered = new HashSet<>();
      HelpTextRenderer renderer = new HelpTextRenderer(instance);
      HelpBundles bundles = HelpBundles.of(bundleInstance);
      for(HelpBundle bundle : bundles.getBundles())
      {
         for(var entry : bundle.getEntries())
         {
            String key = entry.getKey();
            if(!resolves(instance, key))
            {
               problems.add("Unresolved key: " + key);
            }
            String summary = entry.getSummary();
            if(summary != null)
            {
               String withoutReferences = summary.replaceAll("\\{\\{[^{}]*}}", "");
               if(summary.length() > 120 || withoutReferences.matches("(?s).*[<>*_`\\[\\]\\r\\n].*")
                  || withoutReferences.matches("(?s)^\\s*(?:#{1,6}\\s|[-+]\\s|\\d+[.)]\\s).*"))
               {
                  problems.add("Summary must be plain text and at most 120 characters: " + key);
               }
            }
            for(String text : new String[] { entry.getSummary(), entry.getContent() })
            {
               for(String reference : renderer.unresolved(text))
               {
                  problems.add("Unresolved reference in " + key + ": " + reference);
               }
            }
         }
      }
      for(var winner : bundles.resolveWinningEntries().values())
      {
         var entry = winner.entry();
         if(!entry.getRetired() && ((entry.getSummary() != null && !entry.getSummary().isBlank()) || (entry.getContent() != null && !entry.getContent().isBlank())))
         {
            targets.stream().filter(target -> matches(entry.getKey(), target.key())).forEach(target -> covered.add(target.key()));
         }
      }
      for(HelpTarget target : targets)
      {
         if(required(target) && owned(target, targets) && !exclusions.contains(target.key()) && !covered.contains(target.key()))
         {
            problems.add("Missing help: " + target.key());
         }
      }
      return (problems);
   }



   /*******************************************************************************
    ** Fail once with the complete list, so authors can fix every issue together.
    *******************************************************************************/
   public void assertComplete()
   {
      List<String> problems = report();
      if(!problems.isEmpty())
      {
         throw new AssertionError("Help bundle coverage failed:\n - " + String.join("\n - ", problems));
      }
   }



   /*******************************************************************************
    ** A table's ownership includes its non-trivial fields.
    *******************************************************************************/
   private boolean owned(HelpTarget target, List<HelpTarget> targets)
   {
      return (owning.test(target) || ("field".equals(target.kind()) && targets.stream()
         .anyMatch(parent -> "table".equals(parent.kind()) && target.table().equals(parent.name()) && owning.test(parent))));
   }



   /*******************************************************************************
    ** Owned objects and non-trivial fields need help; sections remain optional.
    *******************************************************************************/
   private boolean required(HelpTarget target)
   {
      if(List.of("app", "table", "process").contains(target.kind()))
      {
         return (target.key().endsWith(";slot:help"));
      }
      if("field".equals(target.kind()))
      {
         var table = instance.getTable(target.table());
         var field = table.getField(target.name());
         return (!field.getIsHidden() && !target.name().equals(table.getPrimaryKeyField())
            && !List.of("createDate", "modifyDate").contains(target.name()) && !excludedFields.test(field));
      }
      return (false);
   }



   /*******************************************************************************
    ** Match the supported process suffix wildcard without treating keys as regex.
    *******************************************************************************/
   public static boolean matches(String key, String target)
   {
      if(key.startsWith("process:*"))
      {
         return (target.startsWith("process:") && target.substring(8).endsWith(key.substring(9)));
      }
      return (key.equals(target));
   }



   /*******************************************************************************
    ** Slots are extensible; fields, sections and steps must exist in metadata.
    *******************************************************************************/
   public static boolean resolves(QInstance instance, String key)
   {
      if(key.startsWith("instanceLevel:true;slot:"))
      {
         return (true);
      }
      List<HelpTarget> targets = HelpTargetCatalog.collect(instance);
      if(targets.stream().anyMatch(target -> matches(key, target.key())))
      {
         return (true);
      }
      int slot = key.indexOf(";slot:");
      return (slot > 0 && targets.stream().anyMatch(target -> matches(key.substring(0, slot) + ";slot:help", target.key())
         || matches(key.substring(0, slot) + ";slot:label", target.key())));
   }
}
