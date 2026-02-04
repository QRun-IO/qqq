/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.dashboard;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.instances.QInstanceHelpContentManager;
import com.kingsrook.qqq.backend.core.instances.validation.plugins.QInstanceValidatorPluginInterface;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.layout.CollapsibleMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;


/*******************************************************************************
 ** Base metadata for frontend dashboard widgets
 **
 *******************************************************************************/
public class QWidgetMetaData implements QWidgetMetaDataInterface
{
   protected String         name;
   protected String         icon;
   protected String         label;
   protected String         tooltip;
   protected String         type;
   protected String         minHeight;
   protected String         footerHTML;
   protected boolean        isCard;
   protected Integer        gridColumns;
   protected QCodeReference codeReference;

   private QPermissionRules permissionRules;

   private List<WidgetDropdownData> dropdowns;
   private boolean                  storeDropdownSelections;

   private boolean showReloadButton = true;
   private boolean showExportButton = false;

   protected Map<String, QIcon> icons;

   protected Map<String, List<QHelpContent>> helpContent;

   protected Map<String, Serializable> defaultValues = new LinkedHashMap<>();

   protected QInstanceValidatorPluginInterface<QWidgetMetaDataInterface> validatorPlugin;

   protected CollapsibleMetaData collapsible;

   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public QWidgetMetaData withName(String name)
   {
      this.name = name;
      return (this);
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
    ** Setter for icon
    **
    *******************************************************************************/
   public void setIcon(String icon)
   {
      this.icon = icon;
   }



   /*******************************************************************************
    ** Fluent setter for icon
    **
    *******************************************************************************/
   public QWidgetMetaData withIcon(String icon)
   {
      this.icon = icon;
      return (this);
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
    ** Setter for label
    **
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    **
    *******************************************************************************/
   public QWidgetMetaData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for codeReference
    **
    *******************************************************************************/
   public QCodeReference getCodeReference()
   {
      return codeReference;
   }



   /*******************************************************************************
    ** Setter for codeReference
    **
    *******************************************************************************/
   public void setCodeReference(QCodeReference codeReference)
   {
      this.codeReference = codeReference;
   }



   /*******************************************************************************
    ** Fluent setter for codeReference
    **
    *******************************************************************************/
   public QWidgetMetaData withCodeReference(QCodeReference codeReference)
   {
      this.codeReference = codeReference;
      return (this);
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
    ** Setter for type
    **
    *******************************************************************************/
   public void setType(String type)
   {
      this.type = type;

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // originally, showExportButton defaulted to true, and only a few frontend components knew how to render it. //
      // but, with the advent of csvData that any widget type can export, then the generic frontend widget code    //
      // became aware of the export button, so we wanted to flip the default for showExportButton to false, but    //
      // still have it by-default be true for these 2 types                                                        //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(WidgetType.TABLE.getType().equals(type) || WidgetType.CHILD_RECORD_LIST.getType().equals(type))
      {
         setShowExportButton(true);
      }
   }



   /*******************************************************************************
    ** Fluent setter for type
    **
    *******************************************************************************/
   public QWidgetMetaData withType(String type)
   {
      setType(type);
      return (this);
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
    ** Setter for gridColumns
    **
    *******************************************************************************/
   public void setGridColumns(Integer gridColumns)
   {
      this.gridColumns = gridColumns;
   }



   /*******************************************************************************
    ** Fluent setter for gridColumns
    **
    *******************************************************************************/
   public QWidgetMetaData withGridColumns(Integer gridColumns)
   {
      this.gridColumns = gridColumns;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultValues
    **
    *******************************************************************************/
   public Map<String, Serializable> getDefaultValues()
   {
      return defaultValues;
   }



   /*******************************************************************************
    ** Setter for defaultValues
    **
    *******************************************************************************/
   public void setDefaultValues(Map<String, Serializable> defaultValues)
   {
      this.defaultValues = defaultValues;
   }



   /*******************************************************************************
    ** Fluent setter for defaultValues
    **
    *******************************************************************************/
   public QWidgetMetaData withDefaultValues(Map<String, Serializable> defaultValues)
   {
      this.defaultValues = defaultValues;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for a single defaultValue
    **
    *******************************************************************************/
   public QWidgetMetaData withDefaultValue(String key, Serializable value)
   {
      if(this.defaultValues == null)
      {
         this.defaultValues = new LinkedHashMap<>();
      }

      this.defaultValues.put(key, value);

      return (this);
   }



   /*******************************************************************************
    ** Getter for dropdowns
    **
    *******************************************************************************/
   public List<WidgetDropdownData> getDropdowns()
   {
      return dropdowns;
   }



   /*******************************************************************************
    ** Setter for dropdowns
    **
    *******************************************************************************/
   public void setDropdowns(List<WidgetDropdownData> dropdowns)
   {
      this.dropdowns = dropdowns;
   }



   /*******************************************************************************
    ** Fluent setter for dropdowns
    **
    *******************************************************************************/
   public QWidgetMetaData withDropdowns(List<WidgetDropdownData> dropdowns)
   {
      this.dropdowns = dropdowns;
      return (this);
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
    ** Setter for storeDropdownSelections
    **
    *******************************************************************************/
   public void setStoreDropdownSelections(boolean storeDropdownSelections)
   {
      this.storeDropdownSelections = storeDropdownSelections;
   }



   /*******************************************************************************
    ** Fluent setter for storeDropdownSelections
    **
    *******************************************************************************/
   public QWidgetMetaData withStoreDropdownSelections(boolean storeDropdownSelections)
   {
      this.storeDropdownSelections = storeDropdownSelections;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for dropdowns
    **
    *******************************************************************************/
   public QWidgetMetaData withDropdown(WidgetDropdownData dropdown)
   {
      if(this.dropdowns == null)
      {
         this.dropdowns = new ArrayList<>();
      }
      this.dropdowns.add(dropdown);
      return (this);
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
    ** Setter for isCard
    **
    *******************************************************************************/
   public void setIsCard(boolean isCard)
   {
      this.isCard = isCard;
   }



   /*******************************************************************************
    ** Fluent setter for isCard
    **
    *******************************************************************************/
   public QWidgetMetaData withIsCard(boolean isCard)
   {
      this.isCard = isCard;
      return (this);
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
    ** Setter for minHeight
    **
    *******************************************************************************/
   public void setMinHeight(String minHeight)
   {
      this.minHeight = minHeight;
   }



   /*******************************************************************************
    ** Fluent setter for minHeight
    **
    *******************************************************************************/
   public QWidgetMetaData withMinHeight(String minHeight)
   {
      this.minHeight = minHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissionRules
    *******************************************************************************/
   @Override
   public QPermissionRules getPermissionRules()
   {
      return (this.permissionRules);
   }



   /*******************************************************************************
    ** Setter for permissionRules
    *******************************************************************************/
   @Override
   public void setPermissionRules(QPermissionRules permissionRules)
   {
      this.permissionRules = permissionRules;
   }



   /*******************************************************************************
    ** Fluent setter for permissionRules
    *******************************************************************************/
   public QWidgetMetaData withPermissionRules(QPermissionRules permissionRules)
   {
      this.permissionRules = permissionRules;
      return (this);
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
    ** Setter for footerHTML
    **
    *******************************************************************************/
   public void setFooterHTML(String footerHTML)
   {
      this.footerHTML = footerHTML;
   }



   /*******************************************************************************
    ** Fluent setter for footerHTML
    **
    *******************************************************************************/
   public QWidgetMetaData withFooterHTML(String footerHTML)
   {
      this.footerHTML = footerHTML;
      return (this);
   }



   /*******************************************************************************
    ** Getter for showReloadButton
    *******************************************************************************/
   public boolean getShowReloadButton()
   {
      return (this.showReloadButton);
   }



   /*******************************************************************************
    ** Setter for showReloadButton
    *******************************************************************************/
   public void setShowReloadButton(boolean showReloadButton)
   {
      this.showReloadButton = showReloadButton;
   }



   /*******************************************************************************
    ** Fluent setter for showReloadButton
    *******************************************************************************/
   public QWidgetMetaData withShowReloadButton(boolean showReloadButton)
   {
      this.showReloadButton = showReloadButton;
      return (this);
   }



   /*******************************************************************************
    ** Getter for showExportButton
    *******************************************************************************/
   public boolean getShowExportButton()
   {
      return (this.showExportButton);
   }



   /*******************************************************************************
    ** Setter for showExportButton
    *******************************************************************************/
   public void setShowExportButton(boolean showExportButton)
   {
      this.showExportButton = showExportButton;
   }



   /*******************************************************************************
    ** Fluent setter for showExportButton
    *******************************************************************************/
   public QWidgetMetaData withShowExportButton(boolean showExportButton)
   {
      this.showExportButton = showExportButton;
      return (this);
   }



   /*******************************************************************************
    ** Getter for icons
    *******************************************************************************/
   public Map<String, QIcon> getIcons()
   {
      return (this.icons);
   }



   /*******************************************************************************
    ** Setter for icons
    *******************************************************************************/
   public void setIcons(Map<String, QIcon> icons)
   {
      this.icons = icons;
   }



   /*******************************************************************************
    ** Fluent setter for icons
    *******************************************************************************/
   public QWidgetMetaData withIcon(String role, QIcon icon)
   {
      if(this.icons == null)
      {
         this.icons = new LinkedHashMap<>();
      }
      this.icons.put(role, icon);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for icons
    *******************************************************************************/
   public QWidgetMetaData withIcons(Map<String, QIcon> icons)
   {
      this.icons = icons;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tooltip
    *******************************************************************************/
   public String getTooltip()
   {
      return (this.tooltip);
   }



   /*******************************************************************************
    ** Setter for tooltip
    *******************************************************************************/
   public void setTooltip(String tooltip)
   {
      this.tooltip = tooltip;
   }



   /*******************************************************************************
    ** Fluent setter for tooltip
    *******************************************************************************/
   public QWidgetMetaData withTooltip(String tooltip)
   {
      this.tooltip = tooltip;
      return (this);
   }



   /*******************************************************************************
    ** Getter for helpContent
    *******************************************************************************/
   public Map<String, List<QHelpContent>> getHelpContent()
   {
      return (this.helpContent);
   }



   /*******************************************************************************
    ** Setter for helpContent
    *******************************************************************************/
   public void setHelpContent(Map<String, List<QHelpContent>> helpContent)
   {
      this.helpContent = helpContent;
   }



   /*******************************************************************************
    ** Fluent setter for helpContent
    *******************************************************************************/
   public QWidgetMetaData withHelpContent(Map<String, List<QHelpContent>> helpContent)
   {
      this.helpContent = helpContent;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for adding 1 helpContent (for a slot)
    *******************************************************************************/
   public QWidgetMetaData withHelpContent(String slot, QHelpContent helpContent)
   {
      if(this.helpContent == null)
      {
         this.helpContent = new HashMap<>();
      }

      List<QHelpContent> listForSlot = this.helpContent.computeIfAbsent(slot, (k) -> new ArrayList<>());
      QInstanceHelpContentManager.putHelpContentInList(helpContent, listForSlot);

      return (this);
   }



   /*******************************************************************************
    ** remove a helpContent for a slot based on its set of roles
    *******************************************************************************/
   public void removeHelpContent(String slot, Set<HelpRole> roles)
   {
      if(this.helpContent == null)
      {
         return;
      }

      List<QHelpContent> listForSlot = this.helpContent.get(slot);
      if(listForSlot == null)
      {
         return;
      }

      QInstanceHelpContentManager.removeHelpContentByRoleSetFromList(roles, listForSlot);
   }


   /*******************************************************************************
    ** Getter for validatorPlugin
    *******************************************************************************/
   public QInstanceValidatorPluginInterface<QWidgetMetaDataInterface> getValidatorPlugin()
   {
      return (this.validatorPlugin);
   }



   /*******************************************************************************
    ** Setter for validatorPlugin
    *******************************************************************************/
   public void setValidatorPlugin(QInstanceValidatorPluginInterface<QWidgetMetaDataInterface> validatorPlugin)
   {
      this.validatorPlugin = validatorPlugin;
   }



   /*******************************************************************************
    ** Fluent setter for validatorPlugin
    *******************************************************************************/
   public QWidgetMetaData withValidatorPlugin(QInstanceValidatorPluginInterface<QWidgetMetaDataInterface> validatorPlugin)
   {
      this.validatorPlugin = validatorPlugin;
      return (this);
   }



   /*******************************************************************************
    * Getter for collapsible
    * @see #withCollapsible(CollapsibleMetaData)
    *******************************************************************************/
   public CollapsibleMetaData getCollapsible()
   {
      return (this.collapsible);
   }



   /*******************************************************************************
    * Setter for collapsible
    * @see #withCollapsible(CollapsibleMetaData)
    *******************************************************************************/
   public void setCollapsible(CollapsibleMetaData collapsible)
   {
      this.collapsible = collapsible;
   }



   /*******************************************************************************
    * Fluent setter for collapsible
    *
    * @param collapsible
    * collapsible behavior for the widget.
    * @return this
    *******************************************************************************/
   public QWidgetMetaData withCollapsible(CollapsibleMetaData collapsible)
   {
      this.collapsible = collapsible;
      return (this);
   }


}
