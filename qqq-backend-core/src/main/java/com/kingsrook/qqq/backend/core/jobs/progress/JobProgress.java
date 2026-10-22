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


import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobCallback;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.plans.ChunkContext;
import com.kingsrook.qqq.backend.core.jobs.plans.JobChunkScope;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;


/*******************************************************************************
 ** Shared reporting API for durable chunks and ordinary async process steps.
 *******************************************************************************/
public class JobProgress
{
   private final ChunkContext context;
   private final AsyncJobCallback callback;
   private final JobLogWriter writer;
   private final JobProgressMath math = new JobProgressMath();
   private final JobProgressSnapshot snapshot = new JobProgressSnapshot().withUnitLabel("rows");
   private final List<JobLogLine> tail = new ArrayList<>();
   private long sequence;



   /*******************************************************************************
    ** Construct an inline reporter (a null callback is a safe no-op destination).
    *******************************************************************************/
   public JobProgress(AsyncJobCallback callback)
   {
      this.context = null;
      this.callback = callback;
      this.writer = null;
   }



   /*******************************************************************************
    ** Construct the single reporter for a chunk attempt.
    *******************************************************************************/
   public JobProgress(ChunkContext context)
   {
      this.context = context;
      this.callback = null;
      this.writer = new JobLogWriter(context.getStore(), context.getLease().jobId(), context.getLease(), context::markLeaseLost);
   }



   /*******************************************************************************
    ** Construct a reporter for a tracking job (JobTracking): no lease, written in
    ** its own short transactions like a chunk's.
    *******************************************************************************/
   public JobProgress(DurableJobStore store, Integer jobId)
   {
      this.context = null;
      this.callback = null;
      this.writer = new JobLogWriter(store, jobId, null, null);
   }



   /*******************************************************************************
    ** Never null. Reuse the callback reporter outside durable execution.
    *******************************************************************************/
   public static JobProgress of(RunBackendStepInput input)
   {
      if(input != null && input.getAsyncJobCallback() instanceof JobProgressAsyncJobCallback adapter)
      {
         return (adapter.getProgress());
      }
      if(JobChunkScope.current().isPresent() && JobChunkScope.current().get().getProgress() != null)
      {
         return (JobChunkScope.current().get().getProgress());
      }
      return (input == null || input.getAsyncJobCallback() == null ? current() : input.getAsyncJobCallback().getJobProgress());
   }



   /*******************************************************************************
    ** Reporter on the running chunk, or a safe standalone reporter.
    *******************************************************************************/
   public static JobProgress current()
   {
      return (JobChunkScope.current().map(JobChunkScope::getProgress).orElseGet(() -> new JobProgress((AsyncJobCallback) null)));
   }



   /*******************************************************************************
    ** Declare phases for this attempt.
    *******************************************************************************/
   public synchronized JobProgress phases(JobPhase... phases)
   {
      snapshot.setPhases(new ArrayList<>(Arrays.asList(phases)));
      publish(false);
      return (this);
   }



   /*******************************************************************************
    ** Advance to a named phase; a phase transition flushes immediately.
    *******************************************************************************/
   public synchronized JobProgress phase(String name)
   {
      if(snapshot.getPhases() == null || snapshot.getPhases().stream().noneMatch(p -> name.equals(p.getName())))
      {
         throw (new IllegalArgumentException("Unknown job phase: " + name));
      }
      for(JobPhase phase : snapshot.getPhases())
      {
         if(phase.getStatus() == JobPhase.Status.RUNNING)
         {
            phase.setStatus(JobPhase.Status.DONE);
         }
         if(name.equals(phase.getName()))
         {
            phase.setStatus(JobPhase.Status.RUNNING);
         }
      }
      snapshot.setCurrentPhase(name);
      snapshot.setDone(null);
      snapshot.setTotal(null);
      snapshot.setBytesDone(null);
      snapshot.setBytesTotal(null);
      snapshot.setMessage(null);
      publish(true);
      return (this);
   }



   /*******************************************************************************
    ** Set current phase total and its display unit.
    *******************************************************************************/
   public synchronized JobProgress total(long total, String unitLabel)
   {
      snapshot.setTotal(total);
      snapshot.setUnitLabel(unitLabel);
      publish(false);
      return (this);
   }



