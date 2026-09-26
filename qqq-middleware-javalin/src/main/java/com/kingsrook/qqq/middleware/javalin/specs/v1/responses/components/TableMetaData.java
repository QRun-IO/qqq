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
import java.util.Map;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIIncludeProperties;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapValueType;


/***************************************************************************
 * API response component for full table metadata, extending
 * {@link TableMetaDataLight} with fields, sections, exposed joins,
 * supplemental metadata, sharing configuration, and virtual fields.
 ***************************************************************************/
@OpenAPIIncludeProperties(ancestorClasses = TableMetaDataLight.class)
public class TableMetaData extends TableMetaDataLight implements ToSchema
{


   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TableMetaData(QFrontendTableMetaData wrapped)
   {
      super(wrapped);
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TableMetaData()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Fields in this table")
   @OpenAPIMapValueType(value = FieldMetaData.class, useRef = true)
   public Map<String, FieldMetaData> getFields()
   {
      return (CollectionUtils.nonNullMap(this.wrapped.getFields()).values().stream()
         .collect(Collectors.toMap(f -> f.getName(), f -> new FieldMetaData(f))));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Name of the primary key field in this table")
   public String getPrimaryKeyField()
   {
      return (wrapped.getPrimaryKeyField());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Whether or not this table's backend uses variants.")
   public Boolean getUsesVariants()
   {
      return (wrapped.getUsesVariants());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Sections to organize fields on screens for this record")
   @OpenAPIListItems(value = TableSection.class, useRef = true)
   public List<TableSection> getSections()
   {
      if(wrapped.getSections() == null)
      {
         return (null);
      }

      return (wrapped.getSections().stream().map(s -> new TableSection(s)).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Sections to organize fields on screens for this record")
   @OpenAPIListItems(value = ExposedJoin.class, useRef = true)
   public List<ExposedJoin> getExposedJoins()
   {
      if(wrapped.getExposedJoins() == null)
      {
         return (null);
      }

      return (wrapped.getExposedJoins().stream().map(s -> new ExposedJoin(s)).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Named record associations and their joins, without child record access")
   @OpenAPIListItems(value = Association.class, useRef = true)
   public List<Association> getAssociations()
   {
      if(wrapped.getAssociations() == null)
      {
         return (null);
      }

      return (wrapped.getAssociations().stream().map(Association::new).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Additional meta data about the table, not necessarily known to QQQ.")
   public Map<String, QSupplementalTableMetaData> getSupplementalMetaData()
   {
      return (wrapped.getSupplementalTableMetaData());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("For tables that support the sharing feature, meta-data about the sharing setup.")
   @OpenAPIListItems(value = ShareableTableMetaData.class, useRef = false)
   public ShareableTableMetaData getShareableTableMetaData()
   {
      return (wrapped.getShareableTableMetaData());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Virtual fields in this table")
   @OpenAPIMapValueType(value = VirtualFieldMetaData.class, useRef = true)
   public Map<String, VirtualFieldMetaData> getVirtualFields()
   {
      return (CollectionUtils.nonNullMap(this.wrapped.getVirtualFields()).values().stream()
         .collect(Collectors.toMap(f -> f.getName(), f -> new VirtualFieldMetaData(f))));
   }


}
