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


import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreInsertCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.sampleapp.metadata.FieldLabTableMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Native constraints protect final stored values beyond application preflight.
 ** Only these disposable test schemas gain the decimal/composite variations.
 *******************************************************************************/
class SampleStorageConstraintTest
{
   private static final String TABLE = FieldLabTableMetaDataProducer.NAME;
   private static volatile RaceBarrier raceBarrier;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testH2NativeConstraintsCoverRoundingCollationAndConcurrentWrites() throws Exception
   {
      exerciseNativeConstraints(SampleMetaDataProvider.defineRdbmsBackend(), "h2");
   }



   /*******************************************************************************
    ** Shared only by the named H2/MySQL/PostgreSQL sample acceptance methods.
    *******************************************************************************/
   static void exerciseNativeConstraints(RDBMSBackendMetaData backend, String vendor) throws Exception
   {
      try
      {
         ConnectionManager.resetConnectionProviders();
         if(vendor.equals("h2"))
         {
            SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
         }
         QInstance instance = SampleMetaDataProvider.defineTestInstance();
         instance.getBackends().put(backend.getName(), backend);
         QTableMetaData table = instance.getTable(TABLE);
         if(vendor.equals("postgres"))
         {
            table.setBackendDetails(new PostgreSQLTableBackendDetails().withTableName("field_lab"));
         }
         QContext.init(instance, new QSession());
         if(!vendor.equals("h2"))
         {
            try(InputStream schema = SampleStorageConstraintTest.class.getResourceAsStream("/database/field-lab-" + vendor + ".sql"))
            {
               assertNotNull(schema);
               sql(backend, new String(schema.readAllBytes(), StandardCharsets.UTF_8));
            }
         }
         try(Connection connection = ConnectionManager.getConnection(backend))
         {
            assertTrue(connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).startsWith(vendor));
         }
         assertCanonicalConstraints(backend, vendor, table);

         sql(backend, "ALTER TABLE field_lab ADD CONSTRAINT sample_decimal_unique UNIQUE(decimal_value)");
         table.withUniqueKey(new UniqueKey("decimalValue"));
         assertRoundedWrites(backend, vendor);
         assertRoundedBatch(backend, vendor);
         assertConfiguredCollation(backend, vendor);
         assertConcurrentWriters(instance, backend, vendor);

         sql(backend, "DELETE FROM field_lab");
         sql(backend, "ALTER TABLE field_lab DROP " + (vendor.equals("mysql") ? "INDEX " : "CONSTRAINT ") + "sample_decimal_unique");
         table.getUniqueKeys().removeIf(key -> key.getFieldNames().equals(List.of("decimalValue")));
         sql(backend, "ALTER TABLE field_lab ADD CONSTRAINT sample_composite_unique UNIQUE(long_value, decimal_value)");
         table.withUniqueKey(new UniqueKey("longValue", "decimalValue"));
         assertCompositeAndNullSemantics(backend, vendor);
      }
      finally
      {
         raceBarrier = null;
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Confirm actual DDL enforcement independently of the matching QQQ declarations.
    *******************************************************************************/
   private static void assertCanonicalConstraints(RDBMSBackendMetaData backend, String vendor, QTableMetaData table) throws Exception
   {
      assertEquals(List.of(List.of("name"), List.of("normalizedKey")), table.getUniqueKeys().stream().map(UniqueKey::getFieldNames).toList());
      assertEquals("normalized_key", table.getField("normalizedKey").getBackendName());
      sql(backend, "INSERT INTO field_lab(name, normalized_key) VALUES ('canonical-owner', 'CANONICAL')");
      List<List<String>> before = snapshot(backend);
      assertUniqueViolation(assertThrows(SQLException.class, () -> sql(backend,
         "INSERT INTO field_lab(name, normalized_key) VALUES ('canonical-owner', 'DIFFERENT')")), vendor);
      assertEquals(before, snapshot(backend));
      assertUniqueViolation(assertThrows(SQLException.class, () -> sql(backend,
         "INSERT INTO field_lab(name, normalized_key) VALUES ('different-name', 'CANONICAL')")), vendor);
      assertEquals(before, snapshot(backend));
      sql(backend, "DELETE FROM field_lab");
   }



   /*******************************************************************************
    ** Preflight compares2.00004; DECIMAL(20,4) stores2.0000. Native enforcement wins.
    *******************************************************************************/
   private static void assertRoundedWrites(RDBMSBackendMetaData backend, String vendor) throws Exception
   {
      insert(record("round-owner", "2.0000"));
      QRecord target = insert(record("round-target", "5.0000"));
      List<List<String>> before = snapshot(backend);
      assertUniqueViolation(assertThrows(QException.class, () -> update(new QRecord().withValue("id", target.getValueString("id"))
         .withValue("decimalValue", new BigDecimal("2.00004")))), vendor);
      assertEquals(before, snapshot(backend));
      assertUniqueViolation(assertThrows(QException.class, () -> insert(record("round-insert", "2.00004"))), vendor);
      assertEquals(before, snapshot(backend));
      assertEquals(1, count(backend, "decimal_value = 2.0000"));

      update(new QRecord().withValue("id", target.getValueString("id")).withValue("decimalValue", new BigDecimal("5.2500")));
      update(new QRecord().withValue("id", target.getValueString("id")).withValue("decimalValue", new BigDecimal("5.25000")));
      assertEquals(1, count(backend, "decimal_value = 5.2500"));
      update(new QRecord().withValue("id", target.getValue("id")).withValue("decimalValue", null));
      insert(record("null-one", null));
      insert(record("null-two", null));
      assertEquals(3, count(backend, "decimal_value IS NULL"));
   }



   /*******************************************************************************
    ** Fresh Java-distinct tuples can collide only when the native INSERT converts them.
    *******************************************************************************/
   private static void assertRoundedBatch(RDBMSBackendMetaData backend, String vendor) throws Exception
   {
      assertUniqueViolation(assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TABLE)
         .withInputSource(QInputSource.USER).withRecords(List.of(record("batch-one", "31.00001"), record("batch-two", "31.00002"))))), vendor);
      assertTrue(count(backend, "decimal_value = 31.0000") <= 1);
      assertNoStoredDuplicates(backend, "decimal_value");
   }



   /*******************************************************************************
    ** Test the configured native comparison; PostgreSQL's default stays case-sensitive.
    *******************************************************************************/
   private static void assertConfiguredCollation(RDBMSBackendMetaData backend, String vendor) throws Exception
   {
      if(vendor.equals("h2"))
      {
         sql(backend, "ALTER TABLE field_lab ALTER COLUMN name VARCHAR_IGNORECASE(80) NOT NULL");
      }
      else if(vendor.equals("mysql"))
      {
         sql(backend, "ALTER TABLE field_lab MODIFY name VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL");
      }
      sql(backend, "INSERT INTO field_lab(name) VALUES ('native-case-control')");
      assertEquals(vendor.equals("postgres") ? 0 : 1, count(backend, "name = 'NATIVE-CASE-CONTROL'"));
      sql(backend, "DELETE FROM field_lab WHERE name = 'native-case-control'");
      InsertInput batch = new InsertInput(TABLE).withInputSource(QInputSource.USER).withRecords(List.of(
         record("NativeBatch", "41.0000"), record("nativebatch", "42.0000")));
      if(vendor.equals("postgres"))
      {
         new InsertAction().execute(batch).getRecords().forEach(record -> assertTrue(record.getErrors().isEmpty(), record.getErrorsAsString()));
         assertEquals(1, count(backend, "name = 'NativeBatch'"));
         assertEquals(1, count(backend, "name = 'nativebatch'"));
      }
      else
      {
         assertUniqueViolation(assertThrows(QException.class, () -> new InsertAction().execute(batch)), vendor);
         assertTrue(count(backend, "name = 'NativeBatch'") <= 1);
      }
      assertNoStoredDuplicates(backend, "name");
   }



   /*******************************************************************************
    ** Both writers finish preflight before either INSERT, then one native key wins.
    *******************************************************************************/
   private static void assertConcurrentWriters(QInstance instance, RDBMSBackendMetaData backend, String vendor) throws Exception
   {
      assertEquals(0, count(backend, "decimal_value = 71.0000"));
      RaceBarrier barrier = new RaceBarrier(new CountDownLatch(2), new CountDownLatch(1));
      raceBarrier = barrier;
      QTableMetaData table = instance.getTable(TABLE);
      assertTrue(table.getCustomizer(TableCustomizers.PRE_INSERT_RECORD.getRole()).isEmpty());
      table.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(WaitAfterPreflight.class));
      ExecutorService workers = Executors.newFixedThreadPool(2);
      List<Future<Exception>> results = new ArrayList<>();
      try(Connection first = ConnectionManager.getConnection(backend); Connection second = ConnectionManager.getConnection(backend))
      {
         for(Connection connection : List.of(first, second))
         {
            try(Statement statement = connection.createStatement())
            {
               statement.execute(switch(vendor)
               {
                  case "mysql" -> "SET SESSION innodb_lock_wait_timeout = 5";
                  case "postgres" -> "SET lock_timeout = '5s'";
                  default -> "SET LOCK_TIMEOUT 5000";
               });
            }
         }
         results.add(workers.submit(() -> raceWriter(instance, first, "race-one")));
         results.add(workers.submit(() -> raceWriter(instance, second, "race-two")));
         try
         {
            assertTrue(barrier.ready().await(10, TimeUnit.SECONDS), "Both writers must complete preflight before release");
            barrier.release().countDown();
            int successes = 0;
            for(Future<Exception> result : results)
            {
               Exception failure = result.get(15, TimeUnit.SECONDS);
               if(failure == null)
               {
                  successes++;
               }
               else
               {
                  assertUniqueViolation(failure, vendor);
               }
            }
            assertEquals(1, successes);
            assertFalse(first.isClosed());
            assertFalse(second.isClosed());
         }
         finally
         {
            barrier.release().countDown();
            results.forEach(result -> result.cancel(true));
         }
      }
      finally
      {
         workers.shutdownNow();
         table.getCustomizers().remove(TableCustomizers.PRE_INSERT_RECORD.getRole());
         raceBarrier = null;
         assertTrue(workers.awaitTermination(15, TimeUnit.SECONDS), "Owned native writers must terminate");
      }
      assertEquals(1, count(backend, "decimal_value = 71.0000"));
      assertNoStoredDuplicates(backend, "decimal_value");
   }



   /*******************************************************************************
    ** Worker owns its QContext and transaction, while the fixture owns the connection.
    *******************************************************************************/
   private static Exception raceWriter(QInstance instance, Connection connection, String name)
   {
      QContext.init(instance, new QSession());
      try
      {
         RDBMSTransaction transaction = new RDBMSTransaction(connection);
         try
         {
            QRecord result = new InsertAction().execute(new InsertInput(TABLE).withInputSource(QInputSource.USER)
               .withTransaction(transaction).withRecord(record(name, "71.0000"))).getRecords().get(0);
            assertTrue(result.getErrors().isEmpty(), result.getErrorsAsString());
            transaction.commit();
            return null;
         }
         catch(Exception e)
         {
            transaction.rollback();
            return e;
         }
      }
      catch(Exception e)
      {
         return e;
      }
      finally
      {
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Independent SQL readback includes every stored column, including nullable fields.
    *******************************************************************************/
   private static List<List<String>> snapshot(RDBMSBackendMetaData backend) throws SQLException
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery("SELECT * FROM field_lab ORDER BY id"))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.add(result.getString(column));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void assertCompositeAndNullSemantics(RDBMSBackendMetaData backend, String vendor) throws Exception
   {
      insert(record("composite-owner", "2.0000").withValue("longValue", 1L));
      QRecord target = insert(record("composite-target", "5.0000").withValue("longValue", 1L));
      insert(record("different-prefix", "2.0000").withValue("longValue", 2L));
      List<List<String>> before = snapshot(backend);
      assertUniqueViolation(assertThrows(QException.class, () -> update(new QRecord().withValue("id", target.getValueString("id"))
         .withValue("decimalValue", new BigDecimal("2.00004")))), vendor);
      assertEquals(before, snapshot(backend));
      assertUniqueViolation(assertThrows(QException.class, () -> insert(record("composite-insert", "2.00004").withValue("longValue", 1L))), vendor);
      assertEquals(before, snapshot(backend));
      update(new QRecord().withValue("id", target.getValueString("id")).withValue("longValue", 1L).withValue("decimalValue", new BigDecimal("5.00000")));
      insert(record("null-prefix-one", "3.0000"));
      insert(record("null-prefix-two", "3.0000"));
      insert(record("null-decimal-one", null).withValue("longValue", 9L));
      insert(record("null-decimal-two", null).withValue("longValue", 9L));
      assertEquals(2, count(backend, "long_value IS NULL AND decimal_value = 3.0000"));
      assertEquals(2, count(backend, "long_value = 9 AND decimal_value IS NULL"));
      assertEquals(1, count(backend, "long_value = 1 AND decimal_value = 2.0000"));
      assertEquals(1, count(backend, "long_value = 2 AND decimal_value = 2.0000"));
   }



   /*******************************************************************************
    ** SQLState class23 alone also includes non-unique constraint errors.
    *******************************************************************************/
   private static void assertUniqueViolation(Throwable failure, String vendor)
   {
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         if(cause instanceof SQLException sqlException)
         {
            for(SQLException sql = sqlException; sql != null; sql = sql.getNextException())
            {
               if(vendor.equals("mysql") ? "23000".equals(sql.getSQLState()) && sql.getErrorCode() == 1062 : "23505".equals(sql.getSQLState()))
               {
                  return;
               }
            }
         }
      }
      fail("Expected native duplicate-key identity for " + vendor, failure);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void assertNoStoredDuplicates(RDBMSBackendMetaData backend, String column) throws SQLException
   {
      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement();
          ResultSet duplicates = statement.executeQuery("SELECT " + column + " FROM field_lab WHERE " + column + " IS NOT NULL GROUP BY " + column + " HAVING COUNT(*) > 1"))
      {
         assertFalse(duplicates.next(), "Native key must have no stored duplicates");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static long count(RDBMSBackendMetaData backend, String predicate) throws SQLException
   {
      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM field_lab WHERE " + predicate))
      {
         assertTrue(result.next());
         return result.getLong(1);
      }
   }



   /*******************************************************************************
    ** Only fixed fixture SQL reaches this helper.
    *******************************************************************************/
   private static void sql(RDBMSBackendMetaData backend, String sql) throws SQLException
   {
      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement())
      {
         statement.execute(sql);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord record(String name, String decimal)
   {
      return new QRecord().withValue("name", name).withValue("decimalValue", decimal == null ? null : new BigDecimal(decimal));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord insert(QRecord record) throws QException
   {
      QRecord result = new InsertAction().execute(new InsertInput(TABLE).withInputSource(QInputSource.USER).withRecord(record)).getRecords().get(0);
      assertTrue(result.getErrors().isEmpty(), result.getErrorsAsString());
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void update(QRecord record) throws QException
   {
      QRecord result = new UpdateAction().execute(new UpdateInput(TABLE).withInputSource(QInputSource.USER).withRecord(record)).getRecords().get(0);
      assertTrue(result.getErrors().isEmpty(), result.getErrorsAsString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record RaceBarrier(CountDownLatch ready, CountDownLatch release)
   {
   }



   /*******************************************************************************
    ** Synchronization only: existing phase order and candidate values remain untouched.
    *******************************************************************************/
   public static class WaitAfterPreflight extends AbstractPreInsertCustomizer
   {
      @Override
      public WhenToRun getWhenToRun()
      {
         return WhenToRun.AFTER_ALL_VALIDATIONS;
      }



      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         records.forEach(record -> assertTrue(record.getErrors().isEmpty(), record.getErrorsAsString()));
         RaceBarrier barrier = raceBarrier;
         assertNotNull(barrier);
         barrier.ready().countDown();
         try
         {
            if(!barrier.release().await(10, TimeUnit.SECONDS))
            {
               throw new QException("Timed out waiting for both native-key writers");
            }
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new QException("Interrupted native-key writer", e);
         }
         return records;
      }
   }
}
