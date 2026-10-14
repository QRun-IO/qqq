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

package com.kingsrook.qqq.backend.core.model.metadata.help.bundles;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.DurableJobDiversion;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpFormat;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpValueProvider;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpRole;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.YamlUtils;


/*******************************************************************************
 ** Classpath help bundles in registration order, with deterministic precedence.
 *******************************************************************************/
public class HelpBundles implements QSupplementalInstanceMetaData
{
   public static final String NAME = "helpBundles";
   private static final QLogger LOG = QLogger.getLogger(HelpBundles.class);
   private final List<HelpBundle> bundles = new ArrayList<>();
   private final List<HelpValueProvider> valueProviders = new ArrayList<>();


   /*******************************************************************************
    ** A help row's unique address.
    *******************************************************************************/
   public record EntryKey(String key, String role)
   {
   }



   /*******************************************************************************
    ** Winning text and its provenance.
    *******************************************************************************/
   public record WinningEntry(HelpBundleEntry entry, HelpBundle bundle)
   {
   }



   /*******************************************************************************
    ** Supplemental metadata name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Obtain the instance's registry.
    *******************************************************************************/
   public static HelpBundles of(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(instance, NAME, HelpBundles::new));
   }



   /*******************************************************************************
    ** Register using the context class loader, falling back to core's loader.
    *******************************************************************************/
   public static void register(QInstance instance, String resource)
   {
      register(instance, resource, Thread.currentThread().getContextClassLoader());
   }



   /*******************************************************************************
    ** Register with the caller's loader when the host uses isolated class loaders.
    *******************************************************************************/
   public static void register(QInstance instance, String resource, ClassLoader loader)
   {
      of(instance).bundles.add(load(resource, loader));
   }



   /*******************************************************************************
    ** Load independently of registration; report the resource on every failure.
    *******************************************************************************/
   public static HelpBundle load(String resource, ClassLoader loader)
   {
      ClassLoader effective = loader == null ? HelpBundles.class.getClassLoader() : loader;
      try(InputStream stream = effective.getResourceAsStream(resource))
      {
         if(stream == null)
         {
            throw new IllegalArgumentException("Resource not found");
         }
         Map<String, Object> yaml = YamlUtils.toMap(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
         Object id = yaml.remove("bundle");
         yaml.put("id", id);
         HelpBundle bundle = new ObjectMapper().convertValue(yaml, HelpBundle.class);
         if(bundle.getId() == null || bundle.getId().isBlank() || bundle.getId().length() > 100
            || bundle.getVersion() == null || bundle.getVersion().isBlank() || bundle.getVersion().length() > 50 || bundle.getLayer() == null || bundle.getEntries() == null)
         {
            throw new IllegalArgumentException("bundle, version, layer and entries are required");
         }
         for(HelpBundleEntry entry : bundle.getEntries())
         {
            validateEntry(entry);
         }
         return (bundle);
      }
      catch(Exception e)
      {
         throw new IllegalArgumentException("Invalid help bundle " + resource + ": " + e.getMessage(), e);
      }
   }



   /*******************************************************************************
    ** Validate the grammar without requiring optional provider metadata to exist.
    *******************************************************************************/
   public static void validateEntry(HelpBundleEntry entry)
   {
      if(entry == null || entry.getKey() == null || entry.getKey().length() > 250
         || !entry.getKey().matches("(?:instanceLevel:true;slot:[^;:]+|(?:app|table|process|report|widget|possibleValueSource):[^;:]+;(?:slot|field|section|step):[^;:]+)"))
      {
         throw new IllegalArgumentException("Invalid help key");
      }
      String kind = entry.getKey().substring(0, entry.getKey().indexOf(':'));
      String member = entry.getKey().substring(entry.getKey().indexOf(';') + 1).split(":", 2)[0];
      if(!(member.equals("slot") || (kind.equals("table") && List.of("field", "section").contains(member))
         || (kind.equals("process") && List.of("field", "step").contains(member))))
      {
         throw new IllegalArgumentException("Invalid help key " + entry.getKey());
      }
      QHelpRole.valueOf(entry.getRole());
      if(entry.getSummary() != null && entry.getSummary().length() > 500)
      {
         throw new IllegalArgumentException("Summary exceeds 500 characters: " + entry.getKey());
      }
      if(entry.getFormat() == null && entry.getContent() != null)
      {
         entry.setFormat(HelpFormat.MARKDOWN.name());
      }
      if(entry.getFormat() != null)
      {
         HelpFormat.valueOf(entry.getFormat());
      }
   }



   /*******************************************************************************
    ** Registered bundles in precedence tie order.
    *******************************************************************************/
   public List<HelpBundle> getBundles()
   {
      return (List.copyOf(bundles));
   }



   /*******************************************************************************
    ** Resolve one winner for each key and role. Later peers override earlier peers.
    *******************************************************************************/
   public Map<EntryKey, WinningEntry> resolveWinningEntries()
   {
      Map<EntryKey, WinningEntry> result = new LinkedHashMap<>();
      for(HelpBundle bundle : bundles)
      {
         for(HelpBundleEntry entry : bundle.getEntries())
         {
            EntryKey key = new EntryKey(entry.getKey(), entry.getRole());
            WinningEntry old = result.get(key);
            if(old != null && old.bundle().getLayer() == bundle.getLayer())
            {
               LOG.info("Help bundle tie for " + key + "; later registration wins: " + bundle.getId());
            }
            if(old == null || bundle.getLayer().ordinal() >= old.bundle().getLayer().ordinal())
            {
               result.put(key, new WinningEntry(entry, bundle));
            }
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** Register a host or qbit setting resolver.
    *******************************************************************************/
   public HelpBundles withValueProvider(HelpValueProvider provider)
   {
      valueProviders.add(provider);
      return (this);
   }



   /*******************************************************************************
    ** Setting resolvers in registration order.
    *******************************************************************************/
   public List<HelpValueProvider> getValueProviders()
   {
      return (List.copyOf(valueProviders));
   }



   /*******************************************************************************
    ** Start after loadHelpContent; startup and the caller's session are unaffected.
    *******************************************************************************/
   public static void syncOnStartup(QInstance instance)
   {
      try
      {
         if(instance.getTable("helpContent") == null || !instance.getTable("helpContent").getFields().containsKey("source"))
         {
            return;
         }
         //////////////////////////////////////////////////////////////////////////
         // the sync is short, system-scoped work: it runs in the background as  //
         // the system user, always inline (never diverted to a durable job,     //
         // which is for user-launched work and needs a user to own it), so it   //
         // completes the same way whether or not durable jobs are configured    //
         //////////////////////////////////////////////////////////////////////////
         QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession(instance)), () ->
            new AsyncJobManager().startJob("helpBundleSync", callback -> new RunProcessAction().execute(new RunProcessInput()
               .withProcessName("helpBundleSync")
               .withValue(DurableJobDiversion.VALUE_RUN_INLINE, true)
               .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP))));
      }
      catch(Exception | LinkageError e)
      {
         LOG.warn("Unable to start help bundle sync", e);
      }
   }
}
