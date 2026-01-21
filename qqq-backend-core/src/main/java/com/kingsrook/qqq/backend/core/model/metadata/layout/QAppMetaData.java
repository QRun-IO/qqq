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

package com.kingsrook.qqq.backend.core.model.metadata.layout;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** MetaData definition of an App - an entity that organizes tables & processes
 ** and can be arranged hierarchically (e.g, apps can contain other apps).
 *******************************************************************************/
public class QAppMetaData implements QAppChildMetaData, MetaDataWithPermissionRules, TopLevelMetaDataInterface
{
   public static final Integer DEFAULT_SORT_ORDER = 500;

   private String name;
   private String label;

   private Integer sortOrder = DEFAULT_SORT_ORDER;

   private QPermissionRules permissionRules;

   private List<QAppChildMetaData> children;

   private String parentAppName;
   private QIcon  icon;

   private List<String>      widgets;
   private List<QAppSection> sections;

   private Map<String, Integer> childAppAffinities;

   private Map<String, QSupplementalAppMetaData> supplementalMetaData;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QAppMetaData()
   {
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
   public QAppMetaData withName(String name)
   {
      this.name = name;
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
   public QAppMetaData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for children
    **
    *******************************************************************************/
   public List<QAppChildMetaData> getChildren()
   {
      return children;
   }



   /*******************************************************************************
    ** Setter for children
    **
    *******************************************************************************/
   public void setChildren(List<QAppChildMetaData> children)
   {
      if(CollectionUtils.nullSafeHasContents(children))
      {
         for(QAppChildMetaData child : children)
         {
            addChild(child);
         }
      }
   }



   /*******************************************************************************
    ** Add a child to this app.
    **
    *******************************************************************************/
   public void addChild(QAppChildMetaData child)
   {
      if(this.children == null)
      {
         this.children = new ArrayList<>();
      }
      this.children.add(child);

      if(child instanceof QAppMetaData childApp)
      {
         childApp.setParentAppName(this.getName());
      }
   }



   /*******************************************************************************
    ** Fluently add a child to this app.
    **
    *******************************************************************************/
   public QAppMetaData withChild(QAppChildMetaData child)
   {
      addChild(child);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for children
    **
    *******************************************************************************/
   public QAppMetaData withChildren(List<QAppChildMetaData> children)
   {
      setChildren(children);
      return (this);
   }



   /*******************************************************************************
    ** Getter for parentAppName
    **
    *******************************************************************************/
   public String getParentAppName()
   {
      return parentAppName;
   }



   /*******************************************************************************
    ** Setter for parentAppName
    **
    *******************************************************************************/
   public void setParentAppName(String parentAppName)
   {
      this.parentAppName = parentAppName;
   }



   /*******************************************************************************
    ** Getter for icon
    **
    *******************************************************************************/
   public QIcon getIcon()
   {
      return icon;
   }



   /*******************************************************************************
    ** Setter for icon
    **
    *******************************************************************************/
   public void setIcon(QIcon icon)
   {
      this.icon = icon;
   }



   /*******************************************************************************
    ** Fluent setter for icon
    **
    *******************************************************************************/
   public QAppMetaData withIcon(QIcon icon)
   {
      this.icon = icon;
      return (this);
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
    ** Setter for widgets
    **
    *******************************************************************************/
   public void setWidgets(List<String> widgets)
   {
      this.widgets = widgets;
   }



   /*******************************************************************************
    ** Fluent setter for widgets
    **
    *******************************************************************************/
   public QAppMetaData withWidgets(List<String> widgets)
   {
      this.widgets = widgets;
      return (this);
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
    ** Setter for sections
    **
    *******************************************************************************/
   public void setSections(List<QAppSection> sections)
   {
      this.sections = sections;
   }



   /*******************************************************************************
    ** Fluent setter for sections
    **
    *******************************************************************************/
   public QAppMetaData withSections(List<QAppSection> sections)
   {
      this.sections = sections;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addSection(QAppSection section)
   {
      if(this.sections == null)
      {
         this.sections = new ArrayList<>();
      }
      this.sections.add(section);
   }



   /*******************************************************************************
    ** Fluent setter for sections
    **
    *******************************************************************************/
   public QAppMetaData withSection(QAppSection section)
   {
      this.addSection(section);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QAppMetaData withSectionOfChildren(QAppSection section, Collection<? extends QAppChildMetaData> children)
   {
      this.addSection(section);

      for(QAppChildMetaData child : CollectionUtils.nonNullCollection(children))
      {
         withChild(child);
         if(child instanceof QTableMetaData)
         {
            section.withTable(child.getName());
         }
         else if(child instanceof QProcessMetaData)
         {
            section.withProcess(child.getName());
         }
         else if(child instanceof QReportMetaData)
         {
            section.withReport(child.getName());
         }
         else
         {
            throw new IllegalArgumentException("Unrecognized child type: " + child.getName());
         }
      }

      return (this);
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   public QAppMetaData withSectionOfChildren(QAppSection section, QAppChildMetaData... children)
   {
      return (withSectionOfChildren(section, children == null ? null : Arrays.stream(children).toList()));
   }



   /*******************************************************************************
    ** Getter for permissionRules
    *******************************************************************************/
   public QPermissionRules getPermissionRules()
   {
      return (this.permissionRules);
   }



   /*******************************************************************************
    ** Setter for permissionRules
    *******************************************************************************/
   public void setPermissionRules(QPermissionRules permissionRules)
   {
      this.permissionRules = permissionRules;
   }



   /*******************************************************************************
    ** Fluent setter for permissionRules
    *******************************************************************************/
   public QAppMetaData withPermissionRules(QPermissionRules permissionRules)
   {
      this.permissionRules = permissionRules;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.addApp(this);
   }



   /*******************************************************************************
    ** Getter for sortOrder
    *******************************************************************************/
   public Integer getSortOrder()
   {
      return (this.sortOrder);
   }



   /*******************************************************************************
    ** Setter for sortOrder
    *******************************************************************************/
   public void setSortOrder(Integer sortOrder)
   {
      this.sortOrder = sortOrder;
   }



   /*******************************************************************************
    ** Fluent setter for sortOrder
    *******************************************************************************/
   public QAppMetaData withSortOrder(Integer sortOrder)
   {
      this.sortOrder = sortOrder;
      return (this);
   }



   /*******************************************************************************
    ** Getter for supplementalMetaData
    *******************************************************************************/
   public Map<String, QSupplementalAppMetaData> getSupplementalMetaData()
   {
      return (this.supplementalMetaData);
   }



   /*******************************************************************************
    ** Setter for supplementalMetaData
    *******************************************************************************/
   public void setSupplementalMetaData(Map<String, QSupplementalAppMetaData> supplementalMetaData)
   {
      this.supplementalMetaData = supplementalMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for supplementalMetaData
    *******************************************************************************/
   public QAppMetaData withSupplementalMetaData(QSupplementalAppMetaData supplementalMetaData)
   {
      if(this.supplementalMetaData == null)
      {
         this.supplementalMetaData = new HashMap<>();
      }
      this.supplementalMetaData.put(supplementalMetaData.getType(), supplementalMetaData);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for supplementalMetaData
    *******************************************************************************/
   public QAppMetaData withSupplementalMetaData(Map<String, QSupplementalAppMetaData> supplementalMetaData)
   {
      this.supplementalMetaData = supplementalMetaData;
      return (this);
   }



   /***************************************************************************
    * set an appAffinity for a child.  Higher values are higher affinity, with
    * null (the default) considered the lowest. Tiebreaking behavior is not
    * specified and may differ by use-case (e.g., by front-end), though app
    * sort-order is recommended.
    *
    * <p>The purpose of this appAffinity is not about ordering the children of this
    * app - but rather - for cases where the same object (e.g. a table) is
    * referenced by multiple apps, and we want to control which app should be
    * displayed first when the object is referenced.</p>
    *
    * @param childName name of child (e.g., a table name) to set appAffinity for
    * @param appAffinity appAffinity level, or null to clear any existing appAffinity.
    *                 Higher values are higher affinity.  null is lowest.
    ***************************************************************************/
   public void setChildAppAffinity(String childName, Integer appAffinity)
   {
      if(this.childAppAffinities == null)
      {
         this.childAppAffinities = new HashMap<>();
      }

      this.childAppAffinities.put(childName, appAffinity);
   }



   /***************************************************************************
    * get the appAffinity assigned to this child (by name) for this app - null if not set.
    *
    * @param childName name of child (e.g., a table name) to get appAffinity for
    * @return appAffinity level, or null if not set.
    ***************************************************************************/
   public Integer getChildAppAffinity(String childName)
   {
      return (this.childAppAffinities == null ? null : this.childAppAffinities.get(childName));
   }

}
