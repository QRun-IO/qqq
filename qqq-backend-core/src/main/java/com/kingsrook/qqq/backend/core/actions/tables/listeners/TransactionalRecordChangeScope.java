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

package com.kingsrook.qqq.backend.core.actions.tables.listeners;


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.BoundedIntentData;


/*******************************************************************************
 ** Full native action scope for explicitly required transactional listeners.
 ** Owns only transactions it opens. Nested actions share the same connection;
 ** failures poison joined transactions even when a caller catches the exception.
 *******************************************************************************/
public final class TransactionalRecordChangeScope implements AutoCloseable
{
   private static final ThreadLocal<TransactionalRecordChangeScope> CURRENT = new ThreadLocal<>();
   private final AbstractTableActionInput input;
   private final RecordChangeType type;
   private final String tableName;
   private final boolean preview;
   private final QBackendTransaction priorInput;
   private final QBackendTransaction priorAmbient;
   private final TransactionalRecordChangeScope parent;
   private final QInstance instance;
   private final QSession session;
   private QBackendTransaction transaction;
   private RecordChangeListenerInterface listener;
   private RecordChangeEvent firingEvent;
   private String eventId;
   private String eventFingerprint;
   private boolean owned;
   private boolean active;
   private boolean previewOnly;
   private boolean successful;
   private int depth;



   /*******************************************************************************
    ** Capture exact input and ambient references before admitting any mutation.
    *******************************************************************************/
   private TransactionalRecordChangeScope(AbstractTableActionInput input, RecordChangeType type, boolean preview)
   {
      this.input = input;
      this.type = type;
      this.tableName = input.getTableName();
      this.preview = preview;
      priorInput = transaction(input);
      priorAmbient = QContext.getQBackendTransaction();
      parent = CURRENT.get();
      instance = QContext.getQInstance();
      session = QContext.getQSession();
   }



   /*******************************************************************************
    ** Load required code before DML. Ordinary actions and previews remain unchanged
    ** unless nested inside an already strict action, which cannot escape its tx.
    *******************************************************************************/
   public static TransactionalRecordChangeScope open(AbstractTableActionInput input, RecordChangeType type, boolean dryRun) throws QException
   {
      TransactionalRecordChangeScope scope = new TransactionalRecordChangeScope(input, type, dryRun);
      try
      {
         if(scope.instance == null || scope.session == null)
         {
            com.kingsrook.qqq.backend.core.actions.ActionHelper.validateSession(input);
         }
         TransactionalRecordChangeMetaData metadata = TransactionalRecordChangeMetaData.of(scope.instance.getTable(input.getTableName()));
         boolean required = metadata != null && metadata.requires(type);
         if(scope.parent == null && !required)
         {
            return (scope);
         }
         scope.active = true;
         if(scope.parent == null && dryRun)
         {
            scope.previewOnly = true;
            if(scope.priorInput != null && scope.priorAmbient != null && scope.priorInput != scope.priorAmbient)
            {
               throw (new QException("Required record preview has conflicting transaction references"));
            }
            scope.transaction = scope.priorInput != null ? scope.priorInput : scope.priorAmbient;
            setTransaction(input, scope.transaction);
            QContext.setTransaction(scope.transaction);
            CURRENT.set(scope);
            return (scope);
         }
         if(scope.parent != null && scope.parent.preview && !dryRun)
         {
            throw (new QException("A required record preview cannot perform nested writes"));
         }
         scope.depth = scope.parent == null ? 1 : scope.parent.depth + 1;
         if(scope.depth > 32 || scope.priorInput != null && scope.priorAmbient != null && scope.priorInput != scope.priorAmbient)
         {
            throw (new QException("Required record transaction scope is inconsistent or too deeply nested"));
         }
         scope.transaction = scope.priorInput != null ? scope.priorInput : scope.priorAmbient;
         if(scope.parent != null && scope.transaction != scope.parent.transaction)
         {
            throw (new QException("Nested required record writes must use the parent transaction"));
         }
         if(scope.transaction == null)
         {
            scope.transaction = QBackendTransaction.openFor(input);
            scope.owned = true;
         }
         if(scope.transaction.isRollbackOnly())
         {
            throw (new QException("Required record transaction is rollback-only"));
         }
         scope.transaction.requireAtomicWrites(input.getTable());
         if(required && !dryRun)
         {
            scope.listener = QCodeLoader.getAdHoc(RecordChangeListenerInterface.class, metadata.getListener());
            if(scope.listener == null || !scope.listener.appliesTo(input.getTableName(), type))
            {
               throw (new QException("Required record listener is unavailable or does not apply"));
            }
         }
         setTransaction(input, scope.transaction);
         QContext.setTransaction(scope.transaction);
         CURRENT.set(scope);
         return (scope);
      }
      catch(QException | RuntimeException | LinkageError error)
      {
         if(!scope.previewOnly)
         {
            scope.poison();
         }
         if(scope.owned)
         {
            try
            {
               scope.transaction.rollback();
            }
            catch(QException | RuntimeException rollbackError)
            {
               error.addSuppressed(rollbackError);
            }
            scope.transaction.close();
         }
         throw (error instanceof QException exception ? exception : new QException("Required record listener admission failed", error));
      }
   }



