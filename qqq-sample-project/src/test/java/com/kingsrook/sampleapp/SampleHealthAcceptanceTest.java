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

package com.kingsrook.sampleapp;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.middleware.health.HealthIndicator;
import com.kingsrook.qqq.middleware.health.indicators.DatabaseHealthIndicator;
import com.kingsrook.qqq.middleware.health.indicators.DiskSpaceHealthIndicator;
import com.kingsrook.qqq.middleware.health.indicators.MemoryHealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;
import com.kingsrook.sampleapp.healthfixture.SampleHealthMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.h2.tools.Server;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** First-party sample health contracts through autoloaded routes and owned data.
 *******************************************************************************/
class SampleHealthAcceptanceTest
{
   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final BlockingQueue<List<Boolean>> completedRequests = new LinkedBlockingQueue<>();
   private final Set<Thread> workers = ConcurrentHashMap.newKeySet();
   private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
   private QInstance instance;
   private HealthCheckMetaData health;
   private SampleJavalinServer server;

   @TempDir
   Path diskDirectory;



   /*******************************************************************************
    ** Load the producer via the same package scanner used by the sample.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      instance = SampleMetaDataProvider.defineTestInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(instance, SampleHealthMetaDataProducer.class.getPackageName());
      health = QSupplementalInstanceMetaData.of(instance, HealthCheckMetaData.METADATA_KEY);
      assertNotNull(health);
   }



   /*******************************************************************************
    ** Stop only the owned listener, then verify recorded indicator workers ended.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(server != null)
         {
            server.stop();
         }
         assertWorkersStopped();
      }
      finally
      {
         client.close();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** No explicit route-provider registration is needed after metadata scanning.
    *******************************************************************************/
   @Test
   void testAutoloadedDefaultEndpoint() throws Exception
   {
      assertEquals("/health", health.getEndpointPath());
      assertTrue(health.getEnabled());
      health.setIndicators(health.getIndicators().stream().map(this::tracked).toList());
      start();
      JSONObject body = healthResponse("/health", 200, "UP");
      assertTrue(Math.abs(Instant.now().getEpochSecond() - body.getLong("timestamp")) < 5);
      assertEquals(1, body.getJSONObject("checks").length());
      JSONObject alive = body.getJSONObject("checks").getJSONObject(health.getIndicators().get(0).getName());
      assertEquals("UP", alive.getString("status"));
      assertTrue(alive.getLong("durationMs") >= 0);
   }



   /*******************************************************************************
    ** A custom path replaces the default path, including over real HTTP.
    *******************************************************************************/
   @Test
   void testConfiguredEndpointPath() throws Exception
   {
      health.setEndpointPath("/sample-readiness");
      start();
      healthResponse("/sample-readiness", 200, "UP");
      assertEquals(404, get("/health").statusCode());
   }



   /*******************************************************************************
    ** Disabled means no route, rather than an empty successful health response.
    *******************************************************************************/
   @Test
   void testDisabledEndpointIsAbsent() throws Exception
   {
      health.withEndpointPath("/sample-readiness").withEnabled(false);
      start();
      assertEquals(404, get("/sample-readiness").statusCode());
      assertEquals(404, get("/health").statusCode());
   }



   /*******************************************************************************
    ** Preserve each check and choose the worst status without losing details.
    *******************************************************************************/
   @Test
   void testAggregateStatusCustomDetailsAndDuration() throws Exception
   {
      AtomicReference<HealthStatus> status = new AtomicReference<>(HealthStatus.UP);
      health.setIndicators(List.of(indicator("control", () -> new HealthCheckResult().withStatus(HealthStatus.UP)),
         indicator("sampleDependency", () -> new HealthCheckResult().withStatus(status.get())
            .withDurationMs(7L).withDetail("fixture", "owned-health-service").withDetail("count", 3))));
      start();
      for(HealthStatus state : List.of(HealthStatus.UP, HealthStatus.DEGRADED, HealthStatus.DOWN, HealthStatus.UNKNOWN))
      {
         status.set(state);
         JSONObject checks = healthResponse("/health", state == HealthStatus.DOWN ? 503 : 200, state.name()).getJSONObject("checks");
         assertEquals(2, checks.length());
         assertEquals("UP", checks.getJSONObject("control").getString("status"));
         assertTrue(checks.getJSONObject("control").getLong("durationMs") >= 0);
         JSONObject dependency = checks.getJSONObject("sampleDependency");
         assertEquals(state.name(), dependency.getString("status"));
         assertEquals(7, dependency.getLong("durationMs"));
         assertEquals("owned-health-service", dependency.getJSONObject("details").getString("fixture"));
         assertEquals(3, dependency.getJSONObject("details").getInt("count"));
      }
   }



