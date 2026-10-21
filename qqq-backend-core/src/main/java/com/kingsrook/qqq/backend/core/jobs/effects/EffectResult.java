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


/*******************************************************************************
 ** What a target's delivery attempt achieved.
 **
 ** - SENT: delivered (providerRef: the provider's id, if any).
 ** - RETRY: definitely not delivered, try again later (e.g., 429/503 before the
 **   request was accepted).
 ** - FAILED: definitely not delivered, and retrying will not help (e.g., 400).
 ** - UNKNOWN: it may have been delivered (a timeout after sending, a dropped
 **   connection). Never treated as "not delivered".
 *******************************************************************************/
public record EffectResult(Outcome outcome, String providerRef, String message)
{
   /*******************************************************************************
    ** The four outcomes.
    *******************************************************************************/
   public enum Outcome
   {
      SENT,
      RETRY,
      FAILED,
      UNKNOWN
   }



   /*******************************************************************************
    ** Delivered.
    *******************************************************************************/
   public static EffectResult sent(String providerRef)
   {
      return (new EffectResult(Outcome.SENT, providerRef, null));
   }



   /*******************************************************************************
    ** Not delivered; retry later.
    *******************************************************************************/
   public static EffectResult retry(String message)
   {
      return (new EffectResult(Outcome.RETRY, null, message));
   }



   /*******************************************************************************
    ** Not delivered; do not retry.
    *******************************************************************************/
   public static EffectResult failed(String message)
   {
      return (new EffectResult(Outcome.FAILED, null, message));
   }



   /*******************************************************************************
    ** May or may not have been delivered.
    *******************************************************************************/
   public static EffectResult unknown(String message)
   {
      return (new EffectResult(Outcome.UNKNOWN, null, message));
   }
}
