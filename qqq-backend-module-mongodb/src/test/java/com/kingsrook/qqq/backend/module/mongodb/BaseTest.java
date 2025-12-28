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

package com.kingsrook.qqq.backend.module.mongodb;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.mongodb.actions.AbstractMongoDBAction;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoClientContainer;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;


/*******************************************************************************
 ** Base for all tests in this module
 *******************************************************************************/
public class BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(BaseTest.class);

   private static GenericContainer<?> mongoDBContainer;

   private static final String MONGO_IMAGE = "mongo:6.0";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeAll
   static void beforeAll()
   {
      System.setProperty("qqq.mongodb.logQueries", "true");

      mongoDBContainer = new GenericContainer<>(DockerImageName.parse(MONGO_IMAGE))
         .withEnv("MONGO_INITDB_ROOT_USERNAME", TestUtils.MONGO_USERNAME)
         .withEnv("MONGO_INITDB_ROOT_PASSWORD", TestUtils.MONGO_PASSWORD)
         .withEnv("MONGO_INITDB_DATABASE", TestUtils.MONGO_DATABASE)
         .withExposedPorts(TestUtils.MONGO_PORT);

      mongoDBContainer.start();
   }



   /*******************************************************************************
    ** init the QContext with the instance from TestUtils and a new session
    *******************************************************************************/
   @BeforeEach
   void baseBeforeEach()
   {
      QContext.init(TestUtils.defineInstance(), new QSession());

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // host could(?) be different, and mapped port will be, so set them in backend meta-data based on our running container //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      MongoDBBackendMetaData backend = (MongoDBBackendMetaData) QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      backend.setHost(mongoDBContainer.getHost());
      backend.setPort(mongoDBContainer.getMappedPort(TestUtils.MONGO_PORT));
   }



   /*******************************************************************************
    ** clear the QContext
    *******************************************************************************/
   @AfterEach
   void baseAfterEach()
   {
      clearDatabase();

      QContext.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected static void clearDatabase()
   {
      ///////////////////////////////////////
      // clear test database between tests //
      ///////////////////////////////////////
      MongoClient   mongoClient = getMongoClient();
      MongoDatabase database    = mongoClient.getDatabase(TestUtils.MONGO_DATABASE);
      database.drop();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected static MongoClient getMongoClient()
   {
      MongoDBBackendMetaData backend              = (MongoDBBackendMetaData) QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      MongoClientContainer   mongoClientContainer = new AbstractMongoDBAction().openClient(backend, null);
      MongoClient            mongoClient          = mongoClientContainer.getMongoClient();
      return mongoClient;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterAll
   static void afterAll()
   {
      // this.mongoDbReplicaSet.close();
      mongoDBContainer.close();
   }



   /*******************************************************************************
    ** if needed, re-initialize the QInstance in context.
    *******************************************************************************/
   protected static void reInitInstanceInContext(QInstance qInstance)
   {
      if(qInstance.equals(QContext.getQInstance()))
      {
         LOG.warn("Unexpected condition - the same qInstance that is already in the QContext was passed into reInit.  You probably want a new QInstance object instance.");
      }
      QContext.init(qInstance, new QSession());
   }

}
