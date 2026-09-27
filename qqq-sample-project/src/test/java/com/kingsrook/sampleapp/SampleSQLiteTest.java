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


import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.C3P0PooledConnectionProvider;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.SimpleConnectionProvider;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.ConnectionPoolSettings;
import com.kingsrook.qqq.backend.module.sqlite.SQLiteBackendModule;
import com.kingsrook.qqq.backend.module.sqlite.model.metadata.SQLiteBackendMetaData;
import com.kingsrook.qqq.backend.module.sqlite.model.metadata.SQLiteTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.ProgressHandler;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Canonical Field Lab on an owned SQLite file, checked against native SQL.
 *******************************************************************************/
class SampleSQLiteTest
{
   private static final String TABLE = "fieldLab";
   private static final List<Connection> OPENED = new CopyOnWriteArrayList<>();
   private static ComboPooledDataSource ownedPool;

   @TempDir
   Path directory;

   private SQLiteBackendMetaData backend;
   private QInstance instance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      OPENED.clear();
      backend = new SQLiteBackendMetaData().withName("ownedSQLite").withPath(directory.resolve("sample.db").toString());
      backend.setConnectionProvider(new QCodeReference(ObservedConnections.class));
      backend.setQueriesForNewConnections(List.of("PRAGMA foreign_keys = ON"));
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addBackend(backend);
      instance.getTable(TABLE).withBackendName(backend.getName()).withBackendDetails(new SQLiteTableBackendDetails().withTableName("field_lab"));
      instance.addTable(new QTableMetaData().withName("ownedCategory").withBackendName(backend.getName())
         .withBackendDetails(new SQLiteTableBackendDetails().withTableName("owned_category")).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.LONG)).withField(new QFieldMetaData("name", QFieldType.STRING)));
      instance.addTable(new QTableMetaData().withName("ownedIdentity").withBackendName(backend.getName())
         .withBackendDetails(new SQLiteTableBackendDetails().withTableName("owned_identity")).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      instance.addTable(new QTableMetaData().withName("ownedSlow").withBackendName(backend.getName())
         .withBackendDetails(new SQLiteTableBackendDetails().withTableName("owned_slow")).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.LONG)));
      instance.addJoin(new QJoinMetaData().withName("ownedFieldCategory").withLeftTable(TABLE).withRightTable("ownedCategory")
         .withType(JoinType.MANY_TO_ONE).withJoinOn(new JoinOn("longValue", "id")));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("ownedSQLiteOwner"));
      new QInstanceValidator().validate(instance);
      QContext.init(instance, new QSession());
      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement();
          InputStream schema = getClass().getResourceAsStream("/database/field-lab-sqlite.sql"))
      {
         assertNotNull(schema);
         statement.executeUpdate(new String(schema.readAllBytes(), StandardCharsets.UTF_8));
         statement.executeUpdate("CREATE TABLE owned_category(id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
         statement.executeUpdate("INSERT INTO owned_category VALUES (1,'Alpha'),(2,'Beta')");
         statement.executeUpdate("CREATE TABLE owned_identity(id INTEGER PRIMARY KEY AUTOINCREMENT)");
         statement.executeUpdate("CREATE VIEW owned_slow AS WITH RECURSIVE seq(x) AS (VALUES(1) UNION ALL SELECT x+1 FROM seq WHERE x<1000000000) SELECT SUM(x) AS id FROM seq");
      }
   }



   /*******************************************************************************
    ** Close any leaked fixture handle before reporting the failure.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      List<Connection> leaked = new ArrayList<>();
      for(Connection connection : OPENED)
      {
         if(!connection.isClosed())
         {
            leaked.add(connection);
            connection.close();
         }
      }
      OPENED.clear();
      if(ownedPool != null)
      {
         ownedPool.close();
         ownedPool = null;
      }
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
      assertTrue(leaked.isEmpty(), "Every acquired connection must be closed by its owner");
   }



   /*******************************************************************************
    ** Batch RETURNING and DEFAULT VALUES both preserve generated identities.
    *******************************************************************************/
   @Test
   void testSingleBatchAndDefaultGeneratedKeys() throws Exception
   {
      QRecord first = insert(record("First", 1L));
      List<QRecord> batch = new InsertAction().execute(new InsertInput(TABLE)
         .withRecords(List.of(record("Second", 1L), record("Third", 2L)))).getRecords();
      assertEquals(2, batch.size());
      assertTrue(batch.stream().allMatch(row -> row.getErrors().isEmpty()));
      assertNotEquals(batch.get(0).getValue("id"), batch.get(1).getValue("id"));
      for(QRecord row : List.of(first, batch.get(0), batch.get(1)))
      {
         assertNotNull(row.getValue("id"));
         assertEquals(row.getValue("name"), GetAction.execute(TABLE, row.getValueInteger("id")).getValue("name"));
      }
      assertEquals(List.of("1:First", "2:Second", "3:Third"), rows("SELECT id || ':' || name FROM field_lab ORDER BY id"));
      QRecord defaultRow = new InsertAction().executeForRecord(new InsertInput("ownedIdentity").withRecord(new QRecord()));
      assertTrue(defaultRow.getErrors().isEmpty());
      assertEquals(1, defaultRow.getValueInteger("id"));
      assertEquals(List.of("1"), rows("SELECT id FROM owned_identity"));
   }



   /*******************************************************************************
    ** SQLite's temporal strings are compared with Java civil/instant values.
    *******************************************************************************/
   @Test
   void testTypedValuesAndSparseUpdateDelete() throws Exception
   {
      LocalDate date = LocalDate.of(2026, 9, 24);
      LocalTime time = LocalTime.of(12, 34, 56);
      Instant instant = Instant.parse("2026-09-24T17:34:56Z");
      QRecord inserted = insert(record("Typed", 9007199254740993L).withValue("decimalValue", new BigDecimal("12.25"))
         .withValue("booleanValue", false).withValue("dateValue", date).withValue("timeValue", time).withValue("dateTimeValue", instant)
         .withValue("textValue", "Line one\nLine two").withValue("htmlValue", "<p>Owned</p>").withValue("passwordValue", "sample-only-password")
         .withValue("blobValue", new byte[] { 0, 1, 2, 127 }));
      insert(record("Unrelated", 2L));
      Integer id = inserted.getValueInteger("id");
      QRecord read = GetAction.execute(TABLE, id);
      assertEquals(9007199254740993L, read.getValue("longValue"));
      assertEquals(0, new BigDecimal("12.25").compareTo((BigDecimal) read.getValue("decimalValue")));
      assertEquals(false, read.getValue("booleanValue"));
      assertEquals(date, read.getValue("dateValue"));
      assertEquals(time, read.getValue("timeValue"));
      assertEquals(instant, read.getValue("dateTimeValue"));
      assertEquals("Line one\nLine two", read.getValue("textValue"));
      assertEquals("<p>Owned</p>", read.getValue("htmlValue"));
      assertEquals("************", read.getValue("passwordValue"));
      assertArrayEquals(new byte[] { 0, 1, 2, 127 }, (byte[]) new GetAction().execute(new GetInput(TABLE).withPrimaryKey(id)
         .withShouldFetchHeavyFields(true)).getRecord().getValue("blobValue"));
      assertEquals(List.of("0001027F"), rows("SELECT HEX(blob_value) FROM field_lab WHERE id=1"));
      assertEquals(List.of("2026-09-24:12:34:56:2026-09-24T17:34:56Z:9007199254740993:12.25:0"), rows(
         "SELECT date_value || ':' || time_value || ':' || date_time_value || ':' || long_value || ':' || decimal_value || ':' || boolean_value FROM field_lab WHERE id=1"));
      QRecord changed = new UpdateAction().execute(new UpdateInput(TABLE)
         .withRecord(new QRecord().withValue("id", id).withValue("name", "Changed").withValue("textValue", null))).getRecords().get(0);
      assertTrue(changed.getErrors().isEmpty());
      assertEquals(List.of("Changed:1:12.25", "Unrelated:1:"), rows("SELECT name || ':' || (text_value IS NULL) || ':' || COALESCE(decimal_value,'') FROM field_lab ORDER BY id"));
      assertEquals(1, new DeleteAction().execute(new DeleteInput(TABLE).withPrimaryKey(id)).getDeletedRecordCount());
      assertNull(GetAction.execute(TABLE, id));
      assertEquals(List.of("Unrelated"), rows("SELECT name FROM field_lab"));
   }



   /*******************************************************************************
    ** Native tuples independently establish filter, pagination, joins and aggregates.
    *******************************************************************************/
   @Test
   void testQueryCountAggregateAndJoins() throws Exception
   {
      for(Integer i = 1; i <= 4; i++)
      {
         insert(record("Row " + i, i % 2 == 0 ? 2L : 1L).withValue("decimalValue", new BigDecimal(i * 10)));
      }
      QQueryFilter filter = new QQueryFilter().withCriteria(new QFilterCriteria("decimalValue", QCriteriaOperator.GREATER_THAN, 10))
         .withOrderBy(new QFilterOrderBy("decimalValue", false)).withSkip(1).withLimit(2);
      List<QRecord> selected = new QueryAction().execute(new QueryInput(TABLE).withFilter(filter)).getRecords();
      assertEquals(rows("SELECT name FROM field_lab WHERE decimal_value>10 ORDER BY decimal_value DESC LIMIT 2 OFFSET 1"), selected.stream().map(row -> row.getValueString("name")).toList());
      assertEquals(3, new CountAction().execute(new CountInput(TABLE).withFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("decimalValue", QCriteriaOperator.GREATER_THAN, 10)))).getCount());
      Aggregate sum = new Aggregate("decimalValue", AggregateOperator.SUM);
      Aggregate count = new Aggregate("id", AggregateOperator.COUNT);
      var total = new AggregateAction().execute(new AggregateInput(TABLE).withAggregates(List.of(sum, count))).getResults().get(0);
      assertEquals(new BigDecimal(rows("SELECT SUM(decimal_value) FROM field_lab").get(0)), new BigDecimal(total.getAggregateValue(sum).toString()));
      assertEquals(4L, ((Number) total.getAggregateValue(count)).longValue());
      GroupBy owner = new GroupBy(QFieldType.LONG, "longValue");
      assertEquals(rows("SELECT long_value || ':' || SUM(decimal_value) || ':' || COUNT(id) FROM field_lab GROUP BY long_value ORDER BY long_value"),
         new AggregateAction().execute(new AggregateInput(TABLE).withAggregates(List.of(sum, count)).withGroupBy(owner)).getResults().stream()
            .map(row -> row.getGroupByValue(owner) + ":" + ((Number) row.getAggregateValue(sum)).longValue() + ":" + ((Number) row.getAggregateValue(count)).longValue()).sorted().toList());
      QueryInput joined = new QueryInput(TABLE).withQueryJoin(new QueryJoin(instance.getJoin("ownedFieldCategory"))
         .withAlias("category").withSelect(true).withType(QueryJoin.Type.INNER))
         .withFieldNamesToInclude(Set.of("id", "category.name"))
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
      assertEquals(rows("SELECT f.id || ':' || c.name FROM field_lab f JOIN owned_category c ON f.long_value=c.id ORDER BY f.id"),
         new QueryAction().execute(joined).getRecords().stream().map(row -> row.getValueInteger("id") + ":" + row.getValueString("category.name")).toList());
      insert(record("Orphan", 99L));
      QueryInput left = new QueryInput(TABLE).withQueryJoin(new QueryJoin(instance.getJoin("ownedFieldCategory"))
         .withAlias("category").withSelect(true).withType(QueryJoin.Type.LEFT))
         .withFieldNamesToInclude(Set.of("id", "category.name"))
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
      assertEquals(rows("SELECT f.id || ':' || COALESCE(c.name,'null') FROM field_lab f LEFT JOIN owned_category c ON f.long_value=c.id ORDER BY f.id"),
         new QueryAction().execute(left).getRecords().stream().map(row -> row.getValueInteger("id") + ":" + row.getValueString("category.name")).toList());
   }



   /*******************************************************************************
    ** Rejected input and failed native batches leave the existing record intact.
    *******************************************************************************/
   @Test
   void testDuplicateInvalidInputsAndNativeBatchFailure() throws Exception
   {
      insert(record("Existing", 1L));
      QRecord duplicate = new InsertAction().executeForRecord(new InsertInput(TABLE).withRecord(record("Existing", 2L)));
      assertFalse(duplicate.getErrors().isEmpty());
      assertThrows(QException.class, () -> new InsertAction().executeForRecord(new InsertInput(TABLE)
         .withRecord(record("Invalid", 1L).withValue("decimalValue", "not-a-number"))));
      assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput(TABLE)
         .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria("missingField", QCriteriaOperator.EQUALS, 1)))));
      assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TABLE).withSkipUniqueKeyCheck(true)
         .withRecords(List.of(record("Duplicate batch", 1L), record("Duplicate batch", 1L)))));
      assertThrows(IllegalStateException.class, () -> new SQLiteBackendModule().getStorageInterface());
      assertEquals(List.of("Existing"), rows("SELECT name FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    ** Read and write policies use native owner values on this provider too.
    *******************************************************************************/
   @Test
   void testRecordLocksProtectReadsAndWrites() throws Exception
   {
      QRecord allowed = insert(record("Allowed", 1L));
      QRecord denied = insert(record("Denied", 2L));
      instance.getTable(TABLE).withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("ownedSQLiteOwner").withFieldName("longValue"));
      QContext.getQSession().withSecurityKeyValue("ownedSQLiteOwner", 1L);
      assertEquals(List.of("Allowed"), new QueryAction().execute(new QueryInput(TABLE)).getRecords().stream().map(row -> row.getValueString("name")).toList());
      assertEquals(1, CountAction.execute(TABLE, null));
      assertNotNull(GetAction.execute(TABLE, allowed.getValueInteger("id")));
      assertNull(GetAction.execute(TABLE, denied.getValueInteger("id")));
      Aggregate count = new Aggregate("id", AggregateOperator.COUNT);
      assertEquals(1L, ((Number) new AggregateAction().execute(new AggregateInput(TABLE).withAggregate(count)).getResults().get(0).getAggregateValue(count)).longValue());
      assertFalse(new InsertAction().executeForRecord(new InsertInput(TABLE).withRecord(record("Rejected", 2L))).getErrors().isEmpty());
      assertFalse(new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", denied.getValueInteger("id"))
         .withValue("name", "Taken"))).getRecords().get(0).getErrors().isEmpty());
      assertEquals(0, new DeleteAction().execute(new DeleteInput(TABLE).withPrimaryKey(denied.getValueInteger("id"))).getDeletedRecordCount());
      assertEquals(List.of("Allowed:1", "Denied:2"), rows("SELECT name || ':' || long_value FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    ** Commit, rollback and statement failure leave transaction ownership explicit.
    *******************************************************************************/
   @Test
   void testCallerTransactionCommitRollbackAndReuse() throws Exception
   {
      Connection connection = ConnectionManager.getConnection(backend);
      try(RDBMSTransaction transaction = new RDBMSTransaction(connection))
      {
         QRecord pending = new InsertAction().executeForRecord(new InsertInput(TABLE).withTransaction(transaction).withRecord(record("Pending", 1L)));
         assertTrue(pending.getErrors().isEmpty());
         assertEquals(1, new CountAction().execute(new CountInput(TABLE).withTransaction(transaction)).getCount());
         assertEquals(List.of("0"), rows("SELECT COUNT(*) FROM field_lab"));
         assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction)
            .withSkipUniqueKeyCheck(true).withRecord(record("Pending", 1L))));
         assertFalse(connection.isClosed());
         assertEquals(List.of("Pending"), rows(connection, "SELECT name FROM field_lab"));
         transaction.rollback();
         assertEquals(List.of("0"), rows("SELECT COUNT(*) FROM field_lab"));
         QRecord committed = new InsertAction().executeForRecord(new InsertInput(TABLE).withTransaction(transaction).withRecord(record("Committed", 2L)));
         assertTrue(committed.getErrors().isEmpty());
         transaction.commit();
         assertEquals(List.of("Committed"), rows("SELECT name FROM field_lab"));
         assertNotNull(new GetAction().execute(new GetInput(TABLE).withTransaction(transaction).withPrimaryKey(committed.getValueInteger("id"))).getRecord());
      }
      assertTrue(connection.isClosed());
      try(Connection fresh = ConnectionManager.getConnection(backend))
      {
         assertEquals(List.of("Committed"), rows(fresh, "SELECT name FROM field_lab"));
         assertEquals(List.of("1"), rows(fresh, "PRAGMA foreign_keys"));
      }
   }



   /*******************************************************************************
    ** File/open and missing-table errors do not poison subsequent valid operations.
    *******************************************************************************/
   @Test
   void testMissingTableAndConnectionFailureRecover() throws Exception
   {
      insert(record("Preserved", 1L));
      SQLiteTableBackendDetails details = (SQLiteTableBackendDetails) instance.getTable(TABLE).getBackendDetails();
      details.setTableName("does_not_exist");
      assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput(TABLE)));
      details.setTableName("field_lab");
      String original = backend.getPath();
      backend.setPath(directory.resolve("missing-directory/database.db").toString());
      assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput(TABLE)));
      backend.setPath(original);
      assertEquals("Preserved", GetAction.execute(TABLE, 1).getValueString("name"));
      assertEquals(List.of("Preserved"), rows("SELECT name FROM field_lab"));
   }



   /*******************************************************************************
    ** Observe real SQLite VM progress before requesting cancellation.
    *******************************************************************************/
   @Test
   @Timeout(30)
   void testTimeoutCancellationAndConnectionRecovery() throws Exception
   {
      insert(record("Preserved", 1L));
      for(Boolean cancel : List.of(false, true))
      {
         ExecutorService worker = Executors.newSingleThreadExecutor();
         QueryAction action = new QueryAction();
         try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(backend)))
         {
            CountDownLatch executing = new CountDownLatch(1);
            ProgressHandler.setHandler(transaction.getConnection(), 1000, new ProgressHandler()
            {
               /*******************************************************************************
                ** Observe only: returning zero lets SQLite continue normally.
                *******************************************************************************/
               @Override
               protected int progress()
               {
                  executing.countDown();
                  return 0;
               }
            });
            Future<QueryOutput> future = worker.submit(() ->
            {
               try
               {
                  QContext.init(instance, new QSession());
                  return action.execute(new QueryInput("ownedSlow").withTransaction(transaction).withTimeoutSeconds(cancel ? null : 1));
               }
               finally
               {
                  QContext.clear();
               }
            });
            try
            {
               assertTrue(executing.await(5, TimeUnit.SECONDS), "Native SQLite VM must begin execution");
               if(cancel)
               {
                  action.cancel();
               }
               ExecutionException failure = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
               QUserFacingException cause = assertInstanceOf(QUserFacingException.class, failure.getCause());
               assertEquals(cancel ? "Query was cancelled." : "Query timed out.", cause.getMessage());
               assertFalse(transaction.getConnection().isClosed());
               assertEquals(List.of("Preserved"), rows(transaction.getConnection(), "SELECT name FROM field_lab"));
               transaction.rollback();
            }
            finally
            {
               action.cancel();
               future.cancel(true);
               worker.shutdownNow();
               assertTrue(worker.awaitTermination(10, TimeUnit.SECONDS));
               ProgressHandler.clearHandler(transaction.getConnection());
            }
         }
         finally
         {
            worker.shutdownNow();
         }
      }
      assertEquals("Preserved", GetAction.execute(TABLE, 1).getValueString("name"));
      assertEquals(List.of("Preserved"), rows("SELECT name FROM field_lab"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPooledConnectionReuseExhaustionAndCleanup() throws Exception
   {
      insert(record("Pooled", 1L));
      ConnectionManager.resetConnectionProviders();
      backend.setConnectionProvider(new QCodeReference(ObservedPool.class));
      backend.setConnectionPoolSettings(new ConnectionPoolSettings().withInitialPoolSize(1).withMinPoolSize(1)
         .withMaxPoolSize(1).withCheckoutTimeoutSeconds(1));
      Connection first = ConnectionManager.getConnection(backend);
      try
      {
         assertEquals(1, ownedPool.getNumBusyConnectionsDefaultUser());
         assertThrows(SQLException.class, () -> ConnectionManager.getConnection(backend));
      }
      finally
      {
         first.close();
      }
      assertTrue(first.isClosed());
      try(Connection second = ConnectionManager.getConnection(backend))
      {
         assertEquals(1, ownedPool.getNumConnectionsDefaultUser());
         assertEquals(List.of("Pooled"), rows(second, "SELECT name FROM field_lab"));
         assertEquals(List.of("1"), rows(second, "PRAGMA foreign_keys"));
      }
      assertEquals("Pooled", GetAction.execute(TABLE, 1).getValueString("name"));
      ((SQLiteTableBackendDetails) instance.getTable(TABLE).getBackendDetails()).setTableName("does_not_exist");
      assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput(TABLE)));
      assertEquals(0, ownedPool.getNumBusyConnectionsDefaultUser());
      ownedPool.close();
      assertThrows(SQLException.class, () -> ownedPool.getConnection());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord record(String name, Long owner)
   {
      return new QRecord().withValue("name", name).withValue("longValue", owner);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insert(QRecord record) throws Exception
   {
      QRecord result = new InsertAction().executeForRecord(new InsertInput(TABLE).withRecord(record));
      assertTrue(result.getErrors().isEmpty(), result.getErrorsAsString());
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> rows(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         return rows(connection, sql);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> rows(Connection connection, String sql) throws Exception
   {
      try(Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         List<String> rows = new ArrayList<>();
         while(result.next())
         {
            rows.add(result.getString(1));
         }
         return rows;
      }
   }



   /*******************************************************************************
    ** Tracks native connections without replacing JDBC behavior.
    *******************************************************************************/
   public static class ObservedConnections extends SimpleConnectionProvider
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Connection getConnection() throws SQLException
      {
         Connection connection = super.getConnection();
         OPENED.add(connection);
         return connection;
      }
   }



   /*******************************************************************************
    ** Retains the real pool solely to verify and clean up this fixture's resources.
    *******************************************************************************/
   public static class ObservedPool extends C3P0PooledConnectionProvider
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      protected void customizePool(ComboPooledDataSource pool)
      {
         ownedPool = pool;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Connection getConnection() throws SQLException
      {
         Connection connection = super.getConnection();
         OPENED.add(connection);
         return connection;
      }
   }
}
