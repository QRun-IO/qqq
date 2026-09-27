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


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.SystemErrorStatusMessage;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Omission and cascade ordering preserves ancestor-dependent record security.
 ** Native SQL observes mutations independently of QQQ result visibility.
 *******************************************************************************/
class AssociationMutationOrderTest extends RDBMSActionTest
{
   private static final String PARENT = "mutationParent";
   private static final String CHILD = "mutationChild";
   private static final String LEAF = "mutationLeaf";
   private static final String GROUP = "owned children";

   private QTableMetaData parent;
   private QTableMetaData child;
   private QTableMetaData leaf;

   /*******************************************************************************
    ** The two protected levels isolate immediate and recursive cascade timing.
    *******************************************************************************/
   private enum LockAt
   {
      NONE,
      CHILD,
      LEAF
   }



   /*******************************************************************************
    ** No physical cascading constraints or triggers can hide missing framework work.
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      primeTestDatabase();
      for(String name : List.of("mutation_parent", "mutation_child", "mutation_leaf"))
      {
         executeSql("CREATE TABLE " + name + " (id INTEGER PRIMARY KEY, parent_id INTEGER, child_id INTEGER, state VARCHAR(30), payload VARCHAR(30))");
      }
      executeSql("INSERT INTO mutation_parent(id,state,payload) VALUES(1,'ALLOWED','original'),(2,'ALLOWED','other')");
      executeSql("INSERT INTO mutation_child(id,parent_id,state,payload) VALUES(10,1,'ALLOWED','first'),(20,1,'ALLOWED','second'),(30,2,'ALLOWED','unrelated')");
      executeSql("INSERT INTO mutation_leaf(id,child_id,state,payload) VALUES(100,10,'ALLOWED','first leaf'),(200,20,'ALLOWED','second leaf'),(300,30,'ALLOWED','unrelated leaf')");
      parent = table(PARENT, "mutation_parent");
      child = table(CHILD, "mutation_child");
      leaf = table(LEAF, "mutation_leaf");
      parent.withAssociation(new Association().withName(GROUP).withAssociatedTableName(CHILD).withJoinName("mutationParentChild"));
      child.withAssociation(new Association().withName("owned leaves").withAssociatedTableName(LEAF).withJoinName("mutationChildLeaf"));
      QContext.getQInstance().addTable(parent);
      QContext.getQInstance().addTable(child);
      QContext.getQInstance().addTable(leaf);
      QContext.getQInstance().addJoin(new QJoinMetaData().withName("mutationParentChild").withLeftTable(PARENT).withRightTable(CHILD)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "parentId")));
      QContext.getQInstance().addJoin(new QJoinMetaData().withName("mutationChildLeaf").withLeftTable(CHILD).withRightTable(LEAF)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "childId")));
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("mutationAllowedState"));
      QContext.getQSession().withSecurityKeyValue("mutationAllowedState", "ALLOWED");
   }



   /*******************************************************************************
    ** Drop only this class's owned tables, including its optional RESTRICT fixture.
    *******************************************************************************/
   @AfterEach
   void removeFixture() throws Exception
   {
      executeSql("DROP TABLE IF EXISTS mutation_blocker");
      executeSql("DROP TABLE IF EXISTS mutation_leaf");
      executeSql("DROP TABLE IF EXISTS mutation_child");
      executeSql("DROP TABLE IF EXISTS mutation_parent");
   }



