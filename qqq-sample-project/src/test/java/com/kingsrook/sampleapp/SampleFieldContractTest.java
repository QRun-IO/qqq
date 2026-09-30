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


import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.WhiteSpaceBehavior;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.FieldLabTableMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    ** A length boundary must not persist half of a supplementary character.
    *******************************************************************************/
   @Test
   void testSupplementaryUnicodeLengthBoundaries() throws Exception
   {
      String supplementary = "\uD834\uDD1E"; // Musical symbol G clef is represented by a surrogate pair.
      QRecord record = insertAndRead(new QRecord().withValue("name", "unicode-boundaries")
         .withValue("truncateValue", "1234567" + supplementary + "X")
         .withValue("ellipsisValue", supplementary.repeat(5)));
      QRecord updated = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord()
         .withValue("id", record.getValueInteger("id")).withValue("truncateValue", "ABCDEFG" + supplementary + "X")
         .withValue("ellipsisValue", "XY" + supplementary.repeat(4)))).get(0);
      assertTrue(updated.getErrors().isEmpty());
      QRecord persistedUpdate = GetAction.execute(TABLE, record.getValueInteger("id"));
      assertAll(
         () -> assertEquals("1234567", record.getValueString("truncateValue")),
         () -> assertEquals(supplementary.repeat(2) + "...", record.getValueString("ellipsisValue")),
         () -> assertEquals("ABCDEFG", persistedUpdate.getValueString("truncateValue")),
         () -> assertEquals("XY" + supplementary + "...", persistedUpdate.getValueString("ellipsisValue")),
         () ->
         {
            QContext.getQInstance().getTable(TABLE).getField("ellipsisValue").setMaxLength(2);
            QRecord shortened = insertAndRead(new QRecord().withValue("name", "short-ellipsis").withValue("ellipsisValue", "abcdef"));
            assertEquals("..", shortened.getValueString("ellipsisValue"));
         });
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
      assertStoredStrings(record.getValueInteger("id"), "upper_value", " \tAB ÇD\n ", "lower_value", " \tab çd\n ",
         "unchanged_value", " \tAb Çd\n ", "trim_value", "Ab Çd", "trim_left_value", "Ab Çd\n ", "trim_right_value", " \tAb Çd", "remove_space_value", "AbÇd");

      QRecord blank = insertAndRead(new QRecord().withValue("name", "blank-normalization").withValue("trimValue", " \t ").withValue("removeSpaceValue", " \n "));
      assertEquals("", blank.getValueString("trimValue"));
      assertEquals("", blank.getValueString("removeSpaceValue"));
      assertNull(blank.getValueString("upperValue"));
      assertNull(blank.getValueString("lowerValue"));

      QRecord updated = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord()
         .withValue("id", record.getValueInteger("id")).withValue("upperValue", "mixed")
         .withValue("trimValue", " \t updated \n ").withValue("trimLeftValue", " \tLeft \n ")
         .withValue("trimRightValue", " \tRight \n ").withValue("removeSpaceValue", " \tN o n e \n ")
         .withValue("unchangedValue", " \tAs Is \n "))).get(0);
      assertTrue(updated.getErrors().isEmpty());
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          PreparedStatement statement = connection.prepareStatement("SELECT upper_value, trim_value, lower_value FROM field_lab WHERE id = ?"))
      {
         statement.setInt(1, record.getValueInteger("id"));
         try(ResultSet values = statement.executeQuery())
         {
            assertTrue(values.next());
            assertEquals("MIXED", values.getString(1));
            assertEquals("updated", values.getString(2));
            assertEquals(record.getValueString("lowerValue"), values.getString(3));
         }
      }
      assertStoredStrings(record.getValueInteger("id"), "trim_left_value", "Left \n ", "trim_right_value", " \tRight",
         "remove_space_value", "None", "unchanged_value", " \tAs Is \n ");
      QRecord lowerUpdate = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord()
         .withValue("id", record.getValueInteger("id")).withValue("lowerValue", "UPDated Ç"))).get(0);
      assertTrue(lowerUpdate.getErrors().isEmpty());
      assertStoredStrings(record.getValueInteger("id"), "lower_value", "updated ç", "upper_value", "MIXED", "trim_value", "updated");

      QRecord nulls = new QRecord().withValue("id", record.getValueInteger("id"));
      for(String field : new String[] { "upperValue", "lowerValue", "unchangedValue", "trimValue", "trimLeftValue", "trimRightValue", "removeSpaceValue" })
      {
         nulls.setValue(field, null);
      }
      assertTrue(UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(nulls)).get(0).getErrors().isEmpty());
      assertStoredStrings(record.getValueInteger("id"), "upper_value", null, "lower_value", null, "unchanged_value", null,
         "trim_value", null, "trim_left_value", null, "trim_right_value", null, "remove_space_value", null);
   }



   /*******************************************************************************
    ** Locale and Unicode whitespace semantics are part of the existing policies.
    *******************************************************************************/
   @Test
   void testLocaleAndUnicodeWhitespaceSemantics() throws Exception
   {
      Locale original = Locale.getDefault();
      try
      {
         Locale.setDefault(Locale.forLanguageTag("tr-TR"));
         String text = "\u2003A\u00A0B\u2003"; // EM SPACE surrounds the text; NO-BREAK SPACE separates A and B.
         QRecord record = insertAndRead(new QRecord().withValue("name", "locale-whitespace")
            .withValue("upperValue", "iI").withValue("lowerValue", "iI")
            .withValue("trimValue", text).withValue("trimLeftValue", text)
            .withValue("trimRightValue", text).withValue("removeSpaceValue", text));
         assertStoredStrings(record.getValueInteger("id"), "upper_value", "İI", "lower_value", "iı",
            "trim_value", text, "trim_left_value", "A\u00A0B\u2003", "trim_right_value", "\u2003A\u00A0B", "remove_space_value", "A\u00A0B"); // NO-BREAK SPACE survives all policies.
      }
      finally
      {
         Locale.setDefault(original);
      }
   }



   /*******************************************************************************
    ** Values written outside QQQ are normalized on reads without changing storage.
    *******************************************************************************/
   @Test
   void testReadNormalizationDoesNotRewriteStorage() throws Exception
   {
      QRecord record = insertAndRead(new QRecord().withValue("name", "raw-normalization"));
      int id = record.getValueInteger("id");
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          PreparedStatement statement = connection.prepareStatement("UPDATE field_lab SET upper_value=?, lower_value=?, unchanged_value=?, trim_value=?, trim_left_value=?, trim_right_value=?, remove_space_value=? WHERE id=?"))
      {
         for(int i = 1; i <= 7; i++)
         {
            statement.setString(i, " \tMiXeD Ç\n ");
         }
         statement.setInt(8, id);
         assertEquals(1, statement.executeUpdate());
      }
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, id));
      for(QRecord read : List.of(GetAction.execute(TABLE, id), QueryAction.execute(TABLE, filter).get(0)))
      {
         assertEquals(" \tMIXED Ç\n ", read.getValueString("upperValue"));
         assertEquals(" \tmixed ç\n ", read.getValueString("lowerValue"));
         assertEquals(" \tMiXeD Ç\n ", read.getValueString("unchangedValue"));
         assertEquals("MiXeD Ç", read.getValueString("trimValue"));
         assertEquals("MiXeD Ç\n ", read.getValueString("trimLeftValue"));
         assertEquals(" \tMiXeD Ç", read.getValueString("trimRightValue"));
         assertEquals("MiXeDÇ", read.getValueString("removeSpaceValue"));
      }
      assertStoredStrings(id, "upper_value", " \tMiXeD Ç\n ", "lower_value", " \tMiXeD Ç\n ", "unchanged_value", " \tMiXeD Ç\n ",
         "trim_value", " \tMiXeD Ç\n ", "trim_left_value", " \tMiXeD Ç\n ", "trim_right_value", " \tMiXeD Ç\n ", "remove_space_value", " \tMiXeD Ç\n ");
   }



   /*******************************************************************************
    ** Filter operands use the same policies without mutating reusable filters.
    *******************************************************************************/
   @Test
   void testFilterNormalizationAcrossReadActions() throws Exception
   {
      QRecord first = insertAndRead(new QRecord().withValue("name", "filter-first").withValue("upperValue", "MiXeD")
         .withValue("lowerValue", "MiXeD").withValue("trimValue", " Mixed ").withValue("trimLeftValue", "\tMixed ")
         .withValue("trimRightValue", " Mixed\t").withValue("removeSpaceValue", "\tMi xed ")
         .withValue("unchangedValue", " Mixed ").withValue("normalizedKey", " code-a "));
      QRecord second = insertAndRead(new QRecord().withValue("name", "filter-second").withValue("normalizedKey", "code-b"));
      int id = first.getValueInteger("id");
      Map<String, String> operands = Map.of("upperValue", "mixed", "lowerValue", "MIXED", "trimValue", " \tMixed \n",
         "trimLeftValue", "\nMixed ", "trimRightValue", " Mixed\n", "removeSpaceValue", " M i x e d ",
         "unchangedValue", " Mixed ", "normalizedKey", " \tCoDe-A\n");
      Aggregate count = new Aggregate("id", AggregateOperator.COUNT);
      for(Map.Entry<String, String> operand : operands.entrySet())
      {
         QFilterCriteria criteria = new QFilterCriteria(operand.getKey(), QCriteriaOperator.EQUALS, List.of(operand.getValue()));
         QQueryFilter filter = new QQueryFilter();
         filter.setCriteria(List.of(criteria));
         assertEquals(List.of(id), QueryAction.execute(TABLE, filter).stream().map(row -> row.getValueInteger("id")).toList(), operand.getKey());
         assertEquals(1, CountAction.execute(TABLE, filter), operand.getKey());
         Number aggregateCount = (Number) new AggregateAction().execute(new AggregateInput(TABLE).withFilter(filter).withAggregate(count))
            .getResults().get(0).getAggregateValue(count);
         assertEquals(1, aggregateCount.intValue(), operand.getKey());
         assertEquals(List.of(operand.getValue()), criteria.getValues(), "The caller's operand must remain unchanged");
      }
      assertEquals(0, CountAction.execute(TABLE, new QQueryFilter(new QFilterCriteria("unchangedValue", QCriteriaOperator.EQUALS, "Mixed"))));
      assertEquals(1, CountAction.execute(TABLE, new QQueryFilter(new QFilterCriteria("upperValue", QCriteriaOperator.IS_BLANK))));
      assertEquals(0, CountAction.execute(TABLE, new QQueryFilter(new QFilterCriteria("normalizedKey", QCriteriaOperator.EQUALS, "absent"))));

      QFilterCriteria in = new QFilterCriteria("normalizedKey", QCriteriaOperator.IN, List.of(" code-a ", " code-b "));
      QQueryFilter alternatives = new QQueryFilter();
      alternatives.setCriteria(List.of(in));
      assertEquals(Set.of(id, second.getValueInteger("id")), QueryAction.execute(TABLE, alternatives).stream().map(row -> row.getValueInteger("id")).collect(Collectors.toSet()));
      QQueryFilter nested = new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "filter-first"))
         .withSubFilters(List.of(alternatives));
      assertEquals(List.of(id), QueryAction.execute(TABLE, nested).stream().map(row -> row.getValueInteger("id")).toList());
      assertEquals(List.of(" code-a ", " code-b "), in.getValues());

      Map<String, Serializable> key = Map.of("normalizedKey", " code-a ");
      assertEquals(id, new GetAction().executeForRecord(new GetInput(TABLE).withUniqueKey(key)).getValueInteger("id"));
      assertEquals(" code-a ", key.get("normalizedKey"));
      QContext.getQInstance().getTable(TABLE).setPrimaryKeyField("normalizedKey");
      assertEquals(id, GetAction.execute(TABLE, " code-a ").getValueInteger("id"));
      QContext.getQInstance().getTable(TABLE).setPrimaryKeyField("id");

      QQueryFilter qualified = new QQueryFilter(new QFilterCriteria(TABLE + ".normalizedKey", QCriteriaOperator.EQUALS, " code-a "));
      QQueryFilter normalized = ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getQInstance(), QContext.getQInstance().getTable(TABLE), qualified, null);
      assertEquals(List.of("CODE-A"), normalized.getCriteria().get(0).getValues());
      assertEquals(List.of(" code-a "), qualified.getCriteria().get(0).getValues());
      QQueryFilter mixedTypes = new QQueryFilter(new QFilterCriteria("normalizedKey", QCriteriaOperator.IN, Arrays.asList(null, 12, " code-a ")));
      normalized = ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getQInstance(), QContext.getQInstance().getTable(TABLE), mixedTypes, null);
      assertEquals(Arrays.asList(null, 12, "CODE-A"), normalized.getCriteria().get(0).getValues());
      normalized = ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getQInstance(), QContext.getQInstance().getTable(TABLE), qualified, Set.of(CaseChangeBehavior.TO_UPPER_CASE));
      assertEquals(List.of("code-a"), normalized.getCriteria().get(0).getValues());
   }



   /*******************************************************************************
    ** Normalization supports string-like fields; invalid configuration is rejected.
    *******************************************************************************/
   @Test
   void testNormalizationConfigurationBoundaries() throws Exception
   {
      Set<QFieldType> supportedTypes = Set.of(QFieldType.STRING, QFieldType.TEXT, QFieldType.HTML, QFieldType.PASSWORD);
      for(FieldBehavior<?> behavior : List.of(CaseChangeBehavior.TO_UPPER_CASE, WhiteSpaceBehavior.TRIM))
      {
         for(QFieldType type : QFieldType.values())
         {
            QFieldMetaData field = new QFieldMetaData("normalization", type).withBehavior(behavior);
            assertEquals(supportedTypes.contains(type), behavior.validateBehaviorConfiguration(null, field).isEmpty(), type.name());
         }
         QInstance invalid = SampleMetaDataProvider.defineTestInstance();
         invalid.getTable(TABLE).getField("decimalValue").withBehavior(behavior);
         QInstanceValidationException exception = assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().validate(invalid));
         assertTrue(exception.getMessage().contains("decimalValue"));
      }
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
      for(int value : new int[] { -1, 0, 100, 101 })
      {
         assertRejected(new QRecord().withValue("name", "exclusive-" + value).withValue("exclusiveBoundedValue", value), value <= 0 ? "too small" : "too large");
         QRecord clipped = insertAndRead(new QRecord().withValue("name", "inclusive-" + value).withValue("inclusiveClippedValue", value));
         assertEquals(value <= 0 ? new BigDecimal("0.00") : new BigDecimal("100.00"), clipped.getValueBigDecimal("inclusiveClippedValue"));
      }
      QRecord inside = insertAndRead(new QRecord().withValue("name", "inside-all-ranges")
         .withValue("exclusiveBoundedValue", new BigDecimal("12.34")).withValue("inclusiveClippedValue", new BigDecimal("12.34")));
      assertEquals(new BigDecimal("12.34"), inside.getValueBigDecimal("exclusiveBoundedValue"));
      assertEquals(new BigDecimal("12.34"), inside.getValueBigDecimal("inclusiveClippedValue"));
      QRecord updated = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord()
         .withValue("id", inside.getValueInteger("id")).withValue("boundedValue", 42)
         .withValue("clippedValue", 101).withValue("inclusiveClippedValue", -1))).get(0);
      assertTrue(updated.getErrors().isEmpty());
      QRecord rejected = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord()
         .withValue("id", inside.getValueInteger("id")).withValue("boundedValue", 101)
         .withValue("exclusiveBoundedValue", 0))).get(0);
      assertFalse(rejected.getErrors().isEmpty());
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          PreparedStatement statement = connection.prepareStatement("SELECT bounded_value, clipped_value, inclusive_clipped_value, exclusive_bounded_value FROM field_lab WHERE id = ?"))
      {
         statement.setInt(1, inside.getValueInteger("id"));
         try(ResultSet values = statement.executeQuery())
         {
            assertTrue(values.next());
            assertEquals(new BigDecimal("42.00"), values.getBigDecimal(1));
            assertEquals(new BigDecimal("99.99"), values.getBigDecimal(2));
            assertEquals(new BigDecimal("0.00"), values.getBigDecimal(3));
            assertEquals(new BigDecimal("12.34"), values.getBigDecimal(4));
         }
      }
      QRecord missing = insertAndRead(new QRecord().withValue("name", "missing-ranges"));
      for(String field : new String[] { "boundedValue", "exclusiveBoundedValue", "clippedValue", "inclusiveClippedValue" })
      {
         assertNull(missing.getValue(field));
      }
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
    ** Defaults are persisted; explicit user IDs survive, timestamps follow policy.
    *******************************************************************************/
   @Test
   void testDynamicDefaults() throws Exception
   {
      QRecord anonymous = insertAndRead(new QRecord().withValue("name", "no-user-default"));
      assertNull(anonymous.getValue("userIdValue"));
      QContext.getQSession().setUser(new QUser().withIdReference("sample-user"));
      Instant before = Instant.now().minusSeconds(1);
      QRecord generated = insertAndRead(new QRecord().withValue("name", "generated-defaults")
         .withValue("createDate", Instant.EPOCH).withValue("modifyDate", Instant.EPOCH));
      assertEquals("sample-user", generated.getValueString("userIdValue"));
      assertFalse(generated.getValueInstant("createDate").isBefore(before));
      assertFalse(generated.getValueInstant("modifyDate").isBefore(before));
      Instant created = generated.getValueInstant("createDate");
      QRecord explicit = insertAndRead(new QRecord().withValue("name", "explicit-user").withValue("userIdValue", "explicit-sample-user"));
      assertEquals("explicit-sample-user", explicit.getValueString("userIdValue"));
      QRecord update = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord()
         .withValue("id", generated.getValueInteger("id")).withValue("textValue", "updated")
         .withValue("userIdValue", "explicit-updated-user").withValue("modifyDate", Instant.EPOCH))).get(0);
      assertTrue(update.getErrors().isEmpty());
      QRecord updated = GetAction.execute(TABLE, generated.getValueInteger("id"));
      assertEquals(created, updated.getValueInstant("createDate"));
      assertFalse(updated.getValueInstant("modifyDate").isBefore(before));
      assertEquals("explicit-updated-user", updated.getValueString("userIdValue"));
      QContext.getQSession().setUser(new QUser().withIdReference("second-sample-user"));
      update = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord()
         .withValue("id", generated.getValueInteger("id")).withValue("userIdValue", null))).get(0);
      assertTrue(update.getErrors().isEmpty());
      updated = GetAction.execute(TABLE, generated.getValueInteger("id"));
      assertEquals("second-sample-user", updated.getValueString("userIdValue"));
      assertEquals(created, updated.getValueInstant("createDate"));
      assertStoredStrings(generated.getValueInteger("id"), "user_id_value", "second-sample-user");
   }



   /*******************************************************************************
    ** DATE defaults use calendar dates; NONE and omit-modify preserve explicit data.
    *******************************************************************************/
   @Test
   void testDateDefaultsAndModifyOptOut() throws Exception
   {
      LocalDate before = LocalDate.now();
      QRecord record = insertAndRead(new QRecord().withValue("name", "date-defaults")
         .withValue("createdDay", LocalDate.of(1900, 1, 1)).withValue("modifiedDay", LocalDate.of(1900, 1, 1))
         .withValue("manualDateTime", Instant.EPOCH));
      LocalDate after = LocalDate.now();
      assertTrue(List.of(before, after).contains(record.getValueLocalDate("createdDay")));
      assertTrue(List.of(before, after).contains(record.getValueLocalDate("modifiedDay")));
      assertEquals(Instant.EPOCH, record.getValueInstant("manualDateTime"));
      int id = record.getValueInteger("id");
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          PreparedStatement statement = connection.prepareStatement("UPDATE field_lab SET created_day=DATE '1900-01-01', modified_day=DATE '1900-01-01', modify_date=TIMESTAMP '1900-01-01 00:00:00' WHERE id=?"))
      {
         statement.setInt(1, id);
         assertEquals(1, statement.executeUpdate());
      }
      Instant beforeUpdate = Instant.now().minusSeconds(1);
      QRecord update = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", id).withValue("textValue", "sparse date update"))).get(0);
      assertTrue(update.getErrors().isEmpty());
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          PreparedStatement statement = connection.prepareStatement("SELECT created_day, modified_day, modify_date, manual_date_time FROM field_lab WHERE id=?"))
      {
         statement.setInt(1, id);
         try(ResultSet values = statement.executeQuery())
         {
            assertTrue(values.next());
            assertEquals(LocalDate.of(1900, 1, 1), values.getObject(1, LocalDate.class));
            assertTrue(List.of(before, LocalDate.now()).contains(values.getObject(2, LocalDate.class)));
            assertFalse(values.getObject(3, LocalDateTime.class).toInstant(ZoneOffset.UTC).isBefore(beforeUpdate));
            assertEquals(LocalDateTime.of(1970, 1, 1, 0, 0), values.getObject(4, LocalDateTime.class));
         }
      }
      LocalDate explicitDay = LocalDate.of(2001, 2, 3);
      Instant explicitTime = Instant.parse("2001-02-03T04:05:06Z");
      update = UpdateAction.executeForRecords(new UpdateInput(TABLE).withOmitModifyDateUpdate(true).withRecord(new QRecord().withValue("id", id)
         .withValue("modifiedDay", explicitDay).withValue("modifyDate", explicitTime).withValue("manualDateTime", null))).get(0);
      assertTrue(update.getErrors().isEmpty());
      update = UpdateAction.executeForRecords(new UpdateInput(TABLE).withOmitModifyDateUpdate(true).withRecord(new QRecord().withValue("id", id).withValue("textValue", "preserve date update"))).get(0);
      assertTrue(update.getErrors().isEmpty());
      QRecord read = GetAction.execute(TABLE, id);
      assertEquals(explicitDay, read.getValueLocalDate("modifiedDay"));
      assertEquals(explicitTime, read.getValueInstant("modifyDate"));
      assertNull(read.getValue("manualDateTime"));
      assertStoredStrings(id, "modified_day", "2001-02-03", "manual_date_time", null);
      update = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", id)
         .withValue("createdDay", null).withValue("modifiedDay", null))).get(0);
      assertTrue(update.getErrors().isEmpty());
      read = GetAction.execute(TABLE, id);
      assertNull(read.getValue("createdDay"));
      assertTrue(List.of(before, LocalDate.now()).contains(read.getValueLocalDate("modifiedDay")));
      assertStoredStrings(id, "created_day", null);
      update = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", id).withValue("createdDay", explicitDay))).get(0);
      assertTrue(update.getErrors().isEmpty());
      assertStoredStrings(id, "created_day", "2001-02-03");
   }



   /*******************************************************************************
    ** DATE defaults use the host calendar even when its date differs from UTC.
    *******************************************************************************/
   @Test
   void testDateDefaultsAcrossHostCalendarBoundaries() throws Exception
   {
      TimeZone original = TimeZone.getDefault();
      try
      {
         for(String zone : new String[] { "Pacific/Kiritimati", "Etc/GMT+12" })
         {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            LocalDate before = LocalDate.now();
            QRecord record = insertAndRead(new QRecord().withValue("name", "date-default-" + zone));
            LocalDate after = LocalDate.now();
            assertTrue(List.of(before, after).contains(record.getValueLocalDate("createdDay")), zone);
            assertTrue(List.of(before, after).contains(record.getValueLocalDate("modifiedDay")), zone);
            assertStoredStrings(record.getValueInteger("id"), "created_day", record.getValueLocalDate("createdDay").toString(),
               "modified_day", record.getValueLocalDate("modifiedDay").toString());
         }
      }
      finally
      {
         TimeZone.setDefault(original);
      }
   }



   /*******************************************************************************
    ** The enricher flag applies to that run; explicit NONE survives validation.
    *******************************************************************************/
   @Test
   void testConfiguredInferenceAndExplicitNone() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      new QInstanceEnricher(instance).withConfigAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate(false).enrich();
      assertNull(instance.getTable(TABLE).getField("createDate").getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
      assertNull(instance.getTable(TABLE).getField("modifyDate").getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
      assertEquals(DynamicDefaultValueBehavior.CREATE_DATE, instance.getTable(TABLE).getField("createdDay").getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
      instance.getTable(TABLE).getField("createDate").withBehavior(DynamicDefaultValueBehavior.NONE);
      instance.getTable(TABLE).getField("modifyDate").withBehavior(DynamicDefaultValueBehavior.NONE);
      QContext.init(instance, new QSession());
      assertEquals(DynamicDefaultValueBehavior.NONE, instance.getTable(TABLE).getField("createDate").getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
      assertEquals(DynamicDefaultValueBehavior.NONE, instance.getTable(TABLE).getField("modifyDate").getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
      QRecord absent = insertAndRead(new QRecord().withValue("name", "no-inferred-dates"));
      assertStoredStrings(absent.getValueInteger("id"), "create_date", null, "modify_date", null, "manual_date_time", null);
      QRecord explicit = insertAndRead(new QRecord().withValue("name", "explicit-dates").withValue("createDate", Instant.EPOCH).withValue("modifyDate", Instant.EPOCH));
      QRecord update = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", explicit.getValueInteger("id")).withValue("textValue", "sparse update"))).get(0);
      assertTrue(update.getErrors().isEmpty());
      QRecord read = GetAction.execute(TABLE, explicit.getValueInteger("id"));
      assertEquals(Instant.EPOCH, read.getValueInstant("createDate"));
      assertEquals(Instant.EPOCH, read.getValueInstant("modifyDate"));
   }



   /*******************************************************************************
    ** Reads preserve stored user IDs; writes default missing or blank input fields.
    *******************************************************************************/
   @Test
   void testUserDefaultsAcrossReadsAndSparseUpdates() throws Exception
   {
      QRecord anonymous = insertAndRead(new QRecord().withValue("name", "anonymous-read"));
      int anonymousId = anonymous.getValueInteger("id");
      QContext.getQSession().setUser(new QUser().withIdReference("reader"));
      assertNull(GetAction.execute(TABLE, anonymousId).getValue("userIdValue"));
      assertNull(QueryAction.execute(TABLE, new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, anonymousId))).get(0).getValue("userIdValue"));
      assertStoredStrings(anonymousId, "user_id_value", null);

      for(String blank : new String[] { null, "", " \t " })
      {
         QRecord record = insertAndRead(new QRecord().withValue("name", "blank-user-" + String.valueOf(blank)).withValue("userIdValue", blank));
         assertStoredStrings(record.getValueInteger("id"), "user_id_value", "reader");
      }
      QRecord explicit = insertAndRead(new QRecord().withValue("name", "sparse-user").withValue("userIdValue", "original-owner"));
      QContext.getQSession().setUser(new QUser().withIdReference("updater"));
      QRecord update = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", explicit.getValueInteger("id")).withValue("textValue", "unrelated edit"))).get(0);
      assertTrue(update.getErrors().isEmpty());
      assertStoredStrings(explicit.getValueInteger("id"), "user_id_value", "updater");
      QContext.setQSession(new QSession());
      update = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", explicit.getValueInteger("id")).withValue("textValue", "anonymous edit"))).get(0);
      assertTrue(update.getErrors().isEmpty());
      assertStoredStrings(explicit.getValueInteger("id"), "user_id_value", "updater");
      update = UpdateAction.executeForRecords(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", explicit.getValueInteger("id")).withValue("userIdValue", null))).get(0);
      assertTrue(update.getErrors().isEmpty());
      assertStoredStrings(explicit.getValueInteger("id"), "user_id_value", null);
   }



   /*******************************************************************************
    ** Display zones change presentation across DST while stored values stay UTC.
    *******************************************************************************/
   @Test
   void testDateTimeDisplayZonesAndFallback() throws Exception
   {
      String[] instants = { "2026-03-08T07:59:59Z", "2026-03-08T08:00:00Z" };
      String[] expected = { "2026-03-08 01:59:59 AM CST", "2026-03-08 03:00:00 AM CDT" };
      for(int i = 0; i < instants.length; i++)
      {
         Instant instant = Instant.parse(instants[i]);
         QRecord record = insertAndRead(new QRecord().withValue("name", "dst-" + i).withValue("timeZone", "America/Chicago")
            .withValue("fixedZoneDateTime", instant).withValue("recordZoneDateTime", instant));
         QRecord display = new GetAction().executeForRecord(new GetInput(TABLE).withPrimaryKey(record.getValueInteger("id")).withShouldGenerateDisplayValues(true));
         assertEquals(expected[i], display.getDisplayValue("fixedZoneDateTime"));
         assertEquals(expected[i], display.getDisplayValue("recordZoneDateTime"));
         assertEquals(instant, display.getValueInstant("fixedZoneDateTime"));
         assertEquals(instant, display.getValueInstant("recordZoneDateTime"));
      }
      for(String zone : new String[] { null, "invalid/sample-zone" })
      {
         Instant instant = Instant.parse("2026-03-08T08:00:00Z");
         QRecord record = insertAndRead(new QRecord().withValue("name", "fallback-" + zone).withValue("timeZone", zone).withValue("recordZoneDateTime", instant));
         QRecord display = new GetAction().executeForRecord(new GetInput(TABLE).withPrimaryKey(record.getValueInteger("id")).withShouldGenerateDisplayValues(true));
         assertEquals("2026-03-08 08:00:00 AM UTC", display.getDisplayValue("recordZoneDateTime"));
         assertNull(display.getValue("fixedZoneDateTime"));
         assertNull(display.getDisplayValue("fixedZoneDateTime"));
         assertEquals(instant, display.getValueInstant("recordZoneDateTime"));
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
    ** Check storage directly so read behaviors cannot conceal a missing write.
    *******************************************************************************/
   private void assertStoredStrings(int id, String... columnsAndValues) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          PreparedStatement statement = connection.prepareStatement("SELECT * FROM field_lab WHERE id=?"))
      {
         statement.setInt(1, id);
         try(ResultSet values = statement.executeQuery())
         {
            assertTrue(values.next());
            for(int i = 0; i < columnsAndValues.length; i += 2)
            {
               assertEquals(columnsAndValues[i + 1], values.getString(columnsAndValues[i]), columnsAndValues[i]);
            }
         }
      }
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



   /*******************************************************************************
    ** Failed temporal conversion leaves the native rows unchanged for both writes.
    *******************************************************************************/
   @Test
   void testInvalidTemporalInputsDoNotMutateStorage() throws Exception
   {
      QRecord record = insertAndRead(new QRecord().withValue("name", "temporal-control")
         .withValue("dateValue", LocalDate.of(2024, 2, 29)).withValue("timeValue", LocalTime.of(23, 59, 58))
         .withValue("dateTimeValue", Instant.parse("2024-02-29T23:59:58Z")));
      List<List<String>> before = nativeFieldRows();
      for(String field : List.of("dateValue", "timeValue", "dateTimeValue"))
      {
         assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TABLE)
            .withRecord(new QRecord().withValue("name", "invalid-" + field).withValue(field, "not-a-temporal-value"))));
         assertEquals(before, nativeFieldRows());
         assertThrows(QException.class, () -> UpdateAction.executeForRecords(new UpdateInput(TABLE)
            .withRecord(new QRecord().withValue("id", record.getValueInteger("id")).withValue(field, "not-a-temporal-value"))));
         assertEquals(before, nativeFieldRows());
      }
   }



   /*******************************************************************************
    ** Native readback includes all columns, independently of QQQ conversion.
    *******************************************************************************/
   private List<List<String>> nativeFieldRows() throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM field_lab ORDER BY id");
         ResultSet result = statement.executeQuery())
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
