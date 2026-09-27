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


import java.time.Duration;
import com.github.dockerjava.api.command.InspectContainerResponse;
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
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;


/*******************************************************************************
 ** Base for all tests in this module
 *******************************************************************************/
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(BaseTest.class);

   private GenericContainer<?> mongoDBContainer;
   private String mongoDiagnostics = "";
   private final StringBuffer nativeMongoLog = new StringBuffer();

   private static final String MONGO_IMAGE = "mongo:7.0";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeAll
   void beforeAll()
   {
      System.setProperty("qqq.mongodb.logQueries", "true");

      mongoDBContainer = createMongoContainer();
      try
      {
         mongoDBContainer.start();
      }
      catch(RuntimeException | Error failure)
      {
         if(mongoDiagnostics.isEmpty())
         {
            captureMongoDiagnostics(mongoDBContainer);
         }
         throw failure;
      }
   }



   /*******************************************************************************
    ** Allow lifecycle regressions to install owned initialization scripts.
    *******************************************************************************/
   protected GenericContainer<?> createMongoContainer()
   {
      return new MongoFixtureContainer()
         .withEnv("MONGO_INITDB_ROOT_USERNAME", TestUtils.MONGO_USERNAME)
         .withEnv("MONGO_INITDB_ROOT_PASSWORD", TestUtils.MONGO_PASSWORD)
         .withEnv("MONGO_INITDB_DATABASE", TestUtils.MONGO_DATABASE)
         .withExposedPorts(TestUtils.MONGO_PORT)
         .withLogConsumer(frame -> nativeMongoLog.append(redactMongoCredentials(frame.getUtf8String())))
         .waitingFor(new WaitAllStrategy()
            .withStrategy(Wait.forLogMessage(".*MongoDB init process complete; ready for start up.*", 1))
            .withStrategy(Wait.forListeningPort())
            .withStrategy(Wait.forSuccessfulCommand("""
               mongosh --nodb --quiet --eval '
               const uri = "mongodb://" + encodeURIComponent(process.env.MONGO_INITDB_ROOT_USERNAME) + ":" +
                  encodeURIComponent(process.env.MONGO_INITDB_ROOT_PASSWORD) + "@127.0.0.1:27017/admin?serverSelectionTimeoutMS=1000&connectTimeoutMS=1000";
               const admin = new Mongo(uri).getDB("admin");
               if (admin.runCommand({connectionStatus: 1}).authInfo.authenticatedUsers.length !== 1 || admin.runCommand({ping: 1}).ok !== 1) quit(1);
               '
               """))
            .withStartupTimeout(Duration.ofSeconds(60)));
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
      try
      {
         clearDatabase();
      }
      catch(RuntimeException | Error failure)
      {
         captureMongoDiagnostics(mongoDBContainer);
         throw failure;
      }
      finally
      {
         QContext.clear();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected static void clearDatabase()
   {
      ///////////////////////////////////////
      // clear test database between tests //
      ///////////////////////////////////////
      try(MongoClient mongoClient = getMongoClient())
      {
         MongoDatabase database = mongoClient.getDatabase(TestUtils.MONGO_DATABASE);
         database.drop();
      }
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
   void afterAll()
   {
      if(mongoDBContainer != null)
      {
         if(mongoDBContainer.getContainerId() != null && !mongoDBContainer.isRunning())
         {
            captureMongoDiagnostics(mongoDBContainer);
         }
         mongoDBContainer.close();
      }
   }



   /*******************************************************************************
    ** Keep diagnostics inside the startup failure boundary, before automatic removal.
    *******************************************************************************/
   private class MongoFixtureContainer extends GenericContainer<MongoFixtureContainer>
   {
      private boolean started;



      /*******************************************************************************
       ** Preserve the existing fixture image and container configuration.
       *******************************************************************************/
      private MongoFixtureContainer()
      {
         super(DockerImageName.parse(MONGO_IMAGE));
      }



      /*******************************************************************************
       ** Capture state while the failed container still exists.
       *******************************************************************************/
      @Override
      protected void containerIsStopping(InspectContainerResponse containerInfo)
      {
         if(!started)
         {
            captureMongoDiagnostics(this);
         }
         super.containerIsStopping(containerInfo);
      }



      /*******************************************************************************
       ** Normal teardown does not need startup-failure diagnostics.
       *******************************************************************************/
      @Override
      protected void containerIsStarted(InspectContainerResponse containerInfo)
      {
         super.containerIsStarted(containerInfo);
         started = true;
      }
   }



   /*******************************************************************************
    ** Preserve native startup output and selected state, never Docker Config/Env.
    ** Diagnostic failure must not replace the original startup or cleanup error.
    *******************************************************************************/
   private void captureMongoDiagnostics(GenericContainer<?> container)
   {
      if(container == null)
      {
         return;
      }
      StringBuilder evidence = new StringBuilder("Owned Mongo fixture diagnostics");
      try
      {
         InspectContainerResponse info = container.getDockerClient().inspectContainerCmd(container.getContainerId()).exec();
         var state = info.getState();
         evidence.append(" container=").append(info.getId()).append(" status=").append(state.getStatus())
            .append(" exitCode=").append(state.getExitCodeLong()).append(" oomKilled=").append(state.getOOMKilled())
            .append(" startedAt=").append(state.getStartedAt()).append(" finishedAt=").append(state.getFinishedAt());
      }
      catch(RuntimeException failure)
      {
         evidence.append(" stateUnavailable=").append(failure.getClass().getSimpleName());
      }
      try
      {
         evidence.append('\n').append(redactMongoCredentials(container.getLogs()));
      }
      catch(RuntimeException failure)
      {
         evidence.append("\nCaptured startup stream:\n").append(nativeMongoLog);
      }
      mongoDiagnostics = evidence.toString();
      LOG.warn(mongoDiagnostics);
   }



   /*******************************************************************************
    ** Even synthetic fixture authentication values do not belong in diagnostics.
    *******************************************************************************/
   private String redactMongoCredentials(String text)
   {
      return text.replace(TestUtils.MONGO_USERNAME, "[fixture-user]").replace(TestUtils.MONGO_PASSWORD, "[fixture-secret]");
   }



   /*******************************************************************************
    ** Failure evidence is also retained in the ordinary test output artifact.
    *******************************************************************************/
   String getMongoDiagnostics()
   {
      return mongoDiagnostics;
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
