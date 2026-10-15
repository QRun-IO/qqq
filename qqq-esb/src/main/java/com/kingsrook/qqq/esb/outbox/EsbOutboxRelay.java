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

package com.kingsrook.qqq.esb.outbox;


import java.io.Serializable;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.store.SkipLockedClaims;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbUnparseableMessageException;
import com.kingsrook.qqq.esb.model.EsbDedupeConfig;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbOutboxConfig;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.publish.EsbPublisher;
import com.kingsrook.qqq.esb.stats.EsbStats;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Publishes the ESB outbox's PENDING rows, and purges old SENT outbox rows and
 * old processed-message rows.  Runs on every node, on one daemon thread (so it
 * never blocks JVM exit); EsbRuntimeService starts and stops it.
 *
 * Each pass, for each destination with PENDING rows:
 * - Claim: in one short transaction, lock the destination's first batchSize
 *   PENDING rows (SELECT ... FOR UPDATE SKIP LOCKED, where the backend has it
 *   - see SkipLockedClaims) and lease them to this node (leaseOwner,
 *   leaseExpiresAt).  Only the destination's head-of-line rows are claimed:
 *   if the oldest PENDING row is locked or leased by another node, or waiting
 *   out a retry backoff, the destination is skipped this pass - so each
 *   destination's rows are sent in order (by id), by one node at a time.
 * - Send the claimed rows in one broker transaction, each with its row's uuid
 *   as its qqq_message_id, and mark them SENT.
 * - On failure, release the lease.  If the provider is connected, the failure
 *   counts as an attempt on the head row (which is then retried alone, after
 *   a doubling backoff, and goes DEAD after maxAttempts); if the broker is
 *   unreachable it doesn't count, and the rows wait for it.
 *
 * Delivery is at-least-once: a send whose commit reached the broker, but
 * whose rows weren't marked SENT (crash, lost DB connection, expired lease),
 * is sent again with the same message id - consumers dedupe on it.
 *
 * Backends without SKIP LOCKED rely on the leases alone, which is safe for one
 * relay at a time; run more than one node only on a backend that has it
 * (PostgreSQL).
 *******************************************************************************/
public final class EsbOutboxRelay
{
   private static final QLogger LOG = QLogger.getLogger(EsbOutboxRelay.class);

   static final Long STOP_TIMEOUT_MS = 5000L;

   private static final EsbOutboxRelay INSTANCE = new EsbOutboxRelay();

   private final String       nodeId           = makeNodeId();
   private final Object       wakeLock         = new Object();
   private final Set<String>  failingProviders = ConcurrentHashMap.newKeySet();
   private       boolean      wakeRequested    = false;
   private       long         lastPurgeMs      = 0L;

   //////////////////////////////////////////
   // guarded by this (start/stop/running) //
   //////////////////////////////////////////
   private Thread    thread;
   private QInstance qInstance;
   private volatile boolean running = false;



   /*******************************************************************************
    ** Singleton - use getInstance.
    *******************************************************************************/
   private EsbOutboxRelay()
   {
   }



   /*******************************************************************************
    ** Get the singleton instance.
    *******************************************************************************/
   public static EsbOutboxRelay getInstance()
   {
      return (INSTANCE);
   }



   /*******************************************************************************
    ** Start the relay thread, if the instance has the outbox or dedupe enabled
    ** (and it isn't already running).  Doesn't block.
    *******************************************************************************/
   public synchronized void start(QInstance qInstance)
   {
      EsbInstanceMetaData esbInstanceMetaData = (qInstance == null) ? null : QSupplementalInstanceMetaData.of(qInstance, EsbInstanceMetaData.NAME);
      if(esbInstanceMetaData == null || (!esbInstanceMetaData.isOutboxEnabled() && !esbInstanceMetaData.isDedupeEnabled()))
      {
         return;
      }
      if(running)
      {
         return;
      }

      this.qInstance = qInstance;
      this.running = true;
      this.lastPurgeMs = 0L;
      this.thread = Thread.ofPlatform()
         .daemon()
         .name("qqq-esb-outbox-relay")
         .start(this::loop);
      LOG.info("Started the ESB outbox relay", logPair("nodeId", nodeId), logPair("outbox", esbInstanceMetaData.isOutboxEnabled()), logPair("dedupe", esbInstanceMetaData.isDedupeEnabled()));
   }



