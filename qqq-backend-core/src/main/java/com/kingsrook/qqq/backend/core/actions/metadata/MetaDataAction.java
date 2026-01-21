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

package com.kingsrook.qqq.backend.core.actions.metadata;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionCheckResult;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.AppTreeNode;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppChildMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;


/*******************************************************************************
 ** Action to fetch top-level meta-data in a qqq instance.
 **
 *******************************************************************************/
public class MetaDataAction
{
   private static final QLogger LOG = QLogger.getLogger(MetaDataAction.class);

   private static Memoization<QInstance, MetaDataActionCustomizerInterface> metaDataActionCustomizerMemoization = new Memoization<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public MetaDataOutput execute(MetaDataInput metaDataInput) throws QException
   {
      ActionHelper.validateSession(metaDataInput);

      MetaDataOutput           metaDataOutput = new MetaDataOutput();
      Map<String, AppTreeNode> treeNodes      = new LinkedHashMap<>();

      MetaDataActionCustomizerInterface customizer = getMetaDataActionCustomizer();

      /////////////////////////////////////
      // map tables to frontend metadata //
      /////////////////////////////////////
      Map<String, QFrontendTableMetaData> tables = new LinkedHashMap<>();
      for(Map.Entry<String, QTableMetaData> entry : QContext.getQInstance().getTables().entrySet())
      {
         String         tableName = entry.getKey();
         QTableMetaData table     = entry.getValue();

         if(isObjectDenied(metaDataInput, customizer, table))
         {
            continue;
         }

         QBackendMetaData backendForTable = QContext.getQInstance().getBackendForTable(tableName);
         tables.put(tableName, new QFrontendTableMetaData(metaDataInput, backendForTable, table, false, false));
         treeNodes.put(tableName, new AppTreeNode(table));
      }
      metaDataOutput.setTables(tables);

      // addJoinsToTables(tables);
      // addJoinedTablesToTables(tables);

      ////////////////////////////////////////
      // map processes to frontend metadata //
      ////////////////////////////////////////
      Map<String, QFrontendProcessMetaData> processes = new LinkedHashMap<>();
      for(Map.Entry<String, QProcessMetaData> entry : QContext.getQInstance().getProcesses().entrySet())
      {
         String           processName = entry.getKey();
         QProcessMetaData process     = entry.getValue();

         if(isObjectDenied(metaDataInput, customizer, process))
         {
            continue;
         }

         processes.put(processName, new QFrontendProcessMetaData(metaDataInput, process, false));
         treeNodes.put(processName, new AppTreeNode(process));
      }
      metaDataOutput.setProcesses(processes);

      //////////////////////////////////////
      // map reports to frontend metadata //
      //////////////////////////////////////
      Map<String, QFrontendReportMetaData> reports = new LinkedHashMap<>();
      for(Map.Entry<String, QReportMetaData> entry : QContext.getQInstance().getReports().entrySet())
      {
         String          reportName = entry.getKey();
         QReportMetaData report     = entry.getValue();

         if(isObjectDenied(metaDataInput, customizer, report))
         {
            continue;
         }

         reports.put(reportName, new QFrontendReportMetaData(metaDataInput, report, false));
         treeNodes.put(reportName, new AppTreeNode(report));
      }
      metaDataOutput.setReports(reports);

      //////////////////////////////////////
      // map widgets to frontend metadata //
      //////////////////////////////////////
      Map<String, QFrontendWidgetMetaData> widgets = new LinkedHashMap<>();
      for(Map.Entry<String, QWidgetMetaDataInterface> entry : QContext.getQInstance().getWidgets().entrySet())
      {
         String                   widgetName = entry.getKey();
         QWidgetMetaDataInterface widget     = entry.getValue();

         if(isObjectDenied(metaDataInput, customizer, widget))
         {
            continue;
         }

         widgets.put(widgetName, new QFrontendWidgetMetaData(metaDataInput, widget));
      }
      metaDataOutput.setWidgets(widgets);

      ////////////////////////////////////////////////////////////////////////
      // sort apps - by sortOrder (integer), then by label, finally by name //
      ////////////////////////////////////////////////////////////////////////
      List<QAppMetaData> sortedApps = QContext.getQInstance().getApps().values().stream()
         .sorted(Comparator.comparing((QAppMetaData a) -> Objects.requireNonNullElse(a.getSortOrder(), QAppMetaData.DEFAULT_SORT_ORDER))
            .thenComparing((QAppMetaData a) -> Objects.requireNonNullElse(a.getLabel(), ""))
            .thenComparing((QAppMetaData a) -> Objects.requireNonNullElse(a.getName(), "")))
         .toList();

      ///////////////////////////////////
      // map apps to frontend metadata //
      ///////////////////////////////////
      Map<String, QFrontendAppMetaData> apps = new LinkedHashMap<>();
      for(QAppMetaData app : sortedApps)
      {
         String appName = app.getName();

         if(isObjectDenied(metaDataInput, customizer, app))
         {
            continue;
         }

         //////////////////////////////////////
         // build the frontend-app meta-data //
         //////////////////////////////////////
         QFrontendAppMetaData frontendAppMetaData = new QFrontendAppMetaData(app, metaDataOutput);

         /////////////////////////////////////////
         // add children (if they're permitted) //
         /////////////////////////////////////////
         if(CollectionUtils.nullSafeHasContents(app.getChildren()))
         {
            for(QAppChildMetaData child : app.getChildren())
            {
               if(child instanceof MetaDataWithPermissionRules metaDataWithPermissionRules)
               {
                  if(isObjectDenied(metaDataInput, customizer, metaDataWithPermissionRules))
                  {
                     continue;
                  }
               }

               //////////////////////////////////////////////////////////////////////////////////////////////////////
               // if the child was filtered away, so it isn't in its corresponding map, then don't include it here //
               //////////////////////////////////////////////////////////////////////////////////////////////////////
               if(child instanceof QTableMetaData table && !tables.containsKey(table.getName()))
               {
                  continue;
               }
               if(child instanceof QProcessMetaData process && !processes.containsKey(process.getName()))
               {
                  continue;
               }
               if(child instanceof QReportMetaData report && !reports.containsKey(report.getName()))
               {
                  continue;
               }
               if(child instanceof QAppMetaData childApp && !apps.containsKey(childApp.getName()))
               {
                  // continue;
               }

               frontendAppMetaData.addChild(new AppTreeNode(child));
            }
         }

         //////////////////////////////////////////////////////////////////////////////////////////////////////
         // if the app ended up having no children, then discard it                                          //
         // todo - i think this was wrong, because it didn't take into account ... something nested maybe... //
         //////////////////////////////////////////////////////////////////////////////////////////////////////
         if(CollectionUtils.nullSafeIsEmpty(frontendAppMetaData.getChildren()) && CollectionUtils.nullSafeIsEmpty(frontendAppMetaData.getWidgets()))
         {
            // LOG.debug("Discarding empty app", logPair("name", frontendAppMetaData.getName()));
            // continue;
         }

         apps.put(appName, frontendAppMetaData);
         treeNodes.put(appName, new AppTreeNode(app));
      }
      metaDataOutput.setApps(apps);

      ////////////////////////////////////////////////
      // organize app tree nodes by their hierarchy //
      ////////////////////////////////////////////////
      List<AppTreeNode> appTree = new ArrayList<>();
      for(QAppMetaData appMetaData : sortedApps)
      {
         if(appMetaData.getParentAppName() == null)
         {
            buildAppTree(metaDataInput, treeNodes, appTree, appMetaData, treeNodes.get(appMetaData.getName()), customizer);
         }
      }
      metaDataOutput.setAppTree(appTree);

      ////////////////////////////////////
      // add branding metadata if found //
      ////////////////////////////////////
      if(QContext.getQInstance().getBranding() != null)
      {
         metaDataOutput.setBranding(QContext.getQInstance().getBranding());
      }

      metaDataOutput.setEnvironmentValues(Objects.requireNonNullElse(QContext.getQInstance().getEnvironmentValues(), Collections.emptyMap()));
      metaDataOutput.setHelpContents(Objects.requireNonNullElse(QContext.getQInstance().getHelpContent(), Collections.emptyMap()));
      metaDataOutput.setSupplementalInstanceMetaData(QContext.getQInstance().getSupplementalMetaData());

      Map<String, String> redirects = buildRedirectsForMultiAppTablesWithLimitedUserAccess(metaDataInput, sortedApps, apps.keySet());
      metaDataOutput.setRedirects(redirects);

      try
      {
         customizer.postProcess(metaDataOutput);
      }
      catch(QUserFacingException e)
      {
         LOG.debug("User-facing exception thrown in meta-data customizer post-processing", e);
      }
      catch(Exception e)
      {
         LOG.warn("Unexpected error thrown in meta-data customizer post-processing", e);
      }

      return metaDataOutput;
   }



