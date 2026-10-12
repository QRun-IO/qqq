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

package com.kingsrook.qqq.backend.core.alerts;


import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** The producer API for alerts ("manage by exception").  Any qbit or app can
 ** call raise, resolve and heartbeat; with no alerts implementation installed
 ** these go to LoggingAlertSink, so producers need nothing beyond core.
 **
 ** Guarantees (the same promises the ESB makes for publishing):
 ** - never throws into the caller and never blocks it: a failure is logged and
 **   counted (getProducerFailureCount).
 ** - transaction-aware: when given a transaction (or when QContext has one),
 **   the call is deferred with addAfterCommitCallback, and dropped on rollback,
 **   so an alert never describes a write that did not happen.
 ** - an unknown alert type name is passed on (with a rate-limited warning), so
 **   a typo never loses a signal.
 **
 ** Example:
 **   QAlerts.raise(new AlertSignal("cdn.purge.dead")
 **      .withDedupKey("provider:" + providerId)
 **      .withTitle("3 CDN purges failed after retries")
 **      .withRecordRefs(AlertRecordRef.of("cdn_purge_request", ids)));
 **   QAlerts.resolve("cdn.purge.dead", "provider:" + providerId, "A later purge succeeded");
 **   QAlerts.heartbeat("backups.job.nightly-db");
 *******************************************************************************/
public final class QAlerts
{
   private static final QLogger LOG = QLogger.getLogger(QAlerts.class);

   private static final AlertSinkInterface DEFAULT_SINK = new LoggingAlertSink();

   private static final AtomicLong producerFailures = new AtomicLong();

   private static final Map<String, Instant> unknownTypeWarnedAt = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** Not instantiable.
    *******************************************************************************/
   private QAlerts()
   {
   }



   /*******************************************************************************
    ** Raise (or refresh) an alert.  Idempotent on (type, dedupKey).  Deferred
    ** until commit when QContext has a transaction.
    *******************************************************************************/
   public static void raise(AlertSignal signal)
   {
      raise(signal, null);
   }



   /*******************************************************************************
    ** Raise (or refresh) an alert after the given transaction commits (or now,
    ** when transaction is null and QContext has none).
    *******************************************************************************/
   public static void raise(AlertSignal signal, QBackendTransaction transaction)
   {
      try
      {
         if(signal == null || signal.getAlertTypeName() == null || signal.getAlertTypeName().isBlank())
         {
            producerFailures.incrementAndGet();
            LOG.warn("Ignoring an alert signal with no alert type name");
            return;
         }

         if(signal.getOccurredAt() == null)
         {
            signal.setOccurredAt(Instant.now());
         }

         QInstance          qInstance = QContext.getQInstance();
         QAlertTypeMetaData alertType = QAlertTypeMetaData.of(qInstance, signal.getAlertTypeName());
         if(alertType == null)
         {
            warnUnknownType(signal.getAlertTypeName());
         }
         else if(signal.getSeverity() == null)
         {
            signal.setSeverity(alertType.getDefaultSeverity());
         }

         AlertSinkInterface sink = getSink(qInstance);
         dispatch(transaction, "raise", signal.getAlertTypeName(), () -> sink.raise(signal));
      }
      catch(Exception | LinkageError e)
      {
         recordFailure("raise", signal == null ? null : signal.getAlertTypeName(), e);
      }
   }



   /*******************************************************************************
    ** Resolve an open alert (a no-op if it is not open).
    *******************************************************************************/
   public static void resolve(String alertTypeName, String dedupKey, String note)
   {
      resolve(new AlertResolution(alertTypeName, dedupKey, note), null);
   }



   /*******************************************************************************
    ** Resolve an open alert after the given transaction commits (or now).
    *******************************************************************************/
   public static void resolve(AlertResolution resolution, QBackendTransaction transaction)
   {
      try
      {
         if(resolution == null || resolution.getAlertTypeName() == null || resolution.getAlertTypeName().isBlank())
         {
            producerFailures.incrementAndGet();
            LOG.warn("Ignoring an alert resolution with no alert type name");
            return;
         }

         if(resolution.getResolvedAt() == null)
         {
            resolution.withResolvedAt(Instant.now());
         }

         AlertSinkInterface sink = getSink(QContext.getQInstance());
         dispatch(transaction, "resolve", resolution.getAlertTypeName(), () -> sink.resolve(resolution));
      }
      catch(Exception | LinkageError e)
      {
         recordFailure("resolve", resolution == null ? null : resolution.getAlertTypeName(), e);
      }
   }



