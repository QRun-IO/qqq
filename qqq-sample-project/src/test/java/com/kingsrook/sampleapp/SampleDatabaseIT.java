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
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
