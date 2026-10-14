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


import java.io.Serializable;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.model.JobMode;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.plans.StepsJobPlan;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobFinalizer;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobSignals;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** The effect of each control action (spec section 2.7), as compare-and-set
 ** transitions on the job and its chunks. Authorization (owner or jobs_admin,
 ** and the assistant gate) is checked by DurableJobs.control before this runs.
 *******************************************************************************/
class JobControl
{
   private static final EnumSet<JobStatus> ACTIVE = EnumSet.of(JobStatus.AWAITING_CONFIRMATION, JobStatus.QUEUED, JobStatus.PLANNING, JobStatus.RUNNING, JobStatus.PAUSING, JobStatus.PAUSED, JobStatus.NEEDS_REVIEW);

   private final DurableJobStore store;
   private final String          actorUserId;
   private final String          clientChannel;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   JobControl(DurableJobStore store, String actorUserId, String clientChannel)
   {
      this.store = store;
      this.actorUserId = actorUserId;
      this.clientChannel = clientChannel;
   }



   /*******************************************************************************
    ** Apply an action; return a one-line result. Throws QUserFacingException when
    ** the action does not apply to the job's current status.
    *******************************************************************************/
   String apply(QRecord job, JobAction action) throws QException
   {
      Integer   jobId  = job.getValueInteger("id");
      JobStatus status = JobStatus.valueOf(job.getValueString("status"));
      String    label  = job.getValueString("label");

      switch(action)
      {
         case START ->
         {
            require(store.setJobStatus(jobId, EnumSet.of(JobStatus.AWAITING_CONFIRMATION), JobStatus.QUEUED, reason(null)), action, status);
            event(jobId, JobEventType.CONFIRMED, "Confirmed and queued");
         }
         case PAUSE ->
         {
            if(!store.setJobStatus(jobId, EnumSet.of(JobStatus.QUEUED, JobStatus.AWAITING_CONFIRMATION), JobStatus.PAUSED, reason("Paused")))
            {
               require(store.setJobStatus(jobId, EnumSet.of(JobStatus.RUNNING, JobStatus.PLANNING), JobStatus.PAUSING, reason("Pausing: running chunks stop at their next safe point")), action, status);
            }
            event(jobId, JobEventType.PAUSED, "Paused");
         }
         case RESUME ->
         {
            require(store.setJobStatus(jobId, EnumSet.of(JobStatus.PAUSED, JobStatus.PAUSING), JobStatus.QUEUED, reason(null)), action, status);
            event(jobId, JobEventType.RESUMED, "Resumed");
         }
         case CANCEL ->
         {
            require(store.setJobStatus(jobId, ACTIVE, JobStatus.CANCELLING, reason("Cancelling")), action, status);
            int cancelled = store.setChunkStatuses(jobId, EnumSet.of(ChunkStatus.PENDING, ChunkStatus.RETRY, ChunkStatus.NEEDS_REVIEW), ChunkStatus.CANCELLED, null);
            event(jobId, JobEventType.CANCELLED, "Cancelled; " + cancelled + " chunks will not run. Finished chunks stay applied.");
         }
         case RETRY_FAILED ->
         {
            int retried = store.setChunkStatuses(jobId, EnumSet.of(ChunkStatus.FAILED, ChunkStatus.NEEDS_REVIEW), ChunkStatus.PENDING, Map.of("attempt", 0));
            if(retried == 0)
            {
               throw (new QUserFacingException(label + " has no failed chunks to retry."));
            }
            require(store.setJobStatus(jobId, EnumSet.of(JobStatus.FAILED, JobStatus.NEEDS_REVIEW, JobStatus.COMPLETED_WITH_ERRORS, JobStatus.PAUSED, JobStatus.QUEUED, JobStatus.RUNNING), JobStatus.QUEUED, requeue()), action, status);
            event(jobId, JobEventType.RETRIED, "Retrying " + retried + " failed chunks");
         }
         case RESTART ->
         {
            if(!(status.isTerminal() || status == JobStatus.PAUSED || status == JobStatus.NEEDS_REVIEW))
            {
               throw (new QUserFacingException(label + " is " + status.getPossibleValueLabel().toLowerCase() + "; pause or cancel it before restarting."));
            }
            int reset = store.setChunkStatuses(jobId, EnumSet.of(ChunkStatus.PENDING, ChunkStatus.RETRY, ChunkStatus.FAILED, ChunkStatus.NEEDS_REVIEW, ChunkStatus.CANCELLED), ChunkStatus.PENDING,
               new LinkedHashMap<>(Map.of("attempt", 0)));
            require(store.setJobStatus(jobId, EnumSet.of(status), JobStatus.QUEUED, requeue()), action, status);
            event(jobId, JobEventType.RESTARTED, "Restarted; " + reset + " unfinished chunks will run again");
         }
         case RUN_AGAIN ->
         {
            Integer newJobId = DurableJobs.launch(relaunchRequest(job));
            event(jobId, JobEventType.LAUNCHED, "Run again as job " + newJobId);
            return (label + ": started again as job " + newJobId + ".");
         }
         default -> throw (new QUserFacingException("Unknown job action " + action));
      }

      JobStatus after = new JobFinalizer(store).evaluate(jobId);
      if(after != null)
      {
         JobSignals.status(jobId, job.getValueString("userId"), after);
      }
      return (label + ": " + action.getPastTense() + ".");
   }



