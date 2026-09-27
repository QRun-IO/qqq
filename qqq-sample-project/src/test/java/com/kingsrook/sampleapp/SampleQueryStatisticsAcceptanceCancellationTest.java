/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobStatus;
import com.kingsrook.qqq.backend.core.actions.async.NonPersistedAsyncJobCallback;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*******************************************************************************
 ** Native cancellation distinguishes failed SQL from normal partial delivery.
 ** Latches establish execution order; deadlines only bound a broken fixture.
 ******************************************************************************/
public class SampleQueryStatisticsAcceptanceCancellationTest extends SampleQueryStatisticsAcceptanceFixture
{
   private static CountDownLatch entered;
   private static CountDownLatch release;



   /*******************************************************************************
    ** Cancel an active JDBC statement, then recover on a fresh normal query.
    ******************************************************************************/
   @Test
   void testNativeStatementCancellationDoesNotPublishCompletedStatistic() throws Exception
   {
      prepareNativeGate();
      QueryAction action = new QueryAction();
      ExecutorService executor = Executors.newSingleThreadExecutor();
      try
      {
         Future<QueryOutput> result = submit(executor, action, new QueryInput("person"));
         assertTrue(entered.await(5, TimeUnit.SECONDS), "Native SQL never entered the owned gate");
         action.cancel();
         release.countDown();
         ExecutionException failure = assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
         assertInstanceOf(QUserFacingException.class, failure.getCause());
         assertEquals("Query was cancelled.", failure.getCause().getMessage());
         assertTrue(snapshots.isEmpty(), "Cancelled SQL cannot report a completed backend measurement");
         manager.storeStatsNow();
         assertTrue(rows("SELECT id FROM query_stat").isEmpty());
         assertWorkerContextCleared(executor);
         assertEquals(List.of(List.of("20480")), rows("SELECT COUNT(*) FROM query_stat_gated_person"));
      }
      finally
      {
         finish(executor);
         restorePersonTable();
      }
      assertRecovery();
   }



   /*******************************************************************************
    ** Termination while SQL is active discards delivery; SQL still completes.
    ******************************************************************************/
   @Test
   void testConcurrentPlainPipeTerminationRetainsZeroDeliveryMeasurement() throws Exception
   {
      prepareNativeGate();
      RecordPipe pipe = new RecordPipe(25000);
      ExecutorService executor = Executors.newSingleThreadExecutor();
      try
      {
         Future<QueryOutput> result = submit(executor, new QueryAction(), new QueryInput("person").withRecordPipe(pipe).withIncludeAssociations(false));
         assertTrue(entered.await(5, TimeUnit.SECONDS));
         pipe.terminate();
         release.countDown();
         result.get(5, TimeUnit.SECONDS);
         assertEquals(0, pipe.getTotalRecordCount());
         assertEquals(0, pipe.countAvailableRecords());
         assertTrue(pipe.consumeAvailableRecords().isEmpty());
         assertMeasurement(0);
         assertWorkerContextCleared(executor);
         assertEquals(List.of(List.of("20480")), rows("SELECT COUNT(*) FROM query_stat_gated_person"));
      }
      finally
      {
         finish(executor);
         restorePersonTable();
      }
      assertRecovery();
   }



