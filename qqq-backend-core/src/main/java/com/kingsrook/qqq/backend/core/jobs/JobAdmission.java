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


package com.kingsrook.qqq.backend.core.jobs;


import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QTooManyRequestsException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Shared atomic capacity admission for opt-in insertions and state transitions.
 ** Owns only transactions opened here; joined source transactions remain caller-owned.
 *******************************************************************************/
public final class JobAdmission
{
   private static final Set<String> WAITING = Set.of("QUEUED", "AWAITING_CONFIRMATION");
   private static final Set<String> EXECUTING = Set.of("RUNNING", "PLANNING", "PAUSING", "CANCELLING");
   private static final Set<String> ACTIVE = Set.of("AWAITING_CONFIRMATION", "QUEUED", "PLANNING", "RUNNING", "PAUSING", "PAUSED");



   /*******************************************************************************
    ** Native owner insertion supplied by DurableJobStore, not application code.
    *******************************************************************************/
   @FunctionalInterface
   public interface Operation<T>
   {
      /*******************************************************************************
       ** Insert the one reserved job using the exact admitted transaction.
       *******************************************************************************/
      T run(QBackendTransaction transaction) throws QException;
   }



   /*******************************************************************************
    ** Static Jobs-owned admission only.
    *******************************************************************************/
   private JobAdmission() {}



   /*******************************************************************************
    ** Reserve capacity and insert atomically, without any planner under the lock.
    ** Enlisted calls never commit or close their supplied source transaction.
    *******************************************************************************/
   public static Integer insert(DurableJobsMetaData settings, QRecord job, QBackendTransaction supplied, Operation<Integer> insertion) throws QException
   {
      return (transaction(settings, supplied, transaction ->
      {
         requireCapacity(settings, transaction, null, job);
         return (insertion.run(transaction));
      }));
   }



   /*******************************************************************************
    ** Serialize an entire state-changing operation before its chunk/effect writes.
    ** An exception poisons joined work even if its caller catches the exception.
    *******************************************************************************/
   public static <T> T transaction(DurableJobsMetaData settings, QBackendTransaction supplied, Operation<T> operation) throws QException
   {
      QBackendTransaction transaction = supplied;
      boolean owned = transaction == null;
      try
      {
         if(settings != DurableJobsMetaData.of(QContext.getQInstance()) || !settings.isTransactionalAdmissionEnabled())
         {
            throw (new QException("Transactional job admission is not enabled in this instance"));
         }
         if(transaction == null)
         {
            transaction = QBackendTransaction.openFor(new InsertInput(DurableJobsTables.TABLE_JOB));
         }
         lock(transaction, DurableJobsTables.TABLE_JOB);
         T result = operation.run(transaction);
         if(owned)
         {
            transaction.commit();
         }
         return (result);
      }
      catch(QException | RuntimeException | LinkageError error)
      {
         if(transaction != null)
         {
            transaction.markRollbackOnly();
            if(owned)
            {
               try
               {
                  transaction.rollback();
               }
               catch(QException rollbackError)
               {
                  error.addSuppressed(rollbackError);
               }
            }
         }
         throw (error);
      }
      finally
      {
         if(owned && transaction != null)
         {
            transaction.close();
         }
      }
   }



