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


package com.kingsrook.qqq.backend.core.jobs.effects;


import java.util.List;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** A provider of job-level side effects, such as delivering a job's outputs
 ** (spec section 2.13). Registered in DurableJobsMetaData.completionEffectProviders.
 **
 ** When a job's chunks (and their effects) are done and it is about to complete,
 ** the finalizer asks every provider for effects, with the job record (including
 ** outputStorageJson) and the status it would end with (COMPLETED or
 ** COMPLETED_WITH_ERRORS). The effects are written to the outbox with key
 ** jobId:-1:rowKey:effectName and the job finishes after they are delivered.
 **
 ** The finalizer may ask more than once: return the same effects (same names and
 ** row keys) every time - keys already in the outbox are skipped. Runs as the
 ** system user; return an empty list when there is nothing to do.
 *******************************************************************************/
public interface JobCompletionEffects
{
   /*******************************************************************************
    ** The job-level effects for a job about to complete.
    *******************************************************************************/
   List<JobLevelEffect> effectsFor(QRecord job, JobStatus completingAs) throws Exception;
}
