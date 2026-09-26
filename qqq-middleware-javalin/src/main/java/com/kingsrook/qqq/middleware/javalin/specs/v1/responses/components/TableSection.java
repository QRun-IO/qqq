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
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/***************************************************************************
 **
 ***************************************************************************/
public class TableSection implements ToSchema
{
   @OpenAPIExclude()
   private QFieldSection wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TableSection(QFieldSection section)
   {
      this.wrapped = section;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TableSection()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Unique identifier for this section within this table")
   public String getName()
   {
      return (this.wrapped.getName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing label to display for this section")
   public String getLabel()
   {
      return (this.wrapped.getLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Importance of this section (T1, T2, or T3)")
   public String getTier()
   {
      return (this.wrapped.getTier().name());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("List of field names to include in this section.")
   public List<String> getFieldNames()
   {
      return (this.wrapped.getFieldNames());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Name of a widget in this QQQ instance to include in this section (instead of fields).")
   public String getWidgetName()
   {
      return (this.wrapped.getWidgetName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Icon to display for the table")
   public Icon getIcon()
   {
      return (this.wrapped.getIcon() == null ? null : new Icon(this.wrapped.getIcon()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Whether or not to hide this section")
   public Boolean isHidden()
   {
      return (this.wrapped.getIsHidden());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Layout suggestion, for how many columns of a 12-grid this section should use.")
   public Integer getGridColumns()
   {
      return (this.wrapped.getGridColumns());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Help Contents for this section table.") // todo describe more
   public List<QHelpContent> getHelpContents()
   {
      return (this.wrapped.getHelpContents());
   }

}
