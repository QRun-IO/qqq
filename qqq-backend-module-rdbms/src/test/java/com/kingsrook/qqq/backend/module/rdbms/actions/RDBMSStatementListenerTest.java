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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecution;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecutionListener;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecutionListeners;
import com.kingsrook.qqq.backend.core.instrumentation.StatementOperation;
import com.kingsrook.qqq.backend.core.instrumentation.UnitOfWorkKind;
import com.kingsrook.qqq.backend.core.instrumentation.UnitsOfWork;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** The RDBMS actions report every statement to core's statement execution
 ** listeners, attributed to the open unit of work.
 *******************************************************************************/
class RDBMSStatementListenerTest extends RDBMSActionTest
{
   private final List<StatementExecution> events = Collections.synchronizedList(new ArrayList<>());

   private final StatementExecutionListener listener = events::add;



   /*******************************************************************************
    ** Prime the database and listen.
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      super.primeTestDatabase();
      StatementExecutionListeners.register(listener);
   }



   /*******************************************************************************
    ** Stop listening.
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      StatementExecutionListeners.clear();
      UnitsOfWork.reset();
   }



   /*******************************************************************************
    ** Each action kind is reported with its table, parameterized SQL, row count
    ** and the open unit of work.
    *******************************************************************************/
   @Test
   void testEveryActionKindIsReported() throws Exception
   {
      String table = TestUtils.TABLE_NAME_PERSON;
      try(UnitsOfWork.Scope scope = UnitsOfWork.begin(UnitOfWorkKind.SCHEDULED_JOB, "nightly"))
      {
         new QueryAction().execute(new QueryInput(table));
         new CountAction().execute(new CountInput(table));
         new AggregateAction().execute(new AggregateInput(table).withAggregate(new Aggregate("id", AggregateOperator.COUNT)));
         Integer id = new InsertAction().execute(new InsertInput(table).withRecord(new QRecord().withValue("firstName", "Ada").withValue("lastName", "L")
            .withValue("email", "ada@example.com"))).getRecords().get(0).getValueInteger("id");
         new UpdateAction().execute(new UpdateInput(table).withRecord(new QRecord().withValue("id", id).withValue("firstName", "Grace")));
         new DeleteAction().execute(new DeleteInput(table).withPrimaryKey(id));
      }

      List<StatementOperation> operations = events.stream().filter(e -> table.equals(e.tableName())).map(StatementExecution::operation).toList();
      assertThat(operations).contains(StatementOperation.QUERY, StatementOperation.COUNT, StatementOperation.AGGREGATE, StatementOperation.INSERT,
         StatementOperation.UPDATE, StatementOperation.DELETE);

      StatementExecution query = events.stream().filter(e -> e.operation() == StatementOperation.QUERY && table.equals(e.tableName())).findFirst().orElseThrow();
      assertThat(query.rowCount()).isEqualTo(5);
      assertThat(query.succeeded()).isTrue();
      assertThat(query.backendName()).isEqualTo(TestUtils.DEFAULT_BACKEND_NAME);
      assertThat(query.sql()).containsIgnoringCase("SELECT");
      assertThat(query.unitOfWork().getName()).isEqualTo("nightly");
      assertThat(query.unitOfWork().getKind()).isEqualTo(UnitOfWorkKind.SCHEDULED_JOB);

      StatementExecution insert = events.stream().filter(e -> e.operation() == StatementOperation.INSERT).findFirst().orElseThrow();
      assertThat(insert.rowCount()).isEqualTo(1);
      assertThat(insert.sql()).doesNotContain("ada@example.com");
   }



   /*******************************************************************************
    ** A failing statement is reported as failed.
    *******************************************************************************/
   @Test
   void testFailureIsReported() throws Exception
   {
      try(Connection connection = new ConnectionManager().getConnection(TestUtils.defineBackend()))
      {
         QueryManager.executeUpdate(connection, "DROP TABLE person");
      }
      try
      {
         new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON));
      }
      catch(Exception e)
      {
         ///////////////////////
         // expected to fail  //
         ///////////////////////
      }
      assertThat(events).anyMatch(e -> e.operation() == StatementOperation.QUERY && !e.succeeded());
   }
}
