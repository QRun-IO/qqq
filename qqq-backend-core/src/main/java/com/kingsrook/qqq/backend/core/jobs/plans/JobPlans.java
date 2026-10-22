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


import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.DurableProcessMetaData;
import com.kingsrook.qqq.backend.core.jobs.model.JobSemantics;
import com.kingsrook.qqq.backend.core.jobs.model.JobSourceType;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertExtractStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;


/*******************************************************************************
 ** Chooses the plan for a job (spec section 2.4):
 ** - streamed ETL whose extract is ExtractViaQueryStep, launched over a filter
 **   or record ids: CHUNKED by key range (TABLE_FILTER / RECORD_IDS);
 ** - streamed ETL whose extract is BulkInsertExtractStep, launched with an
 **   upload: CHUNKED by file rows (STORAGE_FILE);
 ** - retry of failed rows: CHUNKED over just those rows (ROW_SET);
 ** - ATOMIC semantics, and every other process: the STEPS plan.
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
      DurableProcessMetaData durable = DurableProcessMetaData.of(process);
      if(durable != null && durable.getSemantics() == JobSemantics.ATOMIC)
      {
         return (JobSourceType.STEPS);
      }
      if(request.getRowSetSource() != null)
      {
         return (JobSourceType.ROW_SET);
      }

      String extract = DurableJobs.getStreamedEtlExtractClassName(process);
      if(BulkInsertExtractStep.class.getName().equals(extract) && !request.getUploadRefs().isEmpty())
      {
         return (JobSourceType.STORAGE_FILE);
      }
      if(ExtractViaQueryStep.class.getName().equals(extract))
      {
         if(request.getFilter() != null)
         {
            return (JobSourceType.TABLE_FILTER);
         }
         if(request.getRecordIds() != null && !request.getRecordIds().isEmpty())
         {
            return (JobSourceType.RECORD_IDS);
         }
      }
      return (JobSourceType.STEPS);
   }



   /*******************************************************************************
    ** The plan that runs a job.
    *******************************************************************************/
   public static JobPlan forJob(QRecord job)
   {
      JobSourceType sourceType = job.getValueString("sourceType") == null ? JobSourceType.STEPS : JobSourceType.valueOf(job.getValueString("sourceType"));
      return (sourceType == JobSourceType.STEPS ? new StepsJobPlan() : new ChunkedProcessPlan());
   }

}
