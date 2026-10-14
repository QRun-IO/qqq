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
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Progress changes owned by the kernel at reclaim and terminal transitions.
 *******************************************************************************/
public final class JobProgressLifecycle
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private JobProgressLifecycle()
   {
   }



   /*******************************************************************************
    ** Read a snapshot, falling back to committed counts for existing jobs.
    *******************************************************************************/
   public static JobProgressSnapshot snapshot(QRecord job)
   {
      JobProgressSnapshot snapshot = null;
      if(job.getValueString("progressJson") != null)
      {
         try
         {
            snapshot = JsonUtils.toObject(job.getValueString("progressJson"), JobProgressSnapshot.class);
         }
         catch(Exception e)
         {
            // An old or invalid snapshot must not hide committed progress.
         }
      }
      if(snapshot == null)
      {
         snapshot = new JobProgressSnapshot().withUnitLabel("rows");
         new JobProgressMath().update(snapshot, job, 0, Instant.now());
      }
      if(snapshot.getPercent() == null && job.getValueInteger("percentDone") != null)
      {
         snapshot.setPercent(job.getValueInteger("percentDone").doubleValue());
      }
      return (snapshot);
   }



   /*******************************************************************************
    ** Reset live fields to committed work, preserving the interrupted attempt log.
    *******************************************************************************/
   public static void reclaimed(DurableJobStore store, QRecord chunk) throws QException
   {
      Integer jobId = chunk.getValueInteger("jobId");
      QRecord job = store.getJob(jobId);
      int rowsDone = 0;
      int chunksDone = 0;
      var chunks = store.getChunks(jobId);
      for(QRecord completed : chunks)
      {
         if("DONE".equals(completed.getValueString("status")))
         {
            chunksDone++;
            rowsDone += completed.getValueInteger("rowsIn") == null ? 0 : completed.getValueInteger("rowsIn");
         }
      }
      job.setValue("rowsDone", rowsDone);
      job.setValue("chunksDone", chunksDone);
      job.setValue("chunksTotal", chunks.size());
      JobProgressSnapshot snapshot = new JobProgressSnapshot().withUnitLabel("rows");
      new JobProgressMath().update(snapshot, job, 0, Instant.now());
      JobLogWriter writer = new JobLogWriter(store, jobId, null, null).withReclaimedChunk(chunk);
      String outcome = "RETRY".equals(chunk.getValueString("status")) ? "retrying" : "needs review";
      writer.append(JobLogLevel.WARN, null, "Attempt " + chunk.getValueInteger("attempt") + " was interrupted (the server stopped); " + outcome,
         chunk.getValueInteger("id"), chunk.getValueInteger("attempt"));
      writer.flush(snapshot, true);
   }



   /*******************************************************************************
    ** Append the winning terminal transition summary after its snapshot commits.
    *******************************************************************************/
   public static void finished(DurableJobStore store, QRecord job, JobStatus status, String summary)
   {
      JobLogWriter writer = new JobLogWriter(store, job.getValueInteger("id"), null, null);
      writer.append(status == JobStatus.FAILED || status == JobStatus.NEEDS_REVIEW ? JobLogLevel.ERROR : JobLogLevel.INFO, job.getValueString("currentPhase"), summary);
      writer.flush(null, true);
   }



   /*******************************************************************************
    ** Persist final progress atomically with the winning status transition.
    *******************************************************************************/
   public static Map<String, Serializable> terminalValues(DurableJobStore store, QRecord job, JobStatus status, String summary)
   {
      return (JobLogWriter.progressValues(finishingSnapshot(store, job, status, summary)));
   }



   /*******************************************************************************
    ** Derive the same final snapshot for the status CAS and final log flush.
    *******************************************************************************/
   private static JobProgressSnapshot finishingSnapshot(DurableJobStore store, QRecord job, JobStatus status, String summary)
   {
      JobProgressSnapshot snapshot = snapshot(job);
      if(status == JobStatus.COMPLETED)
      {
         snapshot.setPercent(100.0);
      }
      if(snapshot.getPhases() != null)
      {
         for(JobPhase phase : snapshot.getPhases())
         {
            if(phase.getStatus() == JobPhase.Status.RUNNING)
            {
               phase.setStatus(status == JobStatus.COMPLETED || status == JobStatus.COMPLETED_WITH_ERRORS ? JobPhase.Status.DONE : JobPhase.Status.FAILED);
            }
         }
      }
      snapshot.setEtaSeconds(null);
      snapshot.setMessage(JobLogWriter.redact(summary, store.getSettings()));
      snapshot.setUpdatedAt(Instant.now());
      return (snapshot);
   }
}