   /*******************************************************************************
    ** An old member must be deleted while its parent lock still permits the write;
    ** an unchanged security value is a positive control of the same fixture.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testOmissionsDeletedBeforeParentSecurityValueChanges(boolean hideChildren) throws Exception
   {
      configureLocks(LockAt.CHILD);
      Map<String, Object> before = snapshot();
      assertEquals(List.of(10, 20, 30), visibleIds(CHILD));
      QRecord output = new UpdateAction().execute(update(hideChildren ? "DENIED" : "ALLOWED", null)).getRecords().get(0);
      assertThat(output.getErrors()).isEmpty();
      assertEquals(hideChildren ? "DENIED" : "ALLOWED", rows("SELECT state FROM mutation_parent WHERE id=1").get(0).get("STATE"));
      assertEquals("updated", rows("SELECT payload FROM mutation_parent WHERE id=1").get(0).get("PAYLOAD"));
      assertTargetDescendantsGone();
      assertUnrelatedUnchanged(before);
   }



   /*******************************************************************************
    ** Descendants are removed while their locked ancestor is still present.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(LockAt.class)
   void testDescendantsDeletedBeforeTheirSecurityAncestor(LockAt lockAt) throws Exception
   {
      configureLocks(lockAt);
      Map<String, Object> before = snapshot();
      assertEquals(List.of(10, 20, 30), visibleIds(CHILD));
      assertEquals(List.of(100, 200, 300), visibleIds(LEAF));
      DeleteOutput output = new DeleteAction().execute(delete(null));
      assertEquals(1, output.getDeletedRecordCount());
      assertThat(output.getRecordsWithErrors()).isNullOrEmpty();
      assertThat(rows("SELECT * FROM mutation_parent WHERE id=1")).isEmpty();
      assertTargetDescendantsGone();
      assertUnrelatedUnchanged(before);
   }



   /*******************************************************************************
    ** The same successful ordering stays inside the caller's transaction. Merely
    ** using a transaction does not justify stale descendants within that transaction.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testSuccessfulCascadeAndOmissionCanBeRolledBack(boolean updating) throws Exception
   {
      configureLocks(LockAt.CHILD);
      Map<String, Object> before = snapshot();
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(TestUtils.defineBackend())))
      {
         if(updating)
         {
            QRecord output = new UpdateAction().execute(update("DENIED", transaction)).getRecords().get(0);
            assertThat(output.getErrors()).isEmpty();
            assertEquals("DENIED", rows(transaction.getConnection(), "SELECT state FROM mutation_parent WHERE id=1").get(0).get("STATE"));
         }
         else
         {
            DeleteOutput output = new DeleteAction().execute(delete(transaction));
            assertEquals(1, output.getDeletedRecordCount());
            assertThat(output.getRecordsWithErrors()).isNullOrEmpty();
            assertThat(rows(transaction.getConnection(), "SELECT * FROM mutation_parent WHERE id=1")).isEmpty();
         }
         assertThat(rows(transaction.getConnection(), "SELECT id FROM mutation_child WHERE parent_id=1")).isEmpty();
         assertThat(rows(transaction.getConnection(), "SELECT id FROM mutation_leaf WHERE child_id IN(10,20)")).isEmpty();
         assertUnrelatedUnchanged(before, transaction.getConnection());
         assertFalse(transaction.getConnection().isClosed());
         assertEquals(before, snapshot(), "Another connection must see no uncommitted changes");
         transaction.rollback();
      }
      assertEquals(before, snapshot(), "Caller rollback must restore parents, children and leaves");
   }



   /*******************************************************************************
    ** Native membership cannot turn an unreadable or write-denied child into a
    ** successful parent deletion; returned errors identify only the parent target.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = RecordSecurityLock.LockScope.class, names = { "READ_AND_WRITE", "WRITE" })
   void testUnavailableOrDeniedChildPreservesParent(RecordSecurityLock.LockScope scope) throws Exception
   {
      child.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("mutationAllowedState").withFieldName("state").withLockScope(scope));
      configureLocks(LockAt.NONE);
      executeSql("UPDATE mutation_child SET state='DENIED',payload='private child value' WHERE parent_id=1");
      Map<String, Object> before = snapshot();
      assertEquals(scope == RecordSecurityLock.LockScope.WRITE ? List.of(10, 20, 30) : List.of(30), visibleIds(CHILD));
      DeleteOutput output = new DeleteAction().execute(delete(null));
      assertEquals(0, output.getDeletedRecordCount());
      assertThat(output.getRecordsWithErrors()).singleElement().satisfies(record ->
      {
         assertEquals(1, record.getValueInteger("id"));
         assertThat(record.getErrors()).isNotEmpty();
         assertThat(record.getErrors().toString()).doesNotContain("private child value", "10", "20");
      });
      assertEquals(before, snapshot());
      QRecord updateOutput = new UpdateAction().execute(update("DENIED", null)).getRecords().get(0);
      assertEquals(1, updateOutput.getValueInteger("id"));
      assertThat(updateOutput.getErrors()).isNotEmpty();
      assertThat(updateOutput.getErrors().toString()).doesNotContain("private child value", "10", "20");
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Native filter deletion must honor pre-delete rejection after resolving keys,
    ** preserving both the rejected branch and the caller's original filter input.
    *******************************************************************************/
   @Test
   void testFilterDeleteHonorsParentCustomizerAndRestoresInput() throws Exception
   {
      parent.withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(RejectFirstParentDelete.class));
      configureLocks(LockAt.NONE);
      List<Map<String, Object>> rejectedParent = rows("SELECT * FROM mutation_parent WHERE id=1");
      List<Map<String, Object>> rejectedChildren = rows("SELECT * FROM mutation_child WHERE parent_id=1 ORDER BY id");
      List<Map<String, Object>> rejectedLeaves = rows("SELECT * FROM mutation_leaf WHERE child_id IN(10,20) ORDER BY id");
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, List.of(1, 2)));
      String beforeFilter = JsonUtils.toJson(filter);
      DeleteInput input = new DeleteInput(PARENT).withInputSource(QInputSource.USER).withQueryFilter(filter);
      assertNull(input.getPrimaryKeys());
      DeleteOutput output = new DeleteAction().execute(input);
      assertEquals(1, output.getDeletedRecordCount());
      assertThat(output.getRecordsWithErrors()).singleElement().satisfies(record ->
      {
         assertEquals(1, record.getValueInteger("id"));
         assertThat(record.getErrors()).anyMatch(error -> error instanceof BadInputStatusMessage);
      });
      assertEquals(rejectedParent, rows("SELECT * FROM mutation_parent ORDER BY id"));
      assertEquals(rejectedChildren, rows("SELECT * FROM mutation_child ORDER BY id"));
      assertEquals(rejectedLeaves, rows("SELECT * FROM mutation_leaf ORDER BY id"));
      assertSame(filter, input.getQueryFilter());
      assertEquals(beforeFilter, JsonUtils.toJson(input.getQueryFilter()));
      assertNull(input.getPrimaryKeys());
   }



   /*******************************************************************************
    ** A returned native child failure prevents ancestor deletion. Successful
    ** earlier descendants are not presumed rolled back without a transaction.
    *******************************************************************************/
   @Test
   void testNativeChildFailurePreventsParentDelete() throws Exception
   {
      configureLocks(LockAt.NONE);
      blockChildDeletion();
      List<Map<String, Object>> parentsBefore = rows("SELECT * FROM mutation_parent ORDER BY id");
      List<Map<String, Object>> blockedChild = rows("SELECT * FROM mutation_child WHERE id=10");
      List<Map<String, Object>> unrelatedChild = rows("SELECT * FROM mutation_child WHERE id=30");
      List<Map<String, Object>> unrelatedLeaf = rows("SELECT * FROM mutation_leaf WHERE id=300");
      DeleteOutput output = new DeleteAction().execute(delete(null));
      assertEquals(0, output.getDeletedRecordCount());
      assertThat(output.getRecordsWithErrors()).singleElement().satisfies(this::assertParentSystemError);
      assertEquals(parentsBefore, rows("SELECT * FROM mutation_parent ORDER BY id"));
      assertEquals(blockedChild, rows("SELECT * FROM mutation_child WHERE id=10"));
      assertEquals(unrelatedChild, rows("SELECT * FROM mutation_child WHERE id=30"));
      assertEquals(unrelatedLeaf, rows("SELECT * FROM mutation_leaf WHERE id=300"));
   }



   /*******************************************************************************
    ** Omission deletion must finish before a parent UPDATE reaches native storage.
    *******************************************************************************/
   @Test
   void testNativeOmissionFailurePreventsParentUpdate() throws Exception
   {
      configureLocks(LockAt.NONE);
      blockChildDeletion();
      List<Map<String, Object>> parentsBefore = rows("SELECT * FROM mutation_parent ORDER BY id");
      List<Map<String, Object>> blockedChild = rows("SELECT * FROM mutation_child WHERE id=10");
      List<Map<String, Object>> unrelatedChild = rows("SELECT * FROM mutation_child WHERE id=30");
      List<Map<String, Object>> unrelatedLeaf = rows("SELECT * FROM mutation_leaf WHERE id=300");
      QRecord output = new UpdateAction().execute(update("DENIED", null)).getRecords().get(0);
      assertParentSystemError(output);
      assertEquals(parentsBefore, rows("SELECT * FROM mutation_parent ORDER BY id"));
      assertEquals(blockedChild, rows("SELECT * FROM mutation_child WHERE id=10"));
      assertEquals(unrelatedChild, rows("SELECT * FROM mutation_child WHERE id=30"));
      assertEquals(unrelatedLeaf, rows("SELECT * FROM mutation_leaf WHERE id=300"));
   }



   /*******************************************************************************
    ** Explicit rollback restores earlier successful descendant deletions after a
    ** later native child failure; no automatic atomicity is assumed.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testCallerRollbackRestoresDescendantsAfterNativeFailure(boolean updating) throws Exception
   {
      configureLocks(LockAt.NONE);
      blockChildDeletion();
      Map<String, Object> before = snapshot();
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(TestUtils.defineBackend())))
      {
         if(updating)
         {
            QRecord output = new UpdateAction().execute(update("DENIED", transaction)).getRecords().get(0);
            assertParentSystemError(output);
         }
         else
         {
            DeleteOutput output = new DeleteAction().execute(delete(transaction));
            assertEquals(0, output.getDeletedRecordCount());
            assertThat(output.getRecordsWithErrors()).singleElement().satisfies(this::assertParentSystemError);
         }
         assertEquals(before.get("parents"), rows(transaction.getConnection(), "SELECT * FROM mutation_parent ORDER BY id"));
         assertThat(rows(transaction.getConnection(), "SELECT id FROM mutation_leaf WHERE id=100")).isEmpty();
         assertFalse(transaction.getConnection().isClosed());
         assertEquals(before, snapshot());
         transaction.rollback();
      }
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void configureLocks(LockAt lockAt) throws Exception
   {
      if(lockAt == LockAt.CHILD)
      {
         child.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("mutationAllowedState")
            .withFieldName(PARENT + ".state").withJoinNameChain(List.of("mutationParentChild")));
      }
      else if(lockAt == LockAt.LEAF)
      {
         leaf.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("mutationAllowedState")
            .withFieldName(CHILD + ".state").withJoinNameChain(List.of("mutationChildLeaf")));
      }
      new QInstanceValidator().revalidate(QContext.getQInstance());
   }



   /*******************************************************************************
    ** A native RESTRICT constraint is independent from the framework graph.
    *******************************************************************************/
   private void blockChildDeletion() throws Exception
   {
      executeSql("CREATE TABLE mutation_blocker(id INTEGER PRIMARY KEY, child_id INTEGER REFERENCES mutation_child(id))");
      executeSql("INSERT INTO mutation_blocker VALUES(1,10)");
      SQLException failure = assertThrows(SQLException.class, () -> executeSql("DELETE FROM mutation_child WHERE id=10"));
      assertEquals("23503", failure.getSQLState(), "The owned native constraint must reject this specific child deletion");
   }



   /*******************************************************************************
    ** Preserve the adapter failure category without echoing private child details.
    *******************************************************************************/
   private void assertParentSystemError(QRecord record)
   {
      assertEquals(1, record.getValueInteger("id"));
      assertThat(record.getErrors()).singleElement().isInstanceOf(SystemErrorStatusMessage.class);
      assertThat(record.getErrors().toString()).doesNotContain("mutation_blocker", "mutation_child", "10", "20");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private UpdateInput update(String state, RDBMSTransaction transaction)
   {
      return new UpdateInput(PARENT).withInputSource(QInputSource.USER).withTransaction(transaction)
         .withRecord(new QRecord().withValue("id", 1).withValue("state", state).withValue("payload", "updated").withAssociatedRecords(GROUP, List.of()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private DeleteInput delete(RDBMSTransaction transaction)
   {
      return new DeleteInput(PARENT).withInputSource(QInputSource.USER).withTransaction(transaction).withPrimaryKeys(List.of(1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData table(String name, String physical)
   {
      return new QTableMetaData().withName(name).withBackendName(TestUtils.DEFAULT_BACKEND_NAME).withPrimaryKeyField("id")
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName(physical))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("parentId", QFieldType.INTEGER).withBackendName("parent_id"))
         .withField(new QFieldMetaData("childId", QFieldType.INTEGER).withBackendName("child_id"))
         .withField(new QFieldMetaData("state", QFieldType.STRING))
         .withField(new QFieldMetaData("payload", QFieldType.STRING));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Integer> visibleIds(String tableName) throws Exception
   {
      return new QueryAction().execute(new QueryInput(tableName).withInputSource(QInputSource.SYSTEM)).getRecords().stream()
         .map(record -> record.getValueInteger("id")).sorted().toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertTargetDescendantsGone() throws Exception
   {
      assertThat(rows("SELECT id FROM mutation_child WHERE parent_id=1")).isEmpty();
      assertThat(rows("SELECT id FROM mutation_leaf WHERE child_id IN(10,20)")).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertUnrelatedUnchanged(Map<String, Object> before) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         assertUnrelatedUnchanged(before, connection);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertUnrelatedUnchanged(Map<String, Object> before, Connection connection) throws Exception
   {
      assertEquals(((List<?>) before.get("parents")).get(1), rows(connection, "SELECT * FROM mutation_parent WHERE id=2").get(0));
      assertEquals(((List<?>) before.get("children")).get(2), rows(connection, "SELECT * FROM mutation_child WHERE id=30").get(0));
      assertEquals(((List<?>) before.get("leaves")).get(2), rows(connection, "SELECT * FROM mutation_leaf WHERE id=300").get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, Object> snapshot() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         Map<String, Object> result = new LinkedHashMap<>();
         result.put("parents", rows(connection, "SELECT * FROM mutation_parent ORDER BY id"));
         result.put("children", rows(connection, "SELECT * FROM mutation_child ORDER BY id"));
         result.put("leaves", rows(connection, "SELECT * FROM mutation_leaf ORDER BY id"));
         return result;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void executeSql(String query) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()); Statement statement = connection.createStatement())
      {
         statement.execute(query);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Map<String, Object>> rows(String query) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         return rows(connection, query);
      }
   }



   /*******************************************************************************
    ** Preserve nulls and leave caller transaction ownership unchanged.
    *******************************************************************************/
   private List<Map<String, Object>> rows(Connection connection, String query) throws Exception
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      try(Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query))
      {
         while(result.next())
         {
            Map<String, Object> row = new LinkedHashMap<>();
            for(int i = 1; i <= result.getMetaData().getColumnCount(); i++)
            {
               row.put(result.getMetaData().getColumnLabel(i).toUpperCase(Locale.ROOT), result.getObject(i));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Ordinary application validation rejects one parent selected by the filter.
    *******************************************************************************/
   public static class RejectFirstParentDelete implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         for(QRecord record : records)
         {
            if(Integer.valueOf(1).equals(record.getValueInteger("id")))
            {
               record.addError(new BadInputStatusMessage("This parent must remain"));
            }
         }
         return records;
      }
   }
}
