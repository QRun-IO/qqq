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


package com.kingsrook.qqq.backend.core.jobs.progress;


import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.ConditionalUpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTables;
import com.kingsrook.qqq.backend.core.jobs.model.JobLease;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobSignals;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_JOB;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_LOG;


/*******************************************************************************
 ** One buffer/flusher per running chunk. Fencing and log pruning use an independent
 ** short transaction. WARN/ERROR are retained even when they alone exceed the cap.
 *******************************************************************************/
public class JobLogWriter
{
   private static final QLogger LOG = QLogger.getLogger(JobLogWriter.class);
   private final DurableJobStore store;
   private final Integer jobId;
   private final JobLease lease;
   private final Runnable lostLease;
   private final List<QRecord> pending = new ArrayList<>();
   private long lastFlush;
   private QRecord reclaimedChunk;



   /*******************************************************************************
    ** A reclaim reset must not overwrite a newly claimed attempt's live progress.
    *******************************************************************************/
   public synchronized JobLogWriter withReclaimedChunk(QRecord chunk)
   {
      this.reclaimedChunk = chunk;
      return (this);
   }



   /*******************************************************************************
    ** Construct a chunk writer, or a kernel writer with no lease.
    *******************************************************************************/
   public JobLogWriter(DurableJobStore store, Integer jobId, JobLease lease, Runnable lostLease)
   {
      this.store = store;
      this.jobId = jobId;
      this.lease = lease;
      this.lostLease = lostLease;
   }



   /*******************************************************************************
    ** Apply mandatory rules and optional application rules to every displayed line.
    *******************************************************************************/
   public static String redact(String message, DurableJobsMetaData settings)
   {
      DefaultJobLogRedactor defaults = new DefaultJobLogRedactor();
      String safe = defaults.redact(message);
      if(settings != null && settings.getLogRedactor() != null)
      {
         safe = defaults.redact(settings.getLogRedactor().redact(safe));
      }
      return (safe);
   }



   /*******************************************************************************
    ** Buffer a line. The caller combines log and progress in a single flush.
    *******************************************************************************/
   public synchronized void append(JobLogLevel level, String phase, String message)
   {
      append(level, phase, message, lease == null ? null : lease.chunkId(), lease == null ? null : lease.attempt());
   }



   /*******************************************************************************
    ** Kernel lines can identify an interrupted attempt without its expired lease.
    *******************************************************************************/
   public synchronized void append(JobLogLevel level, String phase, String message, Integer chunkId, Integer attempt)
   {
      pending.add(new QRecord().withValue("jobId", jobId)
         .withValue("chunkId", chunkId)
         .withValue("attempt", attempt)
         .withValue("level", level.name()).withValue("loggedAt", Instant.now())
         .withValue("phase", redact(phase, store.getSettings()))
         .withValue("message", redact(message, store.getSettings())));
   }



