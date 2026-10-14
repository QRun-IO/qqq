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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.tables.ConditionalUpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Regression coverage for RDBMSConditionalUpdateAction.
 *******************************************************************************/
class RDBMSConditionalUpdateActionTest extends RDBMSActionTest
{

   /*******************************************************************************
    ** SQL conditional updates return the changed count and preserve nonmatching rows.
    *******************************************************************************/
   @Test
   void updatesOnlyMatchingRowsAndReturnsCount() throws Exception
   {
      int count = new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, List.of(1, 2))))
         .withValue("lastName", "changed"));
      assertThat(count).isEqualTo(2);
      assertThat(GetAction.execute(TABLE, 1).getValueString("lastName")).isEqualTo("changed");
      assertThat(GetAction.execute(TABLE, 2).getValueString("lastName")).isEqualTo("changed");
      assertThat(GetAction.execute(TABLE, 3).getValueString("lastName")).isEqualTo("Chamberlain");
      assertThat(new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE).withFilter(idFilter(-1)).withValue("lastName", "absent"))).isZero();
   }



   /*******************************************************************************
    ** Two SQL contenders for the same expected integer value have exactly one winner.
    *******************************************************************************/
   @Test
   void twoConcurrentCompareAndSetsHaveOneWinner() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      CountDownLatch ready = new CountDownLatch(2);
      CountDownLatch start = new CountDownLatch(1);
      var executor = Executors.newFixedThreadPool(2);
      try
      {
         List<Future<Integer>> results = new ArrayList<>();
         for(int i = 0; i < 2; i++)
         {
            results.add(executor.submit(() ->
            {
               QContext.init(instance, new QSession());
               try
               {
                  ready.countDown();
                  assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                  return (new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE)
                     .withFilter(idFilter(1).withCriteria(new QFilterCriteria("daysWorked", QCriteriaOperator.EQUALS, 27)))
                     .withValue("daysWorked", 28)));
               }
               finally
               {
                  QContext.clear();
               }
            }));
         }
         assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
         start.countDown();
         List<Integer> counts = new ArrayList<>();
         for(Future<Integer> result : results)
         {
            counts.add(result.get(15, TimeUnit.SECONDS));
         }
         assertThat(counts).containsExactlyInAnyOrder(1, 0);
         assertThat(GetAction.execute(TABLE, 1).getValueInteger("daysWorked")).isEqualTo(28);
      }
      finally
      {
         start.countDown();
         executor.shutdownNow();
         assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
      }
   }



   /*******************************************************************************
    ** SQL bindings preserve Instant, Integer, and null assignments.
    *******************************************************************************/
   @Test
   void bindsInstantIntegerAndNullValues() throws Exception
   {
      Instant timestamp = Instant.parse("2026-01-02T03:04:05Z");
      assertThat(new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE).withFilter(idFilter(1))
         .withValue("modifyDate", timestamp).withValue("daysWorked", 42).withValue("homeTown", null))).isEqualTo(1);
      var record = GetAction.execute(TABLE, 1);
      assertThat(record.getValue("modifyDate")).isInstanceOf(Instant.class).isEqualTo(timestamp);
      assertThat(record.getValue("daysWorked")).isInstanceOf(Integer.class).isEqualTo(42);
      assertThat(record.getValue("homeTown")).isNull();
   }



   /*******************************************************************************
    ** Conditional updates share the caller's transaction and are undone by rollback.
    *******************************************************************************/
   @Test
   void joinsAndRollsBackCallerTransaction() throws Exception
   {
      String original = GetAction.execute(TABLE, 1).getValueString("lastName");
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
         RDBMSTransaction transaction = new RDBMSTransaction(connection))
      {
         assertThat(new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE).withFilter(idFilter(1))
            .withValue("lastName", "uncommitted").withTransaction(transaction))).isEqualTo(1);
         assertThat(new GetAction().execute(new GetInput(TABLE).withPrimaryKey(1).withTransaction(transaction))
            .getRecord().getValueString("lastName")).isEqualTo("uncommitted");
         assertThat(GetAction.execute(TABLE, 1).getValueString("lastName")).isEqualTo(original);
         transaction.rollback();
      }
      assertThat(GetAction.execute(TABLE, 1).getValueString("lastName")).isEqualTo(original);
   }



   /*******************************************************************************
    ** The H2 clock is within five minutes of the JVM clock.
    *******************************************************************************/
   @Test
   void databaseClockIsWithinFiveMinutes() throws Exception
   {
      Instant databaseNow = new ConditionalUpdateAction().databaseNow(TABLE);
      assertThat(Duration.between(databaseNow, Instant.now()).abs()).isLessThan(Duration.ofMinutes(5));
   }



   private static final String TABLE = TestUtils.TABLE_NAME_PERSON;



   /*******************************************************************************
    ** Recreate the module's existing H2 schema and seed data before each test.
    *******************************************************************************/
   @BeforeEach
   void prime() throws Exception
   {
      primeTestDatabase();
   }



   /*******************************************************************************
    ** Restrict an update to the selected person.
    *******************************************************************************/
   private static QQueryFilter idFilter(Integer id)
   {
      return (new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, id)));
   }
}

