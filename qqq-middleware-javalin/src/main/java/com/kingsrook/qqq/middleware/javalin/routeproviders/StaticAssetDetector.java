/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility class for detecting whether a request path represents a static asset
 ** versus a client-side SPA route.
 **
 ** PROBLEM:
 ** When serving SPAs with deep linking support, we need to distinguish between:
 ** - 404s for missing static assets (should stay 404)
 ** - 404s for SPA client-side routes (should serve index.html)
 **
 ** SOLUTION:
 ** Use heuristics based on file extensions and common path patterns to make
 ** an educated guess about whether a path is likely a static asset.
 **
 ** LIMITATIONS & CAVEATS:
 ** This class uses heuristics and cannot be 100% accurate because:
 ** 1. False Positives: SPA routes that look like assets (e.g., /learn/javascript.js)
 **    will be incorrectly identified as assets and return 404 instead of serving
 **    the SPA's index.html
 ** 2. False Negatives: Assets without standard extensions (e.g., /assets/logo)
 **    may be incorrectly identified as routes and serve index.html instead of 404
 ** 3. Framework-Specific: Some frameworks use non-standard asset paths that may
 **    not be recognized (e.g., Vite's /@vite/client)
 ** 4. Maintenance: New file formats and web standards require periodic updates
 **
 ** CUSTOMIZATION:
 ** For cases where the default heuristics don't work, you can:
 ** - Add custom extensions: detector.withCustomExtensions(".myext")
 ** - Add custom path patterns: detector.withCustomPathPatterns("/my-assets/")
 ** - Provide custom logic: detector.withCustomDetector(path -> ...)
 **
 ** METRICS:
 ** Tracks detection statistics via incrementCounter() for monitoring and debugging.
 ** Override incrementCounter() to integrate with your metrics system.
 **
 ** THREAD SAFETY:
 ** This class is thread-safe for read operations after configuration.
 ** Configuration methods should only be called during initialization.
 **
 ** @since 0.31.0
 *******************************************************************************/
public class StaticAssetDetector
{
   private static final QLogger LOG = QLogger.getLogger(StaticAssetDetector.class);

   //////////////////////////////
   // Standard file extensions //
   //////////////////////////////
   private static final Set<String> DEFAULT_EXTENSIONS = new HashSet<>(Arrays.asList(
      ////////////////////
      // Code & Scripts //
      ////////////////////
      ".js", ".mjs", ".cjs",           // JavaScript (standard, ES module, CommonJS)
      ".jsx",                          // JSX (React)
      ".ts", ".tsx",                   // TypeScript
      ".css",                          // Stylesheets
      ".scss", ".sass", ".less",       // CSS preprocessors
      ".map",                          // Source maps
      ".wasm",                         // WebAssembly

      ////////////
      // Images //
      ////////////
      ".png", ".jpg", ".jpeg",         // Common formats
      ".gif", ".svg", ".ico",          // Graphics & icons
      ".webp", ".avif",                // Modern formats
      ".bmp", ".tiff",                 // Legacy formats

      ///////////
      // Fonts //
      ///////////
      ".woff", ".woff2",               // Web fonts (standard)
      ".ttf", ".eot", ".otf",          // Alternative font formats

      ////////////////
      // Data Files //
      ////////////////
      ".json", ".xml",                 // Structured data
      ".csv", ".txt",                  // Text data

      ///////////
      // Video //
      ///////////
      ".mp4", ".webm", ".ogg",         // Web video
      ".avi", ".mov",                  // Desktop video

      ///////////
      // Audio //
      ///////////
      ".mp3", ".wav", ".ogg",          // Common audio
      ".m4a", ".flac"                  // High quality audio
   ));

   ////////////////////////////////
   // Standard path patterns     //
   // (directories commonly used //
   // for static assets)         //
   ////////////////////////////////
   private static final List<String> DEFAULT_PATH_PATTERNS = Arrays.asList(
      "/assets/",                      // Common asset directory
      "/static/",                      // Common static directory
      "/public/",                      // Create React App, Next.js
      "/dist/",                        // Build output directory
      "/build/",                       // Build output directory
      "/_next/",                       // Next.js specific
      "/fonts/",                       // Font directory
      "/images/",                      // Image directory
      "/media/"                        // Media directory
   );

   private final Set<String>         extensions;
   private final List<String>        pathPatterns;
   private final List<Predicate<String>> customDetectors;
   private       String              name = "default";


   /*******************************************************************************
    ** Constructor - creates detector with default configuration
    *******************************************************************************/
   public StaticAssetDetector()
   {
      this.extensions = new HashSet<>(DEFAULT_EXTENSIONS);
      this.pathPatterns = new ArrayList<>(DEFAULT_PATH_PATTERNS);
      this.customDetectors = new ArrayList<>();
   }



   /*******************************************************************************
    ** Detect if a request path represents a static asset.
    **
    ** Uses the following logic in order:
    ** 1. Custom detectors (if any configured)
    ** 2. File extension matching (e.g., .js, .css, .png)
    ** 3. Path pattern matching (e.g., /assets/, /static/)
    **
    ** @param path The request path to check (e.g., "/admin/assets/main.js")
    ** @return true if the path appears to be a static asset, false otherwise
    *******************************************************************************/
   public boolean isStaticAsset(String path)
   {
      if(path == null || path.isEmpty())
      {
         return false;
      }

      ////////////////////////////////////////////////////
      // Normalize path: lowercase and strip fragments //
      ////////////////////////////////////////////////////
      String normalizedPath = normalizePath(path);

      ////////////////////////////////
      // Try custom detectors first //
      ////////////////////////////////
      for(Predicate<String> detector : customDetectors)
      {
         try
         {
            if(detector.test(normalizedPath))
            {
               logDetection(path, "custom detector");
               incrementCounter("custom_detector");
               return true;
            }
         }
         catch(Exception e)
         {
            LOG.warn("Custom detector threw exception", e, logPair("path", path));
         }
      }

      //////////////////////////
      // Check file extension //
      //////////////////////////
      for(String ext : extensions)
      {
         if(normalizedPath.endsWith(ext))
         {
            logDetection(path, "extension: " + ext);
            incrementCounter("extension_match");
            return true;
         }
      }

      /////////////////////////
      // Check path patterns //
      /////////////////////////
      for(String pattern : pathPatterns)
      {
         if(normalizedPath.contains(pattern))
         {
            logDetection(path, "path pattern: " + pattern);
            incrementCounter("path_pattern_match");
            return true;
         }
      }

      ////////////////////////
      // Not a static asset //
      ////////////////////////
      LOG.trace("Path not identified as static asset", logPair("path", path));
      incrementCounter("not_asset");
      return false;
   }



   /*******************************************************************************
    ** Normalize path for comparison by:
    ** - Converting to lowercase
    ** - Stripping query parameters (?foo=bar)
    ** - Stripping hash fragments (#section)
    **
    ** @param path The raw request path
    ** @return Normalized path ready for pattern matching
    *******************************************************************************/
   private String normalizePath(String path)
   {
      String normalized = path.toLowerCase();

      ////////////////////////////
      // Strip query parameters //
      ////////////////////////////
      int queryIndex = normalized.indexOf('?');
      if(queryIndex > 0)
      {
         normalized = normalized.substring(0, queryIndex);
      }

      //////////////////////////
      // Strip hash fragments //
      //////////////////////////
      int hashIndex = normalized.indexOf('#');
      if(hashIndex > 0)
      {
         normalized = normalized.substring(0, hashIndex);
      }

      return normalized;
   }



   /*******************************************************************************
    ** Log when a static asset is detected (at DEBUG level)
    **
    ** @param path The original request path
    ** @param reason Why it was detected (e.g., "extension: .js")
    *******************************************************************************/
   private void logDetection(String path, String reason)
   {
      LOG.debug("Static asset detected, allowing natural 404",
         logPair("path", path),
         logPair("reason", reason),
         logPair("detector", name));
   }



   /*******************************************************************************
    ** Increment a counter for metrics tracking.
    **
    ** Override this method to integrate with your metrics system (e.g., Prometheus,
    ** Micrometer, custom metrics).
    **
    ** Default implementation does nothing.
    **
    ** @param counterName Name of the counter to increment
    **                    Values: "custom_detector", "extension_match",
    **                           "path_pattern_match", "not_asset"
    *******************************************************************************/
   protected void incrementCounter(String counterName)
   {
      ////////////////////////////////////////////////////////////////////////////
      // Override this method to integrate with your metrics system             //
      // Example: metricsRegistry.counter("spa.asset.detection." + counterName) //
      ////////////////////////////////////////////////////////////////////////////
   }



   /*******************************************************************************
    ** Fluent setter: Add custom file extensions to detect as static assets
    **
    ** @param customExtensions Extensions to add (e.g., ".myext", ".custom")
    ** @return this for method chaining
    *******************************************************************************/
   public StaticAssetDetector withCustomExtensions(String... customExtensions)
   {
      for(String ext : customExtensions)
      {
         String normalized = ext.toLowerCase();
         if(!normalized.startsWith("."))
         {
            normalized = "." + normalized;
         }
         this.extensions.add(normalized);
      }
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Add custom path patterns to detect as static assets
    **
    ** @param patterns Path patterns to add (e.g., "/my-assets/", "/resources/")
    ** @return this for method chaining
    *******************************************************************************/
   public StaticAssetDetector withCustomPathPatterns(String... patterns)
   {
      this.pathPatterns.addAll(Arrays.asList(patterns));
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Add custom detection logic
    **
    ** Custom detectors are evaluated FIRST, before extension and path matching.
    ** If any custom detector returns true, the path is considered a static asset.
    **
    ** @param detector Predicate that returns true if path is a static asset
    ** @return this for method chaining
    *******************************************************************************/
   public StaticAssetDetector withCustomDetector(Predicate<String> detector)
   {
      this.customDetectors.add(detector);
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Set a name for this detector (used in logging)
    **
    ** @param name Name for this detector instance
    ** @return this for method chaining
    *******************************************************************************/
   public StaticAssetDetector withName(String name)
   {
      this.name = name;
      return this;
   }



   /*******************************************************************************
    ** Get the set of file extensions this detector recognizes
    **
    ** @return Unmodifiable set of extensions (includes defaults + custom)
    *******************************************************************************/
   public Set<String> getExtensions()
   {
      return new HashSet<>(extensions);
   }



   /*******************************************************************************
    ** Get the list of path patterns this detector recognizes
    **
    ** @return Unmodifiable list of patterns (includes defaults + custom)
    *******************************************************************************/
   public List<String> getPathPatterns()
   {
      return new ArrayList<>(pathPatterns);
   }
}
