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


package com.kingsrook.qqq.middleware.javalin.executors;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsSecurity;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgressLifecycle;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.JobsInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.JobsOutput;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobActionResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobDetailV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobListResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobLogLineV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobProgressPollV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobProgressV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobSummaryV1;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_JOB;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_LOG;


/*******************************************************************************
 ** Authenticated jobs operations. Read permission and parent lock precede all
 ** system-level log reads or control actions, preventing existence disclosure.
 *******************************************************************************/
public class JobsExecutor extends AbstractMiddlewareExecutor<JobsInput, JobsOutput>
{
   private static final List<String> ACTIVE = Arrays.stream(JobStatus.values()).filter(s -> !s.isTerminal()).map(Enum::name).toList();



   /*******************************************************************************
    ** Invalid state transitions are mapped to HTTP 409 by the endpoint.
    *******************************************************************************/
   public static class Conflict extends QException
   {
      /*******************************************************************************
       ** Constructor.
       *******************************************************************************/
      public Conflict(String message)
      {
         super(message);
      }
   }



   /*******************************************************************************
    ** Execute one jobs operation under the current session.
    *******************************************************************************/
   @Override
   public void execute(JobsInput input, JobsOutput output) throws QException
   {
      if(!QContext.getQSession().hasPermission("jobs_job.read"))
      {
         throw (new QPermissionDeniedException("Permission denied."));
      }
      DurableJobsSecurity.ensureSessionKeys(QContext.getQSession());
      DurableJobStore store = new DurableJobStore(DurableJobs.settings());
      if("list".equals(input.getOperation()))
      {
         output.setBody(list(input));
         return;
      }
      if("track".equals(input.getOperation()))
      {
         var request = input.getTrack();
         try
         {
            Integer id = DurableJobsRuntime.start(QContext.getQInstance()).getInlineJobTracker().track(request.getProcessName(), request.getProcessUUID(), request.getJobUUID());
            output.setBody(new JobActionResponseV1().withJobId(id));
         }
         catch(QUserFacingException e)
         {
            if(e instanceof QNotFoundException)
            {
               throw (e);
            }
            throw (new Conflict(e.getMessage()));
         }
         return;
      }
      QRecord job = GetAction.execute(TABLE_JOB, input.getJobId());
      if(job == null)
      {
         throw (new QNotFoundException("Job was not found."));
      }
      JobStatus status = JobStatus.valueOf(job.getValueString("status"));
      switch(input.getOperation())
      {
         case "detail" ->
         {
            JobDetailV1 detail = convert(summary(job), JobDetailV1.class);
            detail.setProgress(convert(JobProgressLifecycle.snapshot(job), JobProgressV1.class));
            if(job.getValueString("summaryJson") != null)
            {
               detail.setSummary(convertJson(job.getValueString("summaryJson"), Object.class));
            }
            detail.setActions(status.isTerminal() || status == JobStatus.NEEDS_REVIEW ? List.of("retry") : status == JobStatus.CANCELLING ? List.of() : List.of("cancel"));
            output.setBody(detail);
         }
         case "progress" -> output.setBody(poll(input, job, store));
         case "cancel" ->
         {
            if(status.isTerminal() || status == JobStatus.CANCELLING)
            {
               throw (new Conflict("This job cannot be cancelled in its current state."));
            }
            control(input.getJobId(), JobAction.CANCEL);
            output.setBody(new JobActionResponseV1().withJobId(input.getJobId()));
         }
         case "retry" ->
         {
            if(!status.isTerminal() && status != JobStatus.NEEDS_REVIEW)
            {
               throw (new Conflict("Only a finished job or a job needing review can be retried."));
            }
            boolean failedChunks = store.getChunks(input.getJobId()).stream().anyMatch(c -> "FAILED".equals(c.getValueString("status")) || "NEEDS_REVIEW".equals(c.getValueString("status")));
            Integer id = input.getJobId();
            if(failedChunks)
            {
               control(id, JobAction.RETRY_FAILED);
            }
            else
            {
               id = DurableJobs.runAgain(id);
            }
            output.setBody(new JobActionResponseV1().withJobId(id));
         }
         default -> throw (new QBadRequestException("Unknown jobs operation."));
      }
   }



   /*******************************************************************************
    ** Keep permission errors distinct from invalid transitions.
    *******************************************************************************/
   private void control(Integer id, JobAction action) throws QException
   {
      try
      {
         DurableJobs.control(id, action);
      }
      catch(QUserFacingException e)
      {
         throw (new Conflict(e.getMessage()));
      }
   }



