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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 **
 *******************************************************************************/
class QueryManagerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws SQLException
   {
      try(Connection connection = getConnection())
      {
         QueryManager.executeUpdate(connection, """
            CREATE TABLE test_table
            (
               int_col INTEGER,
               datetime_col DATETIME,
               char_col CHAR(1),
               date_col DATE,
               time_col TIME,
               long_col LONG
            )
            """);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach() throws SQLException
   {
      try(Connection connection = getConnection())
      {
         QueryManager.executeUpdate(connection, "DROP TABLE test_table");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection getConnection() throws SQLException
   {
      return new ConnectionManager().getConnection(TestUtils.defineBackend());
   }



   /*******************************************************************************
    ** Test the various overloads that bind params.
    ** Note, we're just confirming that these methods don't throw...
    *******************************************************************************/
   @Test
   void testBindParams() throws SQLException
   {
      try(Connection connection = getConnection())
      {
         long              ctMillis = System.currentTimeMillis();
         PreparedStatement ps       = connection.prepareStatement("UPDATE test_table SET int_col = ? WHERE int_col > 0");

         ///////////////////////////////////////////////////////////////////////////////
         // these calls - we just want to assert that they don't throw any exceptions //
         ///////////////////////////////////////////////////////////////////////////////
         QueryManager.bindParamObject(ps, 1, (short) 1);
         QueryManager.bindParamObject(ps, 1, (long) 1);
         QueryManager.bindParamObject(ps, 1, true);
         QueryManager.bindParamObject(ps, 1, BigDecimal.ONE);
         QueryManager.bindParamObject(ps, 1, "hello".getBytes(StandardCharsets.UTF_8));
         QueryManager.bindParamObject(ps, 1, new Timestamp(ctMillis));
         QueryManager.bindParamObject(ps, 1, new Date(ctMillis));
         QueryManager.bindParamObject(ps, 1, new GregorianCalendar());
         QueryManager.bindParamObject(ps, 1, LocalDate.now());
         QueryManager.bindParamObject(ps, 1, OffsetDateTime.now());
         QueryManager.bindParamObject(ps, 1, LocalDateTime.now());
         QueryManager.bindParamObject(ps, 1, AutomationStatus.PENDING_INSERT_AUTOMATIONS);

         assertThrows(SQLException.class, () ->
         {
            QueryManager.bindParamObject(ps, 1, new Object());
         });

         QueryManager.bindParam(ps, 1, (Integer) null);
         QueryManager.bindParam(ps, 1, (Boolean) null);
         QueryManager.bindParam(ps, 1, (BigDecimal) null);
         QueryManager.bindParam(ps, 1, (byte[]) null);
         QueryManager.bindParam(ps, 1, (Timestamp) null);
         QueryManager.bindParam(ps, 1, (String) null);
         QueryManager.bindParam(ps, 1, (Date) null);
         QueryManager.bindParam(ps, 1, (GregorianCalendar) null);
         QueryManager.bindParam(ps, 1, (LocalDate) null);
         QueryManager.bindParam(ps, 1, (LocalDateTime) null);

         QueryManager.bindParam(ps, 1, 1);
         QueryManager.bindParam(ps, 1, true);
         QueryManager.bindParam(ps, 1, BigDecimal.ONE);
         QueryManager.bindParam(ps, 1, "hello".getBytes(StandardCharsets.UTF_8));
         QueryManager.bindParam(ps, 1, new Timestamp(ctMillis));
         QueryManager.bindParam(ps, 1, "hello");
         QueryManager.bindParam(ps, 1, new Date(ctMillis));
         QueryManager.bindParam(ps, 1, new GregorianCalendar());
         QueryManager.bindParam(ps, 1, LocalDate.now());
         QueryManager.bindParam(ps, 1, LocalDateTime.now());

         ////////////////////////////////////////////////////////////////////////////////////////////////
         // originally longs were being downgraded to int when binding, so, verify that doesn't happen //
         ////////////////////////////////////////////////////////////////////////////////////////////////
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLongBinding() throws SQLException
   {
      try(Connection connection = getConnection())
      {
         Long biggerThanMaxInteger = 2147483648L;

         PreparedStatement ps = connection.prepareStatement("INSERT INTO test_table (long_col) VALUES (?)");
         QueryManager.bindParam(ps, 1, biggerThanMaxInteger);
         ps.execute();

         ps = connection.prepareStatement("SELECT long_col FROM test_table WHERE long_col = ?");
         QueryManager.bindParam(ps, 1, biggerThanMaxInteger);
         ps.execute();
         ResultSet rs = ps.getResultSet();
         assertTrue(rs.next());
         assertEquals(biggerThanMaxInteger, QueryManager.getLong(rs, "long_col"));
      }
   }



   /*******************************************************************************
    ** Test the various getXXX methods from result sets
    *******************************************************************************/
   @Test
   void testGetValueMethods() throws SQLException
   {
      try(Connection connection = getConnection())
      {
         Long biggerThanMaxInteger = 2147483648L;

         QueryManager.executeUpdate(connection, "INSERT INTO test_table (int_col, datetime_col, char_col, long_col) VALUES (1, now(), 'A', " + biggerThanMaxInteger + ")");
         PreparedStatement preparedStatement = connection.prepareStatement("SELECT int_col, datetime_col, char_col, long_col from test_table");
         preparedStatement.execute();
         ResultSet rs = preparedStatement.getResultSet();
         rs.next();

         assertEquals(1, QueryManager.getInteger(rs, "int_col"));
         assertEquals(1, QueryManager.getInteger(rs, 1));
         assertEquals(1L, QueryManager.getLong(rs, "int_col"));
         assertEquals(1L, QueryManager.getLong(rs, 1));
         assertArrayEquals(new byte[] { 0, 0, 0, 1 }, QueryManager.getByteArray(rs, "int_col"));
         assertArrayEquals(new byte[] { 0, 0, 0, 1 }, QueryManager.getByteArray(rs, 1));
         assertEquals(1, QueryManager.getObject(rs, "int_col"));
         assertEquals(1, QueryManager.getObject(rs, 1));
         assertEquals(BigDecimal.ONE, QueryManager.getBigDecimal(rs, "int_col"));
         assertEquals(BigDecimal.ONE, QueryManager.getBigDecimal(rs, 1));
         assertEquals(true, QueryManager.getBoolean(rs, "int_col"));
         assertEquals(true, QueryManager.getBoolean(rs, 1));
         assertNotNull(QueryManager.getDate(rs, "datetime_col"));
         assertNotNull(QueryManager.getDate(rs, 2));
         assertNotNull(QueryManager.getCalendar(rs, "datetime_col"));
         assertNotNull(QueryManager.getCalendar(rs, 2));
         assertNotNull(QueryManager.getLocalDate(rs, "datetime_col"));
         assertNotNull(QueryManager.getLocalDate(rs, 2));
         assertNotNull(QueryManager.getLocalDateTime(rs, "datetime_col"));
         assertNotNull(QueryManager.getLocalDateTime(rs, 2));
         assertNotNull(QueryManager.getOffsetDateTime(rs, "datetime_col"));
         assertNotNull(QueryManager.getOffsetDateTime(rs, 2));
         assertNotNull(QueryManager.getTimestamp(rs, "datetime_col"));
         assertNotNull(QueryManager.getTimestamp(rs, 2));
         assertEquals("A", QueryManager.getObject(rs, "char_col"));
         assertEquals("A", QueryManager.getObject(rs, 3));
         assertEquals(biggerThanMaxInteger, QueryManager.getLong(rs, "long_col"));
         assertEquals(biggerThanMaxInteger, QueryManager.getLong(rs, 4));
      }
   }



   /*******************************************************************************
    ** Test the various getXXX methods from result sets, when they return null
    *******************************************************************************/
   @Test
   void testGetValueMethodsReturningNull() throws SQLException
   {
      try(Connection connection = getConnection())
      {
         QueryManager.executeUpdate(connection, "INSERT INTO test_table (int_col, datetime_col, char_col) VALUES (null, null, null)");
         PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from test_table");
         preparedStatement.execute();
         ResultSet rs = preparedStatement.getResultSet();
         rs.next();

         assertNull(QueryManager.getInteger(rs, "int_col"));
         assertNull(QueryManager.getInteger(rs, 1));
         assertNull(QueryManager.getLong(rs, "int_col"));
         assertNull(QueryManager.getLong(rs, 1));
         assertNull(QueryManager.getByteArray(rs, "int_col"));
         assertNull(QueryManager.getByteArray(rs, 1));
         assertNull(QueryManager.getObject(rs, "int_col"));
         assertNull(QueryManager.getObject(rs, 1));
         assertNull(QueryManager.getBigDecimal(rs, "int_col"));
         assertNull(QueryManager.getBigDecimal(rs, 1));
         assertNull(QueryManager.getBoolean(rs, "int_col"));
         assertNull(QueryManager.getBoolean(rs, 1));
         assertNull(QueryManager.getDate(rs, "datetime_col"));
         assertNull(QueryManager.getDate(rs, 2));
         assertNull(QueryManager.getCalendar(rs, "datetime_col"));
         assertNull(QueryManager.getCalendar(rs, 2));
         assertNull(QueryManager.getLocalDate(rs, "datetime_col"));
         assertNull(QueryManager.getLocalDate(rs, 2));
         assertNull(QueryManager.getLocalDateTime(rs, "datetime_col"));
         assertNull(QueryManager.getLocalDateTime(rs, 2));
         assertNull(QueryManager.getOffsetDateTime(rs, "datetime_col"));
         assertNull(QueryManager.getOffsetDateTime(rs, 2));
         assertNull(QueryManager.getTimestamp(rs, "datetime_col"));
         assertNull(QueryManager.getTimestamp(rs, 2));
         assertNull(QueryManager.getObject(rs, "char_col"));
         assertNull(QueryManager.getObject(rs, 3));
      }
   }



   /*******************************************************************************
    ** We had a bug where LocalDates weren't being properly bound.  This test
    ** confirms (more?) correct behavior
    *******************************************************************************/
   @Test
   @SuppressWarnings("deprecation")
   void testLocalDate() throws SQLException
   {
      try(Connection connection = getConnection())
      {
         QueryManager.executeUpdate(connection, "INSERT INTO test_table (date_col) VALUES (?)", LocalDate.of(2013, Month.OCTOBER, 1));

         PreparedStatement preparedStatement = connection.prepareStatement("SELECT date_col from test_table");
         preparedStatement.execute();
         ResultSet rs = preparedStatement.getResultSet();
         rs.next();

         Date date  = QueryManager.getDate(rs, 1);
         assertNotNull(date);
         assertEquals(1, date.getDate(), "Date value");
         assertEquals(Month.OCTOBER.getValue(), date.getMonth() + 1, "Month value");
         assertEquals(2013, date.getYear() + 1900, "Year value");

         LocalDate localDate = QueryManager.getLocalDate(rs, 1);
         assertNotNull(localDate);
         assertEquals(1, localDate.getDayOfMonth(), "Date value");
         assertEquals(Month.OCTOBER, localDate.getMonth(), "Month value");
         assertEquals(2013, localDate.getYear(), "Year value");

         LocalDateTime localDateTime = QueryManager.getLocalDateTime(rs, 1);
         assertNotNull(localDateTime);
         assertEquals(1, localDateTime.getDayOfMonth(), "Date value");
         assertEquals(Month.OCTOBER, localDateTime.getMonth(), "Month value");
         assertEquals(2013, localDateTime.getYear(), "Year value");
         assertEquals(0, localDateTime.getHour(), "Hour value");
         assertEquals(0, localDateTime.getMinute(), "Minute value");

         OffsetDateTime offsetDateTime = QueryManager.getOffsetDateTime(rs, 1);
         assertNotNull(offsetDateTime);
         assertEquals(1, offsetDateTime.getDayOfMonth(), "Date value");
         assertEquals(Month.OCTOBER, offsetDateTime.getMonth(), "Month value");
         assertEquals(2013, offsetDateTime.getYear(), "Year value");
         assertEquals(0, offsetDateTime.getHour(), "Hour value");
         assertEquals(0, offsetDateTime.getMinute(), "Minute value");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLocalTime() throws SQLException
   {
      try(Connection connection = getConnection())
      {
         ////////////////////////////////////
         // insert one just hour & minutes //
         ////////////////////////////////////
         QueryManager.executeUpdate(connection, "INSERT INTO test_table (int_col, time_col) VALUES (?, ?)", 1, LocalTime.of(10, 42));

         PreparedStatement preparedStatement = connection.prepareStatement("SELECT time_col from test_table where int_col=1");
         preparedStatement.execute();
         ResultSet rs = preparedStatement.getResultSet();
         rs.next();

         LocalTime localTime = QueryManager.getLocalTime(rs, 1);
         assertEquals(10, localTime.getHour(), "Hour value");
         assertEquals(42, localTime.getMinute(), "Minute value");
         assertEquals(0, localTime.getSecond(), "Second value");

         localTime = QueryManager.getLocalTime(rs, "time_col");
         assertEquals(10, localTime.getHour(), "Hour value");
         assertEquals(42, localTime.getMinute(), "Minute value");
         assertEquals(0, localTime.getSecond(), "Second value");

         /////////////////////////////////
         // now insert one with seconds //
         /////////////////////////////////
         QueryManager.executeUpdate(connection, "INSERT INTO test_table (int_col, time_col) VALUES (?, ?)", 2, LocalTime.of(10, 42, 59));

         preparedStatement = connection.prepareStatement("SELECT time_col from test_table where int_col=2");
         preparedStatement.execute();
         rs = preparedStatement.getResultSet();
         rs.next();

         localTime = QueryManager.getLocalTime(rs, 1);
         assertEquals(10, localTime.getHour(), "Hour value");
         assertEquals(42, localTime.getMinute(), "Minute value");
         assertEquals(59, localTime.getSecond(), "Second value");

         localTime = QueryManager.getLocalTime(rs, "time_col");
         assertEquals(10, localTime.getHour(), "Hour value");
         assertEquals(42, localTime.getMinute(), "Minute value");
         assertEquals(59, localTime.getSecond(), "Second value");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExecuteStatementForSingleValue() throws SQLException
   {
      try(Connection connection = getConnection())
      {
         QueryManager.executeUpdate(connection, """
            INSERT INTO test_table
            ( int_col, datetime_col, char_col, date_col, time_col )
            VALUES
            ( 47, '2022-08-10 19:22:08', 'Q', '2022-08-10', '19:22:08')
            """);
         assertEquals(null, QueryManager.executeStatementForSingleValue(connection, Integer.class, "SELECT int_col FROM test_table WHERE int_col = -1"));
         assertEquals(1, QueryManager.executeStatementForSingleValue(connection, Integer.class, "SELECT COUNT(*) FROM test_table"));
         assertEquals(47, QueryManager.executeStatementForSingleValue(connection, Integer.class, "SELECT int_col FROM test_table"));
         assertEquals("Q", QueryManager.executeStatementForSingleValue(connection, String.class, "SELECT char_col FROM test_table"));
         assertEquals(new BigDecimal("1.1"), QueryManager.executeStatementForSingleValue(connection, BigDecimal.class, "SELECT 1.1 FROM test_table"));
         assertEquals(1, QueryManager.executeStatementForSingleValue(connection, Integer.class, "SELECT 1.1 FROM test_table"));

         QueryManager.executeUpdate(connection, """
            INSERT INTO test_table
            ( int_col, datetime_col, char_col, date_col, time_col )
            VALUES
            ( null, null, null, null, null)
            """);
         assertEquals(null, QueryManager.executeStatementForSingleValue(connection, Integer.class, "SELECT int_col FROM test_table WHERE int_col IS NULL"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryForRows() throws SQLException
   {
      try(Connection connection = getConnection())
      {
         QueryManager.executeUpdate(connection, """
            INSERT INTO test_table
            ( int_col, datetime_col, char_col, date_col, time_col )
            VALUES
            ( 47, '2022-08-10 19:22:08', 'Q', '2022-08-10', '19:22:08')
            """);
         List<Map<String, Object>> rows = QueryManager.executeStatementForRows(connection, "SELECT * FROM test_table");
         assertNotNull(rows);
         assertEquals(47, rows.get(0).get("int_col"));
         assertEquals("Q", rows.get(0).get("char_col"));
      }
   }

}