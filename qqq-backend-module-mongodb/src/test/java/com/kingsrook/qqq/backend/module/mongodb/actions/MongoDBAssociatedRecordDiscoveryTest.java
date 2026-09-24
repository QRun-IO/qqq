/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Native relationship discovery must retain ordinary read locks and context.
 *******************************************************************************/
class MongoDBAssociatedRecordDiscoveryTest extends BaseTest
{
   private final ObjectId firstId = new ObjectId();
   private final ObjectId secondId = new ObjectId();
   private final ObjectId otherId = new ObjectId();
   private QTableMetaData parent;
   private Association association;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed()
   {
      QInstance instance = QContext.getQInstance();
      parent = instance.getTable(TestUtils.TABLE_NAME_STORE);
      QTableMetaData child = instance.getTable(TestUtils.TABLE_NAME_ORDER);
      child.getField("id").setBackendName("_id");
      child.getField("storeKey").setBackendName("store_key");
      association = new Association().withName("orders").withAssociatedTableName(child.getName()).withJoinName("storeOrders");
      parent.withAssociation(association);
      instance.addJoin(new QJoinMetaData().withName("storeOrders").withLeftTable(parent.getName()).withRightTable(child.getName())
         .withJoinOn(new JoinOn("key", "storeKey")));
      getMongoClient().getDatabase(TestUtils.MONGO_DATABASE).getCollection("order").insertMany(List.of(
         new Document("_id", firstId).append("store_key", 1).append("key", 1),
         new Document("_id", secondId).append("store_key", 1).append("key", 2),
         new Document("_id", otherId).append("store_key", 2).append("key", 3)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHiddenChildrenAndRetainedKeys() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      QSession session = QContext.getQSession();
      QTableMetaData child = instance.getTable(TestUtils.TABLE_NAME_ORDER);
      var locks = child.getRecordSecurityLocks();
      assertThat(new MongoDBQueryAction().execute(new QueryInput(child.getName())).getRecords()).isEmpty();

      assertThat(discover(List.of(new AssociatedRecordDiscovery.Parent(Map.of("key", 1), List.of()))))
         .containsExactlyInAnyOrder(firstId.toHexString(), secondId.toHexString());
      assertThat(discover(List.of(new AssociatedRecordDiscovery.Parent(Map.of("key", 1), List.of(firstId.toHexString())))))
         .containsExactly(secondId.toHexString());
      assertThat(discover(List.of(new AssociatedRecordDiscovery.Parent(Map.of("key", 1), List.of(firstId.toHexString(), secondId.toHexString())))))
         .isEmpty();

      assertSame(instance, QContext.getQInstance());
      assertSame(session, QContext.getQSession());
      assertSame(child, instance.getTable(child.getName()));
      assertSame(locks, child.getRecordSecurityLocks());
      assertEquals(1, locks.size());
      assertThat(new MongoDBQueryAction().execute(new QueryInput(child.getName())).getRecords()).isEmpty();
      assertEquals(3, getMongoClient().getDatabase(TestUtils.MONGO_DATABASE).getCollection("order").countDocuments());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRetainedKeysAreScopedToEachParent() throws Exception
   {
      assertThat(discover(List.of(
         new AssociatedRecordDiscovery.Parent(Map.of("key", 1), List.of(firstId.toHexString())),
         new AssociatedRecordDiscovery.Parent(Map.of("key", 2), List.of(secondId.toHexString())))))
         .containsExactlyInAnyOrder(secondId.toHexString(), otherId.toHexString());
   }



   /*******************************************************************************
    ** Unrelated display calculations are outside a physical relationship lookup.
    *******************************************************************************/
   @Test
   void testUnrelatedVirtualFieldIsNotEvaluated() throws Exception
   {
      QTableMetaData child = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER);
      child.withField(new QFieldMetaData("unrelated", QFieldType.STRING));
      child.withVirtualField(new QVirtualFieldMetaData("unrelatedLength", QFieldType.INTEGER)
         .withIsQuerySelectable(true).withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER).withFieldName("unrelated")));
      var virtualFields = child.getVirtualFields();
      QContext.getQSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1);
      getMongoClient().getDatabase(TestUtils.MONGO_DATABASE).getCollection("order")
         .updateMany(new Document(), new Document("$set", new Document("unrelated", 42)));
      assertThrows(QException.class, () -> new MongoDBQueryAction().execute(new QueryInput(child.getName())));
      assertThat(discover(List.of(new AssociatedRecordDiscovery.Parent(Map.of("key", 1), List.of()))))
         .containsExactlyInAnyOrder(firstId.toHexString(), secondId.toHexString());
      assertSame(virtualFields, child.getVirtualFields());
      assertThrows(QException.class, () -> new MongoDBQueryAction().execute(new QueryInput(child.getName())));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Serializable> discover(List<AssociatedRecordDiscovery.Parent> parents) throws Exception
   {
      return AssociatedRecordDiscovery.findPrimaryKeys(parent, association, parents, null);
   }
}
