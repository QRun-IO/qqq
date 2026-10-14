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

package com.kingsrook.qqq.middleware.health;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.health.HealthCheck;
import com.kingsrook.qqq.backend.core.health.HealthCheckInput;
import com.kingsrook.qqq.backend.core.health.HealthCheckKind;
import com.kingsrook.qqq.backend.core.health.HealthCheckOutcome;
import com.kingsrook.qqq.backend.core.health.HealthChecksMetaData;
import com.kingsrook.qqq.backend.core.health.HealthDetails;
import com.kingsrook.qqq.backend.core.health.HealthMetaDataProvider;
import com.kingsrook.qqq.backend.core.health.HealthMonitoringMetaData;
import com.kingsrook.qqq.backend.core.health.HealthRunner;
import com.kingsrook.qqq.backend.core.health.HealthStatus;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Real HTTP probes remain public and bounded, with sanitized legacy details.
 *******************************************************************************/
class CentralHealthRoutesTest
{
   private Javalin activeServer;



   /*******************************************************************************
    ** Stop the local HTTP listener even if assertions fail.
    *******************************************************************************/
   @AfterEach
   void stopServer()
   {
      if(activeServer != null)
      {
         activeServer.stop();
      }
   }



   /*******************************************************************************
    ** Database readiness failures do not affect liveness; dependencies never run.
    *******************************************************************************/
   @Test
   void probeSelectionAndSafeBodies() throws Exception
   {
      QInstance instance = instance().withSupplementalMetaData(new HealthCheckMetaData());
      AtomicInteger dependencyRuns = new AtomicInteger();
      HealthChecksMetaData.ofOrWithNew(instance)
         .withCheck(definition("test.alive", HealthCheckKind.LIVENESS, input -> HealthCheckOutcome.up("secret message").withLatencyMs(9L).withDetails(new HealthDetails().withValue("fixture", "registry"))))
         .withCheck(definition("test.database", HealthCheckKind.READINESS, input -> HealthCheckOutcome.down("password=sentinel")))
         .withCheck(definition("test.provider", HealthCheckKind.DEPENDENCY, input ->
         {
            dependencyRuns.incrementAndGet();
            return (HealthCheckOutcome.down("external outage"));
         }));
      Javalin server = server(instance);
      try(HttpClient client = HttpClient.newHttpClient())
      {
         var health = get(client, server, "/health");
         assertThat(health.statusCode()).isEqualTo(200);
         assertThat(health.body()).contains("test.alive", "timestamp").doesNotContain("test.database", "test.provider", "details", "durationMs", "message", "sentinel");
         var ready = get(client, server, "/ready");
         assertThat(ready.statusCode()).isEqualTo(503);
         assertThat(ready.body()).contains("test.database", "DOWN").doesNotContain("test.provider", "details", "message", "timestamp", "sentinel");
         assertThat(dependencyRuns).hasValue(0);
      }
   }



   /*******************************************************************************
    ** A dependency outage cannot take a pod out of service.
    *******************************************************************************/
   @Test
   void dependencyOutageIsIgnored() throws Exception
   {
      QInstance instance = instance().withSupplementalMetaData(new HealthCheckMetaData().withReadinessEndpointPath("/custom-ready"));
      HealthChecksMetaData.ofOrWithNew(instance)
         .withCheck(definition("test.alive", HealthCheckKind.LIVENESS, input -> HealthCheckOutcome.up("")))
         .withCheck(definition("test.provider", HealthCheckKind.DEPENDENCY, input -> HealthCheckOutcome.down("")))
         .withCheck(definition("test.excluded", HealthCheckKind.READINESS, input -> HealthCheckOutcome.down("")).withIncludeInReadiness(false));
      Javalin server = server(instance);
      try(HttpClient client = HttpClient.newHttpClient())
      {
         assertThat(get(client, server, "/custom-ready").statusCode()).isEqualTo(200);
         assertThat(get(client, server, "/metrics").statusCode()).isEqualTo(404);
      }
   }



   /*******************************************************************************
    ** All concurrent checks share a one-second total liveness budget.
    *******************************************************************************/
   @Test
   void hangingChecksAreBounded() throws Exception
   {
      QInstance instance = instance();
      for(int i = 0; i < 3; i++)
      {
         HealthChecksMetaData.ofOrWithNew(instance).withCheck(definition("test.slow" + i, HealthCheckKind.LIVENESS, input ->
         {
            try
            {
               Thread.sleep(5000);
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
            }
            return (HealthCheckOutcome.up(""));
         }));
      }
      long started = System.nanoTime();
      assertThat(HealthProbes.run(instance, false).get("status")).isEqualTo(HealthStatus.DOWN);
      assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
   }



