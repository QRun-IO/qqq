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

package com.kingsrook.qqq.middleware.javalin.specs.extensions;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;


/*******************************************************************************
 ** A set of endpoint specs that a module or qbit adds to the versioned UI API
 ** (`/qqq/v1`), under one namespace.  The specs are ordinary
 ** AbstractEndpointSpec classes; their paths must begin with the extension's
 ** path prefix (by default `/qbit/{name}`).  See docs/UI-API-EXTENSIONS.md.
 *******************************************************************************/
public class MiddlewareSpecExtension implements QMetaDataObject
{
   private String               name;
   private String               description;
   private String               semanticVersion = "1.0.0";
   private String               pathPrefix;
   private List<QCodeReference> specs           = new ArrayList<>();



   /*******************************************************************************
    ** The path prefix in effect: the explicit one, or `/qbit/{name}`.  Always
    ** begins with a slash and never ends with one.
    *******************************************************************************/
   public String getEffectivePathPrefix()
   {
      String prefix = StringUtils.hasContent(pathPrefix) ? pathPrefix : "/qbit/" + name;
      prefix = ("/" + prefix).replaceAll("/+", "/");
      return (prefix.replaceFirst("/+$", ""));
   }



   /*******************************************************************************
    ** Add a spec class.  Adding the same class again is a no-op.
    *******************************************************************************/
   public MiddlewareSpecExtension withSpec(Class<? extends AbstractEndpointSpec<?, ?, ?>> specClass)
   {
      return (withSpec(new QCodeReference(specClass)));
   }



   /*******************************************************************************
    ** Add a spec by code reference.  Adding the same class again is a no-op.
    *******************************************************************************/
   public MiddlewareSpecExtension withSpec(QCodeReference specReference)
   {
      if(specReference != null && specs.stream().noneMatch(existing -> Objects.equals(existing.getName(), specReference.getName())))
      {
         specs.add(specReference);
      }
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
    ** Fluent setter for name - the namespace, lowercase a-z, 0-9 and '-'.
    *******************************************************************************/
   public MiddlewareSpecExtension withName(String name)
   {
      this.name = name;
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
    ** Setter for description
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public MiddlewareSpecExtension withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for semanticVersion
    *******************************************************************************/
   public String getSemanticVersion()
   {
      return (this.semanticVersion);
   }



   /*******************************************************************************
    ** Setter for semanticVersion
    *******************************************************************************/
   public void setSemanticVersion(String semanticVersion)
   {
      this.semanticVersion = semanticVersion;
   }



   /*******************************************************************************
    ** Fluent setter for semanticVersion - the extension's own MAJOR.MINOR.PATCH.
    *******************************************************************************/
   public MiddlewareSpecExtension withSemanticVersion(String semanticVersion)
   {
      this.semanticVersion = semanticVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pathPrefix (as set; see getEffectivePathPrefix)
    *******************************************************************************/
   public String getPathPrefix()
   {
      return (this.pathPrefix);
   }



   /*******************************************************************************
    ** Setter for pathPrefix
    *******************************************************************************/
   public void setPathPrefix(String pathPrefix)
   {
      this.pathPrefix = pathPrefix;
   }



   /*******************************************************************************
    ** Fluent setter for pathPrefix.  Only the established module prefixes in
    ** MiddlewareSpecExtensionsMetaData.ESTABLISHED_PREFIXES are allowed; new
    ** extensions use the default `/qbit/{name}`.
    *******************************************************************************/
   public MiddlewareSpecExtension withPathPrefix(String pathPrefix)
   {
      this.pathPrefix = pathPrefix;
      return (this);
   }



   /*******************************************************************************
    ** Getter for specs
    *******************************************************************************/
   public List<QCodeReference> getSpecs()
   {
      return (this.specs);
   }



   /*******************************************************************************
    ** Setter for specs
    *******************************************************************************/
   public void setSpecs(List<QCodeReference> specs)
   {
      this.specs = specs == null ? new ArrayList<>() : new ArrayList<>(specs);
   }
}
