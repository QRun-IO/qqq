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


import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;


/*******************************************************************************
 ** Tracks whether this JVM is under sustained memory pressure: the heap still
 ** full after garbage collection, or the JVM spending a large share of wall
 ** time in GC pauses. A JVM in that state answers every request slowly for
 ** minutes before it throws OutOfMemoryError, so probes use it to take the pod
 ** out of the load balancer (readiness) and, if it lasts, restart it
 ** (liveness).
 **
 ** Readings are taken when a probe or the health runner asks (kubelet probes
 ** every few seconds); nothing runs in the background. Each reading compares
 ** against the oldest sample still inside the window:
 ** - heap after GC is the lowest after-GC occupancy seen in the window, so
 **   one full collection that frees the heap clears the condition;
 ** - GC time is the pause time added across the window divided by its length.
 ** Pressure starts when either crosses its threshold and ends only when the
 ** latest heap after GC is below the recovery percent and GC time is below
 ** its threshold, so a JVM near the line does not flap.
 *******************************************************************************/
public final class JvmMemoryPressure
{
   private static volatile JvmMemoryPressure instance;

   private final Settings      settings;
   private final Source        source;
   private final Clock         clock;
   private final Deque<Sample> samples = new ArrayDeque<>();

   private boolean pressured;
   private Instant pressuredSince;



   /*******************************************************************************
    ** Thresholds, all from system properties or environment variables.
    **
    ** @param enabled               register the probes at all (QQQ_HEALTH_MEMORY_PRESSURE_ENABLED, default true)
    ** @param heapAfterGcPercent    pressure when heap after GC stays at or above this (QQQ_HEALTH_HEAP_AFTER_GC_PERCENT, 85)
    ** @param recoverPercent        pressure ends below this heap after GC (QQQ_HEALTH_HEAP_RECOVER_PERCENT, 75)
    ** @param gcTimePercent         pressure when GC pauses take this share of wall time (QQQ_HEALTH_GC_TIME_PERCENT, 30)
    ** @param windowSeconds         sliding window for both measures (QQQ_HEALTH_MEMORY_WINDOW_SECONDS, 30)
    ** @param readinessAfterSeconds pressure this long fails readiness (QQQ_HEALTH_MEMORY_READINESS_AFTER_SECONDS, 15)
    ** @param livenessAfterSeconds  pressure this long fails liveness (QQQ_HEALTH_MEMORY_LIVENESS_AFTER_SECONDS, 120)
    ** @param readinessDown         readiness reports DOWN (503) instead of DEGRADED (200). Off by default:
    **                              with one replica, leaving the load balancer is an outage, and liveness
    **                              still restarts a JVM that stays sick (QQQ_HEALTH_MEMORY_READINESS_ACTION=down)
    *******************************************************************************/
   public record Settings(boolean enabled, int heapAfterGcPercent, int recoverPercent, int gcTimePercent, int windowSeconds,
                          int readinessAfterSeconds, int livenessAfterSeconds, boolean readinessDown)
   {
      /***************************************************************************
       ** Defaults, safe for a single node.
       ***************************************************************************/
      public static Settings defaults()
      {
         return (new Settings(true, 85, 75, 30, 30, 15, 120, false));
      }



      /***************************************************************************
       ** Read from qqq.health.memory.* properties or QQQ_HEALTH_* variables.
       ***************************************************************************/
      public static Settings fromEnvironment(QMetaDataVariableInterpreter interpreter)
      {
         Settings defaults = defaults();
         String action = interpreter.getStringFromPropertyOrEnvironment("qqq.health.memory.readinessAction", "QQQ_HEALTH_MEMORY_READINESS_ACTION", "degrade");
         return (new Settings(
            interpreter.getBooleanFromPropertyOrEnvironment("qqq.health.memory.enabled", "QQQ_HEALTH_MEMORY_PRESSURE_ENABLED", defaults.enabled()),
            percent(interpreter.getIntegerFromPropertyOrEnvironment("qqq.health.memory.heapAfterGcPercent", "QQQ_HEALTH_HEAP_AFTER_GC_PERCENT", defaults.heapAfterGcPercent())),
            percent(interpreter.getIntegerFromPropertyOrEnvironment("qqq.health.memory.recoverPercent", "QQQ_HEALTH_HEAP_RECOVER_PERCENT", defaults.recoverPercent())),
            percent(interpreter.getIntegerFromPropertyOrEnvironment("qqq.health.memory.gcTimePercent", "QQQ_HEALTH_GC_TIME_PERCENT", defaults.gcTimePercent())),
            Math.max(5, interpreter.getIntegerFromPropertyOrEnvironment("qqq.health.memory.windowSeconds", "QQQ_HEALTH_MEMORY_WINDOW_SECONDS", defaults.windowSeconds())),
            Math.max(0, interpreter.getIntegerFromPropertyOrEnvironment("qqq.health.memory.readinessAfterSeconds", "QQQ_HEALTH_MEMORY_READINESS_AFTER_SECONDS", defaults.readinessAfterSeconds())),
            Math.max(10, interpreter.getIntegerFromPropertyOrEnvironment("qqq.health.memory.livenessAfterSeconds", "QQQ_HEALTH_MEMORY_LIVENESS_AFTER_SECONDS", defaults.livenessAfterSeconds())),
            "down".equals(Objects.toString(action, "").trim().toLowerCase(Locale.ROOT))));
      }



      /***************************************************************************
       ** Clamp to 1..100.
       ***************************************************************************/
      private static int percent(Integer value)
      {
         return (Math.max(1, Math.min(100, value == null ? 100 : value)));
      }
   }



