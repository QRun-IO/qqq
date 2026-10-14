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
 ** Type of a durable job timeline event (jobs_event.type).
 *******************************************************************************/
public enum JobEventType implements PossibleValueEnum<String>
{
   LAUNCHED("Launched"),
   CONFIRMED("Confirmed"),
   STARTED("Started"),
   PAUSED("Paused"),
   RESUMED("Resumed"),
   CANCELLED("Cancelled"),
   RECLAIMED("Reclaimed"),
   RELEASED("Released"),
   RETRIED("Retried"),
   RESTARTED("Restarted"),
   STALLED("Stalled"),
   COMPLETED("Completed"),
   FAILED("Failed");

   public static final String NAME = "jobs_event_type";

   private final String label;



   /*******************************************************************************
    **
    *******************************************************************************/
   JobEventType(String label)
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
