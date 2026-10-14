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


package com.kingsrook.qqq.backend.core.jobs.model;


import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;


/*******************************************************************************
 ** Status of a durable job (jobs_job.status).
 *******************************************************************************/
public enum JobStatus implements PossibleValueEnum<String>
{
   AWAITING_CONFIRMATION("Awaiting Confirmation"),
   QUEUED("Queued"),
   PLANNING("Planning"),
   RUNNING("Running"),
   PAUSING("Pausing"),
   PAUSED("Paused"),
   CANCELLING("Cancelling"),
   CANCELLED("Cancelled"),
   COMPLETED("Completed"),
   COMPLETED_WITH_ERRORS("Completed With Errors"),
   FAILED("Failed"),
   NEEDS_REVIEW("Needs Review");

   public static final String NAME = "jobs_job_status";

   private final String label;



   /*******************************************************************************
    **
    *******************************************************************************/
   JobStatus(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getPossibleValueId()
   {
      return (name());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getPossibleValueLabel()
   {
      return (label);
   }



   /*******************************************************************************
    ** True for statuses a job never leaves on its own.
    *******************************************************************************/
   public boolean isTerminal()
   {
      return (this == CANCELLED || this == COMPLETED || this == COMPLETED_WITH_ERRORS || this == FAILED);
   }



   /*******************************************************************************
    ** True for statuses in which workers may claim the job's chunks.
    *******************************************************************************/
   public boolean isClaimable()
   {
      return (this == QUEUED || this == RUNNING);
   }
}
