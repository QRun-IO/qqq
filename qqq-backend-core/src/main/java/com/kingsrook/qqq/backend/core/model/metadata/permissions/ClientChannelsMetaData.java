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

package com.kingsrook.qqq.backend.core.model.metadata.permissions;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** Client channels: which clients (Next UI, the AI assistant, a CLI...) may
 ** call the UI API, and which objects are always denied to a channel, whatever
 ** the human user's permissions.
 **
 ** - A request names its client in the X-QQQ-Client header
 **   (`channel[/detail]`, e.g. `next-ui`, `assistant/turn-42`); the channel must
 **   be one of getChannels().  The server stores it on the QSession
 **   (QSession.clientChannel); it is written to audits.  The actor is always the
 **   human user: a channel never grants anything.
 ** - Deny rules: QPermissionRules.withDeniedClients on an object, deniedObjects
 **   here by exact name, and deniedPatterns here by name glob (`*` wildcard),
 **   over tables, processes, reports, widgets and apps.  A denied object is
 **   handled as if the user had no permission: omitted from metadata, and
 **   its operations refused (PermissionsHelper).
 ** - Fail-closed: when an instance registers nothing, defaults() applies -
 **   including the assistant wall (its own configuration, identity and
 **   permission administration, audits, API keys, credentials and secrets).
 **   Only code or admin metadata changes these rules; a client cannot remove
 **   its own channel label (UiApiInvoker sets it server side).
 ** See docs/UI-API-CLIENT-CHANNELS.md.
 *******************************************************************************/
