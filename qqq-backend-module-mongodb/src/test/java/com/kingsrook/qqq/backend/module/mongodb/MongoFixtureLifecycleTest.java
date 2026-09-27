/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.mongodb;


import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.utility.MountableFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Verifies MongoDB fixture startup readiness and per-class ownership.
 ******************************************************************************/
class MongoFixtureLifecycleTest
{
   /***************************************************************************
    ** Authenticated native operations must work after fixture startup returns.
    ***************************************************************************/
   @Test
   void startupWaitsForMappedMongoPort() throws Exception
   {
      BaseTest owner = new BaseTest();
      try
      {
         owner.beforeAll();
         owner.baseBeforeEach();
         try(MongoClient client = BaseTest.getMongoClient())
         {
            client.getDatabase(TestUtils.MONGO_DATABASE).runCommand(new Document("ping", 1));
         }
      }
      finally
      {
         QContext.clear();
         GenericContainer<?> container = container(owner);
         if(container != null)
         {
            container.stop();
         }
      }
   }



   /***************************************************************************
    ** Two owners overlap, as they can when test classes execute concurrently.
    ***************************************************************************/
   @Test
   void closingOneOwnerLeavesTheOtherMongoAvailable() throws Exception
   {
      BaseTest first = new BaseTest();
      BaseTest second = new BaseTest();
      GenericContainer<?> firstContainer = null;
      GenericContainer<?> secondContainer = null;
      try
      {
         first.beforeAll();
         firstContainer = container(first);
         second.beforeAll();
         secondContainer = container(second);
         first.afterAll();

         assertTrue(secondContainer.isRunning(), "closing the first fixture stopped the second container");
         second.baseBeforeEach();
         try(MongoClient client = BaseTest.getMongoClient())
         {
            client.getDatabase(TestUtils.MONGO_DATABASE).runCommand(new Document("ping", 1));
         }
      }
      finally
      {
         QContext.clear();
         if(secondContainer != null)
         {
            secondContainer.stop();
         }
         if(firstContainer != null)
         {
            firstContainer.stop();
         }
      }
   }



   /***************************************************************************
    ** Hold init after root-user creation: a reachable temporary server cannot
    ** satisfy readiness, even though authentication already works inside it.
    ***************************************************************************/
   @Test
   void initializationServerCannotSatisfyReadiness() throws Exception
   {
      CountDownLatch gateEntered = new CountDownLatch(1);
      AtomicReference<GenericContainer<?>> owned = new AtomicReference<>();
      BaseTest owner = new BaseTest()
      {
         /***************************************************************************
          ** The native entrypoint sources this owned script before final startup.
          ***************************************************************************/
         @Override
         protected GenericContainer<?> createMongoContainer()
         {
            GenericContainer<?> container = super.createMongoContainer()
               .withCopyFileToContainer(MountableFile.forClasspathResource("mongo-fixture/hold-initialization.sh"), "/docker-entrypoint-initdb.d/hold-initialization.sh")
               .withLogConsumer(frame ->
               {
                  if(frame.getUtf8String().contains("QQQ_OWNED_INIT_GATE_ENTERED"))
                  {
                     gateEntered.countDown();
                  }
               });
            owned.set(container);
            return container;
         }
      };
      var executor = Executors.newSingleThreadExecutor();
      var startup = executor.submit(owner::beforeAll);
      try
      {
         assertTrue(gateEntered.await(30, TimeUnit.SECONDS), "owned native initialization gate was not reached");
         var temporaryPing = owned.get().execInContainer("mongosh", "--quiet", "--eval", """
            const admin = db.getSiblingDB('admin');
            admin.auth(process.env.MONGO_INITDB_ROOT_USERNAME, process.env.MONGO_INITDB_ROOT_PASSWORD);
            if (admin.runCommand({connectionStatus: 1}).authInfo.authenticatedUsers.length !== 1 || admin.runCommand({ping: 1}).ok !== 1) quit(1);
            """);
         assertEquals(0, temporaryPing.getExitCode(), "temporary initialization server must be reachable and authenticate");
         assertThrows(TimeoutException.class, () -> startup.get(500, TimeUnit.MILLISECONDS), "startup returned while initialization was still held");
         assertEquals(0, owned.get().execInContainer("touch", "/tmp/qqq-owned-init-release").getExitCode());
         startup.get(60, TimeUnit.SECONDS);
         owner.baseBeforeEach();
         try(MongoClient client = BaseTest.getMongoClient())
         {
            var database = client.getDatabase(TestUtils.MONGO_DATABASE);
            assertEquals(1.0, database.runCommand(new Document("ping", 1)).getDouble("ok"));
            assertEquals(1, client.getDatabase("admin").runCommand(new Document("connectionStatus", 1))
               .get("authInfo", Document.class).getList("authenticatedUsers", Document.class).size());
            database.getCollection("ownedFixtureControl").insertOne(new Document("marker", "ready"));
            assertEquals(1, database.getCollection("ownedFixtureControl").countDocuments());
         }
      }
      finally
      {
         GenericContainer<?> container = owned.get();
         if(container != null && container.isRunning())
         {
            container.execInContainer("touch", "/tmp/qqq-owned-init-release");
         }
         try
         {
            startup.get(60, TimeUnit.SECONDS);
         }
         finally
         {
            executor.shutdownNow();
            if(container != null)
            {
               container.stop();
            }
            QContext.clear();
         }
      }
   }



