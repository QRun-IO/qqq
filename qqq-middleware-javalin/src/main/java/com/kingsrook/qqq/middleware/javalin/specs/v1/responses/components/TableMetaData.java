/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
