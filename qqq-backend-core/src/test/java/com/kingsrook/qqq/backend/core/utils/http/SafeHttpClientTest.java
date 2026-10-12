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

package com.kingsrook.qqq.backend.core.utils.http;


import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/*******************************************************************************
 ** Unit test for SafeHttpClient, against a local HTTP server on 127.0.0.1
 ** (reached through an allowlist and a policy that allows plain http).
 *******************************************************************************/
class SafeHttpClientTest extends BaseTest
{
   private HttpServer             server;
   private int                    port;
   private final AtomicInteger    hits         = new AtomicInteger();
   private final AtomicReference<String> lastBody     = new AtomicReference<>();
   private final AtomicReference<String> lastSignature = new AtomicReference<>();



   /*******************************************************************************
    ** Start the local server.
    *******************************************************************************/
   @BeforeEach
   void startServer() throws IOException
   {
      server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
      server.createContext("/ok", exchange ->
      {
         hits.incrementAndGet();
         lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
         lastSignature.set(exchange.getRequestHeaders().getFirst("webhook-signature"));
         exchange.getResponseHeaders().add("X-Request-Id", "req-1");
         respond(exchange, 200, "accepted");
      });
      server.createContext("/fail", exchange -> respond(exchange, 503, "busy"));
      server.createContext("/redirect302", exchange -> redirect(exchange, 302));
      server.createContext("/redirect307", exchange -> redirect(exchange, 307));
      server.createContext("/slow", exchange ->
      {
         sleep(2000);
         respond(exchange, 200, "late");
      });
      server.createContext("/trickle", exchange ->
      {
         exchange.sendResponseHeaders(200, 0);
         try(OutputStream out = exchange.getResponseBody())
         {
            for(int i = 0; i < 40; i++)
            {
               out.write('x');
               out.flush();
               sleep(100);
            }
         }
         catch(IOException e)
         {
            ////////////////////////////////////
            // client went away, as expected //
            ////////////////////////////////////
         }
      });
      server.createContext("/big", exchange ->
      {
         byte[] big = new byte[200 * 1024];
         java.util.Arrays.fill(big, (byte) 'a');
         exchange.sendResponseHeaders(200, big.length);
         try(OutputStream out = exchange.getResponseBody())
         {
            out.write(big);
         }
         catch(IOException e)
         {
            //////////////////////////////////////////////////
            // client stops reading at the cap, as expected //
            //////////////////////////////////////////////////
         }
      });
      server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
      server.start();
      port = server.getAddress().getPort();
   }



