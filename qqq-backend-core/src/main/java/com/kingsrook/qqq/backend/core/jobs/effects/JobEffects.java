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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableProcessMetaData;
import com.kingsrook.qqq.backend.core.jobs.plans.JobChunkScope;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** How a process step causes a side effect outside the job's database (spec
 ** section 2.13). Never call the external system from a step; emit the effect:
 **
 **   JobEffects.emit(runBackendStepInput, "welcomeEmail", customerId, payload);
 **
 ** In a durable job, the effect is buffered in the chunk's scope and written to
 ** the jobs_effect outbox with the chunk's progress (its transaction, with
 ** SAME_TRANSACTION); the dispatcher delivers it. Its idempotency key is
 ** jobId:chunkSeq:rowKey:effectName, so a rerun chunk emits the same keys and
 ** nothing is doubled. Outside a job, it is delivered at once (with a fresh key;
 ** there is no chunk to rerun) and anything but SENT throws.
 *******************************************************************************/
public final class JobEffects
{
   /** The chunk sequence in the keys of job-level effects (not from a chunk). */
   public static final int JOB_LEVEL_CHUNK_SEQ = -1;



   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private JobEffects()
   {
   }



   /*******************************************************************************
    ** Emit a declared effect for a row (rowKey: a stable id of the row the effect
    ** is for, e.g. its primary key). Returns the idempotency key.
    *******************************************************************************/
   public static String emit(RunBackendStepInput input, String effectName, String rowKey, Map<String, Serializable> payload) throws QException
   {
      return (emit(input, effectName, rowKey, payload, null));
   }



   /*******************************************************************************
    ** Emit a declared effect, with a reference to a larger stored payload.
    *******************************************************************************/
   public static String emit(RunBackendStepInput input, String effectName, String rowKey, Map<String, Serializable> payload, String payloadRef) throws QException
   {
      Optional<JobChunkScope>     scope    = JobChunkScope.current().filter(s -> s.getIdentity() != null);
      String                      process  = scope.map(s -> s.getIdentity().processName()).orElse(input == null ? null : input.getProcessName());
      JobEffectDeclaration        declared = declaration(process, effectName);
      Map<String, Serializable>   values   = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);

      if(scope.isPresent())
      {
         JobChunkScope.ChunkIdentity identity = scope.get().getIdentity();
         String                      key      = EffectKeys.key(identity.jobId(), identity.chunkSeq(), rowKey, effectName);
         DurableJobsMetaData         settings = DurableJobsMetaData.of(QContext.getQInstance());
         scope.get().addEffect(new QRecord()
            .withValue("jobId", identity.jobId())
            .withValue("chunkId", identity.chunkId())
            .withValue("chunkSeq", identity.chunkSeq())
            .withValue("effectName", effectName)
            .withValue("effectType", declared.getEffectType().name())
            .withValue("targetName", declared.getTargetName())
            .withValue("rowKey", StringUtils.safeTruncate(rowKey, 250))
            .withValue("idempotencyKey", key)
            .withValue("payloadJson", JsonUtils.toJson(values))
            .withValue("payloadRef", payloadRef)
            .withValue("status", EffectStatus.PENDING.name())
            .withValue("needsReview", false)
            .withValue("attempt", 0)
            .withValue("reconcileAttempts", 0)
            .withValue("maxAttempts", settings == null ? 5 : settings.getEffectMaxAttempts())
            .withValue("leaseVersion", 0));
         return (key);
      }

      //////////////////////////////////////////////////////////////////////
      // not in a job: deliver now; this run's own key (no chunk to rerun) //
      //////////////////////////////////////////////////////////////////////
      String runId = UUID.randomUUID().toString();
      String key   = "inline:" + runId + ":" + (rowKey == null ? "" : rowKey) + ":" + effectName;
      EffectRequest request = new EffectRequest(key, effectName, declared.getEffectType(), null, null, rowKey, values, payloadRef, 1);
      EffectResult  result;
      try
      {
         result = EffectTargets.get(declared.getTargetName()).deliver(request);
      }
      catch(Exception e)
      {
         throw (new QException("Effect " + effectName + " for " + rowKey + " may or may not have been delivered: " + e.getMessage(), e));
      }
      if(result == null || result.outcome() != EffectResult.Outcome.SENT)
      {
         throw (new QException("Effect " + effectName + " for " + rowKey + " was not delivered (" + (result == null ? "no result" : result.outcome() + ": " + result.message()) + ")."));
      }
      return (key);
   }



   /*******************************************************************************
    ** Write a job-level effect (not from a chunk) to the outbox, with key
    ** jobId:-1:rowKey:effectName. Returns true when a new row was written,
    ** false when the key was already in the outbox. Used by the job completion
    ** hook (JobCompletionEffects); external effect types only.
    *******************************************************************************/
   public static boolean emitForJob(Integer jobId, JobLevelEffect effect) throws QException
   {
      if(effect.effectType() == null || !effect.effectType().isExternal())
      {
         throw (new QException("Job-level effect " + effect.effectName() + " must be IDEMPOTENT_EXTERNAL or NON_IDEMPOTENT_EXTERNAL."));
      }
      DurableJobsMetaData settings = DurableJobsMetaData.of(QContext.getQInstance());
      if(settings == null || !settings.getEffectTargets().containsKey(effect.targetName()))
      {
         throw (new QException("Job-level effect " + effect.effectName() + " names target " + effect.targetName() + ", which is not registered in DurableJobsMetaData.effectTargets."));
      }
      Map<String, Serializable> values = effect.payload() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(effect.payload());
      QRecord row = new QRecord()
         .withValue("jobId", jobId)
         .withValue("chunkSeq", JOB_LEVEL_CHUNK_SEQ)
         .withValue("effectName", effect.effectName())
         .withValue("effectType", effect.effectType().name())
         .withValue("targetName", effect.targetName())
         .withValue("rowKey", StringUtils.safeTruncate(effect.rowKey(), 250))
         .withValue("idempotencyKey", EffectKeys.key(jobId, JOB_LEVEL_CHUNK_SEQ, effect.rowKey(), effect.effectName()))
         .withValue("payloadJson", JsonUtils.toJson(values))
         .withValue("payloadRef", effect.payloadRef())
         .withValue("status", EffectStatus.PENDING.name())
         .withValue("needsReview", false)
         .withValue("attempt", 0)
         .withValue("reconcileAttempts", 0)
         .withValue("maxAttempts", settings.getEffectMaxAttempts())
         .withValue("leaseVersion", 0);
      return (new EffectStore(settings).insertEffects(List.of(row), null) > 0);
   }



   /*******************************************************************************
    ** The external effect declaration a process makes for the name.
    *******************************************************************************/
   private static JobEffectDeclaration declaration(String processName, String effectName) throws QException
   {
      QProcessMetaData       process  = processName == null ? null : QContext.getQInstance().getProcess(processName);
      DurableProcessMetaData durable  = DurableProcessMetaData.of(process);
      JobEffectDeclaration   declared = durable == null ? null : durable.getEffect(effectName);
      if(declared == null || declared.getEffectType() == null)
      {
         throw (new QException("Effect " + effectName + " is not declared by process " + processName + " (DurableProcessMetaData.withEffect)."));
      }
      if(!declared.getEffectType().isExternal())
      {
         throw (new QException("Effect " + effectName + " is LOCAL_TX: write it in the chunk's transaction instead of emitting it."));
      }
      return (declared);
   }
}
