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
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.alerts.AlertLink;
import com.kingsrook.qqq.backend.core.alerts.AlertRecordRef;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.NotificationAudience;
import com.kingsrook.qqq.backend.core.alerts.NotificationCategory;
import com.kingsrook.qqq.backend.core.alerts.NotificationSeverity;
import com.kingsrook.qqq.backend.core.alerts.NotificationSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTables;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgressLifecycle;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.realtime.RealtimeBus;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMessage;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_JOB;


/*******************************************************************************
 ** Everything a job tells the outside world: realtime events (job.status on
 ** every change, job.progress throttled to one per second per job - counts
 ** only, never row values), alerts (jobs.job-failed, jobs.job-stalled) and the
 ** owner's completion notification. All best effort: a signal failure never
 ** fails a job. Core never depends on qbit-alerts; QAlerts routes to whatever
 ** sink the app installed.
 *******************************************************************************/
public final class JobSignals
{
   private static final QLogger LOG = QLogger.getLogger(JobSignals.class);

   public static final String ALERT_JOB_FAILED  = "jobs.job-failed";
   public static final String ALERT_JOB_STALLED = "jobs.job-stalled";
   public static final String ALERT_EFFECT_UNRESOLVED = "jobs.effect-unresolved";
   public static final String HEARTBEAT_REAPER  = "jobs.reaper";

   public static final String EVENT_STATUS   = "job.status";
   public static final String EVENT_PROGRESS = "job.progress";

   private static final Map<Integer, Long> LAST_PROGRESS = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private JobSignals()
   {
   }



   /*******************************************************************************
    ** Publish job.status to the job's record channel and the owner's user channel.
    *******************************************************************************/
   public static void status(Integer jobId, String userId, JobStatus status)
   {
      Map<String, Serializable> data = new LinkedHashMap<>();
      data.put("jobId", jobId);
      data.put("status", status.name());
      publish(jobId, userId, EVENT_STATUS, data);
   }



   /*******************************************************************************
    ** Publish job.progress (at most once per second per job unless forced).
    *******************************************************************************/
   public static void progress(QRecord job, boolean force)
   {
      Integer jobId = job.getValueInteger("id");
      long    now   = System.currentTimeMillis();
      Long    last  = LAST_PROGRESS.get(jobId);
      if(!force && last != null && now - last < 1000)
      {
         return;
      }
      LAST_PROGRESS.put(jobId, now);

      int     rowsDone    = intValue(job, "rowsDone");
      Integer rowsTotal   = job.getValueInteger("rowsTotal");
      Instant startedAt   = job.getValueInstant("startedAt");
      double  seconds     = startedAt == null ? 0 : Math.max(1, Duration.between(startedAt, Instant.now()).toSeconds());
      double  rowsPerSec  = seconds == 0 ? 0 : rowsDone / seconds;
      Integer etaSeconds  = (rowsTotal == null || rowsPerSec <= 0) ? null : (int) Math.max(0, (rowsTotal - rowsDone) / rowsPerSec);

      Map<String, Serializable> data = new LinkedHashMap<>();
      data.put("jobId", jobId);
      data.put("status", job.getValueString("status"));
      data.put("chunksDone", intValue(job, "chunksDone"));
      data.put("chunksTotal", intValue(job, "chunksTotal"));
      data.put("rowsDone", rowsDone);
      data.put("rowsTotal", rowsTotal);
      data.put("rowsFailed", intValue(job, "rowsFailed"));
      data.put("rowsPerSecond", Math.round(rowsPerSec * 10) / 10.0);
      data.put("etaSeconds", job.getValue("etaSeconds") == null ? etaSeconds : job.getValue("etaSeconds"));
      data.put("currentPhase", job.getValueString("currentPhase"));
      data.put("percent", JobProgressLifecycle.snapshot(job).getPercent());
      data.put("message", job.getValueString("progressMessage"));
      data.put("bytesDone", job.getValueLong("bytesDone"));
      data.put("bytesTotal", job.getValueLong("bytesTotal"));
      data.put("logSeq", job.getValueLong("logSeq"));
      publish(jobId, job.getValueString("userId"), EVENT_PROGRESS, data);
   }



