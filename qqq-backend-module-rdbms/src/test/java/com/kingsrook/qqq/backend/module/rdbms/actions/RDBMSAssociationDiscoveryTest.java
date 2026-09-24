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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native structural discovery and JDBC ownership use real H2 statements.
 *******************************************************************************/
class RDBMSAssociationDiscoveryTest extends RDBMSActionTest
{
   private QTableMetaData parent;
   private QTableMetaData child;
   private Association association;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      primeTestDatabase();
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
          Statement statement = connection.createStatement())
      {
         statement.execute("CREATE TABLE discovery_parent (id INT PRIMARY KEY, code VARCHAR(20), tenant INT)");
         statement.execute("CREATE TABLE discovery_child (child_key INT PRIMARY KEY, parent_code VARCHAR(20))");
         statement.execute("INSERT INTO discovery_parent VALUES (1, 'ALPHA', 99), (2, 'BETA', 99)");
         statement.execute("INSERT INTO discovery_child VALUES (10, 'ALPHA'), (20, 'ALPHA'), (30, 'BETA')");
      }
      association = new Association().withName("arbitraryGroup").withAssociatedTableName("discoveryChild").withJoinName("discoveryJoin");
      parent = new QTableMetaData().withName("discoveryParent").withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("discovery_parent"))
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("code", QFieldType.STRING)).withField(new QFieldMetaData("tenant", QFieldType.INTEGER))
         .withAssociation(association);
      child = new QTableMetaData().withName("discoveryChild").withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("discovery_child"))
         .withPrimaryKeyField("childKey").withField(new QFieldMetaData("childKey", QFieldType.INTEGER).withBackendName("child_key").withIsHidden(true).withIsHeavy(true))
         .withField(new QFieldMetaData("parentCode", QFieldType.STRING).withBackendName("parent_code").withBehavior(CaseChangeBehavior.TO_UPPER_CASE));
      QContext.getQInstance().addTable(parent);
      QContext.getQInstance().addTable(child);
      QContext.getQInstance().addJoin(new QJoinMetaData().withName("discoveryJoin").withLeftTable(parent.getName()).withRightTable(child.getName())
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("code", "parentCode")));
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("discoveryTenant"));
      child.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("discoveryTenant").withFieldName("discoveryParent.tenant")
         .withJoinNameChain(List.of("discoveryJoin")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void removeFixtures() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
          Statement statement = connection.createStatement())
      {
         statement.execute("DROP TABLE IF EXISTS discovery_child");
         statement.execute("DROP TABLE IF EXISTS discovery_parent");
      }
   }



   /*******************************************************************************
    ** The parent security join hides ordinary reads but cannot hide affected IDs.
    *******************************************************************************/
   @Test
   void testFindsStructurallyRelatedRowsBehindSecurityJoin() throws Exception
   {
      assertThat(new QueryAction().execute(new QueryInput(child.getName())).getRecords()).isEmpty();
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association,
         List.of(new AssociatedRecordDiscovery.Parent(Map.of("code", "alpha"), List.of("20"))), null)).containsExactly(10);
      assertThat(new QueryAction().execute(new QueryInput(child.getName())).getRecords()).isEmpty();
      assertEquals(1, child.getRecordSecurityLocks().size());
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(TestUtils.defineBackend())))
      {
         try(Statement statement = transaction.getConnection().createStatement())
         {
            statement.execute("INSERT INTO discovery_child VALUES (40, 'ALPHA')");
         }
         assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association,
            List.of(new AssociatedRecordDiscovery.Parent(Map.of("code", "ALPHA"), List.of())), transaction)).containsExactlyInAnyOrder(10, 20, 40);
         assertFalse(transaction.getConnection().isClosed());
         transaction.rollback();
      }
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association,
         List.of(new AssociatedRecordDiscovery.Parent(Map.of("code", "ALPHA"), List.of())), null)).containsExactlyInAnyOrder(10, 20);
   }



   /*******************************************************************************
    ** Stored parent tuples retain raw mapped fields and the caller's transaction.
    *******************************************************************************/
   @Test
   void testStoredParentValuesBehindLocksAndWithinTransaction() throws Exception
   {
      parent.getField("id").withIsHidden(true).withIsHeavy(true);
      parent.getFields().remove("code");
      parent.withField(new QFieldMetaData("storedCode", QFieldType.STRING).withBackendName("code").withIsHidden(true).withIsHeavy(true));
      QContext.getQInstance().getJoin(association.getJoinName()).setJoinOns(List.of(new JoinOn("storedCode", "parentCode")));
      parent.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("discoveryTenant").withFieldName("tenant"));
      var session = QContext.getQSession();
      var locks = parent.getRecordSecurityLocks();
      assertThat(new QueryAction().execute(new QueryInput(parent.getName())).getRecords()).isEmpty();
      List<QRecord> stored = AssociatedRecordDiscovery.readParentValues(parent, List.of(association), List.of("2"), null);
      assertThat(stored).singleElement().satisfies(record -> assertThat(record.getValues())
         .containsOnlyKeys("id", "storedCode").containsEntry("id", 2).containsEntry("storedCode", "BETA"));
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(TestUtils.defineBackend())))
      {
         try(Statement statement = transaction.getConnection().createStatement())
         {
            statement.execute("UPDATE discovery_parent SET code = 'PENDING' WHERE id = 2");
            assertThat(AssociatedRecordDiscovery.readParentValues(parent, List.of(association), List.of(2), transaction))
               .singleElement().satisfies(record -> assertEquals("PENDING", record.getValueString("storedCode")));
            statement.execute("UPDATE discovery_parent SET code = NULL WHERE id = 2");
            assertThat(AssociatedRecordDiscovery.readParentValues(parent, List.of(association), List.of(2), transaction))
               .singleElement().satisfies(record -> assertThat(record.getValues()).containsEntry("storedCode", null));
         }
         assertFalse(transaction.getConnection().isClosed());
         transaction.rollback();
      }
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
          Statement statement = connection.createStatement();
          var result = statement.executeQuery("SELECT code FROM discovery_parent WHERE id = 2"))
      {
         assertTrue(result.next());
         assertEquals("BETA", result.getString(1));
      }
      assertThat(new QueryAction().execute(new QueryInput(parent.getName())).getRecords()).isEmpty();
      assertThat(QContext.getQSession()).isSameAs(session);
      assertThat(parent.getRecordSecurityLocks()).isSameAs(locks);
   }
}
