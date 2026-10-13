/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.postgres.actions;


import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.JsonPathStorage;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.postgres.strategy.PostgreSQLRDBMSActionStrategy;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSAggregateAction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSInsertAction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSQueryAction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSUpdateAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Runtime custom field metadata and storage.
 *******************************************************************************/
public class JsonPathSQLTest
{
   private RDBMSTransaction transaction;
   private RecordingStrategy strategy;



   /*******************************************************************************
    ** Real SQL generation with a recording execution strategy; no Docker required.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      QInstance instance = new QInstance();
      PostgreSQLBackendMetaData backend = new PostgreSQLBackendMetaData();
      backend.withName("postgres").withHostName("localhost").withDatabaseName("test").withUsername("test").withPassword("test")
         .withActionStrategyCodeReference(new QCodeReference(RecordingStrategy.class));
      instance.addBackend(backend);
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), TestUtils.defineAuthentication());
      instance.addTable(table("postgres"));
      QContext.init(instance, new QSession());
      PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { PreparedStatement.class },
         (proxy, method, args) -> null);
      Connection connection = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Connection.class },
         (proxy, method, args) -> method.getName().equals("prepareStatement") ? statement : null);
      transaction = new RDBMSTransaction(connection);
      strategy = (RecordingStrategy) backend.getActionStrategy();
   }



   /*******************************************************************************
    ** Clear thread context.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Common metadata used by SQL and live PostgreSQL tests.
    *******************************************************************************/
   public static QTableMetaData table(String backend)
   {
      return (new QTableMetaData().withName("udf_test").withBackendName(backend).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(field("score", QFieldType.INTEGER)).withField(field("note", QFieldType.STRING)));
   }



   /*******************************************************************************
    ** A JSON-backed field with an ignored physical backendName.
    *******************************************************************************/
   public static QFieldMetaData field(String name, QFieldType type)
   {
      return (new QFieldMetaData(name, type).withBackendName("ignored")
         .withBackendStorage(new JsonPathStorage().withColumnName("qqq_custom_fields").withKey(name)));
   }



   /*******************************************************************************
    ** All scalar casts; select, containment, range and ordering share the expressions.
    *******************************************************************************/
   @Test
   void readSqlAndParameters() throws Exception
   {
      Map<QFieldType, String> casts = Map.of(QFieldType.STRING, "", QFieldType.TEXT, "", QFieldType.INTEGER, "::integer",
         QFieldType.LONG, "::bigint", QFieldType.DECIMAL, "::numeric", QFieldType.BOOLEAN, "::boolean", QFieldType.DATE, "::date", QFieldType.DATE_TIME, "::timestamptz");
      casts.forEach((type, cast) -> assertEquals("(j->>'score')" + cast,
         strategy.jsonPathExpression("j", (JsonPathStorage) field("score", type).getBackendStorage(), type)));
      query(new QQueryFilter(new QFilterCriteria("score", QCriteriaOperator.EQUALS, "7")));
      assertTrue(strategy.sql.contains("(\"udf_test\".\"qqq_custom_fields\"->>'score')::integer"), strategy.sql);
      assertTrue(strategy.sql.contains("(\"udf_test\".\"qqq_custom_fields\" @> ?::jsonb)"), strategy.sql);
      assertEquals(List.of("{\"score\":7}"), strategy.params);
      query(new QQueryFilter(new QFilterCriteria("score", QCriteriaOperator.IN, 7, 9)));
      assertTrue(strategy.sql.contains("@> ?::jsonb OR \"udf_test\".\"qqq_custom_fields\" @> ?::jsonb"), strategy.sql);
      assertEquals(List.of("{\"score\":7}", "{\"score\":9}"), strategy.params);
      query(new QQueryFilter(new QFilterCriteria("score", QCriteriaOperator.GREATER_THAN, "3"))
         .withOrderBy(new QFilterOrderBy("score", false)));
      assertTrue(strategy.sql.contains("(\"udf_test\".\"qqq_custom_fields\"->>'score')::integer > ?"), strategy.sql);
      assertTrue(strategy.sql.contains("ORDER BY (\"udf_test\".\"qqq_custom_fields\"->>'score')::integer DESC"), strategy.sql);
      assertEquals(List.of(3), strategy.params);
   }