   /*******************************************************************************
    ** Allow event preparation even when there are no optional instance listeners.
    *******************************************************************************/
   static boolean applies(String tableName, RecordChangeType type)
   {
      TransactionalRecordChangeScope scope = CURRENT.get();
      return (scope != null && scope.listener != null && scope.tableName.equals(tableName) && scope.type == type);
   }



   /*******************************************************************************
    ** Recheck the effective personalized target before native writes. A required
    ** obligation comes from authoritative metadata and cannot be removed by an
    ** input clone or a presentation personalizer.
    *******************************************************************************/
   public static void verifyTarget(AbstractTableActionInput input) throws QException
   {
      TransactionalRecordChangeScope scope = CURRENT.get();
      if(scope != null && scope.active)
      {
         if(scope.input != input)
         {
            scope.poison();
            throw (new QException("Required record action scope changed"));
         }
         scope.checkIdentity();
         if(!scope.previewOnly)
         {
            scope.transaction.requireAtomicWrites(input.getTable());
         }
      }
   }



   /*******************************************************************************
    ** Validation must not turn an admitted preview into a native mutation.
    *******************************************************************************/
   public static void beforeMutation(AbstractTableActionInput input) throws QException
   {
      verifyTarget(input);
      TransactionalRecordChangeScope scope = CURRENT.get();
      if(scope != null && scope.preview)
      {
         scope.poison();
         throw (new QException("A required record preview cannot become a mutation"));
      }
   }



   /*******************************************************************************
    ** Permission and input failures can precede open(). Preserve their original
    ** error, but prevent callers from committing earlier work after catching it.
    ** Ordinary inputs and standalone previews retain existing behavior.
    *******************************************************************************/
   public static void failed(AbstractTableActionInput input, RecordChangeType type, boolean preview)
   {
      TransactionalRecordChangeScope parent = CURRENT.get();
      QInstance currentInstance = QContext.getQInstance();
      QTableMetaData table = currentInstance == null || input == null ? null : currentInstance.getTable(input.getTableName());
      boolean required = table != null && table.getSupplementalMetaData(TransactionalRecordChangeMetaData.TYPE) != null;
      if(required && table.getSupplementalMetaData(TransactionalRecordChangeMetaData.TYPE) instanceof TransactionalRecordChangeMetaData metadata)
      {
         required = metadata.requires(type);
      }
      if(parent != null || !preview && required)
      {
         QBackendTransaction explicit = transaction(input);
         QBackendTransaction ambient = QContext.getQBackendTransaction();
         if(explicit != null)
         {
            explicit.markRollbackOnly();
         }
         if(ambient != null)
         {
            ambient.markRollbackOnly();
         }
         if(parent != null)
         {
            parent.poison();
         }
      }
   }