public class ClientChannelsMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "clientChannels";

   public static final String CHANNEL_NEXT_UI            = "next-ui";
   public static final String CHANNEL_MATERIAL_DASHBOARD = "material-dashboard";
   public static final String CHANNEL_ASSISTANT          = "assistant";
   public static final String CHANNEL_CLI                = "cli";
   public static final String CHANNEL_TUI                = "tui";
   public static final String CHANNEL_IOS                = "ios";
   public static final String CHANNEL_API                = "api";

   /***************************************************************************
    ** the assistant's default deny patterns (lower-cased glob over names)
    ***************************************************************************/
   public static final List<String> ASSISTANT_DEFAULT_DENIED_PATTERNS = List.of(
      "assistant_*",   // its own configuration: providers, models, prices, profiles, skills, policy, budgets, usage, tool-call logs, conversations
      "authentik_*",   // identity, permissions and policies
      "*api_key*", "*apikey*",
      "*credential*",
      "*secret*",
      "*password*",
      "*permission*", "role", "roles", "*_role", "*_roles", "*_role_*", "user_role*", "userrole*",
      "*user_session*", "*usersession*",
      "webhooks_*",    // webhook endpoints and their signing secrets
      "worm_audit_*"); // the WORM audit chain, checkpoints, sealed exports and purges

   /***************************************************************************
    ** the assistant's default write-deny patterns: readable (e.g. a record's
    ** history), never insertable, editable or deletable - no tampering
    ***************************************************************************/
   public static final List<String> ASSISTANT_DEFAULT_WRITE_DENIED_PATTERNS = List.of("audit*");

   private static final Pattern CHANNEL_PATTERN = Pattern.compile("[a-z0-9][a-z0-9-]*");

   private Map<String, String>       channels       = new LinkedHashMap<>();
   private Map<String, Set<String>>  deniedObjects  = new LinkedHashMap<>();
   private Map<String, List<String>> deniedPatterns = new LinkedHashMap<>();
   private Map<String, List<String>> deniedWritePatterns = new LinkedHashMap<>();



   /*******************************************************************************
    ** The defaults: the standard channels, and the assistant wall.
    *******************************************************************************/
   public static ClientChannelsMetaData defaults()
   {
      ClientChannelsMetaData rs = new ClientChannelsMetaData()
         .withChannel(CHANNEL_NEXT_UI, "The Next web UI")
         .withChannel(CHANNEL_MATERIAL_DASHBOARD, "The Material Dashboard web UI")
         .withChannel(CHANNEL_ASSISTANT, "The AI assistant, acting for the signed-in user")
         .withChannel(CHANNEL_CLI, "A command-line client")
         .withChannel(CHANNEL_TUI, "A terminal UI client")
         .withChannel(CHANNEL_IOS, "A native iOS client")
         .withChannel(CHANNEL_API, "Another program calling the UI API");
      for(String pattern : ASSISTANT_DEFAULT_DENIED_PATTERNS)
      {
         rs.withDeniedPattern(CHANNEL_ASSISTANT, pattern);
      }
      for(String pattern : ASSISTANT_DEFAULT_WRITE_DENIED_PATTERNS)
      {
         rs.withDeniedWritePattern(CHANNEL_ASSISTANT, pattern);
      }
      return (rs);
   }



   /*******************************************************************************
    ** The instance's registry - or, when nothing registered one, the defaults
    ** (so the assistant wall always applies).
    *******************************************************************************/
   public static ClientChannelsMetaData of(QInstance qInstance)
   {
      ClientChannelsMetaData registered = qInstance == null ? null : QSupplementalInstanceMetaData.of(qInstance, NAME);
      return (registered == null ? DefaultsHolder.DEFAULTS : registered);
   }



   /*******************************************************************************
    ** The instance's registry, adding the defaults first if it has none - for
    ** modules that add channels or deny rules.
    *******************************************************************************/
   public static ClientChannelsMetaData ofOrWithNew(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, ClientChannelsMetaData::defaults));
   }



   /*******************************************************************************
    ** holder for the shared (read-only by convention) defaults
    *******************************************************************************/
   private static class DefaultsHolder
   {
      private static final ClientChannelsMetaData DEFAULTS = defaults();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** The channel name of a channel value (`assistant/turn-42` -> `assistant`).
    *******************************************************************************/
   public static String channelName(String clientChannel)
   {
      return (new QSession().withClientChannel(clientChannel).getClientChannelName());
   }



   /*******************************************************************************
    ** Whether a channel name is allowed (registered).
    *******************************************************************************/
   public boolean isAllowedChannel(String channelName)
   {
      return (channelName != null && channels.containsKey(channelName.toLowerCase()));
   }



   /*******************************************************************************
    ** Whether an object (by kind and name, and its effective permission rules)
    ** is denied to a channel.  A null channel is never denied here.
    *******************************************************************************/
   public boolean isDenied(String channelName, String objectName, QPermissionRules effectiveRules)
   {
      if(channelName == null)
      {
         return (false);
      }

      String channel = channelName.toLowerCase();
      if(effectiveRules != null && effectiveRules.getDeniedClients() != null && effectiveRules.getDeniedClients().stream().anyMatch(c -> c != null && c.equalsIgnoreCase(channel)))
      {
         return (true);
      }

      if(objectName == null)
      {
         return (false);
      }

      if(deniedObjects.getOrDefault(channel, Set.of()).contains(objectName))
      {
         return (true);
      }

      String name = objectName.toLowerCase();
      for(String pattern : deniedPatterns.getOrDefault(channel, List.of()))
      {
         if(globMatches(pattern, name))
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** Whether writes (insert, edit, delete) to an object are denied to a channel
    ** (it may still be read).  A fully denied object is write-denied too.
    *******************************************************************************/
   public boolean isWriteDenied(String channelName, String objectName, QPermissionRules effectiveRules)
   {
      if(channelName == null)
      {
         return (false);
      }
      if(isDenied(channelName, objectName, effectiveRules))
      {
         return (true);
      }
      if(objectName == null)
      {
         return (false);
      }

      String name = objectName.toLowerCase();
      for(String pattern : deniedWritePatterns.getOrDefault(channelName.toLowerCase(), List.of()))
      {
         if(globMatches(pattern, name))
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** Deny a channel writes (insert, edit, delete) to every object whose name
    ** matches a `*` glob - reads stay allowed.
    *******************************************************************************/
   public ClientChannelsMetaData withDeniedWritePattern(String channelName, String pattern)
   {
      List<String> patterns = deniedWritePatterns.computeIfAbsent(channelName.toLowerCase(), k -> new ArrayList<>());
      String       lower    = pattern.toLowerCase();
      if(!patterns.contains(lower))
      {
         patterns.add(lower);
      }
      return (this);
   }



   /*******************************************************************************
    ** Getter for deniedWritePatterns (channel to name globs)
    *******************************************************************************/
   public Map<String, List<String>> getDeniedWritePatterns()
   {
      return (deniedWritePatterns);
   }



   /*******************************************************************************
    ** `*` glob match (case-insensitive; the pattern is lower-cased on entry).
    *******************************************************************************/
   static boolean globMatches(String pattern, String name)
   {
      StringBuilder regex = new StringBuilder();
      for(String part : pattern.split("\\*", -1))
      {
         if(!regex.isEmpty())
         {
            regex.append(".*");
         }
         regex.append(Pattern.quote(part));
      }
      return (name.matches(regex.toString()));
   }



   /*******************************************************************************
    ** Allow a channel.
    *******************************************************************************/
   public ClientChannelsMetaData withChannel(String name, String description)
   {
      channels.put(name.toLowerCase(), description);
      return (this);
   }



   /*******************************************************************************
    ** Deny a channel one object (table, process, report, widget or app) by name.
    *******************************************************************************/
   public ClientChannelsMetaData withDeniedObject(String channelName, String objectName)
   {
      deniedObjects.computeIfAbsent(channelName.toLowerCase(), k -> new LinkedHashSet<>()).add(objectName);
      return (this);
   }



   /*******************************************************************************
    ** Deny a channel every object whose name matches a `*` glob.
    *******************************************************************************/
   public ClientChannelsMetaData withDeniedPattern(String channelName, String pattern)
   {
      List<String> patterns = deniedPatterns.computeIfAbsent(channelName.toLowerCase(), k -> new ArrayList<>());
      String       lower    = pattern.toLowerCase();
      if(!patterns.contains(lower))
      {
         patterns.add(lower);
      }
      return (this);
   }



   /*******************************************************************************
    ** Getter for channels (name to description)
    *******************************************************************************/
   public Map<String, String> getChannels()
   {
      return (channels);
   }



   /*******************************************************************************
    ** Getter for deniedObjects (channel to object names)
    *******************************************************************************/
   public Map<String, Set<String>> getDeniedObjects()
   {
      return (deniedObjects);
   }



   /*******************************************************************************
    ** Getter for deniedPatterns (channel to name globs)
    *******************************************************************************/
   public Map<String, List<String>> getDeniedPatterns()
   {
      return (deniedPatterns);
   }



   /*******************************************************************************
    ** Channel names must be lowercase a-z, 0-9 and '-'; deny rules must name
    ** allowed channels; denied object names should exist.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      for(String channel : channels.keySet())
      {
         validator.assertCondition(CHANNEL_PATTERN.matcher(channel).matches(), "Client channel [" + channel + "] must match " + CHANNEL_PATTERN.pattern());
      }

      for(String channel : deniedPatterns.keySet())
      {
         validator.assertCondition(channels.containsKey(channel), "Client channel deny patterns name an unknown channel [" + channel + "]");
      }

      for(Map.Entry<String, Set<String>> entry : deniedObjects.entrySet())
      {
         validator.assertCondition(channels.containsKey(entry.getKey()), "Client channel denied objects name an unknown channel [" + entry.getKey() + "]");
         for(String objectName : entry.getValue())
         {
            boolean exists = qInstance.getTable(objectName) != null || qInstance.getProcess(objectName) != null || qInstance.getReport(objectName) != null
               || qInstance.getWidget(objectName) != null || qInstance.getApp(objectName) != null;
            if(!exists)
            {
               validator.warn("Client channel [" + entry.getKey() + "] denies [" + objectName + "], which is not a table, process, report, widget or app in the instance");
            }
         }
      }
   }
}
