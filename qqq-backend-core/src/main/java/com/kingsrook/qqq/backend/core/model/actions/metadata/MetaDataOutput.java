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

package com.kingsrook.qqq.backend.core.model.actions.metadata;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.branding.QBrandingMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.AppTreeNode;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;


/*******************************************************************************
 * Output for a metaData action
 *
 *******************************************************************************/
public class MetaDataOutput extends AbstractActionOutput
{
   private Map<String, QFrontendTableMetaData>        tables;
   private Map<String, QFrontendProcessMetaData>      processes;
   private Map<String, QFrontendReportMetaData>       reports;
   private Map<String, QFrontendAppMetaData>          apps;
   private Map<String, QFrontendWidgetMetaData>       widgets;
   private Map<String, String>                        environmentValues;
   private Map<String, QSupplementalInstanceMetaData> supplementalInstanceMetaData;

   private List<AppTreeNode>               appTree;
   private QBrandingMetaData               branding;
   private Map<String, List<QHelpContent>> helpContents;

   private Map<String, String> redirects;


   /*******************************************************************************
    ** Getter for tables
    **
    *******************************************************************************/
   public Map<String, QFrontendTableMetaData> getTables()
   {
      return tables;
   }



   /*******************************************************************************
    ** Setter for tables
    **
    *******************************************************************************/
   public void setTables(Map<String, QFrontendTableMetaData> tables)
   {
      this.tables = tables;
   }



   /*******************************************************************************
    ** Getter for processes
    **
    *******************************************************************************/
   public Map<String, QFrontendProcessMetaData> getProcesses()
   {
      return processes;
   }



   /*******************************************************************************
    ** Setter for processes
    **
    *******************************************************************************/
   public void setProcesses(Map<String, QFrontendProcessMetaData> processes)
   {
      this.processes = processes;
   }



   /*******************************************************************************
    ** Getter for reports
    **
    *******************************************************************************/
   public Map<String, QFrontendReportMetaData> getReports()
   {
      return reports;
   }



   /*******************************************************************************
    ** Setter for reports
    **
    *******************************************************************************/
   public void setReports(Map<String, QFrontendReportMetaData> reports)
   {
      this.reports = reports;
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
    ** Setter for appTree
    **
    *******************************************************************************/
   public void setAppTree(List<AppTreeNode> appTree)
   {
      this.appTree = appTree;
   }



   /*******************************************************************************
    ** Getter for apps
    **
    *******************************************************************************/
   public Map<String, QFrontendAppMetaData> getApps()
   {
      return apps;
   }



   /*******************************************************************************
    ** Setter for apps
    **
    *******************************************************************************/
   public void setApps(Map<String, QFrontendAppMetaData> apps)
   {
      this.apps = apps;
   }



   /*******************************************************************************
    ** Getter for widgets
    **
    *******************************************************************************/
   public Map<String, QFrontendWidgetMetaData> getWidgets()
   {
      return widgets;
   }



   /*******************************************************************************
    ** Setter for widgets
    **
    *******************************************************************************/
   public void setWidgets(Map<String, QFrontendWidgetMetaData> widgets)
   {
      this.widgets = widgets;
   }



   /*******************************************************************************
    ** Getter for branding
    **
    *******************************************************************************/
   public QBrandingMetaData getBranding()
   {
      return branding;
   }



   /*******************************************************************************
    ** Setter for branding
    **
    *******************************************************************************/
   public void setBranding(QBrandingMetaData branding)
   {
      this.branding = branding;
   }



   /*******************************************************************************
    ** Getter for environmentValues
    **
    *******************************************************************************/
   public Map<String, String> getEnvironmentValues()
   {
      return environmentValues;
   }



   /*******************************************************************************
    ** Setter for environmentValues
    **
    *******************************************************************************/
   public void setEnvironmentValues(Map<String, String> environmentValues)
   {
      this.environmentValues = environmentValues;
   }



   /*******************************************************************************
    ** Getter for supplementalInstanceMetaData
    **
    *******************************************************************************/
   public Map<String, QSupplementalInstanceMetaData> getSupplementalInstanceMetaData()
   {
      return supplementalInstanceMetaData;
   }



   /*******************************************************************************
    ** Setter for supplementalInstanceMetaData
    **
    *******************************************************************************/
   public void setSupplementalInstanceMetaData(Map<String, QSupplementalInstanceMetaData> supplementalInstanceMetaData)
   {
      this.supplementalInstanceMetaData = supplementalInstanceMetaData;
   }



   /*******************************************************************************
    ** Setter for helpContents
    **
    *******************************************************************************/
   public void setHelpContents(Map<String, List<QHelpContent>> helpContents)
   {
      this.helpContents = helpContents;
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
    * Getter for redirects
    * @see #withRedirects(Map)
    *******************************************************************************/
   public Map<String, String> getRedirects()
   {
      return (this.redirects);
   }



   /*******************************************************************************
    * Setter for redirects
    * @see #withRedirects(Map)
    *******************************************************************************/
   public void setRedirects(Map<String, String> redirects)
   {
      this.redirects = redirects;
   }



   /*******************************************************************************
    * Fluent setter for redirects
    *
    * @param redirects
    * Map of string to string, where the key is the URL path to redirect from, and
    * the value is the URL path to redirect to.
    *
    * <p>The core {@link com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction}
    * will build some of these redirects automatically, e.g., for the use case of a
    * table in multiple apps, but where a user doesn't have permission to all of those apps.</p>
    *
    * <p>An app may build its own redirects via a
    * {@link com.kingsrook.qqq.backend.core.actions.metadata.MetaDataActionCustomizerInterface},
    * for whatever custom logic is needed.</p>
    *
    * @return this
    *******************************************************************************/
   public MetaDataOutput withRedirects(Map<String, String> redirects)
   {
      this.redirects = redirects;
      return (this);
   }



   /***************************************************************************
    * Fluent setter to add a single redirect.
    * @see #withRedirects(Map)
    ***************************************************************************/
   public MetaDataOutput withRedirect(String from, String to)
   {
      if(this.redirects == null)
      {
         this.redirects = new LinkedHashMap<>();
      }
      this.redirects.put(from, to);
      return (this);
   }

}
