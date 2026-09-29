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

package com.kingsrook.sampleapp;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobStatus;
import com.kingsrook.qqq.backend.core.actions.async.NonPersistedAsyncJobCallback;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.mongodb.actions.AbstractMongoDBAction;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoClientContainer;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoDBTransaction;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBTableBackendDetails;
import com.kingsrook.sampleapp.metadata.FieldLabTableMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.Binary;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.mongodb.MongoDBContainer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Exercises sample Field Lab transactions against an owned MongoDB replica set.
 ** Native reads outside the transaction establish commit and rollback effects.
 ******************************************************************************/
class SampleMongoReplicaSetIT
{
   private static final String TABLE = FieldLabTableMetaDataProducer.NAME;
   private static final String COLLECTION = "field_lab";
   private static final String BACKEND = "sampleMongoReplica";
   private static final String DATABASE = "qqq_sample_rs_" + UUID.randomUUID().toString().replace("-", "");
   private static final String USERNAME = "sample";
   private static final String PASSWORD = "sample-fixture-only";
   private static MongoDBContainer container;
   private static MongoClient client;



   /*******************************************************************************
    ** The container and mapped port belong only to this class. A user is added
    ** after Testcontainers initializes its single-node replica set.
    ******************************************************************************/
   @BeforeAll
   static void startMongo()
   {
      container = new MongoDBContainer("mongo:7.0").withReplicaSet();
      container.withCommand("--replSet", "docker-rs", "--setParameter", "enableTestCommands=1");
      container.waitingFor(Wait.forListeningPort());
      try
      {
         container.start();
         String address = "mongodb://" + container.getHost() + ":" + container.getMappedPort(27017) + "/?directConnection=true";
         try(MongoClient bootstrap = MongoClients.create(address))
         {
            bootstrap.getDatabase("admin").runCommand(new Document("createUser", USERNAME).append("pwd", PASSWORD)
               .append("roles", List.of(new Document("role", "root").append("db", "admin"))));
         }
         client = MongoClients.create(MongoClientSettings.builder().applyConnectionString(new ConnectionString(address))
            .credential(MongoCredential.createCredential(USERNAME, "admin", PASSWORD.toCharArray())).build());
         client.getDatabase("admin").runCommand(new Document("ping", 1));
      }
      catch(RuntimeException failure)
      {
         stopMongo();
         throw failure;
      }
   }



   /*******************************************************************************
    ** Close the native oracle before stopping its disposable server.
    ******************************************************************************/
   @AfterAll
   static void stopMongo()
   {
      try
      {
         if(client != null)
         {
            client.close();
            client = null;
         }
      }
      finally
      {
         if(container != null)
         {
            container.stop();
            container = null;
         }
      }
   }



