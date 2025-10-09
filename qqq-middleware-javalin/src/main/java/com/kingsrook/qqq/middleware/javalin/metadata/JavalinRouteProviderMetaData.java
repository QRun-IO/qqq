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

   private List<String> methods;

   private QCodeReference routeAuthenticator;
   private QCodeReference contextHandler;



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
   }

}
