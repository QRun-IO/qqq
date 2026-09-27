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

package com.kingsrook.sampleapp;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter.BooleanOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Count contracts use the real sample and explicit owned configuration variants.
 *******************************************************************************/
public class SampleCountContractTest
{
   private static final String PERSON = SampleMetaDataProvider.TABLE_NAME_PERSON;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QContext.init(SampleMetaDataProvider.defineTestInstance(), new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** Count is the total selected set, independent of query paging and sorting.
    *******************************************************************************/
   @Test
   void testTotalAndTimeoutConfiguration() throws Exception
   {
      CountOutput defaults = new CountAction().execute(new CountInput(PERSON));
      assertEquals(5, defaults.getCount());
      assertNull(defaults.getDistinctCount());
      assertEquals(5, CountAction.execute(PERSON, new QQueryFilter()));
      QQueryFilter page = new QQueryFilter(new QFilterCriteria("isEmployed", QCriteriaOperator.EQUALS, true))
         .withOrderBy(new QFilterOrderBy("firstName", false)).withSkip(50).withLimit(1);
      assertEquals(4, CountAction.execute(PERSON, page));
      assertEquals(50, page.getSkip());
      assertEquals(1, page.getLimit());
      for(Integer timeout : new Integer[] { null, 0, -1, 5 })
      {
         assertEquals(5, new CountAction().execute(new CountInput(PERSON).withTimeoutSeconds(timeout)).getCount());
      }
   }



   /*******************************************************************************
    ** Fan-out counts joined rows and can separately count distinct parent keys.
    *******************************************************************************/
   @Test
   void testJoinsAndDistinctCounts() throws Exception
   {
      CountOutput inner = new CountAction().execute(new CountInput(PERSON).withQueryJoin(new QueryJoin("pet")).withIncludeDistinctCount(true));
      assertEquals(6, inner.getCount());
      assertEquals(3, inner.getDistinctCount());
      CountOutput left = new CountAction().execute(new CountInput(PERSON).withQueryJoin(new QueryJoin("pet").withType(QueryJoin.Type.LEFT)).withIncludeDistinctCount(true));
      assertEquals(8, left.getCount());
      assertEquals(5, left.getDistinctCount());
      CountInput filtered = new CountInput(PERSON).withQueryJoin(new QueryJoin("pet").withAlias("animal"))
         .withFilter(new QQueryFilter(new QFilterCriteria("animal.speciesId", QCriteriaOperator.EQUALS, 1))).withIncludeDistinctCount(true);
      CountOutput dogs = new CountAction().execute(filtered);
      assertEquals(5, dogs.getCount());
      assertEquals(2, dogs.getDistinctCount());
      filtered.setFilter(new QQueryFilter(new QFilterCriteria("animal.name", QCriteriaOperator.EQUALS, "absent")));
      CountOutput empty = new CountAction().execute(filtered);
      assertEquals(0, empty.getCount());
      assertEquals(0, empty.getDistinctCount());
   }



   /*******************************************************************************
    ** Logical primary-key names must resolve to the actual database column.
    *******************************************************************************/
   @Test
   void testDistinctWithMappedPrimaryKey() throws Exception
   {
      QContext.getQInstance().addTable(new QTableMetaData().withName("mappedPerson")
         .withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("person"))
         .withPrimaryKeyField("recordId").withField(new QFieldMetaData("recordId", QFieldType.INTEGER).withBackendName("id")));
      assertEquals(5, CountAction.execute("mappedPerson", null));
      CountOutput output = new CountAction().execute(new CountInput("mappedPerson").withIncludeDistinctCount(true));
      assertEquals(5, output.getCount());
      assertEquals(5, output.getDistinctCount());
   }



