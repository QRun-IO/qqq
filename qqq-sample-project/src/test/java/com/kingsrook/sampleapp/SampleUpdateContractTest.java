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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.DuplicateKeyBadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Common Update invocation and patch contracts use canonical FieldLab/Person.
 ** Native H2 assertions qualify transaction and statement-failure behavior; they
 ** do not promise automatic batch rollback or other providers' recovery rules.
 *******************************************************************************/
class SampleUpdateContractTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;



   /*******************************************************************************
    ** Native seeding isolates Update from Insert behavior already tested elsewhere.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement())
      {
         assertEquals(4, statement.executeUpdate("INSERT INTO field_lab (id,name,long_value,decimal_value,boolean_value,text_value) VALUES "
            + "(1,'Target 1',10,1.0000,TRUE,'Original 1'),(2,'Target 2',20,2.0000,TRUE,'Original 2'),"
            + "(3,'Target 3',30,3.0000,TRUE,'Original 3'),(4,'Target 4',40,4.0000,TRUE,'Original 4')"));
      }
   }



   /*******************************************************************************
    ** Each fixture has its own context and reset native connection providers.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** Both public execution forms return sparse results in input order even when
    ** the backend groups distinct updated field sets into different statements.
    *******************************************************************************/
   @Test
   void testSingleAndListUpdatesPreserveOmittedFieldsAndOrder() throws Exception
   {
      List<Map<String, String>> expected = snapshot();
      QRecord singlePatch = new QRecord().withValue("id", "1").withValue("textValue", "Single update");
      UpdateInput singleInput = input(singlePatch);
      assertEquals(QInputSource.SYSTEM, singleInput.getInputSource());
      List<QRecord> single = new UpdateAction().execute(singleInput).getRecords();
      expected.get(0).put("TEXT_VALUE", "Single update");
      assertEquals(1, single.size());
      assertAll(
         () -> assertTrue(single.get(0).getErrors().isEmpty(), single.get(0).getErrorsAsString()),
         () -> assertEquals(1, single.get(0).getValueInteger("id")),
         () -> assertEquals("Single update", single.get(0).getValueString("textValue")),
         () -> assertFalse(single.get(0).getValues().containsKey("name")),
         () -> assertFalse(singlePatch.getValues().containsKey("longValue")),
         () -> assertEquals(expected, snapshot()));

      List<QRecord> batch = UpdateAction.executeForRecords(input(
         new QRecord().withValue("id", 2).withValue("longValue", 44L),
         new QRecord().withValue("id", 1).withValue("textValue", "Batch update")));
      expected.get(0).put("TEXT_VALUE", "Batch update");
      expected.get(1).put("LONG_VALUE", "44");
      assertAll(
         () -> assertEquals(List.of(2, 1), batch.stream().map(record -> record.getValueInteger("id")).toList()),
         () -> assertTrue(batch.stream().allMatch(record -> record.getErrors().isEmpty())),
         () -> assertEquals(44L, batch.get(0).getValueLong("longValue")),
         () -> assertEquals("Batch update", batch.get(1).getValueString("textValue")),
         () -> assertFalse(batch.get(0).getValues().containsKey("textValue")),
         () -> assertEquals(expected, snapshot()));
   }



   /*******************************************************************************
    ** Explicit null clears nullable storage; empty text is a separate value.
    *******************************************************************************/
   @Test
   void testExplicitNullAndEmptyTextDifferFromOmission() throws Exception
   {
      List<Map<String, String>> expected = snapshot();
      List<QRecord> records = UpdateAction.executeForRecords(input(
         new QRecord().withValue("id", 1).withValue("textValue", null).withValue("decimalValue", null),
         new QRecord().withValue("id", 2).withValue("textValue", "")));
      expected.get(0).put("TEXT_VALUE", null);
      expected.get(0).put("DECIMAL_VALUE", null);
      expected.get(1).put("TEXT_VALUE", "");
      assertAll(
         () -> assertEquals(2, records.size()),
         () -> assertTrue(records.stream().allMatch(record -> record.getErrors().isEmpty())),
         () -> assertTrue(records.get(0).getValues().containsKey("textValue")),
         () -> assertNull(records.get(0).getValue("textValue")),
         () -> assertTrue(records.get(0).getValues().containsKey("decimalValue")),
         () -> assertNull(records.get(0).getValue("decimalValue")),
         () -> assertEquals("", records.get(1).getValueString("textValue")),
         () -> assertFalse(records.get(1).getValues().containsKey("decimalValue")),
         () -> assertEquals(expected, snapshot()));
   }



   /*******************************************************************************
    ** Validation failures remain per record and do not reorder permitted writes.
    ** This is not a native failing-statement or automatic rollback assertion.
    *******************************************************************************/
   @Test
   void testMixedBatchPreservesErrorsWarningsAndSuccessfulRows() throws Exception
   {
      List<Map<String, String>> expected = snapshot();
      List<QRecord> patches = List.of(
         new QRecord().withValue("id", 1).withValue("textValue", "First permitted").withWarning(new QWarningMessage("Reviewed warning")),
         new QRecord().withValue("textValue", "Missing key"),
         new QRecord().withValue("id", 999).withValue("textValue", "Not found"),
         new QRecord().withValue("id", 2).withValue("name", " \t ").withValue("textValue", "Required failure"),
         new QRecord().withValue("id", 3).withValue("textValue", "Pre-rejected").withError(new BadInputStatusMessage("Rejected before update")),
         new QRecord().withValue("id", 4).withValue("textValue", "Last permitted"));
      List<QRecord> results = new UpdateAction().execute(input().withRecords(patches)).getRecords();
      assertEquals(patches.size(), results.size());
      assertEquals(Arrays.asList("1", null, "999", "2", "3", "4"), results.stream().map(record -> record.getValueString("id")).toList());
      for(int index : List.of(0, 5))
      {
         assertTrue(results.get(index).getErrors().isEmpty(), results.get(index).getErrorsAsString());
      }
      assertEquals(List.of("Reviewed warning"), results.get(0).getWarnings().stream().map(warning -> warning.getMessage()).toList());
      assertTrue(results.get(1).getErrors().stream().anyMatch(error -> error instanceof BadInputStatusMessage));
      assertTrue(results.get(1).getErrorsAsString().contains("Missing value in primary key field"));
      assertTrue(results.get(2).getErrors().stream().anyMatch(error -> error instanceof NotFoundStatusMessage));
      assertTrue(results.get(3).getErrors().stream().anyMatch(error -> error instanceof BadInputStatusMessage));
      assertTrue(results.get(3).getErrorsAsString().contains("required"));
      assertTrue(results.get(4).getErrorsAsString().contains("Rejected before update"));
      expected.get(0).put("TEXT_VALUE", "First permitted");
      expected.get(3).put("TEXT_VALUE", "Last permitted");
      assertEquals(expected, snapshot());
   }



   /*******************************************************************************
    ** Correct caller hints preserve results. A forced true hint with unequal
    ** values is expressly outside the UpdateInput optimization contract.
    *******************************************************************************/
   @Test
   void testMatchingValuesOptimizationPreservesRequestedRows() throws Exception
   {
      List<Map<String, String>> expected = snapshot();
      long nextValue = 90;
      for(Boolean hint : Arrays.asList(null, true, false))
      {
         long value = nextValue++;
         List<QRecord> results = UpdateAction.executeForRecords(input(
            new QRecord().withValue("id", 2).withValue("longValue", value),
            new QRecord().withValue("id", 1).withValue("longValue", value)).withAreAllValuesBeingUpdatedTheSame(hint));
         expected.get(0).put("LONG_VALUE", Long.toString(value));
         expected.get(1).put("LONG_VALUE", Long.toString(value));
         assertEquals(List.of(2, 1), results.stream().map(record -> record.getValueInteger("id")).toList());
         assertTrue(results.stream().allMatch(record -> record.getErrors().isEmpty()));
         assertEquals(expected, snapshot());
      }
      for(Boolean hint : Arrays.asList(null, false))
      {
         long first = nextValue++;
         long second = nextValue++;
         List<QRecord> results = UpdateAction.executeForRecords(input(
            new QRecord().withValue("id", 1).withValue("longValue", first),
            new QRecord().withValue("id", 2).withValue("longValue", second)).withAreAllValuesBeingUpdatedTheSame(hint));
         expected.get(0).put("LONG_VALUE", Long.toString(first));
         expected.get(1).put("LONG_VALUE", Long.toString(second));
         assertTrue(results.stream().allMatch(record -> record.getErrors().isEmpty()));
         assertEquals(expected, snapshot());
      }
   }



   /*******************************************************************************
    ** Required validation applies to USER and SYSTEM; an explicit application
    ** source override does not override the database's native NOT NULL rule.
    *******************************************************************************/
   @Test
   void testInputSourceRequiredOverrideRetainsNativeConstraint() throws Exception
   {
      instance.getTable(TABLE).getField("textValue").setIsRequired(true);
      List<Map<String, String>> expected = snapshot();
      for(QInputSource source : QInputSource.values())
      {
         QRecord rejected = UpdateAction.executeForRecords(input(new QRecord().withValue("id", 1).withValue("textValue", null))
            .withInputSource(source)).get(0);
         assertTrue(rejected.getErrors().stream().anyMatch(error -> error instanceof BadInputStatusMessage));
         assertEquals(expected, snapshot());
      }
      QRecord allowed = UpdateAction.executeForRecords(input(new QRecord().withValue("id", 1).withValue("textValue", null))
         .withInputSource(() -> false)).get(0);
      expected.get(0).put("TEXT_VALUE", null);
      assertTrue(allowed.getErrors().isEmpty(), allowed.getErrorsAsString());
      assertEquals(expected, snapshot());
      QException failure = assertThrows(QException.class, () -> UpdateAction.executeForRecords(
         input(new QRecord().withValue("id", 1).withValue("name", null)).withInputSource(() -> false)));
      assertSqlState("23502", failure);
      assertEquals(expected, snapshot());
   }



   /*******************************************************************************
    ** Fields removed from USER metadata are ignored by native Update. Another
    ** allowed field still changes; SYSTEM retains the unrestricted schema.
    *******************************************************************************/
   @Test
   void testUserRemovedFieldCannotChangeStorage() throws Exception
   {
      QTableMetaData canonical = instance.getTable(TABLE);
      List<String> originalFields = new ArrayList<>(canonical.getFields().keySet());
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(ProtectedUpdateField.class));
      List<Map<String, String>> expected = snapshot();
      UpdateInput userInput = input(new QRecord().withValue("id", 1).withValue("name", "Allowed USER name")
         .withValue("textValue", "Forbidden USER replacement")).withInputSource(QInputSource.USER);
      QRecord userResult = UpdateAction.executeForRecords(userInput).get(0);
      expected.get(0).put("NAME", "Allowed USER name");
      assertAll(
         () -> assertTrue(userResult.getErrors().isEmpty(), userResult.getErrorsAsString()),
         () -> assertEquals("Allowed USER name", userResult.getValueString("name")),
         () -> assertFalse(userInput.getTable().getFields().containsKey("textValue")),
         () -> assertEquals(expected, snapshot()),
         () -> assertSame(canonical, instance.getTable(TABLE)),
         () -> assertEquals(originalFields, new ArrayList<>(canonical.getFields().keySet())));

      UpdateInput systemInput = input(new QRecord().withValue("id", 1).withValue("name", "Allowed SYSTEM name")
         .withValue("textValue", "Allowed SYSTEM replacement")).withInputSource(QInputSource.SYSTEM);
      QRecord systemResult = UpdateAction.executeForRecords(systemInput).get(0);
      expected.get(0).put("NAME", "Allowed SYSTEM name");
      expected.get(0).put("TEXT_VALUE", "Allowed SYSTEM replacement");
      assertAll(
         () -> assertTrue(systemResult.getErrors().isEmpty(), systemResult.getErrorsAsString()),
         () -> assertTrue(systemInput.getTable().getFields().containsKey("textValue")),
         () -> assertEquals(expected, snapshot()),
         () -> assertSame(canonical, instance.getTable(TABLE)),
         () -> assertEquals(originalFields, new ArrayList<>(canonical.getFields().keySet())));
   }



   /*******************************************************************************
    ** Native value conversion occurs before the matching-field statement runs.
    ** No claim is made that separate field groups are automatically atomic.
    *******************************************************************************/
   @Test
   void testInvalidNativeValuesDoNotWriteTheirStatement() throws Exception
   {
      List<Map<String, String>> before = snapshot();
      for(String invalid : List.of("not-a-number", "1.5"))
      {
         assertThrows(QException.class, () -> UpdateAction.executeForRecords(input(new QRecord().withValue("id", 1).withValue("longValue", invalid))));
         assertEquals(before, snapshot());
         assertThrows(QException.class, () -> UpdateAction.executeForRecords(input(
            new QRecord().withValue("id", 1).withValue("longValue", 55L),
            new QRecord().withValue("id", 2).withValue("longValue", invalid))));
         assertEquals(before, snapshot());
      }
      QRecord recovered = UpdateAction.executeForRecords(input(new QRecord().withValue("id", 1).withValue("longValue", "55.0"))).get(0);
      before.get(0).put("LONG_VALUE", "55");
      assertTrue(recovered.getErrors().isEmpty(), recovered.getErrorsAsString());
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** A native failure preserves prior caller work in H2 until explicit rollback.
    *******************************************************************************/
   @Test
   void testNativeFailureLeavesCallerTransactionOwnership() throws Exception
   {
      List<Map<String, String>> before = snapshot();
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend())))
      {
         Connection connection = transaction.getConnection();
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE field_lab SET text_value='Earlier caller work' WHERE id=2"));
         }
         List<Map<String, String>> pending = rows(connection, "SELECT * FROM field_lab ORDER BY id");
         UpdateInput failing = input(new QRecord().withValue("id", 1).withValue("name", null)).withInputSource(() -> false).withTransaction(transaction);
         assertSqlState("23502", assertThrows(QException.class, () -> new UpdateAction().execute(failing)));
         assertSame(transaction, failing.getTransaction());
         assertSame(connection, transaction.getConnection());
         assertFalse(connection.isClosed());
         assertFalse(connection.getAutoCommit());
         assertEquals(pending, rows(connection, "SELECT * FROM field_lab ORDER BY id"));
         assertEquals(before, snapshot());
         QRecord later = UpdateAction.executeForRecords(input(new QRecord().withValue("id", 3).withValue("textValue", "Later caller work"))
            .withTransaction(transaction)).get(0);
         pending.get(2).put("TEXT_VALUE", "Later caller work");
         assertTrue(later.getErrors().isEmpty(), later.getErrorsAsString());
         assertEquals(pending, rows(connection, "SELECT * FROM field_lab ORDER BY id"));
         assertEquals(before, snapshot());
         transaction.rollback();
         assertFalse(connection.isClosed());
         assertEquals(before, rows(connection, "SELECT * FROM field_lab ORDER BY id"));
      }
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Prefetch and uniqueness must see the caller's uncommitted target/owner;
    ** ordinary independent connections still see the prior committed database.
    *******************************************************************************/
   @Test
   void testCallerTransactionFindsPendingTargetAndOwnerThenRollsBack() throws Exception
   {
      List<Map<String, String>> before = snapshot();
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend())))
      {
         Connection connection = transaction.getConnection();
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("INSERT INTO field_lab (id,name,text_value) VALUES(99,'Pending owner','Native pending')"));
         }
         List<Map<String, String>> expected = rows(connection, "SELECT * FROM field_lab ORDER BY id");
         expected.get(4).put("TEXT_VALUE", "Updated pending target");
         UpdateInput pendingInput = input(new QRecord().withValue("id", "99").withValue("textValue", "Updated pending target")).withTransaction(transaction);
         QRecord updated = UpdateAction.executeForRecords(pendingInput).get(0);
         assertTrue(updated.getErrors().isEmpty(), updated.getErrorsAsString());
         assertEquals(expected, rows(connection, "SELECT * FROM field_lab ORDER BY id"));
         QRecord collision = UpdateAction.executeForRecords(input(new QRecord().withValue("id", 1).withValue("name", "Pending owner"))
            .withTransaction(transaction)).get(0);
         assertTrue(collision.getErrors().stream().anyMatch(error -> error instanceof DuplicateKeyBadInputStatusMessage));
         assertEquals(expected, rows(connection, "SELECT * FROM field_lab ORDER BY id"));
         assertEquals(before, snapshot());
         assertSame(transaction, pendingInput.getTransaction());
         assertSame(connection, transaction.getConnection());
         assertFalse(connection.isClosed());
         assertFalse(connection.getAutoCommit());
         transaction.rollback();
         assertFalse(connection.isClosed());
         assertEquals(before, rows(connection, "SELECT * FROM field_lab ORDER BY id"));
      }
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Update never commits an explicitly supplied caller transaction itself.
    *******************************************************************************/
   @Test
   void testOnlyCallerCommitMakesUpdateVisible() throws Exception
   {
      List<Map<String, String>> before = snapshot();
      List<Map<String, String>> expected = snapshot();
      expected.get(0).put("LONG_VALUE", "99");
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend())))
      {
         Connection connection = transaction.getConnection();
         UpdateInput updateInput = input(new QRecord().withValue("id", 1).withValue("longValue", 99L)).withTransaction(transaction);
         QRecord updated = UpdateAction.executeForRecords(updateInput).get(0);
         assertTrue(updated.getErrors().isEmpty(), updated.getErrorsAsString());
         assertEquals(expected, rows(connection, "SELECT * FROM field_lab ORDER BY id"));
         assertEquals(before, snapshot());
         assertSame(transaction, updateInput.getTransaction());
         assertFalse(connection.isClosed());
         assertFalse(connection.getAutoCommit());
         transaction.commit();
         assertFalse(connection.isClosed());
         assertEquals(expected, rows(connection, "SELECT * FROM field_lab ORDER BY id"));
         assertEquals(expected, snapshot());
      }
      assertEquals(expected, snapshot());
   }



   /*******************************************************************************
    ** Empty updates still require valid table/context/session. Recovery uses the
    ** same pending input, proving rejected invocations did not consume the patch.
    *******************************************************************************/
   @Test
   void testEmptyInputsAndMissingContextRecoverWithoutMutation() throws Exception
   {
      List<Map<String, String>> before = snapshot();
      List<Map<String, String>> people = rows("SELECT * FROM person ORDER BY id");
      assertTrue(new UpdateAction().execute(new UpdateInput(TABLE)).getRecords().isEmpty());
      assertTrue(UpdateAction.executeForRecords(input()).isEmpty());
      assertThrows(QException.class, () -> new UpdateAction().execute(new UpdateInput()));
      assertThrows(QException.class, () -> new UpdateAction().execute(new UpdateInput("missingUpdateTable")));
      UpdateInput pending = new UpdateInput("person").withOmitModifyDateUpdate(true)
         .withRecord(new QRecord().withValue("id", 1).withValue("firstName", "Recovered update"));
      try
      {
         QContext.setQSession(null);
         assertEquals("QSession was not set in QContext.", assertThrows(QException.class, () -> new UpdateAction().execute(pending)).getMessage());
         QContext.clear();
         assertEquals("QInstance was not set in QContext.", assertThrows(QException.class, () -> new UpdateAction().execute(pending)).getMessage());
      }
      finally
      {
         QContext.init(instance, new QSession());
      }
      assertEquals(before, snapshot());
      assertEquals(people, rows("SELECT * FROM person ORDER BY id"));
      QRecord recovered = UpdateAction.executeForRecords(pending).get(0);
      people.get(0).put("FIRST_NAME", "Recovered update");
      assertTrue(recovered.getErrors().isEmpty(), recovered.getErrorsAsString());
      assertEquals(people, rows("SELECT * FROM person ORDER BY id"));
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Date defaults have separate acceptance; omit them for exact patch oracles.
    *******************************************************************************/
   private UpdateInput input(QRecord... records)
   {
      return new UpdateInput(TABLE).withOmitModifyDateUpdate(true).withRecords(List.of(records));
   }



   /*******************************************************************************
    ** Require a concrete native error rather than an unrelated QException.
    *******************************************************************************/
   private void assertSqlState(String expected, QException exception)
   {
      Throwable cause = exception;
      while(cause != null && !(cause instanceof SQLException))
      {
         cause = cause.getCause();
      }
      assertEquals(expected, assertInstanceOf(SQLException.class, cause).getSQLState());
   }



   /*******************************************************************************
    ** Stable native row order includes all untouched values and unrelated rows.
    *******************************************************************************/
   private List<Map<String, String>> snapshot() throws SQLException
   {
      return rows("SELECT * FROM field_lab ORDER BY id");
   }



   /*******************************************************************************
    ** This helper owns its independent native connection.
    *******************************************************************************/
   private List<Map<String, String>> rows(String sql) throws SQLException
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()))
      {
         return rows(connection, sql);
      }
   }



   /*******************************************************************************
    ** Statement/result cleanup must leave the supplied caller connection open.
    ** Binary contents are not changed by this fixture and remain SQL NULL.
    *******************************************************************************/
   private List<Map<String, String>> rows(Connection connection, String sql) throws SQLException
   {
      List<Map<String, String>> rows = new ArrayList<>();
      try(Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         while(result.next())
         {
            Map<String, String> row = new LinkedHashMap<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.put(result.getMetaData().getColumnLabel(column), result.getString(column));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Supplemental customizers load by class reference and must return a clone.
    *******************************************************************************/
   public static class ProtectedUpdateField implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       ** The control deliberately leaves SYSTEM metadata unrestricted.
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(!TABLE.equals(input.getTableName()) || !QInputSource.USER.equals(input.getInputSource()))
         {
            return input.getTable();
         }
         QTableMetaData personalized = input.getTable().clone();
         personalized.getFields().remove("textValue");
         return personalized;
      }
   }
}
