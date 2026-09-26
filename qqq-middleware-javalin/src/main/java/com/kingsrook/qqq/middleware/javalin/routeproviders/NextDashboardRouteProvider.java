/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.Auth0AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Serves the QQQ Next dashboard (qqq-frontend-next static export) from the
 ** classpath at the server root.
 **
 ** The export prerenders each dynamic route once, with the placeholder
 ** directory name "_" (for example app/_/_/index.html for /app/{table}/{id}).
 ** A request path is resolved one segment at a time, preferring an exact file or
 ** directory and otherwise the placeholder directory; directories serve their
 ** index.html. The same rule serves the router payload files (*.txt) that Next
 ** fetches during client-side navigation.
 **
 ** Requests are only handled after no other route matched (via the shared
 ** SpaNotFoundHandlerRegistry), so API endpoints, including their own 404
 ** responses, and path-scoped SPAs are never shadowed.
 **
 ** Every response carries the NextDashboardSecurityHeaders; HTML documents also
 ** get a Content-Security-Policy whose script-src lists the SHA-256 hash of each
 ** inline script in that document (the export's router payload bootstrap), so
 ** no other inline script can run (QRun-IO/qqq#695).
 *******************************************************************************/
public final class NextDashboardRouteProvider implements QJavalinRouteProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(NextDashboardRouteProvider.class);

   public static final String DEFAULT_RESOURCE_ROOT = "next-dashboard";
   public static final String PLACEHOLDER_SEGMENT   = "_";

   //////////////////////////////////////////////////////////////////////////
   // where AWS serves QuickSight embeds (the quickSightChart widget's URL) //
   //////////////////////////////////////////////////////////////////////////
   public static final String QUICKSIGHT_FRAME_SOURCE = "https://*.quicksight.aws.amazon.com";

   private static final Pattern SCRIPT_ELEMENT = Pattern.compile("<script\\b([^>]*)>(.*?)</script\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
   private static final Pattern SRC_ATTRIBUTE  = Pattern.compile("(^|\\s)src\\s*=", Pattern.CASE_INSENSITIVE);

   private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
      Map.entry("html", "text/html; charset=utf-8"),
      Map.entry("txt", "text/x-component; charset=utf-8"),
      Map.entry("js", "text/javascript; charset=utf-8"),
      Map.entry("css", "text/css; charset=utf-8"),
      Map.entry("json", "application/json"),
      Map.entry("map", "application/json"),
      Map.entry("svg", "image/svg+xml"),
      Map.entry("png", "image/png"),
      Map.entry("jpg", "image/jpeg"),
      Map.entry("jpeg", "image/jpeg"),
      Map.entry("gif", "image/gif"),
      Map.entry("webp", "image/webp"),
      Map.entry("ico", "image/x-icon"),
      Map.entry("woff", "font/woff"),
      Map.entry("woff2", "font/woff2"),
      Map.entry("ttf", "font/ttf"),
      Map.entry("webmanifest", "application/manifest+json"));

   private final String      resourceRoot;
   private final Set<String> files       = new HashSet<>();
   private final Set<String> directories = new HashSet<>();

   private final Map<String, List<String>>                     scriptHashes    = new ConcurrentHashMap<>();
   private final AtomicReference<NextDashboardSecurityHeaders> securityHeaders = new AtomicReference<>();
   private       Consumer<NextDashboardSecurityHeaders>        securityHeadersCustomizer;
   private       InstanceOrigins                               instanceOrigins = InstanceOrigins.NONE;



   /*******************************************************************************
    ** The origins an instance's metadata adds to the policy.
    *******************************************************************************/
   private record InstanceOrigins(Set<String> connect, Set<String> frame, Set<String> script)
   {
      static final InstanceOrigins NONE = new InstanceOrigins(Set.of(), Set.of(), Set.of());



      /***************************************************************************
       ** Keep insertion order (the header lists sources in metadata order).
       ***************************************************************************/
      InstanceOrigins
      {
         connect = Collections.unmodifiableSet(new LinkedHashSet<>(connect));
         frame = Collections.unmodifiableSet(new LinkedHashSet<>(frame));
         script = Collections.unmodifiableSet(new LinkedHashSet<>(script));
      }
   }



   /*******************************************************************************
    ** Serve the dashboard packaged under the default classpath folder.
    *******************************************************************************/
   public NextDashboardRouteProvider()
   {
      this(DEFAULT_RESOURCE_ROOT);
   }



   /*******************************************************************************
    ** Serve a static export found under the given classpath folder.
    **
    ** @param resourceRoot classpath folder, without leading or trailing slash
    *******************************************************************************/
   public NextDashboardRouteProvider(String resourceRoot)
   {
      this.resourceRoot = resourceRoot.replaceAll("^/+|/+$", "");
      indexResources();
      this.securityHeaders.set(buildSecurityHeaders());
   }



   /*******************************************************************************
    ** Adjust the security headers (for example extra CSP sources, or allowing the
    ** dashboard to be framed). The customizer receives the defaults with the
    ** instance's identity provider and QuickSight origins already added, and runs
    ** again whenever the QInstance is set or hot-swapped.
    **
    ** @param customizer changes the headers in place, or null for the defaults
    ** @return this
    *******************************************************************************/
   public NextDashboardRouteProvider withSecurityHeadersCustomizer(Consumer<NextDashboardSecurityHeaders> customizer)
   {
      this.securityHeadersCustomizer = customizer;
      this.securityHeaders.set(buildSecurityHeaders());
      return (this);
   }



   /*******************************************************************************
    ** The headers currently sent (after the instance additions and customizer).
    *******************************************************************************/
   public NextDashboardSecurityHeaders getSecurityHeaders()
   {
      return (securityHeaders.get());
   }



   /*******************************************************************************
    ** Whether the default Next dashboard export is on the classpath.
    *******************************************************************************/
   public static boolean isAvailable()
   {
      return (NextDashboardRouteProvider.class.getClassLoader().getResource(DEFAULT_RESOURCE_ROOT + "/index.html") != null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void setQInstance(QInstance qInstance)
   {
      ////////////////////////////////////////////////////////////////////////
      // static files only; authentication happens in the API it calls. The //
      // instance only decides which identity provider and embed origins    //
      // the Content-Security-Policy allows.                                //
      ////////////////////////////////////////////////////////////////////////
      this.instanceOrigins = qInstance == null ? InstanceOrigins.NONE : new InstanceOrigins(identityProviderOrigins(qInstance),
         hasQuickSightWidget(qInstance) ? Set.of(QUICKSIGHT_FRAME_SOURCE) : Set.of(), customComponentOrigins(qInstance));
      this.securityHeaders.set(buildSecurityHeaders());
   }



   /*******************************************************************************
    ** The default headers plus the current instance's origins, customized.
    *******************************************************************************/
   private NextDashboardSecurityHeaders buildSecurityHeaders()
   {
      NextDashboardSecurityHeaders headers = new NextDashboardSecurityHeaders();
      instanceOrigins.connect().forEach(origin -> headers.withSources("connect-src", origin));
      instanceOrigins.frame().forEach(origin -> headers.withSources("frame-src", origin));
      instanceOrigins.script().forEach(origin -> headers.withSources(NextDashboardSecurityHeaders.SCRIPT_SRC, origin));
      if(securityHeadersCustomizer != null)
      {
         securityHeadersCustomizer.accept(headers);
      }
      return (headers);
   }



   /*******************************************************************************
    ** Origins of the instance's OAUTH2 and AUTH_0 identity providers, which the
    ** dashboard calls from the browser (OIDC discovery, the Auth0 token exchange).
    *******************************************************************************/
   static Set<String> identityProviderOrigins(QInstance qInstance)
   {
      List<QAuthenticationMetaData> providers = new ArrayList<>();
      providers.add(qInstance.getAuthentication());
      if(qInstance.getScopedAuthenticationProviders() != null)
      {
         providers.addAll(qInstance.getScopedAuthenticationProviders().values());
      }

      Set<String> origins = new LinkedHashSet<>();
      for(QAuthenticationMetaData provider : providers)
      {
         String baseUrl = null;
         if(provider instanceof OAuth2AuthenticationMetaData oauth2)
         {
            baseUrl = oauth2.getBaseUrl();
         }
         else if(provider instanceof Auth0AuthenticationMetaData auth0)
         {
            baseUrl = auth0.getBaseUrl();
         }
         String origin = NextDashboardSecurityHeaders.originOf(baseUrl);
         if(origin != null)
         {
            origins.add(origin);
         }
      }
      return (origins);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean hasQuickSightWidget(QInstance qInstance)
   {
      return (widgets(qInstance).stream().anyMatch(widget -> WidgetType.QUICK_SIGHT_CHART.getType().equals(widget.getType())));
   }



   /*******************************************************************************
    ** Origins of the script bundles that customComponent widgets load (their
    ** componentSourceUrl default value), when they are on another origin.
    *******************************************************************************/
   static Set<String> customComponentOrigins(QInstance qInstance)
   {
      Set<String> origins = new LinkedHashSet<>();
      for(QWidgetMetaDataInterface widget : widgets(qInstance))
      {
         if(WidgetType.CUSTOM_COMPONENT.getType().equals(widget.getType()) && widget.getDefaultValues() != null)
         {
            Object sourceUrl = widget.getDefaultValues().get("componentSourceUrl");
            String origin    = NextDashboardSecurityHeaders.originOf(sourceUrl == null ? null : String.valueOf(sourceUrl));
            if(origin != null)
            {
               origins.add(origin);
            }
         }
      }
      return (origins);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QWidgetMetaDataInterface> widgets(QInstance qInstance)
   {
      Map<String, QWidgetMetaDataInterface> widgets = qInstance.getWidgets();
      return (widgets == null ? List.of() : widgets.values().stream().filter(Objects::nonNull).toList());
   }



   /*******************************************************************************
    ** CSP hash sources ('sha256-...') of the inline scripts in an HTML document.
    ** Line breaks are normalized first, as the HTML parser does before hashing.
    **
    ** @param html the document
    ** @return one source per distinct inline script, in document order
    *******************************************************************************/
   public static List<String> inlineScriptHashes(String html)
   {
      Set<String> hashes  = new LinkedHashSet<>();
      Matcher     matcher = SCRIPT_ELEMENT.matcher(html);
      while(matcher.find())
      {
         if(SRC_ATTRIBUTE.matcher(matcher.group(1)).find())
         {
            continue;
         }
         String script = matcher.group(2).replace("\r\n", "\n").replace('\r', '\n');
         try
         {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(script.getBytes(StandardCharsets.UTF_8));
            hashes.add("'sha256-" + Base64.getEncoder().encodeToString(digest) + "'");
         }
         catch(NoSuchAlgorithmException e)
         {
            throw (new IllegalStateException("SHA-256 is not available", e));
         }
      }
      return (new ArrayList<>(hashes));
   }



   /*******************************************************************************
    ** The inline script hashes of an exported HTML file (read once, then cached).
    *******************************************************************************/
   private List<String> scriptHashesFor(String file)
   {
      return (scriptHashes.computeIfAbsent(file, name ->
      {
         try(InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceRoot + "/" + name))
         {
            return (stream == null ? List.of() : inlineScriptHashes(new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
         }
         catch(IOException e)
         {
            throw (new IllegalStateException("Could not read " + name + " from the Next dashboard export", e));
         }
      }));
   }



   /*******************************************************************************
    ** Register the root fallback that serves the export.
    *******************************************************************************/
   @Override
   public void acceptJavalinConfig(JavalinConfig config)
   {
      if(!files.contains("index.html"))
      {
         throw (new IllegalStateException("Next dashboard export was not found on the classpath at " + resourceRoot + "/index.html"));
      }
      SpaNotFoundHandlerRegistry.getInstance(config).registerSpaHandler("/", this::handle);
      LOG.info("Serving Next dashboard", logPair("resourceRoot", resourceRoot), logPair("files", files.size()));
   }



   /*******************************************************************************
    ** Resolve a request path to an exported file, or null when there is none.
    **
    ** @param requestPath URL path, for example /app/person/1
    ** @return the file path relative to the resource root
    *******************************************************************************/
   public String resolve(String requestPath)
   {
      String[] segments = requestPath.split("/");
      String   directory = "";
      int      last      = segments.length - 1;
      while(last >= 0 && segments[last].isEmpty())
      {
         last--;
      }

      for(int i = 0; i <= last; i++)
      {
         if(segments[i].isEmpty())
         {
            continue;
         }

         String segment = URLDecoder.decode(segments[i], StandardCharsets.UTF_8);
         if(segment.equals("..") || segment.equals(".") || segment.contains("/") || segment.contains("\\"))
         {
            return (null);
         }

         String exact = join(directory, segment);
         if(i == last && files.contains(exact))
         {
            return (exact);
         }
         else if(directories.contains(exact))
         {
            directory = exact;
         }
         else if(directories.contains(join(directory, PLACEHOLDER_SEGMENT)))
         {
            directory = join(directory, PLACEHOLDER_SEGMENT);
         }
         else
         {
            return (null);
         }
      }

      String index = join(directory, "index.html");
      return (files.contains(index) ? index : null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String join(String directory, String name)
   {
      return (directory.isEmpty() ? name : directory + "/" + name);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void handle(Context context)
   {
      if(context.method() != HandlerType.GET && context.method() != HandlerType.HEAD)
      {
         return;
      }

      String file = resolve(context.path());
      if(file == null)
      {
         ///////////////////////////////////////////////////////////////////////
         // unknown page-like path: show the dashboard's own not-found page //
         ///////////////////////////////////////////////////////////////////////
         if(!context.path().contains(".") && files.contains("404.html"))
         {
            send(context, "404.html", HttpStatus.NOT_FOUND);
         }
         return;
      }
      send(context, file, HttpStatus.OK);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void send(Context context, String file, HttpStatus status)
   {
      InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceRoot + "/" + file);
      if(stream == null)
      {
         return;
      }

      String extension = file.contains(".") ? file.substring(file.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
      context.status(status);
      context.contentType(CONTENT_TYPES.getOrDefault(extension, "application/octet-stream"));
      /////////////////////////////////////////////////////////////////////////////////
      // hashed build assets never change; pages and router payloads must revalidate //
      /////////////////////////////////////////////////////////////////////////////////
      context.header("Cache-Control", file.startsWith("_next/static/") ? "public, max-age=31536000, immutable" : "no-cache");

      NextDashboardSecurityHeaders headers = securityHeaders.get();
      headers.getHeaders().forEach(context::header);
      String policyHeader = headers.getContentSecurityPolicyHeaderName();
      if(policyHeader != null && "html".equals(extension))
      {
         context.header(policyHeader, headers.buildContentSecurityPolicy(scriptHashesFor(file)));
      }
      context.result(stream);
   }



   /*******************************************************************************
    ** Build the file and directory index from a jar or directory classpath entry.
    *******************************************************************************/
   private void indexResources()
   {
      ////////////////////////////////////////////////////////////////////////////////
      // Index only the first export on the classpath (the one getResource serves), //
      // so a development export placed ahead of a packaged jar is never mixed.     //
      ////////////////////////////////////////////////////////////////////////////////
      URL index = getClass().getClassLoader().getResource(resourceRoot + "/index.html");
      if(index == null)
      {
         return;
      }

      try
      {
         String indexUrl = index.toString();
         URL    root     = URI.create(indexUrl.substring(0, indexUrl.length() - "/index.html".length())).toURL();
         if("jar".equals(root.getProtocol()))
         {
            indexJar(root);
         }
         else if("file".equals(root.getProtocol()))
         {
            indexDirectory(Path.of(root.toURI()));
         }
         else
         {
            LOG.warn("Unsupported classpath location for the Next dashboard", logPair("url", indexUrl));
         }
      }
      catch(IOException | URISyntaxException e)
      {
         throw (new IllegalStateException("Could not read the Next dashboard export at " + resourceRoot, e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void indexJar(URL root) throws IOException
   {
      JarURLConnection connection = (JarURLConnection) root.openConnection();
      connection.setUseCaches(false);
      try(JarFile jar = connection.getJarFile())
      {
         String prefix = resourceRoot + "/";
         Enumeration<JarEntry> entries = jar.entries();
         while(entries.hasMoreElements())
         {
            JarEntry entry = entries.nextElement();
            if(entry.getName().startsWith(prefix) && entry.getName().length() > prefix.length())
            {
               add(entry.getName().substring(prefix.length()), entry.isDirectory());
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void indexDirectory(Path root) throws IOException
   {
      try(Stream<Path> paths = Files.walk(root))
      {
         paths.filter(path -> !path.equals(root)).forEach(path ->
            add(root.relativize(path).toString().replace(File.separatorChar, '/'), Files.isDirectory(path)));
      }
   }



   /*******************************************************************************
    ** Record a file and every parent directory of it.
    *******************************************************************************/
   private void add(String relativePath, boolean isDirectory)
   {
      String path = relativePath.replaceAll("/+$", "");
      if(isDirectory)
      {
         directories.add(path);
      }
      else
      {
         files.add(path);
      }
      for(int slash = path.lastIndexOf('/'); slash > 0; slash = path.lastIndexOf('/', slash - 1))
      {
         directories.add(path.substring(0, slash));
      }
   }
}
