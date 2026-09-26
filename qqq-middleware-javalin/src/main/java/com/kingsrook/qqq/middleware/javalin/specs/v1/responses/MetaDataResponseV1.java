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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendWidgetMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.MetaDataOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapValueType;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.AppMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.AppTreeNode;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.Branding;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.ProcessMetaDataLight;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.ReportMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.SupplementalInstanceMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableMetaDataLight;


/*******************************************************************************
 **
 *******************************************************************************/
public class MetaDataResponseV1 implements MetaDataOutputInterface, ToSchema
{
   @OpenAPIDescription("Map of all apps within the QQQ Instance (that the user has permission to see that they exist).")
   @OpenAPIMapValueType(value = AppMetaData.class, useRef = true)
   private Map<String, AppMetaData> apps;

   @OpenAPIDescription("Tree of apps within the QQQ Instance, sorted and organized hierarchically, for presentation to a user.")
   @OpenAPIListItems(value = AppTreeNode.class, useRef = true)
   private List<AppTreeNode> appTree;

   @OpenAPIDescription("Map of all tables within the QQQ Instance (that the user has permission to see that they exist).")
   @OpenAPIMapValueType(value = TableMetaDataLight.class, useRef = true)
   private Map<String, TableMetaDataLight> tables;

   @OpenAPIDescription("Map of all processes within the QQQ Instance (that the user has permission to see that they exist).")
   @OpenAPIMapValueType(value = ProcessMetaDataLight.class, useRef = true)
   private Map<String, ProcessMetaDataLight> processes;

   @OpenAPIDescription("Map of all widgets within the QQQ Instance (that the user has permission to see that they exist).")
   @OpenAPIMapValueType(value = WidgetMetaData.class)
   private Map<String, WidgetMetaData> widgets;

   @OpenAPIDescription("Map of all reports within the QQQ Instance (that the user has permission to see that they exist).")
   @OpenAPIMapValueType(value = ReportMetaData.class)
   private Map<String, ReportMetaData> reports;

   @OpenAPIDescription("Application identity (names, logo, icon, accent colors and banners), when the instance defines it.")
   private Branding branding;

   @OpenAPIDescription("Settings from supplemental modules that a frontend may use (an explicit allow-list - not the supplemental meta-data objects themselves).  Omitted when the instance defines none of them.")
   private SupplementalInstanceMetaData supplementalInstanceMetaData;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setMetaDataOutput(MetaDataOutput metaDataOutput)
   {
      apps = new HashMap<>();
      for(QFrontendAppMetaData app : CollectionUtils.nonNullMap(metaDataOutput.getApps()).values())
      {
         apps.put(app.getName(), new AppMetaData(app));
      }

      appTree = new ArrayList<>();
      for(com.kingsrook.qqq.backend.core.model.metadata.frontend.AppTreeNode app : CollectionUtils.nonNullList(metaDataOutput.getAppTree()))
      {
         appTree.add(new AppTreeNode(app));
      }

      tables = new HashMap<>();
      for(QFrontendTableMetaData table : CollectionUtils.nonNullMap(metaDataOutput.getTables()).values())
      {
         tables.put(table.getName(), new TableMetaDataLight(table));
      }

      processes = new HashMap<>();
      for(QFrontendProcessMetaData process : CollectionUtils.nonNullMap(metaDataOutput.getProcesses()).values())
      {
         processes.put(process.getName(), new ProcessMetaDataLight(process));
      }

      widgets = new HashMap<>();
      for(QFrontendWidgetMetaData widget : CollectionUtils.nonNullMap(metaDataOutput.getWidgets()).values())
      {
         widgets.put(widget.getName(), new WidgetMetaData(widget));
      }

      reports = new HashMap<>();
      for(QFrontendReportMetaData report : CollectionUtils.nonNullMap(metaDataOutput.getReports()).values())
      {
         reports.put(report.getName(), new ReportMetaData(report));
      }

      branding = metaDataOutput.getBranding() == null ? null : new Branding(metaDataOutput.getBranding());

      supplementalInstanceMetaData = SupplementalInstanceMetaData.of(metaDataOutput);
   }



   /*******************************************************************************
    ** Fluent setter for MetaDataOutput
    **
    *******************************************************************************/
   public MetaDataResponseV1 withMetaDataOutput(MetaDataOutput metaDataOutput)
   {
      setMetaDataOutput(metaDataOutput);
      return (this);
   }



   /*******************************************************************************
    ** Getter for apps
    **
    *******************************************************************************/
   public Map<String, AppMetaData> getApps()
   {
      return apps;
   }



   /*******************************************************************************
    ** Getter for appTree
    **
    *******************************************************************************/
   public List<AppTreeNode> getAppTree()
   {
      return appTree;
   }



   /*******************************************************************************
    ** Getter for tables
    **
    *******************************************************************************/
   public Map<String, TableMetaDataLight> getTables()
   {
      return tables;
   }



   /*******************************************************************************
    ** Getter for processes
    **
    *******************************************************************************/
   public Map<String, ProcessMetaDataLight> getProcesses()
   {
      return processes;
   }



   /*******************************************************************************
    ** Getter for widgets
    **
    *******************************************************************************/
   public Map<String, WidgetMetaData> getWidgets()
   {
      return widgets;
   }



   /*******************************************************************************
    ** Getter for reports
    **
    *******************************************************************************/
   public Map<String, ReportMetaData> getReports()
   {
      return reports;
   }



   /*******************************************************************************
    ** Getter for branding
    **
    *******************************************************************************/
   public Branding getBranding()
   {
      return branding;
   }



   /*******************************************************************************
    ** Getter for supplementalInstanceMetaData
    **
    *******************************************************************************/
   public SupplementalInstanceMetaData getSupplementalInstanceMetaData()
   {
      return supplementalInstanceMetaData;
   }
}
