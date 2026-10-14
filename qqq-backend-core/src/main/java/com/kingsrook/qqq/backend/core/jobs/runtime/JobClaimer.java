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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableProcessMetaData;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.model.JobLease;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_CHUNK;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_JOB;


/*******************************************************************************
 ** Picks the next chunk for this pod and claims it (spec section 2.8).
 **
 ** Order: highest priority first, then round-robin between users (a user's
 ** second job ranks after every other user's first), then oldest queuedAt.
 ** Caps: maxConcurrentJobs per process, maxActiveJobsPerUser per user, and
 ** maxParallelChunks per job. A QUEUED job held back by a cap gets a
 ** statusReason saying why.
 *******************************************************************************/
public class JobClaimer
{
   private static final EnumSet<JobStatus> ACTIVE = EnumSet.of(JobStatus.RUNNING, JobStatus.PLANNING, JobStatus.PAUSING, JobStatus.CANCELLING);

   private final DurableJobStore     store;
   private final DurableJobsMetaData settings;
   private final String              podId;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public JobClaimer(DurableJobStore store, String podId)
   {
      this.store = store;
      this.settings = store.getSettings();
      this.podId = podId;
   }



   /*******************************************************************************
    ** Claim the next chunk this pod may run, or empty when there is none.
    *******************************************************************************/
   public Optional<JobLease> claimNext() throws QException
   {
      List<QRecord> jobs = store.query(TABLE_JOB, new QQueryFilter()
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, List.of(JobStatus.QUEUED.name(), JobStatus.RUNNING.name(), JobStatus.PLANNING.name(), JobStatus.PAUSING.name(), JobStatus.CANCELLING.name())))
         .withOrderBy(new QFilterOrderBy("priority", false))
         .withOrderBy(new QFilterOrderBy("queuedAt", true))
         .withLimit(500));
      if(jobs.isEmpty())
      {
         return (Optional.empty());
      }

      //////////////////////////////////////////////////////////////
      // what is active now, per process and per user, for caps  //
      //////////////////////////////////////////////////////////////
      Map<String, Integer> activeByProcess = new HashMap<>();
      Map<String, Integer> activeByUser    = new HashMap<>();
      for(QRecord job : jobs)
      {
         if(ACTIVE.contains(JobStatus.valueOf(job.getValueString("status"))))
         {
            activeByProcess.merge(job.getValueString("processName"), 1, Integer::sum);
            activeByUser.merge(job.getValueString("userId"), 1, Integer::sum);
         }
      }

      ////////////////////////////////////////////////////////////////////////
      // candidates: RUNNING jobs (more chunks) and QUEUED jobs, fair order //
      ////////////////////////////////////////////////////////////////////////
      Map<String, Integer> rankWithinUser = new HashMap<>();
      List<Candidate>      candidates     = new ArrayList<>();
      for(QRecord job : jobs)
      {
         JobStatus status = JobStatus.valueOf(job.getValueString("status"));
         if(!status.isClaimable())
         {
            continue;
         }
         int rank = rankWithinUser.merge(job.getValueString("userId"), 1, Integer::sum);
         candidates.add(new Candidate(job, status, rank));
      }
      candidates.sort(Comparator.comparing((Candidate c) -> -intValue(c.job, "priority"))
         .thenComparing(c -> c.rank)
         .thenComparing(c -> Objects.requireNonNullElse(c.job.getValueInstant("queuedAt"), Instant.EPOCH)));

      for(Candidate candidate : candidates)
      {
         QRecord job       = candidate.job;
         Integer jobId     = job.getValueInteger("id");
         String  process   = job.getValueString("processName");
         String  userId    = job.getValueString("userId");

         if(candidate.status == JobStatus.QUEUED)
         {
            int processCap = processCap(process);
            if(activeByProcess.getOrDefault(process, 0) >= processCap)
            {
               holdBack(job, "Waiting: " + processCap + (processCap == 1 ? " job" : " jobs") + " of this process are already running.");
               continue;
            }
            if(activeByUser.getOrDefault(userId, 0) >= settings.getMaxActiveJobsPerUser())
            {
               holdBack(job, "Waiting: you already have " + settings.getMaxActiveJobsPerUser() + " jobs running.");
               continue;
            }
         }

         List<QRecord> chunks = store.query(TABLE_CHUNK, new QQueryFilter()
            .withCriteria(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId))
            .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, List.of(ChunkStatus.PENDING.name(), ChunkStatus.RETRY.name(), ChunkStatus.RUNNING.name())))
            .withOrderBy(new QFilterOrderBy("seq")));

         long running     = chunks.stream().filter(c -> ChunkStatus.RUNNING.name().equals(c.getValueString("status"))).count();
         int  maxParallel = Math.max(1, intValue(job, "maxParallelChunks"));
         if(running >= maxParallel)
         {
            continue;
         }

         for(QRecord chunk : chunks)
         {
            if(!ChunkStatus.valueOf(chunk.getValueString("status")).isClaimable())
            {
               continue;
            }
            Optional<JobLease> lease = store.claimChunk(chunk, podId, settings.getLeaseTtlSeconds());
            if(lease.isPresent())
            {
               if(candidate.status == JobStatus.QUEUED)
               {
                  Map<String, Serializable> values = new LinkedHashMap<>();
                  values.put("statusReason", null);
                  if(job.getValue("startedAt") == null)
                  {
                     values.put("startedAt", store.now());
                  }
                  if(store.setJobStatus(jobId, EnumSet.of(JobStatus.QUEUED), JobStatus.RUNNING, values))
                  {
                     store.recordEvent(jobId, null, JobEventType.STARTED, null, null, podId, "Started on " + podId);
                     JobSignals.status(jobId, userId, JobStatus.RUNNING);
                  }
               }
               return (lease);
            }
            ////////////////////////////////////////////////////////////////
            // another pod won this chunk - try the job's next chunk once //
            ////////////////////////////////////////////////////////////////
            if(maxParallel == 1)
            {
               break;
            }
         }
      }
      return (Optional.empty());
   }



   /*******************************************************************************
    ** The concurrent-job cap for a process.
    *******************************************************************************/
   private int processCap(String processName)
   {
      DurableProcessMetaData durable = DurableProcessMetaData.of(QContext.getQInstance().getProcess(processName));
      if(durable != null && durable.getMaxConcurrentJobs() != null)
      {
         return (durable.getMaxConcurrentJobs());
      }
      return (settings.getDefaultMaxConcurrentJobsPerProcess());
   }



   /*******************************************************************************
    ** Say why a queued job is not starting (only when the reason changes).
    *******************************************************************************/
   private void holdBack(QRecord job, String reason) throws QException
   {
      if(!reason.equals(job.getValueString("statusReason")))
      {
         store.updateJob(job.getValueInteger("id"), Map.of("statusReason", reason));
         job.setValue("statusReason", reason);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int intValue(QRecord record, String field)
   {
      Integer value = ValueUtils.getValueAsInteger(record.getValue(field));
      return (value == null ? 0 : value);
   }



   /*******************************************************************************
    ** A job that could get a chunk claimed, with its rank among its user's jobs.
    *******************************************************************************/
   private record Candidate(QRecord job, JobStatus status, int rank)
   {
   }

}