   /*******************************************************************************
    ** Where readings come from: the platform MXBeans, or a test double.
    *******************************************************************************/
   public interface Source
   {
      /***************************************************************************
       ** Heap used after the most recent collection, as a percent of the maximum
       ** heap; null when the JVM does not report it.
       ***************************************************************************/
      Double heapAfterGcPercent();

      /***************************************************************************
       ** Cumulative GC pause time in milliseconds (concurrent cycles excluded).
       ***************************************************************************/
      long gcPauseMillis();
   }



   /*******************************************************************************
    ** One evaluation.
    *******************************************************************************/
   public record Reading(Instant at, Double heapAfterGcPercent, double gcTimePercent, boolean pressured, Instant pressuredSince)
   {
      /***************************************************************************
       ** How long pressure has lasted at this reading; zero when not pressured.
       ***************************************************************************/
      public Duration pressuredFor()
      {
         return (pressured && pressuredSince != null ? Duration.between(pressuredSince, at) : Duration.ZERO);
      }
   }



   /*******************************************************************************
    ** A raw sample kept for the sliding window.
    *******************************************************************************/
   private record Sample(Instant at, Double heapAfterGcPercent, long gcPauseMillis)
   {
   }



   /*******************************************************************************
    ** Constructor (tests inject a source and clock).
    *******************************************************************************/
   public JvmMemoryPressure(Settings settings, Source source, Clock clock)
   {
      this.settings = settings;
      this.source = source;
      this.clock = clock;
   }



   /*******************************************************************************
    ** The JVM-wide monitor, configured from the environment on first use.
    *******************************************************************************/
   public static JvmMemoryPressure get()
   {
      JvmMemoryPressure current = instance;
      if(current == null)
      {
         synchronized(JvmMemoryPressure.class)
         {
            if(instance == null)
            {
               instance = new JvmMemoryPressure(Settings.fromEnvironment(new QMetaDataVariableInterpreter()), new PlatformSource(), Clock.systemUTC());
            }
            current = instance;
         }
      }
      return (current);
   }



   /*******************************************************************************
    ** Replace the JVM-wide monitor (tests); null re-reads the environment.
    *******************************************************************************/
   public static void set(JvmMemoryPressure monitor)
   {
      instance = monitor;
   }



   /*******************************************************************************
    ** Getter for settings
    *******************************************************************************/
   public Settings getSettings()
   {
      return (settings);
   }



   /*******************************************************************************
    ** Take a sample and evaluate the window.
    *******************************************************************************/
   public synchronized Reading sample()
   {
      Instant now  = clock.instant();
      Double  heap = source.heapAfterGcPercent();
      long    gc   = source.gcPauseMillis();
      samples.addLast(new Sample(now, heap, gc));

      ///////////////////////////////////////////////////////////////////////////
      // keep one sample at or before the window start as the GC-time baseline //
      ///////////////////////////////////////////////////////////////////////////
      Instant windowStart = now.minusSeconds(settings.windowSeconds());
      while(samples.size() > 2 && !samples.stream().skip(1).findFirst().orElseThrow().at().isAfter(windowStart))
      {
         samples.removeFirst();
      }

      Sample oldest    = samples.getFirst();
      long   elapsedMs = Duration.between(oldest.at(), now).toMillis();
      double gcPercent = elapsedMs < 1000 ? 0 : Math.min(100, 100.0 * Math.max(0, gc - oldest.gcPauseMillis()) / elapsedMs);

      //////////////////////////////////////////////////////////////////////
      // lowest after-GC occupancy in the window (the baseline sample may //
      // predate the window, so it is left out once others exist)         //
      //////////////////////////////////////////////////////////////////////
      Double lowestHeap = samples.stream().skip(samples.size() > 1 && oldest.at().isBefore(windowStart) ? 1 : 0)
         .map(Sample::heapAfterGcPercent).filter(Objects::nonNull).min(Double::compare).orElse(null);
      boolean windowFilled = elapsedMs >= settings.windowSeconds() * 500L;
      boolean gcHigh = windowFilled && gcPercent >= settings.gcTimePercent();

      if(!pressured)
      {
         boolean heapHigh = windowFilled && lowestHeap != null && lowestHeap >= settings.heapAfterGcPercent();
         if(heapHigh || gcHigh)
         {
            pressured = true;
            pressuredSince = now;
         }
      }
      else
      {
         boolean heapRecovered = heap == null || heap < settings.recoverPercent();
         if(heapRecovered && !gcHigh)
         {
            pressured = false;
            pressuredSince = null;
         }
      }
      return (new Reading(now, heap, gcPercent, pressured, pressuredSince));
   }



   /*******************************************************************************
    ** Platform MXBeans: summed after-GC usage of heap pools, and pause time of
    ** collectors that stop the world.
    *******************************************************************************/
   public static class PlatformSource implements Source
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Double heapAfterGcPercent()
      {
         long max = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
         if(max <= 0)
         {
            max = Runtime.getRuntime().maxMemory();
         }
         long    used     = 0;
         boolean reported = false;
         for(MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans())
         {
            if(pool.getType() == MemoryType.HEAP && pool.isValid())
            {
               MemoryUsage afterGc = pool.getCollectionUsage();
               if(afterGc != null)
               {
                  used += afterGc.getUsed();
                  reported = true;
               }
            }
         }
         return (reported && max > 0 ? 100.0 * used / max : null);
      }



      /***************************************************************************
       ** G1 "Concurrent GC" and ZGC/Shenandoah "Cycles" beans time concurrent
       ** work, not pauses, so they are left out.
       ***************************************************************************/
      @Override
      public long gcPauseMillis()
      {
         long total = 0;
         for(GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans())
         {
            String name = collector.getName();
            if(name.contains("Concurrent") || name.contains("Cycles"))
            {
               continue;
            }
            total += Math.max(0, collector.getCollectionTime());
         }
         return (total);
      }
   }
}
