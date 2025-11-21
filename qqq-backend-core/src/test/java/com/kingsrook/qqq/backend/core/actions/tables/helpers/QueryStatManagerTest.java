/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.util.List;
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


/*******************************************************************************
 ** Unit test for QueryStatManager 
 *******************************************************************************/
class QueryStatManagerTest extends BaseTest
{
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