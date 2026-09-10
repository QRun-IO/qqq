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
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.sampleapp.metadata.FieldLabTableMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.Test;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Run Field Lab temporal data through QQQ and real disposable SQL databases.
 ** These containers contain only synthetic sample data and stop after each test.
 *******************************************************************************/
class SampleDatabaseIT
{
   private static final String TABLE = FieldLabTableMetaDataProducer.NAME;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMySqlTemporalValuesAcrossHostTimeZones() throws Exception
   {
      try(MySQLContainer database = new MySQLContainer("mysql:8.4")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only"))
      {
         database.start();
         RDBMSBackendMetaData backend = new RDBMSBackendMetaData().withVendor("mysql")
            .withName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
            .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
            .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword())
            .withQueriesForNewConnections(List.of("SET time_zone = '+00:00'"));
         exerciseTemporalValues(backend, "mysql");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostgresTemporalValuesAcrossHostTimeZones() throws Exception
   {
      try(PostgreSQLContainer database = new PostgreSQLContainer("postgres:17-alpine")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only"))
      {
         database.start();
         RDBMSBackendMetaData backend = new PostgreSQLBackendMetaData()
            .withName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
            .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
            .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword())
            .withQueriesForNewConnections(List.of("SET TIME ZONE 'UTC'"));
         exerciseTemporalValues(backend, "postgres");
      }
   }



   /*******************************************************************************
    ** Real blocked PostgreSQL counts must time out or cancel, then recover.
    *******************************************************************************/
   @Test
   void testPostgresCountTimeoutAndCancellation() throws Exception
   {
      try(PostgreSQLContainer database = new PostgreSQLContainer("postgres:17-alpine")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only"))
      {
         database.start();
         RDBMSBackendMetaData backend = new PostgreSQLBackendMetaData()
            .withName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
            .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
            .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword())
            .withQueriesForNewConnections(List.of("SET TIME ZONE 'UTC'", "SET application_name = 'qqq-sample-count'"));
         try
         {
            ConnectionManager.resetConnectionProviders();
            QInstance instance = SampleMetaDataProvider.defineTestInstance();
            instance.getBackends().put(backend.getName(), backend);
            instance.getTable(TABLE).setBackendDetails(new PostgreSQLTableBackendDetails().withTableName("field_lab"));
            QContext.init(instance, new QSession());
            try(Connection connection = ConnectionManager.getConnection(backend);
                Statement statement = connection.createStatement();
                InputStream schema = SampleDatabaseIT.class.getResourceAsStream("/database/field-lab-postgres.sql"))
            {
               assertNotNull(schema);
               statement.execute(new String(schema.readAllBytes(), StandardCharsets.UTF_8));
               statement.executeUpdate("INSERT INTO field_lab(name) VALUES ('count-recovery')");
            }
            exerciseBlockedPostgresCount(instance, backend, true);
            exerciseBlockedPostgresCount(instance, backend, false);
         }
         finally
         {
            QContext.clear();
            ConnectionManager.resetConnectionProviders();
         }
      }
   }



   /*******************************************************************************
    ** Hold a table lock until the driver has ended the count; release on any failure.
    *******************************************************************************/
   private void exerciseBlockedPostgresCount(QInstance instance, RDBMSBackendMetaData backend, boolean timeout) throws Exception
   {
      ExecutorService worker = Executors.newSingleThreadExecutor();
      CountAction action = new CountAction();
      try(Connection lock = ConnectionManager.getConnection(backend);
          Connection monitor = ConnectionManager.getConnection(backend))
      {
         lock.setAutoCommit(false);
         try(Statement statement = lock.createStatement())
         {
            statement.execute("LOCK TABLE field_lab IN ACCESS EXCLUSIVE MODE");
            int lockPid;
            try(ResultSet result = statement.executeQuery("SELECT pg_backend_pid()"))
            {
               assertTrue(result.next());
               lockPid = result.getInt(1);
            }
            Future<CountOutput> count = worker.submit(() ->
            {
               QContext.init(instance, new QSession());
               try
               {
                  return action.execute(new CountInput(TABLE).withTimeoutSeconds(timeout ? 3 : null));
               }
               finally
               {
                  QContext.clear();
               }
            });
            int countPid = awaitBlockedCount(monitor, lockPid);
            if(!timeout)
            {
               action.cancel();
            }
            ExecutionException failure = assertThrows(ExecutionException.class, () -> count.get(10, TimeUnit.SECONDS));
            QUserFacingException cause = assertInstanceOf(QUserFacingException.class, failure.getCause());
            assertEquals(timeout ? "Count timed out." : "Count was cancelled.", cause.getMessage());
            try(PreparedStatement waiting = monitor.prepareStatement("SELECT 1 FROM pg_locks WHERE pid = ? AND NOT granted"))
            {
               waiting.setInt(1, countPid);
               try(ResultSet result = waiting.executeQuery())
               {
                  assertFalse(result.next(), "The completed count must leave no blocked PostgreSQL request");
               }
            }
         }
         finally
         {
            lock.rollback();
         }
      }
      finally
      {
         worker.shutdownNow();
         assertTrue(worker.awaitTermination(10, TimeUnit.SECONDS), "Count worker must terminate after releasing its owned lock");
      }
      assertEquals(1, CountAction.execute(TABLE, null), "A fresh count must recover after " + (timeout ? "timeout" : "cancellation"));
   }



   /*******************************************************************************
    ** Synchronize on the real driver query waiting for this scenario's table lock.
    *******************************************************************************/
   private int awaitBlockedCount(Connection monitor, int lockPid) throws Exception
   {
      String sql = "SELECT activity.pid FROM pg_stat_activity activity JOIN pg_locks locks USING (pid) "
         + "WHERE locks.relation = 'field_lab'::regclass AND NOT locks.granted "
         + "AND activity.wait_event_type = 'Lock' AND activity.application_name = 'qqq-sample-count' "
         + "AND activity.query ILIKE 'SELECT COUNT(%' AND ? = ANY(pg_blocking_pids(activity.pid))";
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      try(PreparedStatement statement = monitor.prepareStatement(sql))
      {
         statement.setQueryTimeout(2);
         statement.setInt(1, lockPid);
         while(System.nanoTime() < deadline)
         {
            try(ResultSet result = statement.executeQuery())
            {
               if(result.next())
               {
                  return result.getInt(1);
               }
            }
            Thread.sleep(25);
         }
      }
      return fail("PostgreSQL never observed the Count query blocked by the owned table lock");
   }



   /*******************************************************************************
    ** Independent SQL reads ensure matching write/read mistakes cannot cancel out.
    *******************************************************************************/
   private void exerciseTemporalValues(RDBMSBackendMetaData backend, String vendor) throws Exception
   {
      TimeZone original = TimeZone.getDefault();
      try
      {
         ConnectionManager.resetConnectionProviders();
         QInstance instance = SampleMetaDataProvider.defineTestInstance();
         instance.getBackends().put(backend.getName(), backend);
         if(vendor.equals("postgres"))
         {
            instance.getTable(TABLE).setBackendDetails(new PostgreSQLTableBackendDetails().withTableName("field_lab"));
         }
         QContext.init(instance, new QSession());
         try(Connection connection = ConnectionManager.getConnection(backend);
             Statement statement = connection.createStatement();
             InputStream schema = SampleDatabaseIT.class.getResourceAsStream("/database/field-lab-" + vendor + ".sql"))
         {
            assertTrue(connection.getMetaData().getDatabaseProductName().toLowerCase(java.util.Locale.ROOT).startsWith(vendor));
            assertNotNull(schema);
            statement.execute(new String(schema.readAllBytes(), StandardCharsets.UTF_8));
         }
         for(String zone : List.of("UTC", "America/Chicago", "Asia/Tokyo"))
         {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            ConnectionManager.resetConnectionProviders();
            LocalDate date = LocalDate.of(2024, 2, 29);
            LocalTime time = LocalTime.of(23, 59, 58);
            Instant instant = Instant.parse("2026-03-08T07:59:59Z");
            QRecord inserted = InsertAction.executeForRecords(new InsertInput().withTableName(TABLE)
               .withRecord(new QRecord().withValue("name", vendor + "-" + zone)
                  .withValue("dateValue", date).withValue("timeValue", time).withValue("dateTimeValue", instant))).get(0);
            assertTrue(inserted.getErrors().isEmpty(), String.valueOf(inserted.getErrors()));
            QRecord stored = GetAction.execute(TABLE, inserted.getValueInteger("id"));
            assertEquals(date, stored.getValueLocalDate("dateValue"), zone);
            assertEquals(time, stored.getValueLocalTime("timeValue"), zone);
            assertEquals(instant, stored.getValueInstant("dateTimeValue"), zone);
            try(Connection connection = ConnectionManager.getConnection(backend);
                PreparedStatement statement = connection.prepareStatement("SELECT date_value, time_value, date_time_value FROM field_lab WHERE id = ?"))
            {
               statement.setInt(1, inserted.getValueInteger("id"));
               try(ResultSet values = statement.executeQuery())
               {
                  assertTrue(values.next());
                  assertEquals("2024-02-29", values.getString(1), zone);
                  assertEquals("23:59:58", values.getString(2), zone);
                  assertTrue(values.getString(3).startsWith("2026-03-08 07:59:59"), zone + ": " + values.getString(3));
                  assertFalse(values.next());
               }
            }
            QRecord updated = UpdateAction.executeForRecords(new UpdateInput().withTableName(TABLE)
               .withRecord(new QRecord().withValue("id", inserted.getValueInteger("id"))
                  .withValue("dateValue", LocalDate.of(2024, 3, 1)).withValue("timeValue", LocalTime.MIDNIGHT))).get(0);
            assertTrue(updated.getErrors().isEmpty(), String.valueOf(updated.getErrors()));
            stored = GetAction.execute(TABLE, inserted.getValueInteger("id"));
            assertEquals(LocalDate.of(2024, 3, 1), stored.getValueLocalDate("dateValue"), zone);
            assertEquals(LocalTime.MIDNIGHT, stored.getValueLocalTime("timeValue"), zone);
            assertEquals(instant, stored.getValueInstant("dateTimeValue"), zone);
            updated = UpdateAction.executeForRecords(new UpdateInput().withTableName(TABLE)
               .withRecord(new QRecord().withValue("id", inserted.getValueInteger("id"))
                  .withValue("dateValue", null).withValue("timeValue", null).withValue("dateTimeValue", null))).get(0);
            assertTrue(updated.getErrors().isEmpty(), String.valueOf(updated.getErrors()));
            stored = GetAction.execute(TABLE, inserted.getValueInteger("id"));
            assertNull(stored.getValue("dateValue"));
            assertNull(stored.getValue("timeValue"));
            assertNull(stored.getValue("dateTimeValue"));
         }
      }
      finally
      {
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
         TimeZone.setDefault(original);
      }
   }
}
