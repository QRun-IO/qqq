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


package com.kingsrook.qqq.backend.core.jobs.store;


import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.ConditionalUpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkOutcome;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.jobs.model.JobLease;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_CHUNK;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_EVENT;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_JOB;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_ROW_ERROR;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** The durable job store, over the jobs_* tables, on any backend with the
 ** CONDITIONAL_UPDATE capability.
 **
 ** Every state change that two pods could race on is a compare-and-set through
 ** ConditionalUpdateAction:
 ** - claim: chunk PENDING/RETRY -> RUNNING, guarded by leaseVersion;
 ** - heartbeat, checkpoint, complete, fail, release: guarded by the lease token
 **   and status RUNNING (fencing - a reclaimed lease's writes change 0 rows);
 ** - reap: RUNNING with an expired lease -> RETRY/FAILED/NEEDS_REVIEW, guarded
 **   by leaseVersion;
 ** - job status: guarded by the expected current statuses.
 **
 ** Lease times come from the database clock. All methods run as the system user
 ** (the kernel's own bookkeeping), never as the job's user.
 *******************************************************************************/
public class DurableJobStore
{
   private static final QLogger LOG = QLogger.getLogger(DurableJobStore.class);

   private final DurableJobsMetaData settings;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public DurableJobStore(DurableJobsMetaData settings)
   {
      this.settings = settings;
   }



   /*******************************************************************************
    ** The database clock.
    *******************************************************************************/
   public Instant now() throws QException
   {
      return (new ConditionalUpdateAction().databaseNow(TABLE_CHUNK));
   }



   /*******************************************************************************
    ** Insert a job row; return its id.
    *******************************************************************************/
   public Integer insertJob(QRecord job, QBackendTransaction transaction) throws QException
   {
      return (SystemScope.call(() ->
      {
         InsertOutput output = new InsertAction().execute(new InsertInput(TABLE_JOB).withRecord(job).withTransaction(transaction));
         QRecord      stored = output.getRecords().get(0);
         if(stored.getErrors() != null && !stored.getErrors().isEmpty())
         {
            throw (new QException("Could not store the job: " + stored.getErrors()));
         }
         return (stored.getValueInteger("id"));
      }));
   }



   /*******************************************************************************
    ** Insert planned chunks for a job.
    *******************************************************************************/
   public void insertChunks(List<QRecord> chunks, QBackendTransaction transaction) throws QException
   {
      if(chunks.isEmpty())
      {
         return;
      }
      SystemScope.call(() ->
      {
         for(int i = 0; i < chunks.size(); i += 500)
         {
            new InsertAction().execute(new InsertInput(TABLE_CHUNK).withRecords(new ArrayList<>(chunks.subList(i, Math.min(chunks.size(), i + 500)))).withTransaction(transaction));
         }
         return (null);
      });
   }



   /*******************************************************************************
    ** Read a job (as the system user).
    *******************************************************************************/
   public QRecord getJob(Integer jobId) throws QException
   {
      return (SystemScope.call(() -> GetAction.execute(TABLE_JOB, jobId)));
   }



   /*******************************************************************************
    ** Read a chunk (as the system user).
    *******************************************************************************/
   public QRecord getChunk(Integer chunkId) throws QException
   {
      return (SystemScope.call(() -> GetAction.execute(TABLE_CHUNK, chunkId)));
   }



   /*******************************************************************************
    ** Query a jobs table (as the system user).
    *******************************************************************************/
   public List<QRecord> query(String tableName, QQueryFilter filter) throws QException
   {
      return (SystemScope.call(() -> new QueryAction().execute(new QueryInput(tableName).withFilter(filter)).getRecords()));
   }



   /*******************************************************************************
    ** The chunks of a job, in sequence order.
    *******************************************************************************/
   public List<QRecord> getChunks(Integer jobId) throws QException
   {
      return (query(TABLE_CHUNK, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)).withOrderBy(new QFilterOrderBy("seq"))));
   }



   /*******************************************************************************
    ** Compare-and-set a job's status: from one of the expected statuses to a new
    ** one, setting any extra values at the same time. True if this caller won.
    *******************************************************************************/
   public boolean setJobStatus(Integer jobId, Collection<JobStatus> from, JobStatus to, Map<String, Serializable> extraValues) throws QException
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      if(extraValues != null)
      {
         values.putAll(extraValues);
      }
      values.put("status", to.name());
      values.put("modifyDate", now());

      return (cas(TABLE_JOB, values, new QQueryFilter()
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, jobId))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, from.stream().map(Enum::name).toList())), null) == 1);
   }



   /*******************************************************************************
    ** Set values on a job without a status condition (counters, reasons).
    *******************************************************************************/
   public void updateJob(Integer jobId, Map<String, Serializable> values) throws QException
   {
      cas(TABLE_JOB, values, new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, jobId)), null);
   }



   /*******************************************************************************
    ** Try to claim one specific claimable chunk. Returns the lease if this pod won.
    *******************************************************************************/
   public Optional<JobLease> claimChunk(QRecord chunk, String podId, int leaseTtlSeconds) throws QException
   {
      return (claimChunk(chunk, podId, leaseTtlSeconds, null));
   }



   /*******************************************************************************
    ** Whether chunk claims use SELECT ... FOR UPDATE SKIP LOCKED (setting on, and
    ** the jobs backend supports it).
    *******************************************************************************/
   public boolean usesSkipLockedClaims()
   {
      return (Boolean.TRUE.equals(settings.getUseSkipLockedClaims()) && SkipLockedClaims.isSupported(TABLE_CHUNK));
   }



   /*******************************************************************************
    ** Claim the job's next claimable chunk (lowest seq) that no other claimer has
    ** locked: FOR UPDATE SKIP LOCKED plus the fenced conditional update, in one
    ** transaction.
    *******************************************************************************/
   public Optional<JobLease> claimNextChunkSkipLocked(Integer jobId, String podId, int leaseTtlSeconds) throws QException
   {
      QQueryFilter claimable = new QQueryFilter()
         .withCriteria(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, List.of(ChunkStatus.PENDING.name(), ChunkStatus.RETRY.name())))
         .withOrderBy(new QFilterOrderBy("seq"))
         .withLimit(1);
      return (SkipLockedClaims.claim(TABLE_CHUNK, claimable, (locked, transaction) ->
         locked.isEmpty() ? Optional.empty() : claimChunk(locked.get(0), podId, leaseTtlSeconds, transaction)));
   }



   /*******************************************************************************
    ** Claim one specific chunk, optionally in a (claiming) transaction.
    *******************************************************************************/
   public Optional<JobLease> claimChunk(QRecord chunk, String podId, int leaseTtlSeconds, QBackendTransaction transaction) throws QException
   {
      Instant now       = now();
      Instant expiresAt = now.plusSeconds(leaseTtlSeconds);
      String  token     = UUID.randomUUID().toString();
      int     attempt   = ValueUtils.getValueAsInteger(Objects.requireNonNullElse(chunk.getValue("attempt"), 0)) + 1;
      int     version   = ValueUtils.getValueAsInteger(Objects.requireNonNullElse(chunk.getValue("leaseVersion"), 0));

      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("status", ChunkStatus.RUNNING.name());
      values.put("leaseOwner", podId);
      values.put("leaseToken", token);
      values.put("leaseExpiresAt", expiresAt);
      values.put("heartbeatAt", now);
      values.put("attempt", attempt);
      values.put("leaseVersion", version + 1);
      values.put("startedAt", now);
      values.put("errorMessage", null);
      values.put("modifyDate", now);

      int count = cas(TABLE_CHUNK, values, new QQueryFilter()
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, chunk.getValueInteger("id")))
         .withCriteria(new QFilterCriteria("leaseVersion", QCriteriaOperator.EQUALS, version))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, List.of(ChunkStatus.PENDING.name(), ChunkStatus.RETRY.name()))), transaction);

      if(count != 1)
      {
         return (Optional.empty());
      }
      return (Optional.of(new JobLease(chunk.getValueInteger("id"), chunk.getValueInteger("jobId"), chunk.getValueInteger("seq"), attempt, podId, token, expiresAt)));
   }



   /*******************************************************************************
    ** Renew a lease. False when the lease is no longer held.
    *******************************************************************************/
   public boolean heartbeat(JobLease lease, int leaseTtlSeconds) throws QException
   {
      Instant now = now();
      return (cas(TABLE_CHUNK, Map.of("leaseExpiresAt", now.plusSeconds(leaseTtlSeconds), "heartbeatAt", now), fence(lease), null) == 1);
   }



   /*******************************************************************************
    ** Save a mid-chunk checkpoint (e.g., process values after a step). Fenced.
    *******************************************************************************/
   public void checkpoint(JobLease lease, String checkpointJson, QBackendTransaction transaction) throws QException
   {
      Map<String, Serializable> values = new HashMap<>();
      values.put("checkpointJson", checkpointJson);
      values.put("heartbeatAt", now());
      if(cas(TABLE_CHUNK, values, fence(lease), transaction) != 1)
      {
         throw (new LeaseLostException("Lost the lease on chunk " + lease.chunkId() + " before its checkpoint was saved."));
      }
      updateJob(lease.jobId(), Map.of("lastProgressAt", now()));
   }



   /*******************************************************************************
    ** Mark a chunk DONE with its outcome. Fenced: throws LeaseLostException when
    ** the lease is gone, so a caller holding a transaction rolls it back. Pass the
    ** chunk's transaction for exactly-once (SAME_TRANSACTION).
    *******************************************************************************/
   public void completeChunk(JobLease lease, ChunkOutcome outcome, QBackendTransaction transaction) throws QException
   {
      Instant                   now    = now();
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("status", ChunkStatus.DONE.name());
      values.put("rowsIn", outcome.rowsIn());
      values.put("rowsOk", outcome.rowsOk());
      values.put("rowsFailed", outcome.rowsFailed());
      values.put("summaryJson", outcome.summaryJson());
      values.put("finishedAt", now);
      values.put("leaseToken", null);
      values.put("leaseExpiresAt", null);
      values.put("modifyDate", now);
      if(cas(TABLE_CHUNK, values, fence(lease), transaction) != 1)
      {
         throw (new LeaseLostException("Lost the lease on chunk " + lease.chunkId() + "; its work is rolled back and will be redone by the new lease holder."));
      }
   }



   /*******************************************************************************
    ** A chunk run threw. Retry it while attempts remain and the failure is
    ** retryable; otherwise FAILED. Fenced. Returns the new status, or null when the
    ** lease was already lost.
    *******************************************************************************/
   public ChunkStatus failChunk(JobLease lease, String errorMessage, boolean retryable) throws QException
   {
      QRecord chunk       = getChunk(lease.chunkId());
      int     maxAttempts = ValueUtils.getValueAsInteger(Objects.requireNonNullElse(chunk.getValue("maxAttempts"), 1));
      ChunkStatus next    = (retryable && lease.attempt() < maxAttempts) ? ChunkStatus.RETRY : ChunkStatus.FAILED;

      Instant                   now    = now();
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("status", next.name());
      values.put("errorMessage", StringUtils.safeTruncate(errorMessage, 4000));
      values.put("finishedAt", now);
      values.put("leaseToken", null);
      values.put("leaseOwner", null);
      values.put("leaseExpiresAt", null);
      values.put("modifyDate", now);
      return (cas(TABLE_CHUNK, values, fence(lease), null) == 1 ? next : null);
   }



   /*******************************************************************************
    ** Give a chunk back without counting the attempt (graceful drain, pause):
    ** RUNNING -> PENDING. Fenced. True when released.
    *******************************************************************************/
   public boolean releaseChunk(JobLease lease, ChunkStatus to) throws QException
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("status", to.name());
      values.put("attempt", Math.max(0, lease.attempt() - 1));
      values.put("leaseToken", null);
      values.put("leaseOwner", null);
      values.put("leaseExpiresAt", null);
      values.put("modifyDate", now());
      return (cas(TABLE_CHUNK, values, fence(lease), null) == 1);
   }



   /*******************************************************************************
    ** Reclaim every RUNNING chunk whose lease expired (by the database clock).
    ** Each goes to RETRY, or FAILED at max attempts, or NEEDS_REVIEW when its job
    ** is not idempotent. Returns the reclaimed chunks.
    *******************************************************************************/
   public List<QRecord> reapExpired(String reaperPodId) throws QException
   {
      Instant       now       = now();
      List<QRecord> expired   = query(TABLE_CHUNK, new QQueryFilter()
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, ChunkStatus.RUNNING.name()))
         .withCriteria(new QFilterCriteria("leaseExpiresAt", QCriteriaOperator.LESS_THAN, now))
         .withLimit(200));
      List<QRecord> reclaimed = new ArrayList<>();

      Map<Integer, QRecord> jobs = new HashMap<>();
      for(QRecord chunk : expired)
      {
         Integer jobId = chunk.getValueInteger("jobId");
         QRecord job   = jobs.computeIfAbsent(jobId, id -> getJobQuietly(id));

         int     attempt     = ValueUtils.getValueAsInteger(Objects.requireNonNullElse(chunk.getValue("attempt"), 0));
         int     maxAttempts = ValueUtils.getValueAsInteger(Objects.requireNonNullElse(chunk.getValue("maxAttempts"), 1));
         int     version     = ValueUtils.getValueAsInteger(Objects.requireNonNullElse(chunk.getValue("leaseVersion"), 0));
         boolean idempotent  = job == null || !JobIdempotency.NONE.name().equals(job.getValueString("idempotency"));

         ChunkStatus next = !idempotent ? ChunkStatus.NEEDS_REVIEW : (attempt >= maxAttempts ? ChunkStatus.FAILED : ChunkStatus.RETRY);

         Map<String, Serializable> values = new LinkedHashMap<>();
         values.put("status", next.name());
         values.put("leaseOwner", null);
         values.put("leaseToken", null);
         values.put("leaseExpiresAt", null);
         values.put("leaseVersion", version + 1);
         values.put("errorMessage", next == ChunkStatus.NEEDS_REVIEW
            ? "The pod running this chunk stopped mid-chunk; it may have partly run. Review it, then retry or skip."
            : "Reclaimed after the pod running it stopped (" + chunk.getValueString("leaseOwner") + ").");
         values.put("modifyDate", now);

         int count = cas(TABLE_CHUNK, values, new QQueryFilter()
            .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, chunk.getValueInteger("id")))
            .withCriteria(new QFilterCriteria("leaseVersion", QCriteriaOperator.EQUALS, version))
            .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, ChunkStatus.RUNNING.name())), null);

         if(count == 1)
         {
            chunk.setValue("status", next.name());
            reclaimed.add(chunk);
            recordEvent(jobId, chunk.getValueInteger("id"), JobEventType.RECLAIMED, null, null, reaperPodId,
               "Chunk " + chunk.getValueInteger("seq") + " (attempt " + attempt + ") reclaimed from " + chunk.getValueString("leaseOwner") + "; now " + next.name());
            LOG.info("Reclaimed expired job chunk", logPair("jobId", jobId), logPair("chunkId", chunk.getValueInteger("id")), logPair("from", chunk.getValueString("leaseOwner")), logPair("to", next));
         }
      }
      return (reclaimed);
   }



   /*******************************************************************************
    ** Set chunks of a job from some statuses to another (control actions).
    *******************************************************************************/
   public int setChunkStatuses(Integer jobId, Collection<ChunkStatus> from, ChunkStatus to, Map<String, Serializable> extraValues) throws QException
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      if(extraValues != null)
      {
         values.putAll(extraValues);
      }
      values.put("status", to.name());
      values.put("modifyDate", now());
      return (cas(TABLE_CHUNK, values, new QQueryFilter()
         .withCriteria(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, from.stream().map(Enum::name).toList())), null));
   }



   /*******************************************************************************
    ** Record row errors for a chunk (batched), in the chunk's transaction.
    *******************************************************************************/
   public void recordRowErrors(List<QRecord> rowErrors, QBackendTransaction transaction) throws QException
   {
      if(rowErrors == null || rowErrors.isEmpty())
      {
         return;
      }
      SystemScope.call(() ->
      {
         for(int i = 0; i < rowErrors.size(); i += 500)
         {
            new InsertAction().execute(new InsertInput(TABLE_ROW_ERROR).withRecords(new ArrayList<>(rowErrors.subList(i, Math.min(rowErrors.size(), i + 500)))).withTransaction(transaction));
         }
         return (null);
      });
   }



   /*******************************************************************************
    ** Add a timeline event. Best effort: a failure is logged, never thrown.
    *******************************************************************************/
   public void recordEvent(Integer jobId, Integer chunkId, JobEventType type, String actorUserId, String clientChannel, String pod, String detail)
   {
      try
      {
         SystemScope.call(() -> new InsertAction().execute(new InsertInput(TABLE_EVENT).withRecord(new QRecord()
            .withValue("jobId", jobId)
            .withValue("chunkId", chunkId)
            .withValue("type", type.name())
            .withValue("actorUserId", actorUserId)
            .withValue("clientChannel", clientChannel)
            .withValue("pod", pod)
            .withValue("detail", StringUtils.safeTruncate(detail, 4000))
            .withValue("createDate", Instant.now()))));
      }
      catch(Exception e)
      {
         LOG.warn("Could not record a job event", e, logPair("jobId", jobId), logPair("type", type));
      }
   }



   /*******************************************************************************
    ** Recompute a job's row and chunk counters from its chunks; return the chunks.
    *******************************************************************************/
   public List<QRecord> refreshCounters(Integer jobId) throws QException
   {
      List<QRecord> chunks = getChunks(jobId);
      int rowsDone     = 0;
      int rowsOk       = 0;
      int rowsFailed   = 0;
      int chunksDone   = 0;
      int chunksFailed = 0;
      for(QRecord chunk : chunks)
      {
         String status = chunk.getValueString("status");
         if(ChunkStatus.DONE.name().equals(status))
         {
            chunksDone++;
            rowsDone += ValueUtils.getValueAsInteger(Objects.requireNonNullElse(chunk.getValue("rowsIn"), 0));
            rowsOk += ValueUtils.getValueAsInteger(Objects.requireNonNullElse(chunk.getValue("rowsOk"), 0));
            rowsFailed += ValueUtils.getValueAsInteger(Objects.requireNonNullElse(chunk.getValue("rowsFailed"), 0));
         }
         else if(ChunkStatus.FAILED.name().equals(status) || ChunkStatus.NEEDS_REVIEW.name().equals(status))
         {
            chunksFailed++;
         }
      }

      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("rowsDone", rowsDone);
      values.put("rowsOk", rowsOk);
      values.put("rowsFailed", rowsFailed);
      values.put("chunksDone", chunksDone);
      values.put("chunksFailed", chunksFailed);
      values.put("chunksTotal", chunks.size());
      values.put("lastProgressAt", now());
      updateJob(jobId, values);

      return (chunks);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QQueryFilter fence(JobLease lease)
   {
      return (new QQueryFilter()
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, lease.chunkId()))
         .withCriteria(new QFilterCriteria("leaseToken", QCriteriaOperator.EQUALS, lease.token()))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, ChunkStatus.RUNNING.name())));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int cas(String tableName, Map<String, Serializable> values, QQueryFilter filter, QBackendTransaction transaction) throws QException
   {
      return (SystemScope.call(() -> new ConditionalUpdateAction().execute(new ConditionalUpdateInput(tableName)
         .withValues(new LinkedHashMap<>(values))
         .withFilter(filter)
         .withTransaction(transaction))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord getJobQuietly(Integer jobId)
   {
      try
      {
         return (getJob(jobId));
      }
      catch(Exception e)
      {
         return (null);
      }
   }



   /*******************************************************************************
    ** Getter for settings
    *******************************************************************************/
   public DurableJobsMetaData getSettings()
   {
      return (this.settings);
   }

}