   /*******************************************************************************
    ** A job reached a final (or review) status: notify its owner, raise or
    ** resolve the failure alert, publish status.
    *******************************************************************************/
   public static void finished(QRecord job, JobStatus status)
   {
      Integer jobId  = job.getValueInteger("id");
      String  userId = job.getValueString("userId");
      LAST_PROGRESS.remove(jobId);
      status(jobId, userId, status);
      progress(job.withValue("status", status.name()), true);

      String label    = Objects.requireNonNullElse(job.getValueString("label"), "Job " + jobId);
      String counts   = intValue(job, "rowsOk") + " rows ok, " + intValue(job, "rowsFailed") + " failed";
      String duration = duration(job);

      try
      {
         if(status == JobStatus.COMPLETED || status == JobStatus.COMPLETED_WITH_ERRORS || status == JobStatus.FAILED)
         {
            NotificationSignal signal = new NotificationSignal()
               .withTitle(label + switch(status)
               {
                  case COMPLETED -> " finished";
                  case COMPLETED_WITH_ERRORS -> " finished with errors";
                  default -> " failed";
               })
               .withBody(counts + (duration == null ? "" : ", in " + duration) + "."
                  + (job.getValueString("errorMessage") == null ? "" : " " + job.getValueString("errorMessage")))
               .withSeverity(status == JobStatus.COMPLETED ? NotificationSeverity.SUCCESS : status == JobStatus.FAILED ? NotificationSeverity.ERROR : NotificationSeverity.WARNING)
               .withCategory(NotificationCategory.JOB)
               .withSourceName("jobs")
               .withSourceKey("job:" + jobId + ":" + status.name())
               .withSourceTableName(TABLE_JOB)
               .withSourceRecordId(String.valueOf(jobId))
               .withLink(AlertLink.toRecord(TABLE_JOB, jobId, "Open the job"))
               .withAudience(NotificationAudience.user(userId));
            if(Boolean.TRUE.equals(job.getValueBoolean("notifyEmail")))
            {
               ///////////////////////////////////////////////////////////////////
               // per-job opt-in email: the alerts sink sends this one by email //
               // even without a standing preference for the JOB category.     //
               ///////////////////////////////////////////////////////////////////
               signal.getAttributes().put("emailRequested", "true");
            }
            QAlerts.notify(signal);
         }

         if(status == JobStatus.FAILED || status == JobStatus.NEEDS_REVIEW)
         {
            QAlerts.raise(new AlertSignal()
               .withAlertTypeName(ALERT_JOB_FAILED)
               .withDedupKey(String.valueOf(jobId))
               .withSeverity(AlertSeverity.ERROR)
               .withTitle(label + (status == JobStatus.FAILED ? " failed" : " needs review"))
               .withSummary(counts + ". " + Objects.requireNonNullElse(job.getValueString("errorMessage"), ""))
               .withRecordRef(AlertRecordRef.of(TABLE_JOB, jobId))
               .withLink(AlertLink.toRecord(TABLE_JOB, jobId, "Open the job")));
         }
         else if(status == JobStatus.COMPLETED || status == JobStatus.COMPLETED_WITH_ERRORS)
         {
            QAlerts.resolve(ALERT_JOB_FAILED, String.valueOf(jobId), "The job completed.");
            Integer parentJobId = job.getValueInteger("parentJobId");
            if(parentJobId != null)
            {
               QAlerts.resolve(ALERT_JOB_FAILED, String.valueOf(parentJobId), "A retry of this job completed (job " + jobId + ").");
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Could not signal a finished job", e);
      }
   }



   /*******************************************************************************
    ** Raise the stalled alert for a job.
    *******************************************************************************/
   public static void stalled(QRecord job, String why)
   {
      Integer jobId = job.getValueInteger("id");
      QAlerts.raise(new AlertSignal()
         .withAlertTypeName(ALERT_JOB_STALLED)
         .withDedupKey(String.valueOf(jobId))
         .withSeverity(AlertSeverity.WARNING)
         .withTitle(Objects.requireNonNullElse(job.getValueString("label"), "Job " + jobId) + " is stalled")
         .withSummary(why)
         .withRecordRef(AlertRecordRef.of(TABLE_JOB, jobId))
         .withLink(AlertLink.toRecord(TABLE_JOB, jobId, "Open the job")));
   }



   /*******************************************************************************
    ** Raise the alert for an outbox effect whose delivery could not be
    ** determined and that is queued for review (deduped per effect).
    *******************************************************************************/
   public static void effectUnresolved(QRecord effect)
   {
      Integer jobId    = effect.getValueInteger("jobId");
      Integer effectId = effect.getValueInteger("id");
      QAlerts.raise(new AlertSignal()
         .withAlertTypeName(ALERT_EFFECT_UNRESOLVED)
         .withDedupKey(String.valueOf(effectId))
         .withSeverity(AlertSeverity.WARNING)
         .withTitle("Effect " + effect.getValueString("effectName") + " of job " + jobId + " needs review")
         .withSummary("It may or may not have been delivered to " + effect.getValueString("targetName") + ", and the target's lookup could not tell. Mark it sent, resend it, or skip it.")
         .withRecordRef(AlertRecordRef.of(DurableJobsTables.TABLE_EFFECT, effectId))
         .withLink(AlertLink.toRecord(TABLE_JOB, jobId, "Open the job")));
   }



   /*******************************************************************************
    ** Resolve an effect's review alert.
    *******************************************************************************/
   public static void effectResolved(Integer effectId, String how)
   {
      QAlerts.resolve(ALERT_EFFECT_UNRESOLVED, String.valueOf(effectId), how);
   }



   /*******************************************************************************
    ** Resolve the stalled alert for a job (it moved again).
    *******************************************************************************/
   public static void unstalled(Integer jobId)
   {
      QAlerts.resolve(ALERT_JOB_STALLED, String.valueOf(jobId), "The job is moving again.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void publish(Integer jobId, String userId, String event, Map<String, Serializable> data)
   {
      try
      {
         if(QContext.getQInstance() == null)
         {
            return;
         }
         var bus = RealtimeBus.get(QContext.getQInstance());
         bus.publish(new RealtimeMessage().withChannel("record:" + TABLE_JOB + ":" + jobId).withEvent(event).withData(data).withId(UUID.randomUUID().toString()).withAt(Instant.now()));
         if(userId != null)
         {
            bus.publish(new RealtimeMessage().withChannel("user:" + userId).withEvent(event).withData(data).withId(UUID.randomUUID().toString()).withAt(Instant.now()));
         }
      }
      catch(Exception e)
      {
         LOG.debug("Could not publish a job event", e);
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
    **
    *******************************************************************************/
   private static String duration(QRecord job)
   {
      Instant started  = job.getValueInstant("startedAt");
      Instant finished = Objects.requireNonNullElse(job.getValueInstant("finishedAt"), Instant.now());
      if(started == null)
      {
         return (null);
      }
      long seconds = Math.max(0, Duration.between(started, finished).toSeconds());
      return (seconds < 120 ? seconds + "s" : seconds < 7200 ? (seconds / 60) + "m" : (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m");
   }

}
