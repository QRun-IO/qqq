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
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Bounded Memory/H2 aggregate parity over identical canonical FieldLab values.
 ** The Memory variant reuses its complete metadata with no new physical schema.
 *******************************************************************************/
class SampleAggregateMemoryParityTest
{
   private static final String NATIVE_TABLE = "fieldLab";
   private static final String MEMORY_TABLE = "fieldLabAggregateMemory";
   private static final String MEMORY_BACKEND = "aggregateMemory";
   private QInstance instance;
   private List<List<String>> nativeBefore;
   private List<Map<String, String>> memoryBefore;
   private String metadataBefore;



   /*******************************************************************************
    ** Seed native H2 independently, then use the supported Memory store INSERT
    ** API to copy actual typed values, preserving null/zero and duplicate groups.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      MemoryRecordStore.fullReset();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addBackend(new QBackendMetaData().withName(MEMORY_BACKEND).withBackendType(MemoryBackendModule.class));
      instance.addTable(instance.getTable(NATIVE_TABLE).clone().withName(MEMORY_TABLE).withBackendName(MEMORY_BACKEND));
      QContext.init(instance, new QSession());
      List<QRecord> records = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(6, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,decimal_value,unchanged_value) VALUES "
            + "(1,'Alpha',10,1.5000,'red'),(2,'Beta',20,2.5000,'red'),"
            + "(3,'Gamma',30,NULL,'blue'),(4,'Delta',40,NULL,'blue'),"
            + "(5,'Epsilon',0,NULL,'green'),(6,'Zeta',NULL,NULL,'green')"));
         try(ResultSet rows = statement.executeQuery("SELECT id,name,long_value,decimal_value,unchanged_value FROM field_lab ORDER BY id"))
         {
            while(rows.next())
            {
               records.add(new QRecord().withValue("id", rows.getInt("id")).withValue("name", rows.getString("name"))
                  .withValue("longValue", rows.getObject("long_value", Long.class)).withValue("decimalValue", rows.getBigDecimal("decimal_value"))
                  .withValue("unchangedValue", rows.getString("unchanged_value")));
            }
         }
      }
      List<QRecord> inserted = MemoryRecordStore.getInstance().insert(new InsertInput(MEMORY_TABLE).withRecords(records), true);
      assertEquals(6, inserted.size());
      assertTrue(inserted.stream().allMatch(record -> record.getErrors().isEmpty()));
      memoryBefore = memoryRows();
      List<List<String>> memoryValues = memoryBefore.stream().map(record ->
      {
         List<String> row = new ArrayList<>();
         for(String field : List.of("id", "name", "longValue", "decimalValue", "unchangedValue"))
         {
            row.add(record.get(field));
         }
         return row;
      }).toList();
      assertEquals(sql("SELECT id,name,long_value,decimal_value,unchanged_value FROM field_lab ORDER BY id"), memoryValues);
      nativeBefore = nativeRows();
      metadataBefore = metadata();
   }



   /*******************************************************************************
    ** Both stores and original metadata remain unchanged; public Memory reset and
    ** statistics APIs restore fixture state after any failed assertion.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         assertAll(() -> assertEquals(nativeBefore, nativeRows()), () -> assertEquals(memoryBefore, memoryRows()),
            () -> assertEquals(metadataBefore, metadata()));
      }
      finally
      {
         MemoryRecordStore.fullReset();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Populated scalar and grouped controls establish identical values before
    ** empty-input and pagination edge cases are exercised.
    *******************************************************************************/
   @Test
   void testPopulatedScalarAndGroupsMatchNativeSql() throws Exception
   {
      assertBoth(table -> input(table, count("id"), sum("longValue"), new Aggregate("longValue", AggregateOperator.MIN),
         new Aggregate("longValue", AggregateOperator.MAX), new Aggregate("longValue", AggregateOperator.AVG), sum("decimalValue")),
         "SELECT COUNT(id),SUM(long_value),MIN(long_value),MAX(long_value),AVG(long_value),SUM(decimal_value) FROM field_lab");
      assertBoth(this::groups, "SELECT unchanged_value,SUM(long_value),COUNT(id) FROM field_lab GROUP BY unchanged_value ORDER BY SUM(long_value) DESC,unchanged_value");
   }