   /***************************************************************************
    * look at tables that are in multiple apps, where the user has access to
    * some but not all - so deep-links to ones they don't have permission to
    * should redirect to the first app they have access to.
    *
    * <p>for example, consider a Roles table under /admin/ and under /setup/,
    * and a user who only has access to /setup/.  We want to build a redirect
    * from /admin/Roles to /setup/Roles.</p>
    *
    * @param metaDataInput input object for the request
    * @param sortedApps list of apps, sorted by sortOrder and name.  Note, this
    *                   is a full list of apps in the instance - not filtered by
    *                   permissions or via the customizer.
    * @param allowedApps set of names of apps that the user has access to.
    * @return map of table name to redirect path
    ***************************************************************************/
   private Map<String, String> buildRedirectsForMultiAppTablesWithLimitedUserAccess(MetaDataInput metaDataInput, List<QAppMetaData> sortedApps, Set<String> allowedApps)
   {
      Map<String, String> redirects = new LinkedHashMap<>();

      ////////////////////////////////////////////////////////////////////////////////////////
      // build listing-hash from table to app, to identify tables that are in multiple apps //
      ////////////////////////////////////////////////////////////////////////////////////////
      MetaDataActionCustomizerInterface customizer          = getMetaDataActionCustomizer();
      ListingHash<String, QAppMetaData> tableAppListingHash = new ListingHash<>();
      for(QAppMetaData app : sortedApps)
      {
         if(CollectionUtils.nullSafeHasContents(app.getChildren()))
         {
            for(QAppChildMetaData child : app.getChildren())
            {
               if(child instanceof QTableMetaData tableMetaData)
               {
                  if(!isObjectDenied(metaDataInput, customizer, tableMetaData))
                  {
                     tableAppListingHash.add(tableMetaData.getName(), app);
                  }
               }
            }
         }
      }

      /////////////////////////////////////////////////////////////////////////////////
      // now for table entries in the map with more than 1 app, check which apps the //
      // user has (full) access to and if there are some that they do and some that  //
      // they don't, then build redirects from the denied apps to the allowed apps.  //
      /////////////////////////////////////////////////////////////////////////////////
      for(Map.Entry<String, List<QAppMetaData>> entry : tableAppListingHash.entrySet())
      {
         String             tableName = entry.getKey();
         List<QAppMetaData> tableApps = entry.getValue();

         if(tableApps.size() > 1)
         {
            List<String> tableAppsThatUserHas         = new ArrayList<>();
            List<String> tableAppsThatUserDoesNotHave = new ArrayList<>();

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // for each app the table is in, check if the user has full access to that app (checking each parent app) //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            for(QAppMetaData tableApp : tableApps)
            {
               if(doesUserHaveAppAndParents(tableApp, allowedApps))
               {
                  tableAppsThatUserHas.add(tableApp.getName());
               }
               else
               {
                  tableAppsThatUserDoesNotHave.add(tableApp.getName());
               }
            }

            /////////////////////////////////////////////////////////////////////////////////
            // if the user has access to some apps that this table is in, but not all of   //
            // them, then we need redirects from any of the denied ones to the primary one //
            /////////////////////////////////////////////////////////////////////////////////
            if(tableAppsThatUserHas.size() < tableApps.size() && !tableAppsThatUserHas.isEmpty())
            {
               String firstAppThatUserHas = Objects.requireNonNullElseGet(getHighestAffinityAppName(tableAppsThatUserHas, tableName), () -> tableAppsThatUserHas.getFirst());

               for(String appNeedingRedirect : tableAppsThatUserDoesNotHave)
               {
                  String fromPath = buildAppPath(appNeedingRedirect) + "/" + tableName;
                  String toPath   = buildAppPath(firstAppThatUserHas) + "/" + tableName;
                  LOG.debug("Redirecting {} to {}", fromPath, toPath);
                  redirects.put(fromPath, toPath);
                  redirects.put(fromPath + "/*", toPath);
               }
            }
         }
      }
      return redirects;
   }



