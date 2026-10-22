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

package com.kingsrook.qqq.middleware.health;


import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.health.HealthCheck;
import com.kingsrook.qqq.backend.core.health.HealthCheckInput;
import com.kingsrook.qqq.backend.core.health.HealthCheckKind;
import com.kingsrook.qqq.backend.core.health.HealthCheckMetaData;
import com.kingsrook.qqq.backend.core.health.HealthCheckOutcome;
import com.kingsrook.qqq.backend.core.health.HealthChecksMetaData;
import com.kingsrook.qqq.backend.core.health.HealthRunner;
import com.kingsrook.qqq.backend.core.health.HealthStatus;
import com.kingsrook.qqq.backend.core.health.JvmMemoryPressure;
import com.kingsrook.qqq.backend.core.health.JvmMemoryPressureCheck;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;


/*******************************************************************************
 ** Bounded probe execution shared across requests, including interrupt-ignoring
 ** checks. Legacy liveness responses also include duration and sanitized details.
 *******************************************************************************/
public final class HealthProbes
{
   private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(0, 10, 30, TimeUnit.SECONDS,
      new SynchronousQueue<>(), Thread.ofPlatform().daemon().name("health-probe-", 0).factory());



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private HealthProbes()
   {
   }



   /*******************************************************************************
    ** Merge legacy indicators without executing a duplicate adapted check.
    *******************************************************************************/
   static List<HealthCheckMetaData> definitions(QInstance instance)
   {
      Map<String, HealthCheckMetaData> definitions = new LinkedHashMap<>();
      com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData legacy = QSupplementalInstanceMetaData.of(instance,
         com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData.METADATA_KEY);
      HealthChecksMetaData registry = QSupplementalInstanceMetaData.of(instance, HealthChecksMetaData.NAME);
      if(registry != null)
      {
         registry.getChecks().stream().filter(check -> legacy == null || !(check.getCheckInstance() instanceof HealthIndicatorCheck))
            .forEach(check -> definitions.put(check.getName(), check));
      }
      if(legacy != null && legacy.getIndicators() != null)
      {
         legacy.getIndicators().forEach(indicator ->
         {
            HealthCheckMetaData definition = new HealthIndicatorCheck(indicator).toMetaData();
            definitions.putIfAbsent(definition.getName(), definition);
         });
      }
      if(JvmMemoryPressure.get().getSettings().enabled())
      {
         JvmMemoryPressureCheck.definitions().forEach(definition -> definitions.putIfAbsent(definition.getName(), definition));
      }
      return (new ArrayList<>(definitions.values()));
   }



   /*******************************************************************************
    ** Whether a probe runs a check.
    *******************************************************************************/
   static boolean includedIn(HealthCheckMetaData check, Probe probe)
   {
      return (switch(probe)
      {
         case HEALTH -> check.getKind() == HealthCheckKind.LIVENESS && !check.getIncludeInLivenessProbe();
         case READINESS -> check.getKind() == HealthCheckKind.LIVENESS || (check.getKind() == HealthCheckKind.READINESS && check.getIncludeInReadiness());
         case LIVENESS -> check.getIncludeInLivenessProbe();
      });
   }



   /*******************************************************************************
    ** Launch concurrently, then wait against one monotonic total budget.
    *******************************************************************************/
   public static Map<String, Object> run(QInstance instance, boolean readiness)
   {
      return (run(instance, readiness ? Probe.READINESS : Probe.HEALTH));
   }



   /*******************************************************************************
    ** Which endpoint is asking.
    ** HEALTH (/health): LIVENESS checks, with legacy indicator details. Its JSON
    ** shape is kept for deploy checks, so /live-only checks are not added to it.
    ** READINESS (/ready): LIVENESS checks plus READINESS checks marked includeInReadiness.
    ** LIVENESS (/live): only checks marked includeInLivenessProbe, the ones a
    ** restart can fix; a broken dependency must never restart the pod.
    *******************************************************************************/
   public enum Probe
   {
      HEALTH, READINESS, LIVENESS
   }



