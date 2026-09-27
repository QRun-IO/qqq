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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeIdentifier;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeRegistry;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Field functions on canonical Field Lab data and an owned Memory variant.
 *******************************************************************************/
public class SampleVirtualFieldTest
{
   private static final String MEMORY_TABLE = "virtualFieldLab";
   private static final FieldFunctionTypeIdentifier DOUBLE_LENGTH = () -> "SampleDoubleLength";
   private QInstance instance;
   private List<List<String>> nativeBefore;
   private List<Map<String, Serializable>> memoryBefore;



   /*******************************************************************************
    ** Seed through SQL, then copy native values directly into the owned Memory store.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      MemoryRecordStore.fullReset();
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.setDefaultTimeZoneId("UTC");
      instance.addBackend(new QBackendMetaData().withName("virtualMemory").withBackendType(MemoryBackendModule.class));
      instance.addTable(instance.getTable("fieldLab").clone().withName(MEMORY_TABLE).withBackendName("virtualMemory"));
      QContext.init(instance, new QSession());
      List<QRecord> seeds = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(3, statement.executeUpdate("INSERT INTO field_lab (id,name,text_value,date_value,date_time_value) VALUES "
            + "(101,'Virtual Sunday','AlphaBeta','2026-09-20','2026-09-21 00:30:00'),"
            + "(102,'Virtual Monday','','2026-09-21','2026-09-21 06:30:00'),"
            + "(103,'Virtual Null',NULL,NULL,NULL)"));
         try(ResultSet rows = statement.executeQuery("SELECT id,name,text_value,date_value,date_time_value FROM field_lab ORDER BY id"))
         {
            while(rows.next())
            {
               LocalDateTime timestamp = rows.getObject("date_time_value", LocalDateTime.class);
               seeds.add(new QRecord().withValue("id", rows.getInt("id")).withValue("name", rows.getString("name"))
                  .withValue("textValue", rows.getString("text_value")).withValue("dateValue", rows.getObject("date_value", LocalDate.class))
                  .withValue("dateTimeValue", timestamp == null ? null : timestamp.toInstant(ZoneOffset.UTC)));
            }
         }
      }
      assertEquals(3, seeds.size());
      var inserted = MemoryRecordStore.getInstance().insert(new InsertInput(MEMORY_TABLE).withRecords(seeds), true);
      assertEquals(3, inserted.size());
      assertTrue(inserted.stream().allMatch(record -> record.getErrors().isEmpty()));
      nativeBefore = sqlRows("SELECT * FROM field_lab ORDER BY id");
      memoryBefore = physicalMemoryRows();
   }



   /*******************************************************************************
    ** Computed reads must not change either backing store, even after a refusal.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(instance != null)
         {
            instance.getTable("fieldLab").withVirtualFields(List.of());
            instance.getTable(MEMORY_TABLE).withVirtualFields(List.of());
         }
         if(nativeBefore != null)
         {
            assertEquals(nativeBefore, sqlRows("SELECT * FROM field_lab ORDER BY id"));
            assertEquals(memoryBefore, physicalMemoryRows());
         }
      }
      finally
      {
         QContext.clear();
         MemoryRecordStore.fullReset();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Length and both substring forms use independent native SQL expected values.
    *******************************************************************************/
   @Test
   void testStringFunctionsInMemoryAndNativeQueries() throws Exception
   {
      for(String table : List.of("fieldLab", MEMORY_TABLE))
      {
         addStrings(table);
      }
      new QInstanceValidator().revalidate(instance);
      List<List<String>> expected = sqlRows("SELECT id,CHAR_LENGTH(text_value),SUBSTRING(text_value FROM 2 FOR 3),SUBSTRING(text_value FROM 2) FROM field_lab ORDER BY id");
      assertEquals(List.of(Arrays.asList("101", "9", "lph", "lphaBeta"), Arrays.asList("102", "0", "", ""), Arrays.asList("103", null, null, null)), expected);
      for(String table : List.of("fieldLab", MEMORY_TABLE))
      {
         assertEquals(expected, values(query(table), "id", "textLength", "fragment", "remainder"));
      }
   }



   /*******************************************************************************
    ** Date and zoned timestamp weekdays differ at the explicit UTC/Chicago boundary.
    *******************************************************************************/
   @Test
   void testWeekdayFunctionsAndSundayFirstSorting() throws Exception
   {
      QTableMetaData table = instance.getTable(MEMORY_TABLE);
      table.withVirtualField(virtual("dateWeekday", QFieldType.INTEGER, function(WeekdayOfDateFunction.IDENTIFIER, "dateValue")
         .withArguments(Map.of(WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST, true))));
      table.withVirtualField(virtual("utcWeekday", QFieldType.INTEGER, function(WeekdayOfDateTimeFunction.IDENTIFIER, "dateTimeValue")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC"))));
      table.withVirtualField(virtual("chicagoWeekday", QFieldType.INTEGER, function(WeekdayOfDateTimeFunction.IDENTIFIER, "dateTimeValue")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "America/Chicago"))));
      new QInstanceValidator().revalidate(instance);
      assertEquals(List.of(Arrays.asList("101", "7", "1", "7"), Arrays.asList("102", "1", "1", "1"), Arrays.asList("103", null, null, null)),
         values(query(MEMORY_TABLE), "id", "dateWeekday", "utcWeekday", "chicagoWeekday"));
      List<QRecord> sorted = QueryAction.execute(MEMORY_TABLE, new QQueryFilter(new QFilterCriteria("dateValue", QCriteriaOperator.IS_NOT_BLANK))
         .withOrderBy(new QFilterOrderBy("dateWeekday")));
      assertEquals(List.of(101, 102), sorted.stream().map(record -> record.getValueInteger("id")).toList());
      assertEquals(List.of(List.of("2026-09-20", "2026-09-21 00:30:00"), List.of("2026-09-21", "2026-09-21 06:30:00")),
         sqlRows("SELECT CAST(date_value AS VARCHAR),CAST(date_time_value AS VARCHAR) FROM field_lab WHERE id IN (101,102) ORDER BY id"));
   }



