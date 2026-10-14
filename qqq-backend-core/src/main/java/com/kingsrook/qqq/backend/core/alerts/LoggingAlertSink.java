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


import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** The default alert sink, used when no alerts implementation is installed: it
 ** logs.  INFO signals log at INFO, WARNING and above at WARN.  Logging is
 ** rate-limited per (type, dedupKey), so a producer that raises the same alert
 ** every few seconds writes one line per window, not thousands.  Heartbeats
 ** are not logged (they are expected, frequent and not a problem).
 ** Notifications log at INFO (WARN for ERROR and CRITICAL), rate-limited per
 ** (category, sourceKey or title).
 **
 ** With this sink, producer code paths are identical with and without an
 ** alerts implementation - which is the point.
 *******************************************************************************/
public class LoggingAlertSink implements AlertSinkInterface
{
   private static final QLogger LOG = QLogger.getLogger(LoggingAlertSink.class);

   /** how long to stay quiet about a key after logging it. */
   public static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(5);

   /** cap on remembered keys; past it the memory is cleared (worst case: a few extra lines). */
   private static final int MAX_TRACKED_KEYS = 10_000;

   private final Map<String, Instant> lastLoggedByKey = new ConcurrentHashMap<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void raise(AlertSignal signal)
   {
      String key = signal.getAlertTypeName() + "|" + signal.getEffectiveDedupKey();
      if(!shouldLog("raise|" + key, signal.getOccurredAt()))
      {
         return;
      }

      AlertSeverity severity = signal.getSeverity();
      if(severity == null || severity == AlertSeverity.INFO)
      {
         LOG.info("Alert raised (no alerts implementation installed; logged only)", logPair("alertType", signal.getAlertTypeName()), logPair("dedupKey", signal.getEffectiveDedupKey()), logPair("severity", severity), logPair("title", signal.getTitle()), logPair("count", signal.getEffectiveCount()));
      }
      else
      {
         LOG.warn("Alert raised (no alerts implementation installed; logged only)", logPair("alertType", signal.getAlertTypeName()), logPair("dedupKey", signal.getEffectiveDedupKey()), logPair("severity", severity), logPair("title", signal.getTitle()), logPair("count", signal.getEffectiveCount()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void resolve(AlertResolution resolution)
   {
      String key = resolution.getAlertTypeName() + "|" + resolution.getEffectiveDedupKey();
      if(!shouldLog("resolve|" + key, resolution.getResolvedAt()))
      {
         return;
      }

      LOG.info("Alert resolved (no alerts implementation installed; logged only)", logPair("alertType", resolution.getAlertTypeName()), logPair("dedupKey", resolution.getEffectiveDedupKey()), logPair("note", resolution.getNote()));
   }



   /*******************************************************************************
    ** Heartbeats are expected and frequent, so they are not logged.
    *******************************************************************************/
   @Override
   public void heartbeat(String heartbeatKey, Instant at)
   {
      ////////////////////////////////////////////////////////
      // noop - a beat is "all is well", nothing to log     //
      ////////////////////////////////////////////////////////
   }



   /*******************************************************************************
    ** Log a user notification (there is no inbox without an implementation).
    *******************************************************************************/
   @Override
   public void notify(NotificationSignal signal)
   {
      String sourceKey = signal.getSourceKey() == null || signal.getSourceKey().isBlank() ? signal.getTitle() : signal.getSourceKey();
      if(!shouldLog("notify|" + signal.getCategory() + "|" + sourceKey, signal.getOccurredAt()))
      {
         return;
      }

      NotificationSeverity severity = signal.getSeverity();
      if(severity == NotificationSeverity.ERROR || severity == NotificationSeverity.CRITICAL)
      {
         LOG.warn("Notification (no alerts implementation installed; logged only)", logPair("category", signal.getCategory()), logPair("severity", severity), logPair("title", signal.getTitle()), logPair("sourceKey", signal.getSourceKey()), logPair("audience", signal.getAudience()));
      }
      else
      {
         LOG.info("Notification (no alerts implementation installed; logged only)", logPair("category", signal.getCategory()), logPair("severity", severity), logPair("title", signal.getTitle()), logPair("sourceKey", signal.getSourceKey()), logPair("audience", signal.getAudience()));
      }
   }



   /*******************************************************************************
    ** True when the key hasn't been logged within the rate-limit window (and
    ** records that it now has been).
    *******************************************************************************/
   boolean shouldLog(String key, Instant at)
   {
      Instant now = at == null ? Instant.now() : at;
      if(lastLoggedByKey.size() > MAX_TRACKED_KEYS)
      {
         lastLoggedByKey.clear();
      }

      boolean[] log = {false};
      lastLoggedByKey.compute(key, (k, last) ->
      {
         if(last == null || !now.isBefore(last.plus(RATE_LIMIT_WINDOW)))
         {
            log[0] = true;
            return (now);
         }
         return (last);
      });
      return (log[0]);
   }
}