   /*******************************************************************************
    ** The request that runs a job again from the start, with the same inputs.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static LaunchRequest relaunchRequest(QRecord job) throws QException
   {
      LaunchRequest request = new LaunchRequest(job.getValueString("processName"))
         .withValues(StepsJobPlan.readValues(job.getValueString("paramsJson")))
         .withMode(JobMode.valueOf(job.getValueString("mode")))
         .withPriority(job.getValueInteger("priority"))
         .withNotifyEmail(job.getValueBoolean("notifyEmail"))
         .withLabel(job.getValueString("label"))
         .withSavedBulkLoadProfileId(job.getValueInteger("savedBulkLoadProfileId"))
         .withParentJobId(job.getValueInteger("id"));
      try
      {
         Map<String, Object> source = job.getValueString("sourceJson") == null ? Map.of() : JsonUtils.toObject(job.getValueString("sourceJson"), Map.class);
         if(source.get(StepsJobPlan.SOURCE_START_AFTER_STEP) instanceof String s)
         {
            request.withStartAfterStep(s);
         }
         if(source.get("filter") != null)
         {
            request.withFilter(JsonUtils.toObject(JsonUtils.toJson(source.get("filter")), QQueryFilter.class));
         }
         if(source.get("recordIds") instanceof List<?> ids)
         {
            request.withRecordIds(ids.stream().map(id -> (Serializable) id).toList());
         }
         if(source.get("uploadRefs") instanceof List<?> refs)
         {
            request.withUploadRefs(refs.stream().map(String::valueOf).toList());
         }
         if(source.get("profileJson") instanceof String profileJson)
         {
            request.withProfileJson(profileJson);
         }
      }
      catch(Exception e)
      {
         throw (new QException("Could not read the job's source to run it again", e));
      }
      return (request);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void require(boolean won, JobAction action, JobStatus status) throws QUserFacingException
   {
      if(!won)
      {
         throw (new QUserFacingException("Cannot " + action.getLabel().toLowerCase() + " a job that is " + status.getPossibleValueLabel().toLowerCase() + "."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Serializable> reason(String reason)
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("statusReason", reason);
      return (values);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Serializable> requeue()
   {
      Map<String, Serializable> values = reason(null);
      values.put("finishedAt", null);
      values.put("errorMessage", null);
      return (values);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void event(Integer jobId, JobEventType type, String detail)
   {
      store.recordEvent(jobId, null, type, actorUserId, clientChannel, null, detail);
   }

}
