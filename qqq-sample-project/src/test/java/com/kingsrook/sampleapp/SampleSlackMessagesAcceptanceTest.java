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


import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.DividerWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.LineChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StatisticsData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.slack.QSlackImplementation;
import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native widget conversion and Slack SDK wire payloads, without live Slack.
 *******************************************************************************/
class SampleSlackMessagesAcceptanceTest
{
   private static final String TOKEN = "synthetic-local-slack-token";
   private static final String CHANNEL = "synthetic-local-channel";
   private final List<WireRequest> requests = new CopyOnWriteArrayList<>();
   private HttpServer receiver;
   private String endpoint;
   private String response = "{\"ok\":true,\"channel\":\"synthetic-local-channel\",\"ts\":\"1.0\"}";
   private Integer responseStatus = 200;
   @TempDir(cleanup = CleanupMode.ON_SUCCESS)
   Path directory;



   /*******************************************************************************
    ** Receive the SDK's actual HTTP request on an owned ephemeral loopback port.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws IOException
   {
      receiver = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      receiver.createContext("/api/", exchange ->
      {
         requests.add(new WireRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
            exchange.getRequestHeaders().getFirst("Authorization"), exchange.getRequestHeaders().getFirst("Content-Type"),
            new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
         boolean authenticationLookup = exchange.getRequestURI().getPath().equals("/api/auth.test");
         String body = authenticationLookup ? "{\"ok\":true,\"team_id\":\"TLOCAL\",\"user_id\":\"ULOCAL\"}" : response;
         byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
         exchange.getResponseHeaders().set("Content-Type", "application/json");
         exchange.sendResponseHeaders(authenticationLookup ? 200 : responseStatus, bytes.length);
         try(var output = exchange.getResponseBody())
         {
            output.write(bytes);
         }
      });
      receiver.start();
      endpoint = "http://127.0.0.1:" + receiver.getAddress().getPort() + "/api/";
   }



   /*******************************************************************************
    ** No SDK singleton is changed in the test JVM; close only our local receiver.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      if(receiver != null)
      {
         receiver.stop(0);
      }
   }



   /*******************************************************************************
    ** Wrong conversion, channel, text or markdown flag fails the wire assertion.
    *******************************************************************************/
   @Test
   void testStatisticsConversionAndOutboundPayload() throws Exception
   {
      String text = convert(new StatisticsData(1234, null, null));
      assertEquals("\n\n*Owned people*: 1,234", text);
      assertTrue(sendInOwnedJvm(text, "configured").contains("POST_MESSAGE_RETURNED"));
      assertWire(text);
   }



   /*******************************************************************************
    ** Chart and line payloads retain their supported label/number formatting.
    *******************************************************************************/
   @Test
   void testChartAndLineConversion() throws Exception
   {
      ChartData chart = new ChartData().withChartData(new ChartData.Data().withLabels(List.of("Owned", "Other"))
         .withDatasets(List.of(new ChartData.Data.Dataset().withLabel("People").withData(List.of(1200, 3)))));
      String text = convert(chart);
      assertEquals("\n*Owned*: 1,200\n*Other*: 3\n", text);
      sendInOwnedJvm(text, "configured");
      assertWire(text);
      LineChartData line = new LineChartData().withChartData(new LineChartData.Data().withLabels(List.of("Owned series"))
         .withDatasets(List.of(new LineChartData.Data.Dataset().withLabel("People").withData(List.of(1200, 3)))));
      assertEquals("\n*Owned series*: People [1,200,3] \n", convert(line));
   }



   /*******************************************************************************
    ** Empty and unsupported data use explicit existing conversion messages.
    *******************************************************************************/
   @Test
   void testEmptyAndUnsupportedWidgets() throws Exception
   {
      assertEquals("\nNo data was found", convert(new ChartData()));
      assertEquals("\nNo data was found", convert(new LineChartData()));
      assertEquals("\nUnsupported widget type", convert(null));
      assertEquals("\nUnsupported widget type", convert(new DividerWidgetData()));
      assertTrue(requests.isEmpty());
   }



   /*******************************************************************************
    ** API rejection is logged by the void adapter; it is not success confirmation.
    *******************************************************************************/
   @Test
   void testServiceRejectionIsObservedLocally() throws Exception
   {
      response = "{\"ok\":false,\"error\":\"fixture_rejected\"}";
      String output = sendInOwnedJvm("Owned rejection case", "configured");
      assertWire("Owned rejection case");
      assertTrue(output.contains("fixture_rejected"));
      assertTrue(output.contains("POST_MESSAGE_RETURNED"));
   }



   /*******************************************************************************
    ** Transport rejection stays local and is caught by the existing void adapter.
    *******************************************************************************/
   @Test
   void testHttpRejectionIsObservedLocally() throws Exception
   {
      responseStatus = 400;
      response = "{\"ok\":false,\"error\":\"fixture_http_rejected\"}";
      String output = sendInOwnedJvm("Owned HTTP rejection", "configured");
      assertWire("Owned HTTP rejection");
      assertTrue(output.contains("fixture_http_rejected"));
      assertTrue(output.contains("POST_MESSAGE_RETURNED"));
   }



   /*******************************************************************************
    ** Missing configuration must not emit a message, including to our receiver.
    *******************************************************************************/
   @Test
   void testAbsentConfigurationDoesNotTransmit() throws Exception
   {
      for(String configuration : List.of("absent", "noToken", "noChannel"))
      {
         String output = sendInOwnedJvm("Must not transmit", configuration);
         assertTrue(requests.isEmpty(), requests.stream().map(WireRequest::path).toList().toString());
         assertTrue(output.contains("Slack token and channel must be configured"));
         assertTrue(output.contains("POST_MESSAGE_RETURNED"));
      }
   }



