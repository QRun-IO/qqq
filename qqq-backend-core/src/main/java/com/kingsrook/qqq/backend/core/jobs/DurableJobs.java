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
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QTooManyRequestsException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.jobs.model.JobSemantics;
import com.kingsrook.qqq.backend.core.jobs.model.JobSourceType;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.plans.ChunkedProcessPlan;
import com.kingsrook.qqq.backend.core.jobs.plans.JobPlan;
import com.kingsrook.qqq.backend.core.jobs.plans.JobPlans;
import com.kingsrook.qqq.backend.core.jobs.plans.StepsJobPlan;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobFinalizer;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobSignals;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRefs;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.QUploadedFile;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertExtractStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.CHANNEL_ASSISTANT;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.DEFAULT_MAX_ATTEMPTS;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.DEFAULT_PRIORITY;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.PERMISSION_ADMIN;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_JOB;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** The durable jobs facade: launch any eligible process as a job, control it,
 ** and read its status. Every client (UI, API, assistant, scheduler) goes
 ** through here, so permissions, caps and the assistant confirmation gate are
 ** enforced in one place, by the server.
 *******************************************************************************/
public final class DurableJobs
{
   private static final QLogger LOG = QLogger.getLogger(DurableJobs.class);

   private static final Set<JobStatus> WAITING_STATUSES = EnumSet.of(JobStatus.QUEUED, JobStatus.AWAITING_CONFIRMATION);



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private DurableJobs()
   {
   }



   /*******************************************************************************
    ** The instance settings; throws when durable jobs are off.
    *******************************************************************************/
   public static DurableJobsMetaData settings() throws QException
   {
      DurableJobsMetaData settings = DurableJobsMetaData.of(QContext.getQInstance());
      if(settings == null)
      {
         throw (new QUserFacingException("Durable jobs are not enabled in this application."));
      }
      return (settings);
   }



   /*******************************************************************************
    ** Whether durable jobs are on for the current instance.
    *******************************************************************************/
   public static boolean isEnabled()
   {
      return (DurableJobsMetaData.of(QContext.getQInstance()) != null);
   }



   /*******************************************************************************
    ** Whether a process may run as a durable job: its own durable meta data
    ** decides first, then the instance's list of eligible names, then the default
    ** for its kind (streamed ETL whose extract is ExtractViaQueryStep or
    ** BulkInsertExtractStep - which covers the core bulk processes).
    *******************************************************************************/
   public static boolean isEligible(QProcessMetaData process)
   {
      DurableJobsMetaData settings = DurableJobsMetaData.of(QContext.getQInstance());
      if(settings == null || process == null)
      {
         return (false);
      }

      DurableProcessMetaData durable = DurableProcessMetaData.of(process);
      if(durable != null && durable.getEligible() != null)
      {
         return (durable.getEligible());
      }
      if(durable != null && Boolean.TRUE.equals(durable.getAlwaysDurable()))
      {
         return (true);
      }
      if(settings.getEligibleProcessNames().contains(process.getName()))
      {
         return (true);
      }
      return (getStreamedEtlExtractClassName(process) != null);
   }



   /*******************************************************************************
    ** For a streamed ETL process whose extract step is partitionable, the extract
    ** class name; else null.
    *******************************************************************************/
   public static String getStreamedEtlExtractClassName(QProcessMetaData process)
   {
      if(process.getStepList() == null)
      {
         return (null);
      }
      for(QStepMetaData step : process.getStepList())
      {
         if(step instanceof QBackendStepMetaData backendStep && backendStep.getInputMetaData() != null && backendStep.getInputMetaData().getFieldList() != null)
         {
            for(QFieldMetaData field : backendStep.getInputMetaData().getFieldList())
            {
               if(StreamedETLWithFrontendProcess.FIELD_EXTRACT_CODE.equals(field.getName()) && field.getDefaultValue() instanceof QCodeReference codeReference)
               {
                  String name = codeReference.getName();
                  if(ExtractViaQueryStep.class.getName().equals(name) || BulkInsertExtractStep.class.getName().equals(name))
                  {
                     return (name);
                  }
               }
            }
         }
      }
      return (null);
   }



