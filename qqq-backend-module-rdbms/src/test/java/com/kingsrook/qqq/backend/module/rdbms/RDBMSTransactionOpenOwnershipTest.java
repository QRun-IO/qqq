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

package com.kingsrook.qqq.backend.module.rdbms;


import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistentReadInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionProviderInterface;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.strategy.BaseRDBMSActionStrategy;
import com.kingsrook.qqq.backend.module.rdbms.strategy.ConsistentReadStart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Native transaction opening transfers ownership only after construction succeeds.
 ** Faults enter through the existing connection-provider seam, without a database.
 *******************************************************************************/
class RDBMSTransactionOpenOwnershipTest
{
   private static final String TABLE = "ownership_test";


   /*******************************************************************************
    ** Scope the deterministic provider to a new instance for every test.
    *******************************************************************************/
   @BeforeEach
   void setUp()
   {
      FaultProvider.reset();
      RDBMSBackendMetaData backend = new RDBMSBackendMetaData().withName("ownership_backend").withVendor("h2")
         .withConnectionProvider(new QCodeReference(FaultProvider.class))
         .withActionStrategyCodeReference(new QCodeReference(SnapshotStrategy.class));
      QInstance instance = new QInstance().withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("anonymous").withType(QAuthenticationType.FULLY_ANONYMOUS));
      instance.addBackend(backend);
      instance.addTable(new QTableMetaData().withName(TABLE).withBackendName(backend.getName()).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      QContext.init(instance, new QSession());
   }


   /*******************************************************************************
    ** Release the test provider/context; no checkout is hidden in provider.close.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      ConnectionManager.resetConnectionProviders();
      QContext.clear();
      FaultProvider.reset();
   }


   /*******************************************************************************
    ** A setAutoCommit failure leaves no transaction to own the acquired resource.
    *******************************************************************************/
   @Test
   void constructorFailureClosesAcquiredConnection()
   {
      SQLException original = new SQLException("Synthetic constructor failure", "08007");
      FaultProvider.constructorFailure = original;
      QException failure = assertThrows(QException.class, () -> new RDBMSBackendModule().openTransaction(new InsertInput(TABLE)));
      assertSame(original, failure.getCause());
      assertEquals(1, FaultProvider.acquisitions);
      assertEquals(1, FaultProvider.autoCommitCalls);
      assertEquals(1, FaultProvider.closes);
      assertEquals(0, original.getSuppressed().length);
   }


   /*******************************************************************************
    ** Cleanup failure is retained without replacing the constructor's primary error.
    *******************************************************************************/
   @Test
   void closeFailureIsSuppressedOnConstructorFailure()
   {
      SQLException original = new SQLException("Synthetic constructor failure");
      SQLException cleanup = new SQLException("Synthetic close failure");
      FaultProvider.constructorFailure = original;
      FaultProvider.closeFailure = cleanup;
      QException failure = assertThrows(QException.class, () -> new RDBMSBackendModule().openTransaction(new InsertInput(TABLE)));
      assertSame(original, failure.getCause());
      assertEquals(1, FaultProvider.closes);
      assertEquals(1, original.getSuppressed().length);
      assertSame(cleanup, original.getSuppressed()[0]);
   }


   /*******************************************************************************
    ** A driver reusing one exception must not replace it with self-suppression error.
    *******************************************************************************/
   @Test
   void identicalCleanupExceptionPreservesPrimaryFailure()
   {
      SQLException original = new SQLException("Synthetic reused driver failure");
      FaultProvider.constructorFailure = original;
      FaultProvider.closeFailure = original;
      QException failure = assertThrows(QException.class, () -> new RDBMSBackendModule().openTransaction(new InsertInput(TABLE)));
      assertSame(original, failure.getCause());
      assertEquals(1, FaultProvider.closes);
      assertEquals(0, original.getSuppressed().length);
   }


