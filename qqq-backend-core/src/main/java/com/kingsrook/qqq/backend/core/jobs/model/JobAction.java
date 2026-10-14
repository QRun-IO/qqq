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


/*******************************************************************************
 ** Control actions on a durable job (spec section 2.7).
 *******************************************************************************/
public enum JobAction
{
   START("Start", "started"),
   PAUSE("Pause", "paused"),
   RESUME("Resume", "resumed"),
   CANCEL("Cancel", "cancelled"),
   RETRY_FAILED("Retry Failed", "set to retry"),
   RESTART("Restart", "restarted"),
   RUN_AGAIN("Run Again", "run again");

   private final String label;
   private final String pastTense;



   /*******************************************************************************
    **
    *******************************************************************************/
   JobAction(String label, String pastTense)
   {
      this.label = label;
      this.pastTense = pastTense;
   }



   /*******************************************************************************
    ** Whether the assistant channel may take this action itself. It may pause or
    ** cancel; everything that starts work needs a person (spec section 3.5).
    *******************************************************************************/
   public boolean isAllowedFromAssistant()
   {
      return (this == PAUSE || this == CANCEL);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Getter for pastTense
    *******************************************************************************/
   public String getPastTense()
   {
      return (this.pastTense);
   }

}