   /***************************************************************************
    ** A deterministic native entrypoint exit must retain stdout and exit state
    ** before Testcontainers removes the failed container, with credentials redacted.
    ***************************************************************************/
   @Test
   void failedInitializationRetainsNativeDiagnostics() throws Exception
   {
      AtomicReference<GenericContainer<?>> owned = new AtomicReference<>();
      BaseTest owner = new BaseTest()
      {
         /***************************************************************************
          ** A shorter test budget does not change the fixture's 60-second default.
          ***************************************************************************/
         @Override
         protected GenericContainer<?> createMongoContainer()
         {
            GenericContainer<?> container = super.createMongoContainer()
               .withCopyFileToContainer(MountableFile.forClasspathResource("mongo-fixture/fail-initialization.sh"), "/docker-entrypoint-initdb.d/fail-initialization.sh")
               .withStartupTimeout(Duration.ofSeconds(10));
            owned.set(container);
            return container;
         }
      };
      try
      {
         assertThrows(RuntimeException.class, owner::beforeAll);
         String evidence = owner.getMongoDiagnostics();
         assertTrue(evidence.contains("QQQ_OWNED_INIT_FAILURE"), evidence);
         assertTrue(evidence.contains("exitCode=42"), evidence);
         assertTrue(evidence.contains("oomKilled=false"), evidence);
         assertFalse(evidence.contains(TestUtils.MONGO_USERNAME));
         assertFalse(evidence.contains(TestUtils.MONGO_PASSWORD));
         assertFalse(owned.get().isRunning());
      }
      finally
      {
         if(owned.get() != null)
         {
            owned.get().stop();
         }
      }
   }



   /***************************************************************************
    ** An exit before the readiness strategy runs still preserves native evidence.
    ***************************************************************************/
   @Test
   void earlyExitRetainsNativeDiagnostics()
   {
      BaseTest owner = new BaseTest()
      {
         /***************************************************************************
          ** Exit before Mongo initialization or the port wait can begin.
          ***************************************************************************/
         @Override
         protected GenericContainer<?> createMongoContainer()
         {
            return super.createMongoContainer()
               .withCommand("bash", "-c", "echo QQQ_OWNED_EARLY_EXIT; exit 43")
               .withStartupCheckStrategy(new MinimumDurationRunningStartupCheckStrategy(Duration.ofSeconds(5)));
         }
      };
      try
      {
         assertThrows(RuntimeException.class, owner::beforeAll);
         String evidence = owner.getMongoDiagnostics();
         assertTrue(evidence.contains("QQQ_OWNED_EARLY_EXIT"), evidence);
         assertTrue(evidence.contains("exitCode=43"), evidence);
         assertTrue(evidence.contains("oomKilled=false"), evidence);
      }
      finally
      {
         owner.afterAll();
      }
   }



   /***************************************************************************
    ** Successful cleanup owns and closes the client it opened.
    ***************************************************************************/
   @Test
   void successfulCleanupClosesOwnedClientAndClearsContext()
   {
      assertCleanupOwnership(false);
   }



   /***************************************************************************
    ** Drop and close failures retain their causes and still clear named context.
    ***************************************************************************/
   @Test
   void failedCleanupClosesOwnedClientAndClearsContext()
   {
      assertCleanupOwnership(true);
   }



   /***************************************************************************
    ** Intercept only the native client factory; execute the actual fixture cleanup.
    ***************************************************************************/
   private void assertCleanupOwnership(boolean fail)
   {
      QContext.init(TestUtils.defineInstance(), new QSession());
      QContext.setObject("ownedCleanupMarker", "present");
      MongoClient client = mock(MongoClient.class);
      MongoDatabase database = mock(MongoDatabase.class);
      when(client.getDatabase(TestUtils.MONGO_DATABASE)).thenReturn(database);
      IllegalStateException dropFailure = new IllegalStateException("owned drop failure");
      IllegalStateException closeFailure = new IllegalStateException("owned close failure");
      if(fail)
      {
         doThrow(dropFailure).when(database).drop();
         doThrow(closeFailure).when(client).close();
      }
      try(MockedStatic<MongoClients> factory = mockStatic(MongoClients.class))
      {
         factory.when(() -> MongoClients.create(any(MongoClientSettings.class))).thenReturn(client);
         if(fail)
         {
            assertSame(dropFailure, assertThrows(IllegalStateException.class, () -> new BaseTest().baseAfterEach()));
            assertEquals(1, dropFailure.getSuppressed().length);
            assertSame(closeFailure, dropFailure.getSuppressed()[0]);
         }
         else
         {
            new BaseTest().baseAfterEach();
         }
         verify(database).drop();
         verify(client).close();
         assertNull(QContext.getQInstance());
         assertNull(QContext.getQSession());
         assertNull(QContext.getObjects());
      }
      finally
      {
         QContext.clear();
      }
   }



   /***************************************************************************
    ** Keep both handles for cleanup even if the old shared field is overwritten.
    ***************************************************************************/
   private GenericContainer<?> container(BaseTest owner) throws Exception
   {
      Field field = BaseTest.class.getDeclaredField("mongoDBContainer");
      field.setAccessible(true);
      return (GenericContainer<?>) field.get(owner);
   }
}