   /*******************************************************************************
    ** The backend callback breaks normally after the current row is delivered.
    ** This is a partial backend count, not a whole-request success indicator.
    ******************************************************************************/
   @Test
   void testAsyncCancellationDuringDeliveryRetainsExactPartialMeasurement() throws Exception
   {
      entered = new CountDownLatch(1);
      release = new CountDownLatch(1);
      instance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(DeliveryGate.class));
      AsyncJobStatus status = new AsyncJobStatus();
      QueryInput input = new QueryInput("person").withIncludeAssociations(false)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
      input.setAsyncJobCallback(new NonPersistedAsyncJobCallback(UUID.randomUUID(), status));
      RecordPipe pipe = new RecordPipe(5);
      input.setRecordPipe(pipe);
      ExecutorService executor = Executors.newSingleThreadExecutor();
      try
      {
         Future<QueryOutput> result = submit(executor, new QueryAction(), input);
         assertTrue(entered.await(5, TimeUnit.SECONDS), "No native row reached the delivery customizer");
         status.setCancelRequested(true);
         release.countDown();
         result.get(5, TimeUnit.SECONDS);
         List<QRecord> delivered = pipe.consumeAvailableRecords();
         assertEquals(1, delivered.size());
         assertEquals(rows("SELECT MIN(id) FROM person").get(0).get(0), delivered.get(0).getValueString("id"));
         assertEquals(1, pipe.getTotalRecordCount());
         assertTrue(input.getAsyncJobCallback().wasCancelRequested());
         assertMeasurement(1);
         assertWorkerContextCleared(executor);
      }
      finally
      {
         finish(executor);
         instance.getTable("person").getCustomizers().remove(TableCustomizers.POST_QUERY_RECORD.getRole());
      }
      assertRecovery();
   }



   /*******************************************************************************
    ** A real H2 function pauses statement execution, before any result delivery.
    ** A bounded scan ensures the driver reaches its statement-cancellation check.
    ******************************************************************************/
   private void prepareNativeGate() throws Exception
   {
      entered = new CountDownLatch(1);
      release = new CountDownLatch(1);
      try(Statement statement = oracle.createStatement())
      {
         statement.execute("CREATE ALIAS query_stat_gate FOR \"com.kingsrook.sampleapp.SampleQueryStatisticsAcceptanceCancellationTest.nativeGate\"");
         statement.execute("CREATE VIEW query_stat_gated_person AS SELECT p.* FROM person p CROSS JOIN SYSTEM_RANGE(1, 4096) r WHERE query_stat_gate(r.X) > 0");
      }
      instance.getTable("person").withBackendDetails(new RDBMSTableBackendDetails().withTableName("query_stat_gated_person"));
   }



   /*******************************************************************************
    ** Invoked by H2 itself, not by a substituted query action or JDBC object.
    ******************************************************************************/
   public static Long nativeGate(Long value) throws InterruptedException
   {
      entered.countDown();
      if(!release.await(5, TimeUnit.SECONDS))
      {
         throw new IllegalStateException("Native gate was not released");
      }
      return value;
   }



   /*******************************************************************************
    ** A normal table customizer gates delivery without changing native records.
    ******************************************************************************/
   public static class DeliveryGate implements TableCustomizerInterface
   {
      /*******************************************************************************
       ** Pause the current real result; the coordinator requests callback cancel.
       ******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records) throws QException
      {
         try
         {
            nativeGate(1L);
            return records;
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new QException("Delivery gate interrupted", e);
         }
      }
   }



   /*******************************************************************************
    ** Install only the owned session, always clearing it on success or failure.
    ******************************************************************************/
   private Future<QueryOutput> submit(ExecutorService executor, QueryAction action, QueryInput input)
   {
      return executor.submit(() ->
      {
         QContext.init(instance, session);
         try
         {
            return action.execute(input);
         }
         finally
         {
            assertSame(instance, QContext.getQInstance());
            assertSame(session, QContext.getQSession());
            QContext.clear();
         }
      });
   }



   /*******************************************************************************
    ** Check persisted SQL/session metadata and real native connection ownership.
    ******************************************************************************/
   private void assertMeasurement(Integer count) throws Exception
   {
      assertEquals(1, snapshots.size());
      Snapshot stat = snapshots.get(0);
      assertEquals(count, stat.count());
      assertEquals(session.getUuid(), stat.session());
      assertEquals("QueryAction", stat.backendAction());
      assertFalse(stat.first().isBefore(stat.start()));
      assertTrue(stat.millis() >= 0);
      manager.storeStatsNow();
      assertEquals(List.of(List.of(session.getUuid(), "person", stat.sql())),
         rows("SELECT s.session_id,t.name,s.query_text FROM query_stat s JOIN qqq_table t ON t.id=s.qqq_table_id"));
   }



   /*******************************************************************************
    ** Reuse the same executor to prove no query context remains on its thread.
    ******************************************************************************/
   private void assertWorkerContextCleared(ExecutorService executor) throws Exception
   {
      executor.submit(() ->
      {
         assertNull(QContext.getQInstance());
         assertNull(QContext.getQSession());
         assertNull(QContext.getObjects());
      }).get(5, TimeUnit.SECONDS);
      assertEquals(List.of(List.of("1")), rows("SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS"));
      assertEquals(List.of(List.of("5")), rows("SELECT COUNT(*) FROM person"));
      assertSame(instance, QContext.getQInstance());
      assertSame(session, QContext.getQSession());
   }



   /*******************************************************************************
    ** Release native/customizer gates before bounded executor shutdown on failure.
    ******************************************************************************/
   private void finish(ExecutorService executor) throws InterruptedException
   {
      release.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Query worker survived cleanup");
   }



   /*******************************************************************************
    ** Restore canonical metadata before the independent recovery read.
    ******************************************************************************/
   private void restorePersonTable()
   {
      instance.getTable("person").setBackendDetails(null);
   }



   /*******************************************************************************
    ** A new ordinary query measures all native rows without stale cancellation.
    ******************************************************************************/
   private void assertRecovery() throws Exception
   {
      int before = snapshots.size();
      assertEquals(5, new QueryAction().execute(new QueryInput("person")).getRecords().size());
      assertEquals(before + 1, snapshots.size());
      assertEquals(5, snapshots.get(before).count());
      manager.storeStatsNow();
      assertEquals(List.of(List.of(String.valueOf(before + 1))), rows("SELECT COUNT(*) FROM query_stat"));
      assertEquals(List.of(List.of("1")), rows("SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS"));
   }
}
