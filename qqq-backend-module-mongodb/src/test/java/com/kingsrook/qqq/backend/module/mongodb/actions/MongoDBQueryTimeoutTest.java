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


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.session.ServerSession;
import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Exercise cancellation races independently of native server scheduling.
 ******************************************************************************/
class MongoDBQueryTimeoutTest
{
   private final MongoClient owner = mock(MongoClient.class);
   private final ClientSession ownerSession = mock(ClientSession.class);
   private final MongoClientContainer control = mock(MongoClientContainer.class);
   private final MongoClient controlClient = mock(MongoClient.class);
   private final MongoDatabase database = mock(MongoDatabase.class);
   private final BsonDocument sessionId = new BsonDocument("id", new BsonBinary((byte) 4, new byte[16]));



   /*******************************************************************************
    ** Late timer execution cannot act on a session returned to its owner.
    ******************************************************************************/
   @Test
   void completionDisarmsLateCancellation()
   {
      MongoDBQueryTimeout timeout = timeout();
      timeout.close();
      timeout.run();
      timeout.close();
      assertFalse(timeout.hasTimedOut());
      verifyNoInteractions(controlClient);
      verify(owner, never()).close();
      verify(ownerSession, never()).close();
   }



   /*******************************************************************************
    ** Retrying the exact captured ID covers cancellation before server dispatch.
    ******************************************************************************/
   @Test
   void cancellationRepeatsUntilCompletionWithoutQContext() throws Exception
   {
      CountDownLatch attempts = new CountDownLatch(2);
      AtomicInteger calls = new AtomicInteger();
      MongoDBQueryTimeout timeout = timeout();
      when(database.runCommand(any(Bson.class))).thenAnswer(invocation ->
      {
         assertEquals(null, QContext.getQInstance());
         BsonDocument command = invocation.getArgument(0);
         assertEquals(1, command.getArray("killSessions").size());
         assertEquals(sessionId, command.getArray("killSessions").get(0));
         calls.incrementAndGet();
         attempts.countDown();
         return new Document("ok", 1);
      });
      try(var executor = Executors.newSingleThreadExecutor())
      {
         var cancellation = executor.submit(timeout);
         assertTrue(attempts.await(5, TimeUnit.SECONDS));
         assertThrows(QUserFacingException.class, timeout::throwIfTimedOut);
         timeout.close();
         cancellation.get(5, TimeUnit.SECONDS);
         int completedCalls = calls.get();
         timeout.run();
         assertEquals(completedCalls, calls.get());
      }
      verify(control).closeIfNeeded();
      verify(owner, never()).close();
      verify(ownerSession, never()).close();
   }



   /*******************************************************************************
    ** Session reuse waits for the command and control-client cleanup, including
    ** when the caller is already interrupted.
    ******************************************************************************/
   @Test
   void completionDrainsCommandAndControlClose() throws Exception
   {
      CountDownLatch commandEntered = new CountDownLatch(1);
      CountDownLatch releaseCommand = new CountDownLatch(1);
      CountDownLatch closeEntered = new CountDownLatch(1);
      CountDownLatch releaseClose = new CountDownLatch(1);
      CountDownLatch completionEntered = new CountDownLatch(1);
      MongoDBQueryTimeout timeout = timeout();
      when(database.runCommand(any(Bson.class))).thenAnswer(invocation ->
      {
         commandEntered.countDown();
         assertTrue(releaseCommand.await(5, TimeUnit.SECONDS));
         return new Document("ok", 1);
      });
      doAnswer(invocation ->
      {
         closeEntered.countDown();
         assertTrue(releaseClose.await(5, TimeUnit.SECONDS));
         return null;
      }).when(control).closeIfNeeded();
      try(var executor = Executors.newFixedThreadPool(2))
      {
         var cancellation = executor.submit(timeout);
         assertTrue(commandEntered.await(5, TimeUnit.SECONDS));
         var completion = executor.submit(() ->
         {
            Thread.currentThread().interrupt();
            completionEntered.countDown();
            timeout.close();
            return Thread.interrupted();
         });
         try
         {
            assertTrue(completionEntered.await(5, TimeUnit.SECONDS));
            assertFalse(completion.isDone());
            releaseCommand.countDown();
            assertTrue(closeEntered.await(5, TimeUnit.SECONDS));
            assertFalse(completion.isDone());
            releaseClose.countDown();
            assertTrue(completion.get(5, TimeUnit.SECONDS));
            cancellation.get(5, TimeUnit.SECONDS);
         }
         finally
         {
            releaseCommand.countDown();
            releaseClose.countDown();
         }
      }
   }



   /*******************************************************************************
    ** Rejected cancellation preserves timeout reporting and closes owned control
    ** resources without closing a borrowed query owner.
    ******************************************************************************/
   @Test
   void commandFailureStillClosesControl()
   {
      MongoDBQueryTimeout timeout = timeout();
      when(database.runCommand(any(Bson.class))).thenThrow(new IllegalStateException("command rejected"));
      timeout.run();
      timeout.close();
      assertThrows(QUserFacingException.class, timeout::throwIfTimedOut);
      verify(control).closeIfNeeded();
      verify(owner, never()).close();
      verify(ownerSession, never()).close();
   }



   /*******************************************************************************
    ** Cleanup failure must not leave action completion waiting forever.
    ******************************************************************************/
   @Test
   void controlCloseFailureReleasesCompletion()
   {
      MongoDBQueryTimeout timeout = timeout();
      when(database.runCommand(any(Bson.class))).thenThrow(new IllegalStateException("command rejected"));
      doThrow(new IllegalStateException("close failed")).when(control).closeIfNeeded();
      assertThrows(IllegalStateException.class, timeout::run);
      timeout.close();
      assertTrue(timeout.hasTimedOut());
   }



   /*******************************************************************************
    ** The container is borrowed; the coordinator owns only its control client.
    ******************************************************************************/
   private MongoDBQueryTimeout timeout()
   {
      ServerSession serverSession = mock(ServerSession.class);
      when(ownerSession.getServerSession()).thenReturn(serverSession);
      when(serverSession.getIdentifier()).thenReturn(sessionId);
      when(control.getMongoClient()).thenReturn(controlClient);
      when(controlClient.getDatabase("admin")).thenReturn(database);
      when(database.withTimeout(1, TimeUnit.SECONDS)).thenReturn(database);
      MongoClientContainer borrowed = new MongoClientContainer(owner, ownerSession, false);
      assertNotNull(borrowed.getMongoSession());
      return new MongoDBQueryTimeout(1, borrowed, () -> control);
   }
}
