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

package com.kingsrook.qqq.backend.core.model.metadata.frontend;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QSupplementalAppMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 * Version of QAppMetaData that's meant for transmitting to a frontend.
 *
 *******************************************************************************/
@JsonInclude(Include.NON_NULL)
public class QFrontendAppMetaData
{
   private String name;
   private String label;
   private QIcon  icon;

   private List<String>                    widgets  = new ArrayList<>();
   private List<AppTreeNode>               children = new ArrayList<>();
   private Map<String, AppTreeNode>        childMap = new HashMap<>();
   private Map<String, List<QHelpContent>> helpContents;

   private List<QAppSection> sections;

   private Map<String, QSupplementalAppMetaData> supplementalAppMetaData;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFrontendAppMetaData(QAppMetaData appMetaData, MetaDataOutput metaDataOutput)
   {
      this.name = appMetaData.getName();
      this.label = appMetaData.getLabel();
      this.icon = appMetaData.getIcon();

      List<String> filteredWidgets = CollectionUtils.nonNullList(appMetaData.getWidgets()).stream().filter(n -> metaDataOutput.getWidgets().containsKey(n)).toList();
      if(CollectionUtils.nullSafeHasContents(filteredWidgets))
      {
         this.widgets = filteredWidgets;
      }

      List<QAppSection> filteredSections = new ArrayList<>();
      for(QAppSection section : CollectionUtils.nonNullList(appMetaData.getSections()))
      {
         List<String> filteredTables    = CollectionUtils.nonNullList(section.getTables()).stream().filter(n -> metaDataOutput.getTables().containsKey(n)).toList();
         List<String> filteredProcesses = CollectionUtils.nonNullList(section.getProcesses()).stream().filter(n -> metaDataOutput.getProcesses().containsKey(n)).toList();
         List<String> filteredReports   = CollectionUtils.nonNullList(section.getReports()).stream().filter(n -> metaDataOutput.getReports().containsKey(n)).toList();
         List<String> filteredApps      = CollectionUtils.nonNullList(section.getApps());

         //////////////////////////////////////////////////////
         // only include the section if it has some contents //
         //////////////////////////////////////////////////////
         if(!filteredTables.isEmpty() || !filteredApps.isEmpty() || !filteredProcesses.isEmpty() || !filteredReports.isEmpty())
         {
            QAppSection clonedSection = section.clone();
            clonedSection.setTables(filteredTables);
            clonedSection.setProcesses(filteredProcesses);
            clonedSection.setReports(filteredReports);
            clonedSection.setApps(filteredApps);
            filteredSections.add(clonedSection);
         }
      }

      if(CollectionUtils.nullSafeHasContents(filteredSections))
      {
         this.sections = filteredSections;
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // include supplemental meta data, based on if it's meant for full or partial frontend meta-data requests //
      // todo - take includeFullMetaData as a param?                                                            //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      boolean includeFullMetaData = true;
      for(QSupplementalAppMetaData supplementalAppMetaData : CollectionUtils.nonNullMap(appMetaData.getSupplementalMetaData()).values())
      {
         boolean include;
         if(includeFullMetaData)
         {
            include = supplementalAppMetaData.includeInFullFrontendMetaData();
         }
         else
         {
            include = supplementalAppMetaData.includeInPartialFrontendMetaData();
         }

         if(include)
         {
            this.supplementalAppMetaData = Objects.requireNonNullElseGet(this.supplementalAppMetaData, HashMap::new);
            this.supplementalAppMetaData.put(supplementalAppMetaData.getType(), supplementalAppMetaData);
         }
      }

      this.helpContents = appMetaData.getHelpContent();
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
    ** Getter for children
    **
    *******************************************************************************/
   public List<AppTreeNode> getChildren()
   {
      return children;
   }



   /*******************************************************************************
    ** Getter for childMap
    **
    *******************************************************************************/
   public Map<String, AppTreeNode> getChildMap()
   {
      return childMap;
   }



   /*******************************************************************************
    ** Getter for iconName
    **
    *******************************************************************************/
   public String getIconName()
   {
      return (icon == null ? null : icon.getName());
   }



   /*******************************************************************************
    ** Getter for helpContents
    **
    *******************************************************************************/
   public Map<String, List<QHelpContent>> getHelpContents()
   {
      return helpContents;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addChild(AppTreeNode childAppTreeNode)
   {
      if(children == null)
      {
         children = new ArrayList<>();
      }
      childMap.put(childAppTreeNode.getName(), childAppTreeNode);
      children.add(childAppTreeNode);
   }



   /*******************************************************************************
    ** Getter for widgets
    **
    *******************************************************************************/
   public List<String> getWidgets()
   {
      return widgets;
   }



   /*******************************************************************************
    ** Getter for sections
    **
    *******************************************************************************/
   public List<QAppSection> getSections()
   {
      return sections;
   }



   /*******************************************************************************
    ** Getter for supplementalAppMetaData
    **
    *******************************************************************************/
   public Map<String, QSupplementalAppMetaData> getSupplementalAppMetaData()
   {
      return supplementalAppMetaData;
   }



   /*******************************************************************************
    ** Getter for icon
    **
    *******************************************************************************/
   public QIcon getIcon()
   {
      return icon;
   }
}
