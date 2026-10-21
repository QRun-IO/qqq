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

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/*******************************************************************************
 ** The origins the Next dashboard's analytics providers need in its
 ** Content-Security-Policy (QRun-IO/qqq#730), from the instance's environment
 ** values (QQQ_ENV_*, the same settings the dashboards read):
 ** - ANALYTICS_PROVIDERS lists the active providers (default: google, posthog);
 ** - Google Analytics 4, when GOOGLE_ANALYTICS_ENABLED is "true" and
 **   GOOGLE_ANALYTICS_TRACKING_ID is set: the gtag.js script origin, and the
 **   origins it sends to;
 ** - PostHog, when POSTHOG_ENABLED is "true" and POSTHOG_API_KEY (or
 **   POSTHOG_PROJECT_API_KEY) is set: the POSTHOG_HOST origin (default
 **   https://us.i.posthog.com) and the origin its script is loaded from;
 ** - the origins of ANALYTICS_PLUGIN_SCRIPTS (or ANALYTICS_PLUGIN_SCRIPT_URLS).
 ** Nothing is added for a provider that is not configured.
 *******************************************************************************/
final class NextDashboardAnalyticsOrigins
{
   static final String GOOGLE_TAG_MANAGER_ORIGIN = "https://www.googletagmanager.com";
   static final String GOOGLE_ANALYTICS_ORIGIN   = "https://*.google-analytics.com";
   static final String GOOGLE_ANALYTICS_ORIGIN_2 = "https://*.analytics.google.com";
   static final String DEFAULT_POSTHOG_HOST      = "https://us.i.posthog.com";

   private static final List<String> DEFAULT_PROVIDERS = List.of("google", "posthog");

   private static final String POSTHOG_CLOUD_DOMAIN = "i.posthog.com";

   private final Set<String> connect = new LinkedHashSet<>();
   private final Set<String> script  = new LinkedHashSet<>();



   /*******************************************************************************
    ** Compute the origins for a set of environment values.
    **
    ** @param environmentValues the instance's environment values (may be null)
    *******************************************************************************/
   NextDashboardAnalyticsOrigins(Map<String, String> environmentValues)
   {
      Map<String, String> values    = environmentValues == null ? Map.of() : environmentValues;
      List<String>        providers = parseList(values.get("ANALYTICS_PROVIDERS")).stream().map(name -> name.toLowerCase(Locale.ROOT)).toList();
      if(providers.isEmpty())
      {
         providers = DEFAULT_PROVIDERS;
      }

      if(providers.contains("google") && "true".equals(values.get("GOOGLE_ANALYTICS_ENABLED")) && !isBlank(values.get("GOOGLE_ANALYTICS_TRACKING_ID")))
      {
         script.add(GOOGLE_TAG_MANAGER_ORIGIN);
         connect.addAll(List.of(GOOGLE_TAG_MANAGER_ORIGIN, GOOGLE_ANALYTICS_ORIGIN, GOOGLE_ANALYTICS_ORIGIN_2));
      }

      if(providers.contains("posthog") && "true".equals(values.get("POSTHOG_ENABLED"))
         && (!isBlank(values.get("POSTHOG_API_KEY")) || !isBlank(values.get("POSTHOG_PROJECT_API_KEY"))))
      {
         String host = isBlank(values.get("POSTHOG_HOST")) ? DEFAULT_POSTHOG_HOST : values.get("POSTHOG_HOST").trim();
         addIfPresent(connect, NextDashboardSecurityHeaders.originOf(host));
         addIfPresent(script, postHogScriptOrigin(host));
      }

      String pluginScripts = isBlank(values.get("ANALYTICS_PLUGIN_SCRIPTS")) ? values.get("ANALYTICS_PLUGIN_SCRIPT_URLS") : values.get("ANALYTICS_PLUGIN_SCRIPTS");
      for(String pluginScript : parseList(pluginScripts))
      {
         addIfPresent(script, NextDashboardSecurityHeaders.originOf(pluginScript));
      }
   }



   /*******************************************************************************
    ** Origins the providers call (connect-src), in order.
    *******************************************************************************/
   Set<String> getConnect()
   {
      return (Collections.unmodifiableSet(connect));
   }



   /*******************************************************************************
    ** Origins the providers' scripts load from (script-src), in order.
    *******************************************************************************/
   Set<String> getScript()
   {
      return (Collections.unmodifiableSet(script));
   }



   /*******************************************************************************
    ** Origin PostHog's array.js is loaded from for an API host, as the
    ** dashboards build it: PostHog Cloud ingestion hosts (i.posthog.com and
    ** {region}.i.posthog.com) serve it from their assets host; any other host
    ** (a reverse proxy or self-hosted PostHog) serves it itself.
    **
    ** @param host the PostHog API host URL
    ** @return the script origin, or null when the host is not an http(s) URL
    *******************************************************************************/
   static String postHogScriptOrigin(String host)
   {
      String origin = NextDashboardSecurityHeaders.originOf(host);
      if(origin == null)
      {
         return (null);
      }

      URI    uri      = URI.create(origin);
      String hostName = uri.getHost();
      String assets;
      if(POSTHOG_CLOUD_DOMAIN.equals(hostName))
      {
         assets = "assets." + POSTHOG_CLOUD_DOMAIN;
      }
      else if(hostName.endsWith("." + POSTHOG_CLOUD_DOMAIN))
      {
         assets = hostName.substring(0, hostName.length() - POSTHOG_CLOUD_DOMAIN.length() - 1) + "-assets." + POSTHOG_CLOUD_DOMAIN;
      }
      else
      {
         return (origin);
      }
      return (uri.getScheme() + "://" + assets + (uri.getPort() == -1 ? "" : ":" + uri.getPort()));
   }



   /*******************************************************************************
    ** Split a list setting on commas, semicolons and line breaks.
    *******************************************************************************/
   private static List<String> parseList(String value)
   {
      if(value == null)
      {
         return (List.of());
      }
      return (Arrays.stream(value.split("[,;\\n\\r]")).map(String::trim).filter(item -> !item.isEmpty()).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean isBlank(String value)
   {
      return (value == null || value.isBlank());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void addIfPresent(Set<String> origins, String origin)
   {
      if(origin != null)
      {
         origins.add(origin);
      }
   }
}
