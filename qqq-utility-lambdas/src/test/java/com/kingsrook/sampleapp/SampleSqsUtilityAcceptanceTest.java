/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qqq.utilitylambdas.QPostToSQSLambda;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Real SDK requests to an owned SQS protocol fixture; no AWS account or network.
 *******************************************************************************/
@Timeout(40)
class SampleSqsUtilityAcceptanceTest
{
   private static final ObjectMapper JSON = new ObjectMapper();
   @TempDir
   Path directory;
   private HttpServer server;
   private String queueUrl;
   private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
   private volatile String errorCode;



   /***************************************************************************
    ** The SDK uses QueueUrlHandler to direct these requests to the owned listener.
    ***************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      queueUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/000000000000/owned-queue";
      server.createContext("/", exchange ->
      {
         JsonNode body = JSON.readTree(exchange.getRequestBody().readAllBytes());
         requests.add(body);
         byte[] response;
         Integer status;
         if(errorCode != null)
         {
            status = 400;
            response = JSON.writeValueAsBytes(Map.of("__type", errorCode, "message", "Owned service refusal"));
            exchange.getResponseHeaders().set("x-amzn-ErrorType", errorCode);
         }
         else
         {
            status = 200;
            try
            {
               String md5 = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(body.path("MessageBody").asText().getBytes(StandardCharsets.UTF_8)));
               response = JSON.writeValueAsBytes(Map.of("MessageId", "owned-message", "MD5OfMessageBody", md5));
            }
            catch(Exception e)
            {
               throw new IllegalStateException(e);
            }
         }
         exchange.getResponseHeaders().set("Content-Type", "application/x-amz-json-1.0");
         exchange.sendResponseHeaders(status, response.length);
         try(var output = exchange.getResponseBody())
         {
            output.write(response);
         }
         exchange.close();
      });
      server.start();
   }



   /***************************************************************************
    ** Stop only the listener owned by this test.
    ***************************************************************************/
   @AfterEach
   void tearDown()
   {
      if(server != null)
      {
         server.stop(0);
      }
   }



   /***************************************************************************
    ** Queue and UTF-8 body must survive the public stream handler and SDK wire.
    ***************************************************************************/
   @Test
   void testExactQueueAndBody() throws Exception
   {
      String body = "{\"value\":\"café λ\",\"line\":\"one\\ntwo\"}";
      JsonNode result = invoke(body, queueUrl, true);
      assertEquals("{\"body\":\"OK\"}", result.path("response").asText());
      assertFalse(result.has("error"));
      assertEquals(1, requests.size());
      assertEquals(queueUrl, requests.getFirst().path("QueueUrl").asText());
      assertEquals(body, requests.getFirst().path("MessageBody").asText());
   }



   /***************************************************************************
    ** This utility transports raw text; it does not parse application JSON.
    ***************************************************************************/
   @Test
   void testMalformedJsonIsForwardedAsRawText() throws Exception
   {
      String body = "{not-json café";
      assertFalse(invoke(body, queueUrl, true).has("error"));
      assertEquals(1, requests.size());
      assertEquals(body, requests.getFirst().path("MessageBody").asText());
   }



   /***************************************************************************
    ** Empty-message validation belongs to SQS and cannot return an OK response.
    ***************************************************************************/
   @Test
   void testEmptyBodyServiceRejection() throws Exception
   {
      errorCode = "InvalidParameterValue";
      JsonNode result = invoke("", queueUrl, true);
      assertTrue(result.path("error").asText().contains("Owned service refusal"));
      assertEquals("", result.path("response").asText());
      assertEquals(1, requests.size());
      assertEquals("", requests.getFirst().path("MessageBody").asText());
   }



   /***************************************************************************
    ** A service authorization failure is an invocation failure, not success.
    ***************************************************************************/
   @Test
   void testServiceRefusal() throws Exception
   {
      errorCode = "AccessDeniedException";
      JsonNode result = invoke("owned-refused", queueUrl, true);
      assertTrue(result.path("error").asText().contains("Owned service refusal"));
      assertEquals("", result.path("response").asText());
      assertEquals(1, requests.size());
   }



   /***************************************************************************
    ** No developer credentials, profile, container or instance metadata are used.
    ***************************************************************************/
   @Test
   void testMissingCredentialsCannotSend() throws Exception
   {
      JsonNode result = invoke("owned-no-credentials", queueUrl, false);
      assertTrue(result.path("error").asText().contains("Unable to load AWS credentials"));
      assertEquals("", result.path("response").asText());
      assertTrue(requests.isEmpty());
   }



   /***************************************************************************
    ** Configuration errors must be diagnosed before credential lookup or send.
    ***************************************************************************/
   @Test
   void testMissingQueueUrlFailsBeforeCredentials() throws Exception
   {
      JsonNode result = invoke("owned-no-queue", null, false);
      assertTrue(result.path("error").asText().contains("QUEUE_URL"));
      assertEquals("", result.path("response").asText());
      assertTrue(requests.isEmpty());
   }



