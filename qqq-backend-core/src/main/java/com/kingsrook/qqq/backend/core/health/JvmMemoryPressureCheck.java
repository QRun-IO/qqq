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


import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;


/*******************************************************************************
 ** Readiness and liveness views of JvmMemoryPressure.
 **
 ** READINESS: DEGRADED as soon as pressure starts; after readinessAfterSeconds
 ** it is DOWN (503 on /ready, the pod leaves the Service) when the host opted
 ** in with QQQ_HEALTH_MEMORY_READINESS_ACTION=down, else it stays DEGRADED.
 **
 ** LIVENESS: DOWN once pressure has lasted livenessAfterSeconds, which fails
 ** /live and lets the orchestrator restart the JVM instead of letting it
 ** crawl until OutOfMemoryError.
 *******************************************************************************/
public class JvmMemoryPressureCheck implements HealthCheck
{
   public static final String READINESS_NAME = "core.jvm.memory_pressure";
   public static final String LIVENESS_NAME   = "core.jvm.memory_pressure_sustained";

   private final HealthCheckKind              kind;
   private final Supplier<JvmMemoryPressure> monitor;



   /*******************************************************************************
    ** Check against the JVM-wide monitor.
    *******************************************************************************/
   public JvmMemoryPressureCheck(HealthCheckKind kind)
   {
      this(kind, JvmMemoryPressure::get);
   }



   /*******************************************************************************
    ** Check against a given monitor (tests).
    *******************************************************************************/
   public JvmMemoryPressureCheck(HealthCheckKind kind, Supplier<JvmMemoryPressure> monitor)
   {
      this.kind = kind;
      this.monitor = monitor;
   }



   /*******************************************************************************
    ** Core registrations: a readiness check and a liveness-probe check, both
    ** evaluated live on every probe (never from the runner's cache).
    *******************************************************************************/
   public static List<HealthCheckMetaData> definitions()
   {
      return (List.of(
         new HealthCheckMetaData().withName(READINESS_NAME).withLabel("JVM memory pressure").withComponent("core")
            .withCategory(HealthCategory.CORE).withKind(HealthCheckKind.READINESS).withSeverity(AlertSeverity.WARNING)
            .withIncludeInReadiness(true).withProbeCache(false).withIntervalSeconds(30).withTimeoutMs(1000).withFailureThreshold(1)
            .withCheckInstance(new JvmMemoryPressureCheck(HealthCheckKind.READINESS)),
         new HealthCheckMetaData().withName(LIVENESS_NAME).withLabel("JVM memory pressure (sustained)").withComponent("core")
            .withCategory(HealthCategory.CORE).withKind(HealthCheckKind.LIVENESS).withSeverity(AlertSeverity.ERROR)
            .withIncludeInLivenessProbe(true).withProbeCache(false).withIntervalSeconds(30).withTimeoutMs(1000).withFailureThreshold(1)
            .withCheckInstance(new JvmMemoryPressureCheck(HealthCheckKind.LIVENESS))));
   }



   /*******************************************************************************
    ** Sample and judge.
    *******************************************************************************/
   @Override
   public HealthCheckOutcome run(HealthCheckInput input)
   {
      JvmMemoryPressure                   pressure = monitor.get();
      JvmMemoryPressure.Settings          settings = pressure.getSettings();
      JvmMemoryPressure.Reading           reading  = pressure.sample();
      long                                seconds  = reading.pressuredFor().toSeconds();
      String                              summary  = String.format(Locale.ROOT, "heap after GC %s, GC time %.0f%% of the last %ds",
         reading.heapAfterGcPercent() == null ? "unknown" : String.format(Locale.ROOT, "%.0f%%", reading.heapAfterGcPercent()), reading.gcTimePercent(), settings.windowSeconds());

      HealthCheckOutcome outcome;
      if(!reading.pressured())
      {
         outcome = HealthCheckOutcome.up("No memory pressure: " + summary);
      }
      else if(kind == HealthCheckKind.LIVENESS)
      {
         outcome = seconds >= settings.livenessAfterSeconds()
            ? HealthCheckOutcome.down("Memory pressure for " + seconds + "s (limit " + settings.livenessAfterSeconds() + "s), restart needed: " + summary)
            : HealthCheckOutcome.degraded("Memory pressure for " + seconds + "s: " + summary);
      }
      else
      {
         outcome = seconds >= settings.readinessAfterSeconds() && settings.readinessDown()
            ? HealthCheckOutcome.down("Memory pressure for " + seconds + "s, not taking traffic: " + summary)
            : HealthCheckOutcome.degraded("Memory pressure for " + seconds + "s: " + summary);
      }
      HealthDetails details = new HealthDetails()
         .withValue("pressured", reading.pressured())
         .withValue("pressuredSeconds", seconds)
         .withValue("gcTimePercent", Math.round(reading.gcTimePercent()))
         .withValue("heapAfterGcThresholdPercent", settings.heapAfterGcPercent())
         .withValue("gcTimeThresholdPercent", settings.gcTimePercent());
      if(reading.heapAfterGcPercent() != null)
      {
         details.withValue("heapAfterGcPercent", Math.round(reading.heapAfterGcPercent()));
      }
      return (outcome.withDetails(details)
         .withMetrics(List.of(
            new HealthMetric().withName("heapAfterGc").withValue(reading.heapAfterGcPercent() == null ? Double.valueOf(0) : reading.heapAfterGcPercent()).withUnit("%"),
            new HealthMetric().withName("gcTime").withValue(reading.gcTimePercent()).withUnit("%"))));
   }
}