   /*******************************************************************************
    ** An unsuccessful checkout never returns a connection for this caller to close.
    *******************************************************************************/
   @Test
   void acquisitionFailureDoesNotAttemptClose()
   {
      SQLException original = new SQLException("Synthetic checkout failure");
      FaultProvider.acquisitionFailure = original;
      QException failure = assertThrows(QException.class, () -> new RDBMSBackendModule().openTransaction(new InsertInput(TABLE)));
      assertSame(original, failure.getCause());
      assertEquals(1, FaultProvider.acquisitions);
      assertEquals(0, FaultProvider.autoCommitCalls);
      assertEquals(0, FaultProvider.closes);
   }


   /*******************************************************************************
    ** A successful transaction retains the exact checkout until its owner closes it.
    *******************************************************************************/
   @Test
   void successTransfersOwnershipToTransaction() throws QException
   {
      try(QBackendTransaction transaction = new RDBMSBackendModule().openTransaction(new InsertInput(TABLE)))
      {
         assertSame(FaultProvider.connection, ((RDBMSTransaction) transaction).getConnection());
         assertEquals(1, FaultProvider.acquisitions);
         assertEquals(1, FaultProvider.autoCommitCalls);
         assertEquals(0, FaultProvider.closes);
      }
      assertEquals(1, FaultProvider.closes);
   }


   /*******************************************************************************
    ** The consistent-read superclass can fail before subclass cleanup is entered.
    *******************************************************************************/
   @Test
   void consistentConstructorFailureClosesAcquiredConnection()
   {
      SQLException original = new SQLException("Synthetic consistent constructor failure");
      FaultProvider.constructorFailure = original;
      QException failure = assertThrows(QException.class, () -> new RDBMSBackendModule().openConsistentReadTransaction(new ConsistentReadInput("ownership_backend")));
      assertSame(original, failure.getCause());
      assertEquals(1, FaultProvider.closes);
   }


   /*******************************************************************************
    ** Consistent-read opening preserves both its primary and cleanup failures.
    *******************************************************************************/
   @Test
   void consistentCloseFailureIsSuppressed()
   {
      SQLException original = new SQLException("Synthetic consistent constructor failure");
      SQLException cleanup = new SQLException("Synthetic close failure");
      FaultProvider.constructorFailure = original;
      FaultProvider.closeFailure = cleanup;
      QException failure = assertThrows(QException.class, () -> new RDBMSBackendModule().openConsistentReadTransaction(new ConsistentReadInput("ownership_backend")));
      assertSame(original, failure.getCause());
      assertEquals(1, FaultProvider.closes);
      assertEquals(1, original.getSuppressed().length);
      assertSame(cleanup, original.getSuppressed()[0]);
   }


   /*******************************************************************************
    ** Consistent-read acquisition failures must not touch an unacquired resource.
    *******************************************************************************/
   @Test
   void consistentAcquisitionFailureDoesNotAttemptClose()
   {
      SQLException original = new SQLException("Synthetic consistent checkout failure");
      FaultProvider.acquisitionFailure = original;
      QException failure = assertThrows(QException.class, () -> new RDBMSBackendModule().openConsistentReadTransaction(new ConsistentReadInput("ownership_backend")));
      assertSame(original, failure.getCause());
      assertEquals(1, FaultProvider.acquisitions);
      assertEquals(0, FaultProvider.autoCommitCalls);
      assertEquals(0, FaultProvider.closes);
   }


   /*******************************************************************************
    ** Success retains the exact resource and normal snapshot restoration on close.
    *******************************************************************************/
   @Test
   void consistentSuccessTransfersOwnership() throws QException
   {
      try(QBackendTransaction transaction = new RDBMSBackendModule().openConsistentReadTransaction(new ConsistentReadInput("ownership_backend")).orElseThrow())
      {
         assertSame(FaultProvider.connection, ((RDBMSTransaction) transaction).getConnection());
         assertEquals(ConsistencyGuarantee.ONE_INSTANT, transaction.getConsistencyGuarantee());
         assertEquals(0, FaultProvider.closes);
      }
      assertEquals(1, FaultProvider.closes);
      assertEquals(1, FaultProvider.rollbacks);
      assertEquals(1, FaultProvider.isolationRestores);
      assertEquals(1, FaultProvider.readOnlyRestores);
   }