   /*******************************************************************************
    ** Invoke the preloaded required listener before optional listeners can mutate
    ** the shared event. Suppression flags never disable this obligation.
    *******************************************************************************/
   static void fire(RecordChangeEvent event)
   {
      TransactionalRecordChangeScope scope = CURRENT.get();
      if(scope == null || !applies(event.getTableName(), event.getType()))
      {
         return;
      }
      try
      {
         scope.checkIdentity();
         if(event.getTransaction() != scope.transaction)
         {
            throw (new QException("Required record event transaction changed"));
         }
         scope.firingEvent = event;
         scope.eventId = java.util.UUID.randomUUID().toString();
         scope.eventFingerprint = fingerprint(event);
         scope.listener.onRecordsChanged(event);
         scope.checkIdentity();
         if(!scope.eventFingerprint.equals(fingerprint(event)))
         {
            throw (new QException("Required record listeners must not mutate their native event"));
         }
      }
      catch(QException | RuntimeException | LinkageError error)
      {
         scope.poison();
         throw (new RecordChangeListenerHelper.RequiredListenerException(error));
      }
      finally
      {
         scope.firingEvent = null;
         scope.eventId = null;
         scope.eventFingerprint = null;
      }
   }



   /*******************************************************************************
    ** Only the exact currently dispatched event can enlist work on its transaction.
    ** A fabricated event with matching fields or a delayed callback is rejected.
    *******************************************************************************/
   public static AdmittedEvent admittedEvent(RecordChangeEvent event) throws QException
   {
      TransactionalRecordChangeScope scope = CURRENT.get();
      if(scope == null || scope.firingEvent != event || event == null)
      {
         if(scope != null)
         {
            scope.poison();
         }
         throw (new QException("Transactional enlistment requires the exact active required record event"));
      }
      try
      {
         scope.checkIdentity();
         if(event.getTransaction() != scope.transaction || !scope.eventFingerprint.equals(fingerprint(event)))
         {
            scope.poison();
            throw (new QException("Required record event changed before enlistment"));
         }
         return (new AdmittedEvent(scope.transaction, scope.tableName, scope.type, scope.eventFingerprint, scope.eventId));
      }
      catch(QException | RuntimeException | LinkageError error)
      {
         scope.poison();
         throw (error);
      }
   }



   /*******************************************************************************
    ** Immutable event identity, not a separately accepted authorization token.
    *******************************************************************************/
   public record AdmittedEvent(QBackendTransaction transaction, String tableName, RecordChangeType type, String fingerprint, String eventId) {}



   /*******************************************************************************
    ** Bound and hash native record values without putting them in Jobs parameters.
    ** Binary values contribute only their length and digest to the canonical hash.
    *******************************************************************************/
   private static String fingerprint(RecordChangeEvent event) throws QException
   {
      if(event.getRecords() == null || event.getRecords().size() > 1000 || event.getOldRecords() != null && event.getOldRecords().size() > 1000)
      {
         throw (new QException("Required record event exceeds its record budget"));
      }
      Map<String, Object> snapshot = new LinkedHashMap<>();
      snapshot.put("table", event.getTableName());
      snapshot.put("type", event.getType().name());
      snapshot.put("records", event.getRecords().stream().map(record -> record == null ? null : record.getValues()).toList());
      snapshot.put("prior", event.getOldRecords() == null ? null : event.getOldRecords().stream().map(record -> record == null ? null : record.getValues()).toList());
      snapshot.put("notificationsSuppressed", event.getNotificationsSuppressed());
      return (BoundedIntentData.fingerprint(snapshot));
   }



   /*******************************************************************************
    ** Mark the complete native action successful, after its nested writes and
    ** presentation. A joined scope still never commits its caller's transaction.
    *******************************************************************************/
   public void success() throws QException
   {
      if(active)
      {
         checkIdentity();
      }
      successful = true;
   }