   /*******************************************************************************
    ** Metadata types/references and required or mistyped runtime arguments reject.
    *******************************************************************************/
   @Test
   void testInvalidFunctionMetadataAndArguments() throws Exception
   {
      QTableMetaData table = instance.getTable(MEMORY_TABLE);
      for(FieldFunction invalid : List.of(new FieldFunction().withFieldName("textValue"),
         function(() -> "MissingSampleFunction", "textValue"), function(StringLengthFunction.IDENTIFIER, "missingField"),
         function(StringLengthFunction.IDENTIFIER, "dateValue")))
      {
         table.withVirtualFields(List.of(virtual("invalid", QFieldType.INTEGER, invalid)));
         assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().revalidate(instance));
      }
      FieldFunction substring = function(SubStringFunction.IDENTIFIER, "textValue");
      table.withVirtualFields(List.of(virtual("fragment", QFieldType.STRING, substring)));
      new QInstanceValidator().revalidate(instance);
      assertThrows(QException.class, () -> query(MEMORY_TABLE));
      substring.withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, "not-an-integer"));
      QException invalidArgument = assertThrows(QException.class, () -> query(MEMORY_TABLE));
      assertInstanceOf(QValueException.class, invalidArgument.getCause());
      substring.withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2, SubStringFunction.LENGTH_PARAM, 3));
      assertEquals(List.of(Arrays.asList("101", "lph"), Arrays.asList("102", ""), Arrays.asList("103", null)), values(query(MEMORY_TABLE), "id", "fragment"));
   }



   /*******************************************************************************
    ** Instance-local registration does not imply an adapter exists in every backend.
    *******************************************************************************/
   @Test
   void testRegisteredFunctionAndUnsupportedBackend() throws Exception
   {
      FieldFunctionTypeRegistry.ofOrWithNew(instance).register(DOUBLE_LENGTH, DoubleLengthFunction.class);
      assertNotNull(FieldFunctionTypeRegistry.of(instance).getFieldFunctionType(DOUBLE_LENGTH));
      for(String table : List.of("fieldLab", MEMORY_TABLE))
      {
         instance.getTable(table).withVirtualField(virtual("doubleLength", QFieldType.INTEGER, function(DOUBLE_LENGTH, "textValue")));
      }
      new QInstanceValidator().revalidate(instance);
      assertEquals(List.of(Arrays.asList("101", "18"), Arrays.asList("102", "0"), Arrays.asList("103", null)), values(query(MEMORY_TABLE), "id", "doubleLength"));
      QException exception = assertThrows(QException.class, () -> query("fieldLab"));
      Throwable cause = exception;
      while(cause.getCause() != null)
      {
         cause = cause.getCause();
      }
      assertTrue(cause.getMessage().contains("Missing field function adapter for function [SampleDoubleLength]"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addStrings(String table)
   {
      instance.getTable(table)
         .withVirtualField(virtual("textLength", QFieldType.INTEGER, function(StringLengthFunction.IDENTIFIER, "textValue")))
         .withVirtualField(virtual("fragment", QFieldType.STRING, function(SubStringFunction.IDENTIFIER, "textValue")
            .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2, SubStringFunction.LENGTH_PARAM, 3))))
         .withVirtualField(virtual("remainder", QFieldType.STRING, function(SubStringFunction.IDENTIFIER, "textValue")
            .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private FieldFunction function(FieldFunctionTypeIdentifier identifier, String field)
   {
      return new FieldFunction().withFunctionTypeIdentifier(identifier).withFieldName(field);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QVirtualFieldMetaData virtual(String name, QFieldType type, FieldFunction function)
   {
      return new QVirtualFieldMetaData(name, type).withIsQuerySelectable(true).withIsQueryCriteria(true).withFieldFunction(function);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> query(String table) throws QException
   {
      return QueryAction.execute(table, new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Map<String, Serializable>> physicalMemoryRows() throws QException
   {
      return new QueryAction().execute(new QueryInput(MEMORY_TABLE).withFieldNamesToInclude(Set.of("id", "name", "textValue", "dateValue", "dateTimeValue"))
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")))).getRecords().stream().map(QRecord::getValues).toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> values(List<QRecord> records, String... fields)
   {
      return records.stream().map(record -> Arrays.stream(fields).map(record::getValueString).toList()).toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> sqlRows(String sql) throws Exception
   {
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         List<List<String>> rows = new ArrayList<>();
         while(result.next())
         {
            List<String> values = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               values.add(result.getString(column));
            }
            rows.add(values);
         }
         return rows;
      }
   }



   /*******************************************************************************
    ** A distinct result proves that the registered sample implementation was invoked.
    *******************************************************************************/
   public static class DoubleLengthFunction extends StringLengthFunction
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public FieldFunctionTypeIdentifier getIdentifier()
      {
         return DOUBLE_LENGTH;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Serializable apply(FieldFunction function, QRecord record)
      {
         Integer length = (Integer) super.apply(function, record);
         return length == null ? null : length * 2;
      }
   }
}
