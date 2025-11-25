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
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import com.kingsrook.qqq.middleware.javalin.metadata.JavalinRouteProviderMetaData;
import com.kingsrook.qqq.middleware.javalin.routeproviders.authentication.RouteAuthenticatorInterface;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.http.staticfiles.StaticFileConfig;
import org.apache.commons.io.IOUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** javalin route provider that hosts a path in the http server via a path on
 ** the file system
 *******************************************************************************/
public class SimpleFileSystemDirectoryRouter implements QJavalinRouteProviderInterface
{
   private static final QLogger LOG                    = QLogger.getLogger(SimpleFileSystemDirectoryRouter.class);
   public static        boolean loadStaticFilesFromJar = false;


   private final String hostedPath;
   private final String fileSystemPath;
   private       String spaRootPath;
   private       String spaRootFile;

   private QCodeReference      routeAuthenticator;
   private QInstance           qInstance;
   private StaticAssetDetector staticAssetDetector;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public SimpleFileSystemDirectoryRouter(String hostedPath, String fileSystemPath)
   {
      this.hostedPath = hostedPath;
      this.fileSystemPath = fileSystemPath;
      this.staticAssetDetector = new StaticAssetDetector().withName("router:" + hostedPath);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // read the property to see if we should load static files from the jar file or from the file system //
      // Javan only supports loading via one method per path, so its a choice of one or the other...       //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      try
      {
         String propertyName  = "qqq.javalin.enableStaticFilesFromJar";  // TODO: make a more general way to handle properties like this system-wide via a central config class
         String propertyValue = System.getProperty(propertyName, "");
         if(propertyValue.equals("true"))
         {
            loadStaticFilesFromJar = true;
         }
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public SimpleFileSystemDirectoryRouter(JavalinRouteProviderMetaData routeProvider)
   {
      this(routeProvider.getHostedPath(), routeProvider.getFileSystemPath());
      setSpaRootPath(routeProvider.getSpaRootPath());
      setSpaRootFile(routeProvider.getSpaRootFile());
      setRouteAuthenticator(routeProvider.getRouteAuthenticator());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setQInstance(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void handleJavalinStaticFileConfig(StaticFileConfig staticFileConfig)
   {

      if(!hostedPath.startsWith("/"))
      {
         LOG.warn("hostedPath [" + hostedPath + "] should probably start with a leading slash...");
      }

      /////////////////////////////////////////////////////////////////////////////////////////
      // Handle loading static files from the jar OR the filesystem based on system property //
      /////////////////////////////////////////////////////////////////////////////////////////
      if(SimpleFileSystemDirectoryRouter.loadStaticFilesFromJar)
      {
         staticFileConfig.directory = fileSystemPath;
         staticFileConfig.hostedPath = hostedPath;
         staticFileConfig.location = Location.CLASSPATH;
         LOG.info("Static File Config : hostedPath [" + hostedPath + "] : directory [" + staticFileConfig.directory + "] : location [CLASSPATH]");
      }
      else
      {
         URL resource = getClass().getClassLoader().getResource(fileSystemPath);
         if(resource == null)
         {
            String message = "Could not find file system path: " + fileSystemPath;
            if(fileSystemPath.startsWith("/") && getClass().getClassLoader().getResource(fileSystemPath.replaceFirst("^/+", "")) != null)
            {
               message += ".  For non-absolute paths, do not prefix with a leading slash.";
            }
            throw new RuntimeException(message);
         }

         staticFileConfig.directory = resource.getFile();
         staticFileConfig.hostedPath = hostedPath;
         staticFileConfig.location = Location.EXTERNAL;
         LOG.info("Static File Config : hostedPath [" + hostedPath + "] : directory [" + staticFileConfig.directory + "] : location [EXTERNAL]");
      }

   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void before(Context context) throws QException
   {
      LOG.debug("In before handler for simpleFileSystemRouter", logPair("hostedPath", hostedPath));
      QContext.init(qInstance, new QSystemUserSession());

      if(routeAuthenticator != null)
      {
         try
         {
            RouteAuthenticatorInterface routeAuthenticator = QCodeLoader.getAdHoc(RouteAuthenticatorInterface.class, this.routeAuthenticator);
            boolean                     isAuthenticated    = routeAuthenticator.authenticateRequest(context);
            if(!isAuthenticated)
            {
               LOG.info("Static file request is not authenticated, so telling javalin to skip remaining handlers", logPair("path", context.path()));
               context.skipRemainingHandlers();
            }
         }
         catch(Exception e)
         {
            context.skipRemainingHandlers();
            QJavalinImplementation.handleException(context, e);
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void after(Context context)
   {
      LOG.debug("In after handler for simpleFileSystemRouter", logPair("hostedPath", hostedPath));
      QContext.clear();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void acceptJavalinConfig(JavalinConfig config)
   {
      config.staticFiles.add(this::handleJavalinStaticFileConfig);
   }



   /***************************************************************************
    ** Setup SPA deep linking support and authentication handlers.
    ** This method is called AFTER Javalin config phase, which allows us to
    ** register handlers that run after static file serving. This is critical
    ** for proper SPA deep linking support with multiple SPAs.
    ***************************************************************************/
   @Override
   public void acceptJavalinService(Javalin service)
   {
      ///////////////////////////////////////////////////////////////////////////////////////////
      // If this is configured as an SPA, use Javalin's error handler (404) to catch unmatched //
      // routes and serve index.html. This enables deep linking to work properly while still   //
      // allowing static files to be served normally.                                          //
      //                                                                                       //
      // KEY INSIGHT: Using error(404) ensures this runs AFTER static file serving, which is   //
      // critical for the feature to work correctly with multiple SPAs.                        //
      ///////////////////////////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(spaRootPath) && StringUtils.hasContent(spaRootFile))
      {
         LOG.info("Registering SPA deep linking handler via 404 error handler",
            logPair("spaRootPath", spaRootPath),
            logPair("spaRootFile", spaRootFile));

         service.error(404, ctx ->
         {
            String requestPath = ctx.path();

            /////////////////////////////////////////////////////////////////////////////////////
            // Only handle 404s for paths under our SPA root                                  //
            // Special case: if spaRootPath is "/", we need to be more careful to avoid       //
            // catching API routes and other non-SPA paths                                    //
            /////////////////////////////////////////////////////////////////////////////////////
            boolean isUnderSpaRoot = requestPath.startsWith(spaRootPath);
            
            // If SPA root is "/", check that it's not explicitly for other route types
            if("/".equals(spaRootPath))
            {
               // For root-level SPAs, only proceed if it's not an API or static asset
               // The isApiRequest() check below will handle this
            }
            else if(!isUnderSpaRoot)
            {
               LOG.debug("404 path not under our SPA root, skipping", logPair("path", requestPath), logPair("spaRoot", spaRootPath));
               return;
            }

            /////////////////////////////////////////////////////////////////////////////
            // Check if this looks like a static asset request (js, css, images, etc.) //
            // If so, let the 404 stand (assets should 404 if they don't exist).       //
            // Otherwise, serve the SPA's index.html to support client-side routing.   //
            /////////////////////////////////////////////////////////////////////////////
            if(isStaticAssetRequest(requestPath))
            {
               LOG.debug("404 for static asset, letting 404 stand", logPair("path", requestPath));
               return;
            }

            /////////////////////////////////////////////////////////////////////////////
            // Check if this looks like an API request (e.g., /qqq-api/...).          //
            // If so, let the 404 stand (API routes should 404 if they don't exist).  //
            // This prevents API 404s from being caught by the SPA fallback.          //
            /////////////////////////////////////////////////////////////////////////////
            if(isApiRequest(requestPath))
            {
               LOG.debug("404 for API path, letting 404 stand", logPair("path", requestPath));
               return;
            }

            LOG.debug("Serving SPA index for client-side route (404 handler)",
               logPair("path", requestPath),
               logPair("spaRoot", spaRootPath));

            try
            {
               InputStream indexStream = loadSpaIndex();
               if(indexStream != null)
               {
                  String indexHtml = IOUtils.toString(indexStream, StandardCharsets.UTF_8);
                  ctx.html(indexHtml);
                  ctx.status(200);
               }
               else
               {
                  LOG.error("Could not load SPA index file", logPair("spaRootFile", spaRootFile));
                  ///////////////////////////////////////////
                  // Don't change status - leave it as 404 //
                  ///////////////////////////////////////////
               }
            }
            catch(IOException e)
            {
               LOG.error("Error serving SPA index", e, logPair("spaRootFile", spaRootFile));
               ///////////////////////////////////////////
               // Don't change status - leave it as 404 //
               ///////////////////////////////////////////
            }
         });
      }

      ////////////////////////////////////////////////////////////////////
      // Set up authentication before/after handlers for all requests  //
      ////////////////////////////////////////////////////////////////////
      String javalinPath = hostedPath;
      if(!javalinPath.endsWith("/"))
      {
         javalinPath += "/";
      }
      javalinPath += "<subPath>";

      service.before(javalinPath, this::before);
      service.after(javalinPath, this::after);
   }



   /***************************************************************************
    ** Determines if a request path looks like a static asset based on file
    ** extension and common asset path patterns.
    **
    ** Delegates to StaticAssetDetector utility for centralized logic.
    **
    ** Returns true for common asset file extensions (.js, .css, images, fonts, etc.)
    ** and for paths that contain common asset directory names.
    **
    ** @param path The request path to check
    ** @return true if the path appears to be a static asset
    ***************************************************************************/
   private boolean isStaticAssetRequest(String path)
   {
      return staticAssetDetector.isStaticAsset(path);
   }



   /***************************************************************************
    ** Determines if a request path looks like an API request.
    **
    ** Returns true for paths that start with common API prefixes like:
    ** - /qqq-api/       (versioned application APIs)
    ** - /api/           (generic API paths)
    ** - /metaData       (legacy qqq middleware)
    ** - /data/          (legacy qqq middleware)
    ** - /processes/     (legacy qqq middleware)
    ** - /reports/       (legacy qqq middleware)
    ** - /download/      (legacy qqq middleware)
    **
    ** This prevents API 404s from being caught by SPA fallback handlers.
    ***************************************************************************/
   private boolean isApiRequest(String path)
   {
      String lowerPath = path.toLowerCase();

      //////////////////////////////////////////////////////
      // Check for common API path prefixes               //
      //////////////////////////////////////////////////////
      return lowerPath.startsWith("/qqq-api/")
         || lowerPath.startsWith("/api/")
         || lowerPath.equals("/metadata")
         || lowerPath.startsWith("/metadata/")
         || lowerPath.startsWith("/data/")
         || lowerPath.startsWith("/processes/")
         || lowerPath.startsWith("/reports/")
         || lowerPath.startsWith("/download/")
         || lowerPath.startsWith("/possiblevalues/")
         || lowerPath.startsWith("/widget/");
   }



   /***************************************************************************
    ** Loads the SPA index.html from either the classpath (for JAR deployments)
    ** or the filesystem (for development).
    **
    ** @return InputStream of the index.html file, or null if not found
    ***************************************************************************/
   private InputStream loadSpaIndex()
   {
      try
      {
         if(loadStaticFilesFromJar)
         {
            ///////////////////////////////////
            // Load from classpath (in JAR) //
            ///////////////////////////////////
            LOG.debug("Loading SPA index from classpath", logPair("spaRootFile", spaRootFile));
            return getClass().getClassLoader().getResourceAsStream(spaRootFile);
         }
         else
         {
            ////////////////////////////////////
            // Load from filesystem (for dev) //
            ////////////////////////////////////
            URL resource = getClass().getClassLoader().getResource(fileSystemPath);
            if(resource != null)
            {
               // Extract just the filename from spaRootFile (remove directory prefix if present)
               String fileName = spaRootFile;
               if(spaRootFile.contains("/"))
               {
                  fileName = spaRootFile.substring(spaRootFile.lastIndexOf('/') + 1);
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
               LOG.warn("Could not find fileSystemPath resource", logPair("fileSystemPath", fileSystemPath));
            }
         }
      }
      catch(IOException e)
      {
         LOG.error("Error loading SPA index", e, logPair("spaRootFile", spaRootFile));
      }

      return null;
   }



   /*******************************************************************************
    ** Getter for routeAuthenticator
    *******************************************************************************/
   public QCodeReference getRouteAuthenticator()
   {
      return (this.routeAuthenticator);
   }



   /*******************************************************************************
    ** Setter for routeAuthenticator
    *******************************************************************************/
   public void setRouteAuthenticator(QCodeReference routeAuthenticator)
   {
      this.routeAuthenticator = routeAuthenticator;
   }



   /*******************************************************************************
    ** Fluent setter for routeAuthenticator
    *******************************************************************************/
   public SimpleFileSystemDirectoryRouter withRouteAuthenticator(QCodeReference routeAuthenticator)
   {
      this.routeAuthenticator = routeAuthenticator;
      return (this);
   }



   /*******************************************************************************
    ** Getter for spaRootPath
    *******************************************************************************/
   public String getSpaRootPath()
   {
      return (this.spaRootPath);
   }



   /*******************************************************************************
    ** Setter for spaRootPath
    *******************************************************************************/
   public void setSpaRootPath(String spaRootPath)
   {
      this.spaRootPath = spaRootPath;
   }



   /*******************************************************************************
    ** Fluent setter for spaRootPath
    *******************************************************************************/
   public SimpleFileSystemDirectoryRouter withSpaRootPath(String spaRootPath)
   {
      this.spaRootPath = spaRootPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for spaRootFile
    *******************************************************************************/
   public String getSpaRootFile()
   {
      return (this.spaRootFile);
   }



   /*******************************************************************************
    ** Setter for spaRootFile
    *******************************************************************************/
   public void setSpaRootFile(String spaRootFile)
   {
      this.spaRootFile = spaRootFile;
   }



   /*******************************************************************************
    ** Fluent setter for spaRootFile
    *******************************************************************************/
   public SimpleFileSystemDirectoryRouter withSpaRootFile(String spaRootFile)
   {
      this.spaRootFile = spaRootFile;
      return (this);
   }

}
