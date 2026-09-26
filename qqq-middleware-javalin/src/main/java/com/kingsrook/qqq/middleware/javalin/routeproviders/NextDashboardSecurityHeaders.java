/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


/*******************************************************************************
 ** Security response headers for the QQQ Next dashboard (QRun-IO/qqq#695).
 **
 ** The defaults are strict and match what the static export needs:
 ** - Content-Security-Policy: everything from the application's own origin;
 **   inline scripts only by the SHA-256 hash of each script in the served page
 **   (the provider adds these per document); no plugins, no base-URI or form
 **   targets elsewhere, and the dashboard may not be framed. Inline styles are
 **   allowed: the UI's dialog scroll lock, toasts and React style hoisting
 **   create style elements at runtime, and branding customCss is applied as a
 **   style element. Images may also come from any HTTPS origin (branding logos
 **   and image fields hold external URLs).
 ** - X-Frame-Options DENY (for browsers without frame-ancestors).
 ** - Referrer-Policy strict-origin-when-cross-origin: other origins see only the
 **   application origin, never a path or query (such as an authorization code).
 ** - Permissions-Policy: device and sensor features the dashboard never uses
 **   are disabled.
 ** - X-Content-Type-Options nosniff.
 **
 ** NextDashboardRouteProvider adds the configured identity provider origins to
 ** connect-src (OAUTH2 discovery, AUTH_0 token exchange), the QuickSight
 ** origin to frame-src when the instance has a quickSightChart widget, and the
 ** origin of each customComponent widget's componentSourceUrl to script-src.
 ** Applications change the policy with
 ** QApplicationJavalinServer.withNextDashboardSecurityHeadersCustomizer, which
 ** receives this object after those additions.
 *******************************************************************************/
public final class NextDashboardSecurityHeaders
{
   public static final String CONTENT_SECURITY_POLICY             = "Content-Security-Policy";
   public static final String CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only";
   public static final String X_FRAME_OPTIONS                     = "X-Frame-Options";
   public static final String REFERRER_POLICY                     = "Referrer-Policy";
   public static final String PERMISSIONS_POLICY                  = "Permissions-Policy";
   public static final String X_CONTENT_TYPE_OPTIONS              = "X-Content-Type-Options";

   public static final String SCRIPT_SRC = "script-src";

   public static final String DEFAULT_PERMISSIONS_POLICY = String.join(", ",
      "accelerometer=()", "camera=()", "display-capture=()", "geolocation=()", "gyroscope=()", "hid=()",
      "magnetometer=()", "microphone=()", "midi=()", "payment=()", "serial=()", "usb=()");

   private static final Pattern DIRECTIVE_NAME = Pattern.compile("[a-z][a-z0-9-]*");
   private static final Pattern SOURCE_TOKEN   = Pattern.compile("[^\\s;,]+");
   private static final Pattern HEADER_NAME    = Pattern.compile("[A-Za-z0-9-]+");

   private final Map<String, Set<String>> directives = new LinkedHashMap<>();
   private final Map<String, String>      headers    = new LinkedHashMap<>();

   private boolean contentSecurityPolicyEnabled = true;
   private boolean reportOnly;



   /*******************************************************************************
    ** The default (strict) policy and headers.
    *******************************************************************************/
   public NextDashboardSecurityHeaders()
   {
      withDirective("default-src", "'self'");
      withDirective(SCRIPT_SRC, "'self'");
      withDirective("style-src", "'self'", "'unsafe-inline'");
      withDirective("img-src", "'self'", "data:", "blob:", "https:");
      withDirective("font-src", "'self'", "data:");
      withDirective("connect-src", "'self'");
      withDirective("frame-src", "'self'");
      withDirective("worker-src", "'self'", "blob:");
      withDirective("manifest-src", "'self'");
      withDirective("media-src", "'self'", "data:", "blob:");
      withDirective("object-src", "'none'");
      withDirective("base-uri", "'self'");
      withDirective("form-action", "'self'");
      withDirective("frame-ancestors", "'none'");

      withHeader(X_FRAME_OPTIONS, "DENY");
      withHeader(REFERRER_POLICY, "strict-origin-when-cross-origin");
      withHeader(PERMISSIONS_POLICY, DEFAULT_PERMISSIONS_POLICY);
      withHeader(X_CONTENT_TYPE_OPTIONS, "nosniff");
   }



   /*******************************************************************************
    ** The origin (scheme://host[:port]) of a URL, for use as a CSP source.
    **
    ** @param url an absolute http(s) URL, such as an identity provider base URL
    ** @return the origin, or null when the URL is blank, relative or not http(s)
    *******************************************************************************/
   public static String originOf(String url)
   {
      if(url == null || url.isBlank())
      {
         return (null);
      }
      try
      {
         URI    uri    = URI.create(url.trim());
         String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
         if(!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null)
         {
            return (null);
         }
         return (scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (uri.getPort() == -1 ? "" : ":" + uri.getPort()));
      }
      catch(IllegalArgumentException e)
      {
         return (null);
      }
   }



   /*******************************************************************************
    ** Replace a directive's sources (for example withDirective("frame-ancestors",
    ** "'self'") to allow same-origin framing).
    **
    ** @param directive CSP directive name, lowercase
    ** @param sources source expressions; none gives a directive without sources
    ** @return this
    *******************************************************************************/
   public NextDashboardSecurityHeaders withDirective(String directive, String... sources)
   {
      directives.put(checkDirective(directive), checkSources(sources));
      return (this);
   }



