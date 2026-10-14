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


package com.kingsrook.qqq.backend.core.jobs.plans;


import com.kingsrook.qqq.backend.core.jobs.model.JobSourceType;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** Chooses the plan for a job. Phase 1: every job runs the STEPS plan (or
 ** ATOMIC, by semantics). Phase 2 adds the chunked table and file partitioners
 ** for streamed ETL.
 *******************************************************************************/
public final class JobPlans
{

   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private JobPlans()
   {
   }



   /*******************************************************************************
    ** The source type a launch gets.
    *******************************************************************************/
   public static JobSourceType sourceTypeFor(QProcessMetaData process, LaunchRequest request)
   {
      return (JobSourceType.STEPS);
   }



   /*******************************************************************************
    ** The plan that runs a job.
    *******************************************************************************/
   public static JobPlan forJob(QRecord job)
   {
      return (new StepsJobPlan());
   }

}
