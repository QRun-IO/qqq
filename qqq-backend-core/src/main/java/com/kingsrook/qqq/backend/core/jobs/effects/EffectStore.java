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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.ConditionalUpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.jobs.store.SkipLockedClaims;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_EFFECT;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** The jobs_effect outbox: write effects with a chunk's progress, claim due
 ** effects (FOR UPDATE SKIP LOCKED where supported, else conditional updates),
 ** record outcomes (fenced), and reap leases of dispatchers that died.
 *******************************************************************************/
public class EffectStore
{
   private static final QLogger LOG = QLogger.getLogger(EffectStore.class);

   private static final long MAX_BACKOFF_SECONDS = 3600;

   private final DurableJobsMetaData settings;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public EffectStore(DurableJobsMetaData settings)
   {
      this.settings = settings;
   }



   /*******************************************************************************
    ** The database clock.
    *******************************************************************************/
   public Instant now() throws QException
   {
      return (new ConditionalUpdateAction().databaseNow(TABLE_EFFECT));
   }



   /*******************************************************************************
    ** Write outbox rows (in the chunk's transaction when given). Keys already in
    ** the outbox are skipped, so a rerun chunk never doubles its effects.
    *******************************************************************************/
   public int insertEffects(List<QRecord> effects, QBackendTransaction transaction) throws QException
   {
      if(effects == null || effects.isEmpty())
      {
         return (0);
      }
      return (SystemScope.call(() ->
      {
         int inserted = 0;
         for(int i = 0; i < effects.size(); i += 500)
         {
            List<QRecord> page = effects.subList(i, Math.min(effects.size(), i + 500));
            List<String>  keys = page.stream().map(e -> e.getValueString("idempotencyKey")).toList();
            Set<String>   have = new HashSet<>();
            for(QRecord existing : new QueryAction().execute(new QueryInput(TABLE_EFFECT).withTransaction(transaction)
               .withFilter(new QQueryFilter(new QFilterCriteria("idempotencyKey", QCriteriaOperator.IN, keys)))).getRecords())
            {
               have.add(existing.getValueString("idempotencyKey"));
            }
            List<QRecord> fresh = new ArrayList<>();
            for(QRecord effect : page)
            {
               if(have.add(effect.getValueString("idempotencyKey")))
               {
                  fresh.add(effect);
               }
            }
            if(!fresh.isEmpty())
            {
               List<QRecord> stored = new InsertAction().execute(new InsertInput(TABLE_EFFECT).withRecords(fresh).withTransaction(transaction)).getRecords();
               for(QRecord record : stored)
               {
                  if(record.getErrors() != null && !record.getErrors().isEmpty())
                  {
                     throw (new QException("Could not write job effect " + record.getValueString("idempotencyKey") + ": " + record.getErrors()));
                  }
               }
               inserted += fresh.size();
            }
         }
         return (inserted);
      }));
   }



