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


import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*******************************************************************************
 ** Query-statistics source evidence using owned native persistence.
 ******************************************************************************/
class SampleQueryStatisticsAcceptanceTest extends SampleQueryStatisticsAcceptanceFixture
{
   /*******************************************************************************
    ** Restart waits for an in-flight native insert, then retires that worker and
    ** uses only the new supplier for the replacement generation.
    ******************************************************************************/
   @Test
   void testRestartWaitsForNativeFlushAndRetiresPreviousWorker() throws Exception
   {
      manager.stop();
      awaitWorkersStopped();
      var entered = new CountDownLatch(1);
      var release = new CountDownLatch(1);
      var restarting = new CountDownLatch(1);
      var oldWorker = new AtomicReference<Thread>();
      var oldCalls = new AtomicInteger();
      var newCalls = new AtomicInteger();
      QSession oldStorageSession = new QSession();
      QSession newStorageSession = new QSession();
      StorageObserver.contexts.clear();
      instance.getTable("queryStat").withCapability(Capability.QUERY_STATS);
      instance.getTable("queryStat").withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(StorageObserver.class));
      manager.setJobInitialDelay(0);
      manager.start(instance, () ->
      {
         oldCalls.incrementAndGet();
         oldWorker.set(Thread.currentThread());
         entered.countDown();
         try
         {
            assertTrue(release.await(5, TimeUnit.SECONDS), "Old flush was not released");
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
         }
         return oldStorageSession;
      });
      try(var executor = Executors.newSingleThreadExecutor())
      {
         try
         {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            new QueryAction().execute(new QueryInput("person"));
            manager.setJobInitialDelay(3600);
            var restart = executor.submit(() ->
            {
               restarting.countDown();
               manager.start(instance, () ->
               {
                  newCalls.incrementAndGet();
                  return newStorageSession;
               });
            });
            try
            {
               assertTrue(restarting.await(5, TimeUnit.SECONDS));
               assertThrows(TimeoutException.class, () -> restart.get(100, TimeUnit.MILLISECONDS));
            }
            finally
            {
               release.countDown();
               restart.get(5, TimeUnit.SECONDS);
            }
            oldWorker.get().join(3000);
            assertFalse(oldWorker.get().isAlive(), "Previous generation still has a worker");
            assertEquals(List.of(List.of(session.getUuid())), rows("SELECT session_id FROM query_stat"));
            assertEquals(1, oldCalls.get());
            assertEquals(0, newCalls.get());
            assertEquals(1, StorageObserver.contexts.size());
            assertSame(oldStorageSession, StorageObserver.contexts.get(0).session());

            QSession nextSession = new QSession();
            nextSession.setUuid(UUID.randomUUID().toString());
            QContext.init(instance, nextSession);
            new QueryAction().execute(new QueryInput("person"));
            manager.storeStatsNow();
            assertEquals(List.of(List.of(session.getUuid()), List.of(nextSession.getUuid())),
               rows("SELECT session_id FROM query_stat ORDER BY id"));
            assertEquals(2, snapshots.size());
            assertEquals(1, oldCalls.get());
            assertEquals(1, newCalls.get());
            assertEquals(2, StorageObserver.contexts.size());
            assertSame(newStorageSession, StorageObserver.contexts.get(1).session());
            assertSame(nextSession, QContext.getQSession());
         }
         finally
         {
            release.countDown();
            StorageObserver.contexts.clear();
         }
      }
   }



   /*******************************************************************************
    ** Direct delivery reports the native row count and the actual owned session.
    ******************************************************************************/
   @Test
   void testDirectQuerySnapshotAndExplicitNativeStorage() throws Exception
   {
      List<QRecord> records = new QueryAction().execute(new QueryInput("person")).getRecords();
      assertEquals(rows("SELECT id FROM person").size(), records.size());
      assertEquals(1, snapshots.size());
      Snapshot stat = snapshots.get(0);
      assertEquals(records.size(), stat.count());
      assertEquals(session.getUuid(), stat.session());
      assertEquals("person", stat.table());
      assertEquals("QueryAction", stat.backendAction());
      assertNotNull(stat.start());
      assertNotNull(stat.first());
      assertTrue(!stat.first().isBefore(stat.start()));
      assertTrue(stat.millis() >= 0);
      assertTrue(stat.sql().contains("person"));
      assertTrue(rows("SELECT id FROM query_stat").isEmpty());
      manager.storeStatsNow();
      assertEquals(List.of(List.of(session.getUuid(), "person")), rows("SELECT s.session_id, t.name FROM query_stat s JOIN qqq_table t ON t.id=s.qqq_table_id"));
   }




   /*******************************************************************************
    ** Empty, short, full and tail-bearing plain pipes report actual delivered rows.
    ******************************************************************************/
   @Test
   void testPlainPipeCountsEmptyFullBatchesAndTails() throws Exception
   {
      for(Integer size : List.of(0, 3, 100, 105))
      {
         try(Statement statement = oracle.createStatement())
         {
            statement.executeUpdate("DELETE FROM person");
            if(size > 0)
            {
               statement.executeUpdate("INSERT INTO person(id,first_name,last_name,email) SELECT x,'Owned','Row','row@example.invalid' FROM SYSTEM_RANGE(1," + size + ")");
            }
         }
         snapshots.clear();
         RecordPipe pipe = new RecordPipe(200);
         new QueryAction().execute(new QueryInput("person").withRecordPipe(pipe).withIncludeAssociations(false));
         List<QRecord> records = pipe.consumeAvailableRecords();
         assertEquals(rows("SELECT id FROM person").size(), records.size());
         assertEquals(size, records.size());
         assertEquals(1, snapshots.size());
         assertEquals(size, snapshots.get(0).count());
         assertEquals(size, pipe.getTotalRecordCount());
      }
   }



   /*******************************************************************************
    ** Metadata storage records native query timing, criteria, ordering and joins.
    ******************************************************************************/
   @Test
   void testNativeTimingFilterOrderAndJoinMetadata() throws Exception
   {
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))
         .withSubFilter(new QQueryFilter(new QFilterCriteria("pet.name", QCriteriaOperator.IN, "Charlie", "Coco")))
         .withOrderBy(new QFilterOrderBy("pet.name", false)).withOrderBy(new QFilterOrderBy("firstName", true));
      QueryInput input = new QueryInput("person").withQueryJoin(new QueryJoin("pet").withSelect(true)).withFilter(filter);
      QContext.pushAction(input);
      List<QRecord> records;
      try
      {
         records = new QueryAction().execute(input).getRecords();
      }
      finally
      {
         QContext.popAction();
      }
      assertEquals(List.of(List.of("Coco"), List.of("Charlie")), rows("SELECT pet.name FROM person p JOIN pet ON pet.person_id=p.id WHERE p.id=1 AND pet.name IN ('Charlie','Coco') ORDER BY pet.name DESC"));
      assertEquals(List.of("Coco", "Charlie"), records.stream().map(r -> r.getValueString("pet.name")).toList());
      Snapshot stat = snapshots.get(0);
      assertEquals(2, stat.count());
      assertEquals(Set.of("pet"), stat.joins());
      assertTrue(stat.filter().contains("Charlie"));
      assertFalse(stat.action().isBlank());
      flushOnOwnedThread();
      assertEquals(List.of(List.of("person", "id", "EQUALS", "1"), List.of("pet", "name", "IN", "Charlie,Coco")),
         rows("SELECT t.name,c.name,c.operator,c.criteria_values FROM query_stat_criteria_field c JOIN qqq_table t ON t.id=c.qqq_table_id ORDER BY c.id"));
      assertEquals(List.of(List.of("pet", "name"), List.of("person", "firstName")),
         rows("SELECT t.name,o.name FROM query_stat_order_by_field o JOIN qqq_table t ON t.id=o.qqq_table_id ORDER BY o.id"));
      assertEquals(List.of(List.of("pet")), rows("SELECT t.name FROM query_stat_join_table j JOIN qqq_table t ON t.id=j.qqq_table_id"));
      try(Statement statement = oracle.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM query_stat"))
      {
         assertTrue(result.next());
         assertEquals(stat.start(), result.getObject("start_timestamp", LocalDateTime.class).toInstant(ZoneOffset.UTC));
         assertEquals(stat.first(), result.getObject("first_result_timestamp", LocalDateTime.class).toInstant(ZoneOffset.UTC));
         assertEquals(stat.millis(), result.getInt("first_result_millis"));
         assertEquals(stat.sql(), result.getString("query_text"));
         assertEquals(stat.action(), result.getString("action"));
         assertEquals(session.getUuid(), result.getString("session_id"));
         assertFalse(result.next());
      }
      assertSame(instance, QContext.getQInstance());
      assertSame(session, QContext.getQSession());
   }



   /*******************************************************************************
    ** Disabled backend/table capabilities and a disabled manager produce no stats.
    ******************************************************************************/
   @Test
   void testDisabledCapabilitiesAndManager() throws Exception
   {
      var backend = instance.getBackendForTable("person");
      backend.withoutCapability(Capability.QUERY_STATS);
      assertEquals(5, new QueryAction().execute(new QueryInput("person")).getRecords().size());
      backend.withCapability(Capability.QUERY_STATS);
      instance.getTable("person").withoutCapability(Capability.QUERY_STATS);
      new QueryAction().execute(new QueryInput("person"));
      instance.getTable("person").withCapability(Capability.QUERY_STATS);
      manager.stop();
      awaitWorkersStopped();
      System.setProperty(ENABLED, "false");
      manager.start(instance, QSession::new);
      new QueryAction().execute(new QueryInput("person"));
      assertTrue(snapshots.isEmpty());
      assertTrue(rows("SELECT id FROM query_stat").isEmpty());
      assertFalse(Thread.getAllStackTraces().keySet().stream().anyMatch(t -> t.isAlive() && t.getName().startsWith("QueryStatManager-")));
   }



   /*******************************************************************************
    ** The storage threshold does not suppress consumers, including a failed one.
    ******************************************************************************/
   @Test
   void testThresholdAndThrowingConsumerDoNotBreakRead() throws Exception
   {
      var goodConsumer = manager.getQueryStatConsumers().get(0);
      manager.setQueryStatConsumers(List.of(stat ->
      {
         throw new IllegalStateException("owned consumer failure");
      }, goodConsumer));
      manager.setMinMillisToStore(Integer.MAX_VALUE);
      assertEquals(5, new QueryAction().execute(new QueryInput("person")).getRecords().size());
      assertEquals(1, snapshots.size());
      flushOnOwnedThread();
      assertTrue(rows("SELECT id FROM query_stat").isEmpty());
      manager.setMinMillisToStore(0);
      new QueryAction().execute(new QueryInput("person"));
      flushOnOwnedThread();
      assertEquals(2, snapshots.size());
      assertEquals(1, rows("SELECT id FROM query_stat").size());
   }



   /*******************************************************************************
    ** Table opt-outs prevent collection/storage queries recursively collecting stats.
    ******************************************************************************/
   @Test
   void testExplicitMetadataOptOutPreventsRecursiveCollection() throws Exception
   {
      new QueryAction().execute(new QueryInput("person"));
      flushOnOwnedThread();
      assertEquals(1, new QueryAction().execute(new QueryInput("queryStat")).getRecords().size());
      flushOnOwnedThread();
      flushOnOwnedThread();
      assertEquals(1, snapshots.size());
      assertEquals(1, rows("SELECT id FROM query_stat").size());
      assertEquals(1, rows("SELECT id FROM qqq_table").size());
   }



   /*******************************************************************************
    ** A real unavailable storage table loses that batch, but later reads recover.
    ** Loss is an explicit boundary: there is no retry/durability claim here.
    ******************************************************************************/
   @Test
   void testStorageFailureDropsBatchAndFreshReadRecovers() throws Exception
   {
      var caller = QContext.capture();
      instance.getTable("queryStat").withCapability(Capability.QUERY_STATS);
      var details = (RDBMSTableBackendDetails) instance.getTable("queryStat").getBackendDetails();
      details.setTableName("missing_owned_query_stat");
      new QueryAction().execute(new QueryInput("person"));
      manager.storeStatsNow();
      assertEquals(caller, QContext.capture());
      assertEquals(1, snapshots.size());
      assertTrue(rows("SELECT id FROM query_stat").isEmpty());
      details.setTableName("query_stat");
      manager.storeStatsNow();
      assertEquals(caller, QContext.capture());
      assertTrue(rows("SELECT id FROM query_stat").isEmpty(), "A failed batch is not retried");
      new QueryAction().execute(new QueryInput("person"));
      manager.storeStatsNow();
      assertEquals(caller, QContext.capture());
      assertEquals(2, snapshots.size());
      assertEquals(1, rows("SELECT id FROM query_stat").size());
   }



   /*******************************************************************************
    ** A native query failure is propagated and creates neither consumer nor stored stats.
    ******************************************************************************/
   @Test
   void testFailedNativeQueryHasNoSuccessStatistic() throws Exception
   {
      var details = (RDBMSTableBackendDetails) instance.getTable("person").getBackendDetails();
      instance.getTable("person").setBackendDetails(new RDBMSTableBackendDetails().withTableName("missing_owned_person"));
      assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput("person")));
      assertTrue(snapshots.isEmpty());
      flushOnOwnedThread();
      assertTrue(rows("SELECT id FROM query_stat").isEmpty());
      instance.getTable("person").setBackendDetails(details);
      assertEquals(5, new QueryAction().execute(new QueryInput("person")).getRecords().size());
      assertEquals(1, snapshots.size());
   }



   /*******************************************************************************
    ** A pipe terminated before the read delivers no rows and records a zero count.
    ** This does not certify concurrent cancellation or an outcome field.
    ******************************************************************************/
   @Test
   void testPreterminatedPlainPipeRecordsZeroDeliveredRows() throws Exception
   {
      RecordPipe pipe = new RecordPipe(10);
      pipe.terminate();
      new QueryAction().execute(new QueryInput("person").withRecordPipe(pipe));
      assertTrue(pipe.consumeAvailableRecords().isEmpty());
      assertEquals(0, pipe.getTotalRecordCount());
      assertEquals(1, snapshots.size());
      assertEquals(0, snapshots.get(0).count());
      assertEquals(5, rows("SELECT id FROM person").size());
   }



   /*******************************************************************************
    ** The real scheduler flushes with the supplied storage session, clears its
    ** context between jobs and terminates when configuration disables the manager.
    ******************************************************************************/
   @Test
   void testScheduledFlushUsesOwnedSessionAndStopsWithoutWorkerLeak() throws Exception
   {
      manager.stop();
      awaitWorkersStopped();
      QSession storageSession = new QSession();
      storageSession.setUuid(UUID.randomUUID().toString());
      var calls = new AtomicInteger();
      var dirtyWorker = new AtomicBoolean();
      StorageObserver.contexts.clear();
      instance.getTable("queryStat").withCapability(Capability.QUERY_STATS);
      instance.getTable("queryStat").withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(StorageObserver.class));
      manager.setJobInitialDelay(1);
      manager.setJobPeriodSeconds(1);
      manager.start(instance, () ->
      {
         calls.incrementAndGet();
         if(QContext.getQInstance() != null || QContext.getQSession() != null)
         {
            dirtyWorker.set(true);
         }
         return storageSession;
      });
      try
      {
         new QueryAction().execute(new QueryInput("person"));
         long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(6);
         while(rows("SELECT id FROM query_stat").isEmpty() || calls.get() < 2)
         {
            assertTrue(System.nanoTime() < deadline, "Scheduled flush did not persist and run again");
            Thread.sleep(20);
         }
         assertEquals(List.of(List.of(session.getUuid())), rows("SELECT session_id FROM query_stat"));
         assertEquals(1, StorageObserver.contexts.size());
         assertEquals(1, snapshots.size(), "Scheduled storage observed its own work");
         assertSame(instance, StorageObserver.contexts.get(0).instance());
         assertSame(storageSession, StorageObserver.contexts.get(0).session());
         assertTrue(StorageObserver.contexts.get(0).thread().startsWith("QueryStatManager-"));
         assertFalse(dirtyWorker.get());
         assertSame(instance, QContext.getQInstance());
         assertSame(session, QContext.getQSession());
         System.setProperty(ENABLED, "false");
         awaitWorkersStopped();
         new QueryAction().execute(new QueryInput("person"));
         assertEquals(1, snapshots.size());
         assertEquals(1, rows("SELECT id FROM query_stat").size());
      }
      finally
      {
         manager.stop();
         awaitWorkersStopped();
         StorageObserver.contexts.clear();
      }
   }



   /*******************************************************************************
    ** Stop drops unflushed work; a sequential restart stores only the new session.
    ******************************************************************************/
   @Test
   void testStopDropsPendingBatchAndSequentialRestartKeepsSessionIdentity() throws Exception
   {
      new QueryAction().execute(new QueryInput("person"));
      manager.stop();
      awaitWorkersStopped();
      assertTrue(rows("SELECT id FROM query_stat").isEmpty());
      QSession nextSession = new QSession();
      nextSession.setUuid(UUID.randomUUID().toString());
      QContext.init(instance, nextSession);
      new QueryAction().execute(new QueryInput("person"));
      assertEquals(1, snapshots.size(), "Stopped manager must not deliver statistics");
      manager.start(instance, QSession::new);
      new QueryAction().execute(new QueryInput("person"));
      flushOnOwnedThread();
      assertEquals(List.of(List.of(nextSession.getUuid())), rows("SELECT session_id FROM query_stat"));
      assertEquals(List.of(session.getUuid(), nextSession.getUuid()), snapshots.stream().map(Snapshot::session).toList());
      assertSame(nextSession, QContext.getQSession());
   }



   /*******************************************************************************
    ** Statistics describe the completed backend action, not request success.
    ** A later customization failure must propagate without erasing that measurement.
    ******************************************************************************/
   @Test
   void testPostQueryRejectionRetainsCompletedBackendMeasurement() throws Exception
   {
      instance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(FailingPostQuery.class));
      List<List<String>> before = rows("SELECT id,first_name,last_name FROM person ORDER BY id");
      assertEquals(5, before.size());
      QException failure = assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput("person")));
      assertEquals("owned post-query failure", failure.getMessage());
      assertEquals(before, rows("SELECT id,first_name,last_name FROM person ORDER BY id"));
      assertEquals(1, snapshots.size());
      Snapshot stat = snapshots.get(0);
      assertEquals("person", stat.table());
      assertEquals("QueryAction", stat.backendAction());
      assertEquals(5, stat.count());
      assertEquals(session.getUuid(), stat.session());
      assertNotNull(stat.start());
      assertNotNull(stat.first());
      assertFalse(stat.first().isBefore(stat.start()));
      assertTrue(stat.millis() >= 0);
      assertTrue(stat.sql().contains("person"));
      assertTrue(rows("SELECT id FROM query_stat").isEmpty());
      flushOnOwnedThread();
      assertEquals(List.of(List.of(session.getUuid(), "person", stat.sql())),
         rows("SELECT s.session_id,t.name,s.query_text FROM query_stat s JOIN qqq_table t ON t.id=s.qqq_table_id"));
      try(Statement statement = oracle.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM query_stat"))
      {
         assertTrue(result.next());
         assertEquals(stat.start(), result.getObject("start_timestamp", LocalDateTime.class).toInstant(ZoneOffset.UTC));
         assertEquals(stat.first(), result.getObject("first_result_timestamp", LocalDateTime.class).toInstant(ZoneOffset.UTC));
         assertEquals(stat.millis(), result.getInt("first_result_millis"));
         assertFalse(result.next());
      }
      assertSame(instance, QContext.getQInstance());
      assertSame(session, QContext.getQSession());
   }



   /*******************************************************************************
    ** Fault injection at the normal public customization boundary, after native read.
    ******************************************************************************/
   public static class FailingPostQuery implements TableCustomizerInterface
   {
      /*******************************************************************************
       ** Reject delivery without changing any native row.
       ******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records) throws QException
      {
         throw new QException("owned post-query failure");
      }
   }



   /*******************************************************************************
    ** Observe actual InsertAction storage context without substituting persistence.
    ******************************************************************************/
   public static class StorageObserver implements TableCustomizerInterface
   {
      static final List<StorageContext> contexts = new CopyOnWriteArrayList<>();



      /*******************************************************************************
       ** Read-only observation; the native insert continues unchanged.
       ******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean preview)
      {
         contexts.add(new StorageContext(QContext.getQInstance(), QContext.getQSession(), Thread.currentThread().getName()));
         return records;
      }
   }



   /*******************************************************************************
    ** Identity-only capture cleared before fixture teardown.
    ******************************************************************************/
   private record StorageContext(QInstance instance, QSession session, String thread)
   {
   }



   /*******************************************************************************
    ** A context-free caller must remain context-free after an explicit flush.
    ******************************************************************************/
   private void flushOnOwnedThread() throws Exception
   {
      try(var executor = Executors.newSingleThreadExecutor())
      {
         executor.submit(() ->
         {
            manager.storeStatsNow();
            assertNull(QContext.getQInstance());
            assertNull(QContext.getQSession());
         }).get(5, TimeUnit.SECONDS);
      }
   }
}
