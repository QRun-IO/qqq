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


package com.kingsrook.qqq.backend.core.jobs.effects.targets;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectKeys;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectRequest;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectResult;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffectType;
import com.kingsrook.qqq.backend.core.jobs.effects.ReconcileResult;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Real loopback HTTP requests exercise response, timeout and connection outcomes.
 *******************************************************************************/
class HttpEffectTargetTest extends BaseTest
{
   /*******************************************************************************
    ** Provider response codes must not turn an ambiguous delivery into a safe retry.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({ "200,SENT", "201,SENT", "409,SENT", "429,RETRY", "503,RETRY", "400,FAILED", "425,FAILED", "500,UNKNOWN", "408,UNKNOWN", "502,UNKNOWN", "504,UNKNOWN" })
   void responseMappingAndStableHeader(int status, EffectResult.Outcome expected) throws Exception
   {
      List<String> headers = new CopyOnWriteArrayList<>();
      HttpServer   server  = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/", exchange ->
      {
         headers.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
         exchange.getRequestBody().readAllBytes();
         exchange.getResponseHeaders().add("Provider-Ref", "receipt");
         exchange.sendResponseHeaders(status, -1);
         exchange.close();
      });
      server.start();
      try
      {
         HttpEffectTarget target = target(server.getAddress().getPort(), Duration.ofSeconds(5));
         for(int attempt = 0; attempt < 2; attempt++)
         {
            EffectResult result = target.deliver(request());
            assertThat(result.outcome()).isEqualTo(expected);
            assertThat(result.providerRef()).isEqualTo(expected == EffectResult.Outcome.SENT ? "receipt" : null);
         }
         assertThat(headers).containsExactly(EffectKeys.idempotencyKeyHeader(request().idempotencyKey()), EffectKeys.idempotencyKeyHeader(request().idempotencyKey()));
         assertThat(target.reconcile(request())).isEqualTo(ReconcileResult.unknown("no lookup"));
      }
      finally
      {
         server.stop(0);
      }
   }



   /*******************************************************************************
    ** A server that accepted the body but did not answer leaves an unknown delivery.
    *******************************************************************************/
   @Test
   void timeoutAfterSendingIsUnknown() throws Exception
   {
      CountDownLatch received = new CountDownLatch(1);
      CountDownLatch release  = new CountDownLatch(1);
      HttpServer     server   = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/", exchange ->
      {
         exchange.getRequestBody().readAllBytes();
         received.countDown();
         try
         {
            release.await(5, TimeUnit.SECONDS);
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
         }
         finally
         {
            exchange.close();
         }
      });
      server.start();
      try
      {
         assertThat(target(server.getAddress().getPort(), Duration.ofMillis(500)).deliver(request()).outcome()).isEqualTo(EffectResult.Outcome.UNKNOWN);
         assertThat(received.await(1, TimeUnit.SECONDS)).isTrue();
      }
      finally
      {
         release.countDown();
         server.stop(0);
      }
   }



   /*******************************************************************************
    ** Connection refusal proves that the provider never accepted the request.
    *******************************************************************************/
   @Test
   void closedPortIsRetry() throws IOException
   {
      int port;
      try(ServerSocket socket = new ServerSocket(0))
      {
         port = socket.getLocalPort();
      }
      assertThat(target(port, Duration.ofSeconds(5)).deliver(request()).outcome()).isEqualTo(EffectResult.Outcome.RETRY);
   }



   /*******************************************************************************
    ** Reconciliation calls the subclass lookup instead of sending the POST again.
    *******************************************************************************/
   @Test
   void delegatesLookup() throws Exception
   {
      HttpEffectTarget target = new HttpEffectTarget()
      {
         /*******************************************************************************
          ** No delivery is expected during this reconciliation.
          *******************************************************************************/
         @Override
         protected HttpRequest.Builder buildRequest(EffectRequest request)
         {
            throw (new AssertionError("Must not deliver"));
         }



         /*******************************************************************************
          ** Simulate a provider lookup by idempotency key.
          *******************************************************************************/
         @Override
         protected ReconcileResult lookup(EffectRequest request)
         {
            return (ReconcileResult.found(request.idempotencyKey()));
         }
      };
      assertThat(target.reconcile(request())).isEqualTo(ReconcileResult.found(request().idempotencyKey()));
   }



   /*******************************************************************************
    ** Configure a local POST target with a provider receipt header.
    *******************************************************************************/
   private HttpEffectTarget target(int port, Duration requestTimeout)
   {
      return (new HttpEffectTarget()
      {
         /*******************************************************************************
          ** Supply a stale header to prove the base replaces it with the stable key.
          *******************************************************************************/
         @Override
         protected HttpRequest.Builder buildRequest(EffectRequest request)
         {
            return (HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
               .header("Idempotency-Key", "replace-me").POST(HttpRequest.BodyPublishers.ofString("payload")));
         }



         /*******************************************************************************
          ** Use a short bounded deadline for the timeout test.
          *******************************************************************************/
         @Override
         protected Duration timeout()
         {
            return (requestTimeout);
         }



         /*******************************************************************************
          ** Extract a sample provider receipt.
          *******************************************************************************/
         @Override
         protected String providerRef(HttpResponse<String> response)
         {
            return (response.headers().firstValue("Provider-Ref").orElse(null));
         }
      });
   }



   /*******************************************************************************
    ** A stable external effect shared across attempts.
    *******************************************************************************/
   private EffectRequest request()
   {
      return (new EffectRequest("1:0:row:http", "http", JobEffectType.IDEMPOTENT_EXTERNAL, 1, 0, "row", Map.of(), null, 1));
   }
}
