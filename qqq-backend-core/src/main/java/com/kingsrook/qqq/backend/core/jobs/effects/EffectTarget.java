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
 ** A system outside the job's database that receives effects (email, an HTTP
 ** API, object storage, another database). Registered by name in
 ** DurableJobsMetaData.effectTargets; the dispatcher calls it, never chunk code.
 **
 ** Implementations must use the request's idempotencyKey the way the target
 ** allows (EffectKeys): an Idempotency-Key header, a deterministic object name,
 ** an upsert on a natural key, a fixed email Message-ID.
 *******************************************************************************/
public interface EffectTarget
{
   /*******************************************************************************
    ** Deliver one effect. Report UNKNOWN (or throw) when the request may have
    ** reached the target but the answer was lost; never report RETRY or FAILED
    ** unless the target definitely did not act. Runs as the system user.
    *******************************************************************************/
   EffectResult deliver(EffectRequest request) throws Exception;



   /*******************************************************************************
    ** For an effect whose delivery outcome is unknown: look it up at the target
    ** by its idempotency key or natural id. The default can't tell, which sends
    ** the effect to review after the configured reconcile attempts.
    *******************************************************************************/
   default ReconcileResult reconcile(EffectRequest request) throws Exception
   {
      return (ReconcileResult.unknown("This target has no reconcile lookup."));
   }
}
