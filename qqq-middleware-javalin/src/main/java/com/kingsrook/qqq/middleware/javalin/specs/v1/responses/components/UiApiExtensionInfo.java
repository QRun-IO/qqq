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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 ** A module or qbit extension of the UI API, as listed by the version endpoint.
 *******************************************************************************/
public class UiApiExtensionInfo implements ToSchema
{
   @OpenAPIDescription("The extension namespace, e.g. alerts or sql-workbench.")
   private String name;

   @OpenAPIDescription("The extension semantic version (MAJOR.MINOR.PATCH).")
   private String version;

   @OpenAPIDescription("The path prefix of the extension operations under the API base path, e.g. /qbit/backups or /esb.")
   private String pathPrefix;

   @OpenAPIDescription("A short description of the extension.")
   private String description;



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public UiApiExtensionInfo withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for version
    *******************************************************************************/
   public String getVersion()
   {
      return (this.version);
   }



   /*******************************************************************************
    ** Fluent setter for version
    *******************************************************************************/
   public UiApiExtensionInfo withVersion(String version)
   {
      this.version = version;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pathPrefix
    *******************************************************************************/
   public String getPathPrefix()
   {
      return (this.pathPrefix);
   }



   /*******************************************************************************
    ** Fluent setter for pathPrefix
    *******************************************************************************/
   public UiApiExtensionInfo withPathPrefix(String pathPrefix)
   {
      this.pathPrefix = pathPrefix;
      return (this);
   }



   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public UiApiExtensionInfo withDescription(String description)
   {
      this.description = description;
      return (this);
   }
}
