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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.health.HealthCheckOutcome;
import com.kingsrook.qqq.backend.core.health.HealthStatus;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.RDBMSBackendModule;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.ConnectionPoolSettings;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** C3P0 pool behaviour QQQ relies on: bounded checkout waits with a clear
 ** error, state reset between checkouts, leak detection, pool gauges in the
 ** health check, and close.
 *******************************************************************************/
class C3P0PoolHardeningTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      TestUtils.primeTestDatabase("prime-test-database.sql");
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      ConnectionManager.closeConnectionProviders();
   }



   /*******************************************************************************
    ** A pooled backend with the given settings, in a fresh QContext.
    *******************************************************************************/
   private RDBMSBackendMetaData pooledBackend(ConnectionPoolSettings settings) throws Exception
   {
      QInstance            qInstance = TestUtils.defineInstance();
      RDBMSBackendMetaData backend   = (RDBMSBackendMetaData) qInstance.getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      backend.setConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class));
      backend.setConnectionPoolSettings(settings);
      QContext.init(qInstance, new QSession());
      return (backend);
   }



   /*******************************************************************************
    ** An exhausted pool fails the checkout after the timeout, with a message
    ** naming the backend and the pool state, and counts the timeout.
    *******************************************************************************/
   @Test
   void exhaustedPoolFailsWithinTheCheckoutTimeout() throws Exception
   {
      RDBMSBackendMetaData backend = pooledBackend(new ConnectionPoolSettings().withInitialPoolSize(1).withMinPoolSize(1).withMaxPoolSize(2).withAcquireIncrement(1).withCheckoutTimeoutSeconds(1));

      try(Connection a = ConnectionManager.getConnection(backend); Connection b = ConnectionManager.getConnection(backend))
      {
         Instant start = Instant.now();
         assertThatThrownBy(() -> ConnectionManager.getConnection(backend))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("waiting for a pooled connection to backend [" + backend.getName() + "]")
            .hasMessageContaining("pool exhausted (max 2, busy 2");
         assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofSeconds(10));

         Map<String, Number> metrics = ConnectionManager.getPoolMetrics(backend);
         assertThat(metrics.get("connectionsBusy").intValue()).isEqualTo(2);
         assertThat(metrics.get("maxPoolSize").intValue()).isEqualTo(2);
         assertThat(metrics.get("checkoutTimeouts").longValue()).isEqualTo(1);
      }

      ////////////////////////////////////////
      // the pool recovers once they return //
      ////////////////////////////////////////
      try(Connection c = ConnectionManager.getConnection(backend))
      {
         assertThat(c.isValid(1)).isTrue();
      }
   }



   /*******************************************************************************
    ** Without a checkoutTimeoutSeconds setting the wait is bounded by the
    ** provider's default, not C3P0's wait-forever.
    *******************************************************************************/
   @Test
   void checkoutTimeoutDefaultsToBounded() throws Exception
   {
      RDBMSBackendMetaData backend = pooledBackend(null);
      try(Connection c = ConnectionManager.getConnection(backend))
      {
         JSONObject settings = ConnectionManager.dumpConnectionProviderDebug().getJSONObject(0).getJSONObject("values").getJSONObject("settings");
         assertThat(settings.getInt("checkoutTimeout")).isEqualTo(C3P0PooledConnectionProvider.DEFAULT_CHECKOUT_TIMEOUT_SECONDS * 1000);
      }
   }



   /*******************************************************************************
    ** A connection returned with an open transaction, autoCommit off, read-only
    ** on and a changed isolation level comes back reset, with the uncommitted
    ** work rolled back. One-connection pool, so it is the same connection.
    *******************************************************************************/
   @Test
   void connectionStateIsResetOnReturn() throws Exception
   {
      RDBMSBackendMetaData backend = pooledBackend(new ConnectionPoolSettings().withInitialPoolSize(1).withMinPoolSize(1).withMaxPoolSize(1).withAcquireIncrement(1).withCheckoutTimeoutSeconds(5));

      int defaultIsolation;
      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         defaultIsolation = connection.getTransactionIsolation();
         connection.setAutoCommit(false);
         try(Statement statement = connection.createStatement())
         {
            statement.executeUpdate("INSERT INTO person (first_name, last_name, email) VALUES ('Leaky', 'State', 'leaky@state.com')");
         }
      }

      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
         connection.setReadOnly(true);
      }

      try(Connection connection = ConnectionManager.getConnection(backend);
          Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM person WHERE email = 'leaky@state.com'"))
      {
         assertThat(connection.getAutoCommit()).isTrue();
         assertThat(connection.isReadOnly()).isFalse();
         assertThat(connection.getTransactionIsolation()).isEqualTo(defaultIsolation);
         resultSet.next();
         assertThat(resultSet.getInt(1)).isZero();
      }
   }



   /*******************************************************************************
    ** A connection held past the leak threshold is reported (metric and health
    ** DEGRADED) but not reclaimed; returning it clears the report.
    *******************************************************************************/
   @Test
   void leakDetection() throws Exception
   {
      RDBMSBackendMetaData backend = pooledBackend(new ConnectionPoolSettings().withInitialPoolSize(1).withMaxPoolSize(3).withLeakDetectionThresholdSeconds(1));

      try(Connection held = ConnectionManager.getConnection(backend))
      {
         assertThat(ConnectionManager.getPoolMetrics(backend).get("suspectedLeaks").intValue()).isZero();
         Thread.sleep(1300);
         assertThat(ConnectionManager.getPoolMetrics(backend).get("suspectedLeaks").intValue()).isEqualTo(1);
         assertThat(held.isValid(1)).isTrue();

         HealthCheckOutcome outcome = RDBMSBackendModule.withPoolMetrics(HealthCheckOutcome.up("Database reachable"), backend);
         assertThat(outcome.getStatus()).isEqualTo(HealthStatus.DEGRADED);
         assertThat(outcome.getMessage()).contains("1 suspected leaks");
         assertThat(outcome.getMetrics()).anyMatch(m -> m.getName().equals("pool_suspectedLeaks") && m.getValue().intValue() == 1);
      }

      ////////////////////////////////////////////////////////
      // C3P0 checks connections in asynchronously; allow it //
      ////////////////////////////////////////////////////////
      Instant deadline = Instant.now().plusSeconds(5);
      while(ConnectionManager.getPoolMetrics(backend).get("suspectedLeaks").intValue() > 0 && Instant.now().isBefore(deadline))
      {
         Thread.sleep(50);
      }
      assertThat(ConnectionManager.getPoolMetrics(backend).get("suspectedLeaks").intValue()).isZero();
      HealthCheckOutcome outcome = RDBMSBackendModule.withPoolMetrics(HealthCheckOutcome.up("Database reachable"), backend);
      assertThat(outcome.getStatus()).isEqualTo(HealthStatus.UP);
      assertThat(outcome.getDetails().getValues()).containsKeys("pool.connectionsBusy", "pool.connectionsIdle", "pool.threadsAwaitingCheckout", "pool.maxPoolSize");
   }



   /*******************************************************************************
    ** The default (unpooled) provider reports no pool gauges.
    *******************************************************************************/
   @Test
   void simpleProviderHasNoPoolMetrics() throws Exception
   {
      QInstance            qInstance = TestUtils.defineInstance();
      RDBMSBackendMetaData backend   = (RDBMSBackendMetaData) qInstance.getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      QContext.init(qInstance, new QSession());
      try(Connection c = ConnectionManager.getConnection(backend))
      {
         assertThat(ConnectionManager.getPoolMetrics(backend)).isEmpty();
      }
      assertThat(RDBMSBackendModule.withPoolMetrics(HealthCheckOutcome.up("ok"), backend).getMetrics()).isNullOrEmpty();
   }



   /*******************************************************************************
    ** Closing the providers closes the pool; the next use starts a new one.
    *******************************************************************************/
   @Test
   void closeConnectionProviders() throws Exception
   {
      RDBMSBackendMetaData backend = pooledBackend(new ConnectionPoolSettings().withMaxPoolSize(2));
      try(Connection c = ConnectionManager.getConnection(backend))
      {
         assertThat(ConnectionManager.getPoolMetrics(backend)).isNotEmpty();
      }

      ConnectionManager.closeConnectionProviders();
      assertThat(ConnectionManager.getPoolMetrics(backend)).isEmpty();

      try(Connection c = ConnectionManager.getConnection(backend))
      {
         assertThat(c.isValid(1)).isTrue();
      }
   }

}
