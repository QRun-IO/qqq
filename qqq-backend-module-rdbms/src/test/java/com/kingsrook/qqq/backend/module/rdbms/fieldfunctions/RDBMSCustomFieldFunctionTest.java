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

package com.kingsrook.qqq.backend.module.rdbms.fieldfunctions;


import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionIdentifierRegistry;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionParameter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeIdentifier;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeRegistry;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSAggregateAction;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Integration test demonstrating how to define and use a completely custom
 ** field function type ("NumberBucket") that translates integer values into
 ** string labels via a SQL CASE statement, with custom ORDER BY logic.
 *******************************************************************************/
class RDBMSCustomFieldFunctionTest
{
   public static final FieldFunctionTypeIdentifier IDENTIFIER = () -> "NumberBucket";



   /***************************************************************************
    ** FieldFunctionType that buckets integer values into string labels.
    ***************************************************************************/
   public static class NumberBucketFunction implements FieldFunctionType
   {
      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public FieldFunctionTypeIdentifier getIdentifier()
      {
         return (IDENTIFIER);
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public Set<QFieldType> getAllowedFieldTypes()
      {
         return QFieldType.INTEGRAL_TYPES;
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public List<FieldFunctionParameter> getParameters()
      {
         return Collections.emptyList();
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public QFieldType getReturnType()
      {
         return QFieldType.STRING;
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public Serializable apply(FieldFunction fieldFunction, QRecord record)
      {
         Integer value = record.getValueInteger(fieldFunction.getFieldName());
         if(value == null)
         {
            return (null);
         }

         return switch(value)
         {
            case 0 -> "none";
            case 1 -> "one";
            case 2 -> "a couple";
            default -> "lots";
         };
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public Serializable applyForSorting(FieldFunction fieldFunction, QRecord record)
      {
         Integer value = record.getValueInteger(fieldFunction.getFieldName());
         if(value == null)
         {
            return (null);
         }

         return switch(value)
         {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            default -> 3;
         };
      }
   }



   /***************************************************************************
    ** RDBMS adapter that generates SQL CASE expressions for NumberBucket.
    ***************************************************************************/
   public static class RDBMSNumberBucketFunction implements RDBMSFieldFunctionAdapterInterface
   {
      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public String wrapColumnName(String escapedColumnName, FieldFunction fieldFunction, Function<String, String> fieldNameToColumnReference)
      {
         return "CASE WHEN " + escapedColumnName + " = 0 THEN 'none'"
            + " WHEN " + escapedColumnName + " = 1 THEN 'one'"
            + " WHEN " + escapedColumnName + " = 2 THEN 'a couple'"
            + " ELSE 'lots' END";
      }



      /***************************************************************************
       * 
       ***************************************************************************/
      @Override
      public String wrapColumnNameForOrderBy(String escapedColumnName, FieldFunction fieldFunction, Function<String, String> fieldNameToColumnReference)
      {
         return "CASE WHEN " + escapedColumnName + " = 0 THEN 0"
            + " WHEN " + escapedColumnName + " = 1 THEN 1"
            + " WHEN " + escapedColumnName + " = 2 THEN 2"
            + " ELSE 3 END";
      }
   }



   /***************************************************************************
    ** Extended RDBMSBackendMetaData that registers the custom NumberBucket
    ** field function adapter alongside the standard ones.
    ***************************************************************************/
   public static class TestRDBMSBackendMetaData extends RDBMSBackendMetaData
   {
      @Override
      public void doRegisterFieldFunctionAdapters()
      {
         super.doRegisterFieldFunctionAdapters();
         registerBackendFieldFunctionAdapter(IDENTIFIER, RDBMSNumberBucketFunction.class);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      ////////////////////////////////////
      // register the custom identifier //
      ////////////////////////////////////
      FieldFunctionIdentifierRegistry.getInstance().register(IDENTIFIER);

      ///////////////////////////////////////////////////////////////////////
      // build instance with our custom backend instead of the default one //
      ///////////////////////////////////////////////////////////////////////
      QInstance qInstance = TestUtils.defineInstance();

      TestRDBMSBackendMetaData testBackend = new TestRDBMSBackendMetaData();
      testBackend.setName(TestUtils.DEFAULT_BACKEND_NAME);
      testBackend.setVendor("h2");
      testBackend.setHostName("mem");
      testBackend.setDatabaseName("test_database");
      testBackend.setUsername("sa");
      testBackend.setJdbcUrl(com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager.getJdbcUrl(testBackend) + ";DATABASE_TO_UPPER=FALSE");

      qInstance.getBackends().put(TestUtils.DEFAULT_BACKEND_NAME, testBackend);

      /////////////////////////
      // re-init the context //
      /////////////////////////
      QContext.init(qInstance, new com.kingsrook.qqq.backend.core.model.session.QSession());

      ////////////////////////////////
      // register the function type //
      ////////////////////////////////
      FieldFunctionTypeRegistry.ofOrWithNew(qInstance).register(IDENTIFIER, NumberBucketFunction.class);

      /////////////////////////////
      // prime the test database //
      /////////////////////////////
      TestUtils.primeTestDatabase("prime-test-database.sql");

      //////////////////////////////////////////////////////
      // add virtual field "idBucket" to the person table //
      //////////////////////////////////////////////////////
      QVirtualFieldMetaData idBucketField = new QVirtualFieldMetaData("idBucket", QFieldType.STRING)
         .withIsQuerySelectable(true)
         .withIsQueryCriteria(true)
         .withFieldFunction(new FieldFunction()
            .withFieldName("id")
            .withFunctionTypeIdentifier(IDENTIFIER));

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .withVirtualField(idBucketField);
   }



   /*******************************************************************************
    ** Verify that querying selects the virtual field values correctly.
    ** Test data ids: 1=Darin, 2=James, 3=Tim, 4=Tyler, 5=Garret
    ** Expected buckets: 1→"one", 2→"a couple", 3→"lots", 4→"lots", 5→"lots"
    *******************************************************************************/
   @Test
   void testQuerySelect() throws Exception
   {
      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON,
         new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));

      assertEquals(5, records.size());
      assertEquals("one", records.get(0).getValueString("idBucket"));
      assertEquals("a couple", records.get(1).getValueString("idBucket"));
      assertEquals("lots", records.get(2).getValueString("idBucket"));
      assertEquals("lots", records.get(3).getValueString("idBucket"));
      assertEquals("lots", records.get(4).getValueString("idBucket"));
   }



   /*******************************************************************************
    ** Verify that ORDER BY uses the custom wrapColumnNameForOrderBy (numeric
    ** bucket order), NOT alphabetical order of the string labels.
    **
    ** Alphabetical would be: "a couple"(2), "lots"(3,4,5), "one"(1)
    ** Bucket order should be: "one"(1), "a couple"(2), "lots"(3,4,5)
    *******************************************************************************/
   @Test
   void testQueryOrderBy() throws Exception
   {
      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON,
         new QQueryFilter().withOrderBy(new QFilterOrderBy("idBucket")));

      assertEquals(5, records.size());

      /////////////////////////////////////////////////////
      // bucket order: one(1), a couple(2), lots(3,4,5)  //
      /////////////////////////////////////////////////////
      assertEquals("one", records.get(0).getValueString("idBucket"));
      assertEquals("a couple", records.get(1).getValueString("idBucket"));
      assertEquals("lots", records.get(2).getValueString("idBucket"));
      assertEquals("lots", records.get(3).getValueString("idBucket"));
      assertEquals("lots", records.get(4).getValueString("idBucket"));

      ////////////////////////////////////////////////////
      // verify the actual ids are in the expected order //
      ////////////////////////////////////////////////////
      assertEquals(1, records.get(0).getValueInteger("id"));
      assertEquals(2, records.get(1).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Verify filtering by the virtual field value.
    *******************************************************************************/
   @Test
   void testQueryFilter() throws Exception
   {
      ////////////////////////////////////
      // filter where idBucket = 'lots' //
      ////////////////////////////////////
      List<QRecord> lotsRecords = QueryAction.execute(TestUtils.TABLE_NAME_PERSON,
         new QQueryFilter(new QFilterCriteria("idBucket", QCriteriaOperator.EQUALS, "lots")));
      assertEquals(3, lotsRecords.size());

      ///////////////////////////////////
      // filter where idBucket = 'one' //
      ///////////////////////////////////
      List<QRecord> oneRecords = QueryAction.execute(TestUtils.TABLE_NAME_PERSON,
         new QQueryFilter(new QFilterCriteria("idBucket", QCriteriaOperator.EQUALS, "one")));
      assertEquals(1, oneRecords.size());
      assertEquals(1, oneRecords.get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Verify COUNT(*) GROUP BY idBucket produces the expected groups.
    ** Expected: "one"(1), "a couple"(1), "lots"(3)
    *******************************************************************************/
   @Test
   void testAggregateGroupBy() throws Exception
   {
      QVirtualFieldMetaData idBucketField = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .getVirtualFields().get("idBucket");

      AggregateInput aggregateInput = new AggregateInput();
      aggregateInput.setTableName(TestUtils.TABLE_NAME_PERSON);

      Aggregate countOfId = new Aggregate("id", AggregateOperator.COUNT);
      aggregateInput.withAggregate(countOfId);

      GroupBy bucketGroupBy = new GroupBy(idBucketField);
      aggregateInput.withGroupBy(bucketGroupBy);

      AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      assertEquals(3, aggregateOutput.getResults().size());

      AggregateResult oneRow = aggregateOutput.getResults().stream()
         .filter(r -> "one".equals(r.getGroupByValue(bucketGroupBy)))
         .findFirst().orElseThrow();
      assertEquals(1, oneRow.getAggregateValue(countOfId));

      AggregateResult coupleRow = aggregateOutput.getResults().stream()
         .filter(r -> "a couple".equals(r.getGroupByValue(bucketGroupBy)))
         .findFirst().orElseThrow();
      assertEquals(1, coupleRow.getAggregateValue(countOfId));

      AggregateResult lotsRow = aggregateOutput.getResults().stream()
         .filter(r -> "lots".equals(r.getGroupByValue(bucketGroupBy)))
         .findFirst().orElseThrow();
      assertEquals(3, lotsRow.getAggregateValue(countOfId));
   }



   /*******************************************************************************
    ** Verify COUNT(*) GROUP BY idBucket, ORDER BY idBucket ASC returns results
    ** in logical bucket order (one, a couple, lots) — NOT alphabetical order
    ** (a couple, lots, one).
    *******************************************************************************/
   @Test
   void testAggregateGroupByWithOrderBy() throws Exception
   {
      QVirtualFieldMetaData idBucketField = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .getVirtualFields().get("idBucket");

      AggregateInput aggregateInput = new AggregateInput();
      aggregateInput.setTableName(TestUtils.TABLE_NAME_PERSON);

      Aggregate countOfId = new Aggregate("id", AggregateOperator.COUNT);
      aggregateInput.withAggregate(countOfId);

      GroupBy bucketGroupBy = new GroupBy(idBucketField);
      aggregateInput.withGroupBy(bucketGroupBy);

      aggregateInput.setFilter(new QQueryFilter()
         .withOrderBy(new QFilterOrderByGroupBy(bucketGroupBy, true)));

      AggregateOutput aggregateOutput = new RDBMSAggregateAction().execute(aggregateInput);
      assertEquals(3, aggregateOutput.getResults().size());

      //////////////////////////////////////////////////////////////////////////
      // results should be in logical bucket order: one, a couple, lots       //
      // NOT alphabetical: a couple, lots, one                                //
      //////////////////////////////////////////////////////////////////////////
      assertEquals("one", aggregateOutput.getResults().get(0).getGroupByValue(bucketGroupBy));
      assertEquals(1, aggregateOutput.getResults().get(0).getAggregateValue(countOfId));

      assertEquals("a couple", aggregateOutput.getResults().get(1).getGroupByValue(bucketGroupBy));
      assertEquals(1, aggregateOutput.getResults().get(1).getAggregateValue(countOfId));

      assertEquals("lots", aggregateOutput.getResults().get(2).getGroupByValue(bucketGroupBy));
      assertEquals(3, aggregateOutput.getResults().get(2).getAggregateValue(countOfId));
   }

}
