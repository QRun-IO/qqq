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


package com.kingsrook.qqq.backend.core.jobs.plans;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.AbstractBulkLoadRollableValueError;


/*******************************************************************************
 ** What a durable job chunk hands to the process steps it runs, on the thread
 ** that runs them:
 **
 ** - a provided transaction: StreamedETLExecuteStep loads into it instead of
 **   opening (and committing) its own, so the chunk's rows and its DONE mark
 **   commit together (SAME_TRANSACTION: exactly once);
 ** - a row-error sink: transform and load steps report each failed row (file
 **   row number or record key, field, code, message) so the job can record
 **   them in jobs_row_error.
 **
 ** Outside a job (no scope open) everything behaves as before.
 *******************************************************************************/
public final class JobChunkScope implements AutoCloseable
{
   private static final ThreadLocal<JobChunkScope> CURRENT = new ThreadLocal<>();

   private final QBackendTransaction  transaction;
   private final PageCheck            pageCheck;
   private final ChunkIdentity        identity;
   private final List<RowError>       rowErrors = new ArrayList<>();
   private final Map<String, QRecord> effects   = new LinkedHashMap<>();
   private final JobChunkScope        previous;



   /*******************************************************************************
    ** Which job chunk this scope runs: the parts of an effect's idempotency key,
    ** and the process whose effect declarations apply.
    *******************************************************************************/
   public record ChunkIdentity(Integer jobId, Integer chunkId, Integer chunkSeq, String processName)
   {
   }



   /*******************************************************************************
    ** The chunk's cooperative check (pause, cancel, drain, lost lease), run
    ** between pages; it throws JobInterruptedException to stop the chunk.
    *******************************************************************************/
   @FunctionalInterface
   public interface PageCheck
   {
      /*******************************************************************************
       ** Throw to stop the chunk here.
       *******************************************************************************/
      void check() throws QException;
   }



   /*******************************************************************************
    ** One failed row.
    **
    ** @param rowNumber  the file row number (file sources), or null
    ** @param recordPk   the record's primary key (table sources), or null
    ** @param fieldName  the field the error is about, or null
    ** @param errorCode  a short machine code (e.g., the error class), or null
    ** @param message    the user-facing message
    *******************************************************************************/
   public record RowError(Integer rowNumber, Serializable recordPk, String fieldName, String errorCode, String message)
   {
   }



   /*******************************************************************************
    ** Open a scope on this thread (close it when the chunk ends).
    *******************************************************************************/
   public static JobChunkScope open(QBackendTransaction transaction)
   {
      return (open(transaction, null));
   }



   /*******************************************************************************
    ** Open a scope on this thread with a cooperative check that page-wise steps
    ** (such as the streamed ETL execute step) run between pages.
    *******************************************************************************/
   public static JobChunkScope open(QBackendTransaction transaction, PageCheck pageCheck)
   {
      return (open(transaction, pageCheck, null));
   }



   /*******************************************************************************
    ** Open a scope for an identified job chunk: side effects emitted in it
    ** (JobEffects.emit) are buffered here and written to the outbox with the
    ** chunk's progress.
    *******************************************************************************/
   public static JobChunkScope open(QBackendTransaction transaction, PageCheck pageCheck, ChunkIdentity identity)
   {
      JobChunkScope scope = new JobChunkScope(transaction, pageCheck, identity, CURRENT.get());
      CURRENT.set(scope);
      return (scope);
   }



   /*******************************************************************************
    ** Between pages: run the chunk's cooperative check, if a job chunk with one
    ** is running on this thread (no-op otherwise). Pause and cancel then stop a
    ** chunk at the next page instead of at its end.
    *******************************************************************************/
   public static void checkBetweenPages() throws QException
   {
      JobChunkScope scope = CURRENT.get();
      if(scope != null && scope.pageCheck != null)
      {
         scope.pageCheck.check();
      }
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   private JobChunkScope(QBackendTransaction transaction, PageCheck pageCheck, ChunkIdentity identity, JobChunkScope previous)
   {
      this.transaction = transaction;
      this.pageCheck = pageCheck;
      this.identity = identity;
      this.previous = previous;
   }



   /*******************************************************************************
    ** The scope open on this thread, if any.
    *******************************************************************************/
   public static Optional<JobChunkScope> current()
   {
      return (Optional.ofNullable(CURRENT.get()));
   }



   /*******************************************************************************
    ** The transaction a durable chunk provides on this thread, if any.
    *******************************************************************************/
   public static Optional<QBackendTransaction> providedTransaction()
   {
      JobChunkScope scope = CURRENT.get();
      return (scope == null ? Optional.empty() : Optional.ofNullable(scope.transaction));
   }



   /*******************************************************************************
    ** Report a failed row (no-op outside a job).
    *******************************************************************************/
   public static void reportRowError(RowError rowError)
   {
      JobChunkScope scope = CURRENT.get();
      if(scope != null && rowError != null)
      {
         synchronized(scope.rowErrors)
         {
            scope.rowErrors.add(rowError);
         }
      }
   }



   /*******************************************************************************
    ** Report each error on a record that failed (no-op outside a job). Rows of a
    ** bulk-load file are reported by file row number.
    *******************************************************************************/
   public static void reportRecordErrors(QRecord record, List<Integer> fileRowNumbers, Serializable recordPk)
   {
      if(CURRENT.get() == null || record == null || record.getErrors() == null || record.getErrors().isEmpty())
      {
         return;
      }
      var    error     = record.getErrors().get(0);
      String message   = error instanceof AbstractBulkLoadRollableValueError rollable ? rollable.getMessageToUseAsProcessSummaryRollupKey() : error.getMessage();
      String errorCode = error.getClass().getSimpleName();
      String fieldName = null;
      if(fileRowNumbers != null && !fileRowNumbers.isEmpty())
      {
         for(Integer rowNumber : fileRowNumbers)
         {
            reportRowError(new RowError(rowNumber, recordPk, fieldName, errorCode, message));
         }
      }
      else
      {
         reportRowError(new RowError(null, recordPk, fieldName, errorCode, message));
      }
   }



   /*******************************************************************************
    ** The rows reported so far.
    *******************************************************************************/
   public List<RowError> getRowErrors()
   {
      synchronized(rowErrors)
      {
         return (new ArrayList<>(rowErrors));
      }
   }



   /*******************************************************************************
    ** Restore whatever scope was open before.
    *******************************************************************************/
   @Override
   public void close()
   {
      if(previous == null)
      {
         CURRENT.remove();
      }
      else
      {
         CURRENT.set(previous);
      }
   }



   /*******************************************************************************
    ** Getter for identity (null for a scope that is not a job chunk).
    *******************************************************************************/
   public ChunkIdentity getIdentity()
   {
      return (this.identity);
   }



   /*******************************************************************************
    ** Buffer an outbox effect row; the same idempotency key twice keeps one.
    *******************************************************************************/
   public void addEffect(QRecord effect)
   {
      synchronized(effects)
      {
         effects.putIfAbsent(effect.getValueString("idempotencyKey"), effect);
      }
   }



   /*******************************************************************************
    ** Take the buffered effects (the buffer is emptied: a STEPS job writes them
    ** at each step boundary).
    *******************************************************************************/
   public List<QRecord> drainEffects()
   {
      synchronized(effects)
      {
         List<QRecord> list = new ArrayList<>(effects.values());
         effects.clear();
         return (list);
      }
   }
}
