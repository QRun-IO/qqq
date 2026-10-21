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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeIdentifier;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native Aggregate expression/unsupported-operation probes. Existing module
 ** metadata, driver and disposable server only; no SQL/null-count parity claim.
 *******************************************************************************/
public class MongoDBAggregateExpressionContractTest extends BaseTest
{
   private static final String TABLE = TestUtils.TABLE_NAME_PERSON;
   private static final String LENGTH = "computedNameLength";
   private QInstance instance;
   private QSession session;



   /*******************************************************************************
    ** All aggregate operands are nonnull INTEGER. String lengths are 5,5,2,7.
    *******************************************************************************/
   @BeforeEach
   void seedNativeDocuments()
   {
      instance = QContext.getQInstance();
      session = new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1);
      QContext.setQSession(session);
      assertNotNull(backend().getFieldFunctionAdapter(StringLengthFunction.IDENTIFIER), "Existing native StringLength adapter must be registered");
      try(MongoClient client = getMongoClient())
      {
         MongoDatabase database = client.getDatabase(TestUtils.MONGO_DATABASE);
         database.getCollection(TABLE).insertMany(List.of(
            person(1, "Alpha", 10), person(2, "Bravo", 20), person(3, "Li", 100), person(4, "Charlie", 1000)));
         database.getCollection(TestUtils.TABLE_NAME_ORDER).insertMany(List.of(
            order(11, 1), order(12, 1), order(13, 3)));
         database.getCollection(TestUtils.TABLE_NAME_STORE).insertOne(new Document("_id", id(99)).append("key", 1).append("name", "Unrelated store"));
      }
   }



   /*******************************************************************************
    ** Virtual predicates must be computed before $match, including nested filters.
    *******************************************************************************/
   @Test
   void testVirtualPredicatesFlatAndNested() throws Exception
   {
      table().withVirtualField(virtual(lengthFunction()));
      assertPredicateVariants(new QFilterCriteria(LENGTH, QCriteriaOperator.GREATER_THAN, 4), "virtual");
   }



   /*******************************************************************************
    ** Inline criteria use the registered function without a named virtual field.
    *******************************************************************************/
   @Test
   void testInlinePredicatesFlatAndNested() throws Exception
   {
      assertPredicateVariants(new QFilterCriteria("firstName", QCriteriaOperator.GREATER_THAN, 4).withFieldFunction(lengthFunction()), "inline");
   }



   /*******************************************************************************
    ** Existing registered value/group expressions are independent positive controls.
    *******************************************************************************/
   @Test
   void testRegisteredVirtualValuesAndGroups() throws Exception
   {
      table().withVirtualField(virtual(lengthFunction()));
      List<List<Integer>> scalar = nativeRows(List.of(new Document("$group", new Document("_id", null)
         .append("count", new Document("$sum", 1)).append("sum", new Document("$sum", new Document("$strLenCP", "$firstName")))
         .append("min", new Document("$min", new Document("$strLenCP", "$firstName")))
         .append("max", new Document("$max", new Document("$strLenCP", "$firstName"))))), List.of("count", "sum", "min", "max"));
      assertEquals(List.of(List.of(4, 19, 2, 7)), scalar);
      List<List<Integer>> groups = nativeRows(List.of(new Document("$group", new Document("_id", new Document("$strLenCP", "$firstName"))
         .append("count", new Document("$sum", 1)).append("sum", new Document("$sum", "$daysWorked"))),
         new Document("$sort", new Document("_id", 1))), List.of("_id", "count", "sum"));
      assertEquals(List.of(List.of(2, 1, 100), List.of(5, 2, 30), List.of(7, 1, 1000)), groups);
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         checks.add(() -> assertAggregate("registered value / " + source, input(source,
            new Aggregate("seqNo", AggregateOperator.COUNT), new Aggregate(LENGTH, AggregateOperator.SUM),
            new Aggregate(LENGTH, AggregateOperator.MIN), new Aggregate(LENGTH, AggregateOperator.MAX)), scalar));
         checks.add(() ->
         {
            GroupBy group = new GroupBy(QFieldType.INTEGER, LENGTH);
            assertAggregate("registered group / " + source, input(source, count(), sum()).withGroupBy(group)
               .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(group, true))), groups);
         });
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** Count shares computed predicate semantics with an actual native Query.
    *******************************************************************************/
   @Test
   void testCountVirtualPredicatesFlatAndNestedWithQueryControls() throws Exception
   {
      table().withVirtualField(virtual(lengthFunction()));
      assertCountPredicateVariants(new QFilterCriteria(LENGTH, QCriteriaOperator.GREATER_THAN, 4), "virtual", true);
   }



   /*******************************************************************************
    ** Inline functions require the same native pre-match computation in Count.
    *******************************************************************************/
   @Test
   void testCountInlinePredicatesFlatAndNestedWithQueryControls() throws Exception
   {
      assertCountPredicateVariants(new QFilterCriteria("firstName", QCriteriaOperator.GREATER_THAN, 4).withFieldFunction(lengthFunction()), "inline", false);
   }



   /*******************************************************************************
    ** The direct driver count and row projection are independent of QQQ filters.
    *******************************************************************************/
   private void assertCountPredicateVariants(QFilterCriteria criterion, String label, Boolean includeVirtual) throws Exception
   {
      Document lengthPredicate = new Document("$gt", List.of(new Document("$strLenCP", "$firstName"), 4));
      Document rowPredicate = new Document("$gt", List.of("$seqNo", 1));
      Document nativeFilter = new Document("$expr", new Document("$and", List.of(lengthPredicate, rowPredicate)));
      List<Map<String, Object>> expected = new ArrayList<>();
      Long expectedCount;
      try(MongoClient client = getMongoClient())
      {
         MongoCollection<Document> collection = client.getDatabase(TestUtils.MONGO_DATABASE).getCollection(TABLE);
         expectedCount = collection.countDocuments(nativeFilter);
         Document projection = new Document("_id", 1).append("seqNo", 1).append("firstName", 1).append("daysWorked", 1);
         if(includeVirtual)
         {
            projection.append(LENGTH, new Document("$strLenCP", "$firstName"));
         }
         List<Document> pipeline = List.of(new Document("$match", nativeFilter), new Document("$sort", new Document("_id", 1)), new Document("$project", projection));
         for(Document document : collection.aggregate(pipeline))
         {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", document.getObjectId("_id").toHexString());
            row.put("seqNo", document.getInteger("seqNo"));
            row.put("firstName", document.getString("firstName"));
            row.put("daysWorked", document.getInteger("daysWorked"));
            if(includeVirtual)
            {
               row.put(LENGTH, document.getInteger(LENGTH));
            }
            expected.add(row);
         }
      }
      assertAll("Native computed Count/Query oracle",
         () -> assertEquals(2L, expectedCount),
         () -> assertEquals(List.of(id(2).toHexString(), id(4).toHexString()), expected.stream().map(row -> row.get("id")).toList()),
         () -> assertEquals(List.of(20, 1000), expected.stream().map(row -> row.get("daysWorked")).toList()));
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         checks.add(() -> assertCountAndQuery(label + " / flat / " + source, source,
            new QQueryFilter().withCriteria(criterion.clone()).withCriteria(new QFilterCriteria("seqNo", QCriteriaOperator.GREATER_THAN, 1)), expected));
         checks.add(() -> assertCountAndQuery(label + " / nested AND / " + source, source,
            new QQueryFilter().withSubFilter(new QQueryFilter().withCriteria(criterion.clone()))
               .withCriteria(new QFilterCriteria("seqNo", QCriteriaOperator.GREATER_THAN, 1)), expected));
         checks.add(() -> assertCountAndQuery(label + " / physical positive / " + source, source,
            new QQueryFilter().withCriteria(new QFilterCriteria("seqNo", QCriteriaOperator.IN, List.of(2, 4))), expected));
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** Separate public requests preserve every serialized property, even on red.
    ** Query uses the provider's ordinary default selection. This control checks
    ** exact matching identities and values, not a new virtual-projection contract.
    *******************************************************************************/
   private void assertCountAndQuery(String label, QInputSource source, QQueryFilter filter, List<Map<String, Object>> expected) throws Exception
   {
      CountInput countInput = new CountInput();
      countInput.setTableName(TABLE);
      countInput.setInputSource(source);
      countInput.setFilter(filter.clone());
      QueryInput queryInput = new QueryInput(TABLE).withInputSource(source)
         .withFilter(filter.clone().withOrderBy(new QFilterOrderBy("id")));
      String countBefore = JsonUtils.toJson(countInput);
      String queryBefore = JsonUtils.toJson(queryInput);
      QQueryFilter countFilter = countInput.getFilter();
      QQueryFilter queryFilter = queryInput.getFilter();
      String metadataBefore = metadata();
      String sessionBefore = JsonUtils.toJson(session);
      Map<String, List<String>> storedBefore = storedDocuments();
      assertAll(label,
         () -> assertEquals(expected.size(), new CountAction().execute(countInput).getCount(), "Count must apply the computed predicate"),
         () ->
         {
            List<QRecord> records = new QueryAction().execute(queryInput).getRecords();
            List<Map<String, Object>> rows = new ArrayList<>();
            for(QRecord record : records)
            {
               assertInstanceOf(String.class, record.getValue("id"));
               assertInstanceOf(Integer.class, record.getValue("seqNo"));
               assertInstanceOf(String.class, record.getValue("firstName"));
               assertInstanceOf(Integer.class, record.getValue("daysWorked"));
               if(table().getVirtualField(LENGTH) != null)
               {
                  assertInstanceOf(Integer.class, record.getValue(LENGTH));
               }
               Map<String, Object> row = new LinkedHashMap<>();
               for(String field : expected.get(0).keySet())
               {
                  row.put(field, record.getValue(field));
               }
               rows.add(row);
            }
            assertEquals(expected, rows, "Query must return the exact independently matched native identities and values");
         },
         () -> assertTrue(countBefore.equals(JsonUtils.toJson(countInput)), "Count input must remain unchanged"),
         () -> assertTrue(queryBefore.equals(JsonUtils.toJson(queryInput)), "Query input must remain unchanged"),
         () -> assertSame(countFilter, countInput.getFilter()),
         () -> assertSame(queryFilter, queryInput.getFilter()),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"),
         () -> assertEquals(storedBefore, storedDocuments()),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"));
   }



   /*******************************************************************************
    ** A registered but unrequested expression must not be evaluated as a side
    ** effect of another operand. Native $strLenCP on absent email proves the trap.
    *******************************************************************************/
   @Test
   void testUnreferencedRegisteredExpressionIsNotEvaluated() throws Exception
   {
      table().withVirtualField(virtual(lengthFunction()));
      table().withVirtualField(new QVirtualFieldMetaData("unusedEmailLength", QFieldType.INTEGER)
         .withIsQuerySelectable(true).withIsQueryCriteria(true)
         .withFieldFunction(new FieldFunction().withFieldName("email").withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)));
      try(MongoClient client = getMongoClient())
      {
         MongoCollection<Document> collection = client.getDatabase(TestUtils.MONGO_DATABASE).getCollection(TABLE);
         assertEquals(4L, collection.countDocuments(new Document("email", new Document("$exists", false))));
         Document invalidProjection = new Document("$project", new Document("_id", 0).append("bad", new Document("$strLenCP", "$email")));
         assertThrows(MongoCommandException.class, () -> collection.aggregate(List.of(invalidProjection)).first(), "Independent native proof that evaluating unused email length fails");
      }
      Document nativeMatch = new Document("$match", new Document("$expr", new Document("$gt", List.of(new Document("$strLenCP", "$firstName"), 4))));
      List<List<Integer>> all = nativeRows(List.of(scalarGroup()), List.of("count", "sum"));
      List<List<Integer>> matched = nativeRows(List.of(nativeMatch, scalarGroup()), List.of("count", "sum"));
      assertAll("Native unused-expression control",
         () -> assertEquals(List.of(List.of(4, 1130)), all),
         () -> assertEquals(List.of(List.of(3, 1030)), matched));
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         checks.add(() -> assertAggregate("unused expression / physical Aggregate / " + source, input(source, count(), sum()), all));
         checks.add(() -> assertAggregate("unused expression / computed Aggregate / " + source, input(source, count(), sum())
            .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria(LENGTH, QCriteriaOperator.GREATER_THAN, 4))), matched));
         checks.add(() -> assertCountOnly("unused expression / physical Count / " + source, source, new QQueryFilter(), 4));
         checks.add(() -> assertCountOnly("unused expression / computed Count / " + source, source,
            new QQueryFilter().withCriteria(new QFilterCriteria(LENGTH, QCriteriaOperator.GREATER_THAN, 4)), 3));
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** Count-only preservation helper for a declared expression outside its request.
    ** Query default selection of virtual fields is intentionally a separate contract.
    *******************************************************************************/
   private void assertCountOnly(String label, QInputSource source, QQueryFilter filter, Integer expected) throws Exception
   {
      CountInput input = new CountInput();
      input.setTableName(TABLE);
      input.setInputSource(source);
      input.setFilter(filter);
      String inputBefore = JsonUtils.toJson(input);
      String metadataBefore = metadata();
      String sessionBefore = JsonUtils.toJson(session);
      Map<String, List<String>> storedBefore = storedDocuments();
      assertAll(label,
         () -> assertEquals(expected, new CountAction().execute(input).getCount()),
         () -> assertTrue(inputBefore.equals(JsonUtils.toJson(input)), "Count input must remain unchanged"),
         () -> assertSame(filter, input.getFilter()),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"),
         () -> assertEquals(storedBefore, storedDocuments()),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"));
   }



   /*******************************************************************************
    ** Different arguments on the same function must remain distinct expressions,
    ** including reverse encounter order and nested Boolean filters.
    *******************************************************************************/
   @Test
   void testSameFunctionArgumentsRemainDistinctAcrossAllReadActions() throws Exception
   {
      assertNotNull(backend().getFieldFunctionAdapter(SubStringFunction.IDENTIFIER));
      QFilterCriteria firstA = substringCriterion(1, "A");
      QFilterCriteria secondL = substringCriterion(2, "l");
      QFilterCriteria secondR = substringCriterion(2, "r");
      Document firstNative = new Document("$eq", List.of(new Document("$substrCP", List.of("$firstName", 0, 1)), "A"));
      Document secondLNative = new Document("$eq", List.of(new Document("$substrCP", List.of("$firstName", 1, 1)), "l"));
      Document secondRNative = new Document("$eq", List.of(new Document("$substrCP", List.of("$firstName", 1, 1)), "r"));
      Document nativeAnd = new Document("$expr", new Document("$and", List.of(firstNative, secondLNative)));
      Document nativeOr = new Document("$expr", new Document("$or", List.of(firstNative, secondRNative)));
      assertAll(
         () -> assertThreeActions("SubString AND flat", new QQueryFilter().withCriteria(firstA.clone()).withCriteria(secondL.clone()),
            nativeAnd, "daysWorked", List.of(1), 10),
         () -> assertThreeActions("SubString AND nested", new QQueryFilter()
            .withSubFilter(new QQueryFilter().withCriteria(firstA.clone())).withSubFilter(new QQueryFilter().withCriteria(secondL.clone())),
            nativeAnd, "daysWorked", List.of(1), 10),
         () -> assertThreeActions("SubString OR flat", new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withCriteria(firstA.clone()).withCriteria(secondR.clone()), nativeOr, "daysWorked", List.of(1, 2), 30),
         () -> assertThreeActions("SubString OR reverse encounter order", new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withCriteria(secondR.clone()).withCriteria(firstA.clone()), nativeOr, "daysWorked", List.of(1, 2), 30),
         () -> assertThreeActions("SubString OR nested", new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withSubFilter(new QQueryFilter().withCriteria(firstA.clone())).withSubFilter(new QQueryFilter().withCriteria(secondR.clone())),
            nativeOr, "daysWorked", List.of(1, 2), 30));
   }



   /*******************************************************************************
    ** A physical backend field can have the old generated expression name; reads
    ** must neither overwrite its value nor mistake it for the computed predicate.
    *******************************************************************************/
   @Test
   void testComputedNameCannotOverwriteMappedPhysicalField() throws Exception
   {
      String physicalAmount = "firstName_StringLength";
      table().getField("daysWorked").setBackendName(physicalAmount);
      try(MongoClient client = getMongoClient())
      {
         MongoCollection<Document> collection = client.getDatabase(TestUtils.MONGO_DATABASE).getCollection(TABLE);
         assertEquals(4L, collection.updateMany(new Document(), new Document("$rename", new Document("daysWorked", physicalAmount))).getModifiedCount());
         assertEquals(0L, collection.countDocuments(new Document("daysWorked", new Document("$exists", true))));
         assertEquals(List.of(10, 20, 100, 1000), collection.find().sort(new Document("_id", 1))
            .map(document -> document.getInteger(physicalAmount)).into(new ArrayList<>()));
      }
      QFilterCriteria criterion = new QFilterCriteria("firstName", QCriteriaOperator.GREATER_THAN, 4).withFieldFunction(lengthFunction());
      Document nativeFilter = new Document("$expr", new Document("$gt", List.of(new Document("$strLenCP", "$firstName"), 4)));
      assertAll(
         () -> assertThreeActions("mapped-name physical positive", new QQueryFilter(), new Document(), physicalAmount, List.of(1, 2, 3, 4), 1130),
         () -> assertThreeActions("mapped-name inline flat", new QQueryFilter().withCriteria(criterion.clone()), nativeFilter, physicalAmount, List.of(1, 2, 4), 1030),
         () -> assertThreeActions("mapped-name inline nested", new QQueryFilter().withSubFilter(new QQueryFilter().withCriteria(criterion.clone())),
            nativeFilter, physicalAmount, List.of(1, 2, 4), 1030));
   }



   /*******************************************************************************
    ** Native $expr avoids all generated temporary field names. Both raw values
    ** and native aggregate totals are compared to literal independent expectations.
    *******************************************************************************/
   private void assertThreeActions(String label, QQueryFilter filter, Document nativeFilter, String amountField, List<Integer> expectedIds, Integer expectedSum) throws Exception
   {
      List<Map<String, Object>> expectedRows = new ArrayList<>();
      Long nativeCount;
      try(MongoClient client = getMongoClient())
      {
         MongoCollection<Document> collection = client.getDatabase(TestUtils.MONGO_DATABASE).getCollection(TABLE);
         nativeCount = collection.countDocuments(nativeFilter);
         for(Document document : collection.find(nativeFilter).sort(new Document("_id", 1)))
         {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", document.getObjectId("_id").toHexString());
            row.put("seqNo", document.getInteger("seqNo"));
            row.put("firstName", document.getString("firstName"));
            row.put("daysWorked", document.getInteger(amountField));
            expectedRows.add(row);
         }
      }
      Document group = new Document("$group", new Document("_id", null).append("count", new Document("$sum", 1))
         .append("sum", new Document("$sum", "$" + amountField)));
      List<List<Integer>> expectedAggregate = nativeRows(List.of(new Document("$match", nativeFilter), group), List.of("count", "sum"));
      assertAll(label + " direct Mongo oracle",
         () -> assertEquals(expectedIds.size(), nativeCount.intValue()),
         () -> assertEquals(expectedIds.stream().map(number -> id(number).toHexString()).toList(), expectedRows.stream().map(row -> row.get("id")).toList()),
         () -> assertEquals(List.of(List.of(expectedIds.size(), expectedSum)), expectedAggregate));
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         checks.add(() -> assertCountAndQuery(label + " / Query + Count / " + source, source, filter.clone(), expectedRows));
         checks.add(() -> assertAggregate(label + " / Aggregate / " + source, input(source, count(), sum()).withFilter(filter.clone()), expectedAggregate));
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** QQQ arguments are 1-based; the independent native expressions above are not
    ** generated from these objects, so argument collisions cannot taint the oracle.
    *******************************************************************************/
   private static QFilterCriteria substringCriterion(Integer start, String expected)
   {
      return new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, expected).withFieldFunction(new FieldFunction()
         .withFieldName("firstName").withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, start, SubStringFunction.LENGTH_PARAM, 1)));
   }



   /*******************************************************************************
    ** Computation must not change which native owner satisfies a READ lock.
    ** Original unstructured source/f0 fields also survive any internal wrapping.
    *******************************************************************************/
   @Test
   void testComputedNameCollisionCannotChangeReadOwnershipOrPublicValues() throws Exception
   {
      String ownerField = "firstName_StringLength";
      String ownerKey = "mongoComputedOwner";
      table().getField("daysWorked").setBackendName(ownerField);
      instance.addSecurityKeyType(new QSecurityKeyType().withName(ownerKey));
      table().setRecordSecurityLocks(List.of(new RecordSecurityLock().withFieldName("daysWorked").withSecurityKeyType(ownerKey)
         .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY)));
      session.withSecurityKeyValue(ownerKey, 5);
      Map<String, Object> expectedPublicValues = new LinkedHashMap<>();
      table().getFields().keySet().forEach(field -> expectedPublicValues.put(field, null));
      try(MongoClient client = getMongoClient())
      {
         MongoCollection<Document> collection = client.getDatabase(TestUtils.MONGO_DATABASE).getCollection(TABLE);
         assertEquals(1L, collection.updateOne(new Document("_id", id(3)), new Document("$set", new Document("daysWorked", 5))).getModifiedCount());
         assertEquals(4L, collection.updateMany(new Document(), new Document("$rename", new Document("daysWorked", ownerField))).getModifiedCount());
         for(Integer number : List.of(1, 2, 3, 4))
         {
            Document unstructured = new Document("source", new Document("marker", "native-source-" + number)).append("f0", "native-f0-" + number);
            assertEquals(1L, collection.updateOne(new Document("_id", id(number)), new Document("$set", unstructured)).getModifiedCount());
         }
         assertEquals(List.of(10, 20, 5, 1000), collection.find().sort(new Document("_id", 1)).map(document -> document.getInteger(ownerField)).into(new ArrayList<>()));
         Document visible = collection.find(new Document(ownerField, 5)).first();
         assertNotNull(visible);
         assertEquals(id(3), visible.getObjectId("_id"));
         expectedPublicValues.put("id", visible.getObjectId("_id").toHexString());
         expectedPublicValues.put("seqNo", visible.getInteger("seqNo"));
         expectedPublicValues.put("firstName", visible.getString("firstName"));
         expectedPublicValues.put("lastName", visible.getString("lastName"));
         expectedPublicValues.put("daysWorked", visible.getInteger(ownerField));
         expectedPublicValues.put("audit", new LinkedHashMap<>(visible.get("audit", Document.class)));
         expectedPublicValues.put("source", new LinkedHashMap<>(visible.get("source", Document.class)));
         expectedPublicValues.put("f0", visible.getString("f0"));
      }
      QFilterCriteria length = new QFilterCriteria("firstName", QCriteriaOperator.GREATER_THAN, 0).withFieldFunction(lengthFunction());
      QQueryFilter filter = new QQueryFilter().withCriteria(length);
      Document nativeFilter = new Document(ownerField, 5)
         .append("$expr", new Document("$gt", List.of(new Document("$strLenCP", "$firstName"), 0)));
      assertAll(
         () -> assertThreeActions("mapped READ-owner physical positive", new QQueryFilter(), new Document(ownerField, 5), ownerField, List.of(3), 5),
         () -> assertThreeActions("mapped READ-owner computed", filter, nativeFilter, ownerField, List.of(3), 5),
         () -> assertExactPublicQuery("physical positive", QInputSource.USER, new QQueryFilter(), expectedPublicValues),
         () -> assertExactPublicQuery("physical positive", QInputSource.SYSTEM, new QQueryFilter(), expectedPublicValues),
         () -> assertExactPublicQuery("computed", QInputSource.USER, filter.clone(), expectedPublicValues),
         () -> assertExactPublicQuery("computed", QInputSource.SYSTEM, filter.clone(), expectedPublicValues));
   }



   /*******************************************************************************
    ** Exact complete default values catch leaked temporary aliases and discarded
    ** original unstructured data; nullable declared fields must remain present.
    *******************************************************************************/
   private void assertExactPublicQuery(String label, QInputSource source, QQueryFilter filter, Map<String, Object> expected) throws Exception
   {
      QueryInput input = new QueryInput(TABLE).withInputSource(source).withFilter(filter.withOrderBy(new QFilterOrderBy("id")));
      String inputBefore = JsonUtils.toJson(input);
      String metadataBefore = metadata();
      String sessionBefore = JsonUtils.toJson(session);
      Map<String, List<String>> storedBefore = storedDocuments();
      assertAll("exact public READ-owner Query / " + label + " / " + source,
         () ->
         {
            List<QRecord> records = new QueryAction().execute(input).getRecords();
            List<Executable> checks = new ArrayList<>();
            for(QRecord record : records)
            {
               checks.add(() -> assertEquals(expected.keySet(), record.getValues().keySet(), "Public value keys must contain only declared and original native fields"));
               checks.add(() -> assertInstanceOf(Integer.class, record.getValue("daysWorked")));
            }
            checks.add(() -> assertEquals(List.of(expected), records.stream().map(record -> record.getValues()).toList(), "Only the original Li owner record is visible, with exact raw stored values"));
            assertAll(checks);
         },
         () -> assertTrue(inputBefore.equals(JsonUtils.toJson(input)), "Query input must remain unchanged"),
         () -> assertSame(filter, input.getFilter()),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"),
         () -> assertEquals(storedBefore, storedDocuments()),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"));
   }



   /*******************************************************************************
    ** Each registered function accepts its source type, but composing an inline
    ** function over a virtual result is not a supported MongoDB predicate.
    *******************************************************************************/
   @Test
   void testInlineFunctionOnVirtualFieldRefusesWithoutIgnoringComposition() throws Exception
   {
      String initialField = "computedNameInitial";
      FieldFunction initialFunction = new FieldFunction().withFieldName("firstName").withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 1, SubStringFunction.LENGTH_PARAM, 1));
      table().withVirtualField(new QVirtualFieldMetaData(initialField, QFieldType.STRING)
         .withIsQuerySelectable(true).withIsQueryCriteria(true).withFieldFunction(initialFunction));
      FieldFunction inlineFunction = new FieldFunction().withFieldName(initialField).withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER);
      Document nativeInitial = new Document("$substrCP", List.of("$firstName", 0, 1));
      Document nativeComposed = new Document("$expr", new Document("$eq", List.of(new Document("$strLenCP", nativeInitial), 1)));
      Document nativeIgnored = new Document("$expr", new Document("$eq", List.of(nativeInitial, 1)));
      try(MongoClient client = getMongoClient())
      {
         MongoCollection<Document> collection = client.getDatabase(TestUtils.MONGO_DATABASE).getCollection(TABLE);
         assertEquals(4L, collection.countDocuments(nativeComposed), "Both valid function stages select all four native rows");
         assertEquals(0L, collection.countDocuments(nativeIgnored), "Ignoring the outer function changes the native result");
      }
      assertEquals(List.of(List.of(4, 1130)), nativeRows(List.of(new Document("$match", nativeComposed), scalarGroup()), List.of("count", "sum")));
      List<Executable> checks = new ArrayList<>();
      checks.add(() -> assertThreeActions("composition physical positive", new QQueryFilter(), new Document(), "daysWorked", List.of(1, 2, 3, 4), 1130));
      checks.add(() -> assertThreeActions("composition normal virtual positive",
         new QQueryFilter().withCriteria(new QFilterCriteria(initialField, QCriteriaOperator.EQUALS, "A")),
         new Document("$expr", new Document("$eq", List.of(nativeInitial, "A"))), "daysWorked", List.of(1), 10));
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         for(Boolean nested : List.of(false, true))
         {
            QQueryFilter criterionFilter = new QQueryFilter().withCriteria(new QFilterCriteria(initialField, QCriteriaOperator.EQUALS, 1).withFieldFunction(inlineFunction));
            QQueryFilter filter = nested ? new QQueryFilter().withSubFilter(criterionFilter) : criterionFilter;
            QueryInput queryInput = new QueryInput(TABLE).withInputSource(source).withFilter(filter.clone());
            CountInput countInput = new CountInput();
            countInput.setTableName(TABLE);
            countInput.setInputSource(source);
            countInput.setFilter(filter.clone());
            AggregateInput aggregateInput = input(source, count(), sum()).withFilter(filter.clone());
            String label = "inline on virtual / " + source + " / " + (nested ? "nested AND" : "flat");
            checks.add(() ->
            {
               List<Object> inputs = List.of(queryInput, countInput, aggregateInput);
               String inputsBefore = JsonUtils.toJson(inputs);
               String filterBefore = JsonUtils.toJson(filter);
               String metadataBefore = metadata();
               String sessionBefore = JsonUtils.toJson(session);
               Map<String, List<String>> storedBefore = storedDocuments();
               QQueryFilter originalQueryFilter = queryInput.getFilter();
               QQueryFilter originalCountFilter = countInput.getFilter();
               QQueryFilter originalAggregateFilter = aggregateInput.getFilter();
               Map<String, Executable> actions = new LinkedHashMap<>();
               actions.put("Query", () -> new QueryAction().execute(queryInput));
               actions.put("Count", () -> new CountAction().execute(countInput));
               actions.put("Aggregate", () -> new AggregateAction().execute(aggregateInput));
               List<Executable> assertions = new ArrayList<>();
               actions.forEach((action, execute) -> assertions.add(() ->
               {
                  QException failure = assertThrows(QException.class, execute, action + " must refuse unsupported composition");
                  Boolean foundReason = false;
                  for(Throwable cause = failure; cause != null; cause = cause.getCause())
                  {
                     assertFalse(cause instanceof NullPointerException || cause instanceof IllegalArgumentException || cause instanceof MongoException,
                        action + " refusal must not be a malformed native request or null dereference");
                     String message = cause.getMessage();
                     if(message != null && message.toLowerCase().contains("virtual") && message.toLowerCase().contains("inline"))
                     {
                        foundReason = true;
                     }
                  }
                  assertTrue(foundReason, action + " must identify unsupported inline-on-virtual composition");
               }));
               assertions.add(() -> assertTrue(inputsBefore.equals(JsonUtils.toJson(inputs)), "Rejected read inputs must remain unchanged"));
               assertions.add(() -> assertTrue(filterBefore.equals(JsonUtils.toJson(filter)), "Original function/filter arguments must remain unchanged"));
               assertions.add(() -> assertSame(originalQueryFilter, queryInput.getFilter()));
               assertions.add(() -> assertSame(originalCountFilter, countInput.getFilter()));
               assertions.add(() -> assertSame(originalAggregateFilter, aggregateInput.getFilter()));
               assertions.add(() -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"));
               assertions.add(() -> assertEquals(storedBefore, storedDocuments()));
               assertions.add(() -> assertSame(session, QContext.getQSession()));
               assertions.add(() -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"));
               assertAll(label, assertions);
            });
         }
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** Driver $expr computes lengths directly; no framework-generated field names.
    *******************************************************************************/
   private void assertPredicateVariants(QFilterCriteria criterion, String label) throws Exception
   {
      Document lengthPredicate = new Document("$gt", List.of(new Document("$strLenCP", "$firstName"), 4));
      Document rowPredicate = new Document("$gt", List.of("$seqNo", 1));
      Document nativeMatch = new Document("$match", new Document("$expr", new Document("$and", List.of(lengthPredicate, rowPredicate))));
      List<List<Integer>> expected = nativeRows(List.of(nativeMatch, scalarGroup()), List.of("count", "sum"));
      assertEquals(List.of(List.of(2, 1020)), expected);
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         checks.add(() -> assertAggregate(label + " / flat / " + source, input(source, count(), sum())
            .withFilter(new QQueryFilter().withCriteria(criterion.clone())
               .withCriteria(new QFilterCriteria("seqNo", QCriteriaOperator.GREATER_THAN, 1))), expected));
         checks.add(() -> assertAggregate(label + " / nested AND / " + source, input(source, count(), sum())
            .withFilter(new QQueryFilter().withSubFilter(new QQueryFilter().withCriteria(criterion.clone()))
               .withCriteria(new QFilterCriteria("seqNo", QCriteriaOperator.GREATER_THAN, 1))), expected));
         checks.add(() -> assertAggregate(label + " / physical positive / " + source, input(source, count(), sum())
            .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria("seqNo", QCriteriaOperator.IN, List.of(2, 4)))), expected));
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** Each referenced unsupported role must reject; unreferenced metadata is safe.
    *******************************************************************************/
   private void assertUnavailableVariants(String label) throws Exception
   {
      List<List<Integer>> expected = nativeRows(List.of(scalarGroup()), List.of("count", "sum"));
      assertEquals(List.of(List.of(4, 1130)), expected);
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         checks.add(() -> assertAggregate(label + " unreferenced / " + source, input(source, count(), sum()), expected));
         checks.add(() -> assertAggregate(label + " unavailable expression row COUNT / " + source,
            input(source, new Aggregate(LENGTH, AggregateOperator.COUNT)), List.of(List.of(expected.get(0).get(0)))));
         checks.add(() -> assertRefused(label + " SUM / " + source, input(source, new Aggregate(LENGTH, AggregateOperator.SUM)), "function"));
         checks.add(() -> assertRefused(label + " GROUP / " + source, input(source, count()).withGroupBy(new GroupBy(QFieldType.INTEGER, LENGTH)), "function"));
         checks.add(() -> assertRefused(label + " FILTER / " + source, input(source, count())
            .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria(LENGTH, QCriteriaOperator.GREATER_THAN, 4))), "function"));
         checks.add(() -> assertCountOnly(label + " unreferenced Count / " + source, source, new QQueryFilter(), 4));
         checks.add(() -> assertCountRefused(label + " Count FILTER / flat / " + source, source,
            new QQueryFilter().withCriteria(new QFilterCriteria(LENGTH, QCriteriaOperator.GREATER_THAN, 4))));
         checks.add(() -> assertCountRefused(label + " Count FILTER / nested AND / " + source, source,
            new QQueryFilter().withSubFilter(new QQueryFilter().withCriteria(new QFilterCriteria(LENGTH, QCriteriaOperator.GREATER_THAN, 4)))));
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** Customizer-only values really work in Query; Aggregate must not fake them.
    *******************************************************************************/
   private void assertCustomizerQueryControl() throws Exception
   {
      QueryInput input = new QueryInput(TABLE).withInputSource(QInputSource.USER)
         .withFieldNamesToInclude(Set.of("id", "firstName"))
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
      String inputBefore = JsonUtils.toJson(input);
      String metadataBefore = metadata();
      String sessionBefore = JsonUtils.toJson(session);
      QQueryFilter originalFilter = input.getFilter();
      Map<String, List<String>> storedBefore = storedDocuments();
      List<List<Integer>> expected = nativeRows(List.of(new Document("$sort", new Document("_id", 1)),
         new Document("$project", new Document("seqNo", 1).append("length", new Document("$strLenCP", "$firstName")))), List.of("seqNo", "length"));
      assertEquals(List.of(List.of(1, 5), List.of(2, 5), List.of(3, 2), List.of(4, 7)), expected);
      assertAll("customizer Query positive",
         () ->
         {
            List<QRecord> records = new QueryAction().execute(input).getRecords();
            List<List<Integer>> actual = new ArrayList<>();
            for(QRecord record : records)
            {
               assertEquals(Set.of("id", "firstName", LENGTH), record.getValues().keySet());
               Integer number = Integer.parseInt(assertInstanceOf(String.class, record.getValue("id")), 16);
               actual.add(List.of(number, assertInstanceOf(Integer.class, record.getValue(LENGTH))));
            }
            assertEquals(expected, actual);
         },
         () -> assertTrue(inputBefore.equals(JsonUtils.toJson(input)), "Query input must remain unchanged"),
         () -> assertSame(originalFilter, input.getFilter()),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"),
         () -> assertEquals(storedBefore, storedDocuments()));
   }



   /*******************************************************************************
    ** Typed exact descriptors/results plus preservation, even after result failures.
    *******************************************************************************/
   private void assertAggregate(String label, AggregateInput input, List<List<Integer>> expected) throws Exception
   {
      String inputBefore = JsonUtils.toJson(input);
      String metadataBefore = metadata();
      String sessionBefore = JsonUtils.toJson(session);
      Map<String, List<String>> storedBefore = storedDocuments();
      QQueryFilter filter = input.getFilter();
      List<Aggregate> aggregates = input.getAggregates();
      assertAll(label,
         () ->
         {
            List<AggregateResult> results = new AggregateAction().execute(input).getResults();
            List<List<Integer>> rows = new ArrayList<>();
            for(AggregateResult result : results)
            {
               assertEquals(Set.copyOf(input.getAggregates()), result.getAggregateValues().keySet());
               assertEquals(Set.copyOf(input.getGroupBys()), result.getGroupByValues().keySet());
               List<Integer> row = new ArrayList<>();
               for(GroupBy group : input.getGroupBys())
               {
                  row.add(assertInstanceOf(Integer.class, result.getGroupByValue(group)));
               }
               for(Aggregate aggregate : input.getAggregates())
               {
                  row.add(assertInstanceOf(Integer.class, result.getAggregateValue(aggregate)));
               }
               rows.add(row);
            }
            assertEquals(expected, rows);
         },
         () -> assertTrue(inputBefore.equals(JsonUtils.toJson(input)), "Aggregate input must remain unchanged"),
         () -> assertSame(filter, input.getFilter()),
         () -> assertSame(aggregates, input.getAggregates()),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"),
         () -> assertEquals(storedBefore, storedDocuments()),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"));
   }



   /*******************************************************************************
    ** Deliberate unsupported-operation errors, not driver or null dereference errors.
    *******************************************************************************/
   private void assertRefused(String label, AggregateInput input, String reason) throws Exception
   {
      String inputBefore = JsonUtils.toJson(input);
      String metadataBefore = metadata();
      String sessionBefore = JsonUtils.toJson(session);
      Map<String, List<String>> storedBefore = storedDocuments();
      assertAll(label,
         () ->
         {
            QException failure = assertThrows(QException.class, () -> new AggregateAction().execute(input));
            Boolean foundReason = false;
            for(Throwable cause = failure; cause != null; cause = cause.getCause())
            {
               assertFalse(cause instanceof NullPointerException || cause instanceof IllegalArgumentException || cause instanceof MongoException,
                  "Refusal must be deliberate, not a malformed native request or null dereference");
               String message = cause.getMessage();
               if(message != null && (message.toLowerCase().contains(reason)
                  || ("function".equals(reason) && (message.toLowerCase().contains("adapter") || message.toLowerCase().contains("virtual")))))
               {
                  foundReason = true;
               }
            }
            assertTrue(foundReason, "Unsupported-operation reason must identify " + reason);
         },
         () -> assertTrue(inputBefore.equals(JsonUtils.toJson(input)), "Rejected input must remain unchanged"),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"),
         () -> assertEquals(storedBefore, storedDocuments()),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"));
   }



   /*******************************************************************************
    ** Count has no post-query row computation. Referenced unavailable native
    ** functions must refuse without returning a plausible zero count.
    *******************************************************************************/
   private void assertCountRefused(String label, QInputSource source, QQueryFilter filter) throws Exception
   {
      CountInput input = new CountInput();
      input.setTableName(TABLE);
      input.setInputSource(source);
      input.setFilter(filter);
      String inputBefore = JsonUtils.toJson(input);
      String metadataBefore = metadata();
      String sessionBefore = JsonUtils.toJson(session);
      Map<String, List<String>> storedBefore = storedDocuments();
      assertAll(label,
         () ->
         {
            QException failure = assertThrows(QException.class, () -> new CountAction().execute(input));
            Boolean foundReason = false;
            for(Throwable cause = failure; cause != null; cause = cause.getCause())
            {
               assertFalse(cause instanceof NullPointerException || cause instanceof IllegalArgumentException || cause instanceof MongoException,
                  "Refusal must be deliberate, not a malformed native request or null dereference");
               String message = cause.getMessage();
               if(message != null && (message.toLowerCase().contains("function") || message.toLowerCase().contains("adapter") || message.toLowerCase().contains("virtual")))
               {
                  foundReason = true;
               }
            }
            assertTrue(foundReason, "Unsupported Count predicate reason must identify its unavailable function");
         },
         () -> assertTrue(inputBefore.equals(JsonUtils.toJson(input)), "Rejected Count input must remain unchanged"),
         () -> assertSame(filter, input.getFilter()),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"),
         () -> assertEquals(storedBefore, storedDocuments()),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"));
   }



   /*******************************************************************************
    ** Independent driver aggregation with only fixed fixture-owned expressions.
    *******************************************************************************/
   private List<List<Integer>> nativeRows(List<Document> pipeline, List<String> fields)
   {
      try(MongoClient client = getMongoClient())
      {
         List<List<Integer>> rows = new ArrayList<>();
         for(Document document : client.getDatabase(TestUtils.MONGO_DATABASE).getCollection(TABLE).aggregate(pipeline))
         {
            List<Integer> row = new ArrayList<>();
            for(String field : fields)
            {
               row.add(document.getInteger(field));
            }
            rows.add(row);
         }
         return rows;
      }
   }



   /*******************************************************************************
    ** Whole-document snapshots include all three collections and nested markers.
    *******************************************************************************/
   private Map<String, List<String>> storedDocuments()
   {
      try(MongoClient client = getMongoClient())
      {
         MongoDatabase database = client.getDatabase(TestUtils.MONGO_DATABASE);
         Map<String, List<String>> result = new LinkedHashMap<>();
         for(String collection : database.listCollectionNames().into(new ArrayList<>()).stream().sorted().toList())
         {
            result.put(collection, database.getCollection(collection).find().sort(new Document("_id", 1)).map(Document::toJson).into(new ArrayList<>()));
         }
         return result;
      }
   }



   /*******************************************************************************
    ** No backend credentials are emitted in metadata assertion messages.
    *******************************************************************************/
   private String metadata()
   {
      return JsonUtils.toJson(List.of(instance.getTables(), instance.getJoins(), instance.getSecurityKeyTypes(),
         instance.getSupplementalCustomizers(), instance.getSupplementalMetaData()));
   }



   /*******************************************************************************
    ** Native nonnull numeric cardinality and value sums intentionally avoid COUNT
    ** of nullable/text operands and empty-scalar compatibility policy.
    *******************************************************************************/
   private static Document scalarGroup()
   {
      return new Document("$group", new Document("_id", null).append("count", new Document("$sum", 1)).append("sum", new Document("$sum", "$daysWorked")));
   }



   /*******************************************************************************
    ** Public input construction retains bare root field names.
    *******************************************************************************/
   private AggregateInput input(QInputSource source, Aggregate... aggregates)
   {
      return new AggregateInput(TABLE).withInputSource(source).withFilter(new QQueryFilter()).withAggregates(List.of(aggregates));
   }



   /*******************************************************************************
    ** Existing registered function; arguments and source name remain unchanged.
    *******************************************************************************/
   private static FieldFunction lengthFunction()
   {
      return new FieldFunction().withFieldName("firstName").withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER);
   }



   /*******************************************************************************
    ** Both selection and predicate capability are explicitly declared.
    *******************************************************************************/
   private static QVirtualFieldMetaData virtual(FieldFunction function)
   {
      return new QVirtualFieldMetaData(LENGTH, QFieldType.INTEGER).withIsQuerySelectable(true).withIsQueryCriteria(true).withFieldFunction(function);
   }



   /*******************************************************************************
    ** Stable physical fixture identity, independently read through the native driver.
    *******************************************************************************/
   private static Document person(Integer number, String name, Integer amount)
   {
      return new Document("_id", id(number)).append("seqNo", number).append("firstName", name).append("daysWorked", amount)
         .append("lastName", "Unchanged " + number).append("audit", new Document("marker", "keep-" + number));
   }



   /*******************************************************************************
    ** The declared foreign key is STRING; native lookup converts the BSON _id.
    *******************************************************************************/
   private static Document order(Integer number, Integer person)
   {
      return new Document("_id", id(number)).append("billToPersonId", id(person).toHexString()).append("storeKey", 1);
   }



   /*******************************************************************************
    ** Deterministic keys, no generated identity or transaction behavior under test.
    *******************************************************************************/
   private static ObjectId id(Integer number)
   {
      return new ObjectId(String.format("%024x", number));
   }



   /*******************************************************************************
    ** Small operand helpers retain the documented native INTEGER result type.
    *******************************************************************************/
   private static Aggregate count()
   {
      return new Aggregate("seqNo", AggregateOperator.COUNT);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Aggregate sum()
   {
      return new Aggregate("daysWorked", AggregateOperator.SUM);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData table()
   {
      return instance.getTable(TABLE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private MongoDBBackendMetaData backend()
   {
      return (MongoDBBackendMetaData) instance.getBackend(TestUtils.DEFAULT_BACKEND_NAME);
   }



   /*******************************************************************************
    ** Supported extension API registers a real function type, deliberately without
    ** any native Mongo adapter. Its implementation reuses existing StringLength.
    *******************************************************************************/
   public static class MissingNativeLength extends StringLengthFunction
   {
      public static final FieldFunctionTypeIdentifier IDENTIFIER = () -> "MissingMongoLength";

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public FieldFunctionTypeIdentifier getIdentifier()
      {
         return IDENTIFIER;
      }
   }



   /*******************************************************************************
    ** Actual Query callback demonstrates the non-native virtual's supported path.
    *******************************************************************************/
   public static class QueryLength implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         for(QRecord record : records)
         {
            record.setValue(LENGTH, record.getValueString("firstName").length());
         }
         return records;
      }
   }
}
