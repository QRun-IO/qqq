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


import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.module.rdbms.RDBMSBackendModule;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.PoolPressureEvaluator;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.SimpleConnectionProvider;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Deterministic production pool snapshots from issue 1273, without pool races.
 *******************************************************************************/
class RDBMSPoolHealthRegressionTest
{
   private RDBMSBackendMetaData backend;



   /*******************************************************************************
    ** Initialize the real provider seam; only its gauge readings are controlled.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      backend = TestUtils.defineBackend().withConnectionProvider(new QCodeReference(SnapshotProvider.class));
      SnapshotProvider.metrics = snapshot(0, 0, 0);
      try(Connection ignored = ConnectionManager.getConnection(backend))
      {
         // Creating the provider makes its real health-metric hook available.
      }
   }



   /*******************************************************************************
    ** Each test has a fresh provider lifetime and independent counter history.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** One queued borrower while the pool can grow is not demonstrated pressure.
    *******************************************************************************/
   @Test
   void transientWaiterBelowCapacityStaysUp()
   {
      SnapshotProvider.metrics = snapshot(1, 0, 0);
      HealthCheckOutcome outcome = RDBMSBackendModule.withPoolMetrics(HealthCheckOutcome.up("Database reachable"), backend);
      assertThat(outcome.getDetails().getValues()).containsEntry("pool.threadsAwaitingCheckout", 1);
      assertThat(outcome.getStatus()).isEqualTo(HealthStatus.UP);
   }



   /*******************************************************************************
    ** A confirmed leak remains actionable even with unused pool capacity.
    *******************************************************************************/
   @Test
   void suspectedLeakRemainsDegraded()
   {
      SnapshotProvider.metrics = snapshot(0, 1, 0);
      assertThat(RDBMSBackendModule.withPoolMetrics(HealthCheckOutcome.up("reachable"), backend).getStatus()).isEqualTo(HealthStatus.DEGRADED);
   }



   /*******************************************************************************
    ** Diagnostic pool decoration must never turn a database outage into warning.
    *******************************************************************************/
   @Test
   void poolPressureCannotDowngradeConnectionFailure()
   {
      SnapshotProvider.metrics = snapshot(1, 1, 0);
      assertThat(RDBMSBackendModule.withPoolMetrics(HealthCheckOutcome.down("Connection failed"), backend).getStatus()).isEqualTo(HealthStatus.DOWN);
   }



   /*******************************************************************************
    ** A newly failed checkout is actionable even after its waiter disappears.
    *******************************************************************************/
   @Test
   void checkoutTimeoutGrowthRemainsActionable()
   {
      RDBMSBackendModule.withPoolMetrics(HealthCheckOutcome.up("reachable"), backend);
      SnapshotProvider.metrics = snapshot(0, 0, 1);
      assertThat(RDBMSBackendModule.withPoolMetrics(HealthCheckOutcome.up("reachable"), backend).getStatus()).isEqualTo(HealthStatus.DEGRADED);
   }



   /*******************************************************************************
    ** Sustained saturation remains visible and an actual local recovery clears it.
    *******************************************************************************/
   @Test
   void repeatedSaturatedSnapshotsStayDegradedUntilLocalRecovery()
   {
      Map<String, Number> saturated = Map.of("connectionsTotal", 8, "connectionsBusy", 8, "connectionsIdle", 0,
         "maxPoolSize", 8, "threadsAwaitingCheckout", 1, "suspectedLeaks", 0, "checkoutTimeouts", 0);
      PoolPressureEvaluator evaluator = new PoolPressureEvaluator();
      assertThat(evaluator.evaluate(saturated, 0)).isEqualTo(HealthStatus.UP);
      assertThat(evaluator.evaluate(saturated, Duration.ofSeconds(29).toNanos())).isEqualTo(HealthStatus.UP);
      assertThat(evaluator.evaluate(saturated, Duration.ofSeconds(30).toNanos())).isEqualTo(HealthStatus.DEGRADED);
      assertThat(evaluator.evaluate(saturated, Duration.ofSeconds(60).toNanos())).isEqualTo(HealthStatus.DEGRADED);
      assertThat(evaluator.evaluate(snapshot(0, 0, 0), Duration.ofSeconds(61).toNanos())).isEqualTo(HealthStatus.UP);
      assertThat(evaluator.evaluate(saturated, Duration.ofSeconds(62).toNanos())).isEqualTo(HealthStatus.UP);
   }



   /*******************************************************************************
    ** Cumulative counters do not latch; reset and unavailable gauges are safe.
    *******************************************************************************/
   @Test
   void counterResetAndMissingMetricsDoNotInventRecovery()
   {
      PoolPressureEvaluator evaluator = new PoolPressureEvaluator();
      assertThat(evaluator.evaluate(snapshot(0, 0, 1), 0)).isEqualTo(HealthStatus.DEGRADED);
      assertThat(evaluator.evaluate(snapshot(0, 0, 1), 1)).isEqualTo(HealthStatus.UP);
      assertThat(evaluator.evaluate(snapshot(0, 0, 0), 2)).isEqualTo(HealthStatus.UP);
      assertThat(evaluator.evaluate(snapshot(0, 0, 1), 3)).isEqualTo(HealthStatus.DEGRADED);
      assertThat(evaluator.evaluate(Map.of(), 4)).isEqualTo(HealthStatus.UNKNOWN);
      assertThat(new PoolPressureEvaluator().evaluate(snapshot(1, 0, 0), 5)).isEqualTo(HealthStatus.UP);
   }



   /*******************************************************************************
    ** Rolling activation retains pool monitoring and then separates ownership.
    *******************************************************************************/
   @Test
   void activationSeparatesConnectivityWithoutLosingPoolMonitoring() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      HealthCheckInput input = new HealthCheckInput().withQInstance(instance).withDeadline(Instant.now().plusSeconds(10));
      RDBMSBackendModule module = new RDBMSBackendModule();
      SnapshotProvider.metrics = snapshot(0, 1, 0);
      assertThat(module.getHealthCheck(backend).run(input).getStatus()).isEqualTo(HealthStatus.DEGRADED);
      HealthChecksMetaData.ofOrWithNew(instance).withNodeLocalChecksEnabled(true);
      assertThat(module.getHealthCheck(backend).run(input).getStatus()).isEqualTo(HealthStatus.UP);
      assertThat(module.getNodeLocalHealthCheck(backend).run(input).getStatus()).isEqualTo(HealthStatus.DEGRADED);
      assertThat(module.getNodeLocalHealthCheck(TestUtils.defineBackend())).isNull();
   }



   /*******************************************************************************
    ** Reproduce four occupied connections with capacity to grow to eight.
    *******************************************************************************/
   private static Map<String, Number> snapshot(int waiting, int leaks, int timeouts)
   {
      return (Map.of("connectionsTotal", 4, "connectionsBusy", 4, "connectionsIdle", 0,
         "maxPoolSize", 8, "threadsAwaitingCheckout", waiting, "suspectedLeaks", leaks, "checkoutTimeouts", timeouts));
   }



   /*******************************************************************************
    ** H2-backed provider with deterministic pool gauges; never opens a real pool.
    *******************************************************************************/
   public static class SnapshotProvider extends SimpleConnectionProvider
   {
      private static Map<String, Number> metrics;



      /***************************************************************************
       ** Supply the observed snapshot through the normal provider contract.
       ***************************************************************************/
      @Override
      public Map<String, Number> getPoolMetrics()
      {
         return (metrics);
      }
   }
}
