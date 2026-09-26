/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for MongoDBTransaction 
 *******************************************************************************/
class MongoDBTransactionTest extends BaseTest
{

   /*******************************************************************************
    ** Our testcontainer only runs a single mongo, so it doesn't support transactions.
    ** The Backend built by TestUtils is configured to with transactionsSupported = false
    ** make sure things all work like this.
    *******************************************************************************/
   @Test
   void testWithTransactionsDisabled() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      insertInput.setRecords(List.of(new QRecord().withValue("firstName", "Darin")));

      QBackendTransaction transaction = QBackendTransaction.openFor(insertInput);
      assertNotNull(transaction);
      assertThat(transaction).isInstanceOf(MongoDBTransaction.class);
      MongoDBTransaction mongoDBTransaction = (MongoDBTransaction) transaction;
      assertNotNull(mongoDBTransaction.getMongoClient());
      assertNotNull(mongoDBTransaction.getClientSession());

      insertInput.setTransaction(transaction);
      new InsertAction().execute(insertInput);
      transaction.commit();
   }



   /*******************************************************************************
    ** make sure we throw an error if we do turn on transaction support, but our
    ** mongo backend can't handle them
    *******************************************************************************/
   @Test
   void testWithTransactionsEnabled() throws QException
   {
      MongoDBBackendMetaData backend = (MongoDBBackendMetaData) QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);

      try
      {
         backend.setTransactionsSupported(true);

         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
         insertInput.setRecords(List.of(new QRecord().withValue("firstName", "Darin")));

         QBackendTransaction transaction = QBackendTransaction.openFor(insertInput);
         assertNotNull(transaction);
         assertThat(transaction).isInstanceOf(MongoDBTransaction.class);
         MongoDBTransaction mongoDBTransaction = (MongoDBTransaction) transaction;
         assertNotNull(mongoDBTransaction.getMongoClient());
         assertNotNull(mongoDBTransaction.getClientSession());

         insertInput.setTransaction(transaction);

         assertThatThrownBy(() -> new InsertAction().execute(insertInput))
            .isInstanceOf(QException.class)
            .hasRootCauseInstanceOf(MongoCommandException.class);

         assertThatThrownBy(() -> transaction.commit())
            .isInstanceOf(QException.class)
            .hasRootCauseInstanceOf(MongoCommandException.class);
      }
      finally
      {
         backend.setTransactionsSupported(false);
      }
   }



   /*******************************************************************************
    ** Callbacks run once, in order, after commit - including when the backend
    ** doesn't support transactions (as its writes are already durable) - and a
    ** failing callback doesn't fail the commit or stop later callbacks.
    *******************************************************************************/
   @Test
   void callbacksRunAfterCommitInOrder() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      insertInput.setRecords(List.of(new QRecord().withValue("firstName", "Darin")));

      try(QBackendTransaction transaction = QBackendTransaction.openFor(insertInput))
      {
         List<String> ran = new ArrayList<>();
         transaction.addAfterCommitCallback(() -> ran.add("a"));
         transaction.addAfterCommitCallback(() ->
         {
            throw (new IllegalStateException("callback failure"));
         });
         transaction.addAfterCommitCallback(() -> ran.add("c"));

         insertInput.setTransaction(transaction);
         new InsertAction().execute(insertInput);
         assertThat(ran).isEmpty();

         transaction.commit();
         transaction.commit();
         assertThat(ran).containsExactly("a", "c");
      }
   }



   /*******************************************************************************
    ** Rollback must discard callbacks, so a later commit doesn't run them.
    *******************************************************************************/
   @Test
   void callbacksDiscardedOnRollback() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);

      try(QBackendTransaction transaction = QBackendTransaction.openFor(insertInput))
      {
         AtomicInteger runCount = new AtomicInteger(0);
         transaction.addAfterCommitCallback(runCount::incrementAndGet);

         transaction.rollback();
         transaction.commit();
         assertThat(runCount.get()).isEqualTo(0);
      }
   }



   /*******************************************************************************
    ** If the commit itself fails (here, transactions turned on against our
    ** single-node mongo, which can't do them), callbacks must not run.
    *******************************************************************************/
   @Test
   void callbacksNotRunWhenCommitFails() throws QException
   {
      MongoDBBackendMetaData backend = (MongoDBBackendMetaData) QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);

      try
      {
         backend.setTransactionsSupported(true);

         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
         insertInput.setRecords(List.of(new QRecord().withValue("firstName", "Darin")));

         try(QBackendTransaction transaction = QBackendTransaction.openFor(insertInput))
         {
            AtomicInteger runCount = new AtomicInteger(0);
            transaction.addAfterCommitCallback(runCount::incrementAndGet);

            insertInput.setTransaction(transaction);
            assertThatThrownBy(() -> new InsertAction().execute(insertInput)).isInstanceOf(QException.class);

            assertThatThrownBy(transaction::commit).isInstanceOf(QException.class);
            assertThat(runCount.get()).isEqualTo(0);
         }
      }
      finally
      {
         backend.setTransactionsSupported(false);
      }
   }



   /*******************************************************************************
    ** If the commit fails, its pending callbacks are discarded - so a later,
    ** successful commit on the same transaction must not run them (their work
    ** was never committed).  Uses a stand-in client whose commit can be made to
    ** fail, as our single-node test mongo can't commit transactions at all.
    *******************************************************************************/
   @Test
   void callbacksDiscardedWhenCommitFails() throws QException
   {
      AtomicBoolean          failCommit = new AtomicBoolean(true);
      MongoDBBackendMetaData backend    = new MongoDBBackendMetaData().withTransactionsSupported(true);

      try(MongoDBTransaction transaction = new MongoDBTransaction(backend, clientWithFailableCommit(failCommit)))
      {
         AtomicInteger runCount = new AtomicInteger(0);
         transaction.addAfterCommitCallback(runCount::incrementAndGet);
         assertThatThrownBy(transaction::commit).isInstanceOf(QException.class);

         failCommit.set(false);
         transaction.commit();
         assertThat(runCount.get()).isEqualTo(0);

         ////////////////////////////////////////////////////
         // callbacks added after the failure run as usual //
         ////////////////////////////////////////////////////
         transaction.addAfterCommitCallback(runCount::incrementAndGet);
         transaction.commit();
         assertThat(runCount.get()).isEqualTo(1);
      }
   }



   /*******************************************************************************
    ** A MongoClient whose sessions' commitTransaction throws while failCommit is
    ** true.  Other session and client calls do nothing.
    *******************************************************************************/
   private static MongoClient clientWithFailableCommit(AtomicBoolean failCommit)
   {
      ClientSession clientSession = (ClientSession) Proxy.newProxyInstance(ClientSession.class.getClassLoader(), new Class<?>[] { ClientSession.class }, (proxy, method, args) ->
      {
         if("commitTransaction".equals(method.getName()) && failCommit.get())
         {
            throw (new MongoException("Expected commit failure"));
         }
         return (null);
      });

      return ((MongoClient) Proxy.newProxyInstance(MongoClient.class.getClassLoader(), new Class<?>[] { MongoClient.class }, (proxy, method, args) ->
         "startSession".equals(method.getName()) ? clientSession : null));
   }

}
