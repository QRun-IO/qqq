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

package com.kingsrook.qqq.middleware.javalin.metadata;


import java.util.List;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.routeproviders.authentication.RouteAuthenticatorInterface;
import com.kingsrook.qqq.middleware.javalin.routeproviders.contexthandlers.RouteProviderContextHandlerInterface;
import com.kingsrook.qqq.middleware.javalin.routeproviders.handlers.RouteProviderAfterHandlerInterface;
import com.kingsrook.qqq.middleware.javalin.routeproviders.handlers.RouteProviderBeforeHandlerInterface;


/*******************************************************************************
 ** Meta-data to define a Javalin route provider configuration.
 **
 ** This class configures how routes are provided and served in the Javalin
 ** middleware layer. It supports multiple types of route providers including:
 ** - Process-based routes (executing QQQ processes via HTTP endpoints)
 ** - Static file serving (hosting files from the file system or JAR)
 ** - Single Page Application (SPA) hosting with deep linking support
 ** - Isolated SPA hosting with separate route prefixes and static files
 **
 ** Route providers can be configured with authentication, context handlers,
 ** and before/after handlers for request processing customization.
 *******************************************************************************/
public class JavalinRouteProviderMetaData implements QMetaDataObject
{
   private String name;
   private String hostedPath;

   private String fileSystemPath;
   private String spaRootPath;
   private String spaRootFile;

   private String processName;

   //////////////////////////////////////////////
   // IsolatedSpaRouteProvider specific fields //
   /// ///////////////////////////////////////////
   private String       spaPath;
   private String       staticFilesPath;
   private String       spaIndexFile;
   private List<String> excludedPaths;
   private boolean      enableDeepLinking = true;
   private boolean      loadFromJar       = false;

   private List<String> methods;

   private QCodeReference routeAuthenticator;
   private QCodeReference contextHandler;

   //////////////////////////////////////////////
   // IsolatedSpaRouteProvider handler support //
   /// ///////////////////////////////////////////
   private List<QCodeReference> beforeHandlers;
   private List<QCodeReference> afterHandlers;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public JavalinRouteProviderMetaData()
   {
   }



   /*******************************************************************************
    ** Getter for hostedPath
    ** @see #withHostedPath(String)
    *******************************************************************************/
   public String getHostedPath()
   {
      return (this.hostedPath);
   }



   /*******************************************************************************
    ** Setter for hostedPath
    ** @see #withHostedPath(String)
    *******************************************************************************/
   public void setHostedPath(String hostedPath)
   {
      this.hostedPath = hostedPath;
   }



   /*******************************************************************************
    ** Fluent setter for hostedPath
    **
    ** @param hostedPath The URL path where this route will be hosted
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withHostedPath(String hostedPath)
   {
      this.hostedPath = hostedPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fileSystemPath
    ** @see #withFileSystemPath(String)
    *******************************************************************************/
   public String getFileSystemPath()
   {
      return (this.fileSystemPath);
   }



   /*******************************************************************************
    ** Setter for fileSystemPath
    ** @see #withFileSystemPath(String)
    *******************************************************************************/
   public void setFileSystemPath(String fileSystemPath)
   {
      this.fileSystemPath = fileSystemPath;
   }



   /*******************************************************************************
    ** Fluent setter for fileSystemPath
    **
    ** @param fileSystemPath The file system path where static files are located
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withFileSystemPath(String fileSystemPath)
   {
      this.fileSystemPath = fileSystemPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for spaRootPath
    ** @see #withSpaRootPath(String)
    *******************************************************************************/
   public String getSpaRootPath()
   {
      return (this.spaRootPath);
   }



   /*******************************************************************************
    ** Setter for spaRootPath
    ** @see #withSpaRootPath(String)
    *******************************************************************************/
   public void setSpaRootPath(String spaRootPath)
   {
      this.spaRootPath = spaRootPath;
   }