   /*******************************************************************************
    ** A caller owns its transaction; Count sees its writes without publishing them.
    *******************************************************************************/
   @Test
   void testTransactionVisibilityRollbackAndCommit() throws Exception
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) QContext.getQInstance().getBackend(SampleMetaDataProvider.RDBMS_BACKEND_NAME);
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(backend)))
      {
         insertTransactionPerson(transaction.getConnection());
         assertEquals(6, new CountAction().execute(new CountInput(PERSON).withTransaction(transaction)).getCount());
         assertEquals(5, CountAction.execute(PERSON, null));
         assertFalse(transaction.getConnection().isClosed());
         assertFalse(transaction.getConnection().getAutoCommit());
         transaction.rollback();
         assertEquals(5, new CountAction().execute(new CountInput(PERSON).withTransaction(transaction)).getCount());
         insertTransactionPerson(transaction.getConnection());
         assertEquals(5, CountAction.execute(PERSON, null));
         transaction.commit();
         assertEquals(6, CountAction.execute(PERSON, null));
         assertFalse(transaction.getConnection().isClosed());
      }
   }



   /*******************************************************************************
    ** The replica hint is optional; an explicit transaction always wins.
    *******************************************************************************/
   @Test
   void testReadOnlyRoutingAndTransactionPriority() throws Exception
   {
      RDBMSBackendMetaData primary = (RDBMSBackendMetaData) QContext.getQInstance().getBackend(SampleMetaDataProvider.RDBMS_BACKEND_NAME);
      CountInput hinted = new CountInput(PERSON).withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
      assertEquals(5, new CountAction().execute(hinted).getCount());
      RDBMSBackendMetaData replica = SampleMetaDataProvider.defineRdbmsBackend().withName("countReplica").withDatabaseName("sample_count_replica");
      try(Connection connection = ConnectionManager.getConnection(replica);
          Statement statement = connection.createStatement())
      {
         statement.execute("DROP TABLE IF EXISTS person");
         statement.execute("CREATE TABLE person (id INTEGER PRIMARY KEY)");
         statement.execute("INSERT INTO person VALUES (1), (2)");
         primary.setReadOnlyBackendMetaData(replica);
         assertEquals(5, CountAction.execute(PERSON, null));
         assertEquals(2, new CountAction().execute(hinted).getCount());
         try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(primary)))
         {
            insertTransactionPerson(transaction.getConnection());
            assertEquals(6, new CountAction().execute(new CountInput(PERSON).withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND).withTransaction(transaction)).getCount());
            assertEquals(2, new CountAction().execute(new CountInput(PERSON).withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND)).getCount());
            transaction.rollback();
         }
      }
   }



   /*******************************************************************************
    ** Missing/invalid execution context and invalid fields fail before a count.
    *******************************************************************************/
   @Test
   void testInvalidContextAndFilters() throws Exception
   {
      assertThrows(QException.class, () -> new CountAction().execute(new CountInput()));
      assertThrows(QException.class, () -> CountAction.execute("missingTable", null));
      for(QQueryFilter filter : List.of(
         new QQueryFilter().withSubFilter(new QQueryFilter(new QFilterCriteria("missingField", QCriteriaOperator.EQUALS, "value"))),
         new QQueryFilter().withOrderBy(new QFilterOrderBy("missingField")),
         new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, "not-an-integer"))))
      {
         assertThrows(QException.class, () -> CountAction.execute(PERSON, filter));
      }
      assertThrows(QException.class, () -> new CountAction().execute(new CountInput(PERSON).withQueryJoin(new QueryJoin("pet"))
         .withFilter(new QQueryFilter(new QFilterCriteria("pet.missingField", QCriteriaOperator.EQUALS, "value")))));
      QInstance instance = QContext.getQInstance();
      QContext.setQSession(new QSession().withValue("isInvalid", "true"));
      assertThrows(QException.class, () -> CountAction.execute(PERSON, null));
      QContext.setQSession(null);
      assertThrows(QException.class, () -> CountAction.execute(PERSON, null));
      CountInput missingContext = new CountInput(PERSON);
      QContext.clear();
      assertThrows(QException.class, () -> new CountAction().execute(missingContext));
      QContext.init(instance, new QSession());
      assertEquals(5, CountAction.execute(PERSON, null));
   }



   /*******************************************************************************
    ** Count must not filter a field removed from the user's personalized metadata.
    *******************************************************************************/
   @Test
   void testPersonalizedFilterBoundary() throws Exception
   {
      QContext.getQInstance().addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(HideSalary.class));
      QQueryFilter salary = new QQueryFilter(new QFilterCriteria("annualSalary", QCriteriaOperator.GREATER_THAN, 100000));
      assertThrows(QException.class, () -> new CountAction().execute(new CountInput(PERSON).withInputSource(QInputSource.USER).withFilter(salary)));
      assertEquals(4, CountAction.execute(PERSON, salary));
      assertTrue(QContext.getQInstance().getTable(PERSON).getFields().containsKey("annualSalary"));
   }



   /*******************************************************************************
    ** A failed joined-table personalizer must not restore access to private filters.
    *******************************************************************************/
   @Test
   void testPersonalizerFailureRejectsJoinedFilters() throws Exception
   {
      QContext.getQInstance().addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(FailingPetPersonalizer.class));
      QQueryFilter petName = new QQueryFilter(new QFilterCriteria("pet.name", QCriteriaOperator.EQUALS, "Charlie"));
      assertEquals(1, new CountAction().execute(new CountInput(PERSON).withInputSource(QInputSource.SYSTEM)
         .withQueryJoin(new QueryJoin("pet")).withFilter(petName)).getCount());
      assertEquals(1, new QueryAction().execute(new QueryInput(PERSON).withInputSource(QInputSource.SYSTEM)
         .withQueryJoin(new QueryJoin("pet").withSelect(false)).withFilter(petName)).getRecords().size());
      assertAll(
         () -> assertThrows(QException.class, () -> new CountAction().execute(new CountInput(PERSON).withInputSource(QInputSource.USER)
            .withQueryJoin(new QueryJoin("pet")).withFilter(petName))),
         () -> assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput(PERSON).withInputSource(QInputSource.USER)
            .withQueryJoin(new QueryJoin("pet").withSelect(false)).withFilter(petName))));
   }



   /*******************************************************************************
    ** A joined security lock counts authorized parents once, across instance reuse.
    *******************************************************************************/
   @Test
   void testJoinedSecurityAcrossInstances() throws Exception
   {
      QInstance unsecured = QContext.getQInstance();
      assertEquals(5, CountAction.execute(PERSON, null));
      QInstance secured = SampleMetaDataProvider.defineTestInstance();
      secured.addSecurityKeyType(new QSecurityKeyType().withName("species"));
      secured.getTable(PERSON).withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("species")
         .withFieldName("pet.speciesId").withJoinNameChain(List.of("personJoinPet")));
      QContext.init(secured, new QSession().withSecurityKeyValue("species", 1));
      assertEquals(2, CountAction.execute(PERSON, null));
      assertEquals(List.of(1, 2), QueryAction.execute(PERSON, new QQueryFilter().withOrderBy(new QFilterOrderBy("id")))
         .stream().map(record -> record.getValueInteger("id")).toList());
      CountOutput distinct = new CountAction().execute(new CountInput(PERSON).withIncludeDistinctCount(true));
      assertEquals(5, distinct.getCount());
      assertEquals(2, distinct.getDistinctCount());
      QContext.init(unsecured, new QSession());
      assertEquals(5, CountAction.execute(PERSON, null));
      QContext.init(secured, new QSession());
      assertEquals(0, CountAction.execute(PERSON, null));
   }



   /*******************************************************************************
    ** User OR filters cannot broaden security; null policies and multiple keys
    ** apply to the selected population before counting.
    *******************************************************************************/
   @Test
   void testSecurityKeysAndNullPolicies() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement())
      {
         assertEquals(1, statement.executeUpdate("UPDATE person SET days_worked = NULL WHERE id = 5"));
      }
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("workGroup"));
      RecordSecurityLock lock = new RecordSecurityLock().withSecurityKeyType("workGroup").withFieldName("daysWorked");
      QContext.getQInstance().getTable(PERSON).withRecordSecurityLock(lock);
      QQueryFilter userOr = new QQueryFilter().withBooleanOperator(BooleanOperator.OR)
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 3));
      for(RecordSecurityLock.NullValueBehavior behavior : RecordSecurityLock.NullValueBehavior.values())
      {
         lock.setNullValueBehavior(behavior);
         QContext.setQSession(new QSession().withSecurityKeyValue("workGroup", 1001).withSecurityKeyValue("workGroup", 10100));
         assertEquals(behavior == RecordSecurityLock.NullValueBehavior.ALLOW ? 3 : 2, CountAction.execute(PERSON, null));
         assertEquals(1, CountAction.execute(PERSON, userOr));
         QContext.setQSession(new QSession());
         assertEquals(behavior == RecordSecurityLock.NullValueBehavior.ALLOW ? 1 : 0, CountAction.execute(PERSON, null));
      }
      lock.setLockScope(RecordSecurityLock.LockScope.WRITE);
      assertEquals(5, CountAction.execute(PERSON, null));
      lock.setLockScope(RecordSecurityLock.LockScope.READ);
      assertEquals(0, CountAction.execute(PERSON, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void insertTransactionPerson(Connection connection) throws Exception
   {
      try(PreparedStatement statement = connection.prepareStatement("INSERT INTO person (id, first_name, last_name, email) VALUES (1001, 'Transaction', 'Sample', 'transaction@example.invalid')"))
      {
         assertEquals(1, statement.executeUpdate());
      }
   }



   /*******************************************************************************
    ** A narrowly scoped fixture for the supported metadata-personalizer contract.
    *******************************************************************************/
   public static class HideSalary implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(!PERSON.equals(input.getTableName()) || !QInputSource.USER.equals(input.getInputSource()))
         {
            return input.getTable();
         }
         QTableMetaData personalized = input.getTable().clone();
         personalized.getFields().remove("annualSalary");
         return personalized;
      }
   }



   /*******************************************************************************
    ** A synthetic unavailable USER policy; trusted SYSTEM behavior remains available.
    *******************************************************************************/
   public static class FailingPetPersonalizer implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input) throws QException
      {
         if("pet".equals(input.getTableName()) && QInputSource.USER.equals(input.getInputSource()))
         {
            throw new QException("Sample user field policy is unavailable.");
         }
         return input.getTable();
      }
   }
}
