/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for RDBMSTransaction
 *******************************************************************************/
class RDBMSTransactionTest extends BaseTest
{
   private final String testToken = getClass().getName();



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   protected void beforeEach() throws Exception
   {
      TestUtils.primeTestDatabase("prime-test-database.sql");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCommit() throws Exception
   {
      ConnectionManager connectionManager = new ConnectionManager();
      Connection        connection        = connectionManager.getConnection(TestUtils.defineBackend());
      Integer           preCount          = QueryManager.executeStatementForSingleValue(connection, Integer.class, "SELECT COUNT(*) FROM person");

      Connection       connectionForTransaction = connectionManager.getConnection(TestUtils.defineBackend());
      RDBMSTransaction transaction              = new RDBMSTransaction(connectionForTransaction);

      QueryManager.executeUpdate(transaction.getConnection(), "INSERT INTO person (first_name, last_name, email) VALUES (?, ?, ?)", testToken, testToken, testToken);
      transaction.commit();

      Integer postCount = QueryManager.executeStatementForSingleValue(connection, Integer.class, "SELECT COUNT(*) FROM person");
      assertEquals(preCount + 1, postCount);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRollback() throws Exception
   {
      ConnectionManager connectionManager = new ConnectionManager();
      Connection        connection        = connectionManager.getConnection(TestUtils.defineBackend());
      Integer           preCount          = QueryManager.executeStatementForSingleValue(connection, Integer.class, "SELECT COUNT(*) FROM person");

      Connection       connectionForTransaction = connectionManager.getConnection(TestUtils.defineBackend());
      RDBMSTransaction transaction              = new RDBMSTransaction(connectionForTransaction);

      QueryManager.executeUpdate(transaction.getConnection(), "INSERT INTO person (first_name, last_name, email) VALUES (?, ?, ?)", testToken, testToken, testToken);
      transaction.rollback();

      Integer postCount = QueryManager.executeStatementForSingleValue(connection, Integer.class, "SELECT COUNT(*) FROM person");
      assertEquals(preCount, postCount);
   }



   /*******************************************************************************
    ** An after-commit callback must run only once the data is committed - so a
    ** different connection, which can't see the uncommitted insert, can see it
    ** from within the callback.
    *******************************************************************************/
   @Test
   void callbackSeesCommittedDataFromSecondConnection() throws Exception
   {
      ConnectionManager connectionManager = new ConnectionManager();
      try(Connection otherConnection = connectionManager.getConnection(TestUtils.defineBackend());
         RDBMSTransaction transaction = new RDBMSTransaction(connectionManager.getConnection(TestUtils.defineBackend())))
      {
         Integer preCount = countPeople(otherConnection);
         QueryManager.executeUpdate(transaction.getConnection(), "INSERT INTO person (first_name, last_name, email) VALUES (?, ?, ?)", testToken, testToken, testToken);

         AtomicReference<Integer> countSeenByCallback = new AtomicReference<>();
         transaction.addAfterCommitCallback(() -> countSeenByCallback.set(countPeople(otherConnection)));
         assertEquals(preCount, countPeople(otherConnection));
         assertNull(countSeenByCallback.get());

         transaction.commit();
         assertEquals(preCount + 1, countSeenByCallback.get());
      }
   }



   /*******************************************************************************
    ** Rollback must discard callbacks, so a later commit doesn't run them.
    *******************************************************************************/
   @Test
   void callbacksDiscardedOnRollback() throws Exception
   {
      try(RDBMSTransaction transaction = new RDBMSTransaction(new ConnectionManager().getConnection(TestUtils.defineBackend())))
      {
         AtomicInteger runCount = new AtomicInteger(0);
         transaction.addAfterCommitCallback(runCount::incrementAndGet);

         transaction.rollback();
         transaction.commit();
         assertEquals(0, runCount.get());
      }
   }



   /*******************************************************************************
    ** Callbacks run once, in order, and a failing callback doesn't fail the commit.
    *******************************************************************************/
   @Test
   void callbacksRunOnceInOrderDespiteCallbackException() throws Exception
   {
      try(RDBMSTransaction transaction = new RDBMSTransaction(new ConnectionManager().getConnection(TestUtils.defineBackend())))
      {
         List<String> ran = new ArrayList<>();
         transaction.addAfterCommitCallback(() -> ran.add("a"));
         transaction.addAfterCommitCallback(() ->
         {
            throw (new IllegalStateException("callback failure"));
         });
         transaction.addAfterCommitCallback(() -> ran.add("c"));

         transaction.commit();
         transaction.commit();
         assertEquals(List.of("a", "c"), ran);
      }
   }



   /*******************************************************************************
    ** If the commit itself fails, callbacks must not run.
    *******************************************************************************/
   @Test
   void callbacksNotRunWhenCommitFails() throws Exception
   {
      try(RDBMSTransaction transaction = new RDBMSTransaction(new ConnectionManager().getConnection(TestUtils.defineBackend())))
      {
         AtomicInteger runCount = new AtomicInteger(0);
         transaction.addAfterCommitCallback(runCount::incrementAndGet);

         transaction.getConnection().close();
         assertThrows(QException.class, transaction::commit);
         assertEquals(0, runCount.get());
      }
   }



   /*******************************************************************************
    ** If the commit fails, its pending callbacks are discarded - so a later,
    ** successful commit on the same transaction must not run them (their work
    ** was never committed).
    *******************************************************************************/
   @Test
   void callbacksDiscardedWhenCommitFails() throws Exception
   {
      AtomicBoolean failCommit = new AtomicBoolean(true);
      try(RDBMSTransaction transaction = new RDBMSTransaction(connectionWithFailableCommit(new ConnectionManager().getConnection(TestUtils.defineBackend()), failCommit)))
      {
         AtomicInteger runCount = new AtomicInteger(0);
         transaction.addAfterCommitCallback(runCount::incrementAndGet);
         assertThrows(QException.class, transaction::commit);

         failCommit.set(false);
         transaction.commit();
         assertEquals(0, runCount.get());

         ////////////////////////////////////////////////////
         // callbacks added after the failure run as usual //
         ////////////////////////////////////////////////////
         transaction.addAfterCommitCallback(runCount::incrementAndGet);
         transaction.commit();
         assertEquals(1, runCount.get());
      }
   }



   /*******************************************************************************
    ** Wrap a connection so that its commit() throws while failCommit is true.
    *******************************************************************************/
   private static Connection connectionWithFailableCommit(Connection connection, AtomicBoolean failCommit)
   {
      return ((Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class }, (proxy, method, args) ->
      {
         if("commit".equals(method.getName()) && failCommit.get())
         {
            throw (new SQLException("Expected commit failure"));
         }

         try
         {
            return (method.invoke(connection, args));
         }
         catch(InvocationTargetException e)
         {
            throw (e.getCause());
         }
      }));
   }



   /*******************************************************************************
    ** count rows in the person table - unchecked, for use inside a Runnable.
    *******************************************************************************/
   private static Integer countPeople(Connection connection)
   {
      try
      {
         return (QueryManager.executeStatementForSingleValue(connection, Integer.class, "SELECT COUNT(*) FROM person"));
      }
      catch(SQLException e)
      {
         throw (new IllegalStateException("Error counting people", e));
      }
   }

}