   /*******************************************************************************
    ** USER-only active READ metadata reaches both native implementations. Memory's
    ** supported input statistics additionally observe source propagation directly.
    *******************************************************************************/
   @Test
   void testUserReadPolicyAndSystemSourceControl() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("aggregateParityOwner"));
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(ReadPolicy.class));
      QContext.setQSession(new QSession().withSecurityKeyValue("aggregateParityOwner", 20L));
      assertAll(
         () -> assertReadSource(QInputSource.USER, "SELECT COUNT(id),SUM(long_value) FROM field_lab WHERE long_value=20"),
         () -> assertReadSource(QInputSource.SYSTEM, "SELECT COUNT(id),SUM(long_value) FROM field_lab"));
   }



   /*******************************************************************************
    ** Only the owned operand column changes. Full unrelated SQL columns and
    ** Memory fields are checked before refreshing the read-only test baseline.
    *******************************************************************************/
   private void seedOperands(String fieldName, String columnName, List<? extends Serializable> values) throws Exception
   {
      assertEquals(6, values.size());
      int columnIndex;
      try(Connection connection = connection(); PreparedStatement update = connection.prepareStatement("UPDATE field_lab SET " + columnName + "=? WHERE id=?");
         Statement statement = connection.createStatement(); ResultSet metadata = statement.executeQuery("SELECT * FROM field_lab WHERE 1=0"))
      {
         columnIndex = metadata.findColumn(columnName) - 1;
         if(fieldName.equals("decimalValue"))
         {
            assertEquals(20, metadata.getMetaData().getPrecision(columnIndex + 1));
            assertEquals(4, metadata.getMetaData().getScale(columnIndex + 1));
         }
         for(int index = 0; index < values.size(); index++)
         {
            if(fieldName.equals("decimalValue"))
            {
               update.setBigDecimal(1, (BigDecimal) values.get(index));
            }
            else if(fieldName.equals("longValue"))
            {
               update.setObject(1, values.get(index), Types.BIGINT);
            }
            else
            {
               update.setBytes(1, (byte[]) values.get(index));
            }
            update.setInt(2, index + 1);
            assertEquals(1, update.executeUpdate());
         }
      }
      List<QRecord> changes = new ArrayList<>();
      List<Map<String, String>> expectedMemory = new ArrayList<>();
      for(int index = 0; index < values.size(); index++)
      {
         Serializable operand = values.get(index);
         changes.add(new QRecord().withValue("id", index + 1).withValue(fieldName, operand instanceof byte[] bytes ? bytes.clone() : operand));
         Map<String, String> expectedRow = new LinkedHashMap<>(memoryBefore.get(index));
         expectedRow.put(fieldName, value(operand));
         expectedMemory.add(expectedRow);
      }
      List<QRecord> updated = MemoryRecordStore.getInstance().update(new UpdateInput(MEMORY_TABLE).withRecords(changes), true);
      assertEquals(6, updated.size());
      assertTrue(updated.stream().allMatch(record -> record.getErrors().isEmpty()));
      assertStoredOperands(fieldName, columnName, values);
      List<List<String>> changedNative = nativeRows();
      for(int index = 0; index < nativeBefore.size(); index++)
      {
         List<String> oldOtherColumns = new ArrayList<>(nativeBefore.get(index));
         List<String> newOtherColumns = new ArrayList<>(changedNative.get(index));
         oldOtherColumns.remove(columnIndex);
         newOtherColumns.remove(columnIndex);
         assertEquals(oldOtherColumns, newOtherColumns);
      }
      assertEquals(expectedMemory, memoryRows());
      nativeBefore = changedNative;
      memoryBefore = expectedMemory;
   }



   /*******************************************************************************
    ** Decimal equality is exact, including the intentionally different Memory
    ** scales. Binary storage is compared by bytes, never a display conversion.
    *******************************************************************************/
   private void assertStoredOperands(String fieldName, String columnName, List<? extends Serializable> expected) throws Exception
   {
      List<QRecord> memory = MemoryRecordStore.getInstance().query(new QueryInput(MEMORY_TABLE).withInputSource(QInputSource.SYSTEM).withShouldFetchHeavyFields(true)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id"))));
      assertEquals(expected.size(), memory.size());
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id," + columnName + " FROM field_lab ORDER BY id"))
      {
         int index = 0;
         while(rows.next())
         {
            assertEquals(index + 1, rows.getInt("id"));
            assertEquals(index + 1, memory.get(index).getValueInteger("id"));
            Object memoryValue = memory.get(index).getValue(fieldName);
            if(fieldName.equals("decimalValue"))
            {
               BigDecimal expectedValue = (BigDecimal) expected.get(index);
               assertEquals(expectedValue == null ? null : expectedValue.setScale(4), rows.getBigDecimal(columnName));
               assertEquals(expectedValue, memoryValue);
            }
            else if(fieldName.equals("longValue"))
            {
               assertEquals(expected.get(index), rows.getObject(columnName, Long.class));
               assertEquals(expected.get(index), memoryValue);
            }
            else
            {
               byte[] expectedValue = (byte[]) expected.get(index);
               assertArrayEquals(expectedValue, rows.getBytes(columnName));
               if(expectedValue == null)
               {
                  assertNull(memoryValue);
               }
               else
               {
                  assertArrayEquals(expectedValue, assertInstanceOf(byte[].class, memoryValue));
               }
            }
            index++;
         }
         assertEquals(expected.size(), index);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<BigDecimal> decimalOperands(String... values)
   {
      return Arrays.stream(values).map(value -> value == null ? null : new BigDecimal(value)).toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateInput decimalStatistics(String table)
   {
      return input(table, count("decimalValue"), sum("decimalValue"), new Aggregate("decimalValue", AggregateOperator.MIN),
         new Aggregate("decimalValue", AggregateOperator.MAX), new Aggregate("decimalValue", AggregateOperator.AVG));
   }



   /*******************************************************************************
    ** A public integer aggregate orders complete groups without ordering BLOBs.
    *******************************************************************************/
   private AggregateInput typedGroups(String table, GroupBy group)
   {
      Aggregate minimumId = new Aggregate("id", AggregateOperator.MIN);
      return input(table, minimumId, count("id")).withGroupBy(group)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(minimumId)));
   }



   /*******************************************************************************
    ** Native SQL is the independent public result oracle, not another Aggregate.
    *******************************************************************************/
   private void assertReadSource(QInputSource source, String query) throws Exception
   {
      List<List<String>> expected = sql(query);
      AggregateInput nativeInput = input(NATIVE_TABLE, count("id"), sum("longValue")).withInputSource(source);
      assertResult(nativeInput, new AggregateAction().execute(nativeInput), expected);
      MemoryRecordStore.resetStatistics();
      MemoryRecordStore.setCollectStatistics(true);
      try
      {
         AggregateInput memoryInput = input(MEMORY_TABLE, count("id"), sum("longValue")).withInputSource(source);
         AggregateOutput memoryOutput = new AggregateAction().execute(memoryInput);
         List<AbstractActionInput> queries = MemoryRecordStore.getActionInputs().get(QueryInput.class);
         assertAll(
            () -> assertResult(memoryInput, memoryOutput, expected),
            () ->
            {
               assertEquals(1, queries.size());
               assertEquals(source, assertInstanceOf(QueryInput.class, queries.get(0)).getInputSource());
            });
      }
      finally
      {
         MemoryRecordStore.setCollectStatistics(false);
      }
   }



   /*******************************************************************************
    ** Execute each adapter independently, so one mismatch does not skip the other.
    *******************************************************************************/
   private void assertBoth(Function<String, AggregateInput> request, String query) throws Exception
   {
      List<List<String>> expected = sql(query);
      assertAll(
         () ->
         {
            AggregateInput input = request.apply(NATIVE_TABLE);
            assertResult(input, new AggregateAction().execute(input), expected);
         },
         () ->
         {
            AggregateInput input = request.apply(MEMORY_TABLE);
            assertResult(input, new AggregateAction().execute(input), expected);
         });
   }



   /*******************************************************************************
    ** Exact descriptor maps and complete ordered tuples are required.
    *******************************************************************************/
   private void assertResult(AggregateInput input, AggregateOutput output, List<List<String>> expected)
   {
      List<List<String>> actual = new ArrayList<>();
      for(AggregateResult result : output.getResults())
      {
         assertEquals(new LinkedHashSet<>(input.getAggregates()), result.getAggregateValues().keySet());
         assertEquals(new LinkedHashSet<>(input.getGroupBys()), result.getGroupByValues().keySet());
         List<String> row = new ArrayList<>();
         input.getGroupBys().forEach(group ->
         {
            Serializable groupedValue = result.getGroupByValue(group);
            assertNumericOrBinaryType(group.getType(), groupedValue);
            row.add(value(groupedValue));
         });
         input.getAggregates().forEach(aggregate ->
         {
            Serializable aggregateValue = result.getAggregateValue(aggregate);
            QFieldType resultType = aggregate.getFieldType();
            if(resultType == null)
            {
               resultType = input.getTable().getField(aggregate.getFieldName()).getType();
               if(aggregate.getOperator() == AggregateOperator.COUNT || aggregate.getOperator() == AggregateOperator.COUNT_DISTINCT)
               {
                  resultType = QFieldType.INTEGER;
               }
               else if(aggregate.getOperator() == AggregateOperator.AVG && (resultType == QFieldType.INTEGER || resultType == QFieldType.LONG))
               {
                  resultType = QFieldType.DECIMAL;
               }
            }
            assertNumericOrBinaryType(resultType, aggregateValue);
            row.add(value(aggregateValue));
         });
         actual.add(row);
      }
      assertEquals(expected, actual);
   }



   /*******************************************************************************
    ** Formatting the oracle must not accept a numeric or binary-looking STRING.
    *******************************************************************************/
   private void assertNumericOrBinaryType(QFieldType type, Serializable value)
   {
      if(value == null)
      {
         return;
      }
      if(type == QFieldType.DECIMAL)
      {
         assertInstanceOf(BigDecimal.class, value);
      }
      else if(type == QFieldType.BLOB)
      {
         assertInstanceOf(byte[].class, value);
      }
      else if(type == QFieldType.INTEGER || type == QFieldType.LONG)
      {
         assertInstanceOf(Number.class, value);
      }
   }



   /*******************************************************************************
    ** Memory's public native read API verifies every stored field value unchanged.
    *******************************************************************************/
   private List<Map<String, String>> memoryRows() throws Exception
   {
      List<Map<String, String>> result = new ArrayList<>();
      for(QRecord record : MemoryRecordStore.getInstance().query(new QueryInput(MEMORY_TABLE).withInputSource(QInputSource.SYSTEM).withShouldFetchHeavyFields(true)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")))))
      {
         Map<String, String> row = new LinkedHashMap<>();
         record.getValues().forEach((field, stored) -> row.put(field, value(stored)));
         result.add(row);
      }
      return result;
   }



   /*******************************************************************************
    ** All columns in the actual H2 table are preserved, including unseeded NULLs.
    *******************************************************************************/
   private List<List<String>> nativeRows() throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM field_lab ORDER BY id"))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int index = 1; index <= result.getMetaData().getColumnCount(); index++)
            {
               row.add(result.getMetaData().getColumnType(index) == Types.BLOB ? value(result.getBytes(index)) : result.getString(index));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Queries only use fixed fixture expressions and numeric/string results.
    *******************************************************************************/
   private List<List<String>> sql(String query) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int index = 1; index <= result.getMetaData().getColumnCount(); index++)
            {
               row.add(value(result.getMetaData().getColumnType(index) == Types.BLOB ? result.getBytes(index) : result.getObject(index)));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Only backend/name differ; the complete canonical field model is reused.
    *******************************************************************************/
   private String metadata()
   {
      return JsonUtils.toJson(List.of(instance.getTable(NATIVE_TABLE), instance.getTable(MEMORY_TABLE)));
   }



   /*******************************************************************************
    ** Numeric scale is normalized without equating NULL and zero.
    *******************************************************************************/
   private static String value(Object value)
   {
      if(value instanceof byte[] bytes)
      {
         return Base64.getEncoder().encodeToString(bytes);
      }
      return value instanceof Number ? new BigDecimal(value.toString()).stripTrailingZeros().toPlainString() : value == null ? null : value.toString();
   }



   /*******************************************************************************
    ** Request instances are fresh for each adapter and use explicit source policy.
    *******************************************************************************/
   private AggregateInput input(String table, Aggregate... aggregates)
   {
      return new AggregateInput(table).withAggregates(List.of(aggregates)).withInputSource(QInputSource.USER);
   }



   /*******************************************************************************
    ** The highest sum occurs in blue, although red's native ids come first.
    *******************************************************************************/
   private AggregateInput groups(String table)
   {
      GroupBy group = new GroupBy(QFieldType.STRING, "unchangedValue");
      Aggregate sum = sum("longValue");
      return input(table, sum, count("id")).withGroupBy(group)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(sum, false)).withOrderBy(new QFilterOrderByGroupBy(group)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Aggregate count(String field)
   {
      return new Aggregate(field, AggregateOperator.COUNT);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Aggregate sum(String field)
   {
      return new Aggregate(field, AggregateOperator.SUM);
   }



   /*******************************************************************************
    ** All native connections belong to this fixture.
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    ** Active-only READ policy isolates source/metadata propagation from base SQL.
    *******************************************************************************/
   public static class ReadPolicy implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !(NATIVE_TABLE.equals(input.getTableName()) || MEMORY_TABLE.equals(input.getTableName())))
         {
            return input.getTable();
         }
         return input.getTable().clone().withRecordSecurityLock(new RecordSecurityLock().withFieldName("longValue").withSecurityKeyType("aggregateParityOwner")
            .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      }
   }
}
