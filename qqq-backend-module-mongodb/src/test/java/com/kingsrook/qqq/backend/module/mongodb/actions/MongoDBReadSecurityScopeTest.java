/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native Mongo READ-scope checks using the module's existing disposable server.
 ** Numeric nonnull operands keep documented Mongo aggregate differences separate.
 ** This is provider-module evidence, not canonical sample or transaction acceptance.
 *******************************************************************************/
public class MongoDBReadSecurityScopeTest extends BaseTest
{
   private static final String TABLE = TestUtils.TABLE_NAME_PERSON;
   private static final String OWNER = "mongoReadOwner";
   private static final String ALTERNATE_OWNER = "mongoReadAlternateOwner";
   private static final String AMOUNT = "mongoReadAmount";
   private static final Set<String> FIELDS = Set.of("id", "seqNo", "firstName", "daysWorked");

   private QInstance instance;
   private QSession session;



   /*******************************************************************************
    ** Seed with the native driver; no framework write path can pre-filter the data.
    *******************************************************************************/
   @BeforeEach
   void seedNativeDocuments()
   {
      instance = QContext.getQInstance();
      session = new QSession().withSecurityKeyValue(OWNER, 1)
         .withSecurityKeyValue(ALTERNATE_OWNER, 2).withSecurityKeyValue(AMOUNT, 10);
      QContext.setQSession(session);
      for(String key : List.of(OWNER, ALTERNATE_OWNER, AMOUNT))
      {
         instance.addSecurityKeyType(new QSecurityKeyType().withName(key));
      }

      try(MongoClient client = getMongoClient())
      {
         MongoDatabase database = client.getDatabase(TestUtils.MONGO_DATABASE);
         database.getCollection(TABLE).insertMany(List.of(
            person(1, 1, "Alpha", 10), person(2, 1, "Bravo", 20),
            person(3, 2, "Charlie", 100), person(4, 3, "Delta", 1000)));
         database.getCollection(TestUtils.TABLE_NAME_STORE).insertOne(new Document("_id", objectId(99))
            .append("key", 99).append("name", "Unrelated native store"));
      }
   }



   /*******************************************************************************
    ** Existing scope is a positive control; SYSTEM still obeys canonical locks.
    *******************************************************************************/
   @Test
   void testReadAndWriteScopeControls() throws Exception
   {
      table().setRecordSecurityLocks(List.of(lock("seqNo", OWNER, RecordSecurityLock.LockScope.READ_AND_WRITE)));
      assertAll(
         () -> assertBothSources(new Document("seqNo", 1), List.of(1, 2), 30),
         this::assertFilteredBothSources);
   }



   /*******************************************************************************
    ** READ is a read restriction for all three public actions and both sources.
    *******************************************************************************/
   @Test
   void testReadOnlyScopeAcrossQueryCountAndAggregate() throws Exception
   {
      table().setRecordSecurityLocks(List.of(lock("seqNo", OWNER, RecordSecurityLock.LockScope.READ)));
      assertAll(
         () -> assertBothSources(new Document("seqNo", 1), List.of(1, 2), 30),
         this::assertFilteredBothSources);
   }



   /*******************************************************************************
    ** WRITE-only restrictions must not restrict read actions.
    *******************************************************************************/
   @Test
   void testWriteOnlyScopeDoesNotRestrictReads() throws Exception
   {
      table().setRecordSecurityLocks(List.of(lock("seqNo", OWNER, RecordSecurityLock.LockScope.WRITE)));
      assertBothSources(new Document(), List.of(1, 2, 3, 4), 1130);
   }