   /*******************************************************************************
    ** Dead-man's switch: "I ran".  A heartbeat monitor raises when beats stop.
    ** Not deferred to a commit - a beat is about the code running, not data.
    *******************************************************************************/
   public static void heartbeat(String heartbeatKey)
   {
      try
      {
         if(heartbeatKey == null || heartbeatKey.isBlank())
         {
            producerFailures.incrementAndGet();
            LOG.warn("Ignoring a heartbeat with no key");
            return;
         }

         getSink(QContext.getQInstance()).heartbeat(heartbeatKey, Instant.now());
      }
      catch(Exception | LinkageError e)
      {
         recordFailure("heartbeat", heartbeatKey, e);
      }
   }



   /*******************************************************************************
    ** How many producer calls have failed (bad input, a sink that threw, a sink
    ** that could not load) since startup.
    *******************************************************************************/
   public static long getProducerFailureCount()
   {
      return (producerFailures.get());
   }



   /*******************************************************************************
    ** The sink for an instance: its AlertsInstanceMetaData's sink (loaded once
    ** and cached), else the logging sink.  A sink that fails to load falls
    ** back to the logging sink (and is counted), so signals are never lost
    ** silently.
    *******************************************************************************/
   static AlertSinkInterface getSink(QInstance qInstance)
   {
      AlertsInstanceMetaData alertsInstanceMetaData = AlertsInstanceMetaData.of(qInstance);
      if(alertsInstanceMetaData == null || alertsInstanceMetaData.getSink() == null)
      {
         return (DEFAULT_SINK);
      }

      AlertSinkInterface loadedSink = alertsInstanceMetaData.getLoadedSink();
      if(loadedSink != null)
      {
         return (loadedSink);
      }

      try
      {
         loadedSink = QCodeLoader.getAdHoc(AlertSinkInterface.class, alertsInstanceMetaData.getSink());
      }
      catch(Exception | LinkageError e)
      {
         LOG.warn("Could not load the alerts sink; using the logging sink", e, logPair("sink", alertsInstanceMetaData.getSink().getName()));
      }

      if(loadedSink == null)
      {
         producerFailures.incrementAndGet();
         return (DEFAULT_SINK);
      }

      alertsInstanceMetaData.setLoadedSink(loadedSink);
      return (loadedSink);
   }



   /*******************************************************************************
    ** Run the sink call now, or after the transaction commits.  The deferred
    ** call has the same no-throw guarantee.
    *******************************************************************************/
   private static void dispatch(QBackendTransaction transaction, String operation, String alertTypeName, Runnable sinkCall)
   {
      QBackendTransaction effectiveTransaction = transaction != null ? transaction : QContext.getQBackendTransaction();
      Runnable            safeCall             = () ->
      {
         try
         {
            sinkCall.run();
         }
         catch(Exception | LinkageError e)
         {
            recordFailure(operation, alertTypeName, e);
         }
      };

      if(effectiveTransaction != null)
      {
         effectiveTransaction.addAfterCommitCallback(safeCall);
      }
      else
      {
         safeCall.run();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void recordFailure(String operation, String alertTypeName, Throwable throwable)
   {
      producerFailures.incrementAndGet();
      LOG.warn("Error sending an alert signal", throwable, logPair("operation", operation), logPair("alertType", alertTypeName));
   }



   /*******************************************************************************
    ** Warn (at most once an hour per name) about a type that isn't declared.
    ** Only when some alert meta-data exists - otherwise nothing is declared and
    ** every type would warn.
    *******************************************************************************/
   private static void warnUnknownType(String alertTypeName)
   {
      if(AlertsInstanceMetaData.of(QContext.getQInstance()) == null)
      {
         return;
      }

      Instant now  = Instant.now();
      Instant last = unknownTypeWarnedAt.get(alertTypeName);
      if(last == null || now.isAfter(last.plusSeconds(3600)))
      {
         if(unknownTypeWarnedAt.size() > 1000)
         {
            unknownTypeWarnedAt.clear();
         }
         unknownTypeWarnedAt.put(alertTypeName, now);
         LOG.warn("Alert raised with a type that no QAlertTypeMetaData declares; it will be recorded as unregistered", logPair("alertType", alertTypeName));
      }
   }
}