   /*******************************************************************************
    ** Legacy names and JSON structure survive; details are sanitized by the adapter.
    *******************************************************************************/
   @Test
   void legacyAdapterAndShape() throws Exception
   {
      HealthIndicator indicator = new HealthIndicator()
      {
         /***********************************************************************
          ** Legacy mixed-case name.
          ***********************************************************************/
         @Override
         public String getName()
         {
            return ("diskSpace");
         }



         /***********************************************************************
          ** Poisoned scalar and nested values from an existing indicator.
          ***********************************************************************/
         @Override
         public HealthCheckResult check(QInstance instance)
         {
            return (new HealthCheckResult().withStatus(com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus.UP)
               .withDurationMs(7L).withDetails(Map.of("password", "sentinel", "nested", Map.of("x", "secret"), "vendor", "H2")));
         }
      };
      var adapter = new HealthIndicatorCheck(indicator);
      assertThat(adapter.getName()).isEqualTo("core.diskspace");
      assertThat(adapter.toMetaData().getTimeoutMs()).isEqualTo(1000);
      var outcome = adapter.run(new HealthCheckInput().withQInstance(instance()).withDeadline(Instant.now().plusSeconds(1)));
      assertThat(outcome.getStatus()).isEqualTo(HealthStatus.UP);
      assertThat(outcome.getDetails().toJson()).contains("[redacted]", "H2").doesNotContain("sentinel", "nested");
      QInstance instance = instance().withSupplementalMetaData(new HealthCheckMetaData().withIndicators(List.of(indicator)));
      var response = HealthProbes.run(instance, false);
      assertThat(response.get("checks")).isEqualTo(Map.of("diskSpace", Map.of("status", HealthStatus.UP, "durationMs", 7L,
         "details", Map.of("password", "[redacted]", "vendor", "H2"))));
   }



   /*******************************************************************************
    ** Public legacy details redact secret values while preserving deployment metadata.
    *******************************************************************************/
   @Test
   void legacyDetailsAreRedactedOnHealth() throws Exception
   {
      HealthIndicator indicator = new HealthIndicator()
      {
         /***********************************************************************
          ** Preserve the deployment script's mixed-case lookup.
          ***********************************************************************/
         @Override
         public String getName()
         {
            return ("buildInfo");
         }



         /***********************************************************************
          ** Use harmless fixture credentials under ordinary detail keys.
          ***********************************************************************/
         @Override
         public HealthCheckResult check(QInstance instance)
         {
            return (new HealthCheckResult().withStatus(com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus.UP)
               .withDetail("description", "Bearer fixture-value")
               .withDetail("endpoint", "https://u:p@h/x?token=1")
               .withDetail("commitShaShort", "a1b2c3d"));
         }
      };
      Javalin server = server(instance().withSupplementalMetaData(new HealthCheckMetaData().withIndicators(List.of(indicator))));
      try(HttpClient client = HttpClient.newHttpClient())
      {
         var response = get(client, server, "/health");
         assertThat(response.statusCode()).isEqualTo(200);
         JSONObject check = new JSONObject(response.body()).getJSONObject("checks").getJSONObject("buildInfo");
         assertThat(check.getLong("durationMs")).isGreaterThanOrEqualTo(0);
         JSONObject details = check.getJSONObject("details");
         assertThat(details.getString("description")).isEqualTo("[redacted]");
         assertThat(details.getString("endpoint")).isEqualTo("https://h/x");
         assertThat(details.getString("commitShaShort")).isEqualTo("a1b2c3d");
         var ready = get(client, server, "/ready");
         assertThat(ready.statusCode()).isEqualTo(200);
         assertThat(new JSONObject(ready.body()).getJSONObject("checks").getString("core.buildinfo")).isEqualTo("UP");
         assertThat(ready.body()).doesNotContain("details", "durationMs", "fixture-value", "a1b2c3d");
      }
   }



   /*******************************************************************************
    ** Label escaping protects Prometheus text boundaries.
    *******************************************************************************/
   @Test
   void metricEscaping()
   {
      assertThat(HealthMetrics.escape("a\\b\"c\nd")).isEqualTo("a\\\\b\\\"c\\nd");
   }



