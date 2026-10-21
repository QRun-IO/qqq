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


import java.io.Serializable;
import java.util.Map;


/*******************************************************************************
 ** One effect, as handed to its target for delivery or reconciliation.
 **
 ** @param idempotencyKey jobId:chunkSeq:rowKey:effectName - send it as the
 **                       provider's Idempotency-Key, use it for object names,
 **                       upsert keys or the email Message-ID (see EffectKeys)
 ** @param effectName     the declared effect name
 ** @param effectType     how the effect is made safe
 ** @param jobId          the job (null outside a job)
 ** @param chunkSeq       the chunk's sequence (null outside a job)
 ** @param rowKey         the row the effect is for (caller-chosen, stable)
 ** @param payload        the effect's data (small values)
 ** @param payloadRef     a reference to a larger stored payload, or null
 ** @param attempt        1 for the first delivery attempt
 *******************************************************************************/
public record EffectRequest(String idempotencyKey, String effectName, JobEffectType effectType, Integer jobId, Integer chunkSeq, String rowKey,
                            Map<String, Serializable> payload, String payloadRef, int attempt)
{
}