   /*******************************************************************************
    ** Stop the relay thread, waiting (up to 5 s) for its current pass to end.
    ** Rows it had claimed but not sent are released when their lease expires.
    ** No-op if not running.
    *******************************************************************************/
   public void stop()
   {
      Thread threadToStop;
      synchronized(this)
      {
         if(!running)
         {
            return;
         }
         running = false;
         threadToStop = thread;
         thread = null;
      }

      nudge();
      threadToStop.interrupt();
      try
      {
         if(!threadToStop.join(Duration.ofMillis(STOP_TIMEOUT_MS)))
         {
            LOG.warn("The ESB outbox relay did not stop in time; it will stop when its current send ends");
         }
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
      LOG.info("Stopped the ESB outbox relay");
   }



   /*******************************************************************************
    ** Whether the relay thread is running.
    *******************************************************************************/
   public boolean isRunning()
   {
      return (running);
   }



   /*******************************************************************************
    ** Wake the relay now (e.g., new outbox rows were committed on this node),
    ** rather than at its next poll.
    *******************************************************************************/
   public void nudge()
   {
      synchronized(wakeLock)
      {
         wakeRequested = true;
         wakeLock.notifyAll();
      }
   }



   /*******************************************************************************
    ** This node's lease owner id.
    *******************************************************************************/
   public String getNodeId()
   {
      return (nodeId);
   }



   /*******************************************************************************
    ** The relay thread's body: relay until a pass sends nothing, then wait for
    ** a nudge or the poll interval; purge every purgeIntervalMs.
    *******************************************************************************/
   private void loop()
   {
      try
      {
         QContext.init(qInstance, new QSystemUserSession());
         while(running)
         {
            int sent = 0;
            try
            {
               sent = relayOnce();
               if(System.currentTimeMillis() - lastPurgeMs >= getPurgeIntervalMs())
               {
                  lastPurgeMs = System.currentTimeMillis();
                  purgeOnce();
               }
            }
            catch(Exception | LinkageError e)
            {
               if(running)
               {
                  LOG.warn("Error in the ESB outbox relay; will try again", e);
               }
            }

            if(sent == 0 && running)
            {
               awaitWake(getPollIntervalMs());
            }
         }
      }
      finally
      {
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Wait for a nudge, or up to maxWaitMs.
    *******************************************************************************/
   private void awaitWake(long maxWaitMs)
   {
      synchronized(wakeLock)
      {
         long deadline = System.currentTimeMillis() + maxWaitMs;
         while(!wakeRequested && running)
         {
            long remaining = deadline - System.currentTimeMillis();
            if(remaining <= 0)
            {
               break;
            }
            try
            {
               wakeLock.wait(remaining);
            }
            catch(InterruptedException e)
            {
               return;
            }
         }
         wakeRequested = false;
      }
   }



   /*******************************************************************************
    ** One pass over every destination with PENDING rows (in the QContext's
    ** instance).  Returns how many rows were sent.  Does nothing (returns 0) if
    ** the outbox is not enabled.
    *******************************************************************************/
   public int relayOnce() throws QException
   {
      EsbOutboxConfig config = EsbOutbox.getEnabledConfig(QContext.getQInstance());
      if(config == null)
      {
         return (0);
      }

      int sent = 0;
      for(String destinationName : findPendingDestinations(config))
      {
         sent += relayDestination(config, destinationName);
      }
      return (sent);
   }



   /*******************************************************************************
    ** Purge SENT outbox rows, and processed-message rows, older than their
    ** retention (each, if enabled).  Returns how many rows were deleted.
    *******************************************************************************/
   public int purgeOnce() throws QException
   {
      QInstance           instance            = QContext.getQInstance();
      EsbInstanceMetaData esbInstanceMetaData = QSupplementalInstanceMetaData.of(instance, EsbInstanceMetaData.NAME);
      if(esbInstanceMetaData == null)
      {
         return (0);
      }

      int deleted = 0;
      if(esbInstanceMetaData.isOutboxEnabled())
      {
         EsbOutboxConfig config = esbInstanceMetaData.getOutbox();
         Instant         cutoff = Instant.now().minus(config.getRetentionDays(), ChronoUnit.DAYS);
         deleted += new DeleteAction().execute(new DeleteInput(config.getTableName())
            .withQueryFilter(new QQueryFilter(
               new QFilterCriteria(EsbOutboxTable.FIELD_STATUS, QCriteriaOperator.EQUALS, EsbOutboxTable.STATUS_SENT),
               new QFilterCriteria(EsbOutboxTable.FIELD_SENT_AT, QCriteriaOperator.LESS_THAN, cutoff)))
            .withOmitRecordChangeEvents(true)
            .withOmitDmlAudit(true)).getDeletedRecordCount();
      }
      if(esbInstanceMetaData.isDedupeEnabled())
      {
         EsbDedupeConfig config = esbInstanceMetaData.getDedupe();
         Instant         cutoff = Instant.now().minus(config.getRetentionDays(), ChronoUnit.DAYS);
         deleted += new DeleteAction().execute(new DeleteInput(config.getTableName())
            .withQueryFilter(new QQueryFilter(new QFilterCriteria(EsbProcessedMessageTable.FIELD_PROCESSED_AT, QCriteriaOperator.LESS_THAN, cutoff)))
            .withOmitRecordChangeEvents(true)
            .withOmitDmlAudit(true)).getDeletedRecordCount();
      }

      if(deleted > 0)
      {
         LOG.info("Purged old ESB outbox and processed-message rows", logPair("deletedCount", deleted));
      }
      return (deleted);
   }



   /*******************************************************************************
    ** The destinations that have PENDING rows.
    *******************************************************************************/
   private List<String> findPendingDestinations(EsbOutboxConfig config) throws QException
   {
      GroupBy groupBy = new GroupBy(QFieldType.STRING, EsbOutboxTable.FIELD_DESTINATION_NAME);
      List<AggregateResult> results = new AggregateAction().execute(new AggregateInput(config.getTableName())
         .withFilter(new QQueryFilter(new QFilterCriteria(EsbOutboxTable.FIELD_STATUS, QCriteriaOperator.EQUALS, EsbOutboxTable.STATUS_PENDING)))
         .withGroupBy(groupBy)
         .withAggregate(new Aggregate(EsbOutboxTable.FIELD_ID, AggregateOperator.COUNT))).getResults();

      Set<String> destinationNames = new LinkedHashSet<>();
      for(AggregateResult result : CollectionUtils.nonNullList(results))
      {
         Serializable value = result.getGroupByValue(groupBy);
         if(value != null)
         {
            destinationNames.add(value.toString());
         }
      }
      return (new ArrayList<>(destinationNames));
   }



   /*******************************************************************************
    ** Claim, send, and mark one destination's head-of-line rows.  Returns how
    ** many were sent.
    *******************************************************************************/
   int relayDestination(EsbOutboxConfig config, String destinationName) throws QException
   {
      List<QRecord> claimed = claim(config, destinationName);
      if(claimed.isEmpty())
      {
         return (0);
      }

      /////////////////////////////////////////////////////////////////////
      // a row whose event can't be read can never be sent: it's DEAD    //
      // right away (and the rest of the batch goes on without it)       //
      /////////////////////////////////////////////////////////////////////
      List<QRecord>  rows       = new ArrayList<>();
      List<EsbEvent> events     = new ArrayList<>();
      List<String>   messageIds = new ArrayList<>();
      for(QRecord row : claimed)
      {
         try
         {
            events.add(EsbEventCodec.fromJson(row.getValueString(EsbOutboxTable.FIELD_EVENT_JSON)));
            messageIds.add(row.getValueString(EsbOutboxTable.FIELD_UUID));
            rows.add(row);
         }
         catch(EsbUnparseableMessageException e)
         {
            LOG.error("An ESB outbox row's event can't be read; it is DEAD", logPair("destinationName", destinationName), logPair("uuid", row.getValueString(EsbOutboxTable.FIELD_UUID)), logPair("error", e.getMessage()));
            update(config, List.of(new QRecord()
               .withValue(EsbOutboxTable.FIELD_ID, row.getValue(EsbOutboxTable.FIELD_ID))
               .withValue(EsbOutboxTable.FIELD_STATUS, EsbOutboxTable.STATUS_DEAD)
               .withValue(EsbOutboxTable.FIELD_LAST_ERROR, e.getMessage())
               .withValue(EsbOutboxTable.FIELD_LEASE_OWNER, null)
               .withValue(EsbOutboxTable.FIELD_LEASE_EXPIRES_AT, null)));
         }
      }
      if(rows.isEmpty())
      {
         return (0);
      }

      try
      {
         EsbPublisher.getInstance().sendOutbox(destinationName, events, messageIds);
      }
      catch(Exception | LinkageError e)
      {
         onSendFailed(config, destinationName, rows, e);
         return (0);
      }

      markSent(config, rows);
      String providerName = getProviderName(destinationName);
      if(providerName != null && failingProviders.remove(providerName))
      {
         LOG.info("The ESB outbox is sending again", logPair("providerName", providerName));
      }
      for(int i = 0; i < rows.size(); i++)
      {
         EsbStats.getInstance().published(destinationName);
      }
      return (rows.size());
   }



   /*******************************************************************************
    ** Lease the destination's head-of-line rows to this node - see the class
    ** comment.  Empty if the destination is busy (another node), backing off,
    ** or has no PENDING rows.
    *******************************************************************************/
   private List<QRecord> claim(EsbOutboxConfig config, String destinationName) throws QException
   {
      String       tableName = config.getTableName();
      QQueryFilter filter    = pendingFilter(destinationName).withLimit(config.getBatchSize());
      if(SkipLockedClaims.isSupported(tableName))
      {
         return (SkipLockedClaims.claim(tableName, filter, (locked, transaction) -> claimHead(config, destinationName, locked, transaction)));
      }

      List<QRecord> candidates = new QueryAction().execute(new QueryInput(tableName).withFilter(filter)).getRecords();
      return (claimHead(config, destinationName, candidates, null));
   }



   /*******************************************************************************
    ** Lease the candidates (in id order) if they start at the destination's
    ** head-of-line row, and it is free and due.  A head row that has failed
    ** before is claimed alone, so a poison row is isolated.
    *******************************************************************************/
   private List<QRecord> claimHead(EsbOutboxConfig config, String destinationName, List<QRecord> candidates, QBackendTransaction transaction) throws QException
   {
      if(CollectionUtils.nullSafeIsEmpty(candidates))
      {
         return (List.of());
      }

      QRecord head = candidates.get(0);
      if(transaction != null)
      {
         //////////////////////////////////////////////////////////////////////
         // SKIP LOCKED skipped any rows another node has locked: the first  //
         // candidate must still be the destination's oldest PENDING row     //
         //////////////////////////////////////////////////////////////////////
         List<QRecord> trueHead = new QueryAction().execute(new QueryInput(config.getTableName())
            .withTransaction(transaction)
            .withFilter(pendingFilter(destinationName).withLimit(1))).getRecords();
         if(trueHead.isEmpty() || !Objects.equals(asLong(trueHead.get(0).getValue(EsbOutboxTable.FIELD_ID)), asLong(head.getValue(EsbOutboxTable.FIELD_ID))))
         {
            return (List.of());
         }
      }

      Instant now = Instant.now();
      if(isLeasedByAnother(head, now))
      {
         return (List.of());
      }
      Instant nextAttemptAt = head.getValueInstant(EsbOutboxTable.FIELD_NEXT_ATTEMPT_AT);
      if(nextAttemptAt != null && nextAttemptAt.isAfter(now))
      {
         return (List.of());
      }

      int           take      = (Objects.requireNonNullElse(head.getValueInteger(EsbOutboxTable.FIELD_ATTEMPTS), 0) > 0) ? 1 : candidates.size();
      Instant       expiresAt = now.plusMillis(config.getLeaseMs());
      List<QRecord> claimed   = new ArrayList<>();
      List<QRecord> leases    = new ArrayList<>();
      for(int i = 0; i < take; i++)
      {
         QRecord candidate = candidates.get(i);
         if(i > 0 && isLeasedByAnother(candidate, now))
         {
            break;
         }
         candidate.setValue(EsbOutboxTable.FIELD_LEASE_OWNER, nodeId);
         candidate.setValue(EsbOutboxTable.FIELD_LEASE_EXPIRES_AT, expiresAt);
         claimed.add(candidate);
         leases.add(new QRecord()
            .withValue(EsbOutboxTable.FIELD_ID, candidate.getValue(EsbOutboxTable.FIELD_ID))
            .withValue(EsbOutboxTable.FIELD_LEASE_OWNER, nodeId)
            .withValue(EsbOutboxTable.FIELD_LEASE_EXPIRES_AT, expiresAt));
      }

      new UpdateAction().execute(new UpdateInput(config.getTableName())
         .withRecords(leases)
         .withTransaction(transaction)
         .withOmitRecordChangeEvents(true)
         .withOmitDmlAudit(true));
      return (claimed);
   }



   /*******************************************************************************
    ** Whether a row is leased, unexpired, to another node.
    *******************************************************************************/
   private boolean isLeasedByAnother(QRecord row, Instant now)
   {
      String  owner     = row.getValueString(EsbOutboxTable.FIELD_LEASE_OWNER);
      Instant expiresAt = row.getValueInstant(EsbOutboxTable.FIELD_LEASE_EXPIRES_AT);
      return (owner != null && !owner.equals(nodeId) && expiresAt != null && expiresAt.isAfter(now));
   }



   /*******************************************************************************
    ** Mark sent rows SENT, and release their lease.
    *******************************************************************************/
   private void markSent(EsbOutboxConfig config, List<QRecord> rows) throws QException
   {
      Instant       now     = Instant.now();
      List<QRecord> updates = new ArrayList<>();
      for(QRecord row : rows)
      {
         updates.add(new QRecord()
            .withValue(EsbOutboxTable.FIELD_ID, row.getValue(EsbOutboxTable.FIELD_ID))
            .withValue(EsbOutboxTable.FIELD_STATUS, EsbOutboxTable.STATUS_SENT)
            .withValue(EsbOutboxTable.FIELD_SENT_AT, now)
            .withValue(EsbOutboxTable.FIELD_ATTEMPTS, Objects.requireNonNullElse(row.getValueInteger(EsbOutboxTable.FIELD_ATTEMPTS), 0) + 1)
            .withValue(EsbOutboxTable.FIELD_NEXT_ATTEMPT_AT, null)
            .withValue(EsbOutboxTable.FIELD_LEASE_OWNER, null)
            .withValue(EsbOutboxTable.FIELD_LEASE_EXPIRES_AT, null));
      }
      update(config, updates);
   }



   /*******************************************************************************
    ** A send of the rows failed: release them, and - if the provider is
    ** connected, so this isn't an outage - count an attempt on the head row
    ** (backing it off, or making it DEAD after maxAttempts).
    *******************************************************************************/
   private void onSendFailed(EsbOutboxConfig config, String destinationName, List<QRecord> rows, Throwable throwable) throws QException
   {
      String  errorText    = getErrorText(throwable);
      String  providerName = getProviderName(destinationName);
      boolean connected    = providerName != null && EsbConnectionManager.getInstance().isConnected(providerName);

      List<QRecord> updates = new ArrayList<>();
      for(QRecord row : rows)
      {
         updates.add(new QRecord()
            .withValue(EsbOutboxTable.FIELD_ID, row.getValue(EsbOutboxTable.FIELD_ID))
            .withValue(EsbOutboxTable.FIELD_LEASE_OWNER, null)
            .withValue(EsbOutboxTable.FIELD_LEASE_EXPIRES_AT, null));
      }

      QRecord head       = rows.get(0);
      QRecord headUpdate = updates.get(0);
      headUpdate.setValue(EsbOutboxTable.FIELD_LAST_ERROR, errorText);
      if(!connected)
      {
         if(providerName == null || failingProviders.add(providerName))
         {
            LOG.warn("The ESB outbox can't reach its broker; rows will wait for it", logPair("destinationName", destinationName), logPair("providerName", providerName), logPair("error", errorText));
         }
      }
      else
      {
         int attempts = Objects.requireNonNullElse(head.getValueInteger(EsbOutboxTable.FIELD_ATTEMPTS), 0) + 1;
         headUpdate.setValue(EsbOutboxTable.FIELD_ATTEMPTS, attempts);
         if(attempts >= config.getMaxAttempts())
         {
            headUpdate.setValue(EsbOutboxTable.FIELD_STATUS, EsbOutboxTable.STATUS_DEAD);
            LOG.error("An ESB outbox row failed its last attempt; it is DEAD", logPair("destinationName", destinationName), logPair("uuid", head.getValueString(EsbOutboxTable.FIELD_UUID)), logPair("attempts", attempts), logPair("error", errorText));
         }
         else
         {
            long backoffMs = getBackoffMs(config, attempts);
            headUpdate.setValue(EsbOutboxTable.FIELD_NEXT_ATTEMPT_AT, Instant.now().plusMillis(backoffMs));
            LOG.info("An ESB outbox send failed; it will be retried", logPair("destinationName", destinationName), logPair("uuid", head.getValueString(EsbOutboxTable.FIELD_UUID)), logPair("attempts", attempts), logPair("retryInMs", backoffMs), logPair("error", errorText));
         }
      }

      for(int i = 0; i < rows.size(); i++)
      {
         EsbStats.getInstance().publishFailed(destinationName, throwable instanceof Exception exception ? exception : new QException(errorText, throwable));
      }
      update(config, updates);
   }



   /*******************************************************************************
    ** The wait before a row's next send, after its attempts-th failure:
    ** initialBackoffMs, doubling, up to maxBackoffMs.
    *******************************************************************************/
   static long getBackoffMs(EsbOutboxConfig config, int attempts)
   {
      long backoffMs = config.getInitialBackoffMs();
      for(int i = 1; i < attempts && backoffMs < config.getMaxBackoffMs(); i++)
      {
         backoffMs *= 2;
      }
      return (Math.min(backoffMs, config.getMaxBackoffMs()));
   }



   /*******************************************************************************
    ** Update outbox rows (as the system user, without events or audits).
    *******************************************************************************/
   private static void update(EsbOutboxConfig config, List<QRecord> records) throws QException
   {
      new UpdateAction().execute(new UpdateInput(config.getTableName())
         .withRecords(records)
         .withOmitRecordChangeEvents(true)
         .withOmitDmlAudit(true));
   }



   /*******************************************************************************
    ** One destination's PENDING rows, oldest first.
    *******************************************************************************/
   private static QQueryFilter pendingFilter(String destinationName)
   {
      return (new QQueryFilter(
         new QFilterCriteria(EsbOutboxTable.FIELD_STATUS, QCriteriaOperator.EQUALS, EsbOutboxTable.STATUS_PENDING),
         new QFilterCriteria(EsbOutboxTable.FIELD_DESTINATION_NAME, QCriteriaOperator.EQUALS, destinationName))
         .withOrderBy(new QFilterOrderBy(EsbOutboxTable.FIELD_ID)));
   }



   /*******************************************************************************
    ** The provider of a destination (in the QContext's instance), or null.
    *******************************************************************************/
   private static String getProviderName(String destinationName)
   {
      EsbInstanceMetaData     esbInstanceMetaData = QSupplementalInstanceMetaData.of(QContext.getQInstance(), EsbInstanceMetaData.NAME);
      QEsbDestinationMetaData destination         = (esbInstanceMetaData == null) ? null : esbInstanceMetaData.getDestination(destinationName);
      return (destination == null ? null : destination.getProviderName());
   }



   /*******************************************************************************
    ** A failure's message, or its class name if it has none.
    *******************************************************************************/
   private static String getErrorText(Throwable throwable)
   {
      return (throwable.getMessage() != null && !throwable.getMessage().isBlank() ? throwable.getMessage() : throwable.getClass().getSimpleName());
   }



   /*******************************************************************************
    ** An id value as a Long (backends return Integer or Long).
    *******************************************************************************/
   private static Long asLong(Serializable value)
   {
      return (value instanceof Number number ? number.longValue() : null);
   }



   /*******************************************************************************
    *******************************************************************************/
   private long getPollIntervalMs()
   {
      EsbOutboxConfig config = EsbOutbox.getEnabledConfig(qInstance);
      return (config == null ? 1000L : config.getPollIntervalMs());
   }



   /*******************************************************************************
    *******************************************************************************/
   private long getPurgeIntervalMs()
   {
      EsbOutboxConfig config = EsbOutbox.getEnabledConfig(qInstance);
      return (config == null ? 3_600_000L : config.getPurgeIntervalMs());
   }



   /*******************************************************************************
    ** host:random - unique per JVM, readable in the outbox table.
    *******************************************************************************/
   private static String makeNodeId()
   {
      String host;
      try
      {
         host = InetAddress.getLocalHost().getHostName();
      }
      catch(Exception e)
      {
         host = "unknown";
      }
      return (host + ":" + UUID.randomUUID().toString().substring(0, 8));
   }

}