   /*******************************************************************************
    ** Claim up to batch effects that are due: PENDING (deliver) and UNKNOWN not
    ** yet sent to review (reconcile).
    *******************************************************************************/
   public List<EffectLease> claimDue(String podId, int batch) throws QException
   {
      Instant now = now();
      QQueryFilter due = new QQueryFilter()
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, List.of(EffectStatus.PENDING.name(), EffectStatus.UNKNOWN.name())))
         .withCriteria(new QFilterCriteria("needsReview", QCriteriaOperator.NOT_EQUALS, true))
         .withSubFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withCriteria(new QFilterCriteria("nextAttemptAt", QCriteriaOperator.IS_BLANK))
            .withCriteria(new QFilterCriteria("nextAttemptAt", QCriteriaOperator.LESS_THAN_OR_EQUALS, now)))
         .withOrderBy(new QFilterOrderBy("id"))
         .withLimit(batch);

      if(Boolean.TRUE.equals(settings.getUseSkipLockedClaims()) && SkipLockedClaims.isSupported(TABLE_EFFECT))
      {
         return (SkipLockedClaims.claim(TABLE_EFFECT, due, (candidates, transaction) ->
         {
            List<EffectLease> leases = new ArrayList<>();
            for(QRecord candidate : candidates)
            {
               claim(candidate, podId, now, transaction).ifPresent(leases::add);
            }
            return (leases);
         }));
      }

      List<EffectLease> leases = new ArrayList<>();
      for(QRecord candidate : SystemScope.call(() -> new QueryAction().execute(new QueryInput(TABLE_EFFECT).withFilter(due)).getRecords()))
      {
         claim(candidate, podId, now, null).ifPresent(leases::add);
      }
      return (leases);
   }



   /*******************************************************************************
    ** Claim one effect: status -> SENDING, a new token, version + 1. Fenced on
    ** the version and status read, so two dispatchers never both win.
    *******************************************************************************/
   private Optional<EffectLease> claim(QRecord effect, String podId, Instant now, QBackendTransaction transaction) throws QException
   {
      int    version  = intValue(effect, "leaseVersion");
      String status   = effect.getValueString("status");
      String token    = UUID.randomUUID().toString();
      boolean reconcile = EffectStatus.UNKNOWN.name().equals(status);

      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("status", EffectStatus.SENDING.name());
      values.put("leaseOwner", podId);
      values.put("leaseToken", token);
      values.put("leaseExpiresAt", now.plusSeconds(settings.getEffectLeaseTtlSeconds()));
      values.put("leaseVersion", version + 1);
      values.put(reconcile ? "reconcileAttempts" : "attempt", intValue(effect, reconcile ? "reconcileAttempts" : "attempt") + 1);
      values.put("modifyDate", now);

      int count = cas(values, new QQueryFilter()
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, effect.getValueInteger("id")))
         .withCriteria(new QFilterCriteria("leaseVersion", QCriteriaOperator.EQUALS, version))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, status)), transaction);
      if(count != 1)
      {
         return (Optional.empty());
      }
      return (Optional.of(new EffectLease(effect.getValueInteger("id"), effect.getValueInteger("jobId"), token, version + 1, reconcile)));
   }



   /*******************************************************************************
    ** Read an effect (as the system user).
    *******************************************************************************/
   public QRecord get(Integer effectId) throws QException
   {
      List<QRecord> records = query(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, effectId)));
      return (records.isEmpty() ? null : records.get(0));
   }



   /*******************************************************************************
    ** Query the outbox (as the system user).
    *******************************************************************************/
   public List<QRecord> query(QQueryFilter filter) throws QException
   {
      return (SystemScope.call(() -> new QueryAction().execute(new QueryInput(TABLE_EFFECT).withFilter(filter)).getRecords()));
   }



   /*******************************************************************************
    ** Delivered. Fenced: false when the lease was lost (another holder decides).
    *******************************************************************************/
   public boolean markSent(EffectLease lease, String providerRef) throws QException
   {
      Instant                   now    = now();
      Map<String, Serializable> values = release(now);
      values.put("status", EffectStatus.SENT.name());
      values.put("providerRef", StringUtils.safeTruncate(providerRef, 500));
      values.put("sentAt", now);
      values.put("lastError", null);
      values.put("needsReview", false);
      return (cas(values, fence(lease), null) == 1);
   }



   /*******************************************************************************
    ** Definitely not delivered: retry later with backoff, or FAILED when the
    ** attempts are used up (or retry is false).
    *******************************************************************************/
   public EffectStatus markNotDelivered(EffectLease lease, String message, boolean retry) throws QException
   {
      QRecord effect      = get(lease.effectId());
      int     attempt     = effect == null ? 1 : intValue(effect, "attempt");
      int     maxAttempts = effect == null || effect.getValueInteger("maxAttempts") == null ? settings.getEffectMaxAttempts() : effect.getValueInteger("maxAttempts");
      boolean again       = retry && attempt < maxAttempts;

      Instant                   now    = now();
      Map<String, Serializable> values = release(now);
      values.put("status", again ? EffectStatus.PENDING.name() : EffectStatus.FAILED.name());
      values.put("lastError", StringUtils.safeTruncate(message, 4000));
      values.put("nextAttemptAt", again ? now.plusSeconds(backoffSeconds(attempt)) : null);
      return (cas(values, fence(lease), null) == 1 ? (again ? EffectStatus.PENDING : EffectStatus.FAILED) : null);
   }



   /*******************************************************************************
    ** The outcome is unknown (it may have been delivered). An idempotent effect
    ** is simply due again (same key); a non-idempotent one becomes UNKNOWN, to be
    ** reconciled - or, after the reconcile attempts, queued for review.
    *******************************************************************************/
   public EffectStatus markUnknown(EffectLease lease, JobEffectType type, String message) throws QException
   {
      if(type == JobEffectType.IDEMPOTENT_EXTERNAL)
      {
         return (markNotDelivered(lease, "Outcome unknown; resending with the same idempotency key: " + message, true));
      }

      QRecord effect   = get(lease.effectId());
      int     attempts = effect == null ? 0 : intValue(effect, "reconcileAttempts");
      boolean review   = lease.reconcile() && attempts >= settings.getEffectReconcileAttempts();

      Instant                   now    = now();
      Map<String, Serializable> values = release(now);
      values.put("status", EffectStatus.UNKNOWN.name());
      values.put("needsReview", review);
      values.put("lastError", StringUtils.safeTruncate(message, 4000));
      values.put("nextAttemptAt", review ? null : now.plusSeconds(backoffSeconds(Math.max(1, attempts))));
      return (cas(values, fence(lease), null) == 1 ? EffectStatus.UNKNOWN : null);
   }



   /*******************************************************************************
    ** A reconcile lookup found the effect was definitely not delivered: due
    ** again (safe to send). Counts as a delivery attempt already made.
    *******************************************************************************/
   public boolean markNotFound(EffectLease lease) throws QException
   {
      Instant                   now    = now();
      Map<String, Serializable> values = release(now);
      values.put("status", EffectStatus.PENDING.name());
      values.put("lastError", "Reconciled: the target has no record of it; it will be sent.");
      values.put("nextAttemptAt", null);
      return (cas(values, fence(lease), null) == 1);
   }



   /*******************************************************************************
    ** Leases held past their expiry (the dispatcher died mid-delivery): the
    ** outcome is unknown. Idempotent effects go back to PENDING (resent with the
    ** same key); others to UNKNOWN (reconcile, never a blind resend). Returns the
    ** job ids touched.
    *******************************************************************************/
   public Set<Integer> reapExpired() throws QException
   {
      Instant       now     = now();
      Set<Integer>  jobIds  = new HashSet<>();
      List<QRecord> expired = query(new QQueryFilter()
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, EffectStatus.SENDING.name()))
         .withCriteria(new QFilterCriteria("leaseExpiresAt", QCriteriaOperator.LESS_THAN, now))
         .withLimit(200));
      for(QRecord effect : expired)
      {
         int     version    = intValue(effect, "leaseVersion");
         boolean idempotent = JobEffectType.IDEMPOTENT_EXTERNAL.name().equals(effect.getValueString("effectType"));

         Map<String, Serializable> values = release(now);
         values.put("leaseVersion", version + 1);
         values.put("status", idempotent ? EffectStatus.PENDING.name() : EffectStatus.UNKNOWN.name());
         values.put("nextAttemptAt", null);
         values.put("lastError", "The dispatcher (" + effect.getValueString("leaseOwner") + ") stopped while delivering this effect; "
            + (idempotent ? "resending with the same idempotency key." : "its outcome is unknown and will be reconciled."));

         int count = cas(values, new QQueryFilter()
            .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, effect.getValueInteger("id")))
            .withCriteria(new QFilterCriteria("leaseVersion", QCriteriaOperator.EQUALS, version))
            .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, EffectStatus.SENDING.name())), null);
         if(count == 1)
         {
            jobIds.add(effect.getValueInteger("jobId"));
            LOG.info("Reclaimed an effect from a stopped dispatcher", logPair("effectId", effect.getValueInteger("id")), logPair("jobId", effect.getValueInteger("jobId")),
               logPair("to", idempotent ? EffectStatus.PENDING : EffectStatus.UNKNOWN));
         }
      }
      return (jobIds);
   }



   /*******************************************************************************
    ** Review: an operator resolves an effect (not while a dispatcher holds it).
    ** to is SENT (it was delivered), PENDING (send it now) or FAILED (skip it).
    *******************************************************************************/
   public boolean resolve(Integer effectId, EffectStatus to, String note) throws QException
   {
      QRecord effect = get(effectId);
      if(effect == null || EffectStatus.SENDING.name().equals(effect.getValueString("status")) || EffectStatus.SENT.name().equals(effect.getValueString("status")))
      {
         return (false);
      }
      Instant                   now    = now();
      Map<String, Serializable> values = release(now);
      values.put("leaseVersion", intValue(effect, "leaseVersion") + 1);
      values.put("status", to.name());
      values.put("needsReview", false);
      values.put("nextAttemptAt", null);
      values.put("lastError", StringUtils.safeTruncate(note, 4000));
      if(to == EffectStatus.SENT)
      {
         values.put("sentAt", now);
      }
      if(to == EffectStatus.PENDING)
      {
         values.put("reconcileAttempts", 0);
      }
      return (cas(values, new QQueryFilter()
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, effectId))
         .withCriteria(new QFilterCriteria("leaseVersion", QCriteriaOperator.EQUALS, intValue(effect, "leaseVersion")))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, effect.getValueString("status"))), null) == 1);
   }



   /*******************************************************************************
    ** A job's effects, summarized for its finalizer.
    *******************************************************************************/
   public EffectCounts countsForJob(Integer jobId) throws QException
   {
      int    outstanding = 0;
      int    failed      = 0;
      int    review      = 0;
      int    sent        = 0;
      String firstReview = null;
      for(QRecord effect : query(new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId))))
      {
         EffectStatus status = EffectStatus.valueOf(effect.getValueString("status"));
         if(Boolean.TRUE.equals(effect.getValueBoolean("needsReview")))
         {
            review++;
            if(firstReview == null)
            {
               firstReview = effect.getValueString("effectName") + " " + Objects.requireNonNullElse(effect.getValueString("rowKey"), "");
            }
         }
         else if(status.isOutstanding())
         {
            outstanding++;
         }
         else if(status == EffectStatus.FAILED)
         {
            failed++;
         }
         else
         {
            sent++;
         }
      }
      return (new EffectCounts(outstanding, failed, review, sent, firstReview));
   }



   /*******************************************************************************
    ** Counts of a job's effects.
    *******************************************************************************/
   public record EffectCounts(int outstanding, int failed, int needsReview, int sent, String firstReview)
   {
   }



   /*******************************************************************************
    ** Values that end a hold.
    *******************************************************************************/
   private static Map<String, Serializable> release(Instant now)
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("leaseOwner", null);
      values.put("leaseToken", null);
      values.put("leaseExpiresAt", null);
      values.put("modifyDate", now);
      return (values);
   }



   /*******************************************************************************
    ** The fence of a lease: its id, token and version, still SENDING.
    *******************************************************************************/
   private static QQueryFilter fence(EffectLease lease)
   {
      return (new QQueryFilter()
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, lease.effectId()))
         .withCriteria(new QFilterCriteria("leaseToken", QCriteriaOperator.EQUALS, lease.token()))
         .withCriteria(new QFilterCriteria("leaseVersion", QCriteriaOperator.EQUALS, lease.version()))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, EffectStatus.SENDING.name())));
   }



   /*******************************************************************************
    ** Exponential backoff from effectRetryBaseSeconds, capped at an hour.
    *******************************************************************************/
   private long backoffSeconds(int attempt)
   {
      long base = Math.max(1, settings.getEffectRetryBaseSeconds());
      return (Math.min(MAX_BACKOFF_SECONDS, base << Math.min(20, Math.max(0, attempt - 1))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int cas(Map<String, Serializable> values, QQueryFilter filter, QBackendTransaction transaction) throws QException
   {
      return (SystemScope.call(() -> new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE_EFFECT)
         .withValues(new LinkedHashMap<>(values))
         .withFilter(filter)
         .withTransaction(transaction))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int intValue(QRecord record, String field)
   {
      Integer value = ValueUtils.getValueAsInteger(record.getValue(field));
      return (value == null ? 0 : value);
   }
}
