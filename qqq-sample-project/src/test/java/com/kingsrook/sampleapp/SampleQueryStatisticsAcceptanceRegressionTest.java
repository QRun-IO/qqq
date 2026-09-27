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


import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditDetailAccumulator;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStatMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.tables.QQQTablesMetaDataProvider;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.SimpleConnectionProvider;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*******************************************************************************
 ** Native persistence regressions for query-statistics lifecycle and isolation.
 ******************************************************************************/
class SampleQueryStatisticsAcceptanceRegressionTest extends SampleQueryStatisticsAcceptanceFixture
{
   /*******************************************************************************
    ** A real statistics insert gets private named objects, preserving caller audit
    ** accumulation, mutable values and map identity across successful storage.
    ******************************************************************************/
   @Test
   void testNativeFlushPreservesAndIsolatesNamedObjects() throws Exception
   {
      assertNativeNamedObjectIsolation(false);
   }



   /*******************************************************************************
    ** Native storage failure must restore the caller map without leaking objects
    ** created by customization or changing the existing dropped-batch policy.
    ******************************************************************************/
   @Test
   void testFailedNativeFlushPreservesAndIsolatesNamedObjects() throws Exception
   {
      assertNativeNamedObjectIsolation(true);
   }



   /*******************************************************************************
    ** Observe and mutate only through the normal pre-insert customization seam;
    ** native JDBC independently proves whether the real storage write succeeded.
    ******************************************************************************/
   private void assertNativeNamedObjectIsolation(boolean failStorage) throws Exception
   {
      NamedObjectStorageObserver.observations.clear();
      manager.start(instance, () ->
      {
         QContext.setObject("flushSupplier", "owned supplier value");
         return new QSession();
      });
      instance.getTable("queryStat").withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(NamedObjectStorageObserver.class));
      var details = (RDBMSTableBackendDetails) instance.getTable("queryStat").getBackendDetails();
      if(failStorage)
      {
         details.setTableName("missing_owned_query_stat");
      }
      try
      {
         new QueryAction().execute(new QueryInput("person"));
         var payload = new ArrayList<>(List.of("owned by caller"));
         var audit = new AuditDetailAccumulator("owned caller audit");
         audit.addAuditDetail("person", new QRecord().withValue("id", 1), "existing detail");
         Map<String, Serializable> callerObjects = new HashMap<>();
         QContext.setObjects(callerObjects);
         QContext.setObject("callerPayload", payload);
         audit.setInContext();
         var expected = new HashMap<>(callerObjects);
         var caller = QContext.capture();

         manager.storeStatsNow();
         assertEquals(failStorage ? 0 : 1, rows("SELECT id FROM query_stat").size());
         assertEquals(1, NamedObjectStorageObserver.observations.size());
         var storage = NamedObjectStorageObserver.observations.get(0);
         assertNotSame(callerObjects, storage.objects());
         assertEquals("owned supplier value", storage.objects().get("flushSupplier"));
         assertEquals("owned by flush", storage.objects().get("flushOnly"));
         assertNull(storage.payload(), "Storage inherited a caller-owned mutable value");
         assertNull(storage.audit(), "Storage inherited the caller audit accumulator");
         assertSame(callerObjects, QContext.getObjects());
         assertEquals(expected, callerObjects);
         assertSame(payload, QContext.getObject("callerPayload"));
         assertEquals(List.of("owned by caller"), payload);
         assertSame(audit, AuditDetailAccumulator.getFromContext().orElseThrow());
         assertEquals(1, audit.getAccumulatedAuditSingleInputs().size());
         assertNull(QContext.getObject("flushOnly"));
         assertNull(QContext.getObject("flushSupplier"));
         assertEquals(caller, QContext.capture());

         details.setTableName("query_stat");
         manager.storeStatsNow();
         assertSame(callerObjects, QContext.getObjects());
         assertEquals(expected, callerObjects);
         assertEquals(failStorage ? 0 : 1, rows("SELECT id FROM query_stat").size(), "A dropped batch must not be retried");
      }
      finally
      {
         NamedObjectStorageObserver.observations.clear();
      }
   }



   /*******************************************************************************
    ** The real insert continues after observing and changing its own object map.
    ******************************************************************************/
   public static class NamedObjectStorageObserver implements TableCustomizerInterface
   {
      static final List<NamedObjectObservation> observations = new ArrayList<>();



      /*******************************************************************************
       ** Attempt changes to visible objects so accidental sharing is observable.
       ******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean preview)
      {
         var audit = AuditDetailAccumulator.getFromContext().orElse(null);
         Serializable payload = QContext.getObject("callerPayload");
         observations.add(new NamedObjectObservation(QContext.getObjects(), payload, audit));
         if(audit != null)
         {
            audit.clear();
         }
         if(payload instanceof ArrayList<?> list)
         {
            list.clear();
         }
         QContext.setObject("callerPayload", "storage replacement");
         QContext.setObject("flushOnly", "owned by flush");
         return records;
      }
   }



   /*******************************************************************************
    ** Retain observed identities until the test asserts isolation and clears them.
    ******************************************************************************/
   private record NamedObjectObservation(Map<String, Serializable> objects, Serializable payload, AuditDetailAccumulator audit)
   {
   }



   /*******************************************************************************
    ** A short association buffer must report the delivered tail.
    ******************************************************************************/
   @Test
   void testAssociationShortTailCount() throws Exception
   {
      assertAssociationCount(3);
   }



   /*******************************************************************************
    ** A full association batch must report its delivered row count.
    ******************************************************************************/
   @Test
   void testAssociationFullBatchCount() throws Exception
   {
      assertAssociationCount(100);
   }



   /*******************************************************************************
    ** A full batch and final tail must both appear in the snapshot count.
    ******************************************************************************/
   @Test
   void testAssociationFullBatchAndTailCount() throws Exception
   {
      assertAssociationCount(105);
   }



   /*******************************************************************************
    ** Independent native count, actual delivery and actual child records precede
    ** the desired statistics assertion; setup errors cannot masquerade as this bug.
    ******************************************************************************/
   private void assertAssociationCount(Integer size) throws Exception
   {
      try(Statement statement = oracle.createStatement())
      {
         statement.executeUpdate("DELETE FROM person");
         statement.executeUpdate("INSERT INTO person(id,first_name,last_name,email) SELECT x,'Owned','Tail','owned@example.invalid' FROM SYSTEM_RANGE(1," + size + ")");
      }
      RecordPipe pipe = new RecordPipe(1000);
      new QueryAction().execute(new QueryInput("person").withIncludeAssociations(true).withRecordPipe(pipe)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id"))));
      List<QRecord> records = pipe.consumeAvailableRecords();
      assertEquals(size, rows("SELECT id FROM person").size());
      assertEquals(size, records.size());
      assertEquals(4, records.get(0).getAssociatedRecords().get("pets").size());
      Snapshot stat = snapshots.stream().filter(snapshot -> snapshot.table().equals("person")).findFirst().orElseThrow();
      assertEquals(size, stat.count());
   }



   /*******************************************************************************
    ** An explicit flush must leave the caller's instance and session intact.
    ******************************************************************************/
   @Test
   void testExplicitFlushPreservesCallerContext() throws Exception
   {
      new QueryAction().execute(new QueryInput("person"));
      try(var transaction = QBackendTransaction.openFor(new QueryInput("person")))
      {
         QContext.init(instance, session, transaction, new QueryInput("person"));
         var before = QContext.capture();
         manager.storeStatsNow();
         assertEquals(before, QContext.capture());
         assertSame(transaction, QContext.getQBackendTransaction());
         assertEquals(List.of(List.of(session.getUuid())), rows("SELECT session_id FROM query_stat"));
         manager.storeStatsNow();
         assertEquals(before, QContext.capture());
         assertSame(instance, QContext.getQInstance());
         assertSame(session, QContext.getQSession());
         transaction.rollback();
      }
      QContext.init(instance, session);
   }




   /*******************************************************************************
    ** An active manager for instance A must not persist instance B's statistics.
    ******************************************************************************/
   @Test
   void testForeignInstanceDoesNotWriteToStartedInstance() throws Exception
   {
      QInstance foreign = SampleMetaDataProvider.defineTestInstance();
      RDBMSBackendMetaData foreignBackend = SampleMetaDataProvider.defineRdbmsBackend();
      foreignBackend.setName("foreignStats" + UUID.randomUUID());
      foreignBackend.setDatabaseName("foreign_stats_" + UUID.randomUUID().toString().replace("-", ""));
      foreignBackend.setJdbcUrl("jdbc:h2:mem:" + foreignBackend.getDatabaseName() + ";MODE=MySQL");
      foreignBackend.setConnectionProvider(new QCodeReference(SimpleConnectionProvider.class));
      foreignBackend.withCapability(Capability.QUERY_STATS);
      foreign.addBackend(foreignBackend);
      foreign.getTable("person").setBackendName(foreignBackend.getName());
      new QQQTablesMetaDataProvider().defineAll(foreign, foreignBackend.getName(), foreignBackend.getName(), this::mapStatisticsTable);
      new QueryStatMetaDataProvider().defineAll(foreign, foreignBackend.getName(), this::mapStatisticsTable);
      QSession foreignSession = new QSession();
      foreignSession.setUuid(UUID.randomUUID().toString());
      try(Connection other = DriverManager.getConnection(foreignBackend.getJdbcUrl(), "sa", "");
          var script = new InputStreamReader(getClass().getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(other, script);
         try(Statement statement = other.createStatement())
         {
            statement.executeUpdate("DELETE FROM person WHERE id>1");
         }
         try(var statisticsScript = new InputStreamReader(getClass().getResourceAsStream("/SampleQueryStatisticsAcceptance.sql"), StandardCharsets.UTF_8))
         {
            RunScript.execute(other, statisticsScript);
         }
         QContext.init(foreign, foreignSession);
         assertEquals(1, new QueryAction().execute(new QueryInput("person")).getRecords().size());
         assertTrue(snapshots.isEmpty(), "Foreign instance reached a consumer");
         manager.storeStatsNow();
         assertTrue(rows("SELECT session_id FROM query_stat").isEmpty(),
            "Foreign query was persisted in started instance A: " + rows("SELECT session_id FROM query_stat"));

         QContext.init(instance, session);
         assertEquals(5, new QueryAction().execute(new QueryInput("person")).getRecords().size());
         manager.storeStatsNow();
         assertEquals(List.of(List.of(session.getUuid())), rows("SELECT session_id FROM query_stat"));
         snapshots.clear();
         manager.start(foreign, QSession::new);
         assertEquals(5, new QueryAction().execute(new QueryInput("person")).getRecords().size());
         assertTrue(snapshots.isEmpty(), "Old instance reached replacement generation");
         QContext.init(foreign, foreignSession);
         assertEquals(1, new QueryAction().execute(new QueryInput("person")).getRecords().size());
         assertEquals(1, snapshots.size());
         assertEquals(foreignSession.getUuid(), snapshots.get(0).session());
         manager.storeStatsNow();
         try(Statement statement = other.createStatement(); var stored = statement.executeQuery("SELECT session_id FROM query_stat"))
         {
            assertTrue(stored.next());
            assertEquals(foreignSession.getUuid(), stored.getString(1));
            assertTrue(!stored.next());
         }
         assertEquals(List.of(List.of(session.getUuid())), rows("SELECT session_id FROM query_stat"));
      }
   }



   /*******************************************************************************
    ** Backend-wide opt-in must not let the statistics table collect its own writes.
    ******************************************************************************/
   @Test
   void testStatisticsTableDoesNotCollectItsOwnWrites() throws Exception
   {
      instance.getTable("queryStat").withCapability(Capability.QUERY_STATS);
      new QueryAction().execute(new QueryInput("person"));
      for(int i = 0; i < 3; i++)
      {
         manager.storeStatsNow();
         assertEquals(1, rows("SELECT id FROM query_stat").size(), "Flush persisted self-generated statistics");
         assertEquals(1, snapshots.size(), "Storage reached a consumer");
      }
      assertEquals(1, new QueryAction().execute(new QueryInput("queryStat")).getRecords().size());
      assertEquals(2, snapshots.size(), "A legitimate application query was suppressed");
      manager.storeStatsNow();
      assertEquals(2, rows("SELECT id FROM query_stat").size());
      assertEquals(2, snapshots.size());
   }



   /*******************************************************************************
    ** Check repeated starts in a disposable JVM so a worker regression cannot
    ** contaminate the acceptance JVM. Every previous worker must terminate.
    ******************************************************************************/
   @Test
   void testRepeatedStartThenStopLeavesNoWorker() throws Exception
   {
      Path log = Files.createTempFile("qqq-564-workers-", ".log");
      Process process = null;
      try
      {
         process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-Dlog4j2.configurationFile=SampleQueryStatisticsAcceptance-log4j2.xml",
            "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            SampleQueryStatisticsAcceptanceRegressionTest.class.getName())
            .redirectErrorStream(true).redirectOutput(log.toFile()).start();
         assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Worker probe exceeded deadline");
         assertEquals(0, process.exitValue(), Files.readString(log));
      }
      finally
      {
         if(process != null && process.isAlive())
         {
            process.destroyForcibly();
            assertTrue(process.waitFor(5, TimeUnit.SECONDS));
         }
         Files.deleteIfExists(log);
      }
   }



   /*******************************************************************************
    ** Child process exit makes repeated-start leak diagnostics bounded and owned.
    ******************************************************************************/
   public static void main(String[] args) throws Exception
   {
      System.setProperty(ENABLED, "true");
      var manager = QueryStatManager.getInstance();
      manager.setJobInitialDelay(3600);
      manager.setJobPeriodSeconds(3600);
      manager.start(new QInstance(), QSession::new);
      manager.start(new QInstance(), QSession::new);
      manager.stop();
      for(Thread thread : Thread.getAllStackTraces().keySet())
      {
         if(thread.getName().startsWith("QueryStatManager-"))
         {
            thread.join(500);
         }
      }
      long workers = Thread.getAllStackTraces().keySet().stream()
         .filter(t -> t.isAlive() && t.getName().startsWith("QueryStatManager-")).count();
      System.out.println("QueryStatManager workers after two starts and one stop: " + workers);
      System.exit(workers == 0 ? 0 : 1);
   }



}
