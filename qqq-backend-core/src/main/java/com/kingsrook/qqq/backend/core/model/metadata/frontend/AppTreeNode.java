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
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppChildMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Frontend-version of objects that are parts of the app-hierarchy/tree.
 ** e.g., Tables, Processes, and Apps themselves (since they can be nested).
 **
 ** These objects are organized into a tree - where each Node can have 0 or more
 ** other Nodes as children.
 *******************************************************************************/
public class AppTreeNode implements Cloneable
{
   private AppTreeNodeType   type;
   private String            name;
   private String            label;
   private List<AppTreeNode> children;

   private Integer appAffinity;

   private QIcon icon;



   /*******************************************************************************
    **
    *******************************************************************************/
   public AppTreeNode(QAppChildMetaData appChildMetaData)
   {
      this.name = appChildMetaData.getName();
      this.label = appChildMetaData.getLabel();

      if(appChildMetaData.getClass().equals(QTableMetaData.class))
      {
         this.type = AppTreeNodeType.TABLE;
      }
      else if(appChildMetaData.getClass().equals(QProcessMetaData.class))
      {
         this.type = AppTreeNodeType.PROCESS;
      }
      else if(appChildMetaData.getClass().equals(QReportMetaData.class))
      {
         this.type = AppTreeNodeType.REPORT;
      }
      else if(appChildMetaData.getClass().equals(QAppMetaData.class))
      {
         this.type = AppTreeNodeType.APP;
         children = new ArrayList<>();
      }
      else
      {
         throw (new IllegalStateException("Unrecognized class for app child meta data: " + appChildMetaData.getClass()));
      }

      if(appChildMetaData.getIcon() != null)
      {
         // todo - propagate icons from parents, if they aren't set here...
         this.icon = appChildMetaData.getIcon();
      }
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public AppTreeNodeType getType()
   {
      return type;
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
    ** Getter for iconName
    **
    *******************************************************************************/
   public String getIconName()
   {
      return (icon == null ? null : icon.getName());
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
    **
    *******************************************************************************/
   public void addChild(AppTreeNode childTreeNode)
   {
      if(children == null)
      {
         children = new ArrayList<>();
      }
      children.add(childTreeNode);
   }



   /*******************************************************************************
    * Getter for appAffinity
    * @see #withAppAffinity(Integer)
    *******************************************************************************/
   public Integer getAppAffinity()
   {
      return (this.appAffinity);
   }



   /*******************************************************************************
    * Setter for appAffinity
    * @see #withAppAffinity(Integer)
    *******************************************************************************/
   public void setAppAffinity(Integer appAffinity)
   {
      this.appAffinity = appAffinity;
   }



   /*******************************************************************************
    * Fluent setter for appAffinity
    *
    * @param appAffinity
    * appAffinity level for this child node, as it is related to its parent app.
    * See {@link QAppMetaData#setChildAppAffinity(String, Integer)}.
    * @return this
    *******************************************************************************/
   public AppTreeNode withAppAffinity(Integer appAffinity)
   {
      this.appAffinity = appAffinity;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public AppTreeNode clone()
   {
      try
      {
         AppTreeNode clone = (AppTreeNode) super.clone();
         if(children != null)
         {
            clone.children = new ArrayList<>();
            children.forEach(child -> clone.children.add(child.clone()));
         }
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }

}
