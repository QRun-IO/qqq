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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Scheduled processes as durable jobs (spec section 2.4, "Scheduled").
 **
 ** A scheduled run becomes a job when durable jobs are on, the instance's
 ** scheduledRunsAsJobs is true (the default), and the process opted in: it is
 ** alwaysDurable, explicitly eligible in its durable meta data, or named in the
 ** instance's eligibleProcessNames. (Processes that are only eligible by kind,
 ** such as any streamed ETL, keep running inline when scheduled.)
 **
 ** The job is owned by the scheduler's user, or, when the scheduler runs as the
 ** system user, by DurableJobsMetaData.scheduledJobsUserId. Without an owner the
 ** run stays inline, as it was before durable jobs.
 *******************************************************************************/
public final class ScheduledJobs
{
   private static final QLogger LOG = QLogger.getLogger(ScheduledJobs.class);

   public static final String CHANNEL_SCHEDULER = "scheduler";

   private static final Set<String> WARNED_NO_OWNER = ConcurrentHashMap.newKeySet();



   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private ScheduledJobs()
   {
   }



   /*******************************************************************************
    ** Whether a scheduled run of this process becomes a job.
    *******************************************************************************/
   public static boolean runsAsJob(QProcessMetaData process)
   {
      DurableJobsMetaData settings = DurableJobsMetaData.of(QContext.getQInstance());
      if(settings == null || process == null || !Boolean.TRUE.equals(settings.getScheduledRunsAsJobs()))
      {
         return (false);
      }
      DurableProcessMetaData durable = DurableProcessMetaData.of(process);
      if(durable != null && Boolean.FALSE.equals(durable.getEligible()))
      {
         return (false);
      }
      boolean optedIn = (durable != null && (Boolean.TRUE.equals(durable.getAlwaysDurable()) || Boolean.TRUE.equals(durable.getEligible())))
         || settings.getEligibleProcessNames().contains(process.getName());
      return (optedIn && DurableJobs.isEligible(process));
   }



   /*******************************************************************************
    ** When a scheduled run of the process should be a job, launch it and return
    ** the job id. Null means: run it inline, as before. A launch that fails (for
    ** example, over the queue cap) throws; the caller logs it and skips this run,
    ** the next schedule tick tries again.
    *******************************************************************************/
   public static Integer launchIfDurable(QProcessMetaData process, Map<String, Serializable> values) throws QException
   {
      if(!runsAsJob(process))
      {
         return (null);
      }

      QInstance instance = QContext.getQInstance();
      QSession  owner    = ownerSession(instance);
      if(owner == null)
      {
         if(WARNED_NO_OWNER.add(process.getName()))
         {
            LOG.warn("Scheduled process runs inline: it would run as a durable job, but the scheduler runs as the system user and durable jobs have no scheduledJobsUserId",
               logPair("processName", process.getName()));
         }
         return (null);
      }
      if(!StringUtils.hasContent(owner.getClientChannel()))
      {
         owner.setClientChannel(CHANNEL_SCHEDULER);
      }

      Integer[] jobId = new Integer[1];
      QContext.<QException>withTemporaryContext(new CapturedContext(instance, owner), () ->
      {
         jobId[0] = DurableJobs.launch(new LaunchRequest(process.getName()).withValues(values));
      });
      LOG.info("Scheduled process launched as a durable job", logPair("processName", process.getName()), logPair("jobId", jobId[0]));
      return (jobId[0]);
   }



   /*******************************************************************************
    ** The session that owns a scheduled job: the scheduler's own user, else the
    ** configured scheduledJobsUserId's automated session, else null.
    *******************************************************************************/
   static QSession ownerSession(QInstance instance) throws QException
   {
      QSession current = QContext.getQSession();
      if(current != null && !(current instanceof QSystemUserSession) && StringUtils.hasContent(DurableJobsSecurity.userId(current)))
      {
         return (current);
      }

      String userId = DurableJobsMetaData.of(instance).getScheduledJobsUserId();
      if(!StringUtils.hasContent(userId))
      {
         return (null);
      }
      QSession session = new QAuthenticationModuleDispatcher().getQModule(instance.getAuthentication()).createAutomatedSessionForUser(instance, userId);
      if(session == null || session instanceof QSystemUserSession || !userId.equals(DurableJobsSecurity.userId(session)))
      {
         LOG.warn("Could not create the session of the scheduled jobs user", logPair("scheduledJobsUserId", userId));
         return (null);
      }
      return (session);
   }
}