   /*******************************************************************************
    ** Refuse replacing the admitted principal, instance, input or ambient tx.
    *******************************************************************************/
   private void checkIdentity() throws QException
   {
      if(QContext.getQInstance() != instance || QContext.getQSession() != session
         || !tableName.equals(input.getTableName()) || QContext.getQBackendTransaction() != transaction
         || transaction(input) != transaction || transaction != null && transaction.isRollbackOnly())
      {
         poison();
         throw (new QException("Required record transaction or initiating context changed"));
      }
   }



   /*******************************************************************************
    ** Every participating reference stays poisoned after a failed admission.
    *******************************************************************************/
   private void poison()
   {
      if(transaction != null)
      {
         transaction.markRollbackOnly();
      }
      if(priorInput != null)
      {
         priorInput.markRollbackOnly();
      }
      if(priorAmbient != null)
      {
         priorAmbient.markRollbackOnly();
      }
      if(parent != null && parent.transaction != null)
      {
         parent.transaction.markRollbackOnly();
      }
   }



   /*******************************************************************************
    ** Finish only an owned transaction, restoring exact caller references even
    ** when rollback, commit, or a required listener fails.
    *******************************************************************************/
   @Override
   public void close() throws QException
   {
      if(!active)
      {
         return;
      }
      try
      {
         if(!successful && !previewOnly)
         {
            poison();
         }
         if(owned)
         {
            // Commit invokes existing after-commit callbacks. Their native writes
            // must start fresh scopes, never rejoin this completed transaction.
            restore();
            if(successful && !preview && !transaction.isRollbackOnly())
            {
               try
               {
                  transaction.commit();
               }
               catch(QException | RuntimeException | LinkageError error)
               {
                  ////////////////////////////////////////////////////////////////////////
                  // any failed commit, including an unchecked one from a transaction   //
                  // that does not wrap its own, is a failure: poison and roll back,    //
                  // then rethrow as-is (the commit outcome is not guessed or retried)  //
                  ////////////////////////////////////////////////////////////////////////
                  poison();
                  try
                  {
                     transaction.rollback();
                  }
                  catch(QException | RuntimeException rollbackError)
                  {
                     error.addSuppressed(rollbackError);
                  }
                  throw (error);
               }
            }
            else
            {
               transaction.rollback();
            }
         }
      }
      finally
      {
         restore();
         if(owned)
         {
            transaction.close();
         }
      }
   }



   /*******************************************************************************
    ** Detach the native scope before callbacks and on every failure path.
    *******************************************************************************/
   private void restore()
   {
      setTransaction(input, priorInput);
      QContext.setTransaction(priorAmbient);
      if(parent == null)
      {
         CURRENT.remove();
      }
      else
      {
         CURRENT.set(parent);
      }
   }



   /*******************************************************************************
    ** Native mutation inputs carry explicit transaction references.
    *******************************************************************************/
   private static QBackendTransaction transaction(AbstractTableActionInput input)
   {
      if(input instanceof InsertInput insert)
      {
         return (insert.getTransaction());
      }
      if(input instanceof UpdateInput update)
      {
         return (update.getTransaction());
      }
      if(input instanceof DeleteInput delete)
      {
         return (delete.getTransaction());
      }
      throw (new IllegalArgumentException("Required record scope only accepts native mutation inputs"));
   }



   /*******************************************************************************
    ** Restore or install a transaction without changing other caller input state.
    *******************************************************************************/
   private static void setTransaction(AbstractTableActionInput input, QBackendTransaction transaction)
   {
      if(input instanceof InsertInput insert)
      {
         insert.setTransaction(transaction);
      }
      else if(input instanceof UpdateInput update)
      {
         update.setTransaction(transaction);
      }
      else if(input instanceof DeleteInput delete)
      {
         delete.setTransaction(transaction);
      }
      else
      {
         throw (new IllegalArgumentException("Required record scope only accepts native mutation inputs"));
      }
   }
}
