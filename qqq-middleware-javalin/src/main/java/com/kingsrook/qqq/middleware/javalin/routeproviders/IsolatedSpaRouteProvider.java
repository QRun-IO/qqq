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


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import com.kingsrook.qqq.middleware.javalin.routeproviders.authentication.RouteAuthenticatorInterface;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import io.javalin.http.staticfiles.StaticFileConfig;
import org.apache.commons.io.IOUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Complete isolated SPA route provider with deep linking support.
 **
 ** This provider offers full SPA functionality including:
 ** - Static file serving from classpath or filesystem
 ** - Deep linking support (404 -> index.html fallback)
 ** - Path-scoped before/after handlers
 ** - Complete isolation from other SPAs
 ** - Support for root path ("/") with exclusions for other SPAs
 **
 ** Usage Examples:
 **
 ** // Admin SPA at /admin
 ** new IsolatedSpaRouteProvider("/admin", "admin-spa/dist/")
 **    .withSpaIndexFile("admin-spa/dist/index.html")
 **    .withAuthenticator(new QCodeReference(AdminAuthenticator.class))
 **
 ** // Root SPA at / (excluding other SPAs)
 ** new IsolatedSpaRouteProvider("/", "public-site/")
 **    .withSpaIndexFile("public-site/index.html")
 **    .withExcludedPaths(List.of("/admin", "/api"))
 **
 *******************************************************************************/
