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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Count must honor physical columns, the active security model and JDBC ownership.
 *******************************************************************************/
class RDBMSCountContractTest extends RDBMSActionTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      primeTestDatabase();
   }



   /*******************************************************************************
    ** Remove only this test's fixtures, including the view depending on Person.
    *******************************************************************************/
   @AfterEach
   void removeCountFixtures() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
          Statement statement = connection.createStatement())
      {
         statement.execute("DROP VIEW IF EXISTS count_mapped_person");
         statement.execute("DROP TABLE IF EXISTS count_child");
         statement.execute("DROP TABLE IF EXISTS count_parent");
      }
   }



   /*******************************************************************************
    ** A logical primary-key name need not match the physical database column.
    *******************************************************************************/
   @Test
   void testDistinctCountUsesMappedPrimaryKey() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
          Statement statement = connection.createStatement())
      {
         statement.execute("CREATE OR REPLACE VIEW count_mapped_person AS SELECT id AS person_key FROM person");
      }
      QInstance instance = QContext.getQInstance();
      instance.addTable(new QTableMetaData().withName("countMappedPerson").withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("count_mapped_person"))
         .withPrimaryKeyField("personKey").withField(new QFieldMetaData("personKey", QFieldType.INTEGER).withBackendName("person_key")));
      assertEquals(5, CountAction.execute("countMappedPerson", null));
      CountOutput output = new CountAction().execute(new CountInput("countMappedPerson").withIncludeDistinctCount(true));
      assertEquals(5, output.getCount());
      assertEquals(5, output.getDistinctCount());
   }



   /*******************************************************************************
    ** Instances with the same table names may have different security join fan-out.
    *******************************************************************************/
   @Test
   void testDistinctSecurityDecisionBelongsToCurrentInstance() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
          Statement statement = connection.createStatement())
      {
         statement.execute("DROP TABLE IF EXISTS count_child");
         statement.execute("DROP TABLE IF EXISTS count_parent");
         statement.execute("CREATE TABLE count_parent (id INT PRIMARY KEY)");
         statement.execute("CREATE TABLE count_child (id INT PRIMARY KEY, parent_id INT, tenant_id INT)");
         statement.execute("INSERT INTO count_parent VALUES (1), (2)");
         statement.execute("INSERT INTO count_child VALUES (10, 1, 7), (11, 1, 7), (12, 2, 8)");
      }
      QInstance unsecured = defineSecurityInstance(false);
      QInstance secured = defineSecurityInstance(true);
      QContext.init(unsecured, new QSession());
      assertEquals(2, CountAction.execute("countParent", null));

      QContext.init(secured, new QSession().withSecurityKeyValue("countTenant", 7));
      assertEquals(1, CountAction.execute("countParent", null));
      assertEquals(List.of(1), QueryAction.execute("countParent", null).stream().map(record -> record.getValueInteger("id")).toList());

      QContext.init(unsecured, new QSession());
      CountOutput joined = new CountAction().execute(new CountInput("countParent")
         .withQueryJoin(new QueryJoin("countParent", "countChild")));
      assertEquals(3, joined.getCount(), "An ordinary explicit join counts matching rows, not distinct parents");
   }



   /*******************************************************************************
    ** Count closes its statement while leaving pending writes and connection owned
    ** by the caller's transaction untouched.
    *******************************************************************************/
   @Test
   void testCountClosesStatementWithinTransaction() throws Exception
   {
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(TestUtils.defineBackend())))
      {
         try(PreparedStatement insert = transaction.getConnection().prepareStatement("INSERT INTO person (first_name, last_name, email) VALUES (?, ?, ?)"))
         {
            insert.setString(1, "Count");
            insert.setString(2, "Transaction");
            insert.setString(3, "count-transaction@example.invalid");
            assertEquals(1, insert.executeUpdate());
         }
         RDBMSCountAction action = new RDBMSCountAction();
         assertEquals(6, action.execute(new CountInput(TestUtils.TABLE_NAME_PERSON).withTransaction(transaction)).getCount());
         assertTrue(action.statement.isClosed(), "Count must release its statement before the caller closes the transaction");
         assertFalse(transaction.getConnection().isClosed());
         assertEquals(5, CountAction.execute(TestUtils.TABLE_NAME_PERSON, null), "Count must not commit the pending insert");
         transaction.rollback();
         assertEquals(5, action.execute(new CountInput(TestUtils.TABLE_NAME_PERSON).withTransaction(transaction)).getCount());
         assertTrue(action.statement.isClosed());
      }
   }



   /*******************************************************************************
    ** An execution error also releases the statement without closing the transaction.
    *******************************************************************************/
   @Test
   void testCountClosesStatementAfterExecutionFailure() throws Exception
   {
      ///////////////////////////////////////////////////////////////////////
      // Model a schema mismatch so JDBC, rather than filter parsing, fails. //
      ///////////////////////////////////////////////////////////////////////
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).getField("id").setType(QFieldType.STRING);
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(TestUtils.defineBackend())))
      {
         RDBMSCountAction action = new RDBMSCountAction();
         CountInput input = new CountInput(TestUtils.TABLE_NAME_PERSON).withTransaction(transaction)
            .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, "invalid-number")));
         QException failure = assertThrows(QException.class, () -> action.execute(input));
         assertTrue(failure.getCause() instanceof SQLException);
         assertTrue(action.statement.isClosed(), "A failed Count must also release its statement");
         assertFalse(transaction.getConnection().isClosed());
         assertEquals(5, action.execute(new CountInput(TestUtils.TABLE_NAME_PERSON).withTransaction(transaction)).getCount());
         assertTrue(action.statement.isClosed());
         transaction.rollback();
      }
   }



   /*******************************************************************************
    ** Only the security policy differs between independently constructed instances.
    *******************************************************************************/
   private QInstance defineSecurityInstance(boolean secured)
   {
      QInstance instance = TestUtils.defineInstance();
      QTableMetaData parent = new QTableMetaData().withName("countParent").withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("count_parent"))
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER));
      instance.addTable(parent);
      instance.addTable(new QTableMetaData().withName("countChild").withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("count_child"))
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("parentId", QFieldType.INTEGER).withBackendName("parent_id"))
         .withField(new QFieldMetaData("tenantId", QFieldType.INTEGER).withBackendName("tenant_id")));
      instance.addJoin(new QJoinMetaData().withLeftTable("countParent").withRightTable("countChild").withInferredName()
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "parentId")));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("countTenant"));
      if(secured)
      {
         parent.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("countTenant").withFieldName("countChild.tenantId")
            .withJoinNameChain(List.of(QJoinMetaData.makeInferredJoinName("countParent", "countChild"))));
      }
      return instance;
   }
}
