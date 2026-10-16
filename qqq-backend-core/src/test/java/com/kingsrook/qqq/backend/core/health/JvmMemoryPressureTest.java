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


import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Sustained memory pressure fails readiness, then liveness; brief spikes and
 ** a JVM near the line do not flap. Modeled on the kof22 web pod that served
 ** 4-6 s responses in GC thrash before OutOfMemoryError (2026-10-16).
 *******************************************************************************/
class JvmMemoryPressureTest
{
   private final FakeSource source = new FakeSource();
   private final MutableClock clock = new MutableClock();



   /*******************************************************************************
    ** Leave the JVM-wide monitor as found.
    *******************************************************************************/
   @AfterEach
   void reset()
   {
      JvmMemoryPressure.set(null);
      System.clearProperty("qqq.health.memory.heapAfterGcPercent");
      System.clearProperty("qqq.health.memory.readinessAction");
      System.clearProperty("qqq.health.memory.livenessAfterSeconds");
   }



   /*******************************************************************************
    ** A healthy JVM is UP on both views.
    *******************************************************************************/
   @Test
   void healthyIsUp()
   {
      JvmMemoryPressure monitor = monitor(JvmMemoryPressure.Settings.defaults());
      source.heap = 40.0;
      for(int i = 0; i < 20; i++)
      {
         step(monitor, 5, 10);
      }
      assertThat(readiness(monitor).getStatus()).isEqualTo(HealthStatus.UP);
      assertThat(liveness(monitor).getStatus()).isEqualTo(HealthStatus.UP);
   }



   /*******************************************************************************
    ** Heap full after GC: readiness degrades, goes DOWN when the host opted
    ** in, and liveness fails after the sustained limit.
    *******************************************************************************/
   @Test
   void sustainedHeapFailsReadinessThenLiveness()
   {
      JvmMemoryPressure monitor = monitor(withReadinessDown(JvmMemoryPressure.Settings.defaults()));
      source.heap = 92.0;
      JvmMemoryPressure.Reading reading = null;
      for(int seconds = 0; seconds <= 15; seconds += 5)
      {
         reading = step(monitor, 5, 0);
      }
      assertThat(reading.pressured()).as("half a window of samples before judging").isTrue();

      assertThat(readiness(monitor).getStatus()).isEqualTo(HealthStatus.DEGRADED);
      advance(monitor, 20);
      assertThat(readiness(monitor).getStatus()).isEqualTo(HealthStatus.DOWN);
      assertThat(liveness(monitor).getStatus()).isEqualTo(HealthStatus.DEGRADED);
      advance(monitor, 120);
      HealthCheckOutcome live = liveness(monitor);
      assertThat(live.getStatus()).isEqualTo(HealthStatus.DOWN);
      assertThat(live.getMessage()).contains("restart needed", "heap after GC 92%");
      assertThat(live.getDetails().getValues()).containsEntry("heapAfterGcPercent", 92L).containsEntry("pressured", true);
   }



   /*******************************************************************************
    ** Single-node default: readiness never leaves the load balancer; liveness
    ** still restarts a JVM that stays sick.
    *******************************************************************************/
   @Test
   void singleNodeDefaultOnlyDegradesReadiness()
   {
      JvmMemoryPressure monitor = monitor(JvmMemoryPressure.Settings.defaults());
      source.heap = 95.0;
      advance(monitor, 300);
      assertThat(readiness(monitor).getStatus()).isEqualTo(HealthStatus.DEGRADED);
      assertThat(liveness(monitor).getStatus()).isEqualTo(HealthStatus.DOWN);
   }



   /*******************************************************************************
    ** GC thrash alone (heap reading unavailable) is pressure.
    *******************************************************************************/
   @Test
   void gcTimeAloneIsPressure()
   {
      JvmMemoryPressure monitor = monitor(JvmMemoryPressure.Settings.defaults());
      source.heap = null;
      JvmMemoryPressure.Reading reading = null;
      for(int i = 0; i < 6; i++)
      {
         reading = step(monitor, 5, 2500);
      }
      assertThat(reading.gcTimePercent()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(0.1));
      assertThat(reading.pressured()).isTrue();
   }



   /*******************************************************************************
    ** One collection that frees the heap inside the window prevents pressure.
    *******************************************************************************/
   @Test
   void oneGoodCollectionInWindowIsNotPressure()
   {
      JvmMemoryPressure monitor = monitor(JvmMemoryPressure.Settings.defaults());
      source.heap = 90.0;
      step(monitor, 5, 0);
      source.heap = 50.0;
      step(monitor, 5, 0);
      source.heap = 90.0;
      JvmMemoryPressure.Reading reading = null;
      for(int i = 0; i < 4; i++)
      {
         reading = step(monitor, 5, 0);
      }
      assertThat(reading.pressured()).isFalse();
   }



   /*******************************************************************************
    ** Recovery needs heap below the recovery percent, not just below the
    ** threshold, so a JVM hovering near the line does not flap.
    *******************************************************************************/
   @Test
   void recoveryHasHysteresis()
   {
      JvmMemoryPressure monitor = monitor(JvmMemoryPressure.Settings.defaults());
      source.heap = 90.0;
      advance(monitor, 30);
      assertThat(monitor.sample().pressured()).isTrue();
      source.heap = 80.0;
      assertThat(step(monitor, 5, 0).pressured()).isTrue();
      source.heap = 60.0;
      assertThat(step(monitor, 5, 0).pressured()).isFalse();
      assertThat(liveness(monitor).getStatus()).isEqualTo(HealthStatus.UP);
   }