   /*******************************************************************************
    ** USER adds a cloned active policy; SYSTEM retains the unrestricted table.
    *******************************************************************************/
   @Test
   void testUserPersonalizedReadPolicyAndSystemControl() throws Exception
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(UserReadPolicy.class));
      QTableMetaData active = table().clone();
      active.setRecordSecurityLocks(List.of(lock("seqNo", OWNER, RecordSecurityLock.LockScope.READ)));
      assertAll(
         () -> assertReads(QInputSource.USER, new QQueryFilter(), new Document("seqNo", 1), List.of(1, 2), 30, active),
         () -> assertReads(QInputSource.SYSTEM, new QQueryFilter(), new Document(), List.of(1, 2, 3, 4), 1130, table()));
   }



   /*******************************************************************************
    ** A small independent tree case distinguishes AND/OR from flattened locks.
    *******************************************************************************/
   @Test
   void testNestedReadTreeRetainsBooleanStructure() throws Exception
   {
      MultiRecordSecurityLock ownerAndAmount = new MultiRecordSecurityLock()
         .withOperator(MultiRecordSecurityLock.BooleanOperator.AND)
         .withLock(lock("seqNo", OWNER, RecordSecurityLock.LockScope.READ))
         .withLock(lock("daysWorked", AMOUNT, RecordSecurityLock.LockScope.READ_AND_WRITE));
      table().setRecordSecurityLocks(List.of(new MultiRecordSecurityLock()
         .withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
         .withLock(ownerAndAmount)
         .withLock(lock("seqNo", ALTERNATE_OWNER, RecordSecurityLock.LockScope.READ))));
      Document nativeFilter = new Document("$or", List.of(
         new Document("seqNo", 1).append("daysWorked", 10), new Document("seqNo", 2)));
      assertBothSources(nativeFilter, List.of(1, 3), 110);
   }



   /*******************************************************************************
    ** An all-access alternative is TRUE within OR, not an absent predicate.
    *******************************************************************************/
   @Test
   void testAllAccessLeafWithinOrPreservesTrueIdentity() throws Exception
   {
      String allAccess = "mongoReadAllAmounts";
      instance.getSecurityKeyType(AMOUNT).setAllAccessKeyName(allAccess);
      session.withSecurityKeyValue(AMOUNT, 1000).withSecurityKeyValue(allAccess, false);
      table().setRecordSecurityLocks(List.of(new MultiRecordSecurityLock()
         .withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
         .withLock(lock("seqNo", OWNER, RecordSecurityLock.LockScope.READ))
         .withLock(lock("daysWorked", AMOUNT, RecordSecurityLock.LockScope.READ_AND_WRITE))));
      Document nativeWithoutAllAccess = new Document("$or", List.of(new Document("seqNo", 1),
         new Document("daysWorked", new Document("$in", List.of(10, 1000)))));
      assertAll(
         () -> assertBothSources(nativeWithoutAllAccess, List.of(1, 2, 4), 1030),
         () ->
         {
            session.withSecurityKeyValue(allAccess, true);
            assertBothSources(new Document(), List.of(1, 2, 3, 4), 1130);
         });
   }



   /*******************************************************************************
    ** TRUE inside OR must not erase a surrounding AND restriction. A subtree
    ** containing only WRITE locks contributes no read restriction after pruning.
    *******************************************************************************/
   @Test
   void testOuterAndAndWriteOnlySubtreeControls() throws Exception
   {
      String allAccess = "mongoReadNestedAllAmounts";
      instance.getSecurityKeyType(AMOUNT).setAllAccessKeyName(allAccess);
      session.withSecurityKeyValue(allAccess, true);
      assertAll(
         () ->
         {
            table().setRecordSecurityLocks(List.of(new MultiRecordSecurityLock()
               .withOperator(MultiRecordSecurityLock.BooleanOperator.AND)
               .withLock(new MultiRecordSecurityLock().withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
                  .withLock(lock("seqNo", ALTERNATE_OWNER, RecordSecurityLock.LockScope.READ))
                  .withLock(lock("daysWorked", AMOUNT, RecordSecurityLock.LockScope.READ_AND_WRITE)))
               .withLock(lock("seqNo", OWNER, RecordSecurityLock.LockScope.READ))));
            assertBothSources(new Document("seqNo", 1), List.of(1, 2), 30);
         },
         () ->
         {
            table().setRecordSecurityLocks(List.of(new MultiRecordSecurityLock()
               .withOperator(MultiRecordSecurityLock.BooleanOperator.AND)
               .withLock(new MultiRecordSecurityLock().withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
                  .withLock(lock("seqNo", ALTERNATE_OWNER, RecordSecurityLock.LockScope.WRITE)))
               .withLock(lock("seqNo", OWNER, RecordSecurityLock.LockScope.READ))));
            assertBothSources(new Document("seqNo", 1), List.of(1, 2), 30);
         });
   }



   /*******************************************************************************
    ** Existing STRING metadata maps a logical owner to a nested native BSON path.
    *******************************************************************************/
   @Test
   void testMappedStringOwnerPreservesNativeTypeAndPath() throws Exception
   {
      seedStringOwners(false);
      session.withSecurityKeyValue("mongoReadTextOwner", "blue-team");
      table().setRecordSecurityLocks(List.of(lock("homeTown", "mongoReadTextOwner", RecordSecurityLock.LockScope.READ)));
      assertBothSources(new Document("access.owner", "blue-team"), List.of(1, 2), 30);
   }



   /*******************************************************************************
    ** Null/missing stored owners are distinct from a valid owner key. No-match
    ** controls use Query/Count so Mongo empty aggregate policy stays separate.
    *******************************************************************************/
   @Test
   void testNullAndMissingOwnerWithMissingEmptyAndNullSessionKeys() throws Exception
   {
      seedStringOwners(true);
      session.withSecurityKeyValue("mongoReadTextOwner", "blue-team");
      RecordSecurityLock owner = lock("homeTown", "mongoReadTextOwner", RecordSecurityLock.LockScope.READ);
      table().setRecordSecurityLocks(List.of(owner));
      assertAll(
         () -> assertBothSources(new Document("access.owner", "blue-team"), List.of(1), 10),
         () ->
         {
            session.withSecurityKeyValue("mongoReadTextOwner", null);
            assertAll("Mixed valid/null key with DENY",
               () -> assertBothSources(new Document("access.owner", "blue-team"), List.of(1), 10));
         },
         () ->
         {
            owner.setNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW_WRITE_ONLY);
            assertAll("Mixed valid/null key with ALLOW_WRITE_ONLY must still deny null-owner reads",
               () -> assertBothSources(new Document("access.owner", "blue-team"), List.of(1), 10));
         },
         () ->
         {
            owner.setNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW);
            Document nativeFilter = new Document("$or", List.of(new Document("access.owner", "blue-team"), new Document("access.owner", null)));
            assertBothSources(nativeFilter, List.of(1, 3, 4), 1110);
         },
         () ->
         {
            List<Executable> checks = new ArrayList<>();
            for(QSession noKey : List.of(new QSession(),
               new QSession().withSecurityKeyValues(Map.of("mongoReadTextOwner", List.of())),
               new QSession().withSecurityKeyValue("mongoReadTextOwner", null)))
            {
               checks.add(() ->
               {
                  session = noKey;
                  QContext.setQSession(session);
                  assertAll("Missing/empty/null owner key: " + JsonUtils.toJson(noKey.getSecurityKeyValues()),
                     () ->
                     {
                        owner.setNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW);
                        assertBothSources(new Document("access.owner", null), List.of(3, 4), 1100);
                     },
                     () ->
                     {
                        List<Executable> readDenials = new ArrayList<>();
                        for(RecordSecurityLock.NullValueBehavior behavior : List.of(RecordSecurityLock.NullValueBehavior.DENY, RecordSecurityLock.NullValueBehavior.ALLOW_WRITE_ONLY))
                        {
                           readDenials.add(() ->
                           {
                              owner.setNullValueBehavior(behavior);
                              assertAll(behavior + " must deny null-owner reads",
                                 () -> assertNoMatches(QInputSource.USER),
                                 () -> assertNoMatches(QInputSource.SYSTEM));
                           });
                        }
                        assertAll(readDenials);
                     });
               });
            }
            assertAll(checks);
         });
   }



   /*******************************************************************************
    ** The existing registered Person-to-Order path is deliberately unsupported
    ** by Mongo security. Its explicit refusal must survive the tree correction.
    *******************************************************************************/
   @Test
   void testRegisteredJoinChainStillRefusesBeforePublishingResults() throws Exception
   {
      RecordSecurityLock chain = lock("order.storeKey", TestUtils.TABLE_NAME_STORE, RecordSecurityLock.LockScope.READ_AND_WRITE)
         .withJoinNameChain(List.of("orderJoinBillToPerson"));
      table().setRecordSecurityLocks(List.of(chain));
      assertAll(
         () -> assertUnsupportedChain(QInputSource.USER),
         () -> assertUnsupportedChain(QInputSource.SYSTEM));
   }



   /*******************************************************************************
    ** Driver-only fixture variation; existing public row fields remain unchanged.
    *******************************************************************************/
   private void seedStringOwners(Boolean includeNullAndMissing)
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("mongoReadTextOwner"));
      table().getField("homeTown").setBackendName("access.owner");
      try(MongoClient client = getMongoClient())
      {
         MongoCollection<Document> collection = client.getDatabase(TestUtils.MONGO_DATABASE).getCollection(TABLE);
         for(Integer id : List.of(1, 2, 3, 4))
         {
            Object owner = id <= 2 ? "blue-team" : id == 3 ? "red-team" : "green-team";
            if(includeNullAndMissing)
            {
               owner = id == 1 ? "blue-team" : id == 2 ? "red-team" : null;
            }
            Document change = includeNullAndMissing && id == 4
               ? new Document("$unset", new Document("access.owner", ""))
               : new Document("$set", new Document("access.owner", owner));
            assertEquals(1L, collection.updateOne(new Document("_id", objectId(id)), change).getMatchedCount());
         }
         if(includeNullAndMissing)
         {
            assertEquals(1L, collection.countDocuments(new Document("access.owner", new Document("$type", 10))), "One explicit BSON null owner");
            assertEquals(1L, collection.countDocuments(new Document("access.owner", new Document("$exists", false))), "One absent owner path");
         }
      }
   }



   /*******************************************************************************
    ** Require zero native matches without prescribing empty Aggregate output.
    *******************************************************************************/
   private void assertNoMatches(QInputSource source) throws Exception
   {
      QueryInput query = new QueryInput(TABLE).withInputSource(source).withFieldNamesToInclude(FIELDS).withFilter(new QQueryFilter());
      CountInput count = new CountInput();
      count.setTableName(TABLE);
      count.setInputSource(source);
      count.setFilter(new QQueryFilter());
      List<ObjectNode> inputs = List.of(callerInput(query), callerInput(count));
      List<QQueryFilter> filters = List.of(query.getFilter(), count.getFilter());
      Map<String, List<String>> stored = storedDocuments();
      String metadata = metadata();
      String active = JsonUtils.toJson(table());
      String sessionJson = JsonUtils.toJson(session);
      try(MongoClient client = getMongoClient())
      {
         assertEquals(0L, client.getDatabase(TestUtils.MONGO_DATABASE).getCollection(TABLE)
            .countDocuments(new Document("access.owner", new Document("$in", List.of()))), "Independent empty owner-set oracle");
      }
      assertAll(source + " missing-key DENY",
         () -> assertTrue(new QueryAction().execute(query).getRecords().isEmpty(), "Query must publish no records"),
         () -> assertEquals(0, new CountAction().execute(count).getCount(), "Count must be zero"),
         () -> assertInput(query, inputs.get(0), filters.get(0), query.getFilter(), active),
         () -> assertInput(count, inputs.get(1), filters.get(1), count.getFilter(), active),
         () -> assertEquals(stored, storedDocuments()),
         () -> assertTrue(metadata.equals(metadata()), "Canonical metadata must remain unchanged"),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertTrue(sessionJson.equals(JsonUtils.toJson(session)), "Session must remain unchanged"));
   }



   /*******************************************************************************
    ** Each common action must reach Mongo's declared unsupported-chain boundary.
    *******************************************************************************/
   private void assertUnsupportedChain(QInputSource source) throws Exception
   {
      QueryInput query = new QueryInput(TABLE).withInputSource(source).withFieldNamesToInclude(FIELDS).withFilter(new QQueryFilter());
      CountInput count = new CountInput();
      count.setTableName(TABLE);
      count.setInputSource(source);
      count.setFilter(new QQueryFilter());
      AggregateInput aggregate = new AggregateInput(TABLE).withInputSource(source).withFilter(new QQueryFilter())
         .withAggregates(List.of(new Aggregate("daysWorked", AggregateOperator.COUNT), new Aggregate("daysWorked", AggregateOperator.SUM)));
      List<ObjectNode> inputs = List.of(callerInput(query), callerInput(count), callerInput(aggregate));
      List<QQueryFilter> filters = List.of(query.getFilter(), count.getFilter(), aggregate.getFilter());
      Map<String, List<String>> stored = storedDocuments();
      String metadata = metadata();
      String active = JsonUtils.toJson(table());
      String sessionJson = JsonUtils.toJson(session);
      assertAll(source + " unsupported registered chain",
         () -> assertChainFailure(() -> new QueryAction().execute(query)),
         () -> assertChainFailure(() -> new CountAction().execute(count)),
         () -> assertChainFailure(() -> new AggregateAction().execute(aggregate)),
         () -> assertInput(query, inputs.get(0), filters.get(0), query.getFilter(), active),
         () -> assertInput(count, inputs.get(1), filters.get(1), count.getFilter(), active),
         () -> assertInput(aggregate, inputs.get(2), filters.get(2), aggregate.getFilter(), active),
         () -> assertEquals(stored, storedDocuments()),
         () -> assertTrue(metadata.equals(metadata()), "Canonical metadata must remain unchanged"),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertTrue(sessionJson.equals(JsonUtils.toJson(session)), "Session must remain unchanged"));
   }



   /*******************************************************************************
    ** Check the actual unsupported-operation reason, not an arbitrary QException.
    *******************************************************************************/
   private void assertChainFailure(Executable action)
   {
      QException failure = assertThrows(QException.class, action);
      Boolean found = false;
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         String message = cause.getMessage();
         if(message != null && message.contains("joinNameChain") && message.contains("not yet supported"))
         {
            found = true;
         }
      }
      assertTrue(found, "Expected Mongo unsupported security joinNameChain refusal");
   }



   /*******************************************************************************
    ** Run source controls independently so one missing scope cannot hide another.
    *******************************************************************************/
   private void assertBothSources(Document nativeFilter, List<Integer> ids, Integer sum) throws Exception
   {
      assertAll(
         () -> assertReads(QInputSource.USER, new QQueryFilter(), nativeFilter, ids, sum, table()),
         () -> assertReads(QInputSource.SYSTEM, new QQueryFilter(), nativeFilter, ids, sum, table()));
   }



   /*******************************************************************************
    ** The caller's OR filter must be intersected with the security restriction.
    *******************************************************************************/
   private void assertFilteredBothSources() throws Exception
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Bravo"))
         .withCriteria(new QFilterCriteria("daysWorked", QCriteriaOperator.GREATER_THAN, 50));
      Document nativeFilter = new Document("$and", List.of(new Document("seqNo", 1),
         new Document("$or", List.of(new Document("firstName", "Bravo"), new Document("daysWorked", new Document("$gt", 50))))));
      assertAll(
         () -> assertReads(QInputSource.USER, filter.clone(), nativeFilter, List.of(2), 20, table()),
         () -> assertReads(QInputSource.SYSTEM, filter.clone(), nativeFilter, List.of(2), 20, table()));
   }



   /*******************************************************************************
    ** Each action has its own request and native oracle. Preservation assertions
    ** also run after an action/result assertion fails, without replacing that failure.
    *******************************************************************************/
   private void assertReads(QInputSource source, QQueryFilter filter, Document nativeFilter, List<Integer> ids, Integer sum, QTableMetaData expectedTable) throws Exception
   {
      QueryInput query = new QueryInput(TABLE).withInputSource(source).withFieldNamesToInclude(FIELDS)
         .withFilter(filter.clone().withOrderBy(new QFilterOrderBy("id")));
      CountInput count = new CountInput();
      count.setTableName(TABLE);
      count.setInputSource(source);
      count.setFilter(filter.clone());
      Aggregate cardinality = new Aggregate("daysWorked", AggregateOperator.COUNT);
      Aggregate total = new Aggregate("daysWorked", AggregateOperator.SUM);
      AggregateInput aggregate = new AggregateInput(TABLE).withInputSource(source)
         .withFilter(filter.clone()).withAggregates(List.of(cardinality, total));

      Map<String, List<String>> storedBefore = storedDocuments();
      String metadataBefore = metadata();
      String sessionBefore = JsonUtils.toJson(session);
      String tableBefore = JsonUtils.toJson(expectedTable);
      List<ObjectNode> inputsBefore = List.of(callerInput(query), callerInput(count), callerInput(aggregate));
      List<QQueryFilter> filters = List.of(query.getFilter(), count.getFilter(), aggregate.getFilter());
      NativeExpectation expected = nativeExpectation(nativeFilter);
      assertAll("Native oracle",
         () -> assertEquals(ids.stream().map(id -> objectId(id).toHexString()).toList(), expected.rows().stream().map(row -> row.get(0)).toList()),
         () -> assertEquals(ids.size(), expected.count()),
         () -> assertEquals(sum, expected.sum()));

      assertAll(source + " / " + filter,
         () ->
         {
            List<QRecord> records = new QueryAction().execute(query).getRecords();
            List<Executable> checks = new ArrayList<>();
            for(QRecord record : records)
            {
               checks.add(() -> assertEquals(FIELDS, record.getValues().keySet(), "Exact selected fields"));
               checks.add(() -> assertInstanceOf(String.class, record.getValue("id")));
               checks.add(() -> assertInstanceOf(Integer.class, record.getValue("seqNo")));
               checks.add(() -> assertInstanceOf(String.class, record.getValue("firstName")));
               checks.add(() -> assertInstanceOf(Integer.class, record.getValue("daysWorked")));
            }
            checks.add(() -> assertEquals(expected.rows(), records.stream().map(record -> List.of(
               (Object) record.getValue("id"), record.getValue("seqNo"), record.getValue("firstName"), record.getValue("daysWorked"))).toList()));
            assertAll("Query", checks);
         },
         () -> assertEquals(expected.count(), new CountAction().execute(count).getCount(), "Count"),
         () ->
         {
            AggregateOutput output = new AggregateAction().execute(aggregate);
            assertEquals(1, output.getResults().size(), "Aggregate scalar row");
            AggregateResult result = output.getResults().get(0);
            assertAll("Aggregate",
               () -> assertEquals(Set.of(cardinality, total), result.getAggregateValues().keySet()),
               () -> assertTrue(result.getGroupByValues().isEmpty()),
               () -> assertEquals(expected.count(), assertInstanceOf(Integer.class, result.getAggregateValue(cardinality))),
               () -> assertEquals(expected.sum(), assertInstanceOf(Integer.class, result.getAggregateValue(total))));
         },
         () -> assertAll("Inputs",
            () -> assertInput(query, inputsBefore.get(0), filters.get(0), query.getFilter(), tableBefore),
            () -> assertInput(count, inputsBefore.get(1), filters.get(1), count.getFilter(), tableBefore),
            () -> assertInput(aggregate, inputsBefore.get(2), filters.get(2), aggregate.getFilter(), tableBefore)),
         () -> assertEquals(storedBefore, storedDocuments(), "All seeded collections and complete documents remain unchanged"),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"));
   }



   /*******************************************************************************
    ** Actions intentionally replace resolved table metadata with an active clone.
    ** Check that separately; retain every other serialized caller input property.
    *******************************************************************************/
   private ObjectNode callerInput(AbstractTableActionInput input) throws Exception
   {
      ObjectNode snapshot = JsonUtils.toObject(JsonUtils.toJson(input), ObjectNode.class);
      snapshot.remove(List.of("table", "tableMetaData"));
      return snapshot;
   }



   /*******************************************************************************
    ** Input objects/filters retain their original values and caller-owned references.
    *******************************************************************************/
   private void assertInput(AbstractTableActionInput input, ObjectNode before, QQueryFilter originalFilter, QQueryFilter currentFilter, String expectedTable) throws Exception
   {
      assertAll(
         () -> assertTrue(before.equals(callerInput(input)), "Caller input properties must remain unchanged"),
         () -> assertSame(originalFilter, currentFilter, "Caller filter reference"),
         () -> assertTrue(expectedTable.equals(JsonUtils.toJson(input.getTable())), "Expected active table metadata"),
         () -> assertSame(input.getTable(), input.getTableMetaData(), "Resolved metadata aliases"));
   }



   /*******************************************************************************
    ** Driver queries independently enforce the intended owner/filter predicates.
    *******************************************************************************/
   private NativeExpectation nativeExpectation(Document filter)
   {
      try(MongoClient client = getMongoClient())
      {
         MongoCollection<Document> collection = client.getDatabase(TestUtils.MONGO_DATABASE).getCollection(TABLE);
         List<List<Object>> rows = new ArrayList<>();
         for(Document document : collection.find(filter).sort(new Document("_id", 1)))
         {
            rows.add(List.of(document.getObjectId("_id").toHexString(), document.getInteger("seqNo"),
               document.getString("firstName"), document.getInteger("daysWorked")));
         }
         Document totals = collection.aggregate(List.of(new Document("$match", filter),
            new Document("$group", new Document("_id", null).append("count", new Document("$sum", 1))
               .append("sum", new Document("$sum", "$daysWorked"))))).first();
         return new NativeExpectation(rows, totals.getInteger("count"), totals.getInteger("sum"));
      }
   }



   /*******************************************************************************
    ** Full BSON documents, including unrelated fields and the separate store table.
    *******************************************************************************/
   private Map<String, List<String>> storedDocuments()
   {
      try(MongoClient client = getMongoClient())
      {
         MongoDatabase database = client.getDatabase(TestUtils.MONGO_DATABASE);
         Map<String, List<String>> result = new LinkedHashMap<>();
         for(String collection : database.listCollectionNames().into(new ArrayList<>()).stream().sorted().toList())
         {
            result.put(collection, database.getCollection(collection).find().sort(new Document("_id", 1))
               .map(Document::toJson).into(new ArrayList<>()));
         }
         return result;
      }
   }



   /*******************************************************************************
    ** Complete table/join/key/customizer metadata; backend connection settings are
    ** fixture-owned and never printed in assertion failures.
    *******************************************************************************/
   private String metadata()
   {
      return JsonUtils.toJson(List.of(instance.getTables(), instance.getJoins(), instance.getSecurityKeyTypes(), instance.getSupplementalCustomizers()));
   }



   /*******************************************************************************
    ** Existing numeric fields express ownership and values without new schema.
    *******************************************************************************/
   private static Document person(Integer id, Integer owner, String name, Integer amount)
   {
      return new Document("_id", objectId(id)).append("seqNo", owner).append("firstName", name)
         .append("lastName", "Stored surname " + id).append("daysWorked", amount)
         .append("email", "person" + id + "@example.invalid")
         .append("audit", new Document("marker", "untouched-" + id).append("version", 1));
   }



   /*******************************************************************************
    ** Stable native keys make exact selection independent of generated identities.
    *******************************************************************************/
   private static ObjectId objectId(Integer value)
   {
      return new ObjectId(String.format("%024x", value));
   }



   /*******************************************************************************
    ** All operands are INTEGER; this slice makes no null/key-conversion claim.
    *******************************************************************************/
   private static RecordSecurityLock lock(String field, String key, RecordSecurityLock.LockScope scope)
   {
      return new RecordSecurityLock().withFieldName(field).withSecurityKeyType(key).withLockScope(scope)
         .withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY);
   }



   /*******************************************************************************
    ** Canonical table belongs to this test's per-method QInstance.
    *******************************************************************************/
   private QTableMetaData table()
   {
      return instance.getTable(TABLE);
   }



   /*******************************************************************************
    ** Native oracle retains result types rather than formatting numbers as strings.
    *******************************************************************************/
   private record NativeExpectation(List<List<Object>> rows, Integer count, Integer sum)
   {
   }



   /*******************************************************************************
    ** Source-dependent metadata is cloned; no authorization bypass is inferred.
    *******************************************************************************/
   public static class UserReadPolicy implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !TABLE.equals(input.getTableName()))
         {
            return input.getTable();
         }
         QTableMetaData result = input.getTable().clone();
         result.setRecordSecurityLocks(List.of(lock("seqNo", OWNER, RecordSecurityLock.LockScope.READ)));
         return result;
      }
   }
}
