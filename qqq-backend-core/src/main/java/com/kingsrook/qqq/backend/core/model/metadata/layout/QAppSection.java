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
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;


/*******************************************************************************
 ** A section of apps/tables/processes - a logical grouping.
 *******************************************************************************/
public class QAppSection implements Cloneable, QMetaDataObject
{
   private String name;
   private String label;
   private QIcon  icon;

   private List<String> tables;
   private List<String> apps;
   private List<String> processes;
   private List<String> reports;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QAppSection()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QAppSection(String name, String label, QIcon icon, List<String> tables, List<String> processes, List<String> reports)
   {
      this.name = name;
      this.label = label;
      this.icon = icon;
      this.tables = tables;
      this.processes = processes;
      this.reports = reports;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QAppSection clone()
   {
      try
      {
         QAppSection clone = (QAppSection) super.clone();
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
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
   public QAppSection withName(String name)
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
   public QAppSection withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tables
    **
    *******************************************************************************/
   public List<String> getTables()
   {
      return tables;
   }



   /*******************************************************************************
    ** Setter for tables
    **
    *******************************************************************************/
   public void setTables(List<String> tables)
   {
      this.tables = tables;
   }



   /*******************************************************************************
    ** Fluent setter for tables
    **
    *******************************************************************************/
   public QAppSection withTables(List<String> tables)
   {
      this.tables = tables;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for tables
    **
    *******************************************************************************/
   public QAppSection withTable(String tableName)
   {
      if(this.tables == null)
      {
         this.tables = new ArrayList<>();
      }
      this.tables.add(tableName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for processes
    **
    *******************************************************************************/
   public List<String> getProcesses()
   {
      return processes;
   }



   /*******************************************************************************
    ** Setter for processes
    **
    *******************************************************************************/
   public void setProcesses(List<String> processes)
   {
      this.processes = processes;
   }



   /*******************************************************************************
    ** Fluent setter for processes
    **
    *******************************************************************************/
   public QAppSection withProcesses(List<String> processes)
   {
      this.processes = processes;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for processes
    **
    *******************************************************************************/
   public QAppSection withProcess(String processName)
   {
      if(this.processes == null)
      {
         this.processes = new ArrayList<>();
      }
      this.processes.add(processName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for reports
    **
    *******************************************************************************/
   public List<String> getReports()
   {
      return reports;
   }



   /*******************************************************************************
    ** Setter for reports
    **
    *******************************************************************************/
   public void setReports(List<String> reports)
   {
      this.reports = reports;
   }



   /*******************************************************************************
    ** Fluent setter for reports
    **
    *******************************************************************************/
   public QAppSection withReports(List<String> reports)
   {
      this.reports = reports;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for reports
    **
    *******************************************************************************/
   public QAppSection withReport(String reportName)
   {
      if(this.reports == null)
      {
         this.reports = new ArrayList<>();
      }
      this.reports.add(reportName);
      return (this);
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
   public QAppSection withIcon(QIcon icon)
   {
      this.icon = icon;
      return (this);
   }



   /*******************************************************************************
    ** Getter for apps
    *******************************************************************************/
   public List<String> getApps()
   {
      return (this.apps);
   }



   /*******************************************************************************
    ** Setter for apps
    *******************************************************************************/
   public void setApps(List<String> apps)
   {
      this.apps = apps;
   }



   /*******************************************************************************
    ** Fluent setter for apps
    *******************************************************************************/
   public QAppSection withApps(List<String> apps)
   {
      this.apps = apps;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for apps
    **
    *******************************************************************************/
   public QAppSection withApp(String appName)
   {
      if(this.apps == null)
      {
         this.apps = new ArrayList<>();
      }
      this.apps.add(appName);
      return (this);
   }

}
