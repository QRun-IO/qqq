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

package com.kingsrook.qqq.backend.core.health;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.alerts.AlertLink;
import com.kingsrook.qqq.backend.core.alerts.AlertRecordRef;
import com.kingsrook.qqq.backend.core.alerts.AlertResolution;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.json.JSONArray;


/*******************************************************************************
 ** Pure threshold and flap evaluation; the runner persists and dispatches effects.
 *******************************************************************************/
public class HealthTransitionEvaluator
{
   /*******************************************************************************
    ** A decision contains only new state and alert intents, with no I/O.
    *******************************************************************************/
   public record Decision(QRecord state, boolean transition, List<AlertSignal> raises, List<AlertResolution> resolutions)
   {
   }



   /*******************************************************************************
    ** UNKNOWN breaks consecutive streaks but never changes state or raises alerts.
    *******************************************************************************/
   public Decision evaluate(QRecord previous, HealthCheckOutcome outcome, int failureThreshold, int recoveryThreshold, Instant now)
   {
      QRecord state = new QRecord().withValue("id", previous.getValue("id"));
      List<AlertSignal> raises = new ArrayList<>();
      List<AlertResolution> resolutions = new ArrayList<>();
      HealthStatus current = status(previous.getValueString("currentStatus"));
      HealthStatus observed = outcome.getStatus() == null ? HealthStatus.UNKNOWN : outcome.getStatus();
      String name = previous.getValueString("name");
      Instant since = previous.getValueInstant("currentSince");
      boolean flapping = Boolean.TRUE.equals(previous.getValueBoolean("flapping"));
      List<Instant> changes = new ArrayList<>();
      String json = previous.getValueString("recentTransitionsJson");
      if(json != null)
      {
         for(Object value : new JSONArray(json))
         {
            Instant change = Instant.parse(value.toString());
            if(change.isAfter(now.minusSeconds(1800)))
            {
               changes.add(change);
            }
         }
      }

      int failures = observed == HealthStatus.UP || observed == HealthStatus.UNKNOWN ? 0 : integer(previous, "consecutiveFailures") + 1;
      int successes = observed == HealthStatus.UP ? integer(previous, "consecutiveSuccesses") + 1 : 0;
      boolean transition = observed != HealthStatus.UNKNOWN && observed != current
         && (observed == HealthStatus.UP ? successes >= recoveryThreshold : failures >= failureThreshold);

      if(transition)
      {
         changes.add(now);
         state.withValue("currentStatus", observed.name()).withValue("currentSince", now);
         if(!flapping && changes.size() >= 4)
         {
            flapping = true;
            raises.add(signal(previous, "flapping", " is flapping", outcome.getMessage(), now));
         }
         if(!flapping)
         {
            addTransitionEffects(previous, observed, outcome, now, since, raises, resolutions);
         }
      }
      else if(flapping && observed != HealthStatus.UNKNOWN && since != null && !now.isBefore(since.plusSeconds(1800)))
      {
         flapping = false;
         resolutions.add(resolution("health.check.flapping", name, "Stable for 30 minutes"));
         // Reconcile suppressed alerts to the stable state, including a suppressed recovery.
         addTransitionEffects(previous, current, outcome, now, since, raises, resolutions);
      }
      state.withValue("consecutiveFailures", failures).withValue("consecutiveSuccesses", successes)
         .withValue("flapping", flapping).withValue("recentTransitionsJson", JsonUtils.toJson(changes.stream().map(Instant::toString).toList()));
      return (new Decision(state, transition, List.copyOf(raises), List.copyOf(resolutions)));
   }



   /*******************************************************************************
    ** Alert intents use only redacted data and stable record references.
    *******************************************************************************/
   private void addTransitionEffects(QRecord previous, HealthStatus status, HealthCheckOutcome outcome, Instant now, Instant since,
      List<AlertSignal> raises, List<AlertResolution> resolutions)
   {
      String name = previous.getValueString("name");
      if(status == HealthStatus.UP)
      {
         String note = "Recovered after " + Duration.between(since == null ? now : since, now);
         resolutions.add(resolution("health.check.down", name, note));
         resolutions.add(resolution("health.check.degraded", name, note));
      }
      else if(status == HealthStatus.DOWN || status == HealthStatus.DEGRADED)
      {
         String suffix = status == HealthStatus.DOWN ? "down" : "degraded";
         raises.add(signal(previous, suffix, " is " + suffix, outcome.getMessage(), now));
         resolutions.add(resolution(status == HealthStatus.DOWN ? "health.check.degraded" : "health.check.down", name, "Changed to " + status));
      }
   }



   /*******************************************************************************
    ** DEGRADED severity is one level below the check's DOWN severity.
    *******************************************************************************/
   private AlertSignal signal(QRecord check, String suffix, String titleSuffix, String summary, Instant now)
   {
      AlertSeverity severity = AlertSeverity.valueOf(check.getValueString("severity"));
      if("degraded".equals(suffix))
      {
         severity = switch(severity)
         {
            case CRITICAL -> AlertSeverity.ERROR;
            case ERROR -> AlertSeverity.WARNING;
            default -> AlertSeverity.INFO;
         };
      }
      return (new AlertSignal("health.check." + suffix).withDedupKey(check.getValueString("name"))
         .withSeverity(severity).withTitle(HealthRedactor.redact(check.getValueString("label")) + titleSuffix)
         .withSummary(HealthRedactor.redact(summary)).withOccurredAt(now)
         .withRecordRef(new AlertRecordRef(HealthMetaDataProvider.CHECK_TABLE, check.getValue("id")))
         .withAttribute("component", HealthRedactor.redact(check.getValueString("component")))
         .withLink(AlertLink.toPath(homePath(), "System Health")));
   }



   /*******************************************************************************
    ** Construct a resolution intent.
    *******************************************************************************/
   private AlertResolution resolution(String type, String name, String note)
   {
      return (new AlertResolution().withAlertTypeName(type).withDedupKey(name).withNote(note));
   }



   /*******************************************************************************
    ** Uninitialized state is UNKNOWN.
    *******************************************************************************/
   static HealthStatus status(String value)
   {
      return (value == null ? HealthStatus.UNKNOWN : HealthStatus.valueOf(value));
   }



   /*******************************************************************************
    ** Uninitialized counters are zero.
    *******************************************************************************/
   private int integer(QRecord record, String field)
   {
      return (record.getValueInteger(field) == null ? 0 : record.getValueInteger(field));
   }



   /*******************************************************************************
    ** Where alerts link to: the host's configured health home, else /systemHealth.
    *******************************************************************************/
   static String homePath()
   {
      QInstance instance = QContext.getQInstance();
      HealthMonitoringMetaData settings = instance == null ? null : QSupplementalInstanceMetaData.of(instance, HealthMonitoringMetaData.NAME);
      return (settings == null || settings.getHomePath() == null || settings.getHomePath().isBlank() ? "/systemHealth" : settings.getHomePath());
   }
}
