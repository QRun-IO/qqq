/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.frontend;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.WidgetDropdownData;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;


/*******************************************************************************
 * Version of QWidgetMetaData that's meant for transmitting to a frontend.
 * e.g., it excludes backend-only details (when/if we have them)
 *
 *******************************************************************************/
@JsonInclude(Include.NON_NULL)
public class QFrontendWidgetMetaData
{
   private final String                   name;
   private final String                   label;
   private final String                   tooltip;
   private final String                   type;
   private final String                   icon;
   private final Integer                  gridColumns;
   private final String                   footerHTML;
   private final boolean                  isCard;
   private final String                   minHeight;
   private final boolean                  storeDropdownSelections;
   private final List<WidgetDropdownData> dropdowns;

   private boolean showReloadButton = false;
   private boolean showExportButton = false;

   protected Map<String, QIcon>              icons;
   protected Map<String, List<QHelpContent>> helpContent;
   protected Map<String, Serializable>       defaultValues;

   private final boolean hasPermission;

   //////////////////////////////////////////////////////////////////////////////////
   // DO add getters for all fields - this tells Jackson to include them in JSON.  //
   // do NOT add setters.  take values from the source-object in the constructor!! //
   //////////////////////////////////////////////////////////////////////////////////



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFrontendWidgetMetaData(AbstractActionInput actionInput, QWidgetMetaDataInterface widgetMetaData)
   {
      this.name = widgetMetaData.getName();
      this.label = widgetMetaData.getLabel();
      this.tooltip = widgetMetaData.getTooltip();
      this.type = widgetMetaData.getType();
      this.icon = widgetMetaData.getIcon();
      this.gridColumns = widgetMetaData.getGridColumns();
      this.isCard = widgetMetaData.getIsCard();
      this.footerHTML = widgetMetaData.getFooterHTML();
      this.minHeight = widgetMetaData.getMinHeight();
      this.dropdowns = widgetMetaData.getDropdowns();
      this.storeDropdownSelections = widgetMetaData.getStoreDropdownSelections();

      if(widgetMetaData instanceof QWidgetMetaData qWidgetMetaData)
      {
         this.showExportButton = qWidgetMetaData.getShowExportButton();
         this.showReloadButton = qWidgetMetaData.getShowReloadButton();
         this.icons = qWidgetMetaData.getIcons();
      }

      this.helpContent = widgetMetaData.getHelpContent();
      this.defaultValues = widgetMetaData.getDefaultValues();

      hasPermission = PermissionsHelper.hasWidgetPermission(actionInput, name);
   }



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Getter for gridColumns
    **
    *******************************************************************************/
   public Integer getGridColumns()
   {
      return gridColumns;
   }



   /*******************************************************************************
    ** Getter for footerHTML
    **
    *******************************************************************************/
   public String getFooterHTML()
   {
      return footerHTML;
   }



   /*******************************************************************************
    ** Getter for isCard
    **
    *******************************************************************************/
   public boolean getIsCard()
   {
      return isCard;
   }



   /*******************************************************************************
    ** Getter for minHeight
    **
    *******************************************************************************/
   public String getMinHeight()
   {
      return minHeight;
   }



   /*******************************************************************************
    ** Getter for isCard
    **
    *******************************************************************************/
   public List<WidgetDropdownData> getDropdowns()
   {
      return dropdowns;
   }



   /*******************************************************************************
    ** Getter for icon
    **
    *******************************************************************************/
   public String getIcon()
   {
      return icon;
   }



   /*******************************************************************************
    ** Getter for hasPermission
    **
    *******************************************************************************/
   public boolean getHasPermission()
   {
      return hasPermission;
   }



   /*******************************************************************************
    ** Getter for storeDropdownSelections
    **
    *******************************************************************************/
   public boolean getStoreDropdownSelections()
   {
      return storeDropdownSelections;
   }



   /*******************************************************************************
    ** Getter for showReloadButton
    **
    *******************************************************************************/
   public boolean getShowReloadButton()
   {
      return showReloadButton;
   }



   /*******************************************************************************
    ** Getter for showExportButton
    **
    *******************************************************************************/
   public boolean getShowExportButton()
   {
      return showExportButton;
   }



   /*******************************************************************************
    ** Getter for icons
    **
    *******************************************************************************/
   public Map<String, QIcon> getIcons()
   {
      return icons;
   }



   /*******************************************************************************
    ** Getter for tooltip
    **
    *******************************************************************************/
   public String getTooltip()
   {
      return tooltip;
   }



   /*******************************************************************************
    ** Getter for helpContent
    **
    *******************************************************************************/
   public Map<String, List<QHelpContent>> getHelpContent()
   {
      return helpContent;
   }



   /*******************************************************************************
    ** Getter for defaultValues
    **
    *******************************************************************************/
   public Map<String, Serializable> getDefaultValues()
   {
      return defaultValues;
   }

}
