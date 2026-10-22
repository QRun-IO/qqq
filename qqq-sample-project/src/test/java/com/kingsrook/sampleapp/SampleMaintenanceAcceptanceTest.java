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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.NowWithOffset;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.processes.implementations.columnstats.ColumnStatsStep;
import com.kingsrook.qqq.backend.core.processes.implementations.garbagecollector.GarbageCollectorProcessMetaDataProducer;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Registered maintenance processes against owned sample H2 rows and native SQL.
 *******************************************************************************/
class SampleMaintenanceAcceptanceTest
{
   private CapturedContext previousContext;
   private Map<String, Serializable> previousObjects;
   private QInstance instance;
   private QSession session;
   private Connection anchor;
   private String jdbcUrl;
   private Instant now;



   /*******************************************************************************
    ** Reuse the sample schema in a uniquely named database; never start its server.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousContext = QContext.capture();
      previousObjects = QContext.getObjects();
      QContext.setObjects(new LinkedHashMap<>());
      ConnectionManager.resetConnectionProviders();
      jdbcUrl = "jdbc:h2:mem:maintenance_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=0";
      anchor = DriverManager.getConnection(jdbcUrl, "sa", "");
      try(InputStreamReader reader = new InputStreamReader(SampleMetaDataProvider.class.getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(anchor, reader);
      }
      instance = SampleMetaDataProvider.defineTestInstance();
      RDBMSBackendMetaData backend = SampleMetaDataProvider.defineRdbmsBackend().withName("maintenanceDatabase").withJdbcUrl(jdbcUrl);
      instance.addBackend(backend);
      instance.getTable("fieldLab").setBackendName(backend.getName());
      instance.addProcess(GarbageCollectorProcessMetaDataProducer.createProcess("fieldLab", "dateTimeValue", NowWithOffset.minus(30, ChronoUnit.DAYS), null));
      instance.addProcess(ColumnStatsStep.getProcessMetaData());
      session = new QSession();
      QContext.init(instance, session);
      QContext.setObject("maintenanceFixture", "owned");
      now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
      seed(1, "expired-allowed", now.minus(90, ChronoUnit.DAYS), "allowed");
      seed(2, "expired-other", now.minus(60, ChronoUnit.DAYS), "other");
      seed(3, "recent", now.minus(1, ChronoUnit.DAYS), "allowed");
      seed(4, "undated", null, "allowed");
   }



   /*******************************************************************************
    ** Close the entire owned database and restore both ordinary and named context.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(anchor != null)
         {
            try(Statement statement = anchor.createStatement())
            {
               statement.execute("SHUTDOWN");
            }
            finally
            {
               anchor.close();
            }
         }
      }
      finally
      {
         ConnectionManager.resetConnectionProviders();
         QContext.clear();
         QContext.init(previousContext);
         QContext.setObjects(previousObjects);
      }
   }



   /*******************************************************************************
    ** Configured age removes only old rows; SQL observes persisted survivors and
    ** a repeat invocation leaves every survivor column unchanged.
    *******************************************************************************/
   @Test
   void testConfiguredExpiryAndRepeatPreserveNonexpiredRecords() throws Exception
   {
      seed(5, "just-expired", now.minus(30, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS), "allowed");
      seed(6, "not-yet-expired", now.minus(30, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS), "allowed");
      Map<Integer, List<String>> expected = snapshot();
      expected.remove(1);
      expected.remove(2);
      expected.remove(5);
      assertEquals(List.of(1, 2, 3, 4, 5, 6), new ArrayList<>(snapshot().keySet()));
      assertSuccess(cleanup(null));
      assertEquals(expected, snapshot());
      assertSuccess(cleanup(null));
      assertEquals(expected, snapshot());
   }



   /*******************************************************************************
    ** The documented limitDate override is strictly less-than, not inclusive.
    *******************************************************************************/
   @Test
   void testExplicitCutoffPreservesEqualAndLaterDates() throws Exception
   {
      Instant cutoff = now.minus(60, ChronoUnit.DAYS);
      Map<Integer, List<String>> expected = snapshot();
      expected.remove(1);
      assertSuccess(cleanup(cutoff));
      assertEquals(expected, snapshot());
   }