   /*******************************************************************************
    ** Fluent setter for spaRootPath
    **
    ** @param spaRootPath The root path for the SPA
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withSpaRootPath(String spaRootPath)
   {
      this.spaRootPath = spaRootPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for spaRootFile
    ** @see #withSpaRootFile(String)
    *******************************************************************************/
   public String getSpaRootFile()
   {
      return (this.spaRootFile);
   }



   /*******************************************************************************
    ** Setter for spaRootFile
    ** @see #withSpaRootFile(String)
    *******************************************************************************/
   public void setSpaRootFile(String spaRootFile)
   {
      this.spaRootFile = spaRootFile;
   }



   /*******************************************************************************
    ** Fluent setter for spaRootFile
    **
    ** @param spaRootFile The root file for the SPA (typically index.html)
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withSpaRootFile(String spaRootFile)
   {
      this.spaRootFile = spaRootFile;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processName
    ** @see #withProcessName(String)
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    ** Setter for processName
    ** @see #withProcessName(String)
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    ** Fluent setter for processName
    **
    ** @param processName The name of the process to be executed by this route
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for methods
    ** @see #withMethods(List)
    *******************************************************************************/
   public List<String> getMethods()
   {
      return (this.methods);
   }



   /*******************************************************************************
    ** Setter for methods
    ** @see #withMethods(List)
    *******************************************************************************/
   public void setMethods(List<String> methods)
   {
      this.methods = methods;
   }



   /*******************************************************************************
    ** Fluent setter for methods
    **
    ** @param methods List of HTTP methods this route should handle (e.g., GET, POST)
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withMethods(List<String> methods)
   {
      this.methods = methods;
      return (this);
   }



   /*******************************************************************************
    ** Getter for routeAuthenticator
    ** @see #withRouteAuthenticator(QCodeReference)
    *******************************************************************************/
   public QCodeReference getRouteAuthenticator()
   {
      return (this.routeAuthenticator);
   }



   /*******************************************************************************
    ** Setter for routeAuthenticator
    ** @see #withRouteAuthenticator(QCodeReference)
    *******************************************************************************/
   public void setRouteAuthenticator(QCodeReference routeAuthenticator)
   {
      this.routeAuthenticator = routeAuthenticator;
   }



   /*******************************************************************************
    ** Fluent setter for routeAuthenticator
    **
    ** @param routeAuthenticator Code reference to the authenticator for this route
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withRouteAuthenticator(QCodeReference routeAuthenticator)
   {
      this.routeAuthenticator = routeAuthenticator;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contextHandler
    ** @see #withContextHandler(QCodeReference)
    *******************************************************************************/
   public QCodeReference getContextHandler()
   {
      return (this.contextHandler);
   }



   /*******************************************************************************
    ** Setter for contextHandler
    ** @see #withContextHandler(QCodeReference)
    *******************************************************************************/
   public void setContextHandler(QCodeReference contextHandler)
   {
      this.contextHandler = contextHandler;
   }



   /*******************************************************************************
    ** Fluent setter for contextHandler
    **
    ** @param contextHandler Code reference to the context handler for this route
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withContextHandler(QCodeReference contextHandler)
   {
      this.contextHandler = contextHandler;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    ** @see #withName(String)
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    ** @see #withName(String)
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    ** @param name The unique name for this route provider
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for spaPath
    ** @see #withSpaPath(String)
    *******************************************************************************/
   public String getSpaPath()
   {
      return (this.spaPath);
   }



   /*******************************************************************************
    ** Setter for spaPath
    ** @see #withSpaPath(String)
    *******************************************************************************/
   public void setSpaPath(String spaPath)
   {
      this.spaPath = spaPath;
   }



   /*******************************************************************************
    ** Fluent setter for spaPath
    **
    ** @param spaPath The URL path where the SPA will be served
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withSpaPath(String spaPath)
   {
      this.spaPath = spaPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for staticFilesPath
    ** @see #withStaticFilesPath(String)
    *******************************************************************************/
   public String getStaticFilesPath()
   {
      return (this.staticFilesPath);
   }



