/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStatCriteriaField;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStatJoinTable;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStatOrderByField;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSessionRefreshToken;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Permission rules for QQQ's own system tables: the tables the framework
 ** writes for itself, about every user, rather than an application's data.
 **
 ** Two kinds:
 ** - Readable system tables (the audit trail and query statistics) require an
 **   explicit permission to read directly (table query, get, count, export,
 **   reports and possible values, through every API): audit.read for the audit
 **   tables, queryStat.read for the query statistics tables.  Holders see them
 **   as ordinary tables.  Everyone else still sees a record's history through
 **   the GetAuditsForRecord process, which checks that they may read the
 **   record instead.
 ** - Internal tables (userSession, redirectState, userSessionRefreshToken) hold
 **   session ids, access tokens, refresh tokens and login state.  They are
 **   hidden and require a permission no role is ever granted, so no user,
 **   admins included, can query them; only the authentication modules (as the
 **   system user) read and write them.
 **
 ** The rules are set by the tables' meta-data providers.  An application that
 ** sets its own permission rules on one of these tables after defining it
 ** replaces them.
 *******************************************************************************/
public final class SystemTablePermissions
{
   /** permission base name shared by the four audit tables */
   public static final String AUDIT_PERMISSION_BASE_NAME = "audit";

   /** the permission to read the audit tables directly */
   public static final String AUDIT_READ_PERMISSION = AUDIT_PERMISSION_BASE_NAME + "." + TablePermissionSubType.READ.getPermissionSuffix();

   /** permission base name shared by the four query statistics tables */
   public static final String QUERY_STAT_PERMISSION_BASE_NAME = "queryStat";

   /** the permission to read the query statistics tables directly */
   public static final String QUERY_STAT_READ_PERMISSION = QUERY_STAT_PERMISSION_BASE_NAME + "." + TablePermissionSubType.READ.getPermissionSuffix();

   public static final List<String> AUDIT_TABLE_NAMES = List.of(
      AuditsMetaDataProvider.TABLE_NAME_AUDIT,
      AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL,
      AuditsMetaDataProvider.TABLE_NAME_AUDIT_TABLE,
      AuditsMetaDataProvider.TABLE_NAME_AUDIT_USER);

   public static final List<String> QUERY_STAT_TABLE_NAMES = List.of(
      QueryStat.TABLE_NAME,
      QueryStatJoinTable.TABLE_NAME,
      QueryStatCriteriaField.TABLE_NAME,
      QueryStatOrderByField.TABLE_NAME);

   public static final List<String> INTERNAL_TABLE_NAMES = List.of(
      UserSession.TABLE_NAME,
      RedirectStateMetaDataProducer.TABLE_NAME,
      UserSessionRefreshToken.TABLE_NAME);

   private static final Set<String> ALL_SYSTEM_TABLE_NAMES = Stream.of(AUDIT_TABLE_NAMES, QUERY_STAT_TABLE_NAMES, INTERNAL_TABLE_NAMES).flatMap(List::stream).collect(Collectors.toUnmodifiableSet());



   /*******************************************************************************
    ** Utility class - no instances.
    *******************************************************************************/
   private SystemTablePermissions()
   {
   }



   /*******************************************************************************
    ** Whether a table name is one of QQQ's system tables (see the class comment).
    *******************************************************************************/
   public static boolean isSystemTable(String tableName)
   {
      return (tableName != null && ALL_SYSTEM_TABLE_NAMES.contains(tableName));
   }



   /*******************************************************************************
    ** Rules for a readable system table: reading it needs [baseName].read.  An
    ** instance whose default rules are protected keeps its level (so, e.g.,
    ** HAS_ACCESS_PERMISSION instances use [baseName].hasAccess); an unprotected
    ** default becomes READ_INSERT_EDIT_DELETE_PERMISSIONS.  The tables have no
    ** insert, edit or delete capabilities, so only read is ever offered.
    *******************************************************************************/
   public static QPermissionRules readableSystemTableRules(QInstance instance, String permissionBaseName)
   {
      QPermissionRules defaults = instance == null ? null : instance.getDefaultPermissionRules();
      PermissionLevel  level    = defaults == null ? null : defaults.getLevel();
      if(level == null || level == PermissionLevel.NOT_PROTECTED)
      {
         level = PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS;
      }

      return (new QPermissionRules()
         .withLevel(level)
         .withPermissionBaseName(permissionBaseName)
         .withDenyBehavior(DenyBehavior.HIDDEN));
   }



   /*******************************************************************************
    ** Rules for an internal table: a dedicated permission that no role should
    ** ever be granted (and that, as the table is hidden, is not offered among the
    ** instance's available permissions).
    *******************************************************************************/
   public static QPermissionRules internalTableRules(String tableName)
   {
      return (new QPermissionRules()
         .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
         .withPermissionBaseName(noAccessPermissionBaseName(tableName))
         .withDenyBehavior(DenyBehavior.HIDDEN));
   }



   /*******************************************************************************
    ** The never-granted permission base name of an internal table.
    *******************************************************************************/
   public static String noAccessPermissionBaseName(String tableName)
   {
      return ("qqq.internal." + tableName + ".noAccess");
   }



   /*******************************************************************************
    ** Hide and lock an internal table (see internalTableRules).
    *******************************************************************************/
   public static QTableMetaData lockInternalTable(QTableMetaData table)
   {
      table.setIsHidden(true);
      table.setPermissionRules(internalTableRules(table.getName()));
      return (table);
   }



   /*******************************************************************************
    ** Called by the enricher: lock the OAuth2 session, redirect-state and
    ** refresh-token tables an instance names in its authentication meta-data,
    ** for any that the application defined itself (not through QQQ's
    ** producers, which lock them already) without permission rules.
    *******************************************************************************/
   public static void lockConfiguredAuthenticationTables(QInstance instance)
   {
      List<QAuthenticationMetaData> providers = new ArrayList<>();
      providers.add(instance.getAuthentication());
      providers.addAll(CollectionUtils.nonNullMap(instance.getScopedAuthenticationProviders()).values());
      for(QAuthenticationMetaData provider : providers)
      {
         if(provider instanceof OAuth2AuthenticationMetaData oauth2)
         {
            for(String tableName : List.of(String.valueOf(oauth2.getUserSessionTableName()), String.valueOf(oauth2.getRedirectStateTableName()), String.valueOf(oauth2.getRefreshTokenTableName())))
            {
               QTableMetaData table = instance.getTable(tableName);
               if(table != null && table.getPermissionRules() == null)
               {
                  lockInternalTable(table);
               }
            }
         }
      }
   }

}
