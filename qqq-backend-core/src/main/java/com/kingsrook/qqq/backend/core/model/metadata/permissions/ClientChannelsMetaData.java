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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.actions.ai.exposure.AiExposureChannelPolicy;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessChannelPolicyInterface;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
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
   public static final String CHANNEL_TOKEN              = "token";

   /***************************************************************************
    ** core's audit tables (audit, auditDetail, ...): denied to the assistant by
    ** default; withAssistantAuditReads() makes them readable, never writable
    ***************************************************************************/
   public static final String AUDIT_PATTERN = "audit*";

   /***************************************************************************
    ** the assistant's generic default deny patterns - security surfaces any
    ** application may have, whoever defines them (lower-cased globs)
    ***************************************************************************/
   public static final List<String> ASSISTANT_GENERIC_DENIED_PATTERNS = List.of(
      "*api_key*", "*apikey*",
      "*credential*",
      "*secret*",
      "*password*",
      "*permission*", "role", "roles", "*_role", "*_roles", "*_role_*", "user_role*", "userrole*",
      "*user_session*", "*usersession*",
      AUDIT_PATTERN);  // the audit trail: hidden unless the host opts in (withAssistantAuditReads)

   /***************************************************************************
    ** TRANSITIONAL: qbit-specific assistant denies, kept in core only until each
    ** qbit's producer registers its own rule (QRun-IO/qqq#1157) - each qbit owns
    ** its wall from its next release; these are removed at 4.1.0 GA.  They stay
    ** here meanwhile so the wall never opens (fail-closed) for an application
    ** still on an older qbit.
    ***************************************************************************/
   public static final List<String> ASSISTANT_TRANSITIONAL_QBIT_DENIED_PATTERNS = List.of(
      "assistant_*",   // qbit-assistant: providers, models, prices, profiles, skills, policy, budgets, usage, tool-call logs, conversations
      "authentik_*",   // qbit-authentik: identity, permissions and policies
      "webhooks_*",    // qbit-webhooks: endpoints and their signing secrets
      "worm_audit_*"); // qbit-worm-audit: the audit chain, checkpoints, sealed exports and purges

   /***************************************************************************
    ** every assistant deny pattern applied by default (generic + transitional)
    ***************************************************************************/
   public static final List<String> ASSISTANT_DEFAULT_DENIED_PATTERNS = Stream.concat(ASSISTANT_TRANSITIONAL_QBIT_DENIED_PATTERNS.stream(), ASSISTANT_GENERIC_DENIED_PATTERNS.stream()).toList();

   /***************************************************************************
    ** table-name globs that look sensitive (configuration, policy, secrets,
    ** tokens): the validator warns when one is not denied to the assistant
    ***************************************************************************/
   public static final List<String> SENSITIVE_TABLE_PATTERNS = List.of(
      "*_policy", "*_policies", "*_policy_*", "*_config", "*_configs", "*_config_*", "*_configuration*", "*_settings", "*_setting",
      "*_secret*", "*_token", "*_tokens", "*_token_*", "*_key", "*_keys");

   /***************************************************************************
    ** the assistant's default write-deny patterns: never insertable, editable
    ** or deletable - no tampering - even when a host opts in to reading them
    ** (withAssistantAuditReads)
    ***************************************************************************/
   public static final List<String> ASSISTANT_DEFAULT_WRITE_DENIED_PATTERNS = List.of(AUDIT_PATTERN);

   private static final Pattern CHANNEL_PATTERN = Pattern.compile("[a-z0-9][a-z0-9-]*");

   private Map<String, QCodeReference> fieldAccessPolicies = new LinkedHashMap<>();
   private transient Map<String, FieldAccessChannelPolicyInterface> fieldAccessPolicyInstances = new LinkedHashMap<>();
   private static final FieldAccessChannelPolicyInterface ASSISTANT_FIELD_POLICY = new AiExposureChannelPolicy();

   private Map<String, String>       channels       = new LinkedHashMap<>();
   private Map<String, Set<String>>  deniedObjects  = new LinkedHashMap<>();
   private Map<String, List<String>> deniedPatterns = new LinkedHashMap<>();
   private Map<String, List<String>> deniedWritePatterns = new LinkedHashMap<>();
   private Map<String, Set<String>>  reviewedObjects     = new LinkedHashMap<>();



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
         .withChannel(CHANNEL_API, "Another program calling the UI API")
         .withChannel(CHANNEL_TOKEN, "A scoped access token (token:<id>), set by the server from the credential");
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
    ** Tables whose names look sensitive (SENSITIVE_TABLE_PATTERNS) but that no
    ** rule denies to the assistant - each should either be walled by its qbit
    ** (withDeniedObject / withDeniedPattern / QPermissionRules.withDeniedClients)
    ** or be reviewed as safe (withReviewedObject).
    *******************************************************************************/
   public static List<String> findUnwalledSensitiveTables(QInstance qInstance)
   {
      List<String> rs = new ArrayList<>();
      if(qInstance == null || qInstance.getTables() == null)
      {
         return (rs);
      }

      for(QTableMetaData table : qInstance.getTables().values())
      {
         if(table == null || table.getName() == null)
         {
            continue;
         }
         String name = table.getName().toLowerCase();
         if(SENSITIVE_TABLE_PATTERNS.stream().noneMatch(pattern -> globMatches(pattern, name)))
         {
            continue;
         }
         ClientChannelsMetaData registry = of(qInstance);
         if(registry.reviewedObjects.getOrDefault(CHANNEL_ASSISTANT, Set.of()).contains(table.getName()))
         {
            continue;
         }
         if(!registry.isDenied(CHANNEL_ASSISTANT, table.getName(), table.getPermissionRules()))
         {
            rs.add(table.getName());
         }
      }
      return (rs);
   }



   /*******************************************************************************
    ** Validator warnings for findUnwalledSensitiveTables (called for every
    ** instance, whether or not it registers a ClientChannelsMetaData).
    *******************************************************************************/
   public static void warnUnwalledSensitiveTables(QInstance qInstance, QInstanceValidator validator)
   {
      for(String tableName : findUnwalledSensitiveTables(qInstance))
      {
         validator.warn("Table [" + tableName + "] looks sensitive (configuration, policy, secret, token or key) but no client-channel rule denies it to the assistant.  Its qbit should register one (ClientChannelsMetaData.withDeniedObject / withDeniedPattern, or QPermissionRules.withDeniedClients) - or mark it reviewed with withReviewedObject(\"assistant\", name).");
      }
   }



   /*******************************************************************************
    ** Record that an object was reviewed as safe for a channel (silences the
    ** sensitive-table warning; grants nothing).
    *******************************************************************************/
   public ClientChannelsMetaData withReviewedObject(String channelName, String objectName)
   {
      reviewedObjects.computeIfAbsent(channelName.toLowerCase(), k -> new LinkedHashSet<>()).add(objectName);
      return (this);
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
    ** Host opt-in: let the assistant READ the audit tables (audit*), e.g. for a
    ** record-history tool.  They stay write-denied (no tampering), and the
    ** user's own audit permissions still apply.  By default the assistant
    ** cannot see audits at all.  Call it on the instance's own registry
    ** (ofOrWithNew), never on the shared defaults.
    *******************************************************************************/
   public ClientChannelsMetaData withAssistantAuditReads()
   {
      List<String> patterns = deniedPatterns.get(CHANNEL_ASSISTANT);
      if(patterns != null)
      {
         patterns.remove(AUDIT_PATTERN);
      }
      withDeniedWritePattern(CHANNEL_ASSISTANT, AUDIT_PATTERN);
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
    ** Register a channel policy by code reference. Invalid policies fail closed.
    *******************************************************************************/
   public ClientChannelsMetaData withFieldAccessPolicy(String channelName, QCodeReference policy)
   {
      String channel = channelName.toLowerCase(Locale.ROOT);
      fieldAccessPolicies.put(channel, Objects.requireNonNull(policy));
      if(fieldAccessPolicyInstances != null)
      {
         fieldAccessPolicyInstances.remove(channel);
      }
      return (this);
   }



   /*******************************************************************************
    ** Register a channel policy instance.
    *******************************************************************************/
   public ClientChannelsMetaData withFieldAccessPolicy(String channelName, FieldAccessChannelPolicyInterface policy)
   {
      String channel = channelName.toLowerCase(Locale.ROOT);
      if(fieldAccessPolicyInstances == null)
      {
         fieldAccessPolicyInstances = new LinkedHashMap<>();
      }
      fieldAccessPolicyInstances.put(channel, Objects.requireNonNull(policy));
      fieldAccessPolicies.remove(channel);
      return (this);
   }



   /*******************************************************************************
    ** Resolve a policy once, never its per-session decisions. The assistant has
    ** a default even when the instance has registered no channel metadata.
    *******************************************************************************/
   public synchronized FieldAccessChannelPolicyInterface getFieldAccessPolicy(String channelName)
   {
      if(channelName == null)
      {
         return (null);
      }
      String channel = channelName.toLowerCase(Locale.ROOT);
      if(fieldAccessPolicyInstances == null)
      {
         fieldAccessPolicyInstances = new LinkedHashMap<>();
      }
      if(fieldAccessPolicies.containsKey(channel) && !fieldAccessPolicyInstances.containsKey(channel))
      {
         fieldAccessPolicyInstances.put(channel, Objects.requireNonNull(QCodeLoader.getAdHoc(FieldAccessChannelPolicyInterface.class, fieldAccessPolicies.get(channel))));
      }
      return (fieldAccessPolicyInstances.getOrDefault(channel, CHANNEL_ASSISTANT.equals(channel) ? ASSISTANT_FIELD_POLICY : null));
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
      for(Map.Entry<String, QCodeReference> entry : fieldAccessPolicies.entrySet())
      {
         validator.assertCondition(channels.containsKey(entry.getKey()), "Field access policy names an unknown channel [" + entry.getKey() + "]");
         validator.validateSimpleCodeReference("Client channel field access policy: ", entry.getValue(), FieldAccessChannelPolicyInterface.class);
      }
      for(String channel : fieldAccessPolicyInstances == null ? Set.<String>of() : fieldAccessPolicyInstances.keySet())
      {
         validator.assertCondition(channels.containsKey(channel), "Field access policy names an unknown channel [" + channel + "]");
      }
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
