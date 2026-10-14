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


import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectStore;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkOutcome;
import com.kingsrook.qqq.backend.core.jobs.model.JobLease;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** What a running chunk can see and do: its job and chunk rows, its lease, the
 ** fenced checkpoint write, and the cooperative check (pause, cancel, drain,
 ** lost lease) that plans call at safe points - between pages or steps.
 *******************************************************************************/
public class ChunkContext
{
   private static final long STATUS_CACHE_MILLIS = 2000;

   private final QRecord         job;
   private final QRecord         chunk;
   private final JobLease        lease;
   private final DurableJobStore store;
   private final BooleanSupplier draining;
   private final AtomicBoolean   leaseLost = new AtomicBoolean(false);

   private boolean completedByPlan = false;

   private JobStatus cachedJobStatus;
   private long      cachedAt = 0;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public ChunkContext(QRecord job, QRecord chunk, JobLease lease, DurableJobStore store, BooleanSupplier draining)
   {
      this.job = job;
      this.chunk = chunk;
      this.lease = lease;
      this.store = store;
      this.draining = draining;
   }



   /*******************************************************************************
    ** Save a checkpoint (fenced by the lease). A lost lease stops the chunk.
    *******************************************************************************/
   public void checkpoint(String checkpointJson) throws QException
   {
      try
      {
         store.checkpoint(lease, checkpointJson, null);
      }
      catch(QException e)
      {
         leaseLost.set(true);
         throw (new JobInterruptedException(JobInterruptedException.Reason.LEASE_LOST));
      }
   }



   /*******************************************************************************
    ** The cooperative check: throw JobInterruptedException when the chunk should
    ** stop here. Reads the job status at most every 2 seconds.
    *******************************************************************************/
   public void check() throws QException
   {
      if(leaseLost.get())
      {
         throw (new JobInterruptedException(JobInterruptedException.Reason.LEASE_LOST));
      }

      JobStatus status = currentJobStatus();
      if(status == JobStatus.CANCELLING || status == JobStatus.CANCELLED)
      {
         throw (new JobInterruptedException(JobInterruptedException.Reason.CANCEL));
      }
      if(status == JobStatus.PAUSING || status == JobStatus.PAUSED)
      {
         throw (new JobInterruptedException(JobInterruptedException.Reason.PAUSE));
      }
      if(draining != null && draining.getAsBoolean())
      {
         throw (new JobInterruptedException(JobInterruptedException.Reason.DRAIN));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JobStatus currentJobStatus() throws QException
   {
      long now = System.currentTimeMillis();
      if(cachedJobStatus == null || now - cachedAt > STATUS_CACHE_MILLIS)
      {
         QRecord current = store.getJob(lease.jobId());
         cachedJobStatus = current == null ? JobStatus.CANCELLED : JobStatus.valueOf(current.getValueString("status"));
         cachedAt = now;
      }
      return (cachedJobStatus);
   }



   /*******************************************************************************
    ** Mark the chunk DONE inside the plan's own transaction (SAME_TRANSACTION:
    ** the chunk's writes and its DONE mark commit together, fenced by the lease).
    ** Records the row errors in the same transaction first.
    *******************************************************************************/
   public void completeInTransaction(ChunkOutcome outcome, List<QRecord> rowErrors, QBackendTransaction transaction) throws QException
   {
      completeInTransaction(outcome, rowErrors, List.of(), transaction);
   }



   /*******************************************************************************
    ** Mark the chunk DONE in the plan's transaction, with its row errors and its
    ** outbox effects: the chunk's writes, its effects and its DONE mark commit
    ** together (spec section 2.13).
    *******************************************************************************/
   public void completeInTransaction(ChunkOutcome outcome, List<QRecord> rowErrors, List<QRecord> effects, QBackendTransaction transaction) throws QException
   {
      store.recordRowErrors(rowErrors, transaction);
      new EffectStore(store.getSettings()).insertEffects(effects, transaction);
      store.completeChunk(lease, outcome, transaction);
      completedByPlan = true;
   }



   /*******************************************************************************
    ** Write outbox effects outside a transaction (keys already written are
    ** skipped, so a rerun never doubles them).
    *******************************************************************************/
   public void recordEffects(List<QRecord> effects) throws QException
   {
      new EffectStore(store.getSettings()).insertEffects(effects, null);
   }



   /*******************************************************************************
    ** This chunk's identity for effect keys.
    *******************************************************************************/
   public JobChunkScope.ChunkIdentity identity()
   {
      return (new JobChunkScope.ChunkIdentity(job.getValueInteger("id"), chunk.getValueInteger("id"), chunk.getValueInteger("seq"), job.getValueString("processName")));
   }



   /*******************************************************************************
    ** Whether the plan already marked the chunk DONE (in its transaction).
    *******************************************************************************/
   public boolean isCompletedByPlan()
   {
      return (completedByPlan);
   }



   /*******************************************************************************
    ** Mark the lease lost (from the heartbeat thread).
    *******************************************************************************/
   public void markLeaseLost()
   {
      leaseLost.set(true);
   }



   /*******************************************************************************
    ** Whether the heartbeat found the lease gone.
    *******************************************************************************/
   public boolean isLeaseLost()
   {
      return (leaseLost.get());
   }



   /*******************************************************************************
    ** Getter for job
    *******************************************************************************/
   public QRecord getJob()
   {
      return (this.job);
   }



   /*******************************************************************************
    ** Getter for chunk
    *******************************************************************************/
   public QRecord getChunk()
   {
      return (this.chunk);
   }



   /*******************************************************************************
    ** Getter for lease
    *******************************************************************************/
   public JobLease getLease()
   {
      return (this.lease);
   }



   /*******************************************************************************
    ** Getter for store
    *******************************************************************************/
   public DurableJobStore getStore()
   {
      return (this.store);
   }

}
