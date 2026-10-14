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
 ** A dispatcher's hold on one effect while it delivers or reconciles it.
 ** Every write that ends the hold is fenced on (id, leaseToken, leaseVersion).
 **
 ** @param effectId   the jobs_effect id
 ** @param jobId      the effect's job
 ** @param token      the lease token
 ** @param version    the lease version this claim set
 ** @param reconcile  true when the effect was UNKNOWN (reconcile, don't deliver)
 *******************************************************************************/
public record EffectLease(Integer effectId, Integer jobId, String token, int version, boolean reconcile)
{
}
