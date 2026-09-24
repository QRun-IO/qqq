/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.sampleapp;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native H2 Aggregate cancellation and connection routing. The SQL alias gates
 ** actual row evaluation; JDBC instrumentation delegates every operation/result.
 *******************************************************************************/
@Timeout(30)
public class SampleAggregateExecutionContractTest
{
   private static volatile NativeGate currentGate;
   private QInstance instance;
   private RDBMSBackendMetaData primary;
   private Map<String, List<List<String>>> before;



   /*******************************************************************************
    ** A bounded 257-row group scan crosses H2's periodic cancellation checks.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      currentGate = null;
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      primary = (RDBMSBackendMetaData) instance.getBackend(SampleMetaDataProvider.RDBMS_BACKEND_NAME);
      QContext.init(instance, new QSession());
      try(Connection connection = connection(); Statement statement = connection.createStatement();
          PreparedStatement insert = connection.prepareStatement("INSERT INTO field_lab(id,name,long_value) VALUES(?,?,?)"))
      {
         for(int id = 1; id <= 257; id++)
         {
            insert.setInt(1, id);
            insert.setString(2, "Aggregate row " + id);
            insert.setLong(3, id);
            assertEquals(1, insert.executeUpdate());
         }
         statement.execute("CREATE ALIAS SAMPLE_AGGREGATE_GATE FOR \"" + getClass().getName() + ".awaitNativeRow\"");
      }
      before = snapshot();
   }



   /*******************************************************************************
    ** Workers are joined by each test before removing the owned SQL alias.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(currentGate != null)
         {
            currentGate.release.countDown();
         }
         try(Connection connection = connection(); Statement statement = connection.createStatement())
         {
            statement.execute("DROP ALIAS IF EXISTS SAMPLE_AGGREGATE_GATE");
         }
         assertEquals(before, snapshot());
      }
      finally
      {
         currentGate = null;
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Timeout must cancel actual native execution and leave caller work recoverable.
    *******************************************************************************/
   @Test
   void testNativeTimeoutPreservesCallerTransaction() throws Exception
   {
      exerciseBlocked(1, false);
   }



   /*******************************************************************************
    ** Explicit cancellation has a distinct user-facing failure and native cause.
    *******************************************************************************/
   @Test
   void testExplicitNativeCancellationPreservesCallerTransaction() throws Exception
   {
      exerciseBlocked(null, true);
   }



   /*******************************************************************************
    ** Null, zero and negative timeout retain their existing no-timeout meaning.
    *******************************************************************************/
   @Test
   void testDisabledTimeoutControlsCompleteOnlyAfterNativeRelease() throws Exception
   {
      for(Integer timeout : Arrays.asList(null, 0, -1))
      {
         exerciseBlocked(timeout, false);
      }
   }



   /*******************************************************************************
    ** Completed statements must not receive a delayed cancellation attempt.
    *******************************************************************************/
   @Test
   void testCompletedCallCancelsItsScheduledTimeout() throws Exception
   {
      try(Connection nativeConnection = connection())
      {
         JdbcProbe probe = new JdbcProbe(nativeConnection, null);
         try(RDBMSTransaction transaction = new RDBMSTransaction(probe.connection))
         {
            pendingWrite(nativeConnection);
            List<List<String>> pending = rows(nativeConnection, "SELECT * FROM field_lab ORDER BY id");
            AggregateInput input = scalar().withTransaction(transaction).withTimeoutSeconds(1);
            assertScalar(input, nativeConnection);
            assertEquals(1, probe.statements.size());

            assertFalse(probe.cancelAttempt.await(1500, TimeUnit.MILLISECONDS), "Completed statement received a lingering timeout cancellation");
            assertEquals(0, probe.cancelCalls.get());
            assertOwned(probe, transaction);
            assertScalar(scalar().withTransaction(transaction), nativeConnection);
            assertEquals(pending, rows(nativeConnection, "SELECT * FROM field_lab ORDER BY id"));
            transaction.rollback();
            assertEquals(before.get("field_lab"), rows(nativeConnection, "SELECT * FROM field_lab ORDER BY id"));
         }
      }
   }



