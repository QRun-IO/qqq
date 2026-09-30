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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 **
 *******************************************************************************/
public class RDBMSAggregateActionTest extends RDBMSActionTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws Exception
   {
      super.primeTestDatabase();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUnfilteredNoGroupBy() throws QException
   {
      AggregateInput aggregateInput      = initAggregateRequest();
      Aggregate      countOfId           = new Aggregate("id", AggregateOperator.COUNT);
      Aggregate      sumOfId             = new Aggregate("id", AggregateOperator.SUM);
      Aggregate      averageOfDaysWorked = new Aggregate("daysWorked", AggregateOperator.AVG);
      Aggregate      maxAnnualSalary     = new Aggregate("annualSalary", AggregateOperator.MAX);
      Aggregate      minFirstName        = new Aggregate("firstName", AggregateOperator.MIN);
      Aggregate      countOfBirthDate    = new Aggregate("birthDate", AggregateOperator.COUNT);
      aggregateInput.withAggregate(countOfId);
      aggregateInput.withAggregate(sumOfId);
      aggregateInput.withAggregate(averageOfDaysWorked);
      aggregateInput.withAggregate(maxAnnualSalary);
      aggregateInput.withAggregate(minFirstName);
      aggregateInput.withAggregate(countOfBirthDate);

      AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);

      AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
      assertEquals(5, aggregateResult.getAggregateValue(countOfId));
      assertEquals(15, aggregateResult.getAggregateValue(sumOfId));
      assertEquals(new BigDecimal("96.4"), aggregateResult.getAggregateValue(averageOfDaysWorked));
      assertEquals(new BigDecimal("1000000.00"), aggregateResult.getAggregateValue(maxAnnualSalary));
      assertEquals("Darin", aggregateResult.getAggregateValue(minFirstName));
      assertEquals(4, aggregateResult.getAggregateValue(countOfBirthDate));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @Disabled("Interesting to see effects of all operators on all types, but failures are expected (e.g., avg(string), so not for CI.")
   void testOperatorsCrossTypes()
   {
      List<String> failures = new ArrayList<>();
      for(QFieldMetaData field : QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).getFields().values())
      {
         for(AggregateOperator aggregateOperator : AggregateOperator.values())
         {
            try
            {
               AggregateInput aggregateInput = initAggregateRequest();
               Aggregate      aggregate      = new Aggregate(field.getName(), aggregateOperator);
               aggregateInput.withAggregate(aggregate);
               AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
               AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
               assertNotNull(aggregateResult.getAggregateValue(aggregate));
            }
            catch(Exception e)
            {
               failures.add(ExceptionUtils.getRootException(e).getMessage());
            }
         }
      }

      failures.forEach(System.out::println);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testFilteredNoGroupBy() throws QException
   {
      AggregateInput aggregateInput      = initAggregateRequest();
      Aggregate      countOfId           = new Aggregate("id", AggregateOperator.COUNT);
      Aggregate      sumOfId             = new Aggregate("id", AggregateOperator.SUM);
      Aggregate      averageOfDaysWorked = new Aggregate("daysWorked", AggregateOperator.AVG);
      Aggregate      maxAnnualSalary     = new Aggregate("annualSalary", AggregateOperator.MAX);
      Aggregate      minFirstName        = new Aggregate("firstName", AggregateOperator.MIN);
      aggregateInput.withAggregate(countOfId);
      aggregateInput.withAggregate(sumOfId);
      aggregateInput.withAggregate(averageOfDaysWorked);
      aggregateInput.withAggregate(maxAnnualSalary);
      aggregateInput.withAggregate(minFirstName);

      aggregateInput.setFilter(new QQueryFilter().withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.IN, List.of("Tim", "James"))));
      AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);

      AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
      assertEquals(2, aggregateResult.getAggregateValue(countOfId));
      assertEquals(5, aggregateResult.getAggregateValue(sumOfId));
      assertEquals(new BigDecimal("62.0"), aggregateResult.getAggregateValue(averageOfDaysWorked));
      assertEquals(new BigDecimal("26000.00"), aggregateResult.getAggregateValue(maxAnnualSalary));
      assertEquals("James", aggregateResult.getAggregateValue(minFirstName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUnfilteredWithGroupBy() throws QException
   {
      ////////////////////////////////////////////////////
      // insert a few extra rows from the core data set //
      ////////////////////////////////////////////////////
      insertExtraPersonRecords();

      AggregateInput aggregateInput  = initAggregateRequest();
      Aggregate      countOfId       = new Aggregate("id", AggregateOperator.COUNT);
      Aggregate      sumOfDaysWorked = new Aggregate("daysWorked", AggregateOperator.SUM);
      aggregateInput.withAggregate(countOfId);
      aggregateInput.withAggregate(sumOfDaysWorked);

      GroupBy lastNameGroupBy = new GroupBy(QFieldType.STRING, "lastName", null);
      aggregateInput.withGroupBy(lastNameGroupBy);
      aggregateInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("lastName")));

      AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      {
         AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
         assertEquals("Chamberlain", aggregateResult.getGroupByValue(lastNameGroupBy));
         assertEquals(2, aggregateResult.getAggregateValue(countOfId));
         assertEquals(17, aggregateResult.getAggregateValue(sumOfDaysWorked));
      }
      {
         AggregateResult aggregateResult = aggregateOutput.getResults().get(1);
         assertEquals("Kelkhoff", aggregateResult.getGroupByValue(lastNameGroupBy));
         assertEquals(4, aggregateResult.getAggregateValue(countOfId));
         assertEquals(11364, aggregateResult.getAggregateValue(sumOfDaysWorked));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUnfilteredWithMultiGroupBy() throws QException
   {
      ////////////////////////////////////////////////////
      // insert a few extra rows from the core data set //
      ////////////////////////////////////////////////////
      insertExtraPersonRecords();

      AggregateInput aggregateInput  = initAggregateRequest();
      Aggregate      countOfId       = new Aggregate("id", AggregateOperator.COUNT);
      Aggregate      sumOfDaysWorked = new Aggregate("daysWorked", AggregateOperator.SUM);
      aggregateInput.withAggregate(countOfId);
      aggregateInput.withAggregate(sumOfDaysWorked);

      GroupBy lastNameGroupBy  = new GroupBy(QFieldType.STRING, "lastName", null);
      GroupBy firstNameGroupBy = new GroupBy(QFieldType.STRING, "firstName", null);
      aggregateInput.withGroupBy(lastNameGroupBy);
      aggregateInput.withGroupBy(firstNameGroupBy);

      aggregateInput.setFilter(new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("lastName"))
         .withOrderBy(new QFilterOrderBy("firstName")));

      AggregateOutput           aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      Iterator<AggregateResult> iterator        = aggregateOutput.getResults().iterator();
      AggregateResult           aggregateResult;

      aggregateResult = iterator.next();
      assertEquals("Chamberlain", aggregateResult.getGroupByValue(lastNameGroupBy));
      assertEquals("Donny", aggregateResult.getGroupByValue(firstNameGroupBy));
      assertEquals(1, aggregateResult.getAggregateValue(countOfId));

      aggregateResult = iterator.next();
      assertEquals("Chamberlain", aggregateResult.getGroupByValue(lastNameGroupBy));
      assertEquals("Tim", aggregateResult.getGroupByValue(firstNameGroupBy));
      assertEquals(1, aggregateResult.getAggregateValue(countOfId));

      aggregateResult = iterator.next();
      assertEquals("Kelkhoff", aggregateResult.getGroupByValue(lastNameGroupBy));
      assertEquals("Aaron", aggregateResult.getGroupByValue(firstNameGroupBy));
      assertEquals(1, aggregateResult.getAggregateValue(countOfId));

      aggregateResult = iterator.next();
      assertEquals("Kelkhoff", aggregateResult.getGroupByValue(lastNameGroupBy));
      assertEquals("Darin", aggregateResult.getGroupByValue(firstNameGroupBy));
      assertEquals(2, aggregateResult.getAggregateValue(countOfId));

      aggregateResult = iterator.next();
      assertEquals("Kelkhoff", aggregateResult.getGroupByValue(lastNameGroupBy));
      assertEquals("Trevor", aggregateResult.getGroupByValue(firstNameGroupBy));
      assertEquals(1, aggregateResult.getAggregateValue(countOfId));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testOrderByAggregate() throws QException
   {
      ////////////////////////////////////////////////////
      // insert a few extra rows from the core data set //
      ////////////////////////////////////////////////////
      insertExtraPersonRecords();

      AggregateInput aggregateInput  = initAggregateRequest();
      Aggregate      countOfId       = new Aggregate("id", AggregateOperator.COUNT);
      Aggregate      sumOfDaysWorked = new Aggregate("daysWorked", AggregateOperator.SUM);
      aggregateInput.withAggregate(countOfId);
      // note - don't query this value - just order by it!! aggregateInput.withAggregate(sumOfDaysWorked);

      GroupBy lastNameGroupBy = new GroupBy(QFieldType.STRING, "lastName", null);
      aggregateInput.withGroupBy(lastNameGroupBy);

      aggregateInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(sumOfDaysWorked, false)));

      AggregateOutput           aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      Iterator<AggregateResult> iterator        = aggregateOutput.getResults().iterator();
      AggregateResult           aggregateResult;

      aggregateResult = iterator.next();
      assertEquals("Kelkhoff", aggregateResult.getGroupByValue(lastNameGroupBy));
      assertEquals(4, aggregateResult.getAggregateValue(countOfId));

      aggregateResult = iterator.next();
      assertEquals("Richardson", aggregateResult.getGroupByValue(lastNameGroupBy));
      assertEquals(1, aggregateResult.getAggregateValue(countOfId));

      aggregateResult = iterator.next();
      assertEquals("Maes", aggregateResult.getGroupByValue(lastNameGroupBy));
      assertEquals(1, aggregateResult.getAggregateValue(countOfId));

      aggregateResult = iterator.next();
      assertEquals("Samples", aggregateResult.getGroupByValue(lastNameGroupBy));
      assertEquals(1, aggregateResult.getAggregateValue(countOfId));

      aggregateResult = iterator.next();
      assertEquals("Chamberlain", aggregateResult.getGroupByValue(lastNameGroupBy));
      assertEquals(2, aggregateResult.getAggregateValue(countOfId));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testNoRowsFound() throws QException
   {
      AggregateInput aggregateInput = initAggregateRequest();
      Aggregate      countOfId      = new Aggregate("id", AggregateOperator.COUNT);
      aggregateInput.withAggregate(countOfId);
      aggregateInput.withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, -9)));

      ////////////////////////////////////////////////////////////
      // when there's no group-by, we get a row, but w/ 0 count //
      ////////////////////////////////////////////////////////////
      AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
      assertEquals(0, aggregateResult.getAggregateValue(countOfId));

      /////////////////////////////////////////////////////////////////////////////////////////
      // but re-run w/ a group-by -- then, if no rows are found, there are 0 result objects. //
      /////////////////////////////////////////////////////////////////////////////////////////
      aggregateInput.withGroupBy(new GroupBy(QFieldType.STRING, "lastName", null));
      aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      assertTrue(aggregateOutput.getResults().isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOmsJoinAggregate() throws Exception
   {
      AggregateInput aggregateInput = new AggregateInput();
      Aggregate      sumOfQuantity  = new Aggregate(TestUtils.TABLE_NAME_ORDER_LINE + ".quantity", AggregateOperator.SUM);
      aggregateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      aggregateInput.withAggregate(sumOfQuantity);
      aggregateInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER, TestUtils.TABLE_NAME_ORDER_LINE));

      AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
      assertNull(aggregateResult.getAggregateValue(sumOfQuantity));

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      aggregateResult = aggregateOutput.getResults().get(0);
      assertEquals(43, aggregateResult.getAggregateValue(sumOfQuantity));

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      aggregateResult = aggregateOutput.getResults().get(0);
      assertEquals(33, aggregateResult.getAggregateValue(sumOfQuantity));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOmsJoinGroupBy() throws Exception
   {
      GroupBy groupBy = new GroupBy(QFieldType.STRING, TestUtils.TABLE_NAME_ORDER_LINE + ".sku", null);

      AggregateInput aggregateInput = new AggregateInput();
      Aggregate      sumOfQuantity  = new Aggregate(TestUtils.TABLE_NAME_ORDER_LINE + ".quantity", AggregateOperator.SUM);
      aggregateInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      aggregateInput.withAggregate(sumOfQuantity);
      aggregateInput.withGroupBy(groupBy);
      aggregateInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER, TestUtils.TABLE_NAME_ORDER_LINE));

      AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      assertEquals(0, aggregateOutput.getResults().size());

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      assertSkuQuantity("QM-1", 30, aggregateOutput.getResults(), groupBy);
      assertSkuQuantity("QM-2", 1, aggregateOutput.getResults(), groupBy);
      assertSkuQuantity("QM-3", 1, aggregateOutput.getResults(), groupBy);
      assertSkuQuantity("QRU-1", 3, aggregateOutput.getResults(), groupBy);
      assertSkuQuantity("QRU-2", 2, aggregateOutput.getResults(), groupBy);
      assertSkuQuantity("QD-1", 6, aggregateOutput.getResults(), groupBy);

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertSkuQuantity(String sku, int quantity, List<AggregateResult> results, GroupBy groupBy)
   {
      for(AggregateResult result : results)
      {
         if(result.getGroupByValue(groupBy).equals(sku))
         {
            assertEquals(quantity, result.getAggregateValues().values().iterator().next());
            return;
         }
      }
      fail("Didn't find SKU " + sku + " in aggregate results");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGroupByVirtualField() throws Exception
   {
      QVirtualFieldMetaData firstNameLengthField = new QVirtualFieldMetaData("firstNameLength", QFieldType.INTEGER)
         .withFieldFunction(new FieldFunction()
            .withFieldName("firstName")
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER));

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .withVirtualField(firstNameLengthField);

      AggregateInput aggregateInput = initAggregateRequest();
      Aggregate      countOfId      = new Aggregate("id", AggregateOperator.COUNT);
      aggregateInput.withAggregate(countOfId);

      GroupBy lengthGroupBy = new GroupBy(firstNameLengthField);
      aggregateInput.withGroupBy(lengthGroupBy);

      AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      assertEquals(3, aggregateOutput.getResults().size());

      // find the row with length=5 and verify count=3 (Darin, James, Tyler)
      AggregateResult length5Row = aggregateOutput.getResults().stream()
         .filter(r -> Integer.valueOf(5).equals(r.getGroupByValue(lengthGroupBy)))
         .findFirst().orElseThrow();
      assertEquals(3, length5Row.getAggregateValue(countOfId));

      // find the row with length=3 and verify count=1 (Tim)
      AggregateResult length3Row = aggregateOutput.getResults().stream()
         .filter(r -> Integer.valueOf(3).equals(r.getGroupByValue(lengthGroupBy)))
         .findFirst().orElseThrow();
      assertEquals(1, length3Row.getAggregateValue(countOfId));

      // find the row with length=6 and verify count=1 (Garret)
      AggregateResult length6Row = aggregateOutput.getResults().stream()
         .filter(r -> Integer.valueOf(6).equals(r.getGroupByValue(lengthGroupBy)))
         .findFirst().orElseThrow();
      assertEquals(1, length6Row.getAggregateValue(countOfId));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAggregateOnVirtualField() throws Exception
   {
      QVirtualFieldMetaData firstNameLengthField = new QVirtualFieldMetaData("firstNameLength", QFieldType.INTEGER)
         .withFieldFunction(new FieldFunction()
            .withFieldName("firstName")
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER));

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .withVirtualField(firstNameLengthField);

      // MIN(firstNameLength) with no group-by — should be 3 (Tim)
      {
         AggregateInput aggregateInput = initAggregateRequest();
         Aggregate      minLength      = new Aggregate("firstNameLength", AggregateOperator.MIN);
         aggregateInput.withAggregate(minLength);

         AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
         assertEquals(1, aggregateOutput.getResults().size());
         assertEquals(3, aggregateOutput.getResults().get(0).getAggregateValue(minLength));
      }

      // SUM(firstNameLength) with no group-by — should be 3+5+5+5+6=24
      {
         AggregateInput aggregateInput = initAggregateRequest();
         Aggregate      sumLength      = new Aggregate("firstNameLength", AggregateOperator.SUM);
         aggregateInput.withAggregate(sumLength);

         AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
         assertEquals(1, aggregateOutput.getResults().size());
         assertEquals(24, aggregateOutput.getResults().get(0).getAggregateValue(sumLength));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void insertExtraPersonRecords() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.defineTablePerson().getName());
      insertInput.setRecords(List.of(
         new QRecord().withValue("lastName", "Kelkhoff").withValue("firstName", "Trevor").withValue("email", "tk@kr.com").withValue("daysWorked", 1024),
         new QRecord().withValue("lastName", "Kelkhoff").withValue("firstName", "Darin").withValue("email", "dk2@kr.com").withValue("daysWorked", 314),
         new QRecord().withValue("lastName", "Kelkhoff").withValue("firstName", "Aaron").withValue("email", "ak@kr.com").withValue("daysWorked", 9999),
         new QRecord().withValue("lastName", "Chamberlain").withValue("firstName", "Donny").withValue("email", "dc@kr.com").withValue("daysWorked", 17)
      ));
      new InsertAction().execute(insertInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateInput initAggregateRequest()
   {
      AggregateInput aggregateInput = new AggregateInput();
      aggregateInput.setTableName(TestUtils.defineTablePerson().getName());
      return aggregateInput;
   }

}
