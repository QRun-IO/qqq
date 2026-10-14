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


import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.alerts.AlertResolution;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.AlertSinkInterface;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Memory-backed acceptance tests for leases, timeouts, rollups and alerts.
 *******************************************************************************/
class HealthRunnerTest extends BaseTest
{
   private QInstance instance;
   private MutableClock clock;



   /*******************************************************************************
    ** Use the normal core fixture, memory backend and declared alert sink.
    *******************************************************************************/
   @BeforeEach
   void setup() throws QException
   {
      instance = QContext.getQInstance();
      QSessionStoreRegistry.getInstance().clear();
      new HealthMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      QContext.getQSession().withPermissions("health.read", "health.write");
      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(Sink.class));
      Sink.raises.clear();
      Sink.resolutions.clear();
      Sink.beats.set(0);
      clock = new MutableClock();
   }



   /*******************************************************************************
    ** Add a focused fixture check, then suppress unrelated built-in schedules.
    *******************************************************************************/
   private void register(HealthCheckMetaData check) throws QException
   {
      HealthChecksMetaData.ofOrWithNew(instance).withCheck(check);
      new HealthCheckSync().sync(instance);
      for(QRecord row : HealthStore.query(HealthMetaDataProvider.CHECK_TABLE, new QQueryFilter()))
      {
         if(row.getValueString("name").startsWith("core."))
         {
            HealthStore.update(HealthMetaDataProvider.CHECK_TABLE, new QRecord().withValue("id", row.getValue("id")).withValue("enabled", false));
         }
      }
   }



   /*******************************************************************************
    ** Shared check identity and component.
    *******************************************************************************/
   private HealthCheckMetaData definition(String name, HealthCheck check)
   {
      return (new HealthCheckMetaData().withName(name).withLabel("Fixture").withComponent("fixture")
         .withCategory(HealthCategory.OTHER).withKind(HealthCheckKind.DEPENDENCY).withCheckInstance(check));
   }



   /*******************************************************************************
    ** Read the test definition.
    *******************************************************************************/
   private QRecord check(String name) throws QException
   {
      return (HealthStore.find(HealthMetaDataProvider.CHECK_TABLE, "name", name));
   }



   /*******************************************************************************
    ** Read history for exactly one check.
    *******************************************************************************/
   private List<QRecord> results(String name) throws QException
   {
      return (HealthStore.query(HealthMetaDataProvider.RESULT_TABLE, new QQueryFilter()
         .withCriteria(new QFilterCriteria("healthCheckId", QCriteriaOperator.EQUALS, check(name).getValue("id")))));
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void flipAndRecoverRaisesOnceAndPersistsBothTransitions() throws Exception
   {
      AtomicInteger calls = new AtomicInteger();
      register(definition("test.flip", input -> calls.incrementAndGet() <= 2 ? HealthCheckOutcome.down("unreachable") : HealthCheckOutcome.up("reachable")));
      try(HealthRunner runner = new HealthRunner(instance, "pod-a", clock))
      {
         runner.tick();
         assertThat(check("test.flip").getValueString("currentStatus")).isEqualTo("UNKNOWN");
         clock.advance(70);
         runner.tick();
         assertThat(Sink.raises).extracting(AlertSignal::getAlertTypeName).containsExactly("health.check.down");
         assertThat(check("test.flip").getValueString("currentStatus")).isEqualTo("DOWN");
         assertThat(HealthStore.find(HealthMetaDataProvider.COMPONENT_TABLE, "name", "fixture").getValueString("status")).isEqualTo("DOWN");
         clock.advance(70);
         runner.tick();
         assertThat(check("test.flip").getValueString("currentStatus")).isEqualTo("UP");
         assertThat(HealthStore.find(HealthMetaDataProvider.COMPONENT_TABLE, "name", "fixture").getValueString("status")).isEqualTo("UP");
         assertThat(results("test.flip")).filteredOn(row -> Boolean.TRUE.equals(row.getValueBoolean("isTransition"))).hasSize(2);
         assertThat(Sink.resolutions).anySatisfy(resolution ->
         {
            assertThat(resolution.getAlertTypeName()).isEqualTo("health.check.down");
            assertThat(resolution.getNote()).startsWith("Recovered after ");
         });
         assertThat(runner.getLastOutcome("test.flip").getStatus()).isEqualTo(HealthStatus.UP);
      }
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void flappingSuppressesTransitionsAndReconcilesAfterThirtyMinutes() throws Exception
   {
      AtomicReference<HealthStatus> observed = new AtomicReference<>(HealthStatus.DOWN);
      register(definition("test.flap", input -> new HealthCheckOutcome().withStatus(observed.get()).withMessage("state changed")).withFailureThreshold(1));
      try(HealthRunner runner = new HealthRunner(instance, "pod-a", clock))
      {
         for(int i = 0; i < 6; i++)
         {
            observed.set(i % 2 == 0 ? HealthStatus.DOWN : HealthStatus.UP);
            runner.tick();
            clock.advance(70);
         }
         assertThat(check("test.flap").getValueBoolean("flapping")).isTrue();
         assertThat(Sink.raises).filteredOn(signal -> signal.getAlertTypeName().equals("health.check.flapping")).hasSize(1);
         assertThat(Sink.raises).filteredOn(signal -> signal.getAlertTypeName().equals("health.check.down")).hasSize(2);
         clock.advance(1800);
         runner.tick();
         assertThat(check("test.flap").getValueBoolean("flapping")).isFalse();
         assertThat(Sink.resolutions).anyMatch(resolution -> resolution.getAlertTypeName().equals("health.check.flapping"));
      }
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void twoPodsClaimExactlyOnceThenReapAndRunNextSlot() throws Exception
   {
      AtomicInteger calls = new AtomicInteger();
      register(definition("test.claim", input ->
      {
         calls.incrementAndGet();
         return HealthCheckOutcome.up("ready");
      }));
      try(HealthRunner first = new HealthRunner(instance, "pod-a", clock); HealthRunner second = new HealthRunner(instance, "pod-b", clock))
      {
         ExecutorService concurrent = Executors.newFixedThreadPool(2);
         try
         {
            Future<?> one = concurrent.submit(first::tick);
            Future<?> two = concurrent.submit(second::tick);
            one.get(10, TimeUnit.SECONDS);
            two.get(10, TimeUnit.SECONDS);
         }
         finally
         {
            concurrent.shutdownNow();
         }
         assertThat(calls.get()).isEqualTo(1);
         assertThat(results("test.claim")).hasSize(1);
         QRecord abandoned = HealthStore.insert(HealthMetaDataProvider.RESULT_TABLE, new QRecord().withValue("healthCheckId", check("test.claim").getValue("id"))
            .withValue("slot", 1L).withValue("onDemandRequestId", "").withValue("status", "RUNNING").withValue("runnerPod", "dead-pod")
            .withValue("startedAt", clock.instant().minusSeconds(100)).withValue("leaseExpiresAt", clock.instant().minusSeconds(1)));
         clock.advance(70);
         second.tick();
         QRecord closed = HealthStore.find(HealthMetaDataProvider.RESULT_TABLE, "id", abandoned.getValue("id"));
         assertThat(closed.getValueString("status")).isEqualTo("UNKNOWN");
         assertThat(closed.getValueString("message")).contains("abandoned by pod dead-pod");
         assertThat(closed.getValueBoolean("isTransition")).isFalse();
         assertThat(calls.get()).isEqualTo(2);
      }
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void timeoutDoesNotBlockAnotherCheckAndCannotLeakPoolCapacity() throws Exception
   {
      CountDownLatch release = new CountDownLatch(1);
      register(definition("test.hang", input ->
      {
         while(release.getCount() > 0)
         {
            try
            {
               release.await();
            }
            catch(InterruptedException ignored)
            {
               // Deliberately simulate a provider ignoring cancellation.
            }
         }
         return HealthCheckOutcome.up("late");
      }).withTimeoutMs(100).withFailureThreshold(1));
      register(definition("test.fast", input -> HealthCheckOutcome.up("fast")));
      try(HealthRunner runner = new HealthRunner(instance, "pod-a", clock))
      {
         runner.tick();
         assertThat(results("test.hang")).singleElement().satisfies(row ->
         {
            assertThat(row.getValueString("status")).isEqualTo("DOWN");
            assertThat(row.getValueString("message")).contains("timed out after 100ms");
         });
         assertThat(results("test.fast")).singleElement().satisfies(row -> assertThat(row.getValueString("status")).isEqualTo("UP"));
      }
      finally
      {
         release.countDown();
      }
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void onDemandRunsDisabledChecksWithUniqueIdsAndRestoresContext() throws Exception
   {
      register(definition("test.manual", input ->
      {
         assertThat(input.getOnDemand()).isTrue();
         assertThat(input.getDeadline()).isEqualTo(clock.instant().plusMillis(5000));
         assertThat(input.getAttempt()).isEqualTo(1);
         assertThat(input.getQInstance()).isSameAs(instance);
         assertThat(QContext.getQSession().getUser().getIdReference()).isEqualTo("health_runner");
         assertThat(QContext.getQSession().getSecurityKeyValues()).isEmpty();
         return HealthCheckOutcome.degraded("slow").withLatencyMs(42L).withDetails(new HealthDetails().withValue("latency", 42))
            .withMetrics(List.of(new HealthMetric().withName("lag").withValue(3).withUnit("seconds")));
      }).withFailureThreshold(1).withSeverity(AlertSeverity.CRITICAL));
      HealthStore.update(HealthMetaDataProvider.CHECK_TABLE, new QRecord().withValue("id", check("test.manual").getValue("id")).withValue("enabled", false));
      QSession original = QContext.getQSession();
      try(HealthRunner runner = new HealthRunner(instance, "pod-a", clock))
      {
         runner.tick();
         assertThat(results("test.manual")).isEmpty();
         assertThat(runner.runNow("test.manual").getStatus()).isEqualTo(HealthStatus.DEGRADED);
         runner.runNow("test.manual");
         assertThat(results("test.manual")).hasSize(2).allSatisfy(row ->
         {
            assertThat(row.getValueString("onDemandRequestId")).isNotBlank();
            assertThat(row.getValueString("triggeredBy")).isEqualTo("ON_DEMAND");
         });
         assertThat(Sink.raises).singleElement().satisfies(signal -> assertThat(signal.getSeverity()).isEqualTo(AlertSeverity.ERROR));
         assertThatThrownBy(() -> runner.runNow("missing")).isInstanceOf(QException.class);
      }
      assertThat(QContext.getQSession()).isSameAs(original);
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void unknownNeverTransitionsAndInfoDownCapsTheComponent() throws Exception
   {
      AtomicReference<HealthCheckOutcome> outcome = new AtomicReference<>(HealthCheckOutcome.down("down"));
      register(definition("test.info", input -> outcome.get()).withSeverity(AlertSeverity.INFO).withFailureThreshold(1));
      try(HealthRunner runner = new HealthRunner(instance, "pod-a", clock))
      {
         runner.tick();
         assertThat(HealthStore.find(HealthMetaDataProvider.COMPONENT_TABLE, "name", "fixture").getValueString("status")).isEqualTo("DEGRADED");
         outcome.set(HealthCheckOutcome.unknown("not configured"));
         clock.advance(70);
         runner.tick();
         assertThat(check("test.info").getValueString("currentStatus")).isEqualTo("DOWN");
         assertThat(results("test.info")).filteredOn(row -> Boolean.TRUE.equals(row.getValueBoolean("isTransition"))).hasSize(1);
         assertThat(Sink.raises).hasSize(1);
      }
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void retentionKeepsNewestTransitionAndDeletesInBatches() throws Exception
   {
      register(definition("test.retention", input -> HealthCheckOutcome.up("ok")));
      Serializable checkId = check("test.retention").getValue("id");
      Instant old = clock.instant().minusSeconds(40 * 86400L);
      List<QRecord> history = new ArrayList<>();
      for(int i = 0; i < 1005; i++)
      {
         history.add(new QRecord().withValue("healthCheckId", checkId).withValue("slot", (long) i).withValue("onDemandRequestId", "")
            .withValue("status", "UP").withValue("startedAt", old.plusSeconds(i)).withValue("isTransition", i == 3 || i == 1004));
      }
      new InsertAction().execute(new InsertInput().withTableName(HealthMetaDataProvider.RESULT_TABLE).withRecords(history));
      assertThat(HealthRetention.sweep(instance, clock.instant())).isEqualTo(1004);
      assertThat(results("test.retention")).singleElement().satisfies(row -> assertThat(row.getValueLong("slot")).isEqualTo(1004L));
      assertThat(HealthRetention.sweep(instance, clock.instant())).isZero();
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void lifecycleHonorsDisabledAndCleansUpAccessor() throws Exception
   {
      HealthMonitoringMetaData.ofOrWithNew(instance).setRunnerEnabled(false);
      try(HealthRunner runner = new HealthRunner(instance, "pod-a", clock))
      {
         runner.start();
         runner.tick();
         assertThat(HealthRunner.getForInstance(instance)).isNull();
      }
      HealthMonitoringMetaData.ofOrWithNew(instance).setRunnerEnabled(true);
      try(HealthRunner runner = new HealthRunner(instance, "pod-a", clock))
      {
         runner.start();
         runner.start();
         assertThat(HealthRunner.getForInstance(instance)).isSameAs(runner);
         runner.stop();
         runner.tick();
      }
      assertThat(HealthRunner.getForInstance(instance)).isNull();
   }



   /*******************************************************************************
    ** Database readiness probes continue when the health tables cannot be read.
    *******************************************************************************/
   @Test
   void databaseProbeContinuesDuringHealthStorageFailure() throws Exception
   {
      AtomicInteger calls = new AtomicInteger();
      register(definition("test.database", input ->
      {
         calls.incrementAndGet();
         return HealthCheckOutcome.down("database unavailable");
      }).withCategory(HealthCategory.DATABASE));
      try(HealthRunner runner = new HealthRunner(instance, "pod-a", clock))
      {
         instance.getTable(HealthMetaDataProvider.CHECK_TABLE).setBackendName("missing");
         runner.tick();
         assertThat(calls.get()).isEqualTo(1);
         assertThat(runner.getLastOutcome("test.database").getMessage()).isEqualTo("database unavailable");
      }
      finally
      {
         instance.getTable(HealthMetaDataProvider.CHECK_TABLE).setBackendName(TestUtils.MEMORY_BACKEND_NAME);
      }
   }



   /*******************************************************************************
    ** A completed future must still fail if it completed after its own deadline.
    *******************************************************************************/
   @Test
   void completedSlowProbeCannotEscapeItsDeadlineBehindAnotherProbe() throws Exception
   {
      register(definition("test.first", input ->
      {
         try
         {
            Thread.sleep(150);
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
         }
         return HealthCheckOutcome.up("first");
      }));
      register(definition("test.second", input ->
      {
         try
         {
            Thread.sleep(50);
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
         }
         return HealthCheckOutcome.up("late");
      }).withTimeoutMs(20));
      try(HealthRunner runner = new HealthRunner(instance, "pod-a", clock))
      {
         runner.tick();
         assertThat(results("test.first").getFirst().getValueString("status")).isEqualTo("UP");
         assertThat(results("test.second").getFirst().getValueString("status")).isEqualTo("DOWN");
         assertThat(results("test.second").getFirst().getValueString("message")).startsWith("timed out after ");
      }
   }



   /*******************************************************************************
    ** Stop must interrupt a running tick without waiting for its probe timeout.
    *******************************************************************************/
   @Test
   void stopInterruptsAnActiveTickPromptly() throws Exception
   {
      CountDownLatch entered = new CountDownLatch(1);
      CountDownLatch exited = new CountDownLatch(1);
      register(definition("test.stop", input ->
      {
         entered.countDown();
         try
         {
            new CountDownLatch(1).await();
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
         }
         finally
         {
            exited.countDown();
         }
         return HealthCheckOutcome.unknown("stopped");
      }).withTimeoutMs(30000));
      try(HealthRunner runner = new HealthRunner(instance, "pod-a", clock))
      {
         runner.start();
         assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
         runner.stop();
         assertThat(exited.await(2, TimeUnit.SECONDS)).isTrue();
      }
   }



   /*******************************************************************************
    ** Advancing clock avoids sleeping for lease and threshold behavior.
    *******************************************************************************/
   static class MutableClock extends Clock
   {
      private Instant now = Instant.parse("2026-10-14T12:00:00Z");



      /*******************************************************************************
       ** Advance deterministic wall time.
       *******************************************************************************/
      void advance(long seconds)
      {
         now = now.plusSeconds(seconds);
      }

      @Override
      public ZoneId getZone()
      {
         return (ZoneOffset.UTC);
      }

      @Override
      public Clock withZone(ZoneId zone)
      {
         return (this);
      }

      @Override
      public Instant instant()
      {
         return (now);
      }
   }



   /*******************************************************************************
    ** Captures the public alerts SPI, as a real qbit sink would.
    *******************************************************************************/
   public static class Sink implements AlertSinkInterface
   {
      static final List<AlertSignal> raises = new CopyOnWriteArrayList<>();
      static final List<AlertResolution> resolutions = new CopyOnWriteArrayList<>();
      static final AtomicInteger beats = new AtomicInteger();

      @Override
      public void raise(AlertSignal signal)
      {
         raises.add(signal);
      }

      @Override
      public void resolve(AlertResolution resolution)
      {
         resolutions.add(resolution);
      }

      @Override
      public void heartbeat(String key, Instant at)
      {
         beats.incrementAndGet();
      }
   }
}
