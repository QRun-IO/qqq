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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.mongodb.MongoDBBackendModule;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Checks resource ownership without opening external MongoDB connections.
 ******************************************************************************/
class MongoResourceOwnershipTest
{
   /*******************************************************************************
    ** Keep mock-backed context out of subsequent native fixture tests.
    ******************************************************************************/
   @AfterEach
   void clearContext()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Opening one transaction must create exactly one owned client session.
    ******************************************************************************/
   @Test
   void transactionOpensExactlyOneSession()
   {
      QContext.init(TestUtils.defineInstance(), new QSession());
      MongoClient client = mock(MongoClient.class);
      ClientSession session = mock(ClientSession.class);
      when(client.startSession()).thenReturn(session);
      try(MockedStatic<MongoClients> factory = mockStatic(MongoClients.class))
      {
         factory.when(() -> MongoClients.create(any(MongoClientSettings.class))).thenReturn(client);
         InsertInput input = new InsertInput(TestUtils.TABLE_NAME_PERSON);
         try(MongoDBTransaction transaction = (MongoDBTransaction) new MongoDBBackendModule().openTransaction(input))
         {
            assertSame(session, transaction.getClientSession());
            verify(client, times(1)).startSession();
         }
         verify(session).close();
         verify(client).close();
      }
   }



   /*******************************************************************************
    ** Failed session creation must not strand the already-created client.
    ******************************************************************************/
   @Test
   void openClientClosesClientWhenSessionCreationFails()
   {
      MongoDBBackendMetaData backend = TestUtils.defineBackend();
      MongoClient client = mock(MongoClient.class);
      IllegalStateException failure = new IllegalStateException("session unavailable");
      when(client.startSession()).thenThrow(failure);
      try(MockedStatic<MongoClients> factory = mockStatic(MongoClients.class))
      {
         factory.when(() -> MongoClients.create(any(MongoClientSettings.class))).thenReturn(client);
         assertSame(failure, assertThrows(IllegalStateException.class, () -> new AbstractMongoDBAction().openClient(backend, null)));
         verify(client).close();
      }
   }



   /*******************************************************************************
    ** Failed transaction initialization closes both resources and keeps the
    ** original failure rather than returning a partially initialized owner.
    ******************************************************************************/
   @Test
   void transactionClosesClientAndSessionWhenStartFails()
   {
      MongoClient client = mock(MongoClient.class);
      ClientSession session = mock(ClientSession.class);
      IllegalStateException failure = new IllegalStateException("transaction unavailable");
      when(client.startSession()).thenReturn(session);
      doThrow(failure).when(session).startTransaction();
      assertSame(failure, assertThrows(IllegalStateException.class,
         () -> new MongoDBTransaction(new MongoDBBackendMetaData().withTransactionsSupported(true), client)));
      verify(session).close();
      verify(client).close();
   }



   /*******************************************************************************
    ** A failing session close cannot skip closing the owned client.
    ******************************************************************************/
   @Test
   void ownedContainerClosesClientAfterSessionCloseFailure()
   {
      MongoClient client = mock(MongoClient.class);
      ClientSession session = mock(ClientSession.class);
      IllegalStateException failure = new IllegalStateException("session close failed");
      doThrow(failure).when(session).close();
      MongoClientContainer container = new MongoClientContainer(client, session, true);
      assertSame(failure, assertThrows(IllegalStateException.class, container::closeIfNeeded));
      verify(client).close();
   }



   /*******************************************************************************
    ** Transaction close swallows its historic exception but still closes client.
    ******************************************************************************/
   @Test
   void transactionClosesClientAfterSessionCloseFailure()
   {
      MongoClient client = mock(MongoClient.class);
      ClientSession session = mock(ClientSession.class);
      when(client.startSession()).thenReturn(session);
      doThrow(new IllegalStateException("session close failed")).when(session).close();
      new MongoDBTransaction(new MongoDBBackendMetaData().withTransactionsSupported(false), client).close();
      verify(client).close();
   }



