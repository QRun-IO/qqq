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


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.effects.JobCompletionEffects;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffects;
import com.kingsrook.qqq.backend.core.jobs.effects.JobLevelEffect;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Asks the registered JobCompletionEffects providers for a completing job's
 ** job-level effects and writes them to the outbox. A provider that throws is
 ** logged and holds the job open: it is asked again on the next evaluation, so
 ** an outage never skips a delivery (a provider that keeps failing leaves the
 ** job running, and stall detection raises jobs.job-stalled). It never fails
 ** the job.
 *******************************************************************************/
public final class JobCompletionEffectsRunner
{
   private static final QLogger LOG = QLogger.getLogger(JobCompletionEffectsRunner.class);



   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private JobCompletionEffectsRunner()
   {
   }



   /*******************************************************************************
    ** Emit the providers' effects for the job. Returns true when the job must
    ** wait: new outbox rows were written, or a provider failed (ask again).
    *******************************************************************************/
   public static boolean emit(QRecord job, JobStatus completingAs)
   {
      DurableJobsMetaData settings = DurableJobsMetaData.of(QContext.getQInstance());
      if(settings == null || settings.getCompletionEffectProviders() == null || settings.getCompletionEffectProviders().isEmpty())
      {
         return (false);
      }

      Integer jobId   = job.getValueInteger("id");
      int     written = 0;
      boolean failed  = false;
      for(QCodeReference code : settings.getCompletionEffectProviders())
      {
         try
         {
            JobCompletionEffects provider = QCodeLoader.getAdHoc(JobCompletionEffects.class, code);
            List<JobLevelEffect> effects  = provider.effectsFor(job, completingAs);
            for(JobLevelEffect effect : effects == null ? List.<JobLevelEffect>of() : effects)
            {
               written += JobEffects.emitForJob(jobId, effect) ? 1 : 0;
            }
         }
         catch(Exception e)
         {
            LOG.warn("A job completion effects provider failed; the job waits and it will be asked again", e, logPair("jobId", jobId), logPair("provider", code.getName()));
            failed = true;
         }
      }
      return (written > 0 || failed);
   }
}