   /*******************************************************************************
    ** Admit only a positive change in counted membership. Same-owner transitions
    ** within an existing budget do not consume a second reservation.
    *******************************************************************************/
   public static void requireCapacity(DurableJobsMetaData settings, QBackendTransaction transaction, QRecord before, QRecord after) throws QException
   {
      String channel = after.getValueString("clientChannel");
      if(channel != null && !channel.matches("[a-z][a-z0-9_-]*([/:].*)?"))
      {
         throw (new QException("Strict Jobs requires a canonical lowercase channel name"));
      }
      String owner = after.getValueString("userId");
      String status = after.getValueString("status");
      if(owner == null || owner.isBlank() || status == null)
      {
         throw (new QException("Atomic job admission requires an owner and status"));
      }
      String process = after.getValueString("processName");
      if(process == null || process.isBlank())
      {
         throw (new QException("Atomic job admission requires a process identity"));
      }
      boolean sameOwner = before != null && Objects.equals(owner, before.getValueString("userId"));
      boolean previouslyExecuting = before != null && EXECUTING.contains(before.getValueString("status"));
      if(before != null && !EXECUTING.contains(status))
      {
         QQueryFilter running = new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, before.getValueInteger("id")),
            new QFilterCriteria("status", QCriteriaOperator.EQUALS, "RUNNING"));
         Integer count = SystemScope.call(() -> new CountAction().execute(new CountInput(DurableJobsTables.TABLE_CHUNK).withFilter(running).withTransaction(transaction)).getCount());
         if(count == null || count != 0)
         {
            throw (new QException("A job with running chunks must retain its execution reservation"));
         }
      }
      if(EXECUTING.contains(status))
      {
         if(!(sameOwner && previouslyExecuting)
            && countBy(transaction, "userId", owner, EXECUTING) >= settings.getMaxActiveJobsPerUser())
         {
            throw (new QTooManyRequestsException("Your active-job capacity is currently full"));
         }
         DurableProcessMetaData processSettings = DurableProcessMetaData.of(QContext.getQInstance().getProcess(process));
         int processCap = processSettings != null && processSettings.getMaxConcurrentJobs() != null
            ? processSettings.getMaxConcurrentJobs() : settings.getDefaultMaxConcurrentJobsPerProcess();
         if(!(previouslyExecuting && Objects.equals(process, before.getValueString("processName")))
            && countBy(transaction, "processName", process, EXECUTING) >= processCap)
         {
            throw (new QTooManyRequestsException("This process's active-job capacity is currently full"));
         }
      }
      boolean alreadyWaiting = sameOwner && WAITING.contains(before.getValueString("status"));
      boolean alreadyAssistant = sameOwner && assistant(before) && ACTIVE.contains(before.getValueString("status"));
      if(WAITING.contains(status) && !alreadyWaiting && count(transaction, owner, WAITING, false) >= settings.getMaxQueuedJobsPerUser())
      {
         throw (new QTooManyRequestsException("Your queued-job capacity is currently full"));
      }
      if(assistant(after) && ACTIVE.contains(status) && !alreadyAssistant
         && count(transaction, owner, ACTIVE, true) >= settings.getAssistantMaxActiveJobs())
      {
         throw (new QTooManyRequestsException("Your assistant job capacity is currently full"));
      }
   }



   /*******************************************************************************
    ** Use the original trusted channel stored with the job for budget membership.
    *******************************************************************************/
   private static boolean assistant(QRecord job)
   {
      String channel = job.getValueString("clientChannel");
      return ("assistant".equals(channel) || channel != null && (channel.startsWith("assistant/") || channel.startsWith("assistant:")));
   }



   /*******************************************************************************
    ** A backend must supply the actual durable transaction qualification and lock.
    *******************************************************************************/
   public static String lock(QBackendTransaction transaction, String sourceTable) throws QException
   {
      if(transaction == null || transaction.isRollbackOnly() || !(transaction instanceof TransactionalJobAdmission admission))
      {
         throw (new QException("The supplied transaction cannot provide durable Jobs admission"));
      }
      return (admission.lockJobAdmission(QContext.getQInstance().getTable(sourceTable)));
   }



   /*******************************************************************************
    ** Execution-active budgets include inline rows in explicitly enabled mode.
    *******************************************************************************/
   private static int countBy(QBackendTransaction transaction, String field, String value, Set<String> statuses) throws QException
   {
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.EQUALS, value),
         new QFilterCriteria("status", QCriteriaOperator.IN, statuses));
      Integer count = SystemScope.call(() -> new CountAction().execute(new CountInput(DurableJobsTables.TABLE_JOB).withFilter(filter).withTransaction(transaction)).getCount());
      if(count == null)
      {
         throw (new QException("Could not determine current execution capacity"));
      }
      return (count);
   }



   /*******************************************************************************
    ** Count current committed work plus this transaction's earlier reservations.
    ** The qualified backend requires READ COMMITTED under the admission row lock.
    *******************************************************************************/
   private static int count(QBackendTransaction transaction, String userId, Set<String> statuses, boolean assistant) throws QException
   {
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("userId", QCriteriaOperator.EQUALS, userId),
         new QFilterCriteria("status", QCriteriaOperator.IN, statuses));
      if(assistant)
      {
         filter.withSubFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withCriteria(new QFilterCriteria("clientChannel", QCriteriaOperator.EQUALS, "assistant"))
            .withCriteria(new QFilterCriteria("clientChannel", QCriteriaOperator.STARTS_WITH, "assistant/"))
            .withCriteria(new QFilterCriteria("clientChannel", QCriteriaOperator.STARTS_WITH, "assistant:")));
      }
      Integer count = SystemScope.call(() -> new CountAction().execute(new CountInput(DurableJobsTables.TABLE_JOB).withFilter(filter).withTransaction(transaction)).getCount());
      if(count == null)
      {
         throw (new QException("Atomic job admission could not determine current launch capacity"));
      }
      return (count);
   }
}