   /*******************************************************************************
    ** Setter for staticFilesPath
    ** @see #withStaticFilesPath(String)
    *******************************************************************************/
   public void setStaticFilesPath(String staticFilesPath)
   {
      this.staticFilesPath = staticFilesPath;
   }



   /*******************************************************************************
    ** Fluent setter for staticFilesPath
    **
    ** @param staticFilesPath The path where static files are located for the SPA
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withStaticFilesPath(String staticFilesPath)
   {
      this.staticFilesPath = staticFilesPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for spaIndexFile
    ** @see #withSpaIndexFile(String)
    *******************************************************************************/
   public String getSpaIndexFile()
   {
      return (this.spaIndexFile);
   }



   /*******************************************************************************
    ** Setter for spaIndexFile
    ** @see #withSpaIndexFile(String)
    *******************************************************************************/
   public void setSpaIndexFile(String spaIndexFile)
   {
      this.spaIndexFile = spaIndexFile;
   }



   /*******************************************************************************
    ** Fluent setter for spaIndexFile
    **
    ** @param spaIndexFile The index file name for the SPA (e.g., index.html)
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withSpaIndexFile(String spaIndexFile)
   {
      this.spaIndexFile = spaIndexFile;
      return (this);
   }



   /*******************************************************************************
    ** Getter for excludedPaths
    ** @see #withExcludedPaths(List)
    *******************************************************************************/
   public List<String> getExcludedPaths()
   {
      return (this.excludedPaths);
   }



   /*******************************************************************************
    ** Setter for excludedPaths
    ** @see #withExcludedPaths(List)
    *******************************************************************************/
   public void setExcludedPaths(List<String> excludedPaths)
   {
      this.excludedPaths = excludedPaths;
   }



   /*******************************************************************************
    ** Fluent setter for excludedPaths
    **
    ** @param excludedPaths List of URL paths to exclude from SPA routing
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withExcludedPaths(List<String> excludedPaths)
   {
      this.excludedPaths = excludedPaths;
      return (this);
   }



   /*******************************************************************************
    ** Getter for enableDeepLinking
    ** @see #withEnableDeepLinking(boolean)
    *******************************************************************************/
   public boolean getEnableDeepLinking()
   {
      return (this.enableDeepLinking);
   }



   /*******************************************************************************
    ** Setter for enableDeepLinking
    ** @see #withEnableDeepLinking(boolean)
    *******************************************************************************/
   public void setEnableDeepLinking(boolean enableDeepLinking)
   {
      this.enableDeepLinking = enableDeepLinking;
   }



   /*******************************************************************************
    ** Fluent setter for enableDeepLinking
    **
    ** @param enableDeepLinking Whether to enable deep linking for the SPA (default true)
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withEnableDeepLinking(boolean enableDeepLinking)
   {
      this.enableDeepLinking = enableDeepLinking;
      return (this);
   }



   /*******************************************************************************
    ** Getter for loadFromJar
    ** @see #withLoadFromJar(boolean)
    *******************************************************************************/
   public boolean getLoadFromJar()
   {
      return (this.loadFromJar);
   }



   /*******************************************************************************
    ** Setter for loadFromJar
    ** @see #withLoadFromJar(boolean)
    *******************************************************************************/
   public void setLoadFromJar(boolean loadFromJar)
   {
      this.loadFromJar = loadFromJar;
   }



   /*******************************************************************************
    ** Fluent setter for loadFromJar
    **
    ** @param loadFromJar Whether to load SPA files from JAR resources (default false)
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withLoadFromJar(boolean loadFromJar)
   {
      this.loadFromJar = loadFromJar;
      return (this);
   }



   /*******************************************************************************
    ** Getter for beforeHandlers
    ** @see #withBeforeHandlers(List)
    *******************************************************************************/
   public List<QCodeReference> getBeforeHandlers()
   {
      return (this.beforeHandlers);
   }



