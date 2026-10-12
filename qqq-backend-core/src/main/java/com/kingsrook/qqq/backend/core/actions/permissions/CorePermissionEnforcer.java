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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeSupplier;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Permission enforcement inside the core actions (issues #1061 and #1062).
 **
 ** Historically, table/process/widget/report permissions were checked only by
 ** the middlewares (Javalin, API, picocli, Slack) before they called a core
 ** action - so any other entry point (MCP, an AI assistant, custom routes,
 ** scripts) bypassed them, and FieldSecurityLock was not enforced at all.
 **
 ** The core actions now call this class.  Whether a given call is enforced:
 ** - input.getPermissionEnforcement() == SKIP (set only through
 **   withPermissionBypass(reason)): never enforced.
 ** - == ENFORCE: always enforced - even when nested inside another action, and
 **   even if the instance setting is off.
 ** - == DEFAULT: enforced only for an "outermost" call (one not running inside
 **   another core action - see QContext.getAuthorizedActionScopeDepth) and only
 **   if the instance setting enforcePermissionsInCoreActions is on.
 **
 ** Calls nested inside a core action (pre/post customizers, process steps,
 ** audits, possible-value translation, widget renderers, async children of
 ** those) are framework-internal and trusted - the outer action already applied
 ** the check owed to its caller.  This preserves QQQ's existing model where,
 ** e.g., a process the user may run can read tables the user can't.
 **
 ** System user sessions (QSystemUserSession) pass every permission check and
 ** field lock, so scheduled jobs, automations and ESB handlers that run as the
 ** system user are unaffected; the framework's own schedulers, automation and
 ** queue pollers also mark their process runs with withPermissionBypass.
 **
 ** Record security locks are not handled here - they are enforced (as before)
 ** inside the backend modules and the DML actions, for every call.
 *******************************************************************************/
public final class CorePermissionEnforcer
{
   private static final QLogger LOG = QLogger.getLogger(CorePermissionEnforcer.class);

   public static final String SYSTEM_PROPERTY_NAME       = "qqq.enforcePermissionsInCoreActions";
   public static final String ENVIRONMENT_VARIABLE_NAME = "QQQ_ENFORCE_PERMISSIONS_IN_CORE_ACTIONS";

   private static final AtomicBoolean LOGGED_NOT_CONFIGURED = new AtomicBoolean(false);



   /*******************************************************************************
    ** private constructor - static utility class.
    *******************************************************************************/
   private CorePermissionEnforcer()
   {
   }



   /*******************************************************************************
    ** The effective instance-level setting: QInstance.enforcePermissionsInCoreActions
    ** if set; else the system property / environment variable if set; else false.
    *******************************************************************************/
   public static boolean isInstanceEnforcementEnabled()
   {
      QInstance qInstance = QContext.getQInstance();
      if(qInstance != null && qInstance.getEnforcePermissionsInCoreActions() != null)
      {
         return (qInstance.getEnforcePermissionsInCoreActions());
      }

      String value = System.getProperty(SYSTEM_PROPERTY_NAME);
      if(!StringUtils.hasContent(value))
      {
         value = System.getenv(ENVIRONMENT_VARIABLE_NAME);
      }

      if(StringUtils.hasContent(value))
      {
         return ("true".equalsIgnoreCase(value.trim()));
      }

      if(LOGGED_NOT_CONFIGURED.compareAndSet(false, true))
      {
         LOG.info("QInstance.enforcePermissionsInCoreActions is not configured, so core actions only enforce permissions for inputs that request PermissionEnforcement.ENFORCE.  The default is planned to become true in QQQ 4.2 - set it explicitly to keep the current behavior.");
      }
      return (false);
   }



   /*******************************************************************************
    ** Should permissions (and field security locks) be enforced for this input,
    ** at this point (e.g., before its action opens its authorized scope)?
    *******************************************************************************/
   public static boolean shouldEnforce(AbstractActionInput input)
   {
      if(input == null)
      {
         return (false);
      }

      switch(input.getPermissionEnforcement())
      {
         case SKIP ->
         {
            LOG.debug("Bypassing core permission enforcement", logPair("action", input.getActionIdentity()), logPair("reason", input.getPermissionBypassReason()));
            return (false);
         }
         case ENFORCE ->
         {
            return (true);
         }
         default ->
         {
            if(QContext.getAuthorizedActionScopeDepth() > 0)
            {
               return (false);
            }
            return (isInstanceEnforcementEnabled());
         }
      }
   }



   /*******************************************************************************
    ** If enforcing for this input, check the table permission(s) - throwing
    ** QPermissionDeniedException if denied.  Returns whether enforcement applied
    ** (so the caller can also apply field security locks and the like).
    *******************************************************************************/
   public static boolean checkTablePermissions(AbstractTableActionInput input, TablePermissionSubType... permissionSubTypes) throws QException
   {
      if(!shouldEnforce(input))
      {
         return (false);
      }

      for(TablePermissionSubType permissionSubType : permissionSubTypes)
      {
         PermissionsHelper.checkTablePermissionThrowing(input, permissionSubType);
      }
      return (true);
   }



   /*******************************************************************************
    ** For read-style actions that are being enforced: check READ on every joined
    ** table (explicit or implied by the filter), and that the filter / additional
    ** field names don't reference a field denied by a field security lock.
    *******************************************************************************/
   public static void checkReadCriteria(AbstractTableActionInput input, List<QueryJoin> queryJoins, QQueryFilter filter, Collection<String> additionalFieldNames) throws QException
   {
      PermissionsHelper.checkJoinedTableReadPermissions(input, queryJoins, filter);
      FieldSecurityLockHelper.assertCriteriaAccessible(input.getTableName(), queryJoins, filter, additionalFieldNames);
   }



   /*******************************************************************************
    ** If enforcing for this input, check the process permission (using the input's
    ** process values, for custom permission checkers that consider them).
    *******************************************************************************/
   public static boolean checkProcessPermission(AbstractActionInput input, String processName, Map<String, Serializable> processValues) throws QException
   {
      if(!shouldEnforce(input))
      {
         return (false);
      }

      PermissionsHelper.checkProcessPermissionThrowing(input, processName, processValues == null ? Collections.emptyMap() : processValues);
      return (true);
   }



   /*******************************************************************************
    ** For write actions that are being enforced: check permissions (and field
    ** security locks) for the associated child records the write will insert,
    ** update or delete - including an update's omitted children and a delete's
    ** cascades.  Discovery queries run inside an authorized scope; the permission
    ** checks themselves are explicit.
    *******************************************************************************/
   public static void checkAssociatedWrites(AbstractTableActionInput input) throws QException
   {
      try(AuthorizedScope ignored = openAuthorizedScope())
      {
         if(input instanceof InsertInput insertInput && hasAssociatedRecords(insertInput.getRecords()))
         {
            AssociatedWritePermissions.checkWithFieldSecurityLocks(insertInput);
         }
         else if(input instanceof UpdateInput updateInput && hasAssociatedRecords(updateInput.getRecords()))
         {
            AssociatedWritePermissions.checkWithFieldSecurityLocks(updateInput);
         }
         else if(input instanceof DeleteInput deleteInput && input.getTable() != null && CollectionUtils.nullSafeHasContents(input.getTable().getAssociations()))
         {
            List<Serializable> primaryKeys = deleteInput.getPrimaryKeys();
            if(CollectionUtils.nullSafeIsEmpty(primaryKeys) && deleteInput.getQueryFilter() != null)
            {
               ///////////////////////////////////////////////////////////////
               // resolve a filter to the keys it would delete, to check    //
               // the cascades those records would cause                    //
               ///////////////////////////////////////////////////////////////
               String     primaryKeyField = input.getTable().getPrimaryKeyField();
               QueryInput queryInput      = new QueryInput(input.getTableName());
               queryInput.setFilter(deleteInput.getQueryFilter().clone());
               queryInput.setTransaction(deleteInput.getTransaction());
               primaryKeys = new ArrayList<>();
               for(QRecord record : new QueryAction().execute(queryInput).getRecords())
               {
                  primaryKeys.add(record.getValue(primaryKeyField));
               }
            }

            if(CollectionUtils.nullSafeHasContents(primaryKeys))
            {
               AssociatedWritePermissions.checkWithFieldSecurityLocks(new DeleteInput(input.getTableName())
                  .withPrimaryKeys(primaryKeys)
                  .withTransaction(deleteInput.getTransaction()));
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean hasAssociatedRecords(List<QRecord> records)
   {
      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         if(record != null && CollectionUtils.nullSafeHasContents(record.getAssociatedRecords()))
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** Open an authorized action scope, to be closed via try-with-resources.  Core
    ** actions do this after applying whatever enforcement is owed to their caller,
    ** so the actions they call internally are trusted.
    *******************************************************************************/
   public static AuthorizedScope openAuthorizedScope()
   {
      QContext.enterAuthorizedActionScope();
      return (new AuthorizedScope());
   }



   /*******************************************************************************
    ** Run framework- or application-internal code as trusted: core actions called
    ** by the supplier (with PermissionEnforcement.DEFAULT) are not subject to the
    ** instance-level permission enforcement.  The reason is required, and logged
    ** at debug level - grep for runTrusted / withPermissionBypass to audit.
    *******************************************************************************/
   public static <T, E extends Exception> T runTrusted(String reason, UnsafeSupplier<T, E> supplier) throws E
   {
      if(!StringUtils.hasContent(reason))
      {
         throw (new IllegalArgumentException("A reason is required to run code as trusted"));
      }

      LOG.debug("Running trusted code (core permission enforcement bypassed)", logPair("reason", reason));
      try(AuthorizedScope ignored = openAuthorizedScope())
      {
         return (supplier.get());
      }
   }



   /*******************************************************************************
    ** AutoCloseable handle on an authorized action scope.
    *******************************************************************************/
   public static class AuthorizedScope implements AutoCloseable
   {
      private boolean closed = false;



      /*******************************************************************************
       ** close the scope (idempotent).
       *******************************************************************************/
      @Override
      public void close()
      {
         if(!closed)
         {
            closed = true;
            QContext.exitAuthorizedActionScope();
         }
      }
   }

}
