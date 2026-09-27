/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QueryStatManager 
 *******************************************************************************/
class QueryStatManagerTest extends BaseTest
{
   /*******************************************************************************
    ** A consumer finishing after restart must not enqueue an old-generation stat.
    *******************************************************************************/
   @Test
   void testRestartDoesNotCollectPreviousGenerationAfterConsumer() throws Exception
   {
      var manager = QueryStatManager.getInstance();
      var caller = QContext.capture();
      var originalConsumers = manager.getQueryStatConsumers();
      var entered = new CountDownLatch(1);
      var release = new CountDownLatch(1);
      caller.qInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withCapability(Capability.QUERY_STATS);
      manager.setQueryStatConsumers(List.of(stat ->
      {
         entered.countDown();
         try
         {
            assertTrue(release.await(5, TimeUnit.SECONDS));
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
         }
      }));
      try(var executor = Executors.newFixedThreadPool(2))
      {
         var read = executor.submit(() ->
         {
            QContext.init(caller);
            try
            {
               return new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY));
            }
            finally
            {
               QContext.clear();
            }
         });
         try
         {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            executor.submit(() -> manager.start(caller.qInstance(), QSystemUserSession::new)).get(5, TimeUnit.SECONDS);
         }
         finally
         {
            release.countDown();
            read.get(5, TimeUnit.SECONDS);
            manager.setQueryStatConsumers(originalConsumers);
         }
      }
      assertEquals(0, manager.getQueryStats().size());
      new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY));
      assertEquals(1, manager.getQueryStats().size());
   }



   /*******************************************************************************
    ** Stop cannot return while an already running flush can still persist a batch.
    *******************************************************************************/
   @Test
   void testStopWaitsForInFlightFlush() throws Exception
   {
      var manager = QueryStatManager.getInstance();
      var caller = QContext.capture();
      var entered = new CountDownLatch(1);
      var release = new CountDownLatch(1);
      var stopping = new CountDownLatch(1);
      manager.stop();
      manager.start(caller.qInstance(), () ->
      {
         entered.countDown();
         try
         {
            if(!release.await(5, TimeUnit.SECONDS))
            {
               throw new IllegalStateException("Flush release deadline exceeded");
            }
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
         }
         return caller.qSession();
      });
      try(var executor = Executors.newFixedThreadPool(2))
      {
         var flush = executor.submit(manager::storeStatsNow);
         try
         {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var stop = executor.submit(() ->
            {
               stopping.countDown();
               manager.stop();
            });
            assertTrue(stopping.await(5, TimeUnit.SECONDS));
            try
            {
               assertThrows(TimeoutException.class, () -> stop.get(100, TimeUnit.MILLISECONDS));
            }
            finally
            {
               release.countDown();
               stop.get(5, TimeUnit.SECONDS);
            }
         }
         finally
         {
            release.countDown();
            flush.get(5, TimeUnit.SECONDS);
         }
      }
      assertEquals(caller, QContext.capture());
   }



   /*******************************************************************************
    ** Flush-generated reads must not reach consumers, even if setup fails; the
    ** caller can still collect its next real query after the failed flush.
    *******************************************************************************/
   @Test
   void testFlushFailureRestoresCollectionSuppression() throws Exception
   {
      var manager = QueryStatManager.getInstance();
      var caller = QContext.capture();
      var originalConsumers = manager.getQueryStatConsumers();
      var observed = new ArrayList<QueryStat>();
      manager.setQueryStatConsumers(List.of(observed::add));
      caller.qInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withCapability(Capability.QUERY_STATS);
      manager.stop();
      manager.start(caller.qInstance(), () ->
      {
         try
         {
            new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY));
         }
         catch(QException e)
         {
            throw new IllegalStateException(e);
         }
         throw new IllegalStateException("owned supplier failure after read");
      });
      try
      {
         manager.storeStatsNow();
         assertEquals(0, observed.size());
         assertEquals(0, manager.getQueryStats().size());
         assertEquals(caller, QContext.capture());
         new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY));
         assertEquals(1, observed.size());
         assertEquals(1, manager.getQueryStats().size());
      }
      finally
      {
         manager.setQueryStatConsumers(originalConsumers);
      }
   }



   /*******************************************************************************
    ** Only the configured instance may reach consumers or the pending batch.
    *******************************************************************************/
   @Test
   void testForeignInstanceRejectedBeforeConsumers() throws Exception
   {
      var manager = QueryStatManager.getInstance();
      var caller = QContext.capture();
      var originalConsumers = manager.getQueryStatConsumers();
      var observed = new ArrayList<QueryStat>();
      manager.setQueryStatConsumers(List.of(observed::add));
      try
      {
         QContext.init(TestUtils.defineInstance(), caller.qSession());
         QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withCapability(Capability.QUERY_STATS);
         new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY));
         assertEquals(0, observed.size());
         assertEquals(0, manager.getQueryStats().size());
         QContext.init(caller);
         caller.qInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withCapability(Capability.QUERY_STATS);
         new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY));
         assertEquals(1, observed.size());
         assertEquals(1, manager.getQueryStats().size());
      }
      finally
      {
         QContext.init(caller);
         manager.setQueryStatConsumers(originalConsumers);
      }
   }



   /*******************************************************************************
    ** Flush must preserve every caller context field even when setup fails or the
    ** job is disabled; a later flush must still collect normally.
    *******************************************************************************/
   @Test
   void testExplicitFlushRestoresContextOnEveryExit()
   {
      var manager = QueryStatManager.getInstance();
      var before = QContext.capture();
      manager.storeStatsNow();
      assertEquals(before, QContext.capture());
      manager.stop();
      manager.start(before.qInstance(), () ->
      {
         QContext.clear();
         throw new IllegalStateException("owned supplier failure");
      });
      manager.storeStatsNow();
      assertEquals(before, QContext.capture());
      String enabled = System.getProperty("qqq.queryStatManager.enabled");
      try
      {
         System.setProperty("qqq.queryStatManager.enabled", "false");
         manager.storeStatsNow();
         assertEquals(before, QContext.capture());
      }
      finally
      {
         if(enabled == null)
         {
            System.clearProperty("qqq.queryStatManager.enabled");
         }
         else
         {
            System.setProperty("qqq.queryStatManager.enabled", enabled);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      QueryStatManager queryStatManager = QueryStatManager.getInstance();
      queryStatManager.start(QContext.getQInstance(), () -> new QSystemUserSession());
      queryStatManager.setMinMillisToStore(0);
      QContext.pushAction(new AbstractActionInput());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QueryStatManager queryStatManager = QueryStatManager.getInstance();
      queryStatManager.stop();
      QContext.popAction();
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void runAllActionsOnPersonTable(String firstName) throws QException
   {
      String       tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;
      Serializable id        = new InsertAction().execute(new InsertInput(tableName).withRecord(new QRecord().withValue("firstName", firstName))).getRecords().get(0).getValue("id");
      new UpdateAction().execute(new UpdateInput(tableName).withRecord(new QRecord().withValue("id", id).withValue("lastName", "Simpson")));
      QueryAction.execute(tableName, new QQueryFilter());
      GetAction.execute(tableName, id);
      new CountAction().execute(new CountInput(tableName).withFilter(new QQueryFilter()));
      new AggregateAction().execute(new AggregateInput(tableName).withAggregate(new Aggregate("id", AggregateOperator.COUNT)));
      new DeleteAction().execute(new DeleteInput(tableName).withPrimaryKeys(List.of(id)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithoutCapability() throws Exception
   {
      ///////////////////////////////////////////////////////////////////////////////////////////
      // make sure the query stats capability is turned off, to ensure it doesn't get recorded //
      ///////////////////////////////////////////////////////////////////////////////////////////
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withoutCapability(Capability.QUERY_STATS);

      runAllActionsOnPersonTable("Homer");

      assertEquals(0, QueryStatManager.getInstance().getQueryStats().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithCapability() throws Exception
   {
      ///////////////////////////////////////////////////////////////////////////////////////
      // make sure the query stats capability is turned on, to ensure they do get recorded //
      ///////////////////////////////////////////////////////////////////////////////////////
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withCapability(Capability.QUERY_STATS);

      runAllActionsOnPersonTable("Marge");

      assertThat(QueryStatManager.getInstance().getQueryStats())
         .hasSizeGreaterThanOrEqualTo(7)
         .anyMatch(qs -> qs.getBackendAction().equals(InsertAction.class.getSimpleName()) && qs.getRecordCount().equals(1))
         .anyMatch(qs -> qs.getBackendAction().equals(UpdateAction.class.getSimpleName()) && qs.getRecordCount().equals(1))
         .anyMatch(qs -> qs.getBackendAction().equals(DeleteAction.class.getSimpleName()) && qs.getRecordCount().equals(1))
         .anyMatch(qs -> qs.getBackendAction().equals(QueryAction.class.getSimpleName()))
         .anyMatch(qs -> qs.getBackendAction().equals(CountAction.class.getSimpleName()) && qs.getRecordCount().equals(1))
         .anyMatch(qs -> qs.getBackendAction().equals(AggregateAction.class.getSimpleName()) && qs.getRecordCount().equals(1))
         .allMatch(qs -> qs.getTableName().equals(table.getName()))
         .allMatch(qs -> qs.getRecordCount() != null)
         .allMatch(qs -> qs.getStartTimestamp() != null)
         .allMatch(qs -> qs.getFirstResultTimestamp() != null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithMinMillisToStore() throws Exception
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure the query stats capability is turned on, but set query stat manager to only store if slow (100 millis) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QueryStatManager.getInstance().setMinMillisToStore(100);
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withCapability(Capability.QUERY_STATS);

      runAllActionsOnPersonTable("Bart");

      assertEquals(0, QueryStatManager.getInstance().getQueryStats().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithQueryStatConsumer() throws Exception
   {
      /////////////////////////////////////////////////////////////////////////////////////
      // make sure the query stats capability is turned on, and that min millis is low   //
      /////////////////////////////////////////////////////////////////////////////////////
      TestQueryStatConsumer consumer = new TestQueryStatConsumer();

      QueryStatManager queryStatManager = QueryStatManager.getInstance();
      queryStatManager.setMinMillisToStore(0);
      queryStatManager.setQueryStatConsumers(List.of(consumer));

      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withCapability(Capability.QUERY_STATS);

      runAllActionsOnPersonTable("Lisa");

      //////////////////////////////////////////////////////////////////
      // make sure consumer and manager itself both got all the stats //
      //////////////////////////////////////////////////////////////////
      assertThat(queryStatManager.getQueryStats()).hasSizeGreaterThanOrEqualTo(7);
      assertThat(consumer.count).isGreaterThanOrEqualTo(7);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithQueryStatConsumerAndHighMinMillis() throws Exception
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure the query stats capability is turned on, but set query stat manager to only store if slow (100 millis) //
      // but - it has a consumer, which will get the events regardless of the min-millis                                  //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      TestQueryStatConsumer consumer = new TestQueryStatConsumer();

      QueryStatManager queryStatManager = QueryStatManager.getInstance();
      queryStatManager.setMinMillisToStore(100);
      queryStatManager.setQueryStatConsumers(List.of(consumer));

      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withCapability(Capability.QUERY_STATS);

      runAllActionsOnPersonTable("Maggie");

      assertEquals(0, queryStatManager.getQueryStats().size());
      assertThat(consumer.count).isGreaterThanOrEqualTo(7);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMultipleConsumersIncludingOneThatThrows() throws Exception
   {
      TestQueryStatConsumer       consumer1       = new TestQueryStatConsumer();
      TestBrokenQueryStatConsumer brokenConsumer1 = new TestBrokenQueryStatConsumer();
      TestQueryStatConsumer       consumer2       = new TestQueryStatConsumer();

      QueryStatManager queryStatManager = QueryStatManager.getInstance();
      queryStatManager.setQueryStatConsumers(List.of(consumer1, brokenConsumer1, consumer2));

      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withCapability(Capability.QUERY_STATS);

      runAllActionsOnPersonTable("Ned");

      assertThat(consumer1.count).isGreaterThanOrEqualTo(7);
      assertThat(consumer2.count).isGreaterThanOrEqualTo(7);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryRecordCount() throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      new InsertAction().execute(new InsertInput(table.getName()).withRecords(List.of(
         new QRecord().withValue("firstName", "Homer"),
         new QRecord().withValue("firstName", "Marge"),
         new QRecord().withValue("firstName", "Bart")
      )));

      table.withCapability(Capability.QUERY_STATS);

      new QueryAction().execute(new QueryInput(table.getName()));
      RecordPipe recordPipe = new RecordPipe();
      new QueryAction().execute(new QueryInput(table.getName()).withRecordPipe(recordPipe));

      assertEquals(3, recordPipe.getTotalRecordCount());
      assertThat(QueryStatManager.getInstance().getQueryStats())
         .hasSize(2)
         .allMatch(qs -> qs.getRecordCount().equals(3));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static class TestQueryStatConsumer implements QueryStatConsumerInterface
   {
      private int count = 0;



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public void accept(QueryStat queryStat)
      {
         count++;
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static class TestBrokenQueryStatConsumer implements QueryStatConsumerInterface
   {

      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public void accept(QueryStat queryStat)
      {
         throw new RuntimeException("Broken query stat");
      }
   }

}