   /*******************************************************************************
    ** Throwing checks become UNKNOWN; an explicit DOWN still dominates them.
    *******************************************************************************/
   @Test
   void testThrowingIndicatorAndDownAggregationReleaseWorkers() throws Exception
   {
      health.setIndicators(List.of(indicator("throws", () ->
      {
         throw new QException("owned failure");
      })));
      start();
      JSONObject thrown = healthResponse("/health", 200, "UNKNOWN").getJSONObject("checks").getJSONObject("throws");
      assertEquals("Indicator execution failed", thrown.getJSONObject("details").getString("error"));
      assertEquals("Exception", thrown.getJSONObject("details").getString("exceptionType"));
      health.setIndicators(List.of(health.getIndicators().get(0),
         indicator("down", () -> new HealthCheckResult().withStatus(HealthStatus.DOWN))));
      JSONObject checks = healthResponse("/health", 503, "DOWN").getJSONObject("checks");
      assertEquals("UNKNOWN", checks.getJSONObject("throws").getString("status"));
      assertEquals("DOWN", checks.getJSONObject("down").getString("status"));
   }



   /*******************************************************************************
    ** A rendezvous requires both indicators to start before either can succeed.
    *******************************************************************************/
   @Test
   void testIndicatorsWithinOneRequestRunConcurrently() throws Exception
   {
      CountDownLatch entered = new CountDownLatch(2);
      Probe rendezvous = () ->
      {
         entered.countDown();
         if(!entered.await(2, TimeUnit.SECONDS))
         {
            throw new QException("second indicator was not started concurrently");
         }
         return new HealthCheckResult().withStatus(HealthStatus.UP);
      };
      health.withTimeoutMs(5000).withIndicators(List.of(indicator("first", rendezvous), indicator("second", rendezvous)));
      start();
      JSONObject checks = healthResponse("/health", 200, "UP").getJSONObject("checks");
      assertEquals("UP", checks.getJSONObject("first").getString("status"));
      assertEquals("UP", checks.getJSONObject("second").getString("status"));
      assertEquals(0, entered.getCount());
   }



   /*******************************************************************************
    ** Independent requests may execute together without sharing response state.
    *******************************************************************************/
   @Test
   void testConcurrentHttpRequestsReleaseTheirWorkers() throws Exception
   {
      CountDownLatch entered = new CountDownLatch(2);
      health.setIndicators(List.of(indicator("rendezvous", () ->
      {
         entered.countDown();
         if(!entered.await(2, TimeUnit.SECONDS))
         {
            throw new QException("second request did not enter");
         }
         return new HealthCheckResult().withStatus(HealthStatus.UP);
      })));
      start();
      var first = client.sendAsync(request("/health"), HttpResponse.BodyHandlers.ofString());
      var second = client.sendAsync(request("/health"), HttpResponse.BodyHandlers.ofString());
      for(var pending : List.of(first, second))
      {
         HttpResponse<String> response = pending.get(10, TimeUnit.SECONDS);
         assertEquals(200, response.statusCode(), response.body());
         assertEquals("UP", new JSONObject(response.body()).getString("status"));
         assertRequestContextCleared();
      }
      assertWorkersStopped();
      assertEquals(2, workers.size());
   }



   /*******************************************************************************
    ** Repeated timeouts interrupt cooperative checks and leave no live workers.
    *******************************************************************************/
   @Test
   void testTimeoutCancelsIndicatorAndCleansUpRequest() throws Exception
   {
      CountDownLatch interrupted = new CountDownLatch(3);
      health.withTimeoutMs(200).withIndicators(List.of(indicator("blocked", () ->
      {
         try
         {
            new CountDownLatch(1).await();
            throw new QException("unexpected release");
         }
         catch(InterruptedException e)
         {
            interrupted.countDown();
            throw e;
         }
      })));
      start();
      for(Integer attempt = 0; attempt < 3; attempt++)
      {
         JSONObject check = healthResponse("/health", 200, "UNKNOWN").getJSONObject("checks").getJSONObject("blocked");
         assertEquals(200, check.getLong("durationMs"));
         assertEquals("Health check timed out", check.getJSONObject("details").getString("error"));
         assertEquals(200, check.getJSONObject("details").getInt("timeoutMs"));
      }
      assertTrue(interrupted.await(2, TimeUnit.SECONDS));
      assertEquals(3, workers.size());
   }



