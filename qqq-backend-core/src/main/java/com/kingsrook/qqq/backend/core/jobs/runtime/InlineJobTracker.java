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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobState;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobStatus;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsSecurity;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.plans.StepsJobPlan;
import com.kingsrook.qqq.backend.core.jobs.progress.JobLogLine;
import com.kingsrook.qqq.backend.core.jobs.progress.JobLogWriter;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgressLifecycle;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgressMath;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgressSnapshot;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.QUploadedFile;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_JOB;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_WORKER;


/*******************************************************************************
 ** Per-pod mirrors of already-running inline processes. These have no chunks and
 ** cannot survive loss of the process's pod.
 *******************************************************************************/
public class InlineJobTracker
{
   public static final String RESTART_MESSAGE = "The server restarted while this ran in the foreground; it was not a durable job, so it could not be resumed. Run it again.";
   private static final QLogger LOG = QLogger.getLogger(InlineJobTracker.class);
   private final DurableJobStore store;
   private final String podId;
   private final Map<String, Tracking> tracking = new LinkedHashMap<>();
   private final Map<Integer, Tracked> tracked  = new LinkedHashMap<>();



   /*******************************************************************************
    ** A tracking job with no process behind it (JobTracking), owned by this pod.
    *******************************************************************************/
   private static class Tracked
   {
      private Runnable onCancel;
      private boolean  cancelFired;
   }

   /*******************************************************************************
    ** Cursor of the mirrored in-memory ring.
    *******************************************************************************/
   private static class Tracking
   {
      private Integer jobId;
      private long logCursor;
      private final JobProgressMath math = new JobProgressMath();
   }



   /*******************************************************************************
    ** Construct the tracker owned by the runtime.
    *******************************************************************************/
   public InlineJobTracker(DurableJobStore store, String podId)
   {
      this.store = store;
      this.podId = podId;
   }



   /*******************************************************************************
    ** Validate process ownership before creating or returning a tracking job.
    *******************************************************************************/
   public synchronized Integer track(String processName, String processUUID, String jobUUID) throws QException
   {
      return (trackRun(processName, processUUID, jobUUID, true, null));
   }