   /*******************************************************************************
    ** Launch a process as a durable job, as the current session's user. Returns
    ** the job id. A launch from the assistant channel waits for confirmation.
    *******************************************************************************/
   public static Integer launch(LaunchRequest request) throws QException
   {
      DurableJobsMetaData settings = settings();
      QInstance           instance = QContext.getQInstance();
      QSession            session  = QContext.getQSession();
      String              userId   = DurableJobsSecurity.userId(session);
      if(!StringUtils.hasContent(userId))
      {
         throw (new QPermissionDeniedException("A durable job needs a signed-in user."));
      }

      QProcessMetaData process = instance.getProcess(request.getProcessName());
      if(process == null)
      {
         throw (new QNotFoundException("Process " + request.getProcessName() + " was not found."));
      }
      if(!isEligible(process))
      {
         throw (new QUserFacingException("Process " + process.getName() + " cannot run as a background job."));
      }

      RunProcessInput permissionInput = new RunProcessInput();
      permissionInput.setProcessName(process.getName());
      PermissionsHelper.checkProcessPermissionThrowing(permissionInput, process.getName(), request.getValues());

      DurableJobStore store     = new DurableJobStore(settings);
      boolean         assistant = isAssistant(session);

      ///////////////////////////////////////////////
      // caps: queued jobs per user (refuse = 429) //
      ///////////////////////////////////////////////
      long waiting = countJobs(userId, WAITING_STATUSES);
      if(waiting >= settings.getMaxQueuedJobsPerUser())
      {
         throw (new QTooManyRequestsException("You have " + waiting + " jobs waiting to run (the limit is " + settings.getMaxQueuedJobsPerUser() + "). Try again when some have finished."));
      }
      if(assistant)
      {
         long active = countJobs(userId, EnumSet.of(JobStatus.AWAITING_CONFIRMATION, JobStatus.QUEUED, JobStatus.PLANNING, JobStatus.RUNNING, JobStatus.PAUSING, JobStatus.PAUSED), CHANNEL_ASSISTANT);
         if(active >= settings.getAssistantMaxActiveJobs())
         {
            throw (new QTooManyRequestsException("The assistant already has " + active + " active jobs for you (the limit is " + settings.getAssistantMaxActiveJobs() + ")."));
         }
      }

      DurableProcessMetaData durable  = DurableProcessMetaData.of(process);
      int                    priority = request.getPriority() == null ? DEFAULT_PRIORITY : Math.max(0, Math.min(9, request.getPriority()));
      if(assistant)
      {
         priority = Math.min(priority, DEFAULT_PRIORITY);
      }

      boolean autoStart = !assistant || (settings.getAssistantAutoStartMaxRows() > 0 && request.getExpectedRows() != null && request.getExpectedRows() <= settings.getAssistantAutoStartMaxRows());
      JobStatus status = autoStart ? JobStatus.QUEUED : JobStatus.AWAITING_CONFIRMATION;

      JobSemantics semantics = durable != null && durable.getSemantics() != null ? durable.getSemantics() : JobSemantics.CHUNKED;
      JobSourceType  sourceType  = JobPlans.sourceTypeFor(process, request);
      JobIdempotency idempotency = durable != null && durable.getIdempotency() != null ? durable.getIdempotency() : defaultIdempotency(process, sourceType);

      Map<String, Object> source = new LinkedHashMap<>();
      if(request.getStartAfterStep() != null)
      {
         source.put(StepsJobPlan.SOURCE_START_AFTER_STEP, request.getStartAfterStep());
      }
      if(request.getFilter() != null)
      {
         source.put("filter", request.getFilter());
      }
      if(request.getRecordIds() != null)
      {
         source.put("recordIds", request.getRecordIds());
      }
      if(!request.getUploadRefs().isEmpty())
      {
         source.put("uploadRefs", request.getUploadRefs());
      }
      if(request.getProfileJson() != null)
      {
         source.put("profileJson", request.getProfileJson());
      }
      if(request.getRowSetSource() != null)
      {
         source.putAll(request.getRowSetSource());
      }

      Instant now = store.now();
      QRecord job = new QRecord()
         .withValue("label", StringUtils.hasContent(request.getLabel()) ? StringUtils.safeTruncate(request.getLabel(), 250) : Objects.requireNonNullElse(process.getLabel(), process.getName()))
         .withValue("processName", process.getName())
         .withValue("tableName", process.getTableName())
         .withValue("mode", request.getMode().name())
         .withValue("status", status.name())
         .withValue("statusReason", status == JobStatus.AWAITING_CONFIRMATION ? "Waiting for you to confirm it in the app." : null)
         .withValue("priority", priority)
         .withValue("userId", userId)
         .withValue("clientChannel", session.getClientChannel())
         .withValue("credentialScopeJson", JsonUtils.toJson(JobSessions.captureScope(session)))
         .withValue("sourceType", sourceType.name())
         .withValue("sourceJson", JsonUtils.toJson(source))
         .withValue("paramsJson", JsonUtils.toJson(paramValues(request.getValues())))
         .withValue("idempotency", idempotency.name())
         .withValue("semantics", semantics.name())
         .withValue("chunkSize", durable != null && durable.getChunkSize() != null ? durable.getChunkSize() : settings.getDefaultChunkSize())
         .withValue("maxParallelChunks", durable != null && durable.getMaxParallelChunks() != null ? durable.getMaxParallelChunks() : 1)
         .withValue("rowsTotal", request.getExpectedRows() == null ? null : request.getExpectedRows().intValue())
         .withValue("rowsDone", 0)
         .withValue("rowsOk", 0)
         .withValue("rowsFailed", 0)
         .withValue("chunksDone", 0)
         .withValue("chunksFailed", 0)
         .withValue("parentJobId", request.getParentJobId())
         .withValue("savedBulkLoadProfileId", request.getSavedBulkLoadProfileId())
         .withValue("notifyEmail", Boolean.TRUE.equals(request.getNotifyEmail()))
         .withValue("queuedAt", now)
         .withValue("leaseVersion", 0)
         .withValue("createDate", now)
         .withValue("modifyDate", now);

      ////////////////////////////////////////////////////////////////////
      // plan as the launching user (a plan may count or page the data) //
      ////////////////////////////////////////////////////////////////////
      JobPlan       plan   = JobPlans.forJob(job);
      List<QRecord> chunks = plan.planChunks(job);

      Integer jobId = store.insertJob(job, null);
      try
      {
         List<QRecord> chunkRecords = new ArrayList<>();
         for(QRecord chunk : chunks)
         {
            chunkRecords.add(chunk
               .withValue("jobId", jobId)
               .withValue("status", ChunkStatus.PENDING.name())
               .withValue("attempt", 0)
               .withValue("maxAttempts", DEFAULT_MAX_ATTEMPTS)
               .withValue("leaseVersion", 0)
               .withValue("createDate", now)
               .withValue("modifyDate", now));
         }
         store.insertChunks(chunkRecords, null);
         Map<String, Serializable> planned = new LinkedHashMap<>();
         planned.put("chunksTotal", chunkRecords.size());
         planned.put("rowsTotal", job.getValueInteger("rowsTotal"));
         store.updateJob(jobId, planned);
         for(String ref : request.getUploadRefs())
         {
            UploadRefs.extendForJob(ref, jobId);
         }
      }
      catch(Exception e)
      {
         store.setJobStatus(jobId, EnumSet.allOf(JobStatus.class), JobStatus.FAILED, Map.of("errorMessage", "Could not plan the job: " + e.getMessage(), "finishedAt", Instant.now()));
         throw (e instanceof QException qe ? qe : new QException("Could not plan the job", e));
      }

      store.recordEvent(jobId, null, JobEventType.LAUNCHED, userId, session.getClientChannel(), null,
         "Launched " + process.getName() + " (" + chunks.size() + (chunks.size() == 1 ? " chunk" : " chunks") + ")" + (status == JobStatus.AWAITING_CONFIRMATION ? "; awaiting confirmation" : ""));
      JobSignals.status(jobId, userId, status);
      if(chunks.isEmpty() && status == JobStatus.QUEUED)
      {
         //////////////////////////////////////////////////
         // nothing to do (e.g., an empty file): finish //
         //////////////////////////////////////////////////
         new JobFinalizer(store).evaluate(jobId);
      }
      LOG.info("Launched durable job", logPair("jobId", jobId), logPair("processName", process.getName()), logPair("userId", userId), logPair("status", status), logPair("chunks", chunks.size()));
      return (jobId);
   }



