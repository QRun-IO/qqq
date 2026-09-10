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


import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.FieldLabTableMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Field Lab acceptance against the same metadata and database used by the UI.
 *******************************************************************************/
class SampleFieldContractTest
{
   private static final String TABLE = FieldLabTableMetaDataProducer.NAME;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QContext.init(SampleMetaDataProvider.defineTestInstance(), new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAllFieldTypesPersistAndReadBack() throws Exception
   {
      Set<QFieldType> types = QContext.getQInstance().getTable(TABLE).getFields().values().stream().map(field -> field.getType()).collect(Collectors.toSet());
      assertEquals(Set.copyOf(Arrays.asList(QFieldType.values())), types);
      Instant beforeInsert = Instant.now().minusSeconds(1);
      byte[] bytes = new byte[] { 0, 1, 2, 127, -1 };
      QRecord record = insertAndRead(new QRecord().withValue("name", "typed-values")
         .withValue("longValue", Long.MAX_VALUE)
         .withValue("decimalValue", new BigDecimal("1234567890123456.7890"))
         .withValue("booleanValue", false)
         .withValue("dateValue", LocalDate.of(2024, 2, 29))
         .withValue("timeValue", LocalTime.of(23, 59, 58))
         .withValue("textValue", "First line\nSecond line é")
         .withValue("htmlValue", "<p>Sample <strong>HTML</strong></p>")
         .withValue("passwordValue", "synthetic-example-only")
         .withValue("blobValue", bytes));
      assertNotNull(record.getValueInteger("id"));
      assertEquals(Long.MAX_VALUE, record.getValueLong("longValue"));
      assertEquals(new BigDecimal("1234567890123456.7890"), record.getValueBigDecimal("decimalValue"));
      assertEquals(false, record.getValueBoolean("booleanValue"));
      assertEquals(LocalDate.of(2024, 2, 29), record.getValueLocalDate("dateValue"));
      assertEquals(LocalTime.of(23, 59, 58), record.getValueLocalTime("timeValue"));
      assertNotNull(record.getValueInstant("createDate"));
      assertNotNull(record.getValueInstant("modifyDate"));
      assertFalse(record.getValueInstant("createDate").isBefore(beforeInsert));
      assertFalse(record.getValueInstant("createDate").isAfter(Instant.now().plusSeconds(1)));
      assertEquals("First line\nSecond line é", record.getValueString("textValue"));
      assertEquals("<p>Sample <strong>HTML</strong></p>", record.getValueString("htmlValue"));
      assertEquals("************", record.getValueString("passwordValue"));
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         PreparedStatement statement = connection.prepareStatement("SELECT password_value FROM field_lab WHERE id = ?"))
      {
         statement.setInt(1, record.getValueInteger("id"));
         try(ResultSet result = statement.executeQuery())
         {
            assertTrue(result.next());
            assertEquals("synthetic-example-only", result.getString(1));
         }
      }
      assertArrayEquals(bytes, record.getValueByteArray("blobValue"));

      QRecord optional = insertAndRead(new QRecord().withValue("name", "null-values"));
      assertNull(optional.getValueLong("longValue"));
      assertNull(optional.getValueLocalDate("dateValue"));
      assertNull(optional.getValueLocalTime("timeValue"));
      assertNull(optional.getValueByteArray("blobValue"));
      assertEquals(true, optional.getValueBoolean("booleanValue"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLengthPoliciesAndRejectedWrite() throws Exception
   {
      QRecord record = insertAndRead(new QRecord().withValue("name", "length-policies")
         .withValue("truncateValue", "éééééééééé")
         .withValue("ellipsisValue", "abcdefghij")
         .withValue("rejectLongValue", "12345678")
         .withValue("passThroughValue", "abcdefghij"));
      assertEquals("éééééééé", record.getValueString("truncateValue"));
      assertEquals("abcde...", record.getValueString("ellipsisValue"));
      assertEquals("12345678", record.getValueString("rejectLongValue"));
      assertEquals("abcdefghij", record.getValueString("passThroughValue"));
      assertRejected(new QRecord().withValue("name", "invalid-length").withValue("rejectLongValue", "123456789"), "too long");

      QRecord rejectedUpdate = new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord()
         .withValue("id", record.getValueInteger("id")).withValue("rejectLongValue", "123456789"))).getRecords().get(0);
      assertFalse(rejectedUpdate.getErrors().isEmpty());
      assertEquals("12345678", GetAction.execute(TABLE, record.getValueInteger("id")).getValueString("rejectLongValue"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCaseAndWhitespacePolicies() throws Exception
   {
      QRecord input = new QRecord().withValue("name", "normalization");
      for(String field : new String[] { "upperValue", "lowerValue", "unchangedValue", "trimValue", "trimLeftValue", "trimRightValue", "removeSpaceValue" })
      {
         input.setValue(field, " \tAb Çd\n ");
      }
      QRecord record = insertAndRead(input);
      assertEquals(" \tAB ÇD\n ", record.getValueString("upperValue"));
      assertEquals(" \tab çd\n ", record.getValueString("lowerValue"));
      assertEquals(" \tAb Çd\n ", record.getValueString("unchangedValue"));
      assertEquals("Ab Çd", record.getValueString("trimValue"));
      assertEquals("Ab Çd\n ", record.getValueString("trimLeftValue"));
      assertEquals(" \tAb Çd", record.getValueString("trimRightValue"));
      assertEquals("AbÇd", record.getValueString("removeSpaceValue"));

      QRecord blank = insertAndRead(new QRecord().withValue("name", "blank-normalization").withValue("trimValue", " \t ").withValue("removeSpaceValue", " \n "));
      assertEquals("", blank.getValueString("trimValue"));
      assertEquals("", blank.getValueString("removeSpaceValue"));
      assertNull(blank.getValueString("upperValue"));
      assertNull(blank.getValueString("lowerValue"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNumericBoundsAndClipping() throws Exception
   {
      for(String value : new String[] { "-1", "0", "100", "101" })
      {
         QRecord record = insertAndRead(new QRecord().withValue("name", "clip-" + value).withValue("clippedValue", new BigDecimal(value)));
         BigDecimal expected = new BigDecimal(value).compareTo(BigDecimal.ZERO) <= 0 ? new BigDecimal("0.01") : new BigDecimal("99.99");
         assertEquals(expected, record.getValueBigDecimal("clippedValue"));
      }
      assertEquals(new BigDecimal("12.34"), insertAndRead(new QRecord().withValue("name", "inside").withValue("clippedValue", new BigDecimal("12.34"))).getValueBigDecimal("clippedValue"));
      assertEquals(new BigDecimal("0.00"), insertAndRead(new QRecord().withValue("name", "minimum").withValue("boundedValue", 0)).getValueBigDecimal("boundedValue"));
      assertEquals(new BigDecimal("100.00"), insertAndRead(new QRecord().withValue("name", "maximum").withValue("boundedValue", 100)).getValueBigDecimal("boundedValue"));
      assertRejected(new QRecord().withValue("name", "below").withValue("boundedValue", -1), "too small");
      assertRejected(new QRecord().withValue("name", "above").withValue("boundedValue", 101), "too large");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRequiredUniqueDefaultsAndSparseUpdates() throws Exception
   {
      assertRejected(new QRecord(), "required");
      QRecord first = insertAndRead(new QRecord().withValue("name", "unique-name").withValue("textValue", "preserve"));
      assertRejected(new QRecord().withValue("name", "unique-name"), "already exists");
      Instant created = first.getValueInstant("createDate");
      QRecord update = new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord()
         .withValue("id", first.getValueInteger("id")).withValue("booleanValue", false))).getRecords().get(0);
      assertTrue(update.getErrors().isEmpty());
      QRecord updated = GetAction.execute(TABLE, first.getValueInteger("id"));
      assertEquals("preserve", updated.getValueString("textValue"));
      assertEquals(false, updated.getValueBoolean("booleanValue"));
      assertEquals(created, updated.getValueInstant("createDate"));
      assertFalse(updated.getValueInstant("modifyDate").isBefore(first.getValueInstant("modifyDate")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUtcStorageAcrossHostTimeZones() throws Exception
   {
      TimeZone original = TimeZone.getDefault();
      try
      {
         for(String zone : new String[] { "UTC", "America/Chicago", "Asia/Tokyo" })
         {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            Instant instant = Instant.parse("2026-03-08T07:59:59Z");
            QRecord record = insertAndRead(new QRecord().withValue("name", zone).withValue("dateTimeValue", instant));
            assertEquals(instant, record.getValueInstant("dateTimeValue"), zone);
         }
      }
      finally
      {
         TimeZone.setDefault(original);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insertAndRead(QRecord record) throws Exception
   {
      QRecord inserted = new InsertAction().execute(new InsertInput(TABLE).withRecord(record)).getRecords().get(0);
      assertTrue(inserted.getErrors().isEmpty(), () -> inserted.getErrors().toString());
      return GetAction.execute(TABLE, inserted.getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertRejected(QRecord record, String message) throws Exception
   {
      Integer before = CountAction.execute(TABLE, null);
      QRecord rejected = new InsertAction().execute(new InsertInput(TABLE).withRecord(record)).getRecords().get(0);
      assertFalse(rejected.getErrors().isEmpty());
      assertTrue(rejected.getErrors().toString().toLowerCase().contains(message), () -> rejected.getErrors().toString());
      assertEquals(before, CountAction.execute(TABLE, null));
   }
}
