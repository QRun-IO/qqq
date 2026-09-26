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
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapKnownEntries;


/***************************************************************************
 **
 ***************************************************************************/
public class AppTreeNode implements ToSchema
{
   @OpenAPIExclude()
   private com.kingsrook.qqq.backend.core.model.metadata.frontend.AppTreeNode wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public AppTreeNode(com.kingsrook.qqq.backend.core.model.metadata.frontend.AppTreeNode wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public AppTreeNode()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The type of node (table, process, report, app)")
   public String getType()
   {
      return (this.wrapped.getType() == null ? null : this.wrapped.getType().name());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Unique (within its type) name for this element.  e.g., for type = 'table', the table's name.")
   public String getName()
   {
      return (this.wrapped.getName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing name of the element.")
   public String getLabel()
   {
      return (this.wrapped.getLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Child elements.  Only applies for type='app', which contains additional apps under it")
   @OpenAPIListItems(value = AppTreeNode.class, useRef = true)
   public List<AppTreeNode> getChildren()
   {
      return (CollectionUtils.nonNullList(this.wrapped.getChildren()).stream().map(a -> new AppTreeNode(a)).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Icon to display for the item.")
   @OpenAPIMapKnownEntries(value = Icon.class, useRef = true)
   public Icon getIcon()
   {
      return (this.wrapped.getIcon() == null ? null : new Icon(this.wrapped.getIcon()));
   }

}