   /*******************************************************************************
    ** Setter for beforeHandlers
    ** @see #withBeforeHandlers(List)
    *******************************************************************************/
   public void setBeforeHandlers(List<QCodeReference> beforeHandlers)
   {
      this.beforeHandlers = beforeHandlers;
   }



   /*******************************************************************************
    ** Fluent setter for beforeHandlers
    **
    ** @param beforeHandlers List of code references to handlers executed before the route
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withBeforeHandlers(List<QCodeReference> beforeHandlers)
   {
      this.beforeHandlers = beforeHandlers;
      return (this);
   }



   /*******************************************************************************
    ** Getter for afterHandlers
    ** @see #withAfterHandlers(List)
    *******************************************************************************/
   public List<QCodeReference> getAfterHandlers()
   {
      return (this.afterHandlers);
   }



   /*******************************************************************************
    ** Setter for afterHandlers
    ** @see #withAfterHandlers(List)
    *******************************************************************************/
   public void setAfterHandlers(List<QCodeReference> afterHandlers)
   {
      this.afterHandlers = afterHandlers;
   }



   /*******************************************************************************
    ** Fluent setter for afterHandlers
    **
    ** @param afterHandlers List of code references to handlers executed after the route
    ** @return this
    *******************************************************************************/
   public JavalinRouteProviderMetaData withAfterHandlers(List<QCodeReference> afterHandlers)
   {
      this.afterHandlers = afterHandlers;
      return (this);
   }



   /*******************************************************************************
    ** Validate this route provider configuration against the QInstance.
    **
    ** Ensures that:
    ** - Referenced process names exist in the QInstance
    ** - Route authenticator code references are valid
    ** - Context handler code references are valid
    ** - Before and after handler code references are valid
    ** - IsolatedSpaRouteProvider fields are properly configured when used
    **
    ** @param qInstance the QInstance to validate against
    ** @param validator the validator to record validation errors
    *******************************************************************************/
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      String prefix = "In javalinRouteProvider '" + name + "', ";
      if(StringUtils.hasContent(processName))
      {
         validator.assertCondition(qInstance.getProcesses().containsKey(processName), prefix + "unrecognized process name: " + processName + " in a javalinRouteProvider");
      }

      if(routeAuthenticator != null)
      {
         validator.validateSimpleCodeReference(prefix + "routeAuthenticator ", routeAuthenticator, RouteAuthenticatorInterface.class);
      }

      if(contextHandler != null)
      {
         validator.validateSimpleCodeReference(prefix + "contextHandler ", contextHandler, RouteProviderContextHandlerInterface.class);
      }

      //////////////////////////////
      // Validate before handlers //
      //////////////////////////////
      if(beforeHandlers != null)
      {
         for(int i = 0; i < beforeHandlers.size(); i++)
         {
            QCodeReference handler = beforeHandlers.get(i);
            if(handler != null)
            {
               validator.validateSimpleCodeReference(prefix + "beforeHandlers[" + i + "] ", handler, RouteProviderBeforeHandlerInterface.class);
            }
         }
      }

      /////////////////////////////
      // Validate after handlers //
      /////////////////////////////
      if(afterHandlers != null)
      {
         for(int i = 0; i < afterHandlers.size(); i++)
         {
            QCodeReference handler = afterHandlers.get(i);
            if(handler != null)
            {
               validator.validateSimpleCodeReference(prefix + "afterHandlers[" + i + "] ", handler, RouteProviderAfterHandlerInterface.class);
            }
         }
      }

      ///////////////////////////////////////////////////////
      // Validate IsolatedSpaRouteProvider specific fields //
      ///////////////////////////////////////////////////////
      if(StringUtils.hasContent(spaPath) && StringUtils.hasContent(staticFilesPath))
      {
         validator.assertCondition(StringUtils.hasContent(spaIndexFile), prefix + "spaIndexFile is required when using IsolatedSpaRouteProvider");
      }
   }

}