   /*******************************************************************************
    ** A configured READ lock excludes other owners even from trusted cleanup.
    ** Granting that owner later proves the expired row was eligible all along.
    *******************************************************************************/
   @Test
   void testReadDeniedExpiredRecordsRemainUntilGranted() throws Exception
   {
      assertDeniedRecordsRemainUntilGranted(RecordSecurityLock.LockScope.READ);
   }



   /*******************************************************************************
    ** A WRITE-only lock allows extraction but still prevents the delete itself.
    *******************************************************************************/
   @Test
   void testWriteDeniedExpiredRecordsRemainUntilGranted() throws Exception
   {
      assertDeniedRecordsRemainUntilGranted(RecordSecurityLock.LockScope.WRITE);
   }



   /*******************************************************************************
    ** Empty cleanup is repeatable; empty statistics match native SQL null/count
    ** values instead of inventing zero-valued averages, extrema or sums.
    *******************************************************************************/
   @Test
   void testEmptyCleanupAndStatistics() throws Exception
   {
      try(Statement statement = anchor.createStatement())
      {
         assertEquals(4, statement.executeUpdate("DELETE FROM field_lab"));
      }
      assertSuccess(cleanup(null));
      assertSuccess(cleanup(null));
      RunProcessOutput output = run(columnStatsInput());
      assertSuccess(output);
      assertEquals(List.of(), output.getValues().get("valueCounts"));
      QRecord stats = assertInstanceOf(QRecord.class, output.getValues().get("statsRecord"));
      try(Statement statement = anchor.createStatement();
         ResultSet nativeStats = statement.executeQuery("SELECT COUNT(long_value),COUNT(DISTINCT long_value),SUM(long_value),AVG(long_value),MIN(long_value),MAX(long_value) FROM field_lab"))
      {
         assertTrue(nativeStats.next());
         List<String> names = List.of("count", "countDistinct", "sum", "average", "min", "max");
         assertEquals(Set.copyOf(names), stats.getValues().keySet());
         for(int column = 1; column <= names.size(); column++)
         {
            Object expected = nativeStats.getObject(column);
            Object actual = stats.getValue(names.get(column - 1));
            assertEquals(expected == null ? null : new BigDecimal(expected.toString()).stripTrailingZeros(),
               actual == null ? null : new BigDecimal(actual.toString()).stripTrailingZeros(), names.get(column - 1));
         }
      }
      assertEquals(Map.of(), snapshot());
   }