   /*******************************************************************************
    ** Thresholds come from qqq.health.memory.* properties or QQQ_HEALTH_* variables.
    *******************************************************************************/
   @Test
   void settingsFromEnvironment()
   {
      System.setProperty("qqq.health.memory.heapAfterGcPercent", "90");
      System.setProperty("qqq.health.memory.readinessAction", "down");
      System.setProperty("qqq.health.memory.livenessAfterSeconds", "60");
      JvmMemoryPressure.Settings settings = JvmMemoryPressure.Settings.fromEnvironment(new QMetaDataVariableInterpreter());
      assertThat(settings.heapAfterGcPercent()).isEqualTo(90);
      assertThat(settings.readinessDown()).isTrue();
      assertThat(settings.livenessAfterSeconds()).isEqualTo(60);
      assertThat(JvmMemoryPressure.Settings.defaults().readinessDown()).isFalse();
   }



   /*******************************************************************************
    ** The platform source reports plausible numbers on this JVM.
    *******************************************************************************/
   @Test
   void platformSource()
   {
      System.gc();
      JvmMemoryPressure.PlatformSource platform = new JvmMemoryPressure.PlatformSource();
      Double heap = platform.heapAfterGcPercent();
      if(heap != null)
      {
         assertThat(heap).isBetween(0.0, 100.0);
      }
      assertThat(platform.gcPauseMillis()).isGreaterThanOrEqualTo(0);
      assertThat(new JvmMemoryPressureCheck(HealthCheckKind.LIVENESS).run(new HealthCheckInput()).getStatus()).isEqualTo(HealthStatus.UP);
   }



   /*******************************************************************************
    ** Both core definitions are probe-only-live and flagged for their probes.
    *******************************************************************************/
   @Test
   void definitions()
   {
      assertThat(JvmMemoryPressureCheck.definitions()).extracting(HealthCheckMetaData::getName)
         .containsExactly(JvmMemoryPressureCheck.READINESS_NAME, JvmMemoryPressureCheck.LIVENESS_NAME);
      assertThat(JvmMemoryPressureCheck.definitions()).allSatisfy(definition -> assertThat(definition.getProbeCache()).isFalse());
      assertThat(JvmMemoryPressureCheck.definitions().get(0).getIncludeInReadiness()).isTrue();
      assertThat(JvmMemoryPressureCheck.definitions().get(0).getIncludeInLivenessProbe()).isFalse();
      assertThat(JvmMemoryPressureCheck.definitions().get(1).getIncludeInLivenessProbe()).isTrue();
   }



   /*******************************************************************************
    ** Monitor on the fake source and clock.
    *******************************************************************************/
   private JvmMemoryPressure monitor(JvmMemoryPressure.Settings settings)
   {
      return (new JvmMemoryPressure(settings, source, clock));
   }



   /*******************************************************************************
    ** Copy with readiness DOWN enabled.
    *******************************************************************************/
   private static JvmMemoryPressure.Settings withReadinessDown(JvmMemoryPressure.Settings s)
   {
      return (new JvmMemoryPressure.Settings(s.enabled(), s.heapAfterGcPercent(), s.recoverPercent(), s.gcTimePercent(), s.windowSeconds(),
         s.readinessAfterSeconds(), s.livenessAfterSeconds(), true));
   }



   /*******************************************************************************
    ** Advance the clock, add GC pause time, sample.
    *******************************************************************************/
   private JvmMemoryPressure.Reading step(JvmMemoryPressure monitor, int seconds, long gcMillis)
   {
      clock.now = clock.now.plusSeconds(seconds);
      source.gc += gcMillis;
      return (monitor.sample());
   }



   /*******************************************************************************
    ** Probe every 5 seconds for a while at the current readings.
    *******************************************************************************/
   private void advance(JvmMemoryPressure monitor, int seconds)
   {
      for(int i = 0; i < seconds; i += 5)
      {
         step(monitor, 5, 0);
      }
   }



   /*******************************************************************************
    ** Run the readiness view.
    *******************************************************************************/
   private static HealthCheckOutcome readiness(JvmMemoryPressure monitor)
   {
      return (new JvmMemoryPressureCheck(HealthCheckKind.READINESS, () -> monitor).run(new HealthCheckInput()));
   }



   /*******************************************************************************
    ** Run the liveness view.
    *******************************************************************************/
   private static HealthCheckOutcome liveness(JvmMemoryPressure monitor)
   {
      return (new JvmMemoryPressureCheck(HealthCheckKind.LIVENESS, () -> monitor).run(new HealthCheckInput()));
   }



   /*******************************************************************************
    ** Settable readings.
    *******************************************************************************/
   private static class FakeSource implements JvmMemoryPressure.Source
   {
      private Double heap = 40.0;
      private long   gc   = 0;



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Double heapAfterGcPercent()
      {
         return (heap);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public long gcPauseMillis()
      {
         return (gc);
      }
   }



   /*******************************************************************************
    ** Settable clock.
    *******************************************************************************/
   private static class MutableClock extends Clock
   {
      private Instant now = Instant.parse("2026-10-16T19:00:00Z");



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public ZoneId getZone()
      {
         return (ZoneOffset.UTC);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Clock withZone(ZoneId zone)
      {
         return (this);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Instant instant()
      {
         return (now);
      }
   }
}