public class IsolatedSpaRouteProvider implements QJavalinRouteProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(IsolatedSpaRouteProvider.class);

   private final String spaPath;
   private final String staticFilesPath;

   private String              spaIndexFile;
   private QCodeReference      authenticator;
   private QInstance           qInstance;
   private List<String>        excludedPaths     = new ArrayList<>();
   private List<Handler>       beforeHandlers    = new ArrayList<>();
   private List<Handler>       afterHandlers     = new ArrayList<>();
   private boolean             loadFromJar       = false;
   private boolean             enableDeepLinking = true;
   private StaticAssetDetector staticAssetDetector;



   /*******************************************************************************
    ** Constructor
    **
    ** @param spaPath Path where the SPA is hosted (e.g., "/", "/admin", "/customer")
    ** @param staticFilesPath Path to static files (classpath or filesystem)
    *******************************************************************************/
   public IsolatedSpaRouteProvider(String spaPath, String staticFilesPath)
   {
      this.spaPath = normalizePath(spaPath);
      this.staticFilesPath = staticFilesPath;
      this.staticAssetDetector = new StaticAssetDetector().withName("spa:" + spaPath);

      //////////////////////////////////////////////////////////////
      // Check system property for loading files from JAR vs file //
      //////////////////////////////////////////////////////////////
      try
      {
         String propertyValue = System.getProperty("qqq.javalin.enableStaticFilesFromJar", "false");
         this.loadFromJar = "true".equals(propertyValue);
      }
      catch(Exception e)
      {
         LOG.warn("Error reading system property", e);
      }
   }



   /*******************************************************************************
    ** Check if a request path is under a given path prefix.
    **
    ** This is the correct way to check path prefixes with boundary checking
    ** to prevent false matches like "/administrator" matching "/admin".
    **
    ** Handles query parameters and hash fragments by stripping them before
    ** comparison, so "/admin?tab=users" correctly matches prefix "/admin".
    **
    ** SPECIAL CASE - ROOT PATH:
    ** The root path "/" matches everything when used as a prefix. This allows
    ** root-level exclusions or catch-all behaviors.
    **
    ** RULES:
    ** 1. Exact match: "/admin" matches "/admin"
    ** 2. Sub-path: "/admin/users" matches prefix "/admin"
    ** 3. Boundary check: "/administrator" does NOT match prefix "/admin"
    ** 4. Query params ignored: "/admin?tab=users" matches prefix "/admin"
    ** 5. Root matches all: anything matches prefix "/"
    **
    ** This method is used by both exclusion checking and path matching to ensure
    ** consistent behavior across the codebase.
    **
    ** Examples:
    **   isPathUnderPrefix("/admin", "/admin")              → TRUE (exact)
    **   isPathUnderPrefix("/admin/users", "/admin")        → TRUE (sub-path)
    **   isPathUnderPrefix("/admin?tab=users", "/admin")    → TRUE (query param ignored)
    **   isPathUnderPrefix("/administrator", "/admin")      → FALSE (different path)
    **   isPathUnderPrefix("/api-docs", "/api")             → FALSE (different path)
    **   isPathUnderPrefix("/anything", "/")                → TRUE (root matches all)
    **
    ** @param requestPath The request path to check (may include query params/hash)
    ** @param pathPrefix The path prefix to match against
    ** @return true if requestPath is equal to or under pathPrefix
    *******************************************************************************/
   private static boolean isPathUnderPrefix(String requestPath, String pathPrefix)
   {
      ////////////////////////////////////////////////
      // Special case: root path matches everything //
      ////////////////////////////////////////////////
      if("/".equals(pathPrefix))
      {
         return true;
      }

      ////////////////////////////////////////////////
      // Normalize request path: strip query params //
      // and hash fragments for comparison          //
      ////////////////////////////////////////////////
      String normalizedPath = requestPath;
      int    queryIndex     = normalizedPath.indexOf('?');
      if(queryIndex != -1)
      {
         normalizedPath = normalizedPath.substring(0, queryIndex);
      }
      int hashIndex = normalizedPath.indexOf('#');
      if(hashIndex != -1)
      {
         normalizedPath = normalizedPath.substring(0, hashIndex);
      }

      //////////////////////
      // Exact match case //
      //////////////////////
      if(normalizedPath.equals(pathPrefix))
      {
         return true;
      }

      ////////////////////////////////////////////////////////////
      // Sub-path match with boundary check                     //
      // Ensures "/administrator" doesn't match prefix "/admin" //
      ////////////////////////////////////////////////////////////
      return normalizedPath.startsWith(pathPrefix + "/");
   }



   /*******************************************************************************
    ** Normalize path to ensure it starts with / and doesn't end with /
    ** (except for root path which stays as "/")
    *******************************************************************************/
   private String normalizePath(String path)
   {
      if(path == null || path.isEmpty())
      {
         return "/";
      }

      if(!path.startsWith("/"))
      {
         path = "/" + path;
      }

      if(path.length() > 1 && path.endsWith("/"))
      {
         path = path.substring(0, path.length() - 1);
      }

      return path;
   }



   /*******************************************************************************
    ** Set the QInstance (required by interface)
    *******************************************************************************/
   @Override
   public void setQInstance(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    ** Configure static file serving during Javalin config phase
    *******************************************************************************/
   @Override
   public void acceptJavalinConfig(JavalinConfig config)
   {
      config.staticFiles.add(this::configureStaticFiles);
   }



   /*******************************************************************************
    ** Configure static files - called by Javalin during initialization
    *******************************************************************************/
   private void configureStaticFiles(StaticFileConfig staticFileConfig)
   {
      if(loadFromJar)
      {
         ////////////////////////////////
         // Load from classpath in JAR //
         ////////////////////////////////
         staticFileConfig.directory = staticFilesPath;
         staticFileConfig.hostedPath = spaPath;
         staticFileConfig.location = Location.CLASSPATH;
         LOG.info("Configuring SPA static files from classpath",
            logPair("spaPath", spaPath),
            logPair("directory", staticFilesPath),
            logPair("location", "CLASSPATH"));
      }
      else
      {
         ////////////////////////////////////
         // Load from filesystem (for dev) //
         ////////////////////////////////////
         URL resource = getClass().getClassLoader().getResource(staticFilesPath);
         if(resource == null)
         {
            String message = "Could not find file system path: " + staticFilesPath;
            if(staticFilesPath.startsWith("/") && getClass().getClassLoader().getResource(staticFilesPath.replaceFirst("^/+", "")) != null)
            {
               message += ". For non-absolute paths, do not prefix with a leading slash.";
            }
            throw new RuntimeException(message);
         }

         staticFileConfig.directory = resource.getFile();
         staticFileConfig.hostedPath = spaPath;
         staticFileConfig.location = Location.EXTERNAL;
         LOG.info("Configuring SPA static files from filesystem",
            logPair("spaPath", spaPath),
            logPair("directory", staticFileConfig.directory),
            logPair("location", "EXTERNAL"));
      }
   }



   /*******************************************************************************
    ** Register path-scoped handlers after Javalin service is created
    *******************************************************************************/
   @Override
   public void acceptJavalinService(Javalin service)
   {
      //////////////////////////////////////////////////////////
      // Ensure global 404 handler is registered with Javalin //
      // (Safe to call multiple times - only registers once)  //
      //////////////////////////////////////////////////////////
      SpaNotFoundHandlerRegistry.getInstance().registerGlobalHandler(service);

      if("/".equals(spaPath))
      {
         /////////////////////////////////////////
         // Special handling for root path SPAs //
         // Need to exclude other SPA paths     //
         /////////////////////////////////////////
         registerRootSpaHandlers(service);
      }
      else
      {
         ///////////////////////////////////
         // Standard path-scoped SPA      //
         // Everything is nicely isolated //
         ///////////////////////////////////
         registerPathScopedHandlers(service);
      }
   }



   /*******************************************************************************
    ** Register handlers for a path-scoped SPA (e.g., /admin, /customer)
    ** Everything is automatically isolated within the path prefix
    *******************************************************************************/
   private void registerPathScopedHandlers(Javalin service)
   {
      LOG.info("Registering path-scoped SPA handlers",
         logPair("spaPath", spaPath),
         logPair("enableDeepLinking", enableDeepLinking));

      ////////////////////////////////////////////////////
      // Register before handlers (auth, logging, etc.) //
      ////////////////////////////////////////////////////
      String pathPattern = spaPath + "/*";
      for(Handler handler : beforeHandlers)
      {
         service.before(pathPattern, handler);
      }

      ///////////////////////////////////
      // Built-in authentication check //
      ///////////////////////////////////
      if(authenticator != null)
      {
         service.before(pathPattern, this::authenticateRequest);
      }

      /////////////////////////////
      // Register after handlers //
      /////////////////////////////
      for(Handler handler : afterHandlers)
      {
         service.after(pathPattern, handler);
      }

      /////////////////////////////////////////////////////////////////
      // Register 404 handler for deep linking support               //
      // Uses centralized registry to avoid global handler conflicts //
      /////////////////////////////////////////////////////////////////
      if(enableDeepLinking && StringUtils.hasContent(spaIndexFile))
      {
         SpaNotFoundHandlerRegistry.getInstance().registerSpaHandler(spaPath, ctx -> handleNotFound(ctx, false));
      }
   }



   /*******************************************************************************
    ** Register handlers for root path SPA ("/")
    ** More complex because we need to exclude other SPA paths
    *******************************************************************************/
   private void registerRootSpaHandlers(Javalin service)
   {
      LOG.info("Registering root SPA handlers",
         logPair("spaPath", spaPath),
         logPair("excludedPaths", excludedPaths),
         logPair("enableDeepLinking", enableDeepLinking));

      ////////////////////////////////////////////////
      // Register before handlers (with exclusions) //
      ////////////////////////////////////////////////
      for(Handler handler : beforeHandlers)
      {
         service.before("/*", ctx ->
         {
            if(!isExcludedPath(ctx.path()))
            {
               handler.handle(ctx);
            }
         });
      }

      ///////////////////////////////////
      // Built-in authentication check //
      ///////////////////////////////////
      if(authenticator != null)
      {
         service.before("/*", ctx ->
         {
            if(!isExcludedPath(ctx.path()))
            {
               authenticateRequest(ctx);
            }
         });
      }

      /////////////////////////////
      // Register after handlers //
      /////////////////////////////
      for(Handler handler : afterHandlers)
      {
         service.after("/*", ctx ->
         {
            if(!isExcludedPath(ctx.path()))
            {
               handler.handle(ctx);
            }
         });
      }

      /////////////////////////////////////////////////////////////////
      // Register 404 handler for deep linking support               //
      // Uses centralized registry to avoid global handler conflicts //
      /////////////////////////////////////////////////////////////////
      if(enableDeepLinking && StringUtils.hasContent(spaIndexFile))
      {
         SpaNotFoundHandlerRegistry.getInstance().registerSpaHandler(spaPath, ctx -> handleNotFound(ctx, true));
      }
   }



   /*******************************************************************************
    ** Handle 404 errors - serve SPA index.html for deep linking
    **
    ** @param ctx Javalin context
    ** @param checkExclusions Whether to check excluded paths (for root SPA)
    *******************************************************************************/
   private void handleNotFound(Context ctx, boolean checkExclusions)
   {
      String requestPath = ctx.path();

      ////////////////////////////////////////////////////////////
      // For root SPAs, skip if path is excluded (e.g., /admin) //
      ////////////////////////////////////////////////////////////
      if(checkExclusions && isExcludedPath(requestPath))
      {
         LOG.debug("404 on excluded path, not serving SPA index", logPair("path", requestPath));
         return;
      }

      ///////////////////////////////////////////////////////////
      // For path-scoped SPAs, only handle 404s under our path //
      ///////////////////////////////////////////////////////////
      if(!checkExclusions && !"/".equals(spaPath) && !requestPath.startsWith(spaPath))
      {
         LOG.debug("404 not under SPA path, not serving index",
            logPair("path", requestPath),
            logPair("spaPath", spaPath));
         return;
      }

      //////////////////////////////////////////////////////
      // Don't serve index.html for static asset requests //
      // (let them 404 naturally)                         //
      //////////////////////////////////////////////////////
      if(isStaticAsset(requestPath))
      {
         LOG.debug("404 for static asset, letting it 404", logPair("path", requestPath));
         return;
      }

      //////////////////////////////////////////////////
      // Serve index.html for SPA client-side routing //
      //////////////////////////////////////////////////
      LOG.debug("Serving SPA index for deep link", logPair("path", requestPath), logPair("spaPath", spaPath));

      try
      {
         InputStream indexStream = loadSpaIndexFile();
         if(indexStream != null)
         {
            String indexHtml = IOUtils.toString(indexStream, StandardCharsets.UTF_8);

            //////////////////////////////////////////////////////////////////////
            // Rewrite relative asset paths for SPAs at subpaths (deep linking) //
            //////////////////////////////////////////////////////////////////////
            if(!"/".equals(spaPath))
            {
               indexHtml = rewriteIndexHtmlPaths(indexHtml, spaPath);
            }

            ctx.html(indexHtml);
            ctx.status(HttpStatus.OK);
         }
         else
         {
            LOG.error("Could not load SPA index file", logPair("spaIndexFile", spaIndexFile));
         }
      }
      catch(IOException e)
      {
         LOG.error("Error serving SPA index", e, logPair("spaIndexFile", spaIndexFile));
      }
   }



   /*******************************************************************************
    ** Inject or update the HTML <base> tag to fix relative URL resolution for SPAs
    ** hosted at subpaths with deep linking support.
    **
    ** THE PROBLEM:
    ** When an SPA is hosted at a subpath (e.g., "/admin") and uses client-side
    ** routing with deep links (e.g., "/admin/users/123"), relative asset paths
    ** break because the browser resolves them relative to the current route, not
    ** the SPA's base path.
    **
    ** Example:
    **   - SPA hosted at: /admin
    **   - User navigates to: /admin/users/123
    **   - HTML contains: <script src="./assets/main.js">
    **   - Browser incorrectly resolves to: /admin/users/assets/main.js ❌
    **   - Should resolve to: /admin/assets/main.js ✅
    **
    ** THE SOLUTION:
    ** The HTML5 <base> tag tells the browser the base URL for all relative URLs
    ** in the document. By injecting <base href="/admin/"> into the <head>, all
    ** relative URLs (href, src, fetch, import, etc.) automatically resolve
    ** correctly regardless of the current route depth.
    **
    ** This approach is:
    ** - Standards-compliant (HTML5)
    ** - Framework-agnostic (works with any SPA build tool)
    ** - Comprehensive (handles ALL relative URLs, not just href/src)
    ** - Future-proof (works with new HTML features automatically)
    **
    ** EDGE CASES HANDLED:
    ** - If <base> tag already exists, it's updated with the correct href
    ** - If <head> tag has attributes, it's preserved
    ** - Case-insensitive matching for existing <base> tags
    **
    ** NOTE: The <base> tag affects ALL relative URLs including hash links
    ** (e.g., <a href="#section">). Most modern SPAs handle this correctly,
    ** but older SPAs may need updates if they use hash-based navigation.
    **
    ** @param html The HTML content from the SPA's index.html file
    ** @param basePath The SPA base path (e.g., "/admin", "/customer")
    ** @return The HTML with <base> tag injected or updated
    *******************************************************************************/
   String rewriteIndexHtmlPaths(String html, String basePath)
   {
      String baseHref  = basePath.endsWith("/") ? basePath : basePath + "/";
      String lowerHtml = html.toLowerCase();

      if(!lowerHtml.contains("<base "))
      {
         //////////////////////////////////////////////////////
         // No existing <base> tag - inject one after <head> //
         //////////////////////////////////////////////////////
         html = html.replaceFirst("(?i)(<head[^>]*>)", "$1\n  <base href=\"" + baseHref + "\">");
         LOG.debug("Injected <base> tag for deep linking support",
            logPair("basePath", basePath),
            logPair("baseHref", baseHref));
      }
      else
      {
         ///////////////////////////////////////////////////////////
         // Existing <base> tag found - update its href attribute //
         ///////////////////////////////////////////////////////////
         html = html.replaceAll("(?i)<base([^>]*)href\\s*=\\s*['\"]([^'\"]*)['\"]([^>]*)>",
            "<base$1href=\"" + baseHref + "\"$3>");
         LOG.debug("Updated existing <base> tag for deep linking support",
            logPair("basePath", basePath),
            logPair("baseHref", baseHref));
      }

      return html;
   }



   /*******************************************************************************
    ** Check if a request path should be excluded from this SPA's handling.
    **
    ** Used by root SPAs ("/") to exclude specific sub-paths that are handled
    ** by other SPAs or route providers (e.g., exclude /admin, /api from root SPA).
    **
    ** Uses proper path prefix matching with boundary checking to prevent
    ** false matches like /administrator matching /admin.
    **
    ** Examples:
    **   excludedPath="/admin", requestPath="/admin"          → TRUE (exact match)
    **   excludedPath="/admin", requestPath="/admin/users"    → TRUE (sub-path)
    **   excludedPath="/admin", requestPath="/administrator"  → FALSE (different path)
    **
    ** @param path The request path to check
    ** @return true if this path should be excluded from SPA handling
    *******************************************************************************/
   private boolean isExcludedPath(String path)
   {
      for(String excludedPath : excludedPaths)
      {
         if(isPathUnderPrefix(path, excludedPath))
         {
            return true;
         }
      }
      return false;
   }



   /*******************************************************************************
    ** Check if a path looks like a static asset (should 404 naturally)
    **
    ** Delegates to StaticAssetDetector utility for centralized logic.
    **
    ** @param path The request path to check
    ** @return true if the path appears to be a static asset
    *******************************************************************************/
   private boolean isStaticAsset(String path)
   {
      return staticAssetDetector.isStaticAsset(path);
   }



   /*******************************************************************************
    ** Load the SPA index.html file
    *******************************************************************************/
   private InputStream loadSpaIndexFile()
   {
      try
      {
         if(loadFromJar)
         {
            //////////////////////////////////
            // Load from classpath (in JAR) //
            //////////////////////////////////
            LOG.debug("Loading SPA index from classpath", logPair("spaIndexFile", spaIndexFile));
            return getClass().getClassLoader().getResourceAsStream(spaIndexFile);
         }
         else
         {
            ////////////////////////////////////
            // Load from filesystem (for dev) //
            ////////////////////////////////////
            URL resource = getClass().getClassLoader().getResource(staticFilesPath);
            if(resource != null)
            {
               /////////////////////////////////////////////////
               // Extract just the filename from spaIndexFile //
               /////////////////////////////////////////////////
               String fileName = spaIndexFile;
               if(spaIndexFile.contains("/"))
               {
                  fileName = spaIndexFile.substring(spaIndexFile.lastIndexOf('/') + 1);
               }

               File indexFile = new File(resource.getFile(), fileName);
               LOG.debug("Loading SPA index from filesystem",
                  logPair("indexFile", indexFile.getAbsolutePath()),
                  logPair("exists", indexFile.exists()));

               if(indexFile.exists())
               {
                  return new FileInputStream(indexFile);
               }
            }
            else
            {
               LOG.warn("Could not find staticFilesPath resource", logPair("staticFilesPath", staticFilesPath));
            }
         }
      }
      catch(IOException e)
      {
         LOG.error("Error loading SPA index", e, logPair("spaIndexFile", spaIndexFile));
      }

      return null;
   }



   /*******************************************************************************
    ** Authenticate request using configured authenticator
    *******************************************************************************/
   private void authenticateRequest(Context ctx) throws Exception
   {
      if(authenticator == null)
      {
         return;
      }

      if(qInstance == null)
      {
         LOG.error("QInstance is null in authenticateRequest", logPair("path", ctx.path()), logPair("spaPath", spaPath));
         ctx.status(500);
         return;
      }

      RouteAuthenticatorInterface authenticatorInstance = QCodeLoader.getAdHoc(RouteAuthenticatorInterface.class, authenticator);

      //////////////////////////////////////////////
      // Set up QContext for authentication check //
      //////////////////////////////////////////////
      QContext.init(qInstance, new QSystemUserSession());

      try
      {
         boolean authenticated = authenticatorInstance.authenticateRequest(ctx);
         if(!authenticated)
         {
            LOG.warn("Authentication failed for request", logPair("path", ctx.path()), logPair("spaPath", spaPath));
         }
      }
      catch(Exception e)
      {
         LOG.error("Exception in authentication handler", e, logPair("path", ctx.path()), logPair("spaPath", spaPath));
         throw e;
      }
      finally
      {
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Fluent setter: Set the SPA index file path
    *******************************************************************************/
   public IsolatedSpaRouteProvider withSpaIndexFile(String spaIndexFile)
   {
      this.spaIndexFile = spaIndexFile;
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Set the authenticator
    *******************************************************************************/
   public IsolatedSpaRouteProvider withAuthenticator(QCodeReference authenticator)
   {
      this.authenticator = authenticator;
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Add paths to exclude (for root SPA only)
    *******************************************************************************/
   public IsolatedSpaRouteProvider withExcludedPaths(List<String> paths)
   {
      if(paths != null)
      {
         for(String path : paths)
         {
            this.excludedPaths.add(normalizePath(path));
         }
      }
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Add a single excluded path
    *******************************************************************************/
   public IsolatedSpaRouteProvider withExcludedPath(String path)
   {
      this.excludedPaths.add(normalizePath(path));
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Add a custom before handler
    *******************************************************************************/
   public IsolatedSpaRouteProvider withBeforeHandler(Handler handler)
   {
      this.beforeHandlers.add(handler);
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Add a custom after handler
    *******************************************************************************/
   public IsolatedSpaRouteProvider withAfterHandler(Handler handler)
   {
      this.afterHandlers.add(handler);
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Enable/disable deep linking (default: true)
    *******************************************************************************/
   public IsolatedSpaRouteProvider withDeepLinking(boolean enable)
   {
      this.enableDeepLinking = enable;
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Set whether to load from JAR (overrides system property)
    *******************************************************************************/
   public IsolatedSpaRouteProvider withLoadFromJar(boolean loadFromJar)
   {
      this.loadFromJar = loadFromJar;
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Add custom file extensions to detect as static assets
    **
    ** Use this when your SPA uses non-standard file extensions that should be
    ** treated as static assets rather than client-side routes.
    **
    ** Example: .myext, .customdata
    **
    ** @param extensions Custom extensions to add (e.g., ".myext", ".custom")
    ** @return this for method chaining
    *******************************************************************************/
   public IsolatedSpaRouteProvider withCustomAssetExtensions(String... extensions)
   {
      this.staticAssetDetector.withCustomExtensions(extensions);
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Add custom path patterns to detect as static assets
    **
    ** Use this when your SPA uses non-standard directory structures for assets.
    **
    ** Example: /my-assets/, /resources/, /cdn/
    **
    ** @param patterns Path patterns to add (should contain "/" to avoid false positives)
    ** @return this for method chaining
    *******************************************************************************/
   public IsolatedSpaRouteProvider withCustomAssetPathPatterns(String... patterns)
   {
      this.staticAssetDetector.withCustomPathPatterns(patterns);
      return this;
   }



   /*******************************************************************************
    ** Fluent setter: Add custom detection logic for static assets
    **
    ** Use this for complex cases where file extensions and path patterns aren't
    ** sufficient. The custom detector runs FIRST, before extension/path checks.
    **
    ** Example: Detect all paths starting with /cdn/ as assets
    ** provider.withCustomAssetDetector(path -> path.startsWith("/cdn/"))
    **
    ** @param detector Predicate that returns true if path is a static asset
    ** @return this for method chaining
    *******************************************************************************/
   public IsolatedSpaRouteProvider withCustomAssetDetector(java.util.function.Predicate<String> detector)
   {
      this.staticAssetDetector.withCustomDetector(detector);
      return this;
   }



   /*******************************************************************************
    ** Getter for spaPath
    *******************************************************************************/
   public String getSpaPath()
   {
      return spaPath;
   }



   /*******************************************************************************
    ** Getter for staticFilesPath
    *******************************************************************************/
   public String getStaticFilesPath()
   {
      return staticFilesPath;
   }



   /*******************************************************************************
    ** Getter for spaIndexFile
    *******************************************************************************/
   public String getSpaIndexFile()
   {
      return spaIndexFile;
   }
}