   /*******************************************************************************
    ** An action borrowing a transaction handle cannot close either resource.
    ******************************************************************************/
   @Test
   void borrowedHandleClosesOnlyWhenTransactionOwnerCloses()
   {
      MongoClient client = mock(MongoClient.class);
      ClientSession session = mock(ClientSession.class);
      when(client.startSession()).thenReturn(session);
      MongoDBTransaction transaction = new MongoDBTransaction(new MongoDBBackendMetaData().withTransactionsSupported(false), client);
      MongoClientContainer borrowed = new AbstractMongoDBAction().openClient(null, transaction);
      assertSame(client, borrowed.getMongoClient());
      assertSame(session, borrowed.getMongoSession());
      borrowed.closeIfNeeded();
      verify(session, times(0)).close();
      verify(client, times(0)).close();
      transaction.close();
      verify(session).close();
      verify(client).close();
   }



   /*******************************************************************************
    ** A failing client close must be secondary to the session-open failure.
    ******************************************************************************/
   @Test
   void failedSessionOpenPreservesOriginalWhenClientCloseAlsoFails()
   {
      MongoClient client = mock(MongoClient.class);
      IllegalStateException openFailure = new IllegalStateException("session unavailable");
      IllegalStateException closeFailure = new IllegalStateException("client close failed");
      when(client.startSession()).thenThrow(openFailure);
      doThrow(closeFailure).when(client).close();
      try(MockedStatic<MongoClients> factory = mockStatic(MongoClients.class))
      {
         factory.when(() -> MongoClients.create(any(MongoClientSettings.class))).thenReturn(client);
         IllegalStateException thrown = assertThrows(IllegalStateException.class,
            () -> new AbstractMongoDBAction().openClient(TestUtils.defineBackend(), null));
         assertSame(openFailure, thrown);
         assertArrayEquals(new Throwable[] { closeFailure }, thrown.getSuppressed());
         verify(client).close();
      }
   }



   /*******************************************************************************
    ** Try-with-resources closes the client but preserves session-close failure.
    ******************************************************************************/
   @Test
   void ownedContainerPreservesSessionFailureWhenBothClosesThrow()
   {
      MongoClient client = mock(MongoClient.class);
      ClientSession session = mock(ClientSession.class);
      IllegalStateException sessionFailure = new IllegalStateException("session close failed");
      IllegalStateException clientFailure = new IllegalStateException("client close failed");
      doThrow(sessionFailure).when(session).close();
      doThrow(clientFailure).when(client).close();
      MongoClientContainer container = new MongoClientContainer(client, session, true);
      IllegalStateException thrown = assertThrows(IllegalStateException.class, container::closeIfNeeded);
      assertSame(sessionFailure, thrown);
      assertArrayEquals(new Throwable[] { clientFailure }, thrown.getSuppressed());
      verify(session).close();
      verify(client).close();
   }



   /*******************************************************************************
    ** Transaction-start failure remains primary when both cleanup calls fail.
    ******************************************************************************/
   @Test
   void failedTransactionStartPreservesOriginalWhenBothClosesThrow()
   {
      MongoClient client = mock(MongoClient.class);
      ClientSession session = mock(ClientSession.class);
      IllegalStateException startFailure = new IllegalStateException("transaction unavailable");
      IllegalStateException sessionFailure = new IllegalStateException("session close failed");
      IllegalStateException clientFailure = new IllegalStateException("client close failed");
      when(client.startSession()).thenReturn(session);
      doThrow(startFailure).when(session).startTransaction();
      doThrow(sessionFailure).when(session).close();
      doThrow(clientFailure).when(client).close();
      IllegalStateException thrown = assertThrows(IllegalStateException.class,
         () -> new MongoDBTransaction(new MongoDBBackendMetaData().withTransactionsSupported(true), client));
      assertSame(startFailure, thrown);
      assertArrayEquals(new Throwable[] { sessionFailure }, thrown.getSuppressed());
      assertArrayEquals(new Throwable[] { clientFailure }, sessionFailure.getSuppressed());
      verify(session).close();
      verify(client).close();
   }
}
