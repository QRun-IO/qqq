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
import com.kingsrook.qqq.middleware.javalin.routeproviders.handlers.RouteProviderBeforeHandlerInterface;
import com.kingsrook.qqq.middleware.javalin.routeproviders.handlers.RouteProviderAfterHandlerInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class JavalinRouteProviderMetaData implements QMetaDataObject
{
   private String name;
   private String hostedPath;

   private String fileSystemPath;
   private String spaRootPath;
   private String spaRootFile;
   
   private String processName;
   
   // IsolatedSpaRouteProvider specific fields
   private String spaPath;
   private String staticFilesPath;
   private String spaIndexFile;
   private List<String> excludedPaths;
   private boolean enableDeepLinking = true;
   private boolean loadFromJar = false;

   private List<String> methods;

   private QCodeReference routeAuthenticator;
   private QCodeReference contextHandler;
   
   // IsolatedSpaRouteProvider handler support
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
    *******************************************************************************/
   public String getHostedPath()
   {
      return (this.hostedPath);
   }



   /*******************************************************************************
    ** Setter for hostedPath
    *******************************************************************************/
   public void setHostedPath(String hostedPath)
   {
      this.hostedPath = hostedPath;
   }



   /*******************************************************************************
    ** Fluent setter for hostedPath
    *******************************************************************************/
   public JavalinRouteProviderMetaData withHostedPath(String hostedPath)
   {
      this.hostedPath = hostedPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fileSystemPath
    *******************************************************************************/
   public String getFileSystemPath()
   {
      return (this.fileSystemPath);
   }



   /*******************************************************************************
    ** Setter for fileSystemPath
    *******************************************************************************/
   public void setFileSystemPath(String fileSystemPath)
   {
      this.fileSystemPath = fileSystemPath;
   }



   /*******************************************************************************
    ** Fluent setter for fileSystemPath
    *******************************************************************************/
   public JavalinRouteProviderMetaData withFileSystemPath(String fileSystemPath)
   {
      this.fileSystemPath = fileSystemPath;
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
   public JavalinRouteProviderMetaData withSpaRootPath(String spaRootPath)
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
   public JavalinRouteProviderMetaData withSpaRootFile(String spaRootFile)
   {
      this.spaRootFile = spaRootFile;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    ** Setter for processName
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    ** Fluent setter for processName
    *******************************************************************************/
   public JavalinRouteProviderMetaData withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for methods
    *******************************************************************************/
   public List<String> getMethods()
   {
      return (this.methods);
   }



   /*******************************************************************************
    ** Setter for methods
    *******************************************************************************/
   public void setMethods(List<String> methods)
   {
      this.methods = methods;
   }



   /*******************************************************************************
    ** Fluent setter for methods
    *******************************************************************************/
   public JavalinRouteProviderMetaData withMethods(List<String> methods)
   {
      this.methods = methods;
      return (this);
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
   public JavalinRouteProviderMetaData withRouteAuthenticator(QCodeReference routeAuthenticator)
   {
      this.routeAuthenticator = routeAuthenticator;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contextHandler
    *******************************************************************************/
   public QCodeReference getContextHandler()
   {
      return (this.contextHandler);
   }



   /*******************************************************************************
    ** Setter for contextHandler
    *******************************************************************************/
   public void setContextHandler(QCodeReference contextHandler)
   {
      this.contextHandler = contextHandler;
   }



   /*******************************************************************************
    ** Fluent setter for contextHandler
    *******************************************************************************/
   public JavalinRouteProviderMetaData withContextHandler(QCodeReference contextHandler)
   {
      this.contextHandler = contextHandler;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public JavalinRouteProviderMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for spaPath
    *******************************************************************************/
   public String getSpaPath()
   {
      return (this.spaPath);
   }



   /*******************************************************************************
    ** Setter for spaPath
    *******************************************************************************/
   public void setSpaPath(String spaPath)
   {
      this.spaPath = spaPath;
   }



   /*******************************************************************************
    ** Fluent setter for spaPath
    *******************************************************************************/
   public JavalinRouteProviderMetaData withSpaPath(String spaPath)
   {
      this.spaPath = spaPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for staticFilesPath
    *******************************************************************************/
   public String getStaticFilesPath()
   {
      return (this.staticFilesPath);
   }



   /*******************************************************************************
    ** Setter for staticFilesPath
    *******************************************************************************/
   public void setStaticFilesPath(String staticFilesPath)
   {
      this.staticFilesPath = staticFilesPath;
   }



   /*******************************************************************************
    ** Fluent setter for staticFilesPath
    *******************************************************************************/
   public JavalinRouteProviderMetaData withStaticFilesPath(String staticFilesPath)
   {
      this.staticFilesPath = staticFilesPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for spaIndexFile
    *******************************************************************************/
   public String getSpaIndexFile()
   {
      return (this.spaIndexFile);
   }



   /*******************************************************************************
    ** Setter for spaIndexFile
    *******************************************************************************/
   public void setSpaIndexFile(String spaIndexFile)
   {
      this.spaIndexFile = spaIndexFile;
   }



   /*******************************************************************************
    ** Fluent setter for spaIndexFile
    *******************************************************************************/
   public JavalinRouteProviderMetaData withSpaIndexFile(String spaIndexFile)
   {
      this.spaIndexFile = spaIndexFile;
      return (this);
   }



   /*******************************************************************************
    ** Getter for excludedPaths
    *******************************************************************************/
   public List<String> getExcludedPaths()
   {
      return (this.excludedPaths);
   }



   /*******************************************************************************
    ** Setter for excludedPaths
    *******************************************************************************/
   public void setExcludedPaths(List<String> excludedPaths)
   {
      this.excludedPaths = excludedPaths;
   }



   /*******************************************************************************
    ** Fluent setter for excludedPaths
    *******************************************************************************/
   public JavalinRouteProviderMetaData withExcludedPaths(List<String> excludedPaths)
   {
      this.excludedPaths = excludedPaths;
      return (this);
   }



   /*******************************************************************************
    ** Getter for enableDeepLinking
    *******************************************************************************/
   public boolean getEnableDeepLinking()
   {
      return (this.enableDeepLinking);
   }



   /*******************************************************************************
    ** Setter for enableDeepLinking
    *******************************************************************************/
   public void setEnableDeepLinking(boolean enableDeepLinking)
   {
      this.enableDeepLinking = enableDeepLinking;
   }



   /*******************************************************************************
    ** Fluent setter for enableDeepLinking
    *******************************************************************************/
   public JavalinRouteProviderMetaData withEnableDeepLinking(boolean enableDeepLinking)
   {
      this.enableDeepLinking = enableDeepLinking;
      return (this);
   }



   /*******************************************************************************
    ** Getter for loadFromJar
    *******************************************************************************/
   public boolean getLoadFromJar()
   {
      return (this.loadFromJar);
   }



   /*******************************************************************************
    ** Setter for loadFromJar
    *******************************************************************************/
   public void setLoadFromJar(boolean loadFromJar)
   {
      this.loadFromJar = loadFromJar;
   }



   /*******************************************************************************
    ** Fluent setter for loadFromJar
    *******************************************************************************/
   public JavalinRouteProviderMetaData withLoadFromJar(boolean loadFromJar)
   {
      this.loadFromJar = loadFromJar;
      return (this);
   }



   /*******************************************************************************
    ** Getter for beforeHandlers
    *******************************************************************************/
   public List<QCodeReference> getBeforeHandlers()
   {
      return (this.beforeHandlers);
   }



   /*******************************************************************************
    ** Setter for beforeHandlers
    *******************************************************************************/
   public void setBeforeHandlers(List<QCodeReference> beforeHandlers)
   {
      this.beforeHandlers = beforeHandlers;
   }



   /*******************************************************************************
    ** Fluent setter for beforeHandlers
    *******************************************************************************/
   public JavalinRouteProviderMetaData withBeforeHandlers(List<QCodeReference> beforeHandlers)
   {
      this.beforeHandlers = beforeHandlers;
      return (this);
   }



   /*******************************************************************************
    ** Getter for afterHandlers
    *******************************************************************************/
   public List<QCodeReference> getAfterHandlers()
   {
      return (this.afterHandlers);
   }



   /*******************************************************************************
    ** Setter for afterHandlers
    *******************************************************************************/
   public void setAfterHandlers(List<QCodeReference> afterHandlers)
   {
      this.afterHandlers = afterHandlers;
   }



   /*******************************************************************************
    ** Fluent setter for afterHandlers
    *******************************************************************************/
   public JavalinRouteProviderMetaData withAfterHandlers(List<QCodeReference> afterHandlers)
   {
      this.afterHandlers = afterHandlers;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
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

      // Validate before handlers
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

      // Validate after handlers
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

      // Validate IsolatedSpaRouteProvider specific fields
      if(StringUtils.hasContent(spaPath) && StringUtils.hasContent(staticFilesPath))
      {
         validator.assertCondition(StringUtils.hasContent(spaIndexFile), prefix + "spaIndexFile is required when using IsolatedSpaRouteProvider");
      }
   }

}