   /*******************************************************************************
    ** Take a control action on a job, as the current session. Owners may control
    ** their own jobs; jobs_admin may control any. From the assistant channel only
    ** pause and cancel are allowed. Returns a one-line result for summaries.
    *******************************************************************************/
   public static String control(Integer jobId, JobAction action) throws QException
   {
      DurableJobsMetaData settings = settings();
      DurableJobStore     store    = new DurableJobStore(settings);
      QSession            session  = QContext.getQSession();
      QRecord             job      = store.getJob(jobId);
      if(job == null)
      {
         throw (new QNotFoundException("Job " + jobId + " was not found."));
      }

      String actor = DurableJobsSecurity.userId(session);
      if(!(session.hasPermission(PERMISSION_ADMIN) || (actor != null && actor.equals(job.getValueString("userId")))))
      {
         throw (new QPermissionDeniedException("You can only control your own jobs."));
      }
      if(isAssistant(session) && !action.isAllowedFromAssistant())
      {
         throw (new QPermissionDeniedException("The assistant cannot " + action.getLabel().toLowerCase() + " a job; confirm it in the app."));
      }

      return (new JobControl(store, actor, session.getClientChannel()).apply(job, action));
   }



   /*******************************************************************************
    ** Status of a job the current session can see (owner or jobs_admin).
    *******************************************************************************/
   public static QRecord status(Integer jobId) throws QException
   {
      DurableJobStore store   = new DurableJobStore(settings());
      QSession        session = QContext.getQSession();
      QRecord         job     = store.getJob(jobId);
      String          actor   = DurableJobsSecurity.userId(session);
      if(job == null || !(session.hasPermission(PERMISSION_ADMIN) || (actor != null && actor.equals(job.getValueString("userId")))))
      {
         throw (new QNotFoundException("Job " + jobId + " was not found."));
      }
      return (job);
   }