   /*******************************************************************************
    ** Distinct native datasets prove hint fallback, replica routing and transaction
    ** precedence without relying on connection-request counters alone.
    *******************************************************************************/
   @Test
   void testReadOnlyHintFallbackReplicaAndCallerTransactionPrecedence() throws Exception
   {
      AggregateInput hinted = scalar().withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
      try(Connection primaryConnection = connection())
      {
         assertScalar(hinted, primaryConnection);
      }
      RDBMSBackendMetaData previous = primary.getReadOnlyBackendMetaData();
      String uniqueName = UUID.randomUUID().toString();
      RDBMSBackendMetaData replica = SampleMetaDataProvider.defineRdbmsBackend().withName("aggregateReplica_" + uniqueName)
         .withDatabaseName("sample_aggregate_replica_" + uniqueName);
      try(Connection replicaConnection = ConnectionManager.getConnection(replica);
          Statement replicaStatement = replicaConnection.createStatement();
          InputStream seed = getClass().getResourceAsStream("/prime-test-database.sql"))
      {
         assertNotNull(seed);
         assertTrue(replicaConnection.getMetaData().getURL().startsWith("jdbc:h2:mem:sample_aggregate_replica_"));
         try(InputStreamReader reader = new InputStreamReader(seed, StandardCharsets.UTF_8))
         {
            RunScript.execute(replicaConnection, reader);
         }
         assertEquals(2, replicaStatement.executeUpdate("INSERT INTO field_lab(id,name,long_value) VALUES(1,'Replica one',700),(2,'Replica two',900)"));
         List<List<String>> replicaBefore = rows(replicaConnection, "SELECT * FROM field_lab ORDER BY id");
         assertEquals(List.of(List.of("2", "1600")), rows(replicaConnection, "SELECT COUNT(id),SUM(long_value) FROM field_lab"));
         try
         {
            primary.setReadOnlyBackendMetaData(replica);
            try(Connection primaryConnection = connection())
            {
               assertScalar(scalar(), primaryConnection);
               assertScalar(scalar().withQueryHints(null), primaryConnection);
               assertScalar(hinted, replicaConnection);
               try(RDBMSTransaction transaction = new RDBMSTransaction(primaryConnection))
               {
                  pendingWrite(primaryConnection);
                  List<List<String>> pending = rows(primaryConnection, "SELECT * FROM field_lab ORDER BY id");
                  AggregateInput transactionInput = scalar().withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND).withTransaction(transaction);
                  assertScalar(transactionInput, primaryConnection);
                  assertScalar(hinted, replicaConnection);
                  assertEquals(pending, rows(primaryConnection, "SELECT * FROM field_lab ORDER BY id"));
                  assertSame(transaction, transactionInput.getTransaction());
                  assertTrue(transactionInput.hasQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND));
                  assertFalse(primaryConnection.isClosed());
                  assertFalse(primaryConnection.getAutoCommit());
                  assertEquals(before, snapshot());
                  transaction.rollback();
                  assertScalar(transactionInput, primaryConnection);
               }
            }
            assertEquals(replicaBefore, rows(replicaConnection, "SELECT * FROM field_lab ORDER BY id"));
         }
         finally
         {
            replicaStatement.execute("DROP ALL OBJECTS");
         }
      }
      finally
      {
         primary.setReadOnlyBackendMetaData(previous);
      }
      try(Connection primaryConnection = connection())
      {
         assertScalar(hinted, primaryConnection);
      }
   }



   /*******************************************************************************
    ** All waits are bounded; cleanup releases native evaluation before joining.
    *******************************************************************************/
   private void exerciseBlocked(Integer timeout, boolean cancel) throws Exception
   {
      NativeGate gate = new NativeGate();
      currentGate = gate;
      ExecutorService worker = Executors.newSingleThreadExecutor();
      Future<AggregateOutput> future = null;
      try(Connection nativeConnection = connection())
      {
         JdbcProbe probe = new JdbcProbe(nativeConnection, gate);
         try(RDBMSTransaction transaction = new RDBMSTransaction(probe.connection))
         {
            pendingWrite(nativeConnection);
            List<List<String>> pending = rows(nativeConnection, "SELECT * FROM field_lab ORDER BY id");
            AggregateAction action = new AggregateAction();
            Aggregate sum = new Aggregate("longValue", AggregateOperator.SUM);
            GroupBy group = new GroupBy(QFieldType.INTEGER, "id", "SAMPLE_AGGREGATE_GATE(%s)");
            AggregateInput input = new AggregateInput("fieldLab").withAggregate(sum).withGroupBy(group).withTransaction(transaction).withTimeoutSeconds(timeout);
            try
            {
               future = worker.submit(() ->
               {
                  try
                  {
                     QContext.init(instance, new QSession());
                     return action.execute(input);
                  }
                  finally
                  {
                     QContext.clear();
                  }
               });
               Future<AggregateOutput> active = future;
               assertTrue(gate.started.await(5, TimeUnit.SECONDS), "Aggregate never entered actual native row evaluation");
               if(cancel)
               {
                  action.cancel();
               }
               if(cancel || (timeout != null && timeout > 0))
               {
                  ExecutionException failure = assertThrows(ExecutionException.class, () -> active.get(8, TimeUnit.SECONDS));
                  assertEquals(cancel ? "Aggregate query was cancelled." : "Aggregate query timed out.",
                     assertInstanceOf(QUserFacingException.class, failure.getCause()).getMessage());
                  assertEquals(1, probe.cancelCalls.get());
                  assertEquals("57014", probe.nativeFailureState.get(), "Driver must report native statement cancellation before action wrapping");
               }
               else
               {
                  assertThrows(TimeoutException.class, () -> active.get(150, TimeUnit.MILLISECONDS));
                  assertEquals(0, probe.cancelCalls.get());
                  gate.release.countDown();
                  AggregateOutput output = active.get(8, TimeUnit.SECONDS);
                  Map<Integer, String> actual = new TreeMap<>();
                  for(AggregateResult result : output.getResults())
                  {
                     assertNull(actual.put(((Number) result.getGroupByValue(group)).intValue(), number(result.getAggregateValue(sum))));
                  }
                  Map<Integer, String> expected = new TreeMap<>();
                  for(List<String> row : rows(nativeConnection, "SELECT id,SUM(long_value) FROM field_lab GROUP BY id ORDER BY id"))
                  {
                     expected.put(Integer.valueOf(row.get(0)), row.get(1));
                  }
                  assertEquals(257, actual.size());
                  assertEquals(expected, actual);
                  assertNull(probe.nativeFailureState.get());
               }
               assertEquals(1, probe.statements.size());

               assertOwned(probe, transaction);
               assertEquals(pending, rows(nativeConnection, "SELECT * FROM field_lab ORDER BY id"));
               assertEquals(before, snapshot());
               assertScalar(scalar().withTransaction(transaction), nativeConnection);

               assertOwned(probe, transaction);
               transaction.rollback();
               assertEquals(before.get("field_lab"), rows(nativeConnection, "SELECT * FROM field_lab ORDER BY id"));
            }
            finally
            {
               gate.release.countDown();
               if(future != null && !future.isDone())
               {
                  future.cancel(true);
               }
               worker.shutdownNow();
               assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS), "Native aggregate worker did not terminate after gate release");
            }
         }
      }
      finally
      {
         gate.release.countDown();
         worker.shutdownNow();
         assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
         currentGate = null;
      }
   }



   /*******************************************************************************
    ** Invoked by H2 only during native evaluation of a field-dependent expression.
    *******************************************************************************/
   public static Integer awaitNativeRow(Integer value) throws SQLException
   {
      NativeGate gate = currentGate;
      if(gate != null && gate.entered.compareAndSet(false, true))
      {
         gate.started.countDown();
         try
         {
            if(!gate.release.await(15, TimeUnit.SECONDS))
            {
               throw new SQLException("Fixture native gate was not released", "HYT00");
            }
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new SQLException("Fixture native gate interrupted", "HY008", e);
         }
      }
      return value;
   }



   /*******************************************************************************
    ** Compare cardinality and SUM with direct JDBC on the intended connection.
    *******************************************************************************/
   private void assertScalar(AggregateInput input, Connection expectedConnection) throws Exception
   {
      AggregateOutput output = new AggregateAction().execute(input);
      assertEquals(1, output.getResults().size());
      List<String> actual = input.getAggregates().stream().map(aggregate -> number(output.getResults().get(0).getAggregateValue(aggregate))).toList();
      assertEquals(rows(expectedConnection, "SELECT COUNT(id),SUM(long_value) FROM field_lab"), List.of(actual));
   }



   /*******************************************************************************
    ** Independent caller work must survive success, timeout and cancellation.
    *******************************************************************************/
   private void pendingWrite(Connection connection) throws SQLException
   {
      try(Statement statement = connection.createStatement())
      {
         assertEquals(1, statement.executeUpdate("UPDATE field_lab SET long_value=1001 WHERE id=1"));
      }
   }



   /*******************************************************************************
    ** Keep transaction ownership separate from native statement ownership.
    *******************************************************************************/
   private void assertOwned(JdbcProbe probe, RDBMSTransaction transaction) throws SQLException
   {
      assertSame(probe.connection, transaction.getConnection());
      assertFalse(probe.connection.isClosed());
      assertFalse(probe.connection.getAutoCommit());
      assertTrue(probe.boundaries.isEmpty(), "Aggregate took over a caller transaction boundary");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateInput scalar()
   {
      return new AggregateInput("fieldLab").withAggregate(new Aggregate("id", AggregateOperator.COUNT))
         .withAggregate(new Aggregate("longValue", AggregateOperator.SUM));
   }



   /*******************************************************************************
    ** Every original physical column is compared; this fixture has no BLOB values.
    *******************************************************************************/
   private Map<String, List<List<String>>> snapshot() throws SQLException
   {
      Map<String, List<List<String>>> result = new LinkedHashMap<>();
      try(Connection connection = connection())
      {
         for(String table : List.of("field_lab", "person", "pet", "pet_note"))
         {
            result.put(table, rows(connection, "SELECT * FROM " + table + " ORDER BY id"));
         }
      }
      return result;
   }



   /*******************************************************************************
    ** Native rows are independent of framework aggregate conversion.
    *******************************************************************************/
   private List<List<String>> rows(Connection connection, String sql) throws SQLException
   {
      List<List<String>> result = new ArrayList<>();
      try(Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql))
      {
         while(rows.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= rows.getMetaData().getColumnCount(); column++)
            {
               row.add(rows.getString(column));
            }
            result.add(row);
         }
      }
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String number(Object value)
   {
      return value == null ? null : new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws SQLException
   {
      return ConnectionManager.getConnection(primary);
   }



   /*******************************************************************************
    ** A fresh native evaluation gate belongs to exactly one worker.
    *******************************************************************************/
   private static class NativeGate
   {
      private final CountDownLatch started = new CountDownLatch(1);
      private final CountDownLatch release = new CountDownLatch(1);
      private final AtomicBoolean entered = new AtomicBoolean();
   }



   /*******************************************************************************
    ** Observe actual driver cancellation, failures and closes without substituting
    ** results. Releasing the SQL alias happens only after native cancel returns.
    *******************************************************************************/
   private static class JdbcProbe
   {
      private final Connection connection;
      private final List<PreparedStatement> statements = new CopyOnWriteArrayList<>();
      private final List<String> boundaries = new CopyOnWriteArrayList<>();
      private final AtomicInteger cancelCalls = new AtomicInteger();
      private final CountDownLatch cancelAttempt = new CountDownLatch(1);
      private final AtomicReference<String> nativeFailureState = new AtomicReference<>();



      /*******************************************************************************
       ** Forward every call, preserving actual SQLException causes.
       *******************************************************************************/
      private JdbcProbe(Connection delegate, NativeGate gate)
      {
         connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class }, (proxy, method, args) ->
         {
            try
            {
               Object result = method.invoke(delegate, args);
               if("commit".equals(method.getName()) || "rollback".equals(method.getName()) || "close".equals(method.getName()))
               {
                  boundaries.add(method.getName());
               }
               if("prepareStatement".equals(method.getName()) && result instanceof PreparedStatement statement)
               {
                  statements.add(statement);
                  return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class<?>[] { PreparedStatement.class }, (statementProxy, operation, arguments) ->
                  {
                     boolean cancel = "cancel".equals(operation.getName());
                     if(cancel)
                     {
                        cancelCalls.incrementAndGet();
                        cancelAttempt.countDown();
                     }
                     try
                     {
                        return operation.invoke(statement, arguments);
                     }
                     catch(InvocationTargetException e)
                     {
                        if(operation.getName().startsWith("execute") && e.getCause() instanceof SQLException failure)
                        {
                           nativeFailureState.set(failure.getSQLState());
                        }
                        throw e.getCause();
                     }
                     finally
                     {
                        if(cancel && gate != null)
                        {
                           gate.release.countDown();
                        }
                     }
                  });
               }
               return result;
            }
            catch(InvocationTargetException e)
            {
               throw e.getCause();
            }
         });
      }
   }
}