   /*******************************************************************************
    ** Owner-scoped list with independent active count.
    *******************************************************************************/
   private JobListResponseV1 list(JobsInput input) throws QException
   {
      if("all".equals(input.getScope()) && !QContext.getQSession().hasPermission("jobs_admin"))
      {
         throw (new QPermissionDeniedException("scope=all requires jobs_admin."));
      }
      QQueryFilter filter = baseFilter(input);
      switch(input.getState())
      {
         case "active" -> filter.addCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, ACTIVE));
         case "recent" -> filter.addCriteria(new QFilterCriteria("finishedAt", QCriteriaOperator.GREATER_THAN_OR_EQUALS, Instant.now().minusSeconds(7 * 86400)));
         default ->
         {
            // All accessible jobs, without a state restriction.
         }
      }
      filter.withLimit(input.getLimit()).withOrderBy(new QFilterOrderBy("recent".equals(input.getState()) ? "finishedAt" : "queuedAt", false)).withOrderBy(new QFilterOrderBy("id", false));
      List<JobSummaryV1> jobs = new ArrayList<>();
      for(QRecord job : new QueryAction().execute(new QueryInput(TABLE_JOB).withFilter(filter)).getRecords())
      {
         jobs.add(summary(job));
      }
      QQueryFilter active = baseFilter(input).withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, ACTIVE));
      Integer count = new CountAction().execute(new CountInput(TABLE_JOB).withFilter(active)).getCount();
      return (new JobListResponseV1().withJobs(jobs).withActiveCount(count).withGeneratedAt(Instant.now().toString()));
   }



   /*******************************************************************************
    ** The parent record lock remains applied by QueryAction and CountAction.
    *******************************************************************************/
   private QQueryFilter baseFilter(JobsInput input)
   {
      QQueryFilter filter = new QQueryFilter();
      if(!"all".equals(input.getScope()))
      {
         filter.addCriteria(new QFilterCriteria("userId", QCriteriaOperator.EQUALS, DurableJobsSecurity.userId(QContext.getQSession())));
      }
      if(input.getProcessName() != null)
      {
         filter.addCriteria(new QFilterCriteria("processName", QCriteriaOperator.EQUALS, input.getProcessName()));
      }
      return (filter);
   }



   /*******************************************************************************
    ** Read a twenty-id overlap to tolerate parallel chunks committing out of order.
    *******************************************************************************/
   private JobProgressPollV1 poll(JobsInput input, QRecord job, DurableJobStore store) throws QException
   {
      long          after = input.getAfterLogId();
      List<QRecord> records;
      if(after <= 0)
      {
         //////////////////////////////////////////////////////////////////
         // first read: the newest lines (the tail), oldest first, so a  //
         // client opening a long job sees what it is doing now          //
         //////////////////////////////////////////////////////////////////
         records = new ArrayList<>(store.query(TABLE_LOG, new QQueryFilter()
            .withCriteria(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, input.getJobId()))
            .withOrderBy(new QFilterOrderBy("id", false)).withLimit(input.getLogLimit())));
         Collections.reverse(records);
      }
      else
      {
         records = store.query(TABLE_LOG, new QQueryFilter()
            .withCriteria(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, input.getJobId()))
            .withCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, Math.max(0, after - 20)))
            .withOrderBy(new QFilterOrderBy("id")).withLimit(input.getLogLimit() + 20));
      }
      List<JobLogLineV1> lines = new ArrayList<>();
      for(QRecord record : records)
      {
         lines.add(convert(record.getValues(), JobLogLineV1.class));
         after = Math.max(after, record.getValueLong("id"));
      }
      if(records.size() < input.getLogLimit() + 20 && job.getValueLong("logSeq") != null)
      {
         after = Math.max(after, job.getValueLong("logSeq"));
      }
      return (new JobProgressPollV1().withJob(summary(job)).withProgress(convert(JobProgressLifecycle.snapshot(job), JobProgressV1.class))
         .withLog(lines).withNextAfterLogId(after).withLogDropped(job.getValueInteger("logDropped") == null ? 0 : job.getValueInteger("logDropped"))
         .withTerminal(JobStatus.valueOf(job.getValueString("status")).isTerminal()));
   }



   /*******************************************************************************
    ** Expose only display fields, never parameters, source data or credentials.
    *******************************************************************************/
   private JobSummaryV1 summary(QRecord job) throws QException
   {
      Map<String, Object> values = new LinkedHashMap<>();
      for(String field : List.of("label", "processName", "status", "sourceType", "userId", "currentPhase", "bytesDone", "bytesTotal", "etaSeconds", "rowsDone", "rowsTotal", "rowsFailed", "chunksDone", "chunksTotal", "queuedAt", "startedAt", "finishedAt", "logSeq"))
      {
         values.put(field, job.getValue(field));
      }
      values.put("jobId", job.getValueInteger("id"));
      values.put("percent", JobProgressLifecycle.snapshot(job).getPercent());
      values.put("message", job.getValueString("progressMessage"));
      return (convert(values, JobSummaryV1.class));
   }



   /*******************************************************************************
    ** Convert typed domain values to the versioned wire representation.
    *******************************************************************************/
   public static <T> T convert(Object value, Class<T> type) throws QException
   {
      return (convertJson(JsonUtils.toJson(value), type));
   }



   /*******************************************************************************
    ** Read stored JSON using the framework mapper.
    *******************************************************************************/
   private static <T> T convertJson(String json, Class<T> type) throws QException
   {
      try
      {
         return (JsonUtils.toObject(json, type));
      }
      catch(Exception e)
      {
         throw (new QException("Could not read job display data", e));
      }
   }
}