   /*******************************************************************************
    ** Re-evaluate a job's status from its chunks (finish, pause or cancel it when
    ** its chunks allow). Safe to call any time, from any pod.
    *******************************************************************************/
   public static void evaluate(Integer jobId) throws QException
   {
      new JobFinalizer(new DurableJobStore(settings())).evaluate(jobId);
   }



   /*******************************************************************************
    ** The idempotency a job gets when its process does not say (spec 2.5):
    ** SAME_TRANSACTION (exactly once) when a chunked job writes to a table on the
    ** jobs tables' own transactional backend; NATURAL for bulk edit and delete
    ** (re-applying a chunk is harmless); else NONE (a reclaimed chunk is
    ** reviewed, never silently rerun).
    *******************************************************************************/
   static JobIdempotency defaultIdempotency(QProcessMetaData process, JobSourceType sourceType)
   {
      if(sourceType == JobSourceType.STEPS)
      {
         return (JobIdempotency.NONE);
      }

      String destination = String.valueOf(Objects.requireNonNullElse(ChunkedProcessPlan.inputDefault(process, StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE), Objects.requireNonNullElse(process.getTableName(), "")));
      QInstance instance = QContext.getQInstance();
      if(instance.getTable(destination) != null && Objects.equals(instance.getTable(destination).getBackendName(), instance.getTable(TABLE_JOB).getBackendName()) && supportsTransactions(destination))
      {
         return (JobIdempotency.SAME_TRANSACTION);
      }

      String name = process.getName();
      if(name.endsWith(".bulkEdit") || name.endsWith(".bulkDelete") || name.endsWith(".bulkEditWithFile"))
      {
         return (JobIdempotency.NATURAL);
      }
      return (JobIdempotency.NONE);
   }



   /*******************************************************************************
    ** Whether a table's backend opens real transactions (the base class is a
    ** no-op, e.g., the memory backend).
    *******************************************************************************/
   private static boolean supportsTransactions(String tableName)
   {
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(tableName)))
      {
         return (transaction != null && transaction.getClass() != QBackendTransaction.class);
      }
      catch(Exception e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static boolean isAssistant(QSession session)
   {
      return (session != null && CHANNEL_ASSISTANT.equals(session.getClientChannelName()));
   }



   /*******************************************************************************
    ** Values worth storing as job parameters: no uploaded-file bytes.
    *******************************************************************************/
   private static Map<String, Serializable> paramValues(Map<String, Serializable> values)
   {
      Map<String, Serializable> rs = new LinkedHashMap<>();
      if(values != null)
      {
         values.forEach((k, v) ->
         {
            if(!(v instanceof QUploadedFile))
            {
               rs.put(k, v);
            }
         });
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static long countJobs(String userId, Set<JobStatus> statuses) throws QException
   {
      return (countJobs(userId, statuses, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static long countJobs(String userId, Set<JobStatus> statuses, String channelPrefix) throws QException
   {
      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("userId", QCriteriaOperator.EQUALS, userId))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, statuses.stream().map(Enum::name).toList()));
      if(channelPrefix != null)
      {
         filter.withCriteria(new QFilterCriteria("clientChannel", QCriteriaOperator.STARTS_WITH, channelPrefix));
      }
      Integer count = SystemScope.call(() -> new CountAction().execute(new CountInput(TABLE_JOB).withFilter(filter)).getCount());
      return (count == null ? 0 : count);
   }

}
