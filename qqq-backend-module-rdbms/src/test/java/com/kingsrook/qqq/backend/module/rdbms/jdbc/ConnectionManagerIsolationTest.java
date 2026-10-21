/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.health.HealthCheckInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.module.rdbms.RDBMSBackendModule;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.ConnectionPoolSettings;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Owned databases exercise connection-provider isolation without external services.
 *******************************************************************************/
class ConnectionManagerIsolationTest
{
   /*******************************************************************************
    ** Failed native-pool customization closes its pool and removes callback state.
    *******************************************************************************/
   @Test
   void failedPoolInitializationReleasesNativeResourcesAndCallbacks() throws Exception
   {
      CapturedContext previous = QContext.capture();
      QInstance owner = TestUtils.defineInstance();
      try
      {
         RDBMSBackendMetaData backend = isolatedPool(owner, "failed");
         backend.setConnectionProvider(new QCodeReference(FailingPoolCustomization.class));
         assertThrows(java.sql.SQLException.class, () -> ConnectionManager.getConnection(owner, backend));
         ComboPooledDataSource failed = FailingPoolCustomization.allocated;
         assertTrue(failed != null);
         assertThrows(java.sql.SQLException.class, failed::getConnection);
         assertTrue(ConnectionManager.getPoolMetrics(owner, backend).isEmpty());
         try(Connection probe = DriverManager.getConnection("jdbc:h2:mem:callback_cleanup_" + UUID.randomUUID(), "sa", ""))
         {
            new BaseC3P0ConnectionCustomizer().onAcquire(probe, failed.getIdentityToken());
            assertEquals(null, connectionMarker(probe), "A failed pool must leave no initialization callback registered");
         }
         backend.setConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class));
         try(Connection retried = ConnectionManager.getConnection(owner, backend))
         {
            assertEquals("failed", connectionMarker(retried));
         }
      }
      finally
      {
         ConnectionManager.closeConnectionProviders(owner);
         FailingPoolCustomization.allocated = null;
         QContext.init(previous);
      }
   }



   /*******************************************************************************
    ** Capture the real native pool before deliberately failing its customization.
    *******************************************************************************/
   public static class FailingPoolCustomization extends C3P0PooledConnectionProvider
   {
      private static ComboPooledDataSource allocated;



      /*******************************************************************************
       ** Exercise failure after callback registration but before provider publication.
       *******************************************************************************/
      @Override
      protected void customizePool(ComboPooledDataSource pool)
      {
         allocated = pool;
         throw new IllegalStateException("Owned pool customization failure");
      }
   }



   /*******************************************************************************
    ** New physical connections retain their own callbacks across peer startup/shutdown.
    *******************************************************************************/
   @Test
   void sameNamePoolsKeepConnectionInitializationIsolated() throws Exception
   {
      CapturedContext previous = QContext.capture();
      ConnectionManager.resetConnectionProviders();
      QInstance first = TestUtils.defineInstance();
      QInstance second = TestUtils.defineInstance();
      try
      {
         RDBMSBackendMetaData firstBackend = isolatedPool(first, "first");
         RDBMSBackendMetaData secondBackend = isolatedPool(second, "second");
         firstBackend.getConnectionPoolSettings().setLeakDetectionThresholdSeconds(1);
         try(Connection firstConnection = ConnectionManager.getConnection(first, firstBackend))
         {
            assertEquals("first", connectionMarker(firstConnection));
            try(Connection secondConnection = ConnectionManager.getConnection(second, secondBackend))
            {
               assertEquals("second", connectionMarker(secondConnection));
            }
            assertNotEquals(ConnectionManager.dumpConnectionProviderDebug(first).getJSONObject(0).getJSONObject("values").getString("poolIdentityToken"),
               ConnectionManager.dumpConnectionProviderDebug(second).getJSONObject(0).getJSONObject("values").getString("poolIdentityToken"));
            try(Connection expanded = ConnectionManager.getConnection(first, firstBackend))
            {
               assertEquals("first", connectionMarker(expanded), "Peer startup must not replace this pool's initialization query");
               Thread.sleep(1100);
               assertTrue(ConnectionManager.getPoolMetrics(first, firstBackend).get("suspectedLeaks").intValue() >= 2,
                  "The peer's disabled leak detection must not disable this pool's tracking");
               ConnectionManager.closeConnectionProviders(second);
               assertTrue(ConnectionManager.getPoolMetrics(first, firstBackend).get("suspectedLeaks").intValue() >= 2,
                  "Peer shutdown must retain this pool's outstanding checkouts");
               try(Connection afterPeerClose = ConnectionManager.getConnection(first, firstBackend))
               {
                  assertEquals("first", connectionMarker(afterPeerClose), "Peer shutdown must not remove this pool's initialization query");
               }
            }
         }
      }
      finally
      {
         ConnectionManager.closeConnectionProviders(first);
         ConnectionManager.closeConnectionProviders(second);
         QContext.init(previous);
      }
   }



   /*******************************************************************************
    ** One initial connection; holding it forces later borrows to allocate new sockets.
    *******************************************************************************/
   private RDBMSBackendMetaData isolatedPool(QInstance instance, String marker)
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) instance.getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      backend.setJdbcUrl("jdbc:h2:mem:callback_" + marker + "_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=0");
      backend.setConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class));
      backend.setConnectionPoolSettings(new ConnectionPoolSettings().withInitialPoolSize(1).withMinPoolSize(1)
         .withMaxPoolSize(3).withAcquireIncrement(1).withCheckoutTimeoutSeconds(2));
      backend.setQueriesForNewConnections(List.of("SET @owned_pool = '" + marker + "'"));
      return backend;
   }



   /*******************************************************************************
    ** Observe state on the physical JDBC connection, not the customizer registry.
    *******************************************************************************/
   private String connectionMarker(Connection connection) throws Exception
   {
      try(var statement = connection.createStatement(); var result = statement.executeQuery("CALL @owned_pool"))
      {
         assertTrue(result.next());
         return result.getString(1);
      }
   }



   /*******************************************************************************
    ** Metrics, shutdown and restart use the explicit owner, not the calling thread.
    *******************************************************************************/
   @Test
   void ownedLifecycleAndMetrics() throws Exception
   {
      CapturedContext previous = QContext.capture();
      ConnectionManager.resetConnectionProviders();
      TrackingProvider.created.clear();
      try
      {
         QInstance first = TestUtils.defineInstance();
         QInstance second = TestUtils.defineInstance();
         RDBMSBackendMetaData firstBackend = trackedBackend(first, "first");
         RDBMSBackendMetaData secondBackend = trackedBackend(second, "second");
         QContext.setQInstance(second);
         assertTrue(ConnectionManager.getPoolMetrics(first, firstBackend).isEmpty());
         assertTrue(TrackingProvider.created.isEmpty());
         try(Connection firstConnection = ConnectionManager.getConnection(first, firstBackend);
             Connection secondConnection = ConnectionManager.getConnection(second, secondBackend))
         {
            assertNotEquals(firstConnection.getMetaData().getURL(), secondConnection.getMetaData().getURL());
            assertEquals(second, QContext.getQInstance());
            TrackingProvider firstProvider = TrackingProvider.created.get(0);
            TrackingProvider secondProvider = TrackingProvider.created.get(1);
            assertEquals(firstProvider.marker, ConnectionManager.getPoolMetrics(first, firstBackend).get("marker"));
            assertEquals(secondProvider.marker, ConnectionManager.getPoolMetrics(secondBackend).get("marker"));
            var health = new RDBMSBackendModule().getNodeLocalHealthCheck(firstBackend)
               .run(new HealthCheckInput().withQInstance(first));
            assertEquals(firstProvider.marker, health.getMetrics().stream().filter(metric -> "pool_marker".equals(metric.getName()))
               .findFirst().orElseThrow().getValue());
            assertEquals(second, QContext.getQInstance());
            assertEquals(1, ConnectionManager.dumpConnectionProviderDebug(first).length());
            ConnectionManager.closeConnectionProviders(first);
            ConnectionManager.closeConnectionProviders(first);
            assertEquals(1, firstProvider.closeCount);
            assertEquals(0, secondProvider.closeCount);
            assertTrue(ConnectionManager.getPoolMetrics(first, firstBackend).isEmpty());
            assertFalse(ConnectionManager.getPoolMetrics(second, secondBackend).isEmpty());
            try(Connection restarted = ConnectionManager.getConnection(first, firstBackend))
            {
               assertEquals(firstConnection.getMetaData().getURL(), restarted.getMetaData().getURL());
               assertEquals(3, TrackingProvider.created.size());
            }
            ConnectionManager.closeConnectionProviders();
            assertEquals(1, secondProvider.closeCount);
            assertEquals(0, TrackingProvider.created.get(2).closeCount);
         }
      }
      finally
      {
         ConnectionManager.resetConnectionProviders();
         TrackingProvider.created.clear();
         QContext.init(previous);
      }
   }



   /*******************************************************************************
    ** A partially initialized provider is closed, never published, and can retry.
    *******************************************************************************/
   @Test
   void failedInitializationClosesUnpublishedProvider() throws Exception
   {
      CapturedContext previous = QContext.capture();
      ConnectionManager.resetConnectionProviders();
      TrackingProvider.created.clear();
      try
      {
         QInstance owner = TestUtils.defineInstance();
         RDBMSBackendMetaData backend = trackedBackend(owner, "failure");
         backend.setDatabaseName("fail-owned-initialization");
         assertThrows(java.sql.SQLException.class, () -> ConnectionManager.getConnection(owner, backend));
         assertEquals(1, TrackingProvider.created.get(0).closeCount);
         assertTrue(ConnectionManager.getPoolMetrics(owner, backend).isEmpty());
         backend.setDatabaseName("recovered");
         try(Connection connection = ConnectionManager.getConnection(owner, backend))
         {
            assertFalse(connection.isClosed());
            assertEquals(2, TrackingProvider.created.size());
         }
      }
      finally
      {
         ConnectionManager.resetConnectionProviders();
         TrackingProvider.created.clear();
         QContext.init(previous);
      }
   }



   /*******************************************************************************
    ** Keep fixtures on owned databases and the real JDBC connection path.
    *******************************************************************************/
   private RDBMSBackendMetaData trackedBackend(QInstance instance, String suffix)
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) instance.getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      backend.setJdbcUrl("jdbc:h2:mem:tracked_" + suffix + "_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=0");
      backend.setConnectionProvider(new QCodeReference(TrackingProvider.class));
      return backend;
   }



   /*******************************************************************************
    ** Concurrent first use publishes exactly one provider for the owning application.
    *******************************************************************************/
   @Test
   void concurrentInitializationUsesOneProvider() throws Exception
   {
      CapturedContext previous = QContext.capture();
      ConnectionManager.resetConnectionProviders();
      TrackingProvider.created.clear();
      try(var executor = Executors.newFixedThreadPool(4))
      {
         QInstance owner = TestUtils.defineInstance();
         RDBMSBackendMetaData backend = trackedBackend(owner, "concurrent");
         List<Callable<String>> calls = new ArrayList<>();
         for(int i = 0; i < 16; i++)
         {
            calls.add(() ->
            {
               try(Connection connection = ConnectionManager.getConnection(owner, backend))
               {
                  return connection.getMetaData().getURL();
               }
            });
         }
         var results = executor.invokeAll(calls);
         String expected = results.get(0).get();
         for(var result : results)
         {
            assertEquals(expected, result.get());
         }
         assertEquals(1, TrackingProvider.created.size());
      }
      finally
      {
         ConnectionManager.resetConnectionProviders();
         assertTrue(TrackingProvider.created.stream().allMatch(provider -> provider.closeCount == 1));
         TrackingProvider.created.clear();
         QContext.init(previous);
      }
   }



   /*******************************************************************************
    ** Count resource ownership while delegating actual connections to the simple provider.
    *******************************************************************************/
   public static class TrackingProvider implements ConnectionProviderInterface
   {
      private static final List<TrackingProvider> created = new ArrayList<>();
      private final SimpleConnectionProvider delegate = new SimpleConnectionProvider();
      private int marker;
      private int closeCount;



      /*******************************************************************************
       ** Simulate an allocated resource before an optional initialization failure.
       *******************************************************************************/
      @Override
      public void init(RDBMSBackendMetaData backend) throws QException
      {
         created.add(this);
         marker = created.size();
         delegate.init(backend);
         if("fail-owned-initialization".equals(backend.getDatabaseName()))
         {
            throw new QException("Owned initialization failure");
         }
      }



      /*******************************************************************************
       ** Use a real JDBC connection and reject use after provider shutdown.
       *******************************************************************************/
      @Override
      public Connection getConnection() throws java.sql.SQLException
      {
         if(closeCount != 0)
         {
            throw new java.sql.SQLException("Owned provider closed");
         }
         return delegate.getConnection();
      }



      /*******************************************************************************
       ** Distinguish otherwise identically named providers without exposing config.
       *******************************************************************************/
      @Override
      public Map<String, Number> getPoolMetrics()
      {
         return Map.of("marker", marker);
      }



      /*******************************************************************************
       ** Count every close, including unpublished failed providers.
       *******************************************************************************/
      @Override
      public void close()
      {
         closeCount++;
      }
   }



   /*******************************************************************************
    ** Identical backend names in different applications must select different databases.
    *******************************************************************************/
   @Test
   void applicationsWithSameBackendNameUseTheirOwnDatabase() throws Exception
   {
      CapturedContext previous = QContext.capture();
      ConnectionManager.resetConnectionProviders();
      try
      {
         QInstance first = TestUtils.defineInstance();
         QInstance second = TestUtils.defineInstance();
         RDBMSBackendMetaData firstBackend = (RDBMSBackendMetaData) first.getBackend(TestUtils.DEFAULT_BACKEND_NAME);
         RDBMSBackendMetaData secondBackend = (RDBMSBackendMetaData) second.getBackend(TestUtils.DEFAULT_BACKEND_NAME);
         firstBackend.setJdbcUrl("jdbc:h2:mem:owner_a_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=0");
         secondBackend.setJdbcUrl("jdbc:h2:mem:owner_b_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=0");
         QContext.setQInstance(first);
         try(Connection firstConnection = ConnectionManager.getConnection(firstBackend))
         {
            QContext.setQInstance(second);
            try(Connection secondConnection = ConnectionManager.getConnection(secondBackend))
            {
               assertNotEquals(firstConnection.getMetaData().getURL(), secondConnection.getMetaData().getURL(),
                  "Application B must not borrow application A's connection provider");
            }
         }
      }
      finally
      {
         ConnectionManager.resetConnectionProviders();
         QContext.init(previous);
      }
   }
}
