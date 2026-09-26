/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.mongodb.fieldfunctions;


import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Integration tests for MongoDB field functions using testcontainers.
 **
 ** Test data (same as PostgreSQL tests):
 ** Darin:  birthDate=1980-05-31 (Saturday => ISO 6), firstName length = 5
 ** James:  birthDate=1980-05-15 (Thursday => ISO 4), firstName length = 5
 ** Tim:    birthDate=1976-05-28 (Friday   => ISO 5), firstName length = 3
 ** Tyler:  birthDate=null,                           firstName length = 5
 ** Garret: birthDate=1981-01-01 (Thursday => ISO 4), firstName length = 6
 *******************************************************************************/
class MongoDBFieldFunctionIntegrationTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      primeTestDatabase();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void primeTestDatabase() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      insertInput.setRecords(List.of(
         new QRecord().withValue("seqNo", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff").withValue("birthDate", LocalDate.parse("1980-05-31")).withValue("email", "darin.kelkhoff@gmail.com").withValue("isEmployed", true).withValue("annualSalary", 25000).withValue("daysWorked", 27).withValue("homeTown", "Chester"),
         new QRecord().withValue("seqNo", 2).withValue("firstName", "James").withValue("lastName", "Maes").withValue("birthDate", LocalDate.parse("1980-05-15")).withValue("email", "jmaes@mmltholdings.com").withValue("isEmployed", true).withValue("annualSalary", 26000).withValue("daysWorked", 124).withValue("homeTown", "Chester"),
         new QRecord().withValue("seqNo", 3).withValue("firstName", "Tim").withValue("lastName", "Chamberlain").withValue("birthDate", LocalDate.parse("1976-05-28")).withValue("email", "tchamberlain@mmltholdings.com").withValue("isEmployed", false).withValue("annualSalary", null).withValue("daysWorked", 0).withValue("homeTown", "Decatur"),
         new QRecord().withValue("seqNo", 4).withValue("firstName", "Tyler").withValue("lastName", "Samples").withValue("birthDate", null).withValue("email", "tsamples@mmltholdings.com").withValue("isEmployed", true).withValue("annualSalary", 30000).withValue("daysWorked", 99).withValue("homeTown", "Texas"),
         new QRecord().withValue("seqNo", 5).withValue("firstName", "Garret").withValue("lastName", "Richardson").withValue("birthDate", LocalDate.parse("1981-01-01")).withValue("email", "grichardson@mmltholdings.com").withValue("isEmployed", true).withValue("annualSalary", 1000000).withValue("daysWorked", 232).withValue("homeTown", null)
      ));
      new InsertAction().execute(insertInput);
   }



   /*******************************************************************************
    ** Add virtual fields to the person table for testing.
    *******************************************************************************/
   private void addVirtualFieldsToPersonTable()
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON);

      table.withVirtualField(new QVirtualFieldMetaData("firstNameLength", QFieldType.INTEGER)
         .withIsQuerySelectable(true)
         .withIsQueryCriteria(true)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("firstName")));

      table.withVirtualField(new QVirtualFieldMetaData("birthDayOfWeek", QFieldType.INTEGER)
         .withIsQuerySelectable(true)
         .withIsQueryCriteria(true)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
            .withFieldName("birthDate")));
   }



   /*******************************************************************************
    ** Test 1: Query with virtual fields in SELECT — verify computed values
    *******************************************************************************/
   @Test
   void testQueryWithVirtualFieldsInSelect() throws QException
   {
      addVirtualFieldsToPersonTable();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput.setFilter(new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("seqNo")));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      List<QRecord> records = queryOutput.getRecords();
      assertEquals(5, records.size());

      // Darin: firstName length = 5, birthDate 1980-05-31 = Saturday = ISO 6
      assertEquals(5, records.get(0).getValueInteger("firstNameLength"));
      assertEquals(6, records.get(0).getValueInteger("birthDayOfWeek"));

      // James: firstName length = 5, birthDate 1980-05-15 = Thursday = ISO 4
      assertEquals(5, records.get(1).getValueInteger("firstNameLength"));
      assertEquals(4, records.get(1).getValueInteger("birthDayOfWeek"));

      // Tim: firstName length = 3, birthDate 1976-05-28 = Friday = ISO 5
      assertEquals(3, records.get(2).getValueInteger("firstNameLength"));
      assertEquals(5, records.get(2).getValueInteger("birthDayOfWeek"));

      // Tyler: firstName length = 5, birthDate = null
      assertEquals(5, records.get(3).getValueInteger("firstNameLength"));
      assertNull(records.get(3).getValue("birthDayOfWeek"));

      // Garret: firstName length = 6, birthDate 1981-01-01 = Thursday = ISO 4
      assertEquals(6, records.get(4).getValueInteger("firstNameLength"));
      assertEquals(4, records.get(4).getValueInteger("birthDayOfWeek"));
   }



   /*******************************************************************************
    ** Test 2: Query with virtual field in WHERE — filter on firstNameLength > 4
    *******************************************************************************/
   @Test
   void testQueryWithVirtualFieldInWhere() throws QException
   {
      addVirtualFieldsToPersonTable();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("firstNameLength", QCriteriaOperator.GREATER_THAN, 4))
         .withOrderBy(new QFilterOrderBy("seqNo")));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      List<QRecord> records = queryOutput.getRecords();

      // Should return: Darin(5), James(5), Tyler(5), Garret(6) — NOT Tim(3)
      assertEquals(4, records.size());
      assertEquals("Darin", records.get(0).getValueString("firstName"));
      assertEquals("James", records.get(1).getValueString("firstName"));
      assertEquals("Tyler", records.get(2).getValueString("firstName"));
      assertEquals("Garret", records.get(3).getValueString("firstName"));
   }



   /*******************************************************************************
    ** Test 3: Query with virtual field in ORDER BY — sort by birthDayOfWeek
    *******************************************************************************/
   @Test
   void testQueryWithVirtualFieldInOrderBy() throws QException
   {
      addVirtualFieldsToPersonTable();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.IS_NOT_BLANK))
         .withOrderBy(new QFilterOrderBy("birthDayOfWeek"))
         .withOrderBy(new QFilterOrderBy("seqNo")));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      List<QRecord> records = queryOutput.getRecords();

      // Exclude Tyler (null birthDate)
      assertEquals(4, records.size());

      // ISO weekday order: James(4), Garret(4), Tim(5), Darin(6)
      assertEquals("James", records.get(0).getValueString("firstName"));
      assertEquals("Garret", records.get(1).getValueString("firstName"));
      assertEquals("Tim", records.get(2).getValueString("firstName"));
      assertEquals("Darin", records.get(3).getValueString("firstName"));
   }



   /*******************************************************************************
    ** Test 4: Aggregate with virtual field in GROUP BY
    *******************************************************************************/
   @Test
   void testAggregateWithVirtualFieldInGroupBy() throws QException
   {
      addVirtualFieldsToPersonTable();

      GroupBy groupByDayOfWeek = new GroupBy(QFieldType.INTEGER, "birthDayOfWeek");

      AggregateInput aggregateInput = new AggregateInput();
      aggregateInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      aggregateInput.withAggregate(new Aggregate("seqNo", AggregateOperator.COUNT));
      aggregateInput.withGroupBy(groupByDayOfWeek);
      aggregateInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.IS_NOT_BLANK))
         .withOrderBy(new QFilterOrderByGroupBy(groupByDayOfWeek, true)));

      AggregateOutput aggregateOutput = new AggregateAction().execute(aggregateInput);
      List<AggregateResult> results = aggregateOutput.getResults();

      assertFalse(results.isEmpty());

      // Should have 3 groups: ISO 4 (Thu - James+Garret), ISO 5 (Fri - Tim), ISO 6 (Sat - Darin)
      assertEquals(3, results.size());

      // Ordered ascending by day-of-week
      AggregateResult thursday = results.get(0);
      assertEquals(4, thursday.getGroupByValue(groupByDayOfWeek));
      assertEquals(2, thursday.getAggregateValues().values().iterator().next());

      AggregateResult friday = results.get(1);
      assertEquals(5, friday.getGroupByValue(groupByDayOfWeek));
      assertEquals(1, friday.getAggregateValues().values().iterator().next());

      AggregateResult saturday = results.get(2);
      assertEquals(6, saturday.getGroupByValue(groupByDayOfWeek));
      assertEquals(1, saturday.getAggregateValues().values().iterator().next());
   }



   /*******************************************************************************
    ** Test 5: Query without virtual fields — verify existing behavior is unchanged
    *******************************************************************************/
   @Test
   void testQueryWithoutVirtualFields() throws QException
   {
      // NO virtual fields added — table is in its default state

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Darin")));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      List<QRecord> records = queryOutput.getRecords();

      assertEquals(1, records.size());
      assertEquals("Darin", records.get(0).getValueString("firstName"));
      assertEquals("Kelkhoff", records.get(0).getValueString("lastName"));
      assertNotNull(records.get(0).getValue("id"));
   }



   /*******************************************************************************
    ** Test: Query with virtual field equality in WHERE
    *******************************************************************************/
   @Test
   void testQueryWithVirtualFieldEquals() throws QException
   {
      addVirtualFieldsToPersonTable();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("birthDayOfWeek", QCriteriaOperator.EQUALS, 6))
         .withOrderBy(new QFilterOrderBy("seqNo")));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      List<QRecord> records = queryOutput.getRecords();

      // Only Darin has birthDayOfWeek = 6 (Saturday)
      assertEquals(1, records.size());
      assertEquals("Darin", records.get(0).getValueString("firstName"));
   }

}