   /*******************************************************************************
    ** Readiness uses recent cached results but retries stale entries inline.
    *******************************************************************************/
   @Test
   void readinessCacheFreshness()
   {
      QInstance instance = instance();
      AtomicInteger calls = new AtomicInteger();
      HealthChecksMetaData.ofOrWithNew(instance).withCheck(definition("test.database", HealthCheckKind.READINESS, input ->
      {
         calls.incrementAndGet();
         return (HealthCheckOutcome.up(""));
      }));
      var runner = Mockito.mock(HealthRunner.class);
      try(var runners = Mockito.mockStatic(HealthRunner.class))
      {
         runners.when(() -> HealthRunner.getForInstance(instance)).thenReturn(runner);
         Mockito.when(runner.getLastOutcome("test.database")).thenReturn(HealthCheckOutcome.down("cached failure"));
         Mockito.when(runner.getLastOutcomeAt("test.database")).thenReturn(Instant.now());
         assertThat(HealthProbes.run(instance, true).get("status")).isEqualTo(HealthStatus.DOWN);
         assertThat(calls).hasValue(0);
         Mockito.when(runner.getLastOutcomeAt("test.database")).thenReturn(Instant.now().minusSeconds(61));
         assertThat(HealthProbes.run(instance, true).get("status")).isEqualTo(HealthStatus.UP);
         assertThat(calls).hasValue(1);
      }
   }



   /*******************************************************************************
    ** Metrics use persisted last state, exporting no details or messages.
    *******************************************************************************/
   @Test
   void metricsOptIn() throws Exception
   {
      QInstance instance = instance();
      MemoryRecordStore.getInstance().reset();
      new HealthMetaDataProvider().defineAll(instance, "memory", null);
      HealthMonitoringMetaData.ofOrWithNew(instance).withPrometheusEnabled(true).withRunnerEnabled(false);
      HealthChecksMetaData.ofOrWithNew(instance).withCheck(definition("test.metric", HealthCheckKind.LIVENESS,
         input -> HealthCheckOutcome.up("password=sentinel").withLatencyMs(12L)));
      try(var runner = new HealthRunner(instance, "metric", Clock.systemUTC()))
      {
         runner.runNow("test.metric");
      }
      Javalin server = server(instance);
      try(HttpClient client = HttpClient.newHttpClient())
      {
         var response = get(client, server, "/metrics");
         assertThat(response.statusCode()).isEqualTo(200);
         assertThat(response.headers().firstValue("content-type").orElse("")).startsWith("text/plain");
         assertThat(response.body()).contains("qqq_health_check_status{check=\"test.metric\",component=\"test\",kind=\"LIVENESS\"} 0",
            "qqq_health_check_latency_ms{check=\"test.metric\",component=\"test\",kind=\"LIVENESS\"} 12").doesNotContain("message", "details", "sentinel");
      }
   }



   /*******************************************************************************
    ** Worker QContexts validate their instance, including its authentication scope.
    *******************************************************************************/
   private QInstance instance()
   {
      QInstance instance = new QInstance();
      instance.addBackend(new QBackendMetaData().withName("memory")
         .withBackendType(MemoryBackendModule.class));
      instance.addTable(new QTableMetaData().withName("fixture").withBackendName("memory")
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(),
         new QAuthenticationMetaData().withName("mock")
            .withType(QAuthenticationType.MOCK));
      return (instance);
   }



   /*******************************************************************************
    ** Small definitions shared by route tests.
    *******************************************************************************/
   private com.kingsrook.qqq.backend.core.health.HealthCheckMetaData definition(String name, HealthCheckKind kind, HealthCheck check)
   {
      return (new com.kingsrook.qqq.backend.core.health.HealthCheckMetaData().withName(name).withComponent("test").withKind(kind)
         .withIntervalSeconds(30).withTimeoutMs(1000).withIncludeInReadiness(true).withCheckInstance(check));
   }



   /*******************************************************************************
    ** Use an ephemeral local port and the real route provider.
    *******************************************************************************/
   private Javalin server(QInstance instance)
   {
      QContext.init(instance, new QSession());
      QContext.clear();
      JavalinHealthRouteProvider provider = new JavalinHealthRouteProvider();
      provider.setQInstance(instance);
      activeServer = Javalin.create(provider::acceptJavalinConfig).start(0);
      return (activeServer);
   }



   /*******************************************************************************
    ** Probe without credentials.
    *******************************************************************************/
   private HttpResponse<String> get(HttpClient client, Javalin server, String path) throws Exception
   {
      return (client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path)).GET().build(), HttpResponse.BodyHandlers.ofString()));
   }
}