   /*******************************************************************************
    ** A configured nonexistent timestamp column must fail before any deletion.
    *******************************************************************************/
   @Test
   void testInvalidCleanupFieldPreservesEveryRecord() throws Exception
   {
      Map<Integer, List<String>> before = snapshot();
      instance.addProcess(GarbageCollectorProcessMetaDataProducer.createProcess("fieldLab", "missingExpiryField", NowWithOffset.minus(30, ChronoUnit.DAYS), null)
         .withName("invalidMaintenance"));
      RunProcessInput input = new RunProcessInput().withProcessName("invalidMaintenance");
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      QException failure = assertThrows(QException.class, () -> run(input));
      assertTrue(causeMessages(failure).contains("missingExpiryField"), causeMessages(failure));
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** The canonical column-stats process rejects an unavailable operand without
    ** modifying its source records, then works again with valid configuration.
    *******************************************************************************/
   @Test
   void testInvalidStatisticsFieldPreservesRecordsAndRecovers() throws Exception
   {
      Map<Integer, List<String>> before = snapshot();
      RunProcessInput input = columnStatsInput();
      input.addValue("fieldName", "missingStatsField");
      QException failure = assertThrows(QException.class, () -> run(input));
      assertTrue(causeMessages(failure).contains("Field [missingStatsField] was not found in table [fieldLab]"), causeMessages(failure));
      assertEquals(before, snapshot());
      RunProcessOutput recovered = run(columnStatsInput());
      assertSuccess(recovered);
      assertEquals(4, assertInstanceOf(QRecord.class, recovered.getValues().get("statsRecord")).getValueInteger("count"));
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** A malformed supplemental config is an error, not unrestricted fallback.
    *******************************************************************************/
   @Test
   void testInvalidStatisticsConfigurationPreservesRecords() throws Exception
   {
      Map<Integer, List<String>> before = snapshot();
      RunProcessInput input = columnStatsInput();
      input.addValue("ColumnStatsTableConfig", "not-a-column-stats-config");
      QException failure = assertThrows(QException.class, () -> run(input));
      assertTrue(causeMessages(failure).contains("ColumnStatsTableConfig"), causeMessages(failure));
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Canonical registered process inputs; numeric fixture-value evidence is also
    ** covered by SampleAggregateColumnStatsContractTest's independent SQL oracle.
    *******************************************************************************/
   private RunProcessInput columnStatsInput()
   {
      RunProcessInput input = new RunProcessInput().withProcessName("columnStats");
      input.addValue("tableName", "fieldLab");
      input.addValue("fieldName", "longValue");
      return input;
   }



   /*******************************************************************************
    ** Confirm specific configuration failure without matching backend stack traces.
    *******************************************************************************/
   private String causeMessages(Throwable failure)
   {
      StringBuilder messages = new StringBuilder();
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         messages.append(cause.getMessage()).append('\n');
      }
      return messages.toString();
   }



   /*******************************************************************************
    ** Observe every column via unrestricted native SQL, with an allowed deletion
    ** in the same invocation and an explicit later grant as a positive control.
    *******************************************************************************/
   private void assertDeniedRecordsRemainUntilGranted(RecordSecurityLock.LockScope scope) throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("maintenanceOwner"));
      instance.getTable("fieldLab").withRecordSecurityLock(new RecordSecurityLock().withFieldName("textValue")
         .withSecurityKeyType("maintenanceOwner").withLockScope(scope).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      session.withSecurityKeyValue("maintenanceOwner", "allowed");
      seed(5, "expired-no-owner", now.minus(90, ChronoUnit.DAYS), null);
      Map<Integer, List<String>> expected = snapshot();
      expected.remove(1);
      assertSuccess(cleanup(null));
      assertEquals(expected, snapshot());
      session.withSecurityKeyValue("maintenanceOwner", "other");
      expected.remove(2);
      assertSuccess(cleanup(null));
      assertEquals(expected, snapshot());
   }



   /*******************************************************************************
    ** Native seeding prevents the write API from becoming its own readback oracle.
    *******************************************************************************/
   private void seed(int id, String name, Instant date, String owner) throws Exception
   {
      try(PreparedStatement statement = anchor.prepareStatement("INSERT INTO field_lab(id,name,date_time_value,text_value,long_value) VALUES(?,?,?,?,?)"))
      {
         statement.setInt(1, id);
         statement.setString(2, name);
         statement.setObject(3, date == null ? null : LocalDateTime.ofInstant(date, ZoneOffset.UTC));
         statement.setString(4, owner);
         statement.setLong(5, id * 10L);
         assertEquals(1, statement.executeUpdate());
      }
   }



   /*******************************************************************************
    ** A fresh UUID isolates and then removes each registered process's state.
    *******************************************************************************/
   private RunProcessOutput cleanup(Instant cutoff) throws Exception
   {
      RunProcessInput input = new RunProcessInput().withProcessName("fieldLabGarbageCollector");
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      if(cutoff != null)
      {
         input.addValue("limitDate", cutoff);
      }
      return run(input);
   }



   /*******************************************************************************
    ** Common state ownership and caller-context preservation for both processes.
    *******************************************************************************/
   private RunProcessOutput run(RunProcessInput input) throws Exception
   {
      UUID id = UUID.randomUUID();
      input.setProcessUUID(id.toString());
      try
      {
         return new RunProcessAction().execute(input);
      }
      finally
      {
         RunProcessAction.getStateProvider().remove(new UUIDAndTypeStateKey(id, StateType.PROCESS_STATUS));
         assertSame(instance, QContext.getQInstance());
         assertSame(session, QContext.getQSession());
         assertEquals("owned", QContext.getObject("maintenanceFixture"));
      }
   }



   /*******************************************************************************
    ** A process may report failure in its output instead of throwing it.
    *******************************************************************************/
   private void assertSuccess(RunProcessOutput output)
   {
      assertTrue(output.getException().isEmpty(), () -> output.getException().toString());
   }



   /*******************************************************************************
    ** Independent JDBC sees all persisted columns, not only the QQQ projection.
    *******************************************************************************/
   private Map<Integer, List<String>> snapshot() throws Exception
   {
      Map<Integer, List<String>> rows = new LinkedHashMap<>();
      try(Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT * FROM field_lab ORDER BY id"))
      {
         while(result.next())
         {
            List<String> values = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               values.add(result.getString(column));
            }
            rows.put(result.getInt("id"), values);
         }
      }
      return rows;
   }
}
