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


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.jobs.runtime.InlineJobTracker;


/*******************************************************************************
 ** Tracking jobs for work that no durable plan runs (for example an assistant
 ** turn): a jobs_job row the owner sees in the Jobs tray, with the same live
 ** progress and log as a durable job. The work itself is not durable: if this
 ** pod goes away, the reaper fails the row with an honest message.
 **
 ** <pre>
 ** Integer jobId = JobTracking.start(new TrackedJobRequest().withUserId(user)
 **    .withLabel("Assistant answer").withProcessName("assistant.turn"));
 ** JobTracking.onCancel(jobId, () -> cancelTurn());
 ** JobTracking.progress(jobId).phases(...).phase("thinking").message("...");
 ** JobTracking.finish(jobId, JobStatus.COMPLETED, "Answered.");
 ** </pre>
 **
 ** Requires the durable jobs runtime to be running on this pod.
 *******************************************************************************/
public final class JobTracking
{

   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private JobTracking()
   {
   }



   /*******************************************************************************
    ** Start a RUNNING tracking job; returns its jobs_job id.
    *******************************************************************************/
   public static Integer start(TrackedJobRequest request) throws QException
   {
      return (tracker().startTracked(request.getProcessName(), request.getLabel(), request.getUserId(), request.getClientChannel(), request.getTableName(), request.getRecordId()));
   }



   /*******************************************************************************
    ** A progress reporter for the tracking job (phases, counts, message, log).
    ** Keep and reuse one reporter per job: it holds the phase state.
    *******************************************************************************/
   public static JobProgress progress(Integer jobId) throws QException
   {
      return (new JobProgress(runtime().getStore(), jobId));
   }



   /*******************************************************************************
    ** Record that the work is alive (progress flushes do this too).
    *******************************************************************************/
   public static void heartbeat(Integer jobId) throws QException
   {
      tracker().heartbeat(jobId);
   }



   /*******************************************************************************
    ** Finalize with COMPLETED, FAILED or CANCELLED (the first final status wins).
    *******************************************************************************/
   public static void finish(Integer jobId, JobStatus status, String message) throws QException
   {
      tracker().finishTracked(jobId, status, message);
   }



   /*******************************************************************************
    ** Register a listener run on this pod when a client cancels the job; it must
    ** stop the work and call finish(..., CANCELLED).
    *******************************************************************************/
   public static void onCancel(Integer jobId, Runnable listener) throws QException
   {
      tracker().onCancel(jobId, listener);
   }



   /*******************************************************************************
    ** The runtime's tracker.
    *******************************************************************************/
   private static InlineJobTracker tracker() throws QException
   {
      return (runtime().getInlineJobTracker());
   }



   /*******************************************************************************
    ** The running runtime, or an error explaining it is not running.
    *******************************************************************************/
   private static DurableJobsRuntime runtime() throws QException
   {
      DurableJobsRuntime runtime = DurableJobsRuntime.get(QContext.getQInstance());
      if(runtime == null)
      {
         throw (new QException("The durable jobs runtime is not running, so a tracking job cannot be started."));
      }
      return (runtime);
   }

}