   /*******************************************************************************
    ** Probe a live owned H2 TCP database, then make that same service unreachable.
    *******************************************************************************/
   @Test
   void testOwnedDatabaseConnectivityAndUnavailableService() throws Exception
   {
      Server database = Server.createTcpServer("-tcpPort", "0", "-ifNotExists").start();
      String jdbcUrl = "jdbc:h2:tcp://127.0.0.1:" + database.getPort() + "/mem:health_" + UUID.randomUUID();
      try
      {
         try(Connection control = DriverManager.getConnection(jdbcUrl, "sa", ""); var statement = control.createStatement())
         {
            statement.execute("CREATE TABLE health_fixture (id INTEGER PRIMARY KEY)");
            statement.execute("INSERT INTO health_fixture VALUES (17)");
            instance.addBackend(new RDBMSBackendMetaData().withName("healthOwnedDatabase").withVendor("h2")
               .withJdbcUrl(jdbcUrl).withUsername("sa").withPassword(""));
            health.setIndicators(List.of(tracked(new DatabaseHealthIndicator().withBackendName("healthOwnedDatabase"))));
            start();
            for(Integer attempt = 0; attempt < 3; attempt++)
            {
               JSONObject check = healthResponse("/health", 200, "UP").getJSONObject("checks").getJSONObject("database");
               assertEquals("healthOwnedDatabase", check.getJSONObject("details").getString("backendName"));
               assertEquals("h2", check.getJSONObject("details").getString("vendor"));
               assertEquals(1, check.getJSONObject("details").getInt("queryResult"));
               assertTrue(check.getLong("durationMs") >= 0);
            }
            try(var result = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS"))
            {
               assertTrue(result.next());
               assertEquals(1, result.getInt(1), "health probe connections must be closed");
            }
            try(var result = statement.executeQuery("SELECT id FROM health_fixture"))
            {
               assertTrue(result.next());
               assertEquals(17, result.getInt(1), "health checks must preserve owned fixture data");
            }
         }
         database.stop();
         JSONObject down = healthResponse("/health", 503, "DOWN").getJSONObject("checks").getJSONObject("database");
         assertFalse(down.getJSONObject("details").getString("error").isBlank());
         assertTrue(down.getJSONObject("details").getString("exceptionType").contains("Exception"));
      }
      finally
      {
         database.stop();
      }
   }



   /*******************************************************************************
    ** Use thresholds outside observed heap use rather than exhausting the JVM.
    *******************************************************************************/
   @Test
   void testMemoryThresholdsAndInvalidConfiguration() throws Exception
   {
      MemoryHealthIndicator memory = new MemoryHealthIndicator().withThreshold(100);
      health.setIndicators(List.of(tracked(memory)));
      start();
      JSONObject details = healthResponse("/health", 200, "UP").getJSONObject("checks").getJSONObject("memory").getJSONObject("details");
      assertEquals(100, details.getInt("thresholdPercent"));
      assertTrue(details.getLong("maxBytes") > 0);
      assertTrue(details.getLong("usedBytes") > 0);
      assertEquals(details.getLong("maxBytes"), details.getLong("usedBytes") + details.getLong("freeBytes"));
      memory.setThresholdPercent(0);
      JSONObject exceeded = healthResponse("/health", 200, "DEGRADED").getJSONObject("checks").getJSONObject("memory");
      assertEquals("Memory usage exceeds threshold", exceeded.getJSONObject("details").getString("warning"));
      memory.setThresholdPercent(null);
      JSONObject invalid = healthResponse("/health", 200, "UNKNOWN").getJSONObject("checks").getJSONObject("memory");
      assertEquals("Indicator execution failed", invalid.getJSONObject("details").getString("error"));
   }



   /*******************************************************************************
    ** Test thresholds against the owned directory without filling the host disk.
    *******************************************************************************/
   @Test
   void testDiskThresholdsAndInvalidPaths() throws Exception
   {
      DiskSpaceHealthIndicator disk = new DiskSpaceHealthIndicator().withPath(diskDirectory.toString()).withMinimumFreeBytes(0L);
      health.setIndicators(List.of(tracked(disk)));
      start();
      JSONObject details = healthResponse("/health", 200, "UP").getJSONObject("checks").getJSONObject("diskSpace").getJSONObject("details");
      assertEquals(diskDirectory.toString(), details.getString("path"));
      Long freeBytes = details.getLong("freeBytes");
      assertTrue(freeBytes > 0);
      assertEquals(details.getLong("totalBytes"), freeBytes + details.getLong("usedBytes"));
      disk.setMinimumFreeBytes(freeBytes - freeBytes / 4);
      healthResponse("/health", 200, "DEGRADED");
      disk.setMinimumFreeBytes(Long.MAX_VALUE);
      JSONObject exceeded = healthResponse("/health", 503, "DOWN").getJSONObject("checks").getJSONObject("diskSpace");
      assertEquals("Free space below minimum threshold", exceeded.getJSONObject("details").getString("error"));
      disk.setPath(diskDirectory.resolve("missing").toString());
      JSONObject missing = healthResponse("/health", 503, "DOWN").getJSONObject("checks").getJSONObject("diskSpace");
      assertEquals("Path does not exist", missing.getJSONObject("details").getString("error"));
      disk.setPath(null);
      JSONObject absent = healthResponse("/health", 200, "UNKNOWN").getJSONObject("checks").getJSONObject("diskSpace");
      assertEquals("No path configured", absent.getJSONObject("details").getString("error"));
   }



   /*******************************************************************************
    ** Invalid database choices return diagnostic status rather than success.
    *******************************************************************************/
   @Test
   void testInvalidDatabaseConfiguration() throws Exception
   {
      DatabaseHealthIndicator database = new DatabaseHealthIndicator();
      health.setIndicators(List.of(tracked(database)));
      start();
      JSONObject absent = healthResponse("/health", 200, "UNKNOWN").getJSONObject("checks").getJSONObject("database");
      assertEquals("No backend name configured", absent.getJSONObject("details").getString("error"));
      database.setBackendName("missing-health-backend");
      JSONObject missing = healthResponse("/health", 503, "DOWN").getJSONObject("checks").getJSONObject("database");
      assertEquals("Backend not found", missing.getJSONObject("details").getString("error"));
      database.setBackendName(SampleMetaDataProvider.MEMORY_BACKEND_NAME);
      JSONObject wrongType = healthResponse("/health", 200, "UNKNOWN").getJSONObject("checks").getJSONObject("database");
      assertEquals("Backend is not an RDBMS backend", wrongType.getJSONObject("details").getString("error"));
   }



   /*******************************************************************************
    ** Use the sample server, disabling UI fallback so absent routes remain 404.
    *******************************************************************************/
   private void start() throws Exception
   {
      server = new SampleJavalinServer(new SampleMetaDataProvider()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      });
      server.setPort(0);
      server.setServeFrontendMaterialDashboard(false);
      server.withJavalinConfigurationCustomizer(service::set);
      server.withJavalinConfigCustomizer(config ->
      {
         config.jetty.host = "127.0.0.1";
         config.requestLogger.http((context, elapsed) -> completedRequests.add(List.of(
            QContext.getQInstance() == null, QContext.getQSession() == null,
            QContext.getQBackendTransaction() == null, QContext.getActionStack() == null)));
      });
      server.start();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpRequest request(String path)
   {
      return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + service.get().port() + path))
         .timeout(Duration.ofSeconds(10)).GET().build();
   }



   /*******************************************************************************
    ** Wait for request completion before asserting worker lifecycle.
    *******************************************************************************/
   private HttpResponse<String> get(String path) throws Exception
   {
      HttpResponse<String> response = client.send(request(path), HttpResponse.BodyHandlers.ofString());
      assertRequestContextCleared();
      assertWorkersStopped();
      return response;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject healthResponse(String path, Integer statusCode, String status) throws Exception
   {
      HttpResponse<String> response = get(path);
      assertEquals(statusCode, response.statusCode(), response.body());
      assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
      JSONObject body = new JSONObject(response.body());
      assertEquals(status, body.getString("status"), response.body());
      return body;
   }



   /*******************************************************************************
    ** Observe framework cleanup before fixture teardown can mask a leak.
    *******************************************************************************/
   private void assertRequestContextCleared() throws InterruptedException
   {
      assertEquals(List.of(true, true, true, true), completedRequests.poll(5, TimeUnit.SECONDS));
   }



   /*******************************************************************************
    ** Thread identities belong only to this fixture, avoiding global thread counts.
    *******************************************************************************/
   private void assertWorkersStopped() throws InterruptedException
   {
      for(Thread worker : workers)
      {
         worker.join(2000);
         assertFalse(worker.isAlive(), "health worker survived request completion: " + worker.getName());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HealthIndicator tracked(HealthIndicator delegate)
   {
      return indicator(delegate.getName(), () -> delegate.check(instance));
   }



   /*******************************************************************************
    ** Record actual executor workers while keeping the real indicator behavior.
    *******************************************************************************/
   private HealthIndicator indicator(String name, Probe probe)
   {
      return new HealthIndicator()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public String getName()
         {
            return name;
         }



         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public HealthCheckResult check(QInstance qInstance) throws QException
         {
            workers.add(Thread.currentThread());
            try
            {
               return probe.check();
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
               throw new QException("owned indicator interrupted", e);
            }
         }
      };
   }



   /*******************************************************************************
    ** Small owned fixture callback; exceptions cross the real health executor.
    *******************************************************************************/
   @FunctionalInterface
   private interface Probe
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      HealthCheckResult check() throws QException, InterruptedException;
   }
}