   /***************************************************************************
    ** Whitespace configuration must not fall through to the SDK endpoint.
    ***************************************************************************/
   @Test
   void testBlankQueueUrlFailsBeforeCredentials() throws Exception
   {
      for(String queue : List.of("", " \t "))
      {
         JsonNode result = invoke("owned-blank-queue", queue, false);
         assertTrue(result.path("error").asText().contains("QUEUE_URL"));
         assertEquals("", result.path("response").asText());
      }
      assertTrue(requests.isEmpty());
   }



   /***************************************************************************
    ** Transporting a body must not copy its content into Lambda logs.
    ***************************************************************************/
   @Test
   void testPayloadIsNotLogged() throws Exception
   {
      String marker = "SYNTHETIC-PRIVATE-BODY-604";
      JsonNode result = invoke(marker, queueUrl, true);
      assertFalse(result.has("error"));
      assertEquals(marker, requests.getFirst().path("MessageBody").asText());
      assertFalse(result.path("logs").asText().contains(marker));
      errorCode = "AccessDeniedException";
      JsonNode rejected = invoke(marker, queueUrl, true);
      assertTrue(rejected.has("error"));
      assertFalse(rejected.path("logs").asText().contains(marker));
   }



   /***************************************************************************
    ** A fresh JVM isolates environment-based default client configuration.
    ***************************************************************************/
   private JsonNode invoke(String body, String queue, Boolean credentials) throws Exception
   {
      Path input = directory.resolve("input.txt");
      Path result = directory.resolve("result.json");
      Files.writeString(input, body, StandardCharsets.UTF_8);
      Path credentialsFile = directory.resolve("empty-credentials");
      Files.writeString(credentialsFile, "");
      List<String> command = new ArrayList<>();
      command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
      ManagementFactory.getRuntimeMXBean().getInputArguments().stream().filter(a -> a.startsWith("-javaagent:")).forEach(command::add);
      command.add("-Duser.home=" + directory);
      command.add("-Dcom.amazonaws.sdk.disableEc2Metadata=true");
      command.add("-cp");
      command.add(System.getProperty("java.class.path"));
      command.add(Invocation.class.getName());
      command.add(input.toString());
      command.add(result.toString());
      ProcessBuilder builder = new ProcessBuilder(command);
      builder.environment().clear();
      builder.environment().put("AWS_REGION", "us-east-1");
      builder.environment().put("AWS_EC2_METADATA_DISABLED", "true");
      builder.environment().put("AWS_CREDENTIAL_PROFILES_FILE", credentialsFile.toString());
      if(queue != null)
      {
         builder.environment().put("QUEUE_URL", queue);
      }
      if(credentials)
      {
         builder.environment().put("AWS_ACCESS_KEY_ID", "owned-synthetic-key");
         builder.environment().put("AWS_SECRET_ACCESS_KEY", "owned-synthetic-secret");
      }
      builder.redirectErrorStream(true).redirectOutput(directory.resolve("child.log").toFile());
      Process child = builder.start();
      try
      {
         assertTrue(child.waitFor(30, TimeUnit.SECONDS), "Owned invocation did not exit");
         assertEquals(0, child.exitValue(), "Owned invocation harness failed");
         return JSON.readTree(Files.readString(result));
      }
      finally
      {
         if(child.isAlive())
         {
            child.destroyForcibly();
            child.waitFor(5, TimeUnit.SECONDS);
         }
      }
   }



   /***************************************************************************
    ** The child invokes the production handler, capturing only synthetic data.
    ***************************************************************************/
   public static class Invocation
   {
      /***************************************************************************
       **
       ***************************************************************************/
      public static void main(String[] args) throws Exception
      {
         StringBuilder logs = new StringBuilder();
         LambdaLogger logger = new LambdaLogger()
         {
            /*******************************************************************
             **
             *******************************************************************/
            @Override
            public void log(String message)
            {
               logs.append(message);
            }

            /*******************************************************************
             **
             *******************************************************************/
            @Override
            public void log(byte[] message)
            {
               logs.append(new String(message, StandardCharsets.UTF_8));
            }
         };
         Context context = (Context) Proxy.newProxyInstance(Context.class.getClassLoader(), new Class<?>[] {Context.class}, (proxy, method, values) ->
         {
            if("getLogger".equals(method.getName()))
            {
               return logger;
            }
            throw new UnsupportedOperationException(method.getName());
         });
         ByteArrayOutputStream output = new ByteArrayOutputStream();
         Map<String, String> result = new java.util.LinkedHashMap<>();
         try
         {
            new QPostToSQSLambda().handleRequest(new ByteArrayInputStream(Files.readAllBytes(Path.of(args[0]))), output, context);
         }
         catch(Exception e)
         {
            result.put("error", e.toString());
         }
         result.put("response", output.toString(StandardCharsets.UTF_8));
         result.put("logs", logs.toString());
         JSON.writeValue(Path.of(args[1]).toFile(), result);
      }
   }
}