   /*******************************************************************************
    ** Start a fresh inline run with the captured process values. Fast completion
    ** is valid here: the tracking row is immediately finalized in that case.
    *******************************************************************************/
   public Integer runAgain(QRecord parent) throws QException
   {
      String processName = parent.getValueString("processName");
      String processUUID = UUID.randomUUID().toString();
      Map<String, Serializable> values = StepsJobPlan.readValues(parent.getValueString("paramsJson"));
      String uuid = new AsyncJobManager().withProcessUUID(processUUID).startJob(processName, callback ->
      {
         var input = new RunProcessInput()
            .withProcessName(processName).withProcessUUID(processUUID).withValues(new LinkedHashMap<>(values))
            .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP)
            .withAsyncJobCallback(callback);
         var output = new RunProcessAction().execute(input);
         if(output.getException().isPresent())
         {
            throw (output.getException().get());
         }
         return (Boolean.TRUE);
      });
      Integer id = trackRun(processName, processUUID, uuid, false, values);
      store.updateJob(id, Map.of("parentJobId", parent.getValueInteger("id"), "paramsJson", JsonUtils.toJson(values)));
      return (id);
   }



   /*******************************************************************************
    ** Attach one authorized run, optionally after a very fast rerun completed.
    *******************************************************************************/
   private synchronized Integer trackRun(String processName, String processUUID, String jobUUID, boolean requireRunning, Map<String, Serializable> capturedValues) throws QException
   {
      AsyncJobStatus status = new AsyncJobManager().getJobStatusForUser(jobUUID, processName, processUUID)
         .orElseThrow(() -> new QNotFoundException("Running process was not found."));
      if(tracking.containsKey(jobUUID))
      {
         return (tracking.get(jobUUID).jobId);
      }
      if(requireRunning && status.getState() != AsyncJobState.RUNNING)
      {
         throw (new QUserFacingException("Only a running inline process can be tracked."));
      }
      String owner = DurableJobsSecurity.userId(QContext.getQSession());
      if(owner == null)
      {
         throw (new QUserFacingException("A tracking job needs an owner."));
      }
      var process = QContext.getQInstance().getProcess(processName);
      Map<String, Serializable> values = new LinkedHashMap<>();
      if(capturedValues != null)
      {
         values.putAll(capturedValues);
      }
      else
      {
         var processState = RunProcessAction.getStateForUser(processUUID, processName);
         if(processState.isPresent() && processState.get().getValues() != null)
         {
            processState.get().getValues().forEach((key, value) ->
            {
               if(!(value instanceof QUploadedFile))
               {
                  values.put(key, value);
               }
            });
         }
      }
      Instant now = store.now();
      Tracking entry = new Tracking();
      entry.jobId = store.insertJob(new QRecord().withValue("processName", processName)
         .withValue("label", process == null || process.getLabel() == null ? processName : process.getLabel())
         .withValue("sourceType", "INLINE").withValue("status", "RUNNING").withValue("userId", owner)
         .withValue("leaseOwner", podId).withValue("startedAt", now).withValue("queuedAt", now)
         .withValue("createDate", now).withValue("lastProgressAt", now).withValue("mode", "EXECUTE")
         .withValue("paramsJson", JsonUtils.toJson(values))
         .withValue("sourceJson", JsonUtils.toJson(Map.of("processName", processName, "processUUID", processUUID, "jobUUID", jobUUID))), null);
      tracking.put(jobUUID, entry);
      mirror(jobUUID, entry, status);
      return (entry.jobId);
   }



   /*******************************************************************************
    ** Start a tracking job that no process backs (see JobTracking): RUNNING,
    ** owned by this pod, failed by the reaper if this pod goes away.
    *******************************************************************************/
   public synchronized Integer startTracked(String processName, String label, String owner, String clientChannel, String tableName, Serializable recordId) throws QException
   {
      if(owner == null)
      {
         throw (new QUserFacingException("A tracking job needs an owner."));
      }
      Instant                   now    = store.now();
      Map<String, Serializable> source = new LinkedHashMap<>();
      source.put("tracked", true);
      source.put("processName", processName);
      if(recordId != null)
      {
         source.put("recordId", recordId);
      }
      Integer jobId = store.insertJob(new QRecord().withValue("processName", processName)
         .withValue("label", label == null ? processName : label)
         .withValue("tableName", tableName)
         .withValue("clientChannel", clientChannel)
         .withValue("sourceType", "INLINE").withValue("status", "RUNNING").withValue("userId", owner)
         .withValue("leaseOwner", podId).withValue("startedAt", now).withValue("queuedAt", now)
         .withValue("createDate", now).withValue("lastProgressAt", now).withValue("mode", "EXECUTE")
         .withValue("sourceJson", JsonUtils.toJson(source)), null);
      tracked.put(jobId, new Tracked());
      return (jobId);
   }



   /*******************************************************************************
    ** Register the listener fired (once, on this pod) when the job is cancelled.
    *******************************************************************************/
   public synchronized void onCancel(Integer jobId, Runnable listener)
   {
      Tracked entry = tracked.get(jobId);
      if(entry != null)
      {
         entry.onCancel = listener;
      }
   }



   /*******************************************************************************
    ** Record that a tracking job is alive.
    *******************************************************************************/
   public void heartbeat(Integer jobId) throws QException
   {
      store.updateJob(jobId, Map.of("lastProgressAt", store.now()));
   }



   /*******************************************************************************
    ** Finalize a tracking job (first terminal status wins).
    *******************************************************************************/
   public synchronized void finishTracked(Integer jobId, JobStatus terminal, String message) throws QException
   {
      if(!terminal.isTerminal())
      {
         throw (new IllegalArgumentException("Not a terminal status: " + terminal));
      }
      tracked.remove(jobId);
      finish(jobId, terminal, message);
   }



   /*******************************************************************************
    ** Fire cancel listeners for tracking jobs that a client asked to cancel.
    *******************************************************************************/
   private void tickTracked()
   {
      for(Map.Entry<Integer, Tracked> entry : new ArrayList<>(tracked.entrySet()))
      {
         try
         {
            QRecord job = store.getJob(entry.getKey());
            if(job == null || JobStatus.valueOf(job.getValueString("status")).isTerminal())
            {
               tracked.remove(entry.getKey());
            }
            else if("CANCELLING".equals(job.getValueString("status")) && !entry.getValue().cancelFired && entry.getValue().onCancel != null)
            {
               entry.getValue().cancelFired = true;
               entry.getValue().onCancel.run();
            }
         }
         catch(Exception e)
         {
            LOG.warn("Could not check a tracking job", e);
         }
      }
   }



   /*******************************************************************************
    ** Mirror each active process once per runtime tick.
    *******************************************************************************/
   public synchronized void tick()
   {
      tickTracked();
      for(String uuid : new ArrayList<>(tracking.keySet()))
      {
         try
         {
            AsyncJobStatus status = new AsyncJobManager().getJobStatus(uuid).orElse(null);
            Tracking entry = tracking.get(uuid);
            if(status == null)
            {
               finish(entry.jobId, JobStatus.FAILED, "The foreground process status is no longer available.");
               tracking.remove(uuid);
            }
            else
            {
               mirror(uuid, entry, status);
            }
         }
         catch(Exception e)
         {
            LOG.warn("Could not mirror inline job progress", e);
         }
      }
   }



   /*******************************************************************************
    ** Persist a snapshot and only the new ring entries.
    *******************************************************************************/
   private void mirror(String uuid, Tracking entry, AsyncJobStatus status) throws QException
   {
      QRecord job = store.getJob(entry.jobId);
      if(job == null || JobStatus.valueOf(job.getValueString("status")).isTerminal())
      {
         tracking.remove(uuid);
         return;
      }
      boolean cancelling = "CANCELLING".equals(job.getValueString("status"));
      if(cancelling)
      {
         new AsyncJobManager().cancelJob(uuid);
      }
      JobProgressSnapshot snapshot = status.getProgress();
      if(snapshot == null)
      {
         snapshot = new JobProgressSnapshot().withUnitLabel("rows")
            .withMessage(JobLogWriter.redact(status.getMessage(), store.getSettings()))
            .withDone(status.getCurrent() == null ? null : status.getCurrent().longValue())
            .withTotal(status.getTotal() == null ? null : status.getTotal().longValue());
         entry.math.update(snapshot, job, 0, Instant.now());
      }
      JobLogWriter writer = new JobLogWriter(store, entry.jobId, null, null);
      long nextCursor = entry.logCursor;
      if(status.getLogTail() != null)
      {
         for(JobLogLine line : new ArrayList<>(status.getLogTail()))
         {
            if(line.getId() != null && line.getId() > entry.logCursor)
            {
               writer.append(line.getLevel(), line.getPhase(), line.getMessage());
               nextCursor = Math.max(nextCursor, line.getId());
            }
         }
      }
      if(writer.flush(snapshot, true))
      {
         entry.logCursor = nextCursor;
      }
      Map<String, Serializable> counts = new LinkedHashMap<>();
      counts.put("rowsDone", status.getCurrent());
      counts.put("rowsTotal", status.getTotal());
      store.updateJob(entry.jobId, counts);
      if(status.getState() != AsyncJobState.RUNNING)
      {
         JobStatus terminal = cancelling ? JobStatus.CANCELLED : status.getState() == AsyncJobState.COMPLETE ? JobStatus.COMPLETED : JobStatus.FAILED;
         finish(entry.jobId, terminal, terminal == JobStatus.FAILED ? "Foreground process failed." : "Foreground process " + terminal.getPossibleValueLabel().toLowerCase() + ".");
         tracking.remove(uuid);
      }
   }



   /*******************************************************************************
    ** Only the winning status transition appends a final line.
    *******************************************************************************/
   private void finish(Integer jobId, JobStatus terminal, String message) throws QException
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("finishedAt", store.now());
      values.put("statusReason", message);
      values.putAll(JobProgressLifecycle.terminalValues(store, store.getJob(jobId), terminal, message));
      if(store.setJobStatus(jobId, EnumSet.of(JobStatus.RUNNING, JobStatus.CANCELLING), terminal,
         values))
      {
         JobProgressLifecycle.finished(store, store.getJob(jobId), terminal, message);
         JobSignals.finished(store.getJob(jobId), terminal);
      }
   }



   /*******************************************************************************
    ** Fail inline jobs whose worker heartbeat has disappeared.
    *******************************************************************************/
   public void reap() throws QException
   {
      Instant cutoff = store.now().minusSeconds(Math.max(60, store.getSettings().getLeaseTtlSeconds()));
      for(QRecord job : store.query(TABLE_JOB, new QQueryFilter()
         .withCriteria(new QFilterCriteria("sourceType", QCriteriaOperator.EQUALS, "INLINE"))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, List.of("RUNNING", "CANCELLING")))))
      {
         List<QRecord> workers = store.query(TABLE_WORKER, new QQueryFilter()
            .withCriteria(new QFilterCriteria("podId", QCriteriaOperator.EQUALS, job.getValueString("leaseOwner")))
            .withCriteria(new QFilterCriteria("heartbeatAt", QCriteriaOperator.GREATER_THAN_OR_EQUALS, cutoff)));
         if(workers.isEmpty())
         {
            finish(job.getValueInteger("id"), JobStatus.FAILED, RESTART_MESSAGE);
         }
      }
   }
}
