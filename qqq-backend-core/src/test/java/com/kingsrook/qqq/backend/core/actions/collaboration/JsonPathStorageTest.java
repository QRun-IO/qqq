/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.JsonPathStorage;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.utils.BackendQueryFilterUtils;
import com.kingsrook.qqq.backend.core.utils.JsonFieldUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Runtime custom field metadata and storage.
 *******************************************************************************/
public class JsonPathStorageTest extends BaseTest
{
   private QTableMetaData table;



   /*******************************************************************************
    ** A dedicated memory table keeps tests independent of application fixtures.
    *******************************************************************************/
   @BeforeEach
   void setUp()
   {
      table = new QTableMetaData().withName("customTest").withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(field("score", QFieldType.INTEGER)).withField(field("note", QFieldType.STRING));
      QContext.getQInstance().addTable(table);
   }



   /*******************************************************************************
    ** Shared JSON column with a deliberately ignored backendName.
    *******************************************************************************/
   public static QFieldMetaData field(String name, QFieldType type)
   {
      return (new QFieldMetaData(name, type).withBackendName("ignored")
         .withBackendStorage(new JsonPathStorage().withColumnName("qqq_custom_fields").withKey(name)));
   }



   /*******************************************************************************
    ** CRUD, typed filters, ordering and null removal through the public actions.
    *******************************************************************************/
   @Test
   void memoryCrudAndFilters() throws Exception
   {
      var inserted = new InsertAction().execute(new InsertInput().withTableName(table.getName()).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("score", "12").withValue("note", "first"),
         new QRecord().withValue("id", 2).withValue("score", 3).withValue("note", "second"))));
      assertEquals(12, inserted.getRecords().get(0).getValue("score"));
      assertFalse(inserted.getRecords().get(0).getValues().containsKey("qqq_custom_fields"));
      assertEquals(1, query(new QFilterCriteria("score", QCriteriaOperator.EQUALS, 12)).size());
      assertEquals(2, query(new QFilterCriteria("score", QCriteriaOperator.IN, 12, 3)).size());
      assertEquals(1, query(new QFilterCriteria("score", QCriteriaOperator.GREATER_THAN, 5)).size());
      var ordered = QueryAction.execute(table.getName(), new QQueryFilter().withOrderBy(new QFilterOrderBy("score")));
      assertEquals(2, ordered.get(0).getValue("id"));
      update(new QRecord().withValue("id", 1).withValue("score", 21));
      assertEquals("first", query(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1)).get(0).getValue("note"));
      update(new QRecord().withValue("id", 1).withValue("note", null));
      QRecord result = query(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1)).get(0);
      assertNull(result.getValue("note"));
      assertEquals(21, result.getValue("score"));
      assertEquals(1, query(new QFilterCriteria("note", QCriteriaOperator.IS_BLANK)).size());
   }



   /*******************************************************************************
    ** Maps use copy-on-write, preserving other keys and snapshots.
    *******************************************************************************/
   @Test
   void nestedStorageAndTypes()
   {
      QRecord record = new QRecord();
      JsonFieldUtils.write(record, field("score", QFieldType.INTEGER), 4);
      JsonFieldUtils.write(record, field("note", QFieldType.STRING), "a");
      QRecord snapshot = new QRecord(record);
      JsonFieldUtils.write(record, field("note", QFieldType.STRING), null);
      assertEquals(Map.of("score", 4), record.getValue("qqq_custom_fields"));
      assertEquals("a", JsonFieldUtils.read(snapshot, field("note", QFieldType.STRING), ""));
      Map<QFieldType, java.io.Serializable> values = Map.of(QFieldType.LONG, 12345678900L, QFieldType.DECIMAL, new BigDecimal("1.25"),
         QFieldType.BOOLEAN, true, QFieldType.DATE, LocalDate.of(2026, 10, 13), QFieldType.DATE_TIME, Instant.parse("2026-10-13T12:00:00Z"));
      values.forEach((type, value) ->
      {
         QFieldMetaData field = field("typed", type);
         JsonFieldUtils.write(record, field, value);
         assertEquals(value, JsonFieldUtils.read(record, field, ""));
      });
   }



   /*******************************************************************************
    ** The shared filter utility also understands raw nested storage and qualified names.
    *******************************************************************************/
   @Test
   void filtersAndSortsNestedStorage() throws Exception
   {
      QRecord first = new QRecord().withTableName(table.getName());
      QRecord second = new QRecord().withTableName(table.getName());
      JsonFieldUtils.write(first, table.getField("score"), 20);
      JsonFieldUtils.write(second, table.getField("score"), 2);
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("customTest.score", QCriteriaOperator.GREATER_THAN, 10))
         .withOrderBy(new QFilterOrderBy("score"));
      JoinsContext joins = new JoinsContext(table.getName(), List.of(), filter);
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, joins, first));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, joins, second));
      List<QRecord> records = new ArrayList<>(List.of(first, second));
      BackendQueryFilterUtils.sortRecordList(joins, filter, records);
      assertSame(second, records.get(0));
   }



   /*******************************************************************************
    ** Validation rejects unsafe paths, unsupported types and physical column collisions.
    *******************************************************************************/
   @Test
   void validatesAndClonesStorage()
   {
      QFieldMetaData field = field("valid", QFieldType.STRING);
      QFieldMetaData clone = field.clone();
      assertNotSame(field.getBackendStorage(), clone.getBackendStorage());
      ((JsonPathStorage) clone.getBackendStorage()).setKey("changed");
      assertEquals("valid", ((JsonPathStorage) field.getBackendStorage()).getKey());
      for(QFieldType type : QFieldType.values())
      {
         QInstanceValidator validator = new QInstanceValidator();
         QFieldMetaData candidate = field("valid", type);
         new QInstanceEnricher(QContext.getQInstance()).enrichField(candidate);
         validator.validateField(QContext.getQInstance(), "test", Optional.of(table), candidate);
         assertEquals(List.of(QFieldType.TIME, QFieldType.HTML, QFieldType.PASSWORD, QFieldType.BLOB).contains(type), !validator.getErrors().isEmpty(), type.name());
      }
      for(String key : List.of("Bad", "a-b", "a' OR true", "a".repeat(42), ""))
      {
         ((JsonPathStorage) field.getBackendStorage()).setKey(key);
         QInstanceValidator validator = new QInstanceValidator();
         validator.validateField(QContext.getQInstance(), "test", Optional.of(table), field);
         assertTrue(validator.getErrors().stream().anyMatch(error -> error.contains("Invalid JSON storage key")));
      }
      ((JsonPathStorage) field.getBackendStorage()).setKey("valid");
      table.withField(new QFieldMetaData("raw", QFieldType.TEXT).withBackendName("qqq_custom_fields"));
      QInstanceValidator validator = new QInstanceValidator();
      validator.validateField(QContext.getQInstance(), "test", Optional.of(table), field);
      assertTrue(validator.getErrors().stream().anyMatch(error -> error.contains("must not be a normal field")));
   }



   /*******************************************************************************
    ** Query helper.
    *******************************************************************************/
   private List<QRecord> query(QFilterCriteria criterion) throws Exception
   {
      return (QueryAction.execute(table.getName(), new QQueryFilter(criterion)));
   }



   /*******************************************************************************
    ** Update helper.
    *******************************************************************************/
   private void update(QRecord record) throws Exception
   {
      new UpdateAction().execute(new UpdateInput().withTableName(table.getName()).withRecords(List.of(record)));
   }
}

