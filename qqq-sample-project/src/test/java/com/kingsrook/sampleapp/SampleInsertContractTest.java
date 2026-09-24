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


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.DuplicateKeyBadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Insert examples reuse the canonical schema and independently stored SQL.
 *******************************************************************************/
class SampleInsertContractTest
{
   private QInstance instance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSingleAndListOverloadsReturnStoredKeysAndDefaults() throws Exception
   {
      QRecord single = new InsertAction().executeForRecord(new InsertInput("fieldLab").withRecord(new QRecord().withValue("name", "Single")));
      List<QRecord> batch = InsertAction.executeForRecords(new InsertInput("fieldLab").withRecords(List.of(
         new QRecord().withValue("name", "Default null").withValue("booleanValue", null),
         new QRecord().withValue("name", "Explicit false").withValue("booleanValue", false))));
      assertEquals(2, batch.size());
      for(QRecord record : List.of(single, batch.get(0), batch.get(1)))
      {
         assertTrue(record.getErrors().isEmpty(), record.getErrorsAsString());
         assertEquals("fieldLab", record.getTableName());
         assertNotNull(record.getValueInteger("id"));
         assertTrue(record.getValueInteger("id") > 0);
      }
      assertEquals(3, List.of(single, batch.get(0), batch.get(1)).stream().map(record -> record.getValueInteger("id")).distinct().count());
      assertEquals(true, single.getValueBoolean("booleanValue"));
      assertEquals(true, batch.get(0).getValueBoolean("booleanValue"));
      assertEquals(false, batch.get(1).getValueBoolean("booleanValue"));
      assertEquals(List.of(List.of(single.getValueString("id"), "Single", "TRUE"),
         List.of(batch.get(0).getValueString("id"), "Default null", "TRUE"),
         List.of(batch.get(1).getValueString("id"), "Explicit false", "FALSE")), rows("SELECT id,name,boolean_value FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    ** Optional generated identity inputs use the same effective null semantics.
    *******************************************************************************/
   @Test
   void testGeneratedKeysTreatEmptyTextAsAbsentAndPreserveZero() throws Exception
   {
      List<QRecord> output = new InsertAction().execute(new InsertInput("fieldLab").withRecords(List.of(
         new QRecord().withValue("name", "Absent key"),
         new QRecord().withValue("name", "Blank key").withValue("id", ""),
         new QRecord().withValue("name", "Null key").withValue("id", null),
         new QRecord().withValue("name", "Zero key").withValue("id", 0)))).getRecords();
      assertEquals(4, output.size());
      List<List<String>> stored = rows("SELECT id,name FROM field_lab ORDER BY name");
      assertEquals(4, stored.size());
      for(QRecord record : output)
      {
         assertTrue(record.getErrors().isEmpty(), record.getErrorsAsString());
         assertNotNull(record.getValueInteger("id"), () -> "Missing returned key for " + record.getValueString("name") + "; stored rows: " + stored);
      }
      assertEquals(0, output.get(3).getValueInteger("id"));
      assertEquals(4, output.stream().map(record -> record.getValueInteger("id")).distinct().count());
      assertEquals(output.stream().map(record -> List.of(record.getValueString("id"), record.getValueString("name"))).toList(),
         stored);
   }



   /*******************************************************************************
    ** Every input retains its position even when only some records are inserted.
    *******************************************************************************/
   @Test
   void testMixedBatchPreservesResultsErrorsWarningsAndStoredOwners() throws Exception
   {
      QRecord owner = new InsertAction().executeForRecord(new InsertInput("fieldLab").withRecord(new QRecord().withValue("name", "Existing")));
      List<List<String>> existing = rows("SELECT * FROM field_lab ORDER BY id");
      List<QRecord> input = List.of(
         new QRecord().withValue("name", "First").withValue("truncateValue", "123456789").withWarning(new QWarningMessage("Reviewed import warning")),
         new QRecord().withValue("name", " \t "),
         new QRecord().withValue("name", "Existing"),
         new QRecord().withValue("name", "Shared"),
         new QRecord().withValue("name", "Shared"),
         new QRecord().withValue("name", "Pre-rejected").withError(new BadInputStatusMessage("Rejected before insert")),
         new QRecord().withValue("name", "Last"));
      List<QRecord> output = new InsertAction().execute(new InsertInput("fieldLab").withRecords(input)).getRecords();
      assertEquals(input.size(), output.size());
      assertEquals(input.stream().map(record -> record.getValueString("name")).toList(), output.stream().map(record -> record.getValueString("name")).toList());
      for(Integer index : List.of(0, 3, 6))
      {
         assertTrue(output.get(index).getErrors().isEmpty(), output.get(index).getErrorsAsString());
         assertNotNull(output.get(index).getValueInteger("id"));
      }
      for(Integer index : List.of(1, 2, 4, 5))
      {
         assertFalse(output.get(index).getErrors().isEmpty());
         assertNull(output.get(index).getValue("id"));
      }
      assertTrue(output.get(1).getErrorsAsString().contains("required"));
      assertTrue(output.get(2).getErrorsAsString().contains("already exists"));
      assertTrue(output.get(4).getErrors().stream().anyMatch(error -> error instanceof DuplicateKeyBadInputStatusMessage));
      assertTrue(output.get(5).getErrorsAsString().contains("Rejected before insert"));
      assertEquals("12345678", output.get(0).getValueString("truncateValue"));
      assertEquals(List.of("Reviewed import warning"), output.get(0).getWarnings().stream().map(warning -> warning.getMessage()).toList());
      assertEquals(List.of(List.of(owner.getValueString("id"), "Existing"), List.of(output.get(0).getValueString("id"), "First"),
         List.of(output.get(3).getValueString("id"), "Shared"), List.of(output.get(6).getValueString("id"), "Last")), rows("SELECT id,name FROM field_lab ORDER BY id"));
      assertEquals(existing, rows("SELECT * FROM field_lab WHERE name='Existing'"));
   }



   /*******************************************************************************
    ** Required typed fields cannot become null only after reaching the backend.
    *******************************************************************************/
   @Test
   void testRequiredNumericBlanksRejectWhileZeroRemainsValid() throws Exception
   {
      instance.getTable("fieldLab").getField("longValue").setIsRequired(true);
      List<QRecord> output = new InsertAction().execute(new InsertInput("fieldLab").withRecords(List.of(
         new QRecord().withValue("name", "Missing"),
         new QRecord().withValue("name", "Null").withValue("longValue", null),
         new QRecord().withValue("name", "Empty").withValue("longValue", ""),
         new QRecord().withValue("name", "Spaces").withValue("longValue", " \t "),
         new QRecord().withValue("name", "Numeric zero").withValue("longValue", 0L),
         new QRecord().withValue("name", "String zero").withValue("longValue", "0")))).getRecords();
      assertEquals(6, output.size());
      assertEquals(List.of(List.of("Numeric zero", "0"), List.of("String zero", "0")), rows("SELECT name,long_value FROM field_lab ORDER BY id"));
      for(Integer index : List.of(0, 1, 2, 3))
      {
         assertTrue(output.get(index).getErrorsAsString().contains("Missing value in required field"));
         assertNull(output.get(index).getValue("id"));
      }
      assertTrue(output.get(4).getErrors().isEmpty());
      assertTrue(output.get(5).getErrors().isEmpty());
   }



   /*******************************************************************************
    ** Native conversion failure rejects this statement before any row is written.
    *******************************************************************************/
   @Test
   void testInvalidNumericInputCannotWriteSingleOrSameStatementBatch() throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
      assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("fieldLab")
         .withRecord(new QRecord().withValue("name", "Invalid").withValue("longValue", "not-a-number"))));
      assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
      assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("fieldLab").withRecords(List.of(
         new QRecord().withValue("name", "Valid same statement").withValue("longValue", 4L),
         new QRecord().withValue("name", "Invalid same statement").withValue("longValue", "not-a-number")))));
      assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    ** Integral fields must reject fractional text instead of truncating it.
    *******************************************************************************/
   @Test
   void testFractionalIntegralStringsRejectBeforeStorage() throws Exception
   {
      List<List<String>> beforePeople = rows("SELECT * FROM person ORDER BY id");
      for(String value : List.of("1.5", "-1.5", "1,000.5", "1.00000000000000000001", "1.0.0"))
      {
         assertAll(
            () -> assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("fieldLab")
               .withRecord(new QRecord().withValue("name", "Fractional long").withValue("longValue", value)))),
            () -> assertEquals(List.of(), rows("SELECT name,long_value FROM field_lab")));
         assertAll(
            () -> assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("person")
               .withRecord(new QRecord().withValue("firstName", "Fractional").withValue("lastName", "Sample")
                  .withValue("email", "fractional@example.invalid").withValue("daysWorked", value)))),
            () -> assertEquals(beforePeople, rows("SELECT * FROM person ORDER BY id")));
      }
      QRecord exact = new InsertAction().executeForRecord(new InsertInput("fieldLab")
         .withRecord(new QRecord().withValue("name", "Exact long").withValue("longValue", "1,000.00")));
      assertTrue(exact.getErrors().isEmpty());
      assertEquals(List.of(List.of("Exact long", "1000")), rows("SELECT name,long_value FROM field_lab"));
   }



   /*******************************************************************************
    ** Required does not mean truthy, positive, or nonempty binary content.
    *******************************************************************************/
   @Test
   void testRequiredNonStringValuesPreserveFalseZeroAndEmptyBinary() throws Exception
   {
      instance.getTable("fieldLab").getField("longValue").setIsRequired(true);
      instance.getTable("fieldLab").getField("booleanValue").setIsRequired(true);
      instance.getTable("fieldLab").getField("booleanValue").setDefaultValue(null);
      instance.getTable("fieldLab").getField("blobValue").setIsRequired(true);
      List<QRecord> output = new InsertAction().execute(new InsertInput("fieldLab").withRecords(List.of(
         new QRecord().withValue("name", "Present values").withValue("longValue", 0L).withValue("booleanValue", false).withValue("blobValue", new byte[0]),
         new QRecord().withValue("name", "Missing boolean").withValue("longValue", 0L).withValue("blobValue", new byte[0]),
         new QRecord().withValue("name", "Missing blob").withValue("longValue", 0L).withValue("booleanValue", false)))).getRecords();
      assertEquals(3, output.size());
      assertTrue(output.get(0).getErrors().isEmpty(), output.get(0).getErrorsAsString());
      assertArrayEquals(new byte[0], (byte[]) output.get(0).getValue("blobValue"));
      for(Integer index : List.of(1, 2))
      {
         assertTrue(output.get(index).getErrorsAsString().contains("required"));
         assertNull(output.get(index).getValue("id"));
      }
      assertEquals(List.of(List.of("Present values", "0", "FALSE", "0")), rows("SELECT name,long_value,boolean_value,OCTET_LENGTH(blob_value) FROM field_lab"));
   }



   /*******************************************************************************
    ** The source flag changes metadata validation, not native database constraints.
    *******************************************************************************/
   @Test
   void testInputSourceRequiredOverrideRetainsNativeConstraints() throws Exception
   {
      instance.getTable("fieldLab").getField("textValue").setIsRequired(true);
      for(QInputSource source : List.of(QInputSource.SYSTEM, QInputSource.USER))
      {
         QRecord denied = new InsertAction().executeForRecord(new InsertInput("fieldLab").withInputSource(source)
            .withRecord(new QRecord().withValue("name", source.toString())));
         assertTrue(denied.getErrorsAsString().contains("required"));
      }
      assertTrue(rows("SELECT * FROM field_lab").isEmpty());
      List<QRecord> sameKey = new InsertAction().execute(new InsertInput("fieldLab").withRecords(List.of(
         new QRecord().withValue("name", "Rejected then valid"),
         new QRecord().withValue("name", "Rejected then valid").withValue("textValue", "Required value")))).getRecords();
      assertEquals(2, sameKey.size());
      assertTrue(sameKey.get(0).getErrorsAsString().contains("required"));
      assertNull(sameKey.get(0).getValue("id"));
      assertTrue(sameKey.get(1).getErrors().isEmpty(), sameKey.get(1).getErrorsAsString());
      assertEquals(List.of(List.of("Rejected then valid", "Required value")), rows("SELECT name,text_value FROM field_lab"));
      QRecord allowed = new InsertAction().executeForRecord(new InsertInput("fieldLab").withInputSource(() -> false)
         .withRecord(new QRecord().withValue("name", "Custom source")));
      assertTrue(allowed.getErrors().isEmpty());
      assertEquals(List.of(List.of("Custom source")), rows("SELECT name FROM field_lab WHERE text_value IS NULL"));
      List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
      assertSqlState("23502", assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("fieldLab")
         .withInputSource(() -> false).withRecord(new QRecord()))));
      assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRowSecurityKeepsAllowedAndDeniedBatchOutcomesSeparate() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("insertName"));
      instance.getTable("fieldLab").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("insertName").withFieldName("name"));
      QContext.setQSession(new QSession().withSecurityKeyValue("insertName", "Allowed"));
      List<QRecord> output = new InsertAction().execute(new InsertInput("fieldLab").withInputSource(QInputSource.USER).withRecords(List.of(
         new QRecord().withValue("name", "Allowed"), new QRecord().withValue("name", "Denied")))).getRecords();
      assertEquals(2, output.size());
      assertTrue(output.get(0).getErrors().isEmpty(), output.get(0).getErrorsAsString());
      assertFalse(output.get(1).getErrors().isEmpty());
      assertNull(output.get(1).getValue("id"));
      assertEquals(List.of(List.of(output.get(0).getValueString("id"), "Allowed")), rows("SELECT id,name FROM field_lab"));
   }



   /*******************************************************************************
    ** Preflight sees uncommitted keys and leaves commit/rollback with the caller.
    *******************************************************************************/
   @Test
   void testCallerTransactionVisibilityUniquenessRollbackAndCommit() throws Exception
   {
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend())))
      {
         QRecord first = new InsertAction().executeForRecord(new InsertInput("fieldLab").withTransaction(transaction)
            .withRecord(new QRecord().withValue("name", "Uncommitted")));
         assertTrue(first.getErrors().isEmpty());
         assertEquals(List.of(List.of("Uncommitted")), rows(transaction.getConnection(), "SELECT name FROM field_lab"));
         assertTrue(rows("SELECT * FROM field_lab").isEmpty());
         QRecord duplicate = new InsertAction().executeForRecord(new InsertInput("fieldLab").withTransaction(transaction)
            .withRecord(new QRecord().withValue("name", "Uncommitted")));
         assertTrue(duplicate.getErrorsAsString().contains("already exists"));
         assertFalse(transaction.getConnection().isClosed());
         assertEquals(List.of(List.of("Uncommitted")), rows(transaction.getConnection(), "SELECT name FROM field_lab"));
         transaction.rollback();
      }
      assertTrue(rows("SELECT * FROM field_lab").isEmpty());
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend())))
      {
         QRecord committed = new InsertAction().executeForRecord(new InsertInput("fieldLab").withTransaction(transaction)
            .withRecord(new QRecord().withValue("name", "Committed")));
         assertTrue(committed.getErrors().isEmpty());
         assertTrue(rows("SELECT * FROM field_lab").isEmpty());
         assertFalse(transaction.getConnection().isClosed());
         transaction.commit();
      }
      assertEquals(List.of(List.of("Committed")), rows("SELECT name FROM field_lab"));
   }



   /*******************************************************************************
    ** A failed native statement does not close or roll back earlier caller work.
    *******************************************************************************/
   @Test
   void testNativeConstraintFailureLeavesCallerRollbackAvailable() throws Exception
   {
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend())))
      {
         QRecord first = new InsertAction().executeForRecord(new InsertInput("fieldLab").withTransaction(transaction)
            .withRecord(new QRecord().withValue("name", "Caller owned")));
         assertTrue(first.getErrors().isEmpty());
         assertSqlState("23505", assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("fieldLab")
            .withTransaction(transaction).withSkipUniqueKeyCheck(true).withRecord(new QRecord().withValue("name", "Caller owned")))));
         assertFalse(transaction.getConnection().isClosed());
         assertEquals(List.of(List.of("Caller owned")), rows(transaction.getConnection(), "SELECT name FROM field_lab"));
         assertTrue(rows("SELECT * FROM field_lab").isEmpty());
         transaction.rollback();
      }
      assertTrue(rows("SELECT * FROM field_lab").isEmpty());
      assertSqlState("23505", assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("fieldLab")
         .withSkipUniqueKeyCheck(true).withRecords(List.of(new QRecord().withValue("name", "Native batch duplicate"),
            new QRecord().withValue("name", "Native batch duplicate"))))));
      assertTrue(rows("SELECT * FROM field_lab").isEmpty());
   }



   /*******************************************************************************
    ** Empty input is a no-op only after normal table/session validation succeeds.
    *******************************************************************************/
   @Test
   void testEmptyInputsAndMissingContextHaveNoMutation() throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM person ORDER BY id");
      assertTrue(new InsertAction().execute(new InsertInput("person")).getRecords().isEmpty());
      assertTrue(new InsertAction().execute(new InsertInput("person").withRecords(List.of())).getRecords().isEmpty());
      assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput()));
      assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("missing")));
      InsertInput input = new InsertInput("person").withRecord(new QRecord().withValue("firstName", "Never inserted")
         .withValue("lastName", "Sample").withValue("email", "missing-context@example.invalid"));
      try
      {
         QContext.setQSession(null);
         assertEquals("QSession was not set in QContext.", assertThrows(QException.class, () -> new InsertAction().execute(input)).getMessage());
         QContext.clear();
         assertEquals("QInstance was not set in QContext.", assertThrows(QException.class, () -> new InsertAction().execute(input)).getMessage());
      }
      finally
      {
         QContext.init(instance, new QSession());
      }
      assertEquals(before, rows("SELECT * FROM person ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void assertSqlState(String expected, QException exception)
   {
      Throwable cause = exception;
      while(cause != null && !(cause instanceof SQLException))
      {
         cause = cause.getCause();
      }
      assertEquals(expected, assertInstanceOf(SQLException.class, cause).getSQLState());
   }



   /*******************************************************************************
    ** Only fixed canonical fixture SQL reaches these helpers.
    *******************************************************************************/
   private static List<List<String>> rows(String sql) throws SQLException
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()))
      {
         return rows(connection, sql);
      }
   }



   /*******************************************************************************
    ** A caller-owned connection is never closed by the assertion helper.
    *******************************************************************************/
   private static List<List<String>> rows(Connection connection, String sql) throws SQLException
   {
      List<List<String>> rows = new ArrayList<>();
      try(Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
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
}
