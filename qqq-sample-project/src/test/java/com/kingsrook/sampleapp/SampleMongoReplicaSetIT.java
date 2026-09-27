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
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
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
    ** separate native client to prove that timeout cancellation is recoverable.
    ******************************************************************************/
   @Test
   void testTimedOutQueryCancelsAndFreshActionRecovers() throws Exception
   {
      MongoDBBackendMetaData backend = (MongoDBBackendMetaData) QContext.getQInstance().getBackend(BACKEND);
      String originalSuffix = backend.getUrlSuffix();
      new InsertAction().execute(new InsertInput(TABLE).withRecord(new QRecord().withValue("name", "Survivor")));
      assertEquals(List.of("Survivor"), nativeNames());

      backend.setUrlSuffix("directConnection=true&appName=qqqMongoTimeoutProbe");
      Document enabled = client.getDatabase("admin").runCommand(new Document("configureFailPoint", "failCommand")
         .append("mode", new Document("times", 1))
         .append("data", new Document("failCommands", List.of("aggregate")).append("appName", "qqqMongoTimeoutProbe")
            .append("blockConnection", true).append("blockTimeMS", 2500)));
      try
      {
         QUserFacingException failure = assertThrows(QUserFacingException.class, () ->
            new QueryAction().execute(new QueryInput(TABLE).withTimeoutSeconds(1)));
         assertTrue(failure.getMessage().toLowerCase().contains("timed out"));
         client.getDatabase("admin").runCommand(new Document("waitForFailPoint", "failCommand")
            .append("timesEntered", enabled.getInteger("count") + 1).append("maxTimeMS", 1000));
      }
      finally
      {
         client.getDatabase("admin").runCommand(new Document("configureFailPoint", "failCommand").append("mode", "off"));
         backend.setUrlSuffix(originalSuffix);
      }
      assertEquals(List.of("Survivor"), nativeNames());
      assertEquals(1, new QueryAction().execute(new QueryInput(TABLE)).getRecords().size());
   }



   /*******************************************************************************
    ** Closing an uncommitted transaction releases its session and client, and
    ** a subsequent action can still use the server without seeing its write.
    ******************************************************************************/
   @Test
   void testCloseDiscardsUncommittedWriteAndReleasesOwnedClient() throws Exception
   {
      MongoClient transactionClient;
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE)))
      {
         MongoDBTransaction mongo = assertInstanceOf(MongoDBTransaction.class, transaction);
         transactionClient = mongo.getMongoClient();
         new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction)
            .withRecord(new QRecord().withValue("name", "Uncommitted")));
         assertEquals(0L, collection().countDocuments());
      }
      assertThrows(IllegalStateException.class, () -> transactionClient.getDatabase("admin").runCommand(new Document("ping", 1)));
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
      MongoClientContainer owned = action.openClient(backend, null);
      MongoClient ownedClient = owned.getMongoClient();
      assertEquals(1.0, ownedClient.getDatabase("admin").runCommand(owned.getMongoSession(), new Document("ping", 1)).getDouble("ok"));
      owned.closeIfNeeded();
      assertThrows(IllegalStateException.class, () -> ownedClient.getDatabase("admin").runCommand(new Document("ping", 1)));

      MongoClient transactionClient;
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE)))
      {
         MongoDBTransaction mongo = assertInstanceOf(MongoDBTransaction.class, transaction);
         transactionClient = mongo.getMongoClient();
         MongoClientContainer borrowed = action.openClient(backend, transaction);
         assertSame(transactionClient, borrowed.getMongoClient());
         assertSame(mongo.getClientSession(), borrowed.getMongoSession());
         borrowed.closeIfNeeded();
         new InsertAction().execute(new InsertInput(TABLE).withTransaction(transaction)
            .withRecord(new QRecord().withValue("name", "Borrowed owner")));
         transaction.commit();
      }
      assertThrows(IllegalStateException.class, () -> transactionClient.getDatabase("admin").runCommand(new Document("ping", 1)));
      assertEquals(List.of("Borrowed owner"), nativeNames());
      assertEquals(1, new QueryAction().execute(new QueryInput(TABLE)).getRecords().size());
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
