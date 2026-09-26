/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.ConnectionPoolSettings;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.mchange.v2.resourcepool.TimeoutException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for C3P0PooledConnectionProvider 
 *******************************************************************************/
class C3P0PooledConnectionProviderTest extends BaseTest
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      TestUtils.primeTestDatabase("prime-test-database.sql");

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // must call this after the primeTestDatabase call (as i uses a raw version of the backend, w/o our updated settings) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      ////////////////////////////////////////////////////////////
      // just for good measure, do this after each test in here //
      ////////////////////////////////////////////////////////////
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   // @RepeatedTest(100)
   void test() throws Exception
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // change the default database backend to use the class under test here - the C3PL connection pool provider //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QInstance            qInstance = TestUtils.defineInstance();
      RDBMSBackendMetaData backend   = (RDBMSBackendMetaData) qInstance.getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      backend.setConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class));
      QContext.init(qInstance, new QSession());

      /////////////////////////////////////////////////////////////////////////////////
      // sometimes we're seeing this test fail w/ only 2 connections in the pool...  //
      // theory is, maybe, the pool doesn't quite have enough time to open them all? //
      // so, try adding a little sleep here.                                         //
      /////////////////////////////////////////////////////////////////////////////////
      new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON));
      SleepUtils.sleep(500, TimeUnit.MILLISECONDS);

      for(int i = 0; i < 5; i++)
      {
         new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON));
      }

      JSONObject debugValues = getDebugStateValues(true);
      assertThat(debugValues.getInt("numConnections")).isBetween(3, 6); // due to potential timing issues, sometimes pool will acquire another 3 conns, so 3 or 6 seems ok.

      //////////////////////////////////////////////////////////////////////////
      // open up several transactions - confirm the pool opens some new conns //
      //////////////////////////////////////////////////////////////////////////
      int noTransactions = 7;
      List<QBackendTransaction> transactions = new ArrayList<>();
      for(int i = 0; i < noTransactions; i++)
      {
         transactions.add(QBackendTransaction.openFor(new InsertInput(TestUtils.TABLE_NAME_PERSON)));
      }

      debugValues = getDebugStateValues(true);
      assertThat(debugValues.getInt("numConnections")).isGreaterThanOrEqualTo(noTransactions);

      transactions.forEach(transaction -> transaction.close());

      /////////////////////////////////////////////////////////////////////////
      // might take a second for the pool to re-claim the closed connections //
      /////////////////////////////////////////////////////////////////////////
      boolean foundMatch = false;
      for(int i = 0; i < noTransactions; i++)
      {
         debugValues = getDebugStateValues(true);
         if(debugValues.getInt("numConnections") == debugValues.getInt("numIdleConnections"))
         {
            foundMatch = true;
            break;
         }
         System.out.println("oops!");
         SleepUtils.sleep(250, TimeUnit.MILLISECONDS);
      }

      assertTrue(foundMatch, "The pool didn't re-claim all connections...");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPoolSettings() throws Exception
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // change the default database backend to use the class under test here - the C3PL connection pool provider //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QInstance            qInstance = TestUtils.defineInstance();
      RDBMSBackendMetaData backend   = (RDBMSBackendMetaData) qInstance.getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      backend.setConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class));
      backend.setConnectionPoolSettings(new ConnectionPoolSettings()
         .withInitialPoolSize(2)
         .withAcquireIncrement(1)
         .withMinPoolSize(1)
         .withMaxPoolSize(4)
         .withCheckoutTimeoutSeconds(1));
      QContext.init(qInstance, new QSession());

      /////////////////////////
      // assert initial size //
      /////////////////////////
      new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON));
      JSONObject debugValues = getDebugStateValues(true);
      assertThat(debugValues.getInt("numConnections")).isEqualTo(2);

      ///////////////////////////////////////////////////////////////////////
      // open (and close) 5 conns - shouldn't get bigger than initial size //
      ///////////////////////////////////////////////////////////////////////
      for(int i = 0; i < 5; i++)
      {
         new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON));
      }
      debugValues = getDebugStateValues(true);
      assertThat(debugValues.getInt("numConnections")).isBetween(2, 4); // due to potential timing issues, sometimes pool will acquire 1 or 2 more, so, this seems ok

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // open up 4 transactions - confirm the pool opens some new conns, but stops at the max, and throws based on checkoutTimeout setting //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<QBackendTransaction> transactions = new ArrayList<>();
      for(int i = 0; i < 5; i++)
      {
         if(i == 4)
         {
            //////////////////////////////////////////
            // expect this one to fail - full pool! //
            //////////////////////////////////////////
            assertThatThrownBy(() -> QBackendTransaction.openFor(new InsertInput(TestUtils.TABLE_NAME_PERSON)))
               .hasRootCauseInstanceOf(TimeoutException.class);
         }
         else
         {
            transactions.add(QBackendTransaction.openFor(new InsertInput(TestUtils.TABLE_NAME_PERSON)));
         }
      }

      debugValues = getDebugStateValues(true);
      assertThat(debugValues.getInt("numConnections")).isEqualTo(4);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueriesForNewConnections() throws Exception
   {
      String uuid = UUID.randomUUID().toString();

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // change the default database backend to use the class under test here - the C3PL connection pool provider //
      // and add a new-connection query to it to insert a record with a UUID, that we can then query for.         //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QInstance            qInstance = TestUtils.defineInstance();
      RDBMSBackendMetaData backend   = (RDBMSBackendMetaData) qInstance.getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      backend.setQueriesForNewConnections(List.of("insert into person (first_name, last_name, email) values ('D', 'K', '" + uuid + "')"));
      backend.setConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class));
      QContext.init(qInstance, new QSession());

      //////////////////////////////////////////////////////////////////////////////////////////////
      // we actually get multiple, because c3p0 default config opens multiple connections at once //
      //////////////////////////////////////////////////////////////////////////////////////////////
      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, uuid)));
      assertThat(records.size()).isGreaterThanOrEqualTo(1);
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   private static JSONObject getDebugStateValues(boolean printIt)
   {
      JSONArray debugArray = ConnectionManager.dumpConnectionProviderDebug();
      for(int i = 0; i < debugArray.length(); i++)
      {
         JSONObject object = debugArray.getJSONObject(i);
         if(TestUtils.DEFAULT_BACKEND_NAME.equals(object.optString("backendName")))
         {
            JSONObject values = object.getJSONObject("values");
            if(printIt)
            {
               System.out.println(values.toString(3));
            }

            JSONObject state = values.getJSONObject("state");
            return state;
         }
      }

      fail("Didn't find debug values...");
      return (null);
   }

}