   /*******************************************************************************
    ** Set current phase total in the existing unit.
    *******************************************************************************/
   public JobProgress total(long total)
   {
      return (total(total, snapshot.getUnitLabel()));
   }



   /*******************************************************************************
    ** Set done for the current phase.
    *******************************************************************************/
   public synchronized JobProgress done(long value)
   {
      snapshot.setDone(value);
      publish(false);
      return (this);
   }



   /*******************************************************************************
    ** Set bytesDone for the current phase.
    *******************************************************************************/
   public synchronized JobProgress bytesDone(long value)
   {
      snapshot.setBytesDone(value);
      publish(false);
      return (this);
   }



   /*******************************************************************************
    ** Set bytesTotal for the current phase.
    *******************************************************************************/
   public synchronized JobProgress bytesTotal(long value)
   {
      snapshot.setBytesTotal(value);
      publish(false);
      return (this);
   }



   /*******************************************************************************
    ** Set a redacted display message.
    *******************************************************************************/
   public synchronized JobProgress message(String message)
   {
      snapshot.setMessage(redact(message));
      publish(false);
      return (this);
   }



   /*******************************************************************************
    ** Map an existing async callback update to one consistent snapshot.
    *******************************************************************************/
   public synchronized void status(String message, Integer current, Integer total)
   {
      snapshot.setMessage(redact(message));
      snapshot.setDone(current == null ? null : current.longValue());
      snapshot.setTotal(total == null ? null : total.longValue());
      publish(false);
   }



   /*******************************************************************************
    ** Append a redacted DEBUG line.
    *******************************************************************************/
   public JobProgress debug(String message)
   {
      return (log(JobLogLevel.DEBUG, message));
   }



   /*******************************************************************************
    ** Append a redacted INFO line.
    *******************************************************************************/
   public JobProgress info(String message)
   {
      return (log(JobLogLevel.INFO, message));
   }



   /*******************************************************************************
    ** Append a redacted WARN line.
    *******************************************************************************/
   public JobProgress warn(String message)
   {
      return (log(JobLogLevel.WARN, message));
   }



   /*******************************************************************************
    ** Append a redacted ERROR line.
    *******************************************************************************/
   public JobProgress error(String message)
   {
      return (log(JobLogLevel.ERROR, message));
   }



   /*******************************************************************************
    ** Append a log line; warnings and errors force a flush.
    *******************************************************************************/
   public synchronized JobProgress log(JobLogLevel level, String message)
   {
      if(writer != null)
      {
         writer.append(level, snapshot.getCurrentPhase(), message);
      }
      else
      {
         tail.add(new JobLogLine().withId(++sequence).withLevel(level).withLoggedAt(Instant.now())
            .withPhase(redact(snapshot.getCurrentPhase())).withMessage(redact(message)));
         if(tail.size() > 200)
         {
            tail.removeFirst();
         }
      }
      publish(level == JobLogLevel.WARN || level == JobLogLevel.ERROR);
      return (this);
   }



   /*******************************************************************************
    ** Flush before the chunk completion fence changes, including on exceptions.
    *******************************************************************************/
   public synchronized void flush()
   {
      publish(true);
   }



   /*******************************************************************************
    ** Current snapshot for adapters and tests.
    *******************************************************************************/
   public JobProgressSnapshot getSnapshot()
   {
      return (snapshot);
   }



   /*******************************************************************************
    ** Derive and publish the rich status.
    *******************************************************************************/
   private void publish(boolean force)
   {
      math.update(snapshot, context == null ? null : context.getJob(), context == null ? 0 : context.getLease().attempt(), Instant.now());
      if(writer != null)
      {
         writer.flush(snapshot, force);
      }
      else if(callback != null)
      {
         callback.updateStatus(snapshot.getMessage());
         if(snapshot.getDone() != null && snapshot.getTotal() != null)
         {
            callback.updateStatus((int) Math.min(Integer.MAX_VALUE, snapshot.getDone()), (int) Math.min(Integer.MAX_VALUE, snapshot.getTotal()));
         }
         callback.updateProgress(snapshot, new ArrayList<>(tail));
      }
   }



   /*******************************************************************************
    ** Apply default and host rules to display text.
    *******************************************************************************/
   private String redact(String text)
   {
      return (JobLogWriter.redact(text, DurableJobsMetaData.of(QContext.getQInstance())));
   }
}
