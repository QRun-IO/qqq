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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
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
import com.kingsrook.qqq.backend.module.postgres.PostgreSQLBackendModule;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.C3P0PooledConnectionProvider;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.ConnectionPoolSettings;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.DataSources;
import com.mchange.v2.c3p0.PooledDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Native PostgreSQL oracles for the sample consumer's data and lifecycle APIs.
 *******************************************************************************/
@Timeout(30)
class SamplePostgresAcceptanceIT
{
   private static final String TABLE = "postgresItem";
   private static final String CATEGORY = "postgresCategory";
   private static final String JOIN = "postgresItemCategory";
   private static PostgreSQLContainer database;
   private PostgreSQLBackendMetaData backend;
   private QInstance instance;



   /*******************************************************************************
    ** Testcontainers selects a free host port and owns this synthetic database.
    *******************************************************************************/
   @BeforeAll
   static void startDatabase()
   {
      database = new PostgreSQLContainer("postgres:17-alpine")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only");
      database.start();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterAll
   static void stopDatabase()
   {
      if(database != null)
      {
         database.close();
      }
   }



   /*******************************************************************************
    ** Each method starts from native SQL, independently of the actions under test.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      backend = new PostgreSQLBackendMetaData().withName("pg-acceptance-" + UUID.randomUUID())
         .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
         .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword());
      backend.setQueriesForNewConnections(List.of("SET TIME ZONE 'UTC'", "SET statement_timeout = '5s'",
         "SET application_name = '" + backend.getName() + "'"));
      try(Connection connection = nativeConnection(); Statement statement = connection.createStatement();
          InputStream schema = getClass().getResourceAsStream("/database/postgres-acceptance.sql"))
      {
         assertNotNull(schema, "Owned PostgreSQL schema is required");
         statement.execute(new String(schema.readAllBytes(), StandardCharsets.UTF_8));
      }
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addBackend(backend);
      instance.addTable(new QTableMetaData().withName(TABLE).withBackendName(backend.getName()).withPrimaryKeyField("id")
         .withBackendDetails(new PostgreSQLTableBackendDetails().withTableName("pg_item"))
         .withField(field("id", "id", QFieldType.INTEGER)).withField(field("tenant", "tenant", QFieldType.INTEGER))
         .withField(field("name", "name", QFieldType.STRING)).withField(field("categoryId", "category_id", QFieldType.INTEGER))
         .withField(field("quantity", "quantity", QFieldType.LONG)).withField(field("amount", "amount", QFieldType.DECIMAL))
         .withField(field("enabled", "enabled", QFieldType.BOOLEAN)).withField(field("day", "day", QFieldType.DATE))
         .withField(field("atTime", "at_time", QFieldType.TIME)).withField(field("atInstant", "at_instant", QFieldType.DATE_TIME))
         .withField(field("note", "note", QFieldType.TEXT)).withField(field("html", "html", QFieldType.HTML))
         .withField(field("payload", "payload", QFieldType.BLOB)));
      instance.addTable(new QTableMetaData().withName(CATEGORY).withBackendName(backend.getName()).withPrimaryKeyField("id")
         .withBackendDetails(new PostgreSQLTableBackendDetails().withTableName("pg_category"))
         .withField(field("id", "id", QFieldType.INTEGER)).withField(field("label", "label", QFieldType.STRING)));
      instance.addJoin(new QJoinMetaData().withName(JOIN).withLeftTable(TABLE).withRightTable(CATEGORY)
         .withType(JoinType.MANY_TO_ONE).withJoinOn(new JoinOn("categoryId", "id")));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("pgTenant"));
      QContext.init(instance, new QSession());
      new QInstanceValidator().revalidate(instance);
   }



   /*******************************************************************************
    ** Registry reset is not a pool shutdown API; explicitly destroy only our pool.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         closeOwnedPools();
         if(backend != null)
         {
            awaitNoOwnedConnections();
         }
      }
      finally
      {
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Generated identity and all stored scalar/binary values have a JDBC oracle.
    *******************************************************************************/
   @Test
   void testCrudTypesGeneratedKeyAndNullRoundTrip() throws Exception
   {
      QRecord input = record("Zoë's native row").withValue("categoryId", 2).withValue("quantity", 9000000000L)
         .withValue("amount", new BigDecimal("12345.6789")).withValue("enabled", false)
         .withValue("day", LocalDate.of(2024, 2, 29)).withValue("atTime", LocalTime.of(23, 45, 12))
         .withValue("atInstant", Instant.parse("2024-02-29T23:45:12Z")).withValue("note", "line one\nline two")
         .withValue("html", "<b>synthetic</b>").withValue("payload", new byte[] { 0, 1, -1, 42 });
      QRecord inserted = insert(input);
      Integer id = inserted.getValueInteger("id");
      assertTrue(id >= 100);
      QRecord loaded = GetAction.execute(TABLE, id);
      assertInstanceOf(Integer.class, loaded.getValue("id"));
      assertInstanceOf(String.class, loaded.getValue("name"));
      assertInstanceOf(Long.class, loaded.getValue("quantity"));
      assertInstanceOf(BigDecimal.class, loaded.getValue("amount"));
      assertInstanceOf(Boolean.class, loaded.getValue("enabled"));
      assertInstanceOf(LocalDate.class, loaded.getValue("day"));
      assertInstanceOf(LocalTime.class, loaded.getValue("atTime"));
      assertInstanceOf(Instant.class, loaded.getValue("atInstant"));
      assertInstanceOf(byte[].class, loaded.getValue("payload"));
      try(Connection connection = nativeConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM pg_item WHERE id=?"))
      {
         statement.setInt(1, id);
         try(ResultSet result = statement.executeQuery())
         {
            assertTrue(result.next());
            assertEquals("Zoë's native row", result.getString("name"));
            assertEquals(result.getString("name"), loaded.getValueString("name"));
            assertEquals(9000000000L, result.getLong("quantity"));
            assertEquals(result.getLong("quantity"), loaded.getValueLong("quantity"));
            assertEquals(new BigDecimal("12345.6789"), result.getBigDecimal("amount"));
            assertEquals(result.getBigDecimal("amount"), loaded.getValueBigDecimal("amount"));
            assertFalse(result.getBoolean("enabled"));
            assertEquals(result.getBoolean("enabled"), loaded.getValueBoolean("enabled"));
            assertEquals(input.getValueLocalDate("day"), result.getObject("day", LocalDate.class));
            assertEquals(result.getObject("day", LocalDate.class), loaded.getValueLocalDate("day"));
            assertEquals(input.getValueLocalTime("atTime"), result.getObject("at_time", LocalTime.class));
            assertEquals(result.getObject("at_time", LocalTime.class), loaded.getValueLocalTime("atTime"));
            assertEquals(input.getValueInstant("atInstant"), loaded.getValueInstant("atInstant"));
            assertEquals("2024-02-29 23:45:12", result.getString("at_instant"));
            assertEquals("line one\nline two", result.getString("note"));
            assertEquals(result.getString("note"), loaded.getValueString("note"));
            assertEquals("<b>synthetic</b>", result.getString("html"));
            assertEquals(result.getString("html"), loaded.getValueString("html"));
            assertArrayEquals(new byte[] { 0, 1, -1, 42 }, result.getBytes("payload"));
            assertArrayEquals(result.getBytes("payload"), loaded.getValueByteArray("payload"));
            assertFalse(result.next());
         }
      }
      QRecord updated = new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord()
         .withValue("id", id).withValue("name", "Updated").withValue("amount", null))).getRecords().get(0);
      assertTrue(updated.getErrors().isEmpty());
      assertEquals(List.of(Arrays.asList("Updated", null, "9000000000")), sql("SELECT name,amount,quantity FROM pg_item WHERE id=" + id));
      assertEquals("Updated", GetAction.execute(TABLE, id).getValueString("name"));
      assertNull(GetAction.execute(TABLE, id).getValue("amount"));
      assertEquals(1, new DeleteAction().execute(new DeleteInput(TABLE).withPrimaryKey(id)).getDeletedRecordCount());
      assertNull(GetAction.execute(TABLE, id));
      assertTrue(sql("SELECT id FROM pg_item WHERE id=" + id).isEmpty());
      QRecord empty = insert(record("Null values"));
      QRecord emptyLoaded = GetAction.execute(TABLE, empty.getValueInteger("id"));
      for(String name : List.of("categoryId", "quantity", "amount", "enabled", "day", "atTime", "atInstant", "note", "html", "payload"))
      {
         assertNull(emptyLoaded.getValue(name), name);
      }
      assertEquals(List.of(List.of("1")), sql("SELECT count(*) FROM pg_item WHERE name='Null values' AND category_id IS NULL AND quantity IS NULL "
         + "AND amount IS NULL AND enabled IS NULL AND day IS NULL AND at_time IS NULL AND at_instant IS NULL AND note IS NULL AND html IS NULL AND payload IS NULL"));
   }




