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

package com.kingsrook.qqq.middleware.javalin.health;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.health.HealthCheckMetaData;
import com.kingsrook.qqq.backend.core.health.HealthCheckOutcome;
import com.kingsrook.qqq.backend.core.health.HealthChecksMetaData;
import com.kingsrook.qqq.backend.core.health.HealthDetails;
import com.kingsrook.qqq.backend.core.health.HealthMetaDataProvider;
import com.kingsrook.qqq.backend.core.health.HealthMonitoringMetaData;
import com.kingsrook.qqq.backend.core.health.HealthRunner;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiInProcessSessions;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Exercise the real extension through authentication, permissions and routing.
 *******************************************************************************/
class HealthEndpointsTest
{
   private static final int PORT = 6289;
   private QApplicationJavalinServer server;
   private QInstance instance;
   private String reader;
   private String writer;
   private String denied;



   /*******************************************************************************
    ** Opt in and seed a real redacted result through the runner.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      MemoryRecordStore.getInstance().reset();
      instance = TestUtils.defineInstance().withEnforcePermissionsInCoreActions(true);
      new HealthMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      HealthMonitoringMetaData.ofOrWithNew(instance).withRunnerEnabled(false);
      HealthChecksMetaData.ofOrWithNew(instance).withCheck(new HealthCheckMetaData().withName("test.health").withComponent("test")
         .withCheckInstance(input -> HealthCheckOutcome.up("password=sentinel").withDetails(new HealthDetails().withValue("password", "sentinel"))));
      startServer();
      try(HealthRunner runner = new HealthRunner(instance, "test", Clock.systemUTC()))
      {
         runner.runNow("test.health");
      }
      reader = token("health.read");
      writer = token("health.write");
      denied = token("unrelated.read");
   }



   /*******************************************************************************
    ** Start a fresh server around the same metadata for lifecycle assertions.
    *******************************************************************************/
   private void startServer() throws QException
   {
      server = new QApplicationJavalinServer(new AbstractQQQApplication()
      {
         /***********************************************************************
          ** This test owns a single instance throughout server startup.
          ***********************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return (instance);
         }
      }).withPort(PORT).withServeFrontendNext(false).withServeFrontendMaterialDashboard(false).withServeLegacyUnversionedMiddlewareAPI(false);
      server.start();
   }



   /*******************************************************************************
    ** Stop the server and revoke every test credential.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      if(server != null)
      {
         server.stop();
      }
      for(String token : new String[] { reader, writer, denied })
      {
         if(token != null)
         {
            UiApiInProcessSessions.revoke(token);
         }
      }
      QContext.clear();
      Unirest.config().reset();
   }



   /*******************************************************************************
    ** Every endpoint serves authorized users and refuses missing permissions.
    *******************************************************************************/
   @Test
   void authorizedAndForbiddenEndpoints()
   {
      for(String path : List.of("summary", "components/test", "checks/test.health/history"))
      {
         var response = Unirest.get(url(path)).header(UiApiInProcessSessions.HEADER, reader).asString();
         assertThat(response.getStatus()).as(path).isEqualTo(200);
         assertThat(response.getBody()).doesNotContain("sentinel");
         assertThat(Unirest.get(url(path)).header(UiApiInProcessSessions.HEADER, denied).asString().getStatus()).as(path).isEqualTo(403);
      }
      var run = Unirest.post(url("checks/test.health/run")).header(UiApiInProcessSessions.HEADER, writer).asString();
      assertThat(run.getStatus()).isEqualTo(200);
      assertThat(run.getBody()).contains("UP", "[redacted]").doesNotContain("sentinel");
      assertThat(Unirest.post(url("checks/test.health/run")).header(UiApiInProcessSessions.HEADER, reader).asString().getStatus()).isEqualTo(403);
   }



   /*******************************************************************************
    ** Unknown entities are 404 after permission checking, never a successful empty result.
    *******************************************************************************/
   @Test
   void unknownNames()
   {
      for(String path : List.of("components/missing", "checks/missing/history"))
      {
         assertThat(Unirest.get(url(path)).header(UiApiInProcessSessions.HEADER, reader).asString().getStatus()).isEqualTo(404);
      }
      assertThat(Unirest.post(url("checks/missing/run")).header(UiApiInProcessSessions.HEADER, writer).asString().getStatus()).isEqualTo(404);
   }



   /*******************************************************************************
    ** More than 500 stored rows cannot bypass the history cap; bounds are validated.
    *******************************************************************************/
   @Test
   void historyLimitAndBounds() throws Exception
   {
      QContext.init(instance, new QSession().withPermissions("health.read", "health.write"));
      var response = Unirest.get(url("checks/test.health/history")).header(UiApiInProcessSessions.HEADER, reader).asString();
      int resultId = new JSONObject(response.getBody()).getJSONArray("results").getJSONObject(0).getInt("id");
      var getInput = new GetInput().withPrimaryKey(resultId);
      getInput.setTableName(HealthMetaDataProvider.RESULT_TABLE);
      var row = new GetAction().execute(getInput).getRecord();
      List<QRecord> records = new ArrayList<>();
      for(int i = 0; i < 510; i++)
      {
         records.add(new QRecord().withValue("healthCheckId", row.getValue("healthCheckId")).withValue("slot", i)
            .withValue("onDemandRequestId", "").withValue("status", "UP").withValue("startedAt", Instant.now().plusSeconds(i)));
      }
      new InsertAction().execute(new InsertInput().withTableName(HealthMetaDataProvider.RESULT_TABLE).withRecords(records));
      response = Unirest.get(url("checks/test.health/history?limit=900")).header(UiApiInProcessSessions.HEADER, reader).asString();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(new JSONObject(response.getBody()).getJSONArray("results").length()).isEqualTo(500);
      for(String query : List.of("limit=0", "limit=nope", "from=bad", "from=2026-10-15T00:00:00Z&to=2026-10-14T00:00:00Z"))
      {
         assertThat(Unirest.get(url("checks/test.health/history?" + query)).header(UiApiInProcessSessions.HEADER, reader).asString().getStatus()).as(query).isEqualTo(400);
      }
   }



   /*******************************************************************************
    ** Runtime service owns start/stop and honors opt-out.
    *******************************************************************************/
   @Test
   void runtimeLifecycle() throws Exception
   {
      HealthRuntimeService service = new HealthRuntimeService();
      service.start(instance);
      assertThat(HealthRunner.getForInstance(instance)).isNull();
      HealthMonitoringMetaData.ofOrWithNew(instance).withRunnerEnabled(true);
      service.start(instance);
      assertThat(HealthRunner.getForInstance(instance)).isNotNull();
      service.start(instance);
      service.stop();
      assertThat(HealthRunner.getForInstance(instance)).isNull();
   }



   /*******************************************************************************
    ** QApplicationJavalinServer starts and stops an opted-in runner itself.
    *******************************************************************************/
   @Test
   void serverOwnsRunner() throws Exception
   {
      server.stop();
      HealthMonitoringMetaData.ofOrWithNew(instance).withRunnerEnabled(true);
      startServer();
      assertThat(HealthRunner.getForInstance(instance)).isNotNull();
      server.stop();
      assertThat(HealthRunner.getForInstance(instance)).isNull();
   }



   /*******************************************************************************
    ** Local in-process sessions exercise the normal spec session setup.
    *******************************************************************************/
   private String token(String... permissions)
   {
      return (UiApiInProcessSessions.register(new QSession().withUser(new QUser().withIdReference("health-test")).withPermissions(permissions), Duration.ofMinutes(5)));
   }



   /*******************************************************************************
    ** Versioned extension URL.
    *******************************************************************************/
   private String url(String path)
   {
      return ("http://127.0.0.1:" + PORT + "/qqq/v1/health/" + path);
   }
}