   /*******************************************************************************
    ** Add sources to a directive, creating it when absent.
    **
    ** @param directive CSP directive name, lowercase
    ** @param sources source expressions to append (duplicates are ignored)
    ** @return this
    *******************************************************************************/
   public NextDashboardSecurityHeaders withSources(String directive, String... sources)
   {
      directives.computeIfAbsent(checkDirective(directive), name -> new LinkedHashSet<>()).addAll(checkSources(sources));
      return (this);
   }



   /*******************************************************************************
    ** Remove sources from a directive.
    **
    ** @param directive CSP directive name, lowercase
    ** @param sources source expressions to remove
    ** @return this
    *******************************************************************************/
   public NextDashboardSecurityHeaders withoutSources(String directive, String... sources)
   {
      Set<String> existing = directives.get(checkDirective(directive));
      if(existing != null)
      {
         existing.removeAll(List.of(sources));
      }
      return (this);
   }



   /*******************************************************************************
    ** Remove a directive from the policy.
    **
    ** @param directive CSP directive name, lowercase
    ** @return this
    *******************************************************************************/
   public NextDashboardSecurityHeaders withoutDirective(String directive)
   {
      directives.remove(checkDirective(directive));
      return (this);
   }



   /*******************************************************************************
    ** Set a response header sent with every dashboard file, or remove it.
    **
    ** @param name header name, such as Referrer-Policy
    ** @param value the value, or null to not send the header
    ** @return this
    *******************************************************************************/
   public NextDashboardSecurityHeaders withHeader(String name, String value)
   {
      if(name == null || !HEADER_NAME.matcher(name).matches()
         || CONTENT_SECURITY_POLICY.equalsIgnoreCase(name) || CONTENT_SECURITY_POLICY_REPORT_ONLY.equalsIgnoreCase(name))
      {
         throw (new IllegalArgumentException("Invalid header name (set the Content-Security-Policy through its directives): " + name));
      }
      headers.keySet().removeIf(existing -> existing.equalsIgnoreCase(name));
      if(value != null)
      {
         if(value.contains("\r") || value.contains("\n"))
         {
            throw (new IllegalArgumentException("Header values may not contain line breaks: " + name));
         }
         headers.put(name, value);
      }
      return (this);
   }



   /*******************************************************************************
    ** Send the Content-Security-Policy (default) or not at all.
    **
    ** @param enabled false to send no policy
    ** @return this
    *******************************************************************************/
   public NextDashboardSecurityHeaders withContentSecurityPolicyEnabled(boolean enabled)
   {
      this.contentSecurityPolicyEnabled = enabled;
      return (this);
   }



   /*******************************************************************************
    ** Send the policy as Content-Security-Policy-Report-Only, for trying out a
    ** changed policy without enforcing it.
    **
    ** @param reportOnly true to report violations instead of blocking
    ** @return this
    *******************************************************************************/
   public NextDashboardSecurityHeaders withReportOnly(boolean reportOnly)
   {
      this.reportOnly = reportOnly;
      return (this);
   }



   /*******************************************************************************
    ** The sources of a directive, in order (empty when the directive is absent).
    *******************************************************************************/
   public Set<String> getSources(String directive)
   {
      return (Collections.unmodifiableSet(directives.getOrDefault(directive, Set.of())));
   }



   /*******************************************************************************
    ** The non-CSP headers, in order.
    *******************************************************************************/
   public Map<String, String> getHeaders()
   {
      return (Collections.unmodifiableMap(headers));
   }



   /*******************************************************************************
    ** Name of the policy header: Content-Security-Policy, the report-only variant,
    ** or null when the policy is disabled.
    *******************************************************************************/
   public String getContentSecurityPolicyHeaderName()
   {
      if(!contentSecurityPolicyEnabled || directives.isEmpty())
      {
         return (null);
      }
      return (reportOnly ? CONTENT_SECURITY_POLICY_REPORT_ONLY : CONTENT_SECURITY_POLICY);
   }



   /*******************************************************************************
    ** The policy for one document, with the hashes of its inline scripts added to
    ** script-src.
    **
    ** @param scriptHashes source expressions such as 'sha256-...'
    ** @return the header value
    *******************************************************************************/
   public String buildContentSecurityPolicy(Collection<String> scriptHashes)
   {
      List<String> parts = new ArrayList<>();
      for(Map.Entry<String, Set<String>> entry : directives.entrySet())
      {
         Set<String> sources = new LinkedHashSet<>(entry.getValue());
         if(SCRIPT_SRC.equals(entry.getKey()) && scriptHashes != null)
         {
            sources.addAll(scriptHashes);
         }
         parts.add(sources.isEmpty() ? entry.getKey() : entry.getKey() + " " + String.join(" ", sources));
      }
      return (String.join("; ", parts));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String checkDirective(String directive)
   {
      if(directive == null || !DIRECTIVE_NAME.matcher(directive).matches())
      {
         throw (new IllegalArgumentException("Invalid Content-Security-Policy directive name: " + directive));
      }
      return (directive);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Set<String> checkSources(String... sources)
   {
      Set<String> checked = new LinkedHashSet<>();
      for(String source : sources)
      {
         if(source == null || !SOURCE_TOKEN.matcher(source).matches())
         {
            throw (new IllegalArgumentException("Invalid Content-Security-Policy source expression: " + source));
         }
         checked.add(source);
      }
      return (checked);
   }
}