   /*******************************************************************************
    ** Convert through the public normal helper, not a test reimplementation.
    *******************************************************************************/
   private String convert(QWidgetData data) throws Exception
   {
      return QSlackImplementation.convertWidgetDataToSlackMessage(new QWidgetMetaData().withName("ownedPeople").withLabel("Owned people"),
         new RenderWidgetOutput(data));
   }



   /*******************************************************************************
    ** Fork because production intentionally reads environment configuration.
    ** Empty environment/temp cwd prevent inherited credentials or dotenv files.
    *******************************************************************************/
   private String sendInOwnedJvm(String text, String configuration) throws Exception
   {
      Path output = directory.resolve("child-output.txt");
      Path logging = directory.resolve("log4j2.xml");
      Files.writeString(logging, """
         <Configuration status="OFF"><Appenders><Console name="local"><PatternLayout pattern="%level %message%n"/></Console></Appenders>
         <Loggers><Root level="info"><AppenderRef ref="local"/></Root></Loggers></Configuration>
         """);
      ProcessBuilder builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
         "-Dlog4j.configurationFile=" + logging, "-cp", System.getProperty("java.class.path"),
         OutboundProcess.class.getName(), endpoint, text, configuration);
      builder.directory(directory.toFile()).redirectErrorStream(true).redirectOutput(output.toFile());
      builder.environment().clear();
      if(!configuration.equals("absent") && !configuration.equals("noToken"))
      {
         builder.environment().put("SLACK_TOKEN", TOKEN);
      }
      if(!configuration.equals("absent") && !configuration.equals("noChannel"))
      {
         builder.environment().put("SLACK_CHANNEL_ID", CHANNEL);
      }
      Process child = builder.start();
      try
      {
         assertTrue(child.waitFor(20, TimeUnit.SECONDS), "Owned Slack adapter child timed out");
         assertEquals(0, child.exitValue(), "Owned Slack adapter child failed; inspect local child output");
         return Files.readString(output);
      }
      finally
      {
         if(child.isAlive())
         {
            child.destroyForcibly();
            assertTrue(child.waitFor(5, TimeUnit.SECONDS));
         }
      }
   }



   /*******************************************************************************
    ** Verify destination and actual encoded data; no unrelated API call is allowed.
    *******************************************************************************/
   private void assertWire(String text)
   {
      List<WireRequest> messages = requests.stream().filter(request -> request.path().equals("/api/chat.postMessage")).toList();
      assertEquals(1, messages.size());
      assertTrue(requests.size() <= 2);
      for(WireRequest captured : requests)
      {
         assertTrue(List.of("/api/auth.test", "/api/chat.postMessage").contains(captured.path()));
         assertEquals("Bearer " + TOKEN, captured.authorization());
      }
      WireRequest request = messages.get(0);
      assertEquals("POST", request.method());
      assertEquals("/api/chat.postMessage", request.path());
      assertEquals("Bearer " + TOKEN, request.authorization());
      assertTrue(request.contentType().startsWith("application/x-www-form-urlencoded"));
      Map<String, String> form = new LinkedHashMap<>();
      for(String field : request.body().split("&"))
      {
         String[] parts = field.split("=", 2);
         form.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), URLDecoder.decode(parts.length == 2 ? parts[1] : "", StandardCharsets.UTF_8));
      }
      assertEquals(CHANNEL, form.get("channel"));
      assertEquals(text, form.get("text"));
      assertEquals("1", form.get("mrkdwn"));
      assertFalse(form.containsKey("token"));
   }



   /*******************************************************************************
    ** Immutable capture of the owned loopback request.
    *******************************************************************************/
   private record WireRequest(String method, String path, String authorization, String contentType, String body)
   {
   }



   /*******************************************************************************
    ** Only child JVM entry point. The helper hardcodes the immutable SDK singleton,
    ** so test-only reflection redirects its endpoint before client initialization.
    ** Request construction and HTTP delivery remain the actual SDK implementation.
    *******************************************************************************/
   public static class OutboundProcess
   {
      /***************************************************************************
       ** Explicit process exit bounds SDK background workers, not fixture assertions.
       ***************************************************************************/
      public static void main(String[] args) throws Exception
      {
         URI endpoint = URI.create(args[0]);
         if(!"http".equals(endpoint.getScheme()) || !"127.0.0.1".equals(endpoint.getHost()) || endpoint.getPort() <= 0)
         {
            throw new IllegalArgumentException("Only owned loopback is allowed");
         }
         for(Map.Entry<String, Object> setting : Map.<String, Object>of("methodsEndpointUrlPrefix", endpoint.toString(), "statsEnabled", false,
            "httpClientCallTimeoutMillis", 5000).entrySet())
         {
            Field field = SlackConfig.class.getDeclaredField(setting.getKey());
            field.setAccessible(true);
            field.set(SlackConfig.DEFAULT, setting.getValue());
         }
         if(!endpoint.toString().equals(Slack.getInstance().methods().getEndpointUrlPrefix()))
         {
            throw new IllegalStateException("SDK endpoint is not the owned receiver");
         }
         try
         {
            QSlackImplementation.postMessage(args[1]);
            System.out.println("POST_MESSAGE_RETURNED");
         }
         finally
         {
            Slack.getInstance().close();
         }
         System.exit(0);
      }
   }
}
