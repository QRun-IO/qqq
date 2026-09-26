/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapKnownEntries;


/***************************************************************************
 **
 ***************************************************************************/
public class AppSection implements ToSchema
{
   @OpenAPIExclude()
   private QAppSection wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public AppSection(QAppSection wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public AppSection()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Unique (within the app) name for this section.")
   public String getName()
   {
      return (this.wrapped.getName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing name of the section.")
   public String getLabel()
   {
      return (this.wrapped.getLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Icon to display for the section.")
   @OpenAPIMapKnownEntries(value = Icon.class, useRef = true)
   public Icon getIcon()
   {
      return (this.wrapped.getIcon() == null ? null : new Icon(this.wrapped.getIcon()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("List of table names for the section")
   @OpenAPIListItems(value = String.class)
   public List<String> getTables()
   {
      return (this.wrapped.getTables());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("List of process names for the section")
   @OpenAPIListItems(value = String.class)
   public List<String> getProcesses()
   {
      return (this.wrapped.getProcesses());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("List of report names for the section")
   @OpenAPIListItems(value = String.class)
   public List<String> getReports()
   {
      return (this.wrapped.getReports());
   }

}
