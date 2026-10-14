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
 ** What a target's reconcile lookup (by idempotency key or natural id) found
 ** for an effect whose delivery outcome is unknown.
 **
 ** - FOUND: it was delivered: the effect becomes SENT.
 ** - NOT_FOUND: it definitely was not: the effect becomes PENDING (safe to send).
 ** - UNKNOWN: still can't tell: tried again, then queued for review.
 *******************************************************************************/
public record ReconcileResult(Outcome outcome, String providerRef, String message)
{
   /*******************************************************************************
    ** The three outcomes.
    *******************************************************************************/
   public enum Outcome
   {
      FOUND,
      NOT_FOUND,
      UNKNOWN
   }



   /*******************************************************************************
    ** It was delivered.
    *******************************************************************************/
   public static ReconcileResult found(String providerRef)
   {
      return (new ReconcileResult(Outcome.FOUND, providerRef, null));
   }



   /*******************************************************************************
    ** It definitely was not delivered.
    *******************************************************************************/
   public static ReconcileResult notFound()
   {
      return (new ReconcileResult(Outcome.NOT_FOUND, null, null));
   }



   /*******************************************************************************
    ** Can't tell.
    *******************************************************************************/
   public static ReconcileResult unknown(String message)
   {
      return (new ReconcileResult(Outcome.UNKNOWN, null, message));
   }
}
