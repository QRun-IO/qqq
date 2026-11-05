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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for how ConnectionManager scopes its connection providers - with
 ** real H2 databases: QInstances (and configurations) that use the same backend
 ** name for different databases must not share a provider.
 *******************************************************************************/
class ConnectionManagerScopingTest extends BaseTest
{
   private static final String PASSWORD_THAT_MUST_NOT_LEAK = "s3cr3t-pw-xyz";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      ConnectionManager.closeConnectionProviders();
      CountingProvider.reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      ConnectionManager.closeConnectionProviders();
      CountingProvider.reset();
   }



   /*******************************************************************************
    ** two instances, same backend name, different urls - each gets its own database
    ** (and the second one's create-table would fail if it reached the first's).
    *******************************************************************************/
   @Test
   void testSameBackendNameDifferentUrlsInDifferentInstances() throws SQLException
   {
      QInstance instanceA = newInstance(newDatabaseName("a"), false);
      QInstance instanceB = newInstance(newDatabaseName("b"), false);

      writeRow(instanceA, "A");
      writeRow(instanceB, "B");

      assertEquals(List.of("A"), readRows(instanceA));
      assertEquals(List.of("B"), readRows(instanceB));

      ///////////////////////////////////////////////////////////////////
      // and that holds, going back and forth, in either order         //
      ///////////////////////////////////////////////////////////////////
      assertEquals(List.of("B"), readRows(instanceB));
      assertEquals(List.of("A"), readRows(instanceA));
      assertTrue(getUrl(instanceA).contains("_a_"));
      assertTrue(getUrl(instanceB).contains("_b_"));
   }



   /*******************************************************************************
    ** a second instance with the same configuration is a different provider, but
    ** (as it should be) reaches the same database.
    *******************************************************************************/
   @Test
   void testSameConfigurationInDifferentInstancesReachesSameDatabase() throws SQLException
   {
      String    databaseName = newDatabaseName("shared");
      QInstance instanceA    = newInstance(databaseName, true);
      QInstance instanceB    = newInstance(databaseName, true);

      writeRow(instanceA, "shared");
      assertEquals(List.of("shared"), readRows(instanceB));
      assertEquals(2, CountingProvider.INITS.get());
   }



   /*******************************************************************************
    ** in one instance, backends with the same name but different configuration
    ** (here, a different url) don't share a provider either - nor does one close
    ** the other's.
    *******************************************************************************/
   @Test
   void testSameBackendNameDifferentConfigurationInOneInstance() throws SQLException
   {
      QInstance instance = newInstance(newDatabaseName("x"), true);
      QContext.init(instance, new QSession());

      RDBMSBackendMetaData backendX = backend(instance);
      RDBMSBackendMetaData backendY = copyOf(backendX).withJdbcUrl(url(newDatabaseName("y")));

      writeRow(backendX, "X");
      writeRow(backendY, "Y");
      assertEquals(List.of("X"), readRows(backendX));
      assertEquals(List.of("Y"), readRows(backendY));
      assertEquals(2, CountingProvider.INITS.get());

      ///////////////////////////////////////////////////////////////////////////////////
      // an equal config (a different object) re-uses the provider - no third init     //
      ///////////////////////////////////////////////////////////////////////////////////
      assertEquals(List.of("X"), readRows(copyOf(backendX)));
      assertEquals(2, CountingProvider.INITS.get());
      assertEquals(0, CountingProvider.CLOSES.get());
   }



   /*******************************************************************************
    ** concurrent access from both instances - each thread sees only its own
    ** instance's data, and each provider is initialized once, however many threads
    ** race to be first.
    *******************************************************************************/
   @Test
   void testConcurrentAccess() throws Exception
   {
      QInstance instanceA = newInstance(newDatabaseName("a"), true);
      QInstance instanceB = newInstance(newDatabaseName("b"), true);
      writeRow(instanceA, "A");
      writeRow(instanceB, "B");
      ConnectionManager.closeConnectionProviders();
      CountingProvider.reset();

      int                threadCount = 16;
      ExecutorService    executor    = Executors.newFixedThreadPool(threadCount);
      CountDownLatch     start       = new CountDownLatch(1);
      List<Future<Void>> futures     = new ArrayList<>();
      try
      {
         for(int i = 0; i < threadCount; i++)
         {
            QInstance instance = i % 2 == 0 ? instanceA : instanceB;
            String    expected = i % 2 == 0 ? "A" : "B";
            futures.add(executor.submit(() ->
            {
               start.await();
               QContext.init(instance, new QSession());
               try
               {
                  for(int j = 0; j < 25; j++)
                  {
                     assertEquals(List.of(expected), readRows(backend(instance)));
                  }
                  return (null);
               }
               finally
               {
                  QContext.clear();
               }
            }));
         }

         start.countDown();
         for(Future<Void> future : futures)
         {
            future.get(60, TimeUnit.SECONDS);
         }
      }
      finally
      {
         executor.shutdownNow();
      }

      assertEquals(2, CountingProvider.INITS.get());
      assertEquals(0, CountingProvider.CLOSES.get());
   }



   /*******************************************************************************
    ** closing closes every instance's providers, and (the data being intact) a
    ** later call opens new ones.
    *******************************************************************************/
   @Test
   void testCloseAndReopen() throws SQLException
   {
      QInstance instanceA = newInstance(newDatabaseName("a"), true);
      QInstance instanceB = newInstance(newDatabaseName("b"), true);
      writeRow(instanceA, "A");
      writeRow(instanceB, "B");
      assertEquals(2, CountingProvider.INITS.get());

      ConnectionManager.closeConnectionProviders();
      assertEquals(2, CountingProvider.CLOSES.get());

      assertEquals(List.of("A"), readRows(instanceA));
      assertEquals(List.of("B"), readRows(instanceB));
      assertEquals(4, CountingProvider.INITS.get());

      ///////////////////////////////////////////////////////////////////////
      // closing again closes only what was opened since (nothing twice)   //
      ///////////////////////////////////////////////////////////////////////
      ConnectionManager.closeConnectionProviders();
      assertEquals(4, CountingProvider.CLOSES.get());
      ConnectionManager.closeConnectionProviders();
      assertEquals(4, CountingProvider.CLOSES.get());
   }



   /*******************************************************************************
    ** many threads closing at once: every provider is closed exactly once (the
    ** claim on each is atomic - not an iterator remove that can be raced).
    *******************************************************************************/
   @Test
   void testConcurrentCloseClosesEachProviderOnce() throws Exception
   {
      QInstance instanceA = newInstance(newDatabaseName("a"), true);
      QInstance instanceB = newInstance(newDatabaseName("b"), true);
      writeRow(instanceA, "A");
      writeRow(instanceB, "B");
      assertEquals(2, CountingProvider.INITS.get());

      int             threadCount = 16;
      ExecutorService executor    = Executors.newFixedThreadPool(threadCount);
      CountDownLatch  start       = new CountDownLatch(1);
      try
      {
         List<Future<?>> futures = new ArrayList<>();
         for(int i = 0; i < threadCount; i++)
         {
            futures.add(executor.submit(() ->
            {
               start.await();
               ConnectionManager.closeConnectionProviders();
               return (null);
            }));
         }

         start.countDown();
         for(Future<?> future : futures)
         {
            future.get(60, TimeUnit.SECONDS);
         }
      }
      finally
      {
         executor.shutdownNow();
      }

      assertEquals(2, CountingProvider.CLOSES.get());
   }



   /*******************************************************************************
    ** closers racing with callers that (re)open the same backend name: nothing
    ** fails, and every provider that was initialized is closed exactly once.
    *******************************************************************************/
   @Test
   void testConcurrentCloseAndReopenOfSameBackendName() throws Exception
   {
      QInstance instance = newInstance(newDatabaseName("reopen"), true);
      writeRow(instance, "R");
      ConnectionManager.closeConnectionProviders();
      CountingProvider.reset();

      int             readerCount = 6;
      int             closerCount = 2;
      ExecutorService executor    = Executors.newFixedThreadPool(readerCount + closerCount);
      CountDownLatch  start       = new CountDownLatch(1);
      try
      {
         List<Future<?>> futures = new ArrayList<>();
         for(int i = 0; i < readerCount; i++)
         {
            futures.add(executor.submit(() ->
            {
               start.await();
               QContext.init(instance, new QSession());
               try
               {
                  for(int j = 0; j < 100; j++)
                  {
                     assertEquals(List.of("R"), readRows(backend(instance)));
                  }
                  return (null);
               }
               finally
               {
                  QContext.clear();
               }
            }));
         }

         for(int i = 0; i < closerCount; i++)
         {
            futures.add(executor.submit(() ->
            {
               start.await();
               for(int j = 0; j < 100; j++)
               {
                  ConnectionManager.closeConnectionProviders();
               }
               return (null);
            }));
         }

         start.countDown();
         for(Future<?> future : futures)
         {
            future.get(120, TimeUnit.SECONDS);
         }
      }
      finally
      {
         executor.shutdownNow();
      }

      ConnectionManager.closeConnectionProviders();
      assertTrue(CountingProvider.INITS.get() >= 1);
      assertEquals(CountingProvider.ATTEMPTS.get(), CountingProvider.INITS.get());
      assertEquals(CountingProvider.INITS.get(), CountingProvider.CLOSES.get(), "every initialized provider is closed exactly once");
   }



   /*******************************************************************************
    ** a slow init in one instance blocks neither another instance's checkouts nor
    ** the debug dump - only callers that want that very provider wait for it.
    *******************************************************************************/
   @Test
   void testSlowInitInOneInstanceDoesNotBlockAnother() throws Exception
   {
      QInstance slowInstance = newInstance(newDatabaseName("slowa"), true);
      QInstance fastInstance = newInstance(newDatabaseName("fast"), true);
      writeRow(fastInstance, "fast");
      ConnectionManager.closeConnectionProviders();
      CountingProvider.reset();

      CountingProvider.slowMarker = "_slowa_";
      CountingProvider.slowEntered = new CountDownLatch(1);
      CountingProvider.slowGate = new CountDownLatch(1);

      ExecutorService executor = Executors.newFixedThreadPool(2);
      try
      {
         Future<Object> slowFuture = executor.submit(() ->
         {
            QContext.init(slowInstance, new QSession());
            try(Connection ignored = ConnectionManager.getConnection(backend(slowInstance)))
            {
               return (null);
            }
            finally
            {
               QContext.clear();
            }
         });

         assertTrue(CountingProvider.slowEntered.await(30, TimeUnit.SECONDS), "the slow init should have started");

         ///////////////////////////////////////////////////////////////////////////////
         // while it is stuck: the other instance checks out, and the dump returns    //
         ///////////////////////////////////////////////////////////////////////////////
         Future<String> fastFuture = executor.submit(() ->
         {
            List<String> rows = readRows(fastInstance);
            String       dump = ConnectionManager.dumpConnectionProviderDebug().toString();
            QContext.clear();
            return (rows + dump);
         });
         String result = fastFuture.get(20, TimeUnit.SECONDS);
         assertTrue(result.startsWith("[fast]"), result);
         assertFalse(slowFuture.isDone(), "the slow init is still held");

         CountingProvider.slowGate.countDown();
         slowFuture.get(30, TimeUnit.SECONDS);
      }
      finally
      {
         CountingProvider.slowGate.countDown();
         executor.shutdownNow();
      }

      assertEquals(2, CountingProvider.INITS.get());
   }



   /*******************************************************************************
    ** a provider that fails to initialize isn't remembered - the next call tries
    ** again.
    *******************************************************************************/
   @Test
   void testInitFailureIsRetried() throws SQLException
   {
      QInstance instance = newInstance(newDatabaseName("retry"), true);
      QContext.init(instance, new QSession());

      CountingProvider.failNextInit = true;
      assertThrows(SQLException.class, () -> ConnectionManager.getConnection(backend(instance)));
      assertEquals(1, CountingProvider.ATTEMPTS.get());
      assertEquals(0, CountingProvider.INITS.get());

      writeRow(instance, "recovered");
      assertEquals(List.of("recovered"), readRows(instance));
      assertEquals(2, CountingProvider.ATTEMPTS.get());
      assertEquals(1, CountingProvider.INITS.get());
   }



   /*******************************************************************************
    ** with no QInstance in the context, the shared standalone scope is used - and
    ** it isn't the same as an instance's.
    *******************************************************************************/
   @Test
   void testStandaloneScope() throws SQLException
   {
      QInstance instance = newInstance(newDatabaseName("inst"), true);
      writeRow(instance, "instance");

      QContext.clear();
      RDBMSBackendMetaData standalone = copyOf(backend(instance)).withJdbcUrl(url(newDatabaseName("standalone")));
      writeRow(standalone, "standalone");
      assertEquals(List.of("standalone"), readRows(standalone));
      assertEquals(2, CountingProvider.INITS.get());

      QContext.init(instance, new QSession());
      assertEquals(List.of("instance"), readRows(instance));
      assertEquals(2, CountingProvider.INITS.get());
   }



   /*******************************************************************************
    ** nothing that identifies the cache (and no credential) is in the debug dump,
    ** and asking for pool metrics doesn't create a provider.
    *******************************************************************************/
   @Test
   void testNoSecretsExposed() throws SQLException
   {
      QInstance instance = newInstance(newDatabaseName("secret"), true);
      backend(instance).setPassword(PASSWORD_THAT_MUST_NOT_LEAK);
      QContext.init(instance, new QSession());

      assertEquals(Map.of(), ConnectionManager.getPoolMetrics(backend(instance)));
      assertEquals(0, CountingProvider.ATTEMPTS.get());

      writeRow(instance, "x");
      String dump = ConnectionManager.dumpConnectionProviderDebug().toString();
      assertFalse(dump.contains(PASSWORD_THAT_MUST_NOT_LEAK), dump);
      assertTrue(dump.contains("\"backendName\":\"" + TestUtils.DEFAULT_BACKEND_NAME + "\""), dump);
      assertTrue(dump.contains("\"scope\":\"instance\""), dump);
   }



   /*******************************************************************************
    ** a new instance with the default backend of TestUtils, pointed at the given
    ** in-memory h2 database (and optionally, using the counting provider).
    *******************************************************************************/
   private QInstance newInstance(String databaseName, boolean withCountingProvider)
   {
      QInstance            instance = TestUtils.defineInstance();
      RDBMSBackendMetaData backend  = backend(instance);
      backend.setJdbcUrl(url(databaseName));
      if(withCountingProvider)
      {
         backend.setConnectionProvider(new QCodeReference(CountingProvider.class));
      }
      return (instance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String newDatabaseName(String label)
   {
      return ("scoping_" + label + "_" + UUID.randomUUID().toString().replace("-", ""));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String url(String databaseName)
   {
      return ("jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RDBMSBackendMetaData backend(QInstance instance)
   {
      return ((RDBMSBackendMetaData) instance.getBackend(TestUtils.DEFAULT_BACKEND_NAME));
   }



   /*******************************************************************************
    ** a new backend object, equal in every way that matters to the given one.
    *******************************************************************************/
   private static RDBMSBackendMetaData copyOf(RDBMSBackendMetaData original)
   {
      return (new RDBMSBackendMetaData()
         .withName(original.getName())
         .withVendor(original.getVendor())
         .withHostName(original.getHostName())
         .withPort(original.getPort())
         .withDatabaseName(original.getDatabaseName())
         .withUsername(original.getUsername())
         .withPassword(original.getPassword())
         .withJdbcUrl(original.getJdbcUrl())
         .withConnectionProvider(original.getConnectionProvider()));
   }



   /*******************************************************************************
    ** create the table in (the database of) the instance's backend, and add a row.
    *******************************************************************************/
   private void writeRow(QInstance instance, String value) throws SQLException
   {
      QContext.init(instance, new QSession());
      writeRow(backend(instance), value);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void writeRow(RDBMSBackendMetaData backend, String value) throws SQLException
   {
      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement())
      {
         statement.execute("CREATE TABLE scoping_test (v VARCHAR(20))");
         statement.execute("INSERT INTO scoping_test (v) VALUES ('" + value + "')");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> readRows(QInstance instance) throws SQLException
   {
      QContext.init(instance, new QSession());
      return (readRows(backend(instance)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> readRows(RDBMSBackendMetaData backend) throws SQLException
   {
      List<String> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT v FROM scoping_test ORDER BY v"))
      {
         while(resultSet.next())
         {
            rows.add(resultSet.getString(1));
         }
      }
      return (rows);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getUrl(QInstance instance) throws SQLException
   {
      QContext.init(instance, new QSession());
      try(Connection connection = ConnectionManager.getConnection(backend(instance)))
      {
         return (connection.getMetaData().getURL());
      }
   }



   /*******************************************************************************
    ** a provider that counts what is done to it (and can be told to fail its
    ** next init) - but otherwise opens plain connections, like the simple one.
    *******************************************************************************/
   public static class CountingProvider implements ConnectionProviderInterface
   {
      static final AtomicInteger ATTEMPTS = new AtomicInteger();
      static final AtomicInteger INITS    = new AtomicInteger();
      static final AtomicInteger CLOSES   = new AtomicInteger();

      static volatile boolean failNextInit = false;

      ////////////////////////////////////////////////////////////////////////////////////
      // when set, an init for a url containing slowMarker announces it has begun, then //
      // waits for the gate - a "slow init"                                              //
      ////////////////////////////////////////////////////////////////////////////////////
      static volatile String         slowMarker  = null;
      static volatile CountDownLatch slowEntered = null;
      static volatile CountDownLatch slowGate    = null;

      private RDBMSBackendMetaData backend;



      /*******************************************************************************
       **
       *******************************************************************************/
      static void reset()
      {
         ATTEMPTS.set(0);
         INITS.set(0);
         CLOSES.set(0);
         failNextInit = false;
         slowMarker = null;
         slowEntered = null;
         slowGate = null;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void init(RDBMSBackendMetaData backend) throws QException
      {
         ATTEMPTS.incrementAndGet();
         if(failNextInit)
         {
            failNextInit = false;
            throw (new QException("simulated init failure"));
         }

         if(slowMarker != null && ConnectionManager.getJdbcUrl(backend).contains(slowMarker))
         {
            slowEntered.countDown();
            try
            {
               if(!slowGate.await(30, TimeUnit.SECONDS))
               {
                  throw (new QException("slow init gate was never opened"));
               }
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
               throw (new QException("interrupted", e));
            }
         }

         this.backend = backend;
         INITS.incrementAndGet();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Connection getConnection() throws SQLException
      {
         return (DriverManager.getConnection(ConnectionManager.getJdbcUrl(backend), backend.getUsername(), backend.getPassword()));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void close()
      {
         CLOSES.incrementAndGet();
      }
   }

}