   /*******************************************************************************
    ** Existing subclass cleanup after snapshot failure still rolls back/restores.
    *******************************************************************************/
   @Test
   void snapshotFailureRetainsSubclassCleanup()
   {
      SQLException original = new SQLException("Synthetic snapshot failure");
      FaultProvider.snapshotFailure = original;
      QException failure = assertThrows(QException.class, () -> new RDBMSBackendModule().openConsistentReadTransaction(new ConsistentReadInput("ownership_backend")));
      assertSame(original, failure.getCause());
      assertEquals(1, FaultProvider.closes);
      assertEquals(1, FaultProvider.rollbacks);
      assertEquals(1, FaultProvider.isolationRestores);
      assertEquals(1, FaultProvider.readOnlyRestores);
   }


   /*******************************************************************************
    ** Fixed snapshot strategy uses the existing native strategy metadata seam.
    *******************************************************************************/
   public static class SnapshotStrategy extends BaseRDBMSActionStrategy
   {
      /*******************************************************************************
       ** Return a deterministic snapshot or fail after superclass construction.
       *******************************************************************************/
      @Override
      public ConsistentReadStart beginConsistentRead(Connection connection, ConsistentReadInput input) throws SQLException
      {
         if(FaultProvider.snapshotFailure != null)
         {
            throw (FaultProvider.snapshotFailure);
         }
         return (new ConsistentReadStart(Optional.empty(), ConsistencyGuarantee.ONE_INSTANT));
      }
   }


   /*******************************************************************************
    ** Existing metadata extension used to inject failures at JDBC ownership edges.
    *******************************************************************************/
   public static class FaultProvider implements ConnectionProviderInterface
   {
      private static SQLException acquisitionFailure;
      private static SQLException constructorFailure;
      private static SQLException closeFailure;
      private static SQLException snapshotFailure;
      private static Connection connection;
      private static int acquisitions;
      private static int autoCommitCalls;
      private static int closes;
      private static int rollbacks;
      private static int isolationRestores;
      private static int readOnlyRestores;


      /*******************************************************************************
       ** Clear all synthetic state between independent test instances.
       *******************************************************************************/
      private static void reset()
      {
         acquisitionFailure = null;
         constructorFailure = null;
         closeFailure = null;
         snapshotFailure = null;
         connection = null;
         acquisitions = 0;
         autoCommitCalls = 0;
         closes = 0;
         rollbacks = 0;
         isolationRestores = 0;
         readOnlyRestores = 0;
      }


      /*******************************************************************************
       ** No external database or pool is needed for ownership fault injection.
       *******************************************************************************/
      @Override
      public void init(RDBMSBackendMetaData backend)
      {
      }


      /*******************************************************************************
       ** Fail checkout or return a strict JDBC double recording only expected calls.
       *******************************************************************************/
      @Override
      public Connection getConnection() throws SQLException
      {
         acquisitions++;
         if(acquisitionFailure != null)
         {
            throw (acquisitionFailure);
         }
         connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, (proxy, method, args) ->
         {
            switch(method.getName())
            {
               case "setAutoCommit":
                  autoCommitCalls++;
                  assertFalse((Boolean) args[0]);
                  if(constructorFailure != null)
                  {
                     throw (constructorFailure);
                  }
                  return (null);
               case "close":
                  if(closes > 0 && closeFailure == null)
                  {
                     return (null);
                  }
                  closes++;
                  if(closeFailure != null)
                  {
                     throw (closeFailure);
                  }
                  return (null);
               case "isClosed":
                  return (closes > 0 && closeFailure == null);
               case "getTransactionIsolation":
                  return (Connection.TRANSACTION_READ_COMMITTED);
               case "isReadOnly":
                  return (false);
               case "setTransactionIsolation":
                  assertEquals(Connection.TRANSACTION_READ_COMMITTED, args[0]);
                  isolationRestores++;
                  return (null);
               case "setReadOnly":
                  assertFalse((Boolean) args[0]);
                  readOnlyRestores++;
                  return (null);
               case "rollback":
                  rollbacks++;
                  return (null);
               default:
                  throw (new AssertionError("Unexpected JDBC method: " + method.getName()));
            }
         });
         return (connection);
      }
   }
}