   /*******************************************************************************
    ** Flush at most once per second, except phase changes, warnings, and chunk end.
    *******************************************************************************/
   public synchronized boolean flush(JobProgressSnapshot snapshot, boolean force)
   {
      long now = System.currentTimeMillis();
      if(!force && now - lastFlush < 1000)
      {
         return (false);
      }
      CapturedContext original = QContext.capture();
      try
      {
         QContext.init(new CapturedContext(original.qInstance(), new QSystemUserSession()));
         try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE_JOB)))
         {
            try
            {
               boolean resetReclaim = true;
               if(reclaimedChunk != null)
               {
                  resetReclaim = new ConditionalUpdateAction().execute(new ConditionalUpdateInput(DurableJobsTables.TABLE_CHUNK)
                     .withValues(Map.of("modifyDate", store.now())).withTransaction(transaction)
                     .withFilter(new QQueryFilter()
                        .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, reclaimedChunk.getValueInteger("id")))
                        .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, reclaimedChunk.getValueString("status")))
                        .withCriteria(new QFilterCriteria("leaseVersion", QCriteriaOperator.EQUALS, reclaimedChunk.getValueInteger("leaseVersion") + 1)))) == 1;
               }
               if(lease != null && !store.heartbeat(lease, store.getSettings().getLeaseTtlSeconds(), transaction))
               {
                  pending.clear();
                  if(lostLease != null)
                  {
                     lostLease.run();
                  }
                  transaction.rollback();
                  return (false);
               }
               // Serialize concurrent chunk flushes on the parent before pruning/counting.
               update(Map.of("lastProgressAt", store.now()), transaction);
               QRecord job = new GetAction().execute(new GetInput(TABLE_JOB).withPrimaryKey(jobId).withTransaction(transaction)).getRecord();
               long seq = job.getValueLong("logSeq") == null ? 0 : job.getValueLong("logSeq");
               boolean wroteLog = !pending.isEmpty();
               if(wroteLog)
               {
                  var inserted = new InsertAction().execute(new InsertInput(TABLE_LOG).withRecords(new ArrayList<>(pending)).withTransaction(transaction));
                  for(QRecord line : inserted.getRecords())
                  {
                     if(line.getErrors() != null && !line.getErrors().isEmpty())
                     {
                        throw (new QException("Could not persist job log"));
                     }
                     seq = Math.max(seq, line.getValueLong("id"));
                  }
               }
               Map<String, Serializable> values = new LinkedHashMap<>();
               values.put("logSeq", seq);
               if(snapshot != null && resetReclaim)
               {
                  snapshot.setMessage(redact(snapshot.getMessage(), store.getSettings()));
                  snapshot.setCurrentPhase(redact(snapshot.getCurrentPhase(), store.getSettings()));
                  if(snapshot.getPhases() != null)
                  {
                     for(JobPhase phase : snapshot.getPhases())
                     {
                        phase.setName(redact(phase.getName(), store.getSettings()));
                        phase.setLabel(redact(phase.getLabel(), store.getSettings()));
                     }
                  }
                  if(lease != null && job.getValueInteger("percentDone") != null && snapshot.getPercent() != null)
                  {
                     snapshot.setPercent(Math.max(snapshot.getPercent(), job.getValueInteger("percentDone")));
                  }
                  values.putAll(progressValues(snapshot));
               }
               ///////////////////////////////////////////////////////////////////
               // count first; only read prunable lines when over the cap, so a //
               // flush under the cap costs one count, not a read of every line //
               ///////////////////////////////////////////////////////////////////
               List<Serializable> remove = List.of();
               if(wroteLog)
               {
                  Integer count = new CountAction().execute(new CountInput(TABLE_LOG).withTransaction(transaction)
                     .withFilter(new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)))).getCount();
                  int excess = Math.max(0, (count == null ? 0 : count) - store.getSettings().getLogMaxLines());
                  if(excess > 0)
                  {
                     remove = new QueryAction().execute(new QueryInput(TABLE_LOG).withTransaction(transaction)
                        .withFilter(new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId))
                           .withCriteria(new QFilterCriteria("level", QCriteriaOperator.IN, List.of("INFO", "DEBUG")))
                           .withOrderBy(new QFilterOrderBy("id")).withLimit(excess))).getRecords()
                        .stream().map(r -> r.getValue("id")).toList();
                  }
               }
               if(!remove.isEmpty())
               {
                  new DeleteAction().execute(new DeleteInput(TABLE_LOG).withPrimaryKeys(remove).withTransaction(transaction));
                  values.put("logDropped", (job.getValueInteger("logDropped") == null ? 0 : job.getValueInteger("logDropped")) + remove.size());
               }
               update(values, transaction);
               transaction.commit();
               pending.clear();
               lastFlush = now;
               JobSignals.progress(store.getJob(jobId), force || wroteLog);
               return (true);
            }
            catch(Exception e)
            {
               transaction.rollback();
               throw (e);
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Could not flush job progress", e);
         return (false);
      }
      finally
      {
         QContext.init(original);
      }
   }



   /*******************************************************************************
    ** Columns mirror the snapshot. Stored percent is an integer, rounded down.
    *******************************************************************************/
   public static Map<String, Serializable> progressValues(JobProgressSnapshot snapshot)
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("currentPhase", snapshot.getCurrentPhase());
      values.put("percentDone", snapshot.getPercent() == null ? null : snapshot.getPercent().intValue());
      values.put("progressMessage", snapshot.getMessage());
      values.put("bytesDone", snapshot.getBytesDone());
      values.put("bytesTotal", snapshot.getBytesTotal());
      values.put("etaSeconds", snapshot.getEtaSeconds());
      values.put("progressJson", JsonUtils.toJson(snapshot));
      return (values);
   }



   /*******************************************************************************
    ** Write under this flush transaction, never the caller transaction.
    *******************************************************************************/
   private void update(Map<String, Serializable> values, QBackendTransaction transaction) throws QException
   {
      new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE_JOB).withValues(values)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, jobId))).withTransaction(transaction));
   }
}