   /*******************************************************************************
    ** Stop the local server.
    *******************************************************************************/
   @AfterEach
   void stopServer()
   {
      server.stop(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostSucceedsAndReportsAddressAndHeaders() throws Exception
   {
      try(SafeHttpClient client = new SafeHttpClient(localPolicy()))
      {
         SafeHttpResponse response = client.post(url("/ok"), Map.of("Content-Type", "application/json", "webhook-signature", "v1,abc"), "{\"a\":1}".getBytes(StandardCharsets.UTF_8));
         assertThat(response.isSuccess()).isTrue();
         assertThat(response.statusCode()).isEqualTo(200);
         assertThat(response.bodyExcerpt(100)).isEqualTo("accepted");
         assertThat(response.bodyExcerpt(3)).isEqualTo("acc");
         assertThat(response.truncated()).isFalse();
         assertThat(response.remoteAddress()).isEqualTo("127.0.0.1");
         assertThat(response.header("X-REQUEST-ID")).isEqualTo("req-1");
         assertThat(response.header(null)).isNull();
         assertThat(response.durationMillis()).isGreaterThanOrEqualTo(0);
         assertThat(lastBody.get()).isEqualTo("{\"a\":1}");
         assertThat(lastSignature.get()).isEqualTo("v1,abc");

         SafeHttpResponse failure = client.post(url("/fail"), Map.of(), null);
         assertThat(failure.isSuccess()).isFalse();
         assertThat(failure.statusCode()).isEqualTo(503);
      }
   }



   /*******************************************************************************
    ** Without the allowlist, the loopback server is refused before connecting.
    *******************************************************************************/
   @Test
   void testLoopbackBlockedWithoutAllowlist()
   {
      try(SafeHttpClient client = new SafeHttpClient(SafeHttpPolicy.defaults().withAllowPlainHttp(true)))
      {
         assertReason(() -> client.post(url("/ok"), Map.of(), new byte[0]), SafeHttpException.Reason.BLOCKED_ADDRESS);
         assertReason(() -> client.checkUrl("http://[::1]:" + port + "/ok"), SafeHttpException.Reason.BLOCKED_ADDRESS);
      }
      assertThat(hits.get()).isZero();
   }



   /*******************************************************************************
    ** Every blocked address is refused, whatever the resolver answers.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"10.0.0.7", "172.20.1.1", "192.168.0.10", "169.254.169.254", "100.100.100.200", "fd00:ec2::254", "::ffff:127.0.0.1", "64:ff9b::a00:1",
      "2002:c0a8:101::", "0.0.0.0"})
   void testBlockedAnswersAreRefused(String answer)
   {
      try(SafeHttpClient client = new SafeHttpClient(SafeHttpPolicy.defaults(), host -> new InetAddress[] {InetAddress.getByName(answer)}))
      {
         assertReason(() -> client.post("https://hooks.example.test/x", Map.of(), new byte[0]), SafeHttpException.Reason.BLOCKED_ADDRESS);
      }
   }



   /*******************************************************************************
    ** Metadata stays blocked even when the allowlist covers it.
    *******************************************************************************/
   @Test
   void testMetadataBlockedEvenWhenAllowlisted()
   {
      SafeHttpPolicy policy = SafeHttpPolicy.defaults().withInternalAllowlist(List.of("169.254.0.0/16", "metadata.internal"));
      try(SafeHttpClient client = new SafeHttpClient(policy, host -> new InetAddress[] {InetAddress.getByName("169.254.169.254")}))
      {
         assertReason(() -> client.post("https://metadata.internal/latest/meta-data/", Map.of(), new byte[0]), SafeHttpException.Reason.BLOCKED_ADDRESS);
      }
   }



   /*******************************************************************************
    ** DNS rebinding: the host is resolved once per request, the checked
    ** address is the one connected to, and a later (internal) answer is never
    ** used. A mixed answer with any blocked address is refused.
    *******************************************************************************/
   @Test
   void testDnsRebindingIsPinned() throws Exception
   {
      AtomicInteger calls = new AtomicInteger();
      SafeHttpClient.HostResolver rebinding = host ->
      {
         int call = calls.incrementAndGet();
         //////////////////////////////////////////////////////////////////////////
         // first answer (per request) is the allowed address; any further     //
         // lookup would get an internal address that is not allowlisted.      //
         //////////////////////////////////////////////////////////////////////////
         return (call % 2 == 1 ? new InetAddress[] {InetAddress.getByName("127.0.0.1")} : new InetAddress[] {InetAddress.getByName("10.9.9.9")});
      };
      try(SafeHttpClient client = new SafeHttpClient(localPolicy(), rebinding))
      {
         SafeHttpResponse response = client.post("http://rebind.example.test:" + port + "/ok", Map.of(), "1".getBytes(StandardCharsets.UTF_8));
         assertThat(response.statusCode()).isEqualTo(200);
         assertThat(response.remoteAddress()).isEqualTo("127.0.0.1");
         assertThat(calls.get()).as("resolved exactly once for the request").isEqualTo(1);

         ///////////////////////////////////////////////////////////////////////////
         // the next request resolves again (answer 2: internal) and is refused,  //
         // even though a pooled connection to the earlier address exists.        //
         ///////////////////////////////////////////////////////////////////////////
         assertReason(() -> client.post("http://rebind.example.test:" + port + "/ok", Map.of(), new byte[0]), SafeHttpException.Reason.BLOCKED_ADDRESS);
         assertThat(calls.get()).isEqualTo(2);
         assertThat(hits.get()).isEqualTo(1);
      }

      try(SafeHttpClient client = new SafeHttpClient(localPolicy(), host -> new InetAddress[] {InetAddress.getByName("127.0.0.1"), InetAddress.getByName("10.9.9.9")}))
      {
         assertReason(() -> client.post("http://mixed.example.test:" + port + "/ok", Map.of(), new byte[0]), SafeHttpException.Reason.BLOCKED_ADDRESS);
      }
      assertThat(hits.get()).isEqualTo(1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDnsFailure()
   {
      try(SafeHttpClient client = new SafeHttpClient(localPolicy(), host ->
      {
         throw (new UnknownHostException(host));
      }))
      {
         assertReason(() -> client.post("http://nowhere.example.test/x", Map.of(), new byte[0]), SafeHttpException.Reason.DNS_FAILURE);
      }
      try(SafeHttpClient client = new SafeHttpClient(localPolicy(), host -> new InetAddress[0]))
      {
         assertReason(() -> client.checkUrl("http://empty.example.test/x"), SafeHttpException.Reason.DNS_FAILURE);
      }
   }



   /*******************************************************************************
    ** Redirects are returned, never followed (302 and 307 to metadata).
    *******************************************************************************/
   @Test
   void testRedirectsAreNotFollowed() throws Exception
   {
      try(SafeHttpClient client = new SafeHttpClient(localPolicy()))
      {
         for(String path : List.of("/redirect302", "/redirect307"))
         {
            SafeHttpResponse response = client.post(url(path), Map.of(), "x".getBytes(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isIn(302, 307);
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.header("location")).isEqualTo("http://169.254.169.254/latest/meta-data/");
            assertThat(response.body()).isEmpty();
            assertThat(response.bodyExcerpt(10)).isEmpty();
            assertThat(response.remoteAddress()).as("captured even though a bodiless response releases the connection").isEqualTo("127.0.0.1");
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSchemeAndUrlRules()
   {
      try(SafeHttpClient client = new SafeHttpClient(SafeHttpPolicy.defaults().withInternalAllowlist(List.of("127.0.0.1"))))
      {
         assertReason(() -> client.post(url("/ok"), Map.of(), new byte[0]), SafeHttpException.Reason.BLOCKED_SCHEME);
         assertReason(() -> client.post("ftp://example.test/x", Map.of(), new byte[0]), SafeHttpException.Reason.BLOCKED_SCHEME);
         assertReason(() -> client.post("https://user:pass@example.test/x", Map.of(), new byte[0]), SafeHttpException.Reason.INVALID_URL);
         assertReason(() -> client.post("https:///nohost", Map.of(), new byte[0]), SafeHttpException.Reason.INVALID_URL);
         assertReason(() -> client.post("https://exa mple.test/", Map.of(), new byte[0]), SafeHttpException.Reason.INVALID_URL);
         assertReason(() -> client.post(null, Map.of(), new byte[0]), SafeHttpException.Reason.INVALID_URL);
         assertReason(() -> client.post(" ", Map.of(), new byte[0]), SafeHttpException.Reason.INVALID_URL);
      }
      try(SafeHttpClient client = new SafeHttpClient(localPolicy()))
      {
         assertReason(() -> client.post("gopher://127.0.0.1/x", Map.of(), new byte[0]), SafeHttpException.Reason.BLOCKED_SCHEME);
      }
      assertThat(SafeHttpException.Reason.BLOCKED_ADDRESS.isPolicy()).isTrue();
      assertThat(SafeHttpException.Reason.READ_TIMEOUT.isPolicy()).isFalse();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReadTimeout()
   {
      SafeHttpPolicy policy = localPolicy().withReadTimeout(Duration.ofMillis(300)).withTotalTimeout(Duration.ofSeconds(10));
      try(SafeHttpClient client = new SafeHttpClient(policy))
      {
         assertReason(() -> client.post(url("/slow"), Map.of(), new byte[0]), SafeHttpException.Reason.READ_TIMEOUT);
      }
   }



   /*******************************************************************************
    ** A server that keeps sending a byte every 100 ms never trips the read
    ** timeout, so the total timeout stops it.
    *******************************************************************************/
   @Test
   void testTotalTimeout()
   {
      SafeHttpPolicy policy = localPolicy().withReadTimeout(Duration.ofSeconds(2)).withTotalTimeout(Duration.ofMillis(600));
      try(SafeHttpClient client = new SafeHttpClient(policy))
      {
         long start = System.currentTimeMillis();
         assertReason(() -> client.post(url("/trickle"), Map.of(), new byte[0]), SafeHttpException.Reason.TOTAL_TIMEOUT);
         assertThat(System.currentTimeMillis() - start).isLessThan(3000);
      }
   }



   /*******************************************************************************
    ** Connect timeout against a non-routable address (allowlisted). Skipped
    ** when the network answers "unreachable" at once instead of dropping.
    *******************************************************************************/
   @Test
   void testConnectTimeout()
   {
      SafeHttpPolicy policy = SafeHttpPolicy.defaults().withAllowPlainHttp(true).withInternalAllowlist(List.of("10.255.255.1"))
         .withConnectTimeout(Duration.ofMillis(300)).withTotalTimeout(Duration.ofSeconds(10));
      try(SafeHttpClient client = new SafeHttpClient(policy))
      {
         SafeHttpException e = catchSafe(() -> client.post("http://10.255.255.1:81/x", Map.of(), new byte[0]));
         assumeTrue(e.getReason() == SafeHttpException.Reason.CONNECT_TIMEOUT, "network rejected the connect immediately (" + e.getReason() + "); cannot observe a connect timeout here");
         assertThat(e.getReason()).isEqualTo(SafeHttpException.Reason.CONNECT_TIMEOUT);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testResponseSizeCap() throws Exception
   {
      try(SafeHttpClient client = new SafeHttpClient(localPolicy().withMaxResponseBytes(1024)))
      {
         SafeHttpResponse response = client.post(url("/big"), Map.of(), new byte[0]);
         assertThat(response.statusCode()).isEqualTo(200);
         assertThat(response.truncated()).isTrue();
         assertThat(response.body()).hasSize(1024);

         ///////////////////////////////////////////////////////////////
         // the client stays usable after an aborted, capped response //
         ///////////////////////////////////////////////////////////////
         assertThat(client.post(url("/ok"), Map.of(), new byte[0]).statusCode()).isEqualTo(200);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPolicyDefaultsAndCacheKey()
   {
      SafeHttpPolicy defaults = SafeHttpPolicy.defaults();
      assertThat(defaults.isAllowPlainHttp()).isFalse();
      assertThat(defaults.getInternalAllowlist()).isEmpty();
      assertThat(defaults.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
      assertThat(defaults.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
      assertThat(defaults.getTotalTimeout()).isEqualTo(Duration.ofSeconds(30));
      assertThat(defaults.getMaxResponseBytes()).isEqualTo(64 * 1024);
      assertThat(defaults.getUserAgent()).isNotBlank();
      assertThat(defaults.cacheKey()).isEqualTo(SafeHttpPolicy.defaults().cacheKey());
      assertThat(defaults.cacheKey()).isNotEqualTo(SafeHttpPolicy.defaults().withInternalAllowlist(List.of("10.0.0.0/8")).cacheKey());
      assertThat(SafeHttpPolicy.defaults().withInternalAllowlist(null).getInternalAllowlist()).isEmpty();
      assertThat(SafeHttpPolicy.defaults().withUserAgent("x/1").getUserAgent()).isEqualTo("x/1");
   }



   /*******************************************************************************
    ** Plain http to the local server, with 127.0.0.1 allowlisted.
    *******************************************************************************/
   private static SafeHttpPolicy localPolicy()
   {
      return (SafeHttpPolicy.defaults().withAllowPlainHttp(true).withInternalAllowlist(List.of("127.0.0.1")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String url(String path)
   {
      return ("http://127.0.0.1:" + port + path);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void respond(HttpExchange exchange, int status, String body) throws IOException
   {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(status, bytes.length);
      try(OutputStream out = exchange.getResponseBody())
      {
         out.write(bytes);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void redirect(HttpExchange exchange, int status) throws IOException
   {
      exchange.getRequestBody().readAllBytes();
      exchange.getResponseHeaders().add("Location", "http://169.254.169.254/latest/meta-data/");
      exchange.sendResponseHeaders(status, -1);
      exchange.close();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void sleep(long millis)
   {
      try
      {
         Thread.sleep(millis);
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
   }



   /*******************************************************************************
    ** A call that may throw SafeHttpException.
    *******************************************************************************/
   @FunctionalInterface
   private interface SafeCall
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      void run() throws SafeHttpException;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static SafeHttpException catchSafe(SafeCall call)
   {
      try
      {
         call.run();
      }
      catch(SafeHttpException e)
      {
         return (e);
      }
      throw (new AssertionError("Expected a SafeHttpException"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void assertReason(SafeCall call, SafeHttpException.Reason reason)
   {
      SafeHttpException e = catchSafe(call);
      assertThat(e.getReason()).as(e.getMessage()).isEqualTo(reason);
   }
}
