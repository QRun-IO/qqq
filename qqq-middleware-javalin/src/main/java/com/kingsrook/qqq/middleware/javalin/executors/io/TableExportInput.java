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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;


/*******************************************************************************
 ** Input for the table export endpoint.
 *******************************************************************************/
public class TableExportInput extends AbstractMiddlewareInput
{
   private String       tableName;
   private String       format;
   private String       filename;
   private QQueryFilter filter;
   private List<String> fieldNames;
   private Integer      limit;
   private Boolean      includeHeaderRow;
   private TableVariant tableVariant;



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public TableExportInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for format
    *******************************************************************************/
   public String getFormat()
   {
      return (this.format);
   }



   /*******************************************************************************
    ** Setter for format
    *******************************************************************************/
   public void setFormat(String format)
   {
      this.format = format;
   }



   /*******************************************************************************
    ** Fluent setter for format
    *******************************************************************************/
   public TableExportInput withFormat(String format)
   {
      this.format = format;
      return (this);
   }



   /*******************************************************************************
    ** Getter for filename
    *******************************************************************************/
   public String getFilename()
   {
      return (this.filename);
   }



   /*******************************************************************************
    ** Setter for filename
    *******************************************************************************/
   public void setFilename(String filename)
   {
      this.filename = filename;
   }



   /*******************************************************************************
    ** Fluent setter for filename
    *******************************************************************************/
   public TableExportInput withFilename(String filename)
   {
      this.filename = filename;
      return (this);
   }



   /*******************************************************************************
    ** Getter for filter
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return (this.filter);
   }



   /*******************************************************************************
    ** Setter for filter
    *******************************************************************************/
   public void setFilter(QQueryFilter filter)
   {
      this.filter = filter;
   }



   /*******************************************************************************
    ** Fluent setter for filter
    *******************************************************************************/
   public TableExportInput withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldNames
    *******************************************************************************/
   public List<String> getFieldNames()
   {
      return (this.fieldNames);
   }



   /*******************************************************************************
    ** Setter for fieldNames
    *******************************************************************************/
   public void setFieldNames(List<String> fieldNames)
   {
      this.fieldNames = fieldNames;
   }



   /*******************************************************************************
    ** Fluent setter for fieldNames
    *******************************************************************************/
   public TableExportInput withFieldNames(List<String> fieldNames)
   {
      this.fieldNames = fieldNames;
      return (this);
   }



   /*******************************************************************************
    ** Getter for limit
    *******************************************************************************/
   public Integer getLimit()
   {
      return (this.limit);
   }



   /*******************************************************************************
    ** Setter for limit
    *******************************************************************************/
   public void setLimit(Integer limit)
   {
      this.limit = limit;
   }



   /*******************************************************************************
    ** Fluent setter for limit
    *******************************************************************************/
   public TableExportInput withLimit(Integer limit)
   {
      this.limit = limit;
      return (this);
   }



   /*******************************************************************************
    ** Getter for includeHeaderRow
    *******************************************************************************/
   public Boolean getIncludeHeaderRow()
   {
      return (this.includeHeaderRow);
   }



   /*******************************************************************************
    ** Setter for includeHeaderRow
    *******************************************************************************/
   public void setIncludeHeaderRow(Boolean includeHeaderRow)
   {
      this.includeHeaderRow = includeHeaderRow;
   }



   /*******************************************************************************
    ** Fluent setter for includeHeaderRow
    *******************************************************************************/
   public TableExportInput withIncludeHeaderRow(Boolean includeHeaderRow)
   {
      this.includeHeaderRow = includeHeaderRow;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableVariant
    *******************************************************************************/
   public TableVariant getTableVariant()
   {
      return (tableVariant);
   }



   /*******************************************************************************
    ** Setter for tableVariant
    *******************************************************************************/
   public void setTableVariant(TableVariant tableVariant)
   {
      this.tableVariant = tableVariant;
   }



   /*******************************************************************************
    ** Fluent setter for tableVariant
    *******************************************************************************/
   public TableExportInput withTableVariant(TableVariant tableVariant)
   {
      this.tableVariant = tableVariant;
      return (this);
   }

}