   /*******************************************************************************
    ** Reuse the canonical sample table and only replace its Mongo key mapping.
    ******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      QTableMetaData mongoTable = instance.getTable(TABLE).clone();
      mongoTable.getField("id").setType(QFieldType.STRING);
      mongoTable.getField("id").setBackendName("_id");
      mongoTable.setBackendName(BACKEND);
      mongoTable.setBackendDetails(new MongoDBTableBackendDetails().withTableName(COLLECTION));
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(instance.getTables());
      tables.put(TABLE, mongoTable);
      instance.setTables(tables);
      instance.addBackend(new MongoDBBackendMetaData().withName(BACKEND).withHost(container.getHost()).withPort(container.getMappedPort(27017))
         .withDatabaseName(DATABASE).withUsername(USERNAME).withPassword(PASSWORD).withAuthSourceDatabase("admin")
         .withUrlSuffix("directConnection=true").withTransactionsSupported(true));
      instance.setHasBeenValidated(null);
      new QInstanceValidator().validate(instance);
      client.getDatabase(DATABASE).drop();
      client.getDatabase(DATABASE).createCollection(COLLECTION);
   }



   /*******************************************************************************
    ** A fresh generated database leaves no persistent fixture data behind.
    ******************************************************************************/
   @AfterEach
   void tearDown()
   {
      try
      {
         client.getDatabase(DATABASE).drop();
      }
      finally
      {
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** A batch is visible to its owning transaction before commit, but native
    ** reads outside that session see both documents only after commit.
    ******************************************************************************/
   @Test
   void testCommitPublishesBatchOnlyAfterCommit() throws Exception
   {
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE)))
      {
         List<QRecord> records = new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction)
            .withRecords(List.of(new QRecord().withValue("name", "First"), new QRecord().withValue("name", "Second")))).getRecords();
         assertEquals(2, records.size());
         assertTrue(records.stream().allMatch(record -> record.getErrors().isEmpty()));
         assertEquals(0L, collection().countDocuments());
         for(QRecord record : records)
         {
            assertNotNull(new GetAction().execute(new GetInput(TABLE).withTransaction(transaction)
               .withPrimaryKey(record.getValueString("id"))).getRecord());
         }
         transaction.commit();
         assertEquals(List.of("First", "Second"), nativeNames());
      }
   }



   /*******************************************************************************
    ** A rollback removes an uncommitted sample write and the same transaction
    ** can then commit a new write without leaking the first one.
    ******************************************************************************/
   @Test
   void testRollbackAndReusePreserveOnlyCommittedNativeRow() throws Exception
   {
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE)))
      {
         new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction)
            .withRecord(new QRecord().withValue("name", "Rolled back")));
         assertEquals(0L, collection().countDocuments());
         transaction.rollback();
         assertEquals(0L, collection().countDocuments());
         new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction)
            .withRecord(new QRecord().withValue("name", "Committed")));
         transaction.commit();
      }
      assertEquals(List.of("Committed"), nativeNames());
   }



   /*******************************************************************************
    ** A duplicate key aborts the whole replica-set transaction, unlike the
    ** standalone partial-write behavior in SampleMongoDatabaseIT.
    ******************************************************************************/
   @Test
   void testDuplicateMidBatchRollsBackAllNativeWrites() throws Exception
   {
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE)))
      {
         InsertInput batch = new InsertInput(TABLE).withTransaction(transaction).withSkipUniqueKeyCheck(true)
            .withRecords(List.of(
               new QRecord().withValue("id", "same-key").withValue("name", "First"),
               new QRecord().withValue("id", "same-key").withValue("name", "Duplicate")));
         assertThrows(QException.class, () -> new InsertAction().execute(batch));
         transaction.rollback();
         assertEquals(0L, collection().countDocuments());
         new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction)
            .withRecord(new QRecord().withValue("name", "Recovered")));
         transaction.commit();
      }
      assertEquals(List.of("Recovered"), nativeNames());
   }



   /*******************************************************************************
    ** Mongo's failCommand blocks only the QQQ aggregate operation, allowing a
    ** separate native client to observe post-timeout cleanup and recovery.
    ** This does not prove the configured deadline stops server execution.
    ******************************************************************************/
   @Test
   void testTimedOutQueryCleansUpAndFreshActionRecovers() throws Exception
   {
      MongoDBBackendMetaData backend = (MongoDBBackendMetaData) QContext.getQInstance().getBackend(BACKEND);
      String originalSuffix = backend.getUrlSuffix();
      new InsertAction().execute(new InsertInput(TABLE).withRecord(new QRecord().withValue("name", "Survivor")));
      assertEquals(List.of("Survivor"), nativeNames());

      String applicationName = "qqqTimeout-" + UUID.randomUUID();
      QInstance instance = QContext.getQInstance();
      backend.setUrlSuffix("directConnection=true&appName=" + applicationName);
      Document enabled = client.getDatabase("admin").runCommand(new Document("configureFailPoint", "failCommand")
         .append("mode", new Document("times", 1))
         .append("data", new Document("failCommands", List.of("aggregate")).append("appName", applicationName)
            .append("blockConnection", true).append("blockTimeMS", 2500)));
      try(var executor = Executors.newSingleThreadExecutor())
      {
         try
         {
            var query = executor.submit(() ->
            {
               QContext.init(instance, new QSession());
               try
               {
                  QUserFacingException failure = assertThrows(QUserFacingException.class, () ->
                     new QueryAction().execute(new QueryInput(TABLE).withTimeoutSeconds(1)));
                  assertTrue(failure.getMessage().toLowerCase().contains("timed out"));
               }
               finally
               {
                  QContext.clear();
               }
            });
            List<Document> entered = awaitOperations(new Document("appName", applicationName).append("command.aggregate", COLLECTION), rows -> !rows.isEmpty());
            assertEquals(1, entered.size());
            Object operationId = entered.getFirst().get("opid");
            assertNotNull(operationId);
            assertTrue(entered.getFirst().getBoolean("active"));
            client.getDatabase("admin").runCommand(new Document("waitForFailPoint", "failCommand")
               .append("timesEntered", enabled.getInteger("count") + 1).append("maxTimeMS", 1000));
            query.get(10, TimeUnit.SECONDS);
            assertTrue(awaitOperations(new Document("opid", operationId), List::isEmpty).isEmpty());
            assertTrue(awaitOperations(new Document("appName", applicationName), List::isEmpty).isEmpty());
         }
         finally
         {
            client.getDatabase("admin").runCommand(new Document("configureFailPoint", "failCommand").append("mode", "off"));
            backend.setUrlSuffix(originalSuffix);
         }
      }
      assertEquals(List.of("Survivor"), nativeNames());
      assertEquals(1, new QueryAction().execute(new QueryInput(TABLE)).getRecords().size());
   }



   /*******************************************************************************
    ** Cooperative cancellation during delivery closes the real server cursor and
    ** owned client, while a fresh query can still return the complete native set.
    ******************************************************************************/
   @Test
   void testCooperativeCancellationClosesNativeCursorAndClient() throws Exception
   {
      List<QRecord> records = new ArrayList<>();
      for(int i = 0; i < 200; i++)
      {
         records.add(new QRecord().withValue("name", "Owned row " + i));
      }
      new InsertAction().execute(new InsertInput(TABLE).withRecords(records));
      assertEquals(200L, collection().countDocuments());
      QInstance instance = QContext.getQInstance();
      MongoDBBackendMetaData backend = (MongoDBBackendMetaData) instance.getBackend(BACKEND);
      String applicationName = "qqqCursor-" + UUID.randomUUID();
      backend.setUrlSuffix("directConnection=true&appName=" + applicationName);
      AsyncJobStatus status = new AsyncJobStatus();
      CountDownLatch entered = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);
      QueryInput input = new QueryInput(TABLE);
      input.setAsyncJobCallback(new NonPersistedAsyncJobCallback(UUID.randomUUID(), status)
      {
         /*******************************************************************************
          ** The normal callback is reached after the first native result is read.
          ******************************************************************************/
         @Override
         public boolean wasCancelRequested()
         {
            entered.countDown();
            try
            {
               assertTrue(release.await(10, TimeUnit.SECONDS));
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
               throw new AssertionError(e);
            }
            return super.wasCancelRequested();
         }
      });
      Document cursorFilter = new Document("type", "idleCursor").append("ns", DATABASE + "." + COLLECTION);
      assertTrue(awaitOperations(cursorFilter, List::isEmpty).isEmpty());
      try(var executor = Executors.newSingleThreadExecutor())
      {
         try
         {
            var query = executor.submit(() ->
            {
               QContext.init(instance, new QSession());
               try
               {
                  return new QueryAction().execute(input);
               }
               finally
               {
                  QContext.clear();
               }
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            List<Document> cursors = awaitOperations(cursorFilter, rows -> !rows.isEmpty());
            assertEquals(1, cursors.size());
            Object cursorId = cursors.getFirst().get("cursor", Document.class).get("cursorId");
            assertNotNull(cursorId);
            assertFalse(awaitOperations(new Document("appName", applicationName), rows -> !rows.isEmpty()).isEmpty());
            status.setCancelRequested(true);
            release.countDown();
            assertEquals(1, query.get(5, TimeUnit.SECONDS).getRecords().size());
            assertTrue(awaitOperations(new Document("cursor.cursorId", cursorId), List::isEmpty).isEmpty());
            assertTrue(awaitOperations(new Document("appName", applicationName), List::isEmpty).isEmpty());
         }
         finally
         {
            release.countDown();
         }
      }
      assertEquals(200, new QueryAction().execute(new QueryInput(TABLE)).getRecords().size());
      assertTrue(awaitOperations(new Document("appName", applicationName), List::isEmpty).isEmpty());
   }



   /*******************************************************************************
    ** Closing an uncommitted transaction releases its session and client, and
    ** a subsequent action can still use the server without seeing its write.
    ******************************************************************************/
   @Test
   void testCloseDiscardsUncommittedWriteAndReleasesOwnedClient() throws Exception
   {
      String applicationName = "qqqAbort-" + UUID.randomUUID();
      ((MongoDBBackendMetaData) QContext.getQInstance().getBackend(BACKEND)).setUrlSuffix("directConnection=true&appName=" + applicationName);
      Document applicationFilter = new Document("appName", applicationName);
      MongoClient transactionClient;
      Document sessionFilter;
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE)))
      {
         MongoDBTransaction mongo = assertInstanceOf(MongoDBTransaction.class, transaction);
         transactionClient = mongo.getMongoClient();
         new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction)
            .withRecord(new QRecord().withValue("name", "Uncommitted")));
         assertEquals(0L, collection().countDocuments());
         var sessionId = mongo.getClientSession().getServerSession().getIdentifier().getBinary("id");
         sessionFilter = new Document("lsid.id", new Binary(sessionId.getType(), sessionId.getData())).append("type", "idleSession");
         assertEquals(1, awaitOperations(sessionFilter, rows -> rows.size() == 1).size());
         assertFalse(awaitOperations(applicationFilter, rows -> !rows.isEmpty()).isEmpty());
      }
      assertThrows(IllegalStateException.class, () -> transactionClient.getDatabase("admin").runCommand(new Document("ping", 1)));
      assertTrue(awaitOperations(sessionFilter, List::isEmpty).isEmpty());
      assertTrue(awaitOperations(applicationFilter, List::isEmpty).isEmpty());
      assertEquals(0L, collection().countDocuments());
      new InsertAction().execute(new InsertInput(TABLE).withRecord(new QRecord().withValue("name", "Fresh client")));
      assertEquals(List.of("Fresh client"), nativeNames());
   }



   /*******************************************************************************
    ** Direct native commands establish owned closure and borrowed usability.
    ** The transaction, rather than the borrowed action, closes its client.
    ******************************************************************************/
   @Test
   void testOwnedAndBorrowedClientHandlesFollowTransactionOwnership() throws Exception
   {
      MongoDBBackendMetaData backend = (MongoDBBackendMetaData) QContext.getQInstance().getBackend(BACKEND);
      AbstractMongoDBAction action = new AbstractMongoDBAction();
      String applicationName = "qqqOwnership-" + UUID.randomUUID();
      Document applicationFilter = new Document("appName", applicationName);
      backend.setUrlSuffix("directConnection=true&appName=" + applicationName);
      assertTrue(awaitOperations(applicationFilter, List::isEmpty).isEmpty());
      MongoClientContainer owned = action.openClient(backend, null);
      MongoClient ownedClient = owned.getMongoClient();
      try
      {
         assertEquals(1.0, ownedClient.getDatabase("admin").runCommand(owned.getMongoSession(), new Document("ping", 1)).getDouble("ok"));
         assertFalse(awaitOperations(applicationFilter, rows -> !rows.isEmpty()).isEmpty());
         assertNotNull(owned.getMongoSession().getServerSession());
      }
      finally
      {
         owned.closeIfNeeded();
      }
      assertThrows(IllegalStateException.class, () -> ownedClient.getDatabase("admin").runCommand(new Document("ping", 1)));
      assertThrows(IllegalStateException.class, () -> owned.getMongoSession().getServerSession());
      assertTrue(awaitOperations(applicationFilter, List::isEmpty).isEmpty());

      MongoClient transactionClient;
      Document sessionFilter;
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE)))
      {
         MongoDBTransaction mongo = assertInstanceOf(MongoDBTransaction.class, transaction);
         transactionClient = mongo.getMongoClient();
         MongoClientContainer borrowed = action.openClient(backend, transaction);
         assertSame(transactionClient, borrowed.getMongoClient());
         assertSame(mongo.getClientSession(), borrowed.getMongoSession());
         new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction)
            .withRecord(new QRecord().withValue("name", "Borrowed owner")));
         var sessionId = mongo.getClientSession().getServerSession().getIdentifier().getBinary("id");
         sessionFilter = new Document("lsid.id", new Binary(sessionId.getType(), sessionId.getData())).append("type", "idleSession");
         assertEquals(1, awaitOperations(sessionFilter, rows -> rows.size() == 1).size());
         List<Object> connections = awaitOperations(applicationFilter, rows -> !rows.isEmpty()).stream()
            .filter(row -> row.containsKey("connectionId")).map(row -> row.get("connectionId")).distinct().toList();
         assertFalse(connections.isEmpty());
         borrowed.closeIfNeeded();
         assertSame(mongo.getClientSession().getServerSession(), borrowed.getMongoSession().getServerSession());
         assertEquals(1, awaitOperations(sessionFilter, rows -> rows.size() == 1).size());
         assertTrue(awaitOperations(applicationFilter, rows -> !rows.isEmpty()).stream()
            .map(row -> row.get("connectionId")).toList().containsAll(connections));
         transaction.commit();
         assertTrue(awaitOperations(sessionFilter, List::isEmpty).isEmpty());
         assertFalse(awaitOperations(applicationFilter, rows -> !rows.isEmpty()).isEmpty());
      }
      assertThrows(IllegalStateException.class, () -> transactionClient.getDatabase("admin").runCommand(new Document("ping", 1)));
      assertTrue(awaitOperations(sessionFilter, List::isEmpty).isEmpty());
      assertTrue(awaitOperations(applicationFilter, List::isEmpty).isEmpty());
      assertEquals(List.of("Borrowed owner"), nativeNames());
      assertEquals(1, new QueryAction().execute(new QueryInput(TABLE)).getRecords().size());
      assertTrue(awaitOperations(applicationFilter, List::isEmpty).isEmpty());
   }



   /*******************************************************************************
    ** Observe only correlated fixture resources, including idle native handles.
    ******************************************************************************/
   private static List<Document> awaitOperations(Document filter, Predicate<List<Document>> ready) throws InterruptedException
   {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      List<Document> operations;
      do
      {
         operations = client.getDatabase("admin").aggregate(List.of(
            new Document("$currentOp", new Document("allUsers", true).append("idleConnections", true)
               .append("idleCursors", true).append("idleSessions", true)),
            new Document("$match", filter))).into(new ArrayList<>());
         if(ready.test(operations))
         {
            return operations;
         }
         Thread.sleep(25);
      }
      while(System.nanoTime() < deadline);
      assertTrue(ready.test(operations), "Correlated native operations did not reach expected state; count=" + operations.size());
      return operations;
   }



   /*******************************************************************************
    ** The native client is the oracle; no QQQ query conversion is used here.
    ******************************************************************************/
   private static MongoCollection<Document> collection()
   {
      return client.getDatabase(DATABASE).getCollection(COLLECTION);
   }



   /*******************************************************************************
    ** Return native names in deterministic lexical order.
    ******************************************************************************/
   private static List<String> nativeNames()
   {
      return collection().find().map(row -> row.getString("name")).into(new ArrayList<>()).stream().sorted().toList();
   }
}