   /*******************************************************************************
    ** Launch concurrently, then wait against one monotonic total budget.
    *******************************************************************************/
   public static Map<String, Object> run(QInstance instance, Probe probe)
   {
      boolean readiness = probe != Probe.HEALTH;
      ///////////////////////////////////////////////////////////////////////
      // resolve definitions before the clock starts: the first call loads //
      // the JVM-wide memory monitor, which must not eat the probe budget  //
      ///////////////////////////////////////////////////////////////////////
      List<HealthCheckMetaData> checkDefinitions = definitions(instance);
      long budgetMs = probe == Probe.READINESS ? 2000 : 1000;
      long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(budgetMs);
      Instant now = Instant.now();
      Map<String, HealthStatus> statuses = new LinkedHashMap<>();
      Map<String, Future<HealthStatus>> pending = new LinkedHashMap<>();

      /////////////////////////////////////////////////////////////////////////
      // legacy HealthIndicators keep their durationMs and (redacted) details //
      // on /health, as before; registry checks expose status only            //
      /////////////////////////////////////////////////////////////////////////
      Map<String, Map<String, Object>> legacyBodies = new ConcurrentHashMap<>();
      HealthRunner runner = HealthRunner.getForInstance(instance);
      for(HealthCheckMetaData check : checkDefinitions)
      {
         if(!includedIn(check, probe))
         {
            continue;
         }
         boolean legacyOnHealth = !readiness && check.getCheckInstance() instanceof HealthIndicatorCheck;
         String  name           = legacyOnHealth ? check.getLabel() : check.getName();
         Instant cachedAt = runner == null ? null : runner.getLastOutcomeAt(check.getName());
         HealthCheckOutcome cached = runner == null ? null : runner.getLastOutcome(check.getName());
         if(readiness && check.getProbeCache() && cached != null && cachedAt != null && cachedAt.isAfter(now.minusSeconds(2L * check.getIntervalSeconds())))
         {
            statuses.put(name, cached.getStatus());
            continue;
         }
         try
         {
            pending.put(name, POOL.submit(() ->
            {
               try
               {
                  QContext.init(new CapturedContext(instance, new QSystemUserSession(instance)));
                  HealthCheck implementation = check.getCheckInstance() == null ? QCodeLoader.getAdHoc(HealthCheck.class, check.getCheck()) : check.getCheckInstance();
                  long started = System.nanoTime();
                  HealthCheckOutcome outcome = implementation.run(new HealthCheckInput().withQInstance(instance)
                     .withDeadline(now.plusMillis(Math.min(budgetMs, check.getTimeoutMs()))).withAttempt(1));
                  if(System.nanoTime() > deadline)
                  {
                     return (HealthStatus.DOWN);
                  }
                  HealthStatus status = outcome == null || outcome.getStatus() == null ? HealthStatus.UNKNOWN : outcome.getStatus();
                  if(legacyOnHealth && outcome != null)
                  {
                     Map<String, Object> legacyBody = new LinkedHashMap<>();
                     legacyBody.put("status", status);
                     legacyBody.put("durationMs", outcome.getLatencyMs() == null ? TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) : outcome.getLatencyMs());
                     if(outcome.getDetails() != null && !outcome.getDetails().getValues().isEmpty())
                     {
                        legacyBody.put("details", new LinkedHashMap<>(outcome.getDetails().getValues()));
                     }
                     legacyBodies.put(name, legacyBody);
                  }
                  return (status);
               }
               finally
               {
                  QContext.clear();
               }
            }));
         }
         catch(RejectedExecutionException e)
         {
            statuses.put(name, HealthStatus.DOWN);
         }
      }
      for(var entry : pending.entrySet())
      {
         try
         {
            statuses.put(entry.getKey(), entry.getValue().get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS));
         }
         catch(Exception e)
         {
            entry.getValue().cancel(true);
            statuses.put(entry.getKey(), HealthStatus.DOWN);
            if(e instanceof InterruptedException)
            {
               Thread.currentThread().interrupt();
            }
         }
      }
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("status", HealthStatus.aggregate(statuses.values().toArray(HealthStatus[]::new)));
      if(!readiness)
      {
         response.put("timestamp", now);
      }
      Map<String, Object> checks = new LinkedHashMap<>();
      statuses.forEach((name, status) ->
      {
         Map<String, Object> legacyBody = legacyBodies.get(name);
         checks.put(name, readiness ? status : (legacyBody != null && legacyBody.get("status") == status ? legacyBody : Map.of("status", status)));
      });
      response.put("checks", checks);
      return (response);
   }
}
