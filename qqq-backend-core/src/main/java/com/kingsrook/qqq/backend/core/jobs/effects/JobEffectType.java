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


import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;


/*******************************************************************************
 ** How a step's side effect is made safe across crashes and reruns (spec
 ** section 2.13).
 *******************************************************************************/
public enum JobEffectType implements PossibleValueEnum<String>
{
   /*******************************************************************************
    ** A write to the jobs' own database, made in the chunk's transaction: exactly
    ** once with SAME_TRANSACTION. Never goes through the outbox.
    *******************************************************************************/
   LOCAL_TX("Local Transaction"),

   /*******************************************************************************
    ** The target honors an idempotency key (or the write is naturally
    ** idempotent): an unknown outcome is safely resent with the same key.
    *******************************************************************************/
   IDEMPOTENT_EXTERNAL("Idempotent External"),

   /*******************************************************************************
    ** The target cannot dedupe: an unknown outcome is reconciled, or queued for
    ** review - never resent blindly.
    *******************************************************************************/
   NON_IDEMPOTENT_EXTERNAL("Non-Idempotent External");

   public static final String NAME = "jobs_effect_type";

   private final String label;



   /*******************************************************************************
    **
    *******************************************************************************/
   JobEffectType(String label)
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
    ** Whether the effect goes through the outbox (anything but LOCAL_TX).
    *******************************************************************************/
   public boolean isExternal()
   {
      return (this != LOCAL_TX);
   }
}
