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


import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobFinalizer;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobSignals;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Delivers outbox effects (spec section 2.13): claims due effects with leases,
 ** calls their targets as the system user, and records the outcome, fenced on
 ** the lease. UNKNOWN outcomes of non-idempotent effects are reconciled through
 ** the target's lookup, never resent blindly; after effectReconcileAttempts they
 ** are queued for review (needsReview, the jobs.effect-unresolved alert, and the
 ** job ends NEEDS_REVIEW). Every pod runs it.
 *******************************************************************************/
public class EffectDispatcher
{
   private static final QLogger LOG = QLogger.getLogger(EffectDispatcher.class);

   private final DurableJobsMetaData settings;
   private final EffectStore         effects;
   private final DurableJobStore     jobs;
   private final String              podId;
   private final FaultHook           faultHook;



   /*******************************************************************************
    ** A test seam: called around a delivery (the fault-injection tests halt the
    ** JVM there). Production code passes NONE.
    *******************************************************************************/
   public interface FaultHook
   {
      FaultHook NONE = (point, effect) ->
      {
      };



      /*******************************************************************************
       ** Called at a named point of a delivery.
       *******************************************************************************/
      void at(String point, QRecord effect);
   }



   public static final String POINT_BEFORE_DELIVER = "beforeDeliver";
   public static final String POINT_AFTER_DELIVER  = "afterDeliverBeforeMark";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public EffectDispatcher(DurableJobsMetaData settings, DurableJobStore jobs, String podId, FaultHook faultHook)
   {
      this.settings = settings;
      this.effects = new EffectStore(settings);
      this.jobs = jobs;
      this.podId = podId;
      this.faultHook = faultHook == null ? FaultHook.NONE : faultHook;
   }



   /*******************************************************************************
    ** One pass: reap dead dispatchers' leases, then claim and handle a batch.
    ** Returns how many effects it handled.
    *******************************************************************************/
   public int dispatchOnce() throws QException
   {
      Set<Integer> touched = new HashSet<>(effects.reapExpired());

      List<EffectLease> leases = effects.claimDue(podId, Math.max(1, settings.getEffectBatchSize()));
      for(EffectLease lease : leases)
      {
         try
         {
            handle(lease);
         }
         catch(Exception e)
         {
            LOG.warn("Could not handle a job effect", e, logPair("effectId", lease.effectId()), logPair("jobId", lease.jobId()));
         }
         touched.add(lease.jobId());
      }

      for(Integer jobId : touched)
      {
         if(jobId != null)
         {
            try
            {
               new JobFinalizer(jobs).evaluate(jobId);
            }
            catch(Exception e)
            {
               LOG.warn("Could not evaluate a job after its effects", e, logPair("jobId", jobId));
            }
         }
      }
      return (leases.size());
   }



   /*******************************************************************************
    ** Deliver, or reconcile, one claimed effect.
    *******************************************************************************/
   void handle(EffectLease lease) throws QException
   {
      QRecord effect = effects.get(lease.effectId());
      if(effect == null)
      {
         return;
      }
      JobEffectType type    = JobEffectType.valueOf(effect.getValueString("effectType"));
      EffectRequest request = toRequest(effect);
      EffectTarget  target;
      try
      {
         target = EffectTargets.get(effect.getValueString("targetName"));
      }
      catch(Exception e)
      {
         effects.markNotDelivered(lease, e.getMessage(), false);
         return;
      }

      if(lease.reconcile())
      {
         reconcile(lease, type, request, target, effect);
         return;
      }

      faultHook.at(POINT_BEFORE_DELIVER, effect);
      EffectResult result;
      try
      {
         result = target.deliver(request);
      }
      catch(Exception e)
      {
         result = EffectResult.unknown(e.getClass().getSimpleName() + ": " + e.getMessage());
      }
      faultHook.at(POINT_AFTER_DELIVER, effect);

      if(result == null)
      {
         result = EffectResult.unknown("The target returned no result.");
      }
      switch(result.outcome())
      {
         case SENT -> effects.markSent(lease, result.providerRef());
         case RETRY -> effects.markNotDelivered(lease, result.message(), true);
         case FAILED -> effects.markNotDelivered(lease, result.message(), false);
         case UNKNOWN -> effects.markUnknown(lease, type, result.message());
         default -> throw (new IllegalStateException("Unexpected outcome " + result.outcome()));
      }
   }



   /*******************************************************************************
    ** Look an UNKNOWN effect up at its target.
    *******************************************************************************/
   private void reconcile(EffectLease lease, JobEffectType type, EffectRequest request, EffectTarget target, QRecord effect) throws QException
   {
      ReconcileResult result;
      try
      {
         result = target.reconcile(request);
      }
      catch(Exception e)
      {
         result = ReconcileResult.unknown(e.getClass().getSimpleName() + ": " + e.getMessage());
      }
      if(result == null)
      {
         result = ReconcileResult.unknown("The target's reconcile returned no result.");
      }

      switch(result.outcome())
      {
         case FOUND -> effects.markSent(lease, result.providerRef());
         case NOT_FOUND -> effects.markNotFound(lease);
         case UNKNOWN ->
         {
            effects.markUnknown(lease, type, "Reconcile could not tell whether it was delivered: " + result.message());
            QRecord after = effects.get(lease.effectId());
            if(after != null && Boolean.TRUE.equals(after.getValueBoolean("needsReview")))
            {
               queuedForReview(after);
            }
         }
         default -> throw (new IllegalStateException("Unexpected outcome " + result.outcome()));
      }
   }



   /*******************************************************************************
    ** An effect that could not be resolved: alert (deduped by effect).
    *******************************************************************************/
   private void queuedForReview(QRecord effect)
   {
      LOG.warn("Job effect queued for review", logPair("effectId", effect.getValueInteger("id")), logPair("jobId", effect.getValueInteger("jobId")),
         logPair("effectName", effect.getValueString("effectName")));
      JobSignals.effectUnresolved(effect);
   }



   /*******************************************************************************
    ** The request a target sees.
    *******************************************************************************/
   static EffectRequest toRequest(QRecord effect) throws QException
   {
      Map<String, Serializable> payload = null;
      String                    json    = effect.getValueString("payloadJson");
      if(json != null && !json.isBlank())
      {
         try
         {
            payload = JsonUtils.toObject(json, new TypeReference<>()
            {
            });
         }
         catch(IOException e)
         {
            throw (new QException("Effect " + effect.getValueInteger("id") + " has an unreadable payload", e));
         }
      }
      Integer attempt = effect.getValueInteger("attempt");
      return (new EffectRequest(effect.getValueString("idempotencyKey"), effect.getValueString("effectName"),
         JobEffectType.valueOf(effect.getValueString("effectType")), effect.getValueInteger("jobId"), effect.getValueInteger("chunkSeq"),
         effect.getValueString("rowKey"), payload == null ? Map.of() : payload, effect.getValueString("payloadRef"), attempt == null ? 1 : attempt));
   }
}
