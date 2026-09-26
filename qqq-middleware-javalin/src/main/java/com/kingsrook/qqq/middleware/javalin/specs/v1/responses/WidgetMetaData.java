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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.WidgetDropdownData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIHasAdditionalProperties;


/*******************************************************************************
 **
 *******************************************************************************/
public class WidgetMetaData implements ToSchema
{
   @OpenAPIExclude()
   private QFrontendWidgetMetaData wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetMetaData(QFrontendWidgetMetaData wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetMetaData()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Unique name for this widget within the QQQ Instance")
   public String getName()
   {
      return (this.wrapped.getName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing name for this widget")
   public String getLabel()
   {
      return (this.wrapped.getLabel());
   }


   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The type of this widget.")
   // todo enum of the NAMES of the widget types??  or, can we just f'ing change to return the enum.name's?
   public String getType()
   {
      return this.wrapped.getType();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Frontend widget defaults, including explicit association bindings")
   @OpenAPIHasAdditionalProperties
   public Map<String, Serializable> getDefaultValues()
   {
      return (wrapped.getDefaultValues());
   }



   /*******************************************************************************
    ** Tooltip text for the widget.
    *******************************************************************************/
   @OpenAPIDescription("Tooltip text for the widget.")
   public String getTooltip()
   {
      return (wrapped.getTooltip());
   }



   /*******************************************************************************
    ** Name of the widget's icon.
    *******************************************************************************/
   @OpenAPIDescription("Name of the widget's icon.")
   public String getIcon()
   {
      return (wrapped.getIcon());
   }



   /*******************************************************************************
    ** Number of grid columns (of 12) the widget spans.
    *******************************************************************************/
   @OpenAPIDescription("Number of grid columns (of 12) the widget spans.")
   public Integer getGridColumns()
   {
      return (wrapped.getGridColumns());
   }



   /*******************************************************************************
    ** HTML shown below the widget.
    *******************************************************************************/
   @OpenAPIDescription("HTML shown below the widget.")
   public String getFooterHTML()
   {
      return (wrapped.getFooterHTML());
   }



   /*******************************************************************************
    ** Whether the widget is drawn as a card.
    *******************************************************************************/
   @OpenAPIDescription("Whether the widget is drawn as a card.")
   public boolean getIsCard()
   {
      return (wrapped.getIsCard());
   }



   /*******************************************************************************
    ** Minimum height of the widget (a CSS length).
    *******************************************************************************/
   @OpenAPIDescription("Minimum height of the widget (a CSS length).")
   public String getMinHeight()
   {
      return (wrapped.getMinHeight());
   }



   /*******************************************************************************
    ** Whether dropdown selections are remembered for the user.
    *******************************************************************************/
   @OpenAPIDescription("Whether dropdown selections are remembered for the user.")
   public boolean getStoreDropdownSelections()
   {
      return (wrapped.getStoreDropdownSelections());
   }



   /*******************************************************************************
    ** Dropdowns the widget offers to filter its data.
    *******************************************************************************/
   @OpenAPIDescription("Dropdowns the widget offers to filter its data.")
   public List<WidgetDropdownData> getDropdowns()
   {
      return (wrapped.getDropdowns());
   }



   /*******************************************************************************
    ** Whether the widget offers a reload button.
    *******************************************************************************/
   @OpenAPIDescription("Whether the widget offers a reload button.")
   public boolean getShowReloadButton()
   {
      return (wrapped.getShowReloadButton());
   }



   /*******************************************************************************
    ** Whether the widget offers an export button.
    *******************************************************************************/
   @OpenAPIDescription("Whether the widget offers an export button.")
   public boolean getShowExportButton()
   {
      return (wrapped.getShowExportButton());
   }



   /*******************************************************************************
    ** Icons by role (for example the widget header icon).
    *******************************************************************************/
   @OpenAPIDescription("Icons by role (for example the widget header icon).")
   @OpenAPIHasAdditionalProperties
   public Map<String, QIcon> getIcons()
   {
      return (wrapped.getIcons());
   }



   /*******************************************************************************
    ** Help content by role.
    *******************************************************************************/
   @OpenAPIDescription("Help content by role.")
   @OpenAPIHasAdditionalProperties
   public Map<String, List<QHelpContent>> getHelpContent()
   {
      return (wrapped.getHelpContent());
   }



   /*******************************************************************************
    ** Whether the user may render the widget.
    *******************************************************************************/
   @OpenAPIDescription("Whether the user may render the widget.")
   public boolean getHasPermission()
   {
      return (wrapped.getHasPermission());
   }

}