   /***************************************************************************
    * given a set of app names, and a table name, return the app name with the
    * highest affinity value
    ***************************************************************************/
   private String getHighestAffinityAppName(List<String> tableAppsThatUserHas, String tableName)
   {
      List<AppTreeNode> list = tableAppsThatUserHas.stream()
         .map(appName -> QContext.getQInstance().getApps().get(appName))
         .filter(Objects::nonNull)
         .map(app -> new AppTreeNode(app).withAppAffinity(app.getChildAppAffinity(tableName)))
         .sorted(Comparator.comparing((AppTreeNode atn) -> Objects.requireNonNullElse(atn.getAppAffinity(), Integer.MIN_VALUE)).reversed())
         .toList();

      if(list.isEmpty())
      {
         return (tableAppsThatUserHas.getFirst());
      }

      return (list.getFirst().getName());
   }



   /***************************************************************************
    * test if the user has full access to the app and all parent apps.
    *
    * @param app the app to check
    * @param allowedApps set of names of apps that the user has access to.
    * @return true if the user has full access to the app (thorough all parent apps),
    * false otherwise
    ***************************************************************************/
   private boolean doesUserHaveAppAndParents(QAppMetaData app, Set<String> allowedApps)
   {
      if(app == null)
      {
         return (false);
      }

      if(!allowedApps.contains(app.getName()))
      {
         return (false);
      }

      if(app.getParentAppName() == null)
      {
         return (true);
      }

      QAppMetaData parentApp = QContext.getQInstance().getApps().get(app.getParentAppName());
      return (doesUserHaveAppAndParents(parentApp, allowedApps));
   }