   /*******************************************************************************
    ** One JSON object per insert column; update parameters contain only changed keys.
    *******************************************************************************/
   @Test
   void writeSqlAndParameters() throws Exception
   {
      new RDBMSInsertAction().execute(new InsertInput().withTableName("udf_test").withTransaction(transaction)
         .withRecords(List.of(new QRecord().withValue("id", 1).withValue("score", 7).withValue("note", null))));
      assertEquals("INSERT INTO \"udf_test\"(\"id\", \"qqq_custom_fields\") VALUES(?,?::jsonb)", strategy.sql);
      assertEquals(List.of(1, "{\"score\":7}"), strategy.params);
      new RDBMSUpdateAction().execute(new UpdateInput().withTableName("udf_test").withTransaction(transaction)
         .withRecords(List.of(new QRecord().withValue("id", 1).withValue("score", 8).withValue("note", null))));
      assertEquals("UPDATE \"udf_test\" SET \"qqq_custom_fields\" = (COALESCE(\"qqq_custom_fields\",'{}'::jsonb) || ?::jsonb) - ?::text[] WHERE \"id\"  IN (?)", strategy.sql);
      assertEquals(List.of("{\"score\":8}", "{note}", 1), strategy.params);
      new RDBMSUpdateAction().execute(new UpdateInput().withTableName("udf_test").withTransaction(transaction)
         .withRecords(List.of(new QRecord().withValue("id", 1).withValue("score", 9), new QRecord().withValue("id", 2).withValue("score", 10))));
      assertEquals(List.of(List.of("{\"score\":9}", "{}", 1), List.of("{\"score\":10}", "{}", 2)), strategy.batches);
   }



   /*******************************************************************************
    ** Aggregates and group-by use the same typed JSON expression.
    *******************************************************************************/
   @Test
   void aggregates() throws Exception
   {
      new RDBMSAggregateAction().execute(new AggregateInput().withTableName("udf_test").withTransaction(transaction)
         .withAggregate(new Aggregate().withFieldName("score").withFieldType(QFieldType.INTEGER).withOperator(AggregateOperator.SUM))
         .withGroupBy(new GroupBy(QFieldType.STRING, "note")));
      assertTrue(strategy.sql.contains("SUM((\"udf_test\".\"qqq_custom_fields\"->>'score')::integer)"), strategy.sql);
      assertTrue(strategy.sql.contains("GROUP BY (\"udf_test\".\"qqq_custom_fields\"->>'note')"), strategy.sql);
   }



   /*******************************************************************************
    ** Execute through the production query builder.
    *******************************************************************************/
   private void query(QQueryFilter filter) throws Exception
   {
      new RDBMSQueryAction().execute(new QueryInput().withTableName("udf_test").withTransaction(transaction).withFilter(filter));
   }



   /*******************************************************************************
    ** Capture execution after production actions have built their SQL and bind values.
    *******************************************************************************/
   public static class RecordingStrategy extends PostgreSQLRDBMSActionStrategy
   {
      private String sql;
      private List<?> params;
      private List<List<Serializable>> batches;



      /***************************************************************************
       ** Capture a query without invoking a result set.
       ***************************************************************************/
      @Override
      public void executeStatement(PreparedStatement statement, CharSequence sql, ResultSetProcessor processor, Object... params)
      {
         this.sql = sql.toString();
         this.params = params.length == 1 && params[0] instanceof List<?> list ? list : Arrays.asList(params);
      }



      /***************************************************************************
       ** Capture inserts with supplied keys.
       ***************************************************************************/
      @Override
      public Integer executeUpdateForRowCount(Connection connection, String sql, Object... params)
      {
         this.sql = sql;
         this.params = Arrays.asList(params);
         return (1);
      }



      /***************************************************************************
       ** Capture a uniform update.
       ***************************************************************************/
      @Override
      public PreparedStatement executeUpdate(Connection connection, String sql, List<Object> params)
      {
         this.sql = sql;
         this.params = params;
         return (null);
      }



      /***************************************************************************
       ** Capture batch update values.
       ***************************************************************************/
      @Override
      public void executeBatchUpdate(Connection connection, String sql, List<List<Serializable>> values)
      {
         this.sql = sql;
         this.batches = values;
      }
   }
}