   /*******************************************************************************
    ** Counts ignore the query page, while filters and deterministic sort match SQL.
    *******************************************************************************/
   @Test
   void testCountFiltersSortAndPagingAgainstSql() throws Exception
   {
      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("quantity", QCriteriaOperator.GREATER_THAN, 5))
         .withCriteria(new QFilterCriteria("name", QCriteriaOperator.IN, List.of("Alpha", "Beta", "Gamma")))
         .withOrderBy(new QFilterOrderBy("quantity", false)).withOrderBy(new QFilterOrderBy("id"))
         .withSkip(1).withLimit(1);
      assertEquals(sql("SELECT id,name FROM pg_item WHERE quantity>5 AND name IN ('Alpha','Beta','Gamma') ORDER BY quantity DESC,id LIMIT 1 OFFSET 1"),
         queryRows(new QueryInput(TABLE).withFilter(filter), List.of("id", "name")));
      assertEquals(3, CountAction.execute(TABLE, filter));
      assertEquals(List.of(List.of("3")), sql("SELECT count(*) FROM pg_item WHERE quantity>5 AND name IN ('Alpha','Beta','Gamma')"));
      assertTrue(queryRows(new QueryInput(TABLE).withFilter(filter.withLimit(0)), List.of("id")).isEmpty());
      assertTrue(queryRows(new QueryInput(TABLE).withFilter(filter.withLimit(2).withSkip(9)), List.of("id")).isEmpty());
      QQueryFilter nulls = new QQueryFilter(new QFilterCriteria("quantity", QCriteriaOperator.IS_BLANK));
      assertEquals(sql("SELECT id FROM pg_item WHERE quantity IS NULL"), queryRows(new QueryInput(TABLE).withFilter(nulls), List.of("id")));
      String literal = "x' OR 1=1 --";
      insert(record(literal));
      assertEquals(List.of(List.of(literal)), queryRows(new QueryInput(TABLE)
         .withFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, literal))), List.of("name")));
      assertEquals(5, CountAction.execute(TABLE, null));
      assertEquals(List.of(List.of("5")), sql("SELECT count(*) FROM pg_item"));
   }



   /*******************************************************************************
    ** Native grouping and all aggregate operators include SQL null behavior.
    *******************************************************************************/
   @Test
   void testScalarAndGroupedAggregatesAgainstSql() throws Exception
   {
      List<Aggregate> aggregates = List.of(new Aggregate("id", AggregateOperator.COUNT), new Aggregate("quantity", AggregateOperator.SUM),
         new Aggregate("quantity", AggregateOperator.AVG), new Aggregate("quantity", AggregateOperator.MIN),
         new Aggregate("quantity", AggregateOperator.MAX), new Aggregate("categoryId", AggregateOperator.COUNT_DISTINCT));
      assertAggregates(new AggregateInput(TABLE).withAggregates(aggregates),
         "SELECT count(id),sum(quantity),avg(quantity),min(quantity),max(quantity),count(DISTINCT category_id) FROM pg_item");
      GroupBy group = new GroupBy(QFieldType.INTEGER, "categoryId");
      assertAggregates(new AggregateInput(TABLE).withAggregates(aggregates).withGroupBy(group)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(group))),
         "SELECT category_id,count(id),sum(quantity),avg(quantity),min(quantity),max(quantity),count(DISTINCT category_id) FROM pg_item GROUP BY category_id ORDER BY category_id");
      assertAggregates(new AggregateInput(TABLE).withAggregates(aggregates)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, -1))),
         "SELECT count(id),sum(quantity),avg(quantity),min(quantity),max(quantity),count(DISTINCT category_id) FROM pg_item WHERE id=-1");
   }



   /*******************************************************************************
    ** INNER and LEFT joins preserve native multiplicity and unmatched rows.
    *******************************************************************************/
   @Test
   void testInnerAndLeftJoinsAgainstSql() throws Exception
   {
      for(QueryJoin.Type type : List.of(QueryJoin.Type.INNER, QueryJoin.Type.LEFT))
      {
         QueryInput input = new QueryInput(TABLE)
            .withQueryJoin(new QueryJoin(instance.getJoin(JOIN)).withAlias("category").withType(type).withSelect(true))
            .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
         assertEquals(sql("SELECT i.id,i.name,c.label FROM pg_item i " + type.name() + " JOIN pg_category c ON i.category_id=c.id ORDER BY i.id"),
            queryRows(input, List.of("id", "name", "category.label")));
      }
   }



   /*******************************************************************************
    ** Native constraint failures and invalid framework inputs leave rows unchanged.
    *******************************************************************************/
   @Test
   void testDuplicateMissingTableAndMalformedInputs() throws Exception
   {
      List<List<String>> before = sql("SELECT * FROM pg_item ORDER BY id");
      assertSqlState("23505", assertThrows(QException.class, () -> insert(record("Alpha"))));
      assertSqlState("23503", assertThrows(QException.class, () -> insert(record("Bad category").withValue("categoryId", 999))));
      QException invalid = assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TABLE)
         .withRecord(record("Bad type").withValue("quantity", "not-a-number"))));
      assertTrue(invalid.getMessage().contains("could not be converted to a Long"), invalid.getMessage());
      assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput(TABLE)
         .withFilter(new QQueryFilter(new QFilterCriteria("unknownField", QCriteriaOperator.EQUALS, 1)))));
      assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput(TABLE)
         .withFilter(new QQueryFilter(new QFilterCriteria("quantity", QCriteriaOperator.EQUALS, "not-a-number")))));
      assertThrows(QException.class, () -> new AggregateAction().execute(new AggregateInput(TABLE)
         .withAggregate(new Aggregate("name", AggregateOperator.SUM))));
      assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput("unregisteredPostgresTable")));
      PostgreSQLTableBackendDetails details = (PostgreSQLTableBackendDetails) instance.getTable(TABLE).getBackendDetails();
      details.setTableName("absent_pg_table");
      try
      {
         assertSqlState("42P01", assertThrows(QException.class, () -> CountAction.execute(TABLE, null)));
      }
      finally
      {
         details.setTableName("pg_item");
      }
      assertEquals(before, sql("SELECT * FROM pg_item ORDER BY id"));
      assertEquals(4, CountAction.execute(TABLE, null));
   }



   /*******************************************************************************
    ** PostgreSQL is a relational data backend, not a file-storage implementation.
    *******************************************************************************/
   @Test
   void testUnsupportedStorageFailsExplicitly() throws Exception
   {
      IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new PostgreSQLBackendModule().getStorageInterface());
      assertEquals("StorageInterface is not implemented in this module: PostgreSQLBackendModule", exception.getMessage());
      assertEquals(List.of(List.of("4")), sql("SELECT count(*) FROM pg_item"));
   }



   /*******************************************************************************
    ** Allowed controls distinguish record-lock denials from a broken write path.
    *******************************************************************************/
   @Test
   void testDeniedRowsAcrossReadAggregateAndWrites() throws Exception
   {
      instance.getTable(TABLE).withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("pgTenant")
         .withFieldName("tenant").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE)
         .withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      QContext.setQSession(new QSession().withSecurityKeyValue("pgTenant", 1));
      assertEquals(sql("SELECT id FROM pg_item WHERE tenant=1 ORDER BY id"), queryRows(new QueryInput(TABLE)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id"))), List.of("id")));
      assertEquals(3, CountAction.execute(TABLE, null));
      assertNull(GetAction.execute(TABLE, 3));
      assertEquals("Alpha", GetAction.execute(TABLE, 1).getValueString("name"));
      assertAggregates(new AggregateInput(TABLE).withAggregate(new Aggregate("id", AggregateOperator.COUNT)),
         "SELECT count(id) FROM pg_item WHERE tenant=1");
      List<List<String>> before = sql("SELECT * FROM pg_item ORDER BY id");
      QRecord deniedInsert = new InsertAction().execute(new InsertInput(TABLE).withRecord(record("Denied").withValue("tenant", 2))).getRecords().get(0);
      assertFalse(deniedInsert.getErrors().isEmpty());
      QRecord deniedUpdate = new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", 3).withValue("name", "Taken"))).getRecords().get(0);
      assertFalse(deniedUpdate.getErrors().isEmpty());
      assertEquals(0, new DeleteAction().execute(new DeleteInput(TABLE).withPrimaryKey(3)).getDeletedRecordCount());
      assertEquals(before, sql("SELECT * FROM pg_item ORDER BY id"));
      QRecord allowed = insert(record("Allowed"));
      Integer id = allowed.getValueInteger("id");
      assertTrue(new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", id).withValue("name", "Allowed edit")))
         .getRecords().get(0).getErrors().isEmpty());
      assertEquals(List.of(List.of("Allowed edit")), sql("SELECT name FROM pg_item WHERE id=" + id));
      assertEquals(1, new DeleteAction().execute(new DeleteInput(TABLE).withPrimaryKey(id)).getDeletedRecordCount());
      assertEquals(before, sql("SELECT * FROM pg_item ORDER BY id"));
      QContext.setQSession(new QSession());
      assertEquals(0, CountAction.execute(TABLE, null));
      assertTrue(new QueryAction().execute(new QueryInput(TABLE)).getRecords().isEmpty());
   }



   /*******************************************************************************
    ** A real server rejects an absent database; restoring config recovers safely.
    *******************************************************************************/
   @Test
   void testConnectionFailureAndRecovery() throws Exception
   {
      backend.setDatabaseName("absent_acceptance_database");
      try
      {
         assertSqlState("3D000", assertThrows(QException.class, () -> CountAction.execute(TABLE, null)));
      }
      finally
      {
         backend.setDatabaseName(database.getDatabaseName());
         ConnectionManager.resetConnectionProviders();
      }
      assertEquals(4, CountAction.execute(TABLE, null));
      assertEquals(List.of(List.of("4")), sql("SELECT count(*) FROM pg_item"));
   }



   /*******************************************************************************
    ** Multirow insert, update and delete results are checked against native storage.
    *******************************************************************************/
   @Test
   void testBatchWritesAndGeneratedIdentityCorrelation() throws Exception
   {
      List<QRecord> inserted = new InsertAction().execute(new InsertInput(TABLE)
         .withRecords(List.of(record("Batch A"), record("Batch B"), record("Batch C")))).getRecords();
      assertEquals(3, inserted.size());
      assertEquals(3, inserted.stream().map(row -> row.getValueInteger("id")).distinct().count());
      for(QRecord row : inserted)
      {
         assertTrue(row.getErrors().isEmpty());
         assertEquals(List.of(List.of(row.getValueString("name"))), sql("SELECT name FROM pg_item WHERE id=" + row.getValueInteger("id")));
      }
      List<QRecord> patches = inserted.stream().map(row -> new QRecord().withValue("id", row.getValueInteger("id")).withValue("quantity", 42L)).toList();
      assertTrue(new UpdateAction().execute(new UpdateInput(TABLE).withRecords(patches)).getRecords().stream().allMatch(row -> row.getErrors().isEmpty()));
      assertEquals(List.of(List.of("3")), sql("SELECT count(*) FROM pg_item WHERE name LIKE 'Batch %' AND quantity=42"));
      DeleteInput delete = new DeleteInput(TABLE);
      delete.setPrimaryKeys(inserted.stream().map(row -> row.getValue("id")).toList());
      assertEquals(3, new DeleteAction().execute(delete).getDeletedRecordCount());
      assertTrue(sql("SELECT id FROM pg_item WHERE name LIKE 'Batch %'").isEmpty());
      assertEquals(4, CountAction.execute(TABLE, null));
   }



   /*******************************************************************************
    ** Actions share the caller's transaction; separate JDBC sessions cannot see it.
    *******************************************************************************/
   @Test
   void testTransactionCommitVisibilityAndConnectionClose() throws Exception
   {
      Connection transactionConnection;
      try(RDBMSTransaction transaction = (RDBMSTransaction) QBackendTransaction.openFor(new InsertInput(TABLE)))
      {
         transactionConnection = transaction.getConnection();
         assertFalse(transactionConnection.getAutoCommit());
         QRecord inserted = new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction).withRecord(record("Committed"))).getRecords().get(0);
         assertTrue(inserted.getErrors().isEmpty());
         assertEquals(List.of(List.of("Committed")), nativeRows(transactionConnection, "SELECT name FROM pg_item WHERE name='Committed'"));
         assertEquals(5, new QueryAction().execute(new QueryInput(TABLE).withTransaction(transaction)).getRecords().size());
         assertTrue(sql("SELECT id FROM pg_item WHERE name='Committed'").isEmpty());
         transaction.commit();
         assertEquals(List.of(List.of(inserted.getValueString("id"))), sql("SELECT id FROM pg_item WHERE name='Committed'"));
         assertFalse(transactionConnection.isClosed());
      }
      assertTrue(transactionConnection.isClosed());
      assertEquals(5, CountAction.execute(TABLE, null));
   }



   /*******************************************************************************
    ** Insert, update and delete are visible inside the transaction then all undo.
    *******************************************************************************/
   @Test
   void testExplicitRollbackRestoresAllWrites() throws Exception
   {
      List<List<String>> before = sql("SELECT id,name,quantity FROM pg_item ORDER BY id");
      try(RDBMSTransaction transaction = (RDBMSTransaction) QBackendTransaction.openFor(new InsertInput(TABLE)))
      {
         assertTrue(new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction).withRecord(record("Rolled back")))
            .getRecords().get(0).getErrors().isEmpty());
         assertTrue(new UpdateAction().execute(new UpdateInput(TABLE).withTransaction(transaction)
            .withRecord(new QRecord().withValue("id", 1).withValue("quantity", 777L))).getRecords().get(0).getErrors().isEmpty());
         assertEquals(1, new DeleteAction().execute(new DeleteInput(TABLE).withTransaction(transaction).withPrimaryKey(2)).getDeletedRecordCount());
         assertEquals(List.of(List.of("777")), nativeRows(transaction.getConnection(), "SELECT quantity FROM pg_item WHERE id=1"));
         assertTrue(nativeRows(transaction.getConnection(), "SELECT id FROM pg_item WHERE id=2").isEmpty());
         assertEquals(List.of(List.of("1")), nativeRows(transaction.getConnection(), "SELECT count(*) FROM pg_item WHERE name='Rolled back'"));
         assertEquals(before, sql("SELECT id,name,quantity FROM pg_item ORDER BY id"));
         transaction.rollback();
         assertEquals(before, nativeRows(transaction.getConnection(), "SELECT id,name,quantity FROM pg_item ORDER BY id"));
      }
      assertEquals(before, sql("SELECT id,name,quantity FROM pg_item ORDER BY id"));
   }



   /*******************************************************************************
    ** Generated/supplied/generated keys split the batch into separate statements.
    ** Autocommit keeps the first statement when the second hits a duplicate key.
    *******************************************************************************/
   @Test
   void testAutocommitMidBatchFailurePreservesEarlierStatement() throws Exception
   {
      assertSqlState("23505", assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TABLE).withRecords(failingBatch()))));
      assertEquals(List.of(List.of("Early batch")), sql("SELECT name FROM pg_item WHERE name LIKE '% batch' ORDER BY name"));
      assertEquals(5, CountAction.execute(TABLE, null));
      assertEquals("Alpha", GetAction.execute(TABLE, 1).getValueString("name"));
   }



   /*******************************************************************************
    ** The identical failure aborts PostgreSQL's transaction until explicit rollback.
    *******************************************************************************/
   @Test
   void testMidBatchFailureRollbackAndRecovery() throws Exception
   {
      List<List<String>> before = sql("SELECT id,name FROM pg_item ORDER BY id");
      try(RDBMSTransaction transaction = (RDBMSTransaction) QBackendTransaction.openFor(new InsertInput(TABLE)))
      {
         assertTrue(new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction).withRecord(record("Before batch")))
            .getRecords().get(0).getErrors().isEmpty());
         assertEquals(List.of(List.of("Before batch")), nativeRows(transaction.getConnection(), "SELECT name FROM pg_item WHERE name='Before batch'"));
         assertSqlState("23505", assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TABLE)
            .withTransaction(transaction).withRecords(failingBatch()))));
         assertSqlState("25P02", assertThrows(SQLException.class, () -> nativeRows(transaction.getConnection(), "SELECT count(*) FROM pg_item")));
         assertEquals(before, sql("SELECT id,name FROM pg_item ORDER BY id"));
         transaction.rollback();
         assertEquals(before, nativeRows(transaction.getConnection(), "SELECT id,name FROM pg_item ORDER BY id"));
         assertTrue(new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction).withRecord(record("Recovered")))
            .getRecords().get(0).getErrors().isEmpty());
         transaction.commit();
      }
      assertEquals(List.of(List.of("Recovered")), sql("SELECT name FROM pg_item WHERE id>=100 ORDER BY id"));
      assertEquals(5, CountAction.execute(TABLE, null));
   }



   /*******************************************************************************
    ** A size-one native pool proves physical reuse, checkout timeout and shutdown.
    *******************************************************************************/
   @Test
   void testPoolReuseCheckoutTimeoutAndOwnedCleanup() throws Exception
   {
      backend.setConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class));
      backend.setConnectionPoolSettings(new ConnectionPoolSettings().withInitialPoolSize(1).withMinPoolSize(1)
         .withMaxPoolSize(1).withAcquireIncrement(1).withCheckoutTimeoutSeconds(1));
      int firstPid;
      Connection first = ConnectionManager.getConnection(backend);
      try(first)
      {
         firstPid = Integer.parseInt(nativeRows(first, "SELECT pg_backend_pid()").get(0).get(0));
      }
      assertTrue(first.isClosed());
      try(Connection second = ConnectionManager.getConnection(backend))
      {
         assertEquals(firstPid, Integer.parseInt(nativeRows(second, "SELECT pg_backend_pid()").get(0).get(0)));
         QException timeout = assertTimeout(Duration.ofSeconds(5), () -> assertThrows(QException.class, () -> CountAction.execute(TABLE, null)));
         SQLException cause = assertInstanceOf(SQLException.class, timeout.getCause());
         assertTrue(cause.getMessage().contains("checkout a Connection has timed out"), cause.getMessage());
         assertEquals(List.of(List.of("4")), sql("SELECT count(*) FROM pg_item"));
      }
      assertEquals(4, CountAction.execute(TABLE, null));
      try(RDBMSTransaction transaction = (RDBMSTransaction) QBackendTransaction.openFor(new InsertInput(TABLE)))
      {
         assertTrue(new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction).withRecord(record("Uncommitted pooled row")))
            .getRecords().get(0).getErrors().isEmpty());
         assertEquals(List.of(List.of("Uncommitted pooled row")), nativeRows(transaction.getConnection(), "SELECT name FROM pg_item WHERE name='Uncommitted pooled row'"));
         assertTrue(sql("SELECT id FROM pg_item WHERE name='Uncommitted pooled row'").isEmpty());
      }
      try(Connection reused = ConnectionManager.getConnection(backend))
      {
         assertTrue(reused.getAutoCommit(), "Returning an unfinished transaction must reset the next borrower's connection");
         assertEquals(firstPid, Integer.parseInt(nativeRows(reused, "SELECT pg_backend_pid()").get(0).get(0)));
         assertTrue(nativeRows(reused, "SELECT id FROM pg_item WHERE name='Uncommitted pooled row'").isEmpty());
      }
      assertTrue(sql("SELECT id FROM pg_item WHERE name='Uncommitted pooled row'").isEmpty());
      assertEquals(List.of(List.of("1")), sql("SELECT count(*) FROM pg_stat_activity WHERE application_name='" + backend.getName() + "'"));
      closeOwnedPools();
      awaitNoOwnedConnections();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> failingBatch()
   {
      return List.of(record("Early batch"), record("Collision").withValue("id", 1), record("Late batch"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> queryRows(QueryInput input, List<String> fields) throws QException
   {
      return new QueryAction().execute(input).getRecords().stream()
         .map(record -> fields.stream().map(record::getValueString).toList()).toList();
   }



   /*******************************************************************************
    ** Numeric scale is normalized; SQL nulls and group identities stay distinct.
    *******************************************************************************/
   private void assertAggregates(AggregateInput input, String query) throws Exception
   {
      List<List<String>> expected = sql(query).stream().map(row -> row.stream().map(this::number).toList()).toList();
      List<List<String>> actual = new ArrayList<>();
      for(var result : new AggregateAction().execute(input).getResults())
      {
         List<String> row = new ArrayList<>();
         input.getGroupBys().forEach(group -> row.add(number(result.getGroupByValue(group))));
         input.getAggregates().forEach(aggregate -> row.add(number(result.getAggregateValue(aggregate))));
         actual.add(row);
      }
      assertEquals(expected, actual);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String number(Object value)
   {
      return value == null ? null : new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
   }



   /*******************************************************************************
    ** Require the real PostgreSQL diagnostic, not just any framework exception.
    *******************************************************************************/
   private void assertSqlState(String expected, Throwable exception)
   {
      for(Throwable cause = exception; cause != null; cause = cause.getCause())
      {
         if(cause instanceof SQLException sqlException && expected.equals(sqlException.getSQLState()))
         {
            return;
         }
      }
      fail("Expected SQLSTATE " + expected, exception);
   }



   /*******************************************************************************
    ** The caller owns this connection, so transaction-local visibility is preserved.
    *******************************************************************************/
   private List<List<String>> nativeRows(Connection connection, String sql) throws SQLException
   {
      try(Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         List<List<String>> rows = new ArrayList<>();
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.add(result.getString(column));
            }
            rows.add(row);
         }
         return rows;
      }
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   private QFieldMetaData field(String name, String column, QFieldType type)
   {
      return new QFieldMetaData(name, type).withBackendName(column);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord record(String name)
   {
      return new QRecord().withValue("tenant", 1).withValue("name", name);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insert(QRecord record) throws QException
   {
      QRecord output = new InsertAction().execute(new InsertInput(TABLE).withRecord(record)).getRecords().get(0);
      assertTrue(output.getErrors().isEmpty(), String.valueOf(output.getErrors()));
      return output;
   }



   /*******************************************************************************
    ** The oracle bypasses QQQ's connection provider and transaction registry.
    *******************************************************************************/
   private Connection nativeConnection() throws SQLException
   {
      Connection connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
      try(Statement statement = connection.createStatement())
      {
         statement.execute("SET TIME ZONE 'UTC'");
         statement.execute("SET statement_timeout = '5s'");
      }
      catch(SQLException exception)
      {
         connection.close();
         throw exception;
      }
      return connection;
   }



   /*******************************************************************************
    ** Only fixed fixture SQL and framework-returned numeric IDs reach this helper.
    *******************************************************************************/
   private List<List<String>> sql(String sql) throws Exception
   {
      try(Connection connection = nativeConnection())
      {
         return nativeRows(connection, sql);
      }
   }



   /*******************************************************************************
    ** C3P0's public registry identifies the one uniquely named pool owned here.
    *******************************************************************************/
   private void closeOwnedPools() throws Exception
   {
      if(backend != null)
      {
         for(Object entry : Set.copyOf(C3P0Registry.getPooledDataSources()))
         {
            PooledDataSource pool = (PooledDataSource) entry;
            if(backend.getName().equals(pool.getIdentityToken()))
            {
               DataSources.destroy(pool);
            }
         }
      }
   }



   /*******************************************************************************
    ** Server-side state must show cleanup, not merely a closed Java handle.
    *******************************************************************************/
   private void awaitNoOwnedConnections() throws Exception
   {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while(System.nanoTime() < deadline)
      {
         if(sql("SELECT pid FROM pg_stat_activity WHERE application_name='" + backend.getName() + "'").isEmpty())
         {
            return;
         }
         Thread.sleep(25);
      }
      fail("Owned PostgreSQL connections remain after cleanup");
   }
}
