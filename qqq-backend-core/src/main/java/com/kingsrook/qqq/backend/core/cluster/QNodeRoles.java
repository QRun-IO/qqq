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

package com.kingsrook.qqq.backend.core.cluster;


import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** What this node of a multi-node application does (docs/specs/
 ** 2026-10-15-cluster-ha-design.md, section A). Core stays orchestrator-agnostic:
 ** roles come from the environment (or system properties), read once.
 **
 ** QQQ_NODE_ROLES (qqq.node.roles): comma list of web, worker, scheduler, esb, or
 ** "all" (the default - a single node does everything, as before). Explicit
 ** per-feature flags win over the role list:
 ** - QQQ_HTTP_ENABLED (qqq.http.enabled): serve application routes (probes and
 **   metrics are always served);
 ** - QQQ_SCHEDULE_MANAGER_ENABLED (qqq.scheduleManager.enabled): start schedulers;
 ** - QQQ_ESB_TRIGGERS_ENABLED (qqq.esb.triggers.enabled): run ESB trigger consumers;
 ** - QQQ_JOBS_WORKER_THREADS (qqq.jobs.workerThreads): durable job worker threads
 **   (without it, a node without the worker role runs 0).
 ** The node id is QQQ_NODE_ID, else HOSTNAME (the pod name), else the host name.
 *******************************************************************************/
public final class QNodeRoles
{
   private static final QLogger LOG = QLogger.getLogger(QNodeRoles.class);

   public static final String ENV_ROLES          = "QQQ_NODE_ROLES";
   public static final String ENV_HTTP           = "QQQ_HTTP_ENABLED";
   public static final String ENV_SCHEDULER      = "QQQ_SCHEDULE_MANAGER_ENABLED";
   public static final String ENV_ESB_TRIGGERS   = "QQQ_ESB_TRIGGERS_ENABLED";
   public static final String ENV_WORKER_THREADS = "QQQ_JOBS_WORKER_THREADS";
   public static final String ENV_NODE_ID        = "QQQ_NODE_ID";



   /*******************************************************************************
    ** A node role.
    *******************************************************************************/
   public enum Role
   {
      WEB, WORKER, SCHEDULER, ESB
   }



   private static volatile Settings settings;



   /*******************************************************************************
    ** The resolved settings of this node.
    **
    ** @param roles the node's roles
    ** @param http serve application routes
    ** @param scheduler start schedulers
    ** @param esbTriggers run ESB trigger consumers
    ** @param workerThreads durable job worker threads; null means the instance's setting
    ** @param nodeId this node's id
    *******************************************************************************/
   public record Settings(Set<Role> roles, boolean http, boolean scheduler, boolean esbTriggers, Integer workerThreads, String nodeId)
   {
   }



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private QNodeRoles()
   {
   }



   /*******************************************************************************
    ** This node's settings, resolved on first use.
    *******************************************************************************/
   public static Settings get()
   {
      Settings current = settings;
      if(current == null)
      {
         synchronized(QNodeRoles.class)
         {
            if(settings == null)
            {
               settings = resolve(new QMetaDataVariableInterpreter());
               LOG.info("Node roles", logPair("nodeId", settings.nodeId()), logPair("roles", settings.roles()), logPair("http", settings.http()),
                  logPair("scheduler", settings.scheduler()), logPair("esbTriggers", settings.esbTriggers()), logPair("workerThreads", settings.workerThreads()));
            }
            current = settings;
         }
      }
      return (current);
   }



   /*******************************************************************************
    ** Replace the settings (tests, or an app that assigns roles itself before the
    ** server starts). Null re-reads the environment on next use.
    *******************************************************************************/
   public static void set(Settings newSettings)
   {
      settings = newSettings;
   }



   /*******************************************************************************
    ** Resolve from system properties and environment.
    *******************************************************************************/
   static Settings resolve(QMetaDataVariableInterpreter interpreter)
   {
      Set<Role> roles = parseRoles(interpreter.getStringFromPropertyOrEnvironment("qqq.node.roles", ENV_ROLES, "all"));

      boolean http        = interpreter.getBooleanFromPropertyOrEnvironment("qqq.http.enabled", ENV_HTTP, roles.contains(Role.WEB));
      boolean scheduler   = interpreter.getBooleanFromPropertyOrEnvironment("qqq.scheduleManager.enabled", ENV_SCHEDULER, roles.contains(Role.SCHEDULER));
      boolean esbTriggers = interpreter.getBooleanFromPropertyOrEnvironment("qqq.esb.triggers.enabled", ENV_ESB_TRIGGERS, roles.contains(Role.ESB));

      Integer workerThreads = null;
      String  threadsString = interpreter.getStringFromPropertyOrEnvironment("qqq.jobs.workerThreads", ENV_WORKER_THREADS, null);
      if(StringUtils.hasContent(threadsString))
      {
         try
         {
            workerThreads = Math.max(0, Integer.parseInt(threadsString.trim()));
         }
         catch(NumberFormatException e)
         {
            LOG.warn("Ignoring a non-numeric durable job worker thread count", logPair("value", threadsString));
         }
      }
      if(workerThreads == null && !roles.contains(Role.WORKER))
      {
         workerThreads = 0;
      }

      String nodeId = interpreter.getStringFromPropertyOrEnvironment("qqq.node.id", ENV_NODE_ID, null);
      if(!StringUtils.hasContent(nodeId))
      {
         nodeId = interpreter.getStringFromPropertyOrEnvironment("qqq.hostname", "HOSTNAME", null);
      }
      if(!StringUtils.hasContent(nodeId))
      {
         nodeId = localHostName();
      }

      return (new Settings(Collections.unmodifiableSet(roles), http, scheduler, esbTriggers, workerThreads, nodeId));
   }



   /*******************************************************************************
    ** Parse a role list; "all" (or blank) is every role. Unknown names are logged
    ** and ignored.
    *******************************************************************************/
   static Set<Role> parseRoles(String value)
   {
      if(!StringUtils.hasContent(value) || "all".equalsIgnoreCase(value.trim()))
      {
         return (EnumSet.allOf(Role.class));
      }

      EnumSet<Role> roles = EnumSet.noneOf(Role.class);
      for(String name : value.split(","))
      {
         String trimmed = name.trim().toUpperCase(Locale.ROOT);
         if(trimmed.isEmpty())
         {
            continue;
         }
         if("ALL".equals(trimmed))
         {
            return (EnumSet.allOf(Role.class));
         }
         try
         {
            roles.add(Role.valueOf(trimmed));
         }
         catch(IllegalArgumentException e)
         {
            LOG.warn("Ignoring an unknown node role", logPair("role", name), logPair("known", Arrays.stream(Role.values()).map(Enum::name).collect(Collectors.joining(","))));
         }
      }
      return (roles);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String localHostName()
   {
      try
      {
         return (InetAddress.getLocalHost().getHostName());
      }
      catch(Exception e)
      {
         return ("unknown-node");
      }
   }



   /*******************************************************************************
    ** Shorthand: this node's id.
    *******************************************************************************/
   public static String nodeId()
   {
      return (get().nodeId());
   }



   /*******************************************************************************
    ** Shorthand: whether this node has the role.
    *******************************************************************************/
   public static boolean has(Role role)
   {
      return (get().roles().contains(role));
   }
}
