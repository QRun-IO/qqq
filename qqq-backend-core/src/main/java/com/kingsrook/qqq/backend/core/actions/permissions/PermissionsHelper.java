/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.permissions;


import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.ImplicitQueryJoinForSecurityLock;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.ClientChannelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithName;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeaturePermission;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeaturePermissionsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QCredentialScope;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class PermissionsHelper
{
   private static final QLogger LOG = QLogger.getLogger(PermissionsHelper.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void checkTablePermissionThrowing(AbstractTableActionInput tableActionInput, TablePermissionSubType permissionSubType) throws QPermissionDeniedException
   {
      checkTablePermissionThrowing(tableActionInput, tableActionInput.getTableName(), permissionSubType);
   }



   /*******************************************************************************
    ** Call after the base table's READ check. Resolve a copy because JoinsContext
    ** expands joins and security filters. Framework-only security joins enforce
    ** the base table's access policy and do not grant the client joined reads.
    *******************************************************************************/
   public static void checkJoinedTableReadPermissions(AbstractTableActionInput input, List<QueryJoin> queryJoins, QQueryFilter filter) throws QException
   {
      QQueryFilter resolvedFilter = filter == null ? new QQueryFilter() : filter.clone();
      JoinsContext joinsContext = new JoinsContext(QContext.getQInstance(), input.getTableName(), queryJoins, resolvedFilter);
      Set<String> tableNames = new LinkedHashSet<>();
      for(QueryJoin queryJoin : joinsContext.getQueryJoins())
      {
         if(queryJoin instanceof ImplicitQueryJoinForSecurityLock)
         {
            continue;
         }

         tableNames.add(queryJoin.getJoinTable());
         if(queryJoin.getBaseTableOrAlias() != null)
         {
            tableNames.add(joinsContext.resolveTableNameOrAliasToTableName(queryJoin.getBaseTableOrAlias()));
         }
      }

      tableNames.remove(input.getTableName());
      for(String tableName : tableNames)
      {
         if(!PermissionsHelper.hasTablePermission(input, tableName, TablePermissionSubType.READ))
         {
            throw new QPermissionDeniedException("Permission denied.");
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void checkTablePermissionThrowing(AbstractActionInput actionInput, String tableName, TablePermissionSubType permissionSubType) throws QPermissionDeniedException
   {
      warnAboutPermissionSubTypeForTables(permissionSubType);
      QTableMetaData table = QContext.getQInstance().getTable(tableName);

      if(table == null)
      {
         LOG.info("Throwing a permission denied exception in response to a non-existent table name", logPair("tableName", tableName));
         throw (new QPermissionDeniedException("Permission denied."));
      }

      commonCheckPermissionThrowing(actionInput, table, permissionSubType, table.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getTablePermissionName(String tableName, TablePermissionSubType permissionSubType)
   {
      QInstance        qInstance          = QContext.getQInstance();
      QPermissionRules rules              = getEffectivePermissionRules(qInstance.getTable(tableName), qInstance);
      String           permissionBaseName = getEffectivePermissionBaseName(rules, tableName);
      return (getPermissionName(permissionBaseName, permissionSubType));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean hasTablePermission(AbstractActionInput actionInput, String tableName, TablePermissionSubType permissionSubType)
   {
      try
      {
         checkTablePermissionThrowing(actionInput, tableName, permissionSubType);
         return (true);
      }
      catch(QPermissionDeniedException e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    ** Feature permission check for the session in QContext (see the overload).
    *******************************************************************************/
   public static boolean hasTableFeaturePermission(QTableMetaData table, String suffix)
   {
      return (hasTableFeaturePermission(QContext.getQInstance(), QContext.getQSession(), table, suffix));
   }



   /*******************************************************************************
    ** Whether the given session may use a table feature permission (registered in
    ** TableFeaturePermissionsMetaData) on the given table.  Feature grants require
    ** table READ.  Standard implications retain the table's permission-level
    ** mapping and custom permission checker behavior.  Runs in a temporary
    ** context when the instance or session differ from QContext's.
    *******************************************************************************/
   public static boolean hasTableFeaturePermission(QInstance instance, QSession session, QTableMetaData table, String suffix)
   {
      if(instance == QContext.getQInstance() && session == QContext.getQSession())
      {
         return (hasTableFeaturePermissionInContext(table, suffix));
      }

      boolean[] result = new boolean[1];
      QContext.withTemporaryContext(new CapturedContext(instance, session), () -> result[0] = hasTableFeaturePermissionInContext(table, suffix));
      return (result[0]);
   }



   /*******************************************************************************
    ** Implementation of hasTableFeaturePermission against QContext.
    *******************************************************************************/
   private static boolean hasTableFeaturePermissionInContext(QTableMetaData table, String suffix)
   {
      QInstance instance = QContext.getQInstance();
      TableFeaturePermissionsMetaData metadata = TableFeaturePermissionsMetaData.of(instance);
      if(metadata == null || table.getIsHidden())
      {
         return (false);
      }
      TableFeaturePermission feature = metadata.getPermissions().stream()
         .filter(permission -> Objects.equals(permission.getSuffix(), suffix)).findFirst().orElse(null);
      if(feature == null || (feature.getAppliesTo() != null && !feature.getAppliesTo().test(instance, table)))
      {
         return (false);
      }
      GetInput input = new GetInput(table.getName());
      if(!hasTablePermission(input, table.getName(), TablePermissionSubType.READ))
      {
         return (false);
      }
      QPermissionRules rules = getEffectivePermissionRules(table, instance);
      if(rules.getLevel() == PermissionLevel.NOT_PROTECTED
         || QContext.getQSession().hasPermission(getEffectivePermissionBaseName(rules, table.getName()) + "." + suffix))
      {
         return (true);
      }
      TablePermissionSubType implication = feature.getImpliedBy() == null ? null : feature.getImpliedBy().impliedBy(instance, table);
      return (implication != null && hasTablePermission(input, table.getName(), implication));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static PermissionCheckResult getPermissionCheckResult(AbstractActionInput actionInput, MetaDataWithPermissionRules metaDataWithPermissionRules)
   {
      QPermissionRules rules              = getEffectivePermissionRules(metaDataWithPermissionRules, QContext.getQInstance());
      String           permissionBaseName = getEffectivePermissionBaseName(rules, metaDataWithPermissionRules.getName());

      ////////////////////////////////////////////////////////////////////////////
      // a client channel denied this object (e.g. the assistant's wall) - as if //
      // the user had no permission; hidden.  Checked before anything that could //
      // allow (NOT_PROTECTED, custom checkers).                                 //
      ////////////////////////////////////////////////////////////////////////////
      if(isDeniedForClientChannel(metaDataWithPermissionRules, rules) || isOutsideCredentialScope(metaDataWithPermissionRules, null))
      {
         return (PermissionCheckResult.DENY_HIDE);
      }

      switch(rules.getLevel())
      {
         case NOT_PROTECTED:
         {
            /////////////////////////////////////////////////
            // if the entity isn't protected, always ALLOW //
            /////////////////////////////////////////////////
            return PermissionCheckResult.ALLOW;
         }
         case HAS_ACCESS_PERMISSION:
         {
            ////////////////////////////////////////////////////////////////////////
            // if the entity just has a 'has access', then check for 'has access' //
            ////////////////////////////////////////////////////////////////////////
            return getPermissionCheckResult(actionInput, rules, permissionBaseName, metaDataWithPermissionRules, PrivatePermissionSubType.HAS_ACCESS);
         }
         case READ_WRITE_PERMISSIONS:
         {
            ////////////////////////////////////////////////////////////////
            // if the table is configured w/ read/write, check for either //
            ////////////////////////////////////////////////////////////////
            if(metaDataWithPermissionRules instanceof QTableMetaData)
            {
               return getPermissionCheckResult(actionInput, rules, permissionBaseName, metaDataWithPermissionRules, PrivatePermissionSubType.READ, PrivatePermissionSubType.WRITE);
            }
            return getPermissionCheckResult(actionInput, rules, permissionBaseName, metaDataWithPermissionRules, PrivatePermissionSubType.HAS_ACCESS);
         }
         case READ_INSERT_EDIT_DELETE_PERMISSIONS:
         {
            //////////////////////////////////////////////////////////////////////////
            // if the table is configured w/ read/insert/edit/delete, check for any //
            //////////////////////////////////////////////////////////////////////////
            if(metaDataWithPermissionRules instanceof QTableMetaData)
            {
               return getPermissionCheckResult(actionInput, rules, permissionBaseName, metaDataWithPermissionRules, TablePermissionSubType.READ, TablePermissionSubType.INSERT, TablePermissionSubType.EDIT, TablePermissionSubType.DELETE);
            }
            return getPermissionCheckResult(actionInput, rules, permissionBaseName, metaDataWithPermissionRules, PrivatePermissionSubType.HAS_ACCESS);
         }
         default:
         {
            return getPermissionDeniedCheckResult(rules);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void checkProcessPermissionThrowing(AbstractActionInput actionInput, String processName) throws QPermissionDeniedException
   {
      checkProcessPermissionThrowing(actionInput, processName, Collections.emptyMap());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void checkProcessPermissionThrowing(AbstractActionInput actionInput, String processName, Map<String, Serializable> processValues) throws QPermissionDeniedException
   {
      QProcessMetaData process = QContext.getQInstance().getProcess(processName);

      if(process == null)
      {
         LOG.info("Throwing a permission denied exception in response to a non-existent process name", logPair("processName", processName));
         throw (new QPermissionDeniedException("Permission denied."));
      }

      commonCheckPermissionThrowing(actionInput, process, PrivatePermissionSubType.HAS_ACCESS, process.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean hasProcessPermission(AbstractActionInput actionInput, String processName)
   {
      try
      {
         checkProcessPermissionThrowing(actionInput, processName);
         return (true);
      }
      catch(QPermissionDeniedException e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void checkAppPermissionThrowing(AbstractActionInput actionInput, String appName) throws QPermissionDeniedException
   {
      QAppMetaData app = QContext.getQInstance().getApp(appName);

      if(app == null)
      {
         LOG.info("Throwing a permission denied exception in response to a non-existent app name", logPair("appName", appName));
         throw (new QPermissionDeniedException("Permission denied."));
      }

      commonCheckPermissionThrowing(actionInput, app, PrivatePermissionSubType.HAS_ACCESS, app.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean hasAppPermission(AbstractActionInput actionInput, String appName)
   {
      try
      {
         checkAppPermissionThrowing(actionInput, appName);
         return (true);
      }
      catch(QPermissionDeniedException e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void checkReportPermissionThrowing(AbstractActionInput actionInput, String reportName) throws QPermissionDeniedException
   {
      QReportMetaData report = QContext.getQInstance().getReport(reportName);

      if(report == null)
      {
         LOG.info("Throwing a permission denied exception in response to a non-existent process name", logPair("reportName", reportName));
         throw (new QPermissionDeniedException("Permission denied."));
      }

      commonCheckPermissionThrowing(actionInput, report, PrivatePermissionSubType.HAS_ACCESS, report.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean hasReportPermission(AbstractActionInput actionInput, String reportName)
   {
      try
      {
         checkReportPermissionThrowing(actionInput, reportName);
         return (true);
      }
      catch(QPermissionDeniedException e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void checkWidgetPermissionThrowing(AbstractActionInput actionInput, String widgetName) throws QPermissionDeniedException
   {
      QWidgetMetaDataInterface widget = QContext.getQInstance().getWidget(widgetName);

      if(widget == null)
      {
         LOG.info("Throwing a permission denied exception in response to a non-existent widget name", logPair("widgetName", widgetName));
         throw (new QPermissionDeniedException("Permission denied."));
      }

      commonCheckPermissionThrowing(actionInput, widget, PrivatePermissionSubType.HAS_ACCESS, widget.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean hasWidgetPermission(AbstractActionInput actionInput, String widgetName)
   {
      try
      {
         checkWidgetPermissionThrowing(actionInput, widgetName);
         return (true);
      }
      catch(QPermissionDeniedException e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Collection<String> getAllAvailablePermissionNames(QInstance instance)
   {
      return (getAllAvailablePermissions(instance).stream()
         .map(AvailablePermission::getName)
         .collect(Collectors.toCollection(LinkedHashSet::new)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Collection<AvailablePermission> getAllAvailablePermissions(QInstance instance)
   {
      Collection<AvailablePermission> rs = new LinkedHashSet<>();

      for(QTableMetaData tableMetaData : instance.getTables().values())
      {
         if(tableMetaData.getIsHidden())
         {
            continue;
         }

         QPermissionRules rules    = getEffectivePermissionRules(tableMetaData, instance);
         String           baseName = getEffectivePermissionBaseName(rules, tableMetaData.getName());

         TableFeaturePermissionsMetaData features = TableFeaturePermissionsMetaData.of(instance);
         if(features != null)
         {
            for(TableFeaturePermission feature : features.getPermissions())
            {
               if(feature.getAppliesTo() == null || feature.getAppliesTo().test(instance, tableMetaData))
               {
                  rs.add(new AvailablePermission().withName(baseName + "." + feature.getSuffix())
                     .withObjectName(tableMetaData.getLabel()).withObjectType("Table").withPermissionType(feature.getLabel()));
               }
            }
         }

         QBackendMetaData backend = instance.getBackend(tableMetaData.getBackendName());
         if(tableMetaData.isCapabilityEnabled(backend, Capability.TABLE_INSERT))
         {
            addEffectiveAvailablePermission(rules, TablePermissionSubType.INSERT, rs, baseName, tableMetaData, "Table");
         }

         if(tableMetaData.isCapabilityEnabled(backend, Capability.TABLE_UPDATE))
         {
            addEffectiveAvailablePermission(rules, TablePermissionSubType.EDIT, rs, baseName, tableMetaData, "Table");
         }

         if(tableMetaData.isCapabilityEnabled(backend, Capability.TABLE_DELETE))
         {
            addEffectiveAvailablePermission(rules, TablePermissionSubType.DELETE, rs, baseName, tableMetaData, "Table");
         }

         if(tableMetaData.isCapabilityEnabled(backend, Capability.TABLE_QUERY) || tableMetaData.isCapabilityEnabled(backend, Capability.TABLE_GET))
         {
            addEffectiveAvailablePermission(rules, TablePermissionSubType.READ, rs, baseName, tableMetaData, "Table");
         }
      }

      for(QProcessMetaData processMetaData : instance.getProcesses().values())
      {
         if(processMetaData.getIsHidden())
         {
            continue;
         }

         QPermissionRules rules    = getEffectivePermissionRules(processMetaData, instance);
         String           baseName = getEffectivePermissionBaseName(rules, processMetaData.getName());
         addEffectiveAvailablePermission(rules, PrivatePermissionSubType.HAS_ACCESS, rs, baseName, processMetaData, "Process");
      }

      for(QAppMetaData appMetaData : instance.getApps().values())
      {
         QPermissionRules rules    = getEffectivePermissionRules(appMetaData, instance);
         String           baseName = getEffectivePermissionBaseName(rules, appMetaData.getName());
         addEffectiveAvailablePermission(rules, PrivatePermissionSubType.HAS_ACCESS, rs, baseName, appMetaData, "App");
      }

      for(QReportMetaData reportMetaData : instance.getReports().values())
      {
         QPermissionRules rules    = getEffectivePermissionRules(reportMetaData, instance);
         String           baseName = getEffectivePermissionBaseName(rules, reportMetaData.getName());
         addEffectiveAvailablePermission(rules, PrivatePermissionSubType.HAS_ACCESS, rs, baseName, reportMetaData, "Report");
      }

      for(QWidgetMetaDataInterface widgetMetaData : instance.getWidgets().values())
      {
         QPermissionRules rules    = getEffectivePermissionRules(widgetMetaData, instance);
         String           baseName = getEffectivePermissionBaseName(rules, widgetMetaData.getName());
         if(!rules.getLevel().equals(PermissionLevel.NOT_PROTECTED))
         {
            addEffectiveAvailablePermission(rules, PrivatePermissionSubType.HAS_ACCESS, rs, baseName, widgetMetaData, "Widget");
         }
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void addEffectiveAvailablePermission(QPermissionRules rules, PermissionSubType permissionSubType, Collection<AvailablePermission> rs, String baseName, MetaDataWithName metaDataWithName, String objectType)
   {
      if(rules.getCustomPermissionChecker() != null)
      {
         ///////////////////////////////////////////////////////////////////////////////////////////////
         // if there's a custom permission checker for this object, and it says that it               //
         // handlesBuildAvailablePermission, then call that method, and add its result to our result. //
         ///////////////////////////////////////////////////////////////////////////////////////////////
         CustomPermissionChecker customPermissionChecker = QCodeLoader.getAdHoc(CustomPermissionChecker.class, rules.getCustomPermissionChecker());
         if(customPermissionChecker.handlesBuildAvailablePermission())
         {
            AvailablePermission availablePermission = customPermissionChecker.buildAvailablePermission(rules, permissionSubType, baseName, metaDataWithName, objectType);
            CollectionUtils.addIfNotNull(rs, availablePermission);
            return;
         }
      }

      PermissionSubType effectivePermissionSubType = getEffectivePermissionSubType(rules, permissionSubType);
      if(effectivePermissionSubType != null)
      {
         rs.add(new AvailablePermission()
            .withName(getPermissionName(baseName, effectivePermissionSubType))
            .withObjectName(metaDataWithName.getLabel())
            .withPermissionType(effectivePermissionSubType.toString())
            .withObjectType(objectType));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QPermissionRules getEffectivePermissionRules(MetaDataWithPermissionRules metaDataWithPermissionRules, QInstance instance)
   {
      if(metaDataWithPermissionRules.getPermissionRules() == null)
      {
         LOG.warn("Null permission rules on meta data object [" + metaDataWithPermissionRules.getClass().getSimpleName() + "][" + metaDataWithPermissionRules.getName() + "] - does the instance need enriched?  Returning instance default rules.");
         return (instance.getDefaultPermissionRules());
      }
      return (metaDataWithPermissionRules.getPermissionRules());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static boolean hasPermission(QSession session, String permissionBaseName, PermissionSubType permissionSubType)
   {
      if(permissionSubType == null)
      {
         return (true);
      }

      String permissionName = getPermissionName(permissionBaseName, permissionSubType);
      return (session.hasPermission(permissionName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static PermissionCheckResult getPermissionCheckResult(AbstractActionInput actionInput, QPermissionRules rules, String permissionBaseName, MetaDataWithPermissionRules metaDataWithPermissionRules, PermissionSubType... permissionSubTypes)
   {
      for(PermissionSubType permissionSubType : permissionSubTypes)
      {
         PermissionSubType effectivePermissionSubType = getEffectivePermissionSubType(rules, permissionSubType);

         if(rules.getCustomPermissionChecker() != null)
         {
            try
            {
               CustomPermissionChecker customPermissionChecker = QCodeLoader.getAdHoc(CustomPermissionChecker.class, rules.getCustomPermissionChecker());
               customPermissionChecker.checkPermissionsThrowing(actionInput, metaDataWithPermissionRules);
               return (PermissionCheckResult.ALLOW);
            }
            catch(QPermissionDeniedException e)
            {
               return (getPermissionDeniedCheckResult(rules));
            }
         }

         if(hasPermission(QContext.getQSession(), permissionBaseName, effectivePermissionSubType))
         {
            return (PermissionCheckResult.ALLOW);
         }
      }

      return (getPermissionDeniedCheckResult(rules));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getEffectivePermissionBaseName(QPermissionRules rules, String standardName)
   {
      if(rules != null && StringUtils.hasContent(rules.getPermissionBaseName()))
      {
         return (rules.getPermissionBaseName());
      }

      return (standardName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static PermissionSubType getEffectivePermissionSubType(QPermissionRules rules, PermissionSubType originalPermissionSubType)
   {
      if(rules == null || rules.getLevel() == null)
      {
         return (originalPermissionSubType);
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if the original permission sub-type is "hasAccess" - then this is a check for a process/report/widget. //
      // in that case - never return the table-level read/write/insert/edit/delete options                      //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(PrivatePermissionSubType.HAS_ACCESS.equals(originalPermissionSubType))
      {
         return switch(rules.getLevel())
         {
            case NOT_PROTECTED -> null;
            default -> PrivatePermissionSubType.HAS_ACCESS;
         };
      }
      else
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         // else, this is a table check - so - based on the rules being used for this table, map the requested //
         // permission sub-type to what we expect to be set for the table                                      //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         return switch(rules.getLevel())
         {
            case NOT_PROTECTED -> null;
            case HAS_ACCESS_PERMISSION -> PrivatePermissionSubType.HAS_ACCESS;
            case READ_WRITE_PERMISSIONS ->
            {
               if(PrivatePermissionSubType.READ.equals(originalPermissionSubType) || PrivatePermissionSubType.WRITE.equals(originalPermissionSubType))
               {
                  yield (originalPermissionSubType);
               }
               else if(TablePermissionSubType.INSERT.equals(originalPermissionSubType) || TablePermissionSubType.EDIT.equals(originalPermissionSubType) || TablePermissionSubType.DELETE.equals(originalPermissionSubType))
               {
                  yield (PrivatePermissionSubType.WRITE);
               }
               else if(TablePermissionSubType.READ.equals(originalPermissionSubType))
               {
                  yield (PrivatePermissionSubType.READ);
               }
               else
               {
                  throw new IllegalStateException("Unexpected permissionSubType: " + originalPermissionSubType);
               }
            }
            case READ_INSERT_EDIT_DELETE_PERMISSIONS -> originalPermissionSubType;
         };
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static void commonCheckPermissionThrowing(AbstractActionInput actionInput, MetaDataWithPermissionRules metaDataWithPermissionRules, PermissionSubType permissionSubType, String name) throws QPermissionDeniedException
   {
      QPermissionRules effectivePermissionRules = getEffectivePermissionRules(metaDataWithPermissionRules, QContext.getQInstance());

      /////////////////////////////////////////////////////////////////////////////////
      // client-channel deny rules come first: nothing (custom checkers, unprotected //
      // objects, the user's permissions) overrides them                             //
      /////////////////////////////////////////////////////////////////////////////////
      if(isDeniedForClientChannel(metaDataWithPermissionRules, effectivePermissionRules) || isOutsideCredentialScope(metaDataWithPermissionRules, permissionSubType))
      {
         LOG.info("Client channel denied", logPair("clientChannel", QContext.getQSession() == null ? null : QContext.getQSession().getClientChannel()), logPair("name", name));
         throw (new QPermissionDeniedException("Permission denied."));
      }

      ////////////////////////////////////////////////////////////////////
      // use the result of a custom permission checker, if there is one //
      ////////////////////////////////////////////////////////////////////
      if(effectivePermissionRules.getCustomPermissionChecker() != null)
      {
         ////////////////////////////////////////////////////////////////////////////////////////////
         // todo - avoid stack overflows if this custom checker comes back through here somehow... //
         ////////////////////////////////////////////////////////////////////////////////////////////
         CustomPermissionChecker customPermissionChecker = QCodeLoader.getAdHoc(CustomPermissionChecker.class, effectivePermissionRules.getCustomPermissionChecker());
         customPermissionChecker.checkPermissionsThrowing(actionInput, metaDataWithPermissionRules);
         return;
      }

      PermissionSubType effectivePermissionSubType = getEffectivePermissionSubType(effectivePermissionRules, permissionSubType);
      String            permissionBaseName         = getEffectivePermissionBaseName(effectivePermissionRules, name);

      if(effectivePermissionSubType == null)
      {
         return;
      }

      if(!hasPermission(QContext.getQSession(), permissionBaseName, effectivePermissionSubType))
      {
         // LOG.debug("Throwing permission denied for: " + getPermissionName(permissionBaseName, effectivePermissionSubType) + " for " + QContext.getQSession().getUser());
         throw (new QPermissionDeniedException("Permission denied."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String getPermissionName(String permissionBaseName, PermissionSubType permissionSubType)
   {
      return permissionBaseName + "." + permissionSubType.getPermissionSuffix();
   }



   /*******************************************************************************
    ** Whether the session's credential scope (if any) excludes this object - or,
    ** for a write sub-type (or any process) under a read-only scope, the action.
    ** A scope only narrows: the user's own permissions are checked as well.
    *******************************************************************************/
   public static boolean isOutsideCredentialScope(MetaDataWithPermissionRules object, PermissionSubType permissionSubType)
   {
      QSession         session = QContext.getQSession();
      QCredentialScope scope   = session == null ? null : session.getCredentialScope();
      if(scope == null)
      {
         return (false);
      }

      String name = object.getName();
      if(object instanceof QTableMetaData)
      {
         return (!QCredentialScope.allows(scope.getTables(), name) || (scope.getReadOnly() && isWriteSubType(permissionSubType)));
      }
      else if(object instanceof QProcessMetaData)
      {
         ///////////////////////////////////////////////////////////////////////////////
         // processes can write; a read-only scope allows only explicitly listed ones //
         ///////////////////////////////////////////////////////////////////////////////
         if(scope.getReadOnly() && scope.getProcesses() == null)
         {
            return (true);
         }
         return (!QCredentialScope.allows(scope.getProcesses(), name));
      }
      else if(object instanceof QReportMetaData)
      {
         return (!QCredentialScope.allows(scope.getReports(), name));
      }
      else if(object instanceof QWidgetMetaDataInterface)
      {
         return (!QCredentialScope.allows(scope.getWidgets(), name));
      }
      else if(object instanceof QAppMetaData)
      {
         return (!QCredentialScope.allows(scope.getApps(), name));
      }
      return (false);
   }



   /*******************************************************************************
    ** Whether a permission sub-type is a write (insert, edit, delete, write).
    *******************************************************************************/
   private static boolean isWriteSubType(PermissionSubType permissionSubType)
   {
      return (permissionSubType == TablePermissionSubType.INSERT || permissionSubType == TablePermissionSubType.EDIT
         || permissionSubType == TablePermissionSubType.DELETE || permissionSubType == PrivatePermissionSubType.WRITE);
   }



   /*******************************************************************************
    ** Whether the session's client channel (if any) is denied this object -
    ** by its rules' deniedClients, or the instance's ClientChannelsMetaData
    ** (whose defaults apply when the instance registers none).
    *******************************************************************************/
   public static boolean isDeniedForClientChannel(MetaDataWithPermissionRules metaDataWithPermissionRules, QPermissionRules effectiveRules)
   {
      QSession session = QContext.getQSession();
      String   channel = session == null ? null : session.getClientChannelName();
      if(channel == null)
      {
         return (false);
      }

      return (ClientChannelsMetaData.of(QContext.getQInstance()).isDenied(channel, metaDataWithPermissionRules.getName(), effectiveRules));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static PermissionCheckResult getPermissionDeniedCheckResult(QPermissionRules rules)
   {
      if(rules == null || rules.getDenyBehavior() == null || rules.getDenyBehavior().equals(DenyBehavior.HIDDEN))
      {
         return (PermissionCheckResult.DENY_HIDE);
      }
      else
      {
         return (PermissionCheckResult.DENY_DISABLE);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void warnAboutPermissionSubTypeForTables(PermissionSubType permissionSubType)
   {
      if(permissionSubType == null)
      {
         return;
      }

      if(permissionSubType == PrivatePermissionSubType.HAS_ACCESS)
      {
         LOG.warn("PermissionSubType.HAS_ACCESS should not be checked for a table");
      }
   }

}
