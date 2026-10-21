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


import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSUpdateAction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Runtime custom field metadata and storage.
 *******************************************************************************/
public class JsonPathPostgreSQLTest extends BaseTest
{
   /*******************************************************************************
    ** Production actions against PostgreSQL 17, using the module Testcontainers fixture.
    *******************************************************************************/
   @Test
   void jsonCrudAndConcurrentMerges() throws Exception
   {
      runTestSql("CREATE TABLE udf_test (id integer PRIMARY KEY, qqq_custom_fields jsonb NOT NULL DEFAULT '{}')", null);
      runTestSql("CREATE INDEX udf_test_custom_gin ON udf_test USING gin (qqq_custom_fields jsonb_path_ops)", null);
      QContext.getQInstance().addTable(JsonPathSQLTest.table(TestUtils.DEFAULT_BACKEND_NAME));
      new QInstanceValidator().revalidate(QContext.getQInstance());
      new InsertAction().execute(new InsertInput().withTableName("udf_test").withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("score", 7).withValue("note", "first"),
         new QRecord().withValue("id", 2).withValue("score", 3))));
      assertEquals(1, QueryAction.execute("udf_test", new QQueryFilter(new QFilterCriteria("score", QCriteriaOperator.EQUALS, 7))).size());
      assertEquals(2, QueryAction.execute("udf_test", new QQueryFilter(new QFilterCriteria("score", QCriteriaOperator.IN, 3, 7))).size());
      assertEquals(1, QueryAction.execute("udf_test", new QQueryFilter(new QFilterCriteria("score", QCriteriaOperator.GREATER_THAN, 5))).size());
      var ordered = QueryAction.execute("udf_test", new QQueryFilter().withOrderBy(new QFilterOrderBy("score", false)));
      assertEquals(7, ordered.get(0).getValue("score"));
      assertEquals(1, ordered.get(0).getValue("id"));
      var context = QContext.capture();
      CountDownLatch ready = new CountDownLatch(2);
      CountDownLatch start = new CountDownLatch(1);
      try(var executor = Executors.newFixedThreadPool(2))
      {
         var first = executor.submit(() ->
         {
            QContext.init(context);
            try
            {
               ready.countDown();
               assertTrue(start.await(10, TimeUnit.SECONDS));
               new RDBMSUpdateAction().execute(new UpdateInput().withTableName("udf_test")
                  .withRecords(List.of(new QRecord().withValue("id", 1).withValue("score", 9))));
               return (true);
            }
            finally
            {
               QContext.clear();
            }
         });
         var second = executor.submit(() ->
         {
            QContext.init(context);
            try
            {
               ready.countDown();
               assertTrue(start.await(10, TimeUnit.SECONDS));
               new RDBMSUpdateAction().execute(new UpdateInput().withTableName("udf_test")
                  .withRecords(List.of(new QRecord().withValue("id", 1).withValue("note", "concurrent"))));
               return (true);
            }
            finally
            {
               QContext.clear();
            }
         });
         assertTrue(ready.await(10, TimeUnit.SECONDS));
         start.countDown();
         assertTrue(first.get(20, TimeUnit.SECONDS));
         assertTrue(second.get(20, TimeUnit.SECONDS));
      }
      QRecord merged = QueryAction.execute("udf_test", new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))).get(0);
      assertEquals(9, merged.getValue("score"));
      assertEquals("concurrent", merged.getValue("note"));
      new UpdateAction().execute(new UpdateInput().withTableName("udf_test")
         .withRecords(List.of(new QRecord().withValue("id", 1).withValue("note", null))));
      assertNull(QueryAction.execute("udf_test", new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))).get(0).getValue("note"));
      runTestSql("SELECT qqq_custom_fields::text FROM udf_test WHERE id = 1", rs ->
      {
         assertTrue(rs.next());
         assertEquals("{\"score\": 9}", rs.getString(1));
      });
   }
}

