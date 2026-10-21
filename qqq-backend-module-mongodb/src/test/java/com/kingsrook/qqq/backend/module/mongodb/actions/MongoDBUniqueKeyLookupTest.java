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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyLookup;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.statusmessages.DuplicateKeyBadInputStatusMessage;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;


/*******************************************************************************
 ** Native Mongo key lookups preserve mapped ObjectIds and normal read visibility.
 *******************************************************************************/
class MongoDBUniqueKeyLookupTest extends BaseTest
{
   private final ObjectId firstId = new ObjectId();
   private final ObjectId secondId = new ObjectId();
   private QTableMetaData table;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed()
   {
      table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER);
      table.getField("id").setBackendName("_id");
      table.getField("storeKey").setBackendName("store_key");
      getMongoClient().getDatabase(TestUtils.MONGO_DATABASE).getCollection("order").insertMany(List.of(
         new Document("_id", firstId).append("store_key", 1).append("key", 1),
         new Document("_id", secondId).append("store_key", 1).append("key", 2)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFindsAllHiddenOwnersWithoutChangingVisibility() throws Exception
   {
      UniqueKey key = new UniqueKey("storeKey");
      table.withUniqueKey(key);
      var session = QContext.getQSession();
      var locks = table.getRecordSecurityLocks();
      assertThat(new MongoDBQueryAction().execute(new QueryInput(table.getName())).getRecords()).isEmpty();
      assertThat(UniqueKeyLookup.findConflicts(table, key, new QRecord().withValue("storeKey", "1"), null))
         .extracting(record -> record.getValueString("id")).containsExactlyInAnyOrder(firstId.toHexString(), secondId.toHexString());
      assertThat(new MongoDBQueryAction().execute(new QueryInput(table.getName())).getRecords()).isEmpty();
      assertSame(session, QContext.getQSession());
      assertSame(locks, table.getRecordSecurityLocks());
      assertEquals(2, getMongoClient().getDatabase(TestUtils.MONGO_DATABASE).getCollection("order").countDocuments());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMappedHiddenHeavyStoredComponents() throws Exception
   {
      table.withUniqueKey(new UniqueKey("storeKey", "key"));
      table.getField("storeKey").withIsHidden(true).withIsHeavy(true);
      table.getField("id").withIsHidden(true).withIsHeavy(true);
      List<QRecord> stored = UniqueKeyLookup.readStoredComponents(table, List.of(secondId.toHexString()), null);
      assertEquals(1, stored.size());
      assertEquals(secondId.toHexString(), stored.get(0).getValueString("id"));
      assertEquals(1, stored.get(0).getValueInteger("storeKey"));
      assertEquals(2, stored.get(0).getValueInteger("key"));
   }



   /*******************************************************************************
    ** Physical absence is explicit null in a private, exact parent tuple projection.
    *******************************************************************************/
   @Test
   void testStoredAssociationValuesPreserveMappedKeysAndMissingNestedValues() throws Exception
   {
      table.getField("id").withIsHidden(true).withIsHeavy(true);
      table.getField("storeKey").withIsHidden(true).withIsHeavy(true);
      table.withField(new QFieldMetaData("nullableCode", QFieldType.STRING).withBackendName("details.code").withIsHidden(true).withIsHeavy(true));
      Association association = new Association().withName("stored relationship").withAssociatedTableName(table.getName()).withJoinName("storedParentJoin");
      table.withAssociation(association);
      QContext.getQInstance().addJoin(new QJoinMetaData().withName(association.getJoinName()).withLeftTable(table.getName()).withRightTable(table.getName())
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("storeKey", "storeKey")).withJoinOn(new JoinOn("nullableCode", "nullableCode")));
      var collection = getMongoClient().getDatabase(TestUtils.MONGO_DATABASE).getCollection("order");
      List<Document> before = collection.find().into(new ArrayList<>());
      var session = QContext.getQSession();
      var locks = table.getRecordSecurityLocks();
      assertThat(new MongoDBQueryAction().execute(new QueryInput(table.getName())).getRecords()).isEmpty();
      assertThat(AssociatedRecordDiscovery.readParentValues(table, List.of(association), List.of(secondId.toHexString()), null))
         .singleElement().satisfies(record -> assertThat(record.getValues()).containsOnlyKeys("id", "storeKey", "nullableCode")
            .containsEntry("id", secondId.toHexString()).containsEntry("storeKey", 1).containsEntry("nullableCode", null));
      assertThat(new MongoDBQueryAction().execute(new QueryInput(table.getName())).getRecords()).isEmpty();
      assertEquals(before, collection.find().into(new ArrayList<>()));
      assertSame(session, QContext.getQSession());
      assertSame(locks, table.getRecordSecurityLocks());
   }



   /*******************************************************************************
    ** The native collection has no physical unique index to hide missing validation.
    *******************************************************************************/
   @Test
   void testSparseUpdateRejectsCompositeCollision() throws Exception
   {
      table.withUniqueKey(new UniqueKey("storeKey", "key"));
      QContext.getQSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1);
      assertThat(new MongoDBQueryAction().execute(new QueryInput(table.getName())).getRecords())
         .extracting(record -> record.getValueString("id")).contains(secondId.toHexString());
      QRecord patch = new QRecord().withValue("id", secondId.toHexString()).withValue("key", 1);
      QRecord result = new UpdateAction().execute(new UpdateInput(table.getName()).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertThat(result.getErrors()).anyMatch(error -> error instanceof DuplicateKeyBadInputStatusMessage);
      assertEquals(2, getMongoClient().getDatabase(TestUtils.MONGO_DATABASE).getCollection("order").find(new Document("_id", secondId)).first().getInteger("key"));
      assertFalse(patch.getValues().containsKey("storeKey"));
   }



   /*******************************************************************************
    ** Top-level physical absence remains null-distinct in ordinary and native reads.
    *******************************************************************************/
   @Test
   void testMissingTopLevelComponentAllowsNullDistinctSparseUpdate() throws Exception
   {
      table.withField(new QFieldMetaData("nullableCode", QFieldType.STRING).withBackendName("nullable_code"))
         .withUniqueKey(new UniqueKey("key", "nullableCode"));
      QContext.getQSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1);
      var collection = getMongoClient().getDatabase(TestUtils.MONGO_DATABASE).getCollection("order");
      Document target = collection.find(new Document("_id", secondId)).first();
      Document owner = collection.find(new Document("_id", firstId)).first();
      assertThat(UniqueKeyLookup.readStoredComponents(table, List.of(secondId.toHexString()), null).get(0).getValues())
         .containsEntry("nullableCode", null).containsEntry("key", 2);
      assertThat(new MongoDBQueryAction().execute(new QueryInput(table.getName())).getRecords())
         .hasSize(2).allSatisfy(record -> assertThat(record.getValues()).containsEntry("nullableCode", null));
      assertEquals(target, collection.find(new Document("_id", secondId)).first());
      QRecord patch = new QRecord().withValue("id", secondId.toHexString()).withValue("key", 1);
      QRecord result = new UpdateAction().execute(new UpdateInput(table.getName()).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertThat(result.getErrors()).isNullOrEmpty();
      assertThat(patch.getValues()).doesNotContainKey("nullableCode");
      assertEquals(new Document(target).append("key", 1), collection.find(new Document("_id", secondId)).first());
      assertEquals(owner, collection.find(new Document("_id", firstId)).first());
   }



   /*******************************************************************************
    ** An absent nested component cannot consume an ancestor's same-named leaf.
    *******************************************************************************/
   @Test
   void testMissingNestedComponentPreservesAncestorAndNullDistinctUpdate()
   {
      assertAll(
         () -> assertMissingNestedComponent(false, false),
         () -> assertMissingNestedComponent(false, true),
         () -> assertMissingNestedComponent(true, false),
         () -> assertMissingNestedComponent(true, true));
   }



   /*******************************************************************************
    ** Exercise both field orders and absent versus malformed intermediate objects.
    *******************************************************************************/
   private void assertMissingNestedComponent(boolean rootFieldFirst, boolean scalarParent) throws Exception
   {
      QFieldMetaData nestedField = new QFieldMetaData("nestedCode", QFieldType.STRING).withBackendName("details.code");
      QFieldMetaData rootField = new QFieldMetaData("rootCode", QFieldType.STRING).withBackendName("code");
      table.getFields().remove("nestedCode");
      table.getFields().remove("rootCode");
      table.withField(rootFieldFirst ? rootField : nestedField).withField(rootFieldFirst ? nestedField : rootField);
      table.setUniqueKeys(List.of(new UniqueKey("nestedCode", "rootCode")));
      QContext.getQSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1);
      var collection = getMongoClient().getDatabase(TestUtils.MONGO_DATABASE).getCollection("order");
      Document target = new Document("_id", secondId).append("store_key", 1).append("key", 2).append("code", "X");
      if(scalarParent)
      {
         target.append("details", "not a document");
      }
      Document owner = new Document("_id", firstId).append("store_key", 1).append("key", 1)
         .append("code", "Y").append("details", new Document("code", "X"));
      collection.replaceOne(new Document("_id", secondId), target);
      collection.replaceOne(new Document("_id", firstId), owner);

      QRecord stored = UniqueKeyLookup.readStoredComponents(table, List.of(secondId.toHexString()), null).get(0);
      assertThat(stored.getValues()).containsOnlyKeys("id", "nestedCode", "rootCode")
         .containsEntry("nestedCode", null).containsEntry("rootCode", "X");
      for(QueryInput query : List.of(new QueryInput(table.getName()),
         new QueryInput(table.getName()).withFieldNamesToInclude(Set.of("id", "nestedCode", "rootCode"))))
      {
         List<QRecord> records = new MongoDBQueryAction().execute(query).getRecords();
         assertThat(records).filteredOn(record -> secondId.toHexString().equals(record.getValueString("id"))).singleElement()
            .satisfies(record -> assertThat(record.getValues()).containsEntry("nestedCode", null).containsEntry("rootCode", "X"));
         assertThat(records).filteredOn(record -> firstId.toHexString().equals(record.getValueString("id"))).singleElement()
            .satisfies(record -> assertThat(record.getValues()).containsEntry("nestedCode", "X").containsEntry("rootCode", "Y"));
      }
      assertEquals(target, collection.find(new Document("_id", secondId)).first());
      QRecord patch = new QRecord().withValue("id", secondId.toHexString()).withValue("rootCode", "Y");
      QRecord result = new UpdateAction().execute(new UpdateInput(table.getName()).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertThat(result.getErrors()).isNullOrEmpty();
      assertThat(patch.getValues()).doesNotContainKey("nestedCode");
      assertEquals(new Document(target).append("code", "Y"), collection.find(new Document("_id", secondId)).first());
      assertEquals(owner, collection.find(new Document("_id", firstId)).first());
   }
}
