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

package com.kingsrook.qqq.backend.core.actions.scripts;


import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.AdHocScriptCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptExecutionMode;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevision;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeSupplier;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Core-owned guard for the Script execution-isolation marker (the Script
 ** table's executionMode field - see ScriptExecutionMode).
 **
 ** A Script marked ISOLATED holds fully untrusted source.  It must only ever be
 ** run by an isolated engine, so every legacy, in-process execution path
 ** (ExecuteCodeAction, and so the testScript process, the record/associated
 ** script actions, and so on) refuses it - see requireLegacyExecutionAllowed.
 **
 ** The marker is server-controlled and monotonic:
 ** - it is never read from, or written by, a caller-supplied value.  Generic
 **   table writes (see ScriptTableCustomizer) can neither set nor clear it, and
 **   cannot move an ISOLATED script's currentScriptRevisionId.
 ** - only Java code running inside an "isolation authority" scope (see
 **   runAsIsolationAuthority and the typed methods below) may mark a new Script
 **   ISOLATED, or activate one of its revisions.  There is no way to enter the
 **   scope from an http request, and it is refused while a script-origin scope
 **   (see openScriptOriginScope) is open on the thread.
 ** - once ISOLATED, a Script can never go back to LEGACY, even in the scope.
 **
 ** Marker reads are always made as the system user, so a caller's permissions or
 ** record-security locks can never hide a marked Script.  An unmarked Script
 ** (every row that pre-dates the column) is LEGACY and keeps running as before.
 **
 ** Fail closed: any stored script / revision id that is supplied must resolve to
 ** a stored row.  A missing (deleted, orphaned, never-existing, or not-yet-visible)
 ** script or revision, or an instance that can't report the marker, is refused -
 ** never treated as LEGACY.  Only an execution with NO stored identity at all
 ** (standalone, trusted, inline code) is exempt.
 **
 ** TRUST MODEL: LEGACY scripts are trusted, Java-capable, in-process code (the
 ** legacy engines can reach arbitrary Java).  The script-origin thread-local guard
 ** below is NOT a sandbox - it only stops cooperative host code from using the
 ** isolation-authority API on a script's behalf; a hostile LEGACY script is
 ** outside its threat model, which is why untrusted source must be ISOLATED.
 *******************************************************************************/
public class ScriptExecutionIsolation
{
   private static final QLogger LOG = QLogger.getLogger(ScriptExecutionIsolation.class);

   public static final String EXECUTION_MODE_FIELD_NAME = "executionMode";

   private static final ThreadLocal<Integer> authorityDepth    = new ThreadLocal<>();
   private static final ThreadLocal<Integer> scriptOriginDepth = new ThreadLocal<>();



   /*******************************************************************************
    ** private constructor - static utility class.
    *******************************************************************************/
   private ScriptExecutionIsolation()
   {
   }



   /*******************************************************************************
    ** Refuse (by throwing) if the script with the given id is ISOLATED.  A null id
    ** is allowed - there is no project to be marked.
    *******************************************************************************/
   public static void requireLegacyExecutionAllowed(Integer scriptId) throws QException
   {
      requireLegacyExecutionAllowed(scriptId, null);
   }



   /*******************************************************************************
    ** Refuse (by throwing) if the script with the given id is ISOLATED - reading
    ** the marker in the given transaction (so a caller sees its own uncommitted
    ** work).
    *******************************************************************************/
   public static void requireLegacyExecutionAllowed(Integer scriptId, QBackendTransaction transaction) throws QException
   {
      if(getExecutionMode(scriptId, transaction) == ScriptExecutionMode.ISOLATED)
      {
         LOG.warn("Refusing legacy execution of an isolated script", logPair("scriptId", scriptId));
         throw (new QPermissionDeniedException("Script " + scriptId + " is isolated, and cannot be run, tested, or stored by the legacy in-process script engine."));
      }
   }



   /*******************************************************************************
    ** Refuse (by throwing) if any Script that the code reference, scriptId or
    ** scriptRevisionId resolves to is ISOLATED.  The ids are resolved here, from
    ** the stored data - never from a caller-supplied flag.  That is: the scriptId
    ** given; the scriptId of the stored revision with the scriptRevisionId given;
    ** and, for an AdHocScriptCodeReference, its scriptId, its scriptRevisionRecord's
    ** scriptId, and the scriptId of the stored revision for its scriptRevisionId.
    ** ALL of them are checked.
    *******************************************************************************/
   public static void requireLegacyExecutionAllowed(QCodeReference codeReference, Integer scriptId, Integer scriptRevisionId) throws QException
   {
      Set<Integer> scriptIds         = new LinkedHashSet<>();
      Set<Integer> scriptRevisionIds = new LinkedHashSet<>();
      if(scriptId != null)
      {
         scriptIds.add(scriptId);
      }
      if(scriptRevisionId != null)
      {
         scriptRevisionIds.add(scriptRevisionId);
      }

      if(codeReference instanceof AdHocScriptCodeReference adHocScriptCodeReference)
      {
         if(adHocScriptCodeReference.getScriptId() != null)
         {
            scriptIds.add(adHocScriptCodeReference.getScriptId());
         }

         if(adHocScriptCodeReference.getScriptRevisionRecord() != null)
         {
            QRecord revisionRecord = adHocScriptCodeReference.getScriptRevisionRecord();
            if(revisionRecord.getValueInteger("scriptId") != null)
            {
               scriptIds.add(revisionRecord.getValueInteger("scriptId"));
            }
            if(revisionRecord.getValueInteger("id") != null)
            {
               scriptRevisionIds.add(revisionRecord.getValueInteger("id"));
            }
         }

         if(adHocScriptCodeReference.getScriptRevisionId() != null)
         {
            scriptRevisionIds.add(adHocScriptCodeReference.getScriptRevisionId());
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////
      // no stored identity at all (e.g., standalone, trusted, inline java) - nothing to //
      // resolve, so nothing to refuse.  But ANY identity that was supplied must be      //
      // resolved - a caller-supplied id never causes a fall-through to "legacy".        //
      /////////////////////////////////////////////////////////////////////////////////////
      if(scriptIds.isEmpty() && scriptRevisionIds.isEmpty())
      {
         return;
      }

      requireMarkerResolvable();

      for(Integer id : scriptRevisionIds)
      {
         QRecord revision = getRecordAsSystem(ScriptRevision.TABLE_NAME, id, null);
         if(revision == null || revision.getValueInteger("scriptId") == null)
         {
            throw (unresolved("script revision " + id));
         }
         scriptIds.add(revision.getValueInteger("scriptId"));
      }

      for(Integer id : scriptIds)
      {
         requireLegacyExecutionAllowed(id, null);
      }
   }



   /*******************************************************************************
    ** A restore-mode write (RestoreWriteFlag) skips table customizers by design, but
    ** the execution marker is server-controlled: the Script table's guard still
    ** applies to it, so a restore can neither set ISOLATED outside the isolation
    ** authority nor move an isolated script back to LEGACY.  Other tables are
    ** untouched.
    *******************************************************************************/
   public static void guardCustomizerSkippedInsert(InsertInput insertInput) throws QException
   {
      if(isScriptTableWithMarker(insertInput.getTableName()))
      {
         new ScriptTableCustomizer().preInsert(insertInput, insertInput.getRecords(), insertInput.getDryRun());
      }
   }



   /*******************************************************************************
    ** Update counterpart of guardCustomizerSkippedInsert.
    *******************************************************************************/
   public static void guardCustomizerSkippedUpdate(UpdateInput updateInput, Optional<List<QRecord>> oldRecordList) throws QException
   {
      if(isScriptTableWithMarker(updateInput.getTableName()))
      {
         new ScriptTableCustomizer().preUpdate(updateInput, updateInput.getRecords(), updateInput.getDryRun(), oldRecordList);
      }
   }



   /*******************************************************************************
    ** Is this the Script table (the one with the executionMode field)?
    *******************************************************************************/
   private static boolean isScriptTableWithMarker(String tableName)
   {
      QTableMetaData table = getScriptTableWithMarker();
      return (table != null && table.getName().equals(tableName));
   }



   /*******************************************************************************
    ** Fail closed: if the instance can't tell us what is marked (no instance in the
    ** context, no script table, or a script table with no executionMode field),
    ** then a stored identity can't be resolved.
    *******************************************************************************/
   private static void requireMarkerResolvable() throws QPermissionDeniedException
   {
      if(getScriptTableWithMarker() == null)
      {
         throw (unresolved("script execution mode (no script table with an executionMode field is available)"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QPermissionDeniedException unresolved(String what)
   {
      LOG.warn("Refusing legacy execution - a stored script identity could not be resolved", logPair("what", what));
      return (new QPermissionDeniedException("The " + what + " could not be resolved, so it cannot be run, tested, or stored by the legacy in-process script engine."));
   }



   /*******************************************************************************
    ** Get the execution mode of a script, always reading as the system user.  A
    ** null id (no stored project) is LEGACY.  A non-null id that can't be resolved
    ** - no such script, or an instance that can't report the marker - throws
    ** QPermissionDeniedException (fail closed).  A stored value that isn't
    ** recognized fails closed too, as ISOLATED.
    **
    ** The optional transaction lets the caller see its own uncommitted marker.
    *******************************************************************************/
   public static ScriptExecutionMode getExecutionMode(Integer scriptId, QBackendTransaction transaction) throws QException
   {
      if(scriptId == null)
      {
         return (ScriptExecutionMode.LEGACY);
      }

      requireMarkerResolvable();

      QRecord script = getRecordAsSystem(Script.TABLE_NAME, scriptId, transaction);
      if(script == null)
      {
         /////////////////////////////////////////////////////////////////////////////////////////
         // a deleted, never-existing, or not-yet-visible (uncommitted, in another transaction) //
         // script is NOT legacy - its revisions could belong to an isolated project            //
         /////////////////////////////////////////////////////////////////////////////////////////
         throw (unresolved("script " + scriptId));
      }

      return (ScriptExecutionMode.fromValue(script.getValue(EXECUTION_MODE_FIELD_NAME)));
   }



   /*******************************************************************************
    ** Does any stored revision (read as the system user) name the given script id?
    ** Used to refuse re-inserting a script under an explicit id that orphaned
    ** revisions still reference.
    *******************************************************************************/
   static boolean hasRevisionsAsSystem(Integer scriptId, QBackendTransaction transaction) throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      if(qInstance == null || qInstance.getTable(ScriptRevision.TABLE_NAME) == null)
      {
         return (false);
      }

      boolean[]           result               = new boolean[1];
      QBackendTransaction effectiveTransaction = transaction != null ? transaction : QContext.getQBackendTransaction();
      QContext.withTemporaryContext(new CapturedContext(qInstance, new QSystemUserSession(qInstance), effectiveTransaction, null), () ->
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(ScriptRevision.TABLE_NAME);
         queryInput.setTransaction(effectiveTransaction);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("scriptId", QCriteriaOperator.EQUALS, scriptId)).withLimit(1));
         result[0] = CollectionUtils.nullSafeHasContents(new QueryAction().execute(queryInput).getRecords());
      });

      return (result[0]);
   }



   /*******************************************************************************
    ** Insert a new Script that is marked ISOLATED in the same insert - so there
    ** is never a point at which the project exists unmarked.  Create its
    ** revisions and files only after this returns.  It must not already have a
    ** currentScriptRevisionId (activate one with activateIsolatedRevision).
    **
    ** Pass the transaction that the rest of the project creation will use, if any.
    *******************************************************************************/
   public static QRecord insertIsolatedScript(Script script, QBackendTransaction transaction) throws QException
   {
      if(script.getCurrentScriptRevisionId() != null)
      {
         throw (new QException("A new isolated script cannot have a current script revision."));
      }

      script.setId(null);
      script.setExecutionMode(ScriptExecutionMode.ISOLATED.name());

      return (runAsIsolationAuthority("insert isolated script", () ->
      {
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(Script.TABLE_NAME);
         insertInput.setRecords(List.of(script.toQRecord()));
         insertInput.setTransaction(transaction);
         InsertOutput insertOutput = new InsertAction().execute(insertInput);

         QRecord inserted = insertOutput.getRecords().get(0);
         if(CollectionUtils.nullSafeHasContents(inserted.getErrors()))
         {
            throw (new QException("Error inserting isolated script: " + inserted.getErrors().get(0).getMessage()));
         }
         return (inserted);
      }));
   }



   /*******************************************************************************
    ** Point an ISOLATED script at one of its own revisions (the only way an
    ** isolated project's currentScriptRevisionId can change).  Refuses a script
    ** that isn't ISOLATED (legacy scripts are activated by the legacy store
    ** process), and a revision that belongs to some other script.
    *******************************************************************************/
   public static void activateIsolatedRevision(Integer scriptId, Integer scriptRevisionId, QBackendTransaction transaction) throws QException
   {
      if(getExecutionMode(scriptId, transaction) != ScriptExecutionMode.ISOLATED)
      {
         throw (new QException("Script " + scriptId + " is not an isolated script."));
      }

      QRecord revision = getRecordAsSystem(ScriptRevision.TABLE_NAME, scriptRevisionId, transaction);
      if(revision == null || !scriptId.equals(revision.getValueInteger("scriptId")))
      {
         throw (new QException("Script revision " + scriptRevisionId + " does not belong to script " + scriptId + "."));
      }

      runAsIsolationAuthority("activate isolated script revision", () ->
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(Script.TABLE_NAME);
         updateInput.setRecords(List.of(new QRecord().withValue("id", scriptId).withValue("currentScriptRevisionId", scriptRevisionId)));
         updateInput.setTransaction(transaction);
         new UpdateAction().execute(updateInput);
         return (null);
      });
   }



   /*******************************************************************************
    ** Run server-side Java code that is allowed to set the marker, as described
    ** in the class comment.  The reason is required (and is logged at debug).
    ** Refused while a script-origin scope is open on this thread.
    *******************************************************************************/
   public static <T, E extends Exception> T runAsIsolationAuthority(String reason, UnsafeSupplier<T, E> supplier) throws E
   {
      if(!StringUtils.hasContent(reason))
      {
         throw (new IllegalArgumentException("A reason is required to run as the isolation authority"));
      }

      if(depth(scriptOriginDepth) > 0)
      {
         throw (new IllegalStateException("Script-origin code cannot act as the isolation authority"));
      }

      LOG.debug("Running as the script isolation authority", logPair("reason", reason));
      authorityDepth.set(depth(authorityDepth) + 1);
      try
      {
         return (supplier.get());
      }
      finally
      {
         exit(authorityDepth);
      }
   }



   /*******************************************************************************
    ** Is the current thread running inside runAsIsolationAuthority (and not inside
    ** a script-origin scope)?
    *******************************************************************************/
   public static boolean isIsolationAuthorityActive()
   {
      return (depth(authorityDepth) > 0 && depth(scriptOriginDepth) == 0);
   }



   /*******************************************************************************
    ** Mark the current thread as running script-origin code (e.g., while a script
    ** engine runs, and the host callbacks it makes on this thread).  Such code can
    ** never be the isolation authority.  ExecuteCodeAction does this for every
    ** legacy script it runs; isolated engines should do the same around their host
    ** bridge.  Close via try-with-resources.
    *******************************************************************************/
   public static ScriptOriginScope openScriptOriginScope()
   {
      scriptOriginDepth.set(depth(scriptOriginDepth) + 1);
      return (new ScriptOriginScope());
   }



   /*******************************************************************************
    ** AutoCloseable handle on a script-origin scope.  The scope belongs to the
    ** thread that opened it: only that thread can close it (any other thread gets
    ** an IllegalStateException, and the scope stays open), and closing is
    ** idempotent - a second close never unwinds an enclosing scope.
    *******************************************************************************/
   public static class ScriptOriginScope implements AutoCloseable
   {
      private final Thread        owner  = Thread.currentThread();
      private final AtomicBoolean closed = new AtomicBoolean(false);



      /*******************************************************************************
       ** close the scope - on the thread that opened it (idempotent).
       *******************************************************************************/
      @Override
      public void close()
      {
         if(Thread.currentThread() != owner)
         {
            throw (new IllegalStateException("A script-origin scope can only be closed by the thread that opened it"));
         }

         if(closed.compareAndSet(false, true))
         {
            exit(scriptOriginDepth);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int depth(ThreadLocal<Integer> threadLocal)
   {
      Integer depth = threadLocal.get();
      return (depth == null ? 0 : depth);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void exit(ThreadLocal<Integer> threadLocal)
   {
      int depth = depth(threadLocal);
      if(depth <= 1)
      {
         threadLocal.remove();
      }
      else
      {
         threadLocal.set(depth - 1);
      }
   }



   /*******************************************************************************
    ** The script table - if the instance has one, with the executionMode field.
    *******************************************************************************/
   private static QTableMetaData getScriptTableWithMarker()
   {
      QInstance qInstance = QContext.getQInstance();
      if(qInstance == null)
      {
         return (null);
      }

      QTableMetaData table = qInstance.getTable(Script.TABLE_NAME);
      return (table != null && table.getFields().containsKey(EXECUTION_MODE_FIELD_NAME) ? table : null);
   }



   /*******************************************************************************
    ** Get the stored Script record, as the system user (see getRecordAsSystem).
    *******************************************************************************/
   static QRecord getScriptRecordAsSystem(Integer scriptId, QBackendTransaction transaction) throws QException
   {
      return (getRecordAsSystem(Script.TABLE_NAME, scriptId, transaction));
   }



   /*******************************************************************************
    ** Get a record by primary key as the system user - so neither the caller's
    ** permissions nor any record-security lock can hide it.  Uses the caller's
    ** transaction (or the one in the context), if there is one.
    *******************************************************************************/
   private static QRecord getRecordAsSystem(String tableName, Serializable primaryKey, QBackendTransaction transaction) throws QException
   {
      QRecord[]           result               = new QRecord[1];
      QInstance           qInstance            = QContext.getQInstance();
      QBackendTransaction effectiveTransaction = transaction != null ? transaction : QContext.getQBackendTransaction();

      QContext.withTemporaryContext(new CapturedContext(qInstance, new QSystemUserSession(qInstance), effectiveTransaction, null), () ->
         result[0] = new GetAction().executeForRecord(new GetInput(tableName).withPrimaryKey(primaryKey).withTransaction(effectiveTransaction)));

      return (result[0]);
   }

}
