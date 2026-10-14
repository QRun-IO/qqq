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
 ** Where the input of a job comes from (jobs_job.source_type).
 *******************************************************************************/
public enum JobSourceType implements PossibleValueEnum<String>
{
   STEPS("Process Steps"),
   TABLE_FILTER("Table Filter"),
   RECORD_IDS("Record IDs"),
   STORAGE_FILE("Uploaded File"),
   ROW_SET("Failed Row Set");

   public static final String NAME = "jobs_job_source_type";

   private final String label;



   /*******************************************************************************
    **
    *******************************************************************************/
   JobSourceType(String label)
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
}
