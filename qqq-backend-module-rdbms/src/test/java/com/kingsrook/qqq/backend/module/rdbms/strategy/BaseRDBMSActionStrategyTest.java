/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.strategy;


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
import java.time.OffsetDateTime;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for BaseRDBMSActionStrategy 
 *******************************************************************************/
class BaseRDBMSActionStrategyTest extends BaseTest
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

         BaseRDBMSActionStrategy strategy = new BaseRDBMSActionStrategy();

         ///////////////////////////////////////////////////////////////////////////////
         // these calls - we just want to assert that they don't throw any exceptions //
         ///////////////////////////////////////////////////////////////////////////////
         strategy.bindParamObject(ps, 1, (short) 1);
         strategy.bindParamObject(ps, 1, (long) 1);
         strategy.bindParamObject(ps, 1, true);
         strategy.bindParamObject(ps, 1, BigDecimal.ONE);
         strategy.bindParamObject(ps, 1, "hello".getBytes(StandardCharsets.UTF_8));
         strategy.bindParamObject(ps, 1, new Timestamp(ctMillis));
         strategy.bindParamObject(ps, 1, new Date(ctMillis));
         strategy.bindParamObject(ps, 1, new GregorianCalendar());
         strategy.bindParamObject(ps, 1, LocalDate.now());
         strategy.bindParamObject(ps, 1, OffsetDateTime.now());
         strategy.bindParamObject(ps, 1, LocalDateTime.now());
         strategy.bindParamObject(ps, 1, AutomationStatus.PENDING_INSERT_AUTOMATIONS);

         assertThrows(SQLException.class, () -> strategy.bindParamObject(ps, 1, new Object()));

         strategy.bindParam(ps, 1, (Integer) null);
         strategy.bindParam(ps, 1, (Boolean) null);
         strategy.bindParam(ps, 1, (BigDecimal) null);
         strategy.bindParam(ps, 1, (byte[]) null);
         strategy.bindParam(ps, 1, (Timestamp) null);
         strategy.bindParam(ps, 1, (String) null);
         strategy.bindParam(ps, 1, (Date) null);
         strategy.bindParam(ps, 1, (GregorianCalendar) null);
         strategy.bindParam(ps, 1, (LocalDate) null);
         strategy.bindParam(ps, 1, (LocalDateTime) null);

         strategy.bindParam(ps, 1, 1);
         strategy.bindParam(ps, 1, true);
         strategy.bindParam(ps, 1, BigDecimal.ONE);
         strategy.bindParam(ps, 1, "hello".getBytes(StandardCharsets.UTF_8));
         strategy.bindParam(ps, 1, new Timestamp(ctMillis));
         strategy.bindParam(ps, 1, "hello");
         strategy.bindParam(ps, 1, new Date(ctMillis));
         strategy.bindParam(ps, 1, new GregorianCalendar());
         strategy.bindParam(ps, 1, LocalDate.now());
         strategy.bindParam(ps, 1, LocalDateTime.now());

         ////////////////////////////////////////////////////////////////////////////////////////////////
         // originally longs were being downgraded to int when binding, so, verify that doesn't happen //
         ////////////////////////////////////////////////////////////////////////////////////////////////
      }
   }



   /*******************************************************************************
    ** DATE and TIME are wall-clock values, independent of the JVM/connection zones.
    *******************************************************************************/
   @Test
   void testDateAndTimeAcrossConnectionAndHostTimeZones() throws Exception
   {
      TimeZone original = TimeZone.getDefault();
      BaseRDBMSActionStrategy strategy = new BaseRDBMSActionStrategy();
      LocalDate date = LocalDate.of(2024, 2, 29);
      LocalTime time = LocalTime.of(23, 59, 58);
      try
      {
         for(String zone : new String[] { "America/Chicago", "Asia/Tokyo" })
         {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            try(Connection connection = getConnection())
            {
               QueryManager.executeUpdate(connection, "SET TIME ZONE 'UTC'");
               QueryManager.executeUpdate(connection, "DELETE FROM test_table");
               try(PreparedStatement statement = connection.prepareStatement("INSERT INTO test_table (date_col, time_col) VALUES (?, ?)"))
               {
                  strategy.bindParamObject(statement, 1, date);
                  strategy.bindParamObject(statement, 2, time);
                  statement.executeUpdate();
               }
               try(PreparedStatement statement = connection.prepareStatement("SELECT date_col, time_col FROM test_table");
                  ResultSet result = statement.executeQuery())
               {
                  assertTrue(result.next());
                  assertEquals(date.toString(), result.getString(1), zone);
                  assertEquals(time.toString(), result.getString(2), zone);
                  assertEquals(date, ValueUtils.getValueAsLocalDate(strategy.getFieldValueFromResultSet(QFieldType.DATE, result, 1)), zone);
                  assertEquals(time, strategy.getFieldValueFromResultSet(QFieldType.TIME, result, 2), zone);
               }
            }
         }
      }
      finally
      {
         TimeZone.setDefault(original);
      }
   }
}
