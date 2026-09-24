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

package com.kingsrook.sampleapp;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
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
 ** Invocation, replica routing and caller-owned transactions use the sample's
 ** bundled H2 schema. These cases do not imply transaction or replica support
 ** in other backend providers.
 *******************************************************************************/
class SampleQueryInvocationTest
{
   private QInstance instance;
   private RDBMSBackendMetaData primary;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      primary = (RDBMSBackendMetaData) instance.getBackend(SampleMetaDataProvider.RDBMS_BACKEND_NAME);
      QContext.init(instance, new QSession());
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
    ** Missing table identity is an error, distinct from an empty successful read.
    *******************************************************************************/
   @Test
   void testMissingAndUnknownTablesRejectWithoutReturningRows() throws Exception
   {
      List<List<String>> before = rows(primary, "SELECT * FROM person ORDER BY id");
      assertEquals("Table name was not specified in query input",
         assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput())).getMessage());
      assertEquals("A table named [missingQueryTable] was not found in the active QInstance",
         assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput("missingQueryTable"))).getMessage());
      assertEquals(List.of(), query(personInput(99999)));
      assertEquals(List.of(List.of("1", "Avery")), query(personInput(1)));
      assertEquals(before, rows(primary, "SELECT * FROM person ORDER BY id"));
   }



   /*******************************************************************************
    ** Build valid input before removing context, then prove normal reads recover.
    *******************************************************************************/
   @Test
   void testMissingSessionAndContextRejectWithoutMutation() throws Exception
   {
      List<List<String>> before = rows(primary, "SELECT * FROM person ORDER BY id");
      QueryInput input = personInput(1);
      try
      {
         QContext.setQSession(null);
         assertEquals("QSession was not set in QContext.", assertThrows(QException.class, () -> new QueryAction().execute(input)).getMessage());
         QContext.clear();
         assertEquals("QInstance was not set in QContext.", assertThrows(QException.class, () -> new QueryAction().execute(input)).getMessage());
      }
      finally
      {
         QContext.init(instance, new QSession());
      }
      assertEquals(List.of(List.of("1", "Avery")), query(input));
      assertEquals(before, rows(primary, "SELECT * FROM person ORDER BY id"));
   }



   /*******************************************************************************
    ** Malformed operators, fixed arities, values and sorts must reject the read.
    *******************************************************************************/
   @Test
   void testMalformedFiltersRejectWithoutPublishingRows() throws Exception
   {
      List<List<String>> before = rows(primary, "SELECT * FROM person ORDER BY id");
      List<QQueryFilter> invalid = List.of(
         new QQueryFilter(new QFilterCriteria("id", null, 1)),
         new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, 1)),
         new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, 1, 2, 3)),
         new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1, 2)),
         new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS)),
         new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, "invalid-integer")),
         new QQueryFilter().withOrderBy(new QFilterOrderBy("missingSortField")));
      assertAll(invalid.stream().map(filter -> (Executable) () -> assertAll(
         () -> assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput("person").withFilter(filter)), filter.toString()),
         () -> assertEquals(before, rows(primary, "SELECT * FROM person ORDER BY id")))));
      assertEquals(List.of(List.of("1", "Avery")), query(personInput(1)));
   }



   /*******************************************************************************
    ** Malformed filter structure must not silently broaden Query or Count.
    *******************************************************************************/
   @Test
   void testMalformedCriteriaRejectQueryAndCountBeforePublication() throws Exception
   {
      List<List<String>> before = rows(primary, "SELECT * FROM person ORDER BY id");
      Map<String, QQueryFilter> invalid = Map.ofEntries(
         Map.entry("missing operator", new QQueryFilter(new QFilterCriteria("id", null, 1))),
         Map.entry("nested missing operator", new QQueryFilter().withSubFilter(new QQueryFilter(new QFilterCriteria("id", null, 1)))),
         Map.entry("missing field", new QQueryFilter(new QFilterCriteria().withOperator(QCriteriaOperator.EQUALS).withValues(List.of(1)))),
         Map.entry("blank field", new QQueryFilter(new QFilterCriteria(" ", QCriteriaOperator.EQUALS, 1))),
         Map.entry("null criterion", new QQueryFilter().withCriteria((QFilterCriteria) null)),
         Map.entry("missing sort field", new QQueryFilter().withOrderBy(new QFilterOrderBy(null))),
         Map.entry("null sort", new QQueryFilter().withOrderBy((QFilterOrderBy) null)),
         Map.entry("null subfilter", new QQueryFilter().withSubFilters(Arrays.asList((QQueryFilter) null))));
      assertAll(invalid.entrySet().stream().map(entry -> (Executable) () ->
      {
         QQueryFilter filter = entry.getValue();
         assertAll(entry.getKey(),
            () -> assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput("person").withFilter(filter))),
            () -> assertThrows(QException.class, () -> CountAction.execute("person", filter)),
            () ->
            {
               RecordPipe pipe = new RecordPipe(10);
               try
               {
                  assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput("person").withFilter(filter).withRecordPipe(pipe)));
                  assertEquals(0, pipe.countAvailableRecords());
                  assertEquals(0, pipe.getTotalRecordCount());
               }
               finally
               {
                  pipe.terminate();
               }
            },
            () -> assertEquals(before, rows(primary, "SELECT * FROM person ORDER BY id")));
      }));
   }



   /*******************************************************************************
    ** Native execution failure must not commit, close, or lose the caller's work.
    *******************************************************************************/
   @Test
   void testCallerTransactionSurvivesSuccessfulAndFailedQueries() throws Exception
   {
      List<List<String>> before = rows(primary, "SELECT * FROM person ORDER BY id");
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(primary)))
      {
         Connection connection = transaction.getConnection();
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE person SET first_name='Pending Query' WHERE id=1"));
            assertEquals(1, statement.executeUpdate("INSERT INTO person (id,first_name,last_name,email) VALUES (99,'Pending New','Sample','pending-query@example.invalid')"));
         }
         QueryInput input = personInput(1, 99).withTransaction(transaction);
         assertEquals(List.of(List.of("1", "Pending Query"), List.of("99", "Pending New")), query(input));
         assertEquals(rows(connection, "SELECT id,first_name FROM person WHERE id IN (1,99) ORDER BY id"), query(input));
         assertEquals(List.of(List.of("1", "Avery")), query(personInput(1, 99)));
         assertEquals(before, rows(primary, "SELECT * FROM person ORDER BY id"));
         assertSame(transaction, input.getTransaction());
         assertSame(connection, transaction.getConnection());
         assertFalse(connection.isClosed());
         assertFalse(connection.getAutoCommit());

         QFieldMetaData firstName = instance.getTable("person").getField("firstName");
         String originalColumn = firstName.getBackendName();
         try
         {
            firstName.setBackendName("missing_native_query_column");
            QException failure = assertThrows(QException.class, () -> query(personInput(1, 99).withTransaction(transaction)));
            Throwable cause = failure;
            while(!(cause instanceof SQLException) && cause.getCause() != null)
            {
               cause = cause.getCause();
            }
            assertEquals(42122, assertInstanceOf(SQLException.class, cause).getErrorCode(), "The owned nonexistent column must reach H2 execution");
         }
         finally
         {
            firstName.setBackendName(originalColumn);
         }
         assertFalse(connection.isClosed());
         assertFalse(connection.getAutoCommit());
         assertEquals(List.of(List.of("1", "Pending Query"), List.of("99", "Pending New")), query(input));
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE person SET first_name='Still Owned' WHERE id=2"));
         }
         assertEquals(List.of(List.of("2", "Still Owned")), query(personInput(2).withTransaction(transaction)));
         assertEquals(before, rows(primary, "SELECT * FROM person ORDER BY id"));
         transaction.rollback();
         assertFalse(connection.isClosed());
         assertEquals(List.of(List.of("1", "Avery")), query(input));
      }
      assertEquals(before, rows(primary, "SELECT * FROM person ORDER BY id"));
   }



   /*******************************************************************************
    ** Query leaves commit ownership with the caller, including subsequent reads.
    *******************************************************************************/
   @Test
   void testOnlyCallerCommitMakesPendingChangesVisible() throws Exception
   {
      List<List<String>> unrelated = rows(primary, "SELECT * FROM person WHERE id <> 3 ORDER BY id");
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(primary)))
      {
         try(Statement statement = transaction.getConnection().createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE person SET first_name='Caller Committed' WHERE id=3"));
         }
         assertEquals(List.of(List.of("3", "Caller Committed")), query(personInput(3).withTransaction(transaction)));
         assertEquals(List.of(List.of("3", "Casey")), query(personInput(3)));
         assertEquals(List.of(List.of("Casey")), rows(primary, "SELECT first_name FROM person WHERE id=3"));
         assertFalse(transaction.getConnection().isClosed());
         assertFalse(transaction.getConnection().getAutoCommit());
         transaction.commit();
         assertFalse(transaction.getConnection().isClosed());
         assertEquals(List.of(List.of("3", "Caller Committed")), query(personInput(3)));
         assertEquals(List.of(List.of("3", "Caller Committed")), query(personInput(3).withTransaction(transaction)));
         assertEquals(List.of(List.of("Caller Committed")), rows(primary, "SELECT first_name FROM person WHERE id=3"));
      }
      assertEquals(List.of(List.of("Caller Committed")), rows(primary, "SELECT first_name FROM person WHERE id=3"));
      assertEquals(unrelated, rows(primary, "SELECT * FROM person WHERE id <> 3 ORDER BY id"));
   }



   /*******************************************************************************
    ** Distinct native rows prove hint routing and explicit transaction precedence.
    *******************************************************************************/
   @Test
   void testReadOnlyHintFallbackReplicaAndTransactionPriority() throws Exception
   {
      List<List<String>> primaryBefore = rows(primary, "SELECT * FROM person ORDER BY id");
      QueryInput hinted = personInput(1, 2, 6).withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
      List<List<String>> primaryExpected = List.of(List.of("1", "Avery"), List.of("2", "Blair"));
      assertEquals(primaryExpected, query(hinted));
      RDBMSBackendMetaData previousReplica = primary.getReadOnlyBackendMetaData();
      String uniqueName = UUID.randomUUID().toString();
      RDBMSBackendMetaData replica = SampleMetaDataProvider.defineRdbmsBackend().withName("queryReplica_" + uniqueName)
         .withDatabaseName("sample_query_replica_" + uniqueName);
      try(Connection replicaConnection = ConnectionManager.getConnection(replica);
          Statement statement = replicaConnection.createStatement();
          InputStream seed = SampleQueryInvocationTest.class.getResourceAsStream("/prime-test-database.sql"))
      {
         assertNotNull(seed);
         assertTrue(replicaConnection.getMetaData().getURL().startsWith("jdbc:h2:mem:sample_query_replica_"));
         try(InputStreamReader reader = new InputStreamReader(seed, StandardCharsets.UTF_8))
         {
            RunScript.execute(replicaConnection, reader);
         }
         assertEquals(1, statement.executeUpdate("UPDATE person SET first_name='Replica Avery' WHERE id=1"));
         assertEquals(1, statement.executeUpdate("DELETE FROM person WHERE id=2"));
         assertEquals(1, statement.executeUpdate("INSERT INTO person (id,first_name,last_name,email) VALUES (6,'Replica Only','Sample','replica-query@example.invalid')"));
         List<List<String>> replicaBefore = rows(replicaConnection, "SELECT * FROM person ORDER BY id");
         List<List<String>> replicaExpected = List.of(List.of("1", "Replica Avery"), List.of("6", "Replica Only"));
         assertEquals(replicaExpected, rows(replicaConnection, "SELECT id,first_name FROM person WHERE id IN (1,2,6) ORDER BY id"));
         primary.setReadOnlyBackendMetaData(replica);
         assertEquals(primaryExpected, query(personInput(1, 2, 6)));
         assertEquals(primaryExpected, query(personInput(1, 2, 6).withQueryHints(null)));
         assertEquals(replicaExpected, query(hinted));
         try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(primary));
             Statement write = transaction.getConnection().createStatement())
         {
            assertEquals(1, write.executeUpdate("UPDATE person SET first_name='Transaction Avery' WHERE id=1"));
            QueryInput transactionInput = personInput(1, 2, 6).withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND).withTransaction(transaction);
            List<List<String>> pending = List.of(List.of("1", "Transaction Avery"), List.of("2", "Blair"));
            assertEquals(pending, rows(transaction.getConnection(), "SELECT id,first_name FROM person WHERE id IN (1,2,6) ORDER BY id"));
            assertEquals(pending, query(transactionInput));
            assertEquals(replicaExpected, query(hinted));
            assertEquals(primaryExpected, query(personInput(1, 2, 6)));
            assertFalse(transaction.getConnection().isClosed());
            assertFalse(transaction.getConnection().getAutoCommit());
            assertSame(transaction, transactionInput.getTransaction());
            assertTrue(transactionInput.hasQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND));
            transaction.rollback();
            assertEquals(primaryExpected, query(transactionInput));
         }
         assertTrue(hinted.hasQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND));
         assertEquals(primaryBefore, rows(primary, "SELECT * FROM person ORDER BY id"));
         assertEquals(replicaBefore, rows(replicaConnection, "SELECT * FROM person ORDER BY id"));
      }
      finally
      {
         primary.setReadOnlyBackendMetaData(previousReplica);
      }
      assertEquals(primaryExpected, query(hinted));
      assertEquals(primaryBefore, rows(primary, "SELECT * FROM person ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryInput personInput(Integer... ids)
   {
      return new QueryInput("person").withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, List.of(ids)))
            .withOrderBy(new QFilterOrderBy("id")))
         .withFieldNamesToInclude(Set.of("id", "firstName"));
   }



   /*******************************************************************************
    ** Every expected Query result includes exact identities and projected fields.
    *******************************************************************************/
   private List<List<String>> query(QueryInput input) throws QException
   {
      List<List<String>> rows = new ArrayList<>();
      for(QRecord record : new QueryAction().execute(input).getRecords())
      {
         assertEquals(Set.of("id", "firstName"), record.getValues().keySet());
         rows.add(List.of(record.getValueString("id"), record.getValueString("firstName")));
      }
      return rows;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> rows(RDBMSBackendMetaData backend, String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         return rows(connection, sql);
      }
   }



   /*******************************************************************************
    ** Inspect caller-transaction visibility without closing the supplied connection.
    *******************************************************************************/
   private List<List<String>> rows(Connection connection, String sql) throws Exception
   {
      try(Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery(sql))
      {
         List<List<String>> rows = new ArrayList<>();
         while(result.next())
         {
            List<String> values = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               values.add(result.getString(column));
            }
            rows.add(values);
         }
         return rows;
      }
   }
}
