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
 ** One job-level effect a JobCompletionEffects provider asks for.
 **
 ** @param effectName  e.g. "deliver"
 ** @param targetName  a target registered in DurableJobsMetaData.effectTargets
 ** @param effectType  IDEMPOTENT_EXTERNAL or NON_IDEMPOTENT_EXTERNAL
 ** @param rowKey      a stable key for this effect within the job (e.g., the
 **                    destination id), part of the idempotency key
 ** @param payload     small values for the target
 ** @param payloadRef  a reference to a larger stored payload, or null
 *******************************************************************************/
public record JobLevelEffect(String effectName, String targetName, JobEffectType effectType, String rowKey, Map<String, Serializable> payload, String payloadRef)
{
}