   /***************************************************************************
    * recursively build up path to an app, from the root, navigating through the
    * parentAppName attribute.
    *
    * <p>Does not consider permissions of any kind.</p>
    *
    * @param appName the name of the app to build the path for
    * @return the path to the app, starting with a /.  Will be empty string if
    * the app isn't found.
    ***************************************************************************/
   private String buildAppPath(String appName)
   {
      StringBuilder rs = new StringBuilder();

      QAppMetaData appMetaData = QContext.getQInstance().getApps().get(appName);
      while(appMetaData != null)
      {
         rs.insert(0, "/" + appMetaData.getName());
         if(appMetaData.getParentAppName() == null)
         {
            break;
         }

         appMetaData = QContext.getQInstance().getApps().get(appMetaData.getParentAppName());
      }

      return rs.toString();
   }



   /***************************************************************************
    * Check both the customizer and permissions to decide if an object should
    * be denied/hidden from the user
    ***************************************************************************/
   private static boolean isObjectDenied(MetaDataInput metaDataInput, MetaDataActionCustomizerInterface customizer, MetaDataWithPermissionRules object)
   {
      if(object instanceof QTableMetaData table && !customizer.allowTable(metaDataInput, table))
      {
         return true;
      }

      if(object instanceof QProcessMetaData process && !customizer.allowProcess(metaDataInput, process))
      {
         return true;
      }

      if(object instanceof QReportMetaData report && !customizer.allowReport(metaDataInput, report))
      {
         return true;
      }

      if(object instanceof QWidgetMetaDataInterface widget && !customizer.allowWidget(metaDataInput, widget))
      {
         return true;
      }

      if(object instanceof QAppMetaData app && !customizer.allowApp(metaDataInput, app))
      {
         return true;
      }

      PermissionCheckResult permissionResult = PermissionsHelper.getPermissionCheckResult(metaDataInput, object);
      if(permissionResult.equals(PermissionCheckResult.DENY_HIDE))
      {
         return true;
      }
      return false;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private MetaDataActionCustomizerInterface getMetaDataActionCustomizer()
   {
      return metaDataActionCustomizerMemoization.getResult(QContext.getQInstance(), i ->
      {
         MetaDataActionCustomizerInterface actionCustomizer                  = null;
         QCodeReference                    metaDataActionCustomizerReference = QContext.getQInstance().getMetaDataActionCustomizer();
         if(metaDataActionCustomizerReference != null)
         {
            actionCustomizer = QCodeLoader.getAdHoc(MetaDataActionCustomizerInterface.class, metaDataActionCustomizerReference);
         }

         if(actionCustomizer == null)
         {
            /////////////////////////////////////////////////////////////////////////////////////
            // check if QInstance is still using the now-deprecated getMetaDataFilter approach //
            /////////////////////////////////////////////////////////////////////////////////////
            @SuppressWarnings("deprecation")
            QCodeReference metaDataFilterReference = QContext.getQInstance().getMetaDataFilter();
            if(metaDataFilterReference != null)
            {
               LOG.warn("QInstance.metaDataFilter is deprecated in favor of metaDataActionCustomizer.");
               actionCustomizer = QCodeLoader.getAdHoc(MetaDataActionCustomizerInterface.class, metaDataFilterReference);
            }
         }

         if(actionCustomizer == null)
         {
            actionCustomizer = new DefaultNoopMetaDataActionCustomizer();
         }

         return (actionCustomizer);
      }).orElseThrow(() -> new QRuntimeException("Error getting MetaDataActionCustomizer"));
   }



   /*******************************************************************************
    * Recursively build the tree of app children.
    *
    * <p>That is, at the top level are apps
    * (which themselves implement the interface {@link QAppChildMetaData}), and then
    * under them can be more apps, or tables, processes, etc.</p>
    *
    * @param metaDataInput input object for the request
    * @param treeNodes map of app name to {@link AppTreeNode}.  So this is each table/app/process/etc
    *                  metaData object as a tree node object.
    * @param nodeList list of {@link AppTreeNode} objects.  This is the list of nodes
    *                 that will be returned to the client as the appTree.
    * @param appChildMetaData the current app child metadata object being processed.
    *                         For the initial (non-recursive) calls to this method,
    *                         these should be top-level apps (e.g., apps w/o parents).
    * @param childTreeNode an AppTreeNode to use for the appChildMetaData object.
    *                      Generally, this would come from the treeNodes map - but
    *                      for cases where it may need some different attributes
    *                      on it (namely, an appAffinity for a particular child (table)
    *                      within a particular app), then this would be a clone of
    *                      the AppTreeNode from the treeNodes map.
    * @param customizer an optional customizer - used in here for allow/deny decisions.
    *******************************************************************************/
   private void buildAppTree(MetaDataInput metaDataInput, Map<String, AppTreeNode> treeNodes, List<AppTreeNode> nodeList, QAppChildMetaData appChildMetaData, AppTreeNode childTreeNode, MetaDataActionCustomizerInterface customizer)
   {
      if(childTreeNode == null)
      {
         return;
      }

      nodeList.add(childTreeNode);
      if(appChildMetaData instanceof QAppMetaData appChildAsApp)
      {
         if(appChildAsApp.getChildren() != null)
         {
            for(QAppChildMetaData grandChild : appChildAsApp.getChildren())
            {
               if(grandChild instanceof MetaDataWithPermissionRules metaDataWithPermissionRules)
               {
                  if(isObjectDenied(metaDataInput, customizer, metaDataWithPermissionRules))
                  {
                     continue;
                  }
               }

               AppTreeNode grandChildTreeNode = treeNodes.get(grandChild.getName());
               Integer     childAppAffinity   = appChildAsApp.getChildAppAffinity(grandChild.getName());
               if(childAppAffinity != null)
               {
                  grandChildTreeNode = grandChildTreeNode.clone();
                  grandChildTreeNode.setAppAffinity(childAppAffinity);
               }

               buildAppTree(metaDataInput, treeNodes, childTreeNode.getChildren(), grandChild, grandChildTreeNode, customizer);
            }
         }
      }
   }
}
