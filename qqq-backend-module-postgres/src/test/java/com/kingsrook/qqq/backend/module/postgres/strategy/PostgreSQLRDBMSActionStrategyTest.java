/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.postgres.strategy;


import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Test class for PostgreSQLRDBMSActionStrategy.
 **
 ** This class tests PostgreSQL-specific behavior including:
 ** - Type-safe parameter binding for all data types
 ** - Null value handling with Types.OTHER
 ** - Temporal type conversions
 ** - RETURNING clause for insert operations
 ** - Type casting in WHERE clauses (especially for bigint vs varchar issues)
 *******************************************************************************/
public class PostgreSQLRDBMSActionStrategyTest extends BaseTest
{

   /*******************************************************************************
    ** Tests that numeric types (Integer, Long, Short, Double, Float) can be
    ** bound as parameters without type mismatch errors.
    **
    ** This test specifically addresses the issue where PostgreSQL requires
    ** exact type matching and won't automatically cast between types.
    **
    ** @throws Exception if test fails
    *******************************************************************************/
   @Test
   void testNumericTypeParameterBinding() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         PostgreSQLRDBMSActionStrategy strategy = new PostgreSQLRDBMSActionStrategy();

         // Test with all numeric types
         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE id = ?"))
            {
               strategy.bindParamObject(ps, 1, (Integer) 1);
               ps.execute();
            }
         }, "Integer binding should not throw");

         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE id = ?"))
            {
               strategy.bindParamObject(ps, 1, (Long) 1L);
               ps.execute();
            }
         }, "Long binding should not throw");

         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE id = ?"))
            {
               strategy.bindParamObject(ps, 1, (Short) (short) 1);
               ps.execute();
            }
         }, "Short binding should not throw");

         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE annual_salary = ?"))
            {
               strategy.bindParamObject(ps, 1, (Double) 25000.0);
               ps.execute();
            }
         }, "Double binding should not throw");

         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE annual_salary = ?"))
            {
               strategy.bindParamObject(ps, 1, (Float) 25000.0f);
               ps.execute();
            }
         }, "Float binding should not throw");
      }
   }



   /*******************************************************************************
    ** Tests that null values are properly handled using Types.OTHER,
    ** allowing PostgreSQL to infer the correct type from the column definition.
    **
    ** This addresses the issue where using Types.CHAR or specific types for
    ** null values can cause type mismatch errors in PostgreSQL.
    **
    ** @throws Exception if test fails
    *******************************************************************************/
   @Test
   void testNullParameterBindingWithTypesOther() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         PostgreSQLRDBMSActionStrategy strategy = new PostgreSQLRDBMSActionStrategy();

         // Test null Integer
         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE id = ?"))
            {
               strategy.bindParam(ps, 1, (Integer) null);
               ps.execute();
            }
         }, "Null Integer binding should not throw");

         // Test null Long
         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE id = ?"))
            {
               strategy.bindParam(ps, 1, (Long) null);
               ps.execute();
            }
         }, "Null Long binding should not throw");

         // Test null Double
         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE annual_salary = ?"))
            {
               strategy.bindParam(ps, 1, (Double) null);
               ps.execute();
            }
         }, "Null Double binding should not throw");

         // Test null String
         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE first_name = ?"))
            {
               strategy.bindParam(ps, 1, (String) null);
               ps.execute();
            }
         }, "Null String binding should not throw");

         // Test null via bindParamObject
         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE id = ?"))
            {
               strategy.bindParamObject(ps, 1, null);
               ps.execute();
            }
         }, "Null Object binding should not throw");
      }
   }



   /*******************************************************************************
    ** Tests that temporal types (Instant, LocalDate, LocalTime, LocalDateTime)
    ** are properly converted to PostgreSQL-compatible formats.
    **
    ** @throws Exception if test fails
    *******************************************************************************/
   @Test
   void testTemporalTypeParameterBinding() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         PostgreSQLRDBMSActionStrategy strategy = new PostgreSQLRDBMSActionStrategy();

         // Test Instant
         Instant now = Instant.now();
         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE create_date > ?"))
            {
               strategy.bindParamObject(ps, 1, now);
               ps.execute();
            }
         }, "Instant binding should not throw");

         // Test LocalDate
         LocalDate today = LocalDate.now();
         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE birth_date = ?"))
            {
               strategy.bindParamObject(ps, 1, today);
               ps.execute();
            }
         }, "LocalDate binding should not throw");

         // Test LocalTime
         LocalTime time = LocalTime.of(9, 0, 0);
         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE start_time = ?"))
            {
               strategy.bindParamObject(ps, 1, time);
               ps.execute();
            }
         }, "LocalTime binding should not throw");

         // Test LocalDateTime
         LocalDateTime dateTime = LocalDateTime.now();
         assertDoesNotThrow(() ->
         {
            try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM person WHERE create_date > ?"))
            {
               strategy.bindParamObject(ps, 1, dateTime);
               ps.execute();
            }
         }, "LocalDateTime binding should not throw");
      }
   }



   /*******************************************************************************
    ** Tests that the RETURNING clause is properly used for insert operations
    ** to retrieve generated primary keys.
    **
    ** This is a PostgreSQL optimization that is more efficient than JDBC's
    ** Statement.RETURN_GENERATED_KEYS approach.
    **
    ** @throws Exception if test fails
    *******************************************************************************/
   @Test
   void testInsertWithReturningClause() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         PostgreSQLRDBMSActionStrategy strategy = new PostgreSQLRDBMSActionStrategy();

         String         sql     = "INSERT INTO person (first_name, last_name, email) VALUES (?, ?, ?)";
         List<Object>   params  = List.of("Test", "User", "test@example.com");
         QFieldMetaData pkField = new QFieldMetaData("id", QFieldType.INTEGER);

         List<java.io.Serializable> generatedIds = strategy.executeInsertForGeneratedIds(
            connection,
            sql,
            params,
            pkField
         );

         assertNotNull(generatedIds, "Generated IDs should not be null");
         assertEquals(1, generatedIds.size(), "Should return exactly one generated ID");
         assertTrue(generatedIds.get(0) instanceof Integer, "Generated ID should be an Integer");
         assertTrue((Integer) generatedIds.get(0) > 0, "Generated ID should be positive");
      }
   }



   /*******************************************************************************
    ** Tests that update operations with bigint WHERE clauses work correctly.
    **
    ** This specifically tests the original issue where "id IN (?)" with a Long
    ** value was causing "operator does not exist: bigint = character varying" errors.
    **
    ** @throws QException if test fails
    *******************************************************************************/
   @Test
   void testUpdateWithBigIntInWhereClause() throws QException
   {
      // Insert a test record first
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      insertInput.setRecords(List.of(
         new QRecord()
            .withValue("firstName", "Update")
            .withValue("lastName", "Test")
            .withValue("email", "update.test@example.com")
            .withValue("birthDate", "1990-01-01")
      ));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      Integer      newId        = insertOutput.getRecords().get(0).getValueInteger("id");

      // Now update it using the ID in WHERE clause
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      updateInput.setRecords(List.of(
         new QRecord()
            .withValue("id", newId)
            .withValue("firstName", "Updated")
            .withValue("lastName", "User")
      ));

      // This should not throw "operator does not exist: bigint = character varying"
      assertDoesNotThrow(() -> new UpdateAction().execute(updateInput),
         "Update with bigint ID should not throw type mismatch error");

      // Verify the update worked
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, newId)));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      assertEquals(1, queryOutput.getRecords().size(), "Should find exactly one record");
      assertEquals("Updated", queryOutput.getRecords().get(0).getValueString("firstName"));
      assertEquals("User", queryOutput.getRecords().get(0).getValueString("lastName"));
   }



   /*******************************************************************************
    ** Tests comprehensive type handling in a real insert/query scenario.
    **
    ** This test inserts a record with various data types and verifies they
    ** are all properly stored and retrieved.
    **
    ** @throws QException if test fails
    *******************************************************************************/
   @Test
   void testComprehensiveTypeHandling() throws QException
   {
      LocalDate birthDate = LocalDate.of(1985, 6, 15);
      LocalTime startTime = LocalTime.of(9, 30, 0);

      // Insert with various types
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      insertInput.setRecords(List.of(
         new QRecord()
            .withValue("firstName", "Comprehensive")
            .withValue("lastName", "TypeTest")
            .withValue("email", "types@test.com")
            .withValue("birthDate", birthDate)
            .withValue("isEmployed", true)
            .withValue("annualSalary", new BigDecimal("75000.50"))
            .withValue("daysWorked", 250)
            .withValue("homeTown", "Springfield")
            .withValue("startTime", startTime)
      ));

      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      Integer      newId        = insertOutput.getRecords().get(0).getValueInteger("id");

      // Query it back
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, newId)));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      assertEquals(1, queryOutput.getRecords().size(), "Should find exactly one record");
      QRecord record = queryOutput.getRecords().get(0);

      assertEquals("Comprehensive", record.getValueString("firstName"));
      assertEquals("TypeTest", record.getValueString("lastName"));
      assertEquals("types@test.com", record.getValueString("email"));
      assertEquals(birthDate.toString(), record.getValueString("birthDate"));
      assertEquals(true, record.getValueBoolean("isEmployed"));
      assertEquals(new BigDecimal("75000.50"), record.getValueBigDecimal("annualSalary"));
      assertEquals(250, record.getValueInteger("daysWorked"));
      assertEquals("Springfield", record.getValueString("homeTown"));
      assertNotNull(record.getValue("createDate"));
      assertNotNull(record.getValue("modifyDate"));
   }



   /*******************************************************************************
    ** Tests that queries with various numeric types in WHERE clauses work correctly.
    **
    ** @throws QException if test fails
    *******************************************************************************/
   @Test
   void testQueryWithNumericTypes() throws QException
   {
      // Query by Integer ID
      QueryInput queryInput1 = new QueryInput();
      queryInput1.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput1.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1)));
      QueryOutput queryOutput1 = new QueryAction().execute(queryInput1);
      assertEquals(1, queryOutput1.getRecords().size(), "Should find person with id=1");

      // Query by Long ID
      QueryInput queryInput2 = new QueryInput();
      queryInput2.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput2.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 2L)));
      QueryOutput queryOutput2 = new QueryAction().execute(queryInput2);
      assertEquals(1, queryOutput2.getRecords().size(), "Should find person with id=2");

      // Query by Integer days worked
      QueryInput queryInput3 = new QueryInput();
      queryInput3.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput3.setFilter(new QQueryFilter(new QFilterCriteria("daysWorked", QCriteriaOperator.GREATER_THAN, 100)));
      QueryOutput queryOutput3 = new QueryAction().execute(queryInput3);
      assertTrue(queryOutput3.getRecords().size() > 0, "Should find people with daysWorked > 100");

      // Query by BigDecimal salary
      QueryInput queryInput4 = new QueryInput();
      queryInput4.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput4.setFilter(new QQueryFilter(new QFilterCriteria("annualSalary", QCriteriaOperator.GREATER_THAN, new BigDecimal("26000"))));
      QueryOutput queryOutput4 = new QueryAction().execute(queryInput4);
      assertTrue(queryOutput4.getRecords().size() > 0, "Should find people with salary > 26000");
   }



   /*******************************************************************************
    ** Tests that the identifier quote string returns the correct value for PostgreSQL.
    **
    ** PostgreSQL uses double quotes for identifier quoting.
    *******************************************************************************/
   @Test
   void testIdentifierQuoteString()
   {
      PostgreSQLRDBMSActionStrategy strategy = new PostgreSQLRDBMSActionStrategy();
      assertEquals("\"", strategy.getIdentifierQuoteString(),
         "PostgreSQL should use double quotes for identifier quoting");
   }



   /*******************************************************************************
    ** Tests that null values in WHERE clauses work correctly.
    **
    ** @throws QException if test fails
    *******************************************************************************/
   @Test
   void testQueryWithNullValues() throws QException
   {
      // Query for records where birthDate IS NULL
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("birthDate", QCriteriaOperator.IS_BLANK)));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      assertTrue(queryOutput.getRecords().size() > 0, "Should find at least one person with null birthDate");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValue("birthDate") == null),
         "All returned records should have null birthDate");
   }



   /*******************************************************************************
    ** Tests batch operations with proper type handling.
    **
    ** @throws Exception if test fails
    *******************************************************************************/
   @Test
   void testBatchOperationsWithTypeHandling() throws Exception
   {
      // Insert multiple records with various types
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      insertInput.setRecords(List.of(
         new QRecord()
            .withValue("firstName", "Batch1")
            .withValue("lastName", "Test1")
            .withValue("email", "batch1@test.com")
            .withValue("birthDate", "1990-01-01")
            .withValue("daysWorked", 100),
         new QRecord()
            .withValue("firstName", "Batch2")
            .withValue("lastName", "Test2")
            .withValue("email", "batch2@test.com")
            .withValue("birthDate", "1991-02-02")
            .withValue("daysWorked", 200),
         new QRecord()
            .withValue("firstName", "Batch3")
            .withValue("lastName", "Test3")
            .withValue("email", "batch3@test.com")
            .withValue("birthDate", "1992-03-03")
            .withValue("daysWorked", 300)
      ));

      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(3, insertOutput.getRecords().size(), "Should insert 3 records");

      // Verify all have IDs
      assertTrue(insertOutput.getRecords().stream().allMatch(r -> r.getValue("id") != null),
         "All records should have generated IDs");

      // Update all of them
      List<QRecord> recordsToUpdate = insertOutput.getRecords().stream()
         .map(r -> new QRecord()
            .withValue("id", r.getValue("id"))
            .withValue("homeTown", "BatchCity"))
         .toList();

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      updateInput.setRecords(recordsToUpdate);

      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
      assertEquals(3, updateOutput.getRecords().size(), "Should update 3 records");
   }
}
