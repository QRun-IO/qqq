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


package com.kingsrook.qqq.backend.core.jobs.runtime;


import java.io.Serializable;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.FailedRows;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Moves a job to the status its chunks imply, once nothing is running:
 ** CANCELLING -> CANCELLED, PAUSING -> PAUSED, and RUNNING (or QUEUED) with no
 ** work left -> COMPLETED, COMPLETED_WITH_ERRORS, FAILED or NEEDS_REVIEW.
 **
 ** Every transition is a compare-and-set, so any pod may call evaluate at any
 ** time: exactly one wins a transition, and only the winner signals it (one
 ** notification per finish).
 *******************************************************************************/
public class JobFinalizer
{
   private static final QLogger LOG = QLogger.getLogger(JobFinalizer.class);

   private final DurableJobStore store;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public JobFinalizer(DurableJobStore store)
   {
      this.store = store;
   }



   /*******************************************************************************
    ** Re-evaluate one job. Returns the job's status afterwards.
    *******************************************************************************/
   public JobStatus evaluate(Integer jobId) throws QException
   {
      QRecord job = store.getJob(jobId);
      if(job == null)
      {
         return (null);
      }

      JobStatus status = JobStatus.valueOf(job.getValueString("status"));
      if(status.isTerminal() || status == JobStatus.AWAITING_CONFIRMATION)
      {
         return (status);
      }

      List<QRecord> chunks = store.refreshCounters(jobId);
      job = store.getJob(jobId);
      JobSignals.progress(job, false);

      int    running     = 0;
      int    claimable   = 0;
      int    failed      = 0;
      int    needsReview = 0;
      String firstError = null;
      for(QRecord chunk : chunks)
      {
         ChunkStatus chunkStatus = ChunkStatus.valueOf(chunk.getValueString("status"));
         switch(chunkStatus)
         {
            case RUNNING -> running++;
            case PENDING, RETRY -> claimable++;
            case FAILED -> failed++;
            case NEEDS_REVIEW -> needsReview++;
            default ->
            {
               // DONE and CANCELLED need nothing
            }
         }
         if((chunkStatus == ChunkStatus.FAILED || chunkStatus == ChunkStatus.NEEDS_REVIEW) && firstError == null)
         {
            firstError = chunk.getValueString("errorMessage");
         }
      }

      if(running > 0)
      {
         return (status);
      }

      Instant now = store.now();
      if(status == JobStatus.CANCELLING)
      {
         return (transition(job, EnumSet.of(JobStatus.CANCELLING), JobStatus.CANCELLED, finished(now, cancelSummary(job)), JobEventType.CANCELLED));
      }

      if(status == JobStatus.PAUSING && claimable > 0)
      {
         return (transition(job, EnumSet.of(JobStatus.PAUSING), JobStatus.PAUSED, Map.of("statusReason", "Paused"), null));
      }

      if(claimable > 0 || status == JobStatus.PAUSED)
      {
         return (status);
      }

      ///////////////////////////////
      // no work left: finish it.  //
      ///////////////////////////////
      int       rowsFailed = ValueUtils.getValueAsInteger(job.getValue("rowsFailed")) == null ? 0 : ValueUtils.getValueAsInteger(job.getValue("rowsFailed"));
      JobStatus finalStatus;
      if(needsReview > 0)
      {
         finalStatus = JobStatus.NEEDS_REVIEW;
      }
      else if(failed > 0)
      {
         finalStatus = JobStatus.FAILED;
      }
      else if(rowsFailed > 0)
      {
         finalStatus = JobStatus.COMPLETED_WITH_ERRORS;
      }
      else
      {
         finalStatus = JobStatus.COMPLETED;
      }

      Map<String, Serializable> values = finished(now, null);
      values.put("errorMessage", firstError == null ? null : StringUtils.safeTruncate(firstError, 4000));
      if(finalStatus == JobStatus.NEEDS_REVIEW)
      {
         values.remove("finishedAt");
         values.put("statusReason", needsReview + (needsReview == 1 ? " chunk" : " chunks") + " may have partly run; review, then retry or skip.");
      }
      JobEventType eventType = (finalStatus == JobStatus.COMPLETED || finalStatus == JobStatus.COMPLETED_WITH_ERRORS) ? JobEventType.COMPLETED : JobEventType.FAILED;
      return (transition(job, EnumSet.of(JobStatus.QUEUED, JobStatus.PLANNING, JobStatus.RUNNING, JobStatus.PAUSING), finalStatus, values, eventType));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JobStatus transition(QRecord job, EnumSet<JobStatus> from, JobStatus to, Map<String, Serializable> values, JobEventType eventType) throws QException
   {
      Integer jobId = job.getValueInteger("id");
      if(!store.setJobStatus(jobId, from, to, values))
      {
         QRecord current = store.getJob(jobId);
         return (current == null ? null : JobStatus.valueOf(current.getValueString("status")));
      }

      QRecord updated = store.getJob(jobId);
      if(eventType != null)
      {
         store.recordEvent(jobId, null, eventType, null, null, null, describe(updated, to));
      }
      LOG.info("Durable job status changed", logPair("jobId", jobId), logPair("status", to));

      if(to == JobStatus.COMPLETED || to == JobStatus.COMPLETED_WITH_ERRORS)
      {
         try
         {
            FailedRows.resolveAfterRetry(updated);
         }
         catch(Exception e)
         {
            LOG.warn("Could not resolve the parent job's retried rows", e, logPair("jobId", jobId));
         }
      }
      if(to.isTerminal() || to == JobStatus.NEEDS_REVIEW)
      {
         JobSignals.finished(updated, to);
      }
      else
      {
         JobSignals.status(jobId, updated.getValueString("userId"), to);
      }
      return (to);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Serializable> finished(Instant now, String statusReason)
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("finishedAt", now);
      values.put("statusReason", statusReason);
      return (values);
   }



   /*******************************************************************************
    ** Cancel keeps committed work; say how much.
    *******************************************************************************/
   private static String cancelSummary(QRecord job)
   {
      Integer rowsOk = ValueUtils.getValueAsInteger(job.getValue("rowsOk"));
      Integer chunks = ValueUtils.getValueAsInteger(job.getValue("chunksDone"));
      return ("Cancelled. " + (chunks == null ? 0 : chunks) + " finished chunks (" + (rowsOk == null ? 0 : rowsOk) + " rows) were applied and are not undone.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String describe(QRecord job, JobStatus status)
   {
      return (status.getPossibleValueLabel() + ": " + ValueUtils.getValueAsInteger(job.getValue("rowsOk")) + " rows ok, "
         + ValueUtils.getValueAsInteger(job.getValue("rowsFailed")) + " rows failed, "
         + ValueUtils.getValueAsInteger(job.getValue("chunksDone")) + " of " + ValueUtils.getValueAsInteger(job.getValue("chunksTotal")) + " chunks done"
         + (job.getValueString("statusReason") == null ? "" : ". " + job.getValueString("statusReason")));
   }

}
