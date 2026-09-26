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


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendAppMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapKnownEntries;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapValueType;


/***************************************************************************
 **
 ***************************************************************************/
public class AppMetaData implements ToSchema
{
   @OpenAPIExclude()
   private QFrontendAppMetaData wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public AppMetaData(QFrontendAppMetaData wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public AppMetaData()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Unique name for this app within the QQQ Instance")
   public String getName()
   {
      return (this.wrapped.getName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing name for this app")
   public String getLabel()
   {
      return (this.wrapped.getLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Icon to display for the app.")
   @OpenAPIMapKnownEntries(value = Icon.class, useRef = true)
   public Icon getIcon()
   {
      return (this.wrapped.getIcon() == null ? null : new Icon(this.wrapped.getIcon()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("List of widgets names that are part of this app.  These strings should be keys to the widgets map in the QQQ Instance.")
   @OpenAPIListItems(value = String.class)
   public List<String> getWidgets()
   {
      return (this.wrapped.getWidgets());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("List of other apps, tables, process, and reports, which are contained within this app.")
   @OpenAPIListItems(value = AppTreeNode.class, useRef = true)
   public List<AppTreeNode> getChildren()
   {
      return (CollectionUtils.nonNullList(this.wrapped.getChildren()).stream().map(a -> new AppTreeNode(a)).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Map of other apps, tables, process, and reports, which are contained within this app.  Same contents as the children list, just structured as a map.")
   @OpenAPIMapValueType(value = AppTreeNode.class, useRef = true)
   public Map<String, AppTreeNode> getChildMap()
   {
      return (CollectionUtils.nonNullMap(this.wrapped.getChildMap()).entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> new AppTreeNode(e.getValue()))));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("List of sections - sub-divisions of the app, to further organize its children.")
   @OpenAPIListItems(value = AppSection.class, useRef = true) // todo local type
   public List<AppSection> getSections()
   {
      return (CollectionUtils.nonNullList(this.wrapped.getSections()).stream().map(s -> new AppSection(s)).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Additional meta-data describing the app, which may not be known to the QQQ backend core module.")
   public Map<String, Object> getSupplementalAppMetaData()
   {
      return (new LinkedHashMap<>(CollectionUtils.nonNullMap(this.wrapped.getSupplementalAppMetaData())));
   }